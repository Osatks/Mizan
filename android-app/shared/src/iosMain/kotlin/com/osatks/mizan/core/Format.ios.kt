package com.osatks.mizan.core

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfEven

private val cache = HashMap<String, NSNumberFormatter>()

actual fun formatNumber(v: Double, digits: Int, decimal: Char, group: Char): String {
    val f = cache.getOrPut("$digits$decimal$group") {
        NSNumberFormatter().apply {
            // اللغة أولاً: ضبطها بعد الفواصل يعيدها إلى فواصل اللغة
            setLocale(platform.Foundation.NSLocale(localeIdentifier = "en_US_POSIX"))
            numberStyle = NSNumberFormatterDecimalStyle
            usesGroupingSeparator = true
            groupingSize = 3u
            decimalSeparator = decimal.toString()
            groupingSeparator = group.toString()
            minimumFractionDigits = 0u
            maximumFractionDigits = digits.toULong()
            roundingMode = NSNumberFormatterRoundHalfEven
        }
    }
    return f.stringFromNumber(NSNumber(double = v)) ?: v.toString()
}
