@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.widget

import com.osatks.mizan.platform.toByteArray
import com.osatks.mizan.platform.toNSData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
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
        // المجموعات التي منحها ملف التوقيع فعلاً (embedded.mobileprovision): أصدق مصدر، فبعض أدوات التثبيت
        // (إصدارات من AltStore و SideStore و Sideloadly) تعيد تسمية المجموعة دون أن تكتب ALTAppGroups
        val granted = b.pathForResource("embedded", "mobileprovision")?.let { NSData.dataWithContentsOfFile(it)?.toByteArray() }
            ?.let { profileGroups(it.decodeToString()) }.orEmpty()
        val alt = (b.objectForInfoDictionaryKey("ALTAppGroups") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val own = b.objectForInfoDictionaryKey("MizanAppGroup") as? String
        val all = granted + alt + listOfNotNull(own) + DEFAULT_GROUP
        return (all.filter { "mizan" in it.lowercase() } + all).distinct()
    }

    /** أسماء App Group في نص ملف التوقيع (plist داخل توقيع CMS) */
    internal fun profileGroups(text: String): List<String> {
        val m = Regex("<key>com\\.apple\\.security\\.application-groups</key>\\s*<array>([\\s\\S]*?)</array>").find(text) ?: return emptyList()
        return Regex("<string>([^<]+)</string>").findAll(m.groupValues[1]).map { it.groupValues[1].trim() }.filter { it.startsWith("group.") }.toList()
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
            val json = runCatching { WidgetData.snapshotJson() }.getOrNull() ?: return@launch
            container()?.URLByAppendingPathComponent(FILE)?.let { json.encodeToByteArray().toNSData().writeToURL(it, true) }
            // اللقطة مع الإشعار: يكتبها Swift أيضاً في سلسلة المفاتيح المشتركة (لتثبيتات بلا App Group)
            // ثم يطلب إعادة رسم الأدوات. يُرسل على الخيط الرئيسي لأن المراقب هناك.
            dispatch_async(dispatch_get_main_queue()) { NSNotificationCenter.defaultCenter.postNotificationName(RELOAD, json) }
        }
    }

    /** رابط من الأداة (mizan://open?route=…&section=…): يفتح الشاشة نفسها في التطبيق */
    fun open(route: String, section: String?) {
        if (!section.isNullOrEmpty()) WidgetLaunch.adhkarSection = section
        com.osatks.mizan.IosNav.openLink(route)
    }
}
