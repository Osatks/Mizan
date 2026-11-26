package com.osatks.mizan.quran

import kotlin.math.abs
import kotlin.math.roundToInt

/** نوع رحلة الانتقال إلى صفحة: الصفحة نفسها (توهج فقط)، أو قريبة (تقليبة واحدة)، أو بعيدة (تقليب سريع عبر المصحف) */
enum class JourneyKind { Still, Short, Flip }

/**
 * رحلة الانتقال إلى صفحة عبر المصحف: من موضع التقليب [from] إلى [to] (بمعنى Spreads.index: الصفحة ناقص واحد، أو الكتاب
 * المفتوح). [sheets] الأوراق التي تظهر بالترتيب: الأولى الموضع الحالي (صورته)، والأخيرة الموضع المقصود (يُرسم حقاً)، وما بينهما
 * أوراق خفيفة بإطارها ورقمها فقط. [pages] رقم الصفحة الظاهر على كل ورقة (للرقم الدوّار)، و[turns] مدة كل تقليبة بالملّي ثانية:
 * تتسارع ثم تتباطأ. القارئ في iOS (NativeMushaf.swift) وفي Compose (PageJourneyOverlay) يرسمان الرحلة نفسها من هذه البيانات.
 */
data class PageJourney(
    val from: Int,
    val to: Int,
    val sheets: List<Int>,
    val pages: List<Int>,
    val turns: List<Int>,
) {
    val kind: JourneyKind get() = when (abs(to - from)) {
        0 -> JourneyKind.Still
        1, 2 -> JourneyKind.Short
        else -> JourneyKind.Flip
    }

    /** إلى الأمام (الصفحات التالية): تنقلب الأوراق نحو اليمين فوق الكعب كالمصحف المطبوع وتظهر التالية من تحتها */
    val forward: Boolean get() = to > from

    /** مدة التقليب كله (بلا رفع الصفحة ولا هبوطها) */
    val flipMs: Int get() = turns.sum()

    /** مدة الرحلة كلها: رفع الصفحة، والتقليب، والهبوط */
    val totalMs: Int get() = if (kind == JourneyKind.Flip) PageJourneys.LIFT_MS + flipMs + PageJourneys.LAND_MS else flipMs

    /** بداية التقليبة [k] منذ بدء التقليب */
    fun turnStart(k: Int): Int = turns.take(k).sum()

    /**
     * الرقم الدوّار بعد [ms] من بدء التقليب: ينتقل من رقم الورقة إلى التي تحتها أثناء كل تقليبة، فيبلغ رقم كل ورقة حين
     * تنكشف كلها (ويبدأ من الصفحة الحالية وينتهي بالمقصودة)
     */
    fun valueAt(ms: Float): Float {
        if (turns.isEmpty() || pages.isEmpty()) return pages.lastOrNull()?.toFloat() ?: to.toFloat()
        var t = ms.coerceAtLeast(0f)
        for (k in turns.indices) {
            val d = turns[k].toFloat()
            if (t <= d) return pages[k] + (pages[k + 1] - pages[k]) * (t / d)
            t -= d
        }
        return pages.last().toFloat()
    }
}

/** تخطيط رحلات الانتقال إلى صفحة (اختيار الصفحة في القارئ وفي فهرس المصحف) */
object PageJourneys {
    /** رفع الصفحة الحالية عن الكتاب قبل التقليب */
    const val LIFT_MS = 180

    /** هبوط الصفحة المقصودة إلى مكانها بارتداد خفيف */
    const val LAND_MS = 380

    /** مدة التقليبة القصيرة (الصفحة المجاورة) */
    const val SHORT_MS = 340

    /** أسرع تقليبة (في وسط الرحلة) وما يُزاد على أبطئها (أولها وآخرها) */
    private const val FAST_MS = 80
    private const val EXTRA_MS = 82

    /** عدد التقليبات: ثلاث للمسافة القصيرة، وأربع إلى ست كلما بعدت الصفحة */
    fun turnCount(distance: Int): Int = when {
        distance < 4 -> distance
        distance < 40 -> 4
        distance < 200 -> 5
        else -> 6
    }

    /**
     * رحلة من الموضع [from] إلى [to] من [count] موضعاً. [pageOf] رقم الصفحة الظاهر على الموضع (في الكتاب المفتوح صفحته
     * اليمنى)، و[fromPage] و[toPage] الصفحتان الحقيقيتان في طرفي الرحلة (ما يُرى في أولها وما يُختار).
     */
    fun plan(
        from: Int, to: Int, count: Int = QuranPages.COUNT,
        fromPage: Int = from + 1, toPage: Int = to + 1, pageOf: (Int) -> Int = { it + 1 },
    ): PageJourney {
        val a = from.coerceIn(0, count - 1)
        val b = to.coerceIn(0, count - 1)
        val d = abs(b - a)
        if (d == 0) return PageJourney(a, b, listOf(a), listOf(toPage), emptyList())
        if (d <= 2) return PageJourney(a, b, listOf(a, b), listOf(fromPage, toPage), listOf(SHORT_MS))
        val n = turnCount(d)
        val dir = if (b > a) 1 else -1
        // مواضع الأوراق بين الطرفين: متقاربة في أول الرحلة وآخرها ومتباعدة في وسطها (تتسارع ثم تتباطأ)،
        // كلٌّ بعد التي قبله ولا يبلغ المقصود قبل آخر ورقة
        val sheets = ArrayList<Int>(n + 1)
        sheets.add(a)
        for (k in 1 until n) {
            val x = k.toFloat() / n
            val e = x * x * (3 - 2 * x)
            var q = a + ((b - a) * e).roundToInt()
            val prev = sheets.last()
            if ((q - prev) * dir < 1) q = prev + dir
            val room = n - k
            if ((b - q) * dir < room) q = b - room * dir
            sheets.add(q)
        }
        sheets.add(b)
        val pages = sheets.mapIndexed { i, s ->
            when (i) {
                0 -> fromPage
                sheets.size - 1 -> toPage
                else -> pageOf(s)
            }
        }
        val turns = List(n) { k ->
            val c = if (n == 1) 0f else 2f * k / (n - 1) - 1f
            FAST_MS + (EXTRA_MS * c * c).roundToInt()
        }
        return PageJourney(a, b, sheets, pages, turns)
    }
}
