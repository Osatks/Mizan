package com.osatks.mizan.ui.quran

import android.animation.ValueAnimator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranPages
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** ظل الورقة كظل صفحة المصحف (MushafPage) */
private val PaperShadow = Color(0xFF5C4300)

/**
 * اختيار صفحة من مصحف المدينة بطريقة مميزة: بطاقة على ورق المصحف (بلون الورق الذي اختاره القارئ، والليلي وثلاثي الأبعاد
 * كذلك) بإطاره المذهّب المزدوج ونجومه. في أعلاها الصفحة نفسها مصغّرة: إطارها ورأسها الحقيقي (السورة في اليمين، والجزء
 * والحزب في اليسار) وأول سطرين منها بخط المصحف، تتبدل بلطف مع الاختيار؛ وعلى خطها السفلي رقم الصفحة كبيراً في ميدالية
 * ذهبية يدور كعدّاد (تمتلئ بالذهب إن قُرئت في الختمة)، لمسه يكتب الرقم بلوحة الأرقام، وعن جانبيه − و + نجمتان مذهّبتان
 * (الضغط المستمر يكرّرهما ويسرع). تحتها سور الصفحة وجزؤها وحزبها بلغة الواجهة، ثم «حافة المصحف»: حواف أوراق المصحف
 * مرصوصة وبينها خط ذهبي عند بداية كل جزء وعلامات صغيرة للأحزاب، وفوقها أشرطة الأجزاء (لمسها يقفز إلى بداية الجزء)،
 * والمؤشر شريط علامة ذهبي بشرّابة؛ بنقرة خفيفة مع كل صفحة وأقوى عند حدود الأجزاء. ثم شرائح [quick] (آخر قراءة والورد)
 * وبدايات الأجزاء، وزر [actionLabel]. [compact]: في لوحة القارئ (أصغر وبلا ظل).
 */
