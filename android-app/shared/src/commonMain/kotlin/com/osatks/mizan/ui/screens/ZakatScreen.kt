package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.platform.Platform

// عملات الزكاة وحقولها (بلا واجهة؛ الشاشة في أندرويد ZakatMoneyScreen.kt). اسم الملف باقٍ لأن تصدير الحالات
// (CalcFixturesExport) يقرأ defaultCurrency من ZakatScreenKt

/** عملة: رمز ISO، ورمزها المختصر واسمها بالعربية (بقية اللغات تأخذ الاسم من النظام) */
internal data class Cur(val iso: String, val arSymbol: String, val arName: String)

private val CURRENCIES = listOf(
    Cur("SAR", "ر.س", "ريال سعودي"), Cur("AED", "د.إ", "درهم إماراتي"), Cur("KWD", "د.ك", "دينار كويتي"),
    Cur("QAR", "ر.ق", "ريال قطري"), Cur("BHD", "د.ب", "دينار بحريني"), Cur("OMR", "ر.ع", "ريال عماني"),
    Cur("EGP", "ج.م", "جنيه مصري"), Cur("JOD", "د.أ", "دينار أردني"), Cur("IQD", "د.ع", "دينار عراقي"),
    Cur("MAD", "د.م", "درهم مغربي"), Cur("DZD", "د.ج", "دينار جزائري"), Cur("TND", "د.ت", "دينار تونسي"),
    Cur("LBP", "ل.ل", "ليرة لبنانية"), Cur("SYP", "ل.س", "ليرة سورية"), Cur("YER", "ر.ي", "ريال يمني"),
    Cur("SDG", "ج.س", "جنيه سوداني"), Cur("LYD", "د.ل", "دينار ليبي"), Cur("TRY", "TRY", "ليرة تركية"),
    Cur("PKR", "PKR", "روبية باكستانية"), Cur("INR", "INR", "روبية هندية"), Cur("BDT", "BDT", "تاكا بنغلاديشي"),
    Cur("IDR", "IDR", "روبية إندونيسية"), Cur("MYR", "MYR", "رينغيت ماليزي"), Cur("IRR", "IRR", "ريال إيراني"),
    Cur("RUB", "₽", "روبل روسي"), Cur("GBP", "£", "جنيه إسترليني"), Cur("USD", "$", "دولار أمريكي"), Cur("EUR", "€", "يورو"),
    Cur("CAD", "C$", "دولار كندي"), Cur("AUD", "A$", "دولار أسترالي"),
    Cur("UAH", "₴", "هريفنيا أوكرانية"), Cur("MXN", "MX$", "بيزو مكسيكي"), Cur("ARS", "ARS", "بيزو أرجنتيني"),
    Cur("UZS", "UZS", "سوم أوزبكي"), Cur("KZT", "₸", "تنغي كازاخستاني"), Cur("KGS", "KGS", "سوم قيرغيزي"),
    Cur("TJS", "TJS", "سوموني طاجيكي"), Cur("AZN", "₼", "مانات أذربيجاني"),
)

/** عملات التطبيق، ومعها عملة حزمة اللغة الحالية إن لم تكن في القائمة */
internal fun currencies(lang: Lang): List<Cur> {
    val extra = lang.pack?.currency?.takeIf { iso -> CURRENCIES.none { it.iso == iso } && Platform.currencyName(iso, "en") != null }
        ?: return CURRENCIES
    return CURRENCIES + Cur(extra, extra, Platform.currencyName(extra, "ar") ?: extra)
}

/** العملة الافتراضية حسب لغة الواجهة عند أول استخدام */
private fun defaultCurrency(lang: Lang) = lang.pack?.currency ?: when (lang) {
    Lang.AR -> "SAR"; Lang.TR -> "TRY"; Lang.UR -> "PKR"; Lang.ID -> "IDR"; Lang.MS -> "MYR"
    Lang.FA -> "IRR"; Lang.BN -> "BDT"; Lang.RU -> "RUB"; Lang.FR, Lang.DE, Lang.ES -> "EUR"; Lang.UK -> "UAH"
    else -> "USD"
}

/** الإصدارات السابقة حفظت الرمز العربي ("ر.س")؛ يُحوَّل إلى رمز ISO */
internal fun savedCurrency(lang: Lang): String {
    val raw = AppSettings.getString("zk_cur")
    return currencies(lang).firstOrNull { it.iso == raw || it.arSymbol == raw }?.iso ?: defaultCurrency(lang)
}

internal fun Cur.symbol(lang: Lang) = when {
    lang == Lang.AR -> arSymbol
    iso == "USD" || iso == "EUR" || iso == "GBP" -> arSymbol
    else -> iso
}

internal fun Cur.name(lang: Lang) = if (lang == Lang.AR) arName else
    Platform.currencyName(iso, lang.code)?.replaceFirstChar { it.titlecase() } ?: iso

internal val ZAKAT_FIELDS = listOf("gold_price", "silver_price", "cash", "bank", "receivable", "other", "g24", "g22", "g21", "g18", "silver", "jewel", "stocks_trade", "stocks_long", "goods", "debts")
