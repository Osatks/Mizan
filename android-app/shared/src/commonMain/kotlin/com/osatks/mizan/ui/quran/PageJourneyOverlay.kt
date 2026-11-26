package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.JourneyKind
import com.osatks.mizan.quran.PageJourney
import com.osatks.mizan.quran.PageJourneys
import com.osatks.mizan.ui.drawStar8
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/*
 * رحلة الانتقال إلى صفحة في المصحف (Compose: أندرويد، وفي iOS حين يُعرض المصحف بخط Amiri؛ وقارئ النظام في iOS يرسم
 * الرحلة نفسها في NativeMushaf.swift): الصفحة الحالية تُرفع عن الكتاب قليلاً بظلها، ثم تنقلب أوراق المصحف بسرعة في
 * اتجاه القراءة (تتسارع ثم تتباطأ) وفوقها ميدالية ذهبية يدور فيها رقم الصفحة كعدّاد ويعبر تحتها اسم الجزء، ثم تهبط
 * الصفحة المقصودة إلى مكانها بارتداد خفيف ويمرّ عليها ضوء ذهبي مرة ويتوهج رقمها. الأوراق بين الطرفين ورق بإطاره ورأسه
 * ورقمه وأسطر باهتة فقط، والصفحة الأولى صورة من الشاشة والأخيرة الصفحة الحقيقية نفسها (يُقفز إليها تحت الأوراق بلا حركة)،
 * فلا تُخطَّط مئات الصفحات. التخطيط والمدد مشتركان (PageJourneys).
 */

/** مراحل الرحلة: صورة الصفحة الحالية، والتقليب، والتلاشي (إزالة الحركات)، والهبوط */
internal enum class JourneyPhase { Idle, Capture, Flip, Fade, Land }

/** التقليبة الواحدة: تبدأ هادئة وتنتهي هادئة */
private val TurnEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** مقدار رفع الصفحة عن الكتاب */
private const val LIFTED = 0.92f

/** ظل الورقة كظل صفحة المصحف (MushafPage) */
private val SheetShadow = Color(0xFF5C4300)

/**
 * حالة الرحلة في القارئ: المرحلة والتخطيط والتقليبة الحالية وما يتحرك فيها؛ [startShot] صورة الصفحة الحالية،
 * و[liveShot] الصفحة المقصودة كما يرسمها HorizontalPager تحت الأوراق.
 */
@Stable
internal class PageJourneyState(val startShot: GraphicsLayer, val liveShot: GraphicsLayer) {
    var phase by mutableStateOf(JourneyPhase.Idle)
        private set
    var plan by mutableStateOf<PageJourney?>(null)
        private set
    var spread by mutableStateOf(false)
        private set
    /** التقليبة الجارية (عدد التقليبات حين تنكشف الصفحة المقصودة كلها) */
    var turn by mutableIntStateOf(0)
        private set
    /** الصفحة المقصودة: يتوهج رقمها في أسفلها عند الهبوط */
    var target by mutableIntStateOf(1)
        private set
    var sweeping by mutableStateOf(false)
        private set

    val fold = Animatable(0f)
    val lift = Animatable(1f)
    val fade = Animatable(0f)
    val sweep = Animatable(0f)
    val glow = Animatable(0f)
    val badge = Animatable(0f)
    val pulse = Animatable(1f)

    /** صُوّرت الصفحة الحالية (يكتبه الرسم، فليس حالة تُعيد التركيب) */
    internal var captured = false

    /**
     * صورة نقطية ثابتة للصفحة الحالية: تسجيل [startShot] يحيل إلى طبقات الصفحات نفسها، فإذا قفز HorizontalPager
     * تحت الأوراق ظهرت فيه الصفحة المقصودة قبل أوانها؛ الصورة تبقى الصفحة التي بدأت منها الرحلة
     */
    internal var startImage by mutableStateOf<ImageBitmap?>(null)

    val busy: Boolean get() = phase != JourneyPhase.Idle

