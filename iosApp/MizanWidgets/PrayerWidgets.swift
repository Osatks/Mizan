import WidgetKit
import SwiftUI

// ---------- الصلاة القادمة ----------

struct NextPrayerWidget: Widget {
    let kind = "MizanNextPrayer"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .prayer)) { entry in
            NextPrayerView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("nextPrayer"))
        .description(Fallback.gallery("descNext"))
        .supportedFamilies(Families.make([.systemSmall, .systemMedium], circular: true, rectangular: true, inline: true))
    }
}

struct NextPrayerView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    var body: some View {
        if family.isAccessory {
            if #available(iOS 16.0, *) {
                NextPrayerAccessory(entry: entry, family: family)
            }
        } else {
            Shell(snap: entry.snap, url: Deep.url("prayer")) {
                if let snap = entry.snap, let next = snap.nextPrayer(after: entry.date) {
                    if family == .systemMedium {
                        HStack(spacing: 14) {
                            NextPrayerBlock(snap: snap, next: next, now: entry.date)
                            Rectangle().fill(Color.white.opacity(0.12)).frame(width: 1).padding(.vertical, 4)
                            PrayerList(snap: snap, next: next, now: entry.date, compact: true)
                                .frame(maxWidth: .infinity)
                        }
                    } else {
                        if #available(iOS 17.0, *) {
                            // في StandBy (الهاتف على الشاحن بالعرض) بلا خلفية: الاسم والعدّ بخط كبير يُقرأ من بعيد
                            StandByAware {
                                NextPrayerBlock(snap: snap, next: next, now: entry.date)
                            } bare: {
                                NextPrayerLarge(snap: snap, next: next)
                            }
                        } else {
                            NextPrayerBlock(snap: snap, next: next, now: entry.date)
                        }
                    }
                } else {
                    NeedsApp(snap: entry.snap)
                }
            }
        }
    }
}

/// يعرض [normal] في الشاشة الرئيسية، و [bare] حين تُرسم الأداة بلا خلفية (StandBy)
@available(iOS 17.0, *)
struct StandByAware<Normal: View, Bare: View>: View {
    @Environment(\.showsWidgetContainerBackground) private var showsBackground
    @Environment(\.galleryBare) private var galleryBare
    @ViewBuilder let normal: () -> Normal
    @ViewBuilder let bare: () -> Bare

    var body: some View {
        if galleryBare.map({ !$0 }) ?? showsBackground { normal() } else { bare() }
    }
}

