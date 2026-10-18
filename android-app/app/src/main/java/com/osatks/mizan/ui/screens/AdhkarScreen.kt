package com.osatks.mizan.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.AdhkarSection
import com.osatks.mizan.adhkar.AdhkarTr
import com.osatks.mizan.adhkar.Dhikr
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.QuranFont
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

private val SECTION_ICONS: Map<String, ImageVector> = mapOf(
    "sunrise" to Icons.Filled.WbSunny, "sunset" to Icons.Filled.WbTwilight, "sleep" to Icons.Filled.Bedtime,
    "wake" to Icons.Filled.Alarm, "prayer" to Icons.Filled.Spa, "mosque" to Icons.Filled.Mosque, "adhan" to Icons.Filled.Campaign,
    "home" to Icons.Filled.Home, "clothes" to Icons.Filled.Checkroom, "food" to Icons.Filled.Restaurant, "moon" to Icons.Filled.DarkMode,
    "travel" to Icons.Filled.Flight, "heart" to Icons.Filled.Favorite, "health" to Icons.Filled.HealthAndSafety,
    "funeral" to Icons.Filled.LocalFlorist, "weather" to Icons.Filled.Thunderstorm, "kaaba" to Icons.Filled.TravelExplore,
    "istikhara" to Icons.Filled.Lightbulb, "shield" to Icons.Filled.Shield, "people" to Icons.Filled.People,
    "quran" to Icons.AutoMirrored.Filled.MenuBook, "star" to Icons.Filled.AutoAwesome, "hands" to Icons.Filled.VolunteerActivism,
    "repeat" to Icons.Filled.Autorenew,
)

private fun sectionIcon(s: AdhkarSection) = SECTION_ICONS[s.icon] ?: Icons.Filled.AutoAwesome

/** القسم المناسب للوقت: الصباح حتى الظهر، والمساء من العصر، والنوم بعد العشاء، وفي رمضان أذكاره */
private fun suggested(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 4..11 -> "morning"
    in 15..20 -> "evening"
    in 21..23, in 0..1 -> "sleep"
    else -> if (Hijri.toHijri(LocalDate.now())?.m == 9) "ramadan" else "afterPrayer"
}

private fun doneKey(id: String) = "adhkar_done_$id"
private fun doneToday(id: String) = AppSettings.getInt(doneKey(id), -1) == LocalDate.now().toEpochDay().toInt()

/**
 * الأذكار: أقسامها أولاً (والمناسب للوقت في أعلاها)، ثم أذكار القسم المختار بعدّاد لكل ذكر،
 * وتحت كل ذكر ترجمته وزر «الدليل» الذي يبيّن مصدره ودرجته وراويه وفضله.
 */
@Composable
fun AdhkarScreen(onBack: () -> Unit, start: String = "") {
    var open by rememberSaveable { mutableStateOf(start) }
    BackHandler(open.isNotEmpty()) { open = "" }
    val sections by produceState(Adhkar.loaded()) { if (value == null) value = withContext(Dispatchers.IO) { Adhkar.sections() } }
    val list = sections
    AnimatedContent(
        open to (list != null), label = "adhkar",
        transitionSpec = { (fadeIn(tween(Motion.MEDIUM, 60)) + scaleIn(tween(Motion.MEDIUM), 0.97f)) togetherWith fadeOut(tween(Motion.SHORT)) },
    ) { (id, ready) ->
        val sec = if (ready) list!!.firstOrNull { it.id == id } else null
        if (sec != null) AdhkarSectionPage(sec) { open = "" } else AdhkarHome(list.orEmpty(), ready, onBack) { open = it }
    }
}

