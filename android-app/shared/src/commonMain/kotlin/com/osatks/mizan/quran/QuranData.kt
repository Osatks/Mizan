package com.osatks.mizan.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.pack.QuranTranslit
import com.osatks.mizan.i18n.pack.Translit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.locked
import com.osatks.mizan.platform.path
import kotlinx.coroutines.IO

enum class SourceKind { TAFSIR, TRANSLATION }

/** مصدر تفسير أو ترجمة معانٍ: مدمج (ملف من tanzil.net/trans في assets/tafsir) أو منزَّل من مكتبة التفاسير */
data class TafsirSource(val id: String, val lang: Lang, val kind: SourceKind, val author: String, val remote: RemoteTafsir? = null)

object Sources {
    private val BUILT_IN = listOf(
        TafsirSource("ar.muyassar", Lang.AR, SourceKind.TAFSIR, "مجمع الملك فهد لطباعة المصحف الشريف"),
        TafsirSource("ar.jalalayn", Lang.AR, SourceKind.TAFSIR, "جلال الدين المحلي وجلال الدين السيوطي"),
        TafsirSource("id.jalalayn", Lang.ID, SourceKind.TAFSIR, "Tafsir Jalalayn (Bahasa Indonesia)"),
        TafsirSource("en.sahih", Lang.EN, SourceKind.TRANSLATION, "Saheeh International"),
        TafsirSource("fr.hamidullah", Lang.FR, SourceKind.TRANSLATION, "Muhammad Hamidullah"),
        TafsirSource("tr.diyanet", Lang.TR, SourceKind.TRANSLATION, "Diyanet İşleri"),
        TafsirSource("ur.jalandhry", Lang.UR, SourceKind.TRANSLATION, "فتح محمد جالندھری"),
        TafsirSource("id.indonesian", Lang.ID, SourceKind.TRANSLATION, "Kementerian Agama RI"),
        TafsirSource("ms.basmeih", Lang.MS, SourceKind.TRANSLATION, "Abdullah Muhammad Basmeih"),
        TafsirSource("fa.fooladvand", Lang.FA, SourceKind.TRANSLATION, "محمدمهدی فولادوند"),
        TafsirSource("bn.bengali", Lang.BN, SourceKind.TRANSLATION, "মুহিউদ্দীন খান"),
        TafsirSource("ru.kuliev", Lang.RU, SourceKind.TRANSLATION, "Эльмир Кулиев"),
        TafsirSource("de.bubenheim", Lang.DE, SourceKind.TRANSLATION, "Bubenheim & Elyas"),
        TafsirSource("es.garcia", Lang.ES, SourceKind.TRANSLATION, "Muhammad Isa García"),
        TafsirSource("uk.yakubovych", Lang.UK, SourceKind.TRANSLATION, "Михайло Якубович"),
    )

    /** المصادر المدمجة وترجمات معاني لغات الحزم المثبتة */
    val ALL: List<TafsirSource>
        get() = BUILT_IN + Lang.packs.mapNotNull { l -> l.pack?.quran?.let { q -> TafsirSource(q.id, l, SourceKind.TRANSLATION, q.author) } }

    private fun RemoteTafsir.asSource() = TafsirSource(id, lang, SourceKind.TAFSIR, author, this)

    /** المصادر الموجودة على الجهاز: المدمجة في التطبيق، ثم ما نزّله المستخدم من مكتبة التفاسير */
    fun installed(): List<TafsirSource> = ALL + TafsirLibrary.downloaded().map { it.asSource() }

    fun byId(id: String?) = installed().firstOrNull { it.id == id }

    /** ترجمة المعاني بلغة الواجهة (لا توجد للعربية) */
    fun translationFor(lang: Lang) = ALL.firstOrNull { it.lang == lang && it.kind == SourceKind.TRANSLATION }

    /** المصدر الافتراضي عند فتح التفسير: تفسير بلغة الواجهة إن وُجد (الميسر للعربية)، وإلا ترجمتها، من المصادر الظاهرة */
    fun defaultFor(lang: Lang): TafsirSource {
        val v = visible(lang)
        return v.firstOrNull { it.lang == lang && it.kind == SourceKind.TAFSIR } ?: v.firstOrNull { it.lang == lang } ?: v.first()
    }

    /** مصادر لغة الواجهة أولاً، ثم العربية، ثم بقية اللغات */
    fun ordered(lang: Lang, list: List<TafsirSource> = installed()): List<TafsirSource> {
        // لغة الحزمة قد تقترح لغات تُعرض تفاسيرها بعدها (كالروسية للكازاخية)
        val near = lang.pack?.tafsirFallback.orEmpty().filter { it != lang.code && it != Lang.AR.code }
        return list.filter { it.lang == lang } + near.flatMap { c -> list.filter { it.lang.code == c } } +
            list.filter { it.lang == Lang.AR && it.lang != lang } +
            list.filter { it.lang != lang && it.lang != Lang.AR && it.lang.code !in near }
    }

