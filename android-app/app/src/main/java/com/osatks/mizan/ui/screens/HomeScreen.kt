package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.RevealState
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.starBackdrop
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.reveal
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.sharedIcon
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.delay
import java.time.LocalDate

/** ترتيب الأدوات وأيقوناتها (تشترك فيها الرئيسية والبحث ورؤوس الصفحات) */
val TOOL_ORDER = listOf("zakat", "inheritance", "prayer", "qibla", "hijri", "events", "tasbih", "fitr", "age", "fidya")
val TOOL_ICONS: Map<String, ImageVector> = mapOf(
    "zakat" to Icons.Filled.AccountBalanceWallet,
    "inheritance" to Icons.Filled.Groups,
    "prayer" to Icons.Filled.Schedule,
    "qibla" to Icons.Filled.Explore,
    "hijri" to Icons.Filled.DateRange,
    "events" to Icons.Filled.Event,
    "tasbih" to Icons.Filled.TouchApp,
    "fitr" to Icons.Filled.Grain,
    "age" to Icons.Filled.Cake,
    "fidya" to Icons.Filled.VolunteerActivism,
)

@Composable
fun HomeScreen(open: (String) -> Unit) {
    val s = LocalStrings.current
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var showLang by remember { mutableStateOf(false) }
    LaunchedEffect(AppSettings.place, AppSettings.method, AppSettings.hanafi) {
        while (true) { next = PrayerUtil.next(); delay(1000) }
    }
    val today = LocalDate.now()
    val reveal = remember { RevealState() }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp + bottom + LocalBottomInset.current)) {
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(HeroBrush)
                    .starBackdrop(Color.White.copy(alpha = .08f), 120.dp, Alignment.TopEnd, (-60).dp, 90.dp, 80_000, stroke = 4f)
            ) {
                Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f).reveal(reveal, 0)) {
                            Text(s.appName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                            Text(s.appTagline, color = Gold, fontWeight = FontWeight.Bold)
                        }
                        val src = remember { MutableInteractionSource() }
                        Surface(
                            onClick = { showLang = true }, shape = CircleShape, color = Color.White.copy(alpha = .14f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .22f)), interactionSource = src,
                            modifier = Modifier.pressScale(src, 0.9f)
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Language, s.language, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(LocalLang.current.native, color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.reveal(reveal, 1)) {
                        Text(s.hijri(Hijri.toHijri(today)), color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("${s.weekday(today)} ${s.greg(today)}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    SearchBarButton(s.searchHint, Modifier.reveal(reveal, 2)) { open("search") }
                    Spacer(Modifier.height(12.dp))
                    val src = remember { MutableInteractionSource() }
                    Surface(
                        onClick = { open("prayer") },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = .12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .2f)),
                        interactionSource = src,
                        modifier = Modifier.fillMaxWidth().reveal(reveal, 3).pressScale(src, 0.97f)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.nextPrayerIn(s.placeName(AppSettings.place)), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                RollingText(s.prayer(next.key), MaterialTheme.typography.headlineSmall, Color.White)
                            }
                            RollingText(PrayerUtil.hms(next.remaining), MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), Gold)
                        }
                    }
                }
            }
        }
        item {
            Text(s.toolsTitle, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp).reveal(reveal, 4))
        }
        TOOL_ORDER.chunked(2).forEachIndexed { row, pair ->
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp).reveal(reveal, 5 + row).height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { key -> ToolTile(key, Modifier.weight(1f)) { open(key) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (showLang) LanguageSheet { showLang = false }
}

@Composable
private fun ToolTile(key: String, modifier: Modifier, onClick: () -> Unit) {
    val s = LocalStrings.current
    val t = s.tools.getValue(key)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
        interactionSource = src,
        modifier = modifier.sharedCard("card-$key", RoundedCornerShape(20.dp)).fillMaxHeight().heightIn(min = 150.dp).pressScale(src)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier.sharedIcon("icon-$key").size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(TOOL_ICONS.getValue(key), null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(10.dp))
            Text(t.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** شريط البحث في الرئيسية: زر يتحول إلى حقل البحث في صفحته */
@Composable
private fun SearchBarButton(hint: String, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        interactionSource = src,
        modifier = modifier.fillMaxWidth().sharedCard("search-bar", RoundedCornerShape(18.dp)).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.sharedIcon("search-icon"))
            Spacer(Modifier.width(10.dp))
            Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
