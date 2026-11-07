package com.osatks.mizan

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.Dhikr
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.widget.AdhkarClock
import com.osatks.mizan.widget.AdhkarWindow
import com.osatks.mizan.widget.WidgetColors
import com.osatks.mizan.widget.WidgetData
import com.osatks.mizan.widget.WidgetText
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.floor

/**
 * بيانات أدوات iOS المستقلة (iosApp/MizanWidgets/Static): حين لا تصل لقطة التطبيق إلى الأداة (تثبيت بحساب Apple مجاني
 * بلا App Group ولا سلسلة مفاتيح مشتركة) تحسب الأداة مواقيتها وأذكارها وحدها في Swift من هذه الملفات.
 * تُولَّد من مصادر Kotlin نفسها حتى تبقى الأداة مطابقة للتطبيق، ويفشل الاختبار إن تغيّرت المصادر ولم تُولَّد من جديد:
 *   MIZAN_WIDGET_EXPORT=1 sh gradlew :shared:testDebugUnitTest --tests '*WidgetStaticTest*'
 * و MIZAN_WIDGET_FIXTURES=<مجلد> يكتب نتائج Kotlin (مواقيت ونوافذ أذكار ولقطات كاملة) ليقارن بها اختبار Swift على لينكس.
 */
object WidgetStatic {
    val DIR = File("../../iosApp/MizanWidgets/Static")
    const val COMMON = "widget-static.json"
    fun langFile(code: String) = "widget-$code.json"

    /** آية اليوم: آيات هذه الأيام وحدها (بمعانيها بكل لغة)، وبعدها تدور الأداة عليها حتى يُولَّد الملف من جديد */
    val AYAH_START = LocalDate(2026, 10, 1)
    const val AYAH_DAYS = 550

    val KINDS = listOf("adhan", "iftar", "afterPrayer", "morning", "dhikr", "friday", "arafah", "evening", "night", "lastThird")

    fun lists(): Map<String, List<String>> = with(AdhkarClock) {
        linkedMapOf(
            "adhan" to ADHAN, "iftar" to IFTAR, "after" to AFTER, "morning" to MORNING, "evening" to EVENING,
            "dhikr" to DHIKR, "friday" to FRIDAY, "arafah" to ARAFAH, "night" to NIGHT, "lastThird" to LAST_THIRD,
        )
    }

    fun tokens(): List<String> =
        (lists().values.flatten() + listOf("ram07", WidgetText.KAHF, WidgetText.SALAWAT, WidgetText.HOUR)).distinct()

    fun items(): List<Dhikr> = WidgetData.expand(tokens())

    /** آيات الأيام بالترتيب: مواضعها في المصحف بلا تكرار، ولكل يوم رقم آيته في هذه القائمة */
    fun ayahs(q: Array<String>): Pair<List<Int>, List<Int>> {
        val pool = ArrayList<Int>()
        val pos = HashMap<Int, Int>()
        val days = (0 until AYAH_DAYS).map { i ->
            val idx = ayahOfDay(q, AYAH_START.plusDays(i))
            pos.getOrPut(idx) { pool.add(idx); pool.size - 1 }
        }
        return pool to days
    }

    /** كيف يُكتب رقم اليوم في التاريخ (الفرنسية: 1er)؛ من قالب الشهر الأول */
    private fun dayTokens(f: (Int, Int, Int) -> String): List<String> {
        val t = f(27, 1, 1987).replace("1987", "{y}")
        val (pre, post) = t.substringBefore("27") to t.substringAfter("27").replace("{y}", "1987")
        return (1..31).map { d -> f(d, 1, 1987).removePrefix(pre).removeSuffix(post) }
    }

    /** قالب تاريخ لشهر: {d} و {y} مكان اليوم والسنة، ويُتحقق أنه يعطي نص التطبيق نفسه */
    private fun dateTpl(f: (Int, Int, Int) -> String, m: Int, years: List<Int>, days: List<String>): String {
        val t = f(27, m, 1987).replace("1987", "{y}").replace("27", "{d}")
        for (d in 1..31) for (y in years) check(t.replace("{d}", days[d - 1]).replace("{y}", "$y") == f(d, m, y)) { "date template $t ($d/$m/$y)" }
        return t
    }

