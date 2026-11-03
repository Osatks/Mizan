import UIKit
import ARKit
import SceneKit
import AVFoundation
import Shared

/// القبلة بالكاميرا (ARKit): الكعبة على بُعد أمتار في اتجاه القبلة فوق ما تراه الكاميرا، وفي أسفل الشاشة كم يستدير
/// المستخدم وإلى أي جهة، ثم «أنت تتجه إلى القبلة» مع اهتزاز خفيف. محاور ARKit (gravityAndHeading): y إلى الأعلى،
/// و ‎-z إلى الشمال الحقيقي، و x إلى الشرق؛ فاتجاه القبلة (من الشمال باتجاه عقارب الساعة) هو (sin θ, 0, −cos θ).
/// الكاميرا على الجهاز فقط: لا يُصوَّر شيء ولا يُحفظ.
enum ArQibla {
    /// من LiveActivities.start: يخبر Kotlin بدعم الجهاز، ويعرض الشاشة حين يطلبها زر شاشة القبلة
    static func install() {
        IosArQibla.shared.start(supported: ARWorldTrackingConfiguration.isSupported)
        NotificationCenter.default.addObserver(forName: Notification.Name("MizanArQibla"), object: nil, queue: .main) { note in
            guard let data = (note.object as? String)?.data(using: .utf8),
                  let info = try? JSONDecoder().decode(ArQiblaInfo.self, from: data) else { return }
            present(info)
        }
    }

    private static func present(_ info: ArQiblaInfo) {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap { $0.windows }.first { $0.isKeyWindow } ?? scenes.first?.windows.first
        guard var top = window?.rootViewController else { return }
        while let next = top.presentedViewController { top = next }
        if top is ArQiblaController { return }
        let vc = ArQiblaController(info: info)
        vc.modalPresentationStyle = .fullScreen
        top.present(vc, animated: true)
    }
}

struct ArQiblaInfo: Decodable {
    var bearing: Double
    var distance: String
    var place: String
    var title: String
    var hint: String
    var facing: String
    var turn: String
    var cameraDenied: String
    var unsupported: String
    var openSettings: String
    var close: String
    var rtl: Bool
    var hero1: String
    var hero2: String
    var gold: String
    /// معايرة البوصلة: العنوان، والخطوات (سطر لكل خطوة)، وإعادة الضبط، والإغلاق
    var calibTitle: String? = nil
    var calibSteps: String? = nil
    var calibReset: String? = nil
    var calibDone: String? = nil
}

final class ArQiblaController: UIViewController, ARSCNViewDelegate {
    private let info: ArQiblaInfo
    private let sceneView = ARSCNView()
    private let kaaba = SCNNode()
    private let hud = UIView()
    private let hudIcon = UIImageView()
    private let hudText = UILabel()
    private let hintLabel = UILabel()
    private let message = UIStackView()
    private let haptic = UINotificationFeedbackGenerator()
    /// نقرة خفيفة كلما اقترب الاتجاه عشر درجات
    private let tick = UISelectionFeedbackGenerator()
    private var lastBucket = -1
    /// سهم على حافة الشاشة نحو الكعبة حين تكون خارج النظر
    private let edgeArrow = UIImageView()
    private let calibrate = UIButton(type: .system)
    private var lastState = ""
    private var wasFacing = false
    private var tracking = false

    /// القبلة ضمن هذه الزاوية من اتجاه الكاميرا: «أنت تتجه إلى القبلة»
    private static let tolerance = 5.0
    /// بُعد الكعبة الافتراضية عن الكاميرا بالأمتار
    private static let distance: Float = 4

    init(info: ArQiblaInfo) {
        self.info = info
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) is not used") }

    private var gold: UIColor { UIColor(hex: info.gold) }

