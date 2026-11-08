import SwiftUI
import UIKit
import Shared

/// الأذكار بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته وزر الرجوع،
/// وأذكار الوقت في بطاقة بارزة، والأقسام في شبكة بطاقات، ثم أذكار القسم: شريط تقدّم ذهبي، وبطاقة لكل ذكر بنصه بخط المصحف
/// وترجمته، ولمسُها يعدّ (مع اهتزاز، وعند الإتمام حدّ ذهبي)، ودليله في ورقة (الدرجة والمصدر والراوي والفضل)، ومشاركته.
/// الحالة من Kotlin (NativeAdhkar.ios.kt) مع الإشعار MizanAdhkarState، والعدّ هنا.
enum NativeAdhkarScreen {
    static func install() {
        NativeAdhkar.shared.attach { session in AdhkarHostController(session: session) }
    }
}

final class AdhkarHostController: UIHostingController<AdhkarRoot> {
    init(session: NativeAdhkarSession) {
        let model = AdhkarModel(session: session)
        super.init(rootView: AdhkarRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct AdhkarState: Decodable {
    struct Section: Decodable, Identifiable { let id, icon, title, desc, countLabel: String; let done: Bool }
    struct Item: Decodable, Identifiable {
        let id, ar: String
        let tr, shortNote, note: String?
        let count: Int
        let times, grade, gradeLine: String
        let sources: [String]
        let narr, fadl, fadlTr, ayahRefs: String?
    }
    struct Group: Decodable, Identifiable { let key: String; let title: String?; let items: [Item]; var id: String { key } }
    struct Open: Decodable { let id, icon, title, desc: String; let groups: [Group] }

    let rtl, dark, ready: Bool
    let top, bottom, start, end: Double
    let title, desc, back, nowLabel, nowId, tapHint, doneLabel, evidence, share: String
    let gradeLabel, sourceLabel, quranRefLabel, narratorLabel, virtueLabel: String
    let sections: [Section]
    let open: Open?
    let c: HomeState.Colors

    static func decode(_ json: String) -> AdhkarState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(AdhkarState.self, from: data)
    }
}

final class AdhkarModel: ObservableObject {
    let session: NativeAdhkarSession
    @Published private(set) var state: AdhkarState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeAdhkarSession) {
        self.session = session
        state = AdhkarState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanAdhkarState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = AdhkarState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func open(_ id: String) { session.open(id: id) }
    func back() { session.back() }
    func share(_ id: String) { session.share(id: id) }
    func complete(_ id: String) { session.complete(id: id) }
}

/// رموز الأقسام بأيقونات النظام (SECTION_ICONS في AdhkarScreen.kt)
private func sectionSymbol(_ icon: String) -> String {
    switch icon {
    case "sunrise": return "sun.max.fill"
    case "sunset": return "sunset.fill"
    case "sleep": return "bed.double.fill"
    case "wake": return "alarm.fill"
    case "prayer": return "leaf.fill"
    case "mosque": return "building.columns.fill"
    case "adhan": return "megaphone.fill"
    case "home": return "house.fill"
    case "clothes": return "tshirt.fill"
    case "food": return "fork.knife"
    case "moon": return "moon.fill"
    case "travel": return "airplane"
    case "heart": return "heart.fill"
    case "health": return "cross.case.fill"
    case "funeral": return "leaf"
    case "weather": return "cloud.bolt.rain.fill"
    case "kaaba": return "globe.asia.australia.fill"
    case "istikhara": return "lightbulb.fill"
    case "shield": return "shield.fill"
    case "people": return "person.2.fill"
    case "quran": return "book.fill"
    case "hands": return "hands.sparkles.fill"
    case "repeat": return "arrow.triangle.2.circlepath"
    default: return "sparkles"
    }
}

// MARK: - الشاشة

struct AdhkarRoot: View {
    @ObservedObject var model: AdhkarModel
    var body: some View {
        Group {
            if let s = model.state { AdhkarView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

struct AdhkarView: View {
    let s: AdhkarState
    let p: HomePalette
    let model: AdhkarModel

    var body: some View {
        ZStack {
            p.background
            if let sec = s.open {
                SectionPage(s: s, sec: sec, p: p, model: model)
                    .id(sec.id)
                    .transition(.move(edge: .trailing))
            } else {
                SectionsPage(s: s, p: p, model: model)
                    .transition(.move(edge: .leading))
            }
        }
        .animation(.easeInOut(duration: 0.3), value: s.open?.id)
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

/// ترويسة بطراز الرئيسية مع زر رجوع
private struct AdhkarHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let back: String
    let s: AdhkarState
    let p: HomePalette
    let onBack: () -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Button(action: onBack) {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 17, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(p.surface.opacity(0.85)).shadow(color: .black.opacity(0.08), radius: 3, y: 1))
            }
            .buttonStyle(PressScale(scale: 0.9))
            .accessibilityLabel(back)
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 46, height: 46)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
                VStack(alignment: .leading, spacing: 1) {
                    Text(title).font(sys(26, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                    Text(subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                }
                Spacer(minLength: 0)
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 8)
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
    }
}

// MARK: الأقسام

private struct SectionsPage: View {
    let s: AdhkarState
    let p: HomePalette
    let model: AdhkarModel

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                AdhkarHeader(title: s.title, subtitle: s.desc, symbol: "sparkles", back: s.back, s: s, p: p) { model.back() }
                VStack(spacing: 12) {
                    if !s.ready { ProgressView().padding(40) }
                    if let now = s.sections.first(where: { $0.id == s.nowId }) {
                        NowCard(sec: now, label: s.nowLabel, p: p) { model.open(now.id) }
                    }
                    let rest = s.sections.filter { $0.id != s.nowId }
                    ForEach(Array(stride(from: 0, to: rest.count, by: 2)), id: \.self) { i in
                        HStack(spacing: 12) {
                            SectionTile(sec: rest[i], p: p) { model.open(rest[i].id) }
                            if i + 1 < rest.count {
                                SectionTile(sec: rest[i + 1], p: p) { model.open(rest[i + 1].id) }
                            } else {
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
    }
}

private struct DoneBadge: View {
    let p: HomePalette
    var body: some View {
        Image(systemName: "checkmark").font(.system(size: 12, weight: .heavy)).foregroundColor(Color(white: 0.1))
            .frame(width: 26, height: 26).background(Circle().fill(p.gold))
            .accessibilityHidden(true)
    }
}

/// أذكار الوقت: بطاقة بتدرج لون التطبيق كبطاقة القرآن في الرئيسية
private struct NowCard: View {
    let sec: AdhkarState.Section
    let label: String
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: sectionSymbol(sec.icon))
                    .font(.system(size: 24, weight: .semibold)).foregroundColor(p.gold)
                    .frame(width: 56, height: 56).background(Circle().fill(Color.white.opacity(0.12)))
                VStack(alignment: .leading, spacing: 2) {
                    Text(label).font(sys(14, .bold)).foregroundColor(p.gold)
                    Text(sec.title).font(sys(22, .bold)).foregroundColor(.white).lineLimit(2)
                    Text(sec.desc + " · " + sec.countLabel).font(sys(12)).foregroundColor(.white.opacity(0.8)).lineLimit(2)
                }
                Spacer(minLength: 0)
                if sec.done { DoneBadge(p: p) }
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                ZStack(alignment: .trailing) {
                    LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                    StarEmblem(color: .white.opacity(0.07), radius: 70, stroke: 1).offset(x: 30)
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

private struct SectionTile: View {
    let sec: AdhkarState.Section
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Image(systemName: sectionSymbol(sec.icon))
                        .font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.15)))
                    Spacer()
                    if sec.done { DoneBadge(p: p) }
                }
                Text(sec.title).font(sys(14, .bold)).foregroundColor(p.onSurface).lineLimit(2).padding(.top, 10)
                Text(sec.countLabel).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(maxWidth: .infinity, minHeight: 132, maxHeight: .infinity, alignment: .topLeading)
            .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(p.surface))
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.8), lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.97))
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: أذكار القسم

private struct SectionPage: View {
    let s: AdhkarState
    let sec: AdhkarState.Open
    let p: HomePalette
    let model: AdhkarModel
    @State private var done: [String: Int] = [:]
    @State private var evidence: AdhkarState.Item?
    private let tap = UIImpactFeedbackGenerator(style: .light)
    private let finish = UINotificationFeedbackGenerator()

    private var items: [AdhkarState.Item] { sec.groups.flatMap { $0.items } }
    private var total: Int { items.reduce(0) { $0 + $1.count } }
    private var progress: Int { items.reduce(0) { $0 + min(done[$1.id] ?? 0, $1.count) } }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                AdhkarHeader(title: sec.title, subtitle: sec.desc, symbol: sectionSymbol(sec.icon), back: s.back, s: s, p: p) { model.open("") }
                VStack(alignment: .leading, spacing: 12) {
                    let complete = total > 0 && progress >= total
                    VStack(alignment: .leading, spacing: 6) {
                        GeometryReader { g in
                            ZStack(alignment: .leading) {
                                Capsule().fill(p.gold.opacity(0.18))
                                Capsule().fill(p.gold).frame(width: total == 0 ? 0 : g.size.width * CGFloat(progress) / CGFloat(total))
                            }
                        }
                        .frame(height: 6)
                        .animation(.easeOut(duration: 0.3), value: progress)
                        Text(complete ? s.doneLabel : s.tapHint).font(sys(12)).foregroundColor(complete ? p.primary : p.onSurfaceVariant)
                    }
                    ForEach(sec.groups) { g in
                        if let t = g.title {
                            Text(t).font(sys(14, .bold)).foregroundColor(p.primary).padding(.top, 6).padding(.leading, 6)
                        }
                        ForEach(g.items) { d in
                            DhikrCard(d: d, done: done[d.id] ?? 0, s: s, p: p,
                                      onTap: { count(d) }, onEvidence: { evidence = d }, onShare: { model.share(d.id) })
                        }
                    }
                }
                .padding(.leading, CGFloat(s.start))
                .padding(.trailing, CGFloat(s.end))
            }
            .padding(.bottom, CGFloat(s.bottom))
        }
        .sheet(item: $evidence) { d in
            EvidenceSheet(d: d, s: s, p: p)
                .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                .environment(\.colorScheme, s.dark ? .dark : .light)
        }
    }

