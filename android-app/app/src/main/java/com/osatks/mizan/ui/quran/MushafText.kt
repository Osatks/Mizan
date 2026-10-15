package com.osatks.mizan.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep

/** كلمة من نص الصفحة (وآخر كلمة في الآية تحمل علامة ۝ ورقمها)، و index فهرس آيتها */
private class Token(val text: AnnotatedString, val index: Int)

/** سورة (أو جزء منها) داخل الصفحة: ترويسة إن بدأت السورة هنا، ثم كلماتها */
private class Block(val surah: Int, val banner: Boolean, val bismillah: Boolean, val tokens: List<Token>)

private const val REF = 40f          // مقاس القياس المرجعي؛ عرض الكلمة يتناسب مع المقاس
private const val LINE = 1.8f        // ارتفاع السطر نسبةً إلى مقاس الخط
private const val MIN_SIZE = 13f
private const val MAX_SIZE = 30f

/** علامات الوقف (ۖ ۗ ۚ …) تأتي في نص تنزيل كلماتٍ مستقلة؛ تُلصق بالكلمة قبلها حتى لا تبدأ بها سطراً */
private fun isMarkOnly(w: String) = w.all { Character.getType(it) == Character.NON_SPACING_MARK.toInt() || it in 'ۖ'..'ۭ' }

private fun blocksOf(page: Int, text: Array<String>, marker: Color): List<Block> =
    QuranMeta.pageRange(page).groupBy { QuranMeta.locate(it).first }.map { (surah, idxs) ->
        val tokens = ArrayList<Token>()
        idxs.forEach { i ->
            val words = ArrayList<String>()
            QuranData.withoutBismillah(text, i).split(' ').filter { it.isNotEmpty() }.forEach { w ->
                if (isMarkOnly(w) && words.isNotEmpty()) words[words.lastIndex] = words.last() + " " + w else words += w
            }
            val ayah = QuranMeta.locate(i).second
            words.forEachIndexed { k, w ->
                val t = if (k < words.lastIndex) AnnotatedString(w) else buildAnnotatedString {
                    append(w); append(' ')
                    withStyle(SpanStyle(color = marker)) { append("۝" + arabicDigits(ayah)) }
                }
                tokens += Token(t, i)
            }
        }
        val starts = QuranMeta.locate(idxs.first()).second == 1
        Block(surah, starts, starts && QuranData.hasBismillahHeader(surah), tokens)
    }

/** أقل عدد من الأسطر يسع الكلمات (ملء جشع) */
internal fun greedyLines(widths: FloatArray, space: Float, maxW: Float): Int {
    if (widths.isEmpty()) return 0
    var lines = 1; var w = 0f
    for (x in widths) {
        w = if (w == 0f) x else if (w + space + x <= maxW) w + space + x else { lines++; x }
    }
    return lines
}

/**
 * يوزّع الكلمات على العدد نفسه من الأسطر بحيث تتقارب أطوالها (بما فيها السطر الأخير)،
 * فتمتلئ كل الأسطر وتُضبط من الطرفين كسطور المصحف، وتنتهي الصفحة عند نهاية آية في آخر السطر.
 */
internal fun balancedLines(widths: FloatArray, space: Float, maxW: Float): List<IntRange> {
    val n = widths.size
    if (n == 0) return emptyList()
    val lines = greedyLines(widths, space, maxW)
    val inf = Float.MAX_VALUE
    // cost[k][i]: أقل كلفة لوضع أول i كلمة في k سطراً؛ الكلفة مجموع مربعات الفراغ في كل سطر
    val cost = Array(lines + 1) { FloatArray(n + 1) { inf } }
    val from = Array(lines + 1) { IntArray(n + 1) }
    cost[0][0] = 0f
    for (k in 1..lines) for (i in 1..n) {
        var w = -space
        var j = i
        while (j > 0) {
            w += widths[j - 1] + space
            if (w > maxW && j < i) break   // كلمة أعرض من السطر تأخذ سطراً وحدها
            val prev = cost[k - 1][j - 1]
            if (prev != inf) {
                val slack = (maxW - w).coerceAtLeast(0f)
                val c = prev + slack * slack
                if (c < cost[k][i]) { cost[k][i] = c; from[k][i] = j - 1 }
            }
            j--
        }
    }
    val out = ArrayList<IntRange>(lines)
    var i = n
    for (k in lines downTo 1) { val j = from[k][i]; out += j until i; i = j }
    return out.reversed()
}

/**
 * نص صفحة المصحف: يُختار أكبر مقاس يملأ الصفحة، وتُضبط كل الأسطر من الطرفين (حتى الأخير)
 * فتبدأ الصفحة في أعلاها وتنتهي آخر آية فيها عند طرف السطر الأخير، كما في المصحف المطبوع.
 */
