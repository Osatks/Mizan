package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.hadith.Hadith

/** حديث اليوم من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فتبقى شاشة Compose */
internal expect val nativeHadithAvailable: Boolean

/**
 * حديث اليوم بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeHadith.swift) بطراز الرئيسية: نصه العربي ودرجته وتخريجه،
 * وترجمته بلغة الواجهة، والشرح والفوائد، والمصدر ورابطه، والمشاركة صورةَ قصة، وأحاديث الأيام الماضية — كما في [HadithScreen].
 * [hadith] null ما دام يُحمَّل؛ و[title] و[subtitle] (اليوم وتاريخه) منسّقان في Kotlin؛ و[canNext] ما لم يكن اليوم.
 * [onDay] يتقدم يوماً (+1) أو يتأخر (-1)، و[onShare] يفتح ورقة القصة (من Compose)، و[onSource] يفتح الحديث في الموسوعة.
 */
@Composable
internal expect fun NativeHadithView(
    hadith: Hadith?, title: String, subtitle: String, canNext: Boolean,
    onDay: (Int) -> Unit, onShare: () -> Unit, onSource: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
)
