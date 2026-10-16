package com.osatks.mizan.ui.quran

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.PageHeader
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.width

/**
 * صفحة السورة. عرض المصحف: صفحات مصحف المدينة (604) تُقلَّب يميناً ويساراً، في رأسها السورة والجزء والحزب
 * وفي أسفلها رقم الصفحة، وكل آية تنتهي بعلامة \u06DD برقمها. عرض آية آية: بطاقة لكل آية مع ترجمة المعاني.
 * الضغط على أي آية يفتح تفسيرها. الصفحة التي يبقى عليها القارئ ثلاث ثوانٍ تُحسب مقروءة في الختمة.
 */
@Composable
fun SurahScreen(surah: Int, startAyah: Int, from: String, onBack: () -> Unit, openSurah: (Int) -> Unit, startInFocus: Boolean = false) {
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
    // وضع التركيز: بلا رأس ولا أشرطة نظام، والشاشة مضاءة، والإشعارات صامتة إن سمح المستخدم بذلك
    val ctx = LocalContext.current
    var focus by rememberSaveable { mutableStateOf(startInFocus) }
    var askSilence by remember { mutableStateOf(false) }
    var exitPing by remember { mutableIntStateOf(0) }
    var showExit by remember { mutableStateOf(false) }
    FocusEffects(focus)
    BackHandler(enabled = focus) { focus = false }
    // زر الخروج يظهر قليلاً عند الدخول، وعند لمس إطار الصفحة، وعند التمرير لأعلى في عرض آية آية
    LaunchedEffect(focus, exitPing) {
        showExit = focus
        if (focus) { delay(3000); showExit = false }
    }
    LaunchedEffect(focus, mode) {
        if (focus && mode == "list") {
            var last = list.firstVisibleItemIndex * 100_000 + list.firstVisibleItemScrollOffset
            snapshotFlow { list.firstVisibleItemIndex * 100_000 + list.firstVisibleItemScrollOffset }.collect { v ->
                if (v < last - 60) exitPing++
                last = v
            }
        }
    }
    fun enterFocus() {
        focus = true
        if (!QuietReading.granted(ctx) && !QuietReading.asked()) askSilence = true
    }

    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
    Column(Modifier.fillMaxSize().then(if (focus) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier)) {
        AnimatedVisibility(
            !focus,
            enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeIn(tween(Motion.MEDIUM)),
            exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeOut(tween(Motion.SHORT)),
        ) {
        PageHeader(
            s.surahName(headSurah),
            "${if (QuranMeta.isMedinan(headSurah)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[headSurah - 1])} · ${s.juzName(QuranMeta.juzOf(headIndex))}",
            onBack, cardKey = "$from-$surah"
        ) {
            IconButton(onClick = { enterFocus() }) { Icon(Icons.Filled.Fullscreen, s.focusMode, tint = Color.White) }
            // صفحة المصحف تختار مقاسها لتملأ الصفحة؛ التكبير والتصغير لعرض آية آية
            if (mode != "mushaf") {
                // مقاس الخط في زر واحد يفتح قائمة صغيرة (−  المقاس  +)
                var sizeMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { sizeMenu = true }) { Icon(Icons.Filled.FormatSize, "${s.textSmaller} / ${s.textLarger}", tint = Color.White) }
                    DropdownMenu(sizeMenu, onDismissRequest = { sizeMenu = false }, shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { QuranPrefs.updateSize(size - 2) }, enabled = size > 18) { Icon(Icons.Filled.TextDecrease, s.textSmaller) }
                            Text("$size", Modifier.width(40.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { QuranPrefs.updateSize(size + 2) }, enabled = size < 40) { Icon(Icons.Filled.TextIncrease, s.textLarger) }
                        }
                    }
                }
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
        // التبديل بين المصحف وعرض آية آية بتلاشٍ وتكبير خفيف
        AnimatedContent(
            mode,
            transitionSpec = {
                (fadeIn(tween(Motion.MEDIUM, 60)) + scaleIn(tween(Motion.MEDIUM, easing = Motion.Decelerate), 0.96f)) togetherWith
                    fadeOut(tween(Motion.SHORT))
            },
            label = "mode"
        ) { m ->
            if (m == "mushaf") {
                // صفحات المصحف تُقلَّب كالكتاب العربي: التالية على اليسار، والصفحة المغادرة تنحسر قليلاً
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { it }) { p ->
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                val o = ((pager.currentPage - p) + pager.currentPageOffsetFraction).absoluteValue.coerceAtMost(1f)
                                val sc = 1f - 0.06f * o
                                scaleX = sc; scaleY = sc
                                alpha = 1f - 0.4f * o
                            }
                        ) { MushafPage(p + 1, t, selIdx, bottom, onFrameTap = { if (focus) exitPing++ }) { open(it) } }
                    }
                }
            } else {
                val paper = mushafPalette()
                LazyColumn(
                    state = list, modifier = Modifier.fillMaxSize().drawBehind { drawPaper(paper) },
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp + bottom),
                ) {
                    item(key = "banner") {
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            SurahCartouche(QuranMeta.NAMES_AR[listSurah - 1])
                            if (QuranData.hasBismillahHeader(listSurah)) {
                                Text(
                                    t[0], Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center,
                                    style = quranStyle(26, TextAlign.Center), color = paper.ink
                                )
                            }
                            Text(s.quranSubtitle, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = paper.muted)
                        }
                    }
                    items(count, key = { "a${listSurah}_${it + 1}" }) { i ->
                        val a = i + 1
                        val idx = QuranMeta.index(listSurah, a)
                        AyahItem(listSurah, a, t, trText, translation?.lang?.rtl ?: false, selIdx == idx, size, last = a == count) { open(idx) }
                    }
                    item(key = "nav") { SurahNav(listSurah) { n -> listSurah = n; scope.launch { list.scrollToItem(0) } } }
                }
            }
        }
    }
    // زر الخروج من وضع التركيز
    AnimatedVisibility(
        showExit, Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout).padding(top = 14.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { -it },
        exit = fadeOut(tween(Motion.MEDIUM)) + slideOutVertically(tween(Motion.MEDIUM)) { -it },
    ) {
        Surface(onClick = { focus = false }, shape = CircleShape, color = Color.Black.copy(alpha = .62f), contentColor = Color.White) {
            Row(Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FullscreenExit, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(s.exitFocus, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    }

    // إسكات الإشعارات: يُسأل المستخدم مرة واحدة، ويمنح الإذن من إعدادات أندرويد
    if (askSilence) AlertDialog(
        onDismissRequest = { askSilence = false; QuietReading.markAsked() },
        title = { Text(s.silenceTitle) },
        text = { Text(s.silenceBody) },
        confirmButton = { TextButton(onClick = { askSilence = false; QuietReading.requestAccess(ctx) }) { Text(s.allow) } },
        dismissButton = { TextButton(onClick = { askSilence = false; QuietReading.markAsked() }) { Text(s.notNow) } },
    )

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
 * صفحة واحدة من مصحف المدينة على ورق دافئ بإطار مذهّب: في رأسها اسم السورة والجزء والحزب،
 * وتحتها نص الصفحة مضبوطاً من الطرفين، وفي أسفلها رقم الصفحة في ميدالية تجلس على الإطار.
 */
@Composable
internal fun MushafPage(page: Int, text: Array<String>, selected: Int, bottomInset: androidx.compose.ui.unit.Dp, onFrameTap: () -> Unit, onTap: (Int) -> Unit) {
    val p = mushafPalette()
    val first = QuranMeta.pageRange(page).first
    val (firstSurah, _) = QuranMeta.locate(first)
    val read = remember(QuranProgress.readCount, page) { QuranProgress.isRead(page) }
    val range = QuranMeta.pageRange(page)
    val marked = QuranPrefs.bookmarks.any { it in range }
    val ribbon by animateFloatAsState(if (marked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "ribbon")
    val shape = RoundedCornerShape(18.dp)
    val frameTap = rememberUpdatedState(onFrameTap)
    val head = TextStyle(
        fontFamily = QuranFont, fontSize = 15.sp, lineHeight = 22.sp, textDirection = TextDirection.Rtl,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        Modifier.fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp + bottomInset)
            .shadow(10.dp, shape, ambientColor = Color(0xFF5C4300), spotColor = Color(0xFF5C4300))
            .clip(shape)
            // لمس الإطار أو الرأس أو أسفل الصفحة (خارج النص) يُظهر زر الخروج من وضع التركيز
            .pointerInput(Unit) { detectTapGestures { frameTap.value() } }
            .drawBehind { drawPaper(p); drawPageFrame(p); if (ribbon > 0f) drawRibbon(p, ribbon) }
    ) {
    Column(
        Modifier.fillMaxSize()
            // النص داخل الإطار، وينتهي فوق رقم الصفحة
            .padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = MEDALLION_CENTER + MEDALLION_SIZE / 2 + 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("سُورَةُ ${QuranMeta.NAMES_AR[firstSurah - 1]}", Modifier.weight(1f), style = head, color = p.goldDeep, maxLines = 1)
            Text(
                "الجزء ${JUZ_ORDINALS[QuranMeta.juzOf(first) - 1]} · الحزب ${arabicDigits(QuranMeta.hizbOf(first))}",
                style = head.copy(fontSize = 13.sp), color = p.muted, maxLines = 1
            )
        }
        Spacer(Modifier.fillMaxWidth().height(10.dp).drawBehind { drawRule(size.height / 2, 0f, size.width, p) })
        MushafCanvas(page, text, selected, onTap, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 2.dp))
    }
    // رقم الصفحة في وسط خطَّي الإطار السفليين
    PageMedallion(page, read, Modifier.align(Alignment.BottomCenter).padding(bottom = MEDALLION_CENTER - MEDALLION_SIZE / 2))
    }
}

/**
 * آية في عرض آية آية: النص بخط المصحف تنتهي بعلامة \u06DD ورقمها، وتحته ترجمة معانيها،
 * على ورق المصحف بلا صناديق؛ تفصل بين الآيات زخرفة رفيعة، والمختارة تُظلَّل.
 */
@Composable
private fun AyahItem(
    surah: Int, ayah: Int, text: Array<String>, translation: Array<String>?, trRtl: Boolean,
    selected: Boolean, size: Int, last: Boolean, onTap: () -> Unit,
) {
    val p = mushafPalette()
    val index = QuranMeta.index(surah, ayah)
    val bg by animateColorAsState(if (selected) p.select else Color.Transparent, tween(Motion.MEDIUM), label = "ayah")
    val body = remember(index, text, p) {
        buildAnnotatedString {
            append(QuranData.withoutBismillah(text, index))
            append('\u00A0')
            withStyle(SpanStyle(color = p.marker)) { append("\u06DD" + arabicDigits(ayah)) }
        }
    }
    Column {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onTap)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (QuranPrefs.isBookmarked(index)) {
                Icon(Icons.Filled.Bookmark, null, tint = p.goldDeep, modifier = Modifier.size(18.dp).align(Alignment.End))
            }
            Text(body, Modifier.fillMaxWidth(), style = quranStyle(size), color = p.ink)
            if (translation != null) {
                Text(
                    translation[index], Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = if (trRtl) TextDirection.Rtl else TextDirection.Ltr, lineHeight = 22.sp
                    ),
                    color = p.muted
                )
            }
        }
        if (!last) OrnamentDivider(Modifier.padding(vertical = 2.dp))
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
