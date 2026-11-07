package com.osatks.mizan.platform

import androidx.compose.runtime.Composable

/** روابط ميزان في المتجرين: المشاركة تشير إلى متجر النظام الذي يعمل عليه التطبيق */
object StoreLinks {
    const val PLAY_ID = "com.mizan.almuslim"
    const val PLAY = "https://play.google.com/store/apps/details?id=$PLAY_ID"

    /**
     * رقم ميزان في App Store (Apple ID، أرقام فقط): يظهر في App Store Connect › التطبيق › App Information › Apple ID
     * بعد إنشاء سجل التطبيق، وهو الرقم نفسه في رابطه apps.apple.com/app/id<الرقم>.
     * ما دام فارغاً تشير المشاركة في iOS إلى صفحة التنزيل في موقع ميزان، ويطلب صف «قيّم ميزان» نافذة التقييم من النظام.
     */
    const val APP_STORE_ID = "6829457007"

    /** صفحة التنزيل في موقع ميزان (روابط المتجرين) */
    const val SITE_DOWNLOAD = "https://mizan-al-muslem.com/app/"

    val appStore: String get() = if (APP_STORE_ID.isEmpty()) SITE_DOWNLOAD else "https://apps.apple.com/app/id$APP_STORE_ID"

    /** صفحة كتابة مراجعة في App Store (تحتاج رقم التطبيق) */
    val appStoreReview: String? get() = if (APP_STORE_ID.isEmpty()) null else "https://apps.apple.com/app/id$APP_STORE_ID?action=write-review"

    val forThisPlatform: String get() = if (Platform.name == "ios") appStore else PLAY
}

/** المتجر وإعدادات النظام في كل نظام */
expect object AppStore {
    /** صفحة ميزان في إعدادات النظام (الأذونات)، حين رفض المستخدم إذناً من قبل */
    fun openSettings()

    /** صف «قيّم ميزان» بطلب المستخدم: صفحة التطبيق في المتجر */
    fun openRating()
}

/**
 * نافذة التقييم داخل التطبيق (SKStoreReviewController في iOS، و In-App Review في Google Play).
 * النظام يقرر عرضها وعدد مراتها، ولا تُطلب إلا من [ReviewPrompt] بعد استعمال حقيقي.
 */
@Composable
expect fun rememberReviewRequest(): () -> Unit
