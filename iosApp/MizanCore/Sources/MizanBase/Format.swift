import Foundation

/// فاصلا الكسور والآلاف للغة (الأرقام لاتينية دائماً)، كما في i18n/Lang.kt
public struct NumberLang: Hashable, Sendable {
    public let code: String
    public let decimal: Character
    public let group: Character
    public init(code: String, decimal: Character, group: Character) { self.code = code; self.decimal = decimal; self.group = group }

    public static let ar = NumberLang(code: "ar", decimal: ".", group: ",")
    public static let en = NumberLang(code: "en", decimal: ".", group: ",")
    public static let fr = NumberLang(code: "fr", decimal: ",", group: " ")
    public static let tr = NumberLang(code: "tr", decimal: ",", group: ".")
    public static let ur = NumberLang(code: "ur", decimal: ".", group: ",")
    public static let id = NumberLang(code: "id", decimal: ",", group: ".")
    public static let ms = NumberLang(code: "ms", decimal: ".", group: ",")
    public static let fa = NumberLang(code: "fa", decimal: ".", group: ",")
    public static let bn = NumberLang(code: "bn", decimal: ".", group: ",")
    public static let ru = NumberLang(code: "ru", decimal: ",", group: " ")
    public static let de = NumberLang(code: "de", decimal: ",", group: ".")
    public static let es = NumberLang(code: "es", decimal: ",", group: ".")
    public static let uk = NumberLang(code: "uk", decimal: ",", group: " ")

    /// اللغات المكتوبة في التطبيق، بترتيب Lang.BUILT_IN
    public static let builtIn: [NumberLang] = [ar, en, fr, tr, ur, id, ms, fa, bn, ru, de, es, uk]

    /// لغات الحزم المثبتة (تضاف بفاصليها من الحزمة)
    nonisolated(unsafe) public static var packs: [NumberLang] = []

    public static func of(_ code: String) -> NumberLang? {
        builtIn.first { $0.code == code } ?? packs.first { $0.code == code }
    }

    /// لغة الجهاز إن كانت متاحة، وإلا الإنجليزية ("in" للإندونيسية)
    public static func fromTag(_ tag: String) -> NumberLang {
        let c = String(tag.split(separator: "-", maxSplits: 1, omittingEmptySubsequences: false).first ?? "")
            .split(separator: "_", maxSplits: 1, omittingEmptySubsequences: false).first.map(String.init)?.lowercased() ?? ""
        if c == "in" { return .id }
        return of(c) ?? .en
    }

    /// لغة الإعداد إن كانت متاحة، وإلا لغة الجهاز (Lang.resolve)
    public static func resolve(_ pref: String, systemTag: String) -> NumberLang { of(pref) ?? fromTag(systemTag) }

    /// اللغة الحالية للتنسيق (Lang.current)؛ يضبطها التطبيق من إعداد اللغة: NumberLang.resolve(AppSettings.lang, systemTag:)
    nonisolated(unsafe) public static var current: NumberLang = fromTag(Locale.preferredLanguages.first ?? "en")
}

/// تنسيق الأرقام: أرقام لاتينية دائماً، مع فاصلي الكسور والآلاف المعتادين في لغة الواجهة
public enum Fmt {
    public static func num(_ v: Double, digits: Int = 2, lang: NumberLang = NumberLang.current) -> String {
        formatNumber(v, digits: min(max(digits, 0), 2), decimal: lang.decimal, group: lang.group)
    }

    public static func int(_ v: Int64, lang: NumberLang = NumberLang.current) -> String {
        formatNumber(Double(v), digits: 0, decimal: lang.decimal, group: lang.group)
    }

    public static func int(_ v: Int, lang: NumberLang = NumberLang.current) -> String { int(Int64(v), lang: lang) }

    /// رقمان دائماً (للساعات والدقائق) دون الاعتماد على لغة الجهاز
    public static func two(_ v: Int64) -> String { v < 10 ? "0\(v)" : "\(v)" }
    public static func two(_ v: Int) -> String { two(Int64(v)) }

