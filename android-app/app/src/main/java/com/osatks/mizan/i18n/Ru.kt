package com.osatks.mizan.i18n

import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Русские формы числа: 1, 21… → one; 2–4, 22–24… → few; остальные (0, 5–20, 11–14…) → many */
private fun ruPlural(n: Long, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    val w = when {
        m10 == 1L && m100 != 11L -> one
        m10 in 2L..4L && m100 !in 12L..14L -> few
        else -> many
    }
    return "$n $w"
}

/** Родительный падеж месяцев для полных дат: «15 октября 2026 г.» */
private val ruGregMonthsGen = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)
private val ruHijriMonthsGen = listOf(
    "Мухаррама", "Сафара", "Раби аль-авваля", "Раби ас-сани", "Джумада аль-уля", "Джумада аль-ахира",
    "Раджаба", "Шаабана", "Рамадана", "Шавваля", "Зуль-када", "Зуль-хиджа"
)

object Ru : Strings {
    // ---------- General ----------
    override val appName = "Мизан"
    override val appTagline = "Инструменты мусульманина"
    override val tabHome = "Главная"
    override val tabPrayer = "Намаз"
    override val tabQibla = "Кибла"
    override val tabTasbih = "Тасбих"
    override val tabSettings = "Настройки"
    override val back = "Назад"
    override val share = "Поделиться"
    override val shareSignature = "— приложение «Мизан»"
    override val today = "Сегодня"
    override val tomorrow = "Завтра"
    override fun inDays(n: Long) = "через " + ruPlural(n, "день", "дня", "дней")
    override fun daysCount(n: Long) = ruPlural(n, "день", "дня", "дней")
    override val decrease = "Уменьшить"
    override val increase = "Увеличить"
    override val noLimit = "Без лимита"
    override val noResults = "Ничего не найдено"
    override val unitGram = "г"
    override val unitKg = "кг"
    override val unitKm = "км"
    override val am = "AM"
    override val pm = "PM"

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Мухаррам", "Сафар", "Раби аль-авваль", "Раби ас-сани", "Джумада аль-уля", "Джумада аль-ахира",
        "Раджаб", "Шаабан", "Рамадан", "Шавваль", "Зуль-када", "Зуль-хиджа"
    )
    override val gregMonths = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    override val weekdays = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
    override val weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d ${ruHijriMonthsGen[m - 1]} $y г. х."
    override fun gregDate(d: Int, m: Int, y: Int) = "$d ${ruGregMonthsGen[m - 1]} $y г."
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y г. х."
    override val hijriYearSuffix = "г. х."
    override val gregYearSuffix = "г."
    override val dayLabel = "День"
    override val monthLabel = "Месяц"
    override val yearLabel = "Год"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Калькулятор закята", "Деньги, золото и акции с учётом нисаба"),
        "inheritance" to Named("Калькулятор наследства", "Раздел наследства по правилам фараида"),
        "prayer" to Named("Время намаза", "Уведомления об азане и обратный отсчёт"),
        "qibla" to Named("Направление киблы", "Компас на датчиках вашего телефона"),
        "hijri" to Named("Дата по хиджре", "Перевод дат, календарь Умм аль-Кура"),
        "events" to Named("Исламские даты", "Рамадан, оба байрама и день Арафа"),
        "tasbih" to Named("Тасбих", "Счётчик зикров с готовыми формулами"),
        "fitr" to Named("Закят аль-Фитр", "Мерой саа или деньгами"),
        "age" to Named("Калькулятор возраста", "В годах по хиджре и григорианских"),
        "fidya" to Named("Фидья и каффара", "Пропущенный пост и нарушенные клятвы"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "закят закат зякят закях садака милостыня богатство золото серебро нисаб деньги сбережения акции имущество",
        "inheritance" to "наследство наследование наследники фараид фараиз мирас завещание доли раздел",
        "prayer" to "намаз молитва салят салат азан фаджр зухр аср магриб иша время расписание",
        "qibla" to "кибла кыйбла кааба мекка компас направление",
        "hijri" to "календарь дата хиджра хиджри исламский перевод конвертер умм аль-кура",
        "events" to "рамадан рамазан ураза курбан байрам праздник арафа ашура кадр",
        "tasbih" to "зикр зикры азкары чётки четки счётчик счетчик тасбих",
        "fitr" to "фитр фитра закят садака ураза байрам саа",
        "age" to "возраст день рождения годы лет",
        "fidya" to "фидья фидия каффара каффарат искупление клятва пост кормление",
    )
    override val toolsTitle = "Инструменты"
    override fun nextPrayerIn(city: String) = "Следующий намаз · $city"
    override val searchHint = "Поиск инструментов, городов, зикров…"

    // ---------- Search ----------
    override val searchTitle = "Поиск"
    override val searchToolsGroup = "Инструменты"
    override val searchCitiesGroup = "Города"
    override val searchPrayersGroup = "Время намазов сегодня"
    override val searchEventsGroup = "Даты"
    override val searchDhikrGroup = "Зикры"
    override val searchSettingsGroup = "Настройки"
    override val searchEmpty = "Ищите инструменты, города, зикры, даты и настройки"
    override val setAsMyCity = "Использовать для времени намаза"
    override fun searchNoResults(q: String) = "По запросу «$q» ничего не найдено"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Фаджр", "sunrise" to "Восход", "dhuhr" to "Зухр",
        "asr" to "Аср", "maghrib" to "Магриб", "isha" to "Иша"
    )
    override val prayerTitle = "Время намаза"
    override fun nextPrayerLabel(name: String) = "Следующий намаз: $name"
    override fun atTime(time: String) = "в $time"
    override val prevDay = "Предыдущий день"
    override val nextDay = "Следующий день"
    override val monthTable = "Расписание на месяц"
    override val hideMonthTable = "Скрыть расписание"
    override fun monthTableTitle(m: Int, y: Int) = "Расписание на ${gregMonths[m - 1].lowercase()} $y г."
    override val colDay = "День"
    override val locationAndMethod = "Местоположение и расчёт"
    override val useMyLocation = "Использовать моё местоположение"
    override val locating = "Определяем местоположение…"
    override val orPickCity = "Или выберите город"
    override val pickCity = "Выберите город"
    override val searchCity = "Поиск города"
    override val myLocation = "Моё местоположение"
    override val calcMethod = "Метод расчёта"
    override val methods = mapOf(
        "makkah" to "Умм аль-Кура (Саудовская Аравия)",
        "mwl" to "Лига исламского мира",
        "egypt" to "Египетское управление геодезии",
        "dubai" to "ОАЭ (Дубай)",
        "kuwait" to "Кувейт",
        "qatar" to "Катар",
        "jordan" to "Иордания и Палестина",
        "morocco" to "Марокко",
        "algeria" to "Алжир",
        "tunisia" to "Тунис",
        "karachi" to "Университет исламских наук, Карачи",
        "turkey" to "Diyanet (Турция)",
        "tehran" to "Институт геофизики Тегеранского университета",
        "indonesia" to "Министерство по делам религий (Индонезия)",
        "malaysia" to "JAKIM (Малайзия)",
        "singapore" to "MUIS (Сингапур)",
        "russia" to "Духовное управление мусульман России",
        "isna" to "Северная Америка (ISNA)",
        "france" to "Союз исламских организаций Франции",
    )
    override val asrMethod = "Расчёт времени Асра"
    override val asrStandard = "Большинство (шафииты, маликиты, ханбалиты)"
    override val asrHanafi = "Ханафитский"
    override val latLng = "Широта / долгота"
    override val adhanAlert = "Уведомление об азане"
    override val adhanAlertDesc = "Уведомление при наступлении времени каждого намаза"
    override val timesMayDiffer = "Время может отличаться на минуту-две от официального расписания вашей страны."
    override val locationFound = "Местоположение определено ✓"
    override val locationFailed = "Не удалось определить местоположение. Убедитесь, что геолокация включена."
    override val locationDenied = "Доступ к местоположению не предоставлен. Выберите город из списка."
    override val cities = mapOf(
        "makkah" to "Мекка", "madinah" to "Медина", "riyadh" to "Эр-Рияд", "jeddah" to "Джидда",
        "dammam" to "Даммам", "abha" to "Абха", "dubai" to "Дубай", "abudhabi" to "Абу-Даби", "sharjah" to "Шарджа",
        "kuwait" to "Эль-Кувейт", "doha" to "Доха", "manama" to "Манама", "muscat" to "Маскат",
        "cairo" to "Каир", "alexandria" to "Александрия", "aswan" to "Асуан", "amman" to "Амман",
        "jerusalem" to "Иерусалим (аль-Кудс)", "gaza" to "Газа", "beirut" to "Бейрут", "damascus" to "Дамаск", "aleppo" to "Алеппо",
        "baghdad" to "Багдад", "basra" to "Басра", "erbil" to "Эрбиль", "sanaa" to "Сана", "aden" to "Аден",
        "khartoum" to "Хартум", "tripoli" to "Триполи", "benghazi" to "Бенгази", "tunis" to "Тунис",
        "algiers" to "Алжир", "oran" to "Оран", "rabat" to "Рабат", "casablanca" to "Касабланка",
        "marrakesh" to "Марракеш", "nouakchott" to "Нуакшот", "mogadishu" to "Могадишо", "djibouti" to "Джибути",
        "istanbul" to "Стамбул", "ankara" to "Анкара", "karachi" to "Карачи", "lahore" to "Лахор",
        "islamabad" to "Исламабад", "delhi" to "Дели", "mumbai" to "Мумбаи", "hyderabad" to "Хайдарабад",
        "dhaka" to "Дакка", "chittagong" to "Читтагонг", "kabul" to "Кабул", "tehran" to "Тегеран", "mashhad" to "Мешхед",
        "tashkent" to "Ташкент", "baku" to "Баку", "almaty" to "Алматы", "moscow" to "Москва", "kazan" to "Казань",
        "grozny" to "Грозный", "kualalumpur" to "Куала-Лумпур", "penang" to "Пинанг", "singapore" to "Сингапур",
        "brunei" to "Бандар-Сери-Бегаван", "jakarta" to "Джакарта", "surabaya" to "Сурабая", "bandung" to "Бандунг",
        "medan" to "Медан", "makassar" to "Макасар", "lagos" to "Лагос", "kano" to "Кано", "dakar" to "Дакар",
        "sarajevo" to "Сараево", "london" to "Лондон", "birmingham" to "Бирмингем", "paris" to "Париж",
        "marseille" to "Марсель", "lyon" to "Лион", "brussels" to "Брюссель", "amsterdam" to "Амстердам",
        "berlin" to "Берлин", "hamburg" to "Гамбург", "cologne" to "Кёльн", "munich" to "Мюнхен",
        "vienna" to "Вена", "stockholm" to "Стокгольм", "newyork" to "Нью-Йорк", "chicago" to "Чикаго",
        "losangeles" to "Лос-Анджелес", "toronto" to "Торонто", "montreal" to "Монреаль", "sydney" to "Сидней",
    )
    override val palestine = "Палестина"

    // ---------- Notifications ----------
    override val channelName = "Время намаза"
    override val channelDesc = "Уведомление при наступлении времени каждого намаза"
    override fun prayerNowTitle(prayer: String) = "Наступило время намаза $prayer"
    override fun prayerNowText(city: String) = "$city — спешите на намаз"

    // ---------- Qibla ----------
    override val qiblaTitle = "Направление киблы"
    override val noCompass = "В вашем устройстве нет компаса. Направьте его вручную по углу ниже."
    override val facingQibla = "✓ Вы повёрнуты к кибле"
    override fun turnRight(deg: Int) = "Поверните телефон вправо на $deg°"
    override fun turnLeft(deg: Int) = "Поверните телефон влево на $deg°"
    override val qiblaAngle = "Угол киблы от истинного севера"
    override fun towards(dir: String) = "Направление: $dir"
    override val directions = listOf("север", "северо-восток", "восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад")
    override val location = "Местоположение"
    override val distanceToKaaba = "Расстояние до Каабы"
    override val phoneHeading = "Направление телефона"
    override val compassLowAccuracy = "Низкая точность компаса: поводите телефоном восьмёркой для калибровки, вдали от металла и электроники."
    override val qiblaTip = "Чтобы сменить местоположение, откройте страницу «Время намаза». Для точности держите телефон горизонтально и вдали от металла."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Электронный тасбих"
    override val tasbihSubtitle = "Нажимайте на круг для счёта"
    override val modeFree = "Свободный зикр"
    override val modeAfterPrayer = "После намаза"
    override fun ofGoal(n: Int) = "из $n"
    override val nextDhikr = "Переходите к следующему зикру"
    override val completedHundred = "Да примет Аллах ✓ Вы завершили сотню"
    override fun completedN(n: Int) = "Выполнено: $n ✓"
    override val reset = "Сбросить"
    override val undo = "Отменить"
    override val chooseDhikr = "Выберите зикр"
    override val targetCount = "Цель счёта"
    override val yourStats = "Ваша статистика"
    override val roundsDone = "кругов"
    override val todayCount = "сегодня"
    override val totalCount = "всего"
    override val dhikrLabels = listOf(
        "Субханаллах", "Альхамдулиллях", "Аллаху акбар", "Ля иляха илляллах", "Астагфируллах",
        "Субханаллахи ва бихамдихи", "Субханаллахиль-Азым", "Ля хауля ва ля куввата илля биллях", "Салават Пророку ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Пречист Аллах",
        "Хвала Аллаху",
        "Аллах Велик",
        "Нет божества, кроме Аллаха",
        "Прошу прощения у Аллаха",
        "Пречист Аллах, и хвала Ему",
        "Пречист Аллах Великий",
        "Нет мощи и силы ни у кого, кроме Аллаха",
        "О Аллах, благослови Мухаммада",
    )
    override val tamamMeaning = "Нет божества, кроме одного лишь Аллаха, у Которого нет сотоварища. Ему принадлежит власть, Ему хвала, и Он над всякой вещью мощен."
    override val tapToCount = "Нажмите для счёта"

    // ---------- Zakat ----------
    override val zakatTitle = "Калькулятор закята"
    override val zakatSubtitle = "2,5% от имущества, достигшего нисаба, по истечении лунного года"
    override val partCash = "Наличные и счета"
    override val partReceivables = "Долги к возврату"
    override val partGold = "Золото"
    override val partSilver = "Серебро"
    override val partStocks = "Акции"
    override val partTrade = "Товары"
    override val enterGoldPrice = "Введите цену золота для расчёта нисаба"
    override val enterSilverPrice = "Введите цену серебра для расчёта нисаба"
    override val nisabReached = "✓ Имущество достигло нисаба — закят обязателен"
    override val belowNisab = "Ниже нисаба — закят не обязателен"
    override val enterAssets = "Введите ваше имущество"
    override val zakatDue = "Сумма закята"
    override val totalAssets = "Всё облагаемое имущество"
    override val deductedDebts = "Вычтенные долги"
    override val netBase = "Чистая база закята"
    override val nisabValue = "Размер нисаба"
    override val approxEquals = "Примерно равно"
    override fun goldGrams24(g: String) = "$g г золота 24 карата"
    override val shareResult = "Поделиться результатом"
    override fun zakatShareText(zakat: String, base: String) = "Мой закят: $zakat (облагаемое имущество $base)"
    override val settingsAndPrices = "Настройки и цены"
    override val currency = "Валюта"
    override val nisabBasis = "Нисаб для денег"
    override val nisabGold = "Золото (85 г)"
    override val nisabSilver = "Серебро (595 г)"
    override val goldPrice24 = "Цена 1 г золота 24 карата"
    override val goldPriceHint = "У ювелира или на надёжном сайте цен в вашей стране"
    override val silverPrice = "Цена 1 г серебра (необязательно)"
    override val cashSection = "Наличные и счета"
    override val cashInHand = "Наличные на руках"
    override val bankBalances = "Остатки на счетах и в кошельках"
    override val receivablesField = "Долги, которые вам должны вернуть"
    override val otherMoney = "Другие деньги"
    override val goldSilverSection = "Золото и серебро (в граммах)"
    override fun goldKarat(k: Int) = "Золото " + ruPlural(k.toLong(), "карат", "карата", "карат")
    override val pureSilver = "Чистое серебро"
    override val includeJewelry = "Учитывать носимые украшения (мнение ханафитов, более осторожное)"
    override val jewelryField = "Из них носимые украшения (в пересчёте на 21 карат)"
    override val jewelryHint = "Не учитываются по мнению большинства: на украшения для личного пользования закят не выплачивается"
    override val investSection = "Инвестиции и торговля"
    override val stocksTrade = "Акции для торговли (рыночная стоимость)"
    override val stocksLong = "Долгосрочные акции (облагаемая часть)"
    override val tradeGoods = "Товары для торговли (стоимость запасов)"
    override val deductSection = "Вычеты"
    override val debtsField = "Ваши долги со сроком оплаты в этом году"
    override val debtsHint = "Только платежи, приходящиеся на год закята"
    override val breakdownTitle = "Закят по видам имущества"
    override val zakatNote = "Ваши данные остаются на устройстве. Восемь категорий получателей закята Аллах назвал в суре ат-Тауба (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Закят аль-Фитр"
    override val fitrSubtitle = "Выплачивается до праздничного намаза"
    override val foodAmountDue = "Сколько продуктов отдать"
    override val totalCashValue = "Общая сумма деньгами"
    override fun forPeople(n: Int) = "за " + ruPlural(n.toLong(), "человека", "человек", "человек")
    override val peopleCount = "За скольких человек (включая себя)"
    override val modeFood = "Продукты (саа)"
    override val modeMoney = "Деньгами"
    override val foodType = "Вид продукта"
    override val foods = mapOf(
        "rice" to "Рис", "riceSafe" to "Рис (с запасом)", "wheat" to "Пшеница",
        "dates" to "Финики", "barley" to "Ячмень", "other" to "Другое количество…",
    )
    override fun approxKg(kg: String) = "около $kg кг"
    override val saWeight = "Вес одного саа в кг"
    override val kgPrice = "Цена за кг (необязательно)"
    override val kgPriceHint = "Чтобы оценить стоимость продуктов"
    override val saPerPerson = "Один саа на человека"
    override val approxCost = "Примерная стоимость"
    override val valuePerPerson = "Закят аль-Фитр на человека"
    override val valuePerPersonHint = "Ежегодно объявляется официальным органом вашей страны"
    override val valueNote = "Выплата деньгами — мнение ханафитов; большинство выплачивают продуктами."
    override val fitrTimingNote = "Когда выплачивать: до праздничного намаза. Можно на день-два раньше, но без уважительной причины нельзя откладывать на после намаза. Выдаётся бедным и нуждающимся."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Фидья и каффара"
    override val fidyaSubtitle = "Объём кормления в килограммах и деньгах"
    override val tabFidya = "Фидья"
    override val tabOath = "Клятва"
    override val tabIntercourse = "Близость"
    override val fidyaResult = "Фидья за пропущенный пост"
    override fun fidyaSub(days: Int) = "За " + ruPlural(days.toLong(), "день", "дня", "дней") + " — по бедняку на каждый день"
    override val fidyaNote = "Для тех, кто навсегда не в силах поститься, например пожилых или хронически больных. Можно накормить одного бедняка за все дни или нескольких."
    override val oathResult = "Каффара за нарушенную клятву"
    override fun oathSub(oaths: Int) = if (oaths > 1) ruPlural(oaths.toLong(), "каффара", "каффары", "каффар") + " × 10 бедняков" else "Накормить 10 бедняков"
    override val oathAlternative = "Если не в силах"
    override fun fastDays(n: Int) = "Поститься " + ruPlural(n.toLong(), "день", "дня", "дней")
    override val oathNote = "На выбор: накормить десять бедняков, одеть их или освободить раба; кто не может — постится три дня (сура аль-Маида, 5:89)."
    override val intercourseResult = "Каффара за близость днём в Рамадан"
    override val intercourseSub = "Накормить 60 бедняков, если нет сил поститься"
    override val ifStartToday = "Если начать сегодня, пост закончится"
    override val twoMonthsDuration = "Длительность двух лунных месяцев"
    override val intercourseNote = "По порядку: освободить раба; если невозможно — поститься два месяца подряд; если нет сил — накормить шестьдесят бедняков; а также восполнить день и покаяться."
    override val daysField = "Количество дней"
    override val oathsField = "Количество каффар"
    override val feedMeasure = "Еда на одного бедняка"
    override val muddOption = "Один мудд — около 0,75 кг (маликиты и шафииты)"
    override val halfSaOption = "Половина саа — около 1,5 кг (современные фетвы)"
    override val poorCount = "Бедняков / порций"
    override val approxValue = "Примерная стоимость"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Дата по хиджре"
    override val hijriSubtitle = "Календарь Умм аль-Кура"
    override val correspondingHijri = "Соответствующая дата по хиджре"
    override val correspondingGreg = "Соответствующая григорианская дата"
    override val enterGregYear = "Введите год от 1882 до 2174"
    override val enterHijriYear = "Введите год хиджры от 1300 до 1600"
    override val gregToHijri = "Григорианский → хиджра"
    override val hijriToGreg = "Хиджра → григорианский"
    override val todayDate = "Сегодняшняя дата"
    override val prevMonth = "Предыдущий месяц"
    override val nextMonth = "Следующий месяц"
    override val calendarHint = "Крупное число — дата по хиджре, мелкое — григорианская. Нажмите на день, чтобы перевести его."
    override val moonNote = "В некоторых странах дата может отличаться на день из-за наблюдения новой луны."

    // ---------- Age ----------
    override val ageTitle = "Калькулятор возраста"
    override val ageSubtitle = "В годах по хиджре и григорианских"
    override val ageGregorian = "Ваш возраст (григорианский)"
    override fun ymd(y: Int, m: Int, d: Int) = "${ruPlural(y.toLong(), "год", "года", "лет")} ${ruPlural(m.toLong(), "месяц", "месяца", "месяцев")} ${ruPlural(d.toLong(), "день", "дня", "дней")}"
    override fun inHijri(age: String) = "По хиджре: $age"
    override val birthGreg = "Дата рождения (григорианская)"
    override val birthHijri = "Дата рождения (по хиджре)"
    override val birthWeekday = "День недели рождения"
    override val nextBirthday = "Следующий день рождения"
    override val birthdayToday = "Сегодня 🎉"
    override val tileDays = "дней"
    override val tileWeeks = "недель"
    override val tileHours = "часов"
    override val yourAge = "Ваш возраст"
    override val birthAfterToday = "Дата рождения позже сегодняшней"
    override val enterBirth = "Введите дату рождения"
    override val birthDate = "Дата рождения"
    override val gregorian = "Григорианский"
    override val hijri = "По хиджре"

    // ---------- Islamic events ----------
    override val eventsTitle = "Исламские даты"
    override val eventsSubtitle = "Обратный отсчёт по календарю Умм аль-Кура"
    override val events = mapOf(
        "ramadan" to Named("Рамадан", "Месяц поста, ночных молитв и чтения Корана."),
        "lastTen" to Named("Последние десять ночей", "Среди них — Ночь Предопределения, которая лучше тысячи месяцев."),
        "eidFitr" to Named("Ураза-байрам", "Закят аль-Фитр выплачивается до праздничного намаза."),
        "shawwal" to Named("Шесть дней Шавваля", "Кто постился в Рамадан, а затем добавил к нему шесть дней Шавваля, тот словно постился всю жизнь (Муслим)."),
        "dhulHijjah" to Named("Первые десять дней Зуль-хиджа", "Лучшие дни этого мира; в них желательны благие дела и такбир."),
        "arafah" to Named("День Арафа", "Пост в этот день искупает прошлый и следующий год для тех, кто не совершает хадж (Муслим)."),
        "eidAdha" to Named("Курбан-байрам", "День жертвоприношения, за ним следуют три дня ташрика."),
        "newYear" to Named("Исламский Новый год", "Начало нового года по хиджре."),
        "ashura" to Named("День Ашура", "Пост в этот день искупает прошлый год (Муслим); желательно поститься и 9-го числа."),
    )
    override val unitDays = "дней"
    override val unitHours = "часов"
    override val unitMinutes = "мин"
    override val unitSeconds = "сек"
    override val eventsNote = "Даты приблизительные (Умм аль-Кура) и могут отличаться на день в зависимости от наблюдения луны в вашей стране."

    // ---------- Inheritance ----------
    override val inhTitle = "Калькулятор наследства"
    override val inhSubtitle = "Раздел наследства по правилам фараида"
    override val aslLabel = "Основание случая (асль)"
    override val awlCase = "Случай с аулем (доли превышают наследство)"
    override val raddCase = "Случай с раддом (возврат излишка)"
    override val adilCase = "Сбалансированный случай"
    override fun tashihFrom(n: Long) = "Исправлено до " + ruPlural(n, "доли", "долей", "долей")
    override val sharesTitle = "Доля каждого наследника"
    override fun blockedBy(by: String) = "Исключение (хаджб): $by"
    override fun perPerson(v: String) = "каждому $v"
    override val shareDivision = "Поделиться разделом"
    override fun divisionShareText(list: String) = "Раздел наследства: $list"
    override val listSeparator = ", "
    override val addHeirs = "Добавьте наследников из списка ниже"
    override val deceasedAndEstate = "Умерший и наследство"
    override val deceasedMale = "Умерший — мужчина"
    override val deceasedFemale = "Умершая — женщина"
    override val estateTotal = "Всё наследство"
    override val funeralCosts = "Расходы на подготовку и похороны"
    override val deceasedDebts = "Долги умершего"
    override val bequest = "Завещание (не наследнику)"
    override val bequestHint = "Не более трети остатка после долгов, если наследники не согласятся на большее"
    override val netEstate = "Чистое наследство к разделу"
    override val bequestCapped = "(завещание ограничено третью)"
    override val newCase = "Новый случай"
    override val inhDisclaimer = "Результат — ориентир по мнению большинства учёных. Он не заменяет шариатский суд или знатока фараида."
    override val heirs = mapOf(
        "husband" to "Муж", "wife" to "Жена", "father" to "Отец", "mother" to "Мать",
        "gf" to "Дед по отцу", "gmm" to "Бабушка по матери", "gmf" to "Бабушка по отцу",
        "son" to "Сын", "daughter" to "Дочь", "gson" to "Сын сына", "gdau" to "Дочь сына",
        "fb" to "Полнородный брат", "fs" to "Полнородная сестра", "pb" to "Единокровный брат", "ps" to "Единокровная сестра",
        "mb" to "Единоутробный брат", "ms" to "Единоутробная сестра", "fnep" to "Сын полнородного брата", "pnep" to "Сын единокровного брата",
        "fun" to "Полнородный дядя по отцу", "pun" to "Единокровный дядя по отцу", "fcous" to "Сын полнородного дяди по отцу", "pcous" to "Сын единокровного дяди по отцу",
    )
    override val heirGroups = mapOf(
        "spouses" to "Супруги", "ascendants" to "Родители, деды и бабушки", "descendants" to "Дети и внуки",
        "siblings" to "Братья и сёстры", "distant" to "Дальние родственники-мужчины",
    )
    override val blockers = mapOf(
        "gf" to "дед",
        "twoDaughters" to "две дочери (две трети уже взяты)",
        "descendant" to "наследующий потомок",
        "fbAfterMuadda" to "полнородный брат (после муадды)",
        "fsTookRest" to "полнородная сестра, взявшая остаток",
        "gfAndSiblings" to "дед с братьями и сёстрами",
        "twoFullSisters" to "две полнородные сестры",
        "fsGhayr" to "полнородная сестра (асаба с дочерьми)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Фиксированная доля"
        ShareType.FARD_TASIB -> "Фиксированная доля + остаток"
        ShareType.FARD_TAKMILA -> "Фиксированная доля (до двух третей)"
        ShareType.TASIB -> "Асаба (остаток)"
        ShareType.TASIB_BIL_GHAYR -> "Асаба через брата"
        ShareType.TASIB_MAA_GHAYR -> "Асаба вместе с дочерьми"
        ShareType.TASIB_MAA_JADD -> "Асаба вместе с дедом"
        ShareType.FARD_THEN_MUQASAMA -> "Фиксированная доля, затем раздел"
        ShareType.JADD_MUQASAMA -> "С братьями: раздел"
        ShareType.JADD_THIRD_ESTATE -> "С братьями: треть наследства"
        ShareType.JADD_THIRD_REST -> "С братьями: треть остатка"
        ShareType.JADD_SIXTH -> "С братьями: шестая часть наследства"
    } + if (radd) " + радд" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Единоутробные братья и сёстры делят треть поровну, без различия между мужчинами и женщинами."
        InhNote.Umariyya -> "Это один из двух случаев «Умарийя»: мать получает треть остатка после доли мужа или жены."
        InhNote.GrandmothersShare -> "Две бабушки делят шестую часть."
        InhNote.FullSistersGhayr -> "Полнородные сёстры вместе с дочерьми становятся асаба."
        InhNote.Akdariyya -> "Это случай «Акдария»: сестре назначается половина, деду — шестая часть, и основание увеличивается до 9 (ауль); затем их доли объединяются и делятся в соотношении два к одному, что даёт 27."
        is InhNote.JaddRule -> "При братьях и сёстрах дед получает самое выгодное для себя: раздел, или " +
            (if (n.otherFard) "треть остатка, или шестую часть наследства" else "треть наследства") +
            " — мнение большинства (маликиты, шафииты, ханбалиты). Абу Ханифа считал, что дед, как и отец, исключает братьев и сестёр."
        InhNote.Muadda -> "Единокровные братья и сёстры засчитывались против деда (муадда), затем их доля при необходимости переходит к полнородным."
        is InhNote.NothingLeft -> "После фиксированных долей ничего не осталось, поэтому наследник-асаба (${nameOf(n.winner)}) ничего не получает."
        InhNote.Mushtaraka -> "Это случай «Муштарака» (Химария): маликиты и шафииты допускают, чтобы полнородные братья и сёстры делили треть с единоутробными; здесь результат дан по мнению ханафитов и ханбалитов."
        is InhNote.Awl -> "Фиксированные доли превысили основание, поэтому оно увеличено с ${n.asl} до ${n.awl} (ауль), и каждая доля пропорционально уменьшилась."
        InhNote.Radd -> "Наследников-асаба нет, поэтому излишек возвращается обладателям долей пропорционально их долям, кроме супругов."
        InhNote.SpouseOnlyRadd -> "Единственный наследник — супруг или супруга: многие современные учёные и ряд семейных законов возвращают ему остаток; ранние учёные отдавали его дальним родственникам (зауль-архам), затем в казну."
        InhNote.NoHeirs -> "Нет ни обладателей долей, ни асаба; наследство переходит к дальним родственникам (зауль-архам), затем в казну. Обратитесь в компетентный суд."
    }

    // ---------- Quran ----------
    override val tabQuran = "Коран"
    override val quranTitle = "Священный Коран"
    override val quranSubtitle = "Нажмите на аят, чтобы прочитать тафсир"
    override val surahsTab = "Суры"
    override val juzTab = "Джузы"
    override val bookmarksTab = "Закладки"
    override val continueReading = "Продолжить чтение"
    override val startReading = "Начать чтение"
    override val meccan = "Мекканская"
    override val medinan = "Мединская"
    override fun ayahCount(n: Int) = ruPlural(n.toLong(), "аят", "аята", "аятов")
    override fun juzName(n: Int) = "Джуз $n"
    override fun pageName(n: Int) = "Страница $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · аят $ayah"
    override val surahNames = listOf(
        "Аль-Фатиха", "Аль-Бакара", "Аль Имран", "Ан-Ниса", "Аль-Маида", "Аль-Анам",
        "Аль-Араф", "Аль-Анфаль", "Ат-Тауба", "Йунус", "Худ", "Йусуф",
        "Ар-Раад", "Ибрахим", "Аль-Хиджр", "Ан-Нахль", "Аль-Исра", "Аль-Кахф",
        "Марьям", "Та Ха", "Аль-Анбийа", "Аль-Хаджж", "Аль-Муминун", "Ан-Нур",
        "Аль-Фуркан", "Аш-Шуара", "Ан-Намль", "Аль-Касас", "Аль-Анкабут", "Ар-Рум",
        "Лукман", "Ас-Саджда", "Аль-Ахзаб", "Саба", "Фатыр", "Йа Син",
        "Ас-Саффат", "Сад", "Аз-Зумар", "Гафир", "Фуссылат", "Аш-Шура",
        "Аз-Зухруф", "Ад-Духан", "Аль-Джасийа", "Аль-Ахкаф", "Мухаммад", "Аль-Фатх",
        "Аль-Худжурат", "Каф", "Аз-Зарийат", "Ат-Тур", "Ан-Наджм", "Аль-Камар",
        "Ар-Рахман", "Аль-Вакиа", "Аль-Хадид", "Аль-Муджадила", "Аль-Хашр", "Аль-Мумтахана",
        "Ас-Сафф", "Аль-Джумуа", "Аль-Мунафикун", "Ат-Тагабун", "Ат-Талак", "Ат-Тахрим",
        "Аль-Мульк", "Аль-Калам", "Аль-Хакка", "Аль-Мааридж", "Нух", "Аль-Джинн",
        "Аль-Муззаммиль", "Аль-Муддассир", "Аль-Кийама", "Аль-Инсан", "Аль-Мурсалят", "Ан-Наба",
        "Ан-Назиат", "Абаса", "Ат-Таквир", "Аль-Инфитар", "Аль-Мутаффифин", "Аль-Иншикак",
        "Аль-Бурудж", "Ат-Тарик", "Аль-Аля", "Аль-Гашийа", "Аль-Фаджр", "Аль-Балад",
        "Аш-Шамс", "Аль-Лайл", "Ад-Духа", "Аш-Шарх", "Ат-Тин", "Аль-Алак",
        "Аль-Кадр", "Аль-Баййина", "Аз-Залзала", "Аль-Адийат", "Аль-Кариа", "Ат-Такасур",
        "Аль-Аср", "Аль-Хумаза", "Аль-Филь", "Курейш", "Аль-Маун", "Аль-Каусар",
        "Аль-Кафирун", "Ан-Наср", "Аль-Масад", "Аль-Ихлас", "Аль-Фалак", "Ан-Нас",
    )
    override val searchQuranHint = "Поиск суры или слов в аятах…"
    override val searchQuranGroup = "Суры"
    override val searchAyahsGroup = "Аяты"
    override val mushafView = "Режим мусхафа"
    override val ayahView = "По аятам"
    override val textLarger = "Увеличить шрифт"
    override val textSmaller = "Уменьшить шрифт"
    override val tafsirTitle = "Тафсир"
    override val tafsirMuyassar = "Тафсир аль-Муяссар"
    override val tafsirJalalayn = "Тафсир аль-Джалалайн"
    override val translationOfMeanings = "Перевод смыслов"
    override val bookmarkAyah = "Сохранить аят"
    override val removeBookmark = "Удалить из закладок"
    override val copy = "Копировать"
    override val copied = "Скопировано ✓"
    override val prevAyah = "Предыдущий аят"
    override val nextAyah = "Следующий аят"
    override val prevSurah = "Предыдущая сура"
    override val nextSurah = "Следующая сура"
    override val noBookmarks = "Сохранённых аятов пока нет. Откройте аят и нажмите на значок закладки, чтобы сохранить его."
    override val quranLoading = "Открываем мусхаф…"
    override fun hizbName(n: Int) = "Хизб $n"
    override val khatmaTitle = "Мой хатм"
    override val khatmaPlanLabel = "Срок"
    override fun planDays(days: Int) = if (days == 0) "Без срока" else "$days дн."
    override fun pagesRead(read: Int, total: Int) = "$read / $total стр."
    override val todayPortion = "Чтение на сегодня"
    override fun streakDays(n: Int) = "Дней подряд: $n"
    override fun daysLeftLabel(n: Int) = "Осталось дней: $n"
    override val portionDone = "Чтение на сегодня выполнено ✓"
    override val khatmaDone = "Хатм завершён ✓ Да примет Аллах"
    override val newKhatma = "Начать новый хатм"
    override fun khatmasCompleted(n: Int) = "Завершённых хатмов: $n"
    override val continuePortion = "Продолжить чтение"
    override val ayahOfDay = "Аят дня"
    override val focusMode = "Режим фокуса"
    override val exitFocus = "Выйти из режима фокуса"
    override val silenceTitle = "Отключать уведомления во время чтения?"
    override val silenceBody = "В режиме фокуса Мизан может включать «Не беспокоить» и выключать его, когда вы закончите читать. Android один раз попросит разрешение."
    override val allow = "Разрешить"
    override val notNow = "Не сейчас"
    override val tafsirLibrary = "Библиотека тафсиров"
    override val tafsirLibrarySub = "Скачивайте тафсиры и выбирайте, какие показывать при нажатии на аят"
    override val onDevice = "На этом устройстве"
    override val availableDownloads = "Доступно для загрузки"
    override val download = "Скачать"
    override fun downloadingPct(pct: Int) = "Загрузка… $pct%"
    override val cancel = "Отмена"
    override val delete = "Удалить"
    override fun deleteTafsirQ(name: String) = "Удалить «$name» с устройства?"
    override val showInTafsir = "Показывать в тафсире"
    override val builtIn = "Встроенный"
    override fun sizeMb(mb: String) = "≈ $mb МБ"
    override val downloadFailed = "Не удалось скачать. Проверьте подключение к интернету."
    override val retry = "Повторить"
    override val moreTafsirs = "Другие тафсиры"
    override val tafsirLibraryNote = "Тафсиры загружаются из открытой коллекции tafsir_api (по данным quran.com и QUL). Интернет используется только для этих загрузок; после них тафсиры работают офлайн."
    override fun ayahRange(from: Int, to: Int) = "Тафсир аятов $from–$to"
    override val quranSources = "Текст Корана (начертание Усмани) взят из проекта Tanzil (tanzil.net). «Тафсир аль-Муяссар» (Комплекс имени короля Фахда), «Тафсир аль-Джалалайн» и переводы смыслов — с сайта tanzil.net/trans. Шрифт Корана: Amiri Quran, лицензия SIL Open Font License."

    // ---------- Settings ----------
    override val settingsTitle = "Настройки"
    override val appearance = "Оформление"
    override val themeAuto = "Авто"
    override val themeLight = "Светлая"
    override val themeDark = "Тёмная"
    override val language = "Язык"
    override val deviceLanguage = "Язык устройства"
    override val use24h = "24-часовой формат"
    override val use24hDesc = "Показывать время в 24-часовом формате"
    override val exactAlarm = "Точные уведомления"
    override val exactAlarmDesc = "Разрешите точные будильники, чтобы уведомление об азане приходило вовремя"
    override val about = "О приложении"
    override val aboutName = "Мизан — инструменты мусульманина"
    override fun version(v: String) = "Версия $v"
    override val aboutText = "Бесплатное приложение с инструментами, которые нужны мусульманину каждый день: закят, наследство, время намаза, кибла, дата по хиджре и тасбих. " +
        "Расчёты следуют мнению большинства правоведов, время намаза вычисляется по астрономическим формулам, а даты — по календарю Умм аль-Кура."
    override val privacy = "Конфиденциальность"
    override val privacyText = "Приложение не собирает персональные данные и использует интернет только для загрузки выбранных вами тафсиров. Местоположение используется только на вашем устройстве для расчёта времени намаза и киблы, " +
        "а ваши настройки и данные остаются на устройстве."
    override val disclaimer = "Результаты носят справочный характер. По личным религиозным вопросам обращайтесь к знающим учёным и официальным организациям."
}
