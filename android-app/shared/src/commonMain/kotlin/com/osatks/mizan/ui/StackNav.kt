package com.osatks.mizan.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** صفحة فوق التبويب في [StackNav] */
class NavPage internal constructor(id: Long, val route: String) {
    val key = "p$id"
}

/**
 * التنقل في iOS كما في UINavigationController داخل UITabBarController: صفحات التبويبات تبقى حية بعد أول فتح
 * (التبديل بينها فوري بلا إعادة بناء)، وكل صفحة تُفتح فوق التبويب تبقى الصفحة التي تحتها حية أيضاً، فيبدأ الرجوع
 * فوراً ويتبع الإصبع بلا انتظار بناء الصفحة السابقة. [p]: ظهور الصفحة العليا (1 ظاهرة كلها، 0 خارج الشاشة).
 */
@Stable
class StackNav(startTab: String) {
    var tab by mutableStateOf(startTab)
        private set
    /** الصفحات فوق التبويب، آخرها الظاهرة */
    val pages = mutableStateListOf<NavPage>()
    /** التبويبات التي فُتحت مرة: تبقى مركّبة */
    internal val tabsShown = mutableStateListOf(startTab)

    internal var p by mutableFloatStateOf(1f)
    /** انتقال جارٍ (فتح أو رجوع أو سحب): الصفحة تحت العليا ظاهرة معها */
    internal var moving by mutableStateOf(false)
    /** الصفحة الخارجة الآن: لم تعد الحالية، وتُزال حين تخرج كلها */
    internal var leaving by mutableStateOf<NavPage?>(null)
    /** أثناء سحب الحافة: جهة حركة الإصبع (1 نحو اليمين، -1 نحو اليسار)، فتتبعه الصفحة أياً كانت حافته */
    internal var swipeDir by mutableStateOf<Float?>(null)

    internal var scope: CoroutineScope? = null
    /** تبويبات قيست مرة وهي مخفية (تُبنى قوائمها مسبقاً فيظهر التبويب أول مرة فوراً) */
    internal val warmed = HashSet<String>()
    internal var onRemoved: ((String) -> Unit)? = null
    private var nextId = 1L
    private var job: Job? = null
    private var done: (() -> Unit)? = null

    /** المسار الظاهر؛ أثناء خروج صفحة: التي يُرجع إليها */
    val current: String get() = pages.lastOrNull { it !== leaving }?.route ?: tab

    /** صفحة تبويب ظاهرة (لا صفحة فوقها) */
    val onTab: Boolean get() = pages.none { it !== leaving }

    internal val topKey: String get() = pages.lastOrNull()?.key ?: tabKey(tab)

    internal val belowKey: String?
        get() = when (pages.size) {
            0 -> null
            1 -> tabKey(tab)
            else -> pages[pages.size - 2].key
        }

    // ---------- العمليات ----------

    /** يفتح صفحة فوق الحالية (نقرتان سريعتان لا تفتحانها مرتين) */
    fun push(route: String) {
        settle()
        if ((pages.lastOrNull()?.route ?: tab) == route) return
        pages += NavPage(nextId++, route)
        p = 0f
        run(1f, tween(PUSH_MS, easing = Push), prepare = true) { p = 1f; moving = false }
    }

    /** يفتح [route] مكان الصفحة العليا (نتيجة من البحث): تنزلق فوقها ثم تُزال التي تحتها */
    fun replace(route: String) {
        settle()
        val old = pages.lastOrNull() ?: return push(route)
        pages += NavPage(nextId++, route)
        p = 0f
        run(1f, tween(PUSH_MS, easing = Push), prepare = true) {
            p = 1f
            moving = false
            pages.remove(old)
            onRemoved?.invoke(old.key)
        }
    }

    /** يرجع صفحة واحدة؛ false إن كانت صفحة التبويب ظاهرة */
    fun pop(): Boolean {
        settle()
        val top = pages.lastOrNull() ?: return false
        leaving = top
        run(0f, tween(POP_MS, easing = Pop), prepare = true) { remove(top) }
        return true
    }

