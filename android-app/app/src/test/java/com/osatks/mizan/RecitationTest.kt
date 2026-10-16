package com.osatks.mizan

import androidx.compose.ui.graphics.isSpecified
import com.osatks.mizan.quran.Clip
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.quran.recitationQueue
import com.osatks.mizan.quran.recitationUrl
import com.osatks.mizan.ui.quran.readingPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** قائمة التلاوة وروابطها، وألوان القراءة الليلية */
class RecitationTest {
    @Test
    fun surahStartsWithBasmalaExceptAlFatihaAndAtTawbah() {
        val baqarah = recitationQueue(QuranMeta.index(2, 1), 1)
        assertEquals(Clip(QuranMeta.index(2, 1), basmala = true), baqarah.first())
        assertEquals(Clip(QuranMeta.index(2, 1)), baqarah[1])
        assertEquals(Clip(QuranMeta.index(2, 286)), baqarah.last())
        assertEquals(286 + 1, baqarah.size)
        assertEquals(1, baqarah.count { it.basmala })

        // الفاتحة: البسملة آيتها الأولى فلا تُكرر، والتوبة بلا بسملة
        val fatiha = recitationQueue(0, 1)
        assertEquals(7, fatiha.size)
        assertTrue(fatiha.none { it.basmala })
        assertTrue(recitationQueue(QuranMeta.index(9, 1), 1).none { it.basmala })

        // البدء من وسط السورة: بلا بسملة
        assertTrue(recitationQueue(QuranMeta.index(18, 10), 1).none { it.basmala })
    }

    @Test
    fun eachAyahRepeatsButTheBasmalaDoesNot() {
        val q = recitationQueue(QuranMeta.index(112, 1), 3)
        assertEquals(1 + 4 * 3, q.size)
        assertEquals(listOf(true, false, false, false), q.take(4).map { it.basmala })
        assertEquals(List(3) { Clip(QuranMeta.index(112, 4)) }, q.takeLast(3))
        // «بلا نهاية»: كل آية مرة واحدة ويكررها المشغّل
        assertEquals(1 + 4, recitationQueue(QuranMeta.index(112, 1), 0).size)
        // آخر القرآن
        assertEquals(listOf(Clip(6235)), recitationQueue(6235, 1))
    }

    @Test
    fun clipIdsRoundTrip() {
        for (c in listOf(Clip(0), Clip(QuranMeta.index(2, 1), true), Clip(6235))) assertEquals(c, Clip.parse(c.id))
    }

    @Test
    fun urlsUseThreeDigitNumbersInEveryLocale() {
        val saved = Locale.getDefault()
        try {
            for (l in listOf(Locale.US, Locale("ar"), Locale("fa"), Locale("bn"))) {
                Locale.setDefault(l)
                assertEquals("https://everyayah.com/data/Alafasy_128kbps/002255.mp3", recitationUrl("Alafasy_128kbps", Clip(QuranMeta.index(2, 255))))
                assertEquals("https://everyayah.com/data/Husary_128kbps/001001.mp3", recitationUrl("Husary_128kbps", Clip(QuranMeta.index(36, 1), true)))
                assertEquals("https://everyayah.com/data/Husary_128kbps/114006.mp3", recitationUrl("Husary_128kbps", Clip(6235)))
            }
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun recitersHaveUniqueFoldersAndADefault() {
        assertEquals(Reciters.ALL.size, Reciters.ALL.map { it.id }.toSet().size)
        assertEquals(Reciters.ALL[0], Reciters.byId(""))
        assertEquals("Husary_128kbps", Reciters.byId("Husary_128kbps").id)
        assertTrue(Reciters.ALL.all { it.ar.isNotBlank() && it.en.isNotBlank() })
    }

    @Test
    fun readingTones() {
        assertNull(readingPalette(""))
        val night = readingPalette("night")!!
        assertTrue(night.backdrop.isSpecified)
        assertFalse(readingPalette("sepia")!!.backdrop.isSpecified)
    }
}
