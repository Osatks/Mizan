package com.osatks.mizan.ui.quran

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.osatks.mizan.R
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.drawStar8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

// ---------- حساب الصفحة (بلا واجهة، يُختبر وحده) ----------

/** كلمة في الصفحة؛ marker علامة نهاية الآية برقمها (۝١٢) إن كانت آخر كلمة في آيتها */
internal class MWord(val text: String, val marker: String?, val index: Int)

/** سورة (أو جزء منها) في الصفحة: ترويسة إن بدأت السورة هنا، والبسملة، ثم كلماتها */
internal class MBlock(val surah: Int, val banner: Boolean, val bismillah: Boolean, val words: List<MWord>)

/** كلمة في موضعها من السطر؛ left/right حدود الكلمة مع علامتها */
internal class Placed(val word: MWord, val left: Float, val right: Float, val wordW: Float, val markerW: Float)

internal sealed class MRow(val top: Float, val height: Float)
internal class TextRow(top: Float, height: Float, val words: List<Placed>) : MRow(top, height)
internal class BannerRow(top: Float, height: Float, val surah: Int) : MRow(top, height)
internal class BasmalaRow(top: Float, height: Float) : MRow(top, height)

/** نتيجة الحساب: مقاس الخط، والصفوف بمواضعها، وارتفاع المحتوى، وهل وسعته الصفحة */
internal class PageLayout(val textSize: Float, val lineH: Float, val rows: List<MRow>, val height: Float, val fits: Boolean, val capped: Boolean = false) {
    /** الآية تحت نقطة اللمس، أو -1 */
    fun hit(x: Float, y: Float): Int {
        val row = rows.filterIsInstance<TextRow>().minByOrNull { r ->
            if (y < r.top) r.top - y else if (y > r.top + r.height) y - r.top - r.height else 0f
        } ?: return -1
        if (y < row.top - row.height / 2 || y > row.top + row.height * 1.5f) return -1
        val w = row.words.minByOrNull { if (x in it.left..it.right) 0f else min(abs(x - it.left), abs(x - it.right)) } ?: return -1
        return w.word.index
    }
}

internal fun interface Measure { fun width(text: String, size: Float): Float }

private const val LINE = 1.8f       // ارتفاع السطر نسبة إلى المقاس (أعلى التشكيل نحو 1.0 وأسفله نحو -0.5)
private const val BANNER = 1.6f     // ترويسة السورة تأخذ سطراً ونصفاً تقريباً
private const val REF = 100f

/** علامات الوقف (ۖ ۗ ۚ …) تأتي في نص تنزيل كلماتٍ مستقلة؛ تُلصق بالكلمة قبلها حتى لا تبدأ بها سطراً */
private fun isMarkOnly(w: String) = w.all { Character.getType(it) == Character.NON_SPACING_MARK.toInt() || it in '\u06D6'..'\u06ED' }

