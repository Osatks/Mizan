package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** বাংলায় সংখ্যার পরে বিশেষ্য বহুবচনে বদলায় না: "1 দিন", "5 দিন" */
private fun bn(n: Long, unit: String) = "$n $unit"

object Bn : Strings {
    // ---------- General ----------
    override val appName = "মিজান"
    override val appTagline = "মুসলিমের টুলস"
    override val tabHome = "হোম"
    override val tabPrayer = "নামাজ"
    override val tabQibla = "কিবলা"
    override val tabTasbih = "তাসবিহ"
    override val tabSettings = "সেটিংস"
    override val back = "ফিরে যান"
    override val share = "শেয়ার করুন"
    override val shareSignature = "— মিজান অ্যাপ"
    override val today = "আজ"
    override val tomorrow = "আগামীকাল"
    override fun inDays(n: Long) = bn(n, "দিন") + " পর"
    override fun daysCount(n: Long) = bn(n, "দিন")
    override val decrease = "কমান"
    override val increase = "বাড়ান"
    override val noLimit = "সীমাহীন"
    override val noResults = "কোনো ফলাফল নেই"
    override val unitGram = "গ্রাম"
    override val unitKg = "কেজি"
    override val unitKm = "কিমি"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি",
        "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )
    override val gregMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    override val weekdays = listOf("সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার", "রবিবার")
    override val weekdaysShort = listOf("সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি", "রবি")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y হিজরি"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y হিজরি"
    override val hijriYearSuffix = "হিজরি"
    override val gregYearSuffix = "খ্রি."
    override val dayLabel = "দিন"
    override val monthLabel = "মাস"
    override val yearLabel = "বছর"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("যাকাত ক্যালকুলেটর", "অর্থ, স্বর্ণ ও শেয়ার, নিসাবসহ"),
        "inheritance" to Named("উত্তরাধিকার ক্যালকুলেটর", "ফারায়েজের নিয়মে সম্পত্তি বণ্টন"),
        "prayer" to Named("নামাজের সময়সূচি", "আজানের অ্যালার্ট ও লাইভ কাউন্টডাউন"),
        "qibla" to Named("কিবলার দিক", "ফোনের সেন্সরচালিত কম্পাস"),
        "hijri" to Named("হিজরি তারিখ", "তারিখ রূপান্তর, উম্মুল কুরা ক্যালেন্ডার"),
        "events" to Named("ইসলামি দিবস", "রমজান, দুই ঈদ ও আরাফা"),
        "tasbih" to Named("তাসবিহ", "জিকির গণনা, প্রস্তুত আজকারসহ"),
        "adhkar" to Named("যিকির ও দোয়া", "হিসনুল মুসলিম ও দৈনন্দিন যিকির, দলিলসহ"),
        "fitr" to Named("সদকাতুল ফিতর", "সা' অথবা মূল্য হিসেবে"),
        "age" to Named("বয়স ক্যালকুলেটর", "হিজরি ও খ্রিস্টীয় বছরে"),
        "fidya" to Named("ফিদিয়া ও কাফফারা", "ছুটে যাওয়া রোজা ও ভাঙা কসম"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "যাকাত জাকাত যাকাহ সদকা দান সম্পদ স্বর্ণ সোনা রুপা রৌপ্য নিসাব নেসাব টাকা সঞ্চয় শেয়ার zakat jakat",
        "inheritance" to "উত্তরাধিকার মিরাস মীরাস ফারায়েজ ফরায়েজ সম্পত্তি বণ্টন ওয়ারিশ ওয়ারিস অসিয়ত ওসিয়ত faraiz",
        "prayer" to "নামাজ নামায সালাত আজান আযান ওয়াক্ত ফজর যোহর জোহর আসর মাগরিব এশা ইশা সময় namaz",
        "qibla" to "কিবলা কেবলা কাবা কা'বা মক্কা কম্পাস দিক qibla kibla",
        "hijri" to "হিজরি ক্যালেন্ডার পঞ্জিকা তারিখ রূপান্তর আরবি চাঁদ উম্মুল কুরা",
        "events" to "রমজান রমাদান ঈদ আরাফা আরাফাহ আশুরা শবে কদর লাইলাতুল দিবস ছুটি",
        "tasbih" to "তাসবিহ তাসবীহ তসবি জিকির যিকির জিকর আজকার দোয়া কাউন্টার গণনা",
        "adhkar" to "যিকির জিকির দোয়া হিসনুল মুসলিম সকাল সন্ধ্যা ঘুম রমজান আযকার",
        "fitr" to "ফিতরা ফেতরা ফিতর সদকাতুল সদকা যাকাতুল সা ঈদ",
        "age" to "বয়স জন্মদিন জন্ম জন্মতারিখ বছর",
        "fidya" to "ফিদিয়া ফিদইয়া ফিদয়া কাফফারা কাফ্ফারা কসম শপথ রোজা সিয়াম খাওয়ানো",
    )
    override val toolsTitle = "টুলস"
    override fun nextPrayerIn(city: String) = "পরবর্তী নামাজ · $city"
    override val searchHint = "টুল, শহর, জিকির খুঁজুন…"

