package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * المسبحة على شاشة القفل وفي الجزيرة الديناميكية (نشاط حي في iOS 17 فما بعد): زر في شاشة المسبحة يُظهرها هناك.
 * يضبطه iOS (IosTasbih)؛ في أندرويد يبقى [supported] خطأ فلا يظهر الزر.
 */
object LiveTasbih {
    var supported by mutableStateOf(false)

    /** يبدأ النشاط الحي بالعدد الحالي (يضبطه iOS) */
    var show: (() -> Unit)? = null
}
