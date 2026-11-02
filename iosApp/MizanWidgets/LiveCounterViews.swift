import SwiftUI
import WidgetKit
import ActivityKit
import AppIntents

// المسبحة وعدّاد الأذكار على شاشة القفل وفي الجزيرة الديناميكية (iOS 17 فما بعد: أزرارهما نوايا تعمل بلا فتح التطبيق).
// تبدؤهما النوايا (Shared/LiveIntents.swift) من زر الإجراء ومركز التحكم والأداة و Siri، وتعدّ أزرارهما بلا فتح التطبيق.
// التصميم نفسه في الأدوات والعدّ التنازلي للصلاة: أخضر التطبيق العميق، والذهبي للعدد والزر، ونجمة ثمانية خافتة.

// ---------- أجزاء مشتركة ----------

/// حلقة تقدّم ذهبية حول ما فيها
struct ProgressRing<Content: View>: View {
    let progress: Double
    let color: Color
    var track: Color = Color.white.opacity(0.16)
    var width: CGFloat = 5
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack {
            Circle().stroke(track, lineWidth: width)
            Circle()
                .trim(from: 0, to: CGFloat(max(0.001, min(progress, 1))))
                .stroke(color, style: StrokeStyle(lineWidth: width, lineCap: .round))
                .rotationEffect(.degrees(-90))
                // من الأعلى مع عقارب الساعة في كل اللغات (كحلقات النشاط في Apple Watch): زاوية rotationEffect تُعكس
                // في الاتجاه من اليمين إلى اليسار فكانت الحلقة بالعربية تبدأ من الأسفل
                .environment(\.layoutDirection, .leftToRight)
            content()
        }
        .padding(width / 2)
    }
}

/// عدد فوق اسمه (عدد اليوم، والدورات): الاسم عنوان للعدد، فلا يلزمه أن يوافقه في الجمع («3 кругов» و «3 دورة»)،
/// ويُعرض بعرضه كاملاً في كل اللغات
struct LiveStat: View {
    let value: String
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: value)
                .font(Mizan.rounded(12.5, .semibold))
                .monospacedDigit()
                .foregroundColor(Mizan.text2)
            Text(verbatim: label)
                .font(Mizan.rounded(9.5, .medium))
                .foregroundColor(Mizan.text3)
        }
        .lineLimit(1)
        .fixedSize()
    }
}

/// العدد داخل الحلقة: أرقام لاتينية كما في شاشة المسبحة
struct CountLabel: View {
    let count: String
    var caption: String? = nil
    var size: CGFloat
    let color: Color

    var body: some View {
        VStack(spacing: 0) {
            Text(verbatim: count)
                .font(Mizan.rounded(size, .bold))
                .monospacedDigit()
                .foregroundColor(color)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
            if let caption = caption, !caption.isEmpty {
                Text(verbatim: caption)
                    .font(Mizan.rounded(max(9, size * 0.3), .medium))
                    .foregroundColor(Mizan.text2)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
        }
        .environment(\.layoutDirection, .leftToRight)
        .padding(.horizontal, 4)
    }
}

/// زر صغير شفاف بأيقونة ونص (تصفير، إنهاء، التالي)
@available(iOS 17.0, *)
struct LiveChipButton<I: AppIntent>: View {
    let intent: I
    let symbol: String
    let title: String
    /// الرمز وحده (حين لا يتسع الاسم)، والاسم لقارئ الشاشة
    var iconOnly = false

    var body: some View {
        Button(intent: intent) {
            HStack(spacing: 4) {
                Image(systemName: symbol).font(.system(size: 11, weight: .semibold))
                if !iconOnly {
                    Text(verbatim: title).font(Mizan.rounded(12, .semibold)).lineLimit(1)
                }
            }
            .foregroundColor(Mizan.text)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Capsule().fill(Color.white.opacity(0.14)))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(verbatim: title))
    }
}

/// الزر الكبير: حلقة التقدّم وفيها العدد، ولمسها يعدّ
@available(iOS 17.0, *)
struct CounterButton<I: AppIntent>: View {
    let intent: I
    let progress: Double
    let count: String
    let caption: String?
    let gold: Color
    let size: CGFloat
    var line: CGFloat = 6

    var ring: some View {
        ProgressRing(progress: progress, color: gold, width: line) {
            CountLabel(count: count, caption: caption, size: size * 0.3, color: Mizan.text)
        }
        .frame(width: size, height: size)
        .background(Circle().fill(Color.white.opacity(0.08)).padding(line))
    }

    var body: some View {
        Button(intent: intent) { ring }.buttonStyle(.plain)
    }
}

