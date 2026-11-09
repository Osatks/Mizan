package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import com.osatks.mizan.ui.GlassMenu
import com.osatks.mizan.ui.MenuItem
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.QuranFont
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.themed
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TasbihScreen(onBack: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // الحال في Tasbih: تعدّ فيها الشاشة، وفي iOS زر الإجراء ومركز التحكم والجزيرة الديناميكية والأداة أيضاً
    remember { Tasbih.ensure() }
    val st = Tasbih.state
    val count = st.count
    val goal = st.goal
    val text = st.text
    val meaning = st.meaning(s)
    var message by remember { mutableStateOf<String?>(null) }

    val press = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    val ripples = remember { mutableStateListOf<Animatable<Float, *>>() }

    fun inc() {
        val event = Tasbih.count()
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        message = null
        // نبضة الحبة وموجة من مركزها
        scope.launch {
            press.snapTo(0.93f)
            press.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        val r = Animatable(0f)
        ripples += r
        if (ripples.size > 6) ripples.removeAt(0)
        scope.launch { r.animateTo(1f, tween(700, easing = Motion.Decelerate)); ripples.remove(r) }

        if (event != Tasbih.Event.COUNT) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { burst.snapTo(0f); burst.animateTo(1f, tween(900, easing = Motion.Decelerate)) }
            message = when (event) {
                Tasbih.Event.NEXT_DHIKR -> s.nextDhikr
                Tasbih.Event.HUNDRED -> s.completedHundred
                else -> s.completedN(goal)
            }
        }
    }

    val progress by animateFloatAsState(
        if (goal > 0) (count.toFloat() / goal).coerceIn(0f, 1f) else (count % 100) / 100f,
        spring(dampingRatio = 0.8f, stiffness = 200f), label = "p"
    )

    ToolPage(s.tasbihTitle, s.tasbihSubtitle, onBack, sharedKey = "tasbih", icon = TOOL_ICONS["tasbih"], plainIcon = true) {
        item {
            TasbihCard {
                Segmented(listOf("free" to s.modeFree, "salah" to s.modeAfterPrayer), st.mode) { Tasbih.setMode(it) }
                AnimatedContent(
                    text to meaning,
                    transitionSpec = {
                        (fadeIn(tween(Motion.MEDIUM, 80)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 4 }) togetherWith fadeOut(tween(120))
                    },
                    label = "dhikr"
                ) { (t, m) ->
                    Column(Modifier.fillMaxWidth().heightIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            t, Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center,
                            // بخط المصحف كما في iPhone
                            style = TextStyle(
                                fontFamily = QuranFont, fontSize = if (t.length > 40) 21.sp else 30.sp,
                                lineHeight = if (t.length > 40) 36.sp else 48.sp, textDirection = TextDirection.Rtl,
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (m.isNotEmpty()) Text(m, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val track = MaterialTheme.colorScheme.outlineVariant
                    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                        // الموجات والانفجار الذهبي خلف الحبة
                        Canvas(Modifier.fillMaxSize()) {
                            val c = Offset(size.width / 2, size.height / 2)
                            val base = size.minDimension * 0.41f
                            ripples.forEach { a ->
                                val p = a.value
                                drawCircle(Gold.copy(alpha = 0.45f * (1f - p)), base + p * size.minDimension * 0.12f, c, style = Stroke(6f * (1f - p) + 1f))
                            }
                            val b = burst.value
                            if (b in 0.001f..0.999f) {
                                for (k in 0 until 16) {
                                    val ang = k * (2 * PI / 16)
                                    val d = base + b * size.minDimension * 0.1f
                                    drawCircle(Gold.copy(alpha = 1f - b), 7f * (1f - b) + 2f, Offset(c.x + d * cos(ang).toFloat(), c.y + d * sin(ang).toFloat()))
                                }
                            }
                        }
                        Canvas(Modifier.size(250.dp)) {
                            val w = 5.dp.toPx()
                            drawArc(track, 0f, 360f, false, style = Stroke(w))
                            drawArc(Gold, -90f, 360f * progress, false, style = Stroke(w, cap = StrokeCap.Round))
                        }
                        Box(
                            Modifier.size(210.dp)
                                .graphicsLayer { scaleX = press.value; scaleY = press.value }
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF1A8A6F).themed, Color(0xFF0B4A3D).themed)))
                                .semantics { contentDescription = s.tapToCount }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { inc() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RollingText("$count", MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold), Color.White)
                                Text(if (goal > 0) s.ofGoal(goal) else s.noLimit, color = Color.White.copy(alpha = .8f), fontSize = 15.sp, maxLines = 1)
                            }
                        }
                    }
                }
                // سطر التهنئة يتمدد عند ظهوره فقط، فلا يترك فراغاً تحت الدائرة
                val lastMessage = remember { arrayOf("") }
                message?.let { lastMessage[0] = it }
                com.osatks.mizan.ui.Reveal(message != null) {
                    AnimatedContent(
                        lastMessage[0],
                        transitionSpec = { (fadeIn() + scaleIn(spring(dampingRatio = 0.55f), initialScale = 0.8f)) togetherWith fadeOut(tween(120)) },
                        label = "msg"
                    ) { m ->
                        Text(m, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    TasbihOutline(s.reset, Icons.Filled.RestartAlt) { Tasbih.reset() }
                    TasbihOutline(s.undo, Icons.AutoMirrored.Filled.Undo) { Tasbih.undo() }
                }
                // iOS: العدّاد على شاشة القفل وفي الجزيرة الديناميكية (يعدّ منها زر الإجراء ومركز التحكم أيضاً)
                if (com.osatks.mizan.platform.LiveTasbih.supported) {
                    val live = com.osatks.mizan.platform.LiveTasbih
                    val code = com.osatks.mizan.i18n.LocalLang.current.code
                    val lt = com.osatks.mizan.widget.LiveText.of(code)
                    if (live.onLockScreen) {
                        androidx.compose.material3.TextButton(
                            onClick = { live.show?.invoke() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Smartphone, null, Modifier.size(20.dp))
                            Text(" " + lt.showLive, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                    // من لا يريد البطاقة على شاشة القفل يطفئها هنا (بدل «عدم السماح» في سؤال iOS، فتتعطل الجزيرة)
                    val (title, desc) = com.osatks.mizan.platform.LiveTasbihText.of(code)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), fontWeight = FontWeight.SemiBold)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        com.osatks.mizan.ui.AppSwitch(checked = live.onLockScreen, onCheckedChange = { live.update(it) })
                    }
                }
            }
        }
        side {
            com.osatks.mizan.ui.Reveal(st.mode == "free") {
                TasbihCard(s.chooseDhikr) {
                    TasbihChipGrid(s.dhikrLabels, st.idx) { Tasbih.setDhikr(it) }
                    TasbihTargetRow(s.targetCount, Tasbih.TARGETS, st.target, text = { if (it == 0) s.noLimit else "$it" }) { Tasbih.setTarget(it) }
                }
            }
        }
        side {
            TasbihCard(s.yourStats) {
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TasbihStat(Fmt.int(st.rounds.toLong()), s.roundsDone)
                    TasbihStat(Fmt.int(st.today.toLong()), s.todayCount)
                    TasbihStat(Fmt.int(st.total.toLong()), s.totalCount)
                }
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.tasbih) }
    }
}

