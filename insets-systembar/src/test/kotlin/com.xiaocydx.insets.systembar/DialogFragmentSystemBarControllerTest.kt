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

@file:Suppress("KDocUnresolvedReference")

package com.xiaocydx.insets.systembar

import android.os.Build
import android.os.Looper
import androidx.fragment.app.FragmentSystemBarController
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
 * [DialogFragmentSystemBarController]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class DialogFragmentSystemBarControllerTest {
    private lateinit var hostScenario: ActivityScenario<TestSystemBarHostActivity>

    @Before
    fun setup() {
        hostScenario = launch(TestSystemBarHostActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        hostScenario.close()
    }

    @Test
    fun notCreateViewThrowException() {
        hostScenario.onActivity {
            val fragment = TestSystemBarDialogFragment()
            fragment.createView = false
            val result = runCatching {
                fragment.show(it.supportFragmentManager, null)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun errorThemeThrowException() {
        hostScenario.onActivity {
            val fragment = TestSystemBarDialogFragment()
            fragment.theme = 0
            val result = runCatching {
                fragment.show(it.supportFragmentManager, null)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun afterInitializedThrowException() {
        hostScenario.onActivity {
            val fragment = TestSystemBarDialogFragment()
            fragment.show(it.supportFragmentManager, null)
            shadowOf(Looper.getMainLooper()).idle()
            val result = runCatching { FragmentSystemBarController.create(fragment) }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun replaceFragmentViewAfterCreated() {
        hostScenario.onActivity {
            val fragment = TestSystemBarDialogFragment()
            fragment.show(it.supportFragmentManager, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(fragment.view!!.isAttachedToWindow).isTrue()
            assertThat(fragment.view).isInstanceOf(SystemBarContainer::class.java)
            assertThat(fragment.contentView!!.parent).isEqualTo(fragment.view)
        }
    }

    @Test
    fun implementSystemBarNone() {
        hostScenario.onActivity {
            val fragment = TestSystemBarNoneDialogFragment()
            fragment.show(it.supportFragmentManager, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(fragment.view!!.isAttachedToWindow).isTrue()
            assertThat(fragment.view).isEqualTo(fragment.contentView)
        }
    }

    @Test
    fun recreateFragmentView() {
        hostScenario.onActivity {
            val fragment = TestSystemBarDialogFragment()
            val controller = FragmentSystemBarController.create(fragment)
            controller.attach()
            fragment.show(it.supportFragmentManager, null)
            shadowOf(Looper.getMainLooper()).idle()

            fragment.dismiss()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(fragment.contentView).isNull()
            assertThat(controller.hasContainer()).isFalse()
            assertThat(controller.hasEnforcer()).isFalse()

            fragment.show(it.supportFragmentManager, null)
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(fragment.contentView).isNotNull()
            assertThat(controller.hasContainer()).isTrue()
            assertThat(controller.hasEnforcer()).isTrue()
        }
    }
}