@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.VibePattern
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategorySoloAmbient
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticPattern
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

    actual fun play(m: Muezzin, preview: Boolean, onEnd: () -> Unit) {
        stop()
        val u = url(m) ?: return onEnd()
        // المعاينة تُسمع ولو كان مفتاح الصامت مفعّلاً؛ أذان الوقت يتبع مفتاح الصامت كبقية التنبيهات
        runCatching {
            AVAudioSession.sharedInstance().setCategory(if (preview) AVAudioSessionCategoryPlayback else AVAudioSessionCategorySoloAmbient, null)
            AVAudioSession.sharedInstance().setActive(true, null)
        }
        val p = AVAudioPlayer(contentsOfURL = u, error = null)
        val d = Finish(onEnd)
        p.delegate = d
        if (!p.prepareToPlay() || !p.play()) return onEnd()
        player = p
        delegate = d
    }

    actual fun stop() {
        player?.stop()
        player?.delegate = null
        player = null
        delegate = null
    }

    actual fun vibrate(p: VibePattern) {
        if (!CHHapticEngine.capabilitiesForHardware().supportsHaptics) {
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            return
        }
        // المقاطع القصيرة نقرات، والطويلة اهتزاز متصل بشدته
        val events = ArrayList<CHHapticEvent>()
        var t = 0L
        p.timings.forEachIndexed { i, ms ->
            val amp = p.amps.getOrElse(i) { 0 }
            if (amp > 0 && ms > 0) {
                val intensity = CHHapticEventParameter(CHHapticEventParameterIDHapticIntensity, amp / 255f)
                val sharpness = CHHapticEventParameter(CHHapticEventParameterIDHapticSharpness, if (ms <= 120) 0.6f else 0.3f)
                events += if (ms <= 120) CHHapticEvent(CHHapticEventTypeHapticTransient, listOf(intensity, sharpness), t / 1000.0)
                else CHHapticEvent(CHHapticEventTypeHapticContinuous, listOf(intensity, sharpness), t / 1000.0, ms / 1000.0)
            }
            t += ms
        }
        if (events.isEmpty()) return
        val pattern = CHHapticPattern(events = events, parameters = emptyList<Any>(), error = null)
        // المحرك يتوقف حين يغادر التطبيق الواجهة: يُعاد إنشاؤه عند الفشل
        for (attempt in 0..1) {
            val e = engine ?: CHHapticEngine(andReturnError = null).also { engine = it }
            val ok = e.startAndReturnError(null) && (e.createPlayerWithPattern(pattern, null)?.startAtTime(0.0, null) == true)
            if (ok) return
            engine = null
        }
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
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
