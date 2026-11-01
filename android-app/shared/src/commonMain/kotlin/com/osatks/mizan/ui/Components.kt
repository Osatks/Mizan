package com.osatks.mizan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Switch
import androidx.compose.material3.ripple
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.osatks.mizan.ui.theme.LocalDark
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Strings
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush

/** مساحة أسفل المحتوى: ارتفاع شريط التنقل العائم في صفحات التبويبات */
val LocalBottomInset = compositionLocalOf { 0.dp }

/** رأس الصفحة المتدرج مع زر الرجوع؛ sharedKey يربطه ببطاقة الأداة في الرئيسية */
@Composable
fun PageHeader(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, sharedKey: String? = null, icon: ImageVector? = null,
    cardKey: String? = sharedKey?.let { "card-$it" }, actions: @Composable RowScope.() -> Unit = {},
) {
    val s = LocalStrings.current
    Box(
        Modifier
            .then(if (cardKey != null) Modifier.sharedCard(cardKey, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(HeroBrush)
            .starBackdrop(Color.White.copy(alpha = .07f), 72.dp, Alignment.TopEnd, (-28).dp, 44.dp, 90_000)
    ) {
        Row(
            Modifier.statusBarsPadding().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                }
            } else Spacer(Modifier.width(12.dp))
            if (icon != null) {
                Box(
                    Modifier
                        .then(if (sharedKey != null) Modifier.sharedIcon("icon-$sharedKey") else Modifier)
                        .size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = Gold) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                // العنوان يتبدل بانزلاق لطيف (مثل اسم السورة عند تقليب صفحات المصحف)
                androidx.compose.animation.AnimatedContent(
                    title,
                    transitionSpec = {
                        (fadeIn(tween(Motion.MEDIUM)) + androidx.compose.animation.slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 }) togetherWith
                            fadeOut(tween(Motion.SHORT)) using androidx.compose.animation.SizeTransform(clip = false)
                    },
                    label = "title"
                ) { t -> Text(t, style = MaterialTheme.typography.headlineSmall, color = Color.White) }
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
            }
            actions()
        }
    }
}

/**
 * محتوى الصفحة: كل عنصر يظهر متتابعاً عند فتح الصفحة.
 * على الهاتف عمود واحد بترتيب الاستدعاء. على الشاشات الواسعة يتوسط المحتوى بعرض مريح للقراءة،
 * وما أُضيف بـ [side] يصير عموداً ثانياً بجانب عناصر [item] (النتائج بجانب المدخلات) يبقى ظاهراً أثناء التمرير،
 * و[cells] شبكة بعمودين.
 */
class PageScope internal constructor() {
    internal sealed class Entry
    internal class One(val place: Int, val key: Any?, val content: @Composable () -> Unit) : Entry()
    internal class Cells(val count: Int, val key: (Int) -> Any, val content: @Composable (Int) -> Unit) : Entry()

    internal val entries = ArrayList<Entry>()

    /** عنصر في عمود الصفحة (العمود الأول حين تتسع الشاشة لعمودين) */
    fun item(key: Any? = null, content: @Composable () -> Unit) { entries += One(MAIN, key, content) }

    /** عنصر في العمود الثاني حين تتسع الشاشة لعمودين؛ على الهاتف في مكانه من الترتيب */
    fun side(key: Any? = null, content: @Composable () -> Unit) { entries += One(SIDE, key, content) }

    /** عنصر بعرض المحتوى كله، ينهي ما قبله من العمودين */
    fun full(key: Any? = null, content: @Composable () -> Unit) { entries += One(FULL, key, content) }

    /** عناصر متجانسة (السور مثلاً): واحد في كل صف على الهاتف، واثنان على الشاشة الواسعة؛ كل صف عنصر مستقل في القائمة */
    fun cells(count: Int, key: (Int) -> Any, content: @Composable (Int) -> Unit) { entries += Cells(count, key, content) }

    internal val twoColumnForm get() = entries.any { it is Cells || (it is One && it.place == SIDE) }

