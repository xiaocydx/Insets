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

import android.graphics.Rect
import android.os.Build.VERSION
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars

/**
 * 状态栏高度
 */
val WindowInsetsCompat.statusBarHeight: Int
    get() = getInsets(statusBars()).top

/**
 * 导航栏高度
 */
val WindowInsetsCompat.navigationBarHeight: Int
    get() = getInsets(navigationBars()).bottom

/**
 * IME高度
 *
 * **注意**：IME高度包含导航栏高度。
 */
val WindowInsetsCompat.imeHeight: Int
    get() = getInsets(ime()).bottom

/**
 * 是否为手势导航栏
 *
 * **注意**：若导航栏被隐藏，则该函数返回`true`，此时导航栏高度为0，
 * 实际场景可以将隐藏的导航栏，当作手势导航栏来处理，通常不会有问题。
 */
fun WindowInsetsCompat.isGestureNavigationBar(view: View): Boolean {
    val threshold = (24 * view.resources.displayMetrics.density).toInt()
    return navigationBarHeight <= threshold.coerceAtLeast(66)
}

/**
 * 获取`contentView`的IME偏移，可用于设置`contentView`间距的场景
 */
fun WindowInsetsCompat.getImeOffset(view: View): Int {
    val imeHeight = imeHeight.takeIf { it > 0 } ?: return 0
    var navigationBarHeight = navigationBarHeight
    if (navigationBarHeight <= 0) {
        // 父View可能消费了导航栏Insets，尝试通过rootInsets获取导航栏高度
        val rootInsets = view.getRootWindowInsetsCompat()
        navigationBarHeight = rootInsets?.navigationBarHeight ?: 0
    }
    return (imeHeight - navigationBarHeight).coerceAtLeast(0)
}

/**
 * 消费指定类型集的Insets
 *
 * ```
 * val typeMask = WindowInsetsCompat.Type.statusBars()
 * val outcome = insets.consumeInsets(typeMask)
 * outcome.getInsets(typeMask) // 返回Insets.NONE
 * outcome.getInsetsIgnoringVisibility(typeMask) // 返回Insets.NONE
 * outcome.isVisible(typeMask) // 不改变isVisible结果
 * ```
 */
@CheckResult
@Suppress("DEPRECATION")
fun WindowInsetsCompat.consumeInsets(@InsetsType typeMask: Int): WindowInsetsCompat {
    if (typeMask == 0) return this
    var builder = WindowInsetsCompat.Builder(this)
    builder.setInsets(typeMask, Insets.NONE)
    if (typeMask != ime()) {
        // 当typeMask等于ime()时，会抛出IllegalArgumentException
        builder.setInsetsIgnoringVisibility(typeMask, Insets.NONE)
    }
    if (VERSION.SDK_INT < 30) {
        // Android 11以下需要修正stableInsets，getInsetsIgnoringVisibility()才返回Insets.NONE
        builder.setStableInsets(getInsets(systemBars() and typeMask.inv()))
        val systemWindowInsets = getInsets(systemBars() or ime() and typeMask.inv())
        if (!systemWindowInsets.isEmpty) {
            // Android 11以下的systemWindowInsets包含ime，但在调用builder.setInsets()后，
            // builder.build()会替换systemWindowInsets，只剩下statusBars和navigationBars，
            // 这会导致判断systemWindowInsets的代码出现异常，因此需要修正systemWindowInsets。
            builder = WindowInsetsCompat.Builder(builder.build())
            builder.setSystemWindowInsets(systemWindowInsets)
        }
    }
    return builder.build()
}

/**
 * [WindowInsetsAnimationCompat.getTypeMask]是否包含指定类型集的Insets
 */
fun WindowInsetsAnimationCompat.contains(@InsetsType typeMask: Int) = this.typeMask and typeMask == typeMask

val Insets.isEmpty: Boolean
    get() = left == 0 && top == 0 && right == 0 && bottom == 0

fun Insets.toRect() = run { Rect(left, top, right, bottom) }

/**
 * 当分发到[WindowInsetsCompat]时，调用[block]
 */
fun View.doOnApplyWindowInsets(block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit) {
    val initialState = ViewState(this)
    setOnApplyWindowInsetsListenerCompat { view, insets ->
        block(view, insets, initialState)
        insets
    }
    // 当view首次或再次附加到window时，可能错过WindowInsets分发,
    // 因此主动申请WindowInsets分发，确保调用block完成视图初始化。
    requestApplyInsetsOnAttach()
}

/**
 * 当附加到window时，申请[WindowInsets]分发
 */
fun View.requestApplyInsetsOnAttach() {
    if (isAttachedToWindow) requestApplyInsetsCompat()
    removeRequestApplyInsetsOnAttach()
    val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            view.requestApplyInsetsCompat()
        }

        override fun onViewDetachedFromWindow(view: View) = Unit
    }
    setTag(R.id.tag_view_request_apply_insets, listener)
    addOnAttachStateChangeListener(listener)
}

/**
 * 移除[requestApplyInsetsOnAttach]的设置
 */
fun View.removeRequestApplyInsetsOnAttach() {
    getTag(R.id.tag_view_request_apply_insets)
        ?.let { it as? View.OnAttachStateChangeListener }
        ?.let(::removeOnAttachStateChangeListener)
}

/**
 * 当分发到[WindowInsetsCompat]时，调用[handleGestureNavBarEdgeToEdge]
 */
fun View.handleGestureNavBarEdgeToEdgeOnApply() {
    doOnApplyWindowInsets { view, insets, initialState ->
        view.handleGestureNavBarEdgeToEdge(insets, initialState)
    }
}

/**
 * 通用的手势导航栏EdgeToEdge处理逻辑，可以跟[doOnApplyWindowInsets]结合使用
 */
fun View.handleGestureNavBarEdgeToEdge(insets: WindowInsetsCompat, initialState: ViewState) {
    val navigationBarHeight = insets.navigationBarHeight
    val isGestureNavigationBar = insets.isGestureNavigationBar(this)
    // 1. 若当前是手势导航栏，则增加高度，否则保持初始高度
    val height = when {
        initialState.params.height < 0 -> initialState.params.height
        !isGestureNavigationBar -> initialState.params.height
        else -> initialState.params.height + navigationBarHeight
    }
    if (layoutParams.height != height) updateLayoutParams { this.height = height }
    // 2. 若当前是手势导航栏，则增加paddingBottom，否则保持初始paddingBottom
    updatePadding(bottom = when {
        !isGestureNavigationBar -> initialState.paddings.bottom
        else -> initialState.paddings.bottom + navigationBarHeight
    })
    // 3. 手势导航栏EdgeToEdge会增加paddingBottom，将clipToPadding设为false，
    // 使得滚动容器在滚动时，能将内容绘制在paddingBottom区域，当滚动到底部时，
    // 留出paddingBottom区域，内容不会被手势导航栏遮挡。
    (this as? ViewGroup)?.takeIf { it.isScrollContainer }?.clipToPadding = !isGestureNavigationBar
}