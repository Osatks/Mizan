package com.osatks.mizan.ui.quran

import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import com.osatks.mizan.quran.Recitation
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
import androidx.compose.foundation.layout.width

/**
 * صفحة السورة. عرض المصحف: صفحات مصحف المدينة (604) تُقلَّب يميناً ويساراً، في رأسها السورة والجزء والحزب
 * وفي أسفلها رقم الصفحة، وكل آية تنتهي بعلامة \u06DD برقمها. عرض آية آية: بطاقة لكل آية مع ترجمة المعاني.
 * الضغط على أي آية يفتح تفسيرها. الصفحة التي يبقى عليها القارئ ثلاث ثوانٍ تُحسب مقروءة في الختمة.
 * ومعها التلاوة آية آية، ولون الورق والسطوع للقراءة الليلية، وفي وضع التركيز: تقليب الصفحات بلمس طرفيها،
 * وإخفاء الآيات للحفظ.
 */
@Composable
fun SurahScreen(surah: Int, startAyah: Int, from: String, onBack: () -> Unit, openSurah: (Int) -> Unit, startInFocus: Boolean = false, openLibrary: () -> Unit = {}) {
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
    // «تابع في وضع التركيز» يفتح الصفحة نفسها دون تظليل آية
    var selIdx by remember { mutableIntStateOf(if (startAyah > 0 && from != "focus") startIndex else -1) }
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
    var showControls by remember { mutableStateOf(startInFocus) }
    var audioSheet by remember { mutableStateOf(false) }
    var displaySheet by remember { mutableStateOf(false) }
    // الحفظ (في المصحف داخل وضع التركيز): الآيات مخفية حتى يكشفها القارئ
    var hifz by rememberSaveable { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(emptySet<Int>()) }
    val hifzOn = hifz && focus && mode == "mushaf"
    FocusEffects(focus)
    ReadingBrightness()
    RecitationLifecycle()
    BackHandler(enabled = focus) { focus = false }
    // أزرار وضع التركيز تظهر قليلاً عند الدخول، وعند لمس إطار الصفحة أو استعمالها، وتبقى ما دامت ورقة إعداد مفتوحة
    val sheetOpen = audioSheet || displaySheet
    // في الحفظ تبقى الأدوات ظاهرة: الشريط في هامش الصفحة لا يغطي السطور
    LaunchedEffect(focus, exitPing, sheetOpen, hifzOn) {
        showControls = focus
        if (focus && !sheetOpen && !hifzOn) { delay(3500); showControls = false }
    }
    // ورد اليوم يكتمل أثناء القراءة: تهنئة قصيرة
    var portionPill by remember { mutableStateOf(false) }
    val portionReached = QuranProgress.dailyGoal().let { it > 0 && QuranProgress.todayCount >= it }
    var wasReached by remember { mutableStateOf(portionReached) }
    LaunchedEffect(portionReached) {
        if (portionReached && !wasReached) { portionPill = true; delay(3200); portionPill = false }
        wasReached = portionReached
    }
    var hifzHint by remember { mutableStateOf(false) }
    LaunchedEffect(hifzHint) { if (hifzHint) { delay(4500); hifzHint = false } }
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
    // الموضع يُحفظ مع كل صفحة فلا يضيع ولو أُغلق التطبيق، وموضع وضع التركيز وحده لزر «تابع في وضع التركيز»
    LaunchedEffect(mode, focus, text != null) {
        if (text == null) return@LaunchedEffect
        snapshotFlow { currentIndex() }.collectLatest { i ->
            delay(700)
            if (sheetIdx == null) QuranPrefs.updateLast(i)
            if (focus) QuranPrefs.updateFocusLast(i)
        }
    }

    // التلاوة: الآية المتلوة تُظلَّل، والصفحة (أو القائمة) تتبعها
    LaunchedEffect(mode) {
        snapshotFlow { if (Recitation.active) Recitation.current to Recitation.basmala else null }.collect { cur ->
            val (i, basmala) = cur ?: return@collect
            if (i < 0) return@collect
            selIdx = if (basmala) -1 else i
            if (mode == "mushaf") {
                val pg = QuranMeta.pageOf(i) - 1
                if (pg != pager.currentPage && !pager.isScrollInProgress) pager.animateScrollToPage(pg)
            } else {
                val (sn, an) = QuranMeta.locate(i)
                if (sn != listSurah) { listSurah = sn; list.scrollToItem(listItemOf(an)) }
                else {
                    val info = list.layoutInfo
                    val item = info.visibleItemsInfo.firstOrNull { it.index == listItemOf(an) }
                    if (item == null || item.offset < 0 || item.offset + item.size > info.viewportEndOffset) list.animateScrollToItem(listItemOf(an))
                }
            }
        }
    }
    var seenErrors by remember { mutableIntStateOf(Recitation.errors) }
    LaunchedEffect(Recitation.errors) {
        if (Recitation.errors != seenErrors) { seenErrors = Recitation.errors; Toast.makeText(ctx, s.audioError, Toast.LENGTH_LONG).show() }
    }
    // تبدأ التلاوة من الآية المختارة إن كانت في الصفحة، وإلا من أول الصفحة
    fun playFrom(): Int =
        if (mode == "mushaf") QuranMeta.pageRange(pager.currentPage + 1).let { r -> if (selIdx in r) selIdx else r.first }
        else if (selIdx >= 0 && QuranMeta.locate(selIdx).first == listSurah) selIdx else firstListIndex()
    // المقطع المختار: متواصل من الآية الحالية، أو هذه الصفحة، أو هذه السورة، أو من آية إلى آية
    // at = true: يبدأ من الآية from داخل المقطع (عند تغيير الإعداد أثناء التلاوة، أو «استمع من هذه الآية»)
    fun startRange(from: Int = playFrom(), at: Boolean = false) {
        val r = when (QuranPrefs.rangeMode) {
            "page" -> QuranMeta.pageRange(QuranMeta.pageOf(from))
            "surah" -> QuranMeta.locate(from).first.let { sn -> QuranMeta.STARTS[sn - 1]..(QuranMeta.STARTS[sn - 1] + QuranMeta.COUNTS[sn - 1] - 1) }
            "custom" -> QuranPrefs.customFrom..QuranPrefs.customTo
            else -> null
        }
        if (r == null) Recitation.play(ctx, from)
        else Recitation.play(ctx, if (at && from in r) from else r.first, r.last)
    }
    fun playOrPause() { if (Recitation.active) Recitation.toggle() else startRange() }
    fun turn(d: Int) { scope.launch { pager.animateScrollToPage((pager.currentPage + d).coerceIn(0, QuranMeta.PAGES.size - 1)) } }
    fun tapAyah(i: Int) { if (hifzOn && i !in revealed) revealed = revealed + i else open(i) }

    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // المشغّل الصغير خارج وضع التركيز يأخذ مكانه أسفل الصفحة فلا يغطي سطورها
    val playerRoom = if (Recitation.active && !focus) 76.dp else 0.dp
    CompositionLocalProvider(LocalReadingPalette provides readingPalette(QuranPrefs.tone)) {
    val pal = mushafPalette()
    Box(Modifier.fillMaxSize().background(pal.backdrop.takeOrElse { MaterialTheme.colorScheme.surfaceContainerLow })) {
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
            // لون الصفحة والسطوع (ومقاس الخط في عرض آية آية؛ صفحة المصحف تختار مقاسها لتملأ الصفحة)
            IconButton(onClick = { displaySheet = true }) { Icon(Icons.Filled.FormatSize, s.displayTitle, tint = Color.White) }
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
        // التقدم والمواظبة تحت الرأس (يختفي مع الرأس في وضع التركيز)
        AnimatedVisibility(!focus, enter = expandVertically(tween(Motion.MEDIUM)) + fadeIn(), exit = shrinkVertically(tween(Motion.MEDIUM)) + fadeOut()) {
            ReadingProgressBar(Modifier.padding(top = 4.dp))
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
                        ) {
                            MushafPage(
                                p + 1, t, selIdx, bottom + playerRoom, onFrameTap = { if (focus) exitPing++ },
                                hifz = hifzOn, revealed = revealed, onEdge = if (focus) ::turn else null,
                            ) { tapAyah(it) }
                        }
                    }
                }
            } else {
                val paper = mushafPalette()
                LazyColumn(
                    state = list, modifier = Modifier.fillMaxSize().drawBehind { drawPaper(paper) },
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 24.dp + bottom + playerRoom),
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
    // وضع التركيز: خط رفيع أعلى الشاشة بقدر ورد اليوم
    if (focus) ReadingProgressLine(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout))
    // تهنئة حين يكتمل ورد اليوم أثناء القراءة
    AnimatedVisibility(
        portionPill, Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout).padding(top = if (focus) 60.dp else 140.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + scaleIn(tween(Motion.MEDIUM), 0.85f), exit = fadeOut(tween(Motion.MEDIUM)),
    ) { PortionDonePill() }
    // زر الخروج من وضع التركيز
    AnimatedVisibility(
        showControls, Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout).padding(top = 14.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { -it },
        exit = fadeOut(tween(Motion.MEDIUM)) + slideOutVertically(tween(Motion.MEDIUM)) { -it },
    ) {
        Surface(onClick = { focus = false }, shape = CircleShape, color = FocusChrome, contentColor = Color.White, border = FocusEdge) {
            Row(Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FullscreenExit, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(s.exitFocus, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    // أدوات وضع التركيز في الأسفل: التلاوة والحفظ والعرض
    Column(
        Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.displayCutout).padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            hifzHint && hifzOn,
            enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 },
            exit = fadeOut(tween(Motion.MEDIUM)),
        ) { HintPill(s.hifzHint) }
        AnimatedVisibility(
            showControls,
            enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
            exit = fadeOut(tween(Motion.MEDIUM)) + slideOutVertically(tween(Motion.MEDIUM)) { it },
        ) {
            val onPage = QuranMeta.pageRange(pager.currentPage + 1)
            FocusBar(
                hifzAvailable = mode == "mushaf", hifz = hifzOn,
                canReveal = onPage.any { it !in revealed }, canHide = revealed.isNotEmpty(),
                onPlay = { exitPing++; playOrPause() },
                onSkip = { d -> exitPing++; Recitation.skip(ctx, d) },
                onAudio = { audioSheet = true },
                onHifz = { exitPing++; hifz = !hifz; hifzHint = hifz; if (!hifz) revealed = emptySet() },
                onDisplay = { displaySheet = true },
                onRevealNext = { onPage.firstOrNull { it !in revealed }?.let { revealed = revealed + it } },
                onHideAll = { revealed = emptySet() },
            )
        }
    }
    // المشغّل الصغير خارج وضع التركيز
    AnimatedVisibility(
        Recitation.active && !focus, Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = bottom + 10.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
        exit = fadeOut(tween(Motion.SHORT)) + slideOutVertically(tween(Motion.MEDIUM)) { it },
    ) {
        MiniPlayer(onPlay = { playOrPause() }, onStop = { Recitation.stop() })
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
    if (audioSheet) ReadingSheet({ audioSheet = false }) {
        AudioOptions(
            onChanged = { if (Recitation.active) startRange(Recitation.current.coerceAtLeast(0), at = true) },
            onStop = { Recitation.stop(); audioSheet = false },
            from = playFrom(),
            onPlay = { startRange(); audioSheet = false },
        )
    }
    if (displaySheet) ReadingSheet({ displaySheet = false }) { DisplayOptions(showTextSize = mode != "mushaf") }

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
            onDismiss = { sheetIdx = null },
            onLibrary = { sheetIdx = null; openLibrary() },
            onListen = { a -> sheetIdx = null; startRange(QuranMeta.index(sn, a), at = true) },
        )
    }
}

