package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
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
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.platform.PrayerCalendar

/**
 * الأذان والتنبيهات في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية: الحفظ والتنزيل والتشغيل هنا كما في شاشة Compose
 * في أندرويد (AdhanScreen)، وSwift يرسم ويعيد اللمس
 */
@Composable
internal fun IosAdhanScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val toggleNotify = rememberAlertsToggle()
    // المعاينة لا تستمر بعد مغادرة الصفحة
    DisposableEffect(Unit) { onDispose { AdhanPreview.stop() } }
    val ft = FeatureText.of(LocalLang.current.code)
    NativeAdhanView(
        content = nativeAdhanContent(s, ft),
        onAction = { k, x -> nativeAdhanAction(k, x, s, toggleNotify) },
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}


private fun nativeSwitch(title: String, sub: String, on: Boolean) = linkedMapOf<String, Any?>("title" to title, "sub" to sub, "on" to on)

/** صف مؤذن لواجهة النظام بحالته كما في [MuezzinRow]: الاختيار والتوفر والتنزيل ونسبته والحذف والمعاينة */
private fun nativeMuezzin(m: Muezzin, selected: Boolean, s: Strings): Map<String, Any?> {
    val available = AdhanSounds.available(m)
    val pct = AdhanSounds.progress[m.id]
    val downloading = pct != null && pct >= 0
    return linkedMapOf(
        "id" to m.id, "name" to s.muezzinName(m), "selected" to selected, "available" to available,
        "downloading" to downloading, "failed" to (pct == -1), "progress" to ((pct ?: 0).coerceIn(0, 100) / 100.0),
        "sub" to when {
            downloading -> s.downloadingPct(pct ?: 0)
            pct == -1 -> s.downloadFailed
            available -> duration(m.seconds)
            else -> "${duration(m.seconds)} · ${s.sizeMb(Fmt.num(m.sizeKb / 1000.0, 1))}"
        },
        "canDelete" to (available && !m.bundled && !selected),
        "playing" to (AdhanPreview.playing == m.id),
    )
}

/** محتوى الشاشة كله لواجهة النظام (النصوص والحالات منسّقة هنا)، بترتيب بطاقات [AdhanScreen] */
private fun nativeAdhanContent(s: Strings, ft: FeatureText): Map<String, Any?> = linkedMapOf(
    "title" to s.adhanSettings, "subtitle" to s.adhanSettingsSub,
    "alert" to nativeSwitch(s.adhanAlert, s.adhanAlertDesc, AppSettings.notify),
    "exactAlarm" to if (PrayerAlerts.hasExactAlarmSettings) linkedMapOf("title" to s.exactAlarm, "sub" to s.exactAlarmDesc) else null,
    "live" to if (LiveCountdown.supported) {
        nativeSwitch(s.liveCountdown, if (LiveCountdown.systemOff) s.liveActivitiesOff else s.liveCountdownDesc, LiveCountdown.enabled)
    } else null,
    "calendar" to if (PrayerCalendar.supported) {
        nativeSwitch(ft.addToCalendar, ft.calendarDesc, PrayerCalendar.enabled) + ("busy" to PrayerCalendar.busy)
    } else null,
    "perPrayerTitle" to s.alertPerPrayer, "modesHint" to s.alertModesHint,
    "modes" to AlertMode.entries.map { m -> linkedMapOf("key" to m.key, "name" to s.alertModeName(m)) },
    "prayers" to ALERT_PRAYERS.map { k ->
        val mode = AppSettings.alertMode(k)
        linkedMapOf(
            "key" to k, "name" to s.prayer(k), "mode" to mode.key,
            "detail" to if (mode == AlertMode.ADHAN) "${s.alertModeName(mode)} · ${s.muezzinName(Muezzins.forPrayer(k))}" else s.alertModeName(mode),
        )
    },
    "muezzinTitle" to s.muezzinTitle,
    "muezzins" to Muezzins.ALL.filter { !it.fajr }.map { m -> nativeMuezzin(m, AppSettings.muezzin == m.id, s) },
    "fajrTitle" to s.fajrAdhanTitle, "fajrHint" to s.fajrAdhanHint,
    "fajrMuezzins" to Muezzins.ALL.filter { it.fajr }.map { m -> nativeMuezzin(m, AppSettings.fajrMuezzin == m.id, s) },
    "fajrSame" to linkedMapOf(
        "title" to s.fajrSameAsOthers, "sub" to s.muezzinName(Muezzins.forPrayer("dhuhr")), "on" to (AppSettings.fajrMuezzin == Muezzins.SAME),
    ),
    "vibrationTitle" to s.vibrationTitle,
    // رسم كل نمط: لكل مقطع مهتز بدايته وعرضه نسبةً إلى مدته كلها، وارتفاعه من شدته (كما في VibeShape)
    "vibes" to VibePattern.entries.map { p ->
        val total = p.millis.coerceAtLeast(1).toDouble()
        linkedMapOf(
            "key" to p.key, "name" to (s.vibeNames[p.key] ?: p.key), "selected" to (AppSettings.vibePattern == p),
            "pulses" to p.pulses.map { (start, ms, amp) -> listOf(start / total, ms / total, 0.25 + 0.75 * amp / 255.0) },
        )
    },
    "note" to if (Platform.name == "ios") s.adhanIosNote else s.adhanStopHint,
    "sourcesTitle" to s.recordingSources,
    "sources" to Muezzins.ALL.map { m -> "${s.muezzinName(m)}: ${m.credit}" },
    "download" to s.download, "delete" to s.delete, "play" to s.playAdhan, "stop" to s.stopAdhan, "tryVibration" to s.tryVibration,
)

