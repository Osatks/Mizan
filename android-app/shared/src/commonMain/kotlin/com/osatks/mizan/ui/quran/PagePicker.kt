package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.PageInfo
import com.osatks.mizan.quran.QuranPages

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
