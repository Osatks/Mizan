package com.osatks.mizan.ui.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.osatks.mizan.ui.GlassTone
import com.osatks.mizan.ui.IsIos
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.floatingGlass
import com.osatks.mizan.ui.glassMaterial
import com.osatks.mizan.ui.glassTone
import com.osatks.mizan.ui.squish
import androidx.compose.material3.ripple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.theme.Gold

/**
 * شريط خيارات الآية: يظهر أسفل الشاشة بالضغط المطوّل على آية، وفيه كل ما يُفعل بها:
 * التفسير، والاستماع منها، والحفظ، والنسخ، والمشاركة.
 */
@Composable
internal actual fun AyahActionBar(index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onShare: () -> Unit, onClose: () -> Unit) {
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val (surah, ayah) = QuranMeta.locate(index)
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    // كبسولة زجاجية عائمة فوق الصفحة (تموّه ما تحتها وحدها)؛ زوايا الأزرار متّحدة المركز معها (30 − 6 = 24)
    val shape = RoundedCornerShape(BarRadius)
    Column(
        Modifier.fillMaxWidth()
            .floatingGlass(LocalAppHaze.current, shape, glassMaterial(if (glassTone() == GlassTone.Dark) .60f else .66f))
            // تمنع لمسات الشريط من الوصول إلى الصفحة تحته
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(BarPad)
    ) {
        Column {
            Row(Modifier.padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AyahMarker(ayah, Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(ref, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, s.close, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Action(Icons.AutoMirrored.Filled.MenuBook, s.tafsirTitle, primary = true, onClick = onTafsir)
                Action(Icons.Filled.PlayArrow, s.listen, primary = true, onClick = onListen)
                val marked = QuranPrefs.isBookmarked(index)
                Action(
                    if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    if (marked) s.saved else s.save, tint = if (marked) Gold else null,
                ) { QuranPrefs.toggleBookmark(index) }
                Action(Icons.Filled.ContentCopy, s.copy) {
                    clipboard.setText(AnnotatedString("$ayahText\n[$ref]"))
                    Platform.toast(s.copied)
                    onClose()
                }
                // صورة بخط المصحف أو نص (AyahShare.kt)
                Action(Icons.Filled.Share, s.share) { onShare() }
            }
        }
    }
}

/** زوايا شريط خيارات الآية وهامشه الداخلي */
private val BarRadius = 30.dp

private val BarPad = 6.dp

@Composable
private fun androidx.compose.foundation.layout.RowScope.Action(
    icon: ImageVector, label: String, primary: Boolean = false, tint: Color? = null, onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    Column(
        Modifier.weight(1f).squish(src).clip(RoundedCornerShape(BarRadius - BarPad))
            .clickable(interactionSource = src, indication = if (IsIos) null else ripple(), onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(icon, transitionSpec = { scaleIn(spring(dampingRatio = 0.5f)) togetherWith fadeOut(tween(90)) }, label = "act") { ic ->
                Icon(ic, null, tint = tint ?: if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // الأسماء الطويلة («Поделиться»، «Enregistrer») تصغر قليلاً لتتسع في عرض الشاشة الخارجية لـ iPhone Duo
        val color = LocalContentColor.current
        com.osatks.mizan.ui.FitLabel(label, { color }, MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center), 9.sp, Modifier.padding(horizontal = 2.dp))
    }
}
