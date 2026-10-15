package com.osatks.mizan.ui.quran

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.ChipButton
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.Gold

/**
 * تفسير الآية المختارة: نص الآية، واختيار المصدر (التفسير الميسر، الجلالين، ترجمات المعاني)،
 * والانتقال إلى الآية السابقة والتالية بحركة انزلاق، مع الحفظ والنسخ والمشاركة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirSheet(surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        TafsirContent(surah, ayah, text, onAyah)
    }
}

/** محتوى ورقة التفسير (منفصل ليُعرض أيضاً في لقطات الاختبار) */
@Composable
fun TafsirContent(surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val src = QuranPrefs.sourceFor(lang)
    val content by produceState(QuranData.cached("tafsir/${src.id}.txt"), src.id) { value = QuranData.source(src.id) }
    val count = QuranMeta.COUNTS[surah - 1]
    val index = QuranMeta.index(surah, ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    val ref = s.surahAyah(s.surahName(surah), ayah)

    run {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // الرأس: رقم الآية وموضعها، والحفظ والنسخ والمشاركة
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(ayah, transitionSpec = { scaleIn(spring(dampingRatio = 0.55f)) + fadeIn() togetherWith fadeOut() }, label = "badge") { a ->
                    AyahMarker(a, Modifier.size(40.dp), 14.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ref, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${s.juzName(QuranMeta.juzOf(index))} · ${s.pageName(QuranMeta.pageOf(index))}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val marked = QuranPrefs.isBookmarked(index)
                IconButton(onClick = { QuranPrefs.toggleBookmark(index) }) {
                    AnimatedContent(marked, transitionSpec = { scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f)) togetherWith fadeOut(tween(90)) }, label = "mark") { on ->
                        Icon(
                            if (on) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            if (on) s.removeBookmark else s.bookmarkAyah,
                            tint = if (on) Gold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString("$ayahText\n[$ref]"))
                    Toast.makeText(ctx, s.copied, Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Filled.ContentCopy, s.copy, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = {
                    val tafsir = content?.getOrNull(index).orEmpty()
                    shareText(ctx, s, "﴿$ayahText﴾\n[$ref]\n\n${Sources.label(src, s)}:\n$tafsir")
                }) { Icon(Icons.Filled.Share, s.share, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            // نص الآية: ينزلق من جهة الآية الجديدة
            AnimatedContent(
                ayah,
                transitionSpec = {
                    val sign = (if (targetState > initialState) 1 else -1) * (if (rtl) -1 else 1)
                    (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> sign * w / 3 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                        (slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Decelerate)) { w -> -sign * w / 3 } + fadeOut(tween(Motion.SHORT))) using
                        SizeTransform(clip = false)
                },
                label = "ayahText"
            ) { a ->
                Text(
                    QuranData.withoutBismillah(text, QuranMeta.index(surah, a)), Modifier.fillMaxWidth(),
                    style = quranStyle(24), color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // المصادر: لغة الواجهة أولاً
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                items(Sources.ordered(lang), key = { it.id }) { o ->
                    ChipButton(Sources.label(o, s), o.id == src.id) { QuranPrefs.updateSource(o.id) }
                }
            }
            Text(
                "${Sources.kindLabel(src, s)} · ${src.author}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold
            )

            // التفسير: يتبدل بتلاشٍ عند تغيير الآية أو المصدر
            val body = content
            AnimatedContent(
                Triple(src.id, ayah, body != null),
                transitionSpec = { fadeIn(tween(Motion.MEDIUM, 60)) togetherWith fadeOut(tween(Motion.SHORT)) using SizeTransform(clip = false) },
                label = "tafsir"
            ) { (_, a, ready) ->
                if (!ready || body == null) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
                } else {
                    SelectionContainer {
                        Text(
                            body[QuranMeta.index(surah, a)], Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDirection = if (src.lang.rtl) TextDirection.Rtl else TextDirection.Ltr,
                                lineHeight = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // الآية السابقة / التالية داخل السورة
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onAyah(ayah - 1) }, enabled = ayah > 1, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                    Text(s.prevAyah, maxLines = 1)
                }
                OutlinedButton(onClick = { onAyah(ayah + 1) }, enabled = ayah < count, modifier = Modifier.weight(1f)) {
                    Text(s.nextAyah, maxLines = 1)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }
    }
}
