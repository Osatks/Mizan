package com.osatks.mizan.fixtures

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.osatks.mizan.TestPacks
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.AdhkarLive
import com.osatks.mizan.adhkar.AdhkarSection
import com.osatks.mizan.adhkar.Dhikr
import com.osatks.mizan.core.AdhanCatalog
import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.core.TasbihState
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.core.now
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.plusSeconds
import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.platform.AndroidContext
import com.osatks.mizan.platform.LiveTasbihText
import com.osatks.mizan.ui.screens.desc
import com.osatks.mizan.ui.screens.doneKey
import com.osatks.mizan.ui.screens.gradeLine
import com.osatks.mizan.ui.screens.sourceLine
import com.osatks.mizan.ui.screens.suggested
import com.osatks.mizan.ui.screens.title
import com.osatks.mizan.ui.screens.translation
import com.osatks.mizan.widget.AdhkarClock
import com.osatks.mizan.widget.AdhkarWindow
import com.osatks.mizan.widget.LiveText
import com.osatks.mizan.widget.WidgetData
import com.osatks.mizan.widget.WidgetText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * يصدّر نتائج منطق الأذكار والأحاديث والمسبحة والإعدادات وأذكار الوقت من Kotlin إلى iosApp/MizanCore/Fixtures/content،
 * فيقارنها CheckContent بنتائج Swift (MizanContent) حقلاً حقلاً.
 * الإعدادات تُشغَّل بـ AppSettings الحقيقية فوق مخزن في الذاكرة (SharedPreferences)، ويُكتب ما خُزّن بأنواعه كما يكتبه
 * Prefs في iOS (setObject للنص، setInteger للعدد، setBool للمنطقي) تحت البادئة «mizan.».
 */
class ContentFixturesExport {
    private val out = File("../../iosApp/MizanCore/Fixtures/content")

    init {
        TestPacks.load()
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
        Hadiths.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
    }

    // ---------- JSON مضغوط ----------

    private fun j(v: Any?): String = StringBuilder().also { put(it, v) }.toString()

    private fun put(b: StringBuilder, v: Any?) {
        when (v) {
            null -> b.append("null")
            is String -> quote(b, v)
            is Boolean -> b.append(v)
            is Int, is Long -> b.append(v.toString())
            is Double -> if (v.isNaN() || v.isInfinite()) b.append("null") else quote(b, "#" + v.toString())
            is Map<*, *> -> {
                b.append('{')
                var first = true
                for ((k, x) in v) { if (!first) b.append(','); first = false; quote(b, k.toString()); b.append(':'); put(b, x) }
                b.append('}')
            }
            is List<*> -> {
                b.append('[')
                v.forEachIndexed { i, x -> if (i > 0) b.append(','); put(b, x) }
                b.append(']')
            }
            is Pair<*, *> -> put(b, listOf(v.first, v.second))
            is Triple<*, *, *> -> put(b, listOf(v.first, v.second, v.third))
            else -> quote(b, v.toString())
        }
    }

    private fun quote(b: StringBuilder, s: String) {
        b.append('"')
        for (c in s) when (c) {
            '"' -> b.append("\\\""); '\\' -> b.append("\\\\"); '\n' -> b.append("\\n"); '\t' -> b.append("\\t"); '\r' -> b.append("\\r")
            else -> if (c < ' ' || c == ' ' || c == ' ') b.append("\\u").append(c.code.toString(16).padStart(4, '0')) else b.append(c)
        }
        b.append('"')
    }

    private fun write(name: String, v: Any?) {
        out.mkdirs()
        File(out, name).writeText(j(v))
    }

    // الأعداد العشرية تُكتب نصاً "#<toString>" فيقرؤها CheckContent بدقة
    /** FNV-1a 64 على وحدات UTF-16 (كما يحسبه CheckContent على String.utf16) */
    private fun h(s: String?): String? {
        if (s == null) return null
        var x = 0xcbf29ce484222325uL
        for (c in s) { x = (x xor c.code.toULong()) * 0x100000001b3uL }
        return x.toString(16).padStart(16, '0')
    }

    private val codes: List<String> get() = Lang.entries.map { it.code }

    // ---------- المخزن في الذاكرة ----------

    class MemPrefs : SharedPreferences {
        val m = LinkedHashMap<String, Any?>()
        override fun getAll(): MutableMap<String, *> = LinkedHashMap(m)
        override fun getString(k: String?, d: String?): String? = m[k] as? String ?: d
        override fun getStringSet(k: String?, d: MutableSet<String>?): MutableSet<String>? = d
        override fun getInt(k: String?, d: Int): Int = m[k] as? Int ?: d
        override fun getLong(k: String?, d: Long): Long = m[k] as? Long ?: d
        override fun getFloat(k: String?, d: Float): Float = m[k] as? Float ?: d
        override fun getBoolean(k: String?, d: Boolean): Boolean = m[k] as? Boolean ?: d
        override fun contains(k: String?): Boolean = m.containsKey(k)
        override fun edit(): SharedPreferences.Editor = Ed()
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        inner class Ed : SharedPreferences.Editor {
            private val ops = ArrayList<() -> Unit>()
            override fun putString(k: String, v: String?) = also { ops += { if (v == null) m.remove(k) else m[k] = v } }
            override fun putStringSet(k: String, v: MutableSet<String>?) = this
            override fun putInt(k: String, v: Int) = also { ops += { m[k] = v } }
            override fun putLong(k: String, v: Long) = also { ops += { m[k] = v } }
            override fun putFloat(k: String, v: Float) = also { ops += { m[k] = v } }
            override fun putBoolean(k: String, v: Boolean) = also { ops += { m[k] = v } }
            override fun remove(k: String) = also { ops += { m.remove(k) } }
            override fun clear() = also { ops += { m.clear() } }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() { ops.forEach { it() }; ops.clear() }
        }
    }

