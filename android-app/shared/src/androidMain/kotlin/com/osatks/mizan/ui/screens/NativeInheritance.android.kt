package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.InheritanceResult

/** أندرويد: Compose نفسه واجهة النظام، فحاسبة المواريث كما هي */
internal actual val nativeInheritanceAvailable: Boolean = false

@Composable
internal actual fun NativeInheritanceView(
    gender: Char, counts: Map<String, Int>, active: Map<String, Int>,
    estate: String, funeral: String, debts: String, will: String,
    result: InheritanceResult?, net: Double, willCapped: Boolean,
    onGender: (Char) -> Unit, onCount: (String, Int) -> Unit, onField: (String, String) -> Unit,
    onReset: () -> Unit, onShare: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) = Unit
