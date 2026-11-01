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
 * في بلاد الطريقة، ما لم يختر المستخدم غيره. [elevation]: الجهة تحسب الشروق والغروب بارتفاع المكان عن البحر
 * (يُرى الأفق أبعد من المرتفع، فتشرق الشمس عليه قبلُ وتغرب بعدُ): معامل انخفاض الأفق، 1 للصيغة المعتادة
 * (0.0347° × جذر الارتفاع بالأمتار)، وصفر لمن لا يعتمد الارتفاع. [dailySun]: موضع الشمس مرة واحدة لليوم عند منتصف
 * الليل بتوقيت غرينتش (كما تحسب رئاسة الشؤون الدينية التركية) لا عند كل وقت.
 * العروض العليا (حين لا تبلغ الشمس زاوية الفجر أو العشاء، أو يطول الشفق): [fajrFallback] و [ishaFallback] مدة ثابتة
 * قبل الشروق وبعد الغروب؛ [seasonal] حدّ لجنة رؤية الهلال العالمية الموسمي (الفجر لا يسبقه والعشاء لا يتأخر عنه)؛
 * [depression] زاويتا الفجر والعشاء نسبة من أعمق انخفاض للشمس تلك الليلة إن قلّتا عنه (مؤتمر إسطنبول 2021)؛
 * وإلا طريقة الزاوية (نسبة من الليل). [ramadan]: دقائق تزيدها الجهة في رمضان وحده (احتياط الإفطار في قطر).
 */
