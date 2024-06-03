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

@file:SuppressLint("SupportAnnotationUsage")
@file:Suppress("ConvertObjectToDataObject")

package com.xiaocydx.insets.systembar

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.WindowInsets
import androidx.annotation.ColorInt
import com.xiaocydx.insets.getRootWindowInsetsCompat
import com.xiaocydx.insets.systembar.EdgeToEdge.Disabled
import com.xiaocydx.insets.systembar.EdgeToEdge.Enabled
import com.xiaocydx.insets.systembar.EdgeToEdge.Gesture

/**
 * [SystemBar]控制器
 *
 * @author xcc
 * @date 2023/12/21
 */
interface SystemBarController {

    /**
     * 状态栏背景色，默认为`SystemBar.install()`设置的[Default.statusBarColor]，
     * 若[Default.statusBarColor]为`null`，则为`window.statusBarColor`的初始值。
     */
    @get:ColorInt
    @set:ColorInt
    var statusBarColor: Int

    /**
     * 导航栏背景色，默认为`SystemBar.install()`设置的[Default.navigationBarColor]，
     * 若[Default.navigationBarColor]为`null`，则为`window.navigationBarColor`的初始值。
     */
    @get:ColorInt
    @set:ColorInt
    var navigationBarColor: Int

    /**
     * 状态栏EdgeToEdge，默认为`SystemBar.install()`设置的[Default.statusBarEdgeToEdge]
     *
     * 1. [Disabled]：消费[WindowInsets]的状态栏Insets，设置状态栏高度的间距，绘制背景色。
     * 2. [Enabled]：不消费[WindowInsets]的状态栏Insets，不设置状态栏高度的间距，不绘制背景色。
     * 3. [Gesture]：作用等于[Enabled]，[Gesture]仅对[navigationBarEdgeToEdge]有区分。
     *
     * [Disabled]消费[WindowInsets]的状态栏Insets，目的是避免子View重复处理状态栏Insets，设置高度间距，
     * 子View处理[WindowInsets]，仍需要获取状态栏高度，则可以通过[View.getRootWindowInsetsCompat]获取。
     */
    var statusBarEdgeToEdge: EdgeToEdge

    /**
     * 导航栏EdgeToEdge，默认为`SystemBar.install()`设置的[Default.navigationBarEdgeToEdge]
     *
     * 1. [Disabled]：消费[WindowInsets]的导航栏Insets，设置导航栏高度的间距，绘制背景色。
     * 2. [Enabled]：不消费[WindowInsets]的导航栏Insets，不设置导航栏高度的间距，不绘制背景色。
     * 3. [Gesture]：当前为手势导航栏时，作用等于[Enabled]，否则作用等于[Disabled]。
     *
     * [Disabled]消费[WindowInsets]的导航栏Insets，目的是避免子View重复处理导航栏Insets，设置高度间距，
     * 子View处理[WindowInsets]，仍需要获取导航栏高度，则可以通过[View.getRootWindowInsetsCompat]获取。
     */
    var navigationBarEdgeToEdge: EdgeToEdge

    /**
     * 当状态栏的背景为浅色时，可以将该属性设为`true`，以便于清楚看到状态栏的图标
     *
     * 默认为`SystemBar.install()`设置的[Default.isAppearanceLightStatusBar]，
     * 若[Default.isAppearanceLightStatusBar]为`null`，则调用[isAppearanceLight]，
     * 传入`window.statusBarColor`的初始值进行推断。
     *
     * 对应`WindowInsetsControllerCompat.isAppearanceLightStatusBars`。
     */
    var isAppearanceLightStatusBar: Boolean

    /**
     * 当导航栏的背景为浅色时，可以将该属性设为`true`，以便于清楚看到导航栏的图标
     *
     * 默认为`SystemBar.install()`设置的[Default.isAppearanceLightNavigationBar]，
     * 若[Default.isAppearanceLightNavigationBar]为`null`，则调用[isAppearanceLight]，
     * 传入`window.navigationBarColor`的初始值进行推断。
     *
     * 对应`WindowInsetsControllerCompat.isAppearanceLightNavigationBars`。
     */
    var isAppearanceLightNavigationBar: Boolean

    /**
     * [SystemBarController]属性的默认值
     */
    data class Default(
        val statusBarColor: Int? = null,
        val navigationBarColor: Int? = null,
        val statusBarEdgeToEdge: EdgeToEdge = Disabled,
        val navigationBarEdgeToEdge: EdgeToEdge = Disabled,
        val isAppearanceLightStatusBar: Boolean? = null,
        val isAppearanceLightNavigationBar: Boolean? = null
    )

    companion object {
        @Volatile internal var default = Default()
    }
}

val SystemBarController.Companion.name: String
    get() = SystemBarController::class.java.simpleName

sealed class EdgeToEdge {
    object Disabled : EdgeToEdge()
    object Enabled : EdgeToEdge()
    object Gesture : EdgeToEdge()
}

/**
 * 若[color]为浅色，则返回`true`，可用于：
 * ```
 * val controller: SystemBarController = ...
 * controller.isAppearanceLightStatusBar = isAppearanceLight(color)
 * controller.isAppearanceLightNavigationBar = isAppearanceLight(color)
 * ```
 */
fun isAppearanceLight(@ColorInt color: Int): Boolean {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    return hsv[2] > 0.7
}