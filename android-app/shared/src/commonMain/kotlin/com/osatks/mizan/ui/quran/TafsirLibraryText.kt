package com.osatks.mizan.ui.quran

import com.osatks.mizan.i18n.Lang

/** «ع» للعربية، ورمز اللغة لغيرها (كما في [LangBadge]) */
internal fun badge(l: Lang) = if (l == Lang.AR) "ع" else l.code.uppercase()

/** يعزل النص باتجاهه (FSI…PDI) حتى لا يختلط ترتيبه مع ما حوله إذا اختلفت اللغتان */
internal fun isolate(t: String) = "\u2068$t\u2069"
