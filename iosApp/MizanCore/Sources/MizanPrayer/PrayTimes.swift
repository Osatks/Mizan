import Foundation
import MizanBase

/// طريقة حساب: زاوية الفجر، وزاوية العشاء أو مدته بعد المغرب، و [maghribAngle] للطرق التي تؤخر المغرب بزاوية (طهران).
/// [minutes]: ما تضيفه الجهة الرسمية من دقائق إلى كل وقت بعد حسابه (احتياط وتقريب)؛ مفاتيحها fajr و sunrise و dhuhr
/// و asr و maghrib و isha، والغائب صفر. [hanafi]: العصر بمثلَي الظل هو المعتمد رسمياً. [elevation]: معامل انخفاض الأفق
/// للمرتفع (1 للصيغة المعتادة، وصفر لمن لا يعتمد الارتفاع). [dailySun]: موضع الشمس مرة لليوم عند منتصف الليل بتوقيت غرينتش.
/// العروض العليا: [fajrFallback] و [ishaFallback] مدة ثابتة بالدقائق؛ [seasonal] حدّ لجنة رؤية الهلال الموسمي؛
/// [depression] زاويتا الفجر والعشاء نسبة من أعمق انخفاض للشمس (مؤتمر إسطنبول 2021)؛ وإلا طريقة الزاوية.
/// [ramadan]: دقائق تزيدها الجهة في رمضان وحده. (التفاصيل في core/PrayTimes.kt)
public struct Method: Hashable, Sendable {
    public let key: String
    public let fajr: Double
    public let ishaAngle: Double?
    public let ishaMinutes: Int?
    public let maghribAngle: Double?
    public let minutes: [String: Double]
    public let hanafi: Bool
    public let elevation: Double
    public let dailySun: Bool
    public let fajrFallback: Double?
    public let ishaFallback: Double?
    public let seasonal: Bool
    public let depression: (Double, Double)?
    public let ramadan: [String: Double]

    public init(
        _ key: String, _ fajr: Double, _ ishaAngle: Double?, _ ishaMinutes: Int?, _ maghribAngle: Double? = nil,
        minutes: [String: Double] = Method.dhuhrPrecaution, hanafi: Bool = false, elevation: Double = 0.0,
        dailySun: Bool = false, fajrFallback: Double? = nil, ishaFallback: Double? = nil,
        seasonal: Bool = false, depression: (Double, Double)? = nil, ramadan: [String: Double] = [:]
    ) {
        self.key = key; self.fajr = fajr; self.ishaAngle = ishaAngle; self.ishaMinutes = ishaMinutes
        self.maghribAngle = maghribAngle; self.minutes = minutes; self.hanafi = hanafi; self.elevation = elevation
        self.dailySun = dailySun; self.fajrFallback = fajrFallback; self.ishaFallback = ishaFallback
        self.seasonal = seasonal; self.depression = depression; self.ramadan = ramadan
    }

    /// دقائق الجهة لصلاة (وزيادتها في رمضان)
    public func minutes(_ prayer: String, inRamadan: Bool = false) -> Double {
        (minutes[prayer] ?? 0.0) + (inRamadan ? ramadan[prayer] ?? 0.0 : 0.0)
    }

    /// ما لم يُطابَق بجدول رسمي: دقيقة احتياط للظهر وحده
    public static let dhuhrPrecaution: [String: Double] = ["dhuhr": 1.0]
    /// الجدول الرسمي هو الحساب الفلكي نفسه، بلا دقائق
    public static let none: [String: Double] = [:]

    public static func == (a: Method, b: Method) -> Bool {
        a.key == b.key && a.fajr == b.fajr && a.ishaAngle == b.ishaAngle && a.ishaMinutes == b.ishaMinutes
            && a.maghribAngle == b.maghribAngle && a.minutes == b.minutes && a.hanafi == b.hanafi && a.elevation == b.elevation
            && a.dailySun == b.dailySun && a.fajrFallback == b.fajrFallback && a.ishaFallback == b.ishaFallback
            && a.seasonal == b.seasonal && a.depression?.0 == b.depression?.0 && a.depression?.1 == b.depression?.1
            && a.ramadan == b.ramadan
    }

    public func hash(into h: inout Hasher) { h.combine(key) }
}

/// مواقيت يوم بالساعات العشرية بتوقيت المكان (NaN لما تعذّر حسابه)
public struct PrayerTimes: Equatable, Sendable {
    public let fajr: Double
    public let sunrise: Double
    public let dhuhr: Double
    public let asr: Double
    public let maghrib: Double
    public let isha: Double
    /// الغروب، ونهاية وقت المغرب (مغيب الشفق)، ومنتصف الليل (نهاية وقت الاختيار للعشاء)؛ NaN إن لم تُحسب
    public let sunset: Double
    public let maghribEnd: Double
    public let midnight: Double

    public init(
        fajr: Double, sunrise: Double, dhuhr: Double, asr: Double, maghrib: Double, isha: Double,
        sunset: Double = .nan, maghribEnd: Double = .nan, midnight: Double = .nan
    ) {
        self.fajr = fajr; self.sunrise = sunrise; self.dhuhr = dhuhr; self.asr = asr; self.maghrib = maghrib; self.isha = isha
        self.sunset = sunset; self.maghribEnd = maghribEnd; self.midnight = midnight
    }

