package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فمكتبة التفاسير كما هي */
internal actual val nativeTafsirsAvailable: Boolean = false

@Composable
internal actual fun NativeTafsirsView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) = Unit
