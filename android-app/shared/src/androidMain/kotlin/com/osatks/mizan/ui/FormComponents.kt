package com.osatks.mizan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String? = null, hint: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            suffix = suffix?.let { s -> @Composable { Text(s) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
}

@Composable
fun RowScope.Tile(value: String, label: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(value, MaterialTheme.typography.titleLarge, MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

/** نص تصبح فيه tanzil.net و tanzil.net/trans و SIL روابط تُفتح في المتصفح */
@Composable
fun LinkedText(text: String) {
    val color = MaterialTheme.colorScheme.primary
    val styles = androidx.compose.ui.text.TextLinkStyles(
        androidx.compose.ui.text.SpanStyle(color = color, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
    )
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        var last = 0
        Regex("tanzil\\.net/trans|tanzil\\.net|SIL").findAll(text).forEach { m ->
            append(text.substring(last, m.range.first))
            val url = when (m.value) { "SIL" -> "https://openfontlicense.org"; "tanzil.net/trans" -> "https://tanzil.net/trans/"; else -> "https://tanzil.net/" }
            withLink(androidx.compose.ui.text.LinkAnnotation.Url(url, styles)) { append(m.value) }
            last = m.range.last + 1
        }
        append(text.substring(last))
    }
    Text(annotated, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** فراغ بارتفاع ثابت (للمسافات بين عناصر الشبكات) */
@Composable
fun Gap(d: Dp) = Spacer(Modifier.size(d))
