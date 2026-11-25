import SwiftUI
import UIKit
import Shared

/// ركن الأطفال بواجهة النظام (SwiftUI) بطراز الرئيسية والمسبحة (NativeTasbih.swift): ترويسة بتدرج لون التطبيق ونجمته
/// وشريط علوي ثابت فيه زر الرجوع، ثم شريط النجوم، والسور القصيرة، وأسماء الله الحسنى بطاقاتٍ تُسحب، وأدعية اليوم،
/// ولوحة النجوم؛ وصفحة السورة آيةً آية بخط المصحف الكبير مع «ردّد معي» وشريط تشغيل عائم، واحتفال بنجوم ذهبية
/// كلما حُفظت سورة أو خُتمت — كما في KidsScreen.kt. الحال كلها من Kotlin (NativeKids.ios.kt) مع الإشعار MizanKidsState،
/// والتلاوة ووقفة الترديد في Kotlin (KidsRecite)؛ هنا الرسم واللمس والاحتفال. البطاقات والترويسة من NativeQibla.swift.
enum NativeKidsScreen {
    static func install() {
        NativeKids.shared.attach { session in KidsScreenHost(session: session) }
    }
}

final class KidsScreenHost: UIHostingController<KidsScreenRoot> {
    init(session: NativeKidsSession) {
        let model = KidsScreenModel(session: session)
        super.init(rootView: KidsScreenRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct KidsScreenState: Decodable {
    struct Labels: Decodable {
        let surahs, surahsSub, names, namesSub, duas, duasSub, stars, starsSub, mark, memorized: String
        let repeatAfterMe, listen, yourTurn, wellDone, tapAyah, prevName, nextName: String
        let play, pause, stop, repeatEach, reciter, audioError, meaning: String
    }
    struct Surah: Decodable, Identifiable { let n: Int; let name, ar, ayahs: String; let star: Bool; var id: Int { n } }
    struct Name: Decodable, Identifiable { let i: Int; let ar, latin, ayah, ref: String; let meaning: String?; var id: Int { i } }
    struct Dua: Decodable, Identifiable { let id, title, ar: String; let tr: String? }
    struct Option: Decodable, Identifiable { let value: Int; let label: String; var id: Int { value } }
    struct Reciter: Decodable, Identifiable { let id, name: String }
    struct Ayah: Decodable, Identifiable { let step, n: Int; let num, text: String; var id: Int { step } }
    struct Reader: Decodable {
        let n: Int
        let title, sub: String
        let basmala: String?
        let ayahs: [Ayah]
        let star: Bool
        let phase: String
        let active, running, buffering: Bool
        let step: Int
        let rep: String?
    }

    let rtl, dark, ar, canBack: Bool
    let top, bottom, start, end, lead: Double
    let back, title, subtitle: String
    let l: Labels
    let starCount: String
    let progress: Double
    let progressText: String
    let surahs: [Surah]
    let names: [Name]
    let namesCount: Int
    let duas: [Dua]
    let times: Int
    let timesOptions: [Option]
    let reciter: String
    let reciters: [Reciter]
    let celebrations: Int
    let errors: Int
    let reader: Reader?
    let c: HomeState.Colors
    let error: String

    static func decode(_ json: String) -> KidsScreenState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(KidsScreenState.self, from: data)
    }
}

final class KidsScreenModel: ObservableObject {
    let session: NativeKidsSession
    @Published private(set) var state: KidsScreenState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    /// بطاقة الاسم الظاهرة (تبقى عند فتح سورة والعودة منها)
    @Published var namePage = 0
    /// يزيد مع كل احتفال جديد (سورة حُفظت أو خُتمت)
    @Published private(set) var celebrate = 0
    /// تعذّر تشغيل التلاوة (حتى يبدأ التشغيل مرة أخرى)
    @Published private(set) var failed = false
    // اختيارات تظهر فور اللمس، ثم تؤكدها الحال من Kotlin
    @Published private(set) var times = 2
    @Published private(set) var reciter = ""
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?
    private var seenCelebrations: Int?
    private var seenErrors: Int?
    private let success = UINotificationFeedbackGenerator()
    private let tick = UIImpactFeedbackGenerator(style: .medium)

    init(session: NativeKidsSession) {
        self.session = session
        if let s = KidsScreenState.decode(session.state) { apply(s) }
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanKidsState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = KidsScreenState.decode(self.session.state) else { return }
            self.apply(s)
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    private func apply(_ s: KidsScreenState) {
        if let c = seenCelebrations, s.celebrations > c {
            celebrate += 1
            success.notificationOccurred(.success)
            if UIAccessibility.isVoiceOverRunning {
                UIAccessibility.post(notification: .announcement, argument: s.l.wellDone)
            }
        }
        seenCelebrations = s.celebrations
        if let e = seenErrors, s.errors > e { failed = true }
        seenErrors = s.errors
        if s.reader?.active == true { failed = false }
        if times != s.times { times = s.times }
        if reciter != s.reciter { reciter = s.reciter }
        state = s
    }

    func back() { session.back() }
    func open(_ n: Int) { session.open(surah: Int32(n)) }

    func toggle() {
        failed = false
        tick.impactOccurred()
        session.toggle()
    }

    func stop() { session.stop() }

    func jump(_ step: Int) {
        failed = false
        tick.impactOccurred()
        session.jump(step: Int32(step))
    }

    func star(_ n: Int) {
        if !session.star(surah: Int32(n)) { tick.impactOccurred() }
    }

    func setTimes(_ n: Int) {
        times = n
        session.setTimes(n: Int32(n))
    }

    func setReciter(_ id: String) {
        reciter = id
        session.setReciter(id: id)
    }
}

// MARK: - الشاشة

struct KidsScreenRoot: View {
    @ObservedObject var model: KidsScreenModel
    var body: some View {
        Group {
            if let s = model.state {
                let p = HomePalette(s.c)
                ZStack {
                    if let r = s.reader {
                        KidsReaderView(s: s, r: r, p: p, model: model)
                            .transition(.opacity)
                    } else {
                        KidsHomeView(s: s, p: p, model: model)
                            .transition(.opacity)
                    }
                    KidsCelebrationView(trigger: model.celebrate, text: s.l.wellDone, stars: s.starCount, p: p)
                }
                .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.25), value: s.reader?.n ?? 0)
                .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                .environment(\.colorScheme, s.dark ? .dark : .light)
            } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct KidsHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

/// نجمة خماسية (للشريط واللوحة والاحتفال)
struct KidsStar: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let c = CGPoint(x: rect.midX, y: rect.midY), r = min(rect.width, rect.height) / 2
        for i in 0..<10 {
            let rr = i % 2 == 0 ? r : r * 0.46
            let a = (-90 + Double(i) * 36) * .pi / 180
            let pt = CGPoint(x: c.x + rr * CGFloat(cos(a)), y: c.y + rr * CGFloat(sin(a)))
            if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
        }
        p.closeSubpath()
        return p
    }
}

/// بطاقة قسم: زوايا 28، وفقاعة أيقونة ذهبية، وعنوان عريض بلون التطبيق ووصف قصير
private struct KidsCard<Content: View>: View {
    let title: String
    let sub: String?
    let symbol: String
    let p: HomePalette
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 48, height: 48)
                    .background(Circle().fill(p.gold.opacity(0.18)))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 1) {
                    Text(title).font(sys(20, .heavy)).foregroundColor(p.primary)
                    if let sub = sub, !sub.isEmpty {
                        Text(sub).font(sys(13)).foregroundColor(p.onSurfaceVariant).fixedSize(horizontal: false, vertical: true)
                    }
                }
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 0)
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 28, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 28, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