/**
 * صفحة واحدة من مصحف المدينة على ورق دافئ بإطار مذهّب: في رأسها اسم السورة والجزء والحزب،
 * وتحتها نص الصفحة مضبوطاً من الطرفين، وفي أسفلها رقم الصفحة في ميدالية تجلس على الإطار.
 */
@Composable
internal fun MushafPage(
    page: Int, text: Array<String>, selected: Int, bottomInset: androidx.compose.ui.unit.Dp, onFrameTap: () -> Unit,
    hifz: Boolean = false, revealed: Set<Int> = emptySet(), onEdge: ((Int) -> Unit)? = null, onTap: (Int) -> Unit,
) {
    val p = mushafPalette()
    val first = QuranMeta.pageRange(page).first
    val (firstSurah, _) = QuranMeta.locate(first)
    val read = remember(QuranProgress.readCount, page) { QuranProgress.isRead(page) }
    val range = QuranMeta.pageRange(page)
    val marked = QuranPrefs.bookmarks.any { it in range }
    val ribbon by animateFloatAsState(if (marked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "ribbon")
    val shape = RoundedCornerShape(18.dp)
    val frameTap = rememberUpdatedState(onFrameTap)
    val edge = rememberUpdatedState(onEdge)
    val head = TextStyle(
        fontFamily = QuranFont, fontSize = 15.sp, lineHeight = 22.sp, textDirection = TextDirection.Rtl,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        Modifier.fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp + bottomInset)
            .shadow(10.dp, shape, ambientColor = Color(0xFF5C4300), spotColor = Color(0xFF5C4300))
            .clip(shape)
            // في وضع التركيز: لمس طرف الصفحة يقلبها، الأيسر إلى التالية والأيمن إلى السابقة كالكتاب العربي
            .then(if (onEdge != null) Modifier.pointerInput(Unit) { edgeTaps { d -> edge.value?.invoke(d) } } else Modifier)
            // لمس الإطار أو الرأس أو أسفل الصفحة (خارج النص) يُظهر أدوات وضع التركيز
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
        MushafCanvas(page, text, selected, onTap, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), hifz, revealed)
    }
    // رقم الصفحة في وسط خطَّي الإطار السفليين
    PageMedallion(page, read, Modifier.align(Alignment.BottomCenter).padding(bottom = MEDALLION_CENTER - MEDALLION_SIZE / 2))
    }
}

/** عرض طرف الصفحة الذي يقلبها لمسُه، نسبةً إلى عرضها */
private const val EDGE = 0.14f

/**
 * لمسة قصيرة على طرف الصفحة تقلبها: dir = 1 للتالية (الطرف الأيسر) و -1 للسابقة (الأيمن).
 * تُلتقط قبل النص فلا تفتح التفسير، أما السحب فيبقى لتقليب الصفحات المعتاد.
 */
private suspend fun PointerInputScope.edgeTaps(onEdge: (Int) -> Unit) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val zone = size.width * EDGE
    val dir = when {
        down.position.x < zone -> 1
        down.position.x > size.width - zone -> -1
        else -> return@awaitEachGesture
    }
    val slop = viewConfiguration.touchSlop
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
        if ((change.position - down.position).getDistance() > slop) return@awaitEachGesture
        if (!change.pressed) {
            if (change.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) { change.consume(); onEdge(dir) }
            return@awaitEachGesture
        }
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
            appendAyahEnd(ayah)
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
            Text(body, Modifier.fillMaxWidth(), style = quranStyle(size), color = p.ink, inlineContent = ayahEndContent(size.sp, p.marker, p.ink))
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
