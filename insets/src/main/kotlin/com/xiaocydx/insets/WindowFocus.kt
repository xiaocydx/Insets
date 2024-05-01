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
import android.view.ViewTreeObserver

/**
 * 当[View.hasWindowFocus]为`true`时，调用[action]
 */
inline fun View.doOnHasWindowFocus(crossinline action: (view: View) -> Unit) {
    if (hasWindowFocus()) return action(this)
    OneShotHasWindowFocusListener.add(this) { action(this) }
}

/**
 * 在[onWindowFocusChanged]为`true`调用一次[runnable]后，移除Listener
 *
 * ```
 * val listener = OneShotHasWindowFocusListener.add(view, runnable)
 * // runnable被调用之前，主动移除listener
 * listener.removeListener()
 * ```
 */
class OneShotHasWindowFocusListener private constructor(
    private val view: View,
    private val runnable: Runnable
) : ViewTreeObserver.OnWindowFocusChangeListener, View.OnAttachStateChangeListener {
    private var viewTreeObserver = view.viewTreeObserver

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            removeListener()
            runnable.run()
        }
    }

    fun removeListener() {
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnWindowFocusChangeListener(this)
        } else {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(this)
        }
        view.removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(v: View) {
        viewTreeObserver = view.viewTreeObserver
    }

    override fun onViewDetachedFromWindow(v: View) {
        removeListener()
    }

    companion object {

        fun add(view: View, runnable: Runnable): OneShotHasWindowFocusListener {
            val listener = OneShotHasWindowFocusListener(view, runnable)
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            view.addOnAttachStateChangeListener(listener)
            return listener
        }
    }
}