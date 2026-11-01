package com.osatks.mizan

import androidx.compose.ui.graphics.Color
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.ui.TABS
import com.osatks.mizan.ui.tabBarStateJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حالة شريط التبويبات من النظام في iOS 26 (NativeTabBar.kt ← LiquidTabBarState.swift): الحقول التي يقرؤها Swift بأسمائها،
 * ورموز SF Symbols، والمختار null في صفحة ليست تبويباً (فيبقى اختيار الشريط)، ولون التطبيق #RRGGBB
 */
class TabBarStateTest {
    @Test
    fun fieldsSwiftReads() {
        val m = Json.obj(tabBarStateJson(Ar, "prayer", visible = true, rtl = true, dark = false, accent = Color(0xFF0D5C4B)))
        assertEquals(setOf("tabs", "selected", "visible", "rtl", "dark", "accent"), m.keys)
        val tabs = (m["tabs"] as List<*>).map { it as Map<*, *> }
        assertEquals(TABS.map { it.route }, tabs.map { it["route"] })
        assertEquals(listOf("house", "clock", "book", "hand.tap", "gearshape"), tabs.map { it["symbol"] })
        assertEquals(listOf(Ar.tabHome, Ar.tabPrayer, Ar.tabQuran, Ar.tabTasbih, Ar.tabSettings), tabs.map { it["title"] })
        assertEquals("prayer", m["selected"])
        assertEquals(true, m["visible"])
        assertEquals(true, m["rtl"])
        assertEquals(false, m["dark"])
        assertEquals("#0D5C4B", m["accent"])
    }

    @Test
    fun toolScreensKeepTheSelection() {
        val m = Json.obj(tabBarStateJson(En, "qibla", visible = false, rtl = false, dark = true, accent = Color(0xFF3FB595)))
        assertTrue("selected" in m.keys)
        assertNull(m["selected"])
        assertEquals(false, m["visible"])
        assertEquals(En.tabHome, ((m["tabs"] as List<*>)[0] as Map<*, *>)["title"])
        assertEquals("#3FB595", m["accent"])
        // لون شفاف جزئياً: يُرسل لونه وحده بلا الشفافية
        assertEquals("#0A0B0C", Json.obj(tabBarStateJson(En, null, true, false, false, Color(0x800A0B0C)))["accent"])
    }
}
