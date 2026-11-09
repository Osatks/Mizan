package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فشاشة المسبحة كما هي */
internal actual val nativeTasbihAvailable: Boolean = false

@Composable
internal actual fun NativeTasbihView(onBack: (() -> Unit)?, modifier: Modifier) = Unit