    /** الأوراق تغطي الصفحات: HorizontalPager يُسجَّل في [liveShot] ولا يُرسم مباشرة */
    val covering: Boolean get() = phase == JourneyPhase.Flip

    /** الرقم الدوّار الآن: من رقم الورقة الظاهرة إلى التي تنكشف تحتها بقدر التقليبة */
    fun rolling(): Float {
        val j = plan ?: return 0f
        if (j.turns.isEmpty() || turn >= j.turns.size) return j.pages.last().toFloat()
        val k = turn
        return j.pages[k] + (j.pages[k + 1] - j.pages[k]) * fold.value
    }

    /**
     * الرحلة كلها: [jump] يقفز إلى موضع بلا حركة (تحت الأوراق)، و[turnTo] يقلب إليه بالحركة المعتادة (الصفحة المجاورة)،
     * و[onLand] حين تبدأ الصفحة المقصودة بالهبوط (شريط السورة والجزء والصفحة). [reduce]: «إزالة الحركات» في النظام.
     */
    suspend fun run(
        plan: PageJourney, spread: Boolean, toPage: Int, reduce: Boolean, haptic: HapticFeedback,
        jump: suspend (Int) -> Unit, turnTo: suspend (Int) -> Unit, onLand: () -> Unit,
    ) {
        try {
            reset()
            this.plan = plan
            this.spread = spread
            target = toPage
            val moved = when {
                plan.kind == JourneyKind.Still -> true
                reduce -> crossFade(plan, jump)
                plan.kind == JourneyKind.Short -> { turnTo(plan.to); true }
                else -> flip(plan, haptic, jump)
            }
            if (!moved) jump(plan.to)
            phase = JourneyPhase.Land
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onLand()
            land(reduce)
        } finally {
            withContext(NonCancellable) { reset() }
        }
    }

    private suspend fun reset() {
        phase = JourneyPhase.Idle
        plan = null
        startImage = null
        turn = 0
        sweeping = false
        fold.snapTo(0f); lift.snapTo(1f); fade.snapTo(0f); sweep.snapTo(0f)
        glow.snapTo(0f); badge.snapTo(0f); pulse.snapTo(1f)
    }

    /** يصوّر الصفحة الحالية في [startShot] (في أول رسم بعد الطلب)؛ false إن لم تُرسم (القارئ غير ظاهر) */
    private suspend fun capture(): Boolean {
        captured = false
        phase = JourneyPhase.Capture
        var frames = 0
        while (!captured && frames < 6) { withFrameNanos { }; frames++ }
        if (captured) startImage = runCatching { startShot.toImageBitmap() }.getOrNull()
        return captured
    }

    /** إزالة الحركات: صورة الصفحة الحالية تتلاشى عن المقصودة تحتها */
    private suspend fun crossFade(plan: PageJourney, jump: suspend (Int) -> Unit): Boolean {
        if (!capture()) return false
        fade.snapTo(1f)
        phase = JourneyPhase.Fade
        jump(plan.to)
        fade.animateTo(0f, tween(320))
        return true
    }

