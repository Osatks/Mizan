import Foundation

// حال الأنشطة الحية كما يرسلها Kotlin (IosLive.kt) بإشعار فيه JSON، وهي نفسها ContentState في LiveActivityAttributes.swift.
// هذا الملف لا يستورد إلا Foundation حتى يُختبر فك JSON على لينكس. يُبنى في التطبيق وفي الأداة معاً.
// النصوص كلها منسقة مسبقاً بلغة التطبيق (لا بلغة الجهاز)، والأوقات ثوانٍ منذ ١٩٧٠.

/// العدّ التنازلي للصلاة القادمة
struct PrayerLiveState: Codable, Hashable {
    /// الصلاة وتاريخها: نشاط واحد لكل صلاة
    var id: String
    var key: String
    /// اسم الصلاة: «العصر»
    var name: String
    /// وقتها منسقاً: «3:15 م»
    var time: String
    var at: Date
    /// دخول الصلاة السابقة: بداية شريط الوقت
    var from: Date
    var hijri: String
    var place: String
    /// «الصلاة القادمة»
    var label: String
    /// «حان الآن وقت صلاة العصر»
    var now: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
    /// موعد الإقامة إن ضبط المستخدم لها دقائق: بعد دخول الوقت يعدّ النشاط إليها
    var iqama: Date?
    /// «الإقامة بعد»
    var iqamaLabel: String?
    /// وقت الإقامة منسقاً
    var iqamaTime: String?
    /// دعاء الإفطار عند المغرب في رمضان
    var dua: String?
    /// متى ينتهي النشاط (بعد دخول الوقت بعشر دقائق، أو بعد الإقامة)
    var until: Date?

    /// نهاية النشاط: من Kotlin، وإلا بعد دخول الوقت بعشر دقائق (نشاط بدأه إصدار سابق)
    var endsAt: Date { until ?? at.addingTimeInterval(600) }
}

/// رسالة MizanLivePrayer: on الإعداد مفعّل، و start يُسمح ببدء نشاط جديد (عند مغادرة التطبيق)،
/// و prayer الصلاة القادمة إن بقي عليها أقل من ثلاث ساعات. force: طلبه المستخدم (زر الإجراء)
struct PrayerLiveMessage: Decodable {
    var on: Bool
    var start: Bool?
    var force: Bool?
    var prayer: PrayerLiveState?
}

/// المسبحة: النشاط الحي وأداة الشاشة الرئيسية وزر مركز التحكم (IosTasbih.state في Kotlin)
struct TasbihLiveState: Codable, Hashable {
    var id: String
    /// الذكر بالعربية
    var text: String
    /// معناه بلغة التطبيق (فارغ في العربية)
    var meaning: String
    var count: Int
    /// العدد المطلوب، و 0 بلا حد
    var goal: Int
    /// من 0 إلى 1
    var progress: Double
    var countText: String
    /// «من 33» أو «بلا حد»
    var goalText: String
    var rounds: String
    var roundsLabel: String
    var today: String
    var todayLabel: String
    /// «بعد الصلاة · 2/4» أو فارغ
    var mode: String
    var title: String
    var tap: String
    var reset: String
    var end: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
    var at: Date?
}

/// عدّاد الأذكار (AdhkarLive.state في Kotlin)
struct AdhkarLiveState: Codable, Hashable {
    var id: String
    /// morning أو evening
    var section: String
    var title: String
    var text: String
    var meaning: String
    var count: Int
    var goal: Int
    /// «2/3»
    var countText: String
    /// موضع الذكر في القسم: «5/23»
    var position: String
    var progress: Double
    var done: Bool
    var doneText: String
    var next: String
    /// بعد إتمام القسم: «من جديد» (اختياري: نشاط بدأه إصدار سابق بلا هذا النص)
    var again: String?
    var end: String
    var tap: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
}

/// رسالة من Kotlin بعد عمل في المسبحة أو الأذكار: الحال، وما حدث (none, count, next, round, hundred, done)،
/// ونص التنبيه حين يتم شيء (دورة أو قسم)
struct LiveMessage<State: Decodable>: Decodable {
    var state: State
    var event: String
    var alertTitle: String
    var alertBody: String
}

/// آية اليوم لـ Siri (IosAdhkar.ayahOfDayJson)
struct AyahInfo: Codable, Hashable {
    var title: String
    var text: String
    var ref: String
    var meaning: String?
    var dialog: String
    var route: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
}

/// الصلاة القادمة لنافذة Siri (IosLive.nextPrayerJson)
struct NextPrayerInfo: Codable, Hashable {
    var key: String
    var name: String
    var time: String
    var at: Date
    var hijri: String
    var place: String
    var label: String
    /// العبارة التي تقولها Siri: «الصلاة القادمة: العصر عند 3:15 م»
    var dialog: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
}

enum LiveJSON {
    static var decoder: JSONDecoder {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .secondsSince1970
        return d
    }

    static func prayer(_ json: String) -> PrayerLiveMessage? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? decoder.decode(PrayerLiveMessage.self, from: data)
    }

    static func nextPrayer(_ json: String?) -> NextPrayerInfo? {
        guard let data = json?.data(using: .utf8) else { return nil }
        return try? decoder.decode(NextPrayerInfo.self, from: data)
    }

    static func tasbih(_ json: String?) -> LiveMessage<TasbihLiveState>? {
        guard let data = json?.data(using: .utf8) else { return nil }
        return try? decoder.decode(LiveMessage<TasbihLiveState>.self, from: data)
    }

    /// حال المسبحة كما في ملف حاوية App Group (tasbih.json)
    static func tasbihState(_ data: Data?) -> TasbihLiveState? {
        guard let data = data else { return nil }
        return try? decoder.decode(TasbihLiveState.self, from: data)
    }

    static func adhkar(_ json: String?) -> LiveMessage<AdhkarLiveState>? {
        guard let data = json?.data(using: .utf8) else { return nil }
        return try? decoder.decode(LiveMessage<AdhkarLiveState>.self, from: data)
    }

    static func ayah(_ json: String?) -> AyahInfo? {
        guard let data = json?.data(using: .utf8) else { return nil }
        return try? decoder.decode(AyahInfo.self, from: data)
    }
}
