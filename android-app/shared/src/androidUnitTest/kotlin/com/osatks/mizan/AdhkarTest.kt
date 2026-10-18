package com.osatks.mizan

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.quran.QuranMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** الأذكار: الأقسام كاملة، ولكل ذكر مصدر ودرجة، وكل لغة تترجم كل ذكر وأسماء الكتب والدرجات */
class AdhkarTest {
    init {
        TestPacks.load()
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
    }

    private val books = setOf("quran", "bukhari", "muslim", "abudawud", "tirmidhi", "nasai", "nasai_kubra", "ibnmajah", "ahmad", "malik",
        "darimi", "ibnhibban", "ibnkhuzaymah", "hakim", "tabarani", "bayhaqi", "ibnalsunni", "adabmufrad", "ibnabishaybah", "abdurrazzaq", "bukhari_muallaq", "nasai_amal", "majma")
    private val grades = setOf("quran", "muttafaq", "sahih", "hasan", "daif")

    @Test
    fun sectionsAndEvidence() {
        val secs = Adhkar.sections()
        assertTrue("at least 15 sections, got ${secs.size}", secs.size >= 15)
        for (id in listOf("morning", "evening", "sleep", "waking", "afterPrayer", "ramadan", "quranDuas")) assertNotNull(id, Adhkar.section(id))
        val ids = HashSet<String>()
        for (s in secs) {
            assertTrue(s.id, s.items.isNotEmpty())
            for (d in s.items) {
                assertTrue("duplicate ${d.id}", ids.add(d.id))
                assertTrue(d.id, d.ar.isNotBlank() && d.count >= 1)
                assertFalse("${d.id}: instructions left in the text", d.ar.contains("((") || d.ar.contains("مرات)") || d.ar.contains("مرة)"))
                assertTrue("${d.id}: source", d.src.isNotEmpty() && d.src.all { it.first in books && it.second.isNotBlank() })
                assertTrue("${d.id}: grade ${d.grade}", d.grade in grades)
                if (d.grade != "quran") assertNotNull("${d.id}: narrator", d.narr)
                if (d.grade in setOf("sahih", "hasan", "daif") && d.src.none { it.first == "bukhari" || it.first == "muslim" || it.first == "bukhari_muallaq" })
                    assertTrue("${d.id}: who graded it", d.by.isNotEmpty())
                for ((su, a, b) in d.ayah) assertTrue("${d.id}: ayah", su in 1..114 && a in 1..QuranMeta.COUNTS[su - 1] && b in a..QuranMeta.COUNTS[su - 1])
            }
        }
        // المساء غير الصباح: أمسينا بدل أصبحنا
        assertTrue(Adhkar.section("evening")!!.items.any { "أَمْسَيْنَا" in it.ar || "أمسينا" in it.ar })
    }

    @Test
    fun everyLanguageTranslatesEverything() {
        val all = Adhkar.sections()
        for (l in Lang.entries) {
            val tr = Adhkar.tr(l)
            val own = if (l == Lang.EN) tr else com.osatks.mizan.adhkar.AdhkarTr.from(
                l.pack?.adhkar ?: com.osatks.mizan.i18n.pack.Json.obj(File("src/commonMain/assets/adhkar/${l.code}.json").readText()))
            for (b in all.flatMap { s -> s.items.flatMap { d -> d.src.map { it.first } } }.toSet()) assertNotNull("${l.code}: book $b", own.books[b])
            for (g in all.flatMap { s -> s.items.map { it.grade } }.toSet()) assertNotNull("${l.code}: grade $g", own.grades[g])
            for (b in all.flatMap { s -> s.items.map { it.by } }.filter { it.isNotEmpty() }.toSet()) assertNotNull("${l.code}: by $b", own.by[b])
            if (l == Lang.AR) continue
            for (s in all) {
                assertNotNull("${l.code}: section ${s.id}", own.sections[s.id])
                for (g in s.groups) if (g.titleAr != null && s.groups.size > 1) assertNotNull("${l.code}: group ${g.key}", own.groups[g.key])
                for (d in s.items) {
                    val t = own.items[d.id]
                    assertNotNull("${l.code}: item ${d.id}", t)
                    if (d.ayah.isEmpty() || d.grade == "quran") assertFalse("${l.code}: ${d.id} translation", t!!.t.isNullOrBlank())
                    if (d.narr != null) assertFalse("${l.code}: ${d.id} narrator", t!!.narr.isNullOrBlank())
                    if (d.fadl != null) assertFalse("${l.code}: ${d.id} virtue", t!!.fadl.isNullOrBlank())
                    if (d.note != null) assertFalse("${l.code}: ${d.id} note", t!!.note.isNullOrBlank())
                }
            }
        }
    }
}
