import Foundation

// ما يرسله iPhone إلى الساعة (WatchSync.swift في التطبيق): لقطة المواقيت نفسها التي تقرؤها الأدوات بلا الأذكار،
// ونصوص المسبحة بلغة التطبيق. يحفظها تطبيق الساعة في حاوية App Group فيقرؤها وجه الساعة (MizanWatchWidgets).
// الملف في الهدفين معاً (تطبيق الساعة ومضاعفاتها).

struct WatchPayload: Codable {
    var snapshot: Snapshot
    var tasbih: TasbihLabels?
}

/// نصوص المسبحة في الساعة بلغة التطبيق
struct TasbihLabels: Codable {
    var title: String
    var reset: String
    /// «من %d»، أو nil فيُكتب «21/33»
    var of: String?
    var today: String
    var rounds: String
}

enum WatchData {
    static let group = "group.com.mizan.almuslim"
    static let file = "watch.json"

    private static var url: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group)?.appendingPathComponent(file)
    }

    static func load() -> WatchPayload? {
        let data = url.flatMap { try? Data(contentsOf: $0) } ?? UserDefaults.standard.data(forKey: file)
        guard let data = data else { return nil }
        return try? JSONDecoder().decode(WatchPayload.self, from: data)
    }

    static func save(_ data: Data) {
        if let url = url { try? data.write(to: url, options: .atomic) }
        // احتياط إن لم تتح الحاوية (يقرؤه التطبيق وحده)
        UserDefaults.standard.set(data, forKey: file)
    }

    /// لغة الساعة نفسها حين لا لقطة بعد
    static var arabicDevice: Bool { Locale.preferredLanguages.first?.hasPrefix("ar") ?? false }
}

extension Snapshot {
    func text(_ key: String, _ fallback: String) -> String { labels[key] ?? fallback }

    /// الصلاة السابقة والقادمة حول [date]: بداية شريط الوقت ونهايته
    func around(_ date: Date) -> (from: PrayerTime?, next: PrayerTime?) {
        (previousPrayer(before: date), nextPrayer(after: date))
    }
}
