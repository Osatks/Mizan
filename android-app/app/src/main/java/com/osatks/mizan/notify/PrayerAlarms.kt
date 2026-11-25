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
import com.osatks.mizan.core.EndAlerts
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * جدولة تنبيهات الأذان: تنبيه واحد للصلاة القادمة، ثم يجدول الذي يليه عند وقوعه.
 * لكل طريقة تنبيه قناة (أصوات القنوات لا تتغير بعد إنشائها، فالأذان يشغّله [AdhanService] والقناة صامتة):
 * الأذان، والاهتزاز بالنمط المختار (قناة لكل نمط)، والصامت. والقناة القديمة "adhan" بصوت الإشعار الافتراضي احتياطٌ
 * إن تعذّر تشغيل الخدمة.
 *
 * تذكير «قبل خروج الوقت» منبّه مستقل ([REQ_END]) بالطريقة نفسها: تذكير واحد للقادم، ثم يجدول الذي يليه عند وقوعه، ويُعاد
 * مع كل جدولة للأذان (تغيّر الموقع أو الطريقة، والإقلاع، وتغيّر الوقت). قناته [CHANNEL_END] بصوت الإشعار الافتراضي
 * وأهمية عادية (تذكير لطيف بلا نافذة منبثقة)، ويحترم النظام فيها وضع الصامت و«عدم الإزعاج».
 */
object PrayerAlarms {
    private const val CHANNEL = "adhan"
    private const val CHANNEL_VOICE = "adhan_voice"
    private const val CHANNEL_VIBE = "adhan_vibe_"
    private const val CHANNEL_SILENT = "adhan_silent"
    private const val REQ = 4242
    private const val CHANNEL_END = "prayer_end"
    private const val REQ_END = 4243

    fun notificationId(key: String) = key.hashCode()
    private fun endNotificationId(key: String) = "end-$key".hashCode()

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
        // قناة «قبل خروج الوقت»: تُنشأ حين يُفعَّل التذكير لأول مرة، ثم تُحدَّث أسماؤها مع اللغة
        if (AppSettings.endAlerts.isNotEmpty() || nm.getNotificationChannel(CHANNEL_END) != null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_END, s.endAlertTitle, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = s.endAlertHint
                }
            )
        }
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
        // تذكير قبل خروج الوقت مستقل عن تنبيه الأذان: يُعاد قبل أي خروج مبكر هنا
        scheduleEnd(ctx)
        // تذكير صيام التطوع بمنبّهه الخاص، ولو كانت تنبيهات الأذان مطفأة
        runCatching { FastReminders.schedule(ctx) }
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

    private fun pendingEnd(ctx: Context, key: String, endAt: Long, minutes: Int): PendingIntent {
        val i = Intent(ctx, PrayerReceiver::class.java)
            .putExtra(EXTRA_END, true).putExtra("prayer", key).putExtra("endAt", endAt).putExtra("minutes", minutes)
        return PendingIntent.getBroadcast(ctx, REQ_END, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * يجدول تذكير «قبل خروج الوقت» القادم: أقرب موعد بعد الآن بين صلوات اليوم المفعّلة، ومن الأمس (العشاء ينتهي بمنتصف
     * الليل وقد يقع تذكيرها بعد الثانية عشرة) ومن الغد. الحساب في [EndAlerts] المشترك مع iOS.
     */
    fun scheduleEnd(ctx: Context) {
        AppSettings.init(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pendingEnd(ctx, "", 0L, 0))
        val prayers = AppSettings.endAlerts
        if (prayers.isEmpty()) return
        val minutes = AppSettings.endAlertMinutes
        val p = AppSettings.place
        val zone = runCatching { ZoneId.of(p.tz) }.getOrDefault(ZoneId.systemDefault())
        val now = LocalDateTime.now(zone)
        var best: Triple<LocalDateTime, String, LocalDateTime>? = null
        for (dayOffset in -1..1) {
            val day: LocalDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val t = PrayTimes.compute(day.toKotlinLocalDate(), p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi, p.elev)
            for (key in ALERT_PRAYERS) {
                if (key !in prayers) continue
                val h = EndAlerts.hours(t, key, minutes)
                if (h.isNaN()) continue
                val at = day.atStartOfDay().plusSeconds((h * 3600).toLong())
                if (!at.isAfter(now) || (best != null && !at.isBefore(best.first))) continue
                val end = day.atStartOfDay().plusSeconds((EndAlerts.end(t, key) * 3600).toLong())
                best = Triple(at, key, end)
            }
        }
        val (at, key, end) = best ?: return
        val millis = at.atZone(zone).toInstant().toEpochMilli()
        val endMillis = end.atZone(zone).toInstant().toEpochMilli()
        val pi = pendingEnd(ctx, key, endMillis, minutes)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    /**
     * حان تذكير «قبل خروج الوقت»: «بقي 15 دقيقة على خروج وقت الظهر» ونهاية الوقت بتوقيت المكان. الدقائق الباقية تُحسب
     * عند الوصول (قد يتأخر المنبّه غير الدقيق)؛ وإن خرج الوقت فعلاً فلا تذكير.
     */
    @SuppressLint("MissingPermission")
    fun showEnd(ctx: Context, key: String, endAt: Long, minutes: Int) {
        if (key !in AppSettings.endAlerts || endAt <= 0L) return
        val left = ((endAt - System.currentTimeMillis() + 30_000L) / 60_000L).toInt()
        if (left < 1) return
        ensureChannel(ctx)
        val s = Lang.current.strings
        val zone = runCatching { ZoneId.of(AppSettings.place.tz) }.getOrDefault(ZoneId.systemDefault())
        val local = java.time.Instant.ofEpochMilli(endAt).atZone(zone).toLocalTime()
        val endHours = local.hour + local.minute / 60.0 + local.second / 3600.0
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_END)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(s.endAlertNotice(s.prayer(key), left.coerceAtMost(minutes.coerceAtLeast(1))))
            .setContentText(s.endAlertBody(s.time(endHours)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            // يختفي وحده حين يخرج الوقت
            .setTimeoutAfter((endAt - System.currentTimeMillis()).coerceAtLeast(1L))
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(endNotificationId(key), n) }
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
                ctx, req, Intent(ctx, AdhanStopReceiver::class.java).putExtra("removed", removed).putExtra("prayer", key),
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
    /** منبّه «قبل خروج الوقت» يصل إلى [PrayerReceiver] نفسه بهذا المفتاح */
    const val EXTRA_END = "end"
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppSettings.init(context)
        // تذكير «قبل خروج الوقت»: يُعرض ثم يُجدول الذي يليه، ولا يمس منبّه الأذان
        if (intent.getBooleanExtra(PrayerAlarms.EXTRA_END, false)) {
            val key = intent.getStringExtra("prayer").orEmpty()
            PrayerAlarms.showEnd(context, key, intent.getLongExtra("endAt", 0L), intent.getIntExtra("minutes", AppSettings.endAlertMinutes))
            PrayerAlarms.scheduleEnd(context)
            return
        }
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
        // أوقفه المستخدم: لا يكمله التطبيق إن فُتح بعده
        intent.getStringExtra("prayer")?.let { com.osatks.mizan.core.AdhanResume.done(it) }
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
