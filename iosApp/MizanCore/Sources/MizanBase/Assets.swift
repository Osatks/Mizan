import Foundation

/// ملفات assets المضمّنة في التطبيق (Bundle.main.resourcePath/assets كما في Platform.ios.kt)؛
/// [root] قابل للتغيير لتشير برامج Check إلى مجلد المستودع
public enum Assets {
    nonisolated(unsafe) public static var root: String = Bundle.main.resourcePath.map { $0 + "/assets" } ?? ""

    /// المسار الكامل لملف داخل assets
    public static func path(_ path: String) -> String { root + "/" + path }

    /// محتوى الملف، أو nil إن لم يوجد
    public static func data(_ path: String) -> Data? {
        FileManager.default.contents(atPath: self.path(path))
    }

    /// نص الملف (UTF-8)، أو nil إن لم يوجد
    public static func text(_ path: String) -> String? {
        data(path).flatMap { String(data: $0, encoding: .utf8) }
    }

    /// أسماء الملفات في مجلد داخل assets (فارغة إن لم يوجد)
    public static func list(_ dir: String) -> [String] {
        (try? FileManager.default.contentsOfDirectory(atPath: path(dir))) ?? []
    }

    public static func exists(_ path: String) -> Bool {
        FileManager.default.fileExists(atPath: self.path(path))
    }
}
