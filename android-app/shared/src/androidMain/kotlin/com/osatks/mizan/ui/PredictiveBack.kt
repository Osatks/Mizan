package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** تقدّم سحب الرجوع (0 إلى 1)، ومن أي حافة بدأ: الصفحة تتبع الإصبع أياً كانت لغة الجهاز */
class BackSwipe(val progress: Float, val fromLeft: Boolean)

/**
 * رجوع يتبع الإصبع، كما في PredictiveBackHandler: انتهاء التدفق طبيعياً = اكتمل الرجوع، واستثناء الإلغاء = تراجع المستخدم.
 * في أندرويد من رجوع النظام التنبؤي؛ وفي iOS رجوع داخلي ([InnerBack]) بلا تقدّم، تكمله حركة الصفحة نفسها.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PredictiveBack(enabled: Boolean = true, onBack: suspend (progress: Flow<BackSwipe>) -> Unit) {
    if (LocalInnerBack.current != null) {
        val scope = rememberCoroutineScope()
        val current by rememberUpdatedState(onBack)
        BackHandler(enabled) { scope.launch { current(emptyFlow()) } }
        return
    }
    androidx.compose.ui.backhandler.PredictiveBackHandler(enabled && LocalPageActive.current) { events ->
        // swipeEdge: 0 الحافة اليسرى (EDGE_LEFT)، 1 اليمنى
        onBack(events.map { BackSwipe(it.progress, it.swipeEdge == 0) })
    }
}
