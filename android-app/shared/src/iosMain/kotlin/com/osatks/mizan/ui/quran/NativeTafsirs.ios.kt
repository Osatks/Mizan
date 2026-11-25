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
 * جسر مكتبة التفاسير من النظام (iosApp/iosApp/NativeTafsirs.swift)، على مثال [com.osatks.mizan.ui.screens.NativeFitrFidya]:
 * محتوى الشاشة كله يبنيه TafsirLibraryScreen.kt (ويتجدد مع تقدم كل تنزيل)، وهنا يُضاف إليه المظهر والهوامش،
 * ويُرسل JSON مع الإشعار [STATE]. وSwift يخبره باللمس: التبويب، والتنزيل والإلغاء والحذف، وإظهار المصدر، والرجوع.
 */
object NativeTafsirs {
    const val STATE = "MizanTafsirsState"

    private var factory: ((NativeTafsirsSession) -> UIViewController)? = null

    fun attach(make: (NativeTafsirsSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeTafsirsSession): UIViewController = factory!!.invoke(session)
}

class NativeTafsirsSession internal constructor() {
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
        NSNotificationCenter.defaultCenter.postNotificationName(NativeTafsirs.STATE, this)
    }

    /** التبويب: get (التنزيل) أو device (على الجهاز) */
    fun tab(key: String) = onAction("tab", key)

    /** يبدأ تنزيل التفسير أو يعيد محاولته */
    fun download(slug: String) = onAction("download", slug)

    fun cancelDownload(slug: String) = onAction("cancel", slug)

    /** يسأل قبل حذف التفسير المنزَّل (تنبيه النظام من Compose) */
    fun remove(slug: String) = onAction("delete", slug)

    /** يُظهر المصدر في ورقة التفسير أو يخفيه */
    fun visible(id: String, on: Boolean) = onAction("visible:$id", on.toString())

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * مكتبة التفاسير بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeTafsirs.swift) بطراز الرئيسية: اختيار مقسّم بين
 * «تنزيل» و«على الجهاز»، وبطاقات التفاسير بشارة لغتها وحجمها وشريط تقدم تنزيلها، ومفتاح إظهار كل مصدر في ورقة التفسير
 * وحذف المنزَّل — كما في [TafsirLibraryScreen]. [content] يبنيه Kotlin كله (النصوص والحالات والنِّسب منسّقة بأرقام التطبيق).
 * Swift يردّ باللمس: [onAction] بمفتاح الأمر وقيمته (tab، download، cancel، delete، visible:<id>)، و[onBack] للرجوع.
 * التنزيل والحذف والحفظ تبقى في Kotlin، ونافذة تأكيد الحذف من Compose (تنبيه النظام).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeTafsirsView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) {
    val session = remember { NativeTafsirsSession() }
    val actionS by rememberUpdatedState(onAction)
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onAction = { k, v -> actionS(k, v) }
        session.onBack = { backS() }
        onDispose { session.onAction = { _, _ -> }; session.onBack = {} }
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    StatusBarStyle(lightContent = dark)
    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, TextColumnMax, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "back" to s.back,
            ) + content + linkedMapOf<String, Any?>(
                "error" to hex(cs.error),
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
            factory = { NativeTafsirs.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