    internal companion object { const val MAIN = 0; const val SIDE = 1; const val FULL = 2 }
}

/** ارتفاع الشريط العلوي المضغوط (دون شريط الحالة) */
internal val TopBarHeight = 52.dp

/** عدد أعمدة صفحة الأداة الحالية: 2 حين تتسع لعمودين (840 فأكثر)، وإلا 1 */
val LocalPageColumns = compositionLocalOf { 1 }

/** المسافة بين عمودَي الصفحة العريضة */
private val ColumnGap = 20.dp

/**
 * صفحة أداة بأسلوب iOS: عنوان كبير في رأس متدرج يمرّ مع المحتوى، وفوقه شريط زجاجي ثابت فيه زر الرجوع.
 * حين يمرّ الرأس تحت الشريط يظهر زجاج الشريط (تمويه ما تحته) وعنوان صغير في وسطه.
 * الرأس يبقى مربوطاً ببطاقة الأداة في الرئيسية (sharedKey) فيتحول أحدهما إلى الآخر.
 * على iPad وما اتسع: الرأس بعرض الشاشة، والمحتوى في الوسط بعرض أقصاه 720 (أو عمودان بعرض أقصاه 1100
 * حين تستعمل الصفحة [PageScope.side] أو [PageScope.cells] أو [columns]، والعرض 840 فأكثر)، ونصه بمحاذاة عنوان الرأس.
 */
@Composable
fun ToolPage(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null,
    sharedKey: String? = null, icon: ImageVector? = null, spacing: Dp = 14.dp,
    columns: Boolean = false,
    content: PageScope.() -> Unit
) {
    val reveal = rememberRevealState()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val haze = rememberHazeState()
    val lead = LocalLeadingInset.current
    val density = LocalDensity.current
    // أعلى ما يثبت عنده عمود النتائج: تحت الشريط الزجاجي العلوي
    val stickTop = with(density) { (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight + 12.dp).toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val twoColumns = maxWidth - lead >= 840.dp
        val narrow = gutters(maxWidth, TextColumnMax, lead)
        val split = gutters(maxWidth, SplitMax, lead)
        androidx.compose.runtime.key(com.osatks.mizan.ui.theme.LocalThemeKey.current) {
            val list = rememberLazyListState()
            CompositionLocalProvider(LocalPageColumns provides if (twoColumns) 2 else 1) {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().hazeSource(haze).imePadding(),
                contentPadding = PaddingValues(bottom = 16.dp + nav + LocalBottomInset.current),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                val page = PageScope().apply(content)
                val wide = twoColumns && (columns || page.twoColumnForm)
                val g = if (wide) split else narrow
                item(key = "page-header", contentType = "page-header") {
                    Column {
                        LargeHeader(title, subtitle, sharedKey, icon, hasBack = onBack != null, g)
                        Spacer(Modifier.height((16.dp - spacing).coerceAtLeast(0.dp)))
                    }
                }
                emitPage(page, wide, g, reveal, spacing, list, stickTop)
            }
            }
            GlassTopBar(list, haze, title, onBack)
        }
    }
}