/** بطاقة بيضاء بطراز iPhone: زوايا 22 وحدّ رفيع، وعنوان عريض بلون التطبيق */
@Composable
private fun TasbihCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(
                title, Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.ExtraBold, color = cs.primary,
            )
            content()
        }
    }
}

/** زر بحدّ رفيع (التصفير والتراجع) */
@Composable
private fun TasbihOutline(title: String, icon: ImageVector, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = CircleShape, color = Color.Transparent, contentColor = cs.primary,
        border = BorderStroke(1.dp, cs.outlineVariant), interactionSource = src, modifier = Modifier.pressScale(src, 0.95f),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(20.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/** الأذكار شبكةً من خانات متساوية (عرض الخانة 120 على الأقل) */
@Composable
private fun TasbihChipGrid(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().selectableGroup()) {
        val cols = ((maxWidth + 8.dp) / (120.dp + 8.dp)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.indices.chunked(cols).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i -> TasbihChip(labels[i], i == selected, Modifier.weight(1f).fillMaxHeight()) { onSelect(i) } }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TasbihChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) cs.primary else cs.surface, tween(Motion.SHORT), label = "chipBg")
    val fg by animateColorAsState(if (selected) cs.onPrimary else cs.onSurfaceVariant, tween(Motion.SHORT), label = "chipFg")
    Box(
        modifier.heightIn(min = 42.dp).pressScale(src, 0.95f).clip(RoundedCornerShape(14.dp)).background(bg)
            .border(1.dp, if (selected) cs.primary else cs.outlineVariant, RoundedCornerShape(14.dp))
            .selectable(selected, interactionSource = src, indication = androidx.compose.material3.ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** الهدف: اسمه، وكبسولة بالقيمة تفتح قائمة الأهداف */
@Composable
private fun <T> TasbihTargetRow(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 15.sp, color = cs.onSurface)
        Spacer(Modifier.width(8.dp))
        Box {
            val src = remember { MutableInteractionSource() }
            Surface(
                onClick = { open = true }, shape = CircleShape, color = cs.primaryContainer, interactionSource = src,
                modifier = Modifier.pressScale(src, 0.95f).semantics { role = Role.DropdownList; contentDescription = label },
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text(selected), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cs.primary)
                    Icon(Icons.Filled.UnfoldMore, null, Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                }
            }
            GlassMenu(open, { open = false }, options.map { o -> MenuItem(text(o), checked = o == selected) { onSelect(o) } }, title = label, width = 200.dp)
        }
    }
}

/** خانة إحصاء: الرقم عريضاً بلون التطبيق، وتحته اسمه */
@Composable
private fun RowScope.TasbihStat(value: String, label: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(cs.primaryContainer.copy(alpha = .6f))
            .semantics(mergeDescendants = true) {}.padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RollingText(value, MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Black), cs.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
    }
}
