package com.osatks.mizan.ui.quran

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/**
 * تفسير الآية المختارة: نص الآية، واختيار المصدر (التفسير الميسر، الجلالين، ترجمات المعاني)،
 * والانتقال إلى الآية السابقة والتالية بحركة انزلاق، مع الحفظ والنسخ والمشاركة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirSheet(
    surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit, onDismiss: () -> Unit, onLibrary: () -> Unit = {},
    onListen: ((Int) -> Unit)? = null,
) {
    val sheet = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = mushafPalette().paper) {
        TafsirContent(surah, ayah, text, onAyah, onLibrary, onListen)
    }
}

@Composable
actual fun TafsirContent(surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit, onLibrary: () -> Unit, onListen: ((Int) -> Unit)?) =
    TafsirBody(surah, ayah, text, onAyah, onLibrary, onListen)
