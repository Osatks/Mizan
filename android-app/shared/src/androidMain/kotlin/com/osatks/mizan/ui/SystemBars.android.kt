package com.osatks.mizan.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.osatks.mizan.ui.theme.LocalDark

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** أندرويد: أيقونات شريط الحالة فاتحة أو داكنة حسب ما تحتها (الإخفاء في وضع التركيز يتولاه FocusEffects) */
@Composable
actual fun ApplySystemBars() {
    val view = LocalView.current
    val light = SystemBars.lightContent ?: LocalDark.current
    SideEffect {
        val window = view.context.activity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !light
    }
}
