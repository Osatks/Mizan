package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
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
        "adhkar" to Named("Zikir & Doa", "Hisnul Muslim dan zikir harian, berserta dalil"),
        "fitr" to Named("Zakat Fitrah", "Mengikut gantang (sa') atau nilai wang"),
        "age" to Named("Kalkulator Umur", "Dalam tahun Hijrah dan Masihi"),
        "fidya" to Named("Fidyah & Kafarah", "Puasa yang ditinggalkan dan sumpah yang dilanggar"),
        "mosques" to Named("Masjid Berhampiran", "Masjid terdekat berserta arah"),
        "fasting" to Named("Penjejak Puasa", "Ramadan, qada dan puasa sunat"),
        "kids" to Named("Sudut Kanak-kanak", "Surah pendek, Asmaul Husna, doa dan carta bintang"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakat zakah harta emas perak nisab wang simpanan saham sedekah",
        "inheritance" to "faraid pusaka warisan waris wasiat harta mirath pembahagian",
        "prayer" to "solat salat sembahyang azan subuh zohor zuhur asar maghrib isyak waktu jadual",
        "qibla" to "kiblat qiblat kaabah kabah mekah makkah kompas arah",
        "hijri" to "hijrah hijriah hijri takwim kalendar tarikh tukar islam masihi umm al-qura",
        "events" to "ramadan raya aidilfitri aidiladha arafah asyura muharam maal hijrah cuti",
        "tasbih" to "zikir tasbih pembilang pengira biji wirid",
        "adhkar" to "zikir doa hisnul muslim pagi petang tidur ramadan azkar",
        "fitr" to "fitrah fitri gantang beras raya",
        "age" to "umur usia hari jadi lahir tahun",
        "fidya" to "fidyah fidya kafarah kaffarah sumpah puasa qada denda beri makan",
        "mosques" to "masjid surau musolla dekat berhampiran terdekat peta arah laluan",
        "fasting" to "puasa saum ramadan qada ganti sunat sunnah sahur berbuka iftar arafah asyura tasua syawal hari putih isnin khamis",
        "kids" to "kanak-kanak anak hafazan menghafal juz amma surah pendek asmaul husna nama allah doa bintang belajar",
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
        "jordan" to "Jordan",
        "palestine" to "Palestin (Dar al-Ifta)",
        "lebanon" to "Lubnan (Dar al-Fatwa)",
        "syria" to "Syria",
        "libya" to "Libya",
        "bahrain" to "Bahrain",
        "brunei" to "Brunei",
        "azerbaijan" to "Lembaga Muslim Kaukasus (Azerbaijan)",
        "uzbekistan" to "Lembaga Muslim Uzbekistan",
        "kazakhstan" to "Lembaga Muslim Kazakhstan",
        "tatarstan" to "Lembaga Muslim Tatarstan",
        "uk" to "Jadual Bersepadu London (UK)",
        "paris" to "Masjid Besar Paris",
        "europe" to "Kaedah Bersepadu Eropah (Istanbul 2021)",
        "bosnia" to "Komuniti Islam Bosnia dan Herzegovina",
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
    override fun endsAt(t: String) = "Tamat $t"
    override fun iqamaAt(t: String) = "Iqamah $t"
    override val iqamaTitle = "Waktu iqamah"
    override val iqamaDesc = "Minit antara azan dan iqamah di masjid anda; 0 menyembunyikannya."
    override val prayerEndsNote = "Waktu Subuh tamat apabila matahari terbit, Zohor apabila masuk Asar, Asar apabila matahari terbenam (melewatkannya hingga matahari menguning adalah makruh), Maghrib apabila syafak merah hilang, dan waktu ikhtiar Isyak tamat pada tengah malam, manakala waktu daruratnya hingga Subuh."
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
    // ---------- Adhan & alerts ----------
    override val adhanSettings = "Azan & peringatan"
    override val adhanSettingsSub = "Muazin, peringatan setiap solat dan getaran"
    override val alertPerPrayer = "Peringatan setiap solat"
    override val alertModeAdhan = "Azan"
    override val alertModeVibrate = "Getar sahaja"
    override val alertModeSilent = "Pemberitahuan senyap"
    override val alertModeOff = "Tiada peringatan"
    override val alertModesHint = "“Azan” memainkan azan penuh, “Getar sahaja” bergetar tanpa bunyi, “Pemberitahuan senyap” memaparkan pemberitahuan tanpa bunyi, dan “Tiada peringatan” tidak menghantar apa-apa untuk solat itu."
    override val endAlertTitle = "Sebelum waktu tamat"
    override val endAlertHint = "Peringatan lembut dengan bunyi pemberitahuan biasa sejurus sebelum waktu solat tamat. Waktu tamatnya sama seperti yang dipaparkan di halaman waktu solat: Subuh apabila matahari terbit, Zohor apabila masuk Asar, Asar apabila matahari terbenam, Maghrib apabila syafak hilang, dan Isyak pada tengah malam."
    override val endAlertBefore = "Minit sebelum tamat"
    override fun endAlertNotice(prayer: String, n: Int) = "Waktu $prayer tinggal $n minit lagi"
    override fun endAlertBody(t: String) = "Waktunya tamat pada $t — masih sempat untuk bersolat."
    override val muezzinTitle = "Muazin"
    override val fajrAdhanTitle = "Azan Subuh"
    override val fajrAdhanHint = "Azan Subuh ditambah “As-solatu khairun minan naum” (solat lebih baik daripada tidur)."
    override val fajrSameAsOthers = "Sama seperti solat lain"
    override val muezzinNames = mapOf(
        "asim" to "Asim Javed",
        "klcc" to "Masjid KLCC, Kuala Lumpur",
        "mirza" to "Moazzambeg Mirza, Ahmedabad",
        "adam" to "Adam-synagda (Wikimedia Commons)",
        "kalkan_fajr" to "Subuh — Kalkan, Turki",
        "madinah" to "Masjid Nabawi, Madinah",
        "andrewler" to "Andrewler (Wikimedia Commons)",
        "aaqib" to "Aaqib Azeez",
        "fouad" to "Fouad Adan",
        "fouad_fajr" to "Subuh — Fouad Adan",
        "kas_fajr" to "Subuh — Kaş, Turki",
        "malmo_fajr" to "Subuh — Besim Azemi, Malmö",
        "shalqar" to "Masjid Shalqar, Kazakhstan",
    )
    override val playAdhan = "Main azan"
    override val stopAdhan = "Henti"
    override val vibrationTitle = "Corak getaran"
    override val vibeNames = mapOf("short" to "Pendek", "double" to "Ketuk dua kali", "triple" to "Ketuk tiga kali", "long" to "Panjang", "heartbeat" to "Degupan jantung", "pulse" to "3 denyutan", "wave" to "Gelombang", "rise" to "Makin kuat", "gentle" to "Lembut", "strong" to "Kuat", "knock" to "Ketukan pintu", "rapid" to "Pantas")
    override val tryVibration = "Cuba getaran"
    override val adhanStopHint = "Ketik “Henti” pada pemberitahuan atau tekan butang kelantangan untuk menghentikan azan. Jika telefon dalam mod senyap atau getar, peringatan mengikut mod itu."
    override val adhanIosNote = "iPhone mengehadkan bunyi pemberitahuan kepada 30 saat, jadi pemberitahuan memainkan permulaan azan; azan penuh dimainkan jika Mizan dibuka pada waktu solat. Pemberitahuan bergetar mengikut corak iPhone sendiri (Tetapan › Bunyi & Haptik); corak di sini dirasai apabila Mizan dibuka."
    override val recordingSources = "Sumber rakaman"

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

    // ---------- Nearby mosques ----------
    override val mosquesTitle = "Masjid Berhampiran"
    override val mosquesSubtitle = "Masjid yang paling hampir dengan anda"
    override val mosque = "Masjid"
    override val unitM = "m"
    override val mosquesLoading = "Mencari masjid berhampiran anda…"
    override val mosquesEmpty = "Tiada masjid ditemui berhampiran"
    override val mosquesEmptyHint = "Masjid di kawasan anda mungkin belum ada dalam peta. Cuba cari dalam aplikasi peta."
    override val mosquesError = "Gagal memuatkan masjid"
    override val mosquesErrorHint = "Semak sambungan internet anda dan cuba lagi."
    override val mosquesLocationFailed = "Lokasi anda tidak dapat ditentukan. Pastikan perkhidmatan lokasi dihidupkan, kemudian cuba lagi."
    override val mosquesNeedLocation = "Benarkan Mizan menggunakan lokasi anda untuk melihat masjid berhampiran, atau pilih bandar anda di halaman Waktu Solat."
    override fun mosquesNearSaved(place: String) = "Memaparkan masjid berhampiran ${place}, lokasi yang disimpan dalam aplikasi."
    override val openInMaps = "Cari dalam Peta"
    override val mosqueDirections = "Arah ke masjid"
    override val appleMaps = "Apple Maps"
    override val googleMaps = "Google Maps"
    override val osmAttribution = "© penyumbang OpenStreetMap"
    override val mosquesPrivacyOsm = "Untuk carian, anggaran lokasi anda (hingga kira-kira 100 m) dihantar ke perkhidmatan OpenStreetMap tanpa apa-apa yang mengenal pasti anda atau peranti anda. Mizan tidak menyimpannya."
    override val mosquesPrivacyApple = "Untuk carian, lokasi anda dihantar ke Apple Maps, seperti dalam aplikasi Peta. Mizan tidak menyimpannya."
    override val mosquesMap = "Peta masjid berhampiran"

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

    // ---------- Fasting tracker ----------
    override val fastingTitle = "Puasa"
    override val fastingSubtitle = "Ramadan, qada dan puasa sunat"
    override val fastingNow = "Hari ini anda berpuasa"
    override val fastingVoluntaryToday = "Puasa sunat hari ini"
    override val fastingTodayHint = "Hidupkan jika anda berpuasa hari ini untuk melihat kiraan detik hingga tamat sahur dan hingga berbuka, serta merekod hari ini."
    override val fastingForbiddenToday = "Hari ini hari raya atau hari Tasyrik, yang tidak dipuasakan."
    override val fastingFasted = "Berpuasa"
    override val fastingMissed = "Tidak berpuasa"
    override val fastingUnmarked = "Belum ditanda"
    override val fastingRamadanHint = "Ketik hari untuk menanda berpuasa, ketik lagi untuk tidak berpuasa, dan sekali lagi untuk memadam. Hari tidak berpuasa ditambah ke hari qada."
    override val fastingQadaTitle = "Puasa qada"
    override val fastingQadaOwed = "Hari yang perlu diganti"
    override val fastingMadeUp = "Saya ganti satu hari"
    override val fastingHistory = "Sejarah"
    override val fastingNothingYet = "Belum ada rekod"
    override val fastingLogAdded = "Hari ditambah"
    override val fastingLogRemoved = "Hari dibuang"
    override val fastingLogMadeUp = "Hari diganti"
    override val fastingLogMissed = "Tidak berpuasa pada Ramadan"
    override val fastingSunnahTitle = "Puasa sunat"
    override val fastingUpcoming = "Hari akan datang"
    override val fastingKinds = mapOf(
        "arafah" to "Hari Arafah", "tasua" to "Tasua (9 Muharram)", "ashura" to "Asyura (10 Muharram)", "shawwal" to "Enam hari Syawal", "white" to "Hari Putih (13–15)", "monday" to "Isnin", "thursday" to "Khamis",
    )
    override val fastingLogged = "Puasa sunat direkod"
    override fun fastingShawwal(done: Int) = "Enam hari Syawal: ${done} daripada 6"
    override val fastingRemind = "Ingatkan pada malam sebelumnya"
    override fun fastingRemindDesc(time: String) = "Pemberitahuan pada ${time} malam sebelum hari puasa sunat"
    override fun fastingRemindTitle(what: String) = "Esok: ${what}"
    override val fastingRemindBody = "Peringatan puasa sunat esok"
    override val fastingChannel = "Peringatan puasa"

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
    override val searchTheQuran = "Cari dalam al-Quran"
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
    override fun hizbName(n: Int) = "Hizb $n"
    override val khatmaTitle = "Khatam saya"
    override val khatmaPlanLabel = "Tamat dalam"
    override fun planDays(days: Int) = if (days == 0) "Tiada tempoh" else "$days hari"
    override fun pagesRead(read: Int, total: Int) = "$read / $total muka surat"
    override val todayPortion = "Wirid hari ini"
    override fun streakDays(n: Int) = "Berturut-turut: $n hari"
    override fun daysLeftLabel(n: Int) = "Baki hari: $n"
    override val portionDone = "Wirid hari ini selesai ✓"
    override val khatmaDone = "Khatam ✓ Semoga Allah menerimanya"
    override val newKhatma = "Mula khatam baharu"
    override fun khatmasCompleted(n: Int) = "Khatam selesai: $n"
    override val continuePortion = "Sambung wirid"
    override val pagesTab = "Halaman"
    override val goToPage = "Pergi ke halaman"
    override val openPage = "Buka halaman"
    override val goButton = "Pergi"
    override val lastReadChip = "Bacaan terakhir"
    override val prevPage = "Halaman sebelumnya"
    override val nextPage = "Halaman seterusnya"
    override val typePageHint = "Taip nombor halaman"
    override fun pageSpan(from: Int, to: Int) = "Halaman $from–$to"
    override val ayahOfDay = "Ayat hari ini"
    override val focusMode = "Mod fokus"
    override val exitFocus = "Keluar dari mod fokus"
    override val silenceTitle = "Senyapkan pemberitahuan semasa membaca?"
    override val silenceBody = "Dalam mod fokus, Mizan boleh menghidupkan Jangan Ganggu dan mematikannya semula apabila anda berhenti membaca. Android akan meminta kebenaran sekali."
    override val allow = "Benarkan"
    override val notNow = "Bukan sekarang"
    override val tafsirLibrary = "Perpustakaan tafsir"
    override val tafsirLibrarySub = "Muat turun tafsir dan pilih yang dipaparkan apabila anda menyentuh ayat"
    override val onDevice = "Dalam peranti ini"
    override val availableDownloads = "Boleh dimuat turun"
    override val download = "Muat turun"
    override fun downloadingPct(pct: Int) = "Memuat turun… $pct%"
    override val cancel = "Batal"
    override val delete = "Padam"
    override fun deleteTafsirQ(name: String) = "Padam “$name” daripada peranti ini?"
    override val showInTafsir = "Papar dalam tafsir"
    override val builtIn = "Terbina dalam"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "Muat turun gagal. Semak sambungan internet anda."
    override val retry = "Cuba lagi"
    override val moreTafsirs = "Tafsir lain"
    override val tafsirLibraryNote = "Tafsir dimuat turun daripada koleksi terbuka tafsir_api (bersumberkan quran.com dan QUL). Internet hanya digunakan untuk muat turun ini; selepas itu ia berfungsi tanpa talian."
    override val downloadsTitle = "Muat turun"
    override val downloadsSub = "Terjemahan, hadis dan zikir dalam bahasa lain, rakaman azan dan halaman Mushaf"
    override fun downloadsUsed(size: String) = "$size dimuat turun pada peranti ini"
    override val downloadsNone = "Belum ada yang dimuat turun"
    override val downloadsNote = "Dimuat turun daripada laman web Mizan, kemudian berfungsi tanpa internet. Terjemahan bahasa aplikasi dimuat turun dengan sendirinya; apa yang anda padam tidak kembali melainkan anda memuat turunnya semula."
    override val dlTranslations = "Terjemahan al-Quran dan tafsir"
    override val dlHadith = "Terjemahan hadis hari ini"
    override val dlAdhkar = "Terjemahan zikir"
    override val dlAdhan = "Rakaman azan"
    override val dlMushaf = "Halaman Mushaf"
    override val deletePages = "Padam halaman yang dimuat turun"
    override val deletePagesQ = "Padam halaman Mushaf yang dimuat turun? Ia akan dimuat turun semula apabila dibuka."
    override fun downloadSized(mb: String) = "Muat turun ($mb MB)"
    override val translationMissing = "Terjemahan makna dalam bahasa anda belum ada pada peranti ini."
    override val hadithMissing = "Terjemahan hadis dalam bahasa anda belum ada pada peranti ini."
    override fun ayahRange(from: Int, to: Int) = "Tafsir ayat $from–$to"
    override val recitation = "Tilawah"
    override val reciter = "Qari"
    override val playRecitation = "Mainkan tilawah"
    override val pauseRecitation = "Jeda"
    override val stopRecitation = "Hentikan tilawah"
    override val repeatAyah = "Ulang setiap ayat"
    override val listenFromHere = "Dengar dari ayat ini"
    override val listen = "Dengar"
    override val save = "Simpan"
    override val saved = "Disimpan"
    override val ayahActionsHint = "Tekan lama mana-mana ayat untuk tafsir, mendengar, menyimpan, menyalin atau berkongsi"
    override val audioNote = "Tilawah dimainkan daripada EveryAyah.com. Ayat yang telah anda dengar disimpan dalam peranti anda dan boleh dimainkan tanpa internet."
    override val audioError = "Tilawah tidak dapat dimainkan. Semak sambungan internet anda."
    override val hifzMode = "Hafazan"
    override val revealNext = "Tunjuk seterusnya"
    override val hideAll = "Sembunyikan semua"
    override val hifzHint = "Baca daripada hafazan, kemudian ketik ayat yang tersembunyi untuk menyemaknya."
    override val displayTitle = "Paparan"
    override val pageColor = "Warna halaman"
    override val toneAuto = "Automatik"
    override val toneSepia = "Sepia"
    override val toneNight = "Malam"
    override val toneDepth = "3D"
    override val toneDepthNight = "3D Malam"
    override val brightness = "Kecerahan"
    override val brightnessSystem = "Kecerahan peranti"
    override val textSizeLabel = "Saiz teks"
    override val continueFocus = "Teruskan dalam mod fokus"
    override val smartPresets = "Tetapan pintar"
    override val presetListen = "Dengar"
    override val presetMemorize = "Hafazan"
    override val presetReview = "Ulang kaji"
    override val smartHint = "Hafazan: setiap ayat 3 kali dengan jeda untuk mengulanginya selepas qari. Ulang kaji: seluruh surah, sedikit lebih laju."
    override val range = "Julat"
    override val rangeContinue = "Berterusan"
    override val rangePage = "Halaman ini"
    override val rangeSurah = "Surah ini"
    override val rangeCustom = "Tersuai"
    override val fromAyah = "Dari"
    override val toAyah = "Hingga"
    override val repeatRange = "Ulang julat"
    override val pauseAfter = "Jeda selepas setiap ayat"
    override val off = "Tutup"
    override val pauseAyahLength = "Sepanjang ayat"
    override fun seconds(n: Int) = "$n saat"
    override val speed = "Kelajuan"
    override val sleepTimer = "Pemasa tidur"
    override fun minutes(n: Int) = "$n min"
    override val evidenceTitle = "Dalil dan pandangan ulama"
    override val evidenceSub = "Sumber hukum ini: Al-Quran, Sunnah dan mazhab-mazhab fiqh"
    override val proofQuran = "Al-Quran"
    override val proofHadith = "Hadis"
    override val proofConsensus = "Ijmak"
    override val proofScholars = "Pandangan ulama"
    override val zakatTypesTitle = "Lima jenis zakat"
    override val zakatTypesSub = "Pilih jenis harta untuk mengira zakatnya dan melihat dalilnya"
    override val zkMoneyName = "Wang, emas dan perak"
    override val zkMoneyDesc = "Tunai, simpanan, emas dan perak: 2.5% sekali apabila cukup nisab dan cukup haul setahun Hijrah"
    override val zkTradeName = "Barang dagangan dan saham"
    override val zkTradeDesc = "Barang untuk dijual dan saham, dinilai pada akhir tahun"
    override val zkLivestockName = "Binatang ternakan"
    override val zkLivestockDesc = "Unta, lembu, kambing dan biri-biri yang diragut bebas, apabila cukup nisab"
    override val zkCropsName = "Tanaman dan buah-buahan"
    override val zkCropsDesc = "Ketika menuai: sepersepuluh atau separuh daripadanya, mengikut cara pengairan"
    override val zkMineralsName = "Galian dan harta karun"
    override val zkMineralsDesc = "Seperlima daripada harta karun, 2.5% daripada galian"
    override val zkCombinedNote = "Wang, emas, perak dan barang dagangan digabungkan dalam satu nisab, jadi semuanya dikira bersama dalam satu halaman."
    override val camels = "Unta"
    override val cattle = "Lembu"
    override val sheepGoats = "Kambing dan biri-biri"
    override val unitHead = "ekor"
    override val livestockDue = "Zakat ternakan yang wajib"
    override val nothingDue = "Tiada zakat wajib"
    override fun livestockNisab(n: Int) = "Nisab: $n"
    override val grazingCondition = "Meragut di padang terbuka sepanjang sebahagian besar tahun"
    override val yearCondition = "Dimiliki selama setahun Hijrah penuh"
    override val conditionsNotMet = "Tiada zakat sehingga kedua-dua syarat dipenuhi; binatang yang disimpan untuk dijual dikira bersama barang dagangan."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "biri-biri atau kambing"; "bintMakhad" -> "bint makhad (unta betina 1 tahun)"; "bintLabun" -> "bint labun (unta betina 2 tahun)"; "hiqqah" -> "hiqqah (unta betina 3 tahun)"; "jadhaah" -> "jadha'ah (unta betina 4 tahun)"; "tabi" -> "tabi' (anak lembu 1 tahun)"; "musinnah" -> "musinnah (lembu 2 tahun)"; else -> kind }
    override val cropsWeight = "Berat hasil tuaian"
    override val irrigation = "Pengairan"
    override val irrRain = "Tanpa kos (hujan, mata air)"
    override val irrIrrigated = "Dengan kos (pam, air dibeli)"
    override val irrMixed = "Separuh-separuh"
    override val cropsDue = "Zakat hasil tuaian yang wajib"
    override fun cropsNisab(kg: String) = "Nisab: kira-kira $kg kg"
    override val zkRate = "Kadar"
    override val rikazValue = "Nilai harta karun (rikaz)"
    override val mineralValue = "Nilai galian yang dikeluarkan"
    override val mineralsDue = "Zakat galian dan harta karun yang wajib"
    override val obSkip = "Langkau"
    override val obNext = "Seterusnya"
    override val obStart = "Mula"
    override val obLocationTitle = "Di manakah anda?"
    override val obLocationBody = "Lokasi anda diperlukan untuk mengira waktu solat dan arah kiblat dengan tepat. Ia kekal pada peranti anda dan tidak dihantar ke mana-mana pelayan."
    override val obHelloTitle = "Assalamualaikum"
    override val obHelloBody = "Selamat datang ke Mizan. Mari sediakannya bersama dalam beberapa langkah ringkas, bermula dengan bahasa anda."
    override val obTodayTimes = "Waktu solat hari ini"
    override val obAdhanTitle = "Dengar azan"
    override val obAdhanBody = "Peringatan setiap kali masuk waktu solat, dengan suara yang anda gemari."
    override val obMoreVoices = "Suara lain ada dalam Tetapan, di bahagian Azan & peringatan."
    override val obStyleTitle = "Gaya anda"
    override val obStyleBody = "Pilih rupa Mizan. Skrin berubah sebaik sahaja anda ketik."
    override val obTryTitle = "Cuba"
    override val obTryBody = "Ketik biji tasbih tiga kali sambil menyebut “Subhanallah” setiap kali."
    override val obTryDone = "Syabas! Tasbih penuh menanti anda di Alatan."
    override val obBead = "Biji tasbih"
    override val obDoneTitle = "Semua sedia"
    override val obDoneBody = "Semuanya telah disimpan. Anda boleh mengubahnya atau membuka panduan aplikasi dari Tetapan pada bila-bila masa."
    override val obCity = "Bandar"
    override val obNotSet = "Belum ditetapkan"
    override val obOn = "Hidup"
    override val obOff = "Tutup"
    override fun obStep(n: Int, total: Int) = "Langkah $n daripada $total"
    override val locationNeeded = "Tetapkan lokasi anda untuk melihat waktu solat yang tepat bagi bandar anda"
    override val appGuide = "Panduan aplikasi"
    override val appGuideSub = "Lawatan ringkas tentang apa yang boleh dilakukan oleh Mizan"
    override val onboarding = listOf(
        "Selamat datang ke Mizan" to "Alatan Muslim dalam satu aplikasi: solat, kiblat, Al-Quran, zakat dan faraid. Tanpa iklan, dan kebanyakannya berfungsi di luar talian.",
        "Waktu solat dan kiblat" to "Waktu yang tepat untuk bandar anda dengan peringatan azan, arah kiblat dengan kompas, serta tarikh Hijrah dan hari kebesaran Islam.",
        "Al-Quran Al-Karim" to "Halaman mushaf, tafsir dan tilawah, mod fokus dan hafazan, serta khatam harian yang menjejak kemajuan anda dan hari berturut-turut.",
        "Kalkulator berserta dalilnya" to "Zakat dengan lima jenisnya, faraid dan kafarah — setiap hukum disertakan dalilnya daripada Al-Quran, Sunnah dan para ulama.",
        "Cari apa sahaja" to "Gunakan bar carian di halaman Utama untuk alatan, bandar, surah, ayat dan zikir, dalam apa jua bahasa. Anda boleh mengulang lawatan ini dari Tetapan.",
    )
    override val quranSources = "Teks al-Quran (Rasm Uthmani) daripada Projek Tanzil (tanzil.net). Tafsir al-Muyassar (Kompleks Raja Fahd), Tafsir al-Jalalain dan terjemahan makna daripada tanzil.net/trans. Fon al-Quran: Amiri Quran, lesen SIL Open Font License. Tilawah: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Tetapan"
    override val appearance = "Paparan"
    override val themeAuto = "Automatik"
    override val themeLight = "Cerah"
    override val themeDark = "Gelap"
    override val appColor = "Warna aplikasi"
    override val accentNames = listOf("Zamrud", "Biru kehijauan", "Biru", "Ungu", "Merah jambu", "Merah marun", "Jingga", "Madu", "Coklat", "Grafit")
    override val language = "Bahasa"
    override val deviceLanguage = "Bahasa peranti"
    override val use24h = "Format 24 jam"
    override val use24hDesc = "Papar waktu dalam format 24 jam"
    override val exactAlarm = "Peringatan tepat waktu"
    override val exactAlarmDesc = "Benarkan penggera tepat supaya peringatan azan tiba tepat pada waktunya"
    override val about = "Perihal"
    override val aboutName = "Mizan — Alatan Muslim"
    override val adhkarNow = "Sekarang"
    override val dhikrEvidence = "Dalil"
    override val dhikrSource = "Sumber"
    override val dhikrGrade = "Darjat hadis"
    override val dhikrNarrator = "Perawi"
    override val dhikrVirtue = "Kelebihan"
    override val dhikrQuranRef = "Daripada al-Quran"
    override val dhikrDone = "Anda telah menyempurnakan zikir ini. Semoga Allah menerimanya"
    override val dhikrTapHint = "Ketik zikir untuk mengira, dan “Dalil” untuk melihat sumber dan darjatnya"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, other {# kali}}", mapOf("n" to n), "ms")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, other {# zikir}}", mapOf("n" to n), "ms")
    // ---------- ركن الأطفال ----------
    override val kidsSurahs = "Surah pendek"
    override val kidsSurahsSub = "Baca, dengar dan ikut bacaan qari"
    override val kidsNames = "Asmaul Husna"
    override val kidsNamesSub = "Leret untuk mengenali setiap nama, bersama ayat yang menyebutnya"
    override val kidsDuas = "Doa harian"
    override val kidsDuasSub = "Doa pendek untuk makan, tidur, keluar rumah dan lain-lain"
    override val kidsStars = "Carta bintang"
    override val kidsStarsSub = "Ibu bapa mengetik surah setelah anak menghafalnya"
    override fun kidsStarCount(n: Int) = Template.format("{n, plural, other {# bintang}}", mapOf("n" to n), "ms")
    override val kidsMarkMemorized = "Sudah hafal? Beri bintang"
    override val kidsMemorized = "Sudah hafal"
    override val kidsRepeatAfterMe = "Ikut saya"
    override val kidsListen = "Dengar…"
    override val kidsYourTurn = "Giliran kamu! Sebut ayat itu"
    override val kidsWellDone = "Syabas! Masya-Allah"
    override val kidsTapAyah = "Ketik ayat untuk mula dari situ"
    override val kidsPrevName = "Nama sebelumnya"
    override val kidsNextName = "Nama seterusnya"
    override val moreLanguages = "Muat turun bahasa lain"
    override val langPackHint = "Bahasa dimuat turun dari laman web Mizan bersama terjemahan al-Quran, kemudian berfungsi tanpa internet."
    override val update = "Kemas kini"
    override fun version(v: String) = "Versi $v"
    override val aboutText = "Aplikasi percuma yang menghimpunkan alatan harian seorang Muslim: zakat, faraid, waktu solat, kiblat, tarikh Hijrah dan tasbih. " +
        "Hukum mengikut pandangan jumhur fuqaha, waktu solat menggunakan formula astronomi, dan tarikh mengikut kalendar Umm al-Qura."
    override val privacy = "Privasi"
    override val privacyText = "Aplikasi tidak mengumpul sebarang data peribadi dan hanya menggunakan internet apabila anda memintanya: untuk memainkan bacaan dan memuat turun tafsir, pek bahasa dan rakaman azan yang anda pilih. Lokasi anda hanya digunakan pada peranti anda untuk mengira waktu solat dan kiblat, dan tetapan serta angka anda kekal pada peranti anda."
    override val disclaimer = "Keputusan ini sekadar panduan. Untuk persoalan agama anda sendiri, rujuk ulama yang berkelayakan dan pihak berkuasa agama rasmi."
    override val backupTitle = "Sandaran & Penyegerakan"
    override val backupSave = "Simpan sandaran"
    override val backupSaveSub = "Fail mengandungi tetapan, ayat tersimpan, khatam dan tasbih anda"
    override val backupRestore = "Pulihkan daripada sandaran"
    override val backupRestoreSub = "Daripada fail yang disimpan oleh Mizan pada peranti ini atau peranti lain"
    override val backupConfirmTitle = "Pulihkan sandaran ini?"
    override fun backupConfirmBody(date: String) = "Sandaran bertarikh $date akan menggantikan data semasa pada peranti ini."
    override val backupRestoreAction = "Pulihkan"
    override val backupSaved = "Sandaran disimpan"
    override val backupSaveFailed = "Tidak dapat menyimpan sandaran"
    override val backupRestored = "Data anda telah dipulihkan"
    override val backupRestoreFailed = "Tidak dapat memulihkan sandaran. Sila cuba lagi."
    override val backupReadFailed = "Tidak dapat membuka fail"
    override val backupInvalid = "Fail ini bukan sandaran Mizan"
    override val backupTooNew = "Sandaran ini daripada versi Mizan yang lebih baharu. Kemas kini aplikasi dan cuba lagi."
    override val backupAutoNote = "Android juga menyandarkan tetapan Mizan ke akaun Google anda secara automatik, dan memulihkannya apabila anda memasang aplikasi pada telefon baharu dengan akaun yang sama."
    override val backupPrivacyNote = "Sandaran ialah fail anda sendiri: tiada kata laluan di dalamnya dan ia tidak pernah dihantar ke mana-mana pelayan kami."
    override val icloudSync = "Segerakkan dengan iCloud"
    override val icloudSyncSub = "Data yang sama pada peranti anda yang log masuk dengan Akaun Apple yang sama"
    override fun icloudLastSync(time: String) = "Penyegerakan terakhir: $time"
    override val icloudUnavailable = "Tidak dapat mencapai iCloud"
    override val icloudOfferTitle = "iCloud mempunyai data daripada peranti lain"
    override fun icloudOfferBody(date: String) = "Disimpan pada $date. Pulihkan data ini menggantikan data peranti ini, atau kekalkan data peranti ini dan simpan ke iCloud?"
    override val icloudKeepLocal = "Kekalkan data peranti ini"
    // ---------- المتجر والمشاركة والأذونات ----------
    override val rateApp = "Nilai Mizan"
    override val rateAppSub = "Ulasan anda membantu orang lain menemui aplikasi ini"
    override val shareApp = "Kongsi Mizan"
    override val shareAppSub = "Hantar aplikasi ini kepada keluarga dan rakan"
    override fun shareAppText(link: String) = "Mizan: waktu solat, kiblat, al-Quran, zakat dan faraid dalam satu aplikasi percuma tanpa iklan\n$link"
    override val licenses = "Sumber dan lesen"
    override val licensesSub = "Teks al-Quran, tafsir, bacaan, rakaman azan dan fon"
    override val openSettings = "Buka Tetapan"
    override val locationOffTitle = "Akses lokasi dimatikan"
    override val locationOffBody = "Mizan tidak dibenarkan menggunakan lokasi anda. Anda boleh membenarkannya dalam Tetapan, atau pilih bandar anda daripada senarai."
    override val notifyOffTitle = "Pemberitahuan dimatikan"
    override val notifyOffBody = "Mizan tidak dibenarkan menghantar pemberitahuan, jadi peringatan azan tidak dapat sampai kepada anda. Anda boleh membenarkannya dalam Tetapan."
    override val clear = "Kosongkan"
    override val close = "Tutup"
    // ---------- iPhone: Lock Screen and Dynamic Island ----------
    override val liveCountdown = "Kiraan detik langsung di Skrin Kunci"
    override val liveCountdownDesc = "Apabila anda keluar dari Mizan kurang daripada 3 jam sebelum solat, kiraan detiknya dipaparkan di Skrin Kunci dan Dynamic Island."
    override val liveActivitiesOff = "Aktiviti Langsung dimatikan untuk Mizan. Hidupkannya dalam Tetapan › Mizan."
}
