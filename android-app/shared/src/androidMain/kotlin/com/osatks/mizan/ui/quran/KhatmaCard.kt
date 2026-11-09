package com.osatks.mizan.ui.quran

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep

/**
 * متابعة الختمة في بطاقة مدمجة: حلقة الإنجاز، والصفحات المقروءة وأيام القراءة المتتالية،
 * وورد اليوم، وزر يفتح أول صفحة لم تُقرأ. مدة الختمة وبدء ختمة جديدة في قائمة منسدلة.
 */
@Composable
fun KhatmaCard(open: (String) -> Unit) {
    QuranProgress.ensure()
    val s = LocalStrings.current
    val p = QuranProgress
    val total = QuranProgress.PAGES
    val frac by animateFloatAsState(p.readCount / total.toFloat(), spring(dampingRatio = 0.9f, stiffness = 80f), label = "khatma")
    val goal = p.dailyGoal()
    val today by animateFloatAsState(if (goal > 0) (p.todayCount / goal.toFloat()).coerceIn(0f, 1f) else 0f, spring(dampingRatio = 0.9f, stiffness = 120f), label = "today")
    val track = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Gold.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // حلقة الإنجاز حول نجمة ثمانية تمتلئ بالذهب مع التقدم
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(64.dp)) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2
                        val arc = Size(size.width - stroke, size.height - stroke)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                        drawArc(
                            Brush.sweepGradient(listOf(Gold, GoldDeep, Gold), center), -90f, 360f * frac, false,
                            Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        drawStar8(center, size.minDimension * 0.3f, Gold.copy(alpha = .25f + .5f * frac), 1.dp.toPx(), rings = false)
                    }
                    Text("${Fmt.num(frac * 100.0, 0)}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = primary)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.khatmaTitle, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        PlanMenu()
                    }
                    Text(s.pagesRead(p.readCount, total), style = MaterialTheme.typography.bodyMedium, color = muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = if (p.streak > 0) Gold else track, modifier = Modifier.size(17.dp))
                        Text(" " + s.streakDays(p.streak), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (p.planDays > 0 && !p.finished) {
                            Text("  ·  " + s.daysLeftLabel(p.daysLeft()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                        }
                    }
                }
            }

            if (p.finished) {
                Text(s.khatmaDone, color = primary, fontWeight = FontWeight.Bold)
                if (p.completed > 0) Text(s.khatmasCompleted(p.completed), style = MaterialTheme.typography.bodySmall, color = muted)
                Button(onClick = { p.restart() }, modifier = Modifier.fillMaxWidth()) { Text(s.newKhatma) }
            } else {
                if (goal > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.todayPortion, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (p.todayCount >= goal) s.portionDone else s.pagesRead(p.todayCount, goal),
                                style = MaterialTheme.typography.labelLarge, color = if (p.todayCount >= goal) primary else muted
                            )
                        }
                        // شريط ورد اليوم: يمتلئ بتدرج ذهبي
                        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                            val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                            drawRoundRect(track, cornerRadius = r)
                            if (today > 0f) {
                                val w = size.width * today
                                val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) size.width - w else 0f
                                drawRoundRect(Brush.horizontalGradient(listOf(GoldDeep, Gold)), Offset(x, 0f), Size(w, size.height), r)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val (sn, an) = QuranMeta.locate(QuranMeta.PAGES[p.nextUnread() - 1])
                        open(surahRoute(sn, an, "khatma"))
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.continuePortion, maxLines = 1, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** مدة الختمة في شارة صغيرة؛ تفتح قائمة بالمدد، وبدء ختمة جديدة */
@Composable
private fun PlanMenu() {
    val s = LocalStrings.current
    val p = QuranProgress
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true }, shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.planDays(p.planDays), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, s.khatmaPlanLabel, Modifier.size(18.dp))
            }
        }
        // المدد في مجموعة، و«ختمة جديدة» في مجموعة بعدها
        com.osatks.mizan.ui.GlassMenu(
            expanded, { expanded = false },
            QuranProgress.PLANS.map { d -> com.osatks.mizan.ui.MenuItem(s.planDays(d), checked = d == p.planDays) { p.changePlan(d) } } +
                if (p.readCount > 0) listOf(com.osatks.mizan.ui.MenuGroupGap, com.osatks.mizan.ui.MenuItem(s.newKhatma, Icons.Filled.Refresh) { p.restart() }) else emptyList(),
            title = s.khatmaPlanLabel, width = 240.dp,
        )
    }
}
