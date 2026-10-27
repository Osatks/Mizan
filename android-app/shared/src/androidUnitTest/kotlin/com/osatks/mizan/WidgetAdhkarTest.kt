package com.osatks.mizan

import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.plusSeconds
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.widget.AdhkarClock
import com.osatks.mizan.widget.AdhkarWindow
import com.osatks.mizan.widget.WidgetData
import com.osatks.mizan.widget.WidgetText
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** أداة «أذكار الوقت»: الذكر المناسب لكل وقت من مواقيت الصلاة، والجمعة ورمضان وعرفة، والتدوير */
class WidgetAdhkarTest {
    init {
        TestPacks.load()
        Adhkar.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Adhkar.clearTranslations()
    }

    // الفجر ٥:٠٠ الشروق ٦:١٥ الظهر ١٢:٠٠ العصر ١٥:١٥ المغرب ١٧:٤٥ العشاء ١٩:١٥
    private val fixed = PrayerTimes(5.0, 6.25, 12.0, 15.25, 17.75, 19.25)
    private val times: (LocalDate) -> PrayerTimes = { fixed }
    private val noHijri: (LocalDate) -> HDate? = { HDate(1448, 5, 10) }

    private fun t(d: String, hm: String) = LocalDateTime.parse("${d}T$hm")
    private fun at(d: String, hm: String, hijri: (LocalDate) -> HDate? = noHijri) = AdhkarClock.at(t(d, hm), times, hijri)

    private val TUE = "2026-10-27"
    private val THU = "2026-10-29"
    private val FRI = "2026-10-30"

    @Test
    fun ordinaryDay() {
        fun kind(hm: String) = at(TUE, hm).kind
        assertEquals("adhan", kind("05:03"))
        assertEquals("fajr", at(TUE, "05:03").prayer)
        assertEquals("afterPrayer", kind("05:15"))
        assertEquals("morning", kind("05:45"))
        assertEquals("morning", kind("09:00"))      // منتصف الضحى ٩:٠٧
        assertEquals("dhikr", kind("09:30"))
        assertEquals("adhan", kind("12:05"))
        assertEquals("afterPrayer", kind("12:20"))
        assertEquals("dhikr", kind("13:00"))
        assertEquals("afterPrayer", kind("15:25"))
        assertEquals("evening", kind("16:00"))
        assertEquals("adhan", kind("17:50"))
        assertEquals("evening", kind("18:40"))
        assertEquals("afterPrayer", kind("19:30"))
        assertEquals("night", kind("20:00"))
        assertEquals("night", kind("23:59"))
        // بعد منتصف الليل: ليلة اليوم السابق حتى الثلث الأخير (١:١٥) ثم الاستغفار حتى الفجر
        assertEquals("night", at("2026-10-28", "00:30").kind)
        assertEquals("lastThird", at("2026-10-28", "01:30").kind)
        assertEquals("lastThird", at("2026-10-28", "04:59").kind)
        assertEquals("adhan", at("2026-10-28", "05:00").kind)
        assertEquals("morning", at(TUE, "07:00").section)
        assertEquals("sleep", at(TUE, "21:00").section)
        assertNull(at(TUE, "10:00").tag)
    }

    @Test
    fun windowsAreContiguous() {
        val day = LocalDate.parse(TUE)
        val ws = AdhkarClock.day(day, times, noHijri)
        assertEquals(t(TUE, "05:00"), ws.first().start)
        assertEquals(t("2026-10-28", "05:00"), ws.last().end)
        ws.zipWithNext().forEach { (a, b) -> assertEquals("${a.kind} → ${b.kind}", a.end, b.start) }
        assertTrue(ws.all { it.end > it.start })
        // أسبوع كامل متصل أيضاً
        val week = AdhkarClock.range(day, 7, times, noHijri)
        week.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }

    @Test
    fun friday() {
        val w = at(FRI, "10:00")
        assertEquals("friday", w.kind)
        assertEquals(WidgetText.KAHF, w.items.first())
        assertTrue(w.items.contains("tsb14"))
        assertEquals("friday", at(FRI, "13:00").kind)
        // آخر ساعة بعد العصر: ساعة الإجابة ثم أذكار المساء
        val asr = at(FRI, "16:30")
        assertEquals("evening", asr.kind)
        assertEquals(WidgetText.HOUR, asr.items.first())
        assertEquals("friday", asr.tag)
        // بعد المغرب انتهت الجمعة
        val after = at(FRI, "18:40")
        assertEquals("evening", after.kind)
        assertFalse(after.items.contains(WidgetText.HOUR))
        assertNull(after.tag)
        assertEquals("dhikr", at("2026-10-31", "10:00").kind)
    }

    @Test
    fun fridayEve() {
        val w = at(THU, "21:00")
        assertEquals("night", w.kind)
        assertEquals("fridayEve", w.tag)
        assertEquals(WidgetText.SALAWAT, w.items.first())
        assertTrue(w.items.contains(WidgetText.KAHF))
        assertNull(at("2026-10-28", "21:00").tag)
    }

