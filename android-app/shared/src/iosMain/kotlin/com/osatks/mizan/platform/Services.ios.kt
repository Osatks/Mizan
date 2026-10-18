@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import kotlinx.datetime.LocalDate
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.now
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.CoreLocation.CLLocation
import platform.Foundation.NSDateComponents
import platform.Foundation.NSError
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/**
 * تنبيهات الأذان في iOS: إشعارات محلية مجدولة مسبقاً لأيام قادمة (لا يوجد منبّه يعمل في الخلفية).
 * تُعاد جدولتها عند فتح التطبيق وعند تغيير الإعدادات، والحد الأقصى في iOS 64 إشعاراً.
 */
actual object PrayerAlerts {
    private const val DAYS = 7

    actual fun reschedule() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeAllPendingNotificationRequests()
        if (!AppSettings.notify) return
        val s = Lang.current.strings
        val p = AppSettings.place
        val today = LocalDate.now()
        for (offset in 0 until DAYS) {
            val day = today.plusDays(offset)
            val t = PrayTimes.compute(day, p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi)
            for (key in PrayTimes.PRAYERS) {
                if (key == "sunrise") continue
                val h = t[key]
                if (h.isNaN()) continue
                val minutes = (h * 60).toInt()
                val comps = NSDateComponents().apply {
                    year = day.year.toLong(); month = day.monthNumber.toLong(); this.day = day.dayOfMonth.toLong()
                    hour = (minutes / 60).toLong(); minute = (minutes % 60).toLong()
                }
                val content = UNMutableNotificationContent().apply {
                    setTitle(s.prayerNowTitle(s.prayer(key)))
                    setBody(s.prayerNowText(s.placeName(p)))
                    setSound(UNNotificationSound.defaultSound)
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(comps, false)
                center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier("adhan-$day-$key", content, trigger), null)
            }
        }
    }

    actual fun languageChanged() = reschedule()

    actual val hasExactAlarmSettings: Boolean = false
    actual fun openExactAlarmSettings() {}
}

@Composable
actual fun rememberNotifyToggle(onResult: (Boolean) -> Unit): (Boolean) -> Unit = remember {
    { on ->
        if (!on) onResult(false)
        else UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, _ -> runOnMain { onResult(granted) } }
    }
}

/** يحدد الموقع مرة واحدة عبر CoreLocation */
private class LocationRequest(val onResult: (LocationResult) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyKilometer }
    private var done = false

    fun start() {
        manager.delegate = this
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> finish(LocationResult.Denied)
            else -> manager.requestLocation()
        }
    }

    private fun finish(r: LocationResult) {
        if (done) return
        done = true
        manager.delegate = null
        onResult(r)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> finish(LocationResult.Denied)
            else -> {}
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return finish(LocationResult.Failed)
        loc.coordinate.useContents { finish(LocationResult.Found(LocationHelper.placeFrom(latitude, longitude))) }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) = finish(LocationResult.Failed)
}

/** الطلب الجاري (يبقى مرجعه حتى تصل النتيجة) */
private var current: LocationRequest? = null

@Composable
actual fun rememberLocationRequest(onResult: (LocationResult) -> Unit): () -> Unit = remember {
    {
        current = LocationRequest { r -> current = null; onResult(r) }.also { it.start() }
    }
}

@Composable
actual fun systemLanguageTag(): String = Platform.systemLanguage()

/** البوصلة: اتجاه الشمال الحقيقي من CoreLocation (يصحح الانحراف المغناطيسي بنفسه) */
private class HeadingWatcher(val onHeading: (Float, Boolean) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    val manager = CLLocationManager()
    fun start() {
        manager.delegate = this
        manager.headingFilter = 1.0
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) manager.requestWhenInUseAuthorization()
        manager.startUpdatingHeading()
    }
    fun stop() { manager.stopUpdatingHeading(); manager.delegate = null }
    override fun locationManager(manager: CLLocationManager, didUpdateHeading: platform.CoreLocation.CLHeading) {
        val h = if (didUpdateHeading.trueHeading >= 0) didUpdateHeading.trueHeading else didUpdateHeading.magneticHeading
        onHeading(h.toFloat(), didUpdateHeading.headingAccuracy < 0 || didUpdateHeading.headingAccuracy > 25)
    }
}

@Composable
actual fun rememberCompass(lat: Double, lng: Double): Compass {
    val available = remember { CLLocationManager.headingAvailable() }
    var heading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var low by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(available) {
        val w = if (available) HeadingWatcher { h, l -> heading = h; low = l }.also { it.start() } else null
        onDispose { w?.stop() }
    }
    return Compass(heading, available, low)
}
