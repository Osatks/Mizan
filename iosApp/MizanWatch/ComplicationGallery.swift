import SwiftUI
import WidgetKit

// معرض مضاعفات وجه الساعة لالتقاط صورها في المحاكي (ios-gallery.yml)، يُفتح بوسيط التشغيل -watch.page وحده ولا يظهر
// في الاستعمال العادي. يرسم المضاعفات نفسها (ComplicationViews.swift) كما تظهر فعلاً، حول ساعة النظام في أعلى اليمين
// لا تحتها، باللقطة نفسها التي يرسلها iPhone:
//   10: وجه «Modular»: اليوم بجانب الساعة، ومضاعفتان كبيرتان (الصلاة القادمة، والأذكار)، وثلاث دائرية
//   11: المكدس الذكي: مواقيت اليوم، والقبلة، والتاريخ الهجري
//   12: أحوال العدّ: الإقامة بعد الأذان، والإفطار والسحور في رمضان

struct ComplicationGallery: View {
    let payload: WatchPayload?
    let sheet: Int

    var body: some View {
        GeometryReader { g in
            let width = g.size.width - 16
            ZStack(alignment: .top) {
                Color.black
                VStack(alignment: .leading, spacing: 6) {
                    switch sheet {
                    case 10: modular(width)
                    case 11: stack(width)
                    default: states(width)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.top, 12)
            }
        }
        .ignoresSafeArea()
    }

    private var snap: Snapshot? { payload?.snapshot }

    // ---------- 10: وجه Modular ----------

    @ViewBuilder
    private func modular(_ width: CGFloat) -> some View {
        let now = Date()
        // سطر ساعة النظام: اليوم على اليسار والساعة على اليمين
        HStack {
            Text(verbatim: snap?.day(at: now).map { "\($0.weekday) \($0.hijriDay)" } ?? "")
                .font(.system(size: 15, weight: .semibold, design: .rounded))
                .foregroundStyle(Face.gold)
            Spacer()
        }
        .frame(height: 26)
        .environment(\.layoutDirection, .leftToRight)
        face(width, 64) { NextPrayerFace(snap: snap, date: now, family: .accessoryRectangular) }
        face(width, 64) { AdhkarFace(snap: snap, date: now, family: .accessoryRectangular) }
        HStack(spacing: 0) {
            circle { QiblaFace(snap: snap, family: .accessoryCircular) }
            Spacer(minLength: 0)
            circle { NextPrayerFace(snap: snap, date: now, family: .accessoryCircular) }
            Spacer(minLength: 0)
            circle { HijriFace(snap: snap, date: now, family: .accessoryCircular) }
        }
        .frame(width: width)
        .environment(\.layoutDirection, .leftToRight)
    }

    // ---------- 11: المكدس الذكي ----------

    @ViewBuilder
    private func stack(_ width: CGFloat) -> some View {
        let now = Date()
        Spacer().frame(height: 22)
        platter(width) { TodayPrayersFace(snap: snap, date: now) }
        platter(width) { QiblaFace(snap: snap, family: .accessoryRectangular) }
        platter(width) { HijriFace(snap: snap, date: now, family: .accessoryRectangular) }
    }

    // ---------- 12: الإقامة، والإفطار والسحور في رمضان ----------

    @ViewBuilder
    private func states(_ width: CGFloat) -> some View {
        let now = Date()
        let next = snap?.upcoming(after: now, count: 1).first
        // كأن اليوم من رمضان
        let ramadan: Snapshot? = snap.map { s in
            var s = s
            s.watch?.ramadan = s.days.map { $0.date }
            return s
        }
        let maghrib = ramadan?.prayers.first { $0.k == "maghrib" && $0.at > now.timeIntervalSince1970 }
        let fajr = ramadan?.prayers.first { $0.k == "fajr" && $0.at > now.timeIntervalSince1970 }
        Spacer().frame(height: 22)
        platter(width) {
            NextPrayerFace(snap: snap, date: next.map { $0.date.addingTimeInterval(4 * 60) } ?? now, family: .accessoryRectangular)
        }
        platter(width) {
            NextPrayerFace(snap: ramadan, date: maghrib.map { $0.date.addingTimeInterval(-48 * 60) } ?? now, family: .accessoryRectangular)
        }
        platter(width) {
            NextPrayerFace(snap: ramadan, date: fajr.map { $0.date.addingTimeInterval(-75 * 60) } ?? now, family: .accessoryRectangular)
        }
    }

    // ---------- الإطارات ----------

    /// مضاعفة على الوجه مباشرة (بلا خلفية)، بحجمها
    private func face<V: View>(_ width: CGFloat, _ height: CGFloat, @ViewBuilder _ v: () -> V) -> some View {
        v().frame(width: width, height: height)
    }

    /// مضاعفة في المكدس الذكي: شريحة رمادية مستديرة
    private func platter<V: View>(_ width: CGFloat, @ViewBuilder _ v: () -> V) -> some View {
        v()
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .frame(width: width, height: 66)
            .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Color(white: 0.14)))
    }

    private func circle<V: View>(@ViewBuilder _ v: () -> V) -> some View {
        ZStack {
            AccessoryWidgetBackground()
            v()
        }
        .frame(width: 52, height: 52)
        .clipShape(Circle())
    }
}
