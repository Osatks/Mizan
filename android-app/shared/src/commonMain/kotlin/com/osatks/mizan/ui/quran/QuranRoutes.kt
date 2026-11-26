package com.osatks.mizan.ui.quran

import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress

/**
 * مسار صفحة السورة؛ ayah = 0 يعني من أولها. from يحدد العنصر الذي يتحول إلى رأس الصفحة
 * ("row" صف السورة، "continue" بطاقة المتابعة) فلا يتكرر مفتاح الحركة في الشاشة نفسها.
 */
fun surahRoute(surah: Int, ayah: Int = 0, from: String = "row") = "surah/$surah?ayah=$ayah&from=$from"

const val SURAH_ROUTE = "surah/{surah}?ayah={ayah}&from={from}"

/**
 * يفتح صفحة مصحف المدينة [page] (من 1 إلى 604): مسار السورة عند أول آية في الصفحة، و from=page فلا تُظلَّل تلك الآية
 * (القارئ يريد الصفحة لا آية بعينها)
 */
fun pageRoute(page: Int): String {
    val (surah, ayah) = QuranMeta.locate(QuranMeta.pageRange(page.coerceIn(1, QuranMeta.PAGES.size)).first)
    return surahRoute(surah, ayah, "page")
}

/** رابط «تابع القراءة» في الاختصارات (Siri، واختصارات أيقونة أندرويد): يصير عند فتحه صفحة آخر آية قرأها المستخدم */
const val CONTINUE_ROUTE = "quran/continue"

/** صفحة آخر آية قرأها المستخدم، أو تبويب القرآن إن لم يقرأ بعد */
fun continueRoute(): String {
    QuranPrefs.ensure()
    val last = QuranPrefs.last
    return if (last >= 0) QuranMeta.locate(last).let { (surah, ayah) -> surahRoute(surah, ayah, "widget") } else "quran"
}

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
