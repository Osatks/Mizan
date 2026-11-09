package com.osatks.mizan

import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.ReviewPrompt
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.IosStatusBar
import com.osatks.mizan.widget.IosWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.UIKit.UIViewController

/**
 * نقطة دخول iOS: يستدعيها iOSApp.swift. التنقل من UIKit نفسه (MizanNavigation.swift): UITabBarController فيه
 * UINavigationController لكل تبويب، وكل صفحة متحكم خاص بها يعرض شاشتها بـ Compose (IosNav.kt).
 */
fun MainViewController(): UIViewController {
    AppSettings.init()
    // يفتح شاشة عند التشغيل بوسيط «-mizan.open_route <المسار>» (لقطات المتجر من المحاكي، fastlane/screenshots.sh):
    // بلا رابط mizan:// يسأل النظام قبل فتحه. وسائط التشغيل لا تُحفظ في الجهاز. يُفتح بعد أن تستقر الواجهة
    platform.Foundation.NSUserDefaults.standardUserDefaults.stringForKey("mizan.open_route")
        ?.takeIf { it.isNotBlank() }?.let { route -> MainScope().launch { delay(2500); IosWidgets.open(route, null) } }
    // تسجيل حركة التنقل في المحاكي (fastlane/screenshots.sh، motion): خطوات تنقل بلا لمس بعد أن تستقر الواجهة
    platform.Foundation.NSUserDefaults.standardUserDefaults.stringForKey("mizan.motion_script")
        ?.takeIf { it.isNotBlank() }?.let { script -> MainScope().launch { MotionScript.run(script) } }
    // الإشعارات المجدولة لأيام قادمة تُجدَّد عند كل فتح (لا تعمل منبّهات في الخلفية في iOS)،
    // ولقطة أدوات الشاشة الرئيسية في حاوية App Group (وتُحدَّث أيضاً عند الخروج منه)؛
    // بعد أن تظهر الواجهة: حساب أسبوع من المواقيت وطلبات الإشعار لا ينتظرها أول إطار
    MainScope().launch { delay(1200); PrayerAlerts.reschedule(); IosWidgets.refresh() }
    // طلب التقييم بلطف بعد استعمال حقيقي (IosNav.shown)
    ReviewPrompt.onLaunch()
    // الأذكار من أكثر ما يُفتح من الرئيسية: تُقرأ أقسامها مسبقاً، فتظهر كاملة من أول فتح
    MainScope().launch {
        delay(3000)
        withContext(Dispatchers.IO) { runCatching { com.osatks.mizan.adhkar.Adhkar.sections() } }
    }
    // المتحكم الجذر من Swift (MizanNavigation.install)؛ احتياطاً إن لم يُسجَّل: الرئيسية وحدها
    return IosNav.root() ?: IosNav.page("home", tab = true).also { IosStatusBar.controller = it }
}
