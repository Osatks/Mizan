package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** شاشة المواقيت من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativePrayerAvailable: Boolean

/**
 * شاشة المواقيت بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativePrayer.swift) بطراز الرئيسية: الصلاة القادمة والعدّ إليها،
 * ومواقيت اليوم المختار ونهاياتها وإقامتها، وجدول الشهر، والقبلة ببوصلة حية، والموقع وطريقة الحساب، والإقامة، والتنبيهات،
 * والأدلة — كما في [PrayerScreen]. ما يحتاج Compose (تحديد الموقع وإذنه، واختيار المدينة، وإذن الإشعارات) يبقى في PrayerScreen.
 */
@Composable
internal expect fun NativePrayerView(
    open: (String) -> Unit, locating: Boolean, onLocate: () -> Unit, onPickCity: () -> Unit, onNotify: (Boolean) -> Unit,
    modifier: Modifier,
)
