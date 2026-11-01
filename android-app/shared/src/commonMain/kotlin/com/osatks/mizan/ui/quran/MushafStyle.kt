package com.osatks.mizan.ui.quran

import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.LocalDark

/**
 * ألوان المصحف: ورق دافئ وحبر وذهب التذهيب. في الوضع الداكن ورق ليلي دافئ وحروف عاجية،
 * حتى تبقى القراءة مريحة للعين في الحالين.
 */
@Immutable
class MushafPalette(
    val paper: Color, val paperEdge: Color, val ink: Color, val muted: Color,
    val gold: Color, val goldDeep: Color, val fill: Color, val fillDeep: Color,
    val marker: Color, val select: Color, val press: Color,
    /** لون ما حول الصفحة؛ Unspecified = لون التطبيق */
    val backdrop: Color = Color.Unspecified,
)

private val LightPalette = MushafPalette(
    paper = Color(0xFFFFFCF4), paperEdge = Color(0xFFF5ECD6), ink = Color(0xFF1E1A14), muted = Color(0xFF7A6A4C),
    gold = Color(0xFFCDA349), goldDeep = Color(0xFF9C7420), fill = Color(0xFFF7EBCB), fillDeep = Color(0xFFEBD49A),
    marker = Color(0xFF8E6A1C), select = Color(0x55E7CB7E), press = Color(0x33CDA349),
)

private val DarkPalette = MushafPalette(
    paper = Color(0xFF1E1C17), paperEdge = Color(0xFF151410), ink = Color(0xFFEDE6D3), muted = Color(0xFFB3A788),
    gold = Color(0xFFB8933F), goldDeep = Color(0xFFDDB860), fill = Color(0xFF2C261A), fillDeep = Color(0xFF3A311E),
    marker = Color(0xFFD9B45E), select = Color(0x33E0B54A), press = Color(0x22E0B54A),
)

/** ورق دافئ أصفر كالكتب القديمة، أهدأ على العين من الأبيض */
private val SepiaPalette = MushafPalette(
    paper = Color(0xFFF3E6C8), paperEdge = Color(0xFFE6D2A6), ink = Color(0xFF3A2C17), muted = Color(0xFF7A6440),
    gold = Color(0xFFB88B36), goldDeep = Color(0xFF8A6420), fill = Color(0xFFEAD6A6), fillDeep = Color(0xFFDEC28A),
    marker = Color(0xFF7D5A18), select = Color(0x55C9A55A), press = Color(0x33B88B36),
)

/** القراءة الليلية: ورق شبه أسود وحروف كهرمانية دافئة، بلا ضوء أزرق ولا بياض يبهر العين في الظلام */
private val NightPalette = MushafPalette(
    paper = Color(0xFF120E08), paperEdge = Color(0xFF0B0805), ink = Color(0xFFD9B98A), muted = Color(0xFF9C8463),
    gold = Color(0xFF8E6E34), goldDeep = Color(0xFFB8924A), fill = Color(0xFF1E170D), fillDeep = Color(0xFF2A2012),
    marker = Color(0xFFC29B52), select = Color(0x33B8924A), press = Color(0x22B8924A), backdrop = Color(0xFF070503),
)

/** لون ورق القراءة الذي اختاره المستخدم ("" = حسب مظهر التطبيق) */
fun readingPalette(tone: String): MushafPalette? = when (tone) {
    "sepia" -> SepiaPalette
    "night" -> NightPalette
    else -> null
}

/** يُوفَّر داخل صفحة القراءة فقط، فتبقى ورقة التفسير والقوائم بألوان التطبيق */
val LocalReadingPalette = staticCompositionLocalOf<MushafPalette?> { null }

@Composable
fun mushafPalette(): MushafPalette = LocalReadingPalette.current ?: if (LocalDark.current) DarkPalette else LightPalette

/** الأجزاء بأسمائها كما تُكتب في رأس صفحة المصحف */
val JUZ_ORDINALS = arrayOf(
    "الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع", "الثامن", "التاسع", "العاشر",
    "الحادي عشر", "الثاني عشر", "الثالث عشر", "الرابع عشر", "الخامس عشر", "السادس عشر", "السابع عشر", "الثامن عشر",
    "التاسع عشر", "العشرون", "الحادي والعشرون", "الثاني والعشرون", "الثالث والعشرون", "الرابع والعشرون",
    "الخامس والعشرون", "السادس والعشرون", "السابع والعشرون", "الثامن والعشرون", "التاسع والعشرون", "الثلاثون",
)

/** ورقة المصحف: تدرّج خفيف من الوسط إلى الحواف كالورق المصقول */
fun DrawScope.drawPaper(p: MushafPalette) {
    drawRect(Brush.radialGradient(listOf(p.paper, p.paper, p.paperEdge), center = center, radius = size.maxDimension * 0.75f))
}

/**
 * ظل الكعب في الكتاب المفتوح: الورقة تنحني قليلاً عند الخياطة بين الصفحتين.
 * [innerAtLeft]: الكعب في الطرف الأيسر من هذه الصفحة (الصفحة اليمنى)، وإلا ففي طرفها الأيمن.
 */
