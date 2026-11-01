package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Bahasa Indonesia tidak mengubah kata benda setelah bilangan: "1 hari", "5 hari" */
private fun idN(n: Long, unit: String) = "$n $unit"

object Id : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Alat bantu Muslim"
    override val tabHome = "Beranda"
    override val tabPrayer = "Salat"
    override val tabQibla = "Kiblat"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Setelan"
    override val back = "Kembali"
    override val share = "Bagikan"
    override val shareSignature = "— Aplikasi Mizan"
    override val today = "Hari ini"
    override val tomorrow = "Besok"
    override fun inDays(n: Long) = idN(n, "hari") + " lagi"
    override fun daysCount(n: Long) = idN(n, "hari")
    override val decrease = "Kurangi"
    override val increase = "Tambah"
    override val noLimit = "Tanpa batas"
    override val noResults = "Tidak ada hasil"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jumadilawal", "Jumadilakhir",
        "Rajab", "Syakban", "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
    )
    override val gregMonths = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    override val weekdays = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    override val weekdaysShort = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y H"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y H"
    override val hijriYearSuffix = "H"
    override val gregYearSuffix = "M"
    override val dayLabel = "Tanggal"
    override val monthLabel = "Bulan"
    override val yearLabel = "Tahun"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Kalkulator Zakat", "Uang, emas, dan saham beserta nisabnya"),
        "inheritance" to Named("Kalkulator Waris", "Pembagian harta warisan sesuai ilmu faraid"),
        "prayer" to Named("Jadwal Salat", "Pengingat azan dan hitung mundur langsung"),
        "qibla" to Named("Arah Kiblat", "Kompas dengan sensor ponsel Anda"),
        "hijri" to Named("Tanggal Hijriah", "Konversi tanggal, kalender Ummul Qura"),
        "events" to Named("Hari Besar Islam", "Ramadan, dua hari raya, dan Arafah"),
        "tasbih" to Named("Tasbih", "Penghitung zikir dengan bacaan siap pakai"),
        "adhkar" to Named("Zikir & Doa", "Hisnul Muslim dan zikir harian, beserta dalilnya"),
        "fitr" to Named("Zakat Fitrah", "Dengan sha' atau dengan uang"),
        "age" to Named("Kalkulator Usia", "Dalam tahun Hijriah dan Masehi"),
        "fidya" to Named("Fidyah & Kafarat", "Utang puasa dan pelanggaran sumpah"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakat zakah sedekah harta mal maal emas perak nisab uang tabungan saham",
        "inheritance" to "waris warisan ahli faraid faraidh mawaris pusaka tirkah wasiat",
        "prayer" to "salat shalat sholat solat azan adzan subuh zuhur dzuhur dhuhur asar ashar magrib maghrib isya jadwal waktu",
        "qibla" to "kiblat qiblat kakbah ka'bah kabah mekkah makkah kompas arah",
        "hijri" to "kalender tanggal hijriah hijriyah hijri masehi konversi islam ummul qura",
        "events" to "ramadan ramadhan idulfitri idul fitri lebaran iduladha idul adha arafah asyura hari raya libur",
        "tasbih" to "zikir dzikir tasbih penghitung hitungan biji wirid",
        "adhkar" to "zikir dzikir doa hisnul muslim pagi petang tidur ramadhan adzkar",
        "fitr" to "fitrah fitri sha' sa beras idulfitri lebaran",
        "age" to "umur usia ulang tahun lahir kelahiran",
        "fidya" to "fidyah fidya kafarat kaffarah kafarah sumpah puasa memberi makan",
    )
    override val toolsTitle = "Fitur"
    override fun nextPrayerIn(city: String) = "Salat berikutnya · $city"
    override val searchHint = "Cari fitur, kota, zikir…"

    // ---------- Search ----------
    override val searchTitle = "Cari"
    override val searchToolsGroup = "Fitur"
    override val searchCitiesGroup = "Kota"
    override val searchPrayersGroup = "Jadwal salat hari ini"
    override val searchEventsGroup = "Hari besar"
    override val searchDhikrGroup = "Zikir"
    override val searchSettingsGroup = "Setelan"
    override val searchEmpty = "Cari fitur, kota, zikir, hari besar, dan setelan"
    override val setAsMyCity = "Gunakan untuk jadwal salat"
    override fun searchNoResults(q: String) = "Tidak ada hasil untuk “$q”"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Subuh", "sunrise" to "Terbit", "dhuhr" to "Zuhur",
        "asr" to "Asar", "maghrib" to "Magrib", "isha" to "Isya"
    )
    override val prayerTitle = "Jadwal Salat"
    override fun nextPrayerLabel(name: String) = "Salat berikutnya: $name"
    override fun atTime(time: String) = "pukul $time"
    override val prevDay = "Hari sebelumnya"
    override val nextDay = "Hari berikutnya"
    override val monthTable = "Jadwal bulanan"
    override val hideMonthTable = "Sembunyikan jadwal"
    override fun monthTableTitle(m: Int, y: Int) = "Jadwal ${gregMonths[m - 1]} $y"
    override val colDay = "Tgl"
    override val locationAndMethod = "Lokasi & metode hitung"
    override val useMyLocation = "Gunakan lokasi saya saat ini"
    override val locating = "Mencari lokasi Anda…"
    override val orPickCity = "Atau pilih kota"
    override val pickCity = "Pilih kota"
    override val searchCity = "Cari kota"
    override val myLocation = "Lokasi saya"
    override val calcMethod = "Metode perhitungan"
    override val methods = mapOf(
        "makkah" to "Ummul Qura (Arab Saudi)",
        "mwl" to "Liga Muslim Dunia",
        "egypt" to "Otoritas Survei Umum Mesir",
        "dubai" to "UEA (Dubai)",
        "kuwait" to "Kuwait",
        "qatar" to "Qatar",
        "jordan" to "Yordania & Palestina",
        "morocco" to "Maroko",
        "algeria" to "Aljazair",
        "tunisia" to "Tunisia",
        "karachi" to "Universitas Ilmu-Ilmu Islam, Karachi",
        "turkey" to "Diyanet (Turki)",
        "tehran" to "Institut Geofisika, Universitas Teheran",
        "indonesia" to "Kementerian Agama RI",
        "malaysia" to "JAKIM (Malaysia)",
        "singapore" to "MUIS (Singapura)",
        "russia" to "Dewan Spiritual Muslim Rusia",
        "isna" to "Amerika Utara (ISNA)",
        "france" to "Persatuan Organisasi Islam Prancis",
    )
    override val asrMethod = "Perhitungan Asar"
    override val asrStandard = "Jumhur (Syafi'i, Maliki, Hambali)"
    override val asrHanafi = "Hanafi"
    override val latLng = "Lintang / bujur"
    override val adhanAlert = "Pengingat azan"
    override val adhanAlertDesc = "Notifikasi saat setiap waktu salat tiba"
    override val timesMayDiffer = "Waktu bisa berbeda satu atau dua menit dari jadwal resmi di negara Anda."
    override fun endsAt(t: String) = "Berakhir $t"
    override fun iqamaAt(t: String) = "Ikamah $t"
    override val iqamaTitle = "Waktu ikamah"
    override val iqamaDesc = "Menit antara azan dan ikamah di masjid Anda; 0 menyembunyikannya."
    override val prayerEndsNote = "Waktu Subuh berakhir saat matahari terbit, Zuhur saat masuk Asar, Asar saat matahari terbenam (menundanya hingga matahari menguning makruh), Magrib saat mega merah hilang, dan waktu ikhtiar Isya berakhir pada tengah malam, sedangkan waktu daruratnya hingga Subuh."
    override val locationFound = "Lokasi ditemukan ✓"
    override val locationFailed = "Gagal mendapatkan lokasi Anda. Pastikan layanan lokasi aktif."
    override val locationDenied = "Izin lokasi tidak diberikan. Pilih kota Anda dari daftar."
    override val cities = mapOf(
        "makkah" to "Mekkah", "madinah" to "Madinah", "riyadh" to "Riyadh", "jeddah" to "Jeddah",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubai", "abudhabi" to "Abu Dhabi", "sharjah" to "Sharjah",
        "kuwait" to "Kota Kuwait", "doha" to "Doha", "manama" to "Manama", "muscat" to "Muskat",
        "cairo" to "Kairo", "alexandria" to "Iskandariyah", "aswan" to "Aswan", "amman" to "Amman",
        "jerusalem" to "Yerusalem (Al-Quds)", "gaza" to "Gaza", "beirut" to "Beirut", "damascus" to "Damaskus", "aleppo" to "Aleppo",
        "baghdad" to "Bagdad", "basra" to "Basra", "erbil" to "Erbil", "sanaa" to "Sana'a", "aden" to "Aden",
        "khartoum" to "Khartoum", "tripoli" to "Tripoli", "benghazi" to "Benghazi", "tunis" to "Tunis",
        "algiers" to "Aljir", "oran" to "Oran", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakesh", "nouakchott" to "Nouakchott", "mogadishu" to "Mogadishu", "djibouti" to "Djibouti",
        "istanbul" to "Istanbul", "ankara" to "Ankara", "karachi" to "Karachi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Mumbai", "hyderabad" to "Hyderabad",
        "dhaka" to "Dhaka", "chittagong" to "Chittagong", "kabul" to "Kabul", "tehran" to "Teheran", "mashhad" to "Masyhad",
        "tashkent" to "Tashkent", "baku" to "Baku", "almaty" to "Almaty", "moscow" to "Moskwa", "kazan" to "Kazan",
        "grozny" to "Grozny", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapura",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Jakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "London", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marseille", "lyon" to "Lyon", "brussels" to "Brussel", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hamburg", "cologne" to "Köln", "munich" to "München",
        "vienna" to "Wina", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Chicago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sydney",
    )
    override val palestine = "Palestina"

    // ---------- Notifications ----------
    override val channelName = "Jadwal salat"
    override val channelDesc = "Pengingat saat setiap waktu salat tiba"
    override fun prayerNowTitle(prayer: String) = "Waktunya salat $prayer"
    override fun prayerNowText(city: String) = "$city — marilah salat"
    // ---------- Adhan & alerts ----------
    override val adhanSettings = "Azan & pengingat"
    override val adhanSettingsSub = "Muazin, pengingat tiap salat, dan getaran"
    override val alertPerPrayer = "Pengingat tiap salat"
    override val alertModeAdhan = "Azan"
    override val alertModeVibrate = "Getar saja"
    override val alertModeSilent = "Notifikasi senyap"
    override val alertModeOff = "Tanpa pengingat"
    override val alertModesHint = "“Azan” memutar azan lengkap, “Getar saja” bergetar tanpa suara, “Notifikasi senyap” menampilkan notifikasi tanpa suara, dan “Tanpa pengingat” tidak mengirim apa pun untuk salat itu."
    override val muezzinTitle = "Muazin"
    override val fajrAdhanTitle = "Azan Subuh"
    override val fajrAdhanHint = "Azan Subuh ditambah “Ash-shalatu khairun minan naum” (salat lebih baik daripada tidur)."
    override val fajrSameAsOthers = "Sama dengan salat lainnya"
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
    override val playAdhan = "Putar azan"
    override val stopAdhan = "Hentikan"
    override val vibrationTitle = "Pola getaran"
    override val vibeNames = mapOf("short" to "Pendek", "double" to "Ketuk dua kali", "triple" to "Ketuk tiga kali", "long" to "Panjang", "heartbeat" to "Detak jantung", "pulse" to "3 denyut", "wave" to "Gelombang", "rise" to "Makin kuat", "gentle" to "Lembut", "strong" to "Kuat", "knock" to "Ketukan pintu", "rapid" to "Cepat")
    override val tryVibration = "Coba getaran"
    override val adhanStopHint = "Ketuk “Hentikan” di notifikasi atau tekan tombol volume untuk menghentikan azan. Jika ponsel dalam mode senyap atau getar, pengingat mengikuti mode itu."
    override val adhanIosNote = "iPhone membatasi suara notifikasi hingga 30 detik, jadi notifikasi memutar awal azan; azan lengkap diputar jika Mizan terbuka saat waktu salat. Notifikasi bergetar dengan pola iPhone sendiri (Pengaturan › Suara & Haptik); pola di sini terasa saat Mizan terbuka."
    override val recordingSources = "Sumber rekaman"

    // ---------- Qibla ----------
    override val qiblaTitle = "Arah Kiblat"
    override val noCompass = "Perangkat Anda tidak memiliki kompas. Arahkan secara manual sesuai sudut di bawah."
    override val facingQibla = "✓ Anda sudah menghadap kiblat"
    override fun turnRight(deg: Int) = "Putar ponsel ke kanan $deg°"
    override fun turnLeft(deg: Int) = "Putar ponsel ke kiri $deg°"
    override val qiblaAngle = "Sudut kiblat dari utara sejati"
    override fun towards(dir: String) = "Ke arah $dir"
    override val directions = listOf("utara", "timur laut", "timur", "tenggara", "selatan", "barat daya", "barat", "barat laut")
    override val location = "Lokasi"
    override val distanceToKaaba = "Jarak ke Kakbah"
    override val phoneHeading = "Arah ponsel"
    override val compassLowAccuracy = "Akurasi kompas rendah: gerakkan ponsel membentuk angka 8 untuk kalibrasi, jauh dari logam dan perangkat elektronik."
    override val qiblaTip = "Untuk mengubah lokasi, gunakan halaman Jadwal Salat. Pegang ponsel mendatar dan jauh dari logam agar hasilnya akurat."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Tasbih Digital"
    override val tasbihSubtitle = "Ketuk lingkaran untuk menghitung"
    override val modeFree = "Zikir bebas"
    override val modeAfterPrayer = "Setelah salat"
    override fun ofGoal(n: Int) = "dari $n"
    override val nextDhikr = "Lanjut ke zikir berikutnya"
    override val completedHundred = "Semoga Allah menerima amal Anda ✓ Genap seratus"
    override fun completedN(n: Int) = "Selesai $n ✓"
    override val reset = "Reset"
    override val undo = "Urungkan"
    override val chooseDhikr = "Pilih zikir"
    override val targetCount = "Jumlah target"
    override val yourStats = "Statistik Anda"
    override val roundsDone = "putaran selesai"
    override val todayCount = "hari ini"
    override val totalCount = "total"
    override val dhikrLabels = listOf(
        "Subhanallah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah",
        "Subhanallahi wa bihamdihi", "Subhanallahil 'azhim", "La haula wa la quwwata illa billah", "Selawat Nabi ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Mahasuci Allah",
        "Segala puji bagi Allah",
        "Allah Mahabesar",
        "Tiada tuhan selain Allah",
        "Aku memohon ampun kepada Allah",
        "Mahasuci Allah dan segala puji bagi-Nya",
        "Mahasuci Allah Yang Mahaagung",
        "Tiada daya dan kekuatan kecuali dengan pertolongan Allah",
        "Ya Allah, limpahkanlah selawat kepada Muhammad",
    )
    override val tamamMeaning = "Tiada tuhan selain Allah semata, tiada sekutu bagi-Nya. Milik-Nya kerajaan dan milik-Nya segala puji, dan Dia Mahakuasa atas segala sesuatu."
    override val tapToCount = "Ketuk untuk menghitung"

    // ---------- Zakat ----------
    override val zakatTitle = "Kalkulator Zakat"
    override val zakatSubtitle = "2,5% dari harta yang mencapai nisab dan genap setahun (haul)"
    override val partCash = "Uang tunai & saldo"
    override val partReceivables = "Piutang yang bisa ditagih"
    override val partGold = "Emas"
    override val partSilver = "Perak"
    override val partStocks = "Saham"
    override val partTrade = "Barang dagangan"
    override val enterGoldPrice = "Masukkan harga emas untuk menghitung nisab"
    override val enterSilverPrice = "Masukkan harga perak untuk menghitung nisab"
    override val nisabReached = "✓ Harta Anda mencapai nisab — wajib zakat"
    override val belowNisab = "Di bawah nisab — tidak wajib zakat"
    override val enterAssets = "Masukkan harta Anda"
    override val zakatDue = "Zakat yang wajib dibayar"
    override val totalAssets = "Total harta wajib zakat"
    override val deductedDebts = "Utang yang dikurangkan"
    override val netBase = "Harta bersih wajib zakat"
    override val nisabValue = "Nilai nisab"
    override val approxEquals = "Setara sekitar"
    override fun goldGrams24(g: String) = "$g g emas 24 karat"
    override val shareResult = "Bagikan hasil"
    override fun zakatShareText(zakat: String, base: String) = "Zakat saya: $zakat (harta wajib zakat $base)"
    override val settingsAndPrices = "Setelan & harga"
    override val currency = "Mata uang"
    override val nisabBasis = "Nisab untuk uang"
    override val nisabGold = "Emas (85 g)"
    override val nisabSilver = "Perak (595 g)"
    override val goldPrice24 = "Harga 1 g emas 24 karat"
    override val goldPriceHint = "Dari toko emas atau situs harga tepercaya di negara Anda"
    override val silverPrice = "Harga 1 g perak (opsional)"
    override val cashSection = "Uang tunai & saldo"
    override val cashInHand = "Uang tunai di tangan"
    override val bankBalances = "Saldo bank dan dompet digital"
    override val receivablesField = "Piutang yang Anda harapkan kembali"
    override val otherMoney = "Uang lainnya"
    override val goldSilverSection = "Emas & perak (dalam gram)"
    override fun goldKarat(k: Int) = "Emas $k karat"
    override val pureSilver = "Perak murni"
    override val includeJewelry = "Sertakan perhiasan yang dipakai (pendapat Hanafi, dan lebih hati-hati)"
    override val jewelryField = "Di antaranya, perhiasan yang dipakai (setara 21 karat)"
    override val jewelryHint = "Dikecualikan menurut jumhur: perhiasan untuk dipakai sendiri tidak dizakati"
    override val investSection = "Investasi & perdagangan"
    override val stocksTrade = "Saham untuk diperdagangkan (nilai pasar)"
    override val stocksLong = "Saham jangka panjang (bagian wajib zakat)"
    override val tradeGoods = "Barang dagangan (nilai stok)"
    override val deductSection = "Pengurang"
    override val debtsField = "Utang Anda yang jatuh tempo tahun ini"
    override val debtsHint = "Hanya cicilan yang jatuh tempo dalam tahun zakat"
    override val breakdownTitle = "Zakat per jenis harta"
    override val zakatNote = "Data Anda tetap di perangkat Anda. Allah menyebut delapan golongan penerima zakat (asnaf) dalam Surah At-Taubah (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat Fitrah"
    override val fitrSubtitle = "Ditunaikan sebelum salat Id"
    override val foodAmountDue = "Jumlah makanan yang dikeluarkan"
    override val totalCashValue = "Total nilai uang"
    override fun forPeople(n: Int) = "untuk " + idN(n.toLong(), "orang")
    override val peopleCount = "Jumlah jiwa yang ditanggung (termasuk Anda)"
    override val modeFood = "Makanan (sha')"
    override val modeMoney = "Nilai uang"
    override val foodType = "Jenis makanan"
    override val foods = mapOf(
        "rice" to "Beras", "riceSafe" to "Beras (lebih hati-hati)", "wheat" to "Gandum",
        "dates" to "Kurma", "barley" to "Jelai", "other" to "Takaran lain…",
    )
    override fun approxKg(kg: String) = "sekitar $kg kg"
    override val saWeight = "Berat satu sha' dalam kg"
    override val kgPrice = "Harga per kg (opsional)"
    override val kgPriceHint = "Untuk memperkirakan biaya makanan"
    override val saPerPerson = "Satu sha' per orang"
    override val approxCost = "Perkiraan biaya"
    override val valuePerPerson = "Nilai zakat fitrah per jiwa"
    override val valuePerPersonHint = "Ditetapkan setiap tahun oleh lembaga resmi di negara Anda"
    override val valueNote = "Membayar dengan uang adalah pendapat Hanafi; jumhur ulama mengeluarkan makanan."
    override val fitrTimingNote = "Waktu penunaian: sebelum salat Id. Boleh didahulukan sehari atau dua hari, tetapi tidak boleh ditunda melewati salat tanpa uzur. Diberikan kepada fakir dan miskin."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidyah & Kafarat"
    override val fidyaSubtitle = "Takaran makanan dalam kilogram dan nilainya"
    override val tabFidya = "Fidyah"
    override val tabOath = "Sumpah"
    override val tabIntercourse = "Jimak"
    override val fidyaResult = "Fidyah puasa yang ditinggalkan"
    override fun fidyaSub(days: Int) = "Untuk " + idN(days.toLong(), "hari") + " — satu orang miskin per hari"
    override val fidyaNote = "Bagi yang tidak mampu berpuasa secara permanen, seperti lansia atau orang sakit menahun. Boleh memberi makan satu orang miskin untuk semua hari, atau beberapa orang."
    override val oathResult = "Kafarat melanggar sumpah"
    override fun oathSub(oaths: Int) = if (oaths > 1) idN(oaths.toLong(), "kafarat") + " × 10 orang miskin" else "Memberi makan 10 orang miskin"
    override val oathAlternative = "Jika tidak mampu"
    override fun fastDays(n: Int) = "Puasa " + idN(n.toLong(), "hari")
    override val oathNote = "Pilih antara memberi makan sepuluh orang miskin, memberi mereka pakaian, atau memerdekakan budak; yang tidak mampu, berpuasa tiga hari (Surah Al-Ma'idah 5:89)."
    override val intercourseResult = "Kafarat jimak di siang hari Ramadan"
    override val intercourseSub = "Memberi makan 60 orang miskin jika tidak mampu berpuasa"
    override val ifStartToday = "Jika mulai berpuasa hari ini, selesai pada"
    override val twoMonthsDuration = "Lama dua bulan Hijriah"
    override val intercourseNote = "Secara berurutan: memerdekakan budak; jika tidak bisa, berpuasa dua bulan berturut-turut; jika tidak mampu, memberi makan enam puluh orang miskin — disertai mengqada puasa hari itu dan bertobat."
    override val daysField = "Jumlah hari"
    override val oathsField = "Jumlah kafarat"
    override val feedMeasure = "Makanan per orang miskin"
    override val muddOption = "Satu mud — sekitar 0,75 kg (Maliki & Syafi'i)"
    override val halfSaOption = "Setengah sha' — sekitar 1,5 kg (fatwa kontemporer)"
    override val poorCount = "Orang miskin / porsi makan"
    override val approxValue = "Perkiraan nilai"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Tanggal Hijriah"
    override val hijriSubtitle = "Kalender Ummul Qura"
    override val correspondingHijri = "Tanggal Hijriah yang sesuai"
    override val correspondingGreg = "Tanggal Masehi yang sesuai"
    override val enterGregYear = "Masukkan tahun antara 1882 dan 2174"
    override val enterHijriYear = "Masukkan tahun Hijriah antara 1300 dan 1600"
    override val gregToHijri = "Masehi → Hijriah"
    override val hijriToGreg = "Hijriah → Masehi"
    override val todayDate = "Tanggal hari ini"
    override val prevMonth = "Bulan sebelumnya"
    override val nextMonth = "Bulan berikutnya"
    override val calendarHint = "Angka besar adalah tanggal Hijriah, angka kecil Masehi. Ketuk tanggal untuk mengonversinya."
    override val moonNote = "Tanggal bisa berbeda satu hari di beberapa negara karena perbedaan rukyat hilal."

    // ---------- Age ----------
    override val ageTitle = "Kalkulator Usia"
    override val ageSubtitle = "Dalam tahun Hijriah dan Masehi"
    override val ageGregorian = "Usia Anda (Masehi)"
    override fun ymd(y: Int, m: Int, d: Int) = "${idN(y.toLong(), "tahun")} ${idN(m.toLong(), "bulan")} ${idN(d.toLong(), "hari")}"
    override fun inHijri(age: String) = "Hijriah: $age"
    override val birthGreg = "Tanggal lahir (Masehi)"
    override val birthHijri = "Tanggal lahir (Hijriah)"
    override val birthWeekday = "Hari lahir"
    override val nextBirthday = "Ulang tahun berikutnya"
    override val birthdayToday = "Hari ini 🎉"
    override val tileDays = "hari"
    override val tileWeeks = "minggu"
    override val tileHours = "jam"
    override val yourAge = "Usia Anda"
    override val birthAfterToday = "Tanggal lahir setelah hari ini"
    override val enterBirth = "Masukkan tanggal lahir Anda"
    override val birthDate = "Tanggal lahir"
    override val gregorian = "Masehi"
    override val hijri = "Hijriah"

    // ---------- Islamic events ----------
    override val eventsTitle = "Hari Besar Islam"
    override val eventsSubtitle = "Hitung mundur menurut kalender Ummul Qura"
    override val events = mapOf(
        "ramadan" to Named("Ramadan", "Bulan puasa, salat malam, dan tilawah Al-Qur'an."),
        "lastTen" to Named("Sepuluh malam terakhir", "Di dalamnya ada Lailatul Qadar, lebih baik daripada seribu bulan."),
        "eidFitr" to Named("Idulfitri", "Zakat fitrah ditunaikan sebelum salat Idulfitri."),
        "shawwal" to Named("Puasa enam hari Syawal", "Barang siapa berpuasa Ramadan lalu mengiringinya dengan enam hari di bulan Syawal, maka seperti berpuasa sepanjang masa (HR. Muslim)."),
        "dhulHijjah" to Named("Sepuluh hari awal Zulhijah", "Hari-hari terbaik di dunia; dianjurkan memperbanyak amal saleh dan takbir."),
        "arafah" to Named("Hari Arafah", "Puasanya menghapus dosa setahun yang lalu dan setahun yang akan datang, bagi yang tidak berhaji (HR. Muslim)."),
        "eidAdha" to Named("Iduladha", "Hari penyembelihan kurban, diikuti tiga hari Tasyrik."),
        "newYear" to Named("Tahun Baru Islam", "Awal tahun Hijriah yang baru."),
        "ashura" to Named("Hari Asyura", "Puasanya menghapus dosa setahun yang lalu (HR. Muslim); dianjurkan pula berpuasa pada tanggal 9."),
    )
    override val unitDays = "hari"
    override val unitHours = "jam"
    override val unitMinutes = "menit"
    override val unitSeconds = "detik"
    override val eventsNote = "Tanggal bersifat perkiraan (Ummul Qura) dan bisa berbeda satu hari sesuai rukyat hilal di negara Anda."

    // ---------- Inheritance ----------
    override val inhTitle = "Kalkulator Waris"
    override val inhSubtitle = "Pembagian harta warisan sesuai ilmu faraid"
    override val aslLabel = "Asal masalah"
    override val awlCase = "Masalah 'aul (bagian melebihi harta)"
    override val raddCase = "Masalah radd (sisa dikembalikan)"
    override val adilCase = "Masalah 'adilah (seimbang)"
    override fun tashihFrom(n: Long) = "Ditashih menjadi " + idN(n, "bagian")
    override val sharesTitle = "Bagian tiap ahli waris"
    override fun blockedBy(by: String) = "Terhalang oleh $by"
    override fun perPerson(v: String) = "masing-masing $v"
    override val shareDivision = "Bagikan pembagian waris"
    override fun divisionShareText(list: String) = "Pembagian warisan: $list"
    override val listSeparator = ", "
    override val addHeirs = "Tambahkan ahli waris dari daftar di bawah"
    override val deceasedAndEstate = "Pewaris & harta peninggalan"
    override val deceasedMale = "Pewaris laki-laki"
    override val deceasedFemale = "Pewaris perempuan"
    override val estateTotal = "Total harta peninggalan"
    override val funeralCosts = "Biaya pengurusan jenazah dan pemakaman"
    override val deceasedDebts = "Utang pewaris"
    override val bequest = "Wasiat (untuk bukan ahli waris)"
    override val bequestHint = "Tidak lebih dari sepertiga sisa setelah utang, kecuali disetujui ahli waris"
    override val netEstate = "Harta bersih yang dibagi"
    override val bequestCapped = "(wasiat dibatasi sepertiga)"
    override val newCase = "Mulai perhitungan baru"
    override val inhDisclaimer = "Hasil ini bersifat panduan berdasarkan pendapat jumhur. Tidak menggantikan pengadilan agama atau ahli faraid."
    override val heirs = mapOf(
        "husband" to "Suami", "wife" to "Istri", "father" to "Ayah", "mother" to "Ibu",
        "gf" to "Kakek (dari ayah)", "gmm" to "Nenek (dari ibu)", "gmf" to "Nenek (dari ayah)",
        "son" to "Anak laki-laki", "daughter" to "Anak perempuan", "gson" to "Cucu laki-laki dari putra", "gdau" to "Cucu perempuan dari putra",
        "fb" to "Saudara laki-laki kandung", "fs" to "Saudara perempuan kandung", "pb" to "Saudara laki-laki seayah", "ps" to "Saudara perempuan seayah",
        "mb" to "Saudara laki-laki seibu", "ms" to "Saudara perempuan seibu", "fnep" to "Putra saudara laki-laki kandung", "pnep" to "Putra saudara laki-laki seayah",
        "fun" to "Paman kandung (saudara ayah)", "pun" to "Paman seayah (saudara ayah)", "fcous" to "Putra paman kandung", "pcous" to "Putra paman seayah",
    )
    override val heirGroups = mapOf(
        "spouses" to "Suami/istri", "ascendants" to "Orang tua & kakek-nenek", "descendants" to "Anak & cucu",
        "siblings" to "Saudara & saudari", "distant" to "Kerabat laki-laki jauh",
    )
    override val blockers = mapOf(
        "gf" to "kakek",
        "twoDaughters" to "dua anak perempuan (dua pertiga telah genap)",
        "descendant" to "keturunan yang mewarisi",
        "fbAfterMuadda" to "saudara laki-laki kandung (setelah mu'addah)",
        "fsTookRest" to "saudara perempuan kandung yang mengambil sisa",
        "gfAndSiblings" to "kakek dan para saudara",
        "twoFullSisters" to "dua saudara perempuan kandung",
        "fsGhayr" to "saudara perempuan kandung ('asabah bersama anak perempuan)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Bagian pasti"
        ShareType.FARD_TASIB -> "Bagian pasti + 'asabah"
        ShareType.FARD_TAKMILA -> "Bagian pasti (melengkapi dua pertiga)"
        ShareType.TASIB -> "'Asabah"
        ShareType.TASIB_BIL_GHAYR -> "'Asabah melalui saudara laki-laki"
        ShareType.TASIB_MAA_GHAYR -> "'Asabah bersama anak perempuan"
        ShareType.TASIB_MAA_JADD -> "'Asabah bersama kakek"
        ShareType.FARD_THEN_MUQASAMA -> "Bagian pasti, lalu muqasamah"
        ShareType.JADD_MUQASAMA -> "Bersama saudara: muqasamah"
        ShareType.JADD_THIRD_ESTATE -> "Bersama saudara: sepertiga harta"
        ShareType.JADD_THIRD_REST -> "Bersama saudara: sepertiga sisa"
        ShareType.JADD_SIXTH -> "Bersama saudara: seperenam harta"
    } + if (radd) " + radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Saudara seibu berbagi sepertiga sama rata, laki-laki dan perempuan sama besar."
        InhNote.Umariyya -> "Ini salah satu dari dua masalah 'Umariyyah (Gharrawain): ibu mendapat sepertiga sisa setelah bagian suami/istri."
        InhNote.GrandmothersShare -> "Kedua nenek berbagi seperenam."
        InhNote.FullSistersGhayr -> "Saudara perempuan kandung menjadi 'asabah bersama anak perempuan."
        InhNote.Akdariyya -> "Ini masalah Akdariyah: saudara perempuan diberi setengah dan kakek seperenam, sehingga masalah naik ('aul) menjadi 9; lalu bagian keduanya digabung dan dibagi dua banding satu, sehingga menjadi 27."
        is InhNote.JaddRule -> "Bersama saudara, kakek mengambil yang paling menguntungkan baginya: muqasamah, atau " +
            (if (n.otherFard) "sepertiga sisa, atau seperenam harta" else "sepertiga harta") +
            " — pendapat jumhur (Maliki, Syafi'i, Hambali). Abu Hanifah berpendapat kakek menghalangi saudara seperti ayah."
        InhNote.Muadda -> "Saudara seayah diperhitungkan terhadap kakek (mu'addah), lalu bagian mereka dikembalikan kepada saudara kandung bila diperlukan."
        is InhNote.NothingLeft -> "Tidak ada sisa setelah bagian pasti, sehingga ahli waris 'asabah (${nameOf(n.winner)}) tidak mendapat apa-apa."
        InhNote.Mushtaraka -> "Ini masalah Musytarakah (Himariyah): Malikiyah dan Syafi'iyah menyertakan saudara kandung bersama saudara seibu dalam sepertiga; hasil di sini mengikuti pendapat Hanafiyah dan Hanabilah."
        is InhNote.Awl -> "Jumlah bagian pasti melebihi asal masalah, sehingga dinaikkan dari ${n.asl} menjadi ${n.awl} ('aul) dan setiap bagian berkurang secara proporsional."
        InhNote.Radd -> "Tidak ada ahli waris 'asabah, sehingga sisa harta dikembalikan kepada ashabul furud sesuai proporsi bagiannya, kecuali suami/istri."
        InhNote.SpouseOnlyRadd -> "Ahli waris satu-satunya adalah suami/istri: banyak ulama kontemporer dan sejumlah undang-undang keluarga mengembalikan sisa kepadanya; ulama terdahulu memberikannya kepada zawil arham, lalu baitulmal."
        InhNote.NoHeirs -> "Tidak ada ashabul furud maupun 'asabah; harta diberikan kepada zawil arham, lalu baitulmal. Rujuklah ke pengadilan yang berwenang."
    }

    // ---------- Quran ----------
    override val tabQuran = "Al-Qur'an"
    override val quranTitle = "Al-Qur'an Al-Karim"
    override val quranSubtitle = "Ketuk ayat mana pun untuk membaca tafsirnya"
    override val surahsTab = "Surah"
    override val juzTab = "Juz"
    override val bookmarksTab = "Tersimpan"
    override val continueReading = "Lanjutkan membaca"
    override val startReading = "Mulai membaca"
    override val meccan = "Makkiyah"
    override val medinan = "Madaniyah"
    override fun ayahCount(n: Int) = idN(n.toLong(), "ayat")
    override fun juzName(n: Int) = "Juz $n"
    override fun pageName(n: Int) = "Halaman $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · Ayat $ayah"
    override val surahNames = listOf(
        "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa'", "Al-Ma'idah", "Al-An'am",
        "Al-A'raf", "Al-Anfal", "At-Taubah", "Yunus", "Hud", "Yusuf",
        "Ar-Ra'd", "Ibrahim", "Al-Hijr", "An-Nahl", "Al-Isra'", "Al-Kahf",
        "Maryam", "Taha", "Al-Anbiya'", "Al-Hajj", "Al-Mu'minun", "An-Nur",
        "Al-Furqan", "Asy-Syu'ara'", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba'", "Fatir", "Yasin",
        "As-Saffat", "Sad", "Az-Zumar", "Gafir", "Fussilat", "Asy-Syura",
        "Az-Zukhruf", "Ad-Dukhan", "Al-Jasiyah", "Al-Ahqaf", "Muhammad", "Al-Fath",
        "Al-Hujurat", "Qaf", "Az-Zariyat", "At-Tur", "An-Najm", "Al-Qamar",
        "Ar-Rahman", "Al-Waqi'ah", "Al-Hadid", "Al-Mujadilah", "Al-Hasyr", "Al-Mumtahanah",
        "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Tagabun", "At-Talaq", "At-Tahrim",
        "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij", "Nuh", "Al-Jinn",
        "Al-Muzzammil", "Al-Muddassir", "Al-Qiyamah", "Al-Insan", "Al-Mursalat", "An-Naba'",
        "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Insyiqaq",
        "Al-Buruj", "At-Tariq", "Al-A'la", "Al-Gasyiyah", "Al-Fajr", "Al-Balad",
        "Asy-Syams", "Al-Lail", "Ad-Duha", "Asy-Syarh", "At-Tin", "Al-'Alaq",
        "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat", "Al-Qari'ah", "At-Takasur",
        "Al-'Asr", "Al-Humazah", "Al-Fil", "Quraisy", "Al-Ma'un", "Al-Kausar",
        "Al-Kafirun", "An-Nasr", "Al-Lahab", "Al-Ikhlas", "Al-Falaq", "An-Nas",
    )
    override val searchQuranHint = "Cari surah atau kata dalam ayat…"
    override val searchQuranGroup = "Surah"
    override val searchAyahsGroup = "Ayat"
    override val mushafView = "Tampilan mushaf"
    override val ayahView = "Ayat per ayat"
    override val textLarger = "Perbesar teks"
    override val textSmaller = "Perkecil teks"
    override val tafsirTitle = "Tafsir"
    override val tafsirMuyassar = "Tafsir Al-Muyassar"
    override val tafsirJalalayn = "Tafsir Jalalain"
    override val translationOfMeanings = "Terjemahan makna"
    override val bookmarkAyah = "Simpan ayat"
    override val removeBookmark = "Hapus dari Tersimpan"
    override val copy = "Salin"
    override val copied = "Disalin ✓"
    override val prevAyah = "Ayat sebelumnya"
    override val nextAyah = "Ayat berikutnya"
    override val prevSurah = "Surah sebelumnya"
    override val nextSurah = "Surah berikutnya"
    override val noBookmarks = "Belum ada ayat tersimpan. Buka sebuah ayat, lalu ketuk ikon penanda untuk menyimpannya."
    override val quranLoading = "Membuka mushaf…"
    override fun hizbName(n: Int) = "Hizb $n"
    override val khatmaTitle = "Khataman saya"
    override val khatmaPlanLabel = "Selesai dalam"
    override fun planDays(days: Int) = if (days == 0) "Tanpa batas" else "$days hari"
    override fun pagesRead(read: Int, total: Int) = "$read / $total halaman"
    override val todayPortion = "Wirid hari ini"
    override fun streakDays(n: Int) = "Beruntun: $n hari"
    override fun daysLeftLabel(n: Int) = "Sisa hari: $n"
    override val portionDone = "Wirid hari ini selesai ✓"
    override val khatmaDone = "Khatam ✓ Semoga Allah menerimanya"
    override val newKhatma = "Mulai khataman baru"
    override fun khatmasCompleted(n: Int) = "Khatam selesai: $n"
    override val continuePortion = "Lanjutkan wirid"
    override val ayahOfDay = "Ayat hari ini"
    override val focusMode = "Mode fokus"
    override val exitFocus = "Keluar dari mode fokus"
    override val silenceTitle = "Senyapkan notifikasi saat membaca?"
    override val silenceBody = "Dalam mode fokus, Mizan dapat menyalakan Jangan Ganggu dan mematikannya lagi saat Anda selesai membaca. Android akan meminta izin sekali."
    override val allow = "Izinkan"
    override val notNow = "Nanti saja"
    override val tafsirLibrary = "Perpustakaan tafsir"
    override val tafsirLibrarySub = "Unduh tafsir dan pilih mana yang tampil saat Anda menyentuh ayat"
    override val onDevice = "Di perangkat ini"
    override val availableDownloads = "Tersedia untuk diunduh"
    override val download = "Unduh"
    override fun downloadingPct(pct: Int) = "Mengunduh… $pct%"
    override val cancel = "Batal"
    override val delete = "Hapus"
    override fun deleteTafsirQ(name: String) = "Hapus “$name” dari perangkat ini?"
    override val showInTafsir = "Tampilkan di tafsir"
    override val builtIn = "Bawaan"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "Gagal mengunduh. Periksa koneksi internet Anda."
    override val retry = "Coba lagi"
    override val moreTafsirs = "Tafsir lainnya"
    override val tafsirLibraryNote = "Tafsir diunduh dari koleksi terbuka tafsir_api (bersumber dari quran.com dan QUL). Internet hanya dipakai untuk unduhan ini; setelah itu tafsir bisa dibuka tanpa koneksi."
    override fun ayahRange(from: Int, to: Int) = "Tafsir ayat $from–$to"
    override val recitation = "Murotal"
    override val reciter = "Qari"
    override val playRecitation = "Putar murotal"
    override val pauseRecitation = "Jeda"
    override val stopRecitation = "Hentikan murotal"
    override val repeatAyah = "Ulangi setiap ayat"
    override val listenFromHere = "Dengarkan dari ayat ini"
    override val listen = "Dengar"
    override val save = "Simpan"
    override val saved = "Tersimpan"
    override val ayahActionsHint = "Tekan lama ayat mana pun untuk tafsir, mendengarkan, menyimpan, menyalin atau membagikan"
    override val audioNote = "Murotal diputar dari EveryAyah.com. Ayat yang sudah Anda dengarkan disimpan di perangkat dan bisa diputar tanpa internet."
    override val audioError = "Murotal tidak dapat diputar. Periksa koneksi internet Anda."
    override val hifzMode = "Hafalan"
    override val revealNext = "Tampilkan berikutnya"
    override val hideAll = "Sembunyikan semua"
    override val hifzHint = "Bacalah dari hafalan, lalu ketuk ayat yang tersembunyi untuk memeriksanya."
    override val displayTitle = "Tampilan"
    override val pageColor = "Warna halaman"
    override val toneAuto = "Otomatis"
    override val toneSepia = "Sepia"
    override val toneNight = "Malam"
    override val brightness = "Kecerahan"
    override val brightnessSystem = "Kecerahan perangkat"
    override val textSizeLabel = "Ukuran teks"
    override val continueFocus = "Lanjutkan dalam mode fokus"
    override val smartPresets = "Pengaturan cerdas"
    override val presetListen = "Dengar"
    override val presetMemorize = "Hafalan"
    override val presetReview = "Murajaah"
    override val smartHint = "Hafalan: setiap ayat 3 kali dengan jeda untuk mengulanginya setelah qari. Murajaah: seluruh surah, sedikit lebih cepat."
    override val range = "Rentang"
    override val rangeContinue = "Terus"
    override val rangePage = "Halaman ini"
    override val rangeSurah = "Surah ini"
    override val rangeCustom = "Kustom"
    override val fromAyah = "Dari"
    override val toAyah = "Sampai"
    override val repeatRange = "Ulangi rentang"
    override val pauseAfter = "Jeda setelah setiap ayat"
    override val off = "Mati"
    override val pauseAyahLength = "Sepanjang ayat"
    override fun seconds(n: Int) = "$n dtk"
    override val speed = "Kecepatan"
    override val sleepTimer = "Pengatur waktu tidur"
    override fun minutes(n: Int) = "$n mnt"
    override val evidenceTitle = "Dalil dan pendapat ulama"
    override val evidenceSub = "Sumber hukum ini: Al-Qur'an, Sunnah, dan mazhab-mazhab fikih"
    override val proofQuran = "Al-Qur'an"
    override val proofHadith = "Hadis"
    override val proofConsensus = "Ijmak"
    override val proofScholars = "Pendapat ulama"
    override val zakatTypesTitle = "Lima jenis zakat"
    override val zakatTypesSub = "Pilih jenis harta untuk menghitung zakatnya dan melihat dalilnya"
    override val zkMoneyName = "Uang, emas, dan perak"
    override val zkMoneyDesc = "Uang tunai, tabungan, emas, dan perak: 2,5% sekali setelah mencapai nisab selama setahun hijriah"
    override val zkTradeName = "Barang dagangan dan saham"
    override val zkTradeDesc = "Barang untuk dijual dan saham, dinilai di akhir tahun"
    override val zkLivestockName = "Hewan ternak"
    override val zkLivestockDesc = "Unta, sapi, kambing, dan domba yang digembalakan, setelah mencapai nisab"
    override val zkCropsName = "Hasil tanaman dan buah"
    override val zkCropsDesc = "Saat panen: sepersepuluh atau setengahnya, sesuai cara pengairan"
    override val zkMineralsName = "Barang tambang dan harta karun"
    override val zkMineralsDesc = "Seperlima dari harta karun, 2,5% dari barang tambang"
    override val zkCombinedNote = "Uang, emas, perak, dan barang dagangan digabung dalam satu nisab, sehingga dihitung bersama dalam satu halaman."
    override val camels = "Unta"
    override val cattle = "Sapi"
    override val sheepGoats = "Domba dan kambing"
    override val unitHead = "ekor"
    override val livestockDue = "Zakat hewan ternak"
    override val nothingDue = "Tidak wajib zakat"
    override fun livestockNisab(n: Int) = "Nisab: $n"
    override val grazingCondition = "Digembalakan di padang rumput bebas hampir sepanjang tahun"
    override val yearCondition = "Dimiliki selama setahun hijriah penuh"
    override val conditionsNotMet = "Tidak ada zakat sampai kedua syarat terpenuhi; hewan yang disiapkan untuk dijual dihitung bersama barang dagangan."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "domba atau kambing"; "bintMakhad" -> "bint makhad (unta betina 1 tahun)"; "bintLabun" -> "bint labun (unta betina 2 tahun)"; "hiqqah" -> "hiqqah (unta betina 3 tahun)"; "jadhaah" -> "jadha'ah (unta betina 4 tahun)"; "tabi" -> "tabi' (anak sapi 1 tahun)"; "musinnah" -> "musinnah (sapi 2 tahun)"; else -> kind }
    override val cropsWeight = "Berat hasil panen"
    override val irrigation = "Pengairan"
    override val irrRain = "Tanpa biaya (hujan, mata air)"
    override val irrIrrigated = "Dengan biaya (pompa, air beli)"
    override val irrMixed = "Separuh-separuh"
    override val cropsDue = "Zakat hasil panen"
    override fun cropsNisab(kg: String) = "Nisab: sekitar $kg kg"
    override val zkRate = "Kadar"
    override val rikazValue = "Nilai harta karun (rikaz)"
    override val mineralValue = "Nilai barang tambang yang diambil"
    override val mineralsDue = "Zakat barang tambang dan harta karun"
    override val obSkip = "Lewati"
    override val obNext = "Lanjut"
    override val obStart = "Mulai"
    override val obLocationTitle = "Di mana Anda?"
    override val obLocationBody = "Lokasi Anda diperlukan untuk menghitung waktu salat dan arah kiblat dengan tepat. Lokasi tetap di perangkat Anda dan tidak pernah dikirim ke server."
    override val locationNeeded = "Tentukan lokasi Anda untuk melihat waktu salat yang tepat di kota Anda"
    override val appGuide = "Panduan aplikasi"
    override val appGuideSub = "Tur singkat tentang apa saja yang bisa dilakukan Mizan"
    override val onboarding = listOf(
        "Selamat datang di Mizan" to "Alat bantu Muslim dalam satu aplikasi: salat, kiblat, Al-Qur'an, zakat, dan waris. Tanpa iklan, dan sebagian besar bisa dipakai tanpa internet.",
        "Jadwal salat dan kiblat" to "Waktu yang akurat untuk kota Anda dengan pengingat azan, arah kiblat dengan kompas, serta tanggal Hijriah dan hari besar Islam.",
        "Al-Qur'an Al-Karim" to "Halaman mushaf, tafsir dan murotal, mode fokus dan hafalan, serta khataman harian yang mencatat kemajuan dan hari beruntun Anda.",
        "Kalkulator beserta dalilnya" to "Zakat dengan lima jenisnya, waris, dan kafarat — setiap hukum disertai dalilnya dari Al-Qur'an, Sunnah, dan para ulama.",
        "Temukan apa saja" to "Gunakan kolom pencarian di Beranda untuk fitur, kota, surah, ayat, dan zikir, dalam bahasa apa pun. Tur ini bisa diputar ulang dari Setelan.",
    )
    override val quranSources = "Teks Al-Qur'an (rasm Utsmani) dari Proyek Tanzil (tanzil.net). Tafsir Al-Muyassar (Kompleks Raja Fahd), Tafsir Jalalain, dan terjemahan makna dari tanzil.net/trans. Fon Al-Qur'an: Amiri Quran, berlisensi SIL Open Font License. Murotal: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Setelan"
    override val appearance = "Tampilan"
    override val themeAuto = "Otomatis"
    override val themeLight = "Terang"
    override val themeDark = "Gelap"
    override val appColor = "Warna aplikasi"
    override val accentNames = listOf("Zamrud", "Toska", "Biru", "Ungu", "Merah muda", "Marun", "Oranye", "Madu", "Cokelat", "Grafit")
    override val language = "Bahasa"
    override val deviceLanguage = "Bahasa perangkat"
    override val use24h = "Format 24 jam"
    override val use24hDesc = "Tampilkan waktu dalam format 24 jam"
    override val exactAlarm = "Pengingat tepat waktu"
    override val exactAlarmDesc = "Izinkan alarm presisi agar pengingat azan berbunyi tepat waktu"
    override val about = "Tentang"
    override val aboutName = "Mizan — Alat bantu Muslim"
    override val adhkarNow = "Sekarang"
    override val dhikrEvidence = "Dalil"
    override val dhikrSource = "Sumber"
    override val dhikrGrade = "Derajat hadis"
    override val dhikrNarrator = "Perawi"
    override val dhikrVirtue = "Keutamaan"
    override val dhikrQuranRef = "Dari Al-Qur’an"
    override val dhikrDone = "Anda telah menyelesaikan zikir ini. Semoga Allah menerimanya"
    override val dhikrTapHint = "Ketuk zikir untuk menghitung, dan “Dalil” untuk melihat sumber dan derajatnya"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, other {# kali}}", mapOf("n" to n), "id")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, other {# zikir}}", mapOf("n" to n), "id")
    override val moreLanguages = "Unduh bahasa lain"
    override val langPackHint = "Bahasa diunduh dari situs Mizan beserta terjemahan Al-Qur’annya, lalu dapat dipakai tanpa internet."
    override val update = "Perbarui"
    override fun version(v: String) = "Versi $v"
    override val aboutText = "Aplikasi gratis berisi fitur yang dibutuhkan Muslim setiap hari: zakat, waris, jadwal salat, kiblat, tanggal Hijriah, dan tasbih. " +
        "Hukum mengikuti pendapat jumhur fukaha, jadwal salat menggunakan rumus astronomi, dan tanggal mengikuti kalender Ummul Qura."
    override val privacy = "Privasi"
    override val privacyText = "Aplikasi tidak mengumpulkan data pribadi apa pun dan hanya menggunakan internet saat Anda memintanya: untuk memutar murotal serta mengunduh tafsir, paket bahasa, dan rekaman azan yang Anda pilih. Lokasi Anda hanya digunakan di perangkat Anda untuk menghitung waktu salat dan kiblat, dan pengaturan serta angka Anda tetap di perangkat Anda."
    override val disclaimer = "Hasil bersifat panduan. Untuk persoalan agama pribadi Anda, rujuklah kepada ulama yang berkompeten dan lembaga resmi."
    // ---------- المتجر والمشاركة والأذونات ----------
    override val rateApp = "Beri rating Mizan"
    override val rateAppSub = "Ulasan Anda membantu orang lain menemukan aplikasi ini"
    override val shareApp = "Bagikan Mizan"
    override val shareAppSub = "Kirim aplikasi ini ke keluarga dan teman"
    override fun shareAppText(link: String) = "Mizan: waktu salat, kiblat, Al-Qur'an, zakat, dan waris dalam satu aplikasi gratis tanpa iklan\n$link"
    override val licenses = "Sumber dan lisensi"
    override val licensesSub = "Teks Al-Qur'an, tafsir, murattal, rekaman azan, dan font"
    override val openSettings = "Buka Pengaturan"
    override val locationOffTitle = "Akses lokasi nonaktif"
    override val locationOffBody = "Mizan tidak diizinkan menggunakan lokasi Anda. Anda dapat mengizinkannya di Pengaturan, atau memilih kota dari daftar."
    override val notifyOffTitle = "Notifikasi nonaktif"
    override val notifyOffBody = "Mizan tidak diizinkan mengirim notifikasi, sehingga pengingat azan tidak dapat sampai. Anda dapat mengizinkannya di Pengaturan."
    override val clear = "Hapus"
    override val close = "Tutup"
}
