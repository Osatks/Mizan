@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.widget

import com.osatks.mizan.platform.toNSData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.Foundation.writeToURL

/**
 * جسر أدوات iOS (WidgetKit): لا تشغّل الأداة كود Kotlin، فيكتب التطبيق لقطة JSON محسوبة مسبقاً
 * (مواقيت أسبوع وتاريخه وآية كل يوم ونوافذ الأذكار بنصوصها) في حاوية App Group المشتركة مع الأداة،
 * ثم يُرسل إشعار MizanWidgetsReload فيطلب iOSApp.swift من WidgetCenter إعادة رسم الأدوات.
 * يستدعيه Swift: IosWidgets.shared.refresh() عند الخروج من التطبيق، و open(route:section:) لروابط الأداة.
 */
object IosWidgets {
    const val DEFAULT_GROUP = "group.com.mizan.almuslim"
    const val FILE = "widget.json"
    const val RELOAD = "MizanWidgetsReload"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    /**
     * معرّفات المجموعة المحتملة بالترتيب: AltStore يعيد تسمية المجموعات عند التوقيع بحساب مجاني
     * ويكتب أسماءها الجديدة في ALTAppGroups داخل Info.plist، ثم MizanAppGroup من Info.plist، ثم الاسم الأصلي.
     */
    fun groups(): List<String> {
        val b = NSBundle.mainBundle
        val alt = (b.objectForInfoDictionaryKey("ALTAppGroups") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val own = b.objectForInfoDictionaryKey("MizanAppGroup") as? String
        return (alt.filter { "mizan" in it.lowercase() } + alt + listOfNotNull(own) + DEFAULT_GROUP).distinct()
    }

    /** أول حاوية يسمح بها توقيع التطبيق؛ null إن لم تكن للتطبيق مجموعة (تثبيت بلا App Group) */
    fun container(): NSURL? {
        val fm = NSFileManager.defaultManager
        for (g in groups()) fm.containerURLForSecurityApplicationGroupIdentifier(g)?.let { return it }
        return null
    }

    /** يكتب اللقطة في الخلفية ثم يطلب إعادة رسم الأدوات */
    fun refresh() {
        job?.cancel()
        job = scope.launch {
            val dir = container() ?: return@launch
            val json = runCatching { WidgetData.snapshotJson() }.getOrNull() ?: return@launch
            val url = dir.URLByAppendingPathComponent(FILE) ?: return@launch
            if (json.encodeToByteArray().toNSData().writeToURL(url, true)) {
                NSNotificationCenter.defaultCenter.postNotificationName(RELOAD, null)
            }
        }
    }

    /** رابط من الأداة (mizan://open?route=…&section=…): يفتح الشاشة نفسها في التطبيق */
    fun open(route: String, section: String?) {
        if (!section.isNullOrEmpty()) WidgetLaunch.adhkarSection = section
        WidgetLaunch.route = route
    }
}
