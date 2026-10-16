package com.osatks.mizan.ui.quran

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.osatks.mizan.core.AppSettings

/**
 * إسكات الإشعارات أثناء القراءة في وضع التركيز: يُفعَّل «عدم الإزعاج» (بأولوياته التي ضبطها المستخدم)
 * إن سمح المستخدم بذلك مرة، ويُعاد كما كان عند الخروج من وضع التركيز أو مغادرة التطبيق.
 * لا يُلغى إلا ما فعّله التطبيق نفسه: إن كان «عدم الإزعاج» مفعّلاً من قبل، أو غيّره المستخدم أثناء القراءة، يُترك كما هو.
 */
object QuietReading {
    private const val PREV = "dnd_prev"     // المرشّح قبل أن نُسكت الإشعارات؛ -1 = لم نُسكتها
    private const val ASKED = "dnd_asked"

    private fun nm(ctx: Context): NotificationManager? = runCatching { ctx.getSystemService(NotificationManager::class.java) }.getOrNull()

    fun granted(ctx: Context) = nm(ctx)?.isNotificationPolicyAccessGranted == true
    fun asked() = AppSettings.getBool(ASKED, false)
    fun markAsked() = AppSettings.putBool(ASKED, true)

    fun requestAccess(ctx: Context) {
        markAsked()
        runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun silence(ctx: Context) {
        if (!granted(ctx) || AppSettings.getInt(PREV, -1) != -1) return
        val nm = nm(ctx) ?: return
        if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        AppSettings.putInt(PREV, NotificationManager.INTERRUPTION_FILTER_ALL)
        runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
    }

    /** يُستدعى أيضاً عند بدء التطبيق، فلا يبقى «عدم الإزعاج» مفعّلاً إن أُغلق التطبيق أثناء القراءة */
    fun restore(ctx: Context) {
        val prev = AppSettings.getInt(PREV, -1)
        if (prev == -1) return
        AppSettings.putInt(PREV, -1)
        if (!granted(ctx)) return
        val nm = nm(ctx) ?: return
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) runCatching { nm.setInterruptionFilter(prev) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * آثار وضع التركيز على النظام: إخفاء شريط الإشعارات وشريط التنقل (يظهران مؤقتاً بالسحب من الحافة)،
 * وإبقاء الشاشة مضاءة، وإسكات الإشعارات. كل ذلك يُعاد كما كان عند الخروج، ويُعلَّق الإسكات حين يغادر المستخدم التطبيق.
 */
@Composable
fun FocusEffects(active: Boolean) {
    val view = LocalView.current
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(active, view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        fun enter() {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = true
            QuietReading.silence(ctx)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (!active) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> enter()
                Lifecycle.Event.ON_PAUSE -> QuietReading.restore(ctx)
                else -> Unit
            }
        }
        // يُرسل Lifecycle حدث ON_RESUME فوراً للمراقب الجديد إن كانت الشاشة ظاهرة، فيبدأ الوضع من هناك
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (active) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                view.keepScreenOn = false
                QuietReading.restore(ctx)
            }
        }
    }
}
