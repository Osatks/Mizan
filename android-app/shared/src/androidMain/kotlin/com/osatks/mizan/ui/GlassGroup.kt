package com.osatks.mizan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osatks.mizan.ui.theme.LocalDark

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
