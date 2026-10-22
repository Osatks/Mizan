package com.osatks.mizan.i18n.pack

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.httpGet
import com.osatks.mizan.platform.path
import kotlinx.coroutines.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** حزمة معروضة في فهرس الموقع */
data class RemotePack(val code: String, val native: String, val english: String, val version: Int, val sizeMb: Float, val file: String)

/**
 * حزم اللغات: المضمّنة في التطبيق (assets/lang) والمنزَّلة من موقع ميزان (files/lang).
 * الفهرس lang/index.json في الموقع يعرض اللغات المتاحة وإصداراتها، فتُضاف لغة أو تُحدَّث دون تحديث التطبيق.
 * تُنزَّل الحزمة ومعها ترجمة المعاني (إن وُجدت)، ولا تُثبَّت إلا إذا اكتمل الاثنان.
 */
object LanguagePacks {
    const val BASE = "https://mizan-al-muslem.com/lang/"

    private var root: String? = null
    private var assets: ((String) -> ByteArray?)? = null
    private val bundled = HashMap<String, LangPack>()
    private val downloaded = HashMap<String, LangPack>()

    /** فهرس الموقع بعد آخر فحص (null: لم يُفحص بعد) */
    var remote by mutableStateOf<List<RemotePack>?>(null)
        private set
    var checking by mutableStateOf(false)
        private set
    var checkFailed by mutableStateOf(false)
        private set

    /** التنزيلات الجارية: النسبة 0..100، أو -1 إن فشل التنزيل */
    val progress = mutableStateMapOf<String, Int>()

    fun init() {
        if (root != null) return
        root = path(Platform.filesDir, "lang")
        assets = { Platform.asset(it) }
        Platform.assetList("lang").filter { it.endsWith(".json") }.forEach { name ->
            Platform.asset("lang/$name")?.let { bytes -> runCatching { LangPack.parse(bytes.decodeToString()) }.getOrNull()?.let { bundled[it.code] = it } }
        }
        loadDownloaded()
        publish()
    }

    /** للاختبارات: حزم من مجلد على القرص بدلاً من assets */
    fun initForTest(bundledDir: String, filesDir: String) {
        root = path(filesDir, "lang")
        val assetsRoot = bundledDir.substringBeforeLast('/')
        assets = { p -> Fs.read(path(assetsRoot, p)) }
        bundled.clear(); downloaded.clear()
        Fs.list(bundledDir).filter { it.endsWith(".json") }.forEach { n -> LangPack.parse(Fs.read(path(bundledDir, n))!!.decodeToString()).let { bundled[it.code] = it } }
        loadDownloaded()
        publish()
    }

    private fun loadDownloaded() {
        val dir = root ?: return
        Fs.list(dir).filter { it.endsWith(".json") }.forEach { n ->
            runCatching { LangPack.parse(Fs.read(path(dir, n))!!.decodeToString()) }.getOrNull()?.let { downloaded[it.code] = it }
        }
    }

    /** الحزمة الفعالة لكل لغة: الأحدث إصداراً بين المضمّنة والمنزَّلة */
    fun installed(): Map<String, LangPack> =
        (bundled.keys + downloaded.keys).associateWith { c ->
            listOfNotNull(bundled[c], downloaded[c]).maxBy { it.version }
        }

    fun isBundled(code: String) = code in bundled
    fun isDownloaded(code: String) = code in downloaded

    private fun publish() {
        val all = installed().values.toList()
        // نصوص ترجمة المعاني بخط آخر تُحوَّل عند قراءتها
        all.forEach { p -> p.quran?.let { q -> p.quranTranslit()?.let { QuranTranslit.map[q.id] = it } } }
        Lang.register(all)
        com.osatks.mizan.adhkar.Adhkar.clearTranslations()
        Lang.packs.map { it.code }.filter { it !in all.map { p -> p.code } }.forEach(Lang::unregister)
    }

    /** الملف المنزَّل لترجمة المعاني: files/lang/tafsir/<id>.txt (QuranData يبحث فيه قبل assets) */
    fun quranFile(id: String): String? = root?.let { path(it, "tafsir", "$id.txt") }

