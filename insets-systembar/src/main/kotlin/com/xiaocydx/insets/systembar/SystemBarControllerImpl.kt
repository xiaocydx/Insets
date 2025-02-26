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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.insets.systembar

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.getViewInternal
import androidx.fragment.app.setViewInternal
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
import com.xiaocydx.insets.systembar.SystemBarExtensions.Companion.name

/**
 * @author xcc
 * @date 2023/12/24
 */
internal abstract class SystemBarControllerImpl(
    private val systemBarTarget: Any,
    protected val fromInstaller: Boolean
) : SystemBarController {
    private val default = SystemBarController.Default
    private val flags = SystemBarControllerFlags(default)
    protected var container: SystemBarContainer? = null
    protected var enforcer: SystemBarWindowEnforcer? = null
    protected abstract val window: Window?

    final override var statusBarColor = default.statusBarColor ?: 0
        set(value) {
            field = value
            flags.setStatusBarColor()
            container?.statusBarColor = value
            if (!flags.hasAppearanceLightStatusBar) {
                isAppearanceLightStatusBar = isAppearanceLight(value)
                flags.clearAppearanceLightStatusBar()
            }
        }

    final override var navigationBarColor = default.navigationBarColor ?: 0
        set(value) {
            field = value
            flags.setNavigationBarColor()
            container?.navigationBarColor = value
            if (!flags.hasAppearanceLightNavigationBar) {
                isAppearanceLightNavigationBar = isAppearanceLight(value)
                flags.clearAppearanceLightNavigationBar()
            }
        }

    final override var statusBarEdgeToEdge = default.statusBarEdgeToEdge
        set(value) {
            field = value
            container?.statusBarEdgeToEdge = value
        }

    final override var navigationBarEdgeToEdge = default.navigationBarEdgeToEdge
        set(value) {
            field = value
            container?.navigationBarEdgeToEdge = value
        }

    final override var isAppearanceLightStatusBar = default.isAppearanceLightStatusBar ?: false
        set(value) {
            field = value
            flags.setAppearanceLightStatusBar()
            enforcer?.setAppearanceLightStatusBar(value)
        }

    final override var isAppearanceLightNavigationBar = default.isAppearanceLightNavigationBar ?: false
        set(value) {
            field = value
            flags.setAppearanceLightNavigationBar()
            enforcer?.setAppearanceLightNavigationBar(value)
        }

    fun attach(initializer: (SystemBarController.() -> Unit)? = null) = apply {
        if (systemBarTarget is SystemBar.None) return@apply
        initializer?.invoke(this)
        onAttach()
    }

    protected abstract fun onAttach()

    protected fun applyPendingSystemBarConfig() = with(flags) {
        val window = requireNotNull(window)
        if (hasAppearanceLightStatusBar) {
            isAppearanceLightStatusBar = isAppearanceLightStatusBar
        }
        if (hasAppearanceLightNavigationBar) {
            isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
        }
        // 未设置isAppearanceLightStatusBar，根据statusBarColor进行推断
        statusBarColor = when {
            hasStatusBarColor -> statusBarColor
            else -> window.initialState.statusBarColor
        }
        // 未设置isAppearanceLightNavigationBar，根据navigationBarColor进行推断
        navigationBarColor = when {
            hasNavigationBarColor -> navigationBarColor
            else -> window.initialState.navigationBarColor
        }
        statusBarEdgeToEdge = statusBarEdgeToEdge
        navigationBarEdgeToEdge = navigationBarEdgeToEdge
    }
}

