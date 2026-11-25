package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.DayMark
import com.osatks.mizan.core.FastCalendar
import com.osatks.mizan.core.Fasting
import com.osatks.mizan.core.FastingState
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.HDate
import com.osatks.mizan.core.PrayerTimes
import com.osatks.mizan.core.SunnahDay
import com.osatks.mizan.core.daysBetween
import com.osatks.mizan.core.durationBetween
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.hijri
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberNotifyToggle
import com.osatks.mizan.ui.FormStepperControl
import com.osatks.mizan.ui.FormSwitchRow
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.Tile
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import com.osatks.mizan.widget.LiveText
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** لون أيام الإفطار في الشبكة (كشارة «دون النصاب» في ResultCard) */
private val MissedColor = Color(0xFFD9694F)

/**
 * متابعة الصيام: بطاقة اليوم (العدّ إلى نهاية السحور وإلى الإفطار حين يصوم المستخدم أو في رمضان)، وشبكة رمضان الحالي
 * أو الأخير، وأيام القضاء وسجلها، وأيام التطوع القادمة وتذكيرها — كما في iPhone (NativeFasting.swift).
 * الحال كلها في [Fasting] (المفاتيح fast_* في الإعدادات).
 */
@Composable
fun FastingScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val code = LocalLang.current.code
    remember { Fasting.ensure() }
    val st = Fasting.state
    // الوقت بالموقع المختار، كل ثانية والصفحة ظاهرة (للعدّ إلى السحور والإفطار)
    var now by remember { mutableStateOf(Fasting.now()) }
    val active = LocalPageActive.current
    LaunchedEffect(active) { while (true) { now = Fasting.now(); if (!active) break; delay(1000L) } }
    val today = now.date
    val offset = Fasting.offset()
    val place = AppSettings.place
    val method = AppSettings.method
    val hanafi = AppSettings.hanafi
    val times = remember(today, place, method, hanafi) { PrayerUtil.times(today) }
    val hToday = remember(today, offset) { FastCalendar.hijri(today, offset) }
    val year = remember(today, offset) { FastCalendar.ramadanYear(today, offset) }
    val upcoming = remember(today, offset, st.sunnah) { Fasting.upcoming(8) }
    val fasting = remember(today, st) { Fasting.fastingToday() }
    val shawwal = remember(hToday, st.sunnah) { hToday?.takeIf { it.m == 10 }?.let { Fasting.shawwalDone(it.y) } ?: -1 }

    // التذكير يطلب إذن الإشعارات أولاً (أندرويد 13 فأحدث)، ثم تُعاد جدولة التنبيهات
    val askNotify = rememberNotifyToggle { ok ->
        Fasting.setRemind(ok)
        PrayerAlerts.reschedule()
        if (!ok) Platform.toast(s.notifyOffTitle)
    }
    val setRemind: (Boolean) -> Unit = { on ->
        if (on) askNotify(true) else { Fasting.setRemind(false); PrayerAlerts.reschedule() }
    }

    ToolPage(s.fastingTitle, s.fastingSubtitle, onBack, sharedKey = "fasting", icon = TOOL_ICONS["fasting"]) {
        if (fasting) item(key = "hero") { FastingHero(s, LiveText.of(code), times, today, now) }
        item(key = "today") { TodayCard(s, today, hToday, st, upcoming.firstOrNull()?.takeIf { it.date == today }) }
        if (year != null) item(key = "ramadan") { RamadanCard(s, year, offset, today, st) }
        side(key = "qada") { QadaCard(s, st) }
        side(key = "sunnah") { SunnahCard(s, st, upcoming, today, shawwal, setRemind) }
        item(key = "note") { Note(s.eventsNote) }
    }
}

