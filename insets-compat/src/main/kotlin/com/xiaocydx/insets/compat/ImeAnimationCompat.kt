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
import android.graphics.Insets
import android.os.Build
import android.util.Log
import android.view.*
import android.view.animation.Interpolator
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type.ime
import com.xiaocydx.insets.setWindowInsetsAnimationCallbackCompat

/**
 * 修改Android 11及以上IME动画的`durationMillis`和`interpolator`
 *
 * ### 主要作用
 * 该函数的主要作用是结合实际场景需求，微调IME动画的属性，让Android 11及以上的设备有更好的交互体验，
 * 不支持修改Android 11以下IME动画的属性，原因是Android 11以下无法跟IME完全贴合，保持兼容代码即可。
 *
 * ### 修改失败
 * 当修改IME动画的属性失败时，会在[WindowInsetsAnimationCompat.Callback.onPrepare]之后，
 * [WindowInsetsAnimationCompat.Callback.onStart]之前恢复`durationMillis`和`interpolator`，
 * 修改成功或修改失败，都会按[TAG]打印日志。
 *
 * ### 兼容场景
 * 当[WindowInsetsAnimationCompat]的初始`durationMillis <= 0`时，不修改`durationMillis`和`interpolator`，
 * 目的是兼容[WindowInsetsControllerCompat.controlWindowInsetsAnimation]的`durationMillis <= 0`的场景，
 * 例如通过[WindowInsetsAnimationControlListenerCompat.onReady]获取[WindowInsetsAnimationControllerCompat]，
 * 调用[WindowInsetsAnimationControllerCompat.setInsetsAndAlpha]实现手势拖动显示IME。
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
    if (!supportModifyImeAnimation) return
    imeAnimationCompat?.detach()
    val compat = ImeAnimationCompat(this, durationMillis, interpolator)
    imeAnimationCompat = compat.apply { attach() }
}

/**
 * 恢复[modifyImeAnimation]修改的`durationMillis`和`interpolator`
 */
fun Window.restoreImeAnimation() {
    if (!supportModifyImeAnimation) return
    imeAnimationCompat?.detach()
    imeAnimationCompat = null
}

/**
 * 对`window.decorView`设置[WindowInsetsAnimationCompat.Callback]，
 * 该函数能避免跟[modifyImeAnimation]的实现产生冲突，实际效果等同于：
 * ```
 * ViewCompat.setWindowInsetsAnimationCallback(window.decorView, callback)
 * ```
 */
fun Window.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    if (!supportModifyImeAnimation) {
        return decorView.setWindowInsetsAnimationCallbackCompat(callback)
    }
    val proxyCallback = if (callback == null) null else {
        InsetsAnimationReflection.insetsAnimationCompat?.createProxyCallback(callback)
    }
    insetsAnimationCallback = proxyCallback
    imeAnimationCompat?.reattach() ?: run {
        decorView.setWindowInsetsAnimationCallback(insetsAnimationCallback)
    }
}

@ChecksSdkIntAtLeast(api = 30)
private val supportModifyImeAnimation = Build.VERSION.SDK_INT >= 30

@get:RequiresApi(30)
private var Window.imeAnimationCompat: ImeAnimationCompat?
    get() = decorView.getTag(R.id.tag_decor_view_ime_animation_compat) as? ImeAnimationCompat
    set(value) {
        decorView.setTag(R.id.tag_decor_view_ime_animation_compat, value)
    }

@get:RequiresApi(30)
private var Window.insetsAnimationCallback: WindowInsetsAnimation.Callback?
    get() = decorView.getTag(R.id.tag_decor_view_insets_animation_callback) as? WindowInsetsAnimation.Callback
    set(value) {
        decorView.setTag(R.id.tag_decor_view_insets_animation_callback, value)
    }

