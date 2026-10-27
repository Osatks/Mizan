import Foundation
import Security

/// بديل App Group لتثبيتات لا تمنحها (Sideloadly بحساب Apple مجاني): التطبيق والأداة من الفريق نفسه،
/// وملف التوقيع المجاني يسمح عادة بمجموعات سلسلة المفاتيح «TEAMID.*»، فيكتب التطبيق اللقطة في عنصر
/// مشترك ويقرؤها الأداة منه.
enum SharedKeychain {
    private static let service = "com.mizan.widget"
    private static let account = "snapshot"

    /// بادئة الفريق (Team ID) من مجموعة عنصر تجريبي بلا مجموعة محددة: يأخذ النظام المجموعة الافتراضية للتطبيق
    static func teamPrefix() -> String? {
        let probe: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.mizan.probe",
            kSecAttrAccount as String: "probe",
            kSecReturnAttributes as String: true,
        ]
        var r: CFTypeRef?
        var st = SecItemCopyMatching(probe as CFDictionary, &r)
        if st == errSecItemNotFound {
            var add = probe
            add[kSecValueData as String] = Data()
            add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
            st = SecItemAdd(add as CFDictionary, &r)
        }
        guard st == errSecSuccess, let a = r as? [String: Any], let g = a[kSecAttrAccessGroup as String] as? String,
              let prefix = g.split(separator: ".").first else { return nil }
        return String(prefix)
    }

    static var group: String? { teamPrefix().map { "\($0).com.mizan.almuslim.shared" } }

    private static func query(_ group: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecAttrAccessGroup as String: group,
        ]
    }

    /// يكتب اللقطة؛ يعيد حالة Security (0 نجاح، -34018 لا صلاحية للمجموعة)
    @discardableResult
    static func write(_ data: Data) -> OSStatus {
        guard let g = group else { return errSecNoAccessForItem }
        let q = query(g)
        let update: [String: Any] = [kSecValueData as String: data]
        var st = SecItemUpdate(q as CFDictionary, update as CFDictionary)
        if st == errSecItemNotFound {
            var add = q
            add[kSecValueData as String] = data
            add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
            st = SecItemAdd(add as CFDictionary, nil)
        }
        return st
    }

    static func read() -> (Data?, OSStatus) {
        guard let g = group else { return (nil, errSecNoAccessForItem) }
        var q = query(g)
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var r: CFTypeRef?
        let st = SecItemCopyMatching(q as CFDictionary, &r)
        return (r as? Data, st)
    }
}