    /** ينتقل إلى تبويب فوراً، وتُغلق صفحات التبويب الحالي (كاختيار تبويب من الشريط) */
    fun goTab(t: String) {
        settle()
        clear()
        if (t !in tabsShown) tabsShown += t
        tab = t
    }

    /** يُغلق كل ما فوق التبويب بلا حركة (رابط من أداة أو اختصار) */
    fun reset() {
        settle()
        clear()
    }

    /** يركّب تبويباً قبل أن يُفتح (بعد الإقلاع)، فيظهر أول مرة فوراً */
    internal fun prepare(t: String) {
        if (t !in tabsShown) tabsShown += t
    }

    /** سحب الحافة: الصفحة تتبع الإصبع، ثم تُكمل الخروج أو تعود مكانها */
    suspend fun gesture(progress: Flow<BackSwipe>) {
        settle()
        val top = pages.lastOrNull() ?: return
        moving = true
        try {
            progress.collect { e ->
                if (swipeDir == null) swipeDir = if (e.fromLeft) 1f else -1f
                p = 1f - e.progress
            }
        } catch (e: CancellationException) {
            // تراجع المستخدم: تعود الصفحة إلى مكانها من حيث تركها الإصبع
            run(1f, tween(((1f - p) * POP_MS).toInt().coerceIn(90, POP_MS), easing = Settle)) { p = 1f; moving = false; swipeDir = null }
            throw e
        }
        leaving = top
        run(0f, tween((p * POP_MS).toInt().coerceIn(90, POP_MS), easing = Settle)) { remove(top) }
    }

    // ---------- داخلي ----------

    private fun clear() {
        if (pages.isEmpty()) return
        val keys = pages.map { it.key }
        pages.clear()
        leaving = null
        p = 1f
        moving = false
        swipeDir = null
        keys.forEach { onRemoved?.invoke(it) }
    }

    private fun remove(page: NavPage) {
        pages.remove(page)
        if (leaving === page) leaving = null
        p = 1f
        moving = false
        swipeDir = null
        onRemoved?.invoke(page.key)
    }

    /**
     * يحرّك [p] إلى [to] ثم ينفذ [then]. [prepare]: تبدأ الحركة بعد إطارين، أي بعد أن تُبنى الصفحة الجديدة
     * وتُقاس التي تحتها، فلا يقفز أول إطار من الحركة بقدر ما استغرقه البناء
     */
    private fun run(to: Float, spec: AnimationSpec<Float>, prepare: Boolean = false, then: () -> Unit) {
        done = then
        moving = true
        val s = scope
        if (s == null) { p = to; finish(); return }
        job = s.launch {
            if (prepare) { withFrameNanos { }; withFrameNanos { } }
            animate(p, to, animationSpec = spec) { v, _ -> p = v }
            finish()
        }
    }

    private fun finish() {
        val d = done
        done = null
        job = null
        d?.invoke()
    }

    /** يُنهي الحركة الجارية فوراً قبل عملية جديدة (لا تتداخل حركتان) */
    private fun settle() {
        val j = job
        job = null
        j?.cancel()
        finish()
    }

    internal companion object {
        fun tabKey(t: String) = "t:$t"
        const val PUSH_MS = 440
        const val POP_MS = 380
        /** منحنى الدفع في UIKit تقريباً: بداية سريعة وتوقف ناعم */
        val Push = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
        val Pop = CubicBezierEasing(0.3f, 0.6f, 0.2f, 1f)
        /** بعد ترك الإصبع: يكمل بسرعته ويتوقف بنعومة */
        val Settle = CubicBezierEasing(0.1f, 0.5f, 0.3f, 1f)
    }
}

/**
 * يعرض [StackNav]: كل صفحة حية في مكانها، ولا يُقاس ولا يُرسم إلا الظاهر منها (العليا، والتي تحتها أثناء الانتقال).
 * الصفحة العليا تدخل كاملة من جهة القراءة وتنزاح التي تحتها ثلثاً وتعتم قليلاً، كما في تطبيقات النظام.
 */
