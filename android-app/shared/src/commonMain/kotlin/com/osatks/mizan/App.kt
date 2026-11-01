package com.osatks.mizan

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
import com.osatks.mizan.core.Demo
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.ui.LocalTabBarHost
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.WindowLayout
import com.osatks.mizan.ui.IsIos
import com.osatks.mizan.ui.LocalAppHaze
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.osatks.mizan.ui.LanguageHost
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalNavScope
import com.osatks.mizan.ui.LocalSharedScope
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.TABS
import com.osatks.mizan.ui.quran.QuranScreen
import com.osatks.mizan.ui.quran.SURAH_ROUTE
import com.osatks.mizan.ui.quran.SurahScreen
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.screens.AgeScreen
import com.osatks.mizan.ui.screens.EventsScreen
import com.osatks.mizan.ui.screens.FidyaScreen
import com.osatks.mizan.ui.screens.FitrScreen
import com.osatks.mizan.ui.screens.HijriScreen
import com.osatks.mizan.ui.screens.HomeScreen
import com.osatks.mizan.ui.screens.InheritanceScreen
import com.osatks.mizan.ui.screens.PrayerScreen
import com.osatks.mizan.ui.screens.QiblaScreen
import com.osatks.mizan.ui.screens.SearchScreen
import com.osatks.mizan.ui.screens.SettingsScreen
import com.osatks.mizan.ui.screens.TasbihScreen
import com.osatks.mizan.ui.screens.ZakatScreen
import com.osatks.mizan.ui.theme.MizanTheme

/**
 * التطبيق كله: التنقل بين الشاشات، وشريط التبويبات، وجولة أول تشغيل.
 * يستضيفه MainActivity في أندرويد و MainViewController في iOS.
 */
