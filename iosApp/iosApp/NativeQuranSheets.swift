import SwiftUI
import UIKit
import Shared

/// أوراق القرآن من النظام بمحتوى SwiftUI: التفسير، وخيارات الآية، ومشاركة الآية والقصة صورةً (NativeShareSheet.swift).
/// Kotlin (ui/quran/NativeQuranSheets.kt) يعرضها ويغلقها ويرسل حالتها JSON، وكل لمسة تعود إليه (session.send)؛
/// المنطق كله والنصوص في Kotlin كما في أوراق Compose نفسها (TafsirSheet.kt و AyahActions.kt و AyahShare.kt و StoryShare.kt).
enum QuranSheets {
    /// يبني متحكم الورقة لأنواعها هنا، و nil لغيرها (يستدعيه مصنع NativeSheetsUI)
    static func make(_ session: NativeSheetSession) -> UIViewController? {
        switch session.kind {
        case "tafsir":
            let model = SheetModel<TafsirSheetState>(session: session)
            return SheetHostController(model: model, root: AnyView(TafsirSheetView(model: model)))
        case "ayahActions":
            let model = SheetModel<AyahActionsState>(session: session)
            return QuranSheetHostController(model: model, root: AnyView(AyahActionsView(model: model)), fit: {
                guard let s = model.state else { return nil }
                return AnyView(AyahActionsContent(s: s, model: model).fixedSize(horizontal: false, vertical: true))
            })
        case "shareCard":
            let model = SheetModel<ShareCardState>(session: session)
            return SheetHostController(model: model, root: AnyView(ShareCardView(model: model)))
        default:
            return nil
        }
    }
}

/// متحكم ورقة بارتفاع محتواها (خيارات الآية). Kotlin يضبط ارتفاعي الورقة المعتادين قبل عرضها ثم يعرضها؛
/// والعرض يحمّل العرض (viewDidLoad) فيُستبدل بهما هنا ارتفاع المحتوى نفسه قبل أن تبدأ حركة الظهور
final class QuranSheetHostController: UIHostingController<AnyView> {
    private let fit: () -> AnyView?
    private var style: (dark: Bool, rtl: Bool, background: String)?
    private var fitted: (width: CGFloat, height: CGFloat)?

    init<S: SheetStyled>(model: SheetModel<S>, root: AnyView, fit: @escaping () -> AnyView?) {
        self.fit = fit
        super.init(rootView: root)
        modalPresentationStyle = .pageSheet
        model.onStyle = { [weak self] dark, rtl, background in self?.restyle(dark: dark, rtl: rtl, background: background) }
        // لا يُلمس العرض هنا حتى لا يُحمَّل قبل أن يضبط Kotlin الورقة
        if let s = model.state { style = (s.dark, s.rtl, s.c.background) }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }

    private func restyle(dark: Bool, rtl: Bool, background: String) {
        style = (dark, rtl, background)
        guard isViewLoaded else { return }
        applyAppStyle(dark: dark, rtl: rtl)
        view.backgroundColor = UIColor(HomePalette.color(background))
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        if let st = style { restyle(dark: st.dark, rtl: st.rtl, background: st.background) }
        guard let sheet = sheetPresentationController else { return }
        if #available(iOS 16.0, *) {
            sheet.detents = [.custom(identifier: UISheetPresentationController.Detent.Identifier(rawValue: "mizan.fit")) { [weak self] ctx in
                guard let self = self else { return nil }
                return min(ctx.maximumDetentValue, self.contentHeight())
            }]
        } else {
            sheet.detents = [.medium()]
        }
        sheet.prefersGrabberVisible = true
    }

    /// ارتفاع المحتوى بعرض الورقة (بلا المنطقة الآمنة السفلى، فالنظام يضيفها)
    private func contentHeight() -> CGFloat {
        let width = view.bounds.width > 0 ? view.bounds.width : UIScreen.main.bounds.width
        if let f = fitted, f.width == width { return f.height }
        var height: CGFloat = 200
        if let content = fit() {
            height = UIHostingController(rootView: content).sizeThatFits(in: CGSize(width: width, height: 2000)).height
        }
        height = min(max(height, 120), 600)
        fitted = (width, height)
        return height
    }
}

