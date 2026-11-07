package com.osatks.mizan.ui.quran

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.MadinahLine
import kotlin.math.min

/**
 * صفحة مصحف المدينة: خمسة عشر سطراً كما في الطبعة، كل سطر بكلماته نفسها، تمتد بين طرفي الصفحة
 * (والأسطر الموسَّطة في الوسط)، بخط الصفحة من خطوط المجمع. النتيجة PageLayout نفسه الذي يرسمه المصحف الآخر،
 * فيبقى التظليل واللمس والحفظ والتلاوة كما هي. null إن لم يكن خط الصفحة في الجهاز بعد.
 */
internal fun prepareMadinah(page: Int, w: Float, h: Float): PageLayout? {
    val lines = Madinah.lines(page)
    if (lines.isEmpty() || w <= 0f || h <= 0f) return null
    val path = Madinah.fontPath(page)
    val paint = filePaint(path) ?: return null
    val ref = 100f
    paint.textSize = ref
    val ayahLines = lines.filterIsInstance<MadinahLine.Ayah>()
    val widths = ayahLines.associateWith { l -> FloatArray(l.glyphs.size) { paint.measure(l.glyphs[it]) } }
    val natural = { l: MadinahLine.Ayah -> widths.getValue(l).sum() }
    // الفاتحة وأول البقرة: أسطر قليلة أقصر وأكبر في وسط إطار مزخرف
    val special = page <= 2
    val widest = ayahLines.filter { !it.centered || special }.maxOfOrNull(natural) ?: ayahLines.maxOfOrNull(natural) ?: return null
    if (widest <= 0f) return null
    val slots = if (special) 11 else 15
    val lineH = h / slots
    val target = if (special) w * 0.80f else w
    // المقاس من أطول سطر بعرض الصفحة، ولا يتجاوز ما يسعه ارتفاع السطر (الحروف بين -0.6 و 1.2 من المقاس)
    val size = min(ref * target / widest, lineH / 1.62f)
    val lineW = widest * size / ref
    val x0 = (w - lineW) / 2
    val k = size / ref

    val used = if (special) lines.withIndex().filter { it.value != null } else lines.withIndex().toList()
    val top0 = if (special) (h - used.size * lineH) / 2 else 0f
    val rows = ArrayList<MRow>()
    used.forEachIndexed { slot, (_, line) ->
        val top = top0 + slot * lineH
        when (line) {
            is MadinahLine.Header -> rows += BannerRow(top, lineH, line.surah)
            is MadinahLine.Basmala -> rows += BasmalaRow(top, lineH)
            is MadinahLine.Ayah -> {
                val ws = widths.getValue(line)
                val total = ws.sum() * k
                val centered = line.centered || special || line.glyphs.size == 1
                val gap = if (centered) 0f else (lineW - total) / (line.glyphs.size - 1)
                var right = if (centered) x0 + (lineW + total) / 2 else x0 + lineW
                val placed = ArrayList<Placed>(line.glyphs.size)
                for (j in line.glyphs.indices) {
                    val ww = ws[j] * k
                    val word = MWord(line.glyphs[j], if (j in line.ends) END else null, line.ayahs[j])
                    placed += Placed(word, right - ww, right, ww, 0f, line.glyphs[j])
                    right -= ww + gap
                }
                rows += TextRow(top, lineH, placed)
            }
            null -> {}
        }
    }
    // يُشكَّل كل ما يُرسم بالمقاس نفسه، فيكون أول رسم نسخاً من الذاكرة
    paint.textSize = size
    for (r in rows) if (r is TextRow) r.words.forEach { paint.measure(it.text) }
    return PageLayout(size, lineH, rows, h, fits = true, capped = special, qcf = page, font = path)
}

/** علامة الكلمة التي هي نهاية آية في مصحف المدينة (رمزها في الخط يحمل رقمها) */
internal const val END = "end"

/** أقلام صفحة مصحف المدينة: خط الصفحة، وخط الترويسات، وخط البسملة */
internal class MadinahPaints(val page: GlyphPaint, val header: GlyphPaint?, val basmala: GlyphPaint?)

