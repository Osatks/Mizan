package com.osatks.mizan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.ui.theme.LocalDark
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** يعمل التطبيق على iOS (لمسات خاصة بالنظام: شكل المفاتيح، أحجام الخطوط، سحب الرجوع) */
val IsIos: Boolean = Platform.name == "ios"

/**
 * مصدر التمويه على مستوى التطبيق: محتوى الصفحات (NavHost) الذي يظهر مموَّهاً خلف شريط التنقل والأوراق السفلية.
 * بلا مصدر (خارج التطبيق، أو في صفحة المصحف حيث يُعطَّل المصدر) تُرسم الطبقة الزجاجية لوناً شبه معتم.
 */
val LocalAppHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * الزجاج: تمويه حقيقي لما خلف العنصر (RenderEffect في أندرويد 12+، و Skia في iOS) مع طبقة لون شفافة من لون السطح،
 * فيتبع الوضع الداكن ولون التطبيق. في أندرويد الأقدم يُرسم [fallback] بدل التمويه.
 * نصف القطر محدود (24–32dp) والتمويه يُحسب بثلث الدقة، فالكلفة صغيرة حتى بـ120 إطاراً في الثانية.
 */
@Composable
@ReadOnlyComposable
fun glassStyle(tint: Float, blur: Dp = 24.dp, fallback: Float = .96f): HazeStyle {
    val c = MaterialTheme.colorScheme
    return HazeStyle(
        backgroundColor = c.background,
        tints = listOf(HazeTint(c.surface.copy(alpha = tint))),
        blurRadius = blur,
        noiseFactor = 0f,
        fallbackTint = HazeTint(c.surface.copy(alpha = fallback)),
    )
}

/** ألوان الحافة اللامعة واللمعة العلوية للزجاج */
class GlassRim(val top: Color, val bottom: Color, val sheen: Color)

@Composable
@ReadOnlyComposable
fun glassRim(strength: Float = 1f): GlassRim {
    val dark = LocalDark.current
    return GlassRim(
        top = Color.White.copy(alpha = (if (dark) .24f else .95f) * strength),
        bottom = Color.White.copy(alpha = (if (dark) .05f else .40f) * strength),
        sheen = Color.White.copy(alpha = (if (dark) .06f else .30f) * strength),
    )
}

/**
 * سطح زجاجي بشكل [shape]: تمويه ما خلفه من [state]، ولمعة خفيفة في أعلاه، وحافة مضيئة من الأعلى تخفت نحو الأسفل
 * (كضوء ينعكس على حافة الزجاج). إن لم يوجد مصدر تمويه يُرسم لون [style] الاحتياطي.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glass(state: HazeState?, shape: Shape, style: HazeStyle, rim: GlassRim = glassRim(), rimWidth: Dp = 1.dp): Modifier =
    this
        .clip(shape)
        .then(
            // بلا مصدر مسجَّل (صفحة المصحف) لا شيء يُموَّه، فيُرسم اللون الاحتياطي بدل سطح شفاف فارغ
            if (state != null && state.areas.isNotEmpty()) Modifier.hazeEffect(state, style) { inputScale = HazeInputScale.Auto }
            else Modifier.background(style.fallbackTint.color)
        )
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val sheen = Brush.verticalGradient(0f to rim.sheen, .55f to Color.Transparent)
            val edge = Brush.verticalGradient(listOf(rim.top, rim.bottom))
            // نصف الخط خارج القص، فالظاهر منه بعرض rimWidth
            val stroke = Stroke(rimWidth.toPx() * 2)
            onDrawWithContent {
                drawOutline(outline, sheen)
                drawContent()
                drawOutline(outline, edge, style = stroke)
            }
        }

// ---------- الزجاج السائل للأسطح العائمة (القوائم والنوافذ واللوحات وشريط خيارات الآية) ----------

/** درجة الزجاج: فاتح فوق المحتوى الفاتح وداكن فوق الداكن (كالزجاج الذي يتبع ما تحته) */
enum class GlassTone { Light, Dark }

