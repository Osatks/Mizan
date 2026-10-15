package com.osatks.mizan.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.R
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep

/** خط المصحف (Amiri Quran، رخصة SIL OFL) */
val QuranFont = FontFamily(Font(R.font.amiri_quran))

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
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            drawStar8(c, size.minDimension / 2 - 1.5f, GoldDeep, 1.5.dp.toPx(), rings = false)
            drawCircle(Gold.copy(alpha = .14f), size.minDimension * 0.33f, c)
        }
        Text(
            arabicDigits(n), color = ink, fontFamily = QuranFont,
            fontSize = if (n >= 100) numberSize * 0.82f else numberSize, lineHeight = numberSize,
            textAlign = TextAlign.Center
        )
    }
}

/** رقم السورة داخل نجمة (في الفهارس) */
@Composable
fun NumberStar(n: Int, size: Dp = 40.dp) {
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawStar8(Offset(this.size.width / 2, this.size.height / 2), this.size.minDimension / 2 - 2f, color.copy(alpha = .55f), 3f, rings = false)
        }
        Text("$n", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

/** ترويسة السورة كما في المصحف: إطار مذهّب باسم السورة، وتحته البسملة */
@Composable
fun SurahBanner(arabicName: String, bismillah: String?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f),
            border = BorderStroke(1.5.dp, GoldDeep.copy(alpha = .6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Ornament()
                Text(
                    "سُورَةُ $arabicName", Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 44.sp, textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Ornament()
            }
        }
        if (bismillah != null) {
            Spacer(Modifier.size(10.dp))
            Text(
                bismillah, Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = quranStyle(26, TextAlign.Center), color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun Ornament() {
    Canvas(Modifier.size(26.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        drawStar8(c, size.minDimension / 2 - 1f, GoldDeep, 2f)
    }
}

/** شارة صغيرة (مكية/مدنية، الجزء…) */
@Composable
fun MetaPill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        Modifier.background(color.copy(alpha = .1f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)
    ) { Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
fun MetaRow(vararg items: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { MetaPill(it) }
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
