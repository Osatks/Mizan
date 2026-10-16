package com.osatks.mizan.i18n

/**
 * الدليل وأقوال الفقهاء لكل أداة فيها حكم: آيات (تُعرض من نص المصحف وترجمة المعاني المضمّنة)،
 * وأحاديث بنصها العربي ومصدرها، والإجماع، وأقوال المذاهب حيث اختلفوا، ليطمئن المستخدم إلى مصدر كل حكم.
 *
 * النص لكل لغة؛ ما لم يُترجم يُعرض بالإنجليزية.
 */
class Tx(
    val ar: String, val en: String,
    val fr: String? = null, val tr: String? = null, val ur: String? = null, val id: String? = null, val ms: String? = null,
    val fa: String? = null, val bn: String? = null, val ru: String? = null, val de: String? = null,
    val es: String? = null, val uk: String? = null,
) {
    operator fun get(lang: Lang): String = when (lang.code) {
        "ar" -> ar; "fr" -> fr; "tr" -> tr; "ur" -> ur; "id" -> id; "ms" -> ms
        "fa" -> fa; "bn" -> bn; "ru" -> ru; "de" -> de; "es" -> es; "uk" -> uk
        else -> en
    } ?: en
}

/** نوع الدليل: QURAN آية، HADITH حديث، CONSENSUS إجماع، SCHOLARS أقوال الفقهاء (ومنها ما اختاره التطبيق) */
enum class ProofKind { QURAN, HADITH, CONSENSUS, SCHOLARS }

/**
 * نقطة في الدليل. concept: مفتاح أيقونة توضح الفكرة (nisab، year، rate…).
 * ayah: السورة والآية (من-إلى) فتُعرض الآية نفسها، و hadith: نص الحديث بالعربية.
 */
class Proof(
    val kind: ProofKind, val concept: String, val title: Tx, val body: Tx, val source: Tx,
    val ayah: Triple<Int, Int, Int>? = null, val hadith: String? = null,
)

object Evidence {
    private fun q(concept: String, surah: Int, from: Int, to: Int, title: Tx, body: Tx, source: Tx) =
        Proof(ProofKind.QURAN, concept, title, body, source, ayah = Triple(surah, from, to))
    private fun h(concept: String, text: String, title: Tx, body: Tx, source: Tx) =
        Proof(ProofKind.HADITH, concept, title, body, source, hadith = text)
    private fun f(kind: ProofKind, concept: String, title: Tx, body: Tx, source: Tx) = Proof(kind, concept, title, body, source)

