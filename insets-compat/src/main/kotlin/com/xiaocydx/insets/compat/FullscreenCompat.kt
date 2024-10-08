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

@file:SuppressLint("ObsoleteSdkInt")

package com.xiaocydx.insets.compat

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.*
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import android.widget.Scroller
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import com.xiaocydx.insets.Insets
import com.xiaocydx.insets.compat.FullscreenCompat.Companion.MSG_RESIZED
import com.xiaocydx.insets.compat.FullscreenCompat.Companion.MSG_RESIZED_REPORT

/**
 * 启用Android 11以下`window.attributes.flags`包含[FLAG_FULLSCREEN]的兼容方案
 *
 * 以Android 10显示IME为例：
 * 1. IME进程调用`WindowManagerService.setInsetsWindow()`，
 * 进而调用`DisplayPolicy.layoutWindowLw()`计算各项`insets`。
 *
 * 2. `window.attributes.flags`包含[FLAG_FULLSCREEN]，
 * 或`window.attributes.softInputMode`不包含[SOFT_INPUT_ADJUST_RESIZE]，
 * `DisplayPolicy.layoutWindowLw()`计算的`contentInsets`不会包含IME的数值。
 *
 * 3. `WindowManagerService`通知应用进程的`ViewRootImpl`重新设置`mPendingContentInsets`的数值，
 * 并申请下一帧布局，下一帧由于`mPendingContentInsets`跟`mAttachInfo.mContentInsets`的数值相等，
 * 因此不调用`ViewRootImpl.dispatchApplyInsets()`。
 *
 * 兼容方案：
 * 1. 在`ViewRootImpl.ViewRootHandler`处理完[MSG_RESIZED]或[MSG_RESIZED_REPORT]后，
 * 根据情况申请WindowInsets分发，确保下一帧`ViewRooImpl.performTraversals()`调用
 * `ViewRootImpl.dispatchApplyInsets()`。
 *
 * 2. 替换`ViewRootImpl.mScroller`，禁止滚动到焦点可见的位置，让视图树自行处理WindowInsets。
 *
 * **注意**：Android 9.0和Android 10，若`window.attributes.flags`包含[FLAG_FULLSCREEN]，
 * 则[ViewCompat.setWindowInsetsAnimationCallback]设置的回调对象，其函数可能不会被调用，
 * 这个问题跟`WindowInsetsAnimationCompat.Impl21`的兼容代码有关，该函数不负责兼容这种情况。
 */
fun Window.enableDispatchApplyInsetsFullscreenCompat() {
    if (isFullscreenCompatEnabled) return
    if (isFullscreenCompatNeeded) FullscreenCompat(this).attach()
    decorView.setTag(R.id.tag_dispatch_apply_insets_full_screen_enabled, true)
}

/**
 * 是否已启用[enableDispatchApplyInsetsFullscreenCompat]的兼容方案
 */
internal val Window.isFullscreenCompatEnabled: Boolean
    get() = decorView.getTag(R.id.tag_dispatch_apply_insets_full_screen_enabled) == true

@get:ChecksSdkIntAtLeast(api = 21)
private val isFullscreenCompatNeeded: Boolean
    get() = Build.VERSION.SDK_INT in 21..29 && Insets.isFullScreenCompatEnabled()

@RequiresApi(21)
private class FullscreenCompat(window: Window) : WindowAttacher(window) {

    override fun onAttach(): Unit = with(ViewRootReflection) {
        if (!reflectSucceed) return
        val delegate = window.getViewRootHandlerCallback()
        window.replaceViewRootOf(ViewRootScroller(), ViewRootHandlerCallback(delegate))
    }

