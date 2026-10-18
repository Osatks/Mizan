package com.osatks.mizan.i18n

/** اختيار لغة من داخل التطبيق (وفي أندرويد 13+ تُزامَن مع «لغات التطبيقات» في النظام) */
expect object LocaleSync {
    fun choose(pref: String)
}
