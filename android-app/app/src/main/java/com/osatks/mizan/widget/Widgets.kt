package com.osatks.mizan.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.osatks.mizan.MainActivity
import com.osatks.mizan.R
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDate
import kotlin.concurrent.thread

/**
 * أدوات الشاشة الرئيسية الخمس. كل أداة تُرسم في الخلفية (قراءة نص المصحف قد تأخذ لحظة)،
 * وتُحدَّث كل نصف ساعة، وعند دخول وقت كل صلاة وعند منتصف الليل، وعند الخروج من التطبيق.
 */
abstract class MizanWidget : AppWidgetProvider() {
    abstract fun build(ctx: Context, s: Strings, lang: Lang): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val done = goAsync()
        thread(name = "widget") {
            try { Widgets.render(context, this, manager, ids) } finally { done.finish() }
        }
    }

    override fun onEnabled(context: Context) = Widgets.scheduleTick(context)
    override fun onDisabled(context: Context) { if (!Widgets.any(context)) Widgets.cancelTick(context) }
}

object Widgets {
    private val PROVIDERS = listOf(
        NextPrayerWidget::class.java, PrayerTimesWidget::class.java, HijriWidget::class.java,
        KhatmaWidget::class.java, AyahWidget::class.java,
    )
    private const val TICK = 5150

    fun init(ctx: Context) { AppSettings.init(ctx); QuranData.init(ctx) }

    internal fun render(ctx: Context, provider: MizanWidget, manager: AppWidgetManager, ids: IntArray) {
        init(ctx)
        val lang = Lang.current
        val views = runCatching { provider.build(ctx, lang.strings, lang) }.getOrNull() ?: return
        views.setInt(R.id.root, "setLayoutDirection", if (lang.rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR)
        ids.forEach { manager.updateAppWidget(it, views) }
        scheduleTick(ctx)
    }

    fun any(ctx: Context): Boolean {
        val m = AppWidgetManager.getInstance(ctx)
        return PROVIDERS.any { m.getAppWidgetIds(ComponentName(ctx, it)).isNotEmpty() }
    }

    /** تحديث كل الأدوات الموضوعة على الشاشة (بعد تغيير الإعدادات أو التقدم في الختمة) */
    fun updateAll(ctx: Context) {
        val app = ctx.applicationContext
        val m = AppWidgetManager.getInstance(app)
        PROVIDERS.forEach { cls ->
            val ids = m.getAppWidgetIds(ComponentName(app, cls))
            if (ids.isNotEmpty()) app.sendBroadcast(
                Intent(app, cls).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }

    private fun tickIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, TICK, Intent(ctx, WidgetTickReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** منبّه لا يوقظ الجهاز: عند الصلاة القادمة أو منتصف الليل، أيهما أقرب */
    fun scheduleTick(ctx: Context) {
        init(ctx)
        if (!any(ctx)) return
        val zone = PrayerUtil.zone()
        val now = PrayerUtil.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val at = minOf(PrayerUtil.next().at, midnight).plusSeconds(1)
        val millis = at.atZone(zone).toInstant().toEpochMilli()
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = tickIntent(ctx)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExact(AlarmManager.RTC, millis, pi) else am.setWindow(AlarmManager.RTC, millis, 60_000, pi)
    }

    fun cancelTick(ctx: Context) = ctx.getSystemService(AlarmManager::class.java).cancel(tickIntent(ctx))

    /** فتح التطبيق على شاشة معيّنة */
    fun open(ctx: Context, route: String, code: Int): PendingIntent? = PendingIntent.getActivity(
        ctx, code,
        Intent(ctx, MainActivity::class.java).putExtra("route", route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** وقت مختصر يسع خانة صغيرة: بلا ص/م في نظام 12 ساعة */
    fun shortTime(hours: Double) = PrayTimes.format(hours, AppSettings.use24h, "", "").trim()

    fun today(): LocalDate = PrayerUtil.now().toLocalDate()
}

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Widgets.updateAll(context)
        Widgets.scheduleTick(context)
    }
}

/** ١. الصلاة القادمة مع عدّ تنازلي حي */
class NextPrayerWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang) = RemoteViews(ctx.packageName, R.layout.widget_next_prayer).apply {
        val next = PrayerUtil.next()
        val left = Duration.between(PrayerUtil.now(), next.at).toMillis().coerceAtLeast(0)
        setTextViewText(R.id.label, s.nextPrayerIn(s.placeName(AppSettings.place)))
        setTextViewText(R.id.name, s.prayer(next.key))
        setTextViewText(R.id.time, s.time(next.at.toLocalTime().toSecondOfDay() / 3600.0))
        setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + left, null, true)
        setChronometerCountDown(R.id.countdown, true)
        Widgets.open(ctx, "prayer", 1)?.let { setOnClickPendingIntent(R.id.root, it) }
    }
}

/** ٢. مواقيت اليوم الست، والقادمة مميزة */
class PrayerTimesWidget : MizanWidget() {
    private val cells = listOf(R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5)
    private val names = listOf(R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5)
    private val times = listOf(R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5)

