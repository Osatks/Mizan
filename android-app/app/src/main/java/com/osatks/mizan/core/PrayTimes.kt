package com.osatks.mizan.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Method(val key: String, val name: String, val fajr: Double, val ishaAngle: Double?, val ishaMinutes: Int?)

data class PrayerTimes(
    val fajr: Double, val sunrise: Double, val dhuhr: Double,
    val asr: Double, val maghrib: Double, val isha: Double
) {
    operator fun get(key: String): Double = when (key) {
        "fajr" -> fajr; "sunrise" -> sunrise; "dhuhr" -> dhuhr
        "asr" -> asr; "maghrib" -> maghrib; else -> isha
    }
}

data class City(val name: String, val country: String, val lat: Double, val lng: Double, val tz: String, val method: String)

/** حساب مواقيت الصلاة فلكياً من موضع الشمس */
object PrayTimes {
    val PRAYERS = listOf("fajr" to "الفجر", "sunrise" to "الشروق", "dhuhr" to "الظهر", "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء")

    val METHODS = listOf(
        Method("makkah", "أم القرى (السعودية)", 18.5, null, 90),
        Method("mwl", "رابطة العالم الإسلامي", 18.0, 17.0, null),
        Method("egypt", "الهيئة المصرية العامة للمساحة", 19.5, 17.5, null),
        Method("dubai", "الإمارات (دبي)", 18.2, 18.2, null),
        Method("kuwait", "الكويت", 18.0, 17.5, null),
        Method("qatar", "قطر", 18.0, null, 90),
        Method("jordan", "الأردن وفلسطين", 18.0, 18.0, null),
        Method("morocco", "المغرب", 19.0, 17.0, null),
        Method("algeria", "الجزائر", 18.0, 17.0, null),
        Method("tunisia", "تونس", 18.0, 18.0, null),
        Method("karachi", "جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0, null),
        Method("turkey", "رئاسة الشؤون الدينية التركية", 18.0, 17.0, null),
        Method("isna", "أمريكا الشمالية (ISNA)", 15.0, 15.0, null),
        Method("france", "اتحاد المنظمات الإسلامية بفرنسا", 12.0, 12.0, null),
    )

    fun method(key: String): Method = METHODS.firstOrNull { it.key == key } ?: METHODS[1]

    private fun dtr(d: Double) = Math.toRadians(d)
    private fun rtd(r: Double) = Math.toDegrees(r)
    private fun dsin(d: Double) = sin(dtr(d))
    private fun dcos(d: Double) = cos(dtr(d))
    private fun dtan(d: Double) = tan(dtr(d))
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)

    fun tzOffset(tz: String, date: LocalDate): Double = try {
        val instant = date.atTime(12, 0).atZone(ZoneId.of(tz)).toInstant()
        ZoneId.of(tz).rules.getOffset(instant).totalSeconds / 3600.0
    } catch (e: Exception) {
        ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 3600.0
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0); val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = rtd(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val decl = rtd(asin(dsin(e) * dsin(l)))
        val eqt = q / 15.0 - fixHour(ra)
        return decl to eqt
    }

    fun compute(date: LocalDate, lat: Double, lng: Double, tz: String, methodKey: String, hanafi: Boolean): PrayerTimes {
        val m = method(methodKey)
        val offset = tzOffset(tz, date)
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)

