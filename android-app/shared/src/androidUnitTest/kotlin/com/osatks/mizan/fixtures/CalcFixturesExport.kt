package com.osatks.mizan.fixtures

import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.daysBetween
import com.osatks.mizan.core.minusDays
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.core.InheritanceResult
import com.osatks.mizan.core.ZakatKinds
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.screens.currencies
import com.osatks.mizan.ui.screens.symbol
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * يصدّر نتائج حاسبات Kotlin (المواريث، أنواع الزكاة، الفطر والفدية) إلى iosApp/MizanCore/Fixtures/calc (ملفات JSON)
 * لتقارنها CheckCalc بنتائج MizanCalc في Swift. الأعداد العشرية نصوصاً (toString) فتُقرأ بلا فقد، والكسور n/d.
 * حسابات الشاشات (النقدان، والأنعام، والفطر...) منسوخة هنا بنصها من ZakatScreen/NativeZakat.ios/FitrFidyaScreens.
 */
class CalcFixturesExport {
    private val out = File("../../iosApp/MizanCore/Fixtures/calc").apply { mkdirs() }

    // كاتب JSON صغير
    private fun q(s: String) = buildString {
        append('"')
        for (ch in s) when (ch) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n")
            else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
        }
        append('"')
    }
    private fun j(v: Any?): String = when (v) {
        null -> "null"
        is String -> q(v)
        is Boolean -> v.toString()
        is Int, is Long -> v.toString()
        is Double -> q(v.toString())
        is Map<*, *> -> v.entries.joinToString(",", "{", "}") { q(it.key.toString()) + ":" + j(it.value) }
        is List<*> -> v.joinToString(",", "[", "]") { j(it) }
        else -> error("type ${v::class}")
    }
    private fun write(name: String, cases: List<Any?>) {
        File(out, name).writeText(cases.joinToString(",\n", "[\n", "\n]\n") { j(it) })
        println("$name: ${cases.size}")
    }

    private fun note(n: InhNote): String = when (n) {
        is InhNote.JaddRule -> "JaddRule(otherFard=${n.otherFard})"
        is InhNote.NothingLeft -> "NothingLeft(winner=${n.winner})"
        is InhNote.Awl -> "Awl(asl=${n.asl}, awl=${n.awl})"
        else -> n::class.simpleName!!
    }

    private fun res(r: InheritanceResult?): Any? = r?.let {
        linkedMapOf(
            "res" to it.res.map { (k, s) -> listOf(k, s.share?.toString(), s.type.name, s.blocked, s.radd) },
            "notes" to it.notes.map(::note),
            "asl" to it.asl, "awl" to it.awl, "tashih" to it.tashih, "radd" to it.radd,
        )
    }

    private val keys = Inheritance.HEIRS.map { it.key }
    private fun allowed(g: Char) = Inheritance.HEIRS.filter { it.only == null || it.only == g }.map { it.key }

    @Test
    fun inheritance() {
        val inputs = LinkedHashSet<Pair<Char, Map<String, Int>>>()
        fun add(g: Char, m: Map<String, Int>) { inputs += g to LinkedHashMap(m.filterValues { it != 0 }.toSortedMap(compareBy { keys.indexOf(it) })) }
        val genders = listOf('m', 'f')
        // فارغة
        for (g in genders) add(g, emptyMap())
        // وارث واحد بأعداد مختلفة
        for (g in genders) for (k in allowed(g)) for (n in listOf(1, 2, 3, 30)) add(g, mapOf(k to minOf(n, Inheritance.HEIRS.first { it.key == k }.max)))
        // كل زوجين
        for (g in genders) {
            val a = allowed(g)
            for (i in a.indices) for (jx in i + 1 until a.size) for ((x, y) in listOf(1 to 1, 2 to 3)) {
                fun cap(k: String, n: Int) = minOf(n, Inheritance.HEIRS.first { it.key == k }.max)
                add(g, mapOf(a[i] to cap(a[i], x), a[jx] to cap(a[jx], y)))
            }
        }
        // الجد مع الإخوة: جداء كامل
        for (g in genders) for (sp in 0..1) for (mo in 0..1) for (da in 0..2) for (fb in 0..2) for (fs in 0..3) for (pb in 0..1) for (ps in 0..2) for (mb in 0..1) {
            add(g, mapOf((if (g == 'm') "wife" else "husband") to sp, "mother" to mo, "gf" to 1, "daughter" to da, "fb" to fb, "fs" to fs, "pb" to pb, "ps" to ps, "mb" to mb))
        }
        // الإخوة والأخوات والأم والزوج (المشتركة والعمرية والأخوات مع البنات)
        for (g in genders) for (sp in 0..1) for (mo in 0..1) for (fa in 0..1) for (da in 0..2) for (gd in 0..2) for (fs in 0..2) for (ps in 0..2) for (mb in 0..2) for (fb in 0..1) {
            add(g, mapOf((if (g == 'm') "wife" else "husband") to sp, "mother" to mo, "father" to fa, "daughter" to da, "gdau" to gd, "fs" to fs, "ps" to ps, "mb" to mb, "fb" to fb))
        }
        // مسائل مشهورة
        add('f', mapOf("husband" to 1, "mother" to 1, "gf" to 1, "fs" to 1)) // الأكدرية
        add('f', mapOf("husband" to 1, "mother" to 1, "gf" to 1, "ps" to 1))
        add('f', mapOf("husband" to 1, "mother" to 1, "father" to 1)) // العمرية
        add('m', mapOf("wife" to 1, "mother" to 1, "father" to 1))
        add('f', mapOf("husband" to 1, "mother" to 1, "mb" to 2, "fb" to 1)) // المشتركة
        add('f', mapOf("husband" to 1, "gmm" to 1, "ms" to 2, "fb" to 3))
        add('f', mapOf("husband" to 1, "fs" to 2, "mother" to 1, "mb" to 2)) // عول إلى 10
        add('m', mapOf("wife" to 1, "daughter" to 2, "father" to 1, "mother" to 1)) // المنبرية
        add('m', mapOf("wife" to 4, "gmm" to 1, "gmf" to 1, "daughter" to 3, "mb" to 2, "ms" to 5))
        add('m', mapOf("gf" to 1, "fb" to 1, "pb" to 1, "ps" to 2)) // المعادة
        add('m', mapOf("gf" to 1, "fs" to 1, "pb" to 1)) // زيدية
        add('m', mapOf("son" to 30, "daughter" to 30, "wife" to 4, "father" to 1, "mother" to 1))
        add('m', mapOf("husband" to 1, "son" to 1)) // الزوج مع جنس ذكر (يُصفَّر)
        add('f', mapOf("wife" to 2, "daughter" to 1))
        add('m', mapOf("wife" to 1))
        add('f', mapOf("husband" to 1))
        // عشوائية: قليل من الورثة
        val rnd = Random(20261109)
        for (g in genders) repeat(1500) {
            val a = allowed(g)
            val m = mutableMapOf<String, Int>()
            repeat(rnd.nextInt(3, 6)) { val k = a[rnd.nextInt(a.size)]; m[k] = minOf(rnd.nextInt(1, 4), Inheritance.HEIRS.first { it.key == k }.max) }
            add(g, m)
        }
        // عشوائية: كثير من الورثة وأعداد كبيرة
        for (g in genders) repeat(1500) {
            val m = mutableMapOf<String, Int>()
            val p = rnd.nextDouble(0.1, 0.6)
            for (h in Inheritance.HEIRS) if ((h.only == null || h.only == g || rnd.nextInt(20) == 0) && rnd.nextDouble() < p) {
                m[h.key] = if (rnd.nextInt(6) == 0) rnd.nextInt(1, h.max + 1) else minOf(rnd.nextInt(1, 5), h.max)
            }
            add(g, m)
        }
        // بلا أصحاب فروض ولا عصبات قريبة: الحواشي وحدهم
        for (g in genders) for (k1 in listOf("fnep", "pnep", "fun", "pun", "fcous", "pcous")) for (k2 in listOf("mb", "ms", "gmm", "gmf", "fs", "ps")) add(g, mapOf(k1 to 2, k2 to 3))

        write("inheritance.json", inputs.map { (g, m) -> linkedMapOf("g" to g.toString(), "in" to m, "r" to res(Inheritance.solve(m, g))) })

        // التركة: التجهيز والدين والوصية (InheritanceScreen) ومبالغ الورثة
        val vals = listOf(0.0, 0.0, 1.0, 100.0, 999.99, 1000.0, 3000.0, 12345.67, 100000.0, 1e9, 0.1, 0.3, 333.33, 2500.5)
        val estateCases = mutableListOf<Any?>()
        val ins = inputs.toList()
        repeat(600) { i ->
            val (g, m) = ins[rnd.nextInt(ins.size)]
            val estate = if (i % 7 == 0) 3000.0 else vals[rnd.nextInt(vals.size)]
            val funeral = vals[rnd.nextInt(vals.size)].let { if (rnd.nextBoolean()) 0.0 else it / 10 }
            val debts = vals[rnd.nextInt(vals.size)].let { if (rnd.nextBoolean()) 0.0 else it / 3 }
            val will = when (i % 5) { 0 -> 1000.0; 1 -> (estate - funeral - debts) / 3; 2 -> 0.0; else -> vals[rnd.nextInt(vals.size)] }
            val afterDebt = (estate - funeral - debts).coerceAtLeast(0.0)
            val willCapped = will > afterDebt / 3 + 1e-9
            val net = (afterDebt - minOf(will, afterDebt / 3)).coerceAtLeast(0.0)
            val r = Inheritance.solve(m, g)
            val amounts = r?.res?.mapNotNull { (k, s) ->
                val sh = s.share ?: return@mapNotNull null
                if (s.blocked != null) return@mapNotNull null
                val c = m[k] ?: 1
                val amount = net * sh.value
                listOf(k, amount, amount / c, sh.value * 100)
            }
            estateCases += linkedMapOf("g" to g.toString(), "in" to m, "estate" to estate, "funeral" to funeral, "debts" to debts, "will" to will,
                "afterDebt" to afterDebt, "capped" to willCapped, "net" to net, "amounts" to amounts)
        }
        write("estate.json", estateCases)
    }

    @Test
    fun zakatMoney() {
        val rnd = Random(85)
        val pool = listOf(0.0, 0.0, 0.0, 0.0, 1.0, 2.5, 10.0, 33.33, 85.0, 100.0, 595.0, 1000.0, 8500.0, 12345.67, 1e6, 0.1, 0.2, 0.3, 250.0, 7.77)
        val prices = listOf(0.0, 3.0, 0.75, 100.0, 250.5, 300.0, 1.1, 40.0)
        val inputs = mutableListOf<Triple<Map<String, Double>, String, Boolean>>()
        fun f(vararg p: Pair<String, Double>) = mapOf(*p)
        // حدود النصاب
        for (cash in listOf(0.0, 8499.99, 8500.0, 8500.01, 1e-9)) inputs += Triple(f("gold_price" to 100.0, "cash" to cash), "gold", true)
        for (cash in listOf(1784.99, 1785.0, 1785.01)) inputs += Triple(f("silver_price" to 3.0, "cash" to cash), "silver", true)
        inputs += Triple(f("silver_price" to 0.0, "gold_price" to 100.0, "cash" to 9000.0), "silver", true)
        inputs += Triple(f("gold_price" to 0.0, "silver_price" to 3.0, "cash" to 9000.0), "gold", true)
        inputs += Triple(f("gold_price" to 100.0, "cash" to 9000.0, "debts" to 20000.0), "gold", true)
        inputs += Triple(f("gold_price" to 100.0, "g21" to 100.0, "jewel" to 200.0), "gold", false)
        inputs += Triple(f("gold_price" to 100.0, "g21" to 100.0, "jewel" to 50.0), "gold", false)
        inputs += Triple(f("gold_price" to 100.0, "g24" to 85.0), "gold", true)
        inputs += Triple(f("gold_price" to 100.0, "g18" to 113.33333333333333), "gold", true)
        inputs += Triple(emptyMap(), "gold", true)
        repeat(3000) {
            val m = mutableMapOf<String, Double>()
            m["gold_price"] = prices[rnd.nextInt(prices.size)]
            m["silver_price"] = prices[rnd.nextInt(prices.size)]
            for (k in listOf("cash", "bank", "receivable", "other", "g24", "g22", "g21", "g18", "silver", "jewel", "stocks_trade", "stocks_long", "goods", "debts"))
                if (rnd.nextInt(3) == 0) m[k] = if (rnd.nextInt(4) == 0) rnd.nextDouble(0.0, 50000.0) else pool[rnd.nextInt(pool.size)]
            inputs += Triple(m, if (rnd.nextBoolean()) "gold" else "silver", rnd.nextInt(3) != 0)
        }
        val cases = inputs.map { (vals, basis, includeJewelry) ->
            fun n(k: String) = vals[k] ?: 0.0
            // منسوخ من moneyPage (NativeZakat.ios.kt)
            val gp = n("gold_price"); val sp = n("silver_price")
            val pure24 = n("g24") + n("g22") * 22 / 24 + n("g21") * 21 / 24 + n("g18") * 18 / 24
            val excluded = if (includeJewelry) 0.0 else minOf(pure24, n("jewel") * 21 / 24)
            val gold = (pure24 - excluded) * gp
            val parts = listOf(
                "cash" to n("cash") + n("bank") + n("other"),
                "receivables" to n("receivable"),
                "gold" to gold,
                "silver" to n("silver") * sp,
                "stocks" to n("stocks_trade") + n("stocks_long"),
                "trade" to n("goods"),
            )
            val total = parts.sumOf { it.second }
            val debts = minOf(n("debts"), total)
            val net = (total - debts).coerceAtLeast(0.0)
            val nisab = if (basis == "silver") 595 * sp else 85 * gp
            val due = nisab > 0 && net >= nisab && net > 0
            val zakat = if (due) net * 0.025 else 0.0
            val badge = when {
                nisab <= 0 -> if (basis == "silver") "enterSilverPrice" else "enterGoldPrice"
                due -> "nisabReached"
                net > 0 -> "belowNisab"
                else -> "enterAssets"
            }
            val ok = if (nisab <= 0 || net <= 0) null else due
            val progress = if (nisab > 0) (net / nisab).takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) else null
            val grams = if (zakat > 0 && gp > 0) zakat / gp else null
            val breakdown = if (zakat > 0 && parts.count { it.second > 0 } > 1) {
                val ratio = if (total > 0) net / total else 0.0
                parts.filter { it.second > 0 }.map { (name, value) -> listOf(name, value * ratio * 0.025) }
            } else emptyList()
            linkedMapOf(
                "in" to vals, "basis" to basis, "jewelry" to includeJewelry,
                "pure24" to pure24, "excluded" to excluded, "parts" to parts.map { listOf(it.first, it.second) },
                "total" to total, "debts" to debts, "net" to net, "nisab" to nisab, "due" to due, "zakat" to zakat,
                "badge" to badge, "ok" to ok, "progress" to progress, "grams" to grams, "breakdown" to breakdown,
            )
        }
        write("zakat_money.json", cases)

        // العملات لكل لغة مدمجة: القائمة والرمز المعروض، والعملة الافتراضية والمحفوظة (رمز ISO أو الرمز العربي القديم)
        val defaultCurrency = Class.forName("com.osatks.mizan.ui.screens.ZakatScreenKt")
            .getDeclaredMethod("defaultCurrency", Lang::class.java).apply { isAccessible = true }
        val cur = Lang.BUILT_IN.map { lang ->
            val list = currencies(lang)
            val def = defaultCurrency.invoke(null, lang) as String
            val raws = list.map { it.iso } + list.map { it.arSymbol } + listOf("", "XXX", "usd")
            linkedMapOf(
                "lang" to lang.code, "default" to def,
                "list" to list.map { listOf(it.iso, it.arSymbol, it.arName, it.symbol(lang)) },
                "saved" to raws.map { raw -> listOf(raw, list.firstOrNull { it.iso == raw || it.arSymbol == raw }?.iso ?: def) },
            )
        }
        write("currencies.json", cur)
    }

    @Test
    fun zakatKinds() {
        // الأنعام: كل عدد حتى 1000 لكل صنف، وأعداد كبيرة، والشروط
        val counts = (0..1000).toList() + listOf(1199, 1200, 1201, 9999, 10000, 12345, 99999, 123456, 1000000)
        val dues = counts.map { n ->
            fun d(l: List<ZakatKinds.Due>) = l.map { listOf(it.kind, it.count) }
            linkedMapOf("n" to n, "camels" to d(ZakatKinds.camels(n)), "cattle" to d(ZakatKinds.cattle(n)), "sheep" to d(ZakatKinds.sheep(n)))
        }
        write("livestock_dues.json", dues)

        val rnd = Random(40)
        val lsCases = mutableListOf<Any?>()
        val picks = listOf(0, 0, 0, 4, 5, 24, 25, 29, 30, 39, 40, 59, 60, 120, 121, 200, 201, 399, 400, 1000)
        repeat(1500) {
            val texts = List(3) { if (rnd.nextInt(5) == 0) rnd.nextDouble(0.0, 600.0) else picks[rnd.nextInt(picks.size)].toDouble() }
            val grazing = rnd.nextInt(5) != 0; val year = rnd.nextInt(5) != 0
            // منسوخ من livestockPage (NativeZakat.ios.kt)؛ القيمة بعد Fmt.parse
            fun count(v: Double) = v.toInt().coerceAtLeast(0)
            val ns = texts.map(::count)
            val rows = listOf(ZakatKinds.camels(ns[0]), ZakatKinds.cattle(ns[1]), ZakatKinds.sheep(ns[2]))
            val ok = grazing && year
            val total = if (ok) rows.flatten().sumOf { it.count } else 0
            val badge = if (!ok) "conditionsNotMet" else if (total > 0) "nisabReached" else "nothingDue"
            val resOk = if (ns.all { it == 0 }) null else total > 0
            val status = ns.indices.filter { ns[it] > 0 }.map { i ->
                listOf(i, when { !ok -> "nothingDue"; rows[i].isEmpty() -> "belowNisab"; else -> "dues" })
            }
            lsCases += linkedMapOf("in" to texts, "grazing" to grazing, "year" to year, "n" to ns, "total" to total, "badge" to badge, "ok" to resOk, "status" to status)
        }
        write("livestock.json", lsCases)

        val toInt = listOf(0.0, 0.4, 0.9999, 1.0, 39.99, 40.0, 1e9, 2.147483647E9, 3e9, 1e20, Double.NaN, Double.POSITIVE_INFINITY, -5.5)
        write("to_int.json", toInt.map { listOf(it, it.toInt().coerceAtLeast(0)) })

        // الزروع
        val kgs = listOf(0.0, 1.0, 611.0, 611.9999, 612.0, 612.0001, 653.0, 1000.0, 12345.678, 1e7, 0.5)
        val irrs = listOf("rain", "irrigated", "mixed", "", "other")
        val cropPrices = listOf(0.0, 1.5, 2.0, 10.0)
        val crops = mutableListOf<Any?>()
        for (kg in kgs) for (irr in irrs) for (price in cropPrices) {
            val due = ZakatKinds.crops(kg, irr)
            val badge = when { kg <= 0 -> "enterWeight"; due > 0 -> "nisabReached"; else -> "belowNisab" }
            crops += linkedMapOf("kg" to kg, "irr" to irr, "price" to price, "due" to due, "rate" to ZakatKinds.cropsRate(irr) * 100,
                "badge" to badge, "ok" to (if (kg <= 0) null else due > 0), "value" to (if (due > 0 && price > 0) due * price else null))
        }
        write("crops.json", crops)

        // المعادن والركاز
        val mins = mutableListOf<Any?>()
        val goldP = listOf(0.0, 100.0, 250.5, 3.0)
        val mv = listOf(0.0, 254.99, 255.0, 8499.99, 8500.0, 8500.01, 21292.5, 1e6, 0.1)
        for (gold in goldP) for (r in mv) for (m in mv) {
            val rikaz = ZakatKinds.rikaz(r)
            val mineral = ZakatKinds.mineral(m, 85 * gold)
            mins += linkedMapOf("gold" to gold, "rikazValue" to r, "mineralValue" to m, "rikaz" to rikaz, "mineral" to mineral,
                "total" to rikaz + mineral, "nisab" to (if (gold > 0) 85 * gold else null))
        }
        write("minerals.json", mins)
    }

    @Test
    fun fitrFidya() {
        // منسوخ من FitrScreen: الطعام وأوزانه
        val foods = listOf("rice" to 2.5, "riceSafe" to 3.0, "wheat" to 2.04, "dates" to 2.5, "barley" to 2.2, "other" to -1.0)
        val fitr = mutableListOf<Any?>()
        for (people in listOf(1, 2, 3, 5, 7, 10, 13, 50, 99)) for ((key, fkg) in foods) for (custom in listOf(2.5, 0.0, 3.25, 2.176))
            for (kgPrice in listOf(0.0, 1.0, 7.35)) for (mode in listOf("food", "money")) {
                val value = if (mode == "money") kgPrice * 3 else 0.0
                val sa = if (fkg < 0) custom else fkg
                val kg = sa * people
                fitr += linkedMapOf("people" to people, "mode" to mode, "food" to key, "custom" to custom, "kgPrice" to kgPrice, "value" to value,
                    "sa" to sa, "kg" to kg, "cost" to (if (kgPrice > 0) kg * kgPrice else null), "cash" to (if (value > 0) value * people else null))
            }
        write("fitr.json", fitr)

        val fidya = mutableListOf<Any?>()
        for (tab in listOf("fidya", "yamin", "jima")) for (days in listOf(1, 2, 7, 29, 30, 60, 123, 360)) for (oaths in listOf(1, 2, 3, 10, 100))
            for (measure in listOf(0.75, 1.5)) for (price in listOf(0.0, 1.0, 3.3, 12.75)) {
                val poor = when (tab) { "fidya" -> days; "yamin" -> 10 * oaths; else -> 60 }
                val kg = measure * poor
                fidya += linkedMapOf("tab" to tab, "days" to days, "oaths" to oaths, "measure" to measure, "price" to price,
                    "poor" to poor, "kg" to kg, "fast" to (if (tab == "yamin") 3 * oaths else null), "valueOut" to (if (price > 0) price * kg else null),
                    "measureKey" to (if (measure == 0.75) "mudd" else "half"))
            }
        write("fidya.json", fidya)

        // كفارة الجماع: نهاية الشهرين الهجريين (y, m, min(d, طول الشهر))
        val months = mutableListOf<Any?>()
        for (y in 1440..1450) for (m in 1..12) for (d in listOf(1, 15, 29, 30)) {
            var yy = y; var mm = m + 2
            if (mm > 12) { mm -= 12; yy++ }
            val ml = Hijri.monthLength(yy, mm)
            months += linkedMapOf("y" to y, "m" to m, "d" to d, "ml" to ml, "out" to listOf(yy, mm, minOf(d, ml)))
        }
        write("jima_months.json", months)

        // منسوخ من FidyaScreen (jima): من يوم البداية إلى آخر الشهرين الهجريين
        val period = mutableListOf<Any?>()
        var start = LocalDate(2019, 1, 3)
        while (start < LocalDate(2032, 1, 1)) {
            val h = Hijri.toHijri(start)
            var endS: String? = null; var days: Long? = null
            if (h != null) {
                var y = h.y; var m = h.m + 2
                if (m > 12) { m -= 12; y++ }
                val end = Hijri.toGregorian(y, m, minOf(h.d, Hijri.monthLength(y, m)))?.minusDays(1)
                if (end != null) { endS = end.toString(); days = daysBetween(start, end) + 1 }
            }
            period += linkedMapOf("start" to start.toString(), "end" to endS, "days" to days)
            start = start.plus(5, DateTimeUnit.DAY)
        }
        write("jima_period.json", period)
    }
}
