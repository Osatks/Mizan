package com.osatks.mizan

import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.quran.readingPalette
import com.osatks.mizan.ui.screens.WelcomeFlow
import com.osatks.mizan.ui.screens.welcomeThemeName
import com.osatks.mizan.ui.screens.welcomeToneName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** خطوات إعداد أول تشغيل: ترتيبها ومفاتيحها كما تقرؤها الواجهتان (Welcome.kt و NativeWelcome.swift) */
class WelcomeFlowTest {
    @Test
    fun stepsInOrder() {
        assertEquals(WelcomeFlow.COUNT, WelcomeFlow.KEYS.size)
        val idx = listOf(WelcomeFlow.HELLO, WelcomeFlow.PLACE, WelcomeFlow.ADHAN, WelcomeFlow.STYLE, WelcomeFlow.TASTE, WelcomeFlow.DONE)
        assertEquals((0 until WelcomeFlow.COUNT).toList(), idx)
        assertEquals(listOf("hello", "place", "adhan", "style", "taste", "done"), WelcomeFlow.KEYS)
    }

    /** المظهر وألوان الصفحة هي مفاتيح الإعدادات نفسها، ولكل منها اسم بكل لغة */
    @Test
    fun choicesUseAppKeys() {
        assertEquals(listOf("system", "light", "dark"), WelcomeFlow.THEMES)
        assertNull(readingPalette(WelcomeFlow.TONES.first()))
        WelcomeFlow.TONES.drop(1).forEach { assertNotNull(it, readingPalette(it)) }
        for (l in Lang.entries) {
            val s = l.strings
            assertEquals(3, WelcomeFlow.THEMES.map { s.welcomeThemeName(it) }.distinct().size)
            assertEquals(WelcomeFlow.TONES.size, WelcomeFlow.TONES.map { s.welcomeToneName(it) }.distinct().size)
        }
    }

    /** الأصوات المضمّنة في التطبيق تظهر دائماً (بلا تنزيل)، ولا أذان فجر بينها */
    @Test
    fun bundledVoicesAreOffered() {
        val bundled = Muezzins.ALL.filter { it.bundled && !it.fajr }
        assertTrue(bundled.isNotEmpty())
        assertFalse(bundled.any { it.fajr })
        assertTrue(Muezzins.DEFAULT in bundled)
    }
}