/// زر «+» الذهبي في الجزيرة الموسّعة
@available(iOS 17.0, *)
struct PlusButton<I: AppIntent>: View {
    let intent: I
    let gold: Color
    var size: CGFloat = 50

    var body: some View {
        let face = ZStack {
            Circle().fill(gold)
            Image(systemName: "plus").font(.system(size: size * 0.42, weight: .bold)).foregroundColor(.black.opacity(0.78))
        }
        .frame(width: size, height: size)
        Button(intent: intent) { face }.buttonStyle(.plain)
    }
}

// ---------- المسبحة ----------

@available(iOS 17.0, *)
struct TasbihActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TasbihActivityAttributes.self) { context in
            TasbihLockView(state: context.state)
                .activityBackgroundTint(Color(hex: context.state.hero2))
                .activitySystemActionForegroundColor(Color(hex: context.state.gold))
                .widgetURL(Deep.url("tasbih"))
        } dynamicIsland: { context in
            let s = context.state
            let gold = Color(hex: s.gold)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    ProgressRing(progress: s.progress, color: gold, width: 4) {
                        CountLabel(count: s.countText, size: 17, color: Mizan.text)
                    }
                    .frame(width: 56, height: 56)
                    .padding(.leading, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    PlusButton(intent: TasbihCountIntent(), gold: gold, size: 52)
                        .padding(.trailing, 4)
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(spacing: 2) {
                        Text(verbatim: s.text)
                            .font(Mizan.quran(18))
                            .foregroundColor(Mizan.text)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Text(verbatim: s.mode.isEmpty ? s.goalText : "\(s.goalText) · \(s.mode)")
                            .font(Mizan.rounded(11, .medium))
                            .foregroundColor(gold)
                            .lineLimit(1)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack(spacing: 8) {
                        HStack(spacing: 12) {
                            LiveStat(value: s.today, label: s.todayLabel)
                            LiveStat(value: s.rounds, label: s.roundsLabel)
                        }
                        Spacer(minLength: 4)
                        // الزرّان باسميهما، وبرمزيهما وحدهما إن لم يتسع لهما (الجزيرة أضيق في الشاشات الصغيرة)
                        ViewThatFits(in: .horizontal) {
                            HStack(spacing: 6) {
                                LiveChipButton(intent: TasbihResetIntent(), symbol: "arrow.counterclockwise", title: s.reset)
                                LiveChipButton(intent: TasbihEndIntent(), symbol: "xmark", title: s.end)
                            }
                            .fixedSize()
                            HStack(spacing: 6) {
                                LiveChipButton(intent: TasbihResetIntent(), symbol: "arrow.counterclockwise", title: s.reset, iconOnly: true)
                                LiveChipButton(intent: TasbihEndIntent(), symbol: "xmark", title: s.end, iconOnly: true)
                            }
                            .fixedSize()
                        }
                        .layoutPriority(1)
                    }
                    .padding(.horizontal, 4)
                    .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                }
            } compactLeading: {
                ProgressRing(progress: s.progress, color: gold, width: 3) { EmptyView() }
                    .frame(width: 20, height: 20)
            } compactTrailing: {
                Text(verbatim: s.countText)
                    .font(Mizan.rounded(14, .semibold))
                    .monospacedDigit()
                    .foregroundColor(gold)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .frame(maxWidth: 46)
            } minimal: {
                ProgressRing(progress: s.progress, color: gold, width: 2.5) {
                    Text(verbatim: s.countText)
                        .font(Mizan.rounded(9, .bold))
                        .monospacedDigit()
                        .foregroundColor(Mizan.text)
                        .minimumScaleFactor(0.5)
                }
                .frame(width: 22, height: 22)
            }
            .widgetURL(Deep.url("tasbih"))
            .keylineTint(gold)
        }
    }
}

/// شاشة القفل: الذكر بخط المصحف ومعناه، وعدد اليوم، والحلقة الكبيرة التي يعدّ لمسها، وزرّا التصفير والإنهاء
@available(iOS 17.0, *)
struct TasbihLockView: View {
    let state: TasbihLiveState

