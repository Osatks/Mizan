import SwiftUI
import WidgetKit

// رسم مضاعفات وجه الساعة، منفصلاً عن مزوّداتها (WatchComplications.swift) حتى يرسمها تطبيق الساعة أيضاً في صفحة
// المعرض (-watch.page 3 و 4) فتُلتقط صورها في المحاكي كما تظهر. كل رسم يأخذ حجمه صراحةً، والخلفية والرابط من المضاعفة.
// الملف في الهدفين معاً (تطبيق الساعة ومضاعفاتها).

enum Face {
    static let gold = Color(red: 0.91, green: 0.76, blue: 0.35)
}

extension PrayerPhase {
    var image: Image { symbol.custom ? Image(symbol.name) : Image(systemName: symbol.name) }
}

/// العدّ التنازلي إلى [to]: أرقامه من اليسار إلى اليمين في كل اللغات، ويملأ عرضه فيُحاذى نصه في اتجاه اللغة
private struct Countdown: View {
    let from: Date
    let to: Date
    let rtl: Bool

    var body: some View {
        Text(timerInterval: from...max(from, to), countsDown: true)
            .monospacedDigit()
            .multilineTextAlignment(rtl ? .trailing : .leading)
            .environment(\.layoutDirection, .leftToRight)
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
            image.font(.system(size: 20, weight: .semibold)).widgetAccentable()
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
        let done = p.done(at: date)
        switch family {
        case .accessoryCircular:
            Gauge(value: done) {
                p.image
            } currentValueLabel: {
                VStack(spacing: 0) {
                    Text(verbatim: p.prayer.name).font(.system(size: 10, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                    if p.kind == .iqama {
                        // الإقامة بعد دقائق: العدّ نفسه بدل وقت الأذان الذي مضى
                        Text(timerInterval: date...max(date, p.to), countsDown: true)
                            .font(.system(size: 11, weight: .bold)).monospacedDigit()
                            .multilineTextAlignment(.center)
                            .lineLimit(1).minimumScaleFactor(0.5)
                            .environment(\.layoutDirection, .leftToRight)
                    } else {
                        Text(verbatim: p.prayer.s).font(.system(size: 12, weight: .bold)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.6)
                    }
                }
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .widgetAccentable()
        case .accessoryInline:
            if p.kind == .iqama {
                Text(verbatim: "\(p.label) ") + Text(timerInterval: date...max(date, p.to), countsDown: true)
            } else if p.kind == .next {
                Text(verbatim: "\(p.prayer.name) \(p.prayer.t)")
            } else {
                Text(verbatim: "\(p.label) \(p.prayer.t)")
            }
        case .accessoryCorner:
            Text(timerInterval: date...max(date, p.to), countsDown: true)
                .monospacedDigit()
                .widgetLabel {
                    Gauge(value: done) { Text(verbatim: p.prayer.name) }
                        .tint(Face.gold)
                }
        default:
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    p.image.font(.system(size: 11, weight: .semibold))
                    Text(verbatim: p.label).font(.system(size: 12, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                }
                .foregroundStyle(Face.gold)
                .widgetAccentable()
                HStack(alignment: .firstTextBaseline) {
                    Text(verbatim: p.prayer.name).font(.system(size: 17, weight: .bold)).lineLimit(1).minimumScaleFactor(0.7)
                    Spacer(minLength: 4)
                    if p.kind != .iqama {
                        Text(verbatim: p.prayer.t).font(.system(size: 13, weight: .semibold)).monospacedDigit()
                            .lineLimit(1)
                            .environment(\.layoutDirection, .leftToRight)
                    }
                }
                Countdown(from: date, to: p.to, rtl: snap.rtl)
                    .font(.system(size: 15, weight: .medium))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
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
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 4) {
                        Image("mizan.mosque").font(.system(size: 11, weight: .semibold))
                        Text(verbatim: snap.text("prayerTimes", "Prayer times")).font(.system(size: 11, weight: .semibold)).lineLimit(1)
                    }
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    rows(list)
                }
                rows(list)
            }
            .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
        } else {
            FacePlaceholder(family: .accessoryRectangular, image: Image("mizan.mosque"))
        }
    }

    private func rows(_ list: [PrayerTime]) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            ForEach(Array(list.enumerated()), id: \.offset) { i, p in
                HStack(spacing: 4) {
                    Text(verbatim: p.name).lineLimit(1).minimumScaleFactor(0.7)
                    Spacer(minLength: 4)
                    Text(verbatim: p.t).monospacedDigit().lineLimit(1).environment(\.layoutDirection, .leftToRight)
                }
                .font(.system(size: i == 0 ? 16 : 14, weight: i == 0 ? .bold : .medium, design: .rounded))
                .foregroundStyle(i == 0 ? AnyShapeStyle(Face.gold) : AnyShapeStyle(.primary))
                .widgetAccentable(i == 0)
            }
        }
    }
}

// ---------- التاريخ الهجري ----------

