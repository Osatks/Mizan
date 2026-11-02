import Foundation

/// لقطة الأدوات: يكتبها التطبيق (WidgetData.snapshot في Kotlin) في حاوية App Group، أو تبنيها الأداة وحدها
/// من موقعها (StandaloneSnapshot) حين لا تصلها لقطة التطبيق. الأوقات ثوانٍ منذ ١٩٧٠، والنصوص منسقة مسبقاً.
/// هذا الملف لا يستورد إلا Foundation حتى يُختبر على لينكس مع حساب الأداة المستقل.
struct Snapshot: Codable {
    var v: Int
    var at: Double
    var lang: String
    var rtl: Bool
    var theme: String
    var colors: Palette
    var place: String
    /// كل كم ثانية ينتقل الذكر المعروض
    var rotateSec: Int
    var labels: [String: String]
    var days: [Day]
    var windows: [AdhkarWindow]
    var items: [String: Dhikr]
    /// الختمة لأداة الورد (لقطة التطبيق فقط؛ الأداة المستقلة لا تعرف ما قرأه المستخدم)
    var khatma: Khatma? = nil
    /// ما تقرؤه الساعة وحدها: القبلة والإقامة ورمضان (لقطة التطبيق فقط؛ WidgetData.watch في Kotlin)
    var watch: WatchExtras? = nil
}

/// للساعة: القبلة من موقع المستخدم في التطبيق، ودقائق الإقامة بعد كل أذان، وأيام رمضان ونصوصها، واسم الشهر الهجري
struct WatchExtras: Codable {
    /// زاوية القبلة من الشمال الجغرافي بالدرجات (0 إلى 360)
    var qibla: Double
    /// «157°» بأرقام لغة التطبيق
    var qiblaText: String
    /// «الجنوب الشرقي»
    var qiblaDir: String
    /// «1,234 كم»
    var qiblaKm: String
    var qiblaTitle: String
    /// «أنت متجه نحو القبلة»
    var facing: String
    /// دقائق الإقامة بعد أذان كل صلاة (الصلوات التي لها إقامة فقط)
    var iqama: [String: Int]
    var iqamaIn: String
    var iftar: String
    var suhoor: String
    /// أيام رمضان في اللقطة (Day.date)
    var ramadan: [String]
    /// اسم الشهر الهجري لكل يوم (Day.date) بلا السنة
    var months: [String: String]
}

/// ورد اليوم وتقدّم الختمة (WidgetData.khatma في Kotlin)
struct Khatma: Codable {
    var read: Int
    var total: Int
    var percent: Int
    var today: Int
    /// صفحات ورد اليوم حسب مدة الختمة، و 0 بلا مدة
    var goal: Int
    var streak: Int
    var done: Bool
    /// موضع المتابعة في المصحف
    var route: String
    var title: String
    var khatma: String
    /// «3 من 20 صفحة»
    var pages: String
    var streakText: String
    var percentText: String
    var portionDone: Bool
    var doneText: String
    /// «تابع وردي»
    var cta: String
}

struct Palette: Codable {
    var hero1: String
    var hero2: String
    var night1: String
    var night2: String
    var accent: String
    var gold: String
}

struct Day: Codable {
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

struct PrayerTime: Codable {
    var k: String
    var name: String
    var at: Double
    var t: String
    var s: String

    var date: Date { Date(timeIntervalSince1970: at) }
}

struct Ayah: Codable {
    var text: String
    var ref: String
    var meaning: String?
    var route: String
}

struct AdhkarWindow: Codable {
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

struct Dhikr: Codable {
    var ar: String
    var n: Int
    var c: String?
    var src: String
    var tr: String?
    var fadl: String?
}

extension Snapshot {
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

    /// رقم الذكر المعروض: ينتقل كل [rotateSec] ثانية، ويزيد عليه ما تقدّم به المستخدم (مثل AdhkarClock.index)
    func index(at date: Date, in w: AdhkarWindow, offset: Int, count: Int) -> Int {
        guard count > 0 else { return 0 }
        let step = Double(max(rotateSec, 1))
        let steps = max(0, Int((date.timeIntervalSince1970 - w.s) / step))
        let i = (steps + offset) % count
        return i < 0 ? i + count : i
    }
}
