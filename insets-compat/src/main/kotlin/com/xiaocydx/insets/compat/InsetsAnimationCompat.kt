/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.insets.compat

import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.ime
import com.xiaocydx.insets.Insets
import com.xiaocydx.insets.setWindowInsetsAnimationCallbackCompat
import android.graphics.Insets as GraphicsInsets

/**
 * 修改Android 11及以上IME动画的`durationMillis`和`interpolator`
 *
 * ### 主要作用
 * 该函数的主要作用是结合实际场景需求，微调IME动画的属性，让Android 11及以上的设备有更好的交互体验，
 * 不支持修改Android 11以下IME动画的属性，原因是Android 11以下无法跟IME完全贴合，保持兼容代码即可。
 *
 * ### 修改时机
 * 当[WindowInsetsAnimation.Callback.onPrepare]被调用时，[WindowInsetsAnimation]的属性未被修改，
 * 当[WindowInsetsAnimation.Callback.onStart]被调用时，[WindowInsetsAnimation]的属性已被修改，
 * 即在[WindowInsetsAnimation.Callback.onPrepare]之后，[WindowInsetsAnimation.Callback.onStart]之前修改。
 * 这使得在[WindowInsets]分发的过程中，可以调用该函数修改IME动画的属性。例如，在[WindowInsets]分发的过程中，
 * 先判断当前是显示IME还是隐藏IME，再将IME动画的属性修改为指定的值。
 *
 * ### 修改失败
 * 修改IME动画的属性失败一次，应用运行期间不再修改，修改成功或修改失败，都会按[TAG]打印日志。
 * 修改失败可能会导致动画运行过程无法跟IME完全贴合，这取决于如何使用[WindowInsetsAnimation]。
 *
 * ### 兼容场景
 * 当[WindowInsetsAnimation]的初始`durationMillis <= 0`时，不修改`durationMillis`和`interpolator`，
 * 目的是兼容[WindowInsetsController.controlWindowInsetsAnimation]的`durationMillis <= 0`的场景，
 * 例如通过[WindowInsetsAnimationControlListener.onReady]获取[WindowInsetsAnimationController]，
 * 调用[WindowInsetsAnimationController.setInsetsAndAlpha]实现手势拖动显示IME。
 *
 * **注意**：若要对`window.decorView`设置[WindowInsetsAnimationCompat.Callback]，
 * 则调用[Window.setWindowInsetsAnimationCallbackCompat]，避免跟该函数产生冲突。
 *
 * @param durationMillis IME动画的时长，默认值[NO_VALUE]表示保持系统原本的时长，
 * Android 11以下兼容代码的`durationMillis = 160ms`，
 * Android 11及以上系统代码的`durationMillis = 285ms`。
 *
 * @param interpolator   IME动画的插值器，默认值`null`表示保持系统原本的插值器，
 * Android 11以下兼容代码的`interpolator = DecelerateInterpolator()`，
 * Android 11及以上系统代码的`interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)`。
 */
fun Window.modifyImeAnimation(
    durationMillis: Long = NO_VALUE,
    interpolator: Interpolator? = null,
) {
    if (!isInsetsAnimationCompatNeeded) return
    val compat = InsetsAnimationCompat.get(this)
    compat.typeMask = ime()
    compat.durationMillis = durationMillis
    compat.interpolator = interpolator
}

/**
 * 恢复[modifyImeAnimation]修改的`durationMillis`和`interpolator`
 */
fun Window.restoreImeAnimation() {
    if (!isInsetsAnimationCompatNeeded) return
    val compat = InsetsAnimationCompat.peek(this)
    compat?.typeMask = 0
    compat?.durationMillis = NO_VALUE
    compat?.interpolator = null
}

/**
 * 对`window.decorView`设置[WindowInsetsAnimationCompat.Callback]，
 * 该函数能避免跟[modifyImeAnimation]的实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setWindowInsetsAnimationCallback(window.decorView, callback)
 * ```
 */
fun Window.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    if (!isInsetsAnimationCompatNeeded) {
        decorView.setWindowInsetsAnimationCallbackCompat(callback)
    } else {
        InsetsAnimationCompat.setDelegate(this, callback)
    }
}

@get:ChecksSdkIntAtLeast(api = 30)
private val isInsetsAnimationCompatNeeded: Boolean
    get() = Build.VERSION.SDK_INT >= 30 && Insets.isInsetsAnimationCompatEnabled()