@Composable
fun StackNavHost(nav: StackNav, modifier: Modifier = Modifier, frame: @Composable (route: String, tab: Boolean, content: @Composable () -> Unit) -> Unit = { _, _, c -> c() }, content: @Composable (route: String) -> Unit) {
    val holder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    SideEffect { nav.scope = scope }
    DisposableEffect(holder) {
        nav.onRemoved = { holder.removeState(it) }
        onDispose { nav.onRemoved = null }
    }
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    PredictiveBack(enabled = nav.pages.isNotEmpty() && nav.leaving == null) { progress -> nav.gesture(progress) }

    val topKey = nav.topKey
    val belowKey = if (nav.moving) nav.belowKey else null
    val leavingKey = nav.leaving?.key
    Layout(
        modifier = modifier,
        content = {
            nav.tabsShown.forEach { t ->
                val k = StackNav.tabKey(t)
                key(k) { Page(nav, k, topKey, belowKey, leavingKey, dir, holder) { frame(t, true) { content(t) } } }
            }
            nav.pages.forEach { page ->
                key(page.key) { Page(nav, page.key, topKey, belowKey, leavingKey, dir, holder) { frame(page.route, false) { content(page.route) } } }
            }
            // لا لمس أثناء الانتقال (نقرة على الصفحة الخارجة كانت تفتح شيئاً آخر)
            if (nav.moving) Box(Modifier.layoutId(BLOCKER).fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        e.changes.forEach { it.consume() }
                        if (e.changes.none { it.pressed }) break
                    }
                }
            })
        },
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val fixed = Constraints.fixed(w, h)
        var under: androidx.compose.ui.layout.Placeable? = null
        var over: androidx.compose.ui.layout.Placeable? = null
        var blocker: androidx.compose.ui.layout.Placeable? = null
        for (m in measurables) {
            val id = m.layoutId
            when {
                id == topKey -> over = m.measure(fixed)
                id == belowKey -> under = m.measure(fixed)
                id == BLOCKER -> blocker = m.measure(fixed)
                // تبويب رُكّب مسبقاً ولم يظهر بعد: يُقاس مرة دون أن يُرسم، فتُبنى قائمته الآن لا لحظة فتحه
                id is String && id.startsWith("t:") && nav.warmed.add(id) -> m.measure(fixed)
            }
        }
        layout(w, h) {
            under?.place(0, 0)
            over?.place(0, 0)
            blocker?.place(0, 0)
        }
    }
}

private const val BLOCKER = "blocker"

/** صفحة واحدة: حالتها المحفوظة باسمها، وحركتها في طبقة الرسم فقط (لا يُعاد قياس شيء مع كل إطار) */
@Composable
private fun Page(
    nav: StackNav, key: String, topKey: String, belowKey: String?, leavingKey: String?, dir: Float,
    holder: SaveableStateHolder, content: @Composable () -> Unit,
) {
    val top = key == topKey
    val below = key == belowKey
    val motion = when {
        top -> Modifier
            .graphicsLayer { translationX = (nav.swipeDir ?: dir) * (1f - nav.p) * size.width }
            .drawBehind {
                // ظل خفيف على حافة الصفحة الداخلة أثناء الانتقال
                if (nav.moving) {
                    val d = nav.swipeDir ?: dir
                    val shadow = 14.dp.toPx()
                    val a = 0.16f * nav.p
                    val x = if (d > 0) -shadow else size.width
                    drawRect(
                        Brush.horizontalGradient(
                            if (d > 0) listOf(Color.Transparent, Color.Black.copy(alpha = a)) else listOf(Color.Black.copy(alpha = a), Color.Transparent),
                            startX = x, endX = x + shadow,
                        ),
                        topLeft = Offset(x, 0f), size = Size(shadow, size.height),
                    )
                }
            }
        below -> Modifier
            .graphicsLayer { translationX = -(nav.swipeDir ?: dir) * 0.3f * nav.p * size.width }
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = 0.1f * nav.p))
            }
        else -> Modifier
    }
    Box(Modifier.layoutId(key).fillMaxSize().then(motion).background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalPageActive provides (top && key != leavingKey)) {
            holder.SaveableStateProvider(key) { content() }
        }
    }
}
