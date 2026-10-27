package com.osatks.mizan.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.osatks.mizan.MainActivity
import com.osatks.mizan.R
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.platform.init
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.atStartOfDay
import com.osatks.mizan.core.durationBetween
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.plusSeconds
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
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.concurrent.thread

/**
 * أدوات الشاشة الرئيسية الست. كلها بتصميم واحد: سطح بلون التطبيق (وأخضر قاتم في الوضع الداكن) بتدرج ولمعة ذهبية
 * وزخرفة نجمة ثمانية، ونص أبيض وذهبي، وزوايا بقطر النظام في أندرويد ١٢ فما بعد.
 * تبقى RemoteViews (لا Glance): العدّ التنازلي الحي (Chronometer) لا يتاح في Glance، ولا حاجة لمكتبة إضافية.
 *
 * التحديث: منبّه لا يوقظ الجهاز عند دخول كل صلاة ومنتصف الليل (كل الأدوات)، ومنبّه آخر خفيف لأداتي الأذكار
 * والصلاة القادمة عند حدود نوافذ الأذكار وكل عشرين دقيقة (تدوير الذكر وشريط الوقت)، وعند الخروج من التطبيق.
 */
abstract class MizanWidget : AppWidgetProvider() {
    /** القياسات التي يتغير عندها التصميم (بالـ dp) ورقم التصميم لكل منها؛ أندرويد ١٢ يختار بينها وحده */
    open val sizes: List<Pair<SizeF, Int>> = emptyList()
    open val defaultVariant: Int = 0

    abstract fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val done = goAsync()
        thread(name = "widget") {
            try { Widgets.render(context, this, manager, ids) } finally { done.finish() }
        }
    }

    // تغيّر حجم الأداة: قبل أندرويد ١٢ نختار التصميم بأنفسنا
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) {
        if (sizes.size < 2) return
        onUpdate(context, manager, intArrayOf(id))
    }

    override fun onEnabled(context: Context) = Widgets.scheduleTick(context)
    override fun onDisabled(context: Context) { if (!Widgets.any(context)) Widgets.cancelTick(context) }
}

object Widgets {
    private val PROVIDERS = listOf(
        NextPrayerWidget::class.java, PrayerTimesWidget::class.java, HijriWidget::class.java,
        KhatmaWidget::class.java, AyahWidget::class.java, AdhkarWidget::class.java,
    )
    /** ما يتغير خلال الوقت نفسه: الذكر المعروض وشريط ما مضى من وقت الصلاة */
    private val LIVE = listOf(AdhkarWidget::class.java, NextPrayerWidget::class.java)
    private const val TICK = 5150
    private const val TICK_LIVE = 5151
    private const val EXTRA_LIVE = "live"

    fun init(ctx: Context) { AppSettings.init(ctx) }

