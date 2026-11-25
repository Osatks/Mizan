package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.Kids
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.KidsRecite
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.quran.arabicDigits
import com.osatks.mizan.ui.BackHandler
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIViewController

/**
 * جسر ركن الأطفال من النظام (iosApp/iosApp/NativeKids.swift)، على مثال [NativeTasbih]: الحال كلها JSON مع الإشعار
 * [STATE] (السور ونجومها، والأسماء ومعانيها، والأدعية، وصفحة السورة المفتوحة وحال «ردّد معي»)، واللمسات تعود إلى
 * [NativeKidsSession] فتصل إلى [Kids] و[KidsRecite]: Kotlin مصدر الحال والتلاوة، وSwift يرسم ويحتفل.
 */
object NativeKids {
    const val STATE = "MizanKidsState"

    private var factory: ((NativeKidsSession) -> UIViewController)? = null

    fun attach(make: (NativeKidsSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeKidsSession): UIViewController = factory!!.invoke(session)
}

class NativeKidsSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}

    /** السورة المفتوحة في ركن الأطفال (0: الصفحة الأولى) */
    internal var surah by mutableIntStateOf(0)

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeKids.STATE, this)
    }

    /** زر الرجوع: من السورة إلى ركن الأطفال، ومنه إلى الأدوات */
    fun back() {
        if (surah != 0) close() else onBack()
    }

    /** يفتح سورة لـ «ردّد معي» */
    fun open(surah: Int) {
        if (surah !in Kids.SURAHS) return
        KidsRecite.open(surah)
        this.surah = surah
    }

    /** يعود من السورة إلى ركن الأطفال (وتتوقف التلاوة) */
    fun close() {
        KidsRecite.stop()
        surah = 0
    }

    /** تشغيل أو إيقاف مؤقت لـ «ردّد معي» في السورة المفتوحة */
    fun toggle() {
        if (surah == 0) return
        KidsRecite.open(surah)
        KidsRecite.toggle()
    }

    fun stop() = KidsRecite.stop()

    /** يبدأ من الخطوة [step] (البسملة 0 إن كانت، ثم الآيات) */
    fun jump(step: Int) {
        if (surah == 0) return
        KidsRecite.open(surah)
        KidsRecite.start(step)
    }

    /** نجمة السورة: يضعها أحد الوالدين أو يرفعها؛ true إن صارت محفوظة الآن */
    fun star(surah: Int): Boolean = Kids.toggleStar(surah)

    /** تكرار كل آية (من 1 إلى 3) */
    fun setTimes(n: Int) = Kids.updateRepeat(n)

    fun setReciter(id: String) = Kids.updateReciter(id)
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * ركن الأطفال بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeKids.swift) بطراز الرئيسية والمسبحة: السور القصيرة،
 * وأسماء الله الحسنى بطاقاتٍ تُسحب، وأدعية اليوم، ولوحة النجوم؛ وصفحة السورة آية آية مع «ردّد معي» — كما في [KidsScreen].
 * الرجوع من السورة بالزر أو بسحب الحافة، والتلاوة تتوقف مؤقتاً حين تختفي الصفحة أو يغادر المستخدم التطبيق،
 * وتنتهي بالخروج منها.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeKidsView(onBack: (() -> Unit)?, modifier: Modifier) {
    val session = remember { NativeKidsSession() }
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onBack = { backS?.invoke() }
        val token = NSNotificationCenter.defaultCenter.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            KidsRecite.pause()
        }
        onDispose {
            session.onBack = {}
            NSNotificationCenter.defaultCenter.removeObserver(token)
            KidsRecite.release()
        }
    }
    remember { Kids.ensure() }
    BackHandler(enabled = session.surah != 0) { session.close() }
    val pageActive = LocalPageActive.current
    LaunchedEffect(pageActive) { if (!pageActive) KidsRecite.pause() }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) {
        if (value == null) value = runCatching { QuranData.uthmani() }.getOrNull()
    }
    val meaningId = remember(lang) { kidsMeaningSource(lang) }
    val meanings by produceState(meaningId?.let { QuranData.cached("tafsir/$it.txt") }, meaningId, com.osatks.mizan.content.ContentStore.version) {
        value = meaningId?.let { QuranData.sourceOrNull(it) }
    }
    val duas by produceState<List<KidsDua>?>(null, lang) {
        value = withContext(Dispatchers.IO) { runCatching { kidsDuas(lang) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        val g = gutters(maxWidth, TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val tool = s.tools.getValue("kids")
        val total = Kids.total
        val all = Kids.SURAHS.size
        val text = quran
        val open = session.surah
        // صفحة السورة: آياتها وحال «ردّد معي» (والأسماء والأدعية لا تُرسل معها، فتبقى الحال صغيرة مع كل آية)
        val reader = if (open == 0) null else {
            val count = QuranMeta.COUNTS[open - 1]
            val basmala = QuranData.hasBismillahHeader(open)
            val mine = KidsRecite.surah == open
            val active = mine && KidsRecite.active
            val stepClip = if (mine) KidsRecite.steps.getOrNull(KidsRecite.step) else null
            linkedMapOf<String, Any?>(
                "n" to open,
                "title" to s.surahName(open),
                "sub" to "${QuranMeta.NAMES_AR[open - 1]} · ${s.ayahCount(count)}",
                "basmala" to if (basmala) (text?.getOrNull(0) ?: "") else null,
                "ayahs" to (1..count).map { a ->
                    linkedMapOf<String, Any?>(
                        "step" to (a - 1 + if (basmala) 1 else 0), "n" to a,
                        "num" to arabicDigits(a),
                        "text" to (text?.let { QuranData.withoutBismillah(it, QuranMeta.index(open, a)) } ?: ""),
                    )
                },
                "star" to Kids.isMemorized(open),
                "phase" to when (if (mine) KidsRecite.phase else KidsRecite.Phase.IDLE) {
                    KidsRecite.Phase.IDLE -> "idle"
                    KidsRecite.Phase.LISTEN -> "listen"
                    KidsRecite.Phase.REPEAT -> "repeat"
                    KidsRecite.Phase.DONE -> "done"
                },
                "active" to active,
                "running" to (mine && KidsRecite.running),
                "buffering" to (mine && KidsRecite.buffering),
                "step" to if (active) KidsRecite.step else -1,
                "rep" to if (active && KidsRecite.times > 1 && stepClip?.basmala != true)
                    "${Fmt.int(KidsRecite.rep.toLong())} / ${Fmt.int(KidsRecite.times.toLong())}" else null,
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark, "ar" to (lang == Lang.AR),
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "canBack" to (onBack != null), "back" to s.back,
                "title" to tool.name, "subtitle" to tool.desc,
                "l" to linkedMapOf(
                    "surahs" to s.kidsSurahs, "surahsSub" to s.kidsSurahsSub,
                    "names" to s.kidsNames, "namesSub" to s.kidsNamesSub,
                    "duas" to s.kidsDuas, "duasSub" to s.kidsDuasSub,
                    "stars" to s.kidsStars, "starsSub" to s.kidsStarsSub,
                    "mark" to s.kidsMarkMemorized, "memorized" to s.kidsMemorized,
                    "repeatAfterMe" to s.kidsRepeatAfterMe, "listen" to s.kidsListen, "yourTurn" to s.kidsYourTurn,
                    "wellDone" to s.kidsWellDone, "tapAyah" to s.kidsTapAyah,
                    "prevName" to s.kidsPrevName, "nextName" to s.kidsNextName,
                    "play" to s.playRecitation, "pause" to s.pauseRecitation, "stop" to s.stopRecitation,
                    "repeatEach" to s.repeatAyah, "reciter" to s.reciter, "audioError" to s.audioError,
                    "meaning" to kidsMeaningLabel(lang, s),
                ),
                "starCount" to s.kidsStarCount(total),
                "progress" to if (all > 0) total.toDouble() / all else 0.0,
                "progressText" to "${Fmt.int(total.toLong())} / ${Fmt.int(all.toLong())}",
                "surahs" to Kids.SURAHS.map { n ->
                    linkedMapOf<String, Any?>(
                        "n" to n, "name" to s.surahName(n), "ar" to QuranMeta.NAMES_AR[n - 1],
                        "ayahs" to s.ayahCount(QuranMeta.COUNTS[n - 1]), "star" to Kids.isMemorized(n),
                    )
                },
                "names" to if (open != 0) emptyList<Any?>() else Kids.NAMES.mapIndexed { i, n ->
                    linkedMapOf<String, Any?>(
                        "i" to i, "ar" to n.ar, "latin" to n.latin,
                        "ayah" to kidsNameAyah(n, text), "ref" to kidsNameRef(n, s),
                        "meaning" to meanings?.getOrNull(QuranMeta.index(n.surah, n.ayah))?.takeIf { it.isNotBlank() },
                    )
                },
                "namesCount" to Kids.NAMES.size,
                "duas" to if (open != 0) emptyList<Any?>() else duas.orEmpty().map { d ->
                    linkedMapOf<String, Any?>("id" to d.id, "title" to d.title, "ar" to d.ar, "tr" to d.tr)
                },
                "times" to Kids.repeat,
                "timesOptions" to Kids.REPEATS.map { t -> linkedMapOf<String, Any?>("value" to t, "label" to s.dhikrTimes(t)) },
                "reciter" to Kids.reciter,
                "reciters" to Reciters.ALL.map { r -> linkedMapOf<String, Any?>("id" to r.id, "name" to r.name(lang)) },
                "celebrations" to Kids.celebrations,
                "errors" to KidsRecite.errors,
                "reader" to reader,
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                "error" to hex(cs.error),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeKids.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