    /** الرفع ثم التقليبات: نقرة خفيفة مع كل ورقة، وأقوى حين تعبر الورقة بداية جزء */
    private suspend fun flip(plan: PageJourney, haptic: HapticFeedback, jump: suspend (Int) -> Unit): Boolean = coroutineScope {
        if (!capture()) return@coroutineScope false
        turn = 0
        phase = JourneyPhase.Flip
        launch { badge.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
        val rise = launch { lift.animateTo(LIFTED, tween(PageJourneys.LIFT_MS, easing = FastOutSlowInEasing)) }
        // الصفحة المقصودة تُعدّ الآن تحت الأوراق (وأثقل ما في الرحلة يقع في الرفع الهادئ)
        jump(plan.to)
        rise.join()
        for (k in plan.turns.indices) {
            fold.snapTo(0f)
            turn = k
            haptic.performHapticFeedback(
                if (crossesJuz(plan.pages[k], plan.pages[k + 1])) HapticFeedbackType.ContextClick else HapticFeedbackType.SegmentFrequentTick
            )
            fold.animateTo(1f, tween(plan.turns[k], easing = TurnEasing))
        }
        turn = plan.turns.size
        true
    }

    /** الهبوط بارتداد خفيف، والضوء الذهبي يمرّ مرة، ورقم الصفحة يتوهج، والميدالية تنبض ثم تختفي */
    private suspend fun land(reduce: Boolean): Unit = coroutineScope {
        launch { lift.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 420f)) }
        if (!reduce) launch {
            sweeping = true
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
            sweeping = false
        }
        launch {
            glow.animateTo(1f, tween(200))
            glow.animateTo(0f, tween(680))
        }
        if (badge.value > 0f) launch {
            pulse.animateTo(1.12f, tween(150))
            pulse.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
            badge.animateTo(0f, tween(260))
        }
        Unit
    }
}

@Composable
internal fun rememberPageJourney(): PageJourneyState {
    val start = rememberGraphicsLayer()
    val live = rememberGraphicsLayer()
    return remember(start, live) { PageJourneyState(start, live) }
}

/**
 * على HorizontalPager: يصوّر الصفحة الحالية حين تُطلب صورتها، ويسجّل الصفحات في [PageJourneyState.liveShot] بدل رسمها
 * ما دامت الأوراق تغطيها؛ وفي غير الرحلة يرسمها كما هي
 */
internal fun Modifier.journeySource(state: PageJourneyState): Modifier = drawWithContent {
    when (state.phase) {
        JourneyPhase.Capture -> {
            state.startShot.record { this@drawWithContent.drawContent() }
            state.captured = true
            drawContent()
        }
        JourneyPhase.Flip -> state.liveShot.record { this@drawWithContent.drawContent() }
        else -> drawContent()
    }
}

/** الكتاب يرتفع عن مكانه أثناء الرحلة ويهبط بعدها (يُوضع على ما يضم الصفحات والأوراق معاً) */
internal fun Modifier.journeyLift(state: PageJourneyState): Modifier = graphicsLayer {
    val s = state.lift.value
    scaleX = s
    scaleY = s
}

/** حدود ورقة الصفحة كما يضعها MushafPage: [half] null للصفحة الوحيدة، true لليمنى، false لليسرى */
private fun DrawScope.cardRect(frame: Dp, bottomInset: Dp, half: Boolean?): Rect {
    val w = minOf(frame.toPx(), size.width)
    val left = (size.width - w) / 2
    val right = left + w
    val mid = size.width / 2
    val top = 10.dp.toPx()
    val bottom = size.height - 8.dp.toPx() - bottomInset.toPx()
    val side = 12.dp.toPx()
    val gap = SPINE_GAP_J.toPx()
    return when (half) {
        null -> Rect(left + side, top, right - side, bottom)
        true -> Rect(mid + gap, top, right - side, bottom)
        false -> Rect(left + side, top, mid - gap, bottom)
    }
}

/** حدود الكتاب كله: الصفحة الوحيدة، أو الصفحتان المتقابلتان معاً */
private fun DrawScope.bookRect(frame: Dp, bottomInset: Dp, spread: Boolean): Rect =
    if (!spread) cardRect(frame, bottomInset, null)
    else cardRect(frame, bottomInset, false).let { l -> Rect(l.left, l.top, cardRect(frame, bottomInset, true).right, l.bottom) }

/** الهامش بين صفحتي الكتاب المفتوح عند الكعب (SPINE_GAP في SurahScreen) */
private val SPINE_GAP_J = 3.dp

/**
 * ما فوق الصفحات أثناء الرحلة: الأوراق تنقلب (والظل تحت الكتاب المرفوع)، أو صورة الصفحة الحالية تتلاشى، ثم الضوء الذهبي
 * وتوهج رقم الصفحة عند الهبوط. يُوضع فوق HorizontalPager في المكان نفسه وبالإطار نفسه، ويمنع اللمس ما دامت الرحلة جارية.
 */
