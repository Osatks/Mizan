package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** تبويب الأدوات من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فيبقى تبويب Compose */
internal expect val nativeToolsAvailable: Boolean

/**
 * تبويب الأدوات بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeTools.swift) بطراز الرئيسية: ترويستها، وشبكة الأدوات
 * (عمودان على الهاتف، وثلاثة وأربعة على الأوسع) — كما في [ToolsScreen]. [open] يفتح الأداة.
 */
@Composable
internal expect fun NativeToolsView(open: (String) -> Unit, modifier: Modifier)
