package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** تبويب الإعدادات من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فيبقى تبويب Compose */
internal expect val nativeSettingsAvailable: Boolean

/**
 * تبويب الإعدادات بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeSettings.swift) بطراز الرئيسية: مجموعات بطاقات
 * من الصفوف كما في [SettingsScreen]. [open] يفتح الصفحة، و[onLanguage] يفتح ورقة اللغة (تبقى في Compose)،
 * و[onNotify] يبدّل تنبيه الأذان مع طلب الإذن.
 */
@Composable
internal expect fun NativeSettingsView(open: (String) -> Unit, onLanguage: () -> Unit, onNotify: (Boolean) -> Unit, modifier: Modifier)
