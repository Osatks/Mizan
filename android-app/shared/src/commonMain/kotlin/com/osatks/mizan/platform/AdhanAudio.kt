package com.osatks.mizan.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.osatks.mizan.core.Muezzin
import com.osatks.mizan.core.VibePattern

/** صوت الأذان والاهتزاز على الجهاز */
expect object AdhanAudio {
    /**
     * يشغّل الأذان كاملاً؛ onEnd عند انتهائه (لا عند [stop]).
     * [preview] معاينة يطلبها المستخدم (تُسمع ولو كان الهاتف صامتاً في iOS)، وإلا فهو أذان الوقت والتطبيق مفتوح.
     */
    fun play(m: Muezzin, preview: Boolean, onEnd: () -> Unit)
    fun stop()

    /** يجرّب نمط الاهتزاز (Core Haptics في iOS) */
    fun vibrate(p: VibePattern)

    /** iOS: صوت إشعار التسجيل المنزَّل يُحفظ في Library/Sounds ليجده النظام؛ أندرويد لا يحتاجه */
    val needsNotificationSound: Boolean
    fun saveNotificationSound(name: String, bytes: ByteArray)
    fun deleteNotificationSound(name: String)

    /** أندرويد: الإشعار يهتز بالنمط المختار. iOS: يهتز الإشعار بنمط النظام، والنمط المختار داخل التطبيق فقط */
    val patternsInNotifications: Boolean
}

/** معاينة الأذان في الواجهة: أي تسجيل يُسمع الآن */
object AdhanPreview {
    var playing by mutableStateOf<String?>(null)
        private set

    fun toggle(m: Muezzin) {
        if (playing == m.id) return stop()
        AdhanAudio.stop()
        playing = m.id
        AdhanAudio.play(m, preview = true) { if (playing == m.id) playing = null }
    }

    fun stop() {
        AdhanAudio.stop()
        playing = null
    }
}
