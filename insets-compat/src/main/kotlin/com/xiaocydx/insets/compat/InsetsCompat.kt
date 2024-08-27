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

import android.app.Application
import android.view.View
import android.view.Window

/**
 * 兼容方案的全局配置
 *
 * @author xcc
 * @date 2024/8/27
 */
object InsetsCompat {

    @Volatile
    internal var isFullScreenCompatEnabled = true
        private set

    @Volatile
    internal var isImmutableCompatEnabled = true
        private set

    @Volatile
    internal var isInsetsAnimationCompatEnabled = true
        private set

    /**
     * 全局禁用[Window.enableDispatchApplyInsetsFullscreenCompat]的兼容方案
     *
     * **注意**：只能在[Application]初始化阶段调用该函数。
     */
    fun disableFullScreenCompat() {
        isFullScreenCompatEnabled = false
    }

    /**
     * 全局禁用[View.setOnApplyWindowInsetsListenerImmutable]、
     * [View.setWindowInsetsAnimationCallbackImmutable]的兼容方案。
     *
     * **注意**：只能在[Application]初始化阶段调用该函数。
     */
    fun disableImmutableCompatEnabled() {
        isImmutableCompatEnabled = false
    }

    /**
     * 全局禁用[Window.modifyImeAnimation]、[Window.restoreImeAnimation]、
     * [Window.setWindowInsetsAnimationCallbackCompat]的兼容方案。
     *
     * **注意**：只能在[Application]初始化阶段调用该函数。
     */
    fun disableInsetsAnimationCompat() {
        isInsetsAnimationCompatEnabled = false
    }
}