    private val prefs = MemPrefs()

    private fun setupContext() {
        if (!AndroidContext.ready) {
            AndroidContext.set(object : ContextWrapper(null) {
                override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
                override fun getApplicationContext() = this
            })
        }
    }

    /** يعيد AppSettings إلى ما قبل init (كأن التطبيق بدأ من جديد) */
    private fun restartSettings() {
        AppSettings::class.java.getDeclaredField("sp").apply { isAccessible = true }.set(null, null)
        // المكان في الذاكرة قبل أن يُختار: مكة، كما في تشغيل جديد للتطبيق
        @Suppress("UNCHECKED_CAST")
        (AppSettings::class.java.getDeclaredField("place\$delegate").apply { isAccessible = true }.get(null) as androidx.compose.runtime.MutableState<Place>).value =
            Place.of(PrayTimes.CITIES[0])
    }

    /** المخزن كما في NSUserDefaults: المفتاح بلا البادئة، والنوع (string | integer | bool) والقيمة */
    private fun dump(): Map<String, Any?> = prefs.m.keys.sorted().associateWith { k ->
        when (val v = prefs.m[k]) {
            is String -> mapOf("t" to "string", "v" to v)
            is Int -> mapOf("t" to "integer", "v" to v)
            is Long -> mapOf("t" to "integer", "v" to v)
            is Boolean -> mapOf("t" to "bool", "v" to v)
            else -> mapOf("t" to "other", "v" to v.toString())
        }
    }

    private fun placeJ(p: Place) = linkedMapOf("key" to p.key, "lat" to p.lat, "lng" to p.lng, "tz" to p.tz, "method" to p.method, "gps" to p.gps, "elev" to p.elev)

    private fun snapshot(): Map<String, Any?> = linkedMapOf(
        "placeSet" to AppSettings.placeSet,
        "place" to placeJ(AppSettings.place),
        "method" to AppSettings.method,
        "hanafiChoice" to (if (prefs.contains("hanafi")) prefs.getBoolean("hanafi", false) else null),
        "use24h" to AppSettings.use24h,
        "iqama" to AppSettings.iqama,
        "theme" to AppSettings.theme,
        "accent" to AppSettings.accent,
        "notify" to AppSettings.notify,
        "alertModes" to AppSettings.alertModes.mapValues { it.value.key },
        "muezzin" to AppSettings.muezzin,
        "fajrMuezzin" to AppSettings.fajrMuezzin,
        "vibePattern" to AppSettings.vibePattern.key,
        "lang" to AppSettings.lang,
    )

    @Test
    fun export() {
        exportAdhkar()
        exportHadith()
        exportTasbihPure()
        exportClock()
        exportTexts()
        setupContext()
        try {
            exportSettings()
            exportTasbihStore()
            exportAdhkarLive()
        } finally {
            // لا يبقى المخزن المصطنع لاختبارات أخرى في العملية نفسها
            restartSettings()
            Tasbih::class.java.getDeclaredField("loaded").apply { isAccessible = true }.setBoolean(Tasbih, false)
            AndroidContext::class.java.getDeclaredField("app").apply { isAccessible = true }.set(AndroidContext, null)
        }
    }

    // ---------- الأذكار ----------

    private fun dhikrJ(d: Dhikr) = linkedMapOf(
        "id" to d.id, "ar" to d.ar, "count" to d.count,
        "ayah" to d.ayah.map { listOf(it.first, it.second, it.third) },
        "src" to d.src.map { listOf(it.first, it.second) },
        "grade" to d.grade, "by" to d.by, "narr" to d.narr, "fadl" to d.fadl, "note" to d.note,
    )

    private fun exportAdhkar() {
        val secs = Adhkar.sections()
        write("adhkar_sections.json", linkedMapOf(
            "sections" to secs.map { s ->
                linkedMapOf("id" to s.id, "icon" to s.icon, "titleAr" to s.titleAr, "descAr" to s.descAr,
                    "groups" to s.groups.map { g -> linkedMapOf("key" to g.key, "titleAr" to g.titleAr, "items" to g.items.map { it.id }) })
            },
            "items" to secs.flatMap { it.items }.associate { it.id to dhikrJ(it) },
        ))

        val langs = linkedMapOf<String, Any?>()
        for (lang in Lang.entries) {
            val tr = Adhkar.tr(lang)
            val s = lang.strings
            langs[lang.code] = linkedMapOf(
                "sections" to tr.sections.mapValues { listOf(it.value.first, it.value.second) },
                "groups" to tr.groups, "books" to tr.books, "grades" to tr.grades, "by" to tr.by,
                "items" to tr.items.mapValues { (_, t) -> listOf(h(t.t), h(t.narr), h(t.fadl), h(t.note)) },
                "sectionTitles" to secs.associate { it.id to listOf(tr.title(it, lang), tr.desc(it, lang)) },
                "groupTitles" to secs.flatMap { it.groups }.associate { g -> g.key to (if (lang == Lang.AR) g.titleAr else tr.groups[g.key] ?: g.titleAr) },
                "translation" to secs.flatMap { it.items }.associate { it.id to h(translation(it, tr, lang, null)) },
                "gradeLine" to secs.flatMap { it.items }.associate { it.id to gradeLine(it, tr) },
                "sourceLine" to secs.flatMap { it.items }.associate { it.id to sourceLine(it, tr, s) },
                "windowTitles" to KINDS.associateWith { k -> WidgetData.title(AdhkarWindow(k, T0, T0, "", emptyList()), lang, WidgetText.of(lang.code), tr) },
            )
        }
        write("adhkar_tr.json", langs)

        // القسم المقترح لكل ساعة (اليوم ليس من رمضان إلا إن قال todayRamadan)
        val today = LocalDate.now()
        write("adhkar_misc.json", linkedMapOf(
            "todayHijriMonth" to Hijri.toHijri(today)?.m,
            "suggested" to (0..23).map { suggested(it) },
            "doneKeys" to secs.associate { it.id to doneKey(it.id) },
            "expand" to TOKENS.associateWith { t -> WidgetData.expand(listOf(t)).map { it.id } },
            "expandLists" to LISTS.mapValues { (_, l) -> WidgetData.expand(l).map { it.id } },
            "clip" to clipCases(),
            "byId" to listOf("mor01", "slp16", "nope", "tsb14", "ram07", "").associateWith { Adhkar.byId(it)?.id },
        ))
    }

