package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** In Persian a noun stays singular after a number: "1 روز", "5 روز" */
private fun fa(n: Long, noun: String) = "$n $noun"

object Fa : Strings {
    // ---------- General ----------
    override val appName = "میزان"
    override val appTagline = "ابزارهای مسلمان"
    override val tabHome = "خانه"
    override val tabPrayer = "نماز"
    override val tabQibla = "قبله"
    override val tabTasbih = "تسبیح"
    override val tabSettings = "تنظیمات"
    override val back = "بازگشت"
    override val share = "اشتراک‌گذاری"
    override val shareSignature = "— اپلیکیشن میزان"
    override val today = "امروز"
    override val tomorrow = "فردا"
    override fun inDays(n: Long) = fa(n, "روز دیگر")
    override fun daysCount(n: Long) = fa(n, "روز")
    override val decrease = "کاهش"
    override val increase = "افزایش"
    override val noLimit = "بدون محدودیت"
    override val noResults = "نتیجه‌ای یافت نشد"
    override val unitGram = "گرم"
    override val unitKg = "کیلوگرم"
    override val unitKm = "کیلومتر"
    override val am = "ق.ظ"
    override val pm = "ب.ظ"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )
    override val gregMonths = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )
    override val weekdays = listOf("دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه", "یکشنبه")
    override val weekdaysShort = listOf("د", "س", "چ", "پ", "ج", "ش", "ی")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} $y هـ.ق"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y هـ.ق"
    override val hijriYearSuffix = "هـ.ق"
    override val gregYearSuffix = "م"
    override val dayLabel = "روز"
    override val monthLabel = "ماه"
    override val yearLabel = "سال"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("محاسبه‌گر زکات", "پول، طلا و سهام، همراه با نصاب"),
        "inheritance" to Named("محاسبه‌گر ارث", "تقسیم ترکه طبق علم فرائض"),
        "prayer" to Named("اوقات نماز", "اعلان اذان و شمارش معکوس"),
        "qibla" to Named("جهت قبله", "قطب‌نما با حسگرهای گوشی"),
        "hijri" to Named("تاریخ هجری قمری", "تبدیل تاریخ، تقویم ام‌القری"),
        "events" to Named("مناسبت‌های اسلامی", "رمضان، عیدین و روز عرفه"),
        "tasbih" to Named("تسبیح", "شمارشگر ذکر با اذکار آماده"),
        "fitr" to Named("زکات فطره", "به صاع یا به ارزش نقدی"),
        "age" to Named("محاسبه‌گر سن", "به سال قمری و میلادی"),
        "fidya" to Named("فدیه و کفاره", "روزه‌های ازدست‌رفته و قسم"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "زکات زکوه مال طلا نقره نصاب پول پس‌انداز پسانداز سهام صدقه ثروت",
        "inheritance" to "ارث میراث ترکه ماترک وارث ورثه فرائض فرایض سهم‌الارث سهمالارث وصیت",
        "prayer" to "نماز صلات اذان صبح فجر ظهر عصر مغرب عشا اوقات شرعی وقت",
        "qibla" to "قبله کعبه مکه قطب‌نما قطبنما جهت",
        "hijri" to "تقویم تاریخ هجری قمری میلادی تبدیل ام‌القری امالقری",
        "events" to "رمضان عید فطر قربان اضحی عرفه عاشورا مناسبت تعطیلات",
        "tasbih" to "تسبیح ذکر اذکار تسبیحات شمارشگر شمارنده",
        "fitr" to "فطره فطریه فطر صاع عید",
        "age" to "سن عمر تولد سالگرد",
        "fidya" to "فدیه کفاره قسم سوگند یمین روزه اطعام",
    )
    override val toolsTitle = "ابزارها"
    override fun nextPrayerIn(city: String) = "نماز بعدی · $city"
    override val searchHint = "جستجوی ابزار، شهر، ذکر…"

    // ---------- Search ----------
    override val searchTitle = "جستجو"
    override val searchToolsGroup = "ابزارها"
    override val searchCitiesGroup = "شهرها"
    override val searchPrayersGroup = "اوقات نماز امروز"
    override val searchEventsGroup = "مناسبت‌ها"
    override val searchDhikrGroup = "اذکار"
    override val searchSettingsGroup = "تنظیمات"
    override val searchEmpty = "جستجو در ابزارها، شهرها، اذکار، مناسبت‌ها و تنظیمات"
    override val setAsMyCity = "استفاده برای اوقات نماز"
    override fun searchNoResults(q: String) = "نتیجه‌ای برای «$q» یافت نشد"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "صبح", "sunrise" to "طلوع آفتاب", "dhuhr" to "ظهر",
        "asr" to "عصر", "maghrib" to "مغرب", "isha" to "عشا"
    )
    override val prayerTitle = "اوقات نماز"
    override fun nextPrayerLabel(name: String) = "نماز بعدی: $name"
    override fun atTime(time: String) = "ساعت $time"
    override val prevDay = "روز قبل"
    override val nextDay = "روز بعد"
    override val monthTable = "جدول ماهانه"
    override val hideMonthTable = "پنهان‌کردن جدول"
    override fun monthTableTitle(m: Int, y: Int) = "جدول اوقات ${gregMonths[m - 1]} $y"
    override val colDay = "روز"
    override val locationAndMethod = "موقعیت و روش محاسبه"
    override val useMyLocation = "استفاده از موقعیت فعلی من"
    override val locating = "در حال یافتن موقعیت شما…"
    override val orPickCity = "یا یک شهر انتخاب کنید"
    override val pickCity = "انتخاب شهر"
    override val searchCity = "جستجوی شهر"
    override val myLocation = "موقعیت من"
    override val calcMethod = "روش محاسبه"
    override val methods = mapOf(
        "makkah" to "ام‌القری (عربستان سعودی)",
        "mwl" to "رابطه جهان اسلام",
        "egypt" to "سازمان عمومی نقشه‌برداری مصر",
        "dubai" to "امارات (دبی)",
        "kuwait" to "کویت",
        "qatar" to "قطر",
        "jordan" to "اردن و فلسطین",
        "morocco" to "مراکش",
        "algeria" to "الجزایر",
        "tunisia" to "تونس",
        "karachi" to "دانشگاه علوم اسلامی کراچی",
        "turkey" to "دیانت (ترکیه)",
        "tehran" to "مؤسسه ژئوفیزیک دانشگاه تهران",
        "indonesia" to "وزارت امور دینی (اندونزی)",
        "malaysia" to "JAKIM (مالزی)",
        "singapore" to "MUIS (سنگاپور)",
        "russia" to "اداره دینی مسلمانان روسیه",
        "isna" to "آمریکای شمالی (ISNA)",
        "france" to "اتحادیه سازمان‌های اسلامی فرانسه",
    )
    override val asrMethod = "روش محاسبه عصر"
    override val asrStandard = "جمهور (شافعی، مالکی، حنبلی)"
    override val asrHanafi = "حنفی"
    override val latLng = "عرض / طول جغرافیایی"
    override val adhanAlert = "اعلان اذان"
    override val adhanAlertDesc = "اعلان هنگام فرارسیدن وقت هر نماز"
    override val timesMayDiffer = "ممکن است اوقات یک یا دو دقیقه با تقویم رسمی کشور شما تفاوت داشته باشد."
    override val locationFound = "موقعیت شما پیدا شد ✓"
    override val locationFailed = "موقعیت شما به دست نیامد. مطمئن شوید خدمات مکان‌یابی روشن است."
    override val locationDenied = "اجازه دسترسی به موقعیت داده نشد. شهر خود را از فهرست انتخاب کنید."
    override val cities = mapOf(
        "makkah" to "مکه", "madinah" to "مدینه", "riyadh" to "ریاض", "jeddah" to "جده",
        "dammam" to "دمام", "abha" to "ابها", "dubai" to "دبی", "abudhabi" to "ابوظبی", "sharjah" to "شارجه",
        "kuwait" to "کویت", "doha" to "دوحه", "manama" to "منامه", "muscat" to "مسقط",
        "cairo" to "قاهره", "alexandria" to "اسکندریه", "aswan" to "اسوان", "amman" to "امان",
        "jerusalem" to "بیت‌المقدس (قدس)", "gaza" to "غزه", "beirut" to "بیروت", "damascus" to "دمشق", "aleppo" to "حلب",
        "baghdad" to "بغداد", "basra" to "بصره", "erbil" to "اربیل", "sanaa" to "صنعا", "aden" to "عدن",
        "khartoum" to "خارطوم", "tripoli" to "طرابلس", "benghazi" to "بنغازی", "tunis" to "تونس",
        "algiers" to "الجزیره", "oran" to "وهران", "rabat" to "رباط", "casablanca" to "کازابلانکا",
        "marrakesh" to "مراکش", "nouakchott" to "نواکشوت", "mogadishu" to "موگادیشو", "djibouti" to "جیبوتی",
        "istanbul" to "استانبول", "ankara" to "آنکارا", "karachi" to "کراچی", "lahore" to "لاهور",
        "islamabad" to "اسلام‌آباد", "delhi" to "دهلی", "mumbai" to "بمبئی", "hyderabad" to "حیدرآباد",
        "dhaka" to "داکا", "chittagong" to "چیتاگونگ", "kabul" to "کابل", "tehran" to "تهران", "mashhad" to "مشهد",
        "tashkent" to "تاشکند", "baku" to "باکو", "almaty" to "آلماتی", "moscow" to "مسکو", "kazan" to "کازان",
        "grozny" to "گروزنی", "kualalumpur" to "کوالالامپور", "penang" to "پنانگ", "singapore" to "سنگاپور",
        "brunei" to "بندر سری بگاوان", "jakarta" to "جاکارتا", "surabaya" to "سورابایا", "bandung" to "باندونگ",
        "medan" to "مدان", "makassar" to "ماکاسار", "lagos" to "لاگوس", "kano" to "کانو", "dakar" to "داکار",
        "sarajevo" to "سارایوو", "london" to "لندن", "birmingham" to "بیرمنگام", "paris" to "پاریس",
        "marseille" to "مارسی", "lyon" to "لیون", "brussels" to "بروکسل", "amsterdam" to "آمستردام",
        "berlin" to "برلین", "hamburg" to "هامبورگ", "cologne" to "کلن", "munich" to "مونیخ",
        "vienna" to "وین", "stockholm" to "استکهلم", "newyork" to "نیویورک", "chicago" to "شیکاگو",
        "losangeles" to "لس‌آنجلس", "toronto" to "تورنتو", "montreal" to "مونترال", "sydney" to "سیدنی",
    )
    override val palestine = "فلسطین"

    // ---------- Notifications ----------
    override val channelName = "اوقات نماز"
    override val channelDesc = "اعلان هنگام فرارسیدن وقت هر نماز"
    override fun prayerNowTitle(prayer: String) = "وقت نماز $prayer فرا رسید"
    override fun prayerNowText(city: String) = "$city — حی علی الصلاة"

    // ---------- Qibla ----------
    override val qiblaTitle = "جهت قبله"
    override val noCompass = "دستگاه شما قطب‌نما ندارد. آن را با کمک زاویه زیر به‌صورت دستی تنظیم کنید."
    override val facingQibla = "✓ رو به قبله هستید"
    override fun turnRight(deg: Int) = "گوشی را $deg° به راست بچرخانید"
    override fun turnLeft(deg: Int) = "گوشی را $deg° به چپ بچرخانید"
    override val qiblaAngle = "زاویه قبله از شمال جغرافیایی"
    override fun towards(dir: String) = "به سمت $dir"
    override val directions = listOf("شمال", "شمال شرقی", "شرق", "جنوب شرقی", "جنوب", "جنوب غربی", "غرب", "شمال غربی")
    override val location = "موقعیت"
    override val distanceToKaaba = "فاصله تا کعبه"
    override val phoneHeading = "جهت گوشی"
    override val compassLowAccuracy = "دقت قطب‌نما پایین است: برای تنظیم، گوشی را به شکل عدد 8 حرکت دهید و از فلزات و دستگاه‌های الکترونیکی دور نگه دارید."
    override val qiblaTip = "برای تغییر موقعیت از صفحه اوقات نماز استفاده کنید. برای خوانش دقیق، گوشی را صاف و دور از فلزات نگه دارید."

    // ---------- Tasbih ----------
    override val tasbihTitle = "تسبیح دیجیتال"
    override val tasbihSubtitle = "برای شمارش روی دایره بزنید"
    override val modeFree = "ذکر آزاد"
    override val modeAfterPrayer = "بعد از نماز"
    override fun ofGoal(n: Int) = "از $n"
    override val nextDhikr = "به ذکر بعدی بروید"
    override val completedHundred = "قبول باشد ✓ صد ذکر را کامل کردید"
    override fun completedN(n: Int) = "$n ذکر کامل شد ✓"
    override val reset = "صفر کردن"
    override val undo = "واگرد"
    override val chooseDhikr = "انتخاب ذکر"
    override val targetCount = "تعداد هدف"
    override val yourStats = "آمار شما"
    override val roundsDone = "دور کامل"
    override val todayCount = "امروز"
    override val totalCount = "مجموع"
    override val dhikrLabels = listOf(
        "سبحان الله", "الحمد لله", "الله اکبر", "لا اله الا الله", "استغفر الله",
        "سبحان الله و بحمده", "سبحان الله العظیم", "لا حول و لا قوة الا بالله", "صلوات بر پیامبر ﷺ",
    )
    override val dhikrMeanings = listOf(
        "پاک و منزه است خدا",
        "همه ستایش‌ها از آنِ خداست",
        "خدا بزرگ‌تر است",
        "هیچ معبودی جز خدا نیست",
        "از خدا آمرزش می‌خواهم",
        "پاک و منزه است خدا و او را ستایش می‌کنم",
        "پاک و منزه است خدای بزرگ",
        "هیچ نیرو و توانی جز به یاری خدا نیست",
        "خدایا، بر محمد درود فرست",
    )
    override val tamamMeaning = "هیچ معبودی جز خدای یگانه نیست و شریکی ندارد؛ فرمانروایی از آنِ اوست و ستایش از آنِ اوست، و او بر هر چیزی تواناست."
    override val tapToCount = "برای شمارش بزنید"

    // ---------- Zakat ----------
    override val zakatTitle = "محاسبه‌گر زکات"
    override val zakatSubtitle = "2.5% از مالی که به نصاب برسد و یک سال قمری بر آن بگذرد"
    override val partCash = "پول نقد و موجودی‌ها"
    override val partReceivables = "طلب‌های قابل وصول"
    override val partGold = "طلا"
    override val partSilver = "نقره"
    override val partStocks = "سهام"
    override val partTrade = "کالای تجاری"
    override val enterGoldPrice = "برای محاسبه نصاب، قیمت طلا را وارد کنید"
    override val enterSilverPrice = "برای محاسبه نصاب، قیمت نقره را وارد کنید"
    override val nisabReached = "✓ مال شما به نصاب رسیده — زکات واجب است"
    override val belowNisab = "کمتر از نصاب — زکاتی بر شما واجب نیست"
    override val enterAssets = "دارایی‌های خود را وارد کنید"
    override val zakatDue = "زکات واجب"
    override val totalAssets = "مجموع دارایی‌های مشمول زکات"
    override val deductedDebts = "بدهی‌های کسرشده"
    override val netBase = "خالص مال مشمول زکات"
    override val nisabValue = "ارزش نصاب"
    override val approxEquals = "تقریباً برابر با"
    override fun goldGrams24(g: String) = "$g گرم طلای 24 عیار"
    override val shareResult = "اشتراک‌گذاری نتیجه"
    override fun zakatShareText(zakat: String, base: String) = "زکات واجب من: $zakat (مال مشمول زکات: $base)"
    override val settingsAndPrices = "تنظیمات و قیمت‌ها"
    override val currency = "واحد پول"
    override val nisabBasis = "معیار نصاب پول"
    override val nisabGold = "طلا (85 گرم)"
    override val nisabSilver = "نقره (595 گرم)"
    override val goldPrice24 = "قیمت 1 گرم طلای 24 عیار"
    override val goldPriceHint = "از طلافروشی یا یک سایت معتبر قیمت در کشورتان"
    override val silverPrice = "قیمت 1 گرم نقره (اختیاری)"
    override val cashSection = "پول نقد و موجودی‌ها"
    override val cashInHand = "پول نقد در دست"
    override val bankBalances = "موجودی بانک و کیف پول"
    override val receivablesField = "طلب‌هایی که انتظار وصولشان را دارید"
    override val otherMoney = "سایر وجوه"
    override val goldSilverSection = "طلا و نقره (به گرم)"
    override fun goldKarat(k: Int) = "طلای $k عیار"
    override val pureSilver = "نقره خالص"
    override val includeJewelry = "احتساب زیورآلات مورد استفاده (قول حنفیه، و موافق احتیاط)"
    override val jewelryField = "از این میان، زیورآلات مورد استفاده (معادل 21 عیار)"
    override val jewelryHint = "طبق قول جمهور که زیور شخصی زکات ندارد، کسر می‌شود"
    override val investSection = "سرمایه‌گذاری و تجارت"
    override val stocksTrade = "سهام برای خرید و فروش (ارزش بازار)"
    override val stocksLong = "سهام بلندمدت (بخش مشمول زکات)"
    override val tradeGoods = "کالای تجاری (ارزش موجودی)"
    override val deductSection = "کسورات"
    override val debtsField = "بدهی‌هایی که امسال سررسید می‌شوند"
    override val debtsHint = "فقط اقساطی که در سال زکات سررسید دارند"
    override val breakdownTitle = "زکات به تفکیک نوع مال"
    override val zakatNote = "اطلاعات شما فقط روی دستگاهتان می‌ماند. خداوند مصارف هشت‌گانه زکات را در سوره توبه (9:60) بیان کرده است."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "زکات فطره"
    override val fitrSubtitle = "پیش از نماز عید پرداخت می‌شود"
    override val foodAmountDue = "مقدار طعام واجب"
    override val totalCashValue = "مجموع ارزش نقدی"
    override fun forPeople(n: Int) = "برای $n نفر"
    override val peopleCount = "تعداد افراد (با احتساب خودتان)"
    override val modeFood = "طعام (صاع)"
    override val modeMoney = "ارزش نقدی"
    override val foodType = "نوع طعام"
    override val foods = mapOf(
        "rice" to "برنج", "riceSafe" to "برنج (احتیاط بیشتر)", "wheat" to "گندم",
        "dates" to "خرما", "barley" to "جو", "other" to "مقدار دیگر…",
    )
    override fun approxKg(kg: String) = "حدود $kg کیلوگرم"
    override val saWeight = "وزن یک صاع به کیلوگرم"
    override val kgPrice = "قیمت هر کیلوگرم (اختیاری)"
    override val kgPriceHint = "برای تخمین هزینه خرید طعام"
    override val saPerPerson = "یک صاع برای هر نفر"
    override val approxCost = "هزینه تقریبی"
    override val valuePerPerson = "مبلغ فطره برای هر نفر"
    override val valuePerPersonHint = "هر سال از سوی نهاد رسمی کشورتان اعلام می‌شود"
    override val valueNote = "پرداخت قیمت، قول حنفیه است و جمهور طعام می‌دهند."
    override val fitrTimingNote = "زمان پرداخت: پیش از نماز عید. می‌توان آن را یک یا دو روز زودتر داد، اما تأخیر آن از نماز بدون عذر جایز نیست. به فقرا و مساکین داده می‌شود."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "فدیه و کفاره"
    override val fidyaSubtitle = "مقدار اطعام به کیلوگرم و ارزش نقدی"
    override val tabFidya = "فدیه روزه"
    override val tabOath = "قسم"
    override val tabIntercourse = "جماع"
    override val fidyaResult = "فدیه روزه"
    override fun fidyaSub(days: Int) = "برای " + fa(days.toLong(), "روز") + " — هر روز یک مسکین"
    override val fidyaNote = "برای کسی که برای همیشه از روزه گرفتن ناتوان است، مانند سالمند یا بیمار مزمن. می‌توان یک مسکین را برای همه روزها اطعام کرد یا چند مسکین را."
    override val oathResult = "کفاره قسم"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths کفاره × 10 مسکین" else "اطعام 10 مسکین"
    override val oathAlternative = "در صورت ناتوانی"
    override fun fastDays(n: Int) = fa(n.toLong(), "روز روزه")
    override val oathNote = "مخیر است میان اطعام ده مسکین، پوشاندن آنان یا آزاد کردن یک برده؛ هر که نتواند، سه روز روزه بگیرد (سوره مائده 5:89)."
    override val intercourseResult = "کفاره جماع در روز ماه رمضان"
    override val intercourseSub = "اطعام 60 مسکین در صورت ناتوانی از روزه"
    override val ifStartToday = "اگر امروز روزه را آغاز کنید، پایان آن"
    override val twoMonthsDuration = "مدت دو ماه قمری"
    override val intercourseNote = "به ترتیب: آزاد کردن یک برده؛ اگر ممکن نبود، دو ماه روزه پیاپی؛ و اگر نتوانست، اطعام شصت مسکین — همراه با قضای آن روز و توبه."
    override val daysField = "تعداد روزها"
    override val oathsField = "تعداد کفاره‌ها"
    override val feedMeasure = "مقدار طعام برای هر مسکین"
    override val muddOption = "یک مُد — حدود 0.75 کیلوگرم (مالکی و شافعی)"
    override val halfSaOption = "نیم صاع — حدود 1.5 کیلوگرم (فتواهای معاصر)"
    override val poorCount = "تعداد مساکین / وعده‌ها"
    override val approxValue = "ارزش تقریبی"

    // ---------- Hijri converter ----------
    override val hijriTitle = "تاریخ هجری قمری"
    override val hijriSubtitle = "طبق تقویم ام‌القری"
    override val correspondingHijri = "تاریخ قمری متناظر"
    override val correspondingGreg = "تاریخ میلادی متناظر"
    override val enterGregYear = "سالی بین 1882 و 2174 وارد کنید"
    override val enterHijriYear = "سال قمری بین 1300 و 1600 وارد کنید"
    override val gregToHijri = "میلادی ← قمری"
    override val hijriToGreg = "قمری ← میلادی"
    override val todayDate = "تاریخ امروز"
    override val prevMonth = "ماه قبل"
    override val nextMonth = "ماه بعد"
    override val calendarHint = "عدد بزرگ قمری و عدد کوچک میلادی است. برای تبدیل، روی یک روز بزنید."
    override val moonNote = "به دلیل رؤیت هلال، ممکن است تاریخ در برخی کشورها یک روز تفاوت داشته باشد."

    // ---------- Age ----------
    override val ageTitle = "محاسبه‌گر سن"
    override val ageSubtitle = "به سال قمری و میلادی"
    override val ageGregorian = "سن شما (میلادی)"
    override fun ymd(y: Int, m: Int, d: Int) = "${fa(y.toLong(), "سال")} و ${fa(m.toLong(), "ماه")} و ${fa(d.toLong(), "روز")}"
    override fun inHijri(age: String) = "قمری: $age"
    override val birthGreg = "تاریخ تولد (میلادی)"
    override val birthHijri = "تاریخ تولد (قمری)"
    override val birthWeekday = "روز تولد"
    override val nextBirthday = "تولد بعدی"
    override val birthdayToday = "امروز 🎉"
    override val tileDays = "روز"
    override val tileWeeks = "هفته"
    override val tileHours = "ساعت"
    override val yourAge = "سن شما"
    override val birthAfterToday = "تاریخ تولد بعد از امروز است"
    override val enterBirth = "تاریخ تولد خود را وارد کنید"
    override val birthDate = "تاریخ تولد"
    override val gregorian = "میلادی"
    override val hijri = "قمری"

    // ---------- Islamic events ----------
    override val eventsTitle = "مناسبت‌های اسلامی"
    override val eventsSubtitle = "شمارش معکوس طبق تقویم ام‌القری"
    override val events = mapOf(
        "ramadan" to Named("ماه رمضان", "ماه روزه، شب‌زنده‌داری و تلاوت قرآن."),
        "lastTen" to Named("دهه آخر رمضان", "شب قدر در آن است که از هزار ماه بهتر است."),
        "eidFitr" to Named("عید فطر", "زکات فطره پیش از نماز آن پرداخت می‌شود."),
        "shawwal" to Named("شش روز شوال", "هر که رمضان را روزه بگیرد و در پی آن شش روز از شوال را روزه بدارد، گویی همه عمر را روزه گرفته است (روایت مسلم)."),
        "dhulHijjah" to Named("دهه اول ذی‌الحجه", "برترین روزهای دنیا؛ کار نیک و تکبیر در آن مستحب است."),
        "arafah" to Named("روز عرفه", "روزه‌اش کفاره سال گذشته و سال آینده است، برای کسی که در حج نیست (روایت مسلم)."),
        "eidAdha" to Named("عید قربان", "روز قربانی، و پس از آن سه روزِ ایام تشریق."),
        "newYear" to Named("سال نو هجری قمری", "آغاز سال جدید هجری قمری."),
        "ashura" to Named("روز عاشورا", "روزه‌اش کفاره سال گذشته است (روایت مسلم)؛ و روزه تاسوعا همراه آن مستحب است."),
    )
    override val unitDays = "روز"
    override val unitHours = "ساعت"
    override val unitMinutes = "دقیقه"
    override val unitSeconds = "ثانیه"
    override val eventsNote = "تاریخ‌ها تقریبی (طبق ام‌القری) هستند و ممکن است با رؤیت هلال در کشور شما یک روز تفاوت داشته باشند."

    // ---------- Inheritance ----------
    override val inhTitle = "محاسبه‌گر ارث"
    override val inhSubtitle = "تقسیم ترکه طبق علم فرائض"
    override val aslLabel = "اصل مسئله"
    override val awlCase = "مسئله با عول (سهام بیش از ترکه)"
    override val raddCase = "مسئله با رد (بازگشت مازاد)"
    override val adilCase = "مسئله عادله"
    override fun tashihFrom(n: Long) = "تصحیح به " + fa(n, "سهم")
    override val sharesTitle = "سهم هر وارث"
    override fun blockedBy(by: String) = "محجوب توسط $by"
    override fun perPerson(v: String) = "هر نفر $v"
    override val shareDivision = "اشتراک‌گذاری تقسیم"
    override fun divisionShareText(list: String) = "تقسیم ترکه: $list"
    override val listSeparator = "، "
    override val addHeirs = "وارثان را از فهرست زیر اضافه کنید"
    override val deceasedAndEstate = "متوفی و ترکه"
    override val deceasedMale = "متوفی مرد است"
    override val deceasedFemale = "متوفی زن است"
    override val estateTotal = "کل ترکه"
    override val funeralCosts = "هزینه کفن و دفن"
    override val deceasedDebts = "بدهی‌های متوفی"
    override val bequest = "وصیت (برای غیر وارث)"
    override val bequestHint = "بیش از یک سوم باقی‌مانده پس از بدهی‌ها نباشد، مگر با اجازه ورثه"
    override val netEstate = "خالص ترکه قابل تقسیم"
    override val bequestCapped = "(وصیت به یک سوم محدود شد)"
    override val newCase = "شروع مسئله جدید"
    override val inhDisclaimer = "نتیجه جنبه راهنما دارد و بر قول جمهور مبتنی است؛ جای دادگاه شرعی یا عالم متخصص فرائض را نمی‌گیرد."
    override val heirs = mapOf(
        "husband" to "شوهر", "wife" to "زن (همسر)", "father" to "پدر", "mother" to "مادر",
        "gf" to "پدربزرگ (پدر پدر)", "gmm" to "مادربزرگ (مادر مادر)", "gmf" to "مادربزرگ (مادر پدر)",
        "son" to "پسر", "daughter" to "دختر", "gson" to "پسر پسر", "gdau" to "دختر پسر",
        "fb" to "برادر تنی", "fs" to "خواهر تنی", "pb" to "برادر پدری", "ps" to "خواهر پدری",
        "mb" to "برادر مادری", "ms" to "خواهر مادری", "fnep" to "پسر برادر تنی", "pnep" to "پسر برادر پدری",
        "fun" to "عموی تنی", "pun" to "عموی پدری", "fcous" to "پسر عموی تنی", "pcous" to "پسر عموی پدری",
    )
    override val heirGroups = mapOf(
        "spouses" to "همسران", "ascendants" to "والدین و اجداد", "descendants" to "فرزندان و نوادگان",
        "siblings" to "برادران و خواهران", "distant" to "عصبه‌های دور",
    )
    override val blockers = mapOf(
        "gf" to "پدربزرگ",
        "twoDaughters" to "دو دختر (تکمیل دو سوم)",
        "descendant" to "فرزند وارث",
        "fbAfterMuadda" to "برادر تنی (پس از معادّه)",
        "fsTookRest" to "خواهر تنی (با گرفتن باقی‌مانده)",
        "gfAndSiblings" to "پدربزرگ و برادران و خواهران",
        "twoFullSisters" to "دو خواهر تنی",
        "fsGhayr" to "خواهر تنی (عصبه مع‌الغیر با دختران)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "فرض"
        ShareType.FARD_TASIB -> "فرض + تعصیب"
        ShareType.FARD_TAKMILA -> "فرض (تکمیل دو سوم)"
        ShareType.TASIB -> "تعصیب"
        ShareType.TASIB_BIL_GHAYR -> "تعصیب بالغیر (با برادر)"
        ShareType.TASIB_MAA_GHAYR -> "تعصیب مع‌الغیر (با دختران)"
        ShareType.TASIB_MAA_JADD -> "تعصیب همراه پدربزرگ"
        ShareType.FARD_THEN_MUQASAMA -> "فرض، سپس مقاسمه"
        ShareType.JADD_MUQASAMA -> "با برادران و خواهران: مقاسمه"
        ShareType.JADD_THIRD_ESTATE -> "با برادران و خواهران: یک سوم ترکه"
        ShareType.JADD_THIRD_REST -> "با برادران و خواهران: یک سوم باقی‌مانده"
        ShareType.JADD_SIXTH -> "با برادران و خواهران: یک ششم ترکه"
    } + if (radd) " + رد" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "برادران و خواهران مادری در یک سوم به‌طور مساوی شریک‌اند و مرد و زن در آن برابرند."
        InhNote.Umariyya -> "این یکی از دو مسئله عُمَریه است: مادر یک سوم باقی‌مانده پس از سهم همسر را می‌برد."
        InhNote.GrandmothersShare -> "دو مادربزرگ در یک ششم شریک‌اند."
        InhNote.FullSistersGhayr -> "خواهران تنی همراه دختران، عصبه مع‌الغیر می‌شوند."
        InhNote.Akdariyya -> "این مسئله اکدریه است: برای خواهر نصف و برای پدربزرگ یک ششم فرض می‌شود و مسئله به 9 عول می‌کند؛ سپس سهم آن دو جمع و به نسبت دو به یک تقسیم می‌شود و از 27 تصحیح می‌شود."
        is InhNote.JaddRule -> "پدربزرگ همراه برادران و خواهران، هر کدام را که برایش بهتر باشد می‌گیرد: مقاسمه، یا " +
            (if (n.otherFard) "یک سوم باقی‌مانده، یا یک ششم ترکه" else "یک سوم ترکه") +
            " — قول جمهور (مالکی، شافعی، حنبلی). ابوحنیفه معتقد است پدربزرگ مانند پدر، برادران و خواهران را محجوب می‌کند."
        InhNote.Muadda -> "برادران و خواهران پدری در مقاسمه با پدربزرگ به حساب آمدند (معادّه)، سپس سهمشان در صورت نیاز به برادران و خواهران تنی بازمی‌گردد."
        is InhNote.NothingLeft -> "پس از سهام فرض چیزی باقی نماند، پس به عصبه (${nameOf(n.winner)}) چیزی نمی‌رسد."
        InhNote.Mushtaraka -> "این مسئله مشترکه (حماریه) است: مالکیه و شافعیه برادران و خواهران تنی را با برادران و خواهران مادری در یک سوم شریک می‌دانند؛ نتیجه اینجا طبق قول حنفیه و حنابله است."
        is InhNote.Awl -> "سهام فرض از اصل مسئله بیشتر شد، پس مسئله از ${n.asl} به ${n.awl} عول کرد و سهم هر وارث به همان نسبت کم شد."
        InhNote.Radd -> "عصبه‌ای وجود ندارد، پس باقی‌مانده به نسبت سهام صاحبان فرض به آنان رد می‌شود، به‌جز همسران."
        InhNote.SpouseOnlyRadd -> "تنها وارث، همسر است: بسیاری از علمای معاصر و برخی قوانین احوال شخصیه باقی‌مانده را به او رد می‌کنند؛ فقهای متقدم آن را از آنِ ذوی‌الارحام و سپس بیت‌المال می‌دانستند."
        InhNote.NoHeirs -> "هیچ صاحب فرض یا عصبه‌ای وجود ندارد؛ ترکه به ذوی‌الارحام و سپس به بیت‌المال می‌رسد. به دادگاه صالح مراجعه کنید."
    }

    // ---------- Quran ----------
    override val tabQuran = "قرآن"
    override val quranTitle = "قرآن کریم"
    override val quranSubtitle = "برای خواندن تفسیر، روی هر آیه بزنید"
    override val surahsTab = "سوره‌ها"
    override val juzTab = "جزءها"
    override val bookmarksTab = "ذخیره‌شده‌ها"
    override val continueReading = "ادامه تلاوت"
    override val startReading = "شروع تلاوت"
    override val meccan = "مکی"
    override val medinan = "مدنی"
    override fun ayahCount(n: Int) = fa(n.toLong(), "آیه")
    override fun juzName(n: Int) = "جزء $n"
    override fun pageName(n: Int) = "صفحه $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · آیه $ayah"
    override val surahNames = listOf(
        "فاتحه", "بقره", "آل عمران", "نساء", "مائده", "انعام",
        "اعراف", "انفال", "توبه", "یونس", "هود", "یوسف",
        "رعد", "ابراهیم", "حجر", "نحل", "اسراء", "کهف",
        "مریم", "طه", "انبیاء", "حج", "مؤمنون", "نور",
        "فرقان", "شعراء", "نمل", "قصص", "عنکبوت", "روم",
        "لقمان", "سجده", "احزاب", "سبأ", "فاطر", "یس",
        "صافات", "ص", "زمر", "غافر", "فصلت", "شوری",
        "زخرف", "دخان", "جاثیه", "احقاف", "محمد", "فتح",
        "حجرات", "ق", "ذاریات", "طور", "نجم", "قمر",
        "الرحمن", "واقعه", "حدید", "مجادله", "حشر", "ممتحنه",
        "صف", "جمعه", "منافقون", "تغابن", "طلاق", "تحریم",
        "ملک", "قلم", "حاقه", "معارج", "نوح", "جن",
        "مزمل", "مدثر", "قیامت", "انسان", "مرسلات", "نبأ",
        "نازعات", "عبس", "تکویر", "انفطار", "مطففین", "انشقاق",
        "بروج", "طارق", "اعلی", "غاشیه", "فجر", "بلد",
        "شمس", "لیل", "ضحی", "شرح", "تین", "علق",
        "قدر", "بینه", "زلزله", "عادیات", "قارعه", "تکاثر",
        "عصر", "همزه", "فیل", "قریش", "ماعون", "کوثر",
        "کافرون", "نصر", "مسد", "اخلاص", "فلق", "ناس",
    )
    override val searchQuranHint = "جستجوی سوره یا متن آیات…"
    override val searchQuranGroup = "سوره‌ها"
    override val searchAyahsGroup = "آیات"
    override val mushafView = "نمای مصحف"
    override val ayahView = "آیه به آیه"
    override val textLarger = "متن بزرگ‌تر"
    override val textSmaller = "متن کوچک‌تر"
    override val tafsirTitle = "تفسیر"
    override val tafsirMuyassar = "التفسیر المیسر"
    override val tafsirJalalayn = "تفسیر جلالین"
    override val translationOfMeanings = "ترجمه معانی"
    override val bookmarkAyah = "ذخیره آیه"
    override val removeBookmark = "حذف از ذخیره‌شده‌ها"
    override val copy = "کپی"
    override val copied = "کپی شد ✓"
    override val prevAyah = "آیه قبل"
    override val nextAyah = "آیه بعد"
    override val prevSurah = "سوره قبل"
    override val nextSurah = "سوره بعد"
    override val noBookmarks = "هنوز آیه‌ای ذخیره نکرده‌اید. یک آیه را باز کنید و روی علامت ذخیره بزنید."
    override val quranLoading = "در حال باز کردن مصحف…"
    override fun hizbName(n: Int) = "حزب $n"
    override val khatmaTitle = "ختم من"
    override val khatmaPlanLabel = "مدت"
    override fun planDays(days: Int) = if (days == 0) "بدون مهلت" else "$days روز"
    override fun pagesRead(read: Int, total: Int) = "$read از $total صفحه"
    override val todayPortion = "ورد امروز"
    override fun streakDays(n: Int) = "روزهای پیاپی: $n"
    override fun daysLeftLabel(n: Int) = "روزهای باقی‌مانده: $n"
    override val portionDone = "ورد امروز تمام شد ✓"
    override val khatmaDone = "ختم قرآن تمام شد ✓ قبول باشد"
    override val newKhatma = "شروع ختم تازه"
    override fun khatmasCompleted(n: Int) = "ختم‌های کامل‌شده: $n"
    override val continuePortion = "ادامهٔ ورد"
    override val ayahOfDay = "آیهٔ روز"
    override val focusMode = "حالت تمرکز"
    override val exitFocus = "خروج از حالت تمرکز"
    override val silenceTitle = "اعلان‌ها هنگام خواندن بی‌صدا شوند؟"
    override val silenceBody = "در حالت تمرکز، میزان می‌تواند «مزاحم نشوید» را روشن کند و وقتی خواندن را تمام کردید دوباره خاموشش کند. اندروید یک بار اجازه می‌خواهد."
    override val allow = "اجازه دادن"
    override val notNow = "اکنون نه"
    override val tafsirLibrary = "کتابخانه تفسیرها"
    override val tafsirLibrarySub = "تفسیرها را دانلود کنید و انتخاب کنید کدام‌ها هنگام لمس آیه نمایش داده شوند"
    override val onDevice = "روی این دستگاه"
    override val availableDownloads = "قابل دانلود"
    override val download = "دانلود"
    override fun downloadingPct(pct: Int) = "در حال دانلود… $pct٪"
    override val cancel = "لغو"
    override val delete = "حذف"
    override fun deleteTafsirQ(name: String) = "«$name» از این دستگاه حذف شود؟"
    override val showInTafsir = "نمایش در تفسیر"
    override val builtIn = "داخلی"
    override fun sizeMb(mb: String) = "حدود $mb مگابایت"
    override val downloadFailed = "دانلود ناموفق بود. اتصال اینترنت را بررسی کنید."
    override val retry = "تلاش دوباره"
    override val moreTafsirs = "تفسیرهای دیگر"
    override val tafsirLibraryNote = "تفسیرها از مجموعه باز tafsir_api (برگرفته از quran.com و QUL) دانلود می‌شوند. اینترنت فقط برای همین دانلودها به کار می‌رود و پس از آن تفسیرها بدون اینترنت کار می‌کنند."
    override fun ayahRange(from: Int, to: Int) = "تفسیر آیات $from–$to"
    override val recitation = "تلاوت"
    override val reciter = "قاری"
    override val playRecitation = "پخش تلاوت"
    override val pauseRecitation = "مکث"
    override val stopRecitation = "توقف تلاوت"
    override val repeatAyah = "تکرار هر آیه"
    override val listenFromHere = "شنیدن از این آیه"
    override val audioNote = "تلاوت‌ها از EveryAyah.com پخش می‌شوند. آیه‌هایی که شنیده‌اید روی دستگاهتان ذخیره می‌شوند و بدون اینترنت هم پخش می‌شوند."
    override val audioError = "پخش تلاوت ممکن نشد. اتصال اینترنت خود را بررسی کنید."
    override val hifzMode = "حفظ"
    override val revealNext = "نمایش بعدی"
    override val hideAll = "پنهان کردن همه"
    override val hifzHint = "از حفظ بخوانید، سپس برای بررسی روی آیهٔ پنهان بزنید."
    override val displayTitle = "نمایش"
    override val pageColor = "رنگ صفحه"
    override val toneAuto = "خودکار"
    override val toneSepia = "سپیا"
    override val toneNight = "شب"
    override val brightness = "روشنایی"
    override val brightnessSystem = "روشنایی دستگاه"
    override val textSizeLabel = "اندازه متن"
    override val continueFocus = "ادامه در حالت تمرکز"
    override val smartPresets = "تنظیمات هوشمند"
    override val presetListen = "شنیدن"
    override val presetMemorize = "حفظ"
    override val presetReview = "مرور"
    override val smartHint = "حفظ: هر آیه ۳ بار با مکثی برای تکرار آن پس از قاری. مرور: کل سوره، کمی سریع‌تر."
    override val range = "بازه"
    override val rangeContinue = "پیوسته"
    override val rangePage = "این صفحه"
    override val rangeSurah = "این سوره"
    override val rangeCustom = "دلخواه"
    override val fromAyah = "از"
    override val toAyah = "تا"
    override val repeatRange = "تکرار بازه"
    override val pauseAfter = "مکث پس از هر آیه"
    override val off = "خاموش"
    override val pauseAyahLength = "به اندازه آیه"
    override fun seconds(n: Int) = "$n ثانیه"
    override val speed = "سرعت"
    override val sleepTimer = "زمان‌سنج خواب"
    override fun minutes(n: Int) = "$n دقیقه"
    override val quranSources = "متن قرآن کریم به رسم‌الخط عثمانی از پروژه تنزیل (tanzil.net). التفسیر المیسر (مجمع ملک فهد)، تفسیر جلالین و ترجمه‌های معانی از tanzil.net/trans. فونت قرآن: Amiri Quran، با مجوز SIL Open Font License. تلاوت‌ها: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "تنظیمات"
    override val appearance = "ظاهر"
    override val themeAuto = "خودکار"
    override val themeLight = "روشن"
    override val themeDark = "تیره"
    override val language = "زبان"
    override val deviceLanguage = "زبان دستگاه"
    override val use24h = "ساعت 24 ساعته"
    override val use24hDesc = "نمایش زمان‌ها در قالب 24 ساعته"
    override val exactAlarm = "اعلان در زمان دقیق"
    override val exactAlarmDesc = "اجازه هشدار دقیق بدهید تا اعلان اذان سر وقت برسد"
    override val about = "درباره"
    override val aboutName = "میزان — ابزارهای مسلمان"
    override fun version(v: String) = "نسخه $v"
    override val aboutText = "برنامه‌ای رایگان با ابزارهایی که مسلمان هر روز به آن‌ها نیاز دارد: زکات، ارث، اوقات نماز، قبله، تاریخ هجری قمری و تسبیح. " +
        "احکام بر پایه قول جمهور فقها، اوقات نماز بر پایه فرمول‌های نجومی و تاریخ‌ها طبق تقویم ام‌القری است."
    override val privacy = "حریم خصوصی"
    override val privacyText = "این برنامه هیچ داده شخصی جمع‌آوری نمی‌کند و فقط برای دانلود تفسیرهایی که انتخاب می‌کنید و پخش تلاوت به درخواست شما از اینترنت استفاده می‌کند. موقعیت شما فقط روی دستگاهتان و برای محاسبه اوقات نماز و قبله به کار می‌رود، " +
        "و تنظیمات و اعداد شما روی دستگاهتان می‌ماند."
    override val disclaimer = "نتایج جنبه راهنما دارند. برای پرسش‌های شرعی خود به علمای واجد شرایط و نهادهای رسمی مراجعه کنید."
}
