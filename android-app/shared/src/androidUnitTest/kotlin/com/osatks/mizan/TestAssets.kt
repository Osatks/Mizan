package com.osatks.mizan

import java.io.File

/**
 * ملفات المحتوى في الاختبارات: المضمّن في التطبيق (assets) ثم ما ينزّله التطبيق من الموقع ولم يعد مضمّناً
 * (src/content في المستودع: ترجمات المعاني وأحاديث اليوم والأذكار بغير العربية والإنجليزية)، فتبقى الاختبارات
 * والملفات المولَّدة منها (Fixtures، Static) بكل اللغات كما كانت.
 */
object TestAssets {
    val roots = listOf(File("src/commonMain/assets"), File("../../src/content"))

    /** الملف بمساره في assets (tafsir/fr.hamidullah.txt)، أو null إن لم يوجد في أي منهما */
    fun read(path: String): ByteArray? = file(path).takeIf { it.isFile }?.readBytes()

    fun file(path: String): File = roots.map { File(it, path) }.firstOrNull { it.exists() } ?: File(roots[0], path)

    /** أسماء ملفات مجلد في الموضعين معاً */
    fun list(dir: String): List<String> = roots.flatMap { File(it, dir).list()?.toList().orEmpty() }.distinct().sorted()
}
