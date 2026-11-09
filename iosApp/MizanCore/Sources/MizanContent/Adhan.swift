import Foundation

/// الصلوات الخمس التي يُنبَّه لها — core/Adhan.kt
public let ALERT_PRAYERS = ["fajr", "dhuhr", "asr", "maghrib", "isha"]

/// طريقة التنبيه لكل صلاة
public enum AlertMode: String, CaseIterable {
    /// الأذان كاملاً (في iOS: أول 30 ثانية في الإشعار، وكاملاً إن كان التطبيق مفتوحاً)
    case adhan
    /// اهتزاز بلا صوت
    case vibrate
    /// إشعار صامت
    case silent
    /// لا تنبيه
    case off

    public var key: String { rawValue }

    /// من المفتاح المحفوظ، والأذان لغيره
    public static func of(_ k: String?) -> AlertMode { k.flatMap(AlertMode.init(rawValue:)) ?? .adhan }
}

/// مقطع مهتز من نمط: البداية والمدة بالملّي ثانية والشدة 0…255
public struct VibePulse: Hashable {
    public let start: Int64
    public let duration: Int64
    public let amp: Int
}

/// نمط اهتزاز: [timings] أزمنة متعاقبة بالملّي ثانية تبدأ بانتظار ثم اهتزاز ثم انتظار…، و[amps] شدة كل مقطع (0 للانتظار)
public enum VibePattern: String, CaseIterable {
    case short, double, triple, long, heartbeat, pulse, wave, rise, gentle, strong, knock, rapid

    public var key: String { rawValue }

    public var timings: [Int64] {
        switch self {
        case .short: return [0, 320]
        case .double: return [0, 110, 130, 110]
        case .triple: return [0, 90, 110, 90, 110, 90]
        case .long: return [0, 1400]
        case .heartbeat: return [0, 90, 120, 180, 650, 90, 120, 180, 650, 90, 120, 180]
        case .pulse: return [0, 450, 300, 450, 300, 450]
        case .wave: return [0, 220, 0, 220, 0, 220, 0, 220, 0, 220, 0, 220]
        case .rise: return [0, 260, 0, 260, 0, 260, 0, 260, 0, 260]
        case .gentle: return [0, 350, 300, 350]
        case .strong: return [0, 700, 180, 700]
        case .knock: return [0, 55, 100, 55, 420, 55, 100, 55]
        case .rapid: return [0] + Array(repeating: 45, count: 19)
        }
    }

    public var amps: [Int] {
        switch self {
        case .short: return [0, 255]
        case .double: return [0, 255, 0, 255]
        case .triple: return [0, 255, 0, 255, 0, 255]
        case .long: return [0, 230]
        case .heartbeat: return [0, 160, 0, 255, 0, 160, 0, 255, 0, 160, 0, 255]
        case .pulse: return [0, 255, 0, 255, 0, 255]
        case .wave: return [0, 60, 0, 130, 0, 200, 0, 255, 0, 170, 0, 90]
        case .rise: return [0, 40, 0, 90, 0, 145, 0, 200, 0, 255]
        case .gentle: return [0, 70, 0, 70]
        case .strong: return [0, 255, 0, 255]
        case .knock: return [0, 255, 0, 255, 0, 255, 0, 255]
        case .rapid: return (0..<20).map { $0 % 2 == 0 ? 0 : 220 }
        }
    }

    /// مجموع مدته بالملّي ثانية
    public var millis: Int64 { timings.reduce(0, +) }

    /// المقاطع المهتزة: (البداية، المدة، الشدة) — لرسم النمط وتشغيله
    public var pulses: [VibePulse] {
        var t: Int64 = 0
        var out: [VibePulse] = []
        for (i, ms) in timings.enumerated() {
            let start = t
            t += ms
            if amps[i] > 0 && ms > 0 { out.append(VibePulse(start: start, duration: ms, amp: amps[i])) }
        }
        return out
    }

    public static let `default` = VibePattern.heartbeat

    public static func of(_ k: String?) -> VibePattern { k.flatMap(VibePattern.init(rawValue:)) ?? .default }
}

/// تسجيل أذان: الملف الكامل adhan/<id>.m4a (مضمّن أو منزَّل من موقع ميزان)، وصوت إشعار iOS adhan_<id>.caf.
/// [takbir]: نهاية أول تكبيرتين بالثواني؛ الإشعار يُسمع حتى هنا ثم يتلاشى في [fade] ثوانٍ
public struct Muezzin: Hashable, Identifiable {
    public let id: String
    public let fajr: Bool
    public let bundled: Bool
    public let seconds: Int
    public let sizeKb: Int
    public let takbir: Double
    public let credit: String

    public init(id: String, fajr: Bool, bundled: Bool, seconds: Int, sizeKb: Int, takbir: Double, credit: String) {
        self.id = id; self.fajr = fajr; self.bundled = bundled; self.seconds = seconds; self.sizeKb = sizeKb; self.takbir = takbir; self.credit = credit
    }

