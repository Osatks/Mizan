package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.AppSettings

/**
 * المسبحة على شاشة القفل وفي الجزيرة الديناميكية (نشاط حي في iOS 17 فما بعد): زر في شاشة المسبحة يُظهرها هناك.
 * يضبطه iOS (IosTasbih)؛ في أندرويد يبقى [supported] خطأ فلا يظهر الزر.
 */
object LiveTasbih {
    var supported by mutableStateOf(false)

    /** يبدأ النشاط الحي بالعدد الحالي (يضبطه iOS) */
    var show: (() -> Unit)? = null

    /**
     * المسبحة وعدّاد الأذكار على شاشة القفل وفي الجزيرة الديناميكية (مفعّل افتراضياً). مطفأً: يعدّ زر الإجراء ومركز
     * التحكم والأداة بلا نشاط حي، ويُنهى الظاهر منه، فلا تظهر بطاقته على شاشة القفل ولا يُسأل المستخدم عنها.
     * يقرؤه Swift من NSUserDefaults («mizan.live_counters»: مفتاح AppSettings نفسه).
     */
    const val KEY = "live_counters"
    private var chosen by mutableStateOf<Boolean?>(null)
    val onLockScreen: Boolean get() = chosen ?: (!AppSettings.ready || AppSettings.getBool(KEY, true))

    /** iOS: يُنهي الأنشطة الظاهرة حين يُطفأ */
    var onChange: ((Boolean) -> Unit)? = null

    fun update(on: Boolean) {
        chosen = on
        AppSettings.putBool(KEY, on)
        onChange?.invoke(on)
    }
}

/** نص مفتاح «على شاشة القفل» في شاشة المسبحة بلغات التطبيق: العنوان والوصف */
object LiveTasbihText {
    fun of(code: String): Pair<String, String> = when (code) {
        "ar" -> "العدّاد على شاشة القفل والجزيرة" to "أطفئه إن لم ترد بطاقة المسبحة والأذكار على شاشة القفل؛ يبقى زر الإجراء يعدّ"
        "fr" -> "Compteur sur l’écran verrouillé et la Dynamic Island" to "Désactivez-le pour ne pas afficher la carte du tasbih et des adhkar ; le bouton Action continue de compter"
        "tr" -> "Kilit Ekranı ve Dynamic Island’da sayaç" to "Tesbih ve zikir kartını Kilit Ekranı’nda istemiyorsanız kapatın; Eylem düğmesi saymaya devam eder"
        "ur" -> "لاک اسکرین اور ڈائنامک آئی لینڈ پر کاؤنٹر" to "اگر لاک اسکرین پر تسبیح اور اذکار کا کارڈ نہ چاہیں تو اسے بند کریں؛ ایکشن بٹن گنتی جاری رکھے گا"
        "id" -> "Penghitung di Layar Terkunci dan Dynamic Island" to "Matikan jika tidak ingin kartu tasbih dan zikir di Layar Terkunci; tombol Tindakan tetap menghitung"
        "ms" -> "Pembilang di Skrin Kunci dan Dynamic Island" to "Matikan jika tidak mahu kad tasbih dan zikir di Skrin Kunci; butang Tindakan terus mengira"
        "fa" -> "شمارنده روی صفحهٔ قفل و دایناميک آیلند" to "اگر کارت تسبیح و اذکار را روی صفحهٔ قفل نمی‌خواهید خاموشش کنید؛ دکمهٔ اکشن همچنان می‌شمارد"
        "bn" -> "লক স্ক্রিন ও ডায়নামিক আইল্যান্ডে কাউন্টার" to "লক স্ক্রিনে তাসবিহ ও জিকিরের কার্ড না চাইলে বন্ধ করুন; অ্যাকশন বোতাম গণনা চালিয়ে যাবে"
        "ru" -> "Счётчик на экране блокировки и в Dynamic Island" to "Выключите, если не нужна карточка тасбиха и азкаров на экране блокировки; кнопка действия продолжит считать"
        "de" -> "Zähler auf dem Sperrbildschirm und in der Dynamic Island" to "Ausschalten, wenn die Tasbih- und Adhkar-Karte nicht auf dem Sperrbildschirm erscheinen soll; die Aktionstaste zählt weiter"
        "es" -> "Contador en la pantalla bloqueada y la Dynamic Island" to "Desactívalo si no quieres la tarjeta del tasbih y los adhkar en la pantalla bloqueada; el botón de acción sigue contando"
        "uk" -> "Лічильник на екрані блокування та в Dynamic Island" to "Вимкніть, якщо не потрібна картка тасбіху й азкарів на екрані блокування; кнопка дії й далі рахує"
        "uz" -> "Qulf ekrani va Dynamic Island’da hisoblagich" to "Qulf ekranida tasbeh va zikr kartasi kerak bo‘lmasa, o‘chiring; Amal tugmasi sanashda davom etadi"
        "kk" -> "Құлып экраны мен Dynamic Island-тағы санауыш" to "Құлып экранында тәсбих пен зікір картасы керек болмаса, өшіріңіз; Әрекет түймесі санауды жалғастырады"
        else -> "Counter on the Lock Screen and Dynamic Island" to "Turn off if you don’t want the tasbih and adhkar card on the Lock Screen; the Action button keeps counting"
    }
}
