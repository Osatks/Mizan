package com.osatks.mizan

import com.osatks.mizan.quran.JourneyKind
import com.osatks.mizan.quran.PageInfo
import com.osatks.mizan.quran.PageJourneys
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPages
import com.osatks.mizan.ui.quran.JUZ_START_PAGES
import com.osatks.mizan.ui.quran.crossesJuz
import com.osatks.mizan.ui.quran.pageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.io.File

/** اختيار الصفحة: ما في كل صفحة من مصحف المدينة (سورها وجزؤها وحزبها كما في رأسها)، وصفحات الأجزاء، ومسار فتح الصفحة */
class PageInfoTest {
    companion object {
        @JvmStatic @BeforeClass
        fun setUp() { QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() } }
    }

    private val uthmani by lazy { runBlocking { QuranData.uthmani() } }

    @Test
    fun pageContents() {
        assertEquals(PageInfo(1, listOf(1), 1, 1), QuranPages.info(1))
        assertEquals(PageInfo(2, listOf(2), 1, 1), QuranPages.info(2))
        // الأعراف، في الجزء التاسع والحزب الثامن عشر
        assertEquals(PageInfo(173, listOf(7), 9, 18), QuranPages.info(173))
        // أول النبأ: الجزء الثلاثون والحزب التاسع والخمسون
        assertEquals(PageInfo(582, listOf(78), 30, 59), QuranPages.info(582))
        // الصفحة الأخيرة: الإخلاص والفلق والناس
        assertEquals(PageInfo(604, listOf(112, 113, 114), 30, 60), QuranPages.info(604))
        // صفحة فيها سورتان: آخر الإسراء وأول الكهف
        assertEquals(listOf(17, 18), QuranPages.info(293).surahs)
        // خارج المدى يُقصر على أول المصحف وآخره
        assertEquals(1, QuranPages.info(0).page)
        assertEquals(604, QuranPages.info(999).page)
    }

    @Test
    fun everyPageIsConsistent() {
        assertEquals(QuranMeta.PAGES.size, QuranPages.COUNT)
        var juz = 1
        var hizb = 1
        for (p in 1..QuranPages.COUNT) {
            val info = QuranPages.info(p)
            assertTrue(info.surahs.isNotEmpty())
            assertEquals(info.surahs, info.surahs.sorted())
            // الجزء والحزب لا ينقصان من صفحة إلى التي بعدها
            assertTrue(info.juz >= juz && info.hizb >= hizb)
            juz = info.juz; hizb = info.hizb
        }
        assertEquals(30, juz)
        assertEquals(60, hizb)
    }

    @Test
    fun juzPages() {
        assertEquals(1..21, QuranPages.juzPages(1))
        assertEquals(22..41, QuranPages.juzPages(2))
        assertEquals(582..604, QuranPages.juzPages(30))
        assertEquals(30, JUZ_START_PAGES.size)
        assertEquals(1, JUZ_START_PAGES.first())
        assertEquals(582, JUZ_START_PAGES.last())
        assertEquals(30, QuranPages.juzStartingAt(582))
        assertEquals(0, QuranPages.juzStartingAt(173))
        assertEquals(9, QuranPages.juzByStart(173))
        // الحد بين الجزأين الأول والثاني بين الصفحتين 21 و 22
        assertTrue(crossesJuz(21, 22))
        assertTrue(crossesJuz(22, 21))
        assertTrue(crossesJuz(10, 300))
        assertFalse(crossesJuz(22, 23))
        assertFalse(crossesJuz(2, 3))
    }

    @Test
    fun pageRouteOpensTheFirstAyah() {
        assertEquals("surah/1?ayah=1&from=page", pageRoute(1))
        assertEquals("surah/2?ayah=1&from=page", pageRoute(2))
        assertEquals("surah/78?ayah=1&from=page", pageRoute(582))
        assertEquals("surah/112?ayah=1&from=page", pageRoute(604))
    }

    @Test
    fun hizbStarts() {
        assertEquals(1, QuranPages.hizbStartPage(1))
        // الحزب الثاني يبدأ في الصفحة 11 (البقرة 75)، والأخير في الصفحة 591 (الأعلى)
        assertEquals(QuranMeta.pageOf(QuranMeta.index(2, 75)), QuranPages.hizbStartPage(2))
        assertEquals(QuranMeta.pageOf(QuranMeta.index(87, 1)), QuranPages.hizbStartPage(60))
        var last = 0
        for (h in 1..60) {
            val p = QuranPages.hizbStartPage(h)
            assertTrue(p >= last)
            last = p
        }
    }

    /** معاينة الصفحة في اختيار الصفحة: أول آياتها بعلامة نهاية الآية ورقمها، بلا بسملة السورة، ولا كلمة مبتورة */
    @Test
    fun pageOpening() {
        val t = uthmani
        // الفاتحة: البسملة آيتها الأولى فتبقى
        val p1 = QuranPages.opening(t, 1)
        assertTrue(p1.startsWith(t[0]))
        assertTrue(p1.contains(" \u06DD١"))
        // أول البقرة: «الٓمٓ» بلا البسملة، ثم الآية الثانية
        val p2 = QuranPages.opening(t, 2)
        assertTrue(p2.startsWith("الٓمٓ \u06DD١ "))
        assertFalse(p2.contains(t[0]))
        // صفحة تبدأ بسورة: النبأ بلا البسملة
        assertTrue(QuranPages.opening(t, 582).startsWith(QuranData.withoutBismillah(t, QuranMeta.index(78, 1))))
        for (p in listOf(1, 2, 50, 173, 293, 450, 582, 604)) {
            val o = QuranPages.opening(t, p, max = 110)
            assertTrue(o.isNotBlank())
            // لا يزيد كثيراً على الحد (آخر آية كاملة قد تتجاوزه بعلامتها فقط)
            assertTrue("$p: ${o.length}", o.length <= 110 + 8)
            // كل كلمة في المعاينة كلمة كاملة من نص الصفحة
            val words = QuranMeta.pageRange(p).flatMap { QuranData.withoutBismillah(t, it).split(' ') }.toSet()
            for (w in o.split(' ')) assertTrue("$p: $w", w in words || w.startsWith("\u06DD") || w == "…")
        }
        // آية طويلة لا تسعها المعاينة تُقطع عند حد كلمة
        val long = QuranPages.opening(t, 50, max = 30)
        assertTrue(long.endsWith(" …"))
        assertTrue(long.length <= 30 + 2 || long.split(' ').size == 2)
        // خارج المدى
        assertEquals(QuranPages.opening(t, 1), QuranPages.opening(t, 0))
    }

    /** رحلة الانتقال إلى صفحة: الأوراق من الصفحة الحالية إلى المقصودة بالترتيب، وأربع إلى ست تقليبات تتسارع ثم تتباطأ */
    @Test
    fun journeyPlan() {
        val still = PageJourneys.plan(172, 172)
        assertEquals(JourneyKind.Still, still.kind)
        assertTrue(still.turns.isEmpty())
        val near = PageJourneys.plan(172, 173)
        assertEquals(JourneyKind.Short, near.kind)
        assertEquals(1, near.turns.size)
        assertEquals(JourneyKind.Short, PageJourneys.plan(172, 170).kind)

        for ((a, b) in listOf(0 to 3, 0 to 603, 603 to 0, 171 to 299, 299 to 171, 10 to 49, 10 to 50, 100 to 299)) {
            val j = PageJourneys.plan(a, b)
            assertEquals(JourneyKind.Flip, j.kind)
            assertEquals(a, j.sheets.first())
            assertEquals(b, j.sheets.last())
            assertEquals(j.turns.size + 1, j.sheets.size)
            assertEquals(j.sheets.size, j.pages.size)
            assertEquals(a + 1, j.pages.first())
            assertEquals(b + 1, j.pages.last())
            // كل ورقة بعد التي قبلها في اتجاه الرحلة، بلا تكرار
            val dir = if (b > a) 1 else -1
            j.sheets.zipWithNext { x, y -> assertTrue("$a→$b: ${j.sheets}", (y - x) * dir >= 1) }
            val d = kotlin.math.abs(b - a)
            if (d >= 4) assertTrue(j.turns.size in 4..6)
            // أبطأ في الطرفين وأسرع في الوسط
            assertTrue(j.turns.first() >= j.turns[j.turns.size / 2])
            assertTrue(j.turns.last() >= j.turns[j.turns.size / 2])
            // الرحلة كلها نحو ثانية إلى ثانية ونصف
            if (j.turns.size >= 4) assertTrue("${j.totalMs}", j.totalMs in 1000..1400)
            // الرقم الدوّار يبدأ من الصفحة الحالية ويبلغ رقم كل ورقة حين تنكشف، وينتهي بالمقصودة
            assertEquals(j.pages.first().toFloat(), j.valueAt(0f), 0.001f)
            for (k in j.turns.indices) assertEquals(j.pages[k + 1].toFloat(), j.valueAt((j.turnStart(k) + j.turns[k]).toFloat()), 0.001f)
            assertEquals(j.pages.last().toFloat(), j.valueAt(99_999f), 0.001f)
        }
        assertEquals(6, PageJourneys.plan(0, 603).turns.size)
        assertEquals(4, PageJourneys.plan(10, 30).turns.size)
        assertEquals(3, PageJourneys.plan(10, 13).turns.size)

        // الكتاب المفتوح: المواضع كتب، والأرقام صفحاتها اليمنى، وطرفا الرحلة الصفحتان الحقيقيتان
        val spread = PageJourneys.plan(10, 150, count = 303, fromPage = 22, toPage = 301) { 2 * it + 1 }
        assertEquals(22, spread.pages.first())
        assertEquals(301, spread.pages.last())
        spread.pages.drop(1).dropLast(1).forEach { assertTrue(it % 2 == 1) }
        // خارج المدى يُقصر على أول المصحف وآخره
        assertEquals(603, PageJourneys.plan(0, 9999).to)
    }
}
