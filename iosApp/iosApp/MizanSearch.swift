import Foundation
import AppIntents
import CoreSpotlight
import Shared

// بحث iPhone: Spotlight يفهرس السور والآيات ومحتوى التطبيق من Kotlin (SystemSpotlight.kt) ولمس النتيجة يفتحها
// (SystemBridge.swift: SpotlightLink). وهنا كيانات App Intents (iOS 16 فما بعد) للسورة والآية، فيجدها Siri
// و«ابحث أو اسأل» وتطبيق الاختصارات بالبحث فيها، ونيّتا «افتح سورة» و«افتح آية». البيانات بلغة التطبيق من IosSearch.kt.

/// سورة كما يصفها Kotlin (IosSearch.surahMap): المفاتيح نفسها
struct SurahItem: Decodable {
    let n: Int
    let title: String
    let subtitle: String
    let route: String
}

/// آية كما يصفها Kotlin (IosSearch.ayahs): المفاتيح نفسها، والمعنى null إن لم تكن ترجمة اللغة على الجهاز
struct AyahItem: Decodable {
    let i: Int
    let title: String
    let text: String
    let meaning: String?
    let route: String
}

enum SearchJSON {
    static func surahs(_ json: String) -> [SurahItem] { decode(json) ?? [] }
    static func ayahs(_ json: String) -> [AyahItem] { decode(json) ?? [] }

    private static func decode<T: Decodable>(_ json: String) -> T? {
        guard let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}

/// إعادة فهرسة السور في Spotlight تعني أن لغة التطبيق تغيّرت: تُحدَّث عبارات Siri التي فيها أسماء السور
enum SearchIndexSync {
    private static var started = false

    static func start() {
        guard !started else { return }
        started = true
        // بعد أن يستقر الإقلاع: النظام يطلب السور (SurahQuery.suggestedEntities) ليبني العبارات
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) { SearchIndexSync.refreshShortcutParameters() }
        NotificationCenter.default.addObserver(forName: Notification.Name("MizanSpotlightIndexed"), object: nil, queue: .main) { _ in
            SearchIndexSync.refreshShortcutParameters()
        }
    }

    private static func refreshShortcutParameters() {
        if #available(iOS 17.0, *) { MizanShortcuts.updateAppShortcutParameters() }
    }
}

// ---------- السورة ----------

@available(iOS 16.0, *)
struct SurahEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Surah"
    static var defaultQuery = SurahQuery()

    /// رقم السورة (1…114)
    let id: Int
    let title: String
    let subtitle: String
    let route: String

    init(_ item: SurahItem) {
        id = item.n
        title = item.title
        subtitle = item.subtitle
        route = item.route
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)", subtitle: "\(subtitle)")
    }
}

@available(iOS 16.0, *)
struct SurahQuery: EntityStringQuery {
    @MainActor
    func entities(for identifiers: [Int]) async throws -> [SurahEntity] {
        let all = SurahQuery.all()
        return identifiers.compactMap { id in all.first { $0.id == id } }
    }

    @MainActor
    func entities(matching string: String) async throws -> [SurahEntity] {
        SearchJSON.surahs(IosSearch.shared.surahsMatchingJson(query: string)).map(SurahEntity.init)
    }

    @MainActor
    func suggestedEntities() async throws -> [SurahEntity] {
        SurahQuery.all()
    }

    @MainActor
    static func all() -> [SurahEntity] {
        SearchJSON.surahs(IosSearch.shared.surahsJson()).map(SurahEntity.init)
    }
}

// ---------- الآية ----------

@available(iOS 16.0, *)
struct AyahEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Ayah"
    static var defaultQuery = AyahQuery()

    /// فهرس الآية في المصحف (0…6235)
    let id: Int
    let title: String
    let text: String
    let meaning: String?
    let route: String

    init(_ item: AyahItem) {
        id = item.i
        title = item.title
        text = item.text
        meaning = item.meaning
        route = item.route
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)", subtitle: "\(meaning ?? text)")
    }
}

@available(iOS 16.0, *)
struct AyahQuery: EntityStringQuery {
    @MainActor
    func entities(for identifiers: [Int]) async throws -> [AyahEntity] {
        let ids = identifiers.map { String($0) }.joined(separator: ",")
        return SearchJSON.ayahs(IosSearch.shared.ayahsJson(ids: ids)).map(AyahEntity.init)
    }

    @MainActor
    func entities(matching string: String) async throws -> [AyahEntity] {
        SearchJSON.ayahs(IosSearch.shared.ayahsMatchingJson(query: string, limit: 20)).map(AyahEntity.init)
    }

    @MainActor
    func suggestedEntities() async throws -> [AyahEntity] {
        SearchJSON.ayahs(IosSearch.shared.suggestedAyahsJson()).map(AyahEntity.init)
    }
}

// iOS 18: كيانات قابلة للفهرسة في Spotlight (IndexedEntity)، بخصائص البحث المشتقة من وصفها.
// عناصر Spotlight نفسها يكتبها Kotlin لكل الإصدارات (iOS 15 فما بعد)، فلا تُفهرس الكيانات مرة ثانية فتتكرر النتائج.
@available(iOS 18.0, *)
extension SurahEntity: IndexedEntity {}

@available(iOS 18.0, *)
extension AyahEntity: IndexedEntity {}

// ---------- النوايا ----------

/// يفتح سورة في المصحف (أو عرض الآيات، بحسب اختيار المستخدم). OpenIntent: يفتح النظام التطبيق ثم يُنفّذ perform فيه،
/// ولو كان مغلقاً (يُحفظ الرابط حتى تجهز الواجهة: IosNav.openLink)
@available(iOS 16.0, *)
struct OpenSurahIntent: OpenIntent {
    static var title: LocalizedStringResource = "Open surah"
    static var description = IntentDescription("Opens a surah in the Quran")

    @Parameter(title: "Surah")
    var target: SurahEntity

    init() {}

    init(target: SurahEntity) {
        self.target = target
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: target.route, section: nil)
        return .result()
    }
}

/// يفتح المصحف على آية
@available(iOS 16.0, *)
struct OpenAyahIntent: OpenIntent {
    static var title: LocalizedStringResource = "Open ayah"
    static var description = IntentDescription("Opens an ayah in the Quran")

    @Parameter(title: "Ayah")
    var target: AyahEntity

    init() {}

    init(target: AyahEntity) {
        self.target = target
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: target.route, section: nil)
        return .result()
    }
}

/// أذكار الصباح أو المساء: اختصار واحد بمعامل (MizanShortcuts) فيبقى عدد الاختصارات الجاهزة في حدود النظام
@available(iOS 16.0, *)
enum AdhkarTime: String, AppEnum {
    case morning, evening

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Adhkar"
    static var caseDisplayRepresentations: [AdhkarTime: DisplayRepresentation] = [
        .morning: "Morning adhkar",
        .evening: "Evening adhkar",
    ]
}

@available(iOS 16.0, *)
struct OpenAdhkarIntent: AppIntent {
    static var title: LocalizedStringResource = "Adhkar"
    static var openAppWhenRun: Bool = true

    @Parameter(title: "Adhkar", default: .morning)
    var time: AdhkarTime

    init() {}

    init(time: AdhkarTime) {
        self.time = time
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        IosLive.shared.open(route: "adhkar", section: time.rawValue)
        return .result()
    }
}
