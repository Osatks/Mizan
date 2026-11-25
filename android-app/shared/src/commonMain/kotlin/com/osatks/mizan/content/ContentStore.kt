package com.osatks.mizan.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.excludeFromBackup
import com.osatks.mizan.platform.httpGet
import com.osatks.mizan.platform.path
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.quran.Sources
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * المحتوى المنزَّل من موقع ميزان (/content/): ترجمات المعاني والتفاسير وأحاديث اليوم والأذكار بغير العربية والإنجليزية،
 * فلا يُضمَّن في التطبيق إلا القليل ويصغر حجمه. يُحفظ في ملفات التطبيق الدائمة (content/<النوع>/<الملف>، لا في الذاكرة
 * المؤقتة) بالمسار نفسه الذي له في assets، فمن يقرأ assets يجده بعدها ([read]). كل ملف يُتحقق من بصمته (SHA-256) قبل
 * حفظه، ولا يُنسخ احتياطياً (يُنزَّل من جديد عند الحاجة). ترجمة لغة الواجهة وأحاديثها وأذكارها تُنزَّل في الخلفية عند
 * أول تشغيل وعند تغيير اللغة ([ensureFor])، ومنها ما يحذفه المستخدم فلا يعود إلا إن طلبه.
 */
object ContentStore {
    const val BASE = "https://mizan-al-muslem.com/content/"
    private const val REMOVED = "content_removed"

    private var rootDir: String? = null
    private val root: String? get() = rootDir ?: runCatching { path(Platform.filesDir, "content") }.getOrNull()?.also { rootDir = it }

    /** للاختبارات: مجلد على القرص (null: بلا مجلد، فيُعدّ كل المحتوى موجوداً كما في المستودع) */
    fun initForTest(dir: String?) { rootDir = dir; memo = null }

    /** التنزيلات الجارية: النسبة 0..100، أو -1 إن تعثّر التنزيل (بلا إنترنت مثلاً) */
    val progress = mutableStateMapOf<String, Int>()

    /** يزيد بعد كل تنزيل أو حذف، فتُعاد قراءة النصوص والقوائم التي تقرؤه */
    var version by mutableIntStateOf(0)
        private set

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    fun file(item: ContentItem): String? = root?.let { path(it, item.path) }

    private var memo: Pair<Int, Set<String>>? = null

    /** مفاتيح ما نُزّل (tafsir/fr.hamidullah…)، من قائمة الملفات مرة لكل تغيير */
    fun downloadedKeys(): Set<String> {
        val v = version
        memo?.let { (at, keys) -> if (at == v) return keys }
        val r = root ?: return emptySet()
        val keys = ContentKind.entries.flatMap { k ->
            Fs.list(path(r, k.dir)).filter { it.endsWith("." + k.ext) }.map { "${k.dir}/${it.removeSuffix("." + k.ext)}" }
        }.toSet()
        memo = v to keys
        return keys
    }

    fun isDownloaded(item: ContentItem): Boolean = item.key in downloadedKeys()

    fun isBundled(item: ContentItem): Boolean = item.key in ContentCatalog.BUNDLED

    /** على الجهاز: مضمّن أو منزَّل */
    fun available(item: ContentItem): Boolean = isBundled(item) || isDownloaded(item)

    fun isRunning(item: ContentItem): Boolean = progress[item.key]?.let { it >= 0 } == true
    fun failed(item: ContentItem): Boolean = progress[item.key] == -1

    /**
     * ترجمة معانٍ أو تفسير مدمج ([com.osatks.mizan.quran.TafsirSource] بلا remote) على الجهاز: مضمّن، أو منزَّل من
     * الموقع، أو منزَّل مع حزمة لغته (files/lang/tafsir). بلا مجلد ملفات (الاختبارات) يُعدّ موجوداً.
     */
    fun hasTafsir(id: String): Boolean {
        if (root == null) return true
        if ("tafsir/$id" in ContentCatalog.BUNDLED) return true
        if ("tafsir/$id" in downloadedKeys()) return true
        return id in packQuranIds()
    }

    private var packMemo: Pair<Any, Set<String>>? = null

    /** ترجمات المعاني المنزَّلة مع حزم اللغات (files/lang/tafsir)، من قائمة الملفات مرة لكل تغيير في الحزم */
    private fun packQuranIds(): Set<String> {
        val packs = Lang.packs
        packMemo?.let { (at, ids) -> if (at === packs) return ids }
        val dir = LanguagePacks.quranFile("_")?.substringBeforeLast('/') ?: return emptySet()
        val ids = Fs.list(dir).filter { it.endsWith(".txt") }.map { it.removeSuffix(".txt") }.toSet()
        packMemo = packs to ids
        return ids
    }

    /** ملف منزَّل بمساره في assets (tafsir/fr.hamidullah.txt)؛ null إن لم يُنزَّل */
    fun read(assetPath: String): ByteArray? {
        val r = root ?: return null
        if (ContentCatalog.byPath(assetPath) == null) return null
        return Fs.read(path(r, assetPath))
    }

    /** حجم ما نُزّل بالبايت */
    fun downloadedBytes(): Long = ContentCatalog.ALL.filter { isDownloaded(it) }.sumOf { it.bytes }

    // ---------- التنزيل والحذف ----------

