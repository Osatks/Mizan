package com.osatks.mizan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.ui.quran.IosTafsirLibraryScreen
import com.osatks.mizan.ui.quran.NativeQuranView
import com.osatks.mizan.ui.screens.IosAdhanScreen
import com.osatks.mizan.ui.screens.IosAdhkarScreen
import com.osatks.mizan.ui.screens.IosAgeScreen
import com.osatks.mizan.ui.screens.IosEventsScreen
import com.osatks.mizan.ui.screens.IosFidyaScreen
import com.osatks.mizan.ui.screens.IosFitrScreen
import com.osatks.mizan.ui.screens.IosHadithScreen
import com.osatks.mizan.ui.screens.IosHijriScreen
import com.osatks.mizan.ui.screens.IosHomeScreen
import com.osatks.mizan.ui.screens.IosInheritanceScreen
import com.osatks.mizan.ui.screens.IosOnboarding
import com.osatks.mizan.ui.screens.IosPrayerScreen
import com.osatks.mizan.ui.screens.IosSearchScreen
import com.osatks.mizan.ui.screens.IosSettingsScreen
import com.osatks.mizan.ui.screens.IosZakatScreen
import com.osatks.mizan.ui.screens.NativeLicensesView
import com.osatks.mizan.ui.screens.NativeQiblaView
import com.osatks.mizan.ui.screens.NativeTasbihView
import com.osatks.mizan.ui.screens.NativeToolsView

/**
 * شاشة كل مسار في iOS بواجهة النظام (SwiftUI داخل متحكم الصفحة)؛ الحالة والحساب في Kotlin، وSwift يرسم ويعيد اللمس.
 * صفحة السورة مشتركة (Route)، والرئيسية والمواقيت بشاشة Compose المشتركة قبل اختيار الموقع
 */
@Composable
internal actual fun PlatformRoute(route: String, app: AppNav, open: (String) -> Unit, back: () -> Unit) {
    when (route) {
        "home" -> IosHomeScreen(open)
        "prayer" -> IosPrayerScreen(open)
        // البوصلة من اتجاه الهاتف في Swift
        "qibla" -> NativeQiblaView(back, Modifier.fillMaxSize())
        "quran" -> NativeQuranView(open, Modifier.fillMaxSize())
        // العدّ يظهر فور اللمس في Swift ويصل إلى Tasbih
        "tasbih" -> NativeTasbihView(back, Modifier.fillMaxSize())
        "tools" -> NativeToolsView(open, Modifier.fillMaxSize())
        "adhkar" -> IosAdhkarScreen(back)
        "settings" -> IosSettingsScreen(open)
        "tafsirs" -> IosTafsirLibraryScreen(back)
        "guide" -> IosOnboarding(back)
        "adhan" -> IosAdhanScreen(back)
        "licenses" -> NativeLicensesView(back, Modifier.fillMaxSize())
        "hadith" -> IosHadithScreen(back)
        "zakat" -> IosZakatScreen(back)
        "fitr" -> IosFitrScreen(back)
        "inheritance" -> IosInheritanceScreen(back)
        "hijri" -> IosHijriScreen(back)
        "age" -> IosAgeScreen(back)
        "events" -> IosEventsScreen(back)
        "fidya" -> IosFidyaScreen(back)
        "search" -> IosSearchScreen(back) { r -> app.searchResult(r) }
    }
}
