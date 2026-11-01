package com.osatks.mizan.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

actual object AppStore {
    private fun open(url: String) {
        val u = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(u, emptyMap<Any?, Any>(), null)
    }

    /** صفحة ميزان في تطبيق الإعدادات (الموقع والإشعارات) */
    actual fun openSettings() = open(UIApplicationOpenSettingsURLString)

    /** صفحة كتابة مراجعة في App Store إن عُرف رقم التطبيق، وإلا نافذة التقييم من النظام */
    actual fun openRating() {
        val review = StoreLinks.appStoreReview
        if (review != null) open(review) else requestStoreReview()
    }
}

/** نافذة التقييم من النظام في المشهد الظاهر (iOS يقرر عرضها، ولا تظهر في TestFlight) */
internal fun requestStoreReview() {
    val scene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { (it as? UIWindowScene)?.activationState == UISceneActivationStateForegroundActive } as? UIWindowScene
    if (scene != null) SKStoreReviewController.requestReviewInScene(scene)
    else @Suppress("DEPRECATION") SKStoreReviewController.requestReview()
}

@Composable
actual fun rememberReviewRequest(): () -> Unit = remember { { requestStoreReview() } }
