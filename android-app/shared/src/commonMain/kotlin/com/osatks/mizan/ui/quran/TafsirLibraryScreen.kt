package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Download
import com.osatks.mizan.ui.Segmented
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.DownloadState
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.RemoteTafsir
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.TafsirLibrary
import com.osatks.mizan.quran.TafsirSource
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ToolPage

/**
 * مكتبة التفاسير: ما على الجهاز (المدمج والمنزَّل) مع مفتاح لإظهاره في ورقة التفسير أو إخفائه وحذف المنزَّل،
 * وما يمكن تنزيله (لغة الواجهة أولاً ثم العربية) مع حجمه، وشريط تقدم أثناء التنزيل، وإلغاء وإعادة محاولة.
 */
@Composable
fun TafsirLibraryScreen(onBack: () -> Unit, startTab: String = "get") {
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    @Suppress("UNUSED_VARIABLE") val v = TafsirLibrary.version
    val installed = Sources.ordered(lang)
    // كل التفاسير القابلة للتنزيل (لغة الواجهة أولاً ثم العربية)؛ ما نُزّل منها يبقى في القائمة بعلامة ✓
    val catalog = TafsirLibrary.CATALOG.let { list ->
        list.filter { it.lang == lang } + list.filter { it.lang == Lang.AR && lang != Lang.AR } + list.filter { it.lang != lang && it.lang != Lang.AR }
    }
    var tab by rememberSaveable { mutableStateOf(startTab) }
    var confirm by remember { mutableStateOf<RemoteTafsir?>(null) }

    ToolPage(s.tafsirLibrary, s.tafsirLibrarySub, onBack = onBack, icon = Icons.AutoMirrored.Filled.LibraryBooks, spacing = 10.dp) {
        item(key = "tabs") { Segmented(listOf("get" to s.download, "device" to s.onDevice), tab) { tab = it } }
        if (tab == "get") {
            item(key = "h-get") { SectionTitle(s.availableDownloads) }
            catalog.forEach { r -> item(key = "d-${r.slug}") { DownloadRow(r, lang) } }
            item(key = "note") { Note(s.tafsirLibraryNote) }
        } else {
            item(key = "h-device") { SectionTitle(s.onDevice) }
            installed.forEach { src -> item(key = "i-${src.id}") { InstalledRow(src, lang) { confirm = src.remote } } }
        }
    }

    confirm?.let { r ->
        com.osatks.mizan.ui.PlatformDialog(
            onDismissRequest = { confirm = null },
            title = s.deleteTafsirQ(r.name(lang)),
            actions = listOf(
                com.osatks.mizan.ui.DialogAction(s.cancel, com.osatks.mizan.ui.ActionRole.Cancel) { confirm = null },
                com.osatks.mizan.ui.DialogAction(s.delete, com.osatks.mizan.ui.ActionRole.Destructive) { TafsirLibrary.delete(r.slug); confirm = null },
            ),
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, Modifier.padding(start = 6.dp, top = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

/** شارة اللغة: «ع» للعربية، ورمز اللغة لغيرها */
@Composable
private fun LangBadge(l: Lang) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (l == Lang.AR) "ع" else l.code.uppercase(), style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun RowCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)), modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun InstalledRow(src: TafsirSource, lang: Lang, onDelete: () -> Unit) {
    val s = LocalStrings.current
    val shown = !QuranPrefs.isHidden(src.id)
    RowCard {
        Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            LangBadge(src.lang)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(Sources.label(src, s, lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(Sources.kindLabel(src, s), isolate(src.remote?.author(lang) ?: src.author)).joinToString(" · ") + if (src.remote == null) " · ${s.builtIn}" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            if (src.remote != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(checked = shown, onCheckedChange = { QuranPrefs.setVisible(src.id, it) })
        }
    }
}

@Composable
private fun DownloadRow(r: RemoteTafsir, lang: Lang) {
    val s = LocalStrings.current
    val state = TafsirLibrary.states[r.slug]
    val running = state as? DownloadState.Running
    val progress by animateFloatAsState((running?.done ?: 0) / TafsirLibrary.SURAHS.toFloat(), tween(400), label = "download")
    RowCard {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LangBadge(r.lang)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(r.name(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${isolate(r.author(lang))} · ${isolate(s.sizeMb(Fmt.num(r.sizeMb.toDouble(), 1)))}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    TafsirLibrary.isDownloaded(r.slug) -> Icon(
                        Icons.Filled.CheckCircle, s.onDevice, Modifier.padding(horizontal = 12.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary
                    )
                    state is DownloadState.Running -> TextButton(onClick = { TafsirLibrary.cancel(r.slug) }) { Text(s.cancel) }
                    else -> FilledTonalButton(onClick = { TafsirLibrary.download(r.slug) { QuranPrefs.updateSource(r.id) } }) {
                        Icon(if (state == DownloadState.Failed) Icons.Filled.Refresh else Icons.Filled.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (state == DownloadState.Failed) s.retry else s.download, maxLines = 1)
                    }
                }
            }
            AnimatedVisibility(running != null) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        drawStopIndicator = {}
                    )
                    Text(s.downloadingPct((progress * 100).toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state == DownloadState.Failed) Text(
                s.downloadFailed, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** يعزل النص باتجاهه (FSI…PDI) حتى لا يختلط ترتيبه مع ما حوله إذا اختلفت اللغتان */
private fun isolate(t: String) = "\u2068$t\u2069"
