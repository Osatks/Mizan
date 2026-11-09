package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** تبويب القرآن من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فيبقى تبويب Compose */
internal expect val nativeQuranAvailable: Boolean

/**
 * تبويب القرآن بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeQuran.swift) بطراز الرئيسية: متابعة القراءة (ووضع التركيز)،
 * والختمة وورد اليوم ومدتها، والدليل، والبحث في السور والآيات، والسور والأجزاء والمحفوظات — كما في [QuranScreen].
 * [open] يفتح المسار نفسه الذي يفتحه تبويب Compose.
 */
@Composable
internal expect fun NativeQuranView(open: (String) -> Unit, modifier: Modifier)
