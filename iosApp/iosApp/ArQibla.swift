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

    /// كعبة صغيرة سوداء بحزام ذهبي، وحلقة ذهبية مضيئة تحتها، تواجه المستخدم دائماً
    private func buildKaaba() {
        let body = SCNBox(width: 0.42, height: 0.48, length: 0.42, chamferRadius: 0.012)
        let black = SCNMaterial()
        black.diffuse.contents = UIColor(white: 0.06, alpha: 1)
        black.roughness.contents = 0.85
        black.lightingModel = .physicallyBased
        body.materials = [black]
        kaaba.addChildNode(SCNNode(geometry: body))

        let band = SCNBox(width: 0.425, height: 0.05, length: 0.425, chamferRadius: 0.004)
        let goldMaterial = SCNMaterial()
        goldMaterial.diffuse.contents = gold
        goldMaterial.emission.contents = gold.withAlphaComponent(0.35)
        goldMaterial.metalness.contents = 0.9
        goldMaterial.roughness.contents = 0.3
        goldMaterial.lightingModel = .physicallyBased
        band.materials = [goldMaterial]
        let bandNode = SCNNode(geometry: band)
        bandNode.position = SCNVector3(0, 0.12, 0)
        kaaba.addChildNode(bandNode)

        let ring = SCNTorus(ringRadius: 0.46, pipeRadius: 0.012)
        let glow = SCNMaterial()
        glow.diffuse.contents = gold
        glow.emission.contents = gold
        glow.lightingModel = .constant
        ring.materials = [glow]
        let ringNode = SCNNode(geometry: ring)
        ringNode.position = SCNVector3(0, -0.26, 0)
        ringNode.runAction(.repeatForever(.sequence([.fadeOpacity(to: 0.35, duration: 1.1), .fadeOpacity(to: 1, duration: 1.1)])))
        kaaba.addChildNode(ringNode)

        // يدور حول محوره الرأسي فقط ليواجه الكاميرا
        let face = SCNBillboardConstraint()
        face.freeAxes = .Y
        kaaba.constraints = [face]
        kaaba.isHidden = true
        sceneView.scene.rootNode.addChildNode(kaaba)

        let light = SCNLight()
        light.type = .omni
        light.intensity = 900
        let lightNode = SCNNode()
        lightNode.light = light
        lightNode.position = SCNVector3(0, 0.8, 0.6)
        kaaba.addChildNode(lightNode)
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
        ])
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
        if facing && !wasFacing { haptic.notificationOccurred(.success) }
        wasFacing = facing
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