/** يضع عناصر [PageScope] في القائمة: عموداً واحداً بترتيبها، أو عمودين على الشاشة الواسعة */
private fun LazyListScope.emitPage(page: PageScope, wide: Boolean, g: Gutters, reveal: RevealState, spacing: Dp, list: LazyListState, stickTop: Float) {
    var index = 0
    val pad = Modifier.padding(start = g.start, end = g.end)
    fun single(key: Any?, content: @Composable () -> Unit) {
        val i = index++
        item(key = key) { Box(pad.reveal(reveal, i)) { content() } }
    }
    if (!wide) {
        page.entries.forEach { e ->
            when (e) {
                is PageScope.One -> single(e.key, e.content)
                is PageScope.Cells -> for (c in 0 until e.count) single(e.key(c)) { e.content(c) }
            }
        }
        return
    }
    var block = ArrayList<PageScope.One>()
    var blocks = 0
    fun flush() {
        if (block.isEmpty()) return
        val b = block
        block = ArrayList()
        if (b.none { it.place == PageScope.SIDE }) { b.forEach { single(it.key, it.content) }; return }
        val first = index
        index += b.size
        val key = "split-${blocks++}"
        item(key = key) { SplitColumns(b, pad, spacing, reveal, first, list, key, stickTop) }
    }
    page.entries.forEach { e ->
        when {
            e is PageScope.One && e.place != PageScope.FULL -> block += e
            e is PageScope.One -> { flush(); single(e.key, e.content) }
            e is PageScope.Cells -> {
                flush()
                for (r in 0 until (e.count + 1) / 2) {
                    val i = index++
                    item(key = e.key(r * 2)) {
                        Row(pad.reveal(reveal, i).height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(ColumnGap / 2)) {
                            Box(Modifier.weight(1f).fillMaxHeight(), propagateMinConstraints = true) { e.content(r * 2) }
                            Box(Modifier.weight(1f).fillMaxHeight(), propagateMinConstraints = true) { if (r * 2 + 1 < e.count) e.content(r * 2 + 1) }
                        }
                    }
                }
            }
        }
    }
    flush()
}

/**
 * عمودا الصفحة العريضة في عنصر واحد من القائمة: [PageScope.item] في الأول و[PageScope.side] في الثاني.
 * الثاني (النتائج) يثبت تحت الشريط العلوي ما دام الأول (المدخلات) يمرّ تحته، فتبقى النتيجة ظاهرة أثناء الإدخال؛
 * يُحسب ذلك أثناء الرسم فقط فلا يعيد التمرير بناء الصفحة.
 */
@Composable
private fun SplitColumns(
    entries: List<PageScope.One>, pad: Modifier, spacing: Dp, reveal: RevealState, first: Int,
    list: LazyListState, itemKey: Any, stickTop: Float,
) {
    var rowH by remember { mutableStateOf(0) }
    var sideH by remember { mutableStateOf(0) }
    val (sides, mains) = entries.withIndex().partition { it.value.place == PageScope.SIDE }
    Row(pad.onSizeChanged { rowH = it.height }, horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
            mains.forEachIndexed { n, (k, e) ->
                androidx.compose.runtime.key(e.key ?: "m$k") { Box(Modifier.reveal(reveal, first + n)) { e.content() } }
            }
        }
        Column(
            Modifier.weight(1f).onSizeChanged { sideH = it.height }.graphicsLayer {
                val top = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == itemKey }?.offset ?: 0
                translationY = (stickTop - top).coerceIn(0f, (rowH - sideH).coerceAtLeast(0).toFloat())
            },
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            sides.forEachIndexed { n, (k, e) ->
                androidx.compose.runtime.key(e.key ?: "s$k") { Box(Modifier.reveal(reveal, first + mains.size + n)) { e.content() } }
            }
        }
    }
}

/** الرأس الكبير لصفحة الأداة: أيقونة وعنوان كبير وسطر وصف؛ يترك أعلاه مكان الشريط الثابت إن كان فيه زر رجوع */
@Composable
private fun LargeHeader(title: String, subtitle: String?, sharedKey: String?, icon: ImageVector?, hasBack: Boolean, g: Gutters) {
    val shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    Box(
        Modifier
            .then(if (sharedKey != null) Modifier.sharedCard("card-$sharedKey", shape) else Modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(HeroBrush)
            .starBackdrop(Color.White.copy(alpha = .07f), 72.dp, Alignment.TopEnd, (-28).dp, 44.dp, 90_000)
    ) {
        // العنوان بمحاذاة المحتوى تحته (في الوسط على الشاشات العريضة، وبعد القائمة الجانبية)
        Row(
            Modifier.statusBarsPadding().padding(start = g.start + 4.dp, end = g.end + 4.dp, top = if (hasBack) TopBarHeight else 14.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .then(if (sharedKey != null) Modifier.sharedIcon("icon-$sharedKey") else Modifier)
                        .size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = Gold) }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                // العنوان يتبدل بانزلاق لطيف (مثل اسم السورة عند تقليب صفحات المصحف)
                androidx.compose.animation.AnimatedContent(
                    title,
                    transitionSpec = {
                        (fadeIn(tween(Motion.MEDIUM)) + androidx.compose.animation.slideInVertically(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 2 }) togetherWith
                            fadeOut(tween(Motion.SHORT)) using androidx.compose.animation.SizeTransform(clip = false)
                    },
                    label = "title"
                ) { t -> Text(t, style = MaterialTheme.typography.headlineMedium, color = Color.White) }
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
            }
        }
    }
}

