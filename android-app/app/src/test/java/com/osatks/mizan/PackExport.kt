package com.osatks.mizan

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.Named
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.Tx
import com.osatks.mizan.i18n.pack.LangPack
import com.osatks.mizan.i18n.pack.Template
import java.io.File
import java.lang.reflect.Method

/**
 * يبني قالب حزمة اللغة من النصوص الإنجليزية: كل نص كما هو، وكل دالة قالباً بمتغيراتها وصيغ الجمع.
 * هذا القالب هو ما يُترجم لإضافة لغة جديدة (lang/en.template.json في الموقع).
 */
object PackExport {
    /** أسماء معاملات الدوال من ملف Strings.kt (الانعكاس في JVM لا يحفظها) */
    val params: Map<String, List<Pair<String, String>>> by lazy {
        val re = Regex("""^\s*fun (\w+)\(([^)]*)\): String""")
        File("src/main/java/com/osatks/mizan/i18n/Strings.kt").readLines().mapNotNull { re.find(it) }.associate { m ->
            m.groupValues[1] to m.groupValues[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                .map { p -> p.substringBefore(':').trim() to p.substringAfter(':').trim() }
        }
    }

    /** دوال تُكتب قوالبها يدوياً هنا (تحتاج أسماء الأشهر أو مفاتيح أو أكثر من صيغة جمع) */
    private val MANUAL = setOf("hijriDate", "gregDate", "hijriMonthTitle", "monthTableTitle", "shareType", "inhNote", "livestockKind", "nameOf", "ymd")

    val LIVESTOCK = listOf("shah", "bintMakhad", "bintLabun", "hiqqah", "jadhaah", "tabi", "musinnah")
    val NOTES: List<Pair<String, InhNote>> = listOf(
        "MatSiblingsShare" to InhNote.MatSiblingsShare, "Umariyya" to InhNote.Umariyya, "GrandmothersShare" to InhNote.GrandmothersShare,
        "FullSistersGhayr" to InhNote.FullSistersGhayr, "Akdariyya" to InhNote.Akdariyya,
        "JaddRule" to InhNote.JaddRule(false), "JaddRuleOtherFard" to InhNote.JaddRule(true),
        "Muadda" to InhNote.Muadda, "NothingLeft" to InhNote.NothingLeft("fb"), "Mushtaraka" to InhNote.Mushtaraka,
        "Awl" to InhNote.Awl(701, 702), "Radd" to InhNote.Radd, "SpouseOnlyRadd" to InhNote.SpouseOnlyRadd, "NoHeirs" to InhNote.NoHeirs,
    )

    private fun getters(): List<Method> = Strings::class.java.methods
        .filter { it.parameterCount == 0 && it.name.startsWith("get") }
        .sortedBy { it.name }

    private fun key(m: Method) = m.name.removePrefix("get").replaceFirstChar { it.lowercase() }

    /** القيمة بصيغة JSON */
    private fun json(v: Any?): Any? = when (v) {
        is String -> v
        is Named -> linkedMapOf("name" to v.name, "desc" to v.desc)
        is Pair<*, *> -> listOf(v.first, v.second)
        is Map<*, *> -> v.entries.associate { (k, x) -> k.toString() to json(x) }
        is List<*> -> v.map { json(it) }
        else -> error("unexpected $v")
    }

    private fun call(name: String, args: List<Any>): String {
        val m = Strings::class.java.methods.first { it.name == name && it.parameterCount == args.size }
        return m.invoke(En, *args.toTypedArray()) as String
    }

    private fun arg(type: String, v: Long): Any = if (type == "Long") v else v.toInt()

    /** قالب دالة عامة: نصوص {name}، وأعداد قد تحتاج صيغة جمع */
    fun functionTemplate(name: String): String {
        val ps = params.getValue(name)
        val sentinels = ps.indices.map { 701L + it }
        fun args(override: Int = -1, value: Long = 0) = ps.mapIndexed { i, (n, t) ->
            when (t) { "String" -> "{$n}"; else -> arg(t, if (i == override) value else sentinels[i]) }
        }
        var tpl = call(name, args())
        ps.forEachIndexed { i, (n, t) -> if (t != "String") tpl = tpl.replace(sentinels[i].toString(), "{$n}") }
        val nums = ps.withIndex().filter { it.value.second != "String" }
        if (nums.size != 1) return tpl
        val (i, p) = nums[0]
        val n = p.first
        fun render(t: String, v: Long) = t.replace("{$n}", "$v")
        val one = call(name, args(i, 1))
        val zero = call(name, args(i, 0))
        if (one == render(tpl, 1) && zero == render(tpl, 0)) return tpl
        val oneForm = if (one != render(tpl, 1)) "one {${one.replace(Regex("(?<![\\d.,])1(?![\\d.,])"), "#")}} " else ""
        val body = "${oneForm}other {${tpl.replace("{$n}", "#")}}}"
        val plain = "{$n, plural, $body"
        val withZero = if (Template.format(plain, mapOf(n to 0L), "en") != zero) "{$n, plural, =0 {$zero} $body" else plain
        return withZero
    }

    fun strings(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for (m in getters()) out[key(m)] = json(m.invoke(En))
        for (name in params.keys.sorted()) if (name !in MANUAL) out[name] = functionTemplate(name)
        out["hijriDate"] = "{d} {month} {y} AH"
        out["gregDate"] = "{d} {month} {y}"
        out["hijriMonthTitle"] = "{month} {y} AH"
        out["monthTableTitle"] = "{month} {y} timetable"
        out["ymd"] = "{y, plural, one {# year} other {# years}} {m, plural, one {# month} other {# months}} {d, plural, one {# day} other {# days}}"
        out["shareType"] = ShareType.entries.associate { it.name to En.shareType(it, false) } + ("radd" to "{type} + radd")
        out["inhNote"] = NOTES.associate { (k, n) ->
            k to En.inhNote(n).replace(En.nameOf("fb"), "{winner}").replace("701", "{asl}").replace("702", "{awl}")
        }
        out["livestockKind"] = LIVESTOCK.associateWith { En.livestockKind(it) }
        return out.toSortedMap()
    }

    /** كل نصوص الأدلة الإنجليزية (مفتاح الترجمة) */
    fun evidenceKeys(): List<String> {
        Evidence.quran.size // يهيئ كائن الأدلة فتُسجَّل كل نصوصه
        return Tx.all.map { it.en }.distinct()
    }

    fun template(): Map<String, Any?> = linkedMapOf(
        "format" to LangPack.FORMAT.toLong(), "code" to "en", "native" to "English", "english" to "English",
        "rtl" to false, "decimal" to ".", "group" to ",", "version" to 1L, "currency" to "USD",
        "quran" to linkedMapOf("id" to "en.sahih", "author" to "Saheeh International", "file" to "en.sahih.txt", "sizeMb" to 1.0),
        "tafsirs" to emptyList<Any>(), "tafsirFallback" to emptyList<Any>(),
        "strings" to strings(),
        "evidence" to evidenceKeys().associateWith { it },
        // ترجمة الأذكار بصيغة assets/adhkar/en.json (العربية في adhkar.json هي الأصل)
        "adhkar" to Json.obj(File("src/main/assets/adhkar/en.json").readText()),
    )
}
