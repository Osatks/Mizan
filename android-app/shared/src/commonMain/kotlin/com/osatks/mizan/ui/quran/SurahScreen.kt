package com.osatks.mizan.ui.quran

import com.osatks.mizan.platform.Platform
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.osatks.mizan.ui.BackHandler
import androidx.compose.foundation.combinedClickable
import com.osatks.mizan.core.AppSettings
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import com.osatks.mizan.ui.IsIos
import com.osatks.mizan.ui.StatusBarStyle
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.drop
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.GlassTone
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.LocalGlassTone
import androidx.compose.ui.graphics.luminance
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.osatks.mizan.ui.SizeClass
import com.osatks.mizan.ui.gutters

/**
 * صفحة السورة. عرض المصحف: صفحات مصحف المدينة (604) تُقلَّب يميناً ويساراً، في رأسها السورة والجزء والحزب
 * وفي أسفلها رقم الصفحة، وكل آية تنتهي بعلامة \u06DD برقمها. عرض آية آية: بطاقة لكل آية مع ترجمة المعاني.
 * الضغط المطوّل على أي آية يُظهر خياراتها: التفسير والاستماع والحفظ والنسخ والمشاركة. الصفحة التي يبقى عليها القارئ ثلاث ثوانٍ تُحسب مقروءة في الختمة.
 * ومعها التلاوة آية آية، ولون الورق والسطوع للقراءة الليلية، وفي وضع التركيز: تقليب الصفحات بلمس طرفيها،
 * وإخفاء الآيات للحفظ.
 */