    /** ما يظهر في ورقة التفسير: المصادر التي لم يُخفها المستخدم (ويبقى واحد على الأقل) */
    fun visible(lang: Lang): List<TafsirSource> =
        ordered(lang).filter { !QuranPrefs.isHidden(it.id) }.ifEmpty { listOf(ALL[0]) }

    /** اسم قصير يظهر على زر المصدر */
    fun label(src: TafsirSource, s: Strings, lang: Lang): String = src.remote?.name(lang) ?: when (src.id) {
        "ar.muyassar" -> s.tafsirMuyassar
        "ar.jalalayn" -> s.tafsirJalalayn
        "id.jalalayn" -> "Tafsir Jalalayn"
        else -> src.lang.native
    }

    /** تفسير آية إن كان جاهزاً في الذاكرة (فلا يومض مؤشر التحميل عند التنقل بين الآيات) */
    fun cachedEntry(src: TafsirSource, surah: Int, ayah: Int): TafsirEntry? =
        if (src.remote != null) TafsirLibrary.cachedEntry(src.remote.slug, surah, ayah)
        else QuranData.cached("tafsir/${src.id}.txt")?.let { TafsirEntry(it[QuranMeta.index(surah, ayah)], ayah, ayah) }

    suspend fun entry(src: TafsirSource, surah: Int, ayah: Int): TafsirEntry? =
        if (src.remote != null) TafsirLibrary.entry(src.remote.slug, surah, ayah)
        else TafsirEntry(QuranData.source(src.id)[QuranMeta.index(surah, ayah)], ayah, ayah)

    fun kindLabel(src: TafsirSource, s: Strings) = if (src.kind == SourceKind.TAFSIR) s.tafsirTitle else s.translationOfMeanings
}

/**
 * نصوص المصحف والتفاسير المضمّنة (ملفات تنزيل كما هي، بصيغة "سورة|آية|نص").
 * تُقرأ عند أول طلب في الخلفية وتبقى في الذاكرة.
 */
object QuranData {
    /** يُضبط عند بدء التطبيق؛ والاختبارات تقرأ الملفات من القرص مباشرة */
    /** ترجمات معاني الحزم المنزَّلة في files/lang، وما عداها في assets */
    var open: (String) -> ByteArray? = { path -> Fs.read(path(Platform.filesDir, "lang", path)) ?: Platform.asset(path) }

    private val cache = SyncMap<String, Array<String>>()
    private val locks = SyncMap<String, Lock>()

    fun cached(path: String): Array<String>? = cache[path]

    suspend fun load(path: String): Array<String> = cache[path] ?: withContext(Dispatchers.IO) {
        locks.getOrPut(path) { Lock() }.locked {
            cache[path] ?: parse(open(path) ?: error("missing $path")).also { a ->
                // نص بخط آخر (ترجمة الأوزبكية الكيريلية تُعرض باللاتينية)
                QuranTranslit.map[path.substringAfterLast('/').removeSuffix(".txt")]?.let { t -> for (i in a.indices) a[i] = Translit.apply(t, a[i]) }
                cache[path] = a
            }
        }
    }

    suspend fun uthmani() = load(UTHMANI)
    suspend fun clean() = load(CLEAN)
    suspend fun source(id: String) = load("tafsir/$id.txt")

    const val UTHMANI = "quran/uthmani.txt"
    const val CLEAN = "quran/clean.txt"

    fun parse(bytes: ByteArray): Array<String> {
        val out = arrayOfNulls<String>(QuranMeta.AYAHS)
        bytes.decodeToString().lineSequence().let { lines ->
            lines.forEach { line ->
                if (line.isEmpty() || line[0] == '#') return@forEach
                val a = line.indexOf('|')
                val b = line.indexOf('|', a + 1)
                if (a <= 0 || b <= a) return@forEach
                val surah = line.substring(0, a).toIntOrNull() ?: return@forEach
                val ayah = line.substring(a + 1, b).toIntOrNull() ?: return@forEach
                if (surah !in 1..QuranMeta.SURAHS || ayah !in 1..QuranMeta.COUNTS[surah - 1]) return@forEach
                out[QuranMeta.index(surah, ayah)] = line.substring(b + 1).trim()
            }
        }
        return Array(QuranMeta.AYAHS) { out[it] ?: "" }
    }

