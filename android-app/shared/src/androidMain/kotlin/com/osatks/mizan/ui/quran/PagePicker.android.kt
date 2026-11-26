package com.osatks.mizan.ui.quran

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranPages
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.RollingText
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** ظل الورقة كظل صفحة المصحف (MushafPage) */
private val PaperShadow = Color(0xFF5C4300)

/**
 * اختيار صفحة من مصحف المدينة بطريقة لطيفة: بطاقة على ورق المصحف (بلون الورق الذي اختاره القارئ، والوضعان ثلاثيا الأبعاد
 * كذلك) بإطاره المذهّب المزدوج ونجومه، وفي وسطها رقم الصفحة كبيراً في ميدالية ذهبية (تمتلئ بالذهب إن قُرئت في الختمة)،
 * لمسه يكتب الرقم بلوحة الأرقام؛ وعن جانبيه − و + (الضغط المستمر يكرّرهما ويسرع)، وتحته سور الصفحة وجزؤها وحزبها تتبدل
 * مع الاختيار، وشريط من 1 إلى 604 بنقرة خفيفة مع كل صفحة وأقوى حين يعبر بداية جزء، وشرائح [quick] (آخر قراءة والورد)
 * وبدايات الأجزاء، وزر [actionLabel]. [compact]: في لوحة القارئ (ميدالية أصغر وبلا ظل).
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
    val info = remember(page) { QuranPages.info(page) }
    val read = remember(page, QuranProgress.readCount) { QuranProgress.isRead(page) }
    val speech = s.pageSpeech(info, rtl)
    // الضغط المستمر على − و + يستدعي الانتقال مرات قبل أن تتركّب الشاشة من جديد: يبدأ دائماً من آخر صفحة مختارة
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
    Column(
        modifier.fillMaxWidth()
            .then(if (compact) Modifier else Modifier.shadow(6.dp, shape, ambientColor = PaperShadow, spotColor = PaperShadow))
            .clip(shape)
            .drawBehind { drawPaper(p); drawPickerFrame(p) }
            .padding(horizontal = 22.dp, vertical = if (compact) 18.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
    ) {
        // − والرقم و +: يتبعان اتجاه اللغة كالشريط (الصفحة الأولى في اليمين بالعربية)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            StepCircle(Icons.Filled.Remove, s.prevPage, page > 1, p) { move(current - 1) }
            PageDial(page, read, compact, speech) { move(it) }
            StepCircle(Icons.Filled.Add, s.nextPage, page < QuranPages.COUNT, p) { move(current + 1) }
        }
        Text(
            s.pageSummary(info, rtl), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = p.ink, maxLines = 3,
        )
        Spacer(Modifier.fillMaxWidth().height(10.dp).drawBehind { drawRule(size.height / 2, size.width * 0.15f, size.width * 0.85f, p) })
        // الشريط: كل خطوة صفحة (قارئ الشاشة يزيد صفحة وينقص صفحة)، بلا علامات للخطوات الكثيرة
        Slider(
            value = page.toFloat(),
            onValueChange = { move(it.roundToInt()) },
            valueRange = 1f..QuranPages.COUNT.toFloat(),
            steps = QuranPages.COUNT - 2,
            colors = SliderDefaults.colors(
                thumbColor = p.goldDeep, activeTrackColor = p.gold, inactiveTrackColor = p.gold.copy(alpha = .22f),
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = s.goToPage; stateDescription = speech },
        )
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
 * رقم الصفحة كبيراً في ميدالية كميدالية رقم الصفحة في المصحف: حلقتان ذهبيتان ونجمة ثمانية، وتمتلئ بالذهب إن قُرئت الصفحة.
 * لمسها يجعلها حقلاً يُكتب فيه الرقم بلوحة الأرقام، وكل رقم صحيح (1–604) يُختار وهو يُكتب. يكبر مع مقاس الخط حتى مرة ونصف.
 */
@Composable
private fun PageDial(page: Int, read: Boolean, compact: Boolean, speech: String, onType: (Int) -> Unit) {
    val s = LocalStrings.current
    val p = mushafPalette()
    val density = LocalDensity.current
    val grow = density.fontScale.coerceIn(1f, 1.5f)
    val dial = (if (compact) 100.dp else 128.dp) * grow
    val style = TextStyle(fontSize = ((if (compact) 42f else 54f) * grow / density.fontScale).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    var editing by remember { mutableStateOf(false) }
    val fill by animateFloatAsState(if (read) 1f else 0f, tween(Motion.MEDIUM), label = "dial")
    Box(
        Modifier.size(dial)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(p.paper, r)
                drawCircle(p.fillDeep.copy(alpha = .6f * fill), r)
                drawCircle(p.goldDeep, r - 1.dp.toPx(), style = Stroke(1.6.dp.toPx()))
                drawCircle(p.gold.copy(alpha = .7f), r - 5.dp.toPx(), style = Stroke(0.8.dp.toPx()))
                drawStar8(center, r - 6.dp.toPx(), p.gold.copy(alpha = .3f + .4f * fill), 1.dp.toPx(), rings = false)
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
 * زر − أو + دائري بحلقة ذهبية: لمسة صفحة واحدة، والضغط المستمر يكرّره ويسرع شيئاً فشيئاً.
 * الخطوة الأولى عند رفع الإصبع (لا عند وضعه) فلا يقلب الصفحة من يبدأ تمرير القائمة من فوقه.
 */
@Composable
private fun StepCircle(icon: ImageVector, label: String, enabled: Boolean, p: MushafPalette, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(dampingRatio = 0.6f, stiffness = 600f), label = "step")
    Box(
        Modifier.size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(p.fill.copy(alpha = if (enabled) 1f else .45f))
            .border(1.dp, p.goldDeep.copy(alpha = if (enabled) .8f else .3f), CircleShape)
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
        Icon(icon, null, Modifier.size(22.dp), tint = if (enabled) p.goldDeep else p.muted.copy(alpha = .5f))
    }
}

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
