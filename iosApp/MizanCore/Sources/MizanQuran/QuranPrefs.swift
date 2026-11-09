import Foundation

/// مخزن الإعدادات المحفوظة (AppSettings): المفاتيح والصيغ نفسها في Kotlin، فيعمل التطبيقان معاً أثناء الانتقال
public protocol QuranStore: AnyObject {
    func string(_ key: String) -> String?
    func setString(_ value: String, _ key: String)
    func int(_ key: String) -> Int?
    func setInt(_ value: Int, _ key: String)
}

/// NSUserDefaults القياسي، ومفاتيح مخزن "mizan" تبدأ بـ "mizan." (Prefs في Platform.ios.kt)
public final class UserDefaultsQuranStore: QuranStore {
    private let d: UserDefaults
    private let p: String

    public init(defaults: UserDefaults = .standard, name: String = "mizan") {
        d = defaults; p = name + "."
    }

    public func string(_ key: String) -> String? { d.string(forKey: p + key) }
    public func setString(_ value: String, _ key: String) { d.set(value, forKey: p + key) }
    public func int(_ key: String) -> Int? { d.object(forKey: p + key) == nil ? nil : d.integer(forKey: p + key) }
    public func setInt(_ value: Int, _ key: String) { d.set(value, forKey: p + key) }
}

/// مخزن في الذاكرة (للاختبارات)
public final class MemoryQuranStore: QuranStore {
    public var values: [String: Any]

    public init(_ values: [String: Any] = [:]) { self.values = values }

    public func string(_ key: String) -> String? {
        switch values[key] {
        case let s as String: return s
        case let i as Int: return String(i)
        default: return nil
        }
    }
    public func setString(_ value: String, _ key: String) { values[key] = value }
    public func int(_ key: String) -> Int? {
        switch values[key] {
        case let i as Int: return i
        case let s as String: return Int(s) ?? 0
        default: return nil
        }
    }
    public func setInt(_ value: Int, _ key: String) { values[key] = value }
}

public enum QuranSettings {
    /// المخزن الذي تقرأ منه إعدادات القراءة والختمة وتكتب فيه
    public static var store: QuranStore = UserDefaultsQuranStore()

    static func getInt(_ k: String, _ d: Int) -> Int { store.int(k) ?? d }
    static func getString(_ k: String, _ d: String = "") -> String { store.string(k) ?? d }
    static func putInt(_ k: String, _ v: Int) { store.setInt(v, k) }
    static func putString(_ k: String, _ v: String) { store.setString(v, k) }
}

/// إعدادات القراءة (QuranPrefs في QuranData.kt): آخر موضع، والآيات المحفوظة، وحجم الخط، وطريقة العرض، والمصدر المختار.
/// تُستعمل من الخيط الرئيسي؛ وتُرسل [changed] بعد كل تغيير لتحدّث الواجهة.
public enum QuranPrefs {
    public static let changed = Notification.Name("MizanQuranPrefsChanged")

    private static var loaded = false
    public private(set) static var bookmarks: [Int] = []
    public private(set) static var last = -1
    public private(set) static var textSize = 26
    /// "" = حسب اللغة | mushaf | list
    public private(set) static var mode = ""
    /// "" = حسب اللغة
    public private(set) static var source = ""
    /// المصادر التي أخفاها المستخدم من ورقة التفسير
    public private(set) static var hidden: [String] = []
    /// لون الورق في القراءة: "" مثل مظهر التطبيق | sepia ورقي دافئ | night ليلي
    public private(set) static var tone = ""
    /// سطوع الشاشة أثناء القراءة من 0 إلى 1، أو -1 لسطوع الجهاز
    public private(set) static var brightness: Float = -1
    /// القارئ (اسم مجلده في everyayah)، و repeat تكرار كل آية (0 = بلا نهاية)
    public private(set) static var reciter = ""
    public private(set) static var `repeat` = 1
    /// آخر موضع (أول آية في الصفحة) في وضع التركيز
    public private(set) static var focusLast = -1
    /// مقطع التلاوة: continue متواصل | page هذه الصفحة | surah هذه السورة | custom من آية إلى آية
    public private(set) static var rangeMode = "continue"
    public private(set) static var customFrom = 0
    public private(set) static var customTo = 6
    /// تكرار المقطع كله (0 = بلا نهاية)
    public private(set) static var rangeRepeat = 1
    /// وقفة بعد كل آية بالثواني (0 بلا وقفة، -1 بطول الآية)
    public private(set) static var gap = 0
    public private(set) static var speed: Float = 1
    /// مؤقت الإيقاف بالدقائق (0 بلا مؤقت)
    public private(set) static var sleep = 0
    /// خط المصحف: madinah مصحف المدينة (الافتراضي) | amiri الخط الآخر
    public private(set) static var script = "madinah"

    private static func notify() { NotificationCenter.default.post(name: changed, object: nil) }

