package com.osatks.mizan.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.osatks.mizan.i18n.Proof

/** زكاة الفطر والفدية من النظام مسجَّلتان (iOS، ويسجّلهما Swift عند التشغيل)؛ في أندرويد لا توجدان فتبقى شاشتا Compose */
internal expect val nativeFitrFidyaAvailable: Boolean

/**
 * زكاة الفطر ([kind] = "fitr") أو الفدية والكفارات ("fidya") بواجهة النظام (SwiftUI في iOS: iosApp/iosApp/NativeFitrFidya.swift)
 * بطراز الرئيسية: بطاقة النتيجة، والمدخلات (عدّاد، واختيار، وقائمة، وحقول أرقام)، والملاحظات، والأدلة — كما في [FitrScreen] و[FidyaScreen].
 * [content] يبنيه Kotlin كله (النصوص والنتائج منسّقة بأرقام التطبيق، والصفوف الظاهرة)، ويُرسل إلى Swift كما هو.
 * Swift يردّ باللمس: [onStep] لعدّاد (مفتاحه وقيمته)، و[onChange] لاختيار أو نص حقل، و[onBack] للرجوع.
 */
@Composable
internal expect fun NativeFitrFidyaView(
    kind: String, content: Map<String, Any?>, evidenceKey: String, evidence: List<Proof>,
    onStep: (String, Int) -> Unit, onChange: (String, String) -> Unit, onBack: () -> Unit,
    modifier: Modifier,
)
