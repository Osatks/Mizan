package com.osatks.mizan.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.delay

/** فهرس المصحف: متابعة القراءة، والبحث في السور والآيات، والسور والأجزاء والمحفوظات */
@Composable
fun QuranScreen(open: (String) -> Unit) {
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    var tab by rememberSaveable { mutableStateOf("surahs") }
    var q by rememberSaveable { mutableStateOf("") }
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    // خط المصحف يُقرأ ويُهيَّأ تشكيله في الخلفية قبل فتح أي سورة، فلا يتحمله أول ظهور للصفحة
    LaunchedEffect(Unit) { warmMushafFont() }

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

    // على الشاشة الواسعة: متابعة القراءة والختمة متجاورتان، والسور والأجزاء ونتائج البحث في شبكة بعمودين
    ToolPage(s.quranTitle, s.quranSubtitle, sharedKey = "quran", icon = Icons.AutoMirrored.Filled.MenuBook, spacing = 10.dp, columns = true) {
        if (QuranPrefs.last >= 0 && q.isBlank()) item(key = "continue") { ContinueCard(QuranPrefs.last, open) }
        if (q.isBlank()) side(key = "khatma") { KhatmaCard(open) }
        if (q.isBlank()) item(key = "evidence") { com.osatks.mizan.ui.screens.EvidenceCard(com.osatks.mizan.i18n.Evidence.quran) }
        full(key = "search") {
            QuranSearchField(q) { q = it }
        }
        if (q.isNotBlank()) {
            if (ref != null) full(key = "ref") { AyahHit(ref, text, open) }
            if (surahHits.isNotEmpty()) {
                full(key = "hs") { GroupTitle(s.searchQuranGroup) }
                cells(surahHits.size, { "s${surahHits[it]}" }) { SurahRow(surahHits[it], lang, open) }
            }
            val hits = ayahHits
            if (hits == null) full(key = "busy") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
            } else if (hits.isNotEmpty()) {
                full(key = "ha") { GroupTitle("${s.searchAyahsGroup} (${hits.size})") }
                cells(hits.size, { "a${hits[it]}" }) { AyahHit(hits[it], text, open) }
            } else if (surahHits.isEmpty() && ref == null) full(key = "none") { Note(s.searchNoResults(q)) }
        } else {
            full(key = "tabs") {
                Segmented(listOf("surahs" to s.surahsTab, "juz" to s.juzTab, "saved" to s.bookmarksTab), tab) { tab = it }
            }
            when (tab) {
                "surahs" -> cells(QuranMeta.SURAHS, { "s${it + 1}" }) { SurahRow(it + 1, lang, open) }
                "juz" -> cells(30, { "j${it + 1}" }) { JuzRow(it + 1, open) }
                else -> if (QuranPrefs.bookmarks.isEmpty()) full(key = "nomarks") { Note(s.noBookmarks) }
                else QuranPrefs.bookmarks.toList().let { marks -> cells(marks.size, { "b${marks[it]}" }) { AyahHit(marks[it], text, open, bookmarked = true) } }
            }
        }
    }
}

/** حقل البحث بطراز iPhone: بطاقة بيضاء بحافة رفيعة، وعدسة بلون التطبيق، وزر مسح دائري */
@Composable
private fun QuranSearchField(q: String, onChange: (String) -> Unit) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = q, onValueChange = onChange, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
        cursorBrush = SolidColor(cs.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 54.dp).background(cs.surface, shape).border(1.dp, cs.outlineVariant, shape)
                    .padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(22.dp), tint = cs.primary)
                HSpace(10.dp)
                Box(Modifier.weight(1f).padding(vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text(
                        s.searchQuranHint, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    inner()
                }
                if (q.isNotEmpty()) IconButton(onClick = { onChange("") }, Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Cancel, s.clear, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
private fun GroupTitle(t: String) {
    Text(t, Modifier.padding(start = 6.dp, top = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

/**
 * صف السورة: رقمها في نجمة تحيط بها حلقة ذهبية بقدر ما قُرئ منها، واسمها، ونوعها وعدد آياتها،
 * واسمها بخط المصحف (أو صفحة بدايتها في الواجهة العربية).
 */
@Composable
fun SurahRow(n: Int, lang: Lang, open: (String) -> Unit) {
    val s = LocalStrings.current
    val src = remember { MutableInteractionSource() }
    val progress = remember(n, QuranProgress.readCount) { surahProgress(n) }
    Surface(
        onClick = { open(surahRoute(n)) }, shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)),
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().sharedCard("row-$n", RoundedCornerShape(18.dp)).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStar(n, 42.dp, progress)
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                if (lang == Lang.AR) Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(21).copy(lineHeight = 34.sp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                else Text(s.surahName(n), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (lang != Lang.AR) Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(22), color = MaterialTheme.colorScheme.primary)
            else Text(
                s.pageName(QuranMeta.pageOf(QuranMeta.STARTS[n - 1])), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold
            )
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
