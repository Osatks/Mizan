package com.osatks.mizan.notify

import com.osatks.mizan.platform.init
import kotlinx.datetime.toKotlinLocalDate
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.osatks.mizan.MainActivity
import com.osatks.mizan.R
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** جدولة تنبيهات الأذان: تنبيه واحد للصلاة القادمة، ثم يجدول الذي يليه عند وقوعه */
object PrayerAlarms {
    private const val CHANNEL = "adhan"
    private const val REQ = 4242

    /** إنشاء القناة أو تحديث اسمها بلغة الواجهة الحالية (لا يغيّر إعدادات المستخدم للقناة) */
    fun ensureChannel(ctx: Context) {
        AppSettings.init(ctx)
        val s = Lang.current.strings
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, s.channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.channelDesc
            }
        )
    }

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
        if (!AppSettings.notify) return
        val p = AppSettings.place
        val zone = runCatching { ZoneId.of(p.tz) }.getOrDefault(ZoneId.systemDefault())
        val now = LocalDateTime.now(zone)
        var target: Pair<LocalDateTime, String>? = null
        for (dayOffset in 0..1) {
            val day: LocalDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val t = PrayTimes.compute(day.toKotlinLocalDate(), p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi)
            for (key in PrayTimes.PRAYERS) {
                if (key == "sunrise") continue
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

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, key: String) {
        ensureChannel(ctx)
        val s = Lang.current.strings
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(s.prayerNowTitle(s.prayer(key)))
            .setContentText(s.prayerNowText(s.placeName(AppSettings.place)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(key.hashCode(), n) }
    }
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

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerAlarms.scheduleNext(context)
        // تغيّر الوقت أو المنطقة الزمنية: تُعاد كتابة الأدوات ومنبّه تحديثها
        com.osatks.mizan.widget.Widgets.updateAll(context)
    }
}
