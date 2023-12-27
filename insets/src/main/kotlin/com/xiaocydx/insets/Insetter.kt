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
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

/**
 * 通过[WindowInsetsCompat.getInsets]获取statusBars类型的Insets
 */
fun WindowInsetsCompat.statusBars() = getInsets(WindowInsetsCompat.Type.statusBars())

/**
 * 通过[WindowInsetsCompat.getInsets]获取navigationBars类型的Insets
 */
fun WindowInsetsCompat.navigationBars() = getInsets(WindowInsetsCompat.Type.navigationBars())

/**
 * 通过[WindowInsetsCompat.getInsets]获取ime类型的Insets
 */
fun WindowInsetsCompat.ime() = getInsets(WindowInsetsCompat.Type.ime())

/**
 * 消费指定[InsetsType]类型集的Insets
 *
 * ```
 * val typeMask = WindowInsetsCompat.Type.statusBars()
 * val outcome = insets.consume(typeMask)
 * outcome.getInsets(typeMask) // Insets.NONE
 * outcome.getInsetsIgnoringVisibility(typeMask) // Insets.NONE
 * outcome.isVisible(typeMask) // 不改变可见结果
 * ```
 */
@CheckResult
fun WindowInsetsCompat.consume(@InsetsType typeMask: Int): WindowInsetsCompat {
    if (typeMask <= 0) return this
    val builder = WindowInsetsCompat.Builder(this)
    if (typeMask != WindowInsetsCompat.Type.ime()) {
        // typeMask等于IME会抛出IllegalArgumentException
        builder.setInsetsIgnoringVisibility(typeMask, Insets.NONE)
    }
    return builder.setInsets(typeMask, Insets.NONE).build()
}

/**
 * 是否为手势导航栏
 *
 * **注意**：若导航栏被隐藏，则该函数返回`true`，此时导航栏高度为0，
 * 实际场景可以将隐藏的导航栏，当作手势导航栏来处理，一般不会有问题。
 */
fun WindowInsetsCompat.isGestureNavigationBar(view: View): Boolean {
    val threshold = (24 * view.resources.displayMetrics.density).toInt()
    return navigationBars().bottom <= threshold.coerceAtLeast(66)
}

/**
 * [WindowInsetsAnimationCompat.getTypeMask]是否包含指定[InsetsType]类型集的Insets
 */
fun WindowInsetsAnimationCompat.contains(@InsetsType typeMask: Int) = this.typeMask and typeMask == typeMask

/**
 * 当分发到[WindowInsetsCompat]时，调用[block]
 */
fun View.doOnApplyWindowInsets(block: (view: View, insets: WindowInsetsCompat, initialState: ViewState) -> Unit) {
    val initialState = recordCurrentState()
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
    val navigationBarHeight = insets.navigationBars().bottom
    val isGestureNavigationBar = insets.isGestureNavigationBar(this)
    // 1. 若当前是手势导航栏，则增加高度，否则保持初始高度
    val height = when {
        initialState.params.height < 0 -> initialState.params.height
        !isGestureNavigationBar -> initialState.params.height
        else -> navigationBarHeight + initialState.params.height
    }
    if (layoutParams.height != height) updateLayoutParams { this.height = height }
    // 2. 若当前是手势导航栏，则增加paddingBottom，否则保持初始paddingBottom
    updatePadding(bottom = when {
        !isGestureNavigationBar -> initialState.paddings.bottom
        else -> navigationBarHeight + initialState.paddings.bottom
    })
    // 3. 手势导航栏EdgeToEdge会增加paddingBottom，将clipToPadding设为false，
    // 使得滚动容器在滚动时，能将内容绘制在paddingBottom区域，当滚动到底部时，
    // 留出paddingBottom区域，内容不会被手势导航栏遮挡。
    (this as? ViewGroup)?.takeIf { it.isScrollContainer }?.clipToPadding = !isGestureNavigationBar
}