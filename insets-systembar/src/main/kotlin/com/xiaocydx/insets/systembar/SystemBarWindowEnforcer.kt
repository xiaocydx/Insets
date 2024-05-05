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

import android.view.Window
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.getWhoInternal
import androidx.fragment.app.isInBackStackInternal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 负责应用和恢复[WindowState]
 *
 * @author xcc
 * @date 2023/12/22
 */
internal abstract class SystemBarWindowEnforcer(private val window: Window) {
    private var applyStateCount = 0
    private var isAttached = false
    private var detachedAction: (() -> Unit)? = null
    private val insetsController = WindowInsetsControllerCompat(window, window.decorView)

    fun attach() {
        if (isAttached) return
        isAttached = true
        onAttach()
    }

    fun detach() {
        if (!isAttached) return
        isAttached = false
        onDetach()
        detachedAction?.invoke()
        detachedAction = null
    }

    protected open fun onAttach() = Unit

    protected open fun onDetach() = Unit

    abstract fun setAppearanceLightStatusBar(isLight: Boolean)

    abstract fun setAppearanceLightNavigationBar(isLight: Boolean)

    abstract fun setNavigationBarColor(color: Int)

    protected fun applyState(state: WindowState) = with(state) {
        applyStateCount++
        if (insetsController.isAppearanceLightStatusBars != isAppearanceLightStatusBar) {
            insetsController.isAppearanceLightStatusBars = isAppearanceLightStatusBar
        }
        if (insetsController.isAppearanceLightNavigationBars != isAppearanceLightNavigationBar) {
            insetsController.isAppearanceLightNavigationBars = isAppearanceLightNavigationBar
        }
        if (!isAppearanceLightNavigationBar && window.navigationBarColor != navigationBarColor) {
            // 部分机型设置navigationBarColor，isAppearanceLightNavigationBar = false才会生效，
            // 当state.navigationBarColor是InitialColor时，navigationBarColor可能会被特殊处理。
            window.navigationBarColor = navigationBarColor
        }
    }

    @VisibleForTesting
    fun isAttached() = isAttached

    @VisibleForTesting
    fun applyStateCount() = applyStateCount

    @VisibleForTesting
    abstract fun copyState(): WindowState?

    @VisibleForTesting
    fun doOnDetached(action: () -> Unit) {
        if (!isAttached) return action()
        detachedAction = action
    }
}

internal class SimpleWindowEnforcer(window: Window) : SystemBarWindowEnforcer(window) {
    private var currentState = WindowState()

    override fun setAppearanceLightStatusBar(isLight: Boolean) {
        currentState.isAppearanceLightStatusBar = isLight
        applyCurrentState()
    }

    override fun setAppearanceLightNavigationBar(isLight: Boolean) {
        currentState.isAppearanceLightNavigationBar = isLight
        applyCurrentState()
    }

    override fun setNavigationBarColor(color: Int) {
        currentState.navigationBarColor = color
        applyCurrentState()
    }

    private fun applyCurrentState() {
        currentState.isApplied = true
        applyState(currentState)
    }

    override fun copyState() = currentState.copy()
}

