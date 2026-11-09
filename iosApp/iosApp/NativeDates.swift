import SwiftUI
import UIKit
import Shared

/// أدوات التاريخ الثلاث بواجهة النظام (SwiftUI) بطراز الرئيسية (NativeHome.swift): ترويسة بتدرج لون التطبيق ونجمته،
/// وشريط علوي ثابت بزر الرجوع يصير زجاجاً بعنوان الصفحة حين تمر الترويسة تحته، وبطاقة نتيجة خضراء، وبطاقات بيضاء:
/// - hijri: المحوّل بين التقويمين (يوم وشهر وسنة) وتقويم الشهر الهجري (لمس يومه يحوّله)
/// - age: العمر بالميلادي والهجري من تاريخ الميلاد بأي التقويمين
/// - events: المناسبات القادمة والعدّ إلى المختارة منها كل ثانية
/// — كما في DateScreens.kt. الحالة كلها منسّقة من Kotlin (NativeDates.ios.kt) مع الإشعار MizanDatesState، واللمس يعود إليه.
enum NativeDatesScreen {
    static func install() {
        NativeDates.shared.attach { session in DatesHostController(session: session) }
    }
}

final class DatesHostController: UIHostingController<DatesRoot> {
    init(session: NativeDatesSession) {
        let model = DatesModel(session: session)
        super.init(rootView: DatesRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        // الهوامش من Kotlin، ولوحة المفاتيح وحدها تُحسب هنا (حقل السنة)
        if #available(iOS 16.4, *) { safeAreaRegions = .keyboard }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

struct DatesState: Decodable {
    struct Option: Decodable, Identifiable { let key, label: String; var id: String { key } }
    struct Fields: Decodable {
        let dayLabel, monthLabel, yearLabel, suffix, year, yearKey: String
        let day, month: Int
        let days, months: [String]
    }
    struct Cell: Decodable, Identifiable { let n: Int; let d, g, label: String; let today: Bool; var id: Int { n } }
    struct MonthGrid: Decodable {
        let key: Int
        let title, prev, next, hint: String
        let prevOk, nextOk: Bool
        let weekdays: [String]
        let lead: Int
        let cells: [Cell]
    }
    struct HijriPart: Decodable {
        let title, subtitle, resultLabel, main, sub, dir, todayLabel, note: String
        let dirs: [Option]
        let fields: Fields
        let calendar: MonthGrid
    }
    struct Stat: Decodable, Identifiable { let label, value: String; var id: String { label } }
    struct Tile: Decodable, Identifiable { let value, label: String; var id: String { label } }
    struct AgePart: Decodable {
        let title, subtitle, resultLabel, main, sub, birthLabel, mode: String
        let stats: [Stat]?
        let tiles: [Tile]?
        let modes: [Option]
        let fields: Fields
    }
    struct Hero: Decodable { let emoji, name, hijri, greg, desc: String; let at: Double }
    struct Item: Decodable, Identifiable { let key, emoji, name, date, left: String; var id: String { key } }
    struct EventsPart: Decodable {
        let title, subtitle, note: String
        let hero: Hero?
        let units: [String]
        let selected: Int
        let items: [Item]
    }
    struct Proof: Decodable, Identifiable {
        let kind, title, body, source: String
        let ayah, tr, hadith: String?
        var id: String { title + source }
    }
    struct Evidence: Decodable { let title, sub: String; let kinds: [String: String]; let points: [Proof] }

    let kind: String
    let rtl, dark: Bool
    let top, bottom, start, end, barStart: Double
    let back, close: String
    let hijri: HijriPart?
    let age: AgePart?
    let events: EventsPart?
    let evidence: Evidence
    let c: HomeState.Colors

    var title: String { hijri?.title ?? age?.title ?? events?.title ?? "" }
    var subtitle: String { hijri?.subtitle ?? age?.subtitle ?? events?.subtitle ?? "" }

    static func decode(_ json: String) -> DatesState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(DatesState.self, from: data)
    }
}

final class DatesModel: ObservableObject {
    let session: NativeDatesSession
    @Published private(set) var state: DatesState? { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    var onStyle: ((Bool, Bool) -> Void)?
    private var token: NSObjectProtocol?

    init(session: NativeDatesSession) {
        self.session = session
        state = DatesState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanDatesState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = DatesState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit { if let t = token { NotificationCenter.default.removeObserver(t) } }

    func back() { session.back() }
    func day(_ v: Int) { session.day(value: Int32(v)) }
    func month(_ v: Int) { session.month(value: Int32(v)) }
    func year(_ text: String) { session.year(text: text) }
    func dir(_ key: String) { session.dir(key: key) }
    func today() { session.today() }
    func calendar(_ delta: Int) { session.calendar(delta: Int32(delta)) }
    func pick(_ day: Int) { session.pick(day: Int32(day)) }
    func mode(_ key: String) { session.mode(key: key) }
    func select(_ index: Int) { session.select(index: Int32(index)) }
}

// MARK: - الشاشة

struct DatesRoot: View {
    @ObservedObject var model: DatesModel
    var body: some View {
        Group {
            if let s = model.state { DatesView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea(.container, edges: .all)
    }
}

private struct DatesHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private extension View {
    /// سحب الصفحة يخفي لوحة المفاتيح (iOS 16)
    @ViewBuilder func datesDismissKeyboard() -> some View {
        if #available(iOS 16.0, *) { self.scrollDismissesKeyboard(.interactively) } else { self }
    }
}

/// أيقونة الأداة في الترويسة (TOOL_ICONS في HomeScreen.kt)
private func datesSymbol(_ kind: String) -> String {
    switch kind {
    case "age":
        if #available(iOS 16.0, *) { return "birthday.cake.fill" }
        return "gift.fill"
    case "events": return "calendar.badge.clock"
    default: return "calendar"
    }
}

struct DatesView: View {
    let s: DatesState
    let p: HomePalette
    let model: DatesModel
    @State private var headerBottom: CGFloat = .greatestFiniteMagnitude

    /// شفافية الشريط الزجاجي: 0 والصفحة في أعلاها، و1 حين يمر طرف الترويسة السفلي تحته (GlassTopBar)
    private var glass: Double {
        let bar = CGFloat(s.top) + 52, ramp: CGFloat = 36
        return Double(min(max((bar + ramp - headerBottom) / ramp, 0), 1))
    }

    var body: some View {
        ZStack(alignment: .top) {
            p.background.ignoresSafeArea()
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 0) {
                    DatesHeader(title: s.title, subtitle: s.subtitle, symbol: datesSymbol(s.kind), s: s, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: DatesHeaderBottomKey.self, value: g.frame(in: .named("dates")).maxY)
                        })
                    VStack(spacing: 14) {
                        if let h = s.hijri { HijriContent(h: h, s: s, p: p, model: model) }
                        if let a = s.age { AgeContent(a: a, s: s, p: p, model: model) }
                        if let e = s.events { EventsContent(e: e, p: p, model: model) }
                        DatesEvidence(e: s.evidence, p: p)
                    }
                    .padding(.top, 6)
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "dates")
            .onPreferenceChange(DatesHeaderBottomKey.self) { headerBottom = $0 }
            .datesDismissKeyboard()
            DatesTopBar(title: s.title, s: s, p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }
}

// MARK: الترويسة والشريط

/// ترويسة بطراز الرئيسية: تدرج من لون التطبيق الفاتح ونجمة باهتة، وأيقونة الأداة والعنوان والوصف، وتحت الشريط العلوي
private struct DatesHeader: View {
    let title: String
    let subtitle: String
    let symbol: String
    let s: DatesState
    let p: HomePalette
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .semibold)).foregroundColor(p.primary)
                .frame(width: 46, height: 46)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.gold.opacity(0.18)))
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(sys(28, .heavy)).foregroundColor(p.primary).lineLimit(2).minimumScaleFactor(0.8)
                Text(subtitle).font(sys(14)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(.leading, CGFloat(s.start) + 4)
        .padding(.trailing, CGFloat(s.end) + 4)
        .padding(.top, CGFloat(s.top) + 52 + 6)
        .padding(.bottom, 16)
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

/// الشريط العلوي الثابت: زر الرجوع دائماً، والزجاج وعنوان صغير حين تمر الترويسة تحته
private struct DatesTopBar: View {
    let title: String
    let s: DatesState
    let p: HomePalette
    let amount: Double
    let onBack: () -> Void
    var body: some View {
        ZStack {
            Text(title)
                .font(sys(16, .medium)).foregroundColor(p.onSurface).lineLimit(1)
                .padding(.horizontal, 64)
                .opacity(min(max((amount - 0.4) / 0.6, 0), 1))
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
                Spacer(minLength: 0)
            }
            .padding(.leading, CGFloat(s.barStart))
        }
        .frame(maxWidth: .infinity)
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

// MARK: الأجزاء المشتركة

/// بطاقة بيضاء بحافة رفيعة كبطاقات الرئيسية
private struct DatesCard<Content: View>: View {
    let p: HomePalette
    var title: String? = nil
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let t = title { Text(t).font(sys(18, .heavy)).foregroundColor(p.primary).accessibilityAddTraits(.isHeader) }
            content()
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

/// بطاقة النتيجة بتدرج لون التطبيق (ResultCard): العنوان، فالقيمة كبيرة، فشارة ذهبية، ثم ما يُزاد تحتها
private struct DatesHero<Extra: View>: View {
    let label: String
    let value: String
    let badge: String?
    let p: HomePalette
    @ViewBuilder let extra: () -> Extra
    var body: some View {
        VStack(spacing: 8) {
            VStack(spacing: 6) {
                Text(label).font(sys(14)).foregroundColor(.white.opacity(0.85)).multilineTextAlignment(.center)
                Text(value).font(sys(30, .bold)).foregroundColor(.white)
                    .multilineTextAlignment(.center).minimumScaleFactor(0.7)
                    .modifier(NumericRoll(value: value))
                if let b = badge {
                    Text(b).font(sys(12, .bold)).foregroundColor(.white).multilineTextAlignment(.center)
                        .padding(.horizontal, 14).padding(.vertical, 5)
                        .background(Capsule().fill(p.gold.opacity(0.25)))
                }
            }
            .accessibilityElement(children: .combine)
            extra()
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .background(
            ZStack(alignment: .bottomLeading) {
                LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                StarEmblem(color: .white.opacity(0.06), radius: 72, stroke: 1).offset(x: -40, y: 50)
            }
            .environment(\.layoutDirection, .leftToRight)
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

/// حقل يشبه القائمة المنسدلة
private struct DatesField: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack {
            Text(text).font(sys(16)).foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.8)
            Spacer(minLength: 4)
            Image(systemName: "chevron.up.chevron.down").font(.system(size: 12, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
        }
        .padding(.horizontal, 14).padding(.vertical, 13)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
        .contentShape(Rectangle())
    }
}

/// اليوم والشهر من قائمتين، والسنة بالكتابة، كلها تعود إلى Kotlin (أسماء الأشهر وأيام الشهر منه)
private struct DatesFieldsView: View {
    let f: DatesState.Fields
    let close: String
    let p: HomePalette
    let model: DatesModel

    private func label(_ t: String) -> some View {
        Text(t).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant).padding(.leading, 4)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 8) {
                VStack(alignment: .leading, spacing: 6) {
                    label(f.dayLabel)
                    Menu {
                        ForEach(Array(f.days.enumerated()), id: \.offset) { i, t in
                            Button { model.day(i + 1) } label: {
                                if i + 1 == f.day { Label(t, systemImage: "checkmark") } else { Text(t) }
                            }
                        }
                    } label: {
                        DatesField(text: f.days.indices.contains(f.day - 1) ? f.days[f.day - 1] : "", p: p)
                    }
                    .accessibilityLabel(f.dayLabel)
                    .accessibilityValue(f.days.indices.contains(f.day - 1) ? f.days[f.day - 1] : "")
                }
                .frame(width: 108)
                VStack(alignment: .leading, spacing: 6) {
                    label(f.monthLabel)
                    Menu {
                        ForEach(Array(f.months.enumerated()), id: \.offset) { i, t in
                            Button { model.month(i + 1) } label: {
                                if i + 1 == f.month { Label(t, systemImage: "checkmark") } else { Text(t) }
                            }
                        }
                    } label: {
                        DatesField(text: f.months.indices.contains(f.month - 1) ? f.months[f.month - 1] : "", p: p)
                    }
                    .accessibilityLabel(f.monthLabel)
                    .accessibilityValue(f.months.indices.contains(f.month - 1) ? f.months[f.month - 1] : "")
                }
                .frame(maxWidth: .infinity)
            }
            DatesYearField(f: f, close: close, p: p) { model.year($0) }
        }
    }
}

/// السنة بلوحة الأرقام؛ تأخذ ما يرسله Kotlin حين يتغير yearKey (اليوم، أو يوم من التقويم، أو تبديل التقويم)
private struct DatesYearField: View {
    let f: DatesState.Fields
    let close: String
    let p: HomePalette
    let onChange: (String) -> Void
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(f.yearLabel).font(sys(13, .medium)).foregroundColor(p.onSurfaceVariant).padding(.leading, 4)
                .accessibilityHidden(true)
            HStack(spacing: 8) {
                TextField(f.yearLabel, text: $text)
                    .keyboardType(.numberPad)
                    .focused($focused)
                    .font(sys(17, .semibold)).foregroundColor(p.onSurface)
                Text(f.suffix).font(sys(15)).foregroundColor(p.onSurfaceVariant)
                if focused {
                    Button { focused = false } label: {
                        Image(systemName: "keyboard.chevron.compact.down")
                            .font(.system(size: 15, weight: .semibold)).foregroundColor(p.primary)
                            .frame(width: 32, height: 32)
                    }
                    .buttonStyle(PressScale(scale: 0.9))
                    .accessibilityLabel(close)
                }
            }
            .padding(.horizontal, 14).padding(.vertical, 9)
            .frame(minHeight: 48)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.background.opacity(0.7)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(focused ? p.primary : p.outlineVariant, lineWidth: focused ? 1.5 : 1))
        }
        .onAppear { if text != f.year { text = f.year } }
        .onChange(of: f.yearKey) { _ in text = f.year }
        .onChange(of: text) { v in onChange(v) }
    }
}

/// اختيار من اثنين بمقطع النظام
private struct DatesSegmented: View {
    let options: [DatesState.Option]
    let selected: String
    let onSelect: (String) -> Void
    var body: some View {
        Picker("", selection: Binding(get: { selected }, set: { v in
            if v != selected { UISelectionFeedbackGenerator().selectionChanged() }
            onSelect(v)
        })) {
            ForEach(options) { o in Text(o.label).tag(o.key) }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }
}

/// ملاحظة على خلفية لون التطبيق الفاتح (Note)
private struct DatesNote: View {
    let text: String
    let p: HomePalette
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle.fill").font(.system(size: 17)).foregroundColor(p.primary).accessibilityHidden(true)
            Text(text).font(sys(14)).foregroundColor(p.onPrimaryContainer).frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(p.primaryContainer))
    }
}

private struct DatesArrow: View {
    let symbol: String
    let label: String
    let enabled: Bool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 15, weight: .bold)).foregroundColor(p.primary)
                .frame(width: 38, height: 38).background(Circle().fill(p.primaryContainer))
        }
        .buttonStyle(PressScale(scale: 0.9))
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.35)
        .accessibilityLabel(label)
    }
}

