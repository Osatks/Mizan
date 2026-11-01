package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertActionStyleDestructive
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/**
 * iOS: نافذة تنبيه النظام نفسها (UIAlertController)، فتأتي بزجاج iOS 26 وحركته ولمساته كما في تطبيقات النظام
 * حين يُبنى التطبيق بـ SDK iOS 26 فما بعده. الإلغاء والحذف بأسلوبي النظام، والمفضَّل عريض (preferredAction).
 * تُعرض ما دامت في التركيب؛ إن خرجت منه قبل لمس زر (انتقال إلى شاشة أخرى) تُغلق.
 */
@Composable
actual fun PlatformDialog(onDismissRequest: () -> Unit, title: String, message: String?, actions: List<DialogAction>) {
    val latest = rememberUpdatedState(actions)
    DisposableEffect(title, message, actions.size) {
        val alert = UIAlertController.alertControllerWithTitle(title, message, UIAlertControllerStyleAlert)
        var answered = false
        var cancelUsed = false
        actions.forEachIndexed { i, a ->
            // لا يقبل النظام إلا زر إلغاء واحداً
            val style = when {
                a.role == ActionRole.Cancel && !cancelUsed -> { cancelUsed = true; UIAlertActionStyleCancel }
                a.role == ActionRole.Destructive -> UIAlertActionStyleDestructive
                else -> UIAlertActionStyleDefault
            }
            val action = UIAlertAction.actionWithTitle(a.text, style) { _ ->
                answered = true
                latest.value.getOrNull(i)?.onClick?.invoke()
            }
            alert.addAction(action)
            if (a.role == ActionRole.Preferred) alert.preferredAction = action
        }
        topController()?.presentViewController(alert, true, null)
        onDispose {
            if (!answered && alert.presentingViewController != null) alert.dismissViewControllerAnimated(true, null)
        }
    }
}

/** أعلى متحكم ظاهر الآن: يُعرض التنبيه فوقه (فوق ورقة المشاركة مثلاً إن كانت ظاهرة) */
private fun topController(): UIViewController? {
    var top: UIViewController = IosStatusBar.controller ?: UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}
