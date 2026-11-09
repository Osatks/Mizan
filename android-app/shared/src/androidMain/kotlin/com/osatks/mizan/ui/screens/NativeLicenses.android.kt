package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فالصفحة كما هي */
internal actual val nativeLicensesAvailable: Boolean = false

@Composable
internal actual fun NativeLicensesView(onBack: () -> Unit, modifier: Modifier) = Unit
