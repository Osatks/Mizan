import Foundation
import MizanBase

// موقع المستخدم (Place) والإقامة الافتراضية (IQAMA_DEFAULT) وأذكار الوقت (AdhkarClock) في MizanContent؛
// هنا ما يحتاج المدن وطرق الحساب.

extension PrayTimes {
    /// الطريقة الافتراضية لمكان: طريقة مدينته المعروفة، وإلا طريقة أقرب مدينة (AppSettings.defaultMethod)
    public static func defaultMethod(cityKey: String?, lat: Double, lng: Double) -> String {
        city(cityKey)?.method ?? nearestCity(lat, lng).0.method
    }
}

/// يحوّل إحداثيات الجهاز إلى موقع: أقرب مدينة معروفة إن كانت قريبة، وإلا «موقعي الحالي» (key = nil).
/// يُبنى منه Place في MizanContent مع gps = true ومنطقة الجهاز الزمنية.
public enum LocationHelper {
    public static func placeFrom(_ latitude: Double, _ longitude: Double) -> (key: String?, lat: Double, lng: Double, method: String, elev: Int) {
        let lat = (latitude * 10000).rounded(.toNearestOrEven) / 10000.0
        let lng = (longitude * 10000).rounded(.toNearestOrEven) / 10000.0
        let (city, dist) = PrayTimes.nearestCity(lat, lng)
        let near = dist < 1.5
        return (near ? city.key : nil, lat, lng, city.method, PrayTimes.elevationNear(lat, lng))
    }
}

/// الصلاة القادمة: مفتاحها، ووقتها المحلي، والمتبقي إليها بالثواني
public struct NextPrayer: Hashable, Sendable {
    public let key: String
    public let at: LocalDateTime
    /// المتبقي بالثواني (بكسورها)
    public let remaining: TimeInterval
    public init(key: String, at: LocalDateTime, remaining: TimeInterval) { self.key = key; self.at = at; self.remaining = remaining }
    /// المتبقي بالثواني الكاملة (Duration.inWholeSeconds)
    public var remainingSeconds: Int64 { kotlinToLong(remaining) }
}

/// إعدادات الحساب: إحداثيات الموقع ومنطقته وارتفاعه، والطريقة والعصر (AppSettings: place و method و hanafi)
public struct PrayerConfig: Hashable, Sendable {
    public let lat: Double
    public let lng: Double
    public let tz: String
    public let elev: Int
    public let method: String
    public let hanafi: Bool

    public init(lat: Double, lng: Double, tz: String, elev: Int = 0, method: String, hanafi: Bool) {
        self.lat = lat; self.lng = lng; self.tz = tz; self.elev = elev; self.method = method; self.hanafi = hanafi
    }

    /// [hanafiChoice]: اختيار المستخدم للعصر، و nil يتبع المعتمد رسمياً في الطريقة (AppSettings.hanafi)
    public init(lat: Double, lng: Double, tz: String, elev: Int = 0, method: String, hanafiChoice: Bool?) {
        self.init(lat: lat, lng: lng, tz: tz, elev: elev, method: method, hanafi: hanafiChoice ?? PrayTimes.method(method).hanafi)
    }

    /// مدينة معروفة بطريقتها الافتراضية
    public init(city c: City, method: String? = nil, hanafiChoice: Bool? = nil) {
        self.init(lat: c.lat, lng: c.lng, tz: c.tz, elev: c.elev, method: method ?? c.method, hanafiChoice: hanafiChoice)
    }
}

/// مساعدات الصلاة القادمة والحاضرة (ui/screens/PrayerUtil.kt)
public enum PrayerUtil {
    /// الوقت الآن في منطقة الموقع المختار
    public static func now(_ c: PrayerConfig) -> LocalDateTime { nowIn(c.tz) }

    public static func times(_ day: LocalDate, _ c: PrayerConfig) -> PrayerTimes {
        PrayTimes.compute(day, c.lat, c.lng, c.tz, c.method, c.hanafi, elev: c.elev)
    }

    /// الوقت المحلي لساعات عشرية من يوم (الكسر دون الثانية يُبتر)
    public static func at(_ day: LocalDate, _ hours: Double) -> LocalDateTime {
        day.atStartOfDay().plusSeconds(kotlinToLong(hours * 3600))
    }

    /// الصلاة القادمة بعد [now] (اليوم ثم الغد)، وإلا الفجر بعد ساعة
    public static func next(_ c: PrayerConfig, now: LocalDateTime) -> NextPrayer {
        for offset in 0...1 {
            let day = now.date.plusDays(offset)
            let t = times(day, c)
            for key in PrayTimes.PRAYERS {
                if key == "sunrise" || t[key].isNaN { continue }
                let dt = at(day, t[key])
                if dt.isAfter(now) { return NextPrayer(key: key, at: dt, remaining: durationBetween(now, dt)) }
            }
        }
        return NextPrayer(key: "fajr", at: now.plusHours(1), remaining: 3600)
    }

    public static func next(_ c: PrayerConfig) -> NextPrayer { next(c, now: now(c)) }

    /// الصلاة الحاضرة في مواقيت اليوم عند [secondOfDay] (ثواني اليوم): آخر صلاة دخل وقتها حتى الوقت
    /// الذي بعدها (الشروق ينهي الفجر)، أو nil (بين الشروق والظهر، أو قبل الفجر). كما في شبكة مواقيت الرئيسية
    public static func current(_ times: PrayerTimes, secondOfDay sec: Double) -> String? {
        let keys = PrayTimes.PRAYERS.filter { !times[$0].isNaN }
        guard let cur = keys.last(where: { $0 != "sunrise" && times[$0] * 3600 <= sec }) else { return nil }
        let i = keys.firstIndex(of: cur)!
        let after: String? = i + 1 < keys.count ? keys[i + 1] : nil
        return (after == nil || sec < times[after!] * 3600) ? cur : nil
    }

    /// ثواني اليوم الكاملة لوقت محلي (LocalTime.toSecondOfDay)
    public static func secondOfDay(_ t: LocalDateTime) -> Double {
        Double(t.hour * 3600 + t.minute * 60 + t.second)
    }

    /// ساعات:دقائق:ثوانٍ بأرقام لاتينية دائماً
    public static func hms(_ seconds: Int64) -> String {
        let s = max(seconds, 0)
        return "\(Fmt.two(s / 3600)):\(Fmt.two((s % 3600) / 60)):\(Fmt.two(s % 60))"
    }

    /// ساعات:دقائق:ثوانٍ لمدة بالثواني (تُبتر الكسور كما في Duration.inWholeSeconds)
    public static func hms(_ d: TimeInterval) -> String { hms(kotlinToLong(d)) }

    /// وقت الإقامة بالساعات العشرية: الأذان + [minutes] دقيقة، أو nil إن كانت صفراً أو تعذّر الوقت
    public static func iqama(_ times: PrayerTimes, _ key: String, minutes: Int) -> Double? {
        let t = times[key]
        return minutes > 0 && !t.isNaN ? t + Double(minutes) / 60.0 : nil
    }

    /// جدول الشهر: مواقيت كل يوم من شهر [day] (للصلوات الخمس في شاشة المواقيت)
    public static func month(_ day: LocalDate, _ c: PrayerConfig) -> [(LocalDate, PrayerTimes)] {
        let first = LocalDate(day.year, day.month, 1)
        return (0..<first.lengthOfMonth()).map { i in
            let d = first.plusDays(i)
            return (d, times(d, c))
        }
    }
}
