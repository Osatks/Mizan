package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.EndAlerts
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.nowIn
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberNotifyToggle
import com.osatks.mizan.ui.ActionRole
import com.osatks.mizan.ui.DialogAction
import com.osatks.mizan.ui.PlatformDialog

/**
 * مفتاح «قبل خروج الوقت» لصلاة: مستقل عن تنبيه الأذان (قد يريد المستخدم هذا التذكير وحده)، فيطلب إذن الإشعارات
 * بنفسه عند التشغيل؛ إن رفضه المستخدم يبقى المفتاح مطفأً ويشرح ذلك ويفتح الإعدادات بطلبه (كما في [rememberAlertsToggle]).
 * كل تغيير يعيد جدولة التنبيهات.
 */
@Composable
fun rememberEndAlertToggle(): (String, Boolean) -> Unit {
    val s = LocalStrings.current
    // الصلاة التي ينتظر تشغيلها جواب طلب الإذن
    var pending by remember { mutableStateOf<String?>(null) }
    var denied by remember { mutableStateOf(false) }
    val ask = rememberNotifyToggle { ok ->
        val prayer = pending
        pending = null
        if (prayer != null) {
            if (ok) {
                AppSettings.updateEndAlert(prayer, true)
                PrayerAlerts.reschedule()
            } else denied = true
        }
    }
    if (denied) PlatformDialog(
        onDismissRequest = { denied = false },
        title = s.notifyOffTitle,
        message = s.notifyOffBody,
        actions = listOf(
            DialogAction(s.cancel, ActionRole.Cancel) { denied = false },
            DialogAction(s.openSettings, ActionRole.Preferred) { denied = false; AppStore.openSettings() },
        ),
    )
    return remember(ask) {
        { prayer, on ->
            if (on) {
                pending = prayer
                ask(true)
            } else {
                AppSettings.updateEndAlert(prayer, false)
                PrayerAlerts.reschedule()
            }
        }
    }
}

/** يغيّر «كم دقيقة قبل نهاية الوقت» ويعيد الجدولة */
fun setEndAlertMinutes(minutes: Int) {
    if (minutes !in EndAlerts.MINUTES || minutes == AppSettings.endAlertMinutes) return
    AppSettings.updateEndAlertMinutes(minutes)
    PrayerAlerts.reschedule()
}

/** مواقيت اليوم في مكان المستخدم، لعرض نهاية كل وقت تحت مفتاحه */
fun endAlertTimesToday(): PrayerTimes {
    val p = AppSettings.place
    return PrayTimes.compute(nowIn(p.tz).date, p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi, p.elev)
}
