package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.floatingGlass
import com.osatks.mizan.ui.glassMaterial
import com.osatks.mizan.ui.glassTone
import dev.chrisbanes.haze.HazeStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.Motion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * شريط خيارات الآية: يظهر بالضغط المطوّل على آية، وفيه كل ما يُفعل بها: التفسير، والاستماع منها، والحفظ، والنسخ،
 * والمشاركة. في أندرويد كبسولة زجاجية عائمة (AyahActions.android.kt)، وفي iOS ورقة النظام القصيرة (NativeQuranSheets.kt)
 */
@Composable
internal expect fun AyahActionBar(index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onShare: () -> Unit, onClose: () -> Unit)


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
    val native = slot.native
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
