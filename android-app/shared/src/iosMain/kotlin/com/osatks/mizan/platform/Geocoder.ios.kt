@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import platform.Foundation.NSLocale

/**
 * محدد المواقع في iOS (CLGeocoder) بلغة الواجهة (preferredLocale). يحتاج الشبكة، ويحدّ النظام عدد الطلبات،
 * فلا يُطلب إلا لموقع جديد أو لغة جديدة (PlaceNames).
 */
@Suppress("DEPRECATION") // CLGeocoder موسوم بالقديم في iOS 26 (MapKit بديله) ويبقى يعمل، والتطبيق يدعم iOS 15
actual object PlaceGeocoder {
    private const val TIMEOUT_MS = 15_000L

    /** الطلبات الجارية: يبقى مرجع كل CLGeocoder حتى يصل جوابه */
    private val active = mutableListOf<CLGeocoder>()

    actual fun lookup(lat: Double, lng: Double, lang: String, onResult: (GeoName?) -> Unit) {
        runOnMain {
            val geocoder = CLGeocoder()
            active.add(geocoder)
            var done = false
            val finish: (GeoName?) -> Unit = { g ->
                if (!done) {
                    done = true
                    active.remove(geocoder)
                    onResult(g)
                }
            }
            geocoder.reverseGeocodeLocation(
                CLLocation(latitude = lat, longitude = lng),
                preferredLocale = NSLocale(localeIdentifier = lang),
            ) { placemarks, _ ->
                val p = placemarks?.firstOrNull() as? CLPlacemark
                val g = p?.let { geoName(it.locality, it.subAdministrativeArea, it.administrativeArea, it.ISOcountryCode) }
                runOnMain { finish(g) }
            }
            MainScope().launch {
                delay(TIMEOUT_MS)
                if (!done) {
                    geocoder.cancelGeocode()
                    finish(null)
                }
            }
        }
    }
}