    var body: some View {
        let gold = Color(hex: state.gold)
        // أقصى ارتفاع للنشاط على شاشة القفل 160 نقطة (وسطر خط المصحف نحو مرتين ونصف حجمه): الذكر سطر واحد
        // يصغر خطه ليتسع (لا حول ولا قوة إلا بالله على شاشة صغيرة)، وتمام المائة الطويل سطران بلا معناه
        let long = state.text.count > 40
        HStack(alignment: .center, spacing: 14) {
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 6) {
                    StarGlyph().fill(gold).frame(width: 11, height: 11)
                    Text(verbatim: state.mode.isEmpty ? state.title : "\(state.title) · \(state.mode)")
                        .font(Mizan.rounded(13, .semibold))
                        .foregroundColor(gold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                Text(verbatim: state.text)
                    .font(Mizan.quran(long ? 15 : 22))
                    .foregroundColor(Mizan.text)
                    .lineLimit(long ? 2 : 1)
                    .minimumScaleFactor(long ? 0.7 : 0.6)
                    .fixedSize(horizontal: false, vertical: true)
                if !long && !state.meaning.isEmpty {
                    Text(verbatim: state.meaning)
                        .font(Mizan.rounded(12))
                        .foregroundColor(Mizan.text2)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                HStack(spacing: 6) {
                    LiveStat(value: state.today, label: state.todayLabel)
                    Spacer(minLength: 4)
                    // الزرّان باسميهما، وبرمزيهما وحدهما إن لم يتسع لهما (الروسية والألمانية على شاشة صغيرة)
                    ViewThatFits(in: .horizontal) {
                        HStack(spacing: 6) {
                            LiveChipButton(intent: TasbihResetIntent(), symbol: "arrow.counterclockwise", title: state.reset)
                            LiveChipButton(intent: TasbihEndIntent(), symbol: "xmark", title: state.end)
                        }
                        .fixedSize()
                        HStack(spacing: 6) {
                            LiveChipButton(intent: TasbihResetIntent(), symbol: "arrow.counterclockwise", title: state.reset, iconOnly: true)
                            LiveChipButton(intent: TasbihEndIntent(), symbol: "xmark", title: state.end, iconOnly: true)
                        }
                        .fixedSize()
                    }
                    // يُعرض عليه كل ما يبقى بعد عدد اليوم (وإلا اقترح عليه HStack أقل مما يلزم فاختار الرموز وحدها)
                    .layoutPriority(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            CounterButton(intent: TasbihCountIntent(), progress: state.progress, count: state.countText,
                          caption: state.goalText, gold: gold, size: 96, line: 7)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(alignment: .topTrailing) { LiveOrnament(gold: gold) }
        .environment(\.layoutDirection, state.rtl ? .rightToLeft : .leftToRight)
    }
}

// ---------- الأذكار ----------

@available(iOS 17.0, *)
struct AdhkarActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: AdhkarActivityAttributes.self) { context in
            AdhkarLockView(state: context.state)
                .activityBackgroundTint(Color(hex: context.state.hero2))
                .activitySystemActionForegroundColor(Color(hex: context.state.gold))
                .widgetURL(Deep.url("adhkar", section: context.state.section))
        } dynamicIsland: { context in
            let s = context.state
            let gold = Color(hex: s.gold)
            let symbol = s.section == "morning" ? "sunrise.fill" : "sunset.fill"
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    ProgressRing(progress: s.done ? 1 : Double(s.count) / Double(max(s.goal, 1)), color: gold, width: 4) {
                        if s.done {
                            Image(systemName: "checkmark").font(.system(size: 18, weight: .bold)).foregroundColor(gold)
                        } else {
                            CountLabel(count: s.countText, size: 14, color: Mizan.text)
                        }
                    }
                    .frame(width: 56, height: 56)
                    .padding(.leading, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    if !s.done {
                        PlusButton(intent: AdhkarCountIntent(), gold: gold, size: 52)
                            .padding(.trailing, 4)
                    }
                }
                DynamicIslandExpandedRegion(.center) {
                    Label {
                        Text(verbatim: "\(s.title) · \(s.position)").lineLimit(1).minimumScaleFactor(0.7)
                    } icon: {
                        Image(systemName: symbol)
                    }
                    .font(Mizan.rounded(12, .semibold))
                    .foregroundColor(gold)
                    .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(spacing: 6) {
                        if s.done {
                            Text(verbatim: s.doneText)
                                .font(Mizan.rounded(15, .semibold))
                                .foregroundColor(Mizan.text)
                                .lineLimit(2)
                        } else {
                            Text(verbatim: s.text)
                                .font(Mizan.quran(16))
                                .foregroundColor(Mizan.text)
                                .multilineTextAlignment(.center)
                                .lineLimit(2)
                                .minimumScaleFactor(0.7)
                                .environment(\.layoutDirection, .rightToLeft)
                        }
                        HStack(spacing: 8) {
                            Spacer(minLength: 0)
                            if s.done {
                                LiveChipButton(intent: AdhkarRestartIntent(), symbol: "arrow.counterclockwise", title: s.again ?? s.next)
                            } else {
                                // السهم بجهة اللغة صراحةً (لا يُقلب وحده في الجزيرة)
                                LiveChipButton(intent: AdhkarSkipIntent(), symbol: s.rtl ? "chevron.left" : "chevron.right", title: s.next)
                            }
                            LiveChipButton(intent: AdhkarEndIntent(), symbol: "xmark", title: s.end)
                            Spacer(minLength: 0)
                        }
                        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                    }
                    .padding(.horizontal, 4)
                }
            } compactLeading: {
                Image(systemName: symbol).foregroundColor(gold)
            } compactTrailing: {
                Group {
                    if s.done {
                        Image(systemName: "checkmark").foregroundColor(gold)
                    } else {
                        Text(verbatim: s.countText)
                            .font(Mizan.rounded(14, .semibold))
                            .monospacedDigit()
                            .foregroundColor(gold)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                    }
                }
                .frame(maxWidth: 46)
            } minimal: {
                ProgressRing(progress: s.progress, color: gold, width: 2.5) {
                    Image(systemName: symbol).font(.system(size: 9, weight: .semibold)).foregroundColor(gold)
                }
                .frame(width: 22, height: 22)
            }
            .widgetURL(Deep.url("adhkar", section: s.section))
            .keylineTint(gold)
        }
    }
}

/// شاشة القفل: القسم وموضع الذكر، والذكر بخط المصحف ومعناه، وتقدّم القسم، وحلقة عدد الذكر التي يعدّ لمسها
@available(iOS 17.0, *)
struct AdhkarLockView: View {
    let state: AdhkarLiveState

