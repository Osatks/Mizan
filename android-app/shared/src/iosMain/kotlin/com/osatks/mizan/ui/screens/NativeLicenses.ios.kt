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
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.ScriptText
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.Reciters
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
 * جسر صفحة المصادر والتراخيص من النظام (iosApp/iosApp/NativeLicenses.swift)، على مثال [NativeHome]: Kotlin يرسل
 * الأقسام (العنوان والنص، وبعضها Markdown بروابطه) JSON مع الإشعار [STATE]، وSwift يخبره بالرجوع.
 */
object NativeLicenses {
    const val STATE = "MizanLicensesState"

    private var factory: ((NativeLicensesSession) -> UIViewController)? = null

    fun attach(make: (NativeLicensesSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeLicensesSession): UIViewController = factory!!.invoke(session)
}

class NativeLicensesSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeLicenses.STATE, this)
    }

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** نص المصادر بروابطه (LinkedText) بصيغة Markdown يعرضها Swift روابط */
private fun licensesMarkdown(text: String): String {
    fun esc(t: String) = t.replace(Regex("""[\\`*_\[\]#~]""")) { "\\" + it.value }
    val out = StringBuilder()
    var last = 0
    Regex("tanzil\\.net/trans|tanzil\\.net|SIL").findAll(text).forEach { m ->
        out.append(esc(text.substring(last, m.range.first)))
        val url = when (m.value) { "SIL" -> "https://openfontlicense.org"; "tanzil.net/trans" -> "https://tanzil.net/trans/"; else -> "https://tanzil.net/" }
        out.append('[').append(esc(m.value)).append("](").append(url).append(')')
        last = m.range.last + 1
    }
    out.append(esc(text.substring(last)))
    return out.toString()
}

/** قسم من الصفحة: [md] نصه Markdown بروابط، و[mono] نص رخصة طويل يُعرض كما هو */
private fun licenseSection(title: String, body: String, md: Boolean = false, mono: Boolean = false) =
    linkedMapOf<String, Any?>("title" to title, "body" to body, "md" to md, "mono" to mono)

/**
 * المصادر والتراخيص بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeLicenses.swift) بطراز الرئيسية: ترويستها،
 * وبطاقة لكل مصدر، ونصّا الرخصتين كما هما — كما في [LicensesScreen]. [onBack] يغلق الصفحة.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeLicensesView(onBack: () -> Unit, modifier: Modifier) {
    val session = remember { NativeLicensesSession() }
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onBack = { backS() }
        onDispose { session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val font = remember { Platform.asset("licenses/AmiriQuran-OFL.txt")?.decodeToString()?.trim() }
    val adhan = remember { Platform.asset("licenses/adhan-recordings.txt")?.decodeToString()?.trim() }
    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        val g = gutters(maxWidth, TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        // الأقسام بترتيب صفحة Compose
        val sections = buildList {
            add(licenseSection(s.quranTitle, licensesMarkdown(s.quranSources), md = true))
            // مصحف المدينة: خطوط المجمع وبيانات الأسطر
            add(
                licenseSection(
                    ScriptText.of(lang.code).madinah,
                    "King Fahd Glorious Quran Printing Complex (KFGQPC), Madinah: QPC V2 page fonts, surah header font and " +
                        "Uthmanic Hafs font, used unmodified. Page layout and glyph data: Quranic Universal Library " +
                        "(qul.tarteel.ai) and Quran.com. Hadith of the day: HadeethEnc.com.",
                )
            )
            add(licenseSection(s.tafsirLibrary, s.tafsirLibraryNote))
            add(licenseSection(s.reciter, "EveryAyah.com · " + Reciters.ALL.joinToString(" · ") { it.name(lang) }))
            add(licenseSection(s.recordingSources, Muezzins.ALL.joinToString("\n") { m -> "${s.muezzinName(m)}: ${m.credit}" }))
            if (adhan != null) add(licenseSection(adhan.substringBefore('\n'), adhan.substringAfter('\n').trim(), mono = true))
            if (font != null) add(licenseSection("Amiri Quran · SIL Open Font License 1.1", font, mono = true))
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "back" to s.back, "title" to s.licenses, "subtitle" to s.licensesSub,
                "sections" to sections,
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
            factory = { NativeLicenses.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
