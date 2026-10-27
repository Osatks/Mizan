package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** اسم ووصف قصير (أداة، مناسبة، ...) */
data class Named(val name: String, val desc: String)

/**
 * كل نصوص التطبيق. لكل لغة كائن يطبّق هذه الواجهة، فإذا نقص نص في أي لغة فشل البناء.
 *
 * Every user-visible text. Each language is one object implementing this interface, so a missing
 * text is a compile error. Maps are checked by StringsTest (every key of the Arabic map must exist).
 * Numbers passed in are already formatted; functions that take Int/Long must handle plurals.
 */
interface Strings {
    // ---------- General ----------
    val appName: String                 // ميزان / Mizan
    val appTagline: String              // أدوات المسلم / Muslim tools
    val tabHome: String
    val tabPrayer: String
    val tabQibla: String
    val tabTasbih: String
    val tabSettings: String
    val back: String
    val share: String                   // chooser title
    val shareSignature: String          // appended to shared text: "— تطبيق ميزان"
    val today: String                   // "today" (as a button / relative day)
    val tomorrow: String
    /** relative future day for n >= 2: "in 5 days" */
    fun inDays(n: Long): String
    /** a plain count of days: "30 days" */
    fun daysCount(n: Long): String
    val decrease: String
    val increase: String
    val noLimit: String                 // unlimited target
    val noResults: String
    val unitGram: String                // g
    val unitKg: String                  // kg
    val unitKm: String                  // km
    val am: String
    val pm: String

    // ---------- Calendar ----------
    /** 12 Hijri month names (Muharram … Dhu al-Hijjah), standalone form for pickers */
    val hijriMonths: List<String>
    /** 12 Gregorian month names, standalone form for pickers */
    val gregMonths: List<String>
    /** 7 weekday names, Monday first */
    val weekdays: List<String>
    /** 7 very short weekday names for a calendar header, Monday first */
    val weekdaysShort: List<String>
    /** full Hijri date, month 1..12: "3 Rabi' al-Thani 1448 AH" */
    fun hijriDate(d: Int, m: Int, y: Int): String
    /** full Gregorian date, month 1..12: "15 October 2026" */
    fun gregDate(d: Int, m: Int, y: Int): String
    /** Hijri month title: "Rabi' al-Thani 1448 AH" */
    fun hijriMonthTitle(m: Int, y: Int): String
    val hijriYearSuffix: String         // AH
    val gregYearSuffix: String          // CE
    val dayLabel: String                // field label "Day"
    val monthLabel: String
    val yearLabel: String

    // ---------- Home ----------
    /** keys: zakat inheritance prayer qibla hijri events tasbih fitr age fidya */
    val tools: Map<String, Named>
    /** extra search words per tool (same keys as [tools]), space separated */
    val toolKeywords: Map<String, String>
    val toolsTitle: String
    fun nextPrayerIn(city: String): String  // "Next prayer · Riyadh"
    val searchHint: String

    // ---------- Search ----------
    val searchTitle: String
    val searchToolsGroup: String
    val searchCitiesGroup: String
    val searchPrayersGroup: String
    val searchEventsGroup: String
    val searchDhikrGroup: String
    val searchSettingsGroup: String
    val searchEmpty: String             // hint under empty field: what you can search for
    val setAsMyCity: String             // subtitle on a city result
    fun searchNoResults(q: String): String

