package com.osatks.mizan.content

/** نوع المحتوى: اسم مجلده (في assets، وفي الموقع /content/<dir>/، وفي ملفات التطبيق content/<dir>/) وامتداد ملفاته */
enum class ContentKind(val dir: String, val ext: String) {
    /** ترجمات المعاني والتفاسير بصيغة tanzil (سورة|آية|نص) */
    TAFSIR("tafsir", "txt"),
    /** ترجمات أحاديث اليوم (موسوعة الأحاديث النبوية) */
    HADITH("hadith", "json"),
    /** ترجمات الأذكار (حصن المسلم) */
    ADHKAR("adhkar", "json"),
}

/**
 * ملف محتوى ينزّله التطبيق من موقع ميزان بدل أن يُضمَّن فيه: [bytes] حجمه، و[sha256] بصمته التي يُتحقق بها بعد التنزيل،
 * و[version] إصداره في فهرس الموقع (dist/content/<dir>/index.json).
 */
class ContentItem(val kind: ContentKind, val id: String, val lang: String, val bytes: Long, val sha256: String, val version: Int) {
    /** اسم الملف: fr.hamidullah.txt أو fr.json */
    val file: String get() = "$id.${kind.ext}"
    /** مساره كما في assets (tafsir/fr.hamidullah.txt)، فيقرؤه من يقرأ assets بالمسار نفسه */
    val path: String get() = "${kind.dir}/$file"
    /** مفتاحه في التنزيلات والإعدادات: tafsir/fr.hamidullah */
    val key: String get() = "${kind.dir}/$id"
    /** الحجم بالميغابايت (لعرضه: «تنزيل (1.4 م.ب)») */
    val sizeMb: Double get() = bytes / 1_000_000.0
}

/**
 * فهرس المحتوى القابل للتنزيل، والبصمات نفسها في index.json الذي يكتبه build.py (publish_content) من src/content.
 * يتأكد ContentCatalogTest أن كل عنصر هنا منشور في dist/content ببصمته وحجمه؛ عند تعديل ملف في src/content
 * يُعاد بناء الموقع وتُحدَّث بصمته هنا (وإن سبق الموقعُ التطبيق قبِل التطبيق البصمة التي يعلنها فهرس الموقع).
 */
object ContentCatalog {
    /**
     * المضمّن في التطبيق (assets): أربعة تفاسير وترجمات (الميسر والجلالين وصحيح الدولية والإندونيسية)،
     * وأحاديث اليوم والأذكار بالعربية والإنجليزية. ما سواه يُنزَّل من الموقع.
     */
    val BUNDLED: Set<String> = setOf(
        "tafsir/ar.muyassar", "tafsir/ar.jalalayn", "tafsir/en.sahih", "tafsir/id.indonesian",
        "hadith/ar", "hadith/en",
        "adhkar/adhkar", "adhkar/ar", "adhkar/en",
    )

