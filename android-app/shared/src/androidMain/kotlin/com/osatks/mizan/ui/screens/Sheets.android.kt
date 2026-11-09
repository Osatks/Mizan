package com.osatks.mizan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.i18n.pack.LanguagePacks
import com.osatks.mizan.platform.Platform
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Color
import com.osatks.mizan.ui.LocalAppHaze
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.GlassTone
import com.osatks.mizan.ui.floatingGlass
import com.osatks.mizan.ui.glassMaterial
import com.osatks.mizan.ui.glassTone
import com.osatks.mizan.ui.PlatformDialog
import com.osatks.mizan.ui.DialogAction
import com.osatks.mizan.ui.ActionRole
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.osatks.mizan.ui.glass
import com.osatks.mizan.ui.glassRim
import com.osatks.mizan.ui.glassStyle
import com.osatks.mizan.ui.theme.LocalDark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.osatks.mizan.platform.systemLanguageTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.City
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.LocaleSync
import com.osatks.mizan.i18n.city
import com.osatks.mizan.i18n.country
import kotlinx.coroutines.launch

/**
 * اختيار اللغة: كل لغة باسمها الأصلي وتحته اسمها بلغة الواجهة.
 * وتحتها اللغات المتاحة للتنزيل من موقع ميزان (حزم اللغات)، وتحديث ما صدر له إصدار أحدث.
 * بطراز ورقة iPhone: خلفية التطبيق، وشريط بالعنوان وزر إغلاق، والصفوف في بطاقات بيضاء مفصولة بخطوط رفيعة.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun LanguageSheet(onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    GlassSheet(onDismissRequest = onDismiss, sheetState = sheet, background = MaterialTheme.colorScheme.background) {
        LanguageList(onClose = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } }) { code ->
            scope.launch { sheet.hide() }.invokeOnCompletion {
                onDismiss()
                if (code != AppSettings.lang) LocaleSync.choose(code)
            }
        }
    }
}

/** محتوى ورقة اللغات: المثبتة، ثم المتاحة للتنزيل */
@Composable
internal fun LanguageList(state: LazyListState = rememberLazyListState(), onClose: (() -> Unit)? = null, pick: (String) -> Unit) {
    val s = LocalStrings.current
    val ui = LocalLang.current
    val scope = rememberCoroutineScope()
    val system = Lang.fromTag(systemLanguageTag())
    var confirmDelete by remember { mutableStateOf<Lang?>(null) }
    fun displayName(code: String, fallback: String) =
        Platform.displayLanguage(code, ui.code).takeIf { it.isNotEmpty() && it != code }
            ?.replaceFirstChar { it.titlecase() } ?: fallback
    LaunchedEffect(Unit) { if (LanguagePacks.remote == null) LanguagePacks.refresh() }
    Column {
        SheetTitleBar(s.language, Icons.Filled.Language, s.close, onClose)
        val langs = Lang.entries
        val offers = LanguagePacks.offers()
        LazyColumn(state = state, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp)) {
            item {
                SheetCardRow(0, langs.size + 1) {
                    OptionRow(s.deviceLanguage, system.native, AppSettings.lang == Lang.SYSTEM) { pick(Lang.SYSTEM) }
                }
            }
            itemsIndexed(langs, key = { _, l -> l.code }) { i, l ->
                val sub = displayName(l.code, l.pack?.english ?: l.code)
                val removable = l.isPack && LanguagePacks.isDownloaded(l.code) && !LanguagePacks.isBundled(l.code) && AppSettings.lang != l.code
                SheetCardRow(i + 1, langs.size + 1) {
                    OptionRow(l.native, if (l == ui) null else sub, AppSettings.lang == l.code,
                        trailing = if (removable) { { IconButton(onClick = { confirmDelete = l }) { Icon(Icons.Outlined.Delete, s.delete, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } } else null,
                    ) { pick(l.code) }
                }
            }
            // ---------- لغات للتنزيل ----------
            item(key = "more") {
                Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 26.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDownload, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            s.moreLanguages, Modifier.weight(1f).semantics { heading() },
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary
                        )
                        if (LanguagePacks.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(s.langPackHint, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (LanguagePacks.checkFailed) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.downloadFailed, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { scope.launch { LanguagePacks.refresh() } }) { Text(s.retry, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            itemsIndexed(offers, key = { _, r -> "dl:" + r.code }) { i, r ->
                val pct = LanguagePacks.progress[r.code]
                val isUpdate = Lang.of(r.code) != null
                fun go() = LanguagePacks.start(r) { ok -> if (ok && !isUpdate) pick(r.code) }
                val sub = displayName(r.code, r.english) + (if (r.sizeMb > 0) " · " + s.sizeMb(Fmt.num(r.sizeMb.toDouble(), 1, ui)) else "")
                SheetCardRow(i, offers.size) {
                    OptionRow(r.native, sub, false, trailing = {
                        when {
                            pct != null && pct >= 0 -> Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("$pct%", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    progress = { pct / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f), strokeCap = StrokeCap.Round
                                )
                            }
                            pct == -1 -> TextButton(onClick = ::go) { Text(s.retry, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
                            isUpdate -> TextButton(onClick = ::go) { Text(s.update, fontWeight = FontWeight.SemiBold) }
                            else -> IconButton(onClick = ::go) { Icon(Icons.Outlined.ArrowCircleDown, s.download, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary) }
                        }
                    }) { if (pct == null || pct < 0) go() }
                }
            }
        }
    }
    confirmDelete?.let { l ->
        PlatformDialog(
            onDismissRequest = { confirmDelete = null },
            title = s.deleteTafsirQ(l.native),
            actions = listOf(
                DialogAction(s.cancel, ActionRole.Cancel) { confirmDelete = null },
                DialogAction(s.delete, ActionRole.Destructive) { LanguagePacks.delete(l.code); confirmDelete = null },
            ),
        )
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, selected: Boolean, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    // المختار بلون التطبيق الخفيف وعلامة صح في دائرة (كما في iPhone)
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else Color.Transparent, label = "opt")
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().background(bg)
            .selectable(selected, interactionSource = src, indication = ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(start = 16.dp, end = if (trailing != null) 6.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
        AnimatedVisibility(selected, enter = scaleIn(spring(dampingRatio = 0.5f)), exit = scaleOut()) {
            Icon(Icons.Filled.CheckCircle, null, Modifier.padding(start = 8.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** شريط الورقة: الأيقونة والعنوان في الوسط، وزر الإغلاق في جهة النهاية (دائرة باهتة فيها ×) */
@Composable
private fun SheetTitleBar(title: String, icon: ImageVector?, closeLabel: String, onClose: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.padding(horizontal = 44.dp).semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp), tint = cs.primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = cs.onSurface, maxLines = 1)
        }
        if (onClose != null) Box(
            Modifier.align(Alignment.CenterEnd).size(30.dp).clip(CircleShape).background(cs.onSurfaceVariant.copy(alpha = .12f))
                .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onClose)
                .semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, null, Modifier.size(16.dp), tint = cs.onSurfaceVariant) }
    }
}

/**
 * صف من بطاقة بيضاء ممتدة على عدة عناصر في القائمة: الأول بزاويتين علويتين مستديرتين (20) والأخير بالسفليتين،
 * وفوق كل صف عدا الأول خط رفيع يبدأ بعد هامش 16
 */
@Composable
private fun SheetCardRow(index: Int, count: Int, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val r = 20.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) r else 0.dp, topEnd = if (index == 0) r else 0.dp,
        bottomStart = if (index == count - 1) r else 0.dp, bottomEnd = if (index == count - 1) r else 0.dp,
    )
    val line = cs.outlineVariant.copy(alpha = .6f)
    Box(
        Modifier.fillMaxWidth().clip(shape).background(cs.surface).drawWithContent {
            drawContent()
            if (index > 0) {
                val h = 0.5.dp.toPx()
                val inset = 16.dp.toPx()
                val rtl = layoutDirection == LayoutDirection.Rtl
                drawRect(line, Offset(if (rtl) 0f else inset, 0f), Size(size.width - inset, h))
            }
        }
    ) { content() }
}

/**
 * ورقة سفلية زجاجية بأسلوب iOS: زوايا عليا مستديرة 32، ومقبض صغير في أعلاها، وسطحها يموّه الصفحة التي خلفها
 * (من مصدر التطبيق [LocalAppHaze]) بطبقة من لون السطح كثيفة تكفي لقراءة النص فوق أي محتوى.
 * التمويه بثلث الدقة ونصف قطر محدود؛ ويمتد السطح تحت شريط التنقل السفلي ثم يُزاح المحتوى عنه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(onDismissRequest: () -> Unit, sheetState: SheetState, background: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    // على iPad والشاشات العريضة: نافذة في الوسط لا ورقة بعرض الشاشة (sheetState.hide() بلا ورقة ينتهي فوراً، فيُغلق بعده)
    if (LocalWindowLayout.current.wide) {
        GlassSheetDialog(onDismissRequest, background, content)
        return
    }
    val dark = LocalDark.current
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState, shape = shape,
        containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = if (dark) .45f else .25f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            Modifier.fillMaxWidth()
                // [background]: سطح مصمت بلون الخلفية كأوراق iPhone ذات البطاقات البيضاء، وإلا فزجاج
                .then(
                    if (background != null) Modifier.clip(shape).background(background)
                    else Modifier.glass(LocalAppHaze.current, shape, glassStyle(if (dark) .80f else .82f, 32.dp, fallback = 1f), glassRim(.8f))
                )
                .navigationBarsPadding()
        ) {
            // المقبض: كبسولة صغيرة رمادية كما في أوراق iOS
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 14.dp)
                    .size(width = 36.dp, height = 5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) .32f else .22f))
            )
            content()
        }
    }
}

/**
 * الورقة نفسها على iPad والشاشات العريضة: نافذة زجاجية في الوسط بزوايا مستديرة 32، بعرض أقصاه 560 وارتفاع أقصاه
 * نحو أربعة أخماس النافذة، تُغلق بلمس ما حولها أو بالرجوع (كأوراق iPadOS التي تظهر في الوسط).
 */
@Composable
private fun GlassSheetDialog(onDismissRequest: () -> Unit, background: Color?, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val maxH = LocalWindowLayout.current.height * 0.84f
    val haze = LocalAppHaze.current
    // تدخل بنابض خفيف من حجم أصغر قليلاً كنوافذ iPadOS
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, spring(dampingRatio = .74f, stiffness = 420f)) }
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = maxH)
                .graphicsLayer { val k = .92f + .08f * shown.value; scaleX = k; scaleY = k; alpha = shown.value.coerceIn(0f, 1f) }
                .then(
                    if (background != null) Modifier.shadow(24.dp, shape).clip(shape).background(background)
                    else Modifier.floatingGlass(haze, shape, glassMaterial(if (glassTone() == GlassTone.Dark) .80f else .84f, fallback = 1f), elevation = 24.dp)
                )
                .padding(top = if (background != null) 14.dp else 22.dp, bottom = 8.dp)
        ) {
            content()
        }
    }
}

