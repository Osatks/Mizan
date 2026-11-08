package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فالرئيسية كما هي */
internal actual val nativeHomeAvailable: Boolean = false

@Composable
internal actual fun NativeHomeView(open: (String) -> Unit, onLanguage: () -> Unit, modifier: Modifier) = Unit
