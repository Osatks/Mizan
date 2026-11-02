package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.Lang

/**
 * مواقيت الصلاة في تقويم النظام (iPhone و iPad): تقويم خاص بها اسمه «مواقيت الصلاة · ميزان» فيه مواقيت ثلاثين يوماً،
 * يتجدد كلما عاد المستخدم إلى التطبيق، ويُزال كله بلمسة. الإعداد مشترك والتنفيذ في iOS (IosCalendar و CalendarSync.swift)؛
 * أندرويد لا يضبط [supported] فيبقى المفتاح مخفياً فيه.
 */
object PrayerCalendar {
    private const val KEY = "calendar_sync"

    /** يضبطه iOS عند الإقلاع */
    var supported by mutableStateOf(false)

    /** تُضاف المواقيت أو تُزال الآن */
    var busy by mutableStateOf(false)
        private set

    private var chosen by mutableStateOf<Boolean?>(null)

    val enabled: Boolean get() = chosen ?: (AppSettings.ready && AppSettings.getBool(KEY, false))

    /** iOS: يضيف المواقيت إلى التقويم أو يزيله، ثم يستدعي [done] */
    var request: ((on: Boolean) -> Unit)? = null

    fun update(on: Boolean) {
        val r = request ?: return
        if (busy) return
        busy = true
        r(on)
    }

    /** نتيجة iOS: [ok] تمّ، [denied] لا إذن بالوصول إلى التقويم. [quiet]: تحديث تلقائي عند العودة إلى التطبيق، بلا رسالة */
    fun done(on: Boolean, ok: Boolean, denied: Boolean, quiet: Boolean) {
        busy = false
        val ft = FeatureText.of(Lang.current.code)
        when {
            ok -> {
                chosen = on
                AppSettings.putBool(KEY, on)
                if (!quiet) Platform.toast(if (on) ft.calendarAdded else ft.calendarRemoved)
            }
            denied -> {
                // لا إذن (رفضه المستخدم أو سحبه من الإعدادات): يُطفأ المفتاح، ويُدلّ على الإعدادات إن طلب ذلك بنفسه
                chosen = false
                AppSettings.putBool(KEY, false)
                if (!quiet) {
                    Platform.toast(ft.calendarDenied)
                    AppStore.openSettings()
                }
            }
        }
    }
}