    private static let arabic = Array("٠١٢٣٤٥٦٧٨٩")
    private static let persian = Array("۰۱۲۳۴۵۶۷۸۹")
    private static let bengali = Array("০১২৩৪৫৬৭৮৯")

    /// يحوّل نص المستخدم إلى رقم موجب. يقبل الأرقام العربية والفارسية والبنغالية، ويميّز فاصل الكسور:
    /// إذا ظهر الفاصلان فالأخير للكسور، وإذا ظهر واحد مرة واحدة فهو للكسور ما لم يكن فاصل آلاف في
    /// لغة الواجهة وبعده ثلاثة أرقام بالضبط (مثل 1.500 بالألمانية).
    public static func parse(_ text: String, lang: NumberLang = NumberLang.current) -> Double {
        // نعمل على وحدات UTF-16 كما تفعل Kotlin (Char)
        var raw: [UInt16] = []
        for ch in text.utf16 {
            let d = max(index(arabic, ch), index(persian, ch), index(bengali, ch))
            if d >= 0 { raw.append(48 + UInt16(d)); continue }
            switch ch {
            case 0x066B: raw.append(46) // ٫
            case 0x066C, 0x060C, 0x27, 0xA0, 0x202F: break // ٬ ، ' ومسافات لا تنكسر
            default:
                if isWhitespaceKotlin(ch) { break }
                raw.append(ch)
            }
        }
        let dotU: UInt16 = 46, commaU: UInt16 = 44
        let dot = raw.lastIndex(of: dotU) ?? -1, comma = raw.lastIndex(of: commaU) ?? -1
        var cleaned: [UInt16]
        if dot >= 0 && comma >= 0 {
            if dot > comma { cleaned = raw.filter { $0 != commaU } }
            else { cleaned = raw.filter { $0 != dotU }.map { $0 == commaU ? dotU : $0 } }
        } else if dot >= 0 || comma >= 0 {
            let sep = dot >= 0 ? dotU : commaU
            let count = raw.filter { $0 == sep }.count
            let after = raw.count - (raw.lastIndex(of: sep) ?? 0) - 1
            let groupU = lang.group.utf16.count == 1 ? lang.group.utf16.first! : 0
            let isGroup = count > 1 || (sep == groupU && after == 3)
            cleaned = isGroup ? raw.filter { $0 != sep } : raw.map { $0 == sep ? dotU : $0 }
        } else {
            cleaned = raw
        }
        guard let v = kotlinToDouble(String(decoding: cleaned, as: UTF16.self)) else { return 0 }
        return v.isFinite && v > 0 ? v : 0
    }

    private static func index(_ digits: [Character], _ u: UInt16) -> Int {
        for (i, c) in digits.enumerated() where c.utf16.first == u { return i }
        return -1
    }
}

/// رقم بأرقام لاتينية، حتى digits منازل عشرية (تقريب إلى الزوجي على القيمة الثنائية الدقيقة كما في
/// java.text.DecimalFormat)، بفاصلي الكسور والآلاف المعطيين
public func formatNumber(_ v: Double, digits: Int, decimal: Character, group: Character) -> String {
    if v.isNaN { return "NaN" }
    if v.isInfinite { return v < 0 ? "-∞" : "∞" }
    let digits = max(0, digits)
    var intPart: String
    var frac: String
    var negative = v.sign == .minus
    let a = abs(v)
    if a >= 9007199254740992.0 {
        // عدد صحيح كبير: أرقام أقصر تمثيل ثم أصفار (كما يفعل DecimalFormat)
        intPart = expandShortest(a)
        frac = ""
    } else {
        let s = String(format: "%.\(digits)f", a)
        if let dot = s.firstIndex(of: ".") {
            intPart = String(s[s.startIndex..<dot])
            frac = String(s[s.index(after: dot)...])
        } else {
            intPart = s
            frac = ""
        }
        while frac.hasSuffix("0") { frac.removeLast() }
    }
    // DecimalFormat يُظهر -0 لقيمة سالبة تُقرَّب إلى الصفر
    if v == 0 && v.sign == .minus { negative = true }
    var grouped = ""
    let chars = Array(intPart)
    for (i, c) in chars.enumerated() {
        if i > 0 && (chars.count - i) % 3 == 0 { grouped.append(group) }
        grouped.append(c)
    }
    var out = negative ? "-" : ""
    out += grouped
    if !frac.isEmpty { out.append(decimal); out += frac }
    return out
}

