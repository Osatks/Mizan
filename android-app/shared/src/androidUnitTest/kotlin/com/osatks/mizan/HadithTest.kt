package com.osatks.mizan

import com.osatks.mizan.hadith.Hadiths
import kotlinx.datetime.LocalDate
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** أحاديث اليوم (assets/hadith): العدد، واكتمال النص العربي، والترجمة لكل لغة أو بديلها، وتتابع الأيام */
class HadithTest {
    @Before
    fun setUp() {
        Hadiths.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
    }

    @Test
    fun everyHadithIsCompleteInArabic() {
        val ids = Hadiths.ids()
        assertEquals(366, ids.size)
        assertEquals(ids.size, ids.toSet().size)
        for (id in ids) {
            val h = Hadiths.get(id, "ar")!!
            assertTrue(id, h.ar.title.isNotBlank() && h.ar.text.isNotBlank() && h.ar.expl.isNotBlank())
            assertTrue(id, h.ar.title.length <= 120)
            assertNull(h.tr)
        }
    }

    @Test
    fun everyLanguageHasATranslationOrFallback() {
        val codes = listOf("en", "fr", "tr", "ur", "id", "ms", "fa", "bn", "ru", "de", "es", "uk", "uz", "kk")
        val fallback = mapOf("ms" to "id", "kk" to "ru", "uz" to "ru")
        for (code in codes) for (id in Hadiths.ids()) {
            val h = Hadiths.get(id, code)!!
            val tr = h.tr
            assertNotNull("$code $id", tr)
            assertTrue("$code $id", tr!!.text.isNotBlank() && tr.expl.isNotBlank() && tr.title.isNotBlank())
            assertTrue("$code $id ${h.trLang}", h.trLang == code || h.trLang == fallback[code] || h.trLang == "en")
            assertTrue(h.link.startsWith("https://hadeethenc.com/${h.trLang}/browse/hadith/$id"))
        }
    }

    @Test
    fun consecutiveDaysShowDifferentHadiths() {
        val d = LocalDate(2026, 11, 7)
        val a = Hadiths.forDay(d, "ar")!!
        val b = Hadiths.forDay(LocalDate(2026, 11, 8), "ar")!!
        assertNotEquals(a.id, b.id)
        // بعد دورة كاملة يعود الحديث نفسه
        assertEquals(a.id, Hadiths.forDay(LocalDate.fromEpochDays(d.toEpochDays() + 366), "en")!!.id)
    }
}
