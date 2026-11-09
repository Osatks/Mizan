package com.osatks.mizan.ui.quran

import androidx.compose.ui.unit.em
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep

/** نص قرآني: يُكتب من اليمين دائماً مهما كانت لغة الواجهة */
fun quranStyle(size: Int, align: TextAlign = TextAlign.Start) = TextStyle(
    fontFamily = QuranFont, fontSize = size.sp, lineHeight = (size * 2.05f).sp,
    textDirection = TextDirection.Rtl, textAlign = align,
)

/**
 * علامة نهاية الآية: نجمة ثمانية ذهبية بداخلها رقم الآية بالأرقام المشرقية.
 * modifier يحدد المقاس (size في القوائم، fillMaxSize داخل نص المصحف)، و numberSize حجم الرقم.
 */
@Composable
fun AyahMarker(n: Int, modifier: Modifier = Modifier.size(30.dp), numberSize: TextUnit = 11.sp) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paint = remember { mushafPaint() }
    val digits = arabicDigits(n)
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        drawStar8(c, size.minDimension / 2 - 1.5f, GoldDeep, 1.5.dp.toPx(), rings = false)
        drawCircle(Gold.copy(alpha = .14f), size.minDimension * 0.33f, c)
        // الرقم من حدود حروفه الفعلية فيقع في وسط النجمة تماماً (خط المصحف يترك فوق حروفه مساحة كبيرة)
        paint.textSize = (if (n >= 100) numberSize * 0.82f else numberSize).toPx()
        paint.color = ink.toArgb()
        val b = paint.bounds(digits)
        drawIntoCanvas { paint.draw(it, digits, c.x - (b[0] + b[2]) / 2, c.y - (b[1] + b[3]) / 2) }
    }
}

/** علامة نهاية الآية برقمها داخل نص Compose: id ثابت ورقم الآية نصاً بديلاً، ويرسمها [ayahEndContent] */
const val AYAH_END_ID = "ayahEnd"

fun AnnotatedString.Builder.appendAyahEnd(ayah: Int) = appendInlineContent(AYAH_END_ID, "$ayah")

/** مكان علامة الآية في النص بعرض الحلقة، تُرسم فيه الحلقة ورقمها في وسطها كما في صفحة المصحف */
@Composable
fun ayahEndContent(fontSize: TextUnit, ring: Color, number: Color): Map<String, InlineTextContent> {
    val paint = remember { mushafPaint() }
    // الخريطة نفسها ما لم يتغير المقاس أو اللون، فلا تُعاد تهيئة نص الآية مع كل تركيب
    return remember(fontSize, ring, number) { mapOf(
        // المكان المحجوز فوق خط الأساس تماماً، فتجلس الحلقة على سطر الآية كما يضعها الخط (ويتدلى أسفلها تحته)
        AYAH_END_ID to InlineTextContent(Placeholder(1.32.em, 0.72.em, PlaceholderVerticalAlign.AboveBaseline)) { alt ->
            Canvas(Modifier.fillMaxSize()) {
                val px = fontSize.toPx()
                val baseline = size.height
                drawIntoCanvas { drawAyahEnd(it, paint, alt.toIntOrNull() ?: 0, px * 0.02f, baseline, px, ring.toArgb(), number.toArgb()) }
            }
        }
    ) }
}
