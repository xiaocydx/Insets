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

@file:Suppress("DEPRECATION")

package com.xiaocydx.insets

import android.os.Build
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

/**
 * [WindowInsetsCompat]的检查函数，负责兼容版本和处理细节问题
 *
 * @author xcc
 * @date 2024/5/1
 */
@JvmInline
internal value class InsetsInspector(private val insets: WindowInsetsCompat) {

    fun isStatusBarHidden(view: View) = when {
        !isCompatNeeded -> !insets.isVisible(statusBars())
        else -> view.containsFlag(statusBarHiddenFlag) || !insets.isVisible(statusBars())
    }

    fun isNavigationBarHidden(view: View) = when {
        !isCompatNeeded -> !insets.isVisible(navigationBars())
        else -> view.containsFlag(navigationBarHiddenFlag) || !insets.isVisible(navigationBars())
    }

    fun isGestureNavigationBar(view: View): Boolean {
        val threshold = (24 * view.resources.displayMetrics.density).toInt()
        return insets.navigationBarHeight <= threshold.coerceAtLeast(66)
    }

    @InsetsType
    fun getSystemBarHiddenConsumeTypeMask(view: View): Int {
        if (!isCompatNeeded) return 0
        var typeMask = 0
        if (isStatusBarHidden(view)) typeMask = typeMask or statusBars()
        if (isNavigationBarHidden(view)) typeMask = typeMask or navigationBars()
        return typeMask
    }

    private fun View.containsFlag(flag: Int): Boolean {
        return rootView.systemUiVisibility and flag == flag
    }

    private companion object {
        val isCompatNeeded = Build.VERSION.SDK_INT < 30
        const val statusBarHiddenFlag = SYSTEM_UI_FLAG_FULLSCREEN
        const val navigationBarHiddenFlag = SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}