@Composable
private fun AdhkarHome(sections: List<AdhkarSection>, ready: Boolean, onBack: () -> Unit, pick: (String) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val tr = remember(lang) { Adhkar.tr(lang) }
    val tool = s.tools["adhkar"]
    val now = remember { suggested() }
    ToolPage(tool?.name ?: "", tool?.desc, onBack, sharedKey = "adhkar", icon = TOOL_ICONS["adhkar"], spacing = 12.dp) {
        if (!ready) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        sections.firstOrNull { it.id == now }?.let { sec -> item { NowCard(sec, tr, lang) { pick(sec.id) } } }
        sections.filter { it.id != now }.chunked(2).forEach { pair ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { sec -> SectionTile(sec, tr, lang, Modifier.weight(1f)) { pick(sec.id) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun AdhkarTr.title(sec: AdhkarSection, lang: Lang) = if (lang == Lang.AR) sec.titleAr else sections[sec.id]?.first ?: sec.titleAr
private fun AdhkarTr.desc(sec: AdhkarSection, lang: Lang) = if (lang == Lang.AR) sec.descAr else sections[sec.id]?.second ?: sec.descAr

@Composable
private fun NowCard(sec: AdhkarSection, tr: AdhkarTr, lang: Lang, onClick: () -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    Surface(onClick = onClick, shape = RoundedCornerShape(24.dp), color = Color.Transparent, interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.98f)) {
        Row(Modifier.background(HeroBrush).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                Icon(sectionIcon(sec), null, Modifier.size(30.dp), tint = Gold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(s.adhkarNow, style = MaterialTheme.typography.labelLarge, color = Gold, fontWeight = FontWeight.Bold)
                Text(tr.title(sec, lang), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(tr.desc(sec, lang) + " · " + s.dhikrItems(sec.items.size), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .8f))
            }
            if (doneToday(sec.id)) DoneBadge()
        }
    }
}

@Composable
private fun DoneBadge() {
    Box(Modifier.size(26.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Color(0xFF1E1A14))
    }
}

@Composable
private fun SectionTile(sec: AdhkarSection, tr: AdhkarTr, lang: Lang, modifier: Modifier, onClick: () -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f)), interactionSource = src,
        modifier = modifier.heightIn(min = 132.dp).pressScale(src, 0.97f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                    Icon(sectionIcon(sec), null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                if (doneToday(sec.id)) DoneBadge()
            }
            Spacer(Modifier.height(10.dp))
            Text(tr.title(sec, lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(s.dhikrItems(sec.items.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- أذكار القسم ----------

@Composable
internal fun AdhkarSectionPage(sec: AdhkarSection, onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val tr = remember(lang) { Adhkar.tr(lang) }
    val done = remember(sec.id) { mutableStateMapOf<String, Int>() }
    var evidence by remember { mutableStateOf<Dhikr?>(null) }
    val haptic = LocalHapticFeedback.current
    val total = sec.items.sumOf { it.count }
    val progress = sec.items.sumOf { (done[it.id] ?: 0).coerceAtMost(it.count) }
    val complete = progress >= total
    // ترجمة الآيات للأذكار القرآنية من ترجمة المعاني بلغة الواجهة
    val quranSrc = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val quran by produceState(quranSrc?.let { QuranData.cached("tafsir/${it.id}.txt") }, lang) {
        value = if (lang == Lang.AR || sec.items.none { it.ayah.isNotEmpty() }) null
        else Sources.translationFor(lang)?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }
    ToolPage(tr.title(sec, lang), tr.desc(sec, lang), onBack, icon = sectionIcon(sec), spacing = 12.dp) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val p by animateFloatAsState(if (total == 0) 0f else progress / total.toFloat(), tween(Motion.MEDIUM), label = "progress")
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Gold, trackColor = Gold.copy(alpha = .18f))
                Text(if (complete) s.dhikrDone else s.dhikrTapHint, style = MaterialTheme.typography.bodySmall, color = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        sec.groups.forEach { g ->
            val title = if (lang == Lang.AR) g.titleAr else tr.groups[g.key] ?: g.titleAr
            if (title != null && sec.groups.size > 1) item(key = g.key) {
                Text(title, Modifier.padding(start = 6.dp, top = 6.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            g.items.forEach { d ->
                item(key = d.id) {
                    DhikrCard(
                        d, tr, lang, quran, done[d.id] ?: 0,
                        onTap = {
                            val n = (done[d.id] ?: 0)
                            if (n < d.count) {
                                done[d.id] = n + 1
                                haptic.performHapticFeedback(if (n + 1 == d.count) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                                val allDone = sec.items.all { (done[it.id] ?: 0) >= it.count }
                                if (allDone) AppSettings.putInt(doneKey(sec.id), LocalDate.now().toEpochDay().toInt())
                            } else done[d.id] = 0
                        },
                        onEvidence = { evidence = d },
                    )
                }
            }
        }
    }
    evidence?.let { d -> EvidenceSheet(d, tr, lang, quran) { evidence = null } }
}

/** الترجمة المعروضة تحت الذكر: ترجمته، أو ترجمة معاني آياته إن كان قرآناً */
private fun translation(d: Dhikr, tr: AdhkarTr, lang: Lang, quran: Array<String>?): String? {
    if (lang == Lang.AR) return null
    tr.items[d.id]?.t?.let { return it }
    if (d.ayah.isEmpty() || quran == null) return null
    return d.ayah.joinToString("\n") { (su, a, b) -> (a..b).joinToString(" ") { quran[QuranMeta.index(su, it)] } }
}

@Composable
private fun DhikrCard(d: Dhikr, tr: AdhkarTr, lang: Lang, quran: Array<String>?, done: Int, onTap: () -> Unit, onEvidence: () -> Unit) {
    val s = LocalStrings.current
    val ctx = LocalContext.current
    val finished = done >= d.count
    val a by animateFloatAsState(if (finished) .55f else 1f, tween(Motion.MEDIUM), label = "fade")
    val border by animateColorAsState(if (finished) Gold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f), label = "border")
    val src = remember { MutableInteractionSource() }
    val t = translation(d, tr, lang, quran)
    // الملاحظة القصيرة (متى يقال) على البطاقة، والطويلة (التخريج والخلاف) في الدليل فقط
    val note = (if (lang == Lang.AR) d.note else tr.items[d.id]?.note ?: d.note)?.takeIf { it.length <= 80 }
    Surface(
        onClick = onTap, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (finished) 1.5.dp else 1.dp, border), interactionSource = src,
        modifier = Modifier.fillMaxWidth().pressScale(src, 0.985f),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
            Column(Modifier.alpha(a), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    d.ar, Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = QuranFont, fontSize = 21.sp, lineHeight = 42.sp, textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (t != null) Text(t, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = Gold, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                CountPill(d.count, done)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEvidence) {
                    Icon(Icons.Outlined.Verified, null, Modifier.size(18.dp), tint = gradeColor(d.grade))
                    Spacer(Modifier.width(6.dp))
                    Text(s.dhikrEvidence, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { shareText(ctx, s, shareBody(d, tr, lang, s, t)) }) {
                    Icon(Icons.Filled.Share, s.share, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CountPill(count: Int, done: Int) {
    val s = LocalStrings.current
    val finished = done >= count
    val bg by animateColorAsState(if (finished) Gold else MaterialTheme.colorScheme.primaryContainer, label = "pill")
    Row(Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (finished) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Color(0xFF1E1A14))
        else if (count > 1) Text("$done / $count", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (!finished || count > 1) {
            if (!finished && count > 1) Spacer(Modifier.width(6.dp))
            Text(s.dhikrTimes(count), style = MaterialTheme.typography.labelMedium, color = if (finished) Color(0xFF1E1A14) else MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun gradeColor(grade: String): Color = when (grade) {
    "daif" -> MaterialTheme.colorScheme.error
    "hasan" -> Gold
    else -> MaterialTheme.colorScheme.primary
}

/** "صحيح البخاري 6306 · صحيح مسلم 2702"، وللقرآن اسم السورة ورقم الآية */
internal fun sourceLine(d: Dhikr, tr: AdhkarTr, s: Strings): List<String> = d.src.map { (b, n) ->
    if (b == "quran") {
        val su = n.substringBefore(':').toIntOrNull()
        val name = su?.let { s.surahNames.getOrNull(it - 1) }
        if (name != null) s.surahAyah(name, n.substringAfter(':').substringBefore('-').toIntOrNull() ?: 1) + (if ('-' in n) "–" + n.substringAfter('-') else "") else n
    } else "${tr.books[b] ?: b} ${n}".trim()
}

internal fun gradeLine(d: Dhikr, tr: AdhkarTr): String =
    listOfNotNull(tr.grades[d.grade] ?: d.grade, d.by.takeIf { it.isNotEmpty() }?.let { tr.by[it] ?: it }).joinToString(" · ")

private fun shareBody(d: Dhikr, tr: AdhkarTr, lang: Lang, s: Strings, t: String?) = buildString {
    append(d.ar)
    if (t != null) append("\n\n").append(t)
    append("\n\n").append(sourceLine(d, tr, s).joinToString(" · "))
    append(" (").append(gradeLine(d, tr)).append(")")
}

// ---------- الدليل ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvidenceSheet(d: Dhikr, tr: AdhkarTr, lang: Lang, quran: Array<String>?, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, bottom = 36.dp)) {
            DhikrEvidence(d, tr, lang, quran)
        }
    }
}

/** محتوى الدليل: الدرجة، والمصادر، والراوي، والفضل بنصه العربي وترجمته */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DhikrEvidence(d: Dhikr, tr: AdhkarTr, lang: Lang, quran: Array<String>?) {
    val s = LocalStrings.current
    val itr = tr.items[d.id]
    val c = gradeColor(d.grade)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(s.dhikrEvidence, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            d.ar, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = QuranFont, fontSize = 17.sp, lineHeight = 32.sp, textDirection = TextDirection.Rtl),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Field(s.dhikrGrade) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.clip(CircleShape).background(c.copy(alpha = .14f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, null, Modifier.size(16.dp), tint = c)
                    Spacer(Modifier.width(6.dp))
                    Text(gradeLine(d, tr), style = MaterialTheme.typography.labelLarge, color = c, fontWeight = FontWeight.Bold)
                }
            }
        }
        Field(if (d.grade == "quran") s.dhikrQuranRef else s.dhikrSource) {
            sourceLine(d, tr, s).forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Gold))
                    Spacer(Modifier.width(10.dp))
                    Text(line, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        val narr = if (lang == Lang.AR) d.narr else itr?.narr ?: d.narr
        if (narr != null) Field(s.dhikrNarrator) { Text(narr, style = MaterialTheme.typography.bodyLarge) }
        d.fadl?.let { fadl ->
            Field(s.dhikrVirtue) {
                Quote { Text(fadl, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = QuranFont, fontSize = 18.sp, lineHeight = 34.sp, textDirection = TextDirection.Rtl)) }
                if (lang != Lang.AR) itr?.fadl?.let { Text(it, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (d.ayah.isNotEmpty() && d.grade != "quran") Field(s.dhikrQuranRef) {
            Text(d.ayah.joinToString(" · ") { (su, a, b) -> s.surahAyah(s.surahNames[su - 1], a) + if (b > a) "–$b" else "" }, style = MaterialTheme.typography.bodyMedium)
        }
        val note = if (lang == Lang.AR) d.note else itr?.note ?: d.note
        if (note != null) Note(note)
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        content()
    }
}