/// شريط تقدّم مستدير
private struct KidsBar: View {
    let progress: Double
    let track: Color
    let fill: Color
    var body: some View {
        GeometryReader { g in
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule().fill(fill).frame(width: max(0, min(1, CGFloat(progress))) * g.size.width)
            }
        }
        .frame(height: 12)
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .spring(response: 0.5, dampingFraction: 0.8), value: progress)
        .accessibilityHidden(true)
    }
}

// MARK: - الصفحة الأولى

struct KidsHomeView: View {
    let s: KidsScreenState
    let p: HomePalette
    @ObservedObject var model: KidsScreenModel
    @State private var glass: Double = 0

    private func track(_ headerBottom: CGFloat) {
        let g = qiblaTasbihGlass(top: CGFloat(s.top), headerBottom: headerBottom)
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    QiblaTasbihHeader(title: s.title, subtitle: s.subtitle, symbol: kidsSymbol(),
                                      top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: KidsHeaderBottomKey.self, value: g.frame(in: .named("kids")).maxY)
                        })
                    VStack(spacing: 16) {
                        banner
                        KidsCard(title: s.l.surahs, sub: s.l.surahsSub, symbol: "book.fill", p: p) { surahGrid }
                        KidsCard(title: s.l.names, sub: s.l.namesSub, symbol: "sparkles", p: p) {
                            KidsNamesCarousel(s: s, p: p, model: model)
                        }
                        KidsCard(title: s.l.duas, sub: s.l.duasSub, symbol: "hands.sparkles.fill", p: p) {
                            ForEach(s.duas) { d in KidsDuaCard(d: d, p: p) }
                        }
                        KidsCard(title: s.l.stars, sub: s.l.starsSub, symbol: "star.fill", p: p) { chart }
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "kids")
            .onPreferenceChange(KidsHeaderBottomKey.self) { track($0) }
            QiblaTasbihTopBar(title: s.title, back: s.back, canBack: s.canBack, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
        }
    }

    /// عدد النجوم بنجمة ذهبية كبيرة على تدرج لون التطبيق، وكم سورة حُفظت
    private var banner: some View {
        HStack(spacing: 16) {
            ZStack {
                KidsStar().fill(p.gold)
                KidsStar().fill(Color.white.opacity(0.35)).frame(width: 30, height: 30)
            }
            .frame(width: 64, height: 64)
            VStack(alignment: .leading, spacing: 8) {
                Text(s.starCount).font(sys(26, .black)).foregroundColor(.white)
                    .modifier(NumericRoll(value: s.starCount))
                KidsBar(progress: s.progress, track: Color.white.opacity(0.25), fill: p.gold)
                Text(verbatim: s.progressText).font(sys(13, .semibold)).foregroundColor(.white.opacity(0.85))
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .background(RoundedRectangle(cornerRadius: 28, style: .continuous)
            .fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(s.l.stars): \(s.starCount), \(s.progressText)")
    }

    private var surahGrid: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 10)], spacing: 10) {
            ForEach(s.surahs) { x in
                Button { model.open(x.n) } label: {
                    VStack(spacing: 2) {
                        Text(x.ar).font(quranFont(26)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.6)
                        if !s.ar {
                            Text(x.name).font(sys(14, .bold)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.8)
                        }
                        Text(x.ayahs).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
                    }
                    .padding(.horizontal, 10).padding(.vertical, 14)
                    .frame(maxWidth: .infinity, minHeight: 116)
                    .background(RoundedRectangle(cornerRadius: 24, style: .continuous).fill(p.primaryContainer.opacity(0.55)))
                    .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .strokeBorder(x.star ? p.gold : p.outlineVariant.opacity(0.6), lineWidth: x.star ? 2 : 1))
                    .overlay(alignment: .topTrailing) {
                        if x.star {
                            Image(systemName: "star.fill").font(.system(size: 18, weight: .bold)).foregroundColor(p.gold).padding(9)
                        }
                    }
                    .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                }
                .buttonStyle(PressScale(scale: 0.94))
                .accessibilityLabel(x.star ? "\(x.name), \(s.l.memorized)" : x.name)
            }
        }
    }

    /// لوحة النجوم: خانة لكل سورة يلمسها أحد الوالدين فتأخذ نجمتها (ولمسة أخرى ترفعها)
    @ViewBuilder private var chart: some View {
        HStack(spacing: 12) {
            Image(systemName: "star.fill").font(.system(size: 28, weight: .bold)).foregroundColor(p.gold).accessibilityHidden(true)
            Text(s.starCount).font(sys(22, .black)).foregroundColor(p.primary)
            Spacer(minLength: 8)
            Text(verbatim: s.progressText).font(sys(15, .bold)).foregroundColor(p.onSurfaceVariant)
        }
        .accessibilityElement(children: .combine)
        KidsBar(progress: s.progress, track: p.outlineVariant.opacity(0.5), fill: p.gold)
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 92), spacing: 10)], spacing: 10) {
            ForEach(s.surahs) { x in KidsChartCell(x: x, l: s.l, p: p) { model.star(x.n) } }
        }
    }
}

