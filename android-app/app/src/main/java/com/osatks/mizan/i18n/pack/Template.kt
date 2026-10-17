package com.osatks.mizan.i18n.pack

/**
 * قوالب نصوص الحزم: {name} يُستبدل بالقيمة، و{n, plural, =0 {…} one {# …} few {…} many {…} other {# …}}
 * يختار الصيغة بحسب العدد (و# هو العدد نفسه)، على طريقة ICU MessageFormat المبسطة.
 */
object Template {
    fun format(tpl: String, vars: Map<String, Any>, lang: String): String {
        if ('{' !in tpl) return tpl
        val b = StringBuilder()
        var i = 0
        while (i < tpl.length) {
            val c = tpl[i]
            if (c != '{') { b.append(c); i++; continue }
            val end = close(tpl, i)
            if (end < 0) { b.append(tpl, i, tpl.length); break }
            b.append(expand(tpl.substring(i + 1, end), vars, lang))
            i = end + 1
        }
        return b.toString()
    }

    /** موضع القوس المغلق المقابل */
    private fun close(s: String, open: Int): Int {
        var depth = 0
        for (k in open until s.length) {
            if (s[k] == '{') depth++
            else if (s[k] == '}' && --depth == 0) return k
        }
        return -1
    }

    private fun expand(body: String, vars: Map<String, Any>, lang: String): String {
        val comma = body.indexOf(',')
        if (comma < 0) return vars[body.trim()]?.toString() ?: "{$body}"
        val name = body.substring(0, comma).trim()
        val rest = body.substring(comma + 1).trimStart()
        if (!rest.startsWith("plural")) return "{$body}"
        val n = (vars[name] as? Number)?.toLong() ?: return "{$body}"
        val forms = forms(rest.substring(rest.indexOf(',') + 1))
        val chosen = forms["=$n"] ?: forms[category(lang, n)] ?: forms["other"] ?: return "$n"
        return format(chosen.replace("#", vars[name].toString()), vars, lang)
    }

    /** "=0 {…} one {…} other {…}" ← خريطة الصيغ */
    private fun forms(s: String): Map<String, String> {
        val out = HashMap<String, String>()
        var i = 0
        while (i < s.length) {
            val open = s.indexOf('{', i)
            if (open < 0) break
            val key = s.substring(i, open).trim()
            val end = close(s, open)
            if (end < 0) break
            out[key] = s.substring(open + 1, end)
            i = end + 1
        }
        return out
    }

    /** فئة الجمع لعدد صحيح (قواعد CLDR مبسطة لأشهر الأنظمة) */
    fun category(lang: String, n: Long): String {
        val m10 = n % 10; val m100 = n % 100
        return when (lang) {
            "ru", "uk", "be", "sr", "hr", "bs" -> when {
                m10 == 1L && m100 != 11L -> "one"
                m10 in 2..4 && m100 !in 12..14 -> "few"
                else -> "many"
            }
            "pl" -> when {
                n == 1L -> "one"
                m10 in 2..4 && m100 !in 12..14 -> "few"
                else -> "many"
            }
            "cs", "sk" -> when (n) { 1L -> "one"; in 2..4 -> "few"; else -> "other" }
            "fr", "pt" -> if (n == 0L || n == 1L) "one" else "other"
            "ar" -> when {
                n == 0L -> "zero"; n == 1L -> "one"; n == 2L -> "two"
                m100 in 3..10 -> "few"; m100 in 11..99 -> "many"; else -> "other"
            }
            "ja", "zh", "ko", "id", "ms", "th", "vi" -> "other"
            else -> if (n == 1L) "one" else "other"
        }
    }
}
