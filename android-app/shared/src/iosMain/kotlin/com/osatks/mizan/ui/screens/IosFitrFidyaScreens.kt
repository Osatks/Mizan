package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalStrings

/** زكاة الفطر في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية؛ الحساب هنا كما في شاشة Compose (FitrScreen)، وSwift يرسم ويعيد المدخلات */
@Composable
internal fun IosFitrScreen(onBack: () -> Unit) {
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

    val rows = mutableListOf<Map<String, Any?>>()
    rows += nStepper("people", s.peopleCount, people, 1, 99)
    rows += nChoice("segmented", "mode", "", listOf("food" to s.modeFood, "money" to s.modeMoney), mode)
    if (mode == "food") {
        rows += nChoice("menu", "food", s.foodType, FOODS.map { it.key to s.foodLabel(it) }, food.key)
        if (food.kg < 0) rows += nField("custom", s.saWeight, custom, s.unitKg)
        rows += nField("kgPrice", s.kgPrice, kgPrice, null, s.kgPriceHint)
        rows += nStat("sa", s.saPerPerson, "${Fmt.num(sa)} ${s.unitKg}")
        if (Fmt.parse(kgPrice) > 0) rows += nStat("cost", s.approxCost, Fmt.num(kg * Fmt.parse(kgPrice)))
    } else {
        rows += nField("value", s.valuePerPerson, value, null, s.valuePerPersonHint)
        rows += nNote("valueNote", s.valueNote)
    }
    NativeFitrFidyaView(
        kind = "fitr",
        content = linkedMapOf(
            "title" to s.fitrTitle, "subtitle" to s.fitrSubtitle,
            "result" to if (mode == "food") nResult(s.foodAmountDue, Fmt.num(kg), s.forPeople(people), s.unitKg)
            else nResult(s.totalCashValue, if (perValue > 0) Fmt.num(perValue * people) else "—", s.forPeople(people)),
            "rows" to rows, "notes" to listOf(s.fitrTimingNote),
        ),
        evidenceKey = "fitr", evidence = com.osatks.mizan.i18n.Evidence.fitr,
        onStep = { k, v -> if (k == "people") people = v.coerceIn(1, 99) },
        onChange = { k, v ->
            when (k) {
                "mode" -> mode = v
                "food" -> FOODS.firstOrNull { it.key == v }?.let { food = it }
                "custom" -> custom = v
                "kgPrice" -> kgPrice = v
                "value" -> value = v
                else -> {}
            }
        },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
}

/** الفدية والكفارات في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية؛ الحساب هنا كما في شاشة Compose (FidyaScreen) */
@Composable
internal fun IosFidyaScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var tab by remember { mutableStateOf("fidya") }
    var days by remember { mutableIntStateOf(30) }
    var oaths by remember { mutableIntStateOf(1) }
    var measure by remember { mutableStateOf(0.75) }
    var price by remember { mutableStateOf("") }

    val calc = fidyaCalc(s, tab, days, oaths)
    val poor = calc.poor; val title = calc.title; val sub = calc.sub; val note = calc.note; val extra = calc.extra
    val kg = measure * poor
    val p = Fmt.parse(price)

    val rows = mutableListOf<Map<String, Any?>>()
    rows += nChoice("segmented", "tab", "", listOf("fidya" to s.tabFidya, "yamin" to s.tabOath, "jima" to s.tabIntercourse), tab)
    if (tab == "fidya") rows += nStepper("days", s.daysField, days, 1, 360)
    if (tab == "yamin") rows += nStepper("oaths", s.oathsField, oaths, 1, 100)
    rows += nChoice("menu", "measure", s.feedMeasure, listOf("mudd" to s.muddOption, "half" to s.halfSaOption), if (measure == 0.75) "mudd" else "half")
    rows += nField("price", s.kgPrice, price)
    rows += nStat("poor", s.poorCount, Fmt.int(poor.toLong()))
    extra.forEachIndexed { i, (a, b) -> rows += nStat("extra-$tab-$i", a, b) }
    if (p > 0) rows += nStat("value", s.approxValue, Fmt.num(p * kg), strong = true)
    NativeFitrFidyaView(
        kind = "fidya",
        content = linkedMapOf(
            "title" to s.fidyaTitle, "subtitle" to s.fidyaSubtitle,
            "result" to nResult(title, Fmt.num(kg), sub, s.unitKg),
            "rows" to rows, "notes" to listOf(note),
        ),
        evidenceKey = tab,
        evidence = when (tab) {
            "fidya" -> com.osatks.mizan.i18n.Evidence.fidya
            "yamin" -> com.osatks.mizan.i18n.Evidence.oath
            else -> com.osatks.mizan.i18n.Evidence.intercourse
        },
        onStep = { k, v ->
            when (k) {
                "days" -> days = v.coerceIn(1, 360)
                "oaths" -> oaths = v.coerceIn(1, 100)
                else -> {}
            }
        },
        onChange = { k, v ->
            when (k) {
                "tab" -> if (v in listOf("fidya", "yamin", "jima")) tab = v
                "measure" -> measure = if (v == "mudd") 0.75 else 1.5
                "price" -> price = v
                else -> {}
            }
        },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
}

// صفوف الواجهة من النظام (NativeFitrFidya): كل صف خريطة يرسمها Swift بحسب type، ومعرّفه id ثابت لحركة ظهوره واختفائه
private fun nStepper(key: String, label: String, value: Int, min: Int, max: Int) = linkedMapOf<String, Any?>(
    "id" to key, "type" to "stepper", "key" to key, "label" to label,
    "value" to value, "valueText" to Fmt.int(value.toLong()), "min" to min, "max" to max,
)

private fun nChoice(type: String, key: String, label: String, options: List<Pair<String, String>>, selected: String) = linkedMapOf<String, Any?>(
    "id" to key, "type" to type, "key" to key, "label" to label, "selected" to selected,
    "options" to options.map { (k, l) -> linkedMapOf("key" to k, "label" to l) },
)

private fun nField(key: String, label: String, text: String, suffix: String? = null, hint: String? = null) = linkedMapOf<String, Any?>(
    "id" to key, "type" to "field", "key" to key, "label" to label, "text" to text, "suffix" to suffix, "hint" to hint,
)

private fun nStat(id: String, label: String, value: String, strong: Boolean = false) = linkedMapOf<String, Any?>(
    "id" to id, "type" to "stat", "label" to label, "valueText" to value, "strong" to strong,
)

private fun nNote(id: String, text: String) = linkedMapOf<String, Any?>("id" to id, "type" to "note", "label" to text)

private fun nResult(label: String, value: String, badge: String?, unit: String? = null) =
    linkedMapOf<String, Any?>("label" to label, "value" to value, "unit" to unit, "badge" to badge)