/// رمز ركن الأطفال: طفل يمسك يد أبيه (iOS 16)، وقبله نجمة
func kidsSymbol() -> String {
    if #available(iOS 16.0, *) { return "figure.and.child.holdinghands" } else { return "star.circle.fill" }
}

private struct KidsChartCell: View {
    let x: KidsScreenState.Surah
    let l: KidsScreenState.Labels
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: x.star ? "star.fill" : "star")
                    .font(.system(size: 30, weight: .bold)).foregroundColor(x.star ? p.gold : p.onSurfaceVariant.opacity(0.6))
                    .scaleEffect(x.star ? 1 : 0.82)
                    .animation(UIAccessibility.isReduceMotionEnabled ? nil : .spring(response: 0.35, dampingFraction: 0.45), value: x.star)
                Text(x.ar).font(quranFont(15)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.6)
            }
            .padding(.horizontal, 6).padding(.vertical, 10)
            .frame(maxWidth: .infinity, minHeight: 92)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(x.star ? p.gold.opacity(0.22) : p.surface))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(x.star ? p.gold : p.outlineVariant, lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.92))
        .accessibilityLabel(x.name)
        .accessibilityValue(x.star ? l.memorized : l.mark)
        .accessibilityAddTraits(x.star ? .isSelected : [])
    }
}

