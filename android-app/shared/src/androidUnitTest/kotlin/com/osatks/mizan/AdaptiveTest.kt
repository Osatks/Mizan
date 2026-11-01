package com.osatks.mizan

import androidx.compose.ui.unit.dp
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.ui.SizeClass
import com.osatks.mizan.ui.WindowLayout
import com.osatks.mizan.ui.guttersFor
import com.osatks.mizan.ui.heightClassOf
import com.osatks.mizan.ui.spreadFits
import com.osatks.mizan.ui.widthClassOf
import com.osatks.mizan.ui.quran.Spreads
import com.osatks.mizan.ui.quran.mushafFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * التخطيط المتكيّف: فئات النافذة، ومتى تظهر القائمة الجانبية والصفحتان المتقابلتان، وتحويل الصفحة إلى موضع التقليب وعكسه
 * (حتى يبقى القارئ على صفحته حين تتغير النافذة). المقاسات بنقاط الأجهزة الحقيقية.
 */
class AdaptiveTest {
    @Test
    fun sizeClasses() {
        assertEquals(SizeClass.Compact, widthClassOf(360.dp))   // الشاشة الخارجية لـ iPhone Duo وأضيق أندرويد
        assertEquals(SizeClass.Compact, widthClassOf(599.dp))
        assertEquals(SizeClass.Medium, widthClassOf(600.dp))
        assertEquals(SizeClass.Medium, widthClassOf(820.dp))    // iPad Air بالطول
        assertEquals(SizeClass.Expanded, widthClassOf(840.dp))
        assertEquals(SizeClass.Expanded, widthClassOf(1376.dp)) // iPad Pro 13 بالعرض
        assertEquals(SizeClass.Compact, heightClassOf(360.dp))
        assertEquals(SizeClass.Medium, heightClassOf(744.dp))
        assertEquals(SizeClass.Expanded, heightClassOf(1032.dp))
    }

    @Test
    fun railOnlyWhenWideOrMediumLandscape() {
        assertFalse(WindowLayout(402.dp, 874.dp).rail)    // iPhone
        assertFalse(WindowLayout(360.dp, 780.dp).rail)    // الشاشة الخارجية لـ iPhone Duo
        assertFalse(WindowLayout(820.dp, 1180.dp).rail)   // iPad Air بالطول: الشريط السفلي في الوسط
        assertTrue(WindowLayout(1180.dp, 820.dp).rail)    // iPad Air بالعرض
        assertTrue(WindowLayout(1032.dp, 1376.dp).rail)   // iPad Pro 13 بالطول (واسع)
        assertTrue(WindowLayout(760.dp, 700.dp).rail)     // متوسط بالعرض (شاشة داخلية قابلة للطي)
        assertTrue(WindowLayout(800.dp, 360.dp).rail)     // هاتف أندرويد بالعرض
        assertFalse(WindowLayout(590.dp, 1180.dp).rail)   // نصف iPad في Split View
        assertFalse(WindowLayout(400.dp, 800.dp).wide)
        assertTrue(WindowLayout(700.dp, 760.dp).wide)
    }

    @Test
    fun spreadOnlyWhenWideAndNearSquareOrLandscape() {
        assertTrue(spreadFits(1180.dp, 820.dp))   // iPad بالعرض
        assertTrue(spreadFits(760.dp, 780.dp))    // شاشة داخلية شبه مربعة (0.97)
        assertTrue(spreadFits(951.dp, 669.dp))    // شاشة داخلية بالعرض
        assertFalse(spreadFits(669.dp, 951.dp))   // شاشة داخلية بالطول (0.70): صفحة واحدة بنسبة المصحف
        assertFalse(spreadFits(820.dp, 1180.dp))  // iPad بالطول
        assertFalse(spreadFits(402.dp, 874.dp))   // iPhone
        assertFalse(spreadFits(800.dp, 360.dp))   // هاتف بالعرض: لا يتسع الارتفاع لصفحتين مقروءتين
        assertFalse(spreadFits(590.dp, 560.dp))   // أضيق من 600
    }

