package com.osatks.mizan.fixtures

import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.atStartOfDay
import com.osatks.mizan.core.daysBetween
import com.osatks.mizan.core.durationBetween
import com.osatks.mizan.core.formatNumber
import com.osatks.mizan.core.iso
import com.osatks.mizan.core.monthLength
import com.osatks.mizan.core.periodBetween
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.plusHours
import com.osatks.mizan.core.plusMonths
import com.osatks.mizan.core.plusSeconds
import com.osatks.mizan.core.plusYears
import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.core.tzOffsetHours
import com.osatks.mizan.core.withYear
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * يصدّر نتائج Kotlin لمواقيت الصلاة والتاريخ الهجري والتنسيق إلى iosApp/MizanCore/Fixtures/prayer
 * لتقارنها CheckPrayer بنتائج MizanBase و MizanPrayer في Swift. الأعداد العشرية نصوص JSON بصيغة toString (تُقرأ بلا فقد)،
 * و NaN تُكتب null. ما يعتمد على الساعة أو الإعدادات (PrayerUtil.next، والصلاة الحاضرة في الرئيسية، وحسابات شاشات التاريخ)
 * منسوخ هنا بنصه مع وقت «الآن» معطى.
 */
/** صفوف الملف: كل += يضيف صفاً واحداً (لا يفرد القائمة) */
private class Rows : ArrayList<Any?>() {
    operator fun plusAssign(v: List<Any?>) { add(v) }
}

class PrayerFixturesExport {
    private val out = File("../../iosApp/MizanCore/Fixtures/prayer").apply { mkdirs() }