    // ---------- Prayer ----------
    /** keys: fajr sunrise dhuhr asr maghrib isha */
    val prayers: Map<String, String>
    val prayerTitle: String
    fun nextPrayerLabel(name: String): String   // "Next prayer: Asr"
    fun atTime(time: String): String            // "at 3:15 PM"
    val prevDay: String
    val nextDay: String
    val monthTable: String
    val hideMonthTable: String
    fun monthTableTitle(m: Int, y: Int): String // "October 2026 timetable"
    val colDay: String                          // column header "Day"
    val locationAndMethod: String
    val useMyLocation: String
    val locating: String
    val orPickCity: String
    val pickCity: String
    val searchCity: String
    val myLocation: String                      // a GPS place not near a known city
    val calcMethod: String
    /** keys: makkah mwl egypt dubai kuwait qatar jordan morocco algeria tunisia karachi turkey tehran indonesia malaysia singapore russia isna france */
    val methods: Map<String, String>
    val asrMethod: String
    val asrStandard: String                     // majority (Shafi'i, Maliki, Hanbali)
    val asrHanafi: String
    val latLng: String
    val adhanAlert: String
    val adhanAlertDesc: String
    val timesMayDiffer: String
    fun endsAt(t: String): String               // under a prayer time: "Ends 6:21 AM" (t is already formatted)
    fun iqamaAt(t: String): String              // under a prayer time: "Iqama 5:29 AM"
    val iqamaTitle: String                      // section title
    val iqamaDesc: String                       // minutes after the adhan per prayer; 0 hides it
    val prayerEndsNote: String                  // how the end of each prayer's time is determined
    val locationFound: String
    val locationFailed: String
    val locationDenied: String
    /** city names by key (see PrayTimes.CITIES) */
    val cities: Map<String, String>
    val palestine: String                       // country name used for PS

    // ---------- Notifications ----------
    val channelName: String
    val channelDesc: String
    fun prayerNowTitle(prayer: String): String  // "It's time for Asr"
    fun prayerNowText(city: String): String     // "Riyadh — come to prayer"
    // ---------- Adhan & alerts ----------
    val adhanSettings: String                   // screen title and settings row: "Adhan & alerts"
    val adhanSettingsSub: String                // row subtitle: muezzin, alert for each prayer, vibration
    val alertPerPrayer: String                  // section title: "Alert for each prayer"
    val alertModeAdhan: String                  // alert mode: plays the adhan
    val alertModeVibrate: String                // alert mode: "Vibrate only"
    val alertModeSilent: String                 // alert mode: "Silent notification"
    val alertModeOff: String                    // alert mode: "No alert"
    val alertModesHint: String                  // explains the four modes
    val muezzinTitle: String                    // section title: "Muezzin"
    val fajrAdhanTitle: String                  // section title: "Fajr adhan"
    val fajrAdhanHint: String                   // the Fajr adhan adds "prayer is better than sleep"
    val fajrSameAsOthers: String                // Fajr option: "Same as the other prayers"
    /** recording names by id (see core/AdhanCatalog.kt) */
    val muezzinNames: Map<String, String>
    val playAdhan: String                       // content description of the play button: "Play adhan"
    val stopAdhan: String                       // stop button and notification action: "Stop"
    val vibrationTitle: String                  // section title: "Vibration pattern"
    /** keys: short long heartbeat pulse wave */
    val vibeNames: Map<String, String>
    val tryVibration: String                    // button: "Try vibration"
    val adhanStopHint: String                   // Android: how to stop the adhan; silent/vibrate phone mode is followed
    val adhanIosNote: String                    // iOS: 30-second notification sounds, full adhan when the app is open, system vibration
    val recordingSources: String                // section title: "Recording sources"

    // ---------- Qibla ----------
    val qiblaTitle: String
    val noCompass: String
    val facingQibla: String
    fun turnRight(deg: Int): String
    fun turnLeft(deg: Int): String
    val qiblaAngle: String
    fun towards(dir: String): String
    /** 8 compass directions starting at North, clockwise */
    val directions: List<String>
    val location: String
    val distanceToKaaba: String
    val phoneHeading: String
    val compassLowAccuracy: String
    val qiblaTip: String

    // ---------- Tasbih ----------
    val tasbihTitle: String
    val tasbihSubtitle: String
    val modeFree: String
    val modeAfterPrayer: String
    fun ofGoal(n: Int): String                  // "of 33"
    val nextDhikr: String
    val completedHundred: String
    fun completedN(n: Int): String
    val reset: String
    val undo: String
    val chooseDhikr: String
    val targetCount: String
    val yourStats: String
    val roundsDone: String
    val todayCount: String
    val totalCount: String
    /** 9 short labels for the adhkar (same order as the Arabic texts): Subhan Allah … salawat */
    val dhikrLabels: List<String>
    /** 9 meanings shown under the Arabic text; Arabic leaves them empty */
    val dhikrMeanings: List<String>
    /** meaning of "La ilaha illa Allah wahdahu la sharika lah ..."; empty in Arabic */
    val tamamMeaning: String
    val tapToCount: String