/**
 * الشريط العلوي العائم فوق قائمة يبدأ عنصرها الأول برأس متدرج:
 * شفاف فوق الرأس (زر الرجوع أبيض)، ثم يصير زجاجاً يموّه ما يمر تحته مع عنوان صغير في الوسط حين يمرّ الرأس تحته.
 * كل ذلك يُقرأ أثناء الرسم فقط، فالتمرير لا يعيد بناء الصفحة.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun GlassTopBar(list: LazyListState, haze: HazeState, title: String, onBack: (() -> Unit)?) {
    val density = LocalDensity.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barPx = with(density) { (top + TopBarHeight).toPx() }
    val rampPx = with(density) { 36.dp.toPx() }
    // 0 والرأس ظاهر تحت الشريط، 1 حين يمرّ طرفه السفلي تحته
    val glass = remember(list, barPx) {
        derivedStateOf {
            if (list.firstVisibleItemIndex > 0) 1f
            else {
                val h = list.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return@derivedStateOf 0f
                val left = h - barPx - list.firstVisibleItemScrollOffset
                (1f - left / rampPx).coerceIn(0f, 1f)
            }
        }
    }
    val shown by remember(glass) { derivedStateOf { glass.value > 0f } }
    // شريط الحالة: نص فاتح فوق الرأس الأخضر، ويتبع مظهر التطبيق حين يصير الشريط زجاجاً فوق المحتوى
    val overHeader by remember(glass) { derivedStateOf { glass.value < .5f } }
    StatusBarStyle(lightContent = overHeader || LocalDark.current)
    val style = glassStyle(if (LocalDark.current) .66f else .72f, 24.dp)
    val hairline = MaterialTheme.colorScheme.outlineVariant
    // العنوان الصغير في وسط المحتوى لا وسط الشاشة حين تشغل القائمة الجانبية جهة البداية
    val lead = LocalLeadingInset.current
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier.matchParentSize()
                .then(
                    if (shown) Modifier.hazeEffect(haze, style) { alpha = glass.value; inputScale = HazeInputScale.Auto }
                    else Modifier
                )
                .drawBehind { if (glass.value > 0f) drawRect(hairline, Offset(0f, size.height - 1f), Size(size.width, 1f), alpha = glass.value * .8f) }
        )
        Box(Modifier.statusBarsPadding().padding(start = lead).fillMaxWidth().height(TopBarHeight), contentAlignment = Alignment.Center) {
            Text(
                title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                // نسخة مصغّرة من عنوان الرأس: قارئ الشاشة يقرأ العنوان الكبير وحده
                modifier = Modifier.padding(horizontal = 60.dp).clearAndSetSemantics {}.graphicsLayer {
                    val a = ((glass.value - .4f) / .6f).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * 6.dp.toPx()
                }
            )
            if (onBack != null) Box(Modifier.align(Alignment.CenterStart).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)).padding(start = 10.dp)) {
                BackChip(onBack) { glass.value }
            }
        }
    }
}

/** زر الرجوع: دائرة زجاجية بيضاء فوق الرأس، ويأخذ لون التطبيق حين يصير الشريط زجاجاً (كسهم الرجوع في iOS) */
@Composable
private fun BackChip(onBack: () -> Unit, progress: () -> Float) {
    val s = LocalStrings.current
    val p = progress()
    val tint = lerp(Color.White, MaterialTheme.colorScheme.primary, p)
    val bg = lerp(Color.White.copy(alpha = .16f), MaterialTheme.colorScheme.onSurface.copy(alpha = .06f), p)
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(44.dp).pressScale(src, 0.88f).clip(CircleShape).background(bg)
            .clickable(interactionSource = src, indication = if (IsIos) null else ripple(), role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            // في iOS سهم الرجوع المعتاد هناك (‹) بأطراف مستديرة
            if (IsIos) Icons.AutoMirrored.Rounded.ArrowBackIos else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = tint,
            modifier = Modifier.size(if (IsIos) 22.dp else 24.dp)
        )
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

/** يظهر المحتوى وينطوي بحركة بدل الظهور المفاجئ */
@Composable
fun Reveal(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = fadeIn(tween(Motion.MEDIUM)) + expandVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)),
        exit = fadeOut(tween(Motion.SHORT)) + shrinkVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)),
    ) { content() }
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String? = null, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            suffix = suffix?.let { s -> @Composable { Text(s) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        MiniStepper(value, onChange, min, max)
    }
}

