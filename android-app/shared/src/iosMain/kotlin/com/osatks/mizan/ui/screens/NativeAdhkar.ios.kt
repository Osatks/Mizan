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
import com.osatks.mizan.adhkar.Adhkar
import com.osatks.mizan.adhkar.AdhkarSection
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.now
import com.osatks.mizan.core.toEpochDay
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.Sources
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
import kotlinx.datetime.LocalDate
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر الأذكار من النظام (iosApp/iosApp/NativeAdhkar.swift)، على مثال [NativeHome]: الأقسام وأذكار القسم المفتوح JSON
 * مع الإشعار [STATE]؛ والعدّ واهتزازه في Swift، وKotlin يحفظ اكتمال القسم اليوم، ويفتح القسم ويغلقه، ويشارك الذكر.
 */
object NativeAdhkar {
    const val STATE = "MizanAdhkarState"

    private var factory: ((NativeAdhkarSession) -> UIViewController)? = null

    fun attach(make: (NativeAdhkarSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeAdhkarSession): UIViewController = factory!!.invoke(session)
}

class NativeAdhkarSession internal constructor() {
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onBack: () -> Unit = {}
    internal var onShare: (String) -> Unit = {}
    internal var onComplete: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeAdhkar.STATE, this)
    }

    /** يفتح القسم [id]، و"" يعود إلى الأقسام */
    fun open(id: String) = onOpen(id)
    fun back() = onBack()
    fun share(id: String) = onShare(id)

    /** اكتمل القسم [id] اليوم (علامة الإتمام على بطاقته) */
    fun complete(id: String) {
        AppSettings.putInt(doneKey(id), LocalDate.now().toEpochDay().toInt())
        onComplete()
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * الأذكار بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeAdhkar.swift) بطراز الرئيسية: أقسامها (وأذكار الوقت في أعلاها)،
 * ثم أذكار القسم [open] بعدّاد لكل ذكر وترجمته ودليله (الدرجة والمصدر والراوي والفضل) — كما في [AdhkarScreen].
 * [onOpen] يفتح قسماً ("" يعود إلى الأقسام)، و[onBack] يغلق الأذكار، و[onShare] يشارك ذكراً صورةَ قصة (من Compose).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeAdhkarView(
    sections: List<AdhkarSection>?, open: String, onOpen: (String) -> Unit, onBack: () -> Unit, onShare: (String) -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeAdhkarSession() }
    val openS by rememberUpdatedState(onOpen)
    val backS by rememberUpdatedState(onBack)
    val shareS by rememberUpdatedState(onShare)
    var doneTick by remember { mutableIntStateOf(0) }
    DisposableEffect(session) {
        session.onOpen = { openS(it) }
        session.onBack = { backS() }
        session.onShare = { shareS(it) }
        session.onComplete = { doneTick++ }
        onDispose { session.onOpen = {}; session.onBack = {}; session.onShare = {}; session.onComplete = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val tr = remember(lang) { Adhkar.tr(lang) }
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val sec = sections?.firstOrNull { it.id == open }
    // ترجمة معاني الآيات للأذكار القرآنية بلغة الواجهة
    val quranTr by produceState<Array<String>?>(null, lang, sec?.id) {
        value = if (lang == Lang.AR || sec == null || sec.items.none { it.ayah.isNotEmpty() }) null
        else Sources.translationFor(lang)?.let { runCatching { QuranData.source(it.id) }.getOrNull() }
    }
    val now = remember { suggested() }
    val tool = s.tools["adhkar"]

    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, TextColumnMax, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        @Suppress("UNUSED_VARIABLE") val tick = doneTick
        val list = sections.orEmpty().map { x ->
            linkedMapOf<String, Any?>(
                "id" to x.id, "icon" to x.icon, "title" to tr.title(x, lang), "desc" to tr.desc(x, lang),
                "countLabel" to s.dhikrItems(x.items.size), "done" to doneToday(x.id),
            )
        }
        val detail = sec?.let { x ->
            linkedMapOf<String, Any?>(
                "id" to x.id, "icon" to x.icon, "title" to tr.title(x, lang), "desc" to tr.desc(x, lang),
                "groups" to x.groups.map { gr ->
                    val title = if (lang == Lang.AR) gr.titleAr else tr.groups[gr.key] ?: gr.titleAr
                    linkedMapOf<String, Any?>(
                        "key" to gr.key, "title" to if (x.groups.size > 1) title else null,
                        "items" to gr.items.map { d ->
                            val itr = tr.items[d.id]
                            val note = if (lang == Lang.AR) d.note else itr?.note ?: d.note
                            linkedMapOf<String, Any?>(
                                "id" to d.id, "ar" to d.ar, "tr" to translation(d, tr, lang, quranTr),
                                "shortNote" to note?.takeIf { it.length <= 80 }, "note" to note,
                                "count" to d.count, "times" to s.dhikrTimes(d.count),
                                "grade" to d.grade, "gradeLine" to gradeLine(d, tr), "sources" to sourceLine(d, tr, s),
                                "narr" to if (lang == Lang.AR) d.narr else itr?.narr ?: d.narr,
                                "fadl" to d.fadl, "fadlTr" to if (lang != Lang.AR) itr?.fadl else null,
                                "ayahRefs" to if (d.ayah.isNotEmpty() && d.grade != "quran")
                                    d.ayah.joinToString(" · ") { (su, a, b) -> s.surahAyah(s.surahNames[su - 1], a) + if (b > a) "–$b" else "" } else null,
                            )
                        },
                    )
                },
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "ready" to (sections != null),
                "title" to (tool?.name ?: ""), "desc" to (tool?.desc ?: ""), "back" to s.back,
                "nowLabel" to s.adhkarNow, "nowId" to now, "tapHint" to s.dhikrTapHint, "doneLabel" to s.dhikrDone,
                "evidence" to s.dhikrEvidence, "share" to s.share, "gradeLabel" to s.dhikrGrade, "sourceLabel" to s.dhikrSource,
                "quranRefLabel" to s.dhikrQuranRef, "narratorLabel" to s.dhikrNarrator, "virtueLabel" to s.dhikrVirtue,
                "sections" to list, "open" to detail,
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
            factory = { NativeAdhkar.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
