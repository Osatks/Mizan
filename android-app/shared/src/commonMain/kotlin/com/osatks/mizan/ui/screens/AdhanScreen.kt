package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.ALERT_PRAYERS
import com.osatks.mizan.core.AdhanSounds
import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.core.VibePattern
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.AdhanAudio
import com.osatks.mizan.platform.AdhanPreview
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.LiveCountdown
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.AppSwitch
import com.osatks.mizan.ui.ClickRow
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.ToolPage
import androidx.compose.material.icons.filled.Alarm

/** أيقونة كل طريقة تنبيه */
private fun AlertMode.icon(): ImageVector = when (this) {
    AlertMode.ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
    AlertMode.VIBRATE -> Icons.Filled.Vibration
    AlertMode.SILENT -> Icons.Filled.NotificationsNone
    AlertMode.OFF -> Icons.Filled.NotificationsOff
}

fun Strings.alertModeName(m: AlertMode) = when (m) {
    AlertMode.ADHAN -> alertModeAdhan
    AlertMode.VIBRATE -> alertModeVibrate
    AlertMode.SILENT -> alertModeSilent
    AlertMode.OFF -> alertModeOff
}

fun Strings.muezzinName(m: Muezzin) = muezzinNames[m.id] ?: m.id

/**
 * الأذان والتنبيهات: تشغيل التنبيه، وطريقته لكل صلاة (أذان، اهتزاز، صامت، بلا تنبيه)،
 * والمؤذن (وأذان الفجر وحده) مع الاستماع، ونمط الاهتزاز مع تجربته، ومصادر التسجيلات.
 */
@Composable
fun AdhanScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val toggleNotify = rememberAlertsToggle()
    // المعاينة لا تستمر بعد مغادرة الصفحة
    DisposableEffect(Unit) { onDispose { AdhanPreview.stop() } }

    ToolPage(s.adhanSettings, s.adhanSettingsSub, onBack, icon = Icons.Filled.NotificationsActive) {
        item {
            SectionCard {
                ClickRow(s.adhanAlert, s.adhanAlertDesc, onClick = { toggleNotify(!AppSettings.notify) }, leading = Icons.Filled.NotificationsActive) {
                    AppSwitch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                if (PrayerAlerts.hasExactAlarmSettings) {
                    ClickRow(s.exactAlarm, s.exactAlarmDesc, leading = Icons.Filled.Alarm, onClick = { PrayerAlerts.openExactAlarmSettings() })
                }
                if (LiveCountdown.supported) LiveCountdownRow()
            }
        }
        item {
            SectionCard(s.alertPerPrayer) {
                ALERT_PRAYERS.forEach { k -> PrayerModeRow(k) }
                Text(s.alertModesHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(s.muezzinTitle) {
                Column(Modifier.selectableGroup()) {
                    Muezzins.ALL.filter { !it.fajr }.forEach { m ->
                        MuezzinRow(m, AppSettings.muezzin == m.id) { AppSettings.updateMuezzin(m.id); PrayerAlerts.reschedule() }
                    }
                }
            }
        }
        item {
            SectionCard(s.fajrAdhanTitle) {
                Text(s.fajrAdhanHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.selectableGroup()) {
                    Muezzins.ALL.filter { it.fajr }.forEach { m ->
                        MuezzinRow(m, AppSettings.fajrMuezzin == m.id) { AppSettings.updateFajrMuezzin(m.id); PrayerAlerts.reschedule() }
                    }
                    val same = AppSettings.fajrMuezzin == Muezzins.SAME
                    OptionRow(s.fajrSameAsOthers, s.muezzinName(Muezzins.forPrayer("dhuhr")), same) {
                        AppSettings.updateFajrMuezzin(Muezzins.SAME); PrayerAlerts.reschedule()
                    }
                }
            }
        }
        item { VibrationCard() }
        item { Note(if (Platform.name == "ios") s.adhanIosNote else s.adhanStopHint) }
        item {
            SectionCard(s.recordingSources) {
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
    ClickRow(
        s.liveCountdown, if (LiveCountdown.systemOff) s.liveActivitiesOff else s.liveCountdownDesc,
        onClick = { set(!LiveCountdown.enabled) }, leading = Icons.Filled.Timer
    ) {
        AppSwitch(checked = LiveCountdown.enabled, onCheckedChange = { on -> set(on) })
    }
}

/** صلاة وطرق التنبيه الأربع أيقوناتٍ متجاورة، وتحت اسمها الطريقة المختارة */
@Composable
private fun PrayerModeRow(prayer: String) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val mode = AppSettings.alertMode(prayer)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.prayer(prayer), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            val detail = if (mode == AlertMode.ADHAN) "${s.alertModeName(mode)} · ${s.muezzinName(Muezzins.forPrayer(prayer))}" else s.alertModeName(mode)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AlertMode.entries.forEach { m ->
                val on = m == mode
                val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, tween(180), label = "mode")
                val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(180), label = "modeFg")
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(bg)
                        .selectable(on, role = Role.RadioButton) {
                            if (!on) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                AppSettings.updateAlertMode(prayer, m)
                                PrayerAlerts.reschedule()
                            }
                        }
                        .semantics { contentDescription = s.alertModeName(m) },
                    contentAlignment = Alignment.Center
                ) { Icon(m.icon(), null, Modifier.size(20.dp), tint = fg) }
            }
        }
    }
}

