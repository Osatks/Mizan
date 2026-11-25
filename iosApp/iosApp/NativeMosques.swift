import SwiftUI
import UIKit
import MapKit
import CoreLocation
import Contacts
import Shared

/// المساجد القريبة بواجهة النظام (SwiftUI و MapKit) بطراز صفحة القبلة (NativeQibla.swift): ترويستها وشريطها العلوي وبطاقاتها،
/// وخريطة بدبابيس المساجد، وقائمة مرتبة بالمسافة (الاسم والمسافة والعنوان إن وُجد). لمس المسجد يفتح الاتجاهات إليه في
/// خرائط Apple، وزر جانبي يعرض خرائط Google أيضاً إن كانت مثبتة (comgooglemaps في LSApplicationQueriesSchemes).
/// البحث من خرائط Apple وحدها (MKLocalSearch بنص طبيعي: «مسجد» بلغة التطبيق و mosque و masjid)؛ لا فئة للمساجد ولا
/// لأماكن العبادة في MKPointOfInterestCategory، فتُستبعد بعد البحث الفئات التجارية الصريحة (مطعم «المسجد» مثلاً).
/// الموقع الحالي إن كان مأذوناً (بلا طلب إذن عند الفتح)، وإلا موقع التطبيق المحفوظ مع زر لاستعمال الموقع الحالي.
/// Kotlin (NativeMosques.ios.kt) يرسل النصوص والألوان والهوامش والموقع المحفوظ فقط، مع الإشعار MizanMosquesState.
enum NativeMosquesScreen {
    static func install() {
        NativeMosques.shared.attach { session in MosquesHostController(session: session) }
    }
}

final class MosquesHostController: UIHostingController<MosquesRoot> {
    init(session: NativeMosquesSession) {
        let model = MosquesModel(session: session)
        super.init(rootView: MosquesRoot(model: model))
        view.backgroundColor = .clear
        model.onStyle = { [weak self] dark, rtl in self?.applyAppStyle(dark: dark, rtl: rtl) }
        if let s = model.state { applyAppStyle(dark: s.dark, rtl: s.rtl) }
        if #available(iOS 16.4, *) { safeAreaRegions = [] }
    }

    @MainActor required dynamic init?(coder aDecoder: NSCoder) { fatalError() }
}

// MARK: - الحالة

/// مفاتيحها كما في NativeMosques.ios.kt حرفاً بحرف
struct MosquesState: Decodable {
    struct Extra: Decodable { let warnBg, warnFg, warn: String }

    let rtl, dark, canBack, placeSet: Bool
    let top, bottom, start, end, lead, width: Double
    let lat, lng: Double
    let back, title, subtitle, decimal, nearSaved: String
    let words: [String]
    let mosque, unitKm, unitM, locating, loading, empty, emptyHint, error, errorHint: String
    let locationFailed, needLocation, locationOff, retry, openInMaps, useMyLocation, openSettings: String
    let directions, appleMaps, googleMaps, privacy, map: String
    let c: HomeState.Colors
    let x: Extra

    static func decode(_ json: String) -> MosquesState? {
        guard !json.isEmpty, let data = json.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(MosquesState.self, from: data)
    }
}

/// مسجد من خرائط Apple بالنسبة إلى موضع البحث
struct MosqueItem: Identifiable {
    let id: String
    let name: String
    let address: String?
    let coordinate: CLLocationCoordinate2D
    let meters: CLLocationDistance
    let mapItem: MKMapItem
}

/// ما تجمعه عمليات البحث المتوازية (كلها تنتهي على الخيط الرئيسي)
private final class MosqueSearchBag {
    var items: [MKMapItem] = []
    var errors = 0
}

final class MosquesModel: NSObject, ObservableObject, CLLocationManagerDelegate {
    enum Phase: Equatable { case locating, searching, results, empty, failed, noLocation, needLocation(denied: Bool) }
    /// لماذا يُبحث حول موقع التطبيق المحفوظ لا الموقع الحالي
    enum Saved: Equatable { case notAsked, denied, failed }

