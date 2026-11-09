package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import com.osatks.mizan.ui.NativeSheetPresenter
import com.osatks.mizan.ui.NativeSheets

/** أوراق النظام متى سجّلها Swift (NativeSheetsUI.install في iosApp/iosApp/NativeSheets.swift) */
internal actual val nativeSheetsAvailable: Boolean get() = NativeSheets.available

@Composable
internal actual fun NativeSheetHost(
    kind: String,
    json: String,
    startLarge: Boolean,
    onDismiss: () -> Unit,
    onAction: NativeSheetControl.(action: String, arg: String) -> Unit,
) = NativeSheetPresenter(kind, json, startLarge, onDismiss, onAction)
