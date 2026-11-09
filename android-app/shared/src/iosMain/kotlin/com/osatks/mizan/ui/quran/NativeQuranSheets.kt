package com.osatks.mizan.ui.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.Fs
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.encodePng
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.TafsirEntry
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.screens.NativeSheetHost
import com.osatks.mizan.ui.shareText
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.SideEffect
import com.osatks.mizan.ui.LocalWindowLayout

/*
 * أوراق القرآن من النظام في iOS (iosApp/iosApp/NativeQuranSheets.swift و NativeShareSheet.swift عبر NativeSheetHost):
 * التفسير ("tafsir")، وخيارات الآية ("ayahActions")، ومشاركة الآية والقصة صورةً ("shareCard").
 * المنطق كله والنصوص هنا كما في أوراق Compose نفسها؛ و Swift يرسم الحالة ويعيد اللمسات.
 */

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** اتجاه اللغة والمظهر وألوان التطبيق (بأسماء ألوان الرئيسية في Swift)، ومعها ما تحتاجه أوراق القرآن */
@Composable
private fun quranSheetTheme(): Map<String, Any?> {
    val cs = MaterialTheme.colorScheme
    val accent = LocalAccent.current
    return linkedMapOf(
        "rtl" to LocalLang.current.rtl, "dark" to LocalDark.current,
        "c" to linkedMapOf(
            "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
            "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
            "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
            "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
            "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
        ),
        "onPrimary" to hex(cs.onPrimary), "secondary" to hex(cs.secondary),
    )
}

// ---------- التفسير ----------

/** أجزاء الفقرة: الآيات المقتبسة ﴿...﴾ تُكتب بخط المصحف ولون مميز (quotedAyahs في TafsirSheet.kt) */
private fun quotedSegments(text: String): List<Map<String, Any?>> {
    val out = ArrayList<Map<String, Any?>>()
    var i = 0
    for (m in Regex("﴿[^﴾]{1,400}﴾").findAll(text)) {
        if (m.range.first > i) out += linkedMapOf("t" to text.substring(i, m.range.first), "q" to false)
        out += linkedMapOf("t" to m.value, "q" to true)
        i = m.range.last + 1
    }
    if (i < text.length) out += linkedMapOf("t" to text.substring(i), "q" to false)
    return out
}

/**
 * ورقة التفسير من النظام: ما في [TafsirContent] نفسه (رأس الآية والحفظ والنسخ والمشاركة، ونصها، والاستماع منها،
 * والمصادر ومكتبة التفاسير، والتفسير بفقراته، والآية السابقة والتالية).
 */
