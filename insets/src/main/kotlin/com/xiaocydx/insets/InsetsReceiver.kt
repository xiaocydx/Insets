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
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

/**
 * [View.insets]的实现类，提供常用的[WindowInsets]处理逻辑
 *
 * @author xcc
 * @date 2024/3/9
 */
class InsetsReceiver internal constructor() : (View, WindowInsetsCompat, ViewState) -> Unit {
    private var storeMap: Array<InsetsStore?>? = null
    private var gestureNavBarEdgeToEdge = false

    /**
     * 设置`view.paddings`，[typeMask]类型集表示`view.paddings`的方向
     *
     * ```
     * // 记录view的当前状态作为初始值
     * view.insets()
     *
     * // view.paddingTop = initialValue + statusBarHeight
     * view.insets().paddings(statusBars())
     *
     * // view.paddingTop = statusBarHeight
     * view.insets().paddings(statusBars(), ignoringInitial = true)
     *
     * // view.paddingTop = initialValue + statusBarHeight
     * // view.paddingBottom = initialValue + navigationBarHeight
     * view.insets().paddings(statusBars() or navigationBars())
     */
    fun paddings(@InsetsType typeMask: Int, ignoringInitial: Boolean = false) = apply {
        setStore(KEY_PADDINGS, typeMask, ignoringInitial)
    }

    /**
     * 设置`view.margins`，[typeMask]类型集表示`view.margins`的方向
     *
     * ```
     * // 记录view的当前状态作为初始值
     * view.insets()
     *
     * // view.marginTop = initialValue + statusBarHeight
     * view.insets().margins(statusBars())
     *
     * // view.marginTop = statusBarHeight
     * view.insets().margins(statusBars(), ignoringInitial = true)
     *
     * // view.marginTop = initialValue + statusBarHeight
     * // view.marginBottom = initialValue + navigationBarHeight
     * view.insets().margins(statusBars() or navigationBars())
     */
    fun margins(@InsetsType typeMask: Int, ignoringInitial: Boolean = false) = apply {
        setStore(KEY_MARGINS, typeMask, ignoringInitial)
    }

    /**
     * 设置`view.layoutParams`的尺寸，[typeMask]类型集表示`view.layoutParams`的宽高
     *
     * **注意**：当`view.layoutParams`的宽高初始为具体值时，该函数才会生效：
     * ```
     * view.layoutParams.height = MATCH_PARENT // 初始为MATCH_PARENT，该函数不会生效
     * view.layoutParams.height = WRAP_CONTENT // 初始为WRAP_CONTENT，该函数不会生效
     * view.layoutParams.height = 100.dp // 初始为100.dp，该函数会生效
     *
     * // 记录view的当前状态作为初始值
     * view.insets()
     *
     * // view.layoutParams.height = initialValue + statusBarHeight
     * view.insets().dimension(statusBars())
     *
     * // view.layoutParams.height = statusBarHeight
     * view.insets().dimension(statusBars(), ignoringInitial = true)
     *
     * // view.layoutParams.height = initialValue + statusBarHeight + navigationBarHeight
     * view.insets().dimension(statusBars() or navigationBars())
     * ```
     */
    fun dimension(@InsetsType typeMask: Int, ignoringInitial: Boolean = false) = apply {
        setStore(KEY_DIMENSION, typeMask, ignoringInitial)
    }

    /**
     * 设置通用的手势导航栏EdgeToEdge处理逻辑
     *
     * 若当前是手势导航栏，则`view.layoutParams.height`（初始为具体值才生效），
     * 以及`view.paddingBottom`增加navigationBarHeight，否则保持initialValue。
     * 详细的处理逻辑可以看[handleGestureNavBarEdgeToEdge]。
     */
    fun gestureNavBarEdgeToEdge() = apply { gestureNavBarEdgeToEdge = true }

    @SinceKotlin("999.9")
    @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
    override fun invoke(view: View, insets: WindowInsetsCompat, initialState: ViewState) {
        getStore(KEY_PADDINGS)?.let { paddings ->
            val applyInsets = getInsets(paddings, insets)
            val finalState = paddings.getFinalState(initialState)
            view.updatePadding(
                left = finalState.paddings.left + applyInsets.left,
                top = finalState.paddings.top + applyInsets.top,
                right = finalState.paddings.right + applyInsets.right,
                bottom = finalState.paddings.bottom + applyInsets.bottom
            )
        }
        getStore(KEY_MARGINS)?.let { margins ->
            val applyInsets = getInsets(margins, insets)
            val finalState = margins.getFinalState(initialState)
            view.updateMargins(
                left = finalState.params.marginLeft + applyInsets.left,
                top = finalState.params.marginTop + applyInsets.top,
                right = finalState.params.marginRight + applyInsets.right,
                bottom = finalState.params.marginBottom + applyInsets.bottom
            )
        }
        getStore(KEY_DIMENSION)?.let { dimension ->
            val applyInsets = getInsets(dimension, insets)
            val finalState = dimension.getFinalState(initialState)
            view.updateLayoutSize(
                width = when {
                    finalState.params.width < 0 -> finalState.params.width
                    else -> finalState.params.width + applyInsets.left + applyInsets.right
                },
                height = when {
                    finalState.params.height < 0 -> finalState.params.height
                    else -> finalState.params.height + applyInsets.top + applyInsets.bottom
                }
            )
        }
        if (gestureNavBarEdgeToEdge) {
            view.handleGestureNavBarEdgeToEdge(insets, initialState)
        }
        clearInsets()
    }

    private fun setStore(key: Int, typeMask: Int, ignoringInitial: Boolean) {
        if (storeMap == null) {
            storeMap = Array(STORE_MAP_SIZE) { null }
        }
        var store = storeMap!![key]
        if (store == null) {
            store = InsetsStore()
            storeMap!![key] = store
        }
        store.typeMask = typeMask
        store.ignoringInitial = ignoringInitial
    }

    private fun getStore(key: Int) = storeMap?.get(key)

    private fun getInsets(store: InsetsStore, insets: WindowInsetsCompat): Insets {
        // 查找相同typeMask的insets，避免重复调用getInsets()
        storeMap?.forEach {
            val sameInsets = it?.getSameInsets(store)
            if (sameInsets != null) return sameInsets
        }
        store.insets = insets.getInsets(store.typeMask)
        return store.insets!!
    }

    private fun clearInsets() {
        storeMap?.forEach { it?.insets = null }
    }

    private class InsetsStore(
        var typeMask: Int = 0,
        var ignoringInitial: Boolean = false,
        var insets: Insets? = null
    ) {
        fun getSameInsets(other: InsetsStore): Insets? {
            return insets?.takeIf { typeMask == other.typeMask }
        }

        fun getFinalState(initialState: ViewState): ViewState {
            return if (ignoringInitial) EMPTY_STATE else initialState
        }
    }

    private companion object {
        const val KEY_PADDINGS = 0
        const val KEY_MARGINS = 1
        const val KEY_DIMENSION = 2
        const val STORE_MAP_SIZE = KEY_DIMENSION + 1
        val EMPTY_STATE = ViewState()
    }
}