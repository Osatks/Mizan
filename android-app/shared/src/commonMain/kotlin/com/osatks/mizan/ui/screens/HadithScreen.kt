package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.hadith.Hadith
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.quran.StoryContent
import com.osatks.mizan.ui.quran.StoryShareSheet
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
    // في iOS: الحديث بواجهة النظام (SwiftUI) بطراز الرئيسية؛ وورقة القصة من Compose كما هي
    if (nativeHadithAvailable) {
        val uri = LocalUriHandler.current
        NativeHadithView(
            hadith, ht.hadithOfDay, "${s.weekday(day)} · ${s.greg(day)}", canNext = offset < 0,
            onDay = { d -> offset = (offset + d).coerceAtMost(0) },
            onShare = { hadith?.let { story = hadithStory(it, ht.hadithOfDay) } },
            onSource = { hadith?.let { h -> runCatching { uri.openUri(h.link) } } },
            onBack = onBack, modifier = Modifier.fillMaxSize(),
        )
        story?.let { StoryShareSheet(it) { story = null } }
        return
    }
    ToolPage(ht.hadithOfDay, "${s.weekday(day)} · ${s.greg(day)}", onBack) {
        val h = hadith ?: return@ToolPage
        item(key = "text-${h.id}") { HadithHero(h) }
        h.tr?.let { tr -> item(key = "tr-${h.id}") { SectionCard { BodyText(tr.text) } } }
        val main = h.tr ?: h.ar
        item(key = "expl-${h.id}") { SectionCard(ht.explanation) { BodyText(main.expl, arabic = h.tr == null) } }
        if (main.benefits.isNotEmpty()) item(key = "benefits-${h.id}") {
            SectionCard(ht.benefits) {
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

/** صورة القصة للحديث: نصه كاملاً إن قصر، وإلا طرفه (عنوانه في الموسوعة)، ومعه الدرجة والتخريج والمصدر */
fun hadithStory(h: Hadith, kind: String): StoryContent {
    val full = h.ar.text.length <= 420
    val main = h.tr ?: h.ar
    val ref = listOf(main.grade, main.takhrij).filter { it.isNotBlank() }.joinToString(" · ")
    val plain = buildString {
        append(h.ar.text)
        h.tr?.let { append("\n\n").append(it.text) }
        if (ref.isNotEmpty()) append("\n\n").append(ref)
        append("\n").append(h.link)
    }
    return StoryContent(
        kind, if (full) h.ar.text else h.ar.title, h.tr?.let { if (full) it.text else it.title }, ref, plain, Hadiths.SOURCE,
    )
}

/** النص العربي في بطاقة بلون التطبيق، وتحته الدرجة والتخريج */
@Composable
private fun HadithHero(h: Hadith) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = cs.surface, border = BorderStroke(1.dp, Gold.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.background(Brush.verticalGradient(listOf(cs.primaryContainer.copy(alpha = .55f), cs.surface))).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.FormatQuote, null, tint = GoldDeep, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                h.ar.text,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Rtl, lineHeight = MaterialTheme.typography.titleLarge.fontSize * 1.9f),
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, color = cs.onSurface, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            val tr = h.tr
            val grade = listOf(tr?.grade ?: h.ar.grade, tr?.takhrij ?: h.ar.takhrij).filter { it.isNotBlank() }.joinToString(" · ")
            if (grade.isNotEmpty()) Text(
                grade, style = MaterialTheme.typography.labelLarge, color = GoldDeep, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Gold.copy(alpha = .14f)).padding(horizontal = 14.dp, vertical = 6.dp),
            )
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
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Hadiths.SOURCE, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    if (h.tr == null) "موسوعة الأحاديث النبوية" else "Encyclopedia of Translated Prophetic Hadiths",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { runCatching { uri.openUri(h.link) } }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, Hadiths.SOURCE, tint = MaterialTheme.colorScheme.primary)
            }
        }
        FilledTonalButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(shareLabel)
        }
    }
}

/** اليوم السابق والتالي (لا يتجاوز اليوم) */
@Composable
private fun DayArrows(offset: Int, prev: String, next: String, set: (Int) -> Unit) {
    // الأسهم تنعكس وحدها من اليمين إلى اليسار
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { set(offset - 1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
            Text(prev)
        }
        TextButton(onClick = { set(offset + 1) }, enabled = offset < 0) {
            Text(next)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}
