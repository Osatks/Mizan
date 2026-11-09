package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Demo
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.ZakatKinds
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Proof
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.SplitMax
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر الزكاة من النظام (iosApp/iosApp/NativeZakat.swift)، على مثال [NativeAdhkar]: الأنواع، وصفحة النوع المفتوح كتلاً
 * (بطاقة النتيجة، وبطاقات فيها حقول وصفوف ومفاتيح واختيارات، وملاحظات، وأدلة) JSON مع الإشعار [STATE].
 * Swift يرسم ويرسل كل تعديل نصاً كما كُتب، وهنا يُحلَّل (بالأرقام العربية والفارسية كما في Compose) ويُحفظ ويُحسب ويُنسَّق.
 */
object NativeZakat {
    const val STATE = "MizanZakatState"

    private var factory: ((NativeZakatSession) -> UIViewController)? = null

    fun attach(make: (NativeZakatSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeZakatSession): UIViewController = factory!!.invoke(session)
}

/** حقول الأنعام والزروع والمعادن المحفوظة بمفاتيحها (حقول النقدين في [ZAKAT_FIELDS] بالبادئة zk_) */
private val KIND_FIELDS = listOf("zk_camels", "zk_cattle", "zk_sheep", "zk_crop_kg", "zk_crop_price", "zk_rikaz", "zk_mineral")
private val OPTIONS = listOf("zk_irr", "zk_basis", "zk_cur")
private val FLAGS = listOf("zk_grazing", "zk_lyear", "zk_jewelry")

class NativeZakatSession internal constructor() {
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onBack: () -> Unit = {}
    internal var onShare: () -> Unit = {}

    /** نصوص الحقول والاختيارات بمفاتيح حفظها، والمفاتيح (نعم/لا) */
    internal val text = mutableStateMapOf<String, String>()
    internal val flags = mutableStateMapOf<String, Boolean>()

