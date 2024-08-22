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

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import com.xiaocydx.insets.systembar.SystemBarExtensions.Companion.name
import java.util.ServiceLoader

/**
 * [SystemBar]的扩展
 *
 * @author xcc
 * @date 2024/8/21
 */
internal interface SystemBarExtensions {

    /**
     * 检查`fragment.javaClass`是否支持实现[SystemBar]
     *
     * @param fragment 实现[SystemBar]的[Fragment]对象
     * @param parent   `fragment.view`的每一级`parent`
     * @return [UnsupportedOperationException]的`message`，返回非空字符串表示不支持
     */
    fun checkUnsupportedOnResume(fragment: Fragment, parent: ViewGroup): String?

    companion object {
        private val extensions = ServiceLoader.load(
            SystemBarExtensions::class.java,
            SystemBarExtensions::class.java.classLoader
        ).iterator().asSequence().toList()

        internal val Fragment.name: String
            get() = javaClass.canonicalName ?: ""

        internal val FragmentActivity.name: String
            get() = javaClass.canonicalName ?: ""

        internal fun all() = extensions
    }
}

internal class DefaultSystemBarExtensions : SystemBarExtensions {

    override fun checkUnsupportedOnResume(
        fragment: Fragment,
        parent: ViewGroup
    ): String = when {
        parent is SystemBarContainer -> {
            """使用${SystemBar.name}的Fragment不支持父子级关系
                |    Parent ${getParentName(fragment, parent)} : ${SystemBar.name}
                |    Child ${fragment.name} : ${SystemBar.name}
            """.trimMargin()
        }
        parent is ViewPager -> {
            """使用${SystemBar.name}的Fragment不支持ViewPager
               |    ${fragment.name} : ${SystemBar.name}
            """.trimMargin()
        }
        parent.javaClass.name == VP2_CLASS_NAME -> {
            """使用${SystemBar.name}的Fragment不支持ViewPager2
               |    ${fragment.name} : ${SystemBar.name}
            """.trimMargin()
        }
        else -> ""
    }

    private fun getParentName(fragment: Fragment, parentView: View): String {
        var parent = fragment.parentFragment
        while (parent != null) {
            if (parent.view === parentView) return parent.name
            parent = parent.parentFragment
        }
        return fragment.activity?.name ?: ""
    }

    private companion object {
        const val VP2_CLASS_NAME = "androidx.viewpager2.widget.ViewPager2"
    }
}