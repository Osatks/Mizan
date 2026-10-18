package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import com.osatks.mizan.core.Place

/** تنبيهات الأذان: تُعاد جدولتها بعد أي تغيير في الموقع أو طريقة الحساب أو الإعداد */
expect object PrayerAlerts {
    fun reschedule()

    /** تغيّرت لغة الواجهة: تُحدَّث نصوص قناة الإشعارات أو الإشعارات المجدولة */
    fun languageChanged()

    /** أندرويد 12+: صفحة «المنبهات الدقيقة» في الإعدادات (لا توجد في iOS) */
    val hasExactAlarmSettings: Boolean
    fun openExactAlarmSettings()
}

/** نتيجة طلب الموقع */
sealed interface LocationResult {
    data class Found(val place: Place) : LocationResult
    data object Failed : LocationResult
    data object Denied : LocationResult
}

/** يطلب إذن الموقع ثم يحدده؛ يعيد دالة تبدأ الطلب */
@Composable
expect fun rememberLocationRequest(onResult: (LocationResult) -> Unit): () -> Unit

/** يفعّل التنبيهات أو يوقفها، ويطلب إذن الإشعارات عند التفعيل إن لزم؛ onResult بالحالة النهائية */
@Composable
expect fun rememberNotifyToggle(onResult: (Boolean) -> Unit): (Boolean) -> Unit

/** لغة الجهاز داخل الواجهة (تتحدث إن غيّرها المستخدم في أندرويد والتطبيق مفتوح) */
@Composable
expect fun systemLanguageTag(): String

/** اتجاه الجهاز من الشمال الحقيقي بالدرجات (للقبلة) */
class Compass(val heading: Float, val available: Boolean, val accuracyLow: Boolean)

@Composable
expect fun rememberCompass(lat: Double, lng: Double): Compass
