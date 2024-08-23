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
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 当前[WindowInsetsCompat]的[statusBars]高度
 */
val WindowInsetsCompat.statusBarHeight: Int
    get() = getInsets(statusBars()).top

/**
 * 当前[WindowInsetsCompat]的[navigationBars]高度
 */
val WindowInsetsCompat.navigationBarHeight: Int
    get() = getInsets(navigationBars()).bottom

/**
 * 当前[WindowInsetsCompat]的[ime]高度
 *
 * **注意**：[ime]高度包含[navigationBars]高度。
 */
val WindowInsetsCompat.imeHeight: Int
    get() = getInsets(ime()).bottom

/**
 * 当前[WindowInsetsCompat]的[statusBars]是否被隐藏
 *
 * 该函数可用于判断[WindowInsetsControllerCompat.hide]的隐藏结果：
 * ```
 * WindowInsetsControllerCompat(window, view).hide(statusBars())
 * view.doOnApplyWindowInsets { _, insets, initialState ->
 *     val hidden = insets.isStatusBarHidden(view) // 返回false
 * }
 * ```
 */
fun WindowInsetsCompat.isStatusBarHidden(view: View): Boolean {
    return InsetsInspector(this).isStatusBarHidden(view)
}

/**
 * 当前[WindowInsetsCompat]的[navigationBars]是否被隐藏
 *
 * 该函数可用于判断[WindowInsetsControllerCompat.hide]的隐藏结果：
 * ```
 * WindowInsetsControllerCompat(window, view).hide(navigationBars())
 * view.doOnApplyWindowInsets { _, insets, initialState ->
 *     val hidden = insets.isNavigationBarHidden(view) // 返回false
 * }
 * ```
 */
fun WindowInsetsCompat.isNavigationBarHidden(view: View): Boolean {
    return InsetsInspector(this).isNavigationBarHidden(view)
}

/**
 * 当前[WindowInsetsCompat]的[navigationBars]是否为手势导航栏
 *
 * ```
 * view.doOnApplyWindowInsets { _, insets, initialState ->
 *     val isGesture = insets.isGestureNavigationBar(view)
 * }
 * ```
 */
fun WindowInsetsCompat.isGestureNavigationBar(view: View): Boolean {
    return InsetsInspector(this).isGestureNavigationBar(view)
}

/**
 * 获取当前[WindowInsetsCompat]的[systemBars]被隐藏的消费类型集
 */
@InsetsType
fun WindowInsetsCompat.getSystemBarHiddenConsumeTypeMask(view: View): Int {
    return InsetsInspector(this).getSystemBarHiddenConsumeTypeMask(view)
}

/**
 * 获取当前[WindowInsetsCompat]的[ime]偏移，可作为`contentView`的间距
 *
 * ```
 * contentView.doOnApplyWindowInsets { _, insets, initialState ->
 *     contentView.updatePadding(bottom = insets.getImeOffset(contentView))
 * }
 * ```
 */
fun WindowInsetsCompat.getImeOffset(view: View): Int {
    val imeHeight = imeHeight.takeIf { it > 0 } ?: return 0
    val rootInsets = view.getRootWindowInsetsCompat()
    return when {
        rootInsets == null -> imeHeight
        rootInsets.isNavigationBarHidden(view) -> imeHeight
        // 父级View未消费导航栏的Insets，可能是EdgeToEdge场景
        rootInsets.navigationBarHeight == navigationBarHeight -> imeHeight
        // 父级View已消费导航栏的Insets，可能不是EdgeToEdge场景
        else -> (imeHeight - rootInsets.navigationBarHeight).coerceAtLeast(0)
    }
}

/**
 * 消费指定类型集的Insets，消费结果可作为`DecorView.onApplyWindowInsets()`的入参
 *
 * **注意**：消费结果不能作为`DecorView.onApplyWindowInsets()`的返回值
 * ```
 * decorView.setOnApplyWindowInsetsListenerCompat { _, insets ->
 *     val typeMask = statusBars()
 *     val outcome = insets.decorInsets(typeMask)
 *     decorView.onApplyWindowInsetsCompat(outcome)
 *     insets // 注意，不能返回outcome
 * }
 * ```
 */
