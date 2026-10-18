@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.quran

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.setRate
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

/** تشغيل الآيات واحدة بعد الأخرى عبر AVPlayer (التكرار والانتقال يُداران هنا) */
private class AvClipPlayer : ClipPlayer {
    override var events: ClipEvents? = null
    private val player = AVPlayer()
    private val list = ArrayList<Pair<String, String>>()
    private var current = -1
    private var wants = false
    private var speed = 1f
    private val observers = ArrayList<NSObjectProtocol>()

    init {
        val nc = NSNotificationCenter.defaultCenter
        observers += nc.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, null, NSOperationQueue.mainQueue) { n ->
            if (n?.`object` == player.currentItem) ended()
        }
        observers += nc.addObserverForName(AVPlayerItemFailedToPlayToEndTimeNotification, null, NSOperationQueue.mainQueue) { n ->
            if (n?.`object` == player.currentItem) events?.onError()
        }
    }

    private fun load(i: Int, auto: Boolean) {
        current = i
        val url = NSURL(string = list[i].second)
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = url))
        events?.onClip(list[i].first, auto)
        if (wants) player.playImmediatelyAtRate(speed)
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
            else -> { wants = false; events?.onPlaying(false); events?.onWantsPlay(false); events?.onEnded() }
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
    }

    override fun play() {
        runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
            AVAudioSession.sharedInstance().setActive(true, null)
        }
        wants = true
        player.playImmediatelyAtRate(speed)
        events?.onWantsPlay(true); events?.onPlaying(true)
    }

    override fun pause() {
        wants = false
        player.pause()
        events?.onWantsPlay(false); events?.onPlaying(false)
    }

    override val wantsPlay get() = wants
    override val index get() = current
    override val count get() = list.size

    override fun stop() {
        wants = false
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        list.clear(); current = -1
    }

    override fun release() {
        stop()
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
    }
}

actual fun createClipPlayer(): ClipPlayer = AvClipPlayer()
