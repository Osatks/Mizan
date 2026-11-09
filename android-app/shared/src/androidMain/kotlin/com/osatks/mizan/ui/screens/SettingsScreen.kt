package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.StoreLinks
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.ui.LinkedText
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.theme.Accent
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.heroBrush
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SettingsScreen(open: (String) -> Unit = {}) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var showLang by remember { mutableStateOf(false) }
    val toggleNotify = rememberAlertsToggle()

    val system = Lang.fromTag(systemLanguageTag())

    // بطراز الرئيسية كما في iPhone: ترويسة فاتحة بأيقونة الترس، ثم مجموعات بطاقات بيضاء بعنوان صغير فوق كل منها
    ToolPage(s.settingsTitle, icon = Icons.Filled.Settings, plainIcon = true, spacing = 20.dp) {
        item {
            SettingsGroup(s.language) {
                SettingsRow(
                    Icons.Filled.Translate, lang.native,
                    if (AppSettings.lang == Lang.SYSTEM) "${s.deviceLanguage} · ${system.native}" else null,
                    onClick = { showLang = true }
                ) { SettingsChevron() }
            }
        }
        item {
            SettingsGroup {
                SettingsRow(Icons.Filled.Explore, s.appGuide, s.appGuideSub, onClick = { open("guide") }) { SettingsChevron() }
            }
        }
        item {
            SettingsGroup(s.appearance) {
                Column(Modifier.padding(16.dp)) {
                    Segmented(listOf("system" to s.themeAuto, "light" to s.themeLight, "dark" to s.themeDark), AppSettings.theme) { AppSettings.updateTheme(it) }
                    val accent = Accent.of(AppSettings.accent)
                    Text(
                        "${s.appColor} · ${s.accentNames.getOrElse(accent.ordinal) { accent.key }}",
                        Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AccentPicker(accent, s.accentNames) { AppSettings.updateAccent(it.key) }
                }
            }
        }
        item {
            // مكتبة التفاسير: تنزيل التفاسير واختيار ما يظهر منها
            SettingsGroup(s.quranTitle) {
                SettingsRow(Icons.AutoMirrored.Filled.LibraryBooks, s.tafsirLibrary, s.tafsirLibrarySub, onClick = { open("tafsirs") }) { SettingsChevron() }
            }
        }
        item {
            SettingsGroup(s.prayerTitle) {
                SettingsRow(Icons.Filled.NotificationsActive, s.adhanAlert, s.adhanAlertDesc, onClick = { toggleNotify(!AppSettings.notify) }) {
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                SettingsDivider()
                SettingsRow(Icons.AutoMirrored.Filled.VolumeUp, s.adhanSettings, s.adhanSettingsSub, onClick = { open("adhan") }) { SettingsChevron() }
                SettingsDivider()
                SettingsRow(Icons.Filled.AccessTime, s.use24h, s.use24hDesc, onClick = { AppSettings.updateUse24h(!AppSettings.use24h) }) {
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.use24h, onCheckedChange = { AppSettings.updateUse24h(it) })
                }
                if (PrayerAlerts.hasExactAlarmSettings) {
                    SettingsDivider()
                    SettingsRow(Icons.Filled.Alarm, s.exactAlarm, s.exactAlarmDesc, onClick = { PrayerAlerts.openExactAlarmSettings() })
                }
            }
        }
        item {
            // التقييم والمشاركة: كل نظام إلى متجره (Google Play أو App Store)
            SettingsGroup {
                SettingsRow(Icons.Filled.Star, s.rateApp, s.rateAppSub, onClick = { AppStore.openRating() }) { SettingsChevron() }
                SettingsDivider()
                SettingsRow(Icons.Filled.IosShare, s.shareApp, s.shareAppSub, onClick = { Platform.share(s.shareAppText(StoreLinks.forThisPlatform), s.share) }) { SettingsChevron() }
                SettingsDivider()
                // الملاحظات: رسالة بريد فيها الإصدار والنظام
                val ht = com.osatks.mizan.i18n.HomeText.of(LocalLang.current.code)
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                val langCode = LocalLang.current.code
                SettingsRow(Icons.Filled.Email, ht.feedback, ht.feedbackSub, onClick = {
                    runCatching { uri.openUri(com.osatks.mizan.core.Feedback.mailto(langCode)) }
                        .onFailure { Platform.toast(com.osatks.mizan.core.Feedback.EMAIL) }
                }) { SettingsChevron() }
            }
        }
        item {
            SettingsGroup(s.about) {
                SettingsAbout()
                SettingsDivider(inset = 16.dp)
                // المصادر والتراخيص كاملة: المصحف والتفاسير والتلاوات وتسجيلات الأذان والخط
                SettingsRow(Icons.Filled.Info, s.licenses, s.licensesSub, onClick = { open("licenses") }) { SettingsChevron() }
            }
        }
        item {
            SettingsGroup(s.privacy) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsTile(Icons.Filled.PanTool)
                    Text(
                        s.privacyText, Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item { Note(s.disclaimer, warn = true) }
    }

    if (showLang) LanguageSheet { showLang = false }
}

// ---------- المجموعات والصفوف (كما في NativeSettings.swift) ----------

/** مجموعة بأسلوب iOS: عنوان صغير فوق بطاقة بيضاء مستديرة (20) بحد رفيع، صفوفها مفصولة بخطوط رفيعة */
@Composable
private fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(
            title, Modifier.padding(horizontal = 12.dp).semantics { heading() },
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = cs.onSurfaceVariant,
        )
        Column(
            Modifier.fillMaxWidth()
                .shadow(1.dp, shape, ambientColor = Color.Black.copy(alpha = .04f), spotColor = Color.Black.copy(alpha = .06f))
                .clip(shape)
                .background(cs.surface)
                .border(1.dp, cs.outlineVariant.copy(alpha = .8f), shape),
            content = content,
        )
    }
}

