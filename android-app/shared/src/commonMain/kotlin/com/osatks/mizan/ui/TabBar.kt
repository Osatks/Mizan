package com.osatks.mizan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import com.osatks.mizan.ui.theme.LocalDark
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.min
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings

/** [symbol]: رمز SF Symbols للتبويب في شريط النظام (iOS 26)، والمختار بنسخته الممتلئة ([symbol].fill) */
data class TabItem(val route: String, val label: (Strings) -> String, val on: ImageVector, val off: ImageVector, val symbol: String)

val TABS = listOf(
    TabItem("home", { it.tabHome }, Icons.Filled.Home, Icons.Outlined.Home, "house"),
    TabItem("prayer", { it.tabPrayer }, Icons.Filled.Schedule, Icons.Outlined.Schedule, "clock"),
    TabItem("quran", { it.tabQuran }, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "book"),
    TabItem("tasbih", { it.tabTasbih }, Icons.Filled.TouchApp, Icons.Outlined.TouchApp, "hand.tap"),
    TabItem("settings", { it.tabSettings }, Icons.Filled.Settings, Icons.Outlined.Settings, "gearshape"),
)

/** أقصى عرض لشريط التنقل السفلي: على iPad بالطول وما شابهه كبسولة في الوسط لا شريطاً بعرض الشاشة */
private val BarMax = 520.dp
private val BarIcon = 24.dp
/** هامشا أيقونة التبويب غير المختار (يضيقان على أضيق الهواتف حتى يتسع اسم المختار) */
private val PillPad = 14.dp
private val PillPadTight = 10.dp

/**
 * شريط تنقل عائم من زجاج: يُموَّه ما يمر خلفه من المحتوى، وحافته تلمع من الأعلى، وظله ناعم.
 * التبويب المختار "عدسة" زجاجية ملوّنة بلون التطبيق تتمدد لتحمل اسمه، والبقية أيقونات فقط.
 * التمدد والانكماش بنابض خفيف، والأيقونة "تقفز" لحظة الاختيار.
 * اسم التبويب يتسع دائماً: على الشاشات الضيقة (360 كالشاشة الخارجية لـ iPhone Duo) وفي اللغات الطويلة
 * تضيق هوامش الأيقونات ثم يصغر الخط قليلاً، فلا يُقص تبويب ولا يخرج عن الشريط.
 */
@Composable
fun FloatingTabBar(current: String?, haze: HazeState?, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    val dark = LocalDark.current
    val shape = RoundedCornerShape(percent = 50)
    // في iOS يجلس الشريط فوق مؤشر الشاشة الرئيسية مباشرة كشريط النظام، لا فوق المنطقة الآمنة كلها
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom = if (IsIos && nav > 20.dp) nav - 12.dp else nav + 10.dp
    val style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    val measurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    // أطول أسماء التبويبات بلغة الواجهة: يحدد هوامش الأيقونات مرة واحدة فلا تتبدل مع كل اختيار
    val longest = remember(s, style, density) {
        with(density) { TABS.maxOf { measurer.measure(it.label(s), style, maxLines = 1).size.width }.toDp() }
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val inner = min(maxWidth - 28.dp, BarMax) - 12.dp
        val others = TABS.size - 1
        fun room(pad: Dp) = inner - (BarIcon + pad * 2) * others - (pad * 2 + BarIcon + 8.dp)
        val pad = if (room(PillPad) >= longest) PillPad else PillPadTight
        val labelMax = room(pad).coerceIn(0.dp, 110.dp)
        Row(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = bottom)
                .widthIn(max = BarMax)
                .fillMaxWidth()
                .shadow(
                    18.dp, shape,
                    ambientColor = Color.Black.copy(alpha = if (dark) .45f else .10f),
                    spotColor = Color.Black.copy(alpha = if (dark) .55f else .20f),
                )
                .glass(haze, shape, glassStyle(if (dark) .52f else .56f, 24.dp))
                .padding(6.dp)
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TABS.forEach { tab ->
                val on = current == tab.route
                TabPill(tab.label(s), if (on) tab.on else tab.off, on, pad, labelMax, style) { onSelect(tab.route) }
            }
        }
    }
}