    let session: NativeMosquesSession
    @Published private(set) var state: MosquesState? = nil { didSet { if let s = state { onStyle?(s.dark, s.rtl) } } }
    @Published private(set) var phase: Phase = .locating
    @Published private(set) var items: [MosqueItem] = []
    @Published private(set) var center: CLLocationCoordinate2D? = nil
    /// nil: حول الموقع الحالي
    @Published private(set) var saved: Saved? = nil
    var onStyle: ((Bool, Bool) -> Void)?
    /// خرائط Google مثبتة (يحتاج comgooglemaps في LSApplicationQueriesSchemes)
    let hasGoogleMaps: Bool

    private var token: NSObjectProtocol?
    private let manager = CLLocationManager()
    private var started = false
    private var asking = false
    private var locating = false
    private var timeout: DispatchWorkItem?
    private var generation = 0
    private var searches: [MKLocalSearch] = []

    /// آخر نتيجة في الذاكرة طوال الجلسة: فتح الصفحة مرة أخرى من المكان نفسه لا يعيد البحث
    private static var cache: (center: CLLocationCoordinate2D, items: [MosqueItem])?

    /// فئات تجارية صريحة تُستبعد من نتائج «مسجد» (مطعم أو فندق يحمل الكلمة في اسمه)
    private static let excluded: Set<MKPointOfInterestCategory> = [
        .restaurant, .cafe, .bakery, .brewery, .winery, .nightlife, .foodMarket, .store, .hotel, .gasStation, .evCharger,
        .parking, .carRental, .bank, .atm, .pharmacy, .laundry, .fitnessCenter, .movieTheater,
    ]

    init(session: NativeMosquesSession) {
        self.session = session
        hasGoogleMaps = URL(string: "comgooglemaps://").map { UIApplication.shared.canOpenURL($0) } ?? false
        super.init()
        state = MosquesState.decode(session.state)
        token = NotificationCenter.default.addObserver(forName: Notification.Name("MizanMosquesState"), object: session, queue: .main) { [weak self] _ in
            guard let self = self, let s = MosquesState.decode(self.session.state) else { return }
            self.state = s
        }
    }

    deinit {
        if let t = token { NotificationCenter.default.removeObserver(t) }
        timeout?.cancel()
        searches.forEach { $0.cancel() }
        manager.delegate = nil
    }

    func back() { session.back() }

    /// مرة عند ظهور الصفحة: الموقع الحالي إن كان مأذوناً، وإلا الموقع المحفوظ (لا يُسأل الإذن هنا)
    func start() {
        guard !started else { return }
        started = true
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        resolve()
    }

