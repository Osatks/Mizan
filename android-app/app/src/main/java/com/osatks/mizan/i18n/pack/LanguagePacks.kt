package com.osatks.mizan.i18n.pack

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** حزمة معروضة في فهرس الموقع */
data class RemotePack(val code: String, val native: String, val english: String, val version: Int, val sizeMb: Float, val file: String)

/**
 * حزم اللغات: المضمّنة في التطبيق (assets/lang) والمنزَّلة من موقع ميزان (files/lang).
 * الفهرس lang/index.json في الموقع يعرض اللغات المتاحة وإصداراتها، فتُضاف لغة أو تُحدَّث دون تحديث التطبيق.
 * تُنزَّل الحزمة ومعها ترجمة المعاني (إن وُجدت)، ولا تُثبَّت إلا إذا اكتمل الاثنان.
 */
object LanguagePacks {
    const val BASE = "https://mizan-75v.pages.dev/lang/"

    private var root: File? = null
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

    fun init(ctx: Context) {
        if (root != null) return
        val app = ctx.applicationContext ?: ctx
        root = File(app.filesDir, "lang")
        val am = app.assets
        assets = { path -> runCatching { am.open(path).use { it.readBytes() } }.getOrNull() }
        am.list("lang").orEmpty().filter { it.endsWith(".json") }.forEach { name ->
            assets!!("lang/$name")?.let { bytes -> runCatching { LangPack.parse(bytes.decodeToString()) }.getOrNull()?.let { bundled[it.code] = it } }
        }
        loadDownloaded()
        publish()
    }

    /** للاختبارات: حزم من مجلد على القرص بدلاً من assets */
    fun initForTest(bundledDir: File, filesDir: File) {
        root = File(filesDir, "lang")
        assets = { path -> File(bundledDir.parentFile, path).takeIf { it.exists() }?.readBytes() }
        bundled.clear(); downloaded.clear()
        bundledDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty().forEach { f -> LangPack.parse(f.readText()).let { bundled[it.code] = it } }
        loadDownloaded()
        publish()
    }

    private fun loadDownloaded() {
        root?.listFiles { f -> f.name.endsWith(".json") }.orEmpty().forEach { f ->
            runCatching { LangPack.parse(f.readText()) }.getOrNull()?.let { downloaded[it.code] = it }
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
        Lang.packs.map { it.code }.filter { it !in all.map { p -> p.code } }.forEach(Lang::unregister)
    }

    /** الملف المنزَّل لترجمة المعاني: files/lang/tafsir/<id>.txt (QuranData يبحث فيه قبل assets) */
    fun quranFile(id: String): File? = root?.let { File(it, "tafsir/$id.txt") }

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
            val text = withContext(Dispatchers.IO) { fetch(base + "index.json").decodeToString() }
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
                    val have = quranFile(q.id)!!.exists() || assets?.invoke("tafsir/${q.id}.txt") != null
                    if (!have) {
                        val bytes = fetch(base + q.file) { progress[r.code] = 10 + it * 9 / 10 }
                        val f = quranFile(q.id)!!.apply { parentFile?.mkdirs() }
                        val tmp = File(f.path + ".tmp")
                        tmp.writeBytes(bytes)
                        if (!tmp.renameTo(f)) error("rename")
                    }
                }
                dir.mkdirs()
                val tmp = File(dir, "${pack.code}.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(File(dir, "${pack.code}.json"))) error("rename")
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
        root?.let { File(it, "$code.json").delete() }
        p.quran?.let { q -> if (installed().values.none { it.quran?.id == q.id }) quranFile(q.id)?.delete() }
        publish()
        if (Lang.of(code) == null && AppSettings.lang == code) AppSettings.updateLang(Lang.SYSTEM)
    }

    /** يقرأ عنواناً كاملاً (http أو file)؛ onProgress بالنسبة المئوية إن عُرف الحجم */
    private fun fetch(url: String, onProgress: (Int) -> Unit = {}): ByteArray {
        val conn = URL(url).openConnection().apply { connectTimeout = 15_000; readTimeout = 60_000 }
        try {
            if (conn is HttpURLConnection && conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            val out = ByteArrayOutputStream(if (total > 0) total.toInt() else 64 * 1024)
            conn.getInputStream().use { input ->
                val buf = ByteArray(32 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n); done += n
                    if (total > 0) onProgress((done * 100 / total).toInt().coerceIn(0, 100))
                }
            }
            return out.toByteArray()
        } finally {
            (conn as? HttpURLConnection)?.disconnect()
        }
    }
}

private fun LangPack.quranTranslit(): String? = quran?.translit

/** معرّف ترجمة المعاني ← تحويل الخط عند قراءتها */
object QuranTranslit {
    val map = java.util.concurrent.ConcurrentHashMap<String, String>()
}
