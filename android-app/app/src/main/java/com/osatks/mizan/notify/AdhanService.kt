package com.osatks.mizan.notify

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.osatks.mizan.core.AdhanResume
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.platform.AdhanAudio
import com.osatks.mizan.platform.init

/**
 * يشغّل الأذان عند دخول الوقت: خدمة أمامية (تشغيل وسائط) بصوت المنبّه، وإشعارها فيه زر «إيقاف».
 * والتطبيق مغلق: أول تكبيرتين ثم يتلاشى الصوت ([Muezzin.takbir])، ويكمل الأذان من هناك إن فُتح التطبيق بعده
 * (AdhanResume)؛ والتطبيق مفتوح: كاملاً.
 * يتوقف الأذان بالزر، أو بإزالة الإشعار، أو بضغط زر الصوت، أو بمكالمة (فقدان تركيز الصوت)، أو بانتهائه،
 * ويبقى بعده إشعار الصلاة العادي (إلا إن أزاله المستخدم).
 */
class AdhanService : Service() {
    companion object {
        /** أزال المستخدم الإشعار: لا يُعاد نشره بعد الإيقاف */
        @Volatile var removed = false

        /** فُتح التطبيق فأكمل الأذان (AdhanResume): تتوقف الخدمة بلا تعليم الأذان منتهياً */
        fun handOff(ctx: Context) {
            ctx.stopService(Intent(ctx, AdhanService::class.java))
        }

        /** false إن منع النظام بدء الخدمة من الخلفية */
        fun start(ctx: Context, prayer: String): Boolean = try {
            removed = false
            ContextCompat.startForegroundService(ctx, Intent(ctx, AdhanService::class.java).putExtra("prayer", prayer))
            true
        } catch (e: Exception) {
            false
        }
    }

    private var player: MediaPlayer? = null
    private var prayer: String? = null
    private var focus: AudioFocusRequest? = null
    private var volume: BroadcastReceiver? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppSettings.init(this)
        val key = intent?.getStringExtra("prayer")
        if (key == null) { stopSelf(); return START_NOT_STICKY }
        release()
        prayer = key
        val n = PrayerAlarms.build(this, key, PrayerAlarms.VOICE_CHANNEL, stop = true)
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        try {
            ServiceCompat.startForeground(this, PrayerAlarms.notificationId(key), n, type)
        } catch (e: Exception) {
            stopSelf(); return START_NOT_STICKY
        }
        if (!play(key)) stopSelf()
        return START_NOT_STICKY
    }

    private fun play(key: String): Boolean {
        val am = getSystemService(AudioManager::class.java) ?: return false
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        // مكالمة جارية أو تطبيق آخر يمسك الصوت: يكفي الإشعار
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stopSelf()
            }
            .build()
        if (am.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        focus = req
        val p = MediaPlayer()
        return try {
            p.setAudioAttributes(attrs)
            p.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            AdhanAudio.setSource(this, p, Muezzins.forPrayer(key))
            p.setOnCompletionListener { AdhanResume.done(key); stopSelf() }
            p.setOnErrorListener { _, _, _ -> stopSelf(); true }
            p.prepare()
            p.start()
            player = p
            listenVolumeKeys()
            val m = Muezzins.forPrayer(key)
            if (AdhanResume.appVisible) {
                // التطبيق أمام المستخدم: الأذان كاملاً، ولا يُكمَل مرة أخرى
                AdhanResume.done(key)
            } else {
                // أول تكبيرتين ثم يتلاشى الصوت ويتوقف (يكمل التطبيق من هنا إن فُتح)
                android.os.Handler(mainLooper).postDelayed({
                    if (player === p) AdhanAudio.ramp(p, 1f, 0f, (Muezzin.FADE * 1000).toLong()) { if (player === p) stopSelf() }
                }, (m.takbir * 1000).toLong())
            }
            true
        } catch (e: Exception) {
            p.release()
            false
        }
    }

    /** زر الصوت يوقف الأذان (كما في المنبّهات)؛ تُهمل اللحظة الأولى لتجنب بثّ يصل مع بدء التشغيل */
    private fun listenVolumeKeys() {
        val since = SystemClock.elapsedRealtime()
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                if (SystemClock.elapsedRealtime() - since > 1500) { prayer?.let { AdhanResume.done(it) }; stopSelf() }
            }
        }
        ContextCompat.registerReceiver(this, r, IntentFilter("android.media.VOLUME_CHANGED_ACTION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        volume = r
    }

    private fun release() {
        volume?.let { runCatching { unregisterReceiver(it) } }
        volume = null
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        focus?.let { getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(it) }
        focus = null
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        release()
        val key = prayer
        if (removed || key == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } else {
            // يبقى إشعار الصلاة بلا زر الإيقاف (التحديث لا ينبّه مرة أخرى)
            runCatching {
                NotificationManagerCompat.from(this).notify(PrayerAlarms.notificationId(key), PrayerAlarms.build(this, key, PrayerAlarms.VOICE_CHANNEL, stop = false))
            }
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        super.onDestroy()
    }
}
