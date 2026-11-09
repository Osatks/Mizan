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
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.core.InheritanceResult
import com.osatks.mizan.i18n.Evidence
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
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
 * جسر حاسبة المواريث من النظام (iosApp/iosApp/NativeInheritance.swift)، على مثال [NativeTools]: Swift يسجّل ما ينشئ متحكمها،
 * وKotlin يرسل الحالة JSON مع الإشعار [STATE] عند كل تغيّر: القسمة محسوبة ومنسّقة (الكسور والنسب والمبالغ والحجب والملاحظات والأدلة).
 * وSwift يخبره بكل لمسة: جنس الميت، وعدد كل وارث، ومبالغ التركة، ومسألة جديدة، والمشاركة، والرجوع.
 */
object NativeInheritance {
    const val STATE = "MizanInheritanceState"

    private var factory: ((NativeInheritanceSession) -> UIViewController)? = null

    fun attach(make: (NativeInheritanceSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeInheritanceSession): UIViewController = factory!!.invoke(session)
}

class NativeInheritanceSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}
    internal var onGender: (Char) -> Unit = {}
    internal var onCount: (String, Int) -> Unit = { _, _ -> }
    internal var onField: (String, String) -> Unit = { _, _ -> }
    internal var onReset: () -> Unit = {}
    internal var onShare: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeInheritance.STATE, this)
    }

    fun back() = onBack()

    /** جنس الميت: "m" أو "f" */
    fun gender(key: String) = onGender(if (key == "f") 'f' else 'm')

    /** عدد الوارث [key] (0 يرفعه من المسألة)، في حدود ما يجوز له */
    fun heir(key: String, count: Int) {
        val h = Inheritance.HEIRS.firstOrNull { it.key == key } ?: return
        onCount(key, count.coerceIn(0, h.max))
    }

    /** نص حقل من حقول التركة كما كتبه المستخدم: estate أو funeral أو debts أو will */
    fun field(key: String, text: String) = onField(key, text)

    fun reset() = onReset()
    fun share() = onShare()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * حاسبة المواريث بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeInheritance.swift) بطراز الرئيسية: أصل المسألة وعَولها أو ردّها،
 * وأنصبة الورثة وكسورها ومبالغها والمحجوبين وأدلتهم، وملاحظات المسألة، والميت والتركة، والورثة بعدّاد أو مفتاح لكل وارث،
 * والأدلة والتنبيه — كما في [InheritanceScreen]. المدخلات تبقى في InheritanceScreen، والقسمة ([result]) تُحسب هناك وتُنسّق هنا.
 * [onField] مفتاحه: estate أو funeral أو debts أو will.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeInheritanceView(
    gender: Char, counts: Map<String, Int>, active: Map<String, Int>,
    estate: String, funeral: String, debts: String, will: String,
    result: InheritanceResult?, net: Double, willCapped: Boolean,
    onGender: (Char) -> Unit, onCount: (String, Int) -> Unit, onField: (String, String) -> Unit,
    onReset: () -> Unit, onShare: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeInheritanceSession() }
    val backS by rememberUpdatedState(onBack)
    val genderS by rememberUpdatedState(onGender)
    val countS by rememberUpdatedState(onCount)
    val fieldS by rememberUpdatedState(onField)
    val resetS by rememberUpdatedState(onReset)
    val shareS by rememberUpdatedState(onShare)
    // يزيد مع كل «مسألة جديدة» فتُفرَّغ حقول Swift (وهي تحفظ ما يُكتب فيها حتى لا يقفز المؤشر)
    var resetTick by remember { mutableIntStateOf(0) }
    DisposableEffect(session) {
        session.onBack = { backS() }
        session.onGender = { genderS(it) }
        session.onCount = { k, v -> countS(k, v) }
        session.onField = { k, v -> fieldS(k, v) }
        session.onReset = { resetTick++; resetS() }
        session.onShare = { shareS() }
        onDispose {
            session.onBack = {}; session.onGender = {}; session.onCount = { _, _ -> }; session.onField = { _, _ -> }
            session.onReset = {}; session.onShare = {}
        }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    // نص آيات الأدلة وترجمة معانيها بلغة الواجهة (كما في EvidenceCard)
    val quran by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // على الشاشة الواسعة: المدخلات في عمود والقسمة بجانبها (كـ side في ToolPage)
        val wide = maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val arrow = if (lang.rtl) "←" else "→"

        val hero = if (result != null) linkedMapOf<String, Any?>(
            "label" to s.aslLabel,
            "value" to if (result.awl != null) "${result.asl} $arrow ${result.awl}" else "${result.asl}",
            "badge" to when { result.awl != null -> s.awlCase; result.radd -> s.raddCase; else -> s.adilCase },
            "badgeColor" to hex(if (result.awl == null) Color(0xFF3FB595) else Color(0xFFEF8A76)),
            "tashih" to if (result.tashih != result.asl) s.tashihFrom(result.tashih) else null,
        ) else linkedMapOf<String, Any?>(
            "label" to s.aslLabel, "value" to "—", "badge" to s.addHeirs, "badgeColor" to hex(Gold), "tashih" to null,
        )

        var ci = 0
        val shares = result?.let { r ->
            Inheritance.HEIRS.mapNotNull { h ->
                val c = active[h.key] ?: return@mapNotNull null
                val sh = r.res[h.key]
                val share = sh?.share
                val name = (s.heirs[h.key] ?: h.key) + if (c > 1) " ×$c" else ""
                if (sh == null || sh.blocked != null || share == null) {
                    linkedMapOf<String, Any?>(
                        "key" to h.key, "name" to name,
                        "blocked" to s.blockedBy(sh?.blocked?.let { s.nameOf(it) } ?: "—"),
                    )
                } else {
                    val amount = net * share.value
                    linkedMapOf<String, Any?>(
                        "key" to h.key, "name" to name, "blocked" to null,
                        "share" to "$share  ·  ${Fmt.num(share.value * 100, 1)}%",
                        "value" to share.value.coerceIn(0.0, 1.0),
                        "detail" to s.shareType(sh.type, sh.radd) +
                            if (net > 0) "  ·  ${Fmt.num(amount)}" + (if (c > 1) " (${s.perPerson(Fmt.num(amount / c))})" else "") else "",
                        "source" to Evidence.heirSource(h.key)[lang],
                        "color" to hex(INH_BAR_COLORS[ci++ % INH_BAR_COLORS.size]),
                    )
                }
            }
        }

        val heirsShown = Inheritance.HEIRS.filter { it.only == null || it.only == gender }
        val groups = heirsShown.groupBy { it.group }.map { (group, heirs) ->
            linkedMapOf<String, Any?>(
                "key" to group, "title" to (s.heirGroups[group] ?: group),
                "heirs" to heirs.map { h ->
                    val n = counts[h.key] ?: 0
                    linkedMapOf<String, Any?>(
                        "key" to h.key, "name" to (s.heirs[h.key] ?: h.key), "count" to n, "countText" to Fmt.int(n.toLong()), "max" to h.max,
                    )
                },
            )
        }

        val points = Evidence.inheritance
        val proofs = points.map { p ->
            linkedMapOf<String, Any?>(
                "kind" to p.kind.name.lowercase(), "concept" to p.concept,
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
                "rtl" to lang.rtl, "dark" to dark, "wide" to wide,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "title" to s.inhTitle, "subtitle" to s.inhSubtitle, "back" to s.back, "close" to s.close,
                "hero" to hero,
                "sharesTitle" to s.sharesTitle, "shareLabel" to s.shareDivision,
                "shares" to shares,
                "notes" to (result?.notes?.map { s.inhNote(it) } ?: emptyList<String>()),
                "estate" to linkedMapOf(
                    "title" to s.deceasedAndEstate, "male" to s.deceasedMale, "female" to s.deceasedFemale,
                    "gender" to gender.toString(),
                    "fields" to listOf(
                        linkedMapOf("key" to "estate", "label" to s.estateTotal, "value" to estate, "hint" to null),
                        linkedMapOf("key" to "funeral", "label" to s.funeralCosts, "value" to funeral, "hint" to null),
                        linkedMapOf("key" to "debts", "label" to s.deceasedDebts, "value" to debts, "hint" to null),
                        linkedMapOf("key" to "will", "label" to s.bequest, "value" to will, "hint" to s.bequestHint),
                    ),
                    "netLabel" to s.netEstate,
                    "net" to if (Fmt.parse(estate) > 0) Fmt.num(net) + if (willCapped) " " + s.bequestCapped else "" else null,
                ),
                "groups" to groups,
                "newCase" to s.newCase, "resetTick" to resetTick,
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub, "open" to (result != null),
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "kindOrder" to points.map { it.kind.name.lowercase() }.distinct(),
                    "points" to proofs,
                ),
                "disclaimer" to s.inhDisclaimer,
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                // ألوان الملاحظات والحجب والإطارات كما في Note وصف المحجوب
                "x" to linkedMapOf(
                    "secondary" to hex(cs.secondary), "secondaryContainer" to hex(cs.secondaryContainer),
                    "onSecondaryContainer" to hex(cs.onSecondaryContainer), "error" to hex(cs.error),
                    "surfaceVariant" to hex(cs.surfaceVariant), "outline" to hex(cs.outline),
                    "tertiary" to hex(cs.tertiary),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeInheritance.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
