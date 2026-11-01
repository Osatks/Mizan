package com.osatks.mizan.core

import kotlin.math.PI
import kotlinx.datetime.LocalDate
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

/**
 * طريقة حساب: زاوية الفجر، وزاوية العشاء أو مدته بعد المغرب، و [maghribAngle] للطرق التي تؤخر المغرب بزاوية (طهران).
 * [minutes]: ما تضيفه الجهة الرسمية من دقائق إلى كل وقت بعد حسابه (احتياط وتقريب)، فيطابق الوقتُ جدولَها المنشور؛
 * مفاتيحها fajr و sunrise و dhuhr و asr و maghrib و isha، والغائب صفر. قيمها من مقارنة الحساب بالجدول الرسمي يوماً بيوم
 * (لذلك قد تكون كسوراً: تجمع الاحتياط وطريقة التقريب في رقم واحد). [hanafi]: العصر بمثلَي الظل هو المعتمد رسمياً
 * في بلاد الطريقة، ما لم يختر المستخدم غيره.
 */
data class Method(
    val key: String, val fajr: Double, val ishaAngle: Double?, val ishaMinutes: Int?, val maghribAngle: Double? = null,
    val minutes: Map<String, Double> = DHUHR_PRECAUTION, val hanafi: Boolean = false,
) {
    fun minutes(prayer: String): Double = minutes[prayer] ?: 0.0

    companion object {
        /** ما لم يُطابَق بجدول رسمي: دقيقة احتياط للظهر وحده (كما كان التطبيق يفعل لكل الطرق) */
        val DHUHR_PRECAUTION = mapOf("dhuhr" to 1.0)
    }
}

data class PrayerTimes(
    val fajr: Double, val sunrise: Double, val dhuhr: Double,
    val asr: Double, val maghrib: Double, val isha: Double,
    /** الغروب، ونهاية وقت المغرب (مغيب الشفق)، ومنتصف الليل (نهاية وقت الاختيار للعشاء)؛ NaN إن لم تُحسب */
    val sunset: Double = Double.NaN, val maghribEnd: Double = Double.NaN, val midnight: Double = Double.NaN,
) {
    operator fun get(key: String): Double = when (key) {
        "fajr" -> fajr; "sunrise" -> sunrise; "dhuhr" -> dhuhr
        "asr" -> asr; "maghrib" -> maghrib; else -> isha
    }

    /**
     * نهاية وقت الصلاة: الفجر بطلوع الشمس، والظهر بدخول العصر، والعصر بالغروب، والمغرب بمغيب الشفق،
     * والعشاء (وقت الاختيار) بمنتصف الليل. NaN للشروق أو حين لا يمكن الحساب.
     */
    fun end(key: String): Double = when (key) {
        "fajr" -> sunrise
        "dhuhr" -> asr
        "asr" -> if (sunset.isNaN()) maghrib else sunset
        "maghrib" -> if (maghribEnd.isNaN()) isha else maghribEnd
        "isha" -> midnight
        else -> Double.NaN
    }
}

/** مدينة معروفة: key لاسمها في ملفات اللغة، و cc رمز الدولة (ISO 3166) */
data class City(val key: String, val cc: String, val lat: Double, val lng: Double, val tz: String, val method: String)

/** حساب مواقيت الصلاة فلكياً من موضع الشمس */
object PrayTimes {
    val PRAYERS = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    val METHODS = listOf(
        Method("makkah", 18.5, null, 90),
        Method("mwl", 18.0, 17.0, null),
        Method("egypt", 19.5, 17.5, null),
        Method("dubai", 18.2, 18.2, null),
        Method("kuwait", 18.0, 17.5, null),
        Method("qatar", 18.0, null, 90),
        // وزارة الأوقاف الأردنية (awqaf.gov.jo، جدول عمّان 1–10 تشرين الثاني 2026): الشروق قبل الفلكي بست دقائق وربع،
        // والمغرب بعد الغروب بست دقائق ونصف، والبقية تقريب إلى الدقيقة التالية
        Method(
            "jordan", 18.0, 18.0, null,
            minutes = mapOf("fajr" to -0.25, "sunrise" to -6.27, "dhuhr" to 0.36, "asr" to 0.4, "maghrib" to 6.45, "isha" to 0.34),
        ),
        Method("morocco", 19.0, 17.0, null),
        Method("algeria", 18.0, 17.0, null),
        Method("tunisia", 18.0, 18.0, null),
        Method("karachi", 18.0, 18.0, null),
        Method("turkey", 18.0, 17.0, null),
        Method("tehran", 17.7, 14.0, null, 4.5),
        Method("indonesia", 20.0, 18.0, null),
        Method("malaysia", 20.0, 18.0, null),
        Method("singapore", 20.0, 18.0, null),
        Method("russia", 16.0, 15.0, null),
        Method("isna", 15.0, 15.0, null),
        Method("france", 12.0, 12.0, null),
    )