@Composable
internal fun NativeTafsirSheet(
    surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit, onLibrary: () -> Unit,
    onListen: ((Int) -> Unit)?, onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val clipboard = LocalClipboardManager.current
    val src = QuranPrefs.sourceFor(lang)
    val reqKey = "${src.id}:$surah:$ayah"
    val loaded by produceState<Pair<String, TafsirEntry?>?>(null, reqKey) { value = reqKey to Sources.entry(src, surah, ayah) }
    val entry = Sources.cachedEntry(src, surah, ayah) ?: loaded?.takeIf { it.first == reqKey }?.second
    val count = QuranMeta.COUNTS[surah - 1]
    val index = QuranMeta.index(surah, ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val marked = QuranPrefs.isBookmarked(index)
    val mp = mushafPalette()
    val paras = remember(entry) {
        entry?.text?.split(Regex("\\n\\s*\\n"))?.filter { it.isNotBlank() }?.map { quotedSegments(it.trim()) }.orEmpty()
    }
    val json = Json.write(
        quranSheetTheme() + linkedMapOf(
            "paper" to hex(mp.paper), "ink" to hex(mp.ink), "marker" to hex(mp.marker),
            "title" to s.tafsirTitle, "close" to s.close,
            "ayah" to ayah, "num" to arabicDigits(ayah), "ref" to ref,
            "place" to "${s.juzName(QuranMeta.juzOf(index))} · ${s.pageName(QuranMeta.pageOf(index))}",
            "marked" to marked, "bookmarkLabel" to (if (marked) s.removeBookmark else s.bookmarkAyah),
            "copyLabel" to s.copy, "copiedLabel" to s.copied, "shareLabel" to s.share,
            // علامة نهاية الآية برقمها (۝ والأرقام المشرقية يرسمها خط المصحف حلقةً) كما في بطاقة المشاركة
            "verse" to ayahText, "end" to "۝${arabicDigits(ayah)}",
            "listen" to (if (onListen != null) s.listenFromHere else null),
            "sources" to Sources.visible(lang).map { o ->
                linkedMapOf("id" to o.id, "label" to Sources.label(o, s, lang), "selected" to (o.id == src.id))
            },
            "more" to "+ ${s.moreTafsirs}",
            "meta" to "${Sources.kindLabel(src, s)} · ${src.author}",
            "loading" to (entry == null),
            "range" to entry?.takeIf { it.from != it.to }?.let { s.ayahRange(it.from, it.to) },
            "srcRtl" to src.lang.rtl,
            "paras" to paras,
            "prev" to s.prevAyah, "next" to s.nextAyah, "hasPrev" to (ayah > 1), "hasNext" to (ayah < count),
        )
    )
    NativeSheetHost("tafsir", json, startLarge = false, onDismiss = onDismiss) { action, arg ->
        when (action) {
            "bookmark" -> QuranPrefs.toggleBookmark(index)
            "copy" -> {
                clipboard.setText(AnnotatedString("$ayahText\n[$ref]"))
                Platform.toast(s.copied)
            }
            // قائمة المشاركة تُعرض فوق الورقة (أعلى متحكم ظاهر)
            "share" -> shareText(s, "﴿$ayahText﴾\n[$ref]\n\n${Sources.label(src, s, lang)}:\n${entry?.text.orEmpty()}")
            // الاستماع يفتح إعدادات التلاوة في الصفحة: تُغلق الورقة أولاً حتى لا تغطيها
            "listen" -> { onListen?.let { l -> close { l(ayah); onDismiss() } } }
            "source" -> QuranPrefs.updateSource(arg)
            "library" -> close { onLibrary(); onDismiss() }
            "prev" -> { if (ayah > 1) onAyah(ayah - 1) }
            "next" -> { if (ayah < count) onAyah(ayah + 1) }
            "close" -> close(onDismiss)
            else -> {}
        }
    }
}

// ---------- خيارات الآية ----------

/** خيارات الآية من النظام: ما في [AyahActionBar] نفسه (التفسير، والاستماع، والحفظ، والنسخ، والمشاركة) */
@Composable
internal fun NativeAyahActions(
    index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onShare: () -> Unit, onClose: () -> Unit,
) {
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val (surah, ayah) = QuranMeta.locate(index)
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val ayahText = QuranData.withoutBismillah(text, index)
    val marked = QuranPrefs.isBookmarked(index)
    val json = Json.write(
        quranSheetTheme() + linkedMapOf(
            "num" to arabicDigits(ayah), "ref" to ref, "close" to s.close,
            "tafsir" to s.tafsirTitle, "listen" to s.listen, "save" to (if (marked) s.saved else s.save),
            "copy" to s.copy, "share" to s.share, "marked" to marked,
        )
    )
    NativeSheetHost("ayahActions", json, startLarge = false, onDismiss = onClose) { action, _ ->
        // ما يفتح ورقة أو لوحة أخرى يغلق هذه أولاً (ورقتان فوق بعض تُغلقان معاً)
        when (action) {
            "tafsir" -> close(onTafsir)
            "listen" -> close(onListen)
            "share" -> close(onShare)
            "bookmark" -> QuranPrefs.toggleBookmark(index)
            "copy" -> {
                clipboard.setText(AnnotatedString("$ayahText\n[$ref]"))
                close { Platform.toast(s.copied); onClose() }
            }
            "close" -> close(onClose)
            else -> {}
        }
    }
}

// ---------- مشاركة الآية والقصة صورةً ----------

/** مشاركة الآية من النظام: ما في [AyahShareContent] نفسه (بطاقة أو قصة، والألوان، والمعنى، وصورة أو نص) */
@Composable
internal fun NativeAyahShare(index: Int, text: Array<String>, onDone: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ft = remember(lang) { FeatureText.of(lang.code) }
    val ht = remember(lang) { HomeText.of(lang.code) }
    val (surah, ayah) = remember(index) { QuranMeta.locate(index) }
    val ayahText = remember(index, text) { QuranData.withoutBismillah(text, index) }
    val ref = s.surahAyah(s.surahName(surah), ayah)
    val source = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    var meaning by remember(index, source) { mutableStateOf<String?>(null) }
    LaunchedEffect(index, source) {
        meaning = source?.let { runCatching { QuranData.source(it.id) }.getOrNull()?.getOrNull(index)?.trim()?.takeIf { m -> m.isNotEmpty() } }
    }
    val plain = "﴿$ayahText﴾\n[$ref]"
    NativeShareCard(
        title = ft.shareAyah, sub = ref, plain = plain, hasMeaning = meaning != null, formats = ht.post to ht.story,
        contentKey = listOf(index, meaning), onDone = onDone, onDismiss = onDismiss,
    ) { story, style, withMeaning, modifier ->
        if (story) StoryCard(
            StoryContent("سُورَةُ ${QuranMeta.NAMES_AR[surah - 1]}", "$ayahText ۝${arabicDigits(ayah)}", meaning, ref, plain),
            withMeaning, style, s.appName, modifier,
        ) else AyahCard(ayahText, surah, ayah, ref, meaning.takeIf { withMeaning }, style, s.appName, modifier)
    }
}

/** مشاركة القصة (آية أو حديث أو ذكر) من النظام: ما في [StoryShareContent] نفسه */
@Composable
internal fun NativeStoryShare(content: StoryContent, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = remember(lang) { HomeText.of(lang.code) }
    NativeShareCard(
        title = ht.shareStory, sub = content.kind, plain = content.plain, hasMeaning = content.meaning != null, formats = null,
        contentKey = content, onDone = onDismiss, onDismiss = onDismiss,
    ) { _, style, withMeaning, modifier -> StoryCard(content, withMeaning, style, s.appName, modifier) }
}

/** يسجّل المحتوى في [layer] بمقاسه ولا يرسم شيئاً على الشاشة */
private fun Modifier.recordOnly(layer: GraphicsLayer): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
}

