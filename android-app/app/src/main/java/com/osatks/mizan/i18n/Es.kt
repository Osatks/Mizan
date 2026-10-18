package com.osatks.mizan.i18n

import com.osatks.mizan.i18n.pack.Template
import com.osatks.mizan.core.InhNote
import com.osatks.mizan.core.ShareType

/** Plural en español: singular solo para 1 */
private fun es(n: Long, one: String, many: String) = if (n == 1L) "1 $one" else "$n $many"

object Es : Strings {
    // ---------- General ----------
    override val appName = "Mizan"
    override val appTagline = "Herramientas del musulmán"
    override val tabHome = "Inicio"
    override val tabPrayer = "Oración"
    override val tabQibla = "Qibla"
    override val tabTasbih = "Tasbih"
    override val tabSettings = "Ajustes"
    override val back = "Atrás"
    override val share = "Compartir"
    override val shareSignature = "— App Mizan"
    override val today = "Hoy"
    override val tomorrow = "Mañana"
    override fun inDays(n: Long) = "en " + es(n, "día", "días")
    override fun daysCount(n: Long) = es(n, "día", "días")
    override val decrease = "Disminuir"
    override val increase = "Aumentar"
    override val noLimit = "Sin límite"
    override val noResults = "Sin resultados"
    override val unitGram = "g"
    override val unitKg = "kg"
    override val unitKm = "km"
    override val am = "a. m."
    override val pm = "p. m."

