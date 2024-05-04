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
import android.os.Build
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [SystemBarDialogCompat]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class SystemBarDialogCompatTest {
    private lateinit var systemBarScenario: ActivityScenario<TestSystemBarActivity>

    @Before
    fun setup() {
        systemBarScenario = launch(TestSystemBarActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        systemBarScenario.close()
    }

    @Test
    fun withoutAttributesDeliver() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            var gravity = Gravity.BOTTOM or Gravity.NO_GRAVITY
            dialog.window!!.setGravity(gravity)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.gravity()).isNotEqualTo(gravity)

            gravity = Gravity.TOP
            dialog.window!!.setGravity(gravity)
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.gravity()).isNotEqualTo(gravity)
            dialog.dismiss()
        }
    }

    @Test
    fun withAttributesDeliver() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            val compat = SystemBarDialogCompat(dialog)
            compat.attach()
            compat.setContentView(dialog.contentView)

            var gravity = Gravity.BOTTOM or Gravity.NO_GRAVITY
            dialog.window!!.setGravity(gravity)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.gravity()).isEqualTo(gravity)

            gravity = Gravity.TOP
            dialog.window!!.setGravity(gravity)
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.gravity()).isEqualTo(gravity)
            dialog.dismiss()
        }
    }

    @Test
    fun withoutTouchOutsideDeliver() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.isShowing).isTrue()

            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isTrue()
            dialog.dismiss()
        }
    }

    @Test
    fun withTouchOutsideDeliver() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            val compat = SystemBarDialogCompat(dialog)
            compat.attach()
            compat.setContentView(dialog.contentView)

            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.isShowing).isTrue()

            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isFalse()
            dialog.dismiss()
        }
    }

    @Test
    fun touchContentViewNotCancel() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            val compat = SystemBarDialogCompat(dialog)
            compat.attach()
            compat.setContentView(dialog.contentView)

            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.isShowing).isTrue()

            val point = IntArray(2)
            dialog.contentView.getLocationInWindow(point)
            dialog.dispatchDownAndUpTouchEvent(x = point[0].toFloat(), y = point[1].toFloat())
            assertThat(dialog.isShowing).isTrue()
            dialog.dismiss()
        }
    }

    @Test
    fun setCancelable() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            val compat = SystemBarDialogCompat(dialog)
            compat.attach()
            compat.setContentView(dialog.contentView)

            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.isShowing).isTrue()

            dialog.setCancelable(false)
            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isTrue()

            dialog.setCancelable(true)
            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isFalse()
            dialog.dismiss()
        }
    }

    @Test
    fun setCanceledOnTouchOutside() {
        systemBarScenario.onActivity {
            val dialog = TestDialog(it, SystemBar.DialogTheme)
            val compat = SystemBarDialogCompat(dialog)
            compat.attach()
            compat.setContentView(dialog.contentView)

            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.isShowing).isTrue()

            dialog.setCanceledOnTouchOutside(false)
            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isTrue()

            dialog.setCanceledOnTouchOutside(true)
            dialog.dispatchDownAndUpTouchEvent()
            assertThat(dialog.isShowing).isFalse()
            dialog.dismiss()
        }
    }

    private fun View.gravity(): Int {
        return (layoutParams as FrameLayout.LayoutParams).gravity
    }

    private fun Dialog.dispatchDownAndUpTouchEvent(x: Float = 1f, y: Float = 1f) {
        val decorView = window!!.decorView
        decorView.dispatchTouchEvent(MotionEvent(MotionEvent.ACTION_DOWN, x, y))
        decorView.dispatchTouchEvent(MotionEvent(MotionEvent.ACTION_UP, x, y))
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Suppress("TestFunctionName")
    private fun MotionEvent(action: Int, x: Float, y: Float): MotionEvent {
        return MotionEvent.obtain(0L, 0L, action, x, y, 0)
    }
}