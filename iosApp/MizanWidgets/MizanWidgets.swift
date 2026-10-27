import WidgetKit
import SwiftUI

/// أدوات ميزان في iOS: تقرأ لقطة يكتبها التطبيق (IosWidgets في Kotlin) في حاوية App Group،
/// فلا تحتاج إلى الشبكة ولا إلى تشغيل التطبيق، وتبقى صحيحة أسبوعاً.
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
