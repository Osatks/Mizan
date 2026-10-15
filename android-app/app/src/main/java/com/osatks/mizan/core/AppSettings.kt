package com.osatks.mizan.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.TimeZone

data class Place(
    val name: String, val country: String, val lat: Double, val lng: Double,
    val tz: String, val method: String, val gps: Boolean = false
)

/** إعدادات التطبيق المحفوظة على الجهاز، مع حالة Compose للتحديث الفوري */
object AppSettings {
    private lateinit var sp: SharedPreferences

    var place by mutableStateOf(defaultPlace())
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

    private fun defaultPlace() = PrayTimes.CITIES[0].let { Place(it.name, it.country, it.lat, it.lng, it.tz, it.method) }

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("mizan", Context.MODE_PRIVATE)
        if (sp.contains("place_name")) {
            place = Place(
                sp.getString("place_name", "")!!, sp.getString("place_country", "")!!,
                sp.getString("place_lat", "0")!!.toDouble(), sp.getString("place_lng", "0")!!.toDouble(),
                sp.getString("place_tz", TimeZone.getDefault().id)!!, sp.getString("place_method", "mwl")!!,
                sp.getBoolean("place_gps", false)
            )
        }
        method = sp.getString("method", place.method)!!
        hanafi = sp.getBoolean("hanafi", false)
        use24h = sp.getBoolean("use24h", false)
        theme = sp.getString("theme", "system")!!
        notify = sp.getBoolean("notify", false)
    }

    fun updatePlace(p: Place) {
        place = p
        method = p.method
        sp.edit()
            .putString("place_name", p.name).putString("place_country", p.country)
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

    // تخزين عام بسيط للأدوات (المسبحة وغيرها)
    fun getInt(k: String, d: Int = 0) = sp.getInt(k, d)
    fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun getString(k: String, d: String = "") = sp.getString(k, d) ?: d
    fun putString(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun getBool(k: String, d: Boolean) = sp.getBoolean(k, d)
    fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
}
