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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings

data class TabItem(val route: String, val label: (Strings) -> String, val on: ImageVector, val off: ImageVector)

val TABS = listOf(
    TabItem("home", { it.tabHome }, Icons.Filled.Home, Icons.Outlined.Home),
    TabItem("prayer", { it.tabPrayer }, Icons.Filled.Schedule, Icons.Outlined.Schedule),
    TabItem("quran", { it.tabQuran }, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TabItem("tasbih", { it.tabTasbih }, Icons.Filled.TouchApp, Icons.Outlined.TouchApp),
    TabItem("settings", { it.tabSettings }, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * شريط تنقل عائم من زجاج: يُموَّه ما يمر خلفه من المحتوى، وحافته تلمع من الأعلى، وظله ناعم.
 * التبويب المختار "عدسة" زجاجية ملوّنة بلون التطبيق تتمدد لتحمل اسمه، والبقية أيقونات فقط.
 * التمدد والانكماش بنابض خفيف، والأيقونة "تقفز" لحظة الاختيار.
 */
@Composable
fun FloatingTabBar(current: String?, haze: HazeState?, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    val dark = LocalDark.current
    val shape = RoundedCornerShape(percent = 50)
    // في iOS يجلس الشريط فوق مؤشر الشاشة الرئيسية مباشرة كشريط النظام، لا فوق المنطقة الآمنة كلها
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom = if (IsIos && nav > 20.dp) nav - 12.dp else nav + 10.dp
    Row(
        Modifier
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = bottom)
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
        TABS.forEach { tab -> TabPill(tab.label(s), if (current == tab.route) tab.on else tab.off, current == tab.route) { onSelect(tab.route) } }
    }
}

@Composable
private fun TabPill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val dark = LocalDark.current
    val primary = MaterialTheme.colorScheme.primary
    val fg by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "tabFg"
    )
    // العدسة تظهر وتخفت بنابض بدل تبدّل اللون فجأة
    val lens by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = .82f, stiffness = 520f), label = "tabLens")
    val bounce by animateFloatAsState(
        if (selected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "tabBounce"
    )
    val fill = primary.copy(alpha = if (dark) .24f else .13f)
    val sheen = Color.White.copy(alpha = if (dark) .14f else .60f)
    val rimTop = Color.White.copy(alpha = if (dark) .30f else 1f)
    val rimBottom = Color.White.copy(alpha = if (dark) .06f else .45f)
    val shade = Color.Black.copy(alpha = if (dark) .20f else .06f)
    Box(
        Modifier
            .height(48.dp)
            // الضغط يكبّر العدسة قليلاً كزجاج iOS، لا يصغّرها
            .pressScale(src, 1.07f)
            .semantics { this.selected = selected; role = Role.Tab }
            .drawWithCache {
                val r = CornerRadius(size.height / 2)
                val w = 1.dp.toPx()
                val sheenBrush = Brush.verticalGradient(0f to sheen, .6f to Color.Transparent)
                val rimBrush = Brush.verticalGradient(listOf(rimTop, rimBottom))
                onDrawBehind {
                    if (lens > 0.01f) {
                        drawRoundRect(shade, Offset(0f, w), size, r, alpha = lens)
                        drawRoundRect(fill, cornerRadius = r, alpha = lens)
                        drawRoundRect(sheenBrush, cornerRadius = r, alpha = lens)
                        drawRoundRect(rimBrush, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), r, alpha = lens, style = Stroke(w))
                    }
                }
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = src, indication = if (IsIos) null else ripple(), role = Role.Tab,
                onClick = { if (!selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick); onClick() },
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = if (selected) null else label, tint = fg,
                modifier = Modifier.size(24.dp).graphicsLayer {
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
                    Text(
                        label, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 110.dp)
                    )
                }
            }
        }
    }
}
