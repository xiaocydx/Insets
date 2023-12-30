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

@file:SuppressLint("ObsoleteSdkInt")

package com.xiaocydx.insets.compat

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.xiaocydx.insets.compat.InsetsCompatReflection.getLastInsetsFromProxyListener
import com.xiaocydx.insets.compat.InsetsCompatReflection.setStableInsets
import com.xiaocydx.insets.compat.InsetsCompatReflection.setupImmutableListener
import com.xiaocydx.insets.onApplyWindowInsetsCompat
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat
import com.xiaocydx.insets.setWindowInsetsAnimationCallbackCompat

/**
 * 兼容Android 9.0以下的[WindowInsets]可变
 *
 * [ViewCompat.setOnApplyWindowInsetsListener]最后是设置匿名[View.OnApplyWindowInsetsListener]，
 * 该函数确保对匿名[View.OnApplyWindowInsetsListener.onApplyWindowInsets]传入不可变的[WindowInsets]。
 *
 * 匿名[View.OnApplyWindowInsetsListener]的分发优先级：
 * 1. `Impl21OnApplyWindowInsetsListener.onApplyWindowInsets()`。
 * 2. `listener.onApplyWindowInsets()`。
 *
 * 这种分发优先级，决定了无法通过以下方式实现兼容：
 * ```
 * ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
 *     val immutable = insets.toWindowInsets().toImmutable()
 *     val immutableCompat = WindowInsetsCompat.toWindowInsetsCompat(immutable, view)
 *     listener.onApplyWindowInsets(view, immutableCompat)
 * }
 * ```
 */
fun View.setOnApplyWindowInsetsListenerImmutable(
    listener: OnApplyWindowInsetsListener? = defaultOnApplyWindowInsetsListener
) {
    setOnApplyWindowInsetsListenerCompat(listener)
    if (!isImmutableNeeded || !InsetsCompatReflection.reflectSucceed) return
    // 确保对匿名View.OnApplyWindowInsetsListener分发不可变的WindowInsets
    immutableListener = listener?.run { setupImmutableListener() }
}

/**
 * 兼容Android 9.0以下的[WindowInsets]可变
 *
 * Android 11以下，[ViewCompat.setWindowInsetsAnimationCallback]最后是设置`Impl21OnApplyWindowInsetsListener`，
 * 该函数确保`Impl21OnApplyWindowInsetsListener`构造函数创建的`mLastInsets`生成缓存，避免后续读取到改变后的值。
 * 并确保对`Impl21OnApplyWindowInsetsListener.onApplyWindowInsets()`传入不可变的[WindowInsets]。
 */
fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?) {
    setWindowInsetsAnimationCallbackCompat(callback)
    if (!isImmutableNeeded || !InsetsCompatReflection.reflectSucceed) return
    if (isAttachedToWindow) {
        // 确保Impl21OnApplyWindowInsetsListener构造函数创建的mLastInsets生成缓存
        getLastInsetsFromProxyListener()?.ensureCreateCache()
    }
    if (immutableListener == null) {
        // 确保对Impl21OnApplyWindowInsetsListener分发不可变的WindowInsets
        setOnApplyWindowInsetsListenerImmutable()
    }
}

/**
 * Android 9.0及以上，[WindowInsets]不可变，不需要兼容
 */
@ChecksSdkIntAtLeast(api = 21)
private val isImmutableNeeded = Build.VERSION.SDK_INT in 21 until 28

@Suppress("DEPRECATION")
private fun WindowInsets.toImmutable(): WindowInsets {
    if (!isImmutableNeeded || !InsetsCompatReflection.reflectSucceed) return this
    // 先创建新的WindowInsets，再反射修改mStableInsets，避免对当前WindowInsets造成影响
    val insets = replaceSystemWindowInsets(
        systemWindowInsetLeft, systemWindowInsetTop,
        systemWindowInsetRight, systemWindowInsetBottom
    )
    // WindowInsetsCompat没有兼容WindowInsets.mWindowDecorInsets的函数，因此不需要修改
    insets.setStableInsets(stableInsetLeft, stableInsetTop, stableInsetRight, stableInsetBottom)
    return insets
}

@Suppress("DEPRECATION")
private fun WindowInsetsCompat.ensureCreateCache() {
    if (!isImmutableNeeded) return
    // 访问一次stableInsets和systemWindowInsets，即可生成缓存，
    // 避免后续因为内部WindowInsets数据改变，而读取到改变后的值。
    apply { stableInsets }.apply { systemWindowInsets }
}

