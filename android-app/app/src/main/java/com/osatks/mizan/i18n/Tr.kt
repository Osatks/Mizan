package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Türkçede sayıdan sonra isim çoğul eki almaz: "1 gün", "5 gün" */
private fun tr(n: Long, word: String) = "$n $word"

object Tr : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Müslümanın araçları"
    override val tabHome = "Ana Sayfa"
    override val tabPrayer = "Namaz"
    override val tabQibla = "Kıble"
    override val tabTasbih = "Tespih"
    override val tabSettings = "Ayarlar"
    override val back = "Geri"
    override val share = "Paylaş"
    override val shareSignature = "— Mizan uygulaması"
    override val today = "Bugün"
    override val tomorrow = "Yarın"
    override fun inDays(n: Long) = tr(n, "gün") + " sonra"
    override fun daysCount(n: Long) = tr(n, "gün")
    override val decrease = "Azalt"
    override val increase = "Artır"
    override val noLimit = "Sınırsız"
    override val noResults = "Sonuç yok"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "ÖÖ"
    override val pm = "ÖS"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir",
        "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce"
    )
    override val gregMonths = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    override val weekdays = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
    override val weekdaysShort = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y H."
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y H."
    override val hijriYearSuffix = "H."
    override val gregYearSuffix = "M."
    override val dayLabel = "Gün"
    override val monthLabel = "Ay"
    override val yearLabel = "Yıl"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Zekât Hesaplama", "Nisaba göre para, altın ve hisseler"),
        "inheritance" to Named("Miras Hesaplama", "Ferâiz hükümlerine göre miras taksimi"),
        "prayer" to Named("Namaz Vakitleri", "Ezan bildirimi ve canlı geri sayım"),
        "qibla" to Named("Kıble Yönü", "Telefonun sensörleriyle çalışan pusula"),
        "hijri" to Named("Hicri Tarih", "Tarih çevirme, Ümmü'l-Kurâ takvimi"),
        "events" to Named("Dinî Günler", "Ramazan, iki bayram ve Arefe"),
        "tasbih" to Named("Tespih", "Hazır zikirlerle zikir sayacı"),
        "fitr" to Named("Fitre", "Sâ' ölçüsüyle veya bedel olarak"),
        "age" to Named("Yaş Hesaplama", "Hicri ve Miladi yıllarla"),
        "fidya" to Named("Fidye ve Kefaret", "Tutulamayan oruçlar ve bozulan yeminler"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zekat zekât sadaka mal servet altın altin gümüş gumus nisap nisab para birikim hisse",
        "inheritance" to "miras mirasçı mirasci varis vâris feraiz ferâiz terike tereke vasiyet",
        "prayer" to "namaz salat ezan vakit vakti imsak sabah öğle ogle ikindi akşam aksam yatsı yatsi",
        "qibla" to "kıble kible kabe kâbe mekke pusula yön yon",
        "hijri" to "takvim tarih çevir cevir hicri miladi ümmülkura ummulkura",
        "events" to "ramazan bayram arefe aşure asure kandil dini günler gunler",
        "tasbih" to "tespih tesbih zikir zikirmatik sayaç sayac tesbihat",
        "fitr" to "fitre fıtır fitir sadaka sa bayram",
        "age" to "yaş yas doğum dogum günü gunu yıl yil",
        "fidya" to "fidye kefaret keffaret yemin oruç oruc doyurma",
    )
    override val toolsTitle = "Araçlar"
    override fun nextPrayerIn(city: String) = "Sonraki vakit · $city"
    override val searchHint = "Araç, şehir, zikir ara…"

    // ---------- Search ----------
    override val searchTitle = "Ara"
    override val searchToolsGroup = "Araçlar"
    override val searchCitiesGroup = "Şehirler"
    override val searchPrayersGroup = "Bugünün namaz vakitleri"
    override val searchEventsGroup = "Dinî günler"
    override val searchDhikrGroup = "Zikirler"
    override val searchSettingsGroup = "Ayarlar"
    override val searchEmpty = "Araç, şehir, zikir, dinî gün ve ayar arayın"
    override val setAsMyCity = "Namaz vakitleri için kullan"
    override fun searchNoResults(q: String) = "“$q” için sonuç yok"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "İmsak", "sunrise" to "Güneş", "dhuhr" to "Öğle",
        "asr" to "İkindi", "maghrib" to "Akşam", "isha" to "Yatsı"
    )
    override val prayerTitle = "Namaz Vakitleri"
    override fun nextPrayerLabel(name: String) = "Sonraki vakit: $name"
    override fun atTime(time: String) = "saat $time"
    override val prevDay = "Önceki gün"
    override val nextDay = "Sonraki gün"
    override val monthTable = "Aylık çizelge"
    override val hideMonthTable = "Çizelgeyi gizle"
    override fun monthTableTitle(m: Int, y: Int) = "${gregMonths[m - 1]} $y namaz vakitleri"
    override val colDay = "Gün"
    override val locationAndMethod = "Konum ve hesaplama"
    override val useMyLocation = "Mevcut konumumu kullan"
    override val locating = "Konumunuz bulunuyor…"
    override val orPickCity = "Veya bir şehir seçin"
    override val pickCity = "Şehir seçin"
    override val searchCity = "Şehir ara"
    override val myLocation = "Konumum"
    override val calcMethod = "Hesaplama yöntemi"
    override val methods = mapOf(
        "makkah" to "Ümmü'l-Kurâ (Suudi Arabistan)",
        "mwl" to "Müslüman Dünya Birliği",
        "egypt" to "Mısır Genel Ölçme Kurumu",
        "dubai" to "BAE (Dubai)",
        "kuwait" to "Kuveyt",
        "qatar" to "Katar",
        "jordan" to "Ürdün ve Filistin",
        "morocco" to "Fas",
        "algeria" to "Cezayir",
        "tunisia" to "Tunus",
        "karachi" to "İslami İlimler Üniversitesi, Karaçi",
        "turkey" to "Diyanet (Türkiye)",
        "tehran" to "Tahran Üniversitesi Jeofizik Enstitüsü",
        "indonesia" to "Din İşleri Bakanlığı (Endonezya)",
        "malaysia" to "JAKIM (Malezya)",
        "singapore" to "MUIS (Singapur)",
        "russia" to "Rusya Müslümanları Dinî İdaresi",
        "isna" to "Kuzey Amerika (ISNA)",
        "france" to "Fransa İslami Kuruluşlar Birliği",
    )
    override val asrMethod = "İkindi hesabı"
    override val asrStandard = "Cumhur (Şafii, Maliki, Hanbeli)"
    override val asrHanafi = "Hanefi"
    override val latLng = "Enlem / boylam"
    override val adhanAlert = "Ezan bildirimi"
    override val adhanAlertDesc = "Her namaz vakti girdiğinde bildirim"
    override val timesMayDiffer = "Vakitler, ülkenizin resmî takviminden bir iki dakika farklı olabilir."
    override val locationFound = "Konum bulundu ✓"
    override val locationFailed = "Konum alınamadı. Konum hizmetlerinin açık olduğundan emin olun."
    override val locationDenied = "Konum izni verilmedi. Şehrinizi listeden seçin."
    override val cities = mapOf(
        "makkah" to "Mekke", "madinah" to "Medine", "riyadh" to "Riyad", "jeddah" to "Cidde",
        "dammam" to "Dammam", "abha" to "Ebha", "dubai" to "Dubai", "abudhabi" to "Abu Dabi", "sharjah" to "Şarika",
        "kuwait" to "Kuveyt", "doha" to "Doha", "manama" to "Manama", "muscat" to "Maskat",
        "cairo" to "Kahire", "alexandria" to "İskenderiye", "aswan" to "Asvan", "amman" to "Amman",
        "jerusalem" to "Kudüs (el-Kuds)", "gaza" to "Gazze", "beirut" to "Beyrut", "damascus" to "Şam", "aleppo" to "Halep",
        "baghdad" to "Bağdat", "basra" to "Basra", "erbil" to "Erbil", "sanaa" to "Sana", "aden" to "Aden",
        "khartoum" to "Hartum", "tripoli" to "Trablus", "benghazi" to "Bingazi", "tunis" to "Tunus",
        "algiers" to "Cezayir", "oran" to "Vahran", "rabat" to "Rabat", "casablanca" to "Kazablanka",
        "marrakesh" to "Marakeş", "nouakchott" to "Nuakşot", "mogadishu" to "Mogadişu", "djibouti" to "Cibuti",
        "istanbul" to "İstanbul", "ankara" to "Ankara", "karachi" to "Karaçi", "lahore" to "Lahor",
        "islamabad" to "İslamabad", "delhi" to "Delhi", "mumbai" to "Mumbai", "hyderabad" to "Haydarabad",
        "dhaka" to "Dakka", "chittagong" to "Çittagong", "kabul" to "Kabil", "tehran" to "Tahran", "mashhad" to "Meşhed",
        "tashkent" to "Taşkent", "baku" to "Bakü", "almaty" to "Almatı", "moscow" to "Moskova", "kazan" to "Kazan",
        "grozny" to "Grozni", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapur",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Cakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Saraybosna", "london" to "Londra", "birmingham" to "Birmingham", "paris" to "Paris",
        "marseille" to "Marsilya", "lyon" to "Lyon", "brussels" to "Brüksel", "amsterdam" to "Amsterdam",
        "berlin" to "Berlin", "hamburg" to "Hamburg", "cologne" to "Köln", "munich" to "Münih",
        "vienna" to "Viyana", "stockholm" to "Stockholm", "newyork" to "New York", "chicago" to "Şikago",
        "losangeles" to "Los Angeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sidney",
    )
    override val palestine = "Filistin"

    // ---------- Notifications ----------
    override val channelName = "Namaz vakitleri"
    override val channelDesc = "Her namaz vakti girdiğinde bildirim"
    override fun prayerNowTitle(prayer: String) = "$prayer vakti girdi"
    override fun prayerNowText(city: String) = "$city — haydi namaza"

    // ---------- Qibla ----------
    override val qiblaTitle = "Kıble Yönü"
    override val noCompass = "Cihazınızda pusula yok. Aşağıdaki açıya göre elle yönlendirin."
    override val facingQibla = "✓ Kıbleye dönüksünüz"
    override fun turnRight(deg: Int) = "Telefonu $deg° sağa çevirin"
    override fun turnLeft(deg: Int) = "Telefonu $deg° sola çevirin"
    override val qiblaAngle = "Coğrafi kuzeye göre kıble açısı"
    override fun towards(dir: String) = "Yön: $dir"
    override val directions = listOf("kuzey", "kuzeydoğu", "doğu", "güneydoğu", "güney", "güneybatı", "batı", "kuzeybatı")
    override val location = "Konum"
    override val distanceToKaaba = "Kâbe'ye uzaklık"
    override val phoneHeading = "Telefonun yönü"
    override val compassLowAccuracy = "Pusula hassasiyeti düşük: kalibre etmek için telefonu metal ve elektronik cihazlardan uzakta 8 çizerek hareket ettirin."
    override val qiblaTip = "Konumu değiştirmek için Namaz Vakitleri sayfasını kullanın. Doğru ölçüm için telefonu düz ve metallerden uzak tutun."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Dijital Tespih"
    override val tasbihSubtitle = "Saymak için daireye dokunun"
    override val modeFree = "Serbest zikir"
    override val modeAfterPrayer = "Namaz sonrası"
    override fun ofGoal(n: Int) = "/ $n"
    override val nextDhikr = "Sonraki zikre geçin"
    override val completedHundred = "Allah kabul etsin ✓ Yüzü tamamladınız"
    override fun completedN(n: Int) = "$n tamamlandı ✓"
    override val reset = "Sıfırla"
    override val undo = "Geri al"
    override val chooseDhikr = "Zikir seçin"
    override val targetCount = "Hedef sayı"
    override val yourStats = "İstatistikleriniz"
    override val roundsDone = "tamamlanan tur"
    override val todayCount = "bugün"
    override val totalCount = "toplam"
    override val dhikrLabels = listOf(
        "Sübhanallah", "Elhamdülillah", "Allahu Ekber", "Lâ ilâhe illallah", "Estağfirullah",
        "Sübhanallahi ve bihamdihi", "Sübhanallahil-azîm", "Lâ havle ve lâ kuvvete illâ billâh", "Peygamber'e salavat ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Allah'ı her türlü noksanlıktan tenzih ederim",
        "Hamd Allah'a mahsustur",
        "Allah en büyüktür",
        "Allah'tan başka ilah yoktur",
        "Allah'tan bağışlanma dilerim",
        "Allah'ı hamd ile tesbih ederim",
        "Yüce Allah'ı tesbih ederim",
        "Güç ve kuvvet ancak Allah'tandır",
        "Allah'ım, Muhammed'e salât eyle",
    )
    override val tamamMeaning = "Allah'tan başka ilah yoktur; O tektir, ortağı yoktur. Mülk O'nundur, hamd O'na mahsustur. O her şeye kadirdir."
    override val tapToCount = "Saymak için dokunun"

    // ---------- Zakat ----------
    override val zakatTitle = "Zekât Hesaplama"
    override val zakatSubtitle = "Nisaba ulaşan ve üzerinden bir kameri yıl geçen malın %2,5'i"
    override val partCash = "Nakit ve bakiyeler"
    override val partReceivables = "Tahsili umulan alacaklar"
    override val partGold = "Altın"
    override val partSilver = "Gümüş"
    override val partStocks = "Hisseler"
    override val partTrade = "Ticaret malları"
    override val enterGoldPrice = "Nisabı hesaplamak için altın fiyatını girin"
    override val enterSilverPrice = "Nisabı hesaplamak için gümüş fiyatını girin"
    override val nisabReached = "✓ Malınız nisaba ulaştı — zekât farz"
    override val belowNisab = "Nisabın altında — zekât gerekmez"
    override val enterAssets = "Varlıklarınızı girin"
    override val zakatDue = "Verilecek zekât"
    override val totalAssets = "Zekâta tabi toplam varlık"
    override val deductedDebts = "Düşülen borçlar"
    override val netBase = "Zekâta tabi net varlık"
    override val nisabValue = "Nisap değeri"
    override val approxEquals = "Yaklaşık karşılığı"
    override fun goldGrams24(g: String) = "$g g 24 ayar altın"
    override val shareResult = "Sonucu paylaş"
    override fun zakatShareText(zakat: String, base: String) = "Vermem gereken zekât: $zakat (zekâta tabi varlık $base)"
    override val settingsAndPrices = "Ayarlar ve fiyatlar"
    override val currency = "Para birimi"
    override val nisabBasis = "Para için esas alınan nisap"
    override val nisabGold = "Altın (85 g)"
    override val nisabSilver = "Gümüş (595 g)"
    override val goldPrice24 = "24 ayar altının gram fiyatı"
    override val goldPriceHint = "Kuyumcudan veya ülkenizdeki güvenilir bir fiyat sitesinden"
    override val silverPrice = "Gümüşün gram fiyatı (isteğe bağlı)"
    override val cashSection = "Nakit ve bakiyeler"
    override val cashInHand = "Eldeki nakit"
    override val bankBalances = "Banka ve cüzdan bakiyeleri"
    override val receivablesField = "Geri ödenmesini beklediğiniz alacaklar"
    override val otherMoney = "Diğer paralar"
    override val goldSilverSection = "Altın ve gümüş (gram)"
    override fun goldKarat(k: Int) = "$k ayar altın"
    override val pureSilver = "Saf gümüş"
    override val includeJewelry = "Kullanılan ziynet eşyasını dahil et (Hanefi görüşü, daha ihtiyatlı)"
    override val jewelryField = "Bunun kullanılan ziynet kısmı (21 ayar karşılığı)"
    override val jewelryHint = "Kullanılan ziynete zekât düşmediği yönündeki cumhur görüşüne göre hariç tutulur"
    override val investSection = "Yatırım ve ticaret"
    override val stocksTrade = "Alım satım için hisseler (piyasa değeri)"
    override val stocksLong = "Uzun vadeli hisseler (zekâta tabi kısım)"
    override val tradeGoods = "Ticaret malları (stok değeri)"
    override val deductSection = "Düşülecekler"
    override val debtsField = "Bu yıl vadesi gelen borçlarınız"
    override val debtsHint = "Yalnızca zekât yılı içinde ödenecek taksitler"
    override val breakdownTitle = "Mal türüne göre zekât"
    override val zakatNote = "Verileriniz yalnızca cihazınızda kalır. Allah, zekâtın verileceği sekiz sınıfı Tevbe suresinde (9:60) bildirmiştir."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Fitre"
    override val fitrSubtitle = "Bayram namazından önce verilir"
    override val foodAmountDue = "Verilecek gıda miktarı"
    override val totalCashValue = "Toplam nakit bedel"
    override fun forPeople(n: Int) = tr(n.toLong(), "kişi") + " için"
    override val peopleCount = "Fitresini verdiğiniz kişi sayısı (siz dahil)"
    override val modeFood = "Gıda (sâ')"
    override val modeMoney = "Nakit bedel"
    override val foodType = "Gıda türü"
    override val foods = mapOf(
        "rice" to "Pirinç", "riceSafe" to "Pirinç (daha ihtiyatlı)", "wheat" to "Buğday",
        "dates" to "Hurma", "barley" to "Arpa", "other" to "Başka miktar…",
    )
    override fun approxKg(kg: String) = "yaklaşık $kg kg"
    override val saWeight = "Bir sâ' ağırlığı (kg)"
    override val kgPrice = "Kilogram fiyatı (isteğe bağlı)"
    override val kgPriceHint = "Gıdanın yaklaşık maliyetini hesaplamak için"
    override val saPerPerson = "Kişi başı bir sâ'"
    override val approxCost = "Tahmini maliyet"
    override val valuePerPerson = "Kişi başı fitre bedeli"
    override val valuePerPersonHint = "Ülkenizdeki resmî kurum tarafından her yıl açıklanır"
    override val valueNote = "Bedel olarak vermek Hanefi görüşüdür; cumhur gıda olarak verir."
    override val fitrTimingNote = "Veriliş zamanı: bayram namazından önce. Bir iki gün önceden verilebilir, ancak mazeretsiz olarak namazdan sonraya bırakılmamalıdır. Fakirlere ve yoksullara verilir."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidye ve Kefaret"
    override val fidyaSubtitle = "Doyurma miktarları kilogram ve bedel olarak"
    override val tabFidya = "Fidye"
    override val tabOath = "Yemin"
    override val tabIntercourse = "Cima"
    override val fidyaResult = "Tutulamayan oruçların fidyesi"
    override fun fidyaSub(days: Int) = tr(days.toLong(), "gün") + " için — her gün için bir fakir"
    override val fidyaNote = "Yaşlılık veya iyileşme umudu olmayan hastalık gibi sebeplerle oruç tutmaktan sürekli aciz olan kişi içindir. Tüm günler için tek bir fakir veya birkaç kişi doyurulabilir."
    override val oathResult = "Yemin kefareti"
    override fun oathSub(oaths: Int) = if (oaths > 1) tr(oaths.toLong(), "kefaret") + " × 10 fakir" else "10 fakiri doyurmak"
    override val oathAlternative = "Gücü yetmezse"
    override fun fastDays(n: Int) = tr(n.toLong(), "gün") + " oruç"
    override val oathNote = "On fakiri doyurmak, giydirmek veya bir köle azat etmek arasında seçim yapılır; buna gücü yetmeyen üç gün oruç tutar (Mâide suresi 5:89)."
    override val intercourseResult = "Ramazan gündüzü cinsel ilişki kefareti"
    override val intercourseSub = "Oruç tutamazsa 60 fakiri doyurmak"
    override val ifStartToday = "Bugün oruca başlarsanız bitiş tarihi"
    override val twoMonthsDuration = "İki kameri ayın süresi"
    override val intercourseNote = "Sırasıyla: bir köle azat etmek; mümkün değilse iki ay aralıksız oruç tutmak; buna da gücü yetmezse altmış fakiri doyurmak — ayrıca o günün kazası ve tövbe gerekir."
    override val daysField = "Gün sayısı"
    override val oathsField = "Kefaret sayısı"
    override val feedMeasure = "Fakir başına gıda"
    override val muddOption = "Bir müd — yaklaşık 0,75 kg (Maliki ve Şafii)"
    override val halfSaOption = "Yarım sâ' — yaklaşık 1,5 kg (çağdaş fetvalar)"
    override val poorCount = "Fakir / öğün sayısı"
    override val approxValue = "Yaklaşık bedel"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Hicri Tarih"
    override val hijriSubtitle = "Ümmü'l-Kurâ takvimi"
    override val correspondingHijri = "Karşılık gelen Hicri tarih"
    override val correspondingGreg = "Karşılık gelen Miladi tarih"
    override val enterGregYear = "1882 ile 2174 arasında bir yıl girin"
    override val enterHijriYear = "1300 ile 1600 arasında bir Hicri yıl girin"
    override val gregToHijri = "Miladi → Hicri"
    override val hijriToGreg = "Hicri → Miladi"
    override val todayDate = "Bugünün tarihi"
    override val prevMonth = "Önceki ay"
    override val nextMonth = "Sonraki ay"
    override val calendarHint = "Büyük sayı Hicri, küçük sayı Miladi tarihtir. Çevirmek için bir güne dokunun."
    override val moonNote = "Hilalin görülmesine bağlı olarak tarih bazı ülkelerde bir gün farklı olabilir."

    // ---------- Age ----------
    override val ageTitle = "Yaş Hesaplama"
    override val ageSubtitle = "Hicri ve Miladi yıllarla"
    override val ageGregorian = "Yaşınız (Miladi)"
    override fun ymd(y: Int, m: Int, d: Int) = "${tr(y.toLong(), "yıl")} ${tr(m.toLong(), "ay")} ${tr(d.toLong(), "gün")}"
    override fun inHijri(age: String) = "Hicri: $age"
    override val birthGreg = "Doğum tarihi (Miladi)"
    override val birthHijri = "Doğum tarihi (Hicri)"
    override val birthWeekday = "Doğduğunuz gün"
    override val nextBirthday = "Sonraki doğum günü"
    override val birthdayToday = "Bugün 🎉"
    override val tileDays = "gün"
    override val tileWeeks = "hafta"
    override val tileHours = "saat"
    override val yourAge = "Yaşınız"
    override val birthAfterToday = "Doğum tarihi bugünden sonra"
    override val enterBirth = "Doğum tarihinizi girin"
    override val birthDate = "Doğum tarihi"
    override val gregorian = "Miladi"
    override val hijri = "Hicri"

    // ---------- Islamic events ----------
    override val eventsTitle = "Dinî Günler"
    override val eventsSubtitle = "Ümmü'l-Kurâ takvimine göre geri sayım"
    override val events = mapOf(
        "ramadan" to Named("Ramazan", "Oruç, gece ibadeti ve Kur'an tilaveti ayı."),
        "lastTen" to Named("Son on gece", "Aralarında bin aydan hayırlı olan Kadir Gecesi vardır."),
        "eidFitr" to Named("Ramazan Bayramı", "Fitre, bayram namazından önce verilir."),
        "shawwal" to Named("Şevval'in altı günü", "Kim Ramazan orucunu tutar, ardından Şevval'den altı gün oruç tutarsa, ömür boyu oruç tutmuş gibi olur (Müslim)."),
        "dhulHijjah" to Named("Zilhiccenin ilk on günü", "Dünyanın en faziletli günleri; salih ameller ve tekbir müstehaptır."),
        "arafah" to Named("Arefe Günü", "Hacda olmayanlar için bu günün orucu geçmiş ve gelecek yılın günahlarına kefarettir (Müslim)."),
        "eidAdha" to Named("Kurban Bayramı", "Kurban kesme günü; ardından üç teşrik günü gelir."),
        "newYear" to Named("Hicri Yılbaşı", "Yeni bir Hicri yılın başlangıcı."),
        "ashura" to Named("Aşure Günü", "Bu günün orucu geçmiş yılın günahlarına kefarettir (Müslim); dokuzuncu günün de birlikte tutulması müstehaptır."),
    )
    override val unitDays = "gün"
    override val unitHours = "saat"
    override val unitMinutes = "dk"
    override val unitSeconds = "sn"
    override val eventsNote = "Tarihler yaklaşıktır (Ümmü'l-Kurâ) ve ülkenizdeki hilal gözlemine göre bir gün farklı olabilir."

    // ---------- Inheritance ----------
    override val inhTitle = "Miras Hesaplama"
    override val inhSubtitle = "Ferâiz hükümlerine göre miras taksimi"
    override val aslLabel = "Mesele aslı (asl)"
    override val awlCase = "Avl meselesi (paylar terikeyi aşıyor)"
    override val raddCase = "Redd meselesi (artan geri verilir)"
    override val adilCase = "Âdile (denk) mesele"
    override fun tashihFrom(n: Long) = tr(n, "paya") + " tashih edildi"
    override val sharesTitle = "Mirasçıların payları"
    override fun blockedBy(by: String) = "$by tarafından hacbedildi"
    override fun perPerson(v: String) = "kişi başı $v"
    override val shareDivision = "Taksimi paylaş"
    override fun divisionShareText(list: String) = "Miras taksimi: $list"
    override val listSeparator = ", "
    override val addHeirs = "Aşağıdaki listeden mirasçıları ekleyin"
    override val deceasedAndEstate = "Vefat eden ve terike"
    override val deceasedMale = "Vefat eden erkek"
    override val deceasedFemale = "Vefat eden kadın"
    override val estateTotal = "Toplam terike"
    override val funeralCosts = "Cenaze ve defin masrafları"
    override val deceasedDebts = "Vefat edenin borçları"
    override val bequest = "Vasiyet (mirasçı olmayana)"
    override val bequestHint = "Mirasçılar onaylamadıkça borçlardan sonra kalanın üçte birini aşamaz"
    override val netEstate = "Taksim edilecek net terike"
    override val bequestCapped = "(vasiyet üçte bir ile sınırlandı)"
    override val newCase = "Yeni mesele başlat"
    override val inhDisclaimer = "Sonuç, cumhurun görüşüne dayalı bir rehberdir; şer'î mahkemenin veya ferâiz âliminin yerini tutmaz."
    override val heirs = mapOf(
        "husband" to "Koca", "wife" to "Karı", "father" to "Baba", "mother" to "Anne",
        "gf" to "Dede (babanın babası)", "gmm" to "Anneanne", "gmf" to "Babaanne",
        "son" to "Oğul", "daughter" to "Kız", "gson" to "Oğlun oğlu", "gdau" to "Oğlun kızı",
        "fb" to "Öz erkek kardeş", "fs" to "Öz kız kardeş", "pb" to "Baba bir erkek kardeş", "ps" to "Baba bir kız kardeş",
        "mb" to "Anne bir erkek kardeş", "ms" to "Anne bir kız kardeş", "fnep" to "Öz erkek kardeşin oğlu", "pnep" to "Baba bir erkek kardeşin oğlu",
        "fun" to "Öz amca", "pun" to "Baba bir amca", "fcous" to "Öz amcanın oğlu", "pcous" to "Baba bir amcanın oğlu",
    )
    override val heirGroups = mapOf(
        "spouses" to "Eşler", "ascendants" to "Anne-baba ve dede-nineler", "descendants" to "Çocuklar ve torunlar",
        "siblings" to "Kardeşler", "distant" to "Uzak erkek akrabalar",
    )
    override val blockers = mapOf(
        "gf" to "Dede",
        "twoDaughters" to "İki kız (üçte iki tamamlandı)",
        "descendant" to "Mirasçı çocuk veya torun",
        "fbAfterMuadda" to "Öz erkek kardeş (muadde sonrası)",
        "fsTookRest" to "Kalanı alan öz kız kardeş",
        "gfAndSiblings" to "Dede ve kardeşler",
        "twoFullSisters" to "İki öz kız kardeş",
        "fsGhayr" to "Öz kız kardeş (kızlarla birlikte asabe)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Farz pay"
        ShareType.FARD_TASIB -> "Farz pay + asabe"
        ShareType.FARD_TAKMILA -> "Farz pay (üçte ikiyi tamamlama)"
        ShareType.TASIB -> "Asabe"
        ShareType.TASIB_BIL_GHAYR -> "Erkek kardeşiyle asabe"
        ShareType.TASIB_MAA_GHAYR -> "Kızlarla birlikte asabe"
        ShareType.TASIB_MAA_JADD -> "Dedeyle birlikte asabe"
        ShareType.FARD_THEN_MUQASAMA -> "Farz pay, sonra mukaseme"
        ShareType.JADD_MUQASAMA -> "Kardeşlerle: mukaseme"
        ShareType.JADD_THIRD_ESTATE -> "Kardeşlerle: terikenin üçte biri"
        ShareType.JADD_THIRD_REST -> "Kardeşlerle: kalanın üçte biri"
        ShareType.JADD_SIXTH -> "Kardeşlerle: terikenin altıda biri"
    } + if (radd) " + redd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Anne bir kardeşler üçte biri, erkek-kadın ayrımı olmaksızın eşit olarak paylaşır."
        InhNote.Umariyya -> "Bu, iki Ömeriyye meselesinden biridir: anne, eşin payından sonra kalanın üçte birini alır."
        InhNote.GrandmothersShare -> "İki nine altıda biri paylaşır."
        InhNote.FullSistersGhayr -> "Öz kız kardeşler, kızlarla birlikte asabe olur."
        InhNote.Akdariyya -> "Bu, Ekderiyye meselesidir: kız kardeşe yarım, dedeye altıda bir verilir ve mesele 9'a avl eder; sonra ikisinin payı birleştirilip erkeğe iki, kadına bir oranında bölünür ve mesele 27 paya tashih edilir."
        is InhNote.JaddRule -> "Dede, kardeşlerle birlikteyken kendisi için en elverişli olanı alır: mukaseme (paylaşma) veya " +
            (if (n.otherFard) "kalanın üçte biri ya da terikenin altıda biri" else "terikenin üçte biri") +
            " — cumhurun görüşü (Maliki, Şafii, Hanbeli). Ebû Hanîfe ise dedenin, baba gibi kardeşleri hacbettiği görüşündedir."
        InhNote.Muadda -> "Baba bir kardeşler mukasemede dedeye karşı sayıldı (muadde), ardından gerektiğinde payları öz kardeşlere geri verilir."
        is InhNote.NothingLeft -> "Farz paylardan sonra bir şey kalmadığı için asabe olan mirasçı (${nameOf(n.winner)}) bir şey alamaz."
        InhNote.Mushtaraka -> "Bu, Müşterike (Himariyye) meselesidir: Malikiler ve Şafiiler öz kardeşleri üçte birde anne bir kardeşlere ortak eder; buradaki sonuç Hanefi ve Hanbeli görüşüne göredir."
        is InhNote.Awl -> "Farz paylar mesele aslını aştığı için mesele aslı ${n.asl} yerine ${n.awl} alındı (avl) ve her pay orantılı olarak azaldı."
        InhNote.Radd -> "Asabe bulunmadığından artan kısım, eşler hariç farz sahiplerine payları oranında geri verilir (redd)."
        InhNote.SpouseOnlyRadd -> "Tek mirasçı eştir: birçok çağdaş âlim ve bazı aile kanunları kalanı ona iade eder; önceki âlimler ise zevi'l-erhâma, sonra beytülmâle vermiştir."
        InhNote.NoHeirs -> "Farz sahibi veya asabe mirasçı yoktur; terike zevi'l-erhâma, sonra beytülmâle geçer. Yetkili mahkemeye başvurun."
    }

    // ---------- Quran ----------
    override val tabQuran = "Kur'an"
    override val quranTitle = "Kur'an-ı Kerim"
    override val quranSubtitle = "Tefsirini okumak için bir ayete dokunun"
    override val surahsTab = "Sureler"
    override val juzTab = "Cüzler"
    override val bookmarksTab = "Kaydedilenler"
    override val continueReading = "Okumaya devam et"
    override val startReading = "Okumaya başla"
    override val meccan = "Mekkî"
    override val medinan = "Medenî"
    override fun ayahCount(n: Int) = tr(n.toLong(), "ayet")
    override fun juzName(n: Int) = "$n. Cüz"
    override fun pageName(n: Int) = "Sayfa $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · $ayah. Ayet"
    override val surahNames = listOf(
        "Fâtiha", "Bakara", "Âl-i İmrân", "Nisâ", "Mâide", "En'âm",
        "A'râf", "Enfâl", "Tevbe", "Yûnus", "Hûd", "Yûsuf",
        "Ra'd", "İbrâhim", "Hicr", "Nahl", "İsrâ", "Kehf",
        "Meryem", "Tâhâ", "Enbiyâ", "Hac", "Mü'minûn", "Nûr",
        "Furkân", "Şuarâ", "Neml", "Kasas", "Ankebût", "Rûm",
        "Lokmân", "Secde", "Ahzâb", "Sebe'", "Fâtır", "Yâsîn",
        "Sâffât", "Sâd", "Zümer", "Mü'min", "Fussilet", "Şûrâ",
        "Zuhruf", "Duhân", "Câsiye", "Ahkâf", "Muhammed", "Fetih",
        "Hucurât", "Kâf", "Zâriyât", "Tûr", "Necm", "Kamer",
        "Rahmân", "Vâkıa", "Hadîd", "Mücâdele", "Haşr", "Mümtehine",
        "Saf", "Cuma", "Münâfikûn", "Teğâbün", "Talâk", "Tahrîm",
        "Mülk", "Kalem", "Hâkka", "Meâric", "Nûh", "Cin",
        "Müzzemmil", "Müddessir", "Kıyâme", "İnsân", "Mürselât", "Nebe'",
        "Nâziât", "Abese", "Tekvîr", "İnfitâr", "Mutaffifîn", "İnşikâk",
        "Bürûc", "Târık", "A'lâ", "Gâşiye", "Fecr", "Beled",
        "Şems", "Leyl", "Duhâ", "İnşirâh", "Tîn", "Alak",
        "Kadr", "Beyyine", "Zilzâl", "Âdiyât", "Kâria", "Tekâsür",
        "Asr", "Hümeze", "Fîl", "Kureyş", "Mâûn", "Kevser",
        "Kâfirûn", "Nasr", "Tebbet", "İhlâs", "Felak", "Nâs",
    )
    override val searchQuranHint = "Sure veya ayetlerde kelime ara…"
    override val searchQuranGroup = "Sureler"
    override val searchAyahsGroup = "Ayetler"
    override val mushafView = "Mushaf görünümü"
    override val ayahView = "Ayet ayet"
    override val textLarger = "Yazıyı büyüt"
    override val textSmaller = "Yazıyı küçült"
    override val tafsirTitle = "Tefsir"
    override val tafsirMuyassar = "Müyesser Tefsiri"
    override val tafsirJalalayn = "Celâleyn Tefsiri"
    override val translationOfMeanings = "Meal"
    override val bookmarkAyah = "Ayeti kaydet"
    override val removeBookmark = "Kaydedilenlerden çıkar"
    override val copy = "Kopyala"
    override val copied = "Kopyalandı ✓"
    override val prevAyah = "Önceki ayet"
    override val nextAyah = "Sonraki ayet"
    override val prevSurah = "Önceki sure"
    override val nextSurah = "Sonraki sure"
    override val noBookmarks = "Henüz kaydedilmiş ayet yok. Kaydetmek için bir ayeti açıp yer imi simgesine dokunun."
    override val quranLoading = "Mushaf açılıyor…"
    override fun hizbName(n: Int) = "$n. Hizb"
    override val khatmaTitle = "Hatmim"
    override val khatmaPlanLabel = "Süre"
    override fun planDays(days: Int) = if (days == 0) "Süresiz" else "$days gün"
    override fun pagesRead(read: Int, total: Int) = "$read / $total sayfa"
    override val todayPortion = "Bugünkü vird"
    override fun streakDays(n: Int) = "Seri: $n gün"
    override fun daysLeftLabel(n: Int) = "Kalan gün: $n"
    override val portionDone = "Bugünkü vird tamam ✓"
    override val khatmaDone = "Hatim tamamlandı ✓ Allah kabul etsin"
    override val newKhatma = "Yeni hatim başlat"
    override fun khatmasCompleted(n: Int) = "Tamamlanan hatimler: $n"
    override val continuePortion = "Virdime devam et"
    override val ayahOfDay = "Günün ayeti"
    override val focusMode = "Odak modu"
    override val exitFocus = "Odak modundan çık"
    override val silenceTitle = "Okurken bildirimler susturulsun mu?"
    override val silenceBody = "Odak modunda Mizan «Rahatsız Etmeyin»i açabilir ve okumayı bitirdiğinizde yeniden kapatır. Android bunun için bir kez izin isteyecek."
    override val allow = "İzin ver"
    override val notNow = "Şimdi değil"
    override val tafsirLibrary = "Tefsir kütüphanesi"
    override val tafsirLibrarySub = "Tefsir indirin ve bir ayete dokunduğunuzda hangilerinin görüneceğini seçin"
    override val onDevice = "Bu cihazda"
    override val availableDownloads = "İndirilebilir"
    override val download = "İndir"
    override fun downloadingPct(pct: Int) = "İndiriliyor… %$pct"
    override val cancel = "İptal"
    override val delete = "Sil"
    override fun deleteTafsirQ(name: String) = "“$name” bu cihazdan silinsin mi?"
    override val showInTafsir = "Tefsirde göster"
    override val builtIn = "Yerleşik"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "İndirme başarısız. İnternet bağlantınızı kontrol edin."
    override val retry = "Tekrar dene"
    override val moreTafsirs = "Diğer tefsirler"
    override val tafsirLibraryNote = "Tefsirler açık tafsir_api koleksiyonundan (quran.com ve QUL kaynaklı) indirilir. İnternet yalnızca bu indirmeler için kullanılır; sonrasında çevrimdışı çalışırlar."
    override fun ayahRange(from: Int, to: Int) = "$from–$to. ayetlerin tefsiri"
    override val recitation = "Tilavet"
    override val reciter = "Kâri"
    override val playRecitation = "Tilaveti dinle"
    override val pauseRecitation = "Duraklat"
    override val stopRecitation = "Tilaveti durdur"
    override val repeatAyah = "Her ayeti tekrarla"
    override val listenFromHere = "Bu ayetten itibaren dinle"
    override val audioNote = "Tilavetler EveryAyah.com üzerinden çalınır. Dinlediğiniz ayetler cihazınıza kaydedilir ve internetsiz de çalışır."
    override val audioError = "Tilavet çalınamadı. İnternet bağlantınızı kontrol edin."
    override val hifzMode = "Hıfz"
    override val revealNext = "Sonrakini göster"
    override val hideAll = "Tümünü gizle"
    override val hifzHint = "Ezberden okuyun, sonra kontrol etmek için gizli ayete dokunun."
    override val displayTitle = "Görünüm"
    override val pageColor = "Sayfa rengi"
    override val toneAuto = "Otomatik"
    override val toneSepia = "Sepya"
    override val toneNight = "Gece"
    override val brightness = "Parlaklık"
    override val brightnessSystem = "Cihaz parlaklığı"
    override val textSizeLabel = "Yazı boyutu"
    override val continueFocus = "Odak modunda devam et"
    override val smartPresets = "Akıllı ayarlar"
    override val presetListen = "Dinle"
    override val presetMemorize = "Ezberle"
    override val presetReview = "Tekrar et"
    override val smartHint = "Ezberle: her ayet 3 kez, kâriden sonra tekrarlamanız için bir duraklamayla. Tekrar et: surenin tamamı, biraz daha hızlı."
    override val range = "Aralık"
    override val rangeContinue = "Kesintisiz"
    override val rangePage = "Bu sayfa"
    override val rangeSurah = "Bu sure"
    override val rangeCustom = "Özel"
    override val fromAyah = "Başlangıç"
    override val toAyah = "Bitiş"
    override val repeatRange = "Aralığı tekrarla"
    override val pauseAfter = "Her ayetten sonra duraklat"
    override val off = "Kapalı"
    override val pauseAyahLength = "Ayet uzunluğu"
    override fun seconds(n: Int) = "$n sn"
    override val speed = "Hız"
    override val sleepTimer = "Uyku zamanlayıcısı"
    override fun minutes(n: Int) = "$n dk"
    override val quranSources = "Kur'an metni (resm-i Osmânî) Tanzil Projesi'nden (tanzil.net); Müyesser Tefsiri (Kral Fahd Kompleksi), Celâleyn Tefsiri ve mealler ise tanzil.net/trans adresinden alınmıştır. Mushaf yazı tipi: Amiri Quran, SIL Open Font License. Tilavetler: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Ayarlar"
    override val appearance = "Görünüm"
    override val themeAuto = "Otomatik"
    override val themeLight = "Açık"
    override val themeDark = "Koyu"
    override val language = "Dil"
    override val deviceLanguage = "Cihaz dili"
    override val use24h = "24 saat biçimi"
    override val use24hDesc = "Saatleri 24 saat biçiminde göster"
    override val exactAlarm = "Tam zamanlı bildirimler"
    override val exactAlarmDesc = "Ezan bildiriminin zamanında gelmesi için tam alarmlara izin verin"
    override val about = "Hakkında"
    override val aboutName = "Mizan — Müslümanın araçları"
    override fun version(v: String) = "Sürüm $v"
    override val aboutText = "Bir Müslümanın her gün ihtiyaç duyduğu araçları sunan ücretsiz bir uygulama: zekât, miras, namaz vakitleri, kıble, Hicri tarih ve tespih. " +
        "Hükümler fakihlerin çoğunluğunun görüşüne dayanır; namaz vakitleri astronomik formüllerle hesaplanır, tarihler Ümmü'l-Kurâ takvimini esas alır."
    override val privacy = "Gizlilik"
    override val privacyText = "Uygulama hiçbir kişisel veri toplamaz ve interneti yalnızca seçtiğiniz tefsirleri indirmek ve istediğinizde tilavet çalmak için kullanır. Konumunuz yalnızca cihazınızda namaz vakitlerini ve kıbleyi hesaplamak için kullanılır; " +
        "ayarlarınız ve girdiğiniz rakamlar cihazınızda kalır."
    override val disclaimer = "Sonuçlar yol göstericidir. Kendi dinî sorularınız için yetkin âlimlere ve resmî kurumlara başvurun."
}
