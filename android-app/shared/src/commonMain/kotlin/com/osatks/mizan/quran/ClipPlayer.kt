package com.osatks.mizan.quran

/** تكرار قائمة التشغيل */
enum class Repeat { OFF, ONE, ALL }

/** أحداث المشغّل (تصل على الخيط الرئيسي) */
interface ClipEvents {
    /** بدأ مقطع جديد؛ auto: انتقال تلقائي بعد انتهاء السابق أو تكراره */
    fun onClip(id: String, auto: Boolean)
    fun onPlaying(playing: Boolean)
    fun onWantsPlay(wants: Boolean)
    fun onBuffering(buffering: Boolean)
    fun onDuration(ms: Long)
    fun onEnded()
    fun onError()
}

/**
 * مشغّل مقاطع صوتية متتابعة (آية آية): ExoPlayer في أندرويد مع ذاكرة مؤقتة، و AVPlayer في iOS.
 * items: (المعرّف، الرابط).
 */
interface ClipPlayer {
    var events: ClipEvents?
    fun setItems(items: List<Pair<String, String>>)
    fun addItems(items: List<Pair<String, String>>)
    var repeat: Repeat
    fun setSpeed(speed: Float)
    fun play()
    fun pause()
    val wantsPlay: Boolean
    val index: Int
    val count: Int
    fun stop()
    fun release()
}

expect fun createClipPlayer(): ClipPlayer
