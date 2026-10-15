package com.osatks.mizan.i18n

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.osatks.mizan.core.AppSettings

/**
 * على Android 13+ يظهر ميزان في "لغات التطبيقات" بإعدادات النظام. نُبقي الاختيارين متطابقين:
 * ما يُختار داخل التطبيق يُرسل للنظام، وما يُختار من الإعدادات يُعتمد في التطبيق.
 */
object LocaleSync {
    fun push(ctx: Context, pref: String) {
        if (Build.VERSION.SDK_INT < 33) return
        val lm = ctx.getSystemService(LocaleManager::class.java) ?: return
        lm.applicationLocales = if (pref == Lang.SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(pref)
        AppSettings.putBool("lang_synced", true)
    }

    fun pull(ctx: Context) {
        if (Build.VERSION.SDK_INT < 33) return
        val lm = ctx.getSystemService(LocaleManager::class.java) ?: return
        // أول تشغيل بعد التحديث: اختيار التطبيق (العربية للمستخدمين السابقين) يُرسل للنظام
        if (!AppSettings.getBool("lang_synced", false)) { push(ctx, AppSettings.lang); return }
        val list = lm.applicationLocales
        val fromSystem = if (list.isEmpty) Lang.SYSTEM else Lang.fromLocale(list[0]).code
        if (fromSystem != AppSettings.lang) AppSettings.updateLang(fromSystem)
    }

    /** اختيار لغة من داخل التطبيق */
    fun choose(ctx: Context, pref: String) {
        AppSettings.updateLang(pref)
        push(ctx, pref)
    }
}
