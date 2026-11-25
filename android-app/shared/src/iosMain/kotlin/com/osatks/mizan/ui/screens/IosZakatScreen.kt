package com.osatks.mizan.ui.screens

import com.osatks.mizan.ui.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** الزكاة في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية: الأنواع وحاسبة كل نوع، والحساب كله هنا في Kotlin */
@Composable
internal fun IosZakatScreen(onBack: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("") }
    // فتح النوع والرجوع منه بانتقال النظام نفسه بين الصفحات (ينمو من بطاقته ويصغر إليها)
    var origin by remember { mutableStateOf<Any?>(null) }
    fun go(kind: String) {
        val motion = com.osatks.mizan.ui.NavTransitions.current
        when {
            motion == null || kind == type -> type = kind
            type.isEmpty() -> origin = motion.push { type = kind }
            kind.isEmpty() -> motion.pop(origin) { type = "" }
            else -> type = kind
        }
    }
    BackHandler(enabled = type.isNotEmpty()) { go("") }
    NativeZakatView(type, onOpen = { go(it) }, onBack = onBack, modifier = Modifier.fillMaxSize())
}
