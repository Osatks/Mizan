package com.osatks.mizan.i18n

import com.osatks.mizan.core.iso
import com.osatks.mizan.core.*
import androidx.compose.runtime.staticCompositionLocalOf
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import kotlinx.datetime.LocalDate
import com.osatks.mizan.platform.Platform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import com.osatks.mizan.i18n.pack.LangPack
import com.osatks.mizan.i18n.pack.PackStrings

/**
 * لغة الواجهة. اللغات المدمجة لها كائن نصوص مكتوب بالكود، ولغات الحزم (مضمّنة في assets/lang أو منزَّلة
 * من الموقع) تُقرأ نصوصها من JSON عبر [PackStrings]. decimal/group: فاصل الكسور والآلاف (الأرقام لاتينية دائماً).
 * تتساوى لغتان إذا تساوى رمزهما، فتحديث الحزمة لا يغيّر هوية اللغة.
 */
class Lang private constructor(
    val code: String, val native: String, val rtl: Boolean, val decimal: Char, val group: Char,
    private val builtIn: (() -> Strings)?, val pack: LangPack?,
) {
    private val packStrings by lazy { PackStrings(pack!!) }

    val strings: Strings get() = builtIn?.invoke() ?: packStrings


    /** لغة من حزمة وليست مكتوبة في التطبيق */
    val isPack: Boolean get() = pack != null

    override fun equals(other: Any?) = other is Lang && other.code == code
    override fun hashCode() = code.hashCode()
    override fun toString() = code

    companion object {
        const val SYSTEM = "system"

        val AR = Lang("ar", "العربية", true, '.', ',', { Ar }, null)
        val EN = Lang("en", "English", false, '.', ',', { En }, null)
        val FR = Lang("fr", "Français", false, ',', ' ', { Fr }, null)
        val TR = Lang("tr", "Türkçe", false, ',', '.', { Tr }, null)
        val UR = Lang("ur", "اردو", true, '.', ',', { Ur }, null)
        val ID = Lang("id", "Bahasa Indonesia", false, ',', '.', { Id }, null)
        val MS = Lang("ms", "Bahasa Melayu", false, '.', ',', { Ms }, null)
        val FA = Lang("fa", "فارسی", true, '.', ',', { Fa }, null)
        val BN = Lang("bn", "বাংলা", false, '.', ',', { Bn }, null)
        val RU = Lang("ru", "Русский", false, ',', ' ', { Ru }, null)
        val DE = Lang("de", "Deutsch", false, ',', '.', { De }, null)
        val ES = Lang("es", "Español", false, ',', '.', { Es }, null)
        val UK = Lang("uk", "Українська", false, ',', ' ', { Uk }, null)

        /** اللغات المكتوبة في التطبيق */
        val BUILT_IN = listOf(AR, EN, FR, TR, UR, ID, MS, FA, BN, RU, DE, ES, UK)

        /** لغات الحزم المثبتة؛ تتغير عند تنزيل حزمة أو حذفها أو تحديثها فتُحدَّث الواجهة
         *  (المقارنة بالمرجع: الحزمة المحدَّثة تساوي القديمة في الرمز) */
        var packs by mutableStateOf(emptyList<Lang>(), referentialEqualityPolicy())
            private set

        /** كل اللغات المتاحة الآن: المدمجة ثم الحزم */
        val entries: List<Lang> get() = BUILT_IN + packs

        fun fromPack(p: LangPack) = Lang(p.code, p.native, p.rtl, p.decimal, p.group, null, p)

        /** يضيف لغات الحزم أو يستبدلها (ولا تحل حزمة محل لغة مدمجة) */
        fun register(list: List<LangPack>) {
            val add = list.filter { p -> BUILT_IN.none { it.code == p.code } }.map(::fromPack)
            packs = (packs.filter { l -> add.none { it.code == l.code } } + add).sortedBy { it.code }
        }

        fun unregister(code: String) { packs = packs.filter { it.code != code } }

        fun of(code: String): Lang? = entries.firstOrNull { it.code == code }

        /** لغة الجهاز إن كانت متاحة، وإلا الإنجليزية (Android قد يعطي "in" للإندونيسية) */
        fun fromTag(tag: String): Lang = when (val c = tag.substringBefore('-').substringBefore('_').lowercase()) {
            "in" -> ID
            else -> of(c) ?: EN
        }

        fun resolve(pref: String, systemTag: String): Lang = of(pref) ?: fromTag(systemTag)

        /** اللغة الحالية خارج واجهة Compose (الإشعارات، التنسيق) */
        val current: Lang get() = resolve(AppSettings.lang, Platform.systemLanguage())
    }
}