internal class BackStackWindowEnforcer private constructor(
    private val who: String,
    private val window: Window,
    private val lifecycle: Lifecycle,
    private val activeState: Lifecycle.State,
    private val stateHolder: WindowStateHolder,
    private val isSaveStated: () -> Boolean,
    private val canRemoveState: () -> Boolean
) : SystemBarWindowEnforcer(window), LifecycleEventObserver {

    override fun onAttach() {
        if (lifecycle.currentState !== DESTROYED) {
            stateHolder.ensureState(who, isSaveStated())
            lifecycle.addObserver(this)
        }
    }

    override fun onDetach() {
        lifecycle.removeObserver(this)
        if (canRemoveState()) {
            stateHolder.applyPrevState(who)?.let(::applyState)
            stateHolder.removeState(who, isSaveStated())
        }
    }

    override fun setAppearanceLightStatusBar(isLight: Boolean) {
        peekCurrentState()?.isAppearanceLightStatusBar = isLight
        applyCurrentState()
    }

    override fun setAppearanceLightNavigationBar(isLight: Boolean) {
        peekCurrentState()?.isAppearanceLightNavigationBar = isLight
        applyCurrentState()
    }

    override fun setNavigationBarColor(color: Int) {
        peekCurrentState()?.navigationBarColor = color
        applyCurrentState()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        val applyEvent = when (activeState) {
            CREATED -> ON_CREATE
            STARTED -> ON_START
            RESUMED -> ON_RESUME
            else -> null
        }
        when (event) {
            applyEvent -> applyCurrentState()
            ON_DESTROY -> detach()
            else -> return
        }
    }

    private fun peekCurrentState(): WindowState? {
        return stateHolder.peekState(who)
    }

    private fun applyCurrentState() {
        if (lifecycle.currentState.isAtLeast(activeState)) {
            stateHolder.applyState(who)?.let(::applyState)
        }
    }

    override fun copyState() = peekCurrentState()?.copy()

    companion object {
        private val FragmentActivity.isStateSaved: Boolean
            get() = supportFragmentManager.isStateSaved
        private val FragmentActivity.stateHolder: WindowStateHolder
            get() = ViewModelProvider(this)[WindowStateHolder::class.java]

        fun create(
            activity: FragmentActivity
        ) = BackStackWindowEnforcer(
            who = "Activity",
            window = activity.window,
            lifecycle = activity.lifecycle,
            activeState = STARTED,
            stateHolder = activity.stateHolder,
            isSaveStated = { activity.isStateSaved },
            canRemoveState = { false }
        )

        fun create(
            fragment: Fragment
        ) = BackStackWindowEnforcer(
            who = fragment.getWhoInternal(),
            window = fragment.requireActivity().window,
            lifecycle = fragment.lifecycle,
            activeState = RESUMED,
            stateHolder = fragment.requireActivity().stateHolder,
            isSaveStated = { fragment.requireActivity().isStateSaved },
            canRemoveState = { !fragment.isAdded && !fragment.isInBackStackInternal() }
        )
    }
}

/**
 * [navigationBarColor]用于兼容[isAppearanceLightNavigationBar]
 */
internal data class WindowState(
    var isApplied: Boolean = false,
    var isAppearanceLightStatusBar: Boolean = false,
    var isAppearanceLightNavigationBar: Boolean = false,
    var navigationBarColor: Int = 0
)

internal class WindowStateHolder(savaStateHandle: SavedStateHandle) : ViewModel() {
    /**
     * `savaStateHandle`仅保存[backStack]，不需要保存[stateStore]，
     * 当页面重建时，会再次调用[ensureState]构建[WindowState]，
     * 页面构造阶段声明的配置，会通过[peekState]进行赋值。
     */
    private val backStack: ArrayList<String>
    private val stateStore = mutableMapOf<String, WindowState>()

    init {
        val value = savaStateHandle.get<ArrayList<String>>(BACK_STACK_KEY)
        if (value != null) {
            backStack = value
        } else {
            backStack = arrayListOf()
            savaStateHandle[BACK_STACK_KEY] = backStack
        }
    }

    fun ensureState(who: String, isStateSaved: Boolean) {
        checkStateSaved(isStateSaved)
        if (!backStack.contains(who)) {
            backStack.add(who)
        }
        var state = stateStore[who]
        if (state == null) {
            state = WindowState()
            stateStore[who] = state
        }
        // 重建的页面，可能还未执行到修改默认值的生命周期回调函数，
        // 栈顶页面退出，不应当按默认值恢复，需要用isApplied过滤。
        state.isApplied = false
    }

    fun peekState(who: String): WindowState? {
        return stateStore[who]
    }

    fun applyState(who: String): WindowState? {
        val state = stateStore[who]
        state?.isApplied = true
        return if (isLastState(who)) state else null
    }

    fun applyPrevState(who: String): WindowState? {
        var prevState: WindowState? = null
        if (isLastState(who)) {
            val prevIndex = backStack.lastIndex - 1
            val prevWho = backStack.getOrNull(prevIndex)
            if (prevWho != null) prevState = stateStore[prevWho]
        }
        return prevState?.takeIf { it.isApplied }
    }

    fun removeState(who: String, isStateSaved: Boolean): WindowState? {
        checkStateSaved(isStateSaved)
        backStack.remove(who)
        return stateStore.remove(who)
    }

    private fun checkStateSaved(isStateSaved: Boolean) {
        check(!isStateSaved) { "SavedStateHandle已保存，不允许再修改" }
    }

    private fun isLastState(who: String): Boolean {
        return backStack.lastOrNull() == who
    }

    private companion object {
        const val BACK_STACK_KEY = "com.xiaocydx.insets.systembar.BACK_STACK_KEY"
    }
}