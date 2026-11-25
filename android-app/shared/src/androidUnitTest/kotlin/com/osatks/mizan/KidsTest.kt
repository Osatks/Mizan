package com.osatks.mizan

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.core.Kids
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.quran.KidsRecite
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.ui.screens.kidsDuas
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** ركن الأطفال: السور، وأن كل اسم من الأسماء الحسنى موجود في آيته، وأن الأدعية من الأذكار ومترجمة، وخطوات «ردّد معي» */
class KidsTest {
    init {
        TestPacks.load()
        QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
    }

    /** بلا تشكيل ولا همزات على الألف (النص المبسّط في clean.txt بلا تشكيل) */
    private val marks = Regex("[ً-ٰٟۖ-ۭ]")
    private fun norm(t: String) = marks.replace(t, "").replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')

    @Test
    fun surahs() {
        assertEquals(1, Kids.SURAHS.first())
        assertEquals((114 downTo 93).toList(), Kids.SURAHS.drop(1))
        assertEquals(Kids.SURAHS.size, Kids.SURAHS.toSet().size)
    }

    @Test
    fun namesAppearInTheirAyahs(): Unit = runBlocking {
        val clean = QuranData.clean()
        assertTrue(Kids.NAMES.size >= 40)
        assertEquals("names are unique", Kids.NAMES.size, Kids.NAMES.map { it.ar }.toSet().size)
        for (n in Kids.NAMES) {
            assertTrue("${n.latin}: ${n.surah}:${n.ayah}", n.surah in 1..QuranMeta.SURAHS && n.ayah in 1..QuranMeta.COUNTS[n.surah - 1])
            val name = norm(n.ar).let { if (it.startsWith("ال")) it.drop(2) else it }
            val ayah = norm(clean[QuranMeta.index(n.surah, n.ayah)])
            assertTrue("${n.latin} not in ${n.surah}:${n.ayah} ($ayah)", ayah.contains(name))
            assertTrue(n.latin, n.latin.isNotBlank())
        }
    }

    @Test
    fun duasComeFromAdhkar() {
        for (id in Kids.DUAS) assertNotNull(id, Adhkar.byId(id))
        for (l in Lang.entries) {
            val duas = kidsDuas(l)
            assertEquals(l.code, Kids.DUAS, duas.map { it.id })
            for (d in duas) {
                assertFalse("${l.code} ${d.id} title", d.title.isBlank())
                assertFalse(d.id, d.ar.isBlank())
                if (l != Lang.AR) assertFalse("${l.code} ${d.id} translation", d.tr.isNullOrBlank())
            }
        }
    }

    @Test
    fun repeatAfterMeSteps() {
        // الفاتحة: البسملة آيتها الأولى
        val fatiha = KidsRecite.stepsFor(1)
        assertEquals(7, fatiha.size)
        assertTrue(fatiha.none { it.basmala })
        // الناس: البسملة ثم ست آيات
        val nas = KidsRecite.stepsFor(114)
        assertEquals(7, nas.size)
        assertTrue(nas.first().basmala)
        assertEquals(QuranMeta.index(114, 1), nas[1].index)
        assertEquals(QuranMeta.index(114, 6), nas.last().index)
        assertTrue(Reciters.ALL.any { it.id == Kids.DEFAULT_RECITER })
        assertEquals(listOf(1, 2, 3), Kids.REPEATS)
    }
}
