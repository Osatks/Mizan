package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import platform.UIKit.UIViewController

/**
 * شريط الحالة في iOS: كل صفحة متحكم خاص بها (PageController في IosNav.kt) يجيب النظام عن شريطها من طلباتها هي
 * ([LocalBarRequests])، والحاويات (التبويبات والتنقل) تحيل السؤال إلى الصفحة الظاهرة.
 */
internal object IosStatusBar {
    /** المتحكم الجذر للنافذة: تُعرض فوقه الأوراق والتنبيهات (يضبطه IosNav) */
    var controller: UIViewController? = null
}

/** لا شيء على مستوى التطبيق في iOS: كل صفحة تطبّق طلباتها (PageController) */
@Composable
actual fun ApplySystemBars() {
}
