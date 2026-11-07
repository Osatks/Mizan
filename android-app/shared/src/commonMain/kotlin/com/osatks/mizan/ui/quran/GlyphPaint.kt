package com.osatks.mizan.ui.quran

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.font.FontFamily

/**
 * قلم يرسم نص المصحف مباشرة على اللوحة بخط المصحف (أخف بكثير من عنصر Text لكل كلمة).
 * أندرويد: TextPaint، و iOS: Skia مع تشكيل الحروف العربية (HarfBuzz).
 */
interface GlyphPaint {
    var textSize: Float
    /** اللون بصيغة ARGB */
    var color: Int
    fun measure(text: String): Float
    /** حدود الحروف الفعلية [يسار، أعلى، يمين، أسفل] من نقطة البداية وخط الأساس */
    fun bounds(text: String): FloatArray
    fun draw(canvas: Canvas, text: String, x: Float, y: Float)
}

/** قلم جديد بخط المصحف (Amiri Quran) */
expect fun mushafPaint(): GlyphPaint

/** خط المصحف لعناصر Compose (Amiri Quran، رخصة SIL OFL) */
expect val QuranFont: FontFamily

/** قلم بخط من ملف في الجهاز (خط صفحة من مصحف المدينة)؛ null إن تعذّرت قراءته */
expect fun filePaint(path: String): GlyphPaint?

/** قلم بخط من assets (ترويسات السور وخط البسملة في مصحف المدينة)؛ null إن تعذّرت قراءته */
expect fun assetPaint(asset: String): GlyphPaint?
