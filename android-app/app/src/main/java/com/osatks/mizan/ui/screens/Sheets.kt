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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

/** اختيار اللغة: كل لغة باسمها الأصلي وتحته اسمها بلغة الواجهة */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val ui = LocalLang.current
    val ctx = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val system = Lang.fromLocale(LocalConfiguration.current.locales[0])
    fun pick(code: String) {
        scope.launch { sheet.hide() }.invokeOnCompletion {
            onDismiss()
            if (code != AppSettings.lang) LocaleSync.choose(ctx, code)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Translate, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(s.language, style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 32.dp)) {
            item {
                OptionRow(s.deviceLanguage, system.native, AppSettings.lang == Lang.SYSTEM) { pick(Lang.SYSTEM) }
            }
            items(Lang.entries.toList(), key = { it.code }) { l ->
                val sub = l.locale.getDisplayLanguage(ui.locale).replaceFirstChar { it.titlecase(ui.locale) }
                OptionRow(l.native, if (l == ui) null else sub, AppSettings.lang == l.code) { pick(l.code) }
            }
        }
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "opt")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.5f)), exit = scaleOut()) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
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
                            .background(if (c.key == selectedKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
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
