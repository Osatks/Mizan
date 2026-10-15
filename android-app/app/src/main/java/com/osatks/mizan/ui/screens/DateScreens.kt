package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Tile
import com.osatks.mizan.ui.ToolPage
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** حقول اختيار تاريخ (يوم/شهر/سنة) لأي تقويم */
@Composable
private fun DateFields(hijri: Boolean, y: String, m: Int, d: Int, onY: (String) -> Unit, onM: (Int) -> Unit, onD: (Int) -> Unit) {
    val yi = y.toIntOrNull()
    val maxDay = if (hijri) (if (yi != null && yi in 1300..1600) Hijri.monthLength(yi, m) else 30)
    else (if (yi != null && yi in 1..9999) java.time.YearMonth.of(yi, m).lengthOfMonth() else 31)
    LaunchedEffect(maxDay) { if (d > maxDay) onD(maxDay) }
    val months = if (hijri) Hijri.MONTHS else Hijri.GREG_MONTHS
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Picker("اليوم", (1..maxDay).toList(), d.coerceAtMost(maxDay), text = { "$it" }, onSelect = onD, modifier = Modifier.weight(0.8f))
        Picker("الشهر", (1..12).toList(), m, text = { months[it - 1] }, onSelect = onM, modifier = Modifier.weight(1.4f))
    }
    NumField("السنة", y, onY, if (hijri) "هـ" else "م")
}

