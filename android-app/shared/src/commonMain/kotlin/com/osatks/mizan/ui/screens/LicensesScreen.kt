package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Muezzins
import com.osatks.mizan.i18n.LocalLang
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.platform.Platform
import com.osatks.mizan.quran.Reciters
import com.osatks.mizan.ui.LinkedText
import com.osatks.mizan.ui.SectionCard
import com.osatks.mizan.ui.ToolPage

/**
 * المصادر والتراخيص: نص المصحف وترجمات المعاني والتفاسير والتلاوات وتسجيلات الأذان والخط،
 * ونصّا الرخصتين كما هما (رخصة الخط SIL OFL، وبيان تسجيلات الأذان ومصادرها).
 */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    // في iOS: الصفحة بواجهة النظام (SwiftUI)
    if (nativeLicensesAvailable) {
        NativeLicensesView(onBack, Modifier.fillMaxSize())
        return
    }
    val s = LocalStrings.current
    val lang = LocalLang.current
    val font = remember { Platform.asset("licenses/AmiriQuran-OFL.txt")?.decodeToString()?.trim() }
    val adhan = remember { Platform.asset("licenses/adhan-recordings.txt")?.decodeToString()?.trim() }
    ToolPage(s.licenses, s.licensesSub, onBack) {
        item { SectionCard(s.quranTitle) { LinkedText(s.quranSources) } }
        item {
            // مصحف المدينة: خطوط المجمع وبيانات الأسطر
            SectionCard(com.osatks.mizan.i18n.ScriptText.of(lang.code).madinah) {
                Text(
                    "King Fahd Glorious Quran Printing Complex (KFGQPC), Madinah: QPC V2 page fonts, surah header font and " +
                        "Uthmanic Hafs font, used unmodified. Page layout and glyph data: Quranic Universal Library " +
                        "(qul.tarteel.ai) and Quran.com. Hadith of the day: HadeethEnc.com.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SectionCard(s.tafsirLibrary) {
                Text(s.tafsirLibraryNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(s.reciter) {
                Text(
                    "EveryAyah.com · " + Reciters.ALL.joinToString(" · ") { it.name(lang) },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SectionCard(s.recordingSources) {
                Muezzins.ALL.forEach { m ->
                    Text("${s.muezzinName(m)}: ${m.credit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (adhan != null) item { SectionCard(adhan.substringBefore('\n')) { LicenseText(adhan.substringAfter('\n').trim()) } }
        if (font != null) item { SectionCard("Amiri Quran · SIL Open Font License 1.1") { LicenseText(font) } }
    }
}

@Composable
private fun LicenseText(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