    override var prefersStatusBarHidden: Bool { true }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        sceneView.frame = view.bounds
        sceneView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        sceneView.delegate = self
        sceneView.scene = SCNScene()
        sceneView.automaticallyUpdatesLighting = true
        sceneView.rendersContinuously = true
        view.addSubview(sceneView)
        buildKaaba()
        buildOverlay()
        haptic.prepare()
        tick.prepare()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .denied, .restricted:
            showMessage(info.cameraDenied, settings: true)
            return
        default: break
        }
        guard ARWorldTrackingConfiguration.isSupported else {
            showMessage(info.unsupported, settings: false)
            return
        }
        let config = ARWorldTrackingConfiguration()
        config.worldAlignment = .gravityAndHeading
        sceneView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
        UIApplication.shared.isIdleTimerDisabled = true
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        sceneView.session.pause()
        UIApplication.shared.isIdleTimerDisabled = false
    }

    // ---------- المشهد ----------

    /// الكعبة: كسوة سوداء بحريرها وطيّاتها، وحزام ذهبي مزخرف، وباب ذهبي على الوجه المقابل للمستخدم، وقاعدة رخامية
    /// (الشاذروان)؛ والقوام كله مرسوم في صور (إضاءة ثابتة): لا تبيضّ تحت ضوء الغرفة القوي ولا تسودّ في العتمة.
    /// وتحتها هالة ذهبية تتموّج، وعمود نور يصعد منها حين تتجه إلى القبلة. تواجه المستخدم دائماً.
    private let beam = SCNNode()
    private let model = SCNNode()

    private func buildKaaba() {
        let w: CGFloat = 0.40, h: CGFloat = 0.46, l: CGFloat = 0.44
        let body = SCNBox(width: w, height: h, length: l, chamferRadius: 0.003)
        // الوجوه: الأمام، اليمين، الخلف، اليسار، الأعلى، الأسفل (الجانبان أعتم قليلاً فتظهر الكتلة)
        body.materials = [
            flat(kiswa(door: true, shade: 1.0, aspect: w / h)),
            flat(kiswa(door: false, shade: 0.72, aspect: l / h)),
            flat(kiswa(door: false, shade: 0.8, aspect: w / h)),
            flat(kiswa(door: false, shade: 0.72, aspect: l / h)),
            flat(UIColor(white: 0.13, alpha: 1)),
            flat(UIColor(white: 0.05, alpha: 1)),
        ]
        model.addChildNode(SCNNode(geometry: body))

        // الشاذروان: قاعدة رخامية مائلة الحواف
        let base = SCNBox(width: w + 0.04, height: 0.026, length: l + 0.04, chamferRadius: 0.006)
        base.materials = [flat(marble())]
        let baseNode = SCNNode(geometry: base)
        baseNode.position = SCNVector3(0, Float(-h / 2 - 0.013), 0)
        model.addChildNode(baseNode)
        kaaba.addChildNode(model)

        let ground = Float(-h / 2 - 0.026)
        // هالة ناعمة على الأرض
        let glowPlane = SCNPlane(width: 1.3, height: 1.3)
        let glowMat = flat(radial(gold))
        glowMat.blendMode = .add
        glowMat.writesToDepthBuffer = false
        glowPlane.materials = [glowMat]
        let glowNode = SCNNode(geometry: glowPlane)
        glowNode.eulerAngles.x = -.pi / 2
        glowNode.position = SCNVector3(0, ground + 0.002, 0)
        glowNode.runAction(.repeatForever(.sequence([.fadeOpacity(to: 0.55, duration: 1.6), .fadeOpacity(to: 1, duration: 1.6)])))
        kaaba.addChildNode(glowNode)

        // حلقة رفيعة ثابتة، وأخرى تتموّج منها وتتلاشى
        func ring(_ radius: CGFloat) -> SCNNode {
            let t = SCNTorus(ringRadius: radius, pipeRadius: 0.004)
            let m = flat(gold)
            m.emission.contents = gold
            t.materials = [m]
            let n = SCNNode(geometry: t)
            n.position = SCNVector3(0, ground + 0.004, 0)
            return n
        }
        kaaba.addChildNode(ring(0.4))
        let ripple = ring(0.4)
        ripple.runAction(.repeatForever(.sequence([
            .group([.scale(to: 1.7, duration: 2.2), .fadeOut(duration: 2.2)]),
            .group([.scale(to: 1, duration: 0), .fadeIn(duration: 0)]),
        ])))
        kaaba.addChildNode(ripple)

        // عمود النور: يظهر حين تتجه إلى القبلة
        let column = SCNCylinder(radius: 0.06, height: 2.6)
        let beamMat = flat(beamImage(gold))
        beamMat.blendMode = .add
        beamMat.writesToDepthBuffer = false
        beamMat.isDoubleSided = true
        column.materials = [beamMat]
        beam.geometry = column
        beam.position = SCNVector3(0, Float(h / 2) + 1.3, 0)
        beam.opacity = 0
        kaaba.addChildNode(beam)

        // يدور حول محوره الرأسي فقط ليواجه الكاميرا
        let face = SCNBillboardConstraint()
        face.freeAxes = .Y
        kaaba.constraints = [face]
        kaaba.isHidden = true
        sceneView.scene.rootNode.addChildNode(kaaba)
    }

    /// مادة بلا إضاءة: لونها أو صورتها كما رُسمت
    private func flat(_ contents: Any) -> SCNMaterial {
        let m = SCNMaterial()
        m.diffuse.contents = contents
        m.lightingModel = .constant
        return m
    }

    private func shade(_ c: UIColor, _ f: CGFloat) -> UIColor {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        c.getRed(&r, green: &g, blue: &b, alpha: &a)
        return UIColor(red: min(1, r * f), green: min(1, g * f), blue: min(1, b * f), alpha: a)
    }

    /// وجه من الكسوة: حرير أسود بطيّات، والحزام الذهبي بزخرفته في ثلثها الأعلى، والباب على الوجه الأمامي
    private func kiswa(door: Bool, shade k: CGFloat, aspect: CGFloat) -> UIImage {
        let H: CGFloat = 640, W = (H * aspect).rounded()
        let gold = self.gold
        return UIGraphicsImageRenderer(size: CGSize(width: W, height: H)).image { ctx in
            let c = ctx.cgContext
            let space = CGColorSpaceCreateDeviceRGB()
            // الحرير: أفتح قليلاً في الأعلى
            let silk = CGGradient(colorsSpace: space, colors: [
                UIColor(white: 0.16 * k, alpha: 1).cgColor, UIColor(white: 0.07 * k, alpha: 1).cgColor, UIColor(white: 0.03, alpha: 1).cgColor,
            ] as CFArray, locations: [0, 0.45, 1])!
            c.drawLinearGradient(silk, start: .zero, end: CGPoint(x: 0, y: H), options: [])
            // طيّات القماش ونقش خفيف في نسيجه
            for x in stride(from: CGFloat(10), to: W, by: 22) {
                c.setFillColor(UIColor(white: 1, alpha: 0.025 * k).cgColor)
                c.fill(CGRect(x: x, y: 0, width: 3, height: H))
            }
            for y in stride(from: CGFloat(30), to: H, by: 46) {
                for x in stride(from: CGFloat(y.truncatingRemainder(dividingBy: 92) == 30 ? 0 : 23), to: W, by: 46) {
                    c.setFillColor(UIColor(white: 1, alpha: 0.03 * k).cgColor)
                    c.fillEllipse(in: CGRect(x: x, y: y, width: 5, height: 5))
                }
            }
            // الحزام الذهبي
            let band = CGRect(x: 0, y: H * 0.2, width: W, height: H * 0.09)
            c.saveGState()
            c.clip(to: band)
            let metal = CGGradient(colorsSpace: space, colors: [
                self.shade(gold, 0.55 * k).cgColor, self.shade(gold, 1.15 * k).cgColor, self.shade(gold, 0.7 * k).cgColor,
            ] as CFArray, locations: [0, 0.5, 1])!
            c.drawLinearGradient(metal, start: CGPoint(x: 0, y: band.minY), end: CGPoint(x: 0, y: band.maxY), options: [])
            // زخرفة الحزام: معيّنات متصلة بنقاط، كخط الثلث في الكسوة من بعيد
            c.setStrokeColor(self.shade(gold, 0.35).cgColor)
            c.setLineWidth(2)
            let step = band.height * 0.9
            var x: CGFloat = 0
            while x < W {
                let mid = band.midY, r = band.height * 0.3
                c.move(to: CGPoint(x: x, y: mid)); c.addLine(to: CGPoint(x: x + step / 2, y: mid - r))
                c.addLine(to: CGPoint(x: x + step, y: mid)); c.addLine(to: CGPoint(x: x + step / 2, y: mid + r)); c.closePath()
                c.strokePath()
                c.setFillColor(self.shade(gold, 0.35).cgColor)
                c.fillEllipse(in: CGRect(x: x + step / 2 - 3, y: mid - 3, width: 6, height: 6))
                x += step
            }
            c.restoreGState()
            // خيطان ذهبيان رفيعان فوق الحزام وتحته
            c.setFillColor(self.shade(gold, 0.9 * k).cgColor)
            c.fill(CGRect(x: 0, y: band.minY - 7, width: W, height: 2.5))
            c.fill(CGRect(x: 0, y: band.maxY + 4.5, width: W, height: 2.5))
            // قناديل (مربعات مطرزة) تحت الحزام
            for i in 0..<3 {
                let cx = W * (0.2 + 0.3 * CGFloat(i))
                let sq = CGRect(x: cx - 15, y: band.maxY + 26, width: 30, height: 30)
                c.saveGState()
                c.translateBy(x: sq.midX, y: sq.midY); c.rotate(by: .pi / 4)
                c.setStrokeColor(self.shade(gold, 0.8 * k).cgColor); c.setLineWidth(2.5)
                c.stroke(CGRect(x: -11, y: -11, width: 22, height: 22))
                c.restoreGState()
            }
            guard door else { return }
            // الباب: إطار ذهبي بقوس خفيف وألواح، مرتفع عن الأرض كما هو
            let d = CGRect(x: W * 0.56, y: H * 0.48, width: W * 0.24, height: H * 0.4)
            let frame = UIBezierPath(roundedRect: d, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: d.width * 0.28, height: d.width * 0.28))
            c.saveGState()
            c.addPath(frame.cgPath); c.clip()
            let doorMetal = CGGradient(colorsSpace: space, colors: [
                self.shade(gold, 1.2).cgColor, self.shade(gold, 0.85).cgColor, self.shade(gold, 0.6).cgColor,
            ] as CFArray, locations: [0, 0.5, 1])!
            c.drawLinearGradient(doorMetal, start: CGPoint(x: d.minX, y: d.minY), end: CGPoint(x: d.maxX, y: d.maxY), options: [])
            c.restoreGState()
            c.setStrokeColor(self.shade(gold, 0.4).cgColor); c.setLineWidth(3)
            c.addPath(frame.cgPath); c.strokePath()
            // مصراعان وألواح
            c.setLineWidth(2)
            c.move(to: CGPoint(x: d.midX, y: d.minY + d.width * 0.25)); c.addLine(to: CGPoint(x: d.midX, y: d.maxY)); c.strokePath()
            for row in 0..<3 {
                let y = d.minY + d.height * (0.3 + 0.22 * CGFloat(row))
                for col in 0..<2 {
                    let px = d.minX + d.width * (0.1 + 0.45 * CGFloat(col))
                    c.stroke(CGRect(x: px, y: y, width: d.width * 0.35, height: d.height * 0.16))
                }
            }
        }
    }

    /// رخام الشاذروان: أبيض مائل إلى الرمادي بعروق خفيفة
    private func marble() -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 256, height: 32)).image { ctx in
            let c = ctx.cgContext
            c.setFillColor(UIColor(white: 0.86, alpha: 1).cgColor)
            c.fill(CGRect(x: 0, y: 0, width: 256, height: 32))
            c.setStrokeColor(UIColor(white: 0.7, alpha: 0.6).cgColor)
            c.setLineWidth(1)
            for i in 0..<6 {
                c.move(to: CGPoint(x: CGFloat(i) * 48, y: 0)); c.addLine(to: CGPoint(x: CGFloat(i) * 48 + 30, y: 32))
            }
            c.strokePath()
        }
    }

    /// هالة دائرية: ذهبي في الوسط يتلاشى إلى الشفافية
    private func radial(_ color: UIColor) -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 256, height: 256)).image { ctx in
            let g = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [
                color.withAlphaComponent(0.55).cgColor, color.withAlphaComponent(0.18).cgColor, color.withAlphaComponent(0).cgColor,
            ] as CFArray, locations: [0, 0.45, 1])!
            ctx.cgContext.drawRadialGradient(g, startCenter: CGPoint(x: 128, y: 128), startRadius: 0,
                                             endCenter: CGPoint(x: 128, y: 128), endRadius: 128, options: [])
        }
    }

    /// عمود النور: ساطع في أسفله ويتلاشى صعوداً
    private func beamImage(_ color: UIColor) -> UIImage {
        UIGraphicsImageRenderer(size: CGSize(width: 8, height: 256)).image { ctx in
            let g = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [
                color.withAlphaComponent(0).cgColor, color.withAlphaComponent(0.25).cgColor, color.withAlphaComponent(0.7).cgColor,
            ] as CFArray, locations: [0, 0.6, 1])!
            ctx.cgContext.drawLinearGradient(g, start: .zero, end: CGPoint(x: 0, y: 256), options: [])
        }
    }

    // ---------- الواجهة فوق الكاميرا ----------

    private func buildOverlay() {
        let close = UIButton(type: .system)
        close.setImage(UIImage(systemName: "xmark", withConfiguration: UIImage.SymbolConfiguration(pointSize: 16, weight: .bold)), for: .normal)
        close.tintColor = .white
        close.backgroundColor = UIColor.black.withAlphaComponent(0.45)
        close.layer.cornerRadius = 22
        close.accessibilityLabel = info.close
        close.addTarget(self, action: #selector(dismissSelf), for: .touchUpInside)
        close.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(close)

        let title = UILabel()
        title.text = info.title
        title.font = .systemFont(ofSize: 17, weight: .bold)
        title.textColor = .white
        let place = UILabel()
        place.text = "\(info.place) · \(info.distance)"
        place.font = .systemFont(ofSize: 12, weight: .medium)
        place.textColor = UIColor.white.withAlphaComponent(0.8)
        place.adjustsFontSizeToFitWidth = true
        place.minimumScaleFactor = 0.7
        let header = UIStackView(arrangedSubviews: [title, place])
        header.axis = .vertical
        header.alignment = .leading
        header.spacing = 2
        header.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(header)

        hintLabel.text = info.hint
        hintLabel.font = .systemFont(ofSize: 14, weight: .semibold)
        hintLabel.textColor = .white
        hintLabel.numberOfLines = 0
        hintLabel.textAlignment = .center
        hintLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(hintLabel)

        hud.backgroundColor = UIColor(hex: info.hero2).withAlphaComponent(0.88)
        hud.layer.cornerRadius = 26
        hud.layer.borderWidth = 1
        hud.layer.borderColor = gold.withAlphaComponent(0.45).cgColor
        hud.translatesAutoresizingMaskIntoConstraints = false
        hudIcon.tintColor = gold
        hudIcon.contentMode = .scaleAspectFit
        hudIcon.translatesAutoresizingMaskIntoConstraints = false
        hudText.font = .systemFont(ofSize: 17, weight: .bold)
        hudText.textColor = .white
        hudText.adjustsFontSizeToFitWidth = true
        hudText.minimumScaleFactor = 0.6
        hudText.translatesAutoresizingMaskIntoConstraints = false
        let row = UIStackView(arrangedSubviews: [hudIcon, hudText])
        row.spacing = 10
        row.alignment = .center
        row.translatesAutoresizingMaskIntoConstraints = false
        hud.addSubview(row)
        hud.alpha = 0
        view.addSubview(hud)

        edgeArrow.tintColor = gold
        edgeArrow.contentMode = .scaleAspectFit
        edgeArrow.alpha = 0
        edgeArrow.layer.shadowColor = UIColor.black.cgColor
        edgeArrow.layer.shadowOpacity = 0.5
        edgeArrow.layer.shadowRadius = 6
        edgeArrow.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(edgeArrow)

        // معايرة البوصلة: خطواتها، و«إعادة الضبط» تعيد تتبّع الاتجاه من جديد
        var cal = UIButton.Configuration.plain()
        cal.image = UIImage(systemName: "location.north.circle", withConfiguration: UIImage.SymbolConfiguration(pointSize: 14, weight: .semibold))
        cal.imagePadding = 6
        cal.title = info.calibTitle
        cal.baseForegroundColor = .white
        cal.background.backgroundColor = UIColor.black.withAlphaComponent(0.45)
        cal.cornerStyle = .capsule
        cal.contentInsets = NSDirectionalEdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14)
        cal.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { a in
            var a = a; a.font = .systemFont(ofSize: 13, weight: .semibold); return a
        }
        calibrate.configuration = cal
        calibrate.isHidden = info.calibTitle == nil
        calibrate.addTarget(self, action: #selector(showCalibration), for: .touchUpInside)
        calibrate.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(calibrate)

        // الجهتان بلغة التطبيق (قد تخالف لغة الجهاز): العنوان في البداية وزر الإغلاق في النهاية
        let g = view.safeAreaLayoutGuide
        let rtl = info.rtl
        for v in [header, row] { v.semanticContentAttribute = rtl ? .forceRightToLeft : .forceLeftToRight }
        header.alignment = .leading
        for l in [title, place] { l.textAlignment = rtl ? .right : .left }
        NSLayoutConstraint.activate([
            close.topAnchor.constraint(equalTo: g.topAnchor, constant: 8),
            rtl ? close.leftAnchor.constraint(equalTo: g.leftAnchor, constant: 16) : close.rightAnchor.constraint(equalTo: g.rightAnchor, constant: -16),
            close.widthAnchor.constraint(equalToConstant: 44),
            close.heightAnchor.constraint(equalToConstant: 44),
            rtl ? header.rightAnchor.constraint(equalTo: g.rightAnchor, constant: -20) : header.leftAnchor.constraint(equalTo: g.leftAnchor, constant: 20),
            rtl ? header.leftAnchor.constraint(greaterThanOrEqualTo: close.rightAnchor, constant: 12) : header.rightAnchor.constraint(lessThanOrEqualTo: close.leftAnchor, constant: -12),
            header.centerYAnchor.constraint(equalTo: close.centerYAnchor),
            hintLabel.leadingAnchor.constraint(equalTo: g.leadingAnchor, constant: 32),
            hintLabel.trailingAnchor.constraint(equalTo: g.trailingAnchor, constant: -32),
            hintLabel.centerYAnchor.constraint(equalTo: g.centerYAnchor),
            hud.centerXAnchor.constraint(equalTo: g.centerXAnchor),
            hud.bottomAnchor.constraint(equalTo: g.bottomAnchor, constant: -24),
            hud.heightAnchor.constraint(equalToConstant: 52),
            hud.widthAnchor.constraint(lessThanOrEqualTo: g.widthAnchor, constant: -40),
            row.leadingAnchor.constraint(equalTo: hud.leadingAnchor, constant: 18),
            row.trailingAnchor.constraint(equalTo: hud.trailingAnchor, constant: -18),
            row.centerYAnchor.constraint(equalTo: hud.centerYAnchor),
            hudIcon.widthAnchor.constraint(equalToConstant: 24),
            hudIcon.heightAnchor.constraint(equalToConstant: 24),
            edgeArrow.centerYAnchor.constraint(equalTo: g.centerYAnchor),
            edgeArrow.widthAnchor.constraint(equalToConstant: 54),
            edgeArrow.heightAnchor.constraint(equalToConstant: 54),
            calibrate.centerXAnchor.constraint(equalTo: g.centerXAnchor),
            calibrate.bottomAnchor.constraint(equalTo: hud.topAnchor, constant: -12),
        ])
        arrowLeft = edgeArrow.leftAnchor.constraint(equalTo: g.leftAnchor, constant: 12)
        arrowRight = edgeArrow.rightAnchor.constraint(equalTo: g.rightAnchor, constant: -12)
        arrowRight?.isActive = true
    }

    /// رسالة بدل الكاميرا (لا إذن، أو الجهاز لا يدعم الواقع المعزز)، وزر الإعدادات إن نفع
    private func showMessage(_ text: String, settings: Bool) {
        hintLabel.isHidden = true
        hud.isHidden = true
        let label = UILabel()
        label.text = text
        label.textColor = .white
        label.font = .systemFont(ofSize: 17, weight: .semibold)
        label.numberOfLines = 0
        label.textAlignment = .center
        message.arrangedSubviews.forEach { $0.removeFromSuperview() }
        message.addArrangedSubview(label)
        if settings {
            var config = UIButton.Configuration.filled()
            config.title = info.openSettings
            config.baseBackgroundColor = gold
            config.baseForegroundColor = .black
            config.cornerStyle = .capsule
            config.contentInsets = NSDirectionalEdgeInsets(top: 12, leading: 24, bottom: 12, trailing: 24)
            config.titleTextAttributesTransformer = UIConfigurationTextAttributesTransformer { attrs in
                var a = attrs
                a.font = .systemFont(ofSize: 16, weight: .bold)
                return a
            }
            let button = UIButton(configuration: config)
            button.addTarget(self, action: #selector(openSettings), for: .touchUpInside)
            message.addArrangedSubview(button)
        }
        message.axis = .vertical
        message.spacing = 18
        message.alignment = .center
        message.translatesAutoresizingMaskIntoConstraints = false
        if message.superview == nil {
            view.addSubview(message)
            NSLayoutConstraint.activate([
                message.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 32),
                message.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -32),
                message.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor),
            ])
        }
    }

    private var arrowLeft: NSLayoutConstraint?
    private var arrowRight: NSLayoutConstraint?

    /// خطوات المعايرة، و«إعادة الضبط»: يبدأ التتبّع من جديد بعد المعايرة فيستقر الشمال
    @objc private func showCalibration() {
        let alert = UIAlertController(title: info.calibTitle, message: info.calibSteps, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: info.calibReset ?? "Reset", style: .default) { [weak self] _ in
            guard let self = self else { return }
            let config = ARWorldTrackingConfiguration()
            config.worldAlignment = .gravityAndHeading
            self.sceneView.session.run(config, options: [.resetTracking, .removeExistingAnchors])
            self.lastState = ""
        })
        alert.addAction(UIAlertAction(title: info.calibDone ?? "OK", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func dismissSelf() { dismiss(animated: true) }

    @objc private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
    }

    // ---------- كل إطار ----------

    func renderer(_ renderer: SCNSceneRenderer, updateAtTime time: TimeInterval) {
        guard let frame = sceneView.session.currentFrame else { return }
        let t = frame.camera.transform
        let position = simd_make_float3(t.columns.3)
        let forward = -simd_make_float3(t.columns.2)
        let b = info.bearing * .pi / 180
        let dir = simd_float3(Float(sin(b)), 0, Float(-cos(b)))
        let target = position + dir * Self.distance + simd_float3(0, -0.25, 0)
        kaaba.simdPosition = target
        // بمطابقة الحالة لا بـ ==: مقارنة TrackingState من iOS 16 فقط
        var normal = false
        if case .normal = frame.camera.trackingState { normal = true }

        // اتجاه الكاميرا على الأرض (الشمال 0، والشرق 90)؛ لا اتجاه إن كانت نحو الأرض أو السماء
        let flat = hypot(Double(forward.x), Double(forward.z))
        var state = "hint"
        var delta = 0.0
        if normal && flat > 0.35 {
            let heading = atan2(Double(forward.x), Double(-forward.z)) * 180 / .pi
            delta = (info.bearing - heading).truncatingRemainder(dividingBy: 360)
            if delta > 180 { delta -= 360 }
            if delta < -180 { delta += 360 }
            state = abs(delta) < Self.tolerance ? "facing" : (delta > 0 ? "right" : "left")
        }
        let degrees = Int(abs(delta).rounded())
        let key = state == "hint" || state == "facing" ? state : "\(state)\(degrees)"
        guard key != lastState else { return }
        lastState = key
        DispatchQueue.main.async { self.update(state: state, degrees: degrees, tracking: normal) }
    }

    private func update(state: String, degrees: Int, tracking normal: Bool) {
        kaaba.isHidden = !normal
        if normal != tracking {
            tracking = normal
            UIView.animate(withDuration: 0.25) {
                self.hintLabel.alpha = normal ? 0 : 1
                self.hud.alpha = normal ? 1 : 0
            }
        }
        let facing = state == "facing"
        switch state {
        case "facing":
            hudIcon.image = UIImage(systemName: "checkmark.circle.fill")
            hudText.text = info.facing
            hudText.textColor = gold
        case "right", "left":
            // السهم إلى الجهة التي يستدير إليها المستخدم على الشاشة (لا يتبع اتجاه اللغة)
            hudIcon.image = UIImage(systemName: state == "right" ? "arrow.right.circle.fill" : "arrow.left.circle.fill")
            hudText.text = "\(info.turn) · \(degrees)°"
            hudText.textColor = .white
        default:
            hudIcon.image = UIImage(systemName: "location.north.line.fill")
            hudText.text = info.hint
            hudText.textColor = .white
        }
        if facing && !wasFacing {
            haptic.notificationOccurred(.success)
            // عمود النور ونبضة الكعبة
            beam.runAction(.fadeOpacity(to: 0.85, duration: 0.5))
            model.runAction(.sequence([.scale(to: 1.08, duration: 0.18), .scale(to: 1, duration: 0.3)]))
        } else if !facing && wasFacing {
            beam.runAction(.fadeOpacity(to: 0, duration: 0.4))
        }
        wasFacing = facing
        // نقرة خفيفة كلما اقترب عشر درجات (لا عند الابتعاد الكبير)
        let bucket = state == "hint" ? -1 : (facing ? 0 : min(degrees / 10 + 1, 19))
        if bucket != lastBucket, bucket > 0, bucket < lastBucket || lastBucket <= 0, degrees < 90 { tick.selectionChanged() }
        lastBucket = bucket
        // الكعبة خارج النظر: سهم ذهبي ينبض على حافة الشاشة في جهتها
        let away = (state == "right" || state == "left") && degrees > 28
        if away {
            let right = state == "right"
            edgeArrow.image = UIImage(systemName: right ? "chevron.right.2" : "chevron.left.2",
                                      withConfiguration: UIImage.SymbolConfiguration(pointSize: 34, weight: .bold))
            arrowRight?.isActive = right
            arrowLeft?.isActive = !right
            if edgeArrow.layer.animation(forKey: "nudge") == nil {
                let a = CABasicAnimation(keyPath: "transform.translation.x")
                a.fromValue = 0
                a.toValue = right ? 8 : -8
                a.duration = 0.6
                a.autoreverses = true
                a.repeatCount = .infinity
                edgeArrow.layer.add(a, forKey: "nudge")
            }
        } else {
            edgeArrow.layer.removeAnimation(forKey: "nudge")
        }
        UIView.animate(withDuration: 0.25) { self.edgeArrow.alpha = away ? 1 : 0 }
    }

    func session(_ session: ARSession, didFailWithError error: Error) {
        DispatchQueue.main.async {
            if let e = error as? ARError, e.code == .cameraUnauthorized {
                self.showMessage(self.info.cameraDenied, settings: true)
            } else {
                self.showMessage(self.info.unsupported, settings: false)
            }
        }
    }
}

private extension UIColor {
    /// "#RRGGBB" كما في رسائل Kotlin
    convenience init(hex: String) {
        var s = hex.trimmingCharacters(in: .whitespaces)
        if s.hasPrefix("#") { s.removeFirst() }
        var v: UInt64 = 0
        _ = Scanner(string: s).scanHexInt64(&v)
        self.init(red: CGFloat((v >> 16) & 0xFF) / 255, green: CGFloat((v >> 8) & 0xFF) / 255, blue: CGFloat(v & 0xFF) / 255, alpha: 1)
    }
}
