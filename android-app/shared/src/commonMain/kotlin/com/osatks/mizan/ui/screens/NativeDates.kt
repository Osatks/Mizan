package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.i18n.Proof

/** أدوات التاريخ من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشات Compose */
internal expect val nativeDatesAvailable: Boolean

/**
 * أدوات التاريخ الثلاث بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeDates.swift) بطراز الرئيسية:
 * [kind] "hijri" (المحوّل والتقويم الهجري)، و"age" (العمر بالتقويمين)، و"events" (المناسبات والعدّ إليها) — كما في
 * [HijriScreen] و[AgeScreen] و[EventsScreen]. [content] ما تعرضه الشاشة منسّقاً كله في Kotlin (النصوص والأرقام والتواريخ)،
 * و[evidence] أدلتها، و[onAction] يتلقى لمس المستخدم باسمه وقيمته (day، month، year، dir، today، calendar، pick، mode، select).
 */
@Composable
internal expect fun NativeDatesView(
    kind: String, content: Map<String, Any?>, evidence: List<Proof>,
    onAction: (String, String) -> Unit, onBack: () -> Unit, modifier: Modifier,
)