    val zakatMoney = listOf(
        q("duty", 9, 103, 103,
            Tx("الزكاة فريضة", "Zakat is an obligation"),
            Tx("أمر الله نبيه ﷺ بأخذ الصدقة من الأموال تطهيراً لأصحابها، وهي الركن الثالث من أركان الإسلام.",
                "Allah commanded His Prophet ﷺ to take charity from people's wealth to purify them; it is the third pillar of Islam."),
            Tx("التوبة: 103", "At-Tawbah 9:103")),
        q("gold", 9, 34, 35,
            Tx("الوعيد في كنز الذهب والفضة", "The warning against hoarding gold and silver"),
            Tx("من كنز الذهب والفضة ولم يُخرج زكاتهما توعّده الله بالعذاب، فالزكاة واجبة في النقدين.",
                "Whoever hoards gold and silver without paying their zakat is warned of punishment, so zakat is due on gold, silver and money."),
            Tx("التوبة: 34-35", "At-Tawbah 9:34-35")),
        h("nisab", "«ليس فيما دون خمسِ أواقٍ من الوَرِق صدقة»",
            Tx("نصاب الفضة", "The silver nisab"),
            Tx("خمس أواقٍ مائتا درهم، تعادل نحو 595 غراماً من الفضة الخالصة.",
                "Five awaq are 200 dirhams, about 595 g of pure silver."),
            Tx("رواه البخاري (1447) ومسلم (979)", "Bukhari (1447) and Muslim (979)")),
        h("year", "«فإذا كانت لك مائتا درهم وحال عليها الحولُ ففيها خمسةُ دراهم، وليس عليك شيءٌ -يعني في الذهب- حتى يكون لك عشرون ديناراً، فإذا كان لك عشرون ديناراً وحال عليها الحول ففيها نصفُ دينار»",
            Tx("نصاب الذهب والحول والمقدار", "Gold nisab, the lunar year and the rate"),
            Tx("عشرون ديناراً نحو 85 غراماً من الذهب الخالص، ولا تجب حتى يمر عليها عام هجري كامل، والواجب ربع العشر (2.5%).",
                "Twenty dinars are about 85 g of pure gold. Zakat is due only after a full lunar year, and the rate is one fortieth (2.5%)."),
            Tx("رواه أبو داود (1573) وصححه الألباني", "Abu Dawud (1573), graded authentic by al-Albani")),
        f(ProofKind.CONSENSUS, "rate",
            Tx("ربع العشر بالإجماع", "One fortieth by consensus"),
            Tx("أجمع العلماء على أن زكاة الذهب والفضة والنقود ربع العشر إذا بلغت النصاب وحال عليها الحول.",
                "Scholars agree that the zakat of gold, silver and money is one fortieth once it reaches the nisab and a year has passed."),
            Tx("الإجماع لابن المنذر، والمغني لابن قدامة", "Ibn al-Mundhir, al-Ijma'; Ibn Qudamah, al-Mughni")),
        f(ProofKind.SCHOLARS, "jewelry",
            Tx("حُليّ المرأة المستعمل", "Jewellery a woman wears"),
            Tx("أوجب الحنفية الزكاة فيه، ورجّحه من المعاصرين ابن باز وابن عثيمين، ولم يوجبها جمهور المالكية والشافعية والحنابلة في الحلي المباح المعدّ للاستعمال. في التطبيق تختار بينهما.",
                "The Hanafis require zakat on it, as did Ibn Baz and Ibn Uthaymin among contemporaries; most Malikis, Shafi'is and Hanbalis exempt permissible jewellery kept for wear. The app lets you choose."),
            Tx("المغني لابن قدامة؛ فتاوى ابن باز", "Ibn Qudamah, al-Mughni; Fatawa Ibn Baz")),
        f(ProofKind.SCHOLARS, "debt",
            Tx("خصم الديون", "Deducting debts"),
            Tx("يُخصم الدين الحالّ الذي على المزكي من ماله عند الحنفية والحنابلة، وهو ما يعمل به التطبيق، وللشافعية في الجديد أنه لا يُخصم.",
                "Debts you owe that are due now are deducted from your wealth according to the Hanafis and Hanbalis, which the app follows; the later Shafi'i view does not deduct them."),
            Tx("المغني لابن قدامة؛ المجموع للنووي", "Ibn Qudamah, al-Mughni; al-Nawawi, al-Majmu'")),
    )

