package com.osatks.mizan.quran

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Translit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * تفسير يُنزَّل عند الطلب من مجموعة tafsir_api المفتوحة (عبر jsDelivr، ومصادرها quran.com وQUL).
 * names: اسم التفسير بلغات الواجهة (يُستعمل الإنجليزي إن لم يوجد)، و sizeMb حجم التنزيل التقريبي.
 */
data class RemoteTafsir(
    val slug: String, val lang: Lang, val author: String, val authorEn: String, val sizeMb: Float, val names: Map<Lang, String>,
    /** تحويل الخط عند التنزيل (تفسير متاح بخط غير خط الواجهة) */
    val translit: String? = null,
) {
    val id get() = "dl:$slug"
    fun name(ui: Lang): String = names[ui] ?: if (ui.rtl) names[Lang.AR] ?: names.getValue(Lang.EN) else names[Lang.EN] ?: names.values.first()
    /** اسم المؤلف بلغة الواجهة: بخط التفسير إن كانت الواجهة بلغته أو بخط عربي مثله، وإلا بالإنجليزية */
    fun author(ui: Lang): String = if (ui == lang || (ui.rtl && lang.rtl)) author else authorEn
}

/** حالة تنزيل جارٍ أو متعثر */
sealed interface DownloadState {
    data class Running(val done: Int) : DownloadState
    data object Failed : DownloadState
}

/**
 * نص تفسير آية: بعض التفاسير (كابن كثير) تشرح عدة آيات معاً، فيُذكر مداها from..to.
 */
class TafsirEntry(val text: String, val from: Int, val to: Int)

/** تفسير سورة كاملة بعد قراءته من الملف: لكل آية نصها وأول الآيات التي تشترك معها في الشرح وآخرها */
class SurahTafsir(private val text: Array<String>, private val from: IntArray, private val to: IntArray) {
    fun entry(ayah: Int) = TafsirEntry(text[ayah - 1], from[ayah - 1], to[ayah - 1])
}

/**
 * مكتبة التفاسير: الفهرس، والتنزيل (سورةً سورة، يُستأنف إن انقطع)، والحذف، وقراءة تفسير سورة عند الحاجة.
 * يُحفظ كل تفسير في مجلد باسمه داخل ملفات التطبيق: ملف مضغوط لكل سورة، والشرح المشترك بين آيات يُحفظ مرة واحدة.
 */
object TafsirLibrary {
    const val BASE = "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/"
    const val SURAHS = 114

