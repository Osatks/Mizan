package com.osatks.mizan.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.ui.theme.LocalDark
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** يعمل التطبيق على iOS (لمسات خاصة بالنظام: شكل المفاتيح، أحجام الخطوط، سحب الرجوع) */
val IsIos: Boolean = Platform.name == "ios"

/**
 * مصدر التمويه على مستوى التطبيق: محتوى الصفحات (NavHost) الذي يظهر مموَّهاً خلف شريط التنقل والأوراق السفلية.
 * بلا مصدر (خارج التطبيق، أو في صفحة المصحف حيث يُعطَّل المصدر) تُرسم الطبقة الزجاجية لوناً شبه معتم.
 */
val LocalAppHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * الزجاج: تمويه حقيقي لما خلف العنصر (RenderEffect في أندرويد 12+، و Skia في iOS) مع طبقة لون شفافة من لون السطح،
 * فيتبع الوضع الداكن ولون التطبيق. في أندرويد الأقدم يُرسم [fallback] بدل التمويه.
 * نصف القطر محدود (24–32dp) والتمويه يُحسب بثلث الدقة، فالكلفة صغيرة حتى بـ120 إطاراً في الثانية.
 */
@Composable
@ReadOnlyComposable
fun glassStyle(tint: Float, blur: Dp = 24.dp, fallback: Float = .96f): HazeStyle {
    val c = MaterialTheme.colorScheme
    return HazeStyle(
        backgroundColor = c.background,
        tints = listOf(HazeTint(c.surface.copy(alpha = tint))),
        blurRadius = blur,
        noiseFactor = 0f,
        fallbackTint = HazeTint(c.surface.copy(alpha = fallback)),
    )
}

/** ألوان الحافة اللامعة واللمعة العلوية للزجاج */
class GlassRim(val top: Color, val bottom: Color, val sheen: Color)

@Composable
@ReadOnlyComposable
fun glassRim(strength: Float = 1f): GlassRim {
    val dark = LocalDark.current
    return GlassRim(
        top = Color.White.copy(alpha = (if (dark) .24f else .95f) * strength),
        bottom = Color.White.copy(alpha = (if (dark) .05f else .40f) * strength),
        sheen = Color.White.copy(alpha = (if (dark) .06f else .30f) * strength),
    )
}

/**
 * سطح زجاجي بشكل [shape]: تمويه ما خلفه من [state]، ولمعة خفيفة في أعلاه، وحافة مضيئة من الأعلى تخفت نحو الأسفل
 * (كضوء ينعكس على حافة الزجاج). إن لم يوجد مصدر تمويه يُرسم لون [style] الاحتياطي.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.glass(state: HazeState?, shape: Shape, style: HazeStyle, rim: GlassRim = glassRim(), rimWidth: Dp = 1.dp): Modifier =
    this
        .clip(shape)
        .then(
            // بلا مصدر مسجَّل (صفحة المصحف) لا شيء يُموَّه، فيُرسم اللون الاحتياطي بدل سطح شفاف فارغ
            if (state != null && state.areas.isNotEmpty()) Modifier.hazeEffect(state, style) { inputScale = HazeInputScale.Auto }
            else Modifier.background(style.fallbackTint.color)
        )
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val sheen = Brush.verticalGradient(0f to rim.sheen, .55f to Color.Transparent)
            val edge = Brush.verticalGradient(listOf(rim.top, rim.bottom))
            // نصف الخط خارج القص، فالظاهر منه بعرض rimWidth
            val stroke = Stroke(rimWidth.toPx() * 2)
            onDrawWithContent {
                drawOutline(outline, sheen)
                drawContent()
                drawOutline(outline, edge, style = stroke)
            }
        }