// MARK: - المحوّل والتقويم الهجري

private struct HijriContent: View {
    let h: DatesState.HijriPart
    let s: DatesState
    let p: HomePalette
    let model: DatesModel
    var body: some View {
        DatesHero(label: h.resultLabel, value: h.main, badge: h.sub, p: p) { EmptyView() }
        DatesCard(p: p) {
            DatesSegmented(options: h.dirs, selected: h.dir) { model.dir($0) }
            DatesFieldsView(f: h.fields, close: s.close, p: p, model: model)
            Button { model.today() } label: {
                Label(h.todayLabel, systemImage: "calendar")
                    .font(sys(14, .semibold)).foregroundColor(p.primary)
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(Capsule().fill(p.primaryContainer))
            }
            .buttonStyle(PressScale(scale: 0.95))
        }
        HijriMonthCard(cal: h.calendar, p: p, model: model)
        DatesNote(text: h.note, p: p)
    }
}

/// تقويم الشهر الهجري: سهمان للشهر السابق والتالي، والأيام بتاريخها الميلادي، واليوم الحالي بلون التطبيق
private struct HijriMonthCard: View {
    let cal: DatesState.MonthGrid
    let p: HomePalette
    let model: DatesModel
    @State private var forward = true

    var body: some View {
        let still = UIAccessibility.isReduceMotionEnabled
        DatesCard(p: p) {
            HStack {
                DatesArrow(symbol: "chevron.backward", label: cal.prev, enabled: cal.prevOk, p: p) { forward = false; model.calendar(-1) }
                Text(cal.title).font(sys(17, .bold)).foregroundColor(p.onSurface)
                    .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                    .modifier(NumericRoll(value: cal.title))
                    .accessibilityAddTraits(.isHeader)
                DatesArrow(symbol: "chevron.forward", label: cal.next, enabled: cal.nextOk, p: p) { forward = true; model.calendar(1) }
            }
            // الشهر الجديد ينزلق من جهة السهم المضغوط (الجهتان تتبعان اتجاه اللغة)
            ZStack {
                HijriGrid(cal: cal, p: p) { model.pick($0) }
                    .id(cal.key)
                    .transition(still ? AnyTransition.opacity : AnyTransition.asymmetric(
                        insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity),
                        removal: .opacity
                    ))
            }
            .animation(still ? nil : .easeOut(duration: 0.28), value: cal.key)
            .clipped()
            Text(cal.hint).font(sys(12)).foregroundColor(p.onSurfaceVariant)
        }
    }
}