/** اختيار مدينة مع بحث فوري بأي لغة (اسم المدينة أو الدولة) */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
actual fun CityPickerSheet(selectedKey: String?, onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    val all = remember(lang) {
        PrayTimes.CITIES.map { c ->
            val country = s.country(c.cc, lang)
            Triple(c, country, Search.normalize("${s.city(c.key)} ${En.city(c.key)} ${Ar.city(c.key)} $country ${c.cc}"))
        }
    }
    val shown = remember(q, all) {
        val terms = Search.normalize(q).split(' ').filter { it.isNotEmpty() }
        if (terms.isEmpty()) all else all.filter { (_, _, hay) -> terms.all { hay.contains(it) } }
    }
    val close: () -> Unit = { scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() } }
    GlassSheet(onDismissRequest = onDismiss, sheetState = sheet, background = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxHeight(0.92f).imePadding()) {
            SheetTitleBar(s.pickCity, null, s.close, close)
            Spacer(Modifier.height(8.dp))
            SheetSearchField(q, { q = it }, s.searchCity, s.clear)
            if (shown.isEmpty()) {
                Text(
                    s.noResults, Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp)) {
                itemsIndexed(shown, key = { _, it -> it.first.key }) { i, (c, country, _) ->
                    val on = c.key == selectedKey
                    Box(Modifier.animateItem()) {
                        SheetCardRow(i, shown.size) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (on) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else Color.Transparent)
                                    .selectable(on, role = Role.RadioButton) {
                                        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); onPick(c) }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocationCity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(s.city(c.key), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                    Text(country, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (on) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** حقل البحث في الورقة كحقل بحث النظام في iPhone: مستطيل رمادي خفيف بزوايا 12، وعدسة، وزر مسح */
@Composable
private fun SheetSearchField(value: String, onChange: (String) -> Unit, hint: String, clearLabel: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
            .background(cs.onSurface.copy(alpha = .07f)).padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
            val style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
            if (value.isEmpty()) Text(hint, style = style, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = style.copy(color = cs.onSurface), cursorBrush = SolidColor(cs.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Cancel, clearLabel, Modifier.size(18.dp), tint = cs.onSurfaceVariant)
        }
    }
}