internal fun pageBlocks(page: Int, text: Array<String>): List<MBlock> =
    QuranMeta.pageRange(page).groupBy { QuranMeta.locate(it).first }.map { (surah, idxs) ->
        val words = ArrayList<MWord>()
        idxs.forEach { i ->
            val ws = ArrayList<String>()
            QuranData.withoutBismillah(text, i).split(' ').filter { it.isNotEmpty() }.forEach { w ->
                if (isMarkOnly(w) && ws.isNotEmpty()) ws[ws.lastIndex] = ws.last() + " " + w else ws += w
            }
            val ayah = QuranMeta.locate(i).second
            ws.forEachIndexed { k, w -> words += MWord(w, if (k == ws.lastIndex) "\u06DD" + arabicDigits(ayah) else null, i) }
        }
        val starts = QuranMeta.locate(idxs.first()).second == 1
        MBlock(surah, starts, starts && QuranData.hasBismillahHeader(surah), words)
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
 * يختار أكبر مقاس تسع به الصفحة كلماتها، ثم يوزعها على أسطر متقاربة تُضبط من الطرفين،
 * ويوزع ما بقي من الارتفاع بين الصفوف فيبدأ النص أعلى الصفحة وينتهي عند أسفلها.
 */
internal fun layoutPage(blocks: List<MBlock>, width: Float, height: Float, minSize: Float, maxSize: Float, step: Float, m: Measure): PageLayout {
    val nbspRef = m.width("\u00A0", REF)
    // كل كلمة تُقاس مرة واحدة عند مقاس مرجعي
    val refWord = blocks.map { b -> FloatArray(b.words.size) { m.width(b.words[it].text, REF) } }
    val refMark = blocks.map { b -> FloatArray(b.words.size) { j -> b.words[j].marker?.let { m.width(it, REF) } ?: 0f } }
    val ref = blocks.indices.map { bi ->
        FloatArray(blocks[bi].words.size) { j -> refWord[bi][j] + (if (blocks[bi].words[j].marker != null) nbspRef + refMark[bi][j] else 0f) }
    }
    val spaceRef = m.width(" ", REF)
    fun slots(size: Float): Float {
        val f = size / REF
        return blocks.indices.sumOf { b ->
            val ws = FloatArray(ref[b].size) { ref[b][it] * f }
            (greedyLines(ws, spaceRef * f, width) + (if (blocks[b].banner) BANNER else 0f) + (if (blocks[b].bismillah) 1f else 0f)).toDouble()
        }.toFloat()
    }
    var size = maxSize
    while (size > minSize && slots(size) * size * LINE > height) size -= step
    size = size.coerceAtLeast(minSize)
    val lineH = size * LINE
    val needed = slots(size) * lineH
    val fits = needed <= height
    val capped = size >= maxSize && needed < height * 0.85f

    // العرض يتناسب مع المقاس (قياس خطي)، فلا حاجة لقياس الكلمات مرة ثانية
    val k = size / REF
    val space = spaceRef * k
    val nbsp = nbspRef * k
    class Pending(val kind: Int, val h: Float, val surah: Int = 0, val words: List<Placed> = emptyList())
    val pending = ArrayList<Pending>()
    for ((bi, b) in blocks.withIndex()) {
        if (b.banner) pending += Pending(1, lineH * BANNER, b.surah)
        if (b.bismillah) pending += Pending(2, lineH)
        val wordW = FloatArray(b.words.size) { refWord[bi][it] * k }
        val markW = FloatArray(b.words.size) { refMark[bi][it] * k }
        val total = FloatArray(b.words.size) { j -> ref[bi][j] * k + 0.5f }
        val lines = balancedLines(total, space, width)
        for (r in lines) {
            val n = r.count()
            val used = r.sumOf { total[it].toDouble() }.toFloat()
            val fill = (used + space * (n - 1)) / width
            // سطر قصير جداً (في صفحة قليلة الآيات) يُوسَّط بدل أن تتباعد كلماته
            val justify = n > 1 && fill >= 0.6f
            val gap = if (justify) (width - used) / (n - 1) else space
            var cursor = if (justify) width else (width + used + gap * (n - 1)) / 2
            val placed = r.map { k ->
                val right = cursor; val left = right - total[k]
                cursor = left - gap
                Placed(b.words[k], left, right, wordW[k], markW[k])
            }
            pending += Pending(0, lineH, words = placed)
        }
    }
    val content = pending.sumOf { it.h.toDouble() }.toFloat()
    val between = if (fits && !capped && pending.size > 1) (height - content) / (pending.size - 1) else 0f
    var y = if (fits && capped) (height - content) / 2 else 0f
    val rows = pending.map { pr ->
        val row = when (pr.kind) {
            1 -> BannerRow(y, pr.h, pr.surah)
            2 -> BasmalaRow(y, pr.h)
            else -> TextRow(y, pr.h, pr.words)
        }
        y += pr.h + between
        row
    }
    return PageLayout(size, lineH, rows, if (fits) height else content, fits, fits && capped)
}

// ---------- الرسم ----------

/** خط المصحف لرسم الصفحات على Canvas */
private object MushafFont {
    @Volatile private var typeface: Typeface? = null
    fun get(ctx: Context): Typeface = typeface ?: (ResourcesCompat.getFont(ctx, R.font.amiri_quran) ?: Typeface.DEFAULT).also { typeface = it }
}

/** آخر الصفحات المحسوبة، فالرجوع إلى صفحة سابقة فوري */
private object LayoutCache {
    private val map = object : LinkedHashMap<String, PageLayout>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PageLayout>?) = size > 12
    }
    @Synchronized fun peek(key: String) = map[key]
    @Synchronized fun put(key: String, v: PageLayout) { map[key] = v }
}

