package com.osatks.mizan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeUIViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.ReviewPrompt
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.requestStoreReview
import com.osatks.mizan.ui.BarRequests
import com.osatks.mizan.ui.InnerBack
import com.osatks.mizan.ui.IosHaptics
import com.osatks.mizan.ui.IosStatusBar
import com.osatks.mizan.ui.LanguageHost
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.LocalBarRequests
import com.osatks.mizan.ui.LocalInnerBack
import com.osatks.mizan.ui.LocalLanguageVeil
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.NavMotion
import com.osatks.mizan.ui.NavTransitions
import com.osatks.mizan.ui.TABS
import com.osatks.mizan.ui.WindowLayout
import com.osatks.mizan.ui.screens.IosOnboarding
import com.osatks.mizan.ui.tabBarStateJson
import com.osatks.mizan.ui.theme.Accent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.MizanTheme
import com.osatks.mizan.ui.theme.schemeFor
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotificationCenter
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIScreen
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewAutoresizingFlexibleTopMargin
import platform.UIKit.UIViewAutoresizingFlexibleLeftMargin
import platform.UIKit.UIViewAutoresizingFlexibleRightMargin
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * ما ينفّذه منسّق UIKit في Swift (MizanNavigation.swift): UITabBarController فيه UINavigationController لكل تبويب،
 * وكل صفحة متحكم خاص بها ([IosNav.page]). العمليات تُؤجَّل هناك إن جاءت أثناء انتقال جارٍ.
 */
interface IosNavigator {
    /** يفتح صفحة فوق الظاهرة في التبويب الحالي (لا يفتحها مرتين إن كانت في الأعلى) */
    fun push(route: String)

    /** يرجع صفحة واحدة */
    fun pop()

    /** ينتقل إلى تبويب ويرجع إلى أوله؛ [closeCurrent]: يُغلق أيضاً ما فوق التبويب الذي كان ظاهراً */
    fun selectTab(route: String, closeCurrent: Boolean)

    /** يُغلق كل ما فوق أول التبويب الحالي بلا حركة */
    fun popToRoot()

    /** يفتح [route] مكان الصفحة العليا (نتيجة من البحث) */
    fun replaceTop(route: String)

    /** مسار الصفحة الظاهرة في أعلى التبويب الحالي */
    fun topRoute(): String?

    /** يخفي جولة أول تشغيل ([IosNav.guide]) */
    fun closeGuide()
}

/** موضع لمس بنقاط النافذة */
class TouchPoint(val x: Double, val y: Double)

/**
 * جسر التنقل في iOS بين Kotlin ومنسّق UIKit: يصنع متحكم كل صفحة، ويحمل التنقل كما تراه الشاشات ([IosAppNav])
 * إلى [IosNavigator]، ويعرف موضع آخر لمس (تنمو منه الصفحة التالية)، وحالة شريط التبويبات (أسماء ولغة ومظهر).
 */
object IosNav {
    /** إشعار حالة شريط التبويبات (JSON في object، tabBarStateJson) */
    const val TAB_STATE = "MizanTabBarChanged"

    private var makeRoot: (() -> UIViewController)? = null
    internal var navigator: IosNavigator? = null
        private set
    private var pendingLink: String? = null

    /** جولة أول تشغيل ظاهرة فوق التبويبات: الصفحات تحتها ليست الظاهرة للمستخدم */
    internal var guideShown by mutableStateOf(false)
        private set

    /** يستدعيه Swift مرة عند التشغيل (قبل MainViewController): ينشئ المتحكم الجذر عند الطلب */
    fun install(makeRoot: () -> UIViewController) {
        this.makeRoot = makeRoot
    }

    internal fun root(): UIViewController? = makeRoot?.invoke()

    /** يسجّله المتحكم الجذر حين يُبنى: منه تُعرض الأوراق والتنبيهات، وإليه يذهب التنقل */
    @OptIn(ExperimentalForeignApi::class)
    fun attach(navigator: IosNavigator, root: UIViewController) {
        this.navigator = navigator
        IosStatusBar.controller = root
        MotionScript.open = { IosAppNav.open(it) }
        MotionScript.back = { IosAppNav.back() }
        MotionScript.tab = { IosAppNav.goTab(it) }
        MotionScript.touch = { fx, fy -> UIScreen.mainScreen.bounds.useContents { touched(fx * size.width, fy * size.height) } }
        pendingLink?.let { link ->
            pendingLink = null
            dispatch_async(dispatch_get_main_queue()) { openLink(link) }
        }
    }

