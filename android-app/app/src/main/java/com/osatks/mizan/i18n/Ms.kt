package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Bahasa Melayu tidak mengubah kata nama selepas bilangan: "1 hari", "5 hari" */
private fun ms(n: Long, unit: String) = "$n $unit"

object Ms : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Alatan Muslim"
    override val tabHome = "Utama"
    override val tabPrayer = "Solat"
    override val tabQibla = "Kiblat"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Tetapan"
    override val back = "Kembali"
    override val share = "Kongsi"
    override val shareSignature = "— Aplikasi Mizan"
    override val today = "Hari ini"
    override val tomorrow = "Esok"
    override fun inDays(n: Long) = ms(n, "hari") + " lagi"
    override fun daysCount(n: Long) = ms(n, "hari")
    override val decrease = "Kurangkan"
    override val increase = "Tambah"
    override val noLimit = "Tiada had"
    override val noResults = "Tiada hasil"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "PG"
    override val pm = "PTG"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal", "Jamadilakhir",
        "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijah"
    )
    override val gregMonths = listOf(
        "Januari", "Februari", "Mac", "April", "Mei", "Jun",
        "Julai", "Ogos", "September", "Oktober", "November", "Disember"
    )
    override val weekdays = listOf("Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu", "Ahad")
    override val weekdaysShort = listOf("Isn", "Sel", "Rab", "Kha", "Jum", "Sab", "Ahd")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} ${y}H"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} ${y}H"
    override val hijriYearSuffix = "H"
    override val gregYearSuffix = "M"
    override val dayLabel = "Hari"
    override val monthLabel = "Bulan"
    override val yearLabel = "Tahun"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Kalkulator Zakat", "Wang, emas dan saham berserta nisab"),
        "inheritance" to Named("Kalkulator Faraid", "Bahagi harta pusaka mengikut hukum faraid"),
        "prayer" to Named("Waktu Solat", "Peringatan azan dan kiraan detik"),
        "qibla" to Named("Arah Kiblat", "Kompas menggunakan sensor telefon anda"),
        "hijri" to Named("Tarikh Hijrah", "Tukar tarikh, kalendar Umm al-Qura"),
        "events" to Named("Hari Kebesaran Islam", "Ramadan, dua Hari Raya dan Arafah"),
        "tasbih" to Named("Tasbih", "Pengira zikir dengan zikir sedia ada"),
        "fitr" to Named("Zakat Fitrah", "Mengikut gantang (sa') atau nilai wang"),
        "age" to Named("Kalkulator Umur", "Dalam tahun Hijrah dan Masihi"),
        "fidya" to Named("Fidyah & Kafarah", "Puasa yang ditinggalkan dan sumpah yang dilanggar"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakat zakah harta emas perak nisab wang simpanan saham sedekah",
        "inheritance" to "faraid pusaka warisan waris wasiat harta mirath pembahagian",
        "prayer" to "solat salat sembahyang azan subuh zohor zuhur asar maghrib isyak waktu jadual",
        "qibla" to "kiblat qiblat kaabah kabah mekah makkah kompas arah",
        "hijri" to "hijrah hijriah hijri takwim kalendar tarikh tukar islam masihi umm al-qura",
        "events" to "ramadan raya aidilfitri aidiladha arafah asyura muharam maal hijrah cuti",
        "tasbih" to "zikir tasbih pembilang pengira biji wirid",
        "fitr" to "fitrah fitri gantang beras raya",
        "age" to "umur usia hari jadi lahir tahun",
        "fidya" to "fidyah fidya kafarah kaffarah sumpah puasa qada denda beri makan",
    )
    override val toolsTitle = "Alatan"
    override fun nextPrayerIn(city: String) = "Solat seterusnya · $city"
    override val searchHint = "Cari alatan, bandar, zikir…"

    // ---------- Search ----------
    override val searchTitle = "Carian"
    override val searchToolsGroup = "Alatan"
    override val searchCitiesGroup = "Bandar"
    override val searchPrayersGroup = "Waktu solat hari ini"
    override val searchEventsGroup = "Hari kebesaran"
    override val searchDhikrGroup = "Zikir"
    override val searchSettingsGroup = "Tetapan"
    override val searchEmpty = "Cari alatan, bandar, zikir, hari kebesaran dan tetapan"
    override val setAsMyCity = "Guna untuk waktu solat"
    override fun searchNoResults(q: String) = "Tiada hasil untuk “$q”"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Subuh", "sunrise" to "Syuruk", "dhuhr" to "Zohor",
        "asr" to "Asar", "maghrib" to "Maghrib", "isha" to "Isyak"
    )
    override val prayerTitle = "Waktu Solat"
    override fun nextPrayerLabel(name: String) = "Solat seterusnya: $name"
    override fun atTime(time: String) = "pada $time"
    override val prevDay = "Hari sebelumnya"
    override val nextDay = "Hari berikutnya"
    override val monthTable = "Jadual bulanan"
    override val hideMonthTable = "Sembunyikan jadual"
    override fun monthTableTitle(m: Int, y: Int) = "Jadual ${gregMonths[m - 1]} $y"
    override val colDay = "Hari"
    override val locationAndMethod = "Lokasi & kaedah kiraan"
    override val useMyLocation = "Guna lokasi semasa saya"
    override val locating = "Mengesan lokasi anda…"
    override val orPickCity = "Atau pilih bandar"
    override val pickCity = "Pilih bandar"
    override val searchCity = "Cari bandar"
    override val myLocation = "Lokasi saya"
    override val calcMethod = "Kaedah kiraan"
    override val methods = mapOf(
        "makkah" to "Umm al-Qura (Arab Saudi)",
        "mwl" to "Liga Muslim Sedunia",
        "egypt" to "Pihak Berkuasa Am Ukur Mesir",
        "dubai" to "UAE (Dubai)",
        "kuwait" to "Kuwait",
        "qatar" to "Qatar",
        "jordan" to "Jordan & Palestin",
        "morocco" to "Maghribi",
        "algeria" to "Algeria",
        "tunisia" to "Tunisia",
        "karachi" to "Universiti Sains Islam, Karachi",
        "turkey" to "Diyanet (Turki)",
        "tehran" to "Institut Geofizik, Universiti Tehran",
        "indonesia" to "Kementerian Agama (Indonesia)",
        "malaysia" to "JAKIM (Malaysia)",
        "singapore" to "MUIS (Singapura)",
        "russia" to "Lembaga Rohani Muslim Rusia",
        "isna" to "Amerika Utara (ISNA)",
        "france" to "Kesatuan Pertubuhan Islam Perancis",
    )
    override val asrMethod = "Kiraan Asar"
    override val asrStandard = "Jumhur (Syafie, Maliki, Hanbali)"
    override val asrHanafi = "Hanafi"
    override val latLng = "Latitud / longitud"
    override val adhanAlert = "Peringatan azan"
    override val adhanAlertDesc = "Pemberitahuan apabila masuk waktu setiap solat"
    override val timesMayDiffer = "Waktu mungkin berbeza seminit dua daripada takwim rasmi negara anda."
    override val locationFound = "Lokasi ditemui ✓"
    override val locationFailed = "Lokasi anda tidak dapat dikesan. Pastikan perkhidmatan lokasi dihidupkan."
    override val locationDenied = "Kebenaran lokasi tidak diberikan. Pilih bandar anda daripada senarai."
    override val cities = mapOf(
        "makkah" to "Mekah", "madinah" to "Madinah", "riyadh" to "Riyadh", "jeddah" to "Jeddah",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubai", "abudhabi" to "Abu Dhabi", "sharjah" to "Sharjah",
        "kuwait" to "Bandar Kuwait", "doha" to "Doha", "manama" to "Manama", "muscat" to "Muscat",
        "cairo" to "Kaherah", "alexandria" to "Iskandariah", "aswan" to "Aswan", "amman" to "Amman",
        "jerusalem" to "Baitulmaqdis (Al-Quds)", "gaza" to "Gaza", "beirut" to "Beirut", "damascus" to "Damsyik", "aleppo" to "Aleppo",
        "baghdad" to "Baghdad", "basra" to "Basrah", "erbil" to "Erbil", "sanaa" to "Sanaa", "aden" to "Aden",
        "khartoum" to "Khartoum", "tripoli" to "Tripoli", "benghazi" to "Benghazi", "tunis" to "Tunis",
        "algiers" to "Algiers", "oran" to "Oran", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakesh", "nouakchott" to "Nouakchott", "mogadishu" to "Mogadishu", "djibouti" to "Djibouti",
        "istanbul" to "Istanbul", "ankara" to "Ankara", "karachi" to "Karachi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Mumbai", "hyderabad" to "Hyderabad",
        "dhaka" to "Dhaka", "chittagong" to "Chittagong", "kabul" to "Kabul", "tehran" to "Tehran", "mashhad" to "Mashhad",
        "tashkent" to "Tashkent", "baku" to "Baku", "almaty" to "Almaty", "moscow" to "Moscow", "kazan" to "Kazan",
        "grozny" to "Grozny", "kualalumpur" to "Kuala Lumpur", "penang" to "Pulau Pinang", "singapore" to "Singapura",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Jakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "London", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marseille", "lyon" to "Lyon", "brussels" to "Brussels", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hamburg", "cologne" to "Cologne", "munich" to "Munich",
        "vienna" to "Vienna", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Chicago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sydney",
    )
    override val palestine = "Palestin"

    // ---------- Notifications ----------
    override val channelName = "Waktu solat"
    override val channelDesc = "Peringatan apabila masuk waktu setiap solat"
    override fun prayerNowTitle(prayer: String) = "Telah masuk waktu $prayer"
    override fun prayerNowText(city: String) = "$city — marilah mendirikan solat"

    // ---------- Qibla ----------
    override val qiblaTitle = "Arah Kiblat"
    override val noCompass = "Peranti anda tiada kompas. Halakan secara manual mengikut sudut di bawah."
    override val facingQibla = "✓ Anda sedang menghadap kiblat"
    override fun turnRight(deg: Int) = "Pusingkan telefon ke kanan $deg°"
    override fun turnLeft(deg: Int) = "Pusingkan telefon ke kiri $deg°"
    override val qiblaAngle = "Sudut kiblat dari utara benar"
    override fun towards(dir: String) = "Ke arah $dir"
    override val directions = listOf("utara", "timur laut", "timur", "tenggara", "selatan", "barat daya", "barat", "barat laut")
    override val location = "Lokasi"
    override val distanceToKaaba = "Jarak ke Kaabah"
    override val phoneHeading = "Arah telefon"
    override val compassLowAccuracy = "Ketepatan kompas rendah: gerakkan telefon membentuk angka 8 untuk menentukurnya, jauh daripada logam dan peranti elektronik."
    override val qiblaTip = "Untuk menukar lokasi, gunakan halaman Waktu Solat. Pegang telefon secara mendatar dan jauh daripada logam untuk bacaan yang tepat."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Tasbih Digital"
    override val tasbihSubtitle = "Ketik bulatan untuk mengira"
    override val modeFree = "Zikir bebas"
    override val modeAfterPrayer = "Selepas solat"
    override fun ofGoal(n: Int) = "daripada $n"
    override val nextDhikr = "Teruskan ke zikir seterusnya"
    override val completedHundred = "Semoga Allah menerimanya ✓ Anda telah melengkapkan seratus"
    override fun completedN(n: Int) = "Selesai $n ✓"
    override val reset = "Set semula"
    override val undo = "Buat asal"
    override val chooseDhikr = "Pilih zikir"
    override val targetCount = "Bilangan sasaran"
    override val yourStats = "Statistik anda"
    override val roundsDone = "pusingan selesai"
    override val todayCount = "hari ini"
    override val totalCount = "keseluruhan"
    override val dhikrLabels = listOf(
        "Subhanallah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah",
        "Subhanallahi wa bihamdih", "Subhanallahil 'azim", "La haula wa la quwwata illa billah", "Selawat ke atas Nabi ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Maha Suci Allah",
        "Segala puji bagi Allah",
        "Allah Maha Besar",
        "Tiada tuhan melainkan Allah",
        "Aku memohon ampun kepada Allah",
        "Maha Suci Allah dan segala puji bagi-Nya",
        "Maha Suci Allah Yang Maha Agung",
        "Tiada daya dan kekuatan melainkan dengan Allah",
        "Ya Allah, limpahkanlah selawat ke atas Muhammad",
    )
    override val tamamMeaning = "Tiada tuhan melainkan Allah Yang Maha Esa, tiada sekutu bagi-Nya. Bagi-Nya kerajaan dan bagi-Nya segala pujian, dan Dia Maha Berkuasa atas segala sesuatu."
    override val tapToCount = "Ketik untuk mengira"

    // ---------- Zakat ----------
    override val zakatTitle = "Kalkulator Zakat"
    override val zakatSubtitle = "2.5% daripada harta yang cukup nisab dan cukup haul (setahun Hijrah)"
    override val partCash = "Wang tunai & baki"
    override val partReceivables = "Hutang boleh dituntut"
    override val partGold = "Emas"
    override val partSilver = "Perak"
    override val partStocks = "Saham"
    override val partTrade = "Barang dagangan"
    override val enterGoldPrice = "Masukkan harga emas untuk mengira nisab"
    override val enterSilverPrice = "Masukkan harga perak untuk mengira nisab"
    override val nisabReached = "✓ Harta anda cukup nisab — wajib zakat"
    override val belowNisab = "Di bawah nisab — tidak wajib zakat"
    override val enterAssets = "Masukkan harta anda"
    override val zakatDue = "Zakat yang wajib"
    override val totalAssets = "Jumlah harta boleh dizakat"
    override val deductedDebts = "Hutang yang ditolak"
    override val netBase = "Harta bersih boleh dizakat"
    override val nisabValue = "Nilai nisab"
    override val approxEquals = "Lebih kurang bersamaan"
    override fun goldGrams24(g: String) = "$g g emas 24K"
    override val shareResult = "Kongsi keputusan"
    override fun zakatShareText(zakat: String, base: String) = "Zakat saya: $zakat (harta boleh dizakat $base)"
    override val settingsAndPrices = "Tetapan & harga"
    override val currency = "Mata wang"
    override val nisabBasis = "Nisab untuk wang"
    override val nisabGold = "Emas (85 g)"
    override val nisabSilver = "Perak (595 g)"
    override val goldPrice24 = "Harga 1 g emas 24K"
    override val goldPriceHint = "Daripada kedai emas atau laman harga yang dipercayai di negara anda"
    override val silverPrice = "Harga 1 g perak (pilihan)"
    override val cashSection = "Wang tunai & baki"
    override val cashInHand = "Wang tunai di tangan"
    override val bankBalances = "Baki bank dan e-dompet"
    override val receivablesField = "Hutang orang kepada anda yang dijangka dibayar"
    override val otherMoney = "Wang lain"
    override val goldSilverSection = "Emas & perak (dalam gram)"
    override fun goldKarat(k: Int) = "Emas ${k}K"
    override val pureSilver = "Perak tulen"
    override val includeJewelry = "Ambil kira barang kemas yang dipakai (pandangan Hanafi, dan lebih berhati-hati)"
    override val jewelryField = "Daripadanya, barang kemas yang dipakai (setara 21K)"
    override val jewelryHint = "Dikecualikan menurut pandangan jumhur bahawa perhiasan untuk kegunaan sendiri tidak dikenakan zakat"
    override val investSection = "Pelaburan & perniagaan"
    override val stocksTrade = "Saham untuk dagangan (nilai pasaran)"
    override val stocksLong = "Saham jangka panjang (bahagian boleh dizakat)"
    override val tradeGoods = "Barang dagangan (nilai stok)"
    override val deductSection = "Potongan"
    override val debtsField = "Hutang anda yang perlu dibayar tahun ini"
    override val debtsHint = "Hanya ansuran yang perlu dibayar dalam tempoh haul"
    override val breakdownTitle = "Zakat mengikut jenis harta"
    override val zakatNote = "Data anda kekal dalam peranti anda. Allah menyebut lapan asnaf penerima zakat dalam Surah at-Taubah (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat Fitrah"
    override val fitrSubtitle = "Ditunaikan sebelum solat Hari Raya"
    override val foodAmountDue = "Kadar makanan yang perlu dikeluarkan"
    override val totalCashValue = "Jumlah nilai wang"
    override fun forPeople(n: Int) = "untuk " + ms(n.toLong(), "orang")
    override val peopleCount = "Bilangan orang yang dibayar (termasuk diri sendiri)"
    override val modeFood = "Makanan (gantang)"
    override val modeMoney = "Nilai wang"
    override val foodType = "Jenis makanan"
    override val foods = mapOf(
        "rice" to "Beras", "riceSafe" to "Beras (lebih berhati-hati)", "wheat" to "Gandum",
        "dates" to "Kurma", "barley" to "Barli", "other" to "Kadar lain…",
    )
    override fun approxKg(kg: String) = "kira-kira $kg kg"
    override val saWeight = "Berat satu gantang (sa') dalam kg"
    override val kgPrice = "Harga sekilogram (pilihan)"
    override val kgPriceHint = "Untuk menganggar kos makanan"
    override val saPerPerson = "Satu gantang (sa') seorang"
    override val approxCost = "Anggaran kos"
    override val valuePerPerson = "Kadar zakat fitrah seorang"
    override val valuePerPersonHint = "Diumumkan setiap tahun oleh pihak berkuasa rasmi negara anda"
    override val valueNote = "Membayar dengan nilai wang ialah pandangan Hanafi; jumhur mengeluarkan makanan."
    override val fitrTimingNote = "Waktu membayar: sebelum solat Hari Raya. Boleh ditunaikan sehari atau dua lebih awal, tetapi tidak boleh dilewatkan selepas solat tanpa uzur. Ia diberikan kepada fakir dan miskin."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidyah & Kafarah"
    override val fidyaSubtitle = "Kadar memberi makan dalam kilogram dan nilai"
    override val tabFidya = "Fidyah"
    override val tabOath = "Sumpah"
    override val tabIntercourse = "Jimak"
    override val fidyaResult = "Fidyah puasa yang ditinggalkan"
    override fun fidyaSub(days: Int) = "Untuk ${ms(days.toLong(), "hari")} — seorang miskin bagi setiap hari"
    override val fidyaNote = "Bagi yang tidak mampu berpuasa secara kekal, seperti warga emas atau pesakit kronik. Boleh memberi makan seorang miskin bagi semua hari, atau beberapa orang."
    override val oathResult = "Kafarah melanggar sumpah"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths kafarah × 10 orang miskin" else "Beri makan 10 orang miskin"
    override val oathAlternative = "Jika tidak mampu"
    override fun fastDays(n: Int) = "Puasa " + ms(n.toLong(), "hari")
    override val oathNote = "Pilih antara memberi makan sepuluh orang miskin, memberi mereka pakaian, atau memerdekakan hamba; sesiapa yang tidak mampu, berpuasa tiga hari (Surah al-Ma'idah 5:89)."
    override val intercourseResult = "Kafarah jimak pada siang Ramadan"
    override val intercourseSub = "Beri makan 60 orang miskin jika tidak mampu berpuasa"
    override val ifStartToday = "Jika mula berpuasa hari ini, anda selesai pada"
    override val twoMonthsDuration = "Tempoh dua bulan Hijrah"
    override val intercourseNote = "Mengikut tertib: memerdekakan hamba; jika tidak dapat, berpuasa dua bulan berturut-turut; jika tidak mampu, memberi makan enam puluh orang miskin — beserta mengqada puasa hari itu dan bertaubat."
    override val daysField = "Bilangan hari"
    override val oathsField = "Bilangan kafarah"
    override val feedMeasure = "Makanan bagi setiap orang miskin"
    override val muddOption = "Secupak (mud) — kira-kira 0.75 kg (Maliki & Syafie)"
    override val halfSaOption = "Setengah gantang — kira-kira 1.5 kg (fatwa semasa)"
    override val poorCount = "Orang miskin / hidangan"
    override val approxValue = "Anggaran nilai"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Tarikh Hijrah"
    override val hijriSubtitle = "Kalendar Umm al-Qura"
    override val correspondingHijri = "Tarikh Hijrah yang sepadan"
    override val correspondingGreg = "Tarikh Masihi yang sepadan"
    override val enterGregYear = "Masukkan tahun antara 1882 dan 2174"
    override val enterHijriYear = "Masukkan tahun Hijrah antara 1300 dan 1600"
    override val gregToHijri = "Masihi → Hijrah"
    override val hijriToGreg = "Hijrah → Masihi"
    override val todayDate = "Tarikh hari ini"
    override val prevMonth = "Bulan sebelumnya"
    override val nextMonth = "Bulan berikutnya"
    override val calendarHint = "Nombor besar ialah Hijrah, yang kecil Masihi. Ketik sesuatu hari untuk menukarnya."
    override val moonNote = "Tarikh mungkin berbeza sehari di sesetengah negara kerana perbezaan rukyah hilal."

    // ---------- Age ----------
    override val ageTitle = "Kalkulator Umur"
    override val ageSubtitle = "Dalam tahun Hijrah dan Masihi"
    override val ageGregorian = "Umur anda (Masihi)"
    override fun ymd(y: Int, m: Int, d: Int) = "${ms(y.toLong(), "tahun")} ${ms(m.toLong(), "bulan")} ${ms(d.toLong(), "hari")}"
    override fun inHijri(age: String) = "Hijrah: $age"
    override val birthGreg = "Tarikh lahir (Masihi)"
    override val birthHijri = "Tarikh lahir (Hijrah)"
    override val birthWeekday = "Hari lahir"
    override val nextBirthday = "Hari jadi seterusnya"
    override val birthdayToday = "Hari ini 🎉"
    override val tileDays = "hari"
    override val tileWeeks = "minggu"
    override val tileHours = "jam"
    override val yourAge = "Umur anda"
    override val birthAfterToday = "Tarikh lahir selepas hari ini"
    override val enterBirth = "Masukkan tarikh lahir anda"
    override val birthDate = "Tarikh lahir"
    override val gregorian = "Masihi"
    override val hijri = "Hijrah"

    // ---------- Islamic events ----------
    override val eventsTitle = "Hari Kebesaran Islam"
    override val eventsSubtitle = "Kiraan detik mengikut kalendar Umm al-Qura"
    override val events = mapOf(
        "ramadan" to Named("Ramadan", "Bulan berpuasa, qiamullail dan membaca al-Quran."),
        "lastTen" to Named("Sepuluh malam terakhir", "Padanya terdapat Lailatulqadar, lebih baik daripada seribu bulan."),
        "eidFitr" to Named("Hari Raya Aidilfitri", "Zakat fitrah ditunaikan sebelum solatnya."),
        "shawwal" to Named("Puasa enam Syawal", "Sesiapa berpuasa Ramadan kemudian mengikutinya dengan enam hari Syawal, seolah-olah dia berpuasa sepanjang tahun (Riwayat Muslim)."),
        "dhulHijjah" to Named("Sepuluh hari awal Zulhijah", "Hari-hari terbaik di dunia; digalakkan beramal soleh dan bertakbir."),
        "arafah" to Named("Hari Arafah", "Puasanya menghapuskan dosa setahun yang lalu dan setahun yang akan datang, bagi yang tidak berhaji (Riwayat Muslim)."),
        "eidAdha" to Named("Hari Raya Aidiladha", "Hari korban, diikuti tiga hari Tasyrik."),
        "newYear" to Named("Maal Hijrah", "Permulaan tahun Hijrah yang baharu."),
        "ashura" to Named("Hari Asyura", "Puasanya menghapuskan dosa setahun yang lalu (Riwayat Muslim); disunatkan juga berpuasa pada hari ke-9 bersamanya."),
    )
    override val unitDays = "hari"
    override val unitHours = "jam"
    override val unitMinutes = "minit"
    override val unitSeconds = "saat"
    override val eventsNote = "Tarikh adalah anggaran (Umm al-Qura) dan mungkin berbeza sehari mengikut rukyah hilal di negara anda."

    // ---------- Inheritance ----------
    override val inhTitle = "Kalkulator Faraid"
    override val inhSubtitle = "Bahagi harta pusaka mengikut hukum faraid"
    override val aslLabel = "Asal masalah"
    override val awlCase = "Masalah 'aul (bahagian melebihi harta)"
    override val raddCase = "Masalah radd (baki dikembalikan)"
    override val adilCase = "Masalah seimbang ('adilah)"
    override fun tashihFrom(n: Long) = "Ditashih menjadi " + ms(n, "bahagian")
    override val sharesTitle = "Bahagian setiap waris"
    override fun blockedBy(by: String) = "Terhalang oleh $by"
    override fun perPerson(v: String) = "seorang $v"
    override val shareDivision = "Kongsi pembahagian"
    override fun divisionShareText(list: String) = "Pembahagian harta pusaka: $list"
    override val listSeparator = ", "
    override val addHeirs = "Tambah waris daripada senarai di bawah"
    override val deceasedAndEstate = "Si mati & harta pusaka"
    override val deceasedMale = "Si mati lelaki"
    override val deceasedFemale = "Si mati perempuan"
    override val estateTotal = "Jumlah harta pusaka"
    override val funeralCosts = "Kos pengurusan jenazah"
    override val deceasedDebts = "Hutang si mati"
    override val bequest = "Wasiat (kepada bukan waris)"
    override val bequestHint = "Tidak melebihi satu pertiga baki selepas hutang, kecuali dengan persetujuan waris"
    override val netEstate = "Harta bersih untuk dibahagi"
    override val bequestCapped = "(wasiat dihadkan kepada satu pertiga)"
    override val newCase = "Mulakan kes baharu"
    override val inhDisclaimer = "Keputusan ini sekadar panduan berdasarkan pandangan jumhur. Ia tidak menggantikan Mahkamah Syariah atau ahli ilmu faraid."
    override val heirs = mapOf(
        "husband" to "Suami", "wife" to "Isteri", "father" to "Bapa", "mother" to "Ibu",
        "gf" to "Datuk (sebelah bapa)", "gmm" to "Nenek (sebelah ibu)", "gmf" to "Nenek (sebelah bapa)",
        "son" to "Anak lelaki", "daughter" to "Anak perempuan", "gson" to "Cucu lelaki (daripada anak lelaki)", "gdau" to "Cucu perempuan (daripada anak lelaki)",
        "fb" to "Saudara lelaki seibu sebapa", "fs" to "Saudara perempuan seibu sebapa", "pb" to "Saudara lelaki sebapa", "ps" to "Saudara perempuan sebapa",
        "mb" to "Saudara lelaki seibu", "ms" to "Saudara perempuan seibu", "fnep" to "Anak saudara lelaki seibu sebapa", "pnep" to "Anak saudara lelaki sebapa",
        "fun" to "Bapa saudara seibu sebapa", "pun" to "Bapa saudara sebapa", "fcous" to "Anak lelaki bapa saudara seibu sebapa", "pcous" to "Anak lelaki bapa saudara sebapa",
    )
    override val heirGroups = mapOf(
        "spouses" to "Suami isteri", "ascendants" to "Ibu bapa & datuk nenek", "descendants" to "Anak & cucu",
        "siblings" to "Adik-beradik", "distant" to "Kerabat lelaki jauh (asabah)",
    )
    override val blockers = mapOf(
        "gf" to "datuk",
        "twoDaughters" to "dua anak perempuan (cukup dua pertiga)",
        "descendant" to "keturunan yang mewarisi",
        "fbAfterMuadda" to "saudara lelaki seibu sebapa (selepas mu'addah)",
        "fsTookRest" to "saudara perempuan seibu sebapa yang mengambil baki",
        "gfAndSiblings" to "datuk dan adik-beradik",
        "twoFullSisters" to "dua saudara perempuan seibu sebapa",
        "fsGhayr" to "saudara perempuan seibu sebapa (asabah bersama anak perempuan)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Fardu"
        ShareType.FARD_TASIB -> "Fardu + asabah"
        ShareType.FARD_TAKMILA -> "Fardu (menggenapkan dua pertiga)"
        ShareType.TASIB -> "Asabah"
        ShareType.TASIB_BIL_GHAYR -> "Asabah bersama saudara lelaki"
        ShareType.TASIB_MAA_GHAYR -> "Asabah bersama anak perempuan"
        ShareType.TASIB_MAA_JADD -> "Asabah bersama datuk"
        ShareType.FARD_THEN_MUQASAMA -> "Fardu, kemudian berkongsi"
        ShareType.JADD_MUQASAMA -> "Bersama saudara: berkongsi"
        ShareType.JADD_THIRD_ESTATE -> "Bersama saudara: 1/3 harta"
        ShareType.JADD_THIRD_REST -> "Bersama saudara: 1/3 baki"
        ShareType.JADD_SIXTH -> "Bersama saudara: 1/6 harta"
    } + if (radd) " + radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Saudara seibu berkongsi satu pertiga sama rata, lelaki dan perempuan sama banyak."
        InhNote.Umariyya -> "Ini salah satu daripada dua masalah 'Umariyyah: ibu mendapat satu pertiga daripada baki selepas bahagian suami atau isteri."
        InhNote.GrandmothersShare -> "Dua orang nenek berkongsi satu perenam."
        InhNote.FullSistersGhayr -> "Saudara perempuan seibu sebapa menjadi asabah bersama anak perempuan."
        InhNote.Akdariyya -> "Ini masalah Akdariyyah: saudara perempuan diberi satu perdua dan datuk satu perenam, lalu masalah di'aulkan kepada 9; kemudian bahagian kedua-duanya digabungkan dan dibahagi dua banding satu, menjadikannya 27."
        is InhNote.JaddRule -> "Bersama adik-beradik, datuk mengambil yang paling menguntungkannya: berkongsi, atau " +
            (if (n.otherFard) "satu pertiga baki, atau satu perenam harta" else "satu pertiga harta") +
            " — pandangan jumhur (Maliki, Syafie, Hanbali). Abu Hanifah berpendapat datuk menghalang adik-beradik seperti bapa."
        InhNote.Muadda -> "Saudara sebapa turut dikira bersama datuk (mu'addah), kemudian bahagian mereka dikembalikan kepada saudara seibu sebapa jika perlu."
        is InhNote.NothingLeft -> "Tiada baki selepas bahagian fardu, maka waris asabah (${nameOf(n.winner)}) tidak mendapat apa-apa."
        InhNote.Mushtaraka -> "Ini masalah Musytarakah (Himariyyah): mazhab Maliki dan Syafie membenarkan saudara seibu sebapa berkongsi satu pertiga dengan saudara seibu; keputusan di sini mengikut pandangan Hanafi dan Hanbali."
        is InhNote.Awl -> "Jumlah bahagian fardu melebihi asal masalah, maka masalah dinaikkan daripada ${n.asl} kepada ${n.awl} ('aul) dan setiap bahagian berkurang secara berkadar."
        InhNote.Radd -> "Tiada waris asabah, maka baki dikembalikan kepada ashabul furud mengikut kadar bahagian mereka, kecuali suami atau isteri."
        InhNote.SpouseOnlyRadd -> "Satu-satunya waris ialah suami atau isteri: ramai ulama semasa dan beberapa undang-undang keluarga mengembalikan baki kepadanya; ulama terdahulu memberikannya kepada zawil arham, kemudian Baitulmal."
        InhNote.NoHeirs -> "Tiada waris ashabul furud atau asabah; harta pusaka diberikan kepada zawil arham, kemudian Baitulmal. Rujuk mahkamah yang berbidang kuasa."
    }

    // ---------- Quran ----------
    override val tabQuran = "Al-Quran"
    override val quranTitle = "Al-Quran Al-Karim"
    override val quranSubtitle = "Ketik ayat untuk membaca tafsirnya"
    override val surahsTab = "Surah"
    override val juzTab = "Juzuk"
    override val bookmarksTab = "Disimpan"
    override val continueReading = "Sambung bacaan"
    override val startReading = "Mulakan bacaan"
    override val meccan = "Makkiyah"
    override val medinan = "Madaniyah"
    override fun ayahCount(n: Int) = ms(n.toLong(), "ayat")
    override fun juzName(n: Int) = "Juzuk $n"
    override fun pageName(n: Int) = "Halaman $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · Ayat $ayah"
    override val surahNames = listOf(
        "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa'", "Al-Ma'idah", "Al-An'am",
        "Al-A'raf", "Al-Anfal", "At-Taubah", "Yunus", "Hud", "Yusuf",
        "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra'", "Al-Kahfi",
        "Maryam", "Taha", "Al-Anbiya'", "Al-Hajj", "Al-Mu'minun", "An-Nur",
        "Al-Furqan", "Asy-Syu'ara'", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba'", "Fatir", "Yasin",
        "As-Saffat", "Sad", "Az-Zumar", "Ghafir", "Fussilat", "Asy-Syura",
        "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah", "Al-Ahqaf", "Muhammad", "Al-Fath",
        "Al-Hujurat", "Qaf", "Az-Zariyat", "At-Tur", "An-Najm", "Al-Qamar",
        "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadalah", "Al-Hasyr", "Al-Mumtahanah",
        "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq", "At-Tahrim",
        "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij", "Nuh", "Al-Jinn",
        "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba'",
        "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Insyiqaq",
        "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Ghasyiyah", "Al-Fajr", "Al-Balad",
        "Asy-Syams", "Al-Lail", "Ad-Dhuha", "Asy-Syarh", "At-Tin", "Al-'Alaq",
        "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat", "Al-Qari'ah", "At-Takathur",
        "Al-'Asr", "Al-Humazah", "Al-Fil", "Quraisy", "Al-Ma'un", "Al-Kauthar",
        "Al-Kafirun", "An-Nasr", "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas",
    )
    override val searchQuranHint = "Cari surah atau perkataan dalam ayat…"
    override val searchQuranGroup = "Surah"
    override val searchAyahsGroup = "Ayat"
    override val mushafView = "Paparan Mushaf"
    override val ayahView = "Ayat demi ayat"
    override val textLarger = "Besarkan teks"
    override val textSmaller = "Kecilkan teks"
    override val tafsirTitle = "Tafsir"
    override val tafsirMuyassar = "Tafsir al-Muyassar"
    override val tafsirJalalayn = "Tafsir al-Jalalain"
    override val translationOfMeanings = "Terjemahan makna"
    override val bookmarkAyah = "Simpan ayat"
    override val removeBookmark = "Buang daripada simpanan"
    override val copy = "Salin"
    override val copied = "Disalin ✓"
    override val prevAyah = "Ayat sebelumnya"
    override val nextAyah = "Ayat berikutnya"
    override val prevSurah = "Surah sebelumnya"
    override val nextSurah = "Surah berikutnya"
    override val noBookmarks = "Belum ada ayat yang disimpan. Buka mana-mana ayat dan ketik ikon penanda untuk menyimpannya."
    override val quranLoading = "Membuka Mushaf…"
    override val quranSources = "Teks al-Quran (Rasm Uthmani) daripada Projek Tanzil (tanzil.net). Tafsir al-Muyassar (Kompleks Raja Fahd), Tafsir al-Jalalain dan terjemahan makna daripada tanzil.net/trans. Fon al-Quran: Amiri Quran, lesen SIL Open Font License."

    // ---------- Settings ----------
    override val settingsTitle = "Tetapan"
    override val appearance = "Paparan"
    override val themeAuto = "Automatik"
    override val themeLight = "Cerah"
    override val themeDark = "Gelap"
    override val language = "Bahasa"
    override val deviceLanguage = "Bahasa peranti"
    override val use24h = "Format 24 jam"
    override val use24hDesc = "Papar waktu dalam format 24 jam"
    override val exactAlarm = "Peringatan tepat waktu"
    override val exactAlarmDesc = "Benarkan penggera tepat supaya peringatan azan tiba tepat pada waktunya"
    override val about = "Perihal"
    override val aboutName = "Mizan — Alatan Muslim"
    override fun version(v: String) = "Versi $v"
    override val aboutText = "Aplikasi percuma yang menghimpunkan alatan harian seorang Muslim: zakat, faraid, waktu solat, kiblat, tarikh Hijrah dan tasbih. " +
        "Hukum mengikut pandangan jumhur fuqaha, waktu solat menggunakan formula astronomi, dan tarikh mengikut kalendar Umm al-Qura."
    override val privacy = "Privasi"
    override val privacyText = "Aplikasi ini tidak mengumpul sebarang data peribadi dan tidak memerlukan sambungan internet. Lokasi anda hanya digunakan dalam peranti anda untuk mengira waktu solat dan kiblat, " +
        "dan tetapan serta angka anda kekal dalam peranti anda."
    override val disclaimer = "Keputusan ini sekadar panduan. Untuk persoalan agama anda sendiri, rujuk ulama yang berkelayakan dan pihak berkuasa agama rasmi."
}
