@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import platform.Foundation.NSDate
import platform.Foundation.NSNumber
import platform.Foundation.NSThread
import platform.Foundation.timeIntervalSince1970
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeAudio
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueCount
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueIndex
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommand
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.UIKit.UIImage

/**
 * «يُشغَّل الآن» في شاشة القفل ومركز التحكم والسماعات: عنوان ما يُسمع (السورة والآية، أو الأذان) والقارئ
 * وشعار ميزان والوقت المنقضي ومدة المقطع، وأزرار التشغيل والإيقاف والآية التالية والسابقة.
 * مصدران: التلاوة (RecitationLive) والأذان (AdhanAudio)؛ الأذان مقدَّم ما دام يُسمع، ثم تعود التلاوة إن بقيت.
 */
internal object NowPlaying {
    /** ما يُعرض: index موضعه في قائمته (الآية من السورة) و count طولها، أو -1 */
    class Item(val title: String, val artist: String, val album: String, val index: Int = -1, val count: Int = 0)

    /** أزرار المصدر؛ null لما لا يدعمه (الأذان لا تالي له) */
    class Controls(
        val play: (() -> Unit)?, val pause: () -> Unit, val toggle: () -> Unit,
        val next: (() -> Unit)?, val previous: (() -> Unit)?, val stop: () -> Unit,
    )

    /** لقطة توقيت: الثواني المنقضية عند اللحظة at، والمدة (0 = غير معروفة بعد) */
    private class Timing(val elapsed: Double, val duration: Double, val at: Double)

    private class Source(var item: Item? = null, var playing: Boolean = false, var rate: Double = 1.0, var timing: Timing? = null, var controls: Controls? = null)

    private val recitation = Source()
    private val adhan = Source()
    private var registered = false

    private fun now() = NSDate().timeIntervalSince1970

    // ---------- التلاوة ----------

    fun recitationControls(c: Controls) { recitation.controls = c }

    /** ما يُتلى الآن، أو null حين تنتهي التلاوة */
    fun recitation(item: Item?, playing: Boolean) {
        recitation.item = item
        recitation.playing = playing
        if (item == null) recitation.timing = null
        publish()
    }

    /** يرسلها المشغّل عند كل مقطع وتشغيل وإيقاف، وحين تُعرف مدة الآية */
    fun recitationTiming(elapsed: Double, duration: Double, rate: Double) {
        recitation.timing = Timing(elapsed, duration, now())
        recitation.rate = rate
        publish()
    }

    // ---------- الأذان ----------

    fun adhan(item: Item, duration: Double, controls: Controls) {
        adhan.item = item
        adhan.playing = true
        adhan.rate = 1.0
        adhan.timing = Timing(0.0, duration, now())
        adhan.controls = controls
        publish()
    }

    fun adhanEnded() {
        if (adhan.item == null) return
        adhan.item = null
        adhan.timing = null
        publish()
    }

    // ---------- العرض ----------

    private fun current(): Source? = when {
        adhan.item != null -> adhan
        recitation.item != null -> recitation
        else -> null
    }

    /** شعار ميزان (NowPlayingArtwork في Assets.xcassets) */
    private val artwork: MPMediaItemArtwork? by lazy {
        val img = UIImage.imageNamed("NowPlayingArtwork") ?: return@lazy null
        MPMediaItemArtwork(boundsSize = img.size, requestHandler = { _ -> img })
    }

    private fun publish() {
        val src = current()
        commands(src)
        val center = MPNowPlayingInfoCenter.defaultCenter()
        val item = src?.item
        if (src == null || item == null) {
            center.nowPlayingInfo = null
            return
        }
        val rate = if (src.playing) src.rate else 0.0
        val info = HashMap<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.title
        info[MPMediaItemPropertyArtist] = item.artist
        info[MPMediaItemPropertyAlbumTitle] = item.album
        artwork?.let { info[MPMediaItemPropertyArtwork] = it }
        info[MPNowPlayingInfoPropertyMediaType] = NSNumber(unsignedLong = MPNowPlayingInfoMediaTypeAudio)
        info[MPNowPlayingInfoPropertyPlaybackRate] = NSNumber(double = rate)
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = NSNumber(double = 1.0)
        src.timing?.let { t ->
            // الثواني المنقضية الآن (النظام يكمل العدّ وحده بحسب السرعة)
            var elapsed = t.elapsed + if (src.playing) (now() - t.at) * src.rate else 0.0
            if (t.duration > 0) {
                elapsed = elapsed.coerceAtMost(t.duration)
                info[MPMediaItemPropertyPlaybackDuration] = NSNumber(double = t.duration)
            }
            info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = NSNumber(double = elapsed.coerceAtLeast(0.0))
        }
        if (item.index >= 0 && item.count > 0) {
            info[MPNowPlayingInfoPropertyPlaybackQueueIndex] = NSNumber(unsignedLong = item.index.toULong())
            info[MPNowPlayingInfoPropertyPlaybackQueueCount] = NSNumber(unsignedLong = item.count.toULong())
        }
        center.nowPlayingInfo = info
    }

    // ---------- الأزرار ----------

    private enum class Cmd { PLAY, PAUSE, TOGGLE, NEXT, PREVIOUS, STOP }

    private fun commands(src: Source?) {
        val c = src?.controls
        if (c == null && !registered) return
        val center = MPRemoteCommandCenter.sharedCommandCenter()
        if (!registered) {
            registered = true
            // الأزرار تُسجَّل مرة واحدة، وكل ضغطة تذهب إلى المصدر الظاهر الآن
            fun bind(cmd: MPRemoteCommand, k: Cmd) {
                cmd.addTargetWithHandler { _ -> handle(k) }
            }
            bind(center.playCommand, Cmd.PLAY)
            bind(center.pauseCommand, Cmd.PAUSE)
            bind(center.togglePlayPauseCommand, Cmd.TOGGLE)
            bind(center.nextTrackCommand, Cmd.NEXT)
            bind(center.previousTrackCommand, Cmd.PREVIOUS)
            bind(center.stopCommand, Cmd.STOP)
        }
        center.playCommand.enabled = c?.play != null
        center.pauseCommand.enabled = c != null
        center.togglePlayPauseCommand.enabled = c != null
        center.nextTrackCommand.enabled = c?.next != null
        center.previousTrackCommand.enabled = c?.previous != null
        center.stopCommand.enabled = c != null
        // لا تقديم ولا تأخير داخل الآية: الشريط للعرض فقط
        center.changePlaybackPositionCommand.enabled = false
    }

    private fun handle(k: Cmd): Long {
        val c = current()?.controls ?: return MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
        val action: (() -> Unit)? = when (k) {
            Cmd.PLAY -> c.play
            Cmd.PAUSE -> c.pause
            Cmd.TOGGLE -> c.toggle
            Cmd.NEXT -> c.next
            Cmd.PREVIOUS -> c.previous
            Cmd.STOP -> c.stop
        }
        if (action == null) return MPRemoteCommandHandlerStatusNoActionableNowPlayingItem
        // تصل الأوامر على الخيط الرئيسي عادة؛ وإن لم تصل عليه نُنفذها هناك (حال التلاوة من Compose)
        if (NSThread.isMainThread) action() else runOnMain(action)
        return MPRemoteCommandHandlerStatusSuccess
    }
}
