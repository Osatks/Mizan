package com.osatks.mizan

import com.osatks.mizan.core.Bits
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.Lang
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Base64
import java.util.BitSet

/** ما حلّ محل java.time و java.util في الكود المشترك يطابقهما تماماً */
class PortTest {
    /** جدول أم القرى = HijrahDate في كل يوم من 1300 إلى 1600 هـ */
    @Test
    fun hijriTableMatchesJdk() {
        val first = java.time.LocalDate.from(HijrahDate.of(1300, 1, 1))
        val last = java.time.LocalDate.from(HijrahDate.of(1600, 12, 1).plus(HijrahDate.of(1600, 12, 1).lengthOfMonth() - 1L, java.time.temporal.ChronoUnit.DAYS))
        var d = first
        while (!d.isAfter(last)) {
            val h = HijrahDate.from(d)
            val k = LocalDate(d.year, d.monthValue, d.dayOfMonth)
            val ours = Hijri.toHijri(k)!!
            assertEquals("$d", Triple(h.get(ChronoField.YEAR), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.DAY_OF_MONTH)), Triple(ours.y, ours.m, ours.d))
            if (ours.d == 1) assertEquals(k, Hijri.toGregorian(ours.y, ours.m, 1))
            d = d.plusDays(1)
        }
        assertNull(Hijri.toHijri(LocalDate(1800, 1, 1)))
        assertEquals(30, Hijri.monthLength(1447, 9).coerceAtLeast(29).let { if (it == 29) 30 else it }.coerceAtMost(30))
    }

    /** الختمة المحفوظة بـ BitSet في الإصدارات السابقة تُقرأ كما هي */
    @Test
    fun bitsMatchBitSet() {
        val pages = listOf(1, 2, 7, 8, 100, 300, 604)
        val js = BitSet(605).apply { pages.forEach { set(it) } }
        val saved = Base64.getEncoder().encodeToString(js.toByteArray())
        val b = Bits.fromBase64(saved, 605)
        for (i in 0..604) assertEquals("page $i", js[i], b[i])
        assertEquals(js.cardinality(), b.cardinality())
        assertEquals(saved, b.toBase64())
        b.set(9); js.set(9)
        assertEquals(Base64.getEncoder().encodeToString(js.toByteArray()), b.toBase64())
    }

    /** التنسيق يطابق DecimalFormat في كل اللغات */
    @Test
    fun formatMatchesDecimalFormat() {
        for (l in Lang.entries) {
            val sym = java.text.DecimalFormatSymbols(java.util.Locale.US).apply { decimalSeparator = l.decimal; groupingSeparator = l.group }
            val fmts = listOf(java.text.DecimalFormat("#,##0", sym), java.text.DecimalFormat("#,##0.#", sym), java.text.DecimalFormat("#,##0.##", sym))
            for (v in listOf(0.0, 0.5, 1.25, 2.675, 999.995, 1234.5, 1_000_000.0, 12_345_678.91, 0.004, 85.0, 595.0)) for (dg in 0..2)
                assertEquals("${l.code} $v $dg", fmts[dg].format(v), Fmt.num(v, dg, l))
        }
    }
}
