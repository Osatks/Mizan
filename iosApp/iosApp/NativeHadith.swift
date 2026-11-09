import SwiftUI
import UIKit
import Shared

/// حديث اليوم بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته تحت شريط مثبّت
/// فيه زر الرجوع (والعنوان يظهر في زجاجه عند التمرير)، ثم نص الحديث في بطاقة بحدّ ذهبي ودرجته وتخريجه، وترجمته، وشرحه وفوائده،
/// ومصدره ورابطه وزر مشاركته صورةَ قصة (الورقة من Compose)، وسهما اليوم السابق والتالي — كما في HadithScreen.kt.
/// الحالة كلها من Kotlin (NativeHadith.ios.kt) مع الإشعار MizanHadithState، وهنا الرسم فقط.
enum NativeHadithScreen {
    static func install() {
        NativeHadith.shared.attach { session in HadithPageHostController(session: session) }
    }
}

final class HadithPageHostController: UIHostingController<HadithPageRoot> {
    init(session: NativeHadithSession) {
        let model = HadithPageModel(session: session)
        super.init(rootView: HadithPageRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct HadithPageState: Decodable {
    struct Item: Decodable {
        let id, ar: String
        let tr: String?
        let grade: String
        /// الشرح والفوائد بالعربية (لا ترجمة)
        let arabic: Bool
        let expl: String
        let benefits: [String]
        let sourceSub: String
    }

    let rtl, dark, canNext: Bool
    let top, bottom, start, end: Double
    let back, title, subtitle, explanation, benefits, share, source, prev, next: String
    let hadith: Item?
    let c: HomeState.Colors

    static func decode(_ json: String) -> HadithPageState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(HadithPageState.self, from: data)
    }
}

final class HadithPageModel: ObservableObject {
    let session: NativeHadithSession
    @Published private(set) var state: HadithPageState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeHadithSession) {
        self.session = session
        state = HadithPageState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanHadithState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = HadithPageState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func previousDay() { UISelectionFeedbackGenerator().selectionChanged(); session.previousDay() }
    func nextDay() { UISelectionFeedbackGenerator().selectionChanged(); session.nextDay() }
    func share() { session.share() }
    func openSource() { session.openSource() }
    func back() { session.back() }
}

// MARK: - الشاشة

struct HadithPageRoot: View {
    @ObservedObject var model: HadithPageModel
    var body: some View {
        Group {
            if let s = model.state { HadithPageView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct HadithPageHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct HadithPageView: View {
    let s: HadithPageState
    let p: HomePalette
    let model: HadithPageModel
    @State private var glass: Double = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// زجاج الشريط العلوي وعنوانه يظهران حين تمر الترويسة تحته (يُكتب حين يتغير فقط)
    private func track(_ headerBottom: CGFloat) {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        let g = Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollViewReader { proxy in
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 0) {
                        HadithPageHeader(s: s, p: p)
                            .id("hadith-top")
                            .background(GeometryReader { g in
                                Color.clear.preference(key: HadithPageHeaderBottomKey.self, value: g.frame(in: .named("hadithPage")).maxY)
                            })
                        ZStack {
                            if let h = s.hadith {
                                HadithPageContent(s: s, h: h, p: p, model: model)
                                    .id(h.id)
                                    .transition(.opacity)
                            }
                        }
                        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: s.hadith?.id)
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                    }
                    .padding(.bottom, CGFloat(s.bottom))
                }
                .coordinateSpace(name: "hadithPage")
                .onPreferenceChange(HadithPageHeaderBottomKey.self) { track($0) }
                // حديث يوم آخر يبدأ من أعلاه
                .onChange(of: s.hadith?.id) { _ in
                    if reduceMotion { proxy.scrollTo("hadith-top", anchor: .top) } else {
                        withAnimation(.easeInOut(duration: 0.3)) { proxy.scrollTo("hadith-top", anchor: .top) }
                    }
                }
            }
            HadithPageBar(s: s, p: p, amount: glass, onBack: model.back)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

/// النص، والترجمة، والشرح، والفوائد، والمصدر، والأيام
private struct HadithPageContent: View {
    let s: HadithPageState
    let h: HadithPageState.Item
    let p: HomePalette
    let model: HadithPageModel
    var body: some View {
        VStack(spacing: 14) {
            HadithPageHero(h: h, p: p)
            if let tr = h.tr {
                HadithPageCard(title: nil, p: p) { HadithPageBody(text: tr, arabic: false, p: p) }
            }
            HadithPageCard(title: s.explanation, p: p) { HadithPageBody(text: h.expl, arabic: h.arabic, p: p) }
            if !h.benefits.isEmpty {
                HadithPageCard(title: s.benefits, p: p) {
                    VStack(alignment: .leading, spacing: 10) {
                        ForEach(Array(h.benefits.enumerated()), id: \.offset) { _, b in
                            HadithPageBenefit(text: b, arabic: h.arabic, p: p)
                        }
                    }
                }
            }
            HadithPageSource(s: s, h: h, p: p, model: model)
            HadithPageDays(s: s, p: p, model: model)
        }
    }
}

// MARK: الشريط والترويسة

/// الشريط العلوي المثبّت: زر الرجوع دائماً، وزجاج بالعنوان عند التمرير
private struct HadithPageBar: View {
    let s: HadithPageState
    let p: HomePalette
    let amount: Double
    let onBack: () -> Void
    var body: some View {
        ZStack {
            Text(s.title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(amount)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.backward")
                        .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 42, height: 42)
                        .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
                }
                .buttonStyle(PressScale(scale: 0.9))
                .accessibilityLabel(s.back)
                Spacer()
            }
            .padding(.leading, CGFloat(max(s.start - 4, 12)))
            .padding(.trailing, CGFloat(max(s.end - 4, 12)))
        }
        .frame(height: 52)
        .padding(.top, CGFloat(s.top))
        .background(
            Rectangle().fill(.ultraThinMaterial)
                .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
                .opacity(amount)
                .allowsHitTesting(false)
        )
    }
}

/// ترويسة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة الحديث والعنوان واليوم وتاريخه
private struct HadithPageHeader: View {
    let s: HadithPageState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "quote.opening")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
            VStack(alignment: .leading, spacing: 1) {
                Text(s.title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                    .modifier(NumericRoll(value: s.subtitle))
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 52 + 4)
        .padding(.bottom, 18)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: s.rtl ? .topTrailing : .topLeading) {
                GeometryReader { g in
                    VStack(spacing: 0) {
                        p.primaryContainer.frame(height: 800)
                        LinearGradient(colors: [p.primaryContainer, p.primaryContainer.opacity(0.55), p.background], startPoint: .top, endPoint: .bottom)
                            .frame(height: g.size.height)
                    }
                    .offset(y: -800)
                }
                StarEmblem(color: p.primary.opacity(0.07), radius: 150, stroke: 1.7)
                    .offset(x: s.rtl ? 220 : -220, y: -190)
            }
            .environment(\.layoutDirection, .leftToRight)
            .allowsHitTesting(false)
        )
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: البطاقات

/// نص الحديث العربي في بطاقة بتدرج لون التطبيق وحدّ ذهبي، وتحته الدرجة والتخريج في شارة ذهبية (HadithHero)
private struct HadithPageHero: View {
    let h: HadithPageState.Item
    let p: HomePalette
    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: "quote.opening")
                .font(.system(size: 24, weight: .bold)).foregroundColor(p.goldDeep)
                .accessibilityHidden(true)
            Text(h.ar)
                .font(sys(22, .medium)).foregroundColor(p.onSurface)
                .multilineTextAlignment(.center).lineSpacing(14)
                .frame(maxWidth: .infinity)
                .fixedSize(horizontal: false, vertical: true)
                .environment(\.layoutDirection, .rightToLeft)
                .textSelection(.enabled)
                .padding(.top, 8)
            if !h.grade.isEmpty {
                Text(h.grade)
                    .font(sys(13, .bold)).foregroundColor(p.goldDeep).multilineTextAlignment(.center)
                    .padding(.horizontal, 14).padding(.vertical, 6)
                    .background(Capsule().fill(p.gold.opacity(0.14)))
                    .padding(.top, 14)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .topTrailing) {
                p.surface
                LinearGradient(colors: [p.primaryContainer.opacity(0.55), p.surface], startPoint: .top, endPoint: .bottom)
                StarEmblem(color: p.gold.opacity(0.14), radius: 64, stroke: 1).offset(x: 34, y: -34)
            }
            .environment(\.layoutDirection, .leftToRight)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(p.gold.opacity(0.45), lineWidth: 1))
    }
}