    /** ينزّل الملف ويتحقق من بصمته ثم يحفظه؛ true عند النجاح */
    suspend fun download(item: ContentItem, base: String = BASE): Boolean {
        if (isRunning(item)) return false
        progress[item.key] = 0
        return try {
            withContext(Dispatchers.IO) {
                val dest = file(item) ?: error("no files dir")
                val bytes = httpGet(base + item.path) { progress[item.key] = it.coerceIn(0, 99) }
                val hash = Sha256.hex(bytes)
                // البصمة المعروفة للتطبيق، أو بصمة إصدار أحدث يعلنها فهرس الموقع (الملف عُدّل بعد صدور هذه النسخة)
                if (hash != item.sha256 && hash != announcedHash(item, base)) error("sha256 mismatch ${item.key}")
                Fs.write(dest, bytes)
                root?.let(::excludeFromBackup)
            }
            progress.remove(item.key)
            unmarkRemoved(item)
            changed(item)
            true
        } catch (e: CancellationException) {
            progress.remove(item.key); throw e
        } catch (e: Throwable) {
            progress[item.key] = -1
            false
        }
    }

    private val jobs = HashMap<String, Job>()

    /** يبدأ التنزيل في الخلفية (يستمر إن أُغلقت الشاشة)؛ onDone بالنتيجة */
    fun start(item: ContentItem, onDone: (Boolean) -> Unit = {}) {
        if (isRunning(item) || jobs[item.key]?.isActive == true) return
        jobs[item.key] = scope.launch {
            try { onDone(download(item)) } finally { jobs.remove(item.key) }
        }
    }

    /** يلغي تنزيلاً جارياً (لا يُحفظ شيء منه) */
    fun cancel(item: ContentItem) {
        jobs.remove(item.key)?.cancel()
        progress.remove(item.key)
    }

    /** يحذف ملفاً منزَّلاً (المضمّن لا يُحذف)، ولا يُنزَّل تلقائياً بعدها */
    fun delete(item: ContentItem) {
        if (isBundled(item)) return
        file(item)?.let(Fs::delete)
        progress.remove(item.key)
        markRemoved(item)
        changed(item)
    }

    /** بعد تنزيل أو حذف: تُنسى النسخ المقروءة في الذاكرة فتُقرأ من جديد */
    private fun changed(item: ContentItem) {
        when (item.kind) {
            ContentKind.TAFSIR -> { QuranData.forget(item.path); QuranSearch.forget(item.id) }
            ContentKind.HADITH -> Hadiths.clearCache()
            ContentKind.ADHKAR -> Adhkar.clearTranslations()
        }
        memo = null
        version++
    }

    // فهرس الموقع لكل نوع (يُقرأ مرة في الجلسة، وعند اختلاف البصمة فقط)
    private val indexes = SyncMap<String, Map<String, String>>()

    private suspend fun announcedHash(item: ContentItem, base: String): String? {
        val idx = indexes[item.kind.dir] ?: runCatching {
            @Suppress("UNCHECKED_CAST")
            val items = Json.obj(httpGet(base + item.kind.dir + "/index.json").decodeToString())["items"] as? List<Map<String, Any?>>
            items.orEmpty().mapNotNull { m -> (m["id"] as? String)?.let { id -> (m["sha256"] as? String)?.let { id to it } } }.toMap()
        }.getOrNull()?.also { indexes[item.kind.dir] = it }
        return idx?.get(item.id)
    }

    // ما حذفه المستخدم بنفسه: لا يُنزَّل تلقائياً (ويعود إن نزّله بيده)
    private fun removed(): Set<String> =
        if (AppSettings.ready) AppSettings.getString(REMOVED).split(',').filter { it.isNotEmpty() }.toSet() else emptySet()

    private fun markRemoved(item: ContentItem) {
        if (AppSettings.ready) AppSettings.putString(REMOVED, (removed() + item.key).joinToString(","))
    }

    private fun unmarkRemoved(item: ContentItem) {
        if (AppSettings.ready && item.key in removed()) AppSettings.putString(REMOVED, (removed() - item.key).joinToString(","))
    }

    // ---------- التنزيل التلقائي للغة الواجهة ----------

    /**
     * ما تحتاجه لغة الواجهة ولم يُضمَّن: ترجمة المعاني (المعروضة تحت الآيات وفي البحث والأذكار والأدوات وركن الأطفال)،
     * وترجمة أحاديث اليوم (مع اللغة البديلة التي تكمل ما نقص منها)، وترجمة الأذكار ما لم تحملها حزمة اللغة
     */
    fun essentials(lang: Lang): List<ContentItem> {
        if (lang == Lang.AR) return emptyList()
        val tr = Sources.translationFor(lang)?.let { ContentCatalog.tafsir(it.id) }
        val hadith = listOfNotNull(lang.code, Hadiths.fallback(lang.code)).mapNotNull { ContentCatalog.hadith(it) }
        val adhkar = if (lang.pack?.adhkar != null) null else ContentCatalog.adhkar(lang.code)
        return listOfNotNull(tr) + hadith + listOfNotNull(adhkar)
    }

    /** ينزّل في الخلفية ما تحتاجه لغة الواجهة ولم يكن على الجهاز (أول تشغيل، وتغيير اللغة، وأول تشغيل بعد التحديث) */
    fun ensureFor(lang: Lang) {
        if (root == null) return
        val skip = removed()
        essentials(lang).filter { !available(it) && !isRunning(it) && it.key !in skip }.forEach { start(it) }
    }

    /** ملف لغة الواجهة الأساسي من نوع إن لم يكن على الجهاز (لبطاقة «تنزيل» في الشاشة التي تحتاجه) */
    fun missing(lang: Lang, kind: ContentKind): ContentItem? =
        if (root == null) null else essentials(lang).firstOrNull { it.kind == kind }?.takeIf { !available(it) }
}