    public subscript(key: String) -> Double {
        switch key {
        case "fajr": return fajr
        case "sunrise": return sunrise
        case "dhuhr": return dhuhr
        case "asr": return asr
        case "maghrib": return maghrib
        default: return isha
        }
    }

    /// نهاية وقت الصلاة: الفجر بطلوع الشمس، والظهر بدخول العصر، والعصر بالغروب، والمغرب بمغيب الشفق،
    /// والعشاء (وقت الاختيار) بمنتصف الليل. NaN للشروق أو حين لا يمكن الحساب.
    public func end(_ key: String) -> Double {
        switch key {
        case "fajr": return sunrise
        case "dhuhr": return asr
        case "asr": return sunset.isNaN ? maghrib : sunset
        case "maghrib": return maghribEnd.isNaN ? isha : maghribEnd
        case "isha": return midnight
        default: return .nan
        }
    }

    // NaN لا يساوي نفسه؛ نقارن بالبتات حتى تتساوى المواقيت المتعذرة
    public static func == (a: PrayerTimes, b: PrayerTimes) -> Bool {
        let x = [a.fajr, a.sunrise, a.dhuhr, a.asr, a.maghrib, a.isha, a.sunset, a.maghribEnd, a.midnight]
        let y = [b.fajr, b.sunrise, b.dhuhr, b.asr, b.maghrib, b.isha, b.sunset, b.maghribEnd, b.midnight]
        return zip(x, y).allSatisfy { $0 == $1 || ($0.isNaN && $1.isNaN) }
    }
}

/// مدينة معروفة: key لاسمها في ملفات اللغة، و cc رمز الدولة (ISO 3166)، و [elev] ارتفاعها عن البحر بالأمتار
public struct City: Hashable, Sendable {
    public let key: String
    public let cc: String
    public let lat: Double
    public let lng: Double
    public let tz: String
    public let method: String
    public let elev: Int

    public init(_ key: String, _ cc: String, _ lat: Double, _ lng: Double, _ tz: String, _ method: String, elev: Int = 0) {
        self.key = key; self.cc = cc; self.lat = lat; self.lng = lng; self.tz = tz; self.method = method; self.elev = elev
    }
}

/// حساب مواقيت الصلاة فلكياً من موضع الشمس
public enum PrayTimes {
    public static let PRAYERS = ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"]
    /// الصلوات الخمس (بلا الشروق)
    public static let FIVE = ["fajr", "dhuhr", "asr", "maghrib", "isha"]

