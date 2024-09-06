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
 * [SystemBarController]的包装类，可用于扩展`systemBarController()`的Receiver
 *
 * @author xcc
 * @date 2024/5/7
 */
open class SystemBarControllerWrapper : SystemBarController {
    private val default = SystemBarController.default
    private val flags = SystemBarControllerFlags(default)
    private var delegate: SystemBarController? = null

    final override var statusBarColor = default.statusBarColor ?: 0
        get() = delegate?.statusBarColor ?: field
        set(value) {
            field = value
            flags.setStatusBarColor()
            delegate?.statusBarColor = value
        }

    final override var navigationBarColor = default.navigationBarColor ?: 0
        get() = delegate?.navigationBarColor ?: field
        set(value) {
            field = value
            flags.setNavigationBarColor()
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

    final override var isAppearanceLightStatusBar = default.isAppearanceLightStatusBar ?: false
        get() = delegate?.isAppearanceLightStatusBar ?: field
        set(value) {
            field = value
            flags.setAppearanceLightStatusBar()
            delegate?.isAppearanceLightStatusBar = value
        }

    final override var isAppearanceLightNavigationBar = default.isAppearanceLightNavigationBar ?: false
        get() = delegate?.isAppearanceLightNavigationBar ?: field
        set(value) {
            field = value
            flags.setAppearanceLightNavigationBar()
            delegate?.isAppearanceLightNavigationBar = value
        }

    fun attachDelegate(delegate: SystemBarController) {
        check(this.delegate == null) { "已设置delegate" }
        applyPendingSystemBarConfig(delegate)
        this.delegate = delegate
    }

    private fun applyPendingSystemBarConfig(delegate: SystemBarController) = with(flags) {
        if (hasAppearanceLightStatusBar) delegate.isAppearanceLightStatusBar = isAppearanceLightStatusBar
        if (hasAppearanceLightNavigationBar) delegate.isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
        if (hasStatusBarColor) delegate.statusBarColor = statusBarColor
        if (hasNavigationBarColor) delegate.navigationBarColor = navigationBarColor
        delegate.statusBarEdgeToEdge = statusBarEdgeToEdge
        delegate.navigationBarEdgeToEdge = navigationBarEdgeToEdge
    }
}