/** درجة الزجاج في هذا الجزء من الواجهة؛ null = حسب مظهر التطبيق (شاشة القراءة توفّرها من لون الورق) */
val LocalGlassTone = staticCompositionLocalOf<GlassTone?> { null }

@Composable
@ReadOnlyComposable
fun glassTone(): GlassTone = LocalGlassTone.current ?: if (LocalDark.current) GlassTone.Dark else GlassTone.Light

/**
 * مادة الزجاج العائم: تمويه بنصف قطر محدود (كشريط التنقل)، وتشبّع أعلى قليلاً لما خلفه (كانكسار الضوء في عدسة)،
 * ثم طبقة لون خفيفة بحسب [glassTone] أو بلون [color] (ورق المصحف مثلاً). [tint] كثافة الطبقة، و[fallback] لونها حين لا تمويه.
 */
@Composable
@ReadOnlyComposable
fun glassMaterial(tint: Float, color: Color? = null, blur: Dp = 24.dp, fallback: Float = .97f): HazeStyle {
    val dark = glassTone() == GlassTone.Dark
    val base = color ?: if (dark) Color(0xFF1B2421) else Color(0xFFF7FAF8)
    return HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tints = listOf(
            // تشبّع: يزيد ألوان ما خلف الزجاج حيوية دون أن يلوّنه
            HazeTint(Color(0xFFFF2D55).copy(alpha = if (dark) .16f else .22f), BlendMode.Saturation),
            HazeTint(base.copy(alpha = tint)),
        ),
        blurRadius = blur,
        noiseFactor = 0f,
        fallbackTint = HazeTint(base.copy(alpha = fallback)),
    )
}

/** حافة الزجاج العائم: خط رفيع لامع يشتد في الحافة العليا ويخفت نحو الأسفل */
@Composable
@ReadOnlyComposable
fun floatingRim(): GlassRim {
    val dark = glassTone() == GlassTone.Dark
    return GlassRim(
        top = Color.White.copy(alpha = if (dark) .34f else .98f),
        bottom = Color.White.copy(alpha = if (dark) .06f else .38f),
        sheen = Color.White.copy(alpha = if (dark) .07f else .34f),
    )
}

/**
 * سطح زجاجي عائم: [glass] مع ظل ناعم واسع، ولمعة عدسة في الزاوية العليا من جهة البداية تعطي الإحساس بسُمك الزجاج.
 * التمويه لهذا السطح الصغير وحده.
 */
@Composable
fun Modifier.floatingGlass(state: HazeState?, shape: Shape, style: HazeStyle, elevation: Dp = 18.dp): Modifier {
    val dark = glassTone() == GlassTone.Dark
    val lens = Color.White.copy(alpha = if (dark) .07f else .16f)
    return this
        .shadow(
            elevation, shape,
            ambientColor = Color.Black.copy(alpha = if (dark) .50f else .12f),
            spotColor = Color.Black.copy(alpha = if (dark) .60f else .22f),
        )
        .glass(state, shape, style, floatingRim(), rimWidth = 0.8.dp)
        .drawWithContent {
            drawContent()
            // ضوء ينكسر عند الزاوية العليا كما في العدسة
            val rtl = layoutDirection == LayoutDirection.Rtl
            drawRect(
                Brush.radialGradient(
                    listOf(lens, Color.Transparent),
                    center = Offset(if (rtl) size.width * .85f else size.width * .15f, 0f),
                    radius = size.maxDimension * .6f,
                )
            )
        }
}

/**
 * ضغطة «مرنة» كأزرار iOS 26: ينضغط العنصر قليلاً عند اللمس ثم يرتد بنابض يتجاوز موضعه قليلاً عند الرفع
 */
fun Modifier.squish(source: InteractionSource, pressed: Float = 0.94f): Modifier = composed {
    val down by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (down) pressed else 1f,
        if (down) spring(dampingRatio = 1f, stiffness = 900f) else spring(dampingRatio = .45f, stiffness = 420f),
        label = "squish"
    )
    graphicsLayer { scaleX = s; scaleY = s }
}

