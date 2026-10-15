package com.osatks.mizan.quran

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

enum class SourceKind { TAFSIR, TRANSLATION }

/** مصدر تفسير أو ترجمة معانٍ (ملف من tanzil.net/trans في assets/tafsir) */
data class TafsirSource(val id: String, val lang: Lang, val kind: SourceKind, val author: String)

object Sources {
    val ALL = listOf(
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
    )

    fun byId(id: String?) = ALL.firstOrNull { it.id == id }

    /** ترجمة المعاني بلغة الواجهة (لا توجد للعربية) */
    fun translationFor(lang: Lang) = ALL.firstOrNull { it.lang == lang && it.kind == SourceKind.TRANSLATION }

    /** المصدر الافتراضي عند فتح التفسير: التفسير الميسر للعربية، وتفسير بلغة الواجهة إن وُجد، وإلا ترجمتها */
    fun defaultFor(lang: Lang) = ALL.firstOrNull { it.lang == lang && it.kind == SourceKind.TAFSIR }
        ?: translationFor(lang) ?: ALL[0]

    /** مصادر لغة الواجهة أولاً، ثم التفاسير العربية، ثم بقية الترجمات */
    fun ordered(lang: Lang): List<TafsirSource> =
        ALL.filter { it.lang == lang } + ALL.filter { it.lang == Lang.AR && it.lang != lang } +
            ALL.filter { it.lang != lang && it.lang != Lang.AR }

    /** اسم قصير يظهر على زر المصدر */
    fun label(src: TafsirSource, s: Strings): String = when (src.id) {
        "ar.muyassar" -> s.tafsirMuyassar
        "ar.jalalayn" -> s.tafsirJalalayn
        "id.jalalayn" -> "Tafsir Jalalayn"
        else -> src.lang.native
    }

    fun kindLabel(src: TafsirSource, s: Strings) = if (src.kind == SourceKind.TAFSIR) s.tafsirTitle else s.translationOfMeanings
}

/**
 * نصوص المصحف والتفاسير المضمّنة (ملفات تنزيل كما هي، بصيغة "سورة|آية|نص").
 * تُقرأ عند أول طلب في الخلفية وتبقى في الذاكرة.
 */
object QuranData {
    /** يُضبط عند بدء التطبيق؛ والاختبارات تقرأ الملفات من القرص مباشرة */
    var open: (String) -> InputStream = { throw IllegalStateException("QuranData.init was not called") }

    private val cache = ConcurrentHashMap<String, Array<String>>()
    private val locks = ConcurrentHashMap<String, Any>()

    fun init(ctx: Context) {
        val assets = ctx.applicationContext.assets
        open = { path -> assets.open(path) }
    }

    fun cached(path: String): Array<String>? = cache[path]

    suspend fun load(path: String): Array<String> = cache[path] ?: withContext(Dispatchers.IO) {
        synchronized(locks.getOrPut(path) { Any() }) {
            cache[path] ?: open(path).use { parse(it) }.also { cache[path] = it }
        }
    }

    suspend fun uthmani() = load(UTHMANI)
    suspend fun clean() = load(CLEAN)
    suspend fun source(id: String) = load("tafsir/$id.txt")

    const val UTHMANI = "quran/uthmani.txt"
    const val CLEAN = "quran/clean.txt"

    fun parse(input: InputStream): Array<String> {
        val out = arrayOfNulls<String>(QuranMeta.AYAHS)
        input.bufferedReader(Charsets.UTF_8).useLines { lines ->
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

    fun ensure() {
        if (loaded) return
        loaded = true
        last = AppSettings.getInt("q_last", -1)
        textSize = AppSettings.getInt("q_size", 26)
        mode = AppSettings.getString("q_mode")
        source = AppSettings.getString("q_src")
        bookmarks.clear()
        bookmarks += AppSettings.getString("q_marks").split(',').mapNotNull { it.toIntOrNull() }.filter { it in 0 until QuranMeta.AYAHS }
    }

    fun modeFor(lang: Lang) = mode.ifEmpty { if (lang == Lang.AR) "mushaf" else "list" }
    fun sourceFor(lang: Lang) = Sources.byId(source) ?: Sources.defaultFor(lang)

    fun updateLast(i: Int) { if (i != last) { last = i; AppSettings.putInt("q_last", i) } }
    fun updateSize(v: Int) { textSize = v.coerceIn(18, 40); AppSettings.putInt("q_size", textSize) }
    fun updateMode(v: String) { mode = v; AppSettings.putString("q_mode", v) }
    fun updateSource(id: String) { source = id; AppSettings.putString("q_src", id) }

    fun isBookmarked(i: Int) = i in bookmarks
    fun toggleBookmark(i: Int) {
        if (!bookmarks.remove(i)) bookmarks.add(0, i)
        AppSettings.putString("q_marks", bookmarks.joinToString(","))
    }
}

/** البحث في الآيات: بالنص العربي (بلا تشكيل) وبترجمة لغة الواجهة، أو بالإحالة مثل 2:255 */
object QuranSearch {
    private val normalized = ConcurrentHashMap<String, Array<String>>()

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
