import Foundation

// مصادر لقطة الأدوات (النماذج في SnapshotModel.swift):
// ١) لقطة التطبيق في حاوية App Group أو سلسلة المفاتيح المشتركة — المفضلة، بإعدادات المستخدم كلها (AltStore وحساب مدفوع).
// ٢) لقطة تبنيها الأداة وحدها من موقعها والبيانات المضمّنة فيها (StandaloneSnapshot) — لتثبيت Sideloadly بحساب مجاني
//    الذي لا يمنح App Group ولا مجموعة سلسلة مفاتيح، فلا يصل من التطبيق شيء.

extension Snapshot {
    func label(_ key: String) -> String { labels[key] ?? Fallback.text(key) }
}

/// قراءة لقطة التطبيق من حاوية المجموعة
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

    /// حال لقطة التطبيق: G (لا حاوية مجموعة متاحة لهذا التوقيع) أو F (لا ملف بعد) أو D (ملف لا يُقرأ)، مع عدد المجموعات المعروفة
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

/// اللقطة التي تعرضها الأدوات: لقطة التطبيق ما دامت تغطي الآن، وإلا لقطة الأداة المستقلة من موقعها
enum Feed {
    /// لقطة التطبيق إن كان فيها اليوم الحالي
    static func appSnapshot(now: Date) -> Snapshot? {
        Store.load().flatMap { $0.day(at: now) == nil ? nil : $0 }
    }

    /// لقطة مستقلة لموقع: منطقة الجهاز ولغته وصيغة ساعته
    static func standalone(lat: Double, lng: Double, now: Date) -> Snapshot? {
        StandaloneSnapshot.build(.init(lat: lat, lng: lng, tz: TimeZone.current, now: now,
                                       lang: StaticStore.deviceLanguage, h24: StaticStore.deviceUses24h))
    }

    /// بلا انتظار (معاينة المعرض وزر «التالي»): لقطة التطبيق، أو المستقلة من آخر موقع محفوظ
    static func cached(now: Date) -> Snapshot? {
        if let s = appSnapshot(now: now) { return s }
        guard let last = LocationCache.last else { return nil }
        return standalone(lat: last.lat, lng: last.lng, now: now)
    }

    /// لخط الزمن: لقطة التطبيق، وإلا يُطلب موقع الأداة (ثم آخر موقع محفوظ). standalone: هل بُنيت في الأداة
    static func load(now: Date, completion: @escaping (_ snap: Snapshot?, _ standalone: Bool) -> Void) {
        if let s = appSnapshot(now: now) {
            completion(s, false)
            return
        }
        WidgetLocator.fetch { fix in
            let point = fix ?? LocationCache.last.map { (lat: $0.lat, lng: $0.lng) }
            completion(point.flatMap { standalone(lat: $0.lat, lng: $0.lng, now: now) }, true)
        }
    }

    /// سطر التشخيص الصغير تحت رسالة الموقع: حال إذن الموقع، ثم حال لقطة التطبيق
    static func diagnosis() -> String { "\(LocationCache.status) · \(Store.diagnosis())" }
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
        let now = Date()
        guard let snap = Feed.cached(now: now), let w = snap.window(at: now) else { return }
        let d = UserDefaults.standard
        d.set(w.key, forKey: keyName)
        d.set(offset(for: w.key) + 1, forKey: offsetName)
    }
}

/// نصوص بلا لقطة (أسماء الأدوات في المعرض، ورسالة إذن الموقع): بلغة الجهاز من البيانات المضمّنة،
/// وإن تعذّرت فالعربية أو الإنجليزية
enum Fallback {
    /// لغة الجهاز بين لغات التطبيق
    static let lang: String = StaticStore.deviceLanguage
    private static let strings: StaticLanguage? = StaticStore.language(lang)

    static var arabic: Bool {
        if let s = strings { return s.rtl }
        let l = Locale.preferredLanguages.first ?? "ar"
        return l.hasPrefix("ar") || l.hasPrefix("ur") || l.hasPrefix("fa")
    }

    private static let ar: [String: String] = [
        "adhkar": "أذكار الوقت", "nextPrayer": "الصلاة القادمة", "prayerTimes": "مواقيت الصلاة", "hijri": "التاريخ الهجري",
        "ayah": "آية اليوم", "next": "التالي", "in": "بعد", "openApp": "افتح ميزان مرة لتظهر مواقيتك هنا",
        "descNext": "الصلاة القادمة وعدّ تنازلي لها", "descTimes": "مواقيت اليوم الست والقادمة مميزة",
        "descHijri": "التاريخ الهجري والميلادي", "descAyah": "آية قصيرة كل يوم مع معناها",
        "descAdhkar": "الأذكار المناسبة للوقت: الصباح والمساء والنوم وبعد الصلاة والجمعة",
        "allowLocation": "اسمح لأدوات ميزان بمعرفة موقعك لتظهر مواقيتك",
        "locationHint": "الإعدادات ← ميزان ← الموقع: أثناء استخدام التطبيق أو الأدوات",
        "tasbih": "المسبحة", "descTasbih": "سبّح بلمسة من الشاشة الرئيسية أو شاشة القفل",
        "wird": "ورد اليوم", "descWird": "صفحات وردك اليوم وتقدّم ختمتك",
    ]
    private static let en: [String: String] = [
        "adhkar": "Adhkar for now", "nextPrayer": "Next prayer", "prayerTimes": "Prayer times", "hijri": "Hijri date",
        "ayah": "Ayah of the day", "next": "Next", "in": "in", "openApp": "Open Mizan once to show your times here",
        "descNext": "The next prayer with a live countdown", "descTimes": "Today's six times with the next one highlighted",
        "descHijri": "Hijri and Gregorian date", "descAyah": "A short ayah every day with its meaning",
        "descAdhkar": "The right adhkar for the time: morning, evening, sleep, after prayer and Friday",
        "allowLocation": "Allow location for Mizan widgets to show your times",
        "locationHint": "Settings › Mizan › Location: While Using the App or Widgets",
        "tasbih": "Tasbih", "descTasbih": "Count with a tap from the Home Screen or Lock Screen",
        "wird": "Today's portion", "descWird": "Today's Quran pages and your khatma progress",
    ]

    static func text(_ key: String) -> String {
        strings?.labels[key] ?? (arabic ? ar[key] : en[key]) ?? en[key] ?? key
    }

    /// أسماء الأدوات ووصفها في معرض الأدوات: من آخر لقطة للتطبيق إن وُجدت (لغة التطبيق)، وإلا لغة الجهاز
    static func gallery(_ key: String) -> String { galleryLabels?[key] ?? text(key) }

    /// تُقرأ مرة واحدة في كل تشغيل للأداة
    private static let galleryLabels: [String: String]? = Store.load()?.labels
}
