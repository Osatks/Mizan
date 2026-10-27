package com.osatks.mizan.widget

import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.atStartOfDay
import com.osatks.mizan.core.durationBetween
import com.osatks.mizan.core.minusDays
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.plusSeconds
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * نافذة من أوقات الأذكار في أداة «أذكار الوقت».
 * kind: adhan | iftar | afterPrayer | morning | dhikr | friday | arafah | evening | night | lastThird
 * items: معرّفات الأذكار بالترتيب، و"@قسم" تعني القسم كله، و"@قسم/ن" مجموعته رقم ن.
 * section: قسم الأذكار الذي يُفتح في التطبيق عند لمس الأداة.
 * tag: friday (نهار الجمعة) | fridayEve (ليلة الجمعة) | ramadan | arafah | lastTen (العشر الأواخر)
 */
data class AdhkarWindow(
    val kind: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val section: String,
    val items: List<String>,
    val prayer: String? = null,
    val tag: String? = null,
) {
    /** مفتاح ثابت للنافذة: يُصفَّر عنده موضع التدوير الذي اختاره المستخدم */
    val key: String get() = "$kind@$start"
}

/**
 * قواعد اختيار الأذكار حسب الوقت (من مواقيت الصلاة المحسوبة للموقع):
 * - عند دخول كل صلاة (١٠ دقائق): أذكار الأذان، وفي مغرب رمضان دعاء الإفطار معها.
 * - بعدها نصف ساعة: الأذكار بعد السلام من الصلاة.
 * - من بعد الفجر إلى منتصف الضحى (بين الشروق والظهر): أذكار الصباح.
 * - الضحى، وبين الظهر والعصر: تسبيح واستغفار وصلاة على النبي ﷺ؛ وفي الجمعة سورة الكهف والصلاة على النبي ﷺ،
 *   ويوم عرفة دعاؤه.
 * - من بعد العصر إلى العشاء: أذكار المساء، وآخر ساعة من الجمعة تُذكّر بساعة الإجابة.
 * - بعد العشاء: أذكار النوم، وفي الثلث الأخير من الليل الاستغفار وأذكار الاستيقاظ.
 * - ليلة الجمعة تبدأ بالصلاة على النبي ﷺ، وليالي العشر الأواخر بدعاء ليلة القدر.
 */
object AdhkarClock {
    const val ADHAN_MIN = 10
    const val AFTER_MIN = 30
    /** كل كم دقيقة ينتقل الذكر المعروض وحده */
    const val ROTATE_MIN = 20

    val ADHAN = listOf("@adhan")
    val IFTAR = listOf("ram02", "ram03", "@adhan")
    val AFTER = listOf("@afterPrayer/0")
    val MORNING = listOf("@morning")
    val EVENING = listOf("@evening")
    val DHIKR = listOf("tsb01", "tsb03", "tsb04", "tsb06", "tsb07", "tsb02", "stg01", "stg03", "tsb14", "tsb09")
    val FRIDAY = listOf(WidgetText.KAHF, WidgetText.SALAWAT, "tsb14", "tsb16", "tsb15", "tsb17", "tsb20", "tsb19")
    val ARAFAH = listOf("haj06", "tsb04", "tsb01", "stg01")
    val NIGHT = listOf("@sleep/0", "slp16")
    val LAST_THIRD = listOf("stg04", "stg01", "stg03", "wak01", "wak02", "wak03")

    private val SECTION = mapOf(
        "adhan" to "adhan", "iftar" to "ramadan", "afterPrayer" to "afterPrayer", "morning" to "morning",
        "dhikr" to "tasbih", "friday" to "tasbih", "arafah" to "hajj", "evening" to "evening",
        "night" to "sleep", "lastThird" to "istighfar",
    )

    fun sectionOf(kind: String) = SECTION[kind] ?: "morning"

    /** مواقيت سليمة: ما تعذّر حسابه (العروض العالية) يُقدَّر، وتبقى مرتبة */
    internal fun sane(t: PrayerTimes): DoubleArray {
        fun ok(v: Double) = !v.isNaN() && !v.isInfinite()
        val dhuhr = if (ok(t.dhuhr)) t.dhuhr else 12.0
        val sunrise = if (ok(t.sunrise)) t.sunrise else dhuhr - 6
        val fajr = if (ok(t.fajr) && t.fajr < sunrise) t.fajr else sunrise - 1.5
        val asr = if (ok(t.asr) && t.asr > dhuhr) t.asr else dhuhr + 3.5
        val maghrib = if (ok(t.maghrib) && t.maghrib > asr) t.maghrib else asr + 2.5
        val isha = if (ok(t.isha) && t.isha > maghrib) t.isha else maghrib + 1.5
        return doubleArrayOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
    }

    private fun at(day: LocalDate, hours: Double): LocalDateTime = day.atStartOfDay().plusSeconds((hours * 3600).toLong())
    private fun LocalDateTime.plusMin(m: Int) = plusSeconds(m * 60L)

