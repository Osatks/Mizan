@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import platform.CoreSpotlight.CSSearchableIndex
import platform.CoreSpotlight.CSSearchableItem
import platform.CoreSpotlight.CSSearchableItemAttributeSet
import platform.CoreSpotlight.setAlternateNames
import platform.CoreSpotlight.setContentDescription
import platform.CoreSpotlight.setKeywords
import platform.CoreSpotlight.setTitle
import platform.Foundation.NSDate
import platform.Foundation.distantFuture
import platform.UniformTypeIdentifiers.UTTypeText

/**
 * سور القرآن في بحث iPhone (Spotlight): اسم السورة بلغة التطبيق وبالعربية وبالحروف اللاتينية، وعدد آياتها.
 * معرّف كل عنصر هو مسار صفحتها، فيفتحها لمسُ النتيجة (MizanSystem.swift ← IosLive.open).
 * تُعاد الفهرسة حين تتغير لغة التطبيق فقط.
 */
internal object QuranSpotlight {
    private const val DOMAIN = "quran"
    private const val VERSION = 1
    private const val KEY = "spotlight_quran"

    fun index() {
        runCatching {
            AppSettings.init()
            val lang = Lang.current
            val stamp = "$VERSION:${lang.code}"
            if (AppSettings.getString(KEY) == stamp || !CSSearchableIndex.isIndexingAvailable()) return
            val s = lang.strings
            val items = (1..QuranMeta.SURAHS).map { n ->
                val ar = QuranMeta.NAMES_AR[n - 1]
                val set = CSSearchableItemAttributeSet(contentType = UTTypeText)
                set.setTitle(s.surahName(n))
                set.setContentDescription(
                    "${s.quranTitle} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])} · ${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan}"
                )
                set.setAlternateNames(listOf(ar, "سورة $ar"))
                set.setKeywords(listOf(ar, "سورة $ar", QuranMeta.TNAMES[n - 1], QuranMeta.ENAMES[n - 1], s.surahName(n), s.tabQuran, "$n"))
                CSSearchableItem(uniqueIdentifier = surahRoute(n, 0, "widget"), domainIdentifier = DOMAIN, attributeSet = set).also {
                    // العناصر تنتهي بعد شهر افتراضياً
                    it.expirationDate = NSDate.distantFuture
                }
            }
            val index = CSSearchableIndex.defaultSearchableIndex()
            index.deleteSearchableItemsWithDomainIdentifiers(listOf(DOMAIN)) { _ ->
                index.indexSearchableItems(items) { error ->
                    if (error == null) runOnMain { AppSettings.putString(KEY, stamp) }
                }
            }
        }
    }
}
