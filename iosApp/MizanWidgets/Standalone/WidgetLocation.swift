import CoreLocation
import Foundation

// موقع الأداة نفسها (NSWidgetWantsLocation في Info.plist الأداة): إذن «أثناء استخدام التطبيق» الذي يطلبه التطبيق
// يظهر في الإعدادات «أثناء استخدام التطبيق أو الأدوات»، فتحصل الأدوات على الموقع دون أي بيانات من التطبيق.
// الأداة لا تستطيع أن تطلب الإذن بنفسها؛ إن لم يُمنح تعرض رسالة تدل على مكانه في الإعدادات.

/// آخر موقع حصلت عليه الأداة (في UserDefaults الخاص بها)، وحال الإذن لسطر التشخيص
enum LocationCache {
    private static var d: UserDefaults { .standard }

    static var last: (lat: Double, lng: Double, at: Date)? {
        guard d.object(forKey: "loc.lat") != nil, d.object(forKey: "loc.lng") != nil else { return nil }
        return (d.double(forKey: "loc.lat"), d.double(forKey: "loc.lng"), Date(timeIntervalSince1970: d.double(forKey: "loc.at")))
    }

    static func save(_ l: CLLocation) {
        d.set(l.coordinate.latitude, forKey: "loc.lat")
        d.set(l.coordinate.longitude, forKey: "loc.lng")
        d.set(l.timestamp.timeIntervalSince1970, forKey: "loc.at")
    }

    /// L·ok | L·denied | L·ask (لم يُطلب الإذن في التطبيق بعد) | L·widgets (التطبيق مأذون والأدوات لا) | L·restricted | L·errN | L·timeout
    static var status: String {
        get { d.string(forKey: "loc.status") ?? "L·-" }
        set { d.set(newValue, forKey: "loc.status") }
    }
}

/// طلب موقع واحد بمهلة: موقع النظام الحديث إن وُجد، وإلا requestLocation
final class WidgetLocator: NSObject, CLLocationManagerDelegate {
    private var manager: CLLocationManager?
    private var done: ((CLLocation?) -> Void)?
    /// تبقى الطلبات الجارية حية حتى تنتهي (عدة أدوات تبني خطوطها معاً)
    private static var active: [WidgetLocator] = []

    /// completion بالإحداثيات (على الخيط الرئيسي)، أو nil إن لم يُمنح الإذن أو لم يصل موقع في المهلة
    static func fetch(timeout: TimeInterval = 10, completion: @escaping ((lat: Double, lng: Double)?) -> Void) {
        DispatchQueue.main.async {
            let l = WidgetLocator()
            active.append(l)
            l.start(timeout: timeout) { loc in
                active.removeAll { $0 === l }
                completion(loc.map { (lat: $0.coordinate.latitude, lng: $0.coordinate.longitude) })
            }
        }
    }

    private func start(timeout: TimeInterval, completion: @escaping (CLLocation?) -> Void) {
        done = completion
        let m = CLLocationManager()
        manager = m
        m.delegate = self
        m.desiredAccuracy = kCLLocationAccuracyKilometer
        switch m.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            if !m.isAuthorizedForWidgetUpdates {
                LocationCache.status = "L·widgets"
                finish(nil)
                return
            }
        case .denied:
            LocationCache.status = "L·denied"
            finish(nil)
            return
        case .restricted:
            LocationCache.status = "L·restricted"
            finish(nil)
            return
        default:
            LocationCache.status = "L·ask"
            finish(nil)
            return
        }
        // موقع النظام الأخير إن كان حديثاً يكفي (لا يتغير وقت الصلاة في بضعة كيلومترات)
        if let loc = m.location, -loc.timestamp.timeIntervalSinceNow < 15 * 60 {
            LocationCache.status = "L·ok"
            finish(loc)
            return
        }
        m.requestLocation()
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { [weak self] in
            guard let self = self, self.done != nil else { return }
            LocationCache.status = "L·timeout"
            self.finish(self.manager?.location)
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        LocationCache.status = "L·ok"
        finish(locations.last)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        LocationCache.status = "L·err\((error as NSError).code)"
        finish(manager.location)
    }

    private func finish(_ loc: CLLocation?) {
        guard let done = done else { return }
        self.done = nil
        if let loc = loc { LocationCache.save(loc) }
        manager?.delegate = nil
        manager = nil
        done(loc)
    }
}
