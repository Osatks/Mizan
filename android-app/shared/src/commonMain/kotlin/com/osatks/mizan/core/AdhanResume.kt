package com.osatks.mizan.core

import com.osatks.mizan.platform.AdhanAudio
import com.osatks.mizan.ui.screens.PrayerUtil

/**
 * الأذان في الإشعار أول تكبيرتين ثم يتلاشى ([Muezzin.takbir] و [Muezzin.FADE]): صوت إشعار iOS (adhan_<id>.caf)،
 * ومنبّه أندرويد (AdhanService) والتطبيق مغلق. فإن فُتح التطبيق والأذان لو سُمع كاملاً ما زال يُرفع، يكمل من نهاية
 * التكبيرتين (أو من حيث بلغ إن فُتح قبلها) بتلاشٍ لطيف في أوله، فيُسمع كأنه لم ينقطع، مرة واحدة لكل صلاة.
 * لا يكمل ما أوقفه المستخدم (زر الإيقاف أو زر الصوت أو إزالة الإشعار في أندرويد) ولا ما سُمع كاملاً
 * والتطبيق مفتوح: كلاهما يُعلَّم [done].
 */
object AdhanResume {
    private const val KEY = "adhan_done"

    /** أندرويد: التطبيق أمام المستخدم الآن (المنبّه يُسمع كاملاً) */
    var appVisible = false

    /** أندرويد: يوقف منبّه الأذان الجاري قبل أن يكمله التطبيق (لا يُسمعان معاً) */
    var handOff: (() -> Unit)? = null

    private fun id(prayer: String) = "${PrayerUtil.now().date}-$prayer"

    /** هذا الأذان لا يُكمَل: أوقفه المستخدم، أو سُمع كاملاً، أو أُكمل */
    fun done(prayer: String) {
        if (AppSettings.ready) AppSettings.putString(KEY, id(prayer))
    }

    /** عند فتح التطبيق أو عودته إلى الواجهة (MainActivity في أندرويد، IosLive في iOS) */
    fun onAppActive() {
        if (!AppSettings.ready || !AppSettings.notify) return
        val now = PrayerUtil.now()
        val t = PrayerUtil.times(now.date)
        val sec = now.time.toSecondOfDay().toDouble()
        // آخر صلاة دخل وقتها اليوم
        val key = ALERT_PRAYERS.lastOrNull { k -> !t[k].isNaN() && t[k] * 3600 <= sec } ?: return
        if (AppSettings.alertMode(key) != AlertMode.ADHAN) return
        if (AppSettings.getString(KEY) == id(key)) return
        val m = Muezzins.forPrayer(key)
        val since = sec - t[key] * 3600
        // انتهى الأذان لو سُمع كاملاً: لا يُرفع بعد وقته
        if (since >= m.seconds) return
        done(key)
        handOff?.invoke()
        AdhanAudio.alertPrayer(key)
        AdhanAudio.play(m, preview = false, from = minOf(since, m.takbir)) {}
    }
}
