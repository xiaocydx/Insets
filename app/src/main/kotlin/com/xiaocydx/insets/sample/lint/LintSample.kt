package com.xiaocydx.insets.sample.lint

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaocydx.insets.ime
import com.xiaocydx.insets.setWindowInsetsAnimationCallbackCompat

/**
 * @author xcc
 * @date 2025/1/14
 */
class LintSample {

    fun fitsSystemWindows(view: View, fitsSystemWindows: Boolean) {
        view.fitsSystemWindows = true
        view.fitsSystemWindows = fitsSystemWindows
    }

    fun windowInsetsCompat(insets: WindowInsetsCompat) {
        insets.hasSystemWindowInsets()
        insets.systemWindowInsets
        insets.systemWindowInsetLeft
        insets.systemWindowInsetTop
        insets.systemWindowInsetRight
        insets.systemWindowInsetBottom
    }

    fun windowInsetsAnimationCompat(view: View, callback: WindowInsetsAnimationCompat.Callback) {
        ViewCompat.setWindowInsetsAnimationCallback(view, callback)
        view.setWindowInsetsAnimationCallbackCompat(callback)
    }

    fun windowInsetsControllerCompat(window: Window, view: View) {
        val controller = WindowInsetsControllerCompat(window, view)
        controller.show(WindowInsetsCompat.Type.ime())
        controller.show(ime())
    }
}