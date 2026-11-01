package com.osatks.mizan.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * التخطيط المتكيّف: فئات عرض النافذة وارتفاعها من المساحة المتاحة للتطبيق بالـdp، لا من نوع الجهاز.
 * فالشاشة نفسها تتبدّل بين الفئات حين تتغير النافذة: تدوير iPad، أو Split View، أو فتح iPhone Duo وطيّه،
 * أو تطبيق أندرويد في نافذة متعددة. ضيّق < 600 ≤ متوسط < 840 ≤ واسع (كفئات Material).
 */
enum class SizeClass { Compact, Medium, Expanded }

fun widthClassOf(width: Dp): SizeClass = when {
    width < 600.dp -> SizeClass.Compact
    width < 840.dp -> SizeClass.Medium
    else -> SizeClass.Expanded
}

fun heightClassOf(height: Dp): SizeClass = when {
    height < 480.dp -> SizeClass.Compact
    height < 900.dp -> SizeClass.Medium
    else -> SizeClass.Expanded
}

/** مقاس نافذة التطبيق وما يترتب عليه */
@Immutable
class WindowLayout(val width: Dp, val height: Dp) {
    val widthClass = widthClassOf(width)
    val heightClass = heightClassOf(height)
    val landscape: Boolean get() = width > height

    /** شريط تنقل جانبي بدل الشريط السفلي: عرض واسع، أو متوسط والنافذة بالعرض */
    val rail: Boolean get() = widthClass == SizeClass.Expanded || (widthClass == SizeClass.Medium && landscape)

    /** أكثر من عرض الهاتف: الأوراق السفلية نوافذ في الوسط، ولوحات القراءة على الجانب */
    val wide: Boolean get() = widthClass != SizeClass.Compact

    /** المصحف صفحتين متقابلتين كالكتاب المفتوح */
    val mushafSpread: Boolean get() = spreadFits(width, height)

    override fun equals(other: Any?) = other is WindowLayout && other.width == width && other.height == height
    override fun hashCode() = width.hashCode() * 31 + height.hashCode()
}

/**
 * صفحتان متقابلتان حين تتسع النافذة لهما: عرض 600 فأكثر، والعرض قريب من الارتفاع أو أكبر منه
 * (iPad بالعرض، والشاشة الداخلية لـ iPhone Duo)، وارتفاع يكفي لصفحة مقروءة (لا هاتف بالعرض)
 */
fun spreadFits(width: Dp, height: Dp): Boolean =
    width >= 600.dp && height >= 480.dp && width.value / height.value >= SPREAD_RATIO

/** أقل نسبة عرض إلى ارتفاع للصفحتين المتقابلتين */
const val SPREAD_RATIO = 0.95f

/** مقاس النافذة الحالي؛ يوفّره MizanApp من المساحة المتاحة له */
val LocalWindowLayout = compositionLocalOf { WindowLayout(400.dp, 800.dp) }

/** مساحة في جهة البداية تشغلها قائمة التنقل الجانبية في صفحات التبويبات (مقابل [LocalBottomInset]) */
val LocalLeadingInset = compositionLocalOf { 0.dp }

/** أقصى عرض لعمود نصي واحد (سطور مريحة القراءة: نحو 90 حرفاً على الأكثر) */
val TextColumnMax = 720.dp

/** أقصى عرض لصفحة بعمودين (المدخلات بجانب النتائج) */
val SplitMax = 1100.dp

/** هوامش المحتوى: 16 على الهاتف، وأوسع على الشاشات العريضة حتى يتوسط المحتوى بعرض [max] */
class Gutters(val start: Dp, val end: Dp)

/**
 * هوامش عمود محتوى بعرض أقصاه [max] في مساحة عرضها [width]، بعد [lead] في جهة البداية (القائمة الجانبية)
 * وبعد المنطقة الآمنة الأفقية (النتوء أو الكاميرا حين تكون النافذة بالعرض)
 */
@Composable
fun gutters(width: Dp, max: Dp, lead: Dp = 0.dp, min: Dp = 16.dp): Gutters {
    val safe = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val dir = LocalLayoutDirection.current
    val safeStart = safe.calculateStartPadding(dir)
    val safeEnd = safe.calculateEndPadding(dir)
    return guttersFor(width, max, lead + safeStart, safeEnd, min)
}

/** حساب [gutters] بلا واجهة (يُختبر وحده) */
fun guttersFor(width: Dp, max: Dp, lead: Dp, trail: Dp, min: Dp = 16.dp): Gutters {
    val room = width - lead - trail
    val extra = ((room - min * 2 - max) / 2).coerceAtLeast(0.dp)
    return Gutters(lead + min + extra, trail + min + extra)
}
