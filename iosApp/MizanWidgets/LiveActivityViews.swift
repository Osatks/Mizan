import SwiftUI
import WidgetKit
import ActivityKit

// الأنشطة الحية (iOS 16.2 فما بعد): يبدؤها التطبيق (LiveActivities.swift) ويرسمها هذا الامتداد على شاشة القفل
// وفي الجزيرة الديناميكية. التصميم نفسه في الأدوات: أخضر التطبيق العميق، والذهبي للمهم، ونجمة ثمانية خافتة.
// النصوص كلها من Kotlin بلغة التطبيق، والعدّ التنازلي يتحرك وحده (Text(timerInterval:)) بلا تحديث من التطبيق.

// ---------- العدّ التنازلي للصلاة القادمة ----------

@available(iOS 16.2, *)
struct PrayerActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: PrayerActivityAttributes.self) { context in
            PrayerLockView(state: context.state, arrived: context.isStale)
                .activityBackgroundTint(Color(hex: context.state.hero2))
                .activitySystemActionForegroundColor(Color(hex: context.state.gold))
                .widgetURL(Deep.url("prayer"))
        } dynamicIsland: { context in
            let s = context.state
            // بعد دخول الوقت يصير النشاط «قديماً» (staleDate): «حان الآن» بدل العدّ
            let arrived = context.isStale
            let gold = Color(hex: s.gold)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Label {
                            Text(arrived ? s.name : s.label).lineLimit(1)
                        } icon: {
                            Image(systemName: arrived ? "bell.fill" : "moon.stars.fill")
                        }
                        .font(Mizan.rounded(13, .semibold))
                        .foregroundColor(gold)
                        if !arrived {
                            Text(s.name)
                                .font(Mizan.rounded(24, .bold))
                                .foregroundColor(.white)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                        }
                    }
                    .padding(.leading, 4)
                    .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                    .dynamicIsland(verticalPlacement: .belowIfTooWide)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Group {
                        if arrived, let iqama = s.iqamaAhead {
                            // بعد الأذان: العدّ إلى الإقامة
                            VStack(alignment: .trailing, spacing: 0) {
                                Text(verbatim: s.iqamaLabel ?? "")
                                    .font(Mizan.rounded(11, .medium))
                                    .foregroundColor(.white.opacity(0.7))
                                    .lineLimit(1)
                                CountdownText(at: iqama)
                                    .font(Mizan.rounded(24, .light))
                            }
                        } else if arrived {
                            Text(s.time)
                                .font(Mizan.rounded(22, .semibold))
                                .lineLimit(1)
                                .minimumScaleFactor(0.6)
                        } else {
                            CountdownText(at: s.at)
                                .font(Mizan.rounded(28, .light))
                        }
                    }
                    .foregroundColor(gold)
                    .frame(maxWidth: 120, alignment: .trailing)
                    .padding(.trailing, 4)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        if arrived {
                            Text(s.now)
                                .font(Mizan.rounded(15, .semibold))
                                .foregroundColor(.white)
                                .lineLimit(2)
                            if let dua = s.dua {
                                Text(verbatim: dua)
                                    .font(Mizan.quran(15))
                                    .foregroundColor(gold)
                                    .lineLimit(2)
                                    .minimumScaleFactor(0.7)
                                    .environment(\.layoutDirection, .rightToLeft)
                            }
                        } else {
                            TimeBar(from: s.from, to: s.at, now: Date())
                        }
                        HStack {
                            Text(arrived ? s.hijri : s.time)
                            Spacer(minLength: 6)
                            Text(arrived ? s.place : s.hijri)
                        }
                        .font(Mizan.rounded(12, .medium))
                        .foregroundColor(.white.opacity(0.7))
                        .lineLimit(1)
                    }
                    .padding(.horizontal, 4)
                    .environment(\.layoutDirection, s.rtl ? .rightToLeft : .leftToRight)
                }
            } compactLeading: {
                Image(systemName: arrived ? "bell.fill" : "moon.stars.fill")
                    .foregroundColor(gold)
            } compactTrailing: {
                if arrived, let iqama = s.iqamaAhead {
                    CountdownText(at: iqama)
                        .font(Mizan.rounded(14, .semibold))
                        .foregroundColor(gold)
                        .frame(width: 58)
                } else if arrived {
                    Text(s.name)
                        .font(Mizan.rounded(14, .semibold))
                        .foregroundColor(gold)
                        .lineLimit(1)
                        .frame(maxWidth: 64)
                } else {
                    CountdownText(at: s.at)
                        .font(Mizan.rounded(14, .semibold))
                        .foregroundColor(gold)
                        .frame(width: 58)
                }
            } minimal: {
                Image(systemName: arrived ? "bell.fill" : "moon.stars.fill")
                    .foregroundColor(gold)
            }
            .widgetURL(Deep.url("prayer"))
            .keylineTint(gold)
        }
    }
}