// ---------- القائمة المنبثقة ----------

/** عنصر في [GlassMenu] */
sealed interface MenuEntry

/** خيار في القائمة: أيقونة اختيارية في البداية، وعلامة صح في النهاية للمختار، وأحمر للحذف وما شابهه */
class MenuItem(
    val text: String, val icon: ImageVector? = null, val checked: Boolean = false,
    val destructive: Boolean = false, val onClick: () -> Unit,
) : MenuEntry

/** فاصل بين مجموعتين من الخيارات (أعرض من الخط بين الخيارات) */
object MenuGroupGap : MenuEntry

private val MenuRadius = 22.dp
private val MenuPad = 6.dp

/**
 * قائمة منبثقة من الزجاج بدل DropdownMenu: تُستدعى داخل Box فيه ما تنبثق منه، فتظهر تحته (أو فوقه إن لم يتسع ما تحته)
 * بمحاذاة طرف البداية منه (اليمين في العربية)، وتكبر من جهته بنابض يتجاوز قليلاً ثم يستقر، وتخفت عند الإغلاق.
 * زوايا الخيارات متّحدة المركز مع زوايا القائمة (22 − 6 = 16)، وكل خيار بارتفاع 44 على الأقل، وبين الخيارات خط رفيع.
 * تبقى مرتبطة بما انبثقت منه على الشاشات العريضة أيضاً، وتُمرَّر إن طالت (وتبدأ عند الخيار المختار).
 * اختيار خيار يغلقها بعد تنفيذه، مع نقرة اهتزاز خفيفة.
 */
