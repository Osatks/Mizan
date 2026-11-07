package com.osatks.mizan.core

import com.osatks.mizan.platform.Platform

/**
 * «أرسل ملاحظاتك»: رسالة بريد إلى عنوان التواصل المنشور في الموقع، وفي موضوعها الإصدار والنظام ولغة التطبيق،
 * فيُعرف أين ظهرت المشكلة دون أن يكتبها المستخدم. لا يُرسل شيء إلا من تطبيق البريد وبيد المستخدم.
 */
object Feedback {
    const val EMAIL = "mizanalmuslimhilal@gmail.com"

    fun mailto(lang: String): String {
        val platform = if (Platform.name == "ios") "iOS" else "Android"
        val subject = "Mizan ${Platform.versionName()} · $platform · $lang"
        return "mailto:$EMAIL?subject=${encode(subject)}"
    }

    /** ترميز نسبي UTF-8 لجزء من الرابط (ما عدا الحروف والأرقام و -._~) */
    fun encode(s: String): String = buildString {
        for (b in s.encodeToByteArray()) {
            val c = b.toInt() and 0xFF
            if (c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code || c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code) append(c.toChar())
            else append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 15])
        }
    }
}
