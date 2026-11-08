package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.adhkar.AdhkarSection

/** أندرويد: Compose نفسه واجهة النظام، فالأذكار كما هي */
internal actual val nativeAdhkarAvailable: Boolean = false

@Composable
internal actual fun NativeAdhkarView(
    sections: List<AdhkarSection>?, open: String, onOpen: (String) -> Unit, onBack: () -> Unit, onShare: (String) -> Unit,
    modifier: Modifier,
) = Unit