    /** متحكم صفحة لمسار ([tab]: أول تبويب، تحته شريط التبويبات) */
    fun page(route: String, tab: Boolean): UIViewController = PageController(route, tab) { PageContent(route, IosAppNav) }

    /** مسارات التبويبات بترتيبها */
    fun tabRoutes(): List<String> = TABS.map { it.route }

    fun routeOf(page: UIViewController?): String? = (page as? PageController)?.route

    /** في الصفحة رجوع داخلي (صفحة فرعية، وضع التركيز): سحب الحافة له لا للرجوع من الصفحة */
    fun hasInnerBack(page: UIViewController?): Boolean = (page as? PageController)?.state?.inner?.active == true

    fun innerBack(page: UIViewController?) {
        (page as? PageController)?.state?.inner?.back()
    }

    /** جولة أول تشغيل (أو صفحات الدليل الجديدة بعد التحديث) إن حان وقتها، فوق كل شيء؛ null إن رآها المستخدم */
    fun guide(): UIViewController? {
        val from = pendingGuide() ?: return null
        guideShown = true
        return PageController("guide", false, overlay = true) {
            IosOnboarding(onDone = {
                guideDone()
                guideShown = false
                navigator?.closeGuide()
            }, from = from)
        }
    }

    // ---------- اللمس ----------

    private var touchX = 0.0
    private var touchY = 0.0
    private var touchAt = -10.0

    /** كل لمس يبدأ في النافذة (TouchWindow في Swift) بنقاطها */
    fun touched(x: Double, y: Double) {
        touchX = x
        touchY = y
        touchAt = CACurrentMediaTime()
    }

    /** آخر لمس قبل قليل (بطاقة، سطر)؛ null إن لم يكن (رابط أو اختصار: تنمو الصفحة من المنتصف) */
    fun recentTouch(): TouchPoint? = if (CACurrentMediaTime() - touchAt < 1.5) TouchPoint(touchX, touchY) else null

    // ---------- شريط التبويبات ----------

    private var tabState = ""

    /** آخر حالة لشريط التبويبات؛ قبل أن ترسلها صفحة: من الإعدادات نفسها */
    fun tabState(): String = tabState.ifEmpty { initialTabState() }

    /** يُرسل الحالة إن تغيرت (من الصفحة الظاهرة، على الخيط الرئيسي) */
    internal fun publishTabState(json: String) {
        if (json == tabState) return
        tabState = json
        NSNotificationCenter.defaultCenter.postNotificationName(TAB_STATE, json)
    }

    private fun initialTabState(): String {
        val lang = Lang.resolve(AppSettings.lang, Platform.systemLanguage())
        val dark = when (AppSettings.theme) {
            "dark" -> true
            "light" -> false
            else -> UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        return tabBarStateJson(lang.strings, null, true, lang.rtl, dark, schemeFor(Accent.of(AppSettings.accent), dark).primary)
    }

    // ---------- الروابط والتقييم ----------

    /** رابط من أداة أو اختصار أو Spotlight أو وسيط تشغيل: يُغلق ما فوق التبويب ثم يفتح الشاشة */
    internal fun openLink(route: String) {
        if (!isRoute(route)) return
        if (navigator == null) {
            pendingLink = route
            return
        }
        IosAppNav.deepLink(route)
    }

    private var lastRoute: String? = null

    /** ظهرت صفحة: طلب التقييم بلطف عند العودة إلى الرئيسية من أداة (ReviewPrompt)، مرة لكل إصدار */
    internal fun shown(route: String) {
        val from = lastRoute
        lastRoute = route
        if (route == "home" && from != null && from != "home" && ReviewPrompt.due()) {
            MainScope().launch {
                delay(1200)
                ReviewPrompt.markAsked()
                requestStoreReview()
            }
        }
    }
}

/** التنقل كما تراه الشاشات في iOS: كل عملية إلى منسّق UIKit */
internal object IosAppNav : AppNav {
    private val nav: IosNavigator? get() = IosNav.navigator

    // فتح تبويب من داخل شاشة يمرّ بالتبويبات نفسها؛ ونقرتان سريعتان لا تفتحان الصفحة مرتين
    override fun open(route: String) {
        if (route in TAB_ROUTES) return goTab(route)
        val n = nav ?: return
        if (n.topRoute() != route) n.push(route)
    }

    override fun back() {
        nav?.pop()
    }

