package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فشاشة القبلة كما هي */
internal actual val nativeQiblaAvailable: Boolean = false

@Composable
internal actual fun NativeQiblaView(onBack: (() -> Unit)?, modifier: Modifier) = Unit
