package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Stepper
import com.osatks.mizan.ui.ToolPage
import java.time.LocalDate

private data class Food(val name: String, val kg: Double)

private val FOODS = listOf(
    Food("أرز — 2.5 كغ تقريباً", 2.5), Food("أرز (الأحوط) — 3 كغ", 3.0), Food("قمح — 2.04 كغ تقريباً", 2.04),
    Food("تمر — 2.5 كغ تقريباً", 2.5), Food("شعير — 2.2 كغ تقريباً", 2.2), Food("مقدار آخر…", -1.0),
)

private fun peopleLabel(n: Int) = when (n) { 1 -> "عن فرد واحد"; 2 -> "عن فردين"; in 3..10 -> "عن $n أفراد"; else -> "عن $n فرداً" }

@Composable
fun FitrScreen(onBack: () -> Unit) {
    var people by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf("food") }
    var food by remember { mutableStateOf(FOODS[0]) }
    var custom by remember { mutableStateOf("2.5") }
    var kgPrice by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    val sa = if (food.kg < 0) Fmt.parse(custom) else food.kg
    val kg = sa * people
    val perValue = Fmt.parse(value)

    ToolPage("زكاة الفطر", "تُخرج قبل صلاة العيد", onBack) {
        item {
            if (mode == "food") ResultCard("مقدار الطعام الواجب إخراجه", "${Fmt.num(kg)} كغ", peopleLabel(people), true)
            else ResultCard("القيمة النقدية الإجمالية", if (perValue > 0) Fmt.num(perValue * people) else "—", peopleLabel(people), true)
        }
        item {
            SectionCard {
                Stepper("عدد من تُخرج عنهم (مع نفسك)", people, { people = it }, 1, 99)
                Segmented(listOf("food" to "طعام (الصاع)", "money" to "قيمة نقدية"), mode) { mode = it }
                if (mode == "food") {
                    Picker("نوع الطعام", FOODS, food, text = { it.name }, onSelect = { food = it })
                    if (food.kg < 0) NumField("وزن الصاع بالكيلوغرام", custom, { custom = it }, "كغ")
                    NumField("سعر الكيلوغرام (اختياري)", kgPrice, { kgPrice = it }, null, "لتعرف التكلفة التقريبية للشراء")
                    StatRow("مقدار الصاع للفرد", "${Fmt.num(sa)} كغ")
                    if (Fmt.parse(kgPrice) > 0) StatRow("التكلفة التقريبية", Fmt.num(kg * Fmt.parse(kgPrice)))
                } else {
                    NumField("قيمة زكاة الفطر عن الفرد", value, { value = it }, null, "تعلنها الجهة الرسمية في بلدك كل عام")
                    Note("إخراج القيمة قول الحنفية، والجمهور على إخراج الطعام.")
                }
            }
        }
        item { Note("وقت الإخراج: قبل صلاة العيد، ويجوز تقديمها بيوم أو يومين، ولا يجوز تأخيرها عن الصلاة بغير عذر. وتُعطى للفقراء والمساكين.") }
    }
}

@Composable
fun FidyaScreen(onBack: () -> Unit) {
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
            poor = days; title = "فدية الصيام"; sub = "عن $days يوماً — مسكين عن كل يوم"
            note = "لمن عجز عن الصيام عجزاً دائماً كالكبير والمريض الذي لا يُرجى برؤه. يجوز إطعام مسكين واحد عن كل الأيام أو مساكين متعددين."
        }
        "yamin" -> {
            poor = 10 * oaths; title = "كفارة اليمين"; sub = if (oaths > 1) "$oaths كفارات × 10 مساكين" else "إطعام 10 مساكين"
            extra += "البديل عند العجز" to "صيام ${3 * oaths} أيام"
            note = "يُخيّر بين إطعام عشرة مساكين أو كسوتهم أو تحرير رقبة، فإن لم يجد فصيام ثلاثة أيام (سورة المائدة: 89)."
        }
        else -> {
            poor = 60; title = "كفارة الجماع في نهار رمضان"; sub = "إطعام 60 مسكيناً عند العجز عن الصيام"
            val start = LocalDate.now()
            val h = Hijri.toHijri(start)
            if (h != null) {
                var y = h.y; var m = h.m + 2
                if (m > 12) { m -= 12; y++ }
                val end = Hijri.toGregorian(y, m, minOf(h.d, Hijri.monthLength(y, m)))?.minusDays(1)
                if (end != null) {
                    extra += "لو بدأت الصيام اليوم ينتهي في" to Hijri.gregStr(end)
                    extra += "مدة الشهرين الهجريين" to "${java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1} يوماً"
                }
            }
            note = "الواجب على الترتيب: عتق رقبة، فإن لم يجد فصيام شهرين متتابعين، فإن لم يستطع فإطعام ستين مسكيناً، مع قضاء اليوم والتوبة."
        }
    }
    val kg = measure * poor
    val p = Fmt.parse(price)

    ToolPage("الفدية والكفارة", "مقدار الإطعام بالكيلوغرام والقيمة", onBack) {
        item { ResultCard(title, "${Fmt.num(kg)} كغ", sub, true) }
        item {
            SectionCard {
                Segmented(listOf("fidya" to "فدية الصيام", "yamin" to "اليمين", "jima" to "الجماع"), tab) { tab = it }
                if (tab == "fidya") Stepper("عدد الأيام", days, { days = it }, 1, 360)
                if (tab == "yamin") Stepper("عدد الكفارات", oaths, { oaths = it }, 1, 100)
                Picker("مقدار الإطعام لكل مسكين", listOf(0.75, 1.5), measure,
                    text = { if (it == 0.75) "مُدّ — نحو 0.75 كغ (المالكية والشافعية)" else "نصف صاع — نحو 1.5 كغ (فتاوى معاصرة)" },
                    onSelect = { measure = it })
                NumField("سعر الكيلوغرام (اختياري)", price, { price = it })
                StatRow("عدد المساكين / الوجبات", "$poor")
                extra.forEach { (a, b) -> StatRow(a, b) }
                if (p > 0) StatRow("القيمة التقريبية", Fmt.num(p * kg), strong = true)
            }
        }
        item { Note(note) }
    }
}
