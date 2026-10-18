package com.osatks.mizan.ui.quran

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.osatks.mizan.platform.Platform
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font as SkFont
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

private const val FONT = "fonts/amiri_quran.ttf"
private val fontBytes: ByteArray by lazy { Platform.asset(FONT) ?: ByteArray(0) }
private val typeface: Typeface? by lazy { FontMgr.default.makeFromData(Data.makeFromBytes(fontBytes)) }

/** TextLine يشكّل الحروف (HarfBuzz) فتتصل الحروف العربية كما في أندرويد */
private class SkiaGlyphPaint : GlyphPaint {
    private val paint = Paint().apply { isAntiAlias = true }
    private val font = SkFont(typeface, 16f).apply { isSubpixel = true }
    private val lines = HashMap<String, TextLine>()
    private var linesSize = -1f

    private fun line(text: String): TextLine {
        if (font.size != linesSize) { lines.values.forEach { it.close() }; lines.clear(); linesSize = font.size }
        return lines.getOrPut(text) { TextLine.make(text, font) }.also { if (lines.size > 400) { lines.values.forEach { l -> l.close() }; lines.clear() } }
    }

    override var textSize: Float get() = font.size; set(v) { font.size = v }
    override var color: Int get() = paint.color; set(v) { paint.color = v }
    override fun measure(text: String) = line(text).width
    /** حدود الحروف الفعلية (تُستعمل للأرقام وعلامات الوقف، ولا تحتاج تشكيلاً) */
    override fun bounds(text: String): FloatArray {
        val r = font.measureText(text, paint)
        return floatArrayOf(r.left, r.top, r.right, r.bottom)
    }
    override fun draw(canvas: Canvas, text: String, x: Float, y: Float) { canvas.nativeCanvas.drawTextLine(line(text), x, y, paint) }
}

actual fun mushafPaint(): GlyphPaint = SkiaGlyphPaint()

actual val QuranFont: FontFamily by lazy { FontFamily(Font("amiri_quran", fontBytes)) }
