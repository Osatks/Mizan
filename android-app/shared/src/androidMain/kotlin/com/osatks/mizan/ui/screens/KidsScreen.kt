package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Kids
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.KidsRecite
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.Picker
import com.osatks.mizan.ui.Segmented
import com.osatks.mizan.ui.ToolPage
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.quran.AyahMarker
import com.osatks.mizan.ui.quran.quranStyle
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * ركن الأطفال (أندرويد): السور القصيرة، وأسماء الله الحسنى بطاقاتٍ تُسحب، وأدعية اليوم، ولوحة النجوم؛
 * وصفحة السورة آيةً آية بخط كبير مع «ردّد معي» ([KidsRecite]). بطراز ميزان (الخطوط، والذهبي، وشكل البطاقات)
 * بزوايا أكثر استدارة وأزرار أكبر. في iOS الواجهة نفسها بـ SwiftUI (NativeKids.swift).
 */
@Composable
fun KidsScreen(onBack: (() -> Unit)? = null) {
    remember { Kids.ensure() }
    var open by rememberSaveable { mutableIntStateOf(0) }
    // الرجوع من السورة إلى ركن الأطفال لا إلى الأدوات (وتتوقف تلاوتها)
    fun close() {
        KidsRecite.stop()
        open = 0
    }
    BackHandler(enabled = open != 0) { close() }
    // التلاوة تتوقف مؤقتاً بمغادرة التطبيق أو الصفحة، وتنتهي بالخروج منها
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) KidsRecite.pause() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            KidsRecite.release()
        }
    }
    val active = LocalPageActive.current
    LaunchedEffect(active) { if (!active) KidsRecite.pause() }
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) {
        if (value == null) value = runCatching { QuranData.uthmani() }.getOrNull()
    }
    val holder = rememberSaveableStateHolder()
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            open,
            transitionSpec = {
                if (Motion.reduced) fadeIn(tween(Motion.SHORT)) togetherWith fadeOut(tween(Motion.SHORT))
                else (fadeIn(tween(Motion.MEDIUM)) + slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { it / 6 }) togetherWith
                    fadeOut(tween(Motion.SHORT))
            },
            label = "kids",
        ) { n ->
            holder.SaveableStateProvider(if (n == 0) "home" else "surah-$n") {
                if (n == 0) KidsHome(onBack, quran) { open = it }
                else KidsReader(n, quran) { close() }
            }
        }
        KidsCelebration(Kids.celebrations)
    }
}

// ---------- الصفحة الأولى ----------

@Composable
private fun KidsHome(onBack: (() -> Unit)?, quran: Array<String>?, onOpen: (Int) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val tool = s.tools.getValue("kids")
    val duas by produceState<List<KidsDua>?>(null, lang) {
        value = withContext(Dispatchers.IO) { runCatching { kidsDuas(lang) }.getOrNull() }
    }
    val meaningId = remember(lang) { kidsMeaningSource(lang) }
    val meanings by produceState(meaningId?.let { QuranData.cached("tafsir/$it.txt") }, meaningId) {
        value = meaningId?.let { runCatching { QuranData.source(it) }.getOrNull() }
    }
    ToolPage(tool.name, tool.desc, onBack, sharedKey = "kids", icon = TOOL_ICONS["kids"], spacing = 16.dp) {
        item(key = "banner") { KidsStarsBanner() }
        item(key = "surahs") {
            KidsCard(s.kidsSurahs, s.kidsSurahsSub, Icons.AutoMirrored.Filled.MenuBook) { KidsSurahGrid(onOpen) }
        }
        item(key = "names") {
            KidsCard(s.kidsNames, s.kidsNamesSub, Icons.Filled.AutoAwesome) { KidsNamesPager(lang, quran, meanings) }
        }
        item(key = "duas") {
            KidsCard(s.kidsDuas, s.kidsDuasSub, Icons.Filled.VolunteerActivism) {
                duas?.forEach { d -> KidsDuaCard(d) }
            }
        }
        item(key = "chart") {
            KidsCard(s.kidsStars, s.kidsStarsSub, Icons.Filled.Star) { KidsStarChart() }
        }
    }
}

