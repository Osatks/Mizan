package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
 * جسر أدوات التاريخ من النظام (iosApp/iosApp/NativeDates.swift)، على مثال [NativeTools]: Swift يسجّل ما ينشئ متحكمها،
 * وKotlin يرسل الحالة JSON مع الإشعار [STATE] (والحقل kind يختار الشاشة: hijri أو age أو events)، والحساب كله هنا.
 * وSwift يخبره باللمس: اليوم والشهر والسنة، واتجاه التحويل، واليوم الحالي، وتقليب التقويم ولمس يومه، والتقويم، والمناسبة.
 */
object NativeDates {
    const val STATE = "MizanDatesState"

    private var factory: ((NativeDatesSession) -> UIViewController)? = null

    fun attach(make: (NativeDatesSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeDatesSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeDatesAvailable: Boolean get() = NativeDates.available

class NativeDatesSession internal constructor() {
    var state: String = ""
        private set

    internal var onAction: (String, String) -> Unit = { _, _ -> }
    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeDates.STATE, this)
    }

    fun back() = onBack()

    fun day(value: Int) = onAction("day", value.toString())
    fun month(value: Int) = onAction("month", value.toString())

    /** نص السنة كما كُتب، والأرقام العربية والفارسية والبنغالية تصير لاتينية كما يقرؤها الحساب */
    fun year(text: String) = onAction("year", latinDigits(text))

    /** اتجاه التحويل: g2h أو h2g */
    fun dir(key: String) = onAction("dir", key)
    fun today() = onAction("today", "")

    /** تقليب التقويم الهجري شهراً ([delta] ±1) */
    fun calendar(delta: Int) = onAction("calendar", delta.toString())

    /** لمس يوم [day] في التقويم */
    fun pick(day: Int) = onAction("pick", day.toString())

    /** تقويم تاريخ الميلاد (العمر): g أو h */
    fun mode(key: String) = onAction("mode", key)

    /** المناسبة [index] من القائمة */
    fun select(index: Int) = onAction("select", index.toString())
}

private const val OTHER_DIGITS = "٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹০১২৩৪৫৬৭৮৯"

private fun latinDigits(text: String): String = buildString {
    for (ch in text) {
        val i = OTHER_DIGITS.indexOf(ch)
        append(if (i >= 0) '0' + i % 10 else ch)
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeDatesView(
    kind: String, content: Map<String, Any?>, evidence: List<Proof>,
    onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) {
    val session = remember { NativeDatesSession() }
    val actionS by rememberUpdatedState(onAction)
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onAction = { a, v -> actionS(a, v) }
        session.onBack = { backS() }
        onDispose { session.onAction = { _, _ -> }; session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    // نص الآيات من المصحف وترجمة معانيها بلغة الواجهة للأدلة (كما في EvidenceCard)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        val g = gutters(maxWidth, TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val safeStart = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues()
            .calculateStartPadding(LocalLayoutDirection.current)
        val proofs = evidence.map { p ->
            linkedMapOf<String, Any?>(
                "kind" to p.kind.name.lowercase(),
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
                "kind" to kind,
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "barStart" to (lead + safeStart + 10.dp).value.toDouble(),
                "back" to s.back, "close" to s.close,
                kind to content,
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to proofs,
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
            factory = { NativeDates.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