    private func resolve() {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways: locate()
        case .notDetermined: useSaved(.notAsked)
        default: useSaved(.denied)
        }
    }

    /// «استخدم موقعي الحالي»: يسأل الإذن إن لم يُسأل بعد، أو يفتح الإعدادات إن رُفض
    func useCurrentLocation() {
        switch manager.authorizationStatus {
        case .notDetermined:
            asking = true
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            locate()
        default:
            openSettings()
        }
    }

    func openSettings() {
        if let u = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(u) }
    }

    func retry() {
        if phase == .noLocation { locate(); return }
        if let c = center { search(c, force: true) } else { resolve() }
    }

    private func locate() {
        phase = .locating
        locating = true
        timeout?.cancel()
        let t = DispatchWorkItem { [weak self] in self?.located(nil) }
        timeout = t
        DispatchQueue.main.asyncAfter(deadline: .now() + 15, execute: t)
        manager.requestLocation()
    }

    private func located(_ location: CLLocation?) {
        guard locating else { return }
        locating = false
        timeout?.cancel()
        if let l = location {
            saved = nil
            search(l.coordinate)
        } else {
            useSaved(.failed)
        }
    }

    private func useSaved(_ why: Saved) {
        guard let s = state, s.placeSet else {
            saved = nil
            switch why {
            case .failed: phase = .noLocation
            case .denied: phase = .needLocation(denied: true)
            case .notAsked: phase = .needLocation(denied: false)
            }
            return
        }
        saved = why
        search(CLLocationCoordinate2D(latitude: s.lat, longitude: s.lng))
    }

    // MARK: CLLocationManagerDelegate

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        let allowed = status == .authorizedWhenInUse || status == .authorizedAlways
        if asking {
            // جواب سؤال الإذن الذي طلبه المستخدم
            guard status != .notDetermined else { return }
            asking = false
            if allowed { locate() } else { useSaved(.denied) }
        } else if started, allowed, !locating, saved == .denied || phase == .needLocation(denied: true) {
            // سمح به من الإعدادات ثم عاد إلى الصفحة
            locate()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) { located(locations.last) }
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) { located(nil) }

    // MARK: البحث

    /// في 5 كم، ثم في 15 كم إن قلّت النتائج عن 5 (كما في أندرويد)
    private func search(_ c: CLLocationCoordinate2D, force: Bool = false) {
        center = c
        if !force, let cached = Self.cache, Self.distance(cached.center, c) < 250 {
            items = Self.resorted(cached.items, from: c)
            phase = items.isEmpty ? .empty : .results
            return
        }
        guard let s = state else { return }
        phase = .searching
        generation += 1
        let gen = generation
        query(s.words, around: c, radius: 5_000) { [weak self] near, failed in
            guard let self = self, gen == self.generation else { return }
            if near.count >= 5 || failed {
                self.finish(near, failed: failed, center: c)
                return
            }
            self.query(s.words, around: c, radius: 15_000) { [weak self] wide, wideFailed in
                guard let self = self, gen == self.generation else { return }
                // الأوسع إن نجح، وإلا ما وُجد قريباً
                let list = wide.count >= near.count ? wide : near
                self.finish(list, failed: wideFailed && near.isEmpty, center: c)
            }
        }
    }

    private func finish(_ list: [MosqueItem], failed: Bool, center c: CLLocationCoordinate2D) {
        if list.isEmpty && failed {
            phase = .failed
            return
        }
        items = list
        Self.cache = (c, list)
        phase = list.isEmpty ? .empty : .results
    }

    /// كل كلمة بحث على حدة في المنطقة؛ failed: فشلت كلها لسبب غير «لا نتائج» (الشبكة غالباً)
    private func query(_ words: [String], around c: CLLocationCoordinate2D, radius: CLLocationDistance,
                       done: @escaping ([MosqueItem], Bool) -> Void) {
        let region = MKCoordinateRegion(center: c, latitudinalMeters: radius * 2, longitudinalMeters: radius * 2)
        let group = DispatchGroup()
        let bag = MosqueSearchBag()
        for w in words {
            let request = MKLocalSearch.Request()
            request.naturalLanguageQuery = w
            request.region = region
            request.resultTypes = .pointOfInterest
            let search = MKLocalSearch(request: request)
            searches.append(search)
            group.enter()
            search.start { response, error in
                if let r = response {
                    bag.items.append(contentsOf: r.mapItems)
                } else if let e = error, (e as? MKError)?.code != .placemarkNotFound {
                    bag.errors += 1
                }
                group.leave()
            }
        }
        group.notify(queue: .main) { [weak self] in
            guard let self = self else { return }
            self.searches.removeAll { !$0.isSearching }
            let list = self.mosques(from: bag.items, around: c, radius: radius)
            done(list, bag.errors == words.count && bag.items.isEmpty)
        }
    }

    /// النتائج مرتبة بالمسافة، داخل نصف القطر، بلا الفئات التجارية ولا التكرار (المسجد نفسه من كلمتين)
    private func mosques(from found: [MKMapItem], around c: CLLocationCoordinate2D, radius: CLLocationDistance) -> [MosqueItem] {
        guard let s = state else { return [] }
        let here = CLLocation(latitude: c.latitude, longitude: c.longitude)
        let sorted = found
            .map { item -> (MKMapItem, CLLocationDistance) in
                let p = item.placemark.coordinate
                return (item, here.distance(from: CLLocation(latitude: p.latitude, longitude: p.longitude)))
            }
            .sorted { $0.1 < $1.1 }
        var out: [MosqueItem] = []
        for (item, meters) in sorted {
            if meters > radius * 1.2 { continue }
            if let cat = item.pointOfInterestCategory, Self.excluded.contains(cat) { continue }
            let coord = item.placemark.coordinate
            let trimmed = item.name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            let name = trimmed.isEmpty ? s.mosque : trimmed
            let dup = out.contains { o in
                Self.distance(o.coordinate, coord) < 60 && o.name.caseInsensitiveCompare(name) == .orderedSame
            }
            if dup { continue }
            out.append(MosqueItem(id: String(format: "%.6f,%.6f,", coord.latitude, coord.longitude) + name, name: name,
                                  address: Self.address(item.placemark, rtl: s.rtl), coordinate: coord, meters: meters, mapItem: item))
            if out.count >= 60 { break }
        }
        return out
    }

    /// الشارع والمدينة بصيغة البلد (CNPostalAddressFormatter)، في سطر واحد
    private static func address(_ p: MKPlacemark, rtl: Bool) -> String? {
        guard let pa = p.postalAddress else { return nil }
        let short = CNMutablePostalAddress()
        short.street = pa.street
        short.city = pa.city
        let text = CNPostalAddressFormatter.string(from: short, style: .mailingAddress)
            .split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .joined(separator: rtl ? "، " : ", ")
        return text.isEmpty ? nil : text
    }

    private static func distance(_ a: CLLocationCoordinate2D, _ b: CLLocationCoordinate2D) -> CLLocationDistance {
        CLLocation(latitude: a.latitude, longitude: a.longitude).distance(from: CLLocation(latitude: b.latitude, longitude: b.longitude))
    }

    private static func resorted(_ list: [MosqueItem], from c: CLLocationCoordinate2D) -> [MosqueItem] {
        list.map { m in
            MosqueItem(id: m.id, name: m.name, address: m.address, coordinate: m.coordinate,
                       meters: distance(c, m.coordinate), mapItem: m.mapItem)
        }
        .sorted { $0.meters < $1.meters }
    }

    // MARK: الخرائط

    /// الاتجاهات إلى المسجد في خرائط Apple
    func openApple(_ m: MosqueItem) {
        _ = m.mapItem.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeDefault])
    }

    /// الاتجاهات في خرائط Google (إن كانت مثبتة)
    func openGoogle(_ m: MosqueItem) {
        let c = m.coordinate
        guard let u = URL(string: String(format: "comgooglemaps://?daddr=%.6f,%.6f", c.latitude, c.longitude)) else { return }
        UIApplication.shared.open(u)
    }

    /// البحث عن «مسجد» في خرائط Apple حول الموقع (حين لا نتائج هنا)
    func searchInMaps() {
        guard let s = state, var comps = URLComponents(string: "https://maps.apple.com/") else { return }
        var q = [URLQueryItem(name: "q", value: s.mosque)]
        if let c = center { q.append(URLQueryItem(name: "sll", value: String(format: "%.6f,%.6f", c.latitude, c.longitude))) }
        comps.queryItems = q
        if let u = comps.url { UIApplication.shared.open(u) }
    }

    /// المسافة بأرقام لاتينية وفاصل الكسور في لغة التطبيق (Mosques.distance في Kotlin)
    func distanceText(_ meters: Double) -> String {
        guard let s = state else { return "" }
        if meters < 950 {
            let m = max(10, Int((meters / 10).rounded()) * 10)
            return "\(m) \(s.unitM)"
        }
        var t = meters < 9950 ? String(format: "%.1f", meters / 1000) : String(format: "%.0f", meters / 1000)
        if t.hasSuffix(".0") { t.removeLast(2) }
        return t.replacingOccurrences(of: ".", with: s.decimal) + " " + s.unitKm
    }
}

