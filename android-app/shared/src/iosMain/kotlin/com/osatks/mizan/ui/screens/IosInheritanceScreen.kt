package com.osatks.mizan.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.osatks.mizan.core.Demo
import com.osatks.mizan.core.Inheritance
import com.osatks.mizan.i18n.LocalStrings
import com.osatks.mizan.ui.shareText

/**
 * حاسبة المواريث في iOS بواجهة النظام (SwiftUI) بطراز الرئيسية: المدخلات هنا، وكل لمسة تعود إليها فتُحسب القسمة
 * وتُنسّق في Kotlin كما في شاشة Compose في أندرويد (InheritanceScreen)
 */
@Composable
internal fun IosInheritanceScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    var gender by remember { mutableStateOf('m') }
    val counts = remember { mutableStateMapOf<String, Int>().apply { if (Demo.enabled) putAll(Demo.heirs) } }
    var estate by remember { mutableStateOf(if (Demo.enabled) Demo.ESTATE else "") }
    var funeral by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }
    var will by remember { mutableStateOf("") }

    val (net, willCapped) = estateNet(estate, funeral, debts, will)
    val active = activeHeirs(counts, gender)
    val result = Inheritance.solve(active, gender)

    NativeInheritanceView(
        gender, counts, active, estate, funeral, debts, will, result, net, willCapped,
        onGender = { gender = it },
        onCount = { k, v -> counts[k] = v },
        onField = { k, v ->
            when (k) {
                "estate" -> { estate = v }
                "funeral" -> { funeral = v }
                "debts" -> { debts = v }
                "will" -> { will = v }
            }
        },
        onReset = { counts.clear(); estate = ""; funeral = ""; debts = ""; will = "" },
        onShare = { if (result != null) shareText(s, inheritanceShareText(s, active, result)) },
        onBack = onBack,
        modifier = Modifier.fillMaxSize(),
    )
}
