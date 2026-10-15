package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** اردو میں عدد کے ساتھ اسم: 1 مہینہ، 2 مہینے */
private fun ur(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "$n $many"

object Ur : Strings {
    // ---------- عمومی ----------
    override val appName = "میزان"
    override val appTagline = "مسلمانوں کے لیے ٹولز"
    override val tabHome = "ہوم"
    override val tabPrayer = "نماز"
    override val tabQibla = "قبلہ"
    override val tabTasbih = "تسبیح"
    override val tabSettings = "ترتیبات"
    override val back = "واپس"
    override val share = "شیئر کریں"
    override val shareSignature = "— میزان ایپ"
    override val today = "آج"
    override val tomorrow = "کل"
    override fun inDays(n: Long) = ur(n, "دن", "دن") + " بعد"
    override fun daysCount(n: Long) = ur(n, "دن", "دن")
    override val decrease = "کم کریں"
    override val increase = "زیادہ کریں"
    override val noLimit = "کوئی حد نہیں"
    override val noResults = "کوئی نتیجہ نہیں"
    override val unitGram = "گرام"
    override val unitKg = "کلو"
    override val unitKm = "کلومیٹر"
    override val am = "قبل دوپہر"
    override val pm = "بعد دوپہر"

    // ---------- کیلنڈر ----------
    override val hijriMonths = listOf(
        "محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذوالقعدہ", "ذوالحجہ"
    )
    override val gregMonths = listOf(
        "جنوری", "فروری", "مارچ", "اپریل", "مئی", "جون",
        "جولائی", "اگست", "ستمبر", "اکتوبر", "نومبر", "دسمبر"
    )
    override val weekdays = listOf("پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ", "اتوار")
    override val weekdaysShort = listOf("پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ", "اتوار")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${hijriMonths[m - 1]} ${y}ھ"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${gregMonths[m - 1]} ${y}ء"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} ${y}ھ"
    override val hijriYearSuffix = "ھ"
    override val gregYearSuffix = "ء"
    override val dayLabel = "دن"
    override val monthLabel = "مہینہ"
    override val yearLabel = "سال"

    // ---------- ہوم ----------
    override val tools = mapOf(
        "zakat" to Named("زکوٰۃ کیلکولیٹر", "نقدی، سونا اور شیئرز، نصاب کے ساتھ"),
        "inheritance" to Named("وراثت کیلکولیٹر", "علم فرائض کے مطابق ترکے کی تقسیم"),
        "prayer" to Named("اوقات نماز", "اذان کی اطلاع اور الٹی گنتی"),
        "qibla" to Named("سمت قبلہ", "فون کے سینسرز سے چلنے والا قطب نما"),
        "hijri" to Named("ہجری تاریخ", "تاریخ کی تبدیلی، ام القریٰ کیلنڈر"),
        "events" to Named("اسلامی ایام", "رمضان، عیدین اور یوم عرفہ"),
        "tasbih" to Named("تسبیح", "تیار اذکار کے ساتھ ذکر کاؤنٹر"),
        "fitr" to Named("صدقۂ فطر", "صاع کے حساب سے یا قیمت سے"),
        "age" to Named("عمر کیلکولیٹر", "ہجری اور عیسوی سالوں میں"),
        "fidya" to Named("فدیہ و کفارہ", "چھوٹے روزے اور ٹوٹی قسمیں"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "زکوۃ زکاۃ زکات مال سونا چاندی نصاب رقم بچت شیئرز حصص صدقہ خیرات zakat zakaat",
        "inheritance" to "وراثت میراث ترکہ ترکے ورثاء وارث فرائض وصیت wirasat virasat",
        "prayer" to "نماز صلوۃ صلاۃ اذان اوقات وقت فجر ظہر عصر مغرب عشاء namaz salah azan",
        "qibla" to "قبلہ کعبہ مکہ قطب نما کمپاس سمت رخ qibla kaaba",
        "hijri" to "ہجری عیسوی تاریخ کیلنڈر تقویم تبدیلی اسلامی ام القری چاند hijri",
        "events" to "رمضان عید عرفہ عاشورہ محرم تہوار ایام شب قدر ramzan eid",
        "tasbih" to "تسبیح ذکر اذکار کاؤنٹر گنتی وظیفہ tasbeeh zikr",
        "fitr" to "فطرہ فطرانہ صدقہ فطر صاع عید fitrana",
        "age" to "عمر سالگرہ پیدائش سال umar",
        "fidya" to "فدیہ کفارہ قسم روزہ روزے کھانا مسکین fidya kaffara",
    )
    override val toolsTitle = "ٹولز"
    override fun nextPrayerIn(city: String) = "اگلی نماز · $city"
    override val searchHint = "ٹولز، شہر، اذکار تلاش کریں…"

    // ---------- تلاش ----------
    override val searchTitle = "تلاش"
    override val searchToolsGroup = "ٹولز"
    override val searchCitiesGroup = "شہر"
    override val searchPrayersGroup = "آج کے اوقات نماز"
    override val searchEventsGroup = "اسلامی ایام"
    override val searchDhikrGroup = "اذکار"
    override val searchSettingsGroup = "ترتیبات"
    override val searchEmpty = "ٹولز، شہر، اذکار، اسلامی ایام اور ترتیبات تلاش کریں"
    override val setAsMyCity = "اوقات نماز کے لیے منتخب کریں"
    override fun searchNoResults(q: String) = "”$q“ کے لیے کوئی نتیجہ نہیں"

    // ---------- نماز ----------
    override val prayers = mapOf(
        "fajr" to "فجر", "sunrise" to "طلوع آفتاب", "dhuhr" to "ظہر",
        "asr" to "عصر", "maghrib" to "مغرب", "isha" to "عشاء"
    )
    override val prayerTitle = "اوقات نماز"
    override fun nextPrayerLabel(name: String) = "اگلی نماز: $name"
    override fun atTime(time: String) = "بوقت $time"
    override val prevDay = "پچھلا دن"
    override val nextDay = "اگلا دن"
    override val monthTable = "ماہانہ نقشۂ اوقات"
    override val hideMonthTable = "نقشہ چھپائیں"
    override fun monthTableTitle(m: Int, y: Int) = "${gregMonths[m - 1]} $y کے اوقات نماز"
    override val colDay = "تاریخ"
    override val locationAndMethod = "مقام اور طریقۂ حساب"
    override val useMyLocation = "میرا موجودہ مقام استعمال کریں"
    override val locating = "آپ کا مقام معلوم کیا جا رہا ہے…"
    override val orPickCity = "یا شہر منتخب کریں"
    override val pickCity = "شہر منتخب کریں"
    override val searchCity = "شہر تلاش کریں"
    override val myLocation = "میرا مقام"
    override val calcMethod = "طریقۂ حساب"
    override val methods = mapOf(
        "makkah" to "ام القریٰ (سعودی عرب)",
        "mwl" to "رابطہ عالم اسلامی",
        "egypt" to "مصری جنرل سروے اتھارٹی",
        "dubai" to "متحدہ عرب امارات (دبئی)",
        "kuwait" to "کویت",
        "qatar" to "قطر",
        "jordan" to "اردن و فلسطین",
        "morocco" to "مراکش",
        "algeria" to "الجزائر",
        "tunisia" to "تیونس",
        "karachi" to "جامعۃ العلوم الاسلامیہ، کراچی",
        "turkey" to "Diyanet (ترکیہ)",
        "tehran" to "انسٹی ٹیوٹ آف جیو فزکس، تہران یونیورسٹی",
        "indonesia" to "وزارت مذہبی امور (انڈونیشیا)",
        "malaysia" to "JAKIM (ملائیشیا)",
        "singapore" to "MUIS (سنگاپور)",
        "russia" to "روسی مسلمانوں کی دینی انتظامیہ",
        "isna" to "شمالی امریکہ (ISNA)",
        "france" to "فرانس کی اسلامی تنظیموں کا اتحاد",
    )
    override val asrMethod = "عصر کا حساب"
    override val asrStandard = "جمہور (شافعی، مالکی، حنبلی)"
    override val asrHanafi = "حنفی"
    override val latLng = "عرض بلد / طول بلد"
    override val adhanAlert = "اذان کی اطلاع"
    override val adhanAlertDesc = "ہر نماز کا وقت شروع ہونے پر اطلاع"
    override val timesMayDiffer = "اوقات آپ کے ملک کے سرکاری کیلنڈر سے ایک دو منٹ مختلف ہو سکتے ہیں۔"
    override val locationFound = "مقام مل گیا ✓"
    override val locationFailed = "آپ کا مقام معلوم نہیں ہو سکا۔ یقینی بنائیں کہ لوکیشن سروس آن ہے۔"
    override val locationDenied = "مقام کی اجازت نہیں دی گئی۔ فہرست سے اپنا شہر منتخب کریں۔"
    override val cities = mapOf(
        "makkah" to "مکہ مکرمہ", "madinah" to "مدینہ منورہ", "riyadh" to "ریاض", "jeddah" to "جدہ",
        "dammam" to "دمام", "abha" to "ابہا", "dubai" to "دبئی", "abudhabi" to "ابوظہبی", "sharjah" to "شارجہ",
        "kuwait" to "کویت سٹی", "doha" to "دوحہ", "manama" to "منامہ", "muscat" to "مسقط",
        "cairo" to "قاہرہ", "alexandria" to "اسکندریہ", "aswan" to "اسوان", "amman" to "عمّان",
        "jerusalem" to "بیت المقدس (القدس)", "gaza" to "غزہ", "beirut" to "بیروت", "damascus" to "دمشق", "aleppo" to "حلب",
        "baghdad" to "بغداد", "basra" to "بصرہ", "erbil" to "اربیل", "sanaa" to "صنعاء", "aden" to "عدن",
        "khartoum" to "خرطوم", "tripoli" to "طرابلس", "benghazi" to "بن غازی", "tunis" to "تیونس",
        "algiers" to "الجزائر", "oran" to "وہران", "rabat" to "رباط", "casablanca" to "کاسابلانکا",
        "marrakesh" to "مراکش", "nouakchott" to "نواکشوط", "mogadishu" to "موغادیشو", "djibouti" to "جبوتی",
        "istanbul" to "استنبول", "ankara" to "انقرہ", "karachi" to "کراچی", "lahore" to "لاہور",
        "islamabad" to "اسلام آباد", "delhi" to "دہلی", "mumbai" to "ممبئی", "hyderabad" to "حیدرآباد",
        "dhaka" to "ڈھاکہ", "chittagong" to "چٹاگانگ", "kabul" to "کابل", "tehran" to "تہران", "mashhad" to "مشہد",
        "tashkent" to "تاشقند", "baku" to "باکو", "almaty" to "الماتی", "moscow" to "ماسکو", "kazan" to "قازان",
        "grozny" to "گروزنی", "kualalumpur" to "کوالالمپور", "penang" to "پینانگ", "singapore" to "سنگاپور",
        "brunei" to "بندر سری بیگاوان", "jakarta" to "جکارتہ", "surabaya" to "سورابایا", "bandung" to "بانڈونگ",
        "medan" to "میدان", "makassar" to "ماکاسر", "lagos" to "لاگوس", "kano" to "کانو", "dakar" to "ڈاکار",
        "sarajevo" to "سرائیوو", "london" to "لندن", "birmingham" to "برمنگھم", "paris" to "پیرس",
        "marseille" to "مارسے", "lyon" to "لیون", "brussels" to "برسلز", "amsterdam" to "ایمسٹرڈیم",
        "berlin" to "برلن", "hamburg" to "ہیمبرگ", "cologne" to "کولون", "munich" to "میونخ",
        "vienna" to "ویانا", "stockholm" to "اسٹاک ہوم", "newyork" to "نیویارک", "chicago" to "شکاگو",
        "losangeles" to "لاس اینجلس", "toronto" to "ٹورنٹو", "montreal" to "مونٹریال", "sydney" to "سڈنی",
    )
    override val palestine = "فلسطین"

    // ---------- اطلاعات ----------
    override val channelName = "اوقات نماز"
    override val channelDesc = "ہر نماز کا وقت شروع ہونے پر اطلاع"
    override fun prayerNowTitle(prayer: String) = "نماز $prayer کا وقت ہو گیا"
    override fun prayerNowText(city: String) = "$city — حی علی الصلوٰۃ"

    // ---------- قبلہ ----------
    override val qiblaTitle = "سمت قبلہ"
    override val noCompass = "آپ کے آلے میں قطب نما نہیں ہے۔ نیچے دیے گئے زاویے کے مطابق اس کا رخ خود درست کریں۔"
    override val facingQibla = "✓ آپ کا رخ قبلہ کی طرف ہے"
    override fun turnRight(deg: Int) = "فون کو $deg° دائیں گھمائیں"
    override fun turnLeft(deg: Int) = "فون کو $deg° بائیں گھمائیں"
    override val qiblaAngle = "حقیقی شمال سے قبلہ کا زاویہ"
    override fun towards(dir: String) = "$dir کی طرف"
    override val directions = listOf("شمال", "شمال مشرق", "مشرق", "جنوب مشرق", "جنوب", "جنوب مغرب", "مغرب", "شمال مغرب")
    override val location = "مقام"
    override val distanceToKaaba = "کعبہ سے فاصلہ"
    override val phoneHeading = "فون کا رخ"
    override val compassLowAccuracy = "قطب نما کی درستی کم ہے: اسے کیلیبریٹ کرنے کے لیے فون کو 8 کی شکل میں گھمائیں، اور دھات اور برقی آلات سے دور رہیں۔"
    override val qiblaTip = "مقام بدلنے کے لیے اوقات نماز کا صفحہ استعمال کریں۔ درست ریڈنگ کے لیے فون کو سیدھا لٹا کر اور دھات سے دور رکھیں۔"

    // ---------- تسبیح ----------
    override val tasbihTitle = "ڈیجیٹل تسبیح"
    override val tasbihSubtitle = "گننے کے لیے دائرے کو چھوئیں"
    override val modeFree = "آزاد ذکر"
    override val modeAfterPrayer = "نماز کے بعد"
    override fun ofGoal(n: Int) = "$n میں سے"
    override val nextDhikr = "اگلے ذکر کی طرف بڑھیں"
    override val completedHundred = "اللہ قبول فرمائے ✓ آپ نے سو مکمل کر لیے"
    override fun completedN(n: Int) = "$n مکمل ✓"
    override val reset = "ری سیٹ"
    override val undo = "کالعدم"
    override val chooseDhikr = "ذکر منتخب کریں"
    override val targetCount = "ہدف کی تعداد"
    override val yourStats = "آپ کے اعداد و شمار"
    override val roundsDone = "مکمل چکر"
    override val todayCount = "آج"
    override val totalCount = "مجموعی"
    override val dhikrLabels = listOf(
        "سبحان اللہ", "الحمد للہ", "اللہ اکبر", "لا الٰہ الا اللہ", "استغفر اللہ",
        "سبحان اللہ وبحمدہ", "سبحان اللہ العظیم", "لا حول ولا قوۃ الا باللہ", "نبی ﷺ پر درود",
    )
    override val dhikrMeanings = listOf(
        "اللہ پاک ہے",
        "تمام تعریفیں اللہ کے لیے ہیں",
        "اللہ سب سے بڑا ہے",
        "اللہ کے سوا کوئی معبود نہیں",
        "میں اللہ سے بخشش مانگتا ہوں",
        "اللہ پاک ہے اور اسی کی حمد ہے",
        "اللہ پاک ہے، جو عظمت والا ہے",
        "اللہ کی مدد کے بغیر نہ کوئی طاقت ہے نہ قوت",
        "اے اللہ! محمد ﷺ پر رحمتیں نازل فرما",
    )
    override val tamamMeaning = "اللہ کے سوا کوئی معبود نہیں، وہ اکیلا ہے، اس کا کوئی شریک نہیں۔ اسی کی بادشاہی ہے اور اسی کے لیے سب تعریف ہے، اور وہ ہر چیز پر قادر ہے۔"
    override val tapToCount = "گننے کے لیے چھوئیں"

    // ---------- زکوٰۃ ----------
    override val zakatTitle = "زکوٰۃ کیلکولیٹر"
    override val zakatSubtitle = "اس مال کا 2.5% جو نصاب کو پہنچے اور اس پر قمری سال گزر جائے"
    override val partCash = "نقدی اور بیلنس"
    override val partReceivables = "قابل وصول قرضے"
    override val partGold = "سونا"
    override val partSilver = "چاندی"
    override val partStocks = "شیئرز"
    override val partTrade = "مال تجارت"
    override val enterGoldPrice = "نصاب معلوم کرنے کے لیے سونے کی قیمت درج کریں"
    override val enterSilverPrice = "نصاب معلوم کرنے کے لیے چاندی کی قیمت درج کریں"
    override val nisabReached = "✓ آپ کا مال نصاب کو پہنچ گیا — زکوٰۃ واجب ہے"
    override val belowNisab = "نصاب سے کم — زکوٰۃ واجب نہیں"
    override val enterAssets = "اپنے اثاثے درج کریں"
    override val zakatDue = "واجب الادا زکوٰۃ"
    override val totalAssets = "کل قابل زکوٰۃ اثاثے"
    override val deductedDebts = "منہا کیے گئے قرضے"
    override val netBase = "خالص قابل زکوٰۃ مال"
    override val nisabValue = "نصاب کی مالیت"
    override val approxEquals = "تقریباً برابر"
    override fun goldGrams24(g: String) = "$g گرام 24 قیراط سونا"
    override val shareResult = "نتیجہ شیئر کریں"
    override fun zakatShareText(zakat: String, base: String) = "میری واجب الادا زکوٰۃ: $zakat (قابل زکوٰۃ مال $base)"
    override val settingsAndPrices = "ترتیبات اور قیمتیں"
    override val currency = "کرنسی"
    override val nisabBasis = "نقدی کے لیے نصاب کا معیار"
    override val nisabGold = "سونا (85 گرام)"
    override val nisabSilver = "چاندی (595 گرام)"
    override val goldPrice24 = "24 قیراط سونے کی فی گرام قیمت"
    override val goldPriceHint = "کسی سنار یا اپنے ملک کی معتبر ریٹ ویب سائٹ سے"
    override val silverPrice = "چاندی کی فی گرام قیمت (اختیاری)"
    override val cashSection = "نقدی اور بیلنس"
    override val cashInHand = "ہاتھ میں نقدی"
    override val bankBalances = "بینک اور والیٹ بیلنس"
    override val receivablesField = "آپ کے دیے ہوئے قرضے جن کی واپسی متوقع ہے"
    override val otherMoney = "دیگر رقوم"
    override val goldSilverSection = "سونا اور چاندی (گرام میں)"
    override fun goldKarat(k: Int) = "$k قیراط سونا"
    override val pureSilver = "خالص چاندی"
    override val includeJewelry = "زیر استعمال زیورات بھی شامل کریں (احناف کا قول، اور زیادہ محتاط)"
    override val jewelryField = "ان میں سے زیر استعمال زیورات (21 قیراط کے مساوی)"
    override val jewelryHint = "جمہور کے نزدیک ذاتی استعمال کے زیورات پر زکوٰۃ نہیں، اس لیے انہیں نکال دیا جاتا ہے"
    override val investSection = "سرمایہ کاری اور تجارت"
    override val stocksTrade = "خرید و فروخت کے لیے شیئرز (مارکیٹ ویلیو)"
    override val stocksLong = "طویل مدتی شیئرز (قابل زکوٰۃ حصہ)"
    override val tradeGoods = "مال تجارت (اسٹاک کی مالیت)"
    override val deductSection = "کٹوتیاں"
    override val debtsField = "آپ پر واجب قرضے جو اس سال ادا کرنے ہیں"
    override val debtsHint = "صرف زکوٰۃ کے سال میں واجب الادا قسطیں"
    override val breakdownTitle = "مال کی قسم کے لحاظ سے زکوٰۃ"
    override val zakatNote = "آپ کا ڈیٹا صرف آپ کے آلے پر رہتا ہے۔ اللہ تعالیٰ نے سورۃ التوبہ (9:60) میں زکوٰۃ کے آٹھ مصارف بیان فرمائے ہیں۔"

    // ---------- صدقۂ فطر ----------
    override val fitrTitle = "صدقۂ فطر"
    override val fitrSubtitle = "نماز عید سے پہلے ادا کیا جاتا ہے"
    override val foodAmountDue = "ادا کی جانے والی غذا کی مقدار"
    override val totalCashValue = "کل نقد مالیت"
    override fun forPeople(n: Int) = if (n == 1) "1 فرد کی طرف سے" else "$n افراد کی طرف سے"
    override val peopleCount = "جن کی طرف سے ادا کر رہے ہیں (اپنے سمیت)"
    override val modeFood = "غذا (صاع)"
    override val modeMoney = "نقد قیمت"
    override val foodType = "غذا کی قسم"
    override val foods = mapOf(
        "rice" to "چاول", "riceSafe" to "چاول (زیادہ محتاط)", "wheat" to "گندم",
        "dates" to "کھجور", "barley" to "جَو", "other" to "دوسری مقدار…",
    )
    override fun approxKg(kg: String) = "تقریباً $kg کلو"
    override val saWeight = "ایک صاع کا وزن (کلو میں)"
    override val kgPrice = "فی کلو قیمت (اختیاری)"
    override val kgPriceHint = "غذا کی تخمینی لاگت جاننے کے لیے"
    override val saPerPerson = "فی کس ایک صاع"
    override val approxCost = "تخمینی لاگت"
    override val valuePerPerson = "فی کس صدقۂ فطر کی رقم"
    override val valuePerPersonHint = "ہر سال آپ کے ملک کا سرکاری ادارہ اعلان کرتا ہے"
    override val valueNote = "قیمت ادا کرنا احناف کا قول ہے؛ جمہور غذا دیتے ہیں۔"
    override val fitrTimingNote = "ادائیگی کا وقت: نماز عید سے پہلے۔ ایک دو دن پہلے بھی دیا جا سکتا ہے، لیکن بغیر عذر نماز کے بعد تک مؤخر کرنا جائز نہیں۔ یہ فقراء اور مساکین کو دیا جاتا ہے۔"

    // ---------- فدیہ و کفارہ ----------
    override val fidyaTitle = "فدیہ و کفارہ"
    override val fidyaSubtitle = "کھانا کھلانے کی مقدار، کلو اور قیمت میں"
    override val tabFidya = "روزے کا فدیہ"
    override val tabOath = "قسم"
    override val tabIntercourse = "جماع"
    override val fidyaResult = "چھوٹے روزوں کا فدیہ"
    override fun fidyaSub(days: Int) = (if (days == 1) "1 دن" else "$days دنوں") + " کا — ہر دن کے بدلے ایک مسکین"
    override val fidyaNote = "اس شخص کے لیے جو مستقل طور پر روزہ رکھنے سے عاجز ہو، جیسے بہت بوڑھا یا ایسا مریض جس کی شفا کی امید نہ ہو۔ تمام دنوں کا کھانا ایک مسکین کو بھی دیا جا سکتا ہے اور کئی مساکین کو بھی۔"
    override val oathResult = "قسم کا کفارہ"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths کفارے × 10 مساکین" else "10 مساکین کو کھانا کھلانا"
    override val oathAlternative = "استطاعت نہ ہو تو"
    override fun fastDays(n: Int) = if (n == 1) "1 دن کا روزہ" else "$n دن کے روزے"
    override val oathNote = "دس مساکین کو کھانا کھلانا، یا انہیں کپڑے پہنانا، یا ایک غلام آزاد کرنا؛ جو اس کی استطاعت نہ رکھے وہ تین دن روزے رکھے (سورۃ المائدہ 5:89)۔"
    override val intercourseResult = "رمضان کے دن میں جماع کا کفارہ"
    override val intercourseSub = "روزوں کی طاقت نہ ہو تو 60 مساکین کو کھانا"
    override val ifStartToday = "اگر آج روزے شروع کریں تو اختتام ہوگا"
    override val twoMonthsDuration = "دو قمری مہینوں کی مدت"
    override val intercourseNote = "ترتیب وار: غلام آزاد کرنا؛ ممکن نہ ہو تو مسلسل دو مہینے روزے رکھنا؛ اس کی طاقت نہ ہو تو ساٹھ مساکین کو کھانا کھلانا — ساتھ ہی اس دن کی قضا اور توبہ۔"
    override val daysField = "دنوں کی تعداد"
    override val oathsField = "کفاروں کی تعداد"
    override val feedMeasure = "فی مسکین غذا کی مقدار"
    override val muddOption = "ایک مُد — تقریباً 0.75 کلو (مالکی و شافعی)"
    override val halfSaOption = "نصف صاع — تقریباً 1.5 کلو (معاصر فتاویٰ)"
    override val poorCount = "مساکین / کھانوں کی تعداد"
    override val approxValue = "تخمینی قیمت"

    // ---------- ہجری تاریخ ----------
    override val hijriTitle = "ہجری تاریخ"
    override val hijriSubtitle = "ام القریٰ کیلنڈر"
    override val correspondingHijri = "مطابق ہجری تاریخ"
    override val correspondingGreg = "مطابق عیسوی تاریخ"
    override val enterGregYear = "1882 اور 2174 کے درمیان سال درج کریں"
    override val enterHijriYear = "1300 اور 1600 کے درمیان ہجری سال درج کریں"
    override val gregToHijri = "عیسوی ← ہجری"
    override val hijriToGreg = "ہجری ← عیسوی"
    override val todayDate = "آج کی تاریخ"
    override val prevMonth = "پچھلا مہینہ"
    override val nextMonth = "اگلا مہینہ"
    override val calendarHint = "بڑا عدد ہجری ہے اور چھوٹا عیسوی۔ کسی دن کو تبدیل کرنے کے لیے اسے چھوئیں۔"
    override val moonNote = "رؤیت ہلال کے فرق کی وجہ سے بعض ممالک میں تاریخ ایک دن مختلف ہو سکتی ہے۔"

    // ---------- عمر ----------
    override val ageTitle = "عمر کیلکولیٹر"
    override val ageSubtitle = "ہجری اور عیسوی سالوں میں"
    override val ageGregorian = "آپ کی عمر (عیسوی)"
    override fun ymd(y: Int, m: Int, d: Int) = "${ur(y.toLong(), "سال", "سال")} ${ur(m.toLong(), "مہینہ", "مہینے")} ${ur(d.toLong(), "دن", "دن")}"
    override fun inHijri(age: String) = "ہجری: $age"
    override val birthGreg = "تاریخ پیدائش (عیسوی)"
    override val birthHijri = "تاریخ پیدائش (ہجری)"
    override val birthWeekday = "پیدائش کا دن"
    override val nextBirthday = "اگلی سالگرہ"
    override val birthdayToday = "آج 🎉"
    override val tileDays = "دن"
    override val tileWeeks = "ہفتے"
    override val tileHours = "گھنٹے"
    override val yourAge = "آپ کی عمر"
    override val birthAfterToday = "تاریخ پیدائش آج کے بعد کی ہے"
    override val enterBirth = "اپنی تاریخ پیدائش درج کریں"
    override val birthDate = "تاریخ پیدائش"
    override val gregorian = "عیسوی"
    override val hijri = "ہجری"

    // ---------- اسلامی ایام ----------
    override val eventsTitle = "اسلامی ایام"
    override val eventsSubtitle = "ام القریٰ کیلنڈر کے مطابق الٹی گنتی"
    override val events = mapOf(
        "ramadan" to Named("رمضان المبارک", "روزوں، قیام اللیل اور تلاوت قرآن کا مہینہ۔"),
        "lastTen" to Named("آخری عشرہ", "اسی میں شب قدر ہے جو ہزار مہینوں سے بہتر ہے۔"),
        "eidFitr" to Named("عید الفطر", "اس کی نماز سے پہلے صدقۂ فطر ادا کیا جاتا ہے۔"),
        "shawwal" to Named("شوال کے چھ روزے", "جس نے رمضان کے روزے رکھے پھر اس کے بعد شوال کے چھ روزے رکھے، گویا اس نے عمر بھر روزے رکھے (مسلم)۔"),
        "dhulHijjah" to Named("عشرۂ ذوالحجہ", "دنیا کے افضل ترین دن؛ ان میں نیک اعمال اور تکبیر مستحب ہیں۔"),
        "arafah" to Named("یوم عرفہ", "غیر حاجی کے لیے اس کا روزہ گزشتہ اور آئندہ سال کا کفارہ ہے (مسلم)۔"),
        "eidAdha" to Named("عید الاضحیٰ", "قربانی کا دن، جس کے بعد ایام تشریق کے تین دن ہیں۔"),
        "newYear" to Named("اسلامی سال نو", "نئے ہجری سال کا آغاز۔"),
        "ashura" to Named("یوم عاشورہ", "اس کا روزہ گزشتہ سال کا کفارہ ہے (مسلم)؛ اس کے ساتھ 9 محرم کا روزہ رکھنا مستحب ہے۔"),
    )
    override val unitDays = "دن"
    override val unitHours = "گھنٹے"
    override val unitMinutes = "منٹ"
    override val unitSeconds = "سیکنڈ"
    override val eventsNote = "تاریخیں تخمینی ہیں (ام القریٰ) اور آپ کے ملک میں رؤیت ہلال کے مطابق ایک دن مختلف ہو سکتی ہیں۔"

    // ---------- وراثت ----------
    override val inhTitle = "وراثت کیلکولیٹر"
    override val inhSubtitle = "علم فرائض کے مطابق ترکے کی تقسیم"
    override val aslLabel = "اصل مسئلہ"
    override val awlCase = "مسئلۂ عول (حصے ترکے سے بڑھ گئے)"
    override val raddCase = "مسئلۂ رد (زائد ترکہ واپس)"
    override val adilCase = "مسئلۂ عادلہ"
    override fun tashihFrom(n: Long) = "تصحیح: " + ur(n, "حصہ", "حصے")
    override val sharesTitle = "ہر وارث کا حصہ"
    override fun blockedBy(by: String) = "محجوب (حاجب: $by)"
    override fun perPerson(v: String) = "فی کس $v"
    override val shareDivision = "تقسیم شیئر کریں"
    override fun divisionShareText(list: String) = "تقسیم ترکہ: $list"
    override val listSeparator = "، "
    override val addHeirs = "نیچے دی گئی فہرست سے ورثاء شامل کریں"
    override val deceasedAndEstate = "میت اور ترکہ"
    override val deceasedMale = "میت مرد ہے"
    override val deceasedFemale = "میت عورت ہے"
    override val estateTotal = "کل ترکہ"
    override val funeralCosts = "تجہیز و تکفین کے اخراجات"
    override val deceasedDebts = "میت کے قرضے"
    override val bequest = "وصیت (غیر وارث کے لیے)"
    override val bequestHint = "قرضوں کے بعد باقی کے ایک تہائی سے زیادہ نہیں، الا یہ کہ ورثاء رضامند ہوں"
    override val netEstate = "قابل تقسیم خالص ترکہ"
    override val bequestCapped = "(وصیت ایک تہائی تک محدود)"
    override val newCase = "نیا مسئلہ شروع کریں"
    override val inhDisclaimer = "نتیجہ جمہور کے قول پر مبنی رہنمائی ہے۔ یہ شرعی عدالت یا علم فرائض کے ماہر عالم کا متبادل نہیں۔"
    override val heirs = mapOf(
        "husband" to "شوہر", "wife" to "بیوی", "father" to "باپ", "mother" to "ماں",
        "gf" to "دادا", "gmm" to "نانی", "gmf" to "دادی",
        "son" to "بیٹا", "daughter" to "بیٹی", "gson" to "پوتا", "gdau" to "پوتی",
        "fb" to "حقیقی بھائی", "fs" to "حقیقی بہن", "pb" to "باپ شریک بھائی", "ps" to "باپ شریک بہن",
        "mb" to "ماں شریک بھائی", "ms" to "ماں شریک بہن", "fnep" to "حقیقی بھتیجا", "pnep" to "باپ شریک بھائی کا بیٹا",
        "fun" to "حقیقی چچا", "pun" to "باپ شریک چچا", "fcous" to "حقیقی چچا کا بیٹا", "pcous" to "باپ شریک چچا کا بیٹا",
    )
    override val heirGroups = mapOf(
        "spouses" to "زوجین", "ascendants" to "والدین اور دادا دادی", "descendants" to "اولاد اور پوتے پوتیاں",
        "siblings" to "بہن بھائی", "distant" to "دور کے مرد رشتہ دار",
    )
    override val blockers = mapOf(
        "gf" to "دادا",
        "twoDaughters" to "دو بیٹیاں (دو تہائی مکمل)",
        "descendant" to "وارث اولاد",
        "fbAfterMuadda" to "حقیقی بھائی (معادّہ کے بعد)",
        "fsTookRest" to "باقی ترکہ لینے والی حقیقی بہن",
        "gfAndSiblings" to "دادا اور بہن بھائی",
        "twoFullSisters" to "دو حقیقی بہنیں",
        "fsGhayr" to "حقیقی بہن (بیٹیوں کے ساتھ عصبہ)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "مقررہ حصہ"
        ShareType.FARD_TASIB -> "مقررہ حصہ + عصبہ"
        ShareType.FARD_TAKMILA -> "مقررہ حصہ (دو تہائی کی تکمیل)"
        ShareType.TASIB -> "عصبہ"
        ShareType.TASIB_BIL_GHAYR -> "عصبہ بالغیر"
        ShareType.TASIB_MAA_GHAYR -> "عصبہ مع الغیر"
        ShareType.TASIB_MAA_JADD -> "دادا کے ساتھ عصبہ"
        ShareType.FARD_THEN_MUQASAMA -> "مقررہ حصہ، پھر مقاسمہ"
        ShareType.JADD_MUQASAMA -> "بہن بھائیوں کے ساتھ: مقاسمہ"
        ShareType.JADD_THIRD_ESTATE -> "بہن بھائیوں کے ساتھ: کل ترکے کا تہائی"
        ShareType.JADD_THIRD_REST -> "بہن بھائیوں کے ساتھ: باقی کا تہائی"
        ShareType.JADD_SIXTH -> "بہن بھائیوں کے ساتھ: کل ترکے کا چھٹا حصہ"
    } + if (radd) " + رد" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "ماں شریک بہن بھائی ایک تہائی میں برابر کے شریک ہوتے ہیں، مرد اور عورت یکساں۔"
        InhNote.Umariyya -> "یہ دو عمریہ مسائل (غرّاوین) میں سے ایک ہے: ماں کو شوہر یا بیوی کے حصے کے بعد باقی کا ایک تہائی ملتا ہے۔"
        InhNote.GrandmothersShare -> "دادی اور نانی چھٹے حصے میں شریک ہیں۔"
        InhNote.FullSistersGhayr -> "حقیقی بہنیں بیٹیوں کے ساتھ عصبہ مع الغیر بن جاتی ہیں۔"
        InhNote.Akdariyya -> "یہ مسئلۂ اکدریہ ہے: بہن کو نصف اور دادا کو چھٹا حصہ دیا جاتا ہے، جس سے مسئلہ 9 تک عول کرتا ہے؛ پھر دونوں کے حصے جمع کر کے مرد کو عورت سے دگنا کے حساب سے تقسیم کیے جاتے ہیں، اور تصحیح 27 سے ہوتی ہے۔"
        is InhNote.JaddRule -> "بہن بھائیوں کے ساتھ دادا وہ لیتا ہے جو اس کے لیے زیادہ بہتر ہو: مقاسمہ، یا " +
            (if (n.otherFard) "باقی کا تہائی، یا کل ترکے کا چھٹا حصہ" else "کل ترکے کا تہائی") +
            " — یہ جمہور (مالکیہ، شافعیہ، حنابلہ) کا قول ہے۔ امام ابو حنیفہ کے نزدیک دادا باپ کی طرح بہن بھائیوں کو محجوب کر دیتا ہے۔"
        InhNote.Muadda -> "باپ شریک بہن بھائیوں کو دادا کے مقابلے میں شمار کیا گیا (معادّہ)، پھر ضرورت کے مطابق ان کا حصہ حقیقی بہن بھائیوں کو لوٹا دیا جاتا ہے۔"
        is InhNote.NothingLeft -> "ذوی الفروض کے بعد کچھ باقی نہیں بچا، اس لیے عصبہ (${nameOf(n.winner)}) کو کچھ نہیں ملتا۔"
        InhNote.Mushtaraka -> "یہ مسئلۂ مشترکہ (حماریہ) ہے: مالکیہ اور شافعیہ حقیقی بہن بھائیوں کو ماں شریک بہن بھائیوں کے ساتھ تہائی میں شریک کرتے ہیں؛ یہاں نتیجہ احناف اور حنابلہ کے قول کے مطابق ہے۔"
        is InhNote.Awl -> "مقررہ حصے اصل مسئلہ سے بڑھ گئے، اس لیے مسئلہ ${n.asl} سے ${n.awl} تک عول کیا گیا اور ہر حصہ اسی تناسب سے کم ہو گیا۔"
        InhNote.Radd -> "کوئی عصبہ نہیں، اس لیے زائد ترکہ زوجین کے سوا ذوی الفروض پر ان کے حصوں کے تناسب سے رد کیا گیا۔"
        InhNote.SpouseOnlyRadd -> "شوہر/بیوی کے سوا کوئی وارث نہیں: بہت سے معاصر علماء اور کئی عائلی قوانین باقی ترکہ انہی کو لوٹاتے ہیں؛ متقدمین کے نزدیک یہ ذوی الارحام کو، پھر بیت المال کو جاتا ہے۔"
        InhNote.NoHeirs -> "نہ کوئی ذوی الفروض ہے نہ عصبہ؛ ترکہ ذوی الارحام کو، پھر بیت المال کو جاتا ہے۔ متعلقہ عدالت سے رجوع کریں۔"
    }

    // ---------- ترتیبات ----------
    override val settingsTitle = "ترتیبات"
    override val appearance = "ظاہری شکل"
    override val themeAuto = "خودکار"
    override val themeLight = "روشن"
    override val themeDark = "تاریک"
    override val language = "زبان"
    override val deviceLanguage = "آلے کی زبان"
    override val use24h = "24 گھنٹے کی گھڑی"
    override val use24hDesc = "اوقات 24 گھنٹے کے فارمیٹ میں دکھائیں"
    override val exactAlarm = "عین وقت پر اطلاع"
    override val exactAlarmDesc = "عین وقت کے الارم کی اجازت دیں تاکہ اذان کی اطلاع بروقت آئے"
    override val about = "ایپ کے بارے میں"
    override val aboutName = "میزان — مسلمانوں کے لیے ٹولز"
    override fun version(v: String) = "ورژن $v"
    override val aboutText = "ایک مفت ایپ جس میں وہ ٹولز ہیں جن کی ایک مسلمان کو روزانہ ضرورت ہوتی ہے: زکوٰۃ، وراثت، اوقات نماز، قبلہ، ہجری تاریخ اور تسبیح۔ " +
        "احکام جمہور فقہاء کے مطابق ہیں، اوقات نماز فلکیاتی فارمولوں سے نکالے جاتے ہیں، اور تاریخیں ام القریٰ کیلنڈر کے مطابق ہیں۔"
    override val privacy = "رازداری"
    override val privacyText = "ایپ کوئی ذاتی ڈیٹا جمع نہیں کرتی اور اسے انٹرنیٹ کی ضرورت نہیں۔ آپ کا مقام صرف آپ کے آلے پر اوقات نماز اور قبلہ معلوم کرنے کے لیے استعمال ہوتا ہے، " +
        "اور آپ کی ترتیبات اور اعداد آپ کے آلے ہی پر رہتے ہیں۔"
    override val disclaimer = "نتائج رہنمائی کے لیے ہیں۔ اپنے ذاتی شرعی مسائل میں مستند علماء اور سرکاری اداروں سے رجوع کریں۔"
}
