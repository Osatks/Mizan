package com.osatks.mizan.core

/**
 * زكاة بهيمة الأنعام والزروع والمعادن والركاز. الحساب على مذهب الجمهور، والخلاف مبيّن في الأدلة.
 *
 * الأنعام (السائمة التي حال عليها الحول): كتاب أبي بكر في الصدقات (البخاري 1454) للإبل والغنم،
 * وحديث معاذ في البقر (أبو داود 1576، الترمذي 623).
 */
object ZakatKinds {
    /** ما يجب في الأنعام: نوع السن (shah شاة، bintMakhad بنت مخاض، bintLabun بنت لبون، hiqqah حقة،
     *  jadhaah جذعة، tabi تبيع، musinnah مسنة) وعدده */
    data class Due(val kind: String, val count: Int)

    const val SHEEP_NISAB = 40
    const val CATTLE_NISAB = 30
    const val CAMEL_NISAB = 5

    fun sheep(n: Int): List<Due> = when {
        n < 40 -> emptyList()
        n <= 120 -> listOf(Due("shah", 1))
        n <= 200 -> listOf(Due("shah", 2))
        n <= 399 -> listOf(Due("shah", 3))
        else -> listOf(Due("shah", n / 100))
    }

    /** في كل ثلاثين تبيع، وفي كل أربعين مسنة؛ يُختار ما يستوعب أكثر العدد، وعند التساوي المسنّات (أنفع للفقراء) */
    fun cattle(n: Int): List<Due> {
        if (n < 30) return emptyList()
        if (n < 40) return listOf(Due("tabi", 1))
        if (n < 60) return listOf(Due("musinnah", 1))
        val (a, b) = bestSplit(n, 30, 40)
        return listOfNotNull(Due("tabi", a).takeIf { a > 0 }, Due("musinnah", b).takeIf { b > 0 })
    }

    /** الإبل: شياه حتى أربع وعشرين، ثم أسنان الإبل، وما زاد على عشرين ومائة ففي كل أربعين بنت لبون وفي كل خمسين حقة */
    fun camels(n: Int): List<Due> = when {
        n < 5 -> emptyList()
        n < 25 -> listOf(Due("shah", n / 5))
        n <= 35 -> listOf(Due("bintMakhad", 1))
        n <= 45 -> listOf(Due("bintLabun", 1))
        n <= 60 -> listOf(Due("hiqqah", 1))
        n <= 75 -> listOf(Due("jadhaah", 1))
        n <= 90 -> listOf(Due("bintLabun", 2))
        n <= 120 -> listOf(Due("hiqqah", 2))
        else -> {
            val (a, b) = bestSplit(n, 40, 50)
            listOfNotNull(Due("bintLabun", a).takeIf { a > 0 }, Due("hiqqah", b).takeIf { b > 0 })
        }
    }

    /** أكبر ما يُستوعب من n بمضاعفات x و y (عدد x وعدد y)، وعند التساوي يُقدَّم الأكبر y */
    private fun bestSplit(n: Int, x: Int, y: Int): Pair<Int, Int> {
        var best = 0 to 0; var cover = -1
        for (b in 0..n / y) {
            val a = (n - b * y) / x
            val c = a * x + b * y
            if (c > cover || (c == cover && b > best.second)) { cover = c; best = a to b }
        }
        return best
    }

    /** نصاب الزروع والثمار: خمسة أوسق، نحو 612 كغ من القمح (والتقديرات بين 612 و653 كغ) */
    const val CROPS_NISAB_KG = 612.0

    /** نسبة الواجب: rain العشر (بلا كلفة)، irrigated نصف العشر (بكلفة)، mixed ثلاثة أرباع العشر */
    fun cropsRate(irrigation: String): Double = when (irrigation) {
        "irrigated" -> 0.05
        "mixed" -> 0.075
        else -> 0.10
    }

    fun crops(kg: Double, irrigation: String): Double = if (kg >= CROPS_NISAB_KG) kg * cropsRate(irrigation) else 0.0

    /** الركاز (دفين الجاهلية): الخمس في الحال بلا نصاب ولا حول عند الجمهور */
    fun rikaz(value: Double): Double = value.coerceAtLeast(0.0) * 0.2

    /** المعادن المستخرجة من الأرض: ربع العشر إذا بلغت نصاب الذهب (الحنابلة والشافعية) */
    fun mineral(value: Double, goldNisabValue: Double): Double =
        if (goldNisabValue > 0 && value >= goldNisabValue) value * 0.025 else 0.0
}
