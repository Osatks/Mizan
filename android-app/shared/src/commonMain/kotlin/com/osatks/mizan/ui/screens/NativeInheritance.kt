package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.InheritanceResult

/** حاسبة المواريث من النظام مسجَّلة (iOS، ويسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى شاشة Compose */
internal expect val nativeInheritanceAvailable: Boolean

/**
 * حاسبة المواريث بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeInheritance.swift) بطراز الرئيسية: أصل المسألة وعَولها أو ردّها،
 * وأنصبة الورثة وكسورها ومبالغها والمحجوبين وأدلتهم، وملاحظات المسألة، والميت والتركة، والورثة بعدّاد أو مفتاح لكل وارث،
 * والأدلة والتنبيه — كما في [InheritanceScreen]. المدخلات تبقى في InheritanceScreen، والقسمة ([result]) تُحسب هناك وتُنسّق هنا.
 * [onField] مفتاحه: estate أو funeral أو debts أو will.
 */
@Composable
internal expect fun NativeInheritanceView(
    gender: Char, counts: Map<String, Int>, active: Map<String, Int>,
    estate: String, funeral: String, debts: String, will: String,
    result: InheritanceResult?, net: Double, willCapped: Boolean,
    onGender: (Char) -> Unit, onCount: (String, Int) -> Unit, onField: (String, String) -> Unit,
    onReset: () -> Unit, onShare: () -> Unit, onBack: () -> Unit,
    modifier: Modifier,
)
