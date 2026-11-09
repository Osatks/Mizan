package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** شاشة القبلة من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeQiblaAvailable: Boolean

/**
 * القبلة بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeQibla.swift) بطراز الرئيسية: بوصلة حية من اتجاه الهاتف
 * (الشمال الحقيقي)، وكم يستدير المستخدم، والقبلة بالكاميرا، وزاوية القبلة والمسافة إلى الكعبة، ومعايرة البوصلة، والأدلة —
 * كما في [QiblaScreen]. [onBack] يغلق الصفحة (null: بلا زر رجوع).
 */
@Composable
internal expect fun NativeQiblaView(onBack: (() -> Unit)?, modifier: Modifier)
