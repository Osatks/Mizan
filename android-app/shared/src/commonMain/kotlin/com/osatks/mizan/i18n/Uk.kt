package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Українські форми числа: 1, 21… → one; 2–4, 22–24… → few; решта (0, 5–20, 11–14…) → many */
private fun ukPlural(n: Long, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    val w = when {
        m10 == 1L && m100 != 11L -> one
        m10 in 2L..4L && m100 !in 12L..14L -> few
        else -> many
    }
    return "$n $w"
}

/** Родовий відмінок місяців для повних дат: «15 жовтня 2026 р.» */
private val ukGregMonthsGen = listOf(
    "січня", "лютого", "березня", "квітня", "травня", "червня",
    "липня", "серпня", "вересня", "жовтня", "листопада", "грудня"
)
private val ukHijriMonthsGen = listOf(
    "Мухаррама", "Сафара", "Рабі аль-авваля", "Рабі ас-сані", "Джумада аль-уля", "Джумада аль-ахіра",
    "Раджаба", "Шаабана", "Рамадана", "Шавваля", "Зуль-каади", "Зуль-хіджі"
)

object Uk : Strings {
    // ---------- General ----------
    override val appName = "Мізан"
    override val appTagline = "Інструменти мусульманина"
    override val tabHome = "Головна"
    override val tabPrayer = "Намаз"
    override val tabQibla = "Кібла"
    override val tabTasbih = "Тасбіх"
    override val tabSettings = "Налаштування"
    override val back = "Назад"
    override val share = "Поділитися"
    override val shareSignature = "— застосунок «Мізан»"
    override val today = "Сьогодні"
    override val tomorrow = "Завтра"
    override fun inDays(n: Long) = "через " + ukPlural(n, "день", "дні", "днів")
    override fun daysCount(n: Long) = ukPlural(n, "день", "дні", "днів")
    override val decrease = "Зменшити"
    override val increase = "Збільшити"
    override val noLimit = "Без обмежень"
    override val noResults = "Нічого не знайдено"
    override val unitGram = "г"
    override val unitKg = "кг"
    override val unitKm = "км"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Мухаррам", "Сафар", "Рабі аль-авваль", "Рабі ас-сані", "Джумада аль-уля", "Джумада аль-ахіра",
        "Раджаб", "Шаабан", "Рамадан", "Шавваль", "Зуль-каада", "Зуль-хіджа"
    )
    override val gregMonths = listOf(
        "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
        "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    )
    override val weekdays = listOf("Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця", "Субота", "Неділя")
    override val weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${ukHijriMonthsGen[m - 1]} $y р. х."
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${ukGregMonthsGen[m - 1]} $y р."
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y р. х."
    override val hijriYearSuffix = "р. х."
    override val gregYearSuffix = "р."
    override val dayLabel = "День"
    override val monthLabel = "Місяць"
    override val yearLabel = "Рік"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Калькулятор закяту", "Гроші, золото й акції з урахуванням нісабу"),
        "inheritance" to Named("Калькулятор спадщини", "Поділ спадщини за правилами фараїду"),
        "prayer" to Named("Час намазу", "Сповіщення про азан і зворотний відлік"),
        "qibla" to Named("Напрямок кібли", "Компас на датчиках вашого телефона"),
        "hijri" to Named("Дата за хіджрою", "Переведення дат, календар Умм аль-Кура"),
        "events" to Named("Ісламські дати", "Рамадан, обидва Іди та день Арафа"),
        "tasbih" to Named("Тасбіх", "Лічильник зікрів із готовими формулами"),
        "adhkar" to Named("Азкари", "«Фортеця мусульманина» і щоденні азкари з доказами"),
        "fitr" to Named("Закят аль-Фітр", "Мірою са' або грошима"),
        "age" to Named("Калькулятор віку", "У роках за хіджрою та григоріанських"),
        "fidya" to Named("Фідья і каффара", "Пропущений піст і порушені клятви"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "закят закат зякят садака милостиня багатство майно золото срібло нісаб гроші заощадження акції",
        "inheritance" to "спадщина спадок спадкування спадкоємці фараїд фараід мірас заповіт частки поділ",
        "prayer" to "намаз молитва салят салат азан фаджр зухр аср магріб іша час розклад",
        "qibla" to "кібла кибла кааба мекка компас напрямок",
        "hijri" to "календар дата хіджра хіджрі ісламський переведення конвертер умм аль-кура",
        "events" to "рамадан рамазан ід ураза курбан байрам свято арафа ашура кадр",
        "tasbih" to "зікр зікри азкари вервиця чотки лічильник тасбіх",
        "adhkar" to "азкари зікр дуа молитва фортеця мусульманина ранок вечір сон рамадан",
        "fitr" to "фітр фітра закят садака ураза байрам ід са",
        "age" to "вік день народження роки років",
        "fidya" to "фідья фідія каффара кафара спокута клятва піст годування",
    )
    override val toolsTitle = "Інструменти"
    override fun nextPrayerIn(city: String) = "Наступний намаз · $city"
    override val searchHint = "Пошук інструментів, міст, зікрів…"

    // ---------- Search ----------
    override val searchTitle = "Пошук"
    override val searchToolsGroup = "Інструменти"
    override val searchCitiesGroup = "Міста"
    override val searchPrayersGroup = "Час намазів сьогодні"
    override val searchEventsGroup = "Дати"
    override val searchDhikrGroup = "Зікри"
    override val searchSettingsGroup = "Налаштування"
    override val searchEmpty = "Шукайте інструменти, міста, зікри, дати й налаштування"
    override val setAsMyCity = "Використовувати для часу намазу"
    override fun searchNoResults(q: String) = "За запитом «$q» нічого не знайдено"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Фаджр", "sunrise" to "Схід сонця", "dhuhr" to "Зухр",
        "asr" to "Аср", "maghrib" to "Магріб", "isha" to "Іша"
    )
    override val prayerTitle = "Час намазу"
    override fun nextPrayerLabel(name: String) = "Наступний намаз: $name"
    override fun atTime(time: String) = "о $time"
    override val prevDay = "Попередній день"
    override val nextDay = "Наступний день"
    override val monthTable = "Розклад на місяць"
    override val hideMonthTable = "Сховати розклад"
    override fun monthTableTitle(m: Int, y: Int) = "Розклад на ${gregMonths[m - 1].lowercase()} $y р."
    override val colDay = "День"
    override val locationAndMethod = "Місцезнаходження і розрахунок"
    override val useMyLocation = "Використати моє місцезнаходження"
    override val locating = "Визначаємо місцезнаходження…"
    override val orPickCity = "Або виберіть місто"
    override val pickCity = "Виберіть місто"
    override val searchCity = "Пошук міста"
    override val myLocation = "Моє місцезнаходження"
    override val calcMethod = "Метод розрахунку"
    override val methods = mapOf(
        "makkah" to "Умм аль-Кура (Саудівська Аравія)",
        "mwl" to "Ліга ісламського світу",
        "egypt" to "Єгипетське управління геодезії",
        "dubai" to "ОАЕ (Дубай)",
        "kuwait" to "Кувейт",
        "qatar" to "Катар",
        "jordan" to "Йорданія",
        "palestine" to "Палестина (Дар аль-Іфта)",
        "lebanon" to "Ліван (Дар аль-Фатва)",
        "syria" to "Сирія",
        "libya" to "Лівія",
        "bahrain" to "Бахрейн",
        "brunei" to "Бруней",
        "azerbaijan" to "Управління мусульман Кавказу (Азербайджан)",
        "uzbekistan" to "Управління мусульман Узбекистану",
        "kazakhstan" to "Духовне управління мусульман Казахстану",
        "tatarstan" to "Духовне управління мусульман Татарстану",
        "uk" to "Єдиний розклад Лондона (Велика Британія)",
        "paris" to "Велика мечеть Парижа",
        "europe" to "Єдиний європейський метод (Стамбул 2021)",
        "bosnia" to "Ісламська громада Боснії і Герцеговини",
        "morocco" to "Марокко",
        "algeria" to "Алжир",
        "tunisia" to "Туніс",
        "karachi" to "Університет ісламських наук, Карачі",
        "turkey" to "Diyanet (Туреччина)",
        "tehran" to "Інститут геофізики Тегеранського університету",
        "indonesia" to "Міністерство у справах релігій (Індонезія)",
        "malaysia" to "JAKIM (Малайзія)",
        "singapore" to "MUIS (Сінгапур)",
        "russia" to "Духовне управління мусульман Росії",
        "isna" to "Північна Америка (ISNA)",
        "france" to "Союз ісламських організацій Франції",
    )
    override val asrMethod = "Розрахунок часу Асру"
    override val asrStandard = "Більшість (шафіїти, малікіти, ханбаліти)"
    override val asrHanafi = "Ханафітський"
    override val latLng = "Широта / довгота"
    override val adhanAlert = "Сповіщення про азан"
    override val adhanAlertDesc = "Сповіщення, коли настає час кожного намазу"
    override val timesMayDiffer = "Час може відрізнятися на хвилину-дві від офіційного розкладу вашої країни."
    override fun endsAt(t: String) = "До $t"
    override fun iqamaAt(t: String) = "Ікама $t"
    override val iqamaTitle = "Час ікамату"
    override val iqamaDesc = "Хвилини між азаном та ікаматом у вашій мечеті; 0 приховує його."
    override val prayerEndsNote = "Час фаджру закінчується зі сходом сонця, зухру — з настанням асру, асру — із заходом сонця (відкладати його до пожовтіння сонця небажано), магрибу — зі зникненням червоної вечірньої зорі; обраний час іші закінчується опівночі, а вимушений триває до фаджру."
    override val locationFound = "Місцезнаходження визначено ✓"
    override val locationFailed = "Не вдалося визначити місцезнаходження. Переконайтеся, що геолокацію ввімкнено."
    override val locationDenied = "Доступ до місцезнаходження не надано. Виберіть місто зі списку."
    override val cities = mapOf(
        "makkah" to "Мекка", "madinah" to "Медина", "riyadh" to "Ер-Ріяд", "jeddah" to "Джидда",
        "dammam" to "Даммам", "abha" to "Абха", "dubai" to "Дубай", "abudhabi" to "Абу-Дабі", "sharjah" to "Шарджа",
        "kuwait" to "Ель-Кувейт", "doha" to "Доха", "manama" to "Манама", "muscat" to "Маскат",
        "cairo" to "Каїр", "alexandria" to "Александрія", "aswan" to "Асуан", "amman" to "Амман",
        "jerusalem" to "Єрусалим (аль-Кудс)", "gaza" to "Газа", "beirut" to "Бейрут", "damascus" to "Дамаск", "aleppo" to "Алеппо",
        "baghdad" to "Багдад", "basra" to "Басра", "erbil" to "Ербіль", "sanaa" to "Сана", "aden" to "Аден",
        "khartoum" to "Хартум", "tripoli" to "Триполі", "benghazi" to "Бенгазі", "tunis" to "Туніс",
        "algiers" to "Алжир", "oran" to "Оран", "rabat" to "Рабат", "casablanca" to "Касабланка",
        "marrakesh" to "Марракеш", "nouakchott" to "Нуакшот", "mogadishu" to "Могадішо", "djibouti" to "Джибуті",
        "istanbul" to "Стамбул", "ankara" to "Анкара", "karachi" to "Карачі", "lahore" to "Лахор",
        "islamabad" to "Ісламабад", "delhi" to "Делі", "mumbai" to "Мумбаї", "hyderabad" to "Хайдарабад",
        "dhaka" to "Дакка", "chittagong" to "Читтагонг", "kabul" to "Кабул", "tehran" to "Тегеран", "mashhad" to "Мешхед",
        "tashkent" to "Ташкент", "baku" to "Баку", "almaty" to "Алмати", "moscow" to "Москва", "kazan" to "Казань",
        "grozny" to "Грозний", "kualalumpur" to "Куала-Лумпур", "penang" to "Пінанг", "singapore" to "Сінгапур",
        "brunei" to "Бандар-Сері-Бегаван", "jakarta" to "Джакарта", "surabaya" to "Сурабая", "bandung" to "Бандунг",
        "medan" to "Медан", "makassar" to "Макасар", "lagos" to "Лагос", "kano" to "Кано", "dakar" to "Дакар",
        "sarajevo" to "Сараєво", "london" to "Лондон", "birmingham" to "Бірмінгем", "paris" to "Париж",
        "marseille" to "Марсель", "lyon" to "Ліон", "brussels" to "Брюссель", "amsterdam" to "Амстердам",
        "berlin" to "Берлін", "hamburg" to "Гамбург", "cologne" to "Кельн", "munich" to "Мюнхен",
        "vienna" to "Відень", "stockholm" to "Стокгольм", "newyork" to "Нью-Йорк", "chicago" to "Чикаго",
        "losangeles" to "Лос-Анджелес", "toronto" to "Торонто", "montreal" to "Монреаль", "sydney" to "Сідней",
    )
    override val palestine = "Палестина"

    // ---------- Notifications ----------
    override val channelName = "Час намазу"
    override val channelDesc = "Сповіщення, коли настає час кожного намазу"
    override fun prayerNowTitle(prayer: String) = "Настав час намазу $prayer"
    override fun prayerNowText(city: String) = "$city — поспішайте на намаз"
    // ---------- Adhan & alerts ----------
    override val adhanSettings = "Азан і сповіщення"
    override val adhanSettingsSub = "Муедзин, сповіщення для кожної молитви та вібрація"
    override val alertPerPrayer = "Сповіщення для кожної молитви"
    override val alertModeAdhan = "Азан"
    override val alertModeVibrate = "Лише вібрація"
    override val alertModeSilent = "Беззвучне сповіщення"
    override val alertModeOff = "Без сповіщення"
    override val alertModesHint = "«Азан» відтворює азан повністю, «Лише вібрація» вібрує без звуку, «Беззвучне сповіщення» показує сповіщення без звуку, а «Без сповіщення» нічого не надсилає для цієї молитви."
    override val muezzinTitle = "Муедзин"
    override val fajrAdhanTitle = "Азан на Фаджр"
    override val fajrAdhanHint = "До азану на Фаджр додається «Ас-саляту хайрун міна-н-наум» (молитва краща за сон)."
    override val fajrSameAsOthers = "Як для інших молитов"
    override val muezzinNames = mapOf(
        "asim" to "Асім Джавед",
        "klcc" to "Мечеть KLCC, Куала-Лумпур",
        "mirza" to "Моаззам-бег Мірза, Ахмадабад",
        "adam" to "Адам (Вікісховище)",
        "kalkan_fajr" to "Фаджр — Калкан, Туреччина",
        "madinah" to "Мечеть Пророка, Медина",
        "andrewler" to "Andrewler (Вікісховище)",
        "aaqib" to "Акіб Азіз",
        "fouad" to "Фуад Адан",
        "fouad_fajr" to "Фаджр — Фуад Адан",
        "kas_fajr" to "Фаджр — Каш, Туреччина",
        "malmo_fajr" to "Фаджр — Бесім Аземі, Мальме",
        "shalqar" to "Мечеть Шалкара, Казахстан",
    )
    override val playAdhan = "Слухати азан"
    override val stopAdhan = "Зупинити"
    override val vibrationTitle = "Вібрація"
    override val vibeNames = mapOf("short" to "Коротка", "double" to "Подвійна", "triple" to "Потрійна", "long" to "Довга", "heartbeat" to "Серцебиття", "pulse" to "3 імпульси", "wave" to "Хвиля", "rise" to "Наростаюча", "gentle" to "М’яка", "strong" to "Сильна", "knock" to "Стукіт", "rapid" to "Часта")
    override val tryVibration = "Перевірити вібрацію"
    override val adhanStopHint = "Щоб зупинити азан, натисніть «Зупинити» у сповіщенні або кнопку гучності. Якщо телефон у беззвучному режимі чи на вібрації, сповіщення слідує цьому режиму."
    override val adhanIosNote = "iPhone обмежує звук сповіщення 30 секундами, тож у сповіщенні звучить початок азану, а повністю азан звучить, якщо Мізан відкритий під час молитви. Сповіщення вібрують за шаблоном самого iPhone (Параметри › Звуки й тактильні сигнали); шаблони тут відчуваються, коли Мізан відкритий."
    override val recordingSources = "Джерела записів"

    // ---------- Qibla ----------
    override val qiblaTitle = "Напрямок кібли"
    override val noCompass = "У вашому пристрої немає компаса. Спрямуйте його вручну за кутом нижче."
    override val facingQibla = "✓ Ви повернуті до кібли"
    override fun turnRight(deg: Int) = "Поверніть телефон праворуч на $deg°"
    override fun turnLeft(deg: Int) = "Поверніть телефон ліворуч на $deg°"
    override val qiblaAngle = "Кут кібли від істинної півночі"
    override fun towards(dir: String) = "Напрямок: $dir"
    override val directions = listOf("північ", "північний схід", "схід", "південний схід", "південь", "південний захід", "захід", "північний захід")
    override val location = "Місцезнаходження"
    override val distanceToKaaba = "Відстань до Кааби"
    override val phoneHeading = "Напрямок телефона"
    override val compassLowAccuracy = "Низька точність компаса: для калібрування поводьте телефоном у формі вісімки, подалі від металу й електроніки."
    override val qiblaTip = "Щоб змінити місцезнаходження, відкрийте сторінку «Час намазу». Для точності тримайте телефон горизонтально й подалі від металу."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Електронний тасбіх"
    override val tasbihSubtitle = "Натискайте на коло, щоб рахувати"
    override val modeFree = "Вільний зікр"
    override val modeAfterPrayer = "Після намазу"
    override fun ofGoal(n: Int) = "з $n"
    override val nextDhikr = "Переходьте до наступного зікру"
    override val completedHundred = "Нехай Аллах прийме від вас ✓ Ви завершили сотню"
    override fun completedN(n: Int) = "Виконано: $n ✓"
    override val reset = "Скинути"
    override val undo = "Скасувати"
    override val chooseDhikr = "Виберіть зікр"
    override val targetCount = "Мета лічби"
    override val yourStats = "Ваша статистика"
    override val roundsDone = "кіл"
    override val todayCount = "сьогодні"
    override val totalCount = "усього"
    override val dhikrLabels = listOf(
        "Субханаллах", "Альхамдуліллях", "Аллаху акбар", "Ля іляха ілляллах", "Астагфіруллах",
        "Субханаллахі ва біхамдіхі", "Субханаллахіль-Азим", "Ля хауля ва ля куввата ілля біллях", "Салават Пророку ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Пречистий Аллах",
        "Хвала Аллаху",
        "Аллах Великий",
        "Немає божества, крім Аллаха",
        "Прошу прощення в Аллаха",
        "Пречистий Аллах, і хвала Йому",
        "Пречистий Аллах Великий",
        "Немає сили й могутності ні в кого, крім Аллаха",
        "О Аллаху, благослови Мухаммада",
    )
    override val tamamMeaning = "Немає божества, крім одного лише Аллаха, Який не має співучасника. Йому належить влада, Йому хвала, і Він над усякою річчю всемогутній."
    override val tapToCount = "Натисніть, щоб рахувати"

    // ---------- Zakat ----------
    override val zakatTitle = "Калькулятор закяту"
    override val zakatSubtitle = "2,5% від майна, що досягло нісабу й перебувало у власності місячний рік"
    override val partCash = "Готівка й рахунки"
    override val partReceivables = "Борги до повернення"
    override val partGold = "Золото"
    override val partSilver = "Срібло"
    override val partStocks = "Акції"
    override val partTrade = "Товари"
    override val enterGoldPrice = "Введіть ціну золота, щоб розрахувати нісаб"
    override val enterSilverPrice = "Введіть ціну срібла, щоб розрахувати нісаб"
    override val nisabReached = "✓ Майно досягло нісабу — закят обов'язковий"
    override val belowNisab = "Нижче нісабу — закят не обов'язковий"
    override val enterAssets = "Введіть ваше майно"
    override val zakatDue = "Сума закяту"
    override val totalAssets = "Усе майно, що підлягає закяту"
    override val deductedDebts = "Відраховані борги"
    override val netBase = "Чиста база закяту"
    override val nisabValue = "Розмір нісабу"
    override val approxEquals = "Приблизно дорівнює"
    override fun goldGrams24(g: String) = "$g г золота 24 карати"
    override val shareResult = "Поділитися результатом"
    override fun zakatShareText(zakat: String, base: String) = "Мій закят: $zakat (майно, що підлягає закяту, $base)"
    override val settingsAndPrices = "Налаштування й ціни"
    override val currency = "Валюта"
    override val nisabBasis = "Нісаб для грошей"
    override val nisabGold = "Золото (85 г)"
    override val nisabSilver = "Срібло (595 г)"
    override val goldPrice24 = "Ціна 1 г золота 24 карати"
    override val goldPriceHint = "У ювеліра або на надійному сайті цін у вашій країні"
    override val silverPrice = "Ціна 1 г срібла (необов'язково)"
    override val cashSection = "Готівка й рахунки"
    override val cashInHand = "Готівка на руках"
    override val bankBalances = "Залишки на банківських рахунках і в гаманцях"
    override val receivablesField = "Борги, які вам мають повернути і які ви очікуєте отримати"
    override val otherMoney = "Інші гроші"
    override val goldSilverSection = "Золото й срібло (у грамах)"
    override fun goldKarat(k: Int) = "Золото " + ukPlural(k.toLong(), "карат", "карати", "каратів")
    override val pureSilver = "Чисте срібло"
    override val includeJewelry = "Враховувати прикраси, які носять (думка ханафітів, обережніша)"
    override val jewelryField = "Із них прикраси, які носять (у перерахунку на 21 карат)"
    override val jewelryHint = "Не враховуються за думкою більшості: на прикраси для особистого користування закят не сплачується"
    override val investSection = "Інвестиції й торгівля"
    override val stocksTrade = "Акції для торгівлі (ринкова вартість)"
    override val stocksLong = "Довгострокові акції (частина, що підлягає закяту)"
    override val tradeGoods = "Товари для торгівлі (вартість запасів)"
    override val deductSection = "Відрахування"
    override val debtsField = "Ваші борги, строк сплати яких настає цього року"
    override val debtsHint = "Лише платежі, що припадають на рік закяту"
    override val breakdownTitle = "Закят за видами майна"
    override val zakatNote = "Ваші дані залишаються на пристрої. Вісім категорій отримувачів закяту Аллах назвав у сурі ат-Тауба (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Закят аль-Фітр"
    override val fitrSubtitle = "Сплачується до святкового намазу"
    override val foodAmountDue = "Скільки продуктів віддати"
    override val totalCashValue = "Загальна сума грошима"
    override fun forPeople(n: Int) = "за " + ukPlural(n.toLong(), "особу", "особи", "осіб")
    override val peopleCount = "За скількох осіб ви сплачуєте (разом із собою)"
    override val modeFood = "Продукти (са')"
    override val modeMoney = "Грошима"
    override val foodType = "Вид продукту"
    override val foods = mapOf(
        "rice" to "Рис", "riceSafe" to "Рис (із запасом)", "wheat" to "Пшениця",
        "dates" to "Фініки", "barley" to "Ячмінь", "other" to "Інша кількість…",
    )
    override fun approxKg(kg: String) = "близько $kg кг"
    override val saWeight = "Вага одного са' у кг"
    override val kgPrice = "Ціна за кг (необов'язково)"
    override val kgPriceHint = "Щоб оцінити вартість продуктів"
    override val saPerPerson = "Один са' на особу"
    override val approxCost = "Орієнтовна вартість"
    override val valuePerPerson = "Закят аль-Фітр на одну особу"
    override val valuePerPersonHint = "Щороку оголошується офіційним органом вашої країни"
    override val valueNote = "Сплата грошима — думка ханафітів; більшість сплачує продуктами."
    override val fitrTimingNote = "Коли сплачувати: до святкового намазу. Можна на день-два раніше, але без поважної причини не можна відкладати на після намазу. Віддається бідним і нужденним."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Фідья і каффара"
    override val fidyaSubtitle = "Обсяг годування в кілограмах і грошах"
    override val tabFidya = "Фідья за піст"
    override val tabOath = "Клятва"
    override val tabIntercourse = "Близькість"
    override val fidyaResult = "Фідья за пропущений піст"
    override fun fidyaSub(days: Int) = "За " + ukPlural(days.toLong(), "день", "дні", "днів") + " — по одному бідняку на кожен день"
    override val fidyaNote = "Для тих, хто назавжди не здатен постити, наприклад людей похилого віку чи хронічно хворих. Можна нагодувати одного бідняка за всі дні або кількох."
    override val oathResult = "Каффара за порушену клятву"
    override fun oathSub(oaths: Int) = if (oaths > 1) ukPlural(oaths.toLong(), "каффара", "каффари", "каффар") + " × 10 бідняків" else "Нагодувати 10 бідняків"
    override val oathAlternative = "Якщо не в змозі"
    override fun fastDays(n: Int) = "Постити " + ukPlural(n.toLong(), "день", "дні", "днів")
    override val oathNote = "На вибір: нагодувати десятьох бідняків, одягнути їх або звільнити раба; хто не може — постить три дні (сура аль-Маіда, 5:89)."
    override val intercourseResult = "Каффара за близькість удень у Рамадан"
    override val intercourseSub = "Нагодувати 60 бідняків, якщо немає сил постити"
    override val ifStartToday = "Якщо почати піст сьогодні, він завершиться"
    override val twoMonthsDuration = "Тривалість двох місячних місяців"
    override val intercourseNote = "По черзі: звільнити раба; якщо це неможливо — постити два місяці поспіль; якщо немає сил — нагодувати шістдесят бідняків; а також надолужити цей день і покаятися."
    override val daysField = "Кількість днів"
    override val oathsField = "Кількість каффар"
    override val feedMeasure = "Їжа на одного бідняка"
    override val muddOption = "Один мудд — близько 0,75 кг (малікіти й шафіїти)"
    override val halfSaOption = "Половина са' — близько 1,5 кг (сучасні фетви)"
    override val poorCount = "Бідняків / порцій"
    override val approxValue = "Орієнтовна вартість"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Дата за хіджрою"
    override val hijriSubtitle = "Календар Умм аль-Кура"
    override val correspondingHijri = "Відповідна дата за хіджрою"
    override val correspondingGreg = "Відповідна григоріанська дата"
    override val enterGregYear = "Введіть рік від 1882 до 2174"
    override val enterHijriYear = "Введіть рік хіджри від 1300 до 1600"
    override val gregToHijri = "Григоріанський → хіджра"
    override val hijriToGreg = "Хіджра → григоріанський"
    override val todayDate = "Сьогоднішня дата"
    override val prevMonth = "Попередній місяць"
    override val nextMonth = "Наступний місяць"
    override val calendarHint = "Велике число — дата за хіджрою, мале — григоріанська. Натисніть на день, щоб перевести його."
    override val moonNote = "У деяких країнах дата може відрізнятися на день через спостереження молодика."

    // ---------- Age ----------
    override val ageTitle = "Калькулятор віку"
    override val ageSubtitle = "У роках за хіджрою та григоріанських"
    override val ageGregorian = "Ваш вік (за григоріанським календарем)"
    override fun ymd(y: Int, m: Int, d: Int) = "${ukPlural(y.toLong(), "рік", "роки", "років")} ${ukPlural(m.toLong(), "місяць", "місяці", "місяців")} ${ukPlural(d.toLong(), "день", "дні", "днів")}"
    override fun inHijri(age: String) = "За хіджрою: $age"
    override val birthGreg = "Дата народження (григоріанська)"
    override val birthHijri = "Дата народження (за хіджрою)"
    override val birthWeekday = "День тижня народження"
    override val nextBirthday = "Наступний день народження"
    override val birthdayToday = "Сьогодні 🎉"
    override val tileDays = "днів"
    override val tileWeeks = "тижнів"
    override val tileHours = "годин"
    override val yourAge = "Ваш вік"
    override val birthAfterToday = "Дата народження пізніша за сьогоднішню"
    override val enterBirth = "Введіть дату народження"
    override val birthDate = "Дата народження"
    override val gregorian = "Григоріанський"
    override val hijri = "За хіджрою"

    // ---------- Islamic events ----------
    override val eventsTitle = "Ісламські дати"
    override val eventsSubtitle = "Зворотний відлік за календарем Умм аль-Кура"
    override val events = mapOf(
        "ramadan" to Named("Рамадан", "Місяць посту, нічних молитов і читання Корану."),
        "lastTen" to Named("Останні десять ночей", "Серед них — Ніч Предвизначення (Ляйлятуль-Кадр), яка краща за тисячу місяців."),
        "eidFitr" to Named("Ід аль-Фітр", "Закят аль-Фітр сплачується до святкового намазу."),
        "shawwal" to Named("Шість днів Шавваля", "Хто постив у Рамадан, а потім додав до нього шість днів Шавваля, той ніби постив усе життя (Муслім)."),
        "dhulHijjah" to Named("Перші десять днів Зуль-хіджі", "Найкращі дні цього світу; у них бажані добрі справи й такбір."),
        "arafah" to Named("День Арафа", "Піст цього дня спокутує минулий і наступний рік для тих, хто не здійснює хадж (Муслім)."),
        "eidAdha" to Named("Ід аль-Адха", "День жертвопринесення, за ним ідуть три дні ташріку."),
        "newYear" to Named("Ісламський Новий рік", "Початок нового року за хіджрою."),
        "ashura" to Named("День Ашура", "Піст цього дня спокутує минулий рік (Муслім); бажано постити разом із ним і 9-те число."),
    )
    override val unitDays = "днів"
    override val unitHours = "годин"
    override val unitMinutes = "хв"
    override val unitSeconds = "с"
    override val eventsNote = "Дати приблизні (Умм аль-Кура) і можуть відрізнятися на день залежно від спостереження місяця у вашій країні."

    // ---------- Inheritance ----------
    override val inhTitle = "Калькулятор спадщини"
    override val inhSubtitle = "Поділ спадщини за правилами фараїду"
    override val aslLabel = "Основа випадку (асль)"
    override val awlCase = "Випадок з аулем (частки перевищують спадщину)"
    override val raddCase = "Випадок з раддом (повернення надлишку)"
    override val adilCase = "Збалансований випадок"
    override fun tashihFrom(n: Long) = "Виправлено до " + ukPlural(n, "частки", "часток", "часток")
    override val sharesTitle = "Частка кожного спадкоємця"
    override fun blockedBy(by: String) = "Виключення (хаджб): $by"
    override fun perPerson(v: String) = "кожному $v"
    override val shareDivision = "Поділитися розподілом"
    override fun divisionShareText(list: String) = "Поділ спадщини: $list"
    override val listSeparator = ", "
    override val addHeirs = "Додайте спадкоємців зі списку нижче"
    override val deceasedAndEstate = "Померлий і спадщина"
    override val deceasedMale = "Померлий — чоловік"
    override val deceasedFemale = "Померла — жінка"
    override val estateTotal = "Уся спадщина"
    override val funeralCosts = "Витрати на підготовку тіла й поховання"
    override val deceasedDebts = "Борги померлого"
    override val bequest = "Заповіт (не спадкоємцю)"
    override val bequestHint = "Не більше третини того, що залишилося після боргів, якщо спадкоємці не погодяться на більше"
    override val netEstate = "Чиста спадщина до поділу"
    override val bequestCapped = "(заповіт обмежено третиною)"
    override val newCase = "Новий випадок"
    override val inhDisclaimer = "Результат — орієнтир за думкою більшості вчених. Він не замінює шаріатський суд або знавця фараїду."
    override val heirs = mapOf(
        "husband" to "Чоловік", "wife" to "Дружина", "father" to "Батько", "mother" to "Мати",
        "gf" to "Дід по батькові", "gmm" to "Бабуся по матері", "gmf" to "Бабуся по батькові",
        "son" to "Син", "daughter" to "Дочка", "gson" to "Син сина", "gdau" to "Дочка сина",
        "fb" to "Повнорідний брат", "fs" to "Повнорідна сестра", "pb" to "Єдинокровний брат", "ps" to "Єдинокровна сестра",
        "mb" to "Єдиноутробний брат", "ms" to "Єдиноутробна сестра", "fnep" to "Син повнорідного брата", "pnep" to "Син єдинокровного брата",
        "fun" to "Повнорідний дядько по батькові", "pun" to "Єдинокровний дядько по батькові", "fcous" to "Син повнорідного дядька по батькові", "pcous" to "Син єдинокровного дядька по батькові",
    )
    override val heirGroups = mapOf(
        "spouses" to "Подружжя", "ascendants" to "Батьки, діди й бабусі", "descendants" to "Діти й онуки",
        "siblings" to "Брати й сестри", "distant" to "Далекі родичі-чоловіки",
    )
    override val blockers = mapOf(
        "gf" to "дід",
        "twoDaughters" to "дві дочки (дві третини вже взято)",
        "descendant" to "нащадок, що успадковує",
        "fbAfterMuadda" to "повнорідний брат (після муадди)",
        "fsTookRest" to "повнорідна сестра, що взяла залишок",
        "gfAndSiblings" to "дід із братами й сестрами",
        "twoFullSisters" to "дві повнорідні сестри",
        "fsGhayr" to "повнорідна сестра (асаба з дочками)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Фіксована частка"
        ShareType.FARD_TASIB -> "Фіксована частка + залишок"
        ShareType.FARD_TAKMILA -> "Фіксована частка (доповнення до двох третин)"
        ShareType.TASIB -> "Асаба (залишок)"
        ShareType.TASIB_BIL_GHAYR -> "Асаба через брата"
        ShareType.TASIB_MAA_GHAYR -> "Асаба разом із дочками"
        ShareType.TASIB_MAA_JADD -> "Асаба разом із дідом"
        ShareType.FARD_THEN_MUQASAMA -> "Фіксована частка, потім поділ"
        ShareType.JADD_MUQASAMA -> "З братами: поділ"
        ShareType.JADD_THIRD_ESTATE -> "З братами: третина спадщини"
        ShareType.JADD_THIRD_REST -> "З братами: третина залишку"
        ShareType.JADD_SIXTH -> "З братами: шоста частина спадщини"
    } + if (radd) " + радд" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Єдиноутробні брати й сестри ділять третину порівну, без різниці між чоловіками та жінками."
        InhNote.Umariyya -> "Це один із двох випадків «Умарійя»: мати отримує третину того, що залишилося після частки чоловіка або дружини."
        InhNote.GrandmothersShare -> "Дві бабусі ділять шосту частину."
        InhNote.FullSistersGhayr -> "Повнорідні сестри разом із дочками стають асаба."
        InhNote.Akdariyya -> "Це випадок «Акдарія»: сестрі призначається половина, дідові — шоста частина, і основа збільшується до 9; потім їхні частки об'єднуються й діляться у співвідношенні два до одного, що дає 27."
        is InhNote.JaddRule -> "За наявності братів і сестер дід отримує найвигідніше для себе: поділ, або " +
            (if (n.otherFard) "третину залишку, або шосту частину спадщини" else "третину спадщини") +
            " — думка більшості (малікіти, шафіїти, ханбаліти). Абу Ханіфа вважав, що дід, як і батько, виключає братів і сестер."
        InhNote.Muadda -> "Єдинокровних братів і сестер зараховано проти діда (муадда), потім їхня частка за потреби переходить до повнорідних братів і сестер."
        is InhNote.NothingLeft -> "Після фіксованих часток нічого не залишилося, тому спадкоємець-асаба (${nameOf(n.winner)}) нічого не отримує."
        InhNote.Mushtaraka -> "Це випадок «Муштарака» (Хімарійя): малікіти й шафіїти дозволяють повнорідним братам і сестрам ділити третину з єдиноутробними; тут результат подано за думкою ханафітів і ханбалітів."
        is InhNote.Awl -> "Фіксовані частки перевищили основу, тому її збільшено з ${n.asl} до ${n.awl} (ауль), і кожна частка пропорційно зменшилася."
        InhNote.Radd -> "Спадкоємців-асаба немає, тому надлишок повертається власникам часток пропорційно до їхніх часток, окрім подружжя."
        InhNote.SpouseOnlyRadd -> "Єдиний спадкоємець — чоловік або дружина: багато сучасних учених і низка сімейних законів повертають йому (їй) залишок; ранні вчені віддавали його далеким родичам (зав-уль-архам), а потім до скарбниці."
        InhNote.NoHeirs -> "Немає ні власників часток, ні асаба; спадщина переходить до далеких родичів (зав-уль-архам), а потім до скарбниці. Зверніться до компетентного суду."
    }

    // ---------- Quran ----------
    override val tabQuran = "Коран"
    override val quranTitle = "Священний Коран"
    override val quranSubtitle = "Натисніть на аят, щоб прочитати тафсір"
    override val surahsTab = "Сури"
    override val juzTab = "Джузи"
    override val bookmarksTab = "Збережене"
    override val continueReading = "Продовжити читання"
    override val startReading = "Почати читання"
    override val meccan = "Мекканська"
    override val medinan = "Мединська"
    override fun ayahCount(n: Int) = ukPlural(n.toLong(), "аят", "аяти", "аятів")
    override fun juzName(n: Int) = "Джуз $n"
    override fun pageName(n: Int) = "Сторінка $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · аят $ayah"
    override val surahNames = listOf(
        "Аль-Фатіха", "Аль-Бакара", "Алі Імран", "Ан-Ніса", "Аль-Маіда", "Аль-Анам",
        "Аль-Араф", "Аль-Анфаль", "Ат-Тауба", "Юнус", "Худ", "Юсуф",
        "Ар-Раад", "Ібрагім", "Аль-Хіджр", "Ан-Нахль", "Аль-Ісра", "Аль-Кахф",
        "Мар'ям", "Та Ха", "Аль-Анбія", "Аль-Хаджж", "Аль-Мумінун", "Ан-Нур",
        "Аль-Фуркан", "Аш-Шуара", "Ан-Намль", "Аль-Касас", "Аль-Анкабут", "Ар-Рум",
        "Лукман", "Ас-Саджда", "Аль-Ахзаб", "Саба", "Фатір", "Я Сін",
        "Ас-Саффат", "Сад", "Аз-Зумар", "Гафір", "Фуссилят", "Аш-Шура",
        "Аз-Зухруф", "Ад-Духан", "Аль-Джасія", "Аль-Ахкаф", "Мухаммад", "Аль-Фатх",
        "Аль-Худжурат", "Каф", "Аз-Заріят", "Ат-Тур", "Ан-Наджм", "Аль-Камар",
        "Ар-Рахман", "Аль-Вакіа", "Аль-Хадід", "Аль-Муджадила", "Аль-Хашр", "Аль-Мумтахана",
        "Ас-Сафф", "Аль-Джумуа", "Аль-Мунафікун", "Ат-Тагабун", "Ат-Талак", "Ат-Тахрім",
        "Аль-Мульк", "Аль-Калям", "Аль-Хакка", "Аль-Мааридж", "Нух", "Аль-Джинн",
        "Аль-Муззамміль", "Аль-Муддассір", "Аль-Кіяма", "Аль-Інсан", "Аль-Мурсалят", "Ан-Наба",
        "Ан-Назіат", "Абаса", "Ат-Таквір", "Аль-Інфітар", "Аль-Мутаффіфін", "Аль-Іншикак",
        "Аль-Бурудж", "Ат-Тарік", "Аль-Аля", "Аль-Гашія", "Аль-Фаджр", "Аль-Балад",
        "Аш-Шамс", "Аль-Лайль", "Ад-Духа", "Аш-Шарх", "Ат-Тін", "Аль-Алак",
        "Аль-Кадр", "Аль-Баййіна", "Аз-Залзала", "Аль-Адіят", "Аль-Каріа", "Ат-Такасур",
        "Аль-Аср", "Аль-Хумаза", "Аль-Філь", "Курайш", "Аль-Маун", "Аль-Каусар",
        "Аль-Кафірун", "Ан-Наср", "Аль-Масад", "Аль-Іхлас", "Аль-Фалак", "Ан-Нас",
    )
    override val searchQuranHint = "Пошук сури або слів в аятах…"
    override val searchQuranGroup = "Сури"
    override val searchAyahsGroup = "Аяти"
    override val mushafView = "Режим Мусхафу"
    override val ayahView = "За аятами"
    override val textLarger = "Збільшити текст"
    override val textSmaller = "Зменшити текст"
    override val tafsirTitle = "Тафсір"
    override val tafsirMuyassar = "Тафсір аль-Муяссар"
    override val tafsirJalalayn = "Тафсір аль-Джалалайн"
    override val translationOfMeanings = "Переклад смислів"
    override val bookmarkAyah = "Зберегти аят"
    override val removeBookmark = "Видалити зі збереженого"
    override val copy = "Копіювати"
    override val copied = "Скопійовано ✓"
    override val prevAyah = "Попередній аят"
    override val nextAyah = "Наступний аят"
    override val prevSurah = "Попередня сура"
    override val nextSurah = "Наступна сура"
    override val noBookmarks = "Збережених аятів поки немає. Відкрийте аят і натисніть значок закладки, щоб зберегти його."
    override val quranLoading = "Відкриваємо Мусхаф…"
    override fun hizbName(n: Int) = "Хізб $n"
    override val khatmaTitle = "Мій хатм"
    override val khatmaPlanLabel = "Завершити за"
    override fun planDays(days: Int) = if (days == 0) "Без строку" else "$days дн."
    override fun pagesRead(read: Int, total: Int) = "$read / $total стор."
    override val todayPortion = "Читання на сьогодні"
    override fun streakDays(n: Int) = "Днів поспіль: $n"
    override fun daysLeftLabel(n: Int) = "Залишилося днів: $n"
    override val portionDone = "Читання на сьогодні виконано ✓"
    override val khatmaDone = "Хатм завершено ✓ Нехай Аллах прийме"
    override val newKhatma = "Почати новий хатм"
    override fun khatmasCompleted(n: Int) = "Завершених хатмів: $n"
    override val continuePortion = "Продовжити мою частину"
    override val ayahOfDay = "Аят дня"
    override val focusMode = "Режим фокусування"
    override val exitFocus = "Вийти з режиму фокусування"
    override val silenceTitle = "Вимикати сповіщення під час читання?"
    override val silenceBody = "У режимі фокусування Мізан може вмикати «Не турбувати» й вимикати його, коли ви закінчите читати. Android один раз попросить вас дозволити це."
    override val allow = "Дозволити"
    override val notNow = "Не зараз"
    override val tafsirLibrary = "Бібліотека тафсірів"
    override val tafsirLibrarySub = "Завантажуйте тафсіри й вибирайте, які показувати, коли ви натискаєте на аят"
    override val onDevice = "На цьому пристрої"
    override val availableDownloads = "Доступно для завантаження"
    override val download = "Завантажити"
    override fun downloadingPct(pct: Int) = "Завантаження… $pct%"
    override val cancel = "Скасувати"
    override val delete = "Видалити"
    override fun deleteTafsirQ(name: String) = "Видалити «$name» з цього пристрою?"
    override val showInTafsir = "Показувати в тафсірі"
    override val builtIn = "Вбудований"
    override fun sizeMb(mb: String) = "≈ $mb МБ"
    override val downloadFailed = "Не вдалося завантажити. Перевірте підключення до інтернету."
    override val retry = "Спробувати ще раз"
    override val moreTafsirs = "Інші тафсіри"
    override val tafsirLibraryNote = "Тафсіри завантажуються з відкритої колекції tafsir_api (за даними quran.com і QUL). Інтернет використовується лише для цих завантажень; після цього тафсіри працюють офлайн."
    override fun ayahRange(from: Int, to: Int) = "Коментар до аятів $from–$to"
    override val recitation = "Читання Корану"
    override val reciter = "Читець"
    override val playRecitation = "Слухати читання"
    override val pauseRecitation = "Пауза"
    override val stopRecitation = "Зупинити читання"
    override val repeatAyah = "Повторювати кожен аят"
    override val listenFromHere = "Слухати з цього аяту"
    override val listen = "Слухати"
    override val save = "Зберегти"
    override val saved = "Збережено"
    override val ayahActionsHint = "Утримуйте аят, щоб відкрити тафсир, послухати, зберегти, скопіювати або поділитися"
    override val audioNote = "Читання відтворюється з EveryAyah.com. Прослухані аяти зберігаються на вашому пристрої й відтворюються без інтернету."
    override val audioError = "Не вдалося відтворити читання. Перевірте підключення до інтернету."
    override val hifzMode = "Заучування"
    override val revealNext = "Показати наступний"
    override val hideAll = "Сховати всі"
    override val hifzHint = "Прочитайте напам'ять, а потім торкніться прихованого аяту, щоб перевірити себе."
    override val displayTitle = "Відображення"
    override val pageColor = "Колір сторінки"
    override val toneAuto = "Авто"
    override val toneSepia = "Сепія"
    override val toneNight = "Ніч"
    override val brightness = "Яскравість"
    override val brightnessSystem = "Яскравість пристрою"
    override val textSizeLabel = "Розмір тексту"
    override val continueFocus = "Продовжити в режимі фокусування"
    override val smartPresets = "Розумні налаштування"
    override val presetListen = "Слухати"
    override val presetMemorize = "Заучувати"
    override val presetReview = "Повторювати"
    override val smartHint = "Заучувати: кожен аят 3 рази з паузою, щоб повторити за читцем. Повторювати: уся сура, трохи швидше."
    override val range = "Уривок"
    override val rangeContinue = "Далі поспіль"
    override val rangePage = "Ця сторінка"
    override val rangeSurah = "Ця сура"
    override val rangeCustom = "Власний"
    override val fromAyah = "З"
    override val toAyah = "До"
    override val repeatRange = "Повторювати уривок"
    override val pauseAfter = "Пауза після кожного аяту"
    override val off = "Вимк."
    override val pauseAyahLength = "Тривалість аяту"
    override fun seconds(n: Int) = "$n с"
    override val speed = "Швидкість"
    override val sleepTimer = "Таймер сну"
    override fun minutes(n: Int) = "$n хв"
    override val evidenceTitle = "Докази та думки вчених"
    override val evidenceSub = "Звідки походить це положення: Коран, Сунна та правові школи"
    override val proofQuran = "Коран"
    override val proofHadith = "Хадис"
    override val proofConsensus = "Іджма"
    override val proofScholars = "Думки вчених"
    override val zakatTypesTitle = "П'ять видів закяту"
    override val zakatTypesSub = "Виберіть вид майна, щоб обчислити його закят і побачити докази"
    override val zkMoneyName = "Гроші, золото й срібло"
    override val zkMoneyDesc = "Готівка, заощадження, золото й срібло: 2,5% один раз, коли нісаб протримався місячний рік"
    override val zkTradeName = "Товари й акції"
    override val zkTradeDesc = "Товари для продажу й акції, оцінені наприкінці року"
    override val zkLivestockName = "Худоба"
    override val zkLivestockDesc = "Верблюди, корови, вівці й кози на вільному випасі, що досягли нісабу"
    override val zkCropsName = "Урожай і плоди"
    override val zkCropsDesc = "Під час збору врожаю: десята або половина десятої частини, залежно від зрошення"
    override val zkMineralsName = "Корисні копалини та скарби"
    override val zkMineralsDesc = "П'ята частина скарбу, 2,5% від корисних копалин"
    override val zkCombinedNote = "Гроші, золото, срібло й товари об'єднуються в один нісаб, тому обчислюються разом на одній сторінці."
    override val camels = "Верблюди"
    override val cattle = "Корови"
    override val sheepGoats = "Вівці й кози"
    override val unitHead = "голів"
    override val livestockDue = "Закят із худоби"
    override val nothingDue = "Закят не обов'язковий"
    override fun livestockNisab(n: Int) = "Нісаб: $n"
    override val grazingCondition = "Більшу частину року пасеться на вільному пасовищі"
    override val yearCondition = "Перебуває у власності повний місячний рік"
    override val conditionsNotMet = "Закяту немає, доки не виконано обидві умови; тварин, призначених для продажу, враховують разом із товарами."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "вівця або коза"; "bintMakhad" -> "бінт махад (верблюдиця 1 року)"; "bintLabun" -> "бінт лябун (верблюдиця 2 років)"; "hiqqah" -> "хікка (верблюдиця 3 років)"; "jadhaah" -> "джаза (верблюдиця 4 років)"; "tabi" -> "табі (теля 1 року)"; "musinnah" -> "мусінна (корова 2 років)"; else -> kind }
    override val cropsWeight = "Вага врожаю"
    override val irrigation = "Зрошення"
    override val irrRain = "Без витрат (дощ, джерела)"
    override val irrIrrigated = "З витратами (насоси, куплена вода)"
    override val irrMixed = "Навпіл"
    override val cropsDue = "Закят із врожаю"
    override fun cropsNisab(kg: String) = "Нісаб: близько $kg кг"
    override val zkRate = "Ставка"
    override val rikazValue = "Вартість скарбу (ріказ)"
    override val mineralValue = "Вартість видобутих копалин"
    override val mineralsDue = "Закят із копалин і скарбу"
    override val obSkip = "Пропустити"
    override val obNext = "Далі"
    override val obStart = "Почати"
    override val obLocationTitle = "Де ви?"
    override val obLocationBody = "Ваше місцезнаходження потрібне, щоб точно розрахувати час намазу й напрямок кібли. Воно залишається на пристрої й нікуди не надсилається."
    override val locationNeeded = "Укажіть своє місцезнаходження, щоб бачити точний час намазу для вашого міста"
    override val appGuide = "Огляд застосунку"
    override val appGuideSub = "Короткий огляд того, що вміє Мізан"
    override val onboarding = listOf(
        "Ласкаво просимо до Мізану" to "Інструменти мусульманина в одному застосунку: намаз, кібла, Коран, закят і спадщина. Без реклами, і майже все працює без інтернету.",
        "Час намазу та кібла" to "Точний час для вашого міста зі сповіщеннями про азан, кібла за компасом, дата за хіджрою та ісламські дати.",
        "Священний Коран" to "Сторінки Мусхафу, тафсір і читання Корану, режими фокусування та заучування, а також щоденний хатм, що відстежує ваш поступ і серію днів.",
        "Калькулятори з доказами" to "Закят п'яти видів, спадщина та спокутування — кожне положення супроводжується доказами з Корану, Сунни та думок учених.",
        "Знайдіть будь-що" to "Скористайтеся рядком пошуку на Головній, щоб знайти інструменти, міста, сури, аяти та зікри будь-якою мовою. Цей огляд можна повторити в Налаштуваннях.",
    )
    override val quranSources = "Текст Корану (письмо Усмані) взято з проєкту Tanzil (tanzil.net). «Тафсір аль-Муяссар» (Комплекс імені короля Фахда), «Тафсір аль-Джалалайн» і переклади смислів — із сайту tanzil.net/trans. Шрифт Корану: Amiri Quran, ліцензія SIL Open Font License. Читання Корану: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Налаштування"
    override val appearance = "Оформлення"
    override val themeAuto = "Авто"
    override val themeLight = "Світла"
    override val themeDark = "Темна"
    override val appColor = "Колір застосунку"
    override val accentNames = listOf("Смарагдовий", "Бірюзовий", "Синій", "Фіолетовий", "Рожевий", "Бордовий", "Помаранчевий", "Медовий", "Коричневий", "Графітовий")
    override val language = "Мова"
    override val deviceLanguage = "Мова пристрою"
    override val use24h = "24-годинний формат"
    override val use24hDesc = "Показувати час у 24-годинному форматі"
    override val exactAlarm = "Точні сповіщення"
    override val exactAlarmDesc = "Дозвольте точні будильники, щоб сповіщення про азан надходило вчасно"
    override val about = "Про застосунок"
    override val aboutName = "Мізан — інструменти мусульманина"
    override val adhkarNow = "Зараз"
    override val dhikrEvidence = "Доказ"
    override val dhikrSource = "Джерело"
    override val dhikrGrade = "Ступінь хадису"
    override val dhikrNarrator = "Передавач"
    override val dhikrVirtue = "Чеснота"
    override val dhikrQuranRef = "Зі Священного Корану"
    override val dhikrDone = "Ви завершили ці азкари. Нехай Аллаг прийме від вас"
    override val dhikrTapHint = "Торкніться зікру, щоб рахувати, і «Доказ», щоб побачити джерело та ступінь"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, one {# раз} few {# рази} many {# разів} other {# рази}}", mapOf("n" to n), "uk")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, one {# зікр} few {# зікри} many {# зікрів} other {# зікру}}", mapOf("n" to n), "uk")
    override val moreLanguages = "Завантажити інші мови"
    override val langPackHint = "Мови завантажуються з сайту Mizan разом із перекладом Корану, а потім працюють без інтернету."
    override val update = "Оновити"
    override fun version(v: String) = "Версія $v"
    override val aboutText = "Безкоштовний застосунок з інструментами, які щодня потрібні мусульманину: закят, спадщина, час намазу, кібла, дата за хіджрою та тасбіх. " +
        "Розрахунки відповідають думці більшості правознавців, час намазу обчислюється за астрономічними формулами, а дати — за календарем Умм аль-Кура."
    override val privacy = "Конфіденційність"
    override val privacyText = "Застосунок не збирає жодних персональних даних і користується інтернетом лише на ваш запит: щоб відтворювати читання Корану та завантажувати вибрані вами тафсири, мовні пакети й записи азану. Ваше місцезнаходження використовується лише на пристрої для розрахунку часу намазу й кібли, а ваші налаштування та дані залишаються на пристрої."
    override val disclaimer = "Результати мають довідковий характер. З особистих релігійних питань звертайтеся до кваліфікованих учених та офіційних організацій."
    // ---------- المتجر والمشاركة والأذونات ----------
    override val rateApp = "Оцінити Мізан"
    override val rateAppSub = "Ваш відгук допомагає іншим знайти застосунок"
    override val shareApp = "Поділитися Мізаном"
    override val shareAppSub = "Надішліть застосунок рідним і друзям"
    override fun shareAppText(link: String) = "Мізан: час намазу, кибла, Коран, закят і спадщина в одному безкоштовному застосунку без реклами\n$link"
    override val licenses = "Джерела та ліцензії"
    override val licensesSub = "Текст Корану, тафсири, читання, записи азану та шрифт"
    override val openSettings = "Відкрити налаштування"
    override val locationOffTitle = "Доступ до геопозиції вимкнено"
    override val locationOffBody = "Мізану не дозволено використовувати вашу геопозицію. Дозвольте це в налаштуваннях або виберіть місто зі списку."
    override val notifyOffTitle = "Сповіщення вимкнено"
    override val notifyOffBody = "Мізану не дозволено надсилати сповіщення, тому сповіщення про азан не надходитимуть. Дозвольте їх у налаштуваннях."
    override val clear = "Очистити"
    override val close = "Закрити"
    // ---------- iPhone: Lock Screen and Dynamic Island ----------
    override val liveCountdown = "Зворотний відлік на екрані блокування"
    override val liveCountdownDesc = "Якщо ви виходите з Мізану менш ніж за 3 години до намазу, зворотний відлік до нього показується на екрані блокування та в Dynamic Island."
    override val liveActivitiesOff = "Live Activities для Мізану вимкнено. Увімкніть їх у Параметрах › Mizan."
}
