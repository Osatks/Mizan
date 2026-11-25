package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.weekday
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant

/** محوّل التاريخ في iOS بواجهة النظام (SwiftUI): الحالة والحساب كما في شاشة Compose (HijriScreen)، وSwift يرسمها ويعيد اللمس */
@Composable
internal fun IosHijriScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val today = LocalDate.now()
    val th = Hijri.toHijri(today) ?: HDate(1448, 1, 1)
    var dir by remember { mutableStateOf("g2h") }
    var gy by remember { mutableStateOf(today.year.toString()) }
    var gm by remember { mutableIntStateOf(today.monthValue) }
    var gd by remember { mutableIntStateOf(today.dayOfMonth) }
    var hy by remember { mutableStateOf(th.y.toString()) }
    var hm by remember { mutableIntStateOf(th.m) }
    var hd by remember { mutableIntStateOf(th.d) }
    var cal by remember { mutableStateOf(th.y * 12 + (th.m - 1)) }

    val (main, sub) = dateConversion(s, dir, gy, gm, gd, hy, hm, hd)

    var yearRev by remember { mutableIntStateOf(0) }
    val cy = cal / 12
    val cm = cal % 12 + 1
    val first = Hijri.toGregorian(cy, cm, 1)
    val cells = if (first == null) emptyList() else (1..Hijri.monthLength(cy, cm)).map { day ->
        val g = first.plusDays(day - 1)
        linkedMapOf<String, Any?>(
            "n" to day, "d" to "$day", "g" to "${g.dayOfMonth}/${g.monthValue}", "today" to (g == today),
            "label" to "${s.weekday(g)} — ${s.hijri(HDate(cy, cm, day))} — ${s.greg(g)}",
        )
    }
    val fields = if (dir == "g2h") nativeDateFields(false, gy, gm, gd, "g$yearRev") { gd = it }
    else nativeDateFields(true, hy, hm, hd, "h$yearRev") { hd = it }
    NativeDatesView(
        "hijri",
        linkedMapOf(
            "title" to s.hijriTitle, "subtitle" to s.hijriSubtitle,
            "resultLabel" to if (dir == "g2h") s.correspondingHijri else s.correspondingGreg, "main" to main, "sub" to sub,
            "dir" to dir,
            "dirs" to listOf(linkedMapOf("key" to "g2h", "label" to s.gregToHijri), linkedMapOf("key" to "h2g", "label" to s.hijriToGreg)),
            "fields" to fields, "todayLabel" to s.todayDate,
            "calendar" to linkedMapOf(
                "key" to cal, "title" to s.hijriMonthTitle(cm, cy), "prev" to s.prevMonth, "next" to s.nextMonth,
                "prevOk" to (Hijri.toGregorian((cal - 1) / 12, (cal - 1) % 12 + 1, 1) != null),
                "nextOk" to (Hijri.toGregorian((cal + 1) / 12, (cal + 1) % 12 + 1, 1) != null),
                // الأسبوع يبدأ بالأحد كما في التقاويم العربية
                "weekdays" to listOf(6, 0, 1, 2, 3, 4, 5).map { s.weekdaysShort[it] },
                "lead" to (first?.dayOfWeek?.iso?.rem(7) ?: 0), "cells" to cells, "hint" to s.calendarHint,
            ),
            "note" to s.moonNote,
        ),
        com.osatks.mizan.i18n.Evidence.hijri,
        onAction = { a, v ->
            when (a) {
                "dir" -> dir = v
                "day" -> { val n = v.toIntOrNull(); if (n != null && dir == "g2h") gd = n else if (n != null) hd = n }
                "month" -> { val n = v.toIntOrNull(); if (n != null && dir == "g2h") gm = n else if (n != null) hm = n }
                "year" -> if (dir == "g2h") gy = v else hy = v
                "today" -> {
                    gy = today.year.toString(); gm = today.monthValue; gd = today.dayOfMonth
                    hy = th.y.toString(); hm = th.m; hd = th.d
                    yearRev++
                }
                "calendar" -> v.toIntOrNull()?.let { cal += it }
                "pick" -> v.toIntOrNull()?.let { day -> dir = "h2g"; hy = (cal / 12).toString(); hm = cal % 12 + 1; hd = day; yearRev++ }
            }
        },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
}

