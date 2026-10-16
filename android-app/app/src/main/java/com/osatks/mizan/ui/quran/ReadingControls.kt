package com.osatks.mizan.ui.quran

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Recitation
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Note
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush

/** أدوات وضع التركيز داكنة معتمة فوق الصفحة، بحافة رقيقة تفصلها عن الورق الليلي */
internal val FocusChrome = Color(0xFF15130F)
internal val FocusEdge = BorderStroke(1.dp, Color.White.copy(alpha = .14f))
private val OnGold = Color(0xFF1E1A14)
/** ارتفاع شريط وضع التركيز: يجلس في هامش الصفحة السفلي تحت السطر الأخير */
private val BAR = 44.dp

/**
 * شريط وضع التركيز في أسفل الشاشة: التلاوة (التشغيل والإيقاف، والآية السابقة والتالية أثناءها)،
 * والحفظ (في المصحف)، وإعداد التلاوة والعرض. في وضع الحفظ يحل محل الإعدادين زرّا كشف الآية التالية وإخفاء الكل.
 */
@Composable
internal fun FocusBar(
    hifzAvailable: Boolean, hifz: Boolean, canReveal: Boolean, canHide: Boolean,
    onPlay: () -> Unit, onSkip: (Int) -> Unit, onAudio: () -> Unit, onHifz: () -> Unit, onDisplay: () -> Unit,
    onRevealNext: () -> Unit, onHideAll: () -> Unit,
) {
    val s = LocalStrings.current
    Surface(shape = CircleShape, color = FocusChrome, contentColor = Color.White, border = FocusEdge) {
        Row(Modifier.height(BAR).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            // أزرار الوسائط من اليسار إلى اليمين في كل اللغات، كما هي العادة فيها
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (Recitation.active) BarButton(Icons.Filled.SkipPrevious, s.prevAyah) { onSkip(-1) }
                    PlayButton(onPlay, BAR)
                    if (Recitation.active) BarButton(Icons.Filled.SkipNext, s.nextAyah) { onSkip(1) }
                }
            }
            BarDivider()
            if (hifzAvailable) ToggleIcon(hifz, Icons.Filled.Psychology, s.hifzMode, onHifz)
            if (hifz) {
                BarDivider()
                BarButton(Icons.Filled.Visibility, s.revealNext, enabled = canReveal, tint = Gold, onClick = onRevealNext)
                BarButton(Icons.Filled.VisibilityOff, s.hideAll, enabled = canHide, onClick = onHideAll)
            } else {
                BarButton(Icons.Filled.Headphones, s.recitation, onClick = onAudio)
                BarButton(Icons.Filled.FormatSize, s.displayTitle, onClick = onDisplay)
            }
        }
    }
}

/** تلميح قصير فوق الشريط (عند بدء الحفظ) */
@Composable
internal fun HintPill(text: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = FocusChrome, contentColor = Color.White, border = FocusEdge, modifier = Modifier.widthIn(max = 380.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Psychology, null, Modifier.size(20.dp), tint = Gold)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .9f))
        }
    }
}

/** مشغّل صغير خارج وضع التركيز: الآية المتلوة والقارئ، والتشغيل والإيقاف، والإنهاء */
@Composable
internal fun MiniPlayer(onPlay: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val (surah, ayah) = QuranMeta.locate(Recitation.current.coerceAtLeast(0))
    Surface(shape = RoundedCornerShape(26.dp), color = Color.Transparent, shadowElevation = 10.dp, modifier = modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        Row(Modifier.background(HeroBrush).padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayButton(onPlay, 52.dp)
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                AnimatedContent(
                    s.surahAyah(s.surahName(surah), ayah),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "playingAyah"
                ) { t ->
                    Text(t, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    Reciters.byId(QuranPrefs.reciter).name(lang), color = Color.White.copy(alpha = .75f),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onStop) { Icon(Icons.Filled.Close, s.stopRecitation, tint = Color.White) }
        }
    }
}

/** زر التشغيل الذهبي، وحوله حلقة تدور أثناء التحميل */
@Composable
private fun PlayButton(onClick: () -> Unit, size: Dp) {
    val s = LocalStrings.current
    val on = Recitation.active && Recitation.wantsPlay
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Surface(onClick = onClick, shape = CircleShape, color = Gold, contentColor = OnGold, modifier = Modifier.size(size - 8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(on, transitionSpec = { scaleIn(tween(200), 0.6f) + fadeIn(tween(200)) togetherWith fadeOut(tween(100)) }, label = "play") { p ->
                    Icon(if (p) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (p) s.pauseRecitation else s.playRecitation, Modifier.size(size * 0.5f))
                }
            }
        }
        if (Recitation.active && Recitation.buffering) CircularProgressIndicator(Modifier.size(size - 2.dp), color = Gold, strokeWidth = 2.dp)
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, enabled: Boolean = true, tint: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier.size(BAR).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, Modifier.size(24.dp), tint = if (enabled) tint else tint.copy(alpha = .35f)) }
}

