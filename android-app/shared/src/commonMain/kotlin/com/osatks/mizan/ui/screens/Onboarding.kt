package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.core.Place
import com.osatks.mizan.platform.LocationResult
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.platform.PrayerAlerts
import com.osatks.mizan.platform.rememberLocationRequest
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.i18n.placeName
import com.osatks.mizan.ui.SlowStar
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** أيقونة كل صفحة من الجولة */
private val PAGE_ICONS = listOf(Icons.Filled.Star, Icons.Filled.Explore, Icons.AutoMirrored.Filled.MenuBook, Icons.Filled.Calculate, Icons.Filled.Search)

/** أيقونات صفحات الدليل الجديدة (GuideText.pages) بترتيبها لكل نظام */
private fun guideIcons(ios: Boolean) = buildList {
    add(Icons.Filled.Widgets); add(Icons.Filled.FormatQuote); add(Icons.Filled.Share)
    if (ios) { add(Icons.Filled.Lock); add(Icons.Filled.TouchApp); add(Icons.Filled.Watch) }
    add(Icons.Filled.NotificationsActive)
}

/**
 * جولة أول تشغيل: صفحات تعرّف بأهم ما في التطبيق وبما قد يفوت المستخدم (GuideText)، تُقلَّب بالسحب أو بزر «التالي»، ويمكن تخطيها.
 * تُعاد من الإعدادات («دليل التطبيق»).
 */