    public static let METHODS: [Method] = [
        // تقويم أم القرى: لا دقائق، والأوقات مقرّبة إلى الدقيقة التالية والشروق إلى السابقة؛ العشاء بعد المغرب بتسعين دقيقة، وبعشرين ومئة في رمضان
        Method("makkah", 18.5, nil, 90, minutes: ["fajr": 0.5, "sunrise": -0.5, "dhuhr": 0.5, "asr": 0.5, "maghrib": 0.5]),
        Method("mwl", 18.0, 17.0, nil),
        // الهيئة المصرية العامة للمساحة: الحساب الفلكي بلا دقائق
        Method("egypt", 19.5, 17.5, nil, minutes: Method.none),
        // الهيئة العامة للشؤون الإسلامية والأوقاف (الإمارات)
        Method("dubai", 18.0, 18.0, nil, 1.38,
               minutes: ["fajr": -1.1, "sunrise": -3.6, "dhuhr": 2.3, "asr": 0.45, "maghrib": 0.3, "isha": 0.3]),
        Method("kuwait", 18.0, 17.5, nil),
        // وزارة الأوقاف القطرية: المغرب بعد الغروب بدقيقتين، وبثلاث في رمضان، والعشاء بعده بتسعين دقيقة
        Method("qatar", 18.0, nil, 90, minutes: ["fajr": -1.1, "maghrib": 1.95], ramadan: ["maghrib": 1.05]),
        // تقويم «الزبارة والبحرين»
        Method("bahrain", 18.0, 18.0, nil, minutes: ["fajr": -0.9, "dhuhr": 1.0, "maghrib": 0.5, "isha": -1.1]),
        // وزارة الأوقاف الأردنية
        Method("jordan", 18.0, 18.0, nil,
               minutes: ["fajr": -0.25, "sunrise": -6.27, "dhuhr": 0.36, "asr": 0.4, "maghrib": 6.45, "isha": 0.34]),
        // دار الإفتاء الفلسطينية
        Method("palestine", 18.0, 18.0, nil,
               minutes: ["fajr": -0.25, "sunrise": -3.0, "dhuhr": -0.5, "asr": -0.4, "maghrib": 6.0, "isha": -0.55]),
        // دار الفتوى في لبنان
        Method("lebanon", 19.2, 17.3, nil, minutes: ["sunrise": 1.35, "dhuhr": 0.2, "asr": -0.6, "maghrib": 3.4]),
        // وزارة الأوقاف السورية
        Method("syria", 18.0, 17.0, nil,
               minutes: ["fajr": -0.4, "sunrise": -0.6, "dhuhr": 0.4, "asr": 0.6, "maghrib": 2.4, "isha": 0.6]),
        // دار الإفتاء الليبية
        Method("libya", 18.0, 18.0, nil,
               minutes: ["fajr": -2.6, "sunrise": -0.8, "dhuhr": 3.1, "asr": -0.8, "maghrib": 3.4, "isha": 1.2]),
        // وزارة الأوقاف المغربية: الظهر بعد الزوال بخمس دقائق، والشروق والمغرب بارتفاع المدينة
        Method("morocco", 19.0, 17.0, nil,
               minutes: ["fajr": -0.3, "sunrise": -2.2, "dhuhr": 5.2, "asr": 0.25, "maghrib": 3.4, "isha": 0.2],
               elevation: 0.7),
        // وزارة الشؤون الدينية الجزائرية
        Method("algeria", 18.0, 17.0, nil, minutes: ["fajr": 0.5, "dhuhr": 1.0, "maghrib": 3.5]),
        // المعهد الوطني للرصد الجوي التونسي
        Method("tunisia", 18.0, 18.0, nil,
               minutes: ["fajr": -0.5, "sunrise": -0.5, "dhuhr": 7.0, "asr": 0.0, "maghrib": 2.5, "isha": 0.5]),
        // الحنفي هو المعتمد في باكستان والهند وبنغلاديش وأفغانستان
        Method("karachi", 18.0, 18.0, nil, hanafi: true),
        // رئاسة الشؤون الدينية التركية: التمكين، وموضع الشمس مرة لليوم
        Method("turkey", 18.0, 17.0, nil,
               minutes: ["sunrise": -7.0, "dhuhr": 5.0, "asr": 4.0, "maghrib": 7.0], dailySun: true),
        // مؤتمر إسطنبول 2021 في أوروبا
        Method("europe", 18.0, 16.0, nil,
               minutes: ["sunrise": -7.0, "dhuhr": 5.0, "asr": 4.0, "maghrib": 7.0], dailySun: true,
               depression: (0.66, 0.62)),
        // المشيخة الإسلامية في البوسنة والهرسك
        Method("bosnia", 18.0, 16.0, nil, minutes: ["sunrise": -6.0, "dhuhr": 1.0, "maghrib": 6.0]),
        // معهد الجيوفيزياء بجامعة طهران: الحساب الفلكي بلا دقائق
        Method("tehran", 17.7, 14.0, nil, 4.5, minutes: Method.none),
        // إدارة مسلمي القوقاز: المغرب بانخفاض 3.5°، والعصر بمثلَي الظل
        Method("azerbaijan", 16.0, 15.0, nil, 3.5,
               minutes: ["fajr": -0.35, "sunrise": -0.15, "asr": 0.1, "maghrib": 0.2, "isha": 0.4], hanafi: true),
        // الإدارة الدينية لمسلمي أوزبكستان
        Method("uzbekistan", 15.0, 15.0, nil, minutes: ["fajr": -2.45, "dhuhr": 5.0, "maghrib": 3.9], hanafi: true),
        // الإدارة الدينية لمسلمي كازاخستان
        Method("kazakhstan", 15.0, 15.0, nil,
               minutes: ["sunrise": -3.0, "dhuhr": 3.0, "asr": 3.0, "maghrib": 3.0], hanafi: true),
        // وزارة الشؤون الدينية الإندونيسية: والشروق والمغرب بارتفاع المكان
        Method("indonesia", 20.0, 18.0, nil,
               minutes: ["fajr": 2.5, "sunrise": -3.15, "dhuhr": 3.55, "asr": 2.55, "maghrib": 3.2, "isha": 2.5],
               elevation: 1.0),
        // JAKIM
        Method("malaysia", 18.0, 18.0, nil,
               minutes: ["fajr": 1.8, "sunrise": -0.9, "dhuhr": 2.2, "asr": 1.7, "maghrib": 1.7, "isha": 1.8]),
        // MUIS
        Method("singapore", 20.0, 18.0, nil,
               minutes: ["fajr": 0.45, "sunrise": 0.45, "dhuhr": 1.55, "asr": 0.45, "maghrib": 0.55, "isha": 0.5]),
        // وزارة الشؤون الدينية في بروناي
        Method("brunei", 20.0, 18.0, nil),
        // الإدارة الدينية لمسلمي روسيا الاتحادية
        Method("russia", 18.0, 15.0, nil,
               minutes: ["fajr": 0.35, "sunrise": -4.55, "dhuhr": 4.9, "asr": -0.25, "maghrib": 4.3, "isha": -0.5]),
        // الإدارة الدينية لمسلمي تتارستان: العصر الحنفي، وفي الصيف الفجر قبل الشروق بساعتين والعشاء بعد الغروب بساعة ونصف
        Method("tatarstan", 18.0, 15.0, nil,
               minutes: ["fajr": -0.5, "sunrise": 0.35, "dhuhr": 0.4, "asr": 0.8, "maghrib": 0.55, "isha": 0.4],
               hanafi: true, fajrFallback: 120.15, ishaFallback: 90.1),
        // جدول لندن الموحد
        Method("uk", 18.0, 18.0, nil, minutes: ["sunrise": -3.0, "dhuhr": 5.0, "maghrib": 3.0], seasonal: true),
        // المسجد الكبير في باريس
        Method("paris", 18.0, nil, 87, minutes: ["dhuhr": 5.0, "asr": -0.8, "maghrib": 3.0], seasonal: true),
        Method("isna", 15.0, 15.0, nil),
        Method("france", 12.0, 12.0, nil),
    ]