/// أرقام أقصر تمثيل لعدد صحيح كبير ممتدة بالأصفار (1.5E20 → 150000000000000000000)
private func expandShortest(_ a: Double) -> String {
    let d = "\(a)" // مثل 1.5e+20 أو 9007199254740992.0
    let lower = d.lowercased()
    var mantissa = lower, exp = 0
    if let e = lower.firstIndex(of: "e") {
        mantissa = String(lower[lower.startIndex..<e])
        exp = Int(lower[lower.index(after: e)...].replacingOccurrences(of: "+", with: "")) ?? 0
    }
    var intDigits = mantissa, fracDigits = ""
    if let dot = mantissa.firstIndex(of: ".") {
        intDigits = String(mantissa[mantissa.startIndex..<dot])
        fracDigits = String(mantissa[mantissa.index(after: dot)...])
    }
    var all = intDigits + fracDigits
    let pointPos = intDigits.count + exp
    if all.count < pointPos { all += String(repeating: "0", count: pointPos - all.count) }
    let r = String(all.prefix(pointPos))
    let trimmed = r.drop { $0 == "0" }
    return trimmed.isEmpty ? "0" : String(trimmed)
}

/// Char.isWhitespace في Kotlin/JVM (Character.isWhitespace أو isSpaceChar)
private func isWhitespaceKotlin(_ u: UInt16) -> Bool {
    switch u {
    case 0x09...0x0D, 0x1C...0x20, 0xA0, 0x1680, 0x2000...0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000: return true
    default: return false
    }
}

/// String.toDoubleOrNull في Kotlin/JVM (صيغة Double.valueOf: مسافات الأطراف، والإشارة، و NaN و Infinity، واللاحقة d/f)
func kotlinToDouble(_ s: String) -> Double? {
    // Kotlin تتحقق بتعبير منتظم قبل Double.parseDouble؛ نقبل الصيغ العشرية الشائعة
    var t = Substring(s)
    while let f = t.first, f.unicodeScalars.allSatisfy({ $0.value <= 0x20 }) { t.removeFirst() }
    while let l = t.last, l.unicodeScalars.allSatisfy({ $0.value <= 0x20 }) { t.removeLast() }
    if t.isEmpty { return nil }
    var body = t
    var sign = ""
    if body.first == "+" || body.first == "-" { sign = String(body.first!); body = body.dropFirst() }
    if body == "NaN" { return Double.nan }
    if body == "Infinity" { return sign == "-" ? -Double.infinity : Double.infinity }
    if let l = body.last, "dDfF".contains(l) { body = body.dropLast() }
    // أرقام [. أرقام] [e[±]أرقام] مع رقم واحد على الأقل في الجزء الأساسي
    let chars = Array(body)
    var i = 0, mant = 0
    while i < chars.count, chars[i].isASCII, chars[i].isNumber { i += 1; mant += 1 }
    if i < chars.count, chars[i] == "." {
        i += 1
        while i < chars.count, chars[i].isASCII, chars[i].isNumber { i += 1; mant += 1 }
    }
    if mant == 0 { return nil }
    if i < chars.count, chars[i] == "e" || chars[i] == "E" {
        i += 1
        if i < chars.count, chars[i] == "+" || chars[i] == "-" { i += 1 }
        var ed = 0
        while i < chars.count, chars[i].isASCII, chars[i].isNumber { i += 1; ed += 1 }
        if ed == 0 { return nil }
    }
    if i != chars.count { return nil }
    return Double(sign + String(body))
}
