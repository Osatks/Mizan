package com.osatks.mizan.core

import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.pack.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** مسجد من خريطة OpenStreetMap: اسمه بلغة التطبيق إن وُجد، وموضعه (مركز المبنى للطرق والعلاقات)، وعنوانه إن سُجّل */
data class Mosque(val id: String, val name: String?, val lat: Double, val lng: Double, val address: String?)

/** مسجد بالنسبة إلى موقع المستخدم: المسافة بالأمتار، والاتجاه إليه من الشمال الجغرافي بالدرجات */
data class NearMosque(val mosque: Mosque, val meters: Double, val bearing: Double)

/**
 * المساجد القريبة في أندرويد من بيانات OpenStreetMap (خادم Overpass): نص الاستعلام، وقراءة الرد، والترتيب بالمسافة،
 * وتنسيق المسافة والاتجاه. الشبكة في androidMain (MosqueFinder)؛ وفي iPhone تبحث خرائط Apple (NativeMosques.swift).
 * للخصوصية: الإحداثيات المرسلة مقرّبة إلى 3 منازل عشرية (نحو 100 متر)، والمسافات تُحسب على الجهاز من الموقع الدقيق.
 */
object Mosques {
    const val OVERPASS = "https://overpass-api.de/api/interpreter"

    /** نصف قطر البحث الأول بالأمتار، ثم الأوسع إن قلّت النتائج عن [MIN_RESULTS] */
    const val NEAR_M = 5000
    const val WIDE_M = 15000
    const val MIN_RESULTS = 5

    /** أقصى ما يُعرض في القائمة (الأقرب أولاً) */
    const val MAX_SHOWN = 60

    /** إحداثية مقرّبة إلى 3 منازل عشرية بنقطة لاتينية دائماً (لا تتبع لغة الجهاز ولا الصيغة العلمية) */
    fun coord(v: Double): String {
        val t = round(v * 1000).toLong()
        val a = abs(t)
        return (if (t < 0) "-" else "") + (a / 1000) + "." + (a % 1000).toString().padStart(3, '0')
    }

    /**
     * استعلام Overpass: أماكن العبادة للمسلمين نقاطاً وطرقاً (مبانيَ) وعلاقات حول الموقع، مع مركز كل مبنى
     * (out center) ووسومه فقط، بلا قوائم العقد
     */
    fun query(lat: Double, lng: Double, radius: Int): String {
        val filter = "[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radius,${coord(lat)},${coord(lng)});"
        return "[out:json][timeout:25];(node$filter way$filter relation$filter);out tags center;"
    }

    /** رد Overpass (JSON) إلى مساجد؛ الاسم: name:<لغة التطبيق> ثم name، وإلا null (تعرض الواجهة «مسجد») */
    fun parse(text: String, lang: String): List<Mosque> {
        val elements = Json.obj(text)["elements"] as? List<*> ?: return emptyList()
        return elements.mapNotNull { e ->
            val o = e as? Map<*, *> ?: return@mapNotNull null
            val center = o["center"] as? Map<*, *>
            val lat = number(o["lat"] ?: center?.get("lat")) ?: return@mapNotNull null
            val lng = number(o["lon"] ?: center?.get("lon")) ?: return@mapNotNull null
            val tags = o["tags"] as? Map<*, *>
            fun tag(k: String): String? = (tags?.get(k) as? String)?.trim()?.takeIf { it.isNotEmpty() }
            val street = tag("addr:street")?.let { st -> tag("addr:housenumber")?.let { "$st $it" } ?: st }
            val address = tag("addr:full") ?: listOfNotNull(street, tag("addr:city")).joinToString(", ").takeIf { it.isNotEmpty() }
            Mosque(
                id = "${o["type"] ?: "node"}/${o["id"] ?: "$lat,$lng"}",
                name = tag("name:$lang") ?: tag("name"),
                lat = lat, lng = lng, address = address,
            )
        }
    }

    private fun number(v: Any?): Double? = (v as? Number)?.toDouble()

    /**
     * الأقرب أولاً، بلا تكرار: المسجد قد يُرسم نقطةً ومبنىً معاً، فما كان على بعد أقل من 60 متراً من مسجد قبله
     * بالاسم نفسه (أو بلا اسم) يُحذف
     */
    fun nearest(list: List<Mosque>, lat: Double, lng: Double, limit: Int = MAX_SHOWN): List<NearMosque> {
        val sorted = list.map { NearMosque(it, distanceM(lat, lng, it.lat, it.lng), bearing(lat, lng, it.lat, it.lng)) }
            .sortedBy { it.meters }
        val out = ArrayList<NearMosque>()
        for (m in sorted) {
            val dup = out.any { o ->
                distanceM(o.mosque.lat, o.mosque.lng, m.mosque.lat, m.mosque.lng) < 60 &&
                    (o.mosque.name == null || m.mosque.name == null || o.mosque.name.equals(m.mosque.name, ignoreCase = true))
            }
            if (dup) continue
            out += m
            if (out.size >= limit) break
        }
        return out
    }

    private fun rad(d: Double) = d * PI / 180.0

    /** المسافة بين نقطتين بالأمتار (هافرساين) */
    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = rad(lat2 - lat1)
        val dLng = rad(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(rad(lat1)) * cos(rad(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * 6_371_000.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** الاتجاه الابتدائي من النقطة الأولى إلى الثانية، 0..<360 من الشمال باتجاه عقارب الساعة */
    fun bearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val p1 = rad(lat1); val p2 = rad(lat2); val dl = rad(lng2 - lng1)
        val b = atan2(sin(dl) * cos(p2), cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)) * 180.0 / PI
        return (b + 360.0) % 360.0
    }

    /** المسافة بلغة الواجهة: بالأمتار (مقرّبة إلى 10) دون الكيلومتر، ثم بالكيلومتر بمنزلة عشرية حتى 10 كم */
    fun distance(meters: Double, s: Strings, lang: Lang = Lang.current): String =
        if (meters < 950) {
            val m = (round(meters / 10) * 10).toLong().coerceAtLeast(10)
            "${Fmt.int(m, lang)} ${s.unitM}"
        } else {
            "${Fmt.num(meters / 1000, if (meters < 9950) 1 else 0, lang)} ${s.unitKm}"
        }

    /** إحدى الجهات الثماني ([Strings.directions]: الشمال أولاً باتجاه عقارب الساعة) */
    fun direction(bearing: Double, s: Strings): String = s.directions[((round(bearing / 45.0).toInt() % 8) + 8) % 8]
}
