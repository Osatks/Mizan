import Foundation

/// لقطة تبنيها الأداة وحدها من موقعها حين لا تصلها لقطة التطبيق (تثبيت بحساب Apple مجاني بلا App Group):
/// الحساب نفسه الذي يجريه WidgetData.snapshot في Kotlin، بالبيانات المضمّنة في الأداة، فتعرض الواجهات ما يعرضه التطبيق.
/// الطريقة من أقرب مدينة معروفة (كما يفعل LocationHelper عند اختيار GPS في التطبيق)، والعصر بالمذهب الجمهور،
/// ومنطقة الجهاز الزمنية، ولغة الجهاز، و١٢/٢٤ ساعة كما في الجهاز، واللون الزمردي.
enum StandaloneSnapshot {
    struct Input {
        var lat: Double
        var lng: Double
        var tz: TimeZone
        var now: Date
        var lang: String
        var h24: Bool
        var days: Int = 7
    }

    /// Math.round في Kotlin (round الذي يقرّب النصف إلى الزوجي) على أربعة منازل، كما في LocationHelper
    static func round4(_ x: Double) -> Double { (x * 10000).rounded(.toNearestOrEven) / 10000.0 }

    /// أقرب مدينة معروفة ومربع بعدها بالدرجات (PrayTimes.nearestCity)
    static func nearest(_ c: StaticCommon, lat: Double, lng: Double) -> (StaticCommon.City, Double)? {
        var best: (StaticCommon.City, Double)?
        for city in c.cities {
            let d = (city.lat - lat) * (city.lat - lat) + (city.lng - lng) * (city.lng - lng)
            if best == nil || d < best!.1 { best = (city, d) }
        }
        return best
    }

    static func build(_ input: Input) -> Snapshot? {
        guard let c = StaticStore.common, let l = StaticStore.language(input.lang) ?? StaticStore.language("en") else { return nil }
        return build(input, common: c, lang: l)
    }

