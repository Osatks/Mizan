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

    /// ما يُعدّ إليه عند [date] (كالنشاط الحي في iPhone، LiveStates.prayer): بعد أذان صلاة لها إقامة يُعدّ إلى الإقامة
    /// بدقائق المستخدم لها، وإلا إلى الصلاة القادمة؛ وفي رمضان المغرب «الإفطار» والفجر «نهاية السحور»
    func phase(at date: Date) -> PrayerPhase? {
        let t = date.timeIntervalSince1970
        if let prev = previousPrayer(before: date), let w = watch, let minutes = w.iqama[prev.k], minutes > 0 {
            let iqama = prev.at + Double(minutes) * 60
            if t < iqama {
                return PrayerPhase(kind: .iqama, label: w.iqamaIn, prayer: prev, from: prev.date, to: Date(timeIntervalSince1970: iqama))
            }
        }
        guard let next = nextPrayer(after: date) else { return nil }
        let from = previousPrayer(before: date).map { p -> Date in
            // بعد الإقامة يبدأ الشريط منها
            let minutes = watch?.iqama[p.k] ?? 0
            return p.date.addingTimeInterval(Double(minutes) * 60)
        } ?? next.date.addingTimeInterval(-3 * 3600)
        if let w = watch, ramadan(next) {
            if next.k == "maghrib" { return PrayerPhase(kind: .iftar, label: w.iftar, prayer: next, from: from, to: next.date) }
            if next.k == "fajr" { return PrayerPhase(kind: .suhoor, label: w.suhoor, prayer: next, from: from, to: next.date) }
        }
        return PrayerPhase(kind: .next, label: text("nextPrayer", "Next prayer"), prayer: next, from: from, to: next.date)
    }

    /// يوم الصلاة من رمضان (الصيام من فجره إلى مغربه)
    func ramadan(_ p: PrayerTime) -> Bool {
        guard let days = watch?.ramadan, !days.isEmpty, let d = day(at: p.date) else { return false }
        return days.contains(d.date)
    }

    /// أوقات تغيّر ما يُعدّ إليه بعد [date] بـ [hours] ساعة: كل أذان وكل إقامة
    func changes(after date: Date, hours: Double = 36) -> [Date] {
        let t0 = date.timeIntervalSince1970
        var out: [Double] = []
        for p in prayers {
            out.append(p.at)
            if let minutes = watch?.iqama[p.k], minutes > 0 { out.append(p.at + Double(minutes) * 60) }
        }
        return out.filter { $0 > t0 && $0 < t0 + hours * 3600 }.sorted().map { Date(timeIntervalSince1970: $0) }
    }

    /// الصلوات من التالية: الثلاث القادمة لمضاعفة مواقيت اليوم
    func upcoming(after date: Date, count: Int) -> [PrayerTime] {
        let t = date.timeIntervalSince1970
        return Array(prayers.filter { $0.at > t }.prefix(count))
    }
}

/// ما تعدّ إليه الساعة الآن
struct PrayerPhase {
    enum Kind { case next, iqama, iftar, suhoor }
    let kind: Kind
    /// «الصلاة القادمة» أو «الإقامة بعد» أو «الإفطار» أو «نهاية السحور»
    let label: String
    /// الصلاة التي يُعدّ إليها (أو إلى إقامتها)
    let prayer: PrayerTime
    /// بداية شريط ما مضى ونهايته (ما يُعدّ إليه)
    let from: Date
    let to: Date

    /// ما مضى من الوقت عند [date] (0 إلى 1)
    func done(at date: Date) -> Double {
        let span = max(1, to.timeIntervalSince(from))
        return min(1, max(0, date.timeIntervalSince(from) / span))
    }

    /// رمز كل حال: SF، أو المسجد (رمز ميزان في Assets) للإقامة
    var symbol: (name: String, custom: Bool) {
        switch kind {
        case .next: return ("moon.stars.fill", false)
        case .iqama: return ("mizan.mosque", true)
        case .iftar: return ("sunset.fill", false)
        case .suhoor: return ("sun.horizon.fill", false)
        }
    }
}
