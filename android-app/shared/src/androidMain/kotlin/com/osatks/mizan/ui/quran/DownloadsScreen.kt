package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.content.ContentStore
import com.osatks.mizan.content.DownloadEntry
import com.osatks.mizan.content.Downloads
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.TafsirLibrary
import com.osatks.mizan.ui.ActionRole
import com.osatks.mizan.ui.DialogAction
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.PlatformDialog
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage

/**
 * التنزيلات (من الإعدادات): «على الجهاز» ما نُزّل بحجمه الكلي، مع حذف كل عنصر (ومنه صفحات المصحف المنزَّلة)،
 * و«تنزيل» ما يُنزَّل من موقع ميزان من ترجمات المعاني والتفاسير وأحاديث اليوم والأذكار بشريط تقدمه.
 * البيانات من [Downloads] (مشتركة مع iOS)، والرسم بطراز مكتبة التفاسير.
 */
@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    // يُقرأ ما يتغير بعد كل تنزيل أو حذف فتُحدَّث القوائم
    @Suppress("UNUSED_VARIABLE") val v = ContentStore.version + TafsirLibrary.version
    val fonts = Madinah.fontsVersion
    val pages = remember(fonts, Madinah.downloading) { Madinah.downloadedCount() }
    val mushafBytes by produceState(0L, fonts, pages) { value = Downloads.mushafBytes() }
    val onDevice = Downloads.onDevice(s, lang, pages, mushafBytes)
    val total = onDevice.sumOf { it.bytes }
    var tab by rememberSaveable { mutableStateOf("device") }
    var confirm by remember { mutableStateOf<DownloadEntry?>(null) }

    ToolPage(s.downloadsTitle, s.downloadsSub, onBack = onBack, icon = Icons.Filled.Download, spacing = 10.dp) {
        item(key = "tabs") { Segmented(listOf("device" to s.onDevice, "get" to s.download), tab) { tab = it } }
        if (tab == "device") {
            item(key = "h-device") { SectionTitle(s.downloadsUsed(s.sizeMb(Fmt.num(total / 1_000_000.0, 1)))) }
            if (onDevice.isEmpty()) item(key = "none") { Note(s.downloadsNone) }
            onDevice.groupBy { it.group }.forEach { (g, list) ->
                item(key = "g-$g") { GroupLabel(Downloads.groupTitle(g, s)) }
                list.forEach { e -> item(key = "i-${e.key}") { DeviceRow(e) { confirm = e } } }
            }
        } else {
            item(key = "h-get") { SectionTitle(s.availableDownloads) }
            Downloads.available(s, lang).groupBy { it.group }.forEach { (g, list) ->
                item(key = "dg-$g") { GroupLabel(Downloads.groupTitle(g, s)) }
                list.forEach { e -> item(key = "d-${e.key}") { GetRow(e) } }
            }
            item(key = "note") { Note(s.downloadsNote) }
        }
    }

    confirm?.let { e ->
        PlatformDialog(
            onDismissRequest = { confirm = null },
            title = Downloads.deleteQuestion(e, s),
            actions = listOf(
                DialogAction(s.cancel, ActionRole.Cancel) { confirm = null },
                DialogAction(if (e.key == Downloads.MUSHAF) s.deletePages else s.delete, ActionRole.Destructive) {
                    Downloads.delete(e.key); confirm = null
                },
            ),
        )
    }
}

/** عنوان قسم صغير (الترجمات، الأحاديث، الأذكار، الأذان، المصحف) */
@Composable
private fun GroupLabel(t: String) {
    Text(
        t, Modifier.padding(start = 8.dp, top = 6.dp), style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
    )
}

/** شارة الصف: رمز اللغة، أو رمز الأذان وصفحات المصحف */
@Composable
private fun EntryBadge(text: String) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
    }
}

@Composable
private fun EntryTexts(e: DownloadEntry, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp)) {
        Text(e.name, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(e.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** ما على الجهاز: زر الحذف (وللمصحف «حذف الصفحات المنزَّلة») */
@Composable
private fun DeviceRow(e: DownloadEntry, onDelete: () -> Unit) {
    val s = LocalStrings.current
    RowCard {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            EntryBadge(e.badge)
            EntryTexts(e, Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, if (e.key == Downloads.MUSHAF) s.deletePages else s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** ما يُنزَّل من موقع ميزان: ✓ على الجهاز، أو إلغاء مع شريط التقدم، أو تنزيل / إعادة محاولة */
@Composable
private fun GetRow(e: DownloadEntry) {
    val s = LocalStrings.current
    val progress by animateFloatAsState(e.progress, tween(400), label = "dl")
    RowCard {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntryBadge(e.badge)
                EntryTexts(e, Modifier.weight(1f))
                when (e.state) {
                    "done" -> Icon(Icons.Filled.CheckCircle, s.onDevice, Modifier.padding(horizontal = 12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    "running" -> TextButton(onClick = { Downloads.cancel(e.key) }) { Text(s.cancel, fontWeight = FontWeight.SemiBold) }
                    else -> DownloadPill(e.state == "failed") { Downloads.download(e.key) }
                }
            }
            AnimatedVisibility(e.state == "running") {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {}
                    )
                    Text(s.downloadingPct((progress * 100).toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (e.state == "failed") Text(
                s.downloadFailed, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error
            )
        }
    }
}
