package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** أندرويد: Compose نفسه واجهة النظام، فالمصحف بـ HorizontalPager كما هو */
internal actual val nativeMushafAvailable: Boolean = false

@Composable
internal actual fun NativeMushafPager(
    turns: NativeTurns, text: Array<String>, selected: Int, bottomInset: Dp, frame: Dp, focus: Boolean,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onFrameTap: () -> Unit, onEdge: (Int) -> Unit,
    hifz: Boolean, revealed: Set<Int>, modifier: Modifier,
) = Unit
