package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Proof
import com.osatks.mizan.i18n.ProofKind
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.quran.QuranFont
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep

/** أيقونة الفكرة في كل نقطة من الدليل: تشرح بنظرة ما تتحدث عنه النقطة */
private fun conceptIcon(c: String): ImageVector = when (c) {
    "duty" -> Icons.Filled.TaskAlt
    "gold", "jewelry" -> Icons.Filled.Diamond
    "nisab" -> Icons.Filled.Scale
    "year" -> Icons.Filled.CalendarMonth
    "rate" -> Icons.Filled.Percent
    "debt" -> Icons.Filled.MoneyOff
    "trade" -> Icons.Filled.Storefront
    "value" -> Icons.Filled.Paid
    "shares" -> Icons.Filled.ShowChart
    "camel", "cattle" -> Icons.Filled.Workspaces
    "grazing" -> Icons.Filled.Grass
    "age" -> Icons.Filled.Timeline
    "harvest", "crops" -> Icons.Filled.Agriculture
    "water" -> Icons.Filled.WaterDrop
    "treasure" -> Icons.Filled.Savings
    "mine" -> Icons.Filled.Landscape
    "people" -> Icons.Filled.Groups
    "time" -> Icons.Filled.Schedule
    "money" -> Icons.Filled.Paid
    "fast" -> Icons.Filled.NightsStay
    "food" -> Icons.Filled.Restaurant
    "feed" -> Icons.Filled.VolunteerActivism
    "family" -> Icons.Filled.FamilyRestroom
    "share" -> Icons.Filled.PieChart
    "order" -> Icons.Filled.Gavel
    "scholars" -> Icons.Filled.Forum
    "sun" -> Icons.Filled.WbSunny
    "qibla", "compass" -> Icons.Filled.Explore
    "moon" -> Icons.Filled.NightsStay
    "star" -> Icons.Filled.Star
    "dhikr", "fingers" -> Icons.Filled.TouchApp
    "child" -> Icons.Filled.ChildCare
    "quran" -> Icons.AutoMirrored.Filled.MenuBook
    else -> Icons.Filled.Lightbulb
}

private fun kindIcon(k: ProofKind): ImageVector = when (k) {
    ProofKind.QURAN -> Icons.AutoMirrored.Filled.MenuBook
    ProofKind.HADITH -> Icons.Filled.FormatQuote
    ProofKind.CONSENSUS -> Icons.Filled.Groups
    ProofKind.SCHOLARS -> Icons.Filled.Forum
}

/**
 * «الدليل وأقوال الفقهاء» أسفل كل أداة فيها حكم: لكل نقطة أيقونة تشرح فكرتها، ونوع الدليل
 * (قرآن، سنة، إجماع، أقوال الفقهاء)، ونص الآية أو الحديث، وشرح موجز، ومصدره.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvidenceCard(points: List<Proof>, startOpen: Boolean = false) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var open by rememberSaveable(points.hashCode()) { mutableStateOf(startOpen) }
    val turn by animateFloatAsState(if (open) 180f else 0f, tween(250), label = "evidence")
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    val tr = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(tr?.let { QuranData.cached("tafsir/${it.id}.txt") }, tr) { value = tr?.let { QuranData.source(it.id) } }
    Surface(
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Gold.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = GoldDeep)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(s.evidenceTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(s.evidenceSub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // لمحة عن أنواع الأدلة في هذا القسم
                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        points.map { it.kind }.distinct().forEach { k -> KindChip(k) }
                    }
                }
                Icon(Icons.Filled.ExpandMore, null, Modifier.rotate(turn), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(open, enter = expandVertically(tween(300)) + fadeIn(), exit = shrinkVertically(tween(250)) + fadeOut()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    points.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                        ProofItem(p, lang, quran, trText)
                    }
                }
            }
        }
    }
}

@Composable
private fun kindColor(k: ProofKind): Color = when (k) {
    ProofKind.QURAN -> GoldDeep
    ProofKind.HADITH -> MaterialTheme.colorScheme.primary
    ProofKind.CONSENSUS -> MaterialTheme.colorScheme.tertiary
    ProofKind.SCHOLARS -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun KindChip(k: ProofKind) {
    val s = LocalStrings.current
    val c = kindColor(k)
    Row(
        Modifier.clip(CircleShape).background(c.copy(alpha = .12f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(kindIcon(k), null, Modifier.size(13.dp), tint = c)
        Spacer(Modifier.width(4.dp))
        Text(
            when (k) { ProofKind.QURAN -> s.proofQuran; ProofKind.HADITH -> s.proofHadith; ProofKind.CONSENSUS -> s.proofConsensus; ProofKind.SCHOLARS -> s.proofScholars },
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c
        )
    }
}

@Composable
private fun ProofItem(p: Proof, lang: Lang, quran: Array<String>?, translation: Array<String>?) {
    val c = kindColor(p.kind)
    Row(verticalAlignment = Alignment.Top) {
        // أيقونة الفكرة
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
            Icon(conceptIcon(p.concept), null, Modifier.size(20.dp), tint = c)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KindChip(p.kind)
            Text(p.title[lang], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            // نص الآية من المصحف، وترجمة معانيها بلغة الواجهة
            p.ayah?.let { (surah, from, to) ->
                if (quran != null) {
                    val ayat = (from..to).joinToString(" ") { a ->
                        QuranData.withoutBismillah(quran, QuranMeta.index(surah, a)) + " (" + arabicDigits(a) + ")"
                    }
                    Quote {
                        Text("﴿$ayat﴾", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = QuranFont, fontSize = 19.sp, lineHeight = 36.sp, textDirection = TextDirection.Rtl), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (translation != null) Text(
                    (from..to).joinToString(" ") { a -> translation[QuranMeta.index(surah, a)] },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // نص الحديث بالعربية
            p.hadith?.let { h ->
                Quote { Text(h, style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl, lineHeight = 24.sp), color = MaterialTheme.colorScheme.onSurface) }
            }
            Text(p.body[lang], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(kindIcon(p.kind), null, Modifier.size(14.dp), tint = c)
                Spacer(Modifier.width(5.dp))
                Text(p.source[lang], style = MaterialTheme.typography.labelMedium, color = c, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** الاقتباس (آية أو حديث) بخط جانبي ذهبي */
@Composable
private fun Quote(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(12.dp)).background(Gold.copy(alpha = .08f))) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(Gold))
        Box(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) { content() }
    }
}
