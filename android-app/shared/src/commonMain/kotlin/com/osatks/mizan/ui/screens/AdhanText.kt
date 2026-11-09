package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.AlertMode
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.i18n.Strings

/** اسم طريقة التنبيه بلغة الواجهة */
fun Strings.alertModeName(m: AlertMode) = when (m) {
    AlertMode.ADHAN -> alertModeAdhan
    AlertMode.VIBRATE -> alertModeVibrate
    AlertMode.SILENT -> alertModeSilent
    AlertMode.OFF -> alertModeOff
}

/** اسم المؤذن بلغة الواجهة */
fun Strings.muezzinName(m: Muezzin) = muezzinNames[m.id] ?: m.id

/** مدة التسجيل «د:ثث» (صفوف المؤذنين في الشاشتين) */
internal fun duration(sec: Int) = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
