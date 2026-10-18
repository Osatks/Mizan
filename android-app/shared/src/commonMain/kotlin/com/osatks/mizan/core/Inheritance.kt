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

data class Heir(val key: String, val max: Int, val only: Char? = null, val group: String)

/** نوع استحقاق الوارث؛ يُترجم إلى نص في طبقة اللغات */
enum class ShareType {
    FARD, FARD_TASIB, FARD_TAKMILA, TASIB, TASIB_BIL_GHAYR, TASIB_MAA_GHAYR, TASIB_MAA_JADD,
    FARD_THEN_MUQASAMA, JADD_MUQASAMA, JADD_THIRD_ESTATE, JADD_THIRD_REST, JADD_SIXTH
}

/**
 * سبب الحجب: مفتاح وارث (مثل "son") أو أحد المفاتيح الخاصة في [Inheritance.BLOCKERS].
 * النص المعروض يأتي من ملف اللغة.
 */
class Share(var share: Fr? = null, var type: ShareType = ShareType.FARD, var blocked: String? = null, var radd: Boolean = false)

/** ملاحظات المسألة؛ تُترجم في طبقة اللغات */
sealed interface InhNote {
    data object MatSiblingsShare : InhNote
    data object Umariyya : InhNote
    data object GrandmothersShare : InhNote
    data object FullSistersGhayr : InhNote
    data object Akdariyya : InhNote
    data class JaddRule(val otherFard: Boolean) : InhNote
    data object Muadda : InhNote
    data class NothingLeft(val winner: String) : InhNote
    data object Mushtaraka : InhNote
    data class Awl(val asl: Long, val awl: Long) : InhNote
    data object Radd : InhNote
    data object SpouseOnlyRadd : InhNote
    data object NoHeirs : InhNote
}

data class InheritanceResult(
    val res: Map<String, Share>,
    val notes: List<InhNote>,
    val asl: Long,
    val awl: Long?,
    val tashih: Long,
    val radd: Boolean
)

/** محرك علم الفرائض على مذهب الجمهور */
object Inheritance {
    val HEIRS = listOf(
        Heir("husband", 1, 'f', "spouses"),
        Heir("wife", 4, 'm', "spouses"),
        Heir("father", 1, null, "ascendants"),
        Heir("mother", 1, null, "ascendants"),
        Heir("gf", 1, null, "ascendants"),
        Heir("gmm", 1, null, "ascendants"),
        Heir("gmf", 1, null, "ascendants"),
        Heir("son", 30, null, "descendants"),
        Heir("daughter", 30, null, "descendants"),
        Heir("gson", 30, null, "descendants"),
        Heir("gdau", 30, null, "descendants"),
        Heir("fb", 30, null, "siblings"),
        Heir("fs", 30, null, "siblings"),
        Heir("pb", 30, null, "siblings"),
        Heir("ps", 30, null, "siblings"),
        Heir("mb", 30, null, "siblings"),
        Heir("ms", 30, null, "siblings"),
        Heir("fnep", 30, null, "distant"),
        Heir("pnep", 30, null, "distant"),
        Heir("fun", 30, null, "distant"),
        Heir("pun", 30, null, "distant"),
        Heir("fcous", 30, null, "distant"),
        Heir("pcous", 30, null, "distant"),
    )
    val GROUPS = listOf("spouses", "ascendants", "descendants", "siblings", "distant")

    /** أسباب حجب ليست اسم وارث بعينه ("gf" هنا: الجد باسمه المختصر) */
    val BLOCKERS = listOf("gf", "twoDaughters", "descendant", "fbAfterMuadda", "fsTookRest", "gfAndSiblings", "twoFullSisters", "fsGhayr")

    private fun f(n: Long, d: Long = 1) = Fr.of(n, d)

