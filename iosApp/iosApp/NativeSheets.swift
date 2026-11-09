import SwiftUI
import UIKit
import Shared

/// أوراق النظام (UISheetPresentationController) بمحتوى SwiftUI بطراز الرئيسية: ورقة اللغة، واختيار المدينة.
/// Kotlin (ui/NativeSheet.kt) يعرضها ويغلقها ويرسل حالتها JSON، وكل لمسة تعود إليه (session.send)؛
/// المنطق كله والنصوص في Kotlin (LanguageSheet و CityPickerSheet في Sheets.kt).
enum NativeSheetsUI {
    static func install() {
        NativeSheets.shared.attach { session -> UIViewController? in
            switch session.kind {
            case "language":
                let model = SheetModel<LangSheetState>(session: session)
                return SheetHostController(model: model, root: AnyView(LangSheetView(model: model)))
            case "city":
                let model = SheetModel<CitySheetState>(session: session)
                return SheetHostController(model: model, root: AnyView(CitySheetView(model: model)))
            default:
                return nil
            }
        }
    }
}

/// ما تشترك فيه حالات الأوراق: اتجاه اللغة والمظهر وألوان التطبيق
protocol SheetStyled: Decodable {
    var rtl: Bool { get }
    var dark: Bool { get }
    var c: HomeState.Colors { get }
}

/// متحكم محتوى الورقة: مظهر التطبيق واتجاهه على ما يرسمه UIKit (شريط التنقل وحقل البحث)، وخلفيته لون التطبيق
final class SheetHostController: UIHostingController<AnyView> {
    init<S: SheetStyled>(model: SheetModel<S>, root: AnyView) {
        super.init(rootView: root)
        modalPresentationStyle = .pageSheet
        model.onStyle = { [weak self] dark, rtl, background in self?.restyle(dark: dark, rtl: rtl, background: background) }
        if let s = model.state { restyle(dark: s.dark, rtl: s.rtl, background: s.c.background) }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }

    private func restyle(dark: Bool, rtl: Bool, background: String) {
        applyAppStyle(dark: dark, rtl: rtl)
        view.backgroundColor = UIColor(HomePalette.color(background))
    }
}

/// حالة الورقة من Kotlin: تُقرأ عند كل MizanSheetState من جلستها
final class SheetModel<S: SheetStyled>: ObservableObject {
    let session: NativeSheetSession
    @Published private(set) var state: S? { didSet { if let s = state { onStyle?(s.dark, s.rtl, s.c.background) } } }
    var onStyle: ((Bool, Bool, String) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeSheetSession) {
        self.session = session
        state = SheetModel.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanSheetState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = SheetModel.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    static func decode(_ json: String) -> S? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(S.self, from: data)
    }

    func send(_ action: String, _ arg: String = "") { session.send(action: action, arg: arg) }
}

// MARK: - أجزاء مشتركة

/// بطاقة بيضاء مستديرة على خلفية التطبيق
struct SheetCard<Content: View>: View {
    let p: HomePalette
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) { content() }
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

/// خط رفيع بين صفوف البطاقة
struct SheetDivider: View {
    let p: HomePalette
    var body: some View {
        Rectangle().fill(p.outlineVariant.opacity(0.6)).frame(height: 0.5).padding(.leading, 16)
    }
}

/// زر الإغلاق في شريط الورقة (وفي iOS 26 يلبسه النظام زجاجه)
struct SheetCloseButton: View {
    let label: String
    let p: HomePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            if #available(iOS 26.0, *) {
                Image(systemName: "xmark").font(sys(15, .semibold))
            } else {
                Image(systemName: "xmark").font(sys(13, .bold)).foregroundColor(p.onSurfaceVariant)
                    .frame(width: 30, height: 30)
                    .background(Circle().fill(p.onSurfaceVariant.opacity(0.12)))
            }
        }
        .accessibilityLabel(label)
    }
}

/// حلقة تقدّم التنزيل
struct SheetProgressRing: View {
    let value: Double
    let color: Color
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle().stroke(color.opacity(0.2), lineWidth: 3)
            Circle().trim(from: 0, to: CGFloat(max(0.02, min(1, value))))
                .stroke(color, style: StrokeStyle(lineWidth: 3, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: 24, height: 24)
        .animation(reduceMotion ? nil : .easeOut(duration: 0.25), value: value)
    }
}