    /** القيم المحفوظة كما تقرؤها شاشات Compose (والبيانات التجريبية للنقدين) */
    internal fun load(lang: Lang) {
        ZAKAT_FIELDS.forEach { text["zk_$it"] = if (Demo.enabled) Demo.zakat[it] ?: "" else AppSettings.getString("zk_$it") }
        KIND_FIELDS.forEach { text[it] = AppSettings.getString(it) }
        text["zk_irr"] = AppSettings.getString("zk_irr", "rain")
        text["zk_basis"] = AppSettings.getString("zk_basis", "gold")
        text["zk_cur"] = savedCurrency(lang)
        flags["zk_grazing"] = AppSettings.getBool("zk_grazing", true)
        flags["zk_lyear"] = AppSettings.getBool("zk_lyear", true)
        flags["zk_jewelry"] = AppSettings.getBool("zk_jewelry", true)
    }

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeZakat.STATE, this)
    }

    /** يفتح النوع [kind]، و"" يعود إلى الأنواع */
    fun open(kind: String) = onOpen(kind)
    fun back() = onBack()

    /** يشارك نتيجة زكاة النقدين */
    fun share() = onShare()

    /** حقل عددي: النص كما كتبه المستخدم يُحفظ، والحساب يعاد من النص */
    fun edit(key: String, value: String) {
        if (key !in text || key in OPTIONS) return
        text[key] = value
        AppSettings.putString(key, value)
    }

    fun toggle(key: String, on: Boolean) {
        if (key !in FLAGS) return
        flags[key] = on
        AppSettings.putBool(key, on)
    }

    /** اختيار من قائمة أو أزرار متجاورة: العملة، وأساس النصاب، وطريقة السقي */
    fun choose(key: String, value: String) {
        if (key !in OPTIONS) return
        text[key] = value
        AppSettings.putString(key, value)
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

// كتل الصفحة وصفوفها (يرسمها Swift بحسب "t")
private typealias Block = Map<String, Any?>

private fun zkField(key: String, label: String, value: String, suffix: String? = null, hint: String? = null): Block =
    linkedMapOf("t" to "field", "id" to key, "key" to key, "label" to label, "value" to value, "suffix" to suffix, "hint" to hint)

private fun zkStat(id: String, label: String, value: String, strong: Boolean = false): Block =
    linkedMapOf("t" to "stat", "id" to id, "label" to label, "value" to value, "strong" to strong)

private fun zkCheck(key: String, label: String, on: Boolean): Block =
    linkedMapOf("t" to "check", "id" to key, "key" to key, "label" to label, "on" to on)

private fun zkChoice(t: String, key: String, label: String, options: List<Pair<String, String>>, selected: String): Block =
    linkedMapOf(
        "t" to t, "id" to key, "key" to key, "label" to label, "selected" to selected,
        "value" to (options.firstOrNull { it.first == selected }?.second ?: selected),
        "options" to options.map { (k, l) -> linkedMapOf("key" to k, "label" to l) },
    )

private fun zkButton(id: String, label: String): Block = linkedMapOf("t" to "button", "id" to id, "label" to label)

private fun zkCard(id: String, title: String?, rows: List<Block?>, side: Boolean = false): Block =
    linkedMapOf("t" to "card", "id" to id, "side" to side, "title" to title, "rows" to rows.filterNotNull())

private fun zkResult(label: String, value: String, badge: String?, ok: Boolean?, unit: String? = null, progress: Double? = null): Block =
    linkedMapOf(
        "t" to "result", "id" to "result", "side" to true, "label" to label, "value" to value,
        "badge" to badge, "ok" to ok, "unit" to unit, "progress" to progress,
    )

private fun zkNote(id: String, text: String): Block = linkedMapOf("t" to "note", "id" to id, "side" to false, "text" to text)

/** صفحة النوع: العنوان والوصف، وكتلها بترتيب Compose، ونص المشاركة إن وُجدت نتيجة */
private class ZakatPage(val title: String, val subtitle: String, val blocks: List<Block>, val share: String? = null)

/** زكاة النقدين وعروض التجارة معاً في نصاب واحد (ZakatMoneyScreen) */
private fun moneyPage(z: NativeZakatSession, s: Strings, lang: Lang, tradeFirst: Boolean, ev: (String, List<Proof>, Boolean) -> Block): ZakatPage {
    val list = currencies(lang)
    val curObj = list.firstOrNull { it.iso == z.text["zk_cur"] } ?: list.first()
    val cur = curObj.symbol(lang)
    val basis = z.text["zk_basis"] ?: "gold"
    val includeJewelry = z.flags["zk_jewelry"] ?: true
    fun t(k: String) = z.text["zk_$k"] ?: ""
    fun n(k: String) = Fmt.parse(t(k))

    val gp = n("gold_price"); val sp = n("silver_price")
    val pure24 = n("g24") + n("g22") * 22 / 24 + n("g21") * 21 / 24 + n("g18") * 18 / 24
    val excluded = if (includeJewelry) 0.0 else minOf(pure24, n("jewel") * 21 / 24)
    val gold = (pure24 - excluded) * gp
    val parts = listOf(
        s.partCash to n("cash") + n("bank") + n("other"),
        s.partReceivables to n("receivable"),
        s.partGold to gold,
        s.partSilver to n("silver") * sp,
        s.partStocks to n("stocks_trade") + n("stocks_long"),
        s.partTrade to n("goods"),
    )
    val total = parts.sumOf { it.second }
    val debts = minOf(n("debts"), total)
    val net = (total - debts).coerceAtLeast(0.0)
    val nisab = if (basis == "silver") 595 * sp else 85 * gp
    val due = nisab > 0 && net >= nisab && net > 0
    val zakat = if (due) net * 0.025 else 0.0
    val badge = when {
        nisab <= 0 -> if (basis == "silver") s.enterSilverPrice else s.enterGoldPrice
        due -> s.nisabReached
        net > 0 -> s.belowNisab
        else -> s.enterAssets
    }
    val invest = zkCard("invest", s.investSection, listOf(
        zkField("zk_stocks_trade", s.stocksTrade, t("stocks_trade"), cur),
        zkField("zk_stocks_long", s.stocksLong, t("stocks_long"), cur),
        zkField("zk_goods", s.tradeGoods, t("goods"), cur),
    ))
    val blocks = buildList<Block> {
        add(zkResult(s.zakatDue, Fmt.num(zakat), badge, if (nisab <= 0 || net <= 0) null else due, cur,
            if (nisab > 0) (net / nisab).takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) else null))
        add(zkCard("summary", null, listOf(
            zkStat("total", s.totalAssets, "${Fmt.num(total)} $cur"),
            zkStat("debts", s.deductedDebts, "${Fmt.num(debts)} $cur"),
            zkStat("net", s.netBase, "${Fmt.num(net)} $cur", strong = true),
            zkStat("nisab", s.nisabValue, if (nisab > 0) "${Fmt.num(nisab)} $cur" else "—"),
            if (zakat > 0 && gp > 0) zkStat("approx", s.approxEquals, s.goldGrams24(Fmt.num(zakat / gp))) else null,
            if (zakat > 0) zkButton("share", s.shareResult) else null,
        ), side = true))
        if (tradeFirst) add(invest)
        add(zkCard("prices", s.settingsAndPrices, listOf(
            zkChoice("picker", "zk_cur", s.currency, list.map { it.iso to "${it.name(lang)} (${it.symbol(lang)})" }, curObj.iso),
            zkChoice("seg", "zk_basis", s.nisabBasis, listOf("gold" to s.nisabGold, "silver" to s.nisabSilver), basis),
            zkField("zk_gold_price", s.goldPrice24, t("gold_price"), cur, s.goldPriceHint),
            zkField("zk_silver_price", s.silverPrice, t("silver_price"), cur),
        )))
        add(zkCard("cash", s.cashSection, listOf(
            zkField("zk_cash", s.cashInHand, t("cash"), cur),
            zkField("zk_bank", s.bankBalances, t("bank"), cur),
            zkField("zk_receivable", s.receivablesField, t("receivable"), cur),
            zkField("zk_other", s.otherMoney, t("other"), cur),
        )))
        add(zkCard("gold", s.goldSilverSection, listOf(
            zkField("zk_g24", s.goldKarat(24), t("g24"), s.unitGram),
            zkField("zk_g22", s.goldKarat(22), t("g22"), s.unitGram),
            zkField("zk_g21", s.goldKarat(21), t("g21"), s.unitGram),
            zkField("zk_g18", s.goldKarat(18), t("g18"), s.unitGram),
            zkField("zk_silver", s.pureSilver, t("silver"), s.unitGram),
            zkCheck("zk_jewelry", s.includeJewelry, includeJewelry),
            if (!includeJewelry) zkField("zk_jewel", s.jewelryField, t("jewel"), s.unitGram, s.jewelryHint) else null,
        )))
        if (!tradeFirst) add(invest)
        add(zkCard("deduct", s.deductSection, listOf(zkField("zk_debts", s.debtsField, t("debts"), cur, s.debtsHint))))
        if (zakat > 0 && parts.count { it.second > 0 } > 1) {
            val ratio = if (total > 0) net / total else 0.0
            add(zkCard("breakdown", s.breakdownTitle, parts.filter { it.second > 0 }.map { (name, value) ->
                zkStat(name, name, "${Fmt.num(value * ratio * 0.025)} $cur")
            }, side = true))
        }
        add(zkNote("zakatNote", s.zakatNote))
        add(zkNote("combinedNote", s.zkCombinedNote))
        add(ev("ev1", if (tradeFirst) Evidence.zakatTrade else Evidence.zakatMoney, true))
        add(ev("ev2", if (tradeFirst) Evidence.zakatMoney else Evidence.zakatTrade, false))
    }
    val share = if (zakat > 0) s.zakatShareText("${Fmt.num(zakat)} $cur", "${Fmt.num(net)} $cur") else null
    return ZakatPage(if (tradeFirst) s.zkTradeName else s.zkMoneyName, s.zakatSubtitle, blocks, share)
}

