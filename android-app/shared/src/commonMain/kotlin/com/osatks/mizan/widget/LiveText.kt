package com.osatks.mizan.widget

/**
 * نصوص الأنشطة الحية وأدوات iOS الجديدة (المسبحة على شاشة القفل والجزيرة الديناميكية، والإقامة ورمضان في العدّ
 * التنازلي، وعدّاد الأذكار، وأداتا المسبحة والورد) بكل لغات التطبيق، مستقلة عن Strings كما في [WidgetText]
 * حتى لا تتغير حزم اللغات. ما له نص في Strings يُؤخذ منها (اسم المسبحة، والتصفير، وورد اليوم…).
 */
class LiveText(
    val end: String,         // زر إنهاء النشاط الحي
    val iqamaIn: String,     // «الإقامة بعد» فوق العدّ إلى الإقامة
    val iftar: String,       // رمضان: عنوان العدّ إلى المغرب
    val suhoor: String,      // رمضان: عنوان العدّ إلى الفجر
    val iftarNow: String,    // رمضان: عند المغرب
    val adhkarDone: String,  // «أتممت %s»
    val tasbih: String,      // اسم أداة المسبحة في المعرض
    val descTasbih: String,
    val wird: String,        // اسم أداة الورد في المعرض
    val descWird: String,
    val showLive: String,    // زر في شاشة المسبحة (iOS): يُظهر العدّاد على شاشة القفل وفي الجزيرة الديناميكية
    val again: String,       // بعد إتمام الأذكار: من أولها
) {
    fun adhkarDone(section: String) = adhkarDone.replace("%s", section)

    companion object {
        /** دعاء الإفطار، بالعربية في كل اللغات كبقية الأذكار */
        const val IFTAR_DUA = "ذَهَبَ الظَّمَأُ، وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ"

        fun of(code: String): LiveText = ALL[code] ?: EN

        val AR = LiveText(
            "إنهاء", "الإقامة بعد", "موعد الإفطار", "نهاية السحور", "حان وقت الإفطار", "أتممت %s ✓",
            "المسبحة", "سبّح بلمسة من الشاشة الرئيسية أو شاشة القفل",
            "ورد اليوم", "صفحات وردك اليوم وتقدّم ختمتك", "على شاشة القفل",
            "من جديد",
        )
        val EN = LiveText(
            "End", "Iqama in", "Iftar", "Suhoor ends", "Time to break the fast", "%s completed ✓",
            "Tasbih", "Count with a tap from the Home Screen or Lock Screen",
            "Today's portion", "Today's Quran pages and your khatma progress", "On Lock Screen",
            "Start again",
        )
        val FR = LiveText(
            "Terminer", "Iqama dans", "Iftar", "Fin du sahour", "C’est l’heure de rompre le jeûne", "%s terminés ✓",
            "Tasbih", "Comptez d’un geste depuis l’écran d’accueil ou l’écran verrouillé",
            "Portion du jour", "Les pages du jour et l’avancée de votre khatma", "Sur l’écran verrouillé",
            "Recommencer",
        )
        val TR = LiveText(
            "Bitir", "Kamete kalan", "İftar", "Sahur bitişi", "İftar vakti", "%s tamamlandı ✓",
            "Tesbih", "Ana ekrandan veya kilit ekranından tek dokunuşla sayın",
            "Günün virdi", "Bugünkü sayfalar ve hatim ilerlemeniz", "Kilit ekranında",
            "Baştan",
        )
        val UR = LiveText(
            "ختم کریں", "اقامت میں", "افطار", "سحری کا اختتام", "افطار کا وقت ہو گیا", "%s مکمل ✓",
            "تسبیح", "ہوم اسکرین یا لاک اسکرین سے ایک ٹیپ میں گنیں",
            "آج کا ورد", "آج کے صفحات اور ختم کی پیش رفت", "لاک اسکرین پر",
            "دوبارہ شروع",
        )
        val ID = LiveText(
            "Selesai", "Iqamah dalam", "Berbuka", "Sahur berakhir", "Waktunya berbuka puasa", "%s selesai ✓",
            "Tasbih", "Hitung dengan sekali ketuk dari Layar Utama atau Layar Kunci",
            "Wirid hari ini", "Halaman hari ini dan kemajuan khatam Anda", "Di Layar Kunci",
            "Ulangi",
        )
        val MS = LiveText(
            "Tamat", "Iqamah dalam", "Berbuka", "Sahur berakhir", "Waktu berbuka puasa", "%s selesai ✓",
            "Tasbih", "Kira dengan satu ketikan dari Skrin Utama atau Skrin Kunci",
            "Wirid hari ini", "Halaman hari ini dan kemajuan khatam anda", "Di Skrin Kunci",
            "Mula semula",
        )
        val FA = LiveText(
            "پایان", "اقامه تا", "افطار", "پایان سحر", "وقت افطار است", "%s تمام شد ✓",
            "تسبیح", "با یک ضربه از صفحهٔ اصلی یا صفحهٔ قفل بشمارید",
            "ورد امروز", "صفحه‌های امروز و پیشرفت ختم شما", "روی صفحهٔ قفل",
            "از نو",
        )
        val BN = LiveText(
            "শেষ", "ইকামত বাকি", "ইফতার", "সাহরির শেষ", "ইফতারের সময় হয়েছে", "%s সম্পন্ন ✓",
            "তাসবিহ", "হোম স্ক্রিন বা লক স্ক্রিন থেকে এক ট্যাপে গুনুন",
            "আজকের অংশ", "আজকের পৃষ্ঠা ও খতমের অগ্রগতি", "লক স্ক্রিনে",
            "আবার শুরু",
        )
        val RU = LiveText(
            "Завершить", "Икамат через", "Ифтар", "Конец сухура", "Время разговения", "%s — выполнено ✓",
            "Тасбих", "Считайте одним касанием с экрана «Домой» или экрана блокировки",
            "Сегодняшняя часть", "Страницы на сегодня и прогресс хатма", "На экране блокировки",
            "Заново",
        )
        val DE = LiveText(
            "Beenden", "Iqama in", "Iftar", "Ende des Suhur", "Zeit zum Fastenbrechen", "%s abgeschlossen ✓",
            "Tasbih", "Mit einem Tippen vom Home- oder Sperrbildschirm zählen",
            "Heutiger Abschnitt", "Heutige Seiten und Fortschritt deiner Khatma", "Auf dem Sperrbildschirm",
            "Von vorn",
        )
        val ES = LiveText(
            "Terminar", "Iqama en", "Iftar", "Fin del suhur", "Hora de romper el ayuno", "%s completados ✓",
            "Tasbih", "Cuenta con un toque desde la pantalla de inicio o la de bloqueo",
            "Porción de hoy", "Las páginas de hoy y el avance de tu jatma", "En la pantalla bloqueada",
            "De nuevo",
        )
        val UK = LiveText(
            "Завершити", "Ікамат через", "Іфтар", "Кінець сухуру", "Час розговіння", "%s — виконано ✓",
            "Тасбіх", "Рахуйте одним дотиком з початкового екрана або екрана блокування",
            "Сьогоднішня частина", "Сторінки на сьогодні й поступ хатму", "На екрані блокування",
            "Знову",
        )
        val UZ = LiveText(
            "Tugatish", "Iqomatgacha", "Iftor", "Saharlik tugashi", "Iftor vaqti bo‘ldi", "%s yakunlandi ✓",
            "Tasbeh", "Asosiy ekran yoki qulf ekranidan bir bosishda sanang",
            "Bugungi vird", "Bugungi sahifalar va xatm rivoji", "Qulf ekranida",
            "Qaytadan",
        )
        val KK = LiveText(
            "Аяқтау", "Қамәтке дейін", "Ауызашар", "Сәресінің соңы", "Ауызашар уақыты болды", "%s аяқталды ✓",
            "Тәспі", "Басты экраннан не құлып экранынан бір түртумен санаңыз",
            "Бүгінгі бөлік", "Бүгінгі беттер және хатым барысы", "Құлып экранында",
            "Қайтадан",
        )

        private val ALL = mapOf(
            "ar" to AR, "en" to EN, "fr" to FR, "tr" to TR, "ur" to UR, "id" to ID, "ms" to MS, "fa" to FA,
            "bn" to BN, "ru" to RU, "de" to DE, "es" to ES, "uk" to UK, "uz" to UZ, "kk" to KK,
        )

        /** كل اللغات (للاختبار: لا نص فارغ ولا لغة ناقصة) */
        internal val all: Map<String, LiveText> get() = ALL
    }
}
