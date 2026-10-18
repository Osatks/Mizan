package com.osatks.mizan.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** منحنيات الحركة الموحّدة للتطبيق (Material "emphasized") */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 480

    /** "إزالة الحركات" مفعّل في إعدادات النظام: لا ظهور متتابع ولا دوران زخرفي */
    var reduced = false
}

// ---------- العناصر المشتركة بين الصفحات (Shared elements) ----------

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * يربط عنصرين في صفحتين بنفس المفتاح فيتحوّل أحدهما إلى الآخر أثناء الانتقال
 * (بطاقة الأداة في الرئيسية ← رأس صفحة الأداة). بلا أثر خارج NavHost.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCard(key: String, shape: Shape = RoundedCornerShape(24.dp)): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalNavScope.current ?: return this
    return with(shared) {
        this@sharedCard.sharedBounds(
            rememberSharedContentState(key), anim,
            enter = fadeIn(tween(Motion.MEDIUM, delayMillis = 60)),
            exit = fadeOut(tween(Motion.SHORT)),
            boundsTransform = { _, _ -> spring(dampingRatio = 0.86f, stiffness = 380f) },
            clipInOverlayDuringTransition = OverlayClip(shape)
        )
    }
}

/** مثل [sharedCard] لكن للعنصر نفسه في الصفحتين (أيقونة تطير من البطاقة إلى الرأس) */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedIcon(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val anim = LocalNavScope.current ?: return this
    return with(shared) {
        this@sharedIcon.sharedElement(
            rememberSharedContentState(key), anim,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.8f, stiffness = 300f) }
        )
    }
}

// ---------- ظهور متتابع لعناصر الصفحة ----------

/** يتذكر لحظة فتح الصفحة: العناصر الأولى تظهر متتابعة، وما يظهر لاحقاً (بالتمرير) يظهر فوراً */
class RevealState {
    private val start = kotlin.time.TimeSource.Monotonic.markNow()
    fun shouldAnimate(index: Int) = !Motion.reduced && index < 10 && start.elapsedNow().inWholeMilliseconds < 450
}

fun Modifier.reveal(state: RevealState, index: Int): Modifier = composed {
    val anim = remember { Animatable(if (state.shouldAnimate(index)) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (anim.value < 1f) {
            delay(40L * index)
            anim.animateTo(1f, tween(Motion.LONG, easing = Motion.Decelerate))
        }
    }
    val dy = with(LocalDensity.current) { 36.dp.toPx() }
    graphicsLayer {
        val p = anim.value
        alpha = p
        translationY = (1f - p) * dy
        val s = 0.96f + 0.04f * p
        scaleX = s; scaleY = s
    }
}

/** ارتداد خفيف عند الضغط */
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.95f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

// ---------- أرقام متدحرجة ----------

private const val NUMERIC = "0123456789.,:%°   '+-"

/**
 * نص يتغير بحركة: الأرقام تتدحرج خانةً خانة كعدّاد المسافات، وأي نص آخر ينزلق ويتلاشى كاملاً.
 * الأرقام تُعرض دائماً من اليسار لليمين حتى في الواجهات العربية.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val tabular = style.copy(fontFeatureSettings = "tnum")
    if (text.isEmpty() || !text.all { it in NUMERIC }) {
        AnimatedContent(
            text, modifier,
            transitionSpec = {
                (fadeIn(tween(Motion.MEDIUM, 60)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 3 }) togetherWith
                    fadeOut(tween(120)) using SizeTransform(clip = false)
            },
            contentAlignment = Alignment.Center,
            label = "swap"
        ) { Text(it, style = style, color = color, textAlign = textAlign) }
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            val n = text.length
            text.forEachIndexed { i, ch ->
                // المفتاح من اليمين: خانة الآحاد تبقى خانة الآحاد مهما طال الرقم
                key(n - i) {
                    AnimatedContent(
                        ch,
                        transitionSpec = {
                            val up = !initialState.isDigit() || !targetState.isDigit() || targetState > initialState ||
                                (initialState == '9' && targetState == '0')
                            val dir = if (up) 1 else -1
                            (slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { dir * it } + fadeIn(tween(Motion.SHORT))) togetherWith
                                (slideOutVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { -dir * it } + fadeOut(tween(Motion.SHORT))) using
                                SizeTransform(clip = true)
                        },
                        label = "digit"
                    ) { c -> Text(c.toString(), style = tabular, color = color) }
                }
            }
        }
    }
}
