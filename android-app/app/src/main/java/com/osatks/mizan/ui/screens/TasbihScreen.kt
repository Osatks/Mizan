package com.osatks.mizan.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.Tile
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.theme.DhikrStyle
import com.osatks.mizan.ui.theme.Gold
import java.time.LocalDate

private val DHIKR = listOf(
    "سُبْحَانَ اللَّهِ" to "سبحان الله",
    "الْحَمْدُ لِلَّهِ" to "الحمد لله",
    "اللَّهُ أَكْبَرُ" to "الله أكبر",
    "لَا إِلَهَ إِلَّا اللَّهُ" to "لا إله إلا الله",
    "أَسْتَغْفِرُ اللَّهَ" to "أستغفر الله",
    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ" to "سبحان الله وبحمده",
    "سُبْحَانَ اللَّهِ الْعَظِيمِ" to "سبحان الله العظيم",
    "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ" to "لا حول ولا قوة إلا بالله",
    "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ" to "الصلاة على النبي ﷺ",
)
// تسبيح ما بعد الصلاة: 33 + 33 + 33 ثم تمام المائة
private val SALAH = listOf(0 to 33, 1 to 33, 2 to 33, -1 to 1)
private const val TAMAM = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasbihScreen() {
    val view = LocalView.current
    val todayKey = LocalDate.now().toString()
    if (AppSettings.getString("ts_day") != todayKey) { AppSettings.putString("ts_day", todayKey); AppSettings.putInt("ts_today", 0) }

    var mode by remember { mutableStateOf(AppSettings.getString("ts_mode", "free")) }
    var idx by remember { mutableIntStateOf(AppSettings.getInt("ts_idx", 0)) }
    var step by remember { mutableIntStateOf(AppSettings.getInt("ts_step", 0)) }
    var count by remember { mutableIntStateOf(AppSettings.getInt("ts_count", 0)) }
    var target by remember { mutableIntStateOf(AppSettings.getInt("ts_target", 33)) }
    var rounds by remember { mutableIntStateOf(AppSettings.getInt("ts_rounds", 0)) }
    var total by remember { mutableIntStateOf(AppSettings.getInt("ts_total", 0)) }
    var today by remember { mutableIntStateOf(AppSettings.getInt("ts_today", 0)) }
    var message by remember { mutableStateOf<String?>(null) }

    fun save() {
        AppSettings.putString("ts_mode", mode); AppSettings.putInt("ts_idx", idx); AppSettings.putInt("ts_step", step)
        AppSettings.putInt("ts_count", count); AppSettings.putInt("ts_target", target); AppSettings.putInt("ts_rounds", rounds)
        AppSettings.putInt("ts_total", total); AppSettings.putInt("ts_today", today)
    }

    val text: String
    val goal: Int
    if (mode == "salah") {
        val (i, n) = SALAH[step]
        text = if (i < 0) TAMAM else DHIKR[i].first
        goal = n
    } else {
        text = DHIKR[idx].first
        goal = target
    }

    fun inc() {
        count++; total++; today++
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        message = null
        if (goal > 0 && count >= goal) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (mode == "salah") {
                if (step < SALAH.size - 1) { step++; count = 0; message = "انتقل إلى الذكر التالي" }
                else { step = 0; count = 0; rounds++; message = "تقبّل الله منك ✓ أتممت المائة" }
            } else { rounds++; count = 0; message = "أتممت $goal ✓" }
        }
        save()
    }

    val progress by animateFloatAsState(if (goal > 0) (count.toFloat() / goal).coerceIn(0f, 1f) else (count % 100) / 100f, label = "p")
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .95f else 1f, label = "s")

    ToolPage("المسبحة الإلكترونية", "اضغط على الدائرة للتسبيح") {
        item {
            SectionCard {
                Segmented(listOf("free" to "ذكر حر", "salah" to "بعد الصلاة"), mode) { mode = it; count = 0; step = 0; save() }
                Text(
                    text, Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center,
                    style = if (text.length > 40) DhikrStyle.copy(fontSize = 20.sp, lineHeight = 34.sp) else DhikrStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val track = MaterialTheme.colorScheme.outlineVariant
                    Box(Modifier.size(260.dp).scale(scale), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawArc(track, 0f, 360f, false, style = Stroke(14f))
                            drawArc(Gold, -90f, 360f * progress, false, style = Stroke(14f, cap = StrokeCap.Round))
                        }
                        Box(
                            Modifier.size(220.dp).clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF1A8A6F), Color(0xFF0B4A3D))))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    pressed = true; inc(); pressed = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$count", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
                                Text(if (goal > 0) "من $goal" else "بلا حد", color = Color.White.copy(alpha = .8f))
                            }
                        }
                    }
                }
                if (message != null) Text(message!!, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
                    OutlinedButton(onClick = { count = 0; step = 0; save() }) { Icon(Icons.Filled.RestartAlt, null); Text(" تصفير") }
                    OutlinedButton(onClick = {
                        if (count > 0) { count--; total = (total - 1).coerceAtLeast(0); today = (today - 1).coerceAtLeast(0); save() }
                    }) { Icon(Icons.AutoMirrored.Filled.Undo, null); Text(" تراجع") }
                }
            }
        }
        if (mode == "free") item {
            SectionCard("اختر الذكر") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DHIKR.forEachIndexed { i, (_, short) -> ChipButton(short, i == idx) { idx = i; count = 0; save() } }
                }
                Picker("العدد المستهدف", listOf(33, 34, 100, 1000, 0), target, text = { if (it == 0) "بلا حد" else "$it" }, onSelect = { target = it; count = 0; save() })
            }
        }
        item {
            SectionCard("إحصاءاتك") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile(Fmt.int(rounds.toLong()), "دورة مكتملة")
                    Tile(Fmt.int(today.toLong()), "ذكر اليوم")
                    Tile(Fmt.int(total.toLong()), "الإجمالي")
                }
            }
        }
    }
}
