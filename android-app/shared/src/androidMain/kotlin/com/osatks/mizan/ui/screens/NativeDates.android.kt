package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.i18n.Proof

/** أندرويد: Compose نفسه واجهة النظام، فأدوات التاريخ كما هي */
internal actual val nativeDatesAvailable: Boolean = false

@Composable
internal actual fun NativeDatesView(
    kind: String, content: Map<String, Any?>, evidence: List<Proof>,
    onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) = Unit
