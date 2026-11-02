@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.ui.screens.muezzinName
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategorySoloAmbient
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticDynamicParameterIDHapticIntensityControl
import platform.CoreHaptics.CHHapticParameterCurve
import platform.CoreHaptics.CHHapticParameterCurveControlPoint
import platform.CoreHaptics.CHHapticPattern
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.darwin.NSObject

/**
 * الأذان في iOS: AVAudioPlayer للتسجيل الكامل (المعاينة، وأذان الوقت والتطبيق مفتوح)،
 * و Core Haptics لأنماط الاهتزاز (الإشعارات نفسها تهتز بنمط النظام فقط).
 */
actual object AdhanAudio {
    private const val SESSION = "adhan"
    private var player: AVAudioPlayer? = null
    private var delegate: Finish? = null
    private var engine: CHHapticEngine? = null

    private class Finish(val onEnd: () -> Unit) : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            if (AdhanAudio.player === player) { AdhanAudio.stop(); onEnd() }
        }
    }

    private fun url(m: Muezzin): NSURL? {
        val path = AdhanSounds.localFile(m) ?: (NSBundle.mainBundle.resourcePath ?: return null) + "/assets/adhan/${m.file}"
        return if (NSFileManager.defaultManager.fileExistsAtPath(path)) NSURL.fileURLWithPath(path) else null
    }

    /** صلاة الأذان الذي يُشغَّل الآن (يضبطه AlertPresenter)، لعنوانه في شاشة القفل؛ null للمعاينة */
    internal var alertPrayer: String? = null

    actual fun play(m: Muezzin, preview: Boolean, onEnd: () -> Unit) = play(m, preview, 0.0, onEnd)

    actual fun alertPrayer(prayer: String) { alertPrayer = prayer }

    actual fun play(m: Muezzin, preview: Boolean, from: Double, onEnd: () -> Unit) {
        val forPrayer = if (preview) null else alertPrayer
        stop()
        alertPrayer = null
        val u = url(m) ?: return onEnd()
        // المعاينة تُسمع ولو كان مفتاح الصامت مفعّلاً؛ أذان الوقت يتبع مفتاح الصامت كبقية التنبيهات
        // الجلسة تُعطَّل بعد انتهاء الأذان أو إيقافه (AudioSessions)
        AudioSessions.begin(SESSION, (if (preview) AVAudioSessionCategoryPlayback else AVAudioSessionCategorySoloAmbient))
        val p = AVAudioPlayer(contentsOfURL = u, error = null)
        val d = Finish(onEnd)
        p.delegate = d
        if (!p.prepareToPlay()) { AudioSessions.end(SESSION); return onEnd() }
        // يكمل بعد أول تكبيرتين (AdhanResume): من موضعه، بتلاشٍ لطيف في أوله
        if (from > 0) {
            p.currentTime = from
            p.volume = 0f
        }
        if (!p.play()) { AudioSessions.end(SESSION); return onEnd() }
        if (from > 0) p.setVolume(1f, fadeDuration = 0.8)
        player = p
        delegate = d
        nowPlaying(m, forPrayer, p.duration)
    }

    /** الأذان في شاشة القفل: «حان الآن وقت صلاة العصر» والمؤذن، وزر الإيقاف */
    private fun nowPlaying(m: Muezzin, forPrayer: String?, duration: Double) {
        val s = Lang.current.strings
        val item = if (forPrayer != null) NowPlaying.Item(s.prayerNowTitle(s.prayer(forPrayer)), s.muezzinName(m), s.appName)
        else NowPlaying.Item(s.muezzinName(m), s.alertModeAdhan, s.appName)
        val stop = { AdhanPreview.stop() }
        NowPlaying.adhan(item, duration, NowPlaying.Controls(play = null, pause = stop, toggle = stop, next = null, previous = null, stop = stop))
    }

    actual fun stop() {
        player?.stop()
        player?.delegate = null
        player = null
        delegate = null
        AudioSessions.end(SESSION)
        NowPlaying.adhanEnded()
    }

    actual fun vibrate(p: VibePattern) {
        if (!CHHapticEngine.capabilitiesForHardware().supportsHaptics) return impacts(p)
        // المقاطع القصيرة نقرات، والطويلة اهتزاز متصل؛ والمقاطع المتلاصقة (الموجة والتصاعد) اهتزاز واحد
        // تتغير شدته بمنحنى، فيُحسّ تدرّجاً ناعماً لا خطوات متقطعة
        val events = ArrayList<CHHapticEvent>()
        val curves = ArrayList<CHHapticParameterCurve>()
        val groups = ArrayList<MutableList<Triple<Long, Long, Int>>>()
        for (pulse in p.pulses) {
            val last = groups.lastOrNull()?.last()
            if (last != null && last.first + last.second == pulse.first) groups.last() += pulse else groups += mutableListOf(pulse)
        }
        for (g in groups) {
            val start = g.first().first
            val ms = g.last().first + g.last().second - start
            val amp = g.maxOf { it.third }
            val sharp = 0.25f + 0.45f * amp / 255f
            if (g.size == 1 && ms <= 120) {
                events += CHHapticEvent(CHHapticEventTypeHapticTransient, listOf(
                    CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, amp / 255f),
                    CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, sharp + 0.2f),
                ), start / 1000.0)
                continue
            }
            events += CHHapticEvent(CHHapticEventTypeHapticContinuous, listOf(
                CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, if (g.size == 1) amp / 255f else 1f),
                CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, sharp),
            ), start / 1000.0, ms / 1000.0)
            if (g.size > 1) {
                // نقطة في منتصف كل مقطع بشدته، وبدايةٌ ونهاية (الحد 16 نقطة)
                val points = listOf(0.0 to g.first().third) +
                    g.map { (s, d, a) -> (s - start + d / 2) / 1000.0 to a } + (ms / 1000.0 to g.last().third)
                curves += CHHapticParameterCurve(
                    CHHapticDynamicParameterIDHapticIntensityControl,
                    points.take(16).map { (t, a) -> CHHapticParameterCurveControlPoint(t, a / 255f) },
                    start / 1000.0,
                )
            }
        }
        if (events.isEmpty()) return
        val pattern = CHHapticPattern(events = events, parameterCurves = curves, error = null)
        // المحرك يتوقف حين يغادر التطبيق الواجهة: يُعاد إنشاؤه عند الفشل
        for (attempt in 0..1) {
            val e = engine ?: CHHapticEngine(andReturnError = null).also { engine = it }
            val ok = e.startAndReturnError(null) && (e.createPlayerWithPattern(pattern, null)?.startAtTime(0.0, null) == true)
            if (ok) return
            engine = null
        }
        impacts(p)
    }

    /**
     * أجهزة بلا Core Haptics (iPhone 7 وما قبله): نقرة من مولّد النظام لكل مقطع بقوة تناسب شدته،
     * والمقاطع الطويلة اهتزاز النظام المعتاد (يعمل في كل جهاز)
     */
    private fun impacts(p: VibePattern) {
        for ((start, ms, amp) in p.pulses) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, start * 1_000_000), dispatch_get_main_queue()) {
                if (ms >= 400) AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                else UIImpactFeedbackGenerator(
                    when {
                        amp >= 200 -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
                        amp >= 110 -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
                        else -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
                    }
                ).impactOccurred()
            }
        }
    }

    /** Library/Sounds: iOS يبحث فيه عن أصوات الإشعارات بعد جذر حزمة التطبيق */
    private val soundsDir: String by lazy {
        @Suppress("UNCHECKED_CAST")
        val lib = (NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true) as List<String>).first()
        "$lib/Sounds"
    }

    actual val needsNotificationSound = true
    actual fun saveNotificationSound(name: String, bytes: ByteArray) = Fs.write("$soundsDir/$name", bytes)
    actual fun deleteNotificationSound(name: String) = Fs.delete("$soundsDir/$name")
    actual val patternsInNotifications = false
}
