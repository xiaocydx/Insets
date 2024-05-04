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
import android.view.Window
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
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
 * [BackStackWindowEnforcer]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class BackStackWindowEnforcerTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private val activityConfig = TestConfig(
        isAppearanceLightStatusBar = true,
        isAppearanceLightNavigationBar = true
    )
    private val fragmentAConfig = TestConfig(
        isAppearanceLightStatusBar = false,
        isAppearanceLightNavigationBar = true
    )
    private val fragmentBConfig = TestConfig(
        isAppearanceLightStatusBar = false,
        isAppearanceLightNavigationBar = false
    )

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java).moveToState(RESUMED)
    }

    @After
    fun release() {
        scenario.close()
    }

    @Test
    fun applyActivityConfig() {
        scenario.onActivity {
            // 生命周期状态转换为RESUMED，应用初始配置
            val enforcer = createEnforcer(it)
            assertThat(enforcer.copyState()?.isApplied).isTrue()
            assertThat(enforcer.copyState()?.isSame(activityConfig)).isFalse()
            assertThat(it.window.isSame(activityConfig)).isFalse()

            // 应用activityConfig
            enforcer.setConfig(activityConfig)
            assertThat(enforcer.copyState()?.isApplied).isTrue()
            assertThat(enforcer.copyState()?.isSame(activityConfig)).isTrue()
            assertThat(it.window.isSame(activityConfig)).isTrue()
        }
    }

    @Test
    fun applyFragmentConfig() {
        scenario.onActivity {
            val fragment = TestFragment()
            it.addFragment(fragment).setMaxLifecycle(fragment, STARTED).commitNow()

            // 生命周期状态未转换为RESUMED，不应用配置
            val enforcer = createEnforcer(fragment)
            assertThat(enforcer.copyState()?.isApplied).isFalse()
            assertThat(enforcer.copyState()?.isSame(fragmentAConfig)).isFalse()
            assertThat(it.window.isSame(fragmentAConfig)).isFalse()

            // 生命周期状态转换为RESUMED，应用初始配置
            it.setMaxLifecycle(fragment, RESUMED).commitNow()
            assertThat(enforcer.copyState()?.isApplied).isTrue()
            assertThat(enforcer.copyState()?.isSame(fragmentAConfig)).isFalse()
            assertThat(it.window.isSame(fragmentAConfig)).isFalse()

            // 应用fragmentAConfig
            enforcer.setConfig(fragmentAConfig)
            assertThat(enforcer.copyState()?.isApplied).isTrue()
            assertThat(enforcer.copyState()?.isSame(fragmentAConfig)).isTrue()
            assertThat(it.window.isSame(fragmentAConfig)).isTrue()
        }
    }

    @Test
    fun destroyFragmentRemoveEnforcer() {
        scenario.onActivity {
            val fragment = TestFragment()
            it.addFragment(fragment).commitNow()

            val enforcer = createEnforcer(fragment)
            assertThat(enforcer.isAttached()).isTrue()
            assertThat(enforcer.copyState()).isNotNull()

            // 销毁fragment.view，不移除enforcer
            it.setMaxLifecycle(fragment, CREATED).commitNow()
            assertThat(enforcer.isAttached()).isTrue()
            assertThat(enforcer.copyState()).isNotNull()

            // 销毁fragment，移除enforcer
            it.removeAllFragment().commitNow()
            assertThat(enforcer.isAttached()).isFalse()
            assertThat(enforcer.copyState()).isNull()
        }
    }

    @Test
    fun fragmentNotInBackStackRemoveState() {
        scenario.onActivity {
            val fragment = TestFragment()
            it.replaceFragment(fragment).commitNow()

            val enforcer = createEnforcer(fragment)
            assertThat(enforcer.isAttached()).isTrue()
            assertThat(enforcer.copyState()).isNotNull()

            // fragment没在Fragment回退栈中，
            // 移除fragment，移除WindowState。
            it.replaceFragment(TestFragment()).commitNow()
            assertThat(enforcer.isAttached()).isFalse()
            assertThat(enforcer.copyState()).isNull()
        }
    }

    @Test
    fun fragmentInBackStackKeepState() {
        scenario.onActivity {
            val fragment = TestFragment()
            it.replaceFragment(fragment).commitToBackStack()

            val enforcer = createEnforcer(fragment)
            assertThat(enforcer.isAttached()).isTrue()
            assertThat(enforcer.copyState()).isNotNull()

            // fragment在Fragment回退栈中，
            // 移除fragment，不移除WindowState。
            it.replaceFragment(TestFragment()).commitNow()
            enforcer.doOnDetached {
                assertThat(enforcer.isAttached()).isFalse()
                assertThat(enforcer.copyState()).isNotNull()
            }
        }
    }

    @Test
    fun recreateActivityKeepState() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)
            val fragment = TestFragment()
            it.addFragment(fragment).commitNow()
            createEnforcer(fragment, fragmentAConfig)
        }

        // 销毁Activity保留WindowState，
        // 维护WindowState回退栈的正确性。
        scenario.recreate()
        scenario.onActivity {
            assertThat(createEnforcer(it).copyState()?.isSame(activityConfig)).isTrue()
            val fragment = it.supportFragmentManager.fragments.first()
            assertThat(fragment).isInstanceOf(TestFragment::class.java)
            assertThat(createEnforcer(fragment).copyState()?.isSame(fragmentAConfig)).isTrue()
        }
    }

    @Test
    fun fragmentNotInBackStackForward() {
        scenario.onActivity {
            // 应用activityConfig
            createEnforcer(it, activityConfig)
            assertThat(it.window.isSame(activityConfig)).isTrue()

            // 添加fragmentA，应用fragmentAConfig
            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitNow()
            createEnforcer(fragmentA, fragmentAConfig)
            assertThat(it.window.isSame(fragmentAConfig)).isTrue()

            // 添加fragmentB，应用fragmentBConfig
            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitNow()
            createEnforcer(fragmentB, fragmentBConfig)
            assertThat(it.window.isSame(fragmentBConfig)).isTrue()
        }
    }

    @Test
    fun fragmentNotInBackStackBackward() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)

            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitNow()
            val fragmentAEnforcer = createEnforcer(fragmentA, fragmentAConfig)

            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitNow()
            val fragmentBEnforcer = createEnforcer(fragmentB, fragmentBConfig)

            // 移除fragmentB，恢复上一个fragmentAConfig
            it.removeFragment(fragmentB).commitNow()
            fragmentBEnforcer.doOnDetached { assertThat(it.window.isSame(fragmentAConfig)).isTrue() }

            // 移除fragmentA，恢复上一个activityConfig
            it.removeFragment(fragmentA).commitNow()
            fragmentAEnforcer.doOnDetached { assertThat(it.window.isSame(activityConfig)).isTrue() }
        }
    }

    @Test
    fun fragmentNotInBackStackRemoveMiddle() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)

            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitNow()
            val fragmentAEnforcer = createEnforcer(fragmentA, fragmentAConfig)

            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitNow()
            val fragmentBEnforcer = createEnforcer(fragmentB, fragmentBConfig)

            // 移除fragmentA，上一个activityConfig不是最后应用的fragmentBConfig，不做恢复处理
            it.removeFragment(fragmentA).commitNow()
            fragmentAEnforcer.doOnDetached { assertThat(it.window.isSame(fragmentBConfig)).isTrue() }

            // 移除fragmentB，恢复上一个activityConfig
            it.removeFragment(fragmentB).commitNow()
            fragmentBEnforcer.doOnDetached { assertThat(it.window.isSame(activityConfig)).isTrue() }
        }
    }

    @Test
    fun fragmentInBackStackForward() {
        scenario.onActivity {
            // 应用activityConfig
            createEnforcer(it, activityConfig)
            assertThat(it.window.isSame(activityConfig)).isTrue()

            // 添加fragmentA，应用fragmentAConfig
            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitToBackStack()
            createEnforcer(fragmentA, fragmentAConfig)
            assertThat(it.window.isSame(fragmentAConfig)).isTrue()

            // 添加fragmentB，应用fragmentBConfig
            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitToBackStack()
            createEnforcer(fragmentB, fragmentBConfig)
            assertThat(it.window.isSame(fragmentBConfig)).isTrue()
        }
    }

    @Test
    fun fragmentInBackStackBackward() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)

            val fragmentA = TestFragment()
            it.replaceFragment(fragmentA).commitToBackStack()
            createEnforcer(fragmentA, fragmentAConfig)

            val fragmentB = TestFragment()
            it.replaceFragment(fragmentB).commitToBackStack()
            createEnforcer(fragmentB, fragmentBConfig)
            assertThat(fragmentA.view).isNull()

            // 出栈fragmentB，恢复上一个fragmentAConfig
            it.supportFragmentManager.popBackStackImmediate()
            assertThat(fragmentA.view).isNotNull()
            assertThat(it.window.isSame(fragmentAConfig)).isTrue()

            // 出栈fragmentA，恢复上一个activityConfig
            it.supportFragmentManager.popBackStackImmediate()
            assertThat(fragmentA.view).isNull()
            assertThat(it.window.isSame(activityConfig)).isTrue()
        }
    }

    @Test
    fun recreateOnlyApplyLastConfig() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)
            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitNow()
            createEnforcer(fragmentA, fragmentAConfig)
            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitNow()
            createEnforcer(fragmentB, fragmentBConfig)
        }

        scenario.recreate()
        scenario.onActivity {
            val fragmentA = it.supportFragmentManager.fragments.first()
            val fragmentB = it.supportFragmentManager.fragments.last()
            val activityEnforcer = createEnforcer(it)
            val fragmentAEnforcer = createEnforcer(fragmentA)
            val fragmentBEnforcer = createEnforcer(fragmentB)

            // 重建后仅应用最后的fragmentBConfig
            assertThat(activityEnforcer.applyStateCount()).isEqualTo(0)
            assertThat(fragmentAEnforcer.applyStateCount()).isEqualTo(0)
            assertThat(fragmentBEnforcer.applyStateCount()).isEqualTo(1)
            assertThat(it.window.isSame(fragmentBConfig)).isTrue()
        }
    }

    @Test
    fun recreateFilterApplyPrevConfig() {
        scenario.onActivity {
            createEnforcer(it, activityConfig)
            val fragmentA = TestFragment()
            it.addFragment(fragmentA).commitNow()
            createEnforcer(fragmentA, fragmentAConfig)
            val fragmentB = TestFragment()
            it.addFragment(fragmentB).commitNow()
            createEnforcer(fragmentB, fragmentBConfig)
            it.setMaxLifecycle(fragmentA, CREATED).commitNow()
        }

        scenario.recreate()
        scenario.onActivity {
            val fragmentA = it.supportFragmentManager.fragments.first()
            val fragmentB = it.supportFragmentManager.fragments.last()
            val activityEnforcer = createEnforcer(it)
            val fragmentAEnforcer = createEnforcer(fragmentA)
            val fragmentBEnforcer = createEnforcer(fragmentB)

            // 重建后重置fragmentA的isApplied，仅应用最后的fragmentBConfig
            assertThat(fragmentAEnforcer.copyState()?.isApplied).isFalse()
            assertThat(activityEnforcer.applyStateCount()).isEqualTo(0)
            assertThat(fragmentAEnforcer.applyStateCount()).isEqualTo(0)
            assertThat(fragmentBEnforcer.applyStateCount()).isEqualTo(1)
            assertThat(it.window.isSame(fragmentBConfig)).isTrue()

            // 移除fragmentB，将fragmentA转换为RESUMED，应用fragmentAConfig，
            // 移除fragmentB，fragmentBEnforcer不会按fragmentA的默认值进行恢复，
            // 即fragmentBEnforcer.applyStateCount()仍然为1。
            it.removeFragment(fragmentB).setMaxLifecycle(fragmentA, RESUMED).commitNow()
            assertThat(activityEnforcer.applyStateCount()).isEqualTo(0)
            assertThat(fragmentAEnforcer.applyStateCount()).isEqualTo(1)
            assertThat(fragmentBEnforcer.applyStateCount()).isEqualTo(1)
            assertThat(fragmentBEnforcer.isAttached()).isFalse()
            assertThat(it.window.isSame(fragmentAConfig)).isTrue()
        }
    }

    private fun FragmentTransaction.commitToBackStack() {
        addToBackStack(null).commit()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createEnforcer(target: Any, config: TestConfig? = null) = when (target) {
        is FragmentActivity -> BackStackWindowEnforcer.create(target)
        is Fragment -> BackStackWindowEnforcer.create(target)
        else -> throw UnsupportedOperationException()
    }.apply { attach() }.apply { if (config != null) setConfig(config) }

    private fun BackStackWindowEnforcer.setConfig(config: TestConfig) = apply {
        setAppearanceLightStatusBar(config.isAppearanceLightStatusBar)
        setAppearanceLightNavigationBar(config.isAppearanceLightNavigationBar)
    }

    private fun WindowState.isSame(config: TestConfig): Boolean {
        return isAppearanceLightStatusBar == config.isAppearanceLightStatusBar
                && isAppearanceLightNavigationBar == config.isAppearanceLightNavigationBar
    }

    private fun Window.isSame(config: TestConfig): Boolean {
        val container = WindowInsetsControllerCompat(this, decorView)
        return container.isAppearanceLightStatusBars == config.isAppearanceLightStatusBar
                && container.isAppearanceLightNavigationBars == config.isAppearanceLightNavigationBar
    }

    private data class TestConfig(
        val isAppearanceLightStatusBar: Boolean,
        val isAppearanceLightNavigationBar: Boolean
    )
}