package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings

/**
 * العدّ التنازلي الحي للصلاة القادمة على شاشة القفل وفي الجزيرة الديناميكية (Live Activity في iOS 16.2 فما بعد):
 * حين يغادر المستخدم التطبيق قبل الصلاة بأقل من ثلاث ساعات. الإعداد مشترك، والتنفيذ في iOS (IosLive)؛
 * أندرويد لا يضبط [supported] فيبقى المفتاح مخفياً فيه.
 */
object LiveCountdown {
    private const val KEY = "live_countdown"

    /** الجهاز يدعم الأنشطة الحية (يضبطه iOS عند الإقلاع) */
    var supported by mutableStateOf(false)

    /** المستخدم أطفأ الأنشطة الحية لميزان في إعدادات النظام */
    var systemOff by mutableStateOf(false)

    /** ما اختاره المستخدم في هذه الجلسة؛ null: يُقرأ من الإعدادات المحفوظة (مطفأ افتراضياً) */
    private var chosen by mutableStateOf<Boolean?>(null)

    val enabled: Boolean get() = chosen ?: (AppSettings.ready && AppSettings.getBool(KEY, false))

    /** iOS: يبدأ النشاط أو ينهيه فور تغيّر الإعداد */
    var onChange: ((Boolean) -> Unit)? = null

    fun update(on: Boolean) {
        chosen = on
        AppSettings.putBool(KEY, on)
        onChange?.invoke(on)
    }
}