@Composable
internal fun PageJourneyOverlay(state: PageJourneyState, frame: Dp, bottomInset: Dp, modifier: Modifier = Modifier) {
    val phase = state.phase
    if (phase == JourneyPhase.Idle) return
    val p = mushafPalette()
    Box(
        modifier.fillMaxSize()
            .clearAndSetSemantics {}
            // اللمس لا يصل إلى الصفحات تحت الأوراق
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
    ) {
        val plan = state.plan
        when {
            phase == JourneyPhase.Flip && plan != null -> FlipStage(state, plan, frame, bottomInset)
            phase == JourneyPhase.Fade -> Spacer(Modifier.fillMaxSize().graphicsLayer { alpha = state.fade.value }.drawBehind { drawStart(state) })
        }
        // الضوء الذهبي يمرّ مرة على الورقة، ورقم الصفحة في أسفلها يتوهج
        if (phase == JourneyPhase.Land) Spacer(
            Modifier.fillMaxSize().drawBehind {
                val half = if (state.spread) target(state.target) else null
                val card = cardRect(frame, bottomInset, half)
                if (state.sweeping) drawSweep(card, state.sweep.value, p)
                val g = state.glow.value
                if (g > 0f) {
                    val c = Offset(card.center.x, card.bottom - MEDALLION_CENTER.toPx())
                    val r = 34.dp.toPx() * (0.8f + 0.4f * g)
                    drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = .55f * g), p.gold.copy(alpha = 0f)), c, r), r, c)
                    drawCircle(p.goldDeep.copy(alpha = .7f * g), MEDALLION_SIZE.toPx() / 2 + 3.dp.toPx() * (2f - g), c, style = Stroke(1.4.dp.toPx()))
                }
            }
        )
    }
}

/** في الكتاب المفتوح: هل الصفحة يمنى */
private fun target(page: Int): Boolean = page % 2 == 1

/** ضوء ذهبي مائل يعبر الورقة من طرف إلى طرف ([t] من 0 إلى 1) */
private fun DrawScope.drawSweep(card: Rect, t: Float, p: MushafPalette) {
    val band = card.width * 0.55f
    val x = card.left - band + (card.width + 2 * band) * t
    val fadeIn = (t / 0.15f).coerceIn(0f, 1f) * ((1f - t) / 0.2f).coerceIn(0f, 1f)
    val brush = Brush.linearGradient(
        listOf(Color.Transparent, p.gold.copy(alpha = .28f * fadeIn), Color.White.copy(alpha = .22f * fadeIn), p.gold.copy(alpha = .28f * fadeIn), Color.Transparent),
        start = Offset(x - band / 2, card.top), end = Offset(x + band / 2, card.top + band * 0.6f),
    )
    clipRect(card.left, card.top, card.right, card.bottom) {
        drawRoundRect(brush, card.topLeft, card.size, CornerRadius(18.dp.toPx()))
    }
}

/**
 * الأوراق المنقلبة. الصفحة الواحدة: الورقة تدور حول طرفها الأيمن (الكعب) فتنكشف التي تحتها، وفي الرجوع تعود الورقة
 * السابقة فوقها. الكتاب المفتوح: الصفحة اليسرى تنقلب فوق الكعب إلى اليمين وظهرها الصفحة اليمنى من الكتاب التالي
 * (وفي الرجوع اليمنى إلى اليسار)، كالمصحف المطبوع.
 */