    val zakatTrade = listOf(
        h("trade", "«كان رسول الله ﷺ يأمرنا أن نُخرج الصدقةَ من الذي نُعِدُّ للبيع»",
            Tx("الزكاة فيما يُعدّ للبيع", "Zakat on goods held for sale"),
            Tx("في إسناده كلام، لكن عليه عمل الصحابة كعمر وابن عمر، وقال بوجوبها المذاهب الأربعة.",
                "Its chain is debated, but the Companions such as Umar and Ibn Umar acted on it, and all four schools hold zakat due on trade goods."),
            Tx("رواه أبو داود (1562)", "Abu Dawud (1562)")),
        f(ProofKind.CONSENSUS, "value",
            Tx("تُقوَّم بسعر السوق آخر الحول", "Valued at market price at the end of the year"),
            Tx("تُضم قيمة عروض التجارة إلى النقود في النصاب، ويُخرج ربع العشر من قيمتها يوم وجوب الزكاة.",
                "Trade goods are added to your money for the nisab, and one fortieth of their value on the day zakat falls due is paid."),
            Tx("الإجماع لابن المنذر؛ المغني لابن قدامة", "Ibn al-Mundhir, al-Ijma'; Ibn Qudamah, al-Mughni")),
        f(ProofKind.SCHOLARS, "shares",
            Tx("الأسهم", "Shares"),
            Tx("أسهم المضاربة تُزكّى بقيمتها السوقية كعروض التجارة، وأسهم الاستثمار طويل الأجل تُزكّى بقدر ما يقابلها من الأموال الزكوية في الشركة، كما قرر مجمع الفقه الإسلامي الدولي.",
                "Shares bought for trading are valued at market price like trade goods; long-term shares owe zakat on their portion of the company's zakatable assets, as decided by the International Islamic Fiqh Academy."),
            Tx("مجمع الفقه الإسلامي الدولي، القرار 28 (3/4)", "International Islamic Fiqh Academy, resolution 28 (3/4)")),
    )

    val zakatLivestock = listOf(
        h("camel", "«في أربعٍ وعشرين من الإبل فما دونها من الغنم، من كل خمسٍ شاة، فإذا بلغت خمساً وعشرين إلى خمسٍ وثلاثين ففيها بنتُ مخاض… وفي صدقة الغنم في سائمتها إذا كانت أربعين إلى عشرين ومائة شاةٌ»",
            Tx("كتاب أبي بكر في الصدقات", "Abu Bakr's letter on zakat"),
            Tx("الكتاب الذي كتبه أبو بكر الصديق لأنس حين وجّهه إلى البحرين، وفيه أنصبة الإبل والغنم كاملة، وعليه يحسب التطبيق.",
                "The letter Abu Bakr wrote for Anas when sending him to Bahrain, giving the full schedule for camels and sheep; the app calculates by it."),
            Tx("رواه البخاري (1454)", "Bukhari (1454)")),
        h("cattle", "«بعثني النبي ﷺ إلى اليمن، فأمرني أن آخذ من كل ثلاثين من البقر تبيعاً أو تبيعة، ومن كل أربعين مسنّة»",
            Tx("نصاب البقر", "The cattle schedule"),
            Tx("لا شيء في أقل من ثلاثين، وفي كل ثلاثين تبيع، وفي كل أربعين مسنّة.",
                "Nothing is due below thirty; for every thirty a one-year calf, and for every forty a two-year cow."),
            Tx("رواه أبو داود (1576) والترمذي (623) وحسّنه", "Abu Dawud (1576), al-Tirmidhi (623), graded hasan")),
        f(ProofKind.SCHOLARS, "grazing",
            Tx("أن تكون سائمة", "The animals must graze"),
            Tx("السائمة التي ترعى الكلأ المباح أكثر العام، وعليه الجمهور، فلا زكاة في المعلوفة ولا العوامل، وأوجبها المالكية فيهما. وما أُعدّ للتجارة يُزكّى زكاة عروض التجارة بقيمته.",
                "They must graze on open pasture most of the year, per the majority, so stall-fed and working animals owe nothing; the Malikis require it on these too. Animals held for sale are valued as trade goods."),
            Tx("المغني لابن قدامة؛ بداية المجتهد لابن رشد", "Ibn Qudamah, al-Mughni; Ibn Rushd, Bidayat al-Mujtahid")),
        f(ProofKind.CONSENSUS, "year",
            Tx("الحول", "A full year"),
            Tx("يُشترط أن يمر على النصاب عام هجري كامل وهو في ملكه، ونتاجها يتبعها في الحول.",
                "The herd must be owned at nisab for a full lunar year; their young are counted with them."),
            Tx("المغني لابن قدامة", "Ibn Qudamah, al-Mughni")),
        f(ProofKind.SCHOLARS, "age",
            Tx("أسنان الواجب", "The ages due"),
            Tx("بنت مخاض: أنثى إبل لها سنة، وبنت لبون: سنتان، وحِقّة: ثلاث، وجذعة: أربع. التبيع من البقر: له سنة، والمسنّة: سنتان. والشاة: جذعة ضأن (ستة أشهر) أو ثنيّة معز (سنة).",
                "Bint makhad: a she-camel of one year; bint labun: two; hiqqah: three; jadha'ah: four. Tabi' (cattle): one year; musinnah: two. Shah: a sheep of six months or a goat of one year."),
            Tx("المغني لابن قدامة", "Ibn Qudamah, al-Mughni")),
    )

