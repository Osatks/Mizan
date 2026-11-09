package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.i18n.LocalStrings
import androidx.compose.foundation.layout.BoxWithConstraints
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.sharedIcon

/**
 * تبويب الأدوات: الحاسبات والتواريخ والمسبحة والأذكار والقبلة في شبكة، عمودان على الهاتف وثلاثة في العرض المتوسط
 * وأربعة في الواسع
 */
@Composable
fun ToolsScreen(open: (String) -> Unit) {
    val s = LocalStrings.current
    // ترويسة بمربع أيقونة وعنوان وشعار التطبيق كما في iPhone
    com.osatks.mizan.ui.ToolPage(s.toolsTitle, s.appTagline, icon = Icons.Filled.GridView) {
        item(key = "tools") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cols = when { maxWidth >= 840.dp -> 4; maxWidth >= 560.dp -> 3; else -> 2 }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TOOL_ORDER.chunked(cols).forEach { chunk ->
                        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { key -> ToolTile(key, Modifier.weight(1f)) { open(key) } }
                            repeat(cols - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTile(key: String, modifier: Modifier, onClick: () -> Unit) {
    val s = LocalStrings.current
    val t = s.tools.getValue(key)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f)),
        shadowElevation = 1.dp,
        interactionSource = src,
        modifier = modifier.sharedCard("card-$key", RoundedCornerShape(20.dp)).fillMaxHeight().heightIn(min = 150.dp).pressScale(src)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.sharedIcon("icon-$key").size(46.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = .16f)),
                contentAlignment = Alignment.Center
            ) { Icon(TOOL_ICONS.getValue(key), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(t.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(t.desc, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

