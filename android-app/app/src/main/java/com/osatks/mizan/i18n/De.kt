package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

private fun de(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "$n $many"

object De : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Tools für Muslime"
    override val tabHome = "Start"
    override val tabPrayer = "Gebet"
    override val tabQibla = "Qibla"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Optionen"
    override val back = "Zurück"
    override val share = "Teilen"
    override val shareSignature = "— Mizan-App"
    override val today = "Heute"
    override val tomorrow = "Morgen"
    override fun inDays(n: Long) = if (n == 1L) "in 1 Tag" else "in $n Tagen"
    override fun daysCount(n: Long) = de(n, "Tag", "Tage")
    override val decrease = "Verringern"
    override val increase = "Erhöhen"
    override val noLimit = "Unbegrenzt"
    override val noResults = "Keine Ergebnisse"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Achir", "Dschumada al-Ula", "Dschumada al-Achira",
        "Radschab", "Scha'ban", "Ramadan", "Schawwal", "Dhu l-Qa'da", "Dhu l-Hiddscha"
    )
    override val gregMonths = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"
    )
    override val weekdays = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    override val weekdaysShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d. ${hijriMonths[m - 1]} $y n. H."
    override fun gregDate(d: Int, m: Int, y: Int) = "$d. ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y n. H."
    override val hijriYearSuffix = "n. H."
    override val gregYearSuffix = "n. Chr."
    override val dayLabel = "Tag"
    override val monthLabel = "Monat"
    override val yearLabel = "Jahr"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Zakat-Rechner", "Geld, Gold und Aktien mit Nisab"),
        "inheritance" to Named("Erbrechner", "Nachlassteilung nach den Regeln der Fara'id"),
        "prayer" to Named("Gebetszeiten", "Adhan-Hinweis und Live-Countdown"),
        "qibla" to Named("Qibla-Richtung", "Kompass über die Sensoren deines Handys"),
        "hijri" to Named("Hidschra-Datum", "Datumsumrechnung, Umm-al-Qura-Kalender"),
        "events" to Named("Islamische Anlässe", "Ramadan, die beiden Eids und Arafa"),
        "tasbih" to Named("Tasbih", "Dhikr-Zähler mit fertigen Adhkar"),
        "fitr" to Named("Zakat al-Fitr", "Nach Sa' oder als Geldwert"),
        "age" to Named("Altersrechner", "In Hidschra- und gregorianischen Jahren"),
        "fidya" to Named("Fidya & Kaffara", "Versäumtes Fasten und gebrochene Schwüre"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakat zakah sakat almosen spende vermögen gold silber nisab geld ersparnisse aktien",
        "inheritance" to "erbe erbschaft erben nachlass erbteil erbrecht faraid mirath testament vermächtnis",
        "prayer" to "gebet gebetszeit gebetszeiten salah salat namaz adhan azan fadschr fajr dhuhr zuhr asr maghrib ischa isha",
        "qibla" to "kibla kaaba mekka makkah kompass richtung gebetsrichtung",
        "hijri" to "kalender datum umrechnen islamisch hidschri hijri hidschra umm al-qura",
        "events" to "ramadan eid id fest bayram feiertage arafa arafat aschura ashura",
        "tasbih" to "dhikr zikr zähler gebetskette misbaha adhkar",
        "fitr" to "fitra fitrah fitr sa eid fastenbrechen",
        "age" to "alter geburtstag geburt jahre",
        "fidya" to "fidyah kaffara kaffarah sühne schwur eid fasten speisung",
    )
    override val toolsTitle = "Tools"
    override fun nextPrayerIn(city: String) = "Nächstes Gebet · $city"
    override val searchHint = "Tools, Städte, Adhkar suchen…"

    // ---------- Search ----------
    override val searchTitle = "Suche"
    override val searchToolsGroup = "Tools"
    override val searchCitiesGroup = "Städte"
    override val searchPrayersGroup = "Heutige Gebetszeiten"
    override val searchEventsGroup = "Anlässe"
    override val searchDhikrGroup = "Adhkar"
    override val searchSettingsGroup = "Einstellungen"
    override val searchEmpty = "Suche nach Tools, Städten, Adhkar, Anlässen und Einstellungen"
    override val setAsMyCity = "Für Gebetszeiten verwenden"
    override fun searchNoResults(q: String) = "Keine Ergebnisse für „$q“"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Fadschr", "sunrise" to "Sonnenaufgang", "dhuhr" to "Dhuhr",
        "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Ischa"
    )
    override val prayerTitle = "Gebetszeiten"
    override fun nextPrayerLabel(name: String) = "Nächstes Gebet: $name"
    override fun atTime(time: String) = "um $time"
    override val prevDay = "Vorheriger Tag"
    override val nextDay = "Nächster Tag"
    override val monthTable = "Monatsübersicht"
    override val hideMonthTable = "Übersicht ausblenden"
    override fun monthTableTitle(m: Int, y: Int) = "Gebetszeiten ${gregMonths[m - 1]} $y"
    override val colDay = "Tag"
    override val locationAndMethod = "Standort & Berechnung"
    override val useMyLocation = "Aktuellen Standort verwenden"
    override val locating = "Standort wird ermittelt…"
    override val orPickCity = "Oder eine Stadt wählen"
    override val pickCity = "Stadt wählen"
    override val searchCity = "Stadt suchen"
    override val myLocation = "Mein Standort"
    override val calcMethod = "Berechnungsmethode"
    override val methods = mapOf(
        "makkah" to "Umm al-Qura (Saudi-Arabien)",
        "mwl" to "Islamische Weltliga",
        "egypt" to "Ägyptische Generalbehörde für Vermessung",
        "dubai" to "VAE (Dubai)",
        "kuwait" to "Kuwait",
        "qatar" to "Katar",
        "jordan" to "Jordanien & Palästina",
        "morocco" to "Marokko",
        "algeria" to "Algerien",
        "tunisia" to "Tunesien",
        "karachi" to "Universität für Islamische Wissenschaften, Karatschi",
        "turkey" to "Diyanet (Türkei)",
        "tehran" to "Institut für Geophysik, Universität Teheran",
        "indonesia" to "Religionsministerium (Indonesien)",
        "malaysia" to "JAKIM (Malaysia)",
        "singapore" to "MUIS (Singapur)",
        "russia" to "Geistliche Verwaltung der Muslime Russlands",
        "isna" to "Nordamerika (ISNA)",
        "france" to "Union der Islamischen Organisationen Frankreichs",
    )
    override val asrMethod = "Asr-Berechnung"
    override val asrStandard = "Mehrheit (Schafiiten, Malikiten, Hanbaliten)"
    override val asrHanafi = "Hanafitisch"
    override val latLng = "Breitengrad / Längengrad"
    override val adhanAlert = "Adhan-Hinweis"
    override val adhanAlertDesc = "Eine Benachrichtigung zu Beginn jeder Gebetszeit"
    override val timesMayDiffer = "Die Zeiten können um ein bis zwei Minuten vom offiziellen Kalender deines Landes abweichen."
    override val locationFound = "Standort gefunden ✓"
    override val locationFailed = "Standort konnte nicht ermittelt werden. Prüfe, ob die Standortdienste aktiviert sind."
    override val locationDenied = "Die Standortberechtigung wurde nicht erteilt. Wähle deine Stadt aus der Liste."
    override val cities = mapOf(
        "makkah" to "Mekka", "madinah" to "Medina", "riyadh" to "Riad", "jeddah" to "Dschidda",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubai", "abudhabi" to "Abu Dhabi", "sharjah" to "Schardscha",
        "kuwait" to "Kuwait-Stadt", "doha" to "Doha", "manama" to "Manama", "muscat" to "Maskat",
        "cairo" to "Kairo", "alexandria" to "Alexandria", "aswan" to "Assuan", "amman" to "Amman",
        "jerusalem" to "Jerusalem (al-Quds)", "gaza" to "Gaza", "beirut" to "Beirut", "damascus" to "Damaskus", "aleppo" to "Aleppo",
        "baghdad" to "Bagdad", "basra" to "Basra", "erbil" to "Erbil", "sanaa" to "Sanaa", "aden" to "Aden",
        "khartoum" to "Khartum", "tripoli" to "Tripolis", "benghazi" to "Bengasi", "tunis" to "Tunis",
        "algiers" to "Algier", "oran" to "Oran", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakesch", "nouakchott" to "Nouakchott", "mogadishu" to "Mogadischu", "djibouti" to "Dschibuti",
        "istanbul" to "Istanbul", "ankara" to "Ankara", "karachi" to "Karatschi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Mumbai", "hyderabad" to "Hyderabad",
        "dhaka" to "Dhaka", "chittagong" to "Chittagong", "kabul" to "Kabul", "tehran" to "Teheran", "mashhad" to "Maschhad",
        "tashkent" to "Taschkent", "baku" to "Baku", "almaty" to "Almaty", "moscow" to "Moskau", "kazan" to "Kasan",
        "grozny" to "Grosny", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapur",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Jakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "London", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marseille", "lyon" to "Lyon", "brussels" to "Brüssel", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hamburg", "cologne" to "Köln", "munich" to "München",
        "vienna" to "Wien", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Chicago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sydney",
    )
    override val palestine = "Palästina"

    // ---------- Notifications ----------
    override val channelName = "Gebetszeiten"
    override val channelDesc = "Ein Hinweis zu Beginn jeder Gebetszeit"
    override fun prayerNowTitle(prayer: String) = "Es ist Zeit für $prayer"
    override fun prayerNowText(city: String) = "$city — auf zum Gebet"

    // ---------- Qibla ----------
    override val qiblaTitle = "Qibla-Richtung"
    override val noCompass = "Dein Gerät hat keinen Kompass. Richte es anhand des Winkels unten von Hand aus."
    override val facingQibla = "✓ Du bist zur Qibla ausgerichtet"
    override fun turnRight(deg: Int) = "Drehe dein Handy um $deg° nach rechts"
    override fun turnLeft(deg: Int) = "Drehe dein Handy um $deg° nach links"
    override val qiblaAngle = "Qibla-Winkel ab geografischem Nord"
    override fun towards(dir: String) = "Richtung $dir"
    override val directions = listOf("Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten", "Westen", "Nordwesten")
    override val location = "Standort"
    override val distanceToKaaba = "Entfernung zur Kaaba"
    override val phoneHeading = "Ausrichtung des Handys"
    override val compassLowAccuracy = "Geringe Kompassgenauigkeit: Bewege dein Handy in Form einer 8, um den Kompass zu kalibrieren – fern von Metall und Elektronik."
    override val qiblaTip = "Den Standort änderst du auf der Seite „Gebetszeiten“. Halte das Handy flach und fern von Metall, um genau zu messen."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Tasbih-Zähler"
    override val tasbihSubtitle = "Tippe auf den Kreis, um zu zählen"
    override val modeFree = "Freier Dhikr"
    override val modeAfterPrayer = "Nach dem Gebet"
    override fun ofGoal(n: Int) = "von $n"
    override val nextDhikr = "Weiter zum nächsten Dhikr"
    override val completedHundred = "Möge Allah es von dir annehmen ✓ Du hast die Hundert vollendet"
    override fun completedN(n: Int) = "$n vollendet ✓"
    override val reset = "Zurücksetzen"
    override val undo = "Rückgängig"
    override val chooseDhikr = "Dhikr wählen"
    override val targetCount = "Zielanzahl"
    override val yourStats = "Deine Statistik"
    override val roundsDone = "Runden"
    override val todayCount = "heute"
    override val totalCount = "insgesamt"
    override val dhikrLabels = listOf(
        "Subhanallah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah",
        "Subhanallahi wa bihamdihi", "Subhanallahil-Azim", "La hawla wa la quwwata illa billah", "Salawat auf den Propheten ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Gepriesen sei Allah",
        "Alles Lob gebührt Allah",
        "Allah ist der Größte",
        "Es gibt keinen Gott außer Allah",
        "Ich bitte Allah um Vergebung",
        "Gepriesen sei Allah, und Ihm sei Lob",
        "Gepriesen sei Allah, der Gewaltige",
        "Es gibt keine Macht und keine Kraft außer durch Allah",
        "O Allah, segne Muhammad",
    )
    override val tamamMeaning = "Es gibt keinen Gott außer Allah allein, Er hat keinen Teilhaber. Ihm gehört die Herrschaft und Ihm gebührt das Lob, und Er hat Macht über alle Dinge."
    override val tapToCount = "Tippen zum Zählen"

    // ---------- Zakat ----------
    override val zakatTitle = "Zakat-Rechner"
    override val zakatSubtitle = "2,5 % des Vermögens, das den Nisab erreicht und ein Mondjahr lang besessen wird"
    override val partCash = "Bargeld & Guthaben"
    override val partReceivables = "Einbringliche Forderungen"
    override val partGold = "Gold"
    override val partSilver = "Silber"
    override val partStocks = "Aktien"
    override val partTrade = "Handelswaren"
    override val enterGoldPrice = "Gib den Goldpreis ein, um den Nisab zu berechnen"
    override val enterSilverPrice = "Gib den Silberpreis ein, um den Nisab zu berechnen"
    override val nisabReached = "✓ Dein Vermögen erreicht den Nisab – Zakat ist fällig"
    override val belowNisab = "Unter dem Nisab – keine Zakat fällig"
    override val enterAssets = "Gib dein Vermögen ein"
    override val zakatDue = "Fällige Zakat"
    override val totalAssets = "Zakatpflichtiges Gesamtvermögen"
    override val deductedDebts = "Abgezogene Schulden"
    override val netBase = "Zakatpflichtiges Nettovermögen"
    override val nisabValue = "Nisab-Wert"
    override val approxEquals = "Entspricht etwa"
    override fun goldGrams24(g: String) = "$g g Gold (24 Karat)"
    override val shareResult = "Ergebnis teilen"
    override fun zakatShareText(zakat: String, base: String) = "Meine fällige Zakat: $zakat (zakatpflichtiges Vermögen $base)"
    override val settingsAndPrices = "Einstellungen & Preise"
    override val currency = "Währung"
    override val nisabBasis = "Nisab für Geld"
    override val nisabGold = "Gold (85 g)"
    override val nisabSilver = "Silber (595 g)"
    override val goldPrice24 = "Preis für 1 g Gold (24 Karat)"
    override val goldPriceHint = "Beim Juwelier oder auf einer seriösen Preisseite in deinem Land"
    override val silverPrice = "Preis für 1 g Silber (optional)"
    override val cashSection = "Bargeld & Guthaben"
    override val cashInHand = "Bargeld"
    override val bankBalances = "Bank- und Wallet-Guthaben"
    override val receivablesField = "Dir geschuldete Darlehen, die du zurückerwartest"
    override val otherMoney = "Sonstiges Geld"
    override val goldSilverSection = "Gold & Silber (in Gramm)"
    override fun goldKarat(k: Int) = "Gold $k Karat"
    override val pureSilver = "Reines Silber"
    override val includeJewelry = "Getragenen Schmuck einbeziehen (hanafitische und vorsichtigere Ansicht)"
    override val jewelryField = "Davon getragener Schmuck (21-Karat-Äquivalent)"
    override val jewelryHint = "Nach Ansicht der Mehrheit ausgenommen, da auf Schmuck zum Eigengebrauch keine Zakat anfällt"
    override val investSection = "Investitionen & Handel"
    override val stocksTrade = "Aktien zum Handeln (Marktwert)"
    override val stocksLong = "Langfristige Aktien (zakatpflichtiger Anteil)"
    override val tradeGoods = "Handelswaren (Warenwert)"
    override val deductSection = "Abzüge"
    override val debtsField = "Deine Schulden, die dieses Jahr fällig werden"
    override val debtsHint = "Nur Raten, die im Zakat-Jahr fällig werden"
    override val breakdownTitle = "Zakat nach Vermögensart"
    override val zakatNote = "Deine Daten bleiben auf deinem Gerät. Allah nennt die acht Empfängergruppen der Zakat in Sure at-Tauba (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat al-Fitr"
    override val fitrSubtitle = "Wird vor dem Eid-Gebet entrichtet"
    override val foodAmountDue = "Zu gebende Nahrungsmenge"
    override val totalCashValue = "Gesamter Geldwert"
    override fun forPeople(n: Int) = if (n == 1) "für 1 Person" else "für $n Personen"
    override val peopleCount = "Personen, für die du zahlst (inkl. dir selbst)"
    override val modeFood = "Nahrung (Sa')"
    override val modeMoney = "Geldwert"
    override val foodType = "Art der Nahrung"
    override val foods = mapOf(
        "rice" to "Reis", "riceSafe" to "Reis (vorsichtiger)", "wheat" to "Weizen",
        "dates" to "Datteln", "barley" to "Gerste", "other" to "Andere Menge…",
    )
    override fun approxKg(kg: String) = "etwa $kg kg"
    override val saWeight = "Gewicht eines Sa' in kg"
    override val kgPrice = "Preis pro kg (optional)"
    override val kgPriceHint = "Um die Kosten der Nahrung abzuschätzen"
    override val saPerPerson = "Ein Sa' pro Person"
    override val approxCost = "Geschätzte Kosten"
    override val valuePerPerson = "Wert der Zakat al-Fitr pro Person"
    override val valuePerPersonHint = "Wird jährlich von der offiziellen Stelle in deinem Land bekannt gegeben"
    override val valueNote = "Den Geldwert zu zahlen ist die hanafitische Ansicht; die Mehrheit gibt Nahrung."
    override val fitrTimingNote = "Zeitpunkt: vor dem Eid-Gebet. Sie darf ein bis zwei Tage früher gegeben, aber ohne triftigen Grund nicht über das Gebet hinaus aufgeschoben werden. Sie steht den Armen und Bedürftigen zu."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidya & Kaffara"
    override val fidyaSubtitle = "Speisungsmengen in Kilogramm und Wert"
    override val tabFidya = "Fidya"
    override val tabOath = "Schwur"
    override val tabIntercourse = "Beischlaf"
    override val fidyaResult = "Fidya für versäumtes Fasten"
    override fun fidyaSub(days: Int) = "Für ${de(days.toLong(), "Tag", "Tage")} – ein Bedürftiger pro Tag"
    override val fidyaNote = "Für alle, die dauerhaft nicht fasten können, etwa Ältere oder chronisch Kranke. Man kann einen Bedürftigen für alle Tage speisen oder mehrere."
    override val oathResult = "Kaffara für einen gebrochenen Schwur"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths Kaffaras × 10 Bedürftige" else "10 Bedürftige speisen"
    override val oathAlternative = "Falls nicht möglich"
    override fun fastDays(n: Int) = de(n.toLong(), "Tag", "Tage") + " fasten"
    override val oathNote = "Zur Wahl stehen: zehn Bedürftige speisen, sie kleiden oder einen Sklaven befreien; wer das nicht kann, fastet drei Tage (Sure al-Ma'ida 5:89)."
    override val intercourseResult = "Kaffara für Beischlaf tagsüber im Ramadan"
    override val intercourseSub = "60 Bedürftige speisen, falls Fasten nicht möglich ist"
    override val ifStartToday = "Beginnst du heute zu fasten, endet es am"
    override val twoMonthsDuration = "Dauer der zwei Mondmonate"
    override val intercourseNote = "Der Reihe nach: einen Sklaven befreien; falls nicht möglich, zwei Monate ununterbrochen fasten; falls auch das nicht geht, sechzig Bedürftige speisen – dazu den Tag nachholen und bereuen."
    override val daysField = "Anzahl der Tage"
    override val oathsField = "Anzahl der Kaffaras"
    override val feedMeasure = "Nahrung pro Bedürftigem"
    override val muddOption = "Ein Mudd – etwa 0,75 kg (Malikiten & Schafiiten)"
    override val halfSaOption = "Ein halbes Sa' – etwa 1,5 kg (heutige Fatwas)"
    override val poorCount = "Bedürftige / Mahlzeiten"
    override val approxValue = "Ungefährer Wert"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Hidschra-Datum"
    override val hijriSubtitle = "Umm-al-Qura-Kalender"
    override val correspondingHijri = "Entsprechendes Hidschra-Datum"
    override val correspondingGreg = "Entsprechendes gregorianisches Datum"
    override val enterGregYear = "Gib ein Jahr zwischen 1882 und 2174 ein"
    override val enterHijriYear = "Gib ein Hidschra-Jahr zwischen 1300 und 1600 ein"
    override val gregToHijri = "Gregorianisch → Hidschra"
    override val hijriToGreg = "Hidschra → Gregorianisch"
    override val todayDate = "Heutiges Datum"
    override val prevMonth = "Vorheriger Monat"
    override val nextMonth = "Nächster Monat"
    override val calendarHint = "Die große Zahl ist das Hidschra-Datum, die kleine das gregorianische. Tippe auf einen Tag, um ihn umzurechnen."
    override val moonNote = "Das Datum kann in manchen Ländern wegen der Mondsichtung um einen Tag abweichen."

    // ---------- Age ----------
    override val ageTitle = "Altersrechner"
    override val ageSubtitle = "In Hidschra- und gregorianischen Jahren"
    override val ageGregorian = "Dein Alter (gregorianisch)"
    override fun ymd(y: Int, m: Int, d: Int) = "${de(y.toLong(), "Jahr", "Jahre")}, ${de(m.toLong(), "Monat", "Monate")}, ${de(d.toLong(), "Tag", "Tage")}"
    override fun inHijri(age: String) = "Hidschra: $age"
    override val birthGreg = "Geburtsdatum (gregorianisch)"
    override val birthHijri = "Geburtsdatum (Hidschra)"
    override val birthWeekday = "Wochentag der Geburt"
    override val nextBirthday = "Nächster Geburtstag"
    override val birthdayToday = "Heute 🎉"
    override val tileDays = "Tage"
    override val tileWeeks = "Wochen"
    override val tileHours = "Stunden"
    override val yourAge = "Dein Alter"
    override val birthAfterToday = "Das Geburtsdatum liegt in der Zukunft"
    override val enterBirth = "Gib dein Geburtsdatum ein"
    override val birthDate = "Geburtsdatum"
    override val gregorian = "Gregorianisch"
    override val hijri = "Hidschra"

    // ---------- Islamic events ----------
    override val eventsTitle = "Islamische Anlässe"
    override val eventsSubtitle = "Countdown nach dem Umm-al-Qura-Kalender"
    override val events = mapOf(
        "ramadan" to Named("Ramadan", "Der Monat des Fastens, des Nachtgebets und der Koranrezitation."),
        "lastTen" to Named("Die letzten zehn Nächte", "In ihnen liegt Lailat al-Qadr, besser als tausend Monate."),
        "eidFitr" to Named("Eid al-Fitr", "Die Zakat al-Fitr wird vor dem Festgebet entrichtet."),
        "shawwal" to Named("Sechs Tage im Schawwal", "Wer den Ramadan fastet und ihm sechs Tage im Schawwal folgen lässt, ist wie einer, der ein Leben lang gefastet hat (Muslim)."),
        "dhulHijjah" to Named("Erste zehn Tage des Dhu l-Hiddscha", "Die besten Tage der Welt; gute Taten und Takbir sind empfohlen."),
        "arafah" to Named("Tag von Arafa", "Das Fasten an diesem Tag sühnt das vergangene und das kommende Jahr, für Nicht-Pilger (Muslim)."),
        "eidAdha" to Named("Eid al-Adha", "Der Tag des Opfers, gefolgt von den drei Taschriq-Tagen."),
        "newYear" to Named("Islamisches Neujahr", "Der Beginn eines neuen Hidschra-Jahres."),
        "ashura" to Named("Aschura-Tag", "Das Fasten an diesem Tag sühnt das vergangene Jahr (Muslim); empfohlen ist, den 9. mitzufasten."),
    )
    override val unitDays = "Tage"
    override val unitHours = "Stunden"
    override val unitMinutes = "Min."
    override val unitSeconds = "Sek."
    override val eventsNote = "Die Daten sind ungefähr (Umm al-Qura) und können je nach Mondsichtung in deinem Land um einen Tag abweichen."

    // ---------- Inheritance ----------
    override val inhTitle = "Erbrechner"
    override val inhSubtitle = "Nachlassteilung nach den Regeln der Fara'id"
    override val aslLabel = "Grundzahl des Falls (Asl)"
    override val awlCase = "Fall mit 'Aul (Anteile übersteigen den Nachlass)"
    override val raddCase = "Fall mit Radd (Überschuss wird zurückverteilt)"
    override val adilCase = "Ausgeglichener Fall"
    override fun tashihFrom(n: Long) = "Korrigiert auf " + de(n, "Anteil", "Anteile")
    override val sharesTitle = "Anteil jedes Erben"
    override fun blockedBy(by: String) = "Ausgeschlossen durch $by"
    override fun perPerson(v: String) = "je $v"
    override val shareDivision = "Aufteilung teilen"
    override fun divisionShareText(list: String) = "Nachlassteilung: $list"
    override val listSeparator = ", "
    override val addHeirs = "Füge die Erben aus der Liste unten hinzu"
    override val deceasedAndEstate = "Verstorbene(r) & Nachlass"
    override val deceasedMale = "Der Verstorbene ist ein Mann"
    override val deceasedFemale = "Die Verstorbene ist eine Frau"
    override val estateTotal = "Gesamtnachlass"
    override val funeralCosts = "Kosten für Bestattung und Beerdigung"
    override val deceasedDebts = "Schulden des Verstorbenen"
    override val bequest = "Vermächtnis (an Nicht-Erben)"
    override val bequestHint = "Höchstens ein Drittel des Rests nach den Schulden, außer die Erben stimmen zu"
    override val netEstate = "Zu verteilender Nettonachlass"
    override val bequestCapped = "(Vermächtnis auf ein Drittel begrenzt)"
    override val newCase = "Neuen Fall beginnen"
    override val inhDisclaimer = "Das Ergebnis dient zur Orientierung nach der Mehrheitsmeinung. Es ersetzt weder ein Scharia-Gericht noch einen Fara'id-Gelehrten."
    override val heirs = mapOf(
        "husband" to "Ehemann", "wife" to "Ehefrau", "father" to "Vater", "mother" to "Mutter",
        "gf" to "Großvater väterlicherseits", "gmm" to "Großmutter mütterlicherseits", "gmf" to "Großmutter väterlicherseits",
        "son" to "Sohn", "daughter" to "Tochter", "gson" to "Sohn des Sohnes", "gdau" to "Tochter des Sohnes",
        "fb" to "Vollbruder", "fs" to "Vollschwester", "pb" to "Halbbruder väterlicherseits", "ps" to "Halbschwester väterlicherseits",
        "mb" to "Halbbruder mütterlicherseits", "ms" to "Halbschwester mütterlicherseits", "fnep" to "Sohn des Vollbruders", "pnep" to "Sohn des väterlichen Halbbruders",
        "fun" to "Onkel väterlicherseits (vollbürtig)", "pun" to "Onkel väterlicherseits (halbbürtig)", "fcous" to "Sohn des vollbürtigen Onkels", "pcous" to "Sohn des halbbürtigen Onkels",
    )
    override val heirGroups = mapOf(
        "spouses" to "Ehegatten", "ascendants" to "Eltern & Großeltern", "descendants" to "Kinder & Enkel",
        "siblings" to "Geschwister", "distant" to "Entferntere männliche Verwandte",
    )
    override val blockers = mapOf(
        "gf" to "Großvater",
        "twoDaughters" to "zwei Töchter (zwei Drittel ausgeschöpft)",
        "descendant" to "erbende Nachkommen",
        "fbAfterMuadda" to "Vollbruder (nach Mu'adda)",
        "fsTookRest" to "Vollschwester (erhält den Rest)",
        "gfAndSiblings" to "Großvater und Geschwister",
        "twoFullSisters" to "zwei Vollschwestern",
        "fsGhayr" to "Vollschwester (Resterbin mit Töchtern)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Fester Anteil"
        ShareType.FARD_TASIB -> "Fester Anteil + Rest"
        ShareType.FARD_TAKMILA -> "Fester Anteil (Ergänzung zu zwei Dritteln)"
        ShareType.TASIB -> "Restanteil"
        ShareType.TASIB_BIL_GHAYR -> "Restanteil durch einen Bruder"
        ShareType.TASIB_MAA_GHAYR -> "Restanteil mit Töchtern"
        ShareType.TASIB_MAA_JADD -> "Restanteil mit dem Großvater"
        ShareType.FARD_THEN_MUQASAMA -> "Fester Anteil, dann geteilt"
        ShareType.JADD_MUQASAMA -> "Mit Geschwistern: Teilung"
        ShareType.JADD_THIRD_ESTATE -> "Mit Geschwistern: ein Drittel des Nachlasses"
        ShareType.JADD_THIRD_REST -> "Mit Geschwistern: ein Drittel des Rests"
        ShareType.JADD_SIXTH -> "Mit Geschwistern: ein Sechstel des Nachlasses"
    } + if (radd) " + Radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Halbgeschwister mütterlicherseits teilen sich ein Drittel zu gleichen Teilen, Männer wie Frauen."
        InhNote.Umariyya -> "Dies ist einer der beiden 'Umariyya-Fälle: Die Mutter erhält ein Drittel dessen, was nach dem Anteil des Ehegatten übrig bleibt."
        InhNote.GrandmothersShare -> "Die beiden Großmütter teilen sich ein Sechstel."
        InhNote.FullSistersGhayr -> "Vollschwestern werden zusammen mit den Töchtern zu Resterbinnen."
        InhNote.Akdariyya -> "Dies ist der Akdariyya-Fall: Die Schwester erhält die Hälfte und der Großvater ein Sechstel, wodurch der Fall auf 9 erhöht wird; dann werden ihre Anteile zusammengelegt und im Verhältnis zwei zu eins geteilt, was 27 ergibt."
        is InhNote.JaddRule -> "Mit Geschwistern erhält der Großvater, was für ihn am günstigsten ist: Teilung oder " +
            (if (n.otherFard) "ein Drittel des Rests oder ein Sechstel des Nachlasses" else "ein Drittel des Nachlasses") +
            " – die Mehrheitsmeinung (Malikiten, Schafiiten, Hanbaliten). Abu Hanifa vertrat, dass der Großvater die Geschwister wie der Vater ausschließt."
        InhNote.Muadda -> "Die Halbgeschwister väterlicherseits wurden gegenüber dem Großvater mitgezählt (Mu'adda); ihr Anteil geht dann bei Bedarf an die Vollgeschwister zurück."
        is InhNote.NothingLeft -> "Nach den festen Anteilen bleibt nichts übrig, daher erhält der Resterbe (${nameOf(n.winner)}) nichts."
        InhNote.Mushtaraka -> "Dies ist der Muschtaraka-Fall (Himariyya): Malikiten und Schafiiten lassen die Vollgeschwister am Drittel der Halbgeschwister mütterlicherseits teilhaben; das Ergebnis hier folgt der hanafitischen und hanbalitischen Ansicht."
        is InhNote.Awl -> "Die festen Anteile überstiegen die Grundzahl, daher wurde der Fall von ${n.asl} auf ${n.awl} erhöht ('Aul), und jeder Anteil verringert sich anteilig."
        InhNote.Radd -> "Es gibt keinen Resterben, daher wird der Überschuss im Verhältnis ihrer Anteile an die Erben mit festen Anteilen zurückverteilt, außer an die Ehegatten."
        InhNote.SpouseOnlyRadd -> "Einziger Erbe ist der Ehegatte: Viele zeitgenössische Gelehrte und mehrere Familiengesetze geben ihm den Rest; frühere Gelehrte gaben ihn den entfernten Verwandten, dann der Staatskasse."
        InhNote.NoHeirs -> "Es gibt weder Erben mit festem Anteil noch Resterben; der Nachlass geht an entfernte Verwandte, dann an die Staatskasse. Wende dich an das zuständige Gericht."
    }

    // ---------- Quran ----------
    override val tabQuran = "Koran"
    override val quranTitle = "Der edle Koran"
    override val quranSubtitle = "Tippe auf einen Vers, um seinen Tafsir zu lesen"
    override val surahsTab = "Suren"
    override val juzTab = "Juz"
    override val bookmarksTab = "Gespeichert"
    override val continueReading = "Weiterlesen"
    override val startReading = "Lesen beginnen"
    override val meccan = "Mekkanisch"
    override val medinan = "Medinensisch"
    override fun ayahCount(n: Int) = de(n.toLong(), "Vers", "Verse")
    override fun juzName(n: Int) = "Juz $n"
    override fun pageName(n: Int) = "Seite $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · Vers $ayah"
    override val surahNames = listOf(
        "Al-Fatiha", "Al-Baqara", "Al 'Imran", "An-Nisa'", "Al-Ma'ida", "Al-An'am",
        "Al-A'raf", "Al-Anfal", "At-Tauba", "Yunus", "Hud", "Yusuf",
        "Ar-Ra'd", "Ibrahim", "Al-Hidschr", "An-Nahl", "Al-Isra'", "Al-Kahf",
        "Maryam", "Ta-Ha", "Al-Anbiya'", "Al-Haddsch", "Al-Mu'minun", "An-Nur",
        "Al-Furqan", "Asch-Schu'ara'", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
        "Luqman", "As-Sadschda", "Al-Ahzab", "Saba'", "Fatir", "Ya-Sin",
        "As-Saffat", "Sad", "Az-Zumar", "Ghafir", "Fussilat", "Asch-Schura",
        "Az-Zuchruf", "Ad-Duchan", "Al-Dschathiya", "Al-Ahqaf", "Muhammad", "Al-Fath",
        "Al-Hudschurat", "Qaf", "Adh-Dhariyat", "At-Tur", "An-Nadschm", "Al-Qamar",
        "Ar-Rahman", "Al-Waqi'a", "Al-Hadid", "Al-Mudschadila", "Al-Haschr", "Al-Mumtahana",
        "As-Saff", "Al-Dschumu'a", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim",
        "Al-Mulk", "Al-Qalam", "Al-Haqqa", "Al-Ma'aridsch", "Nuh", "Al-Dschinn",
        "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyama", "Al-Insan", "Al-Mursalat", "An-Naba'",
        "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inschiqaq",
        "Al-Burudsch", "At-Tariq", "Al-A'la", "Al-Ghaschiya", "Al-Fadschr", "Al-Balad",
        "Asch-Schams", "Al-Lail", "Ad-Duha", "Asch-Scharh", "At-Tin", "Al-'Alaq",
        "Al-Qadr", "Al-Bayyina", "Az-Zalzala", "Al-'Adiyat", "Al-Qari'a", "At-Takathur",
        "Al-'Asr", "Al-Humaza", "Al-Fil", "Quraisch", "Al-Ma'un", "Al-Kauthar",
        "Al-Kafirun", "An-Nasr", "Al-Masad", "Al-Ichlas", "Al-Falaq", "An-Nas",
    )
    override val searchQuranHint = "Suren oder Wörter in den Versen suchen…"
    override val searchQuranGroup = "Suren"
    override val searchAyahsGroup = "Verse"
    override val mushafView = "Mushaf-Ansicht"
    override val ayahView = "Vers für Vers"
    override val textLarger = "Größere Schrift"
    override val textSmaller = "Kleinere Schrift"
    override val tafsirTitle = "Tafsir"
    override val tafsirMuyassar = "Tafsir al-Muyassar"
    override val tafsirJalalayn = "Tafsir al-Dschalalain"
    override val translationOfMeanings = "Übersetzung der Bedeutungen"
    override val bookmarkAyah = "Vers speichern"
    override val removeBookmark = "Aus „Gespeichert“ entfernen"
    override val copy = "Kopieren"
    override val copied = "Kopiert ✓"
    override val prevAyah = "Vorheriger Vers"
    override val nextAyah = "Nächster Vers"
    override val prevSurah = "Vorherige Sure"
    override val nextSurah = "Nächste Sure"
    override val noBookmarks = "Noch keine gespeicherten Verse. Öffne einen Vers und tippe auf das Lesezeichen, um ihn zu speichern."
    override val quranLoading = "Mushaf wird geöffnet…"
    override fun hizbName(n: Int) = "Hizb $n"
    override val khatmaTitle = "Meine Khatma"
    override val khatmaPlanLabel = "Dauer"
    override fun planDays(days: Int) = if (days == 0) "Ohne Frist" else "$days Tage"
    override fun pagesRead(read: Int, total: Int) = "$read / $total Seiten"
    override val todayPortion = "Heutiger Abschnitt"
    override fun streakDays(n: Int) = "Serie: $n " + (if (n == 1) "Tag" else "Tage")
    override fun daysLeftLabel(n: Int) = "Verbleibende Tage: $n"
    override val portionDone = "Heutiger Abschnitt erledigt ✓"
    override val khatmaDone = "Khatma abgeschlossen ✓ Möge Allah sie annehmen"
    override val newKhatma = "Neue Khatma beginnen"
    override fun khatmasCompleted(n: Int) = "Abgeschlossene Khatmas: $n"
    override val continuePortion = "Abschnitt fortsetzen"
    override val ayahOfDay = "Vers des Tages"
    override val focusMode = "Fokusmodus"
    override val exitFocus = "Fokusmodus beenden"
    override val silenceTitle = "Benachrichtigungen beim Lesen stummschalten?"
    override val silenceBody = "Im Fokusmodus kann Mizan „Bitte nicht stören“ einschalten und wieder ausschalten, wenn du mit dem Lesen aufhörst. Android fragt einmal nach der Erlaubnis."
    override val allow = "Erlauben"
    override val notNow = "Nicht jetzt"
    override val tafsirLibrary = "Tafsir-Bibliothek"
    override val tafsirLibrarySub = "Lade Tafsire herunter und wähle, welche beim Tippen auf einen Vers erscheinen"
    override val onDevice = "Auf diesem Gerät"
    override val availableDownloads = "Zum Herunterladen"
    override val download = "Herunterladen"
    override fun downloadingPct(pct: Int) = "Wird heruntergeladen… $pct %"
    override val cancel = "Abbrechen"
    override val delete = "Löschen"
    override fun deleteTafsirQ(name: String) = "„$name“ von diesem Gerät löschen?"
    override val showInTafsir = "Im Tafsir anzeigen"
    override val builtIn = "Integriert"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "Download fehlgeschlagen. Prüfe deine Internetverbindung."
    override val retry = "Erneut versuchen"
    override val moreTafsirs = "Weitere Tafsire"
    override val tafsirLibraryNote = "Die Tafsire werden aus der offenen Sammlung tafsir_api (Quellen: quran.com und QUL) geladen. Das Internet wird nur für diese Downloads genutzt; danach funktionieren sie offline."
    override fun ayahRange(from: Int, to: Int) = "Erklärung der Verse $from–$to"
    override val recitation = "Rezitation"
    override val reciter = "Rezitator"
    override val playRecitation = "Rezitation abspielen"
    override val pauseRecitation = "Pause"
    override val stopRecitation = "Rezitation beenden"
    override val repeatAyah = "Jeden Vers wiederholen"
    override val listenFromHere = "Ab diesem Vers anhören"
    override val audioNote = "Die Rezitationen werden von EveryAyah.com abgespielt. Verse, die du schon gehört hast, werden auf deinem Gerät gespeichert und laufen auch ohne Internet."
    override val audioError = "Die Rezitation konnte nicht abgespielt werden. Prüfe deine Internetverbindung."
    override val hifzMode = "Auswendiglernen"
    override val revealNext = "Nächsten zeigen"
    override val hideAll = "Alle verbergen"
    override val hifzHint = "Rezitiere aus dem Gedächtnis und tippe dann auf einen verborgenen Vers, um dich zu prüfen."
    override val displayTitle = "Anzeige"
    override val pageColor = "Seitenfarbe"
    override val toneAuto = "Automatisch"
    override val toneSepia = "Sepia"
    override val toneNight = "Nacht"
    override val brightness = "Helligkeit"
    override val brightnessSystem = "Gerätehelligkeit"
    override val textSizeLabel = "Textgröße"
    override val continueFocus = "Im Fokusmodus weiterlesen"
    override val quranSources = "Korantext (uthmanische Schreibweise) vom Tanzil-Projekt (tanzil.net). Tafsir al-Muyassar (König-Fahd-Komplex), Tafsir al-Dschalalain und die Übersetzungen der Bedeutungen von tanzil.net/trans. Koran-Schriftart: Amiri Quran, SIL Open Font License. Rezitationen: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Einstellungen"
    override val appearance = "Darstellung"
    override val themeAuto = "Automatisch"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val language = "Sprache"
    override val deviceLanguage = "Gerätesprache"
    override val use24h = "24-Stunden-Format"
    override val use24hDesc = "Zeiten im 24-Stunden-Format anzeigen"
    override val exactAlarm = "Minutengenaue Hinweise"
    override val exactAlarmDesc = "Exakte Alarme erlauben, damit der Adhan-Hinweis pünktlich kommt"
    override val about = "Über"
    override val aboutName = "Mizan — Tools für Muslime"
    override fun version(v: String) = "Version $v"
    override val aboutText = "Eine kostenlose App mit den Tools, die Muslime täglich brauchen: Zakat, Erbe, Gebetszeiten, Qibla, Hidschra-Datum und Tasbih. " +
        "Die Regeln folgen der Mehrheit der Rechtsgelehrten, die Gebetszeiten beruhen auf astronomischen Formeln und die Daten auf dem Umm-al-Qura-Kalender."
    override val privacy = "Datenschutz"
    override val privacyText = "Die App erhebt keine personenbezogenen Daten und nutzt das Internet nur, um die Tafsire herunterzuladen, die du auswählst, und um Rezitationen abzuspielen, wenn du es möchtest. Dein Standort wird nur auf deinem Gerät verwendet, um Gebetszeiten und Qibla zu berechnen, " +
        "und deine Einstellungen und Zahlen bleiben auf deinem Gerät."
    override val disclaimer = "Die Ergebnisse dienen zur Orientierung. Wende dich bei persönlichen religiösen Fragen an qualifizierte Gelehrte und offizielle Stellen."
}