private struct HijriGrid: View {
    let cal: DatesState.MonthGrid
    let p: HomePalette
    let onPick: (Int) -> Void

    var body: some View {
        let rows = cal.cells.isEmpty ? 0 : (cal.lead + cal.cells.count + 6) / 7
        VStack(spacing: 3) {
            HStack(spacing: 3) {
                ForEach(Array(cal.weekdays.enumerated()), id: \.offset) { _, w in
                    Text(w).font(sys(11, .medium)).foregroundColor(p.onSurfaceVariant)
                        .lineLimit(1).minimumScaleFactor(0.6).frame(maxWidth: .infinity)
                }
            }
            .accessibilityHidden(true)
            ForEach(0..<rows, id: \.self) { r in
                HStack(spacing: 3) {
                    ForEach(0..<7, id: \.self) { c in
                        let i = r * 7 + c - cal.lead
                        if i >= 0 && i < cal.cells.count {
                            HijriDayCell(cell: cal.cells[i], p: p) { onPick(cal.cells[i].n) }
                        } else {
                            Color.clear.frame(maxWidth: .infinity).aspectRatio(0.85, contentMode: .fit)
                        }
                    }
                }
            }
        }
    }
}

private struct HijriDayCell: View {
    let cell: DatesState.Cell
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 1) {
                Text(cell.d).font(sys(15, .bold)).foregroundColor(cell.today ? .white : p.onSurface)
                Text(cell.g).font(sys(9)).foregroundColor(cell.today ? .white.opacity(0.85) : p.onSurfaceVariant)
                    .environment(\.layoutDirection, .leftToRight)
            }
            .lineLimit(1).minimumScaleFactor(0.6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                Group {
                    if cell.today {
                        LinearGradient(colors: [p.hero1, p.hero2], startPoint: .topLeading, endPoint: .bottomTrailing)
                    } else {
                        p.primaryContainer.opacity(0.4)
                    }
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).strokeBorder(cell.today ? p.gold : Color.clear, lineWidth: 1.5))
            .aspectRatio(0.85, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.85))
        .frame(maxWidth: .infinity)
        .accessibilityLabel(cell.label)
    }
}

