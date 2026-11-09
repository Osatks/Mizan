// swift-tools-version:5.9
// منطق ميزان بلغة Swift لتطبيق iPhone (مواقيت الصلاة، التاريخ الهجري، الحاسبات، القرآن، الأذكار)، نقلاً عن Kotlin المشتركة.
// يُبنى على iOS 15 وعلى Linux (Foundation وحدها)، وتُقارَن نتائجه بنتائج Kotlin في Fixtures/ ببرامج Check*.
import PackageDescription

let package = Package(
    name: "MizanCore",
    platforms: [.iOS(.v15), .macOS(.v12)],
    products: [
        .library(name: "MizanCore", targets: ["MizanCore"]),
    ],
    targets: [
        .target(name: "MizanBase"),
        .target(name: "MizanPrayer", dependencies: ["MizanBase"]),
        .target(name: "MizanCalc", dependencies: ["MizanBase"]),
        .target(name: "MizanQuran", dependencies: ["MizanBase"]),
        .target(name: "MizanContent", dependencies: ["MizanBase"]),
        .target(name: "MizanCore", dependencies: ["MizanBase", "MizanPrayer", "MizanCalc", "MizanQuran", "MizanContent"]),
        .executableTarget(name: "CheckPrayer", dependencies: ["MizanPrayer"]),
        .executableTarget(name: "CheckCalc", dependencies: ["MizanCalc", "MizanBase"]),
        .executableTarget(name: "CheckQuran", dependencies: ["MizanQuran"]),
        .executableTarget(name: "CheckContent", dependencies: ["MizanContent"]),
    ]
)
