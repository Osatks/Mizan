import SwiftUI
import WidgetKit

// مضاعفات وجه الساعة والمكدس الذكي، من لقطة iPhone التي حفظها تطبيق الساعة في حاوية App Group (WatchData.swift):
// الصلاة القادمة (والإقامة بعد الأذان، والإفطار والسحور في رمضان)، ومواقيت اليوم، والأذكار حسب الوقت، واتجاه القبلة،
// والتاريخ الهجري، والمسبحة. الرسم في ComplicationViews.swift. أهمية المدخلات (relevance) تقدّم في المكدس الذكي
// الصلاةَ القادمة قبل الأذان بنصف ساعة إلى الإقامة، وأذكارَ الصباح والمساء في وقتها، والمسبحةَ بعد الصلاة.

@main
struct MizanWatchWidgets: WidgetBundle {
    var body: some Widget {
        NextPrayerComplication()
        TodayPrayersComplication()
        AdhkarComplication()
        QiblaComplication()
        HijriComplication()
        TasbihComplication()
    }
}

struct PrayerEntry: TimelineEntry {
    let date: Date
    let snap: Snapshot?
    var relevance: TimelineEntryRelevance? = nil
}

/// خلفية المضاعفة (دائرة الوجه للصغيرة) ورابط الصفحة التي تفتحها في تطبيق الساعة، والحجم من البيئة
struct FaceContainer<Content: View>: View {
    let page: String
    @ViewBuilder let content: (WidgetFamily) -> Content
    @Environment(\.widgetFamily) private var family

    var body: some View {
        content(family)
            .containerBackground(for: .widget) {
                if family == .accessoryCircular { AccessoryWidgetBackground() } else { Color.clear }
            }
            .widgetURL(URL(string: "mizan-watch://\(page)"))
    }
}

// ---------- المزوّدات ----------

/// مدخل عند كل أذان وكل إقامة (يتغير ما يُعدّ إليه)، ومعها قبل كل أذان بنصف ساعة إن طُلبت الأهمية، وكل عشر دقائق
/// إن طُلب ([steps]: حلقة ما مضى من الوقت في المضاعفة الدائرية والزاوية ترسم عند كل مدخل)
struct PrayerProvider: TimelineProvider {
    var relevant = false
    var steps = false

    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snap: nil) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        guard let snap = WatchData.load()?.snapshot else {
            // تصل اللقطة فيعيد تطبيق الساعة بناء المضاعفات (reloadAllTimelines)؛ هذا احتياط
            completion(Timeline(entries: [PrayerEntry(date: now, snap: nil)], policy: .after(now.addingTimeInterval(3600))))
            return
        }
        var dates = [now] + snap.changes(after: now)
        if relevant {
            let end = now.addingTimeInterval(36 * 3600)
            dates += snap.prayers.map { $0.date.addingTimeInterval(-30 * 60) }.filter { $0 > now && $0 < end }
        }
        if steps {
            // كل عشر دقائق في اليوم القادم، على رأس الدقائق العشر
            let start = (now.timeIntervalSince1970 / 600).rounded(.up) * 600
            dates += stride(from: start, to: now.timeIntervalSince1970 + 24 * 3600, by: 600).map { Date(timeIntervalSince1970: $0) }
        }
        let entries = Array(Set(dates)).sorted().map { d in
            PrayerEntry(date: d, snap: snap, relevance: relevant ? Self.relevance(snap, d) : nil)
        }
        // بعد آخر مدخل يُعاد البناء (تصل لقطة جديدة كلما فُتح التطبيق على iPhone)
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    /// مهمة من نصف ساعة قبل الأذان إلى الإقامة (أو ربع ساعة بعد الأذان على الأقل)، وقليلة في غير ذلك
    static func relevance(_ snap: Snapshot, _ date: Date) -> TimelineEntryRelevance {
        let t = date.timeIntervalSince1970
        for p in snap.prayers {
            let end = p.at + Double(max(snap.watch?.iqama[p.k] ?? 0, 15)) * 60
            if t >= p.at - 30 * 60 && t < end {
                return TimelineEntryRelevance(score: 100, duration: end - t)
            }
        }
        return TimelineEntryRelevance(score: 5)
    }
}

/// مدخل عند بداية كل يوم في اللقطة (التاريخ الهجري)
struct DayProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snap: nil) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        guard let snap = WatchData.load()?.snapshot else {
            completion(Timeline(entries: [PrayerEntry(date: now, snap: nil)], policy: .after(now.addingTimeInterval(3600))))
            return
        }
        let starts = snap.days.map { Date(timeIntervalSince1970: $0.start) }.filter { $0 > now }
        completion(Timeline(entries: ([now] + starts).map { PrayerEntry(date: $0, snap: snap) }, policy: .atEnd))
    }
}

/// القبلة لا تتغير إلا بتغيّر الموقع: مدخل واحد، ويُعاد البناء مع كل لقطة جديدة
struct StaticProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snap: nil) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        completion(Timeline(entries: [PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot)], policy: .never))
    }
}

struct TasbihEntry: TimelineEntry {
    let date: Date
    let title: String
    var relevance: TimelineEntryRelevance? = nil
}

