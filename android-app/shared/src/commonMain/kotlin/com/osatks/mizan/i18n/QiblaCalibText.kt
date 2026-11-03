package com.osatks.mizan.i18n

/**
 * معايرة البوصلة بلغات التطبيق (شاشة القبلة، والقبلة بالكاميرا في iOS): العنوان، والخطوات المشتركة، وخطوة النظام
 * (iOS: «معايرة البوصلة» في خدمات النظام؛ أندرويد: الدقة العالية للموقع)، وزر الإغلاق.
 */
class QiblaCalibText(val title: String, val steps: List<String>, val ios: String, val android: String, val done: String, val reset: String) {
    fun all(platform: String): List<String> = steps + (if (platform == "ios") ios else android)

    companion object {
        fun of(code: String): QiblaCalibText = when (code) {
            "ar" -> QiblaCalibText("معايرة البوصلة", listOf(
                "حرّك الهاتف ببطء على شكل الرقم 8 في الهواء ثلاث مرات، مع تدويره في كل الاتجاهات.",
                "ابتعد عن المعادن والمغانط والأجهزة الكهربائية والسيارة، وانزع الغطاء المغناطيسي أو حامل المغناطيس.",
                "أمسك الهاتف مستوياً أمامك بعيداً عن جسمك، وانتظر ثوانيَ حتى تستقر الإبرة.",
            ), "تأكد أن «معايرة البوصلة» مفعّلة: الإعدادات ← الخصوصية والأمن ← خدمات الموقع ← خدمات النظام.",
                "فعّل «الدقة العالية» للموقع من إعدادات الموقع في الهاتف لتحسين الشمال الجغرافي.", "تم", "إعادة الضبط")
            "fr" -> QiblaCalibText("Calibrer la boussole", listOf(
                "Bougez lentement le téléphone en dessinant un 8 dans l’air trois fois, en le tournant dans tous les sens.",
                "Éloignez-vous du métal, des aimants, des appareils électriques et de la voiture ; retirez toute coque magnétique.",
                "Tenez le téléphone à plat devant vous, loin du corps, et attendez quelques secondes que l’aiguille se stabilise.",
            ), "Vérifiez que « Calibrage de la boussole » est activé : Réglages ← Confidentialité et sécurité ← Service de localisation ← Services système.",
                "Activez la « haute précision » de la localisation dans les réglages du téléphone.", "Terminé", "Réinitialiser")
            "tr" -> QiblaCalibText("Pusulayı kalibre et", listOf(
                "Telefonu havada yavaşça üç kez 8 çizecek şekilde, her yöne çevirerek hareket ettirin.",
                "Metal, mıknatıs, elektrikli cihaz ve arabadan uzaklaşın; mıknatıslı kılıfı çıkarın.",
                "Telefonu önünüzde, vücudunuzdan uzakta düz tutun ve iğne oturana kadar birkaç saniye bekleyin.",
            ), "«Pusula Kalibrasyonu» açık olsun: Ayarlar ← Gizlilik ve Güvenlik ← Konum Servisleri ← Sistem Servisleri.",
                "Telefonun konum ayarlarında «yüksek doğruluk»u açın.", "Tamam", "Sıfırla")
            "ur" -> QiblaCalibText("قطب نما کی درستگی", listOf(
                "فون کو ہوا میں آہستہ آہستہ تین بار 8 کی شکل میں ہر طرف گھماتے ہوئے حرکت دیں۔",
                "دھات، مقناطیس، برقی آلات اور گاڑی سے دور رہیں، اور مقناطیسی کور اتار دیں۔",
                "فون کو جسم سے دور سامنے سیدھا رکھیں اور سوئی ٹھہرنے تک چند سیکنڈ انتظار کریں۔",
            ), "یقینی بنائیں کہ «Compass Calibration» آن ہے: سیٹنگز ← پرائیویسی اور سیکیورٹی ← لوکیشن سروسز ← سسٹم سروسز۔",
                "فون کی لوکیشن سیٹنگز میں «زیادہ درستگی» آن کریں۔", "ٹھیک ہے", "دوبارہ ترتیب")
            "id" -> QiblaCalibText("Kalibrasi kompas", listOf(
                "Gerakkan ponsel perlahan membentuk angka 8 di udara tiga kali sambil memutarnya ke segala arah.",
                "Jauhi logam, magnet, perangkat listrik, dan mobil; lepas casing bermagnet.",
                "Pegang ponsel mendatar di depan Anda, jauh dari tubuh, dan tunggu beberapa detik hingga jarum stabil.",
            ), "Pastikan «Kalibrasi Kompas» aktif: Pengaturan ← Privasi & Keamanan ← Layanan Lokasi ← Layanan Sistem.",
                "Aktifkan «akurasi tinggi» lokasi di pengaturan ponsel.", "Selesai", "Atur ulang")
            "ms" -> QiblaCalibText("Kalibrasi kompas", listOf(
                "Gerakkan telefon perlahan membentuk angka 8 di udara tiga kali sambil memusingkannya ke semua arah.",
                "Jauhi logam, magnet, peranti elektrik dan kereta; tanggalkan sarung bermagnet.",
                "Pegang telefon mendatar di hadapan anda, jauh dari badan, dan tunggu beberapa saat sehingga jarum stabil.",
            ), "Pastikan «Kalibrasi Kompas» dihidupkan: Tetapan ← Privasi & Keselamatan ← Perkhidmatan Lokasi ← Perkhidmatan Sistem.",
                "Hidupkan «ketepatan tinggi» lokasi dalam tetapan telefon.", "Selesai", "Set semula")
            "fa" -> QiblaCalibText("کالیبره کردن قطب‌نما", listOf(
                "گوشی را آرام سه بار در هوا به شکل عدد ۸ حرکت دهید و آن را به همه جهت‌ها بچرخانید.",
                "از فلز، آهن‌ربا، وسایل برقی و خودرو دور شوید و قاب مغناطیسی را بردارید.",
                "گوشی را صاف و دور از بدن جلوی خود نگه دارید و چند ثانیه صبر کنید تا عقربه آرام بگیرد.",
            ), "مطمئن شوید «کالیبراسیون قطب‌نما» روشن است: تنظیمات ← حریم خصوصی و امنیت ← خدمات مکان ← خدمات سیستم.",
                "«دقت بالا»ی مکان را در تنظیمات گوشی روشن کنید.", "تمام", "بازنشانی")
            "bn" -> QiblaCalibText("কম্পাস ক্যালিব্রেট করুন", listOf(
                "ফোনটি ধীরে ধীরে বাতাসে তিনবার ৮ আকারে নাড়ুন, সব দিকে ঘুরিয়ে।",
                "ধাতু, চুম্বক, বৈদ্যুতিক যন্ত্র ও গাড়ি থেকে দূরে থাকুন; চুম্বকযুক্ত কভার খুলে ফেলুন।",
                "ফোনটি শরীর থেকে দূরে সামনে সমান করে ধরুন এবং কাঁটা স্থির হওয়া পর্যন্ত কয়েক সেকেন্ড অপেক্ষা করুন।",
            ), "«Compass Calibration» চালু আছে কিনা দেখুন: সেটিংস ← গোপনীয়তা ও নিরাপত্তা ← লোকেশন সার্ভিসেস ← সিস্টেম সার্ভিসেস।",
                "ফোনের লোকেশন সেটিংসে «উচ্চ নির্ভুলতা» চালু করুন।", "হয়েছে", "রিসেট")
            "ru" -> QiblaCalibText("Калибровка компаса", listOf(
                "Медленно проведите телефоном в воздухе восьмёрку три раза, поворачивая его во все стороны.",
                "Отойдите от металла, магнитов, электроприборов и машины; снимите магнитный чехол.",
                "Держите телефон ровно перед собой, подальше от тела, и подождите несколько секунд, пока стрелка успокоится.",
            ), "Убедитесь, что «Калибровка компаса» включена: Настройки ← Конфиденциальность и безопасность ← Службы геолокации ← Системные службы.",
                "Включите «высокую точность» геолокации в настройках телефона.", "Готово", "Сбросить")
            "de" -> QiblaCalibText("Kompass kalibrieren", listOf(
                "Bewegen Sie das Telefon langsam dreimal in einer Acht durch die Luft und drehen Sie es dabei in alle Richtungen.",
                "Halten Sie Abstand zu Metall, Magneten, Elektrogeräten und dem Auto; nehmen Sie magnetische Hüllen ab.",
                "Halten Sie das Telefon flach vor sich, weg vom Körper, und warten Sie einige Sekunden, bis die Nadel ruhig ist.",
            ), "Prüfen Sie, dass „Kompasskalibrierung“ an ist: Einstellungen ← Datenschutz & Sicherheit ← Ortungsdienste ← Systemdienste.",
                "Schalten Sie in den Standorteinstellungen die „hohe Genauigkeit“ ein.", "Fertig", "Zurücksetzen")
            "es" -> QiblaCalibText("Calibrar la brújula", listOf(
                "Mueve el teléfono despacio dibujando un 8 en el aire tres veces, girándolo en todas las direcciones.",
                "Aléjate del metal, imanes, aparatos eléctricos y el coche; quita las fundas magnéticas.",
                "Sujeta el teléfono plano delante de ti, lejos del cuerpo, y espera unos segundos a que la aguja se estabilice.",
            ), "Comprueba que «Calibración de la brújula» está activada: Ajustes ← Privacidad y seguridad ← Localización ← Servicios del sistema.",
                "Activa la «alta precisión» de la ubicación en los ajustes del teléfono.", "Listo", "Restablecer")
            "uk" -> QiblaCalibText("Калібрування компаса", listOf(
                "Повільно проведіть телефоном у повітрі вісімку тричі, повертаючи його в усі боки.",
                "Відійдіть від металу, магнітів, електроприладів і машини; зніміть магнітний чохол.",
                "Тримайте телефон рівно перед собою, далі від тіла, і зачекайте кілька секунд, поки стрілка заспокоїться.",
            ), "Переконайтеся, що «Калібрування компаса» увімкнено: Параметри ← Приватність і безпека ← Служби локації ← Системні служби.",
                "Увімкніть «високу точність» локації в налаштуваннях телефона.", "Готово", "Скинути")
            "uz" -> QiblaCalibText("Kompasni sozlash", listOf(
                "Telefonni havoda sekin uch marta 8 shaklida, har tomonga aylantirib harakatlantiring.",
                "Metall, magnit, elektr asboblari va mashinadan uzoqlashing; magnitli g‘ilofni yeching.",
                "Telefonni tanangizdan uzoqda oldingizda tekis tuting va igna tinchlanguncha bir necha soniya kuting.",
            ), "«Kompas kalibrovkasi» yoqilganini tekshiring: Sozlamalar ← Maxfiylik va xavfsizlik ← Joylashuv xizmatlari ← Tizim xizmatlari.",
                "Telefon joylashuv sozlamalarida «yuqori aniqlik»ni yoqing.", "Tayyor", "Qayta sozlash")
            "kk" -> QiblaCalibText("Компасты калибрлеу", listOf(
                "Телефонды ауада баяу үш рет 8 пішінімен, барлық жаққа бұрап қозғаңыз.",
                "Металдан, магниттен, электр құрылғыларынан және көліктен алыстаңыз; магнитті қапты шешіңіз.",
                "Телефонды денеңізден алыс алдыңызда тегіс ұстап, тіл тұрақталғанша бірнеше секунд күтіңіз.",
            ), "«Компасты калибрлеу» қосулы екенін тексеріңіз: Параметрлер ← Құпиялық және қауіпсіздік ← Орналасу қызметтері ← Жүйелік қызметтер.",
                "Телефонның орналасу параметрлерінде «жоғары дәлдікті» қосыңыз.", "Дайын", "Қалпына келтіру")
            else -> QiblaCalibText("Calibrate the compass", listOf(
                "Slowly move the phone in a figure 8 in the air three times, turning it in every direction.",
                "Stay away from metal, magnets, electrical devices and the car; remove any magnetic case.",
                "Hold the phone flat in front of you, away from your body, and wait a few seconds for the needle to settle.",
            ), "Make sure “Compass Calibration” is on: Settings ← Privacy & Security ← Location Services ← System Services.",
                "Turn on “high accuracy” location in the phone settings.", "Done", "Reset")
        }
    }
}
