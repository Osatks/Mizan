import Foundation

// تواريخ وأوقات محلية بلا منطقة زمنية (كما في kotlinx-datetime و core/Dates.kt)، فوق Foundation وحدها

/// تاريخ ميلادي بلا منطقة زمنية؛ epochDay عدد الأيام منذ 1970-01-01
public struct LocalDate: Hashable, Comparable, Codable, CustomStringConvertible, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    /// تاريخ صحيح فقط (الشهر 1…12 واليوم ضمن طول الشهر)؛ يتوقف البرنامج على غيره كما ترمي Kotlin
    public init(_ year: Int, _ month: Int, _ day: Int) {
        precondition(month >= 1 && month <= 12 && day >= 1 && day <= monthLength(year, month), "تاريخ غير صحيح \(year)-\(month)-\(day)")
        self.year = year; self.month = month; self.day = day
    }

    public init(year: Int, month: Int, day: Int) { self.init(year, month, day) }

    /// تاريخ إن كان صحيحاً، وإلا nil
    public static func valid(_ year: Int, _ month: Int, _ day: Int) -> LocalDate? {
        guard month >= 1 && month <= 12 && day >= 1 && day <= monthLength(year, month) else { return nil }
        return LocalDate(year, month, day)
    }

    public init(epochDay: Int) {
        // خوارزمية civil_from_days
        let z = epochDay + 719468
        let era = (z >= 0 ? z : z - 146096) / 146097
        let doe = z - era * 146097
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        self.year = yoe + era * 400 + (m <= 2 ? 1 : 0)
        self.month = m
        self.day = d
    }

    public var epochDay: Int {
        let y = month <= 2 ? year - 1 : year
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let mp = month > 2 ? month - 3 : month + 9
        let doy = (153 * mp + 2) / 5 + day - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /// اسم مألوف كما في Kotlin
    public static func ofEpochDay(_ day: Int) -> LocalDate { LocalDate(epochDay: day) }
    public func toEpochDay() -> Int { epochDay }

    public var monthValue: Int { month }
    public var dayOfMonth: Int { day }

    /// رقم اليوم في الأسبوع: الاثنين 1 … الأحد 7
    public var dayOfWeek: Int {
        let r = (epochDay + 3) % 7
        return (r < 0 ? r + 7 : r) + 1
    }

    /// رقم اليوم في السنة: 1 … 366
    public var dayOfYear: Int { epochDay - LocalDate(year, 1, 1).epochDay + 1 }

    public var isLeapYear: Bool { isLeap(year) }

    public func lengthOfMonth() -> Int { monthLength(year, month) }
    public func lengthOfYear() -> Int { isLeap(year) ? 366 : 365 }

    public func plusDays(_ n: Int) -> LocalDate { LocalDate(epochDay: epochDay + n) }
    public func minusDays(_ n: Int) -> LocalDate { LocalDate(epochDay: epochDay - n) }

    /// إضافة أشهر: اليوم يُقصَّر إلى آخر الشهر إن لزم (31 يناير + شهر = 28/29 فبراير)
    public func plusMonths(_ n: Int) -> LocalDate {
        let total = year * 12 + (month - 1) + n
        let y = floorDiv(total, 12), m = total - y * 12 + 1
        return LocalDate(y, m, min(day, monthLength(y, m)))
    }

    public func minusMonths(_ n: Int) -> LocalDate { plusMonths(-n) }
    public func plusYears(_ n: Int) -> LocalDate { plusMonths(n * 12) }

    public func withDayOfMonth(_ d: Int) -> LocalDate { LocalDate(year, month, d) }

    /// التاريخ نفسه في سنة أخرى (29 فبراير يصير 28 في السنة غير الكبيسة)
    public func withYear(_ y: Int) -> LocalDate { LocalDate(y, month, min(day, monthLength(y, month))) }

    public func isBefore(_ o: LocalDate) -> Bool { self < o }
    public func isAfter(_ o: LocalDate) -> Bool { self > o }

    public func atTime(_ h: Int, _ m: Int, _ s: Int = 0) -> LocalDateTime { LocalDateTime(self, h, m, s) }
    public func atStartOfDay() -> LocalDateTime { LocalDateTime(self, 0, 0, 0) }

    /// اليوم في منطقة زمنية (معرّف IANA)، أو في منطقة الجهاز
    public static func now(_ tz: String? = nil) -> LocalDate { nowIn(tz).date }

    /// النص بصيغة ISO (2026-01-31)
    public var description: String {
        let y = year >= 0 && year < 10000 ? pad(year, 4) : String(year)
        return "\(y)-\(pad(month, 2))-\(pad(day, 2))"
    }

    /// يقرأ تاريخاً بصيغة ISO (2026-01-31)
    public static func parse(_ s: String) -> LocalDate? {
        let parts = s.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3, let y = Int(parts[0]), let m = Int(parts[1]), let d = Int(parts[2]) else { return nil }
        return valid(y, m, d)
    }

    public static func < (a: LocalDate, b: LocalDate) -> Bool {
        (a.year, a.month, a.day) < (b.year, b.month, b.day)
    }

    // يُخزَّن نصاً بصيغة ISO
    public init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        let s = try c.decode(String.self)
        guard let d = LocalDate.parse(s) else {
            throw DecodingError.dataCorruptedError(in: c, debugDescription: "تاريخ غير صحيح: \(s)")
        }
        self = d
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        try c.encode(description)
    }
}