    internal fun render(ctx: Context, provider: MizanWidget, manager: AppWidgetManager, ids: IntArray) {
        init(ctx)
        val lang = Lang.current
        val s = lang.strings
        for (id in ids) {
            val views = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && provider.sizes.size > 1)
                    RemoteViews(provider.sizes.associate { (size, v) -> size to decorate(ctx, provider.build(ctx, s, lang, v, id), lang) })
                else decorate(ctx, provider.build(ctx, s, lang, variantFor(manager, id, provider), id), lang)
            }.getOrNull() ?: continue
            manager.updateAppWidget(id, views)
        }
        scheduleTick(ctx)
    }

    /** قبل أندرويد ١٢: أكبر تصميم يسعه حجم الأداة الحالي */
    private fun variantFor(manager: AppWidgetManager, id: Int, p: MizanWidget): Int {
        if (p.sizes.size < 2) return p.defaultVariant
        val o = manager.getAppWidgetOptions(id)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (w <= 0 || h <= 0) return p.defaultVariant
        return p.sizes.filter { (sz, _) -> sz.width <= w && sz.height <= h }.maxByOrNull { (sz, _) -> sz.width * sz.height }?.second
            ?: p.sizes.first().second
    }

    /** اتجاه اللغة، ولون السطح بلون التطبيق؛ في «تلقائي» يتبع الوضع الداكن للنظام */
    private fun decorate(ctx: Context, v: RemoteViews, lang: Lang): RemoteViews {
        v.setInt(R.id.root, "setLayoutDirection", if (lang.rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR)
        val c = WidgetColors.of(AppSettings.accent)
        when (AppSettings.theme) {
            "dark" -> v.setInt(R.id.bg, "setColorFilter", c.night1)
            "light" -> v.setInt(R.id.bg, "setColorFilter", c.hero1)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) v.setColorInt(R.id.bg, "setColorFilter", c.hero1, c.night1)
            else v.setInt(R.id.bg, "setColorFilter", if (night(ctx)) c.night1 else c.hero1)
        }
        return v
    }

    private fun night(ctx: Context) =
        (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun ids(ctx: Context, cls: Class<*>) = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, cls))

    fun any(ctx: Context): Boolean = PROVIDERS.any { ids(ctx, it).isNotEmpty() }
    private fun anyLive(ctx: Context): Boolean = LIVE.any { ids(ctx, it).isNotEmpty() }

    /** تحديث كل الأدوات الموضوعة على الشاشة (بعد تغيير الإعدادات أو التقدم في الختمة) */
    fun updateAll(ctx: Context) = update(ctx, PROVIDERS)

    fun update(ctx: Context, classes: List<Class<out MizanWidget>>) {
        val app = ctx.applicationContext
        classes.forEach { cls ->
            val ids = ids(app, cls)
            if (ids.isNotEmpty()) app.sendBroadcast(
                Intent(app, cls).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }

    private fun tickIntent(ctx: Context, live: Boolean) = PendingIntent.getBroadcast(
        ctx, if (live) TICK_LIVE else TICK, Intent(ctx, WidgetTickReceiver::class.java).putExtra(EXTRA_LIVE, live),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun millis(at: LocalDateTime) = at.toInstant(WidgetData.zone()).toEpochMilliseconds()

    /**
     * منبّهان لا يوقظان الجهاز (RTC): الأول عند الصلاة القادمة أو منتصف الليل، أيهما أقرب.
     * والثاني (إن وُضعت أداة الأذكار أو الصلاة القادمة) عند نهاية نافذة الأذكار أو تدوير الذكر التالي، بنافذة دقيقة.
     */
    fun scheduleTick(ctx: Context) {
        init(ctx)
        if (!any(ctx)) return
        val now = PrayerUtil.now()
        val midnight = now.date.plusDays(1).atStartOfDay()
        val at = minOf(PrayerUtil.next().at, midnight).plusSeconds(1)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = tickIntent(ctx, false)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExact(AlarmManager.RTC, millis(at), pi) else am.setWindow(AlarmManager.RTC, millis(at), 60_000, pi)

        if (anyLive(ctx)) {
            val w = WidgetData.window(now)
            val steps = durationBetween(w.start, now).inWholeMinutes / AdhkarClock.ROTATE_MIN + 1
            val rotate = w.start.plusSeconds(steps * AdhkarClock.ROTATE_MIN * 60)
            am.setWindow(AlarmManager.RTC, millis(minOf(w.end, rotate).plusSeconds(1)), 60_000, tickIntent(ctx, true))
        } else am.cancel(tickIntent(ctx, true))
    }

    fun cancelTick(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(tickIntent(ctx, false)); am.cancel(tickIntent(ctx, true))
    }

    internal fun onTick(ctx: Context, live: Boolean) {
        if (live) update(ctx, LIVE) else updateAll(ctx)
    }

    /** فتح التطبيق على شاشة معيّنة (وقسم الأذكار المناسب للوقت) */
    fun open(ctx: Context, route: String, code: Int, section: String? = null): PendingIntent? = PendingIntent.getActivity(
        ctx, code,
        Intent(ctx, MainActivity::class.java).putExtra("route", route).apply { if (section != null) putExtra("section", section) }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** وقت مختصر يسع خانة صغيرة: بلا ص/م في نظام 12 ساعة */
    fun shortTime(hours: Double) = PrayTimes.format(hours, AppSettings.use24h, "", "").trim()

    fun today(): LocalDate = PrayerUtil.now().date

    fun hours(t: LocalDateTime) = t.time.toSecondOfDay() / 3600.0

    /** الصلاة التي دخل وقتها آخراً (لشريط ما مضى من الوقت) */
    fun previousPrayer(now: LocalDateTime): LocalDateTime {
        for (offset in 0 downTo -1) {
            val day = now.date.plusDays(offset)
            val t = PrayerUtil.times(day)
            for (key in PrayTimes.PRAYERS.reversed()) {
                if (key == "sunrise" || t[key].isNaN()) continue
                val dt = PrayerUtil.at(day, t[key])
                if (dt <= now) return dt
            }
        }
        return now.plusSeconds(-3600)
    }

    fun gold(ctx: Context) = ctx.getColor(R.color.w_gold)
    fun text2(ctx: Context) = ctx.getColor(R.color.w_text2)
}

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Widgets.onTick(context, intent.getBooleanExtra("live", false))
        Widgets.scheduleTick(context)
    }
}

/** ١. الصلاة القادمة مع عدّ تنازلي حي؛ بعرض أربع خانات تظهر معها صلوات اليوم الخمس */
class NextPrayerWidget : MizanWidget() {
    override val sizes = listOf(SizeF(110f, 110f) to 0, SizeF(250f, 110f) to 1)

    private val rows = listOf(R.id.r0, R.id.r1, R.id.r2, R.id.r3, R.id.r4)
    private val rowNames = listOf(R.id.rn0, R.id.rn1, R.id.rn2, R.id.rn3, R.id.rn4)
    private val rowTimes = listOf(R.id.rt0, R.id.rt1, R.id.rt2, R.id.rt3, R.id.rt4)

    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int) =
        RemoteViews(ctx.packageName, if (variant == 1) R.layout.widget_next_prayer_wide else R.layout.widget_next_prayer).apply {
            val now = PrayerUtil.now()
            val next = PrayerUtil.next()
            val left = durationBetween(now, next.at).inWholeMilliseconds.coerceAtLeast(0)
            val prev = Widgets.previousPrayer(now)
            val span = durationBetween(prev, next.at).inWholeSeconds.coerceAtLeast(1)
            val wt = WidgetText.of(lang.code)
            setTextViewText(R.id.label, wt.nextPrayer)
            setTextViewText(R.id.name, s.prayer(next.key))
            setTextViewText(R.id.time, s.time(Widgets.hours(next.at)))
            setTextViewText(R.id.city, s.placeName(AppSettings.place))
            setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + left, null, true)
            setChronometerCountDown(R.id.countdown, true)
            setProgressBar(R.id.progress, 1000, (durationBetween(prev, now).inWholeSeconds * 1000 / span).toInt().coerceIn(0, 1000), false)
            if (variant == 1) {
                val day = if (next.at.date == now.date) now.date else next.at.date
                val t = PrayerUtil.times(day)
                PrayTimes.PRAYERS.filter { it != "sunrise" }.forEachIndexed { i, key ->
                    val on = key == next.key
                    setTextViewText(rowNames[i], s.prayer(key))
                    setTextViewText(rowTimes[i], Widgets.shortTime(t[key]))
                    setInt(rows[i], "setBackgroundResource", if (on) R.drawable.widget_cell_on else 0)
                    setTextColor(rowNames[i], if (on) Widgets.gold(ctx) else Widgets.text2(ctx))
                }
            }
            Widgets.open(ctx, "prayer", 1)?.let { setOnClickPendingIntent(R.id.root, it) }
        }
}

