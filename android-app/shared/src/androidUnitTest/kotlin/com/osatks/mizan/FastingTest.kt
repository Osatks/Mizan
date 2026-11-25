package com.osatks.mizan

import com.osatks.mizan.core.DayMark
import com.osatks.mizan.core.FastCalendar
import com.osatks.mizan.core.Fasting
import com.osatks.mizan.core.FastingState
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.SunnahKind
import com.osatks.mizan.core.plusDays
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** متابعة الصيام: أيام التطوع وما لا يُصام، وعلامات رمضان والقضاء، وتذكير المساء السابق */
class FastingTest {
    private fun kindsOn(y: Int, m: Int, d: Int, shawwalDone: Int = 0): List<SunnahKind> {
        val g = Hijri.toGregorian(y, m, d)!!
        return FastCalendar.kinds(g, HDate(y, m, d), shawwalDone)
    }

    @Test fun forbiddenDaysAreNeverSuggested() {
        assertTrue(kindsOn(1447, 10, 1).isEmpty())
        for (d in 10..13) assertTrue("Dhul-Hijjah $d", kindsOn(1447, 12, d).isEmpty())
        // الأبيض الثالث عشر من ذي الحجة يسقط، والرابع عشر والخامس عشر يبقيان
        assertFalse(SunnahKind.WHITE in kindsOn(1447, 12, 13))
        assertTrue(SunnahKind.WHITE in kindsOn(1447, 12, 14))
        assertTrue(SunnahKind.WHITE in kindsOn(1447, 12, 15))
    }

    @Test fun specialDays() {
        assertTrue(SunnahKind.ARAFAH in kindsOn(1447, 12, 9))
        assertTrue(SunnahKind.TASUA in kindsOn(1448, 1, 9))
        assertTrue(SunnahKind.ASHURA in kindsOn(1448, 1, 10))
        assertTrue(SunnahKind.SHAWWAL in kindsOn(1447, 10, 2))
        assertFalse(SunnahKind.SHAWWAL in kindsOn(1447, 10, 2, shawwalDone = 6))
        assertTrue(SunnahKind.WHITE in kindsOn(1448, 3, 13))
        // رمضان فريضة: لا تطوع فيه ولو وافق اثنيناً أو خميساً
        for (d in 1..29) assertTrue(kindsOn(1447, 9, d).isEmpty())
    }

    @Test fun upcomingCoversAYearWithoutForbiddenDays() {
        val from = Hijri.toGregorian(1447, 1, 1)!!
        // سنة هجرية كاملة
        val days = FastCalendar.upcoming(from, 0, 400, 354)
        assertTrue(days.isNotEmpty())
        for (d in days) {
            assertFalse("${d.hijri}", FastCalendar.forbidden(d.hijri))
            assertTrue("${d.hijri}", d.hijri.m != 9)
            assertEquals(Hijri.toHijri(d.date), d.hijri)
        }
        // كل اثنين وخميس خارج رمضان والأعياد
        val mondays = days.count { SunnahKind.MONDAY in it.kinds }
        assertTrue("$mondays", mondays in 40..53)
    }

    @Test fun hijriOffsetShiftsDays() {
        val g = Hijri.toGregorian(1447, 12, 9)!!
        // بفرق يوم: يوم عرفة يأتي قبل يومه في أم القرى بيوم
        assertEquals(HDate(1447, 12, 9), FastCalendar.hijri(g.plusDays(-1), 1))
        assertEquals(g.plusDays(-1), FastCalendar.gregorian(1447, 12, 9, 1))
    }

    @Test fun ramadanYear() {
        assertEquals(1447, FastCalendar.ramadanYear(Hijri.toGregorian(1447, 9, 5)!!, 0))
        assertEquals(1447, FastCalendar.ramadanYear(Hijri.toGregorian(1447, 11, 5)!!, 0))
        assertEquals(1446, FastCalendar.ramadanYear(Hijri.toGregorian(1447, 6, 5)!!, 0))
    }

    @Test fun missedDaysAddToQada() {
        var s = FastingState()
        s = s.withMark(1447, 3, DayMark.FASTED, "2026-02-21")
        assertEquals(DayMark.FASTED, s.mark(1447, 3))
        assertEquals(0, s.qada)
        s = s.withMark(1447, 3, DayMark.MISSED, "2026-02-21")
        assertEquals(1, s.qada)
        assertEquals("missed", s.qadaLog.first().kind)
        s = s.withMark(1447, 3, DayMark.NONE, "2026-02-21")
        assertEquals(0, s.qada)
        assertTrue(s.ramadan.isEmpty())
        assertEquals(DayMark.FASTED, DayMark.NONE.next())
        assertEquals(DayMark.MISSED, DayMark.FASTED.next())
        assertEquals(DayMark.NONE, DayMark.MISSED.next())
    }

    @Test fun qadaNeverNegativeAndLogIsCapped() {
        var s = FastingState().qadaChange(-1, "remove", "2026-01-01")
        assertEquals(0, s.qada)
        assertTrue(s.qadaLog.isEmpty())
        repeat(Fasting.LOG_SIZE + 10) { s = s.qadaChange(+1, "add", "2026-01-01") }
        assertEquals(Fasting.LOG_SIZE + 10, s.qada)
        assertEquals(Fasting.LOG_SIZE, s.qadaLog.size)
        s = s.withSunnah("2026-01-05", true).withSunnah("2026-01-02", true).withSunnah("2026-01-05", true)
        assertEquals(listOf("2026-01-02", "2026-01-05"), s.sunnah)
        assertEquals(listOf("2026-01-05"), s.withSunnah("2026-01-02", false).sunnah)
    }

    @Test fun remindersAreOnTheEveningBefore() {
        val from = Hijri.toGregorian(1447, 1, 1)!!
        val now = LocalDateTime(from.year, from.monthNumber, from.dayOfMonth, 22, 0)
        val r = FastCalendar.reminders(now, 0, 3)
        assertEquals(3, r.size)
        for ((at, day) in r) {
            assertTrue(at > now)
            assertEquals(Fasting.REMIND_HOUR, at.hour)
            assertEquals(day.date.plusDays(-1), at.date)
        }
    }
}