    fun method(key: String): Method = METHODS.firstOrNull { it.key == key } ?: METHODS[1]

    private fun dtr(d: Double) = d * PI / 180.0
    private fun rtd(r: Double) = r * 180.0 / PI
    private fun dsin(d: Double) = sin(dtr(d))
    private fun dcos(d: Double) = cos(dtr(d))
    private fun dtan(d: Double) = tan(dtr(d))
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)

    fun tzOffset(tz: String, date: LocalDate): Double = tzOffsetHours(tz, date.atTime(12, 0))

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

        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0; var asr = 13.0; var sunset = 18.0; var maghrib = 18.0; var isha = 18.0
        // مغيب الشفق الأحمر (17°) عند الجمهور، والأبيض (18°) عند الحنفية: نهاية وقت المغرب
        var shafaq = 19.0
        val shafaqAngle = if (hanafi) 18.0 else 17.0
        repeat(2) {
            fun p(v: Double) = (if (v.isNaN()) 12.0 else v) / 24.0
            val nf = sunAngleTime(m.fajr, p(fajr), true)
            val ns = sunAngleTime(0.833, p(sunrise), true)
            val nd = midDay(p(dhuhr))
            val na = asrTime(if (hanafi) 2.0 else 1.0, p(asr))
            val nss = sunAngleTime(0.833, p(sunset))
            val nm = if (m.maghribAngle != null) sunAngleTime(m.maghribAngle, p(maghrib)) else nss
            val ni = if (m.ishaAngle != null) sunAngleTime(m.ishaAngle, p(isha)) else Double.NaN
            val nsh = sunAngleTime(shafaqAngle, p(shafaq))
            fajr = nf; sunrise = ns; dhuhr = nd; asr = na; sunset = nss; maghrib = nm; isha = ni; shafaq = nsh
        }
        val adj = offset - lng / 15.0
        fajr += adj; sunrise += adj; dhuhr += adj; asr += adj; sunset += adj; maghrib += adj; isha += adj; shafaq += adj
        if (maghrib.isNaN()) maghrib = sunset

        // خطوط العرض العليا: طريقة الزاوية
        val night = fixHour(sunrise - sunset + 24)
        val fajrMax = m.fajr / 60.0 * night
        if (fajr.isNaN() || fixHour(sunrise - fajr) > fajrMax) fajr = sunrise - fajrMax
        if (m.ishaAngle != null) {
            val ishaMax = m.ishaAngle / 60.0 * night
            if (isha.isNaN() || fixHour(isha - sunset) > ishaMax) isha = sunset + ishaMax
        }
        // منتصف الليل من الفجر الفلكي (قبل دقائق الجهة الرسمية)
        val astroFajr = fajr

        // دقائق الجهة الرسمية، ثم العشاء بالمدة الثابتة بعد المغرب كما تنشره الجهة (أم القرى وقطر)
        fun mins(prayer: String) = m.minutes(prayer) / 60.0
        fajr += mins("fajr"); sunrise += mins("sunrise"); dhuhr += mins("dhuhr"); asr += mins("asr"); maghrib += mins("maghrib")
        if (m.ishaMinutes != null) {
            val ramadan = Hijri.toHijri(date)?.m == 9 && m.key == "makkah"
            isha = maghrib + (if (ramadan) 120 else m.ishaMinutes) / 60.0
        }
        isha += mins("isha")

        // نهاية المغرب: في الطرق التي تحسب العشاء بزاوية يدخل العشاء بمغيب الشفق نفسه، وفي طرق المدة الثابتة
        // (أم القرى وقطر) قد يغيب الشفق قبل موعد العشاء المعلن بكثير
        val maghribEnd = when {
            m.ishaAngle != null || shafaq.isNaN() -> isha
            else -> minOf(isha, shafaq)
        }
        // منتصف الليل الشرعي: منتصف ما بين الغروب والفجر التالي (فجر الغد ≈ فجر اليوم + 24 ساعة)
        val midnight = if (astroFajr.isNaN()) Double.NaN else sunset + (astroFajr + 24 - sunset) / 2
        return PrayerTimes(fajr, sunrise, dhuhr, asr, maghrib, isha, sunset, maghribEnd, midnight)
    }

    fun format(hours: Double, h24: Boolean, am: String = "AM", pm: String = "PM"): String {
        if (hours.isNaN()) return "—"
        val mins = (fixHour(hours) * 60).roundToInt()
        var h = (mins / 60) % 24
        val m = mins % 60
        val mm = m.toString().padStart(2, '0')
        if (h24) return "${h.toString().padStart(2, '0')}:$mm"
        val suf = if (h < 12) am else pm
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
        City("makkah", "SA", 21.4225, 39.8262, "Asia/Riyadh", "makkah"),
        City("madinah", "SA", 24.4672, 39.6111, "Asia/Riyadh", "makkah"),
        City("riyadh", "SA", 24.7136, 46.6753, "Asia/Riyadh", "makkah"),
        City("jeddah", "SA", 21.5433, 39.1728, "Asia/Riyadh", "makkah"),
        City("dammam", "SA", 26.4207, 50.0888, "Asia/Riyadh", "makkah"),
        City("abha", "SA", 18.2164, 42.5053, "Asia/Riyadh", "makkah"),
        City("dubai", "AE", 25.2048, 55.2708, "Asia/Dubai", "dubai"),
        City("abudhabi", "AE", 24.4539, 54.3773, "Asia/Dubai", "dubai"),
        City("sharjah", "AE", 25.3463, 55.4209, "Asia/Dubai", "dubai"),
        City("kuwait", "KW", 29.3759, 47.9774, "Asia/Kuwait", "kuwait"),
        City("doha", "QA", 25.2854, 51.531, "Asia/Qatar", "qatar"),
        City("manama", "BH", 26.2285, 50.586, "Asia/Bahrain", "makkah"),
        City("muscat", "OM", 23.588, 58.3829, "Asia/Muscat", "mwl"),
        City("cairo", "EG", 30.0444, 31.2357, "Africa/Cairo", "egypt"),
        City("alexandria", "EG", 31.2001, 29.9187, "Africa/Cairo", "egypt"),
        City("aswan", "EG", 24.0889, 32.8998, "Africa/Cairo", "egypt"),
        City("amman", "JO", 31.9454, 35.9284, "Asia/Amman", "jordan"),
        City("jerusalem", "PS", 31.7683, 35.2137, "Asia/Jerusalem", "jordan"),
        City("gaza", "PS", 31.5017, 34.4668, "Asia/Gaza", "jordan"),
        City("beirut", "LB", 33.8938, 35.5018, "Asia/Beirut", "mwl"),
        City("damascus", "SY", 33.5138, 36.2765, "Asia/Damascus", "mwl"),
        City("aleppo", "SY", 36.2021, 37.1343, "Asia/Damascus", "mwl"),
        City("baghdad", "IQ", 33.3152, 44.3661, "Asia/Baghdad", "mwl"),
        City("basra", "IQ", 30.5085, 47.7804, "Asia/Baghdad", "mwl"),
        City("erbil", "IQ", 36.1912, 44.0092, "Asia/Baghdad", "mwl"),
        City("sanaa", "YE", 15.3694, 44.191, "Asia/Aden", "mwl"),
        City("aden", "YE", 12.7855, 45.0187, "Asia/Aden", "mwl"),
        City("khartoum", "SD", 15.5007, 32.5599, "Africa/Khartoum", "egypt"),
        City("tripoli", "LY", 32.8872, 13.1913, "Africa/Tripoli", "egypt"),
        City("benghazi", "LY", 32.1167, 20.0667, "Africa/Tripoli", "egypt"),
        City("tunis", "TN", 36.8065, 10.1815, "Africa/Tunis", "tunisia"),
        City("algiers", "DZ", 36.7538, 3.0588, "Africa/Algiers", "algeria"),
        City("oran", "DZ", 35.6971, -0.6308, "Africa/Algiers", "algeria"),
        City("rabat", "MA", 34.0209, -6.8416, "Africa/Casablanca", "morocco"),
        City("casablanca", "MA", 33.5731, -7.5898, "Africa/Casablanca", "morocco"),
        City("marrakesh", "MA", 31.6295, -7.9811, "Africa/Casablanca", "morocco"),
        City("nouakchott", "MR", 18.0735, -15.9582, "Africa/Nouakchott", "mwl"),
        City("mogadishu", "SO", 2.0469, 45.3182, "Africa/Mogadishu", "mwl"),
        City("djibouti", "DJ", 11.5721, 43.1456, "Africa/Djibouti", "mwl"),
        City("istanbul", "TR", 41.0082, 28.9784, "Europe/Istanbul", "turkey"),
        City("ankara", "TR", 39.9334, 32.8597, "Europe/Istanbul", "turkey"),
        City("karachi", "PK", 24.8607, 67.0011, "Asia/Karachi", "karachi"),
        City("lahore", "PK", 31.5204, 74.3587, "Asia/Karachi", "karachi"),
        City("islamabad", "PK", 33.6844, 73.0479, "Asia/Karachi", "karachi"),
        City("delhi", "IN", 28.6139, 77.209, "Asia/Kolkata", "karachi"),
        City("mumbai", "IN", 19.076, 72.8777, "Asia/Kolkata", "karachi"),
        City("hyderabad", "IN", 17.385, 78.4867, "Asia/Kolkata", "karachi"),
        City("dhaka", "BD", 23.8103, 90.4125, "Asia/Dhaka", "karachi"),
        City("chittagong", "BD", 22.3569, 91.7832, "Asia/Dhaka", "karachi"),
        City("kabul", "AF", 34.5553, 69.2075, "Asia/Kabul", "karachi"),
        City("tehran", "IR", 35.6892, 51.389, "Asia/Tehran", "tehran"),
        City("mashhad", "IR", 36.2605, 59.6168, "Asia/Tehran", "tehran"),
        City("tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent", "mwl"),
        City("baku", "AZ", 40.4093, 49.8671, "Asia/Baku", "mwl"),
        City("almaty", "KZ", 43.222, 76.8512, "Asia/Almaty", "mwl"),
        City("moscow", "RU", 55.7558, 37.6173, "Europe/Moscow", "russia"),
        City("kazan", "RU", 55.7963, 49.1088, "Europe/Moscow", "russia"),
        City("grozny", "RU", 43.3178, 45.6949, "Europe/Moscow", "russia"),
        City("kualalumpur", "MY", 3.139, 101.6869, "Asia/Kuala_Lumpur", "malaysia"),
        City("penang", "MY", 5.4141, 100.3288, "Asia/Kuala_Lumpur", "malaysia"),
        City("singapore", "SG", 1.3521, 103.8198, "Asia/Singapore", "singapore"),
        City("brunei", "BN", 4.9031, 114.9398, "Asia/Brunei", "singapore"),
        City("jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta", "indonesia"),
        City("surabaya", "ID", -7.2575, 112.7521, "Asia/Jakarta", "indonesia"),
        City("bandung", "ID", -6.9175, 107.6191, "Asia/Jakarta", "indonesia"),
        City("medan", "ID", 3.5952, 98.6722, "Asia/Jakarta", "indonesia"),
        City("makassar", "ID", -5.1477, 119.4327, "Asia/Makassar", "indonesia"),
        City("lagos", "NG", 6.5244, 3.3792, "Africa/Lagos", "mwl"),
        City("kano", "NG", 12.0022, 8.592, "Africa/Lagos", "mwl"),
        City("dakar", "SN", 14.7167, -17.4677, "Africa/Dakar", "mwl"),
        City("sarajevo", "BA", 43.8563, 18.4131, "Europe/Sarajevo", "mwl"),
        City("london", "GB", 51.5074, -0.1278, "Europe/London", "mwl"),
        City("birmingham", "GB", 52.4862, -1.8904, "Europe/London", "mwl"),
        City("paris", "FR", 48.8566, 2.3522, "Europe/Paris", "france"),
        City("marseille", "FR", 43.2965, 5.3698, "Europe/Paris", "france"),
        City("lyon", "FR", 45.764, 4.8357, "Europe/Paris", "france"),
        City("brussels", "BE", 50.8503, 4.3517, "Europe/Brussels", "mwl"),
        City("amsterdam", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "mwl"),
        City("berlin", "DE", 52.52, 13.405, "Europe/Berlin", "mwl"),
        City("hamburg", "DE", 53.5511, 9.9937, "Europe/Berlin", "mwl"),
        City("cologne", "DE", 50.9375, 6.9603, "Europe/Berlin", "mwl"),
        City("munich", "DE", 48.1351, 11.582, "Europe/Berlin", "mwl"),
        City("vienna", "AT", 48.2082, 16.3738, "Europe/Vienna", "mwl"),
        City("stockholm", "SE", 59.3293, 18.0686, "Europe/Stockholm", "mwl"),
        City("newyork", "US", 40.7128, -74.006, "America/New_York", "isna"),
        City("chicago", "US", 41.8781, -87.6298, "America/Chicago", "isna"),
        City("losangeles", "US", 34.0522, -118.2437, "America/Los_Angeles", "isna"),
        City("toronto", "CA", 43.6532, -79.3832, "America/Toronto", "isna"),
        City("montreal", "CA", 45.5017, -73.5673, "America/Toronto", "isna"),
        City("sydney", "AU", -33.8688, 151.2093, "Australia/Sydney", "mwl"),
    )

    fun city(key: String?): City? = CITIES.firstOrNull { it.key == key }

    /** أقرب مدينة معروفة لاقتراح طريقة الحساب */
    fun nearestCity(lat: Double, lng: Double): Pair<City, Double> =
        CITIES.map { it to ((it.lat - lat) * (it.lat - lat) + (it.lng - lng) * (it.lng - lng)) }.minBy { it.second }
}
