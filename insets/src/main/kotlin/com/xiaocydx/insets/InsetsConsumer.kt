/*
 * Copyright 2023 xiaocydx
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

package com.xiaocydx.insets

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.annotation.CheckResult
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.InsetsType

/**
 * [WindowInsetsCompat]的消费函数，负责兼容版本和处理细节问题
 *
 * @author xcc
 * @date 2024/4/28
 */
@JvmInline
internal value class InsetsConsumer(private val insets: WindowInsetsCompat) {

    /**
     * 消费指定类型集的Insets，消费结果可作为`DecorView.onApplyWindowInsets()`的入参
     *
     * **注意**：
     * 消费结果不能作为`DecorView.onApplyWindowInsets()`的返回值，该函数跟[consumeInsets]的区别，
     * 是调用了`WindowInsetsCompat.Builder.setInsetsIgnoringVisibility(typeMask, Insets.NONE)`，
     * 目的是兼容Android 11及以上，`DecorView.onApplyWindowInsets()`处理[WindowInsets]的逻辑，
     * 确保`DecorView`不处理[typeMask]的数值。
     *
     * ```
     * decorView.setOnApplyWindowInsetsListenerCompat { _, insets ->
     *     val typeMask = statusBars()
     *     val outcome = insets.decorInsets(typeMask)
     *     decorView.onApplyWindowInsetsCompat(outcome)
     *     insets // 注意，不能返回outcome
     * }
     * ```
     */
    @CheckResult
    fun decorInsets(@InsetsType typeMask: Int): WindowInsetsCompat {
        if (typeMask == 0) return insets
        checkTypeMask(typeMask)
        val outcome = newBuilder(typeMask)
            .setInsetsIgnoringVisibility(typeMask, Insets.NONE)
            .build()
        insets.copyWindowDataInto(outcome)
        return outcome
    }

    /**
     * 消费指定类型集的Insets，消费结果可作为[View.onApplyWindowInsets]的返回值
     *
     * ```
     * view.setOnApplyWindowInsetsListenerCompat { _, insets ->
     *     val typeMask = statusBars()
     *     val outcome = insets.consumeInsets(typeMask)
     *     outcome.getInsets(typeMask) // 改变Insets结果，返回Insets.NONE
     *     outcome.getInsetsIgnoringVisibility(typeMask) // 不改变Insets结果
     *     outcome.isVisible(typeMask) // 改变isVisible结果，返回false
     *     outcome
     * }
     * ```
     */
    @CheckResult
    fun consumeInsets(@InsetsType typeMask: Int): WindowInsetsCompat {
        if (typeMask == 0) return insets
        checkTypeMask(typeMask)
        val outcome = newBuilder(typeMask).build()
        insets.copyWindowDataInto(outcome)
        return outcome
    }

    /**
     * ### Android 11及以上
     * 调用`WindowInsetsCompat.Builder.setInsets(ime(), Insets.NONE)`,
     * `builder.build()`构建的[WindowInsetsCompat]不会获取到[ime]的数值，
     * 分发过程对子View构建的[WindowInsetsCompat]也不会获取到[ime]的数值，
     * 符合消费意图。
     *
     * ### Android 11以下
     * 调用`WindowInsetsCompat.Builder.setInsets(ime(), Insets.NONE)`,
     * `builder.build()`构建的[WindowInsetsCompat]不会获取到[ime]的数值，
     * 分发过程对子View构建的[WindowInsetsCompat]，其`Root`数据不受消费逻辑影响，
     * 仍能计算出[ime]的数值，不符合消费意图。
     *
     * 为了统一全版本的消费表现，不支持消费[ime]类型的数值。
     */
    private fun checkTypeMask(@InsetsType typeMask: Int) {
        require(typeMask and ime() == 0) { "不支持消费ime()类型的Insets" }
    }

    /**
     * ### Android 11及以上
     * 调用`WindowInsetsCompat.Builder.setInsetsIgnoringVisibility(typeMask, Insets.NONE)`,
     * `builder.build()`构建的[WindowInsetsCompat]不会获取到[typeMask]的数值，符合消费意图。
     *
     * ### Android 11以下
     * 调用`WindowInsetsCompat.Builder.setInsetsIgnoringVisibility(typeMask, Insets.NONE)`,
     * `builder.build()`构建的[WindowInsetsCompat]仍能获取到[typeMask]的数值，不符合消费意图。
     * 修改`Root`数据和`mPlatformInsets`能实现效果，但分发过程对子View构建的[WindowInsetsCompat]，
     * 其`Root`数据不受消费逻辑影响，仍不符合消费意图。
     *
     * ### SystemWindowInsets
     * Android 11及以上，`WindowInsets.systemWindowInsets`不包含[ime]的数值，Android 11以下包含[ime]的数值。
     * `builder.build()`构建的[WindowInsetsCompat]会替换`WindowInsets.systemWindowInsets`，去除[ime]的数值，
     * 不能修正构建结果的`systemWindowInsets`，这会导致[WindowInsetsCompat.getInsets]获取到错误的数值结果。
     * 对构建结果补充`Root`数据，即可让构建结果计算出[ime]的数值，这一步由[copyWindowDataInto]完成。
     *
     * 为了统一全版本的消费表现，仅调用`WindowInsetsCompat.Builder.setInsets(typeMask, Insets.NONE)`。
     */
    private fun newBuilder(@InsetsType typeMask: Int) = WindowInsetsCompat.Builder(insets)
        .setInsets(typeMask, Insets.NONE).setVisible(typeMask, false)

    /**
     * ### Android 11及以上
     * 构建的[WindowInsetsCompat]可以不包含`mRootWindowInsets`和`mRootViewVisibleInsets`。
     *
     * ### Android 11以下
     * 构建的[WindowInsetsCompat]需要包含`mRootWindowInsets`和`mRootViewVisibleInsets`，
     * 才能确保`WindowInsets.systemWindowInsets`去除[ime]的数值后，仍能计算出[ime]的数值。
     * [WindowInsetsCompat.Builder]构建的[WindowInsetsCompat]会缺失`Root`数据，需要补充。
     */
    private fun WindowInsetsCompat.copyWindowDataInto(other: WindowInsetsCompat) {
        if (!isCompatNeeded || mImplFiled == null || copyWindowDataIntoMethod == null) return
        copyWindowDataIntoMethod.invoke(mImplFiled.get(this), other)
    }

    private companion object {
        val isCompatNeeded = Build.VERSION.SDK_INT < 30

        val mImplFiled = runCatching {
            if (!isCompatNeeded) return@runCatching null
            WindowInsetsCompat::class.java.getDeclaredField("mImpl")
        }.getOrNull()?.apply { isAccessible = true }

        val copyWindowDataIntoMethod = runCatching {
            if (!isCompatNeeded) return@runCatching null
            val clazz = Class.forName("androidx.core.view.WindowInsetsCompat\$Impl")
            clazz.getDeclaredMethod("copyWindowDataInto", WindowInsetsCompat::class.java)
        }.getOrNull()?.apply { isAccessible = true }
    }
}