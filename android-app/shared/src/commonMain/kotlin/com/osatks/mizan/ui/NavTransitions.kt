package com.osatks.mizan.ui


/**
 * انتقالات يرسمها النظام للصفحات الفرعية داخل شاشة واحدة ([SubPages]، أقسام الأذكار وأنواع الزكاة في iOS):
 * صورة الصفحة قبل التغيير تتحرك مع الصفحة الحية بعده (iOS: Core Animation في متحكم الصفحة، NavMotion.kt).
 * التغيير نفسه فوري، وينفّذه [change] متزامناً بعد التقاط الصورة. التنقل بين الشاشات نفسها في UIKit لا هنا.
 */
interface NavTransitions {
    /** فتح صفحة فرعية: [change] يظهرها. يعيد موضع اللمس الذي نمت منه، يُمرَّر إلى [pop] عند الرجوع منها */
    fun push(change: () -> Unit): Any?

    /** رجوع منها: [change] يزيلها، وتصغر نحو [origin] الذي فُتحت منه */
    fun pop(origin: Any?, change: () -> Unit)

    companion object {
        /** متحكم الصفحة الظاهرة في iOS يضعه حين يظهر؛ null في أندرويد، فالحركة في Compose كما هي */
        var current: NavTransitions? = null
    }
}
