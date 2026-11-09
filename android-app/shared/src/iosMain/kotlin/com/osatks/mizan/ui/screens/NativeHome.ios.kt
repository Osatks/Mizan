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
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Hijri
import com.osatks.mizan.core.PrayTimes
import com.osatks.mizan.core.plusDays
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.i18n.HomeText
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.pack.Json
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.i18n.prayer
import com.osatks.mizan.i18n.time
import com.osatks.mizan.i18n.weekday
import com.osatks.mizan.quran.QuranData
import com.osatks.mizan.quran.QuranMeta
import com.osatks.mizan.quran.QuranPrefs
import com.osatks.mizan.quran.ayahOfDay
import com.osatks.mizan.ui.LocalBottomInset
import com.osatks.mizan.ui.LocalLeadingInset
import com.osatks.mizan.ui.LocalPageActive
import com.osatks.mizan.ui.StatusBarStyle
import com.osatks.mizan.ui.gutters
import com.osatks.mizan.ui.quran.ayahRef
import com.osatks.mizan.ui.quran.surahName
import com.osatks.mizan.ui.quran.surahRoute
import com.osatks.mizan.ui.theme.BrandGreen
import com.osatks.mizan.ui.theme.BrandGreenDeep
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.GoldDeep
import com.osatks.mizan.ui.theme.LocalAccent
import com.osatks.mizan.ui.theme.LocalDark
import com.osatks.mizan.ui.theme.recolor
import com.osatks.mizan.widget.AdhkarClock
import com.osatks.mizan.widget.WidgetData
import com.osatks.mizan.widget.WidgetLaunch
import com.osatks.mizan.widget.WidgetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر الرئيسية من النظام (iosApp/iosApp/NativeHome.swift): Swift يسجّل عند التشغيل ما ينشئ متحكمها ([attach])،
 * فتعرض HomeScreen واجهة SwiftUI بدل رئيسية Compose. Kotlin يرسل الحالة JSON ([NativeHomeSession.state]) مع
 * الإشعار [STATE] كل دقيقة وعند كل تغيّر (اللغة، الموقع، المظهر، موضع القراءة)، والعدّادات تتقدم كل ثانية في Swift
 * من أوقات الصلاة بالثواني (epoch). وSwift يخبره باللمس: فتح مسار، أو ورقة اللغة، أو أذكار الوقت.
 */
object NativeHome {
    /** الإشعار مع كل تغيّر في حالة الرئيسية (object: جلستها) */
    const val STATE = "MizanHomeState"

    private var factory: ((NativeHomeSession) -> UIViewController)? = null

    /** يستدعيه Swift مرة عند التشغيل (قبل MainViewController) */
    fun attach(make: (NativeHomeSession) -> UIViewController) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeHomeSession): UIViewController = factory!!.invoke(session)
}

/** رئيسية واحدة معروضة: حالتها، وما يصل منها إلى التطبيق */
class NativeHomeSession internal constructor() {
    /** آخر حالة أُرسلت (JSON)، تقرؤها الواجهة حين تُنشأ ومع كل إشعار */
    var state: String = ""
        private set

    internal var onOpen: (String) -> Unit = {}
    internal var onLanguage: () -> Unit = {}

    /** الحالة الأولى قبل أن تُنشأ الواجهة (بلا إشعار) */
    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeHome.STATE, this)
    }

    /** يفتح مساراً كما من رئيسية Compose (prayer، adhkar، surah/…، hadith، search، …) */
    fun open(route: String) = onOpen(route)

    /** ورقة اللغة */
    fun language() = onLanguage()

    /** أذكار الوقت: يفتح قسمها مباشرة */
    fun adhkar(section: String) {
        WidgetLaunch.adhkarSection = section
        onOpen("adhkar")
    }
}

private fun hex(c: Color): String = "#" + (c.toArgb().toLong() and 0xFFFFFFFFL).toString(16).padStart(8, '0')

