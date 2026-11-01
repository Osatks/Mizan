import Foundation
#if canImport(ActivityKit)
import ActivityKit
#endif
#if canImport(AppIntents)
import AppIntents
#endif

// ما يشترك فيه التطبيق والأداة: أنواع الأنشطة الحية (يبدؤها التطبيق ويرسمها امتداد الأدوات)،
// والنوايا (App Intents) التي تعمل من أزرار مركز التحكم والنشاط الحي، فتُنفَّذ في التطبيق نفسه.
// يجب أن يكون الملف في الهدفين معاً: هكذا يجد النظام النية في التطبيق ويفتحه.
// ActivityKit و AppIntents غير موجودين في iOS 15: يُربطان ربطاً ضعيفاً (OTHER_LDFLAGS في project.yml).

#if canImport(ActivityKit)
/// العدّ التنازلي للصلاة القادمة (LiveActivities.swift يبدؤه، و LiveActivityViews.swift يرسمه)
@available(iOS 16.2, *)
struct PrayerActivityAttributes: ActivityAttributes {
    typealias ContentState = PrayerLiveState
    /// الصلاة وتاريخها
    var id: String
}

/// «يُتلى الآن» ما دامت التلاوة
@available(iOS 16.2, *)
struct RecitationActivityAttributes: ActivityAttributes {
    typealias ContentState = RecitationLiveState
}
#endif

/// ما ترسله النوايا إلى التطبيق (يستقبله LiveActivities.swift ويمرره إلى Kotlin)
enum MizanBridge {
    static let openRoute = Notification.Name("MizanOpenRoute")
    static let toggleRecitation = Notification.Name("MizanRecitationToggle")

    static func open(_ route: String, section: String? = nil) {
        var info: [String: String] = ["route": route]
        if let section = section { info["section"] = section }
        NotificationCenter.default.post(name: openRoute, object: nil, userInfo: info)
    }
}

#if canImport(AppIntents)
/// شاشات يفتحها زر في مركز التحكم أو إجراء في تطبيق الاختصارات
@available(iOS 16.0, *)
enum MizanScreen: String, AppEnum {
    case prayer, qibla, quran, tasbih, morning, evening

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Mizan screen"
    static var caseDisplayRepresentations: [MizanScreen: DisplayRepresentation] = [
        .prayer: "Prayer times",
        .qibla: "Qibla",
        .quran: "Continue reading",
        .tasbih: "Tasbih",
        .morning: "Morning adhkar",
        .evening: "Evening adhkar",
    ]

    /// المسار في التطبيق (MizanApp) وقسم الأذكار إن وُجد
    var route: (String, String?) {
        switch self {
        case .prayer: return ("prayer", nil)
        case .qibla: return ("qibla", nil)
        case .quran: return ("quran/continue", nil)
        case .tasbih: return ("tasbih", nil)
        case .morning: return ("adhkar", "morning")
        case .evening: return ("adhkar", "evening")
        }
    }
}

/// يفتح ميزان على شاشة: أزرار مركز التحكم (iOS 18) وإجراء «افتح في ميزان» في الاختصارات.
/// OpenIntent: يفتح النظام التطبيق ثم يُنفذ perform فيه.
@available(iOS 16.0, *)
struct OpenScreenIntent: OpenIntent {
    static var title: LocalizedStringResource = "Open in Mizan"

    @Parameter(title: "Screen")
    var target: MizanScreen

    init() {}

    init(target: MizanScreen) {
        self.target = target
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        let (route, section) = target.route
        MizanBridge.open(route, section: section)
        return .result()
    }
}

/// زر التشغيل والإيقاف في النشاط الحي «يُتلى الآن» (iOS 17): يُنفذ في التطبيق ولو كان في الخلفية
@available(iOS 17.0, *)
struct ToggleRecitationIntent: LiveActivityIntent, AudioPlaybackIntent {
    static var title: LocalizedStringResource = "Play or pause the recitation"
    static var isDiscoverable: Bool = false

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult {
        NotificationCenter.default.post(name: MizanBridge.toggleRecitation, object: nil)
        return .result()
    }
}
#endif
