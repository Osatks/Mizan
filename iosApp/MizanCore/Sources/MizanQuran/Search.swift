import Foundation

/// تطبيع نصوص البحث ليجد المستخدم ما يريد مهما كتب: يحذف التشكيل والعلامات، ويوحّد صور الألف والياء
/// والتاء المربوطة والحروف الفارسية والأردية مع مقابلها العربي، ويحوّل الأرقام الشرقية إلى لاتينية (Search.kt).
public enum Search {
    public static func normalize(_ text: String) -> String {
        let decomposed = javaLowercase(text).decomposedStringWithCanonicalMapping
        var out = String.UnicodeScalarView()
        // مسافة معلّقة: سلسلة من الفواصل والعلامات والرموز تصير مسافة واحدة
        var pendingSpace = false
        func emit(_ s: Unicode.Scalar) {
            if isSeparator(s) { pendingSpace = true; return }
            if pendingSpace { out.append(" "); pendingSpace = false }
            out.append(s)
        }
        for ch in decomposed.unicodeScalars {
            if ch.properties.generalCategory == .nonspacingMark { continue }
            switch ch.value {
            case 0x0623, 0x0625, 0x0622, 0x0671: emit("\u{0627}")            // أ إ آ ٱ ← ا
            case 0x0629: emit("\u{0647}")                                   // ة ← ه
            case 0x0649, 0x06CC, 0x06D2, 0x0626: emit("\u{064A}")            // ى ی ے ئ ← ي
            case 0x06A9: emit("\u{0643}")                                   // ک ← ك
            case 0x06C1, 0x06BE, 0x06C0: emit("\u{0647}")                    // ہ ھ ۀ ← ه
            case 0x0624: emit("\u{0648}")                                   // ؤ ← و
            case 0x0131: emit("i")                                          // ı ← i
            // التطويل والوصلات، والفواصل العليا في الأسماء المنقولة (En'âm، Al-A'raf) تُحذف ولا تفصل الكلمة
            case 0x0640, 0x200C, 0x200D, 0x27, 0x2019, 0x2018, 0x02BC, 0x02BB, 0x02BE, 0x02BF, 0x60: continue
            case 0x0660...0x0669: emit(Unicode.Scalar(0x30 + ch.value - 0x0660)!)
            case 0x06F0...0x06F9: emit(Unicode.Scalar(0x30 + ch.value - 0x06F0)!)
            case 0x09E6...0x09EF: emit(Unicode.Scalar(0x30 + ch.value - 0x09E6)!)
            default: emit(ch)
            }
        }
        if pendingSpace { out.append(" ") }
        return ktTrim(String(out))
    }

    /// درجة التطابق: 0 = لا تطابق. كل كلمات البحث يجب أن توجد؛ وتعلو الدرجة إذا بدأ الاسم بها أو بدأت بها كلمة فيه.
    public static func score(_ query: [String], _ name: String, _ extra: String = "") -> Int {
        if query.isEmpty { return 0 }
        let n = Array(normalize(name).utf8)
        let all = n + [0x20] + Array(normalize(extra).utf8)
        var total = 0
        for qs in query {
            let q = Array(qs.utf8)
            let sq = [0x20] + q
            if bytesHasPrefix(n, q) { total += 30 }
            else if bytesContains(n, sq) { total += 20 }
            else if bytesContains(n, q) { total += 12 }
            else if bytesContains(all, sq) || bytesHasPrefix(all, q) { total += 8 }
            else if bytesContains(all, q) { total += 4 }
            else { return 0 }
        }
        return total
    }

    public static func terms(_ q: String) -> [String] {
        normalize(q).utf8.split(separator: 0x20).map { String(decoding: $0, as: UTF8.self) }
    }

    // ---------- مساعدات بمعنى Java/Kotlin نفسه ----------

    /// \s (ASCII في Java) و \p{P} و \p{S}
    static func isSeparator(_ s: Unicode.Scalar) -> Bool {
        switch s.value {
        case 0x20, 0x09, 0x0A, 0x0B, 0x0C, 0x0D: return true
        default: break
        }
        switch s.properties.generalCategory {
        case .connectorPunctuation, .dashPunctuation, .openPunctuation, .closePunctuation,
             .initialPunctuation, .finalPunctuation, .otherPunctuation,
             .mathSymbol, .currencySymbol, .modifierSymbol, .otherSymbol:
            return true
        default:
            return false
        }
    }

    /// String.lowercase() في Kotlin (toLowerCase(Locale.ROOT)): تحويل كل محرف وحده، إلا سيغما آخر الكلمة (ς)
    static func javaLowercase(_ text: String) -> String {
        if !text.unicodeScalars.contains(where: { $0.properties.changesWhenLowercased }) { return text }
        let a = Array(text.unicodeScalars)
        var out = String.UnicodeScalarView()
        for (i, s) in a.enumerated() {
            if s.value == 0x03A3 { out.append(isFinalSigma(a, i) ? "\u{03C2}" : "\u{03C3}") }
            else if s.properties.changesWhenLowercased { out.append(contentsOf: s.properties.lowercaseMapping.unicodeScalars) }
            else { out.append(s) }
        }
        return String(out)
    }

    private static func isFinalSigma(_ a: [Unicode.Scalar], _ i: Int) -> Bool {
        var j = i - 1
        while j >= 0 && a[j].properties.isCaseIgnorable { j -= 1 }
        guard j >= 0, a[j].properties.isCased else { return false }
        var k = i + 1
        while k < a.count && a[k].properties.isCaseIgnorable { k += 1 }
        return !(k < a.count && a[k].properties.isCased)
    }
}

/// Char.isWhitespace في Kotlin: فواصل Unicode (Zs و Zl و Zp) ومحارف التحكم 9–13 و 28–31
func ktIsWhitespace(_ s: Unicode.Scalar) -> Bool {
    switch s.value {
    case 0x09...0x0D, 0x1C...0x1F: return true
    default: break
    }
    switch s.properties.generalCategory {
    case .spaceSeparator, .lineSeparator, .paragraphSeparator: return true
    default: return false
    }
}

/// String.trim() في Kotlin
func ktTrim(_ s: String) -> String {
    let u = s.unicodeScalars
    guard let first = u.firstIndex(where: { !ktIsWhitespace($0) }) else { return "" }
    let last = u.lastIndex(where: { !ktIsWhitespace($0) })!
    if first == u.startIndex && last == u.index(before: u.endIndex) { return s }
    return String(u[first...last])
}

/// toIntOrNull في Kotlin (Int بـ 32 بت)
func ktInt<S: StringProtocol>(_ s: S) -> Int? { Int32(s).map { Int($0) } }

func bytesHasPrefix(_ a: [UInt8], _ p: [UInt8]) -> Bool {
    if p.count > a.count { return false }
    var i = 0
    while i < p.count { if a[i] != p[i] { return false }; i += 1 }
    return true
}

/// بحث عن تتابع بايتات UTF-8 (يطابق contains في Kotlin على UTF-16)
func bytesContains(_ a: [UInt8], _ p: [UInt8]) -> Bool {
    let n = a.count, m = p.count
    if m == 0 { return true }
    if m > n { return false }
    return a.withUnsafeBufferPointer { ab in
        p.withUnsafeBufferPointer { pb in
            let first = pb[0]
            var i = 0
            let end = n - m
            while i <= end {
                if ab[i] == first {
                    var j = 1
                    while j < m && ab[i + j] == pb[j] { j += 1 }
                    if j == m { return true }
                }
                i += 1
            }
            return false
        }
    }
}
