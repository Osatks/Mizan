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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Proof
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.Sources
import com.osatks.mizan.quran.arabicDigits
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
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر زكاة الفطر والفدية من النظام (iosApp/iosApp/NativeFitrFidya.swift)، على مثال [NativePrayer]: محتوى الشاشة كله
 * (النصوص والنتائج والصفوف الظاهرة) يبنيه FitrFidyaScreens.kt، وهنا يُضاف إليه المظهر والهوامش والأدلة، ويُرسل JSON
 * مع الإشعار [STATE]. وSwift يخبره بالمدخلات: عدّاد (step)، واختيار أو نص حقل (change)، والرجوع.
 */
object NativeFitrFidya {
    const val STATE = "MizanFitrFidyaState"

    private var factory: ((NativeFitrFidyaSession) -> UIViewController)? = null

    fun attach(make: (NativeFitrFidyaSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeFitrFidyaSession): UIViewController = factory!!.invoke(session)
}

class NativeFitrFidyaSession internal constructor() {
    var state: String = ""
        private set

    internal var onStep: (String, Int) -> Unit = { _, _ -> }
    internal var onChange: (String, String) -> Unit = { _, _ -> }
    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeFitrFidya.STATE, this)
    }

    /** عدّاد [key] (الأشخاص، الأيام، الأيمان) صار [value] */
    fun step(key: String, value: Int) = onStep(key, value)

    /** اختيار [key] (النمط، نوع الطعام، المقدار، التبويب) أو نص حقله صار [value] */
    fun change(key: String, value: String) = onChange(key, value)

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * زكاة الفطر ([kind] = "fitr") أو الفدية والكفارات ("fidya") بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeFitrFidya.swift)
 * بطراز الرئيسية: بطاقة النتيجة، والمدخلات (عدّاد، واختيار، وقائمة، وحقول أرقام)، والملاحظات، والأدلة — كما في [FitrScreen] و[FidyaScreen].
 * [content] يبنيه Kotlin كله (النصوص والنتائج منسّقة بأرقام التطبيق، والصفوف الظاهرة)، ويُرسل إلى Swift كما هو.
 * Swift يردّ باللمس: [onStep] لعدّاد (مفتاحه وقيمته)، و[onChange] لاختيار أو نص حقل، و[onBack] للرجوع.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeFitrFidyaView(
    kind: String, content: Map<String, Any?>, evidenceKey: String, evidence: List<Proof>,
    onStep: (String, Int) -> Unit, onChange: (String, String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeFitrFidyaSession() }
    val stepS by rememberUpdatedState(onStep)
    val changeS by rememberUpdatedState(onChange)
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onStep = { k, v -> stepS(k, v) }
        session.onChange = { k, v -> changeS(k, v) }
        session.onBack = { backS() }
        onDispose { session.onStep = { _, _ -> }; session.onChange = { _, _ -> }; session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    // نص الآيات من المصحف وترجمة معانيها بلغة الواجهة (كما في EvidenceCard)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // عمودان على الأوسع (المدخلات والأدلة بجانب النتيجة والملاحظات) كما في ToolPage
        val wide = maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val points = evidence.mapIndexed { i, p ->
            linkedMapOf<String, Any?>(
                "id" to "$evidenceKey-$i", "kind" to p.kind.name.lowercase(), "concept" to p.concept,
                "title" to p.title[lang], "body" to p.body[lang], "source" to p.source[lang],
                "ayah" to p.ayah?.let { (sn, from, to) ->
                    quran?.let { q -> "﴿" + (from..to).joinToString(" ") { a -> QuranData.withoutBismillah(q, QuranMeta.index(sn, a)) + " (" + arabicDigits(a) + ")" } + "﴾" }
                },
                "tr" to p.ayah?.let { (sn, from, to) -> trText?.let { tr -> (from..to).joinToString(" ") { a -> tr[QuranMeta.index(sn, a)] } } },
                "hadith" to p.hadith,
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "kind" to kind, "rtl" to lang.rtl, "dark" to dark, "wide" to wide,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "back" to s.back, "hideKeyboard" to s.close,
            ) + content + linkedMapOf<String, Any?>(
                "evidence" to linkedMapOf(
                    "key" to evidenceKey, "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to points,
                ),
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeFitrFidya.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
