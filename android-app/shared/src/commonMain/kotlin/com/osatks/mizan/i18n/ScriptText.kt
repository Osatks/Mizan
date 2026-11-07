package com.osatks.mizan.i18n

/**
 * نصوص خيار خط المصحف (مصحف المدينة أو الخط الآخر) وتنزيل مصحف المدينة كاملاً، بكل لغات التطبيق.
 * downloadAll يحمل %s للحجم.
 */
class ScriptText(
    val title: String, val madinah: String, /** الخط الآخر باسمه (Amiri Quran) */ val other: String, val note: String,
    val downloadAll: String, val downloadSub: String, val downloading: String, val downloaded: String, val cancel: String,
) {
    companion object {
        fun of(code: String): ScriptText = ALL[code] ?: EN

        private val AR = ScriptText(
            "خط المصحف", "مصحف المدينة", "خط أميري",
            "طبعة مجمع الملك فهد لطباعة المصحف الشريف، صفحة بصفحة كالمطبوع. تُنزَّل كل صفحة عند فتحها.",
            "تنزيل المصحف كاملاً", "للقراءة دون إنترنت (نحو ٢٠٠ م.ب)", "جارٍ التنزيل", "المصحف كاملاً على جهازك", "إيقاف",
        )
        private val EN = ScriptText(
            "Mushaf script", "Madinah Mushaf", "Amiri",
            "The King Fahd Complex print, page by page as printed. Each page downloads when you open it.",
            "Download the whole Mushaf", "For reading offline (about 200 MB)", "Downloading", "The whole Mushaf is on your device", "Stop",
        )
        private val FR = ScriptText(
            "Écriture du Mushaf", "Mushaf de Médine", "Amiri",
            "L’édition du Complexe du Roi Fahd, page par page comme l’imprimé. Chaque page se télécharge à l’ouverture.",
            "Télécharger tout le Mushaf", "Pour lire hors ligne (environ 200 Mo)", "Téléchargement", "Tout le Mushaf est sur votre appareil", "Arrêter",
        )
        private val TR = ScriptText(
            "Mushaf yazısı", "Medine Mushafı", "Amiri hattı",
            "Kral Fahd Kompleksi baskısı, basılı hâliyle sayfa sayfa. Her sayfa açıldığında indirilir.",
            "Mushafın tamamını indir", "İnternetsiz okumak için (yaklaşık 200 MB)", "İndiriliyor", "Mushafın tamamı cihazınızda", "Durdur",
        )
        private val UR = ScriptText(
            "مصحف کا خط", "مصحفِ مدینہ", "امیری خط",
            "شاہ فہد کمپلیکس کی طباعت، چھپے ہوئے کی طرح صفحہ بہ صفحہ۔ ہر صفحہ کھولنے پر ڈاؤن لوڈ ہوتا ہے۔",
            "پورا مصحف ڈاؤن لوڈ کریں", "انٹرنیٹ کے بغیر پڑھنے کے لیے (تقریباً 200 MB)", "ڈاؤن لوڈ ہو رہا ہے", "پورا مصحف آپ کے آلے پر ہے", "روکیں",
        )
        private val ID = ScriptText(
            "Tulisan mushaf", "Mushaf Madinah", "Amiri",
            "Cetakan Kompleks Raja Fahd, halaman demi halaman seperti cetakannya. Setiap halaman diunduh saat dibuka.",
            "Unduh seluruh mushaf", "Untuk membaca tanpa internet (sekitar 200 MB)", "Mengunduh", "Seluruh mushaf ada di perangkat Anda", "Hentikan",
        )
        private val MS = ScriptText(
            "Tulisan mushaf", "Mushaf Madinah", "Amiri",
            "Cetakan Kompleks Raja Fahd, halaman demi halaman seperti cetakannya. Setiap halaman dimuat turun apabila dibuka.",
            "Muat turun seluruh mushaf", "Untuk membaca tanpa internet (kira-kira 200 MB)", "Memuat turun", "Seluruh mushaf ada pada peranti anda", "Henti",
        )
        private val FA = ScriptText(
            "خط مصحف", "مصحف مدینه", "خط امیری",
            "چاپ مجتمع ملک فهد، صفحه به صفحه مانند نسخهٔ چاپی. هر صفحه هنگام باز شدن دانلود می‌شود.",
            "دانلود کل مصحف", "برای خواندن بدون اینترنت (حدود ۲۰۰ مگابایت)", "در حال دانلود", "کل مصحف روی دستگاه شماست", "توقف",
        )
        private val BN = ScriptText(
            "মুসহাফের লিপি", "মদিনা মুসহাফ", "আমিরি",
            "বাদশাহ ফাহাদ কমপ্লেক্সের মুদ্রণ, ছাপা কপির মতো পৃষ্ঠা অনুযায়ী। প্রতিটি পৃষ্ঠা খোলার সময় ডাউনলোড হয়।",
            "পুরো মুসহাফ ডাউনলোড করুন", "ইন্টারনেট ছাড়া পড়ার জন্য (প্রায় 200 MB)", "ডাউনলোড হচ্ছে", "পুরো মুসহাফ আপনার ডিভাইসে আছে", "থামান",
        )
        private val RU = ScriptText(
            "Шрифт мусхафа", "Мединский мусхаф", "Амири",
            "Издание Комплекса короля Фахда, страница в страницу как в печатном. Каждая страница загружается при открытии.",
            "Скачать весь мусхаф", "Для чтения без интернета (около 200 МБ)", "Загрузка", "Весь мусхаф на вашем устройстве", "Остановить",
        )
        private val DE = ScriptText(
            "Mushaf-Schrift", "Medina-Mushaf", "Amiri",
            "Die Ausgabe des König-Fahd-Komplexes, Seite für Seite wie gedruckt. Jede Seite wird beim Öffnen geladen.",
            "Ganzen Mushaf laden", "Zum Lesen ohne Internet (etwa 200 MB)", "Wird geladen", "Der ganze Mushaf ist auf deinem Gerät", "Stopp",
        )
        private val ES = ScriptText(
            "Escritura del Mushaf", "Mushaf de Medina", "Amiri",
            "La edición del Complejo del Rey Fahd, página a página como la impresa. Cada página se descarga al abrirla.",
            "Descargar todo el Mushaf", "Para leer sin internet (unos 200 MB)", "Descargando", "Todo el Mushaf está en tu dispositivo", "Detener",
        )
        private val UK = ScriptText(
            "Шрифт мусхафу", "Мединський мусхаф", "Амірі",
            "Видання Комплексу короля Фахда, сторінка в сторінку як у друкованому. Кожна сторінка завантажується під час відкриття.",
            "Завантажити весь мусхаф", "Для читання без інтернету (близько 200 МБ)", "Завантаження", "Увесь мусхаф на вашому пристрої", "Зупинити",
        )
        private val UZ = ScriptText(
            "Mushaf yozuvi", "Madina mushafi", "Amiri",
            "Qirol Fahd majmuasi nashri, bosma nusxadagidek sahifama-sahifa. Har bir sahifa ochilganda yuklanadi.",
            "Butun mushafni yuklab olish", "Internetsiz o‘qish uchun (taxminan 200 MB)", "Yuklanmoqda", "Butun mushaf qurilmangizda", "To‘xtatish",
        )
        private val KK = ScriptText(
            "Мұсхаф жазуы", "Мәдина мұсхафы", "Әмири",
            "Король Фаһд кешенінің басылымы, баспадағыдай бет-бетімен. Әр бет ашылғанда жүктеледі.",
            "Бүкіл мұсхафты жүктеу", "Интернетсіз оқу үшін (шамамен 200 МБ)", "Жүктелуде", "Бүкіл мұсхаф құрылғыңызда", "Тоқтату",
        )

        val ALL: Map<String, ScriptText> = mapOf(
            "ar" to AR, "en" to EN, "fr" to FR, "tr" to TR, "ur" to UR, "id" to ID, "ms" to MS, "fa" to FA,
            "bn" to BN, "ru" to RU, "de" to DE, "es" to ES, "uk" to UK, "uz" to UZ, "kk" to KK,
        )
    }
}
