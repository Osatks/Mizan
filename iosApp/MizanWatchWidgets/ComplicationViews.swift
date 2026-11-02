import SwiftUI
import WidgetKit

// رسم مضاعفات وجه الساعة والمكدس الذكي، منفصلاً عن مزوّداتها (WatchComplications.swift) حتى يرسمها تطبيق الساعة
// أيضاً في معرضه (-watch.page 10 و 11 و 12) فتُلتقط صورها في المحاكي كما تظهر. كل رسم يأخذ حجمه صراحةً، والخلفية
// والرابط من المضاعفة. الملف في الهدفين معاً (تطبيق الساعة ومضاعفاتها).
//
// لغة التصميم: عنوان صغير ذهبي برمزه، ثم المعلومة الأهم كبيرة بخط SF Rounded، ثم التفاصيل بلون ثانوي؛ كل ما يخص
// شيئاً واحداً متجاور (اسم الصلاة ووقتها، والعدّ وشريطه) بلا فراغ في وسط السطر. الذهبي وحده هو اللون الذي يتلوّن به
// وجه الساعة (widgetAccentable)، والأرقام بعرض ثابت.

enum Face {
    static let gold = Color(red: 0.93, green: 0.78, blue: 0.40)
    static let dim = Color.white.opacity(0.62)
    static let track = Color.white.opacity(0.18)

    static func title(_ size: CGFloat = 12) -> Font { .system(size: size, weight: .semibold, design: .rounded) }
    static func big(_ size: CGFloat) -> Font { .system(size: size, weight: .bold, design: .rounded) }
}

extension PrayerPhase {
    var image: Image { symbol.custom ? Image(symbol.name) : Image(systemName: symbol.name) }
}

/// رمز SF أو رمز ميزان في Assets
func faceImage(_ s: (name: String, custom: Bool)) -> Image {
    s.custom ? Image(s.name) : Image(systemName: s.name)
}

/// عنوان المضاعفة: رمز وكلمة بالذهبي
private struct FaceHeader: View {
    let image: Image
    let text: String

    var body: some View {
        HStack(spacing: 4) {
            image.font(.system(size: 11, weight: .semibold))
            Text(verbatim: text).font(Face.title()).lineLimit(1).minimumScaleFactor(0.7)
        }
        .foregroundStyle(Face.gold)
        .widgetAccentable()
    }
}

/// العدّ التنازلي إلى [to] بعرض نصه فقط (لا يملأ السطر): أرقامه من اليسار إلى اليمين في كل اللغات
private struct Countdown: View {
    let from: Date
    let to: Date
    let size: CGFloat
    let rtl: Bool

    var body: some View {
        let left = max(0, to.timeIntervalSince(from))
        // «2:15:47» أو «15:47»: عرض الأرقام الثابت ونقطتيها
        let width = size * (left >= 3600 ? 3.78 : 2.84) + 2
        Text(timerInterval: from...max(from, to), countsDown: true)
            .font(.system(size: size, weight: .semibold, design: .rounded))
            .monospacedDigit()
            .lineLimit(1)
            .multilineTextAlignment(rtl ? .trailing : .leading)
            .frame(width: width, alignment: rtl ? .trailing : .leading)
            .environment(\.layoutDirection, .leftToRight)
    }
}

/// شريط ما مضى من الوقت: يتقدم وحده (ProgressView بفترة زمنية)
private struct TimeBar: View {
    let from: Date
    let to: Date

    var body: some View {
        ProgressView(timerInterval: from...max(from.addingTimeInterval(1), to), countsDown: false) {
            EmptyView()
        } currentValueLabel: {
            EmptyView()
        }
        .progressViewStyle(.linear)
        .tint(Face.gold)
        .widgetAccentable()
    }
}

/// بلا لقطة من iPhone بعد: رمز المضاعفة وحده
struct FacePlaceholder: View {
    let family: WidgetFamily
    let image: Image

    var body: some View {
        switch family {
        case .accessoryInline:
            Text(verbatim: "Mizan")
        default:
            image.font(.system(size: 20, weight: .semibold)).foregroundStyle(Face.gold).widgetAccentable()
        }
    }
}

// ---------- الصلاة القادمة (والإقامة بعد الأذان، والإفطار والسحور في رمضان) ----------

struct NextPrayerFace: View {
    let snap: Snapshot?
    let date: Date
    let family: WidgetFamily

    var body: some View {
        if let snap = snap, let p = snap.phase(at: date) {
            content(snap, p)
        } else {
            FacePlaceholder(family: family, image: Image(systemName: "moon.stars.fill"))
        }
    }

