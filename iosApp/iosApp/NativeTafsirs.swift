import SwiftUI
import UIKit
import Shared

/// مكتبة التفاسير بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته تحت شريط
/// مثبّت فيه زر الرجوع (والعنوان يظهر في زجاجه عند التمرير)، واختيار مقسّم بين «تنزيل» و«على الجهاز»، وبطاقات التفاسير
/// بشارة لغتها وحجمها وشريط تقدم تنزيلها، ومفتاح إظهار كل مصدر في ورقة التفسير وحذف المنزَّل — كما في TafsirLibraryScreen.kt.
/// الحالة كلها من Kotlin (NativeTafsirs.ios.kt) مع الإشعار MizanTafsirsState، وتتجدد مع تقدم كل تنزيل؛ التنزيل والحذف
/// والحفظ هناك، وهنا الرسم فقط واللمس يعود إليه (وتأكيد الحذف تنبيه النظام من Kotlin).
enum NativeTafsirsScreen {
    static func install() {
        NativeTafsirs.shared.attach { session in TafsirLibHostController(session: session) }
    }
}

final class TafsirLibHostController: UIHostingController<TafsirLibRoot> {
    init(session: NativeTafsirsSession) {
        let model = TafsirLibModel(session: session)
        super.init(rootView: TafsirLibRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin لا من المنطقة الآمنة لهذا المتحكم
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct TafsirLibState: Decodable {
    struct Option: Decodable, Identifiable { let key, name: String; var id: String { key } }
    /// تفسير قابل للتنزيل؛ state: ‏done أو running أو failed أو idle
    struct Download: Decodable, Identifiable {
        let slug, badge, name, sub, state, pct: String
        let progress: Double
        var id: String { slug }
    }
    /// مصدر على الجهاز؛ slug للمنزَّل وحده (يُحذف)
    struct Installed: Decodable, Identifiable {
        let id, badge, name, sub: String
        let slug: String?
        let shown: Bool
    }

    let rtl, dark: Bool
    let top, bottom, start, end, width: Double
    let back, title, subtitle, tab: String
    let tabs: [Option]
    let availableTitle, deviceTitle, note: String
    let download, retry, cancel, delete, failed, onDevice: String
    let downloads: [Download]
    let installed: [Installed]
    let error: String
    let c: HomeState.Colors

    static func decode(_ json: String) -> TafsirLibState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(TafsirLibState.self, from: data)
    }
}

final class TafsirLibModel: ObservableObject {
    let session: NativeTafsirsSession
    @Published private(set) var state: TafsirLibState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeTafsirsSession) {
        self.session = session
        state = TafsirLibState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanTafsirsState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = TafsirLibState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func tab(_ key: String) { session.tab(key: key) }
    func download(_ slug: String) { session.download(slug: slug) }
    func cancel(_ slug: String) { session.cancelDownload(slug: slug) }
    func remove(_ slug: String) { session.remove(slug: slug) }
    func visible(_ id: String, _ on: Bool) { session.visible(id: id, on: on) }
    func back() { session.back() }
}

// MARK: - الشاشة

struct TafsirLibRoot: View {
    @ObservedObject var model: TafsirLibModel
    var body: some View {
        Group {
            if let s = model.state { TafsirLibView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct TafsirLibHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct TafsirLibView: View {
    let s: TafsirLibState
    let p: HomePalette
    let model: TafsirLibModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// زجاج الشريط العلوي وعنوانه يظهران حين تمر الترويسة تحته
    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    TafsirLibHeader(s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: TafsirLibHeaderBottomKey.self, value: g.frame(in: .named("tafsirLib")).maxY)
                        })
                    content
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "tafsirLib")
            .onPreferenceChange(TafsirLibHeaderBottomKey.self) { headerBottom = $0 }
            TafsirLibBar(s: s, p: p, amount: glass, onBack: model.back)
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 10) {
            Picker(s.title, selection: Binding(get: { s.tab }, set: { v in
                if v != s.tab { UISelectionFeedbackGenerator().selectionChanged() }
                model.tab(v)
            })) {
                ForEach(s.tabs) { t in Text(t.name).tag(t.key) }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .padding(.bottom, 4)
            if s.tab == "get" {
                TafsirLibSectionTitle(text: s.availableTitle, p: p)
                ForEach(s.downloads) { r in TafsirLibDownloadCard(r: r, s: s, p: p, model: model) }
                TafsirLibNote(text: s.note, p: p).padding(.top, 4)
            } else {
                TafsirLibSectionTitle(text: s.deviceTitle, p: p)
                ForEach(s.installed) { src in TafsirLibInstalledCard(src: src, s: s, p: p, model: model) }
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.2), value: s.tab)
    }
}

// MARK: الشريط والترويسة

/// الشريط العلوي المثبّت: زر الرجوع دائماً، وزجاج بالعنوان عند التمرير
private struct TafsirLibBar: View {
    let s: TafsirLibState
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

/// ترويسة بطراز ترويسة الرئيسية: تدرج من لون التطبيق الفاتح، ونجمة باهتة، وأيقونة المكتبة والعنوان والوصف
private struct TafsirLibHeader: View {
    let s: TafsirLibState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "books.vertical.fill")
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 1) {
                Text(s.title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(s.subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(3)
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

private struct TafsirLibSectionTitle: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(spacing: 6) {
            Star8().fill(p.gold).frame(width: 11, height: 11).accessibilityHidden(true)
            Text(text).font(sys(14, .bold)).foregroundColor(p.primary)
        }
        .padding(.leading, 6).padding(.top, 6)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// بطاقة بيضاء مستديرة بحد رفيع (RowCard)
private struct TafsirLibCard<Content: View>: View {
    let p: HomePalette
    let content: Content
    init(p: HomePalette, @ViewBuilder content: () -> Content) {
        self.p = p
        self.content = content()
    }
    var body: some View {
        content
            .padding(.leading, 14).padding(.trailing, 10).padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 18, style: .continuous).fill(p.surface).shadow(color: .black.opacity(0.04), radius: 3, y: 1))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.7), lineWidth: 1))
    }
}

