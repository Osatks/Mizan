import Foundation
import MizanBase

// مساعدات قراءة JSON كما تقرؤها i18n/pack/Json.kt (كائن مرتب، ومصفوفة، وأرقام، ونص، و null)

/// ملف من assets: يُقرأ عبر Assets في MizanBase ما لم يُستبدل (للاختبارات)
func contentAsset(_ path: String) -> Data? { Assets.data(path) }

/// كائن JSON من بيانات، أو nil إن لم تكن كائناً
func jsonObject(_ data: Data?) -> [String: Any]? {
    guard let data = data, let o = try? JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed]) else { return nil }
    return o as? [String: Any]
}

/// isBlank في Kotlin: فارغ أو مسافات فقط
func isBlank(_ s: String) -> Bool {
    s.unicodeScalars.allSatisfy { $0.properties.isWhitespace || $0 == "\u{1C}" || $0 == "\u{1D}" || $0 == "\u{1E}" || $0 == "\u{1F}" }
}

/// نص غير فارغ من قيمة JSON (takeIf { it.isNotBlank() })
func nonBlank(_ v: Any?) -> String? {
    guard let s = v as? String, !isBlank(s) else { return nil }
    return s
}

/// رقم JSON صحيحاً (Number.toInt في Kotlin: الكسور تُقطع)؛ nil للنص والمنطقي
func jsonInt(_ v: Any?) -> Int? {
    guard let v = v, !(v is String), !isJSONBool(v) else { return nil }
    let d: Double
    if let i = v as? Int { return i }
    if let n = v as? NSNumber { d = n.doubleValue } else if let x = v as? Double { d = x } else { return nil }
    if d.isNaN { return 0 }
    if d >= 2147483647 { return Int(Int32.max) }
    if d <= -2147483648 { return Int(Int32.min) }
    return Int(d.rounded(.towardZero))
}

/// منطقي JSON (true/false)، لا الرقمان 0 و 1
func isJSONBool(_ v: Any) -> Bool {
    if type(of: v) == Bool.self { return true }
    #if canImport(Darwin)
    if let n = v as? NSNumber { return CFGetTypeID(n) == CFBooleanGetTypeID() }
    return false
    #else
    if type(of: v) == Int.self || type(of: v) == Double.self { return false }
    if let n = v as? NSNumber { return String(cString: n.objCType) == "c" }
    return false
    #endif
}

/// toString لقيمة JSON كما في Kotlin (الأعداد الصحيحة بلا كسر)
func jsonString(_ v: Any?) -> String? {
    if v == nil || v is NSNull { return nil }
    if let s = v as? String { return s }
    if let n = v as? NSNumber {
        let d = n.doubleValue
        if d == d.rounded(), abs(d) < 9.2e18 { return String(Int64(d)) }
        return String(d)
    }
    return "\(v!)"
}
