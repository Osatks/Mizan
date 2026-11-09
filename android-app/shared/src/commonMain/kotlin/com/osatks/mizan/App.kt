package com.osatks.mizan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.ui.TABS
import com.osatks.mizan.ui.quran.SurahScreen

/**
 * التطبيق كله في أندرويد (MizanApp في App.android.kt): التنقل بين الشاشات، وشريط التبويبات، وجولة أول تشغيل.
 * في iOS التنقل من UIKit نفسه (UITabBarController و UINavigationController، MizanNavigation.swift و IosNav.kt):
 * كل صفحة متحكم خاص بها يعرض [PageContent] لمسارها.
 */
/** نسخة دليل التطبيق: تُرفع حين تضاف صفحات جديدة فيراها من حدّث التطبيق مرة واحدة */
internal const val GUIDE_VERSION = 2
/** صفحات الجولة الأساسية (Strings.onboarding) قبل صفحات الدليل الجديدة */
internal const val BASE_GUIDE_PAGES = 5

/** جولة أول تشغيل (من أولها)، أو صفحات الدليل الجديدة بعد التحديث (من [BASE_GUIDE_PAGES])؛ null إن رآها المستخدم */
internal fun pendingGuide(): Int? = when {
    !AppSettings.getBool("onboarded", false) -> 0
    AppSettings.getInt("guideSeen", 0) < GUIDE_VERSION -> BASE_GUIDE_PAGES
    else -> null
}

/** انتهت الجولة: لا تظهر مرة أخرى حتى تُرفع [GUIDE_VERSION] */
internal fun guideDone() {
    AppSettings.putBool("onboarded", true)
    AppSettings.putInt("guideSeen", GUIDE_VERSION)
}

/** مسار يُفتح من رابط (أداة، اختصار، إشعار، وسيط تشغيل) */
internal fun isRoute(route: String): Boolean = route in ROUTES || route.startsWith("surah/")

internal val ROUTES = setOf("home", "prayer", "qibla", "tasbih", "settings", "zakat", "fitr", "inheritance", "hijri", "age", "events", "fidya", "search", "quran", "tafsirs", "guide", "adhkar", "adhan", "licenses", "hadith", "tools")
internal val TAB_ROUTES = TABS.map { it.route }.toSet()

/**
 * التنقل كما تراه الشاشات: في أندرويد NavHost (رجوع النظام التنبؤي والعناصر المشتركة بين الصفحات)،
 * وفي iOS جسر إلى منسّق UIKit (IosAppNav في IosNav.kt).
 */
@Stable
internal interface AppNav {
    fun open(route: String)
    fun back()
    fun goTab(route: String)
    /** رابط من أداة أو اختصار أو إشعار: يُغلق ما فوق التبويب ثم يفتح الشاشة */
    fun deepLink(route: String)
    /** نتيجة من البحث تحل محله */
    fun searchResult(route: String)
}


/**
 * تسجيل حركة التنقل من المحاكي (fastlane/screenshots.sh، الوسيط «-mizan.motion_script»): خطوات بلا لمس تُنفَّذ بالترتيب،
 * مفصولة بفواصل: wait:<ms>، open:<route>، back، tab:<route>، at:<x>:<y>. لفحص سلاسة الانتقالات في فيديو؛ لا يصل إليه المستخدم
 * (وسائط التشغيل لا تُحفظ في الجهاز).
 */
object MotionScript {
    internal var open: ((String) -> Unit)? = null
    internal var back: (() -> Unit)? = null
    internal var tab: ((String) -> Unit)? = null
    /** at:<x>:<y> (كسور من عرض الشاشة وارتفاعها): موضع لمس وهمي تنمو منه الصفحة التالية، كأن الإصبع لمس هناك */
    internal var touch: ((Double, Double) -> Unit)? = null

    suspend fun run(script: String) {
        for (step in script.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val name = step.substringBefore(':')
            val arg = step.substringAfter(':', "")
            when (name) {
                "wait" -> kotlinx.coroutines.delay(arg.toLongOrNull() ?: 0L)
                "open" -> open?.invoke(arg)
                "back" -> back?.invoke()
                "tab" -> tab?.invoke(arg)
                "at" -> {
                    val x = arg.substringBefore(':').toDoubleOrNull()
                    val y = arg.substringAfter(':', "").toDoubleOrNull()
                    if (x != null && y != null) touch?.invoke(x, y)
                }
            }
        }
    }
}


/** الشاشة لمسار واحد بلا إطار التنقل حولها (iOS: محتوى متحكم الصفحة) */
@Composable
internal fun PageContent(route: String, app: AppNav) {
    val back: () -> Unit = remember(app) { { app.back() } }
    val open: (String) -> Unit = remember(app) { { r -> app.open(r) } }
    Route(route, app, open, back)
}

/** الشاشة لكل مسار (المسارات في [ROUTES]، و surah/… بمعاملاتها): صفحة السورة مشتركة، وما سواها من [PlatformRoute] */
@Composable
internal fun Route(route: String, app: AppNav, open: (String) -> Unit, back: () -> Unit) {
    if (route.startsWith("surah/")) {
        // surah/12?ayah=5&from=row
        val path = route.substringBefore('?')
        val query = route.substringAfter('?', "").split('&').mapNotNull { kv ->
            kv.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
        SurahRoute(path.removePrefix("surah/").toIntOrNull() ?: 1, query["ayah"]?.toIntOrNull() ?: 0, query["from"] ?: "row", open, back)
    } else PlatformRoute(route, app, open, back)
}

/**
 * شاشات المسارات في [ROUTES]: في أندرويد شاشات Compose (PlatformRoute.android.kt)، وفي iOS واجهة النظام
 * (PlatformRoute.ios.kt). مسار غير معروف لا يعرض شيئاً
 */
@Composable
internal expect fun PlatformRoute(route: String, app: AppNav, open: (String) -> Unit, back: () -> Unit)

@Composable
internal fun SurahRoute(surah: Int, ayah: Int, from: String, open: (String) -> Unit, back: () -> Unit) {
    SurahScreen(
        surah = surah.coerceIn(1, 114),
        startAyah = ayah,
        from = from,
        onBack = back,
        openSurah = { },
        startInFocus = from == "focus",
        openLibrary = { open("tafsirs") },
    )
}