@RequiresApi(21)
private val defaultOnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { v, insets -> v.onApplyWindowInsetsCompat(insets) }

@get:RequiresApi(21)
private var View.immutableListener: WindowInsetsImmutableListener?
    get() = getTag(R.id.tag_window_insets_immutable_listener) as? WindowInsetsImmutableListener
    set(value) {
        setTag(R.id.tag_window_insets_immutable_listener, value)
    }

@RequiresApi(21)
private class WindowInsetsImmutableListener(
    private val delegate: View.OnApplyWindowInsetsListener
) : View.OnApplyWindowInsetsListener {

    override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        val applyInsets = if (view.hasParentListener()) insets else insets.toImmutable()
        return delegate.onApplyWindowInsets(view, applyInsets)
    }

    private fun View.hasParentListener(): Boolean {
        var parent: View? = parent as? View
        var found: WindowInsetsImmutableListener? = parent?.immutableListener
        while (found == null && parent is View) {
            found = parent.immutableListener
            parent = parent.parent as? View
        }
        return found != null
    }
}

@RequiresApi(21)
@SuppressLint("PrivateApi")
private object InsetsCompatReflection : Reflection {
    private var mListenerInfoField: FieldCache? = null
    private var mOnApplyWindowInsetsListenerField: FieldCache? = null
    private var mStableInsetsField: FieldCache? = null
    private var mLastInsetsField: FieldCache? = null
    private var proxyListenerClass: Class<*>? = null
    var reflectSucceed: Boolean = false; private set

    init {
        runCatching {
            val viewClass = View::class.java
            val listenerInfoClass = Class.forName("android.view.View\$ListenerInfo")
            val windowInsetsClass = WindowInsets::class.java
            val proxyListenerClass = Class.forName("androidx.core.view." +
                    "WindowInsetsAnimationCompat\$Impl21\$Impl21OnApplyWindowInsetsListener")
            val viewFields = viewClass.declaredInstanceFields
            mListenerInfoField = viewFields.find("mListenerInfo").toCache()
            mOnApplyWindowInsetsListenerField = listenerInfoClass
                .declaredInstanceFields.find("mOnApplyWindowInsetsListener").toCache()
            mStableInsetsField = windowInsetsClass
                .declaredInstanceFields.find("mStableInsets").toCache()
            mLastInsetsField = proxyListenerClass
                .declaredInstanceFields.find("mLastInsets").toCache()
            InsetsCompatReflection.proxyListenerClass = proxyListenerClass
            reflectSucceed = true
        }.onFailure {
            mListenerInfoField = null
            mOnApplyWindowInsetsListenerField = null
            mStableInsetsField = null
            proxyListenerClass = null
        }
    }

    /**
     * ```
     * public class View implements Drawable.Callback, KeyEvent.Callback,
     *         AccessibilityEventSource {
     *
     *     static class ListenerInfo {
     *         OnApplyWindowInsetsListener mOnApplyWindowInsetsListener;
     *     }
     * }
     * ```
     */
    fun View.setupImmutableListener(): WindowInsetsImmutableListener? {
        val mListenerInfo = mListenerInfoField?.get(this) ?: return null
        val delegate = mOnApplyWindowInsetsListenerField?.get(mListenerInfo)
                as? View.OnApplyWindowInsetsListener ?: return null
        val listener = WindowInsetsImmutableListener(delegate)
        mOnApplyWindowInsetsListenerField?.set(mListenerInfo, listener)
        return listener
    }

    /**
     * ```
     * private static class Impl21OnApplyWindowInsetsListener implements
     *         View.OnApplyWindowInsetsListener {
     *     private WindowInsetsCompat mLastInsets;
     * }
     * ```
     */
    fun View.getLastInsetsFromProxyListener(): WindowInsetsCompat? {
        val proxyListener = getTag(androidx.core.R.id.tag_window_insets_animation_callback)
        return proxyListener?.let { mLastInsetsField?.get(it) as? WindowInsetsCompat }
    }

    /**
     * [WindowInsets]没有提供`replaceStableInsets()`之类的函数，因此反射修改`mStableInsets`
     *
     * ```
     * public final class WindowInsets {
     *     private Rect mStableInsets;
     * }
     * ```
     */
    fun WindowInsets.setStableInsets(
        stableInsetLeft: Int, stableInsetTop: Int,
        stableInsetRight: Int, stableInsetBottom: Int
    ) {
        mStableInsetsField?.set(this, Rect(
            stableInsetLeft, stableInsetTop,
            stableInsetRight, stableInsetBottom
        ))
    }
}