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

/** عدد الآيات في كل فقرة من عرض المصحف */
private const val CHUNK = 6

/**
 * صفحة السورة. عرض المصحف: نص متصل مضبوط الحواف، كل آية فيه قابلة للضغط وتنتهي بنجمة رقمها.
 * عرض آية آية: بطاقة لكل آية وتحتها ترجمة المعاني بلغة الواجهة. الضغط على آية يفتح تفسيرها.
 */
@Composable
fun SurahScreen(surah: Int, startAyah: Int, from: String, onBack: () -> Unit, openSurah: (Int) -> Unit) {
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    val translation = remember(lang) { Sources.translationFor(lang) }
    val trText by produceState(translation?.let { QuranData.cached("tafsir/${it.id}.txt") }, translation) {
        value = translation?.let { QuranData.source(it.id) }
    }
    val count = QuranMeta.COUNTS[surah - 1]
    val mode = QuranPrefs.modeFor(lang)
    val size = QuranPrefs.textSize
    var selected by remember { mutableIntStateOf(startAyah) }
    var sheet by remember { mutableStateOf<Int?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun itemOf(m: String, ayah: Int) = 1 + if (m == "mushaf") (ayah - 1) / CHUNK else ayah - 1
    fun ayahAt(m: String, item: Int) = when {
        item <= 0 -> 1
        m == "mushaf" -> ((item - 1) * CHUNK + 1).coerceAtMost(count)
        else -> item.coerceAtMost(count)
    }
    fun open(a: Int) {
        selected = a; sheet = a
        QuranPrefs.updateLast(QuranMeta.index(surah, a))
    }

    // الانتقال إلى الآية المطلوبة بعد تحميل النص (مرة واحدة)
    var jumped by remember { mutableStateOf(false) }
    LaunchedEffect(text != null) {
        if (text != null && !jumped) {
            jumped = true
            if (startAyah > 1) list.scrollToItem(itemOf(mode, startAyah))
        }
    }
    // حفظ موضع القراءة عند مغادرة الصفحة
    DisposableEffect(surah) {
        onDispose {
            if (sheet == null) QuranPrefs.updateLast(QuranMeta.index(surah, ayahAt(QuranPrefs.modeFor(lang), list.firstVisibleItemIndex)))
        }
    }

    val juz = QuranMeta.juzOf(QuranMeta.STARTS[surah - 1])
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        PageHeader(
            s.surahName(surah),
            "${if (QuranMeta.isMedinan(surah)) s.medinan else s.meccan} · ${s.ayahCount(count)} · ${s.juzName(juz)}",
            onBack, cardKey = "$from-$surah"
        ) {
            IconButton(onClick = { QuranPrefs.updateSize(size - 2) }, enabled = size > 18) { Icon(Icons.Filled.TextDecrease, s.textSmaller, tint = Color.White) }
            IconButton(onClick = { QuranPrefs.updateSize(size + 2) }, enabled = size < 40) { Icon(Icons.Filled.TextIncrease, s.textLarger, tint = Color.White) }
            IconButton(onClick = {
                val a = ayahAt(mode, list.firstVisibleItemIndex)
                val next = if (mode == "mushaf") "list" else "mushaf"
                QuranPrefs.updateMode(next)
                scope.launch { list.scrollToItem(itemOf(next, a)) }
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
        LazyColumn(
            state = list, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 24.dp + bottom),
            verticalArrangement = Arrangement.spacedBy(if (mode == "mushaf") 0.dp else 10.dp)
        ) {
            item(key = "banner") {
                Column(Modifier.padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SurahBanner(QuranMeta.NAMES_AR[surah - 1], if (QuranData.hasBismillahHeader(surah)) t[0] else null)
                    Spacer(Modifier.size(6.dp))
                    Text(s.quranSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (mode == "mushaf") {
                val chunks = (count + CHUNK - 1) / CHUNK
                items(chunks, key = { "c$it" }) { c ->
                    val from = c * CHUNK + 1
                    MushafChunk(surah, from, minOf(count, from + CHUNK - 1), t, selected, size) { open(it) }
                }
            } else {
                items(count, key = { "a${it + 1}" }) { i ->
                    val a = i + 1
                    AyahCard(surah, a, t, trText, translation?.lang?.rtl ?: false, selected == a, size) { open(a) }
                }
            }
            item(key = "nav") { SurahNav(surah, openSurah) }
        }
    }

    val current = sheet
    val t = text
    if (current != null && t != null) {
        TafsirSheet(
            surah, current, t,
            onAyah = { a ->
                sheet = a; selected = a
                QuranPrefs.updateLast(QuranMeta.index(surah, a))
                scope.launch { list.animateScrollToItem(itemOf(mode, a)) }
            },
            onDismiss = { sheet = null }
        )
    }
}

/** فقرة من عرض المصحف: الآيات متصلة، وكل آية رابط يفتح تفسيرها، والمختارة مظللة بالذهبي */
@Composable
private fun MushafChunk(surah: Int, from: Int, to: Int, text: Array<String>, selected: Int, size: Int, onTap: (Int) -> Unit) {
    val ink = MaterialTheme.colorScheme.onSurface
    val annotated = remember(surah, from, to, selected, text) {
        buildAnnotatedString {
            for (a in from..to) {
                val body = QuranData.withoutBismillah(text, QuranMeta.index(surah, a))
                val link = LinkAnnotation.Clickable(
                    "a$a", TextLinkStyles(pressedStyle = SpanStyle(background = Gold.copy(alpha = .22f)))
                ) { onTap(a) }
                if (a == selected) withStyle(SpanStyle(background = Gold.copy(alpha = .26f))) { withLink(link) { append(body) } }
                else withLink(link) { append(body) }
                append(' ')
                appendInlineContent("m$a", "﴿$a﴾")
                append(' ')
            }
        }
    }
    val inline = remember(from, to, size) {
        (from..to).associate { a ->
            "m$a" to InlineTextContent(Placeholder(1.5.em, 1.5.em, PlaceholderVerticalAlign.TextCenter)) {
                AyahMarker(a, Modifier.fillMaxSize(), (size * 0.5f).sp)
            }
        }
    }
    Text(
        annotated, Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        style = quranStyle(size, TextAlign.Justify), color = ink, inlineContent = inline
    )
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