    private fun monthTpl(f: (Int, Int) -> String, m: Int): String {
        val t = f(m, 1987).replace("1987", "{y}")
        for (y in 1440..1470) check(t.replace("{y}", "$y") == f(m, y)) { "month template $t" }
        return t
    }

    suspend fun common(): Map<String, Any?> {
        val q = QuranData.uthmani()
        val (pool, days) = ayahs(q)
        val c = WidgetColors.of("emerald")
        return linkedMapOf(
            "v" to 1,
            "methods" to PrayTimes.METHODS.map { m ->
                linkedMapOf(
                    "key" to m.key, "fajr" to m.fajr, "isha" to m.ishaAngle, "ishaMin" to m.ishaMinutes, "maghrib" to m.maghribAngle,
                    "min" to m.minutes, "hanafi" to m.hanafi, "elev" to m.elevation, "daily" to m.dailySun,
                    "ff" to m.fajrFallback, "fi" to m.ishaFallback, "seasonal" to m.seasonal, "dep" to m.depression?.toList(),
                    "ram" to m.ramadan,
                )
            },
            "cities" to PrayTimes.CITIES.map {
                linkedMapOf<String, Any?>("k" to it.key, "lat" to it.lat, "lng" to it.lng, "m" to it.method).apply { if (it.elev > 0) put("e", it.elev) }
            },
            "hijri" to linkedMapOf(
                "first" to 1300,
                "epoch" to Hijri.toGregorian(1300, 1, 1)!!.toEpochDay(),
                "lengths" to (1300..1600).flatMap { y -> (1..12).map { m -> if (Hijri.monthLength(y, m) == 30) '1' else '0' } }.joinToString(""),
            ),
            "adhkar" to linkedMapOf(
                "adhanMin" to AdhkarClock.ADHAN_MIN, "afterMin" to AdhkarClock.AFTER_MIN, "rotateSec" to AdhkarClock.ROTATE_SEC,
                "lists" to lists(),
                "sections" to KINDS.associateWith { AdhkarClock.sectionOf(it) },
                "tokens" to tokens().associateWith { t -> WidgetData.expand(listOf(t)).map { it.id } },
            ),
            "items" to items().associate { it.id to linkedMapOf("ar" to it.ar, "n" to it.count) },
            "colors" to linkedMapOf(
                "hero1" to WidgetData.hex(c.hero1), "hero2" to WidgetData.hex(c.hero2), "night1" to WidgetData.hex(c.night1),
                "night2" to WidgetData.hex(c.night2), "accent" to WidgetData.hex(c.accentLight), "gold" to WidgetData.hex(c.gold),
            ),
            "ayah" to linkedMapOf(
                "start" to AYAH_START.toEpochDay(),
                "days" to days,
                "pool" to pool.map { idx ->
                    val (sn, an) = QuranMeta.locate(idx)
                    linkedMapOf("text" to QuranData.withoutBismillah(q, idx), "s" to sn, "a" to an, "route" to surahRoute(sn, an, "tafsir"))
                },
            ),
        )
    }