    // ---------- Zakat ----------
    val zakatTitle: String
    val zakatSubtitle: String
    val partCash: String
    val partReceivables: String
    val partGold: String
    val partSilver: String
    val partStocks: String
    val partTrade: String
    val enterGoldPrice: String
    val enterSilverPrice: String
    val nisabReached: String
    val belowNisab: String
    val enterAssets: String
    val zakatDue: String
    val totalAssets: String
    val deductedDebts: String
    val netBase: String
    val nisabValue: String
    val approxEquals: String
    fun goldGrams24(g: String): String
    val shareResult: String
    fun zakatShareText(zakat: String, base: String): String
    val settingsAndPrices: String
    val currency: String
    val nisabBasis: String
    val nisabGold: String
    val nisabSilver: String
    val goldPrice24: String
    val goldPriceHint: String
    val silverPrice: String
    val cashSection: String
    val cashInHand: String
    val bankBalances: String
    val receivablesField: String
    val otherMoney: String
    val goldSilverSection: String
    fun goldKarat(k: Int): String
    val pureSilver: String
    val includeJewelry: String
    val jewelryField: String
    val jewelryHint: String
    val investSection: String
    val stocksTrade: String
    val stocksLong: String
    val tradeGoods: String
    val deductSection: String
    val debtsField: String
    val debtsHint: String
    val breakdownTitle: String
    val zakatNote: String

    // ---------- Zakat al-Fitr ----------
    val fitrTitle: String
    val fitrSubtitle: String
    val foodAmountDue: String
    val totalCashValue: String
    fun forPeople(n: Int): String               // "for 3 people"
    val peopleCount: String
    val modeFood: String
    val modeMoney: String
    val foodType: String
    /** keys: rice riceSafe wheat dates barley other */
    val foods: Map<String, String>
    fun approxKg(kg: String): String            // "about 2.5 kg"
    val saWeight: String
    val kgPrice: String
    val kgPriceHint: String
    val saPerPerson: String
    val approxCost: String
    val valuePerPerson: String
    val valuePerPersonHint: String
    val valueNote: String
    val fitrTimingNote: String

    // ---------- Fidya & Kaffarah ----------
    val fidyaTitle: String
    val fidyaSubtitle: String
    val tabFidya: String
    val tabOath: String
    val tabIntercourse: String
    val fidyaResult: String
    fun fidyaSub(days: Int): String
    val fidyaNote: String
    val oathResult: String
    fun oathSub(oaths: Int): String
    val oathAlternative: String
    fun fastDays(n: Int): String
    val oathNote: String
    val intercourseResult: String
    val intercourseSub: String
    val ifStartToday: String
    val twoMonthsDuration: String
    val intercourseNote: String
    val daysField: String
    val oathsField: String
    val feedMeasure: String
    val muddOption: String
    val halfSaOption: String
    val poorCount: String
    val approxValue: String

    // ---------- Hijri converter ----------
    val hijriTitle: String
    val hijriSubtitle: String
    val correspondingHijri: String
    val correspondingGreg: String
    val enterGregYear: String
    val enterHijriYear: String
    val gregToHijri: String
    val hijriToGreg: String
    val todayDate: String
    val prevMonth: String
    val nextMonth: String
    val calendarHint: String
    val moonNote: String

    // ---------- Age ----------
    val ageTitle: String
    val ageSubtitle: String
    val ageGregorian: String
    fun ymd(y: Int, m: Int, d: Int): String     // "30 years 2 months 5 days"
    fun inHijri(age: String): String            // "Hijri: …"
    val birthGreg: String
    val birthHijri: String
    val birthWeekday: String
    val nextBirthday: String
    val birthdayToday: String
    val tileDays: String                        // label under a big number: "days"
    val tileWeeks: String
    val tileHours: String
    val yourAge: String
    val birthAfterToday: String
    val enterBirth: String
    val birthDate: String
    val gregorian: String
    val hijri: String

    // ---------- Islamic events ----------
    val eventsTitle: String
    val eventsSubtitle: String
    /** keys: ramadan lastTen eidFitr shawwal dhulHijjah arafah eidAdha newYear ashura */
    val events: Map<String, Named>
    val unitDays: String                        // countdown tile labels
    val unitHours: String
    val unitMinutes: String
    val unitSeconds: String
    val eventsNote: String

