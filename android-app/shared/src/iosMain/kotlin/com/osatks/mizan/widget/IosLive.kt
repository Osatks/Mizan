@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.widget

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.isAfter
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.LiveCountdown
import com.osatks.mizan.platform.QuranSpotlight
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Recitation
import com.osatks.mizan.quran.RecitationLive
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.PrayerUtil
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * جسر الأنشطة الحية (Live Activities) والاختصارات في iOS. ActivityKit و App Intents في Swift وحدها، فيرسل Kotlin
 * حال كل نشاط بإشعار فيه JSON (كما يفعل IosWidgets مع الأدوات) ويبدؤه LiveActivities.swift أو يحدّثه أو ينهيه:
 *  - MizanLivePrayer: الصلاة القادمة إن بقي عليها أقل من ثلاث ساعات والإعداد مفعّل (عدّ تنازلي على شاشة القفل
 *    وفي الجزيرة الديناميكية)، عند مغادرة التطبيق وما زال في المقدمة (لا يُبدأ نشاط من الخلفية) وعند العودة إليه.
 *  - MizanLiveRecitation: السورة والآية والقارئ ما دامت التلاوة (RecitationLive).
 * ويستدعيه Swift مباشرة: start عند الإقلاع، و open للاختصارات وعناصر البحث، و nextPrayerJson لـ Siri.
 */
object IosLive {
    const val PRAYER = "MizanLivePrayer"
    const val RECITATION = "MizanLiveRecitation"
    /** أقصى مدة قبل الصلاة يبدأ فيها العدّ التنازلي الحي */
    private const val WINDOW_SEC = 3 * 3600L
    /** مسار «تابع القراءة»: آخر موضع قرأه المستخدم */
    const val CONTINUE = "quran/continue"

    private var started = false
    private val observers = ArrayList<NSObjectProtocol>()
    private var lastRecitation: String? = null

