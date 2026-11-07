package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.ui.graphics.luminance
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.i18n.time
import kotlin.time.Duration.Companion.seconds
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
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
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.SizeClass
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.widthClassOf
import androidx.compose.foundation.layout.BoxWithConstraints
import com.osatks.mizan.ui.GlassTopBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.osatks.mizan.ui.quran.ContinueCard
import com.osatks.mizan.ui.quran.quranStyle
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
import kotlinx.datetime.LocalDate

/** ترتيب الأدوات وأيقوناتها (تشترك فيها الرئيسية والبحث ورؤوس الصفحات) */
val TOOL_ORDER = listOf("zakat", "inheritance", "prayer", "qibla", "adhkar", "hijri", "events", "tasbih", "fitr", "age", "fidya")
val TOOL_ICONS: Map<String, ImageVector> = mapOf(
    "zakat" to Icons.Filled.AccountBalanceWallet,
    "inheritance" to Icons.Filled.Groups,
    "prayer" to Icons.Filled.Schedule,
    "qibla" to Icons.Filled.Explore,
    "hijri" to Icons.Filled.DateRange,
    "events" to Icons.Filled.Event,
    "tasbih" to Icons.Filled.TouchApp,
    "adhkar" to Icons.Filled.AutoAwesome,
    "fitr" to Icons.Filled.Grain,
    "age" to Icons.Filled.Cake,
    "fidya" to Icons.Filled.VolunteerActivism,
)

@Composable
fun HomeScreen(open: (String) -> Unit) {
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = com.osatks.mizan.i18n.HomeText.of(lang.code)
    var next by remember { mutableStateOf(PrayerUtil.next()) }
    var now by remember { mutableStateOf(PrayerUtil.now()) }
    var showLang by remember { mutableStateOf(false) }
    // العدّ كل ثانية والصفحة ظاهرة فقط (تبقى حية تحت غيرها في iOS)
    val active = com.osatks.mizan.ui.LocalPageActive.current
    LaunchedEffect(AppSettings.place, AppSettings.method, AppSettings.hanafi, active) {
        while (true) { next = PrayerUtil.next(); now = PrayerUtil.now(); if (!active) break; delay(1000) }
    }
    val today = now.date
    val times = remember(today, AppSettings.place, AppSettings.method, AppSettings.hanafi) { PrayerUtil.times(today) }
    val reveal = com.osatks.mizan.ui.rememberRevealState()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val haze = rememberHazeState()
    val lead = LocalLeadingInset.current
    BoxWithConstraints(Modifier.fillMaxSize()) { androidx.compose.runtime.key(com.osatks.mizan.ui.theme.LocalThemeKey.current) {
    // المحتوى في الوسط بعرض أقصاه 1040 (والأدوات في تبويبها: ToolsScreen)
    val g = gutters(maxWidth, HomeMax, lead)
    val list = rememberLazyListState()
    val pad = Modifier.padding(start = g.start, end = g.end)
    LazyColumn(state = list, modifier = Modifier.fillMaxSize().hazeSource(haze), contentPadding = PaddingValues(bottom = 20.dp + bottom + LocalBottomInset.current)) {
        item {
            HomeHeader(times, next, now, g.start, g.end, reveal, open, onLanguage = { showLang = true })
        }
        if (!AppSettings.placeSet) item {
            // قبل اختيار الموقع: نطلبه بدل عرض مواقيت مكة على أنها مواقيت المستخدم
            LocationNeededCard(pad.padding(top = 16.dp))
        }
        item {
            SectionHeader(
                com.osatks.mizan.widget.WidgetText.of(lang.code).prayerTimes, pad.padding(top = 22.dp).reveal(reveal, 4),
                trailing = s.placeName(AppSettings.place), trailingIcon = Icons.Outlined.LocationOn,
            ) { open("prayer") }
        }
        item { PrayerGrid(times, next, now, pad.reveal(reveal, 5)) { open("prayer") } }
        item { SectionHeader(ht.daily, pad.padding(top = 22.dp).reveal(reveal, 6)) }
        item { DailyRow(pad.reveal(reveal, 7), open) }
        item {
            // القرآن الكريم: متابعة القراءة من آخر موضع، أو البدء
            Box(pad.padding(top = 18.dp).reveal(reveal, 8)) {
                if (QuranPrefs.last >= 0) ContinueCard(QuranPrefs.last, open) else QuranStartCard { open("quran") }
            }
        }
        item { AyahOfDayCard(today, pad.padding(top = 14.dp).reveal(reveal, 9), open) }
        item { HadithOfDayCard(today, pad.padding(top = 14.dp).reveal(reveal, 9), open) }
    }
    GlassTopBar(list, haze, s.appName, onBack = null)
} }
    if (showLang) LanguageSheet { showLang = false }
}

