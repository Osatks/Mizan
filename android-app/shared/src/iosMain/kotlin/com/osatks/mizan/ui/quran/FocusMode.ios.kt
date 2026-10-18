package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Recitation
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen

actual object QuietReading {
    actual fun granted() = false
    actual fun asked() = true
    actual fun markAsked() {}
    actual fun requestAccess() {}
    actual fun silence() {}
    actual fun restore() {}
}

/** iOS: تبقى الشاشة مضاءة أثناء وضع التركيز */
@Composable
actual fun FocusEffects(active: Boolean) {
    DisposableEffect(active) {
        if (active) UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { if (active) UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

@Composable
actual fun ReadingBrightness() {
    val level = QuranPrefs.brightness
    DisposableEffect(level) {
        val screen = UIScreen.mainScreen
        val before = screen.brightness
        if (level >= 0f) screen.brightness = level.toDouble()
        onDispose { if (level >= 0f) screen.brightness = before }
    }
}

@Composable
actual fun RecitationLifecycle() {
    DisposableEffect(Unit) { onDispose { Recitation.release() } }
}