// MARK: - الشاشة

struct MosquesRoot: View {
    @ObservedObject var model: MosquesModel
    var body: some View {
        Group {
            if let s = model.state { MosquesView(s: s, p: HomePalette(s.c), model: model) } else { Color.clear }
        }
        .ignoresSafeArea()
    }
}

private struct MosquesHeaderBottomKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct MosquesView: View {
    let s: MosquesState
    let p: HomePalette
    @ObservedObject var model: MosquesModel
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
                    QiblaTasbihHeader(title: s.title, subtitle: s.subtitle, symbol: "mappin.and.ellipse",
                                      top: CGFloat(s.top), start: CGFloat(s.start), end: CGFloat(s.end), rtl: s.rtl, p: p)
                        .background(GeometryReader { g in
                            Color.clear.preference(key: MosquesHeaderBottomKey.self, value: g.frame(in: .named("mosques")).maxY)
                        })
                    VStack(spacing: 14) {
                        if let why = model.saved, model.phase != .locating {
                            savedNote(why)
                        }
                        content
                        Text(s.privacy).font(sys(12)).foregroundColor(p.onSurfaceVariant)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 4)
                    }
                    .padding(.leading, CGFloat(s.start))
                    .padding(.trailing, CGFloat(s.end))
                }
                .padding(.bottom, CGFloat(s.bottom))
            }
            .coordinateSpace(name: "mosques")
            .onPreferenceChange(MosquesHeaderBottomKey.self) { track($0) }
            QiblaTasbihTopBar(title: s.title, back: s.back, canBack: s.canBack, top: CGFloat(s.top), lead: CGFloat(s.lead),
                              p: p, amount: glass) { model.back() }
        }
        .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
        .environment(\.colorScheme, s.dark ? .dark : .light)
        .onAppear { model.start() }
    }

    @ViewBuilder private var content: some View {
        switch model.phase {
        case .locating:
            status(s.locating)
        case .searching:
            status(s.loading)
        case .results:
            mapCard
            ForEach(model.items) { m in
                MosqueRowView(m: m, s: s, p: p, distance: model.distanceText(m.meters), google: model.hasGoogleMaps,
                              onApple: { model.openApple(m) }, onGoogle: { model.openGoogle(m) })
            }
        case .empty:
            message(symbol: "magnifyingglass", title: s.empty, detail: s.emptyHint) {
                primaryButton(s.openInMaps, "map.fill") { model.searchInMaps() }
            }
        case .failed:
            message(symbol: "wifi.exclamationmark", title: s.error, detail: s.errorHint) {
                primaryButton(s.retry, "arrow.clockwise") { model.retry() }
                secondaryButton(s.openInMaps, "map") { model.searchInMaps() }
            }
        case .noLocation:
            message(symbol: "location.slash.fill", title: s.locationFailed, detail: nil) {
                primaryButton(s.retry, "arrow.clockwise") { model.retry() }
                secondaryButton(s.openInMaps, "map") { model.searchInMaps() }
            }
        case .needLocation(let denied):
            message(symbol: "location.slash.fill", title: denied ? s.locationOff : s.title, detail: s.needLocation) {
                if denied {
                    primaryButton(s.openSettings, "gearshape.fill") { model.openSettings() }
                } else {
                    primaryButton(s.useMyLocation, "location.fill") { model.useCurrentLocation() }
                }
            }
        }
    }

    /// الخريطة بدبابيس المساجد؛ موقع المستخدم عليها حين يُبحث حوله
    private var mapCard: some View {
        MosquesMapView(items: model.items, center: model.center ?? CLLocationCoordinate2D(latitude: s.lat, longitude: s.lng),
                       showsUser: model.saved == nil, tint: UIColor(p.primary), dark: s.dark, directions: s.directions,
                       subtitle: { model.distanceText($0.meters) }, onOpen: { model.openApple($0) })
            .frame(height: min(300, max(200, CGFloat(s.width) * 0.62)))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
            .accessibilityElement(children: .contain)
            .accessibilityLabel(s.map)
    }

    /// تحديد الموقع أو البحث جارٍ؛ يُعلن لقارئ الشاشة
    private func status(_ text: String) -> some View {
        QiblaTasbihCard(p: p) {
            HStack(spacing: 14) {
                ProgressView().tint(p.primary)
                Text(text).font(sys(15)).foregroundColor(p.onSurface)
                Spacer(minLength: 0)
            }
            .accessibilityElement(children: .combine)
        }
        .onAppear { UIAccessibility.post(notification: .announcement, argument: text) }
    }

    /// يُبحث حول موقع التطبيق المحفوظ: لماذا، وزر للموقع الحالي أو للإعدادات
    private func savedNote(_ why: MosquesModel.Saved) -> some View {
        let warn = why != .notAsked
        return VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
                    .font(.system(size: 17)).foregroundColor(warn ? HomePalette.color(s.x.warn) : p.primary)
                    .accessibilityHidden(true)
                Text(s.nearSaved).font(sys(14))
                    .foregroundColor(warn ? HomePalette.color(s.x.warnFg) : p.onPrimaryContainer)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(14)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(warn ? HomePalette.color(s.x.warnBg) : p.primaryContainer))
            if why == .denied {
                secondaryButton(s.openSettings, "gearshape") { model.openSettings() }
            } else {
                secondaryButton(s.useMyLocation, "location") { model.useCurrentLocation() }
            }
        }
    }

    /// رسالة في وسط بطاقة: أيقونة في دائرة، وعنوان وشرح، وأزرار
    private func message<A: View>(symbol: String, title: String, detail: String?, @ViewBuilder actions: () -> A) -> some View {
        // تُبنى الأزرار هنا: محتوى البطاقة يُحفظ ليُرسم لاحقاً
        let buttons = actions()
        return QiblaTasbihCard(p: p) {
            VStack(spacing: 10) {
                Image(systemName: symbol).font(.system(size: 24, weight: .semibold)).foregroundColor(p.primary)
                    .frame(width: 56, height: 56).background(Circle().fill(p.primaryContainer))
                    .accessibilityHidden(true)
                Text(title).font(sys(17, .bold)).foregroundColor(p.onSurface).multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)
                if let d = detail {
                    Text(d).font(sys(14)).foregroundColor(p.onSurfaceVariant).multilineTextAlignment(.center)
                }
                VStack(spacing: 8) { buttons }.padding(.top, 4)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        }
    }

    private func primaryButton(_ title: String, _ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: symbol).font(sys(15, .semibold)).foregroundColor(.white)
                .padding(.horizontal, 22).padding(.vertical, 13)
                .frame(minHeight: 48)
                .background(Capsule().fill(LinearGradient(colors: [p.hero1, p.hero2], startPoint: .leading, endPoint: .trailing)))
        }
        .buttonStyle(PressScale())
    }

    private func secondaryButton(_ title: String, _ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: symbol).font(sys(15, .semibold)).foregroundColor(p.primary)
                .padding(.horizontal, 22).padding(.vertical, 13)
                .frame(minHeight: 48)
                .background(Capsule().fill(p.surface))
                .overlay(Capsule().strokeBorder(p.outlineVariant, lineWidth: 1))
        }
        .buttonStyle(PressScale())
    }
}

