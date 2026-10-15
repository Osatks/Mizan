package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.PrayerTimes
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class NextPrayer(val key: String, val at: LocalDateTime, val remaining: Duration)

object PrayerUtil {
    fun zone(): ZoneId = runCatching { ZoneId.of(AppSettings.place.tz) }.getOrDefault(ZoneId.systemDefault())
    fun now(): LocalDateTime = LocalDateTime.now(zone())

    fun times(day: LocalDate): PrayerTimes {
        val p = AppSettings.place
        return PrayTimes.compute(day, p.lat, p.lng, p.tz, AppSettings.method, AppSettings.hanafi)
    }

    fun at(day: LocalDate, hours: Double): LocalDateTime = day.atStartOfDay().plusSeconds((hours * 3600).toLong())

    fun next(): NextPrayer {
        val now = now()
        for (offset in 0L..1L) {
            val day = now.toLocalDate().plusDays(offset)
            val t = times(day)
            for (key in PrayTimes.PRAYERS) {
                if (key == "sunrise" || t[key].isNaN()) continue
                val dt = at(day, t[key])
                if (dt.isAfter(now)) return NextPrayer(key, dt, Duration.between(now, dt))
            }
        }
        val dt = now.plusHours(1)
        return NextPrayer("fajr", dt, Duration.ofHours(1))
    }

    /** ساعات:دقائق:ثوانٍ بأرقام لاتينية دائماً */
    fun hms(d: Duration): String {
        val s = d.seconds.coerceAtLeast(0)
        return "${Fmt.two(s / 3600)}:${Fmt.two((s % 3600) / 60)}:${Fmt.two(s % 60)}"
    }
}