    private val BUILT_IN = listOf(
        RemoteTafsir("ar-tafsir-ibn-kathir", Lang.AR, "الحافظ ابن كثير", "Ibn Kathir", 9.8f, mapOf(
            Lang.AR to "تفسير ابن كثير", Lang.EN to "Tafsir Ibn Kathir", Lang.FR to "Tafsir Ibn Kathir", Lang.TR to "İbn Kesîr Tefsiri",
            Lang.UR to "تفسیر ابن کثیر", Lang.ID to "Tafsir Ibnu Katsir", Lang.MS to "Tafsir Ibnu Kathir", Lang.FA to "تفسیر ابن کثیر",
            Lang.BN to "তাফসীর ইবনে কাসীর", Lang.RU to "Тафсир Ибн Касира", Lang.DE to "Tafsir Ibn Kathir")),
        RemoteTafsir("ar-tafsir-as-saadi", Lang.AR, "عبد الرحمن السعدي", "Abd al-Rahman al-Sa'di", 4.3f, mapOf(
            Lang.AR to "تفسير السعدي", Lang.EN to "Tafsir al-Sa'di", Lang.TR to "Sa'dî Tefsiri", Lang.UR to "تفسیر السعدی",
            Lang.ID to "Tafsir As-Sa'di", Lang.MS to "Tafsir As-Sa'di", Lang.FA to "تفسیر سعدی", Lang.BN to "তাফসীর আস-সা'দী", Lang.RU to "Тафсир ас-Саади")),
        RemoteTafsir("ar-tafsir-al-tabari", Lang.AR, "ابن جرير الطبري", "Ibn Jarir al-Tabari", 9.2f, mapOf(
            Lang.AR to "تفسير الطبري", Lang.EN to "Tafsir al-Tabari", Lang.TR to "Taberî Tefsiri", Lang.UR to "تفسیر طبری",
            Lang.ID to "Tafsir Ath-Thabari", Lang.MS to "Tafsir At-Tabari", Lang.FA to "تفسیر طبری", Lang.BN to "তাফসীর আত-তাবারী", Lang.RU to "Тафсир ат-Табари")),
        RemoteTafsir("ar-tafseer-al-qurtubi", Lang.AR, "أبو عبد الله القرطبي", "Al-Qurtubi", 11.6f, mapOf(
            Lang.AR to "تفسير القرطبي", Lang.EN to "Tafsir al-Qurtubi", Lang.TR to "Kurtubî Tefsiri", Lang.UR to "تفسیر قرطبی",
            Lang.ID to "Tafsir Al-Qurthubi", Lang.MS to "Tafsir Al-Qurtubi", Lang.FA to "تفسیر قرطبی", Lang.BN to "তাফসীর আল-কুরতুবী", Lang.RU to "Тафсир аль-Куртуби")),
        RemoteTafsir("ar-tafsir-al-baghawi", Lang.AR, "الحسين بن مسعود البغوي", "Al-Baghawi", 4.1f, mapOf(
            Lang.AR to "تفسير البغوي", Lang.EN to "Tafsir al-Baghawi", Lang.TR to "Begavî Tefsiri", Lang.UR to "تفسیر بغوی",
            Lang.ID to "Tafsir Al-Baghawi", Lang.MS to "Tafsir Al-Baghawi", Lang.FA to "تفسیر بغوی", Lang.BN to "তাফসীর আল-বাগাভী", Lang.RU to "Тафсир аль-Багави")),
        RemoteTafsir("ar-tafsir-al-wasit", Lang.AR, "محمد سيد طنطاوي", "Muhammad Sayyid Tantawi", 4.7f, mapOf(
            Lang.AR to "التفسير الوسيط", Lang.EN to "Al-Tafsir al-Wasit (Tantawi)", Lang.TR to "Vesît Tefsiri (Tantâvî)", Lang.UR to "التفسیر الوسیط",
            Lang.ID to "Tafsir Al-Wasith", Lang.MS to "Tafsir Al-Wasit", Lang.FA to "التفسیر الوسیط", Lang.BN to "আত-তাফসীর আল-ওয়াসীত", Lang.RU to "Тафсир аль-Васит")),
        RemoteTafsir("ar-tafsir-al-mukhtasar", Lang.AR, "مركز تفسير للدراسات القرآنية", "Tafsir Center for Quranic Studies", 0.6f, mapOf(
            Lang.AR to "المختصر في التفسير", Lang.EN to "Al-Mukhtasar (Arabic)", Lang.TR to "Muhtasar Tefsir (Arapça)", Lang.UR to "المختصر فی التفسیر",
            Lang.ID to "Al-Mukhtashar (Arab)", Lang.MS to "Al-Mukhtasar (Arab)", Lang.FA to "المختصر فی التفسیر", Lang.BN to "আল-মুখতাসার (আরবি)", Lang.RU to "Аль-Мухтасар (араб.)")),
        RemoteTafsir("en-tafisr-ibn-kathir", Lang.EN, "Ibn Kathir (abridged)", "Ibn Kathir (abridged)", 4.2f, mapOf(Lang.EN to "Tafsir Ibn Kathir (English)", Lang.AR to "تفسير ابن كثير (بالإنجليزية)")),
        RemoteTafsir("en-tafsir-al-mukhtasar", Lang.EN, "Tafsir Center for Quranic Studies", "Tafsir Center for Quranic Studies", 0.7f, mapOf(Lang.EN to "Al-Mukhtasar (English)", Lang.AR to "المختصر في التفسير (بالإنجليزية)")),
        RemoteTafsir("french-mokhtasar", Lang.FR, "Centre Tafsir", "Tafsir Center", 0.7f, mapOf(Lang.FR to "Al-Mukhtasar (abrégé du tafsir)", Lang.EN to "Al-Mukhtasar (French)", Lang.AR to "المختصر في التفسير (بالفرنسية)")),
        RemoteTafsir("turkish-mokhtasar", Lang.TR, "Tefsir Merkezi", "Tafsir Center", 0.7f, mapOf(Lang.TR to "Muhtasar Tefsir", Lang.EN to "Al-Mukhtasar (Turkish)", Lang.AR to "المختصر في التفسير (بالتركية)")),
        RemoteTafsir("ur-tafseer-ibn-e-kaseer", Lang.UR, "حافظ ابن کثیر", "Ibn Kathir", 5.6f, mapOf(Lang.UR to "تفسیر ابن کثیر (اردو)", Lang.EN to "Tafsir Ibn Kathir (Urdu)", Lang.AR to "تفسير ابن كثير (بالأردية)")),
        RemoteTafsir("indonesian-mokhtasar", Lang.ID, "Markaz Tafsir", "Tafsir Center", 0.7f, mapOf(Lang.ID to "Al-Mukhtashar (Tafsir Ringkas)", Lang.MS to "Al-Mukhtasar (Bahasa Indonesia)", Lang.EN to "Al-Mukhtasar (Indonesian)", Lang.AR to "المختصر في التفسير (بالإندونيسية)")),
        RemoteTafsir("id-tafsir-as-saadi", Lang.ID, "Abdurrahman As-Sa'di", "Abd al-Rahman al-Sa'di", 8.6f, mapOf(Lang.ID to "Tafsir As-Sa'di (Bahasa Indonesia)", Lang.MS to "Tafsir As-Sa'di (Bahasa Indonesia)", Lang.EN to "Tafsir al-Sa'di (Indonesian)", Lang.AR to "تفسير السعدي (بالإندونيسية)")),
        RemoteTafsir("persian-mokhtasar", Lang.FA, "مرکز تفسیر", "Tafsir Center", 0.7f, mapOf(Lang.FA to "تفسیر المختصر (فارسی)", Lang.EN to "Al-Mukhtasar (Persian)", Lang.AR to "المختصر في التفسير (بالفارسية)")),
        RemoteTafsir("bn-tafseer-ibn-e-kaseer", Lang.BN, "ইবনে কাসীর", "Ibn Kathir", 7.9f, mapOf(Lang.BN to "তাফসীর ইবনে কাসীর (বাংলা)", Lang.EN to "Tafsir Ibn Kathir (Bengali)", Lang.AR to "تفسير ابن كثير (بالبنغالية)")),
        RemoteTafsir("bengali-mokhtasar", Lang.BN, "তাফসীর সেন্টার", "Tafsir Center", 0.9f, mapOf(Lang.BN to "আল-মুখতাসার তাফসীর", Lang.EN to "Al-Mukhtasar (Bengali)", Lang.AR to "المختصر في التفسير (بالبنغالية)")),
        RemoteTafsir("ru-tafsir-ibne-kahtir", Lang.RU, "Ибн Касир", "Ibn Kathir", 4.1f, mapOf(Lang.RU to "Тафсир Ибн Касира", Lang.EN to "Tafsir Ibn Kathir (Russian)", Lang.AR to "تفسير ابن كثير (بالروسية)")),
        RemoteTafsir("ru-tafseer-al-saddi", Lang.RU, "Абдуррахман ас-Саади", "Abd al-Rahman al-Sa'di", 2.3f, mapOf(Lang.RU to "Тафсир ас-Саади", Lang.EN to "Tafsir al-Sa'di (Russian)", Lang.AR to "تفسير السعدي (بالروسية)")),
        RemoteTafsir("russian-mokhtasar", Lang.RU, "Центр Тафсир", "Tafsir Center", 0.8f, mapOf(Lang.RU to "Аль-Мухтасар (краткий тафсир)", Lang.EN to "Al-Mukhtasar (Russian)", Lang.AR to "المختصر في التفسير (بالروسية)")),
    )

