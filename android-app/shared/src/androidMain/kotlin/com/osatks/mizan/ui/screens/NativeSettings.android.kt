package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فتبويب الإعدادات كما هو */
internal actual val nativeSettingsAvailable: Boolean = false

@Composable
internal actual fun NativeSettingsView(open: (String) -> Unit, onLanguage: () -> Unit, onNotify: (Boolean) -> Unit, modifier: Modifier) = Unit
