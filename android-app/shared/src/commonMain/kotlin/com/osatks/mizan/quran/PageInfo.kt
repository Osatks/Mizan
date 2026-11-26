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

    /** صفحة بداية الحزب [hizb] (من 1 إلى 60): علاماته الصغيرة على حافة المصحف في اختيار الصفحة */
    fun hizbStartPage(hizb: Int): Int = QuranMeta.pageOf(QuranMeta.QUARTERS[(hizb.coerceIn(1, 60) - 1) * 4])

    /**
     * أول ما في الصفحة من نص لمعاينتها في اختيار الصفحة: من أول آية فيها (بلا البسملة التي تُكتب زخرفةً فوق السورة)،
     * وكل آية تنتهي بعلامة ۝ ورقمها كما في عرض آية آية، وتُتبع بما بعدها في الصفحة حتى نحو [max] حرفاً، ويُقطع عند حدّ كلمة
     * فلا تظهر كلمة مبتورة. [text] نص المصحف (QuranData.uthmani)، آية لكل عنصر.
     */
    fun opening(text: Array<String>, page: Int, max: Int = 110): String {
        val range = QuranMeta.pageRange(page.coerceIn(1, COUNT))
        val out = StringBuilder()
        for (i in range) {
            if (i !in text.indices) break
            val ayah = QuranData.withoutBismillah(text, i).trim()
            if (ayah.isEmpty()) continue
            val first = out.isEmpty()
            if (!first) out.append(' ')
            val room = max - out.length
            if (ayah.length > room) {
                // الكلمات التي تسعها المساحة الباقية (وكلمة واحدة على الأقل في أول الصفحة)
                var used = 0
                var any = false
                for (w in ayah.split(' ')) {
                    val add = (if (any) 1 else 0) + w.length
                    if ((any || !first) && used + add > room) break
                    if (any) out.append(' ')
                    out.append(w)
                    used += add
                    any = true
                }
                return out.toString().trimEnd() + " …"
            }
            out.append(ayah).append(" ۝").append(arabicDigits(QuranMeta.locate(i).second))
            if (out.length >= max) break
        }
        return out.toString()
    }
}
