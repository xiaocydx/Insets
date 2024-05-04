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
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State.CREATED
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
 * [ActivitySystemBarController]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ActivitySystemBarControllerTest {
    private lateinit var systemBarScenario: ActivityScenario<TestSystemBarActivity>

    @Before
    fun setup() {
        systemBarScenario = launch(TestSystemBarActivity::class.java).moveToState(CREATED)
    }

    @After
    fun release() {
        systemBarScenario.close()
    }

    @Test
    fun afterInitializedThrowException() {
        systemBarScenario.onActivity {
            val result = runCatching { ActivitySystemBarController.create(it) }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun moveAllViewsToContainerAfterCreated() {
        systemBarScenario.onActivity {
            assertThat(it.contentView.isAttachedToWindow).isTrue()
            assertThat(it.overlayView.isAttachedToWindow).isTrue()
            assertThat(it.contentView.parent).isEqualTo(it.overlayView.parent)
            assertThat(it.contentView.parent).isInstanceOf(SystemBarContainer::class.java)
            assertThat(it.overlayView.parent).isInstanceOf(SystemBarContainer::class.java)
            assertThat(it.contentView.index()).isEqualTo(0)
            assertThat(it.overlayView.index()).isEqualTo(1)
        }
    }

    @Test
    fun implementSystemBarNone() {
        launch(TestSystemBarNoneActivity::class.java)
            .moveToState(CREATED).onActivity {
                assertThat(it.contentView.isAttachedToWindow).isTrue()
                assertThat(it.overlayView.isAttachedToWindow).isTrue()
                assertThat(it.contentView.parent).isEqualTo(it.overlayView.parent)
                assertThat(it.contentView.parent).isEqualTo(it.contentParent)
                assertThat(it.overlayView.parent).isEqualTo(it.contentParent)
                assertThat(it.contentView.index()).isEqualTo(0)
                assertThat(it.overlayView.index()).isEqualTo(1)
            }
            .close()
    }

    private fun View.index(): Int {
        return (parent as ViewGroup).indexOfChild(this)
    }
}