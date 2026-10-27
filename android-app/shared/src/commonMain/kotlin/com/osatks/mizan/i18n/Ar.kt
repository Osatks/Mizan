package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType
import com.osatks.mizan.quran.QuranMeta

/** صيغ العدد في العربية: واحد، اثنان، 3-10، 11-99، وما سواها (100، 101…) */
private fun arCount(n: Long, one: String, two: String, few: String, many: String, other: String): String = when {
    n == 1L -> one
    n == 2L -> two
    n % 100 in 3..10 -> "$n $few"
    n % 100 in 11..99 -> "$n $many"
    else -> "$n $other"
}

object Ar : Strings {
    // ---------- عام ----------
    override val appName = "ميزان"
    override val appTagline = "أدوات المسلم"
    override val tabHome = "الرئيسية"
    override val tabPrayer = "الصلاة"
    override val tabQibla = "القبلة"
    override val tabTasbih = "المسبحة"
    override val tabSettings = "الإعدادات"
    override val back = "رجوع"
    override val share = "مشاركة"
    override val shareSignature = "— تطبيق ميزان"
    override val today = "اليوم"
    override val tomorrow = "غداً"
    override fun inDays(n: Long) = "بعد " + arCount(n, "يوم", "يومين", "أيام", "يوماً", "يوم")
    override fun daysCount(n: Long) = arCount(n, "يوم واحد", "يومان", "أيام", "يوماً", "يوم")
    override val decrease = "إنقاص"
    override val increase = "زيادة"
    override val noLimit = "بلا حد"
    override val noResults = "لا توجد نتائج"
    override val unitGram = "غ"
    override val unitKg = "كغ"
    override val unitKm = "كم"
    override val am = "ص"
    override val pm = "م"

