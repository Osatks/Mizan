package com.osatks.mizan.widget

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.PrayerCalendar
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.datetime.toInstant
import platform.Foundation.NSNotificationCenter

/**
 * جسر مواقيت الصلاة في التقويم: Kotlin يحسب مواقيت ثلاثين يوماً بنصوص لغة التطبيق ويرسلها JSON بإشعار [SYNC]،
 * و CalendarSync.swift يكتبها بـ EventKit في تقويم خاص بها (أو يزيله)، ثم يستدعي [done].
 */
object IosCalendar {
    /** Kotlin → Swift: {on, prompt, name, events: [{title, start, end, notes}]} */
    const val SYNC = "MizanCalendarSync"
    const val DAYS = 30

    private var started = false

    fun start() {
        PrayerCalendar.supported = true
        if (started) return
        started = true
        PrayerCalendar.request = { on -> post(on, prompt = true) }
    }

    /** عند العودة إلى التطبيق: مواقيت الأيام القادمة إن كان مفعّلاً (بلا سؤال؛ Swift يتجاوزها إن لم تتغير) */
    fun refresh() {
        if (AppSettings.ready && PrayerCalendar.enabled && !PrayerCalendar.busy) post(true, prompt = false)
    }

    /** Swift → Kotlin بعد المحاولة */
    fun done(on: Boolean, ok: Boolean, denied: Boolean, quiet: Boolean) = PrayerCalendar.done(on, ok, denied, quiet)

    private fun post(on: Boolean, prompt: Boolean) {
        val json = runCatching { payload(on, prompt) }.getOrNull() ?: return done(on, ok = false, denied = false, quiet = !prompt)
        NSNotificationCenter.defaultCenter.postNotificationName(SYNC, json)
    }

    internal fun payload(on: Boolean, prompt: Boolean): String {
        AppSettings.init()
        val lang = Lang.current
        val s = lang.strings
        val zone = WidgetData.zone()
        val place = s.placeName(AppSettings.place)
        val events = ArrayList<Map<String, Any?>>()
        if (on) {
            val today = PrayerUtil.now().date
            for (d in 0 until DAYS) {
                val day = today.plusDays(d)
                val t = PrayerUtil.times(day)
                for (key in PrayTimes.PRAYERS) {
                    if (key == "sunrise" || t[key].isNaN()) continue
                    val start = PrayerUtil.at(day, t[key]).toInstant(zone).epochSeconds
                    // حتى الإقامة إن ضبطها المستخدم (بين 5 و 60 دقيقة)، وإلا ربع ساعة
                    val iqama = AppSettings.iqama[key] ?: 0
                    val minutes = if (iqama in 5..60) iqama else 15
                    events += linkedMapOf("title" to s.prayer(key), "start" to start, "end" to start + minutes * 60L, "notes" to place)
                }
            }
        }
        return Json.write(
            linkedMapOf(
                "on" to on, "prompt" to prompt, "name" to FeatureText.of(lang.code).calendarName, "events" to events,
            )
        )
    }
}
