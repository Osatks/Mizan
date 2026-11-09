package com.osatks.mizan.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * حركة الانتقال بين صفحة وصفحة فوقها في أندرويد (وفي [SubPages] على iOS إن لم يكن [NavTransitions]).
 */
object PageMotion {
    val Push = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
    val Pop = CubicBezierEasing(0.3f, 0.6f, 0.2f, 1f)
    val Settle = CubicBezierEasing(0.1f, 0.5f, 0.3f, 1f)
    const val PUSH_MS = 440
    const val POP_MS = 380

    fun push(rtl: Boolean): androidx.compose.animation.EnterTransition =
        androidx.compose.animation.slideInHorizontally(tween(Motion.LONG, easing = Motion.Emphasized)) { w -> (if (rtl) -w else w) / 4 } +
            androidx.compose.animation.fadeIn(tween(Motion.MEDIUM, delayMillis = 40))

    fun pop(rtl: Boolean): androidx.compose.animation.ExitTransition =
        androidx.compose.animation.slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Accelerate)) { w -> (if (rtl) -w else w) / 4 } +
            androidx.compose.animation.fadeOut(tween(Motion.SHORT, delayMillis = 60))
}

/**
 * صفحات داخل شاشة واحدة (أنواع الزكاة، أقسام الأذكار): [root] صفحة البداية وتبقى حية دائماً، وغيرها صفحة فوقها
 * تدخل كما تدخل الصفحات في التنقل نفسه. الرجوع بزر الرجوع، أو بسحب الحافة في iOS (يتبع الإصبع ويعود إن تُرك
 * قبل اكتماله)، أو بزر الرجوع في أندرويد. [onBack] يُرجع الحالة إلى [root].
 */
