package com.osatks.mizan.core

import kotlin.math.abs
import kotlin.math.roundToLong

/** كسر نسبي دقيق */
class Fr private constructor(val n: Long, val d: Long) : Comparable<Fr> {
    companion object {
        tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) (if (a == 0L) 1L else abs(a)) else gcd(b, a % b)
        fun lcm(a: Long, b: Long): Long = a / gcd(a, b) * b
        fun of(n: Long, d: Long = 1): Fr {
            var nn = n; var dd = d
            if (dd < 0) { nn = -nn; dd = -dd }
            val g = gcd(abs(nn), dd)
            return Fr(nn / g, dd / g)
        }
        val ZERO = of(0)
        val ONE = of(1)
    }
    operator fun plus(o: Fr) = of(n * o.d + o.n * d, d * o.d)
    operator fun minus(o: Fr) = of(n * o.d - o.n * d, d * o.d)
    operator fun times(o: Fr) = of(n * o.n, d * o.d)
    operator fun times(k: Int) = of(n * k, d)
    operator fun div(o: Fr) = of(n * o.d, d * o.n)
    operator fun div(k: Int) = of(n, d * k)
    override fun compareTo(other: Fr): Int = (n * other.d).compareTo(other.n * d)
    override fun equals(other: Any?) = other is Fr && other.n == n && other.d == d
    override fun hashCode() = (n * 31 + d).toInt()
    val value: Double get() = n.toDouble() / d
    fun isZero() = n == 0L
    override fun toString() = if (d == 1L) "$n" else "$n/$d"
}

data class Heir(val key: String, val name: String, val max: Int, val only: Char? = null, val group: String)

class Share(var share: Fr? = null, var type: String = "", var blocked: String? = null)

data class InheritanceResult(
    val res: Map<String, Share>,
    val notes: List<String>,
    val asl: Long,
    val awl: Long?,
    val tashih: Long,
    val radd: Boolean
)

/** محرك علم الفرائض على مذهب الجمهور */
object Inheritance {
    val HEIRS = listOf(
        Heir("husband", "الزوج", 1, 'f', "الزوجان"),
        Heir("wife", "الزوجة", 4, 'm', "الزوجان"),
        Heir("father", "الأب", 1, null, "الأصول"),
        Heir("mother", "الأم", 1, null, "الأصول"),
        Heir("gf", "الجد (أبو الأب)", 1, null, "الأصول"),
        Heir("gmm", "الجدة (أم الأم)", 1, null, "الأصول"),
        Heir("gmf", "الجدة (أم الأب)", 1, null, "الأصول"),
        Heir("son", "الابن", 30, null, "الفروع"),
        Heir("daughter", "البنت", 30, null, "الفروع"),
        Heir("gson", "ابن الابن", 30, null, "الفروع"),
        Heir("gdau", "بنت الابن", 30, null, "الفروع"),
        Heir("fb", "الأخ الشقيق", 30, null, "الإخوة والأخوات"),
        Heir("fs", "الأخت الشقيقة", 30, null, "الإخوة والأخوات"),
        Heir("pb", "الأخ لأب", 30, null, "الإخوة والأخوات"),
        Heir("ps", "الأخت لأب", 30, null, "الإخوة والأخوات"),
        Heir("mb", "الأخ لأم", 30, null, "الإخوة والأخوات"),
        Heir("ms", "الأخت لأم", 30, null, "الإخوة والأخوات"),
        Heir("fnep", "ابن الأخ الشقيق", 30, null, "العصبات البعيدة"),
        Heir("pnep", "ابن الأخ لأب", 30, null, "العصبات البعيدة"),
        Heir("fun", "العم الشقيق", 30, null, "العصبات البعيدة"),
        Heir("pun", "العم لأب", 30, null, "العصبات البعيدة"),
        Heir("fcous", "ابن العم الشقيق", 30, null, "العصبات البعيدة"),
        Heir("pcous", "ابن العم لأب", 30, null, "العصبات البعيدة"),
    )
    val NAME = HEIRS.associate { it.key to it.name }

    private fun f(n: Long, d: Long = 1) = Fr.of(n, d)

