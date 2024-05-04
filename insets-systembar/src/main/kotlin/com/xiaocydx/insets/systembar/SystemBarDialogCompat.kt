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

import android.app.Dialog
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import java.lang.ref.WeakReference

/**
 * 兼容[Dialog]的配置属性
 *
 * @author xcc
 * @date 2024/4/30
 */
internal class SystemBarDialogCompat(private val dialog: Dialog) {
    private var attributesDeliver: DialogAttributesDeliver? = null
    private var touchOutsideDeliver: DialogTouchOutsideDeliver? = null

    fun attach() {
        if (attributesDeliver == null) {
            attributesDeliver = DialogAttributesDeliver(dialog)
            attributesDeliver!!.attach()
        }
        if (touchOutsideDeliver == null) {
            touchOutsideDeliver = DialogTouchOutsideDeliver(dialog)
            touchOutsideDeliver!!.attach()
        }
    }

    fun detach() {
        attributesDeliver?.detach()
        touchOutsideDeliver?.detach()
        attributesDeliver = null
        touchOutsideDeliver = null
    }

    fun setContentView(view: View?) {
        attributesDeliver?.setContentView(view)
        touchOutsideDeliver?.setContentView(view)
    }
}

/**
 * 兼容`Dialog.window.attributes`
 */
private class DialogAttributesDeliver(
    private val dialog: Dialog
) : OnAttachStateChangeListener, OnPreDrawListener {
    private var contentViewRef: WeakReference<View>? = null
    private val decorView: View?
        get() = dialog.window?.decorView

    fun attach() {
        decorView?.addOnAttachStateChangeListener(this)
        decorView?.viewTreeObserver?.addOnPreDrawListener(this)
    }

    fun detach() {
        decorView?.removeOnAttachStateChangeListener(this)
        decorView?.viewTreeObserver?.removeOnPreDrawListener(this)
    }

    fun setContentView(view: View?) {
        contentViewRef = view?.let(::WeakReference)
        deliverAttributes()
    }

    override fun onViewDetachedFromWindow(v: View) = Unit

    override fun onViewAttachedToWindow(v: View) {
        // 首帧在measure之前传递attributes，首帧生效
        deliverAttributes()
    }

    override fun onPreDraw(): Boolean {
        // 非首帧在draw之前传递attributes，下一帧生效
        deliverAttributes()
        return true
    }

    private fun deliverAttributes() {
        val contentView = contentViewRef?.get() ?: return
        val attributes = dialog.window?.attributes ?: return
        val lp = contentView.layoutParams as? FrameLayout.LayoutParams ?: return
        if (lp.gravity != attributes.gravity) {
            lp.gravity = attributes.gravity
            contentView.layoutParams = lp
        }
    }
}

/**
 * 兼容[Dialog.setCancelable]和[Dialog.setCanceledOnTouchOutside]
 */
private class DialogTouchOutsideDeliver(
    private val dialog: Dialog
) : OnTouchListener {
    private var contentViewRef: WeakReference<View>? = null
    private val contentViewPoint = IntArray(2)
    private val contentViewBounds = RectF()
    private val prePieSlop = ViewConfiguration.get(dialog.context).scaledWindowTouchSlop

    fun attach() {
        dialog.window?.decorView?.setOnTouchListener(this)
    }

    fun detach() {
        dialog.window?.decorView?.setOnTouchListener(null)
    }

    fun setContentView(view: View?) {
        contentViewRef = view?.let(::WeakReference)
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val contentView = contentViewRef?.get()
        if (contentView != null) {
            contentView.getLocationInWindow(contentViewPoint)
            val x = contentViewPoint[0].toFloat()
            val y = contentViewPoint[1].toFloat()
            contentViewBounds.set(x, y, x + contentView.width, y + contentView.height)
            if (contentViewBounds.contains(event.x, event.y)) return false
        }

        val outsideEvent = MotionEvent.obtain(event)
        if (event.action == MotionEvent.ACTION_UP) {
            outsideEvent.action = MotionEvent.ACTION_OUTSIDE
        }
        if (Build.VERSION.SDK_INT < 28) {
            // Android 9.0以下，Window.shouldCloseOnTouch()未判断MotionEvent.ACTION_OUTSIDE，
            // 将action确保为MotionEvent.ACTION_DOWN，让Window.isOutOfBounds()做下一步判断。
            outsideEvent.action = MotionEvent.ACTION_DOWN
            // Window.isOutOfBounds()的判断逻辑包含(event.x < -slop) || (event.y < -slop),
            // 将outsideEvent的x和y设置得比-slop更小，以此让Window.isOutOfBounds()返回true。
            outsideEvent.setLocation((-prePieSlop - 1).toFloat(), (-prePieSlop - 1).toFloat())
        }
        view.performClick()
        return dialog.onTouchEvent(outsideEvent)
    }
}