@Composable
fun <T> SubPages(
    page: T,
    root: T,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentKey: (T) -> Any? = { it },
    content: @Composable (T) -> Unit,
) {
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val parentActive = LocalPageActive.current
    val holder = rememberSaveableStateHolder()
    // الصفحة فوق الأولى (ظاهرة أو خارجة)، وظهورها من 0 إلى 1
    var over by remember { mutableStateOf(if (page != root) page else null) }
    var p by remember { mutableFloatStateOf(1f) }
    var moving by remember { mutableStateOf(false) }
    var gesture by remember { mutableStateOf(false) }
    // أثناء سحب الحافة: جهة حركة الإصبع، فتتبعه الصفحة أياً كانت حافته
    var swipeDir by remember { mutableStateOf<Float?>(null) }
    // موضع اللمس الذي فُتحت منه الصفحة العليا (NavTransitions في iOS)
    var origin by remember { mutableStateOf<Any?>(null) }

    // أثناء السحب يبقى مفعّلاً (إيقافه في أندرويد يلغي السحب)، وأثناء حركة أخرى لا يبدأ سحب جديد
    PredictiveBack(enabled = page != root && (gesture || !moving)) { progress ->
        gesture = true
        moving = true
        try {
            progress.collect { e ->
                if (swipeDir == null) swipeDir = if (e.fromLeft) 1f else -1f
                p = 1f - e.progress
            }
        } catch (e: CancellationException) {
            gesture = false
            // تراجع المستخدم: تعود الصفحة مكانها (والمهمة نفسها ملغاة، فالحركة خارج إلغائها)
            withContext(NonCancellable) {
                animate(p, 1f, animationSpec = tween(((1f - p) * PageMotion.POP_MS).toInt().coerceIn(90, PageMotion.POP_MS), easing = PageMotion.Settle)) { v, _ -> p = v }
            }
            moving = false
            swipeDir = null
            throw e
        }
        gesture = false
        onBack()
    }

    LaunchedEffect(page) {
        if (gesture) return@LaunchedEffect
        // iOS: الانتقال يرسمه النظام (صورة الشاشة قبل التغيير)، والصفحة تتغير هنا فوراً
        val motion = NavTransitions.current
        if (page != root) {
            val old = over
            if (motion != null && (old == null || contentKey(old) != contentKey(page))) {
                origin = motion.push {
                    if (old != null) holder.removeState(contentKey(old) ?: "over")
                    over = page
                    p = 1f
                    moving = false
                }
                return@LaunchedEffect
            }
            over = page
            if (old == null || contentKey(old) != contentKey(page)) {
                if (old != null) holder.removeState(contentKey(old) ?: "over")
                moving = true
                p = 0f
                // تبدأ الحركة بعد أن تُبنى الصفحة الجديدة، فلا يقفز أول إطار منها
                withFrameNanos { }
                withFrameNanos { }
                animate(0f, 1f, animationSpec = tween(PageMotion.PUSH_MS, easing = PageMotion.Push)) { v, _ -> p = v }
            }
            p = 1f
            moving = false
        } else if (over != null) {
            // بعد سحب الحافة تكون الصفحة قد خرجت بعضها (p < 1): تكمل هنا خروجها من حيث تركها الإصبع
            if (motion != null && p >= 1f) {
                motion.pop(origin) {
                    over?.let { holder.removeState(contentKey(it) ?: "over") }
                    over = null
                    p = 1f
                    moving = false
                    swipeDir = null
                }
                return@LaunchedEffect
            }
            moving = true
            val from = p
            val ms = (from * PageMotion.POP_MS).toInt().coerceIn(90, PageMotion.POP_MS)
            animate(from, 0f, animationSpec = tween(ms, easing = if (from < 1f) PageMotion.Settle else PageMotion.Pop)) { v, _ -> p = v }
            over?.let { holder.removeState(contentKey(it) ?: "over") }
            over = null
            p = 1f
            moving = false
            swipeDir = null
        }
    }

    val top = over
    Layout(
        modifier = modifier,
        content = {
            // الصفحة الأولى حية دائماً: الرجوع إليها لا يعيد بناءها
            Box(
                Modifier.layoutId(ROOT).fillMaxSize()
                    .then(if (top != null) Modifier.under({ swipeDir ?: dir }) { p } else Modifier)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CompositionLocalProvider(LocalPageActive provides (parentActive && top == null)) { content(root) }
            }
            if (top != null) {
                key(contentKey(top)) {
                    Box(Modifier.layoutId(OVER).fillMaxSize().over({ swipeDir ?: dir }) { p }.background(MaterialTheme.colorScheme.background)) {
                        CompositionLocalProvider(LocalPageActive provides (parentActive && page != root)) {
                            holder.SaveableStateProvider(contentKey(top) ?: "over") { content(top) }
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val fixed = Constraints.fixed(w, h)
        val showRoot = top == null || moving
        val r = if (showRoot) measurables.firstOrNull { it.layoutId == ROOT }?.measure(fixed) else null
        val o = measurables.firstOrNull { it.layoutId == OVER }?.measure(fixed)
        layout(w, h) {
            r?.place(0, 0)
            o?.place(0, 0)
        }
    }
}

private const val ROOT = "root"

private const val OVER = "over"

/** الصفحة العليا: تدخل كاملة من جهة القراءة (iOS)، أو تنزلق قليلاً وتظهر (أندرويد) */
internal fun Modifier.over(dir: () -> Float, p: () -> Float): Modifier = graphicsLayer {
    val v = p()
    if (IsIos) translationX = dir() * (1f - v) * size.width
    else {
        translationX = dir() * (1f - v) * size.width * 0.15f
        alpha = v
    }
}

/** الصفحة تحت العليا: تنزاح ثلثاً وتعتم قليلاً (iOS) */
internal fun Modifier.under(dir: () -> Float, p: () -> Float): Modifier =
    if (!IsIos) this
    else graphicsLayer { translationX = -dir() * 0.3f * p() * size.width }
        .drawWithContent {
            drawContent()
            drawRect(Color.Black.copy(alpha = 0.1f * p()))
        }
