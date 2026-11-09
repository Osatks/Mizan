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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.GlassTone
import com.osatks.mizan.ui.IsIos
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.floatingGlass
import com.osatks.mizan.ui.glassMaterial
import com.osatks.mizan.ui.glassTone
import com.osatks.mizan.ui.squish
import androidx.compose.material3.ripple
import dev.chrisbanes.haze.HazeStyle
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
import androidx.compose.runtime.CompositionLocalProvider
import com.osatks.mizan.ui.screens.nativeSheetsAvailable
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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * شريط خيارات الآية: يظهر أسفل الشاشة بالضغط المطوّل على آية، وفيه كل ما يُفعل بها:
 * التفسير، والاستماع منها، والحفظ، والنسخ، والمشاركة.
 */
@Composable
internal fun AyahActionBar(index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onShare: () -> Unit, onClose: () -> Unit) {
    // في iOS: ورقة النظام القصيرة بالخيارات نفسها (NativeQuranSheets.kt)، فلا يُرسم هنا شيء
    if (nativeSheetsAvailable) { NativeAyahActions(index, text, onTafsir, onListen, onShare, onClose); return }
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val (surah, ayah) = QuranMeta.locate(index)
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    // كبسولة زجاجية عائمة فوق الصفحة (تموّه ما تحتها وحدها)؛ زوايا الأزرار متّحدة المركز معها (30 − 6 = 24)
    val shape = RoundedCornerShape(BarRadius)
    Column(
        Modifier.fillMaxWidth()
            .floatingGlass(LocalAppHaze.current, shape, glassMaterial(if (glassTone() == GlassTone.Dark) .60f else .66f))
            // تمنع لمسات الشريط من الوصول إلى الصفحة تحته
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(BarPad)
    ) {
        Column {
            Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AyahMarker(ayah, Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(ref, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, s.close, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                // صورة بخط المصحف أو نص (AyahShare.kt)
                Action(Icons.Filled.Share, s.share) { onShare() }
            }
        }
    }
}

/** زوايا شريط خيارات الآية وهامشه الداخلي */
private val BarRadius = 30.dp
private val BarPad = 6.dp

@Composable
private fun androidx.compose.foundation.layout.RowScope.Action(
    icon: ImageVector, label: String, primary: Boolean = false, tint: Color? = null, onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    Column(
        Modifier.weight(1f).squish(src).clip(RoundedCornerShape(BarRadius - BarPad))
            .clickable(interactionSource = src, indication = if (IsIos) null else ripple(), onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(icon, transitionSpec = { scaleIn(spring(dampingRatio = 0.5f)) togetherWith fadeOut(tween(90)) }, label = "act") { ic ->
                Icon(ic, null, tint = tint ?: if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // الأسماء الطويلة («Поделиться»، «Enregistrer») تصغر قليلاً لتتسع في عرض الشاشة الخارجية لـ iPhone Duo
        val color = LocalContentColor.current
        com.osatks.mizan.ui.FitLabel(label, { color }, MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center), 9.sp, Modifier.padding(horizontal = 2.dp))
    }
}

/**
 * لوحة داخل شاشة القراءة نفسها (لا نافذة منفصلة) للتفسير وإعدادات التلاوة والعرض، فتبقى الصفحة خلفها ثابتة كما هي.
 * تُغلق بلمس الخلفية أو بسحبها إلى الأسفل أو بزر الرجوع. [paper]: بلون ورق المصحف (للتفسير).
 */
@Composable
internal fun BoxScope.ReadingPanel(visible: Boolean, onDismiss: () -> Unit, paper: Boolean = true, content: @Composable () -> Unit) {
    BackHandler(enabled = visible) { onDismiss() }
    // في iOS قد يعرض المحتوى ورقة النظام بدل اللوحة (التفسير والمشاركة): فتخفي اللوحة زجاجها وستارها ولمسها،
    // ويبقى المحتوى في مكانه من التركيب نفسه حتى لا تُغلق الورقة وتُفتح من جديد
    val slot = remember { ReadingPanelSlot() }
    slot.dismiss = onDismiss
    val native = nativeSheetsAvailable && slot.native
    val inner: @Composable () -> Unit = { CompositionLocalProvider(LocalReadingPanel provides slot) { content() } }
    // على iPad والشاشات العريضة لوحة جانبية بجانب الصفحة (أو الصفحتين)، فيبقى النص ظاهراً أثناء قراءة التفسير
    if (LocalWindowLayout.current.wide) {
        SidePanel(visible, onDismiss, paper, native, inner)
        return
    }
    AnimatedVisibility(visible && !native, Modifier.matchParentSize(), enter = fadeIn(tween(Motion.MEDIUM)), exit = fadeOut(tween(Motion.MEDIUM))) {
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
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Box(
            Modifier.fillMaxWidth().heightIn(max = 640.dp).offset { IntOffset(0, drag.value.roundToInt()) }
                .then(
                    if (native) Modifier
                    else Modifier.floatingGlass(LocalAppHaze.current, shape, panelMaterial(paper), elevation = 16.dp)
                        // تمنع لمسات اللوحة من الوصول إلى الخلفية
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                ),
        ) {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                // مقبض السحب
                if (!native) Box(
                    Modifier.fillMaxWidth().draggable(
                        rememberDraggableState { d -> scope.launch { drag.snapTo((drag.value + d).coerceAtLeast(0f)) } },
                        Orientation.Vertical,
                        onDragStopped = { if (drag.value > limit) onDismiss() else drag.animateTo(0f) },
                    ).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline))
                }
                inner()
            }
        }
    }
}