private fun measureWith(tf: Typeface): Measure {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply { typeface = tf }
    return Measure { t, size -> paint.textSize = size; paint.measureText(t) }
}

/** خط الأساس داخل الصف: وسط ما بين أعلى التشكيل (نحو 1.0) وأسفله (نحو -0.5) */
private fun baseline(top: Float, h: Float, size: Float) = top + h / 2 + size * 0.27f

/**
 * نص صفحة المصحف مرسوماً دفعة واحدة على Canvas: أخف بكثير من عنصر لكل كلمة، فيبقى تقليب الصفحات سلساً.
 * الآية المختارة تُظلَّل بشريط متصل يتلاشى بين آية وأخرى، والضغط على أي كلمة يفتح تفسير آيتها.
 */
@Composable
fun MushafCanvas(page: Int, text: Array<String>, selected: Int, onTap: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = mushafPalette()
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val inspection = LocalInspectionMode.current
    val tf = remember { MushafFont.get(ctx) }
    val blocks = remember(page, text) { pageBlocks(page, text) }
    val description = remember(page, text) {
        QuranMeta.pageRange(page).joinToString(" ") { QuranData.withoutBismillah(text, it) + " (" + QuranMeta.locate(it).second + ")" }
    }
    val tapState = rememberUpdatedState(onTap)

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val spPx = with(density) { 1.sp.toPx() }
        val key = "$page:${System.identityHashCode(text)}:$w:$h:$spPx"
        fun compute() = layoutPage(blocks, w, h, 13 * spPx, 30 * spPx, 0.5f * spPx, measureWith(tf)).also { LayoutCache.put(key, it) }
        // تُحسب الصفحة في الخلفية (والصفحات المجاورة قبل أن تظهر)، وتظهر بتلاشٍ لطيف
        val state = remember(key) { mutableStateOf(LayoutCache.peek(key) ?: if (inspection) compute() else null) }
        LaunchedEffect(key) { if (state.value == null) state.value = withContext(Dispatchers.Default) { compute() } }
        val layout = state.value
        val appear = remember(key) { Animatable(if (layout != null) 1f else 0f) }
        LaunchedEffect(layout != null) { if (layout != null && appear.value < 1f) appear.animateTo(1f, tween(220)) }

        // الآية المختارة: الشريط الجديد يظهر والسابق يتلاشى
        var shown by remember { mutableIntStateOf(selected) }
        var previous by remember { mutableIntStateOf(-1) }
        val swap = remember { Animatable(1f) }
        LaunchedEffect(selected) {
            if (selected != shown) { previous = shown; shown = selected; swap.snapTo(0f); swap.animateTo(1f, tween(280)) }
        }
        var pressed by remember { mutableIntStateOf(-1) }
        val pressA = remember { Animatable(0f) }
        LaunchedEffect(pressed) { if (pressed >= 0) pressA.animateTo(1f, tween(140, delayMillis = 70)) else pressA.snapTo(0f) }

        val paint = remember(tf) { TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply { typeface = tf } }
        val l = layout ?: return@BoxWithConstraints
        val area = if (l.fits) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(with(density) { l.height.toDp() })
        val canvas = area
            .semantics { contentDescription = description }
            .pointerInput(l) {
                detectTapGestures(
                    onPress = { pos ->
                        val i = l.hit(pos.x, pos.y)
                        if (i >= 0) { pressed = i; tryAwaitRelease(); pressed = -1 }
                    },
                    onTap = { pos -> val i = l.hit(pos.x, pos.y); if (i >= 0) tapState.value(i) }
                )
            }
            // الصفحة كلها (الترويسات والنص) تظهر معاً بتلاشٍ واحد
            .graphicsLayer { alpha = appear.value }
            .drawBehind {
                band(l, previous, p.select, 1f - swap.value)
                band(l, shown, p.select, swap.value)
                band(l, pressed, p.press, pressA.value)
                drawPage(l, p, paint, text[0], 1f)
            }
        if (l.fits) Spacer(canvas) else Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { Spacer(canvas) }
    }
}

