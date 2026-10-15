package com.osatks.mizan.core

import com.osatks.mizan.i18n.Lang
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** تنسيق الأرقام: أرقام لاتينية دائماً، مع فاصلي الكسور والآلاف المعتادين في لغة الواجهة */
object Fmt {
    private val cache = HashMap<Lang, Array<DecimalFormat>>()

    private fun formats(lang: Lang): Array<DecimalFormat> = cache.getOrPut(lang) {
        val sym = DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = lang.decimal
            groupingSeparator = lang.group
        }
        arrayOf(DecimalFormat("#,##0", sym), DecimalFormat("#,##0.#", sym), DecimalFormat("#,##0.##", sym))
    }

    fun num(v: Double, digits: Int = 2, lang: Lang = Lang.current): String =
        formats(lang)[digits.coerceIn(0, 2)].format(v)

    fun int(v: Long, lang: Lang = Lang.current): String = formats(lang)[0].format(v)

    /** رقمان دائماً (للساعات والدقائق) دون الاعتماد على لغة الجهاز */
    fun two(v: Long): String = if (v < 10) "0$v" else "$v"

    private const val ARABIC = "٠١٢٣٤٥٦٧٨٩"
    private const val PERSIAN = "۰۱۲۳۴۵۶۷۸۹"
    private const val BENGALI = "০১২৩৪৫৬৭৮৯"

    /**
     * يحوّل نص المستخدم إلى رقم موجب. يقبل الأرقام العربية والفارسية والبنغالية، ويميّز فاصل الكسور:
     * إذا ظهر الفاصلان فالأخير للكسور، وإذا ظهر واحد مرة واحدة فهو للكسور ما لم يكن فاصل آلاف في
     * لغة الواجهة وبعده ثلاثة أرقام بالضبط (مثل 1.500 بالألمانية).
     */
    fun parse(text: String, lang: Lang = Lang.current): Double {
        val raw = buildString {
            for (ch in text) {
                val d = maxOf(ARABIC.indexOf(ch), PERSIAN.indexOf(ch), BENGALI.indexOf(ch))
                when {
                    d >= 0 -> append('0' + d)
                    ch == '٫' -> append('.')
                    ch == '٬' || ch == '،' || ch == '\'' || ch.isWhitespace() || ch == ' ' || ch == ' ' -> {}
                    else -> append(ch)
                }
            }
        }
        val dot = raw.lastIndexOf('.'); val comma = raw.lastIndexOf(',')
        val cleaned = when {
            dot >= 0 && comma >= 0 -> if (dot > comma) raw.replace(",", "") else raw.replace(".", "").replace(',', '.')
            dot >= 0 || comma >= 0 -> {
                val sep = if (dot >= 0) '.' else ','
                val count = raw.count { it == sep }
                val after = raw.length - raw.lastIndexOf(sep) - 1
                val isGroup = count > 1 || (sep == lang.group && after == 3)
                if (isGroup) raw.replace(sep.toString(), "") else raw.replace(sep, '.')
            }
            else -> raw
        }
        val v = cleaned.toDoubleOrNull() ?: return 0.0
        return if (v.isFinite() && v > 0) v else 0.0
    }
}
