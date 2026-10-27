package com.osatks.mizan.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/**
 * اهتزازات iOS بمولّدات النظام نفسها، لكل نوع ما يناسبه:
 * تبديل التبويب والقطع المتجاورة "تكّة" اختيار، وعدّاد التسبيح نقرة خفيفة، والضغط المطوّل وبلوغ الهدف نقرة متوسطة،
 * والمفاتيح نقرة صلبة عند التشغيل وناعمة عند الإيقاف. كل مولّد يُجهَّز بعد استعماله فلا تتأخر النقرة التالية.
 */
internal object IosHaptics : HapticFeedback {
    private val selection by lazy { UISelectionFeedbackGenerator() }
    private val light by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight) }
    private val medium by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium) }
    private val rigid by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleRigid) }
    private val soft by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft) }
    private val notify by lazy { UINotificationFeedbackGenerator() }

    private fun impact(g: UIImpactFeedbackGenerator) { g.impactOccurred(); g.prepare() }

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        when (hapticFeedbackType) {
            HapticFeedbackType.SegmentTick, HapticFeedbackType.SegmentFrequentTick -> { selection.selectionChanged(); selection.prepare() }
            // عدّادات التسبيح والأذكار، ومقابض تحديد النص
            HapticFeedbackType.TextHandleMove, HapticFeedbackType.VirtualKey -> impact(light)
            HapticFeedbackType.LongPress, HapticFeedbackType.GestureThresholdActivate -> impact(medium)
            HapticFeedbackType.ContextClick, HapticFeedbackType.ToggleOn -> impact(rigid)
            HapticFeedbackType.ToggleOff, HapticFeedbackType.GestureEnd -> impact(soft)
            HapticFeedbackType.Confirm -> { notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess); notify.prepare() }
            HapticFeedbackType.Reject -> { notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError); notify.prepare() }
            else -> impact(light)
        }
    }
}