internal fun madinahPaints(font: String): MadinahPaints? {
    val page = filePaint(font) ?: return null
    return MadinahPaints(page, assetPaint(Madinah.HEADER_FONT), assetPaint(Madinah.BASMALA_FONT))
}

/** خط أساس السطر: حروف خطوط المجمع بين -0.6 و 1.2 من المقاس، فوسطها فوق الخط بـ 0.3 */
private fun qBaseline(top: Float, h: Float, size: Float) = top + h / 2 + size * 0.3f

/**
 * يرسم صفحة مصحف المدينة: الترويسة (إطار المجمع واسم السورة) والبسملة بلون التطبيق وحبر المصحف،
 * والكلمات بحبر المصحف، وعلامات نهاية الآيات بأرقامها بلون التطبيق، والمخفي للحفظ أشرطة لينة.
 */
internal fun DrawScope.drawMadinah(l: PageLayout, p: MushafPalette, accent: Color, paints: MadinahPaints, alpha: Float, hide: (Int) -> Float) {
    val ink = p.ink.copy(alpha = p.ink.alpha * alpha).toArgb()
    val mark = accent.copy(alpha = accent.alpha * alpha).toArgb()
    val lineW = l.rows.filterIsInstance<TextRow>().let { rs ->
        val left = rs.minOfOrNull { r -> r.words.minOfOrNull { it.left } ?: Float.MAX_VALUE } ?: 0f
        val right = rs.maxOfOrNull { r -> r.words.maxOfOrNull { it.right } ?: 0f } ?: size.width
        (right - left).coerceAtLeast(size.width * 0.6f)
    }
    drawIntoCanvas { c ->
        for (row in l.rows) when (row) {
            is TextRow -> {
                val pp = paints.page
                pp.textSize = l.textSize
                val y = qBaseline(row.top, row.height, l.textSize)
                for (pl in row.words) {
                    val end = pl.word.marker == END
                    val h = if (end) 0f else hide(pl.word.index)
                    if (h > 0f) {
                        val inset = l.textSize * 0.04f
                        val bh = l.textSize * 0.62f
                        drawRoundRect(
                            p.ink.copy(alpha = 0.16f * h * alpha), Offset(pl.left + inset, y - l.textSize * 0.52f),
                            Size((pl.wordW - 2 * inset).coerceAtLeast(1f), bh), CornerRadius(bh / 2),
                        )
                    }
                    if (h < 1f) {
                        pp.color = when {
                            end -> mark
                            h <= 0f -> ink
                            else -> p.ink.copy(alpha = p.ink.alpha * alpha * (1f - h)).toArgb()
                        }
                        pp.draw(c, pl.text, pl.left, y)
                    }
                }
            }
            is BannerRow -> {
                val hp = paints.header ?: continue
                val code = Madinah.headerCode(row.surah)
                if (code.isEmpty()) continue
                hp.textSize = 100f
                val gw = hp.measure(code).coerceAtLeast(1f)
                // الإطار بعرض الأسطر، ولا يتجاوز ارتفاع السطر (ارتفاعه نحو 0.39 من المقاس وعرضه 3.3)
                val ts = min(100f * lineW / gw, row.height * 0.92f / 0.4f)
                hp.textSize = ts
                hp.color = mark
                val w = hp.measure(code)
                hp.draw(c, code, (size.width - w) / 2, row.top + row.height / 2)
            }
            is BasmalaRow -> {
                val bp = paints.basmala ?: continue
                bp.textSize = l.textSize * 1.02f
                var w = bp.measure(Madinah.BASMALA)
                if (w > lineW * 0.75f) { bp.textSize *= lineW * 0.75f / w; w = bp.measure(Madinah.BASMALA) }
                bp.color = ink
                bp.draw(c, Madinah.BASMALA, (size.width - w) / 2, row.top + row.height / 2 + bp.textSize * 0.32f)
            }
        }
    }
}
