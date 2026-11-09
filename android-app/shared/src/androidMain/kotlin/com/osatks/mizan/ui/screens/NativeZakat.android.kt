package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فالزكاة كما هي */
internal actual val nativeZakatAvailable: Boolean = false

@Composable
internal actual fun NativeZakatView(open: String, onOpen: (String) -> Unit, onBack: () -> Unit, modifier: Modifier) = Unit
