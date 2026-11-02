package com.osatks.mizan.ui.screens

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.osatks.mizan.platform.rememberCompass
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import com.osatks.mizan.ui.ClickRow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.Icons
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.platform.ArQibla

@Composable
fun QiblaScreen(onBack: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val place = AppSettings.place
    val qibla = PrayTimes.qibla(place.lat, place.lng)
    val compass = rememberCompass(place.lat, place.lng)
    val heading = compass.heading
    val hasSensor = compass.available
    val accuracyLow = compass.accuracyLow
    val haptic = LocalHapticFeedback.current

    val diff = (((qibla - heading) % 360) + 540) % 360 - 180 // موجب = لليمين
    val aligned = hasSensor && abs(diff) < 4
    LaunchedEffect(aligned) { if (aligned) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    ToolPage(s.qiblaTitle, s.placeName(place), onBack, sharedKey = "qibla", icon = TOOL_ICONS["qibla"]) {
        item {
            SectionCard {
                // البوصلة بعرض أقصاه 440 في الوسط (لا تملأ عرض iPad فتتجاوز ارتفاع الشاشة)
                Box(Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 440.dp).aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                    Compass(heading = if (hasSensor) heading else 0f, qibla = qibla.toFloat(), aligned = aligned)
                }
                val hintColor by animateColorAsState(
                    if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, tween(Motion.MEDIUM), label = "hint"
                )
                Text(
                    when {
                        !hasSensor -> s.noCompass
                        aligned -> s.facingQibla
                        diff > 0 -> s.turnRight(abs(diff).toInt())
                        else -> s.turnLeft(abs(diff).toInt())
                    },
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                    color = hintColor
                )
            }
        }
        // iPhone و iPad: الكعبة واتجاهها فوق ما تراه الكاميرا (ARKit)
        if (ArQibla.supported) item {
            val ft = FeatureText.of(LocalLang.current.code)
            SectionCard {
                ClickRow(ft.arQibla, ft.arHint, onClick = { ArQibla.open?.invoke() }, leading = Icons.Filled.ViewInAr)
            }
        }
        item {
            ResultCard(s.qiblaAngle, "${Fmt.num(qibla, 1)}°", s.towards(s.directions[(kotlin.math.round(qibla / 45.0).toInt()) % 8]))
        }
        item {
            SectionCard {
                StatRow(s.location, s.placeName(place))
                StatRow(s.distanceToKaaba, Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0) + " " + s.unitKm)
                if (hasSensor) StatRow(s.phoneHeading, "${heading.toInt()}°")
            }
        }
        item {
            Note(
                if (accuracyLow) s.compassLowAccuracy else s.qiblaTip,
                warn = accuracyLow
            )
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.qibla) }
    }
}

@Composable
private fun Compass(heading: Float, qibla: Float, aligned: Boolean) {
    // عند الاتجاه الصحيح: الحلقة تتذهّب وتنبض هالة حولها
    val ring by animateColorAsState(if (aligned) Color(0xFFE0B54A) else MaterialTheme.colorScheme.primaryContainer, tween(Motion.MEDIUM), label = "ring")
    val glowOn by animateFloatAsState(if (aligned) 1f else 0f, tween(Motion.LONG), label = "glow")
    val wave by if (com.osatks.mizan.ui.LocalPageActive.current) {
        rememberInfiniteTransition(label = "qpulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "wave")
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val north = MaterialTheme.colorScheme.error
    val gold = Color(0xFFE0B54A)
    val primary = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.surface
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(ring, r, c)
        drawCircle(face, r * 0.9f, c)
        // موجة ذهبية تنطلق من المركز ما دام الهاتف متجهاً للقبلة
        if (glowOn > 0f) drawCircle(gold.copy(alpha = 0.28f * glowOn * (1f - wave)), r * 0.9f * wave, c)
        // علامة أعلى الهاتف
        val mark = Path().apply { moveTo(c.x, c.y - r - 2); lineTo(c.x - 12, c.y - r + 16); lineTo(c.x + 12, c.y - r + 16); close() }
        drawPath(mark, primary)
        rotate(-heading, c) {
            for (a in 0 until 360 step 10) {
                val long = a % 30 == 0
                val rad = a.toDouble() * kotlin.math.PI / 180.0
                val r1 = r * 0.86f; val r2 = if (long) r * 0.76f else r * 0.81f
                drawLine(
                    if (a == 0) north else tick.copy(alpha = if (long) .7f else .35f),
                    Offset(c.x + r1 * sin(rad).toFloat(), c.y - r1 * cos(rad).toFloat()),
                    Offset(c.x + r2 * sin(rad).toFloat(), c.y - r2 * cos(rad).toFloat()),
                    strokeWidth = if (long || a == 0) 5f else 2.5f, cap = StrokeCap.Round
                )
            }
            // سهم القبلة
            rotate(qibla, c) {
                drawLine(gold, c, Offset(c.x, c.y - r * 0.62f), strokeWidth = 12f, cap = StrokeCap.Round)
                val k = r * 0.13f
                val top = c.y - r * 0.62f - k
                drawRect(Color(0xFF1D1D1D), Offset(c.x - k / 2, top - k / 2), Size(k, k))
                drawRect(gold, Offset(c.x - k / 2, top - k / 2), Size(k, k * 0.22f))
                drawRect(gold, Offset(c.x - k / 2, top - k / 2), Size(k, k), style = Stroke(3f))
            }
        }
        drawCircle(primary, r * 0.06f, c)
        drawCircle(face, r * 0.03f, c)
    }
}