@Composable
fun Onboarding(onDone: () -> Unit, from: Int = 0) {
    // في iOS: الجولة بواجهة النظام (SwiftUI)، أول تشغيل وصفحة «دليل التطبيق» معاً
    if (nativeGuideAvailable) {
        NativeOnboarding(onDone, from)
        return
    }
    val s = LocalStrings.current
    val lang = com.osatks.mizan.i18n.LocalLang.current
    // الصفحات الأساسية، ثم ما قد يفوت المستخدم (الأدوات، وحديث اليوم، والمشاركة قصة، وميزات iPhone، والأذان)
    // [from]: بعد التحديث تبدأ الجولة من الصفحات الجديدة وحدها
    val pages = remember(s, lang, from) { (s.onboarding + com.osatks.mizan.i18n.GuideText.of(lang.code).pages(com.osatks.mizan.ui.IsIos)).drop(from) }
    val icons = remember(from) { (PAGE_ICONS + guideIcons(com.osatks.mizan.ui.IsIos)).drop(from) }
    // الصفحة الأخيرة تطلب الموقع: لا نفترض مكة، فالمواقيت والقبلة تُحسب من موقع المستخدم
    // (ولا تُعرض بعد التحديث لمن اختار موقعه)
    val count = pages.size + if (from > 0 && AppSettings.placeSet) 0 else 1
    val pager = rememberPagerState { count }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == count - 1
    // خلفية خضراء كاملة: نص شريط الحالة فاتح
    com.osatks.mizan.ui.StatusBarStyle(lightContent = true)
    Box(Modifier.fillMaxSize().background(HeroBrush)) {
        // نجمة كبيرة تدور ببطء خلف الصفحات
        SlowStar(Modifier.size(420.dp).align(Alignment.TopEnd).graphicsLayer { translationX = 140f; translationY = -80f }, Color.White.copy(alpha = .06f), 90_000, 4f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                // «تخطي» ينقل إلى صفحة الموقع أولاً، ومنها يُنهي الجولة
                if (!last || !AppSettings.placeSet) TextButton(onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(count - 1) } }) {
                    Text(s.obSkip, color = Color.White.copy(alpha = .85f))
                } else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val (title, body) = pages.getOrNull(i) ?: (s.obLocationTitle to s.obLocationBody)
                // في النوافذ القصيرة (الشاشة الداخلية لـ iPhone Duo بالعرض، أو هاتف بالعرض) تصغر الشارة وتُمرَّر الصفحة،
                // وعلى الشاشات العريضة يبقى النص بعرض مريح في الوسط
                BoxWithConstraints(Modifier.fillMaxSize()) {
                val short = maxHeight < 560.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                        .padding(horizontal = 32.dp).wrapContentWidth().widthIn(max = 560.dp).graphicsLayer {
                        val o = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceAtMost(1f)
                        alpha = 1f - 0.6f * o; scaleX = 1f - 0.08f * o; scaleY = 1f - 0.08f * o
                    },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    // الشارة: نجمة ثمانية ذهبية وفي وسطها أيقونة الصفحة
                    Box(Modifier.size(if (short) 120.dp else 180.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Color.White.copy(alpha = .08f), size.minDimension / 2)
                            drawStar8(Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, Gold.copy(alpha = .9f), 2.5.dp.toPx(), rings = false)
                            drawCircle(Color.White.copy(alpha = .10f), size.minDimension * 0.27f)
                        }
                        Icon(if (i == pages.size) Icons.Filled.Place else icons.getOrElse(i) { Icons.Filled.Star }, null, Modifier.size(if (short) 40.dp else 56.dp), tint = Gold)
                    }
                    Spacer(Modifier.height(if (short) 20.dp else 36.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .85f), textAlign = TextAlign.Center)
                    if (i == pages.size) {
                        Spacer(Modifier.height(28.dp))
                        // الموقع المختار يظهر فوق الزرين بعد تحديده
                        if (AppSettings.placeSet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp), tint = Gold)
                                Spacer(Modifier.width(8.dp))
                                Text(s.placeName(AppSettings.place), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        LocationButtons(onHero = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                }
            }
            // النقاط وزر المتابعة (بعرض أقصاه 640 في الوسط على الشاشات العريضة)
            Row(Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = 640.dp).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until count).forEach { i ->
                        val on = i == pager.currentPage
                        val w by animateDpAsState(if (on) 18.dp else 6.dp, tween(250), label = "dot")
                        val c by animateColorAsState(if (on) Gold else Color.White.copy(alpha = .35f), tween(250), label = "dotColor")
                        Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(c))
                    }
                }
                Button(
                    onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1E1A14))
                ) { Text(if (last) s.obStart else s.obNext, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** مفاتيح أيقونات الصفحات للواجهة الأصلية، بترتيب PAGE_ICONS ثم guideIcons (NativeGuide.swift يحوّلها إلى رموز النظام) */
private val NATIVE_PAGE_ICONS = listOf("star", "explore", "quran", "calc", "search")

private fun nativeGuideIcons(ios: Boolean) = buildList {
    add("widgets"); add("quote"); add("share")
    if (ios) { add("lock"); add("tap"); add("watch") }
    add("adhan")
}

/**
 * الجولة بواجهة النظام (iOS: NativeGuide.swift): الصفحات والصفحة الحالية ومنطق «التالي» و«تخطي» وطلب الموقع هنا كما في
 * جولة Compose و[LocationButtons]، والرسم في Swift. ورقة المدن ونافذة رفض الإذن تبقيان في Compose فوقها.
 */
@Composable
private fun NativeOnboarding(onDone: () -> Unit, from: Int) {
    val s = LocalStrings.current
    val lang = com.osatks.mizan.i18n.LocalLang.current
    val ios = com.osatks.mizan.ui.IsIos
    val base = remember(s, lang, from) { (s.onboarding + com.osatks.mizan.i18n.GuideText.of(lang.code).pages(ios)).drop(from) }
    val keys = remember(from) { (NATIVE_PAGE_ICONS + nativeGuideIcons(ios)).drop(from) }
    // صفحة الموقع الأخيرة، ولا تُعرض بعد التحديث لمن اختار موقعه
    val location = !(from > 0 && AppSettings.placeSet)
    val pages = base.mapIndexed { i, (title, body) -> GuidePage(title, body, keys.getOrElse(i) { "star" }) } +
        if (location) listOf(GuidePage(s.obLocationTitle, s.obLocationBody, "place")) else emptyList()
    val count = pages.size
    var page by remember { mutableStateOf(0) }
    val current = page.coerceIn(0, count - 1)
    val last = current == count - 1
    var locating by remember { mutableStateOf(false) }
    var pickCity by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val locate = rememberLocationRequest { r ->
        locating = false
        when (r) {
            is LocationResult.Found -> { AppSettings.updatePlace(r.place); PrayerAlerts.reschedule(); Platform.toast(s.locationFound) }
            LocationResult.Failed -> Platform.toast(s.locationFailed)
            LocationResult.Denied -> denied = true
        }
    }
    // خلفية خضراء كاملة: نص شريط الحالة فاتح
    com.osatks.mizan.ui.StatusBarStyle(lightContent = true)
    NativeGuideView(
        pages, current, location,
        placeName = if (AppSettings.placeSet) s.placeName(AppSettings.place) else null,
        locating = locating,
        onPage = { page = it.coerceIn(0, count - 1) },
        onNext = { if (last) onDone() else { page = current + 1 } },
        // «تخطي» ينقل إلى الصفحة الأخيرة أولاً، ومنها يُنهي الجولة
        onSkip = { if (last) onDone() else { page = count - 1 } },
        onLocate = { locating = true; locate() },
        onPickCity = { pickCity = true },
        modifier = Modifier.fillMaxSize(),
    )
    if (denied) LocationDeniedDialog(onPickCity = { denied = false; pickCity = true }, onDismiss = { denied = false })
    if (pickCity) CityPickerSheet(AppSettings.place.key.takeIf { AppSettings.placeSet && !AppSettings.place.gps }, onDismiss = { pickCity = false }) { c ->
        AppSettings.updatePlace(Place.of(c))
        PrayerAlerts.reschedule()
    }
}
