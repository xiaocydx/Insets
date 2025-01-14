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
import android.app.Dialog
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State.DESTROYED
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
    SystemBarController.Default = default
    ActivitySystemBarInstaller.register(application)
}

/**
 * ### 使用方式
 * [SystemBar]有三种使用方式，以[Fragment]为例：
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
 * 作为宿主的[FragmentActivity]，需要实现[SystemBar]，
 * 当宿主没有`contentView`时，可以实现[SystemBar.None]：
 * ```
 * class MainActivity : AppCompatActivity(), SystemBar, SystemBar.None
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
 * 以上两个配置项会设置window属性：
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

    /**
     * 当不需要[SystemBar]的实现时，可以实现该接口，
     * 例如父类实现了[SystemBar]，但子类不需要实现：
     * ```
     * open class SuperFragment : Fragment(), SystemBar
     * class SubFragment : SuperFragment(), SystemBar.None
     * ```
     */
    interface None

    companion object
}

/**
 * 用于[Dialog]和[DialogFragment]的主题
 */
@get:StyleRes
val SystemBar.Companion.DialogTheme: Int
    get() = R.style.SystemBarDialog

val SystemBar.Companion.name: String
    get() = SystemBar::class.java.simpleName

fun <A> A.systemBarController(
    initializer: (SystemBarController.() -> Unit)? = null
): SystemBarController where A : FragmentActivity, A : SystemBar = run {
    ActivitySystemBarController.create(this).attach(initializer)
}

/**
 * [DialogFragment]使用[SystemBar]：
 * 1. Dialog主题需要`windowIsFloating = false`，可直接使用[DialogTheme]。
 * 2. 支持[SystemBar]的全部使用方式，跟Fragment一致。
 * 3. 需要通过[Fragment.onCreateView]创建`contentView`。
 *
 * ```
 * class SystemBarDialogFragment : DialogFragment(contentLayoutId), SystemBar {
 *      override fun getTheme() = SystemBar.DialogTheme
 * }
 * ```
 */
fun <F> F.systemBarController(
    initializer: (SystemBarController.() -> Unit)? = null
): SystemBarController where F : Fragment, F : SystemBar = run {
    FragmentSystemBarController.create(this).attach(initializer)
}

/**
 * [Dialog]使用[SystemBar]：
 * 1. Dialog主题需要`windowIsFloating = false`，可直接使用[DialogTheme]。
 * 2. 不支持[SystemBar]的应用默认配置使用方式，需要调用该函数。
 * ```
 * class SystemBarDialog(
 *     context: Context
 * ) : Dialog(context, SystemBar.DialogTheme), SystemBar {
 *      init {
 *          systemBarController {...}
 *      }
 * }
 * ```
 */
fun <D> D.systemBarController(
    initializer: (SystemBarController.() -> Unit)? = null
): SystemBarController where D : Dialog, D : SystemBar = run {
    DialogSystemBarController.create(this).attach(initializer)
}