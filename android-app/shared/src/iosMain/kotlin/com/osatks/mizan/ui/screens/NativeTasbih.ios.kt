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
import com.osatks.mizan.core.Tasbih
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.LiveTasbih
import com.osatks.mizan.platform.LiveTasbihText
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.SplitMax
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import com.osatks.mizan.widget.LiveText
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر المسبحة من النظام (iosApp/iosApp/NativeTasbih.swift)، على مثال [NativeHome]: الحال من [Tasbih] JSON مع الإشعار [STATE].
 * العدّ يظهر في Swift فور اللمس، ويصل كل لمس إلى [NativeTasbihSession.count] فتبقى [Tasbih] مصدر الحال (ومنها الأداة
 * والنشاط الحي وزر الإجراء)؛ و"taps" في الحال عدد اللمسات التي وصلت، فيعرف Swift متى لحقت الحال بما عدّه.
 */
object NativeTasbih {
    const val STATE = "MizanTasbihScreenState"

    private var factory: ((NativeTasbihSession) -> UIViewController)? = null

    fun attach(make: (NativeTasbihSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeTasbihSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeTasbihAvailable: Boolean get() = NativeTasbih.available

class NativeTasbihSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}

    /** اللمسات التي وصلت إلى [Tasbih] من هذه الشاشة (تُقرأ في التركيب فتُعاد الحال مع كل لمسة) */
    internal var taps by mutableIntStateOf(0)

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeTasbih.STATE, this)
    }

    fun back() = onBack()

    /** تسبيحة واحدة (عدّها Swift قبلها على الشاشة) */
    fun count() {
        Tasbih.count()
        taps++
    }

    fun reset() = Tasbih.reset()
    fun undo() = Tasbih.undo()
    fun mode(key: String) = Tasbih.setMode(key)
    fun dhikr(index: Int) = Tasbih.setDhikr(index)
    fun target(value: Int) = Tasbih.setTarget(value)

    /** يُظهر المسبحة على شاشة القفل والجزيرة الآن */
    fun showLive() {
        LiveTasbih.show?.invoke()
    }

    /** مفتاح «على شاشة القفل» */
    fun live(on: Boolean) = LiveTasbih.update(on)
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeTasbihView(onBack: (() -> Unit)?, modifier: Modifier) {
    val session = remember { NativeTasbihSession() }
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onBack = { backS?.invoke() }
        onDispose { session.onBack = {} }
    }
    remember { Tasbih.ensure() }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // عمودان على الشاشة العريضة كما في ToolPage (الحبة في الأول، والاختيار والإحصاء في الثاني)
        val wide = maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val st = Tasbih.state
        val goal = st.goal
        val taps = session.taps
        // ما يُكتب حين يبلغ العدد هدف الذكر الحالي (كرسالة شاشة Compose)
        val doneMessage = when {
            goal <= 0 -> ""
            st.mode == "salah" -> if (st.step < Tasbih.SALAH.size - 1) s.nextDhikr else s.completedHundred
            else -> s.completedN(goal)
        }
        val live = if (LiveTasbih.supported) {
            val lt = LiveText.of(lang.code)
            val (title, desc) = LiveTasbihText.of(lang.code)
            linkedMapOf<String, Any?>("on" to LiveTasbih.onLockScreen, "show" to lt.showLive, "title" to title, "desc" to desc)
        } else null
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "wide" to wide,
                "canBack" to (onBack != null), "back" to s.back,
                "title" to s.tasbihTitle, "subtitle" to s.tasbihSubtitle,
                "taps" to taps,
                "mode" to st.mode,
                "modes" to listOf(linkedMapOf("key" to "free", "label" to s.modeFree), linkedMapOf("key" to "salah", "label" to s.modeAfterPrayer)),
                "text" to st.text, "meaning" to st.meaning(s),
                "count" to st.count, "goal" to goal,
                "goalLabel" to if (goal > 0) s.ofGoal(goal) else s.noLimit,
                "doneMessage" to doneMessage,
                "tapToCount" to s.tapToCount, "reset" to s.reset, "undo" to s.undo,
                "live" to live,
                "chooser" to linkedMapOf(
                    "title" to s.chooseDhikr, "labels" to s.dhikrLabels, "idx" to st.idx,
                    "targetLabel" to s.targetCount, "target" to st.target,
                    "targets" to Tasbih.TARGETS.map { t -> linkedMapOf<String, Any?>("value" to t, "label" to if (t == 0) s.noLimit else "$t") },
                ),
                "stats" to linkedMapOf(
                    "title" to s.yourStats,
                    "tiles" to listOf(
                        linkedMapOf("value" to Fmt.int(st.rounds.toLong()), "label" to s.roundsDone),
                        linkedMapOf("value" to Fmt.int(st.today.toLong()), "label" to s.todayCount),
                        linkedMapOf("value" to Fmt.int(st.total.toLong()), "label" to s.totalCount),
                    ),
                ),
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to nativeProofs(Evidence.tasbih, lang, quran, trText),
                ),
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                // لون الحبة (0xFF1A8A6F و 0xFF0B4A3D بلون التطبيق، كما في TasbihScreen)
                "bead1" to hex(Color(0xFF1A8A6F).recolor(accent)), "bead2" to hex(Color(0xFF0B4A3D).recolor(accent)),
                "secondary" to hex(cs.secondary),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeTasbih.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
