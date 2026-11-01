package com.osatks.mizan.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory

actual object AppStore {
    private val ctx: Context get() = AndroidContext.app

    actual fun openSettings() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** صفحة ميزان في تطبيق Google Play، أو في المتصفح إن لم يكن المتجر مثبتاً */
    actual fun openRating() {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${StoreLinks.PLAY_ID}"))
            .setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { ctx.startActivity(market) }.isSuccess) return
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(StoreLinks.PLAY)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** In-App Review من Google Play: لا يظهر شيء إن لم يُثبَّت التطبيق من المتجر أو استُنفدت حصته، ولا يُعرف ذلك */
@Composable
actual fun rememberReviewRequest(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            val activity = context.activity()
            if (activity != null) runCatching {
                val manager = ReviewManagerFactory.create(activity)
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful && !activity.isFinishing) manager.launchReviewFlow(activity, task.result)
                }
            }
        }
    }
}
