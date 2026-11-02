import SwiftUI
import WidgetKit

// معرض أدوات ميزان وأنشطته الحية للمراجعة البصرية على المحاكي (ios-gallery.yml)، ولا يُنشر.
// كل تشغيل صفحة واحدة يلتقطها اختبار الواجهة (GalleryUITests)، بوسائط التشغيل:
//   -gallery-page check|tasbih|khatma|prayer|lock1|lock2|lock3|live   -gallery-lang ar   -gallery-dark 0|1
//   -gallery-width 359   (عرض النشاط على شاشة القفل: 408 في شاشة 6.9 بوصة، و 359 في أصغرها: iPhone SE و mini)
//   -gallery-live tasbih+prayer   (يبدأ أنشطة حية حقيقية يرسمها امتداد الأدوات في الجزيرة وعلى شاشة القفل)
// الأدوات تُرسم بمصادر MizanWidgets نفسها، بمقاسها في iPhone بشاشة 6.9 بوصة، وخلفيتها كما يرسمها WidgetKit.

@main
struct GalleryApp: App {
    var body: some Scene {
        WindowGroup { GalleryRoot() }
    }
}

enum Args {
    static func value(_ key: String) -> String? {
        let a = ProcessInfo.processInfo.arguments
        guard let i = a.firstIndex(of: "-" + key), i + 1 < a.count else { return nil }
        return a[i + 1]
    }

    static var page: String { value("gallery-page") ?? "check" }
    static var lang: String { value("gallery-lang") ?? "ar" }
    static var dark: Bool { value("gallery-dark") == "1" }
    static var live: String? { value("gallery-live") }
    static var lockWidth: CGFloat { value("gallery-width").flatMap(Double.init).map { CGFloat($0) } ?? 408 }
}

struct GalleryRoot: View {
    @State private var live = ""

    var body: some View {
        let dark = Args.dark
        ZStack {
            Wallpaper(dark: dark)
            if Args.page == "check" {
                CheckPage()
            } else if let f = Fixtures.all[Args.lang] {
                VStack(spacing: 14) {
                    Text(verbatim: "\(Args.page) · \(Args.lang) · \(dark ? "dark" : "light")" + (Args.page.hasPrefix("lock") ? " · \(Int(Args.lockWidth))pt" : ""))
                        .font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundColor(.white.opacity(0.7))
                        .accessibilityIdentifier("page-ready")
                    page(f)
                    Spacer(minLength: 0)
                }
                .padding(.top, 58)
                .task {
                    if let scenario = Args.live { live = await LiveStarter.start(scenario, f) }
                }
            } else {
                Text(verbatim: "NO FIXTURE \(Args.lang)\n" + Fixtures.errors.joined(separator: "\n"))
                    .foregroundColor(.red)
                    .padding()
            }
        }
        .environment(\.colorScheme, dark ? .dark : .light)
        // لغة الجهاز كلغة التطبيق: يختار النظام بها خط اللغة (النستعليق للأردية) وارتفاع سطره
        .environment(\.locale, Locale(identifier: Args.lang))
        .ignoresSafeArea()
    }

    @ViewBuilder
    private func page(_ f: LangFixture) -> some View {
        switch Args.page {
        case "tasbih": TasbihPage(f: f)
        case "khatma": KhatmaPage(f: f)
        case "prayer": PrayerPage(f: f)
        case "lock1": LockPage1(f: f)
        case "lock2": LockPage2(f: f)
        case "lock3": LockPage3(f: f)
        case "live":
            Text(verbatim: live.isEmpty ? "LIVE STARTING" : live)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(live.hasPrefix("LIVE STARTED") ? .green : .orange)
                .accessibilityIdentifier("live-status")
        default: Text(verbatim: "unknown page \(Args.page)").foregroundColor(.red)
        }
    }
}

/// خلفية تشبه خلفية الشاشة الرئيسية
struct Wallpaper: View {
    let dark: Bool

    var body: some View {
        LinearGradient(
            colors: dark ? [Color(hex: "#1C1F3A"), Color(hex: "#05060D")] : [Color(hex: "#7FA8E0"), Color(hex: "#E7B98E")],
            startPoint: .top, endPoint: .bottom
        )
    }
}

