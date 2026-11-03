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
import androidx.compose.foundation.clickable
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
        // القبلة مع المواقيت كما هو المعتاد: بوصلة صغيرة حية، والزاوية والجهة والمسافة، وتفتح شاشة القبلة
        item { QiblaCard(onOpen = { open("qibla") }) }
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


/** بطاقة القبلة في شاشة المواقيت: البوصلة تدور مع الهاتف، والإبرة الذهبية إلى الكعبة، وتتذهّب الحلقة عند الاتجاه الصحيح */
@Composable
private fun QiblaCard(onOpen: () -> Unit) {
    val s = LocalStrings.current
    val place = AppSettings.place
    val qibla = PrayTimes.qibla(place.lat, place.lng)
    val compass = com.osatks.mizan.platform.rememberCompass(place.lat, place.lng)
    val heading = if (compass.available) compass.heading else 0f
    val diff = ((((qibla - heading) % 360) + 540) % 360 - 180).toFloat()
    val aligned = compass.available && kotlin.math.abs(diff) < 4
    val gold = androidx.compose.ui.graphics.Color(0xFFE0B54A)
    val ring by androidx.compose.animation.animateColorAsState(
        if (aligned) gold else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "qiblaRing"
    )
    // الإبرة تدور بأقصر طريق وبنعومة
    val needle by androidx.compose.animation.core.animateFloatAsState(diff, tween(220), label = "qiblaNeedle")
    val dirs = s.directions[(kotlin.math.round(qibla / 45.0).toInt()) % 8]
    SectionCard {
        Row(
            Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(onClick = onOpen).padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                androidx.compose.foundation.Canvas(Modifier.size(52.dp)) {
                    val r = size.minDimension / 2
                    val c = center
                    drawCircle(ring, r - 2.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    // علامات الجهات الأربع تدور مع الهاتف
                    for (i in 0 until 4) {
                        val a = (i * 90 - heading).toDouble() * kotlin.math.PI / 180
                        val o = androidx.compose.ui.geometry.Offset((c.x + kotlin.math.sin(a) * (r - 7.dp.toPx())).toFloat(), (c.y - kotlin.math.cos(a) * (r - 7.dp.toPx())).toFloat())
                        drawCircle(if (i == 0) androidx.compose.ui.graphics.Color(0xFFE5534B) else ring, (if (i == 0) 2.6f else 1.6f).dp.toPx(), o)
                    }
                    // الإبرة إلى الكعبة، ورأسها الكعبة
                    val a = needle.toDouble() * kotlin.math.PI / 180
                    val tip = androidx.compose.ui.geometry.Offset((c.x + kotlin.math.sin(a) * (r - 13.dp.toPx())).toFloat(), (c.y - kotlin.math.cos(a) * (r - 13.dp.toPx())).toFloat())
                    drawLine(gold, c, tip, 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawCircle(gold, 3.dp.toPx(), c)
                    val k = 5.dp.toPx()
                    drawRect(androidx.compose.ui.graphics.Color(0xFF111111), androidx.compose.ui.geometry.Offset(tip.x - k, tip.y - k), androidx.compose.ui.geometry.Size(2 * k, 2 * k))
                    drawRect(gold, androidx.compose.ui.geometry.Offset(tip.x - k, tip.y - k * 0.45f), androidx.compose.ui.geometry.Size(2 * k, k * 0.35f))
                }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (aligned) s.facingQibla else s.qiblaTitle, fontWeight = FontWeight.Bold,
                    color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(
                    "${Fmt.num(qibla, 0)}° · ${s.towards(dirs)} · ${Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0)} ${s.unitKm}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
