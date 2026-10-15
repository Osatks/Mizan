package com.osatks.mizan.core

import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** التقويم الهجري وفق أم القرى (مدمج في Java: من 1300 إلى 1600 هـ تقريباً). أسماء الأشهر في ملفات اللغة. */
data class HDate(val y: Int, val m: Int, val d: Int)

object Hijri {
    fun toHijri(date: LocalDate): HDate? = try {
        val h = HijrahDate.from(date)
        HDate(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH))
    } catch (e: DateTimeException) {
        null
    }

    fun toGregorian(y: Int, m: Int, d: Int): LocalDate? = try {
        LocalDate.from(HijrahDate.of(y, m, d))
    } catch (e: DateTimeException) {
        null
    }

    fun monthLength(y: Int, m: Int): Int = try {
        HijrahDate.of(y, m, 1).lengthOfMonth()
    } catch (e: DateTimeException) {
        30
    }
}
