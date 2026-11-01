import AppIntents
import SwiftUI
import Shared

// Siri والاختصارات و Spotlight (iOS 16 فما بعد): «الصلاة القادمة» تجيب دون فتح التطبيق (عبارة ونافذة بعدّ تنازلي)،
// والبقية تفتح التطبيق على شاشتها عبر IosLive.open (المسارات نفسها التي تفتحها الأدوات).
// العبارات بلغات Siri في <lang>.lproj/AppShortcuts.strings، والعناوين في Localizable.strings.

@available(iOS 16.0, *)
struct NextPrayerIntent: AppIntent {
    static var title: LocalizedStringResource = "Next prayer"

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let info = LiveJSON.nextPrayer(IosLive.shared.nextPrayerJson())
        let dialog: IntentDialog = info.map { IntentDialog(LocalizedStringResource(stringLiteral: $0.dialog)) } ?? IntentDialog("Next prayer")
        return .result(dialog: dialog, view: NextPrayerSnippet(info: info))
    }
}

@available(iOS 16.0, *)
struct PrayerTimesIntent: AppIntent {
    static var title: LocalizedStringResource = "Prayer times"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "prayer", section: nil)
        return .result()
    }
}

@available(iOS 16.0, *)
struct QiblaIntent: AppIntent {
    static var title: LocalizedStringResource = "Qibla"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "qibla", section: nil)
        return .result()
    }
}

@available(iOS 16.0, *)
struct ContinueQuranIntent: AppIntent {
    static var title: LocalizedStringResource = "Continue reading"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "quran/continue", section: nil) // IosLive.CONTINUE
        return .result()
    }
}

@available(iOS 16.0, *)
struct TasbihIntent: AppIntent {
    static var title: LocalizedStringResource = "Tasbih"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "tasbih", section: nil)
        return .result()
    }
}

@available(iOS 16.0, *)
struct MorningAdhkarIntent: AppIntent {
    static var title: LocalizedStringResource = "Morning adhkar"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "adhkar", section: "morning")
        return .result()
    }
}

@available(iOS 16.0, *)
struct EveningAdhkarIntent: AppIntent {
    static var title: LocalizedStringResource = "Evening adhkar"
    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "adhkar", section: "evening")
        return .result()
    }
}

/// اختصارات جاهزة بلا إعداد: تظهر في تطبيق الاختصارات و Spotlight، وتُقال لـ Siri، وتُربط بزر الإجراء.
/// iOS 17 فما بعد (العنوان القصير والرمز لكل اختصار)
@available(iOS 17.0, *)
struct MizanShortcuts: AppShortcutsProvider {
    static var shortcutTileColor: ShortcutTileColor = .teal

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: NextPrayerIntent(),
            phrases: [
                "Next prayer in \(.applicationName)",
                "When is the next prayer in \(.applicationName)",
                "\(.applicationName) next prayer",
            ],
            shortTitle: "Next prayer",
            systemImageName: "moon.stars.fill"
        )
        AppShortcut(
            intent: PrayerTimesIntent(),
            phrases: [
                "Prayer times in \(.applicationName)",
                "Show prayer times in \(.applicationName)",
            ],
            shortTitle: "Prayer times",
            systemImageName: "clock.fill"
        )
        AppShortcut(
            intent: QiblaIntent(),
            phrases: [
                "Qibla in \(.applicationName)",
                "Find the qibla with \(.applicationName)",
            ],
            shortTitle: "Qibla",
            systemImageName: "location.north.line.fill"
        )
        AppShortcut(
            intent: ContinueQuranIntent(),
            phrases: [
                "Continue reading in \(.applicationName)",
                "Continue reading the Quran in \(.applicationName)",
            ],
            shortTitle: "Continue reading",
            systemImageName: "book.fill"
        )
        AppShortcut(
            intent: TasbihIntent(),
            phrases: [
                "Tasbih in \(.applicationName)",
                "Open the tasbih in \(.applicationName)",
            ],
            shortTitle: "Tasbih",
            systemImageName: "hand.tap.fill"
        )
        AppShortcut(
            intent: MorningAdhkarIntent(),
            phrases: ["Morning adhkar in \(.applicationName)"],
            shortTitle: "Morning adhkar",
            systemImageName: "sunrise.fill"
        )
        AppShortcut(
            intent: EveningAdhkarIntent(),
            phrases: ["Evening adhkar in \(.applicationName)"],
            shortTitle: "Evening adhkar",
            systemImageName: "sunset.fill"
        )
    }
}

/// نافذة Siri للصلاة القادمة: بطاقة بلون التطبيق، والاسم والوقت، وعدّ تنازلي ذهبي حي، والتاريخ الهجري والمدينة
@available(iOS 16.0, *)
struct NextPrayerSnippet: View {
    let info: NextPrayerInfo?

    var body: some View {
        if let info = info {
            let gold = Color(snippetHex: info.gold)
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 6) {
                    Image(systemName: "moon.stars.fill")
                        .font(.system(size: 12, weight: .semibold))
                    Text(info.label)
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                    Spacer(minLength: 6)
                    Text(info.place)
                        .font(.system(size: 12, design: .rounded))
                        .foregroundColor(.white.opacity(0.7))
                        .lineLimit(1)
                }
                .foregroundColor(gold)
                HStack(alignment: .lastTextBaseline) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(info.name)
                            .font(.system(size: 30, weight: .bold, design: .rounded))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Text(verbatim: "\(info.time) · \(info.hijri)")
                            .font(.system(size: 13, weight: .medium, design: .rounded))
                            .foregroundColor(.white.opacity(0.78))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    Spacer(minLength: 8)
                    Text(timerInterval: min(Date(), info.at)...info.at, countsDown: true)
                        .font(.system(size: 30, weight: .light, design: .rounded))
                        .monospacedDigit()
                        .foregroundColor(gold)
                        .multilineTextAlignment(info.rtl ? .leading : .trailing)
                        .environment(\.layoutDirection, .leftToRight)
                        .frame(maxWidth: 140, alignment: .trailing)
                }
            }
            .foregroundColor(.white)
            .padding(16)
            .background(
                LinearGradient(colors: [Color(snippetHex: info.hero1), Color(snippetHex: info.hero2)],
                               startPoint: .topLeading, endPoint: .bottomTrailing)
            )
            .environment(\.layoutDirection, info.rtl ? .rightToLeft : .leftToRight)
        }
    }
}

private extension Color {
    /// "#RRGGBB" كما في لقطة الأدوات
    init(snippetHex hex: String) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        var v: UInt64 = 0
        _ = Scanner(string: s).scanHexInt64(&v)
        self.init(.sRGB, red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255, opacity: 1)
    }
}
