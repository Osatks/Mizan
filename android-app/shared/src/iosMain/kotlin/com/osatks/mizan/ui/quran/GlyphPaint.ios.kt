package com.osatks.mizan.ui.quran

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.osatks.mizan.platform.Lock
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.locked
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font as SkFont
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

private const val FONT = "fonts/amiri_quran.ttf"
private val fontBytes: ByteArray by lazy { Platform.asset(FONT) ?: ByteArray(0) }
private val typeface: Typeface? by lazy { FontMgr.default.makeFromData(Data.makeFromBytes(fontBytes)) }

private data class ShapeKey(val size: Float, val text: String, val font: String = "")

/**
 * ذاكرة مشتركة بين كل الأقلام وكل الخيوط: الكلمة تُشكَّل (HarfBuzz) مرة واحدة لكل مقاس.
 * تشكيل خط المصحف مكلف، فتُحضَّر كلمات الصفحة في الخلفية مع حسابها، ويبقى الرسم على الخيط الرئيسي نسخاً فقط.
 * جيلان بدل LRU كامل: ما يُستعمل من الجيل القديم ينتقل إلى الجديد، وما لم يُستعمل يُترك للمنظّف (لا يُغلق
 * يدوياً لأن خيطاً آخر قد يرسمه في اللحظة نفسها؛ الصورة المسجّلة تحتفظ بنسختها).
 */
private class SharedCache<V : Any>(private val limit: Int) {
    private val lock = Lock()
    private var fresh = HashMap<ShapeKey, V>()
    private var old = HashMap<ShapeKey, V>()

    private fun find(key: ShapeKey): V? = fresh[key] ?: old.remove(key)?.also { put(key, it) }
    private fun put(key: ShapeKey, v: V) {
        fresh[key] = v
        if (fresh.size > limit) { old = fresh; fresh = HashMap() }
    }

    fun get(key: ShapeKey, make: () -> V): V {
        val hit = lock.locked { find(key) }
        if (hit != null) return hit
        // التشكيل خارج القفل حتى لا ينتظر الخيط الرئيسي خيطاً يحضّر صفحة أخرى
        val made = make()
        return lock.locked { find(key) ?: made.also { put(key, it) } }
    }
}

private val lines = SharedCache<TextLine>(2500)
private val boxes = SharedCache<FloatArray>(600)

/** TextLine يشكّل الحروف (HarfBuzz) فتتصل الحروف العربية كما في أندرويد */
private class SkiaGlyphPaint(face: Typeface? = typeface, private val id: String = "") : GlyphPaint {
    private val paint = Paint().apply { isAntiAlias = true }
    private val font = SkFont(face, 16f).apply { isSubpixel = true }

    // رموز خطوط صفحات المصحف تتكرر بين الصفحات بأشكال مختلفة: المفتاح يحمل اسم الخط
    private fun line(text: String): TextLine = lines.get(ShapeKey(font.size, text, id)) { TextLine.make(text, font) }

    override var textSize: Float get() = font.size; set(v) { font.size = v }
    override var color: Int get() = paint.color; set(v) { paint.color = v }
    override fun measure(text: String) = line(text).width
    /** حدود الحروف الفعلية (تُستعمل للأرقام وعلامات الوقف، ولا تحتاج تشكيلاً) */
    override fun bounds(text: String): FloatArray = boxes.get(ShapeKey(font.size, text, id)) {
        val r = font.measureText(text, paint)
        floatArrayOf(r.left, r.top, r.right, r.bottom)
    }
    override fun draw(canvas: Canvas, text: String, x: Float, y: Float) { canvas.nativeCanvas.drawTextLine(line(text), x, y, paint) }
}

actual fun mushafPaint(): GlyphPaint = SkiaGlyphPaint()

actual val QuranFont: FontFamily by lazy { FontFamily(Font("amiri_quran", fontBytes)) }

/** خطوط الملفات المقروءة (آخر ما استُعمل منها) */
private val faceLock = Lock()
private val faces = LinkedHashMap<String, Typeface>()

private fun face(key: String, bytes: () -> ByteArray?): Typeface? {
    val hit = faceLock.locked { faces.remove(key)?.also { faces[key] = it } }
    if (hit != null) return hit
    val data = bytes() ?: return null
    val made = runCatching { FontMgr.default.makeFromData(Data.makeFromBytes(data)) }.getOrNull() ?: return null
    faceLock.locked {
        faces[key] = made
        while (faces.size > 24) faces.remove(faces.keys.first())
    }
    return made
}

actual fun filePaint(path: String): GlyphPaint? = face(path) { com.osatks.mizan.platform.Fs.read(path) }?.let { SkiaGlyphPaint(it, path) }

actual fun assetPaint(asset: String): GlyphPaint? = face("asset:$asset") { Platform.asset(asset) }?.let { SkiaGlyphPaint(it, asset) }
