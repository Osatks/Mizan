package com.osatks.mizan.ui.quran

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.DownloadState
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.RemoteTafsir
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.TafsirLibrary

/**
 * مكتبة التفاسير في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية؛ التنزيل والحذف والإظهار هنا كما في شاشة Compose
 * (TafsirLibraryScreen)، وSwift يرسم ويعيد اللمس
 */
@Composable
internal fun IosTafsirLibraryScreen(onBack: () -> Unit, startTab: String = "get") {
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

    NativeTafsirsView(
        content = linkedMapOf(
            "title" to s.tafsirLibrary, "subtitle" to s.tafsirLibrarySub,
            "tab" to tab, "tabs" to listOf("get" to s.download, "device" to s.onDevice).map { (k, n) -> linkedMapOf("key" to k, "name" to n) },
            "availableTitle" to s.availableDownloads, "deviceTitle" to s.onDevice, "note" to s.tafsirLibraryNote,
            "download" to s.download, "retry" to s.retry, "cancel" to s.cancel, "delete" to s.delete,
            "failed" to s.downloadFailed, "onDevice" to s.onDevice,
            "downloads" to catalog.map { r -> nativeDownloadRow(r, lang, s) },
            "installed" to installed.map { src ->
                linkedMapOf<String, Any?>(
                    "id" to src.id, "slug" to src.remote?.slug, "badge" to badge(src.lang),
                    "name" to Sources.label(src, s, lang),
                    "sub" to listOf(Sources.kindLabel(src, s), isolate(src.remote?.author(lang) ?: src.author)).joinToString(" · ") +
                        if (src.remote == null) " · ${s.builtIn}" else "",
                    "shown" to !QuranPrefs.isHidden(src.id),
                )
            },
        ),
        onAction = { k, x ->
            when {
                k == "tab" -> { if (x == "get" || x == "device") tab = x }
                k == "download" -> TafsirLibrary.bySlug(x)?.let { r -> TafsirLibrary.download(r.slug) { QuranPrefs.updateSource(r.id) } }
                k == "cancel" -> TafsirLibrary.cancel(x)
                k == "delete" -> { confirm = TafsirLibrary.bySlug(x) }
                k.startsWith("visible:") -> QuranPrefs.setVisible(k.removePrefix("visible:"), x == "true")
                else -> {}
            }
        },
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )

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

/** صف تفسير قابل للتنزيل لواجهة النظام: حالته (done، running، failed، idle) ونسبة تقدمه منسّقة كما في [DownloadRow] */
private fun nativeDownloadRow(r: RemoteTafsir, lang: Lang, s: com.osatks.mizan.i18n.Strings): Map<String, Any?> {
    val state = TafsirLibrary.states[r.slug]
    val done = (state as? DownloadState.Running)?.done ?: 0
    val progress = done / TafsirLibrary.SURAHS.toDouble()
    return linkedMapOf(
        "slug" to r.slug, "badge" to badge(r.lang), "name" to r.name(lang),
        "sub" to "${isolate(r.author(lang))} · ${isolate(s.sizeMb(Fmt.num(r.sizeMb.toDouble(), 1)))}",
        "state" to when {
            TafsirLibrary.isDownloaded(r.slug) -> "done"
            state is DownloadState.Running -> "running"
            state == DownloadState.Failed -> "failed"
            else -> "idle"
        },
        "progress" to progress, "pct" to s.downloadingPct((progress * 100).toInt()),
    )
}
