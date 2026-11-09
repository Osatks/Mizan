import Foundation

/// مناسبة إسلامية سنوية بتاريخها الهجري (ui/screens/DateScreens.kt)
public struct IslamicEvent: Hashable, Sendable {
    public let m: Int
    public let d: Int
    public let key: String
    public let emoji: String
    public init(_ m: Int, _ d: Int, _ key: String, _ emoji: String) { self.m = m; self.d = d; self.key = key; self.emoji = emoji }
}

/// حسابات شاشات التاريخ: المناسبات القادمة، والعمر بالهجري، وعيد الميلاد القادم
public enum DateCalc {
    public static let EVENTS: [IslamicEvent] = [
        IslamicEvent(9, 1, "ramadan", "🌙"),
        IslamicEvent(9, 21, "lastTen", "✨"),
        IslamicEvent(10, 1, "eidFitr", "🎉"),
        IslamicEvent(10, 2, "shawwal", "📿"),
        IslamicEvent(12, 1, "dhulHijjah", "🕋"),
        IslamicEvent(12, 9, "arafah", "🤲"),
        IslamicEvent(12, 10, "eidAdha", "🐑"),
        IslamicEvent(1, 1, "newYear", "📅"),
        IslamicEvent(1, 10, "ashura", "🌊"),
    ]

    /// المناسبات القادمة (اليوم أو بعده) مرتبة بتاريخها: المناسبة، وتاريخها الميلادي، وسنتها الهجرية
    public static func upcomingEvents(_ today: LocalDate) -> [(event: IslamicEvent, date: LocalDate, hijriYear: Int)] {
        guard let th = Hijri.toHijri(today) else { return [] }
        var out: [(event: IslamicEvent, date: LocalDate, hijriYear: Int)] = []
        for e in EVENTS {
            for y in [th.y, th.y + 1, th.y + 2] {
                if let g = Hijri.toGregorian(y, e.m, e.d), !g.isBefore(today) { out.append((e, g, y)); break }
            }
        }
        // sortedBy مستقر
        return out.enumerated().sorted { a, b in a.element.date != b.element.date ? a.element.date < b.element.date : a.offset < b.offset }.map { $0.element }
    }

    /// العمر بالتقويم الهجري (سنوات وأشهر وأيام)، أو nil إن خرج أحد التاريخين عن الجدول
    public static func hijriAge(_ birth: LocalDate, _ today: LocalDate) -> (years: Int, months: Int, days: Int)? {
        guard let hb = Hijri.toHijri(birth), let ht = Hijri.toHijri(today) else { return nil }
        var yy = ht.y - hb.y, mm = ht.m - hb.m, dd = ht.d - hb.d
        if dd < 0 {
            mm -= 1
            let pm = ht.m == 1 ? 12 : ht.m - 1, py = ht.m == 1 ? ht.y - 1 : ht.y
            dd += Hijri.monthLength(py, pm)
        }
        if mm < 0 { yy -= 1; mm += 12 }
        return (yy, mm, dd)
    }

    /// الأيام إلى عيد الميلاد القادم (صفر إن كان اليوم)
    public static func daysToNextBirthday(_ birth: LocalDate, _ today: LocalDate) -> Int {
        var nextB = birth.withYear(today.year)
        if nextB.isBefore(today) { nextB = nextB.plusYears(1) }
        return daysBetween(today, nextB)
    }

    /// تاريخ الميلاد من حقول المستخدم في شاشة العمر: هجري (1300–1600) أو ميلادي (1800–2200، واليوم يُقصَّر إلى آخر الشهر)
    public static func birth(hijri: Bool, year: Int, month: Int, day: Int) -> LocalDate? {
        if hijri { return Hijri.toGregorian(year, month, day) }
        guard year >= 1800 && year <= 2200, month >= 1 && month <= 12 else { return nil }
        return LocalDate.valid(year, month, min(day, monthLength(year, month)))
    }
}
