package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.osatks.mizan.ui.theme.LocalDark
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * شريط الحالة في iOS: يقرؤه المتحكم الجذر للنافذة (MizanRootController في MainViewController.kt)
 * فيسأله النظام مباشرة؛ ويُخبَر النظام بكل تغيير ليعيد السؤال.
 */
internal object IosStatusBar {
    var light = false
        private set
    var hidden = false
        private set
    /** المتحكم الجذر (يضبطه MainViewController) */
    var controller: UIViewController? = null

    val style: UIStatusBarStyle get() = if (light) UIStatusBarStyleLightContent else UIStatusBarStyleDarkContent

    fun update(light: Boolean, hidden: Boolean) {
        if (light == this.light && hidden == this.hidden) return
        this.light = light
        this.hidden = hidden
        val vc = controller ?: return
        UIView.animateWithDuration(0.25) { vc.setNeedsStatusBarAppearanceUpdate() }
    }
}

@Composable
actual fun ApplySystemBars() {
    val light = SystemBars.lightContent ?: LocalDark.current
    val hidden = SystemBars.hidden
    SideEffect { IosStatusBar.update(light, hidden) }
}
