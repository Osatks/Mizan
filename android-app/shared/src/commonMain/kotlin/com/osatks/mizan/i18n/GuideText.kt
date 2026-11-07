package com.osatks.mizan.i18n

/**
 * صفحات دليل التطبيق الجديدة: ما قد يفوت المستخدم (الأدوات، وحديث اليوم، والمشاركة قصةً، والأذان الهادئ،
 * وفي iPhone: شاشة القفل والجزيرة الديناميكية، وزر الإجراء ومركز التحكم، وApple Watch) بكل لغات التطبيق،
 * مستقلة عن Strings كما في FeatureText حتى لا تتغير حزم اللغات.
 * widgetsIos و widgetsAndroid: طريقة إضافة الأدوات تختلف بين النظامين.
 */
class GuideText(
    val widgetsTitle: String, val widgetsIos: String, val widgetsAndroid: String,
    val hadithTitle: String, val hadithBody: String,
    val shareTitle: String, val shareBody: String,
    val adhanTitle: String, val adhanBody: String,
    val liveTitle: String, val liveBody: String,
    val actionTitle: String, val actionBody: String,
    val watchTitle: String, val watchBody: String,
) {
    /** الصفحات (العنوان والنص) بالترتيب لنظام التشغيل: صفحات iPhone الثلاث في iOS وحده */
    fun pages(ios: Boolean): List<Pair<String, String>> = buildList {
        add(widgetsTitle to if (ios) widgetsIos else widgetsAndroid)
        add(hadithTitle to hadithBody)
        add(shareTitle to shareBody)
        if (ios) {
            add(liveTitle to liveBody)
            add(actionTitle to actionBody)
            add(watchTitle to watchBody)
        }
        add(adhanTitle to adhanBody)
    }

    companion object {
        fun of(code: String): GuideText = ALL[code] ?: EN

        val AR = GuideText(
            "أدوات على شاشتك الرئيسية",
            "الصلاة القادمة بعدّ تنازلي حي، ومواقيت اليوم، وأذكار الوقت، وآية اليوم وحديث اليوم. اضغط مطولاً على الشاشة الرئيسية، ثم «تعديل» ← «إضافة أداة»، وابحث عن ميزان.",
            "الصلاة القادمة بعدّ تنازلي حي، ومواقيت اليوم، وأذكار الوقت، وآية اليوم وحديث اليوم. اضغط مطولاً على الشاشة الرئيسية، ثم «الأدوات»، وابحث عن ميزان.",
            "حديث اليوم",
            "حديث صحيح كل يوم مع شرحه وفوائده من موسوعة الأحاديث النبوية (HadeethEnc.com). والمس آية اليوم لتقرأ تفسيرها.",
            "شاركها قصةً",
            "شارك أي آية أو حديث أو ذكر صورةً أنيقة بمقاس القصة، ومعها المعنى بلغتك. اضغط زر المشاركة ثم اختر «قصة».",
            "أذان هادئ",
            "يُسمع في الإشعار التكبيرتان الأوليان ثم يخفت الصوت بلطف. افتح ميزان ليكمل الأذان إلى آخره.",
            "شاشة القفل والجزيرة الديناميكية",
            "العدّ التنازلي للصلاة القادمة وعدد تسبيحك على شاشة القفل وفي الجزيرة الديناميكية. حين يسألك iPhone اضغط «السماح»، ويمكنك إخفاؤها متى شئت من الإعدادات.",
            "زر الإجراء ومركز التحكم",
            "سبّح بزر الإجراء دون فتح التطبيق (الإعدادات ← زر الإجراء ← اختصار ← عدّ التسبيح). وأضف أزرار ميزان إلى مركز التحكم، أو اطلب آية اليوم من Siri.",
            "ميزان على Apple Watch",
            "مواقيت الصلاة والقبلة وأذكار الوقت والمسبحة على معصمك، ومعها إضافات لواجهة الساعة. ثبّته من تطبيق Watch في iPhone.",
        )

        val EN = GuideText(
            "Widgets for your Home Screen",
            "The next prayer with a live countdown, today's times, adhkar for the moment, and the ayah and hadith of the day. Touch and hold the Home Screen, tap Edit › Add Widget, and search for Mizan.",
            "The next prayer with a live countdown, today's times, adhkar for the moment, and the ayah and hadith of the day. Touch and hold the Home Screen, tap Widgets, and search for Mizan.",
            "Hadith of the day",
            "An authentic hadith every day with its explanation and lessons, from HadeethEnc.com. Tap the ayah of the day to read its tafsir.",
            "Share it as a story",
            "Share any ayah, hadith or dhikr as an elegant story-sized image, with the meaning in your language. Tap share, then choose Story.",
            "A gentle adhan",
            "The notification plays the first two takbirs, then the sound fades softly. Open Mizan and the adhan continues to the end.",
            "Lock Screen and Dynamic Island",
            "See the countdown to the next prayer and your tasbih count on the Lock Screen and in the Dynamic Island. When iPhone asks, tap Allow. You can hide them anytime in Settings.",
            "Action Button and Control Center",
            "Count tasbih with the Action Button without opening the app (Settings › Action Button › Shortcut › Count tasbih). Add Mizan controls to Control Center, or ask Siri for the ayah of the day.",
            "Mizan on Apple Watch",
            "Prayer times, the qibla, adhkar for the moment and a tasbih counter on your wrist, with complications for your watch face. Install it from the Watch app on your iPhone.",
        )

        val FR = GuideText(
            "Widgets pour votre écran d’accueil",
            "La prochaine prière avec un compte à rebours en direct, les horaires du jour, les adhkar du moment, ainsi que le verset et le hadith du jour. Maintenez le doigt sur l’écran d’accueil, touchez Modifier › Ajouter un widget, puis recherchez Mizan.",
            "La prochaine prière avec un compte à rebours en direct, les horaires du jour, les adhkar du moment, ainsi que le verset et le hadith du jour. Maintenez le doigt sur l’écran d’accueil, touchez Widgets, puis recherchez Mizan.",
            "Hadith du jour",
            "Chaque jour, un hadith authentique avec son explication et ses enseignements, tiré de HadeethEnc.com. Touchez le verset du jour pour lire son tafsir.",
            "Partagez-le en story",
            "Partagez un verset, un hadith ou un dhikr sous forme d’image élégante au format story, avec le sens dans votre langue. Touchez Partager, puis choisissez Story.",
            "Un adhan tout en douceur",
            "La notification fait entendre les deux premiers takbirs, puis le son s’estompe doucement. Ouvrez Mizan pour écouter l’adhan jusqu’au bout.",
            "Écran verrouillé et Dynamic Island",
            "Le compte à rebours jusqu’à la prochaine prière et votre compteur de tasbih, sur l’écran verrouillé et dans la Dynamic Island. Quand l’iPhone vous le demande, touchez Autoriser. Vous pouvez les masquer à tout moment dans Réglages.",
            "Bouton Action et centre de contrôle",
            "Faites votre tasbih avec le bouton Action sans ouvrir l’app (Réglages › Bouton Action › Raccourci › Compter le tasbih). Ajoutez les commandes Mizan au centre de contrôle, ou demandez le verset du jour à Siri.",
            "Mizan sur Apple Watch",
            "Les horaires de prière, la qibla, les adhkar du moment et un compteur de tasbih à votre poignet, avec des complications pour votre cadran. Installez Mizan depuis l’app Watch de votre iPhone.",
        )

        val TR = GuideText(
            "Ana ekranınız için araç takımları",
            "Canlı geri sayımla bir sonraki namaz, günün vakitleri, vaktin zikirleri, günün ayeti ve hadisi. Ana ekrana dokunup basılı tutun, Düzenle › Araç Takımı Ekle'ye dokunun ve Mizan'ı arayın.",
            "Canlı geri sayımla bir sonraki namaz, günün vakitleri, vaktin zikirleri, günün ayeti ve hadisi. Ana ekrana dokunup basılı tutun, Widget'lar seçeneğine dokunun ve Mizan'ı arayın.",
            "Günün hadisi",
            "Her gün açıklaması ve faydalarıyla birlikte sahih bir hadis, HadeethEnc.com'dan. Tefsirini okumak için günün ayetine dokunun.",
            "Hikâye olarak paylaşın",
            "Herhangi bir ayeti, hadisi veya zikri, kendi dilinizdeki anlamıyla birlikte hikâye boyutunda şık bir görsel olarak paylaşın. Paylaş'a dokunun, ardından Hikâye'yi seçin.",
            "Yumuşak bir ezan",
            "Bildirimde ilk iki tekbir okunur, ardından ses yavaşça kısılır. Mizan'ı açın, ezan sonuna kadar devam etsin.",
            "Kilitli Ekran ve Dynamic Island",
            "Bir sonraki namaza kalan süreyi ve tesbih sayınızı Kilitli Ekran'da ve Dynamic Island'da görün. iPhone sorduğunda İzin Ver'e dokunun. Bunları istediğiniz zaman Ayarlar'dan gizleyebilirsiniz.",
            "Eylem Düğmesi ve Denetim Merkezi",
            "Uygulamayı açmadan Eylem Düğmesi ile tesbih çekin (Ayarlar › Eylem Düğmesi › Kestirme › Tesbih say). Mizan denetimlerini Denetim Merkezi'ne ekleyin veya Siri'den günün ayetini isteyin.",
            "Apple Watch'ta Mizan",
            "Namaz vakitleri, kıble, vaktin zikirleri ve tesbih sayacı bileğinizde; saat kadranınız için komplikasyonlarla birlikte. iPhone'unuzdaki Watch uygulamasından yükleyin.",
        )

        val UR = GuideText(
            "آپ کی ہوم اسکرین کے لیے ویجٹس",
            "اگلی نماز براہِ راست الٹی گنتی کے ساتھ، آج کے اوقات، اس وقت کے اذکار، اور آج کی آیت اور آج کی حدیث۔ ہوم اسکرین کو دبائے رکھیں، پھر «ایڈٹ» ← «ویجٹ شامل کریں» پر ٹیپ کریں اور میزان تلاش کریں۔",
            "اگلی نماز براہِ راست الٹی گنتی کے ساتھ، آج کے اوقات، اس وقت کے اذکار، اور آج کی آیت اور آج کی حدیث۔ ہوم اسکرین کو دبائے رکھیں، پھر «ویجٹس» پر ٹیپ کریں اور میزان تلاش کریں۔",
            "آج کی حدیث",
            "ہر روز ایک صحیح حدیث، اس کی تشریح اور فوائد کے ساتھ، HadeethEnc.com سے۔ آج کی آیت پر ٹیپ کر کے اس کی تفسیر پڑھیں۔",
            "اسٹوری کے طور پر شیئر کریں",
            "کوئی بھی آیت، حدیث یا ذکر اسٹوری کے سائز کی خوبصورت تصویر کے طور پر شیئر کریں، ساتھ میں آپ کی زبان میں اس کا مفہوم۔ شیئر کا بٹن دبائیں، پھر «اسٹوری» منتخب کریں۔",
            "دھیمی اذان",
            "اطلاع میں پہلی دو تکبیریں سنائی دیتی ہیں، پھر آواز آہستہ سے مدھم ہو جاتی ہے۔ میزان کھولیں تو اذان آخر تک مکمل ہو گی۔",
            "لاک اسکرین اور ڈائنامک آئی لینڈ",
            "اگلی نماز کی الٹی گنتی اور آپ کی تسبیح کی گنتی لاک اسکرین پر اور ڈائنامک آئی لینڈ میں۔ جب iPhone پوچھے تو «اجازت دیں» پر ٹیپ کریں۔ آپ انہیں جب چاہیں سیٹنگز سے چھپا سکتے ہیں۔",
            "ایکشن بٹن اور کنٹرول سینٹر",
            "ایپ کھولے بغیر ایکشن بٹن سے تسبیح پڑھیں (سیٹنگز ← ایکشن بٹن ← شارٹ کٹ ← تسبیح گنیں)۔ کنٹرول سینٹر میں میزان کے بٹن شامل کریں، یا Siri سے آج کی آیت مانگیں۔",
            "Apple Watch پر میزان",
            "نماز کے اوقات، قبلہ، اس وقت کے اذکار اور تسبیح آپ کی کلائی پر، ساتھ ہی واچ فیس کے لیے کمپلیکیشنز۔ اسے iPhone میں Watch ایپ سے انسٹال کریں۔",
        )

        val ID = GuideText(
            "Widget untuk Layar Utama Anda",
            "Salat berikutnya dengan hitung mundur langsung, jadwal hari ini, zikir sesuai waktunya, serta ayat dan hadis hari ini. Sentuh dan tahan Layar Utama, ketuk Edit › Tambah Widget, lalu cari Mizan.",
            "Salat berikutnya dengan hitung mundur langsung, jadwal hari ini, zikir sesuai waktunya, serta ayat dan hadis hari ini. Sentuh lama Layar Utama, ketuk Widget, lalu cari Mizan.",
            "Hadis hari ini",
            "Satu hadis sahih setiap hari beserta penjelasan dan faedahnya, dari HadeethEnc.com. Ketuk ayat hari ini untuk membaca tafsirnya.",
            "Bagikan sebagai story",
            "Bagikan ayat, hadis, atau zikir apa pun sebagai gambar elegan berukuran story, lengkap dengan maknanya dalam bahasa Anda. Ketuk bagikan, lalu pilih Story.",
            "Azan yang lembut",
            "Notifikasi memperdengarkan dua takbir pertama, lalu suaranya memelan dengan lembut. Buka Mizan, dan azan berlanjut hingga selesai.",
            "Layar Terkunci dan Dynamic Island",
            "Lihat hitung mundur menuju salat berikutnya dan hitungan tasbih Anda di Layar Terkunci dan di Dynamic Island. Saat iPhone bertanya, ketuk Izinkan. Anda dapat menyembunyikannya kapan saja di Pengaturan.",
            "Tombol Tindakan dan Pusat Kontrol",
            "Bertasbihlah dengan Tombol Tindakan tanpa membuka app (Pengaturan › Tombol Tindakan › Pintasan › Hitung tasbih). Tambahkan kontrol Mizan ke Pusat Kontrol, atau minta ayat hari ini kepada Siri.",
            "Mizan di Apple Watch",
            "Jadwal salat, kiblat, zikir sesuai waktunya, dan penghitung tasbih di pergelangan tangan Anda, lengkap dengan komplikasi untuk wajah jam. Instal dari app Watch di iPhone Anda.",
        )

        val MS = GuideText(
            "Widget untuk Skrin Utama anda",
            "Solat seterusnya dengan kiraan detik secara langsung, waktu solat hari ini, zikir mengikut waktu, serta ayat dan hadis hari ini. Sentuh dan tahan Skrin Utama, ketik Edit › Tambah Widget, kemudian cari Mizan.",
            "Solat seterusnya dengan kiraan detik secara langsung, waktu solat hari ini, zikir mengikut waktu, serta ayat dan hadis hari ini. Sentuh dan tahan Skrin Utama, ketik Widget, kemudian cari Mizan.",
            "Hadis hari ini",
            "Satu hadis sahih setiap hari bersama penjelasan dan pengajarannya, daripada HadeethEnc.com. Ketik ayat hari ini untuk membaca tafsirnya.",
            "Kongsi sebagai story",
            "Kongsi sebarang ayat, hadis atau zikir sebagai imej elegan bersaiz story, bersama maknanya dalam bahasa anda. Ketik kongsi, kemudian pilih Story.",
            "Azan yang lembut",
            "Pemberitahuan memainkan dua takbir pertama, kemudian bunyinya perlahan-lahan menjadi sayup. Buka Mizan dan azan akan bersambung hingga habis.",
            "Skrin Kunci dan Dynamic Island",
            "Lihat kiraan detik ke solat seterusnya dan kiraan tasbih anda pada Skrin Kunci dan dalam Dynamic Island. Apabila iPhone bertanya, ketik Benarkan. Anda boleh menyembunyikannya pada bila-bila masa dalam Seting.",
            "Butang Tindakan dan Pusat Kawalan",
            "Bertasbih dengan Butang Tindakan tanpa membuka app (Seting › Butang Tindakan › Pintasan › Kira tasbih). Tambah kawalan Mizan ke Pusat Kawalan, atau minta ayat hari ini daripada Siri.",
            "Mizan pada Apple Watch",
            "Waktu solat, kiblat, zikir mengikut waktu dan pembilang tasbih di pergelangan tangan anda, bersama komplikasi untuk muka jam anda. Pasang daripada app Watch pada iPhone anda.",
        )

        val FA = GuideText(
            "ابزارک‌ها برای صفحهٔ اصلی",
            "نماز بعدی با شمارش معکوس زنده، اوقات امروز، اذکار همین وقت، و آیه و حدیث روز. صفحهٔ اصلی را لمس کنید و نگه دارید، سپس «ویرایش» ← «افزودن ابزارک» را بزنید و میزان را جستجو کنید.",
            "نماز بعدی با شمارش معکوس زنده، اوقات امروز، اذکار همین وقت، و آیه و حدیث روز. صفحهٔ اصلی را لمس کنید و نگه دارید، سپس «ابزارک‌ها» را بزنید و میزان را جستجو کنید.",
            "حدیث امروز",
            "هر روز یک حدیث صحیح با شرح و فواید آن، از HadeethEnc.com. آیهٔ روز را لمس کنید تا تفسیرش را بخوانید.",
            "به‌صورت استوری به اشتراک بگذارید",
            "هر آیه، حدیث یا ذکری را به‌صورت تصویری زیبا در اندازهٔ استوری، همراه با معنایش به زبان خودتان، به اشتراک بگذارید. دکمهٔ اشتراک را بزنید و «استوری» را انتخاب کنید.",
            "اذانی آرام",
            "در اعلان، دو تکبیر نخست پخش می‌شود و سپس صدا به‌آرامی محو می‌شود. میزان را باز کنید تا اذان تا پایان ادامه یابد.",
            "صفحهٔ قفل و داینامیک آیلند",
            "شمارش معکوس تا نماز بعدی و شمار تسبیح شما روی صفحهٔ قفل و در داینامیک آیلند. وقتی iPhone پرسید، «اجازه دادن» را بزنید. هر وقت خواستید می‌توانید آن‌ها را از تنظیمات پنهان کنید.",
            "دکمهٔ اکشن و مرکز کنترل",
            "بدون باز کردن برنامه با دکمهٔ اکشن تسبیح بگویید (تنظیمات ← دکمهٔ اکشن ← میان‌بر ← شمردن تسبیح). کنترل‌های میزان را به مرکز کنترل اضافه کنید یا آیهٔ روز را از Siri بخواهید.",
            "میزان روی Apple Watch",
            "اوقات نماز، قبله، اذکار همین وقت و تسبیح‌شمار روی مچ دست شما، همراه با کامپلیکیشن‌هایی برای صفحهٔ ساعت. آن را از برنامهٔ Watch در iPhone نصب کنید.",
        )

        val BN = GuideText(
            "আপনার হোম স্ক্রিনের জন্য উইজেট",
            "লাইভ কাউন্টডাউনসহ পরবর্তী নামাজ, আজকের সময়সূচি, এই সময়ের আজকার, আর আজকের আয়াত ও আজকের হাদিস। হোম স্ক্রিনে টাচ করে ধরে রাখুন, “এডিট” › “উইজেট যোগ করুন”-এ ট্যাপ করুন, তারপর মিজান খুঁজুন।",
            "লাইভ কাউন্টডাউনসহ পরবর্তী নামাজ, আজকের সময়সূচি, এই সময়ের আজকার, আর আজকের আয়াত ও আজকের হাদিস। হোম স্ক্রিনে টাচ করে ধরে রাখুন, “উইজেট”-এ ট্যাপ করুন, তারপর মিজান খুঁজুন।",
            "আজকের হাদিস",
            "প্রতিদিন একটি সহিহ হাদিস, এর ব্যাখ্যা ও শিক্ষাসহ, HadeethEnc.com থেকে। তাফসির পড়তে আজকের আয়াতে ট্যাপ করুন।",
            "স্টোরি হিসেবে শেয়ার করুন",
            "যেকোনো আয়াত, হাদিস বা জিকির স্টোরির মাপের সুন্দর ছবি হিসেবে শেয়ার করুন, সাথে আপনার ভাষায় এর অর্থ। শেয়ার বাটনে ট্যাপ করুন, তারপর “স্টোরি” বেছে নিন।",
            "কোমল আজান",
            "নোটিফিকেশনে প্রথম দুটি তাকবির শোনা যায়, তারপর আওয়াজ ধীরে ধীরে মিলিয়ে যায়। মিজান খুললে আজান শেষ পর্যন্ত চলবে।",
            "লক স্ক্রিন ও ডায়নামিক আইল্যান্ড",
            "পরবর্তী নামাজের কাউন্টডাউন আর আপনার তাসবিহের সংখ্যা দেখুন লক স্ক্রিনে ও ডায়নামিক আইল্যান্ডে। iPhone জানতে চাইলে “অনুমতি দিন”-এ ট্যাপ করুন। যেকোনো সময় সেটিংস থেকে এগুলো লুকাতে পারবেন।",
            "অ্যাকশন বাটন ও কন্ট্রোল সেন্টার",
            "অ্যাপ না খুলেই অ্যাকশন বাটন দিয়ে তাসবিহ পড়ুন (সেটিংস › অ্যাকশন বাটন › শর্টকাট › তাসবিহ গণনা)। কন্ট্রোল সেন্টারে মিজানের কন্ট্রোল যোগ করুন, অথবা Siri-র কাছে আজকের আয়াত চান।",
            "Apple Watch-এ মিজান",
            "নামাজের সময়, কিবলা, এই সময়ের আজকার আর তাসবিহ কাউন্টার আপনার কবজিতে, সাথে ঘড়ির ফেসের জন্য কমপ্লিকেশন। আপনার iPhone-এর Watch অ্যাপ থেকে এটি ইনস্টল করুন।",
        )

        val RU = GuideText(
            "Виджеты для экрана «Домой»",
            "Ближайший намаз с обратным отсчётом в реальном времени, время намазов на сегодня, зикры на текущее время, а также аят и хадис дня. Коснитесь экрана «Домой» и удерживайте палец, нажмите «Изменить» › «Добавить виджет» и найдите Mizan.",
            "Ближайший намаз с обратным отсчётом в реальном времени, время намазов на сегодня, зикры на текущее время, а также аят и хадис дня. Нажмите и удерживайте главный экран, выберите «Виджеты» и найдите Mizan.",
            "Хадис дня",
            "Каждый день достоверный хадис с пояснением и уроками из него, с сайта HadeethEnc.com. Нажмите на аят дня, чтобы прочитать его тафсир.",
            "Поделитесь в истории",
            "Делитесь любым аятом, хадисом или зикром в виде красивой картинки формата истории со смыслом на вашем языке. Нажмите «Поделиться» и выберите «История».",
            "Мягкий азан",
            "В уведомлении звучат два первых такбира, затем звук плавно затихает. Откройте Mizan, и азан прозвучит до конца.",
            "Экран блокировки и Dynamic Island",
            "Обратный отсчёт до следующего намаза и счёт тасбиха на экране блокировки и в Dynamic Island. Когда iPhone спросит, нажмите «Разрешить». Скрыть их можно в любой момент в Настройках.",
            "Кнопка действия и Пункт управления",
            "Делайте тасбих кнопкой действия, не открывая приложение (Настройки › Кнопка действия › Быстрая команда › Счёт тасбиха). Добавьте элементы Mizan в Пункт управления или попросите у Siri аят дня.",
            "Mizan на Apple Watch",
            "Время намаза, кибла, зикры на текущее время и счётчик тасбиха на вашем запястье, а также расширения для циферблата. Установите Mizan из приложения Watch на iPhone.",
        )

        val DE = GuideText(
            "Widgets für deinen Home-Bildschirm",
            "Das nächste Gebet mit Live-Countdown, die heutigen Gebetszeiten, die Adhkar zur jeweiligen Tageszeit sowie Vers und Hadith des Tages. Halte den Finger auf dem Home-Bildschirm gedrückt, tippe auf Bearbeiten › Widget hinzufügen und suche nach Mizan.",
            "Das nächste Gebet mit Live-Countdown, die heutigen Gebetszeiten, die Adhkar zur jeweiligen Tageszeit sowie Vers und Hadith des Tages. Halte den Finger auf dem Startbildschirm gedrückt, tippe auf Widgets und suche nach Mizan.",
            "Hadith des Tages",
            "Jeden Tag ein authentischer Hadith mit Erläuterung und Lehren, von HadeethEnc.com. Tippe auf den Vers des Tages, um seinen Tafsir zu lesen.",
            "Als Story teilen",
            "Teile jeden Vers, Hadith oder Dhikr als elegantes Bild im Story-Format, mit der Bedeutung in deiner Sprache. Tippe auf Teilen und wähle Story.",
            "Ein sanfter Adhan",
            "Die Mitteilung spielt die ersten beiden Takbire, dann klingt der Ton sanft aus. Öffne Mizan, und der Adhan wird bis zum Ende fortgesetzt.",
            "Sperrbildschirm und Dynamic Island",
            "Sieh den Countdown bis zum nächsten Gebet und deinen Tasbih-Zähler auf dem Sperrbildschirm und in der Dynamic Island. Wenn dein iPhone fragt, tippe auf Erlauben. Du kannst sie jederzeit in den Einstellungen ausblenden.",
            "Aktionstaste und Kontrollzentrum",
            "Zähle Tasbih mit der Aktionstaste, ohne die App zu öffnen (Einstellungen › Aktionstaste › Kurzbefehl › Tasbih zählen). Füge Mizan-Steuerelemente zum Kontrollzentrum hinzu oder bitte Siri um den Vers des Tages.",
            "Mizan auf der Apple Watch",
            "Gebetszeiten, Qibla, Adhkar zur jeweiligen Tageszeit und ein Tasbih-Zähler an deinem Handgelenk, mit Komplikationen für dein Zifferblatt. Installiere Mizan über die Watch-App auf deinem iPhone.",
        )

        val ES = GuideText(
            "Widgets para tu pantalla de inicio",
            "La próxima oración con cuenta regresiva en vivo, los horarios de hoy, los adhkar del momento, y la aya y el hadiz del día. Mantén pulsada la pantalla de inicio, toca Editar › Añadir widget y busca Mizan.",
            "La próxima oración con cuenta regresiva en vivo, los horarios de hoy, los adhkar del momento, y la aya y el hadiz del día. Mantén pulsada la pantalla de inicio, toca Widgets y busca Mizan.",
            "Hadiz del día",
            "Un hadiz auténtico cada día con su explicación y enseñanzas, de HadeethEnc.com. Toca la aya del día para leer su tafsir.",
            "Compártelo como historia",
            "Comparte cualquier aya, hadiz o dhikr como una imagen elegante en formato de historia, con el significado en tu idioma. Toca compartir y elige Historia.",
            "Un adhán suave",
            "La notificación reproduce los dos primeros takbires y luego el sonido se desvanece suavemente. Abre Mizan y el adhán continuará hasta el final.",
            "Pantalla bloqueada y Dynamic Island",
            "Consulta la cuenta regresiva para la próxima oración y tu cuenta de tasbih en la pantalla bloqueada y en la Dynamic Island. Cuando el iPhone te lo pida, toca Permitir. Puedes ocultarlos en cualquier momento en Ajustes.",
            "Botón de acción y centro de control",
            "Cuenta tasbih con el botón de acción sin abrir la app (Ajustes › Botón de acción › Atajo › Contar tasbih). Añade los controles de Mizan al centro de control o pídele a Siri la aya del día.",
            "Mizan en el Apple Watch",
            "Horarios de oración, la qibla, los adhkar del momento y un contador de tasbih en tu muñeca, con complicaciones para tu esfera. Instálalo desde la app Watch del iPhone.",
        )

        val UK = GuideText(
            "Віджети для початкового екрана",
            "Найближчий намаз із живим зворотним відліком, час намазів на сьогодні, зікри на поточний час, а також аят і хадис дня. Торкніться й утримуйте початковий екран, торкніться «Редагувати» › «Додати віджет» і знайдіть Mizan.",
            "Найближчий намаз із живим зворотним відліком, час намазів на сьогодні, зікри на поточний час, а також аят і хадис дня. Торкніться й утримуйте головний екран, виберіть «Віджети» і знайдіть Mizan.",
            "Хадис дня",
            "Щодня достовірний хадис із поясненням і повчаннями, з HadeethEnc.com. Торкніться аята дня, щоб прочитати його тафсир.",
            "Поділіться в історії",
            "Діліться будь-яким аятом, хадисом чи зікром у вигляді гарного зображення формату історії зі змістом вашою мовою. Торкніться «Поділитися» й виберіть «Історія».",
            "Лагідний азан",
            "У сповіщенні звучать два перші такбіри, а потім звук плавно стихає. Відкрийте Mizan, і азан пролунає до кінця.",
            "Замкнений екран і Dynamic Island",
            "Зворотний відлік до наступного намазу й лічба тасбіху на замкненому екрані та в Dynamic Island. Коли iPhone запитає, торкніться «Дозволити». Приховати їх можна будь-коли в Параметрах.",
            "Кнопка дії і Центр керування",
            "Робіть тасбіх кнопкою дії, не відкриваючи програму (Параметри › Кнопка дії › Швидка команда › Лічба тасбіху). Додайте елементи Mizan до Центру керування або попросіть у Siri аят дня.",
            "Mizan на Apple Watch",
            "Час намазу, кібла, зікри на поточний час і лічильник тасбіху на вашому зап’ясті, а також ускладнення для циферблата. Установіть Mizan з програми Watch на iPhone.",
        )

        val UZ = GuideText(
            "Asosiy ekraningiz uchun vidjetlar",
            "Jonli teskari sanoqli navbatdagi namoz, bugungi vaqtlar, shu vaqt zikrlari, kun oyati va kun hadisi. Asosiy ekranni bosib turing, “Tahrirlash” › “Vidjet qo‘shish”ni bosing va Mizanni qidiring.",
            "Jonli teskari sanoqli navbatdagi namoz, bugungi vaqtlar, shu vaqt zikrlari, kun oyati va kun hadisi. Asosiy ekranni bosib turing, “Vidjetlar”ni bosing va Mizanni qidiring.",
            "Kun hadisi",
            "Har kuni sharhi va foydalari bilan bitta sahih hadis, HadeethEnc.com saytidan. Tafsirini o‘qish uchun kun oyatini bosing.",
            "Hikoya sifatida ulashing",
            "Istalgan oyat, hadis yoki zikrni o‘z tilingizdagi ma’nosi bilan hikoya o‘lchamidagi chiroyli rasm sifatida ulashing. Ulashish tugmasini bosing va “Hikoya”ni tanlang.",
            "Mayin azon",
            "Bildirishnomada dastlabki ikki takbir eshitiladi, so‘ng ovoz asta pasayadi. Mizanni oching, azon oxirigacha davom etadi.",
            "Qulf ekrani va Dynamic Island",
            "Navbatdagi namozgacha teskari sanoq va tasbeh hisobingiz qulf ekranida va Dynamic Island’da. iPhone so‘raganda “Ruxsat berish”ni bosing. Ularni istalgan vaqtda Sozlamalarda yashirishingiz mumkin.",
            "Amal tugmasi va Boshqaruv markazi",
            "Ilovani ochmasdan Amal tugmasi bilan tasbeh ayting (Sozlamalar › Amal tugmasi › Qisqa buyruq › Tasbeh sanash). Mizan boshqaruv elementlarini Boshqaruv markaziga qo‘shing yoki Siri’dan kun oyatini so‘rang.",
            "Apple Watch’da Mizan",
            "Namoz vaqtlari, qibla, shu vaqt zikrlari va tasbeh hisoblagichi bilagingizda, siferblat uchun vidjetlar bilan birga. iPhone’dagi Watch ilovasidan o‘rnating.",
        )

        val KK = GuideText(
            "Басты экранға арналған виджеттер",
            "Тікелей кері санағы бар келесі намаз, бүгінгі уақыттар, осы уақыттың зікірлері, күннің аяты мен хадисі. Басты экранды басып тұрыңыз, «Өңдеу» › «Виджет қосу» түймесін түртіп, Mizan-ды іздеңіз.",
            "Тікелей кері санағы бар келесі намаз, бүгінгі уақыттар, осы уақыттың зікірлері, күннің аяты мен хадисі. Басты экранды басып тұрыңыз, «Виджеттер» бөлімін түртіп, Mizan-ды іздеңіз.",
            "Күннің хадисі",
            "Күн сайын түсіндірмесі мен пайдалары бар бір сахих хадис, HadeethEnc.com сайтынан. Тәпсірін оқу үшін күннің аятын түртіңіз.",
            "Сторис ретінде бөлісіңіз",
            "Кез келген аятты, хадисті немесе зікірді өз тіліңіздегі мағынасымен бірге сторис өлшеміндегі әдемі сурет ретінде бөлісіңіз. Бөлісу түймесін түртіп, «Сторис» тармағын таңдаңыз.",
            "Жұмсақ азан",
            "Хабарландыруда алғашқы екі тәкбір естіледі, содан кейін дыбыс біртіндеп бәсеңдейді. Mizan-ды ашсаңыз, азан соңына дейін жалғасады.",
            "Құлып экраны және Dynamic Island",
            "Келесі намазға дейінгі кері санақ пен тәспі санағыңыз құлып экранында және Dynamic Island-та. iPhone сұрағанда «Рұқсат ету» түймесін түртіңіз. Оларды кез келген уақытта Баптаулардан жасыра аласыз.",
            "Әрекет түймесі және Басқару орталығы",
            "Қолданбаны ашпай-ақ Әрекет түймесімен тәспі тартыңыз (Баптаулар › Әрекет түймесі › Жылдам пәрмен › Тәспі санау). Mizan басқару элементтерін Басқару орталығына қосыңыз немесе Siri-ден күннің аятын сұраңыз.",
            "Apple Watch-тағы Mizan",
            "Намаз уақыттары, құбыла, осы уақыттың зікірлері және тәспі санағышы білегіңізде, циферблатқа арналған кеңейтімдермен бірге. Оны iPhone-дағы Watch қолданбасынан орнатыңыз.",
        )

        val ALL: Map<String, GuideText> = mapOf(
            "ar" to AR, "en" to EN, "fr" to FR, "tr" to TR, "ur" to UR, "id" to ID, "ms" to MS, "fa" to FA,
            "bn" to BN, "ru" to RU, "de" to DE, "es" to ES, "uk" to UK, "uz" to UZ, "kk" to KK,
        )
    }
}
