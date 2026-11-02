import SwiftUI
import WidgetKit
import AppIntents

// أزرار مركز التحكم وشاشة القفل وزر الإجراء (iOS 18): القبلة والصلاة والمصحف تفتح ميزان على شاشتها
// (OpenScreenIntent)، والمسبحة والأذكار والعدّ التنازلي تعمل دون فتح التطبيق (Shared/LiveIntents.swift).
// النوايا في Shared/ (في التطبيق والأداة معاً) فيجدها النظام في التطبيق وينفذها فيه.

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

/// عدّاد التسبيح: كل لمسة تعدّ تسبيحة دون فتح التطبيق، والعدد على الزر نفسه، والعدّاد في الجزيرة الديناميكية
/// (يعمل من مركز التحكم ومن زرّي شاشة القفل ومن زر الإجراء). النوع نفسه الذي كان يفتح المسبحة، فيصير عدّاداً في مكانه
@available(iOS 18.0, *)
struct TasbihControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: LiveRouter.tasbihControlKind, provider: TasbihControlProvider()) { value in
            ControlWidgetButton(action: TasbihCountIntent()) {
                Label {
                    Text(verbatim: "\(value.count) \(value.goal)")
                } icon: {
                    Image(systemName: "circle.dotted.circle")
                }
            }
        }
        .displayName("Tasbih counter")
        .description("Counts one tasbih without opening Mizan")
    }
}

/// العدّ التنازلي للصلاة القادمة على شاشة القفل وفي الجزيرة الديناميكية، واللمسة التالية تخفيه
@available(iOS 18.0, *)
struct PrayerCountdownControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.countdown") {
            ControlWidgetButton(action: ShowNextPrayerIntent()) {
                Label("Prayer countdown", systemImage: "hourglass")
            }
        }
        .displayName("Prayer countdown")
        .description("Shows the countdown to the next prayer on the Lock Screen")
    }
}

/// عدّاد أذكار الصباح أو المساء في الجزيرة الديناميكية
@available(iOS 18.0, *)
struct AdhkarCounterControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        StaticControlConfiguration(kind: "com.mizan.almuslim.control.adhkar") {
            ControlWidgetButton(action: AdhkarCountIntent()) {
                Label("Count adhkar", systemImage: "sun.and.horizon.fill")
            }
        }
        .displayName("Count adhkar")
        .description("Counts the morning or evening adhkar on the Lock Screen")
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
                Label("Continue reading", systemImage: "bookmark.fill")
            }
        }
        .displayName("Continue reading")
        .description("Opens the Quran where you stopped")
    }
}