    @ViewBuilder
    private func content(_ snap: Snapshot, _ p: PrayerPhase) -> some View {
        let iqama = p.kind == .iqama
        switch family {
        case .accessoryCircular:
            Gauge(value: p.done(at: date)) {
                p.image
            } currentValueLabel: {
                VStack(spacing: 0) {
                    p.image
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(Face.gold)
                        .widgetAccentable()
                    Text(verbatim: p.prayer.name)
                        .font(.system(size: 10, weight: .semibold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    if iqama {
                        // الإقامة بعد دقائق: العدّ نفسه بدل وقت الأذان الذي مضى
                        Text(timerInterval: date...max(date, p.to), countsDown: true)
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .multilineTextAlignment(.center)
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                            .environment(\.layoutDirection, .leftToRight)
                    } else {
                        Text(verbatim: p.prayer.s)
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                    }
                }
                .padding(.horizontal, 2)
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .tint(Face.gold)
        case .accessoryInline:
            if iqama {
                Text(verbatim: "\(p.label) ") + Text(timerInterval: date...max(date, p.to), countsDown: true)
            } else if p.kind == .next {
                Text(verbatim: "\(p.prayer.name) \(p.prayer.t)")
            } else {
                Text(verbatim: "\(p.label) \(p.prayer.t)")
            }
        case .accessoryCorner:
            p.image
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Face.gold)
                .widgetAccentable()
                .widgetLabel {
                    iqama
                        ? Text(verbatim: "\(p.label) ") + Text(timerInterval: date...max(date, p.to), countsDown: true)
                        : Text(verbatim: "\(p.prayer.name) \(p.prayer.s)")
                }
        default:
            VStack(alignment: .leading, spacing: 3) {
                FaceHeader(image: p.image, text: p.label)
                // اسم الصلاة ووقتها متجاوران
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(verbatim: p.prayer.name)
                        .font(Face.big(21))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .layoutPriority(1)
                    if !iqama {
                        Text(verbatim: p.prayer.t)
                            .font(.system(size: 14, weight: .medium, design: .rounded))
                            .foregroundStyle(Face.dim)
                            .monospacedDigit()
                            .lineLimit(1)
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    Spacer(minLength: 0)
                }
                // العدّ وشريط ما مضى في سطر واحد
                HStack(spacing: 7) {
                    Countdown(from: date, to: p.to, size: 14, rtl: snap.rtl)
                        .foregroundStyle(.white)
                    TimeBar(from: p.from, to: p.to)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
        }
    }
}

// ---------- مواقيت اليوم: الصلوات الثلاث القادمة، والتالية منها بالذهبي ----------

struct TodayPrayersFace: View {
    let snap: Snapshot?
    let date: Date

    var body: some View {
        if let snap = snap, !snap.upcoming(after: date, count: 1).isEmpty {
            let list = snap.upcoming(after: date, count: 3)
            // بالعنوان إن اتسع له الارتفاع (أصغر الساعات بلا عنوان)
            ViewThatFits(in: .vertical) {
                VStack(alignment: .leading, spacing: 2) {
                    FaceHeader(image: Image("mizan.mosque"), text: snap.text("prayerTimes", "Prayer times"))
                    rows(list)
                }
                rows(list)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
        } else {
            FacePlaceholder(family: .accessoryRectangular, image: Image("mizan.mosque"))
        }
    }

    private func rows(_ list: [PrayerTime]) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            ForEach(Array(list.enumerated()), id: \.offset) { i, p in
                let next = i == 0
                HStack(spacing: 6) {
                    // خط زمني: نقطة ممتلئة للتالية
                    Circle()
                        .strokeBorder(next ? Face.gold : Face.dim, lineWidth: next ? 0 : 1.2)
                        .background(Circle().fill(next ? Face.gold : .clear))
                        .frame(width: 6, height: 6)
                        .widgetAccentable(next)
                    Text(verbatim: p.name)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Spacer(minLength: 6)
                    Text(verbatim: p.t)
                        .monospacedDigit()
                        .lineLimit(1)
                        .environment(\.layoutDirection, .leftToRight)
                }
                .font(.system(size: next ? 15 : 14, weight: next ? .bold : .medium, design: .rounded))
                .foregroundStyle(next ? AnyShapeStyle(Face.gold) : AnyShapeStyle(Color.white.opacity(0.85)))
                .widgetAccentable(next)
            }
        }
    }
}

// ---------- الأذكار حسب الوقت ----------

struct AdhkarFace: View {
    let snap: Snapshot?
    let date: Date
    let family: WidgetFamily

    var body: some View {
        if let snap = snap, let w = snap.window(at: date) {
            content(snap, w)
        } else {
            FacePlaceholder(family: family, image: Image(systemName: "sun.horizon.fill"))
        }
    }

    @ViewBuilder
    private func content(_ snap: Snapshot, _ w: AdhkarWindow) -> some View {
        let icon = faceImage(AdhkarClockWatch.symbol(w.k))
        switch family {
        case .accessoryCircular:
            VStack(spacing: 1) {
                icon.font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                Text(verbatim: w.chip ?? w.title)
                    .font(.system(size: 8.5, weight: .semibold, design: .rounded))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 5)
            }
        case .accessoryInline:
            Text(verbatim: "\(w.title) · \(w.until)")
        case .accessoryCorner:
            icon.font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Face.gold)
                .widgetAccentable()
                .widgetLabel { Text(verbatim: w.title) }
        default:
            VStack(alignment: .leading, spacing: 2) {
                FaceHeader(image: icon, text: w.chip.map { "\(w.title) · \($0)" } ?? w.title)
                if let d = snap.shortDhikr(at: date, in: w) {
                    // الذكر بالعربية في كل اللغات، يبدأ من اليمين
                    Text(verbatim: d.ar)
                        .font(.system(size: 15, weight: .semibold))
                        .lineLimit(2)
                        .minimumScaleFactor(0.75)
                        .multilineTextAlignment(.leading)
                        .environment(\.layoutDirection, .rightToLeft)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Text(verbatim: [snap.countText(d), w.until].compactMap { $0 }.joined(separator: " · "))
                        .font(.system(size: 11, weight: .medium, design: .rounded))
                        .foregroundStyle(Face.dim)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                } else {
                    Text(verbatim: w.until)
                        .font(.system(size: 13, design: .rounded))
                        .foregroundStyle(Face.dim)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
        }
    }
}

// ---------- التاريخ الهجري ----------

struct HijriFace: View {
    let snap: Snapshot?
    let date: Date
    let family: WidgetFamily

    /// طور القمر من اليوم الهجري (الهلال أول الشهر، والبدر في منتصفه)
    static func moon(_ day: Int) -> String {
        switch day {
        case 1...5: return "moonphase.waxing.crescent"
        case 6...9: return "moonphase.first.quarter"
        case 10...13: return "moonphase.waxing.gibbous"
        case 14...16: return "moonphase.full.moon"
        case 17...20: return "moonphase.waning.gibbous"
        case 21...23: return "moonphase.last.quarter"
        case 24...28: return "moonphase.waning.crescent"
        default: return "moonphase.new.moon"
        }
    }

    var body: some View {
        if let snap = snap, let day = snap.day(at: date) {
            let month = snap.watch?.months[day.date] ?? day.hijriMonth
            let moon = Image(systemName: Self.moon(Int(day.hijriDay) ?? 0))
            switch family {
            case .accessoryInline:
                Text(verbatim: day.hijri)
            case .accessoryCircular:
                VStack(spacing: -1) {
                    moon.font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(Face.gold)
                        .widgetAccentable()
                    Text(verbatim: day.hijriDay)
                        .font(Face.big(21))
                        .monospacedDigit()
                    Text(verbatim: month)
                        .font(.system(size: 8.5, weight: .semibold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .padding(.horizontal, 6)
                }
            case .accessoryCorner:
                Text(verbatim: day.hijriDay)
                    .font(Face.big(20))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    .widgetLabel { Text(verbatim: month) }
            default:
                VStack(alignment: .leading, spacing: 2) {
                    FaceHeader(image: moon, text: day.weekday)
                    Text(verbatim: day.hijri)
                        .font(Face.big(18))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(verbatim: day.greg)
                        .font(.system(size: 13, weight: .medium, design: .rounded))
                        .foregroundStyle(Face.dim)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
            }
        } else {
            FacePlaceholder(family: family, image: Image(systemName: "moonphase.waxing.crescent"))
        }
    }
}

// ---------- اتجاه القبلة ----------

struct QiblaFace: View {
    let snap: Snapshot?
    let family: WidgetFamily

    var body: some View {
        if let snap = snap, let w = snap.watch {
            switch family {
            case .accessoryInline:
                ViewThatFits {
                    Text(verbatim: "\(w.qiblaTitle) \(w.qiblaText)")
                    Text(verbatim: w.qiblaText)
                }
            case .accessoryCircular:
                MiniCompass(degrees: w.qibla)
                    .padding(3)
            case .accessoryCorner:
                Image("mizan.kaaba")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    .widgetLabel { Text(verbatim: "\(w.qiblaText) \(w.qiblaDir)") }
            default:
                HStack(spacing: 10) {
                    ZStack {
                        AccessoryWidgetBackground()
                        MiniCompass(degrees: w.qibla).padding(4)
                    }
                    .frame(width: 58, height: 58)
                    VStack(alignment: .leading, spacing: 1) {
                        FaceHeader(image: Image("mizan.kaaba"), text: w.qiblaTitle)
                        HStack(alignment: .firstTextBaseline, spacing: 5) {
                            Text(verbatim: w.qiblaText)
                                .font(Face.big(22))
                                .lineLimit(1)
                                .environment(\.layoutDirection, .leftToRight)
                            Text(verbatim: w.qiblaDir)
                                .font(.system(size: 13, weight: .medium, design: .rounded))
                                .foregroundStyle(Face.dim)
                                .lineLimit(1)
                                .minimumScaleFactor(0.6)
                        }
                        Text(verbatim: w.qiblaKm)
                            .font(.system(size: 11, weight: .medium, design: .rounded))
                            .foregroundStyle(Face.dim)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                }
                .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
            }
        } else {
            FacePlaceholder(family: family, image: Image("mizan.kaaba"))
        }
    }
}

/// بوصلة صغيرة ثابتة: علامات كل ٣٠°، والشمال مثلث، وإبرة ذهبية من المركز إلى الكعبة في زاوية القبلة
struct MiniCompass: View {
    let degrees: Double

    var body: some View {
        GeometryReader { g in
            let r = min(g.size.width, g.size.height) / 2
            let a = degrees * .pi / 180
            ZStack {
                ForEach(0..<12, id: \.self) { i in
                    Capsule()
                        .fill(Color.white.opacity(i % 3 == 0 ? 0.8 : 0.35))
                        .frame(width: i % 3 == 0 ? 1.6 : 1, height: r * (i % 3 == 0 ? 0.16 : 0.1))
                        .offset(y: -r * (i % 3 == 0 ? 0.92 : 0.95))
                        .rotationEffect(.degrees(Double(i) * 30))
                }
                NorthMark()
                    .fill(Color.white)
                    .frame(width: r * 0.22, height: r * 0.18)
                    .offset(y: -r * 0.7)
                // الإبرة نحو القبلة
                Capsule()
                    .fill(Face.gold)
                    .frame(width: max(1.5, r * 0.06), height: r * 0.34)
                    .offset(y: -r * 0.17)
                    .rotationEffect(.degrees(degrees))
                    .widgetAccentable()
                Circle()
                    .fill(Face.gold)
                    .frame(width: r * 0.14, height: r * 0.14)
                    .widgetAccentable()
                // الكعبة في طرف الإبرة، قائمة دائماً
                Image("mizan.kaaba")
                    .font(.system(size: r * 0.36, weight: .semibold))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    .offset(x: sin(a) * r * 0.56, y: -cos(a) * r * 0.56)
            }
            .frame(width: g.size.width, height: g.size.height)
        }
        // الإزاحة والدوران مع عقارب الساعة في كل اللغات
        .environment(\.layoutDirection, .leftToRight)
    }
}

/// مثلث رأسه للأعلى
struct NorthMark: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        p.closeSubpath()
        return p
    }
}

// ---------- المسبحة ----------

struct TasbihFace: View {
    let title: String
    let family: WidgetFamily

    var body: some View {
        switch family {
        case .accessoryCorner:
            Image("mizan.tasbih")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Face.gold)
                .widgetAccentable()
                .widgetLabel { Text(verbatim: "سُبْحَانَ اللَّهِ") }
        case .accessoryRectangular:
            HStack(spacing: 10) {
                ZStack {
                    AccessoryWidgetBackground()
                    Image("mizan.tasbih")
                        .font(.system(size: 24, weight: .semibold))
                        .foregroundStyle(Face.gold)
                        .widgetAccentable()
                }
                .frame(width: 52, height: 52)
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: title)
                        .font(Face.title(13))
                        .foregroundStyle(Face.gold)
                        .widgetAccentable()
                        .lineLimit(1)
                    // التسبيح دبر الصلاة: ثلاث وثلاثون لكلٍّ
                    Text(verbatim: "سُبْحَانَ اللَّهِ · الْحَمْدُ لِلَّهِ · اللَّهُ أَكْبَرُ")
                        .font(.system(size: 13, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .environment(\.layoutDirection, .rightToLeft)
                    Text(verbatim: "33 × 3")
                        .font(.system(size: 12, weight: .medium, design: .rounded))
                        .foregroundStyle(Face.dim)
                        .environment(\.layoutDirection, .leftToRight)
                }
                Spacer(minLength: 0)
            }
        default:
            Image("mizan.tasbih")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(Face.gold)
                .widgetAccentable()
        }
    }
}
