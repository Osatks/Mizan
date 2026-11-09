package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.hadith.Hadith
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.StoryContent
import com.osatks.mizan.ui.quran.StoryShareSheet
import com.osatks.mizan.ui.starBackdrop
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * حديث اليوم من موسوعة الأحاديث النبوية: نصه العربي، وترجمته بلغة الواجهة، ثم الشرح والفوائد والدرجة والتخريج كما هي،
 * ومصدره ورابطه في الموسوعة. السهمان يعرضان أحاديث الأيام الماضية (حتى اليوم).
 */
@Composable
fun HadithScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val today = remember { PrayerUtil.now().date }
    var offset by remember { mutableIntStateOf(0) }
    val day = today.plusDays(offset)
    val hadith by produceState<Hadith?>(null, day, lang) {
        value = withContext(Dispatchers.IO) { runCatching { Hadiths.forDay(day, lang.code) }.getOrNull() }
    }
    var story by remember { mutableStateOf<StoryContent?>(null) }
    ToolPage(ht.hadithOfDay, "${s.weekday(day)} · ${s.greg(day)}", onBack, icon = Icons.Filled.FormatQuote) {
        val h = hadith ?: return@ToolPage
        item(key = "text-${h.id}") { HadithHero(h) }
        h.tr?.let { tr -> item(key = "tr-${h.id}") { HadithCard { BodyText(tr.text) } } }
        val main = h.tr ?: h.ar
        item(key = "expl-${h.id}") { HadithCard(ht.explanation) { BodyText(main.expl, arabic = h.tr == null) } }
        if (main.benefits.isNotEmpty()) item(key = "benefits-${h.id}") {
            HadithCard(ht.benefits) {
                main.benefits.forEach { b ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 9.dp).size(6.dp).clip(CircleShape).background(Gold))
                        Spacer(Modifier.width(10.dp))
                        BodyText(b, arabic = h.tr == null)
                    }
                }
            }
        }
        item(key = "source-${h.id}") { SourceCard(h, ht.shareStory) { story = hadithStory(h, ht.hadithOfDay) } }
        item(key = "days") { DayArrows(offset, s.prevDay, s.nextDay) { offset = it } }
    }
    story?.let { StoryShareSheet(it) { story = null } }
}

/** النص العربي في بطاقة بتدرج لون التطبيق وحدّ ذهبي ونجمة ذهبية باهتة في زاويتها، وتحته الدرجة والتخريج */
@Composable
private fun HadithHero(h: Hadith) {
    val cs = MaterialTheme.colorScheme
    // النجمة في الزاوية اليمنى العليا في كل اللغات (كما في iPhone)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(shape = RoundedCornerShape(24.dp), color = cs.surface, border = BorderStroke(1.dp, Gold.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(cs.primaryContainer.copy(alpha = .55f), cs.surface)))
                .starBackdrop(Gold.copy(alpha = .14f), 64.dp, if (rtl) Alignment.TopStart else Alignment.TopEnd, if (rtl) 30.dp else (-30).dp, 30.dp, 120_000, stroke = 3f)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.FormatQuote, null, tint = GoldDeep, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                h.ar.text,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl, fontSize = 22.sp, lineHeight = 22.sp * 1.9f),
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = cs.onSurface, modifier = Modifier.fillMaxWidth(),
            )
            val tr = h.tr
            val grade = listOf(tr?.grade ?: h.ar.grade, tr?.takhrij ?: h.ar.takhrij).filter { it.isNotBlank() }.joinToString(" · ")
            if (grade.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    grade, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = GoldDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Gold.copy(alpha = .14f)).padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** بطاقة بيضاء بعنوان اختياري بطراز iPhone (حدّ رفيع وزوايا 22) */
@Composable
private fun HadithCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) Text(
                title, Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.primary, fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun BodyText(text: String, arabic: Boolean = false) {
    val st = MaterialTheme.typography.bodyLarge
    Text(
        text,
        style = if (arabic) st.copy(textDirection = TextDirection.Rtl, lineHeight = st.fontSize * 1.8f) else st.copy(lineHeight = st.fontSize * 1.55f),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** المصدر بلا تعديل، ورابط الحديث في الموسوعة، ومشاركة الحديث */
@Composable
private fun SourceCard(h: Hadith, shareLabel: String, onShare: () -> Unit) {
    val uri = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    HadithCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Hadiths.SOURCE, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp), color = cs.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (h.tr == null) "موسوعة الأحاديث النبوية" else "Encyclopedia of Translated Prophetic Hadiths",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                )
            }
            IconButton(onClick = { runCatching { uri.openUri(h.link) } }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, Hadiths.SOURCE, tint = cs.primary)
            }
        }
        // زر المشاركة: كبسولة بلون التطبيق الفاتح
        val src = remember { MutableInteractionSource() }
        Surface(
            onClick = onShare, shape = CircleShape, color = cs.primaryContainer, contentColor = cs.onPrimaryContainer,
            interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.97f),
        ) {
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(shareLabel, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** اليوم السابق والتالي (لا يتجاوز اليوم) */
@Composable
private fun DayArrows(offset: Int, prev: String, next: String, set: (Int) -> Unit) {
    // الأسهم تنعكس وحدها من اليمين إلى اليسار
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayButton(enabled = true, onClick = { set(offset - 1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(22.dp))
            Text(prev, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
        }
        DayButton(enabled = offset < 0, onClick = { set(offset + 1) }) {
            Text(next, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp))
        }
    }
}

/** زر نصي بلون التطبيق، ويبهت حين يتعطل (كما في iPhone) */
@Composable
private fun DayButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary, interactionSource = src,
        modifier = Modifier.alpha(if (enabled) 1f else .38f).pressScale(src, 0.94f),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
    }
}
