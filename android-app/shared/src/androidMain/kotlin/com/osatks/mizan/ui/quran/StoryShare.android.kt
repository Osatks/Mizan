package com.osatks.mizan.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.encodePng
import com.osatks.mizan.ui.screens.GlassSheet
import com.osatks.mizan.ui.shareText
import kotlinx.coroutines.launch

/** لوحة مشاركة القصة في ورقة سفلية زجاجية */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryShareSheet(content: StoryContent, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    GlassSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        StoryShareContent(content, onDone = onDismiss)
    }
}

/** المعاينة (هي الصورة نفسها)، والألوان، والمعنى، وزرّا الصورة والنص */
@Composable
fun StoryShareContent(content: StoryContent, onDone: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ft = remember(lang) { FeatureText.of(lang.code) }
    val ht = remember(lang) { HomeText.of(lang.code) }
    var style by rememberSaveable { mutableStateOf(CardStyle.EMERALD) }
    var withMeaning by rememberSaveable { mutableStateOf(true) }
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

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
                Text(ht.shareStory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(content.kind, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        StoryCard(
            content, withMeaning, style, s.appName,
            Modifier.widthIn(max = 250.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).recordStory(layer),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardStyle.entries.forEach { st -> StyleSwatch(st, style == st) { style = st } }
            Spacer(Modifier.weight(1f))
            if (content.meaning != null) {
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
                        if (png != null) Platform.shareImage(png, "${content.plain}\n${s.shareSignature}", s.share) else shareText(s, content.plain)
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
                onClick = { shareText(s, content.plain); onDone() },
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

/**
 * يرسم المحتوى كما هو على الشاشة، ويسجّله في [layer] بمقاس القصة (1080×1920) مكبَّراً: النصوص والأشكال
 * تُرسم من جديد بالمقاس الكبير فتبقى حادة، لا صورة المعاينة مكبَّرة.
 */
fun Modifier.recordStory(layer: androidx.compose.ui.graphics.layer.GraphicsLayer): Modifier = drawWithContent {
    val k = STORY_WIDTH / size.width
    layer.record(IntSize(STORY_WIDTH, STORY_HEIGHT)) {
        scale(k, k, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
    }
    drawContent()
}
