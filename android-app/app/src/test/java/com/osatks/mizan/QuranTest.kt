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

    @Test
    fun pagesAndHizbsCoverTheMushaf() {
        var next = 0
        for (p in 1..604) {
            val r = QuranMeta.pageRange(p)
            assertEquals(next, r.first); assertTrue(!r.isEmpty())
            next = r.last + 1
        }
        assertEquals(6236, next)
        assertEquals(1, QuranMeta.hizbOf(0))
        assertEquals(60, QuranMeta.hizbOf(6235))
    }

    @Test
    fun mushafLinesAreBalancedAndJustifiable() {
        val widths = FloatArray(40) { 20f + (it * 37 % 23) }
        val lines = com.osatks.mizan.ui.quran.balancedLines(widths, 5f, 200f)
        // نفس عدد أسطر الملء الجشع، وكل الكلمات بترتيبها، ولا سطر يتجاوز العرض
        assertEquals(com.osatks.mizan.ui.quran.greedyLines(widths, 5f, 200f), lines.size)
        assertEquals((0 until 40).toList(), lines.flatMap { it.toList() })
        val used = lines.map { r -> r.sumOf { widths[it].toDouble() } + 5.0 * (r.count() - 1) }
        assertTrue(used.all { it <= 200.0 })
        // السطر الأخير ممتلئ كغيره (لا يبقى قصيراً)
        assertTrue(used.last() > 200.0 * 0.6)
    }

    @Test
    fun ayahOfDayIsStableAndShort() {
        val text = runBlocking { QuranData.uthmani() }
        val d = java.time.LocalDate.of(2026, 10, 15)
        val a = com.osatks.mizan.widget.AyahWidget.ayahOfDay(text, d)
        assertEquals(a, com.osatks.mizan.widget.AyahWidget.ayahOfDay(text, d))
        assertTrue(QuranData.withoutBismillah(text, a).length in 40..160)
        val week = (0L..6L).map { com.osatks.mizan.widget.AyahWidget.ayahOfDay(text, d.plusDays(it)) }.toSet()
        assertEquals(7, week.size)
    }

    @Test
    fun mushafPagesFillTheFrameAndEndFlush() {
        val text = runBlocking { QuranData.uthmani() }
        // قياس تقريبي ثابت: يكفي لاختبار التوزيع (القياس الحقيقي من خط المصحف على الجهاز)
        val m = com.osatks.mizan.ui.quran.Measure { t, size -> t.length * size * 0.42f }
        val w = 1000f; val h = 1700f
        for (page in listOf(1, 2, 3, 50, 82, 151, 293, 450, 590, 604)) {
            val blocks = com.osatks.mizan.ui.quran.pageBlocks(page, text)
            val l = com.osatks.mizan.ui.quran.layoutPage(blocks, w, h, 13f, 30f, 0.5f, m)
            assertTrue("page $page fits", l.fits)
            val rows = l.rows.filterIsInstance<com.osatks.mizan.ui.quran.TextRow>()
            // كل آيات الصفحة بترتيبها، وكل آية تنتهي بعلامتها مرة واحدة
            val ends = rows.flatMap { r -> r.words.filter { it.word.marker != null }.map { it.word.index } }
            assertEquals("page $page ayahs", QuranMeta.pageRange(page).toList(), ends)
            for (r in rows) for (pl in r.words) assertTrue("page $page inside", pl.left >= -1f && pl.right <= w + 1f)
            if (!l.capped) {
                // تبدأ الصفحة في أعلاها وتنتهي في أسفلها، والسطر الأخير مضبوط حتى طرفه
                assertEquals(0f, l.rows.first().top, 0.5f)
                val last = l.rows.last()
                assertEquals("page $page bottom", h, last.top + last.height, 0.5f)
                val lr = rows.last()
                assertEquals("page $page flush", 0f, lr.words.last().left, 1f)
                assertEquals(w, lr.words.first().right, 1f)
            }
            // اللمس في وسط كلمة يعيد آيتها
            val pl = rows[rows.size / 2].words.first()
            val row = rows[rows.size / 2]
            assertEquals(pl.word.index, l.hit((pl.left + pl.right) / 2, row.top + row.height / 2))
        }
    }

    @Test
    fun downloadedTafsirStoresSharedCommentaryOnce() {
        val lib = com.osatks.mizan.quran.TafsirLibrary
        // تفسير يشرح الآيات 2-4 معاً (يتكرر نصه في المصدر لكل آية)، مع حاشية محقق وأسطر فارغة زائدة
        val shared = "شرح الآيات [[في أ: نسخة]] من 2 إلى 4\n\n\n\nفقرة ثانية"
        val entries = listOf(1 to "شرح الآية الأولى", 2 to shared, 3 to shared, 4 to shared, 5 to "شرح الخامسة")
        val lines = lib.encodeSurah(entries)
        assertEquals(5, lines.size)
        assertEquals(listOf("3|=2", "4|=2"), lines.subList(2, 4))
        assertFalse(lines.joinToString("").contains("[["))
        val surah = lib.decodeSurah(lines.asSequence(), 5)
        val e3 = surah.entry(3)
        assertEquals("شرح الآيات من 2 إلى 4\n\nفقرة ثانية", e3.text)
        assertEquals(2, e3.from); assertEquals(4, e3.to)
        assertEquals(surah.entry(2).text, surah.entry(4).text)
        val e1 = surah.entry(1)
        assertEquals(1, e1.from); assertEquals(1, e1.to)
        assertEquals("شرح الخامسة", surah.entry(5).text)
    }
}
