package com.osatks.mizan.widget

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.platform.ArQibla
import platform.Foundation.NSNotificationCenter

/**
 * جسر القبلة بالكاميرا: Swift يخبر بدعم الجهاز عند الإقلاع ([start])، وزر شاشة القبلة يرسل إشعار [OPEN] بـ JSON فيه
 * اتجاه القبلة من الشمال الحقيقي (بالدرجات) ونصوص الشاشة بلغة التطبيق، فيعرض ArQibla.swift شاشة الكاميرا.
 */
object IosArQibla {
    const val OPEN = "MizanArQibla"

    fun start(supported: Boolean) {
        ArQibla.supported = supported
        ArQibla.open = if (supported) ({ post() }) else null
    }

    private fun post() {
        val json = runCatching { payload() }.getOrNull() ?: return
        NSNotificationCenter.defaultCenter.postNotificationName(OPEN, json)
    }

    internal fun payload(): String {
        AppSettings.init()
        val lang = Lang.current
        val s = lang.strings
        val ft = FeatureText.of(lang.code)
        val cal = com.osatks.mizan.i18n.QiblaCalibText.of(lang.code)
        val p = AppSettings.place
        return Json.write(
            linkedMapOf<String, Any?>(
                "bearing" to PrayTimes.qibla(p.lat, p.lng),
                "distance" to "${s.distanceToKaaba}: ${Fmt.num(PrayTimes.distanceKm(p.lat, p.lng), 0)} ${s.unitKm}",
                "place" to s.placeName(p),
                "title" to ft.arQibla,
                "hint" to ft.arHint,
                "facing" to s.facingQibla,
                "turn" to ft.arTurn,
                "cameraDenied" to ft.cameraDenied,
                "unsupported" to ft.arUnsupported,
                "openSettings" to s.openSettings,
                "close" to s.close,
                "calibTitle" to cal.title,
                "calibSteps" to cal.all("ios").mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n"),
                "calibReset" to cal.reset,
                "calibDone" to cal.done,
                "rtl" to lang.rtl,
            ) + LiveStates.palette(AppSettings.accent)
        )
    }
}
