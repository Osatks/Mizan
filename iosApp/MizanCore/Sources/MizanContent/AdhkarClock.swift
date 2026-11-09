import Foundation
import MizanBase

/// مواقيت يوم بالساعات العشرية (PrayerTimes في MizanPrayer)؛ NaN لما تعذّر حسابه
public struct AdhkarDayTimes: Hashable {
    public let fajr: Double
    public let sunrise: Double
    public let dhuhr: Double
    public let asr: Double
    public let maghrib: Double
    public let isha: Double
    public init(fajr: Double, sunrise: Double, dhuhr: Double, asr: Double, maghrib: Double, isha: Double) {
        self.fajr = fajr; self.sunrise = sunrise; self.dhuhr = dhuhr; self.asr = asr; self.maghrib = maghrib; self.isha = isha
    }
}

/**
 نافذة من أوقات الأذكار في أداة «أذكار الوقت» — widget/AdhkarClock.kt.
 kind: adhan | iftar | afterPrayer | morning | dhikr | friday | arafah | evening | night | lastThird
 items: معرّفات الأذكار بالترتيب، و"@قسم" تعني القسم كله، و"@قسم/ن" مجموعته رقم ن.
 section: قسم الأذكار الذي يُفتح في التطبيق. tag: friday | fridayEve | ramadan | arafah | lastTen
 */
public struct AdhkarWindow: Hashable {
    public let kind: String
    public let start: LocalDateTime
    public let end: LocalDateTime
    public let section: String
    public let items: [String]
    public let prayer: String?
    public let tag: String?

    public init(kind: String, start: LocalDateTime, end: LocalDateTime, section: String, items: [String], prayer: String? = nil, tag: String? = nil) {
        self.kind = kind; self.start = start; self.end = end; self.section = section; self.items = items; self.prayer = prayer; self.tag = tag
    }

    /// مفتاح ثابت للنافذة: يُصفَّر عنده موضع التدوير الذي اختاره المستخدم
    public var key: String { "\(kind)@\(start)" }
}

/// قواعد اختيار الأذكار حسب الوقت (من مواقيت الصلاة المحسوبة للموقع)
public enum AdhkarClock {
    public static let adhanMin = 10
    public static let afterMin = 30
    /// كل كم ثانية ينتقل الذكر المعروض وحده
    public static let rotateSec = 30

    public static let adhan = ["@adhan"]
    public static let iftar = ["ram02", "ram03", "@adhan"]
    public static let after = ["@afterPrayer/0"]
    public static let morning = ["@morning"]
    public static let evening = ["@evening"]
    public static let dhikr = ["tsb01", "tsb03", "tsb04", "tsb06", "tsb07", "tsb02", "stg01", "stg03", "tsb14", "tsb09"]
    public static let friday = [WidgetText.kahfId, WidgetText.salawatId, "tsb14", "tsb16", "tsb15", "tsb17", "tsb20", "tsb19"]
    public static let arafah = ["haj06", "tsb04", "tsb01", "stg01"]
    public static let night = ["@sleep/0", "slp16"]
    public static let lastThird = ["stg04", "stg01", "stg03", "wak01", "wak02", "wak03"]

    private static let sectionMap = [
        "adhan": "adhan", "iftar": "ramadan", "afterPrayer": "afterPrayer", "morning": "morning",
        "dhikr": "tasbih", "friday": "tasbih", "arafah": "hajj", "evening": "evening",
        "night": "sleep", "lastThird": "istighfar",
    ]

    public static func sectionOf(_ kind: String) -> String { sectionMap[kind] ?? "morning" }

    /// مواقيت سليمة: ما تعذّر حسابه (العروض العالية) يُقدَّر، وتبقى مرتبة
    public static func sane(_ t: AdhkarDayTimes) -> [Double] {
        func ok(_ v: Double) -> Bool { !v.isNaN && !v.isInfinite }
        let dhuhr = ok(t.dhuhr) ? t.dhuhr : 12.0
        let sunrise = ok(t.sunrise) ? t.sunrise : dhuhr - 6
        let fajr = ok(t.fajr) && t.fajr < sunrise ? t.fajr : sunrise - 1.5
        let asr = ok(t.asr) && t.asr > dhuhr ? t.asr : dhuhr + 3.5
        let maghrib = ok(t.maghrib) && t.maghrib > asr ? t.maghrib : asr + 2.5
        let isha = ok(t.isha) && t.isha > maghrib ? t.isha : maghrib + 1.5
        return [fajr, sunrise, dhuhr, asr, maghrib, isha]
    }

