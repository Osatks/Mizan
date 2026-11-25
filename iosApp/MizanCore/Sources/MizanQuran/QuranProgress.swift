import Foundation
import MizanBase

/// متابعة القراءة والختمة (QuranProgress.kt): الصفحات المقروءة من الختمة الحالية، ومدة الختمة المختارة،
/// وورد اليوم (عدد الصفحات المطلوبة اليوم لإتمام الختمة في موعدها)، وأيام القراءة المتتالية.
/// تُحسب الصفحة مقروءة إذا بقي عليها القارئ ثلاث ثوانٍ. الأيام أرقام من 1970-01-01 (epoch day).
/// تُستعمل من الخيط الرئيسي؛ وتُرسل [changed] بعد كل تغيير.
public enum QuranProgress {
    public static let PAGES = 604
    public static let PLANS = [0, 7, 10, 15, 30, 60]
    public static let changed = Notification.Name("MizanQuranProgressChanged")

    /// اليوم (epoch day بتوقيت الجهاز)؛ قابل للتغيير في الاختبارات
    public static var today: () -> Int = { LocalDate.now().epochDay }

    private static var loaded = false
    private static var read = QuranBits(PAGES + 1)

    public private(set) static var readCount = 0
    public private(set) static var planDays = 30
    /// يوم بدء الختمة (epoch day)
    public private(set) static var start: Int = today()
    public private(set) static var todayCount = 0
    /// الصفحات المقروءة قبل اليوم (لحساب ورد اليوم بثبات طوال اليوم)
    private static var beforeToday = 0
    public private(set) static var streak = 0
    public private(set) static var completed = 0
    private static var lastDay: Int?

    /// يوم بدء الختمة تاريخاً
    public static var startDate: LocalDate { LocalDate(epochDay: start) }

    private static func notify() { NotificationCenter.default.post(name: changed, object: nil) }

    public static func ensure() {
        let S = QuranSettings.self
        // قد تبقى العملية حية إلى اليوم التالي (الأدوات): يبدأ ورد يوم جديد
        if loaded { if S.getString("k_today_date") != QuranDay.format(today()) { rollDay() }; return }
        loaded = true
        read = QuranBits.fromBase64(S.getString("k_read"), PAGES + 1)
        readCount = read.cardinality()
        planDays = S.getInt("k_plan", 30)
        start = QuranDay.parse(S.getString("k_start")) ?? today()
        completed = S.getInt("k_done", 0)
        streak = S.getInt("k_streak", 0)
        lastDay = QuranDay.parse(S.getString("k_last"))
        rollDay()
    }

    /// يُنسى ما في الذاكرة فيُقرأ من المخزن عند الطلب التالي
    public static func reset() { loaded = false }

    /// يعيد القراءة من المخزن (بعد تغيير المخزن، أو حين تكتب الأدوات فيه)
    public static func reload() { loaded = false; ensure(); notify() }

    /// يوم جديد: يبدأ عدّ ورد اليوم من الصفر، وتنقطع السلسلة إن فات يوم دون قراءة
    private static func rollDay() {
        let S = QuranSettings.self
        let t = today()
        if S.getString("k_today_date") == QuranDay.format(t) {
            todayCount = S.getInt("k_today", 0)
            beforeToday = S.getInt("k_before", readCount)
        } else {
            todayCount = 0
            beforeToday = readCount
            S.putString("k_today_date", QuranDay.format(t))
            S.putInt("k_today", 0)
            S.putInt("k_before", beforeToday)
        }
        if let last = lastDay, t - last > 1 { streak = 0; S.putInt("k_streak", 0) }
    }

    public static func isRead(_ page: Int) -> Bool { read[page] }

    public static func markRead(_ page: Int) {
        ensure()
        if page < 1 || page > PAGES || read[page] { return }
        let S = QuranSettings.self
        if S.getString("k_today_date") != QuranDay.format(today()) { rollDay() }
        read.set(page)
        readCount = read.cardinality()
        todayCount += 1
        let t = today()
        if lastDay != t {
            streak = (lastDay.map { t - $0 == 1 } ?? false) ? streak + 1 : 1
            lastDay = t
            S.putInt("k_streak", streak)
            S.putString("k_last", QuranDay.format(t))
        }
        if readCount >= PAGES { completed += 1; S.putInt("k_done", completed) }
        save()
        notify()
    }

    private static func save() {
        QuranSettings.putString("k_read", read.toBase64())
        QuranSettings.putInt("k_today", todayCount)
    }

    public static var finished: Bool { readCount >= PAGES }