@Composable
private fun TabPill(label: String, icon: ImageVector, selected: Boolean, pad: Dp, labelMax: Dp, style: TextStyle, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val fg by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "tabFg"
    )
    // العدسة تظهر وتخفت بنابض بدل تبدّل اللون فجأة
    val lens by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .82f, stiffness = 520f), label = "tabLens")
    val bounce by animateFloatAsState(
        if (selected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "tabBounce"
    )
    val colors = lensColors(primary)
    Box(
        Modifier
            .height(48.dp)
            // الضغط يكبّر العدسة قليلاً كزجاج iOS، لا يصغّرها
            .pressScale(src, 1.07f)
            .semantics { this.selected = selected; role = Role.Tab }
            .drawWithCache { onDrawBehind { if (lens > 0.01f) drawLens(colors, lens, size.height / 2) } }
            .clip(CircleShape)
            .clickable(
                interactionSource = src, indication = if (IsIos) null else ripple(), role = Role.Tab,
                onClick = { if (!selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onClick() },
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = if (selected) null else label, tint = fg,
                modifier = Modifier.size(BarIcon).graphicsLayer {
                    // قفزة صغيرة عند الاختيار
                    val k = 1f + 0.18f * bounce * (1f - bounce) * 4f
                    scaleX = k; scaleY = k
                }
            )
            AnimatedVisibility(
                selected,
                enter = expandHorizontally(spring(dampingRatio = 0.8f, stiffness = 500f)) + fadeIn(tween(Motion.MEDIUM, 60)),
                exit = shrinkHorizontally(spring(dampingRatio = 1f, stiffness = 700f)) + fadeOut(tween(90)),
            ) {
                Row {
                    Spacer(Modifier.size(8.dp))
                    FitLabel(label, { fg }, style, 11.sp, Modifier.widthIn(max = labelMax))
                }
            }
        }
    }
}

/** اسم في سطر واحد يصغر خطه حتى يتسع (إلى [minSize])، ثم يُختصر بنقاط إن بقي أطول */
@Composable
internal fun FitLabel(text: String, color: () -> Color, style: TextStyle, minSize: TextUnit, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis,
        color = { color() },
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

/** ألوان عدسة التبويب المختار */
private class LensColors(val fill: Color, val sheen: Color, val rimTop: Color, val rimBottom: Color, val shade: Color)

@Composable
private fun lensColors(primary: Color): LensColors {
    val dark = LocalDark.current
    return remember(primary, dark) {
        LensColors(
            fill = primary.copy(alpha = if (dark) .24f else .13f),
            sheen = Color.White.copy(alpha = if (dark) .14f else .60f),
            rimTop = Color.White.copy(alpha = if (dark) .30f else 1f),
            rimBottom = Color.White.copy(alpha = if (dark) .06f else .45f),
            shade = Color.Black.copy(alpha = if (dark) .20f else .06f),
        )
    }
}

/** العدسة الزجاجية خلف التبويب المختار: ظل خفيف ولون التطبيق ولمعة علوية وحافة مضيئة */
private fun DrawScope.drawLens(c: LensColors, alpha: Float, radius: Float) {
    val r = CornerRadius(radius)
    val w = 1.dp.toPx()
    drawRoundRect(c.shade, Offset(0f, w), size, r, alpha = alpha)
    drawRoundRect(c.fill, cornerRadius = r, alpha = alpha)
    drawRoundRect(Brush.verticalGradient(0f to c.sheen, .6f to Color.Transparent), cornerRadius = r, alpha = alpha)
    drawRoundRect(
        Brush.verticalGradient(listOf(c.rimTop, c.rimBottom)), Offset(w / 2, w / 2), Size(size.width - w, size.height - w), r,
        alpha = alpha, style = Stroke(w)
    )
}

// ---------- واجهة شريط التنقل ----------

/**
 * شريط التنقل بين التبويبات خلف واجهة صغيرة: التبويب الحالي، والاختيار، والمساحة التي يشغلها.
 * يُرسم بـ Compose ([ComposeTabBar])، وفي iOS 26 فما بعده يحل محل الشريط السفلي شريط النظام نفسه
 * (UITabBarController بزجاج Liquid Glass، NativeTabBar في iosMain) يوفّره MainViewController عبر [LocalTabBarHost]،
 * دون أي تغيير في الصفحات: فهي تعرف مكانه من [LocalBottomInset] و[LocalLeadingInset] وحدهما.
 */
interface TabBarHost {
    /**
     * الشريط السفلي (الهاتف، والنوافذ المتوسطة بالطول). يُستدعى دائماً، و[visible] يُظهره ويخفيه
     * (صفحات التبويبات وحدها، لا فوق جولة أول تشغيل ولا مع القائمة الجانبية)، وحركة ظهوره واختفائه عليه هو.
     */
    @Composable
    fun Bottom(visible: Boolean, current: String?, modifier: Modifier, onSelect: (String) -> Unit)

    /** القائمة الجانبية في جهة البداية (iPad، والنوافذ الواسعة أو المتوسطة بالعرض)؛ [labels]: الأسماء تحت الأيقونات */
    @Composable
    fun Rail(current: String?, labels: Boolean, onSelect: (String) -> Unit)

    /** ما يُترك أسفل صفحات التبويبات للشريط السفلي */
    val bottomInset: Dp

    /** ما يُترك في جهة البداية من صفحات التبويبات للقائمة الجانبية (بعد المنطقة الآمنة) */
    val leadingInset: Dp
}

/** شريط التنقل الزجاجي المرسوم بـ Compose (الشريط السفلي العائم والقائمة الجانبية) */
object ComposeTabBar : TabBarHost {
    @Composable
    override fun Bottom(visible: Boolean, current: String?, modifier: Modifier, onSelect: (String) -> Unit) {
        AnimatedVisibility(
            visible, modifier,
            enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it } + fadeIn(),
            exit = slideOutVertically(tween(Motion.SHORT, easing = Motion.Accelerate)) { it } + fadeOut(),
        ) {
            FloatingTabBar(current, LocalAppHaze.current, onSelect)
        }
    }

    @Composable
    override fun Rail(current: String?, labels: Boolean, onSelect: (String) -> Unit) = FloatingNavRail(current, LocalAppHaze.current, labels, onSelect)

    override val bottomInset: Dp get() = 92.dp
    override val leadingInset: Dp get() = RailInset
}

