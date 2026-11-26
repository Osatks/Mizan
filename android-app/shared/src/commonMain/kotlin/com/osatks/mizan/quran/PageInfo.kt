package com.osatks.mizan.quran

/**
 * ما في صفحة من مصحف المدينة كما يُكتب في رأسها: سورها بترتيبها (التي تبدأ بها الصفحة وما يبدأ فيها بعدها)،
 * وجزء أول آية فيها وحزبها (كما في رأس الصفحة نفسها).
 */
data class PageInfo(val page: Int, val surahs: List<Int>, val juz: Int, val hizb: Int)

/** صفحات مصحف المدينة: ما في كل صفحة، وصفحات كل جزء (لاختيار الصفحة في فهرس المصحف وفي القارئ) */
object QuranPages {
    /** عدد صفحات مصحف المدينة */
    const val COUNT = 604

    fun info(page: Int): PageInfo {
        val p = page.coerceIn(1, COUNT)
        val range = QuranMeta.pageRange(p)
        val first = QuranMeta.locate(range.first).first
        val last = QuranMeta.locate(range.last).first
        return PageInfo(p, (first..last).toList(), QuranMeta.juzOf(range.first), QuranMeta.hizbOf(range.first))
    }

    /** صفحة بداية الجزء [juz] (من 1 إلى 30) وصفحة آخر آية فيه */
    fun juzPages(juz: Int): IntRange {
        val start = QuranMeta.pageOf(QuranMeta.JUZ[juz - 1])
        val end = if (juz < QuranMeta.JUZ.size) QuranMeta.pageOf(QuranMeta.JUZ[juz] - 1) else COUNT
        return start..end
    }

    /** الجزء الذي تبدأ فيه الصفحة (أعلاها أو وسطها)، أو 0 إن لم يبدأ فيها جزء: اهتزاز أقوى عند حدود الأجزاء */
    fun juzStartingAt(page: Int): Int {
        val j = QuranMeta.JUZ.indexOfFirst { QuranMeta.pageOf(it) == page }
        return if (j < 0) 0 else j + 1
    }

    /** الجزء الذي فيه الصفحة بحسب صفحات بداية الأجزاء (لتظليل شريحة الجزء الذي اختاره القارئ) */
    fun juzByStart(page: Int): Int = (1..QuranMeta.JUZ.size).last { juzPages(it).first <= page }
}
