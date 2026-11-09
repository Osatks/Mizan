package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.i18n.Proof

/** أندرويد: Compose نفسه واجهة النظام، فزكاة الفطر والفدية كما هما */
internal actual val nativeFitrFidyaAvailable: Boolean = false

@Composable
internal actual fun NativeFitrFidyaView(
    kind: String, content: Map<String, Any?>, evidenceKey: String, evidence: List<Proof>,
    onStep: (String, Int) -> Unit, onChange: (String, String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) = Unit
