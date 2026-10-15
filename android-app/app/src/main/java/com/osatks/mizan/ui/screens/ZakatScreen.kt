package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.NumField
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.shareText

private val CURRENCIES = listOf(
    "ر.س" to "ريال سعودي", "د.إ" to "درهم إماراتي", "د.ك" to "دينار كويتي", "ر.ق" to "ريال قطري",
    "د.ب" to "دينار بحريني", "ر.ع" to "ريال عماني", "ج.م" to "جنيه مصري", "د.أ" to "دينار أردني",
    "د.ع" to "دينار عراقي", "د.م" to "درهم مغربي", "د.ج" to "دينار جزائري", "د.ت" to "دينار تونسي",
    "ل.ل" to "ليرة لبنانية", "ل.س" to "ليرة سورية", "ر.ي" to "ريال يمني", "ج.س" to "جنيه سوداني",
    "د.ل" to "دينار ليبي", "$" to "دولار أمريكي", "€" to "يورو",
)
private val FIELDS = listOf("gold_price", "silver_price", "cash", "bank", "receivable", "other", "g24", "g22", "g21", "g18", "silver", "jewel", "stocks_trade", "stocks_long", "goods", "debts")

@Composable
fun ZakatScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val v = remember { mutableStateMapOf<String, String>().apply { FIELDS.forEach { put(it, AppSettings.getString("zk_$it")) } } }
    var cur by remember { mutableStateOf(AppSettings.getString("zk_cur", "ر.س")) }
    var basis by remember { mutableStateOf(AppSettings.getString("zk_basis", "gold")) }
    var includeJewelry by remember { mutableStateOf(AppSettings.getBool("zk_jewelry", true)) }

    fun set(k: String, s: String) { v[k] = s; AppSettings.putString("zk_$k", s) }
    fun n(k: String) = Fmt.parse(v[k] ?: "")

    val gp = n("gold_price"); val sp = n("silver_price")
    val pure24 = n("g24") + n("g22") * 22 / 24 + n("g21") * 21 / 24 + n("g18") * 18 / 24
    val excluded = if (includeJewelry) 0.0 else minOf(pure24, n("jewel") * 21 / 24)
    val gold = (pure24 - excluded) * gp
    val parts = listOf(
        "النقود والأرصدة" to n("cash") + n("bank") + n("other"),
        "الديون المرجوّة" to n("receivable"),
        "الذهب" to gold,
        "الفضة" to n("silver") * sp,
        "الأسهم" to n("stocks_trade") + n("stocks_long"),
        "عروض التجارة" to n("goods"),
    )
    val total = parts.sumOf { it.second }
    val debts = minOf(n("debts"), total)
    val net = (total - debts).coerceAtLeast(0.0)
    val nisab = if (basis == "silver") 595 * sp else 85 * gp
    val due = nisab > 0 && net >= nisab && net > 0
    val zakat = if (due) net * 0.025 else 0.0
    val badge = when {
        nisab <= 0 -> "أدخل سعر ${if (basis == "silver") "الفضة" else "الذهب"} لحساب النصاب"
        due -> "✓ بلغ مالك النصاب — تجب الزكاة"
        net > 0 -> "لم يبلغ النصاب — لا زكاة عليك"
        else -> "أدخل أموالك"
    }

    ToolPage("حاسبة الزكاة", "2.5% من المال إذا بلغ النصاب وحال عليه الحول", onBack) {
        item {
            ResultCard("الزكاة المستحقة", "${Fmt.num(zakat)} $cur", badge, if (nisab <= 0 || net <= 0) null else due) {
                if (nisab > 0) {
                    LinearProgressIndicator(
                        progress = { (net / nisab).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = androidx.compose.ui.graphics.Color(0xFFE0B54A),
                        trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .2f)
                    )
                }
            }
        }
        item {
            SectionCard {
                StatRow("إجمالي الأموال الزكوية", "${Fmt.num(total)} $cur")
                StatRow("الديون المطروحة", "${Fmt.num(debts)} $cur")
                StatRow("صافي الوعاء الزكوي", "${Fmt.num(net)} $cur", strong = true)
                StatRow("قيمة النصاب", if (nisab > 0) "${Fmt.num(nisab)} $cur" else "—")
                if (zakat > 0 && gp > 0) StatRow("تعادل تقريباً", "${Fmt.num(zakat / gp)} غرام ذهب عيار 24")
                if (zakat > 0) Button(onClick = {
                    shareText(ctx, "زكاتي المستحقة: ${Fmt.num(zakat)} $cur (الوعاء الزكوي ${Fmt.num(net)} $cur)")
                }, modifier = Modifier.fillMaxWidth()) { Text("مشاركة النتيجة") }
            }
        }
        item {
            SectionCard("الإعدادات والأسعار") {
                Picker("العملة", CURRENCIES.map { it.first }, cur, text = { c -> CURRENCIES.first { it.first == c }.second },
                    onSelect = { cur = it; AppSettings.putString("zk_cur", it) })
                Text("النصاب المعتمد للنقود", style = MaterialTheme.typography.labelLarge)
                Segmented(listOf("gold" to "الذهب (85 غ)", "silver" to "الفضة (595 غ)"), basis) { basis = it; AppSettings.putString("zk_basis", it) }
                NumField("سعر غرام الذهب عيار 24", v["gold_price"] ?: "", { set("gold_price", it) }, cur, "من محل ذهب أو موقع أسعار موثوق في بلدك")
                NumField("سعر غرام الفضة (اختياري)", v["silver_price"] ?: "", { set("silver_price", it) }, cur)
            }
        }
        item {
            SectionCard("النقود والأرصدة") {
                NumField("النقد في اليد", v["cash"] ?: "", { set("cash", it) }, cur)
                NumField("أرصدة البنوك والمحافظ", v["bank"] ?: "", { set("bank", it) }, cur)
                NumField("ديون لك مرجوّة السداد", v["receivable"] ?: "", { set("receivable", it) }, cur)
                NumField("أموال أخرى", v["other"] ?: "", { set("other", it) }, cur)
            }
        }
        item {
            SectionCard("الذهب والفضة (بالغرام)") {
                NumField("ذهب عيار 24", v["g24"] ?: "", { set("g24", it) }, "غ")
                NumField("ذهب عيار 22", v["g22"] ?: "", { set("g22", it) }, "غ")
                NumField("ذهب عيار 21", v["g21"] ?: "", { set("g21", it) }, "غ")
                NumField("ذهب عيار 18", v["g18"] ?: "", { set("g18", it) }, "غ")
                NumField("فضة خالصة", v["silver"] ?: "", { set("silver", it) }, "غ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeJewelry, onCheckedChange = { includeJewelry = it; AppSettings.putBool("zk_jewelry", it) })
                    Text("إدخال حُليّ الزينة المستعملة في الحساب (قول الحنفية، والأحوط)", style = MaterialTheme.typography.bodyMedium)
                }
                if (!includeJewelry) NumField("منها حُليّ زينة مستعملة (مكافئ عيار 21)", v["jewel"] ?: "", { set("jewel", it) }, "غ",
                    "يُستبعد على قول الجمهور بعدم زكاة الحلي المعدّ للاستعمال")
            }
        }
        item {
            SectionCard("الاستثمار والتجارة") {
                NumField("أسهم للمتاجرة (القيمة السوقية)", v["stocks_trade"] ?: "", { set("stocks_trade", it) }, cur)
                NumField("أسهم للاستثمار طويل الأجل (الوعاء الزكوي)", v["stocks_long"] ?: "", { set("stocks_long", it) }, cur)
                NumField("عروض التجارة (قيمة البضاعة)", v["goods"] ?: "", { set("goods", it) }, cur)
            }
        }
        item {
            SectionCard("ما يُطرح") {
                NumField("ديون عليك حالّة هذا العام", v["debts"] ?: "", { set("debts", it) }, cur, "الأقساط المستحقة خلال الحول فقط")
            }
        }
        if (zakat > 0 && parts.count { it.second > 0 } > 1) item {
            SectionCard("توزيع الزكاة حسب نوع المال") {
                val ratio = net / total
                parts.filter { it.second > 0 }.forEach { (name, value) -> StatRow(name, "${Fmt.num(value * ratio * 0.025)} $cur") }
            }
        }
        item { Note("تُحفظ بياناتك على جهازك فقط. مصارف الزكاة ثمانية ذكرها الله في سورة التوبة (الآية 60).") }
    }
}
