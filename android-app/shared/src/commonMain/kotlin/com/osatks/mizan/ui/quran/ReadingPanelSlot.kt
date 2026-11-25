package com.osatks.mizan.ui.quran

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ما بين لوحة القراءة (ReadingPanel) ومحتواها: إن عرض المحتوى ورقة النظام بدلها ([native]) أخفت اللوحة زجاجها
 * وستارها وبقيت في التركيب ما دامت الورقة ظاهرة؛ و[dismiss] يغلق اللوحة (عند سحب الورقة لإغلاقها).
 */
internal class ReadingPanelSlot {
    var native by mutableStateOf(false)
    var dismiss: () -> Unit = {}
}

internal val LocalReadingPanel = compositionLocalOf<ReadingPanelSlot?> { null }
