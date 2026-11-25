package com.osatks.mizan.ui.screens

import com.osatks.mizan.hadith.Hadith
import com.osatks.mizan.hadith.Hadiths
import com.osatks.mizan.ui.quran.StoryContent

/** صورة القصة للحديث: نصه كاملاً إن قصر، وإلا طرفه (عنوانه في الموسوعة)، ومعه الدرجة والتخريج والمصدر */
fun hadithStory(h: Hadith, kind: String): StoryContent {
    val full = h.ar.text.length <= 420
    val main = h.tr ?: h.ar
    val ref = listOf(main.grade, main.takhrij).filter { it.isNotBlank() }.joinToString(" · ")
    val plain = buildString {
        append(h.ar.text)
        h.tr?.let { append("\n\n").append(it.text) }
        if (ref.isNotEmpty()) append("\n\n").append(ref)
        append("\n").append(h.link)
    }
    return StoryContent(
        kind, if (full) h.ar.text else h.ar.title, h.tr?.let { if (full) it.text else it.title }, ref, plain, Hadiths.SOURCE,
    )
}
