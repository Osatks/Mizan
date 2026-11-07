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
    /** شكل الصورة: قصة (9:16) أو بطاقة (منشور) */
    val story: String = "Story", val post: String = "Post",
    /** عنوان صورة الذكر */
    val dhikr: String = "Dhikr",
    /** وصف أداة حديث اليوم في معرض الأدوات */
    val hadithDesc: String = "An authentic hadith every day; tap to read its explanation",
) {
    companion object {
        fun of(code: String): HomeText = when (code) {
            "ar" -> HomeText("صباح الطاعة", "يومك مبارك", "مساء الطاعة", "مضى على الأذان", "أدواتك اليومية",
                "حديث اليوم", "الشرح", "من فوائد الحديث", "التفسير", "مشاركة صورة", "أرسل ملاحظاتك", "اقتراح أو مشكلة؟ نقرأ كل رسالة", "قصة", "بطاقة", "ذكر", "حديث نبوي صحيح كل يوم، ولمسه يفتح شرحه")
            "fr" -> HomeText("Bonjour", "Belle journée", "Bonsoir", "depuis l’adhan", "Vos outils du jour",
                "Hadith du jour", "Explication", "Enseignements", "Tafsir", "Partager en image", "Envoyer un avis", "Une idée ou un problème ? Nous lisons chaque message", "Story", "Publication", "Dhikr", "Un hadith authentique chaque jour ; touchez pour lire son explication")
            "tr" -> HomeText("Hayırlı sabahlar", "Hayırlı günler", "Hayırlı akşamlar", "ezandan beri", "Günlük araçların",
                "Günün hadisi", "Açıklama", "Hadisten faydalar", "Tefsir", "Görsel olarak paylaş", "Geri bildirim gönder", "Öneri veya sorun mu? Her mesajı okuyoruz", "Hikâye", "Gönderi", "Zikir", "Her gün sahih bir hadis; açıklamasını okumak için dokunun")
            "ur" -> HomeText("صبح بخیر", "دن مبارک", "شام بخیر", "اذان کو گزرے", "آپ کے روزانہ کے اوزار",
                "آج کی حدیث", "تشریح", "حدیث کے فوائد", "تفسیر", "تصویر شیئر کریں", "رائے بھیجیں", "تجویز یا مسئلہ؟ ہم ہر پیغام پڑھتے ہیں", "اسٹوری", "پوسٹ", "ذکر", "ہر روز ایک صحیح حدیث؛ تشریح پڑھنے کے لیے ٹیپ کریں")
            "id" -> HomeText("Selamat pagi", "Selamat siang", "Selamat malam", "sejak azan", "Alat harianmu",
                "Hadis hari ini", "Penjelasan", "Faedah hadis", "Tafsir", "Bagikan gambar", "Kirim masukan", "Saran atau masalah? Kami membaca setiap pesan", "Story", "Postingan", "Zikir", "Satu hadis sahih setiap hari; ketuk untuk membaca penjelasannya")
            "ms" -> HomeText("Selamat pagi", "Selamat tengah hari", "Selamat petang", "sejak azan", "Alatan harian anda",
                "Hadis hari ini", "Penjelasan", "Faedah hadis", "Tafsir", "Kongsi gambar", "Hantar maklum balas", "Cadangan atau masalah? Kami membaca setiap mesej", "Story", "Hantaran", "Zikir", "Satu hadis sahih setiap hari; ketik untuk membaca penjelasannya")
            "fa" -> HomeText("صبح بخیر", "روزتان مبارک", "شب بخیر", "از اذان گذشته", "ابزارهای روزانهٔ شما",
                "حدیث امروز", "شرح", "فواید حدیث", "تفسیر", "اشتراک تصویر", "ارسال بازخورد", "پیشنهاد یا مشکل؟ همهٔ پیام‌ها را می‌خوانیم", "استوری", "پست", "ذکر", "هر روز یک حدیث صحیح؛ برای خواندن شرح آن لمس کنید")
            "bn" -> HomeText("শুভ সকাল", "শুভ দিন", "শুভ সন্ধ্যা", "আজানের পর", "আপনার দৈনিক টুল",
                "আজকের হাদিস", "ব্যাখ্যা", "হাদিসের শিক্ষা", "তাফসির", "ছবি হিসেবে শেয়ার", "মতামত পাঠান", "পরামর্শ বা সমস্যা? আমরা প্রতিটি বার্তা পড়ি", "স্টোরি", "পোস্ট", "জিকির", "প্রতিদিন একটি সহিহ হাদিস; ব্যাখ্যা পড়তে ট্যাপ করুন")
            "ru" -> HomeText("Доброе утро", "Добрый день", "Добрый вечер", "после азана", "Ваши инструменты на день",
                "Хадис дня", "Пояснение", "Польза хадиса", "Тафсир", "Поделиться картинкой", "Отправить отзыв", "Идея или проблема? Мы читаем каждое сообщение", "История", "Пост", "Зикр", "Каждый день достоверный хадис; нажмите, чтобы прочитать пояснение")
            "de" -> HomeText("Guten Morgen", "Guten Tag", "Guten Abend", "seit dem Adhan", "Deine Werkzeuge für heute",
                "Hadith des Tages", "Erläuterung", "Lehren des Hadith", "Tafsir", "Als Bild teilen", "Feedback senden", "Idee oder Problem? Wir lesen jede Nachricht", "Story", "Beitrag", "Dhikr", "Jeden Tag ein authentischer Hadith; tippen für die Erläuterung")
            "es" -> HomeText("Buenos días", "Buen día", "Buenas noches", "desde el adhan", "Tus herramientas del día",
                "Hadiz del día", "Explicación", "Enseñanzas del hadiz", "Tafsir", "Compartir imagen", "Enviar comentarios", "¿Una idea o un problema? Leemos cada mensaje", "Historia", "Publicación", "Dhikr", "Un hadiz auténtico cada día; toca para leer su explicación")
            "uk" -> HomeText("Доброго ранку", "Доброго дня", "Доброго вечора", "після азану", "Ваші інструменти на день",
                "Хадис дня", "Пояснення", "Користь хадису", "Тафсир", "Поділитися зображенням", "Надіслати відгук", "Ідея чи проблема? Ми читаємо кожне повідомлення", "Історія", "Допис", "Зікр", "Щодня достовірний хадис; торкніться, щоб прочитати пояснення")
            "uz" -> HomeText("Xayrli tong", "Xayrli kun", "Xayrli kech", "azondan beri", "Kunlik vositalaringiz",
                "Kun hadisi", "Sharh", "Hadis foydalari", "Tafsir", "Rasm sifatida ulashish", "Fikr yuborish", "Taklif yoki muammo? Har bir xabarni o‘qiymiz", "Hikoya", "Post", "Zikr", "Har kuni bitta sahih hadis; sharhini o‘qish uchun bosing")
            "kk" -> HomeText("Қайырлы таң", "Қайырлы күн", "Қайырлы кеш", "азаннан бері", "Күнделікті құралдарыңыз",
                "Күннің хадисі", "Түсіндірме", "Хадистің пайдасы", "Тәпсір", "Сурет ретінде бөлісу", "Пікір жіберу", "Ұсыныс па, мәселе ме? Әр хатты оқимыз", "Сторис", "Жазба", "Зікір", "Күн сайын бір сахих хадис; түсіндірмесін оқу үшін түртіңіз")
            else -> HomeText("Good morning", "Good day", "Good evening", "since the adhan", "Your daily tools",
                "Hadith of the day", "Explanation", "Lessons from the hadith", "Tafsir", "Share as image", "Send feedback", "An idea or a problem? We read every message", "Story", "Post", "Dhikr", "An authentic hadith every day; tap to read its explanation")
        }
    }
}
