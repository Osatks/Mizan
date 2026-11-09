package com.osatks.mizan.ui.screens

import androidx.compose.ui.graphics.Color
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.core.InheritanceResult
import com.osatks.mizan.i18n.Strings

/** ألوان أشرطة الأنصبة (وتستعملها حاسبة النظام في iOS) */
internal val INH_BAR_COLORS = listOf(
    Color(0xFF0D5C4B), Color(0xFFB88A1E), Color(0xFF2F7FB5), Color(0xFFB4412F), Color(0xFF7A52B3),
    Color(0xFF2E9E74), Color(0xFFD07A2B), Color(0xFF4B6B8A), Color(0xFFA0527A), Color(0xFF5C8F2E),
)

/** نص مشاركة القسمة: كل وارث له نصيب بكسره ونسبته */
internal fun inheritanceShareText(s: Strings, active: Map<String, Int>, result: InheritanceResult): String {
    val text = Inheritance.HEIRS.filter { active.containsKey(it.key) && result.res[it.key]?.share != null }
        .joinToString(s.listSeparator) { "${s.heirs[it.key]}: ${result.res[it.key]!!.share} (${Fmt.num(result.res[it.key]!!.share!!.value * 100, 1)}%)" }
    return s.divisionShareText(text)
}

/** صافي التركة بعد التجهيز والديون والوصية (لا تُنفَّذ فوق الثلث)، وهل زادت الوصية على الثلث */
internal fun estateNet(estate: String, funeral: String, debts: String, will: String): Pair<Double, Boolean> {
    val afterDebt = (Fmt.parse(estate) - Fmt.parse(funeral) - Fmt.parse(debts)).coerceAtLeast(0.0)
    val willCapped = Fmt.parse(will) > afterDebt / 3 + 1e-9
    val net = (afterDebt - minOf(Fmt.parse(will), afterDebt / 3)).coerceAtLeast(0.0)
    return net to willCapped
}

/** الورثة الموجودون بأعدادهم، بلا من لا يكون مع جنس المتوفى [gender] */
internal fun activeHeirs(counts: Map<String, Int>, gender: Char): Map<String, Int> =
    Inheritance.HEIRS.filter { (it.only == null || it.only == gender) && (counts[it.key] ?: 0) > 0 }
        .associate { it.key to (counts[it.key] ?: 0) }
