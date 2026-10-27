import Foundation

// حساب الأداة المستقل (بلا لقطة من التطبيق): نقل حرفي إلى Swift لـ core/PrayTimes.kt و core/Hijri.kt
// وتواريخ kotlinx-datetime التي يستعملها التطبيق، حتى تطابق المواقيت ما يعرضه التطبيق.
// لا يستورد إلا Foundation ليُختبر على لينكس مقابل نتائج Kotlin (WidgetStaticTest يصدّرها).

/// تاريخ مدني بلا منطقة (LocalDate)
struct CivilDate: Hashable, Comparable {
    let y: Int
    let m: Int
    let d: Int

    init(_ y: Int, _ m: Int, _ d: Int) { self.y = y; self.m = m; self.d = d }

    /// من عدد الأيام منذ 1970-01-01 (خوارزمية Howard Hinnant)
    init(epochDay: Int) {
        let z = epochDay + 719_468
        let era = (z >= 0 ? z : z - 146_096) / 146_097
        let doe = z - era * 146_097
        let yoe = (doe - doe / 1460 + doe / 36524 - doe / 146_096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        self.init(yoe + era * 400 + (m <= 2 ? 1 : 0), m, d)
    }

    var epochDay: Int {
        let yy = m <= 2 ? y - 1 : y
        let era = (yy >= 0 ? yy : yy - 399) / 400
        let yoe = yy - era * 400
        let doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    func plus(_ days: Int) -> CivilDate { CivilDate(epochDay: epochDay + days) }

    /// الاثنين 1 … الأحد 7
    var isoWeekday: Int {
        let r = (epochDay + 3) % 7
        return (r < 0 ? r + 7 : r) + 1
    }

    /// 2026-10-27 كما يكتبه LocalDate.toString
    var iso: String { String(format: "%04d-%02d-%02d", y, m, d) }

    /// ثواني بداية اليوم بالتوقيت المحلي «الساذج» (LocalDateTime محسوباً كأنه UTC)
    var startLocal: Int64 { Int64(epochDay) * 86_400 }

    static func < (a: CivilDate, b: CivilDate) -> Bool { a.epochDay < b.epochDay }
}

/// وقت محلي بلا منطقة (LocalDateTime) بالثواني منذ 1970 كأنه UTC، مثل حساب التطبيق
enum LocalTime {
    static func date(_ t: Int64) -> CivilDate {
        CivilDate(epochDay: Int(floorDiv(t, 86_400)))
    }

    static func secondOfDay(_ t: Int64) -> Int64 { t - floorDiv(t, 86_400) * 86_400 }

    static func floorDiv(_ a: Int64, _ b: Int64) -> Int64 {
        let q = a / b
        return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q
    }

    /// يوم + ساعات عشرية (PrayerUtil.at): الثواني مقطوعة نحو الصفر كـ toLong في Kotlin
    static func at(_ day: CivilDate, _ hours: Double) -> Int64 {
        day.startLocal + seconds(hours * 3600)
    }

    /// Double.toLong في Kotlin: NaN صفر، واللانهاية أقصى قيمة
    static func seconds(_ x: Double) -> Int64 {
        if x.isNaN { return 0 }
        if x >= 9.2e18 { return Int64.max }
        if x <= -9.2e18 { return Int64.min }
        return Int64(x)
    }

    /// 2026-10-27T05:00 أو 2026-10-27T05:00:30 كما يكتبه LocalDateTime.toString
    static func iso(_ t: Int64) -> String {
        let s = secondOfDay(t)
        let hm = String(format: "%02d:%02d", Int(s / 3600), Int((s % 3600) / 60))
        let sec = Int(s % 60)
        return "\(date(t).iso)T\(hm)" + (sec != 0 ? String(format: ":%02d", sec) : "")
    }

    /// من نص ISO (للاختبار): 2026-10-27T13:37[:00]
    static func parse(_ text: String) -> Int64? {
        let parts = text.split(separator: "T")
        guard parts.count == 2 else { return nil }
        let d = parts[0].split(separator: "-").compactMap { Int($0) }
        let t = parts[1].split(separator: ":").compactMap { Int($0) }
        guard d.count == 3, t.count >= 2 else { return nil }
        return CivilDate(d[0], d[1], d[2]).startLocal + Int64(t[0] * 3600 + t[1] * 60 + (t.count > 2 ? t[2] : 0))
    }
}

/// تحويل الوقت المحلي إلى لحظة مطلقة بقواعد java.time (ZonedDateTime.ofLocal): في التداخل الإزاحة الأسبق،
/// وفي الفجوة يتقدم الوقت بطولها
enum Zone {
    static func offset(_ tz: TimeZone, at instant: Int64) -> Int64 {
        Int64(tz.secondsFromGMT(for: Date(timeIntervalSince1970: TimeInterval(instant))))
    }

    static func instant(_ local: Int64, _ tz: TimeZone) -> Int64 {
        let before = offset(tz, at: local - 86_400)
        let after = offset(tz, at: local + 86_400)
        if before == after { return local - before }
        if offset(tz, at: local - before) == before { return local - before }
        if offset(tz, at: local - after) == after { return local - after }
        return local - before
    }

    /// الوقت المحلي الآن في المنطقة
    static func local(_ instant: Int64, _ tz: TimeZone) -> Int64 { instant + offset(tz, at: instant) }

    /// فرق المنطقة عن UTC بالساعات ظهر اليوم (tzOffset في PrayTimes.kt)
    static func noonOffsetHours(_ tz: TimeZone, _ day: CivilDate) -> Double {
        let noon = day.startLocal + 12 * 3600
        return Double(offset(tz, at: instant(noon, tz))) / 3600.0
    }
}

/// التاريخ الهجري (أم القرى) من جدول التطبيق نفسه (Hijri.kt، يصدّره WidgetStaticTest)
struct HijriDate: Equatable {
    let y: Int
    let m: Int
    let d: Int
}

struct HijriCalendar {
    let first: Int
    /// بداية كل شهر بعدد الأيام منذ 1970، وعنصر إضافي لنهاية الجدول
    private let starts: [Int]

    init(first: Int, epoch: Int, lengths: String) {
        self.first = first
        var a = [Int]()
        a.reserveCapacity(lengths.utf8.count + 1)
        var s = epoch
        a.append(s)
        for c in lengths.utf8 {
            s += c == UInt8(ascii: "1") ? 30 : 29
            a.append(s)
        }
        starts = a
    }

    func toHijri(_ date: CivilDate) -> HijriDate? {
        let day = date.epochDay
        guard let firstStart = starts.first, let lastStart = starts.last, day >= firstStart, day < lastStart else { return nil }
        var lo = 0
        var hi = starts.count - 2
        while lo < hi {
            let mid = (lo + hi + 1) / 2
            if starts[mid] <= day { lo = mid } else { hi = mid - 1 }
        }
        return HijriDate(y: first + lo / 12, m: lo % 12 + 1, d: day - starts[lo] + 1)
    }
}

/// طريقة حساب (Method في PrayTimes.kt)
struct CalcMethod: Codable {
    let key: String
    let fajr: Double
    let isha: Double?
    let ishaMin: Int?
    let maghrib: Double?
}

/// مواقيت يوم بالساعات العشرية المحلية (PrayerTimes في Kotlin)؛ NaN لما تعذّر حسابه
struct DayTimes {
    var fajr: Double
    var sunrise: Double
    var dhuhr: Double
    var asr: Double
    var maghrib: Double
    var isha: Double
    var sunset: Double = .nan
    var maghribEnd: Double = .nan
    var midnight: Double = .nan

    static let keys = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]

    subscript(key: String) -> Double {
        switch key {
        case "fajr": return fajr
        case "sunrise": return sunrise
        case "dhuhr": return dhuhr
        case "asr": return asr
        case "maghrib": return maghrib
        default: return isha
        }
    }
}

/// حساب مواقيت الصلاة فلكياً من موضع الشمس: نقل compute و format من PrayTimes.kt سطراً بسطر
enum PrayEngine {
    private static func dtr(_ d: Double) -> Double { d * .pi / 180.0 }
    private static func rtd(_ r: Double) -> Double { r * 180.0 / .pi }
    private static func dsin(_ d: Double) -> Double { sin(dtr(d)) }
    private static func dcos(_ d: Double) -> Double { cos(dtr(d)) }
    private static func dtan(_ d: Double) -> Double { tan(dtr(d)) }
    private static func fix(_ a: Double, _ b: Double) -> Double {
        let r = a - b * floor(a / b)
        return r < 0 ? r + b : r
    }
    private static func fixAngle(_ a: Double) -> Double { fix(a, 360.0) }
    static func fixHour(_ a: Double) -> Double { fix(a, 24.0) }

