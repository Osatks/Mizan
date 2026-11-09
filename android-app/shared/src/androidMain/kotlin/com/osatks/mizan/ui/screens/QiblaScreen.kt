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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
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
import com.osatks.mizan.ui.ToolPage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material.icons.filled.ViewInAr
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

    ToolPage(s.qiblaTitle, s.placeName(place), onBack, sharedKey = "qibla", icon = TOOL_ICONS["qibla"], plainIcon = true) {
        item {
            QiblaCard {
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
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = hintColor
                )
            }
        }
        // iPhone و iPad: الكعبة واتجاهها فوق ما تراه الكاميرا (ARKit)
        if (ArQibla.supported) item {
            val ft = FeatureText.of(LocalLang.current.code)
            QiblaCard {
                QiblaLinkRow(Icons.Filled.ViewInAr, ft.arQibla, ft.arHint) { ArQibla.open?.invoke() }
            }
        }
        item {
            ResultCard(s.qiblaAngle, "${Fmt.num(qibla, 1)}°", s.towards(s.directions[(kotlin.math.round(qibla / 45.0).toInt()) % 8]))
        }
        item {
            QiblaCard {
                QiblaStatRow(s.location, s.placeName(place))
                QiblaStatRow(s.distanceToKaaba, Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0) + " " + s.unitKm)
                if (hasSensor) QiblaStatRow(s.phoneHeading, "${heading.toInt()}°")
            }
        }
        item {
            Note(
                if (accuracyLow) s.compassLowAccuracy else s.qiblaTip,
                warn = accuracyLow
            )
        }
        // معايرة البوصلة: خطوات واضحة بلغة التطبيق (وتظهر الدقة المنخفضة فوقها)
        if (hasSensor) item {
            val cal = com.osatks.mizan.i18n.QiblaCalibText.of(LocalLang.current.code)
            var open by remember { mutableStateOf(false) }
            QiblaCard {
                QiblaLinkRow(Icons.Outlined.Explore, cal.title, cal.steps.first()) { open = true }
            }
            if (open) androidx.compose.material3.AlertDialog(
                onDismissRequest = { open = false },
                icon = {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Gold.copy(alpha = .16f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Explore, null, tint = MaterialTheme.colorScheme.primary) }
                },
                title = { Text(cal.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        cal.all(com.osatks.mizan.platform.Platform.name).forEachIndexed { i, step ->
                            // رقم الخطوة في دائرة بلون التطبيق الفاتح (كما في iPhone)
                            Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                                }
                                Text(step, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(CircleShape).background(HeroBrush)
                            .clickable(role = Role.Button) { open = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cal.done, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }
        item { EvidenceCard(com.osatks.mizan.i18n.Evidence.qibla) }
    }
}

/** بطاقة الصفحة كما في iPhone: بيضاء بزوايا 22 وحد رفيع، وحشوة 16 */
@Composable
private fun QiblaCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(cs.surface).border(1.dp, cs.outlineVariant.copy(alpha = .6f), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** سطر معلومة: العنوان باهتاً في البداية والقيمة عريضة في النهاية */
@Composable
private fun QiblaStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp).weight(1f))
        Text(
            value, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End
        )
    }
}

/** صف يُلمس: أيقونة على مربع ذهبي فاتح، وعنوان ووصف، وسهم */
@Composable
private fun QiblaLinkRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().pressScale(src, 0.98f)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Gold.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(
                sub, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Compass(heading: Float, qibla: Float, aligned: Boolean) {
    // عند الاتجاه الصحيح: الحلقة تتذهّب وتنبض هالة حولها
    val ring by animateColorAsState(if (aligned) Gold else MaterialTheme.colorScheme.primaryContainer, tween(Motion.MEDIUM), label = "ring")
    val glowOn by animateFloatAsState(if (aligned) 1f else 0f, tween(Motion.LONG), label = "glow")
    val wave by if (com.osatks.mizan.ui.LocalPageActive.current) {
        rememberInfiniteTransition(label = "qpulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "wave")
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val north = MaterialTheme.colorScheme.error
    val gold = Gold
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
        // مثلث 24×18 فوق الحلقة (كما في iPhone)
        val mw = 12.dp.toPx(); val mh = 18.dp.toPx(); val my = c.y - r + 7.dp.toPx() - mh / 2
        val mark = Path().apply { moveTo(c.x, my); lineTo(c.x - mw, my + mh); lineTo(c.x + mw, my + mh); close() }
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
                    strokeWidth = when { a == 0 -> 2.4.dp.toPx(); long -> 2.dp.toPx(); else -> 1.2.dp.toPx() }, cap = StrokeCap.Round
                )
            }
            // سهم القبلة
            rotate(qibla, c) {
                drawLine(gold, c, Offset(c.x, c.y - r * 0.62f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                val k = r * 0.13f
                val top = c.y - r * 0.62f - k
                drawRect(Color(0xFF1D1D1D), Offset(c.x - k / 2, top - k / 2), Size(k, k))
                drawRect(gold, Offset(c.x - k / 2, top - k / 2), Size(k, k * 0.22f))
                drawRect(gold, Offset(c.x - k / 2, top - k / 2), Size(k, k), style = Stroke(1.dp.toPx()))
            }
        }
        drawCircle(primary, r * 0.06f, c)
        drawCircle(face, r * 0.03f, c)
    }
}