internal val ROUTES = setOf("home", "prayer", "qibla", "tasbih", "settings", "zakat", "fitr", "inheritance", "hijri", "age", "events", "fidya", "search", "quran", "tafsirs", "guide", "adhkar", "adhan", "licenses")
private val TAB_ROUTES = TABS.map { it.route }.toSet()

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MizanApp(startRoute: String? = null) {
    val nav = rememberNavController()
    LaunchedEffect(startRoute) {
        if (startRoute != null && startRoute != "home" && (startRoute in ROUTES || startRoute.startsWith("surah/"))) {
            if (startRoute in TAB_ROUTES) nav.goTab(startRoute) else nav.navigate(startRoute)
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val onTab = current == null || current in TAB_ROUTES
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // الرجوع فقط من شاشة مستقرة: ضغطة ثانية (أو سحب الحافة) أثناء انتقال لم يكتمل كانت تُخرج شاشتين
    // وتترك المكدّس في منتصف انتقال، فتبدو الواجهة معلّقة
    val back: () -> Unit = {
        if (nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == true) nav.popBackStack()
    }
    // فتح تبويب من داخل شاشة (مثل «القرآن» من الرئيسية) يمرّ بالتبويبات نفسها، لا يُكدَّس فوق الرئيسية،
    // وإلا حفظ التبويب الرئيسي هذه الشاشة في حالته فبدا شريط التنقل وكأنه لا يعمل
    val open: (String) -> Unit = { r ->
        // نقرتان سريعتان على أداة كانتا تفتحانها مرتين فوق بعض
        val settled = nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) != false
        if (r in TAB_ROUTES) nav.goTab(r) else if (settled) nav.navigate(r)
    }
    // محتوى الصفحات مصدرٌ للزجاج (شريط التنقل والأوراق السفلية)، إلا صفحة المصحف: لا شريط فوقها، فلا كلفة إضافية على التقليب
    val appHaze = rememberHazeState()
    val glassSource = current != SURAH_ROUTE

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
        SharedTransitionLayout(Modifier.fillMaxSize().then(if (glassSource) Modifier.hazeSource(appHaze) else Modifier)) {
            // لون النص الافتراضي يتبع الخلفية، وإلا ظهر نص بلا لون محدد أسود في الوضع الداكن
            CompositionLocalProvider(
                LocalSharedScope provides this,
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                NavHost(
                    nav, startDestination = "home", modifier = Modifier.fillMaxSize(),
                    enterTransition = { enter(rtl) },
                    exitTransition = { exit(rtl) },
                    popEnterTransition = { popEnter(rtl) },
                    popExitTransition = { popExit(rtl) },
                ) {
                    // مكان شريط التنقل لكل صفحة تبويب بحسبها هي، لا بحسب الصفحة الظاهرة: فلا يقفز محتواها أثناء الانتقال منها وإليها.
                    // مقاس النافذة يُقرأ هنا (لا في منشئ الشبكة) فلا يُعاد بناء شبكة التنقل حين تتغير النافذة
                    fun screen(route: String, content: @Composable () -> Unit) = composable(route) {
                        val tab = route in TAB_ROUTES
                        val side = LocalWindowLayout.current.rail
                        val host = LocalTabBarHost.current
                        CompositionLocalProvider(
                            LocalNavScope provides this,
                            LocalBottomInset provides if (tab && !side) host.bottomInset else 0.dp,
                            LocalLeadingInset provides if (tab && side) host.leadingInset else 0.dp,
                        ) {
                            // كل صفحة معتمة: في iOS تنزلق الصفحتان معاً، فلا تظهر إحداهما من خلال الأخرى
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
                        }
                    }
                    screen("home") { HomeScreen(open = open) }
                    screen("prayer") { PrayerScreen(open = open) }
                    screen("qibla") { QiblaScreen(back) }
                    screen("quran") { QuranScreen(open = open) }
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
                            val from = args?.read { getStringOrNull("from") } ?: "row"
                            SurahScreen(
                                surah = (args?.read { getIntOrNull("surah") } ?: 1).coerceIn(1, 114),
                                startAyah = args?.read { getIntOrNull("ayah") } ?: 0,
                                from = from,
                                onBack = back,
                                openSurah = { n -> nav.navigate(surahRoute(n, 0, "nav")) { popUpTo(SURAH_ROUTE) { inclusive = true } } },
                                startInFocus = from == "focus",
                                openLibrary = { nav.navigate("tafsirs") }
                            )
                        }
                    }
                    screen("tasbih") { TasbihScreen() }
                    screen("adhkar") { com.osatks.mizan.ui.screens.AdhkarScreen(back) }
                    screen("settings") { SettingsScreen(open = open) }
                    screen("tafsirs") { com.osatks.mizan.ui.quran.TafsirLibraryScreen(back) }
                    screen("guide") { com.osatks.mizan.ui.screens.Onboarding(back) }
                    screen("adhan") { com.osatks.mizan.ui.screens.AdhanScreen(back) }
                    screen("licenses") { com.osatks.mizan.ui.screens.LicensesScreen(back) }
                    screen("zakat") { ZakatScreen(back) }
                    screen("fitr") { FitrScreen(back) }
                    screen("inheritance") { InheritanceScreen(back) }
                    screen("hijri") { HijriScreen(back) }
                    screen("age") { AgeScreen(back) }
                    screen("events") { EventsScreen(back) }
                    screen("fidya") { FidyaScreen(back) }
                    screen("search") {
                        SearchScreen(back) { route ->
                            if (route in TAB_ROUTES) { nav.popBackStack(); nav.goTab(route) }
                            else nav.navigate(route) { popUpTo("search") { inclusive = true } }
                        }
                    }
                }
            }
        }

        // جولة أول تشغيل فوق كل شيء، مرة واحدة
        var firstRun by remember { mutableStateOf(!AppSettings.getBool("onboarded", false) && startRoute == null) }
        AnimatedVisibility(firstRun, enter = fadeIn(), exit = fadeOut(tween(Motion.MEDIUM))) {
            com.osatks.mizan.ui.screens.Onboarding { firstRun = false; AppSettings.putBool("onboarded", true) }
        }
        // الشريط لا يظهر فوق جولة أول تشغيل (كان يغطي أزرارها السفلية)، ولا مع القائمة الجانبية.
        // يُستدعى دائماً: شريط النظام في iOS 26 يُخبَر بالإخفاء ويتولى حركته بنفسه
        bars.Bottom(onTab && !firstRun && !rail, current, Modifier.align(Alignment.BottomCenter)) { nav.goTab(it) }
        // القائمة الجانبية تدخل من جهة البداية (اليمين في العربية) وتخرج إليها
        AnimatedVisibility(
            visible = onTab && !firstRun && rail,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { if (rtl) it else -it } + fadeIn(),
            exit = slideOutHorizontally(tween(Motion.SHORT, easing = Motion.Accelerate)) { if (rtl) it else -it } + fadeOut(),
        ) {
            // الأسماء تحت الأيقونات ما دام الارتفاع يتسع لها (لا في هاتف بالعرض)
            bars.Rail(current, labels = maxHeight >= 440.dp) { nav.goTab(it) }
        }
        }
    }
}

// ---------- الانتقالات ----------
// بين التبويبات: "تلاشٍ عابر" (يخبو القديم بسرعة ويكبر الجديد بلطف).
// إلى أداة: تدخل الصفحة من جهة القراءة (اليمين في العربية) بينما تنكمش السابقة للخلف قليلاً.

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    initialState.destination.route in TAB_ROUTES && targetState.destination.route in TAB_ROUTES

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(rtl: Boolean): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(Motion.MEDIUM, delayMillis = 70)) + scaleIn(tween(Motion.LONG, easing = Motion.Decelerate), initialScale = 0.94f)
    else PageMotion.push(rtl)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(rtl: Boolean): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(90))
    else if (IsIos) PageMotion.pushUnder(rtl)
    else scaleOut(tween(Motion.LONG, easing = Motion.Emphasized), targetScale = 0.94f) + fadeOut(tween(Motion.MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(rtl: Boolean): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(Motion.MEDIUM, delayMillis = 70)) + scaleIn(tween(Motion.LONG, easing = Motion.Decelerate), initialScale = 0.94f)
    else if (IsIos) PageMotion.popUnder(rtl)
    else scaleIn(tween(Motion.LONG, easing = Motion.Emphasized), initialScale = 0.94f) + fadeIn(tween(Motion.MEDIUM))

// في iOS: الصفحة كلها تخرج إلى جهة الرجوع وتعود التي تحتها من ثلث العرض، بحركة خطية يقودها سحب الحافة فتبقى تحت الإصبع
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
