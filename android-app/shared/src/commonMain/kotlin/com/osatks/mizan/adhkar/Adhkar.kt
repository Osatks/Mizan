package com.osatks.mizan.adhkar

import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.SyncMap
import com.osatks.mizan.platform.locked

/**
 * ذكر واحد: نصه العربي، وعدد تكراره، ودليله (مصادره ودرجته وراويه وفضله).
 * src: (رمز الكتاب، رقم الحديث) مثل ("bukhari", "6306")، أو ("quran", "2:255").
 * ayah: مواضع الآيات إن كان الذكر قراءة قرآن، فتُعرض ترجمة معانيها بلغة الواجهة.
 */
class Dhikr(
    val id: String, val ar: String, val count: Int,
    val ayah: List<Triple<Int, Int, Int>>, val src: List<Pair<String, String>>,
    val grade: String, val by: String,
    val narr: String?, val fadl: String?, val note: String?,
)

/** مجموعة أذكار تحت عنوان فرعي (باب من حصن المسلم)؛ key: "<القسم>/<الترتيب>" لترجمة العنوان */
class DhikrGroup(val key: String, val titleAr: String?, val items: List<Dhikr>)

/** قسم من الأذكار (الصباح، المساء، النوم…) */
class AdhkarSection(val id: String, val icon: String, val titleAr: String, val descAr: String, val groups: List<DhikrGroup>) {
    val items: List<Dhikr> get() = groups.flatMap { it.items }
}

/** ترجمة ذكر: النص والراوي والفضل والملاحظة */
class DhikrTr(val t: String?, val narr: String?, val fadl: String?, val note: String?)

/**
 * ترجمة الأذكار بلغة: عناوين الأقسام والمجموعات، وأسماء الكتب ودرجات الأحاديث، وترجمة كل ذكر.
 * تأتي من assets/adhkar/<code>.json أو من حقل "adhkar" في حزمة اللغة؛ وما نقص يؤخذ من الإنجليزية.
 */
