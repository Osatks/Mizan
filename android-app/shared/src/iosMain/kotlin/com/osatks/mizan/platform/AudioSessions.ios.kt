@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.osatks.mizan.platform

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive

/**
 * جلسة الصوت المشتركة بين التلاوة والأذان: تُفعَّل عند التشغيل الفعلي فقط وتُعطَّل حين لا يبقى ما يُسمع،
 * فيعود صوت التطبيقات الأخرى (الموسيقى، البودكاست). وضع الخلفية "audio" لا يُستعمل إلا والتلاوة تُسمع فعلاً.
 */
internal object AudioSessions {
    private val users = HashSet<String>()

    /** [category] من فئات AVAudioSession: Playback للتلاوة والمعاينة، و SoloAmbient لأذان الوقت (يتبع مفتاح الصامت) */
    fun begin(user: String, category: String?) {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(category, null)
            session.setActive(true, null)
        }
        users += user
    }

    fun end(user: String) {
        if (!users.remove(user) || users.isNotEmpty()) return
        runCatching { AVAudioSession.sharedInstance().setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null) }
    }
}
