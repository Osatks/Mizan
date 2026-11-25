package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** أندرويد: Compose نفسه واجهة النظام، فعرض آية آية بـ LazyColumn كما هو */
internal actual val nativeAyahListAvailable: Boolean = false

@Composable
internal actual fun NativeAyahListView(
    scroll: NativeAyahScroll, surah: Int, text: Array<String>, translation: Array<String>?, trRtl: Boolean,
    selected: Int, size: Int, bottomInset: Dp,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onTafsir: (Int) -> Unit, onScrollUp: () -> Unit, onSurah: (Int) -> Unit,
    modifier: Modifier,
) = Unit