/** بطاقة قسم: زوايا 28 وحدّ رفيع، وفقاعة أيقونة ذهبية، وعنوان عريض بلون التطبيق ووصف قصير */
@Composable
private fun KidsCard(title: String, sub: String?, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp), color = cs.surface, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Gold.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = cs.primary, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
                    if (!sub.isNullOrEmpty()) Text(sub, fontSize = 13.sp, lineHeight = 18.sp, color = cs.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

/** شريط النجوم أعلى الصفحة: عددها بنجمة ذهبية كبيرة، وكم سورة حُفظت من سور الركن */
@Composable
private fun KidsStarsBanner() {
    val s = LocalStrings.current
    val total = Kids.total
    val all = Kids.SURAHS.size
    val progress by animateFloatAsState(total.toFloat() / all, spring(dampingRatio = 0.8f, stiffness = 120f), label = "stars")
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(HeroBrush)
            .semantics(mergeDescendants = true) { contentDescription = "${s.kidsStars}: ${s.kidsStarCount(total)}, ${Fmt.int(total.toLong())} / ${Fmt.int(all.toLong())}" }
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.size(64.dp)) {
                drawPath(starPath(center, size.minDimension / 2), Gold)
                drawPath(starPath(center, size.minDimension / 4.2f), Color.White.copy(alpha = .35f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.kidsStarCount(total), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                KidsBar(progress, Color.White.copy(alpha = .25f), Gold)
                Text("${Fmt.int(total.toLong())} / ${Fmt.int(all.toLong())}", color = Color.White.copy(alpha = .85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** شريط تقدّم مستدير */
@Composable
private fun KidsBar(progress: Float, track: Color, fill: Color) {
    Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(fill))
    }
}

/** شبكة خانات متساوية: عمودان على الهاتف وثلاثة على الأعرض */
@Composable
private fun KidsGrid(count: Int, minCell: Int, cell: @Composable (Int, Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = ((maxWidth + 10.dp) / (minCell.dp + 10.dp)).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (0 until count).chunked(cols).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i -> cell(i, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** السور: اسمها بخط المصحف، واسمها بلغة الواجهة، وعدد آياتها، ونجمتها إن حُفظت */
@Composable
private fun KidsSurahGrid(onOpen: (Int) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val cs = MaterialTheme.colorScheme
    KidsGrid(Kids.SURAHS.size, 140) { i, modifier ->
        val n = Kids.SURAHS[i]
        val star = Kids.isMemorized(n)
        val src = remember { MutableInteractionSource() }
        Surface(
            onClick = { onOpen(n) }, shape = RoundedCornerShape(24.dp), color = cs.primaryContainer.copy(alpha = .55f),
            border = BorderStroke(1.dp, if (star) Gold else cs.outlineVariant.copy(alpha = .6f)),
            interactionSource = src,
            modifier = modifier.heightIn(min = 116.dp).pressScale(src, 0.94f).semantics {
                contentDescription = s.surahName(n) + if (star) ", ${s.kidsMemorized}" else ""
            },
        ) {
            Box {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(26, TextAlign.Center), color = cs.primary, maxLines = 1)
                    if (lang != Lang.AR) Text(s.surahName(n), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.ayahCount(QuranMeta.COUNTS[n - 1]), fontSize = 12.sp, color = cs.onSurfaceVariant, maxLines = 1)
                }
                if (star) Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp))
            }
        }
    }
}

/** أسماء الله الحسنى: بطاقة لكل اسم تُسحب، بالاسم كبيراً، وآية ورد فيها، ومعناها؛ وزرّا السابق والتالي */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KidsNamesPager(lang: Lang, quran: Array<String>?, meanings: Array<String>?) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val names = Kids.NAMES
    val pager = rememberPagerState { names.size }
    val scope = rememberCoroutineScope()
    val meaningLabel = kidsMeaningLabel(lang, s)
    HorizontalPager(pager, Modifier.fillMaxWidth().height(440.dp), pageSpacing = 12.dp, key = { it }) { i ->
        val n = names[i]
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(Gold.copy(alpha = .20f), cs.primaryContainer.copy(alpha = .45f))))
                .border(1.dp, Gold.copy(alpha = .55f), RoundedCornerShape(26.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(n.ar, style = quranStyle(46, TextAlign.Center), color = cs.primary, maxLines = 1,
                modifier = Modifier.semantics { contentDescription = if (lang == Lang.AR) n.ar else "${n.ar}, ${n.latin}" })
            if (lang != Lang.AR) Text(n.latin, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldDeep)
            Text(kidsNameAyah(n, quran), style = quranStyle(20, TextAlign.Center), color = cs.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(kidsNameRef(n, s), fontSize = 12.sp, color = cs.onSurfaceVariant)
            val meaning = meanings?.getOrNull(QuranMeta.index(n.surah, n.ayah))?.takeIf { it.isNotBlank() }
            if (meaning != null) {
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    Text(meaningLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                    Text(meaning, fontSize = 15.sp, lineHeight = 22.sp, color = cs.onSurface)
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        KidsRoundButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, s.kidsPrevName, enabled = pager.currentPage > 0) {
            scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
        }
        Text(
            "${Fmt.int((pager.currentPage + 1).toLong())} / ${Fmt.int(names.size.toLong())}",
            Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant,
        )
        KidsRoundButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.kidsNextName, enabled = pager.currentPage < names.size - 1) {
            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
        }
    }
}

/** زر دائري كبير (56) بلون فاتح من لون التطبيق */
@Composable
private fun KidsRoundButton(icon: ImageVector, desc: String, enabled: Boolean = true, size: Int = 56, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        color = if (enabled) cs.primaryContainer else cs.surfaceVariant, interactionSource = src,
        modifier = Modifier.size(size.dp).pressScale(src, 0.88f).semantics { contentDescription = desc },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (enabled) cs.primary else cs.outline, modifier = Modifier.size((size * 0.5f).dp))
        }
    }
}