/// شاشة القفل: «الصلاة القادمة · المدينة»، واسم الصلاة كبيراً ووقتها والتاريخ الهجري، والعدّ الذهبي، وشريط ما مضى من الوقت.
/// بعد دخول الوقت: «حان الآن وقت صلاة …» ووقتها
@available(iOS 16.2, *)
struct PrayerLockView: View {
    let state: PrayerLiveState
    let arrived: Bool

    var body: some View {
        let gold = Color(hex: state.gold)
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                if arrived {
                    Image(systemName: "bell.fill").font(.system(size: 11, weight: .semibold))
                } else {
                    StarGlyph().fill(gold).frame(width: 11, height: 11)
                }
                Text(arrived ? state.now : state.label)
                    .font(Mizan.rounded(13, .semibold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Spacer(minLength: 6)
                Text(state.place)
                    .font(Mizan.rounded(12))
                    .foregroundColor(Mizan.text3)
                    .lineLimit(1)
            }
            .foregroundColor(gold)
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.name)
                        .font(Mizan.rounded(30, .bold))
                        .foregroundColor(Mizan.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Text(verbatim: arrived ? state.hijri : "\(state.time) · \(state.hijri)")
                        .font(Mizan.rounded(13, .medium))
                        .foregroundColor(Mizan.text2)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                Spacer(minLength: 8)
                Group {
                    if arrived, let iqama = state.iqamaAhead {
                        // بعد الأذان: «الإقامة بعد» والعدّ إليها
                        VStack(alignment: .trailing, spacing: 0) {
                            Text(verbatim: state.iqamaLabel ?? "")
                                .font(Mizan.rounded(12, .medium))
                                .foregroundColor(Mizan.text2)
                            CountdownText(at: iqama).font(Mizan.rounded(32, .light))
                        }
                    } else if arrived {
                        Text(state.time).font(Mizan.rounded(30, .semibold))
                    } else {
                        CountdownText(at: state.at).font(Mizan.rounded(34, .light))
                    }
                }
                .foregroundColor(gold)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(maxWidth: 150, alignment: .trailing)
            }
            if arrived, let dua = state.dua {
                // رمضان: دعاء الإفطار عند المغرب
                Text(verbatim: dua)
                    .font(Mizan.quran(17))
                    .foregroundColor(gold)
                    .lineLimit(2)
                    .minimumScaleFactor(0.7)
                    .frame(maxWidth: .infinity)
                    .environment(\.layoutDirection, .rightToLeft)
            } else if arrived, let iqama = state.iqamaAhead {
                TimeBar(from: state.at, to: iqama, now: Date())
            } else if !arrived {
                TimeBar(from: state.from, to: state.at, now: Date())
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(alignment: .topTrailing) { LiveOrnament(gold: gold) }
        .environment(\.layoutDirection, state.rtl ? .rightToLeft : .leftToRight)
    }
}

// ---------- أجزاء مشتركة ----------

extension PrayerLiveState {
    /// موعد الإقامة إن لم يأتِ بعد (يُرسم النشاط من جديد عند دخول الوقت، فيعدّ إليها من حينها)
    var iqamaAhead: Date? {
        guard let iqama = iqama, iqama > Date() else { return nil }
        return iqama
    }
}

/// عدّ تنازلي حي حتى [at]: أرقام لاتينية من اليسار إلى اليمين في كل اللغات، ملاصقة لطرف مكانها الأخير
/// (يمين الشاشة في الإنجليزية ويسارها في العربية)
@available(iOS 16.0, *)
struct CountdownText: View {
    let at: Date
    @Environment(\.layoutDirection) private var direction

    var body: some View {
        Text(timerInterval: min(Date(), at)...at, countsDown: true)
            .monospacedDigit()
            .multilineTextAlignment(direction == .rightToLeft ? .leading : .trailing)
            .environment(\.layoutDirection, .leftToRight)
    }
}

/// نجمة ثمانية خافتة في زاوية النشاط (كما في الأدوات)
struct LiveOrnament: View {
    let gold: Color

    var body: some View {
        StarOrnament()
            .stroke(gold.opacity(0.13), lineWidth: 1)
            .frame(width: 130, height: 130)
            .offset(x: 34, y: -42)
            .allowsHitTesting(false)
    }
}