    @Test
    fun spreadsPairOddRightEvenLeft() {
        assertEquals(604, Spreads.PAGES)
        assertEquals(302, Spreads.count(spread = true))
        assertEquals(604, Spreads.count(spread = false))
        assertEquals(1..2, Spreads.pages(0, true))          // الفاتحة يميناً وأول البقرة يساراً
        assertEquals(603..604, Spreads.pages(301, true))
        assertEquals(5..5, Spreads.pages(4, false))
        for (p in 1..604) {
            val i = Spreads.index(p, true)
            assertTrue(p in Spreads.pages(i, true))
            // الأولى في الكتاب المفتوح (اليمنى، أول ما في الصف العربي) فردية دائماً
            assertTrue(Spreads.isRight(Spreads.pages(i, true).first))
            assertEquals(p, Spreads.pages(Spreads.index(p, false), false).first)
        }
        assertEquals(QuranMeta.pageRange(5).first..QuranMeta.pageRange(6).last, Spreads.ayahs(2, true))
    }

    @Test
    fun resizeKeepsTheSamePage() {
        // صفحة زوجية (يسرى) في عرض الصفحة الواحدة → تتسع النافذة → الكتاب المفتوح الذي فيه، والصفحة نفسها باقية
        var anchor = 6
        val wide = Spreads.index(anchor, true)
        assertEquals(2, wide)
        anchor = Spreads.follow(anchor, wide, true)
        assertEquals(6, anchor)
        // تضيق النافذة ثانية دون تقليب: تعود الصفحة 6 لا 5
        assertEquals(5, Spreads.index(anchor, false))
        assertEquals(6, Spreads.follow(anchor, Spreads.index(anchor, false), false))
        // التقليب في الكتاب المفتوح ينقل القارئ إلى الصفحة اليمنى من الموضع الجديد
        assertEquals(7, Spreads.follow(anchor, 3, true))
        // ما خرج عن المصحف يُقصر على أوله وآخره
        assertEquals(0, Spreads.index(0, true))
        assertEquals(301, Spreads.index(9999, true))
    }

    @Test
    fun gutterCentersContent() {
        // الهاتف: 16 من كل جهة كما كان
        guttersFor(390.dp, 720.dp, 0.dp, 0.dp).let { assertEquals(16.dp, it.start); assertEquals(16.dp, it.end) }
        // iPad بالعرض: عمود 720 في الوسط
        guttersFor(1180.dp, 720.dp, 0.dp, 0.dp).let { assertEquals(it.start, it.end); assertEquals(1180.dp - it.start - it.end, 720.dp) }
        // بعد القائمة الجانبية (96) يتوسط المحتوى ما بقي من العرض
        guttersFor(1180.dp, 720.dp, 96.dp, 0.dp).let { assertEquals(1180.dp - it.start - it.end, 720.dp); assertEquals(it.start - 96.dp, it.end) }
    }

    @Test
    fun mushafFrameKeepsPrintedAspect() {
        // الهاتف: عرض الشاشة كله كما كان
        assertEquals(390.dp, mushafFrame(390.dp, 700.dp, 0.dp, spread = false, book = false))
        // iPad بالطول: صفحة واحدة بنسبة 0.7 في الوسط (لا ورقة بعرض الشاشة)
        val single = mushafFrame(820.dp, 1000.dp, 0.dp, spread = false, book = true)
        assertTrue(single < 820.dp)
        assertEquals(((1000 - 18) * 0.7f + 24), single.value, 0.5f)
        // iPad بالعرض: صفحتان كل منهما بنسبة 0.7
        val spread = mushafFrame(1180.dp, 700.dp, 0.dp, spread = true, book = true)
        assertEquals(((700 - 18) * 0.7f * 2 + 30), spread.value, 0.5f)
        // نافذة عريضة جداً وقصيرة: الصفحتان لا تتجاوزان العرض
        assertTrue(mushafFrame(620.dp, 1000.dp, 0.dp, spread = true, book = true) <= 620.dp)
    }
}
