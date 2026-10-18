package com.osatks.mizan

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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.osatks.mizan.ui.FloatingTabBar
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
internal val ROUTES = setOf("home", "prayer", "qibla", "tasbih", "settings", "zakat", "fitr", "inheritance", "hijri", "age", "events", "fidya", "search", "quran", "tafsirs", "guide", "adhkar")
private val TAB_ROUTES = TABS.map { it.route }.toSet()

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MizanApp(startRoute: String? = null) {
    val nav = rememberNavController()
    LaunchedEffect(startRoute) {
        if (startRoute != null && startRoute != "home" && (startRoute in ROUTES || startRoute.startsWith("surah/"))) nav.navigate(startRoute)
    }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val onTab = current == null || current in TAB_ROUTES
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val back: () -> Unit = { nav.popBackStack() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedScope provides this, LocalBottomInset provides if (onTab) 92.dp else 0.dp) {
                NavHost(
                    nav, startDestination = "home", modifier = Modifier.fillMaxSize(),
                    enterTransition = { enter(rtl) },
                    exitTransition = { exit() },
                    popEnterTransition = { popEnter() },
                    popExitTransition = { popExit(rtl) },
                ) {
                    fun screen(route: String, content: @Composable () -> Unit) = composable(route) {
                        CompositionLocalProvider(LocalNavScope provides this) { content() }
                    }
                    screen("home") { HomeScreen(open = { nav.navigate(it) }) }
                    screen("prayer") { PrayerScreen() }
                    screen("qibla") { QiblaScreen(back) }
                    screen("quran") { QuranScreen(open = { nav.navigate(it) }) }
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
                    screen("settings") { SettingsScreen(open = { nav.navigate(it) }) }
                    screen("tafsirs") { com.osatks.mizan.ui.quran.TafsirLibraryScreen(back) }
                    screen("guide") { com.osatks.mizan.ui.screens.Onboarding(back) }
                    screen("zakat") { ZakatScreen(back) }
                    screen("fitr") { FitrScreen(back) }
                    screen("inheritance") { InheritanceScreen(back) }
                    screen("hijri") { HijriScreen(back) }
                    screen("age") { AgeScreen(back) }
                    screen("events") { EventsScreen(back) }
                    screen("fidya") { FidyaScreen(back) }
                    screen("search") {
                        SearchScreen(back) { route ->
                            nav.navigate(route) { popUpTo("search") { inclusive = true } }
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
        AnimatedVisibility(
            visible = onTab,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it } + fadeIn(),
            exit = slideOutVertically(tween(Motion.SHORT, easing = Motion.Accelerate)) { it } + fadeOut(),
        ) {
            FloatingTabBar(current) { nav.goTab(it) }
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
    else slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { w -> (if (rtl) -w else w) / 4 } +
        fadeIn(tween(Motion.MEDIUM, delayMillis = 40))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(90))
    else scaleOut(tween(Motion.LONG, easing = Motion.Emphasized), targetScale = 0.94f) + fadeOut(tween(Motion.MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    if (isTabSwitch()) fadeIn(tween(Motion.MEDIUM, delayMillis = 70)) + scaleIn(tween(Motion.LONG, easing = Motion.Decelerate), initialScale = 0.94f)
    else scaleIn(tween(Motion.LONG, easing = Motion.Emphasized), initialScale = 0.94f) + fadeIn(tween(Motion.MEDIUM))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(rtl: Boolean): ExitTransition =
    if (isTabSwitch()) fadeOut(tween(90))
    else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Accelerate)) { w -> (if (rtl) -w else w) / 4 } +
        fadeOut(tween(Motion.SHORT, delayMillis = 60))

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
