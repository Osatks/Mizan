package com.osatks.mizan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.platform.Platform
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.osatks.mizan.ui.PlatformDialog
import com.osatks.mizan.ui.DialogAction
import com.osatks.mizan.ui.ActionRole
import com.osatks.mizan.ui.theme.LocalDark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.City
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.country
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.toArgb
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.recolor

// ---------- أوراق النظام في iOS (iosApp/iosApp/NativeSheets.swift) ----------


private fun sheetHex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** اتجاه اللغة والمظهر وألوان التطبيق لأوراق النظام (بأسماء ألوان الرئيسية في Swift) */
@Composable
private fun nativeSheetTheme(): Map<String, Any?> {
    val cs = MaterialTheme.colorScheme
    val accent = LocalAccent.current
    return linkedMapOf(
        "rtl" to LocalLang.current.rtl, "dark" to LocalDark.current,
        "c" to linkedMapOf(
            "primary" to sheetHex(cs.primary), "primaryContainer" to sheetHex(cs.primaryContainer),
            "onPrimaryContainer" to sheetHex(cs.onPrimaryContainer), "background" to sheetHex(cs.background),
            "surface" to sheetHex(cs.surface), "onSurface" to sheetHex(cs.onSurface), "onSurfaceVariant" to sheetHex(cs.onSurfaceVariant),
            "outlineVariant" to sheetHex(cs.outlineVariant), "gold" to sheetHex(Gold), "goldDeep" to sheetHex(GoldDeep),
            "hero1" to sheetHex(BrandGreen.recolor(accent)), "hero2" to sheetHex(BrandGreenDeep.recolor(accent)),
        ),
        "errorColor" to sheetHex(cs.error),
    )
}

/** ورقة اللغات من النظام: ما في [LanguageList] نفسه (المثبتة، ثم المتاحة للتنزيل وتقدّمها، والحذف بتأكيد) */
@Composable
actual fun LanguageSheet(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val ui = LocalLang.current
    val scope = rememberCoroutineScope()
    val system = Lang.fromTag(systemLanguageTag())
    var confirmDelete by remember { mutableStateOf<Lang?>(null) }
    fun displayName(code: String, fallback: String) =
        Platform.displayLanguage(code, ui.code).takeIf { it.isNotEmpty() && it != code }
            ?.replaceFirstChar { it.titlecase() } ?: fallback
    LaunchedEffect(Unit) { if (LanguagePacks.remote == null) LanguagePacks.refresh() }
    val rows = listOf(
        linkedMapOf<String, Any?>("code" to Lang.SYSTEM, "title" to s.deviceLanguage, "sub" to system.native, "selected" to (AppSettings.lang == Lang.SYSTEM), "removable" to false)
    ) + Lang.entries.map { l ->
        linkedMapOf<String, Any?>(
            "code" to l.code, "title" to l.native,
            "sub" to (if (l == ui) null else displayName(l.code, l.pack?.english ?: l.code)),
            "selected" to (AppSettings.lang == l.code),
            "removable" to (l.isPack && LanguagePacks.isDownloaded(l.code) && !LanguagePacks.isBundled(l.code) && AppSettings.lang != l.code),
        )
    }
    val offers = LanguagePacks.offers().map { r ->
        val pct = LanguagePacks.progress[r.code]
        linkedMapOf<String, Any?>(
            "code" to r.code, "title" to r.native,
            "sub" to displayName(r.code, r.english) + (if (r.sizeMb > 0) " · " + s.sizeMb(Fmt.num(r.sizeMb.toDouble(), 1, ui)) else ""),
            "pct" to pct, "pctText" to (if (pct != null && pct >= 0) "$pct%" else null),
            "update" to (Lang.of(r.code) != null),
        )
    }
    val json = Json.write(
        nativeSheetTheme() + linkedMapOf(
            "title" to s.language, "close" to s.close, "more" to s.moreLanguages, "hint" to s.langPackHint,
            "checking" to LanguagePacks.checking, "failed" to (if (LanguagePacks.checkFailed) s.downloadFailed else null),
            "retryLabel" to s.retry, "updateLabel" to s.update, "downloadLabel" to s.download, "deleteLabel" to s.delete,
            "rows" to rows, "offers" to offers,
        )
    )
    NativeSheetHost("language", json, startLarge = false, onDismiss = onDismiss) { action, arg ->
        // يُغلق الورقة بحركتها ثم يبدّل اللغة (فيُعاد تلوين التطبيق)، كما في ورقة Compose
        fun NativeSheetControl.pick(code: String) = close { onDismiss(); if (code != AppSettings.lang) LocaleSync.choose(code) }
        when (action) {
            "pick" -> pick(arg)
            "close" -> close(onDismiss)
            "delete" -> { confirmDelete = Lang.of(arg) }
            "retry" -> { scope.launch { LanguagePacks.refresh() } }
            "download" -> {
                val r = LanguagePacks.offers().firstOrNull { it.code == arg }
                val pct = r?.let { LanguagePacks.progress[it.code] }
                if (r != null && (pct == null || pct < 0)) {
                    val isUpdate = Lang.of(r.code) != null
                    LanguagePacks.start(r) { ok -> if (ok && !isUpdate) pick(r.code) }
                }
            }
            else -> {}
        }
    }
    // التأكيد نافذة تنبيه النظام فوق الورقة
    confirmDelete?.let { l ->
        PlatformDialog(
            onDismissRequest = { confirmDelete = null },
            title = s.deleteTafsirQ(l.native),
            actions = listOf(
                DialogAction(s.cancel, ActionRole.Cancel) { confirmDelete = null },
                DialogAction(s.delete, ActionRole.Destructive) { LanguagePacks.delete(l.code); confirmDelete = null },
            ),
        )
    }
}

/** اختيار المدينة من النظام: البحث نفسه في [CityPickerSheet] (اسم المدينة بلغة الواجهة والإنجليزية والعربية، والدولة) */
@Composable
actual fun CityPickerSheet(selectedKey: String?, onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var q by remember { mutableStateOf("") }
    val all = remember(lang) {
        PrayTimes.CITIES.map { c ->
            val country = s.country(c.cc, lang)
            Triple(c, country, Search.normalize("${s.city(c.key)} ${En.city(c.key)} ${Ar.city(c.key)} $country ${c.cc}"))
        }
    }
    val shown = remember(q, all) {
        val terms = Search.normalize(q).split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) all else all.filter { (_, _, hay) -> terms.all { hay.contains(it) } }
    }
    val json = Json.write(
        nativeSheetTheme() + linkedMapOf(
            "title" to s.pickCity, "search" to s.searchCity, "close" to s.close, "empty" to s.noResults,
            "rows" to shown.map { (c, country, _) ->
                linkedMapOf("key" to c.key, "city" to s.city(c.key), "country" to country, "selected" to (c.key == selectedKey))
            },
        )
    )
    NativeSheetHost("city", json, startLarge = true, onDismiss = onDismiss) { action, arg ->
        when (action) {
            "query" -> { q = arg }
            "pick" -> { all.firstOrNull { it.first.key == arg }?.first?.let { c -> close { onDismiss(); onPick(c) } } }
            "close" -> close(onDismiss)
            else -> {}
        }
    }
}
