import Foundation
import MizanBase

/// موقع المستخدم — core/AppSettings.kt. key مفتاح مدينة معروفة، أو nil لموقع GPS بعيد عن أي مدينة؛
/// [elev] ارتفاعه عن البحر بالأمتار
public struct Place: Hashable {
    public let key: String?
    public let lat: Double
    public let lng: Double
    public let tz: String
    public let method: String
    public let gps: Bool
    public let elev: Int

    public init(key: String?, lat: Double, lng: Double, tz: String, method: String, gps: Bool = false, elev: Int = 0) {
        self.key = key; self.lat = lat; self.lng = lng; self.tz = tz; self.method = method; self.gps = gps; self.elev = elev
    }

    public func with(method: String) -> Place { Place(key: key, lat: lat, lng: lng, tz: tz, method: method, gps: gps, elev: elev) }

    /// مكة المكرمة (PrayTimes.CITIES[0]): المكان قبل أن يختار المستخدم موقعه
    public static let makkah = Place(key: "makkah", lat: 21.4225, lng: 39.8262, tz: "Asia/Riyadh", method: "makkah")
}

/// الإقامة الافتراضية بعد الأذان بالدقائق
public let IQAMA_DEFAULT: [String: Int] = ["fajr": 25, "dhuhr": 20, "asr": 20, "maghrib": 10, "isha": 20]

/**
 إعدادات التطبيق المحفوظة على الجهاز، بالمفاتيح والصيغ نفسها التي يكتبها Kotlin في iOS (Prefs في Platform.ios.kt):
 NSUserDefaults.standard، وكل مفتاح يبدأ بـ «mizan.» (اسم المخزن ثم نقطة). النص يُحفظ NSString (setObject)،
 والعدد NSNumber صحيحاً (setInteger)، والمنطقي NSNumber منطقياً (setBool). الأعداد العشرية (place_lat و place_lng)
 نصٌّ بصيغة Double.toString في Kotlin، والارتفاع place_elev نصٌّ أيضاً. فيعمل Kotlin و Swift معاً على المخزن نفسه.
 القراءة مباشرة من المخزن في كل مرة، فيظهر ما كتبه Kotlin فوراً.
 */
public final class AppSettings {
    public static let shared = AppSettings()

    /// إصدار قواعد المواقيت (2: طرق الجهات الرسمية لكل بلد)
    public static let rulesVersion = 2

    /// ارتفاع المكان من المدينة المعروفة القريبة (PrayTimes.elevationNear في MizanPrayer)، لما حُفظ قبل حساب الارتفاع
    public static var elevationNear: (Double, Double) -> Int = { _, _ in 0 }

    public let defaults: UserDefaults
    public let prefix: String

    public init(defaults: UserDefaults = .standard, name: String = "mizan") {
        self.defaults = defaults
        self.prefix = name + "."
    }

    // ---------- تخزين عام (Prefs) ----------

    public func string(_ k: String) -> String? { defaults.string(forKey: prefix + k) }
    public func getString(_ k: String, _ d: String = "") -> String { string(k) ?? d }
    public func putString(_ k: String, _ v: String) { defaults.set(v, forKey: prefix + k) }
    public func getInt(_ k: String, _ d: Int = 0) -> Int { contains(k) ? defaults.integer(forKey: prefix + k) : d }
    public func putInt(_ k: String, _ v: Int) { defaults.set(v, forKey: prefix + k) }
    public func getLong(_ k: String, _ d: Int64 = 0) -> Int64 { contains(k) ? Int64(defaults.integer(forKey: prefix + k)) : d }
    public func putLong(_ k: String, _ v: Int64) { defaults.set(Int(v), forKey: prefix + k) }
    public func getBool(_ k: String, _ d: Bool) -> Bool { contains(k) ? defaults.bool(forKey: prefix + k) : d }
    public func putBool(_ k: String, _ v: Bool) { defaults.set(v, forKey: prefix + k) }
    public func contains(_ k: String) -> Bool { defaults.object(forKey: prefix + k) != nil }
    public func remove(_ k: String) { defaults.removeObject(forKey: prefix + k) }
    public func isEmpty() -> Bool { !defaults.dictionaryRepresentation().keys.contains { $0.hasPrefix(prefix) } }

    // ---------- عند بدء التطبيق (AppSettings.init في Kotlin) ----------

