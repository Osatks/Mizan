package com.osatks.mizan.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.TimeZone

/** تحديد الموقع عبر LocationManager مباشرة (دون خدمات Google) */
object LocationHelper {
    @SuppressLint("MissingPermission")
    fun fetch(ctx: Context, onResult: (Place?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = last != null && System.currentTimeMillis() - last.time < 30 * 60 * 1000
        if (fresh) { onResult(toPlace(last!!)); return }

        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
        if (provider == null) { onResult(last?.let { toPlace(it) }); return }

        val handler = Handler(Looper.getMainLooper())
        var done = false
        val finish = { loc: Location? ->
            if (!done) {
                done = true
                onResult((loc ?: last)?.let { toPlace(it) })
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lm.getCurrentLocation(provider, null, ctx.mainExecutor) { finish(it) }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this); finish(location)
                }
                // مطلوبة على Android 10 وما قبله
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            handler.postDelayed({ lm.removeUpdates(listener); finish(null) }, 15000)
        }
        handler.postDelayed({ finish(null) }, 20000)
    }

    private fun toPlace(loc: Location): Place {
        val lat = Math.round(loc.latitude * 10000) / 10000.0
        val lng = Math.round(loc.longitude * 10000) / 10000.0
        val (city, dist) = PrayTimes.nearestCity(lat, lng)
        val near = dist < 1.5
        return Place(
            name = if (near) city.name else "موقعي الحالي",
            country = if (near) city.country else "",
            lat = lat, lng = lng, tz = TimeZone.getDefault().id, method = city.method, gps = true
        )
    }
}