/// مدخل عند بداية كل نافذة أذكار في اليوم القادم، وكل ربع ساعة فيها (ينتقل الذكر المعروض)؛ وأهميته في المكدس
/// الذكي من نوع النافذة (الصباح والمساء أولاً)
struct AdhkarProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snap: nil) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        guard let snap = WatchData.load()?.snapshot, !snap.windows.isEmpty else {
            completion(Timeline(entries: [PrayerEntry(date: now, snap: WatchData.load()?.snapshot)], policy: .after(now.addingTimeInterval(3600))))
            return
        }
        let t0 = now.timeIntervalSince1970
        let end = t0 + 24 * 3600
        var entries: [PrayerEntry] = []
        for w in snap.windows where w.e > t0 && w.s < end {
            let score = AdhkarClockWatch.relevance(w.k)
            var t = max(w.s, t0)
            while t < min(w.e, end) {
                entries.append(PrayerEntry(date: Date(timeIntervalSince1970: t), snap: snap,
                                           relevance: TimelineEntryRelevance(score: score, duration: w.e - t)))
                // على حدود ربع الساعة من بداية النافذة
                t = w.s + ((t - w.s) / AdhkarClockWatch.rotate + 1).rounded(.down) * AdhkarClockWatch.rotate
            }
        }
        if entries.isEmpty { entries = [PrayerEntry(date: now, snap: snap)] }
        entries.sort { $0.date < $1.date }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

/// المسبحة مهمة بعد كل صلاة (التسبيح دبرها): من الإقامة وخمس دقائق، عشرين دقيقة
struct TasbihProvider: TimelineProvider {
    private static func title(_ p: WatchPayload?) -> String {
        p?.tasbih?.title ?? (WatchData.arabicDevice ? "المسبحة" : "Tasbih")
    }

    func placeholder(in context: Context) -> TasbihEntry { TasbihEntry(date: Date(), title: Self.title(nil)) }

    func getSnapshot(in context: Context, completion: @escaping (TasbihEntry) -> Void) {
        completion(TasbihEntry(date: Date(), title: Self.title(WatchData.load())))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TasbihEntry>) -> Void) {
        let now = Date()
        let payload = WatchData.load()
        let title = Self.title(payload)
        var entries = [TasbihEntry(date: now, title: title, relevance: TimelineEntryRelevance(score: 0))]
        if let snap = payload?.snapshot {
            let t0 = now.timeIntervalSince1970
            for p in snap.prayers where p.at > t0 - 3600 && p.at < t0 + 36 * 3600 {
                let start = p.at + Double((snap.watch?.iqama[p.k] ?? 10) + 5) * 60
                guard start > t0 else {
                    // الآن بعد الصلاة: مهمة لما بقي من الدقائق العشرين
                    if t0 < start + 20 * 60 { entries[0].relevance = TimelineEntryRelevance(score: 60, duration: start + 20 * 60 - t0) }
                    continue
                }
                entries.append(TasbihEntry(date: Date(timeIntervalSince1970: start), title: title,
                                           relevance: TimelineEntryRelevance(score: 60, duration: 20 * 60)))
                entries.append(TasbihEntry(date: Date(timeIntervalSince1970: start + 20 * 60), title: title,
                                           relevance: TimelineEntryRelevance(score: 0)))
            }
        }
        entries.sort { $0.date < $1.date }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

// ---------- المضاعفات ----------

struct NextPrayerComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchNextPrayer", provider: PrayerProvider(relevant: true, steps: true)) { entry in
            FaceContainer(page: "prayer") { family in NextPrayerFace(snap: entry.snap, date: entry.date, family: family) }
        }
        .configurationDisplayName("Mizan")
        .description("Next prayer, iqama, iftar and suhoor")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct TodayPrayersComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchToday", provider: PrayerProvider()) { entry in
            FaceContainer(page: "prayer") { _ in TodayPrayersFace(snap: entry.snap, date: entry.date) }
        }
        .configurationDisplayName("Mizan")
        .description("Today's prayer times")
        .supportedFamilies([.accessoryRectangular])
    }
}

struct AdhkarComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchAdhkar", provider: AdhkarProvider()) { entry in
            FaceContainer(page: "adhkar") { family in AdhkarFace(snap: entry.snap, date: entry.date, family: family) }
        }
        .configurationDisplayName("Mizan")
        .description("Adhkar for this time of day")
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline, .accessoryCorner])
    }
}

struct QiblaComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchQibla", provider: StaticProvider()) { entry in
            FaceContainer(page: "qibla") { family in QiblaFace(snap: entry.snap, family: family) }
        }
        .configurationDisplayName("Mizan")
        .description("Qibla direction")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct HijriComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchHijri", provider: DayProvider()) { entry in
            FaceContainer(page: "prayer") { family in HijriFace(snap: entry.snap, date: entry.date, family: family) }
        }
        .configurationDisplayName("Mizan")
        .description("Hijri date")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

/// المسبحة على وجه الساعة: تفتح صفحة المسبحة
struct TasbihComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchTasbih", provider: TasbihProvider()) { entry in
            FaceContainer(page: "tasbih") { family in TasbihFace(title: entry.title, family: family) }
        }
        .configurationDisplayName("Mizan")
        .description("Tasbih")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryCorner])
    }
}
