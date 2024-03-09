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

package com.xiaocydx.insets

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.ViewCompat

/**
 * 更新`margins`，有改变才申请重新布局
 */
fun View.updateMargins(
    left: Int = marginLeft,
    top: Int = marginTop,
    right: Int = marginRight,
    bottom: Int = marginBottom
) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val changed = left != marginLeft || top != marginTop
            || right != marginTop || bottom != marginBottom
    params.setMargins(left, top, right, bottom)
    if (changed) layoutParams = params
}

/**
 * [View]的状态，可用于记录初始状态
 */
data class ViewState(
    /**
     * [View]的params
     */
    val params: ViewParams = ViewParams(),

    /**
     * [View]的paddings
     */
    val paddings: ViewPaddings = ViewPaddings(),
) {
    constructor(view: View) : this(ViewParams(view), ViewPaddings(view))
}

/**
 * [View]的params
 */
data class ViewParams(
    @Px val width: Int = 0,
    @Px val height: Int = 0,
    @Px val marginLeft: Int = 0,
    @Px val marginTop: Int = 0,
    @Px val marginRight: Int = 0,
    @Px val marginBottom: Int = 0
) {
    constructor(view: View) : this(
        width = view.layoutParams?.width ?: 0,
        height = view.layoutParams?.height ?: 0,
        marginLeft = view.marginLeft,
        marginTop = view.marginTop,
        marginRight = view.marginRight,
        marginBottom = view.marginBottom
    )
}

/**
 * [View]的paddings
 */
data class ViewPaddings(
    @Px val left: Int = 0,
    @Px val top: Int = 0,
    @Px val right: Int = 0,
    @Px val bottom: Int = 0
) {
    constructor(view: View) : this(
        left = view.paddingLeft,
        top = view.paddingTop,
        right = view.paddingRight,
        bottom = view.paddingBottom
    )
}

internal inline val View.marginLeft: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0

internal inline val View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0

internal inline val View.marginRight: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0

internal inline val View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0

internal inline fun View.updateLayoutParams(block: ViewGroup.LayoutParams.() -> Unit) {
    val params = layoutParams
    block(params)
    layoutParams = params
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

internal inline fun View.doOnAttach(crossinline action: (view: View) -> Unit): OneShotAttachStateListener? {
    return if (ViewCompat.isAttachedToWindow(this)) {
        action(this)
        null
    } else {
        OneShotAttachStateListener(this, isAttach = true) { action(it) }
    }
}

internal inline fun View.doOnDetach(crossinline action: (view: View) -> Unit): OneShotAttachStateListener? {
    return if (!ViewCompat.isAttachedToWindow(this)) {
        action(this)
        null
    } else {
        OneShotAttachStateListener(this, isAttach = false) { action(it) }
    }
}

internal class OneShotAttachStateListener(
    private val view: View,
    private val isAttach: Boolean,
    private val action: (view: View) -> Unit
) : View.OnAttachStateChangeListener {

    init {
        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        if (isAttach) complete()
    }

    override fun onViewDetachedFromWindow(view: View) {
        if (!isAttach) complete()
    }

    private fun complete() {
        removeListener()
        action(view)
    }

    fun removeListener() {
        view.removeOnAttachStateChangeListener(this)
    }
}