@RequiresApi(30)
private class ImeAnimationCompat(
    window: Window,
    private val durationMillis: Long,
    private val interpolator: Interpolator?,
) : WindowAttacher(window) {
    private var callback: ImeAnimationCallback? = null

    override fun onAttach() {
        onDetach()
        // onAttach()确保insetsController是InsetsController，而不是PendingInsetsController
        val controller = requireNotNull(window.insetsController) { "InsetsController为null" }
        callback = ImeAnimationCallback(durationMillis, interpolator, controller, window.insetsAnimationCallback)
        // 对视图树的根View（排除ViewRootImpl）decorView设置imeAnimationCallback，
        // 目的是避免WindowInsetsAnimationCompat.Callback的分发逻辑产生歧义，例如：
        // ViewCompat.setWindowInsetsAnimationCallback(decorView, callback)
        // ViewCompat.setWindowInsetsAnimationCallback(childView, imeAnimationCallback)
        // 1. childView是decorView的间接子View。
        // 2. callback的dispatchMode是DISPATCH_MODE_CONTINUE_ON_SUBTREE。
        // 若对childView设置imeAnimationCallback，则imeAnimationCallback修改IME动画的属性，
        // 会影响到decorView的callback函数，callback.onPrepare()获取的是IME动画原本的属性，
        // 而callback.onStart()获取的却是IME动画修改后的属性，分发逻辑产生歧义。
        decorView.setWindowInsetsAnimationCallback(callback)
        decorView.viewTreeObserver.addOnDrawListener(callback)
    }

    override fun onDetach() {
        decorView.setWindowInsetsAnimationCallback(window.insetsAnimationCallback)
        callback?.let(decorView.viewTreeObserver::removeOnDrawListener)
        callback = null
    }
}

/**
 * 修改Android 11及以上IME动画的`durationMillis`和`interpolator`
 *
 * `InsetsController.show()`或`InsetsController.hide()`的执行顺序：
 * 1. 构建`InsetsController.InternalAnimationControlListener`。
 *
 * 2. 构建`InsetsController.InsetsAnimationControlImpl`（因为设置了WindowInsetsAnimationCompat.Callback）。
 *
 * 3. `InsetsController.InsetsAnimationControlImpl`的构造函数调用`InsetsController.startAnimation()`。
 *
 * 4. `InsetsController.startAnimation()`调用至`WindowInsetsAnimationCompat.Callback.onPrepare()`，
 * 然后添加`doOnPreDraw()`，在下一帧调用至`WindowInsetsAnimationCompat.Callback.onStart()`。
 *
 * 5. `InsetsController.mRunningAnimations.add()`添加`RunningAnimation`，
 * `RunningAnimation`包含第2步构建的`InsetsController.InsetsAnimationControlImpl`.
 *
 * 6. 下一帧`doOnPreDraw()`执行，调用至`WindowInsetsAnimationCompat.Callback.onStart()`，
 * 然后调用`InsetsController.InternalAnimationControlListener.onReady()`构建属性动画。
 *
 * [onPrepare]修改第4步的[WindowInsetsAnimation]，[onStart]修改第6步构建的属性动画。
 */
