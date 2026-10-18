package com.osatks.mizan.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val cache = HashMap<String, DecimalFormat>()

actual fun formatNumber(v: Double, digits: Int, decimal: Char, group: Char): String {
    val f = synchronized(cache) {
        cache.getOrPut("$digits$decimal$group") {
            val sym = DecimalFormatSymbols(Locale.US).apply { decimalSeparator = decimal; groupingSeparator = group }
            DecimalFormat(if (digits == 0) "#,##0" else "#,##0." + "#".repeat(digits), sym)
        }
    }
    return synchronized(f) { f.format(v) }
}