    /**
     * نوافذ «يوم الأذكار» [day]: من فجره إلى فجر اليوم التالي، متصلة بلا فجوات.
     * times و hijri دالتان (مواقيت الموقع والتاريخ الهجري) حتى تُختبر القواعد بلا إعدادات.
     */
    fun day(day: LocalDate, times: (LocalDate) -> PrayerTimes, hijri: (LocalDate) -> HDate?): List<AdhkarWindow> {
        val t = sane(times(day))
        val nextFajr = at(day.plusDays(1), sane(times(day.plusDays(1)))[0])
        val fajr = at(day, t[0]); val sunrise = at(day, t[1]); val dhuhr = at(day, t[2])
        val asr = at(day, t[3]); val maghrib = at(day, t[4]); val isha = at(day, t[5])
        val duha = sunrise.plusSeconds(durationBetween(sunrise, dhuhr).inWholeSeconds / 2)
        val lastThird = nextFajr.plusSeconds(-durationBetween(maghrib, nextFajr).inWholeSeconds / 3)

        val h = hijri(day)
        val friday = day.dayOfWeek == DayOfWeek.FRIDAY
        val fridayEve = day.dayOfWeek == DayOfWeek.THURSDAY
        val ramadan = h?.m == 9
        val lastTen = ramadan && (h?.d ?: 0) >= 20  // ليلة ٢١ فما بعدها تبدأ مساء اليوم العشرين
        val arafah = h != null && h.m == 12 && h.d == 9

        val dayTag = when { arafah -> "arafah"; friday -> "friday"; ramadan -> "ramadan"; else -> null }
        val nightTag = when { lastTen -> "lastTen"; fridayEve -> "fridayEve"; ramadan -> "ramadan"; else -> null }
        val dhikrKind = when { arafah -> "arafah"; friday -> "friday"; else -> "dhikr" }
        val dhikrItems = when { arafah -> ARAFAH; friday -> FRIDAY; else -> DHIKR }
        val nightExtra = buildList {
            if (lastTen) add("ram07")
            if (fridayEve) { add(WidgetText.SALAWAT); add(WidgetText.KAHF) }
        }

        // بدايات النوافذ بالترتيب؛ كل نافذة تنتهي عند بداية التي بعدها
        class Mark(val kind: String, val at: LocalDateTime, val items: List<String>, val prayer: String?, val tag: String?)
        val marks = ArrayList<Mark>()
        fun prayer(key: String, p: LocalDateTime, tag: String?) {
            val iftar = key == "maghrib" && ramadan
            marks += Mark(if (iftar) "iftar" else "adhan", p, if (iftar) IFTAR else ADHAN, key, tag)
            marks += Mark("afterPrayer", p.plusMin(ADHAN_MIN), AFTER, key, tag)
        }
        prayer("fajr", fajr, dayTag)
        marks += Mark("morning", fajr.plusMin(ADHAN_MIN + AFTER_MIN), MORNING, null, dayTag)
        marks += Mark(dhikrKind, duha, dhikrItems, null, dayTag)
        prayer("dhuhr", dhuhr, dayTag)
        marks += Mark(dhikrKind, dhuhr.plusMin(ADHAN_MIN + AFTER_MIN), dhikrItems, null, dayTag)
        prayer("asr", asr, dayTag)
        // آخر ساعة بعد عصر الجمعة: ساعة الإجابة أولاً ثم أذكار المساء
        marks += Mark("evening", asr.plusMin(ADHAN_MIN + AFTER_MIN), if (friday) listOf(WidgetText.HOUR) + EVENING else EVENING, null, dayTag)
        prayer("maghrib", maghrib, nightTag)
        marks += Mark("evening", maghrib.plusMin(ADHAN_MIN + AFTER_MIN), EVENING, null, nightTag)
        prayer("isha", isha, nightTag)
        marks += Mark("night", isha.plusMin(ADHAN_MIN + AFTER_MIN), nightExtra + NIGHT, null, nightTag)
        marks += Mark("lastThird", lastThird, nightExtra.filter { it == "ram07" } + LAST_THIRD, null, nightTag)

        // لا ترجع نافذة قبل التي سبقتها (ليالي الصيف القصيرة في العروض العالية)، وما صار طوله صفراً يُحذف
        var prev = fajr
        val starts = marks.map { m -> m.at.coerceIn(prev, maxOf(prev, nextFajr)).also { prev = it } }
        return marks.indices.mapNotNull { i ->
            val start = starts[i]
            val end = if (i + 1 < marks.size) starts[i + 1] else maxOf(nextFajr, start)
            if (end <= start) null
            else marks[i].let { AdhkarWindow(it.kind, start, end, sectionOf(it.kind), it.items, it.prayer, it.tag) }
        }
    }

    /** نوافذ [days] أيام متتالية بدءاً من فجر [from] */
    fun range(from: LocalDate, days: Int, times: (LocalDate) -> PrayerTimes, hijri: (LocalDate) -> HDate?): List<AdhkarWindow> =
        (0 until days).flatMap { day(from.plusDays(it), times, hijri) }

    /** النافذة التي فيها [now] (قبل الفجر تعود إلى ليلة اليوم السابق) */
    fun at(now: LocalDateTime, times: (LocalDate) -> PrayerTimes, hijri: (LocalDate) -> HDate?): AdhkarWindow {
        for (d in listOf(now.date.minusDays(1), now.date)) {
            day(d, times, hijri).firstOrNull { now >= it.start && now < it.end }?.let { return it }
        }
        // لا يحدث عادة: قبل أول نافذة أو بعد آخرها
        return day(now.date, times, hijri).first()
    }

    /** رقم الذكر المعروض: ينتقل كل [ROTATE_MIN] دقيقة، ويزيد عليه ما تقدّم به المستخدم بزر «التالي» */
    fun index(now: LocalDateTime, w: AdhkarWindow, offset: Int, count: Int): Int {
        if (count <= 0) return 0
        val steps = (durationBetween(w.start, now).inWholeMinutes / ROTATE_MIN).coerceAtLeast(0)
        return ((steps + offset) % count).toInt().let { if (it < 0) it + count else it }
    }
}