    private val T0 = LocalDateTime(2026, 1, 1, 0, 0)
    private val KINDS = listOf("adhan", "iftar", "afterPrayer", "morning", "dhikr", "friday", "arafah", "evening", "night", "lastThird", "sleep", "waking", "unknown")
    private val LISTS = linkedMapOf(
        "adhan" to AdhkarClock.ADHAN, "iftar" to AdhkarClock.IFTAR, "after" to AdhkarClock.AFTER, "morning" to AdhkarClock.MORNING,
        "evening" to AdhkarClock.EVENING, "dhikr" to AdhkarClock.DHIKR, "friday" to AdhkarClock.FRIDAY, "arafah" to AdhkarClock.ARAFAH,
        "night" to AdhkarClock.NIGHT, "lastThird" to AdhkarClock.LAST_THIRD,
        "fridayEveNight" to listOf(WidgetText.SALAWAT, WidgetText.KAHF) + AdhkarClock.NIGHT,
        "fridayEvening" to listOf(WidgetText.HOUR) + AdhkarClock.EVENING,
        "mixed" to listOf("@sleep", "@sleep/2", "@sleep/99", "@nope", "@morning/0", "mor01", "zzz", "fri_hour", "@daily/25"),
    )
    private val TOKENS: List<String> get() = (LISTS.values.flatten() + listOf("@sleep", "@sleep/1", "@sleep/x", "@", "@/0", "@travel/10")).distinct()

    private fun clipCases(): List<Any?> {
        val texts = Adhkar.sections().flatMap { it.items }.take(60).map { it.ar } +
            Adhkar.tr(Lang.EN).items.values.take(60).mapNotNull { it.t } +
            listOf("", "a", "one two three", "x".repeat(200), "word ".repeat(40), "  padded\ntext  ", "كلمة، ".repeat(30), "a b", "abc def ghi jkl mno")
        val out = ArrayList<Any>()
        for (t in texts) for (max in listOf(5, 10, 40, 90, 130)) out.add(listOf(t, max, AdhkarLive.clip(t, max)))
        return out
    }

    // ---------- الأحاديث ----------

    private fun exportHadith() {
        val ids = Hadiths.ids()
        val langs = linkedMapOf<String, Any?>()
        for (code in codes) {
            langs[code] = ids.associateWith { id ->
                val x = Hadiths.get(id, code)!!
                val t = x.tr
                listOf(x.trLang, x.link, t?.let { h(listOf(it.title, it.text, it.expl, it.benefits.joinToString("\u0001"), it.grade, it.takhrij).joinToString("\u0002")) })
            }
        }
        val ar = ids.associateWith { id ->
            val a = Hadiths.get(id, "ar")!!.ar
            listOf(a.title, h(a.text), h(a.expl), a.benefits.size, h(a.benefits.joinToString("\u0001")), a.grade, h(a.takhrij))
        }
        val start = LocalDate(1950, 1, 1)
        val end = LocalDate(2100, 12, 31)
        val idx = ArrayList<Int>()
        var d = start
        while (d <= end) { idx += Hadiths.indexFor(d); d = d.plusDays(1) }
        val days = listOf(LocalDate(2026, 11, 9), LocalDate(2000, 2, 29), LocalDate(1969, 12, 31), LocalDate(1970, 1, 1), LocalDate(2030, 6, 15))
        write("hadith.json", linkedMapOf(
            "ids" to ids, "ar" to ar, "langs" to langs,
            "indexStart" to start.toString(), "index" to idx,
            "forDay" to days.associate { day -> day.toString() to codes.associateWith { c -> Hadiths.forDay(day, c)?.let { listOf(it.id, it.trLang) } } },
            "at" to (-800..800 step 7).associate { it.toString() to Hadiths.at(it, "en")?.id },
            "missing" to Hadiths.get("nope", "en")?.id,
            "unknownLang" to Hadiths.get(ids.first(), "xx")?.trLang,
        ))
    }

    // ---------- المسبحة ----------

    private fun stJ(s: TasbihState) = linkedMapOf(
        "mode" to s.mode, "idx" to s.idx, "step" to s.step, "count" to s.count, "target" to s.target,
        "rounds" to s.rounds, "total" to s.total, "today" to s.today, "day" to s.day,
        "dhikrIndex" to s.dhikrIndex, "text" to s.text, "goal" to s.goal,
    )

