package com.osatks.mizan.fixtures

import android.content.SharedPreferences
import com.osatks.mizan.TestPacks
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Bits
import com.osatks.mizan.core.Search
import com.osatks.mizan.core.now
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Translit
import com.osatks.mizan.platform.Prefs
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.MadinahLine
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.quran.JUZ_ORDINALS
import com.osatks.mizan.ui.quran.surahName
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Test
import java.io.File

/**
 * يصدّر نتائج منطق القرآن في Kotlin (البيانات الوصفية، والنصوص، والبحث، والختمة، وآية اليوم، ومصحف المدينة)
 * إلى iosApp/MizanCore/Fixtures/quran ليقارن بها CheckQuran نتائج Swift حالةً حالة.
 */
class QuranFixturesExport {
    private val out = File("../../iosApp/MizanCore/Fixtures/quran")

    init {
        TestPacks.load()
        QuranData.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
        Madinah.open = { File("src/commonMain/assets/$it").takeIf { f -> f.exists() }?.readBytes() }
    }

    // ---------- JSON ----------

    private fun json(v: Any?): String = StringBuilder().also { put(it, v) }.toString()

    private fun put(b: StringBuilder, v: Any?) {
        when (v) {
            null -> b.append("null")
            is String -> str(b, v)
            is Boolean -> b.append(v)
            is Int, is Long -> b.append(v)
            is Float -> b.append(v.toDouble())
            is Double -> b.append(v)
            is IntArray -> put(b, v.toList())
            is Map<*, *> -> { b.append('{'); var f = true; for ((k, x) in v) { if (!f) b.append(','); f = false; str(b, k.toString()); b.append(':'); put(b, x) }; b.append('}') }
            is Iterable<*> -> { b.append('['); var f = true; for (x in v) { if (!f) b.append(','); f = false; put(b, x) }; b.append(']') }
            is Array<*> -> put(b, v.toList())
            is Pair<*, *> -> put(b, listOf(v.first, v.second))
            else -> error("json: ${v::class}")
        }
    }

    private fun str(b: StringBuilder, s: String) {
        b.append('"')
        for (c in s) when {
            c == '"' -> b.append("\\\"")
            c == '\\' -> b.append("\\\\")
            c < ' ' -> b.append("\\u%04x".format(c.code))
            else -> b.append(c)
        }
        b.append('"')
    }

    private fun write(name: String, v: Any?) {
        out.mkdirs()
        File(out, name).writeText(json(v))
    }

    /** بصمة Java للنص (String.hashCode) */
    private fun jh(s: String): Int = s.hashCode()

    /** بصمات مجمّعة لكل خمسين آية */
    private fun chunks(a: Array<String>, f: (String) -> String = { it }): List<Int> =
        a.indices.chunked(50).map { idx -> idx.fold(17) { h, i -> h * 31 + jh(f(a[i])) } }

    // ---------- الاختبار ----------

    @Test
    fun export() {
        exportMeta()
        exportText()
        exportNormalize()
        exportSearch()
        exportAyahOfDay()
        exportMadinah()
        exportProgress()
        exportPrefs()
    }

