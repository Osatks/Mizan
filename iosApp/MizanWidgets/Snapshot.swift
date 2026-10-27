import Foundation

/// لقطة الأدوات التي يكتبها التطبيق (WidgetData.snapshot في Kotlin) في حاوية App Group.
/// الأوقات ثوانٍ منذ ١٩٧٠، والنصوص منسقة مسبقاً بلغة التطبيق وأرقامها.
struct Snapshot: Decodable {
    var v: Int
    var at: Double
    var lang: String
    var rtl: Bool
    var theme: String
    var colors: Palette
    var place: String
    var rotate: Int
    var labels: [String: String]
    var days: [Day]
    var windows: [AdhkarWindow]
    var items: [String: Dhikr]
}

struct Palette: Decodable {
    var hero1: String
    var hero2: String
    var night1: String
    var night2: String
    var accent: String
    var gold: String
}

struct Day: Decodable {
    var date: String
    var start: Double
    var weekday: String
    var friday: Bool
    var greg: String
    var hijri: String
    var hijriDay: String
    var hijriMonth: String
    var times: [PrayerTime]
    var ayah: Ayah?
}

struct PrayerTime: Decodable {
    var k: String
    var name: String
    var at: Double
    var t: String
    var s: String

    var date: Date { Date(timeIntervalSince1970: at) }
}

struct Ayah: Decodable {
    var text: String
    var ref: String
    var meaning: String?
    var route: String
}

struct AdhkarWindow: Decodable {
    var k: String
    var key: String
    var s: Double
    var e: Double
    var title: String
    var chip: String?
    var until: String
    var section: String
    var items: [String]
}

struct Dhikr: Decodable {
    var ar: String
    var n: Int
    var c: String?
    var src: String
    var tr: String?
    var fadl: String?
}

extension Snapshot {
    func label(_ key: String) -> String { labels[key] ?? Fallback.text(key) }

    /// اليوم الذي فيه [date]؛ nil إن انتهت أيام اللقطة (لم يُفتح التطبيق منذ أسبوع)
    func day(at date: Date) -> Day? {
        let t = date.timeIntervalSince1970
        for (i, d) in days.enumerated() {
            let end = i + 1 < days.count ? days[i + 1].start : d.start + 86_400
            if t >= d.start && t < end { return d }
        }
        return nil
    }

    /// الصلوات الخمس (بلا الشروق) في كل الأيام بالترتيب
    var prayers: [PrayerTime] { days.flatMap { $0.times }.filter { $0.k != "sunrise" } }

    func nextPrayer(after date: Date) -> PrayerTime? {
        let t = date.timeIntervalSince1970
        return prayers.first { $0.at > t }
    }

    func previousPrayer(before date: Date) -> PrayerTime? {
        let t = date.timeIntervalSince1970
        return prayers.last { $0.at <= t }
    }

    func window(at date: Date) -> AdhkarWindow? {
        let t = date.timeIntervalSince1970
        return windows.first { t >= $0.s && t < $0.e }
    }

    /// أذكار النافذة، والقصيرة فقط إن طُلب حد أقصى لطول النص (للأحجام الصغيرة)
    func dhikrs(_ w: AdhkarWindow, maxLength: Int = .max) -> [Dhikr] {
        let all = w.items.compactMap { items[$0] }
        let short = all.filter { $0.ar.count <= maxLength }
        return short.isEmpty ? all : short
    }

    /// رقم الذكر المعروض: ينتقل كل [rotate] دقيقة، ويزيد عليه ما تقدّم به المستخدم (مثل AdhkarClock.index)
    func index(at date: Date, in w: AdhkarWindow, offset: Int, count: Int) -> Int {
        guard count > 0 else { return 0 }
        let step = Double(max(rotate, 1) * 60)
        let steps = max(0, Int((date.timeIntervalSince1970 - w.s) / step))
        let i = (steps + offset) % count
        return i < 0 ? i + count : i
    }
}

/// قراءة اللقطة من حاوية المجموعة
enum Store {
    static let defaultGroup = "group.com.mizan.almuslim"
    static let file = "widget.json"

    /// AltStore يعيد تسمية المجموعة عند التوقيع بحساب مجاني ويكتب الاسم الجديد في ALTAppGroups؛
    /// نبحث في Info.plist الأداة وInfo.plist التطبيق الذي يحويها، ثم الاسم الأصلي.
    static func groups() -> [String] {
        var out: [String] = []
        // المجموعات التي منحها ملف التوقيع فعلاً: أصدق مصدر، فبعض أدوات التثبيت تعيد التسمية دون ALTAppGroups
        func profile(_ bundleURL: URL) {
            let url = bundleURL.appendingPathComponent("embedded.mobileprovision")
            guard let data = try? Data(contentsOf: url) else { return }
            out += profileGroups(String(decoding: data, as: UTF8.self))
        }
        func add(_ info: [String: Any]?) {
            if let alt = info?["ALTAppGroups"] as? [String] {
                out += alt.filter { $0.lowercased().contains("mizan") } + alt
            }
            if let own = info?["MizanAppGroup"] as? String { out.append(own) }
        }
        // Mizan.app/PlugIns/MizanWidgets.appex ← التطبيق على بعد مجلدين
        let appURL = Bundle.main.bundleURL.deletingLastPathComponent().deletingLastPathComponent()
        profile(Bundle.main.bundleURL)
        profile(appURL)
        add(Bundle.main.infoDictionary)
        if let app = Bundle(url: appURL) { add(app.infoDictionary) }
        out.append(defaultGroup)
        let ordered = out.filter { $0.lowercased().contains("mizan") } + out
        var seen = Set<String>()
        return ordered.filter { seen.insert($0).inserted }
    }

