import Foundation

/// نافذة من أوقات الأذكار (AdhkarWindow في Kotlin) بالتوقيت المحلي الساذج
struct RuleWindow: Equatable {
    let kind: String
    let start: Int64
    let end: Int64
    let section: String
    let items: [String]
    var prayer: String? = nil
    var tag: String? = nil

    /// مفتاح ثابت للنافذة: يُصفَّر عنده موضع التدوير الذي اختاره المستخدم ("kind@2026-10-27T05:00")
    var key: String { "\(kind)@\(LocalTime.iso(start))" }
}

/// قوائم الأذكار وثوابت النوافذ من widget-static.json (تصدّرها WidgetStaticTest من AdhkarClock)
struct AdhkarSpec: Codable {
    let adhanMin: Int
    let afterMin: Int
    let rotateSec: Int
    let lists: [String: [String]]
    let sections: [String: String]
    let tokens: [String: [String]]

    func list(_ name: String) -> [String] { lists[name] ?? [] }
    func section(_ kind: String) -> String { sections[kind] ?? "morning" }

    /// يفك "@قسم" و"@قسم/ن" إلى معرّفات أذكارها بلا تكرار (WidgetData.expand)
    func expand(_ items: [String]) -> [String] {
        var seen = Set<String>()
        var out: [String] = []
        for t in items {
            for id in tokens[t] ?? [] where seen.insert(id).inserted { out.append(id) }
        }
        return out
    }
}

/// قواعد اختيار الأذكار حسب الوقت: نقل widget/AdhkarClock.kt (day و sane) إلى Swift
enum AdhkarRules {
    static let kahf = "fri_kahf"
    static let salawat = "fri_salawat"
    static let hour = "fri_hour"
    static let laylatAlQadr = "ram07"

    /// مواقيت سليمة: ما تعذّر حسابه (العروض العالية) يُقدَّر، وتبقى مرتبة
    static func sane(_ t: DayTimes) -> [Double] {
        func ok(_ v: Double) -> Bool { !v.isNaN && !v.isInfinite }
        let dhuhr = ok(t.dhuhr) ? t.dhuhr : 12.0
        let sunrise = ok(t.sunrise) ? t.sunrise : dhuhr - 6
        let fajr = ok(t.fajr) && t.fajr < sunrise ? t.fajr : sunrise - 1.5
        let asr = ok(t.asr) && t.asr > dhuhr ? t.asr : dhuhr + 3.5
        let maghrib = ok(t.maghrib) && t.maghrib > asr ? t.maghrib : asr + 2.5
        let isha = ok(t.isha) && t.isha > maghrib ? t.isha : maghrib + 1.5
        return [fajr, sunrise, dhuhr, asr, maghrib, isha]
    }