    private fun exportMeta() {
        val surahs = (1..QuranMeta.SURAHS).map { n ->
            val first = QuranMeta.STARTS[n - 1]
            val last = first + QuranMeta.COUNTS[n - 1] - 1
            linkedMapOf(
                "n" to n, "count" to QuranMeta.COUNTS[n - 1], "start" to first, "medinan" to QuranMeta.isMedinan(n),
                "ar" to QuranMeta.NAMES_AR[n - 1], "t" to QuranMeta.TNAMES[n - 1], "e" to QuranMeta.ENAMES[n - 1],
                "en" to En.surahName(n), "firstPage" to QuranMeta.pageOf(first), "lastPage" to QuranMeta.pageOf(last),
                "firstJuz" to QuranMeta.juzOf(first), "lastJuz" to QuranMeta.juzOf(last), "hizb" to QuranMeta.hizbOf(first),
                "index1" to QuranMeta.index(n, 1), "indexLast" to QuranMeta.index(n, QuranMeta.COUNTS[n - 1]),
            )
        }
        val all = 0 until QuranMeta.AYAHS
        val pages = (1..604).map { p -> val r = QuranMeta.pageRange(p); listOf(r.first, r.last, QuranMeta.juzOf(r.first), QuranMeta.hizbOf(r.first), QuranMeta.locate(r.first).first, QuranMeta.locate(r.first).second) }
        val spreads = listOf(false, true).associate { sp ->
            sp.toString() to linkedMapOf(
                "count" to com.osatks.mizan.ui.quran.Spreads.count(sp),
                "index" to (-2..607).map { com.osatks.mizan.ui.quran.Spreads.index(it, sp) },
                "pages" to (0 until com.osatks.mizan.ui.quran.Spreads.count(sp)).map { com.osatks.mizan.ui.quran.Spreads.pages(it, sp).let { r -> listOf(r.first, r.last) } },
                "ayahs" to (0 until com.osatks.mizan.ui.quran.Spreads.count(sp)).map { com.osatks.mizan.ui.quran.Spreads.ayahs(it, sp).let { r -> listOf(r.first, r.last) } },
                "follow" to (0 until 200).map { k -> val a = (k * 37) % 610 - 2; val i = (k * 13) % com.osatks.mizan.ui.quran.Spreads.count(sp); listOf(a, i, com.osatks.mizan.ui.quran.Spreads.follow(a, i, sp)) },
            )
        }
        write(
            "meta.json", linkedMapOf(
                "ayahs" to QuranMeta.AYAHS, "surahs" to surahs,
                "juz" to QuranMeta.JUZ, "pages" to QuranMeta.PAGES, "quarters" to QuranMeta.QUARTERS, "sajdas" to QuranMeta.SAJDAS,
                "locSurah" to all.map { QuranMeta.locate(it).first }, "locAyah" to all.map { QuranMeta.locate(it).second },
                "juzOf" to all.map { QuranMeta.juzOf(it) }, "pageOf" to all.map { QuranMeta.pageOf(it) }, "hizbOf" to all.map { QuranMeta.hizbOf(it) },
                "pageRanges" to pages,
                "juzOrdinals" to JUZ_ORDINALS.toList(),
                "arabicDigits" to listOf(0, 1, 7, 10, 42, 99, 114, 255, 286, 604, 1000, 6236, 123456789).map { listOf(it, arabicDigits(it)) },
                "isRight" to (1..12).map { com.osatks.mizan.ui.quran.Spreads.isRight(it) },
                "spreads" to spreads,
            )
        )
    }

    private fun textFiles(): List<String> =
        listOf(QuranData.UTHMANI, QuranData.CLEAN) + File("src/commonMain/assets/tafsir").list()!!.sorted().map { "tafsir/$it" }

    private fun exportText() = runBlocking {
        val files = textFiles().associateWith { f ->
            val a = QuranData.load(f)
            linkedMapOf(
                "chunks" to chunks(a), "norm" to chunks(a) { Search.normalize(it) },
                "samples" to (0 until QuranMeta.AYAHS step 97).map { listOf(it, a[it]) },
                "empty" to a.count { it.isEmpty() },
            )
        }
        val u = QuranData.uthmani(); val c = QuranData.clean()
        val bism = (1..QuranMeta.SURAHS).flatMap { n -> listOf(QuranMeta.STARTS[n - 1], QuranMeta.STARTS[n - 1] + 1) }.distinct().filter { it < QuranMeta.AYAHS }
        write(
            "text.json", linkedMapOf(
                "files" to files,
                "bismillah" to bism.map { listOf(it, QuranData.withoutBismillah(u, it), QuranData.withoutBismillah(c, it)) },
                "header" to (1..114).map { QuranData.hasBismillahHeader(it) },
                // نصوص مصطنعة: سطور تعليق ومسافات وأرقام خارج النطاق
                "parse" to listOf(
                    "# x\n1|1|a\r\n1|2| b \r1|3|c|d\n2|0|bad\n115|1|bad\n|1|x\n1||y\n1|4\n+1|5|plus\n 1|6|sp\n1|7|first\n1|7|second\n\n2|286|end ",
                ).map { src -> val a = QuranData.parse(src.encodeToByteArray()); listOf(src, (0..8).map { a[it] } + listOf(a[QuranMeta.index(2, 286)])) },
                "translit" to listOf(
                    "Аллоҳ", "Бисмиллаҳир роҳманир роҳим", "ЕР ер Ер", "Ёшлар ёшлик", "ЦЕНТР цирк Цех ацетон", "Шайтон ШАЙТОН шайтон",
                    "Қуръон ғазаб ҳақ ўқиш", "Ўзбек ЎЗБЕК", "маъно маърифат", "объект", "Чарх ЧАРХ Ч", "Юсуф Яъқуб ЯЪҚУБ", "Эй одамлар",
                    "съезд съел", "Бу — (Ҳа) «Ер», ер!", "ь ъ Ь Ъ", "ЩИТ щ", "ёЕ еЕ", "abc Ер 123ер", "Нуҳ, Иброҳим ва Мусо",
                ).map { listOf(it, Translit.uzLatn(it)) },
            )
        )
    }