    /**
     ما يفعله init في Kotlin قبل القراءة: ترحيل بيانات الإصدارات العربية، ثم قواعد المواقيت.
     [arabicCityKey]: مفتاح مدينة من اسمها العربي (أول مفتاح في Ar.cities)، و[defaultMethod]: طريقة المكان الافتراضية
     (طريقة مدينته المعروفة أو أقرب مدينة، AppSettings.defaultMethod في Kotlin).
     */
    public func initialize(arabicCityKey: (String) -> String?, defaultMethod: (Place) -> String) {
        migrate(arabicCityKey: arabicCityKey)
        if getInt("rules_v", 1) < AppSettings.rulesVersion {
            if placeSet {
                let p = place
                let m = method
                let def = defaultMethod(p)
                // لم يغيّر الطريقة بعد اختيار مكانه (updatePlace يحفظهما معاً، و updateMethod الطريقة وحدها)
                if m == string("place_method") && def != m {
                    putString("place_method", def)
                    putString("method", def)
                }
            }
            putInt("rules_v", AppSettings.rulesVersion)
        }
    }

    /// ترحيل بيانات الإصدارات العربية فقط: اسم المدينة المحفوظ نصاً يصبح مفتاحاً، ويبقى المستخدم الحالي على العربية
    public func migrate(arabicCityKey: (String) -> String?) {
        if isEmpty() { return }
        if !contains("lang") { putString("lang", "ar") }
        if contains("place_name") && !contains("place_key") {
            let name = string("place_name") ?? ""
            putString("place_key", arabicCityKey(name) ?? "")
            remove("place_name")
            remove("place_country")
        }
    }

    // ---------- الإعدادات ----------

    /// هل اختار المستخدم موقعه (GPS أو مدينة)؟ قبل ذلك نطلبه بدل افتراض مكة
    public var placeSet: Bool { contains("place_lat") }

    public var place: Place {
        guard placeSet else { return .makkah }
        let lat = Double(string("place_lat") ?? "0") ?? 0
        let lng = Double(string("place_lng") ?? "0") ?? 0
        return Place(
            key: string("place_key").flatMap { $0.isEmpty ? nil : $0 },
            lat: lat, lng: lng,
            tz: string("place_tz") ?? systemTimeZoneId(),
            method: string("place_method") ?? "mwl",
            gps: getBool("place_gps", false),
            // ما حُفظ قبل حساب الارتفاع: من المدينة المعروفة القريبة
            elev: string("place_elev").flatMap { Int($0) } ?? AppSettings.elevationNear(lat, lng)
        )
    }

    public func updatePlace(_ p: Place) {
        putString("place_key", p.key ?? "")
        putString("place_lat", kotlinDoubleString(p.lat))
        putString("place_lng", kotlinDoubleString(p.lng))
        putString("place_tz", p.tz)
        putString("place_method", p.method)
        putBool("place_gps", p.gps)
        putString("place_elev", String(p.elev))
        putString("method", p.method)
    }

    public var method: String { string("method") ?? place.method }
    public func updateMethod(_ v: String) { putString("method", v) }

    /// ما اختاره المستخدم لوقت العصر؛ nil: لم يختر، فيتبع المعتمد في طريقة الحساب
    public var hanafiChoice: Bool? { contains("hanafi") ? getBool("hanafi", false) : nil }
    /// العصر بمثلَي الظل: اختيار المستخدم، وإلا [methodDefault] (Method.hanafi لطريقة الحساب)
    public func hanafi(methodDefault: Bool) -> Bool { hanafiChoice ?? methodDefault }
    public func updateHanafi(_ v: Bool) { putBool("hanafi", v) }

    public var use24h: Bool { getBool("use24h", false) }
    public func updateUse24h(_ v: Bool) { putBool("use24h", v) }

    /// الإقامة بعد الأذان بالدقائق لكل صلاة (0 = لا تُعرض)
    public var iqama: [String: Int] {
        var m: [String: Int] = [:]
        for (k, v) in IQAMA_DEFAULT { m[k] = getInt("iqama_\(k)", v) }
        return m
    }
    public func iqama(_ prayer: String) -> Int { getInt("iqama_\(prayer)", IQAMA_DEFAULT[prayer] ?? 0) }
    public func updateIqama(_ prayer: String, _ minutes: Int) { putInt("iqama_\(prayer)", minutes) }

    /// system | light | dark
    public var theme: String { string("theme") ?? "system" }
    public func updateTheme(_ v: String) { putString("theme", v) }

    /// لون التطبيق: مفتاح من Accent
    public var accent: String { string("accent") ?? "emerald" }
    public func updateAccent(_ v: String) { putString("accent", v) }

