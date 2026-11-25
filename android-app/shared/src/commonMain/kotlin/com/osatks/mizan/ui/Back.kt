package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * الصفحة الظاهرة في أعلى التنقل: لها وحدها زر الرجوع وسحب الحافة ومظهر شريط الحالة.
 * الصفحات الباقية حية تحتها (متحكمات UINavigationController في iOS) تعمل بلا شيء من ذلك حتى تعود إلى الأعلى.
 */
val LocalPageActive = compositionLocalOf { true }

/**
 * iOS: رجوع داخل الصفحة نفسها (صفحة فرعية، وضع التركيز، لوحة الآية) يقوده سحب الحافة من UIKit لا من Compose،
 * فلا يتنازع سحبان: المتحكم يسأل [active] قبل أن يبدأ، فإن وُجد رجوع داخلي نفّذ [back] بدل الرجوع من الصفحة
 */
class InnerBack {
    private val handlers = mutableListOf<() -> Unit>()

    /** في الصفحة رجوع داخلي الآن */
    val active: Boolean get() = handlers.isNotEmpty()

    /** ينفّذ آخر رجوع سُجِّل؛ false إن لم يكن */
    fun back(): Boolean {
        val h = handlers.lastOrNull() ?: return false
        h()
        return true
    }

    internal fun add(h: () -> Unit) { handlers += h }
    internal fun remove(h: () -> Unit) { handlers.remove(h) }
}

/** يوفّره مضيف كل صفحة في iOS؛ null في أندرويد (رجوع النظام كما هو) */
val LocalInnerBack = staticCompositionLocalOf<InnerBack?> { null }

/** زر الرجوع في أندرويد، وسحب الحافة في iOS: يُنفَّذ onBack حين يكتمل. في الصفحة الظاهرة في الأعلى فقط */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val on = enabled && LocalPageActive.current
    val inner = LocalInnerBack.current
    if (inner != null) {
        val current by rememberUpdatedState(onBack)
        DisposableEffect(inner, on) {
            if (!on) return@DisposableEffect onDispose { }
            val h = { current() }
            inner.add(h)
            onDispose { inner.remove(h) }
        }
        return
    }
    androidx.compose.ui.backhandler.BackHandler(on, onBack)
}
