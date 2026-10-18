package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/** زر الرجوع (أندرويد) وإيماءة الرجوع؛ في iOS يُستعمل زر الرجوع في رأس الصفحة */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) = androidx.compose.ui.backhandler.BackHandler(enabled, onBack)
