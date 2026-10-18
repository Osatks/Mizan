package com.osatks.mizan.quran

import com.osatks.mizan.core.toEpochDay
import kotlinx.datetime.LocalDate

/** آية قصيرة (تسع الأداة) تُختار بثبات لكل يوم */
fun ayahOfDay(text: Array<String>, day: LocalDate): Int {
    val short = text.indices.filter { QuranData.withoutBismillah(text, it).length in 40..160 }
    return short[(day.toEpochDay() * 7919L).mod(short.size.toLong()).toInt()]
}
