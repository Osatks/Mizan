package com.osatks.mizan.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.LocationHelper
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.country
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.notify.PrayerAlarms
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.PickerField
import com.osatks.mizan.ui.Reveal
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun PrayerScreen() {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ctx = LocalContext.current
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var day by remember { mutableStateOf(PrayerUtil.now().toLocalDate()) }
    var showMonth by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    val place = AppSettings.place

    LaunchedEffect(place, AppSettings.method, AppSettings.hanafi) {
        while (true) { next = PrayerUtil.next(); delay(1000) }
    }

    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            locating = true
            LocationHelper.fetch(ctx) { p ->
                locating = false
                if (p != null) { AppSettings.updatePlace(p); PrayerAlarms.scheduleNext(ctx); Toast.makeText(ctx, s.locationFound, Toast.LENGTH_SHORT).show() }
                else Toast.makeText(ctx, s.locationFailed, Toast.LENGTH_LONG).show()
            }
        } else Toast.makeText(ctx, s.locationDenied, Toast.LENGTH_LONG).show()
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        AppSettings.updateNotify(ok); PrayerAlarms.scheduleNext(ctx)
    }

    val city = place.city
    val subtitle = s.placeName(place) + (city?.let { " · " + s.country(it.cc, lang) } ?: "")
    ToolPage(s.prayerTitle, subtitle, sharedKey = "prayer", icon = TOOL_ICONS["prayer"]) {
        item {
            ResultCard(s.nextPrayerLabel(s.prayer(next.key)), PrayerUtil.hms(next.remaining), s.atTime(s.time(next.at.hour + next.at.minute / 60.0)))
        }
        item {
            SectionCard {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { day = day.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, s.prevDay) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.weekday(day)} ${s.greg(day)}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(s.hijri(Hijri.toHijri(day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { day = day.plusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.nextDay) }
                }
                // يوم جديد ينزلق من جهة السهم المضغوط
                AnimatedContent(
                    targetState = day,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                        (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> sign * w / 3 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                            (slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> -sign * w / 3 } + fadeOut(tween(Motion.SHORT))) using
                            SizeTransform(clip = false)
                    },
                    label = "day"
                ) { d -> DayTimes(d, next) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { day = PrayerUtil.now().toLocalDate() }) { Text(s.today) }
                    TextButton(onClick = { showMonth = !showMonth }) { Text(if (showMonth) s.hideMonthTable else s.monthTable) }
                }
            }
        }
        item { Reveal(showMonth) { MonthTable(day) } }
        item {
            SectionCard(s.locationAndMethod) {
                Button(
                    onClick = { locPerm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) },
                    enabled = !locating, modifier = Modifier.fillMaxWidth()
                ) {
                    if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Icon(Icons.Filled.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (locating) s.locating else s.useMyLocation)
                }
                Text(s.orPickCity, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                PickerField(
                    if (place.gps) "📍 ${s.placeName(place)}" else s.placeName(place),
                    Icons.Filled.LocationOn
                ) { pickCity = true }
                Picker(s.calcMethod, PrayTimes.METHODS.map { it.key }, AppSettings.method, text = { s.methods[it] ?: it },
                    onSelect = { AppSettings.updateMethod(it); PrayerAlarms.scheduleNext(ctx) })
                Text(s.asrMethod, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmented(listOf("std" to s.asrStandard, "hanafi" to s.asrHanafi), if (AppSettings.hanafi) "hanafi" else "std") {
                    AppSettings.updateHanafi(it == "hanafi"); PrayerAlarms.scheduleNext(ctx)
                }
                StatRow(s.latLng, "${Fmt.num(place.lat)}° / ${Fmt.num(place.lng)}°")
            }
        }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.adhanAlert, fontWeight = FontWeight.Bold)
                        Text(s.adhanAlertDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = AppSettings.notify, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else { AppSettings.updateNotify(on); PrayerAlarms.scheduleNext(ctx) }
                    })
                }
                Text(s.timesMayDiffer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.prayer) }
    }

    if (pickCity) CityPickerSheet(place.key.takeIf { !place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlarms.scheduleNext(ctx)
    }
}

/** صلوات يوم واحد، والصلاة القادمة مميزة بنقطة نابضة */
@Composable
private fun DayTimes(day: LocalDate, next: NextPrayer) {
    val s = LocalStrings.current
    val t = PrayerUtil.times(day)
    val isToday = day == PrayerUtil.now().toLocalDate()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrayTimes.PRAYERS.forEach { key ->
            val isNext = isToday && key == next.key && next.at.toLocalDate() == day
            val bg by animateColorAsState(
                if (isNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                tween(Motion.LONG), label = "row"
            )
            Surface(
                shape = RoundedCornerShape(14.dp), color = bg,
                border = if (isNext) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isNext) { PulseDot(); Spacer(Modifier.width(10.dp)) }
                    Text(
                        s.prayer(key), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = if (key == "sunrise") 15.sp else 17.sp,
                        color = if (key == "sunrise") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(s.time(t[key]), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PulseDot() {
    val t = rememberInfiniteTransition(label = "pulse")
    val k by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "k")
    val color = MaterialTheme.colorScheme.secondary
    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(14.dp).graphicsLayer { scaleX = 0.6f + k; scaleY = 0.6f + k; alpha = 1f - k }
                .clip(CircleShape).background(color.copy(alpha = .5f))
        )
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun MonthTable(ref: LocalDate) {
    val s = LocalStrings.current
    val first = ref.withDayOfMonth(1)
    val today = PrayerUtil.now().toLocalDate()
    SectionCard(s.monthTableTitle(ref.monthValue, ref.year)) {
        Row(Modifier.fillMaxWidth()) {
            (listOf(s.colDay) + listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { s.prayer(it) }).forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
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
