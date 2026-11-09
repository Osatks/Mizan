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
 * جسر البحث من النظام (iosApp/iosApp/NativeSearch.swift)، على مثال [NativeAdhkar]: النتائج (يحسبها SearchScreen.kt كما هي)
 * مجمّعة بعناوين مجموعاتها، والجزء المطابق من كل عنوان، والنصوص، JSON مع الإشعار [STATE].
 * وSwift يخبر بالنص المكتوب (query)، وبالنتيجة الملموسة (pick)، وبالرجوع.
 */
object NativeSearch {
    const val STATE = "MizanSearchState"

    private var factory: ((NativeSearchSession) -> UIViewController)? = null

    fun attach(make: (NativeSearchSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeSearchSession): UIViewController = factory!!.invoke(session)
}

internal actual val nativeSearchAvailable: Boolean get() = NativeSearch.available

class NativeSearchSession internal constructor() {
    var state: String = ""
        private set

    internal var onQuery: (String) -> Unit = {}
    internal var onPick: (String) -> Unit = {}
    internal var onBack: () -> Unit = {}

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeSearch.STATE, this)
    }

    /** نص البحث صار [text] */
    fun query(text: String) = onQuery(text)

    /** لُمست النتيجة [id] (يُنفّذ فعلها كما في Compose) */
    fun pick(id: String) = onPick(id)

    fun back() = onBack()
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/** نوع النتيجة من معرّفها (أيقونتها في Swift)، على أيقونات SearchScreen */
private fun hitIcon(id: String): String = when {
    id.startsWith("t-") -> id.removePrefix("t-")
    id.startsWith("q-") || id.startsWith("v-") -> "quran"
    id.startsWith("p-") || id == "s-24h" -> "time"
    id.startsWith("c-") -> "city"
    id.startsWith("e-") -> "event"
    id.startsWith("d-") -> "dhikr"
    id == "s-lang" -> "language"
    id == "s-theme" -> "theme"
    id == "s-adhan" -> "adhan"
    id == "s-method" -> "method"
    else -> "search"
}

/** الجزء المطابق حرفياً من العنوان (كما يُبرزه highlight في SearchScreen): قبله، وهو، وبعده */
private fun hitParts(text: String, q: String): List<String>? {
    val needle = q.trim()
    val i = if (needle.isEmpty()) -1 else text.indexOf(needle, ignoreCase = true)
    if (i < 0) return null
    return listOf(text.substring(0, i), text.substring(i, i + needle.length), text.substring(i + needle.length))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun NativeSearchView(
    query: String, blank: Boolean, hits: List<NativeSearchHit>,
    onQuery: (String) -> Unit, onPick: (String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
) {
    val session = remember { NativeSearchSession() }
    val queryS by rememberUpdatedState(onQuery)
    val pickS by rememberUpdatedState(onPick)
    val backS by rememberUpdatedState(onBack)
    DisposableEffect(session) {
        session.onQuery = { queryS(it) }
        session.onPick = { pickS(it) }
        session.onBack = { backS() }
        onDispose { session.onQuery = {}; session.onPick = {}; session.onBack = {} }
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
        // النتائج مرتبة من SearchScreen؛ وهنا تُجمع المتتالية من مجموعة واحدة تحت عنوانها
        val groups = mutableListOf<Pair<String, MutableList<NativeSearchHit>>>()
        for (h in hits) {
            val last = groups.lastOrNull()
            if (last != null && last.first == h.group) last.second.add(h) else groups.add(h.group to mutableListOf(h))
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "back" to s.back, "hint" to s.searchHint, "clear" to s.clear,
                "query" to query, "blank" to blank,
                "emptyText" to if (blank) s.searchEmpty else s.searchNoResults(query), "blankHint" to s.searchEmpty,
                "groups" to groups.map { (title, list) ->
                    linkedMapOf<String, Any?>(
                        "title" to title,
                        "hits" to list.map { h ->
                            linkedMapOf<String, Any?>(
                                "id" to h.id, "icon" to hitIcon(h.id), "title" to h.title, "parts" to hitParts(h.title, query),
                                "sub" to h.subtitle, "quran" to h.id.startsWith("v-"),
                            )
                        },
                    )
                },
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
            factory = { NativeSearch.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