    suspend fun language(lang: Lang): Map<String, Any?> {
        val s = lang.strings
        val wt = WidgetText.of(lang.code)
        val tr = Adhkar.tr(lang)
        val (pool, _) = ayahs(QuranData.uthmani())
        val meaning = Sources.translationFor(lang)?.let { QuranData.source(it.id) }
        val probe = LocalDateTime(2026, 1, 1, 0, 0)
        val days = dayTokens(s::gregDate)
        val surah = "§S§"
        val ref = s.surahAyah(surah, 271).replace(surah, "{surah}").replace("271", "{ayah}")
        for (idx in pool) {
            val (sn, an) = QuranMeta.locate(idx)
            check(ref.replace("{surah}", s.surahName(sn)).replace("{ayah}", "$an") == s.surahAyah(s.surahName(sn), an)) { "ref template $ref" }
        }
        return linkedMapOf(
            "code" to lang.code, "rtl" to lang.rtl,
            "am" to s.am, "pm" to s.pm,
            "weekdays" to s.weekdays,
            "days" to days.takeIf { it != (1..31).map { "$it" } },
            "greg" to (1..12).map { m -> dateTpl(s::gregDate, m, listOf(2025, 2026, 2027, 2030), days) },
            "hijri" to (1..12).map { m -> dateTpl(s::hijriDate, m, listOf(1447, 1448, 1450), days) },
            "hijriMonth" to (1..12).map { m -> monthTpl(s::hijriMonthTitle, m) },
            "prayers" to PrayTimes.PRAYERS.associateWith { s.prayer(it) },
            "myLocation" to s.myLocation,
            "cities" to PrayTimes.CITIES.associate { it.key to s.city(it.key) },
            "labels" to WidgetData.labels(s, wt, com.osatks.mizan.widget.LiveText.of(lang.code)),
            "titles" to KINDS.associateWith { WidgetData.title(AdhkarWindow(it, probe, probe, "", emptyList()), lang, wt, tr) },
            "chips" to linkedMapOf("friday" to s.weekdays[4], "fridayEve" to wt.fridayEve, "lastTen" to wt.lastTen),
            "until" to wt.until,
            "items" to items().associate { d ->
                val x = WidgetData.dhikr(d, lang, s, wt, tr)
                d.id to linkedMapOf("c" to x.countText, "src" to x.source, "tr" to x.tr, "fadl" to x.fadl)
            },
            "surahAyah" to ref,
            "surahs" to (1..114).map { s.surahName(it) },
            "meanings" to if (meaning == null) emptyList() else pool.map { meaning.getOrNull(it) },
        )
    }

    /** اسم الملف ← محتواه */
    suspend fun files(): Map<String, String> {
        val out = linkedMapOf(COMMON to Compact.write(common()))
        for (lang in Lang.entries) out[langFile(lang.code)] = Compact.write(language(lang))
        return out
    }
}

/** JSON مضغوط ثابت الترتيب (سطر لكل عنصر في المستويين الأولين حتى تبقى الفروق مقروءة)، والحقول الفارغة تُحذف */
object Compact {
    fun write(v: Any?): String = buildString { put(this, v, 0); append('\n') }

    private fun num(d: Double): String = if (d == floor(d) && abs(d) < 1e15) d.toLong().toString() else d.toString()

    private fun put(b: StringBuilder, v: Any?, depth: Int) {
        val sep = if (depth < 2) ",\n" else ","
        when (v) {
            null -> b.append("null")
            is String -> quote(b, v)
            is Char -> quote(b, v.toString())
            is Boolean, is Int, is Long -> b.append(v.toString())
            is Double -> b.append(if (v.isNaN() || v.isInfinite()) "null" else num(v))
            is Map<*, *> -> {
                b.append('{')
                var first = true
                for ((k, x) in v) {
                    if (x == null) continue
                    if (!first) b.append(sep)
                    first = false
                    quote(b, k.toString()); b.append(':'); put(b, x, depth + 1)
                }
                b.append('}')
            }
            is List<*> -> {
                b.append('[')
                v.forEachIndexed { i, x -> if (i > 0) b.append(sep); put(b, x, depth + 1) }
                b.append(']')
            }
            else -> quote(b, v.toString())
        }
    }

    private fun quote(b: StringBuilder, s: String) {
        b.append('"')
        for (c in s) when (c) {
            '"' -> b.append("\\\""); '\\' -> b.append("\\\\"); '\n' -> b.append("\\n"); '\t' -> b.append("\\t"); '\r' -> b.append("\\r")
            else -> if (c < ' ') b.append("\\u").append(c.code.toString(16).padStart(4, '0')) else b.append(c)
        }
        b.append('"')
    }
}

class WidgetStaticTest {
    init {
        TestPacks.load()
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
    }

