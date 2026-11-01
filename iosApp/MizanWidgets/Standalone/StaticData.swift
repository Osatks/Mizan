import Foundation

/// بيانات الأداة المستقلة المضمّنة فيها (Static/widget-static.json وملف لكل لغة widget-<code>.json).
/// تولّدها WidgetStaticTest من مصادر Kotlin (المدن والطرق وجدول الهجري وقواعد الأذكار ونصوصها وآية اليوم):
///   cd android-app && MIZAN_WIDGET_EXPORT=1 sh gradlew :shared:testDebugUnitTest --tests '*WidgetStaticTest*'
struct StaticCommon: Codable {
    struct City: Codable {
        let k: String
        let lat: Double
        let lng: Double
        let m: String
        /// الارتفاع عن البحر بالأمتار (City.elev)؛ غائب حين يكون صفراً
        let e: Int?
    }
    struct HijriTable: Codable {
        let first: Int
        let epoch: Int
        let lengths: String
    }
    struct Item: Codable {
        let ar: String
        let n: Int
    }
    struct PoolAyah: Codable {
        let text: String
        let s: Int
        let a: Int
        let route: String
    }
    struct AyahDays: Codable {
        /// أول يوم (عدد الأيام منذ 1970) ثم رقم آية كل يوم في pool
        let start: Int
        let days: [Int]
        let pool: [PoolAyah]
    }

    let v: Int
    let methods: [CalcMethod]
    let cities: [City]
    let hijri: HijriTable
    let adhkar: AdhkarSpec
    let items: [String: Item]
    let colors: Palette
    let ayah: AyahDays
}

/// نصوص لغة واحدة: قوالب التواريخ ({d} و {y})، وأسماء الصلوات والمدن، وعناوين النوافذ، وترجمات الأذكار ومعاني الآيات
struct StaticLanguage: Codable {
    struct Item: Codable {
        let c: String?
        let src: String
        let tr: String?
        let fadl: String?
    }

    let code: String
    let rtl: Bool
    let am: String
    let pm: String
    let weekdays: [String]
    /// كيف يُكتب رقم اليوم (الفرنسية: 1er)؛ غائب = الرقم نفسه
    let days: [String]?
    let greg: [String]
    let hijri: [String]
    let hijriMonth: [String]
    let prayers: [String: String]
    let myLocation: String
    let cities: [String: String]
    let labels: [String: String]
    let titles: [String: String]
    let chips: [String: String]
    let until: String
    let items: [String: Item]
    let surahAyah: String
    let surahs: [String]
    /// معنى آية كل عنصر في pool (فارغ للعربية)
    let meanings: [String?]

    func day(_ d: Int) -> String {
        if let days = days, d >= 1, d <= days.count { return days[d - 1] }
        return "\(d)"
    }
}

/// تحميل البيانات المضمّنة مرة واحدة في كل تشغيل للأداة
enum StaticStore {
    static let languages = ["ar", "en", "fr", "tr", "ur", "id", "ms", "fa", "bn", "ru", "de", "es", "uk", "uz", "kk"]

    /// مجلد البيانات: Static داخل حزمة الأداة (ويُمرَّر مجلد آخر في اختبار لينكس)
    static var directory: URL? = Bundle.main.url(forResource: "Static", withExtension: nil)
        ?? Bundle.main.resourceURL

    private static let lock = NSLock()
    private static var commonCache: StaticCommon?
    private static var langCache: [String: StaticLanguage] = [:]

    private static func read<T: Decodable>(_ name: String, _ type: T.Type) -> T? {
        guard let dir = directory else { return nil }
        let url = dir.appendingPathComponent(name)
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    static var common: StaticCommon? {
        lock.lock(); defer { lock.unlock() }
        if commonCache == nil { commonCache = read("widget-static.json", StaticCommon.self) }
        return commonCache
    }

    static func language(_ code: String) -> StaticLanguage? {
        lock.lock(); defer { lock.unlock() }
        if let l = langCache[code] { return l }
        let l = read("widget-\(code).json", StaticLanguage.self)
        langCache[code] = l
        return l
    }

    /// لغة الجهاز بين لغات التطبيق الخمس عشرة (أول لغة مفضلة متاحة، و"in" القديمة للإندونيسية)، وإلا الإنجليزية
    static func resolve(_ preferred: [String]) -> String {
        for tag in preferred {
            var c = String(tag.split(whereSeparator: { $0 == "-" || $0 == "_" }).first ?? "").lowercased()
            if c == "in" { c = "id" }
            if languages.contains(c) { return c }
        }
        return "en"
    }

    static var deviceLanguage: String { resolve(Locale.preferredLanguages) }

    /// ١٢ أو ٢٤ ساعة كما في إعداد الجهاز (لا يصل إلى الأداة إعداد التطبيق)
    static var deviceUses24h: Bool {
        let f = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: Locale.current) ?? "h a"
        return !f.contains("a")
    }
}
