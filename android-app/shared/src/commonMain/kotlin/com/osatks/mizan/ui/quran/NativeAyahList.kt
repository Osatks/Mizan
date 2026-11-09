package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * موضع عرض آية آية حين يرسم القائمةَ النظامُ نفسه (iOS: iosApp/iosApp/NativeAyahList.swift) بدل LazyColumn:
 * [first] أول عنصر ظاهر في أعلى القائمة (0 الترويسة، ثم رقم الآية) كما يبلغه القارئ أثناء التمرير،
 * و[jump] آخر انتقال طلبه التطبيق (الآية المطلوبة عند الفتح، التبديل من المصحف، التفسير، التلاوة) يطبّقه القارئ حين يتغير [AyahJump.seq].
 */
internal class NativeAyahScroll(initial: Int) {
    var first by mutableIntStateOf(initial)
        private set
    var jump by mutableStateOf(AyahJump(initial, false, false, 0))
        private set

    /** ينتقل إلى العنصر [item] (أعلى القائمة)؛ يُعدّ الموضع الجديد فوراً كما يفعل scrollToItem في Compose */
    fun go(item: Int, animate: Boolean) {
        first = item
        jump = AyahJump(item, animate, false, jump.seq + 1)
    }

    /** يتبع الآية المتلوة: تنساب القائمة إليها إن لم تكن ظاهرة كلها، وإلا تبقى في مكانها */
    fun follow(item: Int) {
        jump = AyahJump(item, true, true, jump.seq + 1)
    }

    /** أبلغ القارئ أن أول عنصر ظاهر صار [item] */
    internal fun scrolled(item: Int) {
        first = item
    }
}

internal class AyahJump(val item: Int, val animate: Boolean, val follow: Boolean, val seq: Int)

/** عرض آية آية من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فتبقى LazyColumn */
internal expect val nativeAyahListAvailable: Boolean

/**
 * آيات السورة [surah] بطاقةً بطاقة يرسمها النظام (SwiftUI و LazyVStack في iOS): ترويسة السورة والبسملة، والآية بخط المصحف
 * وعلامة نهايتها برقمها، وترجمة المعاني، والعلامة، والآية المختارة مظلَّلة، وفي آخرها الانتقال إلى السورة السابقة والتالية —
 * كما في عرض آية آية من Compose. اللمس والضغط المطوّل يصلان إلى الشاشة كما من Compose؛ وما فوق القائمة يبقى من Compose فوقها.
 * [bottomInset] ما يغطي أسفل القائمة (شريط النظام والمشغّل الصغير).
 */
@Composable
internal expect fun NativeAyahListView(
    scroll: NativeAyahScroll, surah: Int, text: Array<String>, translation: Array<String>?, trRtl: Boolean,
    selected: Int, size: Int, bottomInset: Dp,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onTafsir: (Int) -> Unit, onScrollUp: () -> Unit, onSurah: (Int) -> Unit,
    modifier: Modifier,
)