@RequiresApi(30)
private class InsetsAnimationCompat private constructor(window: Window) : WindowAttacher(window) {
    private var insetsCallback: InsetsAnimationCallback? = null

    var typeMask = 0
        set(value) {
            field = value
            insetsCallback?.typeMask = value
        }

    var durationMillis = NO_VALUE
        set(value) {
            field = value
            insetsCallback?.durationMillis = value
        }

    var interpolator: Interpolator? = null
        set(value) {
            field = value
            insetsCallback?.interpolator = value
        }

    override fun onAttach() {
        // onAttach()确保insetsController是InsetsController，而不是PendingInsetsController
        val controller = requireNotNull(window.insetsController) { "InsetsController为null" }
        insetsCallback = InsetsAnimationCallback(controller, window.insetsAnimationDelegate)
        insetsCallback!!.typeMask = typeMask
        insetsCallback!!.durationMillis = durationMillis
        insetsCallback!!.interpolator = interpolator
        // 对decorView设置insetsCallback，是为了避免分发产生歧义：
        // decorView.setWindowInsetsAnimationCallback(callback)
        // childView.setWindowInsetsAnimationCallback(insetsCallback)
        // 1. callback的dispatchMode是DISPATCH_MODE_CONTINUE_ON_SUBTREE。
        // 2. childView是decorView的间接子View。
        // callback.onStart()的WindowInsetsAnimation是原本的属性值，
        // callback.onProgress()的WindowInsetsAnimation却是修改的属性值。
        decorView.setWindowInsetsAnimationCallback(insetsCallback)
        decorView.viewTreeObserver.addOnDrawListener(insetsCallback)
    }

    override fun onDetach() {
        decorView.setWindowInsetsAnimationCallback(window.insetsAnimationDelegate)
        insetsCallback?.let(decorView.viewTreeObserver::removeOnDrawListener)
        insetsCallback = null
    }

    companion object {
        private var Window.insetsAnimationCompat: InsetsAnimationCompat?
            get() = decorView.getTag(R.id.tag_window_insets_animation_compat) as? InsetsAnimationCompat
            set(value) {
                decorView.setTag(R.id.tag_window_insets_animation_compat, value)
            }

        private var Window.insetsAnimationDelegate: WindowInsetsAnimation.Callback?
            get() = decorView.getTag(R.id.tag_window_insets_animation_delegate) as? WindowInsetsAnimation.Callback
            set(value) {
                decorView.setTag(R.id.tag_window_insets_animation_delegate, value)
            }

        @RequiresApi(30)
        fun get(window: Window): InsetsAnimationCompat {
            var compat = window.insetsAnimationCompat
            if (compat == null) {
                compat = InsetsAnimationCompat(window)
                window.insetsAnimationCompat = compat
                compat.attach()
            }
            return compat
        }

        @RequiresApi(30)
        fun peek(window: Window): InsetsAnimationCompat? {
            return window.insetsAnimationCompat
        }

        @RequiresApi(30)
        fun setDelegate(window: Window, callback: WindowInsetsAnimationCompat.Callback?) {
            val proxyCallback = if (callback == null) null else {
                InsetsAnimationReflection.insetsAnimationCompat?.createProxyCallback(callback)
            }
            window.insetsAnimationDelegate = proxyCallback
            val compat = peek(window)
            if (compat != null) {
                compat.detach()
                compat.attach()
            } else {
                window.decorView.setWindowInsetsAnimationCallback(proxyCallback)
            }
        }
    }
}

