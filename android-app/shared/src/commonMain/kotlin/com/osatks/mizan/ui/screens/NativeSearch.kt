package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** البحث من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فتبقى شاشة Compose */
internal expect val nativeSearchAvailable: Boolean

/** نتيجة بحث كما في [SearchScreen] (مجموعتها ومعرّفها وعنوانها ووصفها)، والمعرّف يدل على نوعها وأيقونتها */
internal class NativeSearchHit(val group: String, val id: String, val title: String, val subtitle: String)

/**
 * البحث الشامل بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeSearch.swift) بطراز الرئيسية: حقل بحث من النظام
 * يُركَّز عليه عند الظهور، والنتائج مجمّعة كما في [SearchScreen]. النتائج كلها تُحسب في Kotlin ([hits] مرتبة، و[blank] حين لا كلمات).
 * Swift يخبر بالنص الجديد ([onQuery])، وبالنتيجة الملموسة بمعرّفها ([onPick]: يُنفّذ فعلها كما في Compose)، وبالرجوع.
 */
@Composable
internal expect fun NativeSearchView(
    query: String, blank: Boolean, hits: List<NativeSearchHit>,
    onQuery: (String) -> Unit, onPick: (String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
)