    /// أسماء App Group في نص ملف التوقيع (plist داخل توقيع CMS)
    static func profileGroups(_ text: String) -> [String] {
        guard let key = text.range(of: "<key>com.apple.security.application-groups</key>"),
              let open = text.range(of: "<array>", range: key.upperBound..<text.endIndex),
              let close = text.range(of: "</array>", range: open.upperBound..<text.endIndex) else { return [] }
        var groups: [String] = []
        var rest = text[open.upperBound..<close.lowerBound]
        while let a = rest.range(of: "<string>"), let b = rest.range(of: "</string>", range: a.upperBound..<rest.endIndex) {
            let g = rest[a.upperBound..<b.lowerBound].trimmingCharacters(in: .whitespacesAndNewlines)
            if g.hasPrefix("group.") { groups.append(g) }
            rest = rest[b.upperBound...]
        }
        return groups
    }

    static func container() -> URL? {
        for g in groups() {
            if let url = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: g) { return url }
        }
        return nil
    }

    /// سبب غياب اللقطة: G (لا حاوية مجموعة متاحة لهذا التوقيع) أو F (لا ملف بعد) أو D (ملف لا يُقرأ)، مع عدد المجموعات المعروفة
    static func diagnosis() -> String {
        let gs = groups()
        guard let dir = container() else {
            // بلا App Group: حالة قراءة سلسلة المفاتيح، وتجربة كتابة فيها (‎-34018 = لا صلاحية للمجموعة)
            let (data, r) = SharedKeychain.read()
            if let data = data {
                do { _ = try JSONDecoder().decode(Snapshot.self, from: data); return "K OK" }
                catch { return "KD · \(String(describing: error).prefix(60))" }
            }
            let team = SharedKeychain.teamPrefix() ?? "-"
            return "G · K\(r) · \(team)"
        }
        let url = dir.appendingPathComponent(file)
        guard let data = try? Data(contentsOf: url) else { return "F · \(gs.count)" }
        do { _ = try JSONDecoder().decode(Snapshot.self, from: data); return "OK" }
        catch { return "D · \(String(describing: error).prefix(60))" }
    }

    static func load() -> Snapshot? {
        if let url = container()?.appendingPathComponent(file), let data = try? Data(contentsOf: url),
           let snap = try? JSONDecoder().decode(Snapshot.self, from: data) { return snap }
        // بلا App Group: من سلسلة المفاتيح المشتركة
        guard let data = SharedKeychain.read().0 else { return nil }
        return try? JSONDecoder().decode(Snapshot.self, from: data)
    }
}

/// موضع المستخدم في أذكار النافذة الحالية (زر «التالي» في iOS 17)؛ يعود صفراً حين تتغير النافذة
enum AdhkarCursor {
    private static let keyName = "adhkar.window"
    private static let offsetName = "adhkar.offset"

    static func offset(for key: String?) -> Int {
        guard let key = key else { return 0 }
        let d = UserDefaults.standard
        return d.string(forKey: keyName) == key ? d.integer(forKey: offsetName) : 0
    }

    static func advance() {
        guard let snap = Store.load(), let w = snap.window(at: Date()) else { return }
        let d = UserDefaults.standard
        d.set(w.key, forKey: keyName)
        d.set(offset(for: w.key) + 1, forKey: offsetName)
    }
}

/// نصوص قبل أن يكتب التطبيق أول لقطة (أسماء الأدوات في المعرض)، بالعربية أو الإنجليزية حسب لغة الجهاز
enum Fallback {
    static var arabic: Bool {
        let lang = Locale.preferredLanguages.first ?? "ar"
        return lang.hasPrefix("ar") || lang.hasPrefix("ur") || lang.hasPrefix("fa")
    }

    private static let ar: [String: String] = [
        "adhkar": "أذكار الوقت", "nextPrayer": "الصلاة القادمة", "prayerTimes": "مواقيت الصلاة", "hijri": "التاريخ الهجري",
        "ayah": "آية اليوم", "next": "التالي", "in": "بعد", "openApp": "افتح ميزان مرة لتظهر مواقيتك هنا",
        "descNext": "الصلاة القادمة وعدّ تنازلي لها", "descTimes": "مواقيت اليوم الست والقادمة مميزة",
        "descHijri": "التاريخ الهجري والميلادي", "descAyah": "آية قصيرة كل يوم مع معناها",
        "descAdhkar": "الأذكار المناسبة للوقت: الصباح والمساء والنوم وبعد الصلاة والجمعة",
    ]
    private static let en: [String: String] = [
        "adhkar": "Adhkar for now", "nextPrayer": "Next prayer", "prayerTimes": "Prayer times", "hijri": "Hijri date",
        "ayah": "Ayah of the day", "next": "Next", "in": "in", "openApp": "Open Mizan once to show your times here",
        "descNext": "The next prayer with a live countdown", "descTimes": "Today's six times with the next one highlighted",
        "descHijri": "Hijri and Gregorian date", "descAyah": "A short ayah every day with its meaning",
        "descAdhkar": "The right adhkar for the time: morning, evening, sleep, after prayer and Friday",
    ]

    static func text(_ key: String) -> String { (arabic ? ar[key] : en[key]) ?? en[key] ?? key }

    /// أسماء الأدوات ووصفها في معرض الأدوات: من آخر لقطة إن وُجدت (لغة التطبيق)، وإلا لغة الجهاز
    static func gallery(_ key: String) -> String { galleryLabels?[key] ?? text(key) }

    /// تُقرأ مرة واحدة في كل تشغيل للأداة
    private static let galleryLabels: [String: String]? = Store.load()?.labels
}
