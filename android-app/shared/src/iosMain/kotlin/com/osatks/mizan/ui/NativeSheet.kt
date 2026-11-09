package com.osatks.mizan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.osatks.mizan.ui.screens.NativeSheetControl
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIPresentationController
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UISheetPresentationControllerDetentIdentifierLarge
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * جسر أوراق النظام (iosApp/iosApp/NativeSheets.swift): يسجّل Swift مصنعاً يبني متحكم المحتوى (SwiftUI) لكل نوع،
 * و Kotlin يعرضه ورقةً حقيقية (UISheetPresentationController) فوق أعلى متحكم ظاهر، ويغلقه، ويسمع إغلاقه بالسحب.
 */
object NativeSheets {
    const val STATE = "MizanSheetState"

    private var factory: ((NativeSheetSession) -> UIViewController?)? = null

    fun attach(make: (NativeSheetSession) -> UIViewController?) {
        factory = make
    }

    internal val available: Boolean get() = factory != null

    internal fun make(session: NativeSheetSession): UIViewController? = factory?.invoke(session)
}

/** جلسة ورقة واحدة: نوعها وحالتها (JSON) وما يرسله Swift من لمسات */
class NativeSheetSession internal constructor(val kind: String) {
    var state: String = ""
        private set

    internal var onSend: (String, String) -> Unit = { _, _ -> }

    internal fun prime(json: String) {
        if (state.isEmpty()) state = json
    }

    internal fun publish(json: String) {
        if (json == state) return
        state = json
        NSNotificationCenter.defaultCenter.postNotificationName(NativeSheets.STATE, this)
    }

    /** فعل من الورقة: [action] اسمه و[arg] وسيطه (رمز اللغة، مفتاح المدينة، نص البحث...) */
    fun send(action: String, arg: String) = onSend(action, arg)
}

/** يسمع إغلاق الورقة بسحبها أو بلمس ما حولها (لا يُستدعى عند إغلاقها من Kotlin) */
private class SheetDismissWatch(private val onDismiss: () -> Unit) : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) = onDismiss()
}

/** ورقة معروضة: تُعرض مرة واحدة وتُغلق مرة واحدة */
private class PresentedSheet(
    private val session: NativeSheetSession,
    private val startLarge: Boolean,
    private val onGone: () -> Unit,
) : NativeSheetControl {
    private var vc: UIViewController? = null
    private var closed = false
    private val watch = SheetDismissWatch { gone() }

    private fun gone() {
        if (closed) return
        closed = true
        onGone()
    }

    fun show() {
        val c = NativeSheets.make(session) ?: return gone()
        // بارتفاعين (نصف الشاشة وكلها) ومقبض؛ وعلى iPad نافذة في الوسط كما يعرض النظام أوراقه
        c.sheetPresentationController?.let { sp ->
            sp.detents = listOf(UISheetPresentationControllerDetent.mediumDetent(), UISheetPresentationControllerDetent.largeDetent())
            sp.prefersGrabberVisible = true
            if (startLarge) sp.selectedDetentIdentifier = UISheetPresentationControllerDetentIdentifierLarge
        }
        c.presentationController?.delegate = watch
        vc = c
        present(c, 0)
    }

    // إن كان متحكم آخر يظهر أو يختفي الآن (نافذة تنبيه تُغلق مثلاً) يرفض النظام العرض؛ فنعيد المحاولة بعد قليل
    private fun present(c: UIViewController, attempt: Int) {
        if (closed || c.presentingViewController != null) return
        topController()?.presentViewController(c, true, null)
        if (c.presentingViewController != null) return
        if (attempt >= 10) return gone()
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 200_000_000L), dispatch_get_main_queue()) { present(c, attempt + 1) }
    }

    override fun close(then: () -> Unit) {
        if (closed) { then(); return }
        closed = true
        // من المتحكم الذي عرضها: فتُغلق هي وما فوقها (نافذة تأكيد مثلاً)
        val presenter = vc?.presentingViewController
        if (presenter != null) presenter.dismissViewControllerAnimated(true) { then() } else then()
    }

    /** خرجت الورقة من التركيب: تُغلق بلا إخبار */
    fun dispose() {
        if (closed) return
        closed = true
        vc?.presentingViewController?.dismissViewControllerAnimated(true, null)
    }
}

/** أعلى متحكم ظاهر الآن: تُعرض الورقة فوقه */
private fun topController(): UIViewController? {
    var top: UIViewController = IosStatusBar.controller ?: UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}

/** تعرض ورقة النظام ما دامت في التركيب، وتنقل إليها كل تغيير في [json] (انظر NativeSheetHost) */
@Composable
internal fun NativeSheetPresenter(
    kind: String,
    json: String,
    startLarge: Boolean,
    onDismiss: () -> Unit,
    onAction: NativeSheetControl.(action: String, arg: String) -> Unit,
) {
    val session = remember(kind) { NativeSheetSession(kind) }
    val dismissS by rememberUpdatedState(onDismiss)
    val actionS by rememberUpdatedState(onAction)
    session.prime(json)
    SideEffect { session.publish(json) }
    DisposableEffect(session) {
        val sheet = PresentedSheet(session, startLarge) { dismissS() }
        session.onSend = { a, b -> actionS(sheet, a, b) }
        sheet.show()
        onDispose {
            session.onSend = { _, _ -> }
            sheet.dispose()
        }
    }
}
