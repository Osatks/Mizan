package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Pluriel français : 0 et 1 au singulier, à partir de 2 au pluriel */
private fun frPlural(n: Long, one: String, many: String) = if (n < 2L) "$n $one" else "$n $many"

object Fr : Strings {
    // Élision : « d'octobre », « l'est », « d'Asr »
    private fun startsWithVowel(s: String) = s.isNotEmpty() && s[0].lowercaseChar() in "aàâeéèêëiîïoôuùûüœ"
    private fun de(s: String) = if (startsWithVowel(s)) "d'$s" else "de $s"
    private fun day(d: Int) = if (d == 1) "1er" else "$d"

    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Outils du musulman"
    override val tabHome = "Accueil"
    override val tabPrayer = "Prière"
    override val tabQibla = "Qibla"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Réglages"
    override val back = "Retour"
    override val share = "Partager"
    override val shareSignature = "— Application Mizan"
    override val today = "Aujourd'hui"
    override val tomorrow = "Demain"
    override fun inDays(n: Long) = "dans " + frPlural(n, "jour", "jours")
    override fun daysCount(n: Long) = frPlural(n, "jour", "jours")
    override val decrease = "Diminuer"
    override val increase = "Augmenter"
    override val noLimit = "Sans limite"
    override val noResults = "Aucun résultat"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Mouharram", "Safar", "Rabi' al-Awwal", "Rabi' ath-Thani", "Joumada al-Oula", "Joumada al-Akhira",
        "Rajab", "Cha'bane", "Ramadan", "Chawwal", "Dhou al-Qi'da", "Dhou al-Hijja"
    )
    override val gregMonths = listOf(
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    )
    /** dans une date, les mois s'écrivent en minuscules : « 15 octobre 2026 » */
    private val gregMonthsInDate = gregMonths.map { it.lowercase() }
    override val weekdays = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    override val weekdaysShort = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
    override fun hijriDate(d: Int, m: Int, y: Int) = "${day(d)} ${hijriMonths[m - 1]} $y H"
    override fun gregDate(d: Int, m: Int, y: Int) = "${day(d)} ${gregMonthsInDate[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y H"
    override val hijriYearSuffix = "H"
    override val gregYearSuffix = "apr. J.-C."
    override val dayLabel = "Jour"
    override val monthLabel = "Mois"
    override val yearLabel = "Année"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Calcul de la zakat", "Liquidités, or et actions, avec le nisab"),
        "inheritance" to Named("Calcul de l'héritage", "Partager une succession selon les fara'id"),
        "prayer" to Named("Horaires de prière", "Alertes de l'adhan et compte à rebours"),
        "qibla" to Named("Direction de la qibla", "Une boussole guidée par les capteurs du téléphone"),
        "hijri" to Named("Date hégirienne", "Conversion de dates, calendrier Umm al-Qura"),
        "events" to Named("Événements islamiques", "Ramadan, les deux Aïds et Arafat"),
        "tasbih" to Named("Tasbih", "Compteur de dhikr avec adhkar intégrés"),
        "fitr" to Named("Zakat al-Fitr", "Au sa' ou en valeur"),
        "age" to Named("Calcul de l'âge", "En années hégiriennes et grégoriennes"),
        "fidya" to Named("Fidya et kaffara", "Jeûnes manqués et serments rompus"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakah zakât aumône aumone sadaqa richesse argent or épargne epargne économies economies nisab nissab actions",
        "inheritance" to "héritage heritage succession héritiers heritiers faraid fara'id farâ'id testament legs mirath partage",
        "prayer" to "salat salât prière priere adhan adhân azan appel fajr sobh dhohr dhuhr dohr asr maghrib icha isha namaz horaires heure",
        "qibla" to "kaaba kaba mecque makkah boussole direction orientation",
        "hijri" to "calendrier date conversion convertir hégire hegire hégirien hegirien islamique musulman umm al-qura oumm",
        "events" to "ramadan ramadhan aïd aid eid arafat arafa achoura ashura fêtes fetes laylat al-qadr",
        "tasbih" to "dhikr zikr compteur chapelet misbaha sebha adhkar invocations",
        "fitr" to "fitra fitrah fitr sa sâ aïd aid aumône aumone",
        "age" to "anniversaire naissance âge années annees",
        "fidya" to "fidyah kaffara kaffarah kafara expiation serment jeûne jeune nourrir",
    )
    override val toolsTitle = "Outils"
    override fun nextPrayerIn(city: String) = "Prochaine prière · $city"
    override val searchHint = "Rechercher outils, villes, adhkar…"

    // ---------- Search ----------
    override val searchTitle = "Recherche"
    override val searchToolsGroup = "Outils"
    override val searchCitiesGroup = "Villes"
    override val searchPrayersGroup = "Horaires du jour"
    override val searchEventsGroup = "Événements"
    override val searchDhikrGroup = "Adhkar"
    override val searchSettingsGroup = "Réglages"
    override val searchEmpty = "Recherchez des outils, villes, adhkar, événements et réglages"
    override val setAsMyCity = "Utiliser pour les horaires de prière"
    override fun searchNoResults(q: String) = "Aucun résultat pour « $q »"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Fajr", "sunrise" to "Chourouq", "dhuhr" to "Dhohr",
        "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Icha"
    )
    override val prayerTitle = "Horaires de prière"
    override fun nextPrayerLabel(name: String) = "Prochaine prière : $name"
    override fun atTime(time: String) = "à $time"
    override val prevDay = "Jour précédent"
    override val nextDay = "Jour suivant"
    override val monthTable = "Horaires du mois"
    override val hideMonthTable = "Masquer les horaires"
    override fun monthTableTitle(m: Int, y: Int) = "Horaires ${de(gregMonthsInDate[m - 1])} $y"
    override val colDay = "Jour"
    override val locationAndMethod = "Lieu et méthode de calcul"
    override val useMyLocation = "Utiliser ma position actuelle"
    override val locating = "Localisation en cours…"
    override val orPickCity = "Ou choisissez une ville"
    override val pickCity = "Choisir une ville"
    override val searchCity = "Rechercher une ville"
    override val myLocation = "Ma position"
    override val calcMethod = "Méthode de calcul"
    override val methods = mapOf(
        "makkah" to "Umm al-Qura (Arabie saoudite)",
        "mwl" to "Ligue islamique mondiale",
        "egypt" to "Autorité générale égyptienne d'arpentage",
        "dubai" to "Émirats arabes unis (Dubaï)",
        "kuwait" to "Koweït",
        "qatar" to "Qatar",
        "jordan" to "Jordanie et Palestine",
        "morocco" to "Maroc",
        "algeria" to "Algérie",
        "tunisia" to "Tunisie",
        "karachi" to "Université des sciences islamiques de Karachi",
        "turkey" to "Diyanet (Turquie)",
        "tehran" to "Institut de géophysique, Université de Téhéran",
        "indonesia" to "Ministère des Affaires religieuses (Indonésie)",
        "malaysia" to "JAKIM (Malaisie)",
        "singapore" to "MUIS (Singapour)",
        "russia" to "Direction spirituelle des musulmans de Russie",
        "isna" to "Amérique du Nord (ISNA)",
        "france" to "Union des organisations islamiques de France",
    )
    override val asrMethod = "Calcul de l'Asr"
    override val asrStandard = "Majorité (chaféite, malékite, hanbalite)"
    override val asrHanafi = "Hanafite"
    override val latLng = "Latitude / longitude"
    override val adhanAlert = "Alerte de l'adhan"
    override val adhanAlertDesc = "Une notification au début de chaque prière"
    override val timesMayDiffer = "Les horaires peuvent différer d'une à deux minutes du calendrier officiel de votre pays."
    override val locationFound = "Position trouvée ✓"
    override val locationFailed = "Impossible d'obtenir votre position. Vérifiez que la localisation est activée."
    override val locationDenied = "L'accès à la position n'a pas été autorisé. Choisissez votre ville dans la liste."
    override val cities = mapOf(
        "makkah" to "La Mecque", "madinah" to "Médine", "riyadh" to "Riyad", "jeddah" to "Djeddah",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubaï", "abudhabi" to "Abou Dabi", "sharjah" to "Charjah",
        "kuwait" to "Koweït", "doha" to "Doha", "manama" to "Manama", "muscat" to "Mascate",
        "cairo" to "Le Caire", "alexandria" to "Alexandrie", "aswan" to "Assouan", "amman" to "Amman",
        "jerusalem" to "Jérusalem (Al-Qods)", "gaza" to "Gaza", "beirut" to "Beyrouth", "damascus" to "Damas", "aleppo" to "Alep",
        "baghdad" to "Bagdad", "basra" to "Bassorah", "erbil" to "Erbil", "sanaa" to "Sanaa", "aden" to "Aden",
        "khartoum" to "Khartoum", "tripoli" to "Tripoli", "benghazi" to "Benghazi", "tunis" to "Tunis",
        "algiers" to "Alger", "oran" to "Oran", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakech", "nouakchott" to "Nouakchott", "mogadishu" to "Mogadiscio", "djibouti" to "Djibouti",
        "istanbul" to "Istanbul", "ankara" to "Ankara", "karachi" to "Karachi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Bombay", "hyderabad" to "Hyderabad",
        "dhaka" to "Dacca", "chittagong" to "Chittagong", "kabul" to "Kaboul", "tehran" to "Téhéran", "mashhad" to "Mechhed",
        "tashkent" to "Tachkent", "baku" to "Bakou", "almaty" to "Almaty", "moscow" to "Moscou", "kazan" to "Kazan",
        "grozny" to "Grozny", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapour",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Jakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "Londres", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marseille", "lyon" to "Lyon", "brussels" to "Bruxelles", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hambourg", "cologne" to "Cologne", "munich" to "Munich",
        "vienna" to "Vienne", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Chicago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montréal", "sydney" to "Sydney",
    )
    override val palestine = "Palestine"

    // ---------- Notifications ----------
    override val channelName = "Horaires de prière"
    override val channelDesc = "Une alerte au début de chaque prière"
    override fun prayerNowTitle(prayer: String) = "C'est l'heure ${de(prayer)}"
    override fun prayerNowText(city: String) = "$city — venez à la prière"

    // ---------- Qibla ----------
    override val qiblaTitle = "Direction de la qibla"
    override val noCompass = "Votre appareil n'a pas de boussole. Orientez-le à la main grâce à l'angle ci-dessous."
    override val facingQibla = "✓ Vous êtes face à la qibla"
    override fun turnRight(deg: Int) = "Tournez votre téléphone de $deg° vers la droite"
    override fun turnLeft(deg: Int) = "Tournez votre téléphone de $deg° vers la gauche"
    override val qiblaAngle = "Angle de la qibla par rapport au nord géographique"
    override fun towards(dir: String) = "Vers " + if (startsWithVowel(dir)) "l'$dir" else "le $dir"
    override val directions = listOf("nord", "nord-est", "est", "sud-est", "sud", "sud-ouest", "ouest", "nord-ouest")
    override val location = "Position"
    override val distanceToKaaba = "Distance jusqu'à la Kaaba"
    override val phoneHeading = "Orientation du téléphone"
    override val compassLowAccuracy = "Boussole peu précise : décrivez un 8 avec votre téléphone pour la calibrer, loin du métal et des appareils électroniques."
    override val qiblaTip = "Pour changer de lieu, utilisez la page Horaires de prière. Tenez le téléphone à plat et loin du métal pour une mesure précise."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Tasbih numérique"
    override val tasbihSubtitle = "Touchez le cercle pour compter"
    override val modeFree = "Dhikr libre"
    override val modeAfterPrayer = "Après la prière"
    override fun ofGoal(n: Int) = "sur $n"
    override val nextDhikr = "Passez au dhikr suivant"
    override val completedHundred = "Qu'Allah l'accepte de vous ✓ Vous avez complété les cent"
    override fun completedN(n: Int) = "Objectif $n atteint ✓"
    override val reset = "Réinitialiser"
    override val undo = "Annuler"
    override val chooseDhikr = "Choisir un dhikr"
    override val targetCount = "Nombre visé"
    override val yourStats = "Vos statistiques"
    override val roundsDone = "tours complets"
    override val todayCount = "aujourd'hui"
    override val totalCount = "au total"
    override val dhikrLabels = listOf(
        "SoubhanAllah", "Al-hamdoulillah", "Allahou akbar", "La ilaha illa Allah", "Astaghfiroullah",
        "SoubhanAllahi wa bihamdih", "SoubhanAllahil-'Azim", "La hawla wa la qouwwata illa billah", "Prière sur le Prophète ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Gloire à Allah",
        "Louange à Allah",
        "Allah est le plus Grand",
        "Il n'y a de divinité qu'Allah",
        "Je demande pardon à Allah",
        "Gloire et louange à Allah",
        "Gloire à Allah, l'Immense",
        "Il n'y a de force ni de puissance qu'en Allah",
        "Ô Allah, prie sur Muhammad",
    )
    override val tamamMeaning = "Il n'y a de divinité qu'Allah, Seul, sans associé. À Lui la royauté et à Lui la louange, et Il est capable de toute chose."
    override val tapToCount = "Touchez pour compter"

    // ---------- Zakat ----------
    override val zakatTitle = "Calcul de la zakat"
    override val zakatSubtitle = "2,5 % des biens qui atteignent le nisab et sont détenus une année lunaire"
    override val partCash = "Liquidités et soldes"
    override val partReceivables = "Créances recouvrables"
    override val partGold = "Or"
    override val partSilver = "Argent"
    override val partStocks = "Actions"
    override val partTrade = "Marchandises"
    override val enterGoldPrice = "Saisissez le prix de l'or pour calculer le nisab"
    override val enterSilverPrice = "Saisissez le prix de l'argent pour calculer le nisab"
    override val nisabReached = "✓ Vos biens atteignent le nisab — la zakat est due"
    override val belowNisab = "Sous le nisab — aucune zakat n'est due"
    override val enterAssets = "Saisissez vos biens"
    override val zakatDue = "Zakat due"
    override val totalAssets = "Total des biens soumis à la zakat"
    override val deductedDebts = "Dettes déduites"
    override val netBase = "Assiette nette de la zakat"
    override val nisabValue = "Valeur du nisab"
    override val approxEquals = "Équivaut à environ"
    override fun goldGrams24(g: String) = "$g g d'or 24 carats"
    override val shareResult = "Partager le résultat"
    override fun zakatShareText(zakat: String, base: String) = "Ma zakat due : $zakat (assiette de la zakat : $base)"
    override val settingsAndPrices = "Réglages et prix"
    override val currency = "Devise"
    override val nisabBasis = "Nisab retenu pour la monnaie"
    override val nisabGold = "Or (85 g)"
    override val nisabSilver = "Argent (595 g)"
    override val goldPrice24 = "Prix du gramme d'or 24 carats"
    override val goldPriceHint = "Auprès d'un bijoutier ou d'un site de cours fiable dans votre pays"
    override val silverPrice = "Prix du gramme d'argent (facultatif)"
    override val cashSection = "Liquidités et soldes"
    override val cashInHand = "Espèces en main"
    override val bankBalances = "Soldes bancaires et portefeuilles"
    override val receivablesField = "Sommes prêtées que vous comptez récupérer"
    override val otherMoney = "Autres fonds"
    override val goldSilverSection = "Or et argent (en grammes)"
    override fun goldKarat(k: Int) = "Or $k carats"
    override val pureSilver = "Argent pur"
    override val includeJewelry = "Inclure les bijoux portés en parure (avis hanafite, et le plus prudent)"
    override val jewelryField = "Dont bijoux portés (équivalent 21 carats)"
    override val jewelryHint = "Exclus selon l'avis majoritaire, qui exempte de zakat les bijoux d'usage personnel"
    override val investSection = "Investissements et commerce"
    override val stocksTrade = "Actions détenues pour la revente (valeur de marché)"
    override val stocksLong = "Actions à long terme (part soumise à la zakat)"
    override val tradeGoods = "Marchandises (valeur du stock)"
    override val deductSection = "Déductions"
    override val debtsField = "Vos dettes exigibles cette année"
    override val debtsHint = "Uniquement les échéances dues durant l'année de zakat"
    override val breakdownTitle = "Zakat par type de bien"
    override val zakatNote = "Vos données restent sur votre appareil. Allah cite les huit catégories de bénéficiaires de la zakat dans la sourate At-Tawba (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat al-Fitr"
    override val fitrSubtitle = "À verser avant la prière de l'Aïd"
    override val foodAmountDue = "Quantité de nourriture à donner"
    override val totalCashValue = "Valeur monétaire totale"
    override fun forPeople(n: Int) = "pour " + frPlural(n.toLong(), "personne", "personnes")
    override val peopleCount = "Nombre de personnes (vous compris)"
    override val modeFood = "Nourriture (sa')"
    override val modeMoney = "Valeur monétaire"
    override val foodType = "Type de nourriture"
    override val foods = mapOf(
        "rice" to "Riz", "riceSafe" to "Riz (plus prudent)", "wheat" to "Blé",
        "dates" to "Dattes", "barley" to "Orge", "other" to "Autre quantité…",
    )
    override fun approxKg(kg: String) = "environ $kg kg"
    override val saWeight = "Poids d'un sa' en kg"
    override val kgPrice = "Prix au kg (facultatif)"
    override val kgPriceHint = "Pour estimer le coût de la nourriture"
    override val saPerPerson = "Un sa' par personne"
    override val approxCost = "Coût estimé"
    override val valuePerPerson = "Valeur de la zakat al-Fitr par personne"
    override val valuePerPersonHint = "Annoncée chaque année par l'instance officielle de votre pays"
    override val valueNote = "Verser la valeur est l'avis hanafite ; la majorité donne de la nourriture."
    override val fitrTimingNote = "Quand la verser : avant la prière de l'Aïd. On peut l'avancer d'un ou deux jours, mais on ne doit pas la retarder après la prière sans excuse. Elle revient aux pauvres et aux nécessiteux."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidya et kaffara"
    override val fidyaSubtitle = "Quantités de nourriture en kilos et en valeur"
    override val tabFidya = "Fidya"
    override val tabOath = "Serment"
    override val tabIntercourse = "Rapports"
    override val fidyaResult = "Fidya des jeûnes manqués"
    override fun fidyaSub(days: Int) = "Pour ${frPlural(days.toLong(), "jour", "jours")} — un pauvre par jour"
    override val fidyaNote = "Pour qui est durablement incapable de jeûner, comme la personne âgée ou le malade chronique. On peut nourrir un seul pauvre pour tous les jours, ou plusieurs."
    override val oathResult = "Kaffara du serment rompu"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths kaffaras × 10 pauvres" else "Nourrir 10 pauvres"
    override val oathAlternative = "En cas d'incapacité"
    override fun fastDays(n: Int) = "Jeûner " + frPlural(n.toLong(), "jour", "jours")
    override val oathNote = "Au choix : nourrir dix pauvres, les vêtir ou affranchir un esclave ; qui ne le peut pas jeûne trois jours (sourate Al-Ma'ida 5:89)."
    override val intercourseResult = "Kaffara du rapport sexuel en journée de Ramadan"
    override val intercourseSub = "Nourrir 60 pauvres si l'on ne peut pas jeûner"
    override val ifStartToday = "Si vous commencez à jeûner aujourd'hui, vous finirez le"
    override val twoMonthsDuration = "Durée des deux mois lunaires"
    override val intercourseNote = "Dans l'ordre : affranchir un esclave ; sinon, jeûner deux mois consécutifs ; à défaut, nourrir soixante pauvres — en plus de rattraper le jour et de se repentir."
    override val daysField = "Nombre de jours"
    override val oathsField = "Nombre de kaffaras"
    override val feedMeasure = "Nourriture par pauvre"
    override val muddOption = "Un mudd — environ 0,75 kg (malékites et chaféites)"
    override val halfSaOption = "Un demi-sa' — environ 1,5 kg (fatwas contemporaines)"
    override val poorCount = "Pauvres / repas"
    override val approxValue = "Valeur approximative"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Date hégirienne"
    override val hijriSubtitle = "Calendrier Umm al-Qura"
    override val correspondingHijri = "Date hégirienne correspondante"
    override val correspondingGreg = "Date grégorienne correspondante"
    override val enterGregYear = "Saisissez une année entre 1882 et 2174"
    override val enterHijriYear = "Saisissez une année hégirienne entre 1300 et 1600"
    override val gregToHijri = "Grégorien → Hégirien"
    override val hijriToGreg = "Hégirien → Grégorien"
    override val todayDate = "Date du jour"
    override val prevMonth = "Mois précédent"
    override val nextMonth = "Mois suivant"
    override val calendarHint = "Le grand chiffre est la date hégirienne, le petit la date grégorienne. Touchez un jour pour le convertir."
    override val moonNote = "La date peut différer d'un jour dans certains pays selon l'observation du croissant."

    // ---------- Age ----------
    override val ageTitle = "Calcul de l'âge"
    override val ageSubtitle = "En années hégiriennes et grégoriennes"
    override val ageGregorian = "Votre âge (grégorien)"
    override fun ymd(y: Int, m: Int, d: Int) = "${frPlural(y.toLong(), "an", "ans")} ${frPlural(m.toLong(), "mois", "mois")} ${frPlural(d.toLong(), "jour", "jours")}"
    override fun inHijri(age: String) = "Hégirien : $age"
    override val birthGreg = "Date de naissance (grégorienne)"
    override val birthHijri = "Date de naissance (hégirienne)"
    override val birthWeekday = "Jour de naissance"
    override val nextBirthday = "Prochain anniversaire"
    override val birthdayToday = "Aujourd'hui 🎉"
    override val tileDays = "jours"
    override val tileWeeks = "semaines"
    override val tileHours = "heures"
    override val yourAge = "Votre âge"
    override val birthAfterToday = "La date de naissance est postérieure à aujourd'hui"
    override val enterBirth = "Saisissez votre date de naissance"
    override val birthDate = "Date de naissance"
    override val gregorian = "Grégorien"
    override val hijri = "Hégirien"

    // ---------- Islamic events ----------
    override val eventsTitle = "Événements islamiques"
    override val eventsSubtitle = "Compte à rebours selon le calendrier Umm al-Qura"
    override val events = mapOf(
        "ramadan" to Named("Ramadan", "Le mois du jeûne, de la prière nocturne et de la récitation du Coran."),
        "lastTen" to Named("Les dix dernières nuits", "Parmi elles, Laylat al-Qadr, meilleure que mille mois."),
        "eidFitr" to Named("Aïd al-Fitr", "La zakat al-Fitr se verse avant sa prière."),
        "shawwal" to Named("Six jours de Chawwal", "Celui qui jeûne Ramadan puis le fait suivre de six jours de Chawwal, c'est comme s'il avait jeûné toute sa vie (Muslim)."),
        "dhulHijjah" to Named("Dix premiers jours de Dhou al-Hijja", "Les meilleurs jours de ce monde ; les bonnes œuvres et le takbir y sont recommandés."),
        "arafah" to Named("Jour d'Arafat", "Son jeûne expie l'année passée et l'année à venir, pour qui n'accomplit pas le Hajj (Muslim)."),
        "eidAdha" to Named("Aïd al-Adha", "Le jour du sacrifice, suivi des trois jours de Tachriq."),
        "newYear" to Named("Nouvel an hégirien", "Le début d'une nouvelle année hégirienne."),
        "ashura" to Named("Jour d'Achoura", "Son jeûne expie l'année passée (Muslim) ; il est recommandé de jeûner aussi le 9."),
    )
    override val unitDays = "jours"
    override val unitHours = "heures"
    override val unitMinutes = "min"
    override val unitSeconds = "sec"
    override val eventsNote = "Les dates sont approximatives (Umm al-Qura) et peuvent différer d'un jour selon l'observation du croissant dans votre pays."

    // ---------- Inheritance ----------
    override val inhTitle = "Calcul de l'héritage"
    override val inhSubtitle = "Partager une succession selon les règles des fara'id"
    override val aslLabel = "Base du cas (asl)"
    override val awlCase = "Cas de 'awl (parts supérieures à la succession)"
    override val raddCase = "Cas de radd (reliquat restitué)"
    override val adilCase = "Cas équilibré"
    override fun tashihFrom(n: Long) = "Rectifié à " + frPlural(n, "part", "parts")
    override val sharesTitle = "Part de chaque héritier"
    /** « Exclu par le fils » : ajoute l'article quand l'exclusion vient d'un héritier */
    override fun blockedBy(by: String) =
        "Exclu par " + (heirs.entries.firstOrNull { it.value == by }?.let { heirsWithArticle[it.key] } ?: by)
    override fun perPerson(v: String) = "$v chacun"
    override val shareDivision = "Partager la répartition"
    override fun divisionShareText(list: String) = "Répartition de la succession : $list"
    override val listSeparator = ", "
    override val addHeirs = "Ajoutez les héritiers depuis la liste ci-dessous"
    override val deceasedAndEstate = "Défunt et succession"
    override val deceasedMale = "Le défunt est un homme"
    override val deceasedFemale = "La défunte est une femme"
    override val estateTotal = "Total de la succession"
    override val funeralCosts = "Frais funéraires et d'inhumation"
    override val deceasedDebts = "Dettes du défunt"
    override val bequest = "Legs (à un non-héritier)"
    override val bequestHint = "Pas plus d'un tiers de ce qui reste après les dettes, sauf accord des héritiers"
    override val netEstate = "Succession nette à partager"
    override val bequestCapped = "(legs limité au tiers)"
    override val newCase = "Commencer un nouveau cas"
    override val inhDisclaimer = "Le résultat est indicatif et suit l'avis majoritaire. Il ne remplace ni un tribunal de la charia ni un spécialiste des fara'id."
    override val heirs = mapOf(
        "husband" to "Mari", "wife" to "Épouse", "father" to "Père", "mother" to "Mère",
        "gf" to "Grand-père paternel", "gmm" to "Grand-mère maternelle", "gmf" to "Grand-mère paternelle",
        "son" to "Fils", "daughter" to "Fille", "gson" to "Fils du fils", "gdau" to "Fille du fils",
        "fb" to "Frère germain", "fs" to "Sœur germaine", "pb" to "Frère consanguin", "ps" to "Sœur consanguine",
        "mb" to "Frère utérin", "ms" to "Sœur utérine", "fnep" to "Fils du frère germain", "pnep" to "Fils du frère consanguin",
        "fun" to "Oncle paternel germain", "pun" to "Oncle paternel consanguin", "fcous" to "Fils de l'oncle paternel germain", "pcous" to "Fils de l'oncle paternel consanguin",
    )
    /** mêmes héritiers, avec article, pour « Exclu par … » */
    private val heirsWithArticle = mapOf(
        "husband" to "le mari", "wife" to "l'épouse", "father" to "le père", "mother" to "la mère",
        "gf" to "le grand-père paternel", "gmm" to "la grand-mère maternelle", "gmf" to "la grand-mère paternelle",
        "son" to "le fils", "daughter" to "la fille", "gson" to "le fils du fils", "gdau" to "la fille du fils",
        "fb" to "le frère germain", "fs" to "la sœur germaine", "pb" to "le frère consanguin", "ps" to "la sœur consanguine",
        "mb" to "le frère utérin", "ms" to "la sœur utérine", "fnep" to "le fils du frère germain", "pnep" to "le fils du frère consanguin",
        "fun" to "l'oncle paternel germain", "pun" to "l'oncle paternel consanguin", "fcous" to "le fils de l'oncle paternel germain", "pcous" to "le fils de l'oncle paternel consanguin",
    )
    override val heirGroups = mapOf(
        "spouses" to "Conjoints", "ascendants" to "Parents et grands-parents", "descendants" to "Enfants et petits-enfants",
        "siblings" to "Frères et sœurs", "distant" to "Parents mâles éloignés",
    )
    override val blockers = mapOf(
        "gf" to "le grand-père",
        "twoDaughters" to "les deux filles (deux tiers complétés)",
        "descendant" to "un descendant héritier",
        "fbAfterMuadda" to "le frère germain (après la mu'adda)",
        "fsTookRest" to "la sœur germaine qui prend le reste",
        "gfAndSiblings" to "le grand-père et la fratrie",
        "twoFullSisters" to "les deux sœurs germaines",
        "fsGhayr" to "la sœur germaine (résiduaire avec les filles)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Part fixe"
        ShareType.FARD_TASIB -> "Part fixe + reste"
        ShareType.FARD_TAKMILA -> "Part fixe (complément des deux tiers)"
        ShareType.TASIB -> "Résiduaire ('asaba)"
        ShareType.TASIB_BIL_GHAYR -> "Résiduaire par un frère"
        ShareType.TASIB_MAA_GHAYR -> "Résiduaire avec les filles"
        ShareType.TASIB_MAA_JADD -> "Résiduaire avec le grand-père"
        ShareType.FARD_THEN_MUQASAMA -> "Part fixe, puis partage"
        ShareType.JADD_MUQASAMA -> "Avec la fratrie : partage"
        ShareType.JADD_THIRD_ESTATE -> "Avec la fratrie : un tiers de la succession"
        ShareType.JADD_THIRD_REST -> "Avec la fratrie : un tiers du reste"
        ShareType.JADD_SIXTH -> "Avec la fratrie : un sixième de la succession"
    } + if (radd) " + radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Les frères et sœurs utérins se partagent un tiers à parts égales, hommes et femmes sans distinction."
        InhNote.Umariyya -> "C'est l'un des deux cas 'Umariyya : la mère reçoit un tiers de ce qui reste après la part du conjoint."
        InhNote.GrandmothersShare -> "Les deux grand-mères se partagent un sixième."
        InhNote.FullSistersGhayr -> "Les sœurs germaines deviennent résiduaires avec les filles."
        InhNote.Akdariyya -> "C'est le cas de l'Akdariyya : la sœur reçoit la moitié et le grand-père un sixième, ce qui porte le cas à 9 ; leurs parts sont ensuite réunies et partagées à raison de deux pour un, d'où 27."
        is InhNote.JaddRule -> "Avec la fratrie, le grand-père prend ce qui lui est le plus avantageux : le partage, ou " +
            (if (n.otherFard) "un tiers du reste, ou un sixième de la succession" else "un tiers de la succession") +
            " — avis majoritaire (malékites, chaféites, hanbalites). Abou Hanifa estimait que le grand-père exclut la fratrie, comme le père."
        InhNote.Muadda -> "Les frères et sœurs consanguins ont été comptés face au grand-père (mu'adda), puis leur part revient aux germains si nécessaire."
        is InhNote.NothingLeft -> "Il ne reste rien après les parts fixes : l'héritier résiduaire (${nameOf(n.winner)}) ne reçoit donc rien."
        InhNote.Mushtaraka -> "C'est le cas de la Mushtaraka (Himariyya) : malékites et chaféites font partager le tiers aux germains avec les utérins ; le résultat suit ici l'avis hanafite et hanbalite."
        is InhNote.Awl -> "Les parts fixes dépassent la base : le cas passe de ${n.asl} à ${n.awl} ('awl) et chaque part diminue proportionnellement."
        InhNote.Radd -> "Il n'y a pas d'héritier résiduaire : le surplus est restitué aux héritiers à part fixe au prorata de leurs parts, sauf aux conjoints."
        InhNote.SpouseOnlyRadd -> "Le seul héritier est le conjoint : de nombreux savants contemporains et plusieurs codes de la famille lui restituent le reste ; les anciens le donnaient aux parents éloignés, puis au Trésor public."
        InhNote.NoHeirs -> "Il n'y a ni héritier à part fixe ni résiduaire ; la succession revient aux parents éloignés, puis au Trésor public. Adressez-vous au tribunal compétent."
    }

    // ---------- Quran ----------
    override val tabQuran = "Coran"
    override val quranTitle = "Le Saint Coran"
    override val quranSubtitle = "Touchez un verset pour en lire le tafsir"
    override val surahsTab = "Sourates"
    override val juzTab = "Juz"
    override val bookmarksTab = "Favoris"
    override val continueReading = "Reprendre la lecture"
    override val startReading = "Commencer la lecture"
    override val meccan = "Mecquoise"
    override val medinan = "Médinoise"
    override fun ayahCount(n: Int) = frPlural(n.toLong(), "verset", "versets")
    override fun juzName(n: Int) = "Juz $n"
    override fun pageName(n: Int) = "Page $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · Verset $ayah"
    override val surahNames = listOf(
        "Al-Fâtiha", "Al-Baqara", "Âl 'Imrân", "An-Nisâ'", "Al-Mâ'ida", "Al-An'âm",
        "Al-A'râf", "Al-Anfâl", "At-Tawba", "Yûnus", "Hûd", "Yûsuf",
        "Ar-Ra'd", "Ibrâhîm", "Al-Hijr", "An-Nahl", "Al-Isrâ'", "Al-Kahf",
        "Maryam", "Tâ-Hâ", "Al-Anbiyâ'", "Al-Hajj", "Al-Mu'minûn", "An-Nûr",
        "Al-Furqân", "Ash-Shu'arâ'", "An-Naml", "Al-Qasas", "Al-'Ankabût", "Ar-Rûm",
        "Luqmân", "As-Sajda", "Al-Ahzâb", "Saba'", "Fâtir", "Yâ-Sîn",
        "As-Sâffât", "Sâd", "Az-Zumar", "Ghâfir", "Fussilat", "Ash-Shûrâ",
        "Az-Zukhruf", "Ad-Dukhân", "Al-Jâthiya", "Al-Ahqâf", "Muhammad", "Al-Fath",
        "Al-Hujurât", "Qâf", "Adh-Dhâriyât", "At-Tûr", "An-Najm", "Al-Qamar",
        "Ar-Rahmân", "Al-Wâqi'a", "Al-Hadîd", "Al-Mujâdila", "Al-Hashr", "Al-Mumtahana",
        "As-Saff", "Al-Jumu'a", "Al-Munâfiqûn", "At-Taghâbun", "At-Talâq", "At-Tahrîm",
        "Al-Mulk", "Al-Qalam", "Al-Hâqqa", "Al-Ma'ârij", "Nûh", "Al-Jinn",
        "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyâma", "Al-Insân", "Al-Mursalât", "An-Naba'",
        "An-Nâzi'ât", "'Abasa", "At-Takwîr", "Al-Infitâr", "Al-Mutaffifîn", "Al-Inshiqâq",
        "Al-Burûj", "At-Târiq", "Al-A'lâ", "Al-Ghâshiya", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duhâ", "Ash-Sharh", "At-Tîn", "Al-'Alaq",
        "Al-Qadr", "Al-Bayyina", "Az-Zalzala", "Al-'Âdiyât", "Al-Qâri'a", "At-Takâthur",
        "Al-'Asr", "Al-Humaza", "Al-Fîl", "Quraysh", "Al-Mâ'ûn", "Al-Kawthar",
        "Al-Kâfirûn", "An-Nasr", "Al-Masad", "Al-Ikhlâs", "Al-Falaq", "An-Nâs",
    )
    override val searchQuranHint = "Rechercher une sourate ou des mots d'un verset…"
    override val searchQuranGroup = "Sourates"
    override val searchAyahsGroup = "Versets"
    override val mushafView = "Mode Mushaf"
    override val ayahView = "Verset par verset"
    override val textLarger = "Agrandir le texte"
    override val textSmaller = "Réduire le texte"
    override val tafsirTitle = "Tafsir"
    override val tafsirMuyassar = "Tafsir al-Muyassar"
    override val tafsirJalalayn = "Tafsir al-Jalalayn"
    override val translationOfMeanings = "Traduction du sens"
    override val bookmarkAyah = "Ajouter aux favoris"
    override val removeBookmark = "Retirer des favoris"
    override val copy = "Copier"
    override val copied = "Copié ✓"
    override val prevAyah = "Verset précédent"
    override val nextAyah = "Verset suivant"
    override val prevSurah = "Sourate précédente"
    override val nextSurah = "Sourate suivante"
    override val noBookmarks = "Pas encore de favoris. Ouvrez un verset et touchez le marque-page pour l'ajouter."
    override val quranLoading = "Ouverture du Mushaf…"
    override fun hizbName(n: Int) = "Hizb $n"
    override val khatmaTitle = "Ma khatma"
    override val khatmaPlanLabel = "Terminer en"
    override fun planDays(days: Int) = if (days == 0) "Sans délai" else "$days jours"
    override fun pagesRead(read: Int, total: Int) = "$read / $total pages"
    override val todayPortion = "Portion du jour"
    override fun streakDays(n: Int) = "Série : $n " + (if (n <= 1) "jour" else "jours")
    override fun daysLeftLabel(n: Int) = "Jours restants : $n"
    override val portionDone = "Portion du jour terminée ✓"
    override val khatmaDone = "Khatma terminée ✓ Qu'Allah l'accepte"
    override val newKhatma = "Commencer une nouvelle khatma"
    override fun khatmasCompleted(n: Int) = "Khatmas terminées : $n"
    override val continuePortion = "Continuer ma portion"
    override val ayahOfDay = "Verset du jour"
    override val focusMode = "Mode concentration"
    override val exitFocus = "Quitter le mode concentration"
    override val silenceTitle = "Couper les notifications pendant la lecture ?"
    override val silenceBody = "En mode concentration, Mizan peut activer « Ne pas déranger » et le désactiver quand vous arrêtez de lire. Android vous demandera de l’autoriser une fois."
    override val allow = "Autoriser"
    override val notNow = "Plus tard"
    override val tafsirLibrary = "Bibliothèque de tafsirs"
    override val tafsirLibrarySub = "Téléchargez des tafsirs et choisissez ceux qui s’affichent quand vous touchez un verset"
    override val onDevice = "Sur cet appareil"
    override val availableDownloads = "Disponibles au téléchargement"
    override val download = "Télécharger"
    override fun downloadingPct(pct: Int) = "Téléchargement… $pct %"
    override val cancel = "Annuler"
    override val delete = "Supprimer"
    override fun deleteTafsirQ(name: String) = "Supprimer « $name » de cet appareil ?"
    override val showInTafsir = "Afficher dans le tafsir"
    override val builtIn = "Intégré"
    override fun sizeMb(mb: String) = "≈ $mb Mo"
    override val downloadFailed = "Échec du téléchargement. Vérifiez votre connexion Internet."
    override val retry = "Réessayer"
    override val moreTafsirs = "Autres tafsirs"
    override val tafsirLibraryNote = "Les tafsirs sont téléchargés depuis la collection ouverte tafsir_api (issue de quran.com et QUL). Internet n’est utilisé que pour ces téléchargements ; ensuite, ils fonctionnent hors ligne."
    override fun ayahRange(from: Int, to: Int) = "Commentaire des versets $from–$to"
    override val recitation = "Récitation"
    override val reciter = "Récitateur"
    override val playRecitation = "Écouter la récitation"
    override val pauseRecitation = "Pause"
    override val stopRecitation = "Arrêter la récitation"
    override val repeatAyah = "Répéter chaque verset"
    override val listenFromHere = "Écouter à partir de ce verset"
    override val audioNote = "Les récitations sont diffusées depuis EveryAyah.com. Les versets déjà écoutés sont enregistrés sur votre appareil et se lisent sans Internet."
    override val audioError = "Impossible de lire la récitation. Vérifiez votre connexion Internet."
    override val hifzMode = "Mémorisation"
    override val revealNext = "Afficher le suivant"
    override val hideAll = "Tout masquer"
    override val hifzHint = "Récitez de mémoire, puis touchez un verset masqué pour vérifier."
    override val displayTitle = "Affichage"
    override val pageColor = "Couleur de la page"
    override val toneAuto = "Auto"
    override val toneSepia = "Sépia"
    override val toneNight = "Nuit"
    override val brightness = "Luminosité"
    override val brightnessSystem = "Luminosité de l’appareil"
    override val textSizeLabel = "Taille du texte"
    override val continueFocus = "Continuer en mode concentration"
    override val smartPresets = "Préréglages intelligents"
    override val presetListen = "Écouter"
    override val presetMemorize = "Mémoriser"
    override val presetReview = "Réviser"
    override val smartHint = "Mémoriser : chaque verset 3 fois avec une pause pour le répéter après le récitateur. Réviser : toute la sourate, un peu plus vite."
    override val range = "Passage"
    override val rangeContinue = "En continu"
    override val rangePage = "Cette page"
    override val rangeSurah = "Cette sourate"
    override val rangeCustom = "Personnalisé"
    override val fromAyah = "De"
    override val toAyah = "À"
    override val repeatRange = "Répéter le passage"
    override val pauseAfter = "Pause après chaque verset"
    override val off = "Non"
    override val pauseAyahLength = "Durée du verset"
    override fun seconds(n: Int) = "$n s"
    override val speed = "Vitesse"
    override val sleepTimer = "Minuterie d’arrêt"
    override fun minutes(n: Int) = "$n min"
    override val evidenceTitle = "Preuves et avis des savants"
    override val evidenceSub = "D'où vient ce jugement : le Coran, la Sunna et les écoles juridiques"
    override val proofQuran = "Coran"
    override val proofHadith = "Hadith"
    override val proofConsensus = "Consensus"
    override val proofScholars = "Avis des savants"
    override val zakatTypesTitle = "Les cinq types de zakat"
    override val zakatTypesSub = "Choisissez un type de bien pour calculer sa zakat et voir ses preuves"
    override val zkMoneyName = "Liquidités, or et argent"
    override val zkMoneyDesc = "Espèces, épargne, or et argent : 2,5 % une fois le nisab détenu pendant une année lunaire"
    override val zkTradeName = "Marchandises et actions"
    override val zkTradeDesc = "Biens destinés à la vente et actions, évalués en fin d'année"
    override val zkLivestockName = "Bétail"
    override val zkLivestockDesc = "Chameaux, bovins, ovins et caprins en pâturage libre, à partir du nisab"
    override val zkCropsName = "Récoltes et fruits"
    override val zkCropsDesc = "À la récolte : un dixième ou un demi-dixième, selon l'irrigation"
    override val zkMineralsName = "Minéraux et trésors enfouis"
    override val zkMineralsDesc = "Un cinquième des trésors enfouis, 2,5 % des minéraux"
    override val zkCombinedNote = "Les liquidités, l'or, l'argent et les marchandises sont réunis en un seul nisab : ils sont donc calculés ensemble sur une même page."
    override val camels = "Chameaux"
    override val cattle = "Bovins"
    override val sheepGoats = "Ovins et caprins"
    override val unitHead = "têtes"
    override val livestockDue = "Zakat due sur le bétail"
    override val nothingDue = "Aucune zakat due"
    override fun livestockNisab(n: Int) = "Nisab : $n"
    override val grazingCondition = "Paît en pâturage libre la plus grande partie de l'année"
    override val yearCondition = "Possédé depuis une année lunaire complète"
    override val conditionsNotMet = "Pas de zakat tant que les deux conditions ne sont pas remplies ; les animaux destinés à la vente sont comptés avec les marchandises."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "mouton ou chèvre"; "bintMakhad" -> "bint makhad (chamelle d'un an)"; "bintLabun" -> "bint labun (chamelle de 2 ans)"; "hiqqah" -> "hiqqah (chamelle de 3 ans)"; "jadhaah" -> "jadha'ah (chamelle de 4 ans)"; "tabi" -> "tabi' (veau d'un an)"; "musinnah" -> "musinnah (vache de 2 ans)"; else -> kind }
    override val cropsWeight = "Poids de la récolte"
    override val irrigation = "Irrigation"
    override val irrRain = "Sans frais (pluie, sources)"
    override val irrIrrigated = "Avec frais (pompes, eau achetée)"
    override val irrMixed = "Moitié-moitié"
    override val cropsDue = "Zakat due sur la récolte"
    override fun cropsNisab(kg: String) = "Nisab : environ $kg kg"
    override val zkRate = "Taux"
    override val rikazValue = "Valeur du trésor enfoui (rikaz)"
    override val mineralValue = "Valeur des minéraux extraits"
    override val mineralsDue = "Zakat due sur les minéraux et trésors"
    override val obSkip = "Passer"
    override val obNext = "Suivant"
    override val obStart = "Commencer"
    override val appGuide = "Guide de l'application"
    override val appGuideSub = "Un aperçu rapide de ce que Mizan peut faire"
    override val onboarding = listOf(
        "Bienvenue dans Mizan" to "Les outils du musulman dans une seule application : prière, qibla, Coran, zakat et héritage. Sans publicité, et en grande partie hors ligne.",
        "Horaires de prière et qibla" to "Des horaires précis pour votre ville avec alertes de l'adhan, la qibla à la boussole, la date hégirienne et les événements islamiques.",
        "Le Saint Coran" to "Pages du mushaf, tafsir et récitation, modes concentration et mémorisation, et une khatma quotidienne qui suit votre progression et votre série.",
        "Des calculateurs avec leurs preuves" to "La zakat sous ses cinq types, l'héritage et les expiations — chaque jugement est accompagné de ses preuves tirées du Coran, de la Sunna et des savants.",
        "Tout trouver" to "Utilisez la barre de recherche de l'Accueil pour trouver outils, villes, sourates, versets et adhkar, dans n'importe quelle langue. Vous pouvez revoir cette visite depuis les Réglages.",
    )
    override val quranSources = "Texte du Coran (écriture uthmanienne) issu du projet Tanzil (tanzil.net). Le Tafsir al-Muyassar (Complexe du Roi Fahd), le Tafsir al-Jalalayn et les traductions du sens proviennent de tanzil.net/trans. Police du Coran : Amiri Quran, sous licence SIL Open Font License. Récitations : EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Réglages"
    override val appearance = "Apparence"
    override val themeAuto = "Auto"
    override val themeLight = "Clair"
    override val themeDark = "Sombre"
    override val language = "Langue"
    override val deviceLanguage = "Langue de l'appareil"
    override val use24h = "Format 24 heures"
    override val use24hDesc = "Afficher les heures au format 24 h"
    override val exactAlarm = "Alertes à l'heure exacte"
    override val exactAlarmDesc = "Autoriser les alarmes exactes pour que l'alerte de l'adhan arrive à l'heure"
    override val about = "À propos"
    override val aboutName = "Mizan — Outils du musulman"
    override val moreLanguages = "Télécharger d’autres langues"
    override val langPackHint = "Les langues sont téléchargées depuis le site de Mizan avec leur traduction du Coran, puis fonctionnent hors ligne."
    override val update = "Mettre à jour"
    override fun version(v: String) = "Version $v"
    override val aboutText = "Une application gratuite qui réunit les outils dont un musulman a besoin au quotidien : zakat, héritage, horaires de prière, qibla, date hégirienne et tasbih. " +
        "Les règles suivent la majorité des juristes, les horaires de prière reposent sur des formules astronomiques et les dates suivent le calendrier Umm al-Qura."
    override val privacy = "Confidentialité"
    override val privacyText = "L'application ne collecte aucune donnée personnelle et n’utilise Internet que pour télécharger les tafsirs que vous choisissez et lire les récitations que vous demandez. Votre position n'est utilisée que sur votre appareil pour calculer les horaires de prière et la qibla, " +
        "et vos réglages et chiffres restent sur votre appareil."
    override val disclaimer = "Les résultats sont indicatifs. Pour vos questions religieuses personnelles, consultez des savants qualifiés et les instances officielles."
}
