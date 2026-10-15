package com.osatks.mizan.ui.screens

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.ResultCard
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.StatRow
import com.osatks.mizan.ui.ToolPage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val DIRS = listOf("الشمال", "الشمال الشرقي", "الشرق", "الجنوب الشرقي", "الجنوب", "الجنوب الغربي", "الغرب", "الشمال الغربي")

@Composable
fun QiblaScreen() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val place = AppSettings.place
    val qibla = PrayTimes.qibla(place.lat, place.lng)
    var heading by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }
    var accuracyLow by remember { mutableStateOf(false) }

    DisposableEffect(place) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) { hasSensor = false; return@DisposableEffect onDispose { } }
        val declination = GeomagneticField(place.lat.toFloat(), place.lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rot = FloatArray(9); val orient = FloatArray(3)
        var smooth = Float.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val az = ((Math.toDegrees(orient[0].toDouble()).toFloat() + declination) + 360f) % 360f
                smooth = if (smooth.isNaN()) az else {
                    val d = ((az - smooth + 540f) % 360f) - 180f
                    (smooth + d * 0.15f + 360f) % 360f
                }
                heading = smooth
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {
                accuracyLow = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    val diff = (((qibla - heading) % 360) + 540) % 360 - 180 // موجب = لليمين
    val aligned = hasSensor && abs(diff) < 4
    LaunchedEffect(aligned) { if (aligned) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    ToolPage("اتجاه القبلة", place.name) {
        item {
            SectionCard {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                    Compass(heading = if (hasSensor) heading else 0f, qibla = qibla.toFloat(), aligned = aligned)
                }
                Text(
                    when {
                        !hasSensor -> "لا يحتوي جهازك على بوصلة. وجّه الجهاز يدوياً حسب الزاوية أدناه."
                        aligned -> "✓ أنت متجه نحو القبلة"
                        else -> "أدِر هاتفك ${if (diff > 0) "يميناً" else "يساراً"} بمقدار ${abs(diff).toInt()}°"
                    },
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                    color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        item {
            ResultCard("زاوية القبلة من الشمال الجغرافي", "${Fmt.num(qibla, 1)}°", "باتجاه " + DIRS[(Math.round(qibla / 45.0).toInt()) % 8])
        }
        item {
            SectionCard {
                StatRow("الموقع", place.name)
                StatRow("المسافة إلى الكعبة", Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0) + " كم")
                if (hasSensor) StatRow("اتجاه الهاتف", "${heading.toInt()}°")
            }
        }
        item {
            Note(
                if (accuracyLow) "دقة البوصلة منخفضة: حرّك الهاتف على شكل الرقم 8 لمعايرتها، وأبعده عن المعادن والأجهزة."
                else "لتغيير الموقع استخدم صفحة مواقيت الصلاة. أمسك الهاتف أفقياً وبعيداً عن المعادن للحصول على قراءة دقيقة.",
                warn = accuracyLow
            )
        }
    }
}

@Composable
private fun Compass(heading: Float, qibla: Float, aligned: Boolean) {
    val ring = MaterialTheme.colorScheme.primaryContainer
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val north = MaterialTheme.colorScheme.error
    val gold = Color(0xFFE0B54A)
    val primary = MaterialTheme.colorScheme.primary
    val face = MaterialTheme.colorScheme.surface
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(if (aligned) gold else ring, r, c)
        drawCircle(face, r * 0.9f, c)
        // علامة أعلى الهاتف
        val mark = Path().apply { moveTo(c.x, c.y - r - 2); lineTo(c.x - 12, c.y - r + 16); lineTo(c.x + 12, c.y - r + 16); close() }
        drawPath(mark, primary)
        rotate(-heading, c) {
            for (a in 0 until 360 step 10) {
                val long = a % 30 == 0
                val rad = Math.toRadians(a.toDouble())
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
