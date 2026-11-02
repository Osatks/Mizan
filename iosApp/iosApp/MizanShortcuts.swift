import AppIntents
import SwiftUI
import CoreText
import Shared

// Siri والاختصارات و Spotlight (iOS 16 فما بعد): «الصلاة القادمة» تجيب دون فتح التطبيق (عبارة ونافذة بعدّ تنازلي)،
// والبقية تفتح التطبيق على شاشتها عبر IosLive.open (المسارات نفسها التي تفتحها الأدوات).
// العبارات بلغات Siri في <lang>.lproj/AppShortcuts.strings، والعناوين في Localizable.strings.

/// الصلاة القادمة: يُظهر عدّها التنازلي في الجزيرة الديناميكية وعلى شاشة القفل دون فتح التطبيق، وتقولها Siri،
/// ويعيد وقتها (أتمتة تطبيق الاختصارات)
@available(iOS 17.0, *)
struct NextPrayerIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Next prayer"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var openAppWhenRun: Bool = false

    @MainActor
    func perform() async throws -> some IntentResult & ReturnsValue<Date> & ProvidesDialog {
        try LiveIntentError.check(await AppLive.button(.nextPrayer))
        let info = LiveJSON.nextPrayer(IosLive.shared.nextPrayerJson())
        let dialog: IntentDialog = info.map { IntentDialog(LocalizedStringResource(stringLiteral: $0.dialog)) } ?? IntentDialog("Next prayer")
        return .result(value: info?.at ?? Date(), dialog: dialog)
    }
}

// زر الإجراء و Siri والاختصارات: نوايا في التطبيق وحده، فتُنفَّذ دائماً في عملية التطبيق (في الخلفية، دون فتحه)
// حيث Kotlin و ActivityKit. نوايا أزرار النشاط والأداة ومركز التحكم في الهدفين (Shared/LiveIntents.swift) مخفية عن الاختصارات.

/// تسبيحة بكل ضغطة، بلا بطاقة على شاشة القفل (يُحدَّث العدّاد إن كان ظاهراً)
@available(iOS 17.0, *)
struct ActionTasbihIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Count tasbih"
    static var description = IntentDescription("Adds one to the tasbih count with each press")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var openAppWhenRun: Bool = false

    @MainActor
    func perform() async throws -> some IntentResult {
        try LiveIntentError.check(await AppLive.button(.tasbih))
        return .result()
    }
}

/// تقرير الجزيرة: آخر ضغطات زر الإجراء وما حدث في كل منها (من تطبيق الاختصارات: إجراءات ميزان)
@available(iOS 17.0, *)
struct LiveReportIntent: AppIntent {
    static var title: LocalizedStringResource = "Dynamic Island report"
    static var description = IntentDescription("Shows what happened on the last Action Button presses, to find why the Dynamic Island does not show")
    static var openAppWhenRun: Bool = false

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        .result(dialog: IntentDialog(LocalizedStringResource(stringLiteral: LiveLog.text)))
    }
}

/// الضغطة الأولى تُظهر أذكار الوقت في الجزيرة وعلى شاشة القفل، وكل ضغطة بعدها تعدّ الذكر الحالي
@available(iOS 17.0, *)
struct ActionAdhkarIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Count adhkar"
    static var description = IntentDescription("Shows the morning or evening adhkar in the Dynamic Island, then counts the current dhikr with each press")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var openAppWhenRun: Bool = false

    @MainActor
    func perform() async throws -> some IntentResult {
        let outcome = await AppLive.button(.adhkar)
        // خطوات ما جرى تبقى لفحص المحاكي (LiveCheck.swift) ولا تُعرض للمستخدم
        LiveActivities.log.notice("\(LiveDebug.take(), privacy: .public)")
        try LiveIntentError.check(outcome)
        return .result()
    }
}

/// العدّ التنازلي للصلاة القادمة في الجزيرة وعلى شاشة القفل، والضغطة التالية تخفيه
@available(iOS 17.0, *)
struct ActionPrayerCountdownIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Prayer countdown"
    static var description = IntentDescription("Shows or hides the countdown to the next prayer in the Dynamic Island and on the Lock Screen")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var openAppWhenRun: Bool = false

    @MainActor
    func perform() async throws -> some IntentResult {
        let outcome = await AppLive.button(.prayerToggle)
        // خطوات ما جرى تبقى لفحص المحاكي (LiveCheck.swift) ولا تُعرض للمستخدم
        LiveActivities.log.notice("\(LiveDebug.take(), privacy: .public)")
        try LiveIntentError.check(outcome)
        return .result()
    }
}

