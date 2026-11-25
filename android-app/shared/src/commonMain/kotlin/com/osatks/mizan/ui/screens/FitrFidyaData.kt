package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import kotlinx.datetime.LocalDate

/** نوع الطعام: الوزن بالكيلوغرام (‎-1 = يُدخله المستخدم)، و approx إن كان الوزن تقريبياً */
internal data class Food(val key: String, val kg: Double, val approx: Boolean = true)

internal val FOODS = listOf(
    Food("rice", 2.5), Food("riceSafe", 3.0, approx = false), Food("wheat", 2.04),
    Food("dates", 2.5), Food("barley", 2.2), Food("other", -1.0),
)

internal fun Strings.foodLabel(f: Food): String {
    val name = foods[f.key] ?: f.key
    if (f.kg < 0) return name
    val kg = Fmt.num(f.kg)
    return "$name — " + if (f.approx) approxKg(kg) else "$kg $unitKg"
}

/** نتيجة الفدية أو الكفارة: عدد المساكين، والعنوان ووصفه، والملاحظة، وأسطر إضافية (الصيام البديل، ومدة الشهرين) */
internal class FidyaCalc(val poor: Int, val title: String, val sub: String, val note: String, val extra: List<Pair<String, String>>)

/** حساب [FidyaCalc] للنوع [tab] (fidya، yamin، jima) بعدد الأيام [days] أو الأيمان [oaths] */
internal fun fidyaCalc(s: Strings, tab: String, days: Int, oaths: Int): FidyaCalc {
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
                    extra += s.twoMonthsDuration to s.daysCount(daysBetween(start, end) + 1)
                }
            }
            note = s.intercourseNote
        }
    }
    return FidyaCalc(poor, title, sub, note, extra)
}
