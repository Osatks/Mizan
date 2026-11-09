package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.hadith.Hadith

/** أندرويد: Compose نفسه واجهة النظام، فحديث اليوم كما هو */
internal actual val nativeHadithAvailable: Boolean = false

@Composable
internal actual fun NativeHadithView(
    hadith: Hadith?, title: String, subtitle: String, canNext: Boolean,
    onDay: (Int) -> Unit, onShare: () -> Unit, onSource: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) = Unit