// MARK: - العمر

private struct AgeContent: View {
    let a: DatesState.AgePart
    let s: DatesState
    let p: HomePalette
    let model: DatesModel
    var body: some View {
        DatesHero(label: a.resultLabel, value: a.main, badge: a.sub, p: p) { EmptyView() }
        if let stats = a.stats {
            DatesCard(p: p) {
                ForEach(stats) { st in
                    HStack(spacing: 8) {
                        Text(st.label).font(sys(14)).foregroundColor(p.onSurfaceVariant)
                        Spacer(minLength: 8)
                        Text(st.value).font(sys(16, .bold)).foregroundColor(p.onSurface).multilineTextAlignment(.trailing)
                            .modifier(NumericRoll(value: st.value))
                    }
                    .padding(.horizontal, 14).padding(.vertical, 11)
                    .background(RoundedRectangle(cornerRadius: 12, style: .continuous).fill(p.background.opacity(0.7)))
                    .accessibilityElement(children: .combine)
                }
                if let tiles = a.tiles {
                    HStack(spacing: 8) {
                        ForEach(tiles) { t in
                            VStack(spacing: 2) {
                                Text(t.value).font(sys(20, .bold)).foregroundColor(p.primary)
                                    .lineLimit(1).minimumScaleFactor(0.6)
                                    .modifier(NumericRoll(value: t.value))
                                Text(t.label).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(1).minimumScaleFactor(0.7)
                            }
                            .padding(.vertical, 12).padding(.horizontal, 6)
                            .frame(maxWidth: .infinity)
                            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.gold.opacity(0.12)))
                            .accessibilityElement(children: .combine)
                        }
                    }
                }
            }
        }
        DatesCard(p: p, title: a.birthLabel) {
            DatesSegmented(options: a.modes, selected: a.mode) { model.mode($0) }
            DatesFieldsView(f: a.fields, close: s.close, p: p, model: model)
        }
    }
}