/** بهيمة الأنعام (LivestockZakat) */
private fun livestockPage(z: NativeZakatSession, s: Strings, ev: (String, List<Proof>, Boolean) -> Block): ZakatPage {
    fun count(k: String) = Fmt.parse(z.text[k] ?: "").toInt().coerceAtLeast(0)
    val grazing = z.flags["zk_grazing"] ?: true
    val year = z.flags["zk_lyear"] ?: true
    class Kind(val name: String, val n: Int, val dues: List<ZakatKinds.Due>, val nisab: Int)
    val rows = listOf(
        Kind(s.camels, count("zk_camels"), ZakatKinds.camels(count("zk_camels")), ZakatKinds.CAMEL_NISAB),
        Kind(s.cattle, count("zk_cattle"), ZakatKinds.cattle(count("zk_cattle")), ZakatKinds.CATTLE_NISAB),
        Kind(s.sheepGoats, count("zk_sheep"), ZakatKinds.sheep(count("zk_sheep")), ZakatKinds.SHEEP_NISAB),
    )
    val ok = grazing && year
    val total = if (ok) rows.flatMap { it.dues }.sumOf { it.count } else 0
    val blocks = buildList<Block> {
        add(zkResult(
            s.livestockDue, if (total > 0) "$total" else "—",
            if (!ok) s.conditionsNotMet else if (total > 0) s.nisabReached else s.nothingDue,
            if (rows.all { it.n == 0 }) null else total > 0,
            if (total > 0) s.unitHead else null,
        ))
        add(zkCard("inputs", null, listOf(
            zkField("zk_camels", s.camels, z.text["zk_camels"] ?: "", s.unitHead, s.livestockNisab(ZakatKinds.CAMEL_NISAB)),
            zkField("zk_cattle", s.cattle, z.text["zk_cattle"] ?: "", s.unitHead, s.livestockNisab(ZakatKinds.CATTLE_NISAB)),
            zkField("zk_sheep", s.sheepGoats, z.text["zk_sheep"] ?: "", s.unitHead, s.livestockNisab(ZakatKinds.SHEEP_NISAB)),
            zkCheck("zk_grazing", s.grazingCondition, grazing),
            zkCheck("zk_lyear", s.yearCondition, year),
        )))
        // ما يجب في كل صنف
        rows.filter { it.n > 0 }.forEach { r ->
            add(zkCard("kind-${r.name}", "${r.name} (${r.n})", when {
                !ok -> listOf(zkStat("rate", s.zkRate, s.nothingDue))
                r.dues.isEmpty() -> listOf(zkStat("below", s.livestockNisab(r.nisab), s.belowNisab))
                else -> r.dues.map { d -> zkStat(d.kind, s.livestockKind(d.kind), "× ${d.count}", strong = true) }
            }, side = true))
        }
        add(zkNote("conditions", s.conditionsNotMet))
        add(ev("ev1", Evidence.zakatLivestock, true))
    }
    return ZakatPage(s.zkLivestockName, s.zkLivestockDesc, blocks)
}