    /** الملفات المحفوظة في iosApp مطابقة لما تولّده المصادر الآن */
    @Test
    fun staticFilesAreCurrent() = runBlocking {
        val files = WidgetStatic.files()
        if (System.getenv("MIZAN_WIDGET_EXPORT") != null) {
            WidgetStatic.DIR.mkdirs()
            for ((name, text) in files) File(WidgetStatic.DIR, name).writeText(text)
        }
        System.getenv("MIZAN_WIDGET_FIXTURES")?.let { WidgetFixtures.write(File(it)) }
        val regen = "MIZAN_WIDGET_EXPORT=1 sh gradlew :shared:testDebugUnitTest --tests '*WidgetStaticTest*'"
        for ((name, text) in files) {
            val f = File(WidgetStatic.DIR, name)
            assertTrue("$name is missing: run $regen", f.exists())
            assertEquals("$name is out of date: run $regen", text, f.readText())
        }
        // حجم ما تحمله الأداة: الملف المشترك وملف لغة واحدة
        val total = files.values.sumOf { it.toByteArray().size }
        assertTrue("widget data too large: $total", total < 3_500_000)
    }
}

/** نتائج Kotlin يقارن بها اختبار Swift (مواقيت خام، ونوافذ الأذكار، ولقطات الأداة كاملة) */
object WidgetFixtures {
    class Loc(val name: String, val lat: Double, val lng: Double, val tz: String)