@Composable
private fun FlipStage(state: PageJourneyState, plan: PageJourney, frame: Dp, bottomInset: Dp) {
    val k = state.turn
    val n = plan.turns.size
    val spread = state.spread
    // الظل اللين تحت الكتاب المرفوع
    Spacer(
        Modifier.fillMaxSize().drawBehind {
            val a = ((1f - state.lift.value) / (1f - LIFTED)).coerceIn(0f, 1f)
            if (a <= 0f) return@drawBehind
            val card = bookRect(frame, bottomInset, spread)
            val c = Offset(card.center.x, card.center.y + 14.dp.toPx())
            val r = card.width * 0.62f
            scale(1f, card.height / card.width, pivot = c) {
                drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = .26f * a), Color.Transparent), c, r), r, c)
            }
        }
    )
    val sheet: @Composable (Int) -> Unit = { i ->
        when {
            i <= 0 -> Spacer(Modifier.fillMaxSize().drawBehind { drawStart(state) })
            i >= n -> LayerSheet(state.liveShot)
            else -> PlaceholderSheet(plan.sheets[i], spread, frame, bottomInset)
        }
    }
    if (k >= n) { sheet(n); return }
    val fwd = plan.forward
    // الكتاب المفتوح: وجه الورقة في النصف الأول من التقليبة وظهرها في الثاني (يُعاد التركيب عند الانتقال بينهما فقط)
    val firstHalf by remember(state) { derivedStateOf { state.fold.value < 0.5f } }
    if (!spread) {
        // الصفحة الواحدة: الورقة تدور حول الكعب في طرفها الأيمن
        val under = if (fwd) k + 1 else k
        val top = if (fwd) k else k + 1
        Leaf(state, frame, bottomInset, half = null, angle = { 0f }, shade = { (if (fwd) 1f - state.fold.value else state.fold.value) * 0.16f }) { sheet(under) }
        Leaf(
            state, frame, bottomInset, half = null,
            angle = { 90f * (if (fwd) state.fold.value else 1f - state.fold.value) },
            shade = { 0f },
        ) { sheet(top) }
    } else {
        // الكتاب المفتوح: الكعب في الوسط؛ الأمام: اليسرى تنقلب إلى اليمين، والرجوع: اليمنى إلى اليسار
        val revealedHalf = !fwd // النصف الذي ينكشف من تحته الكتاب الجديد: الأيسر في الأمام (false)، والأيمن في الرجوع
        val coveredHalf = fwd
        Leaf(state, frame, bottomInset, half = revealedHalf, angle = { 0f }, shade = { (1f - (state.fold.value * 2f).coerceIn(0f, 1f)) * 0.16f }) { sheet(k + 1) }
        Leaf(state, frame, bottomInset, half = coveredHalf, angle = { 0f }, shade = { ((state.fold.value - 0.5f) * 2f).coerceIn(0f, 1f) * 0.16f }) { sheet(k) }
        if (firstHalf) {
            // وجه الورقة: من الكتاب الحالي، يرتفع عن مكانه حتى يقف على الكعب
            Leaf(state, frame, bottomInset, half = revealedHalf, angle = { 90f * (state.fold.value * 2f).coerceIn(0f, 1f) }, shade = { 0f }) { sheet(k) }
        } else {
            // ظهرها: من الكتاب الجديد، يهبط على الجهة الأخرى
            Leaf(state, frame, bottomInset, half = coveredHalf, angle = { 90f * (1f - ((state.fold.value - 0.5f) * 2f).coerceIn(0f, 1f)) }, shade = { 0f }) { sheet(k + 1) }
        }
    }
}

/**
 * ورقة أو نصفها: [half] null الصفحة كلها (تدور حول طرفها الأيمن)، و true النصف الأيمن، و false الأيسر (يدوران حول الكعب
 * في الوسط). [angle] زاوية دورانها (0 مستوية، 90 قائمة على حافتها)، و[shade] ظل فوقها (ورقة تحت ورقة تنقلب).
 * الزاوية والظل يُقرآن في طور الرسم فلا يُعاد التركيب مع كل إطار.
 */
