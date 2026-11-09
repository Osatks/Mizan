package com.osatks.mizan.ui.quran

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.encodePng
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.shareText
import kotlinx.coroutines.launch
import com.osatks.mizan.quran.arabicDigits

/** لوحة المشاركة في شاشة القراءة: المعاينة وخياراتها وزرّا الصورة والنص */
@Composable
internal actual fun AyahShareContent(index: Int, text: Array<String>, onDone: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ft = remember(lang) { FeatureText.of(lang.code) }
    val (surah, ayah) = remember(index) { QuranMeta.locate(index) }
    val ayahText = remember(index, text) { QuranData.withoutBismillah(text, index) }
    val ref = s.surahAyah(s.surahName(surah), ayah)
    // المعنى بلغة الواجهة (ترجمة مضمّنة أو منزَّلة)؛ لا معنى في العربية
    val source = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    var meaning by remember(index, source) { mutableStateOf<String?>(null) }
    LaunchedEffect(index, source) {
        meaning = source?.let { runCatching { QuranData.source(it.id) }.getOrNull()?.getOrNull(index)?.trim()?.takeIf { m -> m.isNotEmpty() } }
    }
    var withMeaning by rememberSaveable { mutableStateOf(true) }
    var style by rememberSaveable { mutableStateOf(CardStyle.EMERALD) }
    var story by rememberSaveable { mutableStateOf(false) }
    val ht = remember(lang) { HomeText.of(lang.code) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val shown = meaning.takeIf { withMeaning }
    val plain = "﴿$ayahText﴾\n[$ref]"

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ft.shareAyah, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // الشكل: بطاقة (منشور) أو قصة (9:16)
        FormatToggle(story, ht.post, ht.story) { story = it }
        // المعاينة هي الصورة: تُسجَّل في الطبقة كما تُرسم (بلا قصّ الزوايا، فالصورة مربعة الزوايا)
        val shape = RoundedCornerShape(18.dp)
        if (story) StoryCard(
            StoryContent("سُورَةُ ${QuranMeta.NAMES_AR[surah - 1]}", "$ayahText ۝${arabicDigits(ayah)}", meaning, ref, plain),
            withMeaning, style, s.appName,
            Modifier.widthIn(max = 250.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).recordStory(layer),
        ) else AyahCard(
            ayahText, surah, ayah, ref, shown, style, s.appName,
            Modifier.widthIn(max = 330.dp).fillMaxWidth().clip(shape)
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
        )
        // الألوان، والمعنى
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardStyle.entries.forEach { st -> StyleSwatch(st, style == st) { style = st } }
            Spacer(Modifier.weight(1f))
            if (meaning != null) {
                Text(ft.withMeaning, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Switch(checked = withMeaning, onCheckedChange = { withMeaning = it })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    scope.launch {
                        val png = runCatching { layer.toImageBitmap().encodePng() }.getOrNull()
                        busy = false
                        if (png != null) Platform.shareImage(png, "$plain\n${s.shareSignature}", s.share) else shareText(s, plain)
                        onDone()
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ft.image, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = { shareText(s, plain); onDone() },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Icon(Icons.AutoMirrored.Filled.ShortText, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ft.text, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

/** اختيار شكل الصورة: بطاقة أو قصة */
@Composable
internal fun FormatToggle(story: Boolean, post: String, storyLabel: String, set: (Boolean) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(false to post, true to storyLabel).forEach { (v, label) ->
            val on = v == story
            Text(
                label, maxLines = 1,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { set(v) }.padding(horizontal = 18.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
internal fun StyleSwatch(style: CardStyle, selected: Boolean, onClick: () -> Unit) {
    val c = cardColors(style, AppSettings.accent)
    val scale by animateFloatAsState(if (selected) 1f else 0.86f, label = "swatch")
    Box(
        Modifier.size(34.dp).scale(scale).clip(CircleShape)
            .background(Brush.linearGradient(listOf(c.top, c.bottom)))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c.gold))
    }
}
