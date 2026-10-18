package com.osatks.mizan

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.theme.Accent
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.recolor
import com.osatks.mizan.ui.theme.schemeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** ألوان التطبيق العشرة: الأخضر كما كان، والنصوص مقروءة في كل لون، فاتحاً وداكناً */
class AccentTest {
    private fun contrast(a: Color, b: Color): Double {
        val x = a.luminance() + 0.05; val y = b.luminance() + 0.05
        return max(x, y) / min(x, y)
    }

    @Test
    fun emeraldIsTheOriginalPalette() {
        assertEquals(10, Accent.entries.size)
        assertEquals(Accent.EMERALD, Accent.of("emerald"))
        assertEquals(Accent.EMERALD, Accent.of("no-such-color"))
        assertEquals(BrandGreen, BrandGreen.recolor(Accent.EMERALD))
        assertEquals(Color(0xFF0D5C4B), schemeFor(Accent.EMERALD, false).primary)
    }

    @Test
    fun onlyTheGreensChange() {
        for (a in Accent.entries - Accent.EMERALD) {
            for (c in listOf(Gold, GoldDeep, Color(0xFFF8F5EE), Color(0xFFB4412F), Color.White, Color.Black)) {
                assertEquals("$a $c", c, c.recolor(a))
            }
            assertNotEquals("$a", BrandGreen, BrandGreen.recolor(a))
        }
    }

    @Test
    fun textStaysReadableInEveryColor() {
        fun check(where: String, fg: Color, bg: Color, min: Double) =
            assertTrue("$where: contrast ${"%.2f".format(contrast(fg, bg))} < $min", contrast(fg, bg) >= min)
        for (a in Accent.entries) for (dark in listOf(false, true)) {
            val c: ColorScheme = schemeFor(a, dark)
            val w = "$a dark=$dark"
            check("$w onPrimary/primary", c.onPrimary, c.primary, 4.5)
            check("$w onBackground/background", c.onBackground, c.background, 7.0)
            check("$w onSurface/surface", c.onSurface, c.surface, 7.0)
            check("$w onSurfaceVariant/surface", c.onSurfaceVariant, c.surface, 4.5)
            check("$w onPrimaryContainer/primaryContainer", c.onPrimaryContainer, c.primaryContainer, 4.5)
            check("$w primary/background", c.primary, c.background, 4.5)
            // النص الأبيض على تدرّج الترويسات
            check("$w white on hero", Color.White, BrandGreen.recolor(a), 4.5)
        }
    }

    @Test
    fun everyLanguageNamesAllTenColors() {
        for (l in Lang.entries) assertEquals("${l.code}", Accent.entries.size, l.strings.accentNames.size)
    }
}