    /**
     * 1. 在[startScroll]和[abortAnimation]标记`mScrollY`可能已更改，
     * 2. 在[computeScrollOffset]将`mScrollY`恢复为0，禁止滚动到焦点可见的位置。
     *
     * ```
     * public final class ViewRootImpl implements ViewParent {
     *     private boolean draw(boolean fullRedrawNeeded) {
     *         ...
     *         scrollToRectOrFocus(null, false);
     *         ...
     *         boolean animating = mScroller != null && mScroller.computeScrollOffset();
     *         ...
     *     }
     *
     *     boolean scrollToRectOrFocus(Rect rectangle, boolean immediate) {
     *          ...
     *          if (!immediate) {
     *              mScroller.startScroll(0, mScrollY, 0, scrollY-mScrollY);
     *          } else {
     *              mScroller.abortAnimation();
     *          }
     *          mScrollY = scrollY;
     *          ...
     *     }
     * }
     * ```
     */
    private inner class ViewRootScroller : Scroller(decorView.context) {
        private var maybeScrollYChanged = false

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            maybeScrollYChanged = true
        }

        override fun abortAnimation() {
            super.abortAnimation()
            maybeScrollYChanged = true
        }

        override fun computeScrollOffset(): Boolean {
            if (maybeScrollYChanged) {
                ViewRootReflection.apply { window.setViewRootScrollY(0) }
            }
            maybeScrollYChanged = false
            return false
        }
    }

    /**
     * 在`ViewRootHandler`处理完[MSG_RESIZED]或[MSG_RESIZED_REPORT]后，
     * 若`viewRootImpl.isLayoutRequested = true`，则申请WindowInsets分发。
     * ```
     * public final class ViewRootImpl implements ViewParent {
     *     final class ViewRootHandler extends Handler {
     *
     *         private void handleMessageImpl(Message msg) {
     *             switch (msg.what) {
     *                 case MSG_RESIZED: {
     *                      if (...) {
     *                          break;
     *                      }
     *                 }
     *                 case MSG_RESIZED_REPORT: {
     *                      if (mAdd) {
     *                          ...
     *                          requestLayout();
     *                      }
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     */
    private inner class ViewRootHandlerCallback(
        private val delegate: Handler.Callback?
    ) : Handler.Callback {

        override fun handleMessage(msg: Message): Boolean {
            val viewRootImpl = decorView.parent
            val viewRootHandler = decorView.handler
            var intercept = delegate?.handleMessage(msg) ?: false
            if (viewRootHandler != null && (msg.what == MSG_RESIZED || msg.what == MSG_RESIZED_REPORT)) {
                viewRootHandler.handleMessage(msg)
                viewRootImpl?.takeIf { it.isLayoutRequested }?.requestFitSystemWindows()
                intercept = true
            }
            return intercept
        }
    }

    private companion object {
        const val MSG_RESIZED = 4
        const val MSG_RESIZED_REPORT = 5
    }
}

@RequiresApi(21)
@SuppressLint("PrivateApi")
private object ViewRootReflection : Reflection {
    private var mCallbackField: FieldCache? = null
    private var mScrollYField: FieldCache? = null
    private var mScrollerField: FieldCache? = null
    var reflectSucceed: Boolean = false; private set

    init {
        runCatching {
            val handlerClass = Handler::class.java
            val viewRootClass = Class.forName("android.view.ViewRootImpl")
            mCallbackField = handlerClass.declaredInstanceFields.find("mCallback").toCache()
            val fields = viewRootClass.declaredInstanceFields
            mScrollYField = fields.find("mScrollY").toCache()
            mScrollerField = fields.find("mScroller").toCache()
            reflectSucceed = true
        }.onFailure {
            mScrollYField = null
            mScrollerField = null
            mCallbackField = null
        }
    }

    fun Window.getViewRootHandlerCallback(): Handler.Callback? {
        val viewRootHandler = decorView.handler ?: return null
        return mCallbackField?.get(viewRootHandler) as? Handler.Callback
    }

    fun Window.replaceViewRootOf(scroller: Scroller, callback: Handler.Callback) {
        @Suppress("DEPRECATION")
        if (attributes.flags and FLAG_FULLSCREEN == 0) return
        val viewRootImpl = decorView.parent ?: return
        val viewRootHandler = decorView.handler ?: return
        mScrollerField?.set(viewRootImpl, scroller)
        mCallbackField?.set(viewRootHandler, callback)
    }

    fun Window.setViewRootScrollY(scrollY: Int) {
        val viewRootImpl = decorView.parent ?: return
        mScrollYField?.set(viewRootImpl, scrollY)
    }
}