/** يسجّل القصة في [layer] بمقاس 1080×1920 مكبَّرة (كـ recordStory) ولا يرسم شيئاً على الشاشة */
private fun Modifier.recordStoryOnly(layer: GraphicsLayer): Modifier = drawWithContent {
    val k = STORY_WIDTH / size.width
    layer.record(IntSize(STORY_WIDTH, STORY_HEIGHT)) {
        scale(k, k, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
    }
}

/** يرسم [content] بلا مكان في التخطيط ولا في إمكانية الوصول: يُقاس بلا قيود ويوضع بمقاس صفر فلا يزيح شيئاً */
@Composable
private fun Offscreen(content: @Composable () -> Unit) {
    Layout(content, Modifier.clearAndSetSemantics {}) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(0, 0) { placeables.forEach { it.place(0, 0) } }
    }
}

/**
 * ورقة المشاركة صورةً: البطاقة نفسها ترسمها Compose (فيبقى تصميمها كما هو تماماً) خارج الشاشة، وتُحفظ PNG في
 * مجلد مؤقت، ويعرضها Swift معاينةً ويشاركها بقائمة المشاركة في النظام (UIActivityViewController) مع النص.
 * [formats] اسما الشكلين (بطاقة وقصة) أو null للقصة وحدها، و[card] يرسم البطاقة بالشكل واللون والمعنى المختارة.
 */
