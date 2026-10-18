package com.osatks.mizan.i18n

import com.osatks.mizan.core.AppSettings

actual object LocaleSync {
    actual fun choose(pref: String) = AppSettings.updateLang(pref)
}
