package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** شاشة المسبحة من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeTasbihAvailable: Boolean

/**
 * المسبحة بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeTasbih.swift) بطراز الرئيسية: الذكر ومعناه، والحبة الكبيرة
 * تعدّ فوراً (العدّ يظهر في Swift ويصل كل لمس إلى [com.osatks.mizan.core.Tasbih] فتتبعه الأداة والنشاط الحي)، والتصفير
 * والتراجع، ومفتاح شاشة القفل، واختيار الذكر والهدف، والإحصاء، والأدلة — كما في [TasbihScreen].
 */
@Composable
internal expect fun NativeTasbihView(onBack: (() -> Unit)?, modifier: Modifier)