    val zakatCrops = listOf(
        q("harvest", 6, 141, 141,
            Tx("حقها يوم الحصاد", "Its due on harvest day"),
            Tx("زكاة الزروع والثمار تجب عند الحصاد، ولا يُشترط لها حول.",
                "The zakat of crops and fruit is due at harvest; no year needs to pass."),
            Tx("الأنعام: 141", "Al-An'am 6:141")),
        h("water", "«فيما سقت السماءُ والعيونُ أو كان عَثَرِيّاً العُشر، وما سُقي بالنَّضح نصفُ العشر»",
            Tx("المقدار حسب السقي", "The rate depends on irrigation"),
            Tx("العشر (10%) فيما سُقي بلا كلفة كالمطر والعيون، ونصف العشر (5%) فيما سُقي بكلفة، وثلاثة أرباع العشر (7.5%) إذا سُقي بهما مناصفة.",
                "A tenth (10%) when watered without cost, by rain or springs; half a tenth (5%) when watered at cost; three quarters of a tenth (7.5%) when half and half."),
            Tx("رواه البخاري (1483)", "Bukhari (1483)")),
        h("nisab", "«ليس فيما دون خمسة أوسُقٍ صدقة»",
            Tx("النصاب خمسة أوسق", "The nisab is five awsuq"),
            Tx("خمسة أوسق ثلاثمائة صاع، وتقدّر بنحو 612 كيلوغراماً من القمح، وتختلف قليلاً باختلاف المحصول.",
                "Five awsuq are 300 sa', estimated at about 612 kg of wheat; it varies slightly by crop."),
            Tx("رواه البخاري (1447) ومسلم (979)", "Bukhari (1447) and Muslim (979)")),
        f(ProofKind.SCHOLARS, "crops",
            Tx("ما تجب فيه", "Which produce owes zakat"),
            Tx("عند الجمهور في الحبوب والثمار التي تُكال وتُدّخر كالقمح والشعير والأرز والتمر والزبيب، وأوجبها أبو حنيفة في كل ما تخرجه الأرض.",
                "For the majority: grains and fruit that are measured and stored, such as wheat, barley, rice, dates and raisins; Abu Hanifa required it on all produce of the land."),
            Tx("المغني لابن قدامة؛ بداية المجتهد", "Ibn Qudamah, al-Mughni; Bidayat al-Mujtahid")),
    )

