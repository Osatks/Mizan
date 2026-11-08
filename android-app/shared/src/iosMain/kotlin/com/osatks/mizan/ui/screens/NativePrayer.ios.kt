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
import androidx.compose.runtime.produceState
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
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.country
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalPageActive
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
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر شاشة المواقيت من النظام (iosApp/iosApp/NativePrayer.swift)، على مثال [NativeHome]: Swift يسجّل ما ينشئ متحكمها،
 * وKotlin يرسل الحالة JSON مع الإشعار [STATE] كل دقيقة وعند كل تغيّر، والعدّادات وبوصلة القبلة تتقدم في Swift.
 * وSwift يخبره باللمس: تقليب اليوم، وتحديد الموقع، واختيار المدينة، وطريقة الحساب والعصر والإقامة، والتنبيهات، وفتح مسار.
 */
object NativePrayer {
    const val STATE = "MizanPrayerState"

    private var factory: ((NativePrayerSession) -> UIViewController)? = null

    fun attach(make: (NativePrayerSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativePrayerSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativePrayerAvailable: Boolean get() = NativePrayer.available

class NativePrayerSession internal constructor() {
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onLocate: () -> Unit = {}
    internal var onPickCity: () -> Unit = {}
    internal var onNotify: (Boolean) -> Unit = {}
    internal var onDay: (Int) -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativePrayer.STATE, this)
    }

    fun open(route: String) = onOpen(route)

    /** اليوم المعروض: [delta] أيام عن الحالي، و0 يعيده إلى اليوم */
    fun day(delta: Int) = onDay(delta)

    fun locate() = onLocate()
    fun pickCity() = onPickCity()
    fun notify(on: Boolean) = onNotify(on)

    fun method(key: String) {
        AppSettings.updateMethod(key)
        PrayerAlerts.reschedule()
    }

    fun hanafi(on: Boolean) {
        AppSettings.updateHanafi(on)
        PrayerAlerts.reschedule()
    }

    fun iqama(key: String, minutes: Int) = AppSettings.updateIqama(key, minutes.coerceIn(0, 90))
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

private val FIVE = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativePrayerView(
    open: (String) -> Unit, locating: Boolean, onLocate: () -> Unit, onPickCity: () -> Unit, onNotify: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativePrayerSession() }
    val openS by rememberUpdatedState(open)
    val locateS by rememberUpdatedState(onLocate)
    val cityS by rememberUpdatedState(onPickCity)
    val notifyS by rememberUpdatedState(onNotify)
    // اليوم المعروض أياماً عن اليوم (يبقى بعد تبديل التبويب)
    var offset by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(session) {
        session.onOpen = { openS(it) }
        session.onLocate = { locateS() }
        session.onPickCity = { cityS() }
        session.onNotify = { notifyS(it) }
        session.onDay = { d -> offset = if (d == 0) 0 else offset + d }
        onDispose { session.onOpen = {}; session.onLocate = {}; session.onPickCity = {}; session.onNotify = {}; session.onDay = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)

    var minute by remember { mutableIntStateOf(0) }
    val active = LocalPageActive.current
    LaunchedEffect(active) {
        while (active) {
            minute++
            delay(60_000 - Clock.System.now().toEpochMilliseconds() % 60_000 + 50)
        }
    }
    val place = AppSettings.place
    val now = remember(minute, place) { PrayerUtil.now() }
    val today = now.date
    val next = remember(minute, place, AppSettings.method, AppSettings.hanafi) { PrayerUtil.next() }
    val day = today.plus(DatePeriod(days = offset))
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == com.osatks.mizan.i18n.Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, TextColumnMax, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp

        val nowEpoch = Clock.System.now().epochSeconds
        val secNow = now.time.toSecondOfDay().toLong()
        val t = PrayerUtil.times(day)
        val isToday = offset == 0
        val rows = PrayTimes.PRAYERS.filter { !t[it].isNaN() }.map { k ->
            val end = t.end(k)
            val iq = AppSettings.iqama[k] ?: 0
            linkedMapOf<String, Any?>(
                "key" to k, "name" to s.prayer(k), "time" to s.time(t[k]),
                "end" to if (!end.isNaN()) s.endsAt(s.time(end)) else null,
                "iqama" to if (iq > 0) s.iqamaAt(s.time(t[k] + iq / 60.0)) else null,
                "start" to nowEpoch - secNow + (t[k] * 3600).toLong(),
            )
        }
        val first = kotlinx.datetime.LocalDate(day.year, day.monthNumber, 1)
        val length = (1..31).count { runCatching { kotlinx.datetime.LocalDate(day.year, day.monthNumber, it) }.isSuccess }
        val month = (0 until length).map { i ->
            val d = first.plusDays(i)
            val mt = PrayerUtil.times(d)
            linkedMapOf<String, Any?>("day" to d.dayOfMonth.toString(), "times" to FIVE.map { PrayTimes.format(mt[it], true) }, "today" to (d == today))
        }
        val qibla = PrayTimes.qibla(place.lat, place.lng)
        val dirs = s.directions[(kotlin.math.round(qibla / 45.0).toInt()) % 8]
        val city = place.city
        val proofs = com.osatks.mizan.i18n.Evidence.prayer.map { p ->
            linkedMapOf<String, Any?>(
                "kind" to p.kind.name.lowercase(),
                "title" to p.title[lang], "body" to p.body[lang], "source" to p.source[lang],
                "ayah" to p.ayah?.let { (sn, from, to) ->
                    quran?.let { q -> "﴿" + (from..to).joinToString(" ") { a -> QuranData.withoutBismillah(q, QuranMeta.index(sn, a)) + " (" + arabicDigits(a) + ")" } + "﴾" }
                },
                "tr" to p.ayah?.let { (sn, from, to) -> trText?.let { tr -> (from..to).joinToString(" ") { a -> tr[QuranMeta.index(sn, a)] } } },
                "hadith" to p.hadith,
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "lang" to lang.code, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "title" to s.prayerTitle,
                "subtitle" to s.placeName(place) + (city?.let { " · " + s.country(it.cc, lang) } ?: ""),
                "next" to linkedMapOf(
                    "key" to next.key, "label" to s.nextPrayerLabel(s.prayer(next.key)),
                    "at" to nowEpoch + next.remaining.inWholeSeconds,
                    "atText" to s.atTime(s.time(next.at.hour + next.at.minute / 60.0)),
                    "today" to (next.at.date == today),
                ),
                "day" to linkedMapOf(
                    "offset" to offset, "isToday" to isToday,
                    "title" to "${s.weekday(day)} ${s.greg(day)}", "hijri" to s.hijri(Hijri.toHijri(day)),
                    "rows" to rows,
                ),
                "sinceAdhan" to ht.sinceAdhan, "endsNote" to s.prayerEndsNote, "todayLabel" to s.today,
                "monthLabel" to s.monthTable, "hideMonthLabel" to s.hideMonthTable, "prevDay" to s.prevDay, "nextDay" to s.nextDay,
                "month" to linkedMapOf(
                    "title" to s.monthTableTitle(day.monthNumber, day.year),
                    "headers" to listOf(s.colDay) + FIVE.map { s.prayer(it) },
                    "rows" to month,
                ),
                "qibla" to linkedMapOf(
                    "bearing" to qibla, "title" to s.qiblaTitle, "facing" to s.facingQibla,
                    "info" to "${Fmt.num(qibla, 0)}° · ${s.towards(dirs)} · ${Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0)} ${s.unitKm}",
                ),
                "location" to linkedMapOf(
                    "title" to s.locationAndMethod, "useMyLocation" to s.useMyLocation, "locatingLabel" to s.locating,
                    "locating" to locating, "orPickCity" to s.orPickCity,
                    "place" to if (place.gps) "📍 ${s.placeName(place)}" else s.placeName(place),
                    "methodLabel" to s.calcMethod, "method" to AppSettings.method,
                    "methods" to PrayTimes.METHODS.map { linkedMapOf("key" to it.key, "name" to (s.methods[it.key] ?: it.key)) },
                    "asrLabel" to s.asrMethod, "asrStd" to s.asrStandard, "asrHanafi" to s.asrHanafi, "hanafi" to AppSettings.hanafi,
                    "latLngLabel" to s.latLng, "latLng" to "${Fmt.num(place.lat)}° / ${Fmt.num(place.lng)}°",
                ),
                "iqama" to linkedMapOf(
                    "title" to s.iqamaTitle, "desc" to s.iqamaDesc,
                    "items" to FIVE.map { k -> linkedMapOf("key" to k, "name" to s.prayer(k), "min" to (AppSettings.iqama[k] ?: 0)) },
                ),
                "alerts" to linkedMapOf(
                    "title" to s.adhanAlert, "desc" to s.adhanAlertDesc, "on" to AppSettings.notify,
                    "settings" to s.adhanSettings, "settingsSub" to s.adhanSettingsSub, "note" to s.timesMayDiffer,
                ),
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to proofs,
                ),
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativePrayer.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