    /// ختمة جديدة بالمدة المختارة (0 = بلا مدة محددة)
    public static func restart(_ days: Int? = nil) {
        ensure()
        let days = days ?? planDays
        read = QuranBits(PAGES + 1); readCount = 0
        planDays = days; start = today()
        todayCount = 0; beforeToday = 0
        QuranSettings.putInt("k_plan", days)
        QuranSettings.putString("k_start", QuranDay.format(start))
        QuranSettings.putInt("k_before", 0)
        save()
        notify()
    }

    public static func changePlan(_ days: Int) { ensure(); planDays = days; QuranSettings.putInt("k_plan", days); notify() }

    /// الأيام الباقية في الخطة (اليوم منها)
    public static func daysLeft() -> Int { planDays == 0 ? 0 : max(planDays - (today() - start), 1) }

    /// صفحات ورد اليوم: ما بقي من الختمة عند بداية اليوم مقسوماً على الأيام الباقية
    public static func dailyGoal() -> Int {
        planDays == 0 ? 0 : Int((Double(max(PAGES - beforeToday, 0)) / Double(daysLeft())).rounded(.up))
    }

    /// أول صفحة لم تُقرأ (لمتابعة الورد)
    public static func nextUnread() -> Int { (1...PAGES).first { !read[$0] } ?? 1 }

    // ---------- مساعدات الواجهة ----------

    /// نسبة ورد اليوم (أو الختمة كلها إن لم تكن للختمة مدة)، كما في todayFraction (ReadingControls.kt)
    public static func todayFraction() -> Float {
        let goal = dailyGoal()
        return goal > 0 ? min(max(Float(todayCount) / Float(goal), 0), 1) : Float(readCount) / Float(PAGES)
    }

    /// نسبة ما قُرئ من صفحات السورة في الختمة الحالية (surahProgress في QuranScreen.kt)
    public static func surahProgress(_ n: Int) -> Float {
        let first = QuranMeta.pageOf(QuranMeta.STARTS[n - 1])
        let last = QuranMeta.pageOf(QuranMeta.STARTS[n - 1] + QuranMeta.COUNTS[n - 1] - 1)
        return Float((first...last).filter { isRead($0) }.count) / Float(last - first + 1)
    }

    /// موضع متابعة الورد: أول آية في أول صفحة لم تُقرأ
    public static func nextUnreadAyah() -> (surah: Int, ayah: Int) { QuranMeta.locate(QuranMeta.PAGES[nextUnread() - 1]) }
}

// MARK: - مجموعة البتات

/// مجموعة بتات بصيغة java.util.BitSet نفسها عند الحفظ (البت i في البايت i/8)، كـ Bits.kt
public struct QuranBits {
    private(set) var bytes: [UInt8]

    public init(_ size: Int) { bytes = [UInt8](repeating: 0, count: (size + 7) / 8) }

    public subscript(i: Int) -> Bool {
        // كما في Kotlin: من -7 إلى -1 يُقرأ البت السابع من البايت الأول (إزاحة بعدد سالب)
        if i < 0 { return i > -8 && !bytes.isEmpty && bytes[0] & 0x80 != 0 }
        return i / 8 < bytes.count && (bytes[i / 8] >> UInt8(i % 8)) & 1 == 1
    }

    public mutating func set(_ i: Int) {
        if i / 8 >= bytes.count { bytes += [UInt8](repeating: 0, count: i / 8 + 1 - bytes.count) }
        bytes[i / 8] |= UInt8(1 << (i % 8))
    }

    public func cardinality() -> Int { bytes.reduce(0) { $0 + $1.nonzeroBitCount } }

    /// كما في BitSet.toByteArray: تُحذف البايتات الصفرية في الآخر
    public func toByteArray() -> [UInt8] {
        var n = bytes.count
        while n > 0 && bytes[n - 1] == 0 { n -= 1 }
        return Array(bytes[0..<n])
    }

    public func toBase64() -> String { QuranBase64.encode(toByteArray()) }

    public static func valueOf(_ b: [UInt8], _ size: Int) -> QuranBits {
        var r = QuranBits(0)
        r.bytes = b + [UInt8](repeating: 0, count: max(0, (size + 7) / 8 - b.count))
        return r
    }

    public static func fromBase64(_ s: String, _ size: Int) -> QuranBits {
        guard let b = QuranBase64.decode(s) else { return QuranBits(size) }
        return valueOf(b, size)
    }
}

/// Base64 كـ kotlin.io.encoding.Base64.Default: الحشو مطلوب، ولا محارف خارج الأبجدية، وبتات الحشو أصفار
enum QuranBase64 {
    private static let alphabet = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".utf8)
    private static let values: [Int] = {
        var v = [Int](repeating: -1, count: 256)
        for (i, c) in alphabet.enumerated() { v[Int(c)] = i }
        return v
    }()

