import XCTest

// يلتقط صفحات المعرض، والأنشطة الحية الحقيقية في الجزيرة الديناميكية (المصغّرة والموسّعة) وعلى شاشة القفل.
// الصور في مجلد GALLERY_OUT (TEST_RUNNER_GALLERY_OUT في xcodebuild)، ومرفقة أيضاً بنتيجة الاختبار (xcresult).
// الترتيب بأسماء الاختبارات: التحقق من الحالات، ثم الأدوات، ثم الجزيرة، ثم شاشة القفل (آخراً لأنها تقفل الجهاز).

final class GalleryUITests: XCTestCase {
    static let out: URL = {
        let path = ProcessInfo.processInfo.environment["GALLERY_OUT"] ?? (NSTemporaryDirectory() + "gallery")
        let url = URL(fileURLWithPath: path, isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }()

    let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    override func setUp() {
        continueAfterFailure = true
    }

    private func save(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        try? shot.pngRepresentation.write(to: Self.out.appendingPathComponent(name + ".png"))
        let a = XCTAttachment(screenshot: shot)
        a.name = name
        a.lifetime = .keepAlways
        add(a)
    }

    @discardableResult
    private func launch(_ args: [String]) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = args
        app.launch()
        return app
    }

    private func texts(_ app: XCUIApplication) -> String {
        app.staticTexts.allElementsBoundByIndex.prefix(40).map(\.label).joined(separator: " | ")
    }

    // ---------- 1: حالات Kotlin تُفكّ في Swift ----------

    func test1Fixtures() {
        let app = launch(["-gallery-page", "check"])
        if !app.staticTexts["CHECK OK"].waitForExistence(timeout: 15) {
            save("00-check-failed")
            XCTFail("Kotlin states do not decode in Swift: " + texts(app))
        }
    }

    // ---------- 2: الأدوات وشاشة القفل مرسومة في المعرض ----------

    func test2Widgets() {
        for lang in ["ar", "en", "ru"] {
            for dark in [false, true] {
                for page in ["tasbih", "khatma", "prayer", "lock1", "lock2"] {
                    if dark && page.hasPrefix("lock") { continue }
                    let app = launch(["-gallery-page", page, "-gallery-lang", lang, "-gallery-dark", dark ? "1" : "0"])
                    _ = app.staticTexts["page-ready"].waitForExistence(timeout: 8)
                    sleep(1)
                    save("widget-\(page)-\(lang)-\(dark ? "dark" : "light")")
                }
            }
        }
    }

    // ---------- 3: الجزيرة الديناميكية ----------

    func test3Island() {
        // أول نشاط في المحاكي يتأخر ظهوره: نشاط تمهيدي لا يُلتقط
        if start("tasbih", "en") {
            XCUIDevice.shared.press(.home)
            sleep(5)
        }
        for lang in ["ar", "en"] {
            for scenario in ["tasbih", "tasbihSalah", "adhkar", "adhkarDone", "prayer", "iftar", "tasbih+prayer"] {
                guard start(scenario, lang) else { continue }
                let name = "island-\(scenario.replacingOccurrences(of: "+", with: "-"))-\(lang)"
                XCUIDevice.shared.press(.home)
                sleep(3)
                save(name + "-compact")
                // ضغطة مطوّلة على الجزيرة: الموسّعة
                let size = springboard.frame.size
                springboard.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: size.width / 2, dy: 31)).press(forDuration: 1.2)
                sleep(2)
                save(name + "-expanded")
                springboard.coordinate(withNormalizedOffset: .zero).withOffset(CGVector(dx: size.width / 2, dy: size.height * 0.8)).tap()
                sleep(1)
            }
        }
    }

    // ---------- 4: شاشة القفل (الأنشطة كلها معاً) ----------

    func test4LockScreen() {
        for lang in ["ar", "en"] {
            for (i, scenario) in ["tasbih+adhkar+prayer", "tasbihSalah+adhkarDone+iftar"].enumerated() {
                guard start(scenario, lang) else { continue }
                let name = "lock-\(lang)-\(i + 1)"
                lockAndShow(name)
                unlock()
            }
        }
    }

    // ---------- أدوات ----------

    /// يبدأ أنشطة [scenario] في المعرض وينتظر بدءها
    private func start(_ scenario: String, _ lang: String) -> Bool {
        let app = launch(["-gallery-page", "live", "-gallery-live", scenario, "-gallery-lang", lang])
        let started = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'LIVE STARTED'")).firstMatch
        let ok = started.waitForExistence(timeout: 25)
        if ok {
            // حال الأنشطة (active أو stale…) في سجل الاختبار
            print("GALLERY \(scenario) \(lang): \(started.label)")
        } else {
            save("live-failed-\(scenario)-\(lang)")
            XCTFail("\(scenario) \(lang): " + texts(app))
        }
        return ok
    }

    private func lockAndShow(_ name: String) {
        XCUIDevice.shared.perform(NSSelectorFromString("pressLockButton"))
        sleep(2)
        save(name + "-a")
        // زر الجانب مرة أخرى يوقظ الشاشة على شاشة القفل
        XCUIDevice.shared.perform(NSSelectorFromString("pressLockButton"))
        sleep(2)
        allowIfAsked()
        save(name + "-b")
    }

    /// أول نشاط حي لتطبيق قد يُسأل عنه على شاشة القفل
    private func allowIfAsked() {
        for title in ["Allow", "Always Allow"] {
            let b = springboard.buttons[title]
            if b.exists {
                b.tap()
                sleep(2)
            }
        }
    }

    private func unlock() {
        for _ in 0..<3 {
            XCUIDevice.shared.press(.home)
            sleep(2)
            if springboard.icons.firstMatch.exists && springboard.icons.firstMatch.isHittable { return }
            springboard.swipeUp()
            sleep(1)
        }
    }
}
