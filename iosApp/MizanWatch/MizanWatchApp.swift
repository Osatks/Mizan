import SwiftUI
import WatchKit
import WatchConnectivity
import WidgetKit

// ميزان على Apple Watch: الصلاة القادمة بعدّ تنازلي حي (وإلى الإقامة بعد الأذان، والإفطار والسحور في رمضان) ومواقيت
// اليوم، والمسبحة بالتاج الرقمي أو باللمس، واتجاه القبلة ببوصلة الساعة، ومضاعفات وجه الساعة (MizanWatchWidgets).
// البيانات من iPhone (WatchSync.swift) بلغة التطبيق وإعداداته.

@main
struct MizanWatchApp: App {
    @StateObject private var model = WatchModel()
    /// الصفحة الأولى: المواقيت (0) أو الأذكار (3) أو المسبحة (1) أو القبلة (2)؛ وسيط التشغيل -watch.page لالتقاط الصور
    /// في المحاكي، و 10 فما بعد معرض المضاعفات (ComplicationGallery) للصور وحدها
    @State private var page = UserDefaults.standard.integer(forKey: "watch.page")

    var body: some Scene {
        WindowGroup {
            if page >= 10 {
                ComplicationGallery(payload: model.payload, sheet: page)
            } else {
                TabView(selection: $page) {
                    PrayerPage(model: model).tag(0)
                    AdhkarPage(model: model).tag(3)
                    TasbihPage(labels: model.payload?.tasbih, rtl: model.rtl).tag(1)
                    QiblaPage(model: model, active: page == 2).tag(2)
                }
                .tabViewStyle(.verticalPage)
                .environment(\.layoutDirection, model.rtl ? .rightToLeft : .leftToRight)
                // من المضاعفات: mizan-watch://prayer و adhkar و tasbih و qibla
                .onOpenURL { url in
                    switch url.host {
                    case "adhkar": page = 3
                    case "tasbih": page = 1
                    case "qibla": page = 2
                    default: page = 0
                    }
                }
            }
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
        // مضغوطة (WatchSync)، أو غير مضغوطة من نسخة سابقة
        let data: Data?
        if let z = context["payloadz"] as? Data {
            data = try? (z as NSData).decompressed(using: .zlib) as Data
        } else {
            data = context["payload"] as? Data
        }
        guard let data = data, let p = try? JSONDecoder().decode(WatchPayload.self, from: data) else { return }
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
            if let snap = model.payload?.snapshot, let phase = snap.phase(at: context.date) {
                PrayerContent(snap: snap, phase: phase, now: context.date)
            } else {
                NoData()
            }
        }
        .containerBackground(WatchColors.background(model.payload?.snapshot), for: .tabView)
    }
}

struct PrayerContent: View {
    let snap: Snapshot
    /// ما يُعدّ إليه: الصلاة القادمة، أو الإقامة بعد الأذان، أو الإفطار والسحور في رمضان
    let phase: PrayerPhase
    let now: Date

    var body: some View {
        let gold = WatchColors.gold
        let next = phase.prayer
        ScrollView {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 4) {
                    phase.image.font(.system(size: 11, weight: .semibold))
                    Text(verbatim: phase.label).font(.system(size: 13, weight: .semibold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .foregroundStyle(gold)
                // اسم الصلاة ووقتها متجاوران
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(verbatim: next.name)
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .layoutPriority(1)
                    // بعد الأذان مضى وقته: العدّ إلى الإقامة وحده
                    if phase.kind != .iqama {
                        Text(verbatim: next.t)
                            .font(.system(size: 15, weight: .medium, design: .rounded))
                            .foregroundStyle(.white.opacity(0.65))
                            .monospacedDigit()
                            .lineLimit(1)
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    Spacer(minLength: 0)
                }
                // العدّ في بداية السطر كاسم الصلاة فوقه (يمين الشاشة بالعربية): أرقامه من اليسار إلى اليمين، والنص يملأ
                // عرضه فيُحاذى داخله، والإطار حوله في اتجاه اللغة
                Text(timerInterval: now...max(now, phase.to), countsDown: true)
                    .font(.system(size: 30, weight: .light, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(gold)
                    .multilineTextAlignment(snap.rtl ? .trailing : .leading)
                    .environment(\.layoutDirection, .leftToRight)
                    .frame(maxWidth: .infinity, alignment: .leading)
                // الصفحة تُرسم كل ثلاثين ثانية (TimelineView): شريط رفيع بما مضى
                TimeBar(done: phase.done(at: now), height: 6)
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
                        .padding(.vertical, 3)
                        .padding(.horizontal, 7)
                        // التالية في شريحة ذهبية خفيفة
                        .background {
                            if isNext { RoundedRectangle(cornerRadius: 8, style: .continuous).fill(gold.opacity(0.16)) }
                        }
                    }
                }
            }
            .padding(.horizontal, 4)
            // مؤشر الصفحات والتمرير عند التاج (يمين الشاشة): لا يغطي أول السطر بالعربية ولا آخره بغيرها
            .padding(snap.rtl ? .leading : .trailing, 6)
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