/**
 * 修改Android 11及以上WindowInsets动画的`durationMillis`和`interpolator`
 *
 * `InsetsController.show()`或`InsetsController.hide()`的执行顺序：
 * 1. 构建`InsetsController.InternalAnimationControlListener`。
 *
 * 2. 构建`InsetsAnimationControlImpl`（因为设置了WindowInsetsAnimation.Callback）。
 *
 * 3. `InsetsAnimationControlImpl`的构造函数调用`InsetsController.startAnimation()`。
 *
 * 4. `InsetsController.startAnimation()`先分发`WindowInsetsAnimation.Callback.onPrepare()`，
 * 再调用`addOnPreDrawRunnable()`，在`preDraw`分发`WindowInsetsAnimation.Callback.onStart()`。
 *
 * 5. `InsetsController.mRunningAnimations.add()`添加`RunningAnimation`，
 * `RunningAnimation`包含第2步构建的`InsetsAnimationControlImpl`。
 *
 * 6. `preDraw`执行第4步添加的`Runnable`，先分发`WindowInsetsAnimation.Callback.onStart()`，
 * 再调用`InsetsController.InternalAnimationControlListener.onReady()`构建并开始属性动画。
 *
 * [onStart]修改第4步的[WindowInsetsAnimation]，[onDraw]修改第6步构建的属性动画。
 *
 * 在[onDraw]修改第6步构建的属性动画的原因：
 * 最初是在[onStart]修改`InsetsController.SYNC_IME_INTERPOLATOR`，使得第6步获取修改结果构建属性动画，
 * 但部分设备的`InsetsController.InternalAnimationControlListener.getInsetsInterpolator()`不会获取
 * 修改结果构建属性动画，导致`durationMillis`修改成功，`interpolator`修改失败。
 *
 * 因此改为在[onDraw]对第6步构建的属性动画添加[ValueAnimator.AnimatorUpdateListener]，
 * 调用[WindowInsetsAnimationController.setInsetsAndAlpha]，覆盖系统代码更新的数值。
 */
