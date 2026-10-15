package com.osatks.mizan.core

import java.text.Normalizer
import java.util.Locale

/**
 * تطبيع نصوص البحث ليجد المستخدم ما يريد مهما كتب: يحذف التشكيل والعلامات، ويوحّد صور الألف والياء
 * والتاء المربوطة والحروف الفارسية والأردية مع مقابلها العربي، ويحوّل الأرقام الشرقية إلى لاتينية.
 */
object Search {
    private val MARKS = Regex("\\p{Mn}+")
    private val SPACES = Regex("[\\s\\p{P}\\p{S}]+")

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (ch in decomposed.replace(MARKS, "")) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى', 'ی', 'ے', 'ئ' -> 'ي'
                    'ک' -> 'ك'
                    'ہ', 'ھ', 'ۀ' -> 'ه'
                    'ؤ' -> 'و'
                    'ı' -> 'i'
                    'ـ', '‌', '‍' -> continue
                    in '٠'..'٩' -> '0' + (ch - '٠')
                    in '۰'..'۹' -> '0' + (ch - '۰')
                    in '০'..'৯' -> '0' + (ch - '০')
                    else -> ch
                }
            )
        }
        return sb.toString().replace(SPACES, " ").trim()
    }

    /**
     * درجة التطابق: 0 = لا تطابق. كل كلمات البحث يجب أن توجد؛ وتعلو الدرجة إذا بدأ الاسم بها
     * أو بدأت بها كلمة فيه.
     */
    fun score(query: List<String>, name: String, extra: String = ""): Int {
        if (query.isEmpty()) return 0
        val n = normalize(name)
        val all = "$n ${normalize(extra)}"
        var total = 0
        for (q in query) {
            total += when {
                n.startsWith(q) -> 30
                n.contains(" $q") -> 20
                n.contains(q) -> 12
                all.contains(" $q") || all.startsWith(q) -> 8
                all.contains(q) -> 4
                else -> return 0
            }
        }
        return total
    }

    fun terms(q: String): List<String> = normalize(q).split(' ').filter { it.isNotEmpty() }
}