    /** التفاسير المعروفة: المكتوبة هنا وما تضيفه حزم اللغات المثبتة */
    val CATALOG: List<RemoteTafsir>
        get() = BUILT_IN + Lang.packs.flatMap { l ->
            l.pack!!.tafsirs.filter { t -> BUILT_IN.none { it.slug == t.slug } }.map { t ->
                RemoteTafsir(t.slug, l, t.author, t.authorEn, t.sizeMb, mapOf(l to t.name, Lang.EN to (t.nameEn ?: t.name)), t.translit)
            }
        }

    fun bySlug(slug: String) = CATALOG.firstOrNull { it.slug == slug }

    private var root: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = HashMap<String, Job>()

    /** التنزيلات الجارية أو المتعثرة؛ تُرسم منها أشرطة التقدم */
    val states = mutableStateMapOf<String, DownloadState>()

    /** يزيد عند اكتمال تنزيل أو حذف، فتُحدَّث القوائم التي تقرؤه */
    var version by mutableIntStateOf(0)
        private set

    fun init(ctx: Context) {
        if (root == null) root = File(ctx.applicationContext.filesDir, "tafsir")
    }

    private fun dir(slug: String) = File(root ?: error("TafsirLibrary.init was not called"), slug)

    fun isDownloaded(slug: String) = root != null && File(dir(slug), "complete").exists()

