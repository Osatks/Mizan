package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فالبحث كما هو */
internal actual val nativeSearchAvailable: Boolean = false

@Composable
internal actual fun NativeSearchView(
    query: String, blank: Boolean, hits: List<NativeSearchHit>,
    onQuery: (String) -> Unit, onPick: (String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) = Unit