/// شارة اللغة: «ع» للعربية، ورمز اللغة لغيرها
private struct TafsirLibBadge: View {
    let text: String
    let p: HomePalette
    var body: some View {
        Text(text).font(sys(14, .bold)).foregroundColor(p.onPrimaryContainer)
            .lineLimit(1).minimumScaleFactor(0.7)
            .frame(width: 38, height: 38)
            .background(Circle().fill(p.primaryContainer))
            .accessibilityHidden(true)
    }
}

private struct TafsirLibTexts: View {
    let name: String
    let sub: String
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(name).font(sys(15, .bold)).foregroundColor(p.onSurface).lineLimit(2)
            Text(sub).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

/// تفسير قابل للتنزيل: الحالة في نهاية الصف (✓ على الجهاز، أو إلغاء أثناء التنزيل، أو تنزيل / إعادة محاولة)،
/// وتحته شريط التقدم ونسبته أثناء التنزيل، أو سطر الخطأ إن تعثّر
private struct TafsirLibDownloadCard: View {
    let r: TafsirLibState.Download
    let s: TafsirLibState
    let p: HomePalette
    let model: TafsirLibModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TafsirLibCard(p: p) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 12) {
                    TafsirLibBadge(text: r.badge, p: p)
                    TafsirLibTexts(name: r.name, sub: r.sub, p: p)
                    trailing
                }
                if r.state == "running" {
                    VStack(alignment: .leading, spacing: 4) {
                        TafsirLibProgress(value: r.progress, p: p)
                        Text(r.pct).font(sys(11, .medium)).foregroundColor(p.onSurfaceVariant)
                            .modifier(NumericRoll(value: r.pct))
                    }
                    .padding(.top, 8)
                    .transition(.opacity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(r.pct)
                }
                if r.state == "failed" {
                    Text(s.failed).font(sys(11, .medium)).foregroundColor(HomePalette.color(s.error))
                        .padding(.top, 6)
                }
            }
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: r.state)
        }
    }

    @ViewBuilder private var trailing: some View {
        switch r.state {
        case "done":
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 24)).foregroundColor(p.primary)
                .padding(.horizontal, 12)
                .accessibilityLabel(s.onDevice)
        case "running":
            Button(s.cancel) { model.cancel(r.slug) }
                .font(sys(14, .semibold)).foregroundColor(p.primary)
                .padding(.horizontal, 8).frame(minHeight: 44)
        default:
            let failed = r.state == "failed"
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                model.download(r.slug)
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: failed ? "arrow.clockwise" : "arrow.down.circle.fill")
                        .font(.system(size: 14, weight: .semibold))
                    Text(failed ? s.retry : s.download).font(sys(14, .semibold)).lineLimit(1)
                }
                .foregroundColor(p.onPrimaryContainer)
                .padding(.horizontal, 14).padding(.vertical, 9)
                .background(Capsule().fill(p.primaryContainer))
            }
            .buttonStyle(PressScale(scale: 0.94))
            .accessibilityHint(r.name)
        }
    }
}

/// شريط التقدم: يتقدم بلطف مع كل سورة تُنزَّل
private struct TafsirLibProgress: View {
    let value: Double
    let p: HomePalette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Capsule().fill(p.primaryContainer)
                Capsule().fill(p.primary).frame(width: max(6, g.size.width * CGFloat(min(max(value, 0), 1))))
            }
        }
        .frame(height: 6)
        .animation(reduceMotion ? nil : .easeOut(duration: 0.4), value: value)
    }
}

/// مصدر على الجهاز: مفتاح إظهاره في ورقة التفسير، وزر الحذف للمنزَّل وحده
private struct TafsirLibInstalledCard: View {
    let src: TafsirLibState.Installed
    let s: TafsirLibState
    let p: HomePalette
    let model: TafsirLibModel
    var body: some View {
        TafsirLibCard(p: p) {
            HStack(spacing: 12) {
                TafsirLibBadge(text: src.badge, p: p)
                TafsirLibTexts(name: src.name, sub: src.sub, p: p)
                if let slug = src.slug {
                    Button { model.remove(slug) } label: {
                        Image(systemName: "trash")
                            .font(.system(size: 16, weight: .medium)).foregroundColor(p.onSurfaceVariant)
                            .frame(width: 40, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(PressScale(scale: 0.9))
                    .accessibilityLabel(s.delete)
                    .accessibilityHint(src.name)
                }
                Toggle(src.name, isOn: Binding(get: { src.shown }, set: { model.visible(src.id, $0) }))
                    .labelsHidden()
                    .tint(p.primary)
            }
        }
    }
}

/// ملاحظة بخلفية لون التطبيق الفاتح (Note)
private struct TafsirLibNote: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 17)).foregroundColor(p.primary)
                .accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}
