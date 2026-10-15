package com.osatks.mizan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.screens.AgeScreen
import com.osatks.mizan.ui.screens.EventsScreen
import com.osatks.mizan.ui.screens.FidyaScreen
import com.osatks.mizan.ui.screens.FitrScreen
import com.osatks.mizan.ui.screens.HijriScreen
import com.osatks.mizan.ui.screens.HomeScreen
import com.osatks.mizan.ui.screens.InheritanceScreen
import com.osatks.mizan.ui.screens.PrayerScreen
import com.osatks.mizan.ui.screens.QiblaScreen
import com.osatks.mizan.ui.screens.SettingsScreen
import com.osatks.mizan.ui.screens.TasbihScreen
import com.osatks.mizan.ui.screens.ZakatScreen
import com.osatks.mizan.ui.theme.MizanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        PrayerAlarms.ensureChannel(this)
        PrayerAlarms.scheduleNext(this)
        enableEdgeToEdge()
        setContent { MizanTheme { MizanApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "الرئيسية", Icons.Filled.Home),
    Tab("prayer", "الصلاة", Icons.Filled.Schedule),
    Tab("qibla", "القبلة", Icons.Filled.Explore),
    Tab("tasbih", "المسبحة", Icons.Filled.TouchApp),
    Tab("settings", "الإعدادات", Icons.Filled.Settings),
)

@Composable
fun MizanApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.goTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { padding ->
        val back: () -> Unit = { nav.popBackStack() }
        NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            composable("home") { HomeScreen(open = { nav.navigate(it) }) }
            composable("prayer") { PrayerScreen() }
            composable("qibla") { QiblaScreen() }
            composable("tasbih") { TasbihScreen() }
            composable("settings") { SettingsScreen() }
            composable("zakat") { ZakatScreen(back) }
            composable("fitr") { FitrScreen(back) }
            composable("inheritance") { InheritanceScreen(back) }
            composable("hijri") { HijriScreen(back) }
            composable("age") { AgeScreen(back) }
            composable("events") { EventsScreen(back) }
            composable("fidya") { FidyaScreen(back) }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