/** لمسة من واجهة النظام: تُنفَّذ كما في صفوف Compose نفسها */
private fun nativeAdhanAction(k: String, x: String, s: Strings, toggleNotify: (Boolean) -> Unit) {
    fun select(m: Muezzin) {
        if (m.fajr) AppSettings.updateFajrMuezzin(m.id) else AppSettings.updateMuezzin(m.id)
        PrayerAlerts.reschedule()
    }
    fun download(m: Muezzin) {
        val pct = AdhanSounds.progress[m.id]
        if (pct != null && pct >= 0) return
        AdhanSounds.start(m) { ok -> if (ok) select(m) else Platform.toast(s.downloadFailed) }
    }
    when {
        k == "notify" -> toggleNotify(x == "true")
        k == "exactAlarm" -> PrayerAlerts.openExactAlarmSettings()
        k == "live" -> {
            val on = x == "true"
            LiveCountdown.update(on)
            if (on && LiveCountdown.systemOff) AppStore.openSettings()
        }
        k == "calendar" -> PrayerCalendar.update(x == "true")
        k.startsWith("mode:") -> {
            val prayer = k.removePrefix("mode:")
            val m = AlertMode.of(x)
            if (prayer in ALERT_PRAYERS && AppSettings.alertMode(prayer) != m) {
                AppSettings.updateAlertMode(prayer, m)
                PrayerAlerts.reschedule()
            }
        }
        k == "pick" || k == "pickFajr" -> {
            if (k == "pickFajr" && x == Muezzins.SAME) {
                AppSettings.updateFajrMuezzin(Muezzins.SAME); PrayerAlerts.reschedule()
            } else Muezzins.of(x)?.takeIf { it.fajr == (k == "pickFajr") }?.let { m -> if (AdhanSounds.available(m)) select(m) else download(m) }
        }
        k == "download" -> Muezzins.of(x)?.let { download(it) }
        k == "delete" -> Muezzins.of(x)?.let { AdhanPreview.stop(); AdhanSounds.delete(it) }
        k == "play" -> Muezzins.of(x)?.let { if (AdhanSounds.available(it)) AdhanPreview.toggle(it) }
        k == "vibe" -> VibePattern.entries.firstOrNull { it.key == x }?.let { AppSettings.updateVibePattern(it); AdhanAudio.vibrate(it) }
        k == "tryVibe" -> VibePattern.entries.firstOrNull { it.key == x }?.let { AdhanAudio.vibrate(it) }
        else -> {}
    }
}
