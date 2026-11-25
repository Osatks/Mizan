import SwiftUI
import UIKit
import Shared

/// المصادر والتراخيص بواجهة النظام (SwiftUI) بطراز الرئيسية: شريط علوي بزر الرجوع، وترويسة بتدرج لون التطبيق ونجمته،
/// وبطاقة لكل مصدر (نص المصحف وروابطه، ومصحف المدينة، والتفاسير، والتلاوات، وتسجيلات الأذان)، ونصّا الرخصتين كما هما
/// — كما في LicensesScreen.kt. النصوص كلها من Kotlin (NativeLicenses.ios.kt).
enum NativeLicensesScreen {
    static func install() {
        NativeLicenses.shared.attach { session in LicensesHostController(session: session) }
    }
}

final class LicensesHostController: UIHostingController<LicensesRoot> {
    init(session: NativeLicensesSession) {
        let model = LicensesModel(session: session)
        super.init(rootView: LicensesRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin (شريط الحالة) لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct LicensesState: Decodable {
    /// md: نص Markdown بروابط؛ mono: نص رخصة طويل يُعرض كما هو
    struct Section: Decodable { let title, body: String; let md, mono: Bool }

    let rtl, dark: Bool
    let top, bottom, start, end, lead, width: Double
    let back, title, subtitle: String
    let sections: [Section]
    let c: HomeState.Colors

    static func decode(_ json: String) -> LicensesState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(LicensesState.self, from: data)
    }
}

final class LicensesModel: ObservableObject {
    let session: NativeLicensesSession
    @Published private(set) var state: LicensesState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeLicensesSession) {
        self.session = session
        state = LicensesState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanLicensesState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = LicensesState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func back() { session.back() }
}

/// نص Markdown بروابطه (المصادر) بلون التطبيق
private func licensesLinked(_ md: String, _ color: Color) -> AttributedString {
    var a = (try? AttributedString(markdown: md, options: AttributedString.MarkdownParsingOptions(interpretedSyntax: .inlineOnlyPreservingWhitespace)))
        ?? AttributedString(md)
    for run in a.runs where run.link != nil {
        a[run.range].foregroundColor = color
    }
    return a
}

private struct LicensesHeaderKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

// MARK: - الشاشة

struct LicensesRoot: View {
    @ObservedObject var model: LicensesModel
    var body: some View {
        Group {
            if let s = model.state { LicensesView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct LicensesView: View {
    let s: LicensesState
    let p: HomePalette
    let model: LicensesModel
    /// شفافية الشريط العلوي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته (GlassTopBar)
    @State private var glass: Double = 0

    private func track(_ headerBottom: CGFloat) {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        let g = Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    LicensesHeader(s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: LicensesHeaderKey.self, value: g.frame(in: .named("licenses")).maxY)
                        })
                    LazyVStack(spacing: 14) {
                        ForEach(Array(s.sections.enumerated()), id: \.offset) { _, sec in
                            LicensesCard(sec: sec, p: p)
                        }
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "licenses")
            .onPreferenceChange(LicensesHeaderKey.self) { track($0) }
            LicensesTopBar(s: s, p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

/// بطاقة مصدر: العنوان، ثم النص (بروابطه، أو نص الرخصة كما هو ويمكن تحديده ونسخه)
private struct LicensesCard: View {
    let sec: LicensesState.Section
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(sec.title).font(sys(17, .bold)).foregroundColor(p.primary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Group {
                if sec.md {
                    Text(licensesLinked(sec.body, p.primary))
                } else {
                    Text(verbatim: sec.body)
                }
            }
            .font(sys(13)).foregroundColor(p.onSurfaceVariant)
            .lineSpacing(sec.mono ? 2 : 1)
            .fixedSize(horizontal: false, vertical: true)
            .textSelection(.enabled)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
    }
}

/// ترويسة الصفحة بطراز ترويسة الرئيسية تحت الشريط العلوي: تدرج من لون التطبيق الفاتح، ونجمة باهتة، والأيقونة والعنوان
private struct LicensesHeader: View {
    let s: LicensesState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(s.title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 56)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
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

/// الشريط العلوي الثابت: زر الرجوع دائماً، وزجاج وعنوان صغير حين تمر الترويسة تحته
private struct LicensesTopBar: View {
    let s: LicensesState
    let p: HomePalette
    let amount: Double
    let onBack: () -> Void

    var body: some View {
        let t = min(max((amount - 0.4) / 0.6, 0), 1)
        ZStack {
            Text(s.title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 60)
                .opacity(t)
                .offset(y: CGFloat(1 - t) * 6)
                .accessibilityHidden(true)
            HStack(spacing: 0) {
                Button(action: onBack) {
                    Image(systemName: "chevron.backward")
                        .font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 44, height: 44)
                        .background(
                            ZStack {
                                Circle().fill(p.surface.opacity(0.85 * (1 - amount)))
                                    .shadow(color: .black.opacity(0.08 * (1 - amount)), radius: 3, y: 1)
                                Circle().fill(p.onSurface.opacity(0.06 * amount))
                            }
                        )
                }
                .buttonStyle(PressScale(scale: 0.88))
                .accessibilityLabel(s.back)
                Spacer(minLength: 0)
            }
            .padding(.leading, 10)
        }
        .frame(height: 52)
        .padding(.leading, CGFloat(s.lead))
        .padding(.top, CGFloat(s.top))
        .frame(maxWidth: .infinity)
        .background(
            Rectangle().fill(.ultraThinMaterial)
                .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
                .opacity(amount)
                .allowsHitTesting(false)
        )
    }
}