    private fun handStrings(): List<String> = listOf(
        "", " ", "  \t\n ", "؟", "...", "ا", "ال", "a b", "ب ن", "🙂 الله", "می‌خواهم", "الرحمـــن", "ﷲ", "ﻻ", "ALLAH", "AlLaH",
        "İSTANBUL", "ıIiİ", "ǅǄǆ", "Straße", "ΣΟΦΟΣ σοφός", "ΟΔΟΣ", "Ἀθῆναι", "Ünïcödé façade crème brûlée", "Àà Éé Ññ Çç Øø Åå Œœ Æ",
        "Al-A'raf En'âm Al‘Alaq Al’Asr ʼAbasa ʻAli ʾx ʿy `z", "a‌b‍c", "x y", " lead trail ", " em ",
        "line sep par", "tab\tnl\nvt\u000Bff\u000Ccr\r", "\u001Cfs\u001F", "a\u0085b", "$5 + 3 = 8 € £ ¥ © ® ™ ° ± × ÷ § ¶",
        "«quoted» “double” ‘single’ „low“ ‹a› 「x」 『y』", "foo—bar–baz-qux_quux", "1,234.56", "a/b\\c|d", "#tag @user ~tilde ^caret",
        "٠١٢٣٤٥٦٧٨٩", "۰۱۲۳۴۵۶۷۸۹", "০১২৩৪৫৬৭৮৯", "०१२३", "١٢:٣٤", "۲:۲۵۵", "١٨ ١٠",
        "أإآٱا", "ة ه", "ى ی ے ئ ي", "ک ك", "ہ ھ ۀ ه", "ؤ و", "ء", "لا إله إلا الله", "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
        "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ", "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ", "الٓمٓ", "يٰسٓ", "حمٓ عٓسٓقٓ", "صٓ", "كٓهيعٓصٓ",
        "ﭐﻟﺤﻤﺪ", "ﺑﺴﻢ", "رَحْمَٰن", "رحمٰن", "مہربان", "اللہ", "خداوند بخشنده مهربان", "سورۂ", "ۀ", "আল্লাহ করুণাময়", "নামে",
        "Аллах Милостивый", "Ёлка ЁЖ", "Аллоҳ", "Їжак Ґанок Єва", "Ğğ Şş Çç Öö Üü İı", "Allah'ın", "Allâh Rahmân Rahîm",
        "Maha Pengasih", "Dieu Miséricordieux", "Gott Barmherzigen", "Misericordioso clemente", "é é ẹ̄ ǖ",
        "ﬁ ﬂ ﬀ", "Ⅻ ⅻ", "①②", "ｆｕｌｌ ｗｉｄｔｈ", "x̀́̂y", "a⃝b", "क्षि", "ไทย", "한국어 한글", "日本語", "😀👍",
        "؛،؟٪٫٬", "۔", "ـ", "‘’‚‛“”", "() [] {} <>", "a''b``c", "x ' y", "2 : 255", "  3:5  ", "3/5", "3-5",
    )

    private fun exportNormalize() = runBlocking {
        val samples = ArrayList<String>()
        samples += handStrings()
        val u = QuranData.uthmani()
        for (i in 0 until QuranMeta.AYAHS step 13) samples += u[i]
        for (f in textFiles().drop(1)) { val a = QuranData.load(f); for (i in 5 until QuranMeta.AYAHS step 211) samples += a[i] }
        val scores = ArrayList<List<Any?>>()
        val names = listOf("Al-Kahf", "الكهف", "The Cave", "Ali 'Imran", "Yā Sīn", "سورة البقرة", "Prayer times", "Mecca", "مكة المكرمة", "Tasbih counter", "Ünïcödé")
        val qs = listOf("kahf", "al", "ka", "cave", "the", "imran", "ali imran", "yas", "البقرة", "بقر", "سورة", "x", "time", "pray", "مكة", "مكه", "count", "ün", "a b", "")
        for (n in names) for (q in qs) for (extra in listOf("", "18 الكهف Al-Kahf The Cave", "Quran القرآن")) {
            val t = Search.terms(q)
            scores += listOf(q, n, extra, Search.score(t, n, extra))
        }
        write(
            "normalize.json", linkedMapOf(
                "normalize" to samples.map { listOf(it, Search.normalize(it)) },
                "terms" to handStrings().map { listOf(it, Search.terms(it)) },
                "score" to scores,
            )
        )
    }

