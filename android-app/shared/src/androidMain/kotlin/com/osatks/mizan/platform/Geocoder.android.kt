package com.osatks.mizan.platform

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * محدد المواقع في أندرويد (android.location.Geocoder): الدالة غير المتزامنة في Android 13+،
 * والمتزامنة في خيط خلفي قبله. لا يعمل بلا خدمة محدد مواقع في الجهاز (Geocoder.isPresent) أو بلا شبكة.
 */
actual object PlaceGeocoder {
    private const val TIMEOUT_MS = 15_000L

    @Suppress("DEPRECATION") // getFromLocation المتزامنة: قبل Android 13 وحده
    actual fun lookup(lat: Double, lng: Double, lang: String, onResult: (GeoName?) -> Unit) {
        val main = Handler(Looper.getMainLooper())
        val done = AtomicBoolean(false)
        // النتيجة مرة واحدة على الخيط الرئيسي: أول ما يصل من الجواب أو الخطأ أو انقضاء المهلة
        val finish: (GeoName?) -> Unit = { g -> if (done.compareAndSet(false, true)) { main.post { onResult(g) } } }
        val ctx = if (AndroidContext.ready) AndroidContext.app else null
        if (ctx == null || !runCatching { Geocoder.isPresent() }.getOrDefault(false)) { finish(null); return }
        try {
            val geocoder = Geocoder(ctx, Locale.forLanguageTag(lang))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) { finish(addresses.firstOrNull()?.let(::toName)) }
                    override fun onError(errorMessage: String?) { finish(null) }
                })
            } else {
                Thread {
                    val list = try { geocoder.getFromLocation(lat, lng, 1) } catch (e: Exception) { null }
                    finish(list?.firstOrNull()?.let(::toName))
                }.apply { isDaemon = true }.start()
            }
        } catch (e: Exception) {
            finish(null)
            return
        }
        main.postDelayed({ finish(null) }, TIMEOUT_MS)
    }

    private fun toName(a: Address): GeoName? = geoName(a.locality, a.subAdminArea, a.adminArea, a.countryCode)
}