/** دعاء: مناسبته، ونصه بخط المصحف، وترجمته */
@Composable
private fun KidsDuaCard(d: KidsDua) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cs.primaryContainer.copy(alpha = .45f))
            .semantics(mergeDescendants = true) {}.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Gold))
            Text(d.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary)
        }
        Text(d.ar, Modifier.fillMaxWidth(), style = quranStyle(24, TextAlign.Center), color = cs.onSurface)
        if (d.tr != null) Text(d.tr, Modifier.fillMaxWidth(), fontSize = 14.sp, lineHeight = 20.sp, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** لوحة النجوم: لكل سورة خانة يلمسها أحد الوالدين فتأخذ نجمتها (ولمسة أخرى ترفعها) */
@Composable
private fun KidsStarChart() {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val total = Kids.total
    val all = Kids.SURAHS.size
    val progress by animateFloatAsState(total.toFloat() / all, spring(dampingRatio = 0.8f, stiffness = 120f), label = "chart")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.size(34.dp))
        Text(s.kidsStarCount(total), Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Black, color = cs.primary)
        Text("${Fmt.int(total.toLong())} / ${Fmt.int(all.toLong())}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
    }
    KidsBar(progress, cs.outlineVariant.copy(alpha = .5f), Gold)
    KidsGrid(all, 96) { i, modifier ->
        val n = Kids.SURAHS[i]
        val on = Kids.isMemorized(n)
        val bg by animateColorAsState(if (on) Gold.copy(alpha = .22f) else cs.surface, tween(Motion.SHORT), label = "starBg")
        val scale by animateFloatAsState(if (on) 1f else 0.82f, spring(dampingRatio = 0.4f, stiffness = 400f), label = "starScale")
        val src = remember { MutableInteractionSource() }
        Column(
            modifier.heightIn(min = 92.dp).pressScale(src, 0.92f).clip(RoundedCornerShape(20.dp)).background(bg)
                .border(1.dp, if (on) Gold else cs.outlineVariant, RoundedCornerShape(20.dp))
                .toggleable(on, interactionSource = src, indication = androidx.compose.material3.ripple(), role = Role.Checkbox) {
                    if (Kids.toggleStar(n)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .semantics { contentDescription = s.surahName(n); stateDescription = if (on) s.kidsMemorized else s.kidsMarkMemorized }
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                if (on) Icons.Filled.Star else Icons.Filled.StarBorder, null,
                tint = if (on) Gold else cs.outline, modifier = Modifier.size(34.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
            Text(QuranMeta.NAMES_AR[n - 1], style = quranStyle(15, TextAlign.Center), color = cs.onSurface, maxLines = 1)
        }
    }
}

// ---------- صفحة السورة ----------

@Composable
private fun KidsReader(surah: Int, quran: Array<String>?, onClose: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(surah) { KidsRecite.open(surah) }
    val count = QuranMeta.COUNTS[surah - 1]
    val basmala = QuranData.hasBismillahHeader(surah)
    fun stepOf(ayah: Int) = ayah - 1 + if (basmala) 1 else 0
    val mine = KidsRecite.surah == surah
    val current = if (mine && KidsRecite.active) KidsRecite.step else -1
    val star = Kids.isMemorized(surah)
    // رسالة الخطأ (لا اتصال) حتى تبدأ التلاوة مرة أخرى
    var seenErrors by remember { mutableIntStateOf(KidsRecite.errors) }
    val failed = KidsRecite.errors > seenErrors && !KidsRecite.active
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current

    Box(Modifier.fillMaxSize()) {
        ToolPage(
            s.surahName(surah), "${QuranMeta.NAMES_AR[surah - 1]} · ${s.ayahCount(count)}", onClose,
            icon = Icons.AutoMirrored.Filled.MenuBook, spacing = 14.dp,
        ) {
            item(key = "settings") {
                KidsCard(s.kidsRepeatAfterMe, s.kidsTapAyah, Icons.Filled.RecordVoiceOver) {
                    Text(s.repeatAyah, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
                    Segmented(Kids.REPEATS.map { "$it" to s.dhikrTimes(it) }, "${Kids.repeat}") { v ->
                        v.toIntOrNull()?.let { Kids.updateRepeat(it) }
                    }
                    Picker(s.reciter, Reciters.ALL.map { it.id }, Kids.reciter, { id -> Reciters.byId(id).name(lang) }, { Kids.updateReciter(it) })
                }
            }
            item(key = "star") {
                KidsMemorizeButton(star) { if (Kids.toggleStar(surah)) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            }
            if (basmala) item(key = "basmala") {
                KidsAyahCard(quran?.getOrNull(0) ?: "", null, highlighted = current == 0, listening = KidsRecite.phase == KidsRecite.Phase.LISTEN) {
                    KidsRecite.open(surah); KidsRecite.start(0)
                }
            }
            for (a in 1..count) item(key = "a$a") {
                val text = quran?.let { QuranData.withoutBismillah(it, QuranMeta.index(surah, a)) } ?: ""
                KidsAyahCard(text, a, highlighted = current == stepOf(a), listening = KidsRecite.phase == KidsRecite.Phase.LISTEN) {
                    KidsRecite.open(surah); KidsRecite.start(stepOf(a))
                }
            }
            // مكان شريط التشغيل العائم
            item(key = "space") { Spacer(Modifier.height(112.dp)) }
        }
        KidsControls(
            mine = mine, failed = failed,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = nav + 12.dp),
            onToggle = {
                seenErrors = KidsRecite.errors
                KidsRecite.open(surah)
                KidsRecite.toggle()
            },
            onStop = { KidsRecite.stop() },
        )
    }
}

/** «حفظها؟ أعطِ نجمة»: زر كبير للوالدين، ذهبي ممتلئ حين تكون السورة محفوظة */
@Composable
private fun KidsMemorizeButton(star: Boolean, onToggle: () -> Unit) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (star) Gold else cs.surface, tween(Motion.SHORT), label = "memBg")
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).pressScale(src, 0.96f).clip(CircleShape).background(bg)
            .border(2.dp, Gold, CircleShape)
            .toggleable(star, interactionSource = src, indication = androidx.compose.material3.ripple(), role = Role.Switch) { onToggle() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon(if (star) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = if (star) Color.White else GoldDeep, modifier = Modifier.size(30.dp))
        Text(if (star) s.kidsMemorized else s.kidsMarkMemorized, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (star) Color(0xFF3A2A00) else GoldDeep)
    }
}

/** آية بخط كبير، ورقمها في نجمة المصحف الذهبية؛ تتظلل وتكبر قليلاً وهي تُتلى، وتُلمس فتبدأ منها */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KidsAyahCard(text: String, ayah: Int?, highlighted: Boolean, listening: Boolean, onTap: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(highlighted) { if (highlighted) runCatching { requester.bringIntoView() } }
    val bg by animateColorAsState(
        if (highlighted) (if (listening) Gold.copy(alpha = .22f) else cs.primaryContainer) else cs.surface, tween(Motion.MEDIUM), label = "ayahBg",
    )
    val scale by animateFloatAsState(if (highlighted && !Motion.reduced) 1.02f else 1f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "ayahScale")
    Column(
        Modifier.fillMaxWidth().bringIntoViewRequester(requester).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(24.dp)).background(bg)
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) Gold else cs.outlineVariant.copy(alpha = .6f), RoundedCornerShape(24.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, Modifier.fillMaxWidth(), style = quranStyle(if (ayah == null) 26 else 30, TextAlign.Center), color = if (ayah == null) cs.primary else cs.onSurface)
        if (ayah != null) AyahMarker(ayah, Modifier.size(40.dp), 14.sp)
    }
}

