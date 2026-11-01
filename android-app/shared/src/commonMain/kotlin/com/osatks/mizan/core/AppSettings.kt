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

/** الإقامة الافتراضية بعد الأذان بالدقائق (المعمول به في أكثر المساجد) */
val IQAMA_DEFAULT = mapOf("fajr" to 25, "dhuhr" to 20, "asr" to 20, "maghrib" to 10, "isha" to 20)

/** إعدادات التطبيق المحفوظة على الجهاز، مع حالة Compose للتحديث الفوري */
object AppSettings {
    private lateinit var sp: Prefs
    val ready: Boolean get() = ::sp.isInitialized

    var place by mutableStateOf(Place.of(PrayTimes.CITIES[0]))
        private set
    /** هل اختار المستخدم موقعه (GPS أو مدينة)؟ قبل ذلك نطلبه بدل افتراض مكة */
    var placeSet by mutableStateOf(false)
        private set
    var method by mutableStateOf("makkah")
        private set
    /** ما اختاره المستخدم لوقت العصر؛ null: لم يختر شيئاً، فيتبع المعتمد رسمياً في طريقة الحساب ([Method.hanafi]) */
    private var hanafiChoice by mutableStateOf<Boolean?>(null)
    /** العصر بمثلَي الظل (الحنفي): اختيار المستخدم إن اختار، وإلا المعتمد في بلده */
    val hanafi: Boolean get() = hanafiChoice ?: PrayTimes.method(method).hanafi
    var use24h by mutableStateOf(false)
        private set
    /** الإقامة بعد الأذان بالدقائق لكل صلاة (0 = لا تُعرض)، ويغيّرها المستخدم */
    var iqama by mutableStateOf(IQAMA_DEFAULT)
        private set
    var theme by mutableStateOf("system") // system | light | dark
    var accent by mutableStateOf("emerald") // لون التطبيق: مفتاح من Accent
        private set
    var notify by mutableStateOf(false)
        private set
    /** طريقة التنبيه لكل صلاة (الأذان افتراضياً: من فعّل التنبيه في إصدار سابق يسمع الأذان في الصلوات كلها) */
    var alertModes by mutableStateOf(ALERT_PRAYERS.associateWith { AlertMode.ADHAN })
        private set
    /** المؤذن لكل الصلوات، ولصلاة الفجر وحدها ([Muezzins.SAME]: مثل البقية) */
    var muezzin by mutableStateOf("")
        private set
    var fajrMuezzin by mutableStateOf("")
        private set
    var vibePattern by mutableStateOf(VibePattern.DEFAULT)
        private set
    /** "system" أو رمز لغة من [Lang] */
    var lang by mutableStateOf(Lang.SYSTEM)
        private set

    /** يُستدعى عند بدء التطبيق (في أندرويد بعد ضبط السياق: AppSettings.init(context)) */
    fun init() {
        if (::sp.isInitialized) return
        sp = Prefs("mizan")
        migrate()
        placeSet = sp.contains("place_lat")
        if (placeSet) {
            place = Place(
                sp.getString("place_key")?.takeIf { it.isNotEmpty() },
                (sp.getString("place_lat") ?: "0").toDouble(), (sp.getString("place_lng") ?: "0").toDouble(),
                (sp.getString("place_tz") ?: systemTimeZoneId()), (sp.getString("place_method") ?: "mwl"),
                sp.getBool("place_gps", false)
            )
        }
        method = (sp.getString("method") ?: place.method)
        hanafiChoice = if (sp.contains("hanafi")) sp.getBool("hanafi", false) else null
        use24h = sp.getBool("use24h", false)
        iqama = IQAMA_DEFAULT.mapValues { (k, v) -> sp.getInt("iqama_$k", v) }
        theme = (sp.getString("theme") ?: "system")
        accent = (sp.getString("accent") ?: "emerald")
        notify = sp.getBool("notify", false)
        alertModes = ALERT_PRAYERS.associateWith { AlertMode.of(sp.getString("alert_$it")) }
        muezzin = sp.getString("muezzin") ?: Muezzins.DEFAULT.id
        fajrMuezzin = sp.getString("muezzin_fajr") ?: Muezzins.FAJR_DEFAULT.id
        vibePattern = VibePattern.of(sp.getString("vibe_pattern"))
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
        placeSet = true
        method = p.method
        sp.edit()
            .putString("place_key", p.key ?: "")
            .putString("place_lat", p.lat.toString()).putString("place_lng", p.lng.toString())
            .putString("place_tz", p.tz).putString("place_method", p.method).putBoolean("place_gps", p.gps)
            .putString("method", p.method)
            .apply()
    }

    fun updateMethod(v: String) { method = v; sp.edit().putString("method", v).apply() }
    fun updateHanafi(v: Boolean) { hanafiChoice = v; sp.edit().putBoolean("hanafi", v).apply() }
    fun updateUse24h(v: Boolean) { use24h = v; sp.edit().putBoolean("use24h", v).apply() }
    fun updateIqama(prayer: String, minutes: Int) {
        iqama = iqama + (prayer to minutes)
        sp.edit().putInt("iqama_$prayer", minutes).apply()
    }
    fun updateTheme(v: String) { theme = v; sp.edit().putString("theme", v).apply() }
    fun updateAccent(v: String) { accent = v; sp.edit().putString("accent", v).apply() }
    fun updateNotify(v: Boolean) { notify = v; sp.edit().putBoolean("notify", v).apply() }
    fun updateAlertMode(prayer: String, mode: AlertMode) {
        alertModes = alertModes + (prayer to mode)
        sp.edit().putString("alert_$prayer", mode.key).apply()
    }
    fun alertMode(prayer: String) = alertModes[prayer] ?: AlertMode.ADHAN
    fun updateMuezzin(id: String) { muezzin = id; sp.edit().putString("muezzin", id).apply() }
    fun updateFajrMuezzin(id: String) { fajrMuezzin = id; sp.edit().putString("muezzin_fajr", id).apply() }
    fun updateVibePattern(p: VibePattern) { vibePattern = p; sp.edit().putString("vibe_pattern", p.key).apply() }
    fun updateLang(v: String) { lang = v; sp.edit().putString("lang", v).apply() }

    // تخزين عام بسيط للأدوات (المسبحة وغيرها)
    fun getInt(k: String, d: Int = 0) = sp.getInt(k, d)
    fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun getString(k: String, d: String = "") = sp.getString(k) ?: d
    fun putString(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun getBool(k: String, d: Boolean) = sp.getBool(k, d)
    fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
}