/** ٢. مواقيت اليوم الست في خانات، والقادمة ذهبية، وتحتها عدّ تنازلي لها؛ بارتفاع خانة تبقى الخانات وحدها */
class PrayerTimesWidget : MizanWidget() {
    override val sizes = listOf(SizeF(180f, 40f) to 1, SizeF(180f, 110f) to 0)

    private val cells = listOf(R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5)
    private val names = listOf(R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5)
    private val times = listOf(R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5)

    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int) =
        RemoteViews(ctx.packageName, if (variant == 1) R.layout.widget_prayer_times_compact else R.layout.widget_prayer_times).apply {
            val now = PrayerUtil.now()
            val today = now.date
            val t = PrayerUtil.times(today)
            val next = PrayerUtil.next()
            val nextKey = if (next.at.date == today) next.key else "fajr"
            PrayTimes.PRAYERS.forEachIndexed { i, key ->
                val on = key == nextKey
                setTextViewText(names[i], s.prayer(key))
                setTextViewText(times[i], Widgets.shortTime(t[key]))
                setInt(cells[i], "setBackgroundResource", if (on) R.drawable.widget_cell_on else R.drawable.widget_cell)
                setTextColor(names[i], if (on) Widgets.gold(ctx) else Widgets.text2(ctx))
            }
            if (variant == 0) {
                setTextViewText(R.id.city, s.placeName(AppSettings.place))
                setTextViewText(R.id.date, Hijri.toHijri(today)?.let { h -> s.hijriDate(h.d, h.m, h.y) } ?: s.greg(today))
                setTextViewText(R.id.nextLabel, "${s.prayer(next.key)} ${WidgetText.of(lang.code).inTime}")
                val left = durationBetween(now, next.at).inWholeMilliseconds.coerceAtLeast(0)
                setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + left, null, true)
                setChronometerCountDown(R.id.countdown, true)
            }
            Widgets.open(ctx, "prayer", 2)?.let { setOnClickPendingIntent(R.id.root, it) }
        }
}