/** بطاقة الصائم بتدرج لون التطبيق: العدّ إلى نهاية السحور (الفجر) ثم إلى الإفطار (المغرب) بمواقيت الموقع المختار */
@Composable
private fun FastingHero(s: Strings, lt: LiveText, t: PrayerTimes, today: LocalDate, now: LocalDateTime) {
    val fajr = PrayerUtil.at(today, t["fajr"])
    val maghrib = PrayerUtil.at(today, t["maghrib"])
    val fajrOk = !t["fajr"].isNaN()
    val maghribOk = !t["maghrib"].isNaN()
    val (label, value) = when {
        fajrOk && now < fajr -> lt.suhoor to PrayerUtil.hms(durationBetween(now, fajr))
        maghribOk && now < maghrib -> lt.iftar to PrayerUtil.hms(durationBetween(now, maghrib))
        else -> lt.iftar to lt.iftarNow
    }
    ResultCard(label, value, badge = s.fastingNow, badgeOk = true) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroTile(lt.suhoor, if (fajrOk) s.time(t["fajr"]) else "—", done = fajrOk && now >= fajr)
            HeroTile(lt.iftar, if (maghribOk) s.time(t["maghrib"]) else "—", done = maghribOk && now >= maghrib)
        }
        Text(s.placeName(AppSettings.place), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** خانة وقت بيضاء على خلفية البطاقة (نهاية السحور أو الإفطار)، بعلامة صح بعد مضيّه */
@Composable
private fun RowScope.HeroTile(label: String, time: String, done: Boolean) {
    Surface(Modifier.weight(1f).semantics(mergeDescendants = true) {}, shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .12f)) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                if (done) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Gold)
                }
            }
        }
    }
}