/// أسماء الله الحسنى: بطاقة لكل اسم تُسحب، وزرّا السابق والتالي وعدّاد
private struct KidsNamesCarousel: View {
    let s: KidsScreenState
    let p: HomePalette
    @ObservedObject var model: KidsScreenModel

    var body: some View {
        let count = s.names.count
        // صفحات TabView من اليسار إلى اليمين دائماً: في العربية تُعكس الأسماء فيأتي التالي من اليسار
        let ordered = s.rtl ? Array(s.names.reversed()) : s.names
        VStack(spacing: 12) {
            TabView(selection: Binding(get: { min(model.namePage, max(count - 1, 0)) }, set: { model.namePage = $0 })) {
                ForEach(ordered) { n in
                    KidsNameCard(n: n, s: s, p: p)
                        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                        .padding(.horizontal, 2)
                        .tag(n.i)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .environment(\.layoutDirection, .leftToRight)
            .frame(height: 440)
            HStack(spacing: 0) {
                KidsRoundButton(symbol: "chevron.backward", label: s.l.prevName, enabled: model.namePage > 0, p: p) {
                    withAnimation { model.namePage = max(model.namePage - 1, 0) }
                }
                Text(verbatim: "\(model.namePage + 1) / \(count)")
                    .font(sys(15, .bold)).foregroundColor(p.onSurfaceVariant).monospacedDigit()
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .leftToRight)
                KidsRoundButton(symbol: "chevron.forward", label: s.l.nextName, enabled: model.namePage < count - 1, p: p) {
                    withAnimation { model.namePage = min(model.namePage + 1, max(count - 1, 0)) }
                }
            }
        }
    }
}

private struct KidsNameCard: View {
    let n: KidsScreenState.Name
    let s: KidsScreenState
    let p: HomePalette
    var body: some View {
        VStack(spacing: 6) {
            Text(n.ar).font(quranFont(46)).foregroundColor(p.primary).lineLimit(1).minimumScaleFactor(0.5)
                .accessibilityLabel(s.ar ? n.ar : "\(n.ar), \(n.latin)")
            if !s.ar {
                Text(n.latin).font(sys(16, .bold)).foregroundColor(p.goldDeep).accessibilityHidden(true)
            }
            Text(n.ayah).font(quranFont(20)).foregroundColor(p.onSurface)
                .multilineTextAlignment(.center).lineLimit(4).lineSpacing(6)
                .environment(\.layoutDirection, .rightToLeft)
            Text(n.ref).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            if let m = n.meaning {
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(s.l.meaning).font(sys(12, .bold)).foregroundColor(p.primary)
                        Text(m).font(sys(15)).foregroundColor(p.onSurface).lineSpacing(3)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(RoundedRectangle(cornerRadius: 26, style: .continuous)
            .fill(LinearGradient(colors: [p.gold.opacity(0.20), p.primaryContainer.opacity(0.45)], startPoint: .top, endPoint: .bottom)))
        .overlay(RoundedRectangle(cornerRadius: 26, style: .continuous).strokeBorder(p.gold.opacity(0.55), lineWidth: 1))
    }
}

/// زر دائري كبير بلون فاتح من لون التطبيق
private struct KidsRoundButton: View {
    let symbol: String
    let label: String
    var enabled = true
    var size: CGFloat = 56
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: size * 0.38, weight: .bold))
                .foregroundColor(enabled ? p.primary : p.onSurfaceVariant.opacity(0.4))
                .frame(width: size, height: size)
                .background(Circle().fill(enabled ? p.primaryContainer : p.outlineVariant.opacity(0.3)))
                .contentShape(Circle())
        }
        .buttonStyle(PressScale(scale: 0.88))
        .disabled(!enabled)
        .accessibilityLabel(label)
    }
}

