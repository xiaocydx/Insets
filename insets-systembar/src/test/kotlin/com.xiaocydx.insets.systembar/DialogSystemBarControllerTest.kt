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

import android.os.Build
import android.os.Looper
import android.view.View
import android.view.ViewGroup
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
 * [DialogSystemBarController]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class DialogSystemBarControllerTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        scenario.close()
    }

    @Test
    fun errorThemeThrowException() {
        scenario.onActivity {
            val dialog = TestSystemBarDialog(it, theme = 0, callController = false)
            val result = runCatching { DialogSystemBarController.create(dialog).attach() }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            dialog.dismiss()
        }
    }

    @Test
    fun afterInitializedThrowException() {
        scenario.onActivity {
            val dialog = TestSystemBarDialog(it, callController = false)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            val result = runCatching { DialogSystemBarController.create(dialog) }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            dialog.dismiss()
        }
    }

    @Test
    fun withoutDialogInstaller() {
        scenario.onActivity {
            val dialog = TestSystemBarDialog(it, callController = false)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.isAttachedToWindow).isTrue()
            assertThat(dialog.overlayView.isAttachedToWindow).isTrue()
            assertThat(dialog.contentView.parent).isEqualTo(dialog.overlayView.parent)
            assertThat(dialog.contentView.parent).isEqualTo(dialog.contentParent)
            assertThat(dialog.overlayView.parent).isEqualTo(dialog.contentParent)
            assertThat(dialog.contentView.index()).isEqualTo(0)
            assertThat(dialog.overlayView.index()).isEqualTo(1)
            dialog.dismiss()
        }
    }

    @Test
    fun moveAllViewsToContainerAfterAttached() {
        scenario.onActivity {
            val dialog = TestSystemBarDialog(it, callController = true)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.isAttachedToWindow).isTrue()
            assertThat(dialog.overlayView.isAttachedToWindow).isTrue()
            assertThat(dialog.contentView.parent).isEqualTo(dialog.overlayView.parent)
            assertThat(dialog.contentView.parent).isInstanceOf(SystemBarContainer::class.java)
            assertThat(dialog.overlayView.parent).isInstanceOf(SystemBarContainer::class.java)
            assertThat(dialog.contentView.index()).isEqualTo(0)
            assertThat(dialog.overlayView.index()).isEqualTo(1)
            dialog.dismiss()
        }
    }

    @Test
    fun implementSystemBarNone() {
        scenario.onActivity {
            val dialog = TestSystemBarNoneDialog(it)
            dialog.show()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(dialog.contentView.isAttachedToWindow).isTrue()
            assertThat(dialog.overlayView.isAttachedToWindow).isTrue()
            assertThat(dialog.contentView.parent).isEqualTo(dialog.overlayView.parent)
            assertThat(dialog.contentView.parent).isEqualTo(dialog.contentParent)
            assertThat(dialog.overlayView.parent).isEqualTo(dialog.contentParent)
            assertThat(dialog.contentView.index()).isEqualTo(0)
            assertThat(dialog.overlayView.index()).isEqualTo(1)
            dialog.dismiss()
        }
    }

    @Test
    fun repeatShowAndDismiss() {
        scenario.onActivity {
            val dialog = TestSystemBarDialog(it, callController = true)
            repeat(2) {
                dialog.show()
                shadowOf(Looper.getMainLooper()).idle()
                assertThat(dialog.createCount).isEqualTo(1)
                assertThat(dialog.contentView.isAttachedToWindow).isTrue()
                assertThat(dialog.overlayView.isAttachedToWindow).isTrue()
                assertThat(dialog.contentView.parent).isEqualTo(dialog.overlayView.parent)
                assertThat(dialog.contentView.parent).isInstanceOf(SystemBarContainer::class.java)
                assertThat(dialog.overlayView.parent).isInstanceOf(SystemBarContainer::class.java)
                assertThat(dialog.contentView.index()).isEqualTo(0)
                assertThat(dialog.overlayView.index()).isEqualTo(1)
                dialog.dismiss()
            }
        }
    }

    private fun View.index(): Int {
        return (parent as ViewGroup).indexOfChild(this)
    }
}