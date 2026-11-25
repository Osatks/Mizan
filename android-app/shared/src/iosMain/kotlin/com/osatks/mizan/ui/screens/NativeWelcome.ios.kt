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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.platform.AdhanPreview
import com.osatks.mizan.platform.systemLanguageTag
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.ui.quran.isDepthTone
import com.osatks.mizan.ui.quran.mushafPalette
import com.osatks.mizan.ui.quran.readingPalette
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر إعداد أول تشغيل من النظام (iosApp/iosApp/NativeWelcome.swift)، على مثال [NativeGuide]: Kotlin يرسل الخطوة الحالية
 * وكل ما في الخطوات (النصوص، واللغات، والموقع ومواقيت اليوم، والأذان، والمظهر، والملخص) JSON مع الإشعار [STATE]،
 * وSwift يخبره باللمس فيُنفَّذ هنا بالإعدادات نفسها (IosWelcome). حبات تجربة المسبحة تُعدّ في Swift وحده.
 */
object NativeWelcome {
    const val STATE = "MizanWelcomeState"

    private var factory: ((NativeWelcomeSession) -> UIViewController)? = null

    fun attach(make: (NativeWelcomeSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeWelcomeSession): UIViewController = factory!!.invoke(session)
}

class NativeWelcomeSession internal constructor() {
    var state: String = ""
        private set