    var body: some View {
        let gold = Color(hex: state.gold)
        let symbol = state.section == "morning" ? "sunrise.fill" : "sunset.fill"
        // أقصى ارتفاع للنشاط على شاشة القفل 160 نقطة: الذكر سطران، وتقدّم القسم في موضع الذكر («5/23»)،
        // ومع سطر المعنى (غير العربية) يصغر خط الذكر والمسافات قليلاً فيبقى دونه: 156 نقطة بأطول ذكر
        let meaning = !state.done && !state.meaning.isEmpty
        HStack(alignment: .center, spacing: 14) {
            VStack(alignment: .leading, spacing: meaning ? 4 : 5) {
                HStack(spacing: 6) {
                    Image(systemName: symbol).font(.system(size: 11, weight: .semibold))
                    Text(verbatim: state.title)
                        .font(Mizan.rounded(13, .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    Spacer(minLength: 4)
                    Text(verbatim: state.position)
                        .font(Mizan.rounded(12, .medium))
                        .monospacedDigit()
                        .foregroundColor(Mizan.text3)
                        .environment(\.layoutDirection, .leftToRight)
                }
                .foregroundColor(gold)
                if state.done {
                    Text(verbatim: state.doneText)
                        .font(Mizan.rounded(17, .semibold))
                        .foregroundColor(Mizan.text)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)
                } else {
                    Text(verbatim: state.text)
                        .font(Mizan.quran(meaning ? 14 : 15))
                        .foregroundColor(Mizan.text)
                        .lineLimit(2)
                        .minimumScaleFactor(0.7)
                        .fixedSize(horizontal: false, vertical: true)
                    if meaning {
                        Text(verbatim: state.meaning)
                            .font(Mizan.rounded(11))
                            .foregroundColor(Mizan.text2)
                            .lineLimit(1)
                    }
                }
                HStack(spacing: 6) {
                    if state.done {
                        LiveChipButton(intent: AdhkarRestartIntent(), symbol: "arrow.counterclockwise", title: state.again ?? state.next)
                    } else {
                        LiveChipButton(intent: AdhkarSkipIntent(), symbol: state.rtl ? "chevron.left" : "chevron.right", title: state.next)
                    }
                    LiveChipButton(intent: AdhkarEndIntent(), symbol: "xmark", title: state.end)
                    Spacer(minLength: 0)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if state.done {
                ZStack {
                    Circle().fill(gold.opacity(0.18))
                    Image(systemName: "checkmark").font(.system(size: 28, weight: .bold)).foregroundColor(gold)
                }
                .frame(width: 76, height: 76)
            } else {
                CounterButton(intent: AdhkarCountIntent(), progress: Double(state.count) / Double(max(state.goal, 1)),
                              count: state.countText, caption: nil, gold: gold, size: 76, line: 6)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, meaning ? 10 : 12)
        .background(alignment: .topTrailing) { LiveOrnament(gold: gold) }
        .environment(\.layoutDirection, state.rtl ? .rightToLeft : .leftToRight)
    }
}
