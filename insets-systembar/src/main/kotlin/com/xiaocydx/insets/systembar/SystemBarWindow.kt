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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.insets.systembar

import android.view.Window
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import com.xiaocydx.insets.disableDecorFitsSystemWindowsInternal

internal val Window.initialState: WindowInitialState
    get() {
        val key = R.id.tag_decor_window_initial_state
        var state = decorView.getTag(key) as? WindowInitialState
        if (state == null) {
            state = WindowInitialState(this)
            decorView.setTag(key, state)
        }
        return state
    }

internal data class WindowInitialState(val statusBarColor: Int, val navigationBarColor: Int) {
    constructor(window: Window) : this(window.statusBarColor, window.navigationBarColor)
}

internal fun Window.recordSystemBarInitialColor() {
    // 记录StatusBar和NavigationBar的初始背景色，
    // 执行完decorView创建流程，才能获取到背景色。
    initialState
}

internal fun Window.disableDecorFitsSystemWindows() {
    disableDecorFitsSystemWindowsInternal(consumeTypeMask = statusBars() or navigationBars())
}