/** النصوص المعروضة حالياً؛ تتأخر قليلاً عن الإعداد أثناء حركة تبديل اللغة */
val LocalStrings = staticCompositionLocalOf<Strings> { Ar }
val LocalLang = staticCompositionLocalOf { Lang.AR }

// ---------- مساعدات تنسيق مشتركة ----------

fun Strings.hijri(h: HDate?): String = if (h == null) "—" else hijriDate(h.d, h.m, h.y)
fun Strings.greg(d: LocalDate): String = gregDate(d.dayOfMonth, d.monthValue, d.year)
fun Strings.weekday(d: LocalDate): String = weekdays[d.dayOfWeek.iso - 1]
fun Strings.time(hours: Double): String = PrayTimes.format(hours, AppSettings.use24h, am, pm)
fun Strings.prayer(key: String): String = prayers[key] ?: key
fun Strings.city(key: String): String = cities[key] ?: En.cities[key] ?: key

/**
 * اسم الموقع، بالترتيب:
 * ١) مدينة اختارها المستخدم من القائمة: اسمها كما هو.
 * ٢) موقع GPS: اسمه من محدد المواقع في النظام إن وصل بلغة الواجهة.
 * ٣) وإلا المدينة المعروفة إن كان فيها أو على بعد [LocationHelper.NEAR_KM] كم منها (لا يُعتمد مفتاح محفوظ قديم
 *    كان يُضبط حتى بعد نحو ١٥٠ كم).
 * ٤) وإلا اسمه من محدد المواقع بلغة سابقة (حتى يصل بلغة الواجهة)، وإلا «موقعي الحالي».
 */
fun Strings.placeName(p: Place): String {
    val geo = p.name?.takeIf { it.isNotBlank() }
    if (!p.gps) return p.key?.let { city(it) } ?: geo ?: myLocation
    val code = langCode()
    if (geo != null && (code == null || p.nameLang == null || p.nameLang == code)) return geo
    LocationHelper.nearCity(p.lat, p.lng)?.let { return city(it.key) }
    return geo ?: myLocation
}

/** رمز لغة هذه النصوص (null إن لم تكن لغة معروفة) */
private fun Strings.langCode(): String? = Lang.entries.firstOrNull { it.strings === this }?.code

/** رمز دولة الموقع: دولة المدينة المختارة، ولموقع GPS دولته من محدد المواقع أو دولة المدينة القريبة */
fun placeCountryCode(p: Place): String? =
    if (!p.gps) p.city?.cc else p.cc?.takeIf { it.isNotBlank() } ?: LocationHelper.nearCity(p.lat, p.lng)?.cc

/** سطر الموقع تحت عنوان المواقيت: اسمه ثم دولته إن عُرفت */
fun Strings.placeLine(p: Place, lang: Lang): String =
    placeName(p) + (placeCountryCode(p)?.let { " · " + country(it, lang) } ?: "")

/** اسم الدولة من رمزها، بلغة الواجهة */
fun Strings.country(cc: String, lang: Lang): String =
    if (cc == "PS") palestine else Platform.displayCountry(cc, lang.code).ifEmpty { cc }