    /// الطريقة بمفتاحها، أو رابطة العالم الإسلامي إن لم تُعرف
    public static func method(_ key: String) -> Method { METHODS.first { $0.key == key } ?? METHODS[1] }

    private static func dtr(_ d: Double) -> Double { d * Double.pi / 180.0 }
    private static func rtd(_ r: Double) -> Double { r * 180.0 / Double.pi }
    private static func dsin(_ d: Double) -> Double { sin(dtr(d)) }
    private static func dcos(_ d: Double) -> Double { cos(dtr(d)) }
    private static func dtan(_ d: Double) -> Double { tan(dtr(d)) }
    private static func fix(_ a: Double, _ b: Double) -> Double { let r = a - b * floor(a / b); return r < 0 ? r + b : r }
    private static func fixAngle(_ a: Double) -> Double { fix(a, 360.0) }
    static func fixHour(_ a: Double) -> Double { fix(a, 24.0) }

    /// فرق توقيت المكان عن UTC بالساعات ظهر ذلك اليوم
    public static func tzOffset(_ tz: String, _ date: LocalDate) -> Double { tzOffsetHours(tz, date.atTime(12, 0)) }

    private static func julian(_ year: Int, _ month: Int, _ day: Int) -> Double {
        var y = year, m = month
        if m <= 2 { y -= 1; m += 12 }
        let a = floor(Double(y) / 100.0), b = 2 - a + floor(a / 4.0)
        return floor(365.25 * Double(y + 4716)) + floor(30.6001 * Double(m + 1)) + Double(day) + b - 1524.5
    }

