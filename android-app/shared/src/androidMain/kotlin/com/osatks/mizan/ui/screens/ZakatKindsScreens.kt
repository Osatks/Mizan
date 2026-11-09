package com.osatks.mizan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.ZakatKinds
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.NumberStar
import com.osatks.mizan.ui.theme.Gold

/** أنواع الزكاة الخمسة أولاً، ثم حاسبة النوع المختار ودليله */
@Composable
fun ZakatScreen(onBack: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("") }
    val back = { type = "" }
    // نوع الزكاة صفحة فوق الأنواع: يُرجع منها بزر الرجوع أو بسحب الحافة الذي يتبع الإصبع
    com.osatks.mizan.ui.SubPages(type, root = "", onBack = back) { t ->
        when (t) {
            "money" -> ZakatMoneyScreen(back)
            "trade" -> ZakatMoneyScreen(back, tradeFirst = true)
            "livestock" -> LivestockZakat(back)
            "crops" -> CropsZakat(back)
            "minerals" -> MineralsZakat(back)
            else -> ZakatTypes(onBack) { type = it }
        }
    }
}

@Composable
private fun ZakatTypes(onBack: () -> Unit, pick: (String) -> Unit) {
    val s = LocalStrings.current
    val types = listOf(
        Triple("money", Icons.Filled.Diamond, s.zkMoneyName to s.zkMoneyDesc),
        Triple("trade", Icons.Filled.Storefront, s.zkTradeName to s.zkTradeDesc),
        Triple("livestock", Icons.Filled.Workspaces, s.zkLivestockName to s.zkLivestockDesc),
        Triple("crops", Icons.Filled.Agriculture, s.zkCropsName to s.zkCropsDesc),
        Triple("minerals", Icons.Filled.Landscape, s.zkMineralsName to s.zkMineralsDesc),
    )
    ToolPage(s.zakatTitle, s.zakatTypesSub, onBack, sharedKey = "zakat", icon = TOOL_ICONS["zakat"], spacing = 12.dp) {
        item { Text(s.zakatTypesTitle, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        types.forEachIndexed { i, (key, icon, text) -> item { TypeCard(i + 1, icon, text.first, text.second) { pick(key) } } }
        item { Note(s.zkCombinedNote) }
    }
}

@Composable
private fun TypeCard(n: Int, icon: ImageVector, name: String, desc: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .8f)), interactionSource = src,
        modifier = Modifier.fillMaxWidth().pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStar(n, 40.dp)
            Box(Modifier.padding(start = 12.dp).size(44.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** حقل عددي محفوظ */
@Composable
private fun saved(key: String): Pair<String, (String) -> Unit> {
    var v by rememberSaveable { mutableStateOf(AppSettings.getString(key)) }
    return v to { x: String -> v = x; AppSettings.putString(key, x) }
}

@Composable
internal fun LivestockZakat(onBack: () -> Unit) {
    val s = LocalStrings.current
    val (camelsT, setCamels) = saved("zk_camels")
    val (cattleT, setCattle) = saved("zk_cattle")
    val (sheepT, setSheep) = saved("zk_sheep")
    var grazing by rememberSaveable { mutableStateOf(AppSettings.getBool("zk_grazing", true)) }
    var year by rememberSaveable { mutableStateOf(AppSettings.getBool("zk_lyear", true)) }
    fun count(t: String) = Fmt.parse(t).toInt().coerceAtLeast(0)
    val rows = listOf(
        Triple(s.camels, count(camelsT), ZakatKinds.camels(count(camelsT))) to ZakatKinds.CAMEL_NISAB,
        Triple(s.cattle, count(cattleT), ZakatKinds.cattle(count(cattleT))) to ZakatKinds.CATTLE_NISAB,
        Triple(s.sheepGoats, count(sheepT), ZakatKinds.sheep(count(sheepT))) to ZakatKinds.SHEEP_NISAB,
    )
    val ok = grazing && year
    val dues = if (ok) rows.flatMap { it.first.third } else emptyList()
    val total = dues.sumOf { it.count }
    ToolPage(s.zkLivestockName, s.zkLivestockDesc, onBack, icon = Icons.Filled.Workspaces) {
        side {
            ResultCard(
                s.livestockDue, if (total > 0) "$total" else "—",
                if (!ok) s.conditionsNotMet else if (total > 0) s.nisabReached else s.nothingDue, if (rows.all { it.first.second == 0 }) null else total > 0,
                unit = if (total > 0) s.unitHead else null
            )
        }
        item {
            SectionCard {
                NumField(s.camels, camelsT, setCamels, s.unitHead, s.livestockNisab(ZakatKinds.CAMEL_NISAB))
                NumField(s.cattle, cattleT, setCattle, s.unitHead, s.livestockNisab(ZakatKinds.CATTLE_NISAB))
                NumField(s.sheepGoats, sheepT, setSheep, s.unitHead, s.livestockNisab(ZakatKinds.SHEEP_NISAB))
                CheckRow(s.grazingCondition, grazing) { grazing = it; AppSettings.putBool("zk_grazing", it) }
                CheckRow(s.yearCondition, year) { year = it; AppSettings.putBool("zk_lyear", it) }
            }
        }
        // ما يجب في كل صنف
        rows.filter { it.first.second > 0 }.forEach { (r, nisab) ->
            side(key = r.first) {
                SectionCard("${r.first} (${r.second})") {
                    when {
                        !ok -> StatRow(s.zkRate, s.nothingDue)
                        r.third.isEmpty() -> StatRow(s.livestockNisab(nisab), s.belowNisab)
                        else -> r.third.forEach { d -> StatRow(s.livestockKind(d.kind), "× ${d.count}", strong = true) }
                    }
                }
            }
        }
        item { Note(s.conditionsNotMet) }
        item { EvidenceCard(Evidence.zakatLivestock, startOpen = true) }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun CropsZakat(onBack: () -> Unit) {
    val s = LocalStrings.current
    val (kgT, setKg) = saved("zk_crop_kg")
    val (priceT, setPrice) = saved("zk_crop_price")
    var irr by rememberSaveable { mutableStateOf(AppSettings.getString("zk_irr", "rain")) }
    val kg = Fmt.parse(kgT)
    val due = ZakatKinds.crops(kg, irr)
    val price = Fmt.parse(priceT)
    val nisab = ZakatKinds.CROPS_NISAB_KG
    ToolPage(s.zkCropsName, s.zkCropsDesc, onBack, icon = Icons.Filled.Agriculture) {
        side {
            ResultCard(
                s.cropsDue, if (due > 0) Fmt.num(due) else "—",
                when { kg <= 0 -> s.cropsNisab(Fmt.num(nisab, 0)); due > 0 -> s.nisabReached; else -> s.belowNisab },
                if (kg <= 0) null else due > 0, unit = s.unitKg
            )
        }
        item {
            SectionCard {
                NumField(s.cropsWeight, kgT, setKg, s.unitKg, s.cropsNisab(Fmt.num(nisab, 0)))
                Text(s.irrigation, style = MaterialTheme.typography.labelLarge)
                Segmented(listOf("rain" to s.irrRain, "irrigated" to s.irrIrrigated, "mixed" to s.irrMixed), irr) { irr = it; AppSettings.putString("zk_irr", it) }
                StatRow(s.zkRate, "${Fmt.num(ZakatKinds.cropsRate(irr) * 100, 1)}%")
                NumField(s.kgPrice, priceT, setPrice, null, s.kgPriceHint)
                if (due > 0 && price > 0) StatRow(s.approxValue, Fmt.num(due * price), strong = true)
            }
        }
        item { EvidenceCard(Evidence.zakatCrops, startOpen = true) }
    }
}

@Composable
internal fun MineralsZakat(onBack: () -> Unit) {
    val s = LocalStrings.current
    val (rikazT, setRikaz) = saved("zk_rikaz")
    val (mineralT, setMineral) = saved("zk_mineral")
    val (goldT, setGold) = saved("zk_gold_price")
    val gold = Fmt.parse(goldT)
    val rikaz = ZakatKinds.rikaz(Fmt.parse(rikazT))
    val mineral = ZakatKinds.mineral(Fmt.parse(mineralT), 85 * gold)
    val total = rikaz + mineral
    ToolPage(s.zkMineralsName, s.zkMineralsDesc, onBack, icon = Icons.Filled.Landscape) {
        side { ResultCard(s.mineralsDue, if (total > 0) Fmt.num(total) else "—", null, null) }
        item {
            SectionCard {
                NumField(s.rikazValue, rikazT, setRikaz)
                StatRow("${s.zkRate} · ${s.rikazValue}", "20%")
                if (rikaz > 0) StatRow(s.zakatDue, Fmt.num(rikaz), strong = true)
            }
        }
        item {
            SectionCard {
                NumField(s.mineralValue, mineralT, setMineral)
                NumField(s.goldPrice24, goldT, setGold, null, s.goldPriceHint)
                StatRow(s.nisabValue, if (gold > 0) Fmt.num(85 * gold) else s.enterGoldPrice)
                StatRow(s.zkRate, "2.5%")
                if (mineral > 0) StatRow(s.zakatDue, Fmt.num(mineral), strong = true)
            }
        }
        item { EvidenceCard(Evidence.zakatMinerals, startOpen = true) }
    }
}
