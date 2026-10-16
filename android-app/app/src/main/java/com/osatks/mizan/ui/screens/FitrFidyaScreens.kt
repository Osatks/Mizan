package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.Reveal
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Stepper
import com.osatks.mizan.ui.ToolPage
import java.time.LocalDate

/** نوع الطعام: الوزن بالكيلوغرام (‎-1 = يُدخله المستخدم)، و approx إن كان الوزن تقريبياً */
private data class Food(val key: String, val kg: Double, val approx: Boolean = true)

private val FOODS = listOf(
    Food("rice", 2.5), Food("riceSafe", 3.0, approx = false), Food("wheat", 2.04),
    Food("dates", 2.5), Food("barley", 2.2), Food("other", -1.0),
)

private fun Strings.foodLabel(f: Food): String {
    val name = foods[f.key] ?: f.key
    if (f.kg < 0) return name
    val kg = Fmt.num(f.kg)
    return "$name — " + if (f.approx) approxKg(kg) else "$kg $unitKg"
}

@Composable
fun FitrScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var people by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf("food") }
    var food by remember { mutableStateOf(FOODS[0]) }
    var custom by remember { mutableStateOf(Fmt.num(2.5)) }
    var kgPrice by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    val sa = if (food.kg < 0) Fmt.parse(custom) else food.kg
    val kg = sa * people
    val perValue = Fmt.parse(value)

    ToolPage(s.fitrTitle, s.fitrSubtitle, onBack, sharedKey = "fitr", icon = TOOL_ICONS["fitr"]) {
        item {
            if (mode == "food") ResultCard(s.foodAmountDue, Fmt.num(kg), s.forPeople(people), true, unit = s.unitKg)
            else ResultCard(s.totalCashValue, if (perValue > 0) Fmt.num(perValue * people) else "—", s.forPeople(people), true)
        }
        item {
            SectionCard {
                Stepper(s.peopleCount, people, { people = it }, 1, 99)
                Segmented(listOf("food" to s.modeFood, "money" to s.modeMoney), mode) { mode = it }
                Reveal(mode == "food") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Picker(s.foodType, FOODS, food, text = { s.foodLabel(it) }, onSelect = { food = it })
                        Reveal(food.kg < 0) { NumField(s.saWeight, custom, { custom = it }, s.unitKg) }
                        NumField(s.kgPrice, kgPrice, { kgPrice = it }, null, s.kgPriceHint)
                        StatRow(s.saPerPerson, "${Fmt.num(sa)} ${s.unitKg}")
                        Reveal(Fmt.parse(kgPrice) > 0) { StatRow(s.approxCost, Fmt.num(kg * Fmt.parse(kgPrice))) }
                    }
                }
                Reveal(mode == "money") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumField(s.valuePerPerson, value, { value = it }, null, s.valuePerPersonHint)
                        Note(s.valueNote)
                    }
                }
            }
        }
        item { Note(s.fitrTimingNote) }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.fitr) }
    }
}

@Composable
fun FidyaScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var tab by remember { mutableStateOf("fidya") }
    var days by remember { mutableIntStateOf(30) }
    var oaths by remember { mutableIntStateOf(1) }
    var measure by remember { mutableStateOf(0.75) }
    var price by remember { mutableStateOf("") }

    val poor: Int
    val title: String
    val sub: String
    val note: String
    val extra = mutableListOf<Pair<String, String>>()
    when (tab) {
        "fidya" -> {
            poor = days; title = s.fidyaResult; sub = s.fidyaSub(days)
            note = s.fidyaNote
        }
        "yamin" -> {
            poor = 10 * oaths; title = s.oathResult; sub = s.oathSub(oaths)
            extra += s.oathAlternative to s.fastDays(3 * oaths)
            note = s.oathNote
        }
        else -> {
            poor = 60; title = s.intercourseResult; sub = s.intercourseSub
            val start = LocalDate.now()
            val h = Hijri.toHijri(start)
            if (h != null) {
                var y = h.y; var m = h.m + 2
                if (m > 12) { m -= 12; y++ }
                val end = Hijri.toGregorian(y, m, minOf(h.d, Hijri.monthLength(y, m)))?.minusDays(1)
                if (end != null) {
                    extra += s.ifStartToday to s.greg(end)
                    extra += s.twoMonthsDuration to s.daysCount(java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1)
                }
            }
            note = s.intercourseNote
        }
    }
    val kg = measure * poor
    val p = Fmt.parse(price)

    ToolPage(s.fidyaTitle, s.fidyaSubtitle, onBack, sharedKey = "fidya", icon = TOOL_ICONS["fidya"]) {
        item { ResultCard(title, Fmt.num(kg), sub, true, unit = s.unitKg) }
        item {
            SectionCard {
                Segmented(listOf("fidya" to s.tabFidya, "yamin" to s.tabOath, "jima" to s.tabIntercourse), tab) { tab = it }
                Reveal(tab == "fidya") { Stepper(s.daysField, days, { days = it }, 1, 360) }
                Reveal(tab == "yamin") { Stepper(s.oathsField, oaths, { oaths = it }, 1, 100) }
                Picker(s.feedMeasure, listOf(0.75, 1.5), measure,
                    text = { if (it == 0.75) s.muddOption else s.halfSaOption },
                    onSelect = { measure = it })
                NumField(s.kgPrice, price, { price = it })
                StatRow(s.poorCount, "$poor")
                extra.forEach { (a, b) -> StatRow(a, b) }
                Reveal(p > 0) { StatRow(s.approxValue, Fmt.num(p * kg), strong = true) }
            }
        }
        item { Note(note) }
        item {
            EvidenceCard(when (tab) {
                "fidya" -> com.osatks.mizan.i18n.Evidence.fidya
                "yamin" -> com.osatks.mizan.i18n.Evidence.oath
                else -> com.osatks.mizan.i18n.Evidence.intercourse
            })
        }
    }
}