    // ---------- Inheritance ----------
    val inhTitle: String
    val inhSubtitle: String
    val aslLabel: String
    val awlCase: String
    val raddCase: String
    val adilCase: String
    fun tashihFrom(n: Long): String
    val sharesTitle: String
    fun blockedBy(by: String): String
    fun perPerson(v: String): String
    val shareDivision: String
    fun divisionShareText(list: String): String
    val listSeparator: String                   // "، " / ", "
    val addHeirs: String
    val deceasedAndEstate: String
    val deceasedMale: String
    val deceasedFemale: String
    val estateTotal: String
    val funeralCosts: String
    val deceasedDebts: String
    val bequest: String
    val bequestHint: String
    val netEstate: String
    val bequestCapped: String
    val newCase: String
    val inhDisclaimer: String
    /** heir names by Inheritance.HEIRS key */
    val heirs: Map<String, String>
    /** keys: spouses ascendants descendants siblings distant */
    val heirGroups: Map<String, String>
    /** names for Inheritance.BLOCKERS keys (other blockers are heir keys) */
    val blockers: Map<String, String>
    fun shareType(t: ShareType, radd: Boolean): String
    fun inhNote(n: InhNote): String
    /** display name of a blocker / winning heir key */
    fun nameOf(key: String): String = blockers[key] ?: heirs[key] ?: key

