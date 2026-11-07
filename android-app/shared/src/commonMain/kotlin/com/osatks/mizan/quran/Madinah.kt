package com.osatks.mizan.quran

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.httpGet
import com.osatks.mizan.platform.locked
import com.osatks.mizan.platform.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** سطر من صفحة مصحف المدينة كما في الطبعة */
sealed class MadinahLine {
    class Header(val surah: Int) : MadinahLine()
    object Basmala : MadinahLine()
    /** glyphs رموز الكلمات بخط الصفحة، و ayahs فهرس آية كل كلمة، و ends مواضع علامات نهاية الآيات */
    class Ayah(val centered: Boolean, val glyphs: List<String>, val ayahs: IntArray, val ends: Set<Int>) : MadinahLine()
}

/**
 * مصحف المدينة النبوية (طبعة مجمع الملك فهد ١٤٢١هـ، خمسة عشر سطراً): أسطر كل صفحة وكلماتها كما في المطبوع
 * (assets/madinah، يبنيها tools/make_madinah.py)، وتُرسم بخط الصفحة نفسها من خطوط المجمع (QPC V2)، فتطابق الصفحة
 * المطبوعة حرفاً بحرف. خط كل صفحة (نحو ٣٠٠ ك.ب) يُنزَّل عند فتحها وما حولها ويُحفظ في الجهاز، ويمكن تنزيل المصحف كله.
 * الخطوط من المجمع بلا تعديل (يسمح بنشرها دون تعديل أو بيع).
 */
object Madinah {
    const val PAGES = 604
    private const val FONT_URL = "https://static-cdn.tarteel.ai/qul/fonts/quran_fonts/v2/ttf/p%d.ttf"

    /** إطار ترويسة السورة واسمها (خط المجمع)، والبسملة بخط حفص العثماني من المجمع */
    const val HEADER_FONT = "fonts/qcf_surah_header.ttf"
    const val BASMALA_FONT = "fonts/uthmanic_hafs.ttf"
    const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"

    /** يُضبط عند بدء التطبيق؛ والاختبارات تقرأ الملفات من القرص */
    var open: (String) -> ByteArray? = { Platform.asset(it) }

    /** يزيد كلما وصل خط صفحة، فتُعاد الصفحات التي كانت تنتظره */
    var fontsVersion by mutableIntStateOf(0)
        private set

    private val pages = SyncMap<Int, List<MadinahLine?>>()
    private val lock = Lock()
    private val running = HashMap<Int, Deferred<Boolean>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val net = Semaphore(4)

    fun dir() = path(Platform.filesDir, "madinah")
    fun fontPath(page: Int) = path(dir(), "p$page.ttf")
    fun hasFont(page: Int) = page in 1..PAGES && Fs.exists(fontPath(page))

    /** رمز إطار ترويسة السورة واسمها في خط الترويسات (فارغ إن تعذّر) */
    fun headerCode(surah: Int): String = headerCodes().getOrNull(surah - 1)?.toChar()?.toString() ?: ""

    private var allCodes: IntArray? = null
    private fun headerCodes(): IntArray = allCodes ?: run {
        // الرموز من بيانات الخط (QUL) بترتيب السور: assets/madinah/headers.json
        val txt = open("madinah/headers.json")?.decodeToString()
        val list = (txt?.let { Json.parse(it) } as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
        (list?.takeIf { it.size == 114 }?.toIntArray() ?: IntArray(0)).also { allCodes = it }
    }

    /** أسطر الصفحة الخمسة عشر (null للسطر الفارغ) */
    @Suppress("UNCHECKED_CAST")
    fun lines(page: Int): List<MadinahLine?> = pages.getOrPut(page) {
        val o = open("madinah/p$page.json")?.let { Json.obj(it.decodeToString()) } ?: return@getOrPut emptyList()
        (o["l"] as? List<Any?>).orEmpty().map { l ->
            val a = l as? List<Any?> ?: return@map null
            when (a.firstOrNull()) {
                "s" -> MadinahLine.Header((a[1] as Number).toInt())
                "b" -> MadinahLine.Basmala
                "a" -> MadinahLine.Ayah(
                    (a[1] as Number).toInt() == 1,
                    (a[2] as List<Any?>).map { it as String },
                    (a[3] as List<Any?>).map { (it as Number).toInt() }.toIntArray(),
                    (a[4] as List<Any?>).map { (it as Number).toInt() }.toSet(),
                )
                else -> null
            }
        }
    }

    /** ينزّل خط الصفحة إن لم يكن في الجهاز؛ true إن صار متاحاً */
    suspend fun ensureFont(page: Int): Boolean {
        if (page !in 1..PAGES) return false
        if (hasFont(page)) return true
        val job = lock.locked {
            running.getOrPut(page) {
                scope.async {
                    try {
                        net.withPermit {
                            if (hasFont(page)) return@withPermit true
                            val bytes = httpGet(FONT_URL.replace("%d", page.toString()))
                            // خط صالح: ملف TrueType بحجم معقول
                            if (bytes.size < 10_000) return@withPermit false
                            Fs.mkdirs(dir())
                            Fs.write(fontPath(page), bytes)
                            true
                        }
                    } catch (e: Throwable) {
                        false
                    } finally {
                        lock.locked { running.remove(page) }
                    }
                }
            }
        }
        val ok = job.await()
        if (ok) fontsVersion++
        return ok
    }

    /** يطلب خطوط صفحات قريبة دون انتظار */
    fun prefetch(vararg pagesNear: Int) {
        for (p in pagesNear) if (p in 1..PAGES && !hasFont(p)) scope.launch { ensureFont(p) }
    }

    // ---------- تنزيل المصحف كاملاً ----------

    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    private var all: Job? = null

    fun downloadedCount(): Int = (1..PAGES).count { hasFont(it) }

    fun downloadAll() {
        if (downloading) return
        downloading = true
        all = scope.launch {
            try {
                var done = downloadedCount()
                progress = done / PAGES.toFloat()
                for (p in 1..PAGES) {
                    if (!isActive) break
                    if (hasFont(p)) continue
                    ensureFont(p)
                    done++
                    progress = done / PAGES.toFloat()
                }
            } finally {
                downloading = false
            }
        }
    }

    fun cancelDownload() { all?.cancel(); downloading = false }

    /** حذف الخطوط المنزَّلة (من الإعدادات) */
    fun deleteFonts() { cancelDownload(); Fs.deleteDir(dir()); fontsVersion++ }
}
