import Foundation

// مولَّد من widget/LiveText.kt و widget/WidgetText.kt و platform/LiveTasbih.kt (Fixtures/content/texts.json)؛ لا يُعدَّل يدوياً

/// نصوص الأنشطة الحية وأدوات iOS الجديدة بكل لغات التطبيق، مستقلة عن نصوص الواجهة
public struct LiveText: Hashable {
    public let end: String  // زر إنهاء النشاط الحي
    public let iqamaIn: String  // «الإقامة بعد» فوق العدّ إلى الإقامة
    public let iftar: String  // رمضان: عنوان العدّ إلى المغرب
    public let suhoor: String  // رمضان: عنوان العدّ إلى الفجر
    public let iftarNow: String  // رمضان: عند المغرب
    public let adhkarDone: String  // «أتممت %s»
    public let tasbih: String  // اسم أداة المسبحة في المعرض
    public let descTasbih: String
    public let wird: String  // اسم أداة الورد في المعرض
    public let descWird: String
    public let showLive: String  // زر في شاشة المسبحة: يُظهر العدّاد على شاشة القفل
    public let again: String  // بعد إتمام الأذكار: من أولها

    public func adhkarDone(_ section: String) -> String { adhkarDone.replacingOccurrences(of: "%s", with: section) }

    /// دعاء الإفطار، بالعربية في كل اللغات كبقية الأذكار
    public static let iftarDua = "ذَهَبَ الظَّمَأُ، وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ"

    public static func of(_ code: String) -> LiveText { all[code] ?? en }

    public static let ar = LiveText(end: "إنهاء", iqamaIn: "الإقامة بعد", iftar: "موعد الإفطار", suhoor: "نهاية السحور", iftarNow: "حان وقت الإفطار", adhkarDone: "أتممت %s ✓", tasbih: "المسبحة", descTasbih: "سبّح بلمسة من الشاشة الرئيسية أو شاشة القفل", wird: "ورد اليوم", descWird: "صفحات وردك اليوم وتقدّم ختمتك", showLive: "على شاشة القفل", again: "من جديد")
    public static let en = LiveText(end: "End", iqamaIn: "Iqama in", iftar: "Iftar", suhoor: "Suhoor ends", iftarNow: "Time to break the fast", adhkarDone: "%s completed ✓", tasbih: "Tasbih", descTasbih: "Count with a tap from the Home Screen or Lock Screen", wird: "Today's portion", descWird: "Today's Quran pages and your khatma progress", showLive: "On Lock Screen", again: "Start again")
    public static let fr = LiveText(end: "Terminer", iqamaIn: "Iqama dans", iftar: "Iftar", suhoor: "Fin du sahour", iftarNow: "C’est l’heure de rompre le jeûne", adhkarDone: "%s terminés ✓", tasbih: "Tasbih", descTasbih: "Comptez d’un geste depuis l’écran d’accueil ou l’écran verrouillé", wird: "Portion du jour", descWird: "Les pages du jour et l’avancée de votre khatma", showLive: "Sur l’écran verrouillé", again: "Recommencer")
    public static let tr = LiveText(end: "Bitir", iqamaIn: "Kamete kalan", iftar: "İftar", suhoor: "Sahur bitişi", iftarNow: "İftar vakti", adhkarDone: "%s tamamlandı ✓", tasbih: "Tesbih", descTasbih: "Ana ekrandan veya kilit ekranından tek dokunuşla sayın", wird: "Günün virdi", descWird: "Bugünkü sayfalar ve hatim ilerlemeniz", showLive: "Kilit ekranında", again: "Baştan")
    public static let ur = LiveText(end: "ختم کریں", iqamaIn: "اقامت میں", iftar: "افطار", suhoor: "سحری کا اختتام", iftarNow: "افطار کا وقت ہو گیا", adhkarDone: "%s مکمل ✓", tasbih: "تسبیح", descTasbih: "ہوم اسکرین یا لاک اسکرین سے ایک ٹیپ میں گنیں", wird: "آج کا ورد", descWird: "آج کے صفحات اور ختم کی پیش رفت", showLive: "لاک اسکرین پر", again: "دوبارہ شروع")
    public static let id = LiveText(end: "Selesai", iqamaIn: "Iqamah dalam", iftar: "Berbuka", suhoor: "Sahur berakhir", iftarNow: "Waktunya berbuka puasa", adhkarDone: "%s selesai ✓", tasbih: "Tasbih", descTasbih: "Hitung dengan sekali ketuk dari Layar Utama atau Layar Kunci", wird: "Wirid hari ini", descWird: "Halaman hari ini dan kemajuan khatam Anda", showLive: "Di Layar Kunci", again: "Ulangi")
    public static let ms = LiveText(end: "Tamat", iqamaIn: "Iqamah dalam", iftar: "Berbuka", suhoor: "Sahur berakhir", iftarNow: "Waktu berbuka puasa", adhkarDone: "%s selesai ✓", tasbih: "Tasbih", descTasbih: "Kira dengan satu ketikan dari Skrin Utama atau Skrin Kunci", wird: "Wirid hari ini", descWird: "Halaman hari ini dan kemajuan khatam anda", showLive: "Di Skrin Kunci", again: "Mula semula")
    public static let fa = LiveText(end: "پایان", iqamaIn: "اقامه تا", iftar: "افطار", suhoor: "پایان سحر", iftarNow: "وقت افطار است", adhkarDone: "%s تمام شد ✓", tasbih: "تسبیح", descTasbih: "با یک ضربه از صفحهٔ اصلی یا صفحهٔ قفل بشمارید", wird: "ورد امروز", descWird: "صفحه‌های امروز و پیشرفت ختم شما", showLive: "روی صفحهٔ قفل", again: "از نو")
    public static let bn = LiveText(end: "শেষ", iqamaIn: "ইকামত বাকি", iftar: "ইফতার", suhoor: "সাহরির শেষ", iftarNow: "ইফতারের সময় হয়েছে", adhkarDone: "%s সম্পন্ন ✓", tasbih: "তাসবিহ", descTasbih: "হোম স্ক্রিন বা লক স্ক্রিন থেকে এক ট্যাপে গুনুন", wird: "আজকের অংশ", descWird: "আজকের পৃষ্ঠা ও খতমের অগ্রগতি", showLive: "লক স্ক্রিনে", again: "আবার শুরু")
    public static let ru = LiveText(end: "Завершить", iqamaIn: "Икамат через", iftar: "Ифтар", suhoor: "Конец сухура", iftarNow: "Время разговения", adhkarDone: "%s — выполнено ✓", tasbih: "Тасбих", descTasbih: "Считайте одним касанием с экрана «Домой» или экрана блокировки", wird: "Сегодняшняя часть", descWird: "Страницы на сегодня и прогресс хатма", showLive: "На экране блокировки", again: "Заново")
    public static let de = LiveText(end: "Beenden", iqamaIn: "Iqama in", iftar: "Iftar", suhoor: "Ende des Suhur", iftarNow: "Zeit zum Fastenbrechen", adhkarDone: "%s abgeschlossen ✓", tasbih: "Tasbih", descTasbih: "Mit einem Tippen vom Home- oder Sperrbildschirm zählen", wird: "Heutiger Abschnitt", descWird: "Heutige Seiten und Fortschritt deiner Khatma", showLive: "Auf dem Sperrbildschirm", again: "Von vorn")
    public static let es = LiveText(end: "Terminar", iqamaIn: "Iqama en", iftar: "Iftar", suhoor: "Fin del suhur", iftarNow: "Hora de romper el ayuno", adhkarDone: "%s completados ✓", tasbih: "Tasbih", descTasbih: "Cuenta con un toque desde la pantalla de inicio o la de bloqueo", wird: "Porción de hoy", descWird: "Las páginas de hoy y el avance de tu jatma", showLive: "En la pantalla bloqueada", again: "De nuevo")
    public static let uk = LiveText(end: "Завершити", iqamaIn: "Ікамат через", iftar: "Іфтар", suhoor: "Кінець сухуру", iftarNow: "Час розговіння", adhkarDone: "%s — виконано ✓", tasbih: "Тасбіх", descTasbih: "Рахуйте одним дотиком з початкового екрана або екрана блокування", wird: "Сьогоднішня частина", descWird: "Сторінки на сьогодні й поступ хатму", showLive: "На екрані блокування", again: "Знову")
    public static let uz = LiveText(end: "Tugatish", iqamaIn: "Iqomatgacha", iftar: "Iftor", suhoor: "Saharlik tugashi", iftarNow: "Iftor vaqti bo‘ldi", adhkarDone: "%s yakunlandi ✓", tasbih: "Tasbeh", descTasbih: "Asosiy ekran yoki qulf ekranidan bir bosishda sanang", wird: "Bugungi vird", descWird: "Bugungi sahifalar va xatm rivoji", showLive: "Qulf ekranida", again: "Qaytadan")
    public static let kk = LiveText(end: "Аяқтау", iqamaIn: "Қамәтке дейін", iftar: "Ауызашар", suhoor: "Сәресінің соңы", iftarNow: "Ауызашар уақыты болды", adhkarDone: "%s аяқталды ✓", tasbih: "Тәспі", descTasbih: "Басты экраннан не құлып экранынан бір түртумен санаңыз", wird: "Бүгінгі бөлік", descWird: "Бүгінгі беттер және хатым барысы", showLive: "Құлып экранында", again: "Қайтадан")

