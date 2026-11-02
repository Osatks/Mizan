import SwiftUI
import WatchKit
import WatchConnectivity
import WidgetKit

// ميزان على Apple Watch: الصلاة القادمة بعدّ تنازلي حي ومواقيت اليوم، والمسبحة بالتاج الرقمي أو باللمس،
// ومضاعفات وجه الساعة (MizanWatchWidgets). البيانات من iPhone (WatchSync.swift) بلغة التطبيق وإعداداته.

@main
struct MizanWatchApp: App {
    @StateObject private var model = WatchModel()
    @State private var page = 0

    var body: some Scene {
        WindowGroup {
            TabView(selection: $page) {
                PrayerPage(model: model).tag(0)
                TasbihPage(labels: model.payload?.tasbih, rtl: model.rtl).tag(1)
            }
            .tabViewStyle(.verticalPage)
            .environment(\.layoutDirection, model.rtl ? .rightToLeft : .leftToRight)
            // من مضاعفة المسبحة: mizan-watch://tasbih
            .onOpenURL { url in page = url.host == "tasbih" ? 1 : 0 }
        }
    }
}

/// اللقطة من iPhone: آخر ما حُفظ، وكل ما يصل بعده (updateApplicationContext)
final class WatchModel: NSObject, ObservableObject, WCSessionDelegate {
    @Published var payload: WatchPayload? = WatchData.load()

    var rtl: Bool { payload?.snapshot.rtl ?? WatchData.arabicDevice }

    override init() {
        super.init()
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let context = session.receivedApplicationContext
        if !context.isEmpty {
            apply(context)
        } else if session.isReachable {
            // أول تشغيل: يطلب من iPhone لقطة الآن
            session.sendMessage(["want": "snapshot"], replyHandler: nil, errorHandler: nil)
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        apply(context)
    }

    private func apply(_ context: [String: Any]) {
        guard let data = context["payload"] as? Data, let p = try? JSONDecoder().decode(WatchPayload.self, from: data) else { return }
        WatchData.save(data)
        WidgetCenter.shared.reloadAllTimelines()
        DispatchQueue.main.async { self.payload = p }
    }
}

// ---------- الصلاة القادمة ----------

struct PrayerPage: View {
    @ObservedObject var model: WatchModel

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { context in
            if let snap = model.payload?.snapshot, let next = snap.nextPrayer(after: context.date) {
                PrayerContent(snap: snap, next: next, now: context.date)
            } else {
                NoData()
            }
        }
        .containerBackground(WatchColors.background(model.payload?.snapshot), for: .tabView)
    }
}

struct PrayerContent: View {
    let snap: Snapshot
    let next: PrayerTime
    let now: Date

    var body: some View {
        let gold = WatchColors.gold
        let previous = snap.previousPrayer(before: now)
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 4) {
                    Image(systemName: "moon.stars.fill").font(.system(size: 11, weight: .semibold))
                    Text(verbatim: snap.text("nextPrayer", "Next prayer")).font(.system(size: 13, weight: .semibold, design: .rounded))
                }
                .foregroundStyle(gold)
                HStack(alignment: .firstTextBaseline) {
                    Text(verbatim: next.name)
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Spacer(minLength: 4)
                    Text(verbatim: next.t)
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white.opacity(0.8))
                        .environment(\.layoutDirection, .leftToRight)
                }
                Text(timerInterval: now...max(now, next.date), countsDown: true)
                    .font(.system(size: 30, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(gold)
                    .environment(\.layoutDirection, .leftToRight)
                    .frame(maxWidth: .infinity, alignment: snap.rtl ? .trailing : .leading)
                if let from = previous {
                    ProgressView(timerInterval: from.date...max(from.date.addingTimeInterval(1), next.date), countsDown: false) {
                        EmptyView()
                    } currentValueLabel: {
                        EmptyView()
                    }
                    .tint(gold)
                }
                if let day = snap.day(at: now) {
                    Text(verbatim: "\(snap.place) · \(day.hijri)")
                        .font(.system(size: 11, design: .rounded))
                        .foregroundStyle(.white.opacity(0.7))
                        .lineLimit(2)
                    Divider().padding(.vertical, 2)
                    ForEach(day.times, id: \.k) { t in
                        let isNext = t.k == next.k && t.at == next.at
                        HStack {
                            Text(verbatim: t.name)
                            Spacer()
                            Text(verbatim: t.t).monospacedDigit().environment(\.layoutDirection, .leftToRight)
                        }
                        .font(.system(size: 14, weight: isNext ? .bold : .regular, design: .rounded))
                        .foregroundStyle(isNext ? gold : (t.at < now.timeIntervalSince1970 ? .white.opacity(0.5) : .white))
                        .padding(.vertical, 1)
                    }
                }
            }
            .padding(.horizontal, 4)
        }
    }
}

struct NoData: View {
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "iphone.and.arrow.forward").font(.system(size: 28)).foregroundStyle(WatchColors.gold)
            Text(verbatim: WatchData.arabicDevice ? "افتح ميزان على iPhone لتصل المواقيت إلى الساعة" : "Open Mizan on your iPhone to bring prayer times to your watch")
                .font(.system(size: 14, design: .rounded))
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

// ---------- الألوان ----------

enum WatchColors {
    static let gold = Color(red: 0.91, green: 0.76, blue: 0.35)

    static func background(_ snap: Snapshot?) -> some ShapeStyle {
        let p = snap?.colors
        return LinearGradient(
            colors: [Color(watchHex: p?.hero1 ?? "#0F6A55"), Color(watchHex: p?.night2 ?? "#0B1311")],
            startPoint: .top, endPoint: .bottom
        )
    }
}

extension Color {
    init(watchHex hex: String) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        var v: UInt64 = 0
        _ = Scanner(string: s).scanHexInt64(&v)
        self.init(.sRGB, red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255, opacity: 1)
    }
}
