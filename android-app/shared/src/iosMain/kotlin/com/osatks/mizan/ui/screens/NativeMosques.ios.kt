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
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
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
 * جسر صفحة المساجد القريبة من النظام (iosApp/iosApp/NativeMosques.swift)، على مثال [NativeQibla]: Kotlin يرسل النصوص
 * والألوان والهوامش وموقع التطبيق المحفوظ JSON مع الإشعار [STATE]؛ والموقع الحالي والبحث (MKLocalSearch) والخريطة
 * والتنقل (خرائط Apple أو Google) كلها في Swift. Swift يخبره بالرجوع فقط.
 */
object NativeMosques {
    const val STATE = "MizanMosquesState"

    private var factory: ((NativeMosquesSession) -> UIViewController)? = null

    fun attach(make: (NativeMosquesSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeMosquesSession): UIViewController = factory!!.invoke(session)
}

class NativeMosquesSession internal constructor() {
    var state: String = ""
        private set

    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeMosques.STATE, this)
    }

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * المساجد القريبة بواجهة النظام (SwiftUI و MapKit في iOS: iosApp/iosApp/NativeMosques.swift) بطراز القبلة: خريطة
 * بالدبابيس، وقائمة مرتبة بالمسافة؛ لمس المسجد يفتح الاتجاهات في خرائط Apple، ومعها خرائط Google إن كانت مثبتة.
 * [onBack] يغلق الصفحة (null: بلا زر رجوع).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeMosquesView(onBack: (() -> Unit)?, modifier: Modifier) {
    val session = remember { NativeMosquesSession() }
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
    val placeSet = AppSettings.placeSet

    BoxWithConstraints(modifier) {
        val lead = LocalLeadingInset.current
        val g = gutters(maxWidth, TextColumnMax, lead)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        // كلمات البحث في خرائط Apple: كلمة «مسجد» بلغة التطبيق، ومعها mosque و masjid (أسماء المساجد في الخرائط بلغات شتى)
        val words = linkedSetOf(s.mosque.lowercase(), "mosque", "masjid").toList()
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(), "lead" to lead.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "canBack" to (onBack != null), "back" to s.back,
                "title" to s.mosquesTitle, "subtitle" to s.mosquesSubtitle,
                // الأرقام لاتينية دائماً بفاصل الكسور في لغة التطبيق (Fmt)
                "decimal" to lang.decimal.toString(),
                "placeSet" to placeSet, "lat" to place.lat, "lng" to place.lng,
                "nearSaved" to s.mosquesNearSaved(s.placeName(place)),
                "words" to words,
                "mosque" to s.mosque, "unitKm" to s.unitKm, "unitM" to s.unitM,
                "locating" to s.locating, "loading" to s.mosquesLoading,
                "empty" to s.mosquesEmpty, "emptyHint" to s.mosquesEmptyHint,
                "error" to s.mosquesError, "errorHint" to s.mosquesErrorHint,
                "locationFailed" to s.mosquesLocationFailed, "needLocation" to s.mosquesNeedLocation,
                "locationOff" to s.locationOffTitle, "retry" to s.retry, "openInMaps" to s.openInMaps,
                "useMyLocation" to s.useMyLocation, "openSettings" to s.openSettings,
                "directions" to s.mosqueDirections, "appleMaps" to s.appleMaps, "googleMaps" to s.googleMaps,
                "privacy" to s.mosquesPrivacyApple, "map" to s.mosquesMap,
                "c" to linkedMapOf(
                    "primary" to hex(cs.primary), "primaryContainer" to hex(cs.primaryContainer),
                    "onPrimaryContainer" to hex(cs.onPrimaryContainer), "background" to hex(cs.background),
                    "surface" to hex(cs.surface), "onSurface" to hex(cs.onSurface), "onSurfaceVariant" to hex(cs.onSurfaceVariant),
                    "outlineVariant" to hex(cs.outlineVariant), "gold" to hex(Gold), "goldDeep" to hex(GoldDeep),
                    "hero1" to hex(BrandGreen.recolor(accent)), "hero2" to hex(BrandGreenDeep.recolor(accent)),
                ),
                "x" to linkedMapOf(
                    "warnBg" to hex(cs.secondaryContainer), "warnFg" to hex(cs.onSecondaryContainer), "warn" to hex(cs.secondary),
                ),
            )
        )
        session.prime(json)
        SideEffect { session.publish(json) }
        UIKitViewController(
            factory = { NativeMosques.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
