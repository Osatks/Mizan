package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * موضع التقليب حين يعرض صفحاتِ المصحف قارئُ النظام نفسه (iOS: iosApp/iosApp/NativeMushaf.swift) بدل HorizontalPager:
 * [index] موضع التقليب الظاهر بعد أن يستقر التقليب (بمعنى [Spreads.index]: رقم الصفحة ناقص واحد، أو رقم الكتاب المفتوح
 * حين [spread])، و[scrolling] ما دام الإصبع يقلّب أو الصفحات تنساب،
 * و[jump] آخر انتقال طلبه التطبيق (التلاوة، القفز إلى صفحة، لمس طرف الصفحة) يطبّقه القارئ حين يتغير [JumpRequest.seq].
 * [spread]: كل موضع صفحتان متقابلتان (iPad بالعرض، iPhone Duo مفتوحاً)، و[count] = [Spreads.count].
 */
internal class NativeTurns(initial: Int, val count: Int, val spread: Boolean = false) {
    var index by mutableIntStateOf(initial)
        private set
    var scrolling by mutableStateOf(false)
        internal set
    var jump by mutableStateOf(JumpRequest(initial, false, 0))
        private set

    /** انتقال يطلبه التطبيق: يُعدّ الموضع الجديد فوراً (كما يفعل التقليب في Compose) ويقلّب القارئ إليه */
    fun go(i: Int, animate: Boolean) {
        val to = i.coerceIn(0, count - 1)
        index = to
        jump = JumpRequest(to, animate, jump.seq + 1)
    }

    /** استقر التقليب في القارئ على [i] (بالإصبع، أو بعد انتقال طلبه التطبيق) */
    fun settled(i: Int) {
        index = i.coerceIn(0, count - 1)
        scrolling = false
    }
}

internal class JumpRequest(val index: Int, val animate: Boolean, val seq: Int)

/** قارئ المصحف من النظام مسجَّل (iOS، ويسجّله Swift عند التشغيل)؛ في أندرويد لا يوجد فيبقى HorizontalPager */
internal expect val nativeMushafAvailable: Boolean

/**
 * صفحات مصحف المدينة (صفحة واحدة، أو صفحتان متقابلتان حين [NativeTurns.spread]) يقلّبها ويرسمها النظام نفسه: UICollectionView و Core Text في iOS،
 * بالورقة والإطار والترويسة والتظليل نفسها التي يرسمها [MushafPage]. اللمس والضغط المطوّل ولمس الإطار وطرفي الصفحة
 * تصل إلى الشاشة كما من [MushafPage]؛ وما فوق الصفحات (الرأس، شريط خيارات الآية، الأوراق) يبقى من Compose فوقه.
 */
@Composable
internal expect fun NativeMushafPager(
    turns: NativeTurns, text: Array<String>, selected: Int, bottomInset: Dp, frame: Dp, focus: Boolean,
    onTap: (Int) -> Unit, onLongPress: (Int) -> Unit, onFrameTap: () -> Unit, onEdge: (Int) -> Unit,
    hifz: Boolean, revealed: Set<Int>, modifier: Modifier,
)