    internal var onAction: (String, String) -> Unit = { _, _ -> }

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeWelcome.STATE, this)
    }

    /** سحب المستخدم إلى الخطوة [page] */
    fun go(page: Int) = onAction("go", page.toString())

    /** «التالي»، وفي الخطوة الأخيرة «ابدأ» */
    fun goNext() = onAction("next", "")

    /** «رجوع» خطوة */
    fun goBack() = onAction("back", "")

    /** «تخطي»: يُنهي الإعداد بما اختير حتى الآن (والباقي على الافتراضي) */
    fun skip() = onAction("skip", "")

    /** لغة الواجهة: رمزها، أو system للغة الجهاز */
    fun language(code: String) = onAction("language", code)

    /** «استعمل موقعي» (يطلب الإذن في Kotlin) */
    fun locate() = onAction("locate", "")

    /** اختيار مدينة (ورقة المدن من النظام) */
    fun pickCity() = onAction("city", "")

    /** تنبيهات الأذان (مع طلب إذن الإشعارات) */
    fun alerts(on: Boolean) = onAction("alerts", on.toString())

    /** المؤذن لكل الصلوات */
    fun muezzin(id: String) = onAction("muezzin", id)

    /** يشغّل معاينة الأذان أو يوقفها */
    fun play(id: String) = onAction("play", id)

    /** المظهر: system أو light أو dark */
    fun theme(key: String) = onAction("theme", key)

    /** لون صفحة المصحف ("" تلقائي، sepia، night، depth، depthNight) */
    fun tone(key: String) = onAction("tone", key)
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * إعداد أول تشغيل بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeWelcome.swift)، بخطوات [WelcomeFlow] نفسها كما في
 * [Welcome] بأندرويد. [step] الخطوة الحالية و[locating] طلب الموقع الجاري؛ والباقي يُقرأ هنا من الإعدادات فيتجدد معها.
 * العدّ إلى الصلاة القادمة يتقدم كل ثانية في Swift، وKotlin يعيد الحالة كل دقيقة وعند الأذان.
 * [onAction] بمفتاح الأمر وقيمته: go، next، back، skip، language، locate، city، alerts، muezzin، play، theme، tone.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeWelcomeView(step: Int, locating: Boolean, onAction: (String, String) -> Unit, modifier: Modifier) {
    val session = remember { NativeWelcomeSession() }
    val actionS by rememberUpdatedState(onAction)
    DisposableEffect(session) {
        session.onAction = { k, v -> actionS(k, v) }
        onDispose { session.onAction = { _, _ -> } }
    }
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    val dark = LocalDark.current
    val system = Lang.fromTag(systemLanguageTag())

    // الصلاة القادمة تُعاد كل دقيقة وعند أذانها؛ والأوقات لحظات مطلقة (epoch) في منطقة الموقع
    var minute by remember { mutableIntStateOf(0) }
    val now = remember(minute, AppSettings.place) { PrayerUtil.now() }
    val today = now.date
    val next = remember(minute, AppSettings.place, AppSettings.method, AppSettings.hanafi) { PrayerUtil.next() }
    val zone = remember(AppSettings.place) { runCatching { TimeZone.of(AppSettings.place.tz) }.getOrElse { TimeZone.currentSystemDefault() } }
    val nextEpoch = next.at.toInstant(zone).epochSeconds
    val nextAt by rememberUpdatedState(nextEpoch * 1000)
    LaunchedEffect(Unit) {
        while (true) {
            val ms = Clock.System.now().toEpochMilliseconds()
            val toMinute = 60_000 - ms % 60_000 + 50
            val toAdhan = nextAt - ms + 50
            delay(if (toAdhan in 1 until toMinute) toAdhan else toMinute)
            minute++
        }
    }
    val nextToday = next.key.takeIf { next.at.date == today }
    val placeSet = AppSettings.placeSet
    val prayers = if (placeSet) welcomePrayers(s, today, nextToday) else emptyList()
    val autoPalette = mushafPalette()

    BoxWithConstraints(modifier) {
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "width" to maxWidth.value.toDouble(),
                "step" to step, "steps" to WelcomeFlow.KEYS,
                "stepLabel" to s.obStep(step + 1, WelcomeFlow.COUNT),
                "skip" to s.obSkip, "next" to s.obNext, "back" to s.back, "start" to s.obStart,
                "titles" to listOf(s.obHelloTitle, s.obLocationTitle, s.obAdhanTitle, s.obStyleTitle, s.obTryTitle, s.obDoneTitle),
                "bodies" to listOf(s.obHelloBody, s.obLocationBody, s.obAdhanBody, s.obStyleBody, s.obTryBody, s.obDoneBody),
                // ---------- اللغة ----------
                "languageLabel" to s.language,
                "langs" to listOf(
                    linkedMapOf<String, Any?>("code" to Lang.SYSTEM, "name" to "${s.deviceLanguage} · ${system.native}", "selected" to (AppSettings.lang == Lang.SYSTEM))
                ) + Lang.entries.map { l -> linkedMapOf<String, Any?>("code" to l.code, "name" to l.native, "selected" to (AppSettings.lang == l.code)) },
                // ---------- الموقع ومعاينة المواقيت ----------
                "place" to (if (placeSet) s.placeName(AppSettings.place) else null),
                "locating" to locating,
                "useMyLocation" to s.useMyLocation, "locatingText" to s.locating, "pickCity" to s.pickCity,
                "todayTitle" to s.obTodayTimes,
                "prayers" to prayers.map { p -> linkedMapOf<String, Any?>("key" to p.key, "name" to p.name, "time" to p.time, "next" to p.next) },
                "nextPrayer" to (if (placeSet) linkedMapOf<String, Any?>("label" to s.nextPrayerLabel(s.prayer(next.key)), "at" to nextEpoch) else null),
                // ---------- الأذان ----------
                "alert" to linkedMapOf<String, Any?>("title" to s.adhanAlert, "sub" to s.adhanAlertDesc, "on" to AppSettings.notify),
                "muezzinTitle" to s.muezzinTitle, "moreVoices" to s.obMoreVoices, "play" to s.playAdhan, "stop" to s.stopAdhan,
                "muezzins" to WelcomeFlow.muezzins().map { m ->
                    linkedMapOf<String, Any?>(
                        "id" to m.id, "name" to s.muezzinName(m), "sub" to duration(m.seconds),
                        "selected" to (AppSettings.muezzin == m.id), "playing" to (AdhanPreview.playing == m.id),
                    )
                },
                // ---------- المظهر ----------
                "appearance" to s.appearance,
                "themes" to WelcomeFlow.THEMES.map { k ->
                    linkedMapOf<String, Any?>("key" to k, "label" to s.welcomeThemeName(k), "selected" to (AppSettings.theme == k))
                },
                "pageColor" to s.pageColor,
                // ورق كل لون وحبره وتذهيبه للبطاقات المصغّرة (التلقائي بحسب المظهر الحالي)
                "tones" to WelcomeFlow.TONES.map { t ->
                    val pal = readingPalette(t) ?: autoPalette
                    linkedMapOf<String, Any?>(
                        "key" to t, "label" to s.welcomeToneName(t), "selected" to (QuranPrefs.tone == t), "depth" to isDepthTone(t),
                        "paper" to hex(pal.paper), "paperEdge" to hex(pal.paperEdge), "ink" to hex(pal.ink),
                        "gold" to hex(pal.gold), "goldDeep" to hex(pal.goldDeep),
                    )
                },
                // ---------- التجربة والملخص ----------
                "dhikr" to WelcomeFlow.DHIKR, "bead" to s.obBead, "tryDone" to s.obTryDone, "beads" to WelcomeFlow.BEADS,
                "summary" to welcomeSummary(s, lang).map { r ->
                    linkedMapOf<String, Any?>("icon" to r.icon, "label" to r.label, "value" to r.value, "ok" to r.ok)
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
            factory = { NativeWelcome.make(session) },
            modifier = Modifier.fillMaxSize(),
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