    private fun exportTasbihPure() {
        val rnd = Random(20261109)
        val scripts = ArrayList<Any>()
        fun run(init: TasbihState, ops: List<String>) {
            var s = init
            val steps = ArrayList<Any>()
            for (op in ops) {
                var ev: String? = null
                when {
                    op == "count" -> { val (n, e) = s.count(); s = n; ev = e.name }
                    op.startsWith("countN:") -> {
                        // كما في Tasbih.count(times): آخر حدث غير العدّ المجرد
                        var last = Tasbih.Event.COUNT
                        repeat(op.substringAfter(':').toInt().coerceIn(0, 100_000)) { val (n, e) = s.count(); s = n; if (e != Tasbih.Event.COUNT) last = e }
                        ev = last.name
                    }
                    op == "reset" -> s = s.reset()
                    op == "undo" -> s = s.undo()
                    op.startsWith("mode:") -> { val m = op.substringAfter(':'); s = s.copy(mode = if (m == "salah") "salah" else "free", count = 0, step = 0) }
                    op.startsWith("dhikr:") -> s = s.copy(idx = op.substringAfter(':').toInt().coerceIn(Tasbih.DHIKR.indices), count = 0)
                    op.startsWith("target:") -> { val t = op.substringAfter(':').toInt(); s = s.copy(target = if (t in Tasbih.TARGETS) t else 33, count = 0) }
                    op.startsWith("day:") -> s = s.on(op.substringAfter(':'))
                }
                steps += listOf(op, ev, stJ(s).filterKeys { it != "text" })
            }
            scripts += linkedMapOf("init" to stJ(init), "steps" to steps)
        }
        // مسارات معروفة
        run(TasbihState(target = 33), List(70) { "count" })
        run(TasbihState(target = 0), List(120) { "count" } + listOf("undo", "undo", "reset"))
        run(TasbihState(mode = "salah"), List(205) { "count" })
        run(TasbihState(mode = "salah"), List(99) { "count" } + listOf("undo", "count", "count", "reset", "countN:250"))
        run(TasbihState(target = 100, day = "2026-11-01"), listOf("countN:10", "undo", "day:2026-11-01", "day:2026-11-02", "count", "reset", "undo"))
        run(TasbihState(target = 1000), listOf("countN:999", "count", "count", "target:34", "countN:68", "target:7", "count"))
        run(TasbihState(), listOf("dhikr:5", "dhikr:-3", "dhikr:42", "mode:salah", "countN:33", "mode:free", "mode:other", "countN:100000", "countN:100001", "countN:-5"))
        // مسارات عشوائية
        val opsPool = listOf("count", "count", "count", "count", "undo", "reset", "countN", "mode", "dhikr", "target", "day")
        repeat(120) {
            val init = TasbihState(
                mode = if (rnd.nextBoolean()) "salah" else "free", idx = rnd.nextInt(0, 9), step = rnd.nextInt(0, 4),
                count = rnd.nextInt(0, 40), target = Tasbih.TARGETS[rnd.nextInt(Tasbih.TARGETS.size)],
                rounds = rnd.nextInt(0, 5), total = rnd.nextInt(0, 1000), today = rnd.nextInt(0, 300), day = "2026-0${rnd.nextInt(1, 10)}-1${rnd.nextInt(0, 10)}",
            )
            val ops = List(rnd.nextInt(5, 120)) {
                when (opsPool[rnd.nextInt(opsPool.size)]) {
                    "countN" -> "countN:${rnd.nextInt(0, 300)}"
                    "mode" -> "mode:${if (rnd.nextBoolean()) "salah" else "free"}"
                    "dhikr" -> "dhikr:${rnd.nextInt(-2, 12)}"
                    "target" -> "target:${listOf(33, 34, 100, 1000, 0, 5, 99)[rnd.nextInt(7)]}"
                    "day" -> "day:2026-0${rnd.nextInt(1, 10)}-1${rnd.nextInt(0, 10)}"
                    else -> it.let { opsPool[rnd.nextInt(0, 6)] }
                }
            }
            run(init, ops)
        }
        write("tasbih.json", linkedMapOf(
            "dhikr" to Tasbih.DHIKR, "salah" to Tasbih.SALAH.map { listOf(it.first, it.second) }, "tamam" to Tasbih.TAMAM,
            "targets" to Tasbih.TARGETS, "scripts" to scripts,
        ))
    }