    override fun goTab(route: String) {
        nav?.selectTab(route, false)
    }

    override fun deepLink(route: String) {
        val n = nav ?: return
        if (route in TAB_ROUTES) n.selectTab(route, true)
        else {
            n.popToRoot()
            n.push(route)
        }
    }

    // نقرة ثانية على نتيجة أثناء خروج البحث لا تفتح الشاشة مرتين
    override fun searchResult(route: String) {
        val n = nav ?: return
        if (n.topRoute() != "search") return
        if (route in TAB_ROUTES) n.selectTab(route, true) else n.replaceTop(route)
    }
}

/**
 * حال صفحة واحدة كما تراها شاشتها: طلبات شريط الحالة، والرجوع الداخلي، وظهورها. كائن Kotlin مستقل يمسك المتحكم
 * بمرجع ضعيف: التركيب (تُتلفه Compose حين يُتلف متحكمها) لا يمسك المتحكم، فلا تبقى الصفحة بعد الرجوع منها
 */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
internal class PageState(val route: String, private val overlay: Boolean, private val content: @Composable () -> Unit) {
    val bars = BarRequests()
    val inner = InnerBack()

    /** الصفحة ظاهرة (بين ظهورها واختفائها في UIKit) */
    var active by mutableStateOf(false)
        internal set

    /** الصفحة التي يراها المستخدم الآن (لا تحت جولة أول تشغيل) */
    val front: Boolean get() = active && (overlay || !IosNav.guideShown)

    internal var controller: kotlin.native.ref.WeakReference<PageController>? = null

    @Composable
    internal fun Content() = content()
}

/**
 * متحكم صفحة واحدة: واجهة Compose صغيرة لا تعرض إلا شاشة مسارها (وفيها واجهة النظام المضمّنة)، داخل ما يحيط بالتطبيق
 * كله (الاهتزازات، اللغة، المظهر، مقاس النافذة). يجيب النظام عن شريط الحالة من طلبات شاشته، ويملك الرجوع الداخلي فيها،
 * وحركة صفحاتها الفرعية ([NavMotion]).
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal class PageController(
    val route: String,
    val tab: Boolean,
    /** جولة أول تشغيل فوق التبويبات، لا صفحة في التنقل */
    private val overlay: Boolean = false,
    content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {
    val state = PageState(route, overlay, content)

    private var light = false
    private var barHidden = false
    private var motion: NavMotion? = null
    private var bg: Color? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        val page = state
        page.controller = kotlin.native.ref.WeakReference(this)
        val compose = ComposeUIViewController(configure = {
            // الرسم على الخيط الرئيسي (الرسم المتوازي التجريبي كان يُسقط التطبيق، تقرير TestFlight البناء 209)
            parallelRendering = false
            // سحب الحافة من UIKit وحده (الرجوع من الصفحة، والرجوع الداخلي عبر InnerBack)
            enableBackGesture = false
        }) { PageRoot(page) }
        addChildViewController(compose)
        compose.view.setFrame(view.bounds)
        compose.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(compose.view)
        compose.didMoveToParentViewController(this)
        motion = NavMotion(view).also { it.content = compose.view }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        state.active = true
        NavTransitions.current = motion
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        // بعد رجوع أُلغي في منتصفه تعود الصفحة الظاهرة إلى الأعلى
        state.active = true
        NavTransitions.current = motion
        if (!overlay) IosNav.shown(route)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        state.active = false
        if (NavTransitions.current === motion) NavTransitions.current = null
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = if (light) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent
    override fun prefersStatusBarHidden(): Boolean = barHidden
    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = UIStatusBarAnimation.UIStatusBarAnimationFade

    /** مظهر شريط الحالة كما تطلبه الشاشة، ولون الخلفية (يظهر أثناء الانتقالات قبل أول إطار من Compose) */
    internal fun apply(light: Boolean, hidden: Boolean, background: Color) {
        if (background != bg) {
            bg = background
            view.backgroundColor = UIColor(
                red = background.red.toDouble(), green = background.green.toDouble(), blue = background.blue.toDouble(), alpha = 1.0,
            )
        }
        if (light == this.light && hidden == barHidden) return
        this.light = light
        barHidden = hidden
        UIView.animateWithDuration(0.25) {
            setNeedsStatusBarAppearanceUpdate()
            IosStatusBar.controller?.setNeedsStatusBarAppearanceUpdate()
        }
    }
}

/** ما يحيط بشاشة كل صفحة في iOS: ما كان MizanApp يوفّره للتطبيق كله */
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@Composable
private fun PageRoot(page: PageState) {
    CompositionLocalProvider(LocalHapticFeedback provides IosHaptics) {
        LanguageHost(animate = { page.front }) {
            MizanTheme {
                val cs = MaterialTheme.colorScheme
                val dark = LocalDark.current
                // محتوى الصفحة مصدرٌ للزجاج (الأوراق السفلية)، إلا المصحف: لا كلفة إضافية على التقليب
                val haze = rememberHazeState()
                BoxWithConstraints(Modifier.fillMaxSize().background(cs.background)) {
                    val layout = remember(maxWidth, maxHeight) { WindowLayout(maxWidth, maxHeight) }
                    // شريط التبويبات من النظام داخل المنطقة الآمنة السفلية نفسها: لا مسافة إضافية له (LocalBottomInset = 0)،
                    // ولا قائمة جانبية في iOS (LocalLeadingInset = 0)
                    CompositionLocalProvider(
                        LocalWindowLayout provides layout,
                        LocalAppHaze provides haze,
                        LocalContentColor provides cs.onBackground,
                        LocalPageActive provides page.active,
                        LocalBarRequests provides page.bars,
                        LocalInnerBack provides page.inner,
                    ) {
                        val glass = !page.route.startsWith("surah/")
                        Box(Modifier.fillMaxSize().then(if (glass) Modifier.hazeSource(haze) else Modifier)) { page.Content() }
                    }
                }
                val light = page.bars.lightContent ?: dark
                val hidden = page.bars.hidden
                val background = cs.background
                SideEffect { page.controller?.get()?.apply(light, hidden, background) }
                TabBarState(page)
            }
        }
    }
}

/** حالة شريط التبويبات من الصفحة الظاهرة: الأسماء بلغة التطبيق، واتجاهها، والمظهر واللون، ويختفي مع ستار اللغة */
@Composable
private fun TabBarState(page: PageState) {
    val json = tabBarStateJson(
        LocalStrings.current, null, !LocalLanguageVeil.current,
        LocalLayoutDirection.current == LayoutDirection.Rtl, LocalDark.current, MaterialTheme.colorScheme.primary,
    )
    SideEffect { if (page.front) IosNav.publishTabState(json) }
}

/** رسالة قصيرة أسفل الشاشة فوق كل شيء تختفي وحدها (لا Toast في iOS) */
@OptIn(ExperimentalForeignApi::class)
internal object IosToastView {
    private var shown: UIView? = null

