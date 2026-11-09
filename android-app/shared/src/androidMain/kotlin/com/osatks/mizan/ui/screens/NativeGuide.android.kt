package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** أندرويد: Compose نفسه واجهة النظام، فالجولة كما هي */
internal actual val nativeGuideAvailable: Boolean = false

@Composable
internal actual fun NativeGuideView(
    pages: List<GuidePage>, page: Int, location: Boolean, placeName: String?, locating: Boolean,
    onPage: (Int) -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onLocate: () -> Unit, onPickCity: () -> Unit,
    modifier: Modifier,
) = Unit