@RequiresApi(30)
private class InsetsAnimationCallback(
    private val controller: WindowInsetsController,
    private val delegate: WindowInsetsAnimation.Callback?,
    dispatchMode: Int = delegate?.dispatchMode ?: DISPATCH_MODE_CONTINUE_ON_SUBTREE
) : WindowInsetsAnimation.Callback(dispatchMode), ViewTreeObserver.OnDrawListener {
    private val trackers = mutableMapOf<WindowInsetsAnimation, Tracker>()
    var typeMask = 0
    var durationMillis = NO_VALUE
    var interpolator: Interpolator? = null

    override fun onPrepare(animation: WindowInsetsAnimation) {
        delegate?.onPrepare(animation)
    }

    /**
     * 对视图树分发[animation]之前，修改[animation]的属性值
     */
    override fun onStart(
        animation: WindowInsetsAnimation,
        bounds: WindowInsetsAnimation.Bounds
    ): WindowInsetsAnimation.Bounds {
        val tracker = createTrackerOrNull(animation)
        if (tracker?.step === Step.ON_START) {
            InsetsAnimationReflection.insetsAnimation?.apply {
                tracker.trackInterpolator { animation.setInterpolator(it) }
                tracker.trackDurationMillis { animation.setDurationMillis(it) }
            }
            tracker.checkOnStart()
        }
        return delegate?.onStart(animation, bounds) ?: bounds
    }

    /**
     * [onStart]更改了[Tracker.step]，该函数的逻辑才会真正执行
     */
    override fun onDraw() = with(InsetsAnimationReflection) {
        if (trackers.isEmpty()) return
        val runningAnimations = insetsController?.getRunningAnimations(controller)
        if (runningAnimations.isNullOrEmpty()) return
        trackers.forEach action@{ (animation, tracker) ->
            if (tracker.step !== Step.ON_DRAW) return@action

            // 查找跟animation匹配的InsetsAnimationControlImpl对象，
            // 从InsetsAnimationControlImpl对象获取InternalAnimationControlListener对象，
            // InsetsAnimationControlImpl实现了WindowInsetsAnimationController，
            // 因此能从InsetsAnimationControlImpl对象获取hasZeroInsetsIme属性值。
            var listener: Any? = null
            var hasZeroInsetsIme: Boolean? = null
            for (index in runningAnimations.indices) {
                val runningAnimation = runningAnimations[index]
                val impl = insetsController?.getRunnerFromRunningAnimation(runningAnimation)
                if (insetsAnimationControlImpl?.getAnimation(impl) !== animation) continue
                listener = insetsAnimationControlImpl?.getListener(impl) ?: continue
                hasZeroInsetsIme = insetsAnimationControlImpl?.getHasZeroInsetsIme(impl)
                break
            }

            // 修改InternalAnimationControlListener对象的属性动画，覆盖更新数值
            val controller = animationControlListener?.getController(listener)
            val animator = animationControlListener?.getAnimator(listener)
            val show = animationControlListener?.getShow(listener)
            val floatingImeBottomInset = animationControlListener?.getFloatingImeBottomInset(listener)
            if (controller != null && animator != null && show != null
                    && hasZeroInsetsIme != null && floatingImeBottomInset != null) {
                tracker.trackInterpolator { interpolator ->
                    // insets的计算逻辑copy自InternalAnimationControlListener.onReady()
                    var hiddenInsets = controller.hiddenStateInsets
                    if (hasZeroInsetsIme) {
                        hiddenInsets = GraphicsInsets.of(
                            hiddenInsets.left, hiddenInsets.top,
                            hiddenInsets.right, floatingImeBottomInset
                        )
                    }
                    val start = if (show) hiddenInsets else controller.shownStateInsets
                    val end = if (show) controller.shownStateInsets else hiddenInsets
                    animator.addUpdateListener listener@{
                        // controller已完成或已取消，调用controller.setInsetsAndAlpha()会抛出异常
                        if (controller.isFinished || controller.isCancelled) return@listener
                        val rawFraction = it.animatedFraction
                        val insetsFraction = interpolator.getInterpolation(rawFraction)
                        val insets = insetsEvaluator.evaluate(insetsFraction, start, end)
                        controller.setInsetsAndAlpha(insets, 1f, rawFraction)
                    }
                    true
                }
                tracker.trackDurationMillis {
                    // 虽然已调用animator.start()，但是在下一帧到来之前，仍可以修改duration
                    animator.duration = it
                    true
                }
            }
            tracker.checkOnDraw()
        }
        trackers.clear()
    }

    override fun onProgress(
        insets: WindowInsets,
        runningAnimations: MutableList<WindowInsetsAnimation>
    ): WindowInsets {
        return delegate?.onProgress(insets, runningAnimations) ?: insets
    }

    override fun onEnd(animation: WindowInsetsAnimation) {
        delegate?.onEnd(animation)
    }

    private fun createTrackerOrNull(animation: WindowInsetsAnimation): Tracker? {
        var tracker: Tracker? = trackers[animation]
        if (tracker == null) {
            when {
                !InsetsAnimationReflection.reflectSucceed -> {
                    log("InsetsAnimationReflection反射失败，不做修改")
                }
                animation.typeMask and typeMask == 0 -> {
                    log("WindowInsetsAnimation不包含Insets类型，不做修改")
                }
                animation.durationMillis <= 0 -> {
                    log("兼容WindowInsetsAnimation.durationMillis <= 0的场景，不做修改")
                }
                wasModifyInsetsAnimationFail -> {
                    log("修改失败一次，应用运行期间不再修改")
                }
                else -> if (durationMillis > NO_VALUE || interpolator != null) {
                    tracker = Tracker(durationMillis, interpolator)
                    trackers[animation] = tracker
                }
            }
        }
        return tracker
    }

    private class Tracker(private val durationMillis: Long, private val interpolator: Interpolator?) {
        private var interpolatorOutcome = false
        private var durationMillisOutcome = false
        var step: Step = Step.ON_START
            private set

        inline fun trackInterpolator(modify: (Interpolator) -> Boolean) {
            interpolatorOutcome = if (interpolator == null) true else modify(interpolator)
        }

        inline fun trackDurationMillis(modify: (Long) -> Boolean) {
            durationMillisOutcome = if (durationMillis <= NO_VALUE) true else modify(durationMillis)
        }

        fun checkOnStart() {
            if (step !== Step.ON_START) return
            val outcome = consumeModifyOutcome()
            if (outcome) step = Step.ON_DRAW
            val desc = if (outcome) "成功" else "失败"
            log("onStart()修改WindowInsetsAnimation$desc")
        }

        fun checkOnDraw() {
            if (step !== Step.ON_DRAW) return
            val outcome = consumeModifyOutcome()
            step = Step.COMPLETED
            val desc = if (outcome) "成功" else "失败"
            log("onDraw()修改InternalAnimationControlListener$desc")
        }

        private fun consumeModifyOutcome(): Boolean {
            val outcome = interpolatorOutcome && durationMillisOutcome
            interpolatorOutcome = false
            durationMillisOutcome = false
            recordModifyOutcome(outcome)
            return outcome
        }
    }

    private enum class Step {
        ON_START, ON_DRAW, COMPLETED
    }

    private companion object {
        @Volatile var wasModifyInsetsAnimationFail = false; private set

        private val insetsEvaluator = TypeEvaluator<GraphicsInsets> { fraction, start, end ->
            GraphicsInsets.of(
                (start.left + fraction * (end.left - start.left)).toInt(),
                (start.top + fraction * (end.top - start.top)).toInt(),
                (start.right + fraction * (end.right - start.right)).toInt(),
                (start.bottom + fraction * (end.bottom - start.bottom)).toInt()
            )
        }

        @UiThread
        fun recordModifyOutcome(outcome: Boolean) {
            if (outcome) return
            // 仅记录失败结果，多UI线程修改可能会失败多次，这个影响可以接受
            if (!wasModifyInsetsAnimationFail) wasModifyInsetsAnimationFail = true
        }
    }
}