/// بطاقة بيضاء بعنوان اختياري (SectionCard)
private struct HadithPageCard<Content: View>: View {
    let title: String?
    let p: HomePalette
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let t = title {
                Text(t).font(sys(15, .bold)).foregroundColor(p.primary)
                    .accessibilityAddTraits(.isHeader)
            }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

/// نص الترجمة أو الشرح؛ والعربي من اليمين بسطور أرحب (BodyText)
private struct HadithPageBody: View {
    let text: String
    let arabic: Bool
    let p: HomePalette
    @Environment(\.layoutDirection) private var direction
    var body: some View {
        Text(text)
            .font(sys(arabic ? 17 : 16)).foregroundColor(p.onSurface)
            .lineSpacing(arabic ? 11 : 7)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
            .textSelection(.enabled)
            .environment(\.layoutDirection, arabic ? .rightToLeft : direction)
    }
}

/// فائدة بنقطة ذهبية
private struct HadithPageBenefit: View {
    let text: String
    let arabic: Bool
    let p: HomePalette
    @Environment(\.layoutDirection) private var direction
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Circle().fill(p.gold).frame(width: 6, height: 6).padding(.top, 9)
                .accessibilityHidden(true)
            HadithPageBody(text: text, arabic: arabic, p: p)
        }
        .environment(\.layoutDirection, arabic ? .rightToLeft : direction)
    }
}