    static func encode(_ b: [UInt8]) -> String {
        var out: [UInt8] = []
        var i = 0
        while i + 3 <= b.count {
            let n = Int(b[i]) << 16 | Int(b[i + 1]) << 8 | Int(b[i + 2])
            out += [alphabet[n >> 18 & 63], alphabet[n >> 12 & 63], alphabet[n >> 6 & 63], alphabet[n & 63]]
            i += 3
        }
        let r = b.count - i
        if r == 1 {
            let n = Int(b[i]) << 16
            out += [alphabet[n >> 18 & 63], alphabet[n >> 12 & 63], 61, 61]
        } else if r == 2 {
            let n = Int(b[i]) << 16 | Int(b[i + 1]) << 8
            out += [alphabet[n >> 18 & 63], alphabet[n >> 12 & 63], alphabet[n >> 6 & 63], 61]
        }
        return String(decoding: out, as: UTF8.self)
    }

    static func decode(_ s: String) -> [UInt8]? {
        let c = Array(s.utf8)
        if c.count % 4 != 0 { return nil }
        var out: [UInt8] = []
        var i = 0
        while i < c.count {
            let last = i + 4 == c.count
            var v = [Int](repeating: 0, count: 4)
            var pad = 0
            for k in 0..<4 {
                let ch = c[i + k]
                if ch == 61 {
                    // الحشو في آخر النص فقط، وبعد محرفين على الأقل
                    guard last, k >= 2 else { return nil }
                    pad += 1
                    v[k] = 0
                } else {
                    guard pad == 0 else { return nil }
                    let x = values[Int(ch)]
                    guard x >= 0 else { return nil }
                    v[k] = x
                }
            }
            let n = v[0] << 18 | v[1] << 12 | v[2] << 6 | v[3]
            switch pad {
            case 0: out += [UInt8(n >> 16 & 255), UInt8(n >> 8 & 255), UInt8(n & 255)]
            case 1:
                guard v[2] & 3 == 0 else { return nil }
                out += [UInt8(n >> 16 & 255), UInt8(n >> 8 & 255)]
            default:
                guard v[1] & 15 == 0 else { return nil }
                out += [UInt8(n >> 16 & 255)]
            }
            i += 4
        }
        return out
    }
}

// MARK: - الأيام

/// أيام التقويم الميلادي كأرقام من 1970-01-01، وصيغة LocalDate في Kotlin (yyyy-MM-dd)
public enum QuranDay {
    /// اليوم بتوقيت الجهاز
    public static func today(_ date: Date = Date(), timeZone: TimeZone = .current) -> Int {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = timeZone
        let c = cal.dateComponents([.year, .month, .day], from: date)
        return epochDay(c.year!, c.month!, c.day!)
    }

    public static func epochDay(_ y: Int, _ m: Int, _ d: Int) -> Int {
        let yy = m <= 2 ? y - 1 : y
        let era = (yy >= 0 ? yy : yy - 399) / 400
        let yoe = yy - era * 400
        let mp = (m + 9) % 12
        let doy = (153 * mp + 2) / 5 + d - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    public static func civil(_ day: Int) -> (year: Int, month: Int, day: Int) {
        let z = day + 719468
        let era = (z >= 0 ? z : z - 146096) / 146097
        let doe = z - era * 146097
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        return (yoe + era * 400 + (m <= 2 ? 1 : 0), m, d)
    }

    /// LocalDate.toString
    public static func format(_ day: Int) -> String {
        let (y, m, d) = civil(day)
        let ys: String
        if y > 9999 { ys = "+" + String(y) }
        else if y < 0 { ys = "-" + pad(-y, 4) }
        else { ys = pad(y, 4) }
        return ys + "-" + pad(m, 2) + "-" + pad(d, 2)
    }

    /// LocalDate.parse بصيغة ISO الصارمة (yyyy-MM-dd)، أو nil
    public static func parse(_ s: String) -> Int? {
        let c = Array(s.utf8)
        guard c.count == 10, c[4] == 45, c[7] == 45 else { return nil }
        func num(_ r: Range<Int>) -> Int? {
            var v = 0
            for i in r { guard c[i] >= 48 && c[i] <= 57 else { return nil }; v = v * 10 + Int(c[i] - 48) }
            return v
        }
        guard let y = num(0..<4), let m = num(5..<7), let d = num(8..<10), m >= 1, m <= 12, d >= 1 else { return nil }
        let leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        let dim = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1]
        guard d <= dim else { return nil }
        return epochDay(y, m, d)
    }

    private static func pad(_ v: Int, _ w: Int) -> String {
        let s = String(v)
        return s.count >= w ? s : String(repeating: "0", count: w - s.count) + s
    }
}
