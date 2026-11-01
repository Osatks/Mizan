package com.osatks.mizan

import com.osatks.mizan.core.PrayTimes
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * المواقيت كما تنشرها الجهات الرسمية: ما يعرضه التطبيق (بالدقيقة) يطابق جدول الجهة في كل يوم منه.
 * كل جدول منسوخ كما هو من مصدره المذكور فوقه: التاريخ، ثم الفجر والشروق والظهر والعصر والمغرب والعشاء بتوقيت المدينة.
 */
class OfficialTimesTest {
    private val keys = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    private fun minutes(hhmm: String): Int = hhmm.trim().split(":").let { (h, m) -> h.toInt() * 60 + m.toInt() }

    /** [tolerance]: الفرق المقبول بالدقائق (صفر: مطابقة تامة) */
    private fun check(cityKey: String, rows: String, tolerance: Int = 0) {
        val c = PrayTimes.city(cityKey) ?: error("unknown city $cityKey")
        val hanafi = PrayTimes.method(c.method).hanafi
        val misses = mutableListOf<String>()
        for (line in rows.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }) {
            val cells = line.split(",")
            val day = LocalDate.parse(cells[0])
            val t = PrayTimes.compute(day, c.lat, c.lng, c.tz, c.method, hanafi)
            keys.forEachIndexed { i, k ->
                val official = cells.getOrNull(i + 1)?.trim().orEmpty()
                if (official.isEmpty()) return@forEachIndexed
                val shown = PrayTimes.format(t[k], h24 = true)
                var d = minutes(shown) - minutes(official)
                if (d > 720) d -= 1440 else if (d < -720) d += 1440
                if (abs(d) > tolerance) misses += "$cityKey $day $k: app $shown, official $official"
            }
        }
        assertTrue(misses.joinToString("\n"), misses.isEmpty())
    }

    // وزارة الأوقاف والشؤون والمقدسات الإسلامية، https://www.awqaf.gov.jo/ar/Pages/PrayerTime (عمّان، تشرين الثاني 2026)
    @Test fun jordanAmman() = check(
        "amman", """
        2026-11-01,05:30,06:46,12:20,15:24,17:53,19:10
        2026-11-02,05:30,06:47,12:20,15:24,17:52,19:09
        2026-11-03,05:31,06:48,12:20,15:23,17:51,19:08
        2026-11-04,05:32,06:49,12:20,15:22,17:51,19:07
        2026-11-05,05:33,06:50,12:20,15:22,17:50,19:07
        2026-11-06,05:33,06:51,12:20,15:21,17:49,19:06
        2026-11-07,05:34,06:51,12:20,15:21,17:48,19:06
        2026-11-08,05:35,06:52,12:20,15:20,17:48,19:05
        2026-11-09,05:36,06:53,12:20,15:19,17:47,19:04
        2026-11-10,05:36,06:54,12:21,15:19,17:46,19:04
        """
    )
}