    override fun build(ctx: Context, s: Strings, lang: Lang) = RemoteViews(ctx.packageName, R.layout.widget_prayer_times).apply {
        val today = Widgets.today()
        val t = PrayerUtil.times(today)
        val next = PrayerUtil.next()
        val nextKey = if (next.at.toLocalDate() == today) next.key else "fajr"
        setTextViewText(R.id.city, s.placeName(AppSettings.place))
        setTextViewText(R.id.date, Hijri.toHijri(today)?.let { h -> s.hijriDate(h.d, h.m, h.y) } ?: s.greg(today))
        PrayTimes.PRAYERS.forEachIndexed { i, key ->
            setTextViewText(names[i], s.prayer(key))
            setTextViewText(times[i], Widgets.shortTime(t[key]))
            setInt(cells[i], "setBackgroundResource", if (key == nextKey) R.drawable.widget_pill else 0)
        }
        Widgets.open(ctx, "prayer", 2)?.let { setOnClickPendingIntent(R.id.root, it) }
    }
}

/** ٣. التاريخ الهجري واليوم والتاريخ الميلادي */
class HijriWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang) = RemoteViews(ctx.packageName, R.layout.widget_hijri).apply {
        val today = Widgets.today()
        val h = Hijri.toHijri(today)
        setTextViewText(R.id.day, h?.d?.toString() ?: "—")
        setTextViewText(R.id.hijri, h?.let { s.hijriMonthTitle(it.m, it.y) } ?: "—")
        setTextViewText(R.id.greg, "${s.weekday(today)} · ${s.greg(today)}")
        Widgets.open(ctx, "hijri", 3)?.let { setOnClickPendingIntent(R.id.root, it) }
    }
}

/** ٤. الختمة: نسبة الإنجاز، وموضع المتابعة، وورد اليوم */
class KhatmaWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang) = RemoteViews(ctx.packageName, R.layout.widget_khatma).apply {
        QuranProgress.ensure()
        val page = QuranProgress.nextUnread()
        val (sn, an) = QuranMeta.locate(QuranMeta.PAGES[page - 1])
        val read = QuranProgress.readCount
        setTextViewText(R.id.title, s.khatmaTitle)
        setTextViewText(R.id.percent, "${read * 100 / QuranProgress.PAGES}%")
        setTextViewText(R.id.surah, if (QuranProgress.finished) s.khatmaDone else "${s.continueReading} · ${s.surahAyah(s.surahName(sn), an)}")
        setProgressBar(R.id.progress, QuranProgress.PAGES, read, false)
        val goal = QuranProgress.dailyGoal()
        setTextViewText(
            R.id.today,
            if (goal > 0) "${s.todayPortion}: ${QuranProgress.todayCount.coerceAtMost(goal)} / $goal" else s.pagesRead(read, QuranProgress.PAGES)
        )
        Widgets.open(ctx, surahRoute(sn, an, "widget"), 4)?.let { setOnClickPendingIntent(R.id.root, it) }
    }
}

/** ٥. آية اليوم: تتغير كل يوم، ومعها ترجمة معناها بلغة الواجهة */
class AyahWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang) = RemoteViews(ctx.packageName, R.layout.widget_ayah).apply {
        val text = runBlocking { QuranData.uthmani() }
        val i = ayahOfDay(text, Widgets.today())
        val (sn, an) = QuranMeta.locate(i)
        setTextViewText(R.id.title, s.ayahOfDay)
        setTextViewText(R.id.ayah, "﴿${QuranData.withoutBismillah(text, i)}﴾")
        val tr = Sources.translationFor(lang)
        if (tr != null) {
            setTextViewText(R.id.meaning, runBlocking { QuranData.source(tr.id) }[i])
            setViewVisibility(R.id.meaning, View.VISIBLE)
        } else setViewVisibility(R.id.meaning, View.GONE)
        setTextViewText(R.id.ref, s.surahAyah(s.surahName(sn), an))
        Widgets.open(ctx, surahRoute(sn, an, "widget"), 5)?.let { setOnClickPendingIntent(R.id.root, it) }
    }

    companion object {
        /** آية قصيرة (تسع الأداة) تُختار بثبات لكل يوم */
        fun ayahOfDay(text: Array<String>, day: LocalDate): Int {
            val short = text.indices.filter { QuranData.withoutBismillah(text, it).length in 40..160 }
            return short[Math.floorMod(day.toEpochDay() * 7919L, short.size.toLong()).toInt()]
        }
    }
}
