package com.osatks.mizan

import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.Search
import com.osatks.mizan.core.ShareType
import com.osatks.mizan.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CoreTest {
    init { TestPacks.load() }

    @Test
    fun parseNumbersInEveryLanguage() {
        assertEquals(1234.5, Fmt.parse("1,234.5", Lang.EN), 1e-9)
        assertEquals(1234.5, Fmt.parse("1.234,5", Lang.DE), 1e-9)
        assertEquals(1500.0, Fmt.parse("1.500", Lang.DE), 1e-9)
        assertEquals(1.5, Fmt.parse("1.500", Lang.EN), 1e-9)
        assertEquals(2.5, Fmt.parse("2,5", Lang.FR), 1e-9)
        assertEquals(1500.0, Fmt.parse("1 500", Lang.FR), 1e-9)
        assertEquals(2.5, Fmt.parse("2,5", Lang.EN), 1e-9)
        assertEquals(1234.0, Fmt.parse("١٢٣٤", Lang.AR), 1e-9)
        assertEquals(12.5, Fmt.parse("١٢٫٥", Lang.AR), 1e-9)
        assertEquals(1234.0, Fmt.parse("۱۲۳۴", Lang.FA), 1e-9)
        assertEquals(1234.0, Fmt.parse("১২৩৪", Lang.BN), 1e-9)
        assertEquals(0.0, Fmt.parse("abc", Lang.EN), 1e-9)
        assertEquals(0.0, Fmt.parse("-5", Lang.EN), 1e-9)
    }

    @Test
    fun formatUsesLanguageSeparators() {
        assertEquals("1,234.5", Fmt.num(1234.5, 2, Lang.EN))
        assertEquals("1.234,5", Fmt.num(1234.5, 2, Lang.DE))
        assertEquals("1.234,5", Fmt.num(1234.5, 2, Lang.TR))
        assertEquals("1,234.5", Fmt.num(1234.5, 2, Lang.AR))
        for (l in Lang.entries) assertEquals(l.code, 1234.5, Fmt.parse(Fmt.num(1234.5, 2, l), l), 1e-9)
    }

    @Test
    fun searchNormalizesScripts() {
        assertEquals(Search.normalize("الزكاة"), Search.normalize("الزكاه"))
        assertEquals(Search.normalize("إسلام"), Search.normalize("اسلام"))
        assertEquals(Search.normalize("سُبْحَانَ"), Search.normalize("سبحان"))
        assertEquals(Search.normalize("زکات"), Search.normalize("زكات"))      // ک فارسية
        assertEquals(Search.normalize("Zekât"), Search.normalize("zekat"))
        assertEquals(Search.normalize("İstanbul"), Search.normalize("istanbul"))
        assertTrue(Search.score(Search.terms("mekk"), "Mekke") > 0)
        assertTrue(Search.score(Search.terms("zak"), "Zakat Calculator") > Search.score(Search.terms("zak"), "Other", "zakat"))
        assertEquals(0, Search.score(Search.terms("qibla"), "Zakat"))
        // الفاصلة العليا في الأسماء المنقولة لا تقسم الكلمة
        assertTrue(Search.score(Search.terms("enam"), "En'âm") > 0)
        assertTrue(Search.score(Search.terms("araf"), "Al-A'raf") > 0)
        assertTrue(Search.score(Search.terms("muminun"), "Mü'minûn") > 0)
    }

    @Test
    fun inheritanceClassicCases() {
        // زوج وأم وأخت شقيقة وجد: الأكدرية
        val akd = Inheritance.solve(mapOf("husband" to 1, "mother" to 1, "fs" to 1, "gf" to 1), 'f')!!
        assertEquals(27L, akd.tashih)
        assertTrue(akd.notes.contains(InhNote.Akdariyya))
        // زوجة وأب وأم: العمرية، للأم ربع التركة
        val umar = Inheritance.solve(mapOf("wife" to 1, "father" to 1, "mother" to 1), 'm')!!
        assertEquals("1/4", umar.res.getValue("mother").share.toString())
        // ابن يحجب الأخ الشقيق
        val blk = Inheritance.solve(mapOf("son" to 1, "fb" to 1), 'm')!!
        assertEquals("son", blk.res.getValue("fb").blocked)
        // زوج وأختان شقيقتان: عول من 6 إلى 7
        val awl = Inheritance.solve(mapOf("husband" to 1, "fs" to 2), 'f')!!
        assertEquals(7L, awl.awl)
        // بنت وحدها: فرض وردّ
        val radd = Inheritance.solve(mapOf("daughter" to 1), 'm')!!
        assertTrue(radd.radd)
        assertEquals(ShareType.FARD, radd.res.getValue("daughter").type)
        assertTrue(radd.res.getValue("daughter").radd)
    }

    @Test
    fun tehranMaghribIsAfterSunset() {
        val t = PrayTimes.compute(LocalDate.of(2026, 3, 21), 35.6892, 51.389, "Asia/Tehran", "tehran", false)
        assertTrue(t.maghrib - t.sunrise > 11.0)
        val sunset = PrayTimes.compute(LocalDate.of(2026, 3, 21), 35.6892, 51.389, "Asia/Tehran", "mwl", false).maghrib
        assertTrue("maghrib ${t.maghrib} sunset $sunset", t.maghrib > sunset + 0.2 && t.maghrib < sunset + 0.4)
    }

    @Test
    fun citiesAreUniqueAndValid() {
        assertEquals(PrayTimes.CITIES.size, PrayTimes.CITIES.map { it.key }.toSet().size)
        PrayTimes.CITIES.forEach { c ->
            assertTrue(c.key, PrayTimes.METHODS.any { it.key == c.method })
            assertTrue(c.key, java.time.ZoneId.of(c.tz) != null)
            assertEquals(c.key, 2, c.cc.length)
        }
    }
}
