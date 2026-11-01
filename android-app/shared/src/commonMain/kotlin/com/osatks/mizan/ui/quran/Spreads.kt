package com.osatks.mizan.ui.quran

import com.osatks.mizan.quran.QuranMeta

/**
 * تقليب المصحف صفحةً صفحة أو كتاباً مفتوحاً (صفحتين متقابلتين).
 * الكتاب العربي يُفتح من اليمين، فالصفحة الفردية على اليمين والزوجية على يسارها كما في مصحف المدينة المطبوع:
 * 1 و2 (الفاتحة وأول البقرة)، ثم 3 و4، ... وآخرها 603 و604؛ فـ604 صفحة = 302 كتاباً مفتوحاً.
 * رقم الصفحة هو أصل الحالة دائماً، ويُحوَّل إلى موضع التقليب وعكسه حين تتغير النافذة، فيبقى القارئ على صفحته.
 */
internal object Spreads {
    val PAGES get() = QuranMeta.PAGES.size

    /** عدد مواضع التقليب */
    fun count(spread: Boolean): Int = if (spread) (PAGES + 1) / 2 else PAGES

    /** موضع التقليب الذي فيه الصفحة [page] */
    fun index(page: Int, spread: Boolean): Int = (page.coerceIn(1, PAGES) - 1).let { if (spread) it / 2 else it }

    /** الصفحات الظاهرة في موضع التقليب [index] (اليمنى أولاً) */
    fun pages(index: Int, spread: Boolean): IntRange =
        if (spread) (2 * index + 1)..minOf(2 * index + 2, PAGES) else (index + 1)..(index + 1)

    /** الصفحة اليمنى من الكتاب المفتوح */
    fun isRight(page: Int): Boolean = page % 2 == 1

    /** آيات الصفحات الظاهرة كلها (لتبدأ التلاوة منها، وللكشف في الحفظ) */
    fun ayahs(index: Int, spread: Boolean): IntRange = pages(index, spread).let { QuranMeta.pageRange(it.first).first..QuranMeta.pageRange(it.last).last }

    /**
     * الصفحة التي يقف عندها القارئ بعد الانتقال إلى الموضع [index]: تبقى [anchor] إن كانت ظاهرة فيه
     * (تغيّر النافذة لا يغيّر الصفحة)، وإلا فأول ما يظهر (اليمنى)
     */
    fun follow(anchor: Int, index: Int, spread: Boolean): Int = pages(index, spread).let { if (anchor in it) anchor else it.first }
}

/** موضع الصفحة في الكتاب المفتوح */
internal enum class PageSide { Single, Right, Left }