@Composable
internal fun PagePicker(
    page: Int, onPage: (Int) -> Unit, actionLabel: String, onAction: (Int) -> Unit, compact: Boolean,
    quick: List<Pair<String, Int>> = emptyList(), modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val rtl = LocalLang.current.rtl
    val p = mushafPalette()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val info = remember(page) { QuranPages.info(page) }
    val read = remember(page, QuranProgress.readCount) { QuranProgress.isRead(page) }
    val speech = s.pageSpeech(info, rtl)
    // الضغط المستمر على − و + والسحب على الحافة يستدعيان الانتقال مرات قبل أن تتركّب الشاشة من جديد: يبدأ دائماً من آخر صفحة مختارة
    val current by rememberUpdatedState(page)
    val pick by rememberUpdatedState(onPage)
    fun move(to: Int) {
        val from = current
        val v = to.coerceIn(1, QuranPages.COUNT)
        if (v == from) return
        haptic.performHapticFeedback(if (crossesJuz(from, v)) HapticFeedbackType.ContextClick else HapticFeedbackType.SegmentFrequentTick)
        pick(v)
    }
    val shape = RoundedCornerShape(if (compact) 22.dp else 26.dp)
    // الميدالية تكبر مع مقاس الخط حتى مرة ونصف، ومركزها على خطَّي إطار الصفحة المصغّرة السفليين
    val dial = (if (compact) 84.dp else 104.dp) * density.fontScale.coerceIn(1f, 1.5f)
    Column(
        modifier.fillMaxWidth()
            .then(if (compact) Modifier else Modifier.shadow(6.dp, shape, ambientColor = PaperShadow, spotColor = PaperShadow))
            .clip(shape)
            .drawBehind { drawPaper(p); drawPickerFrame(p) }
            .padding(horizontal = 20.dp, vertical = if (compact) 18.dp else 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            MiniPagePreview(page, compact, dial, Modifier.fillMaxWidth().padding(bottom = dial / 2 - MEDALLION_CENTER))
            // − والرقم و +: يتبعان اتجاه اللغة كالحافة (الصفحة الأولى في اليمين بالعربية)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StepStar(Icons.Filled.Remove, s.prevPage, page > 1, p) { move(current - 1) }
                PageDial(page, read, compact, dial, speech) { move(it) }
                StepStar(Icons.Filled.Add, s.nextPage, page < QuranPages.COUNT, p) { move(current + 1) }
            }
        }
        Text(
            s.pageSummary(info, rtl), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = p.ink, maxLines = 3,
        )
        // حافة المصحف: كل خطوة صفحة (قارئ الشاشة يزيد صفحة وينقص صفحة)
        MushafEdge(page, speech, s.goToPage) { move(it) }
        // الشرائح: آخر قراءة والورد، ثم بدايات الأجزاء؛ تمرّ إلى جزء الصفحة المختارة
        val juz = QuranPages.juzByStart(page)
        val chips = rememberLazyListState()
        LaunchedEffect(juz) { chips.animateScrollToItem((quick.size + juz - 2).coerceAtLeast(0)) }
        LazyRow(
            Modifier.fillMaxWidth(), state = chips,
            horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
        ) {
            items(quick.size, key = { "q$it" }) { i ->
                val (label, target) = quick[i]
                ChipButton("$label · $target", false) { move(target) }
            }
            items(JUZ_START_PAGES.size, key = { "j$it" }) { i ->
                ChipButton(s.juzName(i + 1), juz == i + 1) { move(JUZ_START_PAGES[i]) }
            }
        }
        Button(
            onClick = { onAction(current) }, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** إطار البطاقة كإطار صفحة المصحف: خط ذهبي عريض وآخر رفيع بداخله، وفي زوايا الداخلي نجوم ثمانية */
private fun DrawScope.drawPickerFrame(p: MushafPalette) {
    val o = 7.dp.toPx(); val i = 11.dp.toPx()
    drawRoundRect(p.goldDeep.copy(alpha = .8f), Offset(o, o), Size(size.width - 2 * o, size.height - 2 * o), CornerRadius(16.dp.toPx()), style = Stroke(1.6.dp.toPx()))
    drawRoundRect(p.gold.copy(alpha = .7f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius(12.dp.toPx()), style = Stroke(0.8.dp.toPx()))
    val r = 5.dp.toPx(); val c = i + 1.dp.toPx()
    for (pt in listOf(Offset(c, c), Offset(size.width - c, c), Offset(c, size.height - c), Offset(size.width - c, size.height - c))) {
        drawCircle(p.paper, r * 0.9f, pt)
        drawStar8(pt, r, p.goldDeep, 1.dp.toPx(), rings = false)
        drawCircle(p.goldDeep, r * 0.22f, pt)
    }
}

/**
 * الصفحة المختارة مصغّرة كما في المصحف: الورقة والإطار المزدوج ونجومه، والرأس الحقيقي (السورة في اليمين، والجزء والحزب
 * في اليسار) وتحته الخط ذو المعيّن، ثم أول سطرين منها بخط المصحف (نص المصحف المضمَّن، بلا تنزيل)؛ تتبدل بتلاشٍ قصير مع
 * الاختيار. أسفلها مساحة نصف الميدالية [dial] التي تجلس على خطَّي إطارها السفليين.
 */
@Composable
private fun MiniPagePreview(page: Int, compact: Boolean, dial: Dp, modifier: Modifier) {
    val p = mushafPalette()
    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { if (value == null) value = runCatching { QuranData.uthmani() }.getOrNull() }
    val head = TextStyle(
        fontFamily = QuranFont, fontSize = (if (compact) 13 else 14).sp, lineHeight = 20.sp, textDirection = TextDirection.Rtl,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    Box(
        modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = PaperShadow, spotColor = PaperShadow)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind { drawPaper(p); drawPageFrame(p) }
            .clearAndSetSemantics {},
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = MEDALLION_CENTER + dial / 2 + 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // الرأس من اليمين إلى اليسار كما في الصفحة نفسها، مهما كانت لغة الواجهة
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(mushafHeadTitle(page), Modifier.weight(1f), style = head, color = p.goldDeep, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text(mushafHeadSubtitle(page), style = head.copy(fontSize = head.fontSize * 0.86f), color = p.muted, maxLines = 1)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(10.dp).drawBehind { drawRule(size.height / 2, 0f, size.width, p) })
            val qSize = if (compact) 18 else 20
            // أثناء السحب السريع يتبدل النص كل جزء من عُشر الثانية على الأكثر (بآخر صفحة)، فلا تتراكم التلاشيات
            val latest by rememberUpdatedState(page)
            var shown by remember { mutableIntStateOf(page) }
            LaunchedEffect(Unit) { snapshotFlow { latest }.collect { shown = it; delay(90) } }
            val opening = remember(shown, text) { text?.let { QuranPages.opening(it, shown, if (compact) 90 else 120) } ?: "" }
            Crossfade(opening, Modifier.fillMaxWidth().padding(top = 4.dp), animationSpec = tween(160), label = "opening") { o ->
                Text(
                    o, Modifier.fillMaxWidth(), style = quranStyle(qSize, TextAlign.Center), color = p.ink,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2,
                )
            }
        }
    }
}

/**
 * رقم الصفحة كبيراً في ميدالية كميدالية رقم الصفحة في المصحف: حلقتان ذهبيتان ونجمة ثمانية، وتمتلئ بالذهب إن قُرئت الصفحة.
 * الرقم يدور خانةً خانة كالعدّاد. لمسها يجعلها حقلاً يُكتب فيه الرقم بلوحة الأرقام، وكل رقم صحيح (1–604) يُختار وهو يُكتب.
 */
@Composable
private fun PageDial(page: Int, read: Boolean, compact: Boolean, dial: Dp, speech: String, onType: (Int) -> Unit) {
    val s = LocalStrings.current
    val p = mushafPalette()
    val density = LocalDensity.current
    val grow = density.fontScale.coerceIn(1f, 1.5f)
    val style = TextStyle(fontSize = ((if (compact) 34f else 42f) * grow / density.fontScale).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    var editing by remember { mutableStateOf(false) }
    val fill by animateFloatAsState(if (read) 1f else 0f, tween(Motion.MEDIUM), label = "dial")
    Box(
        Modifier.size(dial)
            .shadow(6.dp, CircleShape, ambientColor = PaperShadow, spotColor = PaperShadow)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(p.paper, p.paper, p.fill), center, r), r)
                drawCircle(p.fillDeep.copy(alpha = .65f * fill), r)
                drawCircle(p.goldDeep, r - 1.dp.toPx(), style = Stroke(1.8.dp.toPx()))
                drawCircle(p.gold.copy(alpha = .75f), r - 5.dp.toPx(), style = Stroke(0.8.dp.toPx()))
                drawStar8(center, r - 7.dp.toPx(), p.gold.copy(alpha = .28f + .4f * fill), 1.dp.toPx(), rings = false)
                // ثماني نقاط ذهبية على الحلقة كزخرفة الميداليات في المصاحف
                for (k in 0 until 8) {
                    rotate(k * 45f, center) { drawCircle(p.goldDeep, 1.3.dp.toPx(), Offset(center.x, center.y - r + 3.dp.toPx())) }
                }
            }
            .clip(CircleShape)
            .then(
                if (editing) Modifier
                else Modifier.semantics { contentDescription = speech }.clickable(onClickLabel = s.typePageHint) { editing = true }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (editing) PageField(page, style.copy(color = p.goldDeep), onType) { editing = false }
        else RollingText("$page", style, p.goldDeep)
    }
}

/** حقل رقم الصفحة: لوحة الأرقام، ويُقبل الرقم بالأرقام العربية المشرقية والفارسية أيضاً؛ «تم» أو مغادرته تعيده رقماً */
@Composable
private fun PageField(page: Int, style: TextStyle, onType: (Int) -> Unit, onDone: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf("$page".let { TextFieldValue(it, TextRange(0, it.length)) }) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() }; keyboard?.show() }
    BasicTextField(
        value = value,
        onValueChange = { v ->
            val digits = v.text.mapNotNull { latinDigit(it) }.joinToString("").take(3)
            value = TextFieldValue(digits, TextRange(digits.length))
            digits.toIntOrNull()?.takeIf { it in 1..QuranPages.COUNT }?.let(onType)
        },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(style.color),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); onDone() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).focusRequester(focus)
            .onFocusChanged { f -> if (focused && !f.isFocused) onDone(); focused = f.isFocused },
    )
}

