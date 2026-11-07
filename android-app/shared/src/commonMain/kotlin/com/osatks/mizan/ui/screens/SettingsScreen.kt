package com.osatks.mizan.ui.screens

import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.StoreLinks
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.osatks.mizan.ui.GlassGroup
import com.osatks.mizan.ui.GroupDivider
import com.osatks.mizan.ui.GroupRow
import com.osatks.mizan.ui.theme.heroBrush

@Composable
fun SettingsScreen(open: (String) -> Unit = {}) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var showLang by remember { mutableStateOf(false) }
    val toggleNotify = rememberAlertsToggle()
    val system = Lang.fromTag(systemLanguageTag())

    // مجموعات بأسلوب iOS (inset grouped): عنوان صغير فوق كل بطاقة زجاجية، وصفوفها مفصولة بخطوط رفيعة
    ToolPage(s.settingsTitle, spacing = 18.dp) {
        item {
            GlassGroup(s.language) {
                GroupRow(
                    lang.native,
                    if (AppSettings.lang == Lang.SYSTEM) "${s.deviceLanguage} · ${system.native}" else null,
                    onClick = { showLang = true },
                    leading = Icons.Filled.Translate
                ) { Chevron() }
            }
        }
        item {
            GlassGroup {
                GroupRow(s.appGuide, s.appGuideSub, onClick = { open("guide") }, leading = Icons.Filled.Explore) { Chevron() }
            }
        }
        item {
            GlassGroup(s.appearance) {
                Column(Modifier.padding(16.dp)) {
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
        }
        item {
            // مكتبة التفاسير: تنزيل التفاسير واختيار ما يظهر منها
            GlassGroup(s.quranTitle) {
                GroupRow(s.tafsirLibrary, s.tafsirLibrarySub, onClick = { open("tafsirs") }, leading = Icons.AutoMirrored.Filled.LibraryBooks) { Chevron() }
            }
        }
        item {
            GlassGroup(s.prayerTitle) {
                GroupRow(s.adhanAlert, s.adhanAlertDesc, onClick = { toggleNotify(!AppSettings.notify) }, leading = Icons.Filled.NotificationsActive) {
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                GroupDivider()
                GroupRow(s.adhanSettings, s.adhanSettingsSub, onClick = { open("adhan") }, leading = Icons.AutoMirrored.Filled.VolumeUp) { Chevron() }
                GroupDivider()
                GroupRow(s.use24h, s.use24hDesc, onClick = { AppSettings.updateUse24h(!AppSettings.use24h) }, leading = Icons.Filled.AccessTime) {
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.use24h, onCheckedChange = { AppSettings.updateUse24h(it) })
                }
                if (PrayerAlerts.hasExactAlarmSettings) {
                    GroupDivider()
                    GroupRow(s.exactAlarm, s.exactAlarmDesc, leading = Icons.Filled.Alarm, onClick = { PrayerAlerts.openExactAlarmSettings() })
                }
            }
        }
        item {
            // التقييم والمشاركة: كل نظام إلى متجره (Google Play أو App Store)
            GlassGroup {
                GroupRow(s.rateApp, s.rateAppSub, onClick = { AppStore.openRating() }, leading = Icons.Filled.StarRate) { Chevron() }
                GroupDivider()
                GroupRow(s.shareApp, s.shareAppSub, onClick = { Platform.share(s.shareAppText(StoreLinks.forThisPlatform), s.share) }, leading = Icons.Filled.Share) { Chevron() }
                GroupDivider()
                // الملاحظات: رسالة بريد فيها الإصدار والنظام
                val ht = com.osatks.mizan.i18n.HomeText.of(LocalLang.current.code)
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                val langCode = LocalLang.current.code
                GroupRow(ht.feedback, ht.feedbackSub, onClick = {
                    runCatching { uri.openUri(com.osatks.mizan.core.Feedback.mailto(langCode)) }
                        .onFailure { Platform.toast(com.osatks.mizan.core.Feedback.EMAIL) }
                }, leading = Icons.Filled.Email) { Chevron() }
            }
        }
        item {
            GlassGroup(s.about) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.aboutName, style = MaterialTheme.typography.titleMedium)
                    Text(s.version(Platform.versionName()), style = MaterialTheme.typography.bodySmall)
                    Text(s.aboutText, style = MaterialTheme.typography.bodyMedium)
                    LinkedText(s.quranSources)
                }
                GroupDivider(inset = 16.dp)
                // المصادر والتراخيص كاملة: المصحف والتفاسير والتلاوات وتسجيلات الأذان والخط
                GroupRow(s.licenses, s.licensesSub, onClick = { open("licenses") }, leading = Icons.Filled.Info) { Chevron() }
            }
        }
        item {
            GlassGroup(s.privacy) {
                Text(s.privacyText, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { Note(s.disclaimer, warn = true) }
    }

    if (showLang) LanguageSheet { showLang = false }
}

/** سهم الصف الذي يفتح صفحة (يتبع اتجاه اللغة) */
@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
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
