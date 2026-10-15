package com.osatks.mizan.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import com.osatks.mizan.ui.starBackdrop
import kotlinx.coroutines.delay

/**
 * مسار صفحة السورة؛ ayah = 0 يعني من أولها. from يحدد العنصر الذي يتحول إلى رأس الصفحة
 * ("row" صف السورة، "continue" بطاقة المتابعة) فلا يتكرر مفتاح الحركة في الشاشة نفسها.
 */
fun surahRoute(surah: Int, ayah: Int = 0, from: String = "row") = "surah/$surah?ayah=$ayah&from=$from"
const val SURAH_ROUTE = "surah/{surah}?ayah={ayah}&from={from}"

/** اسم السورة بلغة الواجهة (أو بالعربية إن نقص) */
fun Strings.surahName(surah: Int): String = surahNames.getOrNull(surah - 1) ?: QuranMeta.NAMES_AR[surah - 1]

fun Strings.ayahRef(index: Int): String {
    val (surah, ayah) = QuranMeta.locate(index)
    return surahAyah(surahName(surah), ayah)
}

/** فهرس المصحف: متابعة القراءة، والبحث في السور والآيات، والسور والأجزاء والمحفوظات */
@Composable
fun QuranScreen(open: (String) -> Unit) {
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    var tab by rememberSaveable { mutableStateOf("surahs") }
    var q by rememberSaveable { mutableStateOf("") }
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }

    // نتائج البحث في الآيات تُحسب في الخلفية بعد توقف الكتابة قليلاً
    var ayahHits by remember { mutableStateOf<List<Int>?>(emptyList()) }
    LaunchedEffect(q, lang) {
        if (q.isBlank()) { ayahHits = emptyList(); return@LaunchedEffect }
        ayahHits = null
        delay(250)
        ayahHits = QuranSearch.ayahs(q, lang)
    }
    val terms = Search.terms(q)
    val surahHits = remember(q, lang) {
        if (terms.isEmpty()) emptyList() else (1..QuranMeta.SURAHS).map { n ->
            n to Search.score(terms, s.surahName(n), "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)}")
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(12)
    }
    val ref = remember(q) { QuranSearch.parseRef(q) }

    ToolPage(s.quranTitle, s.quranSubtitle, sharedKey = "quran", icon = Icons.AutoMirrored.Filled.MenuBook) {
        if (QuranPrefs.last >= 0 && q.isBlank()) item(key = "continue") { ContinueCard(QuranPrefs.last, open) }
        if (q.isBlank()) item(key = "khatma") { KhatmaCard(open) }
        item(key = "search") {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text(s.searchQuranHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, null) } },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (q.isNotBlank()) {
            if (ref != null) item(key = "ref") { AyahHit(ref, text, open) }
            if (surahHits.isNotEmpty()) {
                item(key = "hs") { GroupTitle(s.searchQuranGroup) }
                surahHits.forEach { n -> item(key = "s$n") { SurahRow(n, lang, open) } }
            }
            val hits = ayahHits
            if (hits == null) item(key = "busy") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
            } else if (hits.isNotEmpty()) {
                item(key = "ha") { GroupTitle("${s.searchAyahsGroup} (${hits.size})") }
                hits.forEach { i -> item(key = "a$i") { AyahHit(i, text, open) } }
            } else if (surahHits.isEmpty() && ref == null) item(key = "none") { Note(s.searchNoResults(q)) }
        } else {
            item(key = "tabs") {
                Segmented(listOf("surahs" to s.surahsTab, "juz" to s.juzTab, "saved" to s.bookmarksTab), tab) { tab = it }
            }
            when (tab) {
                "surahs" -> (1..QuranMeta.SURAHS).forEach { n -> item(key = "s$n") { SurahRow(n, lang, open) } }
                "juz" -> (1..30).forEach { j -> item(key = "j$j") { JuzRow(j, open) } }
                else -> if (QuranPrefs.bookmarks.isEmpty()) item(key = "nomarks") { Note(s.noBookmarks) }
                else QuranPrefs.bookmarks.toList().forEach { i -> item(key = "b$i") { AyahHit(i, text, open, bookmarked = true) } }
            }
        }
    }
}

@Composable
private fun GroupTitle(t: String) {
    Text(t, Modifier.padding(start = 6.dp, top = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

/** بطاقة متابعة القراءة؛ تتحول إلى رأس صفحة السورة عند فتحها */
@Composable
fun ContinueCard(index: Int, open: (String) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val (surah, ayah) = QuranMeta.locate(index)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(surah, ayah, "continue")) }, shape = RoundedCornerShape(22.dp), color = Color.Transparent,
        interactionSource = src,
        modifier = modifier.fillMaxWidth().sharedCard("continue-$surah", RoundedCornerShape(22.dp)).pressScale(src, 0.97f)
    ) {
        Row(
            Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .07f), 60.dp, Alignment.CenterEnd, (-30).dp, 0.dp, 90_000)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(s.continueReading, color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(s.ayahRef(index), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("${s.juzName(QuranMeta.juzOf(index))} · ${s.pageName(QuranMeta.pageOf(index))}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
            }
            Text(QuranMeta.NAMES_AR[surah - 1], color = Color.White, style = quranStyle(26, TextAlign.Center))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .8f))
        }
    }
}

@Composable
fun SurahRow(n: Int, lang: Lang, open: (String) -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(n)) }, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().sharedCard("row-$n", RoundedCornerShape(16.dp)).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStar(n)
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                if (lang == Lang.AR) Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(22), color = MaterialTheme.colorScheme.onSurface)
                else Text(s.surahName(n), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (lang != Lang.AR) Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(22), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun JuzRow(j: Int, open: (String) -> Unit) {
    val s = LocalStrings.current
    val start = QuranMeta.JUZ[j - 1]
    val (surah, ayah) = QuranMeta.locate(start)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(surah, ayah, "juz")) }, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStar(j)
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                Text(s.juzName(j), style = MaterialTheme.typography.titleMedium)
                Text(s.ayahRef(start), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(s.pageName(QuranMeta.pageOf(start)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** آية في نتائج البحث أو المحفوظات: الإحالة ونص الآية */
@Composable
fun AyahHit(index: Int, text: Array<String>?, open: (String) -> Unit, bookmarked: Boolean = false) {
    val s = LocalStrings.current
    val (surah, ayah) = QuranMeta.locate(index)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(surah, ayah, "hit")) }, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.98f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahMarker(ayah, Modifier.size(28.dp), 10.sp)
                HSpace(8.dp)
                Text(s.ayahRef(index), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (bookmarked) Icon(Icons.Filled.Bookmark, null, tint = Gold, modifier = Modifier.size(18.dp))
            }
            if (text != null) Text(
                QuranData.withoutBismillah(text, index), Modifier.fillMaxWidth(),
                style = quranStyle(20), maxLines = 3, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** شارة صغيرة لعدد الآيات (تُستعمل في البطاقات) */
@Composable
fun AyahsBadge(n: Int) {
    val s = LocalStrings.current
    Box(Modifier.clip(RoundedCornerShape(50)).background(Gold.copy(alpha = .18f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(s.ayahCount(n), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
