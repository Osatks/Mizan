package com.osatks.mizan.hadith

import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.locked
import kotlinx.datetime.LocalDate

/**
 * حديث بلغة واحدة كما في موسوعة الأحاديث النبوية (HadeethEnc.com) بلا تعديل:
 * العنوان (طرف الحديث)، والنص، والشرح، والفوائد، والدرجة، والتخريج.
 */
class HadithText(
    val title: String, val text: String, val expl: String,
    val benefits: List<String>, val grade: String, val takhrij: String,
)

/** حديث اليوم: الأصل العربي، وترجمته بلغة الواجهة (أو أقرب لغة لها ترجمته)، ورابطه في الموسوعة */
class Hadith(val id: String, val ar: HadithText, val tr: HadithText?, val trLang: String?) {
    /** رابط الحديث في الموسوعة بلغة الترجمة المعروضة */
    val link: String get() = "https://hadeethenc.com/${trLang ?: "ar"}/browse/hadith/$id"
}

/**
 * أحاديث اليوم: ٣٦٦ حديثاً من موسوعة الأحاديث النبوية (assets/hadith، يبنيها tools/make_hadith.py)،
 * حديث لكل يوم بترتيب ثابت، فيتفق التطبيق والأدوات على حديث اليوم.
 * اللغات التي لا ترجمة فيها لحديث تأخذ أقرب لغة: الملايوية الإندونيسية، والكازاخية والأوزبكية الروسية، وغيرها الإنجليزية.
 */
object Hadiths {
    /** يُضبط عند بدء التطبيق؛ والاختبارات تقرأ الملفات من القرص */
    var open: (String) -> ByteArray? = { Platform.asset(it) }

    const val SOURCE = "HadeethEnc.com"
    const val SOURCE_URL = "https://hadeethenc.com"

    private val lock = Lock()
    private var order: List<String>? = null
    private val files = SyncMap<String, Map<String, HadithText>>()

    private val FALLBACK = mapOf("ms" to "id", "kk" to "ru", "uz" to "ru")

    @Suppress("UNCHECKED_CAST")
    private fun file(code: String): Map<String, HadithText> = files.getOrPut(code) {
        val o = open("hadith/$code.json")?.let { Json.obj(it.decodeToString()) } ?: return@getOrPut emptyMap()
        if (code == "ar") lock.locked { order = (o["order"] as? List<Any?>).orEmpty().filterIsInstance<String>() }
        (o["items"] as? Map<String, Any?>).orEmpty().mapNotNull { (id, v) ->
            val m = v as? Map<String, Any?> ?: return@mapNotNull null
            fun s(k: String) = (m[k] as? String).orEmpty()
            id to HadithText(s("title"), s("text"), s("expl"), (m["benefits"] as? List<Any?>).orEmpty().filterIsInstance<String>(), s("grade"), s("takhrij"))
        }.toMap()
    }

    /** ترتيب الأحاديث (معرّفاتها) */
    fun ids(): List<String> {
        order?.let { return it }
        file("ar")
        return order.orEmpty()
    }

    /** موضع حديث اليوم في الترتيب: يتتابع يوماً بعد يوم فلا يتكرر حديث قبل أن تمر الأحاديث كلها */
    fun indexFor(day: LocalDate): Int {
        val n = ids().size
        return if (n == 0) 0 else day.toEpochDay().mod(n.toLong()).toInt()
    }

    /** حديث اليوم بلغة [code]؛ null إن لم تُقرأ البيانات */
    fun forDay(day: LocalDate, code: String): Hadith? = ids().getOrNull(indexFor(day))?.let { get(it, code) }

    /** الحديث في الموضع [index] من الترتيب (يدور) */
    fun at(index: Int, code: String): Hadith? {
        val all = ids()
        if (all.isEmpty()) return null
        return get(all[index.mod(all.size)], code)
    }

    fun get(id: String, code: String): Hadith? {
        val ar = file("ar")[id] ?: return null
        if (code == "ar") return Hadith(id, ar, null, null)
        val chain = listOfNotNull(code, FALLBACK[code], "en").distinct()
        for (c in chain) file(c)[id]?.let { return Hadith(id, ar, it, c) }
        return Hadith(id, ar, null, null)
    }
}
