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
import com.osatks.mizan.hadith.Hadith
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
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
 * جسر حديث اليوم من النظام (iosApp/iosApp/NativeHadith.swift)، على مثال [NativeAdhkar]: الحديث (نصه وترجمته وشرحه
 * وفوائده ودرجته ومصدره) والنصوص JSON مع الإشعار [STATE]؛ وSwift يخبر بالتنقل بين الأيام، والمشاركة، وفتح المصدر، والرجوع.
 */
object NativeHadith {
    const val STATE = "MizanHadithState"

    private var factory: ((NativeHadithSession) -> UIViewController)? = null

    fun attach(make: (NativeHadithSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeHadithSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeHadithAvailable: Boolean get() = NativeHadith.available

class NativeHadithSession internal constructor() {
    var state: String = ""
        private set

    internal var onDay: (Int) -> Unit = {}
    internal var onShare: () -> Unit = {}
    internal var onSource: () -> Unit = {}
    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeHadith.STATE, this)
    }

    /** حديث اليوم السابق */
    fun previousDay() = onDay(-1)

    /** حديث اليوم التالي (لا يتجاوز اليوم) */
    fun nextDay() = onDay(1)

    /** مشاركة الحديث صورةَ قصة (ورقة Compose) */
    fun share() = onShare()

    /** فتح الحديث في الموسوعة */
    fun openSource() = onSource()

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeHadithView(
    hadith: Hadith?, title: String, subtitle: String, canNext: Boolean,
    onDay: (Int) -> Unit, onShare: () -> Unit, onSource: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeHadithSession() }
    val dayS by rememberUpdatedState(onDay)
    val shareS by rememberUpdatedState(onShare)
    val sourceS by rememberUpdatedState(onSource)
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onDay = { dayS(it) }
        session.onShare = { shareS() }
        session.onSource = { sourceS() }
        session.onBack = { backS() }
        onDispose { session.onDay = {}; session.onShare = {}; session.onSource = {}; session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)

    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, TextColumnMax, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        // كما في HadithScreen: الشرح والفوائد بلغة الترجمة إن وُجدت وإلا بالعربية، والدرجة والتخريج تحت النص
        val h = hadith?.let { x ->
            val main = x.tr ?: x.ar
            val grade = listOf(x.tr?.grade ?: x.ar.grade, x.tr?.takhrij ?: x.ar.takhrij).filter { it.isNotBlank() }.joinToString(" · ")
            linkedMapOf<String, Any?>(
                "id" to x.id, "ar" to x.ar.text, "tr" to x.tr?.text, "grade" to grade,
                "arabic" to (x.tr == null), "expl" to main.expl, "benefits" to main.benefits,
                "sourceSub" to if (x.tr == null) "موسوعة الأحاديث النبوية" else "Encyclopedia of Translated Prophetic Hadiths",
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "back" to s.back, "title" to title, "subtitle" to subtitle,
                "explanation" to ht.explanation, "benefits" to ht.benefits, "share" to ht.shareStory,
                "source" to Hadiths.SOURCE, "prev" to s.prevDay, "next" to s.nextDay, "canNext" to canNext,
                "hadith" to h,
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
            factory = { NativeHadith.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
