package com.osatks.mizan.ui

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CACurrentMediaTime
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.kCACornerCurveContinuous
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionBeginFromCurrentState
import platform.UIKit.UIViewAnimationOptionCurveEaseIn
import platform.UIKit.UIViewAnimationOptionCurveEaseOut
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

/**
 * انتقالات الصفحات الفرعية داخل صفحة واحدة في iOS بـ Core Animation ([NavTransitions]: أقسام الأذكار وأنواع الزكاة):
 * قبل التغيير تُلتقط صورة الصفحة كما هي، ثم تتغير في Compose فوراً تحتها، وتتحرك الصورة والصفحة الحية معاً. التنقل بين
 * الشاشات نفسها في UIKit (MizanNavigation.swift) بالحركة نفسها، فتبدو الصفحات الفرعية صفحاتٍ كغيرها.
 *
 * فتح صفحة: تتراجع السابقة فوراً (تصغر وتعتم بزوايا مستديرة فوق خلفية سوداء)، ثم تنمو الجديدة من موضع اللمس حين يُرسم
 * أول إطار منها. الرجوع عكسه: تصغر نحو الموضع الذي فُتحت منه وتختفي. مع «تقليل الحركة»: تلاشٍ فقط.
 * [root]: واجهة متحكم الصفحة، و [content] واجهة Compose فيها.
 */
@OptIn(ExperimentalForeignApi::class)
internal class NavMotion(private val root: UIView) : NavTransitions {
    var content: UIView? = null

    /** ينهي الانتقال الجاري فوراً (قبل انتقال جديد) */
    private var finish: (() -> Unit)? = null

    private val reduce: Boolean get() = UIAccessibilityIsReduceMotionEnabled()

    private fun size(): Pair<Double, Double> = root.bounds.useContents { size.width to size.height }

    /** موضع النمو: حيث لمس الإصبع قبل قليل (بطاقة، سطر)، وإلا منتصف الصفحة */
    private fun origin(): Origin {
        val (w, h) = size()
        val t = com.osatks.mizan.IosNav.recentTouch() ?: return Origin(w / 2, h / 2)
        // اللمس بنقاط النافذة، والصفحة تملؤها عادة
        return root.convertPoint(CGPointMake(t.x, t.y), fromView = null).useContents { Origin(x.coerceIn(0.0, w), y.coerceIn(0.0, h)) }
    }

    override fun push(change: () -> Unit): Any? {
        finish?.invoke()
        val c = content
        val old = if (c != null && root.window != null) root.snapshotViewAfterScreenUpdates(false) else null
        if (c == null || old == null) {
            change()
            return null
        }
        val o = origin()
        if (reduce) {
            fade(old, change)
            return o
        }
        val bg = root.backgroundColor
        val clips = c.clipsToBounds
        old.setFrame(root.bounds)
        old.userInteractionEnabled = false
        old.layer.cornerCurve = kCACornerCurveContinuous
        old.clipsToBounds = true
        // الصورة تحت الشاشة الحية، والحية مخفية حتى تُرسم الصفحة الجديدة
        root.insertSubview(old, atIndex = 0)
        root.backgroundColor = UIColor.blackColor
        c.alpha = 0.0
        root.userInteractionEnabled = false
        change()
        // تتراجع السابقة فوراً: يحرّكها خادم العرض ولو انشغل الخيط الرئيسي ببناء الجديدة
        UIView.animateWithDuration(
            0.5, 0.0, 1.0, 0.0, UIViewAnimationOptionCurveEaseOut,
            { old.transform = CGAffineTransformMakeScale(BACK_SCALE, BACK_SCALE); old.layer.cornerRadius = RADIUS; old.alpha = BACK_ALPHA },
            null,
        )
        var waiter: FrameWaiter? = null
        var done = false
        val end = {
            if (!done) {
                done = true
                waiter?.stop()
                c.layer.removeAllAnimations()
                c.transform = identity()
                c.alpha = 1.0
                c.layer.cornerRadius = 0.0
                c.clipsToBounds = clips
                old.removeFromSuperview()
                root.backgroundColor = bg
                root.userInteractionEnabled = true
                finish = null
            }
        }
        finish = end
        waiter = FrameWaiter(PUSH_WAIT) {
            if (!done) {
                val (w, h) = size()
                c.transform = zoom(START_SCALE, o, w, h)
                c.layer.cornerCurve = kCACornerCurveContinuous
                c.layer.cornerRadius = RADIUS
                c.clipsToBounds = true
                UIView.animateWithDuration(0.22, 0.0, UIViewAnimationOptionCurveEaseOut, { c.alpha = 1.0 }, null)
                UIView.animateWithDuration(
                    0.5, 0.0, 1.0, 0.0, UIViewAnimationOptionBeginFromCurrentState,
                    { c.transform = identity(); c.layer.cornerRadius = 0.0 },
                    { _ -> end() },
                )
            }
        }.also { it.start() }
        guard(end)
        return o
    }

