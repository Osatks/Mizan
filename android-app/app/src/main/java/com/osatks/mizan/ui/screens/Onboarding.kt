package com.osatks.mizan.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.SlowStar
import com.osatks.mizan.ui.drawStar8
import com.osatks.mizan.ui.theme.Gold
import com.osatks.mizan.ui.theme.HeroBrush
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** أيقونة كل صفحة من الجولة */
private val PAGE_ICONS = listOf(Icons.Filled.Star, Icons.Filled.Explore, Icons.AutoMirrored.Filled.MenuBook, Icons.Filled.Calculate, Icons.Filled.Search)

/**
 * جولة أول تشغيل: خمس صفحات تعرّف بأهم ما في التطبيق، تُقلَّب بالسحب أو بزر «التالي»، ويمكن تخطيها.
 * تُعاد من الإعدادات («دليل التطبيق»).
 */
@Composable
fun Onboarding(onDone: () -> Unit) {
    val s = LocalStrings.current
    val pages = s.onboarding
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    Box(Modifier.fillMaxSize().background(HeroBrush)) {
        // نجمة كبيرة تدور ببطء خلف الصفحات
        SlowStar(Modifier.size(420.dp).align(Alignment.TopEnd).graphicsLayer { translationX = 140f; translationY = -80f }, Color.White.copy(alpha = .06f), 90_000, 4f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onDone) { Text(s.obSkip, color = Color.White.copy(alpha = .85f)) }
                else Spacer(Modifier.height(48.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val (title, body) = pages[i]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp).graphicsLayer {
                        val o = ((pager.currentPage - i) + pager.currentPageOffsetFraction).absoluteValue.coerceAtMost(1f)
                        alpha = 1f - 0.6f * o; scaleX = 1f - 0.08f * o; scaleY = 1f - 0.08f * o
                    },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    // الشارة: نجمة ثمانية ذهبية وفي وسطها أيقونة الصفحة
                    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(Color.White.copy(alpha = .08f), size.minDimension / 2)
                            drawStar8(Offset(size.width / 2, size.height / 2), size.minDimension * 0.42f, Gold.copy(alpha = .9f), 2.5.dp.toPx(), rings = false)
                            drawCircle(Color.White.copy(alpha = .10f), size.minDimension * 0.27f)
                        }
                        Icon(PAGE_ICONS.getOrElse(i) { Icons.Filled.Star }, null, Modifier.size(56.dp), tint = Gold)
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = .85f), textAlign = TextAlign.Center)
                }
            }
            // النقاط وزر المتابعة
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.indices.forEach { i ->
                        val on = i == pager.currentPage
                        val w by animateDpAsState(if (on) 22.dp else 8.dp, tween(250), label = "dot")
                        val c by animateColorAsState(if (on) Gold else Color.White.copy(alpha = .35f), tween(250), label = "dotColor")
                        Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c))
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
