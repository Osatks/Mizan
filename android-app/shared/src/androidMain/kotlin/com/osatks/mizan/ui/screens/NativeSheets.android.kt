package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable

/** أندرويد: أوراق Compose نفسها أوراق النظام، فتبقى كما هي */
internal actual val nativeSheetsAvailable: Boolean = false

@Composable
internal actual fun NativeSheetHost(
    kind: String,
    json: String,
    startLarge: Boolean,
    onDismiss: () -> Unit,
    onAction: NativeSheetControl.(action: String, arg: String) -> Unit,
) = Unit
