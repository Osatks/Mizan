package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.ALERT_PRAYERS
import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.AdhanAudio
import com.osatks.mizan.platform.AdhanPreview
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.LiveCountdown
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.PrayerCalendar
import com.osatks.mizan.ui.AppSwitch
import com.osatks.mizan.ui.GlassMenu
import com.osatks.mizan.ui.MenuItem
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.theme.Gold

/** أيقونة كل طريقة تنبيه */
private fun AlertMode.icon(): ImageVector = when (this) {
    AlertMode.ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
    AlertMode.VIBRATE -> Icons.Filled.Vibration
    AlertMode.SILENT -> Icons.Filled.NotificationsNone
    AlertMode.OFF -> Icons.Filled.NotificationsOff
}

/** رموز الأوقات كما في الرئيسية وفي iPhone */
private val ADHAN_PRAYER_ICONS: Map<String, ImageVector> = mapOf(
    "fajr" to Icons.Outlined.AutoAwesome,
    "dhuhr" to Icons.Outlined.WbSunny,
    "asr" to Icons.Outlined.LightMode,
    "maghrib" to Icons.Outlined.WbTwilight,
    "isha" to Icons.Outlined.NightsStay,
)

/**
 * الأذان والتنبيهات بطراز iPhone: مجموعات بطاقات كإعدادات النظام (عنوان صغير فوق بطاقة صفوفها مفصولة بخطوط رفيعة):
 * تشغيل التنبيه، وطريقته لكل صلاة (أذان، اهتزاز، صامت، بلا تنبيه) من قائمة، والمؤذن (وأذان الفجر وحده) مع الاستماع،
 * ونمط الاهتزاز مع تجربته، ومصادر التسجيلات.
 */
@Composable
fun AdhanScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val toggleNotify = rememberAlertsToggle()
    // المعاينة لا تستمر بعد مغادرة الصفحة
    DisposableEffect(Unit) { onDispose { AdhanPreview.stop() } }

    ToolPage(s.adhanSettings, s.adhanSettingsSub, onBack, icon = Icons.AutoMirrored.Filled.VolumeUp, spacing = 20.dp) {
        item {
            AdhanGroup(null) {
                AdhanToggleRow(Icons.Filled.NotificationsActive, s.adhanAlert, s.adhanAlertDesc, AppSettings.notify) { on -> toggleNotify(on) }
                if (PrayerAlerts.hasExactAlarmSettings) {
                    AdhanDivider()
                    AdhanNavRow(Icons.Filled.Alarm, s.exactAlarm, s.exactAlarmDesc) { PrayerAlerts.openExactAlarmSettings() }
                }
                if (LiveCountdown.supported) { AdhanDivider(); LiveCountdownRow() }
                if (PrayerCalendar.supported) { AdhanDivider(); CalendarRow() }
            }
        }
        item {
            AdhanGroup(s.alertPerPrayer) {
                ALERT_PRAYERS.forEachIndexed { i, k ->
                    if (i > 0) AdhanDivider()
                    PrayerModeRow(k)
                }
                AdhanFootnote(s.alertModesHint)
            }
        }
        item {
            AdhanGroup(s.muezzinTitle) {
                Column(Modifier.selectableGroup()) {
                    Muezzins.ALL.filter { !it.fajr }.forEachIndexed { i, m ->
                        if (i > 0) AdhanDivider(52.dp)
                        MuezzinRow(m, AppSettings.muezzin == m.id) { AppSettings.updateMuezzin(m.id); PrayerAlerts.reschedule() }
                    }
                }
            }
        }
        item {
            AdhanGroup(s.fajrAdhanTitle) {
                AdhanFootnote(s.fajrAdhanHint, top = true)
                Column(Modifier.selectableGroup()) {
                    Muezzins.ALL.filter { it.fajr }.forEach { m ->
                        MuezzinRow(m, AppSettings.fajrMuezzin == m.id) { AppSettings.updateFajrMuezzin(m.id); PrayerAlerts.reschedule() }
                        AdhanDivider(52.dp)
                    }
                    val same = AppSettings.fajrMuezzin == Muezzins.SAME
                    OptionRow(s.fajrSameAsOthers, s.muezzinName(Muezzins.forPrayer("dhuhr")), same) {
                        AppSettings.updateFajrMuezzin(Muezzins.SAME); PrayerAlerts.reschedule()
                    }
                }
            }
        }
        item { AdhanGroup(s.vibrationTitle) { VibrationGrid() } }
        item { Note(if (Platform.name == "ios") s.adhanIosNote else s.adhanStopHint) }
        item {
            AdhanGroup(s.recordingSources) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Muezzins.ALL.forEach { m ->
                        Text(
                            "${s.muezzinName(m)}: ${m.credit}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------- المجموعات والصفوف ----------

/** مجموعة بأسلوب iPhone: عنوان صغير فوق بطاقة بيضاء مستديرة صفوفها مفصولة بخطوط رفيعة */
@Composable
private fun AdhanGroup(title: String?, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(
            title, Modifier.padding(horizontal = 12.dp).semantics { heading() },
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(20.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .8f)),
            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = .04f), spotColor = Color.Black.copy(alpha = .04f)),
        ) { Column(Modifier.fillMaxWidth(), content = content) }
    }
}