@Composable
private fun ToggleIcon(on: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Gold else Color.Transparent, tween(220), label = "toggle")
    val fg by animateColorAsState(if (on) OnGold else Color.White, tween(220), label = "toggleFg")
    Box(
        Modifier.size(BAR).clip(CircleShape).toggleable(value = on, role = Role.Switch, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(BAR - 8.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, label, Modifier.size(22.dp), tint = fg)
        }
    }
}

@Composable
private fun BarDivider() {
    Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(20.dp).background(Color.White.copy(alpha = .22f)))
}

// ---------- الأوراق ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) { content() }
}

@Composable
private fun SheetTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptionLabel(t: String) {
    Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

/** إعداد التلاوة: إعدادات ذكية جاهزة، والقارئ، والمقطع (من آية إلى آية) وتكراره، والوقفة للترديد، والسرعة، ومؤقت الإيقاف */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioOptions(onChanged: () -> Unit, onStop: () -> Unit, from: Int = 0, onPlay: (() -> Unit)? = null) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val chosen = Reciters.byId(QuranPrefs.reciter).id
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetTitle(Icons.Filled.Headphones, s.recitation)

        OptionLabel(s.smartPresets)
        val preset = currentPreset()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetCard("listen", Icons.Filled.Headphones, s.presetListen, preset) { applyPreset(it); onChanged() }
            PresetCard("memorize", Icons.Filled.Psychology, s.presetMemorize, preset) { applyPreset(it); onChanged() }
            PresetCard("review", Icons.Filled.Replay, s.presetReview, preset) { applyPreset(it); onChanged() }
        }
        Text(s.smartHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OptionLabel(s.range)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("continue" to s.rangeContinue, "page" to s.rangePage, "surah" to s.rangeSurah, "custom" to s.rangeCustom).forEach { (k, label) ->
                ChipButton(label, QuranPrefs.rangeMode == k) {
                    if (QuranPrefs.rangeMode != k) {
                        // المقطع المخصص يبدأ من الآية الحالية إلى آخر سورتها، ثم يعدّله المستخدم
                        if (k == "custom") QuranPrefs.updateCustom(from, surahEnd(from))
                        QuranPrefs.updateRangeMode(k); onChanged()
                    }
                }
            }
        }
        if (QuranPrefs.rangeMode == "custom") {
            AyahPicker(s.fromAyah, QuranPrefs.customFrom) { QuranPrefs.updateCustom(it, maxOf(it, QuranPrefs.customTo)); onChanged() }
            AyahPicker(s.toAyah, QuranPrefs.customTo) { QuranPrefs.updateCustom(minOf(it, QuranPrefs.customFrom), it); onChanged() }
        }

        OptionLabel(s.repeatAyah)
        Chips(listOf(1, 3, 5, 0), QuranPrefs.repeat, { if (it == 0) "∞" else "$it×" }) { QuranPrefs.updateRepeat(it); onChanged() }
        if (QuranPrefs.rangeMode != "continue") {
            OptionLabel(s.repeatRange)
            Chips(listOf(1, 2, 3, 0), QuranPrefs.rangeRepeat, { if (it == 0) "∞" else "$it×" }) { QuranPrefs.updateRangeRepeat(it); onChanged() }
        }
        OptionLabel(s.pauseAfter)
        Chips(listOf(0, 2, 5, -1), QuranPrefs.gap, { when (it) { 0 -> s.off; -1 -> s.pauseAyahLength; else -> s.seconds(it) } }) { QuranPrefs.updateGap(it) }
        OptionLabel(s.speed)
        Chips(listOf(75, 100, 125, 150), (QuranPrefs.speed * 100).toInt(), { "${it / 100}${if (it % 100 == 0) "" else "." + (it % 100).toString().trimEnd('0')}×" }) {
            QuranPrefs.updateSpeed(it / 100f); Recitation.applySpeed()
        }
        OptionLabel(s.sleepTimer)
        Chips(listOf(0, 15, 30, 60), QuranPrefs.sleep, { if (it == 0) s.off else s.minutes(it) }) {
            QuranPrefs.updateSleep(it); if (Recitation.active) Recitation.applySleep()
        }

        OptionLabel(s.reciter)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Reciters.ALL.forEach { r ->
                ChipButton(r.name(lang), r.id == chosen) { if (r.id != chosen) { QuranPrefs.updateReciter(r.id); onChanged() } }
            }
        }

        Note(s.audioNote)
        if (onPlay != null) Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.playRecitation)
        }
        if (Recitation.active) OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.stopRecitation)
        }
    }
}

private fun surahEnd(i: Int): Int { val sn = QuranMeta.locate(i).first; return QuranMeta.STARTS[sn - 1] + QuranMeta.COUNTS[sn - 1] - 1 }

