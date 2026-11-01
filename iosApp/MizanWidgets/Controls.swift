import SwiftUI
import WidgetKit
import AppIntents

// أزرار مركز التحكم وشاشة القفل وزر الإجراء (iOS 18): كل زر يفتح ميزان على شاشته.
// النية OpenScreenIntent في Shared/ (في التطبيق والأداة معاً) فيفتح النظام التطبيق ويُنفذها فيه.

@available(iOS 18.0, *)
struct QiblaControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.qibla") {
            ControlWidgetButton(action: OpenScreenIntent(target: .qibla)) {
                Label("Qibla", systemImage: "location.north.line.fill")
            }
        }
        .displayName("Qibla")
        .description("Opens the qibla compass")
    }
}

@available(iOS 18.0, *)
struct TasbihControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.tasbih") {
            ControlWidgetButton(action: OpenScreenIntent(target: .tasbih)) {
                Label("Tasbih", systemImage: "hand.tap.fill")
            }
        }
        .displayName("Tasbih")
        .description("Opens the tasbih counter")
    }
}

@available(iOS 18.0, *)
struct NextPrayerControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.prayer") {
            ControlWidgetButton(action: OpenScreenIntent(target: .prayer)) {
                Label("Next prayer", systemImage: "moon.stars.fill")
            }
        }
        .displayName("Next prayer")
        .description("Opens today's prayer times")
    }
}

@available(iOS 18.0, *)
struct ContinueQuranControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.quran") {
            ControlWidgetButton(action: OpenScreenIntent(target: .quran)) {
                Label("Continue reading", systemImage: "book.fill")
            }
        }
        .displayName("Continue reading")
        .description("Opens the Quran where you stopped")
    }
}
