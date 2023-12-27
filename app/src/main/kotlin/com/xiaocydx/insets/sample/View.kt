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
package com.xiaocydx.insets.sample

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.updateLayoutParams

/**
 * [TypedValue.complexToDimensionPixelSize]的舍入逻辑，
 * 用于确保[dp]转换的px值，和xml解析转换的px值一致。
 */
@Px
private fun Float.toRoundingPx(): Int {
    return (if (this >= 0) this + 0.5f else this - 0.5f).toInt()
}

@get:Px
val Int.dp: Int
    get() = toFloat().dp

@get:Px
val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toRoundingPx()

const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

inline fun View.onClick(crossinline block: () -> Unit) {
    setOnClickListener { block() }
}

fun <V : View> V.overScrollNever(): V = apply {
    overScrollMode = View.OVER_SCROLL_NEVER
}

fun <V : View> V.layoutParams(
    width: Int,
    height: Int,
    block: ViewGroup.MarginLayoutParams.() -> Unit = {}
): V = apply {
    if (layoutParams != null) {
        updateLayoutParams {
            this.width = width
            this.height = height
            (this as? ViewGroup.MarginLayoutParams)?.block()
        }
    } else {
        layoutParams = ViewGroup.MarginLayoutParams(width, height).apply(block)
    }
}