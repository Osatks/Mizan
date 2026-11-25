package com.osatks.mizan.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.ui.LinkedText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osatks.mizan.ui.ToolPage

/**
 * المصادر والتراخيص: نص المصحف وترجمات المعاني والتفاسير والتلاوات وتسجيلات الأذان والخط،
 * ونصّا الرخصتين كما هما (رخصة الخط SIL OFL، وبيان تسجيلات الأذان ومصادرها).
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val lang = LocalLang.current
    val font = remember { Platform.asset("licenses/AmiriQuran-OFL.txt")?.decodeToString()?.trim() }
    val adhan = remember { Platform.asset("licenses/adhan-recordings.txt")?.decodeToString()?.trim() }
    ToolPage(s.licenses, s.licensesSub, onBack, icon = Icons.Filled.Info, plainIcon = true) {
        item { LicensesCard(s.quranTitle, selectable = false) { LinkedText(s.quranSources) } }
        item {
            // مصحف المدينة: خطوط المجمع وبيانات الأسطر
            LicensesCard(com.osatks.mizan.i18n.ScriptText.of(lang.code).madinah) {
                Text(
                    "King Fahd Glorious Quran Printing Complex (KFGQPC), Madinah: QPC V2 page fonts, surah header font and " +
                        "Uthmanic Hafs font, used unmodified. Page layout and glyph data: Quranic Universal Library " +
                        "(qul.tarteel.ai) and Quran.com. Hadith of the day: HadeethEnc.com.",
                    style = licenseStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            LicensesCard(s.tafsirLibrary) {
                Text(s.tafsirLibraryNote, style = licenseStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            LicensesCard(s.reciter) {
                Text(
                    "EveryAyah.com · " + Reciters.ALL.joinToString(" · ") { it.name(lang) },
                    style = licenseStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            LicensesCard(s.recordingSources) {
                Muezzins.ALL.forEach { m ->
                    Text("${s.muezzinName(m)}: ${m.credit}", style = licenseStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (adhan != null) item { LicensesCard(adhan.substringBefore('\n')) { LicenseText(adhan.substringAfter('\n').trim()) } }
        if (font != null) item { LicensesCard("Amiri Quran · SIL Open Font License 1.1") { LicenseText(font) } }
    }
}

@Composable
private fun LicenseText(text: String) =
    Text(text, style = licenseStyle(), color = MaterialTheme.colorScheme.onSurfaceVariant)

/** نص المصادر: 13 كما في iPhone */
@Composable
private fun licenseStyle() = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp)

/** بطاقة مصدر كما في iPhone: بيضاء بزوايا 22 وحد رفيع، والعنوان بلون التطبيق، والنص قابل للتحديد والنسخ */
@Composable
private fun LicensesCard(title: String, selectable: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth()
            .shadow(1.dp, shape, ambientColor = Color.Black.copy(alpha = .04f), spotColor = Color.Black.copy(alpha = .06f))
            .clip(shape).background(cs.surface).border(1.dp, cs.outlineVariant.copy(alpha = .8f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title, Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = cs.primary
        )
        // نص الروابط خارج التحديد حتى تبقى روابطه تُفتح باللمس
        if (selectable) SelectionContainer { Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content) }
        else Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}