    val zakatMinerals = listOf(
        h("treasure", "«العجماء جُبار، والبئر جبار، والمعدن جبار، وفي الركاز الخُمس»",
            Tx("في الركاز الخمس", "A fifth on buried treasure"),
            Tx("الركاز: ما وُجد من دفن الجاهلية، وفيه الخمس (20%) في الحال.",
                "Rikaz is treasure buried before Islam; a fifth (20%) is due immediately."),
            Tx("رواه البخاري (1499) ومسلم (1710)", "Bukhari (1499) and Muslim (1710)")),
        f(ProofKind.SCHOLARS, "year",
            Tx("لا نصاب ولا حول في الركاز", "No nisab or year for rikaz"),
            Tx("الجمهور على أن الركاز يُخمّس قليله وكثيره ولا يُنتظر به حول، واشترط الشافعية النصاب.",
                "The majority take a fifth of any amount without waiting a year; the Shafi'is require the nisab."),
            Tx("المغني لابن قدامة", "Ibn Qudamah, al-Mughni")),
        f(ProofKind.SCHOLARS, "mine",
            Tx("المعادن المستخرجة", "Minerals extracted from the earth"),
            Tx("عند الحنابلة ربع العشر في كل معدن بلغت قيمته نصاب الذهب، وخصّه المالكية والشافعية بالذهب والفضة، وجعله الحنفية كالركاز (الخمس). التطبيق يحسب ربع العشر.",
                "Hanbalis: one fortieth on any mineral worth the gold nisab; Malikis and Shafi'is limit it to gold and silver; Hanafis treat it like rikaz (a fifth). The app uses one fortieth."),
            Tx("المغني لابن قدامة؛ بدائع الصنائع", "Ibn Qudamah, al-Mughni; al-Kasani, Bada'i' al-Sana'i'")),
    )

    val fitr = listOf(
        h("people", "«فرض رسول الله ﷺ زكاة الفطر صاعاً من تمرٍ أو صاعاً من شعير، على العبد والحر، والذكر والأنثى، والصغير والكبير من المسلمين، وأمر بها أن تؤدّى قبل خروج الناس إلى الصلاة»",
            Tx("على كل مسلم صاع", "A sa' for every Muslim"),
            Tx("تجب عن المسلم وعمّن تلزمه نفقته، صاعاً من طعام أهل البلد، قبل صلاة العيد.",
                "It is due for every Muslim and those he supports: one sa' of the local staple food, before the Eid prayer."),
            Tx("رواه البخاري (1503) ومسلم (984)", "Bukhari (1503) and Muslim (984)")),
        h("time", "«طُهرةً للصائم من اللغو والرفث، وطُعمةً للمساكين، من أدّاها قبل الصلاة فهي زكاة مقبولة، ومن أدّاها بعد الصلاة فهي صدقة من الصدقات»",
            Tx("الحكمة ووقت الإخراج", "Its purpose and time"),
            Tx("تُخرج ليلة العيد أو قبله بيوم أو يومين، ولا تؤخر عن صلاة العيد.",
                "Pay it on the eve of Eid or a day or two before; do not delay it past the Eid prayer."),
            Tx("رواه أبو داود (1609) وحسّنه الألباني", "Abu Dawud (1609), graded hasan by al-Albani")),
        f(ProofKind.SCHOLARS, "money",
            Tx("إخراج القيمة", "Paying its value in money"),
            Tx("الجمهور (المالكية والشافعية والحنابلة) على إخراجها طعاماً، وأجاز الحنفية إخراج القيمة نقداً. والصاع نحو 2.04 كغ من القمح، ويُحتاط في الأرز بنحو 2.5 إلى 3 كغ.",
                "The majority require food; the Hanafis allow its value in money. A sa' is about 2.04 kg of wheat; for rice 2.5–3 kg is taken as a precaution."),
            Tx("المغني لابن قدامة؛ فتاوى اللجنة الدائمة", "Ibn Qudamah, al-Mughni; Permanent Committee fatwas")),
    )

    val fidya = listOf(
        q("fast", 2, 184, 184,
            Tx("فدية طعام مسكين", "Feeding a poor person as fidya"),
            Tx("قال ابن عباس: هي للشيخ الكبير والمرأة الكبيرة لا يستطيعان الصيام، فيُطعمان مكان كل يوم مسكيناً.",
                "Ibn Abbas said it is for the elderly man and woman who cannot fast: for each day they feed a poor person."),
            Tx("البقرة: 184؛ البخاري (4505)", "Al-Baqarah 2:184; Bukhari (4505)")),
        f(ProofKind.SCHOLARS, "food",
            Tx("مقدار الإطعام", "How much food"),
            Tx("مُدّ (نحو 0.75 كغ) لكل مسكين عند المالكية والشافعية والحنابلة، ونصف صاع عند الحنفية؛ وفي التطبيق تختار بينهما.",
                "One mudd (about 0.75 kg) per poor person for the Malikis, Shafi'is and Hanbalis; half a sa' for the Hanafis. The app lets you choose."),
            Tx("المغني لابن قدامة", "Ibn Qudamah, al-Mughni")),
    )