/** حاسبة العمر في iOS بواجهة النظام (SwiftUI): الحالة والحساب كما في شاشة Compose (AgeScreen) */
@Composable
internal fun IosAgeScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var hijri by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf("") }
    var m by remember { mutableIntStateOf(1) }
    var d by remember { mutableIntStateOf(1) }
    val today = LocalDate.now()

    val birth: LocalDate? = birthDate(hijri, y, m, d)

    var yearRev by remember { mutableIntStateOf(0) }
    val content = linkedMapOf<String, Any?>("title" to s.ageTitle, "subtitle" to s.ageSubtitle)
    if (birth != null && !birth.isAfter(today)) {
        val g = periodBetween(birth, today)
        val hb = Hijri.toHijri(birth)
        val hAge = hijriAge(s, birth, today)
        val days = daysBetween(birth, today)
        var nextB = birth.withYear(today.year)
        if (nextB.isBefore(today)) nextB = nextB.plusYears(1)
        val left = daysBetween(today, nextB)
        content["resultLabel"] = s.ageGregorian
        content["main"] = s.ymd(g.years, g.months, g.days)
        content["sub"] = s.inHijri(hAge)
        content["stats"] = listOf(
            s.birthGreg to s.greg(birth), s.birthHijri to s.hijri(hb), s.birthWeekday to s.weekday(birth),
            s.nextBirthday to if (left == 0L) s.birthdayToday else s.inDays(left),
        ).map { (k, v) -> linkedMapOf("label" to k, "value" to v) }
        content["tiles"] = listOf(Fmt.int(days) to s.tileDays, Fmt.int(days / 7) to s.tileWeeks, Fmt.int(days * 24) to s.tileHours)
            .map { (v, k) -> linkedMapOf("value" to v, "label" to k) }
    } else {
        content["resultLabel"] = s.yourAge
        content["main"] = "—"
        content["sub"] = if (birth != null) s.birthAfterToday else s.enterBirth
    }
    content["birthLabel"] = s.birthDate
    content["mode"] = if (hijri) "h" else "g"
    content["modes"] = listOf(linkedMapOf("key" to "g", "label" to s.gregorian), linkedMapOf("key" to "h", "label" to s.hijri))
    content["fields"] = nativeDateFields(hijri, y, m, d, (if (hijri) "h" else "g") + yearRev) { d = it }
    NativeDatesView(
        "age", content, com.osatks.mizan.i18n.Evidence.age,
        onAction = { a, v ->
            when (a) {
                "mode" -> {
                    val newHijri = v == "h"
                    if (newHijri != hijri && birth != null) {
                        if (newHijri) Hijri.toHijri(birth)?.let { h -> y = h.y.toString(); m = h.m; d = h.d }
                        else { y = birth.year.toString(); m = birth.monthValue; d = birth.dayOfMonth }
                    }
                    hijri = newHijri
                    yearRev++
                }
                "day" -> v.toIntOrNull()?.let { d = it }
                "month" -> v.toIntOrNull()?.let { m = it }
                "year" -> y = v
            }
        },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
}

/** المناسبات في iOS بواجهة النظام (SwiftUI): القائمة والمناسبة المختارة من هنا، والعدّ إليها يتقدم كل ثانية في Swift */
@Composable
internal fun IosEventsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(active) { while (true) { now = LocalDateTime.now(); if (!active) break; delay(60_000L) } }
    val today = now.toLocalDate()
    val upcoming = remember(today) { upcomingEvents(today) }
    var sel by remember {
        val wanted = Pending.event
        Pending.event = null
        mutableIntStateOf(upcoming.indexOfFirst { it.first.key == wanted }.coerceAtLeast(0))
    }

    val hero = if (upcoming.isEmpty()) null else {
        val (e, date, y) = upcoming[sel.coerceIn(upcoming.indices)]
        val text = s.events.getValue(e.key)
        // بداية يوم المناسبة بساعة الجهاز، فالعدّ كما في Compose (durationBetween بين وقتين محليين)
        val offset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now()).totalSeconds
        linkedMapOf<String, Any?>(
            "emoji" to e.emoji, "name" to text.name, "hijri" to s.hijriDate(e.d, e.m, y), "greg" to s.greg(date), "desc" to text.desc,
            "at" to date.atStartOfDay().toInstant(TimeZone.UTC).epochSeconds - offset,
        )
    }
    NativeDatesView(
        "events",
        linkedMapOf(
            "title" to s.eventsTitle, "subtitle" to s.eventsSubtitle, "hero" to hero,
            "units" to listOf(s.unitDays, s.unitHours, s.unitMinutes, s.unitSeconds),
            "selected" to if (upcoming.isEmpty()) 0 else sel.coerceIn(upcoming.indices),
            "items" to upcoming.map { (ev, date2, _) ->
                val days = daysBetween(today, date2)
                linkedMapOf(
                    "key" to ev.key, "emoji" to ev.emoji, "name" to (s.events[ev.key]?.name ?: ev.key),
                    "date" to "${s.weekday(date2)} ${s.greg(date2)}",
                    "left" to when (days) { 0L -> s.today; 1L -> s.tomorrow; else -> s.inDays(days) },
                )
            },
            "note" to s.eventsNote,
        ),
        com.osatks.mizan.i18n.Evidence.events,
        onAction = { a, v -> if (a == "select") v.toIntOrNull()?.let { sel = it } },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
}

/** حقول التاريخ لشاشة النظام (NativeDatesView): أيام الشهر بطوله، وأسماء الأشهر، والسنة؛ واليوم يُقصَر على طول الشهر كما في [DateFields] */
@Composable
private fun nativeDateFields(hijri: Boolean, y: String, m: Int, d: Int, yearKey: String, onD: (Int) -> Unit): Map<String, Any?> {
    val s = LocalStrings.current
    val yi = y.toIntOrNull()
    val maxDay = if (hijri) (if (yi != null && yi in 1300..1600) Hijri.monthLength(yi, m) else 30)
    else (if (yi != null && yi in 1..9999) monthLength(yi, m) else 31)
    LaunchedEffect(hijri, maxDay) { if (d > maxDay) onD(maxDay) }
    return linkedMapOf(
        "dayLabel" to s.dayLabel, "monthLabel" to s.monthLabel, "yearLabel" to s.yearLabel,
        "suffix" to if (hijri) s.hijriYearSuffix else s.gregYearSuffix,
        "days" to (1..maxDay).map { "$it" }, "months" to if (hijri) s.hijriMonths else s.gregMonths,
        "day" to d.coerceAtMost(maxDay), "month" to m, "year" to y,
        // يتغير حين تُكتب السنة من غير لوحة المفاتيح (اليوم، أو يوم من التقويم، أو تبديل التقويم) فيأخذها الحقل
        "yearKey" to yearKey,
    )
}
