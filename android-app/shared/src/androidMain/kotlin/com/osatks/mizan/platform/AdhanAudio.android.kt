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

    actual fun play(m: Muezzin, preview: Boolean, onEnd: () -> Unit) {
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
            p.start()
            player = p
        } catch (e: Exception) {
            p.release()
            onEnd()
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
