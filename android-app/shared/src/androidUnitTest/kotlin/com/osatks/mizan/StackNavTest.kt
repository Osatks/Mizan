package com.osatks.mizan

import com.osatks.mizan.ui.BackSwipe
import com.osatks.mizan.ui.StackNav
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** تنقل iOS (StackNav): بلا نطاق حركة تُطبَّق العمليات فوراً، فتُختبر حالها بعد كل عملية */
class StackNavTest {
    @Test fun startsOnHomeTab() {
        val nav = StackNav("home")
        assertEquals("home", nav.current)
        assertTrue(nav.onTab)
        assertTrue(nav.pages.isEmpty())
    }

    @Test fun pushAndPop() {
        val nav = StackNav("home")
        nav.push("zakat")
        assertEquals("zakat", nav.current)
        assertFalse(nav.onTab)
        // نقرة ثانية على الأداة نفسها لا تفتحها مرتين
        nav.push("zakat")
        assertEquals(1, nav.pages.size)
        nav.push("hijri")
        assertEquals(listOf("zakat", "hijri"), nav.pages.map { it.route })
        assertTrue(nav.pop())
        assertEquals("zakat", nav.current)
        assertTrue(nav.pop())
        assertEquals("home", nav.current)
        assertTrue(nav.onTab)
        // لا رجوع من صفحة التبويب
        assertFalse(nav.pop())
        assertEquals("home", nav.current)
    }

    @Test fun tabSwitchClosesPagesAndKeepsTabs() {
        val nav = StackNav("home")
        nav.push("zakat")
        nav.goTab("prayer")
        assertEquals("prayer", nav.current)
        assertTrue(nav.pages.isEmpty())
        nav.goTab("home")
        // صفحة الزكاة لا تعود مخبّأة في الرئيسية
        assertEquals("home", nav.current)
        assertTrue(nav.onTab)
        // فتح تبويب ظاهر لا يغيّر شيئاً
        nav.push("home")
        assertTrue(nav.pages.isEmpty())
    }

    @Test fun searchResultReplacesSearch() {
        val nav = StackNav("home")
        nav.push("search")
        nav.replace("zakat")
        assertEquals(listOf("zakat"), nav.pages.map { it.route })
        assertTrue(nav.pop())
        assertEquals("home", nav.current)
    }

    @Test fun resetClosesEverythingAboveTheTab() {
        val nav = StackNav("home")
        nav.goTab("settings")
        nav.push("adhan")
        nav.push("licenses")
        nav.reset()
        assertEquals("settings", nav.current)
        assertTrue(nav.pages.isEmpty())
    }

    @Test fun swipeBackCommitsOrCancels() = runBlocking {
        val nav = StackNav("home")
        nav.push("zakat")
        // تراجع المستخدم: تبقى الصفحة
        try {
            nav.gesture(flow {
                emit(BackSwipe(0.2f, fromLeft = false))
                throw CancellationException("cancel")
            })
        } catch (_: CancellationException) {
        }
        assertEquals("zakat", nav.current)
        // سحب اكتمل: ترجع الصفحة
        nav.gesture(flowOf(BackSwipe(0.3f, fromLeft = true), BackSwipe(0.8f, fromLeft = true)))
        assertEquals("home", nav.current)
        assertTrue(nav.pages.isEmpty())
    }
}
