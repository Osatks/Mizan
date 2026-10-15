package com.osatks.mizan.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import java.time.LocalDate
import java.util.Locale

/**
 * لغات التطبيق. decimal/group: فاصل الكسور والآلاف (الأرقام تبقى لاتينية في كل اللغات).
 */
enum class Lang(val code: String, val native: String, val rtl: Boolean, val decimal: Char, val group: Char) {
    AR("ar", "العربية", true, '.', ','),
    EN("en", "English", false, '.', ','),
    FR("fr", "Français", false, ',', ' '),
    TR("tr", "Türkçe", false, ',', '.'),
    UR("ur", "اردو", true, '.', ','),
    ID("id", "Bahasa Indonesia", false, ',', '.'),
    MS("ms", "Bahasa Melayu", false, '.', ','),
    FA("fa", "فارسی", true, '.', ','),
    BN("bn", "বাংলা", false, '.', ','),
    RU("ru", "Русский", false, ',', ' '),
    DE("de", "Deutsch", false, ',', '.');

    val strings: Strings
        get() = when (this) {
            AR -> Ar; EN -> En; FR -> Fr; TR -> Tr; UR -> Ur; ID -> Id
            MS -> Ms; FA -> Fa; BN -> Bn; RU -> Ru; DE -> De
        }

    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        const val SYSTEM = "system"

        fun of(code: String): Lang? = entries.firstOrNull { it.code == code }

        /** لغة الجهاز إن كانت مدعومة، وإلا الإنجليزية (Android قد يعطي "in" للإندونيسية) */
        fun fromLocale(l: Locale): Lang = when (val c = l.language) {
            "in" -> ID
            else -> of(c) ?: EN
        }

        fun resolve(pref: String, system: Locale): Lang = of(pref) ?: fromLocale(system)

        /** اللغة الحالية خارج واجهة Compose (الإشعارات، التنسيق) */
        val current: Lang get() = resolve(AppSettings.lang, Locale.getDefault())
    }
}

/** النصوص المعروضة حالياً؛ تتأخر قليلاً عن الإعداد أثناء حركة تبديل اللغة */
val LocalStrings = staticCompositionLocalOf<Strings> { Ar }
val LocalLang = staticCompositionLocalOf { Lang.AR }

// ---------- مساعدات تنسيق مشتركة ----------

fun Strings.hijri(h: HDate?): String = if (h == null) "—" else hijriDate(h.d, h.m, h.y)
fun Strings.greg(d: LocalDate): String = gregDate(d.dayOfMonth, d.monthValue, d.year)
fun Strings.weekday(d: LocalDate): String = weekdays[d.dayOfWeek.value - 1]
fun Strings.time(hours: Double): String = PrayTimes.format(hours, AppSettings.use24h, am, pm)
fun Strings.prayer(key: String): String = prayers[key] ?: key
fun Strings.city(key: String): String = cities[key] ?: En.cities[key] ?: key

/** اسم الموقع: المدينة المعروفة، أو "موقعي الحالي" */
fun Strings.placeName(p: Place): String = p.key?.let { city(it) } ?: myLocation

/** اسم الدولة من رمزها، بلغة الواجهة */
fun Strings.country(cc: String, lang: Lang): String =
    if (cc == "PS") palestine else Locale("", cc).getDisplayCountry(lang.locale).ifEmpty { cc }