    val ALL: List<ContentItem> = listOf(
        ContentItem(ContentKind.TAFSIR, "bn.bengali", "bn", 2157584, "db537a009464183323fb944f7a75d62c898568f35c6f09394b4d8529b1a43559", 1),
        ContentItem(ContentKind.TAFSIR, "de.bubenheim", "de", 982295, "3c3c12c0189280e83eb52a2bbfd9d685bef16ffa9f4a307037367fe6dd00b958", 1),
        ContentItem(ContentKind.TAFSIR, "es.garcia", "es", 926005, "cac86dd7d24376e565821ddf8bfdc0db9a286aa2770a0521a06ad08d8eb7e2d2", 1),
        ContentItem(ContentKind.TAFSIR, "fa.fooladvand", "fa", 1339156, "4d8cead0c9c7397e59adc4e773dd56b2da4c5700cda82d9ed648660fb190aa45", 1),
        ContentItem(ContentKind.TAFSIR, "fr.hamidullah", "fr", 948096, "9834cfcd14b506a6c0223d86ac259e896f2bfb44c0823d83d827c27ffea57f03", 1),
        ContentItem(ContentKind.TAFSIR, "id.jalalayn", "id", 2753922, "62712bcbde9cc13cf2b8de1b072e127fd25af17fc1d6b5e7e3238328ee6d3350", 1),
        ContentItem(ContentKind.TAFSIR, "kk.altai", "kk", 1444221, "f66128a4779d25454eb2f96373929404886a0f3438a5a58c5b3b0ac203c36d89", 1),
        ContentItem(ContentKind.TAFSIR, "ms.basmeih", "ms", 1488713, "51548b7d7c65f36c4b26b58d81c9c994d96869d06577efb350968968b4ab01b6", 1),
        ContentItem(ContentKind.TAFSIR, "ru.kuliev", "ru", 1454333, "f2d328a28559e219733fb9287df060a643964b6ae9a59256431b786beca76449", 1),
        ContentItem(ContentKind.TAFSIR, "tr.diyanet", "tr", 1013354, "a9be7b4f291d8cf6f72dbd22dbc4b58f4716c137e56dbdc562aee3fc8c4bc4af", 1),
        ContentItem(ContentKind.TAFSIR, "uk.yakubovych", "uk", 1321832, "3b58a9eb538ab36a1a05bf1a617fece5cf18b76d8bb8631489cf60f6f37b333a", 1),
        ContentItem(ContentKind.TAFSIR, "ur.jalandhry", "ur", 1450376, "c7e982b49b5e6f275559985caaf24f5e0136950f73f50f1a6c6f6a5281617c6a", 1),
        ContentItem(ContentKind.TAFSIR, "uz.sodik", "uz", 2528482, "6f065883151ef7da3ffff8ab804074e1a219f223397ac788453f5ae62c240d5c", 1),
        ContentItem(ContentKind.HADITH, "bn", "bn", 1580955, "a7593490008ab569e4838a71ade4ae18ec9ead326c6b1fd8500ca8db28344c80", 1),
        ContentItem(ContentKind.HADITH, "de", "de", 682025, "72c9a635d73a308116bcbf6fb6de65f0b39505c8bfd1fce52af54e46d399cd8a", 1),
        ContentItem(ContentKind.HADITH, "es", "es", 697656, "dbd06455141007d97f61b6e765e21fa99765c6ec08d56dd49f9f92f1af27ac23", 1),
        ContentItem(ContentKind.HADITH, "fa", "fa", 990513, "732ec257c30c7e1ccdff4799c816861a8ccf4f35f0bb706f323fedb8abb11b07", 1),
        ContentItem(ContentKind.HADITH, "fr", "fr", 727225, "2116d12ed9d647de312dbdc2af2b917fd4a099e521dcb5054bf7f5bcf407d2fa", 1),
        ContentItem(ContentKind.HADITH, "id", "id", 651643, "4e30bc5fc74a6fcf1bf3151c9b11e77e02f873a2891b3648a6b7f1a4aea298e5", 1),
        ContentItem(ContentKind.HADITH, "ms", "ms", 316974, "5c79020a0cd851e74aebe5f4ac7f96d9eeb6df8256f6cdd765cebe2166b51e6e", 1),
        ContentItem(ContentKind.HADITH, "ru", "ru", 1219839, "8be5bf0b3154b30ac894e1cd29e5086f4248ba23394c425087610e8c5a38f4a6", 1),
        ContentItem(ContentKind.HADITH, "tr", "tr", 654770, "2a3ec56c6b5f316d75913356ffe548a618c8a760fca6e5c74c3b1db44c99b1b1", 1),
        ContentItem(ContentKind.HADITH, "uk", "uk", 1012389, "e27bd2631b047ce5afb7ca6f30ad8407e3e9f5142a4649c5c6a7065749881af8", 1),
        ContentItem(ContentKind.HADITH, "ur", "ur", 1004929, "5f0318729accf0570085800a56326afb6381bb880ed31aeb4012560bd54ad5aa", 1),
        ContentItem(ContentKind.HADITH, "uz", "uz", 903077, "150db743758071e7247f35e73ff1f3c2d70196008967eb95f4106169cc87548d", 1),
        ContentItem(ContentKind.ADHKAR, "bn", "bn", 335986, "1f60a6b82888005c984e6118736f8d632dd41ebc7a25ba753dbc177026bb0a69", 1),
        ContentItem(ContentKind.ADHKAR, "de", "de", 158874, "e065d8992646b89bb7fb57f10a55290216079ba7fbd41b16e42f11ab2f4479d8", 1),
        ContentItem(ContentKind.ADHKAR, "es", "es", 150256, "37d2606ab05748823215d1bed9ce7549978abdea1b48cb3bf2222ad3ee423cb6", 1),
        ContentItem(ContentKind.ADHKAR, "fa", "fa", 197754, "e1918406c0effbf2de98b334c7036c63e401a566042f4111eda9f23555d84359", 1),
        ContentItem(ContentKind.ADHKAR, "fr", "fr", 152431, "2c2c635e3cbbd597e8a2cf67b4aff552ddd4351d7707703fc5e69c92640154c0", 1),
        ContentItem(ContentKind.ADHKAR, "id", "id", 157397, "92ee50f213283591fad6a9b3c8f5694f3227fd23d87cfdd0ec22b3ba5d65c886", 1),
        ContentItem(ContentKind.ADHKAR, "kk", "kk", 221076, "b59fff897ebd28fbb40f0b46398a7a14183f7a51a1d26f56287b31eab81cf5ce", 1),
        ContentItem(ContentKind.ADHKAR, "ms", "ms", 162329, "fcb4a2dc0c13e2ecb79bba6ccfcd2920f7e41aeb7b1f6785f7b6a47990c08805", 1),
        ContentItem(ContentKind.ADHKAR, "ru", "ru", 229457, "0d913dcf57836636bd6872c6fdee7e0a2c08a39ab6ee46a0727ac6f3f2e9ff27", 1),
        ContentItem(ContentKind.ADHKAR, "tr", "tr", 136952, "d3b863d77cf3b2844ddc5fad2777132582f182c0cfccc8a93d01f9165fd1974d", 1),
        ContentItem(ContentKind.ADHKAR, "uk", "uk", 230614, "a67e748b0d3a1ad0eba3db74b21df4e07ebf55311fa6b4a31f8ca64abfc2df35", 1),
        ContentItem(ContentKind.ADHKAR, "ur", "ur", 214133, "62fb629b029d099ab13aa9ef9148dcf5459709c669b81e7ca4440efac0f70ca1", 1),
        ContentItem(ContentKind.ADHKAR, "uz", "uz", 139258, "fac146e71a0f43efc4068ea813eb725a5409f56e03ef778126c65645225e70ef", 1),
    )

    fun byKey(key: String): ContentItem? = ALL.firstOrNull { it.key == key }
    fun byPath(path: String): ContentItem? = ALL.firstOrNull { it.path == path }
    fun tafsir(id: String): ContentItem? = byKey("tafsir/$id")
    fun hadith(code: String): ContentItem? = byKey("hadith/$code")
    fun adhkar(code: String): ContentItem? = byKey("adhkar/$code")
    fun of(kind: ContentKind): List<ContentItem> = ALL.filter { it.kind == kind }
}
