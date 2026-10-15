package com.osatks.mizan.notify

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** جدولة تنبيهات الأذان: تنبيه واحد للصلاة القادمة، ثم يجدول الذي يليه عند وقوعه */
object PrayerAlarms {
    private const val CHANNEL = "adhan"
    private const val REQ = 4242

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "مواقيت الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "تنبيه عند دخول وقت كل صلاة"
                }
            )
        }
    }

    private fun pending(ctx: Context, name: String): PendingIntent {
        val i = Intent(ctx, PrayerReceiver::class.java).putExtra("name", name)
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
            val t = PrayTimes.compute(day, p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi)
            for ((key, label) in PrayTimes.PRAYERS) {
                if (key == "sunrise") continue
                val h = t[key]
                if (h.isNaN()) continue
                val dt = day.atStartOfDay().plusSeconds((h * 3600).toLong())
                if (dt.isAfter(now)) { target = dt to label; break }
            }
            if (target != null) break
        }
        val (dt, name) = target ?: return
        val millis = dt.atZone(zone).toInstant().toEpochMilli()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, name)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, name: String) {
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("حان الآن وقت صلاة $name")
            .setContentText("${AppSettings.place.name} — حيّ على الصلاة")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(name.hashCode(), n) }
    }
}

class PrayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppSettings.init(context)
        val name = intent.getStringExtra("name").orEmpty()
        if (AppSettings.notify && name.isNotEmpty()) PrayerAlarms.show(context, name)
        PrayerAlarms.scheduleNext(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerAlarms.scheduleNext(context)
    }
}