    fun show(msg: String) = dispatch_async(dispatch_get_main_queue()) {
        val host = IosStatusBar.controller?.view ?: return@dispatch_async
        shown?.removeFromSuperview()
        val (w, h) = host.bounds.useContents { size.width to size.height }
        val bottom = host.safeAreaInsets.useContents { this.bottom }
        val label = UILabel(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
        label.text = msg
        label.textColor = UIColor.whiteColor
        label.font = UIFont.systemFontOfSize(15.0)
        label.numberOfLines = 0
        label.textAlignment = NSTextAlignmentCenter
        val max = (w - 48.0 - 36.0).coerceAtLeast(60.0)
        val (lw, lh) = label.sizeThatFits(platform.CoreGraphics.CGSizeMake(max, 400.0)).useContents { minOf(width, max) to height }
        val bw = lw + 36.0
        val bh = lh + 24.0
        val box = UIView(frame = CGRectMake((w - bw) / 2, h - bottom - 110.0 - bh, bw, bh))
        box.backgroundColor = UIColor(red = 0x22 / 255.0, green = 0x22 / 255.0, blue = 0x22 / 255.0, alpha = 0.9)
        box.layer.cornerRadius = 18.0
        box.userInteractionEnabled = false
        box.autoresizingMask = UIViewAutoresizingFlexibleTopMargin or UIViewAutoresizingFlexibleLeftMargin or UIViewAutoresizingFlexibleRightMargin
        label.setFrame(CGRectMake(18.0, 12.0, lw, lh))
        box.addSubview(label)
        box.alpha = 0.0
        host.addSubview(box)
        shown = box
        UIView.animateWithDuration(0.2) { box.alpha = 1.0 }
        MainScope().launch {
            delay(2600)
            UIView.animateWithDuration(0.25, animations = { box.alpha = 0.0 }, completion = { _ ->
                box.removeFromSuperview()
                if (shown === box) shown = null
            })
        }
    }
}
