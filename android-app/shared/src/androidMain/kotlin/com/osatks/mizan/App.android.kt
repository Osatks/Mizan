package com.osatks.mizan

import androidx.compose.ui.input.pointer.pointerInput
import com.osatks.mizan.ui.PageMotion
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.ui.LocalTabBarHost
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.WindowLayout
import com.osatks.mizan.ui.LocalAppHaze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalNavScope
import com.osatks.mizan.ui.LocalSharedScope
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.quran.SURAH_ROUTE


// التطبيق كله في أندرويد: التنقل بين الشاشات (NavHost)، وشريط التبويبات، وجولة أول تشغيل. يستضيفه MainActivity

private class AndroidNav(val nav: NavHostController) : AppNav {
    private val top get() = nav.currentBackStackEntry?.destination?.route

    // فتح تبويب من داخل شاشة (مثل «القرآن» من الرئيسية) يمرّ بالتبويبات نفسها، لا يُكدَّس فوق الرئيسية.
    // نقرتان سريعتان على أداة لا تفتحانها مرتين: المكدّس يتغير مع النقرة الأولى فوراً، فالثانية تجدها في أعلاه
    override fun open(route: String) {
        if (route in TAB_ROUTES) goTab(route) else if (top != route) nav.navigate(route)
    }

    // الرجوع لا يُخرج الرئيسية نفسها (مكدّس فارغ = شاشة بيضاء)
    override fun back() {
        if (nav.previousBackStackEntry != null) nav.popBackStack()
    }

    override fun goTab(route: String) = nav.goTab(route)

    // ما فوق الرئيسية يُغلق دون حفظ: لا تبقى الشاشة السابقة مخبّأة في تبويب يعود بها لاحقاً
    override fun deepLink(route: String) {
        nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false)
        if (route in TAB_ROUTES) goTab(route) else nav.navigate(route)
    }

    override fun searchResult(route: String) {
        // نقرة ثانية على نتيجة أثناء خروج البحث كانت تفتح الشاشة مرتين
        if (top != "search") return
        if (route in TAB_ROUTES) { nav.popBackStack(); goTab(route) }
        else nav.navigate(route) { popUpTo("search") { inclusive = true } }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MizanApp(startRoute: String? = null) {
    val navController = rememberNavController()
    val app: AppNav = remember { AndroidNav(navController) }
    SideEffect {
        MotionScript.open = { app.open(it) }
        MotionScript.back = { app.back() }
        MotionScript.tab = { app.goTab(it) }
    }
    val current: String? = navController.currentBackStackEntryAsState().value?.destination?.route
    val onTab = current == null || current in TAB_ROUTES
    LaunchedEffect(startRoute) {
        if (startRoute != null && startRoute != "home" && isRoute(startRoute)) app.deepLink(startRoute)
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val back: () -> Unit = remember(app) { { app.back() } }
    val open: (String) -> Unit = remember(app) { { r -> app.open(r) } }
    // محتوى الصفحات مصدرٌ للزجاج (شريط التنقل والأوراق السفلية)، إلا صفحة المصحف: لا شريط فوقها، فلا كلفة إضافية على التقليب
    val appHaze = rememberHazeState()
    val glassSource = current?.startsWith("surah/") != true

    // شريط الحالة كما تطلبه الشاشة الظاهرة
    com.osatks.mizan.ui.ApplySystemBars()
    // طلب التقييم بلطف: بعد استعمال حقيقي (ReviewPrompt)، عند العودة إلى الرئيسية من أداة، مرة لكل إصدار
    val requestReview = com.osatks.mizan.platform.rememberReviewRequest()
    LaunchedEffect(Unit) { com.osatks.mizan.core.ReviewPrompt.onLaunch() }
    var lastRoute by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(current) {
        val from = lastRoute
        lastRoute = current
        if (current == "home" && from != null && from != "home" && com.osatks.mizan.core.ReviewPrompt.due()) {
            kotlinx.coroutines.delay(1200)
            com.osatks.mizan.core.ReviewPrompt.markAsked()
            requestReview()
        }
    }

    // مقاس النافذة كما هو متاح للتطبيق (يتغير بتدوير iPad و Split View وفتح iPhone Duo وطيّه)
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val layout = remember(maxWidth, maxHeight) { WindowLayout(maxWidth, maxHeight) }
        // على الشاشات الواسعة (وبالعرض في المتوسطة) قائمة تنقل جانبية بدل الشريط السفلي
        val rail = layout.rail
        val bars = LocalTabBarHost.current
        CompositionLocalProvider(LocalWindowLayout provides layout, LocalAppHaze provides appHaze) {
            // لون النص الافتراضي يتبع الخلفية، وإلا ظهر نص بلا لون محدد أسود في الوضع الداكن
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                val source = Modifier.fillMaxSize().then(if (glassSource) Modifier.hazeSource(appHaze) else Modifier)
                SharedTransitionLayout(source) {
                    CompositionLocalProvider(LocalSharedScope provides this) {
                        AndroidNavHost(navController, app, open, back, rtl)
                    }
                }
            }
        }

        // جولة أول تشغيل فوق كل شيء، مرة واحدة
        var firstRun by remember { mutableStateOf(!AppSettings.getBool("onboarded", false) && startRoute == null) }
        // بعد التحديث: من مرّ بالجولة من قبل يرى صفحات الدليل الجديدة وحدها، مرة واحدة
        val guideFrom = remember { if (AppSettings.getBool("onboarded", false) && AppSettings.getInt("guideSeen", 0) < GUIDE_VERSION) BASE_GUIDE_PAGES else 0 }
        LaunchedEffect(Unit) { if (guideFrom > 0 && startRoute == null) firstRun = true }
        AnimatedVisibility(firstRun, enter = fadeIn(), exit = fadeOut(tween(Motion.MEDIUM))) {
            // لا يصل لمس الأماكن الفارغة من الجولة إلى الرئيسية تحتها
            Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }) {
                com.osatks.mizan.ui.screens.Onboarding(
                    onDone = { firstRun = false; AppSettings.putBool("onboarded", true); AppSettings.putInt("guideSeen", GUIDE_VERSION) },
                    from = guideFrom,
                )
            }
        }
        // الشريط لا يظهر فوق جولة أول تشغيل (كان يغطي أزرارها السفلية)، ولا مع القائمة الجانبية
        bars.Bottom(onTab && !firstRun && !rail, current, Modifier.align(Alignment.BottomCenter)) { app.goTab(it) }
        // القائمة الجانبية تدخل من جهة البداية (اليمين في العربية) وتخرج إليها
        AnimatedVisibility(
            visible = onTab && !firstRun && rail,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { if (rtl) it else -it } + fadeIn(),
            exit = slideOutHorizontally(tween(Motion.SHORT, easing = Motion.Accelerate)) { if (rtl) it else -it } + fadeOut(),
        ) {
            // الأسماء تحت الأيقونات ما دام الارتفاع يتسع لها (لا في هاتف بالعرض)
            bars.Rail(current, labels = maxHeight >= 440.dp) { app.goTab(it) }
        }
    }
}

