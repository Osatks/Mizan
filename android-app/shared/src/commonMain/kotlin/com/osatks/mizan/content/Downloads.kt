package com.osatks.mizan.content

import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.path
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.TafsirLibrary
import com.osatks.mizan.ui.quran.badge
import com.osatks.mizan.ui.quran.isolate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * صف في شاشة التنزيلات: [key] يميّز نوعه (content:… المحتوى من موقع ميزان، tafsir:… مكتبة التفاسير، adhan:… الأذان،
 * madinah صفحات المصحف)، و[state] done أو running أو failed أو idle، و[bytes] حجمه على الجهاز، و[group] قسمه في الشاشة
 * (tr الترجمات والتفاسير، hadith، adhkar، adhan، mushaf).
 */
class DownloadEntry(
    val key: String, val badge: String, val name: String, val sub: String,
    val state: String, val progress: Float, val bytes: Long, val group: String,
)

/**
 * شاشة التنزيلات في الإعدادات (أندرويد DownloadsScreen، و iOS IosDownloadsScreen): ما نُزّل على الجهاز بحجمه مع حذفه،
 * وما يمكن تنزيله من موقع ميزان (ترجمات المعاني والتفاسير وأحاديث اليوم والأذكار بغير العربية والإنجليزية).
 * التنزيل والحذف هنا يمرّان بأصحابها: [ContentStore] و [TafsirLibrary] و [AdhanSounds] و [Madinah].
 */
object Downloads {
    const val MUSHAF = "madinah"
    private const val CONTENT = "content:"
    private const val TAFSIR = "tafsir:"
    private const val ADHAN = "adhan:"

    private fun mb(bytes: Long, s: Strings) = isolate(s.sizeMb(Fmt.num(bytes / 1_000_000.0, 1)))

    /** اسم لغة بلغتها (Français، Қазақша…) */
    private fun langName(code: String): String = Lang.of(code)?.native ?: Platform.displayLanguage(code, code).ifEmpty { code }

    /** لغة الواجهة أولاً في كل نوع، ثم بقية اللغات */
    private fun order(lang: Lang) = compareBy<ContentItem>({ it.kind.ordinal }, { if (it.lang == lang.code) 0 else 1 }, { it.lang })

    private fun state(item: ContentItem): String {
        val p = ContentStore.progress[item.key]
        return when {
            ContentStore.isDownloaded(item) -> "done"
            p != null && p >= 0 -> "running"
            p == -1 -> "failed"
            else -> "idle"
        }
    }

    private fun contentEntry(item: ContentItem, s: Strings, lang: Lang): DownloadEntry {
        val badgeText = Lang.of(item.lang)?.let(::badge) ?: item.lang.uppercase()
        val (name, sub) = when (item.kind) {
            ContentKind.TAFSIR -> {
                val src = Sources.ALL.firstOrNull { it.id == item.id }
                (src?.let { Sources.label(it, s, lang) } ?: langName(item.lang)) to
                    listOfNotNull(src?.let { Sources.kindLabel(it, s) }, src?.author?.let(::isolate), mb(item.bytes, s)).joinToString(" · ")
            }
            ContentKind.HADITH -> langName(item.lang) to "${s.dlHadith} · ${mb(item.bytes, s)}"
            ContentKind.ADHKAR -> langName(item.lang) to "${s.dlAdhkar} · ${mb(item.bytes, s)}"
        }
        val p = ContentStore.progress[item.key]
        val group = when (item.kind) { ContentKind.TAFSIR -> "tr"; ContentKind.HADITH -> "hadith"; ContentKind.ADHKAR -> "adhkar" }
        return DownloadEntry(CONTENT + item.key, badgeText, name, sub, state(item), (p ?: 0).coerceAtLeast(0) / 100f, item.bytes, group)
    }

    /** ما نُزّل على الجهاز: [pages] صفحات المصحف المنزَّلة و[mushafBytes] حجمها ([mushafBytes] يُحسب في الخلفية) */
    fun onDevice(s: Strings, lang: Lang, pages: Int, mushafBytes: Long): List<DownloadEntry> {
        val out = ArrayList<DownloadEntry>()
        ContentCatalog.ALL.filter { ContentStore.isDownloaded(it) }.sortedWith(order(lang)).forEach { out += contentEntry(it, s, lang) }
        TafsirLibrary.downloaded().forEach { r ->
            val bytes = (r.sizeMb * 1_000_000).toLong()
            out += DownloadEntry(TAFSIR + r.slug, badge(r.lang), r.name(lang), "${s.tafsirTitle} · ${isolate(r.author(lang))} · ${mb(bytes, s)}", "done", 1f, bytes, "tr")
        }
        Muezzins.ALL.filter { !it.bundled && AdhanSounds.available(it) }.forEach { m ->
            val bytes = m.sizeKb * 1000L
            out += DownloadEntry(ADHAN + m.id, "♪", s.muezzinNames[m.id] ?: m.id, "${s.dlAdhan} · ${mb(bytes, s)}", "done", 1f, bytes, "adhan")
        }
        if (pages > 0) out += DownloadEntry(MUSHAF, "۞", s.dlMushaf, "${s.pagesRead(pages, Madinah.PAGES)} · ${mb(mushafBytes, s)}", "done", 1f, mushafBytes, "mushaf")
        return out
    }

    /** ما يُنزَّل من موقع ميزان (المنزَّل منه بعلامة ✓) */
    fun available(s: Strings, lang: Lang): List<DownloadEntry> = ContentCatalog.ALL.sortedWith(order(lang)).map { contentEntry(it, s, lang) }

    /** حجم خطوط صفحات المصحف المنزَّلة (قراءة أحجام الملفات في الخلفية) */
    suspend fun mushafBytes(): Long = withContext(Dispatchers.IO) {
        val dir = Madinah.dir()
        Fs.list(dir).filter { it.endsWith(".ttf") }.sumOf { Fs.size(path(dir, it)) }
    }

    fun download(key: String) {
        if (key.startsWith(CONTENT)) ContentCatalog.byKey(key.removePrefix(CONTENT))?.let { ContentStore.start(it) }
    }

    fun cancel(key: String) {
        if (key.startsWith(CONTENT)) ContentCatalog.byKey(key.removePrefix(CONTENT))?.let(ContentStore::cancel)
    }

    /** عنوان قسم في الشاشة */
    fun groupTitle(group: String, s: Strings): String = when (group) {
        "tr" -> s.dlTranslations
        "hadith" -> s.dlHadith
        "adhkar" -> s.dlAdhkar
        "adhan" -> s.dlAdhan
        else -> s.dlMushaf
    }

    /** سؤال التأكيد قبل الحذف */
    fun deleteQuestion(e: DownloadEntry, s: Strings): String = if (e.key == MUSHAF) s.deletePagesQ else s.deleteTafsirQ(e.name)

    fun delete(key: String) {
        when {
            key == MUSHAF -> Madinah.deleteFonts()
            key.startsWith(CONTENT) -> ContentCatalog.byKey(key.removePrefix(CONTENT))?.let(ContentStore::delete)
            key.startsWith(TAFSIR) -> TafsirLibrary.delete(key.removePrefix(TAFSIR))
            key.startsWith(ADHAN) -> Muezzins.of(key.removePrefix(ADHAN))?.let(AdhanSounds::delete)
        }
    }
}
