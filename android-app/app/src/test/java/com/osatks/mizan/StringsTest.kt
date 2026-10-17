package com.osatks.mizan

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.ShareType
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** كل لغة يجب أن تغطي كل المفاتيح وأن تعمل كل دوالها (الجمع، التواريخ، الملاحظات) دون أخطاء */
class StringsTest {
    init { TestPacks.load() }

    private val notes = listOf(
        InhNote.MatSiblingsShare, InhNote.Umariyya, InhNote.GrandmothersShare, InhNote.FullSistersGhayr,
        InhNote.Akdariyya, InhNote.JaddRule(true), InhNote.JaddRule(false), InhNote.Muadda,
        InhNote.NothingLeft("fb"), InhNote.NothingLeft("gf"), InhNote.Mushtaraka, InhNote.Awl(6, 7),
        InhNote.Radd, InhNote.SpouseOnlyRadd, InhNote.NoHeirs,
    )

    @Test
    fun mapsHaveEveryKey() {
        for (l in Lang.entries) {
            val s = l.strings
            fun check(name: String, keys: Set<String>, got: Set<String>) =
                assertEquals("${l.code}.$name", keys.sorted(), got.sorted())
            check("tools", Ar.tools.keys, s.tools.keys)
            check("toolKeywords", Ar.toolKeywords.keys, s.toolKeywords.keys)
            check("prayers", PrayTimes.PRAYERS.toSet(), s.prayers.keys)
            check("methods", PrayTimes.METHODS.map { it.key }.toSet(), s.methods.keys)
            check("cities", PrayTimes.CITIES.map { it.key }.toSet(), s.cities.keys)
            check("heirs", Inheritance.HEIRS.map { it.key }.toSet(), s.heirs.keys)
            check("heirGroups", Inheritance.GROUPS.toSet(), s.heirGroups.keys)
            check("blockers", Inheritance.BLOCKERS.toSet(), s.blockers.keys)
            check("events", Ar.events.keys, s.events.keys)
            check("foods", Ar.foods.keys, s.foods.keys)
        }
    }

    @Test
    fun listsHaveRightSize() {
        for (l in Lang.entries) {
            val s = l.strings
            assertEquals(l.code, 12, s.hijriMonths.size)
            assertEquals(l.code, 12, s.gregMonths.size)
            assertEquals(l.code, 7, s.weekdays.size)
            assertEquals(l.code, 7, s.weekdaysShort.size)
            assertEquals(l.code, 8, s.directions.size)
            assertEquals(l.code, 9, s.dhikrLabels.size)
            assertEquals(l.code, 9, s.dhikrMeanings.size)
            assertEquals(l.code, 114, s.surahNames.size)
            assertTrue("${l.code} surah names", s.surahNames.all { it.isNotBlank() } && s.surahNames.toSet().size == 114)
            if (l != Lang.AR) {
                assertTrue("${l.code} meanings", s.dhikrMeanings.all { it.isNotBlank() } && s.tamamMeaning.isNotBlank())
            }
        }
    }

    @Test
    fun functionsProduceText() {
        for (l in Lang.entries) {
            val s = l.strings
            for (n in listOf(0L, 1L, 2L, 3L, 5L, 11L, 21L, 22L, 25L, 100L, 101L, 102L, 111L, 1000L)) {
                listOf(s.inDays(n), s.daysCount(n), s.tashihFrom(n)).forEach { assertTrue("${l.code} $n", it.contains("$n") || n <= 2) }
                val i = n.toInt()
                listOf(s.forPeople(i), s.fidyaSub(i), s.oathSub(i), s.fastDays(i), s.ofGoal(i), s.completedN(i)).forEach { assertFalse(it.isBlank()) }
            }
            for (m in 1..12) {
                assertTrue(s.hijriDate(15, m, 1448).contains("1448"))
                assertTrue(s.gregDate(15, m, 2026).contains("2026"))
                assertTrue(s.hijriMonthTitle(m, 1448).contains("1448"))
                assertTrue(s.monthTableTitle(m, 2026).contains("2026"))
            }
            for (t in ShareType.entries) { assertFalse(s.shareType(t, false).isBlank()); assertFalse(s.shareType(t, true).isBlank()) }
            notes.forEach { assertFalse("${l.code} $it", s.inhNote(it).isBlank()) }
            assertTrue(s.inhNote(InhNote.Awl(6, 7)).let { it.contains("6") && it.contains("7") })
            assertFalse(s.ymd(30, 2, 5).isBlank())
            assertTrue(s.searchNoResults("xyz").contains("xyz"))
            assertTrue(s.turnRight(12).contains("12") && s.turnLeft(12).contains("12"))
            for (n in listOf(1, 2, 3, 7, 11, 20, 100, 286)) assertTrue("${l.code} ayahCount($n)", s.ayahCount(n).contains("$n") || n <= 2)
            assertTrue(s.juzName(30).contains("30") && s.pageName(604).contains("604"))
            assertTrue(s.surahAyah("X", 255).contains("255"))
            assertTrue("${l.code} sources keep the links", s.quranSources.contains("tanzil.net") && s.quranSources.contains("tanzil.net/trans"))
        }
    }

    /** أسماء التبويبات تظهر داخل حبة الشريط السفلي فيجب أن تبقى قصيرة */
    @Test
    fun tabLabelsAreShort() {
        for (l in Lang.entries) {
            val s = l.strings
            listOf(s.tabHome, s.tabPrayer, s.tabQuran, s.tabTasbih, s.tabSettings).forEach {
                assertTrue("${l.code}: \"$it\" is too long for a tab", it.length <= 14)
            }
        }
    }
}
