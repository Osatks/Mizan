package com.osatks.mizan

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.AdhkarLive
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.TasbihState
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.widget.LiveStates
import com.osatks.mizan.widget.WidgetData
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * ما يرسله Kotlin إلى Swift يُفكّ هناك بـ JSONDecoder في بنى ثابتة (iosApp/Shared/LiveStates.swift و
 * Shared/SnapshotModel.swift). مفتاح ناقص أو باسم آخر أو بنوع آخر يُفشل الفكّ كله بلا خطأ ظاهر: يختفي النشاط
 * الحي أو الأداة. هنا تُقرأ حقول البنى من ملفات Swift نفسها وتُقارن بمفاتيح الخرائط وأنواع قيمها، بكل لغات التطبيق.
 *
 * و MIZAN_GALLERY_EXPORT=<مجلد> يكتب هذه الحالات (live-fixtures.json) لمعرض الأدوات (iosApp/Gallery) فيرسمها
 * ويفكّها بـ Swift الحقيقي على المحاكي.
 */
class LiveStatesTest {
    init {
        TestPacks.load()
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
    }

    /** حقل في بنية Swift: اسمه ونوعه، واختياري أم لا (بعلامة ? أو بقيمة افتراضية) */
    data class Field(val name: String, val type: String, val optional: Boolean)

    object SwiftStructs {
        private val ROOT = File("../../iosApp")

        /** الحقول المخزّنة في [struct] (لا المحسوبة): «var name: Type» في المستوى الأول من جسم البنية */
        fun fields(file: String, struct: String): List<Field> {
            val lines = File(ROOT, file).readLines()
            val start = lines.indexOfFirst { Regex("^struct $struct\\b").containsMatchIn(it) }
            assertTrue("struct $struct not found in $file", start >= 0)
            val out = ArrayList<Field>()
            for (line in lines.drop(start + 1)) {
                if (line.startsWith("}")) break
                val m = Regex("^ {4}(?:let|var) (\\w+): ([^={]+?)\\s*(=.*)?$").find(line) ?: continue
                val (name, rawType, default) = m.destructured
                out += Field(name, rawType.trim().removeSuffix("?"), rawType.trim().endsWith("?") || default.isNotEmpty())
            }
            assertTrue("no fields read from $struct", out.isNotEmpty())
            return out
        }
    }

    /** يقارن [map] ببنية Swift: كل حقل مطلوب موجود بنوعه، ولا مفتاح زائد (اسم مختلف في أحد الطرفين) */
    private fun check(where: String, map: Map<String, Any?>, fields: List<Field>) {
        val names = fields.map { it.name }.toSet()
        val extra = map.keys - names
        if (extra.isNotEmpty()) fail("$where: keys not in the Swift struct: $extra")
        for (f in fields) {
            val v = map[f.name]
            if (v == null) {
                if (!f.optional) fail("$where: missing ${f.name} (${f.type})")
                continue
            }
            val ok = when (f.type) {
                "String" -> v is String
                "Int" -> v is Int || v is Long
                "Double", "Date" -> v is Number
                "Bool" -> v is Boolean
                else -> v is Map<*, *> || v is List<*>
            }
            if (!ok) fail("$where: ${f.name} is ${v::class.simpleName}, Swift expects ${f.type}")
            if (f.type == "String" && (v as String).isBlank() && f.name in NON_EMPTY) fail("$where: ${f.name} is empty")
        }
        // ما يكتبه Json.write يُقرأ كما هو
        val back = Json.obj(Json.write(map))
        assertTrue("$where: JSON round trip lost keys", back.keys == map.keys)
    }

    /** نصوص تُعرض وحدها في النشاط أو الأداة: فراغها خطأ ترجمة */
    private val NON_EMPTY = setOf(
        "name", "time", "label", "now", "hijri", "place", "title", "goalText", "roundsLabel", "todayLabel", "tap", "reset",
        "end", "next", "doneText", "pages", "streakText", "cta", "khatma", "countText", "position",
    )

    private val ZONE = "Asia/Amman"

    private fun epoch(t: LocalDateTime) = t.toInstant(TimeZone.of(ZONE)).epochSeconds

