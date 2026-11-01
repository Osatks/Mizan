import UIKit
import CoreSpotlight
import Shared

/// نتائج بحث iPhone (Spotlight): معرّف كل سورة هو مسار صفحتها (QuranSpotlight.kt)، فيفتحها لمسُ النتيجة
enum SpotlightLink {
    @discardableResult
    static func open(_ activity: NSUserActivity) -> Bool {
        guard activity.activityType == CSSearchableItemActionType,
              let route = activity.userInfo?[CSSearchableItemActivityIdentifier] as? String else { return false }
        IosLive.shared.open(route: route, section: nil)
        return true
    }
}

/// امتداد شاشة الإقلاع: لون LaunchBackground والشعار LaunchEmblem كما في UILaunchScreen (Info.plist)،
/// يبقى فوق النافذة لحظة حتى يرسم Compose أول إطار، ثم يذوب في الشاشة الأولى. ولون النافذة نفسه حتى لا يومض أسود.
enum LaunchCurtain {
    static func cover(_ window: UIWindow) {
        guard let color = UIColor(named: "LaunchBackground") else { return }
        window.backgroundColor = color
        let curtain = UIView(frame: window.bounds)
        curtain.backgroundColor = color
        curtain.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        curtain.isUserInteractionEnabled = false
        if let image = UIImage(named: "LaunchEmblem") {
            let emblem = UIImageView(image: image)
            emblem.center = CGPoint(x: curtain.bounds.midX, y: curtain.bounds.midY)
            emblem.autoresizingMask = [.flexibleLeftMargin, .flexibleRightMargin, .flexibleTopMargin, .flexibleBottomMargin]
            curtain.addSubview(emblem)
        }
        window.addSubview(curtain)
        UIView.animate(withDuration: 0.4, delay: 0.3, options: [.curveEaseOut, .allowUserInteraction], animations: {
            curtain.alpha = 0
            curtain.transform = CGAffineTransform(scaleX: 1.06, y: 1.06)
        }, completion: { _ in curtain.removeFromSuperview() })
    }
}