    private fun queries(): List<String> = runBlocking {
        val q = ArrayList<String>()
        q += listOf(
            "الحي القيوم", "قُلْ أعوذ برب الناس", "الرحمن", "الرَّحْمَـٰنِ", "بسم الله", "إن الله", "انا انزلناه", "إِنَّا أَنزَلْنَاهُ",
            "يا أيها الذين آمنوا", "الصلاة", "الصلوة", "زكاة", "مُحَمَّدٌ", "موسى", "عيسى ابن مريم", "الكهف", "البقرة", "آل عمران", "ال عمران",
            "يس", "يٰسٓ", "طه", "ق", "ن", "الم", "الٓمٓ", "ٱلْحَمْدُ لِلَّهِ", "جنة", "النار", "ربنا آتنا", "فبأي آلاء ربكما تكذبان", "سبح",
            "تبارك", "الملك", "الفاتحة", "فاتحة", "الاخلاص", "الإخلاص", "قل هو الله أحد", "مريم", "يوسف", "ابراهيم", "إبراهيم", "لقمان",
            "سورة البقرة", "الناس", "الفلق", "الرحمن الرحيم", "رحمان", "کتاب", "یوسف", "مریم", "ہود", "سورہ", "الکهف", "رب العالمین",
            "Kursi", "mercy", "Merciful", "Allah", "Lord of the worlds", "kahf", "Al-Kahf", "al kahf", "baqara", "Al-Baqarah", "yasin",
            "Ya-Sin", "yaseen", "fatiha", "Al-Faatiha", "Fatihah", "ikhlas", "nas", "An-Nas", "Imran", "Ali 'Imran", "Aal-i-Imraan",
            "Jonas", "Joseph", "The Cow", "cow", "the", "a", "ab", "Muhammad", "Moses", "Jesus", "paradise", "Garden", "prayer", "Gott",
            "Barmherzigen", "Dieu", "Miséricordieux", "misericordieux", "Allah'ın", "Rahman", "Rahmân", "Rahmet", "İbrahim", "ibrahim",
            "Ibrahim", "Allâh", "rahmat", "Tuhan", "Allah Maha", "Аллах", "милостивый", "Милостивого", "Господь", "Аллоҳ", "Alloh",
            "Raxmon", "rahmli", "Мейірімді", "আল্লাহ", "করুণাময়", "Fâtiha", "Bakara", "Yâsîn", "Misericordioso", "clemente", "Аллаг",
            "Maha Pengasih", "Pemurah", "خداوند", "بخشنده", "مهربان", "اللہ", "رحمٰن", "مہربان", "night", "Nacht", "nuit", "noche",
            "2:255", "٢:٢٥٥", "۲:۲۵۵", "18 10", "2.255", "2 : 255", "2:287", "115:1", "1:1", "114:6", "114:7", "0:1", "1:0", "2255",
            "2:", ":255", "  3:5  ", "3/5", "3-5", "١١٤ ٦", "১৮:১০", "002:255", "2::255", "2 :: 255", "12:111", "9:129", "2:255a",
            "a2:255", "1234:1", "1:1234", "2", "18", "114", "١٨", "255", "36", "", " ", "؟", "...", "ا", "ال", "a b", "ب ن", "🙂 الله",
            "الرحمـــن", "ﷲ", "ALLAH", "AlLaH", "İSTANBUL", "Sūrat al-Kahf", "Al‘Alaq", "Al’Asr", "naba", "nabaa", "zalzala", "Quraish",
            "quraysh", "Mary", "Maryam", "Meryem", "Marie", "María", "Марьям", "মারইয়াম", "Hud", "Yunus", "Yusuf", "Taha", "Taa-Haa",
            "The Opening", "Opening", "Fatiha Kahf", "kahf 18", "18 kahf", "نور", "النور", "Light", "The Light", "Lumière", "Licht",
        )
        val u = QuranData.uthmani(); val c = QuranData.clean()
        val tr = Sources.ALL.filter { it.kind == com.osatks.mizan.quran.SourceKind.TRANSLATION }
        for ((k, i) in (0 until QuranMeta.AYAHS step 41).withIndex()) {
            val w = u[i].split(' '); val cw = c[i].split(' ')
            val at = (k * 3) % maxOf(1, w.size - 1)
            q += w.drop(at).take(2).joinToString(" ")
            q += cw.drop((k * 5) % maxOf(1, cw.size - 1)).take(if (k % 3 == 0) 1 else 3).joinToString(" ")
            val src = tr[k % tr.size]
            val t = QuranData.source(src.id)[i].split(' ').filter { it.length > 2 }
            if (t.isNotEmpty()) q += t.drop((k * 7) % t.size).take(1 + k % 2).joinToString(" ")
        }
        q.distinct()
    }

