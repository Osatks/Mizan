package com.osatks.mizan.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.LocationFetcher

/** يضبط السياق ويهيئ الإعدادات (لكل نقاط الدخول: النشاط، الودجت، المنبّه) */
fun AppSettings.init(ctx: Context) {
    AndroidContext.set(ctx)
    init()
}

actual object PrayerAlerts {
    /** يضبطها تطبيق أندرويد عند البدء (PrayerAlarms في وحدة app) */
    var onReschedule: (Context) -> Unit = {}

    actual fun reschedule() = onReschedule(AndroidContext.app)
    /** يضبطها تطبيق أندرويد (PrayerAlarms.ensureChannel) */
    var onLanguageChanged: (Context) -> Unit = {}
    actual fun languageChanged() = onLanguageChanged(AndroidContext.app)
    actual val hasExactAlarmSettings: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    actual fun openExactAlarmSettings() {
        val ctx = AndroidContext.app
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Composable
actual fun rememberLocationRequest(onResult: (LocationResult) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) LocationFetcher.fetch(ctx) { p -> onResult(if (p != null) LocationResult.Found(p) else LocationResult.Failed) }
        else onResult(LocationResult.Denied)
    }
    return { perm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
}

@Composable
actual fun rememberNotifyToggle(onResult: (Boolean) -> Unit): (Boolean) -> Unit {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> onResult(ok) }
    return { on ->
        if (on && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onResult(on)
    }
}

@Composable
actual fun systemLanguageTag(): String = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].toLanguageTag()

@Composable
actual fun rememberCompass(lat: Double, lng: Double): Compass {
    val ctx = LocalContext.current
    var heading by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var has by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var low by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(lat, lng) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val sensor = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        if (sm == null || sensor == null) { has = false; return@DisposableEffect onDispose { } }
        val declination = android.hardware.GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination
        val rot = FloatArray(9); val orient = FloatArray(3)
        var smooth = Float.NaN
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                android.hardware.SensorManager.getRotationMatrixFromVector(rot, e.values)
                android.hardware.SensorManager.getOrientation(rot, orient)
                val az = ((Math.toDegrees(orient[0].toDouble()).toFloat() + declination) + 360f) % 360f
                smooth = if (smooth.isNaN()) az else {
                    val d = ((az - smooth + 540f) % 360f) - 180f
                    (smooth + d * 0.15f + 360f) % 360f
                }
                heading = smooth
            }
            override fun onAccuracyChanged(s: android.hardware.Sensor?, accuracy: Int) {
                low = accuracy <= android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        sm.registerListener(listener, sensor, android.hardware.SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return Compass(heading, has, low)
}
