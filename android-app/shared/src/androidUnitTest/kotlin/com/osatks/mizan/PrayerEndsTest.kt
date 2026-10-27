package com.osatks.mizan

import com.osatks.mizan.core.PrayTimes
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** نهاية وقت كل صلاة */
class PrayerEndsTest {
    private val day = LocalDate(2026, 10, 27)

    @Test fun makkahEnds() {
        val t = PrayTimes.compute(day, 21.4225, 39.8262, "Asia/Riyadh", "makkah", false)
        assertEquals(t.sunrise, t.end("fajr"), 1e-9)
        assertEquals(t.asr, t.end("dhuhr"), 1e-9)
        assertTrue(t.end("asr") in t.asr..t.isha)
        // أم القرى: العشاء بعد المغرب بتسعين دقيقة، والشفق الأحمر يغيب قبل ذلك
        assertTrue(t.end("maghrib") > t.maghrib && t.end("maghrib") < t.isha)
        val m = t.end("maghrib") - t.maghrib
        assertTrue("maghrib lasts ${m * 60} min", m * 60 in 60.0..85.0)
        // منتصف الليل بين العشاء والفجر التالي
        assertTrue(t.end("isha") > t.isha && t.end("isha") < t.fajr + 24)
        assertTrue(t.end("sunrise").isNaN())
    }

    @Test fun angleMethodMaghribEndsAtIsha() {
        val t = PrayTimes.compute(day, 51.5074, -0.1278, "Europe/London", "mwl", false)
        assertEquals(t.isha, t.end("maghrib"), 1e-9)
    }
}
