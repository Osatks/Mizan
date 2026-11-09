package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** صفحة المصادر والتراخيص من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى صفحة Compose */
internal expect val nativeLicensesAvailable: Boolean

/**
 * المصادر والتراخيص بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeLicenses.swift) بطراز الرئيسية: ترويستها،
 * وبطاقة لكل مصدر، ونصّا الرخصتين كما هما — كما في [LicensesScreen]. [onBack] يغلق الصفحة.
 */
@Composable
internal expect fun NativeLicensesView(onBack: () -> Unit, modifier: Modifier)
