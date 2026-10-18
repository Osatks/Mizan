package com.osatks.mizan

import com.osatks.mizan.core.ShareType
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.Tx
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.pack.LangPack
import com.osatks.mizan.i18n.pack.PackStrings
import com.osatks.mizan.i18n.pack.Template
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * حزم اللغات: القالب الإنجليزي يعيد النصوص الإنجليزية نفسها حرفاً بحرف، وكل حزمة مضمّنة كاملة وسليمة.
 * MIZAN_EXPORT=<مجلد> يكتب القالب en.template.json هناك (لموقع ميزان).
 */
class PackTest {
    private val enJson by lazy { Json.write(PackExport.template()) }
    private val enPack by lazy { LangPack.parse(enJson) }

    @Test
    fun templateEngine() {
        val t = "{n, plural, =0 {none} one {# day} few {# дня} many {# дней} other {# days}}"
        assertEquals("none", Template.format(t, mapOf("n" to 0), "en"))
        assertEquals("1 day", Template.format(t, mapOf("n" to 1), "en"))
        assertEquals("5 days", Template.format(t, mapOf("n" to 5), "en"))
        assertEquals("3 дня", Template.format(t, mapOf("n" to 3), "ru"))
        assertEquals("11 дней", Template.format(t, mapOf("n" to 11), "ru"))
        assertEquals("21 day", Template.format(t, mapOf("n" to 21), "ru"))
        assertEquals("Hi Ali, {missing}", Template.format("Hi {name}, {missing}", mapOf("name" to "Ali"), "en"))
        assertEquals(mapOf("a" to listOf(1L, 2.5, "x\n\"y\"", null, true)), Json.parse(Json.write(mapOf("a" to listOf(1L, 2.5, "x\n\"y\"", null, true)))))
    }

    /** كل عضو في Strings يُقرأ من القالب الإنجليزي فيطابق En تماماً */
    @Test
    fun englishTemplateRoundTrips() {
        val p = PackStrings(enPack)
        for (m in Strings::class.java.methods) {
            if (m.parameterCount == 0) {
                assertEquals(m.name, m.invoke(En), m.invoke(p)); continue
            }
            val types = m.parameterTypes.map { it.simpleName }
            val samples: List<List<Any>> = when {
                m.name == "shareType" -> ShareType.entries.flatMap { listOf(listOf(it, false), listOf(it, true)) }
                m.name == "inhNote" -> PackExport.NOTES.map { listOf(it.second) } + listOf(listOf(com.osatks.mizan.core.InhNote.NothingLeft("gf")))
                m.name == "livestockKind" -> PackExport.LIVESTOCK.map { listOf(it) }
                m.name == "nameOf" -> continue
                m.name.endsWith("Date") || m.name.endsWith("Title") && types.size == 2 ->
                    (1..12).map { mo -> if (types.size == 3) listOf(5, mo, 1447) else listOf(mo, 1447) }
                else -> listOf(0L, 1L, 2L, 3L, 5L, 11L, 21L, 100L).map { v ->
                    types.mapIndexed { i, t -> when (t) { "String" -> "s$i"; "int" -> v.toInt() + i; "long" -> v + i; else -> error(t) } }
                }
            }
            for (a in samples) assertEquals("${m.name}$a", m.invoke(En, *a.toTypedArray()), m.invoke(p, *a.toTypedArray()))
        }
        System.getenv("MIZAN_EXPORT")?.let { File(it, "en.template.json").writeText(enJson + "\n") }
    }

    /** الحزم المضمّنة: كل مفتاح معروف، والقوائم بأطوالها، والمتغيرات صحيحة، وكل الأدلة مترجمة */
    @Test
    fun bundledPacksAreComplete() {
        val en = enPack.strings
        val evidence = PackExport.evidenceKeys().toSet()
        val dir = File("src/commonMain/assets/lang")
        val files = dir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
        for (f in files) {
            val p = LangPack.parse(f.readText())
            val where = p.code
            assertEquals("$where file name", "${p.code}.json", f.name)
            assertTrue("$where: unknown keys ${p.strings.keys - en.keys}", (p.strings.keys - en.keys).isEmpty())
            assertTrue("$where: missing keys ${en.keys - p.strings.keys}", (en.keys - p.strings.keys).isEmpty())
            for ((k, v) in en) checkValue("$where.$k", v, p.strings[k])
            assertTrue("$where: unknown evidence ${(p.evidence.keys - evidence).take(3)}", (p.evidence.keys - evidence).isEmpty())
            assertTrue("$where: untranslated evidence ${(evidence - p.evidence.keys).take(3)}", (evidence - p.evidence.keys).isEmpty())
            for ((k, v) in p.evidence) assertFalse("$where evidence blank: $k", v.isBlank())
            p.quran?.let { q -> assertTrue("$where quran file", File("src/commonMain/assets/tafsir/${q.id}.txt").exists()) }
            // كل النصوص تعمل بلا أخطاء عبر Lang
            val s = Lang.fromPack(p).strings
            assertEquals(114, s.surahNames.size)
            for (t in ShareType.entries) assertFalse(s.shareType(t, true).contains('{'))
            for ((_, n) in PackExport.NOTES) assertFalse(where, s.inhNote(n).contains('{'))
            assertFalse(s.ymd(3, 1, 12).contains('{'))
            assertFalse(s.hijriDate(1, 9, 1447).contains('{'))
            assertTrue(Tx.all.isNotEmpty())
        }
    }

    /** متغيرات القالب ({name} أو {n, plural…})، لا صيغ الجمع نفسها مثل one {once} */
    private val vars = Regex("""(?<!(?:zero|one|two|few|many|other|=\d|=\d\d) )\{(\w+)[},]""")

    private fun checkValue(where: String, en: Any?, tr: Any?) {
        when (en) {
            is String -> {
                assertTrue("$where: text expected", tr is String)
                assertFalse("$where: blank", (tr as String).isBlank())
                val want = vars.findAll(en).map { it.groupValues[1] }.toSet()
                val got = vars.findAll(tr).map { it.groupValues[1] }.toSet()
                assertTrue("$where: unknown variables ${got - want} in “$tr”", (got - want).isEmpty())
                // متغيرات العدد والاسم يجب أن تبقى (عدا ما يُستغنى عنه في صيغة مفردة)
                assertTrue("$where: missing variables ${want - got} in “$tr”", (want - got).isEmpty())
                assertEquals("$where: braces", en.count { it == '{' } > 0, tr.count { it == '{' } > 0)
                assertEquals("$where: unbalanced braces", tr.count { it == '{' }, tr.count { it == '}' })
            }
            is List<*> -> {
                assertTrue("$where: list expected", tr is List<*>)
                assertEquals("$where size", en.size, (tr as List<*>).size)
                en.indices.forEach { checkValue("$where[$it]", en[it], tr[it]) }
            }
            is Map<*, *> -> {
                assertTrue("$where: object expected", tr is Map<*, *>)
                val m = tr as Map<*, *>
                assertEquals("$where keys", en.keys.map { it.toString() }.sorted(), m.keys.map { it.toString() }.sorted())
                en.forEach { (k, v) -> checkValue("$where.$k", v, m[k]) }
            }
        }
    }
}
