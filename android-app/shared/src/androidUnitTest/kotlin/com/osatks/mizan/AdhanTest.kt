package com.osatks.mizan

import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.LangPack
import com.osatks.mizan.i18n.pack.PackStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** تسجيلات الأذان وملفاتها، وأنماط الاهتزاز، وأسماؤها في كل اللغات */
class AdhanTest {
    private val assets = File("src/commonMain/assets/adhan")
    private val iosSounds = File("../../iosApp/Sounds")
    private val site = File("../../src/adhan")

    @Test fun catalogue() {
        val all = Muezzins.ALL
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(Muezzins.DEFAULT.bundled && !Muezzins.DEFAULT.fajr)
        assertTrue(Muezzins.FAJR_DEFAULT.bundled && Muezzins.FAJR_DEFAULT.fajr)
        for (m in all) {
            assertTrue(m.id, m.id.matches(Regex("[a-z_]+")))
            assertTrue("${m.id} credit", m.credit.isNotBlank())
            val audio = File(if (m.bundled) assets else site, m.file)
            val sound = File(if (m.bundled) iosSounds else site, m.iosSound)
            assertTrue("missing $audio", audio.isFile && audio.length() > 100_000)
            assertTrue("missing $sound", sound.isFile)
            // ثانية واحدة من IMA4 أحادي 22.05 kHz نحو 11.6 KB: صوت الإشعار أقل من 30 ثانية
            assertTrue("${m.iosSound} is ${sound.length()} bytes (≥ 30 s?)", sound.length() < 29 * 11_700)
            // القاعدة لكل أذان: صوت الإشعار أول تكبيرتين ثم التلاشي، فطوله نهايتهما والتلاشي (IMA4: نحو 11.73 KB/s)
            assertTrue("${m.id}: takbir ${m.takbir}", m.takbir in 3.0..26.0)
            val secs = sound.length() / 11_730.0
            assertEquals("${m.iosSound}: takbirs + fade (run make_adhan.py --notify-only)", m.takbir + Muezzin.FADE, secs, 0.6)
            assertTrue("${m.id}: seconds", m.seconds in 60..400)
            assertEquals("${m.id}: sizeKb", audio.length() / 1000.0, m.sizeKb.toDouble(), 2.0)
        }
        // ما في assets كله مذكور في القائمة (لا ملفات يتيمة تكبّر التطبيق)
        val listed = all.filter { it.bundled }.map { it.file }.toSet()
        assertEquals(listed, assets.list()!!.toSet())
        assertTrue(File(iosSounds, "mizan_vibrate.caf").isFile)
    }

    @Test fun patterns() {
        for (p in VibePattern.entries) {
            assertEquals(p.key, p.timings.size, p.amps.size)
            assertTrue(p.key, p.amps.all { it in 0..255 })
            assertTrue(p.key, p.timings.all { it >= 0 } && p.millis in 300..3000)
            // الانتظار بلا شدة، والاهتزاز بشدة (أنماط القنوات في أندرويد تتجاهل الشدة وتعتمد على التناوب)
            p.timings.indices.forEach { i -> if (i % 2 == 0) assertEquals(p.key, 0, p.amps[i]) else assertTrue(p.key, p.amps[i] > 0) }
            // المقاطع المهتزة بترتيبها الزمني ومدتها كاملة
            assertEquals(p.key, p.timings.filterIndexed { i, _ -> i % 2 == 1 }.sum(), p.pulses.sumOf { it.second })
            assertTrue(p.key, p.pulses.zipWithNext().all { (a, b) -> a.first + a.second <= b.first })
            assertTrue(p.key, p.pulses.last().let { it.first + it.second } == p.millis)
        }
        // عشرة أنماط على الأقل، كلها مختلفة، ومفاتيحها ثابتة (تُحفظ في الإعدادات)
        assertTrue(VibePattern.entries.size >= 10)
        assertEquals(VibePattern.entries.size, VibePattern.entries.map { it.timings to it.amps }.toSet().size)
        assertEquals(VibePattern.entries.size, VibePattern.entries.map { it.key }.toSet().size)
        assertEquals(VibePattern.HEARTBEAT, VibePattern.of("heartbeat"))
        assertEquals(VibePattern.DEFAULT, VibePattern.of("gone"))
        assertEquals(AlertMode.ADHAN, AlertMode.of(null))
        assertEquals(AlertMode.OFF, AlertMode.of("off"))
    }

    @Test fun namesInEveryLanguage() {
        val packs = File("src/commonMain/assets/lang").listFiles()!!.filter { it.name.endsWith(".json") }
            .map { PackStrings(LangPack.parse(it.readText())) }
        for (s in Lang.BUILT_IN.map { it.strings } + packs) {
            for (m in Muezzins.ALL) assertTrue("${s.channelName}: ${m.id}", !s.muezzinNames[m.id].isNullOrBlank())
            assertEquals(VibePattern.entries.map { it.key }.toSet(), s.vibeNames.keys)
        }
    }
}
