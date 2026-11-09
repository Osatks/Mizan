package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** دليل التطبيق من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فتبقى جولة Compose */
internal expect val nativeGuideAvailable: Boolean

/** صفحة من الجولة للواجهة: العنوان والنص ومفتاح أيقونتها (star، explore، quran، calc، search، widgets، quote، share، lock، tap، watch، adhan، place) */
internal class GuidePage(val title: String, val body: String, val icon: String)

/**
 * جولة التطبيق بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeGuide.swift): صفحات تُقلَّب بالسحب، ونقاطها،
 * وزر «التالي» و«تخطي»، وصفحة الموقع الأخيرة بزرّيها — كما في [Onboarding]. الصفحة الحالية ومنطق التخطي في Kotlin
 * ([page]، [onPage]، [onNext]، [onSkip])؛ [location]: الصفحة الأخيرة صفحة الموقع، و[placeName] الموقع المختار إن وُجد.
 */
@Composable
internal expect fun NativeGuideView(
    pages: List<GuidePage>, page: Int, location: Boolean, placeName: String?, locating: Boolean,
    onPage: (Int) -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onLocate: () -> Unit, onPickCity: () -> Unit,
    modifier: Modifier,
)
