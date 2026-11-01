package com.osatks.mizan.quran

import com.osatks.mizan.i18n.Lang
import com.osatks.mizan.platform.NowPlaying
import com.osatks.mizan.ui.quran.surahName

/**
 * iOS: التلاوة خارج التطبيق. عند كل تغيّر في حالها (Recitation.onChange) تُحدَّث «يُشغَّل الآن» في شاشة القفل
 * ومركز التحكم والجزيرة الديناميكية (السورة والآية والقارئ، وأزرار التشغيل والإيقاف والآية التالية والسابقة).
 * لا نشاط حي خاص بالتلاوة: مشغّل النظام نفسه يظهر هناك، كما في تطبيقات الصوت من Apple، فلا تتكرر البطاقة.
 */
internal object RecitationLive {
    private var started = false

    fun start() {
        if (started) return
        started = true
        // لا يوقف خطأ هنا التلاوة نفسها
        Recitation.onChange = { runCatching { sync() } }
        NowPlaying.recitationControls(
            NowPlaying.Controls(
                play = { if (Recitation.active && !Recitation.wantsPlay) Recitation.toggle() },
                pause = { if (Recitation.wantsPlay) Recitation.pause() },
                toggle = { Recitation.toggle() },
                next = { Recitation.skip(1) },
                previous = { Recitation.skip(-1) },
                stop = { Recitation.stop() },
            )
        )
    }

    fun sync() {
        val index = Recitation.current
        if (!Recitation.active || index < 0) {
            NowPlaying.recitation(null, false)
            return
        }
        val lang = Lang.current
        val s = lang.strings
        val (surah, ayah) = QuranMeta.locate(index)
        val count = QuranMeta.COUNTS[surah - 1]
        val reciter = Reciters.byId(QuranPrefs.reciter).name(lang)
        val title = s.surahAyah(s.surahName(surah), ayah)
        NowPlaying.recitation(NowPlaying.Item(title, reciter, s.appName, ayah - 1, count), Recitation.wantsPlay)
    }
}