@CheckResult
fun WindowInsetsCompat.decorInsets(@InsetsType typeMask: Int): WindowInsetsCompat {
    return InsetsConsumer(this).decorInsets(typeMask)
}

/**
 * 消费指定类型集的Insets，消费结果可作为[View.onApplyWindowInsets]的返回值
 *
 * ```
 * view.setOnApplyWindowInsetsListenerCompat { _, insets ->
 *     val typeMask = statusBars()
 *     val outcome = insets.consumeInsets(typeMask)
 *     outcome.getInsets(typeMask) // 改变Insets结果，返回Insets.NONE
 *     outcome.getInsetsIgnoringVisibility(typeMask) // 不改变Insets结果
 *     outcome.isVisible(typeMask) // 改变isVisible结果，返回false
 *     outcome
 * }
 * ```
 */
@CheckResult
fun WindowInsetsCompat.consumeInsets(@InsetsType typeMask: Int): WindowInsetsCompat {
    return InsetsConsumer(this).consumeInsets(typeMask)
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
 *
 * **注意**：当前View能被分发[WindowInsets]，该函数才会生效。
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
 * 当首次或再次附加到window时，申请[WindowInsets]分发
 */
fun View.requestApplyInsetsOnAttach() {
    InsetsRequester(this).requestApplyInsetsOnAttach()
}

/**
 * 移除[requestApplyInsetsOnAttach]的设置
 */
fun View.removeRequestApplyInsetsOnAttach() {
    InsetsRequester(this).removeRequestApplyInsetsOnAttach()
}

/**
 * 对View调用[doOnApplyWindowInsets]，构建负责处理[WindowInsets]的[InsetsReceiver]。
 * [InsetsReceiver]提供常用的[WindowInsets]处理逻辑，例如[InsetsReceiver.paddings]。
 *
 * ```
 * view.insets() // 记录view的当前状态作为初始值
 *     .paddings(statusBars()) // view.paddingTop = initialValue + statusBarHeight
 *     .margins(navigationBars()) // view.marginBottom = initialValue + navigationBarHeight
 *     .dimension(navigationBars()) // 当view.layoutParams.height初始为具体值时，增加navigationBarHeight
 * ```
 *
 * **注意**：当前View能被分发[WindowInsets]，该函数才会生效。
 */
@CheckResult
fun View.insets(): InsetsReceiver {
    val receiver = InsetsReceiver()
    doOnApplyWindowInsets(receiver)
    return receiver
}

/**
 * 通用的手势导航栏EdgeToEdge处理逻辑，可以跟[doOnApplyWindowInsets]结合使用
 */
fun View.handleGestureNavBarEdgeToEdge(insets: WindowInsetsCompat, initialState: ViewState) {
    val navigationBarHeight = insets.navigationBarHeight
    val isGestureNavigationBar = insets.isGestureNavigationBar(this)
    // 1. 若当前是手势导航栏，则增加高度，否则保持初始高度
    updateLayoutSize(height = when {
        initialState.params.height < 0 -> initialState.params.height
        !isGestureNavigationBar -> initialState.params.height
        else -> initialState.params.height + navigationBarHeight
    })
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

/**
 * 当分发到[WindowInsetsCompat]时，调用[handleGestureNavBarEdgeToEdge]
 */
@Deprecated(
    message = "合并常用的WindowInsets处理逻辑",
    replaceWith = ReplaceWith("insets().gestureNavBarEdgeToEdge()")
)
fun View.handleGestureNavBarEdgeToEdgeOnApply() {
    doOnApplyWindowInsets { view, insets, initialState ->
        view.handleGestureNavBarEdgeToEdge(insets, initialState)
    }
}