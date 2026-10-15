package com.osatks.mizan.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/** النجمة الزخرفية تدور ببطء شديد في خلفية الرؤوس */
@Composable
fun SlowStar(modifier: Modifier, color: Color, periodMs: Int = 60_000, stroke: Float = 3f) {
    val t = rememberInfiniteTransition(label = "star")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "angle")
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
    val t = rememberInfiniteTransition(label = "backdrop")
    val angle = t.animateFloat(0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    return drawBehind {
        val p = anchor.align(IntSize.Zero, IntSize(size.width.toInt(), size.height.toInt()), layoutDirection)
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
        val c = Offset(p.x + sign * dx.toPx(), p.y + dy.toPx())
        rotate(if (Motion.reduced) 0f else angle.value, c) { drawStar8(c, radius.toPx(), color, stroke) }
    }
}
