package com.osatks.mizan.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings

/**
 * ما يُشارك صورةَ قصة (9:16، 1080×1920): آية أو حديث أو ذكر.
 * [kind] عنوان صغير أعلى الصورة، و[arabic] النص بخط المصحف، و[meaning] ترجمته بلغة الواجهة (اختياري)،
 * و[ref] المرجع (السورة والآية، أو الدرجة والتخريج، أو مصدر الذكر)، و[source] سطر المصدر إن لزم (مثل HadeethEnc.com)،
 * و[plain] النص الذي يُشارك نصاً أو مع الصورة.
 */
class StoryContent(
    val kind: String, val arabic: String, val meaning: String?, val ref: String,
    val plain: String, val source: String? = null,
)

/** عرض صورة القصة بالبكسل (ارتفاعها 1920) */
const val STORY_WIDTH = 1080

const val STORY_HEIGHT = 1920

/**
 * صورة القصة: خلفية متدرجة بنجمة ثمانية كبيرة خافتة في وسطها وإطار ذهبي رفيع، ثم العنوان الصغير، والنص بأكبر خط يسعه،
 * والمعنى تحته، والمرجع. وفي الأسفل شعار ميزان (الميزان الذهبي واسمه) صغيراً هادئاً فوق منطقة أزرار القصة.
 * النص كله داخل المنطقة الآمنة لتطبيقات القصص (لا يغطيه اسم الحساب أعلاها ولا خانة الرد أسفلها).
 */
@Composable
fun StoryCard(content: StoryContent, withMeaning: Boolean, style: CardStyle, brand: String, modifier: Modifier) {
    val c = cardColors(style, AppSettings.accent)
    val meaning = content.meaning.takeIf { withMeaning }
    Box(modifier.aspectRatio(9f / 16f).background(Brush.verticalGradient(listOf(c.top, c.bottom)))) {
        Canvas(Modifier.fillMaxSize()) { storyBackdrop(c.gold) }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp).padding(top = 58.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // العنوان بين خطين ذهبيين قصيرين
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(18.dp).height(1.dp).background(c.gold.copy(alpha = .7f)))
                Spacer(Modifier.width(8.dp))
                Text(
                    content.kind, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontSize = 11.sp, color = c.gold, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, textDirection = TextDirection.Content),
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(18.dp).height(1.dp).background(c.gold.copy(alpha = .7f)))
            }
            Spacer(Modifier.height(14.dp))
            FitText(
                content.arabic, TextStyle(fontFamily = QuranFont, color = c.text, textAlign = TextAlign.Center, textDirection = TextDirection.Rtl),
                maxSize = 30f, minSize = 10f, lineHeight = 1.9f, modifier = Modifier.fillMaxWidth().weight(if (meaning != null) 0.6f else 1f),
            )
            if (meaning != null) {
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.size(9.dp)) { star(c.gold.copy(alpha = .8f)) }
                Spacer(Modifier.height(10.dp))
                FitText(
                    meaning, TextStyle(color = c.sub, textAlign = TextAlign.Center, textDirection = TextDirection.Content),
                    maxSize = 15f, minSize = 7f, lineHeight = 1.45f, modifier = Modifier.fillMaxWidth().weight(0.4f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                content.ref, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = 10.sp, color = c.sub, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, textDirection = TextDirection.Content),
            )
            content.source?.let {
                Text(it, maxLines = 1, style = TextStyle(fontSize = 9.sp, color = c.sub.copy(alpha = .75f), textAlign = TextAlign.Center))
            }
            // منطقة أزرار القصة: الشعار فوقها
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(15.dp)) { mizanEmblem(c.gold) }
                Spacer(Modifier.width(6.dp))
                Text(brand, style = TextStyle(fontSize = 11.sp, color = c.gold, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp), maxLines = 1)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

/** خلفية القصة: إطار ذهبي رفيع، ونجمة ثمانية كبيرة خافتة خلف النص */
private fun DrawScope.storyBackdrop(gold: Color) {
    val inset = 10.dp.toPx()
    val r = 12.dp.toPx()
    drawRoundRect(
        gold.copy(alpha = .38f), topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset),
        cornerRadius = CornerRadius(r, r), style = Stroke(width = 0.8.dp.toPx()),
    )
    val cx = size.width / 2
    val cy = size.height * 0.46f
    val rr = size.width * 0.62f
    val half = rr / kotlin.math.sqrt(2f)
    val w = 1.dp.toPx()
    for (k in listOf(1f, 0.78f)) {
        val h = half * k
        val q = rr * k
        drawRect(gold.copy(alpha = .07f), Offset(cx - h, cy - h), Size(2 * h, 2 * h), style = Stroke(w))
        val p = Path().apply { moveTo(cx, cy - q); lineTo(cx + q, cy); lineTo(cx, cy + q); lineTo(cx - q, cy); close() }
        drawPath(p, gold.copy(alpha = .07f), style = Stroke(w))
    }
}

/**
 * شعار ميزان (الميزان الذهبي من أيقونة التطبيق، branding/icon.py) مرسوماً بالأشكال نفسها في مربع [size]:
 * الحلقة العليا، والعمود، والعارضة، والقاعدة، والكفّتان بخيوطهما.
 */
fun DrawScope.mizanEmblem(gold: Color) {
    // حدود الشعار في مساحة الأيقونة (1024): x من 184 إلى 840، و y من 246 إلى 768
    val minX = 176f; val minY = 238f; val span = 672f
    val k = size.minDimension / span
    val dx = (size.width - span * k) / 2
    val dy = (size.height - span * k) / 2
    withTransform({ translate(dx, dy); scale(k, k, pivot = Offset.Zero); translate(-minX, -minY + (span - 530f) / 2) }) {
        drawCircle(gold, 30f, Offset(512f, 276f))
        drawRoundRect(gold, Offset(493f, 294f), Size(38f, 438f), CornerRadius(19f, 19f))
        drawRoundRect(gold, Offset(250f, 347f), Size(524f, 34f), CornerRadius(17f, 17f))
        drawPath(Path().apply { moveTo(472f, 704f); lineTo(552f, 704f); lineTo(594f, 738f); lineTo(430f, 738f); close() }, gold)
        drawRoundRect(gold, Offset(380f, 732f), Size(264f, 36f), CornerRadius(18f, 18f))
        for (x in listOf(288f, 736f)) {
            // الكفة: نصف قطع ناقص تحت خط الكفة
            drawArc(gold, 0f, 180f, true, Offset(x - 104f, 580f - 76f), Size(208f, 152f))
            drawLine(gold, Offset(x, 364f), Offset(x - 88f, 578f), 18f, StrokeCap.Round)
            drawLine(gold, Offset(x, 364f), Offset(x + 88f, 578f), 18f, StrokeCap.Round)
        }
    }
}
