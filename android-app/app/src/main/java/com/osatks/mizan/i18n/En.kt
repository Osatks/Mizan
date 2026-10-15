package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

private fun en(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "$n $many"

object En : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Muslim tools"
    override val tabHome = "Home"
    override val tabPrayer = "Prayer"
    override val tabQibla = "Qibla"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Settings"
    override val back = "Back"
    override val share = "Share"
    override val shareSignature = "— Mizan app"
    override val today = "Today"
    override val tomorrow = "Tomorrow"
    override fun inDays(n: Long) = "in " + en(n, "day", "days")
    override fun daysCount(n: Long) = en(n, "day", "days")
    override val decrease = "Decrease"
    override val increase = "Increase"
    override val noLimit = "No limit"
    override val noResults = "No results"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )
    override val gregMonths = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    override val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    override val weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y AH"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y AH"
    override val hijriYearSuffix = "AH"
    override val gregYearSuffix = "CE"
    override val dayLabel = "Day"
    override val monthLabel = "Month"
    override val yearLabel = "Year"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Zakat Calculator", "Money, gold and shares with the nisab"),
        "inheritance" to Named("Inheritance Calculator", "Divide an estate by the rules of fara'id"),
        "prayer" to Named("Prayer Times", "Adhan alerts and a live countdown"),
        "qibla" to Named("Qibla Direction", "A compass driven by your phone's sensors"),
        "hijri" to Named("Hijri Date", "Convert dates, Umm al-Qura calendar"),
        "events" to Named("Islamic Events", "Ramadan, the two Eids and Arafah"),
        "tasbih" to Named("Tasbih", "Dhikr counter with ready adhkar"),
        "fitr" to Named("Zakat al-Fitr", "By the sa' or by value"),
        "age" to Named("Age Calculator", "In Hijri and Gregorian years"),
        "fidya" to Named("Fidya & Kaffarah", "Missed fasts and broken oaths"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakah charity wealth gold silver nisab money savings shares",
        "inheritance" to "inheritance estate heirs faraid will mirath",
        "prayer" to "salah salat adhan azan fajr dhuhr asr maghrib isha namaz",
        "qibla" to "kaaba mecca makkah compass direction",
        "hijri" to "calendar date convert islamic umm al-qura",
        "events" to "ramadan eid arafah ashura holidays",
        "tasbih" to "dhikr zikr counter beads adhkar",
        "fitr" to "fitrah fitra sa eid",
        "age" to "birthday birth years",
        "fidya" to "fidyah kaffara expiation oath fasting feeding",
    )
    override val toolsTitle = "Tools"
    override fun nextPrayerIn(city: String) = "Next prayer · $city"
    override val searchHint = "Search tools, cities, adhkar…"

    // ---------- Search ----------
    override val searchTitle = "Search"
    override val searchToolsGroup = "Tools"
    override val searchCitiesGroup = "Cities"
    override val searchPrayersGroup = "Today's prayer times"
    override val searchEventsGroup = "Events"
    override val searchDhikrGroup = "Adhkar"
    override val searchSettingsGroup = "Settings"
    override val searchEmpty = "Search tools, cities, adhkar, events and settings"
    override val setAsMyCity = "Use for prayer times"
    override fun searchNoResults(q: String) = "No results for “$q”"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Fajr", "sunrise" to "Sunrise", "dhuhr" to "Dhuhr",
        "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha"
    )
    override val prayerTitle = "Prayer Times"
    override fun nextPrayerLabel(name: String) = "Next prayer: $name"
    override fun atTime(time: String) = "at $time"
    override val prevDay = "Previous day"
    override val nextDay = "Next day"
    override val monthTable = "Monthly timetable"
    override val hideMonthTable = "Hide timetable"
    override fun monthTableTitle(m: Int, y: Int) = "${gregMonths[m - 1]} $y timetable"
    override val colDay = "Day"
    override val locationAndMethod = "Location & calculation"
    override val useMyLocation = "Use my current location"
    override val locating = "Finding your location…"
    override val orPickCity = "Or choose a city"
    override val pickCity = "Choose a city"
    override val searchCity = "Search for a city"
    override val myLocation = "My location"
    override val calcMethod = "Calculation method"
    override val methods = mapOf(
        "makkah" to "Umm al-Qura (Saudi Arabia)",
        "mwl" to "Muslim World League",
        "egypt" to "Egyptian General Authority of Survey",
        "dubai" to "UAE (Dubai)",
        "kuwait" to "Kuwait",
        "qatar" to "Qatar",
        "jordan" to "Jordan & Palestine",
        "morocco" to "Morocco",
        "algeria" to "Algeria",
        "tunisia" to "Tunisia",
        "karachi" to "University of Islamic Sciences, Karachi",
        "turkey" to "Diyanet (Türkiye)",
        "tehran" to "Institute of Geophysics, University of Tehran",
        "indonesia" to "Ministry of Religious Affairs (Indonesia)",
        "malaysia" to "JAKIM (Malaysia)",
        "singapore" to "MUIS (Singapore)",
        "russia" to "Spiritual Administration of Muslims of Russia",
        "isna" to "North America (ISNA)",
        "france" to "Union of Islamic Organisations of France",
    )
    override val asrMethod = "Asr calculation"
    override val asrStandard = "Majority (Shafi'i, Maliki, Hanbali)"
    override val asrHanafi = "Hanafi"
    override val latLng = "Latitude / longitude"
    override val adhanAlert = "Adhan alert"
    override val adhanAlertDesc = "A notification when each prayer time begins"
    override val timesMayDiffer = "Times may differ by a minute or two from your country's official calendar."
    override val locationFound = "Location found ✓"
    override val locationFailed = "Couldn't get your location. Make sure location services are on."
    override val locationDenied = "Location permission was not granted. Choose your city from the list."
    override val cities = mapOf(
        "makkah" to "Makkah", "madinah" to "Madinah", "riyadh" to "Riyadh", "jeddah" to "Jeddah",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubai", "abudhabi" to "Abu Dhabi", "sharjah" to "Sharjah",
        "kuwait" to "Kuwait City", "doha" to "Doha", "manama" to "Manama", "muscat" to "Muscat",
        "cairo" to "Cairo", "alexandria" to "Alexandria", "aswan" to "Aswan", "amman" to "Amman",
        "jerusalem" to "Jerusalem (Al-Quds)", "gaza" to "Gaza", "beirut" to "Beirut", "damascus" to "Damascus", "aleppo" to "Aleppo",
        "baghdad" to "Baghdad", "basra" to "Basra", "erbil" to "Erbil", "sanaa" to "Sana'a", "aden" to "Aden",
        "khartoum" to "Khartoum", "tripoli" to "Tripoli", "benghazi" to "Benghazi", "tunis" to "Tunis",
        "algiers" to "Algiers", "oran" to "Oran", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakesh", "nouakchott" to "Nouakchott", "mogadishu" to "Mogadishu", "djibouti" to "Djibouti",
        "istanbul" to "Istanbul", "ankara" to "Ankara", "karachi" to "Karachi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Mumbai", "hyderabad" to "Hyderabad",
        "dhaka" to "Dhaka", "chittagong" to "Chittagong", "kabul" to "Kabul", "tehran" to "Tehran", "mashhad" to "Mashhad",
        "tashkent" to "Tashkent", "baku" to "Baku", "almaty" to "Almaty", "moscow" to "Moscow", "kazan" to "Kazan",
        "grozny" to "Grozny", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapore",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Jakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "London", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marseille", "lyon" to "Lyon", "brussels" to "Brussels", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hamburg", "cologne" to "Cologne", "munich" to "Munich",
        "vienna" to "Vienna", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Chicago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sydney",
    )
    override val palestine = "Palestine"

    // ---------- Notifications ----------
    override val channelName = "Prayer times"
    override val channelDesc = "An alert when each prayer time begins"
    override fun prayerNowTitle(prayer: String) = "It's time for $prayer"
    override fun prayerNowText(city: String) = "$city — come to prayer"

    // ---------- Qibla ----------
    override val qiblaTitle = "Qibla Direction"
    override val noCompass = "Your device has no compass. Point it by hand using the angle below."
    override val facingQibla = "✓ You are facing the Qibla"
    override fun turnRight(deg: Int) = "Turn your phone right by $deg°"
    override fun turnLeft(deg: Int) = "Turn your phone left by $deg°"
    override val qiblaAngle = "Qibla angle from true north"
    override fun towards(dir: String) = "Towards the $dir"
    override val directions = listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
    override val location = "Location"
    override val distanceToKaaba = "Distance to the Kaaba"
    override val phoneHeading = "Phone heading"
    override val compassLowAccuracy = "Low compass accuracy: move your phone in a figure 8 to calibrate it, away from metal and electronics."
    override val qiblaTip = "To change your location, use the Prayer Times page. Hold the phone flat and away from metal for an accurate reading."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Digital Tasbih"
    override val tasbihSubtitle = "Tap the circle to count"
    override val modeFree = "Free dhikr"
    override val modeAfterPrayer = "After prayer"
    override fun ofGoal(n: Int) = "of $n"
    override val nextDhikr = "Move on to the next dhikr"
    override val completedHundred = "May Allah accept it from you ✓ You completed the hundred"
    override fun completedN(n: Int) = "Completed $n ✓"
    override val reset = "Reset"
    override val undo = "Undo"
    override val chooseDhikr = "Choose a dhikr"
    override val targetCount = "Target count"
    override val yourStats = "Your stats"
    override val roundsDone = "rounds done"
    override val todayCount = "today"
    override val totalCount = "all time"
    override val dhikrLabels = listOf(
        "SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah",
        "SubhanAllahi wa bihamdih", "SubhanAllahil-Azim", "La hawla wa la quwwata illa billah", "Salawat on the Prophet ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Glory be to Allah",
        "All praise is for Allah",
        "Allah is the Greatest",
        "There is no god but Allah",
        "I seek Allah's forgiveness",
        "Glory be to Allah and praise be to Him",
        "Glory be to Allah, the Magnificent",
        "There is no might nor power except by Allah",
        "O Allah, send blessings upon Muhammad",
    )
    override val tamamMeaning = "There is no god but Allah alone, with no partner. His is the dominion and His is the praise, and He has power over all things."
    override val tapToCount = "Tap to count"

    // ---------- Zakat ----------
    override val zakatTitle = "Zakat Calculator"
    override val zakatSubtitle = "2.5% of wealth that reaches the nisab and is held for a lunar year"
    override val partCash = "Cash & balances"
    override val partReceivables = "Recoverable loans"
    override val partGold = "Gold"
    override val partSilver = "Silver"
    override val partStocks = "Shares"
    override val partTrade = "Trade goods"
    override val enterGoldPrice = "Enter the gold price to work out the nisab"
    override val enterSilverPrice = "Enter the silver price to work out the nisab"
    override val nisabReached = "✓ Your wealth reached the nisab — zakat is due"
    override val belowNisab = "Below the nisab — no zakat is due"
    override val enterAssets = "Enter your assets"
    override val zakatDue = "Zakat due"
    override val totalAssets = "Total zakatable assets"
    override val deductedDebts = "Debts deducted"
    override val netBase = "Net zakatable wealth"
    override val nisabValue = "Nisab value"
    override val approxEquals = "Roughly equal to"
    override fun goldGrams24(g: String) = "$g g of 24K gold"
    override val shareResult = "Share the result"
    override fun zakatShareText(zakat: String, base: String) = "My zakat due: $zakat (zakatable wealth $base)"
    override val settingsAndPrices = "Settings & prices"
    override val currency = "Currency"
    override val nisabBasis = "Nisab used for money"
    override val nisabGold = "Gold (85 g)"
    override val nisabSilver = "Silver (595 g)"
    override val goldPrice24 = "Price of 1 g of 24K gold"
    override val goldPriceHint = "From a jeweller or a trusted price site in your country"
    override val silverPrice = "Price of 1 g of silver (optional)"
    override val cashSection = "Cash & balances"
    override val cashInHand = "Cash in hand"
    override val bankBalances = "Bank and wallet balances"
    override val receivablesField = "Loans owed to you that you expect back"
    override val otherMoney = "Other money"
    override val goldSilverSection = "Gold & silver (in grams)"
    override fun goldKarat(k: Int) = "${k}K gold"
    override val pureSilver = "Pure silver"
    override val includeJewelry = "Include jewellery worn for adornment (Hanafi view, and the more cautious)"
    override val jewelryField = "Of which, jewellery in use (21K equivalent)"
    override val jewelryHint = "Excluded under the majority view that jewellery for personal use has no zakat"
    override val investSection = "Investments & trade"
    override val stocksTrade = "Shares held for trading (market value)"
    override val stocksLong = "Long-term shares (zakatable portion)"
    override val tradeGoods = "Trade goods (stock value)"
    override val deductSection = "Deductions"
    override val debtsField = "Debts you owe that fall due this year"
    override val debtsHint = "Only instalments due within the zakat year"
    override val breakdownTitle = "Zakat by type of wealth"
    override val zakatNote = "Your data stays on your device. Allah names the eight recipients of zakat in Surah at-Tawbah (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat al-Fitr"
    override val fitrSubtitle = "Given before the Eid prayer"
    override val foodAmountDue = "Amount of food to give"
    override val totalCashValue = "Total cash value"
    override fun forPeople(n: Int) = if (n == 1) "for 1 person" else "for $n people"
    override val peopleCount = "People you pay for (including yourself)"
    override val modeFood = "Food (sa')"
    override val modeMoney = "Cash value"
    override val foodType = "Type of food"
    override val foods = mapOf(
        "rice" to "Rice", "riceSafe" to "Rice (more cautious)", "wheat" to "Wheat",
        "dates" to "Dates", "barley" to "Barley", "other" to "Other amount…",
    )
    override fun approxKg(kg: String) = "about $kg kg"
    override val saWeight = "Weight of one sa' in kg"
    override val kgPrice = "Price per kg (optional)"
    override val kgPriceHint = "To estimate what the food will cost"
    override val saPerPerson = "One sa' per person"
    override val approxCost = "Estimated cost"
    override val valuePerPerson = "Zakat al-Fitr value per person"
    override val valuePerPersonHint = "Announced each year by the official body in your country"
    override val valueNote = "Paying the value is the Hanafi view; the majority give food."
    override val fitrTimingNote = "When to give: before the Eid prayer. It may be given a day or two early, but must not be delayed past the prayer without excuse. It goes to the poor and needy."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidya & Kaffarah"
    override val fidyaSubtitle = "Feeding amounts in kilograms and value"
    override val tabFidya = "Fasting fidya"
    override val tabOath = "Oath"
    override val tabIntercourse = "Intercourse"
    override val fidyaResult = "Fidya for missed fasts"
    override fun fidyaSub(days: Int) = "For ${en(days.toLong(), "day", "days")} — one poor person per day"
    override val fidyaNote = "For someone permanently unable to fast, such as the elderly or the chronically ill. One poor person may be fed for all the days, or several people."
    override val oathResult = "Kaffarah for a broken oath"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths kaffarahs × 10 poor people" else "Feed 10 poor people"
    override val oathAlternative = "If unable"
    override fun fastDays(n: Int) = "Fast " + en(n.toLong(), "day", "days")
    override val oathNote = "Choose between feeding ten poor people, clothing them, or freeing a slave; whoever cannot, fasts three days (Surah al-Ma'idah 5:89)."
    override val intercourseResult = "Kaffarah for intercourse during a Ramadan day"
    override val intercourseSub = "Feed 60 poor people if unable to fast"
    override val ifStartToday = "If you start fasting today, you finish on"
    override val twoMonthsDuration = "Length of the two lunar months"
    override val intercourseNote = "In order: free a slave; if not possible, fast two consecutive months; if not able, feed sixty poor people — along with making up the day and repenting."
    override val daysField = "Number of days"
    override val oathsField = "Number of kaffarahs"
    override val feedMeasure = "Food per poor person"
    override val muddOption = "One mudd — about 0.75 kg (Maliki & Shafi'i)"
    override val halfSaOption = "Half a sa' — about 1.5 kg (contemporary fatwas)"
    override val poorCount = "Poor people / meals"
    override val approxValue = "Approximate value"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Hijri Date"
    override val hijriSubtitle = "Umm al-Qura calendar"
    override val correspondingHijri = "Matching Hijri date"
    override val correspondingGreg = "Matching Gregorian date"
    override val enterGregYear = "Enter a year between 1882 and 2174"
    override val enterHijriYear = "Enter a Hijri year between 1300 and 1600"
    override val gregToHijri = "Gregorian → Hijri"
    override val hijriToGreg = "Hijri → Gregorian"
    override val todayDate = "Today's date"
    override val prevMonth = "Previous month"
    override val nextMonth = "Next month"
    override val calendarHint = "The large number is Hijri, the small one Gregorian. Tap a day to convert it."
    override val moonNote = "The date may differ by a day in some countries because of moon sighting."

    // ---------- Age ----------
    override val ageTitle = "Age Calculator"
    override val ageSubtitle = "In Hijri and Gregorian years"
    override val ageGregorian = "Your age (Gregorian)"
    override fun ymd(y: Int, m: Int, d: Int) = "${en(y.toLong(), "year", "years")} ${en(m.toLong(), "month", "months")} ${en(d.toLong(), "day", "days")}"
    override fun inHijri(age: String) = "Hijri: $age"
    override val birthGreg = "Date of birth (Gregorian)"
    override val birthHijri = "Date of birth (Hijri)"
    override val birthWeekday = "Day of birth"
    override val nextBirthday = "Next birthday"
    override val birthdayToday = "Today 🎉"
    override val tileDays = "days"
    override val tileWeeks = "weeks"
    override val tileHours = "hours"
    override val yourAge = "Your age"
    override val birthAfterToday = "The date of birth is after today"
    override val enterBirth = "Enter your date of birth"
    override val birthDate = "Date of birth"
    override val gregorian = "Gregorian"
    override val hijri = "Hijri"

    // ---------- Islamic events ----------
    override val eventsTitle = "Islamic Events"
    override val eventsSubtitle = "Countdown by the Umm al-Qura calendar"
    override val events = mapOf(
        "ramadan" to Named("Ramadan", "The month of fasting, night prayer and reciting the Quran."),
        "lastTen" to Named("The last ten nights", "Among them is Laylat al-Qadr, better than a thousand months."),
        "eidFitr" to Named("Eid al-Fitr", "Zakat al-Fitr is given before its prayer."),
        "shawwal" to Named("Six days of Shawwal", "Whoever fasts Ramadan and follows it with six days of Shawwal, it is as if he fasted for a lifetime (Muslim)."),
        "dhulHijjah" to Named("First ten days of Dhu al-Hijjah", "The best days of the world; good deeds and takbir are recommended."),
        "arafah" to Named("Day of Arafah", "Fasting it expiates the past year and the coming one, for those not on Hajj (Muslim)."),
        "eidAdha" to Named("Eid al-Adha", "The day of sacrifice, followed by the three days of Tashriq."),
        "newYear" to Named("Islamic New Year", "The start of a new Hijri year."),
        "ashura" to Named("Day of Ashura", "Fasting it expiates the past year (Muslim); fasting the 9th with it is recommended."),
    )
    override val unitDays = "days"
    override val unitHours = "hours"
    override val unitMinutes = "min"
    override val unitSeconds = "sec"
    override val eventsNote = "Dates are approximate (Umm al-Qura) and may differ by a day with the moon sighting in your country."

    // ---------- Inheritance ----------
    override val inhTitle = "Inheritance Calculator"
    override val inhSubtitle = "Divide an estate by the rules of fara'id"
    override val aslLabel = "Base of the case (asl)"
    override val awlCase = "Case with 'awl (shares exceed the estate)"
    override val raddCase = "Case with radd (surplus returned)"
    override val adilCase = "Balanced case"
    override fun tashihFrom(n: Long) = "Corrected to $n shares"
    override val sharesTitle = "Each heir's share"
    override fun blockedBy(by: String) = "Excluded by $by"
    override fun perPerson(v: String) = "each $v"
    override val shareDivision = "Share the division"
    override fun divisionShareText(list: String) = "Estate division: $list"
    override val listSeparator = ", "
    override val addHeirs = "Add the heirs from the list below"
    override val deceasedAndEstate = "Deceased & estate"
    override val deceasedMale = "Deceased is male"
    override val deceasedFemale = "Deceased is female"
    override val estateTotal = "Total estate"
    override val funeralCosts = "Funeral and burial costs"
    override val deceasedDebts = "Debts of the deceased"
    override val bequest = "Bequest (to a non-heir)"
    override val bequestHint = "No more than a third of what remains after debts, unless the heirs agree"
    override val netEstate = "Net estate to divide"
    override val bequestCapped = "(bequest limited to one third)"
    override val newCase = "Start a new case"
    override val inhDisclaimer = "The result is a guide based on the majority view. It does not replace a Sharia court or a scholar of fara'id."
    override val heirs = mapOf(
        "husband" to "Husband", "wife" to "Wife", "father" to "Father", "mother" to "Mother",
        "gf" to "Paternal grandfather", "gmm" to "Maternal grandmother", "gmf" to "Paternal grandmother",
        "son" to "Son", "daughter" to "Daughter", "gson" to "Son's son", "gdau" to "Son's daughter",
        "fb" to "Full brother", "fs" to "Full sister", "pb" to "Paternal half-brother", "ps" to "Paternal half-sister",
        "mb" to "Maternal half-brother", "ms" to "Maternal half-sister", "fnep" to "Full brother's son", "pnep" to "Paternal half-brother's son",
        "fun" to "Full paternal uncle", "pun" to "Paternal half-uncle", "fcous" to "Full paternal uncle's son", "pcous" to "Paternal half-uncle's son",
    )
    override val heirGroups = mapOf(
        "spouses" to "Spouses", "ascendants" to "Parents & grandparents", "descendants" to "Children & grandchildren",
        "siblings" to "Brothers & sisters", "distant" to "Distant male relatives",
    )
    override val blockers = mapOf(
        "gf" to "the grandfather",
        "twoDaughters" to "the two daughters (two thirds complete)",
        "descendant" to "an inheriting descendant",
        "fbAfterMuadda" to "the full brother (after mu'addah)",
        "fsTookRest" to "the full sister taking the remainder",
        "gfAndSiblings" to "the grandfather and siblings",
        "twoFullSisters" to "the two full sisters",
        "fsGhayr" to "the full sister (residuary with daughters)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Fixed share"
        ShareType.FARD_TASIB -> "Fixed share + residue"
        ShareType.FARD_TAKMILA -> "Fixed share (completing two thirds)"
        ShareType.TASIB -> "Residuary"
        ShareType.TASIB_BIL_GHAYR -> "Residuary through a brother"
        ShareType.TASIB_MAA_GHAYR -> "Residuary with daughters"
        ShareType.TASIB_MAA_JADD -> "Residuary with the grandfather"
        ShareType.FARD_THEN_MUQASAMA -> "Fixed share, then shared"
        ShareType.JADD_MUQASAMA -> "With siblings: sharing"
        ShareType.JADD_THIRD_ESTATE -> "With siblings: a third of the estate"
        ShareType.JADD_THIRD_REST -> "With siblings: a third of the remainder"
        ShareType.JADD_SIXTH -> "With siblings: a sixth of the estate"
    } + if (radd) " + radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Maternal half-siblings share one third equally, male and female alike."
        InhNote.Umariyya -> "This is one of the two 'Umariyyah cases: the mother takes a third of what remains after the spouse's share."
        InhNote.GrandmothersShare -> "The two grandmothers share one sixth."
        InhNote.FullSistersGhayr -> "Full sisters become residuaries together with the daughters."
        InhNote.Akdariyya -> "This is the Akdariyyah case: the sister is given a half and the grandfather a sixth, raising the case to 9; their shares are then pooled and split two to one, giving 27."
        is InhNote.JaddRule -> "With siblings, the grandfather takes whichever is best for him: sharing, or " +
            (if (n.otherFard) "a third of the remainder, or a sixth of the estate" else "a third of the estate") +
            " — the majority view (Maliki, Shafi'i, Hanbali). Abu Hanifa held that the grandfather excludes the siblings like the father."
        InhNote.Muadda -> "Paternal half-siblings were counted against the grandfather (mu'addah), then their share returns to the full siblings where needed."
        is InhNote.NothingLeft -> "Nothing remains after the fixed shares, so the residuary heir (${nameOf(n.winner)}) receives nothing."
        InhNote.Mushtaraka -> "This is the Mushtarakah (Himariyyah) case: Malikis and Shafi'is let the full siblings share the third with the maternal siblings; the result here follows the Hanafi and Hanbali view."
        is InhNote.Awl -> "The fixed shares exceeded the base, so the case was raised from ${n.asl} to ${n.awl} ('awl) and every share shrinks in proportion."
        InhNote.Radd -> "There is no residuary heir, so the surplus is returned to the sharers in proportion to their shares, except the spouses."
        InhNote.SpouseOnlyRadd -> "The only heir is the spouse: many contemporary scholars and several family laws return the remainder to them; earlier scholars gave it to distant kindred, then the public treasury."
        InhNote.NoHeirs -> "There is no sharer or residuary heir; the estate goes to distant kindred, then the public treasury. Refer to the competent court."
    }

    // ---------- Settings ----------
    override val settingsTitle = "Settings"
    override val appearance = "Appearance"
    override val themeAuto = "Auto"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val language = "Language"
    override val deviceLanguage = "Device language"
    override val use24h = "24-hour clock"
    override val use24hDesc = "Show times in 24-hour format"
    override val exactAlarm = "Exact-time alerts"
    override val exactAlarmDesc = "Allow exact alarms so the adhan alert arrives on time"
    override val about = "About"
    override val aboutName = "Mizan — Muslim tools"
    override fun version(v: String) = "Version $v"
    override val aboutText = "A free app with the tools a Muslim needs every day: zakat, inheritance, prayer times, qibla, the Hijri date and a tasbih. " +
        "Rulings follow the majority of jurists, prayer times use astronomical formulas, and dates follow the Umm al-Qura calendar."
    override val privacy = "Privacy"
    override val privacyText = "The app collects no personal data and needs no internet connection. Your location is used only on your device to compute prayer times and the qibla, " +
        "and your settings and numbers stay on your device."
    override val disclaimer = "Results are a guide. For your own religious questions, refer to qualified scholars and official bodies."
}
