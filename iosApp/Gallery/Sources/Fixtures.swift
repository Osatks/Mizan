import Foundation

// حالات حقيقية من Kotlin بكل لغات التطبيق (LiveStatesTest يكتبها في live-fixtures.json قبل البناء، ios-gallery.yml)،
// تُفكّ هنا بأنواع Swift نفسها التي يفكّ بها التطبيق والأداة رسائل Kotlin: فشل الفكّ هنا فشله على الجهاز.

/// حالات لغة واحدة
struct LangFixture: Decodable {
    var tasbih: TasbihLiveState
    var tasbihSalah: TasbihLiveState
    var tasbihFree: TasbihLiveState
    /// أطول النصوص: تمام المائة بعد الصلاة، و«لا حول ولا قوة إلا بالله»، وأطول أذكار الصباح
    var tasbihTamam: TasbihLiveState
    var tasbihLong: TasbihLiveState
    var adhkar: AdhkarLiveState
    var adhkarDone: AdhkarLiveState
    var adhkarLong: AdhkarLiveState
    var prayer: PrayerLiveState
    var prayerIftar: PrayerLiveState
    var khatma: Khatma
    var khatmaDone: Khatma
    var snapshot: Snapshot
}

enum Fixtures {
    /// كل اللغات، وما تعذّر فكّه بسببه
    static let loaded: (all: [String: LangFixture], errors: [String]) = load()

    static var all: [String: LangFixture] { loaded.all }
    static var errors: [String] { loaded.errors }

    private static func load() -> ([String: LangFixture], [String]) {
        guard let url = Bundle.main.url(forResource: "live-fixtures", withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            return ([:], ["live-fixtures.json is missing from the app bundle"])
        }
        guard let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else {
            return ([:], ["live-fixtures.json is not a JSON object"])
        }
        var out: [String: LangFixture] = [:]
        var errors: [String] = []
        for (lang, value) in root.sorted(by: { $0.key < $1.key }) {
            do {
                let part = try JSONSerialization.data(withJSONObject: value)
                out[lang] = try LiveJSON.decoder.decode(LangFixture.self, from: part)
            } catch {
                errors.append("\(lang): \(describe(error))")
            }
        }
        return (out, errors)
    }

    /// موضع الخطأ في JSON: «prayer.iqamaTime: …»
    private static func describe(_ error: Error) -> String {
        guard let e = error as? DecodingError else { return String(describing: error) }
        func path(_ c: [CodingKey]) -> String { c.map { $0.intValue.map(String.init) ?? $0.stringValue }.joined(separator: ".") }
        switch e {
        case .keyNotFound(let k, let c): return "\(path(c.codingPath + [k])) missing"
        case .typeMismatch(let t, let c): return "\(path(c.codingPath)) is not \(t)"
        case .valueNotFound(let t, let c): return "\(path(c.codingPath)) null (\(t))"
        case .dataCorrupted(let c): return "\(path(c.codingPath)) corrupted: \(c.debugDescription)"
        @unknown default: return String(describing: e)
        }
    }
}

// ---------- الأوقات نسبةً إلى الآن ----------
// الحالات بأوقات ثابتة (يوم من 2026)، والعدّ التنازلي يُرسم من الساعة الآن: تُزاح كلها معاً فيبقى ما بينها كما هو

extension PrayerLiveState {
    /// الحال نفسها وقد صار وقت الصلاة [at]
    func moved(to at: Date) -> PrayerLiveState {
        let d = at.timeIntervalSince(self.at)
        var s = self
        s.at = at
        s.from = from.addingTimeInterval(d)
        s.iqama = iqama?.addingTimeInterval(d)
        s.until = until?.addingTimeInterval(d)
        return s
    }
}

extension Snapshot {
    /// اللقطة وقد كُتبت الآن (المواقيت ونوافذ الأذكار معها)
    func movedToNow() -> Snapshot {
        let d = Date().timeIntervalSince1970 - at
        var s = self
        s.at += d
        s.days = days.map { day in
            var x = day
            x.start += d
            x.times = day.times.map { t in
                var y = t
                y.at += d
                return y
            }
            return x
        }
        s.windows = windows.map { w in
            var x = w
            x.s += d
            x.e += d
            return x
        }
        return s
    }
}