    // ---------- Calendar ----------
    override val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Yumada al-Ula", "Yumada al-Ajira",
        "Rayab", "Shaabán", "Ramadán", "Shawwal", "Dhul Qa'da", "Dhul Hiyya"
    )
    override val gregMonths = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    /** dentro de una fecha, los meses van en minúscula: «15 de octubre de 2026» */
    private val gregMonthsInDate = gregMonths.map { it.lowercase() }
    override val weekdays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    override val weekdaysShort = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    override fun hijriDate(d: Int, m: Int, y: Int) = "$d de ${hijriMonths[m - 1]} de $y H"
    override fun gregDate(d: Int, m: Int, y: Int) = "$d de ${gregMonthsInDate[m - 1]} de $y"
    override fun hijriMonthTitle(m: Int, y: Int) = "${hijriMonths[m - 1]} $y H"
    override val hijriYearSuffix = "H"
    override val gregYearSuffix = "d. C."
    override val dayLabel = "Día"
    override val monthLabel = "Mes"
    override val yearLabel = "Año"

    // ---------- Home ----------
    override val tools = mapOf(
        "zakat" to Named("Calculadora de zakat", "Dinero, oro y acciones, con el nisab"),
        "inheritance" to Named("Calculadora de herencia", "Reparte una herencia según las reglas de los fara'id"),
        "prayer" to Named("Horarios de oración", "Avisos del adhán y cuenta regresiva en vivo"),
        "qibla" to Named("Dirección de la qibla", "Una brújula guiada por los sensores de tu teléfono"),
        "hijri" to Named("Fecha hiyri", "Convierte fechas, calendario Umm al-Qura"),
        "events" to Named("Fechas islámicas", "Ramadán, los dos Eids y Arafat"),
        "tasbih" to Named("Tasbih", "Contador de dhikr con adhkar listos"),
        "adhkar" to Named("Adhkar", "La Fortaleza del musulmán y los adhkar diarios, con sus pruebas"),
        "fitr" to Named("Zakat al-Fitr", "Por sa' o en dinero"),
        "age" to Named("Calculadora de edad", "En años hiyri y gregorianos"),
        "fidya" to Named("Fidya y kaffara", "Ayunos perdidos y juramentos rotos"),
    )
    override val toolKeywords = mapOf(
        "zakat" to "zakat zakah azaque limosna caridad sadaqa riqueza dinero oro plata nisab ahorros acciones",
        "inheritance" to "herencia sucesión sucesion herederos testamento legado faraid fara'id mirath reparto",
        "prayer" to "oración oracion rezo salat salah adhan adhán azan fajr dhuhr asr maghrib magrib isha namaz horarios hora",
        "qibla" to "kaaba caaba meca makkah brújula brujula dirección direccion orientación orientacion",
        "hijri" to "calendario fecha convertir conversión conversion hégira hegira hiyri hijri islámico islamico umm al-qura",
        "events" to "ramadán ramadan eid fiesta fiestas arafat arafah ashura achura laylat al-qadr",
        "tasbih" to "dhikr zikr contador misbaha rosario cuentas adhkar invocaciones",
        "adhkar" to "adhkar dhikr invocaciones dua fortaleza mañana tarde dormir ramadán",
        "fitr" to "fitra fitrah fitr sa eid limosna",
        "age" to "cumpleaños cumpleanos nacimiento edad años anos",
        "fidya" to "fidyah kaffara kaffarah expiación expiacion juramento ayuno alimentar",
    )
    override val toolsTitle = "Herramientas"
    override fun nextPrayerIn(city: String) = "Próxima oración · $city"
    override val searchHint = "Busca herramientas, ciudades, adhkar…"

    // ---------- Search ----------
    override val searchTitle = "Buscar"
    override val searchToolsGroup = "Herramientas"
    override val searchCitiesGroup = "Ciudades"
    override val searchPrayersGroup = "Horarios de oración de hoy"
    override val searchEventsGroup = "Fechas"
    override val searchDhikrGroup = "Adhkar"
    override val searchSettingsGroup = "Ajustes"
    override val searchEmpty = "Busca herramientas, ciudades, adhkar, fechas y ajustes"
    override val setAsMyCity = "Usar para los horarios de oración"
    override fun searchNoResults(q: String) = "Sin resultados para “$q”"

    // ---------- Prayer ----------
    override val prayers = mapOf(
        "fajr" to "Fajr", "sunrise" to "Salida del sol", "dhuhr" to "Dhuhr",
        "asr" to "Asr", "maghrib" to "Maghrib", "isha" to "Isha"
    )
    override val prayerTitle = "Horarios de oración"
    override fun nextPrayerLabel(name: String) = "Próxima oración: $name"
    override fun atTime(time: String) = "a las $time"
    override val prevDay = "Día anterior"
    override val nextDay = "Día siguiente"
    override val monthTable = "Calendario mensual"
    override val hideMonthTable = "Ocultar calendario"
    override fun monthTableTitle(m: Int, y: Int) = "Calendario de ${gregMonthsInDate[m - 1]} de $y"
    override val colDay = "Día"
    override val locationAndMethod = "Ubicación y cálculo"
    override val useMyLocation = "Usar mi ubicación actual"
    override val locating = "Buscando tu ubicación…"
    override val orPickCity = "O elige una ciudad"
    override val pickCity = "Elige una ciudad"
    override val searchCity = "Buscar una ciudad"
    override val myLocation = "Mi ubicación"
    override val calcMethod = "Método de cálculo"
    override val methods = mapOf(
        "makkah" to "Umm al-Qura (Arabia Saudita)",
        "mwl" to "Liga Mundial Musulmana",
        "egypt" to "Autoridad General Egipcia de Topografía",
        "dubai" to "EAU (Dubái)",
        "kuwait" to "Kuwait",
        "qatar" to "Catar",
        "jordan" to "Jordania y Palestina",
        "morocco" to "Marruecos",
        "algeria" to "Argelia",
        "tunisia" to "Túnez",
        "karachi" to "Universidad de Ciencias Islámicas, Karachi",
        "turkey" to "Diyanet (Turquía)",
        "tehran" to "Instituto de Geofísica, Universidad de Teherán",
        "indonesia" to "Ministerio de Asuntos Religiosos (Indonesia)",
        "malaysia" to "JAKIM (Malasia)",
        "singapore" to "MUIS (Singapur)",
        "russia" to "Administración Espiritual de los Musulmanes de Rusia",
        "isna" to "Norteamérica (ISNA)",
        "france" to "Unión de Organizaciones Islámicas de Francia",
    )
    override val asrMethod = "Cálculo del Asr"
    override val asrStandard = "Mayoría (shafi'í, malikí, hanbalí)"
    override val asrHanafi = "Hanafí"
    override val latLng = "Latitud / longitud"
    override val adhanAlert = "Aviso del adhán"
    override val adhanAlertDesc = "Una notificación al comenzar cada horario de oración"
    override val timesMayDiffer = "Los horarios pueden diferir en uno o dos minutos del calendario oficial de tu país."
    override val locationFound = "Ubicación encontrada ✓"
    override val locationFailed = "No se pudo obtener tu ubicación. Asegúrate de que los servicios de ubicación estén activados."
    override val locationDenied = "No se concedió el permiso de ubicación. Elige tu ciudad de la lista."
    override val cities = mapOf(
        "makkah" to "La Meca", "madinah" to "Medina", "riyadh" to "Riad", "jeddah" to "Yeda",
        "dammam" to "Dammam", "abha" to "Abha", "dubai" to "Dubái", "abudhabi" to "Abu Dabi", "sharjah" to "Sharjah",
        "kuwait" to "Ciudad de Kuwait", "doha" to "Doha", "manama" to "Manama", "muscat" to "Mascate",
        "cairo" to "El Cairo", "alexandria" to "Alejandría", "aswan" to "Asuán", "amman" to "Amán",
        "jerusalem" to "Jerusalén (Al-Quds)", "gaza" to "Gaza", "beirut" to "Beirut", "damascus" to "Damasco", "aleppo" to "Alepo",
        "baghdad" to "Bagdad", "basra" to "Basora", "erbil" to "Erbil", "sanaa" to "Saná", "aden" to "Adén",
        "khartoum" to "Jartum", "tripoli" to "Trípoli", "benghazi" to "Bengasi", "tunis" to "Túnez",
        "algiers" to "Argel", "oran" to "Orán", "rabat" to "Rabat", "casablanca" to "Casablanca",
        "marrakesh" to "Marrakech", "nouakchott" to "Nuakchot", "mogadishu" to "Mogadiscio", "djibouti" to "Yibuti",
        "istanbul" to "Estambul", "ankara" to "Ankara", "karachi" to "Karachi", "lahore" to "Lahore",
        "islamabad" to "Islamabad", "delhi" to "Delhi", "mumbai" to "Bombay", "hyderabad" to "Hyderabad",
        "dhaka" to "Daca", "chittagong" to "Chittagong", "kabul" to "Kabul", "tehran" to "Teherán", "mashhad" to "Mashhad",
        "tashkent" to "Taskent", "baku" to "Bakú", "almaty" to "Almaty", "moscow" to "Moscú", "kazan" to "Kazán",
        "grozny" to "Grozni", "kualalumpur" to "Kuala Lumpur", "penang" to "Penang", "singapore" to "Singapur",
        "brunei" to "Bandar Seri Begawan", "jakarta" to "Yakarta", "surabaya" to "Surabaya", "bandung" to "Bandung",
        "medan" to "Medan", "makassar" to "Makassar", "lagos" to "Lagos", "kano" to "Kano", "dakar" to "Dakar",
        "sarajevo" to "Sarajevo", "london" to "Londres", "birmingham" to "Birmingham", "paris" to "París",
        "marseille" to "Marsella", "lyon" to "Lyon", "brussels" to "Bruselas", "amsterdam" to "Ámsterdam",
        "berlin" to "Berlín", "hamburg" to "Hamburgo", "cologne" to "Colonia", "munich" to "Múnich",
        "vienna" to "Viena", "stockholm" to "Estocolmo", "newyork" to "Nueva York", "chicago" to "Chicago",
        "losangeles" to "Los Ángeles", "toronto" to "Toronto", "montreal" to "Montreal", "sydney" to "Sídney",
    )
    override val palestine = "Palestina"

    // ---------- Notifications ----------
    override val channelName = "Horarios de oración"
    override val channelDesc = "Un aviso al comenzar cada horario de oración"
    override fun prayerNowTitle(prayer: String) = "Es la hora de $prayer"
    override fun prayerNowText(city: String) = "$city — ven a la oración"

    // ---------- Qibla ----------
    override val qiblaTitle = "Dirección de la qibla"
    override val noCompass = "Tu dispositivo no tiene brújula. Oriéntalo a mano usando el ángulo de abajo."
    override val facingQibla = "✓ Estás mirando hacia la qibla"
    override fun turnRight(deg: Int) = "Gira tu teléfono $deg° a la derecha"
    override fun turnLeft(deg: Int) = "Gira tu teléfono $deg° a la izquierda"
    override val qiblaAngle = "Ángulo de la qibla desde el norte verdadero"
    override fun towards(dir: String) = "Hacia el $dir"
    override val directions = listOf("norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste")
    override val location = "Ubicación"
    override val distanceToKaaba = "Distancia a la Kaaba"
    override val phoneHeading = "Orientación del teléfono"
    override val compassLowAccuracy = "Precisión baja de la brújula: mueve tu teléfono formando un 8 para calibrarla, lejos de metales y aparatos electrónicos."
    override val qiblaTip = "Para cambiar tu ubicación, usa la página de horarios de oración. Sostén el teléfono en posición horizontal y lejos de metales para una lectura precisa."

    // ---------- Tasbih ----------
    override val tasbihTitle = "Tasbih digital"
    override val tasbihSubtitle = "Toca el círculo para contar"
    override val modeFree = "Dhikr libre"
    override val modeAfterPrayer = "Después de la oración"
    override fun ofGoal(n: Int) = "de $n"
    override val nextDhikr = "Pasar al siguiente dhikr"
    override val completedHundred = "Que Allah te lo acepte ✓ Completaste los cien"
    override fun completedN(n: Int) = "Completado: $n ✓"
    override val reset = "Reiniciar"
    override val undo = "Deshacer"
    override val chooseDhikr = "Elige un dhikr"
    override val targetCount = "Meta"
    override val yourStats = "Tus estadísticas"
    override val roundsDone = "rondas completadas"
    override val todayCount = "hoy"
    override val totalCount = "en total"
    override val dhikrLabels = listOf(
        "SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illallah", "Astaghfirullah",
        "SubhanAllahi wa bihamdih", "SubhanAllahil-Azim", "La hawla wa la quwwata illa billah", "Salawat sobre el Profeta ﷺ",
    )
    override val dhikrMeanings = listOf(
        "Glorificado sea Allah",
        "Toda alabanza pertenece a Allah",
        "Allah es el más Grande",
        "No hay más dios que Allah",
        "Pido perdón a Allah",
        "Glorificado sea Allah y alabado sea",
        "Glorificado sea Allah, el Grandioso",
        "No hay fuerza ni poder sino en Allah",
        "Oh Allah, bendice a Muhammad",
    )
    override val tamamMeaning = "No hay más dios que Allah, Único, sin asociado. Suyo es el reino y Suya es la alabanza, y Él tiene poder sobre todas las cosas."
    override val tapToCount = "Toca para contar"

    // ---------- Zakat ----------
    override val zakatTitle = "Calculadora de zakat"
    override val zakatSubtitle = "El 2,5% de la riqueza que alcanza el nisab y se posee durante un año lunar"
    override val partCash = "Efectivo y saldos"
    override val partReceivables = "Préstamos recuperables"
    override val partGold = "Oro"
    override val partSilver = "Plata"
    override val partStocks = "Acciones"
    override val partTrade = "Mercancías"
    override val enterGoldPrice = "Introduce el precio del oro para calcular el nisab"
    override val enterSilverPrice = "Introduce el precio de la plata para calcular el nisab"
    override val nisabReached = "✓ Tu riqueza alcanzó el nisab — debes pagar zakat"
    override val belowNisab = "Por debajo del nisab — no se debe zakat"
    override val enterAssets = "Introduce tus bienes"
    override val zakatDue = "Zakat a pagar"
    override val totalAssets = "Total de bienes sujetos a zakat"
    override val deductedDebts = "Deudas descontadas"
    override val netBase = "Riqueza neta sujeta a zakat"
    override val nisabValue = "Valor del nisab"
    override val approxEquals = "Equivale aproximadamente a"
    override fun goldGrams24(g: String) = "$g g de oro de 24 quilates"
    override val shareResult = "Compartir el resultado"
    override fun zakatShareText(zakat: String, base: String) = "Mi zakat a pagar: $zakat (riqueza sujeta a zakat: $base)"
    override val settingsAndPrices = "Ajustes y precios"
    override val currency = "Moneda"
    override val nisabBasis = "Nisab usado para el dinero"
    override val nisabGold = "Oro (85 g)"
    override val nisabSilver = "Plata (595 g)"
    override val goldPrice24 = "Precio de 1 g de oro de 24 quilates"
    override val goldPriceHint = "De un joyero o de un sitio de precios confiable en tu país"
    override val silverPrice = "Precio de 1 g de plata (opcional)"
    override val cashSection = "Efectivo y saldos"
    override val cashInHand = "Efectivo en mano"
    override val bankBalances = "Saldos bancarios y de billeteras"
    override val receivablesField = "Préstamos que te deben y esperas recuperar"
    override val otherMoney = "Otro dinero"
    override val goldSilverSection = "Oro y plata (en gramos)"
    override fun goldKarat(k: Int) = "Oro de $k quilates"
    override val pureSilver = "Plata pura"
    override val includeJewelry = "Incluir las joyas de uso personal (opinión hanafí, y la más prudente)"
    override val jewelryField = "De ello, joyas en uso (equivalente a 21 quilates)"
    override val jewelryHint = "Se excluyen según la opinión de la mayoría, para la cual las joyas de uso personal no pagan zakat"
    override val investSection = "Inversiones y comercio"
    override val stocksTrade = "Acciones para negociar (valor de mercado)"
    override val stocksLong = "Acciones a largo plazo (parte sujeta a zakat)"
    override val tradeGoods = "Mercancías (valor del inventario)"
    override val deductSection = "Deducciones"
    override val debtsField = "Deudas que debes y vencen este año"
    override val debtsHint = "Solo las cuotas que vencen dentro del año de zakat"
    override val breakdownTitle = "Zakat por tipo de riqueza"
    override val zakatNote = "Tus datos se quedan en tu dispositivo. Allah nombra a los ocho destinatarios de la zakat en la sura at-Tawba (9:60)."

    // ---------- Zakat al-Fitr ----------
    override val fitrTitle = "Zakat al-Fitr"
    override val fitrSubtitle = "Se entrega antes de la oración del Eid"
    override val foodAmountDue = "Cantidad de alimento a entregar"
    override val totalCashValue = "Valor total en dinero"
    override fun forPeople(n: Int) = if (n == 1) "para 1 persona" else "para $n personas"
    override val peopleCount = "Personas por las que pagas (incluyéndote)"
    override val modeFood = "Alimento (sa')"
    override val modeMoney = "Valor en dinero"
    override val foodType = "Tipo de alimento"
    override val foods = mapOf(
        "rice" to "Arroz", "riceSafe" to "Arroz (más prudente)", "wheat" to "Trigo",
        "dates" to "Dátiles", "barley" to "Cebada", "other" to "Otra cantidad…",
    )
    override fun approxKg(kg: String) = "unos $kg kg"
    override val saWeight = "Peso de un sa' en kg"
    override val kgPrice = "Precio por kg (opcional)"
    override val kgPriceHint = "Para estimar cuánto costará el alimento"
    override val saPerPerson = "Un sa' por persona"
    override val approxCost = "Costo estimado"
    override val valuePerPerson = "Valor de la zakat al-Fitr por persona"
    override val valuePerPersonHint = "Lo anuncia cada año el organismo oficial de tu país"
    override val valueNote = "Pagar el valor en dinero es la opinión hanafí; la mayoría entrega alimento."
    override val fitrTimingNote = "Cuándo entregarla: antes de la oración del Eid. Puede entregarse uno o dos días antes, pero no debe retrasarse más allá de la oración sin excusa. Se destina a los pobres y necesitados."

    // ---------- Fidya & Kaffarah ----------
    override val fidyaTitle = "Fidya y kaffara"
    override val fidyaSubtitle = "Cantidades de alimento en kilogramos y en valor"
    override val tabFidya = "Fidya del ayuno"
    override val tabOath = "Juramento"
    override val tabIntercourse = "Relaciones"
    override val fidyaResult = "Fidya por ayunos perdidos"
    override fun fidyaSub(days: Int) = "Por ${es(days.toLong(), "día", "días")} — un pobre por cada día"
    override val fidyaNote = "Para quien no puede ayunar de forma permanente, como los ancianos o los enfermos crónicos. Se puede alimentar a un solo pobre por todos los días, o a varias personas."
    override val oathResult = "Kaffara por un juramento roto"
    override fun oathSub(oaths: Int) = if (oaths > 1) "$oaths kaffaras × 10 pobres" else "Alimentar a 10 pobres"
    override val oathAlternative = "Si no puedes"
    override fun fastDays(n: Int) = "Ayunar " + es(n.toLong(), "día", "días")
    override val oathNote = "Elige entre alimentar a diez pobres, vestirlos o liberar a un esclavo; quien no pueda, ayuna tres días (sura al-Ma'ida 5:89)."
    override val intercourseResult = "Kaffara por relaciones conyugales durante un día de Ramadán"
    override val intercourseSub = "Alimentar a 60 pobres si no puedes ayunar"
    override val ifStartToday = "Si empiezas a ayunar hoy, terminas el"
    override val twoMonthsDuration = "Duración de los dos meses lunares"
    override val intercourseNote = "En este orden: liberar a un esclavo; si no es posible, ayunar dos meses consecutivos; si no se puede, alimentar a sesenta pobres — además de recuperar el día y arrepentirse."
    override val daysField = "Número de días"
    override val oathsField = "Número de kaffaras"
    override val feedMeasure = "Alimento por cada pobre"
    override val muddOption = "Un mudd — unos 0,75 kg (malikíes y shafi'íes)"
    override val halfSaOption = "Medio sa' — unos 1,5 kg (fatwas contemporáneas)"
    override val poorCount = "Pobres / comidas"
    override val approxValue = "Valor aproximado"

    // ---------- Hijri converter ----------
    override val hijriTitle = "Fecha hiyri"
    override val hijriSubtitle = "Calendario Umm al-Qura"
    override val correspondingHijri = "Fecha hiyri correspondiente"
    override val correspondingGreg = "Fecha gregoriana correspondiente"
    override val enterGregYear = "Introduce un año entre 1882 y 2174"
    override val enterHijriYear = "Introduce un año hiyri entre 1300 y 1600"
    override val gregToHijri = "Gregoriano → Hiyri"
    override val hijriToGreg = "Hiyri → Gregoriano"
    override val todayDate = "Fecha de hoy"
    override val prevMonth = "Mes anterior"
    override val nextMonth = "Mes siguiente"
    override val calendarHint = "El número grande es el hiyri y el pequeño, el gregoriano. Toca un día para convertirlo."
    override val moonNote = "La fecha puede variar un día en algunos países debido al avistamiento de la luna."

    // ---------- Age ----------
    override val ageTitle = "Calculadora de edad"
    override val ageSubtitle = "En años hiyri y gregorianos"
    override val ageGregorian = "Tu edad (gregoriana)"
    override fun ymd(y: Int, m: Int, d: Int) = "${es(y.toLong(), "año", "años")} ${es(m.toLong(), "mes", "meses")} ${es(d.toLong(), "día", "días")}"
    override fun inHijri(age: String) = "Hiyri: $age"
    override val birthGreg = "Fecha de nacimiento (gregoriana)"
    override val birthHijri = "Fecha de nacimiento (hiyri)"
    override val birthWeekday = "Día de nacimiento"
    override val nextBirthday = "Próximo cumpleaños"
    override val birthdayToday = "Hoy 🎉"
    override val tileDays = "días"
    override val tileWeeks = "semanas"
    override val tileHours = "horas"
    override val yourAge = "Tu edad"
    override val birthAfterToday = "La fecha de nacimiento es posterior a hoy"
    override val enterBirth = "Introduce tu fecha de nacimiento"
    override val birthDate = "Fecha de nacimiento"
    override val gregorian = "Gregoriano"
    override val hijri = "Hiyri"

    // ---------- Islamic events ----------
    override val eventsTitle = "Fechas islámicas"
    override val eventsSubtitle = "Cuenta regresiva según el calendario Umm al-Qura"
    override val events = mapOf(
        "ramadan" to Named("Ramadán", "El mes del ayuno, la oración nocturna y la recitación del Corán."),
        "lastTen" to Named("Las últimas diez noches", "Entre ellas está Laylat al-Qadr, mejor que mil meses."),
        "eidFitr" to Named("Eid al-Fitr", "La zakat al-Fitr se entrega antes de su oración."),
        "shawwal" to Named("Seis días de Shawwal", "Quien ayuna Ramadán y le sigue con seis días de Shawwal, es como si hubiera ayunado toda la vida (Muslim)."),
        "dhulHijjah" to Named("Primeros diez días de Dhul Hiyya", "Los mejores días del mundo; se recomiendan las buenas obras y el takbir."),
        "arafah" to Named("Día de Arafat", "Ayunarlo expía el año pasado y el siguiente, para quien no está en el Hayy (Muslim)."),
        "eidAdha" to Named("Eid al-Adha", "El día del sacrificio, seguido de los tres días de Tashriq."),
        "newYear" to Named("Año Nuevo islámico", "El comienzo de un nuevo año hiyri."),
        "ashura" to Named("Día de Ashura", "Ayunarlo expía el año pasado (Muslim); se recomienda ayunar también el día 9."),
    )
    override val unitDays = "días"
    override val unitHours = "horas"
    override val unitMinutes = "min"
    override val unitSeconds = "s"
    override val eventsNote = "Las fechas son aproximadas (Umm al-Qura) y pueden variar un día según el avistamiento de la luna en tu país."

    // ---------- Inheritance ----------
    override val inhTitle = "Calculadora de herencia"
    override val inhSubtitle = "Reparte una herencia según las reglas de los fara'id"
    override val aslLabel = "Base del caso (asl)"
    override val awlCase = "Caso con 'awl (las cuotas superan la herencia)"
    override val raddCase = "Caso con radd (se devuelve el excedente)"
    override val adilCase = "Caso equilibrado"
    override fun tashihFrom(n: Long) = "Corregido a $n partes"
    override val sharesTitle = "La parte de cada heredero"
    override fun blockedBy(by: String) = "Excluido por $by"
    override fun perPerson(v: String) = "$v cada uno"
    override val shareDivision = "Compartir el reparto"
    override fun divisionShareText(list: String) = "Reparto de la herencia: $list"
    override val listSeparator = ", "
    override val addHeirs = "Añade los herederos de la lista de abajo"
    override val deceasedAndEstate = "Difunto y herencia"
    override val deceasedMale = "El difunto es hombre"
    override val deceasedFemale = "La difunta es mujer"
    override val estateTotal = "Herencia total"
    override val funeralCosts = "Gastos del funeral y del entierro"
    override val deceasedDebts = "Deudas del difunto"
    override val bequest = "Legado (a quien no es heredero)"
    override val bequestHint = "No más de un tercio de lo que queda tras las deudas, salvo que los herederos lo acepten"
    override val netEstate = "Herencia neta a repartir"
    override val bequestCapped = "(legado limitado a un tercio)"
    override val newCase = "Empezar un caso nuevo"
    override val inhDisclaimer = "El resultado es una guía basada en la opinión de la mayoría. No sustituye a un tribunal islámico (de la sharía) ni a un especialista en fara'id."
    override val heirs = mapOf(
        "husband" to "Esposo", "wife" to "Esposa", "father" to "Padre", "mother" to "Madre",
        "gf" to "Abuelo paterno", "gmm" to "Abuela materna", "gmf" to "Abuela paterna",
        "son" to "Hijo", "daughter" to "Hija", "gson" to "Hijo del hijo", "gdau" to "Hija del hijo",
        "fb" to "Hermano carnal", "fs" to "Hermana carnal", "pb" to "Medio hermano paterno", "ps" to "Media hermana paterna",
        "mb" to "Medio hermano materno", "ms" to "Media hermana materna", "fnep" to "Hijo del hermano carnal", "pnep" to "Hijo del medio hermano paterno",
        "fun" to "Tío paterno carnal", "pun" to "Medio tío paterno", "fcous" to "Hijo del tío paterno carnal", "pcous" to "Hijo del medio tío paterno",
    )
    override val heirGroups = mapOf(
        "spouses" to "Cónyuges", "ascendants" to "Padres y abuelos", "descendants" to "Hijos y nietos",
        "siblings" to "Hermanos y hermanas", "distant" to "Parientes varones lejanos",
    )
    override val blockers = mapOf(
        "gf" to "el abuelo",
        "twoDaughters" to "las dos hijas (dos tercios completos)",
        "descendant" to "un descendiente heredero",
        "fbAfterMuadda" to "el hermano carnal (tras la mu'adda)",
        "fsTookRest" to "la hermana carnal que toma el remanente",
        "gfAndSiblings" to "el abuelo y los hermanos",
        "twoFullSisters" to "las dos hermanas carnales",
        "fsGhayr" to "la hermana carnal (residuaria con las hijas)",
    )
    override fun shareType(t: ShareType, radd: Boolean) = when (t) {
        ShareType.FARD -> "Cuota fija"
        ShareType.FARD_TASIB -> "Cuota fija + remanente"
        ShareType.FARD_TAKMILA -> "Cuota fija (completa los dos tercios)"
        ShareType.TASIB -> "Residuario"
        ShareType.TASIB_BIL_GHAYR -> "Residuario por medio de un hermano"
        ShareType.TASIB_MAA_GHAYR -> "Residuario con las hijas"
        ShareType.TASIB_MAA_JADD -> "Residuario con el abuelo"
        ShareType.FARD_THEN_MUQASAMA -> "Cuota fija, luego reparto"
        ShareType.JADD_MUQASAMA -> "Con los hermanos: reparto"
        ShareType.JADD_THIRD_ESTATE -> "Con los hermanos: un tercio de la herencia"
        ShareType.JADD_THIRD_REST -> "Con los hermanos: un tercio del remanente"
        ShareType.JADD_SIXTH -> "Con los hermanos: un sexto de la herencia"
    } + if (radd) " + radd" else ""
    override fun inhNote(n: InhNote) = when (n) {
        InhNote.MatSiblingsShare -> "Los medios hermanos maternos comparten un tercio a partes iguales, varones y mujeres por igual."
        InhNote.Umariyya -> "Este es uno de los dos casos 'Umariyya: la madre recibe un tercio de lo que queda tras la parte del cónyuge."
        InhNote.GrandmothersShare -> "Las dos abuelas comparten un sexto."
        InhNote.FullSistersGhayr -> "Las hermanas carnales pasan a ser residuarias junto con las hijas."
        InhNote.Akdariyya -> "Este es el caso Akdariyya: a la hermana se le da la mitad y al abuelo un sexto, lo que eleva el caso a 9; después se juntan sus partes y se reparten dos a uno, lo que da 27."
        is InhNote.JaddRule -> "Con los hermanos, el abuelo recibe lo que le sea más favorable: el reparto, o " +
            (if (n.otherFard) "un tercio del remanente, o un sexto de la herencia" else "un tercio de la herencia") +
            " — opinión de la mayoría (malikíes, shafi'íes y hanbalíes). Abu Hanifa sostuvo que el abuelo excluye a los hermanos, como el padre."
        InhNote.Muadda -> "Los medios hermanos paternos se contaron frente al abuelo (mu'adda) y luego su parte vuelve a los hermanos carnales cuando es necesario."
        is InhNote.NothingLeft -> "No queda nada después de las cuotas fijas, por lo que el heredero residuario (${nameOf(n.winner)}) no recibe nada."
        InhNote.Mushtaraka -> "Este es el caso Mushtaraka (Himariyya): malikíes y shafi'íes hacen que los hermanos carnales compartan el tercio con los hermanos maternos; el resultado aquí sigue la opinión hanafí y hanbalí."
        is InhNote.Awl -> "Las cuotas fijas superaron la base, así que el caso se elevó de ${n.asl} a ${n.awl} ('awl) y cada parte se reduce en proporción."
        InhNote.Radd -> "No hay heredero residuario, así que el excedente se devuelve a los herederos de cuota fija en proporción a sus partes, excepto a los cónyuges."
        InhNote.SpouseOnlyRadd -> "El único heredero es el cónyuge: muchos sabios contemporáneos y varias leyes de familia le devuelven el remanente; los sabios antiguos lo daban a los parientes lejanos y, en su defecto, al tesoro público."
        InhNote.NoHeirs -> "No hay heredero de cuota fija ni residuario; la herencia pasa a los parientes lejanos y, en su defecto, al tesoro público. Consulta al tribunal competente."
    }

    // ---------- Quran ----------
    override val tabQuran = "Corán"
    override val quranTitle = "El Sagrado Corán"
    override val quranSubtitle = "Toca cualquier aya para leer su tafsir"
    override val surahsTab = "Suras"
    override val juzTab = "Yuz"
    override val bookmarksTab = "Guardadas"
    override val continueReading = "Continuar leyendo"
    override val startReading = "Empezar a leer"
    override val meccan = "Mequí"
    override val medinan = "Mediní"
    override fun ayahCount(n: Int) = es(n.toLong(), "aya", "ayas")
    override fun juzName(n: Int) = "Yuz $n"
    override fun pageName(n: Int) = "Página $n"
    override fun surahAyah(surah: String, ayah: Int) = "$surah · Aya $ayah"
    override val surahNames = listOf(
        "Al-Fátiha", "Al-Báqara", "Al 'Imrán", "An-Nisá", "Al-Má'ida", "Al-An'am",
        "Al-A'raf", "Al-Anfal", "At-Tawba", "Yunus", "Hud", "Yusuf",
        "Ar-Ra'd", "Ibrahim", "Al-Hiyr", "An-Nahl", "Al-Isrá", "Al-Kahf",
        "Maryam", "Ta-Ha", "Al-Anbiyá", "Al-Hayy", "Al-Mu'minún", "An-Nur",
        "Al-Furqán", "Ash-Shu'ará", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
        "Luqmán", "As-Sayda", "Al-Ahzab", "Saba", "Fátir", "Ya-Sin",
        "As-Saffat", "Sad", "Az-Zúmar", "Gáfir", "Fussilat", "Ash-Shura",
        "Az-Zújruf", "Ad-Dujan", "Al-Yáziya", "Al-Ahqaf", "Muhammad", "Al-Fath",
        "Al-Huyurat", "Qaf", "Adh-Dhariyat", "At-Tur", "An-Naym", "Al-Qámar",
        "Ar-Rahmán", "Al-Wáqi'a", "Al-Hadid", "Al-Muyádila", "Al-Hashr", "Al-Mumtáhana",
        "As-Saff", "Al-Yumu'a", "Al-Munáfiqun", "At-Tagábun", "At-Talaq", "At-Tahrim",
        "Al-Mulk", "Al-Qálam", "Al-Haqqa", "Al-Ma'ariy", "Nuh", "Al-Yinn",
        "Al-Muzzámmil", "Al-Muddáththir", "Al-Qiyama", "Al-Insán", "Al-Mursalat", "An-Naba",
        "An-Nazi'at", "'Abasa", "At-Takwir", "Al-Infitar", "Al-Mutaffifín", "Al-Inshiqaq",
        "Al-Buruy", "At-Táriq", "Al-A'la", "Al-Gáshiya", "Al-Fayr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin", "Al-'Alaq",
        "Al-Qadr", "Al-Bayyina", "Az-Zálzala", "Al-'Adiyat", "Al-Qári'a", "At-Takázur",
        "Al-'Asr", "Al-Húmaza", "Al-Fil", "Quraysh", "Al-Ma'un", "Al-Kawzar",
        "Al-Kafirún", "An-Nasr", "Al-Masad", "Al-Ijlás", "Al-Falaq", "An-Nas",
    )
    override val searchQuranHint = "Busca una sura o palabras en las ayas…"
    override val searchQuranGroup = "Suras"
    override val searchAyahsGroup = "Ayas"
    override val mushafView = "Vista de Mushaf"
    override val ayahView = "Aya por aya"
    override val textLarger = "Texto más grande"
    override val textSmaller = "Texto más pequeño"
    override val tafsirTitle = "Tafsir"
    override val tafsirMuyassar = "Tafsir al-Muyassar"
    override val tafsirJalalayn = "Tafsir al-Jalalayn"
    override val translationOfMeanings = "Traducción de los significados"
    override val bookmarkAyah = "Guardar aya"
    override val removeBookmark = "Quitar de guardadas"
    override val copy = "Copiar"
    override val copied = "Copiado ✓"
    override val prevAyah = "Aya anterior"
    override val nextAyah = "Aya siguiente"
    override val prevSurah = "Sura anterior"
    override val nextSurah = "Sura siguiente"
    override val noBookmarks = "Aún no hay ayas guardadas. Abre una aya y toca el marcador para guardarla."
    override val quranLoading = "Abriendo el Mushaf…"
    override fun hizbName(n: Int) = "Hizb $n"
    override val khatmaTitle = "Mi khatma"
    override val khatmaPlanLabel = "Terminar en"
    override fun planDays(days: Int) = if (days == 0) "Sin plazo" else "$days días"
    override fun pagesRead(read: Int, total: Int) = "$read / $total páginas"
    override val todayPortion = "Porción de hoy"
    override fun streakDays(n: Int) = "Racha: " + es(n.toLong(), "día", "días")
    override fun daysLeftLabel(n: Int) = "Días restantes: $n"
    override val portionDone = "Porción de hoy completada ✓"
    override val khatmaDone = "Khatma completada ✓ Que Allah la acepte"
    override val newKhatma = "Empezar una nueva khatma"
    override fun khatmasCompleted(n: Int) = "Khatmas completadas: $n"
    override val continuePortion = "Continuar mi porción"
    override val ayahOfDay = "Aya del día"
    override val focusMode = "Modo concentración"
    override val exitFocus = "Salir del modo concentración"
    override val silenceTitle = "¿Silenciar las notificaciones mientras lees?"
    override val silenceBody = "En el modo concentración, Mizan puede activar No molestar y desactivarlo de nuevo cuando dejes de leer. Android te pedirá que lo permitas una vez."
    override val allow = "Permitir"
    override val notNow = "Ahora no"
    override val tafsirLibrary = "Biblioteca de tafsir"
    override val tafsirLibrarySub = "Descarga tafsires y elige cuáles aparecen al tocar una aya"
    override val onDevice = "En este dispositivo"
    override val availableDownloads = "Disponibles para descargar"
    override val download = "Descargar"
    override fun downloadingPct(pct: Int) = "Descargando… $pct%"
    override val cancel = "Cancelar"
    override val delete = "Eliminar"
    override fun deleteTafsirQ(name: String) = "¿Eliminar “$name” de este dispositivo?"
    override val showInTafsir = "Mostrar en el tafsir"
    override val builtIn = "Integrado"
    override fun sizeMb(mb: String) = "≈ $mb MB"
    override val downloadFailed = "La descarga falló. Revisa tu conexión a internet."
    override val retry = "Reintentar"
    override val moreTafsirs = "Más tafsires"
    override val tafsirLibraryNote = "Los tafsires se descargan de la colección abierta tafsir_api (de quran.com y QUL). Internet se usa solo para estas descargas; después funcionan sin conexión."
    override fun ayahRange(from: Int, to: Int) = "Comentario de las ayas $from–$to"
    override val recitation = "Recitación"
    override val reciter = "Recitador"
    override val playRecitation = "Reproducir la recitación"
    override val pauseRecitation = "Pausa"
    override val stopRecitation = "Detener la recitación"
    override val repeatAyah = "Repetir cada aya"
    override val listenFromHere = "Escuchar desde esta aya"
    override val audioNote = "Las recitaciones se reproducen desde EveryAyah.com. Las ayas que ya escuchaste se guardan en tu dispositivo y se reproducen sin internet."
    override val audioError = "No se pudo reproducir la recitación. Revisa tu conexión a internet."
    override val hifzMode = "Memorización"
    override val revealNext = "Mostrar la siguiente"
    override val hideAll = "Ocultar todo"
    override val hifzHint = "Recita de memoria y luego toca una aya oculta para comprobarla."
    override val displayTitle = "Pantalla"
    override val pageColor = "Color de página"
    override val toneAuto = "Automático"
    override val toneSepia = "Sepia"
    override val toneNight = "Noche"
    override val brightness = "Brillo"
    override val brightnessSystem = "Brillo del dispositivo"
    override val textSizeLabel = "Tamaño del texto"
    override val continueFocus = "Continuar en modo concentración"
    override val smartPresets = "Ajustes inteligentes"
    override val presetListen = "Escuchar"
    override val presetMemorize = "Memorizar"
    override val presetReview = "Repasar"
    override val smartHint = "Memorizar: cada aya 3 veces con una pausa para repetirla después del recitador. Repasar: toda la sura, un poco más rápido."
    override val range = "Rango"
    override val rangeContinue = "Seguir"
    override val rangePage = "Esta página"
    override val rangeSurah = "Esta sura"
    override val rangeCustom = "Personalizado"
    override val fromAyah = "Desde"
    override val toAyah = "Hasta"
    override val repeatRange = "Repetir el rango"
    override val pauseAfter = "Pausa después de cada aya"
    override val off = "Desactivado"
    override val pauseAyahLength = "Duración de la aya"
    override fun seconds(n: Int) = "$n s"
    override val speed = "Velocidad"
    override val sleepTimer = "Temporizador de apagado"
    override fun minutes(n: Int) = "$n min"
    override val evidenceTitle = "Pruebas y opiniones de los sabios"
    override val evidenceSub = "De dónde viene este dictamen: el Corán, la Sunna y las escuelas jurídicas"
    override val proofQuran = "Corán"
    override val proofHadith = "Hadiz"
    override val proofConsensus = "Consenso"
    override val proofScholars = "Opiniones de los sabios"
    override val zakatTypesTitle = "Los cinco tipos de zakat"
    override val zakatTypesSub = "Elige un tipo de riqueza para calcular su zakat y ver las pruebas"
    override val zkMoneyName = "Dinero, oro y plata"
    override val zkMoneyDesc = "Efectivo, ahorros, oro y plata: el 2,5% una vez que alcanzan el nisab durante un año lunar"
    override val zkTradeName = "Mercancías y acciones"
    override val zkTradeDesc = "Bienes para la venta y acciones, valorados al final del año"
    override val zkLivestockName = "Ganado"
    override val zkLivestockDesc = "Camellos, vacas, ovejas y cabras de pastoreo que alcanzan el nisab"
    override val zkCropsName = "Cosechas y frutos"
    override val zkCropsDesc = "En la cosecha: una décima parte o la mitad de una décima, según el riego"
    override val zkMineralsName = "Minerales y tesoros enterrados"
    override val zkMineralsDesc = "Una quinta parte de los tesoros enterrados, el 2,5% de los minerales"
    override val zkCombinedNote = "El dinero, el oro, la plata y las mercancías se suman en un solo nisab, por eso se calculan juntos en una misma página."
    override val camels = "Camellos"
    override val cattle = "Vacas"
    override val sheepGoats = "Ovejas y cabras"
    override val unitHead = "cabezas"
    override val livestockDue = "Zakat debida sobre el ganado"
    override val nothingDue = "No se debe zakat"
    override fun livestockNisab(n: Int) = "Nisab: $n"
    override val grazingCondition = "Pasta en pastos libres la mayor parte del año"
    override val yearCondition = "Poseído durante un año lunar completo"
    override val conditionsNotMet = "No hay zakat hasta que se cumplan ambas condiciones; los animales destinados a la venta se cuentan con las mercancías."
    override fun livestockKind(kind: String) = when (kind) { "shah" -> "oveja o cabra"; "bintMakhad" -> "bint makhad (camella de 1 año)"; "bintLabun" -> "bint labun (camella de 2 años)"; "hiqqah" -> "hiqqah (camella de 3 años)"; "jadhaah" -> "jadha'ah (camella de 4 años)"; "tabi" -> "tabi' (becerro de 1 año)"; "musinnah" -> "musinnah (vaca de 2 años)"; else -> kind }
    override val cropsWeight = "Peso de la cosecha"
    override val irrigation = "Riego"
    override val irrRain = "Sin coste (lluvia, manantiales)"
    override val irrIrrigated = "Con coste (bombas, agua comprada)"
    override val irrMixed = "Mitad y mitad"
    override val cropsDue = "Zakat debida sobre la cosecha"
    override fun cropsNisab(kg: String) = "Nisab: unos $kg kg"
    override val zkRate = "Tasa"
    override val rikazValue = "Valor del tesoro enterrado (rikaz)"
    override val mineralValue = "Valor de los minerales extraídos"
    override val mineralsDue = "Zakat debida sobre minerales y tesoros"
    override val obSkip = "Omitir"
    override val obNext = "Siguiente"
    override val obStart = "Empezar"
    override val appGuide = "Guía de la app"
    override val appGuideSub = "Un recorrido rápido por lo que Mizan puede hacer"
    override val onboarding = listOf(
        "Bienvenido a Mizan" to "Herramientas del musulmán en una sola app: oración, qibla, Corán, zakat y herencia. Sin anuncios, y casi todo funciona sin conexión.",
        "Horarios de oración y qibla" to "Horarios precisos para tu ciudad con avisos del adhán, la qibla con brújula, y la fecha hiyri y las fechas islámicas.",
        "El Sagrado Corán" to "Páginas del mushaf, tafsir y recitación, modos de concentración y memorización, y una khatma diaria que sigue tu progreso y tu racha.",
        "Calculadoras con sus pruebas" to "La zakat en sus cinco tipos, la herencia y las expiaciones: cada dictamen viene con sus pruebas del Corán, la Sunna y los sabios.",
        "Encuentra lo que sea" to "Usa la barra de búsqueda de Inicio para herramientas, ciudades, suras, ayas y adhkar, en cualquier idioma. Puedes repetir este recorrido desde Ajustes.",
    )
    override val quranSources = "Texto del Corán (escritura uzmaní) del Proyecto Tanzil (tanzil.net). Tafsir al-Muyassar (Complejo del Rey Fahd), Tafsir al-Jalalayn y las traducciones de los significados de tanzil.net/trans. Fuente del Corán: Amiri Quran, SIL Open Font License. Recitaciones: EveryAyah.com."

    // ---------- Settings ----------
    override val settingsTitle = "Ajustes"
    override val appearance = "Apariencia"
    override val themeAuto = "Automático"
    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val language = "Idioma"
    override val deviceLanguage = "Idioma del dispositivo"
    override val use24h = "Formato de 24 horas"
    override val use24hDesc = "Mostrar las horas en formato de 24 horas"
    override val exactAlarm = "Avisos a la hora exacta"
    override val exactAlarmDesc = "Permite las alarmas exactas para que el aviso del adhán llegue a tiempo"
    override val about = "Acerca de"
    override val aboutName = "Mizan — Herramientas del musulmán"
    override val adhkarNow = "Ahora"
    override val dhikrEvidence = "Prueba"
    override val dhikrSource = "Fuente"
    override val dhikrGrade = "Grado del hadiz"
    override val dhikrNarrator = "Narrador"
    override val dhikrVirtue = "Mérito"
    override val dhikrQuranRef = "Del Sagrado Corán"
    override val dhikrDone = "Has completado estos adhkar. Que Allah te lo acepte"
    override val dhikrTapHint = "Toca un dhikr para contarlo y «Prueba» para ver su fuente y grado"
    override fun dhikrTimes(n: Int) = Template.format("{n, plural, one {una vez} other {# veces}}", mapOf("n" to n), "es")
    override fun dhikrItems(n: Int) = Template.format("{n, plural, one {# invocación} other {# invocaciones}}", mapOf("n" to n), "es")
    override val moreLanguages = "Descargar más idiomas"
    override val langPackHint = "Los idiomas se descargan del sitio de Mizan junto con su traducción del Corán y luego funcionan sin conexión."
    override val update = "Actualizar"
    override fun version(v: String) = "Versión $v"
    override val aboutText = "Una app gratuita con las herramientas que un musulmán necesita cada día: zakat, herencia, horarios de oración, qibla, la fecha hiyri y un tasbih. " +
        "Los dictámenes siguen a la mayoría de los juristas, los horarios de oración usan fórmulas astronómicas y las fechas siguen el calendario Umm al-Qura."
    override val privacy = "Privacidad"
    override val privacyText = "La app no recopila ningún dato personal y usa internet solo para descargar los tafsires que elijas y para reproducir recitaciones cuando lo pidas. Tu ubicación se usa únicamente en tu dispositivo para calcular los horarios de oración y la qibla, " +
        "y tus ajustes y cifras se quedan en tu dispositivo."
    override val disclaimer = "Los resultados son una guía. Para tus propias cuestiones religiosas, consulta a sabios cualificados y a los organismos oficiales."
}
