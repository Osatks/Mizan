package com.osatks.mizan.ui.quran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.arabicDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.PageHeader
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.launch

/**
 * صفحة السورة. عرض المصحف: صفحات مصحف المدينة (604) تُقلَّب يميناً ويساراً، في رأسها السورة والجزء والحزب
 * وفي أسفلها رقم الصفحة، وكل آية تنتهي بعلامة ۝ برقمها. عرض آية آية: بطاقة لكل آية مع ترجمة المعاني.
 * الضغط على أي آية يفتح تفسيرها. الصفحة التي يبقى عليها القارئ ثلاث ثوانٍ تُحسب مقروءة في الختمة.
 */
@Composable
fun SurahScreen(surah: Int, startAyah: Int, from: String, onBack: () -> Unit, openSurah: (Int) -> Unit) {
    QuranPrefs.ensure()
    QuranProgress.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    val translation = remember(lang) { Sources.translationFor(lang) }
    val trText by produceState(translation?.let { QuranData.cached("tafsir/${it.id}.txt") }, translation) {
        value = translation?.let { QuranData.source(it.id) }
    }
    val mode = QuranPrefs.modeFor(lang)
    val size = QuranPrefs.textSize
    val startIndex = QuranMeta.index(surah, startAyah.coerceAtLeast(1))
    var selIdx by remember { mutableIntStateOf(if (startAyah > 0) startIndex else -1) }
    var sheetIdx by remember { mutableStateOf<Int?>(null) }
    var listSurah by remember { mutableIntStateOf(surah) }
    val list = rememberLazyListState()
    val pager = rememberPagerState(initialPage = QuranMeta.pageOf(startIndex) - 1) { QuranMeta.PAGES.size }
    val scope = rememberCoroutineScope()
    val count = QuranMeta.COUNTS[listSurah - 1]

    fun open(i: Int) { selIdx = i; sheetIdx = i; QuranPrefs.updateLast(i) }
    fun listItemOf(ayah: Int) = ayah // العنصر 0 هو الترويسة
    fun firstListIndex() = QuranMeta.index(listSurah, list.firstVisibleItemIndex.coerceIn(1, count))
    fun currentIndex() = if (mode == "mushaf") QuranMeta.pageRange(pager.currentPage + 1).first else firstListIndex()

    // الانتقال إلى الآية المطلوبة في عرض آية آية
    var jumped by remember { mutableStateOf(false) }
    LaunchedEffect(text != null) {
        if (text != null && !jumped) { jumped = true; if (mode == "list" && startAyah > 1) list.scrollToItem(listItemOf(startAyah)) }
    }
    // الصفحة المعروضة ثلاث ثوانٍ تُحسب مقروءة
    LaunchedEffect(mode) {
        if (mode == "mushaf") snapshotFlow { pager.currentPage }.collectLatest { p -> delay(3000); QuranProgress.markRead(p + 1) }
        else snapshotFlow { firstListIndex() }.collectLatest { i -> delay(3000); QuranProgress.markRead(QuranMeta.pageOf(i)) }
    }
    DisposableEffect(Unit) { onDispose { if (sheetIdx == null) QuranPrefs.updateLast(currentIndex()) } }

    val headSurah = if (mode == "mushaf") QuranMeta.locate(QuranMeta.pageRange(pager.currentPage + 1).first).first else listSurah
    val headIndex = if (mode == "mushaf") QuranMeta.pageRange(pager.currentPage + 1).first else QuranMeta.STARTS[listSurah - 1]
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        PageHeader(
            s.surahName(headSurah),
            "${if (QuranMeta.isMedinan(headSurah)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[headSurah - 1])} · ${s.juzName(QuranMeta.juzOf(headIndex))}",
            onBack, cardKey = "$from-$surah"
        ) {
            // صفحة المصحف تختار مقاسها لتملأ الصفحة؛ التكبير والتصغير لعرض آية آية
            if (mode != "mushaf") {
                IconButton(onClick = { QuranPrefs.updateSize(size - 2) }, enabled = size > 18) { Icon(Icons.Filled.TextDecrease, s.textSmaller, tint = Color.White) }
                IconButton(onClick = { QuranPrefs.updateSize(size + 2) }, enabled = size < 40) { Icon(Icons.Filled.TextIncrease, s.textLarger, tint = Color.White) }
            }
            IconButton(onClick = {
                val i = currentIndex()
                if (mode == "mushaf") {
                    val (sn, an) = QuranMeta.locate(i)
                    listSurah = sn
                    QuranPrefs.updateMode("list")
                    scope.launch { list.scrollToItem(listItemOf(an)) }
                } else {
                    QuranPrefs.updateMode("mushaf")
                    scope.launch { pager.scrollToPage(QuranMeta.pageOf(i) - 1) }
                }
            }) {
                Icon(if (mode == "mushaf") Icons.Filled.ViewAgenda else Icons.AutoMirrored.Filled.MenuBook,
                    if (mode == "mushaf") s.ayahView else s.mushafView, tint = Color.White)
            }
        }
        val t = text
        if (t == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(12.dp))
                    Text(s.quranLoading, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Column
        }
        if (mode == "mushaf") {
            // صفحات المصحف تُقلَّب كالكتاب العربي: التالية على اليسار
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { p ->
                    MushafPage(p + 1, t, selIdx, bottom) { open(it) }
                }
            }
        } else {
            LazyColumn(
                state = list, modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 24.dp + bottom),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "banner") {
                    Column(Modifier.padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        SurahBanner(QuranMeta.NAMES_AR[listSurah - 1], if (QuranData.hasBismillahHeader(listSurah)) t[0] else null)
                        Spacer(Modifier.size(6.dp))
                        Text(s.quranSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(count, key = { "a${listSurah}_${it + 1}" }) { i ->
                    val a = i + 1
                    val idx = QuranMeta.index(listSurah, a)
                    AyahCard(listSurah, a, t, trText, translation?.lang?.rtl ?: false, selIdx == idx, size) { open(idx) }
                }
                item(key = "nav") { SurahNav(listSurah) { n -> listSurah = n; scope.launch { list.scrollToItem(0) } } }
            }
        }
    }

    val current = sheetIdx
    val t = text
    if (current != null && t != null) {
        val (sn, an) = QuranMeta.locate(current)
        TafsirSheet(
            sn, an, t,
            onAyah = { a ->
                val i = QuranMeta.index(sn, a)
                sheetIdx = i; selIdx = i
                QuranPrefs.updateLast(i)
                scope.launch {
                    if (mode == "mushaf") pager.animateScrollToPage(QuranMeta.pageOf(i) - 1)
                    else if (sn == listSurah) list.animateScrollToItem(listItemOf(a))
                }
            },
            onDismiss = { sheetIdx = null }
        )
    }
}

