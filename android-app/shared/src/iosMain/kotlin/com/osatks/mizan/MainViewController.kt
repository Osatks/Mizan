package com.osatks.mizan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalHapticFeedback
import com.osatks.mizan.ui.IosHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.platform.IosToast
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.LanguageHost
import com.osatks.mizan.ui.theme.MizanTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

/** نقطة دخول iOS: يستدعيها iOSApp.swift */
@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController {
    AppSettings.init()
    // الإشعارات المجدولة لأيام قادمة تُجدَّد عند كل فتح (لا تعمل منبّهات في الخلفية في iOS)،
    // بعد أن تظهر الواجهة: حساب أسبوع من المواقيت و35 طلب إشعار لا ينتظرها أول إطار
    MainScope().launch { delay(1200); PrayerAlerts.reschedule() }
    return ComposeUIViewController(configure = {
        // ترميز أوامر الرسم لـ Metal على خيط مستقل، فيتفرغ الخيط الرئيسي للتركيب واللمس في كل إطار
        parallelRendering = true
        // سحب الحافة للرجوع (كتطبيقات النظام): يقود انتقال الرجوع في NavHost مع حركة الإصبع
        enableBackGesture = true
    }) {
        // اهتزازات بمولّدات UIKit لكل نوع ما يناسبه (التبويبات، العدّادات، المفاتيح)
        CompositionLocalProvider(LocalHapticFeedback provides IosHaptics) {
            LanguageHost {
                MizanTheme {
                    Box(Modifier.fillMaxSize()) {
                        MizanApp()
                        ToastOverlay()
                    }
                }
            }
        }
    }
}

/** بديل Toast: رسالة قصيرة أسفل الشاشة تختفي وحدها */
@androidx.compose.runtime.Composable
private fun androidx.compose.foundation.layout.BoxScope.ToastOverlay() {
    val msg = IosToast.message
    LaunchedEffect(msg) { if (msg != null) { delay(2600); IosToast.message = null } }
    AnimatedVisibility(msg != null, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 110.dp, start = 24.dp, end = 24.dp), enter = fadeIn(), exit = fadeOut()) {
        Text(
            msg ?: "", color = Color.White, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.background(Color(0xE6222222), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 12.dp)
        )
    }
}
