package com.osatks.mizan

import androidx.compose.runtime.Composable
import com.osatks.mizan.ui.quran.QuranScreen
import com.osatks.mizan.ui.quran.TafsirLibraryScreen
import com.osatks.mizan.ui.screens.AdhanScreen
import com.osatks.mizan.ui.screens.AdhkarScreen
import com.osatks.mizan.ui.screens.AgeScreen
import com.osatks.mizan.ui.screens.EventsScreen
import com.osatks.mizan.ui.screens.FidyaScreen
import com.osatks.mizan.ui.screens.FitrScreen
import com.osatks.mizan.ui.screens.HadithScreen
import com.osatks.mizan.ui.screens.HijriScreen
import com.osatks.mizan.ui.screens.HomeScreen
import com.osatks.mizan.ui.screens.InheritanceScreen
import com.osatks.mizan.ui.screens.LicensesScreen
import com.osatks.mizan.ui.screens.Onboarding
import com.osatks.mizan.ui.screens.PrayerScreen
import com.osatks.mizan.ui.screens.QiblaScreen
import com.osatks.mizan.ui.screens.SearchScreen
import com.osatks.mizan.ui.screens.SettingsScreen
import com.osatks.mizan.ui.screens.TasbihScreen
import com.osatks.mizan.ui.screens.ToolsScreen
import com.osatks.mizan.ui.screens.ZakatScreen

/** شاشات Compose لكل مسار في أندرويد (صفحة السورة مشتركة في Route) */
@Composable
internal actual fun PlatformRoute(route: String, app: AppNav, open: (String) -> Unit, back: () -> Unit) {
    when (route) {
        "home" -> HomeScreen(open = open)
        "prayer" -> PrayerScreen(open = open)
        "qibla" -> QiblaScreen(back)
        "quran" -> QuranScreen(open = open)
        "tasbih" -> TasbihScreen(back)
        "tools" -> ToolsScreen(open)
        "adhkar" -> AdhkarScreen(back)
        "settings" -> SettingsScreen(open = open)
        "tafsirs" -> TafsirLibraryScreen(back)
        "guide" -> Onboarding(back)
        "adhan" -> AdhanScreen(back)
        "licenses" -> LicensesScreen(back)
        "hadith" -> HadithScreen(back)
        "zakat" -> ZakatScreen(back)
        "fitr" -> FitrScreen(back)
        "inheritance" -> InheritanceScreen(back)
        "hijri" -> HijriScreen(back)
        "age" -> AgeScreen(back)
        "events" -> EventsScreen(back)
        "fidya" -> FidyaScreen(back)
        "search" -> SearchScreen(back) { r -> app.searchResult(r) }
    }
}
