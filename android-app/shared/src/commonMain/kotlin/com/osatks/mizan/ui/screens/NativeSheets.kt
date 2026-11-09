package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable

/** أوراق النظام مسجَّلة (iOS، يسجّلها Swift عند التشغيل)؛ في أندرويد لا توجد فتبقى أوراق Compose */
internal expect val nativeSheetsAvailable: Boolean

/** تحكّم الورقة من Kotlin: [close] يغلقها بحركتها ثم ينفّذ [then] (وفوراً إن كانت قد أُغلقت) */
internal interface NativeSheetControl {
    fun close(then: () -> Unit = {})
}

/**
 * ورقة سفلية من النظام (UISheetPresentationController في iOS: بارتفاعين، نصف الشاشة وكلها، ومقبض) يرسم محتواها
 * SwiftUI (iosApp/iosApp/NativeSheets.swift) من [json]؛ وكل تغيير في [json] يصلها وهي ظاهرة.
 * [kind] نوع الورقة ("language" أو "city")، و[startLarge] تبدأ بكامل الارتفاع.
 * [onAction] ما يلمسه المستخدم (اسم الفعل ووسيطه)، و[onDismiss] عند سحبها لإغلاقها.
 * تُغلق وحدها إن خرجت من التركيب.
 */
@Composable
internal expect fun NativeSheetHost(
    kind: String,
    json: String,
    startLarge: Boolean,
    onDismiss: () -> Unit,
    onAction: NativeSheetControl.(action: String, arg: String) -> Unit,
)
