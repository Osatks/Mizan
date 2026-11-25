package com.osatks.mizan.ui.quran

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.content.ContentStore
import com.osatks.mizan.content.DownloadEntry
import com.osatks.mizan.content.Downloads
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.TafsirLibrary

/**
 * التنزيلات في iOS بواجهة النظام: شاشة مكتبة التفاسير نفسها (NativeTafsirs.swift) ببيانات [Downloads]، كما في
 * DownloadsScreen في أندرويد: «على الجهاز» ما نُزّل بحجمه الكلي مع حذف كل عنصر (ومنه صفحات المصحف)، و«تنزيل» ما يُنزَّل
 * من موقع ميزان بشريط تقدمه. مفتاح كل صف في Swift هو مفتاحه في [Downloads]، وتأكيد الحذف تنبيه النظام من Compose.
 */
@Composable
internal fun IosDownloadsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    @Suppress("UNUSED_VARIABLE") val v = ContentStore.version + TafsirLibrary.version
    val fonts = Madinah.fontsVersion
    val pages = remember(fonts, Madinah.downloading) { Madinah.downloadedCount() }
    val mushafBytes by produceState(0L, fonts, pages) { value = Downloads.mushafBytes() }
    val onDevice = Downloads.onDevice(s, lang, pages, mushafBytes)
    val total = onDevice.sumOf { it.bytes }
    var tab by rememberSaveable { mutableStateOf("device") }
    var confirm by remember { mutableStateOf<DownloadEntry?>(null) }

    NativeTafsirsView(
        content = linkedMapOf(
            "title" to s.downloadsTitle, "subtitle" to s.downloadsSub, "icon" to "arrow.down.circle.fill",
            "tab" to tab, "tabs" to listOf("device" to s.onDevice, "get" to s.download).map { (k, n) -> linkedMapOf("key" to k, "name" to n) },
            "availableTitle" to s.availableDownloads,
            "deviceTitle" to s.downloadsUsed(s.sizeMb(Fmt.num(total / 1_000_000.0, 1))),
            "note" to s.downloadsNote,
            "download" to s.download, "retry" to s.retry, "cancel" to s.cancel, "delete" to s.delete,
            "failed" to s.downloadFailed, "onDevice" to s.onDevice,
            "downloads" to Downloads.available(s, lang).map { e ->
                linkedMapOf<String, Any?>(
                    "slug" to e.key, "badge" to e.badge, "name" to e.name, "sub" to e.sub, "state" to e.state,
                    "progress" to e.progress.toDouble(), "pct" to s.downloadingPct((e.progress * 100).toInt()),
                )
            },
            "installed" to onDevice.map { e ->
                linkedMapOf<String, Any?>("id" to e.key, "slug" to e.key, "badge" to e.badge, "name" to e.name, "sub" to e.sub, "shown" to null)
            },
            "empty" to if (onDevice.isEmpty()) s.downloadsNone else null,
        ),
        onAction = { k, x ->
            when (k) {
                "tab" -> { if (x == "get" || x == "device") tab = x }
                "download" -> Downloads.download(x)
                "cancel" -> Downloads.cancel(x)
                "delete" -> { confirm = onDevice.firstOrNull { it.key == x } }
                else -> {}
            }
        },
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )

    confirm?.let { e ->
        com.osatks.mizan.ui.PlatformDialog(
            onDismissRequest = { confirm = null },
            title = Downloads.deleteQuestion(e, s),
            actions = listOf(
                com.osatks.mizan.ui.DialogAction(s.cancel, com.osatks.mizan.ui.ActionRole.Cancel) { confirm = null },
                com.osatks.mizan.ui.DialogAction(if (e.key == Downloads.MUSHAF) s.deletePages else s.delete, com.osatks.mizan.ui.ActionRole.Destructive) {
                    Downloads.delete(e.key); confirm = null
                },
            ),
        )
    }
}