    val LOCS = listOf(
        Loc("makkah", 21.4225, 39.8262, "Asia/Riyadh"), Loc("riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        Loc("dubai", 25.2048, 55.2708, "Asia/Dubai"), Loc("doha", 25.2854, 51.531, "Asia/Qatar"),
        Loc("cairo", 30.0444, 31.2357, "Africa/Cairo"), Loc("casablanca", 33.5731, -7.5898, "Africa/Casablanca"),
        Loc("istanbul", 41.0082, 28.9784, "Europe/Istanbul"), Loc("tehran", 35.6892, 51.389, "Asia/Tehran"),
        Loc("karachi", 24.8607, 67.0011, "Asia/Karachi"), Loc("dhaka", 23.8103, 90.4125, "Asia/Dhaka"),
        Loc("jakarta", -6.2088, 106.8456, "Asia/Jakarta"), Loc("kualalumpur", 3.139, 101.6869, "Asia/Kuala_Lumpur"),
        Loc("moscow", 55.7558, 37.6173, "Europe/Moscow"), Loc("london", 51.5074, -0.1278, "Europe/London"),
        Loc("paris", 48.8566, 2.3522, "Europe/Paris"), Loc("oslo", 59.9139, 10.7522, "Europe/Oslo"),
        Loc("helsinki", 60.1699, 24.9384, "Europe/Helsinki"), Loc("reykjavik", 64.1466, -21.9426, "Atlantic/Reykjavik"),
        Loc("tromso", 69.6492, 18.9553, "Europe/Oslo"), Loc("anchorage", 61.2181, -149.9003, "America/Anchorage"),
        Loc("newyork", 40.7128, -74.006, "America/New_York"), Loc("losangeles", 34.0522, -118.2437, "America/Los_Angeles"),
        Loc("toronto", 43.6532, -79.3832, "America/Toronto"), Loc("tokyo", 35.6762, 139.6503, "Asia/Tokyo"),
        Loc("sydney", -33.8688, 151.2093, "Australia/Sydney"), Loc("auckland", -36.8485, 174.7633, "Pacific/Auckland"),
        Loc("capetown", -33.9249, 18.4241, "Africa/Johannesburg"), Loc("buenosaires", -34.6037, -58.3816, "America/Argentina/Buenos_Aires"),
        Loc("tashkent", 41.2995, 69.2401, "Asia/Tashkent"), Loc("almaty", 43.222, 76.8512, "Asia/Almaty"),
        Loc("amman", 31.9454, 35.9284, "Asia/Amman"), Loc("bandung", -6.9175, 107.6191, "Asia/Jakarta"),
    )

    private fun times(v: com.osatks.mizan.core.PrayerTimes) =
        listOf(v.fajr, v.sunrise, v.dhuhr, v.asr, v.maghrib, v.isha, v.sunset, v.maghribEnd, v.midnight)

    /** أول يوم ميلادي لتاريخ هجري */
    private fun greg(y: Int, m: Int, d: Int) = Hijri.toGregorian(y, m, d)!!

    fun write(dir: File) = runBlocking {
        dir.mkdirs()
        // مواقيت خام: كل مدينة بطريقتها كل ثلاثة أيام لثلاث سنوات، وكل الطرق والحنفي في بعضها
        val prayer = ArrayList<Any?>()
        val from = LocalDate(2026, 1, 1)
        for (l in LOCS) {
            val method = PrayTimes.nearestCity(l.lat, l.lng).first.method
            val elev = PrayTimes.elevationNear(l.lat, l.lng)
            for (i in 0 until 365 * 3 step 3) {
                val d = from.plusDays(i)
                prayer += linkedMapOf("lat" to l.lat, "lng" to l.lng, "tz" to l.tz, "m" to method, "h" to false, "e" to elev, "d" to d.toString(),
                    "t" to times(PrayTimes.compute(d, l.lat, l.lng, l.tz, method, false, elev)))
            }
        }
        for (l in LOCS.filter { it.name in setOf("makkah", "london", "oslo", "reykjavik", "tromso", "sydney", "jakarta", "newyork") }) {
            for (m in PrayTimes.METHODS) for (hanafi in listOf(false, true)) for (i in 0 until 365 step 15) {
                val d = from.plusDays(i)
                prayer += linkedMapOf("lat" to l.lat, "lng" to l.lng, "tz" to l.tz, "m" to m.key, "h" to hanafi, "d" to d.toString(),
                    "t" to times(PrayTimes.compute(d, l.lat, l.lng, l.tz, m.key, hanafi)))
            }
        }
        File(dir, "prayer.json").writeText(Compact.write(prayer))

        // نوافذ الأذكار: رمضان والعشر الأواخر وعرفة والجمعة في كل مدينة
        val ramadan = greg(1448, 9, 1)
        val special = (0 until 30).map { ramadan.plusDays(it) } + greg(1448, 12, 9) + greg(1448, 12, 8) +
            (0 until 365 step 4).map { LocalDate(2027, 1, 1).plusDays(it) }
        val windows = ArrayList<Any?>()
        for (l in LOCS) {
            val p = LocationHelper.placeFrom(l.lat, l.lng)
            val t = { d: LocalDate -> PrayTimes.compute(d, p.lat, p.lng, l.tz, p.method, false) }
            for (d in special.distinct()) {
                windows += linkedMapOf("lat" to l.lat, "lng" to l.lng, "tz" to l.tz, "d" to d.toString(),
                    "w" to AdhkarClock.day(d, t, Hijri::toHijri).map { w ->
                        linkedMapOf("k" to w.kind, "s" to w.start.toString(), "e" to w.end.toString(), "sec" to w.section,
                            "items" to w.items, "p" to w.prayer, "tag" to w.tag)
                    })
            }
        }
        File(dir, "windows.json").writeText(Compact.write(windows))

        // لقطات كاملة: كل مدينة بلغة، ومكة بكل اللغات في رمضان وعرفة والجمعة
        val langs = Lang.entries
        val nows = listOf("2026-10-27T13:37", "2027-02-25T03:10", "2027-03-04T21:30", "2027-05-14T09:45", "2027-06-21T23:50", "2026-12-25T16:20")
        val cases = ArrayList<Triple<Loc, String, Lang>>()
        LOCS.forEachIndexed { i, l -> cases += Triple(l, nows[i % nows.size], langs[i % langs.size]) }
        val makkah = LOCS[0]
        langs.forEachIndexed { i, lang -> cases += Triple(makkah, nows[(i + 1) % nows.size], lang) }
        LOCS.filter { it.name in setOf("oslo", "tromso", "reykjavik", "sydney", "london") }.forEach { l ->
            nows.forEach { n -> cases += Triple(l, n, Lang.EN) }
        }
        val snaps = cases.map { (l, now, lang) ->
            val p = LocationHelper.placeFrom(l.lat, l.lng).copy(tz = l.tz)
            linkedMapOf("lat" to l.lat, "lng" to l.lng, "tz" to l.tz, "now" to now, "lang" to lang.code,
                // الأداة المستقلة لا تعرف اختيار المستخدم للعصر: المعتمد في الطريقة
                "snap" to WidgetData.snapshot(7, LocalDateTime.parse(now), lang, p, p.method, PrayTimes.method(p.method).hanafi, false, "emerald", "system"))
        }
        File(dir, "snapshots.json").writeText(Compact.write(snaps))
    }
}
