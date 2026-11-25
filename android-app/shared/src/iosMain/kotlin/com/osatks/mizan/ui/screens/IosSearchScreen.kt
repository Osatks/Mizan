package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranSearch
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** البحث الشامل في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية؛ النتائج نفسها من [searchHits]، وورقة اللغة من النظام */
@Composable
internal fun IosSearchScreen(onBack: () -> Unit, open: (String) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    var q by rememberSaveable { mutableStateOf("") }
    var showLang by remember { mutableStateOf(false) }

    val terms = Search.terms(q)
    // نص المصحف وآيات مطابقة (تُحسب في الخلفية)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    var ayahHits by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(q, lang) {
        ayahHits = emptyList()
        if (terms.isEmpty()) return@LaunchedEffect
        delay(300)
        ayahHits = QuranSearch.ayahs(q, lang, limit = 6)
    }
    val hits: List<SearchHit> = remember(q, lang, ayahHits, quran) { searchHits(s, lang, q, ayahHits, quran, open) { showLang = true } }

    NativeSearchView(
        q, terms.isEmpty(), hits.map { NativeSearchHit(it.group, it.id, it.title, it.subtitle) },
        onQuery = { q = it },
        onPick = { id -> hits.firstOrNull { it.id == id }?.action?.invoke() },
        onBack = onBack, modifier = Modifier.fillMaxSize(),
    )
    if (showLang) LanguageSheet { showLang = false }
}

/** نتيجة بحث كما في [SearchScreen] (مجموعتها ومعرّفها وعنوانها ووصفها)، والمعرّف يدل على نوعها وأيقونتها */
internal class NativeSearchHit(val group: String, val id: String, val title: String, val subtitle: String)
