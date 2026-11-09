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
 * جسر الأذان والتنبيهات من النظام (iosApp/iosApp/NativeAdhan.swift)، على مثال [NativeFitrFidya]: محتوى الشاشة كله
 * يبنيه AdhanScreen.kt (ويتجدد مع تقدم التنزيل والمعاينة)، وهنا يُضاف إليه المظهر والهوامش، ويُرسل JSON مع الإشعار [STATE].
 * وSwift يخبره باللمس: المفاتيح، وطريقة التنبيه، واختيار المؤذن وتنزيله وحذفه والاستماع إليه، والاهتزاز، والرجوع.
 */
object NativeAdhan {
    const val STATE = "MizanAdhanState"

    private var factory: ((NativeAdhanSession) -> UIViewController)? = null

    fun attach(make: (NativeAdhanSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeAdhanSession): UIViewController = factory!!.invoke(session)
}

class NativeAdhanSession internal constructor() {
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
        NSNotificationCenter.defaultCenter.postNotificationName(NativeAdhan.STATE, this)
    }

    /** تنبيه الأذان (مع طلب الإذن من Compose) */
    fun notify(on: Boolean) = onAction("notify", on.toString())

    /** العدّ التنازلي الحي على شاشة القفل */
    fun live(on: Boolean) = onAction("live", on.toString())

    /** المواقيت في تطبيق التقويم */
    fun calendar(on: Boolean) = onAction("calendar", on.toString())

    fun exactAlarm() = onAction("exactAlarm", "")

    /** طريقة التنبيه [mode] (adhan، vibrate، silent، off) لصلاة [prayer] */
    fun alertMode(prayer: String, mode: String) = onAction("mode:$prayer", mode)

    /** اختيار مؤذن بقية الصلوات (يُنزَّل أولاً إن لم يكن على الجهاز) */
    fun pick(id: String) = onAction("pick", id)

    /** اختيار أذان الفجر، أو same مثل بقية الصلوات */
    fun pickFajr(id: String) = onAction("pickFajr", id)

    fun download(id: String) = onAction("download", id)
    fun remove(id: String) = onAction("delete", id)

    /** يشغّل معاينة التسجيل أو يوقفها */
    fun play(id: String) = onAction("play", id)

    /** يختار نمط الاهتزاز ويجرّبه */
    fun vibe(key: String) = onAction("vibe", key)

    /** يجرّب نمط الاهتزاز دون اختياره */
    fun tryVibe(key: String) = onAction("tryVibe", key)

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * الأذان والتنبيهات بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeAdhan.swift) بطراز الرئيسية: مفاتيح التنبيه
 * والعدّ الحي والتقويم، وطريقة التنبيه لكل صلاة (قائمة النظام)، والمؤذن وأذان الفجر مع الاستماع والتنزيل بتقدمه والحذف،
 * وأنماط الاهتزاز برسمها وتجربتها، ومصادر التسجيلات — كما في [AdhanScreen]. [content] يبنيه Kotlin كله.
 * Swift يردّ باللمس: [onAction] بمفتاح الأمر وقيمته (notify، live، calendar، exactAlarm، mode:<prayer>، pick، pickFajr،
 * download، delete، play، vibe، tryVibe)، و[onBack] للرجوع. التنزيل والتشغيل والحفظ تبقى في Kotlin.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeAdhanView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
) {
    val session = remember { NativeAdhanSession() }
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
            factory = { NativeAdhan.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