/// مسجد في القائمة: لمسه يفتح الاتجاهات في خرائط Apple؛ والزر الجانبي يعرض خرائط Apple و Google إن كانت مثبتة
private struct MosqueRowView: View {
    let m: MosqueItem
    let s: MosquesState
    let p: HomePalette
    let distance: String
    let google: Bool
    let onApple: () -> Void
    let onGoogle: () -> Void

    var body: some View {
        HStack(spacing: 6) {
            Button(action: onApple) {
                HStack(spacing: 12) {
                    Image(systemName: "moon.stars.fill").font(.system(size: 18, weight: .semibold)).foregroundColor(p.primary)
                        .frame(width: 42, height: 42)
                        .background(RoundedRectangle(cornerRadius: 13, style: .continuous).fill(p.gold.opacity(0.16)))
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(m.name).font(sys(16, .bold)).foregroundColor(p.onSurface).lineLimit(2)
                            .multilineTextAlignment(.leading)
                        if let a = m.address {
                            Text(a).font(sys(12)).foregroundColor(p.onSurfaceVariant).lineLimit(2)
                                .multilineTextAlignment(.leading)
                        }
                        Text(distance).font(sys(13, .semibold)).foregroundColor(p.primary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Image(systemName: "arrow.triangle.turn.up.right.diamond.fill")
                        .font(.system(size: 22)).foregroundColor(p.primary)
                        .accessibilityHidden(true)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PressScale(scale: 0.98))
            .accessibilityHint(s.directions)
            if google {
                Menu {
                    Button(action: onApple) { Label(s.appleMaps, systemImage: "map") }
                    Button(action: onGoogle) { Label(s.googleMaps, systemImage: "map.fill") }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 20, weight: .semibold)).foregroundColor(p.onSurfaceVariant)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .accessibilityLabel(s.directions)
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 22, style: .continuous).fill(p.surface))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).strokeBorder(p.outlineVariant.opacity(0.6), lineWidth: 1))
    }
}

