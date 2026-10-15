package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.delay
import java.time.LocalDate

private data class ToolItem(val route: String, val name: String, val desc: String, val icon: ImageVector)

private val TOOLS = listOf(
    ToolItem("zakat", "حاسبة الزكاة", "المال والذهب والأسهم مع النصاب", Icons.Filled.AccountBalanceWallet),
    ToolItem("inheritance", "حاسبة المواريث", "تقسيم التركة وفق علم الفرائض", Icons.Filled.Groups),
    ToolItem("prayer", "مواقيت الصلاة", "مع تنبيه الأذان والعد التنازلي", Icons.Filled.Schedule),
    ToolItem("qibla", "اتجاه القبلة", "بوصلة تعمل بحساسات الجهاز", Icons.Filled.Explore),
    ToolItem("hijri", "التاريخ الهجري", "تحويل وتقويم أم القرى", Icons.Filled.DateRange),
    ToolItem("events", "المناسبات", "رمضان والعيدان وعرفة", Icons.Filled.Event),
    ToolItem("tasbih", "المسبحة", "عدّاد ذكر مع أذكار جاهزة", Icons.Filled.TouchApp),
    ToolItem("fitr", "زكاة الفطر", "بالصاع أو بالقيمة", Icons.Filled.Grain),
    ToolItem("age", "حاسبة العمر", "بالهجري والميلادي", Icons.Filled.Cake),
    ToolItem("fidya", "الفدية والكفارة", "الصيام واليمين", Icons.Filled.VolunteerActivism),
)

@Composable
fun HomeScreen(open: (String) -> Unit) {
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    LaunchedEffect(AppSettings.place, AppSettings.method, AppSettings.hanafi) {
        while (true) { next = PrayerUtil.next(); delay(1000) }
    }
    val today = LocalDate.now()
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(HeroBrush)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text("ميزان", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("أدوات المسلم", color = Gold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(Hijri.hijriStr(Hijri.toHijri(today)), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("${Hijri.weekday(today)} ${Hijri.gregStr(today)}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Surface(
                    onClick = { open("prayer") },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = .12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("الصلاة القادمة · ${AppSettings.place.name}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
                            Text(next.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(PrayerUtil.hms(next.remaining), color = Gold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        item {
            Text("الأدوات", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp))
        }
        TOOLS.chunked(2).forEach { pair ->
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { t ->
                        Surface(
                            onClick = { open(t.route) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 1.dp,
                            modifier = Modifier.weight(1f).height(150.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Box(
                                    Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) { Icon(t.icon, null, tint = MaterialTheme.colorScheme.primary) }
                                Spacer(Modifier.height(10.dp))
                                Text(t.name, style = MaterialTheme.typography.titleMedium)
                                Text(t.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