/// آية اليوم: تقرؤها Siri (معناها بلغة التطبيق، أو نصها بالعربية) وتعرضها بخط المصحف
@available(iOS 16.0, *)
struct AyahOfDayIntent: AppIntent {
    static var title: LocalizedStringResource = "Ayah of the day"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let info = LiveJSON.ayah(IosAdhkar.shared.ayahOfDayJson())
        let dialog: IntentDialog = info.map { IntentDialog(LocalizedStringResource(stringLiteral: $0.dialog)) } ?? IntentDialog("Ayah of the day")
        return .result(dialog: dialog, view: AyahSnippet(info: info))
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

/// اختصارات جاهزة بلا إعداد (عشرة، أقصى ما يسمح به النظام): تظهر في تطبيق الاختصارات و Spotlight، وتُقال لـ Siri،
/// وتُربط بزر الإجراء. التسبيح والأذكار والعدّ التنازلي تعمل دون فتح التطبيق ولو والجهاز مقفل (Shared/LiveIntents.swift).
/// iOS 17 فما بعد (العنوان القصير والرمز لكل اختصار، ونوايا الأنشطة الحية)
@available(iOS 17.0, *)
struct MizanShortcuts: AppShortcutsProvider {
    static var shortcutTileColor: ShortcutTileColor = .teal

    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: ActionTasbihIntent(),
            phrases: [
                "Tasbih in \(.applicationName)",
                "Count tasbih in \(.applicationName)",
            ],
            shortTitle: "Count tasbih",
            systemImageName: "circle.dotted.circle"
        )
        AppShortcut(
            intent: ActionPrayerCountdownIntent(),
            phrases: [
                "Prayer countdown in \(.applicationName)",
                "Show the prayer countdown in \(.applicationName)",
            ],
            shortTitle: "Prayer countdown",
            systemImageName: "hourglass"
        )
        AppShortcut(
            intent: ActionAdhkarIntent(),
            phrases: [
                "Count adhkar in \(.applicationName)",
                "Adhkar counter in \(.applicationName)",
            ],
            shortTitle: "Count adhkar",
            systemImageName: "sun.and.horizon.fill"
        )
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
            intent: AyahOfDayIntent(),
            phrases: [
                "Ayah of the day in \(.applicationName)",
                "Read me the ayah of the day in \(.applicationName)",
            ],
            shortTitle: "Ayah of the day",
            systemImageName: "text.book.closed.fill"
        )
        AppShortcut(
            intent: PrayerTimesIntent(),
            phrases: [
                "Prayer times in \(.applicationName)",
                "Show prayer times in \(.applicationName)",
            ],
            shortTitle: "Prayer times",
            systemImageName: "calendar.badge.clock"
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
            systemImageName: "bookmark.fill"
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

/// نافذة Siri لآية اليوم: الآية بخط المصحف على ورق المصحف، ومرجعها، ومعناها بلغة التطبيق
@available(iOS 16.0, *)
struct AyahSnippet: View {
    let info: AyahInfo?

    init(info: AyahInfo?) {
        self.info = info
        QuranFontRegistry.register()
    }

    var body: some View {
        if let info = info {
            let gold = Color(snippetHex: info.gold)
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 6) {
                    Image(systemName: "text.book.closed.fill")
                        .font(.system(size: 12, weight: .semibold))
                    Text(info.title)
                        .font(.system(size: 13, weight: .semibold, design: .rounded))
                    Spacer(minLength: 6)
                    Text(info.ref)
                        .font(.system(size: 12, design: .rounded))
                        .foregroundColor(.white.opacity(0.75))
                        .lineLimit(1)
                }
                .foregroundColor(gold)
                Text(verbatim: info.text)
                    .font(.custom("AmiriQuran-Regular", size: 22))
                    .lineSpacing(6)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                if let meaning = info.meaning, !meaning.isEmpty {
                    Text(verbatim: meaning)
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.white.opacity(0.82))
                        .multilineTextAlignment(info.rtl ? .trailing : .leading)
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

/// خط المصحف في نوافذ Siri: يُسجَّل من حزمة التطبيق مرة واحدة (واجهة Compose تحمّله بنفسها من الملف)
enum QuranFontRegistry {
    private static var done = false

    static func register() {
        guard !done else { return }
        done = true
        guard let url = Bundle.main.resourceURL?.appendingPathComponent("assets/fonts/amiri_quran.ttf") else { return }
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
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