@Composable
fun MiniStepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    val s = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    // نقرة خفيفة مع كل خطوة، وأقوى قليلاً عند بلوغ الحد
    fun step(v: Int) {
        haptic.performHapticFeedback(if (v == min || v == max) HapticFeedbackType.ContextClick else HapticFeedbackType.SegmentTick)
        onChange(v)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Filled.Remove, s.decrease, enabled = value > min) { step(value - 1) }
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            RollingText("$value", MaterialTheme.typography.titleMedium)
        }
        StepButton(Icons.Filled.Add, s.increase, enabled = value < max) { step(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, label = "step")
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = bg, interactionSource = src,
        modifier = Modifier.size(38.dp).pressScale(src, 0.86f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

/** أزرار اختيار متجاورة، والمؤشر ينزلق بينها */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val pos by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.78f, stiffness = 420f), label = "seg")
    val pill = MaterialTheme.colorScheme.surface
    val edge = MaterialTheme.colorScheme.outlineVariant
    val haptic = LocalHapticFeedback.current
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier.padding(4.dp).height(IntrinsicSize.Min).drawBehind {
                val w = size.width / options.size
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w * (pos + 1) else w * pos
                val r = CornerRadius(12.dp.toPx())
                drawRoundRect(edge, Offset(x, 0f), Size(w, size.height), r)
                drawRoundRect(pill, Offset(x + 1f, 1f), Size(w - 2f, size.height - 2f), r)
            }
        ) {
            options.forEachIndexed { i, (key, label) ->
                val color by animateColorAsState(
                    if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "segText"
                )
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable {
                        if (key != selected) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(key)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, Modifier.padding(vertical = 10.dp, horizontal = 6.dp), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * مفتاح تشغيل وإيقاف، معه اهتزاز خفيف عند التبديل.
 * في iOS بشكل مفتاح النظام (مسار بعرض 51 ونقطة بيضاء تنزلق وتتمدد عند اللمس)، وفي أندرويد مفتاح Material.
 */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val change: (Boolean) -> Unit = { on ->
        haptic.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        onCheckedChange(on)
    }
    if (!IsIos) {
        Switch(checked = checked, onCheckedChange = change)
        return
    }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val pos by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = .74f, stiffness = 520f), label = "switch")
    val stretch by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = 900f), label = "switchPress")
    val track by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .16f), tween(Motion.SHORT), label = "switchTrack"
    )
    Box(
        Modifier.size(51.dp, 31.dp).clip(CircleShape).background(track)
            .toggleable(checked, interactionSource = src, indication = null, role = Role.Switch, onValueChange = change)
            .drawBehind {
                val inset = 2.dp.toPx()
                val h = size.height - inset * 2
                val w = h + 7.dp.toPx() * stretch
                val travel = size.width - inset * 2 - w
                val ltrX = inset + travel * pos
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - ltrX - w else ltrX
                val r = CornerRadius(h / 2)
                drawRoundRect(Color.Black.copy(alpha = .10f), Offset(x, inset + 1.5.dp.toPx()), Size(w, h), r)
                drawRoundRect(Color.White, Offset(x, inset), Size(w, h), r)
            }
    )
}

