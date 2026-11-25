@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.AyahIndexText
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.SearchEntry
import com.osatks.mizan.ui.screens.adhkarEntries
import com.osatks.mizan.ui.screens.eventEntries
import com.osatks.mizan.ui.screens.hadithEntries
import com.osatks.mizan.ui.screens.settingsEntries
import com.osatks.mizan.ui.screens.toolEntries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreSpotlight.CSSearchableIndex
import platform.CoreSpotlight.CSSearchableItem
import platform.CoreSpotlight.CSSearchableItemAttributeSet
import platform.CoreSpotlight.setAlternateNames
import platform.CoreSpotlight.setContentDescription
import platform.CoreSpotlight.setKeywords
import platform.CoreSpotlight.setTitle
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.distantFuture
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.DISPATCH_QUEUE_PRIORITY_LOW
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * بحث iPhone (Spotlight و«ابحث أو اسأل»): ما في ميزان يجده المستخدم من خارج التطبيق، ولمسُ النتيجة يفتح صفحته والتطبيق
 * مفتوح أو مغلق. معرّف كل عنصر هو رابط صفحته (SystemBridge.swift: SpotlightLink ← IosLive.open):
 *  - السور الـ114: اسمها بلغة التطبيق وبالعربية وبالحروف اللاتينية، وعدد آياتها.
 *  - الآيات الـ6236: السورة ورقم الآية عنواناً، ونصها بالرسم العثماني ومعناها بلغة التطبيق إن كانت ترجمته على الجهاز،
 *    وكلماتها بلا تشكيل وبالتطبيع الذي في بحث التطبيق (AyahIndexText)، فيجدها من يكتب بعض كلماتها كيفما كتبها.
 *    تفتح المصحف أو عرض الآيات (بحسب اختيار المستخدم) على الآية نفسها.
 *  - الأدوات وأقسام الأذكار والأحاديث (بعناوينها) والمناسبات وأقسام الإعدادات، من مصادر البحث داخل التطبيق (SearchEntries.kt).
 * تُفهرس في الخلفية بأولوية منخفضة، دفعةً بعد دفعة فلا تجتمع عناصر الآيات كلها في الذاكرة، ولا تُعاد إلا إن تغيّرت لغة
 * التطبيق أو رُفعت نسخة الفهرس أو تغيّر المحتوى (حديث أو ترجمة صارت على الجهاز)؛ وإن انقطعت فهرسة الآيات أكملت من حيث توقفت.
 */
internal object SystemSpotlight {
    private const val SURAH_DOMAIN = "quran"
    // نطاقات منفصلة لا يبدأ أحدها باسم الآخر: حذف نطاق في Spotlight يحذف ما تحته (quran.x تحت quran)
    private const val AYAH_DOMAIN = "ayah"
    private const val APP_DOMAIN = "app"
    // نسخة كل فهرس: تُرفع إن تغيّر شكل عناصره أو معرّفاتها
    private const val SURAH_VERSION = 1
    private const val AYAH_VERSION = 1
    private const val APP_VERSION = 1
    // مفاتيح حال الفهرسة في هذا الجهاز (لا تدخل النسخة الاحتياطية: Backup.EXCLUDED_KEYS والبادئة device_)
    private const val SURAH_KEY = "spotlight_quran"
    private const val AYAH_KEY = "device_spotlight_ayahs"
    private const val AYAH_PROGRESS = "device_spotlight_ayahs_at"
    private const val APP_KEY = "device_spotlight_app"
    /** آيات كل دفعة، والمهلة بين دفعتين */
    private const val BATCH = 200
    private const val PAUSE_MS = 150L

    /** يُرسل بعد إعادة فهرسة السور (تغيّرت اللغة): يحدّث Swift عبارات Siri التي فيها أسماء السور (MizanSearch.swift) */
    const val INDEXED = "MizanSpotlightIndexed"

