package com.osatks.mizan.ui.screens

import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.osatks.mizan.i18n.LocalLang
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Demo
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.Motion
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
    val s = LocalStrings.current
    var gender by remember { mutableStateOf('m') }
    val counts = remember { mutableStateMapOf<String, Int>().apply { if (Demo.enabled) putAll(Demo.heirs) } }
    var estate by remember { mutableStateOf(if (Demo.enabled) Demo.ESTATE else "") }
    var funeral by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }
    var will by remember { mutableStateOf("") }

    val afterDebt = (Fmt.parse(estate) - Fmt.parse(funeral) - Fmt.parse(debts)).coerceAtLeast(0.0)
    val willCapped = Fmt.parse(will) > afterDebt / 3 + 1e-9
    val net = (afterDebt - minOf(Fmt.parse(will), afterDebt / 3)).coerceAtLeast(0.0)

    val active = Inheritance.HEIRS.filter { (it.only == null || it.only == gender) && (counts[it.key] ?: 0) > 0 }
        .associate { it.key to (counts[it.key] ?: 0) }
    val result = Inheritance.solve(active, gender)

    val arrow = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "←" else "→"
    ToolPage(s.inhTitle, s.inhSubtitle, onBack, sharedKey = "inheritance", icon = TOOL_ICONS["inheritance"]) {
        if (result != null) {
            item {
                ResultCard(
                    s.aslLabel,
                    if (result.awl != null) "${result.asl} $arrow ${result.awl}" else "${result.asl}",
                    when { result.awl != null -> s.awlCase; result.radd -> s.raddCase; else -> s.adilCase },
                    result.awl == null
                ) {
                    if (result.tashih != result.asl) Text(s.tashihFrom(result.tashih), color = Color.White.copy(alpha = .85f))
                }
            }
            item {
                SectionCard(s.sharesTitle) {
                    var ci = 0
                    Inheritance.HEIRS.forEach { h ->
                        val c = active[h.key] ?: return@forEach
                        val sh = result.res[h.key]
                        val share = sh?.share
                        val name = s.heirs[h.key] ?: h.key
                        if (sh == null || sh.blocked != null || share == null) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text("$name${if (c > 1) " ×$c" else ""}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(s.blockedBy(sh?.blocked?.let { s.nameOf(it) } ?: "—"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            val pct = share.value * 100
                            val amount = net * share.value
                            val color = BAR_COLORS[ci++ % BAR_COLORS.size]
                            // الشريط يمتد من الصفر إلى النصيب عند الظهور ويتحرك عند كل تغيير
                            var shown by remember { mutableStateOf(if (Motion.reduced) share.value.toFloat().coerceIn(0f, 1f) else 0f) }
                            androidx.compose.runtime.LaunchedEffect(share.value) { shown = share.value.toFloat().coerceIn(0f, 1f) }
                            val width by animateFloatAsState(shown, spring(dampingRatio = 0.8f, stiffness = 140f), label = "bar")
                            val barColor by animateColorAsState(color, tween(Motion.MEDIUM), label = "barColor")
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$name${if (c > 1) " ×$c" else ""}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("$share  ·  ${Fmt.num(pct, 1)}%", fontWeight = FontWeight.Bold)
                                }
                                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                    if (width > 0f) Box(Modifier.fillMaxWidth(width).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(barColor))
                                }
                                Text(
                                    s.shareType(sh.type, sh.radd) + if (net > 0) "  ·  ${Fmt.num(amount)}" + (if (c > 1) " (${s.perPerson(Fmt.num(amount / c))})" else "") else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // دليل هذا النصيب
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook, null,
                                        Modifier.size(14.dp), tint = com.osatks.mizan.ui.theme.GoldDeep
                                    )
                                    Text(
                                        " " + com.osatks.mizan.i18n.Evidence.heirSource(h.key)[LocalLang.current],
                                        style = MaterialTheme.typography.labelMedium, color = com.osatks.mizan.ui.theme.GoldDeep, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Button(onClick = {
                        val text = Inheritance.HEIRS.filter { active.containsKey(it.key) && result.res[it.key]?.share != null }
                            .joinToString(s.listSeparator) { "${s.heirs[it.key]}: ${result.res[it.key]!!.share} (${Fmt.num(result.res[it.key]!!.share!!.value * 100, 1)}%)" }
                        shareText(s, s.divisionShareText(text))
                    }, modifier = Modifier.fillMaxWidth()) { Text(s.shareDivision) }
                }
            }
            result.notes.forEach { n -> item { Note(s.inhNote(n)) } }
        } else item { ResultCard(s.aslLabel, "—", s.addHeirs) }

        item {
            SectionCard(s.deceasedAndEstate) {
                Segmented(listOf("m" to s.deceasedMale, "f" to s.deceasedFemale), gender.toString()) { gender = it[0] }
                NumField(s.estateTotal, estate, { estate = it })
                NumField(s.funeralCosts, funeral, { funeral = it })
                NumField(s.deceasedDebts, debts, { debts = it })
                NumField(s.bequest, will, { will = it }, null, s.bequestHint)
                if (Fmt.parse(estate) > 0) StatRow(s.netEstate, Fmt.num(net) + if (willCapped) " " + s.bequestCapped else "", strong = true)
            }
        }
        Inheritance.HEIRS.filter { it.only == null || it.only == gender }.groupBy { it.group }.forEach { (group, heirs) ->
            item {
                SectionCard(s.heirGroups[group] ?: group) {
                    heirs.forEach { h ->
                        val v = counts[h.key] ?: 0
                        val bg by animateColorAsState(if (v > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "heir")
                        val border by animateColorAsState(if (v > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "heirB")
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = bg,
                            border = BorderStroke(1.dp, border)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(s.heirs[h.key] ?: h.key, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                MiniStepper(v, { counts[h.key] = it }, 0, h.max)
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { counts.clear(); estate = ""; funeral = ""; debts = ""; will = "" }, modifier = Modifier.fillMaxWidth()) {
                Text(s.newCase)
            }
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.inheritance, startOpen = result != null) }
        item { Note(s.inhDisclaimer, warn = true) }
    }
}
