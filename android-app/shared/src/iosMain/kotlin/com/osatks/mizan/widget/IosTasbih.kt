@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.widget

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.LiveTasbih
import com.osatks.mizan.platform.toNSData
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL

/**
 * جسر المسبحة في iOS: زر الإجراء ومركز التحكم وأداة الشاشة الرئيسية وأزرار النشاط الحي و Siri تعدّ في [Tasbih]
 * نفسها التي تعدّ فيها شاشة المسبحة. النوايا (Swift: Shared/LiveIntents.swift) تعمل في عملية التطبيق
 * (LiveActivityIntent) فتستدعي [count] و [reset] مباشرة وتأخذ الحال JSON، فتبدأ النشاط الحي أو تحدّثه.
 * والعدّ من الشاشة يصل إلى Swift بإشعار [STATE] فيُحدَّث النشاط الحي إن كان ظاهراً.
 * الحال مكتوبة أيضاً في حاوية App Group ([FILE]) لأداة الشاشة الرئيسية وزر مركز التحكم.
 */
object IosTasbih {
    /** Kotlin → Swift: الحال بعد عدّ من شاشة المسبحة (JSON رسالة) */
    const val STATE = "MizanTasbihState"
    /** Kotlin → Swift: زر «على شاشة القفل» في شاشة المسبحة */
    const val SHOW = "MizanTasbihShow"
    const val FILE = "tasbih.json"
    /** تسبيحات عُدّت في امتداد الأدوات حين تعذّر تشغيل التطبيق (احتياط)، تُضاف عند أول فرصة */
    const val PENDING = "tasbih.pending"

    private var started = false

    /** أزرار الأنشطة الحية (نوايا تعمل بلا فتح التطبيق) في iOS 17 فما بعد: قبله لا يُعرض النشاط */
    private val ios17: Boolean by lazy {
        platform.UIKit.UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull()?.let { it >= 17 } ?: false
    }

    /** عند الإقلاع (IosLive.start) */
    fun start(supported: Boolean) {
        LiveTasbih.supported = supported && ios17
        if (started) return
        started = true
        AppSettings.init()
        Tasbih.ensure()
        Tasbih.onChange = { publish(null) }
        LiveTasbih.show = { post(SHOW, message(null)) }
        // مفتاح «على شاشة القفل»: مطفأً يُنهي Swift الأنشطة الظاهرة (LiveActivities.swift)
        LiveTasbih.onChange = { on -> post("MizanLiveCounters", if (on) "on" else "off") }
        applyPending()
        write()
    }

    fun availability(supported: Boolean) { LiveTasbih.supported = supported && ios17 }

    /** تسبيحة واحدة (زر الإجراء، مركز التحكم، الأداة، زر النشاط الحي، Siri) */
    fun count(): String = quiet {
        applyPending()
        message(Tasbih.count())
    }

    /** تصفير العدد الحالي (زر النشاط الحي) */
    fun reset(): String = quiet {
        applyPending()
        Tasbih.reset()
        message(null)
    }

    /** الحال الآن بلا تغيير */
    fun current(): String = quiet {
        applyPending()
        message(null)
    }

    private inline fun quiet(block: () -> String): String {
        AppSettings.init()
        Tasbih.ensure()
        Tasbih.muted = true
        try {
            val m = block()
            write()
            return m
        } finally {
            Tasbih.muted = false
        }
    }

    /** العدّ من شاشة المسبحة: الحال إلى Swift (يحدّث النشاط الحي إن كان ظاهراً) وإلى الأداة */
    private fun publish(event: Tasbih.Event?) {
        write()
        post(STATE, message(event))
    }

    /** ما عُدّ في امتداد الأدوات احتياطاً */
    private fun applyPending() {
        val d = groupDefaults() ?: return
        val n = d.integerForKey(PENDING).toInt()
        if (n <= 0) return
        d.setInteger(0, PENDING)
        Tasbih.count(n)
    }

    private fun groupDefaults(): NSUserDefaults? {
        for (g in IosWidgets.groups()) {
            if (platform.Foundation.NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(g) != null) {
                return NSUserDefaults(suiteName = g)
            }
        }
        return null
    }

    /** الحال للنشاط الحي والأداة وزر مركز التحكم (TasbihLiveState في Swift) */
    internal fun state(): Map<String, Any?> =
        LiveStates.tasbih(Tasbih.state, Lang.current, AppSettings.accent, NSDate().timeIntervalSince1970.toLong())

    /** رسالة إلى Swift: الحال، وما حدث مع التسبيحة، ونص التنبيه عند إتمام دورة */
    private fun message(event: Tasbih.Event?): String {
        val s = Lang.current.strings
        val st = Tasbih.state
        val (name, title) = when (event) {
            null -> "none" to ""
            Tasbih.Event.COUNT -> "count" to ""
            Tasbih.Event.NEXT_DHIKR -> "next" to s.nextDhikr
            Tasbih.Event.ROUND -> "round" to s.completedN(st.goal)
            Tasbih.Event.HUNDRED -> "hundred" to s.completedHundred
        }
        return Json.write(mapOf("state" to state(), "event" to name, "alertTitle" to title, "alertBody" to st.text))
    }

    /** الحال في حاوية App Group لأداة الشاشة الرئيسية وزر مركز التحكم */
    private fun write() {
        val json = Json.write(state())
        IosWidgets.container()?.URLByAppendingPathComponent(FILE)?.let { json.encodeToByteArray().toNSData().writeToURL(it, true) }
    }

    private fun post(name: String, json: String) {
        NSNotificationCenter.defaultCenter.postNotificationName(name, json)
    }
}