    override fun pop(origin: Any?, change: () -> Unit) {
        finish?.invoke()
        val c = content
        val top = if (c != null && root.window != null) root.snapshotViewAfterScreenUpdates(false) else null
        if (c == null || top == null) {
            change()
            return
        }
        if (reduce) {
            fade(top, change)
            return
        }
        val (w0, h0) = size()
        val o = origin as? Origin ?: Origin(w0 / 2, h0 / 2)
        val bg = root.backgroundColor
        val clips = c.clipsToBounds
        top.setFrame(root.bounds)
        top.userInteractionEnabled = false
        top.layer.cornerCurve = kCACornerCurveContinuous
        top.clipsToBounds = true
        // الصفحة الخارجة فوق كل شيء، والتي تحتها (مغطاة بها حتى تبدأ الحركة) تعود من تراجعها كما تُركت حين فُتحت الخارجة
        root.addSubview(top)
        root.backgroundColor = UIColor.blackColor
        root.userInteractionEnabled = false
        change()
        var waiter: FrameWaiter? = null
        var done = false
        val end = {
            if (!done) {
                done = true
                waiter?.stop()
                c.layer.removeAllAnimations()
                c.transform = identity()
                c.alpha = 1.0
                c.layer.cornerRadius = 0.0
                c.clipsToBounds = clips
                top.removeFromSuperview()
                root.backgroundColor = bg
                root.userInteractionEnabled = true
                finish = null
            }
        }
        finish = end
        waiter = FrameWaiter(POP_WAIT) {
            if (!done) {
                val (w, h) = size()
                // التحجيم يوضع ويُحرَّك إلى الأصل في الدورة نفسها: لا يرى تخطيط UIKit الواجهةَ مصغّرة أبداً
                // (لو بقيت كذلك لتغيّرت مسافات المنطقة الآمنة في Compose وقفز محتواها)
                val back = CGAffineTransformMakeScale(BACK_SCALE, BACK_SCALE)
                c.transform = back
                c.alpha = BACK_ALPHA
                c.layer.cornerCurve = kCACornerCurveContinuous
                c.layer.cornerRadius = RADIUS
                c.clipsToBounds = true
                UIView.animateWithDuration(
                    0.44, 0.0, 1.0, 0.0, UIViewAnimationOptionBeginFromCurrentState,
                    {
                        c.transform = identity()
                        c.alpha = 1.0
                        c.layer.cornerRadius = 0.0
                        top.transform = zoom(EXIT_SCALE, o, w, h)
                        top.layer.cornerRadius = RADIUS
                    },
                    { _ -> end() },
                )
                UIView.animateWithDuration(0.26, 0.05, UIViewAnimationOptionCurveEaseIn, { top.alpha = 0.0 }, null)
            }
        }.also { it.start() }
        guard(end)
    }

    /** تلاشي صورة الصفحة السابقة فوق الحية بعد أن تُرسم الجديدة (مع «تقليل الحركة») */
    private fun fade(old: UIView, change: () -> Unit) {
        if (content == null) return change()
        old.setFrame(root.bounds)
        old.userInteractionEnabled = false
        root.addSubview(old)
        root.userInteractionEnabled = false
        change()
        var waiter: FrameWaiter? = null
        var done = false
        val end = {
            if (!done) {
                done = true
                waiter?.stop()
                old.removeFromSuperview()
                root.userInteractionEnabled = true
                finish = null
            }
        }
        finish = end
        waiter = FrameWaiter(FADE_WAIT) {
            if (!done) UIView.animateWithDuration(0.3, 0.0, UIViewAnimationOptionCurveEaseOut, { old.alpha = 0.0 }, { _ -> end() })
        }.also { it.start() }
        guard(end)
    }

    /** احتياط: لا تبقى صورة فوق الشاشة ولا لمس معطل إن لم تكتمل حركة لسبب ما */
    private fun guard(end: () -> Unit) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2_500_000_000L), dispatch_get_main_queue()) { end() }
    }

    private companion object {
        const val RADIUS = 38.0
        /** الصفحة السابقة وهي متراجعة */
        const val BACK_SCALE = 0.94
        const val BACK_ALPHA = 0.72
        /** حجم الصفحة الجديدة حين تبدأ النمو، وحجم الخارجة حين تختفي */
        const val START_SCALE = 0.88
        const val EXIT_SCALE = 0.86
        /** أطول انتظار لأول إطار من الصفحة الجديدة قبل أن تبدأ الحركة على أي حال */
        const val PUSH_WAIT = 0.5
        const val POP_WAIT = 0.3
        const val FADE_WAIT = 0.35
    }
}

/** موضع اللمس الذي فُتحت منه الصفحة (بنقاط الشاشة) */
internal class Origin(val x: Double, val y: Double)

@OptIn(ExperimentalForeignApi::class)
private fun identity(): CValue<CGAffineTransform> = CGAffineTransformMakeScale(1.0, 1.0)

/** تكبير بمقدار [s] حول [o] بدل منتصف الطبقة */
@OptIn(ExperimentalForeignApi::class)
private fun zoom(s: Double, o: Origin, w: Double, h: Double): CValue<CGAffineTransform> =
    CGAffineTransformMake(s, 0.0, 0.0, s, (1 - s) * (o.x - w / 2), (1 - s) * (o.y - h / 2))

/**
 * ينتظر أن تُرسم الصفحة الجديدة: إطارات الشاشة تتوالى بلا تأخر بعد التغيير (ثلاثة على الأقل، والأخير في وقته)،
 * أو [maxWait] ثوانٍ على الأكثر، ثم ينفذ [then] مرة واحدة.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class FrameWaiter(private val maxWait: Double, private val then: () -> Unit) : NSObject() {
    private var link: CADisplayLink? = null
    private var frames = 0
    private var last = 0.0
    private val start = CACurrentMediaTime()

    fun start() {
        link = CADisplayLink.displayLinkWithTarget(this, NSSelectorFromString("tick:")).also {
            it.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
        }
    }

    @ObjCAction
    fun tick(sender: CADisplayLink) {
        val t = sender.timestamp
        val dt = if (frames == 0) 1.0 else t - last
        last = t
        frames++
        if ((frames >= 3 && dt < 0.034) || CACurrentMediaTime() - start > maxWait) {
            stop()
            then()
        }
    }

    fun stop() {
        link?.invalidate()
        link = null
    }
}
