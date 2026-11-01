package com.osatks.mizan.core

import com.osatks.mizan.platform.Platform
import kotlinx.datetime.LocalDate

/**
 * متى يُطلب التقييم تلقائياً: بعد استعمال حقيقي (خمس جلسات على الأقل في ثلاثة أيام مختلفة أو أكثر)،
 * ومرة واحدة على الأكثر لكل إصدار. النظام نفسه يحدّ عدد المرات (ثلاث في السنة في iOS).
 */
object ReviewPrompt {
    const val MIN_SESSIONS = 5
    const val MIN_DAYS = 3
    private const val SESSIONS = "rv_sessions"
    private const val DAYS = "rv_days"
    private const val LAST_DAY = "rv_last_day"
    private const val ASKED = "rv_asked_version"

    private var counted = false

    /** جلسة جديدة (مرة لكل تشغيل) */
    fun onLaunch(today: LocalDate = LocalDate.now()) {
        if (counted) return
        counted = true
        AppSettings.putInt(SESSIONS, AppSettings.getInt(SESSIONS) + 1)
        val day = today.toEpochDays().toInt()
        if (AppSettings.getInt(LAST_DAY, -1) != day) {
            AppSettings.putInt(LAST_DAY, day)
            AppSettings.putInt(DAYS, AppSettings.getInt(DAYS) + 1)
        }
    }

    fun due(version: String = Platform.versionName()): Boolean =
        AppSettings.getInt(SESSIONS) >= MIN_SESSIONS && AppSettings.getInt(DAYS) >= MIN_DAYS &&
            AppSettings.getString(ASKED) != version

    fun markAsked(version: String = Platform.versionName()) = AppSettings.putString(ASKED, version)
}