        fun midDay(t: Double) = fixHour(12 - sunPosition(jDate + t).second)
        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean = false): Double {
            val decl = sunPosition(jDate + t).first
            val noon = midDay(t)
            val x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if (x < -1 || x > 1) return Double.NaN
            val tt = rtd(acos(x)) / 15.0
            return noon + if (ccw) -tt else tt
        }
        fun asrTime(factor: Double, t: Double): Double {
            val decl = sunPosition(jDate + t).first
            val angle = -rtd(atan(1.0 / (factor + dtan(abs(lat - decl)))))
            return sunAngleTime(angle, t)
        }

        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0; var asr = 13.0; var sunset = 18.0; var isha = 18.0
        repeat(2) {
            fun p(v: Double) = (if (v.isNaN()) 12.0 else v) / 24.0
            val nf = sunAngleTime(m.fajr, p(fajr), true)
            val ns = sunAngleTime(0.833, p(sunrise), true)
            val nd = midDay(p(dhuhr))
            val na = asrTime(if (hanafi) 2.0 else 1.0, p(asr))
            val nss = sunAngleTime(0.833, p(sunset))
            val ni = if (m.ishaAngle != null) sunAngleTime(m.ishaAngle, p(isha)) else Double.NaN
            fajr = nf; sunrise = ns; dhuhr = nd; asr = na; sunset = nss; isha = ni
        }
        val adj = offset - lng / 15.0
        fajr += adj; sunrise += adj; dhuhr += adj; asr += adj; sunset += adj; isha += adj
        val maghrib = sunset
        if (m.ishaMinutes != null) {
            val ramadan = Hijri.toHijri(date)?.m == 9 && m.key == "makkah"
            isha = maghrib + (if (ramadan) 120 else m.ishaMinutes) / 60.0
        }

        // خطوط العرض العليا: طريقة الزاوية
        val night = fixHour(sunrise - sunset + 24)
        val fajrMax = m.fajr / 60.0 * night
        if (fajr.isNaN() || fixHour(sunrise - fajr) > fajrMax) fajr = sunrise - fajrMax
        if (m.ishaAngle != null) {
            val ishaMax = m.ishaAngle / 60.0 * night
            if (isha.isNaN() || fixHour(isha - sunset) > ishaMax) isha = sunset + ishaMax
        }
        dhuhr += 1.0 / 60.0 // احتياط دقيقة
        return PrayerTimes(fajr, sunrise, dhuhr, asr, maghrib, isha)
    }

    fun format(hours: Double, h24: Boolean): String {
        if (hours.isNaN()) return "—"
        val mins = (fixHour(hours) * 60).roundToInt()
        var h = (mins / 60) % 24
        val m = mins % 60
        val mm = m.toString().padStart(2, '0')
        if (h24) return "${h.toString().padStart(2, '0')}:$mm"
        val suf = if (h < 12) "ص" else "م"
        h %= 12; if (h == 0) h = 12
        return "$h:$mm $suf"
    }

    // ---------- القبلة ----------
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    fun qibla(lat: Double, lng: Double): Double {
        val dl = dtr(KAABA_LNG - lng); val p1 = dtr(lat); val p2 = dtr(KAABA_LAT)
        val b = atan2(sin(dl), cos(p1) * tan(p2) - sin(p1) * cos(dl))
        return fixAngle(rtd(b))
    }

    fun distanceKm(lat: Double, lng: Double): Double {
        val r = 6371.0
        val dLat = dtr(KAABA_LAT - lat); val dLng = dtr(KAABA_LNG - lng)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(dtr(lat)) * cos(dtr(KAABA_LAT)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(a))
    }

    val CITIES = listOf(
        City("مكة المكرمة", "السعودية", 21.4225, 39.8262, "Asia/Riyadh", "makkah"),
        City("المدينة المنورة", "السعودية", 24.4672, 39.6111, "Asia/Riyadh", "makkah"),
        City("الرياض", "السعودية", 24.7136, 46.6753, "Asia/Riyadh", "makkah"),
        City("جدة", "السعودية", 21.5433, 39.1728, "Asia/Riyadh", "makkah"),
        City("الدمام", "السعودية", 26.4207, 50.0888, "Asia/Riyadh", "makkah"),
        City("أبها", "السعودية", 18.2164, 42.5053, "Asia/Riyadh", "makkah"),
        City("دبي", "الإمارات", 25.2048, 55.2708, "Asia/Dubai", "dubai"),
        City("أبوظبي", "الإمارات", 24.4539, 54.3773, "Asia/Dubai", "dubai"),
        City("الشارقة", "الإمارات", 25.3463, 55.4209, "Asia/Dubai", "dubai"),
        City("الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait", "kuwait"),
        City("الدوحة", "قطر", 25.2854, 51.531, "Asia/Qatar", "qatar"),
        City("المنامة", "البحرين", 26.2285, 50.586, "Asia/Bahrain", "makkah"),
        City("مسقط", "عُمان", 23.588, 58.3829, "Asia/Muscat", "mwl"),
        City("القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo", "egypt"),
        City("الإسكندرية", "مصر", 31.2001, 29.9187, "Africa/Cairo", "egypt"),
        City("أسوان", "مصر", 24.0889, 32.8998, "Africa/Cairo", "egypt"),
        City("عمّان", "الأردن", 31.9454, 35.9284, "Asia/Amman", "jordan"),
        City("القدس", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", "jordan"),
        City("غزة", "فلسطين", 31.5017, 34.4668, "Asia/Gaza", "jordan"),
        City("بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut", "mwl"),
        City("دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus", "mwl"),
        City("حلب", "سوريا", 36.2021, 37.1343, "Asia/Damascus", "mwl"),
        City("بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad", "mwl"),
        City("البصرة", "العراق", 30.5085, 47.7804, "Asia/Baghdad", "mwl"),
        City("أربيل", "العراق", 36.1912, 44.0092, "Asia/Baghdad", "mwl"),
        City("صنعاء", "اليمن", 15.3694, 44.191, "Asia/Aden", "mwl"),
        City("عدن", "اليمن", 12.7855, 45.0187, "Asia/Aden", "mwl"),
        City("الخرطوم", "السودان", 15.5007, 32.5599, "Africa/Khartoum", "egypt"),
        City("طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli", "egypt"),
        City("بنغازي", "ليبيا", 32.1167, 20.0667, "Africa/Tripoli", "egypt"),
        City("تونس", "تونس", 36.8065, 10.1815, "Africa/Tunis", "tunisia"),
        City("الجزائر", "الجزائر", 36.7538, 3.0588, "Africa/Algiers", "algeria"),
        City("وهران", "الجزائر", 35.6971, -0.6308, "Africa/Algiers", "algeria"),
        City("الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca", "morocco"),
        City("الدار البيضاء", "المغرب", 33.5731, -7.5898, "Africa/Casablanca", "morocco"),
        City("مراكش", "المغرب", 31.6295, -7.9811, "Africa/Casablanca", "morocco"),
        City("نواكشوط", "موريتانيا", 18.0735, -15.9582, "Africa/Nouakchott", "mwl"),
        City("مقديشو", "الصومال", 2.0469, 45.3182, "Africa/Mogadishu", "mwl"),
        City("جيبوتي", "جيبوتي", 11.5721, 43.1456, "Africa/Djibouti", "mwl"),
        City("إسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul", "turkey"),
        City("كراتشي", "باكستان", 24.8607, 67.0011, "Asia/Karachi", "karachi"),
        City("كوالالمبور", "ماليزيا", 3.139, 101.6869, "Asia/Kuala_Lumpur", "mwl"),
        City("جاكرتا", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta", "mwl"),
        City("لندن", "بريطانيا", 51.5074, -0.1278, "Europe/London", "mwl"),
        City("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris", "france"),
        City("برلين", "ألمانيا", 52.52, 13.405, "Europe/Berlin", "mwl"),
        City("ستوكهولم", "السويد", 59.3293, 18.0686, "Europe/Stockholm", "mwl"),
        City("نيويورك", "الولايات المتحدة", 40.7128, -74.006, "America/New_York", "isna"),
        City("تورونتو", "كندا", 43.6532, -79.3832, "America/Toronto", "isna"),
        City("سيدني", "أستراليا", -33.8688, 151.2093, "Australia/Sydney", "mwl"),
    )

    /** أقرب مدينة معروفة لاقتراح طريقة الحساب */
    fun nearestCity(lat: Double, lng: Double): Pair<City, Double> =
        CITIES.map { it to ((it.lat - lat) * (it.lat - lat) + (it.lng - lng) * (it.lng - lng)) }.minBy { it.second }
}
