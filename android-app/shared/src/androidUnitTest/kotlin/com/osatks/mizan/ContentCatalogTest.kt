package com.osatks.mizan

import com.osatks.mizan.content.ContentCatalog
import com.osatks.mizan.content.ContentKind
import com.osatks.mizan.content.ContentStore
import com.osatks.mizan.content.Sha256
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.pack.LangPack
import com.osatks.mizan.quran.Sources
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * المحتوى الذي ينزّله التطبيق من موقع ميزان بدل تضمينه: كل عنصر في فهرس التطبيق (ContentCatalog) منشور في
 * dist/content/<النوع>/index.json بالبصمة والحجم والإصدار نفسها (وإلا رفض التطبيق ما ينزّله)، والملف المنشور يطابقها،
 * وما في assets هو المضمّن وحده (فلا يعود الحجم يكبر دون قصد)، وكل مصدر غير مضمّن له ملف يُنزَّل.
 */
class ContentCatalogTest {
    private val assets = File("src/commonMain/assets")
    private val dist = File("../../dist/content")
    private val site = File("../../src/content")

    private fun sha(f: File) = MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

    @After
    fun reset() = ContentStore.initForTest(null)

    @Test
    fun everyItemIsPublishedWithItsHash() {
        assertTrue("no catalog", ContentCatalog.ALL.isNotEmpty())
        assertEquals("duplicate keys", ContentCatalog.ALL.size, ContentCatalog.ALL.map { it.key }.toSet().size)
        for (kind in ContentKind.entries) {
            @Suppress("UNCHECKED_CAST")
            val index = (Json.obj(File(dist, "${kind.dir}/index.json").readText())["items"] as List<Map<String, Any?>>)
                .associateBy { it["id"] as String }
            for (item in ContentCatalog.of(kind)) {
                val e = index[item.id]
                assertNotNull("${item.key} missing from dist/content/${kind.dir}/index.json (run build.py)", e)
                assertEquals("${item.key} sha256", item.sha256, e!!["sha256"])
                assertEquals("${item.key} bytes", item.bytes, (e["bytes"] as Number).toLong())
                assertEquals("${item.key} version", item.version, (e["version"] as Number).toInt())
                assertEquals("${item.key} language", item.lang, e["language"])
                assertEquals("${item.key} file", item.file, e["file"])
                val published = File(dist, item.path)
                assertTrue("${item.key}: $published", published.isFile)
                assertEquals("${item.key}: published file", item.sha256, sha(published))
                assertEquals("${item.key}: src/content file", item.sha256, sha(File(site, item.path)))
                // ما يُنزَّل لا يبقى مضمّناً، والمضمّن لا يُنزَّل
                assertFalse("${item.key} is still bundled", File(assets, item.path).exists())
                assertFalse("${item.key} is listed as bundled", item.key in ContentCatalog.BUNDLED)
            }
            // كل ما في الفهرس المنشور معروف للتطبيق
            assertEquals("${kind.dir}: index and catalog differ", index.keys, ContentCatalog.of(kind).map { it.id }.toSet())
        }
    }

    @Test
    fun onlyTheBundledContentIsInAssets() {
        for (key in ContentCatalog.BUNDLED) {
            val kind = ContentKind.entries.first { key.startsWith(it.dir + "/") }
            assertTrue("$key not in assets", File(assets, "$key.${kind.ext}").isFile)
        }
        for (kind in ContentKind.entries) {
            val inAssets = File(assets, kind.dir).list().orEmpty().filter { it.endsWith("." + kind.ext) }
                .map { "${kind.dir}/${it.removeSuffix("." + kind.ext)}" }.toSet()
            assertEquals("${kind.dir}: assets hold only the bundled files", ContentCatalog.BUNDLED.filter { it.startsWith(kind.dir + "/") }.toSet(), inAssets)
        }
        // الأذان: المضمّن فؤاد وحده (أذانه وأذان فجره)، والبقية من الموقع
        assertEquals(setOf("fouad", "fouad_fajr"), Muezzins.ALL.filter { it.bundled }.map { it.id }.toSet())
    }