    public static let all: [String: LiveText] = ["ar": ar, "en": en, "fr": fr, "tr": tr, "ur": ur, "id": id, "ms": ms, "fa": fa, "bn": bn, "ru": ru, "de": de, "es": es, "uk": uk, "uz": uz, "kk": kk]
}

/// نصوص الأدوات وحدها بكل لغات التطبيق؛ kahf و salawat و hour: ترجمة أحاديث الجمعة الثلاثة التي تضيفها الأداة
public struct WidgetText: Hashable {
    public let adhkar: String
    public let nextPrayer: String
    public let prayerTimes: String
    public let hijri: String
    public let dhikr: String
    public let friday: String
    public let arafah: String
    public let iftar: String
    public let lastThird: String
    public let fridayEve: String
    public let lastTen: String
    public let until: String
    public let next: String
    public let inTime: String
    public let openApp: String
    public let kahfCta: String
    public let descNext: String
    public let descTimes: String
    public let descHijri: String
    public let descAyah: String
    public let descAdhkar: String
    public let kahf: String?
    public let salawat: String?
    public let hour: String?
    public let after: String
    public let allowLocation: String
    public let locationHint: String

    /// «حتى %s»
    public func untilTime(_ t: String) -> String { until.replacingOccurrences(of: "%s", with: t) }

    public static let kahfId = "fri_kahf"
    public static let salawatId = "fri_salawat"
    public static let hourId = "fri_hour"

