package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فتبويب الأدوات كما هو */
internal actual val nativeToolsAvailable: Boolean = false

@Composable
internal actual fun NativeToolsView(open: (String) -> Unit, modifier: Modifier) = Unit
