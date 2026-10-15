package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.ui.MiniStepper
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.shareText

private val BAR_COLORS = listOf(
    Color(0xFF0D5C4B), Color(0xFFB88A1E), Color(0xFF2F7FB5), Color(0xFFB4412F), Color(0xFF7A52B3),
    Color(0xFF2E9E74), Color(0xFFD07A2B), Color(0xFF4B6B8A), Color(0xFFA0527A), Color(0xFF5C8F2E),
)

@Composable
fun InheritanceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var gender by remember { mutableStateOf('m') }
    val counts = remember { mutableStateMapOf<String, Int>() }
    var estate by remember { mutableStateOf("") }
    var funeral by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }
    var will by remember { mutableStateOf("") }

    val afterDebt = (Fmt.parse(estate) - Fmt.parse(funeral) - Fmt.parse(debts)).coerceAtLeast(0.0)
    val willCapped = Fmt.parse(will) > afterDebt / 3 + 1e-9
    val net = (afterDebt - minOf(Fmt.parse(will), afterDebt / 3)).coerceAtLeast(0.0)

    val active = Inheritance.HEIRS.filter { (it.only == null || it.only == gender) && (counts[it.key] ?: 0) > 0 }
        .associate { it.key to (counts[it.key] ?: 0) }
    val result = Inheritance.solve(active, gender)

    ToolPage("حاسبة المواريث", "تقسيم التركة وفق علم الفرائض", onBack) {
        if (result != null) {
            item {
                ResultCard(
                    "أصل المسألة",
                    if (result.awl != null) "${result.asl} ← ${result.awl}" else "${result.asl}",
                    when { result.awl != null -> "مسألة عائلة"; result.radd -> "مسألة فيها ردّ"; else -> "مسألة عادلة" },
                    result.awl == null
                ) {
                    if (result.tashih != result.asl) Text("تصح من ${result.tashih} سهماً", color = Color.White.copy(alpha = .85f))
                }
            }
            item {
                SectionCard("نصيب كل وارث") {
                    var ci = 0
                    Inheritance.HEIRS.forEach { h ->
                        val c = active[h.key] ?: return@forEach
                        val s = result.res[h.key]
                        val share = s?.share
                        if (s == null || s.blocked != null || share == null) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("${h.name}${if (c > 1) " ×$c" else ""}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("محجوب بـ${s?.blocked ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            val pct = share.value * 100
                            val amount = net * share.value
                            val color = BAR_COLORS[ci++ % BAR_COLORS.size]
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${h.name}${if (c > 1) " ×$c" else ""}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("$share  ·  ${Fmt.num(pct, 1)}%", fontWeight = FontWeight.Bold)
                                }
                                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    Box(Modifier.fillMaxWidth((share.value).toFloat().coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(color))
                                }
                                Text(
                                    s.type + if (net > 0) "  ·  ${Fmt.num(amount)}" + (if (c > 1) " (للفرد ${Fmt.num(amount / c)})" else "") else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Button(onClick = {
                        val text = Inheritance.HEIRS.filter { active.containsKey(it.key) && result.res[it.key]?.share != null }
                            .joinToString("، ") { "${it.name}: ${result.res[it.key]!!.share} (${Fmt.num(result.res[it.key]!!.share!!.value * 100, 1)}%)" }
                        shareText(ctx, "تقسيم التركة: $text")
                    }, modifier = Modifier.fillMaxWidth()) { Text("مشاركة القسمة") }
                }
            }
            result.notes.forEach { n -> item { Note(n) } }
        } else item { ResultCard("أصل المسألة", "—", "أضف الورثة من القائمة أدناه") }

        item {
            SectionCard("المتوفى والتركة") {
                Segmented(listOf("m" to "المتوفى ذكر", "f" to "المتوفاة أنثى"), gender.toString()) { gender = it[0] }
                NumField("إجمالي التركة", estate, { estate = it })
                NumField("تكاليف التجهيز والدفن", funeral, { funeral = it })
                NumField("الديون على المتوفى", debts, { debts = it })
                NumField("الوصية (لغير وارث)", will, { will = it }, null, "لا تزيد على ثلث الباقي بعد الديون إلا بإجازة الورثة")
                if (Fmt.parse(estate) > 0) StatRow("صافي التركة القابل للقسمة", Fmt.num(net) + if (willCapped) " (قُصرت الوصية على الثلث)" else "", strong = true)
            }
        }
        Inheritance.HEIRS.filter { it.only == null || it.only == gender }.groupBy { it.group }.forEach { (group, heirs) ->
            item {
                SectionCard(group) {
                    heirs.forEach { h ->
                        val v = counts[h.key] ?: 0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (v > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (v > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(h.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                MiniStepper(v, { counts[h.key] = it }, 0, h.max)
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { counts.clear(); estate = ""; funeral = ""; debts = ""; will = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("بدء مسألة جديدة")
            }
        }
        item { Note("النتيجة إرشادية مبنية على مذهب الجمهور، ولا تغني عن مراجعة المحكمة الشرعية أو عالم متخصص في الفرائض.", warn = true) }
    }
}