val LocalTabBarHost = staticCompositionLocalOf<TabBarHost> { ComposeTabBar }

/**
 * حالة شريط النظام في iOS 26 (NativeTabBar في iosMain، ويقرؤها LiquidTabBarState.swift) نصاً JSON:
 * التبويبات بأسمائها ورموزها، والمختار (null في صفحة ليست تبويباً فيبقى اختيار الشريط كما هو)، والظهور،
 * واتجاه الكتابة، والوضع الداكن، ولون التطبيق (#RRGGBB).
 */
internal fun tabBarStateJson(s: Strings, current: String?, visible: Boolean, rtl: Boolean, dark: Boolean, accent: Color): String =
    com.osatks.mizan.i18n.pack.Json.write(
        mapOf(
            "tabs" to TABS.map { mapOf("route" to it.route, "title" to it.label(s), "symbol" to it.symbol) },
            "selected" to current?.takeIf { r -> TABS.any { it.route == r } },
            "visible" to visible,
            "rtl" to rtl,
            "dark" to dark,
            "accent" to "#" + (accent.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase(),
        )
    )

// ---------- قائمة التنقل الجانبية ----------

/** عرض قائمة التنقل الجانبية وبُعدها عن حافة الشاشة */
val RailWidth = 84.dp
val RailMargin = 12.dp

/** ما تشغله القائمة الجانبية من جهة البداية (بعد المنطقة الآمنة)؛ يضيف المحتوى بعده هامشه المعتاد */
val RailInset = RailMargin + RailWidth

/**
 * قائمة تنقل جانبية من الزجاج نفسه في جهة البداية (اليمين في العربية): على iPad والشاشات الواسعة،
 * وعلى الشاشات المتوسطة حين تكون بالعرض (الشاشة الداخلية لـ iPhone Duo بالعرض). كل تبويب أيقونة واسمه تحتها،
 * والمختار في العدسة الملوّنة نفسها التي في الشريط السفلي. في النوافذ القصيرة جداً تبقى الأيقونات وحدها.
 */
@Composable
fun FloatingNavRail(current: String?, haze: HazeState?, labels: Boolean, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    val dark = LocalDark.current
    val shape = RoundedCornerShape(RailWidth / 2)
    Column(
        Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .padding(start = RailMargin, top = 8.dp, bottom = 8.dp)
            .width(RailWidth)
            .shadow(
                18.dp, shape,
                ambientColor = Color.Black.copy(alpha = if (dark) .45f else .10f),
                spotColor = Color.Black.copy(alpha = if (dark) .55f else .20f),
            )
            .glass(haze, shape, glassStyle(if (dark) .52f else .56f, 24.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TABS.forEach { tab ->
            val on = current == tab.route
            RailItem(tab.label(s), if (on) tab.on else tab.off, on, labels) { onSelect(tab.route) }
        }
    }
}

@Composable
private fun RailItem(label: String, icon: ImageVector, selected: Boolean, showLabel: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary
    val fg by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "railFg"
    )
    val lens by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .82f, stiffness = 520f), label = "railLens")
    val bounce by animateFloatAsState(
        if (selected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "railBounce"
    )
    val colors = lensColors(primary)
    Box(
        Modifier
            .size(width = RailWidth - 14.dp, height = if (showLabel) 60.dp else 50.dp)
            .pressScale(src, 1.07f)
            .semantics { this.selected = selected; role = Role.Tab }
            .drawWithCache { onDrawBehind { if (lens > 0.01f) drawLens(colors, lens, 20.dp.toPx()) } }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = src, indication = if (IsIos) null else ripple(), role = Role.Tab,
                onClick = { if (!selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onClick() },
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon, contentDescription = if (showLabel) null else label, tint = fg,
                modifier = Modifier.size(BarIcon).graphicsLayer {
                    val k = 1f + 0.18f * bounce * (1f - bounce) * 4f
                    scaleX = k; scaleY = k
                }
            )
            if (showLabel) {
                Spacer(Modifier.height(3.dp))
                FitLabel(
                    label, { fg }, MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                    8.sp, Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

