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
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
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
 * جسر جولة التطبيق من النظام (iosApp/iosApp/NativeGuide.swift)، على مثال [NativeHome]: Kotlin يرسل الصفحات والصفحة
 * الحالية والنصوص JSON مع الإشعار [STATE]، وSwift يخبره بالسحب إلى صفحة وبالأزرار (التالي، تخطي، الموقع، المدينة).
 */
object NativeGuide {
    const val STATE = "MizanGuideState"

    private var factory: ((NativeGuideSession) -> UIViewController)? = null

    fun attach(make: (NativeGuideSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeGuideSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeGuideAvailable: Boolean get() = NativeGuide.available

class NativeGuideSession internal constructor() {
    var state: String = ""
        private set

    internal var onPage: (Int) -> Unit = {}
    internal var onNext: () -> Unit = {}
    internal var onSkip: () -> Unit = {}
    internal var onLocate: () -> Unit = {}
    internal var onPickCity: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeGuide.STATE, this)
    }

    /** سحب المستخدم إلى الصفحة [page] */
    fun go(page: Int) = onPage(page)

    /** «التالي»، وفي الصفحة الأخيرة «ابدأ» */
    fun goNext() = onNext()

    /** «تخطي»: إلى صفحة الموقع، ومنها يُنهي الجولة */
    fun skip() = onSkip()

    /** «استعمل موقعي» (يطلب الإذن في Kotlin) */
    fun locate() = onLocate()

    /** اختيار مدينة (ورقة Compose) */
    fun pickCity() = onPickCity()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeGuideView(
    pages: List<GuidePage>, page: Int, location: Boolean, placeName: String?, locating: Boolean,
    onPage: (Int) -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onLocate: () -> Unit, onPickCity: () -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeGuideSession() }
    val pageS by rememberUpdatedState(onPage)
    val nextS by rememberUpdatedState(onNext)
    val skipS by rememberUpdatedState(onSkip)
    val locateS by rememberUpdatedState(onLocate)
    val cityS by rememberUpdatedState(onPickCity)
    DisposableEffect(session) {
        session.onPage = { pageS(it) }
        session.onNext = { nextS() }
        session.onSkip = { skipS() }
        session.onLocate = { locateS() }
        session.onPickCity = { cityS() }
        onDispose { session.onPage = {}; session.onNext = {}; session.onSkip = {}; session.onLocate = {}; session.onPickCity = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    BoxWithConstraints(modifier) {
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val last = page == pages.size - 1
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "width" to maxWidth.value.toDouble(),
                "pages" to pages.map { linkedMapOf("title" to it.title, "body" to it.body, "icon" to it.icon) },
                "page" to page, "last" to last, "location" to location,
                // «تخطي» يختفي في الصفحة الأخيرة متى اختير الموقع (كما في جولة Compose)
                "showSkip" to (!last || placeName == null),
                "place" to placeName, "locating" to locating,
                "skip" to s.obSkip, "next" to s.obNext, "start" to s.obStart,
                "useMyLocation" to s.useMyLocation, "locatingText" to s.locating, "pickCity" to s.pickCity,
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
            factory = { NativeGuide.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
