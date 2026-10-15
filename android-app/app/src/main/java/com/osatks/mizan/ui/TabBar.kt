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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    TabItem("qibla", { it.tabQibla }, Icons.Filled.Explore, Icons.Outlined.Explore),
    TabItem("tasbih", { it.tabTasbih }, Icons.Filled.TouchApp, Icons.Outlined.TouchApp),
    TabItem("settings", { it.tabSettings }, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * شريط تنقل عائم: التبويب المختار يتمدد كحبة تحمل اسمه، والبقية أيقونات فقط.
 * التمدد والانكماش بنابض خفيف، والأيقونة "تقفز" لحظة الاختيار.
 */
@Composable
fun FloatingTabBar(current: String?, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    Surface(
        modifier = Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp).height(52.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TABS.forEach { tab -> TabPill(tab.label(s), if (current == tab.route) tab.on else tab.off, current == tab.route) { onSelect(tab.route) } }
        }
    }
}

@Composable
private fun TabPill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "tabBg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "tabFg"
    )
    val bounce by animateFloatAsState(
        if (selected) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "tabBounce"
    )
    Surface(
        onClick = { if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        shape = CircleShape, color = bg, interactionSource = src,
        modifier = Modifier.height(48.dp).pressScale(src, 0.9f).semantics { this.selected = selected; role = Role.Tab }
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
