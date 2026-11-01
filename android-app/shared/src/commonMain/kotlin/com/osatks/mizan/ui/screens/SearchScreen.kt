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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Place
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.country
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.Motion
import com.osatks.mizan.ui.pressScale
import com.osatks.mizan.ui.sharedCard
import com.osatks.mizan.ui.sharedIcon
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranSearch
import com.osatks.mizan.ui.quran.ayahRef
import com.osatks.mizan.ui.quran.quranStyle
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/** طلب مؤجل لصفحة تُفتح من البحث (مناسبة بعينها) */
object Pending {
    var event: String? = null
}

private class Hit(
    val group: String, val id: String, val title: String, val subtitle: String,
    val icon: ImageVector, val score: Int, val action: () -> Unit
)

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
    val hits: List<Hit> = remember(q, lang, ayahHits, quran) {
        val out = mutableListOf<Hit>()
        fun add(group: String, id: String, title: String, sub: String, icon: ImageVector, extra: String, action: () -> Unit) {
            val sc = if (terms.isEmpty()) (if (group == s.searchToolsGroup) 1 else 0) else Search.score(terms, title, "$sub $extra")
            if (sc > 0) out += Hit(group, id, title, sub, icon, sc, action)
        }
        TOOL_ORDER.forEach { k ->
            val t = s.tools.getValue(k)
            add(s.searchToolsGroup, "t-$k", t.name, t.desc, TOOL_ICONS.getValue(k),
                "${s.toolKeywords[k]} ${En.tools[k]?.name} ${En.toolKeywords[k]} ${Ar.tools[k]?.name} ${Ar.toolKeywords[k]}") { open(k) }
        }
        if (terms.isNotEmpty()) {
            for (n in 1..QuranMeta.SURAHS) {
                add(s.searchQuranGroup, "q-$n", s.surahName(n),
                    "${QuranMeta.NAMES_AR[n - 1]} · ${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
                    Icons.AutoMirrored.Filled.MenuBook,
                    "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)} ${s.quranTitle}") {
                    open(surahRoute(n, 0, "hit"))
                }
            }
            val refs = (listOfNotNull(QuranSearch.parseRef(q)) + ayahHits).distinct()
            refs.forEachIndexed { k, i ->
                val (sn, an) = QuranMeta.locate(i)
                val sub = quran?.let { QuranData.withoutBismillah(it, i) } ?: ""
                out += Hit(s.searchAyahsGroup, "v-$i", s.ayahRef(i), sub, Icons.AutoMirrored.Filled.MenuBook, 40 - k) { open(surahRoute(sn, an, "hit")) }
            }
            val today = PrayerUtil.times(PrayerUtil.now().date)
            PrayTimes.PRAYERS.forEach { k ->
                add(s.searchPrayersGroup, "p-$k", s.prayer(k), s.time(today[k]), Icons.Filled.AccessTime,
                    "${En.prayer(k)} ${Ar.prayer(k)} ${s.prayerTitle}") { open("prayer") }
            }
            PrayTimes.CITIES.forEach { c ->
                val country = s.country(c.cc, lang)
                add(s.searchCitiesGroup, "c-${c.key}", s.city(c.key), "$country · ${s.setAsMyCity}", Icons.Filled.LocationCity,
                    "${En.city(c.key)} ${Ar.city(c.key)} ${c.cc}") {
                    AppSettings.updatePlace(Place.of(c)); PrayerAlerts.reschedule(); open("prayer")
                }
            }
            s.events.forEach { (k, e) ->
                add(s.searchEventsGroup, "e-$k", e.name, e.desc, Icons.Filled.Event,
                    "${En.events[k]?.name} ${Ar.events[k]?.name}") { Pending.event = k; open("events") }
            }
            s.dhikrLabels.forEachIndexed { i, label ->
                val meaning = s.dhikrMeanings[i].ifEmpty { DHIKR[i] }
                add(s.searchDhikrGroup, "d-$i", label, meaning, Icons.Filled.TouchApp,
                    "${En.dhikrLabels[i]} ${En.dhikrMeanings[i]} ${Ar.dhikrLabels[i]} ${DHIKR[i]}") {
                    AppSettings.putString("ts_mode", "free"); AppSettings.putInt("ts_idx", i); AppSettings.putInt("ts_count", 0)
                    open("tasbih")
                }
            }
            add(s.searchSettingsGroup, "s-lang", s.language, lang.native, Icons.Filled.Translate,
                "${En.language} ${Ar.language} language lang") { showLang = true }
            add(s.searchSettingsGroup, "s-theme", s.appearance, "${s.themeLight} · ${s.themeDark}", Icons.Filled.DarkMode,
                "${En.appearance} ${En.themeDark} ${Ar.appearance} ${Ar.themeDark}") { open("settings") }
            add(s.searchSettingsGroup, "s-adhan", s.adhanAlert, s.adhanAlertDesc, Icons.Filled.NotificationsActive,
                "${En.adhanAlert} ${Ar.adhanAlert}") { open("settings") }
            add(s.searchSettingsGroup, "s-24h", s.use24h, s.use24hDesc, Icons.Filled.AccessTime,
                "${En.use24h} ${Ar.use24h}") { open("settings") }
            add(s.searchSettingsGroup, "s-method", s.calcMethod, s.locationAndMethod, Icons.Filled.Tune,
                "${En.calcMethod} ${Ar.calcMethod} ${s.methods.values.joinToString(" ")}") { open("prayer") }
        }
        val order = listOf(s.searchToolsGroup, s.searchQuranGroup, s.searchAyahsGroup, s.searchPrayersGroup, s.searchCitiesGroup, s.searchEventsGroup, s.searchDhikrGroup, s.searchSettingsGroup)
        // أفضل مجموعة أولاً (بحسب أعلى نتيجة فيها)، وداخلها الأعلى درجة
        val groups = out.groupBy { it.group }
        groups.keys.sortedWith(compareByDescending<String> { g -> groups.getValue(g).maxOf { it.score } }.thenBy { order.indexOf(it) })
            .flatMap { g -> groups.getValue(g).sortedByDescending { it.score }.take(if (terms.isEmpty()) 20 else if (g == s.searchCitiesGroup) 8 else 6) }
    }

    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // على الشاشات العريضة: حقل البحث والنتائج في عمود بعرض مريح في الوسط
    val g = gutters(LocalWindowLayout.current.width, TextColumnMax)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.statusBarsPadding().padding(start = g.start - 12.dp, end = g.end - 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, s.back) }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.weight(1f).sharedCard("search-bar", RoundedCornerShape(18.dp))
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.sharedIcon("search-icon"))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        if (q.isEmpty()) Text(s.searchHint, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        BasicTextField(
                            value = q, onValueChange = { q = it }, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }
                    if (q.isNotEmpty()) IconButton(onClick = { q = "" }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Close, s.clear) }
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
                    Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(if (blank) s.searchEmpty else s.searchNoResults(q), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                androidx.compose.runtime.key(com.osatks.mizan.ui.theme.LocalThemeKey.current) { LazyColumn(
                    Modifier.fillMaxSize().imePadding(),
                    contentPadding = PaddingValues(start = g.start, end = g.end, top = 4.dp, bottom = 24.dp + bottom)
                ) {
                    if (blank) item(key = "hint") {
                        Text(s.searchEmpty, Modifier.animateItem().padding(4.dp, 4.dp, 4.dp, 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    var lastGroup: String? = null
                    hits.forEach { h ->
                        if (h.group != lastGroup) {
                            lastGroup = h.group
                            item(key = "g-${h.group}") {
                                Text(
                                    h.group, Modifier.animateItem().padding(start = 6.dp, top = 14.dp, bottom = 6.dp),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
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
private fun HitRow(h: Hit, q: String, modifier: Modifier, onClick: () -> Unit) {
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
            Column(Modifier.weight(1f)) {
                Text(highlight(h.title, q), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (h.subtitle.isNotEmpty()) {
                    if (h.id.startsWith("v-")) Text(h.subtitle, Modifier.fillMaxWidth(), style = quranStyle(18), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    else Text(h.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
