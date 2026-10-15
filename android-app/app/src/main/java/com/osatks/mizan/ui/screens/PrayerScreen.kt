package com.osatks.mizan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import kotlinx.coroutines.delay
import android.widget.Toast

@Composable
fun PrayerScreen() {
    val ctx = LocalContext.current
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var day by remember { mutableStateOf(PrayerUtil.now().toLocalDate()) }
    var showMonth by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val place = AppSettings.place

    LaunchedEffect(place, AppSettings.method, AppSettings.hanafi) {
        while (true) { next = PrayerUtil.next(); delay(1000) }
    }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locating = true
            LocationHelper.fetch(ctx) { p ->
                locating = false
                if (p != null) { AppSettings.updatePlace(p); PrayerAlarms.scheduleNext(ctx); Toast.makeText(ctx, "تم تحديد موقعك ✓", Toast.LENGTH_SHORT).show() }
                else Toast.makeText(ctx, "تعذر تحديد الموقع، تأكد من تشغيل خدمة الموقع", Toast.LENGTH_LONG).show()
            }
        } else Toast.makeText(ctx, "لم يُمنح إذن الموقع، اختر مدينتك من القائمة", Toast.LENGTH_LONG).show()
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.updateNotify(ok); PrayerAlarms.scheduleNext(ctx)
    }

    ToolPage("مواقيت الصلاة", "${place.name}${if (place.country.isNotEmpty()) " · " + place.country else ""}") {
        item {
            ResultCard("الصلاة القادمة: ${next.name}", PrayerUtil.hms(next.remaining), "عند " + PrayTimes.format(next.at.hour + next.at.minute / 60.0, AppSettings.use24h))
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { day = day.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "اليوم السابق") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${Hijri.weekday(day)} ${Hijri.gregStr(day)}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(Hijri.hijriStr(Hijri.toHijri(day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { day = day.plusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "اليوم التالي") }
                }
                val t = PrayerUtil.times(day)
                val isToday = day == PrayerUtil.now().toLocalDate()
                PrayTimes.PRAYERS.forEach { (key, name) ->
                    val isNext = isToday && key == next.key && next.at.toLocalDate() == day
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                        border = if (isNext) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = if (key == "sunrise") 15.sp else 17.sp,
                                color = if (key == "sunrise") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            Text(PrayTimes.format(t[key], AppSettings.use24h), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { day = PrayerUtil.now().toLocalDate() }) { Text("اليوم") }
                    TextButton(onClick = { showMonth = !showMonth }) { Text(if (showMonth) "إخفاء جدول الشهر" else "جدول الشهر") }
                }
            }
        }
        if (showMonth) item { MonthTable(day) }
        item {
            SectionCard("الموقع وطريقة الحساب") {
                Button(
                    onClick = { locPerm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                    enabled = !locating, modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(8.dp))
                    Text(if (locating) "جارٍ تحديد الموقع…" else "استخدم موقعي الحالي")
                }
                val cityIdx = PrayTimes.CITIES.indexOfFirst { it.name == place.name && !place.gps }
                Picker(
                    "أو اختر مدينة", listOf(-1) + PrayTimes.CITIES.indices.toList(), cityIdx,
                    text = { i -> if (i < 0) (if (place.gps) "📍 ${place.name}" else "اختر مدينة") else PrayTimes.CITIES[i].let { "${it.name} — ${it.country}" } },
                    onSelect = { i ->
                        if (i >= 0) {
                            val c = PrayTimes.CITIES[i]
                            AppSettings.updatePlace(Place(c.name, c.country, c.lat, c.lng, c.tz, c.method))
                            PrayerAlarms.scheduleNext(ctx)
                        }
                    }
                )
                Picker("طريقة الحساب", PrayTimes.METHODS.map { it.key }, AppSettings.method, text = { PrayTimes.method(it).name },
                    onSelect = { AppSettings.updateMethod(it); PrayerAlarms.scheduleNext(ctx) })
                Text("مذهب حساب العصر", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmented(listOf("std" to "الجمهور", "hanafi" to "الحنفي"), if (AppSettings.hanafi) "hanafi" else "std") {
                    AppSettings.updateHanafi(it == "hanafi"); PrayerAlarms.scheduleNext(ctx)
                }
                StatRow("خط العرض / الطول", "%.2f° / %.2f°".format(place.lat, place.lng))
            }
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("تنبيه الأذان", fontWeight = FontWeight.Bold)
                        Text("إشعار عند دخول وقت كل صلاة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = AppSettings.notify, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else { AppSettings.updateNotify(on); PrayerAlarms.scheduleNext(ctx) }
                    })
                }
                Text("قد تختلف المواقيت دقيقة أو دقيقتين عن التقويم الرسمي في بلدك.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MonthTable(ref: java.time.LocalDate) {
    val first = ref.withDayOfMonth(1)
    val today = PrayerUtil.now().toLocalDate()
    SectionCard("جدول ${Hijri.GREG_MONTHS[ref.monthValue - 1]} ${ref.year}") {
        Row(Modifier.fillMaxWidth()) {
            listOf("اليوم", "الفجر", "الظهر", "العصر", "المغرب", "العشاء").forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        for (i in 0 until ref.lengthOfMonth()) {
            val d = first.plusDays(i.toLong())
            val t = PrayerUtil.times(d)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (d == today) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text("${d.dayOfMonth}", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
                        Text(PrayTimes.format(t[k], true), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