    // ---------- Search ----------
    override val searchTitle = "অনুসন্ধান"
    override val searchToolsGroup = "টুলস"
    override val searchCitiesGroup = "শহর"
    override val searchPrayersGroup = "আজকের নামাজের সময়"
    override val searchEventsGroup = "দিবস"
    override val searchDhikrGroup = "জিকির"
    override val searchSettingsGroup = "সেটিংস"
    override val searchEmpty = "টুল, শহর, জিকির, দিবস ও সেটিংস খুঁজুন"
    override val setAsMyCity = "নামাজের সময়ের জন্য ব্যবহার করুন"
    override fun searchNoResults(q: String) = "“$q”-এর জন্য কোনো ফলাফল নেই"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "ফজর", "sunrise" to "সূর্যোদয়", "dhuhr" to "যোহর",
        "asr" to "আসর", "maghrib" to "মাগরিব", "isha" to "এশা"
    )
    override val prayerTitle = "নামাজের সময়সূচি"
    override fun nextPrayerLabel(name: String) = "পরবর্তী নামাজ: $name"
    override fun atTime(time: String) = "$time-এ"
    override val prevDay = "আগের দিন"
    override val nextDay = "পরের দিন"
    override val monthTable = "মাসিক সময়সূচি"
    override val hideMonthTable = "সময়সূচি লুকান"
    override fun monthTableTitle(m: Int, y: Int) = "${gregMonths[m - 1]} $y-এর সময়সূচি"
    override val colDay = "দিন"
    override val locationAndMethod = "অবস্থান ও হিসাব পদ্ধতি"
    override val useMyLocation = "আমার বর্তমান অবস্থান ব্যবহার করুন"
    override val locating = "আপনার অবস্থান খোঁজা হচ্ছে…"
    override val orPickCity = "অথবা একটি শহর বেছে নিন"
    override val pickCity = "শহর বেছে নিন"
    override val searchCity = "শহর খুঁজুন"
    override val myLocation = "আমার অবস্থান"
    override val calcMethod = "হিসাব পদ্ধতি"
    override val methods = mapOf(
        "makkah" to "উম্মুল কুরা (সৌদি আরব)",
        "mwl" to "মুসলিম ওয়ার্ল্ড লীগ",
        "egypt" to "মিশরের সাধারণ জরিপ কর্তৃপক্ষ",
        "dubai" to "আরব আমিরাত (দুবাই)",
        "kuwait" to "কুয়েত",
        "qatar" to "কাতার",
        "jordan" to "জর্ডান ও ফিলিস্তিন",
        "morocco" to "মরক্কো",
        "algeria" to "আলজেরিয়া",
        "tunisia" to "তিউনিসিয়া",
        "karachi" to "ইসলামি বিজ্ঞান বিশ্ববিদ্যালয়, করাচি",
        "turkey" to "দিয়ানেত (তুরস্ক)",
        "tehran" to "ভূপদার্থবিদ্যা ইনস্টিটিউট, তেহরান বিশ্ববিদ্যালয়",
        "indonesia" to "ধর্মবিষয়ক মন্ত্রণালয় (ইন্দোনেশিয়া)",
        "malaysia" to "JAKIM (মালয়েশিয়া)",
        "singapore" to "MUIS (সিঙ্গাপুর)",
        "russia" to "রাশিয়ার মুসলিমদের ধর্মীয় প্রশাসন",
        "isna" to "উত্তর আমেরিকা (ISNA)",
        "france" to "ফ্রান্সের ইসলামি সংগঠনসমূহের ইউনিয়ন",
    )
    override val asrMethod = "আসরের হিসাব"
    override val asrStandard = "জমহুর (শাফেয়ি, মালেকি, হাম্বলি)"
    override val asrHanafi = "হানাফি"
    override val latLng = "অক্ষাংশ / দ্রাঘিমাংশ"
    override val adhanAlert = "আজানের অ্যালার্ট"
    override val adhanAlertDesc = "প্রতি ওয়াক্ত শুরু হলে একটি নোটিফিকেশন"
    override val timesMayDiffer = "আপনার দেশের সরকারি ক্যালেন্ডারের সাথে সময়ে এক-দুই মিনিট পার্থক্য হতে পারে।"
    override fun endsAt(t: String) = "শেষ $t"
    override fun iqamaAt(t: String) = "ইকামত $t"
    override val iqamaTitle = "ইকামতের সময়"
    override val iqamaDesc = "আপনার মসজিদে আজান ও ইকামতের মাঝের মিনিট; ০ দিলে লুকানো থাকবে।"
    override val prayerEndsNote = "ফজরের সময় শেষ হয় সূর্যোদয়ে, জোহরের আসর শুরু হলে, আসরের সূর্যাস্তে (সূর্য হলুদ হওয়া পর্যন্ত দেরি করা মাকরুহ), মাগরিবের লাল আভা মিলিয়ে গেলে; এশার পছন্দনীয় সময় মধ্যরাতে শেষ হয়, আর প্রয়োজনের সময় থাকে ফজর পর্যন্ত।"
    override val locationFound = "অবস্থান পাওয়া গেছে ✓"
    override val locationFailed = "আপনার অবস্থান পাওয়া যায়নি। লোকেশন সার্ভিস চালু আছে কি না দেখে নিন।"
    override val locationDenied = "লোকেশনের অনুমতি দেওয়া হয়নি। তালিকা থেকে আপনার শহর বেছে নিন।"
    override val cities = mapOf(
        "makkah" to "মক্কা", "madinah" to "মদিনা", "riyadh" to "রিয়াদ", "jeddah" to "জেদ্দা",
        "dammam" to "দাম্মাম", "abha" to "আবহা", "dubai" to "দুবাই", "abudhabi" to "আবুধাবি", "sharjah" to "শারজাহ",
        "kuwait" to "কুয়েত সিটি", "doha" to "দোহা", "manama" to "মানামা", "muscat" to "মাস্কাট",
        "cairo" to "কায়রো", "alexandria" to "আলেকজান্দ্রিয়া", "aswan" to "আসওয়ান", "amman" to "আম্মান",
        "jerusalem" to "জেরুজালেম (আল-কুদস)", "gaza" to "গাজা", "beirut" to "বৈরুত", "damascus" to "দামেস্ক", "aleppo" to "আলেপ্পো",
        "baghdad" to "বাগদাদ", "basra" to "বসরা", "erbil" to "এরবিল", "sanaa" to "সানা", "aden" to "এডেন",
        "khartoum" to "খার্তুম", "tripoli" to "ত্রিপোলি", "benghazi" to "বেনগাজি", "tunis" to "তিউনিস",
        "algiers" to "আলজিয়ার্স", "oran" to "ওরান", "rabat" to "রাবাত", "casablanca" to "কাসাব্লাঙ্কা",
        "marrakesh" to "মারাকেশ", "nouakchott" to "নুয়াকশোট", "mogadishu" to "মোগাদিশু", "djibouti" to "জিবুতি",
        "istanbul" to "ইস্তাম্বুল", "ankara" to "আঙ্কারা", "karachi" to "করাচি", "lahore" to "লাহোর",
        "islamabad" to "ইসলামাবাদ", "delhi" to "দিল্লি", "mumbai" to "মুম্বাই", "hyderabad" to "হায়দরাবাদ",
        "dhaka" to "ঢাকা", "chittagong" to "চট্টগ্রাম", "kabul" to "কাবুল", "tehran" to "তেহরান", "mashhad" to "মাশহাদ",
        "tashkent" to "তাশখন্দ", "baku" to "বাকু", "almaty" to "আলমাতি", "moscow" to "মস্কো", "kazan" to "কাজান",
        "grozny" to "গ্রোজনি", "kualalumpur" to "কুয়ালালামপুর", "penang" to "পেনাং", "singapore" to "সিঙ্গাপুর",
        "brunei" to "বন্দর সেরি বেগাওয়ান", "jakarta" to "জাকার্তা", "surabaya" to "সুরাবায়া", "bandung" to "বান্দুং",
        "medan" to "মেদান", "makassar" to "মাকাসার", "lagos" to "লাগোস", "kano" to "কানো", "dakar" to "ডাকার",
        "sarajevo" to "সারায়েভো", "london" to "লন্ডন", "birmingham" to "বার্মিংহাম", "paris" to "প্যারিস",
        "marseille" to "মার্সেই", "lyon" to "লিওঁ", "brussels" to "ব্রাসেলস", "amsterdam" to "আমস্টারডাম",
        "berlin" to "বার্লিন", "hamburg" to "হামবুর্গ", "cologne" to "কোলন", "munich" to "মিউনিখ",
        "vienna" to "ভিয়েনা", "stockholm" to "স্টকহোম", "newyork" to "নিউ ইয়র্ক", "chicago" to "শিকাগো",
        "losangeles" to "লস অ্যাঞ্জেলেস", "toronto" to "টরন্টো", "montreal" to "মন্ট্রিয়ল", "sydney" to "সিডনি",
    )
    override val palestine = "ফিলিস্তিন"

    // ---------- Notifications ----------
    override val channelName = "নামাজের সময়"
    override val channelDesc = "প্রতি ওয়াক্ত শুরু হলে একটি অ্যালার্ট"
    override fun prayerNowTitle(prayer: String) = "$prayer নামাজের সময় হয়েছে"
    override fun prayerNowText(city: String) = "$city — নামাজের দিকে আসুন"
    // ---------- Adhan & alerts ----------
    override val adhanSettings = "আজান ও সতর্কবার্তা"
    override val adhanSettingsSub = "মুয়াজ্জিন, প্রতিটি নামাজের সতর্কবার্তা ও কম্পন"
    override val alertPerPrayer = "প্রতিটি নামাজের সতর্কবার্তা"
    override val alertModeAdhan = "আজান"
    override val alertModeVibrate = "শুধু কম্পন"
    override val alertModeSilent = "নীরব বিজ্ঞপ্তি"
    override val alertModeOff = "সতর্কবার্তা নেই"
    override val alertModesHint = "“আজান” পুরো আজান বাজায়, “শুধু কম্পন” শব্দ ছাড়া কাঁপে, “নীরব বিজ্ঞপ্তি” শব্দ ছাড়া বিজ্ঞপ্তি দেখায়, আর “সতর্কবার্তা নেই” সেই নামাজের জন্য কিছু পাঠায় না।"
    override val muezzinTitle = "মুয়াজ্জিন"
    override val fajrAdhanTitle = "ফজরের আজান"
    override val fajrAdhanHint = "ফজরের আজানে “আস-সালাতু খাইরুম মিনান নাউম” (ঘুমের চেয়ে নামাজ উত্তম) যুক্ত হয়।"
    override val fajrSameAsOthers = "অন্যান্য নামাজের মতো"
    override val muezzinNames = mapOf(
        "asim" to "আসিম জাভেদ",
        "klcc" to "KLCC মসজিদ, কুয়ালালামপুর",
        "mirza" to "মোয়াজ্জম বেগ মির্জা, আহমেদাবাদ",
        "adam" to "আদম (উইকিমিডিয়া কমন্স)",
        "kalkan_fajr" to "ফজর — কালকান, তুরস্ক",
        "madinah" to "মসজিদে নববী, মদিনা",
        "andrewler" to "অ্যান্ড্রুলার (উইকিমিডিয়া কমন্স)",
        "aaqib" to "আকিব আজিজ",
        "fouad" to "ফুয়াদ আযান",
        "fouad_fajr" to "ফজর — ফুয়াদ আযান",
        "kas_fajr" to "ফজর — কাশ, তুরস্ক",
        "malmo_fajr" to "ফজর — বেসিম আজেমি, মালমো",
        "shalqar" to "শালকার মসজিদ, কাজাখস্তান",
    )
    override val playAdhan = "আজান শুনুন"
    override val stopAdhan = "থামান"
    override val vibrationTitle = "কম্পনের ধরন"
    override val vibeNames = mapOf("short" to "ছোট", "double" to "দুবার টোকা", "triple" to "তিনবার টোকা", "long" to "লম্বা", "heartbeat" to "হৃদস্পন্দন", "pulse" to "3 বার স্পন্দন", "wave" to "ঢেউ", "rise" to "ক্রমশ জোরালো", "gentle" to "মৃদু", "strong" to "জোরালো", "knock" to "দরজায় কড়া", "rapid" to "দ্রুত")
    override val tryVibration = "কম্পন পরীক্ষা করুন"
    override val adhanStopHint = "আজান থামাতে বিজ্ঞপ্তিতে “থামান” চাপুন বা ভলিউম বোতাম চাপুন। ফোন নীরব বা কম্পন মোডে থাকলে সতর্কবার্তা সেই মোড মেনে চলে।"
    override val adhanIosNote = "iPhone বিজ্ঞপ্তির শব্দ 30 সেকেন্ডে সীমিত রাখে, তাই বিজ্ঞপ্তিতে আজানের শুরুটা বাজে; নামাজের সময় মিজান খোলা থাকলে পুরো আজান বাজে। বিজ্ঞপ্তি iPhone-এর নিজস্ব ধরনে কাঁপে (Settings › Sounds & Haptics); এখানকার ধরনগুলো মিজান খোলা থাকলে অনুভূত হয়।"
    override val recordingSources = "রেকর্ডিংয়ের উৎস"

    // ---------- Qibla ----------
    override val qiblaTitle = "কিবলার দিক"
    override val noCompass = "আপনার ডিভাইসে কম্পাস নেই। নিচের কোণ অনুযায়ী হাতে দিক ঠিক করুন।"
    override val facingQibla = "✓ আপনি কিবলামুখী আছেন"
    override fun turnRight(deg: Int) = "ফোনটি $deg° ডানে ঘোরান"
    override fun turnLeft(deg: Int) = "ফোনটি $deg° বাঁয়ে ঘোরান"
    override val qiblaAngle = "প্রকৃত উত্তর থেকে কিবলার কোণ"
    override fun towards(dir: String) = "$dir দিকে"
    override val directions = listOf("উত্তর", "উত্তর-পূর্ব", "পূর্ব", "দক্ষিণ-পূর্ব", "দক্ষিণ", "দক্ষিণ-পশ্চিম", "পশ্চিম", "উত্তর-পশ্চিম")
    override val location = "অবস্থান"
    override val distanceToKaaba = "কাবা পর্যন্ত দূরত্ব"
    override val phoneHeading = "ফোনের দিক"
    override val compassLowAccuracy = "কম্পাসের নির্ভুলতা কম: ক্যালিব্রেট করতে ফোনটি 8-এর আকারে ঘোরান, ধাতু ও ইলেকট্রনিক যন্ত্র থেকে দূরে রাখুন।"
    override val qiblaTip = "অবস্থান বদলাতে নামাজের সময়সূচি পাতাটি ব্যবহার করুন। সঠিক রিডিং পেতে ফোনটি সমতলে ও ধাতু থেকে দূরে ধরে রাখুন।"

    // ---------- Tasbih ----------
    override val tasbihTitle = "ডিজিটাল তাসবিহ"
    override val tasbihSubtitle = "গুনতে বৃত্তে চাপ দিন"
    override val modeFree = "মুক্ত জিকির"
    override val modeAfterPrayer = "নামাজের পর"
    override fun ofGoal(n: Int) = "$n-এর মধ্যে"
    override val nextDhikr = "পরের জিকিরে যান"
    override val completedHundred = "আল্লাহ কবুল করুন ✓ আপনি একশো বার পূর্ণ করেছেন"
    override fun completedN(n: Int) = "$n বার সম্পন্ন ✓"
    override val reset = "রিসেট"
    override val undo = "আনডু"
    override val chooseDhikr = "জিকির বেছে নিন"
    override val targetCount = "লক্ষ্য সংখ্যা"
    override val yourStats = "আপনার পরিসংখ্যান"
    override val roundsDone = "চক্র সম্পন্ন"
    override val todayCount = "আজ"
    override val totalCount = "সর্বমোট"
    override val dhikrLabels = listOf(
        "সুবহানাল্লাহ", "আলহামদুলিল্লাহ", "আল্লাহু আকবার", "লা ইলাহা ইল্লাল্লাহ", "আস্তাগফিরুল্লাহ",
        "সুবহানাল্লাহি ওয়া বিহামদিহি", "সুবহানাল্লাহিল আজিম", "লা হাওলা ওয়া লা কুওয়াতা ইল্লা বিল্লাহ", "নবী ﷺ-এর ওপর দরুদ",
    )
    override val dhikrMeanings = listOf(
        "আল্লাহ মহাপবিত্র",
        "সমস্ত প্রশংসা আল্লাহর",
        "আল্লাহ সর্বমহান",
        "আল্লাহ ছাড়া কোনো ইলাহ নেই",
        "আমি আল্লাহর কাছে ক্ষমা চাই",
        "আল্লাহ মহাপবিত্র, সকল প্রশংসা তাঁর",
        "মহান আল্লাহ মহাপবিত্র",
        "আল্লাহর সাহায্য ছাড়া কোনো শক্তি ও সামর্থ্য নেই",
        "হে আল্লাহ, মুহাম্মাদ ﷺ-এর ওপর রহমত বর্ষণ করুন",
    )
    override val tamamMeaning = "আল্লাহ ছাড়া কোনো ইলাহ নেই, তিনি এক, তাঁর কোনো শরিক নেই। রাজত্ব তাঁরই, প্রশংসাও তাঁরই, এবং তিনি সব কিছুর ওপর ক্ষমতাবান।"
    override val tapToCount = "গুনতে চাপ দিন"

    // ---------- Zakat ----------
    override val zakatTitle = "যাকাত ক্যালকুলেটর"
    override val zakatSubtitle = "নিসাব পরিমাণ সম্পদ পূর্ণ এক চান্দ্রবর্ষ থাকলে তার 2.5%"
    override val partCash = "নগদ ও ব্যালেন্স"
    override val partReceivables = "আদায়যোগ্য পাওনা"
    override val partGold = "স্বর্ণ"
    override val partSilver = "রুপা"
    override val partStocks = "শেয়ার"
    override val partTrade = "ব্যবসায়িক পণ্য"
    override val enterGoldPrice = "নিসাব হিসাব করতে স্বর্ণের দাম লিখুন"
    override val enterSilverPrice = "নিসাব হিসাব করতে রুপার দাম লিখুন"
    override val nisabReached = "✓ আপনার সম্পদ নিসাব পূর্ণ করেছে — যাকাত ফরজ"
    override val belowNisab = "নিসাবের কম — যাকাত ফরজ নয়"
    override val enterAssets = "আপনার সম্পদের পরিমাণ লিখুন"
    override val zakatDue = "প্রদেয় যাকাত"
    override val totalAssets = "যাকাতযোগ্য মোট সম্পদ"
    override val deductedDebts = "বাদ দেওয়া ঋণ"
    override val netBase = "নিট যাকাতযোগ্য সম্পদ"
    override val nisabValue = "নিসাবের মূল্য"
    override val approxEquals = "আনুমানিক সমপরিমাণ"
    override fun goldGrams24(g: String) = "$g গ্রাম 24 ক্যারেট স্বর্ণ"
    override val shareResult = "ফলাফল শেয়ার করুন"
    override fun zakatShareText(zakat: String, base: String) = "আমার প্রদেয় যাকাত: $zakat (যাকাতযোগ্য সম্পদ $base)"
    override val settingsAndPrices = "সেটিংস ও দাম"
    override val currency = "মুদ্রা"
    override val nisabBasis = "নগদ অর্থের নিসাব"
    override val nisabGold = "স্বর্ণ (85 গ্রাম)"
    override val nisabSilver = "রুপা (595 গ্রাম)"
    override val goldPrice24 = "1 গ্রাম 24 ক্যারেট স্বর্ণের দাম"
    override val goldPriceHint = "জুয়েলারি দোকান বা আপনার দেশের নির্ভরযোগ্য দামের সাইট থেকে"
    override val silverPrice = "1 গ্রাম রুপার দাম (ঐচ্ছিক)"
    override val cashSection = "নগদ ও ব্যালেন্স"
    override val cashInHand = "হাতে থাকা নগদ"
    override val bankBalances = "ব্যাংক ও ওয়ালেটের ব্যালেন্স"
    override val receivablesField = "আপনার পাওনা ঋণ, যা ফেরত পাওয়ার আশা আছে"
    override val otherMoney = "অন্যান্য অর্থ"
    override val goldSilverSection = "স্বর্ণ ও রুপা (গ্রামে)"
    override fun goldKarat(k: Int) = "$k ক্যারেট স্বর্ণ"
    override val pureSilver = "খাঁটি রুপা"
    override val includeJewelry = "ব্যবহৃত অলংকারও হিসাবে ধরুন (হানাফি মত, এবং অধিক সতর্কতামূলক)"
    override val jewelryField = "এর মধ্যে ব্যবহৃত অলংকার (21 ক্যারেট সমতুল্য)"
    override val jewelryHint = "জমহুরের মতে ব্যক্তিগত ব্যবহারের অলংকারে যাকাত নেই, তাই এটি বাদ যায়"
    override val investSection = "বিনিয়োগ ও ব্যবসা"
    override val stocksTrade = "কেনাবেচার জন্য রাখা শেয়ার (বাজারমূল্য)"
    override val stocksLong = "দীর্ঘমেয়াদি শেয়ার (যাকাতযোগ্য অংশ)"
    override val tradeGoods = "ব্যবসায়িক পণ্য (মজুদের মূল্য)"
    override val deductSection = "যা বাদ যাবে"
    override val debtsField = "চলতি বছরে পরিশোধযোগ্য আপনার ঋণ"
    override val debtsHint = "শুধু যাকাতবর্ষের মধ্যে দেয় কিস্তিগুলো"
    override val breakdownTitle = "সম্পদের ধরন অনুযায়ী যাকাত"
    override val zakatNote = "আপনার তথ্য শুধু আপনার ডিভাইসেই থাকে। আল্লাহ সূরা তাওবায় (9:60) যাকাতের আটটি খাত উল্লেখ করেছেন।"

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "সদকাতুল ফিতর"
    override val fitrSubtitle = "ঈদের নামাজের আগে আদায় করতে হয়"
    override val foodAmountDue = "প্রদেয় খাদ্যের পরিমাণ"
    override val totalCashValue = "মোট নগদ মূল্য"
    override fun forPeople(n: Int) = "$n জনের জন্য"
    override val peopleCount = "কতজনের পক্ষ থেকে দিচ্ছেন (নিজেসহ)"
    override val modeFood = "খাদ্য (সা')"
    override val modeMoney = "নগদ মূল্য"
    override val foodType = "খাদ্যের ধরন"
    override val foods = mapOf(
        "rice" to "চাল", "riceSafe" to "চাল (অধিক সতর্কতা)", "wheat" to "গম",
        "dates" to "খেজুর", "barley" to "যব", "other" to "অন্য পরিমাণ…",
    )
    override fun approxKg(kg: String) = "প্রায় $kg কেজি"
    override val saWeight = "এক সা'-এর ওজন (কেজি)"
    override val kgPrice = "প্রতি কেজির দাম (ঐচ্ছিক)"
    override val kgPriceHint = "খাদ্য কেনার আনুমানিক খরচ জানতে"
    override val saPerPerson = "জনপ্রতি এক সা'"
    override val approxCost = "আনুমানিক খরচ"
    override val valuePerPerson = "জনপ্রতি সদকাতুল ফিতরের মূল্য"
    override val valuePerPersonHint = "প্রতি বছর আপনার দেশের সরকারি সংস্থা ঘোষণা করে"
    override val valueNote = "মূল্য দিয়ে আদায় করা হানাফি মত; জমহুরের মতে খাদ্য দিতে হয়।"
    override val fitrTimingNote = "কখন দেবেন: ঈদের নামাজের আগে। এক-দুই দিন আগেও দেওয়া যায়, তবে বিনা ওজরে নামাজের পরে দেরি করা যাবে না। এটি ফকির ও মিসকিনদের দেওয়া হয়।"

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "ফিদিয়া ও কাফফারা"
    override val fidyaSubtitle = "খাওয়ানোর পরিমাণ — কেজি ও মূল্যে"
    override val tabFidya = "রোজার ফিদিয়া"
    override val tabOath = "কসম"
    override val tabIntercourse = "সহবাস"
    override val fidyaResult = "ছুটে যাওয়া রোজার ফিদিয়া"
    override fun fidyaSub(days: Int) = bn(days.toLong(), "দিনের") + " জন্য — প্রতিদিন একজন মিসকিন"
    override val fidyaNote = "যিনি স্থায়ীভাবে রোজা রাখতে অক্ষম, যেমন বৃদ্ধ বা আরোগ্যের আশা নেই এমন রোগী। সব দিনের জন্য একজন মিসকিনকে বা একাধিক জনকে খাওয়ানো যায়।"
    override val oathResult = "কসম ভঙ্গের কাফফারা"
    override fun oathSub(oaths: Int) = if (oaths > 1) "${oaths}টি কাফফারা × 10 জন মিসকিন" else "10 জন মিসকিনকে খাওয়ানো"
    override val oathAlternative = "অক্ষম হলে"
    override fun fastDays(n: Int) = bn(n.toLong(), "দিন") + " রোজা"
    override val oathNote = "দশজন মিসকিনকে খাওয়ানো, তাদের বস্ত্র দেওয়া অথবা একজন দাস মুক্ত করা — এর যেকোনো একটি; যে তা পারে না, সে তিন দিন রোজা রাখবে (সূরা মায়েদা 5:89)।"
    override val intercourseResult = "রমজানের দিনে সহবাসের কাফফারা"
    override val intercourseSub = "রোজা রাখতে অক্ষম হলে 60 জন মিসকিনকে খাওয়ানো"
    override val ifStartToday = "আজ রোজা শুরু করলে শেষ হবে"
    override val twoMonthsDuration = "দুই চান্দ্রমাসের মেয়াদ"
    override val intercourseNote = "ক্রমানুসারে: একজন দাস মুক্ত করা; সম্ভব না হলে লাগাতার দুই মাস রোজা রাখা; তাতেও অক্ষম হলে ষাটজন মিসকিনকে খাওয়ানো — সাথে ওই দিনের কাজা ও তওবা।"
    override val daysField = "দিনের সংখ্যা"
    override val oathsField = "কাফফারার সংখ্যা"
    override val feedMeasure = "প্রতি মিসকিনের খাদ্য"
    override val muddOption = "এক মুদ — প্রায় 0.75 কেজি (মালেকি ও শাফেয়ি)"
    override val halfSaOption = "অর্ধ সা' — প্রায় 1.5 কেজি (সমকালীন ফতোয়া)"
    override val poorCount = "মিসকিন / বেলার সংখ্যা"
    override val approxValue = "আনুমানিক মূল্য"

    // ---------- Hijri converter ----------
    override val hijriTitle = "হিজরি তারিখ"
    override val hijriSubtitle = "উম্মুল কুরা ক্যালেন্ডার অনুযায়ী"
    override val correspondingHijri = "সংশ্লিষ্ট হিজরি তারিখ"
    override val correspondingGreg = "সংশ্লিষ্ট খ্রিস্টীয় তারিখ"
    override val enterGregYear = "1882 থেকে 2174-এর মধ্যে একটি বছর লিখুন"
    override val enterHijriYear = "1300 থেকে 1600-এর মধ্যে একটি হিজরি বছর লিখুন"
    override val gregToHijri = "খ্রিস্টীয় → হিজরি"
    override val hijriToGreg = "হিজরি → খ্রিস্টীয়"
    override val todayDate = "আজকের তারিখ"
    override val prevMonth = "আগের মাস"
    override val nextMonth = "পরের মাস"
    override val calendarHint = "বড় সংখ্যাটি হিজরি, ছোটটি খ্রিস্টীয়। রূপান্তর করতে যেকোনো দিনে চাপ দিন।"
    override val moonNote = "চাঁদ দেখার ভিন্নতার কারণে কিছু দেশে তারিখ এক দিন আগে-পরে হতে পারে।"

    // ---------- Age ----------
    override val ageTitle = "বয়স ক্যালকুলেটর"
    override val ageSubtitle = "হিজরি ও খ্রিস্টীয় বছরে"
    override val ageGregorian = "আপনার বয়স (খ্রিস্টীয়)"
    override fun ymd(y: Int, m: Int, d: Int) = "${bn(y.toLong(), "বছর")} ${bn(m.toLong(), "মাস")} ${bn(d.toLong(), "দিন")}"
    override fun inHijri(age: String) = "হিজরি: $age"
    override val birthGreg = "জন্মতারিখ (খ্রিস্টীয়)"
    override val birthHijri = "জন্মতারিখ (হিজরি)"
    override val birthWeekday = "জন্মের বার"
    override val nextBirthday = "পরবর্তী জন্মদিন"
    override val birthdayToday = "আজ 🎉"
    override val tileDays = "দিন"
    override val tileWeeks = "সপ্তাহ"
    override val tileHours = "ঘণ্টা"
    override val yourAge = "আপনার বয়স"
    override val birthAfterToday = "জন্মতারিখটি আজকের পরে"
    override val enterBirth = "আপনার জন্মতারিখ লিখুন"
    override val birthDate = "জন্মতারিখ"
    override val gregorian = "খ্রিস্টীয়"
    override val hijri = "হিজরি"

    // ---------- Islamic events ----------
    override val eventsTitle = "ইসলামি দিবস"
    override val eventsSubtitle = "উম্মুল কুরা ক্যালেন্ডার অনুযায়ী কাউন্টডাউন"
    override val events = mapOf(
        "ramadan" to Named("রমজান", "রোজা, রাতের নামাজ ও কুরআন তিলাওয়াতের মাস।"),
        "lastTen" to Named("শেষ দশ রাত", "এর মধ্যেই রয়েছে লাইলাতুল কদর, যা হাজার মাসের চেয়ে উত্তম।"),
        "eidFitr" to Named("ঈদুল ফিতর", "এর নামাজের আগে সদকাতুল ফিতর আদায় করা হয়।"),
        "shawwal" to Named("শাওয়ালের ছয় রোজা", "যে রমজানের রোজা রাখল, অতঃপর শাওয়ালের ছয়টি রোজা রাখল, সে যেন সারা জীবন রোজা রাখল (মুসলিম)।"),
        "dhulHijjah" to Named("জিলহজের প্রথম দশ দিন", "দুনিয়ার শ্রেষ্ঠ দিন; এ সময় নেক আমল ও তাকবির মুস্তাহাব।"),
        "arafah" to Named("আরাফার দিন", "হজে না থাকা ব্যক্তির জন্য এ দিনের রোজা আগের ও পরের এক বছরের গুনাহের কাফফারা (মুসলিম)।"),
        "eidAdha" to Named("ঈদুল আজহা", "কোরবানির দিন, এরপর আইয়ামে তাশরিকের তিন দিন।"),
        "newYear" to Named("হিজরি নববর্ষ", "নতুন হিজরি বছরের সূচনা।"),
        "ashura" to Named("আশুরার দিন", "এ দিনের রোজা আগের এক বছরের গুনাহের কাফফারা (মুসলিম); এর সাথে 9 তারিখের রোজা রাখাও মুস্তাহাব।"),
    )
    override val unitDays = "দিন"
    override val unitHours = "ঘণ্টা"
    override val unitMinutes = "মিনিট"
    override val unitSeconds = "সেকেন্ড"
    override val eventsNote = "তারিখগুলো আনুমানিক (উম্মুল কুরা); আপনার দেশে চাঁদ দেখার ওপর নির্ভর করে এক দিন আগে-পরে হতে পারে।"

    // ---------- Inheritance ----------
    override val inhTitle = "উত্তরাধিকার ক্যালকুলেটর"
    override val inhSubtitle = "ফারায়েজের নিয়মে সম্পত্তি বণ্টন"
    override val aslLabel = "মাসআলার মূল (আসল)"
    override val awlCase = "আউলের মাসআলা (অংশ সম্পত্তির চেয়ে বেশি)"
    override val raddCase = "রদের মাসআলা (অবশিষ্ট ফেরত)"
    override val adilCase = "সুষম মাসআলা"
    override fun tashihFrom(n: Long) = bn(n, "অংশে") + " সংশোধিত"
    override val sharesTitle = "প্রত্যেক ওয়ারিশের অংশ"
    override fun blockedBy(by: String) = "$by থাকায় বঞ্চিত"
    override fun perPerson(v: String) = "জনপ্রতি $v"
    override val shareDivision = "বণ্টন শেয়ার করুন"
    override fun divisionShareText(list: String) = "সম্পত্তি বণ্টন: $list"
    override val listSeparator = ", "
    override val addHeirs = "নিচের তালিকা থেকে ওয়ারিশ যোগ করুন"
    override val deceasedAndEstate = "মৃত ব্যক্তি ও সম্পত্তি"
    override val deceasedMale = "মৃত ব্যক্তি পুরুষ"
    override val deceasedFemale = "মৃত ব্যক্তি নারী"
    override val estateTotal = "মোট সম্পত্তি"
    override val funeralCosts = "কাফন-দাফনের খরচ"
    override val deceasedDebts = "মৃত ব্যক্তির ঋণ"
    override val bequest = "অসিয়ত (ওয়ারিশ নয় এমন কারও জন্য)"
    override val bequestHint = "ঋণ পরিশোধের পর অবশিষ্টের এক-তৃতীয়াংশের বেশি নয়, যদি না ওয়ারিশরা সম্মত হন"
    override val netEstate = "বণ্টনযোগ্য নিট সম্পত্তি"
    override val bequestCapped = "(অসিয়ত এক-তৃতীয়াংশে সীমিত)"
    override val newCase = "নতুন মাসআলা শুরু করুন"
    override val inhDisclaimer = "ফলাফলটি জমহুরের মতের ভিত্তিতে একটি নির্দেশিকা মাত্র। এটি শরিয়া আদালত বা ফারায়েজ বিশেষজ্ঞ আলেমের বিকল্প নয়।"
    override val heirs = mapOf(
        "husband" to "স্বামী", "wife" to "স্ত্রী", "father" to "পিতা", "mother" to "মাতা",
        "gf" to "দাদা", "gmm" to "নানি", "gmf" to "দাদি",
        "son" to "পুত্র", "daughter" to "কন্যা", "gson" to "পুত্রের পুত্র", "gdau" to "পুত্রের কন্যা",
        "fb" to "আপন ভাই", "fs" to "আপন বোন", "pb" to "বৈমাত্রেয় ভাই", "ps" to "বৈমাত্রেয় বোন",
        "mb" to "বৈপিত্রেয় ভাই", "ms" to "বৈপিত্রেয় বোন", "fnep" to "আপন ভাইয়ের পুত্র", "pnep" to "বৈমাত্রেয় ভাইয়ের পুত্র",
        "fun" to "আপন চাচা", "pun" to "বৈমাত্রেয় চাচা", "fcous" to "আপন চাচার পুত্র", "pcous" to "বৈমাত্রেয় চাচার পুত্র",
    )
    override val heirGroups = mapOf(
        "spouses" to "স্বামী-স্ত্রী", "ascendants" to "পিতা-মাতা ও দাদা-দাদি/নানি", "descendants" to "সন্তান ও নাতি-নাতনি",
        "siblings" to "ভাই-বোন", "distant" to "দূরবর্তী পুরুষ আত্মীয়",
    )
    override val blockers = mapOf(
        "gf" to "দাদা",
        "twoDaughters" to "দুই কন্যা (দুই-তৃতীয়াংশ পূর্ণ)",
        "descendant" to "উত্তরাধিকারী সন্তান-সন্ততি",
        "fbAfterMuadda" to "আপন ভাই (মুআদ্দার পর)",
        "fsTookRest" to "অবশিষ্ট গ্রহণকারী আপন বোন",
        "gfAndSiblings" to "দাদা ও ভাই-বোন",
        "twoFullSisters" to "দুই আপন বোন",
        "fsGhayr" to "আপন বোন (কন্যাদের সাথে আসাবা)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "নির্ধারিত অংশ"
        ShareType.FARD_TASIB -> "নির্ধারিত অংশ + আসাবা"
        ShareType.FARD_TAKMILA -> "নির্ধারিত অংশ (দুই-তৃতীয়াংশ পূরণ)"
        ShareType.TASIB -> "আসাবা"
        ShareType.TASIB_BIL_GHAYR -> "ভাইয়ের মাধ্যমে আসাবা"
        ShareType.TASIB_MAA_GHAYR -> "কন্যাদের সাথে আসাবা"
        ShareType.TASIB_MAA_JADD -> "দাদার সাথে আসাবা"
        ShareType.FARD_THEN_MUQASAMA -> "নির্ধারিত অংশ, তারপর ভাগাভাগি"
        ShareType.JADD_MUQASAMA -> "ভাই-বোনের সাথে: ভাগাভাগি"
        ShareType.JADD_THIRD_ESTATE -> "ভাই-বোনের সাথে: সম্পত্তির এক-তৃতীয়াংশ"
        ShareType.JADD_THIRD_REST -> "ভাই-বোনের সাথে: অবশিষ্টের এক-তৃতীয়াংশ"
        ShareType.JADD_SIXTH -> "ভাই-বোনের সাথে: সম্পত্তির এক-ষষ্ঠাংশ"
    } + if (radd) " + রদ" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "বৈপিত্রেয় ভাই-বোনেরা নারী-পুরুষ নির্বিশেষে এক-তৃতীয়াংশে সমান অংশীদার।"
        InhNote.Umariyya -> "এটি দুই উমারিয়্যা মাসআলার একটি: স্বামী বা স্ত্রীর অংশ দেওয়ার পর অবশিষ্টের এক-তৃতীয়াংশ মাতা পান।"
        InhNote.GrandmothersShare -> "দাদি ও নানি এক-ষষ্ঠাংশে সমান অংশীদার।"
        InhNote.FullSistersGhayr -> "আপন বোনেরা কন্যাদের সাথে আসাবা হয়ে যায়।"
        InhNote.Akdariyya -> "এটি আকদারিয়্যা মাসআলা: বোনকে অর্ধেক ও দাদাকে এক-ষষ্ঠাংশ দেওয়া হয়, ফলে মাসআলা 9-এ আউল হয়; এরপর দুজনের অংশ একত্র করে পুরুষ দুই ভাগ ও নারী এক ভাগ হারে বণ্টন করা হয়, ফলে মাসআলা 27-এ দাঁড়ায়।"
        is InhNote.JaddRule -> "ভাই-বোনদের সাথে দাদা তাঁর জন্য সবচেয়ে লাভজনকটি নেন: ভাগাভাগি, অথবা " +
            (if (n.otherFard) "অবশিষ্টের এক-তৃতীয়াংশ, অথবা সম্পত্তির এক-ষষ্ঠাংশ" else "সম্পত্তির এক-তৃতীয়াংশ") +
            " — এটি জমহুরের মত (মালেকি, শাফেয়ি, হাম্বলি)। ইমাম আবু হানিফা (রহ.)-এর মতে দাদা পিতার মতোই ভাই-বোনদের বঞ্চিত করেন।"
        InhNote.Muadda -> "বৈমাত্রেয় ভাই-বোনদের দাদার বিপরীতে গণনা করা হয়েছে (মুআদ্দা), এরপর প্রয়োজন হলে তাদের অংশ আপন ভাই-বোনদের কাছে ফিরে যায়।"
        is InhNote.NothingLeft -> "নির্ধারিত অংশগুলো দেওয়ার পর কিছুই অবশিষ্ট নেই, তাই আসাবা (${nameOf(n.winner)}) কিছুই পান না।"
        InhNote.Mushtaraka -> "এটি মুশতারাকা (হিমারিয়্যা) মাসআলা: মালেকি ও শাফেয়িগণ আপন ভাই-বোনদের বৈপিত্রেয় ভাই-বোনদের সাথে এক-তৃতীয়াংশে শরিক করেন; এখানে ফলাফল হানাফি ও হাম্বলি মত অনুযায়ী।"
        is InhNote.Awl -> "নির্ধারিত অংশগুলো মূলকে ছাড়িয়ে গেছে, তাই মাসআলা ${n.asl} থেকে ${n.awl}-এ বাড়ানো হয়েছে (আউল) এবং প্রত্যেকের অংশ আনুপাতিক হারে কমেছে।"
        InhNote.Radd -> "কোনো আসাবা নেই, তাই অবশিষ্ট অংশ স্বামী-স্ত্রী ছাড়া বাকি অংশীদারদের মধ্যে তাদের অংশের অনুপাতে ফিরিয়ে দেওয়া হয় (রদ)।"
        InhNote.SpouseOnlyRadd -> "একমাত্র ওয়ারিশ স্বামী বা স্ত্রী: অনেক সমকালীন আলেম ও কয়েকটি পারিবারিক আইনে অবশিষ্ট তাকেই ফিরিয়ে দেওয়া হয়; পূর্ববর্তী আলেমগণ তা যাবিল আরহামকে, এরপর বায়তুল মালকে দিতেন।"
        InhNote.NoHeirs -> "কোনো যাবিল ফুরুজ বা আসাবা নেই; সম্পত্তি যাবিল আরহাম, এরপর বায়তুল মালে যাবে। উপযুক্ত আদালতের শরণাপন্ন হোন।"
    }

    // ---------- Quran ----------
    override val tabQuran = "কুরআন"
    override val quranTitle = "পবিত্র কুরআন"
    override val quranSubtitle = "তাফসির পড়তে যেকোনো আয়াতে চাপ দিন"
    override val surahsTab = "সূরা"
    override val juzTab = "পারা"
    override val bookmarksTab = "সংরক্ষিত"
    override val continueReading = "পড়া চালিয়ে যান"
    override val startReading = "পড়া শুরু করুন"
    override val meccan = "মাক্কি"
    override val medinan = "মাদানি"
    override fun ayahCount(n: Int) = bn(n.toLong(), "আয়াত")
    override fun juzName(n: Int) = "পারা $n"
    override fun pageName(n: Int) = "পৃষ্ঠা $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · আয়াত $ayah"
    override val surahNames = listOf(
        "আল-ফাতিহা", "আল-বাকারা", "আলে ইমরান", "আন-নিসা", "আল-মায়েদা", "আল-আনআম",
        "আল-আরাফ", "আল-আনফাল", "আত-তাওবা", "ইউনুস", "হুদ", "ইউসুফ",
        "আর-রাদ", "ইবরাহিম", "আল-হিজর", "আন-নাহল", "আল-ইসরা", "আল-কাহফ",
        "মারইয়াম", "ত্বা-হা", "আল-আম্বিয়া", "আল-হাজ্জ", "আল-মুমিনুন", "আন-নূর",
        "আল-ফুরকান", "আশ-শুআরা", "আন-নামল", "আল-কাসাস", "আল-আনকাবুত", "আর-রুম",
        "লুকমান", "আস-সাজদা", "আল-আহযাব", "সাবা", "ফাতির", "ইয়াসিন",
        "আস-সাফফাত", "সোয়াদ", "আয-যুমার", "গাফির", "ফুসসিলাত", "আশ-শুরা",
        "আয-যুখরুফ", "আদ-দুখান", "আল-জাসিয়া", "আল-আহকাফ", "মুহাম্মাদ", "আল-ফাতহ",
        "আল-হুজুরাত", "ক্বাফ", "আয-যারিয়াত", "আত-তুর", "আন-নাজম", "আল-কামার",
        "আর-রাহমান", "আল-ওয়াকিয়া", "আল-হাদিদ", "আল-মুজাদালা", "আল-হাশর", "আল-মুমতাহিনা",
        "আস-সাফ", "আল-জুমুআ", "আল-মুনাফিকুন", "আত-তাগাবুন", "আত-তালাক", "আত-তাহরিম",
        "আল-মুলক", "আল-কলম", "আল-হাক্কা", "আল-মাআরিজ", "নূহ", "আল-জিন",
        "আল-মুযযাম্মিল", "আল-মুদ্দাসসির", "আল-কিয়ামা", "আল-ইনসান", "আল-মুরসালাত", "আন-নাবা",
        "আন-নাযিআত", "আবাসা", "আত-তাকভির", "আল-ইনফিতার", "আল-মুতাফফিফিন", "আল-ইনশিকাক",
        "আল-বুরুজ", "আত-তারিক", "আল-আলা", "আল-গাশিয়া", "আল-ফাজর", "আল-বালাদ",
        "আশ-শামস", "আল-লাইল", "আদ-দুহা", "আশ-শারহ", "আত-তিন", "আল-আলাক",
        "আল-কদর", "আল-বাইয়্যিনা", "আয-যালযালা", "আল-আদিয়াত", "আল-কারিআ", "আত-তাকাসুর",
        "আল-আসর", "আল-হুমাযা", "আল-ফিল", "কুরাইশ", "আল-মাউন", "আল-কাওসার",
        "আল-কাফিরুন", "আন-নাসর", "আল-মাসাদ", "আল-ইখলাস", "আল-ফালাক", "আন-নাস",
    )
    override val searchQuranHint = "সূরা বা আয়াতের শব্দ খুঁজুন…"
    override val searchQuranGroup = "সূরা"
    override val searchAyahsGroup = "আয়াত"
    override val mushafView = "মুসহাফ আকারে"
    override val ayahView = "আয়াত ধরে ধরে"
    override val textLarger = "লেখা বড় করুন"
    override val textSmaller = "লেখা ছোট করুন"
    override val tafsirTitle = "তাফসির"
    override val tafsirMuyassar = "তাফসিরে মুয়াসসার"
    override val tafsirJalalayn = "তাফসিরে জালালাইন"
    override val translationOfMeanings = "অর্থানুবাদ"
    override val bookmarkAyah = "আয়াত সংরক্ষণ করুন"
    override val removeBookmark = "সংরক্ষিত থেকে সরান"
    override val copy = "কপি করুন"
    override val copied = "কপি হয়েছে ✓"
    override val prevAyah = "আগের আয়াত"
    override val nextAyah = "পরের আয়াত"
    override val prevSurah = "আগের সূরা"
    override val nextSurah = "পরের সূরা"
    override val noBookmarks = "এখনো কোনো আয়াত সংরক্ষিত নেই। সংরক্ষণ করতে আয়াত খুলে বুকমার্ক চিহ্নে চাপ দিন।"
    override val quranLoading = "মুসহাফ খোলা হচ্ছে…"
    override fun hizbName(n: Int) = "হিযব $n"
    override val khatmaTitle = "আমার খতম"
    override val khatmaPlanLabel = "সময়সীমা"
    override fun planDays(days: Int) = if (days == 0) "সময়সীমা নেই" else "$days দিন"
    override fun pagesRead(read: Int, total: Int) = "$read / $total পৃষ্ঠা"
    override val todayPortion = "আজকের অংশ"
    override fun streakDays(n: Int) = "টানা দিন: $n"
    override fun daysLeftLabel(n: Int) = "বাকি দিন: $n"
    override val portionDone = "আজকের অংশ সম্পন্ন ✓"
    override val khatmaDone = "খতম সম্পন্ন ✓ আল্লাহ কবুল করুন"
    override val newKhatma = "নতুন খতম শুরু করুন"
    override fun khatmasCompleted(n: Int) = "সম্পন্ন খতম: $n"
    override val continuePortion = "আমার অংশ চালিয়ে যান"
    override val ayahOfDay = "আজকের আয়াত"
    override val focusMode = "ফোকাস মোড"
    override val exitFocus = "ফোকাস মোড থেকে বের হোন"
    override val silenceTitle = "পড়ার সময় নোটিফিকেশন বন্ধ রাখবেন?"
    override val silenceBody = "ফোকাস মোডে মিজান ‘বিরক্ত করবেন না’ চালু করতে পারে এবং পড়া শেষ হলে আবার বন্ধ করে দেয়। অ্যান্ড্রয়েড একবার অনুমতি চাইবে।"
    override val allow = "অনুমতি দিন"
    override val notNow = "এখন নয়"
    override val tafsirLibrary = "তাফসির লাইব্রেরি"
    override val tafsirLibrarySub = "তাফসির ডাউনলোড করুন এবং আয়াতে চাপলে কোনগুলো দেখাবে তা বেছে নিন"
    override val onDevice = "এই ডিভাইসে"
    override val availableDownloads = "ডাউনলোডের জন্য উপলব্ধ"
    override val download = "ডাউনলোড"
    override fun downloadingPct(pct: Int) = "ডাউনলোড হচ্ছে… $pct%"
    override val cancel = "বাতিল"
    override val delete = "মুছুন"
    override fun deleteTafsirQ(name: String) = "“$name” এই ডিভাইস থেকে মুছবেন?"
    override val showInTafsir = "তাফসিরে দেখান"
    override val builtIn = "অ্যাপে অন্তর্ভুক্ত"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "ডাউনলোড ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
    override val retry = "আবার চেষ্টা করুন"
    override val moreTafsirs = "আরও তাফসির"
    override val tafsirLibraryNote = "তাফসিরগুলো উন্মুক্ত tafsir_api সংগ্রহ (quran.com ও QUL থেকে) থেকে ডাউনলোড হয়। শুধু এই ডাউনলোডের জন্যই ইন্টারনেট ব্যবহার হয়; এরপর এগুলো অফলাইনে কাজ করে।"
    override fun ayahRange(from: Int, to: Int) = "আয়াত $from–$to-এর তাফসির"
    override val recitation = "তিলাওয়াত"
    override val reciter = "কারী"
    override val playRecitation = "তিলাওয়াত শুনুন"
    override val pauseRecitation = "বিরতি"
    override val stopRecitation = "তিলাওয়াত বন্ধ করুন"
    override val repeatAyah = "প্রতিটি আয়াত পুনরাবৃত্তি"
    override val listenFromHere = "এই আয়াত থেকে শুনুন"
    override val listen = "শুনুন"
    override val save = "সংরক্ষণ"
    override val saved = "সংরক্ষিত"
    override val ayahActionsHint = "তাফসির, শোনা, সংরক্ষণ, কপি বা শেয়ার করতে যেকোনো আয়াতে দীর্ঘক্ষণ চাপুন"
    override val audioNote = "তিলাওয়াত EveryAyah.com থেকে চলে। যে আয়াতগুলো আপনি শুনেছেন সেগুলো আপনার ডিভাইসে সংরক্ষিত থাকে এবং ইন্টারনেট ছাড়াই চলে।"
    override val audioError = "তিলাওয়াত চালানো যায়নি। আপনার ইন্টারনেট সংযোগ পরীক্ষা করুন।"
    override val hifzMode = "হিফজ"
    override val revealNext = "পরেরটি দেখান"
    override val hideAll = "সব লুকান"
    override val hifzHint = "মুখস্থ থেকে পড়ুন, তারপর যাচাই করতে লুকানো আয়াতে স্পর্শ করুন।"
    override val displayTitle = "প্রদর্শন"
    override val pageColor = "পৃষ্ঠার রং"
    override val toneAuto = "স্বয়ংক্রিয়"
    override val toneSepia = "সেপিয়া"
    override val toneNight = "রাত"
    override val brightness = "উজ্জ্বলতা"
    override val brightnessSystem = "ডিভাইসের উজ্জ্বলতা"
    override val textSizeLabel = "লেখার আকার"
    override val continueFocus = "ফোকাস মোডে চালিয়ে যান"
    override val smartPresets = "স্মার্ট সেটিংস"
    override val presetListen = "শুনুন"
    override val presetMemorize = "হিফজ"
    override val presetReview = "পুনরাবৃত্তি"
    override val smartHint = "হিফজ: প্রতিটি আয়াত ৩ বার, কারীর পরে নিজে বলার জন্য বিরতিসহ। পুনরাবৃত্তি: পুরো সূরা, একটু দ্রুত।"
    override val range = "অংশ"
    override val rangeContinue = "একটানা"
    override val rangePage = "এই পৃষ্ঠা"
    override val rangeSurah = "এই সূরা"
    override val rangeCustom = "নিজের মতো"
    override val fromAyah = "থেকে"
    override val toAyah = "পর্যন্ত"
    override val repeatRange = "অংশটি পুনরাবৃত্তি"
    override val pauseAfter = "প্রতি আয়াতের পর বিরতি"
    override val off = "বন্ধ"
    override val pauseAyahLength = "আয়াতের সমান"
    override fun seconds(n: Int) = "$n সে"
    override val speed = "গতি"
    override val sleepTimer = "স্লিপ টাইমার"
    override fun minutes(n: Int) = "$n মিনিট"
    override val evidenceTitle = "দলিল ও ফকিহদের মতামত"
    override val evidenceSub = "এই বিধানের উৎস: কুরআন, সুন্নাহ ও মাযহাবসমূহ"
    override val proofQuran = "কুরআন"
    override val proofHadith = "হাদিস"
    override val proofConsensus = "ইজমা"
    override val proofScholars = "ফকিহদের মতামত"
    override val zakatTypesTitle = "যাকাতের পাঁচ প্রকার"
    override val zakatTypesSub = "যাকাত হিসাব করতে ও দলিল দেখতে সম্পদের একটি ধরন বেছে নিন"
    override val zkMoneyName = "নগদ অর্থ, স্বর্ণ ও রুপা"
    override val zkMoneyDesc = "নগদ, সঞ্চয়, স্বর্ণ ও রুপা: নিসাব পরিমাণ সম্পদ এক চান্দ্রবর্ষ থাকলে একবার 2.5%"
    override val zkTradeName = "ব্যবসায়িক পণ্য ও শেয়ার"
    override val zkTradeDesc = "বিক্রির জন্য রাখা পণ্য ও শেয়ার, বছরের শেষে মূল্য নির্ধারণ করে"
    override val zkLivestockName = "গবাদিপশু"
    override val zkLivestockDesc = "চারণভূমিতে চরে বেড়ানো উট, গরু, ভেড়া ও ছাগল, নিসাব পূর্ণ হলে"
    override val zkCropsName = "ফসল ও ফল"
    override val zkCropsDesc = "ফসল তোলার সময়: সেচ অনুযায়ী এক-দশমাংশ বা তার অর্ধেক"
    override val zkMineralsName = "খনিজ ও গুপ্তধন"
    override val zkMineralsDesc = "গুপ্তধনের এক-পঞ্চমাংশ, খনিজের 2.5%"
    override val zkCombinedNote = "নগদ অর্থ, স্বর্ণ, রুপা ও ব্যবসায়িক পণ্য একই নিসাবে যোগ হয়, তাই এগুলো একই পৃষ্ঠায় একসঙ্গে হিসাব করা হয়।"
    override val camels = "উট"
    override val cattle = "গরু"
    override val sheepGoats = "ভেড়া ও ছাগল"
    override val unitHead = "টি"
    override val livestockDue = "গবাদিপশুর প্রদেয় যাকাত"
    override val nothingDue = "কোনো যাকাত ফরজ নয়"
    override fun livestockNisab(n: Int) = "নিসাব: $n"
    override val grazingCondition = "বছরের বেশির ভাগ সময় উন্মুক্ত চারণভূমিতে চরে"
    override val yearCondition = "পূর্ণ এক চান্দ্রবর্ষ মালিকানায় আছে"
    override val conditionsNotMet = "উভয় শর্ত পূরণ না হওয়া পর্যন্ত যাকাত নেই; বিক্রির জন্য রাখা পশু ব্যবসায়িক পণ্যের সঙ্গে হিসাব হয়।"
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "ভেড়া বা ছাগল"; "bintMakhad" -> "বিনতে মাখাদ (1 বছরের মাদি উট)"; "bintLabun" -> "বিনতে লাবুন (2 বছরের মাদি উট)"; "hiqqah" -> "হিক্কা (3 বছরের মাদি উট)"; "jadhaah" -> "জাযআ (4 বছরের মাদি উট)"; "tabi" -> "তাবি' (1 বছরের বাছুর)"; "musinnah" -> "মুসিন্না (2 বছরের গাভি)"; else -> kind }
    override val cropsWeight = "ফসলের ওজন"
    override val irrigation = "সেচ"
    override val irrRain = "খরচ ছাড়া (বৃষ্টি, ঝরনা)"
    override val irrIrrigated = "খরচসহ (পাম্প, কেনা পানি)"
    override val irrMixed = "অর্ধেক অর্ধেক"
    override val cropsDue = "ফসলের প্রদেয় যাকাত"
    override fun cropsNisab(kg: String) = "নিসাব: প্রায় $kg কেজি"
    override val zkRate = "হার"
    override val rikazValue = "গুপ্তধনের (রিকাজ) মূল্য"
    override val mineralValue = "উত্তোলিত খনিজের মূল্য"
    override val mineralsDue = "খনিজ ও গুপ্তধনের প্রদেয় যাকাত"
    override val obSkip = "এড়িয়ে যান"
    override val obNext = "পরবর্তী"
    override val obStart = "শুরু করুন"
    override val obLocationTitle = "আপনি কোথায়?"
    override val obLocationBody = "নামাজের সময় ও কিবলার দিক সঠিকভাবে হিসাব করতে আপনার অবস্থান প্রয়োজন। এটি আপনার ডিভাইসেই থাকে, কোনো সার্ভারে পাঠানো হয় না।"
    override val locationNeeded = "আপনার শহরের সঠিক নামাজের সময় দেখতে অবস্থান নির্ধারণ করুন"
    override val appGuide = "অ্যাপ নির্দেশিকা"
    override val appGuideSub = "মিজান কী কী করতে পারে তার এক ঝলক"
    override val onboarding = listOf(
        "মিজানে স্বাগতম" to "মুসলিমের প্রয়োজনীয় টুলস এক অ্যাপে: নামাজ, কিবলা, কুরআন, যাকাত ও উত্তরাধিকার। কোনো বিজ্ঞাপন নেই, আর বেশির ভাগই ইন্টারনেট ছাড়া চলে।",
        "নামাজের সময় ও কিবলা" to "আপনার শহরের সঠিক সময় ও আজানের অ্যালার্ট, কম্পাসে কিবলা, এবং হিজরি তারিখ ও ইসলামি দিবস।",
        "পবিত্র কুরআন" to "মুসহাফের পৃষ্ঠা, তাফসির ও তিলাওয়াত, ফোকাস ও হিফজ মোড, এবং দৈনিক খতম যা আপনার অগ্রগতি ও টানা দিনের হিসাব রাখে।",
        "দলিলসহ ক্যালকুলেটর" to "পাঁচ প্রকারের যাকাত, উত্তরাধিকার ও কাফফারা — প্রতিটি বিধানের সঙ্গে কুরআন, সুন্নাহ ও ফকিহদের থেকে দলিল রয়েছে।",
        "যেকোনো কিছু খুঁজুন" to "হোমের সার্চ বারে টুল, শহর, সূরা, আয়াত ও জিকির খুঁজুন, যেকোনো ভাষায়। এই পরিচিতি সেটিংস থেকে আবার দেখতে পারবেন।",
    )
    override val quranSources = "কুরআনের পাঠ (উসমানি লিপি) তানযিল প্রকল্প (tanzil.net) থেকে নেওয়া। তাফসিরে মুয়াসসার (বাদশাহ ফাহাদ কমপ্লেক্স), তাফসিরে জালালাইন ও অর্থানুবাদগুলো tanzil.net/trans থেকে নেওয়া। কুরআনের ফন্ট: Amiri Quran, SIL ওপেন ফন্ট লাইসেন্স। তিলাওয়াত: EveryAyah.com।"

    // ---------- Settings ----------
    override val settingsTitle = "সেটিংস"
    override val appearance = "থিম"
    override val themeAuto = "স্বয়ংক্রিয়"
    override val themeLight = "লাইট"
    override val themeDark = "ডার্ক"
    override val appColor = "অ্যাপের রং"
    override val accentNames = listOf("পান্না", "ফিরোজা", "নীল", "বেগুনি", "গোলাপি", "মেরুন", "কমলা", "মধু", "বাদামি", "ধূসর")
    override val language = "ভাষা"
    override val deviceLanguage = "ডিভাইসের ভাষা"
    override val use24h = "24-ঘণ্টার ঘড়ি"
    override val use24hDesc = "সময় 24-ঘণ্টা ফরম্যাটে দেখান"
    override val exactAlarm = "সঠিক সময়ে অ্যালার্ট"
    override val exactAlarmDesc = "আজানের অ্যালার্ট ঠিক সময়ে পেতে নির্ভুল অ্যালার্মের অনুমতি দিন"
    override val about = "অ্যাপ সম্পর্কে"
    override val aboutName = "মিজান — মুসলিমের টুলস"
    override val adhkarNow = "এখন"
    override val dhikrEvidence = "দলিল"
    override val dhikrSource = "উৎস"
    override val dhikrGrade = "হাদিসের মান"
    override val dhikrNarrator = "বর্ণনাকারী"
    override val dhikrVirtue = "ফজিলত"
    override val dhikrQuranRef = "পবিত্র কুরআন থেকে"
    override val dhikrDone = "আপনি এই যিকিরগুলো শেষ করেছেন। আল্লাহ কবুল করুন"
    override val dhikrTapHint = "গণনা করতে যিকিরে, আর উৎস ও মান দেখতে «দলিল»-এ ট্যাপ করুন"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, one {একবার} other {# বার}}", mapOf("n" to n), "bn")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, other {# টি যিকির}}", mapOf("n" to n), "bn")
    override val moreLanguages = "আরও ভাষা ডাউনলোড করুন"
    override val langPackHint = "ভাষাগুলো কুরআনের অনুবাদসহ মিজান ওয়েবসাইট থেকে ডাউনলোড হয়, এরপর ইন্টারনেট ছাড়াই চলে।"
    override val update = "আপডেট"
    override fun version(v: String) = "সংস্করণ $v"
    override val aboutText = "একটি বিনামূল্যের অ্যাপ, যাতে আছে একজন মুসলিমের প্রতিদিনের প্রয়োজনীয় টুলস: যাকাত, উত্তরাধিকার, নামাজের সময়, কিবলা, হিজরি তারিখ ও তাসবিহ। " +
        "বিধানগুলো জমহুর ফকিহদের মত অনুসরণ করে, নামাজের সময় জ্যোতির্বিজ্ঞানের সূত্রে হিসাব করা হয়, আর তারিখ উম্মুল কুরা ক্যালেন্ডার অনুযায়ী।"
    override val privacy = "গোপনীয়তা"
    override val privacyText = "অ্যাপটি কোনো ব্যক্তিগত তথ্য সংগ্রহ করে না এবং শুধু আপনার বেছে নেওয়া তাফসির ডাউনলোড করতে এবং আপনি চাইলে তিলাওয়াত চালাতে ইন্টারনেট ব্যবহার করে। নামাজের সময় ও কিবলা হিসাবের জন্য আপনার অবস্থান শুধু আপনার ডিভাইসেই ব্যবহৃত হয়, " +
        "আর আপনার সেটিংস ও সংখ্যাগুলো আপনার ডিভাইসেই থাকে।"
    override val disclaimer = "ফলাফলগুলো নির্দেশিকা মাত্র। নিজের দ্বীনি বিষয়ে যোগ্য আলেম ও সরকারি সংস্থার শরণাপন্ন হোন।"
}