/** الرقم بالأرقام اللاتينية من أي من الأرقام الثلاثة، أو null لغير الأرقام */
private fun latinDigit(ch: Char): Char? = when (ch) {
    in '0'..'9' -> ch
    in '٠'..'٩' -> '0' + (ch - '٠')
    in '۰'..'۹' -> '0' + (ch - '۰')
    else -> null
}

/**
 * زر − أو + على نجمة ثمانية مذهّبة (مربعان متراكبان كعلامة الحزب في المصحف): لمسة صفحة واحدة، والضغط المستمر يكرّره
 * ويسرع شيئاً فشيئاً. الخطوة الأولى عند رفع الإصبع (لا عند وضعه) فلا يقلب الصفحة من يبدأ تمرير القائمة من فوقه.
 */
@Composable
private fun StepStar(icon: ImageVector, label: String, enabled: Boolean, p: MushafPalette, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.84f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "step")
    val turn by animateFloatAsState(if (pressed) 22.5f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "stepTurn")
    Box(
        Modifier.size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                val a = if (enabled) 1f else .45f
                val side = size.minDimension * 0.66f
                val tl = Offset(center.x - side / 2, center.y - side / 2)
                rotate(turn, center) {
                    for (deg in listOf(0f, 45f)) rotate(deg, center) {
                        drawRoundRect(p.fill.copy(alpha = a), tl, Size(side, side), CornerRadius(3.dp.toPx()))
                    }
                    drawStar8(center, side * 0.7071f, p.goldDeep.copy(alpha = if (enabled) .85f else .3f), 1.1.dp.toPx(), rings = false)
                }
                drawCircle(p.gold.copy(alpha = .55f * a), side * 0.36f, style = Stroke(0.8.dp.toPx()))
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                if (!enabled) disabled()
                onClick { if (on) step(); on }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!on) return@awaitEachGesture
                    pressed = true
                    var repeated = false
                    val job = scope.launch {
                        delay(420)
                        var gap = 120L
                        while (on) {
                            repeated = true
                            step()
                            delay(gap)
                            gap = (gap * 0.86f).toLong().coerceAtLeast(35L)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    job.cancel()
                    pressed = false
                    if (up != null && !repeated) step()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) p.goldDeep else p.muted.copy(alpha = .5f))
    }
}