// MARK: - المناسبات

private struct EventsContent: View {
    let e: DatesState.EventsPart
    let p: HomePalette
    let model: DatesModel
    var body: some View {
        if let h = e.hero {
            DatesHero(label: h.emoji + " " + h.name, value: h.hijri, badge: h.greg, p: p) {
                EventsCountdown(at: h.at, units: e.units).padding(.top, 6)
                Text(h.desc).font(sys(14)).foregroundColor(.white.opacity(0.9)).multilineTextAlignment(.center)
            }
        }
        ForEach(Array(e.items.enumerated()), id: \.element.id) { i, it in
            EventsRow(it: it, on: i == e.selected, p: p) { model.select(i) }
        }
        DatesNote(text: e.note, p: p)
    }
}

/// العدّ إلى بداية يوم المناسبة: أيام وساعات ودقائق وثوانٍ، يتقدم كل ثانية هنا
private struct EventsCountdown: View {
    let at: Double
    let units: [String]

    private func two(_ v: Int) -> String { v < 10 ? "0\(v)" : "\(v)" }

    var body: some View {
        TimelineView(.periodic(from: wholeSecond(), by: 1)) { ctx in
            let left = max(0, Int(at - ctx.date.timeIntervalSince1970))
            let values = [left / 86400, left % 86400 / 3600, left % 3600 / 60, left % 60]
            HStack(spacing: 8) {
                ForEach(0..<4, id: \.self) { i in
                    VStack(spacing: 2) {
                        Text(two(values[i])).font(sys(24, .heavy)).monospacedDigit().foregroundColor(.white)
                            .modifier(NumericRoll(value: two(values[i])))
                        Text(units.indices.contains(i) ? units[i] : "").font(sys(12, .medium)).foregroundColor(.white.opacity(0.8))
                            .lineLimit(1).minimumScaleFactor(0.7)
                    }
                    .padding(.vertical, 10)
                    .frame(maxWidth: .infinity)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Color.white.opacity(0.12)))
                    .accessibilityElement(children: .combine)
                }
            }
        }
    }
}