    val oath = listOf(
        q("feed", 5, 89, 89,
            Tx("كفارة اليمين", "The expiation of an oath"),
            Tx("إطعام عشرة مساكين من أوسط طعام الأهل، أو كسوتهم، أو عتق رقبة، فمن لم يجد صام ثلاثة أيام.",
                "Feed ten poor people with the average food of your household, or clothe them, or free a slave; whoever cannot, fasts three days."),
            Tx("المائدة: 89", "Al-Ma'idah 5:89")),
    )

    val intercourse = listOf(
        h("fast", "«هل تجد رقبةً تعتقها؟» قال: لا، قال: «فهل تستطيع أن تصوم شهرين متتابعين؟» قال: لا، فقال: «فهل تجد إطعام ستين مسكيناً؟»",
            Tx("كفارة الجماع في نهار رمضان", "Expiation for intercourse during a Ramadan day"),
            Tx("على الترتيب: عتق رقبة، فإن لم يجد فصيام شهرين متتابعين، فإن لم يستطع فإطعام ستين مسكيناً، مع قضاء اليوم.",
                "In order: free a slave; if not possible, fast two consecutive months; if unable, feed sixty poor people — and make up the day."),
            Tx("رواه البخاري (1936) ومسلم (1111)", "Bukhari (1936) and Muslim (1111)")),
    )

    val inheritance = listOf(
        q("family", 4, 11, 12,
            Tx("آيتا المواريث", "The verses of inheritance"),
            Tx("فصّل الله فيهما أنصبة الأولاد والوالدين والأزواج والإخوة لأم، وقدّم الوصية والدين على القسمة.",
                "Allah details the shares of children, parents, spouses and maternal siblings, after any bequest and debt."),
            Tx("النساء: 11-12", "An-Nisa 4:11-12")),
        q("share", 4, 176, 176,
            Tx("آية الكلالة", "The verse of kalalah"),
            Tx("في ميراث الإخوة والأخوات الأشقاء ولأب إذا لم يكن للميت ولد ولا والد.",
                "On the inheritance of full and paternal siblings when the deceased leaves no child or father."),
            Tx("النساء: 176", "An-Nisa 4:176")),
        h("order", "«ألحقوا الفرائض بأهلها، فما بقي فهو لأولى رجلٍ ذكر»",
            Tx("أصحاب الفروض ثم العصبة", "Fixed shares first, then the nearest male relatives"),
            Tx("يُعطى أصحاب الفروض أنصبتهم أولاً، وما بقي لأقرب العصبة.",
                "Those with fixed shares are given first, and the rest goes to the nearest male agnate."),
            Tx("رواه البخاري (6732) ومسلم (1615)", "Bukhari (6732) and Muslim (1615)")),
        f(ProofKind.SCHOLARS, "scholars",
            Tx("المذهب المعتمد في الحساب", "The school followed in the calculation"),
            Tx("يحسب التطبيق على قول الجمهور في الحجب والعول والرد والعمريتين، ويبيّن مواضع الخلاف كالجد مع الإخوة والمشتركة. والأحوط في التركات الكبيرة أو المعقدة مراجعة المحكمة الشرعية أو عالم مختص.",
                "The app follows the majority on exclusion, 'awl, radd and the 'Umariyyatan, and notes disputed cases such as the grandfather with siblings and al-Mushtarakah. For large or complex estates, consult a Sharia court or a qualified scholar."),
            Tx("المغني لابن قدامة؛ الرحبية", "Ibn Qudamah, al-Mughni; al-Rahbiyyah")),
    )
}
