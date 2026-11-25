package com.osatks.mizan.platform

/** اسم مكان من محدد المواقع في النظام: المدينة أو البلدة، ورمز الدولة (ISO) إن عُرف */
class GeoName(val name: String, val cc: String?)

/**
 * محدد المواقع العكسي في النظام (Geocoder في أندرويد، CLGeocoder في iOS): بلا مكتبات ولا خوادم أخرى.
 * الاسم: المدينة (locality)، وإلا المحافظة الفرعية، وإلا المنطقة.
 */
expect object PlaceGeocoder {
    /** [lang] رمز لغة الواجهة؛ [onResult] على الخيط الرئيسي مرة واحدة، بـ null إن تعذّر (لا شبكة أو لا نتيجة) */
    fun lookup(lat: Double, lng: Double, lang: String, onResult: (GeoName?) -> Unit)
}

/** أول اسم غير فارغ: المدينة ثم المحافظة الفرعية ثم المنطقة، مع رمز الدولة بحرفين كبيرين */
internal fun geoName(locality: String?, subAdmin: String?, admin: String?, cc: String?): GeoName? =
    listOf(locality, subAdmin, admin).firstOrNull { !it.isNullOrBlank() }
        ?.let { GeoName(it.trim(), cc?.trim()?.takeIf { c -> c.length == 2 }?.uppercase()) }