fun DrawScope.drawSpine(innerAtLeft: Boolean) {
    val w = 26.dp.toPx().coerceAtMost(size.width / 4)
    val shade = Color.Black.copy(alpha = .09f)
    val x0 = if (innerAtLeft) 0f else size.width - w
    val brush = if (innerAtLeft) Brush.horizontalGradient(listOf(shade, Color.Transparent), x0, x0 + w)
    else Brush.horizontalGradient(listOf(Color.Transparent, shade), x0, x0 + w)
    drawRect(brush, Offset(x0, 0f), Size(w, size.height))
}

/** المسافة بين أسفل الورقة وخطَّي الإطار السفليين: يجلس رقم الصفحة عليهما في وسطهما تماماً */
val FRAME_FOOT = 13.dp

/** إطار الصفحة: خط ذهبي عريض وآخر رفيع بداخله، وفي زوايا الإطار الداخلي نجوم ثمانية */
fun DrawScope.drawPageFrame(p: MushafPalette) {
    val o = 7.dp.toPx(); val i = 11.dp.toPx(); val foot = FRAME_FOOT.toPx()
    drawRoundRect(p.goldDeep.copy(alpha = .8f), Offset(o, o), Size(size.width - 2 * o, size.height - 2 * o - foot), CornerRadius(12.dp.toPx()), style = Stroke(1.6.dp.toPx()))
    drawRoundRect(p.gold.copy(alpha = .7f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i - foot), CornerRadius(8.dp.toPx()), style = Stroke(0.8.dp.toPx()))
    val r = 5.dp.toPx(); val c = i + 1.dp.toPx(); val bottom = size.height - foot - c
    for (pt in listOf(Offset(c, c), Offset(size.width - c, c), Offset(c, bottom), Offset(size.width - c, bottom))) {
        drawCircle(p.paper, r * 0.9f, pt)
        drawStar8(pt, r, p.goldDeep, 1.dp.toPx(), rings = false)
        drawCircle(p.goldDeep, r * 0.22f, pt)
    }
}

/** مركز رقم الصفحة من أسفل الورقة: منتصف ما بين خطَّي الإطار السفليين */
val MEDALLION_CENTER = 9.dp + FRAME_FOOT
val MEDALLION_SIZE = 32.dp

/** شريط علامة ذهبي يتدلى من أعلى الصفحة إذا كان فيها آية محفوظة؛ amount من 0 إلى 1 لحركة ظهوره */
fun DrawScope.drawRibbon(p: MushafPalette, amount: Float) {
    val w = 14.dp.toPx(); val h = 40.dp.toPx() * amount; val notch = 6.dp.toPx() * amount
    val x = size.width / 2 - w / 2
    val ribbon = Path().apply { moveTo(x, 0f); lineTo(x + w, 0f); lineTo(x + w, h); lineTo(x + w / 2, h - notch); lineTo(x, h); close() }
    drawPath(ribbon, Brush.verticalGradient(listOf(p.goldDeep, p.gold), 0f, h.coerceAtLeast(1f)))
    drawLine(p.paper.copy(alpha = .45f), Offset(x + w * 0.28f, 0f), Offset(x + w * 0.28f, (h - notch * 1.4f).coerceAtLeast(0f)), 0.8.dp.toPx())
}

/** خط فاصل رفيع في وسطه معيّن صغير */
fun DrawScope.drawRule(y: Float, x0: Float, x1: Float, p: MushafPalette) {
    val cx = (x0 + x1) / 2; val d = 3.5.dp.toPx(); val gap = 7.dp.toPx()
    drawLine(p.gold.copy(alpha = .7f), Offset(x0, y), Offset(cx - gap, y), 0.8.dp.toPx())
    drawLine(p.gold.copy(alpha = .7f), Offset(cx + gap, y), Offset(x1, y), 0.8.dp.toPx())
    val diamond = Path().apply { moveTo(cx, y - d); lineTo(cx + d, y); lineTo(cx, y + d); lineTo(cx - d, y); close() }
    drawPath(diamond, p.goldDeep)
}

/**
 * ترويسة السورة كما في المصاحف: إطار مذهّب مزدوج، وفي طرفيه ميداليتان بنجمة ثمانية،
 * وفي وسطه لوح مدبّب الطرفين يُكتب فيه اسم السورة.
 * يُرجع عرض اللوح المتاح للاسم.
 */