    /// Double.toLong في Kotlin: NaN صفر، والقيم خارج المدى تُحد
    private static func toLong(_ x: Double) -> Int64 {
        if x.isNaN { return 0 }
        if x >= 9.223372036854775807e18 { return Int64.max }
        if x <= -9.223372036854775808e18 { return Int64.min }
        return Int64(x)
    }

    private static func at(_ day: LocalDate, _ hours: Double) -> LocalDateTime { day.atStartOfDay().plusSeconds(toLong(hours * 3600)) }

    /**
     نوافذ «يوم الأذكار» [day]: من فجره إلى فجر اليوم التالي، متصلة بلا فجوات.
     [times] مواقيت الموقع لكل يوم، و[hijri] التاريخ الهجري (Hijri.toHijri)
     */
    public static func day(_ day: LocalDate, times: (LocalDate) -> AdhkarDayTimes, hijri: (LocalDate) -> HDate?) -> [AdhkarWindow] {
        let t = sane(times(day))
        let nextFajr = at(day.plusDays(1), sane(times(day.plusDays(1)))[0])
        let fajr = at(day, t[0]), sunrise = at(day, t[1]), dhuhr = at(day, t[2])
        let asr = at(day, t[3]), maghrib = at(day, t[4]), isha = at(day, t[5])
        let duha = sunrise.plusSeconds(wholeSecondsBetween(sunrise, dhuhr) / 2)
        let lastThirdAt = nextFajr.plusSeconds(-(wholeSecondsBetween(maghrib, nextFajr) / 3))

        let h = hijri(day)
        let isFriday = day.dayOfWeek == 5
        let fridayEve = day.dayOfWeek == 4
        let ramadan = h?.m == 9
        let lastTen = ramadan && (h?.d ?? 0) >= 20 // ليلة ٢١ فما بعدها تبدأ مساء اليوم العشرين
        let isArafah = h != nil && h!.m == 12 && h!.d == 9

        let dayTag: String? = isArafah ? "arafah" : isFriday ? "friday" : ramadan ? "ramadan" : nil
        let nightTag: String? = lastTen ? "lastTen" : fridayEve ? "fridayEve" : ramadan ? "ramadan" : nil
        let dhikrKind = isArafah ? "arafah" : isFriday ? "friday" : "dhikr"
        let dhikrItems = isArafah ? arafah : isFriday ? friday : dhikr
        var nightExtra: [String] = []
        if lastTen { nightExtra.append("ram07") }
        if fridayEve { nightExtra.append(WidgetText.salawatId); nightExtra.append(WidgetText.kahfId) }

        // بدايات النوافذ بالترتيب؛ كل نافذة تنتهي عند بداية التي بعدها
        struct Mark { let kind: String; let at: LocalDateTime; let items: [String]; let prayer: String?; let tag: String? }
        var marks: [Mark] = []
        func prayer(_ key: String, _ p: LocalDateTime, _ tag: String?) {
            let isIftar = key == "maghrib" && ramadan
            marks.append(Mark(kind: isIftar ? "iftar" : "adhan", at: p, items: isIftar ? iftar : adhan, prayer: key, tag: tag))
            marks.append(Mark(kind: "afterPrayer", at: p.plusMinutes(adhanMin), items: after, prayer: key, tag: tag))
        }
        prayer("fajr", fajr, dayTag)
        marks.append(Mark(kind: "morning", at: fajr.plusMinutes(adhanMin + afterMin), items: morning, prayer: nil, tag: dayTag))
        marks.append(Mark(kind: dhikrKind, at: duha, items: dhikrItems, prayer: nil, tag: dayTag))
        prayer("dhuhr", dhuhr, dayTag)
        marks.append(Mark(kind: dhikrKind, at: dhuhr.plusMinutes(adhanMin + afterMin), items: dhikrItems, prayer: nil, tag: dayTag))
        prayer("asr", asr, dayTag)
        // آخر ساعة بعد عصر الجمعة: ساعة الإجابة أولاً ثم أذكار المساء
        marks.append(Mark(kind: "evening", at: asr.plusMinutes(adhanMin + afterMin), items: isFriday ? [WidgetText.hourId] + evening : evening, prayer: nil, tag: dayTag))
        prayer("maghrib", maghrib, nightTag)
        marks.append(Mark(kind: "evening", at: maghrib.plusMinutes(adhanMin + afterMin), items: evening, prayer: nil, tag: nightTag))
        prayer("isha", isha, nightTag)
        marks.append(Mark(kind: "night", at: isha.plusMinutes(adhanMin + afterMin), items: nightExtra + night, prayer: nil, tag: nightTag))
        marks.append(Mark(kind: "lastThird", at: lastThirdAt, items: nightExtra.filter { $0 == "ram07" } + lastThird, prayer: nil, tag: nightTag))

        // لا ترجع نافذة قبل التي سبقتها (ليالي الصيف القصيرة في العروض العالية)، وما صار طوله صفراً يُحذف
        var prev = fajr
        let starts: [LocalDateTime] = marks.map { m in
            let hi = Swift.max(prev, nextFajr)
            let s = m.at < prev ? prev : (m.at > hi ? hi : m.at)
            prev = s
            return s
        }
        var out: [AdhkarWindow] = []
        for i in marks.indices {
            let start = starts[i]
            let end = i + 1 < marks.count ? starts[i + 1] : Swift.max(nextFajr, start)
            if end <= start { continue }
            let m = marks[i]
            out.append(AdhkarWindow(kind: m.kind, start: start, end: end, section: sectionOf(m.kind), items: m.items, prayer: m.prayer, tag: m.tag))
        }
        return out
    }

