package com.osatks.mizan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.theme.HeroBrush
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** حقل عددي بطراز iPhone: العنوان فوقه، وخلفية فاتحة بحافة رفيعة تصير بلون التطبيق أثناء الكتابة، والوحدة في نهايته */
@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String? = null, hint: String? = null, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val edge by animateColorAsState(if (focused) cs.primary else cs.outlineVariant, tween(150), label = "fieldEdge")
    val edgeWidth by animateDpAsState(if (focused) 1.6.dp else 1.dp, tween(150), label = "fieldEdgeW")
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel(label, if (focused) cs.primary else cs.onSurfaceVariant, Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            interactionSource = src,
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = if (suffix != null) "$label ($suffix)" else label },
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape).background(cs.background.copy(alpha = .7f))
                        .border(edgeWidth, edge, shape).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) { inner() }
                    if (suffix != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(suffix, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        )
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    }
}

/** عنوان الحقل الصغير فوقه (كما في حقول iPhone) */
@Composable
fun FormLabel(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), fontWeight = FontWeight.Medium, color = color)
}

/** اختيار من قائمة بطراز iPhone: العنوان فوق حقل فاتح بسهمين صغيرين، يفتح القائمة الزجاجية والمختار فيها بعلامة صح */
@Composable
fun <T> FormPicker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel(label)
        BoxWithConstraints {
            FormMenuField(text(selected)) { open = true }
            GlassMenu(
                open, { open = false },
                options.map { o -> MenuItem(text(o), checked = o == selected) { onSelect(o) } },
                width = maxWidth.coerceIn(220.dp, 360.dp),
            )
        }
    }
}

/** حقل يشبه قائمة النظام في iPhone: خلفية فاتحة وحافة رفيعة وسهمان للأعلى والأسفل */
@Composable
fun FormMenuField(value: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cs.outlineVariant),
        color = cs.background.copy(alpha = .7f),
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.UnfoldMore, null, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
        }
    }
}

/** عدّاد بطراز iPhone: العنوان، ثم العدد بلون التطبيق، ثم كبسولة فيها − و + */
@Composable
fun FormStepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        RollingText("$value", MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        FormStepperControl(value, onChange, min, max)
    }
}

/** كبسولة العدّاد (كعدّاد النظام في iPhone): زرّا − و + بينهما خط رفيع، واهتزاز خفيف مع كل خطوة وأقوى عند الحد */
@Composable
fun FormStepperControl(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    fun step(v: Int) {
        haptic.performHapticFeedback(if (v == min || v == max) HapticFeedbackType.ContextClick else HapticFeedbackType.SegmentTick)
        onChange(v)
    }
    Row(Modifier.height(36.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = .07f)), verticalAlignment = Alignment.CenterVertically) {
        FormStepHalf(Icons.Filled.Remove, s.decrease, value > min) { step(value - 1) }
        Box(Modifier.width(1.dp).height(18.dp).background(cs.onSurface.copy(alpha = .16f)))
        FormStepHalf(Icons.Filled.Add, s.increase, value < max) { step(value + 1) }
    }
}

@Composable
private fun FormStepHalf(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .28f), tween(150), label = "stepTint"
    )
    Box(
        Modifier.width(48.dp).fillMaxHeight().clickable(enabled = enabled, role = Role.Button, onClickLabel = desc, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, Modifier.size(20.dp), tint = tint) }
}

/** صف بنص ومفتاح تشغيل (كـ Toggle في iPhone)؛ الصف كله يُلمس */
@Composable
fun FormSwitchRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).toggleable(checked, role = Role.Switch) {
            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onChange(it)
        }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** زر المشاركة بطراز iPhone: كبسولة بتدرج لون التطبيق وأيقونة المشاركة */
@Composable
fun FormShareButton(text: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = Color.Transparent, interactionSource = src,
        modifier = Modifier.fillMaxWidth().pressScale(src, 0.97f)
    ) {
        Row(
            Modifier.background(HeroBrush).padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.IosShare, null, Modifier.size(18.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun RowScope.Tile(value: String, label: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(value, MaterialTheme.typography.titleLarge, MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

/** نص تصبح فيه tanzil.net و tanzil.net/trans و SIL روابط تُفتح في المتصفح */
@Composable
fun LinkedText(text: String) {
    val color = MaterialTheme.colorScheme.primary
    val styles = androidx.compose.ui.text.TextLinkStyles(
        androidx.compose.ui.text.SpanStyle(color = color, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
    )
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        Regex("tanzil\\.net/trans|tanzil\\.net|SIL").findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            val url = when (m.value) { "SIL" -> "https://openfontlicense.org"; "tanzil.net/trans" -> "https://tanzil.net/trans/"; else -> "https://tanzil.net/" }
            withLink(androidx.compose.ui.text.LinkAnnotation.Url(url, styles)) { append(m.value) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
    Text(annotated, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** فراغ بارتفاع ثابت (للمسافات بين عناصر الشبكات) */
@Composable
fun Gap(d: Dp) = Spacer(Modifier.size(d))
