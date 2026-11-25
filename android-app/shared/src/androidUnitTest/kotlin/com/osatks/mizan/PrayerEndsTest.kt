package com.osatks.mizan

import com.osatks.mizan.core.ALERT_PRAYERS
import com.osatks.mizan.core.EndAlerts
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

    // ---------- تنبيه قرب خروج الوقت ----------

    /** موعد التنبيه نهاية الوقت ناقص الدقائق: الفجر قبل الشروق، والظهر قبل العصر، والعصر قبل الغروب، والمغرب قبل العشاء، والعشاء قبل منتصف الليل */
    @Test fun endAlertHoursFollowEachEnd() {
        val t = PrayTimes.compute(day, 51.5074, -0.1278, "Europe/London", "mwl", false)
        val q = 15 / 60.0
        assertEquals(t.sunrise - q, EndAlerts.hours(t, "fajr", 15), 1e-9)
        assertEquals(t.asr - q, EndAlerts.hours(t, "dhuhr", 15), 1e-9)
        assertEquals(t.sunset - q, EndAlerts.hours(t, "asr", 15), 1e-9)
        // العصر ينتهي بالمغرب: الغروب والمغرب في الطريقة نفسها لا يفترقان إلا بدقائق الاحتياط
        assertEquals(t.maghrib, t.end("asr"), 2 / 60.0)
        assertEquals(t.isha - q, EndAlerts.hours(t, "maghrib", 15), 1e-9)
        assertEquals(t.midnight - 30 / 60.0, EndAlerts.hours(t, "isha", 30), 1e-9)
        assertTrue(EndAlerts.hours(t, "sunrise", 15).isNaN())
        for (k in ALERT_PRAYERS) for (m in EndAlerts.MINUTES) {
            val h = EndAlerts.hours(t, k, m)
            assertTrue("$k $m", h > t[k] && h < t.end(k))
        }
    }

    /** العشاء ينتهي بمنتصف الليل الشرعي: منتصف ما بين المغرب والفجر التالي */
    @Test fun ishaEndsAtIslamicMidnight() {
        for ((key, method) in listOf("makkah" to "makkah", "cairo" to "egypt", "london" to "mwl")) {
            val c = PrayTimes.city(key)!!
            val t = PrayTimes.compute(day, c.lat, c.lng, c.tz, method, false)
            val next = PrayTimes.compute(LocalDate(2026, 10, 28), c.lat, c.lng, c.tz, method, false)
            val mid = t.maghrib + (next.fajr + 24 - t.maghrib) / 2
            assertEquals("$key midnight", mid, t.end("isha"), 3 / 60.0)
            assertTrue(t.end("isha") > t.isha)
        }
    }

    /** وقت أقصر من المدة المختارة: لا تنبيه (لا يسبق دخول الوقت) */
    @Test fun noEndAlertBeforeThePrayerStarts() {
        val t = PrayerTimes(5.0, 5.3, 12.0, 15.5, 18.0, 19.5, 18.0, 19.0, 23.5)
        assertEquals(5.3 - 10 / 60.0, EndAlerts.hours(t, "fajr", 10), 1e-9)
        assertTrue(EndAlerts.hours(t, "fajr", 20).isNaN())
        assertTrue(EndAlerts.hours(t, "fajr", 18).isNaN())
        // نهاية لا تُعرف: لا تنبيه
        assertTrue(EndAlerts.hours(t.copy(midnight = Double.NaN), "isha", 15).isNaN())
    }

    /** القادمة مرتبة زمنياً، للصلوات المختارة فقط، وتنبيه عشاء الأمس بعد منتصف الليل لا يضيع */
    @Test fun upcomingEndAlertsCrossMidnight() {
        // منتصف الليل 00:30 من اليوم التالي، فتنبيه العشاء قبل ربع ساعة يقع 00:15
        val t = PrayerTimes(5.0, 6.5, 12.0, 15.5, 18.0, 19.5, 18.0, 19.5, 24.5)
        val from = LocalDate(2026, 11, 25)
        val after = LocalDateTime(2026, 11, 25, 0, 5)
        val all = EndAlerts.upcoming(from, 2, after, setOf("isha", "fajr"), 15) { t }
        assertEquals(listOf("isha", "fajr", "isha", "fajr", "isha"), all.map { it.prayer })
        assertEquals(LocalDate(2026, 11, 24), all[0].day)
        assertEquals(LocalDateTime(2026, 11, 25, 0, 15), all[0].at)
        assertEquals(LocalDateTime(2026, 11, 25, 0, 30), all[0].end)
        assertEquals(LocalDateTime(2026, 11, 25, 6, 15), all[1].at)
        assertEquals(LocalDateTime(2026, 11, 26, 0, 15), all[2].at)
        assertEquals(LocalDateTime(2026, 11, 27, 0, 15), all[4].at)
        assertTrue(all.zipWithNext().all { (a, b) -> a.at < b.at })
        // ما مضى لا يُجدول
        val later = EndAlerts.upcoming(from, 1, LocalDateTime(2026, 11, 25, 6, 20), setOf("isha", "fajr"), 15) { t }
        assertEquals(listOf(LocalDateTime(2026, 11, 26, 0, 15)), later.map { it.at })
        assertTrue(EndAlerts.upcoming(from, 7, after, emptySet(), 15) { t }.isEmpty())
    }

    @Test fun endAlertMinutesChoices() {
        assertEquals(listOf(10, 15, 20, 30), EndAlerts.MINUTES)
        assertEquals(15, EndAlerts.DEFAULT_MINUTES)
        assertEquals(20, EndAlerts.minutesOrDefault(20))
        assertEquals(15, EndAlerts.minutesOrDefault(7))
    }
}