/** ٣. التاريخ الهجري: رقم اليوم كبيراً، والشهر والسنة، واليوم والتاريخ الميلادي */
class HijriWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int) = RemoteViews(ctx.packageName, R.layout.widget_hijri).apply {
        val today = Widgets.today()
        val h = Hijri.toHijri(today)
        setTextViewText(R.id.weekday, s.weekday(today))
        setTextViewText(R.id.day, h?.d?.toString() ?: "—")
        setTextViewText(R.id.hijri, h?.let { s.hijriMonthTitle(it.m, it.y) } ?: "—")
        setTextViewText(R.id.greg, s.greg(today))
        Widgets.open(ctx, "hijri", 3)?.let { setOnClickPendingIntent(R.id.root, it) }
    }
}

/** ٤. الختمة: نسبة الإنجاز، وموضع المتابعة، وورد اليوم */
class KhatmaWidget : MizanWidget() {
    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int) = RemoteViews(ctx.packageName, R.layout.widget_khatma).apply {
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
    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int) = RemoteViews(ctx.packageName, R.layout.widget_ayah).apply {
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
}

/**
 * ٦. أذكار الوقت: الأذكار المناسبة لهذه اللحظة من مواقيت الصلاة (انظر AdhkarClock)، ذكراً ذكراً.
 * ينتقل الذكر وحده كل عشرين دقيقة، وزر «التالي» يقدّمه؛ ولمس الأداة يفتح قسم الأذكار نفسه في التطبيق.
 * الصغيرة تعرض الأذكار القصيرة فقط، والكبيرة تضيف الترجمة الأطول والفضل ووقت انتهاء النافذة.
 */
