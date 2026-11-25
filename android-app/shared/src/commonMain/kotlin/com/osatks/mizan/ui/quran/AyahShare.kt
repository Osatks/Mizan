package com.osatks.mizan.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.widget.WidgetColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * مشاركة الآية صورةً: بطاقة بخط المصحف (اسم السورة، والآية بعلامة نهايتها ورقمها، ومعناها بلغة الواجهة إن شاء المستخدم،
 * ومرجعها واسم التطبيق) بثلاثة ألوان: لون التطبيق، وورق المصحف، والليل. المعاينة هي الصورة نفسها: تُسجَّل في طبقة رسم
 * وتُحفظ PNG وتُشارَك بقائمة المشاركة في النظام (حفظ في الصور، والتطبيقات). وزر «نص» يشاركها نصاً كما كانت.
 */

enum class CardStyle { EMERALD, PAPER, NIGHT }

internal class CardColors(val top: Color, val bottom: Color, val text: Color, val sub: Color, val gold: Color)

internal fun cardColors(style: CardStyle, accent: String): CardColors = when (style) {
    CardStyle.EMERALD -> WidgetColors.of(accent).let {
        CardColors(Color(it.hero1), Color(it.hero2), Color.White, Color.White.copy(alpha = .82f), Color(0xFFE8C15A))
    }
    CardStyle.PAPER -> CardColors(Color(0xFFFBF6EA), Color(0xFFF0E5CB), Color(0xFF1F2A24), Color(0xFF4A5650), Color(0xFFAD8219))
    CardStyle.NIGHT -> CardColors(Color(0xFF1B2D28), Color(0xFF0A1210), Color(0xFFF3EEDF), Color(0xFFF3EEDF).copy(alpha = .76f), Color(0xFFE8C15A))
}

/**
 * لوحة المشاركة في شاشة القراءة: المعاينة وخياراتها وزرّا الصورة والنص (AyahShare.android.kt)؛ وفي iOS ورقة النظام
 * بمعاينة البطاقة نفسها (ترسمها Compose خارج الشاشة) وقائمة المشاركة (NativeQuranSheets.kt)
 */
@Composable
internal expect fun AyahShareContent(index: Int, text: Array<String>, onDone: () -> Unit)

/** أرقام عربية مشرقية لرقم الآية داخل علامة نهايتها (۝) */
private fun arabicDigits(n: Int): String = n.toString().map { '٠' + (it - '0') }.joinToString("")

/**
 * البطاقة: 4:5 للآيات القصيرة، وأطول للطويلة حتى يبقى الخط مقروءاً. الآية تملأ ما بين اسم السورة والمرجع بأكبر خط يسعها،
 * والمعنى تحتها بخط أصغر.
 */
@Composable
internal fun AyahCard(
    ayahText: String, surah: Int, ayah: Int, ref: String, meaning: String?, style: CardStyle, brand: String, modifier: Modifier,
) {
    val c = cardColors(style, AppSettings.accent)
    val weight = ayahText.length + (meaning?.length ?: 0) * 0.55f
    val ratio = when {
        weight < 320 -> 4f / 5f
        weight < 700 -> 2f / 3f
        else -> 9f / 16f
    }
    val body = "$ayahText ۝${arabicDigits(ayah)}"
    Box(
        modifier.aspectRatio(ratio).background(Brush.verticalGradient(listOf(c.top, c.bottom))),
    ) {
        // الزخرفة: نجمة ثمانية خافتة في الزاوية، وإطار ذهبي رفيع
        Canvas(Modifier.fillMaxSize()) { ornament(c.gold) }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "سُورَةُ ${QuranMeta.NAMES_AR[surah - 1]}",
                style = TextStyle(fontFamily = QuranFont, fontSize = 17.sp, color = c.gold, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl),
                maxLines = 1,
            )
            Spacer(Modifier.height(10.dp))
            FitText(
                body, TextStyle(fontFamily = QuranFont, color = c.text, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl),
                maxSize = 34f, minSize = 12f, lineHeight = 1.85f, modifier = Modifier.fillMaxWidth().weight(if (meaning != null) 0.62f else 1f),
            )
            if (meaning != null) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(36.dp).height(1.dp).background(c.gold.copy(alpha = .55f)))
                Spacer(Modifier.height(10.dp))
                FitText(
                    meaning, TextStyle(color = c.sub, textAlign = TextAlign.Center, textDirection = TextDirection.Content),
                    maxSize = 16f, minSize = 9f, lineHeight = 1.4f, modifier = Modifier.fillMaxWidth().weight(0.38f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.gold.copy(alpha = .28f)))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ref, Modifier.weight(1f), style = TextStyle(fontSize = 11.sp, color = c.sub, fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Canvas(Modifier.size(10.dp)) { star(c.gold) }
                Spacer(Modifier.width(5.dp))
                Text(brand, style = TextStyle(fontSize = 12.sp, color = c.gold, fontWeight = FontWeight.Bold), maxLines = 1)
            }
        }
    }
}

/** نص بأكبر خط بين [minSize] و [maxSize] يسعه مكانه، في وسطه */
@Composable
internal fun FitText(text: String, style: TextStyle, maxSize: Float, minSize: Float, lineHeight: Float, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val size = remember(text, style, w, h, density) {
            if (w <= 0 || h <= 0 || h == Constraints.Infinity) return@remember minSize
            var lo = minSize
            var hi = maxSize
            fun fits(sz: Float) = measurer.measure(
                text, style.copy(fontSize = sz.sp, lineHeight = (sz * lineHeight).sp), constraints = Constraints(maxWidth = w), density = density,
            ).size.height <= h
            if (fits(hi)) return@remember hi
            repeat(8) {
                val mid = (lo + hi) / 2
                if (fits(mid)) lo = mid else hi = mid
            }
            lo
        }
        Text(text, style = style.copy(fontSize = size.sp, lineHeight = (size * lineHeight).sp), overflow = TextOverflow.Ellipsis)
    }
}

/** إطار ذهبي رفيع داخل البطاقة، ونجمة ثمانية خافتة في زاويتها العليا */
internal fun DrawScope.ornament(gold: Color) {
    val inset = 12.dp.toPx()
    val r = 14.dp.toPx()
    drawRoundRect(
        gold.copy(alpha = .45f), topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r), style = Stroke(width = 1.dp.toPx()),
    )
    val s = size.width * 0.62f
    val cx = size.width - s * 0.2f
    val cy = s * 0.16f
    for (k in listOf(0.5f, 0.36f)) {
        val rr = s * k
        val half = rr / sqrt(2f)
        drawRect(gold.copy(alpha = .10f), Offset(cx - half, cy - half), Size(2 * half, 2 * half), style = Stroke(1.dp.toPx()))
        val p = Path().apply {
            moveTo(cx, cy - rr); lineTo(cx + rr, cy); lineTo(cx, cy + rr); lineTo(cx - rr, cy); close()
        }
        drawPath(p, gold.copy(alpha = .10f), style = Stroke(1.dp.toPx()))
    }
}

/** نجمة ثمانية ممتلئة صغيرة (رمز ميزان) */
internal fun DrawScope.star(gold: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    val r = size.minDimension / 2
    val p = Path()
    for (i in 0 until 16) {
        val a = PI / 8 * i - PI / 2
        val rr = if (i % 2 == 0) r else r * 0.62f
        val x = c.x + (rr * cos(a)).toFloat()
        val y = c.y + (rr * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, gold)
}
