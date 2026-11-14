package com.osatks.mizan.ui.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset

/**
 * ميل الجهاز عن وضع الإمساك المعتاد، لكل محور بين -1 و 1 (الكلمات البارزة في الوضع ثلاثي الأبعاد تتحرك به).
 * الوضع المرجعي يتبع طريقة الإمساك ببطء، فتعود الكلمات إلى مكانها حين يستقر الجهاز على أي زاوية.
 * يتوقف عن قراءة الحساسات حين لا يُطلب (enabled = false) أو تخرج الصفحة من الشاشة.
 * أندرويد: حساس الجاذبية؛ iOS: صفحة المصحف من النظام تقرأ الحركة بنفسها (NativeMushaf.swift)، فهنا لا ميل.
 */
@Composable
expect fun rememberDeviceTilt(enabled: Boolean): State<Offset>
