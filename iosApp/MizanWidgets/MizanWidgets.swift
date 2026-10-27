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
    }
}
