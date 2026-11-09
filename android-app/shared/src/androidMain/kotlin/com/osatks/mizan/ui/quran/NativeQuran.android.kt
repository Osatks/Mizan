package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فتبويب القرآن كما هو */
internal actual val nativeQuranAvailable: Boolean = false

@Composable
internal actual fun NativeQuranView(open: (String) -> Unit, modifier: Modifier) = Unit
