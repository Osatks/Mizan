package com.osatks.mizan.i18n

/**
 * نصوص الميزات الجديدة (مشاركة الآية صورة، ومواقيت الصلاة في تقويم iPhone، والقبلة بالكاميرا) بكل لغات التطبيق،
 * مستقلة عن Strings كما في LiveText حتى لا تتغير حزم اللغات. ما له نص في Strings يُؤخذ منها (مشاركة، فتح الإعدادات…).
 */
class FeatureText(
    val shareAyah: String,        // عنوان لوحة المشاركة
    val image: String,            // زر: مشاركة صورة
    val text: String,             // زر: مشاركة نص
    val withMeaning: String,      // مفتاح: المعنى تحت الآية في الصورة
    val addToCalendar: String,    // زر في شاشة المواقيت (iOS)
    val calendarDesc: String,     // شرح تحت الزر
    val calendarAdded: String,    // بعد الإضافة
    val removeFromCalendar: String,
    val calendarRemoved: String,
    val calendarDenied: String,   // لم يُسمح بالوصول إلى التقويم
    val calendarName: String,     // اسم التقويم في تطبيق التقويم
    val arQibla: String,          // زر في شاشة القبلة: القبلة بالكاميرا
    val arHint: String,           // قبل أن يُعرف الاتجاه
    val arFacing: String,         // الكاميرا نحو القبلة
    val arTurn: String,           // «استدر نحو السهم»
    val cameraDenied: String,     // لم يُسمح باستعمال الكاميرا
    val arUnsupported: String,    // الجهاز لا يدعم الواقع المعزز
) {
    companion object {
        fun of(code: String): FeatureText = ALL[code] ?: EN

        val AR = FeatureText(
            "مشاركة الآية", "صورة", "نص", "مع المعنى",
            "إضافة المواقيت إلى التقويم", "مواقيت ثلاثين يوماً في تطبيق التقويم، وتتجدد كلما فتحت ميزان",
            "أُضيفت المواقيت إلى التقويم", "إزالة المواقيت من التقويم", "أُزيلت المواقيت من التقويم",
            "اسمح لميزان بالوصول إلى التقويم من الإعدادات", "مواقيت الصلاة · ميزان",
            "القبلة بالكاميرا", "حرّك الهاتف ببطء حتى يظهر اتجاه القبلة", "أنت تتجه إلى القبلة", "استدر نحو السهم",
            "اسمح لميزان باستعمال الكاميرا من الإعدادات", "هذا الجهاز لا يدعم الواقع المعزز",
        )
        val EN = FeatureText(
            "Share ayah", "Image", "Text", "With meaning",
            "Add prayer times to Calendar", "Thirty days of prayer times in the Calendar app, renewed whenever you open Mizan",
            "Prayer times added to Calendar", "Remove prayer times from Calendar", "Prayer times removed from Calendar",
            "Allow Mizan to access Calendar in Settings", "Prayer times · Mizan",
            "Qibla in camera", "Move your phone slowly until the qibla direction appears", "You are facing the qibla", "Turn toward the arrow",
            "Allow Mizan to use the camera in Settings", "This device does not support augmented reality",
        )
        val FR = FeatureText(
            "Partager le verset", "Image", "Texte", "Avec le sens",
            "Ajouter les horaires au Calendrier", "Trente jours d’horaires de prière dans l’app Calendrier, renouvelés à chaque ouverture de Mizan",
            "Horaires ajoutés au Calendrier", "Retirer les horaires du Calendrier", "Horaires retirés du Calendrier",
            "Autorisez Mizan à accéder au Calendrier dans Réglages", "Horaires de prière · Mizan",
            "Qibla à la caméra", "Déplacez lentement votre téléphone jusqu’à voir la direction de la qibla", "Vous êtes face à la qibla", "Tournez-vous vers la flèche",
            "Autorisez Mizan à utiliser la caméra dans Réglages", "Cet appareil ne prend pas en charge la réalité augmentée",
        )
        val TR = FeatureText(
            "Ayeti paylaş", "Görsel", "Metin", "Mealiyle",
            "Namaz vakitlerini Takvim'e ekle", "Otuz günlük namaz vakitleri Takvim uygulamasında; Mizan'ı her açtığınızda yenilenir",
            "Vakitler Takvim'e eklendi", "Vakitleri Takvim'den kaldır", "Vakitler Takvim'den kaldırıldı",
            "Ayarlar'dan Mizan'ın Takvim'e erişmesine izin verin", "Namaz vakitleri · Mizan",
            "Kamerayla kıble", "Kıble yönü görünene kadar telefonu yavaşça hareket ettirin", "Kıbleye dönüksünüz", "Oka doğru dönün",
            "Ayarlar'dan Mizan'ın kamerayı kullanmasına izin verin", "Bu cihaz artırılmış gerçekliği desteklemiyor",
        )
        val UR = FeatureText(
            "آیت شیئر کریں", "تصویر", "متن", "ترجمے کے ساتھ",
            "نماز کے اوقات کیلنڈر میں شامل کریں", "تیس دن کے اوقاتِ نماز کیلنڈر ایپ میں، ہر بار میزان کھولنے پر تازہ",
            "اوقات کیلنڈر میں شامل ہو گئے", "اوقات کیلنڈر سے ہٹائیں", "اوقات کیلنڈر سے ہٹا دیے گئے",
            "سیٹنگز میں میزان کو کیلنڈر تک رسائی دیں", "اوقاتِ نماز · میزان",
            "کیمرے سے قبلہ", "قبلے کی سمت ظاہر ہونے تک فون آہستہ سے گھمائیں", "آپ کا رخ قبلے کی طرف ہے", "تیر کی طرف مڑیں",
            "سیٹنگز میں میزان کو کیمرا استعمال کرنے دیں", "یہ ڈیوائس آگمینٹڈ ریئلٹی کو سپورٹ نہیں کرتی",
        )
        val ID = FeatureText(
            "Bagikan ayat", "Gambar", "Teks", "Dengan terjemahan",
            "Tambahkan waktu salat ke Kalender", "Waktu salat tiga puluh hari di app Kalender, diperbarui setiap kali Anda membuka Mizan",
            "Waktu salat ditambahkan ke Kalender", "Hapus waktu salat dari Kalender", "Waktu salat dihapus dari Kalender",
            "Izinkan Mizan mengakses Kalender di Pengaturan", "Waktu salat · Mizan",
            "Kiblat dengan kamera", "Gerakkan ponsel perlahan hingga arah kiblat muncul", "Anda menghadap kiblat", "Putar ke arah panah",
            "Izinkan Mizan menggunakan kamera di Pengaturan", "Perangkat ini tidak mendukung augmented reality",
        )
        val MS = FeatureText(
            "Kongsi ayat", "Gambar", "Teks", "Dengan terjemahan",
            "Tambah waktu solat ke Kalendar", "Waktu solat tiga puluh hari dalam app Kalendar, dikemas kini setiap kali anda membuka Mizan",
            "Waktu solat ditambah ke Kalendar", "Buang waktu solat dari Kalendar", "Waktu solat dibuang dari Kalendar",
            "Benarkan Mizan mengakses Kalendar dalam Seting", "Waktu solat · Mizan",
            "Kiblat dengan kamera", "Gerakkan telefon perlahan sehingga arah kiblat muncul", "Anda menghadap kiblat", "Pusing ke arah anak panah",
            "Benarkan Mizan menggunakan kamera dalam Seting", "Peranti ini tidak menyokong realiti terimbuh",
        )
        val FA = FeatureText(
            "اشتراک آیه", "تصویر", "متن", "با ترجمه",
            "افزودن اوقات نماز به تقویم", "اوقات نماز سی روز در برنامهٔ تقویم، هر بار که میزان را باز کنید تازه می‌شود",
            "اوقات به تقویم افزوده شد", "حذف اوقات از تقویم", "اوقات از تقویم حذف شد",
            "در تنظیمات به میزان اجازهٔ دسترسی به تقویم بدهید", "اوقات نماز · میزان",
            "قبله با دوربین", "گوشی را آرام حرکت دهید تا جهت قبله نمایان شود", "رو به قبله هستید", "به سمت پیکان بچرخید",
            "در تنظیمات به میزان اجازهٔ استفاده از دوربین بدهید", "این دستگاه از واقعیت افزوده پشتیبانی نمی‌کند",
        )
        val BN = FeatureText(
            "আয়াত শেয়ার করুন", "ছবি", "লেখা", "অর্থসহ",
            "নামাজের সময় ক্যালেন্ডারে যোগ করুন", "ক্যালেন্ডার অ্যাপে ত্রিশ দিনের নামাজের সময়, প্রতিবার মিজান খুললে হালনাগাদ",
            "নামাজের সময় ক্যালেন্ডারে যোগ হয়েছে", "ক্যালেন্ডার থেকে নামাজের সময় সরান", "ক্যালেন্ডার থেকে নামাজের সময় সরানো হয়েছে",
            "সেটিংসে মিজানকে ক্যালেন্ডার ব্যবহারের অনুমতি দিন", "নামাজের সময় · মিজান",
            "ক্যামেরায় কিবলা", "কিবলার দিক না দেখা পর্যন্ত ফোন ধীরে নাড়ান", "আপনি কিবলামুখী", "তীরের দিকে ঘুরুন",
            "সেটিংসে মিজানকে ক্যামেরা ব্যবহারের অনুমতি দিন", "এই ডিভাইস অগমেন্টেড রিয়েলিটি সমর্থন করে না",
        )
        val RU = FeatureText(
            "Поделиться аятом", "Картинка", "Текст", "С переводом смысла",
            "Добавить время намаза в Календарь", "Время намаза на тридцать дней в приложении «Календарь», обновляется при каждом открытии Mizan",
            "Время намаза добавлено в Календарь", "Убрать время намаза из Календаря", "Время намаза убрано из Календаря",
            "Разрешите Mizan доступ к Календарю в Настройках", "Время намаза · Mizan",
            "Кибла в камере", "Медленно поводите телефоном, пока не появится направление киблы", "Вы смотрите в сторону киблы", "Повернитесь к стрелке",
            "Разрешите Mizan использовать камеру в Настройках", "Это устройство не поддерживает дополненную реальность",
        )
        val DE = FeatureText(
            "Vers teilen", "Bild", "Text", "Mit Bedeutung",
            "Gebetszeiten zum Kalender hinzufügen", "Gebetszeiten für dreißig Tage in der Kalender-App, erneuert bei jedem Öffnen von Mizan",
            "Gebetszeiten zum Kalender hinzugefügt", "Gebetszeiten aus dem Kalender entfernen", "Gebetszeiten aus dem Kalender entfernt",
            "Erlaube Mizan in den Einstellungen den Zugriff auf den Kalender", "Gebetszeiten · Mizan",
            "Qibla mit der Kamera", "Bewege dein Telefon langsam, bis die Qibla-Richtung erscheint", "Du blickst zur Qibla", "Drehe dich zum Pfeil",
            "Erlaube Mizan in den Einstellungen die Nutzung der Kamera", "Dieses Gerät unterstützt keine erweiterte Realität",
        )
        val ES = FeatureText(
            "Compartir aleya", "Imagen", "Texto", "Con el significado",
            "Añadir los horarios al Calendario", "Treinta días de horarios de oración en la app Calendario, renovados cada vez que abres Mizan",
            "Horarios añadidos al Calendario", "Quitar los horarios del Calendario", "Horarios quitados del Calendario",
            "Permite que Mizan acceda al Calendario en Ajustes", "Horarios de oración · Mizan",
            "Qibla con la cámara", "Mueve el teléfono despacio hasta que aparezca la dirección de la qibla", "Estás orientado a la qibla", "Gira hacia la flecha",
            "Permite que Mizan use la cámara en Ajustes", "Este dispositivo no admite realidad aumentada",
        )
        val UK = FeatureText(
            "Поділитися аятом", "Зображення", "Текст", "Зі змістом",
            "Додати час намазу до Календаря", "Час намазу на тридцять днів у програмі «Календар», оновлюється щоразу, коли ви відкриваєте Mizan",
            "Час намазу додано до Календаря", "Прибрати час намазу з Календаря", "Час намазу прибрано з Календаря",
            "Дозвольте Mizan доступ до Календаря в Параметрах", "Час намазу · Mizan",
            "Кибла в камері", "Повільно поводьте телефоном, доки не з’явиться напрямок кибли", "Ви дивитеся в бік кибли", "Поверніться до стрілки",
            "Дозвольте Mizan використовувати камеру в Параметрах", "Цей пристрій не підтримує доповнену реальність",
        )
        val UZ = FeatureText(
            "Oyatni ulashish", "Rasm", "Matn", "Ma’nosi bilan",
            "Namoz vaqtlarini Taqvimga qo‘shish", "O‘ttiz kunlik namoz vaqtlari Taqvim ilovasida, Mizan har ochilganda yangilanadi",
            "Namoz vaqtlari Taqvimga qo‘shildi", "Namoz vaqtlarini Taqvimdan olib tashlash", "Namoz vaqtlari Taqvimdan olib tashlandi",
            "Sozlamalarda Mizanga Taqvimdan foydalanishga ruxsat bering", "Namoz vaqtlari · Mizan",
            "Kamerada qibla", "Qibla yo‘nalishi ko‘ringuncha telefonni sekin harakatlantiring", "Siz qiblaga qaragansiz", "Strelka tomonga buriling",
            "Sozlamalarda Mizanga kameradan foydalanishga ruxsat bering", "Bu qurilma kengaytirilgan reallikni qo‘llab-quvvatlamaydi",
        )
        val KK = FeatureText(
            "Аятты бөлісу", "Сурет", "Мәтін", "Мағынасымен",
            "Намаз уақыттарын Күнтізбеге қосу", "Отыз күндік намаз уақыттары Күнтізбе қолданбасында, Mizan-ды әр ашқанда жаңарады",
            "Намаз уақыттары Күнтізбеге қосылды", "Намаз уақыттарын Күнтізбеден алып тастау", "Намаз уақыттары Күнтізбеден алынды",
            "Баптауларда Mizan-ға Күнтізбені пайдалануға рұқсат беріңіз", "Намаз уақыттары · Mizan",
            "Камерадағы құбыла", "Құбыла бағыты көрінгенше телефонды баяу қозғаңыз", "Сіз құбылаға қарап тұрсыз", "Көрсеткіге қарай бұрылыңыз",
            "Баптауларда Mizan-ға камераны пайдалануға рұқсат беріңіз", "Бұл құрылғы толықтырылған шындықты қолдамайды",
        )

        val ALL: Map<String, FeatureText> = mapOf(
            "ar" to AR, "en" to EN, "fr" to FR, "tr" to TR, "ur" to UR, "id" to ID, "ms" to MS, "fa" to FA,
            "bn" to BN, "ru" to RU, "de" to DE, "es" to ES, "uk" to UK, "uz" to UZ, "kk" to KK,
        )
    }
}