    public static func ensure() {
        if loaded { return }
        loaded = true
        let S = QuranSettings.self
        last = S.getInt("q_last", -1)
        textSize = S.getInt("q_size", 26)
        mode = S.getString("q_mode")
        source = S.getString("q_src")
        hidden = S.getString("q_hidden").split(separator: ",", omittingEmptySubsequences: false).map(String.init).filter { !$0.isEmpty }
        tone = S.getString("q_tone")
        let b = S.getInt("q_bright", -1)
        brightness = b < 0 ? -1 : Float(b) / 100
        reciter = S.getString("q_reciter")
        `repeat` = S.getInt("q_repeat", 1)
        focusLast = S.getInt("q_focus_last", -1)
        let rm = S.getString("q_rmode")
        rangeMode = rm.isEmpty ? "continue" : rm
        customFrom = clamp(S.getInt("q_cfrom", 0), 0, QuranMeta.AYAHS - 1)
        customTo = clamp(S.getInt("q_cto", 6), 0, QuranMeta.AYAHS - 1)
        rangeRepeat = S.getInt("q_rrep", 1)
        gap = S.getInt("q_gap", 0)
        speed = Float(S.getInt("q_speed", 100)) / 100
        sleep = S.getInt("q_sleep", 0)
        let sc = S.getString("q_script")
        script = sc.isEmpty ? "madinah" : sc
        bookmarks = S.getString("q_marks").split(separator: ",", omittingEmptySubsequences: false)
            .compactMap { ktInt($0) }.filter { $0 >= 0 && $0 < QuranMeta.AYAHS }
    }

    /// يُنسى ما في الذاكرة فيُقرأ من المخزن عند الطلب التالي
    public static func reset() { loaded = false }

    /// يعيد القراءة من المخزن عند الطلب التالي (بعد تغيير المخزن)
    public static func reload() { loaded = false; ensure() }

    public static func modeFor(_ lang: String) -> String { mode.isEmpty ? (lang == "ar" ? "mushaf" : "list") : mode }
    public static func sourceFor(_ lang: String) -> TafsirSource {
        if let s = Sources.byId(source), !isHidden(s.id) { return s }
        return Sources.defaultFor(lang)
    }

    public static func isHidden(_ id: String) -> Bool { hidden.contains(id) }
    public static func setVisible(_ id: String, _ visible: Bool) {
        if visible { if let i = hidden.firstIndex(of: id) { hidden.remove(at: i) } } else if !hidden.contains(id) { hidden.append(id) }
        QuranSettings.putString("q_hidden", hidden.joined(separator: ","))
        notify()
    }

    public static func updateLast(_ i: Int) { if i != last { last = i; QuranSettings.putInt("q_last", i); notify() } }
    public static func updateSize(_ v: Int) { textSize = clamp(v, 18, 40); QuranSettings.putInt("q_size", textSize); notify() }
    public static func updateMode(_ v: String) { mode = v; QuranSettings.putString("q_mode", v); notify() }
    public static func updateSource(_ id: String) { source = id; QuranSettings.putString("q_src", id); notify() }
    public static func updateTone(_ v: String) { tone = v; QuranSettings.putString("q_tone", v); notify() }
    public static func updateBrightness(_ v: Float) {
        brightness = v < 0 ? -1 : min(max(v, 0.02), 1)
        QuranSettings.putInt("q_bright", v < 0 ? -1 : ktFloatToInt(brightness * 100))
        notify()
    }
    public static func updateReciter(_ id: String) { reciter = id; QuranSettings.putString("q_reciter", id); notify() }
    public static func updateRepeat(_ n: Int) { `repeat` = n; QuranSettings.putInt("q_repeat", n); notify() }
    public static func updateRangeMode(_ v: String) { rangeMode = v; QuranSettings.putString("q_rmode", v); notify() }
    public static func updateCustom(_ from: Int, _ to: Int) {
        customFrom = clamp(from, 0, QuranMeta.AYAHS - 1); customTo = clamp(to, customFrom, QuranMeta.AYAHS - 1)
        QuranSettings.putInt("q_cfrom", customFrom); QuranSettings.putInt("q_cto", customTo)
        notify()
    }
    public static func updateRangeRepeat(_ n: Int) { rangeRepeat = n; QuranSettings.putInt("q_rrep", n); notify() }
    public static func updateGap(_ n: Int) { gap = n; QuranSettings.putInt("q_gap", n); notify() }
    public static func updateSpeed(_ v: Float) { speed = v; QuranSettings.putInt("q_speed", ktFloatToInt(v * 100)); notify() }
    public static func updateSleep(_ n: Int) { sleep = n; QuranSettings.putInt("q_sleep", n); notify() }
    public static func updateScript(_ v: String) { script = v; QuranSettings.putString("q_script", v); notify() }
    public static func updateFocusLast(_ i: Int) { if i != focusLast { focusLast = i; QuranSettings.putInt("q_focus_last", i); notify() } }

    public static func isBookmarked(_ i: Int) -> Bool { bookmarks.contains(i) }
    public static func toggleBookmark(_ i: Int) {
        if let k = bookmarks.firstIndex(of: i) { bookmarks.remove(at: k) } else { bookmarks.insert(i, at: 0) }
        QuranSettings.putString("q_marks", bookmarks.map(String.init).joined(separator: ","))
        notify()
    }
}

func clamp(_ v: Int, _ lo: Int, _ hi: Int) -> Int { min(max(v, lo), hi) }

/// Float.toInt() في Kotlin: بتر نحو الصفر، و NaN صفر، ويُحصر في مدى Int بـ 32 بت
func ktFloatToInt(_ f: Float) -> Int {
    if f.isNaN { return 0 }
    if f >= 2147483648 { return Int(Int32.max) }
    if f <= -2147483648 { return Int(Int32.min) }
    return Int(f)
}