    @Test
    fun everySourceHasAFile() {
        val packs = File(assets, "lang").listFiles().orEmpty().filter { it.name.endsWith(".json") }.map { LangPack.parse(it.readText()) }
        val ids = Sources.ALL.map { it.id } + packs.mapNotNull { it.quran?.id }
        for (id in ids) {
            val bundled = "tafsir/$id" in ContentCatalog.BUNDLED
            assertTrue("$id: neither bundled nor downloadable", bundled || ContentCatalog.tafsir(id) != null)
        }
        // لكل لغة مدمجة ما يكمل محتواها: ترجمة المعاني والأحاديث والأذكار مضمّنة أو قابلة للتنزيل
        for (l in Lang.BUILT_IN.filter { it != Lang.AR }) {
            for (item in ContentStore.essentials(l)) assertTrue("${l.code}: ${item.key}", item in ContentCatalog.ALL)
            assertTrue("${l.code} hadith", "hadith/${l.code}" in ContentCatalog.BUNDLED || ContentCatalog.hadith(l.code) != null)
            assertTrue("${l.code} adhkar", "adhkar/${l.code}" in ContentCatalog.BUNDLED || ContentCatalog.adhkar(l.code) != null)
        }
        assertEquals(listOf("tafsir/fr.hamidullah", "hadith/fr", "adhkar/fr"), ContentStore.essentials(Lang.FR).map { it.key })
        assertTrue(ContentStore.essentials(Lang.EN).isEmpty())
        assertTrue(ContentStore.essentials(Lang.AR).isEmpty())
        assertEquals(listOf("hadith/ms", "hadith/id", "adhkar/ms", "tafsir/ms.basmeih").sorted(), ContentStore.essentials(Lang.MS).map { it.key }.sorted())
    }

    @Test
    fun downloadedFilesAreFoundAfterTheBundle() {
        val dir = Files.createTempDirectory("mizan-content").toFile()
        try {
            ContentStore.initForTest(dir.absolutePath)
            val fr = ContentCatalog.tafsir("fr.hamidullah")!!
            assertFalse(ContentStore.hasTafsir("fr.hamidullah"))
            assertTrue(ContentStore.hasTafsir("ar.muyassar"))
            assertNull(ContentStore.read(fr.path))
            assertFalse(Sources.installed().any { it.id == "fr.hamidullah" })
            assertTrue(Sources.downloadable().any { it.first.id == "fr.hamidullah" })
            assertEquals(fr, ContentStore.missing(Lang.FR, ContentKind.TAFSIR))

            File(dir, fr.path).apply { parentFile.mkdirs() }.writeBytes(File(site, fr.path).readBytes())
            ContentStore.initForTest(dir.absolutePath)
            assertTrue(ContentStore.hasTafsir("fr.hamidullah"))
            assertTrue(ContentStore.isDownloaded(fr))
            assertNotNull(ContentStore.read(fr.path))
            assertTrue(Sources.installed().any { it.id == "fr.hamidullah" })
            assertNull(ContentStore.missing(Lang.FR, ContentKind.TAFSIR))
            assertEquals(fr.bytes, ContentStore.downloadedBytes())
            // ما ليس في الفهرس لا يُقرأ من مجلد المحتوى
            assertNull(ContentStore.read("quran/uthmani.txt"))
        } finally {
            ContentStore.initForTest(null)
            dir.deleteRecursively()
        }
    }

    @Test
    fun sha256MatchesTheJdk() {
        val r = java.util.Random(7)
        for (n in listOf(0, 1, 55, 56, 63, 64, 65, 119, 120, 1000, 100_003)) {
            val b = ByteArray(n).also { r.nextBytes(it) }
            val jdk = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
            assertEquals("n=$n", jdk, Sha256.hex(b))
        }
    }
}