/// فكّ حالات Kotlin بكل اللغات بأنواع Swift الحقيقية
struct CheckPage: View {
    var body: some View {
        let errors = Fixtures.errors
        VStack(alignment: .leading, spacing: 8) {
            if errors.isEmpty && Fixtures.all.count >= 15 {
                Text(verbatim: "CHECK OK").font(.title.bold()).foregroundColor(.green)
                Text(verbatim: Fixtures.all.keys.sorted().joined(separator: " ")).foregroundColor(.white)
            } else {
                Text(verbatim: "CHECK FAILED").font(.title.bold()).foregroundColor(.red)
                Text(verbatim: "decoded: \(Fixtures.all.keys.sorted().joined(separator: " "))").foregroundColor(.white)
                ForEach(errors, id: \.self) { e in
                    Text(verbatim: e).font(.system(size: 12, design: .monospaced)).foregroundColor(.white)
                }
            }
        }
        .padding(24)
        .padding(.top, 50)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

// ---------- إطار الأداة ----------

/// الأداة بمقاسها وخلفيتها وهوامشها كما يرسمها WidgetKit (containerBackground لا يُرسم خارج WidgetKit)
struct WidgetBox<Content: View>: View {
    let family: WidgetFamily
    let snap: Snapshot?
    var bare: Bool = false
    @ViewBuilder let content: () -> Content
    @Environment(\.colorScheme) private var scheme

    private var size: CGSize {
        switch family {
        case .systemSmall: return CGSize(width: 170, height: 170)
        case .systemMedium: return CGSize(width: 364, height: 170)
        case .systemLarge: return CGSize(width: 364, height: 382)
        case .accessoryCircular: return CGSize(width: 76, height: 76)
        case .accessoryRectangular: return CGSize(width: 172, height: 76)
        case .accessoryInline: return CGSize(width: 257, height: 26)
        default: return CGSize(width: 170, height: 170)
        }
    }

    var body: some View {
        let palette = snap?.colors ?? Mizan.fallback
        let dark = snap?.isDark(scheme) ?? (scheme == .dark)
        let accessory = family.isAccessory
        content()
            .environment(\.galleryFamily, family)
            .environment(\.galleryBare, bare)
            // شاشة القفل: أبيض فوق الخلفية كما يلوّنه النظام
            .environment(\.colorScheme, accessory ? .dark : scheme)
            .padding(accessory || bare ? 0 : 16)
            .frame(width: size.width, height: size.height)
            .background {
                if accessory {
                    RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(Color.white.opacity(0.25), style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                } else if bare {
                    Color.black
                } else {
                    MizanBackground(palette: palette, dark: dark)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: accessory ? 10 : 22, style: .continuous))
    }
}

/// النشاط الحي على شاشة القفل: عرض البطاقة وخلفيتها (activityBackgroundTint)، وارتفاعه (الحد 160 نقطة)
struct LockBox<Content: View>: View {
    let tint: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        content()
            .frame(width: Args.lockWidth)
            .background(Color(hex: tint))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay {
                GeometryReader { g in
                    Text(verbatim: "\(Int(g.size.height.rounded()))pt")
                        .font(.system(size: 9, weight: .semibold, design: .monospaced))
                        .foregroundColor(g.size.height > 160 ? .red : .white.opacity(0.35))
                        .padding(6)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                        // يقرؤه اختبار الواجهة فيفشل إن تجاوز النشاط 160 نقطة
                        .accessibilityIdentifier("lock-height")
                }
            }
    }
}

// ---------- الصفحات ----------

struct TasbihPage: View {
    let f: LangFixture

    var body: some View {
        let snap = f.snapshot
        VStack(spacing: 14) {
            HStack(spacing: 22) {
                WidgetBox(family: .systemSmall, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbih, snap: snap)) }
                WidgetBox(family: .systemSmall, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbihFree, snap: snap)) }
            }
            WidgetBox(family: .systemMedium, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbihSalah, snap: snap)) }
            WidgetBox(family: .systemMedium, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbih, snap: snap)) }
            HStack(spacing: 16) {
                WidgetBox(family: .accessoryCircular, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbih, snap: snap)) }
                WidgetBox(family: .accessoryRectangular, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbihSalah, snap: snap)) }
                WidgetBox(family: .accessoryCircular, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbihFree, snap: snap)) }
            }
            WidgetBox(family: .accessoryInline, snap: snap) { TasbihWidgetView(entry: TasbihEntry(date: Date(), state: f.tasbih, snap: snap)) }
        }
    }
}

struct KhatmaPage: View {
    let f: LangFixture

