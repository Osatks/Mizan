package com.osatks.mizan.ui.quran

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Canvas
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.locked
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.drawStar8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

// ---------- حساب الصفحة (بلا واجهة، يُختبر وحده) ----------

/** كلمة في الصفحة؛ marker علامة نهاية الآية برقمها (۝١٢) إن كانت آخر كلمة في آيتها */
internal class MWord(val text: String, val marker: String?, val index: Int, val pause: String? = null, val ayah: Int = 0) {
    /** موضع المدّ (الكشيدة) في الكلمة لضبط السطر، أو -1 */
    val kashidaAt = kashidaPoint(text)
}

/** سورة (أو جزء منها) في الصفحة: ترويسة إن بدأت السورة هنا، والبسملة، ثم كلماتها */
internal class MBlock(val surah: Int, val banner: Boolean, val bismillah: Boolean, val words: List<MWord>)

/** كلمة في موضعها من السطر؛ left/right حدود الكلمة مع علامتها، و text نصها كما يُرسم (قد يكون ممدوداً) */
internal class Placed(val word: MWord, val left: Float, val right: Float, val wordW: Float, val markerW: Float, val text: String = word.text)

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

private const val LINE = 1.9f       // ارتفاع السطر نسبة إلى المقاس (أعلى التشكيل نحو 1.0 وأسفله نحو -0.5)
// المسافة بين الكلمات كما في المصحف: متقاربة. أقلها للتقسيم على الأسطر، والطبيعية قبل المدّ
private const val MIN_GAP = 0.14f
private const val NAT_GAP = 0.22f
private const val MAX_KASHIDA = 3   // أقصى عدد للتطويلات في الكلمة الواحدة
private const val KASHIDA_WORDS = 2 // يُمدّ في السطر كلمتان على الأكثر (الأطول)، كما في المصحف
private const val PAUSE_SLOT = 0.2f // مكان علامة الوقف بعد كلمتها مباشرة
private const val BANNER = 1.6f     // ترويسة السورة تأخذ سطراً ونصفاً تقريباً
private const val REF = 100f
// شريط الكلمة المخفية في وضع الحفظ: أعلاه وارتفاعه نسبة إلى المقاس من خط الأساس، وشفافيته
private const val HIFZ_TOP = 0.46f
private const val HIFZ_HEIGHT = 0.56f
private const val HIFZ_ALPHA = 0.16f

/** علامات الوقف (ۖ ۗ ۚ …) تأتي في نص تنزيل كلماتٍ مستقلة؛ تُلصق بالكلمة قبلها حتى لا تبدأ بها سطراً */
private fun isPause(w: String) = w.isNotEmpty() && w.all { it in '\u06D6'..'\u06DC' }
private fun isMarkOnly(w: String) = w.all { it.category == CharCategory.NON_SPACING_MARK || it in '\u06D6'..'\u06ED' }

private const val DUAL = "بتثجحخسشصضطظعغفقكلمنهيئىٮٯڤـ"   // حروف تتصل بما بعدها
private const val RIGHT = "اأإآٱدذرزوؤة"                    // حروف تتصل بما قبلها فقط
private const val ALEFS = "اأإآٱ"

private fun isMarkChar(c: Char) = c.category == CharCategory.NON_SPACING_MARK || c in '\u06D6'..'\u06ED'

/**
 * أين يُمدّ الحرف (كشيدة) في الكلمة: بين حرفين متصلين، أقرب ما يكون إلى آخر الكلمة كما في الخط،
 * ولا يُمدّ داخل «لا» ولا في لفظ الجلالة. يُرجع موضع الإدراج (قبل الحرف التالي وبعد تشكيل ما قبله) أو -1.
 */
