package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.PageInfo
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPages
import com.osatks.mizan.quran.arabicDigits

/**
 * اختيار الصفحة في لوحة القارئ (من زر «انتقل إلى صفحة» في الرأس أو بلمس رقم الصفحة في أسفلها): الرقم وما في الصفحة،
 * والشريط و − و +، وبدايات الأجزاء، و«اذهب». [page] الصفحة الظاهرة حين فُتحت، و[onGo] الصفحة المختارة.
 * في أندرويد من Compose (PagePicker.android.kt)، وفي iOS ورقة النظام بالمحتوى نفسه (NativeQuranSheets.kt و NativeQuranSheets.swift).
 */
@Composable
internal expect fun PageJumpContent(page: Int, onGo: (Int) -> Unit)

/** فاصل الأسماء في سطر واحد: الفاصلة العربية في اللغات التي تُكتب من اليمين */
internal fun listSeparator(rtl: Boolean) = if (rtl) "، " else ", "

/** سور الصفحة بلغة الواجهة: «الأعراف»، أو «الإخلاص، الفلق، الناس» في الصفحة الأخيرة */
internal fun Strings.pageSurahs(info: PageInfo, rtl: Boolean): String = info.surahs.joinToString(listSeparator(rtl)) { surahName(it) }

/** ما في الصفحة في سطر تحت رقمها: «الأعراف · الجزء 9 · الحزب 18» */
internal fun Strings.pageSummary(info: PageInfo, rtl: Boolean): String =
    "${pageSurahs(info, rtl)} · ${juzName(info.juz)} · ${hizbName(info.hizb)}"

/** ما يقرؤه قارئ الشاشة عن الصفحة المختارة: «الصفحة 173، الأعراف، الجزء 9» */
internal fun Strings.pageSpeech(info: PageInfo, rtl: Boolean): String =
    listOf(pageName(info.page), pageSurahs(info, rtl), juzName(info.juz)).joinToString(listSeparator(rtl))

/** صفحة بداية كل جزء (30): شرائح الأجزاء، والاهتزاز الأقوى حين يعبر الاختيار بداية جزء */
internal val JUZ_START_PAGES: List<Int> by lazy { (1..30).map { QuranPages.juzPages(it).first } }

/** هل يعبر الانتقال من الصفحة [from] إلى [to] بداية جزء (الحد بين صفحة البداية وما قبلها) */
internal fun crossesJuz(from: Int, to: Int): Boolean {
    val lo = minOf(from, to); val hi = maxOf(from, to)
    return JUZ_START_PAGES.any { it > lo && it <= hi }
}

/** رأس صفحة المصحف كما يُكتب في أعلاها (في الصفحة نفسها، ومعاينتها في اختيار الصفحة، وأوراق رحلة الانتقال): السورة التي تبدأ بها */
internal fun mushafHeadTitle(page: Int): String =
    "سُورَةُ ${QuranMeta.NAMES_AR[QuranMeta.locate(QuranMeta.pageRange(page.coerceIn(1, QuranPages.COUNT)).first).first - 1]}"

/** طرف الرأس الآخر: الجزء بالحروف والحزب بالأرقام المشرقية، «الجزء التاسع · الحزب ١٨» */
internal fun mushafHeadSubtitle(page: Int): String {
    val first = QuranMeta.pageRange(page.coerceIn(1, QuranPages.COUNT)).first
    return "الجزء ${JUZ_ORDINALS[QuranMeta.juzOf(first) - 1]} · الحزب ${arabicDigits(QuranMeta.hizbOf(first))}"
}

/** ما يظهر في الشريط أعلى القارئ حين يصل إلى صفحة اختارها: «الأعراف · الجزء 9 · الصفحة 173» بأرقام التطبيق */
internal fun Strings.arrivalBanner(page: Int, rtl: Boolean): String {
    val info = QuranPages.info(page)
    return "${pageSurahs(info, rtl)} · ${juzName(info.juz)} · ${pageName(info.page)}"
}

