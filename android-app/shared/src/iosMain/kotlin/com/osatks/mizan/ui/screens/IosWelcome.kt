package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.core.Place
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.platform.AdhanPreview
import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.StatusBarStyle

/**
 * إعداد أول تشغيل في iOS بواجهة النظام (NativeWelcome.swift): الخطوة الحالية وكل ما يُفعل فيها هنا كما في [Welcome] بأندرويد —
 * اللغة (LocaleSync)، والموقع (الإذن نفسه وورقة المدن)، والأذان (rememberAlertsToggle ومعاينة المؤذن)، والمظهر ولون صفحة
 * المصحف — وكل اختيار يُحفظ فوراً. الرسم في Swift، وورقة المدن ونوافذ رفض الإذن من النظام فوقه.
 */
@Composable
internal fun IosWelcome(onDone: () -> Unit) {
    val s = LocalStrings.current
    QuranPrefs.ensure()
    val count = WelcomeFlow.COUNT
    var step by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val locate = rememberLocationRequest { r ->
        locating = false
        when (r) {
            is LocationResult.Found -> { AppSettings.updatePlace(r.place); PrayerAlerts.reschedule(); Platform.toast(s.locationFound) }
            LocationResult.Failed -> Platform.toast(s.locationFailed)
            LocationResult.Denied -> denied = true
        }
    }
    val toggleAlerts = rememberAlertsToggle()
    // معاينة الأذان لا تستمر خارج خطوته ولا بعد الإعداد
    LaunchedEffect(step) { if (step != WelcomeFlow.ADHAN) AdhanPreview.stop() }
    DisposableEffect(Unit) { onDispose { AdhanPreview.stop() } }
    // خلفية خضراء كاملة: نص شريط الحالة فاتح
    StatusBarStyle(lightContent = true)
    // لمسة من Swift: تُنفَّذ بالإعدادات نفسها التي تستعملها بقية التطبيق
    fun act(k: String, x: String) {
        when (k) {
            "go" -> { val i = x.toIntOrNull(); if (i != null) step = i.coerceIn(0, count - 1) }
            "next" -> { if (step >= count - 1) onDone() else step = step + 1 }
            "back" -> { if (step > 0) step = step - 1 }
            "skip" -> onDone()
            // كما في ورقة اللغات: الإعداد نفسه، فتتبدل الواجهة كلها خلف ستار اللغة
            "language" -> { if (x != AppSettings.lang) LocaleSync.choose(x) }
            "locate" -> { if (!locating) { locating = true; locate() } }
            "city" -> { pickCity = true }
            "alerts" -> toggleAlerts(x == "true")
            "muezzin" -> { val m = Muezzins.of(x); if (m != null && !m.fajr) WelcomeFlow.pickMuezzin(m) }
            "play" -> { val m = Muezzins.of(x); if (m != null) AdhanPreview.toggle(m) }
            "theme" -> { if (x in WelcomeFlow.THEMES) AppSettings.updateTheme(x) }
            "tone" -> { if (x in WelcomeFlow.TONES) QuranPrefs.updateTone(x) }
            else -> {}
        }
    }
    NativeWelcomeView(
        step = step.coerceIn(0, count - 1),
        locating = locating,
        onAction = { k, x -> act(k, x) },
        modifier = Modifier.fillMaxSize(),
    )
    if (denied) LocationDeniedDialog(onPickCity = { denied = false; pickCity = true }, onDismiss = { denied = false })
    if (pickCity) CityPickerSheet(AppSettings.place.key.takeIf { AppSettings.placeSet && !AppSettings.place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlerts.reschedule()
    }
}