class AdhkarTr(
    val sections: Map<String, Pair<String, String>>,
    val groups: Map<String, String>,
    val books: Map<String, String>,
    val grades: Map<String, String>,
    val by: Map<String, String>,
    val items: Map<String, DhikrTr>,
) {
    /** يكمل الناقص من ترجمة أخرى (الإنجليزية) */
    fun orElse(o: AdhkarTr) = AdhkarTr(
        o.sections + sections, o.groups + groups, o.books + books, o.grades + grades, o.by + by,
        (o.items.keys + items.keys).associateWith { k ->
            val a = items[k]; val b = o.items[k]
            DhikrTr(a?.t ?: b?.t, a?.narr ?: b?.narr, a?.fadl ?: b?.fadl, a?.note ?: b?.note)
        },
    )

    companion object {
        val EMPTY = AdhkarTr(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

        @Suppress("UNCHECKED_CAST")
        fun from(o: Map<String, Any?>): AdhkarTr {
            fun strMap(k: String) = (o[k] as? Map<String, Any?>).orEmpty().mapNotNull { (a, b) -> (b as? String)?.takeIf { it.isNotBlank() }?.let { a to it } }.toMap()
            fun s(m: Map<String, Any?>?, k: String) = (m?.get(k) as? String)?.takeIf { it.isNotBlank() }
            return AdhkarTr(
                sections = (o["sections"] as? Map<String, Any?>).orEmpty().mapNotNull { (k, v) ->
                    val m = v as? Map<String, Any?>
                    s(m, "title")?.let { k to (it to (s(m, "desc") ?: "")) }
                }.toMap(),
                groups = strMap("groups"), books = strMap("books"), grades = strMap("grades"), by = strMap("by"),
                items = (o["items"] as? Map<String, Any?>).orEmpty().mapValues { (_, v) ->
                    val m = v as? Map<String, Any?>
                    DhikrTr(s(m, "t"), s(m, "narr"), s(m, "fadl"), s(m, "note"))
                },
            )
        }
    }
}

/** الأذكار: تُقرأ من assets/adhkar عند أول طلب وتبقى في الذاكرة */
object Adhkar {
    /** يُضبط عند بدء التطبيق؛ والاختبارات تقرأ الملفات من القرص */
    var open: (String) -> ByteArray? = { Platform.asset(it) }

    private var cached: List<AdhkarSection>? = null
    private val lock = Lock()
    private val trs = SyncMap<String, AdhkarTr>()

    private fun read(path: String): Map<String, Any?>? = open(path)?.let { Json.obj(it.decodeToString()) }

    fun sections(): List<AdhkarSection> = cached ?: lock.locked {
        cached ?: parse(read("adhkar/adhkar.json") ?: emptyMap()).also { cached = it }
    }

    /** الأقسام إن قُرئت من قبل (فلا يظهر مؤشر التحميل عند العودة) */
    fun loaded(): List<AdhkarSection>? = cached

    fun section(id: String) = sections().firstOrNull { it.id == id }

    fun byId(id: String) = sections().asSequence().flatMap { it.items }.firstOrNull { it.id == id }

    /** ترجمة لغة الواجهة مكمَّلة بالإنجليزية (والعربية تأخذ عناوينها وأسماء الكتب من ar.json) */
    fun tr(lang: Lang): AdhkarTr = trs.getOrPut(lang.code) {
        val en = read("adhkar/en.json")?.let(AdhkarTr::from) ?: AdhkarTr.EMPTY
        if (lang == Lang.EN) return@getOrPut en
        @Suppress("UNCHECKED_CAST")
        val own = (lang.pack?.adhkar ?: read("adhkar/${lang.code}.json"))?.let(AdhkarTr::from) ?: AdhkarTr.EMPTY
        own.orElse(en)
    }

    /** بعد تثبيت حزمة أو تحديثها */
    fun clearTranslations() = trs.clear()

    @Suppress("UNCHECKED_CAST")
    fun parse(o: Map<String, Any?>): List<AdhkarSection> {
        val items = (o["items"] as? Map<String, Any?>).orEmpty()
        fun dhikr(id: String): Dhikr? {
            val m = items[id] as? Map<String, Any?> ?: return null
            fun s(k: String) = (m[k] as? String)?.takeIf { it.isNotBlank() }
            return Dhikr(
                id = id, ar = s("ar") ?: return null, count = (m["count"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
                ayah = (m["ayah"] as? List<Any?>).orEmpty().mapNotNull { a ->
                    (a as? List<Any?>)?.mapNotNull { (it as? Number)?.toInt() }?.takeIf { it.size == 3 }?.let { Triple(it[0], it[1], it[2]) }
                },
                src = (m["src"] as? List<Any?>).orEmpty().mapNotNull { r ->
                    (r as? Map<String, Any?>)?.let { (it["b"] as? String ?: return@mapNotNull null) to (it["n"]?.toString() ?: "") }
                },
                grade = s("grade") ?: "", by = s("by") ?: "",
                narr = s("narr"), fadl = s("fadl"), note = s("note"),
            )
        }
        return (o["sections"] as? List<Any?>).orEmpty().mapNotNull { sv ->
            val sm = sv as? Map<String, Any?> ?: return@mapNotNull null
            val id = sm["id"] as? String ?: return@mapNotNull null
            val groups = (sm["groups"] as? List<Any?>).orEmpty().mapIndexedNotNull { gi, gv ->
                val gm = gv as? Map<String, Any?> ?: return@mapIndexedNotNull null
                DhikrGroup("$id/$gi", (gm["title"] as? String)?.takeIf { it.isNotBlank() },
                    (gm["items"] as? List<Any?>).orEmpty().mapNotNull { (it as? String)?.let(::dhikr) })
            }.filter { it.items.isNotEmpty() }
            AdhkarSection(id, sm["icon"] as? String ?: "star", sm["title"] as? String ?: id, sm["desc"] as? String ?: "", groups)
        }
    }
}
