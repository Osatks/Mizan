package com.osatks.mizan.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.osatks.mizan.ui.theme.HeroBrush

/** رأس الصفحة المتدرج مع زر الرجوع */
@Composable
fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(HeroBrush)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            } else Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .85f))
            }
        }
    }
}

/** صفحة أداة: رأس + محتوى قابل للتمرير */
@Composable
fun ToolPage(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        PageHeader(title, subtitle, onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun SectionCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

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
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        MiniStepper(value, onChange, min, max)
    }
}

@Composable
fun MiniStepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 99) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Filled.Remove, "إنقاص", enabled = value > min) { onChange(value - 1) }
        Text("$value", Modifier.width(36.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        StepButton(Icons.Filled.Add, "زيادة", enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

/** أزرار اختيار متجاورة */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(4.dp)) {
            options.forEach { (key, label) ->
                val on = key == selected
                Surface(
                    onClick = { onSelect(key) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (on) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (on) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label, Modifier.padding(vertical = 10.dp, horizontal = 6.dp), textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** قائمة منسدلة بسيطة */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
        Box {
            Surface(
                onClick = { open = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text(selected), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Filled.ArrowDropDown, null)
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, strong: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.Bold)
        }
    }
}

/** بطاقة النتيجة الكبيرة */
@Composable
fun ResultCard(label: String, value: String, badge: String? = null, badgeOk: Boolean? = null, content: @Composable ColumnScope.() -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.background(HeroBrush).padding(22.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
            if (badge != null) {
                val bg = when (badgeOk) { true -> Color(0xFF3FB595); false -> Color(0xFFEF8A76); null -> Color(0xFFE0B54A) }
                Surface(shape = CircleShape, color = bg.copy(alpha = .25f)) {
                    Text(badge, Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
            content()
        }
    }
}

@Composable
fun Note(text: String, warn: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (warn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (warn) Icons.Filled.WarningAmber else Icons.Filled.Info, null,
                tint = if (warn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, color = if (warn) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RowScope.Tile(value: String, label: String) {
    Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "$text\n— تطبيق ميزان")
    ctx.startActivity(Intent.createChooser(i, "مشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** صف قابل للنقر بنص وقيمة (للإعدادات) */
@Composable
fun ClickRow(title: String, subtitle: String? = null, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
