@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.quran

import com.osatks.mizan.platform.AudioSessions
import com.osatks.mizan.platform.NowPlaying
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.setRate
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/** تشغيل الآيات واحدة بعد الأخرى عبر AVPlayer (التكرار والانتقال يُداران هنا) */
private class AvClipPlayer : ClipPlayer {
    override var events: ClipEvents? = null
    private val player = AVPlayer()
    private val list = ArrayList<Pair<String, String>>()
    private var current = -1
    private var wants = false
    private var speed = 1f
    private val observers = ArrayList<NSObjectProtocol>()
    /** مراقب الوقت (كل ثانية والتلاوة تعمل): مدة الآية لشاشة القفل حين تُعرف */
    private var clock: Any? = null
    private var reportedDuration = -1.0
    /** أوقفتها مكالمة أو منبّه وهي تعمل: تُستأنف بعده إن سمح النظام */
    private var interrupted = false

    init {
        val nc = NSNotificationCenter.defaultCenter
        observers += nc.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, null, NSOperationQueue.mainQueue) { n ->
            if (n?.`object` == player.currentItem) ended()
        }
        observers += nc.addObserverForName(AVPlayerItemFailedToPlayToEndTimeNotification, null, NSOperationQueue.mainQueue) { n ->
            if (n?.`object` == player.currentItem) events?.onError()
        }
        // مكالمة أو منبّه: يتوقف AVPlayer وحده، فتُسجَّل التلاوة متوقفة (الزر وشاشة القفل)، وتُستأنف بعدها إن سمح النظام
        observers += nc.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { n -> interruption(n) }
        // فصل السماعة: تتوقف التلاوة ولا تُكمل من مكبّر الهاتف
        observers += nc.addObserverForName(AVAudioSessionRouteChangeNotification, null, NSOperationQueue.mainQueue) { n ->
            if (wants && number(n, AVAudioSessionRouteChangeReasonKey) == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                interrupted = false
                pause()
            }
        }
        clock = player.addPeriodicTimeObserverForInterval(CMTimeMakeWithSeconds(1.0, 600), null) { _ ->
            if (duration() != reportedDuration) timing()
        }
    }

    private fun number(n: NSNotification?, key: String): ULong? = when (val v = n?.userInfo?.get(key)) {
        is NSNumber -> v.unsignedLongValue
        is Number -> v.toLong().toULong()
        else -> null
    }

    private fun interruption(n: NSNotification?) {
        when (number(n, AVAudioSessionInterruptionTypeKey)) {
            AVAudioSessionInterruptionTypeBegan -> if (wants) { pause(); interrupted = true }
            AVAudioSessionInterruptionTypeEnded -> {
                val resume = ((number(n, AVAudioSessionInterruptionOptionKey) ?: 0uL) and AVAudioSessionInterruptionOptionShouldResume) != 0uL
                if (interrupted && resume && current >= 0) play()
                interrupted = false
            }
        }
    }

    /** مدة الآية الحالية بالثواني، أو 0 قبل أن تُعرف */
    private fun duration(): Double {
        val sec = player.currentItem?.let { CMTimeGetSeconds(it.duration) } ?: return 0.0
        return if (sec.isFinite() && sec > 0) sec else 0.0
    }

    /** الوقت المنقضي ومدة الآية والسرعة لـ «يُشغَّل الآن» */
    private fun timing() {
        val d = duration()
        reportedDuration = d
        val e = CMTimeGetSeconds(player.currentTime()).let { if (it.isFinite() && it > 0) it else 0.0 }
        NowPlaying.recitationTiming(e, d, speed.toDouble())
    }

    private fun load(i: Int, auto: Boolean) {
        current = i
        val url = NSURL(string = list[i].second)
        val item = AVPlayerItem(uRL = url)
        player.replaceCurrentItemWithPlayerItem(item)
        watch(item)
        events?.onClip(list[i].first, auto)
        if (wants) player.playImmediatelyAtRate(speed)
        timing()
    }

    /**
     * مقطع لم يُحمَّل أصلاً (لا اتصال، أو خطأ من الخادم) لا يرسل AVPlayerItemFailedToPlayToEndTime:
     * يُفحص خطؤه كل ثانية ما دام هو المقطع الحالي، فتظهر رسالة الخطأ بدل تلاوة "تعمل" بلا صوت
     */
    private var watchToken = 0

    private fun watch(item: AVPlayerItem) {
        val token = ++watchToken
        fun check() {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 1_000_000_000L), dispatch_get_main_queue()) {
                if (token != watchToken || player.currentItem != item) return@dispatch_after
                if (item.error != null) { if (wants) events?.onError() } else check()
            }
        }
        check()
    }

    private fun ended() {
        player.currentItem?.let { item ->
            val sec = CMTimeGetSeconds(item.duration)
            if (sec.isFinite() && sec > 0) events?.onDuration((sec * 1000).toLong())
        }
        when {
            repeat == Repeat.ONE -> load(current, true)
            current + 1 < list.size -> load(current + 1, true)
            repeat == Repeat.ALL && list.isNotEmpty() -> load(0, true)
            else -> { wants = false; AudioSessions.end(SESSION); events?.onPlaying(false); events?.onWantsPlay(false); events?.onEnded() }
        }
    }

    override fun setItems(items: List<Pair<String, String>>) {
        list.clear(); list.addAll(items)
        if (list.isNotEmpty()) load(0, false)
    }

    override fun addItems(items: List<Pair<String, String>>) { list.addAll(items) }
    override var repeat: Repeat = Repeat.OFF

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (wants) player.setRate(speed)
        timing()
    }

    override fun play() {
        // فئة Playback: التلاوة تُسمع ومفتاح الصامت مفعّل، وتستمر والشاشة مطفأة (وضع الخلفية audio)
        AudioSessions.begin(SESSION, AVAudioSessionCategoryPlayback)
        wants = true
        interrupted = false
        player.playImmediatelyAtRate(speed)
        events?.onWantsPlay(true); events?.onPlaying(true)
        timing()
    }

    override fun pause() {
        wants = false
        player.pause()
        AudioSessions.end(SESSION)
        events?.onWantsPlay(false); events?.onPlaying(false)
        timing()
    }

    override val wantsPlay get() = wants
    override val index get() = current
    override val count get() = list.size

    override fun stop() {
        wants = false
        interrupted = false
        watchToken++
        player.pause()
        AudioSessions.end(SESSION)
        player.replaceCurrentItemWithPlayerItem(null)
        list.clear(); current = -1
    }

    override fun release() {
        stop()
        clock?.let { player.removeTimeObserver(it) }
        clock = null
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
    }
}

private const val SESSION = "recitation"

actual fun createClipPlayer(): ClipPlayer {
    // شاشة القفل والنشاط الحي يتبعان التلاوة منذ أول تشغيل
    RecitationLive.start()
    return AvClipPlayer()
}
