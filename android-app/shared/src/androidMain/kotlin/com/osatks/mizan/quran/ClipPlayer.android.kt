package com.osatks.mizan.quran

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.osatks.mizan.platform.AndroidContext
import java.io.File

/** ذاكرة التلاوات المؤقتة (300 م.ب) مشتركة بين المشغّلات */
@UnstableApi
private object RecitationCache {
    val cache: SimpleCache by lazy {
        val ctx = AndroidContext.app
        SimpleCache(File(ctx.cacheDir, "recitation"), LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024), StandaloneDatabaseProvider(ctx))
    }
}

@UnstableApi
private class ExoClipPlayer : ClipPlayer {
    override var events: ClipEvents? = null

    private val player: ExoPlayer = run {
        val app = AndroidContext.app
        val http = DefaultHttpDataSource.Factory().setUserAgent("Mizan").setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
        val source = CacheDataSource.Factory()
            .setCache(RecitationCache.cache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(app, http))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()
        ExoPlayer.Builder(app)
            .setMediaSourceFactory(DefaultMediaSourceFactory(source))
            .setAudioAttributes(attrs, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            val auto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            item?.mediaId?.let { events?.onClip(it, auto) }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) { events?.onPlaying(isPlaying) }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { events?.onWantsPlay(playWhenReady) }
        override fun onPlaybackStateChanged(state: Int) {
            events?.onBuffering(state == Player.STATE_BUFFERING)
            if (state == Player.STATE_READY) events?.onDuration(player.duration)
            if (state == Player.STATE_ENDED) events?.onEnded()
        }
        override fun onPlayerError(error: PlaybackException) { events?.onError() }
    }

    init { player.addListener(listener) }

    private fun media(items: List<Pair<String, String>>) = items.map { (id, url) -> MediaItem.Builder().setUri(url).setMediaId(id).build() }

    override fun setItems(items: List<Pair<String, String>>) { player.setMediaItems(media(items)); player.prepare() }
    override fun addItems(items: List<Pair<String, String>>) = player.addMediaItems(media(items))
    override var repeat: Repeat
        get() = when (player.repeatMode) { Player.REPEAT_MODE_ONE -> Repeat.ONE; Player.REPEAT_MODE_ALL -> Repeat.ALL; else -> Repeat.OFF }
        set(v) { player.repeatMode = when (v) { Repeat.ONE -> Player.REPEAT_MODE_ONE; Repeat.ALL -> Player.REPEAT_MODE_ALL; Repeat.OFF -> Player.REPEAT_MODE_OFF } }
    override fun setSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    override fun play() = player.play()
    override fun pause() = player.pause()
    override val wantsPlay get() = player.playWhenReady
    override val index get() = player.currentMediaItemIndex
    override val count get() = player.mediaItemCount
    override fun stop() { player.stop(); player.clearMediaItems() }
    override fun release() { player.removeListener(listener); player.release() }
}

@androidx.annotation.OptIn(UnstableApi::class)
actual fun createClipPlayer(): ClipPlayer = ExoClipPlayer()