    @Test
    fun ramadanAndArafah() {
        val ramadan: (LocalDate) -> HDate? = { HDate(1448, 9, 25) }
        val iftar = at(TUE, "17:50", ramadan)
        assertEquals("iftar", iftar.kind)
        assertEquals("ram02", iftar.items.first())
        assertEquals("ramadan", iftar.section)
        val night = at(TUE, "21:00", ramadan)
        assertEquals("lastTen", night.tag)
        assertEquals("ram07", night.items.first())
        assertEquals("ram07", at("2026-10-28", "02:00", ramadan).items.first())
        // أول رمضان: إفطار بلا دعاء ليلة القدر
        val early: (LocalDate) -> HDate? = { HDate(1448, 9, 3) }
        assertFalse(at(TUE, "21:00", early).items.contains("ram07"))
        assertEquals("adhan", at(TUE, "12:05", ramadan).kind)

        val arafah: (LocalDate) -> HDate? = { HDate(1448, 12, 9) }
        assertEquals("arafah", at(TUE, "10:00", arafah).kind)
        assertEquals("haj06", at(TUE, "10:00", arafah).items.first())
        assertEquals("hajj", at(TUE, "10:00", arafah).section)
    }

    @Test
    fun highLatitudesStayContiguous() {
        // العشاء والفجر لا يُحسبان في صيف الشمال
        val north: (LocalDate) -> PrayerTimes = { PrayerTimes(Double.NaN, 3.2, 13.1, 17.6, 22.9, Double.NaN) }
        val ws = AdhkarClock.day(LocalDate.parse("2026-06-21"), north, noHijri)
        assertTrue(ws.isNotEmpty())
        ws.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
        assertTrue(ws.all { it.end > it.start })
        // كل لحظة من اليوم لها نافذة
        for (h in 0..23) AdhkarClock.at(LocalDateTime(2026, 6, 21, h, 30), north, noHijri)
        // ومواقيت حقيقية لمدينة في أقصى الشمال
        val real: (LocalDate) -> PrayerTimes = { PrayTimes.compute(it, 69.65, 18.96, "Europe/Oslo", "mwl", false) }
        for (h in 0..23) {
            val w = AdhkarClock.at(LocalDateTime(2026, 6, 21, h, 10), real, noHijri)
            assertTrue(w.end > w.start)
        }
    }

    @Test
    fun rotation() {
        val w = at(TUE, "06:00")
        val start = w.start
        assertEquals(0, AdhkarClock.index(start, w, 0, 5))
        val minute = start.plusSeconds(60)   // دقيقة بعد بدايتها: خطوتان (كل نصف دقيقة)
        assertEquals(2, AdhkarClock.index(minute, w, 0, 5))
        assertEquals(3, AdhkarClock.index(minute, w, 1, 5))
        assertEquals(1, AdhkarClock.index(minute, w, -1, 5))
        assertEquals(0, AdhkarClock.index(minute, w, 0, 0))
        assertEquals(1, AdhkarClock.index(start.plusSeconds(59), w, 0, 5))
        assertTrue(AdhkarClock.index(t(TUE, "06:30"), w, -13, 5) in 0..4)
    }

    /** كل ذكر تذكره القواعد موجود في المكتبة، ولكل لغة نص ومصدر */
    @Test
    fun everyListResolves() {
        val lists = with(AdhkarClock) { listOf(ADHAN, IFTAR, AFTER, MORNING, EVENING, DHIKR, FRIDAY, ARAFAH, NIGHT, LAST_THIRD) } +
            listOf(listOf("ram07", WidgetText.HOUR))
        for (l in lists) for (tok in l) {
            val got = WidgetData.expand(listOf(tok))
            assertTrue("missing $tok", got.isNotEmpty())
        }
        assertEquals(Adhkar.section("morning")!!.items.size, WidgetData.expand(AdhkarClock.MORNING).size)
        val w = AdhkarWindow("friday", t(FRI, "09:00"), t(FRI, "12:00"), "tasbih", AdhkarClock.FRIDAY)
        for (lang in Lang.entries) {
            val ds = WidgetData.dhikrs(w, lang)
            assertEquals(AdhkarClock.FRIDAY.size, ds.size)
            for (d in ds) {
                assertTrue("${lang.code} ${d.id}: text", d.ar.isNotBlank())
                assertTrue("${lang.code} ${d.id}: source", d.source.isNotBlank())
                if (lang != Lang.AR) assertFalse("${lang.code} ${d.id}: translation", d.tr.isNullOrBlank())
            }
            assertNotNull(WidgetText.of(lang.code))
        }
        // المصدر يُكتب بأسماء الكتب بلغة الواجهة
        val kahf = WidgetData.dhikrs(w, Lang.AR).first()
        assertTrue(kahf.source, kahf.source.contains("المستدرك"))
        assertEquals(WidgetText.AR.kahfCta, kahf.fadl)
    }

    @Test
    fun widgetTextHasEveryLanguage() {
        for (lang in Lang.entries) {
            val wt = WidgetText.of(lang.code)
            if (lang != Lang.EN) assertTrue("${lang.code} falls back to English", wt !== WidgetText.EN)
            assertTrue(wt.untilTime("5:42").contains("5:42"))
            if (lang != Lang.AR) assertFalse(lang.code, wt.kahf.isNullOrBlank() || wt.salawat.isNullOrBlank() || wt.hour.isNullOrBlank())
        }
    }
}
