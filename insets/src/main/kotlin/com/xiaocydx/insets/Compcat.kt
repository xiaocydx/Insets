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

package com.xiaocydx.insets

import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.view.animation.Interpolator
import android.widget.EditText
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

/**
 * 禁用`window.decorView`实现的消费逻辑和间距逻辑，让视图树自行处理[WindowInsets]
 */
fun Window.disableDecorFitsSystemWindows() {
    disableDecorFitsSystemWindowsInternal(consumeTypeMask = 0)
}

fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
}

/**
 * Android 11以下，`WindowInsetsControllerCompat.show(ime())`需要[view]为[EditText]
 */
fun Window.getInsetsControllerCompat(view: View) = WindowCompat.getInsetsController(this, view)

fun View.getRootWindowInsetsCompat() = ViewCompat.getRootWindowInsets(this)

fun View.requestApplyInsetsCompat() = ViewCompat.requestApplyInsets(this)

fun View.dispatchApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.dispatchApplyWindowInsets(this, insets)

fun View.onApplyWindowInsetsCompat(insets: WindowInsetsCompat) =
        ViewCompat.onApplyWindowInsets(this, insets)

fun View.setOnApplyWindowInsetsListenerCompat(listener: OnApplyWindowInsetsListener?) {
    ViewCompat.setOnApplyWindowInsetsListener(this, listener)
}

fun View.setWindowInsetsAnimationCallbackCompat(callback: WindowInsetsAnimationCompat.Callback?) {
    ViewCompat.setWindowInsetsAnimationCallback(this, callback)
}

/**
 * 传入[view]是为了确保转换出的[WindowInsetsCompat]是正确的结果
 */
fun WindowInsets.toWindowInsetsCompat(view: View) = WindowInsetsCompat.toWindowInsetsCompat(this, view)

internal fun Window.disableDecorFitsSystemWindowsInternal(
    @InsetsType consumeTypeMask: Int
) = ReflectCompat {
    setDecorFitsSystemWindowsCompat(false)
    @Suppress("DEPRECATION")
    setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
    decorView.setOnApplyWindowInsetsListenerImmutable { _, insets ->
        checkDispatchApplyInsetsCompatibility()
        decorView.onApplyWindowInsetsCompat(insets.consumeInsets(consumeTypeMask))
        insets
    }
}

private fun Window.checkDispatchApplyInsetsCompatibility() {
    check(!isFloating) {
        """window.isFloating = true
           |    需要主题的windowIsFloating = false，否则会导致视图树没有WindowInsets分发
        """.trimMargin()
    }
    @Suppress("DEPRECATION")
    check(attributes.softInputMode and SOFT_INPUT_ADJUST_RESIZE != 0) {
        """window.attributes.softInputMode未包含SOFT_INPUT_ADJUST_RESIZE
           |    需要window.attributes.softInputMode包含SOFT_INPUT_ADJUST_RESIZE，
           |    否则会导致Android 11以下显示或隐藏IME不进行WindowInsets分发，
           |    可以调用Window.setSoftInputMode()设置SOFT_INPUT_ADJUST_RESIZE
        """.trimMargin()
    }
}

@Suppress("FunctionName")
internal inline fun <R> ReflectCompat(block: ReflectCompat.() -> R): R = with(reflectCompat, block)

internal val reflectCompat: ReflectCompat = try {
    val className = "com.xiaocydx.insets.compat.ReflectCompatImpl"
    val clazz = Class.forName(className, false, ReflectCompat::class.java.classLoader)
    clazz.asSubclass(ReflectCompat::class.java).newInstance()
} catch (e: Throwable) {
    NotReflectCompat
}

internal interface ReflectCompat {
    val Window.isFullscreenCompatEnabled: Boolean

    fun Window.modifyImeAnimation(durationMillis: Long, interpolator: Interpolator)

    fun Window.restoreImeAnimation()

    fun View.setOnApplyWindowInsetsListenerImmutable(listener: OnApplyWindowInsetsListener?)

    fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?)
}

private object NotReflectCompat : ReflectCompat {
    override val Window.isFullscreenCompatEnabled: Boolean
        get() = false

    override fun Window.modifyImeAnimation(durationMillis: Long, interpolator: Interpolator) = Unit

    override fun Window.restoreImeAnimation() = Unit

    override fun View.setOnApplyWindowInsetsListenerImmutable(listener: OnApplyWindowInsetsListener?) {
        setOnApplyWindowInsetsListenerCompat(listener)
    }

    override fun View.setWindowInsetsAnimationCallbackImmutable(callback: WindowInsetsAnimationCompat.Callback?) {
        setWindowInsetsAnimationCallbackCompat(callback)
    }
}