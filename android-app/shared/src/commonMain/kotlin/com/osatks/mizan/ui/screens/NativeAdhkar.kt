package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.adhkar.AdhkarSection

/** الأذكار من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeAdhkarAvailable: Boolean

/**
 * الأذكار بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeAdhkar.swift) بطراز الرئيسية: أقسامها (وأذكار الوقت في أعلاها)،
 * ثم أذكار القسم [open] بعدّاد لكل ذكر وترجمته ودليله (الدرجة والمصدر والراوي والفضل) — كما في [AdhkarScreen].
 * [onOpen] يفتح قسماً ("" يعود إلى الأقسام)، و[onBack] يغلق الأذكار، و[onShare] يشارك ذكراً صورةَ قصة (من Compose).
 */
@Composable
internal expect fun NativeAdhkarView(
    sections: List<AdhkarSection>?, open: String, onOpen: (String) -> Unit, onBack: () -> Unit, onShare: (String) -> Unit,
    modifier: Modifier,
)
