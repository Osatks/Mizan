package com.osatks.mizan.i18n.pack

/**
 * تحويل الخط لنصوص الحزم (ترجمة المعاني والتفاسير المتاحة بخط واحد فقط).
 * uz-latn: الأوزبكية من الكيريلية إلى اللاتينية الرسمية (أبجدية 1995: oʻ gʻ والفاصلة العليا ʼ).
 */
object Translit {
    fun apply(scheme: String?, text: String): String = when (scheme) {
        "uz-latn" -> uzLatn(text)
        else -> text
    }

    private const val VOWELS = "аеёиоуэюяўАЕЁИОУЭЮЯЎ"
    private val UZ = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ё' to "yo", 'ж' to "j", 'з' to "z",
        'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
        'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "x", 'ч' to "ch", 'ш' to "sh",
        'щ' to "sh", 'ъ' to "ʼ", 'ы' to "i", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'ў' to "oʻ", 'қ' to "q", 'ғ' to "gʻ", 'ҳ' to "h",
    )

    fun uzLatn(text: String): String {
        val b = StringBuilder(text.length + text.length / 10)
        for (i in text.indices) {
            val c = text[i]
            val lower = c.lowercaseChar()
            val prev = text.getOrNull(i - 1)
            val wordStart = prev == null || !prev.isLetter()
            val out = when (lower) {
                // е بعد صائت أو في أول الكلمة: ye؛ وإلا e
                'е' -> if (wordStart || prev in VOWELS || prev == 'ъ' || prev == 'Ъ' || prev == 'ь') "ye" else "e"
                // ц بين صائتين: ts؛ وإلا s
                'ц' -> if (!wordStart && prev in VOWELS) "ts" else "s"
                else -> UZ[lower]
            }
            if (out == null) { b.append(c); continue }
            if (c.isUpperCase() && out.isNotEmpty()) {
                // كلمة كلها بالأحرف الكبيرة: SH؛ وإلا Sh
                val next = text.getOrNull(i + 1)
                val allCaps = (next != null && next.isLetter() && next.isUpperCase()) || (!wordStart && prev!!.isUpperCase())
                b.append(if (allCaps) out.uppercase() else out.replaceFirstChar { it.uppercaseChar() })
            } else b.append(out)
        }
        return b.toString()
    }
}