    // ---------- Quran ----------
    val tabQuran: String                // short tab label: "Quran"
    val quranTitle: String              // "The Holy Quran"
    val quranSubtitle: String           // "Tap any ayah to read its tafsir"
    val surahsTab: String               // "Surahs"
    val juzTab: String                  // "Juz"
    val bookmarksTab: String            // "Saved"
    val continueReading: String         // "Continue reading"
    val startReading: String            // "Start reading"
    val meccan: String                  // "Meccan"
    val medinan: String                 // "Medinan"
    /** number of ayahs, with plural: "7 ayahs", "1 ayah" */
    fun ayahCount(n: Int): String
    /** "Juz 30" */
    fun juzName(n: Int): String
    /** page of the Madinah Mushaf: "Page 604" */
    fun pageName(n: Int): String
    /** one ayah: "Al-Baqarah · Ayah 255" */
    fun surahAyah(surah: String, ayah: Int): String
    /** the 114 surah names in this language, in order (Al-Fatihah … An-Nas) */
    val surahNames: List<String>
    val searchQuranHint: String         // "Search a surah, or words in the ayahs…"
    val searchQuranGroup: String        // search result group: "Surahs"
    val searchAyahsGroup: String        // search result group: "Ayahs"
    val mushafView: String              // "Mushaf view"
    val ayahView: String                // "Ayah by ayah"
    val textLarger: String              // "Larger text"
    val textSmaller: String             // "Smaller text"
    val tafsirTitle: String             // "Tafsir"
    val tafsirMuyassar: String          // name of the Arabic tafsir: "Tafsir al-Muyassar"
    val tafsirJalalayn: String          // name of the Arabic tafsir: "Tafsir al-Jalalayn"
    val translationOfMeanings: String   // "Translation of the meanings"
    val bookmarkAyah: String            // "Save ayah"
    val removeBookmark: String          // "Remove from saved"
    val copy: String                    // "Copy"
    val copied: String                  // "Copied ✓"
    val prevAyah: String                // "Previous ayah"
    val nextAyah: String                // "Next ayah"
    val prevSurah: String               // "Previous surah"
    val nextSurah: String               // "Next surah"
    val noBookmarks: String             // "No saved ayahs yet. Open an ayah and tap the bookmark to save it."
    val quranLoading: String            // "Opening the Mushaf…"
    /** "Hizb 9" */
    fun hizbName(n: Int): String
    val khatmaTitle: String             // "My khatma"
    val khatmaPlanLabel: String         // "Finish in"
    fun planDays(days: Int): String     // chip: "30 days"; 0 = no deadline
    fun pagesRead(read: Int, total: Int): String   // "120 / 604 pages"
    val todayPortion: String            // "Today's portion"
    fun streakDays(n: Int): String      // "Streak: 5 days"
    fun daysLeftLabel(n: Int): String   // "Days left: 12"
    val portionDone: String             // "Today's portion done ✓"
    val khatmaDone: String              // "Khatma complete ✓ May Allah accept it"
    val newKhatma: String               // "Start a new khatma"
    fun khatmasCompleted(n: Int): String // "Completed khatmas: 2"
    val continuePortion: String         // "Continue my portion"
    val ayahOfDay: String               // "Ayah of the day" (home-screen widget)
    val focusMode: String               // "Focus mode" (Quran reading without the header)
    val exitFocus: String               // "Exit focus mode"
    val silenceTitle: String            // "Silence notifications while reading?"
    val silenceBody: String             // Do Not Disturb explanation, Android asks once
    val allow: String                   // "Allow"
    val notNow: String                  // "Not now"
    val tafsirLibrary: String           // "Tafsir library"
    val tafsirLibrarySub: String        // "Download tafsirs and choose which ones appear when you tap an ayah"
    val onDevice: String                // "On this device"
    val availableDownloads: String      // "Available to download"
    val download: String                // "Download"
    fun downloadingPct(pct: Int): String // "Downloading… 42%"
    val cancel: String                  // "Cancel"
    val delete: String                  // "Delete"
    fun deleteTafsirQ(name: String): String // "Delete “X” from this device?"
    val showInTafsir: String            // "Show in tafsir" (switch)
    val builtIn: String                 // "Built in" (bundled with the app)
    fun sizeMb(mb: String): String      // "≈ 9.8 MB"
    val downloadFailed: String          // "Download failed. Check your internet connection."
    val retry: String                   // "Try again"
    val moreTafsirs: String             // "More tafsirs" (chip in the tafsir sheet)
    val tafsirLibraryNote: String       // source credit + internet only for downloads
    fun ayahRange(from: Int, to: Int): String // "Commentary on ayahs 8–10"
    val recitation: String              // "Recitation" (audio sheet title)
    val reciter: String                 // "Reciter"
    val playRecitation: String          // "Play recitation"
    val pauseRecitation: String         // "Pause"
    val stopRecitation: String          // "Stop recitation"
    val repeatAyah: String              // "Repeat each ayah"
    val listenFromHere: String          // "Listen from this ayah" (tafsir sheet)
    val listen: String                  // "Listen" (ayah actions bar)
    val save: String                    // "Save" (bookmark an ayah)
    val saved: String                   // "Saved"
    val ayahActionsHint: String         // first-time hint for the long-press ayah bar
    val audioNote: String               // streaming source + offline cache
    val audioError: String              // "Couldn't play the recitation. Check your internet connection."
    val hifzMode: String                // "Memorization" (hide ayahs)
    val revealNext: String              // "Reveal next" (ayah)
    val hideAll: String                 // "Hide all"
    val hifzHint: String                // "Recite from memory, then tap a hidden ayah to check it."
    val displayTitle: String            // "Display" (page color, brightness, text size)
    val pageColor: String               // "Page color"
    val toneAuto: String                // "Auto" (follows app theme)
    val toneSepia: String               // "Sepia"
    val toneNight: String               // "Night"
    val brightness: String              // "Brightness"
    val brightnessSystem: String        // "Device brightness" (switch)
    val textSizeLabel: String           // "Text size"
    val continueFocus: String           // "Continue in focus mode"
    val smartPresets: String            // "Smart presets"
    val presetListen: String            // "Listen"
    val presetMemorize: String          // "Memorize"
    val presetReview: String            // "Review"
    val smartHint: String               // explains the Memorize and Review presets
    val range: String                   // "Range" (which ayahs to recite)
    val rangeContinue: String           // "Keep going"
    val rangePage: String               // "This page"
    val rangeSurah: String              // "This surah"
    val rangeCustom: String             // "Custom"
    val fromAyah: String                // "From"
    val toAyah: String                  // "To"
    val repeatRange: String             // "Repeat the range"
    val pauseAfter: String              // "Pause after each ayah" (time to repeat it)
    val off: String                     // "Off"
    val pauseAyahLength: String         // "Ayah length" (pause as long as the ayah)
    fun seconds(n: Int): String         // "2 s"
    val speed: String                   // "Speed"
    val sleepTimer: String              // "Sleep timer"
    fun minutes(n: Int): String         // "15 min"
    val evidenceTitle: String           // section title
    val evidenceSub: String             // subtitle
    val proofQuran: String              // kind label
    val proofHadith: String             // kind label
    val proofConsensus: String          // kind label
    val proofScholars: String           // kind label
    val zakatTypesTitle: String         // hub title
    val zakatTypesSub: String           // hub subtitle
    val zkMoneyName: String             // type
    val zkMoneyDesc: String             // type desc
    val zkTradeName: String             // type
    val zkTradeDesc: String             // type desc
    val zkLivestockName: String         // type
    val zkLivestockDesc: String         // type desc
    val zkCropsName: String             // type
    val zkCropsDesc: String             // type desc
    val zkMineralsName: String          // type
    val zkMineralsDesc: String          // type desc
    val zkCombinedNote: String          // note
    val camels: String                  // label
    val cattle: String                  // label
    val sheepGoats: String              // label
    val unitHead: String                // unit for animals
    val livestockDue: String            // result title
    val nothingDue: String              // result
    fun livestockNisab(n: Int): String  // "Nisab: 40"
    val grazingCondition: String        // checkbox
    val yearCondition: String           // checkbox
    val conditionsNotMet: String        // note
    fun livestockKind(kind: String): String  // animal due by key: shah bintMakhad bintLabun hiqqah jadhaah tabi musinnah
    val cropsWeight: String             // field
    val irrigation: String              // label
    val irrRain: String                 // option
    val irrIrrigated: String            // option
    val irrMixed: String                // option
    val cropsDue: String                // result title
    fun cropsNisab(kg: String): String  // "Nisab: about 612 kg"
    val zkRate: String                  // label
    val rikazValue: String              // field
    val mineralValue: String            // field
    val mineralsDue: String             // result title
    val obSkip: String                  // onboarding
    val obNext: String                  // onboarding
    val obStart: String                 // onboarding
    val obLocationTitle: String         // onboarding: last page asks for the location ("Where are you?")
    val obLocationBody: String          // why the location is needed; it stays on the device
    val locationNeeded: String          // banner on Home/Prayer while no location has been chosen
    val appGuide: String                // settings row
    val appGuideSub: String             // settings row subtitle
    val onboarding: List<Pair<String, String>>  // 5 pages (title, body): welcome, prayer&qibla, Quran, calculators with evidence, search
    /** About: where the Quran text, tafsir, translations and font come from. Keep the two URLs exactly. */
    val quranSources: String

