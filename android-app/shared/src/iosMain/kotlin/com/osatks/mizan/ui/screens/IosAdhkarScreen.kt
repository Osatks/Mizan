package com.osatks.mizan.ui.screens

import com.osatks.mizan.core.*
import com.osatks.mizan.ui.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * الأذكار في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية: الأقسام (وأذكار الوقت في أعلاها) ثم أذكار القسم، والرجوع من القسم
 * بالزر أو بسحب الحافة؛ ومشاركة الذكر صورةَ قصة بورقة النظام
 */
@Composable
internal fun IosAdhkarScreen(onBack: () -> Unit, start: String = com.osatks.mizan.widget.WidgetLaunch.takeAdhkarSection()) {
    var open by rememberSaveable { mutableStateOf(start) }
    val sections by produceState(Adhkar.loaded()) { if (value == null) value = withContext(Dispatchers.IO) { Adhkar.sections() } }
    val list = sections
    var shareId by remember { mutableStateOf<String?>(null) }
    // فتح القسم والرجوع منه بانتقال النظام نفسه بين الصفحات (ينمو القسم من بطاقته ويصغر إليها)
    var origin by remember { mutableStateOf<Any?>(null) }
    fun go(id: String) {
        val motion = com.osatks.mizan.ui.NavTransitions.current
        when {
            motion == null || id == open -> open = id
            open.isEmpty() -> origin = motion.push { open = id }
            id.isEmpty() -> motion.pop(origin) { open = "" }
            else -> open = id
        }
    }
    com.osatks.mizan.ui.BackHandler(enabled = open.isNotEmpty()) { go("") }
    NativeAdhkarView(list, open, onOpen = { go(it) }, onBack = onBack, onShare = { shareId = it }, modifier = Modifier.fillMaxSize())
    val lang = LocalLang.current
    val s = LocalStrings.current
    val d = shareId?.let { id -> list?.flatMap { it.items }?.firstOrNull { it.id == id } }
    if (d != null) {
        val tr = remember(lang) { Adhkar.tr(lang) }
        val t = translation(d, tr, lang, null)
        com.osatks.mizan.ui.quran.NativeStoryShare(
            com.osatks.mizan.ui.quran.StoryContent(
                com.osatks.mizan.i18n.HomeText.of(lang.code).dhikr, d.ar, t,
                "${sourceLine(d, tr, s).take(2).joinToString(" · ")} (${gradeLine(d, tr)})",
                shareBody(d, tr, lang, s, t),
            ),
        ) { shareId = null }
    }
}