    /** طابور النظام بأولوية منخفضة (تقابل «الخدمة» utility): الفهرسة لا تزاحم الواجهة ولا الإقلاع */
    private object LowPriority : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW.toLong(), 0u)) { block.run() }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + LowPriority)
    /** فهرسة جارية (يُقرأ ويُكتب على الخيط الرئيسي وحده) */
    private var running = false

    private class Stamps(val surah: String, val app: String, val ayah: String, val progress: String)

    /** عند الإقلاع وعند العودة إلى التطبيق (IosLive)، على الخيط الرئيسي: لا يغيّر شيئاً إن كانت الفهارس على حالها */
    fun refresh() {
        if (running) return
        runCatching {
            AppSettings.init()
            if (!CSSearchableIndex.isIndexingAvailable()) return
            val lang = Lang.current
            val stamps = Stamps(
                AppSettings.getString(SURAH_KEY), AppSettings.getString(APP_KEY),
                AppSettings.getString(AYAH_KEY), AppSettings.getString(AYAH_PROGRESS),
            )
            running = true
            scope.launch {
                try {
                    indexAll(lang, stamps)
                } catch (e: Throwable) {
                    // تُعاد المحاولة عند العودة التالية إلى التطبيق
                } finally {
                    runOnMain { running = false }
                }
            }
        }
    }

    private suspend fun indexAll(lang: Lang, st: Stamps) {
        val index = CSSearchableIndex.defaultSearchableIndex()
        val s = lang.strings
        // السور
        val surahStamp = "$SURAH_VERSION:${lang.code}"
        if (st.surah != surahStamp && index.replace(SURAH_DOMAIN, (1..QuranMeta.SURAHS).map { surahItem(s, it) })) {
            save(SURAH_KEY, surahStamp)
            runOnMain { NSNotificationCenter.defaultCenter.postNotificationName(INDEXED, null) }
        }
        // محتوى التطبيق: يتغير بتغير اللغة، وبما يُضاف في التحديثات أو يُنزَّل (الأحاديث)
        val entries = toolEntries(s) + adhkarEntries(s, lang) + hadithEntries(lang) + eventEntries(s) + settingsEntries(s, lang)
        val appStamp = "$APP_VERSION:${lang.code}:${entries.size}:${entries.joinToString("|") { "${it.link}=${it.title}" }.hashCode()}"
        if (st.app != appStamp && index.replace(APP_DOMAIN, entries.map(::entryItem))) save(APP_KEY, appStamp)
        // الآيات
        ayahs(index, lang, s, st)
    }

    // ---------- الآيات ----------

    private fun ayahStamp(lang: Lang, translation: String?) = "$AYAH_VERSION:${lang.code}:${translation ?: "-"}"

    private suspend fun ayahs(index: CSSearchableIndex, lang: Lang, s: Strings, st: Stamps) {
        // ترجمة المعاني بلغة التطبيق (لا توجد للعربية)، من محمّل التطبيق نفسه؛ وإن لم تكن على الجهاز فُهرست الآيات بدونها
        val src = if (lang == Lang.AR) null else Sources.translationFor(lang)
        if (st.ayah == ayahStamp(lang, src?.id)) return
        val meaning = src?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
        val stamp = ayahStamp(lang, if (meaning != null) src?.id else null)
        if (st.ayah == stamp) return
        val uthmani = QuranData.uthmani()
        val clean = runCatching { QuranData.clean() }.getOrNull()
        // فهرسة انقطعت بالختم نفسه: تكمل من حيث توقفت
        var from = st.progress.takeIf { it.substringBeforeLast('|') == stamp }?.substringAfterLast('|')?.toIntOrNull()?.coerceIn(0, QuranMeta.AYAHS) ?: 0
        // المعرّفات ثابتة في النسخة الواحدة فتُستبدل العناصر في مكانها (تبقى الآيات في البحث أثناء إعادة الفهرسة)؛
        // وتُحذف كلها أولاً إن تغيّرت النسخة
        if (from == 0 && st.ayah.isNotEmpty() && st.ayah.substringBefore(':') != "$AYAH_VERSION" && !index.deleteDomain(AYAH_DOMAIN)) return
        while (from < QuranMeta.AYAHS) {
            val to = minOf(from + BATCH, QuranMeta.AYAHS)
            val items = (from until to).map { ayahItem(it, s, uthmani, clean, meaning) }
            if (!index.put(items)) return
            from = to
            save(AYAH_PROGRESS, "$stamp|$from")
            delay(PAUSE_MS)
        }
        save(AYAH_KEY, stamp)
    }

    private fun ayahItem(i: Int, s: Strings, uthmani: Array<String>, clean: Array<String>?, meaning: Array<String>?): CSSearchableItem {
        val (surah, ayah) = QuranMeta.locate(i)
        // البسملة في أول كل سورة زخرفة مستقلة في المصحف، فلا تدخل نص الآية ولا كلماتها
        val text = QuranData.withoutBismillah(uthmani, i)
        val plain = clean?.let { QuranData.withoutBismillah(it, i) }.orEmpty()
        val tr = meaning?.getOrNull(i)?.takeIf { it.isNotBlank() }
        val set = CSSearchableItemAttributeSet(contentType = UTTypeText)
        set.setTitle(s.surahAyah(s.surahName(surah), ayah))
        set.setContentDescription(if (tr != null) "$text\n$tr" else text)
        // المستخدم يكتب بلا تشكيل: النص بلا تشكيل كما يُكتب، ومطبَّعاً كما في بحث التطبيق
        set.setAlternateNames(AyahIndexText.alternates(plain, text))
        set.setKeywords(AyahIndexText.keywords(plain, text) + listOf(QuranMeta.NAMES_AR[surah - 1], "$surah:$ayah"))
        return CSSearchableItem(uniqueIdentifier = surahRoute(surah, ayah, "widget"), domainIdentifier = AYAH_DOMAIN, attributeSet = set).also {
            // العناصر تنتهي بعد شهر افتراضياً
            it.expirationDate = NSDate.distantFuture
        }
    }

    // ---------- السور ومحتوى التطبيق ----------

    private fun surahItem(s: Strings, n: Int): CSSearchableItem {
        val ar = QuranMeta.NAMES_AR[n - 1]
        val set = CSSearchableItemAttributeSet(contentType = UTTypeText)
        set.setTitle(s.surahName(n))
        set.setContentDescription(
            "${s.quranTitle} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])} · ${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan}"
        )
        set.setAlternateNames(listOf(ar, "سورة $ar"))
        set.setKeywords(listOf(ar, "سورة $ar", QuranMeta.TNAMES[n - 1], QuranMeta.ENAMES[n - 1], s.surahName(n), s.tabQuran, "$n"))
        return CSSearchableItem(uniqueIdentifier = surahRoute(n, 0, "widget"), domainIdentifier = SURAH_DOMAIN, attributeSet = set).also {
            it.expirationDate = NSDate.distantFuture
        }
    }

    private fun entryItem(e: SearchEntry): CSSearchableItem {
        val set = CSSearchableItemAttributeSet(contentType = UTTypeText)
        set.setTitle(e.title)
        set.setContentDescription(listOf(e.subtitle, e.group).filter { it.isNotBlank() }.distinct().joinToString(" · "))
        // كلمات البحث كما هي ومطبَّعةً (بلا تشكيل ولا همزات)، واسم القسم
        val words = (e.extra.split(' ') + Search.normalize("${e.title} ${e.extra}").split(' ')).filter { it.length >= 2 }
        set.setKeywords((words + e.group).filter { it.isNotBlank() }.distinct())
        val norm = Search.normalize(e.title)
        if (norm.isNotBlank() && norm != e.title) set.setAlternateNames(listOf(norm))
        return CSSearchableItem(uniqueIdentifier = e.link, domainIdentifier = APP_DOMAIN, attributeSet = set).also {
            it.expirationDate = NSDate.distantFuture
        }
    }

    // ---------- الفهرس ----------

    /** يستبدل عناصر النطاق كلها (عناصر قليلة: السور ومحتوى التطبيق) */
    private suspend fun CSSearchableIndex.replace(domain: String, items: List<CSSearchableItem>): Boolean =
        deleteDomain(domain) && items.chunked(BATCH).all { put(it) }

    private suspend fun CSSearchableIndex.deleteDomain(domain: String): Boolean = suspendCoroutine { c ->
        deleteSearchableItemsWithDomainIdentifiers(listOf(domain)) { error -> c.resume(error == null) }
    }

    private suspend fun CSSearchableIndex.put(items: List<CSSearchableItem>): Boolean = suspendCoroutine { c ->
        indexSearchableItems(items) { error -> c.resume(error == null) }
    }

    private fun save(key: String, value: String) = runOnMain { AppSettings.putString(key, value) }
}