// MARK: - ورقة اللغة

struct LangSheetState: SheetStyled {
    struct Row: Decodable, Identifiable {
        let code, title: String
        let sub: String?
        let selected, removable: Bool
        var id: String { code }
    }
    struct Offer: Decodable, Identifiable {
        let code, title, sub: String
        let pct: Int?
        let pctText: String?
        let update: Bool
        var id: String { code }
    }
    let rtl, dark: Bool
    let c: HomeState.Colors
    let errorColor: String
    let title, close, more, hint: String
    let checking: Bool
    let failed: String?
    let retryLabel, updateLabel, downloadLabel, deleteLabel: String
    let rows: [Row]
    let offers: [Offer]
}

struct LangSheetView: View {
    @ObservedObject var model: SheetModel<LangSheetState>
    var body: some View {
        Group {
            if let s = model.state { LangSheetBody(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
    }
}

/// اللغات المثبتة (المختارة بعلامة، كل لغة باسمها الأصلي وتحته اسمها بلغة الواجهة)، ثم المتاحة للتنزيل وتقدّمها
struct LangSheetBody: View {
    let s: LangSheetState
    let p: HomePalette
    let model: SheetModel<LangSheetState>

    private var err: Color { HomePalette.color(s.errorColor) }

    var body: some View {
        NavigationView {
            ScrollView(.vertical) {
                VStack(alignment: .leading, spacing: 0) {
                    SheetCard(p: p) {
                        ForEach(Array(s.rows.enumerated()), id: \.element.id) { i, r in
                            if i > 0 { SheetDivider(p: p) }
                            row(r)
                        }
                    }
                    more.padding(.top, 26)
                    if !s.offers.isEmpty {
                        SheetCard(p: p) {
                            ForEach(Array(s.offers.enumerated()), id: \.element.id) { i, o in
                                if i > 0 { SheetDivider(p: p) }
                                offer(o)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 28)
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle(s.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 8) {
                        Image(systemName: "globe").font(sys(15, .semibold)).foregroundColor(p.primary).accessibilityHidden(true)
                        Text(s.title).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityAddTraits(.isHeader)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    SheetCloseButton(label: s.close, p: p) { model.send("close") }
                }
            }
        }
        .navigationViewStyle(.stack)
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private func row(_ r: LangSheetState.Row) -> some View {
        HStack(spacing: 4) {
            Button { model.send("pick", r.code) } label: {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(r.title).font(sys(17, .bold)).foregroundColor(p.onSurface)
                        if let sub = r.sub {
                            Text(sub).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                        }
                    }
                    Spacer(minLength: 0)
                    if r.selected {
                        Image(systemName: "checkmark.circle.fill").font(sys(22, .semibold)).foregroundColor(p.primary)
                            .accessibilityHidden(true)
                    }
                }
                .padding(.vertical, 12)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.98))
            .accessibilityAddTraits(r.selected ? .isSelected : [])
            if r.removable {
                Button { model.send("delete", r.code) } label: {
                    Image(systemName: "trash").font(sys(16)).foregroundColor(p.onSurfaceVariant)
                        .frame(width: 40, height: 40)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(s.deleteLabel)
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, r.removable ? 6 : 16)
        .background(r.selected ? p.primary.opacity(0.10) : Color.clear)
    }

    /// عنوان «لغات أخرى» وشرحه، ومؤشر الفحص، وإعادة المحاولة إن تعذّر
    private var more: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Image(systemName: "icloud.and.arrow.down").font(sys(16, .semibold)).foregroundColor(p.primary)
                    .accessibilityHidden(true)
                Text(s.more).font(sys(15, .bold)).foregroundColor(p.primary)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 0)
                if s.checking { ProgressView().controlSize(.small) }
            }
            Text(s.hint).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
            if let failed = s.failed {
                HStack(spacing: 8) {
                    Text(failed).font(sys(13)).foregroundColor(err)
                    Spacer(minLength: 0)
                    Button { model.send("retry") } label: {
                        Text(s.retryLabel).font(sys(15, .semibold)).foregroundColor(p.primary)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 4)
        .padding(.bottom, 10)
    }

    private func offer(_ o: LangSheetState.Offer) -> some View {
        let busy = (o.pct ?? -1) >= 0
        return HStack(spacing: 8) {
            Button { if !busy { model.send("download", o.code) } } label: {
                VStack(alignment: .leading, spacing: 2) {
                    Text(o.title).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    Text(o.sub).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 12)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.98))
            offerTrailing(o, busy: busy)
        }
        .padding(.leading, 16)
        .padding(.trailing, 8)
    }

    @ViewBuilder
    private func offerTrailing(_ o: LangSheetState.Offer, busy: Bool) -> some View {
        if busy {
            HStack(spacing: 8) {
                Text(o.pctText ?? "").font(sys(13, .semibold).monospacedDigit()).foregroundColor(p.primary)
                SheetProgressRing(value: Double(o.pct ?? 0) / 100, color: p.primary)
            }
            .padding(.trailing, 8)
            .accessibilityElement(children: .combine)
        } else if o.pct == -1 {
            Button { model.send("download", o.code) } label: {
                Text(s.retryLabel).font(sys(15, .semibold)).foregroundColor(err).padding(.horizontal, 8).padding(.vertical, 6)
            }
            .buttonStyle(.plain)
        } else if o.update {
            Button { model.send("download", o.code) } label: {
                Text(s.updateLabel).font(sys(15, .semibold)).foregroundColor(p.primary).padding(.horizontal, 8).padding(.vertical, 6)
            }
            .buttonStyle(.plain)
        } else {
            Button { model.send("download", o.code) } label: {
                Image(systemName: "arrow.down.circle").font(sys(22)).foregroundColor(p.primary)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(s.downloadLabel)
        }
    }
}

// MARK: - اختيار المدينة

struct CitySheetState: SheetStyled {
    struct Row: Decodable, Identifiable {
        let key, city, country: String
        let selected: Bool
        var id: String { key }
    }
    let rtl, dark: Bool
    let c: HomeState.Colors
    let errorColor: String
    let title, search, close, empty: String
    let rows: [Row]
}

struct CitySheetView: View {
    @ObservedObject var model: SheetModel<CitySheetState>
    /// نص البحث في حقل النظام؛ النتائج يحسبها Kotlin من قاعدة مدنه ويعيدها
    @State private var query = ""

    var body: some View {
        Group {
            if let s = model.state {
                CitySheetBody(s: s, p: HomePalette(s.c), model: model, query: Binding(
                    get: { query },
                    set: { v in
                        query = v
                        model.send("query", v)
                    }
                ))
            } else {
                Color.clear
            }
        }
    }
}

/// حقل بحث النظام في شريط الورقة، وتحته المدن (المدينة بلغة الواجهة ودولتها، والمختارة بعلامة)
struct CitySheetBody: View {
    let s: CitySheetState
    let p: HomePalette
    let model: SheetModel<CitySheetState>
    @Binding var query: String

    var body: some View {
        NavigationView {
            ScrollView(.vertical) {
                if s.rows.isEmpty {
                    Text(s.empty).font(sys(15)).foregroundColor(p.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(32)
                } else {
                    SheetCard(p: p) {
                        LazyVStack(spacing: 0) {
                            ForEach(Array(s.rows.enumerated()), id: \.element.id) { i, r in
                                if i > 0 { SheetDivider(p: p) }
                                row(r)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 28)
                }
            }
            .background(p.background.ignoresSafeArea())
            .navigationTitle(s.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    SheetCloseButton(label: s.close, p: p) { model.send("close") }
                }
            }
            .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: Text(s.search))
        }
        .navigationViewStyle(.stack)
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private func row(_ r: CitySheetState.Row) -> some View {
        Button { model.send("pick", r.key) } label: {
            HStack(spacing: 12) {
                Image(systemName: "building.2.fill").font(sys(16)).foregroundColor(p.primary)
                    .frame(width: 24)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(r.city).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    Text(r.country).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                }
                Spacer(minLength: 0)
                if r.selected {
                    Image(systemName: "checkmark").font(sys(16, .bold)).foregroundColor(p.primary)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(r.selected ? p.primary.opacity(0.10) : Color.clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.98))
        .accessibilityAddTraits(r.selected ? .isSelected : [])
    }
}