    /// نوافذ «يوم الأذكار» [day]: من فجره إلى فجر اليوم التالي، متصلة بلا فجوات
    static func day(_ day: CivilDate, spec: AdhkarSpec, times: (CivilDate) -> DayTimes,
                    hijri: (CivilDate) -> HijriDate?) -> [RuleWindow] {
        let t = sane(times(day))
        let nextFajr = LocalTime.at(day.plus(1), sane(times(day.plus(1)))[0])
        let fajr = LocalTime.at(day, t[0]), sunrise = LocalTime.at(day, t[1]), dhuhr = LocalTime.at(day, t[2])
        let asr = LocalTime.at(day, t[3]), maghrib = LocalTime.at(day, t[4]), isha = LocalTime.at(day, t[5])
        let duha = sunrise + (dhuhr - sunrise) / 2
        let lastThird = nextFajr - (nextFajr - maghrib) / 3

        let h = hijri(day)
        let friday = day.isoWeekday == 5
        let fridayEve = day.isoWeekday == 4
        let ramadan = h?.m == 9
        let lastTen = ramadan && (h?.d ?? 0) >= 20 // ليلة ٢١ فما بعدها تبدأ مساء اليوم العشرين
        let arafah = h != nil && h!.m == 12 && h!.d == 9

        let dayTag: String? = arafah ? "arafah" : friday ? "friday" : ramadan ? "ramadan" : nil
        let nightTag: String? = lastTen ? "lastTen" : fridayEve ? "fridayEve" : ramadan ? "ramadan" : nil
        let dhikrKind = arafah ? "arafah" : friday ? "friday" : "dhikr"
        let dhikrItems = spec.list(arafah ? "arafah" : friday ? "friday" : "dhikr")
        var nightExtra: [String] = []
        if lastTen { nightExtra.append(laylatAlQadr) }
        if fridayEve { nightExtra += [salawat, kahf] }

        struct Mark { let kind: String; let at: Int64; let items: [String]; let prayer: String?; let tag: String? }
        var marks: [Mark] = []
        let adhanMin = Int64(spec.adhanMin) * 60
        let afterMin = Int64(spec.afterMin) * 60
        func prayer(_ key: String, _ p: Int64, _ tag: String?) {
            let iftar = key == "maghrib" && ramadan
            marks.append(Mark(kind: iftar ? "iftar" : "adhan", at: p, items: spec.list(iftar ? "iftar" : "adhan"), prayer: key, tag: tag))
            marks.append(Mark(kind: "afterPrayer", at: p + adhanMin, items: spec.list("after"), prayer: key, tag: tag))
        }
        let evening = spec.list("evening")
        prayer("fajr", fajr, dayTag)
        marks.append(Mark(kind: "morning", at: fajr + adhanMin + afterMin, items: spec.list("morning"), prayer: nil, tag: dayTag))
        marks.append(Mark(kind: dhikrKind, at: duha, items: dhikrItems, prayer: nil, tag: dayTag))
        prayer("dhuhr", dhuhr, dayTag)
        marks.append(Mark(kind: dhikrKind, at: dhuhr + adhanMin + afterMin, items: dhikrItems, prayer: nil, tag: dayTag))
        prayer("asr", asr, dayTag)
        // آخر ساعة بعد عصر الجمعة: ساعة الإجابة أولاً ثم أذكار المساء
        marks.append(Mark(kind: "evening", at: asr + adhanMin + afterMin, items: friday ? [hour] + evening : evening, prayer: nil, tag: dayTag))
        prayer("maghrib", maghrib, nightTag)
        marks.append(Mark(kind: "evening", at: maghrib + adhanMin + afterMin, items: evening, prayer: nil, tag: nightTag))
        prayer("isha", isha, nightTag)
        marks.append(Mark(kind: "night", at: isha + adhanMin + afterMin, items: nightExtra + spec.list("night"), prayer: nil, tag: nightTag))
        marks.append(Mark(kind: "lastThird", at: lastThird, items: nightExtra.filter { $0 == laylatAlQadr } + spec.list("lastThird"),
                          prayer: nil, tag: nightTag))

        // لا ترجع نافذة قبل التي سبقتها (ليالي الصيف القصيرة في العروض العالية)، وما صار طوله صفراً يُحذف
        var prev = fajr
        let starts: [Int64] = marks.map { mk in
            let s = min(max(mk.at, prev), max(prev, nextFajr))
            prev = s
            return s
        }
        var out: [RuleWindow] = []
        for i in marks.indices {
            let start = starts[i]
            let end = i + 1 < marks.count ? starts[i + 1] : max(nextFajr, start)
            if end <= start { continue }
            let mk = marks[i]
            out.append(RuleWindow(kind: mk.kind, start: start, end: end, section: spec.section(mk.kind), items: mk.items,
                                  prayer: mk.prayer, tag: mk.tag))
        }
        return out
    }

    /// نوافذ [days] أيام متتالية بدءاً من فجر [from]
    static func range(_ from: CivilDate, days: Int, spec: AdhkarSpec, times: (CivilDate) -> DayTimes,
                      hijri: (CivilDate) -> HijriDate?) -> [RuleWindow] {
        (0..<days).flatMap { day(from.plus($0), spec: spec, times: times, hijri: hijri) }
    }
}
