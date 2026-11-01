package com.osatks.mizan.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.osatks.mizan.platform.systemLanguageTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.ui.theme.HeroBrush
import com.osatks.mizan.ui.theme.Gold

/**
 * ستار تبديل اللغة نازل (من بدء إسداله حتى بدء رفعه): ما يُرسم خارج Compose فوقها
 * (شريط التبويبات من النظام في iOS 26) يختفي معه ثم يعود بالأسماء الجديدة
 */
val LocalLanguageVeil = compositionLocalOf { false }

/**
 * يوفّر نصوص اللغة واتجاه الكتابة. عند تبديل اللغة يُسدل ستار بلون ميزان عليه النجمة واسم اللغة الجديدة،
 * فتتبدل الواجهة (وقد ينقلب اتجاهها) خلفه ثم ينكشف، بدل القفزة المفاجئة.
 */
@Composable
fun LanguageHost(content: @Composable () -> Unit) {
    val target = Lang.resolve(AppSettings.lang, systemLanguageTag())
    var shown by remember { mutableStateOf(target) }
    val veil = remember { Animatable(0f) }
    val veiled by remember { derivedStateOf { veil.targetValue > 0f } }

    LaunchedEffect(target) {
        if (target != shown) {
            veil.animateTo(1f, tween(260, easing = Motion.Accelerate))
            shown = target
            PrayerAlerts.languageChanged()
            withFrameNanos { }
            withFrameNanos { }
        }
        // يُرفع الستار دائماً، حتى لو عاد المستخدم إلى اللغة السابقة قبل اكتمال الحركة
        if (veil.value > 0f) veil.animateTo(0f, tween(520, easing = Motion.Decelerate))
    }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalStrings provides shown.strings,
            LocalLang provides shown,
            LocalLayoutDirection provides if (shown.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
            LocalLanguageVeil provides veiled,
        ) { content() }

        if (veil.value > 0f) {
            val p = veil.value
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = p }.background(HeroBrush),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer { val s = 0.85f + 0.15f * p; scaleX = s; scaleY = s }
                ) {
                    SlowStar(Modifier.size(120.dp), Gold.copy(alpha = 0.9f), periodMs = 2400, stroke = 5f)
                    Spacer(Modifier.height(20.dp))
                    Text(target.native, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(target.strings.appName, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