internal class ActivitySystemBarController private constructor(
    private val activity: FragmentActivity,
    fromInstaller: Boolean
) : SystemBarControllerImpl(activity, fromInstaller) {
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
                // 第2步之后，不需要支持再次调用activity.setContentView()，
                // 因为这是不合理的做法，会导致已添加的Fragment.view被移除。
                container = createContainerThrowOrNull()
                if (container == null) {
                    // Activity构造阶段已创建SystemBarController，
                    // Installer后创建的SystemBarController不做任何处理。
                    return
                }
                enforcer = BackStackWindowEnforcer.create(activity)
                enforcer!!.attach()
                applyPendingSystemBarConfig()
            }
        })
    }

    private fun createContainerThrowOrNull(): SystemBarContainer? {
        val contentParent = activity.findViewById<ViewGroup>(android.R.id.content)
        for (index in 0 until contentParent.childCount) {
            if (contentParent.getChildAt(index) !is SystemBarContainer) continue
            check(fromInstaller) { "${activity.name}只能关联一个${SystemBarController.name}" }
            return null
        }
        val container = SystemBarContainer(contentParent.context)
        while (contentParent.childCount > 0) {
            val child = contentParent.getChildAt(0)
            contentParent.removeViewAt(0)
            container.addView(child)
        }
        contentParent.addView(container)
        return container
    }

    companion object {

        private fun FragmentActivity.checkStateOnCreate() {
            check(window == null && lifecycle.currentState === INITIALIZED) {
                "只能在${name}的构造阶段获取${SystemBarController.name}"
            }
        }

        fun create(activity: FragmentActivity, fromInstaller: Boolean = false) = run {
            if (!fromInstaller) activity.checkStateOnCreate()
            ActivitySystemBarController(activity, fromInstaller)
        }
    }
}

internal open class FragmentSystemBarController private constructor(
    private val fragment: Fragment,
    fromInstaller: Boolean
) : SystemBarControllerImpl(fragment, fromInstaller) {
    override val window: Window?
        get() = fragment.activity?.window

    final override fun onAttach() {
        fragment.viewLifecycleOwnerLiveData.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(owner: LifecycleOwner?) {
                if (owner == null) {
                    clear()
                    return
                }
                if (container == null) {
                    // fragment.performCreateView()的执行顺序：
                    // 1. fragment.mView = fragment.onCreateView()
                    // 2. fragment.mView.setViewTreeXXXOwner(fragment.mViewLifecycleOwner)
                    // 3. fragment.mViewLifecycleOwnerLiveData.setValue(fragment.mViewLifecycleOwner)
                    // 第3步执行到此处，将fragment.mView替换为container，并对container设置mViewLifecycleOwner。
                    container = createContainerThrowOrNull(owner)
                    if (container == null) {
                        // Fragment构造阶段已创建SystemBarController，
                        // Installer后创建的SystemBarController不做任何处理。
                        fragment.viewLifecycleOwnerLiveData.removeObserver(this)
                        return
                    }
                    enforcer = createEnforcer()
                    enforcer!!.attach()
                    applyPendingSystemBarConfig()
                }
            }
        })

        fragment.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (!fragment.lifecycle.currentState.isAtLeast(RESUMED)) return
                fragment.lifecycle.removeObserver(this)
                checkFragmentViewOnResume()
                if (container != null) checkUnsupportedOnResume()
            }
        })
    }

    @VisibleForTesting
    fun hasContainer() = container != null

    @VisibleForTesting
    fun hasEnforcer() = enforcer != null

    @CallSuper
    protected open fun clear() {
        container = null
        enforcer?.detach()
        enforcer = null
    }

    protected open fun createEnforcer(): SystemBarWindowEnforcer {
        return BackStackWindowEnforcer.create(fragment)
    }

    protected open fun createContainer(view: View): SystemBarContainer {
        val container = SystemBarContainer(view.context)
        container.addView(view)
        container.enableConsumeTouchEvent()
        return container
    }

    private fun createContainerThrowOrNull(owner: LifecycleOwner): SystemBarContainer? {
        val activity = fragment.requireActivity()
        val view = fragment.getViewInternal()
        check(activity is SystemBar) { "${activity.name}作为宿主需要实现${SystemBar.name}" }
        check(view != null) { "${fragment.name}的生命周期状态转换出现异常情况" }
        check(view.parent == null) { "${fragment.name}的view已有parent，不支持替换parent" }
        if (view is SystemBarContainer) {
            check(fromInstaller) { "${fragment.name}只能关联一个${SystemBarController.name}" }
            return null
        }

        val container = createContainer(view)
        fragment.setViewInternal(container)
        ViewTreeLifecycleOwner.set(container, owner)
        ViewTreeViewModelStoreOwner.set(container, owner as? ViewModelStoreOwner)
        ViewTreeSavedStateRegistryOwner.set(container, owner as? SavedStateRegistryOwner)
        return container
    }

    private fun checkFragmentViewOnResume() {
        val view = fragment.getViewInternal()
        check(view != null) { "${fragment.name}未创建view" }
    }

    private fun checkUnsupportedOnResume() {
        val view = fragment.getViewInternal()
        var parent = view?.parent as? ViewGroup
        val contentParentId = android.R.id.content
        val extensions = SystemBarExtensions.all()
        while (parent != null && parent.id != contentParentId) {
            var message: String? = null
            for (i in extensions.indices) {
                message = extensions[i].checkUnsupportedOnResume(fragment, parent)
                if (!message.isNullOrEmpty()) break
            }
            if (!message.isNullOrEmpty()) throw UnsupportedOperationException(message)
            parent = parent.parent as? ViewGroup
        }
    }

    companion object {

        private fun Fragment.checkStateOnCreate() {
            check(activity == null && lifecycle.currentState === INITIALIZED) {
                "只能在${name}的构造阶段获取${SystemBarController.name}"
            }
        }

        fun create(fragment: Fragment, fromInstaller: Boolean = false) = run {
            if (!fromInstaller) fragment.checkStateOnCreate()
            when (fragment) {
                is DialogFragment -> DialogFragmentSystemBarController(fragment, fromInstaller)
                else -> FragmentSystemBarController(fragment, fromInstaller)
            }
        }
    }
}

