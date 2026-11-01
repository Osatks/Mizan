package com.osatks.mizan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.theme.LocalDark
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIViewController

/**
 * جسر شريط التبويبات من النظام في iOS 26 فما بعده (iosApp/iosApp/LiquidTabBar.swift): UITabBarController نفسه
 * بزجاج Liquid Glass (الكبسولة العائمة، وعدسة الاختيار التي تنساب بين التبويبات وتتبع الإصبع) فوق واجهة Compose
 * بدل الشريط المرسوم بـ Compose. التنقل كله يبقى في Compose (NavHost): Kotlin يرسل حالة الشريط JSON ([tabBarStateJson])
 * مع الإشعار [CHANGED]، و Swift يخبره بلمس تبويب ([select]) وبارتفاع الشريط ([reportHeight]).
 * لا يعمل إلا إذا سجّله Swift ([attach]) في iOS 26 فما بعده؛ قبل ذلك (وفي أندرويد) شريط Compose كما هو.
 */
object NativeTabBar {
    /** الإشعار مع كل تغيّر في الحالة (والحالة JSON في object) */
    const val CHANGED = "MizanTabBarChanged"

    private var factory: (() -> UIViewController)? = null

    /** شريط النظام مثبّت فوق Compose: لا يُرسم شريط Compose السفلي */
    internal var active by mutableStateOf(false)
        private set

    /** يستدعيه Swift مرة عند التشغيل في iOS 26 فما بعده (قبل MainViewController): ينشئ متحكم الشريط عند الطلب */
    fun attach(makeOverlay: () -> UIViewController) {
        factory = makeOverlay
    }

    /** متحكم الشريط، يضعه المتحكم الجذر فوق Compose قبل أول تركيب لها؛ null إن لم يسجّله Swift */
    internal fun makeOverlay(): UIViewController? = factory?.invoke()?.also { active = true }

    /**
     * يعيد شريط Compose: يستدعيه Swift إن لم يجد شريط النظام أسفل الشاشة أو لم يفهم الحالة،
     * فلا يبقى التطبيق بلا شريط تنقل أبداً
     */
    fun fallBack() {
        active = false
    }

    /** آخر حالة أُرسلت (JSON)، يقرؤها الشريط حين يُنشأ */
    var state: String = ""
        private set

    internal var onSelect: ((String) -> Unit)? = null

    /** لمس تبويب في شريط النظام */
    fun select(route: String) {
        onSelect?.invoke(route)
    }

    /** ارتفاع الشريط فوق المنطقة الآمنة السفلية؛ قبل أن يقيسه Swift: ارتفاع شريط النظام المعتاد */
    internal var height by mutableStateOf(49.dp)
        private set

    /** يقيسه Swift بعد كل تخطيط للشريط (بالنقاط، وهي dp في Compose على iOS) */
    fun reportHeight(points: Double) {
        if (points > 0) height = points.toFloat().dp
    }

    /** يُرسل الحالة إن تغيرت (على الخيط الرئيسي: من SideEffect؛ و Swift يطبّقها فوراً) */
    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(CHANGED, json)
    }
}

/**
 * [TabBarHost] حين يثبّت Swift شريط النظام: لا يُرسم شريط Compose السفلي بل تُرسل حالته، والمساحة أسفل صفحات
 * التبويبات من ارتفاع شريط النظام. القائمة الجانبية (iPad بالعرض والنوافذ الواسعة) من Compose كما هي.
 */
internal object NativeTabBarHost : TabBarHost {
    @Composable
    override fun Bottom(visible: Boolean, current: String?, modifier: Modifier, onSelect: (String) -> Unit) {
        val s = LocalStrings.current
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val dark = LocalDark.current
        val accent = MaterialTheme.colorScheme.primary
        // ستار تبديل اللغة يغطي Compose لا شريط النظام فوقها: يختفي الشريط معه ويعود بالأسماء الجديدة
        val json = tabBarStateJson(s, current, visible && !LocalLanguageVeil.current, rtl, dark, accent)
        val select by rememberUpdatedState(onSelect)
        // إن خرج الشريط من التركيب (عودة شريط Compose) يختفي شريط النظام
        val hidden by rememberUpdatedState(tabBarStateJson(s, current, false, rtl, dark, accent))
        DisposableEffect(Unit) {
            NativeTabBar.onSelect = { select(it) }
            onDispose {
                NativeTabBar.onSelect = null
                NativeTabBar.publish(hidden)
            }
        }
        SideEffect { NativeTabBar.publish(json) }
    }

    @Composable
    override fun Rail(current: String?, labels: Boolean, onSelect: (String) -> Unit) = ComposeTabBar.Rail(current, labels, onSelect)

    /** المسافة نفسها فوق الشريط التي يتركها شريط Compose في iOS (92 = 52 ارتفاعه فوق المنطقة الآمنة + 40) */
    override val bottomInset: Dp get() = NativeTabBar.height + 40.dp

    override val leadingInset: Dp get() = ComposeTabBar.leadingInset
}
