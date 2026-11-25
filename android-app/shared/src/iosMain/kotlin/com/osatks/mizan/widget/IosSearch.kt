package com.osatks.mizan.widget

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.quran.ayahRef
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.surahSearchExtra
import kotlinx.coroutines.runBlocking

/**
 * السور والآيات لكيانات App Intents (MizanSearch.swift): يجدها Siri و«ابحث أو اسأل» والاختصارات ويفتحها، بلغة التطبيق.
 * يستدعيه Swift على الخيط الرئيسي، ويعيد JSON بالمفاتيح التي يفكّها (SurahItem و AyahItem):
 *  سورة {n, title, subtitle, route}، وآية {i, title, text, meaning, route}.
 */
object IosSearch {
    /** السور كلها (اقتراحات Siri، وما يُحفظ في اختصار يُستعاد برقمه) */
    fun surahsJson(): String = runCatching {
        AppSettings.init()
        val s = Lang.current.strings
        Json.write((1..QuranMeta.SURAHS).map { surahMap(s, it) })
    }.getOrDefault("[]")

    /** السور التي يطابقها ما كُتب، بدرجات البحث داخل التطبيق نفسها */
    fun surahsMatchingJson(query: String): String = runCatching {
        AppSettings.init()
        val s = Lang.current.strings
        val terms = Search.terms(query)
        val hits = (1..QuranMeta.SURAHS)
            .map { n -> n to Search.score(terms, s.surahName(n), surahSearchExtra(s, n)) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        Json.write(hits.map { surahMap(s, it.first) })
    }.getOrDefault("[]")

    /** آيات بفهارسها (0…6235) مفصولة بفواصل: ما حفظه النظام من كيانات يُستعاد بها */
    fun ayahsJson(ids: String): String = runCatching {
        AppSettings.init()
        ayahs(ids.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 0 until QuranMeta.AYAHS })
    }.getOrDefault("[]")

    /** آيات يطابقها ما كُتب: مرجع («2:255») أو كلمات من نصها بلا تشكيل أو من معناها بلغة التطبيق، كما في بحث التطبيق */
    fun ayahsMatchingJson(query: String, limit: Int): String = runCatching {
        AppSettings.init()
        val lang = Lang.current
        val found = runCatching { runBlocking { QuranSearch.ayahs(query, lang, limit) } }.getOrDefault(emptyList())
        ayahs((listOfNotNull(QuranSearch.parseRef(query)) + found).distinct().take(limit.coerceAtLeast(1)))
    }.getOrDefault("[]")

    /** اقتراحات الآيات: آخر موضع قرأه المستخدم، ثم ما حفظه (الأحدث أولاً) */
    fun suggestedAyahsJson(): String = runCatching {
        AppSettings.init()
        QuranPrefs.ensure()
        val last = listOf(QuranPrefs.last).filter { it in 0 until QuranMeta.AYAHS }
        ayahs((last + QuranPrefs.bookmarks.toList().asReversed()).distinct().take(20))
    }.getOrDefault("[]")

    private fun surahMap(s: Strings, n: Int): Map<String, Any?> = linkedMapOf(
        "n" to n,
        "title" to s.surahName(n),
        "subtitle" to "${QuranMeta.NAMES_AR[n - 1]} · ${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
        "route" to surahRoute(n, 0, "widget"),
    )

    /** الآيات بنصها ومرجعها بلغة التطبيق، ومعناها إن كانت ترجمة اللغة على الجهاز (وإلا بلا معنى) */
    private fun ayahs(list: List<Int>): String {
        if (list.isEmpty()) return "[]"
        val lang = Lang.current
        val s = lang.strings
        val q = QuranData.cached(QuranData.UTHMANI) ?: runBlocking { QuranData.uthmani() }
        val tr = if (lang == Lang.AR) null else Sources.translationFor(lang)?.let { src ->
            runCatching { QuranData.cached("tafsir/${src.id}.txt") ?: runBlocking { QuranData.source(src.id) } }.getOrNull()
        }
        return Json.write(list.map { i ->
            val (surah, ayah) = QuranMeta.locate(i)
            linkedMapOf<String, Any?>(
                "i" to i,
                "title" to s.ayahRef(i),
                "text" to QuranData.withoutBismillah(q, i),
                "meaning" to tr?.getOrNull(i)?.takeIf { it.isNotBlank() },
                "route" to surahRoute(surah, ayah, "widget"),
            )
        })
    }
}