internal class DialogSystemBarController private constructor(
    private val dialog: Dialog,
    fromInstaller: Boolean
) : SystemBarControllerImpl(dialog, fromInstaller) {
    private var compat: SystemBarDialogCompat? = null
    override val window: Window?
        get() = dialog.window

    override fun onAttach() {
        val window = requireNotNull(window)
        window.disableDecorFitsSystemWindows()
        window.decorView.doOnAttach {
            container = createContainerThrowOrNull() ?: return@doOnAttach
            enforcer = SimpleWindowEnforcer(window)
            enforcer!!.attach()
            applyPendingSystemBarConfig()
        }
    }

    private fun createContainerThrowOrNull(): SystemBarContainer? {
        val window = requireNotNull(window)
        val contentParent = window.findViewById<ViewGroup>(android.R.id.content) ?: return null
        for (index in 0 until contentParent.childCount) {
            if (contentParent.getChildAt(index) !is SystemBarContainer) continue
            check(fromInstaller) { "${dialog.name}只能关联一个${SystemBarController.name}" }
            return null
        }

        if (compat == null) {
            compat = SystemBarDialogCompat(dialog)
            compat!!.attach()
        }

        var isFistChild = true
        val container = SystemBarContainer(contentParent.context)
        while (contentParent.childCount > 0) {
            val child = contentParent.getChildAt(0)
            contentParent.removeViewAt(0)
            container.addView(child)
            if (isFistChild) compat?.setContentView(child)
            isFistChild = false
        }
        contentParent.addView(container)
        return container
    }

    companion object {
        private val Dialog.name: String
            get() = javaClass.canonicalName ?: ""

        private fun Dialog.checkStateOnCreate() {
            check(!isShowing) { "只能在${name}的构造阶段获取${SystemBarController.name}" }
        }

        fun create(dialog: Dialog, fromInstaller: Boolean = false) = run {
            if (!fromInstaller) dialog.checkStateOnCreate()
            DialogSystemBarController(dialog, fromInstaller)
        }
    }
}

private class DialogFragmentSystemBarController(
    private val fragment: DialogFragment,
    fromInstaller: Boolean
) : FragmentSystemBarController(fragment, fromInstaller) {
    private var compat: SystemBarDialogCompat? = null
    override val window: Window?
        get() = fragment.dialog?.window

    override fun clear() {
        super.clear()
        compat?.detach()
        compat = null
    }

    override fun createEnforcer(): SystemBarWindowEnforcer {
        val window = requireNotNull(window)
        window.disableDecorFitsSystemWindows()
        return SimpleWindowEnforcer(window)
    }

    override fun createContainer(view: View): SystemBarContainer {
        if (compat == null) {
            compat = SystemBarDialogCompat(requireNotNull(fragment.dialog))
            compat!!.attach()
        }
        val container = SystemBarContainer(view.context)
        if (view.layoutParams != null) {
            container.addView(view)
        } else {
            container.addView(view, WRAP_CONTENT, WRAP_CONTENT)
        }
        compat?.setContentView(view)
        return container
    }
}