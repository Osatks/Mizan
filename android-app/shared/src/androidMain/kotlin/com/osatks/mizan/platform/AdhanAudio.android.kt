package com.osatks.mizan.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.VibePattern

actual object AdhanAudio {
    private var player: MediaPlayer? = null

    actual fun play(m: Muezzin, preview: Boolean, onEnd: () -> Unit) = play(m, preview, 0.0, onEnd)

    actual fun alertPrayer(prayer: String) {}

    actual fun play(m: Muezzin, preview: Boolean, from: Double, onEnd: () -> Unit) {
        stop()
        val p = MediaPlayer()
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            setSource(AndroidContext.app, p, m)
            p.setOnCompletionListener { if (player === p) { stop(); onEnd() } }
            p.setOnErrorListener { _, _, _ -> if (player === p) { stop(); onEnd() }; true }
            p.prepare()
            // يكمل بعد أول تكبيرتين (AdhanResume): من موضعه، بتلاشٍ لطيف في أوله
            if (from > 0) {
                p.seekTo((from * 1000).toInt())
                p.setVolume(0f, 0f)
            }
            p.start()
            player = p
            if (from > 0) ramp(p, 0f, 1f, 800L) {}
        } catch (e: Exception) {
            p.release()
            onEnd()
        }
    }

    /** يغيّر صوت [p] تدريجياً من [a] إلى [b] في [ms] (ثم [done])، ما دام لم يُستبدل أو يُوقف */
    fun ramp(p: MediaPlayer, a: Float, b: Float, ms: Long, done: () -> Unit) {
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        val steps = 20
        for (i in 1..steps) {
            h.postDelayed({
                val v = a + (b - a) * i / steps
                runCatching { p.setVolume(v, v) }
                if (i == steps) done()
            }, ms * i / steps)
        }
    }

    /** مصدر الأذان: الملف المنزَّل، أو المضمّن في assets (لا يُضغط m4a في الحزمة فيُقرأ مباشرة) */
    fun setSource(ctx: Context, p: MediaPlayer, m: Muezzin) {
        val f = AdhanSounds.localFile(m)
        if (f != null) p.setDataSource(f)
        else ctx.assets.openFd("adhan/${m.file}").use { p.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
    }

    actual fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    fun vibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)

    fun effect(v: Vibrator, p: VibePattern): VibrationEffect {
        val t = p.timings.toLongArray()
        return if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(t, p.amps.toIntArray(), -1)
        else VibrationEffect.createWaveform(t, -1)
    }

    actual fun vibrate(p: VibePattern) {
        val v = vibrator(AndroidContext.app) ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(effect(v, p)) }
    }

    actual val needsNotificationSound = false
    actual fun saveNotificationSound(name: String, bytes: ByteArray) {}
    actual fun deleteNotificationSound(name: String) {}
    actual val patternsInNotifications = true
}