@Composable
private fun Leaf(
    state: PageJourneyState, frame: Dp, bottomInset: Dp, half: Boolean?,
    angle: () -> Float, shade: () -> Float, content: @Composable () -> Unit,
) {
    Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val wPx = size.width.coerceAtLeast(1f)
                    val a = angle()
                    // الطرف الحر يقترب من العين وهو يدور (الورقة تُرفع عن الكتاب)، والنصف الأيمن يدور بالعكس
                    rotationY = if (half == true) -a else a
                    cameraDistance = 14f * density
                    val pivot = if (half == null) ((wPx + minOf(frame.toPx(), wPx)) / 2 - 12.dp.toPx()) / wPx else 0.5f
                    transformOrigin = TransformOrigin(pivot.coerceIn(0f, 1f), 0.5f)
                    // القائمة على حافتها لا تُرى
                    alpha = if (a >= 89.5f) 0f else 1f
                }
                .drawWithContent {
                    val clip = when (half) {
                        null -> Rect(0f, 0f, size.width, size.height)
                        true -> Rect(size.width / 2, 0f, size.width, size.height)
                        false -> Rect(0f, 0f, size.width / 2, size.height)
                    }
                    clipRect(clip.left, clip.top, clip.right, clip.bottom) {
                        this@drawWithContent.drawContent()
                        // الورقة تعتم وهي تدور (الضوء يميل عنها)، والتي تحتها في ظل الورقة المرتفعة فوقها
                        val a = angle()
                        val dim = 0.26f * sin(a / 180f * PI.toFloat()) + shade()
                        if (dim > 0.004f) {
                            val card = cardRect(frame, bottomInset, half)
                            drawRoundRect(Color.Black.copy(alpha = dim.coerceAtMost(0.5f)), card.topLeft, card.size, CornerRadius(18.dp.toPx()))
                        }
                    }
                },
        ) { content() }
}

/** صورة مسجّلة (الصفحة الحالية، أو المقصودة تحت الأوراق) بمكانها نفسه */
@Composable
private fun LayerSheet(layer: GraphicsLayer) {
    Spacer(Modifier.fillMaxSize().drawBehind { drawLayer(layer) })
}

/** الصفحة التي بدأت منها الرحلة: صورتها الثابتة إن صُنعت، وإلا تسجيلها */
private fun DrawScope.drawStart(state: PageJourneyState) {
    val img = state.startImage
    if (img != null) drawImage(img) else drawLayer(state.startShot)
}

/** ورقة بين الطرفين: صفحة واحدة أو كتاب مفتوح بالإطار نفسه الذي يضعه HorizontalPager */
@Composable
private fun PlaceholderSheet(index: Int, spread: Boolean, frame: Dp, bottomInset: Dp) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.width(frame).fillMaxHeight()) {
            for (pg in Spreads.pages(index, spread)) {
                JourneyPage(
                    pg, bottomInset,
                    side = if (!spread) PageSide.Single else if (Spreads.isRight(pg)) PageSide.Right else PageSide.Left,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * ورقة خفيفة تمرّ في الرحلة: ورق المصحف بإطاره ونجومه، ورأس الصفحة الحقيقي، وأسطر باهتة كالكلمات من بعيد، ورقمها في
 * ميداليته؛ بلا نص يُخطَّط
 */
@Composable
private fun JourneyPage(page: Int, bottomInset: Dp, side: PageSide, modifier: Modifier) {
    val p = mushafPalette()
    val shape = when (side) {
        PageSide.Single -> RoundedCornerShape(18.dp)
        PageSide.Right -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
        PageSide.Left -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
    }
    val head = TextStyle(
        fontFamily = QuranFont, fontSize = 15.sp, lineHeight = 22.sp, textDirection = TextDirection.Rtl,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        modifier.fillMaxSize()
            .padding(start = if (side == PageSide.Left) SPINE_GAP_J else 12.dp, end = if (side == PageSide.Right) SPINE_GAP_J else 12.dp, top = 10.dp, bottom = 8.dp + bottomInset)
            .shadow(10.dp, shape, ambientColor = SheetShadow, spotColor = SheetShadow)
            .clip(shape)
            .drawBehind {
                drawPaper(p)
                if (side != PageSide.Single) drawSpine(innerAtLeft = side == PageSide.Right)
                drawPageFrame(p)
                drawGhostLines(page, p)
            }
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mushafHeadTitle(page), Modifier.weight(1f), style = head, color = p.goldDeep, maxLines = 1, overflow = TextOverflow.Clip)
                Text(mushafHeadSubtitle(page), style = head.copy(fontSize = 13.sp), color = p.muted, maxLines = 1)
            }
            Spacer(Modifier.fillMaxWidth().height(10.dp).drawBehind { drawRule(size.height / 2, 0f, size.width, p) })
        }
        PageMedallion(page, read = false, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = MEDALLION_CENTER - MEDALLION_SIZE / 2))
    }
}