    private var memo: Pair<Any, List<RemoteTafsir>>? = null

    /** التفاسير المنزَّلة؛ تُحسب من الملفات مرة واحدة لكل تغيير (version يُقرأ فتُحدَّث الواجهة) */
    fun downloaded(): List<RemoteTafsir> {
        val v = version to Lang.packs
        memo?.let { (at, list) -> if (at == v) return list }
        return CATALOG.filter { isDownloaded(it.slug) }.also { if (root != null) memo = v to it }
    }

    /** يبدأ التنزيل (أو يستأنفه من أول سورة لم تُنزَّل) */
    fun download(slug: String, onComplete: () -> Unit = {}) {
        if (jobs[slug]?.isActive == true) return
        val d = dir(slug).apply { mkdirs() }
        states[slug] = DownloadState.Running((1..SURAHS).count { File(d, "$it.gz").exists() })
        jobs[slug] = scope.launch {
            try {
                for (s in 1..SURAHS) {
                    ensureActive()
                    val f = File(d, "$s.gz")
                    if (!f.exists()) {
                        val tr = bySlug(slug)?.translit
                        val entries = retry { fetchSurah(slug, s) }.map { (a, t) -> a to Translit.apply(tr, t) }
                        val tmp = File(d, "$s.tmp")
                        GZIPOutputStream(tmp.outputStream()).bufferedWriter(Charsets.UTF_8).use { w ->
                            encodeSurah(entries).forEach { w.write(it); w.write('\n'.code) }
                        }
                        if (!tmp.renameTo(f)) error("rename")
                    }
                    states[slug] = DownloadState.Running(s)
                }
                File(d, "complete").writeText("1")
                states.remove(slug)
                version++
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                states[slug] = DownloadState.Failed
            } finally {
                jobs.remove(slug)
            }
        }
    }

    fun cancel(slug: String) {
        jobs.remove(slug)?.cancel()
        states.remove(slug)
        if (!isDownloaded(slug)) dir(slug).deleteRecursively()
    }

    fun delete(slug: String) {
        jobs.remove(slug)?.cancel()
        states.remove(slug)
        dir(slug).deleteRecursively()
        synchronized(cache) { cache.keys.removeAll { it.startsWith("$slug:") } }
        version++
    }

    private suspend fun <T> retry(times: Int = 3, block: () -> T): T {
        var last: Throwable? = null
        repeat(times) { attempt ->
            try { return block() } catch (e: Throwable) {
                if (e is CancellationException) throw e
                last = e; delay(1500L * (attempt + 1))
            }
        }
        throw last ?: IllegalStateException()
    }

