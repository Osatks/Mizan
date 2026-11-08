package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فشاشة المواقيت كما هي */
internal actual val nativePrayerAvailable: Boolean = false

@Composable
internal actual fun NativePrayerView(
    open: (String) -> Unit, locating: Boolean, onLocate: () -> Unit, onPickCity: () -> Unit, onNotify: (Boolean) -> Unit,
    modifier: Modifier,
) = Unit