/** أيقونة الصف على مربع ذهبي فاتح */
@Composable
private fun AdhanTile(icon: ImageVector) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AdhanRowText(title: String, sub: String?, modifier: Modifier = Modifier, subColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        if (!sub.isNullOrEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = subColor)
    }
}

/** خط فاصل يبدأ بعد الأيقونة (16 + 34 + 12) */
@Composable
private fun AdhanDivider(inset: Dp = 62.dp) {
    HorizontalDivider(Modifier.padding(start = inset), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f))
}

/** شرح صغير داخل البطاقة */
@Composable
private fun AdhanFootnote(text: String, top: Boolean = false) {
    Text(
        text, Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = if (top) 14.dp else 4.dp, bottom = if (top) 4.dp else 14.dp),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** صف بمفتاح تشغيل؛ وأثناء العمل (التقويم) مؤشر انتظار مكانه */
@Composable
private fun AdhanToggleRow(icon: ImageVector, title: String, sub: String?, on: Boolean, busy: Boolean = false, set: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled = !busy) { set(!on) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdhanTile(icon)
        Spacer(Modifier.width(12.dp))
        AdhanRowText(title, sub, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        else AppSwitch(checked = on, onCheckedChange = set)
    }
}

/** صف يفتح إعدادات النظام (المنبّه الدقيق) */
@Composable
private fun AdhanNavRow(icon: ImageVector, title: String, sub: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdhanTile(icon)
        Spacer(Modifier.width(12.dp))
        AdhanRowText(title, sub, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
    }
}

/**
 * iPhone: العدّ التنازلي للصلاة القادمة على شاشة القفل وفي الجزيرة الديناميكية حين يغادر المستخدم التطبيق قبلها
 * بأقل من ثلاث ساعات. إن كانت الأنشطة الحية مطفأة لميزان في إعدادات النظام يقول ذلك ويفتح الإعدادات.
 */
@Composable
private fun LiveCountdownRow() {
    val s = LocalStrings.current
    val set = { on: Boolean ->
        LiveCountdown.update(on)
        if (on && LiveCountdown.systemOff) AppStore.openSettings()
    }
    AdhanToggleRow(Icons.Filled.Timer, s.liveCountdown, if (LiveCountdown.systemOff) s.liveActivitiesOff else s.liveCountdownDesc, LiveCountdown.enabled) { set(it) }
}

/** iPhone و iPad: مواقيت ثلاثين يوماً في تقويم خاص بها في تطبيق التقويم، تتجدد مع كل عودة إلى التطبيق */
@Composable
private fun CalendarRow() {
    val ft = FeatureText.of(LocalLang.current.code)
    AdhanToggleRow(Icons.Filled.CalendarMonth, ft.addToCalendar, ft.calendarDesc, PrayerCalendar.enabled, busy = PrayerCalendar.busy) { PrayerCalendar.update(it) }
}

// ---------- طريقة التنبيه ----------

/** صلاة: رمز وقتها واسمها والطريقة المختارة تحته، وزر يفتح قائمة بالطرق الأربع (برموزها والمختارة بعلامة صح) */
@Composable
private fun PrayerModeRow(prayer: String) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val mode = AppSettings.alertMode(prayer)
    var open by remember { mutableStateOf(false) }
    val detail = if (mode == AlertMode.ADHAN) "${s.alertModeName(mode)} · ${s.muezzinName(Muezzins.forPrayer(prayer))}" else s.alertModeName(mode)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdhanTile(ADHAN_PRAYER_ICONS[prayer] ?: Icons.Outlined.WbSunny)
        Spacer(Modifier.width(12.dp))
        AdhanRowText(s.prayer(prayer), detail, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Box {
            val off = mode == AlertMode.OFF
            val bg by animateColorAsState(if (off) cs.onSurfaceVariant.copy(alpha = .1f) else cs.primaryContainer, tween(180), label = "modeBg")
            val fg = if (off) cs.onSurfaceVariant else cs.onPrimaryContainer
            Row(
                Modifier.height(36.dp).clip(CircleShape).background(bg)
                    .clickable { open = true }
                    .semantics { role = Role.DropdownList; contentDescription = s.prayer(prayer); stateDescription = detail }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(mode.icon(), null, Modifier.size(19.dp), tint = fg)
                Icon(Icons.Filled.UnfoldMore, null, Modifier.size(16.dp), tint = fg.copy(alpha = .8f))
            }
            GlassMenu(
                open, { open = false },
                AlertMode.entries.map { m ->
                    MenuItem(s.alertModeName(m), icon = m.icon(), checked = m == mode) {
                        if (m != mode) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            AppSettings.updateAlertMode(prayer, m)
                            PrayerAlerts.reschedule()
                        }
                    }
                },
                title = s.prayer(prayer),
            )
        }
    }
}

