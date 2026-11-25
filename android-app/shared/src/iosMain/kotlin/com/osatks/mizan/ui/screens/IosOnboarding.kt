package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Place
import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.placeName

/** مفاتيح أيقونات الصفحات للواجهة الأصلية، بترتيب PAGE_ICONS ثم guideIcons (NativeGuide.swift يحوّلها إلى رموز النظام) */
private val NATIVE_PAGE_ICONS = listOf("star", "explore", "quran", "calc", "search")

private fun nativeGuideIcons(ios: Boolean) = buildList {
    add("widgets"); add("quote"); add("share")
    if (ios) { add("lock"); add("tap"); add("watch") }
    add("adhan")
}

/**
 * جولة أول تشغيل وصفحة «دليل التطبيق» في iOS بواجهة النظام (NativeGuide.swift): الصفحات والصفحة الحالية ومنطق «التالي»
 * و«تخطي» وطلب الموقع هنا كما في جولة Compose (Onboarding) و LocationButtons في أندرويد، والرسم في Swift.
 * ورقة المدن ونافذة رفض الإذن من النظام فوقها.
 */
@Composable
internal fun IosOnboarding(onDone: () -> Unit, from: Int = 0) {
    val s = LocalStrings.current
    val lang = com.osatks.mizan.i18n.LocalLang.current
    val ios = com.osatks.mizan.ui.IsIos
    val base = remember(s, lang, from) { (s.onboarding + com.osatks.mizan.i18n.GuideText.of(lang.code).pages(ios)).drop(from) }
    val keys = remember(from) { (NATIVE_PAGE_ICONS + nativeGuideIcons(ios)).drop(from) }
    // صفحة الموقع الأخيرة، ولا تُعرض بعد التحديث لمن اختار موقعه
    val location = !(from > 0 && AppSettings.placeSet)
    val pages = base.mapIndexed { i, (title, body) -> GuidePage(title, body, keys.getOrElse(i) { "star" }) } +
        if (location) listOf(GuidePage(s.obLocationTitle, s.obLocationBody, "place")) else emptyList()
    val count = pages.size
    var page by remember { mutableStateOf(0) }
    val current = page.coerceIn(0, count - 1)
    val last = current == count - 1
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
    // خلفية خضراء كاملة: نص شريط الحالة فاتح
    com.osatks.mizan.ui.StatusBarStyle(lightContent = true)
    NativeGuideView(
        pages, current, location,
        placeName = if (AppSettings.placeSet) s.placeName(AppSettings.place) else null,
        locating = locating,
        onPage = { page = it.coerceIn(0, count - 1) },
        onNext = { if (last) onDone() else { page = current + 1 } },
        // «تخطي» ينقل إلى الصفحة الأخيرة أولاً، ومنها يُنهي الجولة
        onSkip = { if (last) onDone() else { page = count - 1 } },
        onLocate = { locating = true; locate() },
        onPickCity = { pickCity = true },
        modifier = Modifier.fillMaxSize(),
    )
    if (denied) LocationDeniedDialog(onPickCity = { denied = false; pickCity = true }, onDismiss = { denied = false })
    if (pickCity) CityPickerSheet(AppSettings.place.key.takeIf { AppSettings.placeSet && !AppSettings.place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlerts.reschedule()
    }
}

/** صفحة من الجولة للواجهة: العنوان والنص ومفتاح أيقونتها (star، explore، quran، calc، search، widgets، quote، share، lock، tap، watch، adhan، place) */
internal class GuidePage(val title: String, val body: String, val icon: String)