@Composable
fun GlassMenu(
    expanded: Boolean, onDismissRequest: () -> Unit, entries: List<MenuEntry>,
    title: String? = null, width: Dp = 250.dp, maxHeight: Dp = 400.dp,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = expanded
    if (!state.currentState && !state.targetState) return
    val density = LocalDensity.current
    val window = LocalWindowLayout.current
    var origin by remember { mutableStateOf(TransformOrigin(0f, 0f)) }
    val provider = remember(density) {
        MenuPosition(with(density) { 6.dp.roundToPx() }, with(density) { 12.dp.roundToPx() }) { origin = it }
    }
    val haze = LocalAppHaze.current
    val haptic = LocalHapticFeedback.current
    Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        val t = rememberTransition(state, label = "glassMenu")
        val scale by t.animateFloat(
            { if (targetState) spring(dampingRatio = .68f, stiffness = 520f) else tween(130, easing = Motion.Accelerate) }, label = "menuScale"
        ) { if (it) 1f else .82f }
        val alpha by t.animateFloat({ tween(if (targetState) 140 else 120) }, label = "menuAlpha") { if (it) 1f else 0f }
        val shape = RoundedCornerShape(MenuRadius)
        val selected = entries.indexOfFirst { it is MenuItem && it.checked }
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (selected - 2).coerceAtLeast(0))
        Column(
            Modifier
                .width(width.coerceAtMost(window.width - 24.dp))
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha; transformOrigin = origin }
                .floatingGlass(haze, shape, glassMaterial(if (glassTone() == GlassTone.Dark) .62f else .66f))
                .padding(MenuPad)
        ) {
            if (title != null) {
                Text(
                    title, Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2
                )
            }
            LazyColumn(Modifier.heightIn(max = maxHeight.coerceAtMost(window.height - 96.dp)), state = list) {
                itemsIndexed(entries) { i, e ->
                    when (e) {
                        is MenuItem -> {
                            if (i > 0 && entries[i - 1] is MenuItem) MenuHairline()
                            MenuRow(e) {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                e.onClick()
                                onDismissRequest()
                            }
                        }
                        MenuGroupGap -> Box(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).height(6.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .06f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuHairline() {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(0.6.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
    )
}

@Composable
private fun MenuRow(e: MenuItem, onPick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    val fg = if (e.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val press by animateColorAsState(
        if (down) MaterialTheme.colorScheme.onSurface.copy(alpha = .09f) else Color.Transparent, tween(90), label = "menuPress"
    )
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp)
            .squish(src, .97f)
            .clip(RoundedCornerShape(MenuRadius - MenuPad))
            .background(press)
            .semantics { selected = e.checked }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (e.icon != null) {
            Icon(e.icon, null, Modifier.size(20.dp), tint = fg)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            e.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = fg,
            fontWeight = if (e.checked) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (e.checked) {
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * موضع القائمة: تحت ما انبثقت منه بمحاذاة طرف البداية (الأيمن في العربية)، أو فوقه إن لم يتسع ما تحته،
 * وداخل النافذة دائماً بهامش. يُبلغ [onOrigin] بنقطة ارتكاز حركة الظهور (طرف القائمة الملاصق لما انبثقت منه).
 */
private class MenuPosition(val gap: Int, val margin: Int, val onOrigin: (TransformOrigin) -> Unit) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val w = popupContentSize.width
        val h = popupContentSize.height
        val x = (if (rtl) anchorBounds.right - w else anchorBounds.left)
            .coerceIn(margin, (windowSize.width - w - margin).coerceAtLeast(margin))
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - h
        val down = below + h <= windowSize.height - margin || above < margin
        val y = if (down) below.coerceAtMost((windowSize.height - h - margin).coerceAtLeast(margin)) else above
        onOrigin(TransformOrigin(if (rtl) 1f else 0f, if (down) 0f else 1f))
        return IntOffset(x, y)
    }
}

// ---------- نافذة التنبيه ----------

/** دور الزر في نافذة التنبيه: عادي، أو المفضَّل (عريض بلون التطبيق)، أو الإلغاء، أو الحذف (أحمر) */
enum class ActionRole { Normal, Preferred, Cancel, Destructive }

/** زر في نافذة التنبيه؛ يُغلق النافذة بعد تنفيذه (على من يعرضها أن يُخفيها في [onClick]) */
class DialogAction(val text: String, val role: ActionRole = ActionRole.Normal, val onClick: () -> Unit)

/**
 * نافذة تنبيه: في iOS نافذة النظام نفسها (UIAlertController، فتأخذ زجاج النظام في iOS 26 تلقائياً)،
 * وفي أندرويد [GlassDialog]. كل زر يُغلق النافذة؛ و[onDismissRequest] للإغلاق بلمس ما حولها أو بالرجوع (أندرويد).
 */
@Composable
expect fun PlatformDialog(onDismissRequest: () -> Unit, title: String, message: String? = null, actions: List<DialogAction>)

/**
 * نافذة تنبيه زجاجية في الوسط: العنوان والرسالة، ثم أزرار كبسولية (متجاورة لزرين، ومتراصّة لثلاثة فأكثر):
 * المفضَّل ممتلئ بلون التطبيق وعريض، والحذف أحمر. تدخل بنابض يتجاوز قليلاً وتخرج بتلاشٍ، ثم يُنفَّذ الزر.
 * زوايا الأزرار متّحدة المركز مع زوايا النافذة (38 − 14 = 24، نصف ارتفاع الكبسولة).
 */
@Composable
fun GlassDialog(onDismissRequest: () -> Unit, title: String, message: String? = null, actions: List<DialogAction>) {
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { state.targetState = true }
    var after by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun close(then: () -> Unit) { if (after == null) { after = then; state.targetState = false } }
    // بعد حركة الخروج يُنفَّذ الزر (فيُخفي صاحب النافذة حالته)
    LaunchedEffect(state.isIdle, state.currentState) {
        if (state.isIdle && !state.currentState) after?.let { a -> after = null; a() }
    }
    val haze = LocalAppHaze.current
    val window = LocalWindowLayout.current
    Dialog(onDismissRequest = { close(onDismissRequest) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val t = rememberTransition(state, label = "glassDialog")
        val scale by t.animateFloat(
            { if (targetState) spring(dampingRatio = .62f, stiffness = 380f) else tween(150, easing = Motion.Accelerate) }, label = "dialogScale"
        ) { if (it) 1f else .88f }
        val alpha by t.animateFloat({ tween(if (targetState) 160 else 140) }, label = "dialogAlpha") { if (it) 1f else 0f }
        val shape = RoundedCornerShape(38.dp)
        Column(
            Modifier
                .padding(28.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .heightIn(max = window.height * .8f)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .floatingGlass(haze, shape, glassMaterial(if (glassTone() == GlassTone.Dark) .72f else .78f, fallback = 1f), elevation = 28.dp)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (message != null) Text(
                    message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            // زران قصيران متجاوران (الأول في جهة البداية)، وغير ذلك فوق بعض
            val sideBySide = actions.size == 2 && actions.all { it.text.length <= 16 }
            if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.forEach { a -> DialogButton(a, Modifier.weight(1f)) { close(a.onClick) } }
            } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.forEach { a -> DialogButton(a, Modifier.fillMaxWidth()) { close(a.onClick) } }
            }
        }
    }
}

@Composable
private fun DialogButton(a: DialogAction, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val c = MaterialTheme.colorScheme
    val dark = glassTone() == GlassTone.Dark
    val (bg, fg) = when (a.role) {
        ActionRole.Preferred -> c.primary to c.onPrimary
        ActionRole.Destructive -> c.error.copy(alpha = if (dark) .20f else .12f) to c.error
        else -> c.onSurface.copy(alpha = if (dark) .12f else .07f) to c.onSurface
    }
    Box(
        modifier.heightIn(min = 48.dp).squish(src).clip(CircleShape).background(bg)
            .clickable(interactionSource = src, indication = if (IsIos) null else ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            a.text, color = fg, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, maxLines = 2,
            fontWeight = if (a.role == ActionRole.Normal) FontWeight.Medium else FontWeight.SemiBold,
        )
    }
}

// ---------- مجموعات الإعدادات ----------

/**
 * مجموعة بأسلوب iOS (inset grouped): عنوان صغير فوقها، وبطاقة زجاجية بحافة لامعة ولمعة علوية
 * (بلا تمويه: خلفها خلفية الصفحة الثابتة)، وصفوفها مفصولة بخطوط رفيعة [GroupDivider].
 */
@Composable
fun GlassGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalDark.current
    val shape = RoundedCornerShape(22.dp)
    val rim = glassRim(if (dark) 1f else .9f)
    val fill = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) .82f else .88f)
    Column(modifier.fillMaxWidth()) {
        if (title != null) Text(
            title, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            Modifier.fillMaxWidth()
                .shadow(
                    6.dp, shape,
                    ambientColor = Color.Black.copy(alpha = if (dark) .30f else .06f),
                    spotColor = Color.Black.copy(alpha = if (dark) .40f else .10f),
                )
                .clip(shape)
                .background(fill)
                .drawWithCache {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val sheen = Brush.verticalGradient(0f to rim.sheen, .45f to Color.Transparent)
                    val edge = Brush.verticalGradient(listOf(rim.top, rim.bottom))
                    val stroke = Stroke(1.6.dp.toPx())
                    onDrawWithContent {
                        drawOutline(outline, sheen)
                        drawContent()
                        drawOutline(outline, edge, style = stroke)
                    }
                },
            content = content,
        )
    }
}

/** خط رفيع بين صفوف [GlassGroup]، يبدأ بعد أيقونة الصف كما في iOS */
@Composable
fun GroupDivider(inset: Dp = 60.dp) {
    Box(
        Modifier.fillMaxWidth().padding(start = inset).height(0.6.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
    )
}

/** صف في [GlassGroup]: أيقونة في مربع ملوّن، والعنوان وتحته وصف، وما يلحقه في النهاية (سهم أو مفتاح)؛ ارتفاعه 52 على الأقل */
@Composable
fun GroupRow(title: String, subtitle: String? = null, onClick: () -> Unit, leading: ImageVector? = null, trailing: @Composable () -> Unit = {}) {
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    val press by animateColorAsState(if (down) MaterialTheme.colorScheme.onSurface.copy(alpha = .07f) else Color.Transparent, tween(90), label = "rowPress")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).background(press)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(leading, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
