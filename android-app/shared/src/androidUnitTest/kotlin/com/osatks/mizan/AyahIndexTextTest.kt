package com.osatks.mizan

import com.osatks.mizan.quran.AyahIndexText
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.ui.screens.SearchLinks
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * نصوص فهرسة الآيات في بحث iPhone (SystemSpotlight.kt): من يكتب بعض كلمات الآية بلا تشكيل، بهمزاتها أو بدونها،
 * أو بالرسم العثماني، يجدها؛ ولا تطابق آية ما ليس فيها.
 */
class AyahIndexTextTest {
    companion object {
        @JvmStatic @BeforeClass
        fun setUp() { QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() } }
    }

    private val uthmani by lazy { runBlocking { QuranData.uthmani() } }
    private val clean by lazy { runBlocking { QuranData.clean() } }

    /** كلمات الآية كما يفهرسها SystemSpotlight (بلا بسملة أول السورة) */
    private fun keywords(surah: Int, ayah: Int): List<String> {
        val i = QuranMeta.index(surah, ayah)
        return AyahIndexText.keywords(QuranData.withoutBismillah(clean, i), QuranData.withoutBismillah(uthmani, i))
    }

    @Test
    fun stripsTashkeelButKeepsLetters() {
        assertEquals("إياك نعبد", AyahIndexText.stripMarks("إِيَّاكَ نَعْبُدُ"))
        // ألف الوصل والألف الخنجرية والتطويل في الرسم العثماني
        assertEquals("الرحمن الرحيم", AyahIndexText.stripMarks("ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"))
        assertEquals("ذلك الكتب", AyahIndexText.stripMarks("ذَٰلِكَ ٱلْكِتَـٰبُ"))
    }

    @Test
    fun wordsWithoutDiacriticsFindTheAyah() {
        // بلا همزات كما يكتب أكثر الناس
        assertTrue(AyahIndexText.matches("اياك نعبد", keywords(1, 5)))
        // بالهمزات، وبالتشكيل كاملاً
        assertTrue(AyahIndexText.matches("إياك نستعين", keywords(1, 5)))
        assertTrue(AyahIndexText.matches("إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", keywords(1, 5)))
        // التاء المربوطة هاءً، وبداية الكلمة تكفي
        assertTrue(AyahIndexText.matches("ويقيمون الصلاه", keywords(2, 3)))
        assertTrue(AyahIndexText.matches("ويقيم الصل", keywords(2, 3)))
        // الرسم الإملائي والعثماني كلاهما: «الكتاب» و«الكتب»
        assertTrue(AyahIndexText.matches("ذلك الكتاب لا ريب فيه", keywords(2, 2)))
        assertTrue(AyahIndexText.matches("ذلك الكتب", keywords(2, 2)))
        // آية الكرسي: صور الألف والهمزة
        assertTrue(AyahIndexText.matches("الله لا اله الا هو الحي القيوم", keywords(2, 255)))
        assertTrue(AyahIndexText.matches("لا تأخذه سنة ولا نوم", keywords(2, 255)))
        assertTrue(AyahIndexText.matches("لا تاخذه سنه ولا نوم", keywords(2, 255)))
        // الألف المقصورة ياءً
        assertTrue(AyahIndexText.matches("هدي للمتقين", keywords(2, 2)))
    }

    @Test
    fun otherAyahsDoNotMatch() {
        assertFalse(AyahIndexText.matches("الكتاب", keywords(1, 5)))
        assertFalse(AyahIndexText.matches("اياك نعبد", keywords(2, 255)))
        assertFalse(AyahIndexText.matches("", keywords(1, 5)))
        // البسملة زخرفة السورة لا كلمات آيتها الأولى
        assertFalse(AyahIndexText.matches("بسم الله", keywords(2, 1)))
    }

    @Test
    fun everyAyahHasSearchableText() {
        for (i in 0 until QuranMeta.AYAHS) {
            val k = AyahIndexText.keywords(QuranData.withoutBismillah(clean, i), QuranData.withoutBismillah(uthmani, i))
            val alt = AyahIndexText.alternates(QuranData.withoutBismillah(clean, i), QuranData.withoutBismillah(uthmani, i))
            assertTrue("ayah $i keywords", k.isNotEmpty())
            assertTrue("ayah $i alternates", alt.isNotEmpty())
            // لا تشكيل في الكلمات المفهرسة
            assertTrue("ayah $i has marks", k.none { w -> w.any { it in 'ً'..'ٟ' || it == 'ٰ' } })
        }
    }

    @Test
    fun linksCarryTheirArgument() {
        assertEquals("events", SearchLinks.apply(SearchLinks.event("ramadan")))
        assertEquals("hadith", SearchLinks.apply(SearchLinks.hadith("3062")))
        assertEquals("adhkar", SearchLinks.apply(SearchLinks.adhkar("morning")))
        assertEquals("settings", SearchLinks.apply("settings#theme"))
        assertEquals("surah/2?ayah=255&from=widget", SearchLinks.apply("surah/2?ayah=255&from=widget"))
        com.osatks.mizan.ui.screens.Pending.event = null
        com.osatks.mizan.ui.screens.Pending.hadith = null
        com.osatks.mizan.widget.WidgetLaunch.adhkarSection = null
    }
}
