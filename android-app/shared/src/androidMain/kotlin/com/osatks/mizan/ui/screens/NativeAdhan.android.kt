package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فإعدادات الأذان كما هي */
internal actual val nativeAdhanAvailable: Boolean = false

@Composable
internal actual fun NativeAdhanView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) = Unit