private const val NO_VALUE = -1L

private const val TAG = "InsetsAnimationCompat"

private fun log(message: String) = Log.d(TAG, message)

@RequiresApi(30)
@SuppressLint("PrivateApi")
private object InsetsAnimationReflection : Reflection {
    var insetsAnimation: WindowInsetsAnimationCache? = null; private set
    var insetsAnimationCompat: WindowInsetsAnimationCompatCache? = null; private set
    var insetsController: InsetsControllerCache? = null; private set
    var insetsAnimationControlImpl: InsetsAnimationControlImplCache? = null; private set
    var animationControlListener: InternalAnimationControlListenerCache? = null; private set
    var reflectSucceed: Boolean = false; private set

    init {
        runCatching {
            reflectInsetsAnimation()
            reflectInsetsAnimationCompat()
            reflectInsetsController()
            reflectInsetsAnimationControlImpl()
            reflectAnimationControlListener()
            reflectSucceed = true
        }.onFailure {
            insetsAnimation = null
            insetsAnimationCompat = null
            insetsController = null
            insetsAnimationControlImpl = null
            animationControlListener = null
        }
    }

    private fun reflectInsetsAnimation() {
        val fields = WindowInsetsAnimation::class.java.declaredInstanceFields
        insetsAnimation = WindowInsetsAnimationCache(
            mInterpolator = fields.find("mInterpolator").toCache(),
            mDurationMillis = fields.find("mDurationMillis").toCache()
        )
    }

    private fun reflectInsetsAnimationCompat() {
        val proxyCallbackClass = Class.forName("androidx.core.view." +
                "WindowInsetsAnimationCompat\$Impl30\$ProxyCallback")
        val proxyCallbackConstructor = proxyCallbackClass
            .getDeclaredConstructor(WindowInsetsAnimationCompat.Callback::class.java).toCache()
        insetsAnimationCompat = WindowInsetsAnimationCompatCache(proxyCallbackConstructor)
    }

    private fun reflectInsetsController() {
        val insetsControllerClass = Class.forName("android.view.InsetsController")
        val runningAnimationClass = Class.forName("android.view.InsetsController\$RunningAnimation")
        val insetsControllerFields = insetsControllerClass.declaredInstanceFields
        val runningAnimationFields = runningAnimationClass.declaredInstanceFields
        insetsController = InsetsControllerCache(
            mRunningAnimations = insetsControllerFields.find("mRunningAnimations").toCache(),
            runner = runningAnimationFields.find("runner").toCache()
        )
    }

    private fun reflectInsetsAnimationControlImpl() {
        val insetsAnimationRunnerClass = Class.forName("android.view.InsetsAnimationControlImpl")
        val fields = insetsAnimationRunnerClass.declaredInstanceFields
        insetsAnimationControlImpl = InsetsAnimationControlImplCache(
            mListener = fields.find("mListener").toCache(),
            mAnimation = fields.find("mAnimation").toCache(),
            mHasZeroInsetsIme = fields.find("mHasZeroInsetsIme").toCache()
        )
    }

    fun reflectAnimationControlListener() {
        val animationControlListenerClass = Class.forName("android.view." +
                "InsetsController\$InternalAnimationControlListener")
        val fields = animationControlListenerClass.declaredInstanceFields
        animationControlListener = InternalAnimationControlListenerCache(
            mController = fields.find("mController").toCache(),
            mAnimator = fields.find("mAnimator").toCache(),
            mShow = fields.find("mShow").toCache(),
            mFloatingImeBottomInset = fields.find("mFloatingImeBottomInset").toCache(),
        )
    }
}

