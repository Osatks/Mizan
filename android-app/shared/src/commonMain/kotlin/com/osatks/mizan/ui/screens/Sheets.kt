package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import com.osatks.mizan.core.City

/**
 * اختيار اللغة: كل لغة باسمها الأصلي وتحته اسمها بلغة الواجهة، وتحتها اللغات المتاحة للتنزيل (حزم اللغات) وتحديث ما صدر
 * له إصدار أحدث. في أندرويد ورقة Compose زجاجية (Sheets.android.kt)، وفي iOS ورقة النظام بقائمة SwiftUI (Sheets.ios.kt)
 */
@Composable
expect fun LanguageSheet(onDismiss: () -> Unit)

/** اختيار مدينة مع بحث فوري بأي لغة (اسم المدينة أو الدولة): ورقة Compose في أندرويد، وورقة النظام بحقل بحثه في iOS */
@Composable
expect fun CityPickerSheet(selectedKey: String?, onDismiss: () -> Unit, onPick: (City) -> Unit)
