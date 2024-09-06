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

@file:Suppress("UnusedReceiverParameter")

package com.xiaocydx.insets.compat

import android.app.Application
import android.view.View
import android.view.Window
import com.xiaocydx.insets.Insets

/**
 * 全局禁用[Window.enableDispatchApplyInsetsFullscreenCompat]的兼容方案
 *
 * **注意**：只能在[Application]初始化阶段调用该函数。
 */
fun Insets.disableFullScreenCompat() = apply {
    InsetsCompat.isFullScreenCompatEnabled = false
}

/**
 * 全局禁用[View.setOnApplyWindowInsetsListenerImmutable]、
 * [View.setWindowInsetsAnimationCallbackImmutable]的兼容方案。
 *
 * **注意**：只能在[Application]初始化阶段调用该函数。
 */
fun Insets.disableImmutableCompat() = apply {
    InsetsCompat.isImmutableCompatEnabled = false
}

/**
 * 全局禁用[Window.modifyImeAnimation]、[Window.restoreImeAnimation]、
 * [Window.setWindowInsetsAnimationCallbackCompat]的兼容方案。
 *
 * **注意**：只能在[Application]初始化阶段调用该函数。
 */
fun Insets.disableInsetsAnimationCompat() = apply {
    InsetsCompat.isInsetsAnimationCompatEnabled = false
}

internal fun Insets.isFullScreenCompatEnabled(): Boolean {
    return InsetsCompat.isFullScreenCompatEnabled
}

internal fun Insets.isImmutableCompatEnabled(): Boolean {
    return InsetsCompat.isImmutableCompatEnabled
}

internal fun Insets.isInsetsAnimationCompatEnabled(): Boolean {
    return InsetsCompat.isInsetsAnimationCompatEnabled
}

private object InsetsCompat {
    @Volatile var isFullScreenCompatEnabled = true
    @Volatile var isImmutableCompatEnabled = true
    @Volatile var isInsetsAnimationCompatEnabled = true
}