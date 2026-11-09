import SwiftUI
import UIKit
import Shared

/// البحث الشامل بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): شريط مثبّت على تدرج لون التطبيق ونجمته،
/// فيه زر الرجوع وحقل بحث من النظام يُركَّز عليه عند الظهور، ثم النتائج مجمّعة بعناوين مجموعاتها في بطاقات بيضاء
/// (والجزء المطابق من العنوان بارز، ونص الآيات بخط المصحف) — كما في SearchScreen.kt.
/// النتائج كلها تُحسب في Kotlin (NativeSearch.ios.kt) وتصل مع الإشعار MizanSearchState؛ وهنا الحقل والرسم فقط.
enum NativeSearchScreen {
    static func install() {
        NativeSearch.shared.attach { session in SearchPageHostController(session: session) }
    }
}

final class SearchPageHostController: UIHostingController<SearchPageRoot> {
    init(session: NativeSearchSession) {
        let model = SearchPageModel(session: session)
        super.init(rootView: SearchPageRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin، ولوحة المفاتيح وحدها من النظام: تبقى النتائج فوقها
        if #available(iOS 16.4, *) { safeAreaRegions = .keyboard }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct SearchPageState: Decodable {
    struct Hit: Decodable, Identifiable {
        let id, icon, title: String
        /// قبل الجزء المطابق، وهو، وبعده (إن طابق العنوان حرفياً)
        let parts: [String]?
        let sub: String
        let quran: Bool
    }
    struct Section: Decodable, Identifiable { let title: String; let hits: [Hit]; var id: String { title } }

    let rtl, dark, blank: Bool
    let top, bottom, start, end: Double
    let back, hint, clear, query, emptyText, blankHint: String
    let groups: [Section]
    let c: HomeState.Colors

    static func decode(_ json: String) -> SearchPageState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(SearchPageState.self, from: data)
    }
}

final class SearchPageModel: ObservableObject {
    let session: NativeSearchSession
    @Published private(set) var state: SearchPageState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeSearchSession) {
        self.session = session
        state = SearchPageState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanSearchState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = SearchPageState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func query(_ text: String) { session.query(text: text) }
    func pick(_ id: String) { session.pick(id: id) }
    func back() { session.back() }
}

/// أيقونات النتائج بأيقونات النظام (TOOL_ICONS وأيقونات SearchScreen)
private func searchPageSymbol(_ key: String) -> String {
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
    case "age":
        if #available(iOS 16.0, *) { return "birthday.cake.fill" } else { return "gift.fill" }
    case "fidya": return "hands.sparkles.fill"
    case "quran": return "book.fill"
    case "time": return "clock"
    case "city": return "building.2.fill"
    case "event": return "calendar"
    case "dhikr": return "hand.tap.fill"
    case "language": return "character.bubble"
    case "theme": return "moon.fill"
    case "adhan": return "bell.badge.fill"
    case "method": return "slider.horizontal.3"
    default: return "magnifyingglass"
    }
}

private extension View {
    /// سحب النتائج يُنزل لوحة المفاتيح (iOS 16)
    @ViewBuilder func searchPageDismissKeyboardOnScroll() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.immediately) } else { self }
    }
}

// MARK: - الشاشة

struct SearchPageRoot: View {
    @ObservedObject var model: SearchPageModel
    var body: some View {
        Group {
            if let s = model.state { SearchPageView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea(.container, edges: .all)
    }
}

struct SearchPageView: View {
    let s: SearchPageState
    let p: HomePalette
    let model: SearchPageModel
    @State private var text: String
    @FocusState private var focused: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(s: SearchPageState, p: HomePalette, model: SearchPageModel) {
        self.s = s; self.p = p; self.model = model
        _text = State(initialValue: s.query)
    }

    private var ids: [String] { s.groups.flatMap { g in g.hits.map { $0.id } } }

    var body: some View {
        VStack(spacing: 0) {
            SearchPageBar(s: s, p: p, text: $text, focused: $focused, onBack: { focused = false; model.back() })
            ScrollView(.vertical, showsIndicators: false) {
                results
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                    .padding(.top, 4)
                    .padding(.bottom, CGFloat(s.bottom))
            }
            .searchPageDismissKeyboardOnScroll()
        }
        .background(p.background.ignoresSafeArea())
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
        .onChange(of: text) { model.query($0) }
        .onAppear {
            // الحقل جاهز للكتابة بعد أن تستقر الصفحة
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { focused = true }
        }
    }

    /// لا نتائج (أو حقل فارغ)، أو تلميح البحث ثم المجموعات بالترتيب الذي حسبه Kotlin
    @ViewBuilder private var results: some View {
        VStack(alignment: .leading, spacing: 0) {
            if s.groups.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 50, weight: .regular)).foregroundColor(p.onSurfaceVariant.opacity(0.55))
                        .accessibilityHidden(true)
                    Text(s.emptyText).font(sys(16)).foregroundColor(p.onSurfaceVariant).multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(32)
                .transition(.opacity)
            } else {
                if s.blank {
                    Text(s.blankHint).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                        .padding(EdgeInsets(top: 4, leading: 4, bottom: 12, trailing: 4))
                }
                ForEach(s.groups) { g in
                    Text(g.title).font(sys(14, .bold)).foregroundColor(p.primary)
                        .padding(.leading, 6).padding(.top, 14).padding(.bottom, 6)
                        .accessibilityAddTraits(.isHeader)
                    ForEach(g.hits) { h in
                        SearchPageRow(h: h, p: p) { focused = false; model.pick(h.id) }
                            .padding(.vertical, 3)
                    }
                }
            }
        }
        .animation(reduceMotion ? nil : .easeOut(duration: 0.2), value: ids)
    }
}

