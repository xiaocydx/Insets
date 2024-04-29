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

@file:JvmName("SystemBarControllerImplInternalKt")
@file:Suppress("PackageDirectoryMismatch", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.fragment.app

import android.app.Dialog
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.xiaocydx.insets.doOnAttach
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.SystemBarContainer
import com.xiaocydx.insets.systembar.SystemBarController
import com.xiaocydx.insets.systembar.disableDecorFitsSystemWindows
import com.xiaocydx.insets.systembar.hostName
import com.xiaocydx.insets.systembar.initialState
import com.xiaocydx.insets.systembar.name

/**
 * @author xcc
 * @date 2023/12/24
 */
internal sealed class SystemBarControllerImpl : SystemBarController {
    private val default = Default
    private var hasStatusBarColor = default.statusBarColor != null
    private var hasNavigationBarColor = default.navigationBarColor != null
    protected var container: SystemBarContainer? = null
    protected var enforcer: SystemBarStateEnforcer? = null
    protected abstract val window: Window?

    override var statusBarColor = default.statusBarColor ?: 0
        set(value) {
            field = value
            hasStatusBarColor = true
            container?.statusBarColor = value
        }

    override var navigationBarColor = default.navigationBarColor ?: 0
        set(value) {
            field = value
            hasNavigationBarColor = true
            container?.navigationBarColor = value
            enforcer?.setNavigationBarColor(value)
        }

    override var statusBarEdgeToEdge = default.statusBarEdgeToEdge
        set(value) {
            field = value
            container?.statusBarEdgeToEdge = value
        }

    override var navigationBarEdgeToEdge = default.navigationBarEdgeToEdge
        set(value) {
            field = value
            container?.navigationBarEdgeToEdge = value
        }

    override var isAppearanceLightStatusBar = default.isAppearanceLightStatusBar
        set(value) {
            field = value
            enforcer?.setAppearanceLightStatusBar(value)
        }

    override var isAppearanceLightNavigationBar = default.isAppearanceLightNavigationBar
        set(value) {
            field = value
            enforcer?.setAppearanceLightNavigationBar(value)
        }

    protected fun applyPendingSystemBarConfig() {
        val window = requireNotNull(window)
        statusBarColor = when {
            hasStatusBarColor -> statusBarColor
            else -> window.initialState.statusBarColor
        }
        navigationBarColor = when {
            hasNavigationBarColor -> navigationBarColor
            else -> window.initialState.navigationBarColor
        }
        statusBarEdgeToEdge = statusBarEdgeToEdge
        navigationBarEdgeToEdge = navigationBarEdgeToEdge
        isAppearanceLightStatusBar = isAppearanceLightStatusBar
        isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
    }

    fun attach(initializer: (SystemBarController.() -> Unit)? = null) =
            apply { initializer?.invoke(this) }.apply { onAttach() }

    protected abstract fun onAttach()

    companion object {
        @Volatile var Default = SystemBarController.Default()
    }
}

internal class ActivitySystemBarController(
    private val activity: FragmentActivity,
    private val repeatThrow: Boolean
) : SystemBarControllerImpl() {
    private val activityName: String
        get() = activity.javaClass.canonicalName ?: ""
    override val window: Window?
        get() = activity.window

    override fun onAttach() {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (!activity.lifecycle.currentState.isAtLeast(CREATED)) return
                activity.lifecycle.removeObserver(this)
                // activity.performCreate()的执行顺序：
                // 1. activity.onCreate() -> activity.setContentView()
                // 2. activity.dispatchActivityPostCreated()
                // 第2步执行到此处，将contentView的contentParent替换为container，
                // 此时containerId为android.R.id.content的Fragment还未创建view。
                // 第2步之后，不需要支持能再次调用activity.setContentView()，
                // 因为这是错误的调用情况，会导致已添加的Fragment.view被移除。
                container = createContainerThrowOrNull()
                if (container == null) {
                    // Activity构造阶段已创建SystemBarController，
                    // 后注入的SystemBarController不做任何处理。
                    return
                }
                enforcer = SystemBarStateObserver.create(activity)
                applyPendingSystemBarConfig()
            }
        })
    }

    private fun createContainerThrowOrNull(): SystemBarContainer? {
        val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
        for (index in 0 until contentParent.childCount) {
            if (contentParent.getChildAt(index) !is SystemBarContainer) continue
            check(!repeatThrow) { "${activityName}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(contentParent.context)
        while (contentParent.childCount > 0) {
            val child = contentParent.getChildAt(0)
            contentParent.removeViewAt(0)
            container.addView(child)
        }
        contentParent.addView(container)
        container.doOnAttach(ViewCompat::requestApplyInsets)
        return container
    }
}

internal class FragmentSystemBarController(
    private val fragment: Fragment,
    private val repeatThrow: Boolean
) : SystemBarControllerImpl() {
    private val fragmentName: String
        get() = fragment.javaClass.canonicalName ?: ""
    override val window: Window?
        get() = getWindowOrNull()

    override fun onAttach() {
        fragment.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (!fragment.lifecycle.currentState.isAtLeast(RESUMED)) return
                checkFragmentViewOnResume()
            }
        })
        fragment.mViewLifecycleOwnerLiveData.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(owner: LifecycleOwner?) {
                if (owner == null) {
                    container = null
                    enforcer?.remove()
                    enforcer = null
                    return
                }
                if (container == null) {
                    // fragment.performCreateView()的执行顺序：
                    // 1. fragment.mView = fragment.onCreateView()
                    // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
                    // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
                    // 第3步执行到此处，将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
                    // TODO: 支持点击dismiss DialogFragment
                    container = createContainerThrowOrNull(owner)
                    if (container == null) {
                        // Fragment构造阶段已创建SystemBarController，
                        // 后注入的SystemBarController不做任何处理。
                        fragment.viewLifecycleOwnerLiveData.removeObserver(this)
                        return
                    }
                    enforcer = createSystemBarStateEnforcer()
                    disableDecorFitsSystemWindows()
                    applyPendingSystemBarConfig()
                }
            }
        })
    }

    private fun getWindowOrNull() = when (fragment) {
        !is DialogFragment -> fragment.activity?.window
        else -> fragment.dialog?.window
    }

    private fun checkFragmentViewOnResume() {
        if (fragment !is DialogFragment) return
        check(fragment.mView is SystemBarContainer) { "${fragmentName}未创建view" }
    }

    private fun disableDecorFitsSystemWindows() {
        if (fragment !is DialogFragment) return
        requireNotNull(window).disableDecorFitsSystemWindows()
    }

    private fun createSystemBarStateEnforcer() = when (fragment) {
        !is DialogFragment -> SystemBarStateObserver.create(fragment)
        else -> SystemBarStateEnforcer(requireNotNull(window))
    }

    private fun createContainerThrowOrNull(owner: LifecycleOwner): SystemBarContainer? {
        val activity = fragment.requireActivity()
        check(activity is SystemBar.Host) {
            "${activity.javaClass.canonicalName}需要实现${SystemBar.hostName}"
        }
        check(owner.lifecycle.currentState === INITIALIZED) {
            "只能在${fragmentName}的构造阶段获取${SystemBarController.name}"
        }
        check(fragment.mView != null) {
            "${fragmentName}的生命周期状态转换出现异常情况"
        }
        check(fragment.mView.parent == null) {
            "${fragmentName}的view已有parent，不支持替换parent"
        }

        val view = fragment.mView
        if (view is SystemBarContainer) {
            check(!repeatThrow) { "${fragmentName}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(view.context)
        fragment.mView = container.apply {
            addView(view)
            enableConsumeTouchEvent()
            ViewTreeLifecycleOwner.set(this, owner)
            ViewTreeViewModelStoreOwner.set(this, owner as? ViewModelStoreOwner)
            ViewTreeSavedStateRegistryOwner.set(this, owner as? SavedStateRegistryOwner)
            doOnAttach(ViewCompat::requestApplyInsets)
        }
        return container
    }
}

internal class DialogSystemBarController(
    private val dialog: Dialog,
    private val repeatThrow: Boolean
) : SystemBarControllerImpl() {
    private val dialogName: String
        get() = dialog.javaClass.canonicalName ?: ""
    override val window: Window?
        get() = dialog.window

    override fun onAttach() {
        window?.disableDecorFitsSystemWindows()
        window?.decorView?.doOnAttach {
            // TODO: 支持点击dismiss Dialog
            container = createContainerThrowOrNull() ?: return@doOnAttach
            enforcer = SystemBarStateEnforcer(requireNotNull(window))
            applyPendingSystemBarConfig()
        }
    }

    private fun createContainerThrowOrNull(): SystemBarContainer? {
        val contentParent = window?.findViewById<ViewGroup>(android.R.id.content) ?: return null
        for (index in 0 until contentParent.childCount) {
            if (contentParent.getChildAt(index) !is SystemBarContainer) continue
            check(!repeatThrow) { "${dialogName}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(contentParent.context)
        while (contentParent.childCount > 0) {
            val child = contentParent.getChildAt(0)
            contentParent.removeViewAt(0)
            container.addView(child)
        }
        contentParent.addView(container)
        container.doOnAttach(ViewCompat::requestApplyInsets)
        return container
    }
}