    /** حالات كل لغة: المسبحة (حرّة، وبعد الصلاة، وبلا حد)، والأذكار (وسط القسم، ومكتمل)، والصلاة (عادية، وإفطار رمضان بإقامة)، والختمة،
     *  وأطول النصوص لحدّ الارتفاع على شاشة القفل: تمام المائة، و«لا حول ولا قوة إلا بالله»، وأطول أذكار الصباح */
    private fun cases(lang: Lang): Map<String, Map<String, Any?>> {
        val accent = "emerald"
        val at = 1_793_000_000L
        val amman = PrayTimes.city("amman")?.let { com.osatks.mizan.core.Place.of(it) }
            ?: LocationHelper.placeFrom(31.9454, 35.9284).copy(tz = ZONE)
        val asr = LocalDateTime(2026, 11, 2, 14, 41)
        val ramadan = Hijri.toGregorian(1448, 9, 12)!!
        val maghrib = LocalDateTime(ramadan.year, ramadan.monthNumber, ramadan.dayOfMonth, 17, 28)
        val s = lang.strings
        // أطول ذكر له معنى بهذه اللغة (فيُرسم معه سطر المعنى)
        val morning = Adhkar.section("morning")?.items.orEmpty()
        val tr = Adhkar.tr(lang)
        val longest = morning.indices.filter { lang == Lang.AR || tr.items[morning[it].id]?.t != null }
            .maxByOrNull { morning[it].ar.length } ?: 0
        return linkedMapOf(
            "tasbih" to LiveStates.tasbih(TasbihState(idx = 1, count = 21, target = 33, rounds = 3, total = 520, today = 120), lang, accent, at),
            "tasbihSalah" to LiveStates.tasbih(TasbihState(mode = "salah", step = 1, count = 12, rounds = 1, today = 145), lang, accent, at),
            "tasbihFree" to LiveStates.tasbih(TasbihState(idx = 4, count = 1234, target = 0, today = 1234), lang, accent, at),
            "tasbihTamam" to LiveStates.tasbih(TasbihState(mode = "salah", step = 3, count = 0, rounds = 2, today = 1290), lang, accent, at),
            "tasbihLong" to LiveStates.tasbih(TasbihState(idx = 7, count = 87, target = 100, total = 87, today = 87), lang, accent, at),
            "adhkar" to (AdhkarLive.state(AdhkarLive.Position("morning", 3, 1, "2026-11-02"), lang) + LiveStates.palette(accent)),
            "adhkarDone" to (AdhkarLive.state(AdhkarLive.Position("evening", 0, 0, "2026-11-02", done = true), lang) + LiveStates.palette(accent)),
            "adhkarLong" to (AdhkarLive.state(AdhkarLive.Position("morning", longest, 0, "2026-11-02"), lang) + LiveStates.palette(accent)),
            "prayer" to LiveStates.prayer("asr", asr, epoch(asr), epoch(asr) - 3 * 3600, asr.date, lang, amman, 0, false, accent),
            "prayerIftar" to LiveStates.prayer("maghrib", maghrib, epoch(maghrib), epoch(maghrib) - 2 * 3600, maghrib.date, lang, amman, 10, false, accent),
            "khatma" to WidgetData.khatmaMap(s, 112, 604, 20, 8, 6, false, "surah/18?ayah=10&from=widget"),
            "khatmaDone" to WidgetData.khatmaMap(s, 604, 604, 0, 0, 30, true, "quran"),
        )
    }

    @Test fun keysMatchSwift() {
        val tasbih = SwiftStructs.fields("Shared/LiveStates.swift", "TasbihLiveState")
        val adhkar = SwiftStructs.fields("Shared/LiveStates.swift", "AdhkarLiveState")
        val prayer = SwiftStructs.fields("Shared/LiveStates.swift", "PrayerLiveState")
        val khatma = SwiftStructs.fields("Shared/SnapshotModel.swift", "Khatma")
        for (lang in Lang.entries) {
            for ((kind, map) in cases(lang)) {
                val fields = when {
                    kind.startsWith("tasbih") -> tasbih
                    kind.startsWith("adhkar") -> adhkar
                    kind.startsWith("prayer") -> prayer
                    else -> khatma
                }
                check("${lang.code}/$kind", map, fields)
            }
            val iftar = cases(lang).getValue("prayerIftar")
            assertTrue("${lang.code}: iftar dua", iftar["dua"] is String && iftar["iqama"] is Long && iftar["iqamaTime"] is String)
        }
    }

    /** لقطة الأدوات كاملة (بالختمة) بمفاتيح Snapshot في Swift */
    @Test fun snapshotMatchesSwift(): Unit = runBlocking {
        val fields = SwiftStructs.fields("Shared/SnapshotModel.swift", "Snapshot")
        val snap = snapshot(Lang.AR)
        check("snapshot", snap, fields)
        // ما تقرؤه الساعة: بكل لغة، والقبلة من عمّان نحو الجنوب الشرقي تقريباً
        val extras = SwiftStructs.fields("Shared/SnapshotModel.swift", "WatchExtras")
        for (lang in Lang.entries) {
            @Suppress("UNCHECKED_CAST")
            val w = snapshot(lang)["watch"] as Map<String, Any?>
            check("${lang.code}/watch", w, extras)
            val deg = w["qibla"] as Double
            assertTrue("${lang.code}: qibla $deg", deg in 150.0..165.0)
            assertTrue("${lang.code}: iqama ${w["iqama"]}", (w["iqama"] as Map<*, *>).keys.containsAll(listOf("fajr", "isha")))
            assertTrue("${lang.code}: months", (w["months"] as Map<*, *>).size == 2)
        }
        System.getenv("MIZAN_GALLERY_EXPORT")?.let { export(File(it)) }
    }

    private suspend fun snapshot(lang: Lang): Map<String, Any?> {
        val p = LocationHelper.placeFrom(31.9454, 35.9284).copy(tz = ZONE)
        val now = LocalDateTime(2026, 11, 2, 13, 5)
        return WidgetData.snapshot(2, now, lang, p, p.method, false, false, "emerald", "system") +
            ("khatma" to WidgetData.khatmaMap(lang.strings, 112, 604, 20, 8, 6, false, "surah/18?ayah=10&from=widget"))
    }

    /** حالات كل لغة ولقطتها لمعرض الأدوات: { "ar": { "tasbih": {...}, ..., "snapshot": {...} }, ... } */
    private suspend fun export(dir: File) {
        dir.mkdirs()
        val all = linkedMapOf<String, Any?>()
        for (lang in Lang.entries) all[lang.code] = cases(lang) + ("snapshot" to snapshot(lang))
        File(dir, "live-fixtures.json").writeText(Json.write(all))
    }
}