/** أيقونة الصف على مربع ذهبي فاتح */
@Composable
private fun SettingsTile(icon: ImageVector) {
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = .16f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
}

/** صف: الأيقونة والعنوان والوصف، ثم السهم أو المفتاح؛ يظلل تحت الإصبع كصفوف إعدادات النظام */
@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    val press by animateColorAsState(if (down) cs.onSurface.copy(alpha = .07f) else Color.Transparent, tween(90), label = "settingsPress")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(press)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = cs.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/** خط رفيع يبدأ بعد الأيقونة (16 + 34 + 12) */
@Composable
private fun SettingsDivider(inset: Dp = 62.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f)))
}

/** سهم الصف الذي يفتح صفحة (يتبع اتجاه اللغة) */
@Composable
private fun SettingsChevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
}

/** عشرة ألوان للتطبيق في صفين (وفي صف واحد على الأوسع)؛ اللون المختار عليه حلقة وعلامة صح */
@Composable
private fun AccentPicker(selected: Accent, names: List<String>, onPick: (Accent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = if (maxWidth >= 528.dp) 10 else 5
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Accent.entries.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { a ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            AccentSwatch(a, a == selected, names.getOrElse(a.ordinal) { a.key }) { onPick(a) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(a: Accent, on: Boolean, name: String, onPick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val ring by animateColorAsState(if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent, tween(200), label = "swatchRing")
    Box(
        Modifier.size(48.dp).pressScale(src, 0.9f).clip(CircleShape)
            .selectable(on, interactionSource = src, indication = null, role = Role.RadioButton, onClick = onPick)
            .semantics { contentDescription = name }
            .border(2.dp, ring, CircleShape)
            .padding(5.dp).clip(CircleShape).background(heroBrush(a)),
        contentAlignment = Alignment.Center
    ) {
        if (on) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = Color.White)
    }
}

/** عن التطبيق: نجمة ذهبية بجانب الاسم، والإصدار، والنبذة، والمصادر بروابطها */
@Composable
private fun SettingsAbout() {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsStar(Modifier.size(18.dp))
            Text(s.aboutName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = cs.onSurface)
        }
        Text(s.version(Platform.versionName()), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = cs.onSurfaceVariant)
        Text(s.aboutText, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = cs.onSurface)
        LinkedText(s.quranSources)
    }
}

/** النجمة الثمانية ممتلئة بالذهبي (شعار ميزان) */
@Composable
private fun SettingsStar(modifier: Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        for (sq in 0..1) {
            val path = Path()
            for (i in 0..3) {
                val a = (sq * 45.0 + i * 90.0 - 90.0) * PI / 180.0
                val x = c.x + r * cos(a).toFloat()
                val y = c.y + r * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Gold)
        }
    }
}