    // ---------- Settings ----------
    val settingsTitle: String
    val appearance: String
    val themeAuto: String
    val themeLight: String
    val themeDark: String
    val appColor: String
    /** أسماء ألوان التطبيق بترتيب Accent */
    val accentNames: List<String>
    val language: String
    val deviceLanguage: String
    val use24h: String
    val use24hDesc: String
    val exactAlarm: String
    val exactAlarmDesc: String
    val about: String
    val aboutName: String
    // ---------- Adhkar ----------
    val adhkarNow: String              // badge on the section suggested for this time of day: "Now"
    val dhikrEvidence: String          // button + sheet title: "Evidence"
    val dhikrSource: String            // "Source" (book and hadith number)
    val dhikrGrade: String             // "Grade" of the hadith
    val dhikrNarrator: String          // "Narrator"
    val dhikrVirtue: String            // "Virtue"
    val dhikrQuranRef: String          // "From the Holy Quran"
    val dhikrDone: String              // shown when a section is completed
    val dhikrTapHint: String           // tap to count, tap Evidence for the source
    fun dhikrTimes(n: Int): String     // "3 times" (repeat count)
    fun dhikrItems(n: Int): String     // "24 adhkar" (section size)
    val moreLanguages: String          // section in the language sheet: "Download more languages"
    val langPackHint: String           // languages come from the Mizan website, then work offline with their Quran translation
    val update: String                 // button: "Update"
    fun version(v: String): String
    val aboutText: String
    val privacy: String
    val privacyText: String
    val disclaimer: String
}
