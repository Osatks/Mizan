package com.osatks.mizan.core

/**
 * تسجيلات الأذان: كلها برخص تسمح بإعادة النشر (CC0 أو علامة الملك العام أو CC BY أو CC BY-SA)، والتفاصيل والروابط في
 * assets/licenses/adhan-recordings.txt. الملفات تُصنع بـ tools/make_adhan.py (تسوية الصوت وقص الصمت).
 * [Muezzin.takbir]: نهاية أول تكبيرتين، يحسبها make_adhan.py لكل تسجيل (وكل تسجيل جديد) ويطبعها هنا.
 * المضمّن في التطبيق: فؤاد (الافتراضي، أذانه وأذان فجره) وعاصم وأذان فجر كالكان؛ والبقية في موقع ميزان (/adhan/) تُنزَّل عند اختيارها.
 */
internal object AdhanCatalog {
    val ALL = listOf(
        // المؤذن الافتراضي: فؤاد (الأذان وأذان الفجر)، مضمّن في التطبيق
        Muezzin("fouad", fajr = false, bundled = true, seconds = 146, sizeKb = 1184, takbir = 13.25,
            credit = "Fouad Adan · Public Domain Mark 1.0 · archive.org"),
        Muezzin("fouad_fajr", fajr = true, bundled = true, seconds = 137, sizeKb = 1129, takbir = 10.25,
            credit = "Fouad Adan · Public Domain Mark 1.0 · archive.org"),
        Muezzin("asim", fajr = false, bundled = true, seconds = 196, sizeKb = 1606, takbir = 9.45,
            credit = "Asim Javed (ICR) · CC0 1.0 · archive.org"),
        Muezzin("klcc", fajr = false, bundled = false, seconds = 170, sizeKb = 1403, takbir = 21.0,
            credit = "Recorded by the archive.org uploader · CC0 1.0"),
        Muezzin("mirza", fajr = false, bundled = false, seconds = 249, sizeKb = 2049, takbir = 17.8,
            credit = "Moazzambeg Mirza, Muhammadi Masjid, Sarkhej · Public Domain Mark 1.0 · archive.org"),
        Muezzin("adam", fajr = false, bundled = false, seconds = 152, sizeKb = 1251, takbir = 15.2,
            credit = "Adam-synagda (own work) · CC0 1.0 · Wikimedia Commons"),
        Muezzin("madinah", fajr = false, bundled = false, seconds = 187, sizeKb = 1535, takbir = 17.35,
            credit = "ejaz215 (Freesound) · CC BY 3.0 · Wikimedia Commons"),
        Muezzin("andrewler", fajr = false, bundled = false, seconds = 158, sizeKb = 1304, takbir = 16.55,
            credit = "Andrewler (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin("aaqib", fajr = false, bundled = false, seconds = 87, sizeKb = 712, takbir = 5.0,
            credit = "Atcovi (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin("shalqar", fajr = false, bundled = false, seconds = 132, sizeKb = 1088, takbir = 8.4,
            credit = "Esetok (own work) · CC BY-SA 4.0 · Wikimedia Commons"),
        Muezzin("kalkan_fajr", fajr = true, bundled = true, seconds = 279, sizeKb = 2295, takbir = 11.25,
            credit = "Recorded by Piotrek Zyla (piotrekzyla.com) · Public Domain Mark 1.0 · radio aporee / archive.org"),
        Muezzin("kas_fajr", fajr = true, bundled = false, seconds = 312, sizeKb = 2561, takbir = 18.85,
            credit = "Recorded by Heleste (ambientfeel.t.me) · Public Domain Mark 1.0 · radio aporee / archive.org"),
        Muezzin("malmo_fajr", fajr = true, bundled = false, seconds = 234, sizeKb = 1920, takbir = 22.5,
            credit = "Islamic Center Malmö (YouTube) · CC BY 3.0 · Wikimedia Commons"),
    )
}