/// الصلاة القادمة بخط كبير لـ StandBy: الاسم والوقت والعدّ التنازلي الذهبي (يلوّنه النظام بالأحمر ليلاً)
@available(iOS 17.0, *)
struct NextPrayerLarge: View {
    let snap: Snapshot
    let next: PrayerTime

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 5) {
                Image(systemName: "moon.stars.fill").font(.system(size: 13, weight: .semibold))
                Text(verbatim: next.t).font(Mizan.rounded(16, .semibold)).monospacedDigit()
            }
            .foregroundColor(Mizan.gold)
            Text(next.name)
                .font(Mizan.rounded(34, .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
            Text(next.date, style: .timer)
                .font(Mizan.rounded(38, .light))
                .monospacedDigit()
                .foregroundColor(Mizan.gold)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .multilineTextAlignment(snap.rtl ? .trailing : .leading)
                .environment(\.layoutDirection, .leftToRight)
                .frame(maxWidth: .infinity, alignment: .leading)
                .widgetAccentable()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

/// اسم الصلاة القادمة ووقتها، وعدّ تنازلي ذهبي حي، وشريط ما مضى من الوقت، والمدينة
struct NextPrayerBlock: View {
    let snap: Snapshot
    let next: PrayerTime
    let now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Header(symbol: "moon.stars.fill", title: snap.label("nextPrayer"))
            Spacer(minLength: 2)
            Text(next.name)
                .font(Mizan.rounded(26, .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(next.t)
                .font(Mizan.rounded(13, .medium))
                .foregroundColor(Mizan.text2)
            Text(next.date, style: .timer)
                .font(Mizan.rounded(26, .light))
                .monospacedDigit()
                .foregroundColor(Mizan.gold)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .multilineTextAlignment(snap.rtl ? .trailing : .leading)
                .environment(\.layoutDirection, .leftToRight)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 4)
            TimeBar(from: snap.previousPrayer(before: now)?.date ?? now.addingTimeInterval(-3600), to: next.date, now: now)
                .padding(.top, 6)
            // التاريخ الهجري لليوم، وتحته المدينة
            VStack(alignment: .leading, spacing: 1) {
                if let hijri = snap.day(at: now)?.hijri, !hijri.isEmpty {
                    Text(hijri)
                        .font(Mizan.rounded(11, .medium))
                        .foregroundColor(Mizan.text2)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                Text(snap.place)
                    .font(Mizan.rounded(10))
                    .foregroundColor(Mizan.text3)
                    .lineLimit(1)
            }
            .padding(.top, 5)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// الصلوات الخمس في صفوف، والقادمة بإطار ذهبي
struct PrayerList: View {
    let snap: Snapshot
    let next: PrayerTime
    let now: Date
    var compact: Bool = false

    var body: some View {
        // صلوات اليوم الذي فيه الصلاة القادمة (بعد العشاء: صلوات الغد)
        let day = snap.day(at: next.date) ?? snap.day(at: now)
        let rows = (day?.times ?? []).filter { $0.k != "sunrise" }
        VStack(spacing: compact ? 2 : 4) {
            ForEach(rows, id: \.k) { p in
                let on = p.at == next.at
                HStack {
                    Text(p.name)
                        .font(Mizan.rounded(12, on ? .semibold : .regular))
                        .foregroundColor(on ? Mizan.gold : Mizan.text2)
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    Text(p.s)
                        .font(Mizan.rounded(12, .medium))
                        .monospacedDigit()
                        .foregroundColor(on ? Mizan.gold : Mizan.text)
                }
                .padding(.horizontal, 9)
                .padding(.vertical, compact ? 3 : 4)
                .background(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .fill(on ? Mizan.gold.opacity(0.17) : Color.clear)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .stroke(on ? Mizan.gold.opacity(0.6) : Color.clear, lineWidth: 1)
                )
            }
        }
    }
}

@available(iOS 16.0, *)
struct NextPrayerAccessory: View {
    let entry: MizanEntry
    let family: WidgetFamily

    var body: some View {
        let next = entry.snap?.nextPrayer(after: entry.date)
        switch family {
        case .accessoryCircular:
            AccessoryShell(snap: entry.snap, url: Deep.url("prayer"), circular: true) {
                VStack(spacing: 0) {
                    Image(systemName: "moon.stars.fill").font(.system(size: 11))
                    Text(next?.name ?? "—").font(Mizan.rounded(11, .semibold)).lineLimit(1).minimumScaleFactor(0.6)
                    Text(next?.s ?? "").font(Mizan.rounded(13, .bold)).monospacedDigit().minimumScaleFactor(0.6)
                }
                .padding(2)
            }
        case .accessoryInline:
            AccessoryShell(snap: entry.snap, url: Deep.url("prayer")) {
                if let next = next {
                    Text("\(Image(systemName: "moon.stars.fill")) \(next.name) \(next.t)")
                } else {
                    Text(Fallback.text("nextPrayer"))
                }
            }
        default:
            AccessoryShell(snap: entry.snap, url: Deep.url("prayer")) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(entry.snap?.label("nextPrayer") ?? Fallback.text("nextPrayer"))
                        .font(Mizan.rounded(12, .semibold))
                        .widgetAccentable()
                    if let next = next {
                        Text(verbatim: "\(next.name) · \(next.t)").font(Mizan.rounded(15, .bold)).lineLimit(1).minimumScaleFactor(0.7)
                        Text(next.date, style: .timer).font(Mizan.rounded(14)).monospacedDigit().lineLimit(1)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// ---------- مواقيت اليوم ----------

struct PrayerTimesWidget: Widget {
    let kind = "MizanPrayerTimes"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .prayer)) { entry in
            PrayerTimesView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("prayerTimes"))
        .description(Fallback.gallery("descTimes"))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct PrayerTimesView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    var body: some View {
        Shell(snap: entry.snap, url: Deep.url("prayer")) {
            if let snap = entry.snap, let day = snap.day(at: entry.date), let next = snap.nextPrayer(after: entry.date) {
                if family == .systemSmall {
                    VStack(alignment: .leading, spacing: 4) {
                        Header(symbol: nil, title: snap.place)
                        Text(day.hijri)
                            .font(Mizan.rounded(10, .medium))
                            .foregroundColor(Mizan.text2)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        PrayerList(snap: snap, next: next, now: entry.date, compact: true)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 0) {
                        Header(symbol: nil, title: snap.place) {
                            Text(day.hijri).font(Mizan.rounded(11)).foregroundColor(Mizan.text2).lineLimit(1)
                        }
                        Spacer(minLength: 6)
                        PrayerCells(day: day, next: next)
                        Spacer(minLength: 6)
                        HStack {
                            Text(verbatim: "\(next.name) \(snap.label("in"))")
                                .font(Mizan.rounded(12))
                                .foregroundColor(Mizan.text2)
                                .lineLimit(1)
                            Spacer(minLength: 4)
                            Text(next.date, style: .timer)
                                .font(Mizan.rounded(14, .semibold))
                                .monospacedDigit()
                                .foregroundColor(Mizan.gold)
                                .multilineTextAlignment(snap.rtl ? .leading : .trailing)
                                .environment(\.layoutDirection, .leftToRight)
                                .frame(width: 90, alignment: .trailing)
                        }
                    }
                }
            } else {
                NeedsApp(snap: entry.snap)
            }
        }
    }
}

/// المواقيت الست في خانات متساوية، والقادمة ذهبية
struct PrayerCells: View {
    let day: Day
    let next: PrayerTime

    var body: some View {
        HStack(spacing: 5) {
            ForEach(day.times, id: \.k) { p in
                let on = p.at == next.at
                VStack(spacing: 3) {
                    Text(p.name)
                        .font(Mizan.rounded(11, on ? .semibold : .regular))
                        .foregroundColor(on ? Mizan.gold : Mizan.text2)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Text(p.s)
                        .font(Mizan.rounded(15, .semibold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(on ? Mizan.gold.opacity(0.18) : Color.white.opacity(0.07))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(on ? Mizan.gold.opacity(0.65) : Color.clear, lineWidth: 1)
                )
            }
        }
    }
}

// ---------- التاريخ الهجري ----------

struct HijriWidget: Widget {
    let kind = "MizanHijri"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .day)) { entry in
            HijriView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("hijri"))
        .description(Fallback.gallery("descHijri"))
        .supportedFamilies(Families.make([.systemSmall], circular: true, rectangular: true, inline: true))
    }
}

struct HijriView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    var body: some View {
        let day = entry.snap?.day(at: entry.date)
        if family.isAccessory {
            if #available(iOS 16.0, *) {
                HijriAccessory(snap: entry.snap, day: day, family: family)
            }
        } else {
            Shell(snap: entry.snap, url: Deep.url("hijri")) {
                if let day = day {
                    VStack(spacing: 0) {
                        Header(symbol: "moon.fill", title: day.weekday)
                        Spacer(minLength: 2)
                        Text(day.hijriDay)
                            .font(Mizan.rounded(50, .light))
                            .foregroundColor(Mizan.gold)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Text(day.hijriMonth)
                            .font(Mizan.rounded(15, .bold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Capsule().fill(Mizan.gold.opacity(0.5)).frame(width: 36, height: 1.5).padding(.vertical, 7)
                        Text(day.greg)
                            .font(Mizan.rounded(11))
                            .foregroundColor(Mizan.text2)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                        Spacer(minLength: 2)
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    NeedsApp(snap: entry.snap)
                }
            }
        }
    }
}

@available(iOS 16.0, *)
struct HijriAccessory: View {
    let snap: Snapshot?
    let day: Day?
    let family: WidgetFamily

    var body: some View {
        switch family {
        case .accessoryCircular:
            AccessoryShell(snap: snap, url: Deep.url("hijri"), circular: true) {
                VStack(spacing: 0) {
                    Image(systemName: "moon.fill").font(.system(size: 10))
                    Text(day?.hijriDay ?? "—").font(Mizan.rounded(22, .bold)).minimumScaleFactor(0.6)
                }
            }
        case .accessoryInline:
            AccessoryShell(snap: snap, url: Deep.url("hijri")) {
                Text("\(Image(systemName: "moon.fill")) \(day?.hijri ?? Fallback.text("hijri"))")
            }
        default:
            AccessoryShell(snap: snap, url: Deep.url("hijri")) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(day?.weekday ?? Fallback.text("hijri")).font(Mizan.rounded(12, .semibold)).widgetAccentable()
                    Text(day?.hijri ?? "—").font(Mizan.rounded(15, .bold)).lineLimit(1).minimumScaleFactor(0.7)
                    Text(day?.greg ?? "").font(Mizan.rounded(12)).lineLimit(1).minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// ---------- آية اليوم ----------

struct AyahWidget: Widget {
    let kind = "MizanAyah"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MizanProvider(kind: .day)) { entry in
            AyahView(entry: entry)
        }
        .configurationDisplayName(Fallback.gallery("ayah"))
        .description(Fallback.gallery("descAyah"))
        // الكبيرة جداً على iPad (وما يعرضها من الشاشات الواسعة) بعرض ضعف الكبيرة
        .supportedFamilies([.systemMedium, .systemLarge, .systemExtraLarge])
    }
}

struct AyahView: View {
    let entry: MizanEntry
    @WidgetFamilyValue var family

    var body: some View {
        let ayah = entry.snap?.day(at: entry.date)?.ayah
        let large = family == .systemLarge || family == .systemExtraLarge
        Shell(snap: entry.snap, url: Deep.url(ayah?.route ?? "quran")) {
            if let snap = entry.snap, let ayah = ayah {
                VStack(spacing: 0) {
                    Header(symbol: "book.fill", title: snap.label("ayah")) {
                        Chip(text: ayah.ref)
                    }
                    Spacer(minLength: 6)
                    Text(verbatim: "﴿\(ayah.text)﴾")
                        .font(Mizan.quran(family == .systemExtraLarge ? 34 : (large ? 30 : 22)))
                        .multilineTextAlignment(.center)
                        .lineSpacing(large ? 8 : 4)
                        .lineLimit(large ? 9 : 4)
                        .minimumScaleFactor(0.4)
                        .environment(\.layoutDirection, .rightToLeft)
                    if let meaning = ayah.meaning, !meaning.isEmpty {
                        Text(meaning)
                            .font(Mizan.rounded(large ? 13 : 11))
                            .foregroundColor(Mizan.text2)
                            .multilineTextAlignment(.center)
                            .lineLimit(large ? 6 : 2)
                            .minimumScaleFactor(0.8)
                            .padding(.top, 6)
                    }
                    Spacer(minLength: 4)
                }
                .frame(maxWidth: .infinity)
            } else {
                NeedsApp(snap: entry.snap)
            }
        }
    }
}