// ---------- المؤذنون ----------

/** علامة الاختيار في صفوف المؤذنين */
@Composable
private fun RadioDot(on: Boolean) {
    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .12f), tween(180), label = "dot")
    Box(Modifier.size(24.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, on: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(on, role = Role.RadioButton, onClick = onSelect).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioDot(on)
        Spacer(Modifier.width(12.dp))
        AdhanRowText(title, subtitle, Modifier.weight(1f))
    }
}

/** مؤذن: الاختيار، والاستماع، والتنزيل إن لم يكن على الجهاز (ويُختار بعد اكتماله)، والحذف للمنزَّل */
@Composable
private fun MuezzinRow(m: Muezzin, selected: Boolean, onSelect: () -> Unit) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val available = AdhanSounds.available(m)
    val pct = AdhanSounds.progress[m.id]
    val downloading = pct != null && pct >= 0
    val download = { AdhanSounds.start(m) { ok -> if (ok) onSelect() else Platform.toast(s.downloadFailed) } }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 56.dp)
                .selectable(selected, role = Role.RadioButton) { if (available) onSelect() else if (!downloading) download() }
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioDot(selected)
            Spacer(Modifier.width(12.dp))
            val sub = when {
                downloading -> s.downloadingPct(pct ?: 0)
                pct == -1 -> s.downloadFailed
                available -> duration(m.seconds)
                else -> "${duration(m.seconds)} · ${s.sizeMb(Fmt.num(m.sizeKb / 1000.0, 1))}"
            }
            AdhanRowText(s.muezzinName(m), sub, Modifier.weight(1f), if (pct == -1) cs.error else cs.onSurfaceVariant)
        }
        Box(Modifier.padding(end = 8.dp)) {
            when {
                downloading -> CircularProgressIndicator(
                    progress = { ((pct ?: 0) / 100f).coerceAtLeast(.02f) }, modifier = Modifier.padding(10.dp).size(24.dp),
                    strokeWidth = 2.5.dp, trackColor = cs.primaryContainer,
                )
                !available -> IconButton(onClick = download) { Icon(Icons.Outlined.DownloadForOffline, s.download, Modifier.size(26.dp), tint = cs.primary) }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!m.bundled && !selected) {
                        IconButton(onClick = { AdhanPreview.stop(); AdhanSounds.delete(m) }, modifier = Modifier.size(40.dp, 44.dp)) {
                            Icon(Icons.Filled.Delete, s.delete, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                        }
                    }
                    val playing = AdhanPreview.playing == m.id
                    IconButton(onClick = { AdhanPreview.toggle(m) }) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(if (playing) cs.primary else cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (playing) s.stopAdhan else s.playAdhan,
                                Modifier.size(20.dp), tint = if (playing) cs.onPrimary else cs.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- الاهتزاز ----------

/** أنماط الاهتزاز شبكةً من عمودين (وثلاثة على الأوسع): لكل نمط اسمه ورسمه وزر «جرّب»، واختيار النمط يجرّبه فوراً */
@Composable
private fun VibrationGrid() {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
        val perRow = if (maxWidth >= 536.dp) 3 else 2
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VibePattern.entries.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p -> VibeTile(p, AppSettings.vibePattern == p, Modifier.weight(1f)) }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun VibeTile(p: VibePattern, selected: Boolean, modifier: Modifier) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.onSurfaceVariant.copy(alpha = .08f), tween(180), label = "vibeBg")
    val accent = cs.primary
    val bars by animateColorAsState(if (selected) accent else cs.onSurfaceVariant.copy(alpha = .6f), tween(180), label = "vibeBars")
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .border(1.5.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .selectable(selected, role = Role.RadioButton) { AppSettings.updateVibePattern(p); AdhanAudio.vibrate(p) }
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                s.vibeNames[p.key] ?: p.key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            VibeShape(p, bars, Modifier.fillMaxWidth().height(14.dp))
        }
        IconButton(onClick = { AdhanAudio.vibrate(p) }, modifier = Modifier.size(40.dp, 44.dp)) {
            Icon(Icons.Filled.PlayArrow, s.tryVibration, Modifier.size(22.dp), tint = accent)
        }
    }
}

/** رسم النمط: كل مقطع مهتز شريط عرضه مدته وارتفاعه شدته، على خط زمني واحد (اتجاهه يتبع اللغة) */
@Composable
private fun VibeShape(p: VibePattern, color: Color, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier) {
        val total = p.millis.coerceAtLeast(1).toFloat()
        val gap = 1.dp.toPx()
        drawLine(color.copy(alpha = .25f), Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
        for ((start, ms, amp) in p.pulses) {
            val w = (ms / total * size.width - gap).coerceAtLeast(2.dp.toPx())
            val x = start / total * size.width
            val h = size.height * (0.25f + 0.75f * amp / 255f)
            drawRoundRect(color, Offset(if (rtl) size.width - x - w else x, size.height - h), Size(w, h), CornerRadius(1.5.dp.toPx()))
        }
    }
}