/** ارتفاع أشرطة الأجزاء فوق الحافة، والحافة نفسها، وما تحتها للشرّابة */
private val RIBBONS = 18.dp
private val EDGE = 30.dp
private val TASSEL = 22.dp

/**
 * «حافة المصحف»: حواف أوراق المصحف مرصوصة بلون الورق وظلالها (الصفحة الأولى في اليمين بالعربية)، وخط ذهبي أثخن عند بداية
 * كل جزء وعلامة صغيرة عند كل حزب، وفوقها أشرطة الأجزاء الثلاثين (شريط الجزء المختار أعرض وفيه رقمه؛ لمس الأشرطة يقفز إلى
 * بداية أقرب جزء)، والمؤشر شريط علامة ذهبي تتدلى منه شرّابة. السحب يختار صفحة صفحة؛ وقارئ الشاشة يراه شريط تمرير يزيد
 * صفحة وينقص صفحة ويقرأ «الصفحة 173، الأعراف، الجزء 9».
 */
@Composable
private fun MushafEdge(page: Int, speech: String, label: String, onPick: (Int) -> Unit) {
    val p = mushafPalette()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pick by rememberUpdatedState(onPick)
    val measurer = rememberTextMeasurer()
    val hizbPages = remember { (1..60).filter { it % 2 == 0 }.map { QuranPages.hizbStartPage(it) } }
    val juz = QuranPages.juzByStart(page)
    // المؤشر ينساب إلى الصفحة الجديدة (يتبع الإصبع أثناء السحب بلا تأخير يُحسّ)
    val shown by animateFloatAsState(page.toFloat(), spring(dampingRatio = 0.8f, stiffness = 1400f), label = "edgeThumb")
    val inset = 12.dp
    fun DrawScope.xOf(pg: Float): Float {
        val x0 = inset.toPx(); val x1 = size.width - inset.toPx()
        val f = (pg - 1f) / (QuranPages.COUNT - 1)
        return if (rtl) x1 - f * (x1 - x0) else x0 + f * (x1 - x0)
    }
    fun pageAt(x: Float, width: Float, density: Float): Int {
        val x0 = inset.value * density; val x1 = width - inset.value * density
        val f = ((x - x0) / (x1 - x0)).coerceIn(0f, 1f)
        return (1 + (if (rtl) 1f - f else f) * (QuranPages.COUNT - 1)).roundToInt()
    }
    Spacer(
        Modifier.fillMaxWidth().height(RIBBONS + EDGE + TASSEL)
            .semantics {
                contentDescription = label
                stateDescription = speech
                progressBarRangeInfo = ProgressBarRangeInfo(page.toFloat(), 1f..QuranPages.COUNT.toFloat(), QuranPages.COUNT - 2)
                setProgress { v -> pick(v.roundToInt().coerceIn(1, QuranPages.COUNT)); true }
            }
            .pointerInput(rtl) {
                detectTapGestures { o ->
                    val pg = pageAt(o.x, size.width.toFloat(), density)
                    // لمس الأشرطة فوق الحافة يقفز إلى بداية أقرب جزء
                    if (o.y < RIBBONS.toPx()) pick(JUZ_START_PAGES.minBy { abs(it - pg) }) else pick(pg)
                }
            }
            .pointerInput(rtl) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> pick(pageAt(o.x, size.width.toFloat(), density)) },
                ) { change, _ ->
                    change.consume()
                    pick(pageAt(change.position.x, size.width.toFloat(), density))
                }
            }
            .drawWithCache {
                val top = RIBBONS.toPx(); val bottom = top + EDGE.toPx()
                val x0 = inset.toPx(); val x1 = size.width - inset.toPx()
                // حواف الأوراق: خطوط رفيعة متقاربة بدرجتين من الظل، كأوراق مرصوصة
                val light = Path(); val dark = Path()
                var x = x0 + 1f
                var n = 0
                val step = 1.6.dp.toPx()
                while (x < x1 - 1f) {
                    val path = if ((n * 7 + n / 3) % 5 < 2) dark else light
                    path.moveTo(x, top + 2.dp.toPx()); path.lineTo(x, bottom - 2.dp.toPx())
                    x += step; n++
                }
                val block = Brush.verticalGradient(listOf(p.paperEdge, p.paper, p.paper, p.paperEdge), top, bottom)
                val number = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                onDrawBehind {
                    // كتلة الأوراق وظلها
                    drawRoundRect(Color.Black.copy(alpha = .10f), Offset(x0, top + 2.dp.toPx()), Size(x1 - x0, bottom - top), CornerRadius(5.dp.toPx()))
                    drawRoundRect(block, Offset(x0, top), Size(x1 - x0, bottom - top), CornerRadius(5.dp.toPx()))
                    drawPath(light, p.ink.copy(alpha = .07f), style = Stroke(0.6.dp.toPx()))
                    drawPath(dark, p.ink.copy(alpha = .14f), style = Stroke(0.6.dp.toPx()))
                    drawRoundRect(p.gold.copy(alpha = .55f), Offset(x0, top), Size(x1 - x0, bottom - top), CornerRadius(5.dp.toPx()), style = Stroke(0.8.dp.toPx()))
                    // علامات الأحزاب في أسفل الحافة
                    for (h in hizbPages) {
                        val hx = xOf(h.toFloat())
                        drawLine(p.goldDeep.copy(alpha = .6f), Offset(hx, bottom - 7.dp.toPx()), Offset(hx, bottom - 1.dp.toPx()), 1.dp.toPx())
                    }
                    // بدايات الأجزاء: خط ذهبي أثخن، وفوقه شريط الجزء
                    for ((i, start) in JUZ_START_PAGES.withIndex()) {
                        val jx = xOf(start.toFloat())
                        val on = i + 1 == juz
                        if (i > 0) drawLine(p.gold, Offset(jx, top), Offset(jx, bottom), 1.6.dp.toPx())
                        val w = if (on) 15.dp.toPx() else 5.dp.toPx()
                        val h = if (on) RIBBONS.toPx() else RIBBONS.toPx() * 0.55f
                        val notch = if (on) 4.dp.toPx() else 2.dp.toPx()
                        // الشريط يتدلى من أعلى الحافة إلى أعلى (معكوس: أصله عند الحافة وطرفه المشقوق في الأعلى)
                        val left = (jx - w / 2).coerceIn(0f, size.width - w)
                        val ribbon = Path().apply {
                            moveTo(left, top); lineTo(left + w, top); lineTo(left + w, top - h)
                            lineTo(left + w / 2, top - h + notch); lineTo(left, top - h); close()
                        }
                        drawPath(ribbon, if (on) Brush.verticalGradient(listOf(p.gold, p.goldDeep), top - h, top) else Brush.verticalGradient(listOf(p.gold.copy(alpha = .35f), p.gold.copy(alpha = .7f)), top - h, top))
                        if (on) {
                            val t = measurer.measure("${i + 1}", number)
                            drawText(t, p.paper, Offset(left + (w - t.size.width) / 2, top - h + notch + (h - notch - t.size.height) / 2))
                        }
                    }
                    // المؤشر: شريط علامة ذهبي يمرّ فوق الحافة وتحتها، وشرّابة تتدلى منه
                    val tx = xOf(shown)
                    val rw = 10.dp.toPx()
                    val rTop = top - 6.dp.toPx(); val rBottom = bottom + 8.dp.toPx(); val cut = 4.dp.toPx()
                    val mark = Path().apply {
                        moveTo(tx - rw / 2, rTop); lineTo(tx + rw / 2, rTop); lineTo(tx + rw / 2, rBottom)
                        lineTo(tx, rBottom - cut); lineTo(tx - rw / 2, rBottom); close()
                    }
                    translateShadow(mark, 1.5.dp.toPx())
                    drawPath(mark, Brush.verticalGradient(listOf(p.gold, p.goldDeep, p.gold), rTop, rBottom))
                    drawLine(p.paper.copy(alpha = .5f), Offset(tx - rw * 0.22f, rTop + 1.dp.toPx()), Offset(tx - rw * 0.22f, rBottom - cut * 1.3f), 0.8.dp.toPx())
                    drawRoundRect(p.goldDeep, Offset(tx - rw / 2, rTop), Size(rw, rBottom - rTop), CornerRadius(1.dp.toPx()), style = Stroke(0.6.dp.toPx()))
                    // الشرّابة: خيط وعقدة وأهداب
                    val knotY = rBottom + 5.dp.toPx()
                    drawLine(p.goldDeep, Offset(tx, rBottom - cut), Offset(tx, knotY), 1.dp.toPx())
                    drawCircle(p.goldDeep, 2.6.dp.toPx(), Offset(tx, knotY))
                    for (f in -2..2) {
                        drawLine(p.goldDeep.copy(alpha = .9f), Offset(tx + f * 0.8.dp.toPx(), knotY + 2.dp.toPx()), Offset(tx + f * 1.8.dp.toPx(), knotY + 9.dp.toPx()), 0.9.dp.toPx())
                    }
                }
            }
    )
}