/// وقت محلي بلا منطقة؛ الحساب عليه كأنه UTC فلا يتأثر بالتوقيت الصيفي داخل اليوم
public struct LocalDateTime: Hashable, Comparable, CustomStringConvertible, Sendable {
    public let date: LocalDate
    public let hour: Int
    public let minute: Int
    public let second: Int
    public let nanosecond: Int

    public init(_ date: LocalDate, _ hour: Int, _ minute: Int, _ second: Int = 0, _ nanosecond: Int = 0) {
        precondition(hour >= 0 && hour < 24 && minute >= 0 && minute < 60 && second >= 0 && second < 60 && nanosecond >= 0 && nanosecond < 1_000_000_000)
        self.date = date; self.hour = hour; self.minute = minute; self.second = second; self.nanosecond = nanosecond
    }

    public init(_ year: Int, _ month: Int, _ day: Int, _ hour: Int, _ minute: Int, _ second: Int = 0) {
        self.init(LocalDate(year, month, day), hour, minute, second)
    }

    /// من عدد الثواني منذ 1970-01-01T00:00 (كأنه UTC)
    public init(epochSecond: Int64, nanosecond: Int = 0) {
        let days = floorDiv64(epochSecond, 86400)
        let sod = Int(epochSecond - days * 86400)
        self.init(LocalDate(epochDay: Int(days)), sod / 3600, (sod % 3600) / 60, sod % 60, nanosecond)
    }

    public var year: Int { date.year }
    public var month: Int { date.month }
    public var day: Int { date.day }

    /// الثواني منذ 1970-01-01T00:00 كأنه UTC (بلا كسور الثانية)
    public var epochSecond: Int64 { Int64(date.epochDay) * 86400 + Int64(hour * 3600 + minute * 60 + second) }

    public func toLocalDate() -> LocalDate { date }

    public func plusSeconds(_ n: Int64) -> LocalDateTime { LocalDateTime(epochSecond: epochSecond + n, nanosecond: nanosecond) }
    public func plusSeconds(_ n: Int) -> LocalDateTime { plusSeconds(Int64(n)) }
    public func plusMinutes(_ n: Int) -> LocalDateTime { plusSeconds(Int64(n) * 60) }
    public func plusHours(_ n: Int) -> LocalDateTime { plusSeconds(Int64(n) * 3600) }
    public func plusDays(_ n: Int) -> LocalDateTime { LocalDateTime(date.plusDays(n), hour, minute, second, nanosecond) }

    public func isAfter(_ o: LocalDateTime) -> Bool { self > o }
    public func isBefore(_ o: LocalDateTime) -> Bool { self < o }

    /// اللحظة الحقيقية لهذا الوقت في منطقة زمنية (الوقت الساقط في قفزة التوقيت الصيفي يُقدَّم بطولها،
    /// والمتكرر يأخذ الإزاحة الأولى، كما في kotlinx-datetime)
    public func toDate(_ tz: String) -> Date { toDate(zone: resolveZone(tz)) }

    public func toDate(zone: TimeZone) -> Date {
        let offset = resolveOffset(zone, self)
        return Date(timeIntervalSince1970: Double(epochSecond - Int64(offset)) + Double(nanosecond) / 1e9)
    }

    /// الوقت المحلي للحظة في منطقة زمنية
    public init(_ instant: Date, tz: String) { self.init(instant, zone: resolveZone(tz)) }

    public init(_ instant: Date, zone: TimeZone) {
        let t = instant.timeIntervalSince1970
        let whole = floor(t)
        var nanos = Int(((t - whole) * 1e9).rounded(.down))
        if nanos >= 1_000_000_000 { nanos = 999_999_999 }
        let off = zone.secondsFromGMT(for: instant)
        self.init(epochSecond: Int64(whole) + Int64(off), nanosecond: nanos)
    }

    /// النص بصيغة ISO (2026-01-31T05:07:09)
    public var description: String {
        var s = "\(date)T\(pad(hour, 2)):\(pad(minute, 2))"
        if second != 0 || nanosecond != 0 { s += ":\(pad(second, 2))" }
        if nanosecond != 0 {
            // كما في java.time: ثلاثة أرقام أو ستة أو تسعة
            if nanosecond % 1_000_000 == 0 { s += "." + pad(nanosecond / 1_000_000, 3) }
            else if nanosecond % 1000 == 0 { s += "." + pad(nanosecond / 1000, 6) }
            else { s += "." + pad(nanosecond, 9) }
        }
        return s
    }