    // ---------- التقويم ----------
    override val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    override val gregMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    override val weekdays = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    override val weekdaysShort = listOf("اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y هـ"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y م"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y هـ"
    override val hijriYearSuffix = "هـ"
    override val gregYearSuffix = "م"
    override val dayLabel = "اليوم"
    override val monthLabel = "الشهر"
    override val yearLabel = "السنة"

    // ---------- الرئيسية ----------
    override val tools = mapOf(
        "zakat" to Named("حاسبة الزكاة", "المال والذهب والأسهم مع النصاب"),
        "inheritance" to Named("حاسبة المواريث", "تقسيم التركة وفق علم الفرائض"),
        "prayer" to Named("مواقيت الصلاة", "مع تنبيه الأذان والعد التنازلي"),
        "qibla" to Named("اتجاه القبلة", "بوصلة تعمل بحساسات الجهاز"),
        "hijri" to Named("التاريخ الهجري", "تحويل وتقويم أم القرى"),
        "events" to Named("المناسبات", "رمضان والعيدان وعرفة"),
        "tasbih" to Named("المسبحة", "عدّاد ذكر مع أذكار جاهزة"),
        "adhkar" to Named("الأذكار", "حصن المسلم وأذكار اليوم والليلة بأدلتها"),
        "fitr" to Named("زكاة الفطر", "بالصاع أو بالقيمة"),
        "age" to Named("حاسبة العمر", "بالهجري والميلادي"),
        "fidya" to Named("الفدية والكفارة", "الصيام واليمين"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "زكاة مال ذهب فضة نصاب أسهم ثروة صدقة",
        "inheritance" to "ميراث مواريث تركة فرائض ورثة وصية",
        "prayer" to "صلاة أذان فجر ظهر عصر مغرب عشاء وقت",
        "qibla" to "قبلة كعبة بوصلة اتجاه مكة",
        "hijri" to "تاريخ هجري ميلادي تقويم تحويل أم القرى",
        "events" to "رمضان عيد عرفة عاشوراء مناسبة",
        "tasbih" to "تسبيح ذكر أذكار سبحة عداد",
        "adhkar" to "أذكار اذكار حصن المسلم الصباح المساء النوم دعاء أدعية رمضان",
        "fitr" to "فطرة فطر صاع عيد",
        "age" to "عمر ميلاد سن",
        "fidya" to "فدية كفارة يمين صيام إطعام",
    )
    override val toolsTitle = "الأدوات"
    override fun nextPrayerIn(city: String) = "الصلاة القادمة · $city"
    override val searchHint = "ابحث عن أداة أو مدينة أو ذكر…"

    // ---------- البحث ----------
    override val searchTitle = "البحث"
    override val searchToolsGroup = "الأدوات"
    override val searchCitiesGroup = "المدن"
    override val searchPrayersGroup = "مواقيت اليوم"
    override val searchEventsGroup = "المناسبات"
    override val searchDhikrGroup = "الأذكار"
    override val searchSettingsGroup = "الإعدادات"
    override val searchEmpty = "ابحث في الأدوات والمدن والأذكار والمناسبات والإعدادات"
    override val setAsMyCity = "اعتمدها لمواقيت الصلاة"
    override fun searchNoResults(q: String) = "لا نتائج لـ«$q»"

    // ---------- الصلاة ----------
    override val prayers = mapOf(
        "fajr" to "الفجر", "sunrise" to "الشروق", "dhuhr" to "الظهر",
        "asr" to "العصر", "maghrib" to "المغرب", "isha" to "العشاء"
    )
    override val prayerTitle = "مواقيت الصلاة"
    override fun nextPrayerLabel(name: String) = "الصلاة القادمة: $name"
    override fun atTime(time: String) = "عند $time"
    override val prevDay = "اليوم السابق"
    override val nextDay = "اليوم التالي"
    override val monthTable = "جدول الشهر"
    override val hideMonthTable = "إخفاء جدول الشهر"
    override fun monthTableTitle(m: Int, y: Int) = "جدول ${gregMonths[m - 1]} $y"
    override val colDay = "اليوم"
    override val locationAndMethod = "الموقع وطريقة الحساب"
    override val useMyLocation = "استخدم موقعي الحالي"
    override val locating = "جارٍ تحديد الموقع…"
    override val orPickCity = "أو اختر مدينة"
    override val pickCity = "اختر مدينة"
    override val searchCity = "ابحث عن مدينة"
    override val myLocation = "موقعي الحالي"
    override val calcMethod = "طريقة الحساب"
    override val methods = mapOf(
        "makkah" to "أم القرى (السعودية)",
        "mwl" to "رابطة العالم الإسلامي",
        "egypt" to "الهيئة المصرية العامة للمساحة",
        "dubai" to "الإمارات (دبي)",
        "kuwait" to "الكويت",
        "qatar" to "قطر",
        "jordan" to "الأردن وفلسطين",
        "morocco" to "المغرب",
        "algeria" to "الجزائر",
        "tunisia" to "تونس",
        "karachi" to "جامعة العلوم الإسلامية بكراتشي",
        "turkey" to "رئاسة الشؤون الدينية التركية",
        "tehran" to "معهد الجيوفيزياء بجامعة طهران",
        "indonesia" to "وزارة الشؤون الدينية الإندونيسية",
        "malaysia" to "دائرة التنمية الإسلامية الماليزية (جاكيم)",
        "singapore" to "مجلس الشؤون الإسلامية في سنغافورة",
        "russia" to "الإدارة الدينية لمسلمي روسيا",
        "isna" to "أمريكا الشمالية (ISNA)",
        "france" to "اتحاد المنظمات الإسلامية بفرنسا",
    )
    override val asrMethod = "مذهب حساب العصر"
    override val asrStandard = "الجمهور"
    override val asrHanafi = "الحنفي"
    override val latLng = "خط العرض / الطول"
    override val adhanAlert = "تنبيه الأذان"
    override val adhanAlertDesc = "إشعار عند دخول وقت كل صلاة"
    override val timesMayDiffer = "قد تختلف المواقيت دقيقة أو دقيقتين عن التقويم الرسمي في بلدك."
    override fun endsAt(t: String) = "ينتهي $t"
    override fun iqamaAt(t: String) = "الإقامة $t"
    override val iqamaTitle = "مواعيد الإقامة"
    override val iqamaDesc = "الدقائق بين الأذان والإقامة في مسجدك، و0 لإخفائها."
    override val prayerEndsNote = "ينتهي وقت الفجر بطلوع الشمس، والظهر بدخول العصر، والعصر بغروب الشمس (ويُكره تأخيره حتى تصفرّ الشمس)، والمغرب بمغيب الشفق الأحمر، ووقت الاختيار للعشاء بمنتصف الليل ويمتد وقت الضرورة إلى الفجر."
    override val locationFound = "تم تحديد موقعك ✓"
    override val locationFailed = "تعذر تحديد الموقع، تأكد من تشغيل خدمة الموقع"
    override val locationDenied = "لم يُمنح إذن الموقع، اختر مدينتك من القائمة"
    override val cities = mapOf(
        "makkah" to "مكة المكرمة", "madinah" to "المدينة المنورة", "riyadh" to "الرياض", "jeddah" to "جدة",
        "dammam" to "الدمام", "abha" to "أبها", "dubai" to "دبي", "abudhabi" to "أبوظبي", "sharjah" to "الشارقة",
        "kuwait" to "الكويت", "doha" to "الدوحة", "manama" to "المنامة", "muscat" to "مسقط",
        "cairo" to "القاهرة", "alexandria" to "الإسكندرية", "aswan" to "أسوان", "amman" to "عمّان",
        "jerusalem" to "القدس", "gaza" to "غزة", "beirut" to "بيروت", "damascus" to "دمشق", "aleppo" to "حلب",
        "baghdad" to "بغداد", "basra" to "البصرة", "erbil" to "أربيل", "sanaa" to "صنعاء", "aden" to "عدن",
        "khartoum" to "الخرطوم", "tripoli" to "طرابلس", "benghazi" to "بنغازي", "tunis" to "تونس",
        "algiers" to "الجزائر", "oran" to "وهران", "rabat" to "الرباط", "casablanca" to "الدار البيضاء",
        "marrakesh" to "مراكش", "nouakchott" to "نواكشوط", "mogadishu" to "مقديشو", "djibouti" to "جيبوتي",
        "istanbul" to "إسطنبول", "ankara" to "أنقرة", "karachi" to "كراتشي", "lahore" to "لاهور",
        "islamabad" to "إسلام آباد", "delhi" to "دلهي", "mumbai" to "مومباي", "hyderabad" to "حيدر آباد",
        "dhaka" to "دكا", "chittagong" to "شيتاغونغ", "kabul" to "كابل", "tehran" to "طهران", "mashhad" to "مشهد",
        "tashkent" to "طشقند", "baku" to "باكو", "almaty" to "ألماتي", "moscow" to "موسكو", "kazan" to "قازان",
        "grozny" to "غروزني", "kualalumpur" to "كوالالمبور", "penang" to "بينانغ", "singapore" to "سنغافورة",
        "brunei" to "بندر سري بكاوان", "jakarta" to "جاكرتا", "surabaya" to "سورابايا", "bandung" to "باندونغ",
        "medan" to "ميدان", "makassar" to "ماكاسار", "lagos" to "لاغوس", "kano" to "كانو", "dakar" to "داكار",
        "sarajevo" to "سراييفو", "london" to "لندن", "birmingham" to "برمنغهام", "paris" to "باريس",
        "marseille" to "مرسيليا", "lyon" to "ليون", "brussels" to "بروكسل", "amsterdam" to "أمستردام",
        "berlin" to "برلين", "hamburg" to "هامبورغ", "cologne" to "كولونيا", "munich" to "ميونخ",
        "vienna" to "فيينا", "stockholm" to "ستوكهولم", "newyork" to "نيويورك", "chicago" to "شيكاغو",
        "losangeles" to "لوس أنجلوس", "toronto" to "تورونتو", "montreal" to "مونتريال", "sydney" to "سيدني",
    )
    override val palestine = "فلسطين"

    // ---------- الإشعارات ----------
    override val channelName = "مواقيت الصلاة"
    override val channelDesc = "تنبيه عند دخول وقت كل صلاة"
    override fun prayerNowTitle(prayer: String) = "حان الآن وقت صلاة $prayer"
    override fun prayerNowText(city: String) = "$city — حيّ على الصلاة"
    // ---------- Adhan & alerts ----------
    override val adhanSettings = "الأذان والتنبيهات"
    override val adhanSettingsSub = "المؤذن، وطريقة التنبيه لكل صلاة، والاهتزاز"
    override val alertPerPrayer = "التنبيه لكل صلاة"
    override val alertModeAdhan = "أذان"
    override val alertModeVibrate = "اهتزاز فقط"
    override val alertModeSilent = "إشعار صامت"
    override val alertModeOff = "بلا تنبيه"
    override val alertModesHint = "«أذان» يشغّل الأذان كاملاً، و«اهتزاز فقط» يهتز بلا صوت، و«إشعار صامت» يُظهر إشعاراً بلا صوت، و«بلا تنبيه» لا يرسل شيئاً لتلك الصلاة."
    override val muezzinTitle = "المؤذن"
    override val fajrAdhanTitle = "أذان الفجر"
    override val fajrAdhanHint = "يُزاد في أذان الفجر «الصلاة خير من النوم»."
    override val fajrSameAsOthers = "مثل بقية الصلوات"
    override val muezzinNames = mapOf(
        "asim" to "عاصم جاويد",
        "klcc" to "مسجد KLCC، كوالالمبور",
        "mirza" to "معظّم بيك ميرزا، أحمد آباد",
        "adam" to "آدم (ويكيميديا كومنز)",
        "kalkan_fajr" to "الفجر — كالكان، تركيا",
    )
    override val playAdhan = "استمع إلى الأذان"
    override val stopAdhan = "إيقاف"
    override val vibrationTitle = "نمط الاهتزاز"
    override val vibeNames = mapOf("short" to "قصير", "long" to "طويل", "heartbeat" to "نبض القلب", "pulse" to "ثلاث نبضات", "wave" to "موجة")
    override val tryVibration = "جرّب الاهتزاز"
    override val adhanStopHint = "لإيقاف الأذان اضغط «إيقاف» في الإشعار أو أحد زرَّي الصوت. وإن كان الهاتف على الصامت أو الاهتزاز فالتنبيه يتبع ذلك."
    override val adhanIosNote = "يحدّ iPhone صوت الإشعار بثلاثين ثانية، فيُسمع في الإشعار أول الأذان، ويُسمع كاملاً إن كان ميزان مفتوحاً عند دخول الوقت. ويهتز الإشعار بنمط iPhone نفسه (الإعدادات › الأصوات والحس اللمسي)، أما الأنماط هنا فتُحَسّ والتطبيق مفتوح."
    override val recordingSources = "مصادر التسجيلات"

    // ---------- القبلة ----------
    override val qiblaTitle = "اتجاه القبلة"
    override val noCompass = "لا يحتوي جهازك على بوصلة. وجّه الجهاز يدوياً حسب الزاوية أدناه."
    override val facingQibla = "✓ أنت متجه نحو القبلة"
    override fun turnRight(deg: Int) = "أدِر هاتفك يميناً بمقدار $deg°"
    override fun turnLeft(deg: Int) = "أدِر هاتفك يساراً بمقدار $deg°"
    override val qiblaAngle = "زاوية القبلة من الشمال الجغرافي"
    override fun towards(dir: String) = "باتجاه $dir"
    override val directions = listOf("الشمال", "الشمال الشرقي", "الشرق", "الجنوب الشرقي", "الجنوب", "الجنوب الغربي", "الغرب", "الشمال الغربي")
    override val location = "الموقع"
    override val distanceToKaaba = "المسافة إلى الكعبة"
    override val phoneHeading = "اتجاه الهاتف"
    override val compassLowAccuracy = "دقة البوصلة منخفضة: حرّك الهاتف على شكل الرقم 8 لمعايرتها، وأبعده عن المعادن والأجهزة."
    override val qiblaTip = "لتغيير الموقع استخدم صفحة مواقيت الصلاة. أمسك الهاتف أفقياً وبعيداً عن المعادن للحصول على قراءة دقيقة."

    // ---------- المسبحة ----------
    override val tasbihTitle = "المسبحة الإلكترونية"
    override val tasbihSubtitle = "اضغط على الدائرة للتسبيح"
    override val modeFree = "ذكر حر"
    override val modeAfterPrayer = "بعد الصلاة"
    override fun ofGoal(n: Int) = "من $n"
    override val nextDhikr = "انتقل إلى الذكر التالي"
    override val completedHundred = "تقبّل الله منك ✓ أتممت المائة"
    override fun completedN(n: Int) = "أتممت $n ✓"
    override val reset = "تصفير"
    override val undo = "تراجع"
    override val chooseDhikr = "اختر الذكر"
    override val targetCount = "العدد المستهدف"
    override val yourStats = "إحصاءاتك"
    override val roundsDone = "دورة مكتملة"
    override val todayCount = "ذكر اليوم"
    override val totalCount = "الإجمالي"
    override val dhikrLabels = listOf(
        "سبحان الله", "الحمد لله", "الله أكبر", "لا إله إلا الله", "أستغفر الله",
        "سبحان الله وبحمده", "سبحان الله العظيم", "لا حول ولا قوة إلا بالله", "الصلاة على النبي ﷺ",
    )
    override val dhikrMeanings = List(9) { "" }
    override val tamamMeaning = ""
    override val tapToCount = "اضغط للتسبيح"

    // ---------- الزكاة ----------
    override val zakatTitle = "حاسبة الزكاة"
    override val zakatSubtitle = "2.5% من المال إذا بلغ النصاب وحال عليه الحول"
    override val partCash = "النقود والأرصدة"
    override val partReceivables = "الديون المرجوّة"
    override val partGold = "الذهب"
    override val partSilver = "الفضة"
    override val partStocks = "الأسهم"
    override val partTrade = "عروض التجارة"
    override val enterGoldPrice = "أدخل سعر الذهب لحساب النصاب"
    override val enterSilverPrice = "أدخل سعر الفضة لحساب النصاب"
    override val nisabReached = "✓ بلغ مالك النصاب — تجب الزكاة"
    override val belowNisab = "لم يبلغ النصاب — لا زكاة عليك"
    override val enterAssets = "أدخل أموالك"
    override val zakatDue = "الزكاة المستحقة"
    override val totalAssets = "إجمالي الأموال الزكوية"
    override val deductedDebts = "الديون المطروحة"
    override val netBase = "صافي الوعاء الزكوي"
    override val nisabValue = "قيمة النصاب"
    override val approxEquals = "تعادل تقريباً"
    override fun goldGrams24(g: String) = "$g غرام ذهب عيار 24"
    override val shareResult = "مشاركة النتيجة"
    override fun zakatShareText(zakat: String, base: String) = "زكاتي المستحقة: $zakat (الوعاء الزكوي $base)"
    override val settingsAndPrices = "الإعدادات والأسعار"
    override val currency = "العملة"
    override val nisabBasis = "النصاب المعتمد للنقود"
    override val nisabGold = "الذهب (85 غ)"
    override val nisabSilver = "الفضة (595 غ)"
    override val goldPrice24 = "سعر غرام الذهب عيار 24"
    override val goldPriceHint = "من محل ذهب أو موقع أسعار موثوق في بلدك"
    override val silverPrice = "سعر غرام الفضة (اختياري)"
    override val cashSection = "النقود والأرصدة"
    override val cashInHand = "النقد في اليد"
    override val bankBalances = "أرصدة البنوك والمحافظ"
    override val receivablesField = "ديون لك مرجوّة السداد"
    override val otherMoney = "أموال أخرى"
    override val goldSilverSection = "الذهب والفضة (بالغرام)"
    override fun goldKarat(k: Int) = "ذهب عيار $k"
    override val pureSilver = "فضة خالصة"
    override val includeJewelry = "إدخال حُليّ الزينة المستعملة في الحساب (قول الحنفية، والأحوط)"
    override val jewelryField = "منها حُليّ زينة مستعملة (مكافئ عيار 21)"
    override val jewelryHint = "يُستبعد على قول الجمهور بعدم زكاة الحلي المعدّ للاستعمال"
    override val investSection = "الاستثمار والتجارة"
    override val stocksTrade = "أسهم للمتاجرة (القيمة السوقية)"
    override val stocksLong = "أسهم للاستثمار طويل الأجل (الوعاء الزكوي)"
    override val tradeGoods = "عروض التجارة (قيمة البضاعة)"
    override val deductSection = "ما يُطرح"
    override val debtsField = "ديون عليك حالّة هذا العام"
    override val debtsHint = "الأقساط المستحقة خلال الحول فقط"
    override val breakdownTitle = "توزيع الزكاة حسب نوع المال"
    override val zakatNote = "تُحفظ بياناتك على جهازك فقط. مصارف الزكاة ثمانية ذكرها الله في سورة التوبة (الآية 60)."

    // ---------- زكاة الفطر ----------
    override val fitrTitle = "زكاة الفطر"
    override val fitrSubtitle = "تُخرج قبل صلاة العيد"
    override val foodAmountDue = "مقدار الطعام الواجب إخراجه"
    override val totalCashValue = "القيمة النقدية الإجمالية"
    override fun forPeople(n: Int) = when (n) {
        1 -> "عن فرد واحد"
        2 -> "عن فردين"
        in 3..10 -> "عن $n أفراد"
        else -> if (n % 100 in 11..99) "عن $n فرداً" else "عن $n فرد"
    }
    override val peopleCount = "عدد من تُخرج عنهم (مع نفسك)"
    override val modeFood = "طعام (الصاع)"
    override val modeMoney = "قيمة نقدية"
    override val foodType = "نوع الطعام"
    override val foods = mapOf(
        "rice" to "أرز", "riceSafe" to "أرز (الأحوط)", "wheat" to "قمح",
        "dates" to "تمر", "barley" to "شعير", "other" to "مقدار آخر…",
    )
    override fun approxKg(kg: String) = "$kg كغ تقريباً"
    override val saWeight = "وزن الصاع بالكيلوغرام"
    override val kgPrice = "سعر الكيلوغرام (اختياري)"
    override val kgPriceHint = "لتعرف التكلفة التقريبية للشراء"
    override val saPerPerson = "مقدار الصاع للفرد"
    override val approxCost = "التكلفة التقريبية"
    override val valuePerPerson = "قيمة زكاة الفطر عن الفرد"
    override val valuePerPersonHint = "تعلنها الجهة الرسمية في بلدك كل عام"
    override val valueNote = "إخراج القيمة قول الحنفية، والجمهور على إخراج الطعام."
    override val fitrTimingNote = "وقت الإخراج: قبل صلاة العيد، ويجوز تقديمها بيوم أو يومين، ولا يجوز تأخيرها عن الصلاة بغير عذر. وتُعطى للفقراء والمساكين."

    // ---------- الفدية والكفارة ----------
    override val fidyaTitle = "الفدية والكفارة"
    override val fidyaSubtitle = "مقدار الإطعام بالكيلوغرام والقيمة"
    override val tabFidya = "فدية الصيام"
    override val tabOath = "اليمين"
    override val tabIntercourse = "الجماع"
    override val fidyaResult = "فدية الصيام"
    override fun fidyaSub(days: Int) = "عن " + arCount(days.toLong(), "يوم واحد", "يومين", "أيام", "يوماً", "يوم") + " — مسكين عن كل يوم"
    override val fidyaNote = "لمن عجز عن الصيام عجزاً دائماً كالكبير والمريض الذي لا يُرجى برؤه. يجوز إطعام مسكين واحد عن كل الأيام أو مساكين متعددين."
    override val oathResult = "كفارة اليمين"
    override fun oathSub(oaths: Int) = if (oaths > 1) arCount(oaths.toLong(), "كفارة", "كفارتان", "كفارات", "كفارة", "كفارة") + " × 10 مساكين" else "إطعام 10 مساكين"
    override val oathAlternative = "البديل عند العجز"
    override fun fastDays(n: Int) = "صيام " + arCount(n.toLong(), "يوم واحد", "يومين", "أيام", "يوماً", "يوم")
    override val oathNote = "يُخيّر بين إطعام عشرة مساكين أو كسوتهم أو تحرير رقبة، فإن لم يجد فصيام ثلاثة أيام (سورة المائدة: 89)."
    override val intercourseResult = "كفارة الجماع في نهار رمضان"
    override val intercourseSub = "إطعام 60 مسكيناً عند العجز عن الصيام"
    override val ifStartToday = "لو بدأت الصيام اليوم ينتهي في"
    override val twoMonthsDuration = "مدة الشهرين الهجريين"
    override val intercourseNote = "الواجب على الترتيب: عتق رقبة، فإن لم يجد فصيام شهرين متتابعين، فإن لم يستطع فإطعام ستين مسكيناً، مع قضاء اليوم والتوبة."
    override val daysField = "عدد الأيام"
    override val oathsField = "عدد الكفارات"
    override val feedMeasure = "مقدار الإطعام لكل مسكين"
    override val muddOption = "مُدّ — نحو 0.75 كغ (المالكية والشافعية)"
    override val halfSaOption = "نصف صاع — نحو 1.5 كغ (فتاوى معاصرة)"
    override val poorCount = "عدد المساكين / الوجبات"
    override val approxValue = "القيمة التقريبية"

    // ---------- التاريخ الهجري ----------
    override val hijriTitle = "التاريخ الهجري"
    override val hijriSubtitle = "وفق تقويم أم القرى"
    override val correspondingHijri = "التاريخ الهجري الموافق"
    override val correspondingGreg = "التاريخ الميلادي الموافق"
    override val enterGregYear = "أدخل سنة بين 1882 و2174"
    override val enterHijriYear = "أدخل سنة هجرية بين 1300 و1600"
    override val gregToHijri = "ميلادي ← هجري"
    override val hijriToGreg = "هجري ← ميلادي"
    override val todayDate = "تاريخ اليوم"
    override val prevMonth = "الشهر السابق"
    override val nextMonth = "الشهر التالي"
    override val calendarHint = "الرقم الكبير هجري والصغير ميلادي. اضغط أي يوم لتحويله."
    override val moonNote = "قد يختلف التاريخ يوماً واحداً عن بعض الدول بسبب اختلاف رؤية الهلال."

    // ---------- العمر ----------
    override val ageTitle = "حاسبة العمر"
    override val ageSubtitle = "بالتقويمين الهجري والميلادي"
    override val ageGregorian = "عمرك بالميلادي"
    override fun ymd(y: Int, m: Int, d: Int) = "$y سنة $m شهر $d يوم"
    override fun inHijri(age: String) = "بالهجري: $age"
    override val birthGreg = "تاريخ الميلاد ميلادياً"
    override val birthHijri = "تاريخ الميلاد هجرياً"
    override val birthWeekday = "يوم الميلاد"
    override val nextBirthday = "عيد ميلادك القادم"
    override val birthdayToday = "اليوم 🎉"
    override val tileDays = "يوماً"
    override val tileWeeks = "أسبوعاً"
    override val tileHours = "ساعة"
    override val yourAge = "عمرك"
    override val birthAfterToday = "تاريخ الميلاد بعد اليوم"
    override val enterBirth = "أدخل تاريخ ميلادك"
    override val birthDate = "تاريخ الميلاد"
    override val gregorian = "ميلادي"
    override val hijri = "هجري"

    // ---------- المناسبات ----------
    override val eventsTitle = "المناسبات الإسلامية"
    override val eventsSubtitle = "عد تنازلي وفق تقويم أم القرى"
    override val events = mapOf(
        "ramadan" to Named("شهر رمضان", "شهر الصيام والقيام وتلاوة القرآن."),
        "lastTen" to Named("العشر الأواخر", "فيها ليلة القدر خير من ألف شهر."),
        "eidFitr" to Named("عيد الفطر", "تُخرج زكاة الفطر قبل صلاته."),
        "shawwal" to Named("ست من شوال", "من صام رمضان ثم أتبعه ستاً من شوال كان كصيام الدهر (رواه مسلم)."),
        "dhulHijjah" to Named("عشر ذي الحجة", "أفضل أيام الدنيا، يُستحب فيها العمل الصالح والتكبير."),
        "arafah" to Named("يوم عرفة", "صيامه يكفّر السنة الماضية والباقية لغير الحاج (رواه مسلم)."),
        "eidAdha" to Named("عيد الأضحى", "يوم النحر، تليه أيام التشريق الثلاثة."),
        "newYear" to Named("رأس السنة الهجرية", "بداية عام هجري جديد."),
        "ashura" to Named("يوم عاشوراء", "صيامه يكفّر السنة الماضية (رواه مسلم)، ويُستحب صيام التاسع معه."),
    )
    override val unitDays = "يوم"
    override val unitHours = "ساعة"
    override val unitMinutes = "دقيقة"
    override val unitSeconds = "ثانية"
    override val eventsNote = "التواريخ تقريبية وفق أم القرى، وقد تختلف يوماً بحسب رؤية الهلال في بلدك."

    // ---------- المواريث ----------
    override val inhTitle = "حاسبة المواريث"
    override val inhSubtitle = "تقسيم التركة وفق علم الفرائض"
    override val aslLabel = "أصل المسألة"
    override val awlCase = "مسألة عائلة"
    override val raddCase = "مسألة فيها ردّ"
    override val adilCase = "مسألة عادلة"
    override fun tashihFrom(n: Long) = "تصح من " + arCount(n, "سهم واحد", "سهمين", "أسهم", "سهماً", "سهم")
    override val sharesTitle = "نصيب كل وارث"
    override fun blockedBy(by: String) = "محجوب بـ$by"
    override fun perPerson(v: String) = "للفرد $v"
    override val shareDivision = "مشاركة القسمة"
    override fun divisionShareText(list: String) = "تقسيم التركة: $list"
    override val listSeparator = "، "
    override val addHeirs = "أضف الورثة من القائمة أدناه"
    override val deceasedAndEstate = "المتوفى والتركة"
    override val deceasedMale = "المتوفى ذكر"
    override val deceasedFemale = "المتوفاة أنثى"
    override val estateTotal = "إجمالي التركة"
    override val funeralCosts = "تكاليف التجهيز والدفن"
    override val deceasedDebts = "الديون على المتوفى"
    override val bequest = "الوصية (لغير وارث)"
    override val bequestHint = "لا تزيد على ثلث الباقي بعد الديون إلا بإجازة الورثة"
    override val netEstate = "صافي التركة القابل للقسمة"
    override val bequestCapped = "(قُصرت الوصية على الثلث)"
    override val newCase = "بدء مسألة جديدة"
    override val inhDisclaimer = "النتيجة إرشادية مبنية على مذهب الجمهور، ولا تغني عن مراجعة المحكمة الشرعية أو عالم متخصص في الفرائض."
    override val heirs = mapOf(
        "husband" to "الزوج", "wife" to "الزوجة", "father" to "الأب", "mother" to "الأم",
        "gf" to "الجد (أبو الأب)", "gmm" to "الجدة (أم الأم)", "gmf" to "الجدة (أم الأب)",
        "son" to "الابن", "daughter" to "البنت", "gson" to "ابن الابن", "gdau" to "بنت الابن",
        "fb" to "الأخ الشقيق", "fs" to "الأخت الشقيقة", "pb" to "الأخ لأب", "ps" to "الأخت لأب",
        "mb" to "الأخ لأم", "ms" to "الأخت لأم", "fnep" to "ابن الأخ الشقيق", "pnep" to "ابن الأخ لأب",
        "fun" to "العم الشقيق", "pun" to "العم لأب", "fcous" to "ابن العم الشقيق", "pcous" to "ابن العم لأب",
    )
    override val heirGroups = mapOf(
        "spouses" to "الزوجان", "ascendants" to "الأصول", "descendants" to "الفروع",
        "siblings" to "الإخوة والأخوات", "distant" to "العصبات البعيدة",
    )
    override val blockers = mapOf(
        "gf" to "الجد",
        "twoDaughters" to "البنتين (استكمال الثلثين)",
        "descendant" to "الفرع الوارث",
        "fbAfterMuadda" to "الأخ الشقيق (بعد المعادّة)",
        "fsTookRest" to "استغراق الأخت الشقيقة للباقي",
        "gfAndSiblings" to "الجد والإخوة",
        "twoFullSisters" to "الأختين الشقيقتين",
        "fsGhayr" to "الأخت الشقيقة (عصبة مع الغير)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "فرض"
        ShareType.FARD_TASIB -> "فرض وتعصيب"
        ShareType.FARD_TAKMILA -> "فرض (تكملة الثلثين)"
        ShareType.TASIB -> "تعصيب"
        ShareType.TASIB_BIL_GHAYR -> "تعصيب بالغير"
        ShareType.TASIB_MAA_GHAYR -> "تعصيب مع الغير"
        ShareType.TASIB_MAA_JADD -> "تعصيب مع الجد"
        ShareType.FARD_THEN_MUQASAMA -> "فرض ثم مقاسمة"
        ShareType.JADD_MUQASAMA -> "مع الإخوة: المقاسمة"
        ShareType.JADD_THIRD_ESTATE -> "مع الإخوة: ثلث المال"
        ShareType.JADD_THIRD_REST -> "مع الإخوة: ثلث الباقي"
        ShareType.JADD_SIXTH -> "مع الإخوة: سدس المال"
    } + if (radd) " وردّ" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "الإخوة لأم يشتركون في الثلث بالتساوي بين الذكر والأنثى."
        InhNote.Umariyya -> "هذه إحدى المسألتين العُمَريّتين (الغرّاوين): للأم ثلث الباقي بعد نصيب الزوج أو الزوجة."
        InhNote.GrandmothersShare -> "الجدتان تشتركان في السدس."
        InhNote.FullSistersGhayr -> "الأخوات الشقيقات مع البنات عصبة مع الغير."
        InhNote.Akdariyya -> "هذه المسألة الأكدرية: يُفرض للأخت النصف وللجد السدس فتعول إلى 9، ثم يُجمع نصيبهما ويُقسم للذكر مثل حظ الأنثيين فتصح من 27."
        is InhNote.JaddRule -> "الجد مع الإخوة يأخذ الأحظّ له من: المقاسمة، أو " +
            (if (n.otherFard) "ثلث الباقي، أو سدس المال" else "ثلث المال") +
            " — وهو قول الجمهور (المالكية والشافعية والحنابلة)، أما أبو حنيفة فيرى أن الجد يحجب الإخوة كالأب."
        InhNote.Muadda -> "الإخوة لأب عُدّوا على الجد في المقاسمة (المعادّة) ثم يُرد نصيبهم للأشقاء عند الحاجة."
        is InhNote.NothingLeft -> "لم يبقَ شيء بعد أصحاب الفروض، فلا شيء للعاصب (${nameOf(n.winner)})."
        InhNote.Mushtaraka -> "هذه المسألة المُشتَرَكة (الحِماريّة): ذهب المالكية والشافعية إلى تشريك الأشقاء مع الإخوة لأم في الثلث، والنتيجة هنا على قول الحنفية والحنابلة."
        is InhNote.Awl -> "زادت الفروض على أصل المسألة فعالت من ${n.asl} إلى ${n.awl}، ونقص نصيب كل وارث بنسبة العول."
        InhNote.Radd -> "لا يوجد عاصب، فرُدّ الباقي على أصحاب الفروض بنسبة فروضهم عدا الزوجين."
        InhNote.SpouseOnlyRadd -> "لم يوجد وارث غير الزوج/الزوجة: الباقي يُرد عليه عند كثير من المعاصرين وفي عدد من قوانين الأحوال الشخصية، والأصل عند المتقدمين أنه لذوي الأرحام ثم لبيت المال."
        InhNote.NoHeirs -> "لا يوجد وارث من أصحاب الفروض أو العصبات؛ تنتقل التركة إلى ذوي الأرحام ثم بيت المال، ويُرجع في ذلك للمحكمة المختصة."
    }

    // ---------- القرآن الكريم ----------
    override val tabQuran = "القرآن"
    override val quranTitle = "القرآن الكريم"
    override val quranSubtitle = "اضغط على أي آية لقراءة تفسيرها"
    override val surahsTab = "السور"
    override val juzTab = "الأجزاء"
    override val bookmarksTab = "المحفوظة"
    override val continueReading = "تابع القراءة"
    override val startReading = "ابدأ القراءة"
    override val meccan = "مكية"
    override val medinan = "مدنية"
    override fun ayahCount(n: Int) = arCount(n.toLong(), "آية واحدة", "آيتان", "آيات", "آية", "آية")
    override fun juzName(n: Int) = "الجزء $n"
    override fun pageName(n: Int) = "الصفحة $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · الآية $ayah"
    override val surahNames = QuranMeta.NAMES_AR.toList()
    override val searchQuranHint = "ابحث عن سورة أو كلمات من الآيات…"
    override val searchQuranGroup = "السور"
    override val searchAyahsGroup = "الآيات"
    override val mushafView = "عرض المصحف"
    override val ayahView = "آية آية"
    override val textLarger = "تكبير الخط"
    override val textSmaller = "تصغير الخط"
    override val tafsirTitle = "التفسير"
    override val tafsirMuyassar = "التفسير الميسر"
    override val tafsirJalalayn = "تفسير الجلالين"
    override val translationOfMeanings = "ترجمة معاني القرآن"
    override val bookmarkAyah = "حفظ الآية"
    override val removeBookmark = "إزالة من المحفوظة"
    override val copy = "نسخ"
    override val copied = "تم النسخ ✓"
    override val prevAyah = "الآية السابقة"
    override val nextAyah = "الآية التالية"
    override val prevSurah = "السورة السابقة"
    override val nextSurah = "السورة التالية"
    override val noBookmarks = "لا توجد آيات محفوظة بعد. افتح تفسير أي آية واضغط علامة الحفظ."
    override val quranLoading = "جارٍ فتح المصحف…"
    override fun hizbName(n: Int) = "الحزب $n"
    override val khatmaTitle = "ختمتي"
    override val khatmaPlanLabel = "المدة"
    override fun planDays(days: Int) = if (days == 0) "بلا مدة" else "$days يوماً".let { if (days <= 10) "$days أيام" else it }
    override fun pagesRead(read: Int, total: Int) = "$read من $total صفحة"
    override val todayPortion = "ورد اليوم"
    override fun streakDays(n: Int) = "أيام متتالية: $n"
    override fun daysLeftLabel(n: Int) = "الأيام الباقية: $n"
    override val portionDone = "أتممت ورد اليوم ✓"
    override val khatmaDone = "ختمت القرآن ✓ تقبّل الله منك"
    override val newKhatma = "ابدأ ختمة جديدة"
    override fun khatmasCompleted(n: Int) = "الختمات المكتملة: $n"
    override val continuePortion = "تابع وردي"
    override val ayahOfDay = "آية اليوم"
    override val focusMode = "وضع التركيز"
    override val exitFocus = "الخروج من وضع التركيز"
    override val silenceTitle = "إسكات الإشعارات أثناء القراءة؟"
    override val silenceBody = "في وضع التركيز يمكن لميزان تفعيل «عدم الإزعاج» ثم إيقافه حين تنتهي من القراءة. سيطلب منك أندرويد السماح بذلك مرة واحدة."
    override val allow = "السماح"
    override val notNow = "ليس الآن"
    override val tafsirLibrary = "مكتبة التفاسير"
    override val tafsirLibrarySub = "نزّل التفاسير واختر ما يظهر منها عند الضغط على آية"
    override val onDevice = "على جهازك"
    override val availableDownloads = "متاح للتنزيل"
    override val download = "تنزيل"
    override fun downloadingPct(pct: Int) = "جارٍ التنزيل… $pct%"
    override val cancel = "إلغاء"
    override val delete = "حذف"
    override fun deleteTafsirQ(name: String) = "حذف «$name» من جهازك؟"
    override val showInTafsir = "إظهار في التفسير"
    override val builtIn = "مدمج في التطبيق"
    override fun sizeMb(mb: String) = "نحو $mb ميغابايت"
    override val downloadFailed = "تعذّر التنزيل. تحقق من اتصالك بالإنترنت."
    override val retry = "إعادة المحاولة"
    override val moreTafsirs = "تفاسير أخرى"
    override val tafsirLibraryNote = "تُنزَّل التفاسير من مجموعة tafsir_api المفتوحة (عن quran.com وQUL). لا يُستخدم الإنترنت إلا لهذا التنزيل، وبعده تعمل التفاسير دون اتصال."
    override fun ayahRange(from: Int, to: Int) = "تفسير الآيات $from–$to"
    override val recitation = "التلاوة"
    override val reciter = "القارئ"
    override val playRecitation = "تشغيل التلاوة"
    override val pauseRecitation = "إيقاف مؤقت"
    override val stopRecitation = "إيقاف التلاوة"
    override val repeatAyah = "تكرار كل آية"
    override val listenFromHere = "استمع من هذه الآية"
    override val listen = "استماع"
    override val save = "حفظ"
    override val saved = "محفوظة"
    override val ayahActionsHint = "اضغط مطوّلاً على أي آية للتفسير والاستماع والحفظ والنسخ والمشاركة"
    override val audioNote = "تُشغَّل التلاوات من موقع EveryAyah.com، وما استمعت إليه من الآيات يُحفظ على جهازك فيعمل دون إنترنت."
    override val audioError = "تعذّر تشغيل التلاوة. تحقّق من اتصالك بالإنترنت."
    override val hifzMode = "الحفظ"
    override val revealNext = "أظهر التالية"
    override val hideAll = "إخفاء الكل"
    override val hifzHint = "اقرأ من حفظك، ثم المس الآية المخفية لتتأكد منها."
    override val displayTitle = "العرض"
    override val pageColor = "لون الصفحة"
    override val toneAuto = "تلقائي"
    override val toneSepia = "ورقي"
    override val toneNight = "ليلي"
    override val brightness = "السطوع"
    override val brightnessSystem = "سطوع الجهاز"
    override val textSizeLabel = "حجم الخط"
    override val continueFocus = "تابع في وضع التركيز"
    override val smartPresets = "إعدادات ذكية"
    override val presetListen = "استماع"
    override val presetMemorize = "حفظ"
    override val presetReview = "مراجعة"
    override val smartHint = "الحفظ: كل آية ٣ مرات مع وقفة لتكررها بعد القارئ. المراجعة: السورة كاملة بسرعة أعلى قليلاً."
    override val range = "المقطع"
    override val rangeContinue = "متواصل"
    override val rangePage = "هذه الصفحة"
    override val rangeSurah = "هذه السورة"
    override val rangeCustom = "مخصص"
    override val fromAyah = "من"
    override val toAyah = "إلى"
    override val repeatRange = "تكرار المقطع"
    override val pauseAfter = "وقفة بعد كل آية"
    override val off = "بلا"
    override val pauseAyahLength = "بطول الآية"
    override fun seconds(n: Int) = "$n ث"
    override val speed = "السرعة"
    override val sleepTimer = "مؤقت الإيقاف"
    override fun minutes(n: Int) = "$n د"
    override val evidenceTitle = "الدليل وأقوال الفقهاء"
    override val evidenceSub = "مصدر الحكم من القرآن والسنة وأقوال المذاهب"
    override val proofQuran = "القرآن الكريم"
    override val proofHadith = "السنة النبوية"
    override val proofConsensus = "الإجماع"
    override val proofScholars = "أقوال الفقهاء"
    override val zakatTypesTitle = "أنواع الزكاة الخمسة"
    override val zakatTypesSub = "اختر نوع المال لتحسب زكاته وترى دليلها"
    override val zkMoneyName = "النقدان: الذهب والفضة والنقود"
    override val zkMoneyDesc = "النقود والأرصدة والذهب والفضة: ربع العشر إذا بلغت النصاب وحال الحول"
    override val zkTradeName = "عروض التجارة"
    override val zkTradeDesc = "البضائع المعدّة للبيع والأسهم، تُقوَّم بسعرها آخر الحول"
    override val zkLivestockName = "بهيمة الأنعام"
    override val zkLivestockDesc = "الإبل والبقر والغنم السائمة إذا بلغت النصاب"
    override val zkCropsName = "الزروع والثمار"
    override val zkCropsDesc = "عند الحصاد: العشر أو نصفه حسب السقي"
    override val zkMineralsName = "المعادن والركاز"
    override val zkMineralsDesc = "الخمس في الركاز، وربع العشر في المعادن"
    override val zkCombinedNote = "النقود والذهب والفضة وعروض التجارة تُضم في نصاب واحد، لذلك تُحسب معاً في صفحة واحدة."
    override val camels = "الإبل"
    override val cattle = "البقر"
    override val sheepGoats = "الغنم (الضأن والمعز)"
    override val unitHead = "رأس"
    override val livestockDue = "الواجب في الأنعام"
    override val nothingDue = "لا زكاة فيها"
    override fun livestockNisab(n: Int) = "النصاب: $n"
    override val grazingCondition = "ترعى أكثر العام من الكلأ المباح (سائمة)"
    override val yearCondition = "مرّ عليها عام هجري كامل في ملكي"
    override val conditionsNotMet = "لا تجب الزكاة حتى يتحقق الشرطان، وما أُعدّ منها للبيع يُزكّى مع عروض التجارة."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "شاة"; "bintMakhad" -> "بنت مخاض (أنثى إبل لها سنة)"; "bintLabun" -> "بنت لبون (لها سنتان)"; "hiqqah" -> "حِقّة (لها ثلاث سنين)"; "jadhaah" -> "جَذَعة (لها أربع سنين)"; "tabi" -> "تبيع (عجل له سنة)"; "musinnah" -> "مُسنّة (بقرة لها سنتان)"; else -> kind }
    override val cropsWeight = "وزن المحصول"
    override val irrigation = "طريقة السقي"
    override val irrRain = "بلا كلفة (مطر، عيون)"
    override val irrIrrigated = "بكلفة (آلات، ماء مشترى)"
    override val irrMixed = "بهما مناصفة"
    override val cropsDue = "الواجب من المحصول"
    override fun cropsNisab(kg: String) = "النصاب: نحو $kg كغ"
    override val zkRate = "مقدار الواجب"
    override val rikazValue = "قيمة الركاز (الكنز المدفون)"
    override val mineralValue = "قيمة المعدن المستخرج"
    override val mineralsDue = "الواجب في المعادن والركاز"
    override val obSkip = "تخطي"
    override val obNext = "التالي"
    override val obStart = "ابدأ"
    override val obLocationTitle = "أين أنت؟"
    override val obLocationBody = "نحتاج موقعك لحساب مواقيت الصلاة واتجاه القبلة بدقة. يبقى موقعك على جهازك ولا يُرسل إلى أي خادم."
    override val locationNeeded = "حدّد موقعك لتظهر مواقيت الصلاة الصحيحة لمدينتك"
    override val appGuide = "دليل التطبيق"
    override val appGuideSub = "جولة سريعة في أهم ما في ميزان"
    override val onboarding = listOf(
        "أهلاً بك في ميزان" to "أدوات المسلم في تطبيق واحد: الصلاة والقبلة والقرآن والزكاة والمواريث، بلا إعلانات، وأكثرها يعمل دون إنترنت.",
        "مواقيت الصلاة والقبلة" to "مواقيت دقيقة لمدينتك مع تنبيه الأذان، واتجاه القبلة بالبوصلة، والتاريخ الهجري والمناسبات.",
        "القرآن الكريم" to "صفحات المصحف والتفسير والتلاوة، ووضع التركيز والحفظ، وختمة يومية تتابع تقدمك ومواظبتك.",
        "حاسبات بأدلتها" to "الزكاة بأنواعها الخمسة والمواريث والكفارات، ومع كل حكم دليله من القرآن والسنة وأقوال الفقهاء.",
        "ابحث عن أي شيء" to "من شريط البحث في الرئيسية: الأدوات والمدن والسور والآيات والأذكار، بأي لغة. ويمكنك إعادة هذه الجولة من الإعدادات.",
    )
    override val quranSources = "نص القرآن الكريم بالرسم العثماني من مشروع تنزيل (tanzil.net)، والتفسير الميسر (مجمع الملك فهد) وتفسير الجلالين وترجمات المعاني من tanzil.net/trans. خط المصحف: Amiri Quran برخصة SIL OFL. والتلاوات من EveryAyah.com."