/// دعاء: مناسبته، ونصه بخط المصحف، وترجمته
private struct KidsDuaCard: View {
    let d: KidsScreenState.Dua
    let p: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Circle().fill(p.gold).frame(width: 10, height: 10).accessibilityHidden(true)
                Text(d.title).font(sys(15, .heavy)).foregroundColor(p.primary)
            }
            Text(d.ar).font(quranFont(24)).foregroundColor(p.onSurface)
                .multilineTextAlignment(.center).lineSpacing(8)
                .frame(maxWidth: .infinity)
                .environment(\.layoutDirection, .rightToLeft)
            if let t = d.tr {
                Text(t).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                    .multilineTextAlignment(.center).frame(maxWidth: .infinity)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.primaryContainer.opacity(0.45)))
        .accessibilityElement(children: .combine)
    }
}

// MARK: - صفحة السورة

struct KidsReaderView: View {
    let s: KidsScreenState
    let r: KidsScreenState.Reader
    let p: HomePalette
    @ObservedObject var model: KidsScreenModel
    @State private var glass: Double = 0

    private var still: Bool { UIAccessibility.isReduceMotionEnabled }

    private func track(_ headerBottom: CGFloat) {
        let g = qiblaTasbihGlass(top: CGFloat(s.top), headerBottom: headerBottom)
        if g != glass { glass = g }
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background
            ScrollViewReader { proxy in
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 0) {
                        QiblaTasbihHeader(title: r.title, subtitle: r.sub, symbol: "book.fill",
                                          top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                            .background(GeometryReader { g in
                                Color.clear.preference(key: KidsHeaderBottomKey.self, value: g.frame(in: .named("kidsReader")).maxY)
                            })
                        VStack(spacing: 14) {
                            settings
                            memorize
                            if let b = r.basmala {
                                KidsAyahCard(text: b, num: nil, highlighted: r.step == 0, listening: r.phase == "listen", p: p) { model.jump(0) }
                                    .id(0)
                            }
                            ForEach(r.ayahs) { a in
                                KidsAyahCard(text: a.text, num: a.num, highlighted: r.step == a.step, listening: r.phase == "listen", p: p) {
                                    model.jump(a.step)
                                }
                                .id(a.step)
                            }
                        }
                        .padding(.leading, CGFloat(s.start))
                        .padding(.trailing, CGFloat(s.end))
                        // مكان شريط التشغيل العائم
                        Color.clear.frame(height: 112)
                    }
                    .padding(.bottom, CGFloat(s.bottom))
                }
                .coordinateSpace(name: "kidsReader")
                .onPreferenceChange(KidsHeaderBottomKey.self) { track($0) }
                .onChange(of: r.step) { step in
                    guard step >= 0 else { return }
                    if still { proxy.scrollTo(step, anchor: .center) } else {
                        withAnimation(.easeInOut(duration: 0.45)) { proxy.scrollTo(step, anchor: .center) }
                    }
                }
            }
            QiblaTasbihTopBar(title: r.title, back: s.back, canBack: true, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
            VStack {
                Spacer()
                KidsControlsBar(s: s, r: r, p: p, model: model)
                    .padding(.horizontal, 16)
                    .padding(.bottom, CGFloat(s.bottom) - 8)
            }
        }
    }

    /// «ردّد معي»: تكرار كل آية، والقارئ
    private var settings: some View {
        KidsCard(title: s.l.repeatAfterMe, sub: s.l.tapAyah, symbol: "music.mic", p: p) {
            Text(s.l.repeatEach).font(sys(14, .semibold)).foregroundColor(p.onSurfaceVariant)
            Picker(s.l.repeatEach, selection: Binding(get: { model.times }, set: { model.setTimes($0) })) {
                ForEach(s.timesOptions) { o in Text(o.label).tag(o.value) }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            HStack(spacing: 10) {
                Text(s.l.reciter).font(sys(15, .medium)).foregroundColor(p.onSurface)
                Spacer(minLength: 8)
                Menu {
                    ForEach(s.reciters) { x in
                        Button { model.setReciter(x.id) } label: {
                            if x.id == model.reciter { Label(x.name, systemImage: "checkmark") } else { Text(x.name) }
                        }
                    }
                } label: {
                    HStack(spacing: 6) {
                        Text(s.reciters.first(where: { $0.id == model.reciter })?.name ?? "")
                            .font(sys(15, .bold)).foregroundColor(p.primary).lineLimit(1)
                        Image(systemName: "chevron.up.chevron.down").font(.system(size: 12, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(Capsule().fill(p.primaryContainer))
                }
                .accessibilityLabel(s.l.reciter)
            }
        }
    }

    /// «حفظها؟ أعطِ نجمة»: زر كبير للوالدين، ذهبي ممتلئ حين تكون السورة محفوظة
    private var memorize: some View {
        Button { model.star(r.n) } label: {
            HStack(spacing: 10) {
                Image(systemName: r.star ? "star.fill" : "star").font(.system(size: 26, weight: .bold))
                    .foregroundColor(r.star ? .white : p.goldDeep)
                Text(r.star ? s.l.memorized : s.l.mark).font(sys(18, .heavy))
                    .foregroundColor(r.star ? Color(red: 0.23, green: 0.16, blue: 0) : p.goldDeep)
            }
            .frame(maxWidth: .infinity, minHeight: 64)
            .padding(.horizontal, 20)
            .background(Capsule().fill(r.star ? p.gold : p.surface))
            .overlay(Capsule().strokeBorder(p.gold, lineWidth: 2))
            .contentShape(Capsule())
        }
        .buttonStyle(PressScale(scale: 0.96))
        .animation(still ? nil : .easeOut(duration: 0.2), value: r.star)
        .accessibilityAddTraits(r.star ? .isSelected : [])
    }
}

/// آية بخط كبير، ورقمها في نجمة ذهبية؛ تتظلل وتكبر قليلاً وهي تُتلى، وتُلمس فتبدأ منها
private struct KidsAyahCard: View {
    let text: String
    let num: String?
    let highlighted: Bool
    let listening: Bool
    let p: HomePalette
    let action: () -> Void

    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        Button(action: action) {
            VStack(spacing: 8) {
                Text(text).font(quranFont(num == nil ? 26 : 30)).foregroundColor(num == nil ? p.primary : p.onSurface)
                    .multilineTextAlignment(.center).lineSpacing(10)
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
                if let num = num {
                    ZStack {
                        Star8().fill(p.gold.opacity(0.18))
                        Star8().stroke(p.gold, lineWidth: 1.5)
                        Text(verbatim: num).font(sys(14, .bold)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.6)
                    }
                    .frame(width: 40, height: 40)
                    .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 18).padding(.vertical, 16)
            .frame(maxWidth: .infinity)
            .background(RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(highlighted ? (listening ? p.gold.opacity(0.22) : p.primaryContainer) : p.surface))
            .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(highlighted ? p.gold : p.outlineVariant.opacity(0.6), lineWidth: highlighted ? 2 : 1))
            .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.98))
        .scaleEffect(highlighted && !still ? 1.02 : 1)
        .animation(still ? nil : .spring(response: 0.4, dampingFraction: 0.7), value: highlighted)
        .animation(still ? nil : .easeOut(duration: 0.25), value: listening)
        .accessibilityAddTraits(highlighted ? .isSelected : [])
    }
}

/// شريط التشغيل العائم: زر تشغيل كبير، وحال «ردّد معي» (استمع، دورك، المرة)، والإيقاف
private struct KidsControlsBar: View {
    let s: KidsScreenState
    let r: KidsScreenState.Reader
    let p: HomePalette
    @ObservedObject var model: KidsScreenModel

    private var label: String {
        if model.failed { return s.l.audioError }
        switch r.phase {
        case "listen": return s.l.listen
        case "repeat": return s.l.yourTurn
        case "done": return s.l.wellDone
        default: return s.l.repeatAfterMe
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            Button { model.toggle() } label: {
                Image(systemName: r.running ? "pause.fill" : "play.fill")
                    .font(.system(size: 30, weight: .bold)).foregroundColor(.white)
                    .frame(width: 68, height: 68)
                    .background(Circle().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)))
                    .contentShape(Circle())
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(r.running ? s.l.pause : s.l.play)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(label).font(sys(18, .heavy))
                        .foregroundColor(model.failed ? HomePalette.color(s.error) : p.primary)
                        .lineLimit(2).minimumScaleFactor(0.8)
                    if r.buffering && r.phase == "listen" { ProgressView().scaleEffect(0.8) }
                }
                if let rep = r.rep {
                    Text(verbatim: rep).font(sys(14, .bold)).foregroundColor(p.goldDeep)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.updatesFrequently)
            if r.active {
                KidsRoundButton(symbol: "stop.fill", label: s.l.stop, size: 52, p: p) { model.stop() }
            }
        }
        .padding(10)
        .frame(maxWidth: 560)
        .background(RoundedRectangle(cornerRadius: 36, style: .continuous).fill(p.surface)
            .shadow(color: .black.opacity(0.14), radius: 10, y: 3))
        .overlay(RoundedRectangle(cornerRadius: 36, style: .continuous).strokeBorder(p.gold.opacity(0.6), lineWidth: 1))
        .animation(UIAccessibility.isReduceMotionEnabled ? nil : .easeOut(duration: 0.2), value: r.phase)
    }
}