data class Method(
    val key: String, val fajr: Double, val ishaAngle: Double?, val ishaMinutes: Int?, val maghribAngle: Double? = null,
    val minutes: Map<String, Double> = DHUHR_PRECAUTION, val hanafi: Boolean = false, val elevation: Double = 0.0,
    val dailySun: Boolean = false, val fajrFallback: Double? = null, val ishaFallback: Double? = null,
    val seasonal: Boolean = false, val depression: Pair<Double, Double>? = null,
    val ramadan: Map<String, Double> = emptyMap(),
) {
    fun minutes(prayer: String, inRamadan: Boolean = false): Double =
        (minutes[prayer] ?: 0.0) + (if (inRamadan) ramadan[prayer] ?: 0.0 else 0.0)

    companion object {
        /** ما لم يُطابَق بجدول رسمي: دقيقة احتياط للظهر وحده (كما كان التطبيق يفعل لكل الطرق) */
        val DHUHR_PRECAUTION = mapOf("dhuhr" to 1.0)
        /** الجدول الرسمي هو الحساب الفلكي نفسه، بلا دقائق */
        val NONE = emptyMap<String, Double>()
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

/**
 * مدينة معروفة: key لاسمها في ملفات اللغة، و cc رمز الدولة (ISO 3166)، و [elev] ارتفاعها عن البحر بالأمتار
 * حيث تحسب الجهة الرسمية الشروق والغروب به ([Method.elevation])
 */
data class City(val key: String, val cc: String, val lat: Double, val lng: Double, val tz: String, val method: String, val elev: Int = 0)

/** حساب مواقيت الصلاة فلكياً من موضع الشمس */
object PrayTimes {
    val PRAYERS = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")

    val METHODS = listOf(
        // تقويم أم القرى (مدينة الملك عبدالعزيز للعلوم والتقنية، كل أيام 2026 في ست مدن): لا دقائق، والأوقات مقرّبة إلى
        // الدقيقة التالية والشروق إلى السابقة؛ العشاء بعد المغرب بتسعين دقيقة، وبعشرين ومئة في رمضان
        Method("makkah", 18.5, null, 90, minutes = mapOf("fajr" to 0.5, "sunrise" to -0.5, "dhuhr" to 0.5, "asr" to 0.5, "maghrib" to 0.5)),
        Method("mwl", 18.0, 17.0, null),
        // الهيئة المصرية العامة للمساحة (esa.gov.eg، كل أيام 2026 في القاهرة والإسكندرية وأسوان): الحساب الفلكي بلا دقائق
        Method("egypt", 19.5, 17.5, null, minutes = Method.NONE),
        // الهيئة العامة للشؤون الإسلامية والأوقاف (التقويم الهجري الإماراتي 1447، أبوظبي، 355 يوماً): الشروق والمغرب كأن
        // الشمس دون الأفق بـ 1.4° تقريباً
        Method(
            "dubai", 18.0, 18.0, null, 1.38,
            minutes = mapOf("fajr" to -1.1, "sunrise" to -3.6, "dhuhr" to 2.3, "asr" to 0.45, "maghrib" to 0.3, "isha" to 0.3),
        ),
        Method("kuwait", 18.0, 17.5, null),
        // وزارة الأوقاف والشؤون الإسلامية (islam.gov.qa، كل أيام 2026): المغرب بعد الغروب بدقيقتين، وبثلاث في رمضان،
        // والعشاء بعده بتسعين دقيقة
        Method("qatar", 18.0, null, 90, minutes = mapOf("fajr" to -1.1, "maghrib" to 1.95), ramadan = mapOf("maghrib" to 1.05)),
        // تقويم «الزبارة والبحرين» (المجلس الأعلى للشؤون الإسلامية، منذ 1443هـ): العشاء بانخفاض الشمس 18° بدل تسعين دقيقة
        Method("bahrain", 18.0, 18.0, null, minutes = mapOf("fajr" to -0.9, "dhuhr" to 1.0, "maghrib" to 0.5, "isha" to -1.1)),
        // وزارة الأوقاف الأردنية (awqaf.gov.jo، جدول عمّان 1–10 تشرين الثاني 2026): الشروق قبل الفلكي بست دقائق وربع،
        // والمغرب بعد الغروب بست دقائق ونصف، والبقية تقريب إلى الدقيقة التالية
        Method(
            "jordan", 18.0, 18.0, null,
            minutes = mapOf("fajr" to -0.25, "sunrise" to -6.27, "dhuhr" to 0.36, "asr" to 0.4, "maghrib" to 6.45, "isha" to 0.34),
        ),
        // دار الإفتاء الفلسطينية (تقويم الأقصى الدائم، darifta.ps): الشروق قبل الفلكي بثلاث دقائق والمغرب بعد الغروب بست
        Method(
            "palestine", 18.0, 18.0, null,
            minutes = mapOf("fajr" to -0.25, "sunrise" to -3.0, "dhuhr" to -0.5, "asr" to -0.4, "maghrib" to 6.0, "isha" to -0.55),
        ),
        // دار الفتوى في الجمهورية اللبنانية (إمساكية رمضان، بيروت)
        Method("lebanon", 19.2, 17.3, null, minutes = mapOf("sunrise" to 1.35, "dhuhr" to 0.2, "asr" to -0.6, "maghrib" to 3.4)),
        // وزارة الأوقاف السورية (جداول المحافظات الرسمية، رمضان 2026): المغرب بعد الغروب بدقيقتين ونصف تقريباً
        Method(
            "syria", 18.0, 17.0, null,
            minutes = mapOf("fajr" to -0.4, "sunrise" to -0.6, "dhuhr" to 0.4, "asr" to 0.6, "maghrib" to 2.4, "isha" to 0.6),
        ),
        // دار الإفتاء الليبية (ifta.ly، طرابلس)
        Method(
            "libya", 18.0, 18.0, null,
            minutes = mapOf("fajr" to -2.6, "sunrise" to -0.8, "dhuhr" to 3.1, "asr" to -0.8, "maghrib" to 3.4, "isha" to 1.2),
        ),
        // وزارة الأوقاف والشؤون الإسلامية (habous.gov.ma، 61 يوماً في الرباط والدار البيضاء ومراكش): الظهر بعد الزوال
        // بخمس دقائق، والشروق والمغرب بارتفاع المدينة (انخفاض أفق أقل من المعتاد)
        Method(
            "morocco", 19.0, 17.0, null,
            minutes = mapOf("fajr" to -0.3, "sunrise" to -2.2, "dhuhr" to 5.2, "asr" to 0.25, "maghrib" to 3.4, "isha" to 0.2),
            elevation = 0.7,
        ),
        // وزارة الشؤون الدينية والأوقاف (إمساكيات رمضان الرسمية): المغرب بعد الغروب بثلاث دقائق والفجر مقرّب إلى الدقيقة التالية؛
        // الشروق والظهر والعصر والعشاء لم تُطابَق بجدول منشور
        Method("algeria", 18.0, 17.0, null, minutes = mapOf("fajr" to 0.5, "dhuhr" to 1.0, "maghrib" to 3.5)),
        // المعهد الوطني للرصد الجوي (meteo.tn، كل أيام 2026): الظهر بعد الزوال بسبع دقائق، والمغرب بعد الغروب بدقيقتين ونصف
        Method(
            "tunisia", 18.0, 18.0, null,
            minutes = mapOf("fajr" to -0.5, "sunrise" to -0.5, "dhuhr" to 7.0, "asr" to 0.0, "maghrib" to 2.5, "isha" to 0.5),
        ),
        // الحنفي هو المعتمد في باكستان والهند وبنغلاديش وأفغانستان (العصر بمثلَي الظل في جداول المساجد)
        Method("karachi", 18.0, 18.0, null, hanafi = true),
        // رئاسة الشؤون الدينية (namazvakitleri.diyanet.gov.tr، إسطنبول وأنقرة): «التمكين» −7 للشروق و+5 للظهر و+4 للعصر
        // و+7 للمغرب، وموضع الشمس مرة لليوم
        Method(
            "turkey", 18.0, 17.0, null,
            minutes = mapOf("sunrise" to -7.0, "dhuhr" to 5.0, "asr" to 4.0, "maghrib" to 7.0), dailySun = true,
        ),
        // الطريقة الموحدة لمؤتمر إسطنبول 2021 في أوروبا (ديانت، والهيئة الإسلامية في النمسا، والاتحاد الإسلامي في السويد):
        // الفجر 18° والعشاء 16° بالتمكين نفسه، وفي الصيف نسبة من أعمق انخفاض للشمس
        Method(
            "europe", 18.0, 16.0, null,
            minutes = mapOf("sunrise" to -7.0, "dhuhr" to 5.0, "asr" to 4.0, "maghrib" to 7.0), dailySun = true,
            depression = 0.66 to 0.62,
        ),
        // المشيخة الإسلامية في البوسنة والهرسك (vaktija.ba، كل أيام 2026)
        Method("bosnia", 18.0, 16.0, null, minutes = mapOf("sunrise" to -6.0, "dhuhr" to 1.0, "maghrib" to 6.0)),
        // معهد الجيوفيزياء بجامعة طهران: الحساب الفلكي بلا دقائق
        Method("tehran", 17.7, 14.0, null, 4.5, minutes = Method.NONE),
        // إدارة مسلمي القوقاز (qafqazislam.com، باكو): المغرب بانخفاض الشمس 3.5°، والعصر بمثلَي الظل
        Method(
            "azerbaijan", 16.0, 15.0, null, 3.5,
            minutes = mapOf("fajr" to -0.35, "sunrise" to -0.15, "asr" to 0.1, "maghrib" to 0.2, "isha" to 0.4), hanafi = true,
        ),
        // الإدارة الدينية لمسلمي أوزبكستان (muslim.uz، طشقند)
        Method("uzbekistan", 15.0, 15.0, null, minutes = mapOf("fajr" to -2.45, "dhuhr" to 5.0, "maghrib" to 3.9), hanafi = true),
        // الإدارة الدينية لمسلمي كازاخستان (muftyat.kz، كل أيام 2026): الشروق −3 والظهر والعصر والمغرب +3 كما في حسابها المنشور
        Method(
            "kazakhstan", 15.0, 15.0, null,
            minutes = mapOf("sunrise" to -3.0, "dhuhr" to 3.0, "asr" to 3.0, "maghrib" to 3.0), hanafi = true,
        ),
        // وزارة الشؤون الدينية (Kemenag، جدول SIHAT لسنة 2026 في خمس مدن): احتياط دقيقتين تقريباً لكل وقت،
        // والشروق والمغرب بارتفاع المكان (باندونغ)
        Method(
            "indonesia", 20.0, 18.0, null,
            minutes = mapOf("fajr" to 2.5, "sunrise" to -3.15, "dhuhr" to 3.55, "asr" to 2.55, "maghrib" to 3.2, "isha" to 2.5),
            elevation = 1.0,
        ),
        // JAKIM (e-Solat 2026، كوالالمبور وبينانغ): الفجر 18° منذ قرار مجلس الفتوى الوطني 2019، واحتياط دقيقتين تقريباً
        Method(
            "malaysia", 18.0, 18.0, null,
            minutes = mapOf("fajr" to 1.8, "sunrise" to -0.9, "dhuhr" to 2.2, "asr" to 1.7, "maghrib" to 1.7, "isha" to 1.8),
        ),
        // MUIS (بيانات 2026 الرسمية في data.gov.sg)
        Method(
            "singapore", 20.0, 18.0, null,
            minutes = mapOf("fajr" to 0.45, "sunrise" to 0.45, "dhuhr" to 1.55, "asr" to 0.45, "maghrib" to 0.55, "isha" to 0.5),
        ),
        // وزارة الشؤون الدينية في بروناي (جدول 2026): الحساب الفلكي ودقيقة احتياط للظهر
        Method("brunei", 20.0, 18.0, null),
        // الإدارة الدينية لمسلمي روسيا الاتحادية (dumrf.ru، موسكو)
        Method(
            "russia", 18.0, 15.0, null,
            minutes = mapOf("fajr" to 0.35, "sunrise" to -4.55, "dhuhr" to 4.9, "asr" to -0.25, "maghrib" to 4.3, "isha" to -0.5),
        ),
        // الإدارة الدينية لمسلمي تتارستان (dumrt.ru، كل أيام 2026 في قازان): العصر الحنفي، وفي الصيف حين لا تبلغ الشمس 18°
        // الفجر قبل الشروق بساعتين والعشاء بعد الغروب بساعة ونصف
        Method(
            "tatarstan", 18.0, 15.0, null,
            minutes = mapOf("fajr" to -0.5, "sunrise" to 0.35, "dhuhr" to 0.4, "asr" to 0.8, "maghrib" to 0.55, "isha" to 0.4),
            hanafi = true, fajrFallback = 120.15, ishaFallback = 90.1,
        ),
        // جدول لندن الموحد (مسجد شرق لندن): الشروق −3 والظهر +5 والمغرب +3، والفجر والعشاء بحد لجنة رؤية الهلال الموسمي
        Method("uk", 18.0, 18.0, null, minutes = mapOf("sunrise" to -3.0, "dhuhr" to 5.0, "maghrib" to 3.0), seasonal = true),
        // المسجد الكبير في باريس: الفجر بحد لجنة رؤية الهلال الموسمي، والعشاء بعد الغروب بتسعين دقيقة
        Method("paris", 18.0, null, 87, minutes = mapOf("dhuhr" to 5.0, "asr" to -0.8, "maghrib" to 3.0), seasonal = true),
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

    /** [elev]: ارتفاع المكان عن البحر بالأمتار، يُحسب به الشروق والغروب في الطرق التي تعتمده ([Method.elevation]) */
    fun compute(date: LocalDate, lat: Double, lng: Double, tz: String, methodKey: String, hanafi: Boolean, elev: Int = 0): PrayerTimes =
        compute(date, lat, lng, tz, method(methodKey), hanafi, elev)

    fun compute(date: LocalDate, lat: Double, lng: Double, tz: String, m: Method, hanafi: Boolean, elev: Int = 0): PrayerTimes {
        // الأفق: 0.833° (انكسار الضوء ونصف قرص الشمس)، ويزيد بانخفاض الأفق للمرتفع: 0.0347° × جذر الارتفاع بالأمتار
        val horizon = 0.833 + if (elev > 0) m.elevation * 0.0347 * sqrt(elev.toDouble()) else 0.0
        val offset = tzOffset(tz, date)
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24.0)
        // موضع الشمس عند كل وقت، أو مرة لليوم عند منتصف الليل بتوقيت غرينتش ([Method.dailySun])
        val daily = if (m.dailySun) sunPosition(julian(date.year, date.monthValue, date.dayOfMonth)) else null
        fun sun(t: Double) = daily ?: sunPosition(jDate + t)

        fun midDay(t: Double) = fixHour(12 - sun(t).second)
        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean = false): Double {
            val decl = sun(t).first
            val noon = midDay(t)
            val x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if (x < -1 || x > 1) return Double.NaN
            val tt = rtd(acos(x)) / 15.0
            return noon + if (ccw) -tt else tt
        }
        fun asrTime(factor: Double, t: Double): Double {
            val decl = sun(t).first
            val angle = -rtd(atan(1.0 / (factor + dtan(abs(lat - decl)))))
            return sunAngleTime(angle, t)
        }

        // في العروض العليا صيفاً: الزاويتان نسبة من أعمق انخفاض للشمس تلك الليلة إن قلّتا عنه ([Method.depression])
        var fajrAngle = m.fajr
        var ishaAngle = m.ishaAngle
        m.depression?.let { (kf, ki) ->
            val decl = sunPosition(jDate + 0.5).first
            val deepest = 90 - abs(lat) - (if (lat >= 0) decl else -decl)
            fajrAngle = minOf(fajrAngle, kf * deepest)
            ishaAngle = ishaAngle?.let { minOf(it, ki * deepest) }
        }

        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0; var asr = 13.0; var sunset = 18.0; var maghrib = 18.0; var isha = 18.0
        // مغيب الشفق الأحمر (17°) عند الجمهور، والأبيض (18°) عند الحنفية: نهاية وقت المغرب
        var shafaq = 19.0
        val shafaqAngle = if (hanafi) 18.0 else 17.0
        repeat(2) {
            fun p(v: Double) = (if (v.isNaN()) 12.0 else v) / 24.0
            val nf = sunAngleTime(fajrAngle, p(fajr), true)
            val ns = sunAngleTime(horizon, p(sunrise), true)
            val nd = midDay(p(dhuhr))
            val na = asrTime(if (hanafi) 2.0 else 1.0, p(asr))
            val nss = sunAngleTime(horizon, p(sunset))
            val nm = if (m.maghribAngle != null) sunAngleTime(m.maghribAngle, p(maghrib)) else nss
            val ni = ishaAngle?.let { sunAngleTime(it, p(isha)) } ?: Double.NaN
            val nsh = sunAngleTime(shafaqAngle, p(shafaq))
            fajr = nf; sunrise = ns; dhuhr = nd; asr = na; sunset = nss; maghrib = nm; isha = ni; shafaq = nsh
        }
        val adj = offset - lng / 15.0
        fajr += adj; sunrise += adj; dhuhr += adj; asr += adj; sunset += adj; maghrib += adj; isha += adj; shafaq += adj
        if (maghrib.isNaN()) maghrib = sunset

        // خطوط العرض العليا
        val night = fixHour(sunrise - sunset + 24)
        when {
            // لجنة رؤية الهلال العالمية: فوق 55° سُبع الليل، ثم لا يسبق الفجرُ الحدَّ الموسمي ولا يتأخر العشاء عنه
            m.seasonal -> {
                if (abs(lat) >= 55) {
                    fajr = sunrise - night / 7
                    if (ishaAngle != null) isha = sunset + night / 7
                }
                val safeFajr = sunrise - seasonalTwilight(lat, date, morning = true) / 60.0
                if (fajr.isNaN() || safeFajr > fajr) fajr = safeFajr
                if (ishaAngle != null) {
                    val safeIsha = sunset + seasonalTwilight(lat, date, morning = false) / 60.0
                    if (isha.isNaN() || safeIsha < isha) isha = safeIsha
                }
            }
            // الزاويتان مخفّضتان أصلاً بانخفاض الشمس فتبلغهما دائماً
            m.depression != null -> Unit
            else -> {
                // مدة ثابتة إن نصّت الجهة عليها (للفجر والعشاء معاً ما دامت الشمس لا تبلغ زاوية الفجر)، وإلا طريقة الزاوية
                val noDawn = fajr.isNaN()
                if (m.fajrFallback != null) {
                    if (noDawn) fajr = sunrise - m.fajrFallback / 60.0
                } else {
                    val fajrMax = fajrAngle / 60.0 * night
                    if (fajr.isNaN() || fixHour(sunrise - fajr) > fajrMax) fajr = sunrise - fajrMax
                }
                if (ishaAngle != null) {
                    if (m.ishaFallback != null) {
                        if (noDawn || isha.isNaN()) isha = sunset + m.ishaFallback / 60.0
                    } else {
                        val ishaMax = ishaAngle / 60.0 * night
                        if (isha.isNaN() || fixHour(isha - sunset) > ishaMax) isha = sunset + ishaMax
                    }
                }
            }
        }
        // منتصف الليل من الفجر الفلكي (قبل دقائق الجهة الرسمية)
        val astroFajr = fajr

        // دقائق الجهة الرسمية (وزيادتها في رمضان)، ثم العشاء بالمدة الثابتة بعد المغرب كما تنشره الجهة (أم القرى وقطر)
        val inRamadan = (m.ramadan.isNotEmpty() || m.key == "makkah") && Hijri.toHijri(date)?.m == 9
        fun mins(prayer: String) = m.minutes(prayer, inRamadan) / 60.0
        fajr += mins("fajr"); sunrise += mins("sunrise"); dhuhr += mins("dhuhr"); asr += mins("asr"); maghrib += mins("maghrib")
        if (m.ishaMinutes != null) {
            isha = maghrib + (if (inRamadan && m.key == "makkah") 120 else m.ishaMinutes) / 60.0
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

    /**
     * حد الشفق الموسمي للجنة رؤية الهلال العالمية (Moonsighting Committee Worldwide) بالدقائق: قبل الشروق للفجر،
     * وبعد الغروب للعشاء (الشفق العام). خطّي بين أربع قيم في السنة تزيد مع خط العرض، كما في مكتبة adhan.
     */
    fun seasonalTwilight(lat: Double, date: LocalDate, morning: Boolean): Double {
        val l = abs(lat)
        val (a, b, c, d) = if (morning) {
            listOf(75 + 28.65 / 55 * l, 75 + 19.44 / 55 * l, 75 + 32.74 / 55 * l, 75 + 48.10 / 55 * l)
        } else {
            listOf(75 + 25.6 / 55 * l, 75 + 2.05 / 55 * l, 75 - 9.21 / 55 * l, 75 + 6.14 / 55 * l)
        }
        val y = date.year
        val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        val days = if (leap) 366 else 365
        val doy = date.dayOfYear
        // الأيام منذ الانقلاب الشتوي في نصف الكرة
        val dyy = if (lat >= 0) (doy + 10).let { if (it >= days) it - days else it }
        else (doy - (if (leap) 173 else 172)).let { if (it < 0) it + days else it }
        val adj = when {
            dyy < 91 -> a + (b - a) / 91.0 * dyy
            dyy < 137 -> b + (c - b) / 46.0 * (dyy - 91)
            dyy < 183 -> c + (d - c) / 46.0 * (dyy - 137)
            dyy < 229 -> d + (c - d) / 46.0 * (dyy - 183)
            dyy < 275 -> c + (b - c) / 46.0 * (dyy - 229)
            else -> b + (a - b) / 91.0 * (dyy - 275)
        }
        return (adj * 60).roundToInt() / 60.0 // مكتبة adhan تقرّب إلى الثانية
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
        City("manama", "BH", 26.2285, 50.586, "Asia/Bahrain", "bahrain"),
        City("muscat", "OM", 23.588, 58.3829, "Asia/Muscat", "mwl"),
        City("cairo", "EG", 30.0444, 31.2357, "Africa/Cairo", "egypt"),
        City("alexandria", "EG", 31.2001, 29.9187, "Africa/Cairo", "egypt"),
        City("aswan", "EG", 24.0889, 32.8998, "Africa/Cairo", "egypt"),
        City("amman", "JO", 31.9454, 35.9284, "Asia/Amman", "jordan"),
        City("jerusalem", "PS", 31.7683, 35.2137, "Asia/Jerusalem", "palestine"),
        City("gaza", "PS", 31.5017, 34.4668, "Asia/Gaza", "palestine"),
        City("beirut", "LB", 33.8938, 35.5018, "Asia/Beirut", "lebanon"),
        City("damascus", "SY", 33.5138, 36.2765, "Asia/Damascus", "syria"),
        City("aleppo", "SY", 36.2021, 37.1343, "Asia/Damascus", "syria"),
        City("baghdad", "IQ", 33.3152, 44.3661, "Asia/Baghdad", "mwl"),
        City("basra", "IQ", 30.5085, 47.7804, "Asia/Baghdad", "mwl"),
        City("erbil", "IQ", 36.1912, 44.0092, "Asia/Baghdad", "mwl"),
        City("sanaa", "YE", 15.3694, 44.191, "Asia/Aden", "mwl"),
        City("aden", "YE", 12.7855, 45.0187, "Asia/Aden", "mwl"),
        City("khartoum", "SD", 15.5007, 32.5599, "Africa/Khartoum", "egypt"),
        City("tripoli", "LY", 32.8872, 13.1913, "Africa/Tripoli", "libya"),
        City("benghazi", "LY", 32.1167, 20.0667, "Africa/Tripoli", "libya"),
        City("tunis", "TN", 36.8065, 10.1815, "Africa/Tunis", "tunisia"),
        City("algiers", "DZ", 36.7538, 3.0588, "Africa/Algiers", "algeria"),
        City("oran", "DZ", 35.6971, -0.6308, "Africa/Algiers", "algeria"),
        City("rabat", "MA", 34.0209, -6.8416, "Africa/Casablanca", "morocco", elev = 50),
        City("casablanca", "MA", 33.5731, -7.5898, "Africa/Casablanca", "morocco", elev = 50),
        City("marrakesh", "MA", 31.6295, -7.9811, "Africa/Casablanca", "morocco", elev = 466),
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
        City("tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent", "uzbekistan"),
        City("baku", "AZ", 40.4093, 49.8671, "Asia/Baku", "azerbaijan"),
        City("almaty", "KZ", 43.2383, 76.9455, "Asia/Almaty", "kazakhstan"),
        City("moscow", "RU", 55.7558, 37.6173, "Europe/Moscow", "russia"),
        City("kazan", "RU", 55.7963, 49.1088, "Europe/Moscow", "tatarstan"),
        City("grozny", "RU", 43.3178, 45.6949, "Europe/Moscow", "russia"),
        City("kualalumpur", "MY", 3.139, 101.6869, "Asia/Kuala_Lumpur", "malaysia"),
        City("penang", "MY", 5.4141, 100.3288, "Asia/Kuala_Lumpur", "malaysia"),
        City("singapore", "SG", 1.3521, 103.8198, "Asia/Singapore", "singapore"),
        City("brunei", "BN", 4.9031, 114.9398, "Asia/Brunei", "brunei"),
        City("jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta", "indonesia"),
        City("surabaya", "ID", -7.2575, 112.7521, "Asia/Jakarta", "indonesia"),
        City("bandung", "ID", -6.9175, 107.6191, "Asia/Jakarta", "indonesia", elev = 790),
        City("medan", "ID", 3.5952, 98.6722, "Asia/Jakarta", "indonesia"),
        City("makassar", "ID", -5.1477, 119.4327, "Asia/Makassar", "indonesia"),
        City("lagos", "NG", 6.5244, 3.3792, "Africa/Lagos", "mwl"),
        City("kano", "NG", 12.0022, 8.592, "Africa/Lagos", "mwl"),
        City("dakar", "SN", 14.7167, -17.4677, "Africa/Dakar", "mwl"),
        City("sarajevo", "BA", 43.8563, 18.4131, "Europe/Sarajevo", "bosnia"),
        City("london", "GB", 51.5074, -0.1278, "Europe/London", "uk"),
        City("birmingham", "GB", 52.4862, -1.8904, "Europe/London", "uk"),
        City("paris", "FR", 48.8566, 2.3522, "Europe/Paris", "paris"),
        City("marseille", "FR", 43.2965, 5.3698, "Europe/Paris", "paris"),
        City("lyon", "FR", 45.764, 4.8357, "Europe/Paris", "paris"),
        City("brussels", "BE", 50.8503, 4.3517, "Europe/Brussels", "europe"),
        City("amsterdam", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "europe"),
        City("berlin", "DE", 52.52, 13.405, "Europe/Berlin", "europe"),
        City("hamburg", "DE", 53.5511, 9.9937, "Europe/Berlin", "europe"),
        City("cologne", "DE", 50.9375, 6.9603, "Europe/Berlin", "europe"),
        City("munich", "DE", 48.1351, 11.582, "Europe/Berlin", "europe"),
        City("vienna", "AT", 48.2082, 16.3738, "Europe/Vienna", "europe"),
        City("stockholm", "SE", 59.3293, 18.0686, "Europe/Stockholm", "europe"),
        City("newyork", "US", 40.7128, -74.006, "America/New_York", "isna"),
        City("chicago", "US", 41.8781, -87.6298, "America/Chicago", "isna"),
        City("losangeles", "US", 34.0522, -118.2437, "America/Los_Angeles", "isna"),
        City("toronto", "CA", 43.6532, -79.3832, "America/Toronto", "isna"),
        City("montreal", "CA", 45.5017, -73.5673, "America/Toronto", "isna"),
        City("sydney", "AU", -33.8688, 151.2093, "Australia/Sydney", "mwl"),
    )

    fun city(key: String?): City? = CITIES.firstOrNull { it.key == key }

    /** ارتفاع المكان: ارتفاع المدينة المعروفة إن كان الموقع فيها أو قريباً منها (نحو 30 كم)، وإلا صفر */
    fun elevationNear(lat: Double, lng: Double): Int =
        nearestCity(lat, lng).let { (c, d2) -> if (d2 < 0.3 * 0.3) c.elev else 0 }

    /** أقرب مدينة معروفة لاقتراح طريقة الحساب */
    fun nearestCity(lat: Double, lng: Double): Pair<City, Double> =
        CITIES.map { it to ((it.lat - lat) * (it.lat - lat) + (it.lng - lng) * (it.lng - lng)) }.minBy { it.second }
}