/// المصدر بلا تعديل ورابط الحديث في الموسوعة، وزر المشاركة صورةَ قصة (SourceCard)
private struct HadithPageSource: View {
    let s: HadithPageState
    let h: HadithPageState.Item
    let p: HomePalette
    let model: HadithPageModel
    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(s.source).font(sys(15, .bold)).foregroundColor(p.primary)
                    Text(h.sourceSub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                }
                .accessibilityElement(children: .combine)
                Spacer(minLength: 0)
                Button(action: model.openSource) {
                    Image(systemName: "arrow.up.forward.square")
                        .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(PressScale(scale: 0.9))
                .accessibilityLabel(s.source)
                .accessibilityAddTraits(.isLink)
            }
            Button(action: model.share) {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.up").font(.system(size: 16, weight: .semibold))
                        .accessibilityHidden(true)
                    Text(s.share).font(sys(15, .semibold))
                }
                .foregroundColor(p.onPrimaryContainer)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Capsule().fill(p.primaryContainer))
                .contentShape(Capsule())
            }
            .buttonStyle(PressScale())
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

/// اليوم السابق والتالي (لا يتجاوز اليوم)؛ الأسهم تنعكس وحدها من اليمين إلى اليسار
private struct HadithPageDays: View {
    let s: HadithPageState
    let p: HomePalette
    let model: HadithPageModel
    var body: some View {
        HStack {
            Button(action: model.previousDay) {
                HStack(spacing: 4) {
                    Image(systemName: "chevron.backward").font(.system(size: 14, weight: .bold)).accessibilityHidden(true)
                    Text(s.prev).font(sys(15, .semibold))
                }
                .foregroundColor(p.primary)
                .padding(.horizontal, 12).padding(.vertical, 10)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.94))
            Spacer(minLength: 8)
            Button(action: model.nextDay) {
                HStack(spacing: 4) {
                    Text(s.next).font(sys(15, .semibold))
                    Image(systemName: "chevron.forward").font(.system(size: 14, weight: .bold)).accessibilityHidden(true)
                }
                .foregroundColor(p.primary)
                .padding(.horizontal, 12).padding(.vertical, 10)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.94))
            .disabled(!s.canNext)
            .opacity(s.canNext ? 1 : 0.38)
        }
    }
}