    fun solve(input: Map<String, Int>, gender: Char): InheritanceResult? {
        val c = input.toMutableMap()
        if (gender == 'm') c["husband"] = 0 else c["wife"] = 0
        fun cnt(k: String) = c[k] ?: 0
        val res = mutableMapOf<String, Share>()
        val notes = mutableListOf<String>()
        val fard = linkedMapOf<String, Fr>()
        val label = mutableMapOf<String, String>()
        fun block(k: String, by: String) { if (cnt(k) > 0) res[k] = Share(blocked = by) }

        val present = c.filter { it.value > 0 }.keys.toList()
        if (present.isEmpty()) return null

        // الفروع
        val son = cnt("son"); val dau = cnt("daughter")
        var gson = cnt("gson"); var gdau = cnt("gdau")
        if (son > 0) { block("gson", "الابن"); block("gdau", "الابن"); gson = 0; gdau = 0 }
        if (son == 0 && dau >= 2 && gdau > 0 && gson == 0) { block("gdau", "البنتين (استكمال الثلثين)"); gdau = 0 }
        val maleDesc = son > 0 || gson > 0
        val femDesc = dau > 0 || gdau > 0
        val desc = maleDesc || femDesc

        // الأصول
        val father = cnt("father"); val mother = cnt("mother")
        var gf = cnt("gf")
        if (father > 0 && gf > 0) { block("gf", "الأب"); gf = 0 }
        var gmm = cnt("gmm"); var gmf = cnt("gmf")
        if (mother > 0) { block("gmm", "الأم"); block("gmf", "الأم"); gmm = 0; gmf = 0 }
        if (father > 0 && gmf > 0) { block("gmf", "الأب"); gmf = 0 }

        val sibAll = cnt("fb") + cnt("fs") + cnt("pb") + cnt("ps") + cnt("mb") + cnt("ms")
        val spouseK = if (gender == 'm') "wife" else "husband"
        val spouse = cnt(spouseK)

        if (spouse > 0) {
            fard[spouseK] = if (gender == 'f') (if (desc) f(1, 4) else f(1, 2)) else (if (desc) f(1, 8) else f(1, 4))
            label[spouseK] = "فرض"
        }

        // الإخوة لأم
        val matN = cnt("mb") + cnt("ms")
        if (matN > 0) {
            val by = when { desc -> "الفرع الوارث"; father > 0 -> "الأب"; gf > 0 -> "الجد"; else -> null }
            if (by != null) { block("mb", by); block("ms", by) } else {
                val total = if (matN == 1) f(1, 6) else f(1, 3)
                if (cnt("mb") > 0) { fard["mb"] = total * cnt("mb") / matN; label["mb"] = "فرض" }
                if (cnt("ms") > 0) { fard["ms"] = total * cnt("ms") / matN; label["ms"] = "فرض" }
                if (matN > 1) notes += "الإخوة لأم يشتركون في الثلث بالتساوي بين الذكر والأنثى."
            }
        }

        // الإخوة الأشقاء ولأب
        var fb = cnt("fb"); var fs = cnt("fs"); var pb = cnt("pb"); var ps = cnt("ps")
        val sibBlocker = when { son > 0 -> "الابن"; gson > 0 -> "ابن الابن"; father > 0 -> "الأب"; else -> null }
        if (sibBlocker != null) { listOf("fb", "fs", "pb", "ps").forEach { block(it, sibBlocker) }; fb = 0; fs = 0; pb = 0; ps = 0 }
        val jaddMode = gf > 0 && (fb + fs + pb + ps) > 0

        // البنات وبنات الابن
        if (dau > 0 && son == 0) { fard["daughter"] = if (dau == 1) f(1, 2) else f(2, 3); label["daughter"] = "فرض" }
        if (gdau > 0 && gson == 0) {
            if (dau == 0) { fard["gdau"] = if (gdau == 1) f(1, 2) else f(2, 3); label["gdau"] = "فرض" }
            else if (dau == 1) { fard["gdau"] = f(1, 6); label["gdau"] = "فرض (تكملة الثلثين)" }
        }

        // الأم والجدات
        if (mother > 0) {
            if (desc || sibAll >= 2) fard["mother"] = f(1, 6)
            else if (spouse > 0 && father > 0 && present.all { it in listOf("husband", "wife", "father", "mother") || res[it]?.blocked != null }) {
                fard["mother"] = (Fr.ONE - fard.getValue(spouseK)) / 3
                notes += "هذه إحدى المسألتين العُمَريّتين (الغرّاوين): للأم ثلث الباقي بعد نصيب الزوج أو الزوجة."
            } else fard["mother"] = f(1, 3)
            label["mother"] = "فرض"
        }
        if (gmm + gmf > 0) {
            val n = gmm + gmf
            if (gmm > 0) { fard["gmm"] = f(1, 6) / n; label["gmm"] = "فرض" }
            if (gmf > 0) { fard["gmf"] = f(1, 6) / n; label["gmf"] = "فرض" }
            if (n > 1) notes += "الجدتان تشتركان في السدس."
        }

        // الأب والجد
        if (father > 0) {
            if (maleDesc) { fard["father"] = f(1, 6); label["father"] = "فرض" }
            else if (femDesc) { fard["father"] = f(1, 6); label["father"] = "فرض وتعصيب" }
        }
        if (gf > 0 && !jaddMode) {
            if (maleDesc) { fard["gf"] = f(1, 6); label["gf"] = "فرض" }
            else if (femDesc) { fard["gf"] = f(1, 6); label["gf"] = "فرض وتعصيب" }
        }

        // الأخوات (خارج باب الجد)
        var fsGhayr = false; var psGhayr = false
        if (!jaddMode) {
            if (fs > 0 && fb == 0) {
                if (femDesc) { fsGhayr = true; notes += "الأخوات الشقيقات مع البنات عصبة مع الغير." }
                else { fard["fs"] = if (fs == 1) f(1, 2) else f(2, 3); label["fs"] = "فرض" }
            }
            if (fb > 0) { block("pb", "الأخ الشقيق"); block("ps", "الأخ الشقيق"); pb = 0; ps = 0 }
            else if (fsGhayr) { block("pb", "الأخت الشقيقة (عصبة مع الغير)"); block("ps", "الأخت الشقيقة (عصبة مع الغير)"); pb = 0; ps = 0 }
            if (ps > 0 && pb == 0) {
                if (femDesc && fs == 0) psGhayr = true
                else if (fs == 0) { fard["ps"] = if (ps == 1) f(1, 2) else f(2, 3); label["ps"] = "فرض" }
                else if (fs == 1) { fard["ps"] = f(1, 6); label["ps"] = "فرض (تكملة الثلثين)" }
                else { block("ps", "الأختين الشقيقتين"); ps = 0 }
            }
        }

        var fardSum = fard.values.fold(Fr.ZERO) { a, b -> a + b }

        // الأكدرية
        val onlyKeys = { keys: List<String> -> present.all { it in keys || res[it]?.blocked != null } }
        if (jaddMode && gender == 'f' && spouse > 0 && mother > 0 && (fs + ps) == 1 && fb == 0 && pb == 0 &&
            onlyKeys(listOf("husband", "mother", "gf", "fs", "ps"))
        ) {
            val sk = if (fs > 0) "fs" else "ps"
            res["husband"] = Share(f(9, 27), "فرض"); res["mother"] = Share(f(6, 27), "فرض")
            res["gf"] = Share(f(8, 27), "فرض ثم مقاسمة"); res[sk] = Share(f(4, 27), "فرض ثم مقاسمة")
            notes += "هذه المسألة الأكدرية: يُفرض للأخت النصف وللجد السدس فتعول إلى 9، ثم يُجمع نصيبهما ويُقسم للذكر مثل حظ الأنثيين فتصح من 27."
            return InheritanceResult(res, notes, 6, 9, 27, false)
        }

        // الجد مع الإخوة (المقاسمة عند الجمهور)
        if (jaddMode) {
            val rem = Fr.ONE - fardSum
            val units = 2 + fb * 2 + fs + pb * 2 + ps
            val muq = if (rem > Fr.ZERO) rem * 2 / units else Fr.ZERO
            val otherFard = fardSum > Fr.ZERO
            val gfShare: Fr; val how: String
            if (!otherFard) {
                val third = f(1, 3)
                if (muq >= third) { gfShare = muq; how = "المقاسمة" } else { gfShare = third; how = "ثلث المال" }
            } else {
                val sixth = f(1, 6)
                val thirdRem = if (rem > Fr.ZERO) rem / 3 else Fr.ZERO
                var best = sixth; var bestHow = "سدس المال"
                if (thirdRem > best) { best = thirdRem; bestHow = "ثلث الباقي" }
                if (muq > best) { best = muq; bestHow = "المقاسمة" }
                gfShare = best; how = bestHow
            }
            res["gf"] = Share(gfShare, "مع الإخوة: $how")
            fardSum += gfShare
            var sibRem = Fr.ONE - fardSum
            if (sibRem < Fr.ZERO) sibRem = Fr.ZERO
            notes += "الجد مع الإخوة يأخذ الأحظّ له من: المقاسمة، أو " + (if (otherFard) "ثلث الباقي، أو سدس المال" else "ثلث المال") +
                " — وهو قول الجمهور (المالكية والشافعية والحنابلة)، أما أبو حنيفة فيرى أن الجد يحجب الإخوة كالأب."
            if (fb + fs > 0 && pb + ps > 0) notes += "الإخوة لأب عُدّوا على الجد في المقاسمة (المعادّة) ثم يُرد نصيبهم للأشقاء عند الحاجة."

            fun give(bk: String, sk: String, b: Int, s: Int, amount: Fr, type: String) {
                val u = b * 2 + s
                if (u == 0) return
                if (b > 0) res[bk] = Share(amount * (b * 2) / u, type)
                if (s > 0) res[sk] = Share(amount * s / u, type)
            }
            when {
                fb > 0 -> {
                    give("fb", "fs", fb, fs, sibRem, "تعصيب")
                    if (pb > 0) res["pb"] = Share(blocked = "الأخ الشقيق (بعد المعادّة)")
                    if (ps > 0) res["ps"] = Share(blocked = "الأخ الشقيق (بعد المعادّة)")
                }
                fs > 0 -> if (pb + ps > 0) {
                    val cap = if (fs == 1) f(1, 2) else f(2, 3)
                    val fsGet = if (sibRem > cap) cap else sibRem
                    res["fs"] = Share(fsGet, "تعصيب مع الجد")
                    val left = sibRem - fsGet
                    if (left > Fr.ZERO) give("pb", "ps", pb, ps, left, "تعصيب")
                    else {
                        if (pb > 0) res["pb"] = Share(blocked = "استغراق الأخت الشقيقة للباقي")
                        if (ps > 0) res["ps"] = Share(blocked = "استغراق الأخت الشقيقة للباقي")
                    }
                } else give("fb", "fs", 0, fs, sibRem, "تعصيب مع الجد")
                else -> give("pb", "ps", pb, ps, sibRem, "تعصيب مع الجد")
            }
            listOf("fnep", "pnep", "fun", "pun", "fcous", "pcous").forEach { block(it, "الجد والإخوة") }
        }

        val fardDen = fard.values.fold(1L) { a, x -> Fr.lcm(a, x.d) }
        val hasFard = fard.isNotEmpty()

        // العصبات
        var asabaTaken = jaddMode
        if (!jaddMode) {
            data class Rank(val m: String?, val f: String?, val nm: String, val ghayr: Boolean = false)
            val order = listOf(
                Rank("son", "daughter", "الابن"), Rank("gson", "gdau", "ابن الابن"),
                Rank("father", null, "الأب"), Rank("gf", null, "الجد"),
                Rank("fb", "fs", "الأخ الشقيق"), Rank(null, "fs", "الأخت الشقيقة", true),
                Rank("pb", "ps", "الأخ لأب"), Rank(null, "ps", "الأخت لأب", true),
                Rank("fnep", null, "ابن الأخ الشقيق"), Rank("pnep", null, "ابن الأخ لأب"),
                Rank("fun", null, "العم الشقيق"), Rank("pun", null, "العم لأب"),
                Rank("fcous", null, "ابن العم الشقيق"), Rank("pcous", null, "ابن العم لأب"),
            )
            val counts = mapOf("son" to son, "daughter" to dau, "gson" to gson, "gdau" to gdau, "father" to father,
                "gf" to gf, "fb" to fb, "fs" to fs, "pb" to pb, "ps" to ps)
            fun eff(k: String) = counts[k] ?: cnt(k)
            fun live(k: String) = eff(k) > 0 && res[k]?.blocked == null
            var winner: Rank? = null
            for (r in order) {
                if (r.ghayr) {
                    val ok = (r.f == "fs" && fsGhayr) || (r.f == "ps" && psGhayr)
                    if (ok && winner == null) winner = r
                    continue
                }
                if (r.m == null || !live(r.m)) continue
                if (winner == null) winner = r
                else {
                    block(r.m, winner.nm)
                    if (r.f != null && live(r.f) && !fard.containsKey(r.f)) block(r.f, winner.nm)
                }
            }
            val rem = Fr.ONE - fardSum
            if (winner != null) {
                asabaTaken = true
                val residue = if (rem > Fr.ZERO) rem else Fr.ZERO
                val mC = if (winner.m != null) eff(winner.m) else 0
                val fC = if (winner.f != null && !fard.containsKey(winner.f)) eff(winner.f) else 0
                val u = mC * 2 + fC
                val tLabel = if (winner.m != null) "تعصيب" else "تعصيب مع الغير"
                if (winner.m != null) {
                    val s = residue * (mC * 2) / u
                    val own = fard[winner.m]
                    res[winner.m] = Share((own ?: Fr.ZERO) + s,
                        if (own != null) label[winner.m] ?: "فرض وتعصيب" else if (fC > 0) "تعصيب بالغير" else tLabel)
                    fard.remove(winner.m)
                }
                if (fC > 0 && winner.f != null) res[winner.f] = Share(residue * fC / u, if (winner.m != null) "تعصيب بالغير" else tLabel)
                if (residue.isZero() && (mC > 0 || fC > 0)) notes += "لم يبقَ شيء بعد أصحاب الفروض، فلا شيء للعاصب (${winner.nm})."
                if (residue.isZero() && winner.m == "fb" && matN >= 2 && spouse > 0 && (mother > 0 || gmm > 0 || gmf > 0) && !desc && father == 0 && gf == 0) {
                    notes += "هذه المسألة المُشتَرَكة (الحِماريّة): ذهب المالكية والشافعية إلى تشريك الأشقاء مع الإخوة لأم في الثلث، والنتيجة هنا على قول الحنفية والحنابلة."
                }
            }
        }

        for ((k, v) in fard) if (res[k] == null) res[k] = Share(v, label[k] ?: "فرض")

        var asl = 1L
        if (jaddMode || !hasFard) {
            for (s in res.values) { val sh = s.share; if (sh != null && !sh.isZero()) asl = Fr.lcm(asl, sh.d) }
        } else asl = fardDen
        val total = res.values.fold(Fr.ZERO) { a, s -> s.share?.let { a + it } ?: a }
        var awl: Long? = null
        var radd = false

        if (total > Fr.ONE) {
            awl = (total.value * asl).roundToLong()
            for (s in res.values) s.share = s.share?.let { it / total }
            notes += "زادت الفروض على أصل المسألة فعالت من $asl إلى $awl، ونقص نصيب كل وارث بنسبة العول."
        } else if (total < Fr.ONE && !asabaTaken) {
            val nonSpouse = res.filter { it.value.share != null && it.key != "husband" && it.key != "wife" }.keys
            val remain = Fr.ONE - total
            if (nonSpouse.isNotEmpty()) {
                val base = nonSpouse.fold(Fr.ZERO) { a, k -> a + res.getValue(k).share!! }
                for (k in nonSpouse) {
                    val s = res.getValue(k)
                    s.share = s.share!! + remain * s.share!! / base
                    s.type += " وردّ"
                }
                radd = true
                notes += "لا يوجد عاصب، فرُدّ الباقي على أصحاب الفروض بنسبة فروضهم عدا الزوجين."
            } else if (res[spouseK]?.share != null) {
                res.getValue(spouseK).share = Fr.ONE
                res.getValue(spouseK).type += " وردّ"
                radd = true
                notes += "لم يوجد وارث غير الزوج/الزوجة: الباقي يُرد عليه عند كثير من المعاصرين وفي عدد من قوانين الأحوال الشخصية، والأصل عند المتقدمين أنه لذوي الأرحام ثم لبيت المال."
            }
        }
        if (res.values.none { it.share != null && !it.share!!.isZero() }) {
            notes += "لا يوجد وارث من أصحاب الفروض أو العصبات؛ تنتقل التركة إلى ذوي الأرحام ثم بيت المال، ويُرجع في ذلك للمحكمة المختصة."
        }

        var tashih = 1L
        for ((k, s) in res) {
            val sh = s.share ?: continue
            if (!sh.isZero()) tashih = Fr.lcm(tashih, (sh / (c[k] ?: 1).coerceAtLeast(1)).d)
        }
        if (!jaddMode && !hasFard) asl = tashih
        return InheritanceResult(res, notes, asl, awl, tashih, radd)
    }
}
