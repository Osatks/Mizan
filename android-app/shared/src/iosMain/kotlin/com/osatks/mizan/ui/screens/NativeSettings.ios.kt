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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Feedback
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.platform.AppStore
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.StoreLinks
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.TextColumnMax
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.theme.Accent
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
 * جسر تبويب الإعدادات من النظام (iosApp/iosApp/NativeSettings.swift)، على مثال [NativeTools]: Kotlin يرسل الحالة JSON
 * مع الإشعار [STATE] عند كل تغيّر، وSwift يخبره باللمس فيحفظ Kotlin الاختيار ويعيد نشر الحالة (والمظهر واتجاه اللغة معها).
 */
object NativeSettings {
    const val STATE = "MizanSettingsState"

    private var factory: ((NativeSettingsSession) -> UIViewController)? = null

    fun attach(make: (NativeSettingsSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeSettingsSession): UIViewController = factory!!.invoke(session)
}

class NativeSettingsSession internal constructor() {
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onLanguage: () -> Unit = {}
    internal var onNotify: (Boolean) -> Unit = {}
    internal var onShare: () -> Unit = {}
    internal var onFeedback: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeSettings.STATE, this)
    }

    fun open(route: String) = onOpen(route)
    fun language() = onLanguage()

    /** المظهر: system | light | dark */
    fun theme(key: String) = AppSettings.updateTheme(key)

    /** لون التطبيق: مفتاح من [Accent] */
    fun accent(key: String) = AppSettings.updateAccent(Accent.of(key).key)

    fun notify(on: Boolean) = onNotify(on)
    fun use24h(on: Boolean) = AppSettings.updateUse24h(on)
    fun exactAlarm() = PrayerAlerts.openExactAlarmSettings()
    fun rate() = AppStore.openRating()
    fun share() = onShare()
    fun feedback() = onFeedback()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

private fun row(title: String, sub: String?) = linkedMapOf<String, Any?>("title" to title, "sub" to sub)

/** نص المصادر بروابطه (LinkedText) بصيغة Markdown يعرضها Swift روابط */
private fun sourcesMarkdown(text: String): String {
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

/**
 * تبويب الإعدادات بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeSettings.swift) بطراز الرئيسية: مجموعات بطاقات
 * من الصفوف كما في [SettingsScreen]. [open] يفتح الصفحة، و[onLanguage] يفتح ورقة اللغة (تبقى في Compose)،
 * و[onNotify] يبدّل تنبيه الأذان مع طلب الإذن.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeSettingsView(open: (String) -> Unit, onLanguage: () -> Unit, onNotify: (Boolean) -> Unit, modifier: Modifier) {
    val session = remember { NativeSettingsSession() }
    val openS by rememberUpdatedState(open)
    val languageS by rememberUpdatedState(onLanguage)
    val notifyS by rememberUpdatedState(onNotify)
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val uri = LocalUriHandler.current
    val shareText = s.shareAppText(StoreLinks.forThisPlatform)
    val shareTitle = s.share
    val shareS by rememberUpdatedState({ Platform.share(shareText, shareTitle) })
    val langCode = lang.code
    // الملاحظات: رسالة بريد فيها الإصدار والنظام، وإن تعذّر فالعنوان في رسالة قصيرة
    val feedbackS by rememberUpdatedState({
        runCatching { uri.openUri(Feedback.mailto(langCode)) }.onFailure { Platform.toast(Feedback.EMAIL) }
        Unit
    })
    DisposableEffect(session) {
        session.onOpen = { openS(it) }
        session.onLanguage = { languageS() }
        session.onNotify = { notifyS(it) }
        session.onShare = { shareS() }
        session.onFeedback = { feedbackS() }
        onDispose { session.onOpen = {}; session.onLanguage = {}; session.onNotify = {}; session.onShare = {}; session.onFeedback = {} }
    }
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    val system = Lang.fromTag(systemLanguageTag())
    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, TextColumnMax, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 16.dp
        val current = Accent.of(AppSettings.accent)
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "title" to s.settingsTitle,
                "languageTitle" to s.language,
                "language" to row(lang.native, if (AppSettings.lang == Lang.SYSTEM) "${s.deviceLanguage} · ${system.native}" else null),
                "guide" to row(s.appGuide, s.appGuideSub),
                "appearance" to s.appearance,
                "theme" to AppSettings.theme,
                "themes" to listOf("system" to s.themeAuto, "light" to s.themeLight, "dark" to s.themeDark)
                    .map { (k, n) -> linkedMapOf("key" to k, "name" to n) },
                "accentLabel" to "${s.appColor} · ${s.accentNames.getOrElse(current.ordinal) { current.key }}",
                "accent" to current.key,
                "accents" to Accent.entries.map { a ->
                    linkedMapOf(
                        "key" to a.key, "name" to s.accentNames.getOrElse(a.ordinal) { a.key },
                        "c1" to hex(BrandGreen.recolor(a)), "c2" to hex(BrandGreenDeep.recolor(a)),
                    )
                },
                "quranTitle" to s.quranTitle,
                "tafsir" to row(s.tafsirLibrary, s.tafsirLibrarySub),
                "prayerTitle" to s.prayerTitle,
                "notify" to AppSettings.notify,
                "alert" to row(s.adhanAlert, s.adhanAlertDesc),
                "adhan" to row(s.adhanSettings, s.adhanSettingsSub),
                "use24h" to AppSettings.use24h,
                "clock" to row(s.use24h, s.use24hDesc),
                "exactAlarm" to if (PrayerAlerts.hasExactAlarmSettings) row(s.exactAlarm, s.exactAlarmDesc) else null,
                "rate" to row(s.rateApp, s.rateAppSub),
                "shareApp" to row(s.shareApp, s.shareAppSub),
                "feedback" to row(ht.feedback, ht.feedbackSub),
                "aboutTitle" to s.about, "aboutName" to s.aboutName, "version" to s.version(Platform.versionName()),
                "aboutText" to s.aboutText, "sources" to sourcesMarkdown(s.quranSources),
                "licenses" to row(s.licenses, s.licensesSub),
                "privacyTitle" to s.privacy, "privacyText" to s.privacyText,
                "disclaimer" to s.disclaimer,
                "w" to linkedMapOf(
                    "bg" to hex(cs.secondaryContainer), "fg" to hex(cs.onSecondaryContainer), "icon" to hex(cs.secondary),
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
            factory = { NativeSettings.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
