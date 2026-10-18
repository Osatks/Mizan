package com.osatks.mizan.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.platform.Prefs
import com.osatks.mizan.platform.edit

/**
 * موقع المستخدم. key مفتاح مدينة معروفة (يُترجم اسمها حسب اللغة)، أو null لموقع GPS بعيد عن أي مدينة.
 */
data class Place(
    val key: String?, val lat: Double, val lng: Double,
    val tz: String, val method: String, val gps: Boolean = false
) {
    val city: City? get() = PrayTimes.city(key)

    companion object {
        fun of(c: City) = Place(c.key, c.lat, c.lng, c.tz, c.method)
    }
}

/** إعدادات التطبيق المحفوظة على الجهاز، مع حالة Compose للتحديث الفوري */
object AppSettings {
    private lateinit var sp: Prefs
    val ready: Boolean get() = ::sp.isInitialized

    var place by mutableStateOf(Place.of(PrayTimes.CITIES[0]))
        private set
    var method by mutableStateOf("makkah")
        private set
    var hanafi by mutableStateOf(false)
        private set
    var use24h by mutableStateOf(false)
        private set
    var theme by mutableStateOf("system") // system | light | dark
        private set
    var notify by mutableStateOf(false)
        private set
    /** "system" أو رمز لغة من [Lang] */
    var lang by mutableStateOf(Lang.SYSTEM)
        private set

    /** يُستدعى عند بدء التطبيق (في أندرويد بعد ضبط السياق: AppSettings.init(context)) */
    fun init() {
        if (::sp.isInitialized) return
        sp = Prefs("mizan")
        migrate()
        if (sp.contains("place_lat")) {
            place = Place(
                sp.getString("place_key")?.takeIf { it.isNotEmpty() },
                (sp.getString("place_lat") ?: "0").toDouble(), (sp.getString("place_lng") ?: "0").toDouble(),
                (sp.getString("place_tz") ?: systemTimeZoneId()), (sp.getString("place_method") ?: "mwl"),
                sp.getBool("place_gps", false)
            )
        }
        method = (sp.getString("method") ?: place.method)
        hanafi = sp.getBool("hanafi", false)
        use24h = sp.getBool("use24h", false)
        theme = (sp.getString("theme") ?: "system")
        notify = sp.getBool("notify", false)
        lang = (sp.getString("lang") ?: Lang.SYSTEM)
        LanguagePacks.init()
    }

    /** ترحيل بيانات الإصدارات العربية فقط: اسم المدينة المحفوظ نصاً يصبح مفتاحاً، ويبقى المستخدم الحالي على العربية */
    private fun migrate() {
        if (sp.isEmpty()) return
        val e = sp.edit()
        if (!sp.contains("lang")) e.putString("lang", Lang.AR.code)
        if (sp.contains("place_name") && !sp.contains("place_key")) {
            val name = (sp.getString("place_name") ?: "")
            e.putString("place_key", Ar.cities.entries.firstOrNull { it.value == name }?.key ?: "")
            e.remove("place_name").remove("place_country")
        }
        e.apply()
    }

    fun updatePlace(p: Place) {
        place = p
        method = p.method
        sp.edit()
            .putString("place_key", p.key ?: "")
            .putString("place_lat", p.lat.toString()).putString("place_lng", p.lng.toString())
            .putString("place_tz", p.tz).putString("place_method", p.method).putBoolean("place_gps", p.gps)
            .putString("method", p.method)
            .apply()
    }

    fun updateMethod(v: String) { method = v; sp.edit().putString("method", v).apply() }
    fun updateHanafi(v: Boolean) { hanafi = v; sp.edit().putBoolean("hanafi", v).apply() }
    fun updateUse24h(v: Boolean) { use24h = v; sp.edit().putBoolean("use24h", v).apply() }
    fun updateTheme(v: String) { theme = v; sp.edit().putString("theme", v).apply() }
    fun updateNotify(v: Boolean) { notify = v; sp.edit().putBoolean("notify", v).apply() }
    fun updateLang(v: String) { lang = v; sp.edit().putString("lang", v).apply() }

    // تخزين عام بسيط للأدوات (المسبحة وغيرها)
    fun getInt(k: String, d: Int = 0) = sp.getInt(k, d)
    fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun getString(k: String, d: String = "") = sp.getString(k) ?: d
    fun putString(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun getBool(k: String, d: Boolean) = sp.getBool(k, d)
    fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
}