// MARK: - الاحتفال

/// نجوم وقصاصات تتناثر من الوسط، وبطاقة «أحسنت!» تنبثق ثم تختفي، كلما زاد trigger
private struct KidsCelebrationView: View {
    let trigger: Int
    let text: String
    let stars: String
    let p: HomePalette
    @State private var shown = false
    @State private var burst = false
    @State private var token = 0

    private var colors: [Color] {
        [p.gold, p.goldDeep, p.primary, Color(red: 1, green: 0.54, blue: 0.4), Color(red: 0.39, green: 0.71, blue: 0.96), Color(red: 0.73, green: 0.41, blue: 0.78)]
    }

    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        ZStack {
            if shown {
                if !still {
                    ForEach(0..<30, id: \.self) { k in particle(k) }
                }
                VStack(spacing: 8) {
                    ZStack {
                        KidsStar().fill(p.gold)
                        KidsStar().fill(Color.white.opacity(0.4)).frame(width: 40, height: 40)
                    }
                    .frame(width: 84, height: 84)
                    Text(text).font(sys(22, .black)).foregroundColor(p.primary).multilineTextAlignment(.center)
                    Text(stars).font(sys(15, .bold)).foregroundColor(p.goldDeep)
                }
                .padding(.horizontal, 28).padding(.vertical, 20)
                .background(RoundedRectangle(cornerRadius: 30, style: .continuous).fill(p.surface)
                    .shadow(color: .black.opacity(0.18), radius: 12, y: 4))
                .overlay(RoundedRectangle(cornerRadius: 30, style: .continuous).strokeBorder(p.gold, lineWidth: 2))
                .scaleEffect(burst || still ? 1 : 0.6)
                .transition(.opacity)
                .accessibilityHidden(true)
            }
        }
        .allowsHitTesting(false)
        .onChange(of: trigger) { _ in play() }
    }

    private func particle(_ k: Int) -> some View {
        let a = Double(k) * .pi * 2 / 30 + Double(k % 3) * 0.17
        let speed = 0.55 + Double(k * 37 % 45) / 100
        let reach: CGFloat = 230 * CGFloat(speed)
        let size: CGFloat = 14 + CGFloat(k % 4) * 4
        let color = colors[k % colors.count]
        return Group {
            if k % 2 == 0 { KidsStar().fill(color).frame(width: size, height: size) } else { Circle().fill(color).frame(width: size * 0.55, height: size * 0.55) }
        }
        .rotationEffect(.degrees(burst ? (k % 4 == 0 ? 360 : -360) : 0))
        .offset(x: burst ? reach * CGFloat(cos(a)) : 0, y: burst ? reach * CGFloat(sin(a)) + 90 : 0)
        .opacity(burst ? 0 : 1)
    }

    private func play() {
        token += 1
        let mine = token
        var t = Transaction()
        t.disablesAnimations = true
        withTransaction(t) {
            burst = false
            shown = true
        }
        DispatchQueue.main.async {
            withAnimation(UIAccessibility.isReduceMotionEnabled ? .easeOut(duration: 0.2) : .easeOut(duration: 1.8)) { burst = true }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) {
            guard mine == token else { return }
            withAnimation(.easeOut(duration: 0.3)) { shown = false }
        }
    }
}
