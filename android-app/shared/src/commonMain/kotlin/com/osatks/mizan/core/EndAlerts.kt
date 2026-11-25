package com.osatks.mizan.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * تنبيه «قرب خروج الوقت»: [prayer] الصلاة و[day] يومها (العشاء قد ينتهي بعد منتصف الليل فيقع تنبيهها في اليوم التالي)،
 * و[at] موعد التنبيه و[end] نهاية الوقت، وقتان محليان كما تُحسب المواقيت؛ [endHours] النهاية بالساعات من بداية [day].
 */
data class EndAlert(val prayer: String, val day: LocalDate, val at: LocalDateTime, val end: LocalDateTime, val endHours: Double)

/**
 * حساب تنبيهات قرب خروج الوقت من نهاية كل صلاة كما تعرضها صفحة المواقيت ([PrayerTimes.end]): الفجر بطلوع الشمس،
 * والظهر بدخول العصر، والعصر بالغروب (المغرب)، والمغرب بمغيب الشفق (العشاء)، والعشاء بمنتصف الليل الشرعي
 * (منتصف ما بين المغرب والفجر التالي). منطق مشترك: الإعدادات والجدولة في أندرويد و iOS تعتمد عليه.
 */
object EndAlerts {
    /** خيارات «كم دقيقة قبل نهاية الوقت» */
    val MINUTES = listOf(10, 15, 20, 30)
    const val DEFAULT_MINUTES = 15

    /** القيمة المحفوظة إن كانت من الخيارات، وإلا الافتراضية */
    fun minutesOrDefault(v: Int): Int = if (v in MINUTES) v else DEFAULT_MINUTES

    /** نهاية وقت الصلاة بالساعات من بداية يومها (قد تتجاوز 24 للعشاء)؛ NaN للشروق أو إن تعذّر الحساب */
    fun end(t: PrayerTimes, prayer: String): Double = if (prayer in ALERT_PRAYERS) t.end(prayer) else Double.NaN

    /**
     * موعد التنبيه بالساعات من بداية يوم الصلاة: نهاية الوقت ناقص [minutes]. NaN إن لم تُعرف البداية أو النهاية،
     * أو وقع التنبيه عند دخول الوقت أو قبله (وقت قصير جداً في العروض العليا): لا يسبق الأذانَ ولا يزاحمه.
     */
    fun hours(t: PrayerTimes, prayer: String, minutes: Int): Double {
        val start = t[prayer]
        val finish = end(t, prayer)
        if (start.isNaN() || finish.isNaN()) return Double.NaN
        val at = finish - minutes / 60.0
        return if (at > start) at else Double.NaN
    }

    /**
     * التنبيهات القادمة بعد [after] مرتبة زمنياً، لـ [days] يوماً تبدأ من [from]، للصلوات [prayers] فقط.
     * يُحسب يوم قبل [from] أيضاً: تنبيه عشاء الأمس قد يقع بعد منتصف الليل. [times] مواقيت اليوم المعطى.
     */
    fun upcoming(
        from: LocalDate, days: Int, after: LocalDateTime, prayers: Collection<String>, minutes: Int,
        times: (LocalDate) -> PrayerTimes,
    ): List<EndAlert> {
        if (prayers.isEmpty() || days <= 0) return emptyList()
        val out = ArrayList<EndAlert>()
        for (offset in -1 until days) {
            val day = from.plusDays(offset)
            val t = times(day)
            for (key in ALERT_PRAYERS) {
                if (key !in prayers) continue
                val h = hours(t, key, minutes)
                if (h.isNaN()) continue
                val start = day.atStartOfDay()
                val at = start.plusSeconds((h * 3600).toLong())
                if (!at.isAfter(after)) continue
                val endH = end(t, key)
                out += EndAlert(key, day, at, start.plusSeconds((endH * 3600).toLong()), endH)
            }
        }
        return out.sortedBy { it.at }
    }
}
