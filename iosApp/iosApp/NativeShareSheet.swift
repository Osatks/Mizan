import SwiftUI
import UIKit
import Shared

/// ورقة مشاركة الآية والقصة صورةً (shareCard): البطاقة ترسمها Compose خارج الشاشة بتصميمها نفسه وتحفظها PNG،
/// فتُعرض هنا معاينةً، ويُختار الشكل (بطاقة أو قصة) واللون والمعنى (كلها في Kotlin)، وتُشارك الصورة أو النص
/// بقائمة المشاركة في النظام فوق الورقة؛ وإذا تمت المشاركة أُخبر Kotlin فأغلق الورقة (NativeQuranSheets.kt).
struct ShareCardState: SheetStyled {
    struct Style: Decodable, Identifiable {
        let key, label, top, bottom, gold: String
        let selected: Bool
        var id: String { key }
    }
    let rtl, dark: Bool
    let c: HomeState.Colors
    let onPrimary: String
    let title, sub, close: String
    /// اسما الشكلين (بطاقة، قصة)، أو nil للقصة وحدها
    let formats: [String]?
    let story: Bool
    let styles: [Style]
    let meaningLabel: String?
    let withMeaning: Bool
    /// مسار PNG المعاينة (هي الصورة التي تُشارك)، و ready: رُسمت بالخيارات الحالية
    let preview: String?
    let ready: Bool
    let imageLabel, textLabel, shareText: String
}

