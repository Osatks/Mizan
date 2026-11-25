// دوال الأعداد كما تعمل في Kotlin/JVM (minOf وcoerceAtLeast وcoerceIn وtoInt)، لتطابق النتائج حتى في الصفر السالب
// (min وmax في Swift تختلف عنها عند تساوي 0.0 و‎-0.0).

/// minOf(a, b) للأعداد العشرية كما في Math.min
@inline(__always) func kmin(_ a: Double, _ b: Double) -> Double {
    if a.isNaN { return a }
    if a == 0, b == 0, b.sign == .minus { return b }
    return a <= b ? a : b
}

/// coerceAtLeast
@inline(__always) func katLeast(_ x: Double, _ m: Double) -> Double { x < m ? m : x }

/// coerceIn
@inline(__always) func kcoerce(_ x: Double, _ lo: Double, _ hi: Double) -> Double { x < lo ? lo : (x > hi ? hi : x) }

/// Double.toInt() في Kotlin: يُبتر الكسر، وNaN صفر، ويُشبع عند حدود Int (32 بت)
public func kotlinToInt(_ x: Double) -> Int {
    if x.isNaN { return 0 }
    if x >= 2147483647.0 { return 2147483647 }
    if x <= -2147483648.0 { return -2147483648 }
    return Int(x.rounded(.towardZero))
}
