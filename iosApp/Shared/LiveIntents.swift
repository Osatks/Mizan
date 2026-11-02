import Foundation
#if canImport(AppIntents)
import AppIntents
#endif
#if canImport(WidgetKit)
import WidgetKit
#endif

// نوايا الأنشطة الحية: المسبحة وعدّاد الأذكار والعدّ التنازلي للصلاة القادمة. تعمل من زر الإجراء ومركز التحكم
// وأداة الشاشة الرئيسية وأزرار النشاط الحي و Siri، دون فتح التطبيق ولو والجهاز مقفل.
// LiveActivityIntent: يُنفّذها النظام في عملية التطبيق (يُشغَّل في الخلفية إن لم يكن يعمل)، فتعدّ في الحال نفسها
// التي تعدّ فيها شاشة المسبحة (Kotlin) وتبدأ النشاط الحي أو تحدّثه (AppLive في LiveActivities.swift).
// الملف في الهدفين معاً (التطبيق والأدوات)؛ MIZAN_APP معرّف في التطبيق وحده (project.yml).

/// ما تطلبه النوايا من التطبيق
enum LiveAction: String {
    case tasbihCount, tasbihReset, tasbihEnd, tasbihShow
    case adhkarCount, adhkarSkip, adhkarRestart, adhkarEnd
    case prayerToggle
}

/// نتيجة ما طُلب: تمّ، أو الأنشطة الحية متوقفة لميزان في الإعدادات، أو لم يُختر الموقع بعد
enum LiveOutcome {
    case ok, activitiesOff, needsPlace
}

enum LiveRouter {
    /// المسبحة في حاوية App Group: أداة الشاشة الرئيسية وزر مركز التحكم
    static let tasbihWidgetKind = "MizanTasbih"
    static let tasbihControlKind = "com.mizan.almuslim.control.tasbih"
    static let pendingKey = "tasbih.pending"

    static func run(_ action: LiveAction) async -> LiveOutcome {
        #if MIZAN_APP
        if #available(iOS 17.0, *) { return await AppLive.run(action) }
        return .ok
        #else
        // احتياط: لو نُفّذت النية في امتداد الأدوات بدل التطبيق، تُحفظ التسبيحة فيضيفها التطبيق عند أول فرصة
        if action == .tasbihCount { PendingTasbih.add() }
        return .ok
        #endif
    }

    /// تُعاد قراءة حال المسبحة في الأداة وزر مركز التحكم
    static func reloadTasbih() {
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadTimelines(ofKind: tasbihWidgetKind)
        if #available(iOS 18.0, *) { ControlCenter.shared.reloadControls(ofKind: tasbihControlKind) }
        #endif
    }
}

#if !MIZAN_APP
/// تسبيحات عُدّت في امتداد الأدوات (احتياط نادر)، يضيفها التطبيق إلى المسبحة (IosTasbih.applyPending)
enum PendingTasbih {
    static func add() {
        for g in Store.groups() where FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: g) != nil {
            if let d = UserDefaults(suiteName: g) {
                d.set(d.integer(forKey: LiveRouter.pendingKey) + 1, forKey: LiveRouter.pendingKey)
                break
            }
        }
        LiveRouter.reloadTasbih()
    }

    static var count: Int {
        for g in Store.groups() where FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: g) != nil {
            return UserDefaults(suiteName: g)?.integer(forKey: LiveRouter.pendingKey) ?? 0
        }
        return 0
    }
}
#endif

#if canImport(AppIntents)
/// رسالة تظهر للمستخدم حين لا يمكن إظهار النشاط الحي
@available(iOS 16.0, *)
enum LiveIntentError: Error, CustomLocalizedStringResourceConvertible {
    case activitiesOff, needsPlace

    var localizedStringResource: LocalizedStringResource {
        switch self {
        case .activitiesOff: return "Turn on Live Activities for Mizan in Settings"
        case .needsPlace: return "Open Mizan and choose your location first"
        }
    }

    static func check(_ outcome: LiveOutcome) throws {
        switch outcome {
        case .ok: return
        case .activitiesOff: throw LiveIntentError.activitiesOff
        case .needsPlace: throw LiveIntentError.needsPlace
        }
    }
}

// ---------- المسبحة ----------

/// تسبيحة واحدة، والعدّاد على شاشة القفل وفي الجزيرة الديناميكية (زر الإجراء، مركز التحكم، الأداة، زر النشاط، Siri)
@available(iOS 17.0, *)
struct TasbihCountIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Count tasbih"
    static var description = IntentDescription("Counts one tasbih and shows the counter on the Lock Screen and in the Dynamic Island, without opening Mizan")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed

    init() {}

    func perform() async throws -> some IntentResult {
        try LiveIntentError.check(await LiveRouter.run(.tasbihCount))
        return .result()
    }
}

/// تصفير العدد الحالي (زر في النشاط الحي)
@available(iOS 17.0, *)
struct TasbihResetIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Reset tasbih"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        _ = await LiveRouter.run(.tasbihReset)
        return .result()
    }
}

/// يُخفي المسبحة من شاشة القفل والجزيرة (العدد محفوظ)
@available(iOS 17.0, *)
struct TasbihEndIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "End tasbih"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        _ = await LiveRouter.run(.tasbihEnd)
        return .result()
    }
}

// ---------- الأذكار ----------

/// مرة من الذكر الحالي في أذكار الوقت (الصباح من الفجر إلى العصر، والمساء بعده)، وتنتقل إلى التالي عند تمام عدده
@available(iOS 17.0, *)
struct AdhkarCountIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Count adhkar"
    static var description = IntentDescription("Counts the current morning or evening dhikr on the Lock Screen and in the Dynamic Island, moving to the next one when it is complete")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed

    init() {}

    func perform() async throws -> some IntentResult {
        try LiveIntentError.check(await LiveRouter.run(.adhkarCount))
        return .result()
    }
}

/// الذكر التالي دون إتمام الحالي (زر في النشاط الحي)
@available(iOS 17.0, *)
struct AdhkarSkipIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Next dhikr"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        _ = await LiveRouter.run(.adhkarSkip)
        return .result()
    }
}

/// بعد إتمام القسم: من أوله (زر في النشاط الحي)
@available(iOS 17.0, *)
struct AdhkarRestartIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start the adhkar again"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        _ = await LiveRouter.run(.adhkarRestart)
        return .result()
    }
}

/// يُخفي عدّاد الأذكار (الموضع محفوظ لليوم)
@available(iOS 17.0, *)
struct AdhkarEndIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "End adhkar"
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    static var isDiscoverable: Bool = false

    init() {}

    func perform() async throws -> some IntentResult {
        _ = await LiveRouter.run(.adhkarEnd)
        return .result()
    }
}

// ---------- الصلاة القادمة ----------

/// العدّ التنازلي للصلاة القادمة على شاشة القفل وفي الجزيرة الآن، والضغطة التالية تخفيه
@available(iOS 17.0, *)
struct ShowNextPrayerIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Prayer countdown"
    static var description = IntentDescription("Shows or hides the countdown to the next prayer on the Lock Screen and in the Dynamic Island")
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed

    init() {}

    func perform() async throws -> some IntentResult {
        try LiveIntentError.check(await LiveRouter.run(.prayerToggle))
        return .result()
    }
}
#endif