    // ---------- الفهرس والتنزيل ----------

    /** الحزم في الموقع غير المثبتة أو التي لها إصدار أحدث */
    fun offers(): List<RemotePack> {
        val inst = installed()
        return remote.orEmpty().filter { r -> Lang.BUILT_IN.none { it.code == r.code } && (inst[r.code]?.version ?: 0) < r.version }
    }

    suspend fun refresh(base: String = BASE) {
        if (checking) return
        checking = true; checkFailed = false
        try {
            val text = fetch(base + "index.json").decodeToString()
            remote = parseIndex(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            checkFailed = true
        } finally {
            checking = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseIndex(text: String): List<RemotePack> {
        val o = Json.obj(text)
        if (((o["format"] as? Number)?.toInt() ?: 1) > LangPack.FORMAT) return emptyList()
        return (o["packs"] as? List<Map<String, Any?>>).orEmpty().mapNotNull { p ->
            if (((p["format"] as? Number)?.toInt() ?: 1) > LangPack.FORMAT) return@mapNotNull null
            RemotePack(
                p["code"] as? String ?: return@mapNotNull null, p["native"] as? String ?: return@mapNotNull null,
                p["english"] as? String ?: "", (p["version"] as? Number)?.toInt() ?: 1,
                (p["sizeMb"] as? Number)?.toFloat() ?: 0f, p["file"] as? String ?: "${p["code"]}.json",
            )
        }
    }

    /** ينزّل الحزمة وترجمة معانيها ثم يثبتها؛ true عند النجاح */
    suspend fun download(r: RemotePack, base: String = BASE): Boolean {
        if (progress[r.code]?.let { it >= 0 } == true) return false
        progress[r.code] = 0
        return try {
            withContext(Dispatchers.IO) {
                val dir = root ?: error("LanguagePacks.init was not called")
                val json = fetch(base + r.file) { progress[r.code] = it / 10 }.decodeToString()
                val pack = LangPack.parse(json)
                require(pack.code == r.code) { "code mismatch" }
                pack.quran?.let { q ->
                    val have = Fs.exists(quranFile(q.id)!!) || assets?.invoke("tafsir/${q.id}.txt") != null
                    if (!have) Fs.write(quranFile(q.id)!!, fetch(base + q.file) { progress[r.code] = 10 + it * 9 / 10 })
                }
                Fs.write(path(dir, "${pack.code}.json"), json.encodeToByteArray())
                downloaded[pack.code] = pack
            }
            progress.remove(r.code)
            publish()
            true
        } catch (e: CancellationException) {
            progress.remove(r.code); throw e
        } catch (e: Throwable) {
            progress[r.code] = -1
            false
        }
    }

    private val scope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    /** يبدأ التنزيل في الخلفية (يستمر إن أُغلقت الورقة)، ثم onDone بالنتيجة */
    fun start(r: RemotePack, onDone: (Boolean) -> Unit = {}) {
        scope.launch { onDone(download(r)) }
    }

    /** يحذف حزمة منزَّلة (والمضمّنة تبقى بإصدارها الأصلي) */
    fun delete(code: String) {
        val p = downloaded.remove(code) ?: return
        root?.let { Fs.delete(path(it, "$code.json")) }
        p.quran?.let { q -> if (installed().values.none { it.quran?.id == q.id }) quranFile(q.id)?.let(Fs::delete) }
        publish()
        if (Lang.of(code) == null && AppSettings.lang == code) AppSettings.updateLang(Lang.SYSTEM)
    }

    /** يقرأ عنواناً كاملاً (http، أو file في الاختبارات) */
    private suspend fun fetch(url: String, onProgress: (Int) -> Unit = {}): ByteArray =
        if (url.startsWith("file:")) Fs.read(url.removePrefix("file:").removePrefix("//")) ?: error("missing $url")
        else httpGet(url, onProgress)
}

private fun LangPack.quranTranslit(): String? = quran?.translit

/** معرّف ترجمة المعاني ← تحويل الخط عند قراءتها */
object QuranTranslit {
    val map = SyncMap<String, String>()
}
