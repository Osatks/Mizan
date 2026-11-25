package com.osatks.mizan.quran

import com.osatks.mizan.core.Search

/**
 * نصوص فهرسة الآية في بحث النظام (Spotlight في iPhone): المستخدم يكتب بلا تشكيل، وقد يكتب الهمزات أو يتركها.
 * فتُفهرس كلمات الآية بلا تشكيل كما في الرسم الإملائي (بهمزاتها)، ومطبَّعةً كما يطبّع بحث التطبيق ([Search.normalize]:
 * صور الألف والتاء المربوطة والألف المقصورة…)، وكلمات الرسم العثماني مطبَّعةً كذلك (ٱلرَّحْمَـٰنِ ← الرحمن).
 */
object AyahIndexText {
    private val SPACES = Regex("\\s+")

    /** يحذف التشكيل وعلامات الوقف والضبط في الرسم العثماني والتطويل، ويبقي الحروف كما هي (الهمزات وصور الألف) */
    fun stripMarks(text: String): String = buildString(text.length) {
        for (ch in text) {
            when {
                // الحركات والتنوين والشدة والسكون والمدّة والهمزة المركّبة، والألف الخنجرية، وعلامات المصحف الصغيرة
                ch in 'ً'..'ٟ' || ch == 'ٰ' || ch in 'ۖ'..'ۭ' || ch == 'ـ' -> {}
                // ألف الوصل
                ch == 'ٱ' -> append('ا')
                else -> append(ch)
            }
        }
    }.replace(SPACES, " ").trim()

    /** صيغتا نص الآية للبحث: بلا تشكيل كما يُكتب، ومطبَّعاً كما يطبّعه بحث التطبيق (بلا تكرار) */
    fun alternates(clean: String, uthmani: String): List<String> {
        val plain = stripMarks(clean.ifBlank { uthmani })
        return listOf(plain, Search.normalize(plain)).filter { it.isNotBlank() }.distinct()
    }

    /** كلمات الآية المميزة (حرفان فأكثر): الإملائية بهمزاتها، ومطبّعةً، وكلمات الرسم العثماني مطبّعةً */
    fun keywords(clean: String, uthmani: String): List<String> {
        val out = LinkedHashSet<String>()
        fun add(text: String) { for (w in text.split(' ')) if (w.length >= 2) out += w }
        val plain = stripMarks(clean)
        add(plain)
        add(Search.normalize(plain))
        add(Search.normalize(stripMarks(uthmani)))
        return out.toList()
    }

    /** هل يصل بحث المستخدم إلى الآية بكلماتها: كل كلمة من بحثه (مطبَّعة) أول كلمة من كلماتها المطبَّعة، كما يطابق Spotlight */
    fun matches(query: String, keywords: List<String>): Boolean {
        val terms = Search.terms(query)
        if (terms.isEmpty()) return false
        val norm = keywords.map { Search.normalize(it) }
        return terms.all { t -> norm.any { it.startsWith(t) } }
    }
}
