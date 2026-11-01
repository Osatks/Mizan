package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * شريط خيارات الآية: يظهر أسفل الشاشة بالضغط المطوّل على آية، وفيه كل ما يُفعل بها:
 * التفسير، والاستماع منها، والحفظ، والنسخ، والمشاركة.
 */
@Composable
internal fun AyahActionBar(index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onClose: () -> Unit) {
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val (surah, ayah) = QuranMeta.locate(index)
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 10.dp, tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahMarker(ayah, Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(ref, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, s.close, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Action(Icons.AutoMirrored.Filled.MenuBook, s.tafsirTitle, primary = true, onClick = onTafsir)
                Action(Icons.Filled.PlayArrow, s.listen, primary = true, onClick = onListen)
                val marked = QuranPrefs.isBookmarked(index)
                Action(
                    if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    if (marked) s.saved else s.save, tint = if (marked) Gold else null,
                ) { QuranPrefs.toggleBookmark(index) }
                Action(Icons.Filled.ContentCopy, s.copy) {
                    clipboard.setText(AnnotatedString("$ayahText\n[$ref]"))
                    Platform.toast(s.copied)
                    onClose()
                }
                Action(Icons.Filled.Share, s.share) { shareText(s, "﴿$ayahText﴾\n[$ref]"); onClose() }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Action(
    icon: ImageVector, label: String, primary: Boolean = false, tint: Color? = null, onClick: () -> Unit,
) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(icon, transitionSpec = { scaleIn(spring(dampingRatio = 0.5f)) togetherWith fadeOut(tween(90)) }, label = "act") { ic ->
                Icon(ic, null, tint = tint ?: if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/**
 * لوحة داخل شاشة القراءة نفسها (لا نافذة منفصلة) للتفسير وإعدادات التلاوة والعرض، فتبقى الصفحة خلفها ثابتة كما هي.
 * تُغلق بلمس الخلفية أو بسحبها إلى الأسفل أو بزر الرجوع. [paper]: بلون ورق المصحف (للتفسير).
 */
@Composable
internal fun BoxScope.ReadingPanel(visible: Boolean, onDismiss: () -> Unit, paper: Boolean = true, content: @Composable () -> Unit) {
    BackHandler(enabled = visible) { onDismiss() }
    AnimatedVisibility(visible, Modifier.matchParentSize(), enter = fadeIn(tween(Motion.MEDIUM)), exit = fadeOut(tween(Motion.MEDIUM))) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible, Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
        exit = slideOutVertically(tween(Motion.MEDIUM)) { it },
    ) {
        val drag = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val limit = with(LocalDensity.current) { 120.dp.toPx() }
        LaunchedEffect(Unit) { drag.snapTo(0f) }
        Surface(
            Modifier.fillMaxWidth().heightIn(max = 640.dp).offset { IntOffset(0, drag.value.roundToInt()) }
                // تمنع لمسات اللوحة من الوصول إلى الخلفية
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = if (paper) mushafPalette().paper else MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 16.dp,
        ) {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                // مقبض السحب
                Box(
                    Modifier.fillMaxWidth().draggable(
                        rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) } },
                        Orientation.Vertical,
                        onDragStopped = { if (drag.value > limit) onDismiss() else drag.animateTo(0f) },
                    ).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline))
                }
                content()
            }
        }
    }
}
