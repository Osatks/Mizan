import SwiftUI
import WatchKit

// المسبحة في الساعة: كل درجة من التاج الرقمي أو لمسة على الحلقة تسبيحة، مع نقرة اهتزاز خفيفة، واهتزاز النجاح عند
// تمام العدد ثم تبدأ دورة جديدة. الذكر يتغير بلمسه، والعدد المطلوب (33 أو 100 أو بلا حد) بلمس ما تحت العدد.
// العدد محفوظ في الساعة نفسها.

struct TasbihPage: View {
    let labels: TasbihLabels?
    let rtl: Bool

    private static let dhikr = ["سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ", "لَا إِلَهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"]
    private static let targets = [33, 100, 0]

    @AppStorage("tasbih.count") private var count = 0
    @AppStorage("tasbih.rounds") private var rounds = 0
    @AppStorage("tasbih.today") private var today = 0
    @AppStorage("tasbih.day") private var day = ""
    @AppStorage("tasbih.dhikr") private var dhikrIndex = 0
    @AppStorage("tasbih.target") private var target = 33
    @State private var crown = 0.0
    @State private var pulse = false

    var body: some View {
        let gold = WatchColors.gold
        let goal = target
        let progress = goal > 0 ? Double(count) / Double(goal) : Double(count % 100) / 100
        VStack(spacing: 4) {
            Text(verbatim: Self.dhikr[dhikrIndex % Self.dhikr.count])
                .font(.system(size: 17, weight: .semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .environment(\.layoutDirection, .rightToLeft)
                .onTapGesture { dhikrIndex = (dhikrIndex + 1) % Self.dhikr.count; count = 0 }
            ZStack {
                Circle().stroke(Color.white.opacity(0.15), lineWidth: 7)
                Circle()
                    .trim(from: 0, to: CGFloat(max(0.001, min(progress, 1))))
                    .stroke(gold, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeOut(duration: 0.18), value: count)
                VStack(spacing: 0) {
                    Text(verbatim: "\(count)")
                        .font(.system(size: 40, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .contentTransition(.numericText())
                        .scaleEffect(pulse ? 1.08 : 1)
                    Text(verbatim: goalText(goal))
                        .font(.system(size: 12, weight: .medium, design: .rounded))
                        .foregroundStyle(.white.opacity(0.75))
                        .onTapGesture { cycleTarget() }
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            .contentShape(Circle())
            .onTapGesture { add() }
            .focusable()
            .digitalCrownRotation($crown, from: -1_000_000, through: 1_000_000, by: 1, sensitivity: .low, isContinuous: true, isHapticFeedbackEnabled: false)
            .onChange(of: crown) { old, new in
                // كل درجة إلى الأمام تسبيحة (الرجوع لا يُنقص)
                let steps = Int(new.rounded()) - Int(old.rounded())
                if steps > 0 { for _ in 0..<min(steps, 5) { add() } }
            }
            HStack {
                Text(verbatim: "\(labels?.today ?? (rtl ? "اليوم" : "Today")) \(today)")
                    .font(.system(size: 11, design: .rounded))
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(1)
                Spacer(minLength: 4)
                Button {
                    count = 0
                    WKInterfaceDevice.current().play(.retry)
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.system(size: 13, weight: .semibold))
                }
                .buttonStyle(.plain)
                .foregroundStyle(gold)
                .accessibilityLabel(labels?.reset ?? (rtl ? "تصفير" : "Reset"))
            }
            .padding(.horizontal, 6)
        }
        .padding(.horizontal, 4)
        .containerBackground(LinearGradient(colors: [Color(watchHex: "#123B31"), Color(watchHex: "#08130F")], startPoint: .top, endPoint: .bottom), for: .tabView)
        .navigationTitle(labels?.title ?? (rtl ? "المسبحة" : "Tasbih"))
        .onAppear { newDay() }
    }

    private func goalText(_ goal: Int) -> String {
        guard goal > 0 else { return "∞" }
        if let of = labels?.of { return String(format: of, goal) }
        return "/\(goal)"
    }

    private func cycleTarget() {
        let i = Self.targets.firstIndex(of: target) ?? 0
        target = Self.targets[(i + 1) % Self.targets.count]
        count = 0
        WKInterfaceDevice.current().play(.click)
    }

    private func newDay() {
        let key = ISO8601DateFormatter.string(from: Date(), timeZone: .current, formatOptions: [.withFullDate])
        if day != key {
            day = key
            today = 0
        }
    }

    private func add() {
        newDay()
        count += 1
        today += 1
        withAnimation(.spring(response: 0.2, dampingFraction: 0.5)) { pulse = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { withAnimation(.easeOut(duration: 0.15)) { pulse = false } }
        if target > 0 && count >= target {
            count = 0
            rounds += 1
            WKInterfaceDevice.current().play(.success)
        } else {
            WKInterfaceDevice.current().play(.click)
        }
    }
}