/** الزروع والثمار (CropsZakat) */
private fun cropsPage(z: NativeZakatSession, s: Strings, ev: (String, List<Proof>, Boolean) -> Block): ZakatPage {
    val irr = z.text["zk_irr"] ?: "rain"
    val kg = Fmt.parse(z.text["zk_crop_kg"] ?: "")
    val due = ZakatKinds.crops(kg, irr)
    val price = Fmt.parse(z.text["zk_crop_price"] ?: "")
    val nisab = ZakatKinds.CROPS_NISAB_KG
    val blocks = listOf(
        zkResult(
            s.cropsDue, if (due > 0) Fmt.num(due) else "—",
            when { kg <= 0 -> s.cropsNisab(Fmt.num(nisab, 0)); due > 0 -> s.nisabReached; else -> s.belowNisab },
            if (kg <= 0) null else due > 0, s.unitKg,
        ),
        zkCard("inputs", null, listOf(
            zkField("zk_crop_kg", s.cropsWeight, z.text["zk_crop_kg"] ?: "", s.unitKg, s.cropsNisab(Fmt.num(nisab, 0))),
            zkChoice("seg", "zk_irr", s.irrigation, listOf("rain" to s.irrRain, "irrigated" to s.irrIrrigated, "mixed" to s.irrMixed), irr),
            zkStat("rate", s.zkRate, "${Fmt.num(ZakatKinds.cropsRate(irr) * 100, 1)}%"),
            zkField("zk_crop_price", s.kgPrice, z.text["zk_crop_price"] ?: "", null, s.kgPriceHint),
            if (due > 0 && price > 0) zkStat("value", s.approxValue, Fmt.num(due * price), strong = true) else null,
        )),
        ev("ev1", Evidence.zakatCrops, true),
    )
    return ZakatPage(s.zkCropsName, s.zkCropsDesc, blocks)
}

/** المعادن والركاز (MineralsZakat) */
private fun mineralsPage(z: NativeZakatSession, s: Strings, ev: (String, List<Proof>, Boolean) -> Block): ZakatPage {
    val gold = Fmt.parse(z.text["zk_gold_price"] ?: "")
    val rikaz = ZakatKinds.rikaz(Fmt.parse(z.text["zk_rikaz"] ?: ""))
    val mineral = ZakatKinds.mineral(Fmt.parse(z.text["zk_mineral"] ?: ""), 85 * gold)
    val total = rikaz + mineral
    val blocks = listOf(
        zkResult(s.mineralsDue, if (total > 0) Fmt.num(total) else "—", null, null),
        zkCard("rikaz", null, listOf(
            zkField("zk_rikaz", s.rikazValue, z.text["zk_rikaz"] ?: ""),
            zkStat("rikazRate", "${s.zkRate} · ${s.rikazValue}", "20%"),
            if (rikaz > 0) zkStat("rikazDue", s.zakatDue, Fmt.num(rikaz), strong = true) else null,
        )),
        zkCard("mineral", null, listOf(
            zkField("zk_mineral", s.mineralValue, z.text["zk_mineral"] ?: ""),
            zkField("zk_gold_price", s.goldPrice24, z.text["zk_gold_price"] ?: "", null, s.goldPriceHint),
            zkStat("nisab", s.nisabValue, if (gold > 0) Fmt.num(85 * gold) else s.enterGoldPrice),
            zkStat("rate", s.zkRate, "2.5%"),
            if (mineral > 0) zkStat("mineralDue", s.zakatDue, Fmt.num(mineral), strong = true) else null,
        )),
        ev("ev1", Evidence.zakatMinerals, true),
    )
    return ZakatPage(s.zkMineralsName, s.zkMineralsDesc, blocks)
}

