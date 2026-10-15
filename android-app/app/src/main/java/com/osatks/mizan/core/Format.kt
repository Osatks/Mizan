package com.osatks.mizan.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** تنسيق الأرقام بأرقام لاتينية وفواصل آلاف */
object Fmt {
    private val symbols = DecimalFormatSymbols(Locale.US)
    private val two = DecimalFormat("#,##0.##", symbols)
    private val one = DecimalFormat("#,##0.#", symbols)
    private val zero = DecimalFormat("#,##0", symbols)

    fun num(v: Double, digits: Int = 2): String = when (digits) {
        0 -> zero.format(v)
        1 -> one.format(v)
        else -> two.format(v)
    }

    fun int(v: Long): String = zero.format(v)

    /** يحوّل نص المستخدم إلى رقم (يقبل الأرقام العربية والفواصل) */
    fun parse(text: String): Double {
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        val cleaned = buildString {
            for (ch in text) {
                val i = arabic.indexOf(ch)
                when {
                    i >= 0 -> append(('0' + i))
                    ch == '٫' -> append('.')
                    ch == ',' || ch == '،' || ch.isWhitespace() -> {}
                    else -> append(ch)
                }
            }
        }
        val v = cleaned.toDoubleOrNull() ?: return 0.0
        return if (v.isFinite() && v > 0) v else 0.0
    }
}
