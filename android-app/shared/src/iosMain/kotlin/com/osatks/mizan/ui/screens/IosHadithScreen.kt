package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.hadith.Hadith
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.greg
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.ui.quran.StoryContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import com.osatks.mizan.ui.quran.NativeStoryShare

/**
 * حديث اليوم في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية: السهمان يعرضان أحاديث الأيام الماضية (حتى اليوم)،
 * والمشاركة صورةَ قصة بورقة النظام
 */
@Composable
internal fun IosHadithScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val today = remember { PrayerUtil.now().date }
    var offset by remember { mutableIntStateOf(0) }
    val day = today.plusDays(offset)
    val hadith by produceState<Hadith?>(null, day, lang) {
        value = withContext(Dispatchers.IO) { runCatching { Hadiths.forDay(day, lang.code) }.getOrNull() }
    }
    var story by remember { mutableStateOf<StoryContent?>(null) }
    val uri = LocalUriHandler.current
    NativeHadithView(
        hadith, ht.hadithOfDay, "${s.weekday(day)} · ${s.greg(day)}", canNext = offset < 0,
        onDay = { d -> offset = (offset + d).coerceAtMost(0) },
        onShare = { hadith?.let { story = hadithStory(it, ht.hadithOfDay) } },
        onSource = { hadith?.let { h -> runCatching { uri.openUri(h.link) } } },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
    story?.let { NativeStoryShare(it) { story = null } }
}
