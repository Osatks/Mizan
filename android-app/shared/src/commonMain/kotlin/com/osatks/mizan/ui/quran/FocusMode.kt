package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable

/**
 * إسكات الإشعارات أثناء القراءة في وضع التركيز (أندرويد: «عدم الإزعاج» بإذن المستخدم).
 * في iOS لا يسمح النظام للتطبيقات بذلك، فلا يظهر الخيار (granted و asked ثابتان).
 */
expect object QuietReading {
    fun granted(): Boolean
    fun asked(): Boolean
    fun markAsked()
    fun requestAccess()
    fun silence()
    fun restore()
}

/** آثار وضع التركيز على النظام: إخفاء الأشرطة، وإبقاء الشاشة مضاءة، وإسكات الإشعارات؛ كلها تعود عند الخروج */
@Composable
expect fun FocusEffects(active: Boolean)

/** سطوع الشاشة أثناء القراءة كما اختاره المستخدم (سالب = سطوع الجهاز) */
@Composable
expect fun ReadingBrightness()

/** التلاوة تتبع صفحة القراءة: تتوقف مؤقتاً بمغادرة التطبيق، وتنتهي بالخروج من الصفحة */
@Composable
expect fun RecitationLifecycle()
