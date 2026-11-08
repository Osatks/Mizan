package com.osatks.mizan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import com.osatks.mizan.ui.IosHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.platform.IosToast
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.LanguageHost
import com.osatks.mizan.ui.theme.MizanTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.MainScope
import com.osatks.mizan.widget.IosWidgets
import com.osatks.mizan.widget.WidgetLaunch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import com.osatks.mizan.ui.IosStatusBar
import com.osatks.mizan.ui.ComposeTabBar
import com.osatks.mizan.ui.LocalTabBarHost
import com.osatks.mizan.ui.NativeTabBar
import com.osatks.mizan.ui.NativeTabBarHost
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewController

/** نقطة دخول iOS: يستدعيها iOSApp.swift */
@OptIn(ExperimentalComposeUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    AppSettings.init()
    // يفتح شاشة عند التشغيل بوسيط «-mizan.open_route <المسار>» (لقطات المتجر من المحاكي، fastlane/screenshots.sh):
    // بلا رابط mizan:// يسأل النظام قبل فتحه. وسائط التشغيل لا تُحفظ في الجهاز
    // يُفتح بعد أن تستقر الواجهة، كما يصل لمس أداة إلى تطبيق يعمل (فتحه مع أول إطار كان يُغلق التطبيق)
    platform.Foundation.NSUserDefaults.standardUserDefaults.stringForKey("mizan.open_route")
        ?.takeIf { it.isNotBlank() }?.let { route -> MainScope().launch { delay(2500); IosWidgets.open(route, null) } }
    // الإشعارات المجدولة لأيام قادمة تُجدَّد عند كل فتح (لا تعمل منبّهات في الخلفية في iOS)،
    // ولقطة أدوات الشاشة الرئيسية في حاوية App Group (وتُحدَّث أيضاً عند الخروج منه)؛
    // بعد أن تظهر الواجهة: حساب أسبوع من المواقيت وطلبات الإشعار لا ينتظرها أول إطار
    MainScope().launch { delay(1200); PrayerAlerts.reschedule(); IosWidgets.refresh() }
    val vc = ComposeUIViewController(configure = {
        // الرسم على الخيط الرئيسي نفسه (parallelRendering معطّل): الرسم المتوازي (تجريبي في Compose 1.8) كان يُسقط
        // التطبيق على خيط الرسم الخلفي (تقرير TestFlight، البناء 209: EXC_BREAKPOINT في MetalRedrawer.draw عند فتح المواقيت)
        parallelRendering = false
        // سحب الحافة للرجوع (اليسرى واليمنى): يقود الرجوع في StackNavHost و SubPages، والصفحة تتبع الإصبع من أي حافة بدأ
        enableBackGesture = true
    }) {
        // اهتزازات بمولّدات UIKit لكل نوع ما يناسبه (التبويبات، العدّادات، المفاتيح)؛
        // وفي iOS 26 شريط التبويبات من النظام نفسه (NativeTabBar) بدل شريط Compose السفلي
        CompositionLocalProvider(
            LocalHapticFeedback provides IosHaptics,
            LocalTabBarHost provides if (NativeTabBar.active) NativeTabBarHost else ComposeTabBar,
        ) {
            LanguageHost {
                MizanTheme {
                    Box(Modifier.fillMaxSize()) {
                        // لمس أداة يفتح شاشتها؛ يُمسح الطلب بعد التنقل حتى يعمل اللمس التالي على الشاشة نفسها
                        val route = WidgetLaunch.route
                        MizanApp(route)
                        LaunchedEffect(route) { if (route != null) { delay(400); WidgetLaunch.route = null } }
                        ToastOverlay()
                    }
                }
            }
        }
    }
    // المتحكم الجذر يحمل Compose ويجيب النظام عن شريط الحالة (SystemBars.ios.kt)
    return MizanRootController(vc).also { IosStatusBar.controller = it }
}

/**
 * المتحكم الجذر لنافذة التطبيق: واجهة Compose ابنٌ يملأه، وهو يجيب عن شريط الحالة
 * (نص فاتح فوق الرؤوس الخضراء وداكن فوق السطوح الفاتحة، ومخفي في وضع التركيز)
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private class MizanRootController(private val content: UIViewController) : UIViewController(nibName = null, bundle = null) {
    override fun viewDidLoad() {
        super.viewDidLoad()
        // شريط التبويبات من النظام (iOS 26) يُنشأ قبل أول تركيب لـ Compose، فلا يظهر شريط Compose لحظةً
        val bar = NativeTabBar.makeOverlay()
        embed(content)
        // فوق Compose، وتحت نوافذها المنبثقة وأوراقها (تضيفها Compose إلى هذا المتحكم فوق كل شيء حين تظهر)
        bar?.let { embed(it) }
    }

    private fun embed(child: UIViewController) {
        addChildViewController(child)
        child.view.setFrame(view.bounds)
        child.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(child.view)
        child.didMoveToParentViewController(this)
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = IosStatusBar.style
    override fun prefersStatusBarHidden(): Boolean = IosStatusBar.hidden
    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = UIStatusBarAnimation.UIStatusBarAnimationFade
}

/** بديل Toast: رسالة قصيرة أسفل الشاشة تختفي وحدها */
@androidx.compose.runtime.Composable
private fun androidx.compose.foundation.layout.BoxScope.ToastOverlay() {
    val msg = IosToast.message
    LaunchedEffect(msg) { if (msg != null) { delay(2600); IosToast.message = null } }
    AnimatedVisibility(msg != null, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 110.dp, start = 24.dp, end = 24.dp), enter = fadeIn(), exit = fadeOut()) {
        Text(
            msg ?: "", color = Color.White, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.background(Color(0xE6222222), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
