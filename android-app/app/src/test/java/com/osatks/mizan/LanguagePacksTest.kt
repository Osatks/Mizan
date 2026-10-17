package com.osatks.mizan

import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.i18n.pack.Translit
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.TafsirLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** يحمّل الحزم المضمّنة في الاختبارات (Lang.entries تشملها بعدها) */
object TestPacks {
    val files: File by lazy { Files.createTempDirectory("mizan-files").toFile() }
    fun load() = LanguagePacks.initForTest(File("src/main/assets/lang"), files)
}

class LanguagePacksTest {
    @Test
    fun bundledPacksRegister() {
        TestPacks.load()
        val uz = Lang.of("uz")!!
        val kk = Lang.of("kk")!!
        assertTrue(uz.isPack && kk.isPack)
        assertEquals("Oʻzbekcha", uz.native)
        assertEquals("Қазақша", kk.native)
        assertEquals(',', kk.decimal)
        assertEquals("uz.sodik", Sources.translationFor(uz)!!.id)
        assertEquals("kk.altai", Sources.translationFor(kk)!!.id)
        assertNotNull(TafsirLibrary.bySlug("uzbek-mokhtasar"))
        assertEquals("uz-latn", TafsirLibrary.bySlug("uzbek-mokhtasar")!!.translit)
        // تفاسير الروسية تأتي بعد لغة الكازاخية ثم العربية
        val order = Sources.ordered(kk).map { it.lang.code }
        assertEquals("kk", order.first())
        assertTrue(order.indexOf("ru") < order.indexOf("ar"))
        // الإنجليزية لأي نص ناقص، والأدلة مترجمة
        assertEquals("Мизан", kk.strings.appName)
        assertFalse(com.osatks.mizan.i18n.Evidence.zakatMoney[0].title[uz] == com.osatks.mizan.i18n.Evidence.zakatMoney[0].title.en)
    }

    @Test
    fun uzbekTransliteration() {
        assertEquals("Oʻzbekiston", Translit.uzLatn("Ўзбекистон"))
        assertEquals("Qurʼon", Translit.uzLatn("Қуръон"))
        assertEquals("Mehribon va rahmli Alloh", Translit.uzLatn("Меҳрибон ва раҳмли Аллоҳ"))
        assertEquals("yer, Yordam, shoir, gʻoyat, SHAHAR", Translit.uzLatn("ер, Ёрдам, шоир, ғоят, ШАҲАР"))
        assertEquals("sirk, mustaqil, oyat 2:255", Translit.uzLatn("цирк, мустақил, оят 2:255"))
    }

    /** الفهرس، والتنزيل مع ترجمة المعاني، والتحديث، والحذف — من مجلد محلي بدل الموقع */
    @Test
    fun downloadUpdateDelete() = runBlocking {
        TestPacks.load()
        val site = Files.createTempDirectory("mizan-site").toFile()
        @Suppress("UNCHECKED_CAST")
        val kk = Json.obj(File("src/main/assets/lang/kk.json").readText()).toMutableMap()
        kk["version"] = 2L
        kk["quran"] = mapOf("id" to "kk.test", "author" to "Test", "file" to "kk.test.txt")
        File(site, "kk.json").writeText(Json.write(kk))
        File(site, "kk.test.txt").writeText("1|1|Тест\n")
        File(site, "index.json").writeText(Json.write(mapOf("format" to 1L, "packs" to listOf(
            mapOf("code" to "kk", "native" to "Қазақша", "english" to "Kazakh", "version" to 2L, "sizeMb" to 0.1, "file" to "kk.json"),
            mapOf("code" to "xx", "native" to "Future", "version" to 1L, "format" to 99L, "file" to "xx.json"),
            mapOf("code" to "en", "native" to "English", "version" to 9L, "file" to "en.json"),
        ))))
        val base = site.toURI().toString()
        LanguagePacks.refresh(base)
        val offers = LanguagePacks.offers()
        assertEquals(listOf("kk"), offers.map { it.code }) // الصيغة الأحدث والمدمجة لا تُعرض
        assertTrue(LanguagePacks.download(offers[0], base))
        assertEquals(2, Lang.of("kk")!!.pack!!.version)
        assertEquals("kk.test", Sources.translationFor(Lang.of("kk")!!)!!.id)
        assertTrue(File(TestPacks.files, "lang/tafsir/kk.test.txt").exists())
        assertTrue(LanguagePacks.offers().isEmpty())
        LanguagePacks.delete("kk")
        assertEquals(1, Lang.of("kk")!!.pack!!.version) // تعود الحزمة المضمّنة
        assertFalse(File(TestPacks.files, "lang/tafsir/kk.test.txt").exists())
        // فشل التنزيل لا يثبت شيئاً
        File(site, "kk.test.txt").delete()
        assertFalse(LanguagePacks.download(LanguagePacks.offers()[0], base))
        assertEquals(1, Lang.of("kk")!!.pack!!.version)
    }
}