/** شريط متصل خلف كلمات الآية في كل سطر */
private fun DrawScope.band(l: PageLayout, index: Int, color: Color, alpha: Float) {
    if (index < 0 || alpha <= 0.01f) return
    val padX = 4.dp.toPx(); val r = CornerRadius(10.dp.toPx())
    for (row in l.rows) {
        if (row !is TextRow) continue
        var x0 = Float.MAX_VALUE; var x1 = -1f
        for (pl in row.words) if (pl.word.index == index) { x0 = min(x0, pl.left); x1 = maxOf(x1, pl.right) }
        if (x1 < 0) continue
        val top = row.top + row.height * 0.06f
        drawRoundRect(
            color.copy(alpha = color.alpha * alpha), Offset(x0 - padX, top),
            Size(x1 - x0 + 2 * padX, row.height * 0.88f), r
        )
    }
}

private fun DrawScope.drawPage(l: PageLayout, p: MushafPalette, paint: TextPaint, basmala: String, alpha: Float) {
    // صفحتا الفاتحة وأول البقرة (نص قليل في وسط الصفحة): إطار مزخرف حول النص كما في المصاحف المطبوعة
    if (l.capped && l.rows.isNotEmpty()) {
        val pad = l.lineH * 0.45f
        val top = l.rows.first().top - pad
        val bottom = l.rows.last().let { it.top + it.height } + pad
        val inset = 4.dp.toPx()
        val r = CornerRadius(22.dp.toPx())
        drawRoundRect(p.fill.copy(alpha = .35f * alpha), Offset(-inset, top), Size(size.width + 2 * inset, bottom - top), r)
        drawRoundRect(p.goldDeep.copy(alpha = .8f * alpha), Offset(-inset, top), Size(size.width + 2 * inset, bottom - top), r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
        val i2 = 4.dp.toPx()
        drawRoundRect(
            p.gold.copy(alpha = .7f * alpha), Offset(-inset + i2, top + i2), Size(size.width + 2 * inset - 2 * i2, bottom - top - 2 * i2),
            CornerRadius(18.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(0.7.dp.toPx())
        )
        // نجمة كبيرة باهتة فوق الإطار وأخرى تحته تملآن الفراغ
        val room = top
        if (room > l.lineH * 1.5f) {
            val rr = (room * 0.32f).coerceAtMost(size.width * 0.16f)
            drawStar8(Offset(size.width / 2, top / 2), rr, p.gold.copy(alpha = .5f * alpha), 1.2.dp.toPx())
            drawStar8(Offset(size.width / 2, (bottom + size.height) / 2), rr, p.gold.copy(alpha = .5f * alpha), 1.2.dp.toPx())
        }
    }
    // الترويسات أولاً، ثم كل النصوص فوقها
    val plates = HashMap<BannerRow, Float>()
    for (row in l.rows) if (row is BannerRow) {
        val pad = row.height * 0.1f
        plates[row] = drawCartouche(0f, row.top + pad, size.width, row.top + row.height - pad, p)
    }
    val ink = p.ink.copy(alpha = p.ink.alpha * alpha).toArgb()
    val marker = p.marker.copy(alpha = p.marker.alpha * alpha).toArgb()
    val title = p.goldDeep.copy(alpha = alpha).toArgb()
    drawIntoCanvas { c ->
        val nc = c.nativeCanvas
        for (row in l.rows) when (row) {
            is TextRow -> {
                paint.textSize = l.textSize
                val y = baseline(row.top, row.height, l.textSize)
                for (pl in row.words) {
                    paint.color = ink
                    nc.drawText(pl.word.text, pl.right - pl.wordW, y, paint)
                    val mk = pl.word.marker ?: continue
                    paint.color = marker
                    nc.drawText(mk, pl.left, y, paint)
                }
            }
            is BasmalaRow -> {
                paint.textSize = l.textSize
                paint.color = ink
                nc.drawText(basmala, (size.width - paint.measureText(basmala)) / 2, baseline(row.top, row.height, l.textSize), paint)
            }
            is BannerRow -> {
                val name = "سُورَةُ " + QuranMeta.NAMES_AR[row.surah - 1]
                var ts = l.textSize * 0.92f
                paint.textSize = ts
                val avail = (plates[row] ?: size.width) * 0.92f
                val nw = paint.measureText(name)
                if (nw > avail) { ts *= avail / nw; paint.textSize = ts }
                paint.color = title
                nc.drawText(name, (size.width - paint.measureText(name)) / 2, baseline(row.top, row.height, ts * 1.05f), paint)
            }
        }
    }
}
