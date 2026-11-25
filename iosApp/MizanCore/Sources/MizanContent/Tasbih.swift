import Foundation
import MizanBase

/// حال المسبحة: الذكر المختار، والعدد، والهدف، والدورات، وعدد اليوم والمجموع — core/Tasbih.kt.
/// قيمة ثابتة تُحسب منها الحال التالية ([count]) فتُختبر بلا تخزين ولا واجهة
public struct TasbihState: Hashable {
    /// free: ذكر يختاره المستخدم بهدف يختاره، و salah: تسبيح ما بعد الصلاة (33 + 33 + 33 ثم تمام المائة)
    public var mode: String
    /// الذكر المختار في free: موضعه في Tasbih.dhikr
    public var idx: Int
    /// الخطوة في salah: موضعها في Tasbih.salah
    public var step: Int
    public var count: Int
    /// هدف free: 33 أو 34 أو 100 أو 1000، و 0 بلا حد
    public var target: Int
    public var rounds: Int
    public var total: Int
    public var today: Int
    /// يوم [today] (yyyy-mm-dd): يعود عدد اليوم صفراً حين يتغير
    public var day: String

    public init(mode: String = "free", idx: Int = 0, step: Int = 0, count: Int = 0, target: Int = 33,
                rounds: Int = 0, total: Int = 0, today: Int = 0, day: String = "") {
        self.mode = mode; self.idx = idx; self.step = step; self.count = count; self.target = target
        self.rounds = rounds; self.total = total; self.today = today; self.day = day
    }

    private var salahStep: (Int, Int) { Tasbih.salah[min(max(step, 0), Tasbih.salah.count - 1)] }

    /// موضع الذكر في Tasbih.dhikr، أو -1 لتمام المائة
    public var dhikrIndex: Int { mode == "salah" ? salahStep.0 : min(max(idx, 0), Tasbih.dhikr.count - 1) }

    /// الذكر بالعربية كما يُعرض في كل اللغات
    public var text: String { let i = dhikrIndex; return i < 0 ? Tasbih.tamam : Tasbih.dhikr[i] }

    /// العدد المطلوب للذكر الحالي، و 0 بلا حد
    public var goal: Int { mode == "salah" ? salahStep.1 : target }

    /// معنى الذكر بلغة الواجهة: [meanings] معاني الأذكار (Strings.dhikrMeanings)، و[tamamMeaning] معنى تمام المائة
    public func meaning(meanings: [String], tamamMeaning: String) -> String {
        let i = dhikrIndex
        if i < 0 { return tamamMeaning }
        return i < meanings.count ? meanings[i] : ""
    }

    /// يُبدأ يوم جديد: عدد اليوم صفر
    public func on(_ day: String) -> TasbihState {
        if self.day == day { return self }
        var s = self; s.day = day; s.today = 0
        return s
    }

    /// تسبيحة واحدة: العدد والمجموع وعدد اليوم، ثم إتمام الهدف إن بلغه
    public func counted() -> (TasbihState, Tasbih.Event) {
        let g = goal
        var n = self
        n.count = count + 1; n.total = total + 1; n.today = today + 1
        if g <= 0 || n.count < g { return (n, .count) }
        if mode == "salah" {
            if step < Tasbih.salah.count - 1 { n.step = step + 1; n.count = 0; return (n, .nextDhikr) }
            n.step = 0; n.count = 0; n.rounds = rounds + 1
            return (n, .hundred)
        }
        n.count = 0; n.rounds = rounds + 1
        return (n, .round)
    }

    /// يعود العدد صفراً (وفي salah إلى أول خطوة)؛ الدورات والمجموع باقية
    public func reset() -> TasbihState { var s = self; s.count = 0; s.step = 0; return s }

    /// يتراجع تسبيحة واحدة في الذكر الحالي
    public func undo() -> TasbihState {
        if count <= 0 { return self }
        var s = self
        s.count = count - 1; s.total = max(total - 1, 0); s.today = max(today - 1, 0)
        return s
    }

    /// عدة تسبيحات دفعة واحدة؛ آخر حدث غير العدّ المجرد
    public func counted(times: Int) -> (TasbihState, Tasbih.Event) {
        var s = self
        var last = Tasbih.Event.count
        let n = min(max(times, 0), 100_000)
        var i = 0
        while i < n { let (x, e) = s.counted(); s = x; if e != .count { last = e }; i += 1 }
        return (s, last)
    }

    public func withMode(_ mode: String) -> TasbihState { var s = self; s.mode = mode == "salah" ? "salah" : "free"; s.count = 0; s.step = 0; return s }
    public func withDhikr(_ i: Int) -> TasbihState { var s = self; s.idx = min(max(i, 0), Tasbih.dhikr.count - 1); s.count = 0; return s }
    public func withTarget(_ t: Int) -> TasbihState { var s = self; s.target = Tasbih.targets.contains(t) ? t : 33; s.count = 0; return s }
}

/**
 المسبحة الواحدة في التطبيق: الشاشة وزر الإجراء ومركز التحكم والجزيرة الديناميكية والأداة، كلها تعدّ في الحال نفسها
 (المفاتيح ts_* في الإعدادات، كما يكتبها Kotlin)، وكل تغيير يصل إلى [onChange].
 */
