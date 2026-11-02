package com.osatks.mizan.ui.screens

import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.core.*
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
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
import com.osatks.mizan.ui.LocalPageColumns
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.PickerField
import com.osatks.mizan.ui.Reveal
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Stepper
import com.osatks.mizan.ui.ToolPage
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

@Composable
fun PrayerScreen(open: (String) -> Unit = {}) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    // اليوم المختار وجدول الشهر يبقيان بعد تبديل التبويب أو الرجوع من إعدادات الأذان
    var day by androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver({ it.value.toEpochDays() }, { mutableStateOf(LocalDate.fromEpochDays(it)) })
    ) { mutableStateOf(PrayerUtil.now().toLocalDate()) }
    var showMonth by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    var locationDenied by remember { mutableStateOf(false) }
    val place = AppSettings.place

    // العدّ كل ثانية والصفحة ظاهرة فقط (تبقى حية تحت غيرها في iOS)
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(place, AppSettings.method, AppSettings.hanafi, active) {
        while (true) { next = PrayerUtil.next(); if (!active) break; delay(1000) }
    }

    val locate = rememberLocationRequest { r ->
        locating = false
        when (r) {
            is LocationResult.Found -> { AppSettings.updatePlace(r.place); PrayerAlerts.reschedule(); Platform.toast(s.locationFound) }
            LocationResult.Failed -> Platform.toast(s.locationFailed)
            LocationResult.Denied -> locationDenied = true
        }
    }
    if (locationDenied) LocationDeniedDialog(onPickCity = { locationDenied = false; pickCity = true }, onDismiss = { locationDenied = false })
    val toggleNotify = rememberAlertsToggle()

    val city = place.city
    val subtitle = s.placeName(place) + (city?.let { " · " + s.country(it.cc, lang) } ?: "")
    // على الشاشة الواسعة: مواقيت اليوم وجدول الشهر متجاوران، والجدول ظاهر دائماً يتبع اليوم المختار
    ToolPage(s.prayerTitle, subtitle, sharedKey = "prayer", icon = TOOL_ICONS["prayer"]) {
        if (!AppSettings.placeSet) item { LocationNeededCard() }
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
                Text(s.prayerEndsNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { day = PrayerUtil.now().toLocalDate() }) { Text(s.today) }
                    if (LocalPageColumns.current == 1) TextButton(onClick = { showMonth = !showMonth }) { Text(if (showMonth) s.hideMonthTable else s.monthTable) }
                }
            }
        }
        side { Reveal(showMonth || LocalPageColumns.current == 2) { MonthTable(day) } }
        item {
            SectionCard(s.locationAndMethod) {
                Button(
                    onClick = { locating = true; locate() },
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
                    onSelect = { AppSettings.updateMethod(it); PrayerAlerts.reschedule() })
                Text(s.asrMethod, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmented(listOf("std" to s.asrStandard, "hanafi" to s.asrHanafi), if (AppSettings.hanafi) "hanafi" else "std") {
                    AppSettings.updateHanafi(it == "hanafi"); PrayerAlerts.reschedule()
                }
                StatRow(s.latLng, "${Fmt.num(place.lat)}° / ${Fmt.num(place.lng)}°")
            }
        }
        side {
            // الإقامة: دقائق بعد الأذان لكل صلاة كما في مسجد المستخدم
            SectionCard(s.iqamaTitle) {
                Text(s.iqamaDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
                    Stepper(s.prayer(k), AppSettings.iqama[k] ?: 0, { AppSettings.updateIqama(k, it) }, min = 0, max = 90)
                }
            }
        }
        side {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.adhanAlert, fontWeight = FontWeight.Bold)
                        Text(s.adhanAlertDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                // الأذان والتنبيهات: المؤذن، وطريقة التنبيه لكل صلاة، والاهتزاز
                com.osatks.mizan.ui.ClickRow(s.adhanSettings, s.adhanSettingsSub, onClick = { open("adhan") }, leading = Icons.AutoMirrored.Filled.VolumeUp) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(s.timesMayDiffer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.prayer) }
    }

    if (pickCity) CityPickerSheet(place.key.takeIf { !place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlerts.reschedule()
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
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isNext) { PulseDot(); Spacer(Modifier.width(10.dp)) }
                    // الاسم وتحته نهاية الوقت؛ الأذان وتحته الإقامة
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.prayer(key), fontWeight = FontWeight.Bold, fontSize = if (key == "sunrise") 15.sp else 17.sp,
                            color = if (key == "sunrise") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        val end = t.end(key)
                        if (!end.isNaN()) Text(s.endsAt(s.time(end)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(s.time(t[key]), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        val iq = AppSettings.iqama[key] ?: 0
                        if (iq > 0 && !t[key].isNaN()) {
                            Text(s.iqamaAt(s.time(t[key] + iq / 60.0)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseDot() {
    // ينبض والصفحة ظاهرة فقط: الحركة اللانهائية في صفحة مغطاة تبقي الشاشة تُرسم بلا توقف
    val k by if (com.osatks.mizan.ui.LocalPageActive.current) {
        rememberInfiniteTransition(label = "pulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "k")
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0.4f) }
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
