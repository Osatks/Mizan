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
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Fmt
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.FeatureText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.Proof
import com.osatks.mizan.i18n.QiblaCalibText
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.platform.ArQibla
import com.osatks.mizan.platform.Platform
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
 * جسر شاشة القبلة من النظام (iosApp/iosApp/NativeQibla.swift)، على مثال [NativeHome]: Kotlin يرسل اتجاه القبلة والنصوص
 * JSON مع الإشعار [STATE]، واتجاه الهاتف من البوصلة في Swift (الشمال الحقيقي) فتدور البوصلة وتتبدل جملة الاستدارة هناك
 * بلا رجوع إلى Compose؛ نص الاستدارة قالب فيه {d} مكان الدرجات. Swift يخبره بالرجوع وبفتح القبلة بالكاميرا.
 */
object NativeQibla {
    const val STATE = "MizanQiblaState"

    private var factory: ((NativeQiblaSession) -> UIViewController)? = null

    fun attach(make: (NativeQiblaSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeQiblaSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeQiblaAvailable: Boolean get() = NativeQibla.available

class NativeQiblaSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeQibla.STATE, this)
    }

    fun back() = onBack()

    /** القبلة بالكاميرا (ArQibla.swift)، كما يفتحها زر شاشة Compose */
    fun ar() {
        ArQibla.open?.invoke()
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** رقم لا يظهر في أي ترجمة: يُستبدل بـ {d} فيصير نص الاستدارة قالباً يملؤه Swift بالدرجات */
private const val DEG = 9876543

/** نقاط الدليل للواجهة: الآية بخط المصحف وترجمة معانيها، والحديث، والشرح والمصدر (كما في EvidenceCard) */
internal fun nativeProofs(points: List<Proof>, lang: Lang, quran: Array<String>?, tr: Array<String>?): List<Map<String, Any?>> =
    points.map { p ->
        linkedMapOf(
            "kind" to p.kind.name.lowercase(), "concept" to p.concept,
            "title" to p.title[lang], "body" to p.body[lang], "source" to p.source[lang],
            "ayah" to p.ayah?.let { (sn, from, to) ->
                quran?.let { q -> "﴿" + (from..to).joinToString(" ") { a -> QuranData.withoutBismillah(q, QuranMeta.index(sn, a)) + " (" + arabicDigits(a) + ")" } + "﴾" }
            },
            "tr" to p.ayah?.let { (sn, from, to) -> tr?.let { t -> (from..to).joinToString(" ") { a -> t[QuranMeta.index(sn, a)] } } },
            "hadith" to p.hadith,
        )
    }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeQiblaView(onBack: (() -> Unit)?, modifier: Modifier) {
    val session = remember { NativeQiblaSession() }
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onBack = { backS?.invoke() }
        onDispose { session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val place = AppSettings.place
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
        val qibla = PrayTimes.qibla(place.lat, place.lng)
        val ft = FeatureText.of(lang.code)
        val cal = QiblaCalibText.of(lang.code)
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "canBack" to (onBack != null), "back" to s.back,
                "title" to s.qiblaTitle, "subtitle" to s.placeName(place),
                "bearing" to qibla,
                "noCompass" to s.noCompass, "facing" to s.facingQibla,
                "turnRight" to s.turnRight(DEG).replace(DEG.toString(), "{d}"),
                "turnLeft" to s.turnLeft(DEG).replace(DEG.toString(), "{d}"),
                "angleLabel" to s.qiblaAngle, "angle" to "${Fmt.num(qibla, 1)}°",
                "towards" to s.towards(s.directions[(kotlin.math.round(qibla / 45.0).toInt()) % 8]),
                "locationLabel" to s.location, "location" to s.placeName(place),
                "distanceLabel" to s.distanceToKaaba, "distance" to Fmt.num(PrayTimes.distanceKm(place.lat, place.lng), 0) + " " + s.unitKm,
                "headingLabel" to s.phoneHeading,
                "tip" to s.qiblaTip, "lowAccuracy" to s.compassLowAccuracy,
                "ar" to if (ArQibla.supported) linkedMapOf("title" to ft.arQibla, "hint" to ft.arHint) else null,
                "calib" to linkedMapOf(
                    "title" to cal.title, "first" to cal.steps.first(), "steps" to cal.all(Platform.name), "done" to cal.done,
                ),
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to nativeProofs(Evidence.qibla, lang, quran, trText),
                ),
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                "x" to linkedMapOf(
                    "north" to hex(cs.error), "warnBg" to hex(cs.secondaryContainer), "warnFg" to hex(cs.onSecondaryContainer),
                    "warn" to hex(cs.secondary),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeQibla.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
