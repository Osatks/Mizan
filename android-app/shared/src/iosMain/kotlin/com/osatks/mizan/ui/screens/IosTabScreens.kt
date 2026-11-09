package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.quran.QuranPrefs

/** الرئيسية في iOS: بواجهة النظام (SwiftUI) متى اختير الموقع؛ وقبل ذلك شاشة Compose المشتركة، ففيها طلب الموقع */
@Composable
internal fun IosHomeScreen(open: (String) -> Unit) {
    QuranPrefs.ensure()
    if (AppSettings.placeSet) {
        var showLanguage by remember { mutableStateOf(false) }
        NativeHomeView(open, onLanguage = { showLanguage = true }, Modifier.fillMaxSize())
        if (showLanguage) LanguageSheet { showLanguage = false }
    } else HomeScreen(open)
}

/** المواقيت في iOS: بواجهة النظام متى اختير الموقع (وعدّادها في Swift)، وقبل ذلك شاشة Compose المشتركة؛ الحالة لهما في [PrayerScreen] */
@Composable
internal fun IosPrayerScreen(open: (String) -> Unit) {
    PrayerScreen(open) { locating, onLocate, onPickCity, onNotify ->
        NativePrayerView(open, locating, onLocate = onLocate, onPickCity = onPickCity, onNotify = onNotify, modifier = Modifier.fillMaxSize())
    }
}

/** تبويب الإعدادات في iOS بواجهة النظام (SwiftUI)؛ ورقة اللغة وحوار إذن الإشعارات من النظام فوقه */
@Composable
internal fun IosSettingsScreen(open: (String) -> Unit) {
    var showLang by remember { mutableStateOf(false) }
    val toggleNotify = rememberAlertsToggle()
    NativeSettingsView(open, onLanguage = { showLang = true }, onNotify = { toggleNotify(it) }, modifier = Modifier.fillMaxSize())
    if (showLang) LanguageSheet { showLang = false }
}