    /**
     * عند إقلاع التطبيق (AppDelegate). [supported]: الجهاز يدعم الأنشطة الحية (iOS 16.2 فما بعد)،
     * و [systemOff]: أطفأها المستخدم لميزان في إعدادات النظام.
     */
    fun start(supported: Boolean, systemOff: Boolean) {
        AppSettings.init()
        availability(supported, systemOff)
        RecitationLive.start()
        if (started) return
        started = true
        LiveCountdown.onChange = { prayer(start = true) }
        val nc = NSNotificationCenter.defaultCenter
        // قبل أن يغادر التطبيق الواجهة: ما زال في المقدمة فيُسمح ببدء النشاط (المراقب بلا طابور: يُنفَّذ فوراً)
        observers += nc.addObserverForName(UIApplicationWillResignActiveNotification, null, null) { _ -> prayer(start = true) }
        // العودة إلى التطبيق: يُنهى ما انقضى وقته ويُحدَّث الباقي
        observers += nc.addObserverForName(UIApplicationDidBecomeActiveNotification, null, null) { _ -> prayer(start = false) }
        // لا نشاط تلاوة باقٍ من تشغيل سابق أُغلق فيه التطبيق
        recitation(null)
        // فهرسة السور في Spotlight بعد أن تستقر الواجهة
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 6_000_000_000L), dispatch_get_main_queue()) { QuranSpotlight.index() }
    }

    /** يحدّثها Swift عند الإقلاع وعند كل عودة إلى التطبيق (قد يغيّرها المستخدم في الإعدادات) */
    fun availability(supported: Boolean, systemOff: Boolean) {
        LiveCountdown.supported = supported
        LiveCountdown.systemOff = systemOff
    }

    // ---------- الصلاة القادمة ----------

    private fun prayer(start: Boolean) {
        if (!LiveCountdown.supported) return
        post(PRAYER, runCatching { prayerJson(start) }.getOrElse { Json.write(mapOf("on" to false)) })
    }

    internal fun prayerJson(start: Boolean): String {
        val on = LiveCountdown.enabled && !LiveCountdown.systemOff
        val m = linkedMapOf<String, Any?>("on" to on, "start" to start)
        // قبل اختيار الموقع لا عدّ تنازلي (المواقيت الافتراضية لمكة ليست مواقيت المستخدم)
        if (on && AppSettings.placeSet) {
            val next = PrayerUtil.next()
            val secs = next.remaining.inWholeSeconds
            if (secs in 1..WINDOW_SEC) m["prayer"] = prayerMap(next.key, next.at)
        }
        return Json.write(m)
    }

    private fun epoch(t: LocalDateTime): Long = t.toInstant(WidgetData.zone()).epochSeconds

    /** آخر صلاة (بلا الشروق) دخل وقتها قبل [now]: بداية شريط الوقت */
    private fun previous(now: LocalDateTime): LocalDateTime? {
        var best: LocalDateTime? = null
        for (offset in -1..0) {
            val day = now.date.plusDays(offset)
            val t = PrayerUtil.times(day)
            for (key in PrayTimes.PRAYERS) {
                if (key == "sunrise" || t[key].isNaN()) continue
                val at = PrayerUtil.at(day, t[key])
                if (!at.isAfter(now)) best = at
            }
        }
        return best
    }

    private fun prayerMap(key: String, at: LocalDateTime): Map<String, Any?> {
        val lang = Lang.current
        val s = lang.strings
        val now = PrayerUtil.now()
        val name = s.prayer(key)
        val from = previous(now)?.let(::epoch) ?: (epoch(at) - WINDOW_SEC)
        return linkedMapOf<String, Any?>(
            "id" to "${at.date}-$key",
            "key" to key,
            "name" to name,
            "time" to WidgetData.clock(s, at),
            "at" to epoch(at),
            "from" to from,
            "hijri" to s.hijri(Hijri.toHijri(now.date)),
            "place" to s.placeName(AppSettings.place),
            "label" to WidgetText.of(lang.code).nextPrayer,
            "now" to s.prayerNowTitle(name),
            "rtl" to lang.rtl,
        ) + palette(AppSettings.accent)
    }

    /** ألوان النشاط بلون التطبيق الذي اختاره المستخدم */
    internal fun palette(accent: String): Map<String, Any?> {
        val c = WidgetColors.of(accent)
        return mapOf("hero1" to WidgetData.hex(c.hero1), "hero2" to WidgetData.hex(c.hero2), "gold" to WidgetData.hex(c.gold))
    }

    // ---------- التلاوة ----------

    /** حال «يُتلى الآن»؛ null حين تنتهي التلاوة. لا يُرسل إلا ما تغيّر */
    internal fun recitation(state: Map<String, Any?>?) {
        if (!LiveCountdown.supported) return
        val json = Json.write(if (state == null) mapOf("active" to false) else mapOf("active" to true) + state)
        if (json == lastRecitation) return
        lastRecitation = json
        post(RECITATION, json)
    }

    /** زر التشغيل والإيقاف في النشاط الحي (LiveActivityIntent في iOS 17) */
    fun toggleRecitation() {
        if (Recitation.active) Recitation.toggle()
    }

    // ---------- Siri والاختصارات ----------

    /** الصلاة القادمة لنافذة Siri واختصار «الصلاة القادمة»: الاسم والوقت والعدّ وعبارة تُقال */
    fun nextPrayerJson(): String? = runCatching {
        AppSettings.init()
        val lang = Lang.current
        val s = lang.strings
        val next = PrayerUtil.next()
        val name = s.prayer(next.key)
        val time = WidgetData.clock(s, next.at)
        Json.write(
            linkedMapOf<String, Any?>(
                "key" to next.key, "name" to name, "time" to time, "at" to epoch(next.at),
                "hijri" to s.hijri(Hijri.toHijri(PrayerUtil.now().date)),
                "place" to s.placeName(AppSettings.place),
                "label" to WidgetText.of(lang.code).nextPrayer,
                "dialog" to "${s.nextPrayerLabel(name)} ${s.atTime(time)}",
                "rtl" to lang.rtl,
            ) + palette(AppSettings.accent)
        )
    }.getOrNull()

    /**
     * يفتح شاشة في التطبيق من اختصار أو زر في مركز التحكم أو نتيجة في Spotlight (مسار من مسارات MizanApp).
     * «تابع القراءة» يصير صفحة آخر آية قرأها المستخدم.
     */
    fun open(route: String, section: String?) {
        AppSettings.init()
        val target = if (route == CONTINUE) {
            QuranPrefs.ensure()
            val last = QuranPrefs.last
            if (last >= 0) QuranMeta.locate(last).let { (surah, ayah) -> surahRoute(surah, ayah, "widget") } else "quran"
        } else route
        IosWidgets.open(target, section)
    }

    private fun post(name: String, json: String) {
        NSNotificationCenter.defaultCenter.postNotificationName(name, json)
    }
}
