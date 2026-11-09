package com.osatks.mizan.ui.quran

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.ScriptText
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.Madinah
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.theme.LocalDark
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر قارئ المصحف من النظام (iosApp/iosApp/NativeMushaf.swift): Swift يسجّل عند التشغيل ما ينشئ متحكم القارئ
 * ([attach])، فيعرض SurahScreen صفحات مصحف المدينة به بدل HorizontalPager (التقليب بـ UICollectionView والرسم بـ Core Text
 * كما في تطبيقات النظام). Kotlin يرسل حالة القارئ JSON ([NativeMushafSession.state]) مع الإشعار [STATE]،
 * و Swift يخبره باستقرار التقليب واللمس، ويقرأ منه بيانات الصفحة (خطها، ورأسها، ونصها لقارئ الشاشة).
 */
object NativeMushaf {
    /** الإشعار مع كل تغيّر في حالة قارئ (object: جلسته) */
    const val STATE = "MizanMushafState"

    private var factory: ((NativeMushafSession) -> UIViewController)? = null

    /** يستدعيه Swift مرة عند التشغيل (قبل MainViewController) */
    fun attach(make: (NativeMushafSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeMushafSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeMushafAvailable: Boolean get() = NativeMushaf.available

/** قارئ واحد معروض: حالته، وما يصل منه إلى الشاشة */
class NativeMushafSession internal constructor() {
    /** آخر حالة أُرسلت (JSON)، يقرؤها القارئ حين يُنشأ ومع كل إشعار */
    var state: String = ""
        private set

    internal var turns: NativeTurns? = null
    internal var text: Array<String>? = null
    internal var onTap: (Int) -> Unit = {}
    internal var onLongPress: (Int) -> Unit = {}
    internal var onFrameTap: () -> Unit = {}
    internal var onEdge: (Int) -> Unit = {}
    /** صفحات تعذّر تنزيل خطها (بلا اتصال): يعرض القارئ فيها زر إعادة المحاولة */
    internal val failed = mutableStateListOf<Int>()
    private val scope = MainScope()

    /** الحالة الأولى قبل أن يُنشأ القارئ (بلا إشعار)، فيفتح على صفحة القارئ مباشرة */
    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeMushaf.STATE, this)
    }

    // ---------- من Swift ----------

    /** استقر التقليب على الموضع [index] (رقم الصفحة ناقص واحد، أو رقم الكتاب المفتوح في عرض الصفحتين) */
    fun settled(index: Int) { turns?.settled(index) }

    /** بدأ الإصبع يقلّب */
    fun scrolling() { turns?.scrolling = true }

    fun tap(ayah: Int) = onTap(ayah)
    fun longPress(ayah: Int) = onLongPress(ayah)
    fun frameTap() = onFrameTap()
    /** لمس طرف الصفحة في وضع التركيز: 1 للتالية (الطرف الأيسر)، -1 للسابقة */
    fun edge(dir: Int) = onEdge(dir)

    // ---------- بيانات الصفحة ----------

    fun fontPath(page: Int): String = Madinah.fontPath(page)
    fun hasFont(page: Int): Boolean = Madinah.hasFont(page)

    /** ينزّل خط الصفحة إن لم يكن في الجهاز؛ وصوله يزيد «fonts» في الحالة، وتعذّره يضيفها إلى «failed» */
    fun requestFont(page: Int) {
        if (page !in 1..Madinah.PAGES || Madinah.hasFont(page)) return
        failed.remove(page)
        scope.launch { if (!Madinah.ensureFont(page) && page !in failed) failed.add(page) }
    }

    /** خطوط الصفحات حول [page] في الخلفية، فتكون جاهزة قبل أن تصل إليها */
    fun prefetch(page: Int) = Madinah.prefetch(page + 1, page - 1, page + 2, page - 2, page + 3)

    /** رأس الصفحة: السورة التي تبدأ بها */
    fun title(page: Int): String = "سُورَةُ ${QuranMeta.NAMES_AR[QuranMeta.locate(QuranMeta.pageRange(page).first).first - 1]}"

    /** الجزء والحزب في طرف الرأس */
    fun subtitle(page: Int): String {
        val first = QuranMeta.pageRange(page).first
        return "الجزء ${JUZ_ORDINALS[QuranMeta.juzOf(first) - 1]} · الحزب ${arabicDigits(QuranMeta.hizbOf(first))}"
    }

    fun digits(page: Int): String = arabicDigits(page)

    /** نص الصفحة لقارئ الشاشة (VoiceOver): الآيات بأرقامها */
    fun pageText(page: Int): String {
        val t = text ?: return ""
        return QuranMeta.pageRange(page).joinToString(" ") { QuranData.withoutBismillah(t, it) + " (" + QuranMeta.locate(it).second + ")" }
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeMushafPager(
    turns: NativeTurns, text: Array<String>, selected: Int, bottomInset: Dp, frame: Dp, focus: Boolean,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onFrameTap: () -> Unit, onEdge: (Int) -> Unit,
    hifz: Boolean, revealed: Set<Int>, modifier: Modifier,
) {
    val session = remember { NativeMushafSession() }
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val frameTap by rememberUpdatedState(onFrameTap)
    val edge by rememberUpdatedState(onEdge)
    session.turns = turns
    session.text = text
    DisposableEffect(session) {
        session.onTap = { tap(it) }
        session.onLongPress = { long(it) }
        session.onFrameTap = { frameTap() }
        session.onEdge = { edge(it) }
        onDispose {
            session.onTap = {}; session.onLongPress = {}; session.onFrameTap = {}; session.onEdge = {}
        }
    }

    val p = mushafPalette()
    val accent = MaterialTheme.colorScheme.primary
    val backdrop = p.backdrop.takeOrElse { MaterialTheme.colorScheme.surfaceContainerLow }
    val s = LocalStrings.current
    val st = ScriptText.of(com.osatks.mizan.i18n.Lang.current.code)
    val readCount = QuranProgress.readCount
    val read = remember(readCount) { buildString(Madinah.PAGES) { for (pg in 1..Madinah.PAGES) append(if (QuranProgress.isRead(pg)) '1' else '0') } }
    val marked = QuranPrefs.bookmarks.map { QuranMeta.pageOf(it) }.distinct().sorted()
    val jump = turns.jump
    // أول حالة لقارئ جديد (عند الرجوع من عرض آية آية مثلاً) تفتحه على الموضع الذي استقر عليه التقليب [NativeTurns.index]،
    // لا على آخر انتقال طلبه التطبيق؛ وبعدها الانتقال المطلوب كما هو ([JumpRequest.seq] نفسه فلا يقلّب القارئ)
    val jumpTo = if (session.state.isEmpty()) turns.index else jump.index
    val json = Json.write(
        linkedMapOf<String, Any?>(
            "jump" to jumpTo, "animate" to jump.animate, "seq" to jump.seq,
            // الكتاب المفتوح: كل موضع صفحتان متقابلتان (اليمنى 2i+1 واليسرى 2i+2)، وعدد المواضع Spreads.count
            "spread" to turns.spread, "count" to turns.count, "pages" to Madinah.PAGES,
            "selected" to selected,
            "bottom" to bottomInset.value.toDouble(), "frame" to frame.value.toDouble(),
            "focus" to focus, "dark" to LocalDark.current,
            "hifz" to hifz, "revealed" to revealed.sorted(),
            "fonts" to Madinah.fontsVersion, "failed" to session.failed.toList(),
            "marked" to marked, "read" to read,
            "paper" to hex(p.paper), "paperEdge" to hex(p.paperEdge), "ink" to hex(p.ink), "muted" to hex(p.muted),
            "gold" to hex(p.gold), "goldDeep" to hex(p.goldDeep), "fill" to hex(p.fill), "fillDeep" to hex(p.fillDeep),
            "select" to hex(p.select), "press" to hex(p.press), "accent" to hex(accent), "backdrop" to hex(backdrop),
            "downloading" to st.downloading, "retry" to s.retry,
        )
    )
    session.prime(json)
    SideEffect { session.publish(json) }
    UIKitViewController(
        factory = { NativeMushaf.make(session) },
        modifier = modifier,
        // اللمس يذهب إلى القارئ مباشرة (لا تأخير ليحكم Compose أولاً): التقليب يبدأ مع أول حركة للإصبع كما في النظام
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true,
        ),
    )
}
