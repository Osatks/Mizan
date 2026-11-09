package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** مكتبة التفاسير من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeTafsirsAvailable: Boolean

/**
 * مكتبة التفاسير بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeTafsirs.swift) بطراز الرئيسية: اختيار مقسّم بين
 * «تنزيل» و«على الجهاز»، وبطاقات التفاسير بشارة لغتها وحجمها وشريط تقدم تنزيلها، ومفتاح إظهار كل مصدر في ورقة التفسير
 * وحذف المنزَّل — كما في [TafsirLibraryScreen]. [content] يبنيه Kotlin كله (النصوص والحالات والنِّسب منسّقة بأرقام التطبيق).
 * Swift يردّ باللمس: [onAction] بمفتاح الأمر وقيمته (tab، download، cancel، delete، visible:<id>)، و[onBack] للرجوع.
 * التنزيل والحذف والحفظ تبقى في Kotlin، ونافذة تأكيد الحذف من Compose (تنبيه النظام).
 */
@Composable
internal expect fun NativeTafsirsView(
    content: Map<String, Any?>, onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
)
