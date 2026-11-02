import SwiftUI
import WatchKit

// الأذكار حسب الوقت في الساعة: أذكار النافذة الحالية (الصباح، المساء، بعد الصلاة، النوم…) من لقطة iPhone، ذكراً بعد
// ذكر. اللمس على الحلقة يعدّ (نقرة اهتزاز خفيفة)، وعند تمام العدد يهتز اهتزاز النجاح وينتقل إلى التالي، وبعد آخرها
// «أتممت». التاج يمرّر نص الذكر الطويل. يُحفظ الموضع في الساعة لكل نافذة، ويبدأ من أولها مع كل نافذة جديدة.

struct AdhkarPage: View {
    @ObservedObject var model: WatchModel

    /// النافذة التي يُحفظ موضعها (مفتاحها ووقت بدايتها)
    @AppStorage("adhkar.window") private var windowID = ""
    @AppStorage("adhkar.index") private var index = 0
    @AppStorage("adhkar.count") private var count = 0
    @State private var pulse = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 60)) { context in
            if let snap = model.payload?.snapshot, let w = snap.window(at: context.date) {
                content(snap, w)
            } else {
                NoData()
            }
        }
        .containerBackground(WatchColors.background(model.payload?.snapshot), for: .tabView)
        .navigationTitle(model.payload?.snapshot.text("adhkar", "Adhkar") ?? (model.rtl ? "الأذكار" : "Adhkar"))
    }

    private static func id(_ w: AdhkarWindow) -> String { "\(w.key)@\(Int(w.s))" }

    @ViewBuilder
    private func content(_ snap: Snapshot, _ w: AdhkarWindow) -> some View {
        let items = snap.dhikrs(w)
        // نافذة جديدة: من أولها (يُحفظ عند أول لمسة)
        let fresh = windowID != Self.id(w)
        let i = fresh ? 0 : index
        let c = fresh ? 0 : count
        let gold = WatchColors.gold
        VStack(spacing: 6) {
            HStack(spacing: 4) {
                faceImage(AdhkarClockWatch.symbol(w.k)).font(.system(size: 11, weight: .semibold))
                Text(verbatim: w.chip.map { "\(w.title) · \($0)" } ?? w.title)
                    .font(.system(size: 13, weight: .semibold, design: .rounded))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Spacer(minLength: 0)
            }
            .foregroundStyle(gold)
            if i >= items.count || items.isEmpty {
                finished(snap, w)
            } else {
                let d = items[i]
                ScrollView {
                    VStack(spacing: 4) {
                        Text(verbatim: d.ar)
                            .font(.system(size: 16, weight: .medium))
                            .multilineTextAlignment(.center)
                            .lineSpacing(3)
                            .environment(\.layoutDirection, .rightToLeft)
                            .frame(maxWidth: .infinity)
                        if !d.src.isEmpty {
                            Text(verbatim: d.src)
                                .font(.system(size: 10, design: .rounded))
                                .foregroundStyle(.white.opacity(0.55))
                                .multilineTextAlignment(.center)
                        }
                    }
                }
                .frame(maxHeight: .infinity)
                HStack(spacing: 8) {
                    Text(verbatim: "\(i + 1)/\(items.count)")
                        .font(.system(size: 12, weight: .medium, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.7))
                        .environment(\.layoutDirection, .leftToRight)
                        .frame(width: 44)
                    Spacer(minLength: 0)
                    counter(d, c, gold)
                    Spacer(minLength: 0)
                    Button {
                        advance(w, from: i)
                        WKInterfaceDevice.current().play(.click)
                    } label: {
                        // «إلى الأمام» يُعكس وحده من اليمين إلى اليسار
                        Image(systemName: "chevron.forward.2")
                            .font(.system(size: 13, weight: .semibold))
                            .frame(width: 44, height: 32)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(gold)
                    .accessibilityLabel(snap.watch?.next ?? "Next")
                }
            }
        }
        .padding(.horizontal, 4)
    }

    /// حلقة العدد: تمتلئ مع كل لمسة، وفي وسطها ما بقي من مرات الذكر
    private func counter(_ d: Dhikr, _ c: Int, _ gold: Color) -> some View {
        let n = max(d.n, 1)
        return ZStack {
            Circle().stroke(Color.white.opacity(0.18), lineWidth: 4)
            Circle()
                .trim(from: 0, to: CGFloat(max(0.001, Double(c) / Double(n))))
                .stroke(gold, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.18), value: c)
            Text(verbatim: n > 1 ? "\(n - c)" : "✓")
                .font(.system(size: n > 1 ? 17 : 18, weight: .bold, design: .rounded))
                .monospacedDigit()
                .contentTransition(.numericText())
                .scaleEffect(pulse ? 1.1 : 1)
        }
        // من الأعلى مع عقارب الساعة في كل اللغات
        .environment(\.layoutDirection, .leftToRight)
        .frame(width: 46, height: 46)
        .contentShape(Circle())
        .onTapGesture { tap(d) }
    }

    private func finished(_ snap: Snapshot, _ w: AdhkarWindow) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 34, weight: .semibold))
                .foregroundStyle(WatchColors.gold)
            Text(verbatim: (snap.watch?.adhkarDone ?? "%s ✓").replacingOccurrences(of: "%s", with: w.title))
                .font(.system(size: 15, weight: .semibold, design: .rounded))
                .multilineTextAlignment(.center)
            Button(snap.watch?.again ?? "Start again") {
                windowID = Self.id(w)
                index = 0
                count = 0
                WKInterfaceDevice.current().play(.click)
            }
            .font(.system(size: 13, weight: .semibold, design: .rounded))
            .tint(WatchColors.gold)
        }
        .frame(maxHeight: .infinity)
    }

    private func tap(_ d: Dhikr) {
        guard let snap = model.payload?.snapshot, let w = snap.window(at: Date()) else { return }
        if windowID != Self.id(w) {
            windowID = Self.id(w)
            index = 0
            count = 0
        }
        count += 1
        withAnimation(.spring(response: 0.2, dampingFraction: 0.5)) { pulse = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { withAnimation(.easeOut(duration: 0.15)) { pulse = false } }
        if count >= max(d.n, 1) {
            WKInterfaceDevice.current().play(.success)
            advance(w, from: index)
        } else {
            WKInterfaceDevice.current().play(.click)
        }
    }

    /// إلى الذكر التالي (أو «أتممت» بعد آخرها)
    private func advance(_ w: AdhkarWindow, from i: Int) {
        windowID = Self.id(w)
        index = i + 1
        count = 0
    }
}