struct ShareCardView: View {
    @ObservedObject var model: SheetModel<ShareCardState>
    var body: some View {
        Group {
            if let s = model.state { ShareCardBody(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
    }
}

struct ShareCardBody: View {
    let s: ShareCardState
    let p: HomePalette
    let model: SheetModel<ShareCardState>
    @State private var image: UIImage?
    @State private var loadedPath: String?

    private var onPrimary: Color { HomePalette.color(s.onPrimary) }

    var body: some View {
        NavigationView {
            ScrollView(.vertical) {
                VStack(spacing: 18) {
                    if let f = s.formats, f.count == 2 { formatPicker(f) }
                    preview
                    SheetCard(p: p) {
                        optionsRow.padding(.horizontal, 16).padding(.vertical, 10)
                    }
                    buttons
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
                    VStack(spacing: 1) {
                        Text(s.title).font(sys(16, .bold)).foregroundColor(p.onSurface).lineLimit(1)
                        Text(s.sub).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(1)
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
        .onAppear { load(s.preview) }
        .onChange(of: s.preview) { path in load(path) }
    }

    /// الصورة كلها في الذاكرة (لا قراءة مؤجلة من ملف قد يحذفه Kotlin بعد قليل)
    private func load(_ path: String?) {
        guard let path = path, path != loadedPath else { return }
        loadedPath = path
        if let data = FileManager.default.contents(atPath: path), let img = UIImage(data: data) {
            image = img
        }
    }

    private func formatPicker(_ f: [String]) -> some View {
        Picker(f[0], selection: Binding<Int>(
            get: { s.story ? 1 : 0 },
            set: { v in model.send("format", v == 1 ? "story" : "post") }
        )) {
            Text(verbatim: f[0]).tag(0)
            Text(verbatim: f[1]).tag(1)
        }
        .pickerStyle(.segmented)
        .frame(maxWidth: 300)
    }

    /// المعاينة هي الصورة نفسها (البطاقة بعرض 330، والقصة بعرض 250)، وتخفت قليلاً إلى أن تُرسم بالخيار الجديد
    private var preview: some View {
        let radius: CGFloat = s.story ? 16 : 18
        return ZStack {
            if let img = image {
                Image(uiImage: img).resizable().aspectRatio(contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
                    .opacity(s.ready ? 1 : 0.55)
            } else {
                RoundedRectangle(cornerRadius: radius, style: .continuous).fill(p.surface)
                    .aspectRatio(s.story ? 9.0 / 16.0 : 4.0 / 5.0, contentMode: .fit)
            }
            if !s.ready { ProgressView() }
        }
        .frame(maxWidth: s.story ? 250 : 330)
        .shadow(color: Color.black.opacity(0.16), radius: 16, x: 0, y: 8)
        .animation(.easeOut(duration: 0.2), value: s.ready)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(s.sub)
        .accessibilityAddTraits(.isImage)
    }

    /// الألوان الثلاثة، ومفتاح المعنى إن وُجد
    private var optionsRow: some View {
        HStack(spacing: 10) {
            ForEach(s.styles) { st in swatch(st) }
            Spacer(minLength: 8)
            if let m = s.meaningLabel {
                Toggle(isOn: Binding<Bool>(
                    get: { s.withMeaning },
                    set: { v in model.send("meaning", v ? "1" : "0") }
                )) {
                    Text(m).font(sys(15)).foregroundColor(p.onSurface).lineLimit(1)
                }
                .toggleStyle(SwitchToggleStyle(tint: p.primary))
                .fixedSize()
            }
        }
    }

    private func swatch(_ st: ShareCardState.Style) -> some View {
        Button { model.send("style", st.key) } label: {
            ZStack {
                Circle().fill(LinearGradient(
                    gradient: Gradient(colors: [HomePalette.color(st.top), HomePalette.color(st.bottom)]),
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))
                Circle().fill(HomePalette.color(st.gold)).frame(width: 8, height: 8)
            }
            .frame(width: 34, height: 34)
            .overlay(Circle().strokeBorder(st.selected ? p.primary : p.outlineVariant, lineWidth: st.selected ? 2.5 : 1))
            .scaleEffect(st.selected ? 1 : 0.86)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: st.selected)
            .frame(width: 40, height: 40)
            .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(st.label)
        .accessibilityAddTraits(st.selected ? .isSelected : [])
    }

    private var buttons: some View {
        HStack(spacing: 10) {
            Button { shareImage() } label: {
                HStack(spacing: 8) {
                    if s.ready {
                        Image(systemName: "photo").font(sys(16, .semibold)).accessibilityHidden(true)
                    } else {
                        ProgressView().progressViewStyle(CircularProgressViewStyle(tint: onPrimary))
                    }
                    Text(s.imageLabel).font(sys(16, .bold)).lineLimit(1)
                }
                .foregroundColor(onPrimary)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(p.primary))
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale())
            .disabled(!s.ready)
            Button { share([s.shareText]) } label: {
                HStack(spacing: 8) {
                    Image(systemName: "text.alignleft").font(sys(16, .semibold)).accessibilityHidden(true)
                    Text(s.textLabel).font(sys(16, .bold)).lineLimit(1)
                }
                .foregroundColor(p.onSurface)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder(p.outlineVariant, lineWidth: 1))
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale())
        }
    }

    /// الصورة نفسها (فيظهر «حفظ الصورة») ومعها النص؛ وإن لم تُرسم الصورة شاركت النص كما في Compose
    private func shareImage() {
        if let img = image, let path = s.preview, path == loadedPath {
            share([img, s.shareText])
        } else {
            share([s.shareText])
        }
    }

    private func share(_ items: [Any]) {
        guard let top = ShareCardPresenter.top() else { return }
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        let m = model
        // بعد أن تُغلق قائمة المشاركة نفسها تُغلق الورقة؛ وإن أُلغيت بقيت الورقة لاختيار آخر
        vc.completionWithItemsHandler = { _, completed, _, _ in
            guard completed else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { m.send("shared") }
        }
        // في iPad تُعرض نافذة منبثقة، ولا بد لها من موضع: وسط الورقة بلا سهم
        if let pop = vc.popoverPresentationController {
            pop.sourceView = top.view
            pop.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.midY, width: 0, height: 0)
            pop.permittedArrowDirections = []
        }
        top.present(vc, animated: true)
    }
}

/// أعلى متحكم ظاهر (الورقة نفسها): تُعرض قائمة المشاركة فوقه
enum ShareCardPresenter {
    static func top() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap { $0.windows }.first { $0.isKeyWindow } ?? scenes.first?.windows.first
        var top = window?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}