@Composable
fun SurahScreen(surah: Int, startAyah: Int, from: String, onBack: () -> Unit, openSurah: (Int) -> Unit, startInFocus: Boolean = false, openLibrary: () -> Unit = {}) {
    QuranPrefs.ensure()
    QuranProgress.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    // النص وخط المصحف يُقرآن في الخلفية قبل أول صفحة (إن لم يكونا جاهزين من فهرس المصحف)
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) {
        if (value == null) value = QuranData.uthmani().also { warmMushafFont() }
    }
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
    // صفحة واحدة أو صفحتان متقابلتان بحسب النافذة (iPad بالعرض، والشاشة الداخلية لـ iPhone Duo)
    val window = LocalWindowLayout.current
    val spread = window.mushafSpread
    // رقم الصفحة أصل الحالة: يُحفظ، ويُبنى منه موضع التقليب حين تتغير النافذة (فتح iPhone Duo وطيّه، تدوير iPad،
    // Split View)، فتبقى الصفحة نفسها ظاهرة ولا يقفز القارئ إلى غيرها
    var anchor by rememberSaveable { mutableIntStateOf(QuranMeta.pageOf(startIndex)) }
    val pager = remember(spread) { PagerState(currentPage = Spreads.index(anchor, spread)) { Spreads.count(spread) } }
    val scope = rememberCoroutineScope()
    val count = QuranMeta.COUNTS[listSurah - 1]

    // شريط خيارات الآية (بالضغط المطوّل)
    var actionIdx by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    fun open(i: Int) { actionIdx = null; selIdx = i; sheetIdx = i; QuranPrefs.updateLast(i) }
    fun longPress(i: Int) { selIdx = i; actionIdx = i; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun listItemOf(ayah: Int) = ayah // العنصر 0 هو الترويسة
    fun firstListIndex() = QuranMeta.index(listSurah, list.firstVisibleItemIndex.coerceIn(1, count))
    /** صفحة القارئ في المصحف: الظاهرة (وفي الصفحتين: اليمنى، أو التي كان عليها قبل أن تتسع النافذة) */
    fun page() = Spreads.follow(anchor, pager.currentPage, spread)
    fun currentIndex() = if (mode == "mushaf") QuranMeta.pageRange(page()).first else firstListIndex()
    /** يقلب إلى الصفحة [p] (أو إلى الكتاب المفتوح الذي فيه) */
    suspend fun goToPage(p: Int, animate: Boolean) {
        anchor = p
        val i = Spreads.index(p, spread)
        if (i != pager.currentPage) { if (animate) pager.animateScrollToPage(i) else pager.scrollToPage(i) }
        // ما مرّ به التقليب من مواضع في الطريق لا يغيّر الصفحة المقصودة (اليسرى مثلاً)
        anchor = p
    }

    // الانتقال إلى الآية المطلوبة في عرض آية آية
    var jumped by remember { mutableStateOf(false) }
    LaunchedEffect(text != null) {
        if (text != null && !jumped) { jumped = true; if (mode == "list" && startAyah > 1) list.scrollToItem(listItemOf(startAyah)) }
    }
    // الصفحة المعروضة ثلاث ثوانٍ تُحسب مقروءة (وفي الكتاب المفتوح صفحتاه معاً)
    LaunchedEffect(mode, pager) {
        if (mode == "mushaf") snapshotFlow { pager.currentPage }.collectLatest { p -> delay(3000); Spreads.pages(p, spread).forEach { QuranProgress.markRead(it) } }
        else snapshotFlow { firstListIndex() }.collectLatest { i -> delay(3000); QuranProgress.markRead(QuranMeta.pageOf(i)) }
    }
    // التقليب يغيّر صفحة القارئ؛ أما تغيّر النافذة فيبني موضعاً جديداً فيه الصفحة نفسها فلا يغيّرها
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { i -> anchor = Spreads.follow(anchor, i, spread) } }
    val saveOnExit = rememberUpdatedState { if (sheetIdx == null) QuranPrefs.updateLast(currentIndex()) }
    DisposableEffect(Unit) { onDispose { saveOnExit.value() } }

    // الرأس (السورة والجزء) مشتق من الصفحة المعروضة: لا تُعاد الشاشة كلها مع كل صفحة تُقلَّب (منتصف السحب)،
    // بل حين تتغير السورة أو الجزء فقط
    val head by remember(mode, pager) {
        derivedStateOf {
            val i = if (mode == "mushaf") QuranMeta.pageRange(page()).first else QuranMeta.STARTS[listSurah - 1]
            QuranMeta.locate(i).first to QuranMeta.juzOf(i)
        }
    }
    val headSurah = head.first
    // وضع التركيز: بلا رأس ولا أشرطة نظام، والشاشة مضاءة، والإشعارات صامتة إن سمح المستخدم بذلك
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
    // الرأس الأخضر تحت شريط الحالة: نصه فاتح؛ وفي وضع التركيز يُخفى (iOS؛ وفي أندرويد يخفيه FocusEffects)
    StatusBarStyle(lightContent = true, hidden = focus)
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
    // تلميح الضغط المطوّل: يظهر في أول ثلاث مرات تُفتح فيها السورة
    var ayahHint by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val seen = AppSettings.getInt("ayah_hint")
        if (seen < 3) { AppSettings.putInt("ayah_hint", seen + 1); delay(1200); ayahHint = true; delay(5000); ayahHint = false }
    }
    // تقليب الصفحة يغلق شريط خيارات الآية (يُراقب هنا لا في التركيب، فلا تُعاد الشاشة مع كل صفحة)؛
    // أما تغيّر النافذة فلا يغلقه (يبدأ المراقبة من جديد بعد أول قيمة)
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage to listSurah }.drop(1).collect { actionIdx = null } }
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
        if (!QuietReading.granted() && !QuietReading.asked()) askSilence = true
    }
    // الموضع يُحفظ مع كل صفحة فلا يضيع ولو أُغلق التطبيق، وموضع وضع التركيز وحده لزر «تابع في وضع التركيز»
    LaunchedEffect(mode, focus, text != null, pager) {
        if (text == null) return@LaunchedEffect
        snapshotFlow { currentIndex() }.collectLatest { i ->
            delay(700)
            if (sheetIdx == null) QuranPrefs.updateLast(i)
            if (focus) QuranPrefs.updateFocusLast(i)
        }
    }

    // التلاوة: الآية المتلوة تُظلَّل، والصفحة (أو القائمة) تتبعها؛ في الكتاب المفتوح لا يُقلب إلا بعد الصفحة اليسرى
    LaunchedEffect(mode, pager) {
        snapshotFlow { if (Recitation.active) Recitation.current to Recitation.basmala else null }.collect { cur ->
            val (i, basmala) = cur ?: return@collect
            if (i < 0) return@collect
            selIdx = if (basmala) -1 else i
            if (mode == "mushaf") {
                val pg = QuranMeta.pageOf(i)
                // صفحة الآية المتلوة صفحة القارئ (فتبقى هي الظاهرة إن طُوي iPhone Duo أثناء التلاوة)
                if (Spreads.index(pg, spread) == pager.currentPage) anchor = pg
                else if (!pager.isScrollInProgress) goToPage(pg, animate = true)
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
        if (Recitation.errors != seenErrors) { seenErrors = Recitation.errors; Platform.toast(s.audioError) }
    }
    // تبدأ التلاوة من الآية المختارة إن كانت في الصفحة، وإلا من أول الصفحة
    fun playFrom(): Int =
        if (mode == "mushaf") Spreads.ayahs(pager.currentPage, spread).let { r -> if (selIdx in r) selIdx else QuranMeta.pageRange(page()).first }
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
        if (r == null) Recitation.play(from)
        else Recitation.play(if (at && from in r) from else r.first, r.last)
    }
    fun playOrPause() { if (Recitation.active) Recitation.toggle() else startRange() }
    fun turn(d: Int) { scope.launch { pager.animateScrollToPage((pager.currentPage + d).coerceIn(0, pager.pageCount - 1)) } }
    // اللمس لا يفتح شيئاً حتى لا يشوّش القراءة: يُغلق شريط الخيارات إن كان ظاهراً، ويكشف الآية في الحفظ؛
    // وعلى الشاشات العريضة والتفسير مفتوح بجانب الصفحة، ينتقل التفسير إلى الآية الملموسة
    fun tapAyah(i: Int) {
        when {
            actionIdx != null -> actionIdx = null
            hifzOn && i !in revealed -> revealed = revealed + i
            window.wide && sheetIdx != null -> { sheetIdx = i; selIdx = i; QuranPrefs.updateLast(i) }
        }
    }
    // الاستماع: تُفتح إعدادات التلاوة لتبدأ من الآية المختارة
    fun listenFrom(i: Int) { actionIdx = null; sheetIdx = null; selIdx = i; audioSheet = true }

    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // المشغّل الصغير خارج وضع التركيز يأخذ مكانه أسفل الصفحة فلا يغطي سطورها
    val playerRoom = if (Recitation.active && !focus) 76.dp else 0.dp
    // على الشاشات العريضة: ما يظهر أسفل الشاشة (شريط خيارات الآية والمشغّل والأدوات) يبتعد عن اللوحة الجانبية إن فُتحت
    val panelRoom = if (window.wide && (sheetIdx != null || audioSheet || displaySheet)) readingPanelWidth() else 0.dp
    // الزجاج العائم في شاشة القراءة (شريط خيارات الآية واللوحات والقوائم) يموّه الصفحة التي تحته وحده؛
    // والصفحة مصدر للتمويه ما دام شيء منها ظاهراً فقط (وقليلاً بعده حتى تكتمل حركة خروجه)، فلا كلفة على التقليب غير ذلك
    val readerHaze = rememberHazeState()
    val overlays = actionIdx != null || sheetIdx != null || audioSheet || displaySheet
    var lingering by remember { mutableStateOf(false) }
    LaunchedEffect(overlays) { if (overlays) lingering = true else { delay(450); lingering = false } }
    val glassOn = overlays || lingering
    CompositionLocalProvider(LocalReadingPalette provides readingPalette(QuranPrefs.tone)) {
    val pal = mushafPalette()
    val backdrop = pal.backdrop.takeOrElse { MaterialTheme.colorScheme.surfaceContainerLow }
    // الزجاج داكن فوق الورق الداكن (الليلي والوضع الداكن) وفاتح فوق الفاتح
    CompositionLocalProvider(
        LocalAppHaze provides readerHaze,
        LocalGlassTone provides if (pal.paper.luminance() < .4f) GlassTone.Dark else GlassTone.Light,
    ) {
    Box(Modifier.fillMaxSize().background(backdrop)) {
    // وضع التركيز بلا أشرطة نظام: المحتوى يبتعد عن الكاميرا فقط (displayCutout)، وفي iOS عن المنطقة الآمنة كلها
    // (النتوء أو Dynamic Island تبقى ولو أُخفي شريط الحالة)
    val focusTop = if (IsIos) WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal) else WindowInsets.displayCutout
    val focusBottom = if (IsIos) WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal) else WindowInsets.displayCutout
    Column(
        Modifier.fillMaxSize()
            .then(if (glassOn) Modifier.hazeSource(readerHaze).background(backdrop) else Modifier)
            .then(if (focus) Modifier.windowInsetsPadding(focusTop) else Modifier)
    ) {
        AnimatedVisibility(
            !focus,
            enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeIn(tween(Motion.MEDIUM)),
            exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) + fadeOut(tween(Motion.SHORT)),
        ) {
        PageHeader(
            s.surahName(headSurah),
            "${if (QuranMeta.isMedinan(headSurah)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[headSurah - 1])} · ${s.juzName(head.second)}",
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
                    scope.launch { goToPage(QuranMeta.pageOf(i), animate = false) }
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
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            transitionSpec = {
                (fadeIn(tween(Motion.MEDIUM, 60)) + scaleIn(tween(Motion.MEDIUM, easing = Motion.Decelerate), 0.96f)) togetherWith
                    fadeOut(tween(Motion.SHORT))
            },
            label = "mode"
        ) { m ->
            if (m == "mushaf") {
                // صفحات المصحف تُقلَّب كالكتاب العربي: التالية على اليسار، والصفحة المغادرة تنحسر قليلاً.
                // في الكتاب المفتوح تُقلَّب الصفحتان معاً، والفردية على اليمين (أول ما في الصف بالاتجاه العربي)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val frame = mushafFrame(maxWidth, maxHeight, bottom + playerRoom, spread, book = window.wide && window.heightClass != SizeClass.Compact)
                        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { if (spread) "s$it" else "p$it" }) { i ->
                            // بُعد الصفحة عن موضعها أثناء السحب؛ يُقرأ في طبقات الرسم فقط فلا يعيد تركيب الصفحة مع كل إطار
                            val away = { ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceAtMost(1f) }
                            Box(
                                Modifier.fillMaxSize().graphicsLayer {
                                    val sc = 1f - 0.06f * away()
                                    scaleX = sc; scaleY = sc
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(Modifier.width(frame).fillMaxHeight()) {
                                    for (p in Spreads.pages(i, spread)) {
                                        MushafPage(
                                            p, t, selIdx, bottom + playerRoom, onFrameTap = { if (focus) exitPing++ },
                                            hifz = hifzOn, revealed = revealed, onEdge = if (focus) { d -> turn(d) } else null,
                                            onLongPress = { longPress(it) }, fade = away,
                                            side = if (!spread) PageSide.Single else if (Spreads.isRight(p)) PageSide.Right else PageSide.Left,
                                            prefetch = if (spread) PREFETCH_SPREAD else PREFETCH_EACH_SIDE,
                                            modifier = Modifier.weight(1f),
                                        ) { tapAyah(it) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val paper = mushafPalette()
                // على الشاشات العريضة عمود الآيات في الوسط بعرض مريح، لا أسطر بعرض الشاشة
                val g = gutters(window.width, 680.dp, min = 12.dp)
                LazyColumn(
                    state = list, modifier = Modifier.fillMaxSize().drawBehind { drawPaper(paper) },
                    contentPadding = PaddingValues(start = g.start, end = g.end, top = 16.dp, bottom = 24.dp + bottom + playerRoom),
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
                        AyahItem(listSurah, a, t, trText, translation?.lang?.rtl ?: false, selIdx == idx, size, last = a == count, onLongPress = { longPress(idx) }) { tapAyah(idx) }
                    }
                    item(key = "nav") { SurahNav(listSurah) { n -> listSurah = n; scope.launch { list.scrollToItem(0) } } }
                }
            }
        }
    }
    // وضع التركيز: خط رفيع أعلى الشاشة بقدر ورد اليوم
    if (focus) ReadingProgressLine(Modifier.align(Alignment.TopCenter).windowInsetsPadding(focusTop))
    // تهنئة حين يكتمل ورد اليوم أثناء القراءة
    AnimatedVisibility(
        portionPill, Modifier.align(Alignment.TopCenter).windowInsetsPadding(focusTop).padding(top = if (focus) 60.dp else 140.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + scaleIn(tween(Motion.MEDIUM), 0.85f), exit = fadeOut(tween(Motion.MEDIUM)),
    ) { PortionDonePill() }
    // زر الخروج من وضع التركيز
    AnimatedVisibility(
        showControls, Modifier.align(Alignment.TopCenter).windowInsetsPadding(focusTop).padding(top = 14.dp),
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
        Modifier.align(Alignment.BottomCenter).windowInsetsPadding(focusBottom).padding(start = 12.dp, end = 12.dp + panelRoom, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            hifzHint && hifzOn,
            enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 },
            exit = fadeOut(tween(Motion.MEDIUM)),
        ) { HintPill(s.hifzHint) }
        AnimatedVisibility(
            showControls && actionIdx == null,
            enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
            exit = fadeOut(tween(Motion.MEDIUM)) + slideOutVertically(tween(Motion.MEDIUM)) { it },
        ) {
            val onPage = Spreads.ayahs(pager.currentPage, spread)
            FocusBar(
                hifzAvailable = mode == "mushaf", hifz = hifzOn,
                canReveal = onPage.any { it !in revealed }, canHide = revealed.isNotEmpty(),
                onPlay = { exitPing++; playOrPause() },
                onSkip = { d -> exitPing++; Recitation.skip(d) },
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
        Recitation.active && !focus, Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp + panelRoom, bottom = bottom + 10.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
        exit = fadeOut(tween(Motion.SHORT)) + slideOutVertically(tween(Motion.MEDIUM)) { it },
    ) {
        MiniPlayer(onPlay = { playOrPause() }, onStop = { Recitation.stop() })
    }
    val t = text
    // تلميح لأول مرات: الضغط المطوّل على الآية يُظهر خياراتها
    AnimatedVisibility(
        ayahHint && actionIdx == null && sheetIdx == null,
        Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp + panelRoom, bottom = bottom + playerRoom + 24.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 },
        exit = fadeOut(tween(Motion.MEDIUM)),
    ) { HintPill(s.ayahActionsHint) }
    // شريط خيارات الآية
    var barIdx by remember { mutableIntStateOf(0) }
    actionIdx?.let { barIdx = it }
    BackHandler(enabled = actionIdx != null) { actionIdx = null }
    AnimatedVisibility(
        actionIdx != null && t != null,
        // على الشاشات العريضة بعرض أقصاه 560 في الوسط، لا شريطاً بعرض الشاشة
        Modifier.align(Alignment.BottomCenter).padding(start = 10.dp, end = 10.dp + panelRoom, bottom = bottom + playerRoom + 10.dp).widthIn(max = 560.dp),
        enter = fadeIn(tween(Motion.MEDIUM)) + slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it },
        exit = fadeOut(tween(Motion.SHORT)) + slideOutVertically(tween(Motion.MEDIUM)) { it },
    ) {
        if (t != null) AyahActionBar(
            barIdx, t,
            onTafsir = { open(barIdx) },
            onListen = { listenFrom(barIdx) },
            onClose = { actionIdx = null },
        )
    }
    // إعدادات التلاوة والعرض في لوحات داخل الشاشة أيضاً
    ReadingPanel(audioSheet, onDismiss = { audioSheet = false }, paper = false) {
        AudioOptions(
            onChanged = { if (Recitation.active) startRange(Recitation.current.coerceAtLeast(0), at = true) },
            onStop = { Recitation.stop(); audioSheet = false },
            from = playFrom(),
            onPlay = { startRange(); audioSheet = false },
        )
    }
    ReadingPanel(displaySheet, onDismiss = { displaySheet = false }, paper = false) { DisplayOptions(showTextSize = mode != "mushaf") }
    // التفسير في لوحة داخل الشاشة نفسها، فلا تتغير الصفحة خلفها
    var panelIdx by remember { mutableIntStateOf(0) }
    sheetIdx?.let { panelIdx = it }
    ReadingPanel(sheetIdx != null && t != null, onDismiss = { sheetIdx = null }) {
        if (t != null) {
            val (sn, an) = QuranMeta.locate(panelIdx)
            TafsirContent(
                sn, an, t,
                onAyah = { a ->
                    val i = QuranMeta.index(sn, a)
                    sheetIdx = i; selIdx = i
                    QuranPrefs.updateLast(i)
                    scope.launch {
                        if (mode == "mushaf") goToPage(QuranMeta.pageOf(i), animate = true)
                        else if (sn == listSurah) list.animateScrollToItem(listItemOf(a))
                    }
                },
                onLibrary = { sheetIdx = null; openLibrary() },
                onListen = { a -> listenFrom(QuranMeta.index(sn, a)) },
            )
        }
    }
    }
    }
    }

    // إسكات الإشعارات: يُسأل المستخدم مرة واحدة، ويمنح الإذن من إعدادات أندرويد
    if (askSilence) com.osatks.mizan.ui.PlatformDialog(
        onDismissRequest = { askSilence = false; QuietReading.markAsked() },
        title = s.silenceTitle,
        message = s.silenceBody,
        actions = listOf(
            com.osatks.mizan.ui.DialogAction(s.notNow, com.osatks.mizan.ui.ActionRole.Cancel) { askSilence = false; QuietReading.markAsked() },
            com.osatks.mizan.ui.DialogAction(s.allow, com.osatks.mizan.ui.ActionRole.Preferred) { askSilence = false; QuietReading.requestAccess() },
        ),
    )

}

/**
 * صفحة واحدة من مصحف المدينة على ورق دافئ بإطار مذهّب: في رأسها اسم السورة والجزء والحزب،
 * وتحتها نص الصفحة مضبوطاً من الطرفين، وفي أسفلها رقم الصفحة في ميدالية تجلس على الإطار.
 */
@Composable
internal fun MushafPage(
    page: Int, text: Array<String>, selected: Int, bottomInset: androidx.compose.ui.unit.Dp, onFrameTap: () -> Unit,
    hifz: Boolean = false, revealed: Set<Int> = emptySet(), onEdge: ((Int) -> Unit)? = null,
    onLongPress: ((Int) -> Unit)? = null, fade: () -> Float = { 0f }, side: PageSide = PageSide.Single,
    prefetch: Int = PREFETCH_EACH_SIDE, modifier: Modifier = Modifier, onTap: (Int) -> Unit,
) {
    val p = mushafPalette()
    // الصفحة المغادرة تخبو نحو لون ما حولها: غشاء بلون الخلفية فوق الورقة، بدل شفافية الصفحة كلها
    // (الشفافية ترسم الصفحة بظلها في ذاكرة وسيطة مع كل إطار من السحب)
    val veil = p.backdrop.takeOrElse { MaterialTheme.colorScheme.surfaceContainerLow }
    val first = QuranMeta.pageRange(page).first
    val (firstSurah, _) = QuranMeta.locate(first)
    val read = remember(QuranProgress.readCount, page) { QuranProgress.isRead(page) }
    val range = QuranMeta.pageRange(page)
    val marked = QuranPrefs.bookmarks.any { it in range }
    val ribbon by animateFloatAsState(if (marked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "ribbon")
    // في الكتاب المفتوح: الزاويتان عند الكعب (بين الصفحتين) أصغر، والهامش بينهما ضيق
    val shape = when (side) {
        PageSide.Single -> RoundedCornerShape(18.dp)
        PageSide.Right -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
        PageSide.Left -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
    }
    val padStart = if (side == PageSide.Left) SPINE_GAP else 12.dp
    val padEnd = if (side == PageSide.Right) SPINE_GAP else 12.dp
    val frameTap = rememberUpdatedState(onFrameTap)
    val edge = rememberUpdatedState(onEdge)
    val head = TextStyle(
        fontFamily = QuranFont, fontSize = 15.sp, lineHeight = 22.sp, textDirection = TextDirection.Rtl,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        modifier.fillMaxSize()
            .padding(start = padStart, end = padEnd, top = 10.dp, bottom = 8.dp + bottomInset)
            .shadow(10.dp, shape, ambientColor = Color(0xFF5C4300), spotColor = Color(0xFF5C4300))
            .clip(shape)
            // في وضع التركيز: لمس طرف الصفحة يقلبها، الأيسر إلى التالية والأيمن إلى السابقة كالكتاب العربي؛
            // وفي الكتاب المفتوح الطرفان الخارجيان فقط (طرف الكعب بين الصفحتين لا يقلب)
            .then(if (onEdge != null) Modifier.pointerInput(side) {
                edgeTaps(left = side != PageSide.Right, right = side != PageSide.Left) { d -> edge.value?.invoke(d) }
            } else Modifier)
            // لمس الإطار أو الرأس أو أسفل الصفحة (خارج النص) يُظهر أدوات وضع التركيز
            .pointerInput(Unit) { detectTapGestures { frameTap.value() } }
            .drawBehind {
                drawPaper(p)
                if (side != PageSide.Single) drawSpine(innerAtLeft = side == PageSide.Right)
                drawPageFrame(p)
                if (ribbon > 0f) drawRibbon(p, ribbon)
            }
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
        MushafCanvas(page, text, selected, onTap, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), hifz, revealed, onLongPress, prefetch)
    }
    // رقم الصفحة في وسط خطَّي الإطار السفليين
    PageMedallion(page, read, Modifier.align(Alignment.BottomCenter).padding(bottom = MEDALLION_CENTER - MEDALLION_SIZE / 2))
    Spacer(
        Modifier.matchParentSize()
            .graphicsLayer { alpha = 0.4f * fade(); compositingStrategy = CompositingStrategy.ModulateAlpha }
            .background(veil)
    )
    }
}

/** عرض طرف الصفحة الذي يقلبها لمسُه، نسبةً إلى عرضها */
private const val EDGE = 0.14f

/** الهامش بين صفحتي الكتاب المفتوح من كل جهة (عند الكعب) */
private val SPINE_GAP = 3.dp

/** نسبة عرض صفحة المصحف المطبوع إلى ارتفاعها (بإطارها) */
private const val PAGE_ASPECT = 0.7f

/**
 * عرض ما يُرسم من المصحف في المساحة المتاحة. على الهاتف (أو نافذة قصيرة) العرض كله كما كان.
 * وعلى الشاشات العريضة [book]: صفحة بنسبة المصحف المطبوع في الوسط، أو صفحتان متقابلتان [spread] بالنسبة نفسها،
 * لا ورقة بعرض الشاشة.
 */
internal fun mushafFrame(width: Dp, height: Dp, bottomInset: Dp, spread: Boolean, book: Boolean): Dp {
    if (!book && !spread) return width
    val pageW = (height - 18.dp - bottomInset) * PAGE_ASPECT
    return if (spread) minOf((width - (12.dp + SPINE_GAP) * 2) / 2, pageW) * 2 + (12.dp + SPINE_GAP) * 2
    else minOf(width - 24.dp, pageW) + 24.dp
}

/**
 * لمسة قصيرة على طرف الصفحة تقلبها: dir = 1 للتالية (الطرف الأيسر) و -1 للسابقة (الأيمن).
 * تُلتقط قبل النص فلا تفتح التفسير، أما السحب فيبقى لتقليب الصفحات المعتاد. [left] و[right]: الطرفان المفعّلان.
 */
private suspend fun PointerInputScope.edgeTaps(left: Boolean = true, right: Boolean = true, onEdge: (Int) -> Unit) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val zone = size.width * EDGE
    val dir = when {
        left && down.position.x < zone -> 1
        right && down.position.x > size.width - zone -> -1
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
    selected: Boolean, size: Int, last: Boolean, onLongPress: () -> Unit = {}, onTap: () -> Unit,
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).combinedClickable(onLongClick = onLongPress, onClick = onTap)
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