/**
 * تبويب الأدوات: الحاسبات والتواريخ والمسبحة والأذكار والقبلة في شبكة، عمودان على الهاتف وثلاثة في العرض المتوسط
 * وأربعة في الواسع
 */
@Composable
fun ToolsScreen(open: (String) -> Unit) {
    val s = LocalStrings.current
    com.osatks.mizan.ui.ToolPage(s.toolsTitle, s.appTagline) {
        item(key = "tools") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cols = when { maxWidth >= 840.dp -> 4; maxWidth >= 560.dp -> 3; else -> 2 }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TOOL_ORDER.chunked(cols).forEach { chunk ->
                        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { key -> ToolTile(key, Modifier.weight(1f)) { open(key) } }
                            repeat(cols - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/** أقصى عرض لمحتوى الرئيسية على الشاشات العريضة */
private val HomeMax = 1040.dp

/** وقت الصلاة [key] اليوم بالثواني من منتصف الليل */
private fun secOf(times: PrayerTimes, key: String): Double = times[key] * 3600

/**
 * ترويسة الرئيسية: خلفية فاتحة من لون التطبيق تذوب في الصفحة، والتحية بحسب الوقت، وزرّا البحث واللغة،
 * وبطاقة اليوم (اسم اليوم بخط المصحف، والتاريخان الهجري والميلادي)، وبجانبها أذكار الوقت بلمسة، والصلاة القادمة
 */
@Composable
private fun HomeHeader(
    times: PrayerTimes, next: NextPrayer, now: kotlinx.datetime.LocalDateTime,
    start: androidx.compose.ui.unit.Dp, end: androidx.compose.ui.unit.Dp,
    reveal: RevealState, open: (String) -> Unit, onLanguage: () -> Unit,
) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = com.osatks.mizan.i18n.HomeText.of(lang.code)
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(cs.primaryContainer, cs.primaryContainer.copy(alpha = .55f), cs.background)))
            .starBackdrop(cs.primary.copy(alpha = .07f), 150.dp, Alignment.TopStart, (-70).dp, (-40).dp, 90_000, stroke = 5f)
    ) {
        Column(Modifier.statusBarsPadding().padding(start = start + 4.dp, end = end + 4.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.reveal(reveal, 0)) {
                Column(Modifier.weight(1f)) {
                    Text(s.appName, color = cs.primary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.appTagline, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                RoundButton(Icons.Filled.Search, s.searchHint, Modifier.sharedCard("search-bar", CircleShape)) { open("search") }
                Spacer(Modifier.width(10.dp))
                RoundButton(Icons.Filled.Language, s.language) { onLanguage() }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.reveal(reveal, 1)) {
                DayCard(now.date, Modifier.weight(1f))
                Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NowAdhkarButton(now, times, open)
                    NextChip(next)
                }
            }
        }
    }
}

/** زر دائري شفاف في الترويسة */
@Composable
private fun RoundButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .85f),
        shadowElevation = 2.dp, interactionSource = src, modifier = modifier.size(46.dp).pressScale(src, 0.9f),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
    }
}

