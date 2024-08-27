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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
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

@Suppress("DEPRECATION")
internal fun Window.disableDecorFitsSystemWindows() {
    if (isInitialized) return
    // 记录系统栏的初始背景色
    decorView.setTag(initialKey, when {
        Build.VERSION.SDK_INT < 35 -> brokenWindowInitialState()
        else -> newWindowInitialState()
    })
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

private fun Window.ensureSystemBarColorInitialized() {
    // 执行decorView创建流程，确保背景色初始化完成
    decorView
}

@Suppress("DEPRECATION")
private fun Window.brokenWindowInitialState(): WindowInitialState {
    ensureSystemBarColorInitialized()
    return WindowInitialState(statusBarColor, navigationBarColor)
}

@Suppress("DEPRECATION")
@SuppressLint("ResourceType")
private fun Window.newWindowInitialState(): WindowInitialState {
    ensureSystemBarColorInitialized()
    var statusBarColor = statusBarColor
    var navigationBarColor = navigationBarColor
    val maybeEdgeToEdgeEnforced = statusBarColor == Color.TRANSPARENT
            || navigationBarColor == Color.TRANSPARENT
    if (!maybeEdgeToEdgeEnforced) {
        // statusBarColor和navigationBarColor都不为Color.TRANSPARENT,
        // 表示window主题包含windowOptOutEdgeToEdgeEnforcement = true，
        // 或者创建decorView之前设置了背景色，此时不需要做兼容处理。
    } else {
        // 实现逻辑copy自PhoneWindow.generateLayout()
        val typedArray = context.obtainStyledAttributes(intArrayOf
            (android.R.attr.statusBarColor, android.R.attr.navigationBarColor)
        )
        statusBarColor = typedArray.getColor(0, Color.BLACK)

        val navBarCompatibleColor = getColor("navigation_bar_compatible")
        val navBarDefaultColor = getColor("navigation_bar_default")
        navigationBarColor = typedArray.getColor(1, navBarDefaultColor)
        typedArray.recycle()

        val navigationBarColorSpecified = navigationBarColor != navBarDefaultColor
        if (!navigationBarColorSpecified && !getBoolean("config_navBarDefaultTransparent")) {
            navigationBarColor = navBarCompatibleColor
        }
    }
    return WindowInitialState(statusBarColor, navigationBarColor)
}

@Suppress("DEPRECATION")
@SuppressLint("DiscouragedApi")
private fun Window.getColor(name: String): Int {
    val resourceId = context.resources.getIdentifier(name, "color", "android")
    return if (resourceId == 0) 0 else context.resources.getColor(resourceId)
}

@SuppressLint("DiscouragedApi")
private fun Window.getBoolean(name: String): Boolean {
    val resourceId = context.resources.getIdentifier(name, "bool", "android")
    return if (resourceId == 0) false else context.resources.getBoolean(resourceId)
}

private object SystemBarDisableDecorFitsSystemWindowsReason : DisableDecorFitsSystemWindowsReason {
    override fun get(): String = "SystemBar已初始化Window"

    override fun run() {
        throw UnsupportedOperationException("${get()}，不允许再调用Window.disableDecorFitsSystemWindows()")
    }
}