private struct EventsRow: View {
    let it: DatesState.Item
    let on: Bool
    let p: HomePalette
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(it.emoji).font(.system(size: 22)).accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text(it.name).font(sys(16, .bold)).foregroundColor(p.onSurface)
                    Text(it.date).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                }
                Spacer(minLength: 8)
                Text(it.left).font(sys(14, .heavy)).foregroundColor(p.goldDeep).multilineTextAlignment(.trailing)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(on ? p.primaryContainer : p.surface))
            .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(on ? p.primary : p.outlineVariant.opacity(0.8), lineWidth: on ? 1.5 : 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.97))
        .animation(.easeOut(duration: 0.25), value: on)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(on ? [.isButton, .isSelected] : [.isButton])
    }
}

// MARK: - الأدلة

/// الأدلة: آيات وأحاديث وأقوال، تنفتح بلمسة (EvidenceCard)
private struct DatesEvidence: View {
    let e: DatesState.Evidence
    let p: HomePalette
    @State private var open = false

    private func color(_ kind: String) -> Color {
        switch kind {
        case "quran": return p.goldDeep
        case "hadith": return p.primary
        default: return p.goldDeep.opacity(0.85)
        }
    }

    private func symbol(_ kind: String) -> String {
        switch kind {
        case "quran": return "book.fill"
        case "hadith": return "quote.opening"
        case "consensus": return "person.3.fill"
        default: return "graduationcap.fill"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                if UIAccessibility.isReduceMotionEnabled { open.toggle() } else { withAnimation(.easeInOut(duration: 0.3)) { open.toggle() } }
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "book.closed.fill").foregroundColor(p.goldDeep)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(e.title).font(sys(16, .bold)).foregroundColor(p.onSurface)
                        Text(e.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                    }
                    Spacer()
                    Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .rotationEffect(.degrees(open ? 180 : 0))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(open ? [.isButton, .isSelected] : [.isButton])
            if open {
                VStack(alignment: .leading, spacing: 14) {
                    ForEach(e.points) { pt in
                        VStack(alignment: .leading, spacing: 6) {
                            Label(e.kinds[pt.kind] ?? pt.kind, systemImage: symbol(pt.kind))
                                .font(sys(11, .bold)).foregroundColor(color(pt.kind))
                                .padding(.horizontal, 8).padding(.vertical, 3)
                                .background(Capsule().fill(color(pt.kind).opacity(0.12)))
                            Text(pt.title).font(sys(14, .bold)).foregroundColor(p.onSurface)
                            if let a = pt.ayah {
                                Text(a).font(quranFont(19)).foregroundColor(p.onSurface).lineSpacing(8)
                                    .frame(maxWidth: .infinity, alignment: .trailing)
                                    .multilineTextAlignment(.trailing)
                                    .padding(10)
                                    .background(RoundedRectangle(cornerRadius: 12).fill(p.gold.opacity(0.08)))
                                    .environment(\.layoutDirection, .rightToLeft)
                            }
                            if let t = pt.tr { Text(t).font(sys(12)).foregroundColor(p.onSurfaceVariant) }
                            if let h = pt.hadith {
                                Text(h).font(sys(14)).foregroundColor(p.onSurface).lineSpacing(6)
                                    .padding(10)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(RoundedRectangle(cornerRadius: 12).fill(p.gold.opacity(0.08)))
                                    .environment(\.layoutDirection, .rightToLeft)
                            }
                            Text(pt.body).font(sys(14)).foregroundColor(p.onSurface)
                            Text(pt.source).font(sys(12, .bold)).foregroundColor(color(pt.kind))
                        }
                    }
                }
                .padding(.horizontal, 16).padding(.bottom, 16)
                .transition(.opacity)
            }
        }
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.gold.opacity(0.45), lineWidth: 1))
    }
}
