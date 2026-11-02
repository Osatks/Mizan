import WidgetKit
import SwiftUI

// ورد اليوم: صفحات اليوم من خطة الختمة، والمقروء من المصحف، والأيام المتتالية، ولمسها يفتح المصحف حيث توقف القارئ.
// من لقطة التطبيق (WidgetData.khatma في Kotlin)، وتتجدد كلما خرج من التطبيق وعند منتصف الليل.

struct KhatmaWidget: Widget {
    let kind = "MizanWird"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .day)) { entry in
            KhatmaView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("wird"))
        .description(Fallback.gallery("descWird"))
        .supportedFamilies(Families.make([.systemSmall, .systemMedium], circular: true, rectangular: true))
    }
}

extension Khatma {
    /// تقدّم الحلقة: ورد اليوم إن كانت للختمة مدة، وإلا المقروء من المصحف كله
    var ring: Double {
        if goal > 0 { return min(1, Double(today) / Double(goal)) }
        return min(1, Double(read) / Double(max(total, 1)))
    }

    /// ما في الحلقة: «3/20» صفحات اليوم، أو النسبة من المصحف
    var ringText: String { goal > 0 ? "\(today)/\(goal)" : percentText }
}

struct KhatmaView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    var body: some View {
        let k = entry.snap?.khatma
        let url = Deep.url(k?.route ?? "quran")
        if family.isAccessory {
            if #available(iOS 16.0, *) {
                KhatmaAccessory(khatma: k, family: family, snap: entry.snap, url: url)
            }
        } else {
            Shell(snap: entry.snap, url: url) {
                if let k = k {
                    if family == .systemMedium {
                        HStack(spacing: 16) {
                            KhatmaRing(khatma: k, size: 112)
                            KhatmaDetails(khatma: k)
                        }
                    } else {
                        VStack(spacing: 5) {
                            Header(symbol: "book.fill", title: k.title)
                            KhatmaRing(khatma: k, size: 78)
                                .frame(maxWidth: .infinity)
                            // «أتممت ورد اليوم» أو «ختمت القرآن …» قد تطول: سطران
                            Text(verbatim: k.portionDone || k.done ? k.doneText : k.streakText)
                                .font(Mizan.rounded(11, .medium))
                                .foregroundColor(k.portionDone || k.done ? Mizan.gold : Mizan.text2)
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .minimumScaleFactor(0.75)
                                .frame(maxWidth: .infinity)
                        }
                    }
                } else {
                    NeedsApp(snap: entry.snap ?? Sample.legacy)
                }
            }
        }
    }
}

struct KhatmaRing: View {
    let khatma: Khatma
    let size: CGFloat

    var body: some View {
        ProgressRing(progress: khatma.ring, color: Mizan.gold, width: size * 0.075) {
            VStack(spacing: 0) {
                if khatma.portionDone || khatma.done {
                    Image(systemName: "checkmark")
                        .font(.system(size: size * 0.24, weight: .bold))
                        .foregroundColor(Mizan.gold)
                } else {
                    Text(verbatim: khatma.ringText)
                        .font(Mizan.rounded(size * 0.22, .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }
            .padding(.horizontal, 6)
        }
        .frame(width: size, height: size)
        .background(Circle().fill(Color.white.opacity(0.08)).padding(size * 0.075))
    }
}

/// العنوان وصفحات اليوم والمقروء من المصحف والأيام المتتالية وزر المتابعة (الأداة المتوسطة)
struct KhatmaDetails: View {
    let khatma: Khatma

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Header(symbol: "book.fill", title: khatma.title)
            Text(verbatim: khatma.portionDone || khatma.done ? khatma.doneText : khatma.pages)
                .font(Mizan.rounded(15, .semibold))
                .lineLimit(2)
                .minimumScaleFactor(0.7)
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(verbatim: khatma.khatma).font(Mizan.rounded(10.5)).foregroundColor(Mizan.text2)
                    Spacer(minLength: 4)
                    Text(verbatim: khatma.percentText).font(Mizan.rounded(10.5, .semibold)).foregroundColor(Mizan.gold)
                        .environment(\.layoutDirection, .leftToRight)
                }
                ProgressView(value: min(1, Double(khatma.read) / Double(max(khatma.total, 1))))
                    .progressViewStyle(.linear)
                    .tint(Mizan.gold)
            }
            HStack(spacing: 6) {
                if khatma.streak > 0 {
                    Image(systemName: "flame.fill").font(.system(size: 10)).foregroundColor(Mizan.gold)
                    Text(verbatim: khatma.streakText).font(Mizan.rounded(10.5)).foregroundColor(Mizan.text2)
                        .lineLimit(1).minimumScaleFactor(0.7)
                }
                Spacer(minLength: 4)
                // زر المتابعة كاملاً (يصغر خطه قليلاً قبل أن يُقطع)، والأيام المتتالية تتنازل له عن المكان
                Text(verbatim: khatma.cta)
                    .font(Mizan.rounded(11, .semibold))
                    .foregroundColor(Mizan.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background(Capsule().fill(Color.white.opacity(0.15)))
                    .layoutPriority(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

@available(iOS 16.0, *)
struct KhatmaAccessory: View {
    let khatma: Khatma?
    let family: WidgetFamily
    let snap: Snapshot?
    let url: URL?

    var body: some View {
        switch family {
        case .accessoryCircular:
            AccessoryShell(snap: snap, url: url, circular: true) {
                Gauge(value: khatma?.ring ?? 0) {
                    Image(systemName: "book.fill")
                } currentValueLabel: {
                    Text(verbatim: khatma?.ringText ?? "—").monospacedDigit()
                }
                .gaugeStyle(.accessoryCircularCapacity)
            }
        default:
            AccessoryShell(snap: snap, url: url) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: khatma?.title ?? Fallback.text("wird"))
                        .font(Mizan.rounded(12, .semibold))
                        .widgetAccentable()
                        .lineLimit(1)
                    if let k = khatma {
                        Text(verbatim: k.portionDone || k.done ? k.doneText : k.pages)
                            .font(Mizan.rounded(14, .bold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        ProgressView(value: k.ring)
                            .progressViewStyle(.linear)
                    } else {
                        Text(Fallback.text("openApp")).font(Mizan.rounded(12)).lineLimit(2).minimumScaleFactor(0.7)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}
