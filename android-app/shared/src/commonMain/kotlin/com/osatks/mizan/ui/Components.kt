package com.osatks.mizan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush

/** مساحة أسفل المحتوى: ارتفاع شريط التنقل العائم في صفحات التبويبات */
val LocalBottomInset = compositionLocalOf { 0.dp }

/** رأس الصفحة المتدرج مع زر الرجوع؛ sharedKey يربطه ببطاقة الأداة في الرئيسية */
@Composable
fun PageHeader(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, sharedKey: String? = null, icon: ImageVector? = null,
    cardKey: String? = sharedKey?.let { "card-$it" }, actions: @Composable RowScope.() -> Unit = {},
) {
    val s = LocalStrings.current
    Box(
        Modifier
            .then(if (cardKey != null) Modifier.sharedCard(cardKey, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(HeroBrush)
            .starBackdrop(Color.White.copy(alpha = .07f), 72.dp, Alignment.TopEnd, (-28).dp, 44.dp, 90_000)
    ) {
        Row(
            Modifier.statusBarsPadding().padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                }
            } else Spacer(Modifier.width(12.dp))
            if (icon != null) {
                Box(
                    Modifier
                        .then(if (sharedKey != null) Modifier.sharedIcon("icon-$sharedKey") else Modifier)
                        .size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = Gold) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                // العنوان يتبدل بانزلاق لطيف (مثل اسم السورة عند تقليب صفحات المصحف)
                androidx.compose.animation.AnimatedContent(
                    title,
                    transitionSpec = {
                        (fadeIn(tween(Motion.MEDIUM)) + androidx.compose.animation.slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 }) togetherWith
                            fadeOut(tween(Motion.SHORT)) using androidx.compose.animation.SizeTransform(clip = false)
                    },
                    label = "title"
                ) { t -> Text(t, style = MaterialTheme.typography.headlineSmall, color = Color.White) }
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
            }
            actions()
        }
    }
}

/** محتوى الصفحة: كل item يظهر متتابعاً عند فتح الصفحة */
class PageScope internal constructor(private val list: LazyListScope, private val reveal: RevealState) {
    private var index = 0
    fun item(key: Any? = null, content: @Composable () -> Unit) {
        val i = index++
        list.item(key = key) {
            Box(Modifier.reveal(reveal, i)) { content() }
        }
    }
}

/** صفحة أداة: رأس + محتوى قابل للتمرير */
@Composable
fun ToolPage(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null,
    sharedKey: String? = null, icon: ImageVector? = null, spacing: Dp = 14.dp,
    content: PageScope.() -> Unit
) {
    val reveal = remember { RevealState() }
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PageHeader(title, subtitle, onBack, sharedKey, icon)
        androidx.compose.runtime.key(com.osatks.mizan.ui.theme.LocalDark.current) { LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + nav + LocalBottomInset.current),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) { PageScope(this, reveal).content() } }
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

/** يظهر المحتوى وينطوي بحركة بدل الظهور المفاجئ */
@Composable
fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(Motion.MEDIUM)) + expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)),
        exit = fadeOut(tween(Motion.SHORT)) + shrinkVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)),
    ) { content() }
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String? = null, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            suffix = suffix?.let { s -> @Composable { Text(s) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        MiniStepper(value, onChange, min, max)
    }
}

@Composable
fun MiniStepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    val s = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Filled.Remove, s.decrease, enabled = value > min) { onChange(value - 1) }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            RollingText("$value", MaterialTheme.typography.titleMedium)
        }
        StepButton(Icons.Filled.Add, s.increase, enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, label = "step")
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = bg, interactionSource = src,
        modifier = Modifier.size(38.dp).pressScale(src, 0.86f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

/** أزرار اختيار متجاورة، والمؤشر ينزلق بينها */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val pos by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.78f, stiffness = 420f), label = "seg")
    val pill = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.outlineVariant
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.padding(4.dp).height(IntrinsicSize.Min).drawBehind {
                val w = size.width / options.size
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w * (pos + 1) else w * pos
                val r = CornerRadius(12.dp.toPx())
                drawRoundRect(edge, Offset(x, 0f), Size(w, size.height), r)
                drawRoundRect(pill, Offset(x + 1f, 1f), Size(w - 2f, size.height - 2f), r)
            }
        ) {
            options.forEachIndexed { i, (key, label) ->
                val color by animateColorAsState(
                    if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "segText"
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable { onSelect(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, Modifier.padding(vertical = 10.dp, horizontal = 6.dp), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** قائمة منسدلة بسيطة */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        Box {
            PickerField(text(selected)) { open = true }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

/** حقل يشبه القائمة المنسدلة ويفتح شيئاً عند الضغط */
@Composable
fun PickerField(value: String, leading: ImageVector? = null, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { Icon(leading, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)) }
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, strong: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

/** بطاقة النتيجة الكبيرة: القيمة تتدحرج عند التغير، والوحدة (إن وُجدت) بجانبها */
@Composable
fun ResultCard(
    label: String, value: String, badge: String? = null, badgeOk: Boolean? = null,
    unit: String? = null, content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .06f), 72.dp, Alignment.BottomStart, 30.dp, (-24).dp, 120_000)) {
            Column(
                Modifier.padding(22.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(label, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RollingText(value, MaterialTheme.typography.headlineLarge, Color.White, textAlign = TextAlign.Center)
                    if (unit != null) Text(unit, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.titleLarge)
                }
                if (badge != null) {
                    val target = when (badgeOk) { true -> Color(0xFF3FB595); false -> Color(0xFFEF8A76); null -> Gold }
                    val bg by animateColorAsState(target, tween(Motion.MEDIUM), label = "badge")
                    Surface(shape = CircleShape, color = bg.copy(alpha = .25f)) {
                        RollingText(
                            badge, MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), Color.White,
                            Modifier.padding(horizontal = 14.dp, vertical = 5.dp), TextAlign.Center
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun Note(text: String, warn: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (warn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (warn) Icons.Filled.WarningAmber else Icons.Filled.Info, null,
                tint = if (warn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (warn) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "chip")
    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "chipText")
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "chipBorder")
    Surface(
        onClick = onClick, shape = CircleShape, color = bg, border = BorderStroke(1.dp, border),
        interactionSource = src, modifier = Modifier.pressScale(src, 0.92f)
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = fg)
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

fun shareText(s: Strings, text: String) = com.osatks.mizan.platform.Platform.share("$text\n${s.shareSignature}", s.share)

/** صف قابل للنقر بنص وقيمة (للإعدادات) */
@Composable
fun ClickRow(title: String, subtitle: String? = null, onClick: () -> Unit, leading: ImageVector? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(leading, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

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
