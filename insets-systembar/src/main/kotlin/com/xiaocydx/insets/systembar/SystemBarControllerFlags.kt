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

package com.xiaocydx.insets.systembar

/**
 * 记录[SystemBarController]是否设置过值
 *
 * @author xcc
 * @date 2024/6/3
 */
internal class SystemBarControllerFlags(default: SystemBarController.Default) {
    var hasStatusBarColor = default.statusBarColor != null
        private set

    var hasNavigationBarColor = default.navigationBarColor != null
        private set

    var hasAppearanceLightStatusBar = default.isAppearanceLightStatusBar != null
        private set

    var hasAppearanceLightNavigationBar = default.isAppearanceLightNavigationBar != null
        private set

    fun setStatusBarColor() {
        hasStatusBarColor = true
    }

    fun setNavigationBarColor() {
        hasNavigationBarColor = true
    }

    fun setAppearanceLightStatusBar() {
        hasAppearanceLightStatusBar = true
    }

    fun setAppearanceLightNavigationBar() {
        hasAppearanceLightNavigationBar = true
    }
}