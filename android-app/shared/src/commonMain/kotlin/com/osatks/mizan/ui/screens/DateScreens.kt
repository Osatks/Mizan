package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Tile
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** حقول اختيار تاريخ (يوم/شهر/سنة) لأي تقويم */
@Composable
private fun DateFields(hijri: Boolean, y: String, m: Int, d: Int, onY: (String) -> Unit, onM: (Int) -> Unit, onD: (Int) -> Unit) {
    val s = LocalStrings.current
    val yi = y.toIntOrNull()
    val maxDay = if (hijri) (if (yi != null && yi in 1300..1600) Hijri.monthLength(yi, m) else 30)
    else (if (yi != null && yi in 1..9999) monthLength(yi, m) else 31)
    LaunchedEffect(maxDay) { if (d > maxDay) onD(maxDay) }
    val months = if (hijri) s.hijriMonths else s.gregMonths
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Picker(s.dayLabel, (1..maxDay).toList(), d.coerceAtMost(maxDay), text = { "$it" }, onSelect = onD, modifier = Modifier.weight(0.8f))
        Picker(s.monthLabel, (1..12).toList(), m, text = { months[it - 1] }, onSelect = onM, modifier = Modifier.weight(1.4f))
    }
    NumField(s.yearLabel, y, onY, if (hijri) s.hijriYearSuffix else s.gregYearSuffix)
}

@Composable
fun HijriScreen(onBack: () -> Unit) {
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

    val (main, sub) = if (dir == "g2h") {
        val y = gy.toIntOrNull()
        val g = if (y != null && y in 1882..2174) runCatching { LocalDate.of(y, gm, gd.coerceAtMost(monthLength(y, gm))) }.getOrNull() else null
        val h = g?.let { Hijri.toHijri(it) }
        if (g != null && h != null) s.hijri(h) to "${s.weekday(g)} — ${s.greg(g)}" else "—" to s.enterGregYear
    } else {
        val y = hy.toIntOrNull()
        val g = if (y != null) Hijri.toGregorian(y, hm, hd) else null
        if (g != null && y != null) s.greg(g) to "${s.weekday(g)} — ${s.hijri(HDate(y, hm, hd))}" else "—" to s.enterHijriYear
    }

    ToolPage(s.hijriTitle, s.hijriSubtitle, onBack, sharedKey = "hijri", icon = TOOL_ICONS["hijri"]) {
        item { ResultCard(if (dir == "g2h") s.correspondingHijri else s.correspondingGreg, main, sub) }
        item {
            SectionCard {
                Segmented(listOf("g2h" to s.gregToHijri, "h2g" to s.hijriToGreg), dir) { dir = it }
                if (dir == "g2h") DateFields(false, gy, gm, gd, { gy = it }, { gm = it }, { gd = it })
                else DateFields(true, hy, hm, hd, { hy = it }, { hm = it }, { hd = it })
                TextButton(onClick = {
                    gy = today.year.toString(); gm = today.monthValue; gd = today.dayOfMonth
                    hy = th.y.toString(); hm = th.m; hd = th.d
                }) { Text(s.todayDate) }
            }
        }
        item {
            SectionCard {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { cal-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, s.prevMonth) }
                    RollingText(s.hijriMonthTitle(cal % 12 + 1, cal / 12), MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    IconButton(onClick = { cal++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.nextMonth) }
                }
                AnimatedContent(
                    targetState = cal,
                    transitionSpec = {
                        val sign = (if (targetState > initialState) 1 else -1) * (if (rtl) -1 else 1)
                        (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> sign * w / 2 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                            (slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> -sign * w / 2 } + fadeOut(tween(Motion.SHORT))) using
                            SizeTransform(clip = true)
                    },
                    label = "month"
                ) { c ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        HijriMonthGrid(c / 12, c % 12 + 1, today) { day -> dir = "h2g"; hy = (c / 12).toString(); hm = c % 12 + 1; hd = day }
                    }
                }
                Text(s.calendarHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Note(s.moonNote) }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.hijri) }
    }
}