public final class Tasbih {
    /// نصوص الأذكار بالعربية (تُعرض كما هي في كل اللغات، ومعناها تحتها)
    public static let dhikr = [
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "اللَّهُ أَكْبَرُ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "أَسْتَغْفِرُ اللَّهَ",
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        "سُبْحَانَ اللَّهِ الْعَظِيمِ",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ",
    ]

    /// تسبيح ما بعد الصلاة: (موضع الذكر، عدده) — 33 + 33 + 33 ثم تمام المائة (-1)
    public static let salah: [(Int, Int)] = [(0, 33), (1, 33), (2, 33), (-1, 1)]
    public static let tamam = "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ"
    public static let targets = [33, 34, 100, 1000, 0]

    /// ما حدث مع التسبيحة: عدّ فقط، أو انتقال إلى الذكر التالي (بعد الصلاة)، أو إتمام دورة، أو إتمام المائة
    public enum Event: String {
        case count = "COUNT", nextDhikr = "NEXT_DHIKR", round = "ROUND", hundred = "HUNDRED"
    }

    public static let shared = Tasbih()

    public let settings: AppSettings
    /// اليوم الآن (yyyy-mm-dd)؛ يُستبدل في الاختبارات
    public var today: () -> String
    /// كل تغيير (الشاشة والنشاط الحي والأداة)
    public var onChange: ((TasbihState) -> Void)?

    private var loaded = false
    private var current = TasbihState()
    private let lock = NSRecursiveLock()

    public init(settings: AppSettings = .shared, today: @escaping () -> String = { LocalDate.now().description }) {
        self.settings = settings
        self.today = today
    }

    /// الحال الآن (بعد قراءتها من المخزن وبدء يوم جديد إن لزم)
    public var state: TasbihState { lock.lock(); defer { lock.unlock() }; ensure(); return current }

    /// يقرأ الحال من المخزن أول مرة، ويبدأ يوماً جديداً إن تغير اليوم
    public func ensure() {
        lock.lock(); defer { lock.unlock() }
        if !loaded {
            loaded = true
            current = Tasbih.read(settings)
        }
        let d = today()
        if current.day != d { set(current.on(d), notify: false) }
    }

    /// يعيد قراءة المخزن (بعد أن كتب فيه Kotlin أو امتداد آخر)
    public func reload() {
        lock.lock(); defer { lock.unlock() }
        loaded = false
        ensure()
    }

    /// الحال كما في المخزن، بالتصحيح نفسه الذي يجريه Tasbih.ensure في Kotlin
    public static func read(_ s: AppSettings) -> TasbihState {
        TasbihState(
            mode: s.getString("ts_mode", "free") == "salah" ? "salah" : "free",
            idx: min(max(s.getInt("ts_idx", 0), 0), dhikr.count - 1),
            step: min(max(s.getInt("ts_step", 0), 0), salah.count - 1),
            count: max(s.getInt("ts_count", 0), 0),
            target: { let t = s.getInt("ts_target", 33); return targets.contains(t) ? t : 33 }(),
            rounds: max(s.getInt("ts_rounds", 0), 0),
            total: max(s.getInt("ts_total", 0), 0),
            today: max(s.getInt("ts_today", 0), 0),
            day: s.getString("ts_day", "")
        )
    }

    /// يكتب الحال في المخزن (ts_*)
    public static func write(_ st: TasbihState, to s: AppSettings) {
        s.putString("ts_mode", st.mode); s.putInt("ts_idx", st.idx); s.putInt("ts_step", st.step)
        s.putInt("ts_count", st.count); s.putInt("ts_target", st.target); s.putInt("ts_rounds", st.rounds)
        s.putInt("ts_total", st.total); s.putInt("ts_today", st.today); s.putString("ts_day", st.day)
    }

    private func set(_ s: TasbihState, notify: Bool = true) {
        current = s
        Tasbih.write(s, to: settings)
        if notify { onChange?(s) }
    }

    private func apply(_ f: (TasbihState) -> TasbihState) {
        lock.lock(); defer { lock.unlock() }
        ensure()
        set(f(current))
    }

    /// تسبيحة واحدة؛ يعيد ما حدث (لاهتزاز إتمام الدورة ورسالته)
    @discardableResult
    public func count() -> Event {
        lock.lock(); defer { lock.unlock() }
        ensure()
        let (n, e) = current.counted()
        set(n)
        return e
    }

    /// عدة تسبيحات دفعة واحدة (ما عُدّ في الأداة حين تعذّر تشغيل التطبيق)؛ يعيد آخر حدث غير العدّ المجرد
    @discardableResult
    public func count(_ times: Int) -> Event {
        lock.lock(); defer { lock.unlock() }
        ensure()
        let (n, e) = current.counted(times: times)
        set(n)
        return e
    }

    public func reset() { apply { $0.reset() } }
    public func undo() { apply { $0.undo() } }
    public func setMode(_ mode: String) { apply { $0.withMode(mode) } }
    public func setDhikr(_ i: Int) { apply { $0.withDhikr(i) } }
    public func setTarget(_ t: Int) { apply { $0.withTarget(t) } }
}