/** اليوم: التاريخ الهجري (بفرق المستخدم) وأسباب التطوع فيه، ومفتاح صيام تطوع اليوم، أو تنبيه يومَي العيد وأيام التشريق */
@Composable
private fun TodayCard(s: Strings, today: LocalDate, h: HDate?, st: FastingState, sunnah: SunnahDay?) {
    SectionCard(s.today) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            Text(s.hijri(h), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${s.weekday(today)} ${s.greg(today)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (sunnah != null) KindChips(s, sunnah)
        when {
            h != null && FastCalendar.forbidden(h) -> Note(s.fastingForbiddenToday, warn = true)
            // رمضان يُعلَّم في شبكته
            h?.m == 9 -> {}
            else -> {
                FormSwitchRow(s.fastingVoluntaryToday, today.toString() in st.sunnah) { Fasting.setVoluntaryToday(it) }
                Text(s.fastingTodayHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** أسباب صيام التطوع في يوم: كبسولات ذهبية فاتحة، تلتف إلى سطر ثانٍ إن طالت */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KindChips(s: Strings, d: SunnahDay) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        d.kinds.forEach { k ->
            Surface(shape = CircleShape, color = Gold.copy(alpha = .18f)) {
                Text(
                    s.fastingKinds[k.key] ?: k.key, Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** رمضان الحالي أو الأخير: شبكة أيامه (اللمس يتنقل بين الصيام والإفطار وبلا تحديد)، وعدد كل حال */
@Composable
private fun RamadanCard(s: Strings, year: Int, offset: Int, today: LocalDate, st: FastingState) {
    val length = remember(year) { com.osatks.mizan.core.Hijri.monthLength(year, 9) }
    SectionCard(s.hijriMonthTitle(9, year)) {
        Text(s.fastingRamadanHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..length).chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { d ->
                        val date = FastCalendar.gregorian(year, 9, d, offset)
                        DayCell(s, year, d, st.mark(year, d), enabled = date != null && date <= today, isToday = date == today, Modifier.weight(1f))
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("${st.count(year, DayMark.FASTED, length)}", s.fastingFasted)
            Tile("${st.count(year, DayMark.MISSED, length)}", s.fastingMissed)
            Tile("${st.count(year, DayMark.NONE, length)}", s.fastingUnmarked)
        }
    }
}

@Composable
private fun DayCell(s: Strings, year: Int, d: Int, mark: DayMark, enabled: Boolean, isToday: Boolean, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        when (mark) { DayMark.FASTED -> cs.primary; DayMark.MISSED -> MissedColor; DayMark.NONE -> cs.surfaceVariant.copy(alpha = .7f) },
        tween(200), label = "day",
    )
    val fg = if (mark == DayMark.NONE) cs.onSurfaceVariant else Color.White
    val state = when (mark) { DayMark.FASTED -> s.fastingFasted; DayMark.MISSED -> s.fastingMissed; DayMark.NONE -> s.fastingUnmarked }
    val src = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.aspectRatio(1f).alpha(if (enabled) 1f else .45f).pressScale(src, 0.9f).clip(shape).background(bg)
            .then(if (isToday) Modifier.border(BorderStroke(2.dp, Gold), shape) else Modifier)
            .clickable(enabled = enabled, interactionSource = src, indication = null, role = Role.Button) { Fasting.cycle(year, d) }
            .semantics {
                contentDescription = s.hijriDate(d, 9, year)
                stateDescription = state
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("$d", color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr))
    }
}

/** أيام القضاء: العدد بزر − و +، وزر «قضيت يوماً»، وآخر ما سُجّل */
@Composable
private fun QadaCard(s: Strings, st: FastingState) {
    SectionCard(s.fastingQadaTitle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                RollingText(Fmt.int(st.qada.toLong()), MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.Black), MaterialTheme.colorScheme.primary)
                Text(s.fastingQadaOwed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FormStepperControl(st.qada, { v ->
                when {
                    v > st.qada -> Fasting.qadaPlus()
                    v < st.qada -> Fasting.qadaMinus()
                    else -> {}
                }
            }, 0, Fasting.MAX_QADA)
        }
        MadeUpButton(s.fastingMadeUp, enabled = st.qada > 0) { Fasting.madeUp() }
        Text(s.fastingHistory, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() })
        if (st.qadaLog.isEmpty()) Text(s.fastingNothingYet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        st.qadaLog.take(6).forEach { e ->
            val label = when (e.kind) {
                "add" -> s.fastingLogAdded
                "done" -> s.fastingLogMadeUp
                "missed" -> s.fastingLogMissed
                else -> s.fastingLogRemoved
            }
            val date = runCatching { LocalDate.parse(e.date) }.getOrNull()
            StatRow(if (date != null) "$label · ${s.greg(date)}" else label, if (e.delta > 0) "+${e.delta}" else "${e.delta}")
        }
    }
}

/** زر بتدرج لون التطبيق وعلامة صح (كزر المشاركة في الحاسبات) */
@Composable
private fun MadeUpButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, enabled = enabled, shape = CircleShape, color = Color.Transparent, interactionSource = src,
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else .45f).pressScale(src, 0.97f),
    ) {
        Row(
            Modifier.background(HeroBrush).padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

/** صيام التطوع: ما سُجّل، والست من شوال في شوال، والأيام القادمة بأسبابها، والتذكير */
@Composable
private fun SunnahCard(s: Strings, st: FastingState, upcoming: List<SunnahDay>, today: LocalDate, shawwal: Int, setRemind: (Boolean) -> Unit) {
    SectionCard(s.fastingSunnahTitle) {
        StatRow(s.fastingLogged, s.daysCount(st.sunnah.size.toLong()))
        if (shawwal >= 0) StatRow(s.fastingShawwal(shawwal.coerceAtMost(6)), if (shawwal >= 6) "✓" else "", strong = true)
        Text(s.fastingUpcoming, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() })
        upcoming.forEach { d -> UpcomingRow(s, d, today) }
        FormSwitchRow(s.fastingRemind, st.remind, setRemind)
        Text(s.fastingRemindDesc(s.time(Fasting.REMIND_HOUR.toDouble())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** يوم تطوع قادم: أسبابه، ويومه وتاريخاه، وبعد كم يوم (كقائمة المناسبات) */
@Composable
private fun UpcomingRow(s: Strings, d: SunnahDay, today: LocalDate) {
    val days = daysBetween(today, d.date)
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(d.kinds.joinToString(" · ") { s.fastingKinds[it.key] ?: it.key }, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${s.weekday(d.date)} ${s.greg(d.date)} · ${s.hijri(d.hijri)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                when (days) { 0L -> s.today; 1L -> s.tomorrow; else -> s.inDays(days) },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End,
            )
        }
    }
}
