package com.osatks.mizan

import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.quran.Sources
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** سلامة نص المصحف والتفاسير المضمّنة: كل ملف فيه 6236 آية في مواضعها، والبيانات الوصفية متسقة */
class QuranTest {
    companion object {
        @JvmStatic @BeforeClass
        fun setUp() { QuranData.open = { File("src/main/assets/$it").inputStream() } }
    }

    @Test
    fun metadataIsConsistent() {
        assertEquals(114, QuranMeta.COUNTS.size)
        assertEquals(6236, QuranMeta.COUNTS.sum())
        for (i in 0 until 114) assertEquals(QuranMeta.COUNTS.take(i).sum(), QuranMeta.STARTS[i])
        assertEquals(30, QuranMeta.JUZ.size)
        assertEquals(604, QuranMeta.PAGES.size)
        assertEquals(15, QuranMeta.SAJDAS.size)
        assertEquals(1 to 1, QuranMeta.locate(0))
        assertEquals(2 to 255, QuranMeta.locate(QuranMeta.index(2, 255)))
        assertEquals(114 to 6, QuranMeta.locate(6235))
        assertEquals(1, QuranMeta.juzOf(0))
        assertEquals(30, QuranMeta.juzOf(6235))
        assertEquals(604, QuranMeta.pageOf(6235))
        assertTrue(QuranMeta.isMedinan(2))
        assertFalse(QuranMeta.isMedinan(1))
    }

    @Test
    fun everyFileHasEveryAyah() = runBlocking {
        val files = listOf(QuranData.UTHMANI, QuranData.CLEAN) + Sources.ALL.map { "tafsir/${it.id}.txt" }
        for (f in files) {
            val text = QuranData.load(f)
            assertEquals(f, 6236, text.size)
            val empty = text.indices.filter { text[it].isBlank() }
            assertTrue("$f has empty ayahs: ${empty.take(5)}", empty.isEmpty())
        }
    }

    @Test
    fun bismillahIsShownSeparately() = runBlocking {
        val t = QuranData.uthmani()
        val bismillah = t[0]
        assertTrue(bismillah.startsWith("بِسْمِ"))
        for (surah in 2..114) {
            val first = t[QuranMeta.STARTS[surah - 1]]
            if (surah == 9) assertFalse(first.startsWith(bismillah))
            val shown = QuranData.withoutBismillah(t, QuranMeta.STARTS[surah - 1])
            assertFalse("surah $surah", shown.startsWith("بِسْمِ"))
            assertTrue("surah $surah", shown.isNotBlank())
        }
        assertEquals("الٓمٓ", QuranData.withoutBismillah(t, QuranMeta.index(2, 1)))
        assertEquals(bismillah, QuranData.withoutBismillah(t, 0)) // الفاتحة: البسملة آيتها الأولى
        // مقارنة بلا تشكيل (ترتيب علامات الشدة والحركة يختلف بين لوحات المفاتيح)
        assertTrue(com.osatks.mizan.core.Search.normalize(t[QuranMeta.index(2, 255)]).startsWith("الله لا اله الا هو الحي القيوم"))
        assertEquals("من الجنه والناس", com.osatks.mizan.core.Search.normalize(t[6235]))
    }

    @Test
    fun referencesAndSearch() = runBlocking {
        assertEquals(QuranMeta.index(2, 255), QuranSearch.parseRef("2:255"))
        assertEquals(QuranMeta.index(2, 255), QuranSearch.parseRef("٢:٢٥٥"))
        assertEquals(QuranMeta.index(18, 10), QuranSearch.parseRef("18 10"))
        assertEquals(null, QuranSearch.parseRef("2:287"))
        assertEquals(null, QuranSearch.parseRef("115:1"))

        val kursi = QuranMeta.index(2, 255)
        val ar = QuranSearch.ayahs("الحي القيوم", Lang.AR)
        assertTrue(ar.contains(kursi))
        assertTrue(ar.contains(QuranMeta.index(3, 2)))
        // الهمزات والتشكيل لا تمنع التطابق
        assertTrue(QuranSearch.ayahs("قُلْ أعوذ برب الناس", Lang.AR).contains(QuranMeta.index(114, 1)))
        // البحث بترجمة لغة الواجهة
        assertTrue(QuranSearch.ayahs("Kursi", Lang.EN).contains(kursi))
        assertTrue(QuranSearch.ayahs("a", Lang.EN).isEmpty())
    }

    @Test
    fun sourcesCoverEveryLanguage() {
        for (l in Lang.entries) {
            assertNotNull(l.code, Sources.defaultFor(l))
            if (l != Lang.AR) assertNotNull(l.code, Sources.translationFor(l))
            assertEquals(l.code, Sources.ALL.size, Sources.ordered(l).size)
            assertEquals(l.code, Sources.ALL.map { it.id }.toSet(), Sources.ordered(l).map { it.id }.toSet())
        }
        assertEquals("ar.muyassar", Sources.defaultFor(Lang.AR).id)
        assertEquals("id.jalalayn", Sources.defaultFor(Lang.ID).id)
        assertEquals("en.sahih", Sources.defaultFor(Lang.EN).id)
    }
}
