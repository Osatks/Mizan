// كسر نسبي دقيق (نقلاً عن core/Inheritance.kt: Fr)
// العمليات على Int64 بالالتفاف عند الفيضان كما في Long في Kotlin، فلا يتوقف التطبيق ولا تختلف النتيجة.

/// كسر نسبي دقيق مختزل، والمقام موجب دائماً
public struct Fr: Comparable, Hashable, CustomStringConvertible, Sendable {
    public let n: Int64
    public let d: Int64

    private init(raw n: Int64, _ d: Int64) {
        self.n = n
        self.d = d
    }

    public static func gcd(_ a: Int64, _ b: Int64) -> Int64 {
        var a = a, b = b
        while b != 0 {
            let r = a.remainderReportingOverflow(dividingBy: b).partialValue
            a = b
            b = r
        }
        return a == 0 ? 1 : kabs(a)
    }

    public static func lcm(_ a: Int64, _ b: Int64) -> Int64 { kdiv(a, gcd(a, b)) &* b }

    public static func of(_ n: Int64, _ d: Int64 = 1) -> Fr {
        var nn = n, dd = d
        if dd < 0 { nn = 0 &- nn; dd = 0 &- dd }
        let g = gcd(kabs(nn), dd)
        return Fr(raw: kdiv(nn, g), kdiv(dd, g))
    }

    public static let zero = Fr.of(0)
    public static let one = Fr.of(1)

    public static func + (a: Fr, o: Fr) -> Fr { of(a.n &* o.d &+ o.n &* a.d, a.d &* o.d) }
    public static func - (a: Fr, o: Fr) -> Fr { of(a.n &* o.d &- o.n &* a.d, a.d &* o.d) }
    public static func * (a: Fr, o: Fr) -> Fr { of(a.n &* o.n, a.d &* o.d) }
    public static func * (a: Fr, k: Int) -> Fr { of(a.n &* Int64(k), a.d) }
    public static func / (a: Fr, o: Fr) -> Fr { of(a.n &* o.d, a.d &* o.n) }
    public static func / (a: Fr, k: Int) -> Fr { of(a.n, a.d &* Int64(k)) }
    public static func < (a: Fr, b: Fr) -> Bool { (a.n &* b.d) < (b.n &* a.d) }

    public var value: Double { Double(n) / Double(d) }
    public var isZero: Bool { n == 0 }
    public var description: String { d == 1 ? "\(n)" : "\(n)/\(d)" }
}

// القيمة المطلقة والقسمة كما في Long (بلا توقف عند الفيضان)
@inline(__always) func kabs(_ x: Int64) -> Int64 { x < 0 ? 0 &- x : x }
@inline(__always) func kdiv(_ a: Int64, _ b: Int64) -> Int64 { a.dividedReportingOverflow(by: b).partialValue }
