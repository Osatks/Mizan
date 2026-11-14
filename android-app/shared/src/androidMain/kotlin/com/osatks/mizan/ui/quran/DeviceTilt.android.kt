package com.osatks.mizan.ui.quran

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

/**
 * حساس الجاذبية (أو التسارع إن لم يوجد): الفرق بين اتجاه الجاذبية الآن ووضع مرجعي يتبعه ببطء (نحو ثانيتين)،
 * مع تنعيم يزيل الارتجاف. «تقليل الحركة» في النظام (مدة الحركات صفر) يوقفه.
 */
@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        val reduce = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || reduce || sm == null || sensor == null) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        @Suppress("DEPRECATION")
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        var refX = Float.NaN; var refY = Float.NaN
        var sx = 0f; var sy = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // محورا الشاشة كما يراها القارئ (الجهاز بالعرض يبدّلهما)
                var gx = e.values[0] / SensorManager.GRAVITY_EARTH
                var gy = e.values[1] / SensorManager.GRAVITY_EARTH
                when (display?.rotation) {
                    Surface.ROTATION_90 -> { val t = gx; gx = -gy; gy = t }
                    Surface.ROTATION_180 -> { gx = -gx; gy = -gy }
                    Surface.ROTATION_270 -> { val t = gx; gx = gy; gy = -t }
                }
                if (refX.isNaN()) { refX = gx; refY = gy }
                // المرجع يلحق بطريقة الإمساك ببطء، والقراءة تُنعَّم
                refX += (gx - refX) * 0.012f; refY += (gy - refY) * 0.012f
                sx += ((gx - refX) - sx) * 0.18f; sy += ((gy - refY) - sy) * 0.18f
                // ميل نحو 15° يبلغ أقصى الحركة
                val x = (-sx / 0.26f).coerceIn(-1f, 1f)
                val y = (sy / 0.26f).coerceIn(-1f, 1f)
                val old = tilt.value
                if (abs(old.x - x) > 0.004f || abs(old.y - y) > 0.004f) tilt.value = Offset(x, y)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener); tilt.value = Offset.Zero }
    }
    return tilt
}
