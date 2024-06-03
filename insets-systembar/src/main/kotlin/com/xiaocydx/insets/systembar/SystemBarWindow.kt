/*
 * Copyright 2022 xiaocydx
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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.insets.systembar

import android.graphics.Color
import android.view.Window
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import com.xiaocydx.insets.DisableDecorFitsSystemWindowsReason
import com.xiaocydx.insets.checkDispatchApplyInsetsCompatibility
import com.xiaocydx.insets.disableDecorFitsSystemWindowsInternal

private val initialKey: Int
    get() = R.id.tag_decor_window_initial_state

private val Window.isInitialized: Boolean
    get() = decorView.getTag(initialKey) != null

internal val Window.initialState: WindowInitialState
    get() = requireNotNull(decorView.getTag(initialKey) as? WindowInitialState) { "未完成初始化" }

internal data class WindowInitialState(val statusBarColor: Int, val navigationBarColor: Int)

internal fun Window.disableDecorFitsSystemWindows() {
    if (isInitialized) return
    // 记录系统栏的初始背景色，执行完decorView创建流程，才能获取到初始背景色
    decorView.setTag(initialKey, WindowInitialState(statusBarColor, navigationBarColor))
    // navigationBar的背景色会影响isAppearanceLightNavigationBar的实际效果，
    // 例如某些设备的初始背景色是白色，isAppearanceLightNavigationBar = false，
    // 但是navigationBar的前景色是isAppearanceLightNavigationBar = true的效果。
    // 为了解决这种差异，将系统栏背景色设置为透明，由SystemBar的实现控制实际效果。
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT
    disableDecorFitsSystemWindowsInternal(
        consumeTypeMask = statusBars() or navigationBars(),
        reason = SystemBarDisableDecorFitsSystemWindowsReason
    )
    checkDispatchApplyInsetsCompatibility()
}

private object SystemBarDisableDecorFitsSystemWindowsReason : DisableDecorFitsSystemWindowsReason {
    override fun get(): String = "SystemBar已初始化Window"

    override fun run() {
        throw UnsupportedOperationException("${get()}，不允许再调用Window.disableDecorFitsSystemWindows()")
    }
}