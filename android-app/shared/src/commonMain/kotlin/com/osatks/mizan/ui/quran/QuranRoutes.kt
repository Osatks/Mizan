package com.osatks.mizan.ui.quran

import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranProgress

/**
 * مسار صفحة السورة؛ ayah = 0 يعني من أولها. from يحدد العنصر الذي يتحول إلى رأس الصفحة
 * ("row" صف السورة، "continue" بطاقة المتابعة) فلا يتكرر مفتاح الحركة في الشاشة نفسها.
 */
fun surahRoute(surah: Int, ayah: Int = 0, from: String = "row") = "surah/$surah?ayah=$ayah&from=$from"

const val SURAH_ROUTE = "surah/{surah}?ayah={ayah}&from={from}"

/** اسم السورة بلغة الواجهة (أو بالعربية إن نقص) */
fun Strings.surahName(surah: Int): String = surahNames.getOrNull(surah - 1) ?: QuranMeta.NAMES_AR[surah - 1]

fun Strings.ayahRef(index: Int): String {
    val (surah, ayah) = QuranMeta.locate(index)
    return surahAyah(surahName(surah), ayah)
}

/** نسبة ما قُرئ من صفحات السورة في الختمة الحالية */
internal fun surahProgress(n: Int): Float {
    val first = QuranMeta.pageOf(QuranMeta.STARTS[n - 1])
    val last = QuranMeta.pageOf(QuranMeta.STARTS[n - 1] + QuranMeta.COUNTS[n - 1] - 1)
    return (first..last).count { QuranProgress.isRead(it) } / (last - first + 1).toFloat()
}
