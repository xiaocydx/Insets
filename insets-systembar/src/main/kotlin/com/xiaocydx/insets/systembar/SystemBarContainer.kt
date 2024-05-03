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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import com.xiaocydx.insets.consumeInsets
import com.xiaocydx.insets.isGestureNavigationBar
import com.xiaocydx.insets.navigationBarHeight
import com.xiaocydx.insets.requestApplyInsetsCompat
import com.xiaocydx.insets.requestApplyInsetsOnAttach
import com.xiaocydx.insets.statusBarHeight
import com.xiaocydx.insets.toWindowInsetsCompat
import com.xiaocydx.insets.updatePadding

/**
 * [SystemBar]视图容器
 *
 * @author xcc
 * @date 2023/12/21
 */
internal class SystemBarContainer(context: Context) : FrameLayout(context) {
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var consumeTouchEvent = false

    init {
        setWillNotDraw(false)
        requestApplyInsetsOnAttach()
    }

    var statusBarColor: Int
        get() = statusBarDrawable.color
        set(value) {
            if (statusBarDrawable.color == value) return
            statusBarDrawable.color = value
            invalidateDrawable(statusBarDrawable)
        }

    var navigationBarColor: Int
        get() = navigationBarDrawable.color
        set(value) {
            if (navigationBarDrawable.color == value) return
            navigationBarDrawable.color = value
            invalidateDrawable(navigationBarDrawable)
        }

    var statusBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            if (field == value) return
            field = value
            requestApplyInsetsCompat()
        }

    var navigationBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            if (field == value) return
            field = value
            requestApplyInsetsCompat()
        }

    init {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    fun enableConsumeTouchEvent() {
        consumeTouchEvent = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) = consumeTouchEvent

    override fun verifyDrawable(who: Drawable) = when (who) {
        statusBarDrawable, navigationBarDrawable -> true
        else -> super.verifyDrawable(who)
    }

    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
        super.dispatchApplyWindowInsets(insets)
        // 兼容到跟Android 11一样的分发效果，确保同级子View能处理已消费的Insets
        return insets
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        var typeMask = 0
        var paddingTop = 0
        var paddingBottom = 0
        val applyInsets = insets.toWindowInsetsCompat(this)
        if (statusBarEdgeToEdge === EdgeToEdge.Disabled) {
            typeMask = typeMask or statusBars()
            paddingTop = applyInsets.statusBarHeight
        }
        if (navigationBarEdgeToEdge === EdgeToEdge.Disabled
                || (navigationBarEdgeToEdge === EdgeToEdge.Gesture
                        && !applyInsets.isGestureNavigationBar(this))) {
            typeMask = typeMask or navigationBars()
            paddingBottom = applyInsets.navigationBarHeight
        }
        updatePadding(top = paddingTop, bottom = paddingBottom)
        return applyInsets.consumeInsets(typeMask).toWindowInsets()!!
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, paddingTop)
        navigationBarDrawable.setBounds(0, height - paddingBottom, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }
}