// MARK: - أجزاء مشتركة

/// علامة الآية: نجمة ذهبية بداخلها رقمها بالأرقام المشرقية (AyahMarker في QuranUi.kt)
struct QuranSheetAyahMarker: View {
    let num: String
    let p: HomePalette
    var size: CGFloat = 28
    var numberSize: CGFloat = 11

    var body: some View {
        ZStack {
            Star8().stroke(p.goldDeep, lineWidth: 1.5).padding(1.5)
            Circle().fill(p.gold.opacity(0.14)).frame(width: size * 0.66, height: size * 0.66)
            Text(num).font(.system(size: num.count >= 3 ? numberSize * 0.82 : numberSize, weight: .semibold))
                .foregroundColor(p.onSurface).lineLimit(1).minimumScaleFactor(0.6)
        }
        .frame(width: size, height: size)
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityHidden(true)
    }
}

/// زر إغلاق دائري خارج شريط التنقل
struct QuranSheetCloseButton: View {
    let label: String
    let p: HomePalette
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "xmark").font(sys(13, .bold)).foregroundColor(p.onSurfaceVariant)
                .frame(width: 30, height: 30)
                .background(Circle().fill(p.onSurfaceVariant.opacity(0.12)))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

// MARK: - التفسير

struct TafsirSheetState: SheetStyled {
    struct Source: Decodable, Identifiable {
        let id, label: String
        let selected: Bool
    }
    struct Seg: Decodable {
        let t: String
        let q: Bool
    }
    let rtl, dark: Bool
    let c: HomeState.Colors
    let onPrimary, secondary, paper, ink, marker: String
    let title, close: String
    let ayah: Int
    let num, ref, place: String
    let marked: Bool
    let bookmarkLabel, copyLabel, copiedLabel, shareLabel: String
    let verse, end: String
    let listen: String?
    let sources: [Source]
    let more, meta: String
    let loading: Bool
    let range: String?
    let srcRtl: Bool
    let paras: [[Seg]]
    let prev, next: String
    let hasPrev, hasNext: Bool
}

