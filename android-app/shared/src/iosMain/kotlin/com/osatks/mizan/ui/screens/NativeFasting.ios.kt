package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.DayMark
import com.osatks.mizan.core.FastCalendar
import com.osatks.mizan.core.Fasting
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.daysBetween
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberNotifyToggle
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.SplitMax
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import com.osatks.mizan.widget.LiveText
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر متابعة الصيام من النظام (iosApp/iosApp/NativeFasting.swift)، على مثال [NativeTasbih]: الحال من [Fasting] JSON
 * مع الإشعار [STATE]، وكل لمس يعود إلى [NativeFastingSession] فيغيّر [Fasting] وتُعاد الحال. العدّ إلى نهاية السحور
 * والإفطار بالثواني في Swift من لحظتي الفجر والمغرب (epoch)، فلا تُعاد الحال كل ثانية.
 */
object NativeFasting {
    const val STATE = "MizanFastingState"

    private var factory: ((NativeFastingSession) -> UIViewController)? = null

    fun attach(make: (NativeFastingSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeFastingSession): UIViewController = factory!!.invoke(session)
}

class NativeFastingSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}
    internal var onRemind: (Boolean) -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeFasting.STATE, this)
    }

    fun back() = onBack()

    /** لمس يوم [day] من رمضان [year]: صيام ← إفطار ← بلا تحديد */
    fun mark(year: Int, day: Int) = Fasting.cycle(year, day)

    fun qadaPlus() = Fasting.qadaPlus()
    fun qadaMinus() = Fasting.qadaMinus()

    /** «قضيت يوماً» */
    fun madeUp() = Fasting.madeUp()

    /** مفتاح صيام تطوع اليوم */
    fun voluntary(on: Boolean) = Fasting.setVoluntaryToday(on)

    /** مفتاح التذكير: التفعيل يطلب إذن الإشعارات أولاً */
    fun remind(on: Boolean) = onRemind(on)
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * متابعة الصيام بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeFasting.swift) بطراز المسبحة والزكاة: بطاقة الصائم
 * بالعدّ إلى نهاية السحور والإفطار، واليوم، وشبكة رمضان، والقضاء وسجله، وأيام التطوع القادمة وتذكيرها — كما في [FastingScreen].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeFastingView(onBack: (() -> Unit)?, modifier: Modifier) {
    val session = remember { NativeFastingSession() }
    val backS by rememberUpdatedState(onBack)
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    // التذكير يطلب إذن الإشعارات أولاً، ثم تُعاد جدولة الإشعارات (الأذان أولاً ثم التذكير في حدود iOS)
    val askNotify = rememberNotifyToggle { ok ->
        Fasting.setRemind(ok)
        PrayerAlerts.reschedule()
        if (!ok) Platform.toast(s.notifyOffTitle)
    }
    DisposableEffect(session) {
        session.onBack = { backS?.invoke() }
        session.onRemind = { on -> if (on) askNotify(true) else { Fasting.setRemind(false); PrayerAlerts.reschedule() } }
        onDispose { session.onBack = {}; session.onRemind = {} }
    }
    remember { Fasting.ensure() }
    StatusBarStyle(lightContent = dark)

    // الحال تُبنى كل دقيقة والصفحة ظاهرة (يتبدل اليوم عند منتصف الليل)؛ العدّ بالثواني في Swift
    var minute by remember { mutableIntStateOf(0) }
    val active = LocalPageActive.current
    LaunchedEffect(active) {
        while (active) {
            val ms = Clock.System.now().toEpochMilliseconds()
            delay(60_000 - ms % 60_000 + 50)
            minute++
        }
    }
    val st = Fasting.state
    val place = AppSettings.place
    val method = AppSettings.method
    val hanafi = AppSettings.hanafi
    val now = remember(minute, active, place) { Fasting.now() }
    val today = now.date
    val offset = Fasting.offset()
    val zone = remember(place) { runCatching { TimeZone.of(place.tz) }.getOrElse { TimeZone.currentSystemDefault() } }
    val t = remember(today, place, method, hanafi) { PrayerUtil.times(today) }
    val h = remember(today, offset) { FastCalendar.hijri(today, offset) }
    val year = remember(today, offset) { FastCalendar.ramadanYear(today, offset) }
    val upcoming = remember(today, offset, st.sunnah) { Fasting.upcoming(8) }
    val fasting = remember(today, st) { Fasting.fastingToday() }
    val shawwal = remember(h, st.sunnah) { h?.takeIf { it.m == 10 }?.let { Fasting.shawwalDone(it.y) } ?: -1 }
    val lt = LiveText.of(lang.code)

    fun epochOf(key: String): Long? = t[key].takeIf { !it.isNaN() }?.let { PrayerUtil.at(today, it).toInstant(zone).epochSeconds }
    fun clockOf(key: String): String = t[key].takeIf { !it.isNaN() }?.let { s.time(it) } ?: "—"

    val hero = if (fasting) linkedMapOf<String, Any?>(
        "label" to s.fastingNow, "suhoorLabel" to lt.suhoor, "iftarLabel" to lt.iftar, "iftarNow" to lt.iftarNow,
        "fajrAt" to epochOf("fajr"), "maghribAt" to epochOf("maghrib"),
        "fajrTime" to clockOf("fajr"), "maghribTime" to clockOf("maghrib"), "place" to s.placeName(place),
    ) else null

    val todaySunnah = upcoming.firstOrNull()?.takeIf { it.date == today }
    val todayCard = linkedMapOf<String, Any?>(
        "title" to s.today, "hijri" to s.hijri(h), "greg" to "${s.weekday(today)} ${s.greg(today)}",
        "kinds" to (todaySunnah?.kinds?.map { s.fastingKinds[it.key] ?: it.key } ?: emptyList<String>()),
        "forbidden" to if (h != null && FastCalendar.forbidden(h)) s.fastingForbiddenToday else null,
        // رمضان يُعلَّم في شبكته، ولا يُصام العيد وأيام التشريق
        "voluntary" to if (h == null || h.m == 9 || FastCalendar.forbidden(h)) null
        else linkedMapOf<String, Any?>("label" to s.fastingVoluntaryToday, "hint" to s.fastingTodayHint, "on" to (today.toString() in st.sunnah)),
    )

    val ramadan = year?.let { y ->
        val length = Hijri.monthLength(y, 9)
        linkedMapOf<String, Any?>(
            "title" to s.hijriMonthTitle(9, y), "hint" to s.fastingRamadanHint, "year" to y,
            "days" to (1..length).map { d ->
                val g = FastCalendar.gregorian(y, 9, d, offset)
                linkedMapOf<String, Any?>(
                    "d" to d, "mark" to st.mark(y, d).code.toString(),
                    "enabled" to (g != null && g <= today), "today" to (g == today), "label" to s.hijriDate(d, 9, y),
                )
            },
            "fasted" to s.fastingFasted, "missed" to s.fastingMissed, "none" to s.fastingUnmarked,
            "tiles" to listOf(
                linkedMapOf("value" to "${st.count(y, DayMark.FASTED, length)}", "label" to s.fastingFasted),
                linkedMapOf("value" to "${st.count(y, DayMark.MISSED, length)}", "label" to s.fastingMissed),
                linkedMapOf("value" to "${st.count(y, DayMark.NONE, length)}", "label" to s.fastingUnmarked),
            ),
        )
    }

    val qada = linkedMapOf<String, Any?>(
        "title" to s.fastingQadaTitle, "owed" to s.fastingQadaOwed,
        "count" to st.qada, "countText" to Fmt.int(st.qada.toLong()), "max" to Fasting.MAX_QADA,
        "madeUp" to s.fastingMadeUp, "history" to s.fastingHistory, "empty" to s.fastingNothingYet,
        "decrease" to s.decrease, "increase" to s.increase,
        "log" to st.qadaLog.take(6).map { e ->
            val label = when (e.kind) {
                "add" -> s.fastingLogAdded
                "done" -> s.fastingLogMadeUp
                "missed" -> s.fastingLogMissed
                else -> s.fastingLogRemoved
            }
            val date = runCatching { LocalDate.parse(e.date) }.getOrNull()
            linkedMapOf("label" to label, "date" to (date?.let { s.greg(it) } ?: ""), "delta" to if (e.delta > 0) "+${e.delta}" else "${e.delta}")
        },
    )

    val sunnah = linkedMapOf<String, Any?>(
        "title" to s.fastingSunnahTitle, "logged" to s.fastingLogged, "loggedValue" to s.daysCount(st.sunnah.size.toLong()),
        "shawwal" to if (shawwal >= 0) s.fastingShawwal(shawwal.coerceAtMost(6)) else null,
        "upcoming" to s.fastingUpcoming,
        "days" to upcoming.map { d ->
            val n = daysBetween(today, d.date)
            linkedMapOf(
                "id" to d.date.toString(),
                "kinds" to d.kinds.joinToString(" · ") { s.fastingKinds[it.key] ?: it.key },
                "date" to "${s.weekday(d.date)} ${s.greg(d.date)} · ${s.hijri(d.hijri)}",
                "rel" to when (n) { 0L -> s.today; 1L -> s.tomorrow; else -> s.inDays(n) },
            )
        },
        "remind" to s.fastingRemind, "remindDesc" to s.fastingRemindDesc(s.time(Fasting.REMIND_HOUR.toDouble())), "remindOn" to st.remind,
    )

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // عمودان على الشاشة العريضة كما في ToolPage (اليوم ورمضان في الأول، والقضاء والتطوع في الثاني)
        val wide = maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "wide" to wide,
                "canBack" to (onBack != null), "back" to s.back,
                "title" to s.fastingTitle, "subtitle" to s.fastingSubtitle,
                "hero" to hero, "today" to todayCard, "ramadan" to ramadan, "qada" to qada, "sunnah" to sunnah,
                "note" to s.eventsNote,
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                // لون أيام الإفطار في الشبكة، كما في FastingScreen
                "missedColor" to hex(Color(0xFFD9694F)),
                "secondary" to hex(cs.secondary),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeFasting.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