    /// نوافذ [days] أيام متتالية بدءاً من فجر [from]
    public static func range(_ from: LocalDate, days: Int, times: (LocalDate) -> AdhkarDayTimes, hijri: (LocalDate) -> HDate?) -> [AdhkarWindow] {
        (0..<Swift.max(days, 0)).flatMap { day(from.plusDays($0), times: times, hijri: hijri) }
    }

    /// النافذة التي فيها [now] (قبل الفجر تعود إلى ليلة اليوم السابق)
    public static func at(_ now: LocalDateTime, times: (LocalDate) -> AdhkarDayTimes, hijri: (LocalDate) -> HDate?) -> AdhkarWindow {
        for d in [now.date.minusDays(1), now.date] {
            if let w = day(d, times: times, hijri: hijri).first(where: { now >= $0.start && now < $0.end }) { return w }
        }
        // لا يحدث عادة: قبل أول نافذة أو بعد آخرها
        return day(now.date, times: times, hijri: hijri)[0]
    }

    /// رقم الذكر المعروض: ينتقل كل [rotateSec] ثانية، ويزيد عليه ما تقدّم به المستخدم بزر «التالي»
    public static func index(_ now: LocalDateTime, _ w: AdhkarWindow, offset: Int, count: Int) -> Int {
        if count <= 0 { return 0 }
        let steps = Swift.max(wholeSecondsBetween(w.start, now) / Int64(rotateSec), 0)
        let r = Int((steps + Int64(offset)) % Int64(count))
        return r < 0 ? r + count : r
    }

    // ---------- من widget/WidgetData.kt ----------

    /// يفك "@قسم" و"@قسم/ن" إلى أذكارها، ويحذف ما ليس في المكتبة (بلا تكرار)
    public static func expand(_ tokens: [String]) -> [Dhikr] {
        var seen = Set<String>()
        var out: [Dhikr] = []
        for t in tokens {
            var list: [Dhikr] = []
            if t.hasPrefix("@") {
                let body = String(t.dropFirst())
                let id = body.components(separatedBy: "/")[0]
                if let sec = Adhkar.section(id) {
                    // substringAfter('/', "").toIntOrNull()
                    let g: Int? = t.contains("/") ? Int(String(t[t.range(of: "/")!.upperBound...])) : nil
                    if let g = g { list = g >= 0 && g < sec.groups.count ? sec.groups[g].items : [] } else { list = sec.items }
                }
            } else if let d = WidgetText.extra[t] ?? Adhkar.byId(t) {
                list = [d]
            }
            for d in list where seen.insert(d.id).inserted { out.append(d) }
        }
        return out
    }

    /// عنوان النافذة بلغة الواجهة
    public static func title(_ w: AdhkarWindow, lang: String, tr: AdhkarTr) -> String { title(kind: w.kind, lang: lang, tr: tr) }

    public static func title(kind: String, lang: String, tr: AdhkarTr) -> String {
        let wt = WidgetText.of(lang)
        switch kind {
        case "dhikr": return wt.dhikr
        case "friday": return wt.friday
        case "arafah": return wt.arafah
        case "iftar": return wt.iftar
        case "lastThird": return wt.lastThird
        case "afterPrayer": return wt.after
        default:
            let id = kind == "night" ? "sleep" : kind
            if lang == "ar" { return Adhkar.section(id)?.titleAr ?? id }
            return tr.sections[id]?.title ?? Adhkar.section(id)?.titleAr ?? id
        }
    }
}