/**
 * خمسة عشر سطراً باهتاً من «كلمات» بأطوال مختلفة (ثابتة لكل صفحة)، مضبوطة من الطرفين كأسطر المصحف، وبينها علامات آيات صغيرة
 */
private fun DrawScope.drawGhostLines(page: Int, p: MushafPalette) {
    val left = 20.dp.toPx(); val right = size.width - 20.dp.toPx()
    val top = (17 + 22 + 10 + 8).dp.toPx()
    val bottom = size.height - (MEDALLION_CENTER + MEDALLION_SIZE / 2 + 4.dp).toPx()
    if (right - left < 40f || bottom - top < 60f) return
    val rows = 15
    val step = (bottom - top) / rows
    val h = (step * 0.26f).coerceAtMost(5.dp.toPx())
    val gap = 4.dp.toPx()
    val ink = p.ink.copy(alpha = .09f)
    val mark = p.gold.copy(alpha = .55f)
    var seed = page * 7919 + 17
    fun next(): Float { seed = (seed * 1103515245 + 12345) and 0x7fffffff; return (seed % 1000) / 1000f }
    val width = right - left
    for (r in 0 until rows) {
        val y = top + step * (r + 0.5f)
        // كلمات السطر من اليمين إلى اليسار، ثم تُمدّ لتملأ السطر
        val words = ArrayList<Float>()
        var used = 0f
        while (used < width) {
            val w = width * (0.05f + 0.11f * next())
            words.add(w); used += w + gap
        }
        val scale = (width - gap * (words.size - 1)) / words.sumOf { it.toDouble() }.toFloat()
        var x = right
        for ((i, w0) in words.withIndex()) {
            val w = w0 * scale
            drawRoundRect(ink, Offset(x - w, y - h / 2), Size(w, h), CornerRadius(h / 2))
            x -= w + gap
            if (i < words.size - 1 && next() < 0.07f) drawCircle(mark, h * 0.9f, Offset(x + gap / 2, y), style = Stroke(0.8.dp.toPx()))
        }
    }
}

/**
 * الميدالية الذهبية فوق الكتاب أثناء الرحلة: رقم الصفحة يدور فيها كعدّاد (بالأرقام المشرقية كرقم صفحة المصحف) من الصفحة
 * الحالية إلى المقصودة، وتحتها اسم الجزء يعبر حين تتجاوز الأوراق بداية جزء
 */
