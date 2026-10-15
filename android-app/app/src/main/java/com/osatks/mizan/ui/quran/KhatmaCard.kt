package com.osatks.mizan.ui.quran

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Reveal
import com.osatks.mizan.ui.theme.Gold

/**
 * متابعة الختمة: نسبة ما قُرئ من المصحف، وورد اليوم حسب مدة الختمة، وأيام القراءة المتتالية،
 * واختيار المدة، وزر يفتح أول صفحة لم تُقرأ بعد.
 */
@Composable
fun KhatmaCard(open: (String) -> Unit) {
    QuranProgress.ensure()
    val s = LocalStrings.current
    val p = QuranProgress
    val total = QuranProgress.PAGES
    val frac by animateFloatAsState(p.readCount / total.toFloat(), spring(dampingRatio = 0.9f, stiffness = 80f), label = "khatma")
    val goal = p.dailyGoal()
    val today by animateFloatAsState(if (goal > 0) (p.todayCount / goal.toFloat()).coerceIn(0f, 1f) else 0f, label = "today")
    val track = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Gold.copy(alpha = .5f)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(78.dp)) {
                        drawArc(track, 0f, 360f, false, style = Stroke(9f))
                        drawArc(Gold, -90f, 360f * frac, false, style = Stroke(9f, cap = StrokeCap.Round))
                    }
                    Text("${Fmt.num(frac * 100.0, 0)}%", style = MaterialTheme.typography.titleMedium, color = primary)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(s.khatmaTitle, style = MaterialTheme.typography.titleMedium)
                    Text(s.pagesRead(p.readCount, total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = if (p.streak > 0) Gold else track, modifier = Modifier.size(18.dp))
                        Text(" " + s.streakDays(p.streak), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    if (p.planDays > 0 && !p.finished) Text(s.daysLeftLabel(p.daysLeft()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (p.finished) {
                Text(s.khatmaDone, color = primary, fontWeight = FontWeight.Bold)
                if (p.completed > 0) Text(s.khatmasCompleted(p.completed), style = MaterialTheme.typography.bodySmall)
                Button(onClick = { p.restart() }, modifier = Modifier.fillMaxWidth()) { Text(s.newKhatma) }
            } else {
                Reveal(goal > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            Text(s.todayPortion, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (p.todayCount >= goal) s.portionDone else "${p.todayCount} / $goal",
                                style = MaterialTheme.typography.labelLarge, color = if (p.todayCount >= goal) primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { today }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                            color = Gold, trackColor = track, drawStopIndicator = {}
                        )
                    }
                }
                Text(s.khatmaPlanLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QuranProgress.PLANS) { d -> ChipButton(s.planDays(d), d == p.planDays) { p.changePlan(d) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val (sn, an) = QuranMeta.locate(QuranMeta.PAGES[p.nextUnread() - 1])
                        open(surahRoute(sn, an, "khatma"))
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null); Text("  " + s.continuePortion, maxLines = 1)
                    }
                    if (p.readCount > 0) OutlinedButton(onClick = { p.restart() }) { Text(s.newKhatma, maxLines = 1) }
                }
            }
        }
    }
}
