package com.osatks.mizan.ui.quran

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import com.osatks.mizan.ui.starBackdrop

/** بطاقة متابعة القراءة؛ تتحول إلى رأس صفحة السورة عند فتحها */
@Composable
fun ContinueCard(index: Int, open: (String) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val (surah, ayah) = QuranMeta.locate(index)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(surah, ayah, "continue")) }, shape = RoundedCornerShape(22.dp), color = Color.Transparent,
        interactionSource = src,
        modifier = modifier.fillMaxWidth().sharedCard("continue-$surah", RoundedCornerShape(22.dp)).pressScale(src, 0.97f)
    ) {
        Column(
            Modifier.background(HeroBrush).starBackdrop(Color.White.copy(alpha = .07f), 60.dp, Alignment.CenterEnd, (-30).dp, 0.dp, 90_000)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.continueReading, color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(s.ayahRef(index), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("${s.juzName(QuranMeta.juzOf(index))} · ${s.pageName(QuranMeta.pageOf(index))}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
                }
                Text(QuranMeta.NAMES_AR[surah - 1], color = Color.White, style = quranStyle(26, TextAlign.Center))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = .8f))
            }
            val focusAt = QuranPrefs.focusLast
            if (focusAt >= 0) {
                Spacer(Modifier.height(12.dp))
                FocusPill(focusAt, open)
            }
        }
    }
}

/** «تابع في وضع التركيز»: يفتح الصفحة التي توقف عندها القارئ في وضع التركيز، ووضعَ التركيز معها */
@Composable
private fun FocusPill(index: Int, open: (String) -> Unit) {
    val s = LocalStrings.current
    val (surah, ayah) = QuranMeta.locate(index)
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = { open(surahRoute(surah, ayah, "focus")) }, shape = CircleShape, color = Color.White.copy(alpha = .14f),
        border = BorderStroke(1.dp, Gold.copy(alpha = .55f)), interactionSource = src, modifier = Modifier.pressScale(src, 0.95f)
    ) {
        Row(Modifier.padding(start = 12.dp, end = 14.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Fullscreen, null, Modifier.size(18.dp), tint = Gold)
            Spacer(Modifier.width(8.dp))
            Text(
                s.continueFocus, Modifier.weight(1f, fill = false), color = Color.White, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(" · ${s.pageName(QuranMeta.pageOf(index))}", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