    /// أحاديث الجمعة التي ليست في مكتبة الأذكار (نصها ومصدرها كما في بقية الأذكار)
    public static let extra: [String: Dhikr] = {
        let list: [Dhikr] = [
            Dhikr(id: "fri_kahf", ar: "«مَنْ قَرَأَ سُورَةَ الْكَهْفِ فِي يَوْمِ الْجُمُعَةِ أَضَاءَ لَهُ مِنَ النُّورِ مَا بَيْنَ الْجُمُعَتَيْنِ»", count: 1, ayah: [], src: [DhikrSource("hakim", "2/368"), DhikrSource("bayhaqi", "3/249")],
                  grade: "sahih", by: "albani", narr: "أبو سعيد الخدري رضي الله عنه", fadl: nil, note: nil),
            Dhikr(id: "fri_salawat", ar: "«إِنَّ مِنْ أَفْضَلِ أَيَّامِكُمْ يَوْمَ الْجُمُعَةِ … فَأَكْثِرُوا عَلَيَّ مِنَ الصَّلَاةِ فِيهِ، فَإِنَّ صَلَاتَكُمْ مَعْرُوضَةٌ عَلَيَّ»", count: 1, ayah: [], src: [DhikrSource("abudawud", "1047"), DhikrSource("nasai", "1374")],
                  grade: "sahih", by: "albani", narr: "أوس بن أوس رضي الله عنه", fadl: nil, note: nil),
            Dhikr(id: "fri_hour", ar: "«فِيهِ سَاعَةٌ لَا يُوَافِقُهَا عَبْدٌ مُسْلِمٌ وَهُوَ قَائِمٌ يُصَلِّي يَسْأَلُ اللَّهَ تَعَالَى شَيْئًا إِلَّا أَعْطَاهُ إِيَّاهُ»", count: 1, ayah: [], src: [DhikrSource("bukhari", "935"), DhikrSource("muslim", "852")],
                  grade: "muttafaq", by: "", narr: "أبو هريرة رضي الله عنه", fadl: nil, note: nil),
        ]
        var m: [String: Dhikr] = [:]
        for d in list { m[d.id] = d }
        return m
    }()

    public static func of(_ code: String) -> WidgetText { all[code] ?? en }

