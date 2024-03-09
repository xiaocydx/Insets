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

import android.app.Application
import androidx.fragment.app.ActivitySystemBarController
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentSystemBarController
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED

/**
 * 对[FragmentActivity]和[Fragment]注入[SystemBar]的实现
 *
 * ```
 * class App : Application() {
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         SystemBar.install(this)
 *     }
 * }
 * ```
 *
 * @param default [SystemBarController]属性的默认值
 */
fun SystemBar.Companion.install(
    application: Application,
    default: SystemBarController.Default = SystemBarController.Default()
) {
    SystemBarController.setDefault(default)
    ActivitySystemBarInstaller.register(application)
}

/**
 * ### 使用方式
 * [FragmentActivity]和[Fragment]使用[SystemBar]有三种方式，以[Fragment]为例：
 * ```
 * // 1. 实现SystemBar，应用默认配置
 * class SystemBarDefaultFragment : Fragment(), SystemBar
 *
 * // 2. 实现SystemBar，构造声明配置
 * class SystemBarConstructorFragment : Fragment(), SystemBar {
 *     init {
 *         systemBarController {...}
 *     }
 * }
 *
 * // 3. 实现SystemBar，动态修改配置
 * class SystemBarModifyFragment : Fragment(), SystemBar {
 *      private val controller = systemBarController()
 * }
 * ```
 *
 * ### 宿主[FragmentActivity]
 * 作为宿主的[FragmentActivity]，需要实现[SystemBar.Host]，
 * 当宿主没有自己的`contentView`时，可以不实现[SystemBar]：
 * ```
 * // 没有contentView，实现SystemBar.Host，支持Fragment即可
 * class MainActivity : AppCompatActivity(), SystemBar.Host
 *
 * // 有contentView，实现SystemBar，跟SystemBar.Host没有冲突
 * class MainActivity : AppCompatActivity(), SystemBar, SystemBar.Host
 * ```
 *
 * ### 替换`Fragment.view`
 * 在[Fragment.onCreateView]之后，`Fragment.view`会被替换为[SystemBarContainer]，
 * 不能通过[Fragment.onViewCreated]的形参`view`或`Fragment.view`构建`ViewBinding`，
 * 因为此时`view`跟`ViewBinding.rootView`的类型不一致，构建过程会抛出异常：
 * ```
 * class SystemBarDefaultFragment : Fragment(), SystemBar {
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         // 此时view跟ViewBinding.rootView的类型不一致，构建过程会抛出异常，
 *         // 应当在Fragment.onCreateView()创建view时，构建ViewBinding。
 *         val binding = ViewBinding.bind(view)
 *     }
 * }
 * ```
 *
 * ### 设置和恢复window属性
 * * [SystemBarController.isAppearanceLightStatusBar]。
 * * [SystemBarController.isAppearanceLightNavigationBar]。
 *
 * 以上两个配置项需要设置window属性：
 * 1. [FragmentActivity]设置window属性的时机早于[Fragment]。
 * 2. 当`fragment.lifecycle`的状态转换为[RESUMED]时，按当前配置设置window属性。
 * 3. 当`fragment.lifecycle`的状态转换为[DESTROYED]时，按之前配置恢复window属性。
 *
 * 第3点是用于支持`fragment.lifecycle`的状态没有从[RESUMED]回退的场景，
 * 系统配置更改、进程被杀掉导致[FragmentActivity]重建，第3点也仍然满足。
 *
 * **注意**：第3点仅支持`A -> B -> C`的前进，以及`A <- B <- C`或`A <- C`（B被移除）的后退。
 */
interface SystemBar {

    interface Host

    fun <A> A.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where A : FragmentActivity, A : SystemBar = run {
        require(window == null && lifecycle.currentState === INITIALIZED) {
            "只能在${javaClass.canonicalName}的构造阶段获取${SystemBarController.name}"
        }
        ActivitySystemBarController(this, repeatThrow = true).attach(initializer)
    }

    fun <F> F.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where F : Fragment, F : SystemBar = run {
        require(activity == null && lifecycle.currentState === INITIALIZED) {
            "只能在${javaClass.canonicalName}的构造阶段获取${SystemBarController.name}"
        }
        FragmentSystemBarController(this, repeatThrow = true).attach(initializer)
    }

    companion object
}

internal val SystemBar.Companion.name: String
    get() = SystemBar::class.java.simpleName

internal val SystemBar.Companion.hostName: String
    get() = "${name}.${SystemBar.Host::class.java.simpleName}"