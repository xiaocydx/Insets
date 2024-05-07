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
 * @author xcc
 * @date 2024/5/7
 */
open class SystemBarControllerWrapper : SystemBarController {
    private val default = SystemBarController.default
    private var hasStatusBarColor = default.statusBarColor != null
    private var hasNavigationBarColor = default.navigationBarColor != null
    private var delegate: SystemBarController? = null

    final override var statusBarColor = default.statusBarColor ?: 0
        get() = delegate?.statusBarColor ?: field
        set(value) {
            field = value
            hasStatusBarColor = true
            delegate?.statusBarColor = value
        }

    final override var navigationBarColor = default.navigationBarColor ?: 0
        get() = delegate?.navigationBarColor ?: field
        set(value) {
            field = value
            hasNavigationBarColor = true
            delegate?.navigationBarColor = value
        }

    final override var statusBarEdgeToEdge = default.statusBarEdgeToEdge
        get() = delegate?.statusBarEdgeToEdge ?: field
        set(value) {
            field = value
            delegate?.statusBarEdgeToEdge = value
        }

    final override var navigationBarEdgeToEdge = default.navigationBarEdgeToEdge
        get() = delegate?.navigationBarEdgeToEdge ?: field
        set(value) {
            field = value
            delegate?.navigationBarEdgeToEdge = value
        }

    final override var isAppearanceLightStatusBar = default.isAppearanceLightStatusBar
        get() = delegate?.isAppearanceLightStatusBar ?: field
        set(value) {
            field = value
            delegate?.isAppearanceLightStatusBar = value
        }

    final override var isAppearanceLightNavigationBar = default.isAppearanceLightNavigationBar
        get() = delegate?.isAppearanceLightNavigationBar ?: field
        set(value) {
            field = value
            delegate?.isAppearanceLightNavigationBar = value
        }

    fun setDelegate(delegate: SystemBarController) {
        check(this.delegate == null) { "已设置delegate" }
        applyPendingSystemBarConfig(delegate)
        this.delegate = delegate
    }

    private fun applyPendingSystemBarConfig(delegate: SystemBarController) {
        if (hasStatusBarColor) delegate.statusBarColor = statusBarColor
        if (hasNavigationBarColor) delegate.navigationBarColor = navigationBarColor
        delegate.statusBarEdgeToEdge = statusBarEdgeToEdge
        delegate.navigationBarEdgeToEdge = navigationBarEdgeToEdge
        delegate.isAppearanceLightStatusBar = isAppearanceLightStatusBar
        delegate.isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
    }
}