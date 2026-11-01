package com.osatks.mizan.ui

import androidx.compose.runtime.Composable

/** أندرويد: نافذة التنبيه الزجاجية نفسها التي في التطبيق (Compose) */
@Composable
actual fun PlatformDialog(onDismissRequest: () -> Unit, title: String, message: String?, actions: List<DialogAction>) =
    GlassDialog(onDismissRequest, title, message, actions)
