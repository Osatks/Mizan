package com.osatks.mizan.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ShoppingCart
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Demo
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.FormLabel
import com.osatks.mizan.ui.FormPicker
import com.osatks.mizan.ui.FormShareButton
import com.osatks.mizan.ui.FormSwitchRow
import com.osatks.mizan.ui.Reveal
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.Gold

/** زكاة النقدين وعروض التجارة: تُضم في نصاب واحد فتُحسب معاً؛ tradeFirst يقدّم قسم التجارة والأسهم */
@Composable
internal fun ZakatMoneyScreen(onBack: () -> Unit, tradeFirst: Boolean = false) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val v = remember { mutableStateMapOf<String, String>().apply { ZAKAT_FIELDS.forEach { put(it, if (Demo.enabled) Demo.zakat[it] ?: "" else AppSettings.getString("zk_$it")) } } }
    var curIso by remember { mutableStateOf(savedCurrency(lang)) }
    var basis by remember { mutableStateOf(AppSettings.getString("zk_basis", "gold")) }
    var includeJewelry by remember { mutableStateOf(AppSettings.getBool("zk_jewelry", true)) }
    val curObj = currencies(lang).let { l -> l.firstOrNull { it.iso == curIso } ?: l.first() }
    val cur = curObj.symbol(lang)

    fun set(k: String, value: String) { v[k] = value; AppSettings.putString("zk_$k", value) }
    fun n(k: String) = Fmt.parse(v[k] ?: "")

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
    val toNisab by animateFloatAsState(if (nisab > 0) (net / nisab).toFloat().coerceIn(0f, 1f) else 0f, spring(dampingRatio = 0.85f, stiffness = 120f), label = "nisab")

    val invest: @Composable () -> Unit = {
        SectionCard(s.investSection) {
            NumField(s.stocksTrade, v["stocks_trade"] ?: "", { set("stocks_trade", it) }, cur)
            NumField(s.stocksLong, v["stocks_long"] ?: "", { set("stocks_long", it) }, cur)
            NumField(s.tradeGoods, v["goods"] ?: "", { set("goods", it) }, cur)
        }
    }
    // على الشاشة الواسعة: المدخلات في عمود، والنتيجة وتفصيلها بجانبها ثابتة أثناء الإدخال
    ToolPage(if (tradeFirst) s.zkTradeName else s.zkMoneyName, s.zakatSubtitle, onBack, icon = if (tradeFirst) Icons.Filled.ShoppingCart else Icons.Filled.Diamond) {
        side {
            ResultCard(s.zakatDue, Fmt.num(zakat), badge, if (nisab <= 0 || net <= 0) null else due, unit = cur) {
                if (nisab > 0) {
                    LinearProgressIndicator(
                        progress = { toNisab },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = Gold,
                        trackColor = Color.White.copy(alpha = .2f),
                        drawStopIndicator = {}
                    )
                }
            }
        }
        side {
            SectionCard {
                StatRow(s.totalAssets, "${Fmt.num(total)} $cur")
                StatRow(s.deductedDebts, "${Fmt.num(debts)} $cur")
                StatRow(s.netBase, "${Fmt.num(net)} $cur", strong = true)
                StatRow(s.nisabValue, if (nisab > 0) "${Fmt.num(nisab)} $cur" else "—")
                Reveal(zakat > 0 && gp > 0) { StatRow(s.approxEquals, s.goldGrams24(Fmt.num(if (gp > 0) zakat / gp else 0.0))) }
                Reveal(zakat > 0) {
                    FormShareButton(s.shareResult) {
                        shareText(s, s.zakatShareText("${Fmt.num(zakat)} $cur", "${Fmt.num(net)} $cur"))
                    }
                }
            }
        }
        if (tradeFirst) item { invest() }
        item {
            SectionCard(s.settingsAndPrices) {
                FormPicker(s.currency, currencies(lang), curObj, text = { c -> "${c.name(lang)} (${c.symbol(lang)})" },
                    onSelect = { curIso = it.iso; AppSettings.putString("zk_cur", it.iso) })
                FormLabel(s.nisabBasis)
                Segmented(listOf("gold" to s.nisabGold, "silver" to s.nisabSilver), basis) { basis = it; AppSettings.putString("zk_basis", it) }
                NumField(s.goldPrice24, v["gold_price"] ?: "", { set("gold_price", it) }, cur, s.goldPriceHint)
                NumField(s.silverPrice, v["silver_price"] ?: "", { set("silver_price", it) }, cur)
            }
        }
        item {
            SectionCard(s.cashSection) {
                NumField(s.cashInHand, v["cash"] ?: "", { set("cash", it) }, cur)
                NumField(s.bankBalances, v["bank"] ?: "", { set("bank", it) }, cur)
                NumField(s.receivablesField, v["receivable"] ?: "", { set("receivable", it) }, cur)
                NumField(s.otherMoney, v["other"] ?: "", { set("other", it) }, cur)
            }
        }
        item {
            SectionCard(s.goldSilverSection) {
                NumField(s.goldKarat(24), v["g24"] ?: "", { set("g24", it) }, s.unitGram)
                NumField(s.goldKarat(22), v["g22"] ?: "", { set("g22", it) }, s.unitGram)
                NumField(s.goldKarat(21), v["g21"] ?: "", { set("g21", it) }, s.unitGram)
                NumField(s.goldKarat(18), v["g18"] ?: "", { set("g18", it) }, s.unitGram)
                NumField(s.pureSilver, v["silver"] ?: "", { set("silver", it) }, s.unitGram)
                FormSwitchRow(s.includeJewelry, includeJewelry) { includeJewelry = it; AppSettings.putBool("zk_jewelry", it) }
                Reveal(!includeJewelry) {
                    NumField(s.jewelryField, v["jewel"] ?: "", { set("jewel", it) }, s.unitGram, s.jewelryHint)
                }
            }
        }
        if (!tradeFirst) item { invest() }
        item {
            SectionCard(s.deductSection) {
                NumField(s.debtsField, v["debts"] ?: "", { set("debts", it) }, cur, s.debtsHint)
            }
        }
        side {
            Reveal(zakat > 0 && parts.count { it.second > 0 } > 1) {
                SectionCard(s.breakdownTitle) {
                    val ratio = if (total > 0) net / total else 0.0
                    parts.filter { it.second > 0 }.forEach { (name, value) -> StatRow(name, "${Fmt.num(value * ratio * 0.025)} $cur") }
                }
            }
        }
        item { Note(s.zakatNote) }
        item { Note(s.zkCombinedNote) }
        item { EvidenceCard(if (tradeFirst) Evidence.zakatTrade else Evidence.zakatMoney, startOpen = true) }
        item { EvidenceCard(if (tradeFirst) Evidence.zakatMoney else Evidence.zakatTrade) }
    }
}
