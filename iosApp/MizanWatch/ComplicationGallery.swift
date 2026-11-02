import SwiftUI
import WidgetKit

// معرض مضاعفات وجه الساعة لالتقاط صورها في المحاكي (ios-gallery.yml): يُفتح بوسيط التشغيل -watch.page 3 أو 4 وحده،
// ولا يظهر في الاستعمال العادي. يرسم المضاعفات نفسها (ComplicationViews.swift) بأحجام ساعة 45 مم تقريباً،
// كل منها في إطار يبيّن حدوده، باللقطة نفسها التي يرسلها iPhone.

struct ComplicationGallery: View {
    let payload: WatchPayload?
    /// 3: المستطيلة (الصلاة القادمة، ومواقيت اليوم، والقبلة)؛ 4: الدائرية والهجري والسطرية
    let sheet: Int

    private let rect = CGSize(width: 176, height: 70)
    private let circle: CGFloat = 50

    var body: some View {
        let snap = payload?.snapshot
        let now = Date()
        VStack(spacing: 6) {
            if sheet == 3 {
                tile { NextPrayerFace(snap: snap, date: now, family: .accessoryRectangular) }
                tile { TodayPrayersFace(snap: snap, date: now) }
                tile { QiblaFace(snap: snap, family: .accessoryRectangular) }
            } else {
                HStack(spacing: 8) {
                    circular { NextPrayerFace(snap: snap, date: now, family: .accessoryCircular) }
                    circular { QiblaFace(snap: snap, family: .accessoryCircular) }
                    circular { HijriFace(snap: snap, date: now, family: .accessoryCircular) }
                }
                tile { HijriFace(snap: snap, date: now, family: .accessoryRectangular) }
                tile { TasbihFace(title: payload?.tasbih?.title ?? "Tasbih", family: .accessoryRectangular) }
                inline { NextPrayerFace(snap: snap, date: now, family: .accessoryInline) }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.ignoresSafeArea())
    }

    private func tile<V: View>(@ViewBuilder _ v: () -> V) -> some View {
        v()
            .frame(width: rect.width - 12, height: rect.height - 8)
            .padding(.horizontal, 6)
            .padding(.vertical, 4)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.08)))
    }

    private func circular<V: View>(@ViewBuilder _ v: () -> V) -> some View {
        ZStack {
            AccessoryWidgetBackground()
            v()
        }
        .frame(width: circle, height: circle)
        .clipShape(Circle())
    }

    private func inline<V: View>(@ViewBuilder _ v: () -> V) -> some View {
        v()
            .font(.system(size: 14, weight: .medium))
            .lineLimit(1)
            .frame(width: rect.width, height: 18)
    }
}
