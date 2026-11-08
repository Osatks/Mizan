package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** الرئيسية من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى رئيسية Compose */
internal expect val nativeHomeAvailable: Boolean

/**
 * الرئيسية بواجهة النظام نفسها (SwiftUI في iOS: iosApp/iosApp/NativeHome.swift): الترويسة وبطاقة اليوم وأذكار الوقت
 * والصلاة القادمة، ومواقيت اليوم، وأدوات كل يوم، والقرآن الكريم، وآية اليوم وحديثه — كما في [HomeScreen].
 * العدّادات تتقدم كل ثانية في Swift دون Compose؛ وKotlin يرسل الحالة كل دقيقة وعند كل تغيّر.
 * [open] يفتح المسار كما من الرئيسية، و[onLanguage] يفتح ورقة اللغة.
 */
@Composable
internal expect fun NativeHomeView(open: (String) -> Unit, onLanguage: () -> Unit, modifier: Modifier)
