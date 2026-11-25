package com.osatks.mizan.core

import com.osatks.mizan.platform.PlaceGeocoder
import com.osatks.mizan.platform.PrayerAlerts

/**
 * اسم موقع GPS بلغة الواجهة من محدد المواقع في النظام. يُحفظ الموقع أولاً ولا ينتظر الاسم؛ يُطلب في الخلفية
 * بعد كل موقع جديد وعند بدء التطبيق وتبدّل اللغة (LanguageHost)، ثم يُحدَّث المكان والأدوات والإشعارات.
 * يُستدعى على الخيط الرئيسي.
 */
object PlaceNames {
    /** يضبطها التطبيق: تحديث أدوات الشاشة الرئيسية بالاسم الجديد (Widgets.updateAll في أندرويد، IosWidgets في iOS) */
    var onNamed: () -> Unit = {}

    /** الطلب الجاري (الإحداثيات واللغة)، فلا يُكرَّر */
    private var pending: String? = null

    /** هل يحتاج المكان اسماً بهذه اللغة؟ (موقع GPS بلا اسم، أو اسمه بلغة أخرى) */
    fun needs(p: Place, lang: String): Boolean = p.gps && (p.name.isNullOrBlank() || p.nameLang != lang)

    /** يطلب اسم الموقع الحالي بلغة [lang] إن احتاجه */
    fun ensure(lang: String) {
        if (!AppSettings.ready || !AppSettings.placeSet) return
        val p = AppSettings.place
        if (!needs(p, lang)) return
        val req = "${p.lat},${p.lng},$lang"
        if (pending == req) return
        pending = req
        PlaceGeocoder.lookup(p.lat, p.lng, lang) { g ->
            if (pending == req) pending = null
            if (g != null && AppSettings.updatePlaceName(p.lat, p.lng, g.name, lang, g.cc)) {
                // نص إشعارات الأذان المجدولة في iOS فيه اسم الموقع
                runCatching { PrayerAlerts.reschedule() }
                runCatching { onNamed() }
            }
        }
    }
}
