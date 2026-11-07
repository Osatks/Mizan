package com.osatks.mizan.ui.quran

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.osatks.mizan.platform.AndroidContext

private const val FONT = "fonts/amiri_quran.ttf"

private val typeface: Typeface by lazy { runCatching { Typeface.createFromAsset(AndroidContext.app.assets, FONT) }.getOrDefault(Typeface.DEFAULT) }

private class AndroidGlyphPaint(face: Typeface = com.osatks.mizan.ui.quran.typeface) : GlyphPaint {
    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply { typeface = face }
    private val r = Rect()
    override var textSize: Float get() = p.textSize; set(v) { p.textSize = v }
    override var color: Int get() = p.color; set(v) { p.color = v }
    override fun measure(text: String) = p.measureText(text)
    override fun bounds(text: String): FloatArray {
        p.getTextBounds(text, 0, text.length, r)
        return floatArrayOf(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat())
    }
    override fun draw(canvas: Canvas, text: String, x: Float, y: Float) = canvas.nativeCanvas.drawText(text, x, y, p)
}

actual fun mushafPaint(): GlyphPaint = AndroidGlyphPaint()

actual val QuranFont: FontFamily by lazy { FontFamily(Font(FONT, AndroidContext.app.assets)) }

/** خطوط الملفات المقروءة (آخر ما استُعمل منها)؛ خط الصفحة يُقرأ مرة لكل مرور عليها */
private val faces = LinkedHashMap<String, Typeface>()

private fun face(key: String, make: () -> Typeface?): Typeface? = synchronized(faces) {
    faces.remove(key)?.also { faces[key] = it } ?: runCatching(make).getOrNull()?.also {
        faces[key] = it
        while (faces.size > 24) faces.remove(faces.keys.first())
    }
}

actual fun filePaint(path: String): GlyphPaint? = face(path) { Typeface.createFromFile(path) }?.let { AndroidGlyphPaint(it) }

actual fun assetPaint(asset: String): GlyphPaint? = face("asset:$asset") { Typeface.createFromAsset(AndroidContext.app.assets, asset) }?.let { AndroidGlyphPaint(it) }
