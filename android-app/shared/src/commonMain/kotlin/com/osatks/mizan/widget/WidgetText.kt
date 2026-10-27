package com.osatks.mizan.widget

import com.osatks.mizan.adhkar.Dhikr

/**
 * نصوص الأدوات وحدها (أندرويد و iOS) بكل لغات التطبيق، في ملف واحد مستقل عن Strings حتى لا تتغير حزم اللغات.
 * kahf و salawat و hour: ترجمة أحاديث الجمعة الثلاثة التي تضيفها الأداة إلى أذكار المكتبة.
 */
class WidgetText(
    val adhkar: String,        // اسم الأداة: «أذكار الوقت»
    val nextPrayer: String,
    val prayerTimes: String,
    val hijri: String,
    val dhikr: String,         // نافذة التسبيح المطلق
    val friday: String,        // سنن الجمعة
    val arafah: String,
    val iftar: String,
    val lastThird: String,
    val fridayEve: String,
    val lastTen: String,
    val until: String,         // «حتى %s»
    val next: String,          // زر الذكر التالي
    val inTime: String,        // «بعد» قبل العدّ التنازلي
    val openApp: String,       // iOS: لم تُكتب بيانات الأداة بعد
    val kahfCta: String,       // «اقرأ سورة الكهف اليوم»
    val descNext: String, val descTimes: String, val descHijri: String, val descAyah: String, val descAdhkar: String,
    val kahf: String?, val salawat: String?, val hour: String?,
    val after: String,         // الأذكار بعد السلام من الصلاة (عنوان قصير)
    val allowLocation: String, // iOS: الأداة تحسب المواقيت وحدها وتحتاج إذن الموقع
    val locationHint: String,  // iOS: أين يُمنح الإذن في الإعدادات
) {
    fun untilTime(t: String) = until.replace("%s", t)

    companion object {
        const val KAHF = "fri_kahf"
        const val SALAWAT = "fri_salawat"
        const val HOUR = "fri_hour"

        /** أحاديث الجمعة التي ليست في مكتبة الأذكار (نصها ومصدرها كما في بقية الأذكار) */
        val EXTRA: Map<String, Dhikr> = listOf(
            Dhikr(
                KAHF, "«مَنْ قَرَأَ سُورَةَ الْكَهْفِ فِي يَوْمِ الْجُمُعَةِ أَضَاءَ لَهُ مِنَ النُّورِ مَا بَيْنَ الْجُمُعَتَيْنِ»", 1,
                emptyList(), listOf("hakim" to "2/368", "bayhaqi" to "3/249"), "sahih", "albani",
                "أبو سعيد الخدري رضي الله عنه", null, null,
            ),
            Dhikr(
                SALAWAT, "«إِنَّ مِنْ أَفْضَلِ أَيَّامِكُمْ يَوْمَ الْجُمُعَةِ … فَأَكْثِرُوا عَلَيَّ مِنَ الصَّلَاةِ فِيهِ، فَإِنَّ صَلَاتَكُمْ مَعْرُوضَةٌ عَلَيَّ»", 1,
                emptyList(), listOf("abudawud" to "1047", "nasai" to "1374"), "sahih", "albani",
                "أوس بن أوس رضي الله عنه", null, null,
            ),
            Dhikr(
                HOUR, "«فِيهِ سَاعَةٌ لَا يُوَافِقُهَا عَبْدٌ مُسْلِمٌ وَهُوَ قَائِمٌ يُصَلِّي يَسْأَلُ اللَّهَ تَعَالَى شَيْئًا إِلَّا أَعْطَاهُ إِيَّاهُ»", 1,
                emptyList(), listOf("bukhari" to "935", "muslim" to "852"), "muttafaq", "",
                "أبو هريرة رضي الله عنه", null, null,
            ),
        ).associateBy { it.id }

        fun of(code: String): WidgetText = ALL[code] ?: EN

        val AR = WidgetText(
            "أذكار الوقت", "الصلاة القادمة", "مواقيت الصلاة", "التاريخ الهجري",
            "ذكر وتسبيح", "سنن الجمعة", "يوم عرفة", "دعاء الإفطار", "الثلث الأخير من الليل", "ليلة الجمعة", "العشر الأواخر",
            "حتى %s", "التالي", "بعد", "افتح ميزان مرة لتظهر مواقيتك هنا", "اقرأ سورة الكهف اليوم",
            "الصلاة القادمة وعدّ تنازلي لها", "مواقيت اليوم الست والقادمة مميزة", "التاريخ الهجري والميلادي",
            "آية قصيرة كل يوم مع معناها", "الأذكار المناسبة للوقت: الصباح والمساء والنوم وبعد الصلاة والجمعة",
            null, null, null,
            after = "أذكار بعد الصلاة",
            allowLocation = "اسمح لأدوات ميزان بمعرفة موقعك لتظهر مواقيتك",
            locationHint = "الإعدادات ← ميزان ← الموقع: أثناء استخدام التطبيق أو الأدوات",
        )
        val EN = WidgetText(
            "Adhkar for now", "Next prayer", "Prayer times", "Hijri date",
            "Dhikr & tasbih", "Friday sunnahs", "Day of Arafah", "Breaking the fast", "Last third of the night", "Friday eve", "Last ten nights",
            "until %s", "Next", "in", "Open Mizan once to show your times here", "Read Surat al-Kahf today",
            "The next prayer with a live countdown", "Today's six times with the next one highlighted", "Hijri and Gregorian date",
            "A short ayah every day with its meaning", "The right adhkar for the time: morning, evening, sleep, after prayer and Friday",
            "Whoever recites Surat al-Kahf on Friday, a light will shine for him between the two Fridays.",
            "Among the best of your days is Friday… so send abundant blessings upon me on it, for your blessings are presented to me.",
            "On Friday there is an hour in which no Muslim servant stands in prayer asking Allah for something but He gives it to him.",
            after = "After-prayer adhkar",
            allowLocation = "Allow location for Mizan widgets to show your times",
            locationHint = "Settings › Mizan › Location: While Using the App or Widgets",
        )
        val FR = WidgetText(
            "Adhkar du moment", "Prochaine prière", "Horaires de prière", "Date hégirienne",
            "Dhikr et tasbih", "Sunnas du vendredi", "Jour d’Arafat", "Rupture du jeûne", "Dernier tiers de la nuit", "Veille du vendredi", "Dix dernières nuits",
            "jusqu’à %s", "Suivant", "dans", "Ouvrez Mizan une fois pour afficher vos horaires ici", "Lisez la sourate al-Kahf aujourd’hui",
            "La prochaine prière avec un compte à rebours", "Les six horaires du jour, le prochain mis en avant", "Date hégirienne et grégorienne",
            "Un court verset chaque jour avec son sens", "Les adhkar du moment : matin, soir, sommeil, après la prière et vendredi",
            "Celui qui récite la sourate al-Kahf le vendredi, une lumière brillera pour lui entre les deux vendredis.",
            "Parmi les meilleurs de vos jours est le vendredi… multipliez-y donc les prières sur moi, car vos prières me sont présentées.",
            "Le vendredi, il est une heure où aucun serviteur musulman debout en prière ne demande quelque chose à Allah sans qu’Il le lui accorde.",
            after = "Adhkar après la prière",
            allowLocation = "Autorisez la localisation pour les widgets Mizan afin d’afficher vos horaires",
            locationHint = "Réglages › Mizan › Position : Lorsque l’app ou les widgets sont actifs",
        )
        val TR = WidgetText(
            "Vaktin zikirleri", "Sıradaki namaz", "Namaz vakitleri", "Hicri tarih",
            "Zikir ve tesbih", "Cuma sünnetleri", "Arefe günü", "İftar duası", "Gecenin son üçte biri", "Cuma gecesi", "Son on gece",
            "%s vaktine kadar", "Sonraki", "kalan", "Vakitlerinizin burada görünmesi için Mizan’ı bir kez açın", "Bugün Kehf suresini okuyun",
            "Sıradaki namaz ve geri sayım", "Günün altı vakti, sıradaki vurgulu", "Hicri ve miladi tarih",
            "Her gün anlamıyla kısa bir ayet", "Vakte uygun zikirler: sabah, akşam, uyku, namaz sonrası ve cuma",
            "Kim cuma günü Kehf suresini okursa, iki cuma arası onun için nur ile aydınlanır.",
            "Günlerinizin en faziletlilerinden biri cuma günüdür… O gün bana çokça salavat getirin; salavatınız bana arz olunur.",
            "Cuma gününde bir saat vardır ki, namazda duran Müslüman bir kul o saatte Allah’tan bir şey isterse, Allah onu mutlaka verir.",
            after = "Namaz sonrası zikirler",
            allowLocation = "Vakitlerinizi göstermek için Mizan widget’larına konum izni verin",
            locationHint = "Ayarlar › Mizan › Konum: Uygulamayı veya Widget’ları Kullanırken",
        )
        val UR = WidgetText(
            "وقت کے اذکار", "اگلی نماز", "اوقاتِ نماز", "ہجری تاریخ",
            "ذکر و تسبیح", "جمعہ کی سنتیں", "یومِ عرفہ", "افطار کی دعا", "رات کا آخری تہائی", "شبِ جمعہ", "آخری عشرہ",
            "%s تک", "اگلا", "باقی", "اپنے اوقات یہاں دیکھنے کے لیے میزان ایک بار کھولیں", "آج سورۂ کہف پڑھیں",
            "اگلی نماز اور اُلٹی گنتی", "آج کے چھ اوقات، اگلا نمایاں", "ہجری اور عیسوی تاریخ",
            "ہر روز ایک مختصر آیت اور اس کا مفہوم", "وقت کے مطابق اذکار: صبح، شام، سونا، نماز کے بعد اور جمعہ",
            "جو جمعہ کے دن سورۂ کہف پڑھے، اس کے لیے دو جمعوں کے درمیان نور روشن رہتا ہے۔",
            "تمہارے بہترین دنوں میں سے جمعہ کا دن ہے… اس دن مجھ پر کثرت سے درود بھیجو، کیونکہ تمہارا درود مجھ پر پیش کیا جاتا ہے۔",
            "جمعہ میں ایک گھڑی ایسی ہے کہ جو مسلمان بندہ اس میں کھڑا نماز پڑھتے ہوئے اللہ سے کچھ مانگے، اللہ اسے ضرور عطا فرماتا ہے۔",
            after = "نماز کے بعد کے اذکار",
            allowLocation = "اپنے اوقات دکھانے کے لیے میزان ویجیٹس کو مقام کی اجازت دیں",
            locationHint = "سیٹنگز ← میزان ← مقام: ایپ یا ویجیٹس استعمال کرتے وقت",
        )
        val ID = WidgetText(
            "Zikir saat ini", "Salat berikutnya", "Jadwal salat", "Tanggal Hijriah",
            "Zikir dan tasbih", "Sunah hari Jumat", "Hari Arafah", "Doa berbuka", "Sepertiga malam terakhir", "Malam Jumat", "Sepuluh malam terakhir",
            "hingga %s", "Berikutnya", "dalam", "Buka Mizan sekali agar jadwal Anda tampil di sini", "Baca surah al-Kahfi hari ini",
            "Salat berikutnya dengan hitung mundur", "Enam waktu hari ini, yang berikutnya disorot", "Tanggal Hijriah dan Masehi",
            "Satu ayat pendek setiap hari beserta maknanya", "Zikir sesuai waktu: pagi, petang, tidur, setelah salat, dan Jumat",
            "Barang siapa membaca surah al-Kahfi pada hari Jumat, ia akan diterangi cahaya di antara dua Jumat.",
            "Di antara hari terbaik kalian adalah hari Jumat… maka perbanyaklah selawat kepadaku pada hari itu, karena selawat kalian diperlihatkan kepadaku.",
            "Pada hari Jumat ada satu saat; tidaklah seorang hamba muslim berdiri salat lalu memohon sesuatu kepada Allah bertepatan dengannya melainkan Allah memberinya.",
            after = "Zikir setelah salat",
            allowLocation = "Izinkan lokasi untuk widget Mizan agar jadwal Anda tampil",
            locationHint = "Pengaturan › Mizan › Lokasi: Saat Menggunakan App atau Widget",
        )
        val MS = WidgetText(
            "Zikir waktu ini", "Solat seterusnya", "Waktu solat", "Tarikh Hijrah",
            "Zikir dan tasbih", "Sunnah hari Jumaat", "Hari Arafah", "Doa berbuka", "Sepertiga malam terakhir", "Malam Jumaat", "Sepuluh malam terakhir",
            "hingga %s", "Seterusnya", "dalam", "Buka Mizan sekali untuk memaparkan waktu anda di sini", "Baca surah al-Kahfi hari ini",
            "Solat seterusnya dengan kiraan detik", "Enam waktu hari ini, yang seterusnya ditonjolkan", "Tarikh Hijrah dan Masihi",
            "Satu ayat pendek setiap hari bersama maknanya", "Zikir mengikut waktu: pagi, petang, tidur, selepas solat dan Jumaat",
            "Sesiapa membaca surah al-Kahfi pada hari Jumaat, dia akan disinari cahaya antara dua Jumaat.",
            "Antara hari terbaik kamu ialah hari Jumaat… maka perbanyakkanlah selawat kepadaku padanya, kerana selawat kamu dibentangkan kepadaku.",
            "Pada hari Jumaat ada satu saat; tiada hamba muslim yang berdiri solat memohon sesuatu kepada Allah tepat padanya melainkan Allah memberinya.",
            after = "Zikir selepas solat",
            allowLocation = "Benarkan lokasi untuk widget Mizan bagi memaparkan waktu anda",
            locationHint = "Seting › Mizan › Lokasi: Semasa Menggunakan App atau Widget",
        )
        val FA = WidgetText(
            "اذکار این وقت", "نماز بعدی", "اوقات نماز", "تاریخ قمری",
            "ذکر و تسبیح", "سنت‌های جمعه", "روز عرفه", "دعای افطار", "یک‌سوم پایانی شب", "شب جمعه", "دهه آخر",
            "تا %s", "بعدی", "مانده", "میزان را یک بار باز کنید تا اوقاتتان اینجا نمایش داده شود", "امروز سوره کهف را بخوانید",
            "نماز بعدی با شمارش معکوس", "شش وقت امروز، وقت بعدی برجسته", "تاریخ قمری و میلادی",
            "هر روز آیه‌ای کوتاه با معنایش", "اذکار مناسب وقت: صبح، شام، خواب، پس از نماز و جمعه",
            "هر کس روز جمعه سوره کهف را بخواند، میان دو جمعه برایش نوری می‌درخشد.",
            "از برترین روزهای شما روز جمعه است… پس در آن بر من بسیار صلوات بفرستید، زیرا صلوات شما بر من عرضه می‌شود.",
            "در روز جمعه ساعتی است که هیچ بنده مسلمانی در آن به نماز نمی‌ایستد و چیزی از خدا نمی‌خواهد مگر آنکه به او عطا می‌کند.",
            after = "اذکار پس از نماز",
            allowLocation = "برای نمایش اوقاتتان، به ابزارک‌های میزان اجازه موقعیت مکانی بدهید",
            locationHint = "تنظیمات ← میزان ← موقعیت مکانی: هنگام استفاده از برنامه یا ابزارک‌ها",
        )
        val BN = WidgetText(
            "এখনকার জিকির", "পরবর্তী নামাজ", "নামাজের সময়", "হিজরি তারিখ",
            "জিকির ও তাসবিহ", "জুমার সুন্নাহ", "আরাফার দিন", "ইফতারের দোয়া", "রাতের শেষ তৃতীয়াংশ", "জুমার রাত", "শেষ দশ রাত",
            "%s পর্যন্ত", "পরবর্তী", "বাকি", "এখানে আপনার সময় দেখাতে একবার মিজান খুলুন", "আজ সূরা কাহফ পড়ুন",
            "পরবর্তী নামাজ ও কাউন্টডাউন", "আজকের ছয়টি সময়, পরবর্তীটি চিহ্নিত", "হিজরি ও খ্রিস্টীয় তারিখ",
            "প্রতিদিন অর্থসহ একটি ছোট আয়াত", "সময় অনুযায়ী জিকির: সকাল, সন্ধ্যা, ঘুম, নামাজের পর ও জুমা",
            "যে ব্যক্তি জুমার দিন সূরা কাহফ পড়ে, তার জন্য দুই জুমার মাঝে নূর আলোকিত থাকে।",
            "তোমাদের শ্রেষ্ঠ দিনগুলোর একটি জুমার দিন… সেদিন আমার ওপর বেশি বেশি দরুদ পাঠ করো, কেননা তোমাদের দরুদ আমার কাছে পেশ করা হয়।",
            "জুমার দিনে এমন একটি সময় আছে, কোনো মুসলিম বান্দা নামাজে দাঁড়িয়ে তখন আল্লাহর কাছে কিছু চাইলে তিনি তা অবশ্যই দেন।",
            after = "নামাজের পরের জিকির",
            allowLocation = "আপনার সময় দেখাতে মিজান উইজেটকে লোকেশনের অনুমতি দিন",
            locationHint = "সেটিংস › মিজান › লোকেশন: অ্যাপ বা উইজেট ব্যবহারের সময়",
        )
        val RU = WidgetText(
            "Азкары сейчас", "Следующий намаз", "Время намаза", "Дата по хиджре",
            "Зикр и тасбих", "Сунны пятницы", "День Арафа", "Разговение", "Последняя треть ночи", "Ночь на пятницу", "Последние десять ночей",
            "до %s", "Далее", "через", "Откройте Мизан один раз, чтобы здесь появилось ваше время", "Прочитайте суру аль-Кахф сегодня",
            "Следующий намаз и обратный отсчёт", "Шесть времён дня, следующее выделено", "Дата по хиджре и григорианская",
            "Короткий аят каждый день с его смыслом", "Азкары по времени: утро, вечер, сон, после намаза и пятница",
            "Тому, кто прочтёт суру аль-Кахф в пятницу, будет сиять свет между двумя пятницами.",
            "Лучший из ваших дней — пятница… так призывайте же в этот день побольше благословений на меня, ведь ваши благословения представляются мне.",
            "В пятницу есть час: если раб-мусульманин, стоя в молитве, попросит в него у Аллаха что-либо, Он непременно даст ему это.",
            after = "Азкары после намаза",
            allowLocation = "Разрешите виджетам Мизан доступ к геопозиции, чтобы видеть своё время",
            locationHint = "Настройки › Мизан › Геопозиция: при использовании приложения или виджетов",
        )
        val DE = WidgetText(
            "Adhkar für jetzt", "Nächstes Gebet", "Gebetszeiten", "Hidschri-Datum",
            "Dhikr und Tasbih", "Sunna des Freitags", "Tag von Arafa", "Fastenbrechen", "Letztes Drittel der Nacht", "Freitagnacht", "Letzte zehn Nächte",
            "bis %s", "Weiter", "in", "Öffne Mizan einmal, damit deine Zeiten hier erscheinen", "Lies heute Sura al-Kahf",
            "Das nächste Gebet mit Countdown", "Die sechs Zeiten des Tages, die nächste hervorgehoben", "Hidschri- und gregorianisches Datum",
            "Jeden Tag ein kurzer Vers mit seiner Bedeutung", "Die passenden Adhkar zur Zeit: Morgen, Abend, Schlaf, nach dem Gebet und Freitag",
            "Wer am Freitag Sura al-Kahf rezitiert, dem leuchtet ein Licht zwischen den beiden Freitagen.",
            "Zu euren besten Tagen gehört der Freitag… sprecht darum an ihm viele Segenswünsche über mich, denn eure Segenswünsche werden mir vorgelegt.",
            "Am Freitag gibt es eine Stunde: Kein muslimischer Diener steht darin im Gebet und bittet Allah um etwas, ohne dass Er es ihm gibt.",
            after = "Adhkar nach dem Gebet",
            allowLocation = "Erlaube den Standort für Mizan-Widgets, um deine Zeiten zu sehen",
            locationHint = "Einstellungen › Mizan › Standort: Beim Verwenden der App oder Widgets",
        )
        val ES = WidgetText(
            "Adhkar de ahora", "Próxima oración", "Horarios de oración", "Fecha de la Hégira",
            "Dhikr y tasbih", "Sunnas del viernes", "Día de Arafa", "Ruptura del ayuno", "Último tercio de la noche", "Víspera del viernes", "Últimas diez noches",
            "hasta %s", "Siguiente", "en", "Abre Mizan una vez para ver tus horarios aquí", "Lee hoy la sura al-Kahf",
            "La próxima oración con cuenta atrás", "Los seis horarios del día, el próximo destacado", "Fecha de la Hégira y gregoriana",
            "Una aleya breve cada día con su significado", "Los adhkar del momento: mañana, tarde, sueño, tras la oración y viernes",
            "Quien recite la sura al-Kahf el viernes, una luz brillará para él entre los dos viernes.",
            "Entre los mejores de vuestros días está el viernes… multiplicad en él las bendiciones sobre mí, pues vuestras bendiciones me son presentadas.",
            "El viernes hay una hora en la que ningún siervo musulmán que esté de pie rezando pide algo a Allah sin que Él se lo conceda.",
            after = "Adhkar tras la oración",
            allowLocation = "Permite la ubicación para los widgets de Mizan y verás tus horarios",
            locationHint = "Ajustes › Mizan › Ubicación: Cuando se use la app o los widgets",
        )
        val UK = WidgetText(
            "Азкари зараз", "Наступний намаз", "Час намазу", "Дата за хіджрою",
            "Зікр і тасбіх", "Сунни п’ятниці", "День Арафа", "Розговіння", "Остання третина ночі", "Ніч на п’ятницю", "Останні десять ночей",
            "до %s", "Далі", "через", "Відкрийте Мізан один раз, щоб тут з’явився ваш час", "Прочитайте суру аль-Кахф сьогодні",
            "Наступний намаз і зворотний відлік", "Шість часів дня, наступний виділено", "Дата за хіджрою та григоріанська",
            "Короткий аят щодня з його змістом", "Азкари за часом: ранок, вечір, сон, після намазу та п’ятниця",
            "Тому, хто прочитає суру аль-Кахф у п’ятницю, світитиме світло між двома п’ятницями.",
            "Найкращий із ваших днів — п’ятниця… тож благословляйте мене в цей день якомога більше, бо ваші благословення представляються мені.",
            "У п’ятницю є година: якщо раб-мусульманин, стоячи в молитві, попросить у неї в Аллаха щось, Він неодмінно дасть йому це.",
            after = "Азкари після намазу",
            allowLocation = "Дозвольте віджетам Мізан доступ до геопозиції, щоб бачити свій час",
            locationHint = "Параметри › Мізан › Локація: коли застосунок або віджети активні",
        )
        val UZ = WidgetText(
            "Vaqt zikrlari", "Keyingi namoz", "Namoz vaqtlari", "Hijriy sana",
            "Zikr va tasbeh", "Juma sunnatlari", "Arafa kuni", "Iftor duosi", "Kechaning oxirgi uchdan biri", "Juma kechasi", "Oxirgi o‘n kecha",
            "%s gacha", "Keyingi", "qoldi", "Vaqtlaringiz shu yerda chiqishi uchun Mizonni bir marta oching", "Bugun Kahf surasini o‘qing",
            "Keyingi namoz va teskari sanoq", "Kunning olti vaqti, keyingisi ajratilgan", "Hijriy va milodiy sana",
            "Har kuni ma’nosi bilan qisqa oyat", "Vaqtga mos zikrlar: tong, kech, uyqu, namozdan keyin va juma",
            "Kim juma kuni Kahf surasini o‘qisa, ikki juma orasida unga nur yoritib turadi.",
            "Kunlaringizning eng afzallaridan biri juma kunidir… U kuni menga ko‘p salovot ayting, chunki salovatingiz menga yetkaziladi.",
            "Jumada bir soat borki, musulmon banda namozda turib, unda Allohdan biror narsa so‘rasa, albatta, Alloh unga beradi.",
            after = "Namozdan keyingi zikrlar",
            allowLocation = "Vaqtlaringiz chiqishi uchun Mizon vidjetlariga joylashuvga ruxsat bering",
            locationHint = "Sozlamalar › Mizon › Joylashuv: ilova yoki vidjetlar ishlatilganda",
        )
        val KK = WidgetText(
            "Уақыт зікірлері", "Келесі намаз", "Намаз уақыттары", "Һижри күні",
            "Зікір және тәспих", "Жұма сүннеттері", "Арафа күні", "Ауызашар дұғасы", "Түннің соңғы үштен бірі", "Жұма түні", "Соңғы он түн",
            "%s дейін", "Келесі", "қалды", "Уақыттарыңыз осында шығуы үшін Мизанды бір рет ашыңыз", "Бүгін Кәһф сүресін оқыңыз",
            "Келесі намаз және кері санақ", "Күннің алты уақыты, келесісі ерекшеленген", "Һижри және григориан күні",
            "Күн сайын мағынасымен қысқа аят", "Уақытқа сай зікірлер: таң, кеш, ұйқы, намаздан кейін және жұма",
            "Кім жұма күні Кәһф сүресін оқыса, оған екі жұманың арасында нұр жарқырайды.",
            "Күндеріңіздің ең абзалдарының бірі — жұма… сол күні маған көп салауат айтыңдар, өйткені салауаттарың маған жеткізіледі.",
            "Жұма күні бір сағат бар: намазда тұрған мұсылман пенде сол сәтте Алладан бір нәрсе сұраса, Ол оны міндетті түрде береді.",
            after = "Намаздан кейінгі зікірлер",
            allowLocation = "Уақыттарыңыз шығуы үшін Мизан виджеттеріне орналасуға рұқсат беріңіз",
            locationHint = "Параметрлер › Мизан › Орналасу: қолданба не виджеттер қолданылғанда",
        )

        private val ALL = mapOf(
            "ar" to AR, "en" to EN, "fr" to FR, "tr" to TR, "ur" to UR, "id" to ID, "ms" to MS, "fa" to FA,
            "bn" to BN, "ru" to RU, "de" to DE, "es" to ES, "uk" to UK, "uz" to UZ, "kk" to KK,
        )
    }
}