    static func build(_ input: Input, common c: StaticCommon, lang l: StaticLanguage) -> Snapshot {
        let tz = input.tz
        let lat = round4(input.lat), lng = round4(input.lng)
        let near = nearest(c, lat: lat, lng: lng)
        let methodKey = near?.0.m ?? "mwl"
        let method = c.methods.first { $0.key == methodKey } ?? c.methods.first { $0.key == "mwl" } ?? c.methods[0]
        let placeName = (near.map { $0.1 < 1.5 } ?? false) ? (l.cities[near!.0.k] ?? near!.0.k) : l.myLocation
        let hijriCal = HijriCalendar(first: c.hijri.first, epoch: c.hijri.epoch, lengths: c.hijri.lengths)

        var cache: [Int: DayTimes] = [:]
        func times(_ d: CivilDate) -> DayTimes {
            if let t = cache[d.epochDay] { return t }
            let t = PrayEngine.compute(d, lat: lat, lng: lng, tz: tz, method: method, hanafi: false, hijri: hijriCal)
            cache[d.epochDay] = t
            return t
        }
        func hijri(_ d: CivilDate) -> HijriDate? { hijriCal.toHijri(d) }
        func epoch(_ local: Int64) -> Double { Double(Zone.instant(local, tz)) }
        func time(_ h: Double) -> String { PrayEngine.format(h, h24: input.h24, am: l.am, pm: l.pm) }

        let nowInstant = Int64(floor(input.now.timeIntervalSince1970))
        let nowLocal = Zone.local(nowInstant, tz)
        let today = LocalTime.date(nowLocal)

        var dayList: [Day] = []
        for i in 0..<input.days {
            let d = today.plus(i)
            let t = times(d)
            let h = hijri(d)
            let prayerTimes: [PrayerTime] = DayTimes.keys.filter { !t[$0].isNaN }.map { k in
                PrayerTime(k: k, name: l.prayers[k] ?? k, at: epoch(LocalTime.at(d, t[k])), t: time(t[k]),
                           s: PrayEngine.format(t[k], h24: input.h24, am: "", pm: "").trimmingCharacters(in: .whitespaces))
            }
            dayList.append(Day(
                date: d.iso,
                start: epoch(d.startLocal),
                weekday: l.weekdays[d.isoWeekday - 1],
                friday: d.isoWeekday == 5,
                greg: l.greg[d.m - 1].replacingOccurrences(of: "{d}", with: l.day(d.d)).replacingOccurrences(of: "{y}", with: "\(d.y)"),
                hijri: h.map { l.hijri[$0.m - 1].replacingOccurrences(of: "{d}", with: l.day($0.d)).replacingOccurrences(of: "{y}", with: "\($0.y)") } ?? "",
                hijriDay: h.map { "\($0.d)" } ?? "",
                hijriMonth: h.map { l.hijriMonth[$0.m - 1].replacingOccurrences(of: "{y}", with: "\($0.y)") } ?? "",
                times: prayerTimes,
                ayah: ayah(d, c, l)
            ))
        }

        // نوافذ الأذكار من ليلة أمس (ما قبل فجر اليوم) إلى آخر الأيام
        let spec = c.adhkar
        let rules = AdhkarRules.range(today.plus(-1), days: input.days + 1, spec: spec, times: times, hijri: hijri)
            .filter { epoch($0.end) > Double(nowInstant) - 3600 }
        var used: [String: Dhikr] = [:]
        let windows: [AdhkarWindow] = rules.map { w in
            let ids = spec.expand(w.items).filter { c.items[$0] != nil }
            for id in ids where used[id] == nil {
                let base = c.items[id]!
                let x = l.items[id]
                used[id] = Dhikr(ar: base.ar, n: base.n, c: x?.c, src: x?.src ?? "", tr: x?.tr, fadl: x?.fadl)
            }
            return AdhkarWindow(
                k: w.kind, key: w.key, s: epoch(w.start), e: epoch(w.end),
                title: l.titles[w.kind] ?? w.kind, chip: chip(w, l),
                until: l.until.replacingOccurrences(of: "%s", with: time(Double(LocalTime.secondOfDay(w.end)) / 3600.0)),
                section: w.section, items: ids
            )
        }

        return Snapshot(
            v: 1, at: Double(Zone.instant(nowLocal, tz)), lang: l.code, rtl: l.rtl, theme: "system", colors: c.colors,
            place: placeName, rotateSec: spec.rotateSec, labels: l.labels, days: dayList, windows: windows, items: used
        )
    }

    /// شارة النافذة: اسم الصلاة، أو الجمعة، أو ليلة الجمعة، أو العشر الأواخر (WidgetData.chip)
    static func chip(_ w: RuleWindow, _ l: StaticLanguage) -> String? {
        if let p = w.prayer { return l.prayers[p] ?? p }
        if w.tag == "friday" && w.kind != "friday" { return l.chips["friday"] }
        if w.tag == "fridayEve" { return l.chips["fridayEve"] }
        if w.tag == "lastTen" { return l.chips["lastTen"] }
        return nil
    }

    /// آية اليوم كما يختارها التطبيق (ayahOfDay) للأيام المضمّنة؛ بعدها تدور على الآيات نفسها حتى يُحدَّث الملف
    static func ayah(_ d: CivilDate, _ c: StaticCommon, _ l: StaticLanguage) -> Ayah? {
        let a = c.ayah
        guard !a.days.isEmpty else { return nil }
        var i = d.epochDay - a.start
        if i < 0 || i >= a.days.count { i = ((i % a.days.count) + a.days.count) % a.days.count }
        let pos = a.days[i]
        guard pos >= 0, pos < a.pool.count else { return nil }
        let p = a.pool[pos]
        let name = p.s >= 1 && p.s <= l.surahs.count ? l.surahs[p.s - 1] : "\(p.s)"
        let ref = l.surahAyah.replacingOccurrences(of: "{surah}", with: name).replacingOccurrences(of: "{ayah}", with: "\(p.a)")
        let meaning = pos < l.meanings.count ? l.meanings[pos] : nil
        return Ayah(text: p.text, ref: ref, meaning: meaning, route: p.route)
    }
}