@Composable
fun HijriScreen(onBack: () -> Unit) {
    val today = LocalDate.now()
    val th = Hijri.toHijri(today) ?: HDate(1448, 1, 1)
    var dir by remember { mutableStateOf("g2h") }
    var gy by remember { mutableStateOf(today.year.toString()) }
    var gm by remember { mutableIntStateOf(today.monthValue) }
    var gd by remember { mutableIntStateOf(today.dayOfMonth) }
    var hy by remember { mutableStateOf(th.y.toString()) }
    var hm by remember { mutableIntStateOf(th.m) }
    var hd by remember { mutableIntStateOf(th.d) }
    var calY by remember { mutableIntStateOf(th.y) }
    var calM by remember { mutableIntStateOf(th.m) }

    val (main, sub) = if (dir == "g2h") {
        val y = gy.toIntOrNull()
        val g = if (y != null && y in 1882..2174) runCatching { LocalDate.of(y, gm, gd.coerceAtMost(java.time.YearMonth.of(y, gm).lengthOfMonth())) }.getOrNull() else null
        val h = g?.let { Hijri.toHijri(it) }
        if (g != null && h != null) Hijri.hijriStr(h) to "${Hijri.weekday(g)} — ${Hijri.gregStr(g)}" else "—" to "أدخل سنة بين 1882 و2174"
    } else {
        val y = hy.toIntOrNull()
        val g = if (y != null) Hijri.toGregorian(y, hm, hd) else null
        if (g != null && y != null) Hijri.gregStr(g) to "${Hijri.weekday(g)} — ${Hijri.hijriStr(HDate(y, hm, hd))}" else "—" to "أدخل سنة هجرية بين 1300 و1600"
    }

    ToolPage("التاريخ الهجري", "وفق تقويم أم القرى", onBack) {
        item { ResultCard(if (dir == "g2h") "التاريخ الهجري الموافق" else "التاريخ الميلادي الموافق", main, sub) }
        item {
            SectionCard {
                Segmented(listOf("g2h" to "ميلادي ← هجري", "h2g" to "هجري ← ميلادي"), dir) { dir = it }
                if (dir == "g2h") DateFields(false, gy, gm, gd, { gy = it }, { gm = it }, { gd = it })
                else DateFields(true, hy, hm, hd, { hy = it }, { hm = it }, { hd = it })
                TextButton(onClick = {
                    gy = today.year.toString(); gm = today.monthValue; gd = today.dayOfMonth
                    hy = th.y.toString(); hm = th.m; hd = th.d
                }) { Text("تاريخ اليوم") }
            }
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (calM == 1) { calM = 12; calY-- } else calM-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق") }
                    Text("${Hijri.MONTHS[calM - 1]} $calY هـ", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (calM == 12) { calM = 1; calY++ } else calM++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي") }
                }
                HijriMonthGrid(calY, calM, today) { day -> dir = "h2g"; hy = calY.toString(); hm = calM; hd = day }
                Text("الرقم الكبير هجري والصغير ميلادي. اضغط أي يوم لتحويله.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Note("قد يختلف التاريخ يوماً واحداً عن بعض الدول بسبب اختلاف رؤية الهلال.") }
    }
}

@Composable
private fun HijriMonthGrid(y: Int, m: Int, today: LocalDate, onPick: (Int) -> Unit) {
    val first = Hijri.toGregorian(y, m, 1) ?: return
    val len = Hijri.monthLength(y, m)
    // عمود البداية: السبت أول الأسبوع؟ نستخدم الأحد أولاً كما في التقاويم العربية
    val lead = first.dayOfWeek.value % 7 // الأحد = 0
    Row(Modifier.fillMaxWidth()) {
        listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت").forEach {
            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Surface(
                            onClick = { onPick(day) }, shape = RoundedCornerShape(10.dp),
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)
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
    var hijri by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf("") }
    var m by remember { mutableIntStateOf(1) }
    var d by remember { mutableIntStateOf(1) }
    val today = LocalDate.now()

    val birth: LocalDate? = y.toIntOrNull()?.let { yi ->
        if (hijri) Hijri.toGregorian(yi, m, d)
        else if (yi in 1800..2200) runCatching { LocalDate.of(yi, m, d.coerceAtMost(java.time.YearMonth.of(yi, m).lengthOfMonth())) }.getOrNull() else null
    }

    ToolPage("حاسبة العمر", "بالتقويمين الهجري والميلادي", onBack) {
        if (birth != null && !birth.isAfter(today)) {
            val g = java.time.Period.between(birth, today)
            val hb = Hijri.toHijri(birth); val ht = Hijri.toHijri(today)
            val hAge = if (hb != null && ht != null) {
                var yy = ht.y - hb.y; var mm = ht.m - hb.m; var dd = ht.d - hb.d
                if (dd < 0) { mm--; val pm = if (ht.m == 1) 12 else ht.m - 1; val py = if (ht.m == 1) ht.y - 1 else ht.y; dd += Hijri.monthLength(py, pm) }
                if (mm < 0) { yy--; mm += 12 }
                "$yy سنة $mm شهر $dd يوم"
            } else "—"
            val days = ChronoUnit.DAYS.between(birth, today)
            var nextB = birth.withYear(today.year)
            if (nextB.isBefore(today)) nextB = nextB.plusYears(1)
            item { ResultCard("عمرك بالميلادي", "${g.years} سنة ${g.months} شهر ${g.days} يوم", "بالهجري: $hAge") }
            item {
                SectionCard {
                    StatRow("تاريخ الميلاد ميلادياً", Hijri.gregStr(birth))
                    StatRow("تاريخ الميلاد هجرياً", Hijri.hijriStr(hb))
                    StatRow("يوم الميلاد", Hijri.weekday(birth))
                    val left = ChronoUnit.DAYS.between(today, nextB)
                    StatRow("عيد ميلادك القادم", if (left == 0L) "اليوم 🎉" else "بعد $left يوماً")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile(Fmt.int(days), "يوماً")
                        Tile(Fmt.int(days / 7), "أسبوعاً")
                        Tile(Fmt.int(days * 24), "ساعة")
                    }
                }
            }
        } else item { ResultCard("عمرك", "—", if (birth != null) "تاريخ الميلاد بعد اليوم" else "أدخل تاريخ ميلادك") }
        item {
            SectionCard("تاريخ الميلاد") {
                Segmented(listOf("g" to "ميلادي", "h" to "هجري"), if (hijri) "h" else "g") {
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
    }
}

private data class IslamicEvent(val m: Int, val d: Int, val name: String, val desc: String)

private val EVENTS = listOf(
    IslamicEvent(9, 1, "🌙 شهر رمضان", "شهر الصيام والقيام وتلاوة القرآن."),
    IslamicEvent(9, 21, "✨ العشر الأواخر", "فيها ليلة القدر خير من ألف شهر."),
    IslamicEvent(10, 1, "🎉 عيد الفطر", "تُخرج زكاة الفطر قبل صلاته."),
    IslamicEvent(10, 2, "📿 ست من شوال", "من صام رمضان ثم أتبعه ستاً من شوال كان كصيام الدهر (رواه مسلم)."),
    IslamicEvent(12, 1, "🕋 عشر ذي الحجة", "أفضل أيام الدنيا، يُستحب فيها العمل الصالح والتكبير."),
    IslamicEvent(12, 9, "🤲 يوم عرفة", "صيامه يكفّر السنة الماضية والباقية لغير الحاج (رواه مسلم)."),
    IslamicEvent(12, 10, "🐑 عيد الأضحى", "يوم النحر، تليه أيام التشريق الثلاثة."),
    IslamicEvent(1, 1, "📅 رأس السنة الهجرية", "بداية عام هجري جديد."),
    IslamicEvent(1, 10, "🌊 يوم عاشوراء", "صيامه يكفّر السنة الماضية (رواه مسلم)، ويُستحب صيام التاسع معه."),
)

@Composable
fun EventsScreen(onBack: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    val today = now.toLocalDate()
    val th = Hijri.toHijri(today)
    val upcoming = remember(today) {
        EVENTS.mapNotNull { e ->
            if (th == null) return@mapNotNull null
            listOf(th.y, th.y + 1, th.y + 2).firstNotNullOfOrNull { y -> Hijri.toGregorian(y, e.m, e.d)?.takeIf { !it.isBefore(today) }?.let { Triple(e, it, y) } }
        }.sortedBy { it.second }
    }
    var sel by remember { mutableIntStateOf(0) }

    ToolPage("المناسبات الإسلامية", "عد تنازلي وفق تقويم أم القرى", onBack) {
        if (upcoming.isNotEmpty()) {
            val (e, date, y) = upcoming[sel.coerceIn(upcoming.indices)]
            val left = Duration.between(now, date.atStartOfDay()).let { if (it.isNegative) Duration.ZERO else it }
            item {
                ResultCard(e.name, "${left.toDays()} يوم  ${left.toHours() % 24} س  ${left.toMinutes() % 60} د  ${left.seconds % 60} ث",
                    "${e.d} ${Hijri.MONTHS[e.m - 1]} $y هـ — ${Hijri.gregStr(date)}") {
                    Text(e.desc, color = androidx.compose.ui.graphics.Color.White.copy(alpha = .9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
            }
            upcoming.forEachIndexed { i, (ev, date2, _) ->
                item {
                    val days = ChronoUnit.DAYS.between(today, date2)
                    Surface(
                        onClick = { sel = i }, shape = RoundedCornerShape(16.dp),
                        color = if (i == sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (i == sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ev.name, fontWeight = FontWeight.Bold)
                                Text("${Hijri.weekday(date2)} ${Hijri.gregStr(date2)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.padding(4.dp))
                            Text(when (days) { 0L -> "اليوم"; 1L -> "غداً"; else -> "بعد $days يوماً" }, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
        item { Note("التواريخ تقريبية وفق أم القرى، وقد تختلف يوماً بحسب رؤية الهلال في بلدك.") }
    }
}
