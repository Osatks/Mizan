package com.osatks.mizan.core

import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** التقويم الهجري وفق أم القرى (مدمج في Java: من 1300 إلى 1600 هـ تقريباً) */
data class HDate(val y: Int, val m: Int, val d: Int)

object Hijri {
    val MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    val GREG_MONTHS = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    /** أيام الأسبوع بترتيب java.time (الاثنين = 1) */
    private val DOW = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

    fun weekday(date: LocalDate): String = DOW[date.dayOfWeek.value - 1]

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

    fun hijriStr(h: HDate?): String = if (h == null) "—" else "${h.d} ${MONTHS[h.m - 1]} ${h.y} هـ"
    fun gregStr(g: LocalDate): String = "${g.dayOfMonth} ${GREG_MONTHS[g.monthValue - 1]} ${g.year} م"
}