@Composable
private fun HijriMonthGrid(y: Int, m: Int, today: LocalDate, onPick: (Int) -> Unit) {
    val s = LocalStrings.current
    val first = Hijri.toGregorian(y, m, 1) ?: return
    val len = Hijri.monthLength(y, m)
    // الأسبوع يبدأ بالأحد كما في التقاويم العربية
    val lead = first.dayOfWeek.iso % 7 // الأحد = 0
    Row(Modifier.fillMaxWidth()) {
        listOf(6, 0, 1, 2, 3, 4, 5).forEach {
            Text(s.weekdaysShort[it], Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
    val cells = List(lead) { 0 } + (1..len).toList()
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0 until 7) {
                val day = week.getOrNull(i) ?: 0
                Box(Modifier.weight(1f).aspectRatio(0.85f)) {
                    if (day > 0) {
                        val g = first.plusDays((day - 1).toLong())
                        val isToday = g == today
                        val src = remember { MutableInteractionSource() }
                        Surface(
                            onClick = { onPick(day) }, shape = RoundedCornerShape(10.dp),
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
                            interactionSource = src,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).pressScale(src, 0.85f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("$day", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                Text("${g.dayOfMonth}/${g.monthValue}", fontSize = 9.sp,
                                    color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgeScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var hijri by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf("") }
    var m by remember { mutableIntStateOf(1) }
    var d by remember { mutableIntStateOf(1) }
    val today = LocalDate.now()

    val birth: LocalDate? = y.toIntOrNull()?.let { yi ->
        if (hijri) Hijri.toGregorian(yi, m, d)
        else if (yi in 1800..2200) runCatching { LocalDate.of(yi, m, d.coerceAtMost(monthLength(yi, m))) }.getOrNull() else null
    }

    ToolPage(s.ageTitle, s.ageSubtitle, onBack, sharedKey = "age", icon = TOOL_ICONS["age"]) {
        if (birth != null && !birth.isAfter(today)) {
            val g = periodBetween(birth, today)
            val hb = Hijri.toHijri(birth); val ht = Hijri.toHijri(today)
            val hAge = if (hb != null && ht != null) {
                var yy = ht.y - hb.y; var mm = ht.m - hb.m; var dd = ht.d - hb.d
                if (dd < 0) { mm--; val pm = if (ht.m == 1) 12 else ht.m - 1; val py = if (ht.m == 1) ht.y - 1 else ht.y; dd += Hijri.monthLength(py, pm) }
                if (mm < 0) { yy--; mm += 12 }
                s.ymd(yy, mm, dd)
            } else "—"
            val days = daysBetween(birth, today)
            var nextB = birth.withYear(today.year)
            if (nextB.isBefore(today)) nextB = nextB.plusYears(1)
            item { ResultCard(s.ageGregorian, s.ymd(g.years, g.months, g.days), s.inHijri(hAge)) }
            item {
                SectionCard {
                    StatRow(s.birthGreg, s.greg(birth))
                    StatRow(s.birthHijri, s.hijri(hb))
                    StatRow(s.birthWeekday, s.weekday(birth))
                    val left = daysBetween(today, nextB)
                    StatRow(s.nextBirthday, if (left == 0L) s.birthdayToday else s.inDays(left))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile(Fmt.int(days), s.tileDays)
                        Tile(Fmt.int(days / 7), s.tileWeeks)
                        Tile(Fmt.int(days * 24), s.tileHours)
                    }
                }
            }
        } else item { ResultCard(s.yourAge, "—", if (birth != null) s.birthAfterToday else s.enterBirth) }
        item {
            SectionCard(s.birthDate) {
                Segmented(listOf("g" to s.gregorian, "h" to s.hijri), if (hijri) "h" else "g") {
                    val newHijri = it == "h"
                    if (newHijri != hijri && birth != null) {
                        if (newHijri) Hijri.toHijri(birth)?.let { h -> y = h.y.toString(); m = h.m; d = h.d }
                        else { y = birth.year.toString(); m = birth.monthValue; d = birth.dayOfMonth }
                    }
                    hijri = newHijri
                }
                DateFields(hijri, y, m, d, { y = it }, { m = it }, { d = it })
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.age) }
    }
}

private data class IslamicEvent(val m: Int, val d: Int, val key: String, val emoji: String)

private val EVENTS = listOf(
    IslamicEvent(9, 1, "ramadan", "🌙"),
    IslamicEvent(9, 21, "lastTen", "✨"),
    IslamicEvent(10, 1, "eidFitr", "🎉"),
    IslamicEvent(10, 2, "shawwal", "📿"),
    IslamicEvent(12, 1, "dhulHijjah", "🕋"),
    IslamicEvent(12, 9, "arafah", "🤲"),
    IslamicEvent(12, 10, "eidAdha", "🐑"),
    IslamicEvent(1, 1, "newYear", "📅"),
    IslamicEvent(1, 10, "ashura", "🌊"),
)

@Composable
fun EventsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(active) { while (true) { now = LocalDateTime.now(); if (!active) break; delay(1000) } }
    val today = now.toLocalDate()
    val th = Hijri.toHijri(today)
    val upcoming = remember(today) {
        EVENTS.mapNotNull { e ->
            if (th == null) return@mapNotNull null
            listOf(th.y, th.y + 1, th.y + 2).firstNotNullOfOrNull { y -> Hijri.toGregorian(y, e.m, e.d)?.takeIf { !it.isBefore(today) }?.let { Triple(e, it, y) } }
        }.sortedBy { it.second }
    }
    var sel by remember {
        val wanted = Pending.event
        Pending.event = null
        mutableIntStateOf(upcoming.indexOfFirst { it.first.key == wanted }.coerceAtLeast(0))
    }

    ToolPage(s.eventsTitle, s.eventsSubtitle, onBack, sharedKey = "events", icon = TOOL_ICONS["events"]) {
        if (upcoming.isNotEmpty()) {
            val (e, date, y) = upcoming[sel.coerceIn(upcoming.indices)]
            val left = durationBetween(now, date.atStartOfDay()).let { if (it.isNegative()) Duration.ZERO else it }
            val text = s.events.getValue(e.key)
            item {
                ResultCard("${e.emoji} ${text.name}", s.hijriDate(e.d, e.m, y), s.greg(date)) {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CountTile(left.inWholeDays, s.unitDays)
                        CountTile(left.inWholeHours % 24, s.unitHours)
                        CountTile(left.inWholeMinutes % 60, s.unitMinutes)
                        CountTile(left.inWholeSeconds % 60, s.unitSeconds)
                    }
                    Text(text.desc, color = Color.White.copy(alpha = .9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
            upcoming.forEachIndexed { i, (ev, date2, _) ->
                item {
                    val days = daysBetween(today, date2)
                    val on = i == sel
                    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "ev")
                    val border by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "evB")
                    val src = remember { MutableInteractionSource() }
                    Surface(
                        onClick = { sel = i }, shape = RoundedCornerShape(16.dp), color = bg,
                        border = BorderStroke(1.dp, border), interactionSource = src, modifier = Modifier.pressScale(src, 0.97f)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ev.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.events[ev.key]?.name ?: ev.key, fontWeight = FontWeight.Bold)
                                Text("${s.weekday(date2)} ${s.greg(date2)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(when (days) { 0L -> s.today; 1L -> s.tomorrow; else -> s.inDays(days) }, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        item { Note(s.eventsNote) }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.events) }
    }
}

/** خانة عدّ تنازلي بيضاء على خلفية النتيجة */
@Composable
private fun androidx.compose.foundation.layout.RowScope.CountTile(v: Long, label: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .12f)) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(Fmt.two(v), MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), Color.White)
            Text(label, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
