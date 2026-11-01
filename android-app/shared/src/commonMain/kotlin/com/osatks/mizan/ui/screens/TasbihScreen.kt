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
import com.osatks.mizan.core.AppSettings
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

/** نصوص الأذكار بالعربية (تُعرض كما هي في كل اللغات، ومعناها تحتها) */
val DHIKR = listOf(
    "سُبْحَانَ اللَّهِ",
    "الْحَمْدُ لِلَّهِ",
    "اللَّهُ أَكْبَرُ",
    "لَا إِلَهَ إِلَّا اللَّهُ",
    "أَسْتَغْفِرُ اللَّهَ",
    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
    "سُبْحَانَ اللَّهِ الْعَظِيمِ",
    "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
    "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
)
// تسبيح ما بعد الصلاة: 33 + 33 + 33 ثم تمام المائة
private val SALAH = listOf(0 to 33, 1 to 33, 2 to 33, -1 to 1)
private const val TAMAM = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen() {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val todayKey = LocalDate.now().toString()
    if (AppSettings.getString("ts_day") != todayKey) { AppSettings.putString("ts_day", todayKey); AppSettings.putInt("ts_today", 0) }

    var mode by remember { mutableStateOf(AppSettings.getString("ts_mode", "free")) }
    var idx by remember { mutableIntStateOf(AppSettings.getInt("ts_idx", 0).coerceIn(DHIKR.indices)) }
    var step by remember { mutableIntStateOf(AppSettings.getInt("ts_step", 0).coerceIn(SALAH.indices)) }
    var count by remember { mutableIntStateOf(AppSettings.getInt("ts_count", 0)) }
    var target by remember { mutableIntStateOf(AppSettings.getInt("ts_target", 33)) }
    var rounds by remember { mutableIntStateOf(AppSettings.getInt("ts_rounds", 0)) }
    var total by remember { mutableIntStateOf(AppSettings.getInt("ts_total", 0)) }
    var today by remember { mutableIntStateOf(AppSettings.getInt("ts_today", 0)) }
    var message by remember { mutableStateOf<String?>(null) }

    val press = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    val ripples = remember { mutableStateListOf<Animatable<Float, *>>() }

    fun save() {
        AppSettings.putString("ts_mode", mode); AppSettings.putInt("ts_idx", idx); AppSettings.putInt("ts_step", step)
        AppSettings.putInt("ts_count", count); AppSettings.putInt("ts_target", target); AppSettings.putInt("ts_rounds", rounds)
        AppSettings.putInt("ts_total", total); AppSettings.putInt("ts_today", today)
    }

    val text: String
    val meaning: String
    val goal: Int
    if (mode == "salah") {
        val (i, n) = SALAH[step]
        text = if (i < 0) TAMAM else DHIKR[i]
        meaning = if (i < 0) s.tamamMeaning else s.dhikrMeanings[i]
        goal = n
    } else {
        text = DHIKR[idx]
        meaning = s.dhikrMeanings[idx]
        goal = target
    }

    fun inc() {
        count++; total++; today++
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

        if (goal > 0 && count >= goal) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { burst.snapTo(0f); burst.animateTo(1f, tween(900, easing = Motion.Decelerate)) }
            if (mode == "salah") {
                if (step < SALAH.size - 1) { step++; count = 0; message = s.nextDhikr }
                else { step = 0; count = 0; rounds++; message = s.completedHundred }
            } else { rounds++; count = 0; message = s.completedN(goal) }
        }
        save()
    }

    val progress by animateFloatAsState(
        if (goal > 0) (count.toFloat() / goal).coerceIn(0f, 1f) else (count % 100) / 100f,
        spring(dampingRatio = 0.8f, stiffness = 200f), label = "p"
    )

    ToolPage(s.tasbihTitle, s.tasbihSubtitle, sharedKey = "tasbih", icon = TOOL_ICONS["tasbih"]) {
        item {
            SectionCard {
                Segmented(listOf("free" to s.modeFree, "salah" to s.modeAfterPrayer), mode) { mode = it; count = 0; step = 0; save() }
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
                    OutlinedButton(onClick = { count = 0; step = 0; save() }) { Icon(Icons.Filled.RestartAlt, null); Text(" " + s.reset) }
                    OutlinedButton(onClick = {
                        if (count > 0) { count--; total = (total - 1).coerceAtLeast(0); today = (today - 1).coerceAtLeast(0); save() }
                    }) { Icon(Icons.AutoMirrored.Filled.Undo, null); Text(" " + s.undo) }
                }
            }
        }
        side {
            com.osatks.mizan.ui.Reveal(mode == "free") {
                SectionCard(s.chooseDhikr) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.dhikrLabels.forEachIndexed { i, label -> ChipButton(label, i == idx) { idx = i; count = 0; save() } }
                    }
                    Picker(s.targetCount, listOf(33, 34, 100, 1000, 0), target, text = { if (it == 0) s.noLimit else "$it" }, onSelect = { target = it; count = 0; save() })
                }
            }
        }
        side {
            SectionCard(s.yourStats) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(Fmt.int(rounds.toLong()), s.roundsDone)
                    Tile(Fmt.int(today.toLong()), s.todayCount)
                    Tile(Fmt.int(total.toLong()), s.totalCount)
                }
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.tasbih) }
    }
}