struct HijriFace: View {
    let snap: Snapshot?
    let date: Date
    let family: WidgetFamily

    var body: some View {
        if let snap = snap, let day = snap.day(at: date) {
            let month = snap.watch?.months[day.date] ?? day.hijriMonth
            switch family {
            case .accessoryInline:
                Text(verbatim: day.hijri)
            case .accessoryCircular:
                VStack(spacing: -2) {
                    Text(verbatim: day.hijriDay)
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .widgetAccentable()
                    Text(verbatim: month)
                        .font(.system(size: 9, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .padding(.horizontal, 5)
                }
            case .accessoryCorner:
                Text(verbatim: day.hijriDay)
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .widgetAccentable()
                    .widgetLabel { Text(verbatim: month) }
            default:
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 4) {
                        Image(systemName: "moon.fill").font(.system(size: 10, weight: .semibold))
                        Text(verbatim: day.weekday).font(.system(size: 12, weight: .semibold)).lineLimit(1)
                    }
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    Text(verbatim: day.hijri)
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(verbatim: day.greg)
                        .font(.system(size: 13, design: .rounded))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .environment(\.layoutDirection, snap.rtl ? .rightToLeft : .leftToRight)
            }
        } else {
            FacePlaceholder(family: family, image: Image(systemName: "moon.fill"))
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
                QiblaDial(degrees: w.qibla, label: w.qiblaText, background: false)
            case .accessoryCorner:
                Image("mizan.kaaba")
                    .font(.system(size: 20, weight: .semibold))
                    .widgetAccentable()
                    .widgetLabel { Text(verbatim: "\(w.qiblaText) \(w.qiblaDir)") }
            default:
                HStack(spacing: 8) {
                    QiblaDial(degrees: w.qibla, label: nil, background: true)
                        .frame(width: 54, height: 54)
                    VStack(alignment: .leading, spacing: 0) {
                        Text(verbatim: w.qiblaTitle)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Face.gold)
                            .widgetAccentable()
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Text(verbatim: w.qiblaText)
                            .font(.system(size: 22, weight: .bold, design: .rounded))
                            .lineLimit(1)
                        Text(verbatim: w.qiblaDir)
                            .font(.system(size: 12, design: .rounded))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
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

/// قرص ثابت صغير: الشمال أعلاه (مثلث)، والكعبة على الحلقة في زاوية القبلة، والزاوية في وسطه
struct QiblaDial: View {
    let degrees: Double
    let label: String?
    let background: Bool

    var body: some View {
        GeometryReader { g in
            let r = min(g.size.width, g.size.height) / 2
            let a = degrees * .pi / 180
            ZStack {
                if background { AccessoryWidgetBackground() }
                Circle()
                    .stroke(Color.white.opacity(0.35), lineWidth: 1)
                    .padding(r * 0.12)
                // الشمال
                NorthMark()
                    .fill(Color.white.opacity(0.9))
                    .frame(width: r * 0.26, height: r * 0.2)
                    .offset(y: -r * 0.86)
                // الكعبة على الحلقة، قائمة دائماً
                Image("mizan.kaaba")
                    .font(.system(size: r * 0.4, weight: .semibold))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                    .offset(x: sin(a) * r * 0.62, y: -cos(a) * r * 0.62)
                if let label = label {
                    Text(verbatim: label)
                        .font(.system(size: r * 0.36, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .frame(width: r * 0.9)
                        // بعيداً عن الكعبة: تحت المركز إن كانت أعلاه، وفوقه إن كانت أسفله
                        .offset(y: cos(a) > 0.3 ? r * 0.2 : (cos(a) < -0.3 ? -r * 0.2 : 0))
                }
            }
            .frame(width: g.size.width, height: g.size.height)
        }
        // الإزاحة الأفقية من الشمال مع عقارب الساعة في كل اللغات
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
                .widgetAccentable()
                .widgetLabel { Text(verbatim: "سُبْحَانَ اللَّهِ") }
        case .accessoryRectangular:
            HStack(spacing: 8) {
                Image("mizan.tasbih")
                    .font(.system(size: 26, weight: .semibold))
                    .foregroundStyle(Face.gold)
                    .widgetAccentable()
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: title)
                        .font(.system(size: 15, weight: .bold))
                        .lineLimit(1)
                    // التسبيح بعد الصلاة: ٣٣ لكلٍّ
                    Text(verbatim: "سُبْحَانَ اللَّهِ · الْحَمْدُ لِلَّهِ · اللَّهُ أَكْبَرُ")
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .environment(\.layoutDirection, .rightToLeft)
                    Text(verbatim: "33 × 3")
                        .font(.system(size: 12, weight: .semibold, design: .rounded))
                        .foregroundStyle(Face.gold)
                        .widgetAccentable()
                }
                Spacer(minLength: 0)
            }
        default:
            Image("mizan.tasbih")
                .font(.system(size: 22, weight: .semibold))
                .widgetAccentable()
        }
    }
}
