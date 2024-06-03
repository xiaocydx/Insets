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

import android.graphics.Color
import android.os.Build
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SystemBarControllerWrapper]的单元测试
 *
 * @author xcc
 * @date 2024/5/8
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class SystemBarControllerWrapperTest {
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
    fun windowSystemBarColor() {
        systemBarScenario.onActivity {
            val fragment = TestSystemBarFragment()
            val wrapper = SystemBarControllerWrapper()
            wrapper.attachDelegate(fragment.systemBarController())
            it.addFragment(fragment).commitNow()
            assertThat(wrapper.statusBarColor).isEqualTo(it.window.initialState.statusBarColor)
            assertThat(wrapper.navigationBarColor).isEqualTo(it.window.initialState.navigationBarColor)
        }
    }

    @Test
    fun deliverProperty() {
        systemBarScenario.onActivity {
            val color = Color.BLUE
            val fragment = TestSystemBarFragment()
            val wrapper = SystemBarControllerWrapper()
            wrapper.statusBarColor = color
            wrapper.navigationBarColor = color
            val delegate = fragment.systemBarController()
            wrapper.attachDelegate(delegate)
            it.addFragment(fragment).commitNow()
            assertThat(delegate.statusBarColor).isEqualTo(color)
            assertThat(delegate.navigationBarColor).isEqualTo(color)
        }
    }

    @Test
    fun repeatAttachThrowException() {
        systemBarScenario.onActivity {
            val fragment = TestSystemBarFragment()
            val wrapper = SystemBarControllerWrapper()
            val delegate = fragment.systemBarController()
            wrapper.attachDelegate(delegate)
            val result = runCatching { wrapper.attachDelegate(delegate) }
            assertThat(result.exceptionOrNull()).isNotNull()
        }
    }
}