struct TafsirSheetView: View {
    @ObservedObject var model: SheetModel<TafsirSheetState>
    var body: some View {
        Group {
            if let s = model.state { TafsirSheetBody(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
    }
}

/// على ورق المصحف: رأس الآية (رقمها وموضعها، والحفظ والنسخ والمشاركة)، ونصها بخط المصحف، والاستماع منها،
/// ثم المصادر ومكتبة التفاسير، والتفسير بفقراته (والآيات المقتبسة بخط المصحف)، والآية السابقة والتالية
struct TafsirSheetBody: View {
    let s: TafsirSheetState
    let p: HomePalette
    let model: SheetModel<TafsirSheetState>
    @State private var copied = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var paper: Color { HomePalette.color(s.paper) }
    private var ink: Color { HomePalette.color(s.ink) }
    private var marker: Color { HomePalette.color(s.marker) }
    private var secondary: Color { HomePalette.color(s.secondary) }
    private var onPrimary: Color { HomePalette.color(s.onPrimary) }

    var body: some View {
        NavigationView {
            ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    VStack(alignment: .leading, spacing: 14) {
                        header.id("tafsir-top")
                        verse
                        if let l = s.listen { listenButton(l) }
                        Rectangle().fill(p.outlineVariant).frame(height: 0.5)
                        sourceChips
                        Text(s.meta).font(sys(12, .bold)).foregroundColor(secondary)
                        tafsir
                        arrows.padding(.top, 4)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 6)
                    .padding(.bottom, 28)
                }
                // آية أخرى: من أول الورقة
                .onChange(of: s.ayah) { _ in
                    if reduceMotion {
                        proxy.scrollTo("tafsir-top", anchor: .top)
                    } else {
                        withAnimation(.easeOut(duration: 0.25)) { proxy.scrollTo("tafsir-top", anchor: .top) }
                    }
                }
            }
            .background(paper.ignoresSafeArea())
            .navigationTitle(s.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    SheetCloseButton(label: s.close, p: p) { model.send("close") }
                }
            }
        }
        .navigationViewStyle(.stack)
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private var header: some View {
        HStack(spacing: 10) {
            QuranSheetAyahMarker(num: s.num, p: p, size: 40, numberSize: 14)
            VStack(alignment: .leading, spacing: 2) {
                Text(s.ref).font(sys(16, .semibold)).foregroundColor(p.onSurface)
                Text(s.place).font(sys(12)).foregroundColor(p.onSurfaceVariant)
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 4)
            iconButton(s.marked ? "bookmark.fill" : "bookmark", tint: s.marked ? p.gold : p.onSurfaceVariant, label: s.bookmarkLabel) {
                model.send("bookmark")
            }
            .accessibilityAddTraits(s.marked ? .isSelected : [])
            iconButton(copied ? "checkmark" : "doc.on.doc", tint: copied ? p.primary : p.onSurfaceVariant,
                       label: copied ? s.copiedLabel : s.copyLabel) {
                model.send("copy")
                flashCopied()
            }
            iconButton("square.and.arrow.up", tint: p.onSurfaceVariant, label: s.shareLabel) { model.send("share") }
        }
    }

    private func iconButton(_ symbol: String, tint: Color, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(sys(18, .medium)).foregroundColor(tint)
                .frame(width: 40, height: 40)
                .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.88))
        .accessibilityLabel(label)
    }

    /// رسالة «نُسخ» من Kotlin تظهر تحت الورقة، فيُعلَم بالنسخ هنا: علامة صح قليلاً، واهتزاز خفيف، وإعلان لـ VoiceOver
    private func flashCopied() {
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        UIAccessibility.post(notification: .announcement, argument: s.copiedLabel)
        copied = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
    }

    /// نص الآية من اليمين دائماً، وعلامة نهايتها برقمها بلون العلامة
    private var verse: some View {
        (Text(verbatim: s.verse) + Text(verbatim: "\u{00A0}" + s.end).foregroundColor(marker))
            .font(quranFont(24))
            .foregroundColor(ink)
            .lineSpacing(10)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
            .environment(\.layoutDirection, .rightToLeft)
            .textSelection(.enabled)
    }

    private func listenButton(_ label: String) -> some View {
        Button { model.send("listen") } label: {
            HStack(spacing: 6) {
                Image(systemName: "play.fill").font(sys(14, .semibold)).accessibilityHidden(true)
                Text(label).font(sys(15, .semibold)).lineLimit(1)
            }
            .foregroundColor(p.onPrimaryContainer)
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
            .background(Capsule().fill(p.primaryContainer))
        }
        .buttonStyle(PressScale())
    }

    /// المصادر الظاهرة (لغة الواجهة أولاً)، وفي آخرها زر مكتبة التفاسير
    private var sourceChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(s.sources) { o in
                    chip(o.label, selected: o.selected) { model.send("source", o.id) }
                }
                chip(s.more, selected: false) { model.send("library") }
            }
            .padding(.vertical, 2)
        }
    }

    private func chip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label).font(sys(14, .semibold)).lineLimit(1)
                .foregroundColor(selected ? onPrimary : p.onSurface)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(Capsule().fill(selected ? p.primary : p.surface.opacity(0.7)))
                .overlay(Capsule().strokeBorder(selected ? Color.clear : p.outlineVariant, lineWidth: 1))
        }
        .buttonStyle(PressScale(scale: 0.95))
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    @ViewBuilder
    private var tafsir: some View {
        if s.loading {
            ProgressView().frame(maxWidth: .infinity, minHeight: 80)
        } else {
            VStack(alignment: .leading, spacing: 10) {
                // بعض التفاسير تشرح عدة آيات معاً
                if let r = s.range {
                    Text(r).font(sys(14, .bold)).foregroundColor(secondary)
                }
                ForEach(Array(s.paras.enumerated()), id: \.offset) { _, para in
                    paragraph(para)
                        .font(sys(16))
                        .foregroundColor(p.onSurface)
                        .lineSpacing(8)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .textSelection(.enabled)
            .environment(\.layoutDirection, s.srcRtl ? .rightToLeft : .leftToRight)
        }
    }

    /// فقرة واحدة: الآيات المقتبسة ﴿...﴾ بخط المصحف ولون التطبيق
    private func paragraph(_ segs: [TafsirSheetState.Seg]) -> Text {
        segs.reduce(Text(verbatim: "")) { acc, seg in
            acc + (seg.q ? Text(verbatim: seg.t).font(quranFont(18)).foregroundColor(p.primary) : Text(verbatim: seg.t))
        }
    }

    /// الآية السابقة / التالية داخل السورة (السهمان ينقلبان مع اتجاه اللغة)
    private var arrows: some View {
        HStack(spacing: 10) {
            arrowButton(s.prev, symbol: "chevron.backward", iconFirst: true, enabled: s.hasPrev) { model.send("prev") }
            arrowButton(s.next, symbol: "chevron.forward", iconFirst: false, enabled: s.hasNext) { model.send("next") }
        }
    }

    private func arrowButton(_ label: String, symbol: String, iconFirst: Bool, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if iconFirst { Image(systemName: symbol).font(sys(14, .semibold)).accessibilityHidden(true) }
                Text(label).font(sys(15, .semibold)).lineLimit(1).minimumScaleFactor(0.8)
                if !iconFirst { Image(systemName: symbol).font(sys(14, .semibold)).accessibilityHidden(true) }
            }
            .foregroundColor(p.primary)
            .frame(maxWidth: .infinity, minHeight: 46)
            .background(RoundedRectangle(cornerRadius: 23, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale())
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
    }
}

// MARK: - خيارات الآية

struct AyahActionsState: SheetStyled {
    let rtl, dark: Bool
    let c: HomeState.Colors
    let onPrimary: String
    let num, ref, close: String
    let tafsir, listen, save, copy, share: String
    let marked: Bool
}

struct AyahActionsView: View {
    @ObservedObject var model: SheetModel<AyahActionsState>
    var body: some View {
        Group {
            if let s = model.state {
                AyahActionsContent(s: s, model: model)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                    .background(HomePalette(s.c).background.ignoresSafeArea())
            } else {
                Color.clear
            }
        }
    }
}

/// رقم الآية وموضعها وزر الإغلاق، ثم الخيارات: التفسير والاستماع (بلون التطبيق)، والحفظ، والنسخ، والمشاركة
struct AyahActionsContent: View {
    let s: AyahActionsState
    let model: SheetModel<AyahActionsState>

    var body: some View {
        let p = HomePalette(s.c)
        VStack(spacing: 12) {
            HStack(spacing: 8) {
                QuranSheetAyahMarker(num: s.num, p: p, size: 30, numberSize: 11)
                Text(s.ref).font(sys(16, .semibold)).foregroundColor(p.onSurface).lineLimit(1)
                    .accessibilityAddTraits(.isHeader)
                Spacer(minLength: 0)
                QuranSheetCloseButton(label: s.close, p: p) { model.send("close") }
            }
            HStack(alignment: .top, spacing: 4) {
                tile("book", s.tafsir, primary: true, p: p) { model.send("tafsir") }
                tile("play.fill", s.listen, primary: true, p: p) { model.send("listen") }
                tile(s.marked ? "bookmark.fill" : "bookmark", s.save, primary: false, tint: s.marked ? p.gold : nil, p: p) {
                    model.send("bookmark")
                }
                .accessibilityAddTraits(s.marked ? .isSelected : [])
                tile("doc.on.doc", s.copy, primary: false, p: p) { model.send("copy") }
                tile("square.and.arrow.up", s.share, primary: false, p: p) { model.send("share") }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 20)
        .padding(.bottom, 10)
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
    }

    private func tile(_ symbol: String, _ label: String, primary: Bool, tint: Color? = nil, p: HomePalette,
                      action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(sys(19, .semibold))
                    .foregroundColor(tint ?? (primary ? HomePalette.color(s.onPrimary) : p.onSurfaceVariant))
                    .frame(width: 50, height: 50)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(primary ? p.primary : p.onSurface.opacity(0.08)))
                    .accessibilityHidden(true)
                Text(label).font(sys(12, .medium)).foregroundColor(p.onSurface)
                    .lineLimit(1).minimumScaleFactor(0.7)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.92))
    }
}