    /** المسبحة بمخزنها الحقيقي (ts_*): القراءة عند البدء (قيم فاسدة تُصحَّح) ثم العمليات */
    private fun exportTasbihStore() {
        val loaded = Tasbih::class.java.getDeclaredField("loaded").apply { isAccessible = true }
        val cases = ArrayList<Any>()
        val presets = listOf(
            emptyMap(),
            mapOf("ts_mode" to "xyz", "ts_idx" to 99, "ts_step" to 7, "ts_count" to -5, "ts_target" to 50, "ts_rounds" to -1, "ts_total" to 12, "ts_today" to 9, "ts_day" to "2020-01-01"),
            mapOf("ts_mode" to "salah", "ts_idx" to 3, "ts_step" to 2, "ts_count" to 31, "ts_target" to 1000, "ts_rounds" to 4, "ts_total" to 500, "ts_today" to 77, "ts_day" to LocalDate.now().toString()),
            mapOf("ts_idx" to -4, "ts_step" to -1, "ts_target" to 0, "ts_today" to 5),
        )
        val ops = listOf("count", "countN:40", "undo", "setMode:salah", "countN:99", "count", "reset", "setDhikr:6", "setTarget:100", "setTarget:13", "setMode:free", "countN:250", "undo")
        for (pre in presets) {
            prefs.m.clear(); prefs.m.putAll(pre)
            restartSettings(); AppSettings.init()
            loaded.setBoolean(Tasbih, false)
            Tasbih.ensure()
            val steps = ArrayList<Any>()
            steps += listOf("ensure", null, stJ(Tasbih.state), dump())
            for (op in ops) {
                var ev: String? = null
                when {
                    op == "count" -> ev = Tasbih.count().name
                    op.startsWith("countN:") -> ev = Tasbih.count(op.substringAfter(':').toInt()).name
                    op == "undo" -> Tasbih.undo()
                    op == "reset" -> Tasbih.reset()
                    op.startsWith("setMode:") -> Tasbih.setMode(op.substringAfter(':'))
                    op.startsWith("setDhikr:") -> Tasbih.setDhikr(op.substringAfter(':').toInt())
                    op.startsWith("setTarget:") -> Tasbih.setTarget(op.substringAfter(':').toInt())
                }
                steps += listOf(op, ev, stJ(Tasbih.state), dump())
            }
            cases += linkedMapOf("preset" to pre.mapValues { (_, v) -> mapOf("t" to if (v is String) "string" else "integer", "v" to v) }, "steps" to steps)
        }
        write("tasbih_store.json", linkedMapOf("today" to LocalDate.now().toString(), "cases" to cases))
    }

    // ---------- الإعدادات ----------