/**
 * مادة لوحات القراءة: زجاج بلون ورق المصحف كثيف يكاد يكون ورقاً في لوحة التفسير (يُقرأ النص عليه كما على الصفحة)،
 * وزجاج التطبيق في لوحتي التلاوة والعرض
 */
@Composable
private fun panelMaterial(paper: Boolean): HazeStyle =
    if (paper) glassMaterial(.93f, color = mushafPalette().paper, fallback = 1f)
    else glassMaterial(if (glassTone() == GlassTone.Dark) .80f else .84f, fallback = 1f)

/** عرض اللوحة الجانبية في شاشة القراءة على الشاشات العريضة */
@Composable
internal fun readingPanelWidth(): Dp = (LocalWindowLayout.current.width * 0.42f).coerceIn(340.dp, 420.dp)

/**
 * لوحة القراءة على الشاشات العريضة: تنزلق من جهة النهاية (اليسار في العربية) بطول الشاشة وبجانب الصفحة،
 * بلا ستار يغطي النص، فيبقى المصحف ظاهراً ويمكن لمس آية أخرى ليتبعها التفسير. تُغلق بزرها أو بالرجوع.
 */
@Composable
private fun BoxScope.SidePanel(visible: Boolean, onDismiss: () -> Unit, paper: Boolean, native: Boolean, content: @Composable () -> Unit) {
    val s = LocalStrings.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val width = readingPanelWidth()
    AnimatedVisibility(
        visible, Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        enter = slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { if (rtl) -it else it } + fadeIn(tween(Motion.SHORT)),
        exit = slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Accelerate)) { if (rtl) -it else it } + fadeOut(tween(Motion.SHORT)),
    ) {
        Box(
            Modifier.width(width).fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
                .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                .then(
                    if (native) Modifier
                    else Modifier.floatingGlass(LocalAppHaze.current, RoundedCornerShape(28.dp), panelMaterial(paper), elevation = 16.dp)
                        // تمنع لمسات اللوحة من الوصول إلى الصفحة تحتها
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                ),
        ) {
            Column {
                if (!native) Box(Modifier.fillMaxWidth().padding(top = 6.dp, end = 6.dp), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, s.close, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}
