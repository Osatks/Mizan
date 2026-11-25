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
import com.osatks.mizan.core.FastCalendar
import com.osatks.mizan.core.Fasting
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.platform.init
import kotlinx.datetime.LocalDate

/**
 * تذكير صيام التطوع (مطفأ افتراضياً): إشعار في الساعة [Fasting.REMIND_HOUR] من مساء اليوم الذي يسبق يوم تطوع.
 * منبّه واحد للتذكير القادم بمعزل عن منبّه الأذان (رمز طلب آخر)، ثم يجدول الذي يليه عند وقوعه، كما في [PrayerAlarms].
 * يُجدول مع كل جدولة للأذان (PrayerAlarms.scheduleNext: فتح التطبيق، وتغيير الإعدادات، وإعادة التشغيل).
 */
object FastReminders {
    private const val CHANNEL = "fast_reminder"
    private const val REQ = 4343
    private const val NOTIFICATION_ID = 4343

    private fun pending(ctx: Context, day: String): PendingIntent {
        val i = Intent(ctx, FastReminderReceiver::class.java).putExtra("day", day)
        return PendingIntent.getBroadcast(ctx, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context) {
        AppSettings.init(ctx)
        Fasting.ensure()
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx, ""))
        if (!Fasting.state.remind) return
        val (at, day) = Fasting.reminders(1).firstOrNull() ?: return
        val millis = Fasting.epochMillis(at)
        val pi = pending(ctx, day.date.toString())
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
    }

    private fun ensureChannel(ctx: Context) {
        val s = Lang.current.strings
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, s.fastingChannel, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = s.fastingRemindBody
            }
        )
    }

    /** إشعار اليوم [day] (yyyy-mm-dd) بأسباب صيامه، إن بقي يوم تطوع */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, day: String) {
        val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return
        val h = FastCalendar.hijri(date, Fasting.offset()) ?: return
        val kinds = FastCalendar.kinds(date, h, if (h.m == 10) Fasting.shawwalDone(h.y) else 0)
        if (kinds.isEmpty()) return
        ensureChannel(ctx)
        val s = Lang.current.strings
        val open = PendingIntent.getActivity(
            ctx, REQ, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(s.fastingRemindTitle(kinds.joinToString(" · ") { s.fastingKinds[it.key] ?: it.key }))
            .setContentText(s.fastingRemindBody)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, n) }
    }
}

class FastReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppSettings.init(context)
        Fasting.ensure()
        val day = intent.getStringExtra("day").orEmpty()
        if (Fasting.state.remind && day.isNotEmpty()) FastReminders.show(context, day)
        FastReminders.schedule(context)
    }
}