    private fun exportSettings() {
        val scenarios = ArrayList<Any>()

        // 1) تثبيت جديد ثم تعديلات من الإعدادات
        prefs.m.clear()
        restartSettings(); AppSettings.init()
        val steps = ArrayList<Any>()
        steps.add(listOf(listOf("init"), dump(), snapshot()))
        val cities = listOf("riyadh", "london", "jakarta").mapNotNull { PrayTimes.city(it) }
        val ops: List<Pair<List<Any?>, () -> Unit>> = listOf(
            listOf<Any?>("updatePlace", placeJ(Place.of(cities[0]))) to { AppSettings.updatePlace(Place.of(cities[0])) },
            listOf<Any?>("updateMethod", "isna") to { AppSettings.updateMethod("isna") },
            listOf<Any?>("updateHanafi", true) to { AppSettings.updateHanafi(true) },
            listOf<Any?>("updateUse24h", true) to { AppSettings.updateUse24h(true) },
            listOf<Any?>("updateIqama", "fajr", 30) to { AppSettings.updateIqama("fajr", 30) },
            listOf<Any?>("updateIqama", "isha", 0) to { AppSettings.updateIqama("isha", 0) },
            listOf<Any?>("updateTheme", "dark") to { AppSettings.updateTheme("dark") },
            listOf<Any?>("updateAccent", "sapphire") to { AppSettings.updateAccent("sapphire") },
            listOf<Any?>("updateNotify", true) to { AppSettings.updateNotify(true) },
            listOf<Any?>("updateAlertMode", "asr", "vibrate") to { AppSettings.updateAlertMode("asr", AlertMode.VIBRATE) },
            listOf<Any?>("updateAlertMode", "fajr", "off") to { AppSettings.updateAlertMode("fajr", AlertMode.OFF) },
            listOf<Any?>("updateMuezzin", "asim") to { AppSettings.updateMuezzin("asim") },
            listOf<Any?>("updateFajrMuezzin", "same") to { AppSettings.updateFajrMuezzin(Muezzins.SAME) },
            listOf<Any?>("updateVibePattern", "knock") to { AppSettings.updateVibePattern(VibePattern.KNOCK) },
            listOf<Any?>("updateLang", "fr") to { AppSettings.updateLang("fr") },
            listOf<Any?>("putInt", "ayah_hint", 2) to { AppSettings.putInt("ayah_hint", 2) },
            listOf<Any?>("putBool", "onboarded", true) to { AppSettings.putBool("onboarded", true) },
            listOf<Any?>("putString", "q_reciter", "husary") to { AppSettings.putString("q_reciter", "husary") },
            listOf<Any?>("updatePlace", placeJ(Place.of(cities[1]))) to { AppSettings.updatePlace(Place.of(cities[1])) },
            listOf<Any?>("updateHanafi", false) to { AppSettings.updateHanafi(false) },
            Place(null, 51.50735, -0.12776, "Europe/London", "moonsighting", gps = true, elev = 11).let { p -> listOf<Any?>("updatePlace", placeJ(p)) to { AppSettings.updatePlace(p) } },
            Place(null, -0.00001, 179.99999, "Pacific/Fiji", "mwl", gps = true, elev = -3).let { p -> listOf<Any?>("updatePlace", placeJ(p)) to { AppSettings.updatePlace(p) } },
            Place(null, 1.0E-4, 1.23456789E7, "UTC", "mwl", gps = true, elev = 0).let { p -> listOf<Any?>("updatePlace", placeJ(p)) to { AppSettings.updatePlace(p) } },
            Place.of(cities[2]).let { p -> listOf<Any?>("updatePlace", placeJ(p)) to { AppSettings.updatePlace(p) } },
            listOf<Any?>("updateLang", "system") to { AppSettings.updateLang("system") },
        )
        for ((desc, f) in ops) { f(); steps.add(listOf(desc, dump(), snapshot())) }
        // بعد إعادة التشغيل تُقرأ القيم كما حُفظت
        restartSettings(); AppSettings.init()
        steps.add(listOf(listOf("init"), dump(), snapshot()))
        scenarios += linkedMapOf("name" to "fresh", "preset" to emptyMap<String, Any?>(), "steps" to steps)

        // 2) ترحيلات الإصدارات القديمة
        fun legacy(name: String, pre: Map<String, Any>) {
            prefs.m.clear(); prefs.m.putAll(pre)
            val lat = (pre["place_lat"] as? String)?.toDouble() ?: 0.0
            val lng = (pre["place_lng"] as? String)?.toDouble() ?: 0.0
            restartSettings(); AppSettings.init()
            val p = AppSettings.place
            scenarios += linkedMapOf(
                "name" to name,
                "preset" to pre.mapValues { (_, v) -> mapOf("t" to when (v) { is String -> "string"; is Boolean -> "bool"; else -> "integer" }, "v" to v) },
                // ما تحتاجه Swift من بيانات المدن (MizanPrayer): الارتفاع القريب والطريقة الافتراضية للمكان المقروء قبل الترحيل
                "elevationNear" to PrayTimes.elevationNear(lat, lng),
                "defaultMethod" to AppSettings.defaultMethod(p.copy(key = (pre["place_key"] as? String)?.takeIf { it.isNotEmpty() } ?: prefs.getString("place_key", null)?.takeIf { it.isNotEmpty() })),
                "steps" to listOf(listOf(listOf("init"), dump(), snapshot())),
            )
        }
        val riyadhAr = Ar.cities["riyadh"]!!
        legacy("arabicName", mapOf("place_name" to riyadhAr, "place_country" to "السعودية", "place_lat" to "24.7136", "place_lng" to "46.6753", "place_tz" to "Asia/Riyadh", "place_method" to "mwl", "method" to "mwl"))
        legacy("arabicUnknownName", mapOf("place_name" to "قرية ما", "place_lat" to "30.1", "place_lng" to "31.2", "place_tz" to "Africa/Cairo", "place_method" to "egypt", "method" to "egypt", "use24h" to true))
        legacy("chosenMethod", mapOf("place_key" to "jakarta", "place_lat" to "-6.2088", "place_lng" to "106.8456", "place_tz" to "Asia/Jakarta", "place_method" to "mwl", "method" to "isna", "lang" to "en", "place_gps" to false))
        legacy("gpsFar", mapOf("place_key" to "", "place_lat" to "40.4", "place_lng" to "-3.7", "place_tz" to "Europe/Madrid", "place_method" to "mwl", "method" to "mwl", "lang" to "es", "place_gps" to true, "rules_v" to 1))
        legacy("current", mapOf("place_key" to "london", "place_lat" to "51.5074", "place_lng" to "-0.1278", "place_tz" to "Europe/London", "place_method" to "uk", "method" to "uk", "lang" to "en", "rules_v" to 2, "place_elev" to "35", "hanafi" to false, "iqama_maghrib" to 5, "alert_dhuhr" to "silent", "alert_isha" to "bogus", "vibe_pattern" to "nope", "theme" to "light"))
        legacy("noPlace", mapOf("lang" to "de", "rules_v" to 1, "accent" to "rose"))
        legacy("noMethod", mapOf("place_key" to "riyadh", "place_lat" to "24.7136", "place_lng" to "46.6753", "lang" to "ar", "rules_v" to 2))

        // كتابة الأعداد العشرية نصاً (Double.toString في Kotlin) لخطوط الطول والعرض
        val doubles = PrayTimes.CITIES.flatMap { listOf(it.lat, it.lng) } + listOf(
            0.0, -0.0, 1.0, -1.5, 0.001, 0.0009999, 1.0E-4, 1.0E-5, -0.00001, 123.456, 9999999.0, 1.0E7, 1.2345678E7, 51.50735, -0.12776,
            21.422487, 39.826206, 0.1 + 0.2, 1.0 / 3.0, 2.0 / 3.0, 100.0, 1e-300, 1e300, 89.99999999999999,
        )
        write("settings.json", linkedMapOf(
            "suite" to "mizan",
            "systemTimeZone" to kotlinx.datetime.TimeZone.currentSystemDefault().id,
            "arabicCities" to Ar.cities.toList(),
            "scenarios" to scenarios,
            "doubles" to doubles.map { listOf(it, it.toString()) },
            "defaults" to linkedMapOf(
                "place" to placeJ(Place.of(PrayTimes.CITIES[0])), "iqama" to com.osatks.mizan.core.IQAMA_DEFAULT,
                "muezzin" to Muezzins.DEFAULT.id, "fajrMuezzin" to Muezzins.FAJR_DEFAULT.id, "vibePattern" to VibePattern.DEFAULT.key,
            ),
        ))
    }

    // ---------- عدّاد الأذكار (al_*) ----------

    private fun posJ(p: AdhkarLive.Position) = linkedMapOf("section" to p.section, "index" to p.index, "count" to p.count, "day" to p.day, "done" to p.done)