    private static func sunPosition(_ jd: Double) -> (Double, Double) {
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

    /// [elev]: ارتفاع المكان عن البحر بالأمتار، يُحسب به الشروق والغروب في الطرق التي تعتمده ([Method.elevation])
    public static func compute(_ date: LocalDate, _ lat: Double, _ lng: Double, _ tz: String, _ methodKey: String, _ hanafi: Bool, elev: Int = 0) -> PrayerTimes {
        compute(date, lat, lng, tz, method(methodKey), hanafi, elev: elev)
    }

    public static func compute(_ date: LocalDate, _ lat: Double, _ lng: Double, _ tz: String, _ m: Method, _ hanafi: Bool, elev: Int = 0) -> PrayerTimes {
        // الأفق: 0.833° (انكسار الضوء ونصف قرص الشمس)، ويزيد بانخفاض الأفق للمرتفع: 0.0347° × جذر الارتفاع بالأمتار
        let horizon = 0.833 + (elev > 0 ? m.elevation * 0.0347 * Double(elev).squareRoot() : 0.0)
        let offset = tzOffset(tz, date)
        let jDate = julian(date.year, date.month, date.day) - lng / (15.0 * 24.0)
        // موضع الشمس عند كل وقت، أو مرة لليوم عند منتصف الليل بتوقيت غرينتش ([Method.dailySun])
        let daily: (Double, Double)? = m.dailySun ? sunPosition(julian(date.year, date.month, date.day)) : nil
        func sun(_ t: Double) -> (Double, Double) { daily ?? sunPosition(jDate + t) }

        func midDay(_ t: Double) -> Double { fixHour(12 - sun(t).1) }
        func sunAngleTime(_ angle: Double, _ t: Double, _ ccw: Bool = false) -> Double {
            let decl = sun(t).0
            let noon = midDay(t)
            let x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            if x < -1 || x > 1 { return .nan }
            let tt = rtd(acos(x)) / 15.0
            return noon + (ccw ? -tt : tt)
        }
        func asrTime(_ factor: Double, _ t: Double) -> Double {
            let decl = sun(t).0
            let angle = -rtd(atan(1.0 / (factor + dtan(abs(lat - decl)))))
            return sunAngleTime(angle, t)
        }

        // في العروض العليا صيفاً: الزاويتان نسبة من أعمق انخفاض للشمس تلك الليلة إن قلّتا عنه ([Method.depression])
        var fajrAngle = m.fajr
        var ishaAngle = m.ishaAngle
        if let dep = m.depression {
            let (kf, ki) = dep
            let decl = sunPosition(jDate + 0.5).0
            let deepest = 90 - abs(lat) - (lat >= 0 ? decl : -decl)
            fajrAngle = kmin(fajrAngle, kf * deepest)
            ishaAngle = ishaAngle.map { kmin($0, ki * deepest) }
        }

        var fajr = 5.0, sunrise = 6.0, dhuhr = 12.0, asr = 13.0, sunset = 18.0, maghrib = 18.0, isha = 18.0
        // مغيب الشفق الأحمر (17°) عند الجمهور، والأبيض (18°) عند الحنفية: نهاية وقت المغرب
        var shafaq = 19.0
        let shafaqAngle = hanafi ? 18.0 : 17.0
        for _ in 0..<2 {
            func p(_ v: Double) -> Double { (v.isNaN ? 12.0 : v) / 24.0 }
            let nf = sunAngleTime(fajrAngle, p(fajr), true)
            let ns = sunAngleTime(horizon, p(sunrise), true)
            let nd = midDay(p(dhuhr))
            let na = asrTime(hanafi ? 2.0 : 1.0, p(asr))
            let nss = sunAngleTime(horizon, p(sunset))
            let nm = m.maghribAngle != nil ? sunAngleTime(m.maghribAngle!, p(maghrib)) : nss
            let ni = ishaAngle.map { sunAngleTime($0, p(isha)) } ?? .nan
            let nsh = sunAngleTime(shafaqAngle, p(shafaq))
            fajr = nf; sunrise = ns; dhuhr = nd; asr = na; sunset = nss; maghrib = nm; isha = ni; shafaq = nsh
        }
        let adj = offset - lng / 15.0
        fajr += adj; sunrise += adj; dhuhr += adj; asr += adj; sunset += adj; maghrib += adj; isha += adj; shafaq += adj
        if maghrib.isNaN { maghrib = sunset }

        // خطوط العرض العليا
        let night = fixHour(sunrise - sunset + 24)
        if m.seasonal {
            // لجنة رؤية الهلال العالمية: فوق 55° سُبع الليل، ثم لا يسبق الفجرُ الحدَّ الموسمي ولا يتأخر العشاء عنه
            if abs(lat) >= 55 {
                fajr = sunrise - night / 7
                if ishaAngle != nil { isha = sunset + night / 7 }
            }
            let safeFajr = sunrise - seasonalTwilight(lat, date, morning: true) / 60.0
            if fajr.isNaN || safeFajr > fajr { fajr = safeFajr }
            if ishaAngle != nil {
                let safeIsha = sunset + seasonalTwilight(lat, date, morning: false) / 60.0
                if isha.isNaN || safeIsha < isha { isha = safeIsha }
            }
        } else if m.depression != nil {
            // الزاويتان مخفّضتان أصلاً بانخفاض الشمس فتبلغهما دائماً
        } else {
            // مدة ثابتة إن نصّت الجهة عليها (للفجر والعشاء معاً ما دامت الشمس لا تبلغ زاوية الفجر)، وإلا طريقة الزاوية
            let noDawn = fajr.isNaN
            if let fb = m.fajrFallback {
                if noDawn { fajr = sunrise - fb / 60.0 }
            } else {
                let fajrMax = fajrAngle / 60.0 * night
                if fajr.isNaN || fixHour(sunrise - fajr) > fajrMax { fajr = sunrise - fajrMax }
            }
            if let ia = ishaAngle {
                if let ib = m.ishaFallback {
                    if noDawn || isha.isNaN { isha = sunset + ib / 60.0 }
                } else {
                    let ishaMax = ia / 60.0 * night
                    if isha.isNaN || fixHour(isha - sunset) > ishaMax { isha = sunset + ishaMax }
                }
            }
        }
        // منتصف الليل من الفجر الفلكي (قبل دقائق الجهة الرسمية)
        let astroFajr = fajr

        // دقائق الجهة الرسمية (وزيادتها في رمضان)، ثم العشاء بالمدة الثابتة بعد المغرب كما تنشره الجهة (أم القرى وقطر)
        let inRamadan = (!m.ramadan.isEmpty || m.key == "makkah") && Hijri.toHijri(date)?.m == 9
        func mins(_ prayer: String) -> Double { m.minutes(prayer, inRamadan: inRamadan) / 60.0 }
        fajr += mins("fajr"); sunrise += mins("sunrise"); dhuhr += mins("dhuhr"); asr += mins("asr"); maghrib += mins("maghrib")
        if let im = m.ishaMinutes {
            isha = maghrib + Double(inRamadan && m.key == "makkah" ? 120 : im) / 60.0
        }
        isha += mins("isha")

        // نهاية المغرب: في الطرق التي تحسب العشاء بزاوية يدخل العشاء بمغيب الشفق نفسه، وفي طرق المدة الثابتة قد يغيب الشفق قبل العشاء المعلن بكثير
        let maghribEnd = (m.ishaAngle != nil || shafaq.isNaN) ? isha : kmin(isha, shafaq)
        // منتصف الليل الشرعي: منتصف ما بين الغروب والفجر التالي (فجر الغد ≈ فجر اليوم + 24 ساعة)
        let midnight = astroFajr.isNaN ? Double.nan : sunset + (astroFajr + 24 - sunset) / 2
        return PrayerTimes(fajr: fajr, sunrise: sunrise, dhuhr: dhuhr, asr: asr, maghrib: maghrib, isha: isha,
                           sunset: sunset, maghribEnd: maghribEnd, midnight: midnight)
    }

    /// حد الشفق الموسمي للجنة رؤية الهلال العالمية بالدقائق: قبل الشروق للفجر، وبعد الغروب للعشاء.
    /// خطّي بين أربع قيم في السنة تزيد مع خط العرض، كما في مكتبة adhan.
    public static func seasonalTwilight(_ lat: Double, _ date: LocalDate, morning: Bool) -> Double {
        let l = abs(lat)
        let a: Double, b: Double, c: Double, d: Double
        if morning {
            a = 75 + 28.65 / 55 * l; b = 75 + 19.44 / 55 * l; c = 75 + 32.74 / 55 * l; d = 75 + 48.10 / 55 * l
        } else {
            a = 75 + 25.6 / 55 * l; b = 75 + 2.05 / 55 * l; c = 75 - 9.21 / 55 * l; d = 75 + 6.14 / 55 * l
        }
        let y = date.year
        let leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        let days = leap ? 366 : 365
        let doy = date.dayOfYear
        // الأيام منذ الانقلاب الشتوي في نصف الكرة
        let dyy: Int
        if lat >= 0 {
            let v = doy + 10; dyy = v >= days ? v - days : v
        } else {
            let v = doy - (leap ? 173 : 172); dyy = v < 0 ? v + days : v
        }
        let adj: Double
        if dyy < 91 { adj = a + (b - a) / 91.0 * Double(dyy) }
        else if dyy < 137 { adj = b + (c - b) / 46.0 * Double(dyy - 91) }
        else if dyy < 183 { adj = c + (d - c) / 46.0 * Double(dyy - 137) }
        else if dyy < 229 { adj = d + (c - d) / 46.0 * Double(dyy - 183) }
        else if dyy < 275 { adj = c + (b - c) / 46.0 * Double(dyy - 229) }
        else { adj = b + (a - b) / 91.0 * Double(dyy - 275) }
        return Double(roundToInt(adj * 60)) / 60.0 // مكتبة adhan تقرّب إلى الثانية
    }

    /// الوقت نصاً (05:07 أو 5:07 AM)؛ «—» لما تعذّر حسابه
    public static func format(_ hours: Double, _ h24: Bool, am: String = "AM", pm: String = "PM") -> String {
        if hours.isNaN { return "—" }
        let mins = roundToInt(fixHour(hours) * 60)
        var h = (mins / 60) % 24
        let m = mins % 60
        let mm = m < 10 ? "0\(m)" : "\(m)"
        if h24 { return (h < 10 ? "0\(h)" : "\(h)") + ":" + mm }
        let suf = h < 12 ? am : pm
        h %= 12; if h == 0 { h = 12 }
        return "\(h):\(mm) \(suf)"
    }

    // ---------- القبلة ----------
    public static let KAABA_LAT = 21.4225
    public static let KAABA_LNG = 39.8262

    /// اتجاه القبلة بالدرجات من الشمال باتجاه عقارب الساعة
    public static func qibla(_ lat: Double, _ lng: Double) -> Double {
        let dl = dtr(KAABA_LNG - lng), p1 = dtr(lat), p2 = dtr(KAABA_LAT)
        let b = atan2(sin(dl), cos(p1) * tan(p2) - sin(p1) * cos(dl))
        return fixAngle(rtd(b))
    }

    /// المسافة إلى الكعبة بالكيلومترات
    public static func distanceKm(_ lat: Double, _ lng: Double) -> Double {
        let r = 6371.0
        let dLat = dtr(KAABA_LAT - lat), dLng = dtr(KAABA_LNG - lng)
        let a = sin(dLat / 2) * sin(dLat / 2) + cos(dtr(lat)) * cos(dtr(KAABA_LAT)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * asin(a.squareRoot())
    }

    /// رقم الجهة من ثماني جهات (0 شمال، 1 شمال شرق … 7 شمال غرب) لاتجاه بالدرجات، كما في شاشة المواقيت
    /// (s.directions[round(qibla / 45) % 8])
    public static func directionIndex(_ bearing: Double) -> Int {
        kotlinToInt32((bearing / 45.0).rounded(.toNearestOrEven)) % 8
    }

    public static let CITIES: [City] = [
        City("makkah", "SA", 21.4225, 39.8262, "Asia/Riyadh", "makkah"),
        City("madinah", "SA", 24.4672, 39.6111, "Asia/Riyadh", "makkah"),
        City("riyadh", "SA", 24.7136, 46.6753, "Asia/Riyadh", "makkah"),
        City("jeddah", "SA", 21.5433, 39.1728, "Asia/Riyadh", "makkah"),
        City("dammam", "SA", 26.4207, 50.0888, "Asia/Riyadh", "makkah"),
        City("abha", "SA", 18.2164, 42.5053, "Asia/Riyadh", "makkah"),
        City("dubai", "AE", 25.2048, 55.2708, "Asia/Dubai", "dubai"),
        City("abudhabi", "AE", 24.4539, 54.3773, "Asia/Dubai", "dubai"),
        City("sharjah", "AE", 25.3463, 55.4209, "Asia/Dubai", "dubai"),
        City("kuwait", "KW", 29.3759, 47.9774, "Asia/Kuwait", "kuwait"),
        City("doha", "QA", 25.2854, 51.531, "Asia/Qatar", "qatar"),
        City("manama", "BH", 26.2285, 50.586, "Asia/Bahrain", "bahrain"),
        City("muscat", "OM", 23.588, 58.3829, "Asia/Muscat", "mwl"),
        City("cairo", "EG", 30.0444, 31.2357, "Africa/Cairo", "egypt"),
        City("alexandria", "EG", 31.2001, 29.9187, "Africa/Cairo", "egypt"),
        City("aswan", "EG", 24.0889, 32.8998, "Africa/Cairo", "egypt"),
        City("amman", "JO", 31.9454, 35.9284, "Asia/Amman", "jordan"),
        City("jerusalem", "PS", 31.7683, 35.2137, "Asia/Jerusalem", "palestine"),
        City("gaza", "PS", 31.5017, 34.4668, "Asia/Gaza", "palestine"),
        City("beirut", "LB", 33.8938, 35.5018, "Asia/Beirut", "lebanon"),
        City("damascus", "SY", 33.5138, 36.2765, "Asia/Damascus", "syria"),
        City("aleppo", "SY", 36.2021, 37.1343, "Asia/Damascus", "syria"),
        City("baghdad", "IQ", 33.3152, 44.3661, "Asia/Baghdad", "mwl"),
        City("basra", "IQ", 30.5085, 47.7804, "Asia/Baghdad", "mwl"),
        City("erbil", "IQ", 36.1912, 44.0092, "Asia/Baghdad", "mwl"),
        City("sanaa", "YE", 15.3694, 44.191, "Asia/Aden", "mwl"),
        City("aden", "YE", 12.7855, 45.0187, "Asia/Aden", "mwl"),
        City("khartoum", "SD", 15.5007, 32.5599, "Africa/Khartoum", "egypt"),
        City("tripoli", "LY", 32.8872, 13.1913, "Africa/Tripoli", "libya"),
        City("benghazi", "LY", 32.1167, 20.0667, "Africa/Tripoli", "libya"),
        City("tunis", "TN", 36.8065, 10.1815, "Africa/Tunis", "tunisia"),
        City("algiers", "DZ", 36.7538, 3.0588, "Africa/Algiers", "algeria"),
        City("oran", "DZ", 35.6971, -0.6308, "Africa/Algiers", "algeria"),
        City("rabat", "MA", 34.0209, -6.8416, "Africa/Casablanca", "morocco", elev: 50),
        City("casablanca", "MA", 33.5731, -7.5898, "Africa/Casablanca", "morocco", elev: 50),
        City("marrakesh", "MA", 31.6295, -7.9811, "Africa/Casablanca", "morocco", elev: 466),
        City("nouakchott", "MR", 18.0735, -15.9582, "Africa/Nouakchott", "mwl"),
        City("mogadishu", "SO", 2.0469, 45.3182, "Africa/Mogadishu", "mwl"),
        City("djibouti", "DJ", 11.5721, 43.1456, "Africa/Djibouti", "mwl"),
        City("istanbul", "TR", 41.0082, 28.9784, "Europe/Istanbul", "turkey"),
        City("ankara", "TR", 39.9334, 32.8597, "Europe/Istanbul", "turkey"),
        City("karachi", "PK", 24.8607, 67.0011, "Asia/Karachi", "karachi"),
        City("lahore", "PK", 31.5204, 74.3587, "Asia/Karachi", "karachi"),
        City("islamabad", "PK", 33.6844, 73.0479, "Asia/Karachi", "karachi"),
        City("delhi", "IN", 28.6139, 77.209, "Asia/Kolkata", "karachi"),
        City("mumbai", "IN", 19.076, 72.8777, "Asia/Kolkata", "karachi"),
        City("hyderabad", "IN", 17.385, 78.4867, "Asia/Kolkata", "karachi"),
        City("dhaka", "BD", 23.8103, 90.4125, "Asia/Dhaka", "karachi"),
        City("chittagong", "BD", 22.3569, 91.7832, "Asia/Dhaka", "karachi"),
        City("kabul", "AF", 34.5553, 69.2075, "Asia/Kabul", "karachi"),
        City("tehran", "IR", 35.6892, 51.389, "Asia/Tehran", "tehran"),
        City("mashhad", "IR", 36.2605, 59.6168, "Asia/Tehran", "tehran"),
        City("tashkent", "UZ", 41.2995, 69.2401, "Asia/Tashkent", "uzbekistan"),
        City("baku", "AZ", 40.4093, 49.8671, "Asia/Baku", "azerbaijan"),
        City("almaty", "KZ", 43.2383, 76.9455, "Asia/Almaty", "kazakhstan"),
        City("moscow", "RU", 55.7558, 37.6173, "Europe/Moscow", "russia"),
        City("kazan", "RU", 55.7963, 49.1088, "Europe/Moscow", "tatarstan"),
        City("grozny", "RU", 43.3178, 45.6949, "Europe/Moscow", "russia"),
        City("kualalumpur", "MY", 3.139, 101.6869, "Asia/Kuala_Lumpur", "malaysia"),
        City("penang", "MY", 5.4141, 100.3288, "Asia/Kuala_Lumpur", "malaysia"),
        City("singapore", "SG", 1.3521, 103.8198, "Asia/Singapore", "singapore"),
        City("brunei", "BN", 4.9031, 114.9398, "Asia/Brunei", "brunei"),
        City("jakarta", "ID", -6.2088, 106.8456, "Asia/Jakarta", "indonesia"),
        City("surabaya", "ID", -7.2575, 112.7521, "Asia/Jakarta", "indonesia"),
        City("bandung", "ID", -6.9175, 107.6191, "Asia/Jakarta", "indonesia", elev: 790),
        City("medan", "ID", 3.5952, 98.6722, "Asia/Jakarta", "indonesia"),
        City("makassar", "ID", -5.1477, 119.4327, "Asia/Makassar", "indonesia"),
        City("lagos", "NG", 6.5244, 3.3792, "Africa/Lagos", "mwl"),
        City("kano", "NG", 12.0022, 8.592, "Africa/Lagos", "mwl"),
        City("dakar", "SN", 14.7167, -17.4677, "Africa/Dakar", "mwl"),
        City("sarajevo", "BA", 43.8563, 18.4131, "Europe/Sarajevo", "bosnia"),
        City("london", "GB", 51.5074, -0.1278, "Europe/London", "uk"),
        City("birmingham", "GB", 52.4862, -1.8904, "Europe/London", "uk"),
        City("paris", "FR", 48.8566, 2.3522, "Europe/Paris", "paris"),
        City("marseille", "FR", 43.2965, 5.3698, "Europe/Paris", "paris"),
        City("lyon", "FR", 45.764, 4.8357, "Europe/Paris", "paris"),
        City("brussels", "BE", 50.8503, 4.3517, "Europe/Brussels", "europe"),
        City("amsterdam", "NL", 52.3676, 4.9041, "Europe/Amsterdam", "europe"),
        City("berlin", "DE", 52.52, 13.405, "Europe/Berlin", "europe"),
        City("hamburg", "DE", 53.5511, 9.9937, "Europe/Berlin", "europe"),
        City("cologne", "DE", 50.9375, 6.9603, "Europe/Berlin", "europe"),
        City("munich", "DE", 48.1351, 11.582, "Europe/Berlin", "europe"),
        City("vienna", "AT", 48.2082, 16.3738, "Europe/Vienna", "europe"),
        City("stockholm", "SE", 59.3293, 18.0686, "Europe/Stockholm", "europe"),
        City("newyork", "US", 40.7128, -74.006, "America/New_York", "isna"),
        City("chicago", "US", 41.8781, -87.6298, "America/Chicago", "isna"),
        City("losangeles", "US", 34.0522, -118.2437, "America/Los_Angeles", "isna"),
        City("toronto", "CA", 43.6532, -79.3832, "America/Toronto", "isna"),
        City("montreal", "CA", 45.5017, -73.5673, "America/Toronto", "isna"),
        City("sydney", "AU", -33.8688, 151.2093, "Australia/Sydney", "mwl"),    ]

    public static func city(_ key: String?) -> City? { CITIES.first { $0.key == key } }

    /// ارتفاع المكان: ارتفاع المدينة المعروفة إن كان الموقع فيها أو قريباً منها (نحو 30 كم)، وإلا صفر
    public static func elevationNear(_ lat: Double, _ lng: Double) -> Int {
        let (c, d2) = nearestCity(lat, lng)
        return d2 < 0.3 * 0.3 ? c.elev : 0
    }

    /// أقرب مدينة معروفة لاقتراح طريقة الحساب، ومربع المسافة بالدرجات
    public static func nearestCity(_ lat: Double, _ lng: Double) -> (City, Double) {
        var best = CITIES[0]
        var bestD = Double.infinity
        var first = true
        for c in CITIES {
            let d = (c.lat - lat) * (c.lat - lat) + (c.lng - lng) * (c.lng - lng)
            // minBy: أول أصغر قيمة بمقارنة Double.compareTo
            if first { best = c; bestD = d; first = false } else if kless(d, bestD) { best = c; bestD = d }
        }
        return (best, bestD)
    }
}

// ---------- دوال الأعداد كما في Kotlin/JVM ----------

/// minOf(a, b) للأعداد العشرية كما في Math.min (NaN يغلب، و ‎-0.0 أصغر من 0.0)
@inline(__always) func kmin(_ a: Double, _ b: Double) -> Double {
    if a.isNaN || b.isNaN { return .nan }
    if a == 0, b == 0 { return a.sign == .minus ? a : b }
    return a <= b ? a : b
}

/// مقارنة Double.compareTo في Kotlin (NaN أكبر من كل شيء، و ‎-0.0 أصغر من 0.0)
@inline(__always) func kless(_ a: Double, _ b: Double) -> Bool {
    if a.isNaN { return false }
    if b.isNaN { return true }
    if a == 0, b == 0 { return a.sign == .minus && b.sign == .plus }
    return a < b
}

/// roundToInt في Kotlin (Math.round: أقرب عدد صحيح، والنصف إلى الأعلى)
func roundToInt(_ x: Double) -> Int {
    if x.isNaN { return 0 }
    let f = x.rounded(.down)
    let r = (x - f) >= 0.5 ? f + 1 : f
    if r >= 2147483647.0 { return 2147483647 }
    if r <= -2147483648.0 { return -2147483648 }
    return Int(r)
}

/// Double.toInt() في Kotlin: يُبتر الكسر، وNaN صفر، ويُشبع عند حدود Int (32 بت)
func kotlinToInt32(_ x: Double) -> Int {
    if x.isNaN { return 0 }
    if x >= 2147483647.0 { return 2147483647 }
    if x <= -2147483648.0 { return -2147483648 }
    return Int(x.rounded(.towardZero))
}

/// Double.toLong() في Kotlin: يُبتر الكسر، وNaN صفر، ويُشبع عند حدود Long
func kotlinToLong(_ x: Double) -> Int64 {
    if x.isNaN { return 0 }
    if x >= 9.223372036854775807e18 { return Int64.max }
    if x <= -9.223372036854775808e18 { return Int64.min }
    return Int64(x.rounded(.towardZero))
}
