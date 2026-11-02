import SwiftUI
import WidgetKit

// مضاعفات وجه الساعة: الصلاة القادمة (عدّ تنازلي حي وشريط ما مضى من الوقت)، واختصار يفتح المسبحة.
// من لقطة iPhone التي حفظها تطبيق الساعة في حاوية App Group (WatchData.swift)؛ مدخل عند دخول كل صلاة.

@main
struct MizanWatchWidgets: WidgetBundle {
    var body: some Widget {
        NextPrayerComplication()
        TasbihComplication()
    }
}

struct PrayerEntry: TimelineEntry {
    let date: Date
    let snap: Snapshot?
}

struct PrayerProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snap: nil) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snap: WatchData.load()?.snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        let snap = WatchData.load()?.snapshot
        var dates = [now]
        if let snap = snap {
            let t0 = now.timeIntervalSince1970
            dates += snap.prayers.map { $0.date }.filter { $0.timeIntervalSince1970 > t0 && $0.timeIntervalSince1970 < t0 + 36 * 3600 }
        }
        let entries = dates.map { PrayerEntry(date: $0, snap: snap) }
        // بعد آخر صلاة في اللقطة يُعاد البناء (تصل لقطة جديدة كلما فُتح التطبيق على iPhone)
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct NextPrayerComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchNextPrayer", provider: PrayerProvider()) { entry in
            NextPrayerComplicationView(entry: entry)
        }
        .configurationDisplayName("Mizan")
        .description("Next prayer")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

struct NextPrayerComplicationView: View {
    let entry: PrayerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        content
            .containerBackground(for: .widget) {
                if family == .accessoryCircular { AccessoryWidgetBackground() } else { Color.clear }
            }
            .widgetURL(URL(string: "mizan-watch://prayer"))
    }

    @ViewBuilder
    private var content: some View {
        if let snap = entry.snap, let next = snap.nextPrayer(after: entry.date) {
            let from = snap.previousPrayer(before: entry.date)?.date ?? next.date.addingTimeInterval(-3 * 3600)
            let span = max(1, next.date.timeIntervalSince(from))
            let done = min(1, max(0, entry.date.timeIntervalSince(from) / span))
            switch family {
            case .accessoryCircular:
                Gauge(value: done) {
                    Image(systemName: "moon.stars.fill")
                } currentValueLabel: {
                    VStack(spacing: 0) {
                        Text(verbatim: next.name).font(.system(size: 10, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                        Text(verbatim: next.s).font(.system(size: 12, weight: .bold)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.6)
                    }
                }
                .gaugeStyle(.accessoryCircularCapacity)
                .widgetAccentable()
            case .accessoryInline:
                Text(verbatim: "\(next.name) \(next.t)")
            case .accessoryCorner:
                Text(timerInterval: entry.date...max(entry.date, next.date), countsDown: true)
                    .monospacedDigit()
                    .widgetLabel {
                        Gauge(value: done) { Text(verbatim: next.name) }
                            .tint(Color(red: 0.91, green: 0.76, blue: 0.35))
                    }
            default:
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 4) {
                        Image(systemName: "moon.stars.fill").font(.system(size: 11))
                        Text(verbatim: snap.text("nextPrayer", "Next prayer")).font(.system(size: 12, weight: .semibold)).lineLimit(1)
                    }
                    .widgetAccentable()
                    HStack(alignment: .firstTextBaseline) {
                        Text(verbatim: next.name).font(.system(size: 17, weight: .bold)).lineLimit(1).minimumScaleFactor(0.7)
                        Spacer(minLength: 4)
                        Text(verbatim: next.t).font(.system(size: 13, weight: .semibold)).monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    Text(timerInterval: entry.date...max(entry.date, next.date), countsDown: true)
                        .font(.system(size: 15, weight: .medium))
                        .monospacedDigit()
                        .environment(\.layoutDirection, .leftToRight)
                        .frame(maxWidth: .infinity, alignment: snap.rtl ? .trailing : .leading)
                }
                .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
            }
        } else {
            switch family {
            case .accessoryInline: Text(verbatim: "Mizan")
            default:
                Image(systemName: "moon.stars.fill").font(.system(size: 20)).widgetAccentable()
            }
        }
    }
}

/// اختصار المسبحة على وجه الساعة: يفتح صفحة المسبحة
struct TasbihComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MizanWatchTasbih", provider: TasbihProvider()) { _ in
            TasbihComplicationView()
        }
        .configurationDisplayName("Mizan")
        .description("Tasbih")
        .supportedFamilies([.accessoryCircular, .accessoryCorner])
    }
}

struct TasbihProvider: TimelineProvider {
    struct Entry: TimelineEntry { let date: Date }
    func placeholder(in context: Context) -> Entry { Entry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (Entry) -> Void) { completion(Entry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> Void) {
        completion(Timeline(entries: [Entry(date: Date())], policy: .never))
    }
}

struct TasbihComplicationView: View {
    @Environment(\.widgetFamily) private var family

    var body: some View {
        Group {
            if family == .accessoryCorner {
                Image(systemName: "hand.tap.fill").font(.system(size: 18)).widgetLabel { Text(verbatim: "سبحان الله") }
            } else {
                ZStack {
                    AccessoryWidgetBackground()
                    Image(systemName: "hand.tap.fill").font(.system(size: 20, weight: .semibold))
                }
            }
        }
        .widgetAccentable()
        .containerBackground(for: .widget) { Color.clear }
        .widgetURL(URL(string: "mizan-watch://tasbih"))
    }
}
