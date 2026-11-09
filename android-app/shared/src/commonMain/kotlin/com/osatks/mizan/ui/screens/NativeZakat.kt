package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** الزكاة من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeZakatAvailable: Boolean

/**
 * الزكاة بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeZakat.swift) بطراز الرئيسية: أنواعها الخمسة، ثم حاسبة النوع [open]
 * (النقدان، عروض التجارة، الأنعام، الزروع، المعادن والركاز) بمدخلاتها ونتيجتها وأدلتها — كما في [ZakatScreen].
 * الحساب والحفظ والنصوص كلها في Kotlin. [onOpen] يفتح نوعاً ("" يعود إلى الأنواع)، و[onBack] يغلق الزكاة.
 */
@Composable
internal expect fun NativeZakatView(open: String, onOpen: (String) -> Unit, onBack: () -> Unit, modifier: Modifier)
