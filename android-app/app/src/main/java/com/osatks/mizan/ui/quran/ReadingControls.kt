package com.osatks.mizan.ui.quran

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

/** إعداد التلاوة: القارئ، وتكرار كل آية (للحفظ)، ومصدر التلاوات */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioOptions(onChanged: () -> Unit, onStop: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val chosen = Reciters.byId(QuranPrefs.reciter).id
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SheetTitle(Icons.Filled.Headphones, s.recitation)
        OptionLabel(s.reciter)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Reciters.ALL.forEach { r ->
                ChipButton(r.name(lang), r.id == chosen) { if (r.id != chosen) { QuranPrefs.updateReciter(r.id); onChanged() } }
            }
        }
        OptionLabel(s.repeatAyah)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 5, 0).forEach { n ->
                ChipButton(if (n == 0) "∞" else "$n×", QuranPrefs.repeat == n) { if (QuranPrefs.repeat != n) { QuranPrefs.updateRepeat(n); onChanged() } }
            }
        }
        Note(s.audioNote)
        if (Recitation.active) OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.stopRecitation)
        }
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
