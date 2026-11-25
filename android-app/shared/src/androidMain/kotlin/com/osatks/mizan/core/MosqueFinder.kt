package com.osatks.mizan.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * المساجد القريبة من خادم Overpass (OpenStreetMap) في أندرويد، بلا خدمات Google ولا مفتاح.
 * يُرسَل الاستعلام وحده (الإحداثيات مقرّبة إلى نحو 100 متر في [Mosques.query])، باسم عام «Mizan» بدل وكيل النظام
 * الذي يحمل طراز الجهاز، وبلا أي معرّف. يبحث في 5 كم، ثم في 15 كم إن قلّت النتائج عن 5.
 * آخر نتيجة تبقى في الذاكرة طوال الجلسة: فتح الصفحة مرة أخرى من المكان نفسه لا يعيد الطلب.
 */
object MosqueFinder {
    private class Last(val lat: Double, val lng: Double, val lang: String, val raw: List<Mosque>)

    @Volatile
    private var last: Last? = null

    /** النتيجة المحفوظة إن كان الموقع على بعد أقل من 250 متراً من موقعها (المسافات تُعاد من الموقع الجديد) */
    fun cached(lat: Double, lng: Double, lang: String): List<NearMosque>? =
        last?.takeIf { it.lang == lang && Mosques.distanceM(it.lat, it.lng, lat, lng) < 250 }
            ?.let { Mosques.nearest(it.raw, lat, lng) }

    /** يرمي استثناءً إن تعذّر الاتصال أو ردّ الخادم بخطأ؛ والقائمة الفارغة تعني أن لا مساجد مسجلة حوله */
    suspend fun find(lat: Double, lng: Double, lang: String): List<NearMosque> {
        cached(lat, lng, lang)?.let { return it }
        val raw = withContext(Dispatchers.IO) {
            val near = fetch(lat, lng, Mosques.NEAR_M, lang)
            if (Mosques.nearest(near, lat, lng).size >= Mosques.MIN_RESULTS) near
            else {
                // الأوسع إن قلّت النتائج؛ وإن فشل وعندنا نتائج قريبة فهي تكفي
                try {
                    fetch(lat, lng, Mosques.WIDE_M, lang)
                } catch (e: Exception) {
                    if (near.isEmpty()) throw e else near
                }
            }
        }
        last = Last(lat, lng, lang, raw)
        return Mosques.nearest(raw, lat, lng)
    }

    private fun fetch(lat: Double, lng: Double, radius: Int, lang: String): List<Mosque> {
        val body = "data=" + URLEncoder.encode(Mosques.query(lat, lng, radius), "UTF-8")
        val conn = URL(Mosques.OVERPASS).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "Mizan")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) error("Overpass HTTP $code")
            val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            return Mosques.parse(text, lang)
        } finally {
            conn.disconnect()
        }
    }
}