    /// مدة تلاشي الصوت بعد التكبيرتين
    public static let fade = 2.5

    public var file: String { "\(id).m4a" }
    public var iosSound: String { "adhan_\(id).caf" }
}

/// تسجيلات الأذان (كلها برخص تسمح بإعادة النشر؛ التفاصيل في assets/licenses/adhan-recordings.txt) — core/AdhanCatalog.kt
public enum AdhanCatalog {
    public static let all: [Muezzin] = [
        // المؤذن الافتراضي: فؤاد (الأذان وأذان الفجر)، مضمّن في التطبيق
        Muezzin(id: "fouad", fajr: false, bundled: true, seconds: 146, sizeKb: 1184, takbir: 13.25,
                credit: "Fouad Adan · Public Domain Mark 1.0 · archive.org"),
        Muezzin(id: "fouad_fajr", fajr: true, bundled: true, seconds: 137, sizeKb: 1129, takbir: 10.25,
                credit: "Fouad Adan · Public Domain Mark 1.0 · archive.org"),
        Muezzin(id: "asim", fajr: false, bundled: true, seconds: 196, sizeKb: 1606, takbir: 9.45,
                credit: "Asim Javed (ICR) · CC0 1.0 · archive.org"),
        Muezzin(id: "klcc", fajr: false, bundled: false, seconds: 170, sizeKb: 1403, takbir: 21.0,
                credit: "Recorded by the archive.org uploader · CC0 1.0"),
        Muezzin(id: "mirza", fajr: false, bundled: false, seconds: 249, sizeKb: 2049, takbir: 17.8,
                credit: "Moazzambeg Mirza, Muhammadi Masjid, Sarkhej · Public Domain Mark 1.0 · archive.org"),
        Muezzin(id: "adam", fajr: false, bundled: false, seconds: 152, sizeKb: 1251, takbir: 15.2,
                credit: "Adam-synagda (own work) · CC0 1.0 · Wikimedia Commons"),
        Muezzin(id: "madinah", fajr: false, bundled: false, seconds: 187, sizeKb: 1535, takbir: 17.35,
                credit: "ejaz215 (Freesound) · CC BY 3.0 · Wikimedia Commons"),
        Muezzin(id: "andrewler", fajr: false, bundled: false, seconds: 158, sizeKb: 1304, takbir: 16.55,
                credit: "Andrewler (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin(id: "aaqib", fajr: false, bundled: false, seconds: 87, sizeKb: 712, takbir: 5.0,
                credit: "Atcovi (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin(id: "shalqar", fajr: false, bundled: false, seconds: 132, sizeKb: 1088, takbir: 8.4,
                credit: "Esetok (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin(id: "kalkan_fajr", fajr: true, bundled: true, seconds: 279, sizeKb: 2295, takbir: 11.25,
                credit: "Recorded by Piotrek Zyla (piotrekzyla.com) · Public Domain Mark 1.0 · radio aporee / archive.org"),
        Muezzin(id: "kas_fajr", fajr: true, bundled: false, seconds: 312, sizeKb: 2561, takbir: 18.85,
                credit: "Recorded by Heleste (ambientfeel.t.me) · Public Domain Mark 1.0 · radio aporee / archive.org"),
        Muezzin(id: "malmo_fajr", fajr: true, bundled: false, seconds: 234, sizeKb: 1920, takbir: 22.5,
                credit: "Islamic Center Malmö (YouTube) · CC BY 3.0 · Wikimedia Commons"),
    ]
}

/// التسجيلات المتاحة: المضمّنة، والبقية تُنزَّل عند اختيارها
public enum Muezzins {
    public static let base = "https://mizan-al-muslem.com/adhan/"
    /// اختيار أذان الفجر: مثل بقية الصلوات
    public static let same = "same"

    public static var all: [Muezzin] { AdhanCatalog.all }
    public static var `default`: Muezzin { all.first { $0.bundled && !$0.fajr }! }
    public static var fajrDefault: Muezzin { all.first { $0.bundled && $0.fajr }! }

    public static func of(_ id: String?) -> Muezzin? { all.first { $0.id == id } }

    /// الأذان المختار لصلاة، أو المضمّن إن لم يكن المختار على الجهاز. [available]: هل الملف على الجهاز (AdhanSounds.available)
    public static func forPrayer(_ prayer: String, muezzin: String, fajrMuezzin: String, available: (Muezzin) -> Bool) -> Muezzin {
        let fajrPick = prayer == "fajr" ? of(fajrMuezzin) : nil
        let pick = fajrPick ?? of(muezzin).flatMap { $0.fajr ? nil : $0 } ?? self.default
        return available(pick) ? pick : (pick.fajr ? fajrDefault : self.default)
    }
}