/** شريط التشغيل العائم: حال «ردّد معي» (استمع، دورك، المرة)، وزر تشغيل كبير، والإيقاف */
@Composable
private fun KidsControls(mine: Boolean, failed: Boolean, modifier: Modifier, onToggle: () -> Unit, onStop: () -> Unit) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    val active = mine && KidsRecite.active
    val running = mine && KidsRecite.running
    val phase = if (mine) KidsRecite.phase else KidsRecite.Phase.IDLE
    val label = when {
        failed -> s.audioError
        phase == KidsRecite.Phase.LISTEN -> s.kidsListen
        phase == KidsRecite.Phase.REPEAT -> s.kidsYourTurn
        phase == KidsRecite.Phase.DONE -> s.kidsWellDone
        else -> s.kidsRepeatAfterMe
    }
    val basmalaStep = KidsRecite.steps.getOrNull(KidsRecite.step)?.basmala == true
    val rep = if (active && KidsRecite.times > 1 && !basmalaStep) "${Fmt.int(KidsRecite.rep.toLong())} / ${Fmt.int(KidsRecite.times.toLong())}" else null
    Surface(
        shape = RoundedCornerShape(36.dp), color = cs.surface, border = BorderStroke(1.dp, Gold.copy(alpha = .6f)),
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth().shadow(10.dp, RoundedCornerShape(36.dp)),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier.size(68.dp).pressScale(src, 0.9f).clip(CircleShape).background(HeroBrush)
                    .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onToggle)
                    .semantics { contentDescription = if (running) s.pauseRecitation else s.playRecitation },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (failed) cs.error else cs.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (rep != null) Text(rep, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldDeep)
            }
            if (active) KidsRoundButton(Icons.Filled.Stop, s.stopRecitation, size = 52, onClick = onStop)
        }
    }
}

