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

@file:Suppress("UnstableApiUsage", "ConstPropertyName")

package com.xiaocydx.insets.lint.check

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.Companion.JAVA_FILE_SCOPE
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * @author xcc
 * @date 2025/1/13
 */
internal class InsetsAnimationCompatDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(
        SetWindowInsetsAnimationCallback,
        SetWindowInsetsAnimationCallbackCompat
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val isSetMethod = fun(methodName: String, className: String) = run {
            method.name == methodName && context.evaluator.isMemberInClass(method, className)
        }
        if (isSetMethod(SetWindowInsetsAnimationCallback, ClassViewCompat)
                || isSetMethod(SetWindowInsetsAnimationCallbackCompat, ClassInsetsCompatKt)) {
            context.report(
                Incident(context, Callback)
                    .message("确保 `WindowInsetsAnimationCompat.Callback` 正常执行")
                    .at(node)
            )
        }
    }

    companion object {
        private const val SetWindowInsetsAnimationCallback = "setWindowInsetsAnimationCallback"
        private const val SetWindowInsetsAnimationCallbackCompat = "setWindowInsetsAnimationCallbackCompat"

        val Callback = Issue.create(
            id = "WindowInsetsAnimationCompatCallback",
            briefDescription = "WindowInsetsAnimationCompat.Callback的兼容处理",
            explanation = """
                ```
                class MainActivity : Activity() {
                
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        
                        // 1. IME交互的必要兼容
                        // 确保Android 11以下触发WindowInsets分发，
                        // Android 11及以上视图树不会滚动到焦点可见的位置。
                        window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)
                        
                        // 2. 对非DecorView的视图设置Callback，需要自行处理WindowInsets
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        
                        // 3. Android 11以下，Callback基于WindowInsets分发实现，
                        // 需要确保View有WindowInsets分发，并且所需类型未被消费。
                        val view = findViewById<View>(android.R.id.content)
                        ViewCompat.setWindowInsetsAnimationCallback(view, callback)
                    }
                }
                ```
                
                依赖 `com.github.xiaocydx.Insets:insets-compat` ，可以使用以下兼容方案：
                ```
                // Android 11以下，window包含FLAG_FULLSCREEN的兼容方案
                window.enableDispatchApplyInsetsFullscreenCompat()
                
                // Android 9.0以下，WindowInsets可变的兼容方案
                // 注意：未返回DecorView处理WindowInsets后的结果，
                // 当代码不好改动或者改动后造成其他影响，再考虑此方案。
                view.setOnApplyWindowInsetsListenerImmutable(listener)
                view.setWindowInsetsAnimationCallbackImmutable(callback)
                ```
            """,
            implementation = Implementation(InsetsAnimationCompatDetector::class.java, JAVA_FILE_SCOPE)
        )
    }
}