@Composable
fun MushafText(page: Int, text: Array<String>, selected: Int, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val marker = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val blocks = remember(page, text, marker) { blocksOf(page, text, marker) }
    fun style(size: Float) = TextStyle(fontFamily = QuranFont, fontSize = size.sp, lineHeight = (size * LINE).sp, textDirection = TextDirection.Rtl)
    fun widthOf(t: AnnotatedString, size: Float) =
        measurer.measure(t, style(size), softWrap = false, maxLines = 1).size.width.toFloat()

    // العرض عند المقاس المرجعي، يُقاس مرة لكل صفحة
    val ref = remember(blocks, density) {
        blocks.map { b -> FloatArray(b.tokens.size) { widthOf(b.tokens[it].text, REF) } } to widthOf(AnnotatedString(" "), REF)
    }

    BoxWithConstraints(modifier) {
        val maxW = constraints.maxWidth.toFloat() * 0.985f
        val maxH = constraints.maxHeight.toFloat()
        val spPx = with(density) { 1.sp.toPx() }
        // عدد الأسطر عند مقاس معين: أسطر النص + سطر للترويسة + سطر للبسملة
        fun slots(size: Float): Int {
            val k = size / REF
            return blocks.indices.sumOf { b ->
                greedyLines(FloatArray(ref.first[b].size) { ref.first[b][it] * k }, ref.second * k, maxW) +
                    (if (blocks[b].banner) 1 else 0) + (if (blocks[b].bismillah) 1 else 0)
            }
        }
        var size = MAX_SIZE
        while (size > MIN_SIZE && slots(size) * size * LINE * spPx > maxH) size -= 0.5f
        val fits = slots(size) * size * LINE * spPx <= maxH
        val capped = size >= MAX_SIZE && slots(size) * size * LINE * spPx < maxH * 0.85f

        // توزيع الكلمات بالقياس الفعلي عند المقاس المختار
        val layout = remember(blocks, size, maxW) {
            blocks.map { b ->
                val widths = FloatArray(b.tokens.size) { widthOf(b.tokens[it].text, size) + 1f }
                val ranges = balancedLines(widths, widthOf(AnnotatedString(" "), size), maxW)
                val used = ranges.map { r -> r.sumOf { widths[it].toDouble() }.toFloat() }
                ranges.zip(used)
            }
        }
        val lineH = with(density) { (size * LINE).sp.toDp() }
        val textStyle = style(size)
        val selInk = if (com.osatks.mizan.ui.theme.LocalDark.current) Gold else GoldDeep

        Column(
            (if (fits) Modifier.fillMaxSize() else Modifier.fillMaxWidth().verticalScroll(rememberScrollState())),
            verticalArrangement = if (fits && !capped) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            blocks.forEachIndexed { b, block ->
                if (block.banner) MushafBanner(QuranMeta.NAMES_AR[block.surah - 1], size, lineH)
                if (block.bismillah) {
                    Text(
                        text[0], Modifier.fillMaxWidth().height(lineH), style = textStyle.copy(textAlign = TextAlign.Center),
                        color = marker, maxLines = 1, softWrap = false
                    )
                }
                layout[b].forEach { (range, used) ->
                    // سطر قصير وحيد (آخر سورة قصيرة مثلاً) يُوسَّط، وما سواه يُضبط من الطرفين
                    val justify = range.count() > 1 && (used > maxW * 0.55f || layout[b].size > 1)
                    Row(
                        Modifier.fillMaxWidth().height(lineH),
                        horizontalArrangement = if (justify) Arrangement.SpaceBetween else Arrangement.spacedBy(lineH / 5, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (t in range) {
                            val token = block.tokens[t]
                            // الآية المختارة تُلوَّن كلها (خلفية لكل كلمة تتقطع بين الكلمات)
                            Text(
                                token.text,
                                Modifier.clickable(interactionSource = null, indication = null) { onTap(token.index) },
                                style = textStyle, color = if (token.index == selected) selInk else ink, maxLines = 1, softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ترويسة السورة داخل صفحة المصحف: إطار مذهّب بارتفاع سطر واحد */
@Composable
private fun MushafBanner(name: String, size: Float, lineH: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(lineH), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f),
            border = BorderStroke(1.2.dp, GoldDeep.copy(alpha = .6f)),
            modifier = Modifier.fillMaxSize().padding(vertical = 3.dp)
        ) {}
        // النص خارج الإطار حتى لا يُقص أعلى الحروف وأسفلها
        Text(
            "سُورَةُ $name", textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
            style = TextStyle(fontFamily = QuranFont, fontSize = (size * 0.9f).sp, textDirection = TextDirection.Rtl),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
