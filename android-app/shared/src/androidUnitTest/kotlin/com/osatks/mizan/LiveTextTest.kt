package com.osatks.mizan

import com.osatks.mizan.adhkar.AdhkarLive
import com.osatks.mizan.widget.LiveText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** نصوص الأنشطة الحية والأدوات الجديدة: كل لغات التطبيق، بلا نص فارغ، و %s في «أتممت %s» */
class LiveTextTest {
    @Test fun everyLanguageIsComplete() {
        assertEquals(setOf("ar", "en", "fr", "tr", "ur", "id", "ms", "fa", "bn", "ru", "de", "es", "uk", "uz", "kk"), LiveText.all.keys)
        for ((code, t) in LiveText.all) {
            val all = listOf(t.end, t.iqamaIn, t.iftar, t.suhoor, t.iftarNow, t.adhkarDone, t.tasbih, t.descTasbih, t.wird, t.descWird, t.showLive)
            all.forEach { assertTrue("$code: empty text", it.isNotBlank()) }
            assertTrue("$code: adhkarDone needs %s", "%s" in t.adhkarDone)
            assertTrue(t.adhkarDone("X").contains("X"))
        }
    }

    /** نصوص المشاركة صورة والتقويم والقبلة بالكاميرا: كل لغات التطبيق، بلا نص فارغ */
    @Test fun featureTextIsComplete() {
        assertEquals(LiveText.all.keys, com.osatks.mizan.i18n.FeatureText.ALL.keys)
        for ((code, t) in com.osatks.mizan.i18n.FeatureText.ALL) {
            listOf(
                t.shareAyah, t.image, t.text, t.withMeaning, t.addToCalendar, t.calendarDesc, t.calendarAdded,
                t.removeFromCalendar, t.calendarRemoved, t.calendarDenied, t.calendarName, t.arQibla, t.arHint,
                t.arFacing, t.arTurn, t.cameraDenied, t.arUnsupported,
            ).forEach { assertTrue("$code: empty text", it.isNotBlank()) }
        }
    }

    @Test fun clipCutsAtAWord() {
        val long = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ".repeat(20)
        val c = AdhkarLive.clip(long, 60)
        assertTrue(c.length <= 62)
        assertTrue(c.endsWith(" …"))
        assertEquals("قصير", AdhkarLive.clip("قصير", 60))
        assertEquals("سطر واحد", AdhkarLive.clip("سطر\nواحد", 60))
    }
}
