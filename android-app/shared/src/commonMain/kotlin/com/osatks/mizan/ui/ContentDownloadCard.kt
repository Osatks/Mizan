package com.osatks.mizan.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.content.ContentItem
import com.osatks.mizan.content.ContentStore
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalStrings

/**
 * محتوى بلغة الواجهة لم يُنزَّل بعد من موقع ميزان (ترجمة المعاني تحت الآيات، ترجمة حديث اليوم): [text] يشرح ذلك،
 * وزر «تنزيل (الحجم)» ثم شريط التقدم ونسبته، وإن تعذّر (بلا إنترنت) رسالة لطيفة وزر إعادة المحاولة.
 * يختفي وحده حين يكتمل التنزيل (من يعرضه يقرأ [ContentStore.missing]).
 */
@Composable
fun ContentDownloadCard(item: ContentItem, text: String, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val p = ContentStore.progress[item.key]
    Surface(
        shape = RoundedCornerShape(18.dp), color = cs.primaryContainer, contentColor = cs.onPrimaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Download, null, Modifier.size(20.dp), tint = cs.primary)
                Spacer(Modifier.width(10.dp))
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
            if (p != null && p >= 0) {
                LinearProgressIndicator(
                    progress = { p / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                    color = cs.primary, trackColor = cs.surface,
                )
                Text(s.downloadingPct(p), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            } else {
                val failed = p == -1
                if (failed) Text(s.downloadFailed, style = MaterialTheme.typography.labelSmall, color = cs.error)
                val src = remember { MutableInteractionSource() }
                Surface(
                    onClick = { ContentStore.start(item) }, shape = CircleShape, color = cs.surface, contentColor = cs.onSurface,
                    interactionSource = src, modifier = Modifier.pressScale(src, 0.94f),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (failed) Icons.Filled.Refresh else Icons.Filled.ArrowCircleDown, null, Modifier.size(18.dp), tint = cs.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (failed) s.retry else s.downloadSized(Fmt.num(item.sizeMb, 1)),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
