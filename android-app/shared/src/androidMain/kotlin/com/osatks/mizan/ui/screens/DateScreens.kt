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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.osatks.mizan.ui.FormPicker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
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
    // اليوم بعرض ثابت والشهر يملأ الباقي (كما في iPhone)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormPicker(s.dayLabel, (1..maxDay).toList(), d.coerceAtMost(maxDay), text = { "$it" }, onSelect = onD, modifier = Modifier.width(108.dp))
        FormPicker(s.monthLabel, (1..12).toList(), m, text = { months[it - 1] }, onSelect = onM, modifier = Modifier.weight(1f))
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

    val (main, sub) = dateConversion(s, dir, gy, gm, gd, hy, hm, hd)

    ToolPage(s.hijriTitle, s.hijriSubtitle, onBack, sharedKey = "hijri", icon = TOOL_ICONS["hijri"]) {
        item { ResultCard(if (dir == "g2h") s.correspondingHijri else s.correspondingGreg, main, sub) }
        item {
            SectionCard {
                Segmented(listOf("g2h" to s.gregToHijri, "h2g" to s.hijriToGreg), dir) { dir = it }
                if (dir == "g2h") DateFields(false, gy, gm, gd, { gy = it }, { gm = it }, { gd = it })
                else DateFields(true, hy, hm, hd, { hy = it }, { hm = it }, { hd = it })
                // «تاريخ اليوم» كبسولة بلون التطبيق الفاتح
                val src = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        gy = today.year.toString(); gm = today.monthValue; gd = today.dayOfMonth
                        hy = th.y.toString(); hm = th.m; hd = th.d
                    },
                    shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, interactionSource = src,
                    modifier = Modifier.pressScale(src, 0.95f)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(s.todayDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            SectionCard {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, s.prevMonth) { cal-- }
                    RollingText(s.hijriMonthTitle(cal % 12 + 1, cal / 12), MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), TextAlign.Center)
                    MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.nextMonth) { cal++ }
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
            Text(s.weekdaysShort[it], Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                        // اليوم الحالي بتدرج لون التطبيق وحافة ذهبية، وسائر الأيام بلونه الفاتح (كما في iPhone)
                        Surface(
                            onClick = { onPick(day) }, shape = RoundedCornerShape(10.dp),
                            color = if (isToday) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f),
                            border = if (isToday) BorderStroke(1.5.dp, Gold) else null,
                            interactionSource = src,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).pressScale(src, 0.85f)
                        ) {
                            Column(
                                if (isToday) Modifier.background(HeroBrush) else Modifier,
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                            ) {
                                Text("$day", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1,
                                    color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface)
                                Text("${g.dayOfMonth}/${g.monthValue}", fontSize = 9.sp, maxLines = 1,
                                    color = if (isToday) Color.White.copy(alpha = .85f) else MaterialTheme.colorScheme.onSurfaceVariant)
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

    val birth: LocalDate? = birthDate(hijri, y, m, d)

    ToolPage(s.ageTitle, s.ageSubtitle, onBack, sharedKey = "age", icon = TOOL_ICONS["age"]) {
        if (birth != null && !birth.isAfter(today)) {
            val g = periodBetween(birth, today)
            val hb = Hijri.toHijri(birth)
            val hAge = hijriAge(s, birth, today)
            val days = daysBetween(birth, today)
            var nextB = birth.withYear(today.year)
            if (nextB.isBefore(today)) nextB = nextB.plusYears(1)
            item { ResultCard(s.ageGregorian, s.ymd(g.years, g.months, g.days), s.inHijri(hAge)) }
            item {
                SectionCard {
                    AgeStat(s.birthGreg, s.greg(birth))
                    AgeStat(s.birthHijri, s.hijri(hb))
                    AgeStat(s.birthWeekday, s.weekday(birth))
                    val left = daysBetween(today, nextB)
                    AgeStat(s.nextBirthday, if (left == 0L) s.birthdayToday else s.inDays(left))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AgeTile(Fmt.int(days), s.tileDays)
                        AgeTile(Fmt.int(days / 7), s.tileWeeks)
                        AgeTile(Fmt.int(days * 24), s.tileHours)
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

@Composable
fun EventsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(active) { while (true) { now = LocalDateTime.now(); if (!active) break; delay(1000L) } }
    val today = now.toLocalDate()
    val upcoming = remember(today) { upcomingEvents(today) }
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
                    val border by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f), tween(Motion.MEDIUM), label = "evB")
                    val src = remember { MutableInteractionSource() }
                    val edge by animateDpAsState(if (on) 1.5.dp else 1.dp, tween(Motion.MEDIUM), label = "evW")
                    Surface(
                        onClick = { sel = i }, shape = RoundedCornerShape(16.dp), color = bg,
                        border = BorderStroke(edge, border), interactionSource = src,
                        modifier = Modifier.pressScale(src, 0.97f).semantics { selected = on }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ev.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.events[ev.key]?.name ?: ev.key, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${s.weekday(date2)} ${s.greg(date2)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (days) { 0L -> s.today; 1L -> s.tomorrow; else -> s.inDays(days) },
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
        item { Note(s.eventsNote) }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.events) }
    }
}

/** سهم الشهر السابق أو التالي: دائرة بلون التطبيق الفاتح (كما في iPhone) */
@Composable
private fun MonthArrow(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, interactionSource = src,
        modifier = Modifier.size(38.dp).pressScale(src, 0.9f)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, desc, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) }
    }
}

/** سطر من تفاصيل العمر على خلفية فاتحة */
@Composable
private fun AgeStat(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background.copy(alpha = .7f))
            .padding(horizontal = 14.dp, vertical = 11.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        RollingText(value, MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

/** خانة عدد (الأيام والأسابيع والساعات) على خلفية ذهبية فاتحة */
@Composable
private fun androidx.compose.foundation.layout.RowScope.AgeTile(value: String, label: String) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Gold.copy(alpha = .12f))
            .padding(vertical = 12.dp, horizontal = 6.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RollingText(value, MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
