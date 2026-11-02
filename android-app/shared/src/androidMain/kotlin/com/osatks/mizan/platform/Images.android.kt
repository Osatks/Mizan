package com.osatks.mizan.platform

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodePng(): ByteArray? = runCatching {
    val bmp = asAndroidBitmap()
    // صورة الطبقة قد تكون في ذاكرة الرسوميات (HARDWARE): نسخة عادية قبل الضغط
    val plain = if (Build.VERSION.SDK_INT >= 26 && bmp.config == Bitmap.Config.HARDWARE) bmp.copy(Bitmap.Config.ARGB_8888, false) else bmp
    ByteArrayOutputStream().also { plain.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}.getOrNull()