internal fun kashidaPoint(w: String): Int {
    if (w.filterNot(::isMarkChar).contains("لله")) return -1
    val bases = w.indices.filter { !isMarkChar(w[it]) }
    for (k in bases.size - 2 downTo 0) {
        val a = w[bases[k]]; val b = w[bases[k + 1]]
        if (a in DUAL && (b in DUAL || b in RIGHT) && !(a == 'ل' && b in ALEFS)) return bases[k + 1]
    }
    return -1
}

internal fun pageBlocks(page: Int, text: Array<String>): List<MBlock> =
    QuranMeta.pageRange(page).groupBy { QuranMeta.locate(it).first }.map { (surah, idxs) ->
        val words = ArrayList<MWord>()
        idxs.forEach { i ->
            val ws = ArrayList<String>()
            val pauses = HashMap<Int, String>()
            QuranData.withoutBismillah(text, i).split(' ').filter { it.isNotEmpty() }.forEach { w ->
                when {
                    // علامة الوقف تُرسم في مكان خاص بها بعد كلمتها، فتبقى أطراف الأسطر مستوية
                    isPause(w) && ws.isNotEmpty() -> pauses[ws.lastIndex] = (pauses[ws.lastIndex] ?: "") + w
                    isMarkOnly(w) && ws.isNotEmpty() -> ws[ws.lastIndex] = ws.last() + " " + w
                    else -> ws += w
                }
            }
            val ayah = QuranMeta.locate(i).second
            ws.forEachIndexed { k, w -> words += MWord(w, if (k == ws.lastIndex) AYAH_END else null, i, pauses[k], ayah) }
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
    val nbspRef = REF * 0.1f   // علامة الآية قريبة من آخر كلمة فيها
    // كل كلمة تُقاس مرة واحدة عند مقاس مرجعي
    val refWord = blocks.map { b -> FloatArray(b.words.size) { m.width(b.words[it].text, REF) } }
    val refMark = blocks.map { b -> FloatArray(b.words.size) { j -> b.words[j].marker?.let { m.width(it, REF) } ?: 0f } }
    val ref = blocks.indices.map { bi ->
        FloatArray(blocks[bi].words.size) { j ->
            val w = blocks[bi].words[j]
            refWord[bi][j] + (if (w.pause != null) REF * PAUSE_SLOT else 0f) + (if (w.marker != null) nbspRef + refMark[bi][j] else 0f)
        }
    }
    val gapRef = REF * MIN_GAP
    fun slots(size: Float): Float {
        val f = size / REF
        return blocks.indices.sumOf { b ->
            val ws = FloatArray(ref[b].size) { ref[b][it] * f }
            (greedyLines(ws, gapRef * f, width) + (if (blocks[b].banner) BANNER else 0f) + (if (blocks[b].bismillah) 1f else 0f)).toDouble()
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
    val space = gapRef * k
    val natural = size * NAT_GAP
    val tatweel = m.width("\u0640", size)
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
            val idx = r.toList()
            val fill = (r.sumOf { total[it].toDouble() }.toFloat() + space * (n - 1)) / width
            // سطر قصير جداً (في صفحة قليلة الآيات) يُوسَّط بدل أن تتباعد كلماته
            val justify = n > 1 && fill >= 0.6f
            // ضبط السطر كالمصحف: الكلمات متقاربة، ويُملأ الباقي بمدّ الحروف في الكلمات الطويلة أولاً،
            // وما بقي بعد ذلك (أقل من مدّة واحدة لكل كلمة) يتوزع على المسافات
            val kash = IntArray(n)
            if (justify && tatweel > 0f) {
                var budget = ((width - r.sumOf { total[it].toDouble() }.toFloat() - natural * (n - 1)) / tatweel).toInt()
                val order = idx.indices.filter { b.words[idx[it]].kashidaAt >= 0 }
                    .sortedByDescending { b.words[idx[it]].text.count { c -> !isMarkChar(c) } }
                    .take(KASHIDA_WORDS)
                var round = 0
                while (budget > 0 && order.isNotEmpty() && round < MAX_KASHIDA) {
                    for (j in order) { if (budget <= 0) break; kash[j]++; budget-- }
                    round++
                }
            }
            val texts = Array(n) { j ->
                val w = b.words[idx[j]]
                if (kash[j] == 0) w.text else w.text.substring(0, w.kashidaAt) + "\u0640".repeat(kash[j]) + w.text.substring(w.kashidaAt)
            }
            val ww = FloatArray(n) { j -> if (kash[j] == 0) wordW[idx[j]] else m.width(texts[j], size) }
            val tw = FloatArray(n) { j -> total[idx[j]] - wordW[idx[j]] + ww[j] }
            val used = tw.sum()
            val gap = if (justify) ((width - used) / (n - 1)).coerceAtLeast(0f) else space
            var cursor = if (justify) width else (width + used + gap * (n - 1)) / 2
            val placed = List(n) { j ->
                val right = cursor; val left = right - tw[j]
                cursor = left - gap
                Placed(b.words[idx[j]], left, right, ww[j], markW[idx[j]], texts[j])
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

/** علامة نهاية الآية وحدها؛ رقمها يُرسم داخلها بأنفسنا ولا نعتمد على تركيب الخط (لا يجمعهما كل إصدار من أندرويد) */
internal const val AYAH_END = "\u06DD"
// مركز الحلقة في خط المصحف نسبةً إلى المقاس: أفقياً من بدايتها، وعمودياً فوق خط الأساس
private const val RING_CX = 0.64f
private const val RING_CY = 0.166f

/** يرسم ۝ عند left على خط الأساس، وفي وسطها تماماً رقم الآية بالأرقام المشرقية */
internal fun drawAyahEnd(nc: Canvas, paint: GlyphPaint, ayah: Int, left: Float, baseline: Float, size: Float, ringColor: Int, numColor: Int) {
    val saved = paint.textSize
    paint.textSize = size
    paint.color = ringColor
    paint.draw(nc, AYAH_END, left, baseline)
    val digits = arabicDigits(ayah)
    paint.textSize = size * (if (ayah >= 100) 0.38f else 0.5f)
    paint.color = numColor
    val b = paint.bounds(digits)
    paint.draw(nc, digits, left + size * RING_CX - (b[0] + b[2]) / 2, baseline - size * RING_CY - (b[1] + b[3]) / 2)
    paint.textSize = saved
}

/** آخر الصفحات المحسوبة، فالرجوع إلى صفحة سابقة فوري */
private object LayoutCache {
    private val lock = Lock()
    private val map = LinkedHashMap<String, PageLayout>()
    fun peek(key: String) = lock.locked { map[key] }
    fun put(key: String, v: PageLayout) = lock.locked {
        map.remove(key); map[key] = v
        while (map.size > 12) map.remove(map.keys.first())
    }
}

private fun measureWith(paint: GlyphPaint): Measure = Measure { t, size -> paint.textSize = size; paint.measure(t) }

/** خط الأساس داخل الصف: وسط ما بين أعلى التشكيل (نحو 1.0) وأسفله (نحو -0.5) */
private fun baseline(top: Float, h: Float, size: Float) = top + h / 2 + size * 0.27f

/**
 * نص صفحة المصحف مرسوماً دفعة واحدة على Canvas: أخف بكثير من عنصر لكل كلمة، فيبقى تقليب الصفحات سلساً.
 * الآية المختارة تُظلَّل بشريط متصل يتلاشى بين آية وأخرى، والضغط على أي كلمة يفتح تفسير آيتها،
 * والضغط المطوّل يُظهر خيارات الآية.
 */
@Composable
fun MushafCanvas(
    page: Int, text: Array<String>, selected: Int, onTap: (Int) -> Unit, modifier: Modifier = Modifier,
    hifz: Boolean = false, revealed: Set<Int> = emptySet(), onLongPress: ((Int) -> Unit)? = null,
) {
    val p = mushafPalette()
    val density = LocalDensity.current
    val inspection = LocalInspectionMode.current
    val measurer = remember { mushafPaint() }
    val blocks = remember(page, text) { pageBlocks(page, text) }
    val description = remember(page, text) {
        QuranMeta.pageRange(page).joinToString(" ") { QuranData.withoutBismillah(text, it) + " (" + QuranMeta.locate(it).second + ")" }
    }
    val tapState = rememberUpdatedState(onTap)
    val longState = rememberUpdatedState(onLongPress)

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val spPx = with(density) { 1.sp.toPx() }
        val key = "$page:${text.hashCode()}:$w:$h:$spPx"
        fun compute() = layoutPage(blocks, w, h, 13 * spPx, 30 * spPx, 0.5f * spPx, measureWith(measurer)).also { LayoutCache.put(key, it) }
        // تُحسب الصفحة في الخلفية (والصفحات المجاورة قبل أن تظهر). إذا تغيّر المقاس (ظهور الرأس أو اختفاؤه
        // في وضع التركيز) تبقى الصفحة السابقة معروضة حتى يستقر المقاس، ثم تحل محلها الجديدة بتلاشٍ متقاطع.
        val current = remember { mutableStateOf(LayoutCache.peek(key) ?: if (inspection) compute() else null) }
        val leaving = remember { mutableStateOf<PageLayout?>(null) }
        val mix = remember { Animatable(1f) }
        LaunchedEffect(key) {
            val next = LayoutCache.peek(key) ?: run {
                if (current.value != null) delay(140)
                withContext(Dispatchers.Default) { compute() }
            }
            if (next !== current.value) {
                leaving.value = current.value
                current.value = next
                mix.snapTo(0f)
                mix.animateTo(1f, tween(240))
                leaving.value = null
            }
        }

        // الآية المختارة: الشريط الجديد يظهر والسابق يتلاشى
        var shown by remember { mutableIntStateOf(selected) }
        var previous by remember { mutableIntStateOf(-1) }
        val swap = remember { Animatable(1f) }
        LaunchedEffect(selected) {
            if (selected != shown) { previous = shown; shown = selected; swap.snapTo(0f); swap.animateTo(1f, tween(280)) }
        }
        // الحفظ: الآيات المخفية أشرطة لينة وعلامات آياتها ظاهرة، وما يُكشف أو يُخفى يتلاشى إلى حاله الجديدة
        val hifzA = remember { Animatable(if (hifz) 1f else 0f) }
        LaunchedEffect(hifz) { hifzA.animateTo(if (hifz) 1f else 0f, tween(360)) }
        var drawn by remember { mutableStateOf(revealed) }
        var changing by remember { mutableStateOf(emptySet<Int>()) }
        val change = remember { Animatable(1f) }
        LaunchedEffect(revealed) {
            val diff = (revealed - drawn) + (drawn - revealed)
            if (diff.isEmpty()) return@LaunchedEffect
            change.snapTo(0f)
            changing = diff
            drawn = revealed
            change.animateTo(1f, tween(320))
            changing = emptySet()
        }
        // مقدار إخفاء الآية: 0 ظاهرة، 1 شريط
        val hide: (Int) -> Float = { i ->
            val h = hifzA.value
            if (h <= 0f) 0f else {
                val target = if (i in drawn) 0f else 1f
                h * if (i in changing) (1f - target) + (2f * target - 1f) * change.value else target
            }
        }

        var pressed by remember { mutableIntStateOf(-1) }
        val pressA = remember { Animatable(0f) }
        LaunchedEffect(pressed) { if (pressed >= 0) pressA.animateTo(1f, tween(140, delayMillis = 70)) else pressA.snapTo(0f) }

        val paint = remember { mushafPaint() }
        @Composable
        fun layer(l: PageLayout, alpha: () -> Float, interactive: Boolean) {
            val area = if (l.fits) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(with(density) { l.height.toDp() })
            val canvas = area
                .then(
                    if (!interactive) Modifier else Modifier
                        .semantics { contentDescription = description }
                        .pointerInput(l) {
                            detectTapGestures(
                                onPress = { pos ->
                                    val i = l.hit(pos.x, pos.y)
                                    if (i >= 0) { pressed = i; tryAwaitRelease(); pressed = -1 }
                                },
                                onTap = { pos -> val i = l.hit(pos.x, pos.y); if (i >= 0) tapState.value(i) },
                                // الضغط المطوّل: شريط خيارات الآية (التفسير والاستماع والحفظ والنسخ والمشاركة)
                                onLongPress = { pos -> val i = l.hit(pos.x, pos.y); if (i >= 0) longState.value?.invoke(i) },
                            )
                        }
                )
                // الصفحة كلها (الترويسات والنص) تظهر وتختفي معاً
                .graphicsLayer { this.alpha = alpha() }
                .drawBehind {
                    band(l, previous, p.select, 1f - swap.value)
                    band(l, shown, p.select, swap.value)
                    if (interactive) band(l, pressed, p.press, pressA.value)
                    drawPage(l, p, paint, text[0], 1f, hide)
                }
            if (l.fits) Spacer(canvas) else Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { Spacer(canvas) }
        }
        leaving.value?.let { old -> layer(old, { 1f - mix.value }, interactive = false) }
        current.value?.let { l -> layer(l, { mix.value }, interactive = true) }
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

private fun DrawScope.drawPage(l: PageLayout, p: MushafPalette, paint: GlyphPaint, basmala: String, alpha: Float, hide: (Int) -> Float = { 0f }) {
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
        val nc = c
        for (row in l.rows) when (row) {
            is TextRow -> {
                paint.textSize = l.textSize
                val y = baseline(row.top, row.height, l.textSize)
                for (pl in row.words) {
                    val h = hide(pl.word.index)
                    // الكلمة المخفية للحفظ: شريط لين بطولها يدل على موضعها وطولها
                    if (h > 0f) {
                        val inset = l.textSize * 0.04f
                        val top = y - l.textSize * HIFZ_TOP
                        val bh = l.textSize * HIFZ_HEIGHT
                        drawRoundRect(
                            p.ink.copy(alpha = HIFZ_ALPHA * h * alpha), Offset(pl.right - pl.wordW + inset, top),
                            Size((pl.wordW - 2 * inset).coerceAtLeast(1f), bh), CornerRadius(bh / 2)
                        )
                    }
                    if (h < 1f) {
                        paint.color = if (h <= 0f) ink else p.ink.copy(alpha = p.ink.alpha * alpha * (1f - h)).toArgb()
                        paint.draw(nc, pl.text, pl.right - pl.wordW, y)
                        pl.word.pause?.let { mark ->
                            val slot = l.textSize * PAUSE_SLOT
                            val cx = pl.right - pl.wordW - slot / 2
                            val bounds = paint.bounds(mark)
                            paint.draw(nc, mark, cx - (bounds[0] + bounds[2]) / 2f, y)
                        }
                    }
                    if (pl.word.marker == null) continue
                    drawAyahEnd(nc, paint, pl.word.ayah, pl.left, y, l.textSize, marker, ink)
                }
            }
            is BasmalaRow -> {
                paint.textSize = l.textSize
                paint.color = ink
                paint.draw(nc, basmala, (size.width - paint.measure(basmala)) / 2, baseline(row.top, row.height, l.textSize))
            }
            is BannerRow -> {
                val name = "سُورَةُ " + QuranMeta.NAMES_AR[row.surah - 1]
                var ts = l.textSize * 0.92f
                paint.textSize = ts
                val avail = (plates[row] ?: size.width) * 0.92f
                val nw = paint.measure(name)
                if (nw > avail) { ts *= avail / nw; paint.textSize = ts }
                paint.color = title
                paint.draw(nc, name, (size.width - paint.measure(name)) / 2, baseline(row.top, row.height, ts * 1.05f))
            }
        }
    }
}
