package com.osatks.mizan.platform

import androidx.compose.ui.graphics.ImageBitmap

/** الصورة ملف PNG (null إن تعذّر): لمشاركة بطاقة الآية */
expect fun ImageBitmap.encodePng(): ByteArray?
