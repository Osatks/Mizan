import SwiftUI
import UIKit
import Shared

/// تبويب الأدوات بواجهة النظام (SwiftUI) بطراز الرئيسية: ترويسة بتدرج لون التطبيق ونجمته، وشبكة بطاقات الأدوات
/// (عمودان على الهاتف، وثلاثة وأربعة على الأوسع)، لكل أداة أيقونتها واسمها ووصفها — كما في ToolsScreen (HomeScreen.kt).
enum NativeToolsScreen {
    static func install() {
        NativeTools.shared.attach { session in ToolsHostController(session: session) }
    }
}

final class ToolsHostController: UIHostingController<ToolsRoot> {
    init(session: NativeToolsSession) {
        let model = ToolsModel(session: session)
        super.init(rootView: ToolsRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

struct ToolsState: Decodable {
    struct Tool: Decodable, Identifiable { let key, name, desc: String; var id: String { key } }
    let rtl, dark: Bool
    let top, bottom, start, end, width: Double
    let title, tagline: String
    let tools: [Tool]
    let c: HomeState.Colors

    static func decode(_ json: String) -> ToolsState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(ToolsState.self, from: data)
    }
}

final class ToolsModel: ObservableObject {
    let session: NativeToolsSession
    @Published private(set) var state: ToolsState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeToolsSession) {
        self.session = session
        state = ToolsState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanToolsState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = ToolsState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ key: String) { session.open(key: key) }
}

/// أيقونات الأدوات (TOOL_ICONS في HomeScreen.kt) بأيقونات النظام
private func toolIcon(_ key: String) -> String {
    switch key {
    case "zakat": return "wallet.pass.fill"
    case "inheritance": return "person.3.fill"
    case "prayer": return "clock.fill"
    case "qibla": return "safari.fill"
    case "adhkar": return "sparkles"
    case "hijri": return "calendar"
    case "events": return "calendar.badge.clock"
    case "tasbih": return "hand.tap.fill"
    case "fitr": return "leaf.fill"
    // رمز الكعكة من iOS 16، وقبله هدية (الرمز غير الموجود لا يُرسم شيئاً)
    case "age": if #available(iOS 16.0, *) { return "birthday.cake.fill" } else { return "gift.fill" }
    case "fidya": return "hands.sparkles.fill"
    default: return "square.grid.2x2.fill"
    }
}

struct ToolsRoot: View {
    @ObservedObject var model: ToolsModel
    var body: some View {
        Group {
            if let s = model.state { ToolsView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct ToolsHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

/// الشريط الزجاجي العلوي باسم التبويب حين تمر الترويسة تحته (GlassTopBar)
private struct ToolsGlassBar: View {
    let title: String
    let top: CGFloat
    let p: HomePalette
    let amount: Double
    var body: some View {
        Text(title)
            .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
            .frame(maxWidth: .infinity).frame(height: 52)
            .padding(.top, top)
            .background(.ultraThinMaterial)
            .overlay(Rectangle().fill(p.outlineVariant.opacity(0.8)).frame(height: 1), alignment: .bottom)
            .opacity(amount)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }
}

struct ToolsView: View {
    let s: ToolsState
    let p: HomePalette
    let model: ToolsModel
    /// شفافية الشريط الزجاجي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته
    @State private var glass: Double = 0

    private var columns: Int { s.width >= 840 ? 4 : (s.width >= 560 ? 3 : 2) }

    /// تُكتب الشفافية حين تتغير فقط، فلا يُعاد بناء الصفحة مع كل إطار تمرير (كما في الرئيسية)
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
                    header
                        .background(GeometryReader { g in
                            Color.clear.preference(key: ToolsHeaderBottomKey.self, value: g.frame(in: .named("tools")).maxY)
                        })
                    VStack(spacing: 12) {
                        ForEach(Array(stride(from: 0, to: s.tools.count, by: columns)), id: \.self) { i in
                            HStack(spacing: 12) {
                                ForEach(s.tools[i..<min(i + columns, s.tools.count)]) { t in
                                    ToolCard(t: t, p: p) { model.open(t.key) }
                                }
                                ForEach(0..<(columns - min(columns, s.tools.count - i)), id: \.self) { _ in
                                    Color.clear.frame(maxWidth: .infinity)
                                }
                            }
                            .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "tools")
            .onPreferenceChange(ToolsHeaderBottomKey.self) { track($0) }
            ToolsGlassBar(title: s.title, top: CGFloat(s.top), p: p, amount: glass)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "square.grid.2x2.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
            VStack(alignment: .leading, spacing: 0) {
                Text(s.title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(1)
                Text(s.tagline).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 18)
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

private struct ToolCard: View {
    let t: ToolsState.Tool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: toolIcon(t.key))
                    .font(.system(size: 19, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 46, height: 46)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.16)))
                Text(t.name).font(sys(16, .bold)).foregroundColor(p.onSurface).lineLimit(2).padding(.top, 10)
                Text(t.desc).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(3).padding(.top, 2)
                Spacer(minLength: 0)
            }
            .padding(16)
            .frame(maxWidth: .infinity, minHeight: 150, maxHeight: .infinity, alignment: .topLeading)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
        }
        .buttonStyle(PressScale())
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
