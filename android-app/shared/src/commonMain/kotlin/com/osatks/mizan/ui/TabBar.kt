package com.osatks.mizan.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import com.osatks.mizan.i18n.Strings

/** [symbol]: رمز SF Symbols للتبويب في شريط النظام (iOS 26)، ويُعرض بنسخته الممتلئة ([symbol].fill) كما في تطبيقات Apple */
data class TabItem(val route: String, val label: (Strings) -> String, val on: ImageVector, val off: ImageVector, val symbol: String)

val TABS = listOf(
    TabItem("home", { it.tabHome }, Icons.Filled.Home, Icons.Outlined.Home, "house"),
    TabItem("prayer", { it.tabPrayer }, Icons.Filled.Schedule, Icons.Outlined.Schedule, "clock"),
    TabItem("quran", { it.tabQuran }, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "book"),
    // الأدوات (الحاسبات والتواريخ والمسبحة…) في تبويب خاص؛ والمسبحة بلمسة من الرئيسية
    TabItem("tools", { it.toolsTitle }, Icons.Filled.GridView, Icons.Outlined.GridView, "square.grid.2x2"),
    TabItem("settings", { it.tabSettings }, Icons.Filled.Settings, Icons.Outlined.Settings, "gearshape"),
)

/** اسم في سطر واحد يصغر خطه حتى يتسع (إلى [minSize])، ثم يُختصر بنقاط إن بقي أطول */
@Composable
internal fun FitLabel(text: String, color: () -> Color, style: TextStyle, minSize: TextUnit, modifier: Modifier = Modifier) {
    BasicText(
        text, modifier, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis,
        color = { color() },
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

/**
 * حالة شريط النظام في iOS (IosNav.kt في iosMain، ويقرؤها LiquidTabBarState.swift) نصاً JSON:
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