@Composable
private fun NativeShareCard(
    title: String, sub: String, plain: String, hasMeaning: Boolean, formats: Pair<String, String>?,
    contentKey: Any?, onDone: () -> Unit, onDismiss: () -> Unit,
    card: @Composable (story: Boolean, style: CardStyle, withMeaning: Boolean, modifier: Modifier) -> Unit,
) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ft = remember(lang) { FeatureText.of(lang.code) }
    var withMeaning by rememberSaveable { mutableStateOf(true) }
    var style by rememberSaveable { mutableStateOf(CardStyle.EMERALD) }
    var storyPick by rememberSaveable { mutableStateOf(false) }
    val story = formats == null || storyPick
    val accent = AppSettings.accent
    val layer = rememberGraphicsLayer()
    var preview by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    val files = remember { ArrayList<String>() }
    var version by remember { mutableIntStateOf(0) }
    val dir = remember { "${Platform.cacheDir}/share-cards" }

    // البطاقة كما تُشارَك: بعرض المعاينة في Compose (330 للبطاقة، و250 للقصة مكبَّرة إلى 1080×1920)
    Offscreen {
        if (story) card(true, style, withMeaning, Modifier.width(250.dp).recordStoryOnly(layer))
        else card(false, style, withMeaning, Modifier.width(330.dp).recordOnly(layer))
    }
    // بعد كل تغيير: تُنتظر رسمتها ثم تُحفظ PNG وتُرسل إلى المعاينة (وإن تعذّر بعد قليل شاركت نصاً كما في Compose)
    LaunchedEffect(story, style, withMeaning, contentKey, accent) {
        ready = false
        var png: ByteArray? = null
        for (attempt in 0 until 12) {
            withFrameNanos { }
            withFrameNanos { }
            val bmp = runCatching { layer.toImageBitmap() }.getOrNull()
            if (bmp != null && bmp.width > 1 && bmp.height > 1) png = runCatching { bmp.encodePng() }.getOrNull()
            if (png != null) break
        }
        val bytes = png
        if (bytes == null) {
            preview = null
        } else {
            version++
            val path = "$dir/card-$version.png"
            val ok = withContext(Dispatchers.Default) { runCatching { Fs.write(path, bytes) }.isSuccess }
            preview = if (ok) path else null
            if (ok) files += path
            // الصورة السابقة قرأها Swift في ذاكرته؛ ويبقى ملف واحد قبلها احتياطاً
            while (files.size > 2) Fs.delete(files.removeAt(0))
        }
        ready = true
    }
    DisposableEffect(Unit) { onDispose { runCatching { Fs.deleteDir(dir) } } }

    val json = Json.write(
        quranSheetTheme() + linkedMapOf(
            "title" to title, "sub" to sub, "close" to s.close,
            "formats" to formats?.let { listOf(it.first, it.second) }, "story" to story,
            "styles" to CardStyle.entries.map { st ->
                val c = cardColors(st, accent)
                linkedMapOf(
                    "key" to st.name,
                    "label" to when (st) {
                        CardStyle.EMERALD -> s.appColor
                        CardStyle.PAPER -> s.toneSepia
                        CardStyle.NIGHT -> s.toneNight
                    },
                    "top" to hex(c.top), "bottom" to hex(c.bottom), "gold" to hex(c.gold), "selected" to (st == style),
                )
            },
            "meaningLabel" to (if (hasMeaning) ft.withMeaning else null), "withMeaning" to withMeaning,
            "preview" to preview, "ready" to ready,
            "imageLabel" to ft.image, "textLabel" to ft.text,
            "shareText" to "$plain\n${s.shareSignature}",
        )
    )
    NativeSheetHost("shareCard", json, startLarge = true, onDismiss = onDismiss) { action, arg ->
        when (action) {
            "format" -> { storyPick = arg == "story" }
            "style" -> { CardStyle.entries.firstOrNull { it.name == arg }?.let { style = it } }
            "meaning" -> { withMeaning = arg == "1" }
            // شاركها Swift فعلاً من قائمة النظام: تُغلق الورقة كما تُغلق لوحة Compose بعد المشاركة
            "shared" -> close(onDone)
            "close" -> close(onDismiss)
            else -> {}
        }
    }
}

// ---------- ما تستدعيه صفحة السورة المشتركة ----------

/** التفسير داخل لوحة القراءة: ورقة النظام في iPhone، وعلى الشاشات العريضة (iPad) اللوحة الجانبية من Compose */
@Composable
actual fun TafsirContent(surah: Int, ayah: Int, text: Array<String>, onAyah: (Int) -> Unit, onLibrary: () -> Unit, onListen: ((Int) -> Unit)?) {
    // ورقة النظام بدل اللوحة (تخفي اللوحة زجاجها)، وسحبها لإغلاقها يغلق اللوحة. وعلى الشاشات العريضة تبقى اللوحة
    // الجانبية: لا تحجب الصفحة، ويتبع التفسير الآية الملموسة
    val slot = LocalReadingPanel.current
    if (slot != null) {
        val native = !LocalWindowLayout.current.wide
        SideEffect { slot.native = native }
        if (native) {
            NativeTafsirSheet(surah, ayah, text, onAyah, onLibrary, onListen, onDismiss = { slot.dismiss() })
            return
        }
    }
    TafsirBody(surah, ayah, text, onAyah, onLibrary, onListen)
}

/** خيارات الآية: ورقة النظام القصيرة بالخيارات نفسها، فلا يُرسم في الصفحة شيء */
@Composable
internal actual fun AyahActionBar(index: Int, text: Array<String>, onTafsir: () -> Unit, onListen: () -> Unit, onShare: () -> Unit, onClose: () -> Unit) =
    NativeAyahActions(index, text, onTafsir, onListen, onShare, onClose)

/** مشاركة الآية داخل لوحة القراءة: ورقة النظام بمعاينة البطاقة نفسها (ترسمها Compose خارج الشاشة) وقائمة المشاركة */
@Composable
internal actual fun AyahShareContent(index: Int, text: Array<String>, onDone: () -> Unit) {
    val slot = LocalReadingPanel.current ?: return
    SideEffect { slot.native = true }
    NativeAyahShare(index, text, onDone, onDismiss = { slot.dismiss() })
}