    private fun exportAdhkarLive() {
        prefs.m.clear(); restartSettings(); AppSettings.init()
        val today = LocalDate.now().toString()
        val runs = ArrayList<Any>()
        fun run(name: String, pre: Map<String, Any>, ops: List<String>) {
            prefs.m.clear(); prefs.m.putAll(pre)
            val steps = ArrayList<Any>()
            for (op in ops) {
                val (sec, act) = op.split(':')
                val (p, e) = when (act) {
                    "count" -> AdhkarLive.count(sec)
                    "skip" -> AdhkarLive.skip(sec)
                    "restart" -> AdhkarLive.restart(sec) to null
                    else -> AdhkarLive.position(sec) to null
                }
                steps += listOf(op, e?.name, posJ(p), dump())
            }
            runs += linkedMapOf("name" to name, "preset" to pre.mapValues { (_, v) -> mapOf("t" to when (v) { is String -> "string"; is Boolean -> "bool"; else -> "integer" }, "v" to v) }, "steps" to steps)
        }
        val morning = Adhkar.section("morning")!!.items.sumOf { it.count }
        run("morningAll", emptyMap(), listOf("morning:position") + List(morning + 3) { "morning:count" } + listOf("morning:restart", "morning:count"))
        run("skips", emptyMap(), List(5) { "evening:count" } + List(Adhkar.section("evening")!!.items.size + 2) { "evening:skip" } + listOf("evening:count", "morning:count", "evening:position"))
        run("resume", mapOf("al_section" to "morning", "al_index" to 4, "al_count" to 2, "al_day" to today, "al_done" to false), listOf("morning:position", "morning:count", "evening:position", "evening:count", "morning:count"))
        run("oldDay", mapOf("al_section" to "morning", "al_index" to 4, "al_count" to 2, "al_day" to "2020-01-01", "al_done" to true), listOf("morning:position", "morning:count"))
        run("doneToday", mapOf("al_section" to "evening", "al_index" to 2, "al_count" to 0, "al_day" to today, "al_done" to true), listOf("evening:count", "evening:skip", "evening:restart", "evening:skip"))
        run("outOfRange", mapOf("al_section" to "sleep", "al_index" to 999, "al_count" to 0, "al_day" to today), listOf("sleep:count", "sleep:count", "sleep:skip", "nope:count", "nope:skip", "nope:position"))

        // الحال للنشاط الحي بكل اللغات (بلا tap و rtl: من Strings و Lang)
        val states = linkedMapOf<String, Any?>()
        for (lang in Lang.entries) {
            val list = ArrayList<Any>()
            for (sec in listOf("morning", "evening", "sleep", "nope")) {
                val n = Adhkar.section(sec)?.items?.size ?: 0
                for (p in listOf(
                    AdhkarLive.Position(sec, 0, 0, today), AdhkarLive.Position(sec, 3, 1, today), AdhkarLive.Position(sec, n - 1, 0, today),
                    AdhkarLive.Position(sec, n + 5, 2, today), AdhkarLive.Position(sec, -2, 0, today), AdhkarLive.Position(sec, 0, 0, today, done = true),
                )) {
                    val st = AdhkarLive.state(p, lang).filterKeys { it != "tap" && it != "rtl" }
                    list.add(listOf(posJ(p), st))
                }
            }
            states[lang.code] = list
        }
        write("adhkar_live.json", linkedMapOf("today" to today, "runs" to runs, "states" to states))
    }

    // ---------- أذكار الوقت ----------