/**
 * الرئيسية بواجهة النظام نفسها (SwiftUI في iOS: iosApp/iosApp/NativeHome.swift): الترويسة وبطاقة اليوم وأذكار الوقت
 * والصلاة القادمة، ومواقيت اليوم، وأدوات كل يوم، والقرآن الكريم، وآية اليوم وحديثه — كما في [HomeScreen].
 * العدّادات تتقدم كل ثانية في Swift دون Compose؛ وKotlin يرسل الحالة كل دقيقة وعند كل تغيّر.
 * [open] يفتح المسار كما من الرئيسية، و[onLanguage] يفتح ورقة اللغة.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NativeHomeView(open: (String) -> Unit, onLanguage: () -> Unit, modifier: Modifier) {
    val session = remember { NativeHomeSession() }
    val openState by rememberUpdatedState(open)
    val langState by rememberUpdatedState(onLanguage)
    DisposableEffect(session) {
        session.onOpen = { openState(it) }
        session.onLanguage = { langState() }
        onDispose { session.onOpen = {}; session.onLanguage = {} }
    }
    QuranPrefs.ensure()
    val s = LocalStrings.current
    val lang = LocalLang.current
    val ht = HomeText.of(lang.code)
    val wt = WidgetText.of(lang.code)
    val dark = LocalDark.current
    // شريط الحالة: حروف داكنة فوق الترويسة الفاتحة، وفاتحة في الوضع الداكن
    StatusBarStyle(lightContent = dark)

    // الحالة تُبنى كل دقيقة (والصفحة ظاهرة)؛ العدّ بالثواني في Swift
    var minute by remember { mutableIntStateOf(0) }
    val active = LocalPageActive.current
    // تُعاد مع العودة إلى الصفحة في الإطار نفسه، فلا تُرسل صلاة قادمة قديمة
    val now = remember(minute, active, AppSettings.place) { PrayerUtil.now() }
    val today = now.date
    val next = remember(minute, active, AppSettings.place, AppSettings.method, AppSettings.hanafi) { PrayerUtil.next() }
    // الأوقات لحظات مطلقة (epoch) في منطقة الموقع، لا تتعلق بوقت إعادة البناء
    val zone = remember(AppSettings.place) { runCatching { TimeZone.of(AppSettings.place.tz) }.getOrElse { TimeZone.currentSystemDefault() } }
    val nextEpoch = next.at.toInstant(zone).epochSeconds
    val nextAt by rememberUpdatedState(nextEpoch * 1000)
    LaunchedEffect(active) {
        while (active) {
            minute++
            val ms = Clock.System.now().toEpochMilliseconds()
            val toMinute = 60_000 - ms % 60_000 + 50
            // القادمة تتبدّل عند أذانها لا عند الدقيقة التالية (وتُهمل إن مضت ولم تُحدَّث بعد)
            val toAdhan = nextAt - ms + 50
            delay(if (toAdhan in 1 until toMinute) toAdhan else toMinute)
        }
    }
    val times = remember(today, AppSettings.place, AppSettings.method, AppSettings.hanafi) { PrayerUtil.times(today) }
    val quran by produceState<Array<String>?>(null) { value = runCatching { QuranData.uthmani() }.getOrNull() }
    val hadith by produceState<com.osatks.mizan.hadith.Hadith?>(null, today, lang) {
        value = withContext(Dispatchers.Default) { runCatching { Hadiths.forDay(today, lang.code) }.getOrNull() }
    }

    BoxWithConstraints(modifier) {
        val g = gutters(maxWidth, 1040.dp, LocalLeadingInset.current)
        val cs = MaterialTheme.colorScheme
        val accent = LocalAccent.current
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + LocalBottomInset.current + 20.dp

        val prayers = PrayTimes.PRAYERS.filter { !times[it].isNaN() }.map { k ->
            linkedMapOf<String, Any?>(
                "key" to k, "name" to s.prayer(k), "time" to s.time(times[k]),
                "start" to PrayerUtil.at(today, times[k]).toInstant(zone).epochSeconds,
            )
        }
        val window = remember(today, now.hour, now.minute / 10, AppSettings.place, AppSettings.method) {
            runCatching {
                AdhkarClock.range(today.plusDays(-1), 2, { d -> PrayerUtil.times(d) }, { d -> Hijri.toHijri(d) })
                    .firstOrNull { now >= it.start && now < it.end }
            }.getOrNull()
        }
        val adhkar = window?.let {
            linkedMapOf<String, Any?>(
                "title" to WidgetData.title(it, lang, wt, com.osatks.mizan.adhkar.Adhkar.tr(lang)), "section" to it.section,
            )
        }
        val h = Hijri.toHijri(today)
        val last = QuranPrefs.last
        val quranCard: Map<String, Any?> = if (last >= 0) {
            val (sn, an) = QuranMeta.locate(last)
            val focus = QuranPrefs.focusLast
            linkedMapOf(
                "kind" to "continue", "label" to s.continueReading, "title" to s.ayahRef(last),
                "sub" to "${s.juzName(QuranMeta.juzOf(last))} · ${s.pageName(QuranMeta.pageOf(last))}",
                "arabic" to QuranMeta.NAMES_AR[sn - 1], "route" to surahRoute(sn, an, "continue"),
                "focusLabel" to if (focus >= 0) s.continueFocus else null,
                "focusRoute" to if (focus >= 0) QuranMeta.locate(focus).let { (fs, fa) -> surahRoute(fs, fa, "focus") } else null,
            )
        } else linkedMapOf(
            "kind" to "start", "label" to s.startReading, "title" to s.quranTitle, "sub" to "",
            "arabic" to "ٱلْقُرْءَانُ", "route" to "quran", "focusLabel" to null, "focusRoute" to null,
        )
        val ayah = quran?.let { text ->
            val idx = ayahOfDay(text, today)
            val (sn, an) = QuranMeta.locate(idx)
            linkedMapOf<String, Any?>(
                "text" to QuranData.withoutBismillah(text, idx), "ref" to s.surahAyah(s.surahName(sn), an),
                "route" to surahRoute(sn, an, "tafsir"),
            )
        }
        val hadithCard = hadith?.let { hd ->
            val main = hd.tr ?: hd.ar
            linkedMapOf<String, Any?>(
                "ar" to hd.ar.title, "tr" to hd.tr?.title,
                "meta" to listOf(main.grade, main.takhrij).filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
        val json = Json.write(
            linkedMapOf<String, Any?>(
                "rtl" to lang.rtl, "lang" to lang.code, "dark" to dark,
                "top" to top.value.toDouble(), "bottom" to bottom.value.toDouble(),
                "start" to g.start.value.toDouble(), "end" to g.end.value.toDouble(),
                "width" to (maxWidth - g.start - g.end).value.toDouble(),
                "appName" to s.appName, "tagline" to s.appTagline, "search" to s.searchHint, "language" to s.language,
                "prayersTitle" to wt.prayerTimes, "place" to s.placeName(AppSettings.place), "daily" to ht.daily,
                "sinceAdhan" to ht.sinceAdhan, "ayahTitle" to s.ayahOfDay, "tafsir" to ht.tafsir,
                "hadithTitle" to ht.hadithOfDay, "explanation" to ht.explanation,
                "day" to linkedMapOf(
                    "weekday" to s.weekday(today),
                    "hMonth" to (h?.let { s.hijriMonths[it.m - 1] } ?: ""), "hDay" to (h?.d?.toString() ?: ""), "hYear" to (h?.y?.toString() ?: ""),
                    "gMonth" to s.gregMonths[today.monthNumber - 1], "gDay" to today.dayOfMonth.toString(), "gYear" to today.year.toString(),
                ),
                "adhkar" to adhkar,
                "next" to linkedMapOf(
                    "key" to next.key, "name" to s.prayer(next.key),
                    "at" to nextEpoch, "today" to (next.at.date == today),
                ),
                "prayers" to prayers,
                "tools" to listOf("adhkar", "tasbih", "qibla", "prayer").map { k -> linkedMapOf("key" to k, "name" to s.tools.getValue(k).name) },
                "quran" to quranCard, "ayah" to ayah, "hadith" to hadithCard,
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
            factory = { NativeHome.make(session) },
            modifier = Modifier.fillMaxSize(),
            // اللمس والتمرير إلى SwiftUI مباشرة، كما في تطبيقات النظام
            properties = UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = true,
            ),
        )
    }
}