// ---------- الاحتفال ----------

/** نجوم وقصاصات ذهبية تتناثر، وبطاقة «أحسنت!» في الوسط، كلما زاد [count] (سورة حُفظت أو خُتمت) */
@Composable
private fun KidsCelebration(count: Int) {
    val s = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    var last by remember { mutableIntStateOf(count) }
    val anim = remember { Animatable(1f) }
    LaunchedEffect(count) {
        if (count == last) return@LaunchedEffect
        last = count
        anim.snapTo(0f)
        anim.animateTo(1f, tween(if (Motion.reduced) 1400 else 2400, easing = LinearEasing))
    }
    val p = anim.value
    if (p >= 1f) return
    val colors = listOf(Gold, GoldDeep, cs.primary, Color(0xFFFF8A65), Color(0xFF64B5F6), Color(0xFFBA68C8))
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!Motion.reduced) Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val reach = size.minDimension * 0.62f
            val fade = ((1f - p) / 0.35f).coerceIn(0f, 1f)
            for (k in 0 until 30) {
                val a = k * (2 * PI / 30).toFloat() + (k % 3) * 0.17f
                val speed = 0.55f + (k * 37 % 45) / 100f
                val d = reach * speed * (1f - (1f - p) * (1f - p))
                val drop = size.minDimension * 0.25f * p * p
                val pos = Offset(c.x + d * cos(a), c.y + d * sin(a) + drop)
                val color = colors[k % colors.size].copy(alpha = fade)
                val r = 7.dp.toPx() + (k % 4) * 2.dp.toPx()
                if (k % 2 == 0) rotate(p * 360f * (if (k % 4 == 0) 1 else -1), pos) { drawPath(starPath(pos, r), color) }
                else drawCircle(color, r * 0.55f, pos)
            }
        }
        val appear = (p / 0.12f).coerceIn(0f, 1f)
        val vanish = ((1f - p) / 0.2f).coerceIn(0f, 1f)
        val pop = if (Motion.reduced) 1f else 0.6f + 0.4f * appear + 0.06f * sin(appear * PI.toFloat())
        Column(
            Modifier.graphicsLayer { scaleX = pop; scaleY = pop; alpha = min(appear, vanish) }
                .shadow(12.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(cs.surface)
                .border(2.dp, Gold, RoundedCornerShape(30.dp))
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive }
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(84.dp)) {
                drawPath(starPath(center, size.minDimension / 2), Gold)
                drawPath(starPath(center, size.minDimension / 4.2f), Color.White.copy(alpha = .4f))
            }
            Text(s.kidsWellDone, fontSize = 22.sp, fontWeight = FontWeight.Black, color = cs.primary, textAlign = TextAlign.Center)
            Text(s.kidsStarCount(Kids.total), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldDeep)
        }
    }
}

/** نجمة خماسية مركزها [c] ونصف قطرها [r] */
private fun DrawScope.starPath(c: Offset, r: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.46f
        val a = (-90f + i * 36f) * (PI.toFloat() / 180f)
        val x = c.x + rr * cos(a)
        val y = c.y + rr * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