/**
 * ```
 * public final class WindowInsetsAnimation {
 *     private final Interpolator mInterpolator;
 *     private final long mDurationMillis;
 * }
 * ```
 */
@RequiresApi(30)
private class WindowInsetsAnimationCache(
    private val mInterpolator: FieldCache,
    private val mDurationMillis: FieldCache
) {
    fun WindowInsetsAnimation.setInterpolator(interpolator: Interpolator?): Boolean {
        return mInterpolator.set(this, interpolator)
    }

    fun WindowInsetsAnimation.setDurationMillis(durationMillis: Long): Boolean {
        return mDurationMillis.set(this, durationMillis)
    }
}

/**
 * ```
 * public final class WindowInsetsAnimationCompat {
 *
 *     private static class Impl30 extends Impl {
 *         private static class ProxyCallback extends WindowInsetsAnimation.Callback {
 *             ProxyCallback(@NonNull final WindowInsetsAnimationCompat.Callback compat)
 *         }
 *     }
 * }
 * ```
 */
@RequiresApi(30)
private class WindowInsetsAnimationCompatCache(private val proxyCallbackConstructor: ConstructorCache) {
    fun createProxyCallback(callback: WindowInsetsAnimationCompat.Callback) = run {
        proxyCallbackConstructor.newInstance(callback) as? WindowInsetsAnimation.Callback
    }
}

/**
 * ```
 * public class InsetsController implements WindowInsetsController {
 *     private final ArrayList<RunningAnimation> mRunningAnimations = new ArrayList<>();
 *
 *     private static class RunningAnimation {
 *         final InsetsAnimationControlRunner runner;
 *     }
 * }
 * ```
 */
@RequiresApi(30)
private class InsetsControllerCache(
    private val mRunningAnimations: FieldCache,
    private val runner: FieldCache
) {
    fun getRunningAnimations(controller: WindowInsetsController): List<*> {
        return mRunningAnimations.get(controller)?.let { it as? ArrayList<*> } ?: emptyList<Any>()
    }

    fun getRunnerFromRunningAnimation(runningAnimation: Any?): Any? {
        return runningAnimation?.let(runner::get)
    }
}

/**
 * ```
 * public class InsetsAnimationControlImpl implements InternalInsetsAnimationController,
 *          InsetsAnimationControlRunner {
 *     private final WindowInsetsAnimationControlListener mListener;
 *     private final WindowInsetsAnimation mAnimation;
 *     private final boolean mHasZeroInsetsIme;
 * }
 *
 * public interface InternalInsetsAnimationController extends WindowInsetsAnimationController
 * ```
 */
@RequiresApi(30)
private class InsetsAnimationControlImplCache(
    private val mListener: FieldCache,
    private val mAnimation: FieldCache,
    private val mHasZeroInsetsIme: FieldCache,
) {
    fun getListener(impl: Any?): Any? {
        return impl?.let(mListener::get)
    }

    fun getAnimation(impl: Any?): Any? {
        return impl?.let(mAnimation::get)
    }

    fun getHasZeroInsetsIme(impl: Any?): Boolean? {
        return impl?.let(mHasZeroInsetsIme::get) as? Boolean
    }
}

/**
 * ```
 * public static class InternalAnimationControlListener
 *         implements WindowInsetsAnimationControlListener {
 *     private WindowInsetsAnimationController mController;
 *     private ValueAnimator mAnimator;
 *     private final boolean mShow;
 *     private final int mFloatingImeBottomInset;
 * }
 * ```
 */
@RequiresApi(30)
private class InternalAnimationControlListenerCache(
    private val mController: FieldCache,
    private val mAnimator: FieldCache,
    private val mShow: FieldCache,
    private val mFloatingImeBottomInset: FieldCache
) {
    fun getController(listener: Any?): WindowInsetsAnimationController? {
        return listener?.let(mController::get) as? WindowInsetsAnimationController
    }

    fun getAnimator(listener: Any?): ValueAnimator? {
        return listener?.let(mAnimator::get) as? ValueAnimator
    }

    fun getShow(listener: Any?): Boolean? {
        return listener?.let(mShow::get) as? Boolean
    }

    fun getFloatingImeBottomInset(listener: Any?): Int? {
        return listener?.let(mFloatingImeBottomInset::get) as? Int
    }
}