// MARK: - الخريطة

/// دبوس مسجد: اسمه، والمسافة تحته
final class MosqueAnnotation: NSObject, MKAnnotation {
    let item: MosqueItem
    let coordinate: CLLocationCoordinate2D
    let title: String?
    let subtitle: String?

    init(item: MosqueItem, subtitle: String) {
        self.item = item
        coordinate = item.coordinate
        title = item.name
        self.subtitle = subtitle
    }
}

/// MKMapView (يعمل من iOS 15): دبابيس المساجد، والموقع الحالي، وإطار يضم الموقع وأقرب المساجد.
/// لمس الدبوس يعرض اسمه، وزر الاتجاهات في فقاعته يفتح خرائط Apple
struct MosquesMapView: UIViewRepresentable {
    let items: [MosqueItem]
    let center: CLLocationCoordinate2D
    let showsUser: Bool
    let tint: UIColor
    let dark: Bool
    let directions: String
    let subtitle: (MosqueItem) -> String
    let onOpen: (MosqueItem) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        // بلا أماكن الخريطة الأخرى، فتظهر المساجد وحدها
        map.pointOfInterestFilter = .excludingAll
        map.showsCompass = true
        map.register(MKMarkerAnnotationView.self, forAnnotationViewWithReuseIdentifier: Coordinator.reuse)
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        context.coordinator.parent = self
        map.overrideUserInterfaceStyle = dark ? .dark : .light
        if map.showsUserLocation != showsUser { map.showsUserLocation = showsUser }
        let ids = items.map(\.id)
        guard ids != context.coordinator.shown else { return }
        context.coordinator.shown = ids
        map.removeAnnotations(map.annotations.filter { $0 is MosqueAnnotation })
        let pins = items.map { MosqueAnnotation(item: $0, subtitle: subtitle($0)) }
        map.addAnnotations(pins)
        // بعد أن يأخذ العرض مقاسه
        let c = center
        DispatchQueue.main.async {
            var rect = MKMapRect(origin: MKMapPoint(c), size: MKMapSize(width: 0, height: 0))
            for pin in pins.prefix(8) {
                rect = rect.union(MKMapRect(origin: MKMapPoint(pin.coordinate), size: MKMapSize(width: 0, height: 0)))
            }
            if pins.isEmpty || rect.size.width < 1 && rect.size.height < 1 {
                map.setRegion(MKCoordinateRegion(center: c, latitudinalMeters: 2_000, longitudinalMeters: 2_000), animated: false)
            } else {
                map.setVisibleMapRect(rect, edgePadding: UIEdgeInsets(top: 44, left: 32, bottom: 32, right: 32), animated: false)
            }
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        static let reuse = "mosque"
        var parent: MosquesMapView
        var shown: [String] = []

        init(_ parent: MosquesMapView) { self.parent = parent }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            // موقع المستخدم وتجمّع الدبابيس بشكلهما في النظام
            guard let pin = annotation as? MosqueAnnotation else { return nil }
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: Self.reuse, for: pin)
            if let marker = view as? MKMarkerAnnotationView {
                marker.markerTintColor = parent.tint
                marker.glyphImage = UIImage(systemName: "moon.stars.fill")
                marker.clusteringIdentifier = "mosque"
                marker.canShowCallout = true
                let go = UIButton(type: .system)
                go.setImage(UIImage(systemName: "arrow.triangle.turn.up.right.diamond.fill"), for: .normal)
                go.tintColor = parent.tint
                go.frame = CGRect(x: 0, y: 0, width: 44, height: 44)
                go.accessibilityLabel = parent.directions
                marker.rightCalloutAccessoryView = go
            }
            return view
        }

        func mapView(_ mapView: MKMapView, annotationView view: MKAnnotationView, calloutAccessoryControlTapped control: UIControl) {
            if let pin = view.annotation as? MosqueAnnotation { parent.onOpen(pin.item) }
        }
    }
}
