@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.ALERT_PRAYERS
import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Muezzins
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
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject

/**
 * تنبيهات الأذان في iOS: إشعارات محلية مجدولة مسبقاً لأيام قادمة (لا يوجد منبّه يعمل في الخلفية).
 * تُعاد جدولتها عند فتح التطبيق وعند تغيير الإعدادات، والحد الأقصى في iOS 64 إشعاراً.
 *
 * صوت الإشعار لا يتجاوز 30 ثانية في iOS، فالأذان في الإشعار مقطع أوله (adhan_<id>.caf)، ويُسمع كاملاً إن كان التطبيق مفتوحاً.
 * «اهتزاز فقط»: لا يهتز إشعار iOS بلا صوت، فيُعطى صوتاً صامتاً (mizan_vibrate.caf) يصحبه اهتزاز النظام حسب إعدادات الهاتف.
 */
actual object PrayerAlerts {
    private const val DAYS = 7
    const val VIBRATE_SOUND = "mizan_vibrate.caf"

    actual fun reschedule() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        // التطبيق مفتوح عند دخول الوقت: الأذان كاملاً أو نمط الاهتزاز المختار بدل صوت الإشعار
        if (center.delegate == null) center.delegate = alertPresenter
        center.removeAllPendingNotificationRequests()
        if (!AppSettings.notify) return
        val s = Lang.current.strings
        val p = AppSettings.place
        val today = LocalDate.now()
        for (offset in 0 until DAYS) {
            val day = today.plusDays(offset)
            val t = PrayTimes.compute(day, p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi)
            for (key in ALERT_PRAYERS) {
                val mode = AppSettings.alertMode(key)
                if (mode == AlertMode.OFF) continue
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
                    setUserInfo(mapOf<Any?, Any?>("prayer" to key, "mode" to mode.key))
                    setSound(
                        when (mode) {
                            AlertMode.ADHAN -> UNNotificationSound.soundNamed(Muezzins.forPrayer(key).iosSound)
                            AlertMode.VIBRATE -> UNNotificationSound.soundNamed(VIBRATE_SOUND)
                            else -> null
                        }
                    )
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

/** إشعار الأذان والتطبيق في الواجهة: يُعرض بلا صوته، ويُشغَّل الأذان كاملاً أو نمط الاهتزاز المختار */
// صنف لا object: Kotlin/Native لا يقبل object يرث صنفاً من Objective-C عند الربط. نسخة واحدة تبقى حية
// (مفوَّض مركز الإشعارات مرجع ضعيف)
private val alertPresenter = AlertPresenter()

private class AlertPresenter : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        val info = willPresentNotification.request.content.userInfo
        val prayer = info["prayer"] as? String
        val show = UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList
        if (prayer == null) return withCompletionHandler(show or UNNotificationPresentationOptionSound)
        when (AlertMode.of(info["mode"] as? String)) {
            AlertMode.ADHAN -> { AdhanAudio.alertPrayer = prayer; AdhanAudio.play(Muezzins.forPrayer(prayer), preview = false) {} }
            AlertMode.VIBRATE -> AdhanAudio.vibrate(AppSettings.vibePattern)
            else -> {}
        }
        withCompletionHandler(show)
    }
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

/**
 * البوصلة: اتجاه الجهاز من CoreLocation. الاتجاه وحده لا يحتاج إذن الموقع، فلا يُطلب الإذن بفتح شاشة القبلة
 * (يُطلب فقط بزر «استعمل موقعي»). إن كان الإذن ممنوحاً من قبل تُشغَّل تحديثات موقع تقريبية أيضاً،
 * فيعطي النظام الشمال الحقيقي؛ وإلا فالشمال المغناطيسي.
 */
private class HeadingWatcher(val onHeading: (Float, Boolean) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
    val manager = CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyKilometer }
    private var locating = false
    fun start() {
        manager.delegate = this
        manager.headingFilter = 1.0
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> { locating = true; manager.startUpdatingLocation() }
            else -> {}
        }
        manager.startUpdatingHeading()
    }
    fun stop() {
        manager.stopUpdatingHeading()
        if (locating) manager.stopUpdatingLocation()
        manager.delegate = null
    }
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {}
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {}
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