/** علامة الاختيار في صفوف المؤذنين */
@Composable
private fun RadioDot(on: Boolean) {
    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, tween(180), label = "dot")
    Box(Modifier.size(24.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { if (on) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, on: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).selectable(on, role = Role.RadioButton, onClick = onSelect).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioDot(on)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun duration(sec: Int) = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

/** مؤذن: الاختيار، والاستماع، والتنزيل إن لم يكن على الجهاز (ويُختار بعد اكتماله)، والحذف للمنزَّل */
@Composable
private fun MuezzinRow(m: Muezzin, selected: Boolean, onSelect: () -> Unit) {
    val s = LocalStrings.current
    val available = AdhanSounds.available(m)
    val pct = AdhanSounds.progress[m.id]
    val downloading = pct != null && pct >= 0
    val download = { AdhanSounds.start(m) { ok -> if (ok) onSelect() else Platform.toast(s.downloadFailed) } }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .selectable(selected, role = Role.RadioButton) { if (available) onSelect() else if (!downloading) download() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioDot(selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.muezzinName(m), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val sub = when {
                downloading -> s.downloadingPct(pct ?: 0)
                pct == -1 -> s.downloadFailed
                available -> duration(m.seconds)
                else -> "${duration(m.seconds)} · ${s.sizeMb(Fmt.num(m.sizeKb / 1000.0, 1))}"
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = if (pct == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            downloading -> CircularProgressIndicator(progress = { (pct ?: 0) / 100f }, modifier = Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.5.dp)
            !available -> IconButton(onClick = download) { Icon(Icons.Filled.Download, s.download, tint = MaterialTheme.colorScheme.primary) }
            else -> {
                if (!m.bundled && !selected) {
                    IconButton(onClick = { AdhanPreview.stop(); AdhanSounds.delete(m) }) {
                        Icon(Icons.Filled.Delete, s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val playing = AdhanPreview.playing == m.id
                IconButton(onClick = { AdhanPreview.toggle(m) }) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (playing) s.stopAdhan else s.playAdhan,
                            tint = if (playing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/** أنماط الاهتزاز شبكةً من عمودين: لكل نمط اسمه ورسمه وزر «جرّب»، واختيار النمط يجرّبه فوراً */
@Composable
private fun VibrationCard() {
    val s = LocalStrings.current
    SectionCard(s.vibrationTitle) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VibePattern.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { p -> VibeTile(p, AppSettings.vibePattern == p, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VibeTile(p: VibePattern, selected: Boolean, modifier: Modifier) {
    val s = LocalStrings.current
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), tween(180), label = "vibeBg")
    val accent = MaterialTheme.colorScheme.primary
    val bars by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f), tween(180), label = "vibeBars")
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(bg)
            .border(1.5.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(14.dp))
            .selectable(selected, role = Role.RadioButton) { AppSettings.updateVibePattern(p); AdhanAudio.vibrate(p) }
            .padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                s.vibeNames[p.key] ?: p.key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            VibeShape(p, bars, Modifier.fillMaxWidth().height(14.dp))
        }
        IconButton(onClick = { AdhanAudio.vibrate(p) }, modifier = Modifier.size(40.dp)) {
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
