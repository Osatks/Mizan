package com.osatks.mizan.ui.quran

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.osatks.mizan.core.Search
import com.osatks.mizan.i18n.Ar
import com.osatks.mizan.i18n.En
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.QuranProgress
import com.osatks.mizan.quran.QuranSearch
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
import kotlinx.coroutines.delay
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر تبويب القرآن من النظام (iosApp/iosApp/NativeQuran.swift)، على مثال [com.osatks.mizan.ui.screens.NativeTools]:
 * Swift يسجّل ما ينشئ متحكمه، وKotlin يرسل الحالة JSON مع الإشعار [STATE] عند كل تغيّر (البحث يُحسب هنا ويُعاد نشره).
 * وSwift يخبره باللمس: فتح مسار، ونص البحث، والقسم (السور، الأجزاء، المحفوظات)، ومدة الختمة، وختمة جديدة.
 */
object NativeQuran {
    const val STATE = "MizanQuranState"

    private var factory: ((NativeQuranSession) -> UIViewController)? = null

    fun attach(make: (NativeQuranSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeQuranSession): UIViewController = factory!!.invoke(session)
}

class NativeQuranSession internal constructor() {
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onQuery: (String) -> Unit = {}
    internal var onTab: (String) -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeQuran.STATE, this)
    }

    fun open(route: String) = onOpen(route)

    /** نص البحث كما يكتبه القارئ في حقل النظام */
    fun query(text: String) = onQuery(text)

    /** القسم الظاهر: surahs أو juz أو saved */
    fun tab(key: String) = onTab(key)

    /** مدة الختمة بالأيام (0 بلا مدة)، كما في قائمة KhatmaCard */
    fun plan(days: Int) = QuranProgress.changePlan(days)

    fun restart() = QuranProgress.restart()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * تبويب القرآن بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeQuran.swift) بطراز الرئيسية: متابعة القراءة (ووضع التركيز)،
 * والختمة وورد اليوم ومدتها، والدليل، والبحث في السور والآيات، والسور والأجزاء والمحفوظات — كما في [QuranScreen].
 * [open] يفتح المسار نفسه الذي يفتحه تبويب Compose.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeQuranView(open: (String) -> Unit, modifier: Modifier) {
    QuranPrefs.ensure()
    QuranProgress.ensure()
    val session = remember { NativeQuranSession() }
    val openS by rememberUpdatedState(open)
    // القسم ونص البحث يبقيان بعد تبديل التبويب كما في QuranScreen
    var tab by rememberSaveable { mutableStateOf("surahs") }
    var q by rememberSaveable { mutableStateOf("") }
    DisposableEffect(session) {
        session.onOpen = { openS(it) }
        session.onQuery = { q = it }
        session.onTab = { tab = it }
        onDispose { session.onOpen = {}; session.onQuery = {}; session.onTab = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)

    val text by produceState(QuranData.cached(QuranData.UTHMANI)) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    // خط المصحف يُهيَّأ في الخلفية قبل فتح أي سورة
    LaunchedEffect(Unit) { warmMushafFont() }

    // نتائج البحث في الآيات تُحسب في الخلفية بعد توقف الكتابة قليلاً (كما في QuranScreen)
    var ayahHits by remember { mutableStateOf<List<Int>?>(emptyList()) }
    LaunchedEffect(q, lang) {
        if (q.isBlank()) { ayahHits = emptyList(); return@LaunchedEffect }
        ayahHits = null
        delay(250)
        ayahHits = QuranSearch.ayahs(q, lang)
    }
    val surahHits = remember(q, lang) {
        val terms = Search.terms(q)
        if (terms.isEmpty()) emptyList() else (1..QuranMeta.SURAHS).map { n ->
            n to Search.score(terms, s.surahName(n), "$n ${QuranMeta.NAMES_AR[n - 1]} ${QuranMeta.TNAMES[n - 1]} ${QuranMeta.ENAMES[n - 1]} ${En.surahName(n)} ${Ar.surahName(n)}")
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(12)
    }
    val ref = remember(q) { QuranSearch.parseRef(q) }

    // الأدلة: نص الآية من المصحف، وترجمة معانيها بلغة الواجهة
    val trSource = remember(lang) { if (lang == Lang.AR) null else Sources.translationFor(lang) }
    val trText by produceState(trSource?.let { QuranData.cached("tafsir/${it.id}.txt") }, trSource) {
        value = trSource?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }

    // السور والأجزاء ثابتة ما لم تتغير اللغة أو ما قُرئ من الختمة
    val readCount = QuranProgress.readCount
    val surahs = remember(s, readCount) {
        (1..QuranMeta.SURAHS).map { n ->
            linkedMapOf<String, Any?>(
                "n" to n, "num" to "$n", "name" to s.surahName(n), "arabic" to QuranMeta.NAMES_AR[n - 1],
                "meta" to "${if (QuranMeta.isMedinan(n)) s.medinan else s.meccan} · ${s.ayahCount(QuranMeta.COUNTS[n - 1])}",
                "page" to s.pageName(QuranMeta.pageOf(QuranMeta.STARTS[n - 1])),
                "progress" to surahProgress(n).toDouble(), "route" to surahRoute(n),
            )
        }
    }
    val juz = remember(s) {
        (1..30).map { j ->
            val start = QuranMeta.JUZ[j - 1]
            val (sn, an) = QuranMeta.locate(start)
            linkedMapOf<String, Any?>(
                "n" to j, "num" to "$j", "title" to s.juzName(j), "sub" to s.ayahRef(start),
                "page" to s.pageName(QuranMeta.pageOf(start)), "route" to surahRoute(sn, an, "juz"),
            )
        }
    }

    /** آية في النتائج أو المحفوظات: الإحالة، ورقمها بالأرقام المشرقية، ونصها */
    fun hit(prefix: String, index: Int, mark: Boolean = false): Map<String, Any?> {
        val (sn, an) = QuranMeta.locate(index)
        return linkedMapOf(
            "key" to "$prefix$index", "ref" to s.ayahRef(index), "num" to arabicDigits(an),
            "text" to text?.let { QuranData.withoutBismillah(it, index) }, "route" to surahRoute(sn, an, "hit"), "mark" to mark,
        )
    }

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        // على الشاشة الواسعة: متابعة القراءة والختمة متجاورتان، والسور والأجزاء ونتائج البحث في عمودين (ToolPage columns)
        val wide = maxWidth - lead >= 840.dp
        val g = gutters(maxWidth, if (wide) SplitMax else TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 16.dp

        val last = QuranPrefs.last
        val cont = if (last < 0) null else {
            val (sn, an) = QuranMeta.locate(last)
            val f = QuranPrefs.focusLast
            linkedMapOf<String, Any?>(
                "label" to s.continueReading, "title" to s.ayahRef(last),
                "sub" to "${s.juzName(QuranMeta.juzOf(last))} · ${s.pageName(QuranMeta.pageOf(last))}",
                "arabic" to QuranMeta.NAMES_AR[sn - 1], "route" to surahRoute(sn, an, "continue"),
                "focus" to if (f < 0) null else QuranMeta.locate(f).let { (fs, fa) ->
                    linkedMapOf("label" to s.continueFocus, "page" to " · ${s.pageName(QuranMeta.pageOf(f))}", "route" to surahRoute(fs, fa, "focus"))
                },
            )
        }

        val p = QuranProgress
        val total = QuranProgress.PAGES
        val goal = p.dailyGoal()
        val khatma = linkedMapOf<String, Any?>(
            "title" to s.khatmaTitle, "frac" to p.readCount / total.toDouble(),
            "percent" to "${Fmt.num(p.readCount * 100.0 / total, 0)}%",
            "pages" to s.pagesRead(p.readCount, total), "streak" to s.streakDays(p.streak), "streakOn" to (p.streak > 0),
            "daysLeft" to if (p.planDays > 0 && !p.finished) s.daysLeftLabel(p.daysLeft()) else null,
            "plan" to s.planDays(p.planDays), "planLabel" to s.khatmaPlanLabel,
            "plans" to QuranProgress.PLANS.map { d -> linkedMapOf("days" to d, "label" to s.planDays(d), "on" to (d == p.planDays)) },
            "newKhatma" to s.newKhatma, "canRestart" to (p.readCount > 0),
            "finished" to p.finished, "done" to s.khatmaDone,
            "completed" to if (p.completed > 0) s.khatmasCompleted(p.completed) else null,
            "goal" to if (p.finished || goal <= 0) null else linkedMapOf(
                "label" to s.todayPortion,
                "value" to if (p.todayCount >= goal) s.portionDone else s.pagesRead(p.todayCount, goal),
                "done" to (p.todayCount >= goal), "frac" to (p.todayCount / goal.toDouble()).coerceIn(0.0, 1.0),
            ),
            "cta" to s.continuePortion,
            "ctaRoute" to QuranMeta.locate(QuranMeta.PAGES[p.nextUnread() - 1]).let { (sn, an) -> surahRoute(sn, an, "khatma") },
        )

        val quran = text
        val proofs = com.osatks.mizan.i18n.Evidence.quran.map { pr ->
            linkedMapOf<String, Any?>(
                "kind" to pr.kind.name.lowercase(), "concept" to pr.concept,
                "title" to pr.title[lang], "body" to pr.body[lang], "source" to pr.source[lang],
                "ayah" to pr.ayah?.let { (sn, from, to) ->
                    quran?.let { qt -> "﴿" + (from..to).joinToString(" ") { a -> QuranData.withoutBismillah(qt, QuranMeta.index(sn, a)) + " (" + arabicDigits(a) + ")" } + "﴾" }
                },
                "tr" to pr.ayah?.let { (sn, from, to) -> trText?.let { tr -> (from..to).joinToString(" ") { a -> tr[QuranMeta.index(sn, a)] } } },
                "hadith" to pr.hadith,
            )
        }

        val hits = ayahHits
        val search = if (q.isBlank()) null else linkedMapOf<String, Any?>(
            "ref" to ref?.let { hit("r", it) },
            "surahTitle" to s.searchQuranGroup, "surahs" to surahHits,
            "ayahTitle" to hits?.takeIf { it.isNotEmpty() }?.let { "${s.searchAyahsGroup} (${it.size})" },
            "busy" to (hits == null),
            "ayahs" to hits.orEmpty().map { hit("a", it) },
            "none" to if (hits != null && hits.isEmpty() && surahHits.isEmpty() && ref == null) s.searchNoResults(q) else null,
        )

        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark, "wide" to wide, "arabicUi" to (lang == Lang.AR),
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "title" to s.quranTitle, "subtitle" to s.quranSubtitle,
                "hint" to s.searchQuranHint, "clear" to s.clear, "noMarks" to s.noBookmarks,
                "query" to q, "tab" to tab,
                "tabs" to listOf("surahs" to s.surahsTab, "juz" to s.juzTab, "saved" to s.bookmarksTab)
                    .map { (k, l) -> linkedMapOf("key" to k, "label" to l) },
                "cont" to cont,
                "khatma" to khatma,
                "evidence" to linkedMapOf(
                    "title" to s.evidenceTitle, "sub" to s.evidenceSub,
                    "kinds" to linkedMapOf(
                        "quran" to s.proofQuran, "hadith" to s.proofHadith, "consensus" to s.proofConsensus, "scholars" to s.proofScholars,
                    ),
                    "points" to proofs,
                ),
                "surahs" to surahs,
                "juz" to juz,
                "marks" to QuranPrefs.bookmarks.toList().map { hit("b", it, mark = true) },
                "search" to search,
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                "x" to linkedMapOf(
                    "secondary" to hex(cs.secondary), "tertiary" to hex(cs.tertiary), "surfaceVariant" to hex(cs.surfaceVariant),
                    "secondaryContainer" to hex(cs.secondaryContainer), "onSecondaryContainer" to hex(cs.onSecondaryContainer),
                    "onPrimary" to hex(cs.onPrimary),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeQuran.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