    /**
     * في نص تنزيل تبدأ الآية الأولى من كل سورة بالبسملة (عدا الفاتحة، فهي آيتها الأولى، والتوبة بلا بسملة).
     * تُعرض البسملة زخرفةً مستقلة فوق السورة، وتُعرض الآية بدونها.
     */
    fun withoutBismillah(text: Array<String>, index: Int): String {
        val (surah, ayah) = QuranMeta.locate(index)
        val t = text[index]
        if (ayah != 1 || surah == 1 || surah == 9) return t
        // البسملة أربع كلمات؛ قد تختلف علامات تشكيلها (في التين والقدر: بِّسْمِ)، فتُقارن بلا تشكيل
        val words = t.split(' ')
        if (words.size <= 4) return t
        val head = words.take(4).joinToString(" ")
        return if (Search.normalize(head) == Search.normalize(text[0])) words.drop(4).joinToString(" ") else t
    }

    fun hasBismillahHeader(surah: Int) = surah != 1 && surah != 9
}

/** إعدادات القراءة: آخر موضع، والآيات المحفوظة، وحجم الخط، وطريقة العرض، والمصدر المختار */
object QuranPrefs {
    private var loaded = false
    val bookmarks = mutableStateListOf<Int>()
    var last by mutableIntStateOf(-1)
        private set
    var textSize by mutableIntStateOf(26)
        private set
    var mode by mutableStateOf("")     // "" = حسب اللغة | mushaf | list
        private set
    var source by mutableStateOf("")   // "" = حسب اللغة
        private set
    /** المصادر التي أخفاها المستخدم من ورقة التفسير */
    val hidden = mutableStateListOf<String>()
    /** لون الورق في القراءة: "" مثل مظهر التطبيق | sepia ورقي دافئ | night ليلي */
    var tone by mutableStateOf("")
        private set
    /** سطوع الشاشة أثناء القراءة من 0 إلى 1، أو -1 لسطوع الجهاز */
    var brightness by mutableFloatStateOf(-1f)
        private set
    /** القارئ (اسم مجلده في everyayah)، و repeat تكرار كل آية (0 = بلا نهاية) */
    var reciter by mutableStateOf("")
        private set
    var repeat by mutableIntStateOf(1)
        private set
    /** آخر موضع (أول آية في الصفحة) في وضع التركيز، لزر «تابع في وضع التركيز» */
    var focusLast by mutableIntStateOf(-1)
        private set
    /** مقطع التلاوة: continue متواصل | page هذه الصفحة | surah هذه السورة | custom من آية إلى آية */
    var rangeMode by mutableStateOf("continue")
        private set
    var customFrom by mutableIntStateOf(0)
        private set
    var customTo by mutableIntStateOf(6)
        private set
    /** تكرار المقطع كله (0 = بلا نهاية) */
    var rangeRepeat by mutableIntStateOf(1)
        private set
    /** وقفة بعد كل آية بالثواني ليرددها القارئ (0 بلا وقفة، -1 بطول الآية) */
    var gap by mutableIntStateOf(0)
        private set
    var speed by mutableFloatStateOf(1f)
        private set
    /** مؤقت الإيقاف بالدقائق (0 بلا مؤقت) */
    var sleep by mutableIntStateOf(0)
        private set

    fun ensure() {
        if (loaded) return
        loaded = true
        last = AppSettings.getInt("q_last", -1)
        textSize = AppSettings.getInt("q_size", 26)
        mode = AppSettings.getString("q_mode")
        source = AppSettings.getString("q_src")
        hidden.clear()
        hidden += AppSettings.getString("q_hidden").split(',').filter { it.isNotEmpty() }
        tone = AppSettings.getString("q_tone")
        brightness = AppSettings.getInt("q_bright", -1).let { if (it < 0) -1f else it / 100f }
        reciter = AppSettings.getString("q_reciter")
        repeat = AppSettings.getInt("q_repeat", 1)
        focusLast = AppSettings.getInt("q_focus_last", -1)
        rangeMode = AppSettings.getString("q_rmode").ifEmpty { "continue" }
        customFrom = AppSettings.getInt("q_cfrom", 0).coerceIn(0, QuranMeta.AYAHS - 1)
        customTo = AppSettings.getInt("q_cto", 6).coerceIn(0, QuranMeta.AYAHS - 1)
        rangeRepeat = AppSettings.getInt("q_rrep", 1)
        gap = AppSettings.getInt("q_gap", 0)
        speed = AppSettings.getInt("q_speed", 100) / 100f
        sleep = AppSettings.getInt("q_sleep", 0)
        bookmarks.clear()
        bookmarks += AppSettings.getString("q_marks").split(',').mapNotNull { it.toIntOrNull() }.filter { it in 0 until QuranMeta.AYAHS }
    }

    fun modeFor(lang: Lang) = mode.ifEmpty { if (lang == Lang.AR) "mushaf" else "list" }
    fun sourceFor(lang: Lang) = Sources.byId(source)?.takeIf { !isHidden(it.id) } ?: Sources.defaultFor(lang)

