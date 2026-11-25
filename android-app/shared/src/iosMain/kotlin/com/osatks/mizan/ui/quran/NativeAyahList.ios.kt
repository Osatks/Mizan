package com.osatks.mizan.ui.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.LocalWindowLayout
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.theme.LocalDark
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر عرض آية آية من النظام (iosApp/iosApp/NativeAyahList.swift): Swift يسجّل عند التشغيل ما ينشئ متحكم القائمة ([attach])،
 * فيعرض SurahScreen آيات السورة به بدل LazyColumn. Kotlin يرسل حالة القائمة JSON ([NativeAyahListSession.state]) مع الإشعار [STATE]
 * — بلا نصوص الآيات، يقرؤها Swift من الجلسة حين تظهر الآية ([NativeAyahListSession.ayahText]) فلا تُرسل 286 آية مع كل تغيّر —
 * و Swift يخبره بأول آية ظاهرة وباللمس والضغط المطوّل، كما من LazyColumn.
 */
object NativeAyahList {
    /** الإشعار مع كل تغيّر في حالة قائمة (object: جلستها) */
    const val STATE = "MizanAyahListState"

    private var factory: ((NativeAyahListSession) -> UIViewController)? = null

    /** يستدعيه Swift مرة عند التشغيل (قبل MainViewController) */
    fun attach(make: (NativeAyahListSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeAyahListSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeAyahListAvailable: Boolean get() = NativeAyahList.available

/** قائمة واحدة معروضة: حالتها، وما يصل منها إلى الشاشة */
class NativeAyahListSession internal constructor() {
    /** آخر حالة أُرسلت (JSON)، تقرؤها القائمة حين تُنشأ ومع كل إشعار */
    var state: String = ""
        private set

    internal var scroll: NativeAyahScroll? = null
    internal var quran: Array<String>? = null
    internal var trText: Array<String>? = null
    internal var onTap: (Int) -> Unit = {}
    internal var onLongPress: (Int) -> Unit = {}
    internal var onTafsir: (Int) -> Unit = {}
    internal var onScrollUp: () -> Unit = {}
    internal var onSurah: (Int) -> Unit = {}

    /** الحالة الأولى قبل أن تُنشأ القائمة (بلا إشعار) */
    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeAyahList.STATE, this)
    }

    // ---------- من Swift ----------

    /** لمس الآية [index] (فهرسها في المصحف كله) */
    fun tap(index: Int) = onTap(index)

    /** ضغط مطوّل على الآية: شريط خياراتها */
    fun longPress(index: Int) = onLongPress(index)

    /** إجراء قارئ الشاشة: تفسير الآية مباشرة */
    fun tafsir(index: Int) = onTafsir(index)

    /** إجراء قارئ الشاشة: حفظ الآية أو إزالتها من المحفوظات */
    fun bookmark(index: Int) {
        if (index in 0 until QuranMeta.AYAHS) QuranPrefs.toggleBookmark(index)
    }

    /** صار أول عنصر ظاهر [first] (0 الترويسة، ثم رقم الآية) */
    fun scrolled(first: Int) { scroll?.scrolled(first) }

    /** مرّر القارئ القائمة إلى الأعلى (يُظهر أدوات وضع التركيز) */
    fun scrolledUp() = onScrollUp()

    /** الانتقال إلى السورة [surah] من آخر القائمة */
    fun openSurah(surah: Int) = onSurah(surah)

    /** الموضع الذي تبدأ منه القائمة حين تُنشأ (الآية المطلوبة، أو حيث كان القارئ قبل أن يغادر) */
    fun top(): Int = scroll?.first ?: 0

    // ---------- النصوص ----------

    /** نص الآية بلا البسملة في أول السورة (تُكتب في الترويسة) */
    fun ayahText(index: Int): String {
        val t = quran ?: return ""
        return if (index in t.indices) QuranData.withoutBismillah(t, index) else ""
    }

    /** ترجمة معاني الآية بلغة الواجهة، أو null */
    fun translation(index: Int): String? = trText?.getOrNull(index)

    /** رقم الآية بالأرقام المشرقية (علامة نهاية الآية كما في المصحف) */
    fun digits(n: Int): String = arabicDigits(n)
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeAyahListView(
    scroll: NativeAyahScroll, surah: Int, text: Array<String>, translation: Array<String>?, trRtl: Boolean,
    selected: Int, size: Int, bottomInset: Dp,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onTafsir: (Int) -> Unit, onScrollUp: () -> Unit, onSurah: (Int) -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeAyahListSession() }
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val tafsir by rememberUpdatedState(onTafsir)
    val up by rememberUpdatedState(onScrollUp)
    val surahTo by rememberUpdatedState(onSurah)
    session.scroll = scroll
    session.quran = text
    session.trText = translation
    DisposableEffect(session) {
        session.onTap = { tap(it) }
        session.onLongPress = { long(it) }
        session.onTafsir = { tafsir(it) }
        session.onScrollUp = { up() }
        session.onSurah = { surahTo(it) }
        onDispose {
            session.onTap = {}; session.onLongPress = {}; session.onTafsir = {}; session.onScrollUp = {}; session.onSurah = {}
        }
    }

    val s = LocalStrings.current
    val p = mushafPalette()
    val cs = MaterialTheme.colorScheme
    val window = LocalWindowLayout.current
    // على الشاشات العريضة عمود الآيات في الوسط بعرض مريح (كما في LazyColumn)
    val g = gutters(window.width, 680.dp, min = 12.dp)
    val count = QuranMeta.COUNTS[surah - 1]
    val base = QuranMeta.index(surah, 1)
    val range = base until base + count
    val marks = QuranPrefs.bookmarks.filter { it in range }.map { it - base + 1 }.sorted()
    fun nav(n: Int, label: String) = linkedMapOf<String, Any?>("surah" to n, "label" to label, "name" to s.surahName(n))
    val jump = scroll.jump
    val json = Json.write(
        linkedMapOf<String, Any?>(
            "surah" to surah, "count" to count, "base" to base,
            "title" to "سُورَةُ ${QuranMeta.NAMES_AR[surah - 1]}",
            "basmala" to (if (QuranData.hasBismillahHeader(surah)) text[0] else null),
            "subtitle" to s.quranSubtitle,
            "selected" to (if (selected in range) selected - base + 1 else 0),
            "marks" to marks,
            "size" to size, "tr" to (translation != null), "trRtl" to trRtl,
            // تتغير حين تصل ترجمة أخرى (تغيير اللغة أو المصدر): يُعاد Swift قراءة النصوص
            "trKey" to "${translation?.size ?: 0}-${translation?.hashCode() ?: 0}-${text.hashCode()}",
            "jump" to jump.item, "animate" to jump.animate, "follow" to jump.follow, "seq" to jump.seq,
            "bottom" to (bottomInset.value + 24f).toDouble(), "cover" to bottomInset.value.toDouble(),
            "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
            "rtl" to (LocalLayoutDirection.current == LayoutDirection.Rtl), "dark" to LocalDark.current,
            "prev" to (if (surah > 1) nav(surah - 1, s.prevSurah) else null),
            "next" to (if (surah < QuranMeta.SURAHS) nav(surah + 1, s.nextSurah) else null),
            "hint" to s.ayahActionsHint, "tafsir" to s.tafsirTitle, "save" to s.bookmarkAyah, "unsave" to s.removeBookmark,
            "paper" to hex(p.paper), "paperEdge" to hex(p.paperEdge), "ink" to hex(p.ink), "muted" to hex(p.muted),
            "gold" to hex(p.gold), "goldDeep" to hex(p.goldDeep), "fill" to hex(p.fill), "fillDeep" to hex(p.fillDeep),
            "marker" to hex(p.marker), "select" to hex(p.select),
            "accent" to hex(cs.primary), "outline" to hex(cs.outline),
        )
    )
    session.prime(json)
    SideEffect { session.publish(json) }
    UIKitViewController(
        factory = { NativeAyahList.make(session) },
        modifier = modifier,
        // اللمس يذهب إلى القائمة مباشرة: التمرير يبدأ مع أول حركة للإصبع كما في النظام
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
    )
}
