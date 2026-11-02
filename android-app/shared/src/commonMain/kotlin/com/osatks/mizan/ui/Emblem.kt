package com.osatks.mizan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** نجمة ثمانية (ربع الحزب): مربعان متراكبان، شعار ميزان الزخرفي */
fun DrawScope.drawStar8(center: Offset, radius: Float, color: Color, stroke: Float, rings: Boolean = true) {
    val path = Path()
    for (sq in 0..1) {
        for (i in 0..3) {
            val a = (sq * 45.0 + i * 90.0 - 90.0) * PI / 180.0
            val x = center.x + radius * cos(a).toFloat()
            val y = center.y + radius * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
    drawPath(path, color, style = Stroke(stroke))
    if (rings) {
        drawCircle(color, radius * 0.52f, center, style = Stroke(stroke * 0.8f))
        drawCircle(color, radius * 0.18f, center, style = Stroke(stroke * 0.8f))
    }
}

/**
 * زاوية الدوران الزخرفي. الدورة السريعة (مؤشر التحميل) تتقدم مع كل إطار. أما الدورة البطيئة (دقيقة ونصف مثلاً)
 * فتتقدم بخطوات صغيرة جداً (نحو 0.15°) بضع عشرات من المرات في الثانية: لا يرى الفرق أحد، لكنها لا تجبر
 * الشاشة كلها على إعادة الرسم 120 مرة في الثانية (ProMotion) ما دامت الصفحة مفتوحة، فتبقى الإطارات
 * للسحب والتمرير. ولا تدور أصلاً إذا كانت الحركات مُزالة من إعدادات النظام.
 */
@Composable
private fun rotationAngle(periodMs: Int, label: String): State<Float> {
    if (periodMs < 10_000) {
        val t = rememberInfiniteTransition(label = label)
        return t.animateFloat(0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    }
    val angle = remember { mutableFloatStateOf(0f) }
    // الدورة البطيئة لا تعمل في iOS: كانت تعيد رسم الواجهة كلها عشرات المرات في الثانية بلا توقف (Compose في iOS
    // يرسم المشهد كله مع كل تغيير)، فتنافس حركة التنقل والتمرير على الإطارات. ولا في صفحة حية مغطاة بغيرها
    val active = LocalPageActive.current
    LaunchedEffect(periodMs, active) {
        if (Motion.reduced || IsIos || !active) return@LaunchedEffect
        val step = (periodMs / 2400L).coerceIn(16L, 60L)
        val start = withFrameMillis { it }
        while (true) {
            delay(step)
            withFrameMillis { now -> angle.floatValue = ((now - start) % periodMs) * 360f / periodMs }
        }
    }
    return angle
}

/** النجمة الزخرفية تدور ببطء شديد في خلفية الرؤوس */
@Composable
fun SlowStar(modifier: Modifier, color: Color, periodMs: Int = 60_000, stroke: Float = 3f) {
    val angle by rotationAngle(periodMs, "star")
    Canvas(modifier.graphicsLayer { rotationZ = if (Motion.reduced) 0f else angle }) {
        drawStar8(Offset(size.width / 2, size.height / 2), size.minDimension / 2 - stroke, color, stroke)
    }
}

/**
 * النجمة الدوّارة خلفيةً للعنصر نفسه: تُرسم خلف المحتوى ولا تؤثر في مقاسه.
 * anchor زاوية الارتكاز (تنعكس في الواجهات العربية)، و dx/dy إزاحة المركز نحو الخارج.
 */
@Composable
fun Modifier.starBackdrop(color: Color, radius: Dp, anchor: Alignment, dx: Dp, dy: Dp, periodMs: Int, stroke: Float = 3f): Modifier {
    val angle = rotationAngle(periodMs, "backdrop")
    return drawBehind {
        val p = anchor.align(IntSize.Zero, IntSize(size.width.toInt(), size.height.toInt()), layoutDirection)
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        val c = Offset(p.x + sign * dx.toPx(), p.y + dy.toPx())
        rotate(if (Motion.reduced) 0f else angle.value, c) { drawStar8(c, radius.toPx(), color, stroke) }
    }
}