    var body: some View {
        var done = f.snapshot
        done.khatma = f.khatmaDone
        let snap = f.snapshot
        return VStack(spacing: 14) {
            HStack(spacing: 22) {
                WidgetBox(family: .systemSmall, snap: snap) { KhatmaView(entry: MizanEntry(date: Date(), snap: snap)) }
                WidgetBox(family: .systemSmall, snap: done) { KhatmaView(entry: MizanEntry(date: Date(), snap: done)) }
            }
            WidgetBox(family: .systemMedium, snap: snap) { KhatmaView(entry: MizanEntry(date: Date(), snap: snap)) }
            WidgetBox(family: .systemMedium, snap: done) { KhatmaView(entry: MizanEntry(date: Date(), snap: done)) }
            HStack(spacing: 16) {
                WidgetBox(family: .accessoryCircular, snap: snap) { KhatmaView(entry: MizanEntry(date: Date(), snap: snap)) }
                WidgetBox(family: .accessoryRectangular, snap: snap) { KhatmaView(entry: MizanEntry(date: Date(), snap: snap)) }
                WidgetBox(family: .accessoryCircular, snap: done) { KhatmaView(entry: MizanEntry(date: Date(), snap: done)) }
            }
        }
    }
}

/// الصلاة القادمة على الشاشة الرئيسية وفي StandBy (بلا خلفية، بخط كبير)
struct PrayerPage: View {
    let f: LangFixture

    var body: some View {
        let snap = f.snapshot.movedToNow()
        let now = Date()
        VStack(spacing: 14) {
            HStack(spacing: 22) {
                WidgetBox(family: .systemSmall, snap: snap) { NextPrayerView(entry: MizanEntry(date: now, snap: snap)) }
                WidgetBox(family: .systemSmall, snap: snap, bare: true) { NextPrayerView(entry: MizanEntry(date: now, snap: snap)) }
            }
            WidgetBox(family: .systemMedium, snap: snap) { NextPrayerView(entry: MizanEntry(date: now, snap: snap)) }
            // StandBy بالعرض: الأداة الصغيرة مكبّرة
            WidgetBox(family: .systemSmall, snap: snap, bare: true) { NextPrayerView(entry: MizanEntry(date: now, snap: snap)) }
                .scaleEffect(1.25)
                .frame(width: 212, height: 212)
        }
    }
}

/// المسبحة والأذكار على شاشة القفل
struct LockPage1: View {
    let f: LangFixture

    var body: some View {
        VStack(spacing: 14) {
            LockBox(tint: f.tasbih.hero2) { TasbihLockView(state: f.tasbih) }
            LockBox(tint: f.tasbihSalah.hero2) { TasbihLockView(state: f.tasbihSalah) }
            LockBox(tint: f.adhkar.hero2) { AdhkarLockView(state: f.adhkar) }
        }
    }
}

/// الأذكار بعد إتمامها، والصلاة قبل وقتها وبعد أذان المغرب في رمضان (الإفطار والإقامة)
struct LockPage2: View {
    let f: LangFixture

    var body: some View {
        let before = f.prayer.moved(to: Date().addingTimeInterval(25 * 60))
        let iftar = f.prayerIftar.moved(to: Date().addingTimeInterval(-90))
        // أعرض عدّ: أكثر من عشر ساعات (من العشاء إلى الفجر في الشتاء)
        let far = f.prayer.moved(to: Date().addingTimeInterval(11 * 3600 + 24 * 60))
        VStack(spacing: 14) {
            LockBox(tint: f.adhkarDone.hero2) { AdhkarLockView(state: f.adhkarDone) }
            LockBox(tint: before.hero2) { PrayerLockView(state: before, arrived: false) }
            LockBox(tint: far.hero2) { PrayerLockView(state: far, arrived: false) }
            LockBox(tint: iftar.hero2) { PrayerLockView(state: iftar, arrived: true) }
        }
    }
}

/// أطول النصوص: تمام المائة بعد الصلاة، و«لا حول ولا قوة إلا بالله»، وأطول أذكار الصباح بمعناه
struct LockPage3: View {
    let f: LangFixture

    var body: some View {
        VStack(spacing: 14) {
            LockBox(tint: f.tasbihTamam.hero2) { TasbihLockView(state: f.tasbihTamam) }
            LockBox(tint: f.tasbihLong.hero2) { TasbihLockView(state: f.tasbihLong) }
            LockBox(tint: f.adhkarLong.hero2) { AdhkarLockView(state: f.adhkarLong) }
        }
    }
}
