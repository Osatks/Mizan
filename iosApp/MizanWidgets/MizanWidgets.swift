import WidgetKit
import SwiftUI

/// أدوات ميزان في iOS: تقرأ لقطة يكتبها التطبيق (IosWidgets في Kotlin) في حاوية App Group، وإن لم تصلها
/// (تثبيت بحساب Apple مجاني) تحسب المواقيت والأذكار وآية اليوم وحدها من موقعها (Standalone/)،
/// فلا تحتاج إلى الشبكة ولا إلى تشغيل التطبيق.
@main
struct MizanWidgets: WidgetBundle {
    var body: some Widget {
        NextPrayerWidget()
        PrayerTimesWidget()
        AdhkarWidget()
        HijriWidget()
        AyahWidget()
        // الأنشطة الحية: العدّ التنازلي للصلاة القادمة و«يُتلى الآن» (LiveActivityViews.swift)
        if #available(iOS 16.2, *) {
            PrayerActivityWidget()
            RecitationActivityWidget()
        }
        // أزرار مركز التحكم (Controls.swift)
        if #available(iOS 18.0, *) {
            QiblaControl()
            TasbihControl()
            NextPrayerControl()
            ContinueQuranControl()
        }
    }
}
