package com.osatks.mizan.i18n.pack

/**
 * حزمة لغة: ملف JSON واحد فيه كل نصوص الواجهة والأدلة، وما يلزم من ترجمة المعاني والتفاسير.
 * تُضمَّن في التطبيق (assets/lang) أو تُنزَّل من موقع ميزان، فتُضاف لغة جديدة دون تحديث التطبيق.
 * الصيغة موثقة في README (قسم «Language packs»)، والقالب الإنجليزي في site: lang/en.template.json.
 */
class LangPack(
    val code: String,
    val native: String,
    val english: String,
    val rtl: Boolean,
    val decimal: Char,
    val group: Char,
    val version: Int,
    /** العملة الافتراضية في حاسبة الزكاة (رمز ISO) */
    val currency: String?,
    val quran: PackQuran?,
    val tafsirs: List<PackTafsir>,
    /** لغات تُعرض تفاسيرها بعد تفاسير هذه اللغة (إن لم يتوفر تفسير بها) */
    val tafsirFallback: List<String>,
    val strings: Map<String, Any?>,
    /** نص الدليل بالإنجليزية ← ترجمته */
    val evidence: Map<String, String>,
    /** ترجمة الأذكار (بصيغة assets/adhkar/<code>.json)، إن وُجدت */
    val adhkar: Map<String, Any?>? = null,
) {
    companion object {
        /** أعلى إصدار من الصيغة يفهمه هذا التطبيق؛ الحزم الأحدث تُتجاهل حتى يُحدَّث */
        const val FORMAT = 1

        @Suppress("UNCHECKED_CAST")
        fun parse(text: String): LangPack {
            val o = Json.obj(text)
            val format = (o["format"] as? Number)?.toInt() ?: 1
            require(format <= FORMAT) { "pack format $format is newer than $FORMAT" }
            fun str(k: String) = o[k] as? String ?: throw IllegalArgumentException("pack: '$k' missing")
            fun ch(k: String, d: Char) = (o[k] as? String)?.firstOrNull() ?: d
            val code = str("code")
            require(Regex("[a-z]{2,3}(-[A-Za-z0-9]{2,8})*").matches(code)) { "pack: bad code $code" }
            val q = o["quran"] as? Map<String, Any?>
            return LangPack(
                code = code, native = str("native"), english = o["english"] as? String ?: code,
                rtl = o["rtl"] == true, decimal = ch("decimal", '.'), group = ch("group", ','),
                version = (o["version"] as? Number)?.toInt() ?: 1,
                currency = o["currency"] as? String,
                quran = q?.let { PackQuran(it["id"] as String, it["author"] as? String ?: "", it["file"] as? String ?: "${it["id"]}.txt", (it["sizeMb"] as? Number)?.toFloat() ?: 0f, it["translit"] as? String) },
                tafsirs = (o["tafsirs"] as? List<Map<String, Any?>>).orEmpty().map {
                    PackTafsir(
                        it["slug"] as String, it["name"] as? String ?: it["slug"] as String, it["nameEn"] as? String,
                        it["author"] as? String ?: "", it["authorEn"] as? String ?: it["author"] as? String ?: "",
                        (it["sizeMb"] as? Number)?.toFloat() ?: 0f, it["translit"] as? String,
                    )
                },
                tafsirFallback = (o["tafsirFallback"] as? List<String>).orEmpty(),
                strings = o["strings"] as? Map<String, Any?> ?: emptyMap(),
                evidence = (o["evidence"] as? Map<String, Any?>).orEmpty().mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap(),
                adhkar = o["adhkar"] as? Map<String, Any?>,
            )
        }
    }
}

/** ترجمة معاني القرآن المرافقة للحزمة: ملف "سورة|آية|نص" يُنزَّل بجانبها؛ translit: تحويل خطها عند القراءة */
class PackQuran(val id: String, val author: String, val file: String, val sizeMb: Float, val translit: String? = null)

/** تفسير من مجموعة tafsir_api بلغة الحزمة؛ translit: تحويل الخط عند التنزيل (مثل uz-latn) */
class PackTafsir(val slug: String, val name: String, val nameEn: String?, val author: String, val authorEn: String, val sizeMb: Float, val translit: String?)