/** اختيار من قائمة: حقل يفتح قائمة زجاجية منبثقة تحته بعرضه، والمختار فيها بعلامة صح */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        BoxWithConstraints {
            PickerField(text(selected)) { open = true }
            GlassMenu(
                open, { open = false },
                options.map { o -> MenuItem(text(o), checked = o == selected) { onSelect(o) } },
                width = maxWidth.coerceIn(220.dp, 360.dp),
            )
        }
    }
}

/** حقل يشبه القائمة المنسدلة ويفتح شيئاً عند الضغط */
@Composable
fun PickerField(value: String, leading: ImageVector? = null, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        interactionSource = src,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) { Icon(leading, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)) }
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.ArrowDropDown, null)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, strong: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.Bold, textAlign = TextAlign.End)
        }
    }
}

/** بطاقة النتيجة الكبيرة: القيمة تتدحرج عند التغير، والوحدة (إن وُجدت) بجانبها */
@Composable
fun ResultCard(
    label: String, value: String, badge: String? = null, badgeOk: Boolean? = null,
    unit: String? = null, content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .06f), 72.dp, Alignment.BottomStart, 30.dp, (-24).dp, 120_000)) {
            Column(
                Modifier.padding(22.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(label, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RollingText(value, MaterialTheme.typography.headlineLarge, Color.White, textAlign = TextAlign.Center)
                    if (unit != null) Text(unit, color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.titleLarge)
                }
                if (badge != null) {
                    val target = when (badgeOk) { true -> Color(0xFF3FB595); false -> Color(0xFFEF8A76); null -> Gold }
                    val bg by animateColorAsState(target, tween(Motion.MEDIUM), label = "badge")
                    Surface(shape = CircleShape, color = bg.copy(alpha = .25f)) {
                        RollingText(
                            badge, MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), Color.White,
                            Modifier.padding(horizontal = 14.dp, vertical = 5.dp), TextAlign.Center
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun Note(text: String, warn: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (warn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (warn) Icons.Filled.WarningAmber else Icons.Filled.Info, null,
                tint = if (warn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (warn) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, tween(Motion.MEDIUM), label = "chip")
    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.MEDIUM), label = "chipText")
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, tween(Motion.MEDIUM), label = "chipBorder")
    Surface(
        onClick = onClick, shape = CircleShape, color = bg, border = BorderStroke(1.dp, border),
        interactionSource = src, modifier = Modifier.pressScale(src, 0.92f)
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}

@Composable
fun RowScope.Tile(value: String, label: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(value, MaterialTheme.typography.titleLarge, MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

fun shareText(s: Strings, text: String) = com.osatks.mizan.platform.Platform.share("$text\n${s.shareSignature}", s.share)

/** صف قابل للنقر بنص وقيمة (للإعدادات) */
@Composable
fun ClickRow(title: String, subtitle: String? = null, onClick: () -> Unit, leading: ImageVector? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(leading, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** نص تصبح فيه tanzil.net و tanzil.net/trans و SIL روابط تُفتح في المتصفح */
@Composable
fun LinkedText(text: String) {
    val color = MaterialTheme.colorScheme.primary
    val styles = androidx.compose.ui.text.TextLinkStyles(
        androidx.compose.ui.text.SpanStyle(color = color, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
    )
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        Regex("tanzil\\.net/trans|tanzil\\.net|SIL").findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            val url = when (m.value) { "SIL" -> "https://openfontlicense.org"; "tanzil.net/trans" -> "https://tanzil.net/trans/"; else -> "https://tanzil.net/" }
            withLink(androidx.compose.ui.text.LinkAnnotation.Url(url, styles)) { append(m.value) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
    Text(annotated, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** فراغ بارتفاع ثابت (للمسافات بين عناصر الشبكات) */
@Composable
fun Gap(d: Dp) = Spacer(Modifier.size(d))
