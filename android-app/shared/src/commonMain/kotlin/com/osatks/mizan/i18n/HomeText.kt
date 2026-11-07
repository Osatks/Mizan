package com.osatks.mizan.i18n

/**
 * نصوص الرئيسية الجديدة بلغات التطبيق: التحية بحسب الوقت (الصباح من الفجر، والنهار من الظهر، والمساء من المغرب)،
 * و«منذ الأذان» تحت الصلاة الحاضرة، وعنوان أدوات اليوم، وآية اليوم وحديث اليوم وشرحه، ومشاركة الصورة قصةً.
 */
class HomeText(
    val morning: String, val day: String, val evening: String,
    val sinceAdhan: String, val daily: String,
    val hadithOfDay: String, val explanation: String, val benefits: String, val tafsir: String,
    val shareStory: String, val feedback: String, val feedbackSub: String,
) {
    companion object {
        fun of(code: String): HomeText = when (code) {
            "ar" -> HomeText("صباح الطاعة", "يومك مبارك", "مساء الطاعة", "مضى على الأذان", "أدواتك اليومية",
                "حديث اليوم", "الشرح", "من فوائد الحديث", "التفسير", "مشاركة صورة", "أرسل ملاحظاتك", "اقتراح أو مشكلة؟ نقرأ كل رسالة")
            "fr" -> HomeText("Bonjour", "Belle journée", "Bonsoir", "depuis l’adhan", "Vos outils du jour",
                "Hadith du jour", "Explication", "Enseignements", "Tafsir", "Partager en image", "Envoyer un avis", "Une idée ou un problème ? Nous lisons chaque message")
            "tr" -> HomeText("Hayırlı sabahlar", "Hayırlı günler", "Hayırlı akşamlar", "ezandan beri", "Günlük araçların",
                "Günün hadisi", "Açıklama", "Hadisten faydalar", "Tefsir", "Görsel olarak paylaş", "Geri bildirim gönder", "Öneri veya sorun mu? Her mesajı okuyoruz")
            "ur" -> HomeText("صبح بخیر", "دن مبارک", "شام بخیر", "اذان کو گزرے", "آپ کے روزانہ کے اوزار",
                "آج کی حدیث", "تشریح", "حدیث کے فوائد", "تفسیر", "تصویر شیئر کریں", "رائے بھیجیں", "تجویز یا مسئلہ؟ ہم ہر پیغام پڑھتے ہیں")
            "id" -> HomeText("Selamat pagi", "Selamat siang", "Selamat malam", "sejak azan", "Alat harianmu",
                "Hadis hari ini", "Penjelasan", "Faedah hadis", "Tafsir", "Bagikan gambar", "Kirim masukan", "Saran atau masalah? Kami membaca setiap pesan")
            "ms" -> HomeText("Selamat pagi", "Selamat tengah hari", "Selamat petang", "sejak azan", "Alatan harian anda",
                "Hadis hari ini", "Penjelasan", "Faedah hadis", "Tafsir", "Kongsi gambar", "Hantar maklum balas", "Cadangan atau masalah? Kami membaca setiap mesej")
            "fa" -> HomeText("صبح بخیر", "روزتان مبارک", "شب بخیر", "از اذان گذشته", "ابزارهای روزانهٔ شما",
                "حدیث امروز", "شرح", "فواید حدیث", "تفسیر", "اشتراک تصویر", "ارسال بازخورد", "پیشنهاد یا مشکل؟ همهٔ پیام‌ها را می‌خوانیم")
            "bn" -> HomeText("শুভ সকাল", "শুভ দিন", "শুভ সন্ধ্যা", "আজানের পর", "আপনার দৈনিক টুল",
                "আজকের হাদিস", "ব্যাখ্যা", "হাদিসের শিক্ষা", "তাফসির", "ছবি হিসেবে শেয়ার", "মতামত পাঠান", "পরামর্শ বা সমস্যা? আমরা প্রতিটি বার্তা পড়ি")
            "ru" -> HomeText("Доброе утро", "Добрый день", "Добрый вечер", "после азана", "Ваши инструменты на день",
                "Хадис дня", "Пояснение", "Польза хадиса", "Тафсир", "Поделиться картинкой", "Отправить отзыв", "Идея или проблема? Мы читаем каждое сообщение")
            "de" -> HomeText("Guten Morgen", "Guten Tag", "Guten Abend", "seit dem Adhan", "Deine Werkzeuge für heute",
                "Hadith des Tages", "Erläuterung", "Lehren des Hadith", "Tafsir", "Als Bild teilen", "Feedback senden", "Idee oder Problem? Wir lesen jede Nachricht")
            "es" -> HomeText("Buenos días", "Buen día", "Buenas noches", "desde el adhan", "Tus herramientas del día",
                "Hadiz del día", "Explicación", "Enseñanzas del hadiz", "Tafsir", "Compartir imagen", "Enviar comentarios", "¿Una idea o un problema? Leemos cada mensaje")
            "uk" -> HomeText("Доброго ранку", "Доброго дня", "Доброго вечора", "після азану", "Ваші інструменти на день",
                "Хадис дня", "Пояснення", "Користь хадису", "Тафсир", "Поділитися зображенням", "Надіслати відгук", "Ідея чи проблема? Ми читаємо кожне повідомлення")
            "uz" -> HomeText("Xayrli tong", "Xayrli kun", "Xayrli kech", "azondan beri", "Kunlik vositalaringiz",
                "Kun hadisi", "Sharh", "Hadis foydalari", "Tafsir", "Rasm sifatida ulashish", "Fikr yuborish", "Taklif yoki muammo? Har bir xabarni o‘qiymiz")
            "kk" -> HomeText("Қайырлы таң", "Қайырлы күн", "Қайырлы кеш", "азаннан бері", "Күнделікті құралдарыңыз",
                "Күннің хадисі", "Түсіндірме", "Хадистің пайдасы", "Тәпсір", "Сурет ретінде бөлісу", "Пікір жіберу", "Ұсыныс па, мәселе ме? Әр хатты оқимыз")
            else -> HomeText("Good morning", "Good day", "Good evening", "since the adhan", "Your daily tools",
                "Hadith of the day", "Explanation", "Lessons from the hadith", "Tafsir", "Share as image", "Send feedback", "An idea or a problem? We read every message")
        }
    }
}