    private fun ptJ(t: PrayerTimes) = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)

    private fun epoch(t: LocalDateTime) = t.toInstant(TimeZone.UTC).epochSeconds

    private fun exportClock() {
        class Loc(val name: String, val lat: Double, val lng: Double, val tz: String, val method: String, val from: LocalDate, val days: Int, val step: Int)
        val locs = listOf(
            Loc("makkah", 21.4225, 39.8262, "Asia/Riyadh", "makkah", LocalDate(2026, 1, 1), 365, 1),
            Loc("london", 51.5074, -0.1278, "Europe/London", "uk", LocalDate(2025, 1, 3), 730, 9),
            Loc("jakarta", -6.2088, 106.8456, "Asia/Jakarta", "indonesia", LocalDate(2027, 1, 2), 365, 11),
            Loc("reykjavik", 64.1466, -21.9426, "Atlantic/Reykjavik", "mwl", LocalDate(2026, 1, 1), 365, 6),
            Loc("tromso", 69.6492, 18.9553, "Europe/Oslo", "mwl", LocalDate(2026, 1, 1), 365, 5),
            Loc("svalbard", 78.2232, 15.6267, "Arctic/Longyearbyen", "mwl", LocalDate(2026, 1, 2), 365, 8),
        )
        val cases = ArrayList<Any>()
        val rnd = Random(7)
        val lists = LinkedHashMap<String, Int>()
        fun li(items: List<String>): Int = lists.getOrPut(items.joinToString(",")) { lists.size }
        fun wJ(w: AdhkarWindow) = listOf(w.kind, epoch(w.start), epoch(w.end), w.section, li(w.items), w.prayer, w.tag, w.key)
        for (loc in locs) {
            val timesCache = HashMap<LocalDate, PrayerTimes>()
            val times = { d: LocalDate -> timesCache.getOrPut(d) { PrayTimes.compute(d, loc.lat, loc.lng, loc.tz, loc.method, false) } }
            val hijri = { d: LocalDate -> Hijri.toHijri(d) }
            var i = 0
            while (i < loc.days) {
                val day = loc.from.plusDays(i)
                val ws = AdhkarClock.day(day, times, hijri)
                val ats = List(4) { day.atTimeOf(rnd.nextLong(0, 86400 * 2)) }
                cases += linkedMapOf(
                    "loc" to loc.name, "day" to day.toString(),
                    "windows" to ws.map { wJ(it) },
                    "at" to ats.map { now -> val w = AdhkarClock.at(now, times, hijri); listOf(now.toString(), epoch(now), w.key, AdhkarClock.index(now, w, 3, 7), AdhkarClock.index(now, w, -10, 4), AdhkarClock.index(now, w, 0, 0)) },
                )
                i += loc.step
            }
            // المدخلات: مواقيت وهجري كل يوم احتاجته الحسابات
            cases += linkedMapOf("loc" to loc.name, "inputs" to timesCache.keys.sorted().associate { d ->
                d.toString() to listOf(ptJ(timesCache[d]!!), Hijri.toHijri(d)?.let { listOf(it.y, it.m, it.d) })
            })
        }
        // مواقيت مصطنعة: NaN ولا نهاية وترتيب مقلوب
        val synth = listOf(
            PrayerTimes(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN),
            PrayerTimes(5.0, 4.0, 12.0, 11.0, 18.0, 17.0),
            PrayerTimes(1.2, 2.5, 13.1, 19.5, 23.9, 25.2),
            PrayerTimes(Double.POSITIVE_INFINITY, 6.0, Double.NEGATIVE_INFINITY, 15.0, Double.NaN, 21.0),
            PrayerTimes(4.5, 6.1, 12.2, 15.6, 18.3, 19.7),
        )
        val synthCases = synth.map { t -> listOf(listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha).map { if (it.isNaN()) "nan" else if (it == Double.POSITIVE_INFINITY) "inf" else if (it == Double.NEGATIVE_INFINITY) "-inf" else it }, AdhkarClock.sane(t).toList()) }
        val synthDays = ArrayList<Any>()
        for ((si, t) in synth.withIndex()) for (hd in listOf(null, HDate(1447, 9, 1), HDate(1447, 9, 25), HDate(1447, 12, 9), HDate(1447, 1, 1))) {
            for (day in listOf(LocalDate(2026, 11, 5), LocalDate(2026, 11, 6), LocalDate(2026, 11, 7))) {
                val ws = AdhkarClock.day(day, { t }, { hd })
                synthDays += listOf(si, hd?.let { listOf(it.y, it.m, it.d) }, day.toString(), ws.map { wJ(it) })
            }
        }
        write("adhkar_clock.json", linkedMapOf(
            "adhanMin" to AdhkarClock.ADHAN_MIN, "afterMin" to AdhkarClock.AFTER_MIN, "rotateSec" to AdhkarClock.ROTATE_SEC,
            "sections" to KINDS.associateWith { AdhkarClock.sectionOf(it) },
            "cases" to cases, "sane" to synthCases, "synthDays" to synthDays, "lists" to lists.keys.toList(),
        ))
    }

    private fun LocalDate.atTimeOf(sec: Long): LocalDateTime = LocalDateTime(this, kotlinx.datetime.LocalTime(0, 0)).plusSeconds(sec)

    // ---------- النصوص والجداول (يُولَّد منها Swift ويُقارن بها) ----------

    private fun exportTexts() {
        val lt = LiveText.all.mapValues { (_, t) ->
            linkedMapOf("end" to t.end, "iqamaIn" to t.iqamaIn, "iftar" to t.iftar, "suhoor" to t.suhoor, "iftarNow" to t.iftarNow,
                "adhkarDone" to t.adhkarDone, "tasbih" to t.tasbih, "descTasbih" to t.descTasbih, "wird" to t.wird, "descWird" to t.descWird,
                "showLive" to t.showLive, "again" to t.again)
        }
        val wcodes = listOf("ar", "en", "fr", "tr", "ur", "id", "ms", "fa", "bn", "ru", "de", "es", "uk", "uz", "kk")
        val wt = wcodes.associateWith { c ->
            val t = WidgetText.of(c)
            linkedMapOf("adhkar" to t.adhkar, "nextPrayer" to t.nextPrayer, "prayerTimes" to t.prayerTimes, "hijri" to t.hijri, "dhikr" to t.dhikr,
                "friday" to t.friday, "arafah" to t.arafah, "iftar" to t.iftar, "lastThird" to t.lastThird, "fridayEve" to t.fridayEve,
                "lastTen" to t.lastTen, "until" to t.until, "next" to t.next, "inTime" to t.inTime, "openApp" to t.openApp, "kahfCta" to t.kahfCta,
                "descNext" to t.descNext, "descTimes" to t.descTimes, "descHijri" to t.descHijri, "descAyah" to t.descAyah, "descAdhkar" to t.descAdhkar,
                "kahf" to t.kahf, "salawat" to t.salawat, "hour" to t.hour, "after" to t.after, "allowLocation" to t.allowLocation, "locationHint" to t.locationHint)
        }
        val ltt = (wcodes + listOf("xx")).associateWith { c -> LiveTasbihText.of(c).let { listOf(it.first, it.second) } }
        write("texts.json", linkedMapOf(
            "liveText" to lt, "widgetText" to wt, "liveTasbihText" to ltt, "iftarDua" to LiveText.IFTAR_DUA,
            "widgetExtra" to WidgetText.EXTRA.values.map { dhikrJ(it) },
            "fallback" to listOf(LiveText.of("xx") === LiveText.EN, WidgetText.of("xx") === WidgetText.EN),
            "muezzins" to AdhanCatalog.ALL.map { m -> linkedMapOf("id" to m.id, "fajr" to m.fajr, "bundled" to m.bundled, "seconds" to m.seconds, "sizeKb" to m.sizeKb, "takbir" to m.takbir, "credit" to m.credit, "file" to m.file, "iosSound" to m.iosSound) },
            "muezzinDefault" to Muezzins.DEFAULT.id, "muezzinFajrDefault" to Muezzins.FAJR_DEFAULT.id,
            "alertModes" to AlertMode.entries.map { it.key },
            "vibePatterns" to VibePattern.entries.map { v -> linkedMapOf("key" to v.key, "timings" to v.timings, "amps" to v.amps, "millis" to v.millis, "pulses" to v.pulses.map { listOf(it.first, it.second, it.third) }) },
        ))
    }
}