    /// Math.min في Java: NaN إن كان أحدهما NaN
    private static func jmin(_ a: Double, _ b: Double) -> Double {
        if a.isNaN || b.isNaN { return .nan }
        return a < b ? a : b
    }

    private static func julian(_ year: Int, _ month: Int, _ day: Int) -> Double {
        var y = year
        var m = month
        if m <= 2 { y -= 1; m += 12 }
        let a = floor(Double(y) / 100.0)
        let b = 2 - a + floor(a / 4.0)
        return floor(365.25 * Double(y + 4716)) + floor(30.6001 * Double(m + 1)) + Double(day) + b - 1524.5
    }

    private static func sunPosition(_ jd: Double) -> (decl: Double, eqt: Double) {
        let d = jd - 2451545.0
        let g = fixAngle(357.529 + 0.98560028 * d)
        let q = fixAngle(280.459 + 0.98564736 * d)
        let l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        let e = 23.439 - 0.00000036 * d
        let ra = rtd(atan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        let decl = rtd(asin(dsin(e) * dsin(l)))
        let eqt = q / 15.0 - fixHour(ra)
        return (decl, eqt)
    }

    static func compute(_ date: CivilDate, lat: Double, lng: Double, tz: TimeZone, method m: CalcMethod,
                        hanafi: Bool, hijri: HijriCalendar) -> DayTimes {
        let offset = Zone.noonOffsetHours(tz, date)
        let jDate = julian(date.y, date.m, date.d) - lng / (15.0 * 24.0)

        func midDay(_ t: Double) -> Double { fixHour(12 - sunPosition(jDate + t).eqt) }
        func sunAngleTime(_ angle: Double, _ t: Double, ccw: Bool = false) -> Double {
            let decl = sunPosition(jDate + t).decl
            let noon = midDay(t)
            let x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if x < -1 || x > 1 { return .nan }
            let tt = rtd(acos(x)) / 15.0
            return noon + (ccw ? -tt : tt)
        }
        func asrTime(_ factor: Double, _ t: Double) -> Double {
            let decl = sunPosition(jDate + t).decl
            let angle = -rtd(atan(1.0 / (factor + dtan(abs(lat - decl)))))
            return sunAngleTime(angle, t)
        }

        var fajr = 5.0, sunrise = 6.0, dhuhr = 12.0, asr = 13.0, sunset = 18.0, maghrib = 18.0, isha = 18.0
        // مغيب الشفق الأحمر (17°) عند الجمهور، والأبيض (18°) عند الحنفية: نهاية وقت المغرب
        var shafaq = 19.0
        let shafaqAngle = hanafi ? 18.0 : 17.0
        for _ in 0..<2 {
            func p(_ v: Double) -> Double { (v.isNaN ? 12.0 : v) / 24.0 }
            let nf = sunAngleTime(m.fajr, p(fajr), ccw: true)
            let ns = sunAngleTime(0.833, p(sunrise), ccw: true)
            let nd = midDay(p(dhuhr))
            let na = asrTime(hanafi ? 2.0 : 1.0, p(asr))
            let nss = sunAngleTime(0.833, p(sunset))
            let nm = m.maghrib != nil ? sunAngleTime(m.maghrib!, p(maghrib)) : nss
            let ni = m.isha != nil ? sunAngleTime(m.isha!, p(isha)) : Double.nan
            let nsh = sunAngleTime(shafaqAngle, p(shafaq))
            fajr = nf; sunrise = ns; dhuhr = nd; asr = na; sunset = nss; maghrib = nm; isha = ni; shafaq = nsh
        }
        let adj = offset - lng / 15.0
        fajr += adj; sunrise += adj; dhuhr += adj; asr += adj; sunset += adj; maghrib += adj; isha += adj; shafaq += adj
        if maghrib.isNaN { maghrib = sunset }
        if let minutes = m.ishaMin {
            let ramadan = hijri.toHijri(date)?.m == 9 && m.key == "makkah"
            isha = maghrib + Double(ramadan ? 120 : minutes) / 60.0
        }

        // خطوط العرض العليا: طريقة الزاوية
        let night = fixHour(sunrise - sunset + 24)
        let fajrMax = m.fajr / 60.0 * night
        if fajr.isNaN || fixHour(sunrise - fajr) > fajrMax { fajr = sunrise - fajrMax }
        if let angle = m.isha {
            let ishaMax = angle / 60.0 * night
            if isha.isNaN || fixHour(isha - sunset) > ishaMax { isha = sunset + ishaMax }
        }
        dhuhr += 1.0 / 60.0 // احتياط دقيقة

        let maghribEnd = (m.isha != nil || shafaq.isNaN) ? isha : jmin(isha, shafaq)
        let midnight = fajr.isNaN ? Double.nan : sunset + (fajr + 24 - sunset) / 2
        return DayTimes(fajr: fajr, sunrise: sunrise, dhuhr: dhuhr, asr: asr, maghrib: maghrib, isha: isha,
                        sunset: sunset, maghribEnd: maghribEnd, midnight: midnight)
    }

    /// الوقت بصيغة التطبيق: ١٢ ساعة مع ص/م بلغة الواجهة أو ٢٤ ساعة، بأرقام لاتينية
    static func format(_ hours: Double, h24: Bool, am: String = "AM", pm: String = "PM") -> String {
        if hours.isNaN { return "—" }
        let mins = Int(floor(fixHour(hours) * 60 + 0.5)) // roundToInt
        var h = (mins / 60) % 24
        let m = mins % 60
        let mm = m < 10 ? "0\(m)" : "\(m)"
        if h24 { return (h < 10 ? "0\(h)" : "\(h)") + ":" + mm }
        let suf = h < 12 ? am : pm
        h %= 12
        if h == 0 { h = 12 }
        return "\(h):\(mm) \(suf)"
    }
}
