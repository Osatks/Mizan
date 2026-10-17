package com.osatks.mizan.i18n.pack

/**
 * قارئ JSON صغير لحزم اللغات (يعمل في اختبارات JVM بخلاف org.json في أندرويد).
 * الكائن Map<String, Any?> مرتب، والمصفوفة List<Any?>، والأرقام Double أو Long، و null.
 */
object Json {
    fun parse(text: String): Any? = Reader(text).run { val v = value(); ws(); if (i != s.length) fail("trailing data"); v }

    @Suppress("UNCHECKED_CAST")
    fun obj(text: String): Map<String, Any?> = parse(text) as? Map<String, Any?> ?: throw IllegalArgumentException("JSON object expected")

    private class Reader(val s: String) {
        var i = 0
        fun fail(m: String): Nothing = throw IllegalArgumentException("JSON: $m at $i")
        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun value(): Any? {
            ws()
            if (i >= s.length) fail("unexpected end")
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> word("true", true)
                'f' -> word("false", false)
                'n' -> word("null", null)
                else -> if (c == '-' || c.isDigit()) num() else fail("unexpected '$c'")
            }
        }
        fun word(w: String, v: Any?): Any? { if (!s.startsWith(w, i)) fail("bad literal"); i += w.length; return v }
        fun num(): Any {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(st, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: fail("bad number")
        }
        fun str(): String {
            i++
            val b = StringBuilder()
            while (true) {
                if (i >= s.length) fail("unterminated string")
                val c = s[i++]
                when (c) {
                    '"' -> return b.toString()
                    '\\' -> {
                        if (i >= s.length) fail("bad escape")
                        when (val e = s[i++]) {
                            'n' -> b.append('\n'); 't' -> b.append('\t'); 'r' -> b.append('\r')
                            'b' -> b.append('\b'); 'f' -> b.append('\u000C')
                            'u' -> { if (i + 4 > s.length) fail("bad escape"); b.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> b.append(e)
                        }
                    }
                    else -> b.append(c)
                }
            }
        }
        fun arr(): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            ws(); if (s.getOrNull(i) == ']') { i++; return out }
            while (true) {
                out += value(); ws()
                when (s.getOrNull(i++)) { ',' -> continue; ']' -> return out; else -> fail("',' or ']' expected") }
            }
        }
        fun obj(): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            ws(); if (s.getOrNull(i) == '}') { i++; return out }
            while (true) {
                ws(); if (s.getOrNull(i) != '"') fail("key expected")
                val k = str(); ws()
                if (s.getOrNull(i++) != ':') fail("':' expected")
                out[k] = value(); ws()
                when (s.getOrNull(i++)) { ',' -> continue; '}' -> return out; else -> fail("',' or '}' expected") }
            }
        }
    }

    /** كتابة منسقة (للقالب الإنجليزي الذي يُترجم منه) */
    fun write(v: Any?, indent: String = ""): String = buildString { put(this, v, indent) }

    private fun put(b: StringBuilder, v: Any?, ind: String) {
        val next = "$ind  "
        when (v) {
            null -> b.append("null")
            is String -> quote(b, v)
            is Boolean, is Number -> b.append(v.toString())
            is Map<*, *> -> {
                if (v.isEmpty()) { b.append("{}"); return }
                b.append("{\n")
                v.entries.forEachIndexed { n, (k, x) ->
                    b.append(next); quote(b, k.toString()); b.append(": "); put(b, x, next)
                    b.append(if (n < v.size - 1) ",\n" else "\n")
                }
                b.append(ind).append('}')
            }
            is List<*> -> {
                if (v.isEmpty()) { b.append("[]"); return }
                // قوائم النصوص القصيرة في سطر واحد
                if (v.all { it is String && it.length < 40 } && v.size <= 12) {
                    b.append('['); v.forEachIndexed { n, x -> if (n > 0) b.append(", "); quote(b, x as String) }; b.append(']'); return
                }
                b.append("[\n")
                v.forEachIndexed { n, x -> b.append(next); put(b, x, next); b.append(if (n < v.size - 1) ",\n" else "\n") }
                b.append(ind).append(']')
            }
            else -> quote(b, v.toString())
        }
    }

    private fun quote(b: StringBuilder, s: String) {
        b.append('"')
        for (c in s) when (c) {
            '"' -> b.append("\\\""); '\\' -> b.append("\\\\"); '\n' -> b.append("\\n"); '\t' -> b.append("\\t"); '\r' -> b.append("\\r")
            else -> if (c < ' ') b.append("\\u%04x".format(c.code)) else b.append(c)
        }
        b.append('"')
    }
}
