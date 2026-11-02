import WidgetKit
import SwiftUI
import AppIntents

// ---------- أذكار الوقت ----------
// الأذكار المناسبة لهذه اللحظة (قواعد AdhkarClock في Kotlin محسوبة لأسبوع في اللقطة): أذكار الأذان عند دخول الصلاة،
// ثم أذكار ما بعد السلام، والصباح بعد الفجر، والمساء بعد العصر، والنوم بعد العشاء، والاستغفار في الثلث الأخير،
// وسنن الجمعة (الكهف والصلاة على النبي ﷺ وساعة الإجابة)، ودعاء الإفطار في رمضان، ودعاء يوم عرفة.
// الذكر ينتقل وحده كل عشرين دقيقة، وفي iOS 17 زر «التالي» يقدّمه مباشرة.

struct AdhkarWidget: Widget {
    let kind = "MizanAdhkar"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .adhkar)) { entry in
            AdhkarView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("adhkar"))
        .description(Fallback.gallery("descAdhkar"))
        .supportedFamilies(Families.make([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge], rectangular: true))
    }
}

/// زر «التالي» (iOS 17): يقدّم الذكر في النافذة الحالية، ثم يعيد WidgetKit بناء خط الزمن
@available(iOS 17.0, *)
struct NextDhikrIntent: AppIntent {
    static let title: LocalizedStringResource = "Next dhikr"

    init() {}

    func perform() async throws -> some IntentResult {
        AdhkarCursor.advance()
        return .result()
    }
}

struct AdhkarView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    /// أطول نص يسعه كل حجم؛ الأطول منه يُترك للأحجام الأكبر
    var maxLength: Int {
        switch family {
        case .systemSmall: return 110
        case .systemMedium: return 300
        case .systemLarge, .systemExtraLarge: return .max
        default: return 70
        }
    }

    var body: some View {
        let snap = entry.snap
        let w = snap?.window(at: entry.date)
        let list = (snap != nil && w != nil) ? snap!.dhikrs(w!, maxLength: maxLength) : []
        let i = (snap != nil && w != nil) ? snap!.index(at: entry.date, in: w!, offset: entry.offset, count: list.count) : 0
        let d: Dhikr? = list.indices.contains(i) ? list[i] : nil
        let url = Deep.url("adhkar", section: w?.section)

        if family.isAccessory {
            if #available(iOS 16.0, *) {
                AccessoryShell(snap: snap, url: url) {
                    VStack(alignment: .leading, spacing: 1) {
                        Text(w?.title ?? Fallback.text("adhkar"))
                            .font(Mizan.rounded(12, .semibold))
                            .lineLimit(1)
                            .widgetAccentable()
                        Text(d?.ar ?? "")
                            .font(.system(size: 14, weight: .medium))
                            .lineLimit(2)
                            .minimumScaleFactor(0.7)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        } else {
            Shell(snap: snap, url: url) {
                if let snap = snap, let w = w, let d = d {
                    AdhkarCard(snap: snap, window: w, dhikr: d, position: i + 1, total: list.count, family: family)
                } else {
                    NeedsApp(snap: snap)
                }
            }
        }
    }
}

/// بطاقة الذكر: العنوان وشارته وموضع الذكر، ثم النص بخط المصحف، ثم الترجمة والفضل، ثم العدد والمصدر وزر التالي
struct AdhkarCard: View {
    let snap: Snapshot
    let window: AdhkarWindow
    let dhikr: Dhikr
    let position: Int
    let total: Int
    let family: WidgetFamily

    var symbol: String {
        switch window.k {
        case "adhan", "afterPrayer": return "hands.sparkles.fill"
        case "morning": return "sunrise.fill"
        case "evening": return "sunset.fill"
        case "night", "lastThird": return "moon.stars.fill"
        case "iftar": return "moon.fill"
        default: return "sparkles"
        }
    }

    var body: some View {
        let small = family == .systemSmall
        // الكبيرة جداً (iPad) بتصميم الكبيرة نفسه، وعرضها الواسع يكفي الذكر الطويل بخط أكبر قليلاً
        let large = family == .systemLarge || family == .systemExtraLarge
        VStack(spacing: 0) {
            Header(symbol: symbol, title: window.title) {
                if let chip = window.chip, !small {
                    Chip(text: chip)
                }
                if !small {
                    Text(verbatim: "\(position)/\(total)")
                        .font(Mizan.rounded(11))
                        .monospacedDigit()
                        .foregroundColor(Mizan.text3)
                }
            }
            Spacer(minLength: 6)
            Text(verbatim: dhikr.ar)
                .font(Mizan.quran(small ? 17 : (family == .systemExtraLarge ? 28 : (large ? 25 : 20))))
                .multilineTextAlignment(.center)
                .lineSpacing(large ? 6 : 2)
                .lineLimit(small ? 5 : (large ? 12 : 4))
                .minimumScaleFactor(0.45)
                .environment(\.layoutDirection, .rightToLeft)
                .frame(maxWidth: .infinity)
            if let tr = dhikr.tr, !small {
                Text(verbatim: tr)
                    .font(Mizan.rounded(large ? 12 : 11))
                    .foregroundColor(Mizan.text2)
                    .multilineTextAlignment(.center)
                    .lineLimit(large ? 5 : 2)
                    .minimumScaleFactor(0.8)
                    .padding(.top, 5)
            }
            if let fadl = dhikr.fadl, large {
                HStack(spacing: 8) {
                    Capsule().fill(Mizan.gold.opacity(0.7)).frame(width: 2.5)
                    Text(verbatim: fadl)
                        .font(Mizan.rounded(11))
                        .foregroundColor(Mizan.text2)
                        .lineLimit(3)
                        .minimumScaleFactor(0.8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(Color.white.opacity(0.07)))
                .padding(.top, 8)
            }
            Spacer(minLength: 6)
            HStack(spacing: 8) {
                if let c = dhikr.c {
                    Chip(text: c, gold: true)
                }
                if !small {
                    Text(verbatim: dhikr.src)
                        .font(Mizan.rounded(10))
                        .foregroundColor(Mizan.text3)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                if large {
                    Text(verbatim: window.until)
                        .font(Mizan.rounded(10))
                        .foregroundColor(Mizan.text3)
                        .lineLimit(1)
                }
                NextButton(size: small ? 26 : 30)
            }
        }
    }
}

/// زر دائري للذكر التالي؛ قبل iOS 17 لا تفاعل في الأدوات فيبقى التدوير التلقائي وحده
struct NextButton: View {
    let size: CGFloat

    var body: some View {
        if #available(iOS 17.0, *) {
            Button(intent: NextDhikrIntent()) {
                Image(systemName: "chevron.forward")
                    .font(.system(size: size * 0.42, weight: .semibold))
                    .foregroundColor(Mizan.text)
                    .frame(width: size, height: size)
                    .background(Circle().fill(Color.white.opacity(0.15)))
                    .overlay(Circle().stroke(Color.white.opacity(0.2), lineWidth: 1))
            }
            .buttonStyle(.plain)
        }
    }
}
