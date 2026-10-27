package com.osatks.mizan.core

/**
 * تسجيلات الأذان: كلها برخص تسمح بإعادة النشر (CC0 أو علامة الملك العام)، والتفاصيل والروابط في
 * assets/licenses/adhan-recordings.txt. الملفات تُصنع بـ tools/make_adhan.py (تسوية الصوت وقص الصمت).
 * المضمّن في التطبيق: أذان وأذان فجر؛ والبقية في موقع ميزان (/adhan/) تُنزَّل عند اختيارها.
 */
internal object AdhanCatalog {
    val ALL = listOf(
        Muezzin("asim", fajr = false, bundled = true, seconds = 196, sizeKb = 1606,
            credit = "Asim Javed (ICR) · CC0 1.0 · archive.org"),
        Muezzin("klcc", fajr = false, bundled = false, seconds = 170, sizeKb = 1403,
            credit = "Recorded by the archive.org uploader · CC0 1.0"),
        Muezzin("mirza", fajr = false, bundled = false, seconds = 249, sizeKb = 2049,
            credit = "Moazzambeg Mirza, Muhammadi Masjid, Sarkhej · Public Domain Mark 1.0 · archive.org"),
        Muezzin("adam", fajr = false, bundled = false, seconds = 152, sizeKb = 1251,
            credit = "Adam-synagda (own work) · CC0 1.0 · Wikimedia Commons"),
        Muezzin("kalkan_fajr", fajr = true, bundled = true, seconds = 279, sizeKb = 2295,
            credit = "Recorded by Piotrek Zyla (piotrekzyla.com) · Public Domain Mark 1.0 · radio aporee / archive.org"),
    )
}
