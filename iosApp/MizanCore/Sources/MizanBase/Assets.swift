import Foundation

/// ملفات assets المضمّنة في التطبيق (Bundle.main.resourcePath/assets كما في Platform.ios.kt)؛
/// [root] قابل للتغيير لتشير برامج Check إلى مجلد المستودع، و[extraRoots] مجلدات يُبحث فيها بعده
/// (برامج Check: src/content في المستودع، المحتوى الذي ينزّله التطبيق من الموقع ولم يعد مضمّناً — ContentStore.kt)
public enum Assets {
    nonisolated(unsafe) public static var root: String = Bundle.main.resourcePath.map { $0 + "/assets" } ?? ""
    nonisolated(unsafe) public static var extraRoots: [String] = []

    /// المسار الكامل لملف داخل assets (أو في أول مجلد إضافي يوجد فيه)
    public static func path(_ path: String) -> String {
        let main = root + "/" + path
        if extraRoots.isEmpty || FileManager.default.fileExists(atPath: main) { return main }
        for r in extraRoots where FileManager.default.fileExists(atPath: r + "/" + path) { return r + "/" + path }
        return main
    }

    /// محتوى الملف، أو nil إن لم يوجد
    public static func data(_ path: String) -> Data? {
        FileManager.default.contents(atPath: self.path(path))
    }

    /// نص الملف (UTF-8)، أو nil إن لم يوجد
    public static func text(_ path: String) -> String? {
        data(path).flatMap { String(data: $0, encoding: .utf8) }
    }

    /// أسماء الملفات في مجلد داخل assets والمجلدات الإضافية (فارغة إن لم يوجد)
    public static func list(_ dir: String) -> [String] {
        var names = (try? FileManager.default.contentsOfDirectory(atPath: root + "/" + dir)) ?? []
        for r in extraRoots {
            for n in (try? FileManager.default.contentsOfDirectory(atPath: r + "/" + dir)) ?? [] where !names.contains(n) { names.append(n) }
        }
        return names
    }

    public static func exists(_ path: String) -> Bool {
        FileManager.default.fileExists(atPath: self.path(path))
    }
}
