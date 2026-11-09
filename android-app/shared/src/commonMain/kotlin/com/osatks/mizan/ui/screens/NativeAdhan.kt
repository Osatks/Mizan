package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** إعدادات الأذان من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeAdhanAvailable: Boolean

/**
 * الأذان والتنبيهات بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeAdhan.swift) بطراز الرئيسية: مفاتيح التنبيه
 * والعدّ الحي والتقويم، وطريقة التنبيه لكل صلاة (قائمة النظام)، والمؤذن وأذان الفجر مع الاستماع والتنزيل بتقدمه والحذف،
 * وأنماط الاهتزاز برسمها وتجربتها، ومصادر التسجيلات — كما في [AdhanScreen]. [content] يبنيه Kotlin كله.
 * Swift يردّ باللمس: [onAction] بمفتاح الأمر وقيمته (notify، live، calendar، exactAlarm، mode:<prayer>، pick، pickFajr،
 * download، delete، play، vibe، tryVibe)، و[onBack] للرجوع. التنزيل والتشغيل والحفظ تبقى في Kotlin.
 */
@Composable
internal expect fun NativeAdhanView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
)
