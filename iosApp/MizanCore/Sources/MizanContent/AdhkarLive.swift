import Foundation
import MizanBase

/**
 عدّاد الأذكار على شاشة القفل وفي الجزيرة الديناميكية — adhkar/AdhkarLive.kt:
 أذكار الصباح من الفجر إلى العصر، والمساء من العصر إلى الفجر. كل ضغطة تعدّ مرة من الذكر الحالي، فإذا بلغ عدده
 انتقل إلى الذي بعده، حتى يتم القسم. الموضع محفوظ لليوم والقسم (al_*)، فيُتابَع من حيث تُرك.
 */
public enum AdhkarLive {
    /// ما حدث مع الضغطة: عدّ، أو انتقال إلى الذكر التالي، أو إتمام القسم
    public enum Event: String { case count = "COUNT", next = "NEXT", done = "DONE" }

    public struct Position: Hashable {
        public var section: String
        public var index: Int
        public var count: Int
        public var day: String
        public var done: Bool
        public init(section: String, index: Int, count: Int, day: String, done: Bool = false) {
            self.section = section; self.index = index; self.count = count; self.day = day; self.done = done
        }
    }

    /// المخزن واليوم؛ يُستبدلان في الاختبارات
    public static var settings: AppSettings = .shared
    public static var today: () -> String = { LocalDate.now().description }

    /// أذكار الوقت: الصباح من الفجر حتى العصر، وإلا المساء. [hour] الساعة العشرية الآن، و[fajr] و[asr] مواقيت اليوم
    public static func section(hour h: Double, fajr: Double, asr: Double) -> String {
        (!fajr.isNaN && !asr.isNaN && h >= fajr && h < asr) ? "morning" : "evening"
    }

    /// موضع القسم: المحفوظ إن كان القسم نفسه اليوم، وإلا من أوله
    public static func position(_ section: String) -> Position {
        let s = settings
        let d = today()
        let saved = Position(section: s.getString("al_section", ""), index: s.getInt("al_index", 0), count: s.getInt("al_count", 0),
                             day: s.getString("al_day", ""), done: s.getBool("al_done", false))
        return saved.section == section && saved.day == d ? saved : Position(section: section, index: 0, count: 0, day: d)
    }

    private static func save(_ p: Position) {
        let s = settings
        s.putString("al_section", p.section)
        s.putInt("al_index", p.index)
        s.putInt("al_count", p.count)
        s.putString("al_day", p.day)
        s.putBool("al_done", p.done)
    }

    private static func items(_ section: String) -> [Dhikr] { Adhkar.section(section)?.items ?? [] }

    /// ضغطة: مرة من الذكر الحالي، والانتقال إلى التالي عند تمام عدده
    @discardableResult
    public static func count(_ section: String) -> (Position, Event) {
        let p = position(section)
        let list = items(section)
        if list.isEmpty || p.done { return (p, .done) }
        let d = list[min(max(p.index, 0), list.count - 1)]
        let c = p.count + 1
        var next = p
        let event: Event
        if c < max(d.count, 1) { next.count = c; event = .count }
        else if p.index + 1 < list.count { next.index = p.index + 1; next.count = 0; event = .next }
        else { next.count = 0; next.done = true; event = .done }
        save(next)
        return (next, event)
    }

    /// الذكر التالي دون إتمام عدد الحالي
    @discardableResult
    public static func skip(_ section: String) -> (Position, Event) {
        let p = position(section)
        let list = items(section)
        if list.isEmpty || p.done { return (p, .done) }
        var next = p
        if p.index + 1 < list.count { next.index = p.index + 1; next.count = 0 } else { next.count = 0; next.done = true }
        save(next)
        return (next, next.done ? .done : .next)
    }

    /// يبدأ القسم من أوله (بعد إتمامه أو من زر «من البداية»)
    @discardableResult
    public static func restart(_ section: String) -> Position {
        let p = Position(section: section, index: 0, count: 0, day: today())
        save(p)
        return p
    }

    /// نص قصير بقدر ما يسعه النشاط الحي: يُقطع عند آخر كلمة قبل [max] حرفاً (الطول بوحدات UTF-16 كما في Kotlin)
    public static func clip(_ text: String, _ max: Int) -> String {
        let t = Array(text.replacingOccurrences(of: "\n", with: " ").trimmingCharacters(in: kotlinTrim).utf16)
        if t.count <= max { return String(decoding: t, as: UTF16.self) }
        // lastIndexOf(' ', max): آخر مسافة في الموضع max أو قبله
        var cut = -1
        var i = min(max, t.count - 1)
        while i >= 0 { if t[i] == 32 { cut = i; break }; i -= 1 }
        if !(cut > max / 2) { cut = max }
        var head = Array(t[0..<cut])
        let strip: Set<UInt16> = [0x060C, 0x2C, 0x20]
        while let last = head.last, strip.contains(last) { head.removeLast() }
        return String(decoding: head, as: UTF16.self) + " …"
    }

    /// الحال للنشاط الحي (AdhkarLiveState في Swift) بلا ما يأتي من نصوص الواجهة (tap) واتجاه اللغة (rtl)
    public struct State: Hashable {
        public let id: String
        public let section: String
        public let title: String
        public let text: String
        public let meaning: String
        public let count: Int
        public let goal: Int
        public let countText: String
        public let position: String
        public let progress: Double
        public let done: Bool
        public let doneText: String
        public let next: String
        public let again: String
        public let end: String
    }

    public static func state(_ p: Position, lang: String) -> State {
        let sec = Adhkar.section(p.section)
        let tr = Adhkar.tr(lang)
        let list = sec?.items ?? []
        let d: Dhikr? = list.isEmpty ? nil : {
            let i = min(Swift.max(p.index, 0), Swift.max(list.count - 1, 0))
            return list[i]
        }()
        let title = lang == "ar" ? (sec?.titleAr ?? "") : (tr.sections[p.section]?.title ?? sec?.titleAr ?? "")
        let n = d.map { Swift.max($0.count, 1) } ?? 1
        let done = p.done
        let progress = list.isEmpty ? 0.0 : done ? 1.0 : (Double(p.index) + Double(p.count) / Double(n)) / Double(list.count)
        let meaning = (lang == "ar" || d == nil) ? "" : (tr.items[d!.id]?.t.map { clip($0, 90) } ?? "")
        let lt = LiveText.of(lang)
        return State(
            id: "adhkar-\(p.section)", section: p.section, title: title,
            text: done ? "" : (d.map { clip($0.ar, 130) } ?? ""),
            meaning: done ? "" : meaning,
            count: p.count, goal: n, countText: "\(p.count)/\(n)",
            position: "\(Swift.min(p.index + 1, list.count))/\(list.count)",
            progress: Swift.min(Swift.max(progress, 0), 1),
            done: done, doneText: lt.adhkarDone(title), next: WidgetText.of(lang).next, again: lt.again, end: lt.end
        )
    }
}
