package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * القبلة بالكاميرا (iPhone و iPad بـ ARKit): الكعبة واتجاهها فوق ما تراه الكاميرا. يضبطه iOS عند الإقلاع
 * (IosArQibla و ArQibla.swift)؛ أندرويد لا يضبطه فيبقى الزر مخفياً فيه.
 */
object ArQibla {
    /** الجهاز يدعم الواقع المعزز */
    var supported by mutableStateOf(false)

    /** يفتح شاشة الكاميرا */
    var open: (() -> Unit)? = null
}
