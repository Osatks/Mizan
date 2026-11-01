package com.osatks.mizan.notify

import com.osatks.mizan.platform.init
import kotlinx.datetime.toKotlinLocalDate
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.osatks.mizan.MainActivity
import com.osatks.mizan.R
import com.osatks.mizan.core.ALERT_PRAYERS
import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * جدولة تنبيهات الأذان: تنبيه واحد للصلاة القادمة، ثم يجدول الذي يليه عند وقوعه.
 * لكل طريقة تنبيه قناة (أصوات القنوات لا تتغير بعد إنشائها، فالأذان يشغّله [AdhanService] والقناة صامتة):
 * الأذان، والاهتزاز بالنمط المختار (قناة لكل نمط)، والصامت. والقناة القديمة "adhan" بصوت الإشعار الافتراضي احتياطٌ
 * إن تعذّر تشغيل الخدمة.
 */
object PrayerAlarms {
    private const val CHANNEL = "adhan"
    private const val CHANNEL_VOICE = "adhan_voice"
    private const val CHANNEL_VIBE = "adhan_vibe_"
    private const val CHANNEL_SILENT = "adhan_silent"
    private const val REQ = 4242

    fun notificationId(key: String) = key.hashCode()

    /** إنشاء القنوات أو تحديث أسمائها بلغة الواجهة الحالية (لا يغيّر إعدادات المستخدم للقناة) */
    fun ensureChannel(ctx: Context) {
        AppSettings.init(ctx)
        val s = Lang.current.strings
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, s.channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.channelDesc
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_VOICE, "${s.channelName} · ${s.alertModeAdhan}", NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.channelDesc
                setSound(null, null)
                enableVibration(false)
            }
        )
        // قناة الاهتزاز للنمط المختار فقط؛ تُحذف قنوات الأنماط الأخرى
        val vibe = vibeChannel(AppSettings.vibePattern)
        nm.notificationChannels.filter { it.id.startsWith(CHANNEL_VIBE) && it.id != vibe }.forEach { nm.deleteNotificationChannel(it.id) }
        nm.createNotificationChannel(
            NotificationChannel(vibe, "${s.channelName} · ${s.alertModeVibrate}", NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.channelDesc
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = AppSettings.vibePattern.timings.toLongArray()
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, "${s.channelName} · ${s.alertModeSilent}", NotificationManager.IMPORTANCE_LOW).apply {
                description = s.channelDesc
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun vibeChannel(p: VibePattern) = CHANNEL_VIBE + p.key

    private fun pending(ctx: Context, key: String): PendingIntent {
        val i = Intent(ctx, PrayerReceiver::class.java).putExtra("prayer", key)
        return PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx, ""))
    }

    fun scheduleNext(ctx: Context) {
        AppSettings.init(ctx)
        cancel(ctx)
        if (!AppSettings.notify || ALERT_PRAYERS.all { AppSettings.alertMode(it) == AlertMode.OFF }) return
        val p = AppSettings.place
        val zone = runCatching { ZoneId.of(p.tz) }.getOrDefault(ZoneId.systemDefault())
        val now = LocalDateTime.now(zone)
        var target: Pair<LocalDateTime, String>? = null
        for (dayOffset in 0..1) {
            val day: LocalDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val t = PrayTimes.compute(day.toKotlinLocalDate(), p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi, p.elev)
            for (key in ALERT_PRAYERS) {
                if (AppSettings.alertMode(key) == AlertMode.OFF) continue
                val h = t[key]
                if (h.isNaN()) continue
                val dt = day.atStartOfDay().plusSeconds((h * 3600).toLong())
                if (dt.isAfter(now)) { target = dt to key; break }
            }
            if (target != null) break
        }
        val (dt, key) = target ?: return
        val millis = dt.atZone(zone).toInstant().toEpochMilli()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, key)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    /** إشعار دخول الوقت؛ [stop] زر «إيقاف» وإزالة الإشعار يوقفان الأذان */
    fun build(ctx: Context, key: String, channel: String, stop: Boolean): Notification {
        val s = Lang.current.strings
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(s.prayerNowTitle(s.prayer(key)))
            .setContentText(s.prayerNowText(s.placeName(AppSettings.place)))
            .setPriority(if (channel == CHANNEL_SILENT) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (stop) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
        if (stop) {
            fun halt(req: Int, removed: Boolean) = PendingIntent.getBroadcast(
                ctx, req, Intent(ctx, AdhanStopReceiver::class.java).putExtra("removed", removed),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            b.addAction(0, s.stopAdhan, halt(1, false)).setDeleteIntent(halt(2, true))
        }
        return b.build()
    }

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, key: String, channel: String) {
        runCatching { NotificationManagerCompat.from(ctx).notify(notificationId(key), build(ctx, key, channel, stop = false)) }
    }

    /**
     * دخل الوقت: حسب طريقة التنبيه، مع احترام وضع الهاتف — في «صامت» إشعار صامت، وفي «اهتزاز» يهتز بدل الأذان،
     * وفي «عدم الإزعاج» يقرر النظام إظهار الإشعار ولا يُشغَّل الأذان.
     */
    fun show(ctx: Context, key: String) {
        ensureChannel(ctx)
        val audio = ctx.getSystemService(AudioManager::class.java)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val dnd = nm.currentInterruptionFilter.let { it != NotificationManager.INTERRUPTION_FILTER_ALL && it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN }
        val ringer = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
        val vibe = vibeChannel(AppSettings.vibePattern)
        when (AppSettings.alertMode(key)) {
            AlertMode.OFF -> {}
            AlertMode.SILENT -> post(ctx, key, CHANNEL_SILENT)
            AlertMode.VIBRATE -> post(ctx, key, if (ringer == AudioManager.RINGER_MODE_SILENT) CHANNEL_SILENT else vibe)
            AlertMode.ADHAN -> when {
                ringer == AudioManager.RINGER_MODE_SILENT || dnd -> post(ctx, key, CHANNEL_SILENT)
                ringer == AudioManager.RINGER_MODE_VIBRATE -> post(ctx, key, vibe)
                // الخدمة تنشر إشعارها مع زر الإيقاف؛ إن منعها النظام (منبّه غير دقيق في أندرويد 12+) فإشعار بالصوت الافتراضي
                !AdhanService.start(ctx, key) -> post(ctx, key, CHANNEL)
            }
        }
    }

    const val VOICE_CHANNEL = CHANNEL_VOICE
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppSettings.init(context)
        // "name" كان يحمل اسم الصلاة بالعربية في الإصدارات السابقة؛ المنبه المجدول قبل التحديث يصل بلا مفتاح
        val legacy = intent.getStringExtra("name")
        val key = intent.getStringExtra("prayer") ?: PrayTimes.PRAYERS.firstOrNull { Ar.prayers[it] == legacy }.orEmpty()
        if (AppSettings.notify && key.isNotEmpty()) PrayerAlarms.show(context, key)
        PrayerAlarms.scheduleNext(context)
    }
}

/** زر «إيقاف» في إشعار الأذان، أو إزالة الإشعار */
class AdhanStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("removed", false)) AdhanService.removed = true
        context.stopService(Intent(context, AdhanService::class.java))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerAlarms.scheduleNext(context)
        // تغيّر الوقت أو المنطقة الزمنية: تُعاد كتابة الأدوات ومنبّه تحديثها
        com.osatks.mizan.widget.Widgets.updateAll(context)
    }
}