    public static let ar = WidgetText(
        adhkar: "أذكار الوقت",
        nextPrayer: "الصلاة القادمة",
        prayerTimes: "مواقيت الصلاة",
        hijri: "التاريخ الهجري",
        dhikr: "ذكر وتسبيح",
        friday: "سنن الجمعة",
        arafah: "يوم عرفة",
        iftar: "دعاء الإفطار",
        lastThird: "الثلث الأخير من الليل",
        fridayEve: "ليلة الجمعة",
        lastTen: "العشر الأواخر",
        until: "حتى %s",
        next: "التالي",
        inTime: "بعد",
        openApp: "افتح ميزان مرة لتظهر مواقيتك هنا",
        kahfCta: "اقرأ سورة الكهف اليوم",
        descNext: "الصلاة القادمة وعدّ تنازلي لها",
        descTimes: "مواقيت اليوم الست والقادمة مميزة",
        descHijri: "التاريخ الهجري والميلادي",
        descAyah: "آية قصيرة كل يوم مع معناها",
        descAdhkar: "الأذكار المناسبة للوقت: الصباح والمساء والنوم وبعد الصلاة والجمعة",
        kahf: nil,
        salawat: nil,
        hour: nil,
        after: "أذكار بعد الصلاة",
        allowLocation: "اسمح لأدوات ميزان بمعرفة موقعك لتظهر مواقيتك",
        locationHint: "الإعدادات ← ميزان ← الموقع: أثناء استخدام التطبيق أو الأدوات"
    )
    public static let en = WidgetText(
        adhkar: "Adhkar for now",
        nextPrayer: "Next prayer",
        prayerTimes: "Prayer times",
        hijri: "Hijri date",
        dhikr: "Dhikr & tasbih",
        friday: "Friday sunnahs",
        arafah: "Day of Arafah",
        iftar: "Breaking the fast",
        lastThird: "Last third of the night",
        fridayEve: "Friday eve",
        lastTen: "Last ten nights",
        until: "until %s",
        next: "Next",
        inTime: "in",
        openApp: "Open Mizan once to show your times here",
        kahfCta: "Read Surat al-Kahf today",
        descNext: "The next prayer with a live countdown",
        descTimes: "Today's six times with the next one highlighted",
        descHijri: "Hijri and Gregorian date",
        descAyah: "A short ayah every day with its meaning",
        descAdhkar: "The right adhkar for the time: morning, evening, sleep, after prayer and Friday",
        kahf: "Whoever recites Surat al-Kahf on Friday, a light will shine for him between the two Fridays.",
        salawat: "Among the best of your days is Friday… so send abundant blessings upon me on it, for your blessings are presented to me.",
        hour: "On Friday there is an hour in which no Muslim servant stands in prayer asking Allah for something but He gives it to him.",
        after: "After-prayer adhkar",
        allowLocation: "Allow location for Mizan widgets to show your times",
        locationHint: "Settings › Mizan › Location: While Using the App or Widgets"
    )
    public static let fr = WidgetText(
        adhkar: "Adhkar du moment",
        nextPrayer: "Prochaine prière",
        prayerTimes: "Horaires de prière",
        hijri: "Date hégirienne",
        dhikr: "Dhikr et tasbih",
        friday: "Sunnas du vendredi",
        arafah: "Jour d’Arafat",
        iftar: "Rupture du jeûne",
        lastThird: "Dernier tiers de la nuit",
        fridayEve: "Veille du vendredi",
        lastTen: "Dix dernières nuits",
        until: "jusqu’à %s",
        next: "Suivant",
        inTime: "dans",
        openApp: "Ouvrez Mizan une fois pour afficher vos horaires ici",
        kahfCta: "Lisez la sourate al-Kahf aujourd’hui",
        descNext: "La prochaine prière avec un compte à rebours",
        descTimes: "Les six horaires du jour, le prochain mis en avant",
        descHijri: "Date hégirienne et grégorienne",
        descAyah: "Un court verset chaque jour avec son sens",
        descAdhkar: "Les adhkar du moment : matin, soir, sommeil, après la prière et vendredi",
        kahf: "Celui qui récite la sourate al-Kahf le vendredi, une lumière brillera pour lui entre les deux vendredis.",
        salawat: "Parmi les meilleurs de vos jours est le vendredi… multipliez-y donc les prières sur moi, car vos prières me sont présentées.",
        hour: "Le vendredi, il est une heure où aucun serviteur musulman debout en prière ne demande quelque chose à Allah sans qu’Il le lui accorde.",
        after: "Adhkar après la prière",
        allowLocation: "Autorisez la localisation pour les widgets Mizan afin d’afficher vos horaires",
        locationHint: "Réglages › Mizan › Position : Lorsque l’app ou les widgets sont actifs"
    )
    public static let tr = WidgetText(
        adhkar: "Vaktin zikirleri",
        nextPrayer: "Sıradaki namaz",
        prayerTimes: "Namaz vakitleri",
        hijri: "Hicri tarih",
        dhikr: "Zikir ve tesbih",
        friday: "Cuma sünnetleri",
        arafah: "Arefe günü",
        iftar: "İftar duası",
        lastThird: "Gecenin son üçte biri",
        fridayEve: "Cuma gecesi",
        lastTen: "Son on gece",
        until: "%s vaktine kadar",
        next: "Sonraki",
        inTime: "kalan",
        openApp: "Vakitlerinizin burada görünmesi için Mizan’ı bir kez açın",
        kahfCta: "Bugün Kehf suresini okuyun",
        descNext: "Sıradaki namaz ve geri sayım",
        descTimes: "Günün altı vakti, sıradaki vurgulu",
        descHijri: "Hicri ve miladi tarih",
        descAyah: "Her gün anlamıyla kısa bir ayet",
        descAdhkar: "Vakte uygun zikirler: sabah, akşam, uyku, namaz sonrası ve cuma",
        kahf: "Kim cuma günü Kehf suresini okursa, iki cuma arası onun için nur ile aydınlanır.",
        salawat: "Günlerinizin en faziletlilerinden biri cuma günüdür… O gün bana çokça salavat getirin; salavatınız bana arz olunur.",
        hour: "Cuma gününde bir saat vardır ki, namazda duran Müslüman bir kul o saatte Allah’tan bir şey isterse, Allah onu mutlaka verir.",
        after: "Namaz sonrası zikirler",
        allowLocation: "Vakitlerinizi göstermek için Mizan widget’larına konum izni verin",
        locationHint: "Ayarlar › Mizan › Konum: Uygulamayı veya Widget’ları Kullanırken"
    )
    public static let ur = WidgetText(
        adhkar: "وقت کے اذکار",
        nextPrayer: "اگلی نماز",
        prayerTimes: "اوقاتِ نماز",
        hijri: "ہجری تاریخ",
        dhikr: "ذکر و تسبیح",
        friday: "جمعہ کی سنتیں",
        arafah: "یومِ عرفہ",
        iftar: "افطار کی دعا",
        lastThird: "رات کا آخری تہائی",
        fridayEve: "شبِ جمعہ",
        lastTen: "آخری عشرہ",
        until: "%s تک",
        next: "اگلا",
        inTime: "باقی",
        openApp: "اپنے اوقات یہاں دیکھنے کے لیے میزان ایک بار کھولیں",
        kahfCta: "آج سورۂ کہف پڑھیں",
        descNext: "اگلی نماز اور اُلٹی گنتی",
        descTimes: "آج کے چھ اوقات، اگلا نمایاں",
        descHijri: "ہجری اور عیسوی تاریخ",
        descAyah: "ہر روز ایک مختصر آیت اور اس کا مفہوم",
        descAdhkar: "وقت کے مطابق اذکار: صبح، شام، سونا، نماز کے بعد اور جمعہ",
        kahf: "جو جمعہ کے دن سورۂ کہف پڑھے، اس کے لیے دو جمعوں کے درمیان نور روشن رہتا ہے۔",
        salawat: "تمہارے بہترین دنوں میں سے جمعہ کا دن ہے… اس دن مجھ پر کثرت سے درود بھیجو، کیونکہ تمہارا درود مجھ پر پیش کیا جاتا ہے۔",
        hour: "جمعہ میں ایک گھڑی ایسی ہے کہ جو مسلمان بندہ اس میں کھڑا نماز پڑھتے ہوئے اللہ سے کچھ مانگے، اللہ اسے ضرور عطا فرماتا ہے۔",
        after: "نماز کے بعد کے اذکار",
        allowLocation: "اپنے اوقات دکھانے کے لیے میزان ویجیٹس کو مقام کی اجازت دیں",
        locationHint: "سیٹنگز ← میزان ← مقام: ایپ یا ویجیٹس استعمال کرتے وقت"
    )
    public static let id = WidgetText(
        adhkar: "Zikir saat ini",
        nextPrayer: "Salat berikutnya",
        prayerTimes: "Jadwal salat",
        hijri: "Tanggal Hijriah",
        dhikr: "Zikir dan tasbih",
        friday: "Sunah hari Jumat",
        arafah: "Hari Arafah",
        iftar: "Doa berbuka",
        lastThird: "Sepertiga malam terakhir",
        fridayEve: "Malam Jumat",
        lastTen: "Sepuluh malam terakhir",
        until: "hingga %s",
        next: "Berikutnya",
        inTime: "dalam",
        openApp: "Buka Mizan sekali agar jadwal Anda tampil di sini",
        kahfCta: "Baca surah al-Kahfi hari ini",
        descNext: "Salat berikutnya dengan hitung mundur",
        descTimes: "Enam waktu hari ini, yang berikutnya disorot",
        descHijri: "Tanggal Hijriah dan Masehi",
        descAyah: "Satu ayat pendek setiap hari beserta maknanya",
        descAdhkar: "Zikir sesuai waktu: pagi, petang, tidur, setelah salat, dan Jumat",
        kahf: "Barang siapa membaca surah al-Kahfi pada hari Jumat, ia akan diterangi cahaya di antara dua Jumat.",
        salawat: "Di antara hari terbaik kalian adalah hari Jumat… maka perbanyaklah selawat kepadaku pada hari itu, karena selawat kalian diperlihatkan kepadaku.",
        hour: "Pada hari Jumat ada satu saat; tidaklah seorang hamba muslim berdiri salat lalu memohon sesuatu kepada Allah bertepatan dengannya melainkan Allah memberinya.",
        after: "Zikir setelah salat",
        allowLocation: "Izinkan lokasi untuk widget Mizan agar jadwal Anda tampil",
        locationHint: "Pengaturan › Mizan › Lokasi: Saat Menggunakan App atau Widget"
    )
    public static let ms = WidgetText(
        adhkar: "Zikir waktu ini",
        nextPrayer: "Solat seterusnya",
        prayerTimes: "Waktu solat",
        hijri: "Tarikh Hijrah",
        dhikr: "Zikir dan tasbih",
        friday: "Sunnah hari Jumaat",
        arafah: "Hari Arafah",
        iftar: "Doa berbuka",
        lastThird: "Sepertiga malam terakhir",
        fridayEve: "Malam Jumaat",
        lastTen: "Sepuluh malam terakhir",
        until: "hingga %s",
        next: "Seterusnya",
        inTime: "dalam",
        openApp: "Buka Mizan sekali untuk memaparkan waktu anda di sini",
        kahfCta: "Baca surah al-Kahfi hari ini",
        descNext: "Solat seterusnya dengan kiraan detik",
        descTimes: "Enam waktu hari ini, yang seterusnya ditonjolkan",
        descHijri: "Tarikh Hijrah dan Masihi",
        descAyah: "Satu ayat pendek setiap hari bersama maknanya",
        descAdhkar: "Zikir mengikut waktu: pagi, petang, tidur, selepas solat dan Jumaat",
        kahf: "Sesiapa membaca surah al-Kahfi pada hari Jumaat, dia akan disinari cahaya antara dua Jumaat.",
        salawat: "Antara hari terbaik kamu ialah hari Jumaat… maka perbanyakkanlah selawat kepadaku padanya, kerana selawat kamu dibentangkan kepadaku.",
        hour: "Pada hari Jumaat ada satu saat; tiada hamba muslim yang berdiri solat memohon sesuatu kepada Allah tepat padanya melainkan Allah memberinya.",
        after: "Zikir selepas solat",
        allowLocation: "Benarkan lokasi untuk widget Mizan bagi memaparkan waktu anda",
        locationHint: "Seting › Mizan › Lokasi: Semasa Menggunakan App atau Widget"
    )
    public static let fa = WidgetText(
        adhkar: "اذکار این وقت",
        nextPrayer: "نماز بعدی",
        prayerTimes: "اوقات نماز",
        hijri: "تاریخ قمری",
        dhikr: "ذکر و تسبیح",
        friday: "سنت‌های جمعه",
        arafah: "روز عرفه",
        iftar: "دعای افطار",
        lastThird: "یک‌سوم پایانی شب",
        fridayEve: "شب جمعه",
        lastTen: "دهه آخر",
        until: "تا %s",
        next: "بعدی",
        inTime: "مانده",
        openApp: "میزان را یک بار باز کنید تا اوقاتتان اینجا نمایش داده شود",
        kahfCta: "امروز سوره کهف را بخوانید",
        descNext: "نماز بعدی با شمارش معکوس",
        descTimes: "شش وقت امروز، وقت بعدی برجسته",
        descHijri: "تاریخ قمری و میلادی",
        descAyah: "هر روز آیه‌ای کوتاه با معنایش",
        descAdhkar: "اذکار مناسب وقت: صبح، شام، خواب، پس از نماز و جمعه",
        kahf: "هر کس روز جمعه سوره کهف را بخواند، میان دو جمعه برایش نوری می‌درخشد.",
        salawat: "از برترین روزهای شما روز جمعه است… پس در آن بر من بسیار صلوات بفرستید، زیرا صلوات شما بر من عرضه می‌شود.",
        hour: "در روز جمعه ساعتی است که هیچ بنده مسلمانی در آن به نماز نمی‌ایستد و چیزی از خدا نمی‌خواهد مگر آنکه به او عطا می‌کند.",
        after: "اذکار پس از نماز",
        allowLocation: "برای نمایش اوقاتتان، به ابزارک‌های میزان اجازه موقعیت مکانی بدهید",
        locationHint: "تنظیمات ← میزان ← موقعیت مکانی: هنگام استفاده از برنامه یا ابزارک‌ها"
    )
    public static let bn = WidgetText(
        adhkar: "এখনকার জিকির",
        nextPrayer: "পরবর্তী নামাজ",
        prayerTimes: "নামাজের সময়",
        hijri: "হিজরি তারিখ",
        dhikr: "জিকির ও তাসবিহ",
        friday: "জুমার সুন্নাহ",
        arafah: "আরাফার দিন",
        iftar: "ইফতারের দোয়া",
        lastThird: "রাতের শেষ তৃতীয়াংশ",
        fridayEve: "জুমার রাত",
        lastTen: "শেষ দশ রাত",
        until: "%s পর্যন্ত",
        next: "পরবর্তী",
        inTime: "বাকি",
        openApp: "এখানে আপনার সময় দেখাতে একবার মিজান খুলুন",
        kahfCta: "আজ সূরা কাহফ পড়ুন",
        descNext: "পরবর্তী নামাজ ও কাউন্টডাউন",
        descTimes: "আজকের ছয়টি সময়, পরবর্তীটি চিহ্নিত",
        descHijri: "হিজরি ও খ্রিস্টীয় তারিখ",
        descAyah: "প্রতিদিন অর্থসহ একটি ছোট আয়াত",
        descAdhkar: "সময় অনুযায়ী জিকির: সকাল, সন্ধ্যা, ঘুম, নামাজের পর ও জুমা",
        kahf: "যে ব্যক্তি জুমার দিন সূরা কাহফ পড়ে, তার জন্য দুই জুমার মাঝে নূর আলোকিত থাকে।",
        salawat: "তোমাদের শ্রেষ্ঠ দিনগুলোর একটি জুমার দিন… সেদিন আমার ওপর বেশি বেশি দরুদ পাঠ করো, কেননা তোমাদের দরুদ আমার কাছে পেশ করা হয়।",
        hour: "জুমার দিনে এমন একটি সময় আছে, কোনো মুসলিম বান্দা নামাজে দাঁড়িয়ে তখন আল্লাহর কাছে কিছু চাইলে তিনি তা অবশ্যই দেন।",
        after: "নামাজের পরের জিকির",
        allowLocation: "আপনার সময় দেখাতে মিজান উইজেটকে লোকেশনের অনুমতি দিন",
        locationHint: "সেটিংস › মিজান › লোকেশন: অ্যাপ বা উইজেট ব্যবহারের সময়"
    )
    public static let ru = WidgetText(
        adhkar: "Азкары сейчас",
        nextPrayer: "Следующий намаз",
        prayerTimes: "Время намаза",
        hijri: "Дата по хиджре",
        dhikr: "Зикр и тасбих",
        friday: "Сунны пятницы",
        arafah: "День Арафа",
        iftar: "Разговение",
        lastThird: "Последняя треть ночи",
        fridayEve: "Ночь на пятницу",
        lastTen: "Последние десять ночей",
        until: "до %s",
        next: "Далее",
        inTime: "через",
        openApp: "Откройте Мизан один раз, чтобы здесь появилось ваше время",
        kahfCta: "Прочитайте суру аль-Кахф сегодня",
        descNext: "Следующий намаз и обратный отсчёт",
        descTimes: "Шесть времён дня, следующее выделено",
        descHijri: "Дата по хиджре и григорианская",
        descAyah: "Короткий аят каждый день с его смыслом",
        descAdhkar: "Азкары по времени: утро, вечер, сон, после намаза и пятница",
        kahf: "Тому, кто прочтёт суру аль-Кахф в пятницу, будет сиять свет между двумя пятницами.",
        salawat: "Лучший из ваших дней — пятница… так призывайте же в этот день побольше благословений на меня, ведь ваши благословения представляются мне.",
        hour: "В пятницу есть час: если раб-мусульманин, стоя в молитве, попросит в него у Аллаха что-либо, Он непременно даст ему это.",
        after: "Азкары после намаза",
        allowLocation: "Разрешите виджетам Мизан доступ к геопозиции, чтобы видеть своё время",
        locationHint: "Настройки › Мизан › Геопозиция: при использовании приложения или виджетов"
    )
    public static let de = WidgetText(
        adhkar: "Adhkar für jetzt",
        nextPrayer: "Nächstes Gebet",
        prayerTimes: "Gebetszeiten",
        hijri: "Hidschri-Datum",
        dhikr: "Dhikr und Tasbih",
        friday: "Sunna des Freitags",
        arafah: "Tag von Arafa",
        iftar: "Fastenbrechen",
        lastThird: "Letztes Drittel der Nacht",
        fridayEve: "Freitagnacht",
        lastTen: "Letzte zehn Nächte",
        until: "bis %s",
        next: "Weiter",
        inTime: "in",
        openApp: "Öffne Mizan einmal, damit deine Zeiten hier erscheinen",
        kahfCta: "Lies heute Sura al-Kahf",
        descNext: "Das nächste Gebet mit Countdown",
        descTimes: "Die sechs Zeiten des Tages, die nächste hervorgehoben",
        descHijri: "Hidschri- und gregorianisches Datum",
        descAyah: "Jeden Tag ein kurzer Vers mit seiner Bedeutung",
        descAdhkar: "Die passenden Adhkar zur Zeit: Morgen, Abend, Schlaf, nach dem Gebet und Freitag",
        kahf: "Wer am Freitag Sura al-Kahf rezitiert, dem leuchtet ein Licht zwischen den beiden Freitagen.",
        salawat: "Zu euren besten Tagen gehört der Freitag… sprecht darum an ihm viele Segenswünsche über mich, denn eure Segenswünsche werden mir vorgelegt.",
        hour: "Am Freitag gibt es eine Stunde: Kein muslimischer Diener steht darin im Gebet und bittet Allah um etwas, ohne dass Er es ihm gibt.",
        after: "Adhkar nach dem Gebet",
        allowLocation: "Erlaube den Standort für Mizan-Widgets, um deine Zeiten zu sehen",
        locationHint: "Einstellungen › Mizan › Standort: Beim Verwenden der App oder Widgets"
    )
    public static let es = WidgetText(
        adhkar: "Adhkar de ahora",
        nextPrayer: "Próxima oración",
        prayerTimes: "Horarios de oración",
        hijri: "Fecha de la Hégira",
        dhikr: "Dhikr y tasbih",
        friday: "Sunnas del viernes",
        arafah: "Día de Arafa",
        iftar: "Ruptura del ayuno",
        lastThird: "Último tercio de la noche",
        fridayEve: "Víspera del viernes",
        lastTen: "Últimas diez noches",
        until: "hasta %s",
        next: "Siguiente",
        inTime: "en",
        openApp: "Abre Mizan una vez para ver tus horarios aquí",
        kahfCta: "Lee hoy la sura al-Kahf",
        descNext: "La próxima oración con cuenta atrás",
        descTimes: "Los seis horarios del día, el próximo destacado",
        descHijri: "Fecha de la Hégira y gregoriana",
        descAyah: "Una aleya breve cada día con su significado",
        descAdhkar: "Los adhkar del momento: mañana, tarde, sueño, tras la oración y viernes",
        kahf: "Quien recite la sura al-Kahf el viernes, una luz brillará para él entre los dos viernes.",
        salawat: "Entre los mejores de vuestros días está el viernes… multiplicad en él las bendiciones sobre mí, pues vuestras bendiciones me son presentadas.",
        hour: "El viernes hay una hora en la que ningún siervo musulmán que esté de pie rezando pide algo a Allah sin que Él se lo conceda.",
        after: "Adhkar tras la oración",
        allowLocation: "Permite la ubicación para los widgets de Mizan y verás tus horarios",
        locationHint: "Ajustes › Mizan › Ubicación: Cuando se use la app o los widgets"
    )
    public static let uk = WidgetText(
        adhkar: "Азкари зараз",
        nextPrayer: "Наступний намаз",
        prayerTimes: "Час намазу",
        hijri: "Дата за хіджрою",
        dhikr: "Зікр і тасбіх",
        friday: "Сунни п’ятниці",
        arafah: "День Арафа",
        iftar: "Розговіння",
        lastThird: "Остання третина ночі",
        fridayEve: "Ніч на п’ятницю",
        lastTen: "Останні десять ночей",
        until: "до %s",
        next: "Далі",
        inTime: "через",
        openApp: "Відкрийте Мізан один раз, щоб тут з’явився ваш час",
        kahfCta: "Прочитайте суру аль-Кахф сьогодні",
        descNext: "Наступний намаз і зворотний відлік",
        descTimes: "Шість часів дня, наступний виділено",
        descHijri: "Дата за хіджрою та григоріанська",
        descAyah: "Короткий аят щодня з його змістом",
        descAdhkar: "Азкари за часом: ранок, вечір, сон, після намазу та п’ятниця",
        kahf: "Тому, хто прочитає суру аль-Кахф у п’ятницю, світитиме світло між двома п’ятницями.",
        salawat: "Найкращий із ваших днів — п’ятниця… тож благословляйте мене в цей день якомога більше, бо ваші благословення представляються мені.",
        hour: "У п’ятницю є година: якщо раб-мусульманин, стоячи в молитві, попросить у неї в Аллаха щось, Він неодмінно дасть йому це.",
        after: "Азкари після намазу",
        allowLocation: "Дозвольте віджетам Мізан доступ до геопозиції, щоб бачити свій час",
        locationHint: "Параметри › Мізан › Локація: коли застосунок або віджети активні"
    )
    public static let uz = WidgetText(
        adhkar: "Vaqt zikrlari",
        nextPrayer: "Keyingi namoz",
        prayerTimes: "Namoz vaqtlari",
        hijri: "Hijriy sana",
        dhikr: "Zikr va tasbeh",
        friday: "Juma sunnatlari",
        arafah: "Arafa kuni",
        iftar: "Iftor duosi",
        lastThird: "Kechaning oxirgi uchdan biri",
        fridayEve: "Juma kechasi",
        lastTen: "Oxirgi o‘n kecha",
        until: "%s gacha",
        next: "Keyingi",
        inTime: "qoldi",
        openApp: "Vaqtlaringiz shu yerda chiqishi uchun Mizonni bir marta oching",
        kahfCta: "Bugun Kahf surasini o‘qing",
        descNext: "Keyingi namoz va teskari sanoq",
        descTimes: "Kunning olti vaqti, keyingisi ajratilgan",
        descHijri: "Hijriy va milodiy sana",
        descAyah: "Har kuni ma’nosi bilan qisqa oyat",
        descAdhkar: "Vaqtga mos zikrlar: tong, kech, uyqu, namozdan keyin va juma",
        kahf: "Kim juma kuni Kahf surasini o‘qisa, ikki juma orasida unga nur yoritib turadi.",
        salawat: "Kunlaringizning eng afzallaridan biri juma kunidir… U kuni menga ko‘p salovot ayting, chunki salovatingiz menga yetkaziladi.",
        hour: "Jumada bir soat borki, musulmon banda namozda turib, unda Allohdan biror narsa so‘rasa, albatta, Alloh unga beradi.",
        after: "Namozdan keyingi zikrlar",
        allowLocation: "Vaqtlaringiz chiqishi uchun Mizon vidjetlariga joylashuvga ruxsat bering",
        locationHint: "Sozlamalar › Mizon › Joylashuv: ilova yoki vidjetlar ishlatilganda"
    )
    public static let kk = WidgetText(
        adhkar: "Уақыт зікірлері",
        nextPrayer: "Келесі намаз",
        prayerTimes: "Намаз уақыттары",
        hijri: "Һижри күні",
        dhikr: "Зікір және тәспих",
        friday: "Жұма сүннеттері",
        arafah: "Арафа күні",
        iftar: "Ауызашар дұғасы",
        lastThird: "Түннің соңғы үштен бірі",
        fridayEve: "Жұма түні",
        lastTen: "Соңғы он түн",
        until: "%s дейін",
        next: "Келесі",
        inTime: "қалды",
        openApp: "Уақыттарыңыз осында шығуы үшін Мизанды бір рет ашыңыз",
        kahfCta: "Бүгін Кәһф сүресін оқыңыз",
        descNext: "Келесі намаз және кері санақ",
        descTimes: "Күннің алты уақыты, келесісі ерекшеленген",
        descHijri: "Һижри және григориан күні",
        descAyah: "Күн сайын мағынасымен қысқа аят",
        descAdhkar: "Уақытқа сай зікірлер: таң, кеш, ұйқы, намаздан кейін және жұма",
        kahf: "Кім жұма күні Кәһф сүресін оқыса, оған екі жұманың арасында нұр жарқырайды.",
        salawat: "Күндеріңіздің ең абзалдарының бірі — жұма… сол күні маған көп салауат айтыңдар, өйткені салауаттарың маған жеткізіледі.",
        hour: "Жұма күні бір сағат бар: намазда тұрған мұсылман пенде сол сәтте Алладан бір нәрсе сұраса, Ол оны міндетті түрде береді.",
        after: "Намаздан кейінгі зікірлер",
        allowLocation: "Уақыттарыңыз шығуы үшін Мизан виджеттеріне орналасуға рұқсат беріңіз",
        locationHint: "Параметрлер › Мизан › Орналасу: қолданба не виджеттер қолданылғанда"
    )

