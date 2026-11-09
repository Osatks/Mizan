package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.osatks.mizan.ui.lightHeaderBrush
import com.osatks.mizan.ui.starBackdrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.sharedIcon
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.ui.quran.quranStyle
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** بحث شامل في الأدوات والمدن والصلوات والمناسبات والأذكار والإعدادات، بأي من لغات التطبيق */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(onBack: () -> Unit, open: (String) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val keyboard = LocalSoftwareKeyboardController.current
    var q by rememberSaveable { mutableStateOf("") }
    var showLang by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val terms = Search.terms(q)
    // نص المصحف وآيات مطابقة (تُحسب في الخلفية)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = QuranData.uthmani() }
    var ayahHits by remember { mutableStateOf<List<Int>>(emptyList()) }
    LaunchedEffect(q, lang) {
        ayahHits = emptyList()
        if (terms.isEmpty()) return@LaunchedEffect
        delay(300)
        ayahHits = QuranSearch.ayahs(q, lang, limit = 6)
    }
    val hits: List<SearchHit> = remember(q, lang, ayahHits, quran) { searchHits(s, lang, q, ayahHits, quran, open) { showLang = true } }

    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // على الشاشات العريضة: حقل البحث والنتائج في عمود بعرض مريح في الوسط
    val g = gutters(LocalWindowLayout.current.width, TextColumnMax)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // الشريط بطراز الرئيسية (كما في iPhone): زر رجوع دائري وحقل البحث في بطاقة بيضاء، على تدرج الترويسة ونجمتها
        val cs = MaterialTheme.colorScheme
        // شريط الحالة يتبع مظهر التطبيق فوق الترويسة الفاتحة
        com.osatks.mizan.ui.StatusBarStyle(lightContent = com.osatks.mizan.ui.theme.LocalDark.current)
        Box(
            Modifier.fillMaxWidth().clipToBounds().background(lightHeaderBrush())
                .starBackdrop(cs.primary.copy(alpha = .07f), 120.dp, Alignment.TopStart, (-50).dp, (-30).dp, 90_000, stroke = 4.5f)
        ) {
            Row(
                Modifier.statusBarsPadding().padding(start = (g.start - 4.dp).coerceAtLeast(12.dp), end = g.end, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backSrc = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(42.dp).pressScale(backSrc, 0.9f).shadow(3.dp, CircleShape).clip(CircleShape).background(cs.surface.copy(alpha = .85f))
                        .clickable(interactionSource = backSrc, indication = ripple(), role = Role.Button, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back, tint = cs.primary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cs.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f).sharedCard("search-bar", RoundedCornerShape(18.dp))
                ) {
                    Row(Modifier.heightIn(min = 50.dp).padding(start = 14.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, null, tint = cs.primary, modifier = Modifier.sharedIcon("search-icon"))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            if (q.isEmpty()) Text(s.searchHint, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            BasicTextField(
                                value = q, onValueChange = { q = it }, singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus)
                            )
                        }
                        if (q.isNotEmpty()) IconButton(onClick = { q = ""; runCatching { focus.requestFocus() } }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Cancel, s.clear, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = hits.isEmpty() to terms.isEmpty(),
            transitionSpec = { fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.SHORT)) },
            label = "searchState",
            modifier = Modifier.fillMaxSize()
        ) { (empty, blank) ->
            if (empty) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f), modifier = Modifier.size(50.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(
                        if (blank) s.searchEmpty else s.searchNoResults(q), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), textAlign = TextAlign.Center
                    )
                }
            } else {
                androidx.compose.runtime.key(com.osatks.mizan.ui.theme.LocalThemeKey.current) { LazyColumn(
                    Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = g.start, end = g.end, top = 4.dp, bottom = 24.dp + bottom)
                ) {
                    if (blank) item(key = "hint") {
                        Text(s.searchEmpty, Modifier.animateItem().padding(4.dp, 4.dp, 4.dp, 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
                    }
                    var lastGroup: String? = null
                    hits.forEach { h ->
                        if (h.group != lastGroup) {
                            lastGroup = h.group
                            item(key = "g-${h.group}") {
                                Text(
                                    h.group, Modifier.animateItem().padding(start = 6.dp, top = 14.dp, bottom = 6.dp).semantics { heading() },
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        item(key = h.id) {
                            HitRow(h, q, Modifier.animateItem()) { keyboard?.hide(); h.action() }
                        }
                    }
                } }
            }
        }
    }

    if (showLang) LanguageSheet { showLang = false }
}

@Composable
private fun HitRow(h: SearchHit, q: String, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        interactionSource = src, modifier = modifier.fillMaxWidth().padding(vertical = 3.dp).pressScale(src, 0.98f)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(h.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    highlight(h.title, q), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (h.subtitle.isNotEmpty()) {
                    if (h.id.startsWith("v-")) Text(h.subtitle, Modifier.fillMaxWidth(), style = quranStyle(18), color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    else Text(h.subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** يُبرز الجزء المطابق من العنوان إن وُجد حرفياً */
@Composable
private fun highlight(text: String, q: String): AnnotatedString {
    val needle = q.trim()
    val i = if (needle.isEmpty()) -1 else text.indexOf(needle, ignoreCase = true)
    if (i < 0) return AnnotatedString(text)
    val color = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        append(text.substring(0, i))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append(text.substring(i, i + needle.length)) }
        append(text.substring(i + needle.length))
    }
}
