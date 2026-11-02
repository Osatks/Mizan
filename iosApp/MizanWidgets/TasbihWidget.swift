import WidgetKit
import SwiftUI
import AppIntents

// المسبحة على الشاشة الرئيسية وشاشة القفل، وفي مركز التحكم: لمسة تعدّ دون فتح التطبيق (iOS 17، ومركز التحكم في iOS 18).
// العدّ نية LiveActivityIntent تعمل في التطبيق (Shared/LiveIntents.swift) فتعدّ في الحال نفسها التي تعدّ فيها شاشة المسبحة،
// ثم تكتب الحال (IosTasbih في Kotlin) في حاوية App Group وتطلب إعادة رسم الأداة والزر.

/// حال المسبحة كما كتبها التطبيق: من حاوية App Group، وإلا من سلسلة المفاتيح المشتركة (تثبيت بلا App Group)
enum TasbihStore {
    static func load() -> TasbihLiveState? {
        var state: TasbihLiveState?
        if let url = Store.container()?.appendingPathComponent("tasbih.json") {
            state = LiveJSON.tasbihState(try? Data(contentsOf: url))
        }
        if state == nil { state = LiveJSON.tasbihState(SharedKeychain.read(account: SharedKeychain.tasbihAccount).0) }
        // تسبيحات عُدّت هنا احتياطاً ولم يضفها التطبيق بعد
        let pending = PendingTasbih.count
        if pending > 0, var s = state {
            s.count += pending
            s.countText = String(s.count)
            if s.goal > 0 { s.progress = min(1, Double(s.count) / Double(s.goal)) }
            state = s
        }
        return state
    }

    /// قبل أول فتح للمسبحة: «سبحان الله» من صفر بلغة الجهاز
    static var placeholder: TasbihLiveState {
        let ar = Fallback.arabic
        let p = Mizan.fallback
        return TasbihLiveState(
            id: "tasbih", text: "سُبْحَانَ اللَّهِ", meaning: ar ? "" : "Glory be to Allah", count: 0, goal: 33, progress: 0,
            countText: "0", goalText: ar ? "من 33" : "of 33", rounds: "0", roundsLabel: ar ? "دورة مكتملة" : "rounds",
            today: "0", todayLabel: ar ? "ذكر اليوم" : "today", mode: "", title: Fallback.text("tasbih"),
            tap: ar ? "اضغط للتسبيح" : "Tap to count", reset: ar ? "تصفير" : "Reset", end: ar ? "إنهاء" : "End",
            rtl: ar, hero1: p.hero1, hero2: p.hero2, gold: p.gold, at: nil
        )
    }
}

struct TasbihEntry: TimelineEntry {
    let date: Date
    let state: TasbihLiveState
    /// لقطة التطبيق للمظهر (فاتح أو داكن، ولون التطبيق)
    let snap: Snapshot?
}

/// مدخل واحد: يُعاد رسمه حين يعدّ المستخدم (LiveRouter.reloadTasbih)، وعند منتصف الليل يعود «ذكر اليوم» صفراً
struct TasbihProvider: TimelineProvider {
    func placeholder(in context: Context) -> TasbihEntry {
        TasbihEntry(date: Date(), state: TasbihStore.placeholder, snap: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (TasbihEntry) -> Void) {
        completion(TasbihEntry(date: Date(), state: TasbihStore.load() ?? TasbihStore.placeholder, snap: Store.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TasbihEntry>) -> Void) {
        let now = Date()
        let entry = TasbihEntry(date: now, state: TasbihStore.load() ?? TasbihStore.placeholder, snap: Store.load())
        let midnight = Calendar.current.startOfDay(for: now).addingTimeInterval(86_400 + 5)
        completion(Timeline(entries: [entry], policy: .after(midnight)))
    }
}

struct TasbihWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: LiveRouter.tasbihWidgetKind, provider: TasbihProvider()) { entry in
            TasbihWidgetView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("tasbih"))
        .description(Fallback.gallery("descTasbih"))
        .supportedFamilies(Families.make([.systemSmall, .systemMedium], circular: true, rectangular: true, inline: true))
    }
}

struct TasbihWidgetView: View {
    let entry: TasbihEntry
    @Environment(\.widgetFamily) var family

