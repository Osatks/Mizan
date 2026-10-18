package com.osatks.mizan.core

import kotlin.math.round

/** يحوّل إحداثيات الجهاز إلى موقع: أقرب مدينة معروفة إن كانت قريبة، وإلا «موقعي الحالي» */
object LocationHelper {
    fun placeFrom(latitude: Double, longitude: Double): Place {
        val lat = round(latitude * 10000) / 10000.0
        val lng = round(longitude * 10000) / 10000.0
        val (city, dist) = PrayTimes.nearestCity(lat, lng)
        val near = dist < 1.5
        return Place(
            key = if (near) city.key else null,
            lat = lat, lng = lng, tz = systemTimeZoneId(), method = city.method, gps = true
        )
    }
}