    fun solve(input: Map<String, Int>, gender: Char): InheritanceResult? {
        val c = input.toMutableMap()
        if (gender == 'm') c["husband"] = 0 else c["wife"] = 0
        fun cnt(k: String) = c[k] ?: 0
        val res = mutableMapOf<String, Share>()
        val notes = mutableListOf<InhNote>()
        val fard = linkedMapOf<String, Fr>()
        val label = mutableMapOf<String, ShareType>()
        fun block(k: String, by: String) { if (cnt(k) > 0) res[k] = Share(blocked = by) }

        val present = c.filter { it.value > 0 }.keys.toList()
        if (present.isEmpty()) return null

        // الفروع
        val son = cnt("son"); val dau = cnt("daughter")
        var gson = cnt("gson"); var gdau = cnt("gdau")
        if (son > 0) { block("gson", "son"); block("gdau", "son"); gson = 0; gdau = 0 }
        if (son == 0 && dau >= 2 && gdau > 0 && gson == 0) { block("gdau", "twoDaughters"); gdau = 0 }
        val maleDesc = son > 0 || gson > 0
        val femDesc = dau > 0 || gdau > 0
        val desc = maleDesc || femDesc

        // الأصول
        val father = cnt("father"); val mother = cnt("mother")
        var gf = cnt("gf")
        if (father > 0 && gf > 0) { block("gf", "father"); gf = 0 }
        var gmm = cnt("gmm"); var gmf = cnt("gmf")
        if (mother > 0) { block("gmm", "mother"); block("gmf", "mother"); gmm = 0; gmf = 0 }
        if (father > 0 && gmf > 0) { block("gmf", "father"); gmf = 0 }

        val sibAll = cnt("fb") + cnt("fs") + cnt("pb") + cnt("ps") + cnt("mb") + cnt("ms")
        val spouseK = if (gender == 'm') "wife" else "husband"
        val spouse = cnt(spouseK)

        if (spouse > 0) {
            fard[spouseK] = if (gender == 'f') (if (desc) f(1, 4) else f(1, 2)) else (if (desc) f(1, 8) else f(1, 4))
            label[spouseK] = ShareType.FARD
        }

        // الإخوة لأم
        val matN = cnt("mb") + cnt("ms")
        if (matN > 0) {
            val by = when { desc -> "descendant"; father > 0 -> "father"; gf > 0 -> "gf"; else -> null }
            if (by != null) { block("mb", by); block("ms", by) } else {
                val total = if (matN == 1) f(1, 6) else f(1, 3)
                if (cnt("mb") > 0) { fard["mb"] = total * cnt("mb") / matN; label["mb"] = ShareType.FARD }
                if (cnt("ms") > 0) { fard["ms"] = total * cnt("ms") / matN; label["ms"] = ShareType.FARD }
                if (matN > 1) notes += InhNote.MatSiblingsShare
            }
        }

        // الإخوة الأشقاء ولأب
        var fb = cnt("fb"); var fs = cnt("fs"); var pb = cnt("pb"); var ps = cnt("ps")
        val sibBlocker = when { son > 0 -> "son"; gson > 0 -> "gson"; father > 0 -> "father"; else -> null }
        if (sibBlocker != null) { listOf("fb", "fs", "pb", "ps").forEach { block(it, sibBlocker) }; fb = 0; fs = 0; pb = 0; ps = 0 }
        val jaddMode = gf > 0 && (fb + fs + pb + ps) > 0

        // البنات وبنات الابن
        if (dau > 0 && son == 0) { fard["daughter"] = if (dau == 1) f(1, 2) else f(2, 3); label["daughter"] = ShareType.FARD }
        if (gdau > 0 && gson == 0) {
            if (dau == 0) { fard["gdau"] = if (gdau == 1) f(1, 2) else f(2, 3); label["gdau"] = ShareType.FARD }
            else if (dau == 1) { fard["gdau"] = f(1, 6); label["gdau"] = ShareType.FARD_TAKMILA }
        }

        // الأم والجدات
        if (mother > 0) {
            if (desc || sibAll >= 2) fard["mother"] = f(1, 6)
            else if (spouse > 0 && father > 0 && present.all { it in listOf("husband", "wife", "father", "mother") || res[it]?.blocked != null }) {
                fard["mother"] = (Fr.ONE - fard.getValue(spouseK)) / 3
                notes += InhNote.Umariyya
            } else fard["mother"] = f(1, 3)
            label["mother"] = ShareType.FARD
        }
        if (gmm + gmf > 0) {
            val n = gmm + gmf
            if (gmm > 0) { fard["gmm"] = f(1, 6) / n; label["gmm"] = ShareType.FARD }
            if (gmf > 0) { fard["gmf"] = f(1, 6) / n; label["gmf"] = ShareType.FARD }
            if (n > 1) notes += InhNote.GrandmothersShare
        }

        // الأب والجد
        if (father > 0) {
            if (maleDesc) { fard["father"] = f(1, 6); label["father"] = ShareType.FARD }
            else if (femDesc) { fard["father"] = f(1, 6); label["father"] = ShareType.FARD_TASIB }
        }
        if (gf > 0 && !jaddMode) {
            if (maleDesc) { fard["gf"] = f(1, 6); label["gf"] = ShareType.FARD }
            else if (femDesc) { fard["gf"] = f(1, 6); label["gf"] = ShareType.FARD_TASIB }
        }

        // الأخوات (خارج باب الجد)
        var fsGhayr = false; var psGhayr = false
        if (!jaddMode) {
            if (fs > 0 && fb == 0) {
                if (femDesc) { fsGhayr = true; notes += InhNote.FullSistersGhayr }
                else { fard["fs"] = if (fs == 1) f(1, 2) else f(2, 3); label["fs"] = ShareType.FARD }
            }
            if (fb > 0) { block("pb", "fb"); block("ps", "fb"); pb = 0; ps = 0 }
            else if (fsGhayr) { block("pb", "fsGhayr"); block("ps", "fsGhayr"); pb = 0; ps = 0 }
            if (ps > 0 && pb == 0) {
                if (femDesc && fs == 0) psGhayr = true
                else if (fs == 0) { fard["ps"] = if (ps == 1) f(1, 2) else f(2, 3); label["ps"] = ShareType.FARD }
                else if (fs == 1) { fard["ps"] = f(1, 6); label["ps"] = ShareType.FARD_TAKMILA }
                else { block("ps", "twoFullSisters"); ps = 0 }
            }
        }

        var fardSum = fard.values.fold(Fr.ZERO) { a, b -> a + b }

        // الأكدرية
        val onlyKeys = { keys: List<String> -> present.all { it in keys || res[it]?.blocked != null } }
        if (jaddMode && gender == 'f' && spouse > 0 && mother > 0 && (fs + ps) == 1 && fb == 0 && pb == 0 &&
            onlyKeys(listOf("husband", "mother", "gf", "fs", "ps"))
        ) {
            val sk = if (fs > 0) "fs" else "ps"
            res["husband"] = Share(f(9, 27), ShareType.FARD); res["mother"] = Share(f(6, 27), ShareType.FARD)
            res["gf"] = Share(f(8, 27), ShareType.FARD_THEN_MUQASAMA); res[sk] = Share(f(4, 27), ShareType.FARD_THEN_MUQASAMA)
            notes += InhNote.Akdariyya
            return InheritanceResult(res, notes, 6, 9, 27, false)
        }

        // الجد مع الإخوة (المقاسمة عند الجمهور)
        if (jaddMode) {
            val rem = Fr.ONE - fardSum
            val units = 2 + fb * 2 + fs + pb * 2 + ps
            val muq = if (rem > Fr.ZERO) rem * 2 / units else Fr.ZERO
            val otherFard = fardSum > Fr.ZERO
            val gfShare: Fr; val how: ShareType
            if (!otherFard) {
                val third = f(1, 3)
                if (muq >= third) { gfShare = muq; how = ShareType.JADD_MUQASAMA } else { gfShare = third; how = ShareType.JADD_THIRD_ESTATE }
            } else {
                val sixth = f(1, 6)
                val thirdRem = if (rem > Fr.ZERO) rem / 3 else Fr.ZERO
                var best = sixth; var bestHow = ShareType.JADD_SIXTH
                if (thirdRem > best) { best = thirdRem; bestHow = ShareType.JADD_THIRD_REST }
                if (muq > best) { best = muq; bestHow = ShareType.JADD_MUQASAMA }
                gfShare = best; how = bestHow
            }
            res["gf"] = Share(gfShare, how)
            fardSum += gfShare
            var sibRem = Fr.ONE - fardSum
            if (sibRem < Fr.ZERO) sibRem = Fr.ZERO
            notes += InhNote.JaddRule(otherFard)
            if (fb + fs > 0 && pb + ps > 0) notes += InhNote.Muadda

            fun give(bk: String, sk: String, b: Int, s: Int, amount: Fr, type: ShareType) {
                val u = b * 2 + s
                if (u == 0) return
                if (b > 0) res[bk] = Share(amount * (b * 2) / u, type)
                if (s > 0) res[sk] = Share(amount * s / u, type)
            }
            when {
                fb > 0 -> {
                    give("fb", "fs", fb, fs, sibRem, ShareType.TASIB)
                    if (pb > 0) res["pb"] = Share(blocked = "fbAfterMuadda")
                    if (ps > 0) res["ps"] = Share(blocked = "fbAfterMuadda")
                }
                fs > 0 -> if (pb + ps > 0) {
                    val cap = if (fs == 1) f(1, 2) else f(2, 3)
                    val fsGet = if (sibRem > cap) cap else sibRem
                    res["fs"] = Share(fsGet, ShareType.TASIB_MAA_JADD)
                    val left = sibRem - fsGet
                    if (left > Fr.ZERO) give("pb", "ps", pb, ps, left, ShareType.TASIB)
                    else {
                        if (pb > 0) res["pb"] = Share(blocked = "fsTookRest")
                        if (ps > 0) res["ps"] = Share(blocked = "fsTookRest")
                    }
                } else give("fb", "fs", 0, fs, sibRem, ShareType.TASIB_MAA_JADD)
                else -> give("pb", "ps", pb, ps, sibRem, ShareType.TASIB_MAA_JADD)
            }
            listOf("fnep", "pnep", "fun", "pun", "fcous", "pcous").forEach { block(it, "gfAndSiblings") }
        }

        val fardDen = fard.values.fold(1L) { a, x -> Fr.lcm(a, x.d) }
        val hasFard = fard.isNotEmpty()

        // العصبات
        var asabaTaken = jaddMode
        if (!jaddMode) {
            // nm: مفتاح العاصب كما يظهر في سبب الحجب
            data class Rank(val m: String?, val f: String?, val nm: String, val ghayr: Boolean = false)
            val order = listOf(
                Rank("son", "daughter", "son"), Rank("gson", "gdau", "gson"),
                Rank("father", null, "father"), Rank("gf", null, "gf"),
                Rank("fb", "fs", "fb"), Rank(null, "fs", "fs", true),
                Rank("pb", "ps", "pb"), Rank(null, "ps", "ps", true),
                Rank("fnep", null, "fnep"), Rank("pnep", null, "pnep"),
                Rank("fun", null, "fun"), Rank("pun", null, "pun"),
                Rank("fcous", null, "fcous"), Rank("pcous", null, "pcous"),
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
                val tLabel = if (winner.m != null) ShareType.TASIB else ShareType.TASIB_MAA_GHAYR
                if (winner.m != null) {
                    val s = residue * (mC * 2) / u
                    val own = fard[winner.m]
                    res[winner.m] = Share((own ?: Fr.ZERO) + s,
                        if (own != null) label[winner.m] ?: ShareType.FARD_TASIB else if (fC > 0) ShareType.TASIB_BIL_GHAYR else tLabel)
                    fard.remove(winner.m)
                }
                if (fC > 0 && winner.f != null) res[winner.f] = Share(residue * fC / u, if (winner.m != null) ShareType.TASIB_BIL_GHAYR else tLabel)
                if (residue.isZero() && (mC > 0 || fC > 0)) notes += InhNote.NothingLeft(winner.nm)
                if (residue.isZero() && winner.m == "fb" && matN >= 2 && spouse > 0 && (mother > 0 || gmm > 0 || gmf > 0) && !desc && father == 0 && gf == 0) {
                    notes += InhNote.Mushtaraka
                }
            }
        }

        for ((k, v) in fard) if (res[k] == null) res[k] = Share(v, label[k] ?: ShareType.FARD)

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
            notes += InhNote.Awl(asl, awl)
        } else if (total < Fr.ONE && !asabaTaken) {
            val nonSpouse = res.filter { it.value.share != null && it.key != "husband" && it.key != "wife" }.keys
            val remain = Fr.ONE - total
            if (nonSpouse.isNotEmpty()) {
                val base = nonSpouse.fold(Fr.ZERO) { a, k -> a + res.getValue(k).share!! }
                for (k in nonSpouse) {
                    val s = res.getValue(k)
                    s.share = s.share!! + remain * s.share!! / base
                    s.radd = true
                }
                radd = true
                notes += InhNote.Radd
            } else if (res[spouseK]?.share != null) {
                res.getValue(spouseK).share = Fr.ONE
                res.getValue(spouseK).radd = true
                radd = true
                notes += InhNote.SpouseOnlyRadd
            }
        }
        if (res.values.none { it.share != null && !it.share!!.isZero() }) {
            notes += InhNote.NoHeirs
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
