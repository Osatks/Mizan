package com.osatks.mizan

import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.init
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.ui.quran.QuranScreen
import com.osatks.mizan.ui.quran.SURAH_ROUTE
import com.osatks.mizan.ui.quran.SurahScreen
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Demo
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.FloatingTabBar
import com.osatks.mizan.ui.LanguageHost
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalNavScope
import com.osatks.mizan.ui.LocalSharedScope
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.TABS
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        // الكود المشترك يطلب جدولة التنبيهات وتحديث قناتها عبر هذين
        PrayerAlerts.onReschedule = { PrayerAlarms.scheduleNext(it) }
        PrayerAlerts.onLanguageChanged = { PrayerAlarms.ensureChannel(it) }
        // إن أُغلق التطبيق أثناء القراءة في وضع التركيز يُعاد «عدم الإزعاج» كما كان
        com.osatks.mizan.ui.quran.QuietReading.restore()
        Motion.reduced = !ValueAnimator.areAnimatorsEnabled()
        // للقطات الشاشة والاختبار: --es lang en
        intent?.getStringExtra("lang")?.let { if (it == Lang.SYSTEM || Lang.of(it) != null) LocaleSync.choose(it) }
        LocaleSync.pull(this)
        PrayerAlarms.ensureChannel(this)
        PrayerAlarms.scheduleNext(this)
        enableEdgeToEdge()
        // فتح شاشة محددة عند التشغيل (للاختصارات ولقطات الشاشة): --es route prayer
        val startRoute = intent?.getStringExtra("route")
        // أداة أذكار الوقت تفتح قسم الأذكار المناسب للوقت
        intent?.getStringExtra("section")?.let { com.osatks.mizan.widget.WidgetLaunch.adhkarSection = it }
        Demo.enabled = intent?.getBooleanExtra("demo", false) == true
        // لقطات الشاشة: لون التطبيق يُختار من الأمر نفسه
        if (Demo.enabled) intent?.getStringExtra("accent")?.let { AppSettings.updateAccent(it) }
        // لقطات المتجر: موقع ثابت (مكة المكرمة) بدل بطاقة «حدّد موقعك»
        if (Demo.enabled && !AppSettings.placeSet) AppSettings.updatePlace(com.osatks.mizan.core.Place.of(com.osatks.mizan.core.PrayTimes.CITIES[0]))
        setContent { LanguageHost { MizanTheme { MizanApp(startRoute) } } }
    }

    // أذان سُمع أوله في المنبّه (أول تكبيرتين): يكمل الآن من حيث توقف (AdhanResume)
    override fun onResume() {
        super.onResume()
        com.osatks.mizan.core.AdhanResume.appVisible = true
        com.osatks.mizan.core.AdhanResume.handOff = { com.osatks.mizan.notify.AdhanService.handOff(this) }
        com.osatks.mizan.core.AdhanResume.onAppActive()
    }

    override fun onPause() {
        super.onPause()
        com.osatks.mizan.core.AdhanResume.appVisible = false
    }

    // تُحدَّث أدوات الشاشة الرئيسية بما تغير (الختمة، المدينة، اللغة) عند مغادرة التطبيق
    override fun onStop() {
        super.onStop()
        com.osatks.mizan.widget.Widgets.updateAll(this)
    }

    // اللغة تتغير دون إعادة إنشاء النشاط (configChanges) لتبقى حركة التبديل سلسة
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleSync.pull(this)
    }
}