fun DrawScope.drawCartouche(left: Float, top: Float, right: Float, bottom: Float, p: MushafPalette): Float {
    val h = bottom - top; val w = right - left
    val r = h * 0.2f
    drawRoundRect(Brush.verticalGradient(listOf(p.fill, p.fillDeep), top, bottom), Offset(left, top), Size(w, h), CornerRadius(r))
    drawRoundRect(p.goldDeep, Offset(left, top), Size(w, h), CornerRadius(r), style = Stroke(1.4.dp.toPx()))
    val inset = 3.dp.toPx()
    drawRoundRect(p.gold, Offset(left + inset, top + inset), Size(w - 2 * inset, h - 2 * inset), CornerRadius((r - inset).coerceAtLeast(1f)), style = Stroke(0.7.dp.toPx()))
    val cy = top + h / 2
    val mr = h * 0.33f
    val ends = listOf(left + h * 0.56f, right - h * 0.56f)
    for (cx in ends) {
        val c = Offset(cx, cy)
        drawCircle(p.paper, mr, c)
        drawCircle(p.goldDeep, mr, c, style = Stroke(1.2.dp.toPx()))
        drawStar8(c, mr * 0.74f, p.gold, 1.dp.toPx(), rings = false)
        drawCircle(p.goldDeep, mr * 0.16f, c)
    }
    // لوح الاسم بين الميداليتين، وبينه وبينهما نقاط ذهبية
    val x0 = left + h * 1.08f; val x1 = right - h * 1.08f
    val ph = h * 0.62f; val tip = ph * 0.45f
    val plate = Path().apply {
        moveTo(x0, cy); lineTo(x0 + tip, cy - ph / 2); lineTo(x1 - tip, cy - ph / 2); lineTo(x1, cy)
        lineTo(x1 - tip, cy + ph / 2); lineTo(x0 + tip, cy + ph / 2); close()
    }
    drawPath(plate, p.paper)
    drawPath(plate, p.goldDeep, style = Stroke(1.dp.toPx()))
    val dot = 1.4.dp.toPx()
    drawCircle(p.goldDeep, dot, Offset((ends[0] + mr + x0) / 2, cy))
    drawCircle(p.goldDeep, dot, Offset((ends[1] - mr + x1) / 2, cy))
    return cartoucheNameWidth(w, h)
}

/** عرض لوح الاسم في ترويسة عرضها w وارتفاعها h (بين الميداليتين، بلا طرفيه المدبّبين) */
fun cartoucheNameWidth(w: Float, h: Float): Float = w - h * 2.16f - h * 0.62f * 0.45f * 2

/** ترويسة السورة خارج صفحة المصحف (عرض آية آية) */
@Composable
fun SurahCartouche(arabicName: String, modifier: Modifier = Modifier, height: Dp = 58.dp) {
    val p = mushafPalette()
    Box(modifier.fillMaxWidth().height(height).drawBehind { drawCartouche(0f, 0f, size.width, size.height, p) }, contentAlignment = Alignment.Center) {
        Text(
            "سُورَةُ $arabicName", Modifier.offset(y = -(height * 0.06f)), textAlign = TextAlign.Center, maxLines = 1, color = p.goldDeep,
            style = TextStyle(
                fontFamily = QuranFont, fontSize = (height.value * 0.4f).sp, lineHeight = (height.value * 0.6f).sp,
                textDirection = TextDirection.Rtl,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

/**
 * رقم الصفحة في ميدالية تجلس على إطار الصفحة في منتصفه تماماً. تمتلئ بالذهب حين تُحسب الصفحة مقروءة في الختمة.
 * الرقم يُرسم من حدود حروفه الفعلية فيقع في وسط الدائرة تماماً (خط المصحف يترك فوق حروفه مساحة كبيرة).
 */
@Composable
fun PageMedallion(page: Int, read: Boolean, modifier: Modifier = Modifier) {
    val p = mushafPalette()
    val paint = remember { mushafPaint() }
    val digits = arabicDigits(page)
    val fill by animateFloatAsState(if (read) 1f else 0f, tween(600), label = "medallion")
    Canvas(modifier.size(MEDALLION_SIZE)) {
        val r = size.minDimension / 2 - 1.dp.toPx()
        drawCircle(p.paper, r)
        drawCircle(p.fillDeep.copy(alpha = fill), r)
        drawCircle(p.goldDeep, r, style = Stroke(1.3.dp.toPx()))
        drawStar8(center, r * 0.86f, p.gold.copy(alpha = .4f + .45f * fill), 0.9.dp.toPx(), rings = false)
        paint.textSize = (if (page >= 100) 12.5f else 14f).sp.toPx()
        paint.color = p.ink.toArgb()
        val b = paint.bounds(digits)
        drawIntoCanvas { paint.draw(it, digits, center.x - (b[0] + b[2]) / 2, center.y - (b[1] + b[3]) / 2) }
    }
}

/** فاصل مزخرف بين الآيات في عرض آية آية */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    val p = mushafPalette()
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val y = size.height / 2
        val cx = size.width / 2
        val brushL = Brush.horizontalGradient(listOf(Color.Transparent, p.gold.copy(alpha = .6f)), 0f, cx - 10.dp.toPx())
        val brushR = Brush.horizontalGradient(listOf(p.gold.copy(alpha = .6f), Color.Transparent), cx + 10.dp.toPx(), size.width)
        drawLine(brushL, Offset(size.width * 0.12f, y), Offset(cx - 10.dp.toPx(), y), 0.8.dp.toPx())
        drawLine(brushR, Offset(cx + 10.dp.toPx(), y), Offset(size.width * 0.88f, y), 0.8.dp.toPx())
        drawStar8(Offset(cx, y), 5.dp.toPx(), p.goldDeep, 0.9.dp.toPx(), rings = false)
    }
}
