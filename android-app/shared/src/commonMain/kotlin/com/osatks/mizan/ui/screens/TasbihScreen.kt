package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.Tile
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.theme.DhikrStyle
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.themed
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** نصوص الأذكار بالعربية (البحث يعرضها أيضاً) */
val DHIKR get() = Tasbih.DHIKR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen() {
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

    ToolPage(s.tasbihTitle, s.tasbihSubtitle, sharedKey = "tasbih", icon = TOOL_ICONS["tasbih"]) {
        item {
            SectionCard {
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
                            style = if (t.length > 40) DhikrStyle.copy(fontSize = 20.sp, lineHeight = 34.sp) else DhikrStyle,
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
                            drawArc(track, 0f, 360f, false, style = Stroke(14f))
                            drawArc(Gold, -90f, 360f * progress, false, style = Stroke(14f, cap = StrokeCap.Round))
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
                                Text(if (goal > 0) s.ofGoal(goal) else s.noLimit, color = Color.White.copy(alpha = .8f))
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
                        Text(m, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    OutlinedButton(onClick = { Tasbih.reset() }) { Icon(Icons.Filled.RestartAlt, null); Text(" " + s.reset) }
                    OutlinedButton(onClick = { Tasbih.undo() }) { Icon(Icons.AutoMirrored.Filled.Undo, null); Text(" " + s.undo) }
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
                            Icon(Icons.Filled.Smartphone, null)
                            Text(" " + lt.showLive)
                        }
                    }
                    // من لا يريد البطاقة على شاشة القفل يطفئها هنا (بدل «عدم السماح» في سؤال iOS، فتتعطل الجزيرة)
                    val (title, desc) = com.osatks.mizan.platform.LiveTasbihText.of(code)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        com.osatks.mizan.ui.AppSwitch(checked = live.onLockScreen, onCheckedChange = { live.update(it) })
                    }
                }
            }
        }
        side {
            com.osatks.mizan.ui.Reveal(st.mode == "free") {
                SectionCard(s.chooseDhikr) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.dhikrLabels.forEachIndexed { i, label -> ChipButton(label, i == st.idx) { Tasbih.setDhikr(i) } }
                    }
                    Picker(s.targetCount, Tasbih.TARGETS, st.target, text = { if (it == 0) s.noLimit else "$it" }, onSelect = { Tasbih.setTarget(it) })
                }
            }
        }
        side {
            SectionCard(s.yourStats) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(Fmt.int(st.rounds.toLong()), s.roundsDone)
                    Tile(Fmt.int(st.today.toLong()), s.todayCount)
                    Tile(Fmt.int(st.total.toLong()), s.totalCount)
                }
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.tasbih) }
    }
}