/**
 * الزكاة بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeZakat.swift) بطراز الرئيسية: أنواعها الخمسة، ثم حاسبة النوع [open]
 * (النقدان، عروض التجارة، الأنعام، الزروع، المعادن والركاز) بمدخلاتها ونتيجتها وأدلتها — كما في [ZakatScreen].
 * الحساب والحفظ والنصوص كلها في Kotlin. [onOpen] يفتح نوعاً ("" يعود إلى الأنواع)، و[onBack] يغلق الزكاة.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeZakatView(open: String, onOpen: (String) -> Unit, onBack: () -> Unit, modifier: Modifier) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val session = remember { NativeZakatSession().apply { load(lang) } }
    val openS by rememberUpdatedState(onOpen)
    val backS by rememberUpdatedState(onBack)
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    // نص الآيات وترجمة معانيها للأدلة
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }
    val ev: (String, List<Proof>, Boolean) -> Block = { id, points, startOpen ->
        linkedMapOf(
            "t" to "evidence", "id" to id, "side" to false, "open" to startOpen,
            "title" to s.evidenceTitle, "sub" to s.evidenceSub,
            "kinds" to points.map { it.kind.name.lowercase() }.distinct(),
            "points" to points.map { p ->
                linkedMapOf(
                    "kind" to p.kind.name.lowercase(), "concept" to p.concept,
                    "title" to p.title[lang], "body" to p.body[lang], "source" to p.source[lang],
                    "ayah" to p.ayah?.let { (sn, from, to) ->
                        quran?.let { q -> "﴿" + (from..to).joinToString(" ") { a -> QuranData.withoutBismillah(q, QuranMeta.index(sn, a)) + " (" + arabicDigits(a) + ")" } + "﴾" }
                    },
                    "tr" to p.ayah?.let { (sn, from, to) -> trText?.let { tr -> (from..to).joinToString(" ") { a -> tr[QuranMeta.index(sn, a)] } } },
                    "hadith" to p.hadith,
                )
            },
        )
    }
    val page = when (open) {
        "money" -> moneyPage(session, s, lang, false, ev)
        "trade" -> moneyPage(session, s, lang, true, ev)
        "livestock" -> livestockPage(session, s, ev)
        "crops" -> cropsPage(session, s, ev)
        "minerals" -> mineralsPage(session, s, ev)
        else -> null
    }
    val shareMsg = page?.share
    val shareNow by rememberUpdatedState { if (shareMsg != null) shareText(s, shareMsg) }
    DisposableEffect(session) {
        session.onOpen = { openS(it) }
        session.onBack = { backS() }
        session.onShare = { shareNow() }
        onDispose { session.onOpen = {}; session.onBack = {}; session.onShare = {} }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // على الشاشة الواسعة: المدخلات في عمود والنتيجة بجانبها (كما في ToolPage)
        val wide = page != null && maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val kinds = listOf(
            "money" to (s.zkMoneyName to s.zkMoneyDesc),
            "trade" to (s.zkTradeName to s.zkTradeDesc),
            "livestock" to (s.zkLivestockName to s.zkLivestockDesc),
            "crops" to (s.zkCropsName to s.zkCropsDesc),
            "minerals" to (s.zkMineralsName to s.zkMineralsDesc),
        )
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark, "wide" to wide,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "back" to s.back, "close" to s.close,
                "title" to s.zakatTitle, "subtitle" to s.zakatTypesSub, "heading" to s.zakatTypesTitle, "note" to s.zkCombinedNote,
                "kinds" to kinds.mapIndexed { i, (k, t) -> linkedMapOf("key" to k, "n" to "${i + 1}", "name" to t.first, "desc" to t.second) },
                "kindLabels" to linkedMapOf(
                    "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                ),
                "page" to page?.let { pg -> linkedMapOf("kind" to open, "title" to pg.title, "subtitle" to pg.subtitle, "blocks" to pg.blocks) },
                "x" to linkedMapOf(
                    "surfaceVariant" to hex(cs.surfaceVariant), "outline" to hex(cs.outline),
                    "quran" to hex(GoldDeep), "hadith" to hex(cs.primary), "consensus" to hex(cs.tertiary), "scholars" to hex(cs.secondary),
                ),
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeZakat.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
