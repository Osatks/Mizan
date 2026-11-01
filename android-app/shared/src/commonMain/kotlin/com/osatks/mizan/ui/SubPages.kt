package com.osatks.mizan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * حركة الانتقال بين صفحة وصفحة فوقها، كما في تطبيقات iOS: الجديدة تدخل كاملة من جهة القراءة وتنزاح السابقة
 * ثلثاً تحتها، والرجوع عكسه. حركة الرجوع خطية لأن سحب الحافة يقودها بالتدريج نفسه: تبقى الصفحة تحت الإصبع.
 * في أندرويد الحركة الأخف المعتادة (انزلاق ربع العرض مع تلاشٍ).
 */
object PageMotion {
    /** منحنى الدفع في UIKit تقريباً (بداية سريعة وتوقف ناعم) */
    private val Push = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    private const val PUSH = 420
    private const val POP = 340

    fun push(rtl: Boolean): EnterTransition =
        if (IsIos) slideInHorizontally(tween(PUSH, easing = Push)) { w -> if (rtl) -w else w }
        else slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { w -> (if (rtl) -w else w) / 4 } +
            fadeIn(tween(Motion.MEDIUM, delayMillis = 40))

    fun pushUnder(rtl: Boolean): ExitTransition =
        if (IsIos) slideOutHorizontally(tween(PUSH, easing = Push)) { w -> (if (rtl) w else -w) / 3 }
        else fadeOut(tween(Motion.MEDIUM))

    fun popUnder(rtl: Boolean): EnterTransition =
        if (IsIos) slideInHorizontally(tween(POP, easing = LinearEasing)) { w -> (if (rtl) w else -w) / 3 }
        else fadeIn(tween(Motion.MEDIUM))

    fun pop(rtl: Boolean): ExitTransition =
        if (IsIos) slideOutHorizontally(tween(POP, easing = LinearEasing)) { w -> if (rtl) -w else w }
        else slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Accelerate)) { w -> (if (rtl) -w else w) / 4 } +
            fadeOut(tween(Motion.SHORT, delayMillis = 60))
}

/**
 * صفحات داخل شاشة واحدة (أنواع الزكاة، أقسام الأذكار): [root] صفحة البداية، وغيرها صفحة فوقها.
 * الرجوع منها بزر الرجوع أو بسحب الحافة في iOS (يتبع الإصبع، ويعود إن تُرك قبل منتصفه) أو بزر الرجوع في أندرويد.
 * [onBack] يُرجع الحالة إلى [root].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SubPages(
    page: T,
    root: T,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any? = { it },
    content: @Composable AnimatedContentScope.(T) -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val state = remember { SeekableTransitionState(page) }
    val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val transition = rememberTransition(state, label = "subPages")
    var inGesture by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = page != root) { events ->
        progress = 0f
        try {
            events.collect { inGesture = true; progress = it.progress }
            inGesture = false
            onBack()
        } catch (e: CancellationException) {
            inGesture = false
            throw e
        }
    }

    if (inGesture) {
        LaunchedEffect(progress) { state.seekTo(progress, root) }
    } else {
        LaunchedEffect(page) {
            if (state.currentState != page) {
                state.animateTo(page)
            } else if (state.targetState != page) {
                // سحب تُرك قبل اكتماله: تعود الصفحة إلى مكانها من حيث تركها الإصبع
                val total = transition.totalDurationNanos / 1_000_000
                animate(state.fraction, 0f, animationSpec = tween((state.fraction * total).toInt())) { value, _ ->
                    launch { if (value > 0f) state.seekTo(value) else state.snapTo(page) }
                }
            }
        }
    }

    transition.AnimatedContent(
        modifier = modifier,
        contentKey = contentKey,
        transitionSpec = { subPageTransform(rtl, root) },
    ) { p ->
        // حالة كل صفحة (موضع التمرير وما أُدخل) تبقى حين تُغطّى وتعود كما تُركت
        holder.SaveableStateProvider(contentKey(p) ?: "root") {
        // كل صفحة معتمة: لا تظهر الصفحتان متراكبتين أثناء الانزلاق
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content(p) }
        }
    }
}

private fun <T> AnimatedContentTransitionScope<T>.subPageTransform(rtl: Boolean, root: T): ContentTransform {
    val back = targetState == root
    return if (back) (PageMotion.popUnder(rtl) togetherWith PageMotion.pop(rtl)).apply { targetContentZIndex = -1f }
    else PageMotion.push(rtl) togetherWith PageMotion.pushUnder(rtl)
}
