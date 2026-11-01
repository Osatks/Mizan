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
}

/// «يُتلى الآن»
struct RecitationLiveState: Codable, Hashable {
    var playing: Bool
    var surah: Int
    var ayah: Int
    /// عدد آيات السورة
    var count: Int
    /// «البقرة · الآية 255»
    var title: String
    /// اسم السورة بالعربية (يُعرض بخط المصحف)
    var surahAr: String
    var reciter: String
    /// «يُتلى الآن»
    var label: String
    var play: String
    var pause: String
    /// صفحة الآية في التطبيق (mizan://open?route=…)
    var route: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
}

/// رسالة MizanLivePrayer: on الإعداد مفعّل، و start يُسمح ببدء نشاط جديد (عند مغادرة التطبيق)،
/// و prayer الصلاة القادمة إن بقي عليها أقل من ثلاث ساعات
struct PrayerLiveMessage: Decodable {
    var on: Bool
    var start: Bool?
    var prayer: PrayerLiveState?
}

/// رسالة MizanLiveRecitation: active، ثم حال التلاوة في المستوى نفسه إن كانت تعمل
struct RecitationLiveMessage: Decodable {
    var active: Bool
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

    /// nil: الرسالة لا تُقرأ؛ ‎.some(nil)‎: انتهت التلاوة
    static func recitation(_ json: String) -> RecitationLiveState?? {
        guard let data = json.data(using: .utf8),
              let message = try? decoder.decode(RecitationLiveMessage.self, from: data) else { return nil }
        guard message.active else { return .some(nil) }
        guard let state = try? decoder.decode(RecitationLiveState.self, from: data) else { return nil }
        return .some(state)
    }

    static func nextPrayer(_ json: String?) -> NextPrayerInfo? {
        guard let data = json?.data(using: .utf8) else { return nil }
        return try? decoder.decode(NextPrayerInfo.self, from: data)
    }
}
