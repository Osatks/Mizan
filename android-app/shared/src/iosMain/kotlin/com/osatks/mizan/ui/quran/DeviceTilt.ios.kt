package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset

/** صفحة المصحف في iPhone من النظام وتقرأ الحركة بنفسها (MushafDepthMotion في NativeMushaf.swift) */
@Composable
actual fun rememberDeviceTilt(enabled: Boolean): State<Offset> = remember { mutableStateOf(Offset.Zero) }
