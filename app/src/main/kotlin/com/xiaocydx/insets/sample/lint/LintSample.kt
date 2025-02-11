package com.xiaocydx.insets.sample.lint

import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.xiaocydx.insets.compat.setOnApplyWindowInsetsListenerImmutable
import com.xiaocydx.insets.compat.setWindowInsetsAnimationCallbackImmutable
import com.xiaocydx.insets.setOnApplyWindowInsetsListenerCompat
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

        val builder = WindowInsetsCompat.Builder(insets)
        builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.NONE)
        builder.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
    }

    fun windowInsetsAnimationCompat(view: View, callback: WindowInsetsAnimationCompat.Callback) {
        ViewCompat.setWindowInsetsAnimationCallback(view, callback)
        view.setWindowInsetsAnimationCallbackCompat(callback)
        view.setWindowInsetsAnimationCallbackImmutable(callback)
    }

    fun windowInsetsControllerCompat(window: Window, view: View) {
        val controller = WindowInsetsControllerCompat(window, view)
        controller.show(WindowInsetsCompat.Type.ime())
    }

    fun windowInsetsDispatchConsume(view: View) {
        class CustomView : View(view.context) {
            override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                return super.dispatchApplyWindowInsets(insets)
            }

            override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                return super.onApplyWindowInsets(insets)
            }
        }

        class CustomOnApplyWindowInsetsListener : View.OnApplyWindowInsetsListener {
            override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
                return insets.consumeStableInsets()
            }
        }

        class CustomOnApplyWindowInsetsListenerCompat : androidx.core.view.OnApplyWindowInsetsListener {
            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                return insets.consumeStableInsets()
            }
        }

        View.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
        androidx.core.view.OnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
        view.setOnApplyWindowInsetsListener { v, insets -> insets.consumeStableInsets() }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> insets.consumeStableInsets() }
        view.setOnApplyWindowInsetsListenerCompat { _, insets -> insets.consumeStableInsets() }
        view.setOnApplyWindowInsetsListenerImmutable { _, insets -> insets.consumeStableInsets() }
    }
}