    private fun surahHits(q: String, lang: Lang): List<Int> {
        val s = lang.strings
        val terms = Search.terms(q)
        return if (terms.isEmpty()) emptyList() else (1..QuranMeta.SURAHS).map { n ->
            n to Search.score(terms, s.surahName(n), "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)}")
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(12)
    }

    private fun exportSearch() = runBlocking {
        val langs = Lang.entries
        val list = queries().map { q ->
            linkedMapOf(
                "q" to q, "ref" to QuranSearch.parseRef(q), "terms" to Search.terms(q),
                "surahs" to langs.associate { it.code to surahHits(q, it) },
                "ayahs" to langs.associate { it.code to QuranSearch.ayahs(q, it) },
                "ayahs6" to listOf(Lang.AR, Lang.EN, Lang.of("uz")!!).associate { it.code to QuranSearch.ayahs(q, it, limit = 6) },
            )
        }
        // أسماء السور بكل لغة: بحث السور وحده (أسرع)
        val nameQueries = ArrayList<String>()
        for (l in langs) for (n in 1..QuranMeta.SURAHS step 3) nameQueries += l.strings.surahName(n)
        for (n in 1..QuranMeta.SURAHS step 2) { nameQueries += QuranMeta.TNAMES[n - 1]; nameQueries += QuranMeta.ENAMES[n - 1]; nameQueries += QuranMeta.NAMES_AR[n - 1].take(3) }
        val nameHits = nameQueries.distinct().map { q -> linkedMapOf("q" to q, "surahs" to listOf(Lang.AR, Lang.EN, Lang.of("kk")!!, Lang.FA).associate { it.code to surahHits(q, it) }) }
        write(
            "search.json", linkedMapOf(
                "langs" to langs.map { l ->
                    linkedMapOf(
                        "code" to l.code, "surahNames" to (1..QuranMeta.SURAHS).map { l.strings.surahName(it) },
                        "translation" to Sources.translationFor(l)?.id, "default" to Sources.defaultFor(l).id,
                        "ordered" to Sources.ordered(l).map { it.id }, "fallback" to l.pack?.tafsirFallback.orEmpty(),
                    )
                },
                "packs" to Lang.packs.map { l -> linkedMapOf("code" to l.code, "id" to l.pack?.quran?.id, "author" to l.pack?.quran?.author, "translit" to l.pack?.quran?.translit, "fallback" to l.pack?.tafsirFallback.orEmpty()) },
                "all" to Sources.ALL.map { listOf(it.id, it.lang.code, it.kind.name, it.author) },
                "queries" to list,
                "names" to nameHits,
            )
        )
    }

    private fun exportAyahOfDay() = runBlocking {
        val text = QuranData.uthmani()
        val days = ArrayList<LocalDate>()
        var d = LocalDate(2019, 1, 1)
        while (d <= LocalDate(2032, 12, 31)) { days += d; d = d.plusDays(1) }
        days += listOf(LocalDate(1900, 1, 1), LocalDate(1969, 12, 31), LocalDate(1970, 1, 1), LocalDate(1600, 3, 1), LocalDate(2100, 12, 31), LocalDate(2500, 6, 15))
        write("ayahofday.json", days.map { listOf(it.toEpochDay(), ayahOfDay(text, it)) })
    }

    private fun exportMadinah() {
        val pages = (1..Madinah.PAGES).map { p ->
            Madinah.lines(p).map { l ->
                when (l) {
                    null -> null
                    is MadinahLine.Header -> listOf("s", l.surah)
                    MadinahLine.Basmala -> listOf("b")
                    is MadinahLine.Ayah -> listOf("a", l.centered, if (p % 10 == 1) l.glyphs else l.glyphs.joinToString("").hashCode(), l.ayahs.toList(), l.ends.sorted())
                }
            }
        }
        write(
            "madinah.json", linkedMapOf(
                "pages" to pages, "headers" to (0..115).map { Madinah.headerCode(it) },
                "missing" to Madinah.lines(0).size,
            )
        )
    }

    // ---------- الإعدادات المحفوظة: مخزن في الذاكرة بدل SharedPreferences ----------

    private class MemPrefs : SharedPreferences {
        val m = LinkedHashMap<String, Any?>()
        override fun getAll(): MutableMap<String, *> = m
        override fun getString(k: String, d: String?) = m[k] as? String ?: d
        override fun getStringSet(k: String, d: MutableSet<String>?) = d
        override fun getInt(k: String, d: Int) = m[k] as? Int ?: d
        override fun getLong(k: String, d: Long) = m[k] as? Long ?: d
        override fun getFloat(k: String, d: Float) = m[k] as? Float ?: d
        override fun getBoolean(k: String, d: Boolean) = m[k] as? Boolean ?: d
        override fun contains(k: String) = m.containsKey(k)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(k: String, v: String?) = apply { m[k] = v }
            override fun putStringSet(k: String, v: MutableSet<String>?) = apply { m[k] = v }
            override fun putInt(k: String, v: Int) = apply { m[k] = v }
            override fun putLong(k: String, v: Long) = apply { m[k] = v }
            override fun putFloat(k: String, v: Float) = apply { m[k] = v }
            override fun putBoolean(k: String, v: Boolean) = apply { m[k] = v }
            override fun remove(k: String) = apply { m.remove(k) }
            override fun clear() = apply { m.clear() }
            override fun commit() = true
            override fun apply() {}
        }
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private val mem = MemPrefs()

    private fun installPrefs() {
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as sun.misc.Unsafe
        val prefs = unsafe.allocateInstance(Prefs::class.java) as Prefs
        Prefs::class.java.getDeclaredField("sp").apply { isAccessible = true }.set(prefs, mem)
        AppSettings::class.java.getDeclaredField("sp").apply { isAccessible = true }.set(null, prefs)
    }

    private fun unload(obj: Any) {
        obj::class.java.getDeclaredField("loaded").apply { isAccessible = true }.setBoolean(null, false)
    }

    private fun ranges(pages: List<Int>): String {
        val b = StringBuilder(); var i = 0
        while (i < pages.size) {
            var j = i
            while (j + 1 < pages.size && pages[j + 1] == pages[j] + 1) j++
            if (b.isNotEmpty()) b.append(',')
            b.append(if (j > i) "${pages[i]}-${pages[j]}" else "${pages[i]}")
            i = j + 1
        }
        return b.toString()
    }

    private fun snapshot(): Map<String, Any?> {
        val p = QuranProgress
        return linkedMapOf(
            "readCount" to p.readCount, "todayCount" to p.todayCount, "streak" to p.streak, "completed" to p.completed,
            "planDays" to p.planDays, "start" to p.start.toString(), "daysLeft" to p.daysLeft(), "dailyGoal" to p.dailyGoal(),
            "nextUnread" to p.nextUnread(), "finished" to p.finished, "read" to ranges((0..610).filter { p.isRead(it) }),
            "store" to mem.m.filterKeys { it.startsWith("k_") }.toSortedMap(),
        )
    }

    private fun b64(pages: List<Int>): String = Bits(605).also { b -> pages.forEach { b.set(it) } }.toBase64()

    private fun exportProgress() {
        installPrefs()
        val today = LocalDate.now()
        fun day(off: Int) = today.plusDays(off.toLong()).toString()
        val rnd = java.util.Random(42)
        // الحالة المحفوظة الأولى لكل سيناريو، ثم خطوات
        data class Sc(val name: String, val store: Map<String, Any?>, val steps: List<String>)
        val scs = ArrayList<Sc>()
        scs += Sc("fresh", emptyMap(), listOf("ensure", "read 1", "read 2", "read 2", "read 0", "read 605", "read 604", "plan 7", "plan 0", "reload"))
        scs += Sc("fresh-restart", emptyMap(), listOf("read 5", "restart 10", "read 1", "read 3", "restart", "reload"))
        for (plan in listOf(0, 7, 10, 15, 30, 60, -3, 1000)) for (startOff in listOf(0, -1, -6, -29, -45, 3)) {
            val pages = (1..604).filter { rnd.nextInt(100) < (plan.coerceIn(0, 100) % 7) * 12 + 5 }
            val st = linkedMapOf<String, Any?>("k_read" to b64(pages), "k_plan" to plan, "k_start" to day(startOff))
            when (rnd.nextInt(4)) {
                0 -> { st["k_today_date"] = day(0); st["k_today"] = 3; st["k_before"] = (pages.size - 3).coerceAtLeast(0) }
                1 -> { st["k_today_date"] = day(-1); st["k_today"] = 9; st["k_before"] = 1 }
                2 -> { st["k_today_date"] = day(0); st["k_today"] = 0 }
                else -> {}
            }
            when (rnd.nextInt(5)) {
                0 -> { st["k_last"] = day(0); st["k_streak"] = 4 }
                1 -> { st["k_last"] = day(-1); st["k_streak"] = 7 }
                2 -> { st["k_last"] = day(-2); st["k_streak"] = 12 }
                3 -> { st["k_last"] = "garbage"; st["k_streak"] = 2 }
                else -> {}
            }
            if (rnd.nextBoolean()) st["k_done"] = rnd.nextInt(5)
            val unread = (1..604).filter { it !in pages }
            val steps = arrayListOf("ensure")
            repeat(3 + rnd.nextInt(5)) { steps += "read ${if (unread.isEmpty()) 1 else unread[rnd.nextInt(unread.size)]}" }
            if (rnd.nextInt(3) == 0) steps += "rollover"
            if (rnd.nextInt(3) == 0) steps += "read ${unread.firstOrNull() ?: 1}"
            if (rnd.nextInt(4) == 0) steps += "plan ${listOf(0, 7, 10, 15, 30, 60)[rnd.nextInt(6)]}"
            steps += "reload"
            scs += Sc("plan$plan-start$startOff", st, steps)
        }
        val almost = (1..603).toList()
        scs += Sc("finish", mapOf("k_read" to b64(almost), "k_plan" to 30, "k_start" to day(-20), "k_last" to day(-1), "k_streak" to 19, "k_done" to 2),
            listOf("ensure", "read 100", "read 604", "read 604", "read 1", "reload", "restart 15", "read 1", "reload"))
        scs += Sc("page0-bit", mapOf("k_read" to b64(listOf(0) + (1..603).toList()), "k_plan" to 7, "k_start" to day(-2)), listOf("ensure", "read 604", "reload"))
        scs += Sc("extra-bits", mapOf("k_read" to java.util.Base64.getEncoder().encodeToString(ByteArray(90) { (it * 37).toByte() }), "k_plan" to 10, "k_start" to day(-4)), listOf("ensure", "read 1", "read 2", "reload"))
        for (bad in listOf("", "!!!", "AAAA", "AA", "AA==", "AB==", "QUJD", "QUJ", "QQ", "QQ=", "Q===", "////", "_-_-", "QUJD\n", " QUJD", "QU JD", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA//8="))
            scs += Sc("b64 $bad", mapOf("k_read" to bad, "k_start" to "2020-13-01", "k_plan" to 60), listOf("ensure", "read 3", "reload"))
        for (s in listOf("2024-02-30", "2024-2-3", "2024-02-03T00:00", "+2024-02-03", "20240203", "2024-02-29", "2023-02-29", " 2024-01-01"))
            scs += Sc("date $s", mapOf("k_start" to s, "k_plan" to 30, "k_last" to s, "k_streak" to 3, "k_today_date" to s, "k_today" to 5), listOf("ensure", "read 9", "reload"))
        scs += Sc("streak-chain", mapOf("k_last" to day(-1), "k_streak" to 1, "k_today_date" to day(-1), "k_today" to 4, "k_before" to 0, "k_read" to b64((1..4).toList())),
            listOf("ensure", "read 5", "read 6", "rollover", "read 7", "rollover2", "read 8", "reload"))

        val res = scs.map { sc ->
            mem.m.clear(); mem.m.putAll(sc.store)
            unload(QuranProgress)
            val snaps = sc.steps.map { st ->
                val parts = st.split(' ')
                when (parts[0]) {
                    "ensure" -> QuranProgress.ensure()
                    "read" -> QuranProgress.markRead(parts[1].toInt())
                    "restart" -> if (parts.size > 1) QuranProgress.restart(parts[1].toInt()) else QuranProgress.restart()
                    "plan" -> QuranProgress.changePlan(parts[1].toInt())
                    // تبقى العملية حية إلى اليوم التالي: تاريخ ورد اليوم المحفوظ صار أمس
                    "rollover" -> { mem.m["k_today_date"] = day(-1); QuranProgress.ensure() }
                    "rollover2" -> { mem.m["k_today_date"] = day(-1); mem.m["k_last"] = day(-1); unload(QuranProgress); QuranProgress.ensure() }
                    "reload" -> { unload(QuranProgress); QuranProgress.ensure() }
                }
                listOf(st, snapshot())
            }
            linkedMapOf("name" to sc.name, "store" to sc.store, "steps" to snaps)
        }
        write("progress.json", linkedMapOf("today" to today.toString(), "pages" to QuranProgress.PAGES, "plans" to QuranProgress.PLANS, "scenarios" to res))
    }

    private fun prefsSnapshot(): Map<String, Any?> {
        val p = QuranPrefs
        return linkedMapOf(
            "last" to p.last, "textSize" to p.textSize, "mode" to p.mode, "source" to p.source, "hidden" to p.hidden.toList(),
            "tone" to p.tone, "brightness" to p.brightness, "reciter" to p.reciter, "repeat" to p.repeat, "focusLast" to p.focusLast,
            "rangeMode" to p.rangeMode, "customFrom" to p.customFrom, "customTo" to p.customTo, "rangeRepeat" to p.rangeRepeat,
            "gap" to p.gap, "speed" to p.speed, "sleep" to p.sleep, "script" to p.script, "bookmarks" to p.bookmarks.toList(),
            "modeAr" to p.modeFor(Lang.AR), "modeEn" to p.modeFor(Lang.EN),
            "sourceAr" to p.sourceFor(Lang.AR).id, "sourceEn" to p.sourceFor(Lang.EN).id, "sourceKk" to p.sourceFor(Lang.of("kk")!!).id,
            "visibleAr" to Sources.visible(Lang.AR).map { it.id }, "visibleUz" to Sources.visible(Lang.of("uz")!!).map { it.id },
            "store" to mem.m.filterKeys { it.startsWith("q_") }.toSortedMap(),
        )
    }

    private fun exportPrefs() {
        installPrefs()
        val stores = listOf<Map<String, Any?>>(
            emptyMap(),
            mapOf("q_last" to 300, "q_size" to 50, "q_mode" to "list", "q_src" to "en.sahih", "q_hidden" to "ar.muyassar,,fr.hamidullah", "q_tone" to "night",
                "q_bright" to 55, "q_reciter" to "Alafasy_128kbps", "q_repeat" to 3, "q_focus_last" to 77, "q_rmode" to "custom", "q_cfrom" to -5, "q_cto" to 99999,
                "q_rrep" to 0, "q_gap" to -1, "q_speed" to 125, "q_sleep" to 15, "q_script" to "amiri", "q_marks" to "5,x,6236,-1,0,6235,12"),
            mapOf("q_src" to "ar.muyassar", "q_hidden" to "ar.muyassar", "q_bright" to -7, "q_speed" to 33, "q_rmode" to "", "q_script" to ""),
            mapOf("q_hidden" to Sources.ALL.joinToString(",") { it.id }, "q_src" to "nope"),
            mapOf("q_hidden" to "ar.muyassar,ar.jalalayn,en.sahih,kk.altai", "q_src" to "uz.sodik"),
        )
        val actions = listOf(
            "last 10", "last 10", "size 12", "size 33", "size 99", "mode mushaf", "source fr.hamidullah", "tone sepia", "bright 0.5", "bright -1", "bright 0.001", "bright 3",
            "bright 0.333", "reciter Husary", "repeat 0", "rmode surah", "custom 10 5", "custom -3 7000", "custom 100 200", "rrep 4", "gap 5", "speed 1.25", "speed 0.75",
            "speed 0.3333", "sleep 30", "script amiri", "focus 4", "focus 4", "mark 5", "mark 9", "mark 5", "mark 0", "hide en.sahih", "hide en.sahih", "show en.sahih", "hide ar.muyassar", "show nope",
        )
        val res = stores.map { st ->
            mem.m.clear(); mem.m.putAll(st)
            unload(QuranPrefs)
            QuranPrefs.ensure()
            val first = prefsSnapshot()
            val steps = actions.map { a ->
                val p = a.split(' ')
                when (p[0]) {
                    "last" -> QuranPrefs.updateLast(p[1].toInt())
                    "size" -> QuranPrefs.updateSize(p[1].toInt())
                    "mode" -> QuranPrefs.updateMode(p[1])
                    "source" -> QuranPrefs.updateSource(p[1])
                    "tone" -> QuranPrefs.updateTone(p[1])
                    "bright" -> QuranPrefs.updateBrightness(p[1].toFloat())
                    "reciter" -> QuranPrefs.updateReciter(p[1])
                    "repeat" -> QuranPrefs.updateRepeat(p[1].toInt())
                    "rmode" -> QuranPrefs.updateRangeMode(p[1])
                    "custom" -> QuranPrefs.updateCustom(p[1].toInt(), p[2].toInt())
                    "rrep" -> QuranPrefs.updateRangeRepeat(p[1].toInt())
                    "gap" -> QuranPrefs.updateGap(p[1].toInt())
                    "speed" -> QuranPrefs.updateSpeed(p[1].toFloat())
                    "sleep" -> QuranPrefs.updateSleep(p[1].toInt())
                    "script" -> QuranPrefs.updateScript(p[1])
                    "focus" -> QuranPrefs.updateFocusLast(p[1].toInt())
                    "mark" -> QuranPrefs.toggleBookmark(p[1].toInt())
                    "hide" -> QuranPrefs.setVisible(p[1], false)
                    "show" -> QuranPrefs.setVisible(p[1], true)
                }
                listOf(a, prefsSnapshot())
            }
            linkedMapOf("store" to st, "loaded" to first, "steps" to steps)
        }
        write("prefs.json", res)
    }
}