/** الإعدادات الذكية: استماع عادي، أو حفظ (تكرار ووقفة للترديد على صفحة)، أو مراجعة (السورة كاملة أسرع قليلاً) */
private fun applyPreset(p: String) {
    when (p) {
        "listen" -> { QuranPrefs.updateRepeat(1); QuranPrefs.updateRangeMode("continue"); QuranPrefs.updateGap(0); QuranPrefs.updateSpeed(1f) }
        "memorize" -> { QuranPrefs.updateRepeat(3); QuranPrefs.updateRangeMode("page"); QuranPrefs.updateGap(-1); QuranPrefs.updateSpeed(1f) }
        "review" -> { QuranPrefs.updateRepeat(1); QuranPrefs.updateRangeMode("surah"); QuranPrefs.updateGap(0); QuranPrefs.updateSpeed(1.25f) }
    }
    QuranPrefs.updateRangeRepeat(1)
    Recitation.applySpeed()
}

private fun currentPreset(): String? = with(QuranPrefs) {
    when {
        repeat == 1 && rangeMode == "continue" && gap == 0 && speed == 1f -> "listen"
        repeat == 3 && rangeMode == "page" && gap == -1 && speed == 1f && rangeRepeat == 1 -> "memorize"
        repeat == 1 && rangeMode == "surah" && gap == 0 && speed == 1.25f && rangeRepeat == 1 -> "review"
        else -> null
    }
}

@Composable
private fun RowScope.PresetCard(key: String, icon: ImageVector, label: String, selected: String?, onPick: (String) -> Unit) {
    val on = selected == key
    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, tween(220), label = "preset")
    val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(220), label = "presetFg")
    Surface(
        onClick = { onPick(key) }, shape = RoundedCornerShape(16.dp), color = bg, contentColor = fg,
        border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.weight(1f)
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(values: List<Int>, selected: Int, label: (Int) -> String, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v -> ChipButton(label(v), v == selected) { if (v != selected) onPick(v) } }
    }
}

/** اختيار آية: السورة من قائمة، ورقم الآية بزرّي − و + */
@Composable
private fun AyahPicker(label: String, index: Int, onPick: (Int) -> Unit) {
    val s = LocalStrings.current
    val (surah, ayah) = QuranMeta.locate(index)
    val count = QuranMeta.COUNTS[surah - 1]
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(56.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(s.surahName(surah), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Filled.ArrowDropDown, null)
            }
            DropdownMenu(menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                (1..QuranMeta.SURAHS).forEach { n ->
                    DropdownMenuItem(text = { Text("$n. ${s.surahName(n)}") }, onClick = { menu = false; onPick(QuranMeta.STARTS[n - 1]) })
                }
            }
        }
        IconButton(onClick = { onPick(index - 1) }, enabled = ayah > 1) { Icon(Icons.Filled.Remove, null) }
        Text("$ayah", Modifier.width(36.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onPick(index + 1) }, enabled = ayah < count) { Icon(Icons.Filled.Add, null) }
    }
}

/** إعداد العرض: لون الصفحة، وسطوع الشاشة أثناء القراءة، ومقاس الخط في عرض آية آية */
@Composable
fun DisplayOptions(showTextSize: Boolean) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetTitle(Icons.Filled.FormatSize, s.displayTitle)
        OptionLabel(s.pageColor)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToneSwatch("", s.toneAuto)
            ToneSwatch("sepia", s.toneSepia)
            ToneSwatch("night", s.toneNight)
        }
        OptionLabel(s.brightness)
        val system = QuranPrefs.brightness < 0f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BrightnessLow, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = if (system) 0.5f else QuranPrefs.brightness, onValueChange = { QuranPrefs.updateBrightness(it) },
                valueRange = 0.02f..1f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Icon(Icons.Filled.BrightnessHigh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.brightnessSystem, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = system, onCheckedChange = { QuranPrefs.updateBrightness(if (it) -1f else 0.5f) })
        }
        if (showTextSize) {
            OptionLabel(s.textSizeLabel)
            val size = QuranPrefs.textSize
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { QuranPrefs.updateSize(size - 2) }, enabled = size > 18) { Icon(Icons.Filled.TextDecrease, s.textSmaller) }
                Text("$size", Modifier.width(48.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { QuranPrefs.updateSize(size + 2) }, enabled = size < 40) { Icon(Icons.Filled.TextIncrease, s.textLarger) }
            }
        }
    }
}

/** عيّنة من لون الصفحة: ورقها وحبرها وحافتها الذهبية كما تظهر في القراءة */
@Composable
private fun RowScope.ToneSwatch(tone: String, label: String) {
    val selected = QuranPrefs.tone == tone
    val pal = readingPalette(tone) ?: mushafPalette()
    val edge by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else pal.gold.copy(alpha = .6f), tween(220), label = "swatch")
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            onClick = { QuranPrefs.updateTone(tone) }, shape = RoundedCornerShape(16.dp), color = pal.paper,
            border = BorderStroke(if (selected) 2.dp else 1.dp, edge), modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("بِسْمِ ٱللَّهِ", style = quranStyle(17, TextAlign.Center), color = pal.ink, maxLines = 1)
            }
        }
        Text(
            label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
