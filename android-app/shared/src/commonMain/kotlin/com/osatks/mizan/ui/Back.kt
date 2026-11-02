package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * الصفحة الظاهرة في أعلى التنقل: لها وحدها زر الرجوع وسحب الحافة ومظهر شريط الحالة.
 * الصفحات الباقية حية تحتها (StackNavHost في iOS) تعمل بلا شيء من ذلك حتى تعود إلى الأعلى.
 */
val LocalPageActive = compositionLocalOf { true }

/** زر الرجوع في أندرويد، وسحب الحافة في iOS: يُنفَّذ onBack حين يكتمل. في الصفحة الظاهرة في الأعلى فقط */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    androidx.compose.ui.backhandler.BackHandler(enabled && LocalPageActive.current, onBack)
}

/** تقدّم سحب الرجوع (0 إلى 1)، ومن أي حافة بدأ: الصفحة تتبع الإصبع أياً كانت لغة الجهاز */
class BackSwipe(val progress: Float, val fromLeft: Boolean)

/**
 * رجوع يتبع الإصبع، كما في PredictiveBackHandler: انتهاء التدفق طبيعياً = اكتمل الرجوع، واستثناء الإلغاء = تراجع المستخدم.
 * في iOS يأتي من سحب الحافة (اليسرى أو اليمنى)، وفي أندرويد من رجوع النظام التنبؤي.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PredictiveBack(enabled: Boolean = true, onBack: suspend (progress: Flow<BackSwipe>) -> Unit) {
    androidx.compose.ui.backhandler.PredictiveBackHandler(enabled && LocalPageActive.current) { events ->
        // swipeEdge: 0 الحافة اليسرى (EDGE_LEFT)، 1 اليمنى
        onBack(events.map { BackSwipe(it.progress, it.swipeEdge == 0) })
    }
}