    /** يقرأ ملف السورة من الشبكة: قائمة {surah, ayah, text}. يتولى النظام فك ضغط gzip */
    private fun fetchSurah(slug: String, surah: Int): List<Pair<Int, String>> {
        val conn = (URL("$BASE$slug/$surah.json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 60_000
        }
        try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val out = ArrayList<Pair<Int, String>>()
            JsonReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { r ->
                fun readItems() {
                    r.beginArray()
                    while (r.hasNext()) {
                        var ayah = 0; var text = ""
                        r.beginObject()
                        while (r.hasNext()) {
                            when (r.nextName()) {
                                "ayah" -> ayah = r.nextInt()
                                "text" -> text = if (r.peek() == JsonToken.NULL) { r.nextNull(); "" } else r.nextString()
                                else -> r.skipValue()
                            }
                        }
                        r.endObject()
                        if (ayah > 0) out += ayah to text
                    }
                    r.endArray()
                }
                if (r.peek() == JsonToken.BEGIN_OBJECT) {
                    r.beginObject()
                    while (r.hasNext()) if (r.nextName() == "ayahs") readItems() else r.skipValue()
                    r.endObject()
                } else readItems()
            }
            return out
        } finally {
            conn.disconnect()
        }
    }

    // ---------- الملفات ----------

    /**
     * سطر لكل آية: "رقم|النص" والأسطر الجديدة داخل النص تُحفظ بفاصل الفقرات U+2029،
     * وإن تكرر شرح الآية السابقة نفسه يُكتب "رقم|=أول آية في المجموعة" فلا يتكرر النص.
     */
    fun encodeSurah(entries: List<Pair<Int, String>>): List<String> {
        val out = ArrayList<String>(entries.size)
        var prev: String? = null
        var groupStart = 0
        for ((ayah, raw) in entries.sortedBy { it.first }) {
            val text = clean(raw)
            if (prev != null && text == prev && text.isNotEmpty()) out += "$ayah|=$groupStart"
            else { out += "$ayah|" + text.replace('\n', ' '); groupStart = ayah; prev = text }
        }
        return out
    }

    /** يقرأ ملف السورة المحفوظ إلى نصوص الآيات ومجموعاتها */
    fun decodeSurah(lines: Sequence<String>, count: Int): SurahTafsir {
        val text = Array(count) { "" }
        val from = IntArray(count) { it + 1 }
        for (line in lines) {
            val bar = line.indexOf('|')
            if (bar <= 0) continue
            val ayah = line.substring(0, bar).toIntOrNull() ?: continue
            if (ayah !in 1..count) continue
            val body = line.substring(bar + 1)
            if (body.startsWith("=")) {
                val start = body.substring(1).toIntOrNull() ?: continue
                if (start in 1..count) { text[ayah - 1] = text[start - 1]; from[ayah - 1] = start }
            } else text[ayah - 1] = body.replace(' ', '\n')
        }
        val to = IntArray(count) { it + 1 }
        for (i in count - 1 downTo 0) { val f = from[i]; if (f != i + 1) to[f - 1] = maxOf(to[f - 1], i + 1) }
        for (i in 0 until count) to[i] = to[from[i] - 1]
        return SurahTafsir(text, from, to)
    }

    /** تنظيف النص: حواشي المحققين [[...]] تُحذف، والأسطر الفارغة الزائدة تُختصر */
    fun clean(text: String): String = text
        .replace(Regex("\\[\\[.*?]]", RegexOption.DOT_MATCHES_ALL), "")
        .replace("\r", "")
        .replace(Regex("[ \t]+\n"), "\n")
        .replace(Regex("\n{3,}"), "\n\n")
        .replace(Regex(" {2,}"), " ")
        .trim()

    private val cache = object : LinkedHashMap<String, SurahTafsir>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SurahTafsir>?) = size > 6
    }

    fun cachedEntry(slug: String, surah: Int, ayah: Int): TafsirEntry? = synchronized(cache) { cache["$slug:$surah"] }?.entry(ayah)

    /** تفسير آية من تفسير منزَّل؛ تُقرأ سورتها من الملف عند أول طلب */
    suspend fun entry(slug: String, surah: Int, ayah: Int): TafsirEntry? = withContext(Dispatchers.IO) {
        val key = "$slug:$surah"
        val st = synchronized(cache) { cache[key] } ?: run {
            val f = File(dir(slug), "$surah.gz")
            if (!f.exists()) return@withContext null
            val parsed = GZIPInputStream(f.inputStream()).bufferedReader(Charsets.UTF_8).useLines {
                decodeSurah(it, QuranMeta.COUNTS[surah - 1])
            }
            synchronized(cache) { cache[key] = parsed }
            parsed
        }
        st.entry(ayah)
    }
}