    // كاتب JSON صغير
    private fun q(s: String) = buildString {
        append('"')
        for (ch in s) when (ch) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n")
            else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
        }
        append('"')
    }
    private fun j(v: Any?): String = when (v) {
        null -> "null"
        is String -> q(v)
        is Boolean -> if (v) "1" else "0"
        is Int, is Long -> v.toString()
        is Double -> if (v.isNaN()) "null" else q(v.toString())
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { q(it.key.toString()) + ":" + j(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { j(it) }
        is DoubleArray -> v.toList().let(::j)
        else -> error("type ${v::class}")
    }
    private fun write(name: String, cases: List<Any?>) {
        File(out, name).writeText(cases.joinToString(",\n", "[\n", "\n]\n") { j(it) })
        println("$name: ${cases.size}")
    }

    private fun all(t: PrayerTimes) = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha, t.sunset, t.maghribEnd, t.midnight)
    private fun shown(t: PrayerTimes) = PrayTimes.PRAYERS.joinToString(" ") { PrayTimes.format(t[it], true) } +
        " " + listOf("fajr", "dhuhr", "asr", "maghrib", "isha").joinToString(" ") { PrayTimes.format(t.end(it), true) }

    /** مواقع إضافية في العروض العليا والدنيا (صيف قطبي وشتاء قطبي) */
    private val extra = listOf(
        listOf("oslo", 59.9139, 10.7522, "Europe/Oslo", 0),
        listOf("reykjavik", 64.1466, -21.9426, "Atlantic/Reykjavik", 0),
        listOf("tromso", 69.6492, 18.9553, "Europe/Oslo", 0),
        listOf("murmansk", 68.9585, 33.0827, "Europe/Moscow", 0),
        listOf("longyearbyen", 78.2232, 15.6267, "Arctic/Longyearbyen", 0),
        listOf("helsinki", 60.1699, 24.9384, "Europe/Helsinki", 0),
        listOf("ushuaia", -54.8019, -68.303, "America/Argentina/Ushuaia", 0),
        listOf("capetown", -33.9249, 18.4241, "Africa/Johannesburg", 0),
        listOf("quito", -0.1807, -78.4678, "America/Guayaquil", 2850),
        listOf("auckland", -36.8485, 174.7633, "Pacific/Auckland", 0),
        listOf("anchorage", 61.2181, -149.9003, "America/Anchorage", 0),
        listOf("badzone", 30.0, 31.0, "Not/AZone", 0),
    )

    private fun case(name: String, lat: Double, lng: Double, tz: String, method: String, hanafi: Boolean, elev: Int, d: LocalDate): List<Any?> {
        val t = PrayTimes.compute(d, lat, lng, tz, method, hanafi, elev)
        return listOf(name, lat, lng, tz, method, hanafi, elev, d.toString(), all(t), shown(t), PrayTimes.tzOffset(tz, d))
    }

    @Test fun times() {
        val y = 2026
        // كل ثالث عشر يوم من 2026 وأيام بعينها: تحويل التوقيت الصيفي، ورمضان والعيدان وعرفة، والانقلابان، وسنوات أخرى
        val special = listOf(
            "2026-03-08", "2026-03-09", "2026-03-29", "2026-03-30", "2026-10-25", "2026-10-26", "2026-11-01", "2026-11-02",
            "2026-04-05", "2026-10-04", "2026-02-18", "2026-02-19", "2026-03-19", "2026-03-20", "2026-05-26", "2026-05-27",
            "2026-06-21", "2026-12-21", "2025-03-01", "2025-06-15", "2027-02-08", "2027-03-09", "2028-02-29", "2030-01-01",
            "2024-12-31", "2035-07-04", "1990-06-21", "2077-12-25",
        ).map(LocalDate::parse)
        val dates = ((0 until 365 step 13).map { LocalDate(y, 1, 1).plusDays(it) } + special).distinct().sorted()
        val cases = Rows()
        for (c in PrayTimes.CITIES) {
            val m = PrayTimes.method(c.method)
            for (d in dates) for (h in listOf(m.hanafi, !m.hanafi)) cases += case(c.key, c.lat, c.lng, c.tz, c.method, h, c.elev, d)
        }
        write("times_default.json", cases)

        // كل طريقة في كل مدينة: الانقلابان، والعصران
        val cases2 = Rows()
        for (c in PrayTimes.CITIES) for (m in PrayTimes.METHODS) for (d in listOf(LocalDate(y, 6, 21), LocalDate(y, 12, 21))) {
            for (h in listOf(false, true)) cases2 += case(c.key, c.lat, c.lng, c.tz, m.key, h, c.elev, d)
        }
        write("times_methods.json", cases2)

        // العروض العليا: كل طريقة في 24 يوماً من السنة، والعصران
        val cases3 = Rows()
        val dates3 = (1..12).map { mo -> LocalDate(y, mo, 15) } + LocalDate(y, 6, 21) + LocalDate(y, 12, 21) + LocalDate(2027, 2, 28)
        for (e in extra) for (m in PrayTimes.METHODS + listOf(PrayTimes.METHODS[0].copy(key = "unknown"))) for (d in dates3) for (h in listOf(false, true)) {
            cases3 += case(e[0] as String, e[1] as Double, e[2] as Double, e[3] as String, m.key, h, e[4] as Int, d)
        }
        // نقاط عشوائية في العالم بطرق عشوائية
        val r = Random(7)
        val zones = listOf("UTC", "Asia/Riyadh", "Europe/London", "America/New_York", "Asia/Tokyo", "Australia/Sydney", "Asia/Kolkata", "Asia/Tehran", "Pacific/Chatham")
        repeat(1500) {
            val lat = r.nextDouble(-66.0, 66.0); val lng = r.nextDouble(-180.0, 180.0)
            val tz = zones[r.nextInt(zones.size)]
            val m = PrayTimes.METHODS[r.nextInt(PrayTimes.METHODS.size)].key
            val d = LocalDate(2020, 1, 1).plusDays(r.nextInt(4000))
            cases3 += case("rand", lat, lng, tz, m, r.nextBoolean(), if (r.nextInt(4) == 0) r.nextInt(3000) else 0, d)
        }
        write("times_extra.json", cases3)
    }

    @Test fun helpers() {
        // الطرق كما هي
        write("methods.json", PrayTimes.METHODS.map { m ->
            listOf(m.key, m.fajr, m.ishaAngle, m.ishaMinutes, m.maghribAngle, m.minutes.toSortedMap(), m.hanafi, m.elevation, m.dailySun,
                m.fajrFallback, m.ishaFallback, m.seasonal, m.depression?.toList(), m.ramadan.toSortedMap())
        })
        write("cities.json", PrayTimes.CITIES.map { c -> listOf(c.key, c.cc, c.lat, c.lng, c.tz, c.method, c.elev) })

        // تنسيق الأوقات
        val fmt = Rows()
        var h = -3.0
        while (h < 27.0) { fmt += listOf(h, PrayTimes.format(h, true), PrayTimes.format(h, false), PrayTimes.format(h, false, "ص", "م")); h += 1.0 / 240 + 1e-7 }
        for (mins in 0 until 1440) for (dx in listOf(-1e-9, 0.0, 1e-9)) {
            val v = (mins + 0.5) / 60.0 + dx
            fmt += listOf(v, PrayTimes.format(v, true), PrayTimes.format(v, false), PrayTimes.format(v, false, "ص", "م"))
        }
        fmt += listOf(Double.NaN, PrayTimes.format(Double.NaN, true), PrayTimes.format(Double.NaN, false), PrayTimes.format(Double.NaN, false, "ص", "م"))
        write("format.json", fmt)

        // حد الشفق الموسمي، والقبلة والمسافة، وأقرب مدينة، والارتفاع، والجهة، والموقع من الإحداثيات
        val r = Random(11)
        val geo = Rows()
        val points = PrayTimes.CITIES.map { it.lat to it.lng } + PrayTimes.CITIES.map { it.lat + 0.2 to it.lng - 0.25 } +
            listOf(0.0 to 0.0, 21.4225 to 39.8262, -21.4225 to -140.1738, 90.0 to 0.0, -90.0 to 0.0, 21.0 to 39.8262) +
            (1..2000).map { r.nextDouble(-89.0, 89.0) to r.nextDouble(-180.0, 180.0) }
        for ((lat, lng) in points) {
            val q = PrayTimes.qibla(lat, lng)
            val (c, d2) = PrayTimes.nearestCity(lat, lng)
            val p = LocationHelper.placeFrom(lat, lng)
            geo += listOf(lat, lng, q, PrayTimes.distanceKm(lat, lng), c.key, d2, PrayTimes.elevationNear(lat, lng),
                kotlin.math.round(q / 45.0).toInt() % 8, p.key, p.lat, p.lng, p.method, p.elev,
                // AppSettings.defaultMethod بنصه
                PrayTimes.city(p.key)?.method ?: PrayTimes.nearestCity(p.lat, p.lng).first.method)
        }
        write("geo.json", geo)

        val tw = Rows()
        for (lat in listOf(-60.0, -51.5, -33.9, -10.0, 0.0, 10.0, 33.3, 48.85, 51.5074, 55.0, 59.33, 65.0)) {
            for (yy in listOf(2024, 2026)) {
                var d = LocalDate(yy, 1, 1)
                while (d.year == yy) {
                    tw += listOf(lat, d.toString(), PrayTimes.seasonalTwilight(lat, d, true), PrayTimes.seasonalTwilight(lat, d, false))
                    d = d.plusDays(3)
                }
            }
        }
        write("twilight.json", tw)
    }

    @Test fun next() {
        // PrayerUtil.next بنصه مع «الآن» معطى (الأصل يقرأ الساعة والإعدادات)
        fun next(now: LocalDateTime, times: (LocalDate) -> PrayerTimes): List<Any?> {
            for (offset in 0L..1L) {
                val day = now.date.plusDays(offset)
                val t = times(day)
                for (key in PrayTimes.PRAYERS) {
                    if (key == "sunrise" || t[key].isNaN()) continue
                    val dt = PrayerUtil.at(day, t[key])
                    if (dt > now) return listOf(key, dt.toString(), durationBetween(now, dt).inWholeSeconds, PrayerUtil.hms(durationBetween(now, dt)))
                }
            }
            return listOf("fajr", now.plusHours(1).toString(), 3600L, "01:00:00")
        }
        // الصلاة الحاضرة في شبكة الرئيسية بنصها
        fun current(times: PrayerTimes, sec: Double): String? {
            fun secOf(key: String) = times[key] * 3600
            val keys = PrayTimes.PRAYERS.filter { !times[it].isNaN() }
            return keys.lastOrNull { it != "sunrise" && secOf(it) <= sec }?.takeIf { cur ->
                val after = keys.getOrNull(keys.indexOf(cur) + 1)
                after == null || sec < secOf(after)
            }
        }
        val places = listOf("makkah", "london", "stockholm", "kazan", "sydney", "jakarta", "newyork", "doha", "istanbul", "casablanca")
            .map { PrayTimes.city(it)!! }
        val nexts = Rows()
        val r = Random(3)
        for (c in places) {
            val hanafi = PrayTimes.method(c.method).hanafi
            val times = { d: LocalDate -> PrayTimes.compute(d, c.lat, c.lng, c.tz, c.method, hanafi, c.elev) }
            for (i in 0 until 150) {
                val day = LocalDate(2026, 1, 1).plusDays(r.nextInt(500))
                val now = LocalDateTime(day.year, day.monthNumber, day.dayOfMonth, r.nextInt(24), r.nextInt(60), r.nextInt(60), r.nextInt(1_000_000_000))
                val t = times(now.date)
                val sec = now.time.toSecondOfDay().toDouble()
                nexts += listOf(c.key, now.toString(), next(now, times), current(t, sec), sec)
            }
        }
        write("next.json", nexts)
        // المدد بالساعات:الدقائق:الثواني
        write("hms.json", listOf(-5L, 0L, 1L, 59L, 60L, 3599L, 3600L, 86399L, 86400L, 360000L, 1234567L).map { listOf(it, PrayerUtil.hms(it.seconds)) })
    }

    @Test fun hijriAndDates() {
        // التاريخ الهجري لكل يوم ثالث في الجدول وما حوله
        val first = Hijri.toGregorian(1300, 1, 1)!!.toEpochDay()
        val last = Hijri.toGregorian(1600, 12, Hijri.monthLength(1600, 12))!!.toEpochDay()
        val h = Rows()
        var e = first - 5
        while (e <= last + 5) {
            val hd = Hijri.toHijri(LocalDate.fromEpochDays(e.toInt()))
            h += listOf(e, hd?.let { listOf(it.y, it.m, it.d) })
            e += if (e < first + 40 || e > last - 40) 1 else 3
        }
        write("hijri.json", h)
        val g = Rows()
        for (y in 1298..1602) for (m in 0..13) {
            for (d in listOf(-1, 0, 1, 15, 29, 30, 31)) g += listOf(y, m, d, Hijri.toGregorian(y, m, d)?.toString())
            g += listOf(y, m, "len", Hijri.monthLength(y, m))
        }
        write("hijri_greg.json", g)

        // التواريخ: الأيام منذ 1970، ويوم الأسبوع واليوم في السنة، وإضافة الأشهر والسنوات، والفرق بالسنوات والأشهر والأيام
        val r = Random(5)
        val dates = Rows()
        repeat(3000) {
            val a = LocalDate.fromEpochDays(r.nextInt(-80000, 80000))
            val b = LocalDate.fromEpochDays(r.nextInt(-80000, 80000))
            val n = r.nextInt(-40, 40)
            val p = periodBetween(minOf(a, b), maxOf(a, b))
            val p2 = periodBetween(maxOf(a, b), minOf(a, b))
            dates += listOf(a.toString(), a.toEpochDay(), a.dayOfWeek.iso, a.dayOfYear, monthLength(a.year, a.monthNumber), n,
                a.plusMonths(n).toString(), a.plusYears(n).toString(), a.withYear(2027).toString(), b.toString(), daysBetween(a, b),
                listOf(p.years, p.months, p.days), listOf(p2.years, p2.months, p2.days))
        }
        // حالات أطراف الأشهر
        for (s in listOf("2024-01-31", "2024-02-29", "2023-03-31", "2025-12-31", "2000-02-29", "1900-02-28", "2100-03-31")) {
            val a = LocalDate.parse(s)
            for (n in listOf(-13, -12, -1, 1, 2, 12, 13, 48)) {
                val b = a.plusDays(n * 17)
                val p = periodBetween(minOf(a, b), maxOf(a, b)); val p2 = periodBetween(maxOf(a, b), minOf(a, b))
                dates += listOf(a.toString(), a.toEpochDay(), a.dayOfWeek.iso, a.dayOfYear, monthLength(a.year, a.monthNumber), n,
                    a.plusMonths(n).toString(), a.plusYears(n).toString(), a.withYear(2027).toString(), b.toString(), daysBetween(a, b),
                    listOf(p.years, p.months, p.days), listOf(p2.years, p2.months, p2.days))
            }
        }
        write("dates.json", dates)

        // فرق المنطقة الزمنية لوقت محلي، كل ساعة ونصف حول تحويلات التوقيت الصيفي (الفجوة والتكرار)
        val tz = Rows()
        val zones = listOf(
            "Asia/Riyadh", "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Istanbul", "Europe/Moscow", "America/New_York",
            "America/Chicago", "America/Los_Angeles", "America/Toronto", "Australia/Sydney", "Asia/Tehran", "Asia/Jerusalem",
            "Asia/Gaza", "Asia/Beirut", "Asia/Amman", "Asia/Damascus", "Africa/Cairo", "Africa/Casablanca", "Asia/Kolkata",
            "Asia/Kathmandu", "Pacific/Chatham", "Australia/Lord_Howe", "Europe/Dublin", "America/Santiago", "UTC", "Not/AZone",
        )
        for (z in zones) {
            for (ds in listOf("2026-03-08", "2026-03-27", "2026-03-29", "2026-10-25", "2026-11-01", "2026-04-05", "2026-10-04", "2026-01-15", "2026-07-15", "2024-03-31", "2025-10-31", "2026-02-15", "2026-03-22")) {
                val d = LocalDate.parse(ds)
                var t = d.atStartOfDay()
                repeat(48) { tz += listOf(z, t.toString(), tzOffsetHours(z, t)); t = t.plusSeconds(1800) }
            }
        }
        write("tz.json", tz)

        // حسابات شاشات التاريخ بنصها: العمر بالهجري، وعيد الميلاد القادم، والمناسبات القادمة
        data class Ev(val m: Int, val d: Int, val key: String)
        val events = listOf(Ev(9, 1, "ramadan"), Ev(9, 21, "lastTen"), Ev(10, 1, "eidFitr"), Ev(10, 2, "shawwal"), Ev(12, 1, "dhulHijjah"),
            Ev(12, 9, "arafah"), Ev(12, 10, "eidAdha"), Ev(1, 1, "newYear"), Ev(1, 10, "ashura"))
        val screens = Rows()
        repeat(1500) {
            val today = LocalDate.fromEpochDays(r.nextInt(-12000, 40000))
            val birth = today.plusDays(-r.nextInt(0, 30000))
            val hb = Hijri.toHijri(birth); val ht = Hijri.toHijri(today)
            val hAge = if (hb != null && ht != null) {
                var yy = ht.y - hb.y; var mm = ht.m - hb.m; var dd = ht.d - hb.d
                if (dd < 0) { mm--; val pm = if (ht.m == 1) 12 else ht.m - 1; val py = if (ht.m == 1) ht.y - 1 else ht.y; dd += Hijri.monthLength(py, pm) }
                if (mm < 0) { yy--; mm += 12 }
                listOf(yy, mm, dd)
            } else null
            var nextB = birth.withYear(today.year)
            if (nextB < today) nextB = nextB.plusYears(1)
            val th: HDate? = ht
            val upcoming = events.mapNotNull { e ->
                if (th == null) return@mapNotNull null
                listOf(th.y, th.y + 1, th.y + 2).firstNotNullOfOrNull { y -> Hijri.toGregorian(y, e.m, e.d)?.takeIf { it >= today }?.let { Triple(e, it, y) } }
            }.sortedBy { it.second }.map { listOf(it.first.key, it.second.toString(), it.third) }
            screens += listOf(today.toString(), birth.toString(), hAge, daysBetween(today, nextB), upcoming)
        }
        write("datescreens.json", screens)
    }

    @Test fun format() {
        val values = listOf(
            0.0, -0.0, 0.004, 0.005, 0.0050000001, 0.015, 0.025, 0.125, 0.135, 0.5, 1.0, 1.005, 1.015, 1.5, 2.5, 3.5, 2.675, 9.995, 10.0,
            99.995, 999.995, 1234.5, 1234.5678, 12345.678, 123456.789, 1234567.891, 1e6 + 0.5, 1e9, 1e12 + 0.125, 9007199254740991.0,
            9007199254740992.0, 1e17, 1.5e20, 1.2345678901234567e25, -1.0, -1.5, -2.5, -1234.5, -0.004, -0.005, -999999.999, 85.0, 595.0,
            0.1, 0.2, 0.3, 0.1 + 0.2, 1.0 / 3, 2.0 / 3, 100.0 / 7, 4.35, 4.45, 0.045, 0.055, 1.045, 21.4225, 39.8262, 1111.11, 0.999, 0.9951,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
        )
        val r = Random(9)
        val all = values + (1..300).map { (r.nextDouble(-1e7, 1e7) * 1000).let { kotlin.math.round(it) } / 1000.0 } +
            (1..200).map { r.nextInt(0, 100000) / 200.0 } + (1..100).map { r.nextDouble(0.0, 100.0) }
        val num = Rows()
        for (v in all) for (l in Lang.BUILT_IN) {
            num += listOf(v, l.code, (-1..3).map { Fmt.num(v, it, l) }, formatNumber(v, 3, l.decimal, l.group))
        }
        write("fmt_num.json", num)
        val ints = listOf(0L, 1L, -1L, 7L, 999L, 1000L, -1000L, 123456L, 1234567890L, -987654321L, 9007199254740993L, Long.MAX_VALUE, Long.MIN_VALUE) +
            (1..100).map { r.nextLong(-1_000_000_000_000L, 1_000_000_000_000L) }
        write("fmt_int.json", ints.flatMap { v -> Lang.BUILT_IN.map { l -> listOf(v.toString(), l.code, Fmt.int(v, l)) } })
        write("fmt_two.json", listOf(-5L, 0L, 5L, 9L, 10L, 59L, 100L).map { listOf(it, Fmt.two(it)) })
        val texts = listOf(
            "", "abc", "-5", "0", "1", "1.5", "1,5", "1,234.5", "1.234,5", "1.500", "1,500", "1 500", "1 500,25", "1 234 567",
            "1.234.567", "1,234,567", "1,234,567.89", "1.234.567,89", "١٢٣٤", "١٢٫٥", "١٬٢٣٤٫٥", "۱۲۳۴", "۱۲٫۵", "১২৩৪", "১২.৫",
            "12'345.5", "  42  ", "4 2", "1e3", "1E-2", "NaN", "Infinity", "-0", "0.0", ".5", "5.", "1.2.3", "1,2,3", "12,3456",
            "1.2345", "1,234", "1.234", "١،٢٣٤", "1\t000", "+7", "0x10", "1_000", "٣٫١٤١٥٩", "99999999999999999999", "1d", "2f",
            "1 000", "1 000", "12.50 SAR", "١٠٠٠٠٠٠",
        )
        val parse = Rows()
        for (t in texts) for (l in Lang.BUILT_IN) parse += listOf(t, l.code, Fmt.parse(t, l))
        for (v in all.filter { it.isFinite() && it > 0 }) for (l in Lang.BUILT_IN) for (dg in 0..2) {
            val s = Fmt.num(v, dg, l)
            parse += listOf(s, l.code, Fmt.parse(s, l))
        }
        write("fmt_parse.json", parse)
    }
}
