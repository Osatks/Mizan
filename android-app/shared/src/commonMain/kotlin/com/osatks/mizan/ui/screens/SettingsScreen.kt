package com.osatks.mizan.ui.screens

import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberNotifyToggle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.ClickRow
import com.osatks.mizan.ui.LinkedText
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.osatks.mizan.ui.theme.Accent
import com.osatks.mizan.ui.theme.heroBrush

@Composable
fun SettingsScreen(open: (String) -> Unit = {}) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var showLang by remember { mutableStateOf(false) }
    val toggleNotify = rememberNotifyToggle { ok -> AppSettings.updateNotify(ok); PrayerAlerts.reschedule() }
    val system = Lang.fromTag(systemLanguageTag())

    ToolPage(s.settingsTitle) {
        item {
            SectionCard(s.language) {
                ClickRow(
                    lang.native,
                    if (AppSettings.lang == Lang.SYSTEM) "${s.deviceLanguage} · ${system.native}" else null,
                    onClick = { showLang = true },
                    leading = Icons.Filled.Translate
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            SectionCard {
                ClickRow(s.appGuide, s.appGuideSub, onClick = { open("guide") }, leading = Icons.Filled.Explore) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard(s.appearance) {
                Segmented(listOf("system" to s.themeAuto, "light" to s.themeLight, "dark" to s.themeDark), AppSettings.theme) { AppSettings.updateTheme(it) }
                val accent = Accent.of(AppSettings.accent)
                Text(
                    "${s.appColor} · ${s.accentNames.getOrElse(accent.ordinal) { accent.key }}",
                    Modifier.padding(top = 16.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AccentPicker(accent, s.accentNames) { AppSettings.updateAccent(it.key) }
            }
        }
        item {
            // مكتبة التفاسير: تنزيل التفاسير واختيار ما يظهر منها
            SectionCard(s.quranTitle) {
                ClickRow(s.tafsirLibrary, s.tafsirLibrarySub, onClick = { open("tafsirs") }, leading = Icons.AutoMirrored.Filled.LibraryBooks) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard(s.prayerTitle) {
                ClickRow(s.adhanAlert, s.adhanAlertDesc, onClick = {}, leading = Icons.Filled.NotificationsActive) {
                    Switch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                ClickRow(s.use24h, s.use24hDesc, onClick = { AppSettings.updateUse24h(!AppSettings.use24h) }, leading = Icons.Filled.AccessTime) {
                    Switch(checked = AppSettings.use24h, onCheckedChange = { AppSettings.updateUse24h(it) })
                }
                if (PrayerAlerts.hasExactAlarmSettings) {
                    ClickRow(s.exactAlarm, s.exactAlarmDesc, leading = Icons.Filled.Alarm, onClick = { PrayerAlerts.openExactAlarmSettings() })
                }
            }
        }
        item {
            SectionCard(s.about) {
                Text(s.aboutName, style = MaterialTheme.typography.titleMedium)
                Text(s.version(Platform.versionName()), style = MaterialTheme.typography.bodySmall)
                Text(s.aboutText, style = MaterialTheme.typography.bodyMedium)
                LinkedText(s.quranSources)
            }
        }
        item {
            SectionCard(s.privacy) {
                Text(s.privacyText, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { Note(s.disclaimer, warn = true) }
    }

    if (showLang) LanguageSheet { showLang = false }
}

/** عشرة ألوان للتطبيق في صفين؛ اللون المختار عليه حلقة وعلامة صح */
@Composable
private fun AccentPicker(selected: Accent, names: List<String>, onPick: (Accent) -> Unit) {
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Accent.entries.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { a ->
                    val on = a == selected
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .selectable(on, role = Role.RadioButton) { onPick(a) }
                            .semantics { contentDescription = names.getOrElse(a.ordinal) { a.key } }
                            .border(2.dp, if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .padding(5.dp).clip(CircleShape).background(heroBrush(a)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}