@Composable
internal fun JourneyBadge(state: PageJourneyState, modifier: Modifier = Modifier) {
    if (state.phase != JourneyPhase.Flip && state.phase != JourneyPhase.Land) return
    if (state.plan?.kind != JourneyKind.Flip) return
    val s = LocalStrings.current
    val p = mushafPalette()
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)
    val digits = remember(measurer) { Array(10) { measurer.measure(('٠' + it).toString(), style) } }
    val juz by remember { derivedStateOf { JUZ_START_PAGES.indexOfLast { it <= state.rolling().toInt().coerceAtLeast(1) } + 1 } }
    Column(
        modifier.graphicsLayer {
            val b = state.badge.value
            alpha = b
            val sc = (0.86f + 0.14f * b) * state.pulse.value
            scaleX = sc; scaleY = sc
        }.clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(
            Modifier.size(104.dp).drawBehind {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = .45f), p.gold.copy(alpha = 0f)), center, r), r)
                val m = r * 0.78f
                drawCircle(Color.Black.copy(alpha = .18f), m, center + Offset(0f, 3.dp.toPx()))
                drawCircle(p.paper, m)
                drawCircle(p.fill.copy(alpha = .6f), m)
                drawCircle(p.goldDeep, m - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                drawCircle(p.gold.copy(alpha = .8f), m - 5.dp.toPx(), style = Stroke(0.9.dp.toPx()))
                drawStar8(center, m - 6.dp.toPx(), p.gold.copy(alpha = .35f), 1.dp.toPx(), rings = false)
                drawOdometer(state.rolling(), digits, p.goldDeep, center)
            }
        )
        AnimatedContent(
            juz,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(tween(220)) { if (up) it else -it } + fadeIn(tween(160))) togetherWith
                    (slideOutVertically(tween(220)) { if (up) -it else it } + fadeOut(tween(140)))
            },
            label = "journeyJuz",
        ) { j ->
            Text(
                s.juzName(j), color = p.goldDeep, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier.clip(CircleShape).background(p.paper.copy(alpha = .94f)).border(1.dp, p.gold.copy(alpha = .7f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * عدّاد كعدّاد المسافات: كل خانة شريط أرقام يدور، والآحاد تدور مع القيمة، وما فوقها يدور حين تبلغ ما تحته ٩ (فينتقل
 * ١٩٩ إلى ٢٠٠ بدوران الخانات معاً)؛ والأصفار في أول الرقم لا تُرسم
 */
private fun DrawScope.drawOdometer(value: Float, digits: Array<TextLayoutResult>, color: Color, center: Offset) {
    val v = value.coerceAtLeast(0f)
    val whole = floor(v).toLong()
    val frac = v - whole
    val count = maxOf(1, whole.toString().length)
    val dw = digits.maxOf { it.size.width }.toFloat()
    val dh = digits.maxOf { it.size.height }.toFloat()
    val left = center.x - dw * count / 2
    val top = center.y - dh / 2
    var pow = 1L
    repeat(count - 1) { pow *= 10 }
    for (i in 0 until count) {
        val c = whole / pow
        val d = (c % 10).toInt()
        val f = if (pow == 1L || whole % pow == pow - 1) frac else 0f
        val x = left + i * dw
        clipRect(x, top, x + dw, top + dh) {
            if (pow == 1L || c > 0) {
                val g = digits[d]
                drawText(g, color, Offset(x + (dw - g.size.width) / 2, top - f * dh))
            }
            if (f > 0f) {
                val g = digits[(d + 1) % 10]
                drawText(g, color, Offset(x + (dw - g.size.width) / 2, top + (1f - f) * dh))
            }
        }
        pow /= 10
    }
}

/** شريط الوصول أعلى القارئ: «الأعراف · الجزء 9 · الصفحة 173» بين نجمتين ذهبيتين، يقرؤه قارئ الشاشة حين يظهر */
@Composable
internal fun ArrivalBanner(text: String, modifier: Modifier = Modifier) {
    val p = mushafPalette()
    Row(
        modifier.widthIn(max = 420.dp)
            .shadow(8.dp, CircleShape, ambientColor = SheetShadow, spotColor = SheetShadow)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(p.paper, p.fill)))
            .border(1.dp, p.gold.copy(alpha = .8f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite; contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(12.dp).drawBehind { drawStar8(center, size.minDimension / 2, p.goldDeep, 1.dp.toPx(), rings = false) })
        Spacer(Modifier.width(8.dp))
        Text(
            text, color = p.ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(8.dp))
        Spacer(Modifier.size(12.dp).drawBehind { drawStar8(center, size.minDimension / 2, p.goldDeep, 1.dp.toPx(), rings = false) })
    }
}

/** «إزالة الحركات» في النظام: أندرويد (مدة الحركات صفر) و iOS (Reduce Motion) */
internal expect fun systemReduceMotion(): Boolean
