package com.osatks.mizan

import com.osatks.mizan.quran.PageInfo
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPages
import com.osatks.mizan.ui.quran.JUZ_START_PAGES
import com.osatks.mizan.ui.quran.crossesJuz
import com.osatks.mizan.ui.quran.pageRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** اختيار الصفحة: ما في كل صفحة من مصحف المدينة (سورها وجزؤها وحزبها كما في رأسها)، وصفحات الأجزاء، ومسار فتح الصفحة */
class PageInfoTest {
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
}