    public static func < (a: LocalDateTime, b: LocalDateTime) -> Bool {
        (a.date, a.hour, a.minute, a.second, a.nanosecond) < (b.date, b.hour, b.minute, b.second, b.nanosecond)
    }
}

// ---------- مساعدات (core/Dates.kt) ----------

public func isLeap(_ y: Int) -> Bool { (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 }

/// عدد أيام شهر ميلادي
public func monthLength(_ year: Int, _ month: Int) -> Int {
    switch month {
    case 2: return isLeap(year) ? 29 : 28
    case 4, 6, 9, 11: return 30
    default: return 31
    }
}

/// الأيام بين تاريخين (موجب إن كان b بعد a)
public func daysBetween(_ a: LocalDate, _ b: LocalDate) -> Int { b.epochDay - a.epochDay }

/// المدة بين وقتين محليين بالثواني (بكسورها)
public func durationBetween(_ a: LocalDateTime, _ b: LocalDateTime) -> TimeInterval {
    Double(b.epochSecond - a.epochSecond) + Double(b.nanosecond - a.nanosecond) / 1e9
}

/// المدة بين وقتين محليين بالثواني الكاملة (مقطوعة نحو الصفر، كما في Duration.inWholeSeconds)
public func wholeSecondsBetween(_ a: LocalDateTime, _ b: LocalDateTime) -> Int64 {
    var s = b.epochSecond - a.epochSecond
    let n = b.nanosecond - a.nanosecond
    if s > 0 && n < 0 { s -= 1 } else if s < 0 && n > 0 { s += 1 }
    return s
}

/// الفرق بالسنوات والأشهر والأيام (كما في java.time.Period.between و kotlinx periodUntil)
public struct DatePeriod: Hashable, Sendable {
    public let years: Int
    public let months: Int
    public let days: Int
    public var totalMonths: Int { years * 12 + months }
}

public func periodBetween(_ a: LocalDate, _ b: LocalDate) -> DatePeriod {
    let p1 = (a.year * 12 + a.month - 1) * 32 + a.day
    let p2 = (b.year * 12 + b.month - 1) * 32 + b.day
    let months = (p2 - p1) / 32 // قسمة مقطوعة نحو الصفر
    let days = daysBetween(a.plusMonths(months), b)
    return DatePeriod(years: months / 12, months: months % 12, days: days)
}

/// المنطقة الزمنية من معرّف IANA، أو منطقة الجهاز إن لم يُعرف
public func resolveZone(_ tz: String?) -> TimeZone {
    if let tz = tz, let z = TimeZone(identifier: tz) { return z }
    return TimeZone.current
}

/// فرق المنطقة الزمنية عن UTC بالساعات في وقت محلي معيّن (يتبع التوقيت الصيفي)
public func tzOffsetHours(_ tz: String, _ at: LocalDateTime) -> Double {
    let zone = resolveZone(tz)
    return Double(zone.secondsFromGMT(for: at.toDate(zone: zone))) / 3600.0
}

public func systemTimeZoneId() -> String { TimeZone.current.identifier }

/// الوقت الآن في منطقة زمنية (معرّف IANA مثل Asia/Riyadh)، أو في منطقة الجهاز
public func nowIn(_ tz: String? = nil) -> LocalDateTime { LocalDateTime(Date(), zone: resolveZone(tz)) }

/// إزاحة المنطقة بالثواني لوقت محلي: الصالحة إن كانت واحدة، والأولى في التكرار، والسابقة للقفزة في الفجوة
/// (فتصير اللحظة بعد القفزة بطولها)
private func resolveOffset(_ zone: TimeZone, _ t: LocalDateTime) -> Int {
    let local = Double(t.epochSecond)
    func off(_ utc: Double) -> Int { zone.secondsFromGMT(for: Date(timeIntervalSince1970: utc)) }
    func valid(_ o: Int) -> Bool { off(local - Double(o)) == o }
    let guess = off(local - Double(off(local)))
    let before = off(local - 86400 - 14 * 3600)
    let after = off(local + 86400 + 14 * 3600)
    var candidates: [Int] = []
    for o in [before, guess, after] where valid(o) && !candidates.contains(o) { candidates.append(o) }
    if candidates.count == 1 { return candidates[0] }
    if candidates.count > 1 {
        // التكرار: اللحظة الأبكر (الإزاحة الأكبر)
        return candidates.max()!
    }
    // الفجوة: الإزاحة السابقة للقفزة، ثم تُقرأ الإزاحة عند اللحظة الناتجة
    return before
}

private func floorDiv(_ a: Int, _ b: Int) -> Int { let q = a / b; return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q }
private func floorDiv64(_ a: Int64, _ b: Int64) -> Int64 { let q = a / b; return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q }

func pad(_ v: Int, _ n: Int) -> String {
    let s = String(abs(v))
    return (v < 0 ? "-" : "") + String(repeating: "0", count: max(0, n - s.count)) + s
}