/**
 * صفحة واحدة من مصحف المدينة: إطار ذهبي، ورأس فيه اسم السورة والجزء والحزب، وترويسة لكل سورة تبدأ فيها،
 * والآيات متصلة تنتهي كل منها بعلامة ۝ ورقمها، وفي الأسفل رقم الصفحة.
 */
@Composable
private fun MushafPage(page: Int, text: Array<String>, selected: Int, bottomInset: androidx.compose.ui.unit.Dp, onTap: (Int) -> Unit) {
    val s = LocalStrings.current
    val range = QuranMeta.pageRange(page)
    val first = range.first
    val (firstSurah, _) = QuranMeta.locate(first)
    val marker = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp + bottomInset)) {
        Surface(
            shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, Gold.copy(alpha = .55f)), modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سُورَةُ ${QuranMeta.NAMES_AR[firstSurah - 1]}", style = quranStyle(16), color = marker, modifier = Modifier.weight(1f))
                    Text(
                        "${s.juzName(QuranMeta.juzOf(first))} · ${s.hizbName(QuranMeta.hizbOf(first))}",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(6.dp))
                MushafText(page, text, selected, onTap, Modifier.weight(1f).fillMaxWidth())
                Text(
                    "$page", Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** بطاقة آية في عرض آية آية، وتحتها ترجمة المعاني */
@Composable
private fun AyahCard(
    surah: Int, ayah: Int, text: Array<String>, translation: Array<String>?, trRtl: Boolean,
    selected: Boolean, size: Int, onTap: () -> Unit,
) {
    val index = QuranMeta.index(surah, ayah)
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "ayah"
    )
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onTap, shape = RoundedCornerShape(18.dp), color = bg,
        border = BorderStroke(1.dp, if (selected) Gold.copy(alpha = .6f) else MaterialTheme.colorScheme.outlineVariant),
        interactionSource = src, modifier = Modifier.fillMaxWidth().pressScale(src, 0.985f)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AyahMarker(ayah, Modifier.size(34.dp), 12.sp)
                Spacer(Modifier.weight(1f))
                if (QuranPrefs.isBookmarked(index)) Icon(Icons.Filled.Bookmark, null, tint = Gold, modifier = Modifier.size(20.dp))
            }
            Text(QuranData.withoutBismillah(text, index), Modifier.fillMaxWidth(), style = quranStyle(size), color = MaterialTheme.colorScheme.onSurface)
            if (translation != null) {
                Text(
                    translation[index], Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = if (trRtl) TextDirection.Rtl else TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** الانتقال إلى السورة السابقة أو التالية في آخر الصفحة */
@Composable
private fun SurahNav(surah: Int, openSurah: (Int) -> Unit) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (surah > 1) OutlinedButton(onClick = { openSurah(surah - 1) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.prevSurah, style = MaterialTheme.typography.labelSmall)
                Text(s.surahName(surah - 1), maxLines = 1)
            }
        } else Spacer(Modifier.weight(1f))
        if (surah < QuranMeta.SURAHS) OutlinedButton(onClick = { openSurah(surah + 1) }, modifier = Modifier.weight(1f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.nextSurah, style = MaterialTheme.typography.labelSmall)
                Text(s.surahName(surah + 1), maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        } else Spacer(Modifier.weight(1f))
    }
}