class AdhkarWidget : MizanWidget() {
    override val sizes = listOf(SizeF(110f, 110f) to SMALL, SizeF(250f, 110f) to MEDIUM, SizeF(250f, 250f) to LARGE)
    override val defaultVariant = MEDIUM

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NEXT) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val done = goAsync()
            thread(name = "widget") {
                try {
                    Widgets.init(context)
                    val w = WidgetData.window()
                    setOffset(context, id, w.key, offset(context, id, w.key) + 1)
                    Widgets.render(context, this, AppWidgetManager.getInstance(context), intArrayOf(id))
                } finally { done.finish() }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val e = prefs(context).edit()
        appWidgetIds.forEach { e.remove("adhkar_$it") }
        e.apply()
    }

    override fun build(ctx: Context, s: Strings, lang: Lang, variant: Int, id: Int): RemoteViews {
        val layout = when (variant) { SMALL -> R.layout.widget_adhkar_small; LARGE -> R.layout.widget_adhkar_large; else -> R.layout.widget_adhkar }
        return RemoteViews(ctx.packageName, layout).apply {
            val now = PrayerUtil.now()
            val w = WidgetData.window(now)
            val wt = WidgetText.of(lang.code)
            val all = WidgetData.dhikrs(w, lang)
            val cap = when (variant) { SMALL -> 140; MEDIUM -> 330; else -> Int.MAX_VALUE }
            val list = all.filter { it.ar.length <= cap }.ifEmpty { all }
            val i = AdhkarClock.index(now, w, offset(ctx, id, w.key), list.size)
            val d = list.getOrNull(i)

            setImageViewResource(R.id.icon, icon(w.kind))
            setTextViewText(R.id.title, WidgetData.title(w, lang, wt, Adhkar.tr(lang)))
            val chip = WidgetData.chip(w, s, wt)
            setTextViewText(R.id.chip, chip ?: "")
            setViewVisibility(R.id.chip, if (chip != null && variant != SMALL) View.VISIBLE else View.GONE)
            setTextViewText(R.id.text, d?.ar ?: "")
            setTextViewText(R.id.count, d?.countText ?: "")
            setViewVisibility(R.id.count, if (d?.countText != null) View.VISIBLE else View.GONE)
            if (variant != SMALL) {
                setTextViewText(R.id.pos, "${i + 1}/${list.size}")
                setTextViewText(R.id.src, d?.source ?: "")
                setTextViewText(R.id.tr, d?.tr ?: "")
                setViewVisibility(R.id.tr, if (d?.tr != null) View.VISIBLE else View.GONE)
            }
            if (variant == LARGE) {
                setTextViewText(R.id.fadl, d?.fadl ?: "")
                setViewVisibility(R.id.fadl, if (d?.fadl != null) View.VISIBLE else View.GONE)
                setTextViewText(R.id.until, wt.untilTime(WidgetData.clock(s, w.end)))
            }
            setContentDescription(R.id.next, wt.next)
            setOnClickPendingIntent(R.id.next, nextIntent(ctx, id))
            Widgets.open(ctx, "adhkar", 6, w.section)?.let { setOnClickPendingIntent(R.id.root, it) }
        }
    }

    private fun nextIntent(ctx: Context, id: Int) = PendingIntent.getBroadcast(
        ctx, 1000 + id,
        Intent(ctx, AdhkarWidget::class.java).setAction(NEXT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val SMALL = 0
        const val MEDIUM = 1
        const val LARGE = 2
        const val NEXT = "com.osatks.mizan.widget.NEXT_DHIKR"

        private fun prefs(ctx: Context) = ctx.getSharedPreferences("mizan_widgets", Context.MODE_PRIVATE)

        /** ما تقدّم به المستخدم في النافذة الحالية؛ يعود صفراً حين تتغير النافذة */
        fun offset(ctx: Context, id: Int, key: String): Int {
            val v = prefs(ctx).getString("adhkar_$id", null) ?: return 0
            return if (v.substringBeforeLast('|') == key) v.substringAfterLast('|').toIntOrNull() ?: 0 else 0
        }

        fun setOffset(ctx: Context, id: Int, key: String, n: Int) = prefs(ctx).edit().putString("adhkar_$id", "$key|$n").apply()

        fun icon(kind: String) = when (kind) {
            "adhan", "afterPrayer" -> R.drawable.ic_w_mosque
            "morning" -> R.drawable.ic_w_sun
            "evening" -> R.drawable.ic_w_sunset
            "night", "lastThird" -> R.drawable.ic_w_moon
            else -> R.drawable.ic_w_star
        }
    }
}