@RequiresApi(30)
private class ImeAnimationCallback(
    val durationMillis: Long,
    val interpolator: Interpolator?,
    private val controller: WindowInsetsController,
    private val delegate: WindowInsetsAnimation.Callback?,
    dispatchMode: Int = delegate?.dispatchMode ?: DISPATCH_MODE_CONTINUE_ON_SUBTREE
) : WindowInsetsAnimation.Callback(dispatchMode), ViewTreeObserver.OnDrawListener {
    private val trackers = mutableMapOf<WindowInsetsAnimation, Tracker>()

    /**
     * 对视图树分发[animation]之前，修改[animation]的属性值
     */
    // TODO: 记录全局失败，不再做任何修改
    override fun onPrepare(animation: WindowInsetsAnimation) {
        val tracker = getTracker(animation)
        if (tracker.step === Step.ON_PREPARE) {
            InsetsAnimationReflection.insetsAnimation?.apply {
                tracker.trackInterpolator { animation.setInterpolator(it) }
                tracker.trackDurationMillis { animation.setDurationMillis(it) }
            }
            tracker.checkOnPrepare()
        }
        delegate?.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimation,
        bounds: WindowInsetsAnimation.Bounds
    ): WindowInsetsAnimation.Bounds {
        return delegate?.onStart(animation, bounds) ?: bounds
    }

    override fun onDraw() = with(InsetsAnimationReflection) {
        if (trackers.isEmpty()) return
        val runningAnimations = insetsController?.getRunningAnimations(controller)
        if (runningAnimations.isNullOrEmpty()) return
        trackers.forEach action@{ (animation, tracker) ->
            if (tracker.step !== Step.ON_START) return@action

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

            val controller = animationControlListener?.getController(listener)
            val animator = animationControlListener?.getAnimator(listener)
            val show = animationControlListener?.getShow(listener)
            val floatingImeBottomInset = animationControlListener?.getFloatingImeBottomInset(listener)
            if (controller != null && animator != null && show != null
                    && hasZeroInsetsIme != null && floatingImeBottomInset != null) {
                tracker.trackInterpolator { interpolator ->
                    var hiddenInsets = controller.hiddenStateInsets
                    if (hasZeroInsetsIme) {
                        hiddenInsets = Insets.of(
                            hiddenInsets.left, hiddenInsets.top,
                            hiddenInsets.right, floatingImeBottomInset
                        )
                    }
                    val start = if (show) hiddenInsets else controller.shownStateInsets
                    val end = if (show) controller.shownStateInsets else hiddenInsets
                    animator.addUpdateListener {
                        val rawFraction = it.animatedFraction
                        val insetsFraction = interpolator.getInterpolation(rawFraction)
                        val insets = insetsEvaluator.evaluate(insetsFraction, start, end)
                        controller.setInsetsAndAlpha(insets, 1f, rawFraction)
                    }
                    true
                }
                tracker.trackDurationMillis {
                    animator.duration = it
                    true
                }
            }
            tracker.checkOnStart()
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
        trackers.remove(animation)
        delegate?.onEnd(animation)
    }

    private fun getTracker(animation: WindowInsetsAnimation): Tracker {
        var tracker: Tracker? = trackers[animation]
        if (tracker == null) {
            tracker = Tracker(animation, durationMillis, interpolator)
            trackers[animation] = tracker
        }
        return tracker
    }

    private class Tracker(
        animation: WindowInsetsAnimation,
        private val durationMillis: Long,
        private val interpolator: Interpolator?
    ) {
        private var interpolatorOutcome = false
        private var durationMillisOutcome = false
        var step: Step = Step.COMPLETED
            private set

        init {
            if (!InsetsAnimationReflection.reflectSucceed) {
                log("InsetsAnimationReflection反射失败，不做修改")
            } else if (animation.typeMask and ime() == 0) {
                log("WindowInsetsAnimationCompat不包含IME类型，不做修改")
            } else if (animation.durationMillis <= 0) {
                log("兼容animation.durationMillis <= 0的场景，不做修改")
            } else {
                step = Step.ON_PREPARE
            }
        }

        inline fun trackInterpolator(modify: (Interpolator) -> Boolean) {
            interpolatorOutcome = if (interpolator == null) true else modify(interpolator)
        }

        inline fun trackDurationMillis(modify: (Long) -> Boolean) {
            durationMillisOutcome = if (durationMillis <= NO_VALUE) true else modify(durationMillis)
        }

        fun checkOnPrepare() {
            if (step !== Step.ON_PREPARE) return
            val succeed = consumeOutcome()
            if (succeed) step = Step.ON_START
            val outcome = if (succeed) "成功" else "失败"
            log("onPrepare()修改WindowInsetsAnimation$outcome")
        }

        fun checkOnStart() {
            if (step !== Step.ON_START) return
            val succeed = consumeOutcome()
            step = Step.COMPLETED
            val outcome = if (succeed) "成功" else "失败"
            log("onStart()修改InternalAnimationControlListener$outcome")
        }

        private fun consumeOutcome(): Boolean {
            val outcome = interpolatorOutcome && durationMillisOutcome
            interpolatorOutcome = false
            durationMillisOutcome = false
            return outcome
        }
    }

    private enum class Step {
        ON_PREPARE, ON_START, COMPLETED
    }

    private companion object {
        private val insetsEvaluator = TypeEvaluator<Insets> { fraction, start, end ->
            Insets.of(
                (start.left + fraction * (end.left - start.left)).toInt(),
                (start.top + fraction * (end.top - start.top)).toInt(),
                (start.right + fraction * (end.right - start.right)).toInt(),
                (start.bottom + fraction * (end.bottom - start.bottom)).toInt()
            )
        }
    }
}

private const val NO_VALUE = -1L

private const val TAG = "ImeAnimationCompat"

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