/** ظل لين تحت شكل (منقول قليلاً إلى الأسفل) */
private fun DrawScope.translateShadow(path: Path, d: Float) {
    val shadow = Path().apply { addPath(path, Offset(d * 0.6f, d)) }
    drawPath(shadow, Color.Black.copy(alpha = .18f))
}

/** «إزالة الحركات» في أندرويد: مدة الحركات صفر في خيارات المطوّرين أو إمكانية الوصول */
internal actual fun systemReduceMotion(): Boolean = Motion.reduced || !ValueAnimator.areAnimatorsEnabled()

/** الأجزاء الثلاثون في شبكة صغيرة بصفحاتها؛ جزء الصفحة المختارة مظلَّل، ولمس جزء يختار صفحة بدايته */
@Composable
internal fun JuzPagesGrid(page: Int, onPage: (Int) -> Unit) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    val chosen = QuranPages.juzByStart(page)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = (maxWidth / 112.dp).toInt().coerceIn(2, 6)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                s.juzTab, Modifier.padding(start = 6.dp, top = 4.dp), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            )
            for (row in (1..30).chunked(cols)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (j in row) {
                        val range = QuranPages.juzPages(j)
                        val on = j == chosen
                        Surface(
                            onClick = {
                                if (!on) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onPage(range.first)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (on) Gold else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.weight(1f).fillMaxHeight().semantics { selected = on },
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    s.juzName(j), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                    color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(s.pageSpan(range.first, range.last), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** لوحة «انتقل إلى صفحة» في القارئ: عنوانها، والاختيار المصغّر، و«اذهب» */
@Composable
internal actual fun PageJumpContent(page: Int, onGo: (Int) -> Unit) {
    val s = LocalStrings.current
    var pick by remember(page) { mutableIntStateOf(page) }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.FindInPage, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(12.dp))
            Text(s.goToPage, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        PagePicker(pick, { pick = it }, s.goButton, onGo, compact = true)
    }
}