    // ---------- الإعدادات ----------
    override val settingsTitle = "الإعدادات"
    override val appearance = "المظهر"
    override val themeAuto = "تلقائي"
    override val themeLight = "فاتح"
    override val themeDark = "داكن"
    override val appColor = "لون التطبيق"
    override val accentNames = listOf("زمردي", "فيروزي", "أزرق", "بنفسجي", "وردي", "عنابي", "برتقالي", "عسلي", "بني", "رمادي")
    override val language = "اللغة"
    override val deviceLanguage = "لغة الجهاز"
    override val use24h = "نظام 24 ساعة"
    override val use24hDesc = "عرض الأوقات بصيغة 24 ساعة"
    override val exactAlarm = "التنبيه في الوقت الدقيق"
    override val exactAlarmDesc = "اسمح للتطبيق بالمنبهات الدقيقة لضبط وقت الأذان"
    override val about = "عن التطبيق"
    override val aboutName = "ميزان — أدوات المسلم"
    override val adhkarNow = "وقتها الآن"
    override val dhikrEvidence = "الدليل"
    override val dhikrSource = "المصدر"
    override val dhikrGrade = "درجة الحديث"
    override val dhikrNarrator = "الراوي"
    override val dhikrVirtue = "الفضل"
    override val dhikrQuranRef = "من القرآن الكريم"
    override val dhikrDone = "أتممت هذه الأذكار، تقبّل الله منك"
    override val dhikrTapHint = "اضغط على الذكر لتعدّ، وعلى «الدليل» لترى مصدره ودرجته"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, =1 {مرة واحدة} =2 {مرتان} few {# مرات} other {# مرة}}", mapOf("n" to n), "ar")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, =1 {ذكر واحد} =2 {ذكران} few {# أذكار} other {# ذكرًا}}", mapOf("n" to n), "ar")
    override val moreLanguages = "تنزيل لغات أخرى"
    override val langPackHint = "تُنزَّل اللغات من موقع ميزان مع ترجمة معاني القرآن، ثم تعمل دون إنترنت."
    override val update = "تحديث"
    override fun version(v: String) = "الإصدار $v"
    override val aboutText = "تطبيق مجاني يجمع أدوات يحتاجها المسلم يومياً: الزكاة والمواريث ومواقيت الصلاة والقبلة والتاريخ الهجري والمسبحة. " +
        "تعتمد الحسابات على المعتمد عند جمهور الفقهاء، ومواقيت الصلاة على معادلات فلكية، والتاريخ على تقويم أم القرى."
    override val privacy = "الخصوصية"
    override val privacyText = "لا يجمع التطبيق أي بيانات شخصية ولا يستخدم الإنترنت إلا لتنزيل التفاسير التي تختارها وتشغيل التلاوة حين تطلبها. يُستخدم الموقع على جهازك فقط لحساب المواقيت والقبلة، " +
        "وتُحفظ إعداداتك وأرقامك على الجهاز ولا تُرسل إلى أي جهة."
    override val disclaimer = "النتائج إرشادية، ويُرجع في المسائل الشرعية الخاصة إلى أهل العلم والجهات الرسمية المختصة."
}