// MARK: الشريط

/// الشريط المثبّت: زر الرجوع وحقل البحث في بطاقة بيضاء، على تدرج الترويسة ونجمتها
private struct SearchPageBar: View {
    let s: SearchPageState
    let p: HomePalette
    @Binding var text: String
    var focused: FocusState<Bool>.Binding
    let onBack: () -> Void
    var body: some View {
        HStack(spacing: 8) {
            Button(action: onBack) {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(s.back)
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                    .accessibilityHidden(true)
                TextField(s.hint, text: $text)
                    .font(sys(17))
                    .foregroundColor(p.onSurface)
                    .tint(p.primary)
                    .disableAutocorrection(true)
                    .textInputAutocapitalization(.never)
                    .submitLabel(.search)
                    .focused(focused)
                    .onSubmit { focused.wrappedValue = false }
                if !text.isEmpty {
                    Button { text = ""; focused.wrappedValue = true } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 18)).foregroundColor(p.onSurfaceVariant)
                            .frame(width: 32, height: 32)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(s.clear)
                }
            }
            .padding(.leading, 14).padding(.trailing, 8)
            .frame(minHeight: 50)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous).fill(p.surface)
                    .shadow(color: .black.opacity(0.08), radius: 4, y: 1)
            )
        }
        .padding(.leading, CGFloat(max(s.start - 4, 12)))
        .padding(.trailing, CGFloat(s.end))
        .padding(.top, CGFloat(s.top) + 8)
        .padding(.bottom, 10)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: s.rtl ? .topTrailing : .topLeading) {
                LinearGradient(colors: [p.primaryContainer, p.primaryContainer.opacity(0.55), p.background], startPoint: .top, endPoint: .bottom)
                StarEmblem(color: p.primary.opacity(0.07), radius: 120, stroke: 1.5)
                    .offset(x: s.rtl ? 170 : -170, y: -150)
            }
            .environment(\.layoutDirection, .leftToRight)
            .clipped()
            .allowsHitTesting(false)
        )
    }
}

// MARK: النتائج

/// نتيجة في بطاقة بيضاء: أيقونتها في مربع بلون التطبيق، والعنوان (والجزء المطابق بارز)، والوصف أو نص الآية بخط المصحف
private struct SearchPageRow: View {
    let h: SearchPageState.Hit
    let p: HomePalette
    let action: () -> Void

    private var title: Text {
        if let parts = h.parts, parts.count == 3 {
            return Text(parts[0]) + Text(parts[1]).fontWeight(.heavy).foregroundColor(p.primary) + Text(parts[2])
        }
        return Text(h.title)
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: searchPageSymbol(h.icon))
                    .font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 40, height: 40)
                    .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(p.primaryContainer))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    title.font(sys(16, .semibold)).foregroundColor(p.onSurface).lineLimit(1)
                    if !h.sub.isEmpty {
                        if h.quran {
                            Text(h.sub).font(quranFont(18)).foregroundColor(p.onSurface)
                                .lineLimit(2).multilineTextAlignment(.leading)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .environment(\.layoutDirection, .rightToLeft)
                        } else {
                            Text(h.sub).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                                .lineLimit(2).multilineTextAlignment(.leading)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 14).padding(.vertical, 12)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.surface))
            .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}