    fun isHidden(id: String) = id in hidden
    fun setVisible(id: String, visible: Boolean) {
        if (visible) hidden.remove(id) else if (id !in hidden) hidden.add(id)
        AppSettings.putString("q_hidden", hidden.joinToString(","))
    }

    fun updateLast(i: Int) { if (i != last) { last = i; AppSettings.putInt("q_last", i) } }
    fun updateSize(v: Int) { textSize = v.coerceIn(18, 40); AppSettings.putInt("q_size", textSize) }
    fun updateMode(v: String) { mode = v; AppSettings.putString("q_mode", v) }
    fun updateSource(id: String) { source = id; AppSettings.putString("q_src", id) }
    fun updateTone(v: String) { tone = v; AppSettings.putString("q_tone", v) }
    fun updateBrightness(v: Float) { brightness = if (v < 0f) -1f else v.coerceIn(0.02f, 1f); AppSettings.putInt("q_bright", if (v < 0f) -1 else (brightness * 100).toInt()) }
    fun updateReciter(id: String) { reciter = id; AppSettings.putString("q_reciter", id) }
    fun updateRepeat(n: Int) { repeat = n; AppSettings.putInt("q_repeat", n) }
    fun updateRangeMode(v: String) { rangeMode = v; AppSettings.putString("q_rmode", v) }
    fun updateCustom(from: Int, to: Int) {
        customFrom = from.coerceIn(0, QuranMeta.AYAHS - 1); customTo = to.coerceIn(customFrom, QuranMeta.AYAHS - 1)
        AppSettings.putInt("q_cfrom", customFrom); AppSettings.putInt("q_cto", customTo)
    }
    fun updateRangeRepeat(n: Int) { rangeRepeat = n; AppSettings.putInt("q_rrep", n) }
    fun updateGap(n: Int) { gap = n; AppSettings.putInt("q_gap", n) }
    fun updateSpeed(v: Float) { speed = v; AppSettings.putInt("q_speed", (v * 100).toInt()) }
    fun updateSleep(n: Int) { sleep = n; AppSettings.putInt("q_sleep", n) }
    fun updateFocusLast(i: Int) { if (i != focusLast) { focusLast = i; AppSettings.putInt("q_focus_last", i) } }

    fun isBookmarked(i: Int) = i in bookmarks
    fun toggleBookmark(i: Int) {
        if (!bookmarks.remove(i)) bookmarks.add(0, i)
        AppSettings.putString("q_marks", bookmarks.joinToString(","))
    }
}

/** البحث في الآيات: بالنص العربي (بلا تشكيل) وبترجمة لغة الواجهة، أو بالإحالة مثل 2:255 */
object QuranSearch {
    private val normalized = SyncMap<String, Array<String>>()

    private suspend fun norm(path: String, load: suspend () -> Array<String>): Array<String> =
        normalized[path] ?: withContext(Dispatchers.Default) {
            val src = load()
            Array(src.size) { Search.normalize(src[it]) }.also { normalized[path] = it }
        }

    /** "2:255" أو "2 255" أو "٢:٢٥٥" ← فهرس الآية */
    fun parseRef(q: String): Int? {
        val digits = Search.normalize(q)
        val m = Regex("^(\\d{1,3})\\s*[:.\\s]\\s*(\\d{1,3})$").find(digits.replace(" : ", ":")) ?: return null
        val surah = m.groupValues[1].toInt(); val ayah = m.groupValues[2].toInt()
        if (surah !in 1..QuranMeta.SURAHS || ayah !in 1..QuranMeta.COUNTS[surah - 1]) return null
        return QuranMeta.index(surah, ayah)
    }

    suspend fun ayahs(query: String, lang: Lang, limit: Int = 60): List<Int> {
        val terms = Search.terms(query)
        if (terms.isEmpty() || terms.sumOf { it.length } < 2) return emptyList()
        val out = LinkedHashSet<Int>()
        val arabicScript = query.any { it in '؀'..'ۿ' }
        if (arabicScript) {
            val clean = norm(QuranData.CLEAN) { QuranData.clean() }
            for (i in clean.indices) { if (terms.all { clean[i].contains(it) }) out += i; if (out.size >= limit) break }
        }
        val tr = Sources.translationFor(lang)
        if (tr != null && out.size < limit) {
            val text = norm("tafsir/${tr.id}") { QuranData.source(tr.id) }
            for (i in text.indices) { if (terms.all { text[i].contains(it) }) out += i; if (out.size >= limit) break }
        }
        return out.toList()
    }
}

/** أرقام عربية مشرقية لعلامات الآيات (١٢٣) */
fun arabicDigits(n: Int): String = n.toString().map { '٠' + (it - '0') }.joinToString("")
