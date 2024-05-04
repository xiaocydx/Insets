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
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [FragmentSystemBarController]的单元测试
 *
 * @author xcc
 * @date 2024/5/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class FragmentSystemBarControllerTest {
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
    fun noSystemBarHostThrowException() {
        launch(TestSystemBarActivity::class.java)
            .moveToState(RESUMED).onActivity {
                val fragment = TestSystemBarFragment()
                FragmentSystemBarController.create(fragment).attach()
                val transaction = it.addFragment(fragment)
                val result = runCatching { transaction.commitNow() }
                assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            }
            .close()
    }

    @Test
    fun afterInitializedThrowException() {
        hostScenario.onActivity {
            val fragment = TestSystemBarFragment()
            it.addFragment(fragment).commitNow()
            val result = runCatching { FragmentSystemBarController.create(fragment).attach() }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun replaceFragmentViewAfterCreated() {
        hostScenario.onActivity {
            val fragment = TestSystemBarFragment()
            it.addFragment(fragment).commitNow()
            assertThat(fragment.view!!.isAttachedToWindow).isTrue()
            assertThat(fragment.view).isInstanceOf(SystemBarContainer::class.java)
            assertThat(fragment.contentView!!.parent).isEqualTo(fragment.view)
        }
    }

    @Test
    fun implementSystemBarNone() {
        hostScenario.onActivity {
            val fragment = TestSystemBarNoneFragment()
            it.addFragment(fragment).commitNow()
            assertThat(fragment.view!!.isAttachedToWindow).isTrue()
            assertThat(fragment.view).isEqualTo(fragment.contentView)
        }
    }

    @Test
    fun recreateFragmentView() {
        hostScenario.onActivity {
            val fragment = TestFragment()
            val controller = FragmentSystemBarController.create(fragment)
            controller.attach()
            it.addFragment(fragment).commitNow()

            it.setMaxLifecycle(fragment, CREATED).commitNow()
            assertThat(fragment.contentView).isNull()
            assertThat(controller.hasContainer()).isFalse()
            assertThat(controller.hasEnforcer()).isFalse()

            it.setMaxLifecycle(fragment, RESUMED).commitNow()
            assertThat(fragment.contentView).isNotNull()
            assertThat(controller.hasContainer()).isTrue()
            assertThat(controller.hasEnforcer()).isTrue()
        }
    }

    @Test
    fun unsupportedNested() {
        launch(TestSystemBarHostActivity::class.java)
            .moveToState(RESUMED).onActivity { parent ->
                val child = TestSystemBarFragment()
                val transaction = parent.addFragment(child, id = parent.contentView.id)
                val result = runCatching { transaction.commitNow() }
                assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
            }
            .close()

        launch(TestSystemBarNoneActivity::class.java)
            .moveToState(RESUMED).onActivity {
                val parent = TestSystemBarFragment()
                val child = TestSystemBarFragment()
                it.addFragment(parent).commitNow()
                val transaction = parent.childFragmentManager
                    .beginTransaction().add(parent.contentView!!.id, child)
                val result = runCatching { transaction.commitNow() }
                assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
            }
            .close()
    }

    @Test
    fun unsupportedViewPager() {
        hostScenario.onActivity {
            val viewPager = ViewPager(it)
            viewPager.id = ViewCompat.generateViewId()
            it.contentParent.addView(viewPager)
            val result = runCatching {
                viewPager.adapter = TestVpFragmentPagerAdapter(it.supportFragmentManager)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
            it.removeAllFragment().commitNow()
        }
        hostScenario.recreate().onActivity {
            val viewPager = ViewPager(it)
            viewPager.id = ViewCompat.generateViewId()
            it.contentParent.addView(viewPager)
            val result = runCatching {
                viewPager.adapter = TestVpFragmentStatePagerAdapter(it.supportFragmentManager)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
            it.removeAllFragment().commitNow()
        }
    }

    @Test
    fun unsupportedViewPager2() {
        hostScenario.onActivity {
            val viewPager2 = ViewPager2(it)
            it.contentParent.addView(viewPager2)
            val result = runCatching {
                viewPager2.adapter = TestVp2FragmentStateAdapter(it)
                shadowOf(Looper.getMainLooper()).idle()
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
        }
    }

    private class TestVpFragmentPagerAdapter(
        fm: FragmentManager
    ) : FragmentPagerAdapter(fm) {
        override fun getCount() = 1
        override fun getItem(position: Int) = TestSystemBarFragment()
    }

    private class TestVpFragmentStatePagerAdapter(
        fm: FragmentManager
    ) : FragmentStatePagerAdapter(fm) {
        override fun getCount() = 1
        override fun getItem(position: Int) = TestSystemBarFragment()
    }

    private class TestVp2FragmentStateAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount() = 1
        override fun createFragment(position: Int) = TestSystemBarFragment()
    }
}