    /// لمسة على الذكر: تزيد عدّه حتى يكتمل، ولمسة بعد اكتماله تعيده من أوله
    private func count(_ d: AdhkarState.Item) {
        let n = done[d.id] ?? 0
        if n < d.count {
            withAnimation(.easeOut(duration: 0.2)) { done[d.id] = n + 1 }
            if n + 1 == d.count { finish.notificationOccurred(.success) } else { tap.impactOccurred() }
            if items.allSatisfy({ (done[$0.id] ?? 0) >= $0.count }) { model.complete(sec.id) }
        } else {
            withAnimation(.easeOut(duration: 0.2)) { done[d.id] = 0 }
        }
    }
}

private struct DhikrCard: View {
    let d: AdhkarState.Item
    let done: Int
    let s: AdhkarState
    let p: HomePalette
    let onTap: () -> Void
    let onEvidence: () -> Void
    let onShare: () -> Void

    private var gradeColor: Color {
        switch d.grade {
        case "daif": return Color(red: 0.71, green: 0.25, blue: 0.18)
        case "hasan": return p.gold
        default: return p.primary
        }
    }

    var body: some View {
        let finished = done >= d.count
        VStack(alignment: .leading, spacing: 0) {
            Button(action: onTap) {
                VStack(alignment: .leading, spacing: 10) {
                    Text(d.ar)
                        .font(quranFont(21)).foregroundColor(p.onSurface).lineSpacing(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .environment(\.layoutDirection, .rightToLeft)
                    if let t = d.tr { Text(t).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineSpacing(3) }
                    if let n = d.shortNote { Text(n).font(sys(12, .bold)).foregroundColor(p.goldDeep) }
                }
                .opacity(finished ? 0.55 : 1)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.985))
            HStack(spacing: 6) {
                CountPill(count: d.count, done: done, times: d.times, p: p)
                Spacer()
                Button(action: onEvidence) {
                    HStack(spacing: 5) {
                        Image(systemName: "checkmark.seal").foregroundColor(gradeColor)
                        Text(s.evidence).font(sys(14, .bold)).foregroundColor(p.primary)
                    }
                    .padding(.horizontal, 8).padding(.vertical, 6)
                }
                .buttonStyle(PressScale(scale: 0.95))
                Button(action: onShare) {
                    Image(systemName: "square.and.arrow.up").font(.system(size: 16)).foregroundColor(p.onSurfaceVariant)
                        .frame(width: 36, height: 36)
                }
                .buttonStyle(PressScale(scale: 0.9))
                .accessibilityLabel(s.share)
            }
            .padding(.top, 8)
        }
        .padding(.horizontal, 18).padding(.top, 16).padding(.bottom, 8)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(finished ? p.gold : p.outlineVariant.opacity(0.8), lineWidth: finished ? 1.5 : 1))
        .animation(.easeOut(duration: 0.25), value: finished)
    }
}

private struct CountPill: View {
    let count: Int
    let done: Int
    let times: String
    let p: HomePalette
    var body: some View {
        let finished = done >= count
        HStack(spacing: 6) {
            if finished {
                Image(systemName: "checkmark").font(.system(size: 13, weight: .heavy))
            } else if count > 1 {
                Text("\(done) / \(count)").font(sys(14, .bold)).monospacedDigit().modifier(NumericRoll())
                    .environment(\.layoutDirection, .leftToRight)
            }
            if !finished || count > 1 { Text(times).font(sys(12, .medium)) }
        }
        .foregroundColor(finished ? Color(white: 0.1) : p.onPrimaryContainer)
        .padding(.horizontal, 12).padding(.vertical, 6)
        .background(Capsule().fill(finished ? p.gold : p.primaryContainer))
        .animation(.easeOut(duration: 0.2), value: done)
    }
}

/// الدليل: الدرجة، والمصادر، والراوي، والفضل بنصه العربي وترجمته
private struct EvidenceSheet: View {
    let d: AdhkarState.Item
    let s: AdhkarState
    let p: HomePalette

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Image(systemName: "book.fill").foregroundColor(p.primary)
                        .frame(width: 44, height: 44)
                        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.15)))
                    Text(s.evidence).font(sys(22, .bold)).foregroundColor(p.onSurface)
                }
                Text(d.ar).font(quranFont(17)).foregroundColor(p.onSurfaceVariant).lineLimit(3).lineSpacing(8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .environment(\.layoutDirection, .rightToLeft)
                field(s.gradeLabel) {
                    Label(d.gradeLine, systemImage: "checkmark.seal.fill")
                        .font(sys(14, .bold)).foregroundColor(p.primary)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(Capsule().fill(p.primary.opacity(0.12)))
                }
                field(d.grade == "quran" ? s.quranRefLabel : s.sourceLabel) {
                    ForEach(d.sources, id: \.self) { line in
                        HStack(spacing: 10) {
                            Circle().fill(p.gold).frame(width: 6, height: 6)
                            Text(line).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        }
                    }
                }
                if let n = d.narr { field(s.narratorLabel) { Text(n).font(sys(16)).foregroundColor(p.onSurface) } }
                if let f = d.fadl {
                    field(s.virtueLabel) {
                        Text(f).font(quranFont(18)).foregroundColor(p.onSurface).lineSpacing(10)
                            .padding(12).frame(maxWidth: .infinity, alignment: .leading)
                            .background(RoundedRectangle(cornerRadius: 12).fill(p.gold.opacity(0.08)))
                            .environment(\.layoutDirection, .rightToLeft)
                        if let t = d.fadlTr { Text(t).font(sys(14)).foregroundColor(p.onSurfaceVariant) }
                    }
                }
                if let a = d.ayahRefs { field(s.quranRefLabel) { Text(a).font(sys(14)).foregroundColor(p.onSurface) } }
                if let n = d.note {
                    Text(n).font(sys(13)).foregroundColor(p.onSurfaceVariant)
                        .padding(12).frame(maxWidth: .infinity, alignment: .leading)
                        .background(RoundedRectangle(cornerRadius: 12).fill(p.primaryContainer.opacity(0.6)))
                }
            }
            .padding(22)
        }
        .background(p.surface.ignoresSafeArea())
    }

    private func field<C: View>(_ label: String, @ViewBuilder _ content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(sys(14, .bold)).foregroundColor(p.primary)
            content()
        }
    }
}