    var body: some View {
        let s = entry.state
        if family.isAccessory {
            if #available(iOS 16.0, *) {
                TasbihAccessory(state: s, family: family, snap: entry.snap)
            }
        } else {
            Shell(snap: entry.snap, url: Deep.url("tasbih")) {
                if family == .systemMedium {
                    HStack(spacing: 14) {
                        TasbihWidgetText(state: s, large: true)
                        TasbihWidgetRing(state: s, size: 118)
                    }
                } else {
                    VStack(spacing: 6) {
                        Header(symbol: nil, title: s.title)
                        TasbihWidgetRing(state: s, size: 92)
                            .frame(maxWidth: .infinity)
                        Text(verbatim: s.text)
                            .font(Mizan.quran(15))
                            .lineLimit(1)
                            .minimumScaleFactor(0.55)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }
}

/// الذكر ومعناه وعدد اليوم والدورات (الأداة المتوسطة)
struct TasbihWidgetText: View {
    let state: TasbihLiveState
    var large: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Header(symbol: nil, title: state.mode.isEmpty ? state.title : "\(state.title) · \(state.mode)")
            Spacer(minLength: 0)
            Text(verbatim: state.text)
                .font(Mizan.quran(state.text.count > 40 ? 16 : 21))
                .lineLimit(2)
                .minimumScaleFactor(0.6)
            if !state.meaning.isEmpty {
                Text(verbatim: state.meaning)
                    .font(Mizan.rounded(11))
                    .foregroundColor(Mizan.text2)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
            HStack(spacing: 10) {
                stat(state.today, state.todayLabel)
                stat(state.rounds, state.roundsLabel)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: value).font(Mizan.rounded(15, .bold)).monospacedDigit().foregroundColor(Mizan.gold)
            Text(verbatim: label).font(Mizan.rounded(9.5)).foregroundColor(Mizan.text3).lineLimit(1)
        }
    }
}

/// الحلقة وفيها العدد؛ لمسها يعدّ في iOS 17، وقبله يفتح المسبحة
struct TasbihWidgetRing: View {
    let state: TasbihLiveState
    let size: CGFloat

    var ring: some View {
        ProgressRing(progress: state.progress, color: Mizan.gold, width: size * 0.07) {
            CountLabel(count: state.countText, caption: state.goalText, size: size * 0.3, color: Mizan.text)
        }
        .frame(width: size, height: size)
        .background(Circle().fill(Color.white.opacity(0.08)).padding(size * 0.07))
    }

    var body: some View {
        if #available(iOS 17.0, *) {
            Button(intent: TasbihCountIntent()) { ring }.buttonStyle(.plain)
        } else {
            ring
        }
    }
}

/// شاشة القفل: حلقة العدد (لمسها يعدّ)، أو الذكر وعدده، أو سطر واحد فوق الساعة
@available(iOS 16.0, *)
struct TasbihAccessory: View {
    let state: TasbihLiveState
    let family: WidgetFamily
    let snap: Snapshot?

    var body: some View {
        switch family {
        case .accessoryCircular:
            AccessoryShell(snap: snap, url: Deep.url("tasbih"), circular: true) {
                count {
                    Gauge(value: max(0, min(state.progress, 1))) {
                        Image(systemName: "hand.tap.fill")
                    } currentValueLabel: {
                        Text(verbatim: state.countText).monospacedDigit()
                    }
                    .gaugeStyle(.accessoryCircularCapacity)
                }
            }
        case .accessoryInline:
            AccessoryShell(snap: snap, url: Deep.url("tasbih")) {
                Text(verbatim: "\(state.countText) · \(state.title)")
            }
        default:
            AccessoryShell(snap: snap, url: Deep.url("tasbih")) {
                count {
                    HStack(spacing: 8) {
                        Gauge(value: max(0, min(state.progress, 1))) {
                            EmptyView()
                        } currentValueLabel: {
                            Text(verbatim: state.countText).monospacedDigit()
                        }
                        .gaugeStyle(.accessoryCircularCapacity)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(verbatim: state.title).font(Mizan.rounded(12, .semibold)).widgetAccentable().lineLimit(1)
                            Text(verbatim: state.text).font(.system(size: 14, weight: .medium)).lineLimit(1).minimumScaleFactor(0.6)
                            Text(verbatim: state.goalText).font(Mizan.rounded(11)).lineLimit(1)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    /// لمس الأداة يعدّ (iOS 17)، وقبله يفتح المسبحة
    @ViewBuilder
    private func count<C: View>(@ViewBuilder _ content: () -> C) -> some View {
        if #available(iOS 17.0, *) {
            Button(intent: TasbihCountIntent()) { content() }.buttonStyle(.plain)
        } else {
            content()
        }
    }
}

// ---------- مركز التحكم ----------

/// العدد الحالي لزر مركز التحكم
struct TasbihControlValue {
    var count: String
    var goal: String
}

@available(iOS 18.0, *)
struct TasbihControlProvider: ControlValueProvider {
    var previewValue: TasbihControlValue { TasbihControlValue(count: "0", goal: TasbihStore.placeholder.goalText) }

    func currentValue() async throws -> TasbihControlValue {
        let s = TasbihStore.load() ?? TasbihStore.placeholder
        return TasbihControlValue(count: s.countText, goal: s.goalText)
    }
}
