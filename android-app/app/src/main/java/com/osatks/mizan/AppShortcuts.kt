package com.osatks.mizan

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.osatks.mizan.core.AppSettings
import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.ui.quran.CONTINUE_ROUTE

/**
 * اختصارات أيقونة التطبيق في أندرويد: تظهر بالضغط الطويل على الأيقونة وفي بحث المشغّل (كما في Pixel)، وهي أقرب ما في
 * أندرويد إلى بحث النظام في iPhone (لا يتيح أندرويد للتطبيقات فهرسة محتواها في بحث عام).
 * «تابع القراءة» (آخر موضع في المصحف، يُحسب عند الفتح: continueRoute)، وأكثر الأدوات استعمالاً، والبحث في القرآن.
 * بلغة التطبيق، وتُنشر من جديد حين تتغير اللغة؛ وكل اختصار يفتح مساره كما تفتحه أدوات الشاشة الرئيسية (الوسيط route في MainActivity).
 */
object AppShortcuts {
    /** تُرفع إن تغيّرت الاختصارات أو رموزها، فتُنشر من جديد */
    private const val VERSION = 1
    /** لغة آخر نشر في هذا الجهاز (البادئة device_ لا تدخل النسخة الاحتياطية) */
    private const val KEY = "device_shortcuts"
    /** معرّف الاختصار في نية الفتح، ليُبلَّغ المشغّل باستعماله فيرتّب الاختصارات بحسبه */
    private const val EXTRA = "shortcut"

    private class Item(val id: String, val route: String, val short: String, val long: String, val icon: Int)

    /** ينشر الاختصارات بلغة [lang] إن لم تكن منشورة بها */
    fun publish(ctx: Context, lang: Lang) {
        runCatching {
            val stamp = "$VERSION:${lang.code}"
            if (AppSettings.getString(KEY) == stamp && ShortcutManagerCompat.getDynamicShortcuts(ctx).isNotEmpty()) return
            val s = lang.strings
            fun tool(k: String, icon: Int): Item {
                val name = s.tools[k]?.name ?: k
                return Item(k, k, name, name, icon)
            }
            // بالترتيب: الأول أقربها إلى الأيقونة، والمشغّل يعرض أولها إن ضاق المكان
            val items = listOf(
                Item("continue", CONTINUE_ROUTE, s.continueReading, s.continueReading, R.drawable.sc_continue),
                tool("prayer", R.drawable.sc_prayer),
                tool("qibla", R.drawable.sc_qibla),
                Item("search", "search", s.searchTitle, s.searchTheQuran, R.drawable.sc_search),
                tool("tasbih", R.drawable.sc_tasbih),
                tool("adhkar", R.drawable.sc_adhkar),
                tool("mosques", R.drawable.sc_mosques),
            )
            val max = ShortcutManagerCompat.getMaxShortcutCountPerActivity(ctx).coerceAtLeast(1)
            val list = items.take(max).mapIndexed { rank, it ->
                ShortcutInfoCompat.Builder(ctx, it.id)
                    .setShortLabel(it.short)
                    .setLongLabel(it.long)
                    .setIcon(IconCompat.createWithResource(ctx, it.icon))
                    .setIntent(intent(ctx, it))
                    .setRank(rank)
                    .build()
            }
            if (ShortcutManagerCompat.setDynamicShortcuts(ctx, list)) AppSettings.putString(KEY, stamp)
        }
    }

    /** فُتح التطبيق من اختصار: يُبلَّغ المشغّل (ترتيب الاختصارات واقتراحاته) */
    fun reportUsed(ctx: Context, intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA) ?: return
        runCatching { ShortcutManagerCompat.reportShortcutUsed(ctx, id) }
    }

    /** كما في أدوات الشاشة الرئيسية (Widgets.open): مهمة جديدة تفتح المسار من أوله */
    private fun intent(ctx: Context, item: Item) = Intent(ctx, MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .putExtra("route", item.route)
        .putExtra(EXTRA, item.id)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}