/** مكان شريط التنقل لكل صفحة تبويب بحسبها هي، لا بحسب الصفحة الظاهرة: فلا يقفز محتواها أثناء الانتقال منها وإليها */
@Composable
private fun PageFrame(route: String, tab: Boolean, content: @Composable () -> Unit) {
    val side = LocalWindowLayout.current.rail
    val host = LocalTabBarHost.current
    CompositionLocalProvider(
        LocalBottomInset provides if (tab && !side) host.bottomInset else 0.dp,
        LocalLeadingInset provides if (tab && side) host.leadingInset else 0.dp,
    ) { content() }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AndroidNavHost(nav: NavHostController, app: AppNav, open: (String) -> Unit, back: () -> Unit, rtl: Boolean) {
    NavHost(
        nav, startDestination = "home", modifier = Modifier.fillMaxSize(),
        enterTransition = { enter(rtl) },
        exitTransition = { exit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit(rtl) },
    ) {
        for (route in ROUTES) {
            composable(route) {
                CompositionLocalProvider(LocalNavScope provides this) {
                    PageFrame(route, route in TAB_ROUTES) { Route(route, app, open, back) }
                }
            }
        }
        composable(
            SURAH_ROUTE,
            arguments = listOf(
                navArgument("surah") { type = NavType.IntType },
                navArgument("ayah") { type = NavType.IntType; defaultValue = 0 },
                navArgument("from") { type = NavType.StringType; defaultValue = "row" },
            )
        ) { e ->
            val args = e.arguments
            CompositionLocalProvider(LocalNavScope provides this) {
                SurahRoute(
                    args?.read { getIntOrNull("surah") } ?: 1, args?.read { getIntOrNull("ayah") } ?: 0,
                    args?.read { getStringOrNull("from") } ?: "row", open, back,
                )
            }
        }
    }
}

// ---------- الانتقالات (أندرويد) ----------
// بين التبويبات: "تلاشٍ عابر" (يخبو القديم بسرعة ويكبر الجديد بلطف).
// إلى أداة: تدخل الصفحة من جهة القراءة (اليمين في العربية) بينما تنكمش السابقة للخلف قليلاً.

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    initialState.destination.route in TAB_ROUTES && targetState.destination.route in TAB_ROUTES

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(rtl: Boolean): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(Motion.MEDIUM, delayMillis = 70)) + scaleIn(tween(Motion.LONG, easing = Motion.Decelerate), initialScale = 0.94f)
    else PageMotion.push(rtl)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(90))
    else scaleOut(tween(Motion.LONG, easing = Motion.Emphasized), targetScale = 0.94f) + fadeOut(tween(Motion.MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(Motion.MEDIUM, delayMillis = 70)) + scaleIn(tween(Motion.LONG, easing = Motion.Decelerate), initialScale = 0.94f)
    else scaleIn(tween(Motion.LONG, easing = Motion.Emphasized), initialScale = 0.94f) + fadeIn(tween(Motion.MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(rtl: Boolean): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(90))
    else PageMotion.pop(rtl)

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
