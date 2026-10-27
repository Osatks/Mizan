package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.platform.Platform
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.glass
import com.osatks.mizan.ui.glassRim
import com.osatks.mizan.ui.glassStyle
import com.osatks.mizan.ui.theme.LocalDark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.osatks.mizan.platform.systemLanguageTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

/**
 * اختيار اللغة: كل لغة باسمها الأصلي وتحته اسمها بلغة الواجهة.
 * وتحتها اللغات المتاحة للتنزيل من موقع ميزان (حزم اللغات)، وتحديث ما صدر له إصدار أحدث.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GlassSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        LanguageList { code ->
            scope.launch { sheet.hide() }.invokeOnCompletion {
                onDismiss()
                if (code != AppSettings.lang) LocaleSync.choose(code)
            }
        }
    }
}

/** محتوى ورقة اللغات: المثبتة، ثم المتاحة للتنزيل */
@Composable
internal fun LanguageList(state: LazyListState = rememberLazyListState(), pick: (String) -> Unit) {
    val s = LocalStrings.current
    val ui = LocalLang.current
    val scope = rememberCoroutineScope()
    val system = Lang.fromTag(systemLanguageTag())
    var confirmDelete by remember { mutableStateOf<Lang?>(null) }
    fun displayName(code: String, fallback: String) =
        Platform.displayLanguage(code, ui.code).takeIf { it.isNotEmpty() && it != code }
            ?.replaceFirstChar { it.titlecase() } ?: fallback
    LaunchedEffect(Unit) { if (LanguagePacks.remote == null) LanguagePacks.refresh() }
    Column {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Translate, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(s.language, style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(state = state, contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 32.dp)) {
            item {
                OptionRow(s.deviceLanguage, system.native, AppSettings.lang == Lang.SYSTEM) { pick(Lang.SYSTEM) }
            }
            items(Lang.entries, key = { it.code }) { l ->
                val sub = displayName(l.code, l.pack?.english ?: l.code)
                val removable = l.isPack && LanguagePacks.isDownloaded(l.code) && !LanguagePacks.isBundled(l.code) && AppSettings.lang != l.code
                OptionRow(l.native, if (l == ui) null else sub, AppSettings.lang == l.code,
                    trailing = if (removable) { { IconButton(onClick = { confirmDelete = l }) { Icon(Icons.Outlined.Delete, s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } } else null,
                ) { pick(l.code) }
            }
            // ---------- لغات للتنزيل ----------
            item(key = "more") {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CloudDownload, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(s.moreLanguages, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (LanguagePacks.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                Text(s.langPackHint, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (LanguagePacks.checkFailed) item(key = "failed") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.downloadFailed, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { scope.launch { LanguagePacks.refresh() } }) { Text(s.retry) }
                }
            }
            items(LanguagePacks.offers(), key = { "dl:" + it.code }) { r ->
                val pct = LanguagePacks.progress[r.code]
                val isUpdate = Lang.of(r.code) != null
                fun go() = LanguagePacks.start(r) { ok -> if (ok && !isUpdate) pick(r.code) }
                val sub = displayName(r.code, r.english) + (if (r.sizeMb > 0) " · " + s.sizeMb(Fmt.num(r.sizeMb.toDouble(), 1, ui)) else "")
                OptionRow(r.native, sub, false, trailing = {
                    when {
                        pct != null && pct >= 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$pct%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(progress = { pct / 100f }, modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                        }
                        pct == -1 -> TextButton(onClick = ::go) { Text(s.retry, color = MaterialTheme.colorScheme.error) }
                        isUpdate -> TextButton(onClick = ::go) { Text(s.update) }
                        else -> IconButton(onClick = ::go) { Icon(Icons.Filled.Download, s.download, tint = MaterialTheme.colorScheme.primary) }
                    }
                }) { if (pct == null || pct < 0) go() }
            }
        }
    }
    confirmDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            shape = RoundedCornerShape(30.dp), containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(s.deleteTafsirQ(l.native)) },
            confirmButton = { TextButton(onClick = { LanguagePacks.delete(l.code); confirmDelete = null }) { Text(s.delete, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, selected: Boolean, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    // الصفوف شفافة فوق الزجاج، والمختار منها بلون التطبيق الخفيف
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .13f) else Color.Transparent, label = "opt")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (trailing != null) 6.dp else 16.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
        AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.5f)), exit = scaleOut()) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * ورقة سفلية زجاجية بأسلوب iOS: زوايا عليا مستديرة 32، ومقبض صغير في أعلاها، وسطحها يموّه الصفحة التي خلفها
 * (من مصدر التطبيق [LocalAppHaze]) بطبقة من لون السطح كثيفة تكفي لقراءة النص فوق أي محتوى.
 * التمويه بثلث الدقة ونصف قطر محدود؛ ويمتد السطح تحت شريط التنقل السفلي ثم يُزاح المحتوى عنه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(onDismissRequest: () -> Unit, sheetState: SheetState, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalDark.current
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState, shape = shape,
        containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = if (dark) .45f else .25f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier.fillMaxWidth()
                .glass(LocalAppHaze.current, shape, glassStyle(if (dark) .80f else .82f, 32.dp, fallback = 1f), glassRim(.8f))
                .navigationBarsPadding()
        ) {
            // المقبض: كبسولة صغيرة رمادية كما في أوراق iOS
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 14.dp)
                    .size(width = 36.dp, height = 5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) .32f else .22f))
            )
            content()
        }
    }
}

/** اختيار مدينة مع بحث فوري بأي لغة (اسم المدينة أو الدولة) */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CityPickerSheet(selectedKey: String?, onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
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
    GlassSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxHeight(0.92f).imePadding()) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text(s.searchCity) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, null) } },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            if (shown.isEmpty()) {
                Text(s.noResults, Modifier.fillMaxWidth().padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 24.dp)) {
                items(shown, key = { it.first.key }) { (c, country, _) ->
                    Row(
                        Modifier.animateItem().fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (c.key == selectedKey) MaterialTheme.colorScheme.primary.copy(alpha = .13f) else Color.Transparent)
                            .clickable {
                                scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); onPick(c) }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationCity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.city(c.key), fontWeight = FontWeight.Bold)
                            Text(country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (c.key == selectedKey) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