/** بطاقة اليوم: اسم اليوم بخط المصحف (بالعربية)، وتحته الهجري والميلادي: الشهر، فاليوم كبيراً، فالسنة */
@Composable
private fun DayCard(date: LocalDate, modifier: Modifier) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val cs = MaterialTheme.colorScheme
    val h = Hijri.toHijri(date)
    Surface(shape = RoundedCornerShape(22.dp), color = cs.surface, shadowElevation = 3.dp, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val weekday = s.weekday(date)
            if (lang.code == "ar") Text(weekday, style = quranStyle(30), color = cs.primary, maxLines = 1)
            else Text(weekday, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(cs.outlineVariant))
            Row(Modifier.height(IntrinsicSize.Min)) {
                DateColumn(h?.let { s.hijriMonths[it.m - 1] } ?: "", h?.d?.toString() ?: "", h?.y?.toString() ?: "", Modifier.weight(1f))
                Box(Modifier.width(1.dp).fillMaxHeight().background(cs.outlineVariant))
                DateColumn(s.gregMonths[date.monthNumber - 1], date.dayOfMonth.toString(), date.year.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DateColumn(month: String, day: String, year: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(month, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(day, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
        Text(year, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
    }
}

/** أذكار الوقت الآن (أداة «أذكار الوقت» نفسها): زر يفتح قسمها مباشرة */
@Composable
private fun NowAdhkarButton(now: kotlinx.datetime.LocalDateTime, times: PrayerTimes, open: (String) -> Unit) {
    val lang = LocalLang.current
    val window = remember(now.date, now.hour, now.minute / 10, AppSettings.place, AppSettings.method) {
        runCatching {
            com.osatks.mizan.widget.AdhkarClock.range(now.date.plusDays(-1), 2, { d -> PrayerUtil.times(d) }, { d -> Hijri.toHijri(d) })
                .firstOrNull { now >= it.start && now < it.end }
        }.getOrNull()
    } ?: return
    val title = remember(window, lang) {
        com.osatks.mizan.widget.WidgetData.title(window, lang, com.osatks.mizan.widget.WidgetText.of(lang.code), com.osatks.mizan.adhkar.Adhkar.tr(lang))
    }
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { com.osatks.mizan.widget.WidgetLaunch.adhkarSection = window.section; open("adhkar") },
        shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.97f),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** الصلاة القادمة والعدّ إليها في شريحة صغيرة */
@Composable
private fun NextChip(next: NextPrayer) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(cs.surface), contentAlignment = Alignment.Center) {
            Icon(PRAYER_ICONS[next.key] ?: Icons.Outlined.Schedule, null, tint = cs.primary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(s.prayer(next.key), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = .22f)).padding(horizontal = 9.dp, vertical = 4.dp)) {
            RollingText(PrayerUtil.hms(next.remaining), MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), cs.onSurface)
        }
    }
}

/** رموز الأوقات: الفجر نجوم، والشروق والمغرب أفق، والظهر شمس، والعصر شمس مائلة، والعشاء قمر */
private val PRAYER_ICONS: Map<String, ImageVector> = mapOf(
    "fajr" to Icons.Outlined.AutoAwesome,
    "sunrise" to Icons.Outlined.WbTwilight,
    "dhuhr" to Icons.Outlined.WbSunny,
    "asr" to Icons.Outlined.LightMode,
    "maghrib" to Icons.Outlined.WbTwilight,
    "isha" to Icons.Outlined.NightsStay,
)

/** عنوان قسم، وفي نهايته معلومة صغيرة (الموقع) إن وُجدت */
@Composable
private fun SectionHeader(title: String, modifier: Modifier, trailing: String? = null, trailingIcon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = cs.primary, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(trailing, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (trailingIcon != null) { Spacer(Modifier.width(4.dp)); Icon(trailingIcon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

/**
 * مواقيت اليوم: ستة أوقات في شبكة، والصلاة الحاضرة مظللة بلون التطبيق ومعها ما مضى على أذانها، والقادمة بحدّ ذهبي
 * ومعها العدّ إليها
 */
@Composable
private fun PrayerGrid(times: PrayerTimes, next: NextPrayer, now: kotlinx.datetime.LocalDateTime, modifier: Modifier, onClick: () -> Unit) {
    val s = LocalStrings.current
    val ht = com.osatks.mizan.i18n.HomeText.of(LocalLang.current.code)
    val sec = now.time.toSecondOfDay().toDouble()
    val keys = PrayTimes.PRAYERS.filter { !times[it].isNaN() }
    val current = keys.lastOrNull { it != "sunrise" && secOf(times, it) <= sec }?.takeIf { cur ->
        // الحاضرة حتى الوقت الذي بعدها (الشروق ينهي الفجر)
        val after = keys.getOrNull(keys.indexOf(cur) + 1)
        after == null || sec < secOf(times, after)
    }
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                row.forEach { k ->
                    val isCur = k == current
                    val isNext = k == next.key && next.at.date == now.date
                    val extra = when {
                        isCur -> ht.sinceAdhan to PrayerUtil.hms((sec - secOf(times, k)).toLong().seconds)
                        isNext -> null to PrayerUtil.hms(next.remaining)
                        else -> null
                    }
                    PrayerCell(k, s.prayer(k), s.time(times[k]), isCur, isNext, extra, Modifier.weight(1f).fillMaxHeight(), onClick)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PrayerCell(
    key: String, name: String, time: String, current: Boolean, next: Boolean, extra: Pair<String?, String>?,
    modifier: Modifier, onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (current) cs.primaryContainer else cs.surface
    val fg = if (current) cs.onPrimaryContainer else cs.onSurface
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = bg,
        border = if (next) BorderStroke(1.5.dp, Gold) else BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        shadowElevation = if (current) 2.dp else 0.dp, interactionSource = src, modifier = modifier.pressScale(src, 0.96f),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(PRAYER_ICONS[key] ?: Icons.Outlined.Schedule, null, tint = if (current || next) Gold else cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = .8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
            if (extra != null) {
                Spacer(Modifier.height(4.dp))
                extra.first?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(extra.second, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (current) cs.primary else GoldDeep, maxLines = 1)
            }
        }
    }
}

/** أدوات كل يوم في دوائر ملوّنة هادئة: الأذكار، والمسبحة، والقبلة، والمواقيت */
@Composable
private fun DailyRow(modifier: Modifier, open: (String) -> Unit) {
    val s = LocalStrings.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val items = listOf(
        Triple("adhkar", Color(0xFFDDEFE3), Color(0xFF26443A)),
        Triple("tasbih", Color(0xFFF1EBDD), Color(0xFF4A4232)),
        Triple("qibla", Color(0xFFE9E3F3), Color(0xFF3E3550)),
        Triple("prayer", Color(0xFFFBE3E3), Color(0xFF523234)),
    )
    Row(modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        items.forEach { (key, light, deep) ->
            val src = remember { MutableInteractionSource() }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable(interactionSource = src, indication = null) { open(key) }.pressScale(src, 0.92f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(66.dp).clip(CircleShape).background(if (dark) deep else light), contentAlignment = Alignment.Center) {
                    Icon(TOOL_ICONS.getValue(key), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(s.tools.getValue(key).name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** آية اليوم (آية الأدوات نفسها) بخط المصحف ومعناها بلغة الواجهة؛ لمسها يفتح تفسيرها */
@Composable
private fun AyahOfDayCard(today: LocalDate, modifier: Modifier, open: (String) -> Unit) {
    val s = LocalStrings.current
    val ht = com.osatks.mizan.i18n.HomeText.of(LocalLang.current.code)
    val cs = MaterialTheme.colorScheme
    val q by androidx.compose.runtime.produceState<Array<String>?>(null) { value = runCatching { com.osatks.mizan.quran.QuranData.uthmani() }.getOrNull() }
    val text = q ?: return
    val idx = remember(text, today) { com.osatks.mizan.quran.ayahOfDay(text, today) }
    val (sn, an) = remember(idx) { com.osatks.mizan.quran.QuranMeta.locate(idx) }
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(com.osatks.mizan.ui.quran.surahRoute(sn, an, "tafsir")) },
        shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        interactionSource = src, modifier = modifier.fillMaxWidth().pressScale(src, 0.98f),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(s.ayahOfDay, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cs.primary, modifier = Modifier.weight(1f))
                Text(ht.tafsir, style = MaterialTheme.typography.labelLarge, color = GoldDeep, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                com.osatks.mizan.quran.QuranData.withoutBismillah(text, idx), style = quranStyle(22, androidx.compose.ui.text.style.TextAlign.Center),
                color = cs.onSurface, modifier = Modifier.fillMaxWidth(), maxLines = 6, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(s.surahAyah(s.surahName(sn), an), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** حديث اليوم (حديث الأداة نفسه): طرفه بالعربية وترجمته بلغة الواجهة؛ لمسه يفتح الحديث وشرحه */
@Composable
private fun HadithOfDayCard(today: LocalDate, modifier: Modifier, open: (String) -> Unit) {
    val lang = LocalLang.current
    val ht = com.osatks.mizan.i18n.HomeText.of(lang.code)
    val cs = MaterialTheme.colorScheme
    val h by androidx.compose.runtime.produceState<com.osatks.mizan.hadith.Hadith?>(null, today, lang) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            runCatching { com.osatks.mizan.hadith.Hadiths.forDay(today, lang.code) }.getOrNull()
        }
    }
    val hadith = h ?: return
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open("hadith") },
        shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        interactionSource = src, modifier = modifier.fillMaxWidth().pressScale(src, 0.98f),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FormatQuote, null, tint = Gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ht.hadithOfDay, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cs.primary, modifier = Modifier.weight(1f))
                Text(ht.explanation, style = MaterialTheme.typography.labelLarge, color = GoldDeep, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "«${hadith.ar.title}»",
                style = MaterialTheme.typography.titleMedium.copy(
                    textDirection = androidx.compose.ui.text.style.TextDirection.Rtl, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 30.sp,
                ),
                color = cs.onSurface, modifier = Modifier.fillMaxWidth(), maxLines = 4, overflow = TextOverflow.Ellipsis,
            )
            hadith.tr?.let { tr ->
                Spacer(Modifier.height(6.dp))
                Text(
                    tr.title, style = MaterialTheme.typography.bodyMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    color = cs.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            val main = hadith.tr ?: hadith.ar
            Text(
                listOf(main.grade, main.takhrij).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
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

/** بطاقة القرآن الكريم قبل أول قراءة؛ تتحول إلى رأس صفحة المصحف */
@Composable
private fun QuranStartCard(onClick: () -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(22.dp), color = Color.Transparent, interactionSource = src,
        modifier = Modifier.fillMaxWidth().sharedCard("card-quran", RoundedCornerShape(22.dp)).pressScale(src, 0.97f)
    ) {
        Row(
            Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .07f), 60.dp, Alignment.CenterEnd, (-30).dp, 0.dp, 90_000).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.sharedIcon("icon-quran").size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Gold) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.quranTitle, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(s.startReading, color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text("ٱلْقُرْءَانُ", color = Color.White, style = quranStyle(26))
        }
    }
}