    public var notify: Bool { getBool("notify", false) }
    public func updateNotify(_ v: Bool) { putBool("notify", v) }

    /// طريقة التنبيه لكل صلاة (الأذان افتراضياً)
    public var alertModes: [String: AlertMode] {
        var m: [String: AlertMode] = [:]
        for p in ALERT_PRAYERS { m[p] = AlertMode.of(string("alert_\(p)")) }
        return m
    }
    public func alertMode(_ prayer: String) -> AlertMode { ALERT_PRAYERS.contains(prayer) ? AlertMode.of(string("alert_\(prayer)")) : .adhan }
    public func updateAlertMode(_ prayer: String, _ mode: AlertMode) { putString("alert_\(prayer)", mode.key) }

    /// المؤذن لكل الصلوات، ولصلاة الفجر وحدها (Muezzins.same: مثل البقية)
    public var muezzin: String { string("muezzin") ?? Muezzins.default.id }
    public func updateMuezzin(_ id: String) { putString("muezzin", id) }
    public var fajrMuezzin: String { string("muezzin_fajr") ?? Muezzins.fajrDefault.id }
    public func updateFajrMuezzin(_ id: String) { putString("muezzin_fajr", id) }

    public var vibePattern: VibePattern { VibePattern.of(string("vibe_pattern")) }
    public func updateVibePattern(_ p: VibePattern) { putString("vibe_pattern", p.key) }

    /// "system" أو رمز لغة
    public var lang: String { string("lang") ?? "system" }
    public func updateLang(_ v: String) { putString("lang", v) }

    /// أنهى المستخدم التعريف بالتطبيق
    public var onboarded: Bool { getBool("onboarded", false) }
    public func updateOnboarded(_ v: Bool) { putBool("onboarded", v) }

    /// آخر إصدار رآه من صفحات الدليل
    public var guideSeen: Int { getInt("guideSeen", 0) }
    public func updateGuideSeen(_ v: Int) { putInt("guideSeen", v) }

    /// المسبحة وعدّاد الأذكار على شاشة القفل والجزيرة الديناميكية (LiveTasbih.KEY، مفعّل افتراضياً)
    public var liveCounters: Bool { getBool("live_counters", true) }
    public func updateLiveCounters(_ v: Bool) { putBool("live_counters", v) }
}

/// Double.toString في Kotlin: أقصر أرقام تعيد القيمة نفسها، عشرية بين 0.001 و 10^7، وإلا علمية (1.0E-5، 1.2345678E7)
public func kotlinDoubleString(_ v: Double) -> String {
    if v.isNaN { return "NaN" }
    if v.isInfinite { return v < 0 ? "-Infinity" : "Infinity" }
    if v == 0 { return v.sign == .minus ? "-0.0" : "0.0" }
    // أرقام Swift الأقصر وأسّها: "1.2345e-05" أو "21.4225" أو "1e+16"
    let desc = "\(abs(v))"
    var mantissa = desc
    var exp = 0
    if let e = desc.firstIndex(where: { $0 == "e" || $0 == "E" }) {
        mantissa = String(desc[..<e])
        exp = Int(desc[desc.index(after: e)...].replacingOccurrences(of: "+", with: "")) ?? 0
    }
    let parts = mantissa.split(separator: ".", omittingEmptySubsequences: false)
    let intPart = String(parts[0])
    let fracPart = parts.count > 1 ? String(parts[1]) : ""
    var digits = intPart + fracPart
    // الأس العشري للرقم الأول: القيمة = 0.d1d2… × 10^(point)
    var point = intPart.count + exp
    while digits.hasPrefix("0") && digits.count > 1 { digits.removeFirst(); point -= 1 }
    while digits.hasSuffix("0") && digits.count > 1 { digits.removeLast() }
    let sign = v < 0 ? "-" : ""
    let a = abs(v)
    if a >= 1e-3 && a < 1e7 {
        if point <= 0 {
            return sign + "0." + String(repeating: "0", count: -point) + digits
        }
        if digits.count <= point {
            return sign + digits + String(repeating: "0", count: point - digits.count) + ".0"
        }
        let i = digits.index(digits.startIndex, offsetBy: point)
        return sign + String(digits[..<i]) + "." + String(digits[i...])
    }
    let first = String(digits.prefix(1))
    let rest = digits.count > 1 ? String(digits.dropFirst()) : "0"
    return sign + first + "." + rest + "E" + String(point - 1)
}
