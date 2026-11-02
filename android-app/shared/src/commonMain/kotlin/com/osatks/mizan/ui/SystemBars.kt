package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState

/**
 * مظهر شريط الحالة: كل شاشة تطلب ما يناسب ما تحته (نص فاتح فوق الرؤوس الخضراء، وداكن فوق السطوح الفاتحة)،
 * وآخر شاشة دخلت التركيب هي التي تُتّبع. بلا طلب يتبع الشريط مظهر التطبيق (فاتح في الوضع الداكن).
 */
object SystemBars {
    internal class Request(val light: State<Boolean>, val hidden: State<Boolean>)

    private val requests = mutableStateListOf<Request>()

    /** نص الشريط فاتح؟ null = حسب مظهر التطبيق */
    val lightContent: Boolean? get() = requests.lastOrNull()?.light?.value

    /** الشريط مخفي (وضع التركيز في المصحف) */
    val hidden: Boolean get() = requests.lastOrNull()?.hidden?.value == true

    internal fun add(r: Request) { requests += r }
    internal fun remove(r: Request) { requests -= r }
}

/**
 * يطلب مظهر شريط الحالة ما دامت الشاشة ظاهرة في الأعلى (الصفحات الحية تحتها في iOS لا تطلب شيئاً حتى تعود،
 * فيُسجَّل طلبها من جديد آخرَ الطلبات)
 */
@Composable
fun StatusBarStyle(lightContent: Boolean, hidden: Boolean = false) {
    val light = rememberUpdatedState(lightContent)
    val hide = rememberUpdatedState(hidden)
    val active = LocalPageActive.current
    DisposableEffect(active) {
        if (!active) return@DisposableEffect onDispose { }
        val r = SystemBars.Request(light, hide)
        SystemBars.add(r)
        onDispose { SystemBars.remove(r) }
    }
}

/** يطبّق [SystemBars] على النظام؛ يُستدعى مرة داخل MizanTheme */
@Composable
expect fun ApplySystemBars()
