package com.osatks.mizan.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * يحوّل إحداثيات الجهاز إلى موقع. اسمه يأتي لاحقاً من محدد المواقع في النظام ([PlaceNames])، وحتى يصل:
 * المدينة المعروفة إن كان فيها أو قريباً منها ([NEAR_KM])، وإلا «موقعي الحالي».
 * أقرب مدينة معروفة (مهما بعدت) تختار طريقة الحساب الافتراضية فقط.
 */
object LocationHelper {
    /** أبعد مسافة (كم) يُسمّى فيها موقع GPS باسم مدينة معروفة حين لا يُعرف اسمه */
    const val NEAR_KM = 25.0

    fun placeFrom(latitude: Double, longitude: Double): Place {
        val lat = round(latitude * 10000) / 10000.0
        val lng = round(longitude * 10000) / 10000.0
        val (city, _) = PrayTimes.nearestCity(lat, lng)
        return Place(
            key = nearCity(lat, lng)?.key,
            lat = lat, lng = lng, tz = systemTimeZoneId(), method = city.method, gps = true,
            elev = PrayTimes.elevationNear(lat, lng),
        )
    }

    /** المدينة المعروفة التي يقع فيها الموقع أو قربها (على [NEAR_KM] كم على الأكثر)، وإلا null */
    fun nearCity(lat: Double, lng: Double): City? =
        PrayTimes.CITIES.map { it to distanceKm(lat, lng, it.lat, it.lng) }.minBy { it.second }
            .takeIf { it.second <= NEAR_KM }?.first

    /** المسافة بين نقطتين على سطح الأرض بالكيلومترات (هافرسين) */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val rad = PI / 180.0
        val dLat = (lat2 - lat1) * rad
        val dLng = (lng2 - lng1) * rad
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * rad) * cos(lat2 * rad) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(sqrt(min(1.0, a)))
    }
}
