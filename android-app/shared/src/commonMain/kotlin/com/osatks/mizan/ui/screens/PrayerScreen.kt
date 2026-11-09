package com.osatks.mizan.ui.screens

import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.osatks.mizan.ui.MiniStepper
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.starBackdrop
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.HeroBrush
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

/** مواقيت الصلاة بشاشة Compose (أندرويد، و iOS قبل اختيار الموقع) */
@Composable
fun PrayerScreen(open: (String) -> Unit = {}) = PrayerScreen(open, nativeView = null)

/** واجهة النظام لشاشة المواقيت (iOS): حالة الموقع وأفعاله من [PrayerScreen] */
internal typealias NativePrayerSlot = @Composable (locating: Boolean, onLocate: () -> Unit, onPickCity: () -> Unit, onNotify: (Boolean) -> Unit) -> Unit

/**
 * مواقيت الصلاة؛ [nativeView] في iOS: الشاشة بواجهة النظام متى اختير الموقع (PlatformRoute.ios.kt)، والحالة هنا لها
 * وللشاشة قبلها (طلب الموقع واختيار المدينة)، فلا تضيع حين تتبدل إحداهما بالأخرى
 */
@Composable
internal fun PrayerScreen(open: (String) -> Unit, nativeView: NativePrayerSlot?) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var now by remember { mutableStateOf(PrayerUtil.now()) }
    // اليوم المختار وجدول الشهر يبقيان بعد تبديل التبويب أو الرجوع من إعدادات الأذان
    var day by androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver({ it.value.toEpochDays() }, { mutableStateOf(LocalDate.fromEpochDays(it)) })
    ) { mutableStateOf(PrayerUtil.now().toLocalDate()) }
    var showMonth by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    var locationDenied by remember { mutableStateOf(false) }
    val place = AppSettings.place

    // في iOS: الشاشة بواجهة النظام (SwiftUI) متى اختير الموقع، وعدّادها في Swift
    val native = nativeView != null && AppSettings.placeSet
    // العدّ كل ثانية والصفحة ظاهرة فقط (تبقى حية تحت غيرها في iOS)
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(place, AppSettings.method, AppSettings.hanafi, active, native) {
        while (true) { next = PrayerUtil.next(); now = PrayerUtil.now(); if (!active || native) break; delay(1000) }
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

    if (native) {
        nativeView?.invoke(locating, { locating = true; locate() }, { pickCity = true }, { toggleNotify(it) })
        if (pickCity) CityPickerSheet(place.key.takeIf { !place.gps }, onDismiss = { pickCity = false }) { c ->
            AppSettings.updatePlace(Place.of(c))
            PrayerAlerts.reschedule()
        }
        return
    }

    val city = place.city
    val subtitle = s.placeName(place) + (city?.let { " · " + s.country(it.cc, lang) } ?: "")
    // على الشاشة الواسعة: مواقيت اليوم وجدول الشهر متجاوران، والجدول ظاهر دائماً يتبع اليوم المختار
    ToolPage(s.prayerTitle, subtitle, sharedKey = "prayer", icon = TOOL_ICONS["prayer"], plainIcon = true) {
        if (!AppSettings.placeSet) item { LocationNeededCard() }
        item { PrayerNextHero(next) }
        item {
            PrayerCard {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                // اليوم المختار بين سهمين دائريين كما في iPhone
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PrayerArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, s.prevDay) { day = day.minusDays(1) }
                    Column(Modifier.weight(1f).padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${s.weekday(day)} ${s.greg(day)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.hijri(Hijri.toHijri(day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    PrayerArrowButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.nextDay) { day = day.plusDays(1) }
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
                ) { d -> DayTimes(d, next, now) }
                Text(s.prayerEndsNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val isToday = day == now.date
                val showMonthPill = LocalPageColumns.current == 1
                if (!isToday || showMonthPill) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // «اليوم» حين يكون المختار غيره
                    if (!isToday) PrayerPill(s.today) { day = PrayerUtil.now().toLocalDate() }
                    if (showMonthPill) PrayerPill(if (showMonth) s.hideMonthTable else s.monthTable) { showMonth = !showMonth }
                }
            }
        }
        // القبلة مع المواقيت كما هو المعتاد: بوصلة صغيرة حية، والزاوية والجهة والمسافة، وتفتح شاشة القبلة
        item { QiblaCard(onOpen = { open("qibla") }) }
        side { Reveal(showMonth || LocalPageColumns.current == 2) { MonthTable(day) } }
        item {
            PrayerCard(s.locationAndMethod) {
                PrayerLocateButton(locating) { locating = true; locate() }
                Text(s.orPickCity, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PickerField(
                    if (place.gps) "📍 ${s.placeName(place)}" else s.placeName(place),
                    Icons.Filled.LocationOn
                ) { pickCity = true }
                Picker(s.calcMethod, PrayTimes.METHODS.map { it.key }, AppSettings.method, text = { s.methods[it] ?: it },
                    onSelect = { AppSettings.updateMethod(it); PrayerAlerts.reschedule() })
                Text(s.asrMethod, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmented(listOf("std" to s.asrStandard, "hanafi" to s.asrHanafi), if (AppSettings.hanafi) "hanafi" else "std") {
                    AppSettings.updateHanafi(it == "hanafi"); PrayerAlerts.reschedule()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.latLng, Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text("${Fmt.num(place.lat)}° / ${Fmt.num(place.lng)}°", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        side {
            // الإقامة: دقائق بعد الأذان لكل صلاة كما في مسجد المستخدم
            PrayerCard(s.iqamaTitle) {
                Text(s.iqamaDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
                            Icon(PRAYER_ICONS[k] ?: Icons.Outlined.Schedule, null, tint = Gold, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(s.prayer(k), Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        MiniStepper(AppSettings.iqama[k] ?: 0, { AppSettings.updateIqama(k, it) }, min = 0, max = 90)
                    }
                }
            }
        }
        side {
            PrayerCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.adhanAlert, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.adhanAlertDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.osatks.mizan.ui.AppSwitch(checked = AppSettings.notify, onCheckedChange = { on -> toggleNotify(on) })
                }
                // الأذان والتنبيهات: المؤذن، وطريقة التنبيه لكل صلاة، والاهتزاز
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = { open("adhan") }).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.adhanSettings, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.adhanSettingsSub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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

/** بطاقة بيضاء بحافة رفيعة كبطاقات الرئيسية في iPhone، وعنوانها (إن وُجد) بلون التطبيق */
@Composable
private fun PrayerCard(title: String? = null, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(
                title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

/** الصلاة القادمة في بطاقة بتدرج لون التطبيق (كبطاقة القرآن في الرئيسية)، والعدّ إليها كل ثانية */
@Composable
private fun PrayerNextHero(next: NextPrayer) {
    val s = LocalStrings.current
    Surface(shape = RoundedCornerShape(22.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .07f), 70.dp, Alignment.CenterEnd, (-30).dp, 0.dp, 90_000)
                .padding(18.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(PRAYER_ICONS[next.key] ?: Icons.Outlined.Schedule, null, tint = Gold, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(s.nextPrayerLabel(s.prayer(next.key)), color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // العدّاد بأرقام من اليسار دائماً
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    RollingText(PrayerUtil.hms(next.remaining), TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp), Color.White)
                }
                Text(s.atTime(s.time(next.at.hour + next.at.minute / 60.0)), color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
            }
        }
    }
}

/** سهم اليوم السابق أو التالي في دائرة بلون التطبيق الفاتح */
@Composable
private fun PrayerArrowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
        interactionSource = src, modifier = Modifier.size(38.dp).pressScale(src, 0.9f),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
    }
}

/** زر صغير على هيئة كبسولة («اليوم» وجدول الشهر) */
@Composable
private fun PrayerPill(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
        interactionSource = src, modifier = Modifier.pressScale(src, 0.95f),
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

/** زر تحديد الموقع: كبسولة بتدرج لون التطبيق */
@Composable
private fun PrayerLocateButton(locating: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, enabled = !locating, shape = CircleShape, color = Color.Transparent,
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.97f),
    ) {
        Row(
            Modifier.background(HeroBrush).padding(vertical = 13.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else Icon(Icons.Filled.MyLocation, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (locating) s.locating else s.useMyLocation, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * صلوات يوم واحد كما في iPhone: الحاضرة مظللة بلون التطبيق ومعها ما مضى على أذانها، والقادمة بحدّ ذهبي ومعها العدّ إليها،
 * ونهاية كل وقت وإقامته
 */
@Composable
private fun DayTimes(day: LocalDate, next: NextPrayer, now: kotlinx.datetime.LocalDateTime) {
    val s = LocalStrings.current
    val ht = com.osatks.mizan.i18n.HomeText.of(LocalLang.current.code)
    val cs = MaterialTheme.colorScheme
    val t = PrayerUtil.times(day)
    val isToday = day == now.date
    val sec = now.time.toSecondOfDay().toDouble()
    val keys = PrayTimes.PRAYERS
    // الحاضرة حتى الوقت الذي بعدها (الشروق ينهي الفجر)
    val current = if (!isToday) null else keys.filter { !t[it].isNaN() }.let { valid ->
        valid.lastOrNull { it != "sunrise" && t[it] * 3600 <= sec }?.takeIf { cur ->
            val after = valid.getOrNull(valid.indexOf(cur) + 1)
            after == null || sec < t[after] * 3600
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { key ->
            val isCur = key == current
            val isNext = isToday && !isCur && key == next.key && next.at.toLocalDate() == day
            val sunrise = key == "sunrise"
            val fg = if (isCur) cs.onPrimaryContainer else cs.onSurface
            val bg by animateColorAsState(if (isCur) cs.primaryContainer else cs.background.copy(alpha = .6f), tween(Motion.LONG), label = "row")
            Surface(
                shape = RoundedCornerShape(16.dp), color = bg,
                border = if (isNext) BorderStroke(1.5.dp, Gold) else null,
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(if (isCur) cs.surface.copy(alpha = .7f) else cs.primaryContainer.copy(alpha = .6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(PRAYER_ICONS[key] ?: Icons.Outlined.Schedule, null, tint = if (isCur || isNext) Gold else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    // الاسم وتحته نهاية الوقت؛ الأذان وتحته ما مضى أو ما بقي أو الإقامة
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.prayer(key), fontWeight = FontWeight.Bold, fontSize = if (sunrise) 15.sp else 17.sp,
                            color = if (sunrise) cs.onSurfaceVariant else fg
                        )
                        val end = t.end(key)
                        if (!end.isNaN()) Text(s.endsAt(s.time(end)), style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = .7f))
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(s.time(t[key]), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = fg)
                        val extra = when {
                            isCur -> PrayerUtil.hms((sec - t[key] * 3600).toLong().seconds)
                            isNext -> PrayerUtil.hms(next.remaining)
                            else -> null
                        }
                        val iq = AppSettings.iqama[key] ?: 0
                        if (extra != null) Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCur) { Text(ht.sinceAdhan, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg.copy(alpha = .7f)); Spacer(Modifier.width(4.dp)) }
                            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                RollingText(extra, TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold), if (isCur) cs.primary else GoldDeep)
                            }
                        } else if (iq > 0 && !t[key].isNaN()) {
                            Text(s.iqamaAt(s.time(t[key] + iq / 60.0)), fontSize = 12.sp, color = cs.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthTable(ref: LocalDate) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val first = ref.withDayOfMonth(1)
    val today = PrayerUtil.now().toLocalDate()
    PrayerCard(s.monthTableTitle(ref.monthValue, ref.year)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                (listOf(s.colDay) + listOf("fajr", "dhuhr", "asr", "maghrib", "isha").map { s.prayer(it) }).forEach {
                    Text(it, Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            for (i in 0 until ref.lengthOfMonth()) {
                val d = first.plusDays(i.toLong())
                val t = PrayerUtil.times(d)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (d == today) cs.primaryContainer else Color.Transparent).padding(vertical = 5.dp)
                ) {
                    Text("${d.dayOfMonth}", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = cs.onSurface)
                    listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { k ->
                        Text(PrayTimes.format(t[k], true), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = cs.onSurface)
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
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onOpen, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = if (aligned) BorderStroke(1.5.dp, ring) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)),
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.98f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
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
                Text(if (aligned) s.facingQibla else s.qiblaTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(2.dp))
                Text(
                    "${Fmt.num(qibla, 0)}° · ${s.towards(dirs)} · ${Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0)} ${s.unitKm}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