    public static let all: [String: WidgetText] = ["ar": ar, "en": en, "fr": fr, "tr": tr, "ur": ur, "id": id, "ms": ms, "fa": fa, "bn": bn, "ru": ru, "de": de, "es": es, "uk": uk, "uz": uz, "kk": kk]
}

/// نص مفتاح «على شاشة القفل» في شاشة المسبحة بلغات التطبيق: العنوان والوصف
public enum LiveTasbihText {
    public static func of(_ code: String) -> (title: String, desc: String) {
        switch code {
        case "ar": return ("العدّاد على شاشة القفل والجزيرة", "أطفئه إن لم ترد بطاقة المسبحة والأذكار على شاشة القفل؛ يبقى زر الإجراء يعدّ")
        case "fr": return ("Compteur sur l’écran verrouillé et la Dynamic Island", "Désactivez-le pour ne pas afficher la carte du tasbih et des adhkar ; le bouton Action continue de compter")
        case "tr": return ("Kilit Ekranı ve Dynamic Island’da sayaç", "Tesbih ve zikir kartını Kilit Ekranı’nda istemiyorsanız kapatın; Eylem düğmesi saymaya devam eder")
        case "ur": return ("لاک اسکرین اور ڈائنامک آئی لینڈ پر کاؤنٹر", "اگر لاک اسکرین پر تسبیح اور اذکار کا کارڈ نہ چاہیں تو اسے بند کریں؛ ایکشن بٹن گنتی جاری رکھے گا")
        case "id": return ("Penghitung di Layar Terkunci dan Dynamic Island", "Matikan jika tidak ingin kartu tasbih dan zikir di Layar Terkunci; tombol Tindakan tetap menghitung")
        case "ms": return ("Pembilang di Skrin Kunci dan Dynamic Island", "Matikan jika tidak mahu kad tasbih dan zikir di Skrin Kunci; butang Tindakan terus mengira")
        case "fa": return ("شمارنده روی صفحهٔ قفل و دایناميک آیلند", "اگر کارت تسبیح و اذکار را روی صفحهٔ قفل نمی‌خواهید خاموشش کنید؛ دکمهٔ اکشن همچنان می‌شمارد")
        case "bn": return ("লক স্ক্রিন ও ডায়নামিক আইল্যান্ডে কাউন্টার", "লক স্ক্রিনে তাসবিহ ও জিকিরের কার্ড না চাইলে বন্ধ করুন; অ্যাকশন বোতাম গণনা চালিয়ে যাবে")
        case "ru": return ("Счётчик на экране блокировки и в Dynamic Island", "Выключите, если не нужна карточка тасбиха и азкаров на экране блокировки; кнопка действия продолжит считать")
        case "de": return ("Zähler auf dem Sperrbildschirm und in der Dynamic Island", "Ausschalten, wenn die Tasbih- und Adhkar-Karte nicht auf dem Sperrbildschirm erscheinen soll; die Aktionstaste zählt weiter")
        case "es": return ("Contador en la pantalla bloqueada y la Dynamic Island", "Desactívalo si no quieres la tarjeta del tasbih y los adhkar en la pantalla bloqueada; el botón de acción sigue contando")
        case "uk": return ("Лічильник на екрані блокування та в Dynamic Island", "Вимкніть, якщо не потрібна картка тасбіху й азкарів на екрані блокування; кнопка дії й далі рахує")
        case "uz": return ("Qulf ekrani va Dynamic Island’da hisoblagich", "Qulf ekranida tasbeh va zikr kartasi kerak bo‘lmasa, o‘chiring; Amal tugmasi sanashda davom etadi")
        case "kk": return ("Құлып экраны мен Dynamic Island-тағы санауыш", "Құлып экранында тәсбих пен зікір картасы керек болмаса, өшіріңіз; Әрекет түймесі санауды жалғастырады")
        default: return ("Counter on the Lock Screen and Dynamic Island", "Turn off if you don’t want the tasbih and adhkar card on the Lock Screen; the Action